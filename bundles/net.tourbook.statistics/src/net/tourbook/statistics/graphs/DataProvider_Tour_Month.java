/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Month extends DataProvider {

	private static DataProvider_Tour_Month	_instance;

	private TourData_Month					_tourMonthData;

	private DataProvider_Tour_Month() {}

	public static DataProvider_Tour_Month getInstance() {
		if (_instance == null) {
			_instance = new DataProvider_Tour_Month();
		}
		return _instance;
	}

	TourData_Month getMonthData(final TourPerson person,
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
		final ArrayList<TourType> allTourTypes = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = allTourTypes.toArray(new TourType[allTourTypes.size()]);

		_tourMonthData = new TourData_Month();
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

		int numTourTypes = colorOffset + tourTypes.length;
		numTourTypes = numTourTypes == 0 ? 1 : numTourTypes;
		final int numMonths = 12 * numberOfYears;

		try {

			final float[][] dbAltitude = new float[numTourTypes][numMonths];
			final float[][] dbDistance = new float[numTourTypes][numMonths];

			final int[][] dbDurationTime = new int[numTourTypes][numMonths];
			final int[][] dbRecordingTime = new int[numTourTypes][numMonths];
			final int[][] dbDrivingTime = new int[numTourTypes][numMonths];
			final int[][] dbBreakTime = new int[numTourTypes][numMonths];

			final long[][] dbTypeIds = new long[numTourTypes][numMonths];

			final long[] tourTypeSum = new long[numTourTypes];

			final long[] usedTourTypeIds = new long[numTourTypes];
			Arrays.fill(usedTourTypeIds, -1);

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int year = result.getInt(1);
				final int month = result.getInt(2);
				final int distance = (int) (result.getInt(3) / UI.UNIT_VALUE_DISTANCE);
				final int altitude = (int) (result.getInt(4) / UI.UNIT_VALUE_ALTITUDE);
				final int duration = result.getInt(5);
				final int recordingTime = result.getInt(6);
				final int drivingTime = result.getInt(7);
				final Long tourTypeIdObject = (Long) result.getObject(8);

				final int yearIndex = numberOfYears - (lastYear - year + 1);
				final int monthIndex = (month - 1) + yearIndex * 12;

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */
				int colorIndex = 0;

				if (tourTypeIdObject != null) {

					final long dbTypeId = tourTypeIdObject;

					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {

						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {

							colorIndex = colorOffset + typeIndex;
							break;
						}
					}
				}

				final long typeId = tourTypeIdObject == null ? -1 : tourTypeIdObject;

				dbTypeIds[colorIndex][monthIndex] = typeId;

				dbAltitude[colorIndex][monthIndex] = altitude;
				dbDistance[colorIndex][monthIndex] = distance;
				dbDurationTime[colorIndex][monthIndex] = duration;

				dbRecordingTime[colorIndex][monthIndex] = recordingTime;
				dbDrivingTime[colorIndex][monthIndex] = drivingTime;
				dbBreakTime[colorIndex][monthIndex] = recordingTime - drivingTime;

				usedTourTypeIds[colorIndex] = typeId;
				tourTypeSum[colorIndex] += distance + altitude + recordingTime;
			}

			conn.close();

			/*
			 * Remove not used tour types
			 */
			final ArrayList<Object> typeIdsWithData = new ArrayList<Object>();

			final ArrayList<Object> altitudeWithData = new ArrayList<Object>();
			final ArrayList<Object> distanceWithData = new ArrayList<Object>();
			final ArrayList<Object> durationWithData = new ArrayList<Object>();
			final ArrayList<Object> recordingTimeWithData = new ArrayList<Object>();
			final ArrayList<Object> drivingTimeWithData = new ArrayList<Object>();
			final ArrayList<Object> breakTimeWithData = new ArrayList<Object>();

			for (int tourTypeIndex = 0; tourTypeIndex < tourTypeSum.length; tourTypeIndex++) {

				final long summary = tourTypeSum[tourTypeIndex];

				if (summary > 0) {

					typeIdsWithData.add(dbTypeIds[tourTypeIndex]);

					altitudeWithData.add(dbAltitude[tourTypeIndex]);
					distanceWithData.add(dbDistance[tourTypeIndex]);
					durationWithData.add(dbDurationTime[tourTypeIndex]);

					recordingTimeWithData.add(dbRecordingTime[tourTypeIndex]);
					drivingTimeWithData.add(dbDrivingTime[tourTypeIndex]);
					breakTimeWithData.add(dbBreakTime[tourTypeIndex]);
				}
			}

			final int numUsedTourTypes = typeIdsWithData.size();

			if (numUsedTourTypes == 0) {

				// there are NO data, create dummy data that the UI do not fail

				_tourMonthData.typeIds = new long[1][1];
				_tourMonthData.usedTourTypeIds = new long[] { -1 };

				_tourMonthData.altitudeLow = new float[1][numMonths];
				_tourMonthData.distanceLow = new float[1][numMonths];
				_tourMonthData.setDurationTimeLow(new int[1][numMonths]);

				_tourMonthData.altitudeHigh = new float[1][numMonths];
				_tourMonthData.distanceHigh = new float[1][numMonths];
				_tourMonthData.setDurationTimeHigh(new int[1][numMonths]);

				_tourMonthData.recordingTime = new int[1][numMonths];
				_tourMonthData.drivingTime = new int[1][numMonths];
				_tourMonthData.breakTime = new int[1][numMonths];

			} else {

				final long[][] usedTypeIds = new long[numUsedTourTypes][];

				final float[][] usedAltitude = new float[numUsedTourTypes][];
				final float[][] usedDistance = new float[numUsedTourTypes][];
				final int[][] usedDuration = new int[numUsedTourTypes][];
				final int[][] usedRecordingTime = new int[numUsedTourTypes][];
				final int[][] usedDrivingTime = new int[numUsedTourTypes][];
				final int[][] usedBreakTime = new int[numUsedTourTypes][];

				for (int index = 0; index < numUsedTourTypes; index++) {

					usedTypeIds[index] = (long[]) typeIdsWithData.get(index);

					usedAltitude[index] = (float[]) altitudeWithData.get(index);
					usedDistance[index] = (float[]) distanceWithData.get(index);

					usedDuration[index] = (int[]) durationWithData.get(index);
					usedRecordingTime[index] = (int[]) recordingTimeWithData.get(index);
					usedDrivingTime[index] = (int[]) drivingTimeWithData.get(index);
					usedBreakTime[index] = (int[]) breakTimeWithData.get(index);
				}

				_tourMonthData.typeIds = usedTypeIds;
				_tourMonthData.usedTourTypeIds = usedTourTypeIds;

				_tourMonthData.altitudeLow = new float[numUsedTourTypes][numMonths];
				_tourMonthData.distanceLow = new float[numUsedTourTypes][numMonths];
				_tourMonthData.setDurationTimeLow(new int[numUsedTourTypes][numMonths]);

				_tourMonthData.altitudeHigh = usedAltitude;
				_tourMonthData.distanceHigh = usedDistance;
				_tourMonthData.setDurationTimeHigh(usedDuration);

				_tourMonthData.recordingTime = usedRecordingTime;
				_tourMonthData.drivingTime = usedDrivingTime;
				_tourMonthData.breakTime = usedBreakTime;
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourMonthData;
	}

}
