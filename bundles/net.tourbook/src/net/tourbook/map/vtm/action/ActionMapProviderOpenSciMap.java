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
package net.tourbook.map.vtm.action;

import net.tourbook.map.vtm.MapVtmView;

import org.eclipse.jface.action.Action;

public class ActionMapProviderOpenSciMap extends Action {

	private final MapVtmView _mapView;

	public ActionMapProviderOpenSciMap(final MapVtmView mapVtmView) {

		super("OpenScience Map", AS_CHECK_BOX);

		_mapView = mapVtmView;
	}

	@Override
	public void run() {
		_mapView.action_MapProvider_OpenScience();
	}

}
