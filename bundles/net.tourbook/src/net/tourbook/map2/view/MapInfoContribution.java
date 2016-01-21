/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class MapInfoContribution extends WorkbenchWindowControlContribution {

	private MapInfoManager	fMapInfoManager;
	private MapInfoControl	fInfoWidget;

	@Override
	protected Control createControl(final Composite parent) {

		if (fMapInfoManager == null) {
			fMapInfoManager = MapInfoManager.getInstance();
		}
 
		fInfoWidget = new MapInfoControl(parent, getOrientation());

		updateUI();

		return fInfoWidget;
	}

	private void updateUI() {
		fMapInfoManager.setInfoWidget(fInfoWidget);
	}

}
