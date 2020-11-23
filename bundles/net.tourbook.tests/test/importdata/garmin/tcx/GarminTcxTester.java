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
package importdata.garmin.tcx;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.GarminSAXHandler;
import net.tourbook.device.suunto.Suunto9DeviceDataReader;
import net.tourbook.importdata.DeviceData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Comparison;

public class GarminTcxTester {
   private static SAXParser               parser;
   private static final String            IMPORT_PATH = "/importdata/garmin/tcx/files/"; //$NON-NLS-1$

   private static DeviceData              deviceData;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static Suunto9DeviceDataReader deviceDataReader;

   @BeforeAll
   static void initAll() throws ParserConfigurationException, SAXException {
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      parser = factory.newSAXParser();
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new Suunto9DeviceDataReader();
   }

   /**
    * Regression test
    *
    * @throws IOException
    * @throws SAXException
    */
   @Test
   void testFitImportConeyLake() throws SAXException, IOException {

      final String filePathWithoutExtension = IMPORT_PATH +
            "Move_2020_05_23_08_55_42_Trail+running";
      final String importFilePath = filePathWithoutExtension + ".tcx";
      final InputStream tcxFile = GarminTcxTester.class.getResourceAsStream(importFilePath);

      final GarminSAXHandler handler = new GarminSAXHandler(
            deviceDataReader,
            importFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours);

      parser.parse(tcxFile, handler);

      final TourData tour = Comparison.RetrieveImportedTour(newlyImportedTours);

      Comparison.CompareJsonAgainstControl(tour, "test/" + filePathWithoutExtension);
   }
}
