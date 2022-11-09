/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.device.polar.hrm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
<<<<<<< HEAD
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
=======
import java.util.Map;
>>>>>>> refs/remotes/Wolfgang/main

import net.tourbook.data.TourData;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

/**
 * This device reader is importing data from Polar device files.
 */
public class Polar_PDD_DataReader extends TourbookDevice {

<<<<<<< HEAD
   private static final String    DATA_DELIMITER           = "\t";                                       //$NON-NLS-1$
=======
   private static final String SECTION_DAY_INFO = "[DayInfo]"; //$NON-NLS-1$
>>>>>>> refs/remotes/Wolfgang/main

<<<<<<< HEAD
   private static final String    SECTION_DAY_INFO         = "[DayInfo]";                                //$NON-NLS-1$
   private static final String    SECTION_EXERCISE_INFO    = "[ExerciseInfo";                            //$NON-NLS-1$
   //
   private final IPreferenceStore _prefStore               = Activator.getDefault().getPreferenceStore();

   private DeviceData             _deviceData;
   private String                 _importFilePath;
   private Map<Long, TourData>    _alreadyImportedTours;
   private Map<Long, TourData>    _newlyImportedTours;
   //
   private boolean                _isDebug                 = false;
   private int                    _fileVersionDayInfo      = -1;

   private Day                    _currentDay;
   private Exercise               _currentExercise;

   private ArrayList<String>      _exerciseFiles           = new ArrayList<>();
   private ArrayList<String>      _additionalImportedFiles = new ArrayList<>();
   private HashMap<Long, Integer> _tourSportMap            = new HashMap<>();

   private boolean                _isReimport;

   private class Day {

      private ZonedDateTime date;
   }

   private class Exercise {

      private int           fileVersion;

      private String        title;
      private String        description;

      private ZonedDateTime startTime;
      private int           distance;
      private int           duration;

      private int           calories;
      private int           sport;
   }
=======
   private boolean             _isDebug         = false;
>>>>>>> refs/remotes/Wolfgang/main

   public Polar_PDD_DataReader() {
      // plugin constructor
   }

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return false;
   }

