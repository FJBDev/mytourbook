/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.photo.PhotoToolTipUI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Photo tooltip for a tour chart.
 * 
 * @author Wolfgang Schramm, created 1.8.2012
 */
public class ChartPhotoToolTip extends PhotoToolTipUI {

	private ArrayList<ChartPhoto>	_hoveredPhotos	= new ArrayList<ChartPhoto>();

	private int						_devXHoveredPhoto;
	private int						_devYHoveredPhoto;

	/**
	 * This counter is incremented when 0 photos can be displayed with the hovered mouse. Counter is
	 * not incremented when subsequent different number of photos can be displayed until 0 photos
	 * can be displayed. This counter is used to not hide photo tooltip when number of photos
	 * changes.
	 */
	private int						_hideCounter;

	/*
	 * UI controls
	 */
	private TourChart				_tourChart;

	public ChartPhotoToolTip(final TourChart tourChart) {

		super(tourChart);

		_tourChart = tourChart;
	}

	@Override
	public int getHideCounter() {
		return _hideCounter;
	}

	@Override
	public ArrayList<ChartPhoto> getHoveredPhotos() {
		return _hoveredPhotos;
	}

	@Override
	public Point getLocation(final Point tipSize, final Event event) {

		final int margin = 10;

		final int itemPosX = _tourChart.getLeftAxisWidth() + _devXHoveredPhoto;
		final int itemPosY = _devYHoveredPhoto;

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;
		final int tipWidth2 = tipWidth / 2;

		// center tooltip horizontally
		final int ttPosX = itemPosX - tipWidth2;

		int ttPosY = itemPosY - tipHeight - margin;

		// check chart bottom
		final int chartHeight = _tourChart.getBounds().height;
		if (ttPosY > chartHeight) {
			// tooltip is below the gallery bottom
			ttPosY = chartHeight + margin;
		}

		// check display height
		final Rectangle displayBounds = _tourChart.getDisplay().getBounds();
		final Point chartDisplay = _tourChart.toDisplay(0, 0);

		if (chartDisplay.y + ttPosY + tipHeight > displayBounds.height) {
			ttPosY = itemPosY - tipHeight - margin;
		}

		// check display top
		final int aboveChart = -tipHeight - margin;
		if (ttPosY < aboveChart) {
			ttPosY = aboveChart;
		}

		return _tourChart.toDisplay(ttPosX, ttPosY);
	}

	Shell getShell() {
		return getToolTipShell();
	}

	@Override
	public void incrementHideCounter() {
		_hideCounter++;
	}

	private void reset() {

		_hideCounter++;

		hide();
	}

	@Override
	protected void restoreState(final IDialogSettings state) {
		super.restoreState(state);
	}

	@Override
	protected void saveState(final IDialogSettings state) {
		super.saveState(state);
	}

	public void show(	final ChartLayerPhoto photoLayer,
						final int hoveredValueIndex,
						final Point devPositionHoveredValue,
						final int devXMouseMove,
						final int devYMouseMove) {

		_hoveredPhotos.clear();

		final ArrayList<ChartPhoto> chartPhotos = photoLayer.getChartPhotos();
		final Point[] photoPositions = photoLayer.getPhotoPositions();

		final int hoveredXPos = devXMouseMove;

		final int positionMargin = 20;

		final int hoveredXPosLeft = hoveredXPos - positionMargin;
		final int hoveredXPosRight = hoveredXPos + positionMargin;

		_devXHoveredPhoto = Integer.MAX_VALUE;
		_devYHoveredPhoto = Integer.MAX_VALUE;

		// get all photos which are visible for the hovered value point and beside it
		for (int photoIndex = 0; photoIndex < photoPositions.length; photoIndex++) {

			final Point photoPos = photoPositions[photoIndex];
			if (photoPos == null) {
				// photo is not visible
				continue;
			}

			final int photoPosX = photoPos.x;

			if (photoPosX > hoveredXPosLeft && photoPosX < hoveredXPosRight) {

				// photo is within current hovered area

				final ChartPhoto chartPhoto = chartPhotos.get(photoIndex);

				// position tooltip above all posible photos
				if (photoPos.y < _devYHoveredPhoto) {
					_devXHoveredPhoto = photoPos.x;
					_devYHoveredPhoto = photoPos.y;
				}

				_hoveredPhotos.add(chartPhoto);
			}
		}

		if (_hoveredPhotos.size() == 0) {

			_hideCounter++;

			reset();

		} else {

			showPhotoToolTip(devPositionHoveredValue);
		}
	}


}
