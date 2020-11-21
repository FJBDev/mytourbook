/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package importdata.suunto9;

import java.nio.file.Paths;
import java.util.HashMap;

import net.tourbook.data.TourData;
import net.tourbook.device.suunto.Suunto9DeviceDataReader;
import net.tourbook.importdata.DeviceData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;

class Suunto9Tester {

   private static final String            IMPORT_FILE_PATH = "test/importdata/suunto9/files/"; //$NON-NLS-1$

   private static final String            JSON_GZ          = ".json.gz";

   private static DeviceData              deviceData;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static Suunto9DeviceDataReader handler;

   @BeforeAll
   static void setUp() {
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      handler = new Suunto9DeviceDataReader();
   }

//   /**
//    * Used only for unit tests, it retrieves the last processed activity.
//    *
//    * @return If any, the last processed activity.
//    */
//   private TourData GetLastTourDataImported() {
//      https://stackoverflow.com/questions/6694715/junit-testing-private-variables
//      final Field privateStringField = Suunto9DeviceDataReader.class.getDeclaredField("privateString");
//      final PrivateObject privateObject = new PrivateObject("The Private Value");
//      privateStringField.setAccessible(true);
//
//      final String fieldValue = (String) privateStringField.get(privateObject);
//      System.out.println("fieldValue = " + fieldValue);
//      final Iterator<Entry<TourData, ArrayList<TimeData>>> it = handler._processedActivities.entrySet().iterator();
//      TourData lastTourData = null;
//      while (it.hasNext()) {
//         lastTourData = it.next().getKey();
//      }
//
//      return lastTourData;
//   }

   /**
    * City of Rocks, ID
    */
   @Test
   void testParseCityOfRocks() {
      final String filePath = IMPORT_FILE_PATH + "1537365846902_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + JSON_GZ).toAbsolutePath().toString();
      handler.processDeviceData(testFilePath, deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(20189139336610L));

      Comparison.CompareJsonAgainstControl(tour, filePath);
   }

   /**
    * Maxwell, CO
    */
   @Test
   void testParseMaxwell1() {
      final String filePath = IMPORT_FILE_PATH + "1536723722706_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + JSON_GZ).toAbsolutePath().toString();
      handler.processDeviceData(testFilePath, deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(20189117275950L));

      Comparison.CompareJsonAgainstControl(tour, filePath);
   }

   //TODO FB split files

   /**
    * Reservoir Ridge with MoveSense HR belt (R-R data)
    */
   @Test
   void testParseRRData() {
      final String filePath = IMPORT_FILE_PATH + "1549250450458_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + JSON_GZ).toAbsolutePath().toString();
      handler.processDeviceData(testFilePath, deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(201923115114154L));

      Comparison.CompareJsonAgainstControl(tour, filePath);
   }

   /**
    * Shoreline - with laps/markers
    */
   @Test
   void testParseShoreLineWithLaps() {
      final String filePath = IMPORT_FILE_PATH + "1555291925128_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + JSON_GZ).toAbsolutePath().toString();
      handler.processDeviceData(testFilePath, deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(201941073512556L));

      Comparison.CompareJsonAgainstControl(tour, filePath);
   }

