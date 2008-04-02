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

package net.tourbook.statistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class ProviderTourYear extends DataProvider {

	private static ProviderTourYear	fInstance;

	private TourDataYear			fTourDataYear;

	private ProviderTourYear() {}

	public static ProviderTourYear getInstance() {
		if (fInstance == null) {
			fInstance = new ProviderTourYear();
		}
		return fInstance;
	}

	TourDataYear getYearData(	final TourPerson person,
								final TourTypeFilter tourTypeFilter,
								final int lastYear,
								final int numberOfYears,
								final boolean refreshData) {

		/*
		 * check if the required data are already loaded
		 */
		if (fActivePerson == person
				&& fActiveTourTypeFilter == tourTypeFilter
				&& lastYear == fLastYear
				&& numberOfYears == fNumberOfYears
				&& refreshData == false) {

			return fTourDataYear;
		}

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;
		fLastYear = lastYear;
		fNumberOfYears = numberOfYears;

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getTourTypes();
		final TourType[] allTourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		fTourDataYear = new TourDataYear();

		final String sqlString = //
		"SELECT " // //$NON-NLS-1$
				+ "StartYear				, " // 		1 //$NON-NLS-1$
				+ "SUM(TOURDISTANCE)		, " // 		2 //$NON-NLS-1$
				+ "SUM(TOURALTUP)			, " // 		3 //$NON-NLS-1$
				+ "SUM(CASE WHEN tourDrivingTime > 0 THEN tourDrivingTime ELSE tourRecordingTime END)," // 4 //$NON-NLS-1$
				+ "tourType_typeId 			\n" // 		5 //$NON-NLS-1$
				//
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE STARTYEAR IN (" + getYearList(lastYear, numberOfYears) + ")") //$NON-NLS-1$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" GROUP BY STARTYEAR, tourType_typeId") //$NON-NLS-1$
				+ (" ORDER BY STARTYEAR"); //$NON-NLS-1$

		final int tourTypeSerieLength = allTourTypes.length + StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		final int valueLength = numberOfYears;

		try {

			final int[][] dbDistance = new int[tourTypeSerieLength][valueLength];
			final int[][] dbAltitude = new int[tourTypeSerieLength][valueLength];
			final int[][] dbTime = new int[tourTypeSerieLength][valueLength];
			final long[][] dbTourTypeIds = new long[tourTypeSerieLength][valueLength];

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				final int resultYear = result.getInt(1);
				final int yearIndex = numberOfYears - (lastYear - resultYear + 1);

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */

				// set default color index
				int colorIndex = 0;

				// get colorIndex from the type id
				final Long dbTourTypeIdObject = (Long) result.getObject(5);
				if (dbTourTypeIdObject != null) {
					final long dbTypeId = result.getLong(5);
					for (int typeIndex = 0; typeIndex < allTourTypes.length; typeIndex++) {
						if (dbTypeId == allTourTypes[typeIndex].getTypeId()) {
							colorIndex = typeIndex + StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
							break;
						}
					}
				}

				dbTourTypeIds[colorIndex][yearIndex] = dbTourTypeIdObject == null ? -1 : dbTourTypeIdObject;
				dbDistance[colorIndex][yearIndex] = (int) (result.getInt(2) / 1000 / UI.UNIT_VALUE_DISTANCE);
				dbAltitude[colorIndex][yearIndex] = (int) (result.getInt(3) / UI.UNIT_VALUE_ALTITUDE);
				dbTime[colorIndex][yearIndex] = result.getInt(4);
			}

			conn.close();

			int[] years = new int[fNumberOfYears];
			int yearIndex = 0;
			for (int currentYear = fLastYear - fNumberOfYears + 1; currentYear <= fLastYear; currentYear++) {
				years[yearIndex++] = currentYear;
			}

			fTourDataYear.fTypeIds = dbTourTypeIds;
			fTourDataYear.activeTourTypes = tourTypeFilter.getActiveTourTypes(tourTypeList);
			fTourDataYear.years = years;

			fTourDataYear.fDistanceLow = new int[tourTypeSerieLength][valueLength];
			fTourDataYear.fAltitudeLow = new int[tourTypeSerieLength][valueLength];
			fTourDataYear.fTimeLow = new int[tourTypeSerieLength][valueLength];

			fTourDataYear.fDistanceHigh = dbDistance;
			fTourDataYear.fAltitudeHigh = dbAltitude;
			fTourDataYear.fTimeHigh = dbTime;

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return fTourDataYear;
	}
}
