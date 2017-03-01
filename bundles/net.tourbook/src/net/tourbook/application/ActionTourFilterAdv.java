/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.tour.filter.ActionToolbarSlideoutAdv;
import net.tourbook.tour.filter.SlideoutTourFilterAdv;
import net.tourbook.tour.filter.TourFilterManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.ToolBar;

public class ActionTourFilterAdv extends ActionToolbarSlideoutAdv {

// SET_FORMATTING_OFF

	private static final ImageDescriptor	_actionImageDescriptor			= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter);
	private static final ImageDescriptor	_actionImageDisabledDescriptor	= TourbookPlugin.getImageDescriptor(Messages.Image__TourFilter_Disabled);

	private static final IDialogSettings	_state	= TourbookPlugin.getState("TourFilter");//$NON-NLS-1$
	
// SET_FORMATTING_ON

	public ActionTourFilterAdv() {

		super(_actionImageDescriptor, _actionImageDisabledDescriptor);

		isToggleAction = true;
		notSelectedTooltip = Messages.Tour_Filter_Action_Tooltip;
	}

	@Override
	protected SlideoutTourFilterAdv createSlideout(final ToolBar toolbar) {

		return new SlideoutTourFilterAdv(toolbar, toolbar, _state);
	}

	@Override
	protected void onSelect() {

		super.onSelect();

		// update tour filter
		TourFilterManager.setSelection(getSelection());
	}

}
