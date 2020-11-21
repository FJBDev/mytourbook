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

   //TODO FB split files

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
