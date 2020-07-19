/*******************************************************************************
 * Copyright (C) 2018, 2020 Frédéric Bard
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
package net.tourbook.device.suunto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Suunto9DeviceDataReader extends TourbookDevice {

   private HashMap<TourData, ArrayList<TimeData>> _processedActivities         = new HashMap<>();

   private HashMap<String, String>                _childrenActivitiesToProcess = new HashMap<>();

   private HashMap<Long, TourData>                _newlyImportedTours          = new HashMap<>();
   private HashMap<Long, TourData>                _alreadyImportedTours        = new HashMap<>();

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      // NEXT Auto-generated method stub
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return true;
   }

   /**
    * At the end of the import process, we clean the accumulated data so that the next round will
    * begin clean.
    */
   private void cleanUpActivities() {
      _childrenActivitiesToProcess.clear();
      _processedActivities.clear();
   }

   /**
    * Concatenates children activities with a given activity.
    *
    * @param filePath
    *           The absolute path of a given activity.
    * @param currentFileNumber
    *           The file number of the given activity. Example : If the current activity file is
    *           1536723722706_{DeviceSerialNumber}_-2.json.gz its file number will be 2.
    * @param currentActivity
    *           The current activity processed and created.
    */
   private void ConcatenateChildrenActivities(final String filePath,
                                              int currentFileNumber,
                                              final TourData currentActivity,
                                              final ArrayList<TimeData> sampleListToReUse) {
      final SuuntoJsonProcessor suuntoJsonProcessor = new SuuntoJsonProcessor();

      final ArrayList<String> keysToRemove = new ArrayList<>();
      for (@SuppressWarnings("unused")
      final Map.Entry<String, String> unused : _childrenActivitiesToProcess.entrySet()) {

         final String parentFileName = GetFileNameWithoutNumber(
               FilenameUtils.getBaseName(filePath)) +
               UI.DASH +
               String.valueOf(++currentFileNumber) +
               ".json.gz"; //$NON-NLS-1$

         final Map.Entry<String, String> childEntry = getChildActivity(parentFileName);

         if (childEntry == null) {
            continue;
         }

         suuntoJsonProcessor.ImportActivity(
               childEntry.getValue(),
               currentActivity,
               sampleListToReUse);

         // We just concatenated a child activity so we can remove it
         // from the list of activities to process
         keysToRemove.add(childEntry.getKey());

         // We need to update the activity we just concatenated by
         // updating the file path and the activity object.
         removeProcessedActivity(currentActivity.getImportFilePath());
         currentActivity.setImportFilePath(childEntry.getKey());
         _processedActivities.put(currentActivity, suuntoJsonProcessor.getSampleList());
      }

      for (final String element : keysToRemove) {
         _childrenActivitiesToProcess.remove(element);
      }
   }

   /**
    * Retrieves an unprocessed activity that is the child of a given processed activity.
    *
    * @param filePath
    *           The absolute path of a given activity.
    * @return If found, the child activity.
    */
   private Entry<String, String> getChildActivity(final String filePath) {
      for (final Entry<String, String> childEntry : _childrenActivitiesToProcess.entrySet()) {
         if (childEntry.getKey().contains(filePath)) {
            return childEntry;
         }
      }

      return null;
   }

   @Override
   public String getDeviceModeName(final int profileId) {
      return UI.EMPTY_STRING;
   }

   /**
    * Returns a file name without its number. Example : Input :
    * C:\Users\fbard\Downloads\S9\IMTUF100\1537365863086_{DeviceSerialNumber}_post_timeline-1.json.gz
    * Output : 1537365863086_183010004848_post_timeline-
    *
    * @param fileName
    *           The file name to process.
    * @return The processed file name.
    */

   private String GetFileNameWithoutNumber(final String fileName) {
      return fileName.substring(0, fileName.lastIndexOf(UI.DASH));
   }

   /**
    * Retrieves the JSON content from a GZip Suunto file.
    *
    * @param gzipFilePath
    *           The absolute file path of the Suunto file.
    * @param isValidatingFile
    * @return Returns the JSON content.
    */
   private String GetJsonContentFromGZipFile(final String gzipFilePath, final boolean isValidatingFile) {
      String jsonFileContent = null;
      try (final GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(gzipFilePath));
            final BufferedReader br = new BufferedReader(new InputStreamReader(gzip))) {

         jsonFileContent = br.readLine();

      } catch (final IOException e) {

         if (isValidatingFile) {

            /*
             * Log only when reading the zip file, during a validation, an exception can be very
             * likely and should not be displayed
             */

         } else {
            StatusUtil.log(e);
         }

         return UI.EMPTY_STRING;
      }

      return jsonFileContent;
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return 0;
   }

   @Override
   public int getTransferDataSize() {
      return -1;
   }

   /**
    * Checks if the file is a valid Suunto Spartan/9 activity.
    *
    * @param jsonFileContent
    *           The content to check.
    * @return Returns <code>true</code> when the file contains content of a valid activity.
    */
   protected boolean isValidActivity(final String jsonFileContent) {

      final BufferedReader fileReader = null;
      try {

         if (jsonFileContent == null || jsonFileContent == UI.EMPTY_STRING) {
            return false;
         }

         try {
            final JSONObject jsonContent = new JSONObject(jsonFileContent);
            final JSONArray samples = (JSONArray) jsonContent.get(SuuntoJsonProcessor.TAG_SAMPLES);

            for (int index = 0; index < samples.length(); ++index) {
               final String currentSample = samples.getJSONObject(index).toString();
               if (currentSample.contains(SuuntoJsonProcessor.TAG_SAMPLE) &&
                     (currentSample.contains(SuuntoJsonProcessor.TAG_GPSALTITUDE) ||
                           currentSample.contains(SuuntoJsonProcessor.TAG_LONGITUDE) ||
                           currentSample.contains(SuuntoJsonProcessor.TAG_LATITUDE) ||
                           currentSample.contains(SuuntoJsonProcessor.TAG_ALTITUDE))) {
                  Util.closeReader(fileReader);
                  return true;
               }
            }

         } catch (final JSONException ex) {
            StatusUtil.log(ex);
            return false;
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
         return false;
      } finally {
         Util.closeReader(fileReader);
      }

      return false;
   }

   /**
    * Checks if the file is a valid device JSON file.
    *
    * @param jsonFileContent
    *           The content to check.
    * @return Returns <code>true</code> when the file contains content with the requested tag.
    */
   protected boolean isValidJSONFile(final String jsonFileContent) {
      final BufferedReader fileReader = null;
      try {

         if (jsonFileContent == null ||
               jsonFileContent == "") { //$NON-NLS-1$
            return false;
         }

         try {
            final JSONObject jsonContent = new JSONObject(jsonFileContent);
            final JSONArray samples = (JSONArray) jsonContent.get(SuuntoJsonProcessor.TAG_SAMPLES);

            final String firstSample = samples.get(0).toString();
            if (firstSample.contains(SuuntoJsonProcessor.TAG_ATTRIBUTES) &&
                  firstSample.contains(SuuntoJsonProcessor.TAG_SOURCE) &&
                  firstSample.contains(SuuntoJsonProcessor.TAG_TIMEISO8601)) {
               Util.closeReader(fileReader);
               return true;
            }

         } catch (final JSONException ex) {
            StatusUtil.log(ex);
            return false;
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
         return false;
      } finally {
         Util.closeReader(fileReader);
      }

      return false;
   }

   @Override
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final HashMap<Long, TourData> alreadyImportedTours,
                                    final HashMap<Long, TourData> newlyImportedTours) {
      _newlyImportedTours = newlyImportedTours;
      _alreadyImportedTours = alreadyImportedTours;

      // When a new import is started, we need to clean the previous saved activities
      if (newlyImportedTours.size() == 0 && alreadyImportedTours.size() == 0) {
         cleanUpActivities();
      }

      final String jsonFileContent =
            GetJsonContentFromGZipFile(importFilePath, false);

      // At this point, we know that the given file is a valid JSON file.
      // But to avoid for invalid activities to be parsed by other
      // parsers, we return true when a Suunto JSON file is not
      // a valid activity.
      if (!isValidActivity(jsonFileContent)) {
         return true;
      }

      return ProcessFile(importFilePath, jsonFileContent);
   }

   /**
    * Checks if an activity has already been processed.
    *
    * @param tourId
    *           The tour ID of the activity.
    * @return True if the activity has already been processed, false otherwise.
    */
   private boolean processedActivityExists(final long tourId) {
      for (final Map.Entry<TourData, ArrayList<TimeData>> entry : _processedActivities.entrySet()) {
         final TourData key = entry.getKey();
         if (key.getTourId() == tourId) {
            return true;
         }
      }

      return false;
   }

   /**
    * For a given Suunto activity file, the function processes it and imports it as a tour.
    * activity.
    *
    * @param filePath
    *           The absolute full path of a given activity.
    * @param jsonFileContent
    *           The JSON content of the activity file.
    * @return The Suunto activity as a tour.
    */
   private boolean ProcessFile(final String filePath, final String jsonFileContent) {
      final SuuntoJsonProcessor suuntoJsonProcessor = new SuuntoJsonProcessor();

      String fileName =
            FilenameUtils.removeExtension(filePath);

      if (fileName.substring(fileName.length() - 5, fileName.length()) == ".json") { //$NON-NLS-1$
         fileName = FilenameUtils.removeExtension(fileName);
      }

      final String fileNumberString =
            fileName.substring(fileName.lastIndexOf('-') + 1, fileName.lastIndexOf('-') + 2);

      int fileNumber;
      try {
         fileNumber = Integer.parseInt(fileNumberString);
      } catch (final NumberFormatException e) {
         StatusUtil.log(e);
         return false;
      }

      TourData activity = null;
      if (fileNumber == 1) {
         activity = suuntoJsonProcessor.ImportActivity(
               jsonFileContent,
               null,
               null);

         if (activity == null) {
            return false;
         }

         final String uniqueId = this.createUniqueId(activity, Util.UNIQUE_ID_SUFFIX_SUUNTO9);
         activity.createTourId(uniqueId);

         if (!processedActivityExists(activity.getTourId())) {
            _processedActivities.put(activity, suuntoJsonProcessor.getSampleList());
         }

      } else if (fileNumber > 1) {
         // if we find the parent (e.g: The activity just before the
         // current one. Example : If the current is xxx-3, we find xxx-2)
         // then we import it reusing the parent activity AND we check that there is no children waiting to be imported
         // If nothing is found, we store it for (hopefully) future use.
         Map.Entry<TourData, ArrayList<TimeData>> parentEntry = null;
         for (final Map.Entry<TourData, ArrayList<TimeData>> entry : _processedActivities.entrySet()) {
            final TourData key = entry.getKey();

            final String parentFileName = GetFileNameWithoutNumber(
                  FilenameUtils.getBaseName(filePath)) +
                  "-" + //$NON-NLS-1$
                  String.valueOf(fileNumber - 1) +
                  ".json.gz"; //$NON-NLS-1$

            if (key.getImportFileName().contains(parentFileName)) {
               parentEntry = entry;
               break;
            }
         }

         if (parentEntry == null) {
            if (!_childrenActivitiesToProcess.containsKey(filePath)) {
               _childrenActivitiesToProcess.put(filePath, jsonFileContent);
            }
         } else {
            activity = suuntoJsonProcessor.ImportActivity(
                  jsonFileContent,
                  parentEntry.getKey(),
                  parentEntry.getValue());

            //We remove the parent activity to replace it with the
            //updated one (parent activity concatenated with the current
            //one).
            final Iterator<Entry<TourData, ArrayList<TimeData>>> it = _processedActivities.entrySet().iterator();
            while (it.hasNext()) {
               final Map.Entry<TourData, ArrayList<TimeData>> entry = it.next();
               if (entry.getKey().getTourId() == parentEntry.getKey().getTourId()) {
                  it.remove();
               }
            }

            if (!processedActivityExists(activity.getTourId())) {
               _processedActivities.put(activity, suuntoJsonProcessor.getSampleList());
            }
         }
      }

      //We check if the child(ren) has(ve) been provided earlier.
      //In this case, we concatenate it(them) with the parent
      //activity
      if (activity != null) {

         activity.setImportFilePath(filePath);

         ConcatenateChildrenActivities(
               filePath,
               fileNumber,
               activity,
               suuntoJsonProcessor.getSampleList());

         TryFinalizeTour(activity);
      }

      return true;
   }

   /**
    * Removes an already processed activity.
    *
    * @param filePath
    *           The absolute path of a given activity.
    */
   private void removeProcessedActivity(final String filePath) {
      final Iterator<Entry<TourData, ArrayList<TimeData>>> it = _processedActivities.entrySet().iterator();
      while (it.hasNext()) {
         final Map.Entry<TourData, ArrayList<TimeData>> entry = it.next();
         if (entry.getKey().getImportFilePath() == filePath) {
            it.remove();
            return;
         }
      }
   }

   /**
    * Attempting to finalize an activity. If it doesn't contain a tourId, it is not a final
    * activity.
    *
    * @param tourData
    *           The tour to finalize.
    */
   private void TryFinalizeTour(final TourData tourData) {

      tourData.setDeviceId(deviceId);

      long tourId;
      try {

         tourId = tourData.getTourId();

      } catch (final NullPointerException e) {
         StatusUtil.log(e);
         tourId = -1;
      }

      if (tourId != -1) {
         // check if the tour is already imported
         if (_alreadyImportedTours.containsKey(tourId)) {
            _alreadyImportedTours.remove(tourId);
         }

         // add new tour to other tours
         if (_newlyImportedTours.containsKey(tourId)) {
            _newlyImportedTours.remove(tourId);
         }
         _newlyImportedTours.put(tourId, tourData);

         tourData.computeAltitudeUpDown();
         tourData.computeTourDrivingTime();
         tourData.computeComputedValues();
      }
   }

   @Override
   public boolean validateRawData(final String fileName) {
      final String jsonFileContent = GetJsonContentFromGZipFile(fileName, true);
      return isValidJSONFile(jsonFileContent);
   }
}
