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
package net.tourbook.ui.views.tourTag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;

import org.joda.time.DateTime;

public class TVITagViewTour extends TVITagViewItem {

	public static final String	SQL_TOUR_COLUMNS	= ""//
															+ "startYear," //			0
															+ "startMonth," //			1
															+ "startDay," //			2
															+ "tourTitle," //			3
															+ "tourType_typeId," //		4
															+ "deviceTimeInterval," //	5
															+ "startDistance," //		6
															//
															+ SQL_TOUR_SUM_COLUMNS; //	7

	private TVITagViewItem		fParentItem;

	long						tourId;

	DateTime					tourDate;
	int							tourDay;

	String						tourTitle;
	long						tourTypeId;

	ArrayList<Long>				tagIds;

	public long					deviceStartDistance;
	public short				deviceTimeInterval;

	public TVITagViewTour(final TVITagViewItem parentItem) {
		fParentItem = parentItem;
	}

	@Override
	protected void fetchChildren() {}

	public TVITagViewItem getRootItem() {
		return fParentItem;
	}

	public void getTourColumnData(final ResultSet result, final Object resultTagId, final int startIndex)
			throws SQLException {

		final int tourYear = result.getInt(startIndex + 0);
		final int tourMonth = result.getInt(startIndex + 1);
		tourDay = result.getInt(startIndex + 2);
		tourDate = new DateTime(tourYear, tourMonth, tourDay, 0, 0, 0, 0);

		tourTitle = result.getString(startIndex + 3);

		final Object resultTourTypeId = result.getObject(startIndex + 4);
		tourTypeId = (resultTourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) resultTourTypeId);

		deviceTimeInterval = result.getShort(startIndex + 5);
		deviceStartDistance = result.getLong(startIndex + 6);

		getDefaultColumnData(result, startIndex + 7);

		if (resultTagId instanceof Long) {
			tagIds = new ArrayList<Long>();
			tagIds.add((Long) resultTagId);
		}
	}

	public long getTourId() {
		return tourId;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	protected void remove() {}

}
