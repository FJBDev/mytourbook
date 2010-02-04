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
package net.tourbook.ui.views.tourBook;

import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.ui.TreeViewerItem;

public class TVITourBookTour extends TVITourBookItem {

	long					tourId;
	long					tourTypeId;

	long					colStartDistance;
	short					colTimeInterval;

	HashSet<Long>			sqlTagIds;
	HashSet<Long>			sqlMarkerIds;

	/**
	 * id's for the tags or <code>null</code> when tags are not available
	 */
	private ArrayList<Long>	fTagIds;

	/**
	 * id's for the markers or <code>null</code> when markers are not available
	 */
	private ArrayList<Long>	_markerIds;

	public TVITourBookTour(final TourBookView view, final TreeViewerItem parentItem) {

		super(view);

		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {}

	public long getColumnStartDistance() {
		return colStartDistance;
	}

	public short getColumnTimeInterval() {
		return colTimeInterval;
	}

	public ArrayList<Long> getMarkerIds() {
		if (sqlMarkerIds != null && _markerIds == null) {
			_markerIds = new ArrayList<Long>(sqlMarkerIds);
		}
		return _markerIds;
	}

	public ArrayList<Long> getTagIds() {
		if (sqlTagIds != null && fTagIds == null) {
			fTagIds = new ArrayList<Long>(sqlTagIds);
		}
		return fTagIds;
	}

	@Override
	public Long getTourId() {
		return tourId;
	}

	public long getTourTypeId() {
		return tourTypeId;
	}

	/**
	 * tour items do not have children
	 */
	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	protected void remove() {}

	public void setMarkerIds(final HashSet<Long> markerIds) {
		sqlMarkerIds = markerIds;
	}

	public void setTagIds(final HashSet<Long> tagIds) {
		sqlTagIds = tagIds;
	}

}
