/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.views.SlideoutTourBlogMarker;
import net.tourbook.ui.views.TourBlogView;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourBlogMarker extends ContributionItem {

	private static final String		IMAGE_EDIT_TOUR_MARKER	= Messages.Image__edit_tour_marker;

	private IDialogSettings			_state					= TourbookPlugin.getState(//
																	getClass().getSimpleName());

	private ToolBar					_toolBar;
	private ToolItem				_actionToolItem;

	private TourBlogView			_tourBlogView;
	private SlideoutTourBlogMarker	_slideoutTourBlogMarker;

	/*
	 * UI controls
	 */
	private Control					_parent;

	private Image					_actionImage;

	public ActionTourBlogMarker(final TourBlogView tourBlogView, final Control parent) {

		_tourBlogView = tourBlogView;
		_parent = parent;

		_actionImage = TourbookPlugin.getImageDescriptor(IMAGE_EDIT_TOUR_MARKER).createImage();
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionToolItem == null && toolbar != null) {

			// action is not yet created

			_toolBar = toolbar;

			toolbar.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(final DisposeEvent e) {
					onDispose();
				}
			});

			toolbar.addMouseMoveListener(new MouseMoveListener() {
				public void mouseMove(final MouseEvent e) {

					final Point mousePosition = new Point(e.x, e.y);
					final ToolItem hoveredItem = toolbar.getItem(mousePosition);

					onMouseMove(hoveredItem, e);
				}
			});

			_actionToolItem = new ToolItem(toolbar, SWT.PUSH);

			// !!! image must be set before enable state is set
			_actionToolItem.setImage(_actionImage);

			_actionToolItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelect();
				}
			});

			_slideoutTourBlogMarker = new SlideoutTourBlogMarker(_parent, _toolBar, _state, _tourBlogView);
		}
	}

	private void onDispose() {

		_actionImage.dispose();

		_actionToolItem.dispose();
		_actionToolItem = null;
	}

	private void onMouseMove(final ToolItem hoveredItem, final MouseEvent mouseEvent) {

		final boolean isToolItemHovered = hoveredItem == _actionToolItem;

		Rectangle itemBounds = null;

		if (isToolItemHovered) {

			itemBounds = hoveredItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openSlideout(itemBounds, true);
		}
	}

	private void onSelect() {

		if (_slideoutTourBlogMarker.isToolTipVisible() == false) {

			final Rectangle itemBounds = _actionToolItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openSlideout(itemBounds, false);

		} else {

			_slideoutTourBlogMarker.close();
		}
	}

	private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

		_slideoutTourBlogMarker.open(itemBounds, isOpenDelayed);
	}

}
