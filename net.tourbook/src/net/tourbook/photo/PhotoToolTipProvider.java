/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.photo;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.data.TourWayPoint;
import net.tourbook.photo.manager.Photo;
import net.tourbook.ui.IMapToolTipProvider;
import net.tourbook.util.HoveredAreaContext;
import net.tourbook.util.ITourToolTipProvider;
import net.tourbook.util.TourToolTip;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.mapprovider.MP;

public class PhotoToolTipProvider implements ITourToolTipProvider, IMapToolTipProvider {

//	private TourToolTip		_tourToolTip;

	private Color				_bgColor;
	private Color				_fgColor;
	private Font				_boldFont;

	private Photo				_photo;

	private final NumberFormat	_nf_1_1	= NumberFormat.getNumberInstance();
	{
		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);
	}

	public PhotoToolTipProvider() {}

	@Override
	public void afterHideToolTip() {
		_photo = null;
	}

	public Composite createToolTipContentArea(final Event event, final Composite parent) {

		if (_photo == null) {
			// this case should not happen
			return null;
		}

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		return createUI(parent);
	}

	private Composite createUI(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		Label label;

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{

			final Composite container = new Composite(shellContainer, SWT.NONE);
			container.setForeground(_fgColor);
			container.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults()//
					.margins(TourToolTip.SHELL_MARGIN, TourToolTip.SHELL_MARGIN)
					.numColumns(2)
					.spacing(5, 2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				/*
				 * name
				 */
				final String name = Double.toString(_photo.getLatitude());
				if (name != null) {

					label = createUILabel(container, name);
					GridDataFactory.fillDefaults().span(2, 1).indent(0, -5).applyTo(label);
					label.setFont(_boldFont);
				}
			}
		}

		return shellContainer;
	}

	private Label createUILabel(final Composite parent, final String labelText) {

		final Label label = new Label(parent, SWT.NONE);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (labelText != null) {
			label.setText(labelText);
		}

		return label;
	}

	@Override
	public HoveredAreaContext getHoveredContext(final int mousePositionX,
												final int mousePositionY,
												final Rectangle worldPixelTopLeftViewPort,
												final MP mp,
												final int mapZoomLevel,
												final int tilePixelSize,
												final boolean isTourPaintMethodEnhanced,
												final Object requestedTwp) {

		// get mouse world position
		final int worldPixelMouseX = worldPixelTopLeftViewPort.x + mousePositionX;
		final int worldPixelMouseY = worldPixelTopLeftViewPort.y + mousePositionY;

		// get tile from mouse position
		final int tileX = worldPixelMouseX / tilePixelSize;
		final int tileY = worldPixelMouseY / tilePixelSize;

		final int worldPixelTileX = tileX * tilePixelSize;
		final int worldPixelTileY = tileY * tilePixelSize;

		// get tile from the map provider
		final Tile tile = mp.getTile(tileX, tileY, mapZoomLevel);

		final ArrayList<Rectangle> wayPointBounds = tile.getWayPointBounds(mapZoomLevel, isTourPaintMethodEnhanced);
		if (wayPointBounds != null) {

			// tile contains way points

			final ArrayList<TourWayPoint> allTileTwp = tile.getWayPoints(mapZoomLevel);
			int wpIndex = -1;

			for (final Rectangle wayPointBoundInTile : wayPointBounds) {

				final int wpInTileX = wayPointBoundInTile.x;
				final int wpInTileY = wayPointBoundInTile.y;

				final int worldPixelWayPointX = worldPixelTileX + wpInTileX;
				final int worldPixelWayPointY = worldPixelTileY + wpInTileY;

				final int mouseInTileX = worldPixelMouseX - worldPixelWayPointX;
				final int mouseInTileY = worldPixelMouseY - worldPixelWayPointY;

				wpIndex++;

				// check if mouse is within a way point bound (image)
				if (mouseInTileX >= 0
						&& mouseInTileX <= wayPointBoundInTile.width
						&& mouseInTileY >= 0
						&& mouseInTileY <= wayPointBoundInTile.height) {

					final TourWayPoint hoveredTwp = allTileTwp.get(wpIndex);

					if (requestedTwp != null && requestedTwp != hoveredTwp) {
						// this is not the requested way point
						continue;
					}

					final int devWayPointX = worldPixelWayPointX - worldPixelTopLeftViewPort.x;
					final int devWayPointY = worldPixelWayPointY - worldPixelTopLeftViewPort.y;

//					_photo = hoveredTwp;

					return new HoveredAreaContext(
							this,
							hoveredTwp,
							devWayPointX,
							devWayPointY,
							wayPointBoundInTile.width,
							wayPointBoundInTile.height);
				}
			}
		}

		_photo = null;

		return null;
	}

	@Override
	public void hide() {}

	@Override
	public void paint(final GC gc, final Rectangle rectangle) {
		// painting is done in TourPainter
	}

	@Override
	public boolean setHoveredLocation(final int x, final int y) {
		// this method is not used in this tool tip provider, the method getHoveredContext() is used instead
		return false;
	}

	@Override
	public void setTourToolTip(final TourToolTip tourToolTip) {

		// this feature is not yet used in this tool tip provider

//		_tourToolTip = tourToolTip;
	}

	@Override
	public void show(final Point point) {}

}