   /**
    * Unit tests for Suunto Spartan/9 split files.
    * Because the files are split, it causes small discrepancies in the data
    * in between each file.
    * Also, because the import creates a marker at the end of the activity if markers are present,
    * it can create additional markers at the end of each file.
    *
    * @return
    */
//   @Test
//   void testParseSplitFiles() {
//
//      // Maxwell, CO (Split manually)
//
//      // File #1
//      final String maxWell1FilePath = IMPORT_FILE_PATH +
//            "1536723722706_183010004848_post_timeline-1.json.gz"; //$NON-NLS-1$
//
//      // File #2
//      final String maxWell2FilePath = IMPORT_FILE_PATH +
//            "1536723722706_183010004848_post_timeline-2.json.gz"; //$NON-NLS-1$
//
//      // File #3
//      final String maxWell3FilePath = IMPORT_FILE_PATH +
//            "1536723722706_183010004848_post_timeline-3.json.gz"; //$NON-NLS-1$
//
//      // File control
//      final String controlDocumentPath = IMPORT_FILE_PATH +
//            "1536723722706_183010004848_post_timeline-1-SplitTests.xml"; //$NON-NLS-1$
//      final String controlFileContent = GetContentFromResource(controlDocumentPath, false);
//
//      // ORDER 2 - 1 - 3
//
//      String jsonContent = GetContentFromResource(maxWell2FilePath, true);
//      processFile(maxWell2FilePath, jsonContent);
//
//      jsonContent = GetContentFromResource(maxWell1FilePath, true);
//      processFile(maxWell1FilePath, jsonContent);
//
//      jsonContent = GetContentFromResource(maxWell3FilePath, true);
//      processFile(maxWell3FilePath, jsonContent);
//
//      TourData entry = GetLastTourDataImported();
//      String xml = entry.toXml();
//      boolean testResults = CompareAgainstControl(controlFileContent, xml, FilenameUtils.getBaseName(controlDocumentPath));
//
//      cleanUpActivities();
//      // ORDER 2 - 3 - 1
//
//      // File #2
//      jsonContent = GetContentFromResource(maxWell2FilePath, true);
//      processFile(maxWell2FilePath, jsonContent);
//
//      // File #3
//      jsonContent = GetContentFromResource(maxWell3FilePath, true);
//      processFile(maxWell3FilePath, jsonContent);
//
//      // File #1
//      jsonContent = GetContentFromResource(maxWell1FilePath, true);
//      processFile(maxWell1FilePath, jsonContent);
//
//      entry = GetLastTourDataImported();
//      xml = entry.toXml();
//      testResults &= CompareAgainstControl(controlFileContent, xml, FilenameUtils.getBaseName(controlDocumentPath));
//
//      cleanUpActivities();
//      // ORDER 1 - 2 - 3
//
//      // File #1
//      jsonContent = GetContentFromResource(maxWell1FilePath, true);
//      processFile(maxWell1FilePath, jsonContent);
//
//      // File #2
//      jsonContent = GetContentFromResource(maxWell2FilePath, true);
//      processFile(maxWell2FilePath, jsonContent);
//
//      // File #3
//      jsonContent = GetContentFromResource(maxWell3FilePath, true);
//      processFile(maxWell3FilePath, jsonContent);
//
//      entry = GetLastTourDataImported();
//      xml = entry.toXml();
//      testResults &= CompareAgainstControl(controlFileContent, xml, FilenameUtils.getBaseName(controlDocumentPath));
//      cleanUpActivities();
//      // ORDER 1 - 3 - 2
//
//      // File #1
//      jsonContent = GetContentFromResource(maxWell1FilePath, true);
//      processFile(maxWell1FilePath, jsonContent);
//
//      // File #3
//      jsonContent = GetContentFromResource(maxWell3FilePath, true);
//      processFile(maxWell3FilePath, jsonContent);
//
//      // File #2
//      jsonContent = GetContentFromResource(maxWell2FilePath, true);
//      processFile(maxWell2FilePath, jsonContent);
//
//      entry = GetLastTourDataImported();
//      xml = entry.toXml();
//      testResults &= CompareAgainstControl(controlFileContent, xml, FilenameUtils.getBaseName(controlDocumentPath));
//
//      cleanUpActivities();
//      // ORDER 3 - 2 - 1
//
//      // File #3
//      jsonContent = GetContentFromResource(maxWell3FilePath, true);
//      processFile(maxWell3FilePath, jsonContent);
//
//      // File #2
//      jsonContent = GetContentFromResource(maxWell2FilePath, true);
//      processFile(maxWell2FilePath, jsonContent);
//
//      // File #1
//      jsonContent = GetContentFromResource(maxWell1FilePath, true);
//      processFile(maxWell1FilePath, jsonContent);
//
//      entry = GetLastTourDataImported();
//      xml = entry.toXml();
//      testResults &= CompareAgainstControl(controlFileContent, xml, FilenameUtils.getBaseName(controlDocumentPath));
//
//      cleanUpActivities();
//      // ORDER 3 - 1 - 2
//
//      // File #3
//      jsonContent = GetContentFromResource(maxWell3FilePath, true);
//      processFile(maxWell3FilePath, jsonContent);
//
//      // File #1
//      jsonContent = GetContentFromResource(maxWell1FilePath, true);
//      processFile(maxWell1FilePath, jsonContent);
//
//      // File #2
//      jsonContent = GetContentFromResource(maxWell2FilePath, true);
//      processFile(maxWell2FilePath, jsonContent);
//
//      entry = GetLastTourDataImported();
//      xml = entry.toXml();
//      testResults &= CompareAgainstControl(controlFileContent, xml, FilenameUtils.getBaseName(controlDocumentPath));
//
//      return testResults;
//   }

   /**
    * Start -> 100m -> LAP -> LAP -> 100m -> LAP -> LAP -> 100m -> LAP -> LAP -> 100m -> Stop
    * (courtesy of Z74)
    */
   @Test
   void testParseSwimming1() {
      final String filePath = IMPORT_FILE_PATH + "1547628896209_184710003036_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + JSON_GZ).toAbsolutePath().toString();
      handler.processDeviceData(testFilePath, deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(2019116911400L));

      Comparison.CompareJsonAgainstControl(tour, filePath);
   }

   /**
    * Start -> 100m -> Stop (courtesy of Z74)
    */
   @Test
   void testParseSwimming2() {
      final String filePath = IMPORT_FILE_PATH + "1547628897243_184710003036_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + JSON_GZ).toAbsolutePath().toString();
      handler.processDeviceData(testFilePath, deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(2019116921100L));

      Comparison.CompareJsonAgainstControl(tour, filePath);
   }
}
