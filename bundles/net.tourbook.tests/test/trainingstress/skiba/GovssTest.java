/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package trainingstress.skiba;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;

import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.device.garmin.GarminDeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;
import net.tourbook.trainingstress.Govss;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Comparison;
import utils.Initializer;

class GovssTest {

   private static TourPerson              tourPerson;
   private static SAXParser               parser;
   private static DeviceData              deviceData;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static GarminDeviceDataReader  deviceDataReader;

   public static final String             IMPORT_FILE_PATH = "/trainingstress/skiba/files/"; //$NON-NLS-1$

   @BeforeAll
   public static void initialize() {

      tourPerson = new TourPerson();
      tourPerson.setGovssThresholdPower(368);
      tourPerson.setHeight(1.84f);
      tourPerson.setWeight(70);
      tourPerson.setGovssTimeTrialDuration(3600);

      parser = Initializer.initializeParser();
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new GarminDeviceDataReader();
   }

   private int ComputeGovssFromTour(final String fileName) {
      final InputStream gpx = GovssTest.class.getResourceAsStream(IMPORT_FILE_PATH + fileName);

      final GPX_SAX_Handler gpxSaxHandler = new GPX_SAX_Handler(
            deviceDataReader,
            IMPORT_FILE_PATH,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours);

      try {
         parser.parse(gpx, gpxSaxHandler);
      } catch (SAXException | IOException e) {
         e.printStackTrace();
      }

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      return new Govss(tourPerson, tour).Compute();
   }

   @BeforeEach
   void setupTest() {

      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }

   @Test
   void testComputeGovssForestPark() {
      final int govss = ComputeGovssFromTour("2015-05-10-ForestPark.gpx"); //$NON-NLS-1$

      //TopoFusion 5.71 value: 93
      //GoldenCheetah 3.6 value: 94
      assertEquals(81, govss);
   }

   @Test
   void testComputeGovssHallRanch() {
      final int govss = ComputeGovssFromTour("2020-05-03-HallRanch.gpx"); //$NON-NLS-1$

      //TopoFusion 5.71 value: 116
      //GoldenCheetah 3.6 value: 117
      assertEquals(103, govss);
   }

   @Test
   void testComputeGovssMtEddy() {
      final int govss = ComputeGovssFromTour("2018-06-09-MtEddy.gpx"); //$NON-NLS-1$

      //TopoFusion 5.71 value: 75
      //GoldenCheetah 3.6 value: 59
      assertEquals(42, govss);
   }

   @Test
   void testComputeGovssMtWhitney() {
      final int govss = ComputeGovssFromTour("2017-09-30-MtWhitney.gpx"); //$NON-NLS-1$

      //TopoFusion 5.71 value: 82
      //GoldenCheetah 3.6 value: 58
      assertEquals(61, govss);
   }

   @Test
   void testComputeGovssSoftRockPart2() {
      final int govss = ComputeGovssFromTour("2018_07_11-SoftRock-CW-Part2.gpx"); //$NON-NLS-1$

      //TopoFusion 5.71 value: 89
      //GoldenCheetah 3.6 value: 79
      assertEquals(80, govss);
   }

   @Test
   void testComputeGovssTMBPart2() {
      final int govss = ComputeGovssFromTour("2019-07-14-TMB-Part2.gpx"); //$NON-NLS-1$

      //TopoFusion 5.71 value: 179
      //GoldenCheetah 3.6 value: 147
      assertEquals(136, govss);
   }

   @Test
   void testComputeGovssTontoTrail() {
      final int govss = ComputeGovssFromTour("2019-03-20-TontoTrail.gpx"); //$NON-NLS-1$

      //TopoFusion 5.71 value: 246
      //GoldenCheetah 3.6 value: 242
      assertEquals(191, govss);
   }
}
