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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.data.TourData;
import net.tourbook.device.suunto.Suunto9DeviceDataReader;
import net.tourbook.importdata.DeviceData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class Suunto9Tester {

   /**
    * Resource path to all the test files
    */
   private static final String IMPORT_FILE_PATH = "test/importdata/suunto9/files/"; //$NON-NLS-1$

// File #1
   private static final String maxWell1FilePath = IMPORT_FILE_PATH +
         "1536723722706_183010004848_post_timeline-1";              //$NON-NLS-1$

   private static final String JSON_GZ          = ".json.gz";
   private static final String JSON             = ".json";

   /**
    * Compares a test transaction against a control transaction.
    *
    * @param controlDocument
    *           The control document's file path.
    * @param xmlTestDocument
    *           The test Suunto 9 GZip file's content.
    * @return True if no differences were found, false otherwise.
    * @throws IOException
    */
   private static void CompareAgainstControl(final String xmlTestDocument,
                                             final String testFileName) {

      // When using Java 11, convert the line below to the Java 11 method
      //String controlDocument = Files.readString(controlDocumentFilePath, StandardCharsets.US_ASCII);

      final String controlDocumentFilePath = Paths.get(testFileName + JSON).toAbsolutePath().toString();

      final String controlDocument = readFile(controlDocumentFilePath, StandardCharsets.US_ASCII);
//      BufferedWriter bufferedWriter = null;
//      final File myFile = new File(
//            "C:\\Users\\frederic\\Desktop\\toto.json"); //$NON-NLS-1$
//      // check if file exist, otherwise create the file before writing
//      if (!myFile.exists()) {
//         try {
//            myFile.createNewFile();
//      Writer writer = new FileWriter(myFile);
//      bufferedWriter = new BufferedWriter(writer);
//            writer = new FileWriter(myFile);
//            bufferedWriter.write(xmlTestDocument);
//            bufferedWriter.close();
//            writer.close();
//         } catch (final IOException e) {
//            e.printStackTrace();
//         }
//      }

      assert xmlTestDocument.equals(controlDocument);

   }

   private static String readFile(final String path, final Charset encoding) {
      byte[] encoded = null;
      try {
         encoded = Files.readAllBytes(Paths.get(path));
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return new String(encoded, encoding);
   }

   @BeforeAll
   static void setUp() {}

   /**
    * Regression test. Imports GPX into TourData and checks all values of tour1 and waypoint1.
    *
    * @throws URISyntaxException
    * @throws ParserConfigurationException
    * @throws IOException
    * @throws SAXException
    */
   @Test
   void testParseMaxwell1() {

      final DeviceData deviceData = new DeviceData();
      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();

      final String testFilePath = Paths.get(maxWell1FilePath + JSON_GZ).toAbsolutePath().toString();
      final Suunto9DeviceDataReader handler = new Suunto9DeviceDataReader();
      handler.processDeviceData(testFilePath, deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(20189117275950L));
      //TODO Convert to JSON instead of XML
      final String testJson = tour.toJson();

      CompareAgainstControl(testJson, maxWell1FilePath);
   }
}
