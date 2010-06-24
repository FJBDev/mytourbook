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
package de.byteholder.geoclipse.preferences;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionRefreshOfflineInfoSelected extends Action {

	private PrefPageMapProviders	_prefPageMapFactories;

	public ActionRefreshOfflineInfoSelected(final PrefPageMapProviders prefPageMapFactories) {

		setToolTipText(Messages.Pref_Map_Button_RefreshTileInfoSelected_Tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.App_Image_RefreshSelected));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.App_Image_RefreshSelected_Disabled));

		_prefPageMapFactories = prefPageMapFactories;
	}

	@Override
	public void run() {
		_prefPageMapFactories.actionRefreshOfflineInfo(true);
	}

}
