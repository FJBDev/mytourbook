/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourMarker;

public interface ITourMarkerUpdater {

	/**
	 * This is called when a {@link TourMarker} is deleted.
	 * 
	 * @param tourMarker
	 */
	public void deleteTourMarker(TourMarker tourMarker);

	/**
	 * This is called when a {@link TourMarker} is modified and the UI needs to be updated.
	 * 
	 * @param tourMarker
	 */
	public void updateModifiedTourMarker(TourMarker tourMarker);

}
