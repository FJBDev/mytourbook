/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProviderTourMonth extends DataProvider {

	private static DataProviderTourMonth	_instance;

	private TourDataMonth					_tourMonthData;

	private DataProviderTourMonth() {}

	public static DataProviderTourMonth getInstance() {
		if (_instance == null) {
			_instance = new DataProviderTourMonth();
		}
		return _instance;
	}

	TourDataMonth getMonthData(	final TourPerson person,
								final TourTypeFilter tourTypeFilter,
								final int lastYear,
								final int numberOfYears,
								final boolean refreshData) {

		/*
		 * check if the required data are already loaded
		 */
		if (_activePerson == person
				&& _activeTourTypeFilter == tourTypeFilter
				&& lastYear == _lastYear
				&& numberOfYears == _numberOfYears
				&& refreshData == false) {
			return _tourMonthData;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;
		_lastYear = lastYear;
		_numberOfYears = numberOfYears;

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		_tourMonthData = new TourDataMonth();
		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT " // //$NON-NLS-1$
				+ "startYear," // 					1 //$NON-NLS-1$
				+ "startMonth," // 					2 //$NON-NLS-1$
				+ "SUM(tourDistance)," //			3 //$NON-NLS-1$
				+ "SUM(tourAltUp)," // 				4 //$NON-NLS-1$
				+ "SUM(CASE WHEN tourDrivingTime > 0 THEN tourDrivingTime ELSE tourRecordingTime END)," // 5 //$NON-NLS-1$
				+ "SUM(tourRecordingTime)," // 		6 //$NON-NLS-1$
				+ "SUM(tourDrivingTime)," // 		7 //$NON-NLS-1$
				+ "tourType_typeId" // 				8 //$NON-NLS-1$

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$

				+ (" WHERE startYear IN (" + getYearList(lastYear, numberOfYears) + ")") //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()

				+ (" GROUP BY startYear, startMonth, tourType_typeId") //$NON-NLS-1$
				+ (" ORDER BY startYear, startMonth"); //$NON-NLS-1$

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		int serieLength = colorOffset + tourTypes.length;
		serieLength = serieLength == 0 ? 1 : serieLength;
		final int valueLength = 12 * numberOfYears;

		try {

			final int[][] dbDistance = new int[serieLength][valueLength];
			final int[][] dbAltitude = new int[serieLength][valueLength];
			final int[][] dbDurationTime = new int[serieLength][valueLength];

			final int[][] dbRecordingTime = new int[serieLength][valueLength];
			final int[][] dbDrivingTime = new int[serieLength][valueLength];
			final int[][] dbBreakTime = new int[serieLength][valueLength];

			final long[][] dbTypeIds = new long[serieLength][valueLength];

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int resultYear = result.getInt(1);
				final int resultMonth = result.getInt(2);

				final int yearIndex = numberOfYears - (lastYear - resultYear + 1);
				final int monthIndex = (resultMonth - 1) + yearIndex * 12;

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */
				int colorIndex = 0;

				final Long dbTypeIdObject = (Long) result.getObject(8);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(8);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = colorOffset + typeIndex;
							break;
						}
					}
				}

				dbTypeIds[colorIndex][monthIndex] = dbTypeIdObject == null ? -1 : dbTypeIdObject;
				dbDistance[colorIndex][monthIndex] = (int) (result.getInt(3) / UI.UNIT_VALUE_DISTANCE);
				dbAltitude[colorIndex][monthIndex] = (int) (result.getInt(4) / UI.UNIT_VALUE_ALTITUDE);
				dbDurationTime[colorIndex][monthIndex] = result.getInt(5);

				final int recordingTime = result.getInt(6);
				final int drivingTime = result.getInt(7);

				dbRecordingTime[colorIndex][monthIndex] = recordingTime;
				dbDrivingTime[colorIndex][monthIndex] = drivingTime;
				dbBreakTime[colorIndex][monthIndex] = recordingTime - drivingTime;
			}

			conn.close();

			_tourMonthData.fTypeIds = dbTypeIds;

			_tourMonthData.fDistanceLow = new int[serieLength][valueLength];
			_tourMonthData.fAltitudeLow = new int[serieLength][valueLength];
			_tourMonthData.fTimeLow = new int[serieLength][valueLength];

			_tourMonthData.fDistanceHigh = dbDistance;
			_tourMonthData.fAltitudeHigh = dbAltitude;
			_tourMonthData.fTimeHigh = dbDurationTime;

			_tourMonthData.fRecordingTime = dbRecordingTime;
			_tourMonthData.fDrivingTime = dbDrivingTime;
			_tourMonthData.fBreakTime = dbBreakTime;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourMonthData;
	}

}