<<<<<<< HEAD
   private boolean createExercise() throws Exception {

      final TourData exerciseData = new TourData();

      /*
       * set tour start date/time
       */
      exerciseData.setTourStartTime(_currentExercise.startTime);

      exerciseData.setImportFilePath(_importFilePath);

      // set title
      final String title = _currentExercise.title;
      if (title != null) {
         exerciseData.setTourTitle(title);
      } else {
         exerciseData.setTourTitle(UI.EMPTY_STRING);
      }
      // set description
      final String description = _currentExercise.description;
      if (description != null) {
         exerciseData.setTourDescription(description);
      } else {
         exerciseData.setTourDescription(UI.EMPTY_STRING);
      }

      exerciseData.setTourDistance(_currentExercise.distance);
      exerciseData.setTourComputedTime_Moving(_currentExercise.duration);
      exerciseData.setTourDeviceTime_Recorded(_currentExercise.duration);

      // set other fields
      exerciseData.setCalories(_currentExercise.calories);

      // after all data are added, the tour id can be created
      final String uniqueId = createUniqueId(exerciseData, Util.UNIQUE_ID_SUFFIX_POLAR_PDD);
      final Long tourId = exerciseData.createTourId(uniqueId);

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, exerciseData);
      }

      // save the sport type for this exercise
      if (_tourSportMap.containsKey(tourId) == false) {
         _tourSportMap.put(tourId, _currentExercise.sport);
      }

      return true;
   }

   private boolean createExercise(final String hrmFileName, final String gpxFileName) throws Exception {

      // hrm data must be available
      if (hrmFileName == null) {
         return false;
      }

      _exerciseFiles.clear();

      final String titleFromTitle = _prefStore.getString(IPreferences.TITLE_DESCRIPTION);
      final boolean isTitleFromTitle = titleFromTitle
            .equalsIgnoreCase(IPreferences.TITLE_DESCRIPTION_TITLE_FROM_TITLE) || titleFromTitle.length() == 0;

      final IPath importPath = new Path(_importFilePath).removeLastSegments(1);

      // get .hrm data
      final IPath hrmFilePath = importPath.append(hrmFileName);
      final TourData hrmTourData = createExercise_10_ImportSeparatedFile(hrmFilePath, getPolarHRMDataReader());

      if (hrmTourData == null) {
         return false;
      }
      _exerciseFiles.add(hrmFilePath.toOSString());

      // get .gpx data
      if (gpxFileName != null) {

         final IPath gpxFilePath = importPath.append(gpxFileName);
         final TourData gpxTourData = createExercise_10_ImportSeparatedFile(gpxFilePath, getGPXDeviceDataReader());

         if (gpxTourData != null && gpxTourData.latitudeSerie != null) {

            createExercise_20_SyncHrmGpx(hrmTourData, gpxTourData);
            createExercise_22_AdjustTimeSlices(hrmTourData, gpxTourData);
         }

         _exerciseFiles.add(gpxFilePath.toOSString());
      }

      // overwrite path and set it to pdd file so that a reimport works
      hrmTourData.setImportFilePath(_importFilePath);

      // set title
      final String title = _currentExercise.title;
      if (title != null && title.length() > 0) {
         if (isTitleFromTitle) {
            hrmTourData.setTourTitle(title);
         } else {
            hrmTourData.setTourDescription(title);
         }
      }

      // set description
      final String description = _currentExercise.description;
      if (description != null && description.length() > 0) {
         if (isTitleFromTitle) {
            hrmTourData.setTourDescription(description);
         } else {
            hrmTourData.setTourTitle(description);
         }
      }

      // set other fields
      hrmTourData.setCalories(_currentExercise.calories);

      // after all data are added, the tour id can be created
      final String uniqueId = createUniqueId(hrmTourData, Util.UNIQUE_ID_SUFFIX_POLAR_PDD);
      final Long tourId = hrmTourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, hrmTourData);
      }

      // save the sport type for this exercise
      if (_tourSportMap.containsKey(tourId) == false) {
         _tourSportMap.put(tourId, _currentExercise.sport);
      }

      if (_exerciseFiles.size() > 0) {
         _additionalImportedFiles.addAll(_exerciseFiles);
      }

      return true;
   }

   private TourData createExercise_10_ImportSeparatedFile(final IPath importFilePath,
                                                          final TourbookDevice deviceDataReader) throws Exception {

      final File importFile = importFilePath.toFile();

      if (importFile.exists() == false) {
         throw new Exception(
               NLS.bind(
                     "File {0} is not available but is defined in file {1}", //$NON-NLS-1$
                     importFile.toString(),
                     _importFilePath));
      }

      if (deviceDataReader.validateRawData(importFilePath.toOSString()) == false) {
         throw new Exception(
               NLS.bind(
                     "File {0} in parent file {1} is invalid", //$NON-NLS-1$
                     importFile.toString(),
                     _importFilePath));
      }

      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      if (deviceDataReader.processDeviceData(
            importFilePath.toOSString(),
            _deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            _isReimport) == false) {
         return null;
      }

      final TourData[] importTourData = newlyImportedTours.values()
            .toArray(
                  new TourData[newlyImportedTours.values().size()]);

      // check bounds
      if (importTourData.length == 0) {
         return null;
      }

      return importTourData[0];
   }

   /**
    * Sets gpx lat/lon data into hrm tour data. HRM tour data are the leading data serie, GPX data
    * is set according to the time.
    *
    * @param hrmTourData
    * @param gpxTourData
    */
   private void createExercise_20_SyncHrmGpx(final TourData hrmTourData, final TourData gpxTourData) {

      /*
       * set gpx tour start to the same time as the hrm tour start
       */
      final ZonedDateTime hrmTourStart = hrmTourData.getTourStartTime();
      final ZonedDateTime gpxTourStart = gpxTourData.getTourStartTime();

      final long absoluteHrmTourStart = hrmTourStart.toInstant().getEpochSecond();
      long absoluteGpxTourStart = gpxTourStart.toInstant().getEpochSecond();

      final int timeDiff = (int) (absoluteHrmTourStart - absoluteGpxTourStart);
      final int timeDiffHours = (timeDiff / 3600) * 3600;

      // adjust gpx to hrm tour start
      absoluteGpxTourStart = absoluteGpxTourStart + timeDiffHours;

      /*
       * define shortcuts for the data series
       */
      final int[] hrmTimeSerie = hrmTourData.timeSerie;
      final int[] gpxTimeSerie = gpxTourData.timeSerie;

      final int hrmSerieLength = hrmTimeSerie.length;
      final int gpxSerieLength = gpxTimeSerie.length;

      final double[] gpxLatSerie = gpxTourData.latitudeSerie;
      final double[] gpxLonSerie = gpxTourData.longitudeSerie;
      final double[] hrmLatSerie = hrmTourData.latitudeSerie = new double[hrmSerieLength];
      final double[] hrmLonSerie = hrmTourData.longitudeSerie = new double[hrmSerieLength];

      boolean isFirstGpx = true;
      final double firstLat = gpxLatSerie[0];
      final double firstLon = gpxLonSerie[0];
      double prevLat = firstLat;
      double prevLon = firstLon;

      int gpxSerieIndex = 0;

      for (int hrmSerieIndex = 0; hrmSerieIndex < hrmSerieLength; hrmSerieIndex++) {

         final int relativeHrmTime = hrmTimeSerie[hrmSerieIndex];
         final int relativeGpxTime = gpxTimeSerie[gpxSerieIndex];

         final long hrmTime = absoluteHrmTourStart + relativeHrmTime;
         final long gpxTime = absoluteGpxTourStart + relativeGpxTime;

         if (isFirstGpx && gpxTime <= hrmTime) {
            isFirstGpx = false;
         }

         if (gpxTime > hrmTime) {

            // gpx data are not available

            if (isFirstGpx) {

               hrmLatSerie[hrmSerieIndex] = firstLat;
               hrmLonSerie[hrmSerieIndex] = firstLon;

            } else {

               /*
                * set lat/lon from previous slice because it is possible that gpx has missing
                * slices
                */
               hrmLatSerie[hrmSerieIndex] = prevLat;
               hrmLonSerie[hrmSerieIndex] = prevLon;
            }

         } else {

            hrmLatSerie[hrmSerieIndex] = prevLat = gpxLatSerie[gpxSerieIndex];
            hrmLonSerie[hrmSerieIndex] = prevLon = gpxLonSerie[gpxSerieIndex];
         }

         // advance to next slice
         if (hrmTime >= gpxTime) {

            // the case > should not occur but is used to move gpx slice forward

            gpxSerieIndex++;

            // check bounds
            if (gpxSerieIndex >= gpxSerieLength) {
               gpxSerieIndex = gpxSerieLength - 1;
            }
         }
      }

      hrmTourData.computeGeo_Bounds();
   }

   private void createExercise_22_AdjustTimeSlices(final TourData hrmTourData, final TourData gpxTourData) {

      int diffGeoSlices = _prefStore.getInt(IPreferences.SLICE_ADJUSTMENT_VALUE);

      // check if time slices needs to be adjusted
      if (diffGeoSlices == 0) {
         return;
      }

      final int[] hrmTimeSerie = hrmTourData.timeSerie;
      final int hrmSerieLength = hrmTimeSerie.length;

      // adjust slices to bounds
      if (diffGeoSlices > hrmSerieLength) {
         diffGeoSlices = hrmSerieLength - 1;
      } else if (-diffGeoSlices > hrmSerieLength) {
         diffGeoSlices = -(hrmSerieLength - 1);
      }

      final double[] hrmLatSerie = hrmTourData.latitudeSerie;
      final double[] hrmLonSerie = hrmTourData.longitudeSerie;

      final int srcPos = diffGeoSlices >= 0 ? 0 : -diffGeoSlices;
      final int destPos = diffGeoSlices >= 0 ? diffGeoSlices : 0;
      final int adjustedLength = hrmSerieLength - (diffGeoSlices < 0 ? -diffGeoSlices : diffGeoSlices);

      System.arraycopy(hrmLatSerie, srcPos, hrmLatSerie, destPos, adjustedLength);
      System.arraycopy(hrmLonSerie, srcPos, hrmLonSerie, destPos, adjustedLength);

      // fill gaps with starting/ending position
      if (diffGeoSlices >= 0) {

         final double startLat = hrmLatSerie[0];
         final double startLon = hrmLonSerie[0];

         for (int serieIndex = 0; serieIndex < diffGeoSlices; serieIndex++) {
            hrmLatSerie[serieIndex] = startLat;
            hrmLonSerie[serieIndex] = startLon;
         }

      } else {

         // diffGeoSlices < 0

         final int lastIndex = hrmSerieLength - 1;
         final int validEndIndex = lastIndex - (-diffGeoSlices);
         final double endLat = hrmLatSerie[lastIndex];
         final double endLon = hrmLonSerie[lastIndex];

         for (int serieIndex = validEndIndex; serieIndex < hrmSerieLength; serieIndex++) {
            hrmLatSerie[serieIndex] = endLat;
            hrmLonSerie[serieIndex] = endLon;
         }
      }
   }

   @Override
   public ArrayList<String> getAdditionalImportedFiles() {

      if (_additionalImportedFiles.size() > 0) {
         return _additionalImportedFiles;
      }

      return null;
   }

