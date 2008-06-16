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
package net.tourbook.ui.views.tourBook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;

public class TVITourBookMonth extends TourBookTreeViewerItem {

	public TVITourBookMonth(final TourBookView view,
							final TourBookTreeViewerItem parentItem,
							final int year,
							final int month) {

		super(view);

		setParentItem(parentItem);

		fTourYear = year;
		fTourMonth = fFirstColumn = month;
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the month item, these are tour items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final TVITourBookYear yearItem = (TVITourBookYear) (getParentItem());

		final String sqlString = "SELECT " //		//$NON-NLS-1$

				+ "Tdata.STARTYear," //			1	//$NON-NLS-1$
				+ "Tdata.STARTMonth," //		2	//$NON-NLS-1$
				+ "Tdata.STARTDay," //			3	//$NON-NLS-1$
				+ "Tdata.TOURDISTANCE," //		4	//$NON-NLS-1$
				+ "Tdata.TOURRECORDINGTIME," //	5	//$NON-NLS-1$
				+ "Tdata.TOURDRIVINGTIME," //	6	//$NON-NLS-1$
				+ "Tdata.TOURALTUP," //			7	//$NON-NLS-1$
				+ "Tdata.TOURALTDOWN," //		8	//$NON-NLS-1$
				+ "Tdata.startDistance," //		9	//$NON-NLS-1$
				+ "Tdata.tourID," //			10	//$NON-NLS-1$
				+ "Tdata.tourType_typeId," //	11	//$NON-NLS-1$
				+ "Tdata.tourTitle," //			12	//$NON-NLS-1$
				+ "Tdata.deviceTimeInterval," //13	//$NON-NLS-1$
				+ "Tdata.maxSpeed," //			14	//$NON-NLS-1$
				+ "Tdata.maxAltitude," //		15	//$NON-NLS-1$
				+ "Tdata.maxPulse," //			16	//$NON-NLS-1$
				+ "Tdata.avgPulse," //			17	//$NON-NLS-1$
				+ "Tdata.avgCadence," //		18	//$NON-NLS-1$
				+ "Tdata.avgTemperature," //	19	//$NON-NLS-1$

				+ ("jTdataTtag.TourTag_tagId")//20 

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " Tdata" + UI.NEW_LINE) //			//$NON-NLS-1$ //$NON-NLS-2$

				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag")
				+ (" ON Tdata.tourID = jTdataTtag.TourData_tourId")

//				left outer join TourData_TourTag tourtags6_ on tourdata0_.tourId=tourtags6_.tourData_tourId 

				+ (" WHERE Tdata.STARTYEAR = ?")//				//$NON-NLS-1$
				+ (" AND Tdata.STARTMONTH = ?")//					//$NON-NLS-1$
				+ sqlTourPersonId()
				+ sqlTourTypeId()
				+ " ORDER BY Tdata.StartDay, Tdata.StartHour, Tdata.StartMinute"; //$NON-NLS-1$

//		sb.append(" Tdata.TourAltDown");//				// 
//
//		sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");
//
//		sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " Tdata ON");
//		sb.append(" jTdataTtag.TourData_tourId=Tdata.tourId ");
//
//		sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");// + tagId);

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			statement.setInt(1, yearItem.fTourYear);
			statement.setInt(2, fTourMonth);

//			final long time = System.currentTimeMillis();
//			System.out.println(System.currentTimeMillis() - time + "ms");

			long lastTourId = -1;

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(10);

				if (tourId == lastTourId) {

					// get tags from outer join
					
					
				} else {

					// new tour is in the resultset
					final TVITourBookTour tourItem = new TVITourBookTour(fView, this);
					children.add(tourItem);

					tourItem.fTourId = tourId;

					tourItem.fTourYear = result.getInt(1);
					tourItem.fTourMonth = result.getInt(2);
					tourItem.fTourDay = tourItem.fFirstColumn = result.getInt(3);

					fCalendar.set(tourItem.fTourYear, tourItem.fTourMonth - 1, tourItem.fTourDay);
					tourItem.fTourDate = fCalendar.getTimeInMillis();

					tourItem.fColumnDistance = result.getLong(4);
					tourItem.fColumnRecordingTime = result.getLong(5);
					tourItem.fColumnDrivingTime = result.getLong(6);
					tourItem.fColumnAltitudeUp = result.getLong(7);
					tourItem.fColumnAltitudeDown = result.getLong(8);

					tourItem.fColumnStartDistance = result.getLong(9);

					final Object tourTypeId = result.getObject(11);
					tourItem.fTourTypeId = (tourTypeId == null ? //
							TourDatabase.ENTITY_IS_NOT_SAVED
							: (Long) tourTypeId);

					tourItem.fTourTitle = result.getString(12);
					tourItem.fColumnTimeInterval = result.getShort(13);
					tourItem.fColumnMaxSpeed = result.getFloat(14);

					if (tourItem.fColumnDrivingTime != 0) {
						tourItem.fColumnAvgSpeed = (float) tourItem.fColumnDistance
								/ (float) tourItem.fColumnDrivingTime
								* 3.6f;
					}

					tourItem.fColumnMaxAltitude = result.getLong(15);
					tourItem.fColumnMaxPulse = result.getLong(16);
					tourItem.fColumnAvgPulse = result.getLong(17);
					tourItem.fColumnAvgCadence = result.getLong(18);
					tourItem.fColumnAvgTemperature = result.getLong(19);
					
					final Object resultTagId = result.getObject(20);
					if (resultTagId instanceof Long) {
						tourItem.fTagIds = new ArrayList<Long>();
					}
				}

				System.out.println(result.getObject(10) + //
						("\t" + result.getObject(20))
				//
				);

				lastTourId = tourId;
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void remove() {}

}
