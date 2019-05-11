/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Day extends DataProvider {

   private static DataProvider_Tour_Day _instance;

   private TourData_Day                 _tourDayData;

   private DataProvider_Tour_Day() {}

   public static DataProvider_Tour_Day getInstance() {

      if (_instance == null) {
         _instance = new DataProvider_Tour_Day();
      }

      return _instance;
   }

   TourData_Day getDayData(final TourPerson person,
                           final TourTypeFilter tourTypeFilter,
                           final int lastYear,
                           final int numberOfYears,
                           final boolean refreshData) {

      // don't reload data which are already available
      if (person == _activePerson
            && tourTypeFilter == _activeTourTypeFilter
            && lastYear == _lastYear
            && numberOfYears == _numberOfYears
            && refreshData == false) {

         return _tourDayData;
      }

      _activePerson = person;
      _activeTourTypeFilter = tourTypeFilter;

      _lastYear = lastYear;
      _numberOfYears = numberOfYears;

      initYearNumbers();

      int colorOffset = 0;
      if (tourTypeFilter.showUndefinedTourTypes()) {
         colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
      }

      // get the tour types
      final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
      final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

// SET_FORMATTING_OFF

      final String sqlString = NL

            + "SELECT "                            + NL //        //$NON-NLS-1$

            + " TourId,"                           + NL //  1     //$NON-NLS-1$

            + " StartYear,"                        + NL //  2     //$NON-NLS-1$
            + " StartWeek,"                        + NL //  3     //$NON-NLS-1$
            + " TourStartTime,"                    + NL //  4     //$NON-NLS-1$
            + " TimeZoneId,"                       + NL //  5     //$NON-NLS-1$

            + " TourDrivingTime,"                  + NL //  6     //$NON-NLS-1$
            + " TourRecordingTime,"                + NL //  7     //$NON-NLS-1$

            + " TourDistance,"                     + NL //  8     //$NON-NLS-1$
            + " TourAltUp,"                        + NL //  9     //$NON-NLS-1$
            + " TourTitle,"                        + NL //  10    //$NON-NLS-1$
            + " TourDescription,"                  + NL //  11    //$NON-NLS-1$

            + " training_TrainingEffect,"          + NL //  12    //$NON-NLS-1$
            + " training_TrainingPerformance,"     + NL //  13    //$NON-NLS-1$

            + " TourType_typeId,"                  + NL //  14    //$NON-NLS-1$
            + " jTdataTtag.TourTag_tagId"          + NL //  15    //$NON-NLS-1$

            + NL

            + (" FROM " + TourDatabase.TABLE_TOUR_DATA + NL) //$NON-NLS-1$

            // get tag id's
            + (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
            + (" ON tourID = jTdataTtag.TourData_tourId" + NL) //$NON-NLS-1$

            + (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL) //$NON-NLS-1$ //$NON-NLS-2$
            + sqlFilter.getWhereClause()

            + (" ORDER BY TourStartTime" + NL + NL); //$NON-NLS-1$

// SET_FORMATTING_ON

      try {

         final TLongArrayList allTourIds = new TLongArrayList();

         final TIntArrayList allYears = new TIntArrayList();
         final TIntArrayList allMonths = new TIntArrayList();
         final TIntArrayList allYearsDOY = new TIntArrayList(); // DOY...Day Of Year

         final TIntArrayList allTourStartTime = new TIntArrayList();
         final TIntArrayList allTourEndTime = new TIntArrayList();
         final TIntArrayList allTourStartWeek = new TIntArrayList();
         final ArrayList<ZonedDateTime> allTourStartDateTime = new ArrayList<>();

         final TIntArrayList allTourDuration = new TIntArrayList();
         final TIntArrayList allTourRecordingTime = new TIntArrayList();
         final TIntArrayList allTourDrivingTime = new TIntArrayList();

         final TFloatArrayList allDistance = new TFloatArrayList();
         final TFloatArrayList allAvgSpeed = new TFloatArrayList();
         final TFloatArrayList allAvgPace = new TFloatArrayList();
         final TFloatArrayList allAltitudeUp = new TFloatArrayList();

         final TFloatArrayList allTrainingEffect = new TFloatArrayList();
         final TFloatArrayList allTrainingPerformance = new TFloatArrayList();

         final ArrayList<String> allTourTitle = new ArrayList<>();
         final ArrayList<String> allTourDescription = new ArrayList<>();

         final TLongArrayList allTypeIds = new TLongArrayList();
         final TIntArrayList allTypeColorIndex = new TIntArrayList();

         final HashMap<Long, ArrayList<Long>> allTagIds = new HashMap<>();

         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         final Connection conn = TourDatabase.getInstance().getConnection();

         final PreparedStatement statement = conn.prepareStatement(sqlString);
         sqlFilter.setParameters(statement, 1);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long dbTourId = result.getLong(1);
            final Object dbTagId = result.getObject(15);

            if (dbTourId == lastTourId) {

               // get additional tags from outer join

               if (dbTagId instanceof Long) {
                  tagIds.add((Long) dbTagId);
               }

            } else {

               // get first record from a tour

               final int dbTourYear = result.getShort(2);
               final int dbTourStartWeek = result.getInt(3);

               final long dbStartTimeMilli = result.getLong(4);
               final String dbTimeZoneId = result.getString(5);

               final int dbDrivingTime = result.getInt(6);
               final int dbRecordingTime = result.getInt(7);

               final float dbDistance = result.getFloat(8);
               final int dbAltitudeUp = result.getInt(9);

               final String dbTourTitle = result.getString(10);
               final String dbDescription = result.getString(11);

               final float trainingEffect = result.getFloat(12);
               final float trainingPerformance = result.getFloat(13);

               final Object dbTypeIdObject = result.getObject(14);

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbStartTimeMilli, dbTimeZoneId);
               final ZonedDateTime zonedStartDateTime = tourDateTime.tourZonedDateTime;

               // get number of days for the year, start with 0
               final int tourDOY = tourDateTime.tourZonedDateTime.get(ChronoField.DAY_OF_YEAR) - 1;

               final int startDayTime = (zonedStartDateTime.getHour() * 3600)
                     + (zonedStartDateTime.getMinute() * 60)
                     + zonedStartDateTime.getSecond();

               allTourIds.add(dbTourId);

               allYears.add(dbTourYear);
               allMonths.add(zonedStartDateTime.getMonthValue());
               allYearsDOY.add(getYearDOYs(dbTourYear) + tourDOY);
               allTourStartWeek.add(dbTourStartWeek);

               allTourStartDateTime.add(zonedStartDateTime);
               allTourStartTime.add(startDayTime);
               allTourEndTime.add((startDayTime + dbRecordingTime));
               allTourRecordingTime.add(dbRecordingTime);
               allTourDrivingTime.add(dbDrivingTime);

               allTourDuration.add(dbDrivingTime == 0 ? dbRecordingTime : dbDrivingTime);

               // round distance
               final float distance = dbDistance / UI.UNIT_VALUE_DISTANCE;

               allDistance.add(distance);
               allAltitudeUp.add(dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE);

               allAvgPace.add(distance == 0 ? 0 : dbDrivingTime * 1000f / distance / 60.0f);
               allAvgSpeed.add(dbDrivingTime == 0 ? 0 : 3.6f * distance / dbDrivingTime);

               allTrainingEffect.add(trainingEffect);
               allTrainingPerformance.add(trainingPerformance);

               allTourTitle.add(dbTourTitle);
               allTourDescription.add(dbDescription == null ? UI.EMPTY_STRING : dbDescription);

               if (dbTagId instanceof Long) {

                  tagIds = new ArrayList<>();
                  tagIds.add((Long) dbTagId);

                  allTagIds.put(dbTourId, tagIds);
               }

               /*
                * Convert type id to the type index in the tour types list which is also the color
                * index
                */
               int colorIndex = 0;
               long dbTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

               if (dbTypeIdObject instanceof Long) {

                  dbTypeId = (Long) dbTypeIdObject;

                  for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
                     if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
                        colorIndex = colorOffset + typeIndex;
                        break;
                     }
                  }
               }

               allTypeColorIndex.add(colorIndex);
               allTypeIds.add(dbTypeId);
            }

            lastTourId = dbTourId;
         }

         conn.close();

         final int[] tourYear = allYears.toArray();
         final int[] tourAllYearsDOY = allYearsDOY.toArray();

         final int[] durationHigh = allTourDuration.toArray();

         final float[] altitude_High = allAltitudeUp.toArray();
         final float[] avgPace_High = allAvgPace.toArray();
         final float[] avgSpeed_High = allAvgSpeed.toArray();
         final float[] distance_High = allDistance.toArray();
         final float[] trainingEffect_High = allTrainingEffect.toArray();

         final int serieLength = durationHigh.length;
         final int[] durationLow = new int[serieLength];
         final float[] altitudeLow = new float[serieLength];
         final float[] avgPaceLow = new float[serieLength];
         final float[] avgSpeedLow = new float[serieLength];
         final float[] distanceLow = new float[serieLength];
         final float[] trainingEffect_Low = new float[serieLength];

         /*
          * adjust low/high values when a day has multiple tours
          */
         int prevTourDOY = -1;

         for (int tourIndex = 0; tourIndex < tourAllYearsDOY.length; tourIndex++) {

            final int tourDOY = tourAllYearsDOY[tourIndex];

            if (prevTourDOY == tourDOY) {

               // current tour is at the same day as the tour before

               durationHigh[tourIndex] += durationLow[tourIndex] = durationHigh[tourIndex - 1];

               altitude_High[tourIndex] += altitudeLow[tourIndex] = altitude_High[tourIndex - 1];
               avgPace_High[tourIndex] += avgPaceLow[tourIndex] = avgPace_High[tourIndex - 1];
               avgSpeed_High[tourIndex] += avgSpeedLow[tourIndex] = avgSpeed_High[tourIndex - 1];
               distance_High[tourIndex] += distanceLow[tourIndex] = distance_High[tourIndex - 1];

               trainingEffect_High[tourIndex] += trainingEffect_Low[tourIndex] = trainingEffect_High[tourIndex - 1];

            } else {

               // current tour is at another day as the tour before

               prevTourDOY = tourDOY;
            }
         }

         // get number of days for all years
         int yearDays = 0;
         for (final int doy : _yearDays) {
            yearDays += doy;
         }

         _tourDayData = new TourData_Day();

         _tourDayData.tourIds = allTourIds.toArray();

         _tourDayData.yearValues = tourYear;
         _tourDayData.monthValues = allMonths.toArray();
         _tourDayData.setDoyValues(tourAllYearsDOY);
         _tourDayData.weekValues = allTourStartWeek.toArray();

         _tourDayData.allDaysInAllYears = yearDays;
         _tourDayData.yearDays = _yearDays;
         _tourDayData.years = _years;

         _tourDayData.typeIds = allTypeIds.toArray();
         _tourDayData.typeColorIndex = allTypeColorIndex.toArray();

         _tourDayData.tagIds = allTagIds;

         _tourDayData.setDurationLow(durationLow);
         _tourDayData.setDurationHigh(durationHigh);

         _tourDayData.altitude_Low = altitudeLow;
         _tourDayData.altitude_High = altitude_High;
         _tourDayData.distance_Low = distanceLow;
         _tourDayData.distance_High = distance_High;

         _tourDayData.avgPace_Low = avgPaceLow;
         _tourDayData.avgPace_High = avgPace_High;
         _tourDayData.avgSpeed_Low = avgSpeedLow;
         _tourDayData.avgSpeed_High = avgSpeed_High;

         _tourDayData.trainingEffect_Low = trainingEffect_Low;
         _tourDayData.trainingEffect_High = trainingEffect_High;

         _tourDayData.allStartTime = allTourStartTime.toArray();
         _tourDayData.allEndTime = allTourEndTime.toArray();
         _tourDayData.allStartDateTimes = allTourStartDateTime;

         _tourDayData.allDistance = allDistance.toArray();
         _tourDayData.allAltitude = allAltitudeUp.toArray();

         _tourDayData.allTrainingEffect = allTrainingEffect.toArray();
         _tourDayData.allTrainingPerformance = allTrainingPerformance.toArray();

         _tourDayData.allRecordingTime = allTourRecordingTime.toArray();
         _tourDayData.allDrivingTime = allTourDrivingTime.toArray();

         _tourDayData.tourTitle = allTourTitle;
         _tourDayData.tourDescription = allTourDescription;

      } catch (final SQLException e) {

         StatusUtil.log(sqlString);
         UI.showSQLException(e);
      }

      return _tourDayData;
   }

}
