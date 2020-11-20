package utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.tourbook.data.TourData;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class Comparison {

   private static final String JSON = ".json";

   /**
    * Compares a test transaction against a control transaction.
    *
    * @param testTourData
    *           The generated test TourData object.
    * @param testFileName
    *           The test's file name.
    */
   public static void CompareAgainstControl(final TourData testTourData,
                                            final String testFileName) {

      final String testJson = testTourData.toJson();

      // When using Java 11, convert the line below to the Java 11 method
      //String controlDocument = Files.readString(controlDocumentFilePath, StandardCharsets.US_ASCII);

      final String controlDocumentFilePath = Paths.get(testFileName + JSON).toAbsolutePath().toString();
      final String controlDocument = readFile(controlDocumentFilePath, StandardCharsets.US_ASCII);

//    BufferedWriter bufferedWriter = null;
//    final File myFile = new File(
//          "C:\\Users\\frederic\\Desktop\\toto.json"); //$NON-NLS-1$
//    // check if file exist, otherwise create the file before writing
//    if (!myFile.exists()) {
//       try {
//          myFile.createNewFile();
//    Writer writer = new FileWriter(myFile);
//    bufferedWriter = new BufferedWriter(writer);
//          writer = new FileWriter(myFile);
//          bufferedWriter.write(xmlTestDocument);
//          bufferedWriter.close();
//          writer.close();
//       } catch (final IOException e) {
//          e.printStackTrace();
//       }
//    }

      JSONAssert.assertEquals(
            controlDocument,
            testJson,
            JSONCompareMode.LENIENT);
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
}