=======
>>>>>>> refs/remotes/Wolfgang/main
   @Override
   public String getDeviceModeName(final int profileId) {
      return null;
   }

   protected TourbookDevice getGPXDeviceDataReader() {
      return new GPXDeviceDataReader();
   }

   protected TourbookDevice getPolarHRMDataReader() {
      return new Polar_HRM_DataReader();
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return -1;
   }

   @Override
   public int getTransferDataSize() {
      return -1;
   }

   @Override
<<<<<<< HEAD
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final Map<Long, TourData> alreadyImportedTours,
                                    final Map<Long, TourData> newlyImportedTours,
                                    final boolean isReimport) {

      _importFilePath = importFilePath;
      _deviceData = deviceData;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

      _additionalImportedFiles.clear();
      _exerciseFiles.clear();
=======
   public void processDeviceData(final String importFilePath,
                                 final DeviceData deviceData,
                                 final Map<Long, TourData> alreadyImportedTours,
                                 final Map<Long, TourData> newlyImportedTours,
                                 final ImportState_File importState_File,
                                 final ImportState_Process importState_Process) {
>>>>>>> refs/remotes/Wolfgang/main

      _isReimport = isReimport;

      if (_isDebug) {
         System.out.println(importFilePath);
      }

      new Polar_PDD_Data(

            importFilePath,
            alreadyImportedTours,
            newlyImportedTours,

            importState_File,
            importState_Process,

            this

      ).parseSection();
   }

   /**
    * @return Return <code>true</code> when the file has a valid .hrm data format
    */
   @Override
   public boolean validateRawData(final String fileName) {

      try (FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {

         final String firstLine = bufferedReader.readLine();
         if (firstLine == null || firstLine.startsWith(SECTION_DAY_INFO) == false) {
            return false;
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }

      return true;
   }
}
