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
package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.data.TourData;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class Comparison {

   private static final String JSON = ".json"; //$NON-NLS-1$

   /**
    * Compares a test transaction against a control transaction.
    *
    * @param testTourData
    *           The generated test TourData object.
    * @param controlFileName
    *           The control's file name.
    */
   public static void CompareJsonAgainstControl(final TourData testTourData,
                                                final String controlFileName) {

      // When using Java 11, convert the line below to the Java 11 method
      //String controlDocument = Files.readString(controlDocumentFilePath, StandardCharsets.US_ASCII);

      final String controlDocumentFilePath = Paths.get(controlFileName + JSON).toAbsolutePath().toString();
      final String controlDocument = readFile(controlDocumentFilePath, StandardCharsets.US_ASCII);

      final String testJson = testTourData.toJson();

      final JSONCompareResult result =
            JSONCompare.compareJSON(controlDocument, testJson, JSONCompareMode.LENIENT);

      if (result.failed()) {
         WriteErroneousFiles(controlFileName, testJson);
      }

      JSONAssert.assertEquals(
            controlDocument,
            testJson,
            new CustomComparator(JSONCompareMode.LENIENT));
//                  ,new Customization("importFilePathName", (o1, o2) -> true),
//                  new Customization("importFilePathNameText", (o1, o2) -> true),
//                  new Customization("importFilePath", (o1, o2) -> true)));
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

   public static TourData RetrieveImportedTour(final HashMap<Long, TourData> newlyImportedTours) {
      final Map.Entry<Long, TourData> entry = newlyImportedTours.entrySet().iterator().next();
      final TourData tour = entry.getValue();
      return tour;
   }

   /**
    * Code useful when the tests fail and one wants to be able to compare the expected vs actual
    * file
    *
    * @param controlFileName
    * @param testJson
    */
   private static void WriteErroneousFiles(final String controlFileName, final String testJson) {

      final File myFile = new File(
            controlFileName + "-Erroneous.json"); //$NON-NLS-1$

      if (!myFile.exists()) {
         try {
            myFile.createNewFile();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      try (Writer writer = new FileWriter(myFile);
            BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
         bufferedWriter.write(testJson);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
}
