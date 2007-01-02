/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views.tourMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

/**
 * TTI (TreeViewerItem) is used in the tree viewer TourMapView, it contains tree
 * items for reference tours
 */
public class TVITourMapYear extends TreeViewerItem {

	long	refId;
	int		year;

	/**
	 * @param parentItem
	 * @param refId
	 * @param year
	 */
	public TVITourMapYear(TreeViewerItem parentItem, long refId, int year) {

		this.setParentItem(parentItem);

		this.refId = refId;
		this.year = year;
	}

	protected void fetchChildren() {

		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		String sqlString = "SELECT "
				+ "tourDate, "
				+ "tourSpeed, "
				+ "comparedId, "
				+ "tourId , "
				+ "startIndex, "
				+ "endIndex, "
				+ "startYear \n"
				+ ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + " \n")
				+ ("WHERE refTourId=" + refId)
				+ " AND "
				+ ("startYear=" + year)
				+ " ORDER BY tourDate";

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				children.add(new TVTITourMapComparedTour(
						this,
						result.getDate(1),
						result.getFloat(2),
						result.getLong(3),
						result.getLong(4),
						result.getInt(5),
						result.getInt(6),
						refId));
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void remove() {

		// remove all children
		getUnfetchedChildren().clear();

		// remove this tour item from the parent
		getParentItem().getUnfetchedChildren().remove(this);
	}

	TVTITourMapReferenceTour getRefItem() {
		return (TVTITourMapReferenceTour) getParentItem();
	}

}
