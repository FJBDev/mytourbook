/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionReloadFailedMapImages extends Action {

	private MapView	fMapView;

	public ActionReloadFailedMapImages(final MapView mapView) {

		super(null, AS_PUSH_BUTTON);

		fMapView = mapView;

		setText(Messages.map_action_reload_map);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_reload_map));
	}

	@Override
	public void run() {
		fMapView.actionReloadFailedMapImages();
	}

}
