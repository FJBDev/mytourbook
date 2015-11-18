/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourType;

public class DeviceImportLauncher implements Cloneable, Comparable<DeviceImportLauncher> {

	public String					description			= UI.EMPTY_STRING;
	public String					name				= UI.EMPTY_STRING;

	/**
	 * When <code>null</code> then the tour type is not set.
	 */
	public Enum<TourTypeConfig>		tourTypeConfig		= null;

	public TourType					oneTourType;

	public ArrayList<SpeedTourType>	speedTourTypes		= new ArrayList<>();

	/** Contains the image hash or 0 when an image is not displayed. */
	public int						imageHash;

	public int						imageWidth;

	private long					_id;

	/**
	 * Show/hid this launcher in the dashboard.
	 */
	public boolean					isShowInDashboard	= true;

	private static long				_idCreator;

	public DeviceImportLauncher() {

		_id = ++_idCreator;
	}

	@Override
	protected DeviceImportLauncher clone() {

		DeviceImportLauncher clonedObject = null;

		try {

			clonedObject = (DeviceImportLauncher) super.clone();

			clonedObject._id = ++_idCreator;

			clonedObject.speedTourTypes = new ArrayList<>();
			for (final SpeedTourType speedVertex : speedTourTypes) {
				clonedObject.speedTourTypes.add(speedVertex.clone());
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public int compareTo(final DeviceImportLauncher otherConfig) {

		return name.compareTo(otherConfig.name);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DeviceImportLauncher other = (DeviceImportLauncher) obj;
		if (_id != other._id) {
			return false;
		}
		return true;
	}

	/**
	 * @return Returns a unique id for this import tile.
	 */
	public long getId() {
		return _id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_id ^ (_id >>> 32));
		return result;
	}

	/**
	 * Setup data for the tour type config image.
	 */
	void setupItemImage() {

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			final int numVertices = speedTourTypes.size();

			imageHash = speedTourTypes.hashCode();
			imageWidth = numVertices * TourType.TOUR_TYPE_IMAGE_SIZE;

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			if (oneTourType == null) {

				imageHash = 0;
				imageWidth = 0;

			} else {

				imageHash = oneTourType.hashCode();
				imageWidth = TourType.TOUR_TYPE_IMAGE_SIZE;
			}

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			imageHash = 0;
			imageWidth = 0;
		}
	}

	@Override
	public String toString() {
		return "AutoImportLauncher ["
		//
				+ ("name=" + name + ", ")
				+ ("speedVertices=" + speedTourTypes + ", ")
				+ ("configType=" + tourTypeConfig + ", ")

				+ "]";
	}
}
