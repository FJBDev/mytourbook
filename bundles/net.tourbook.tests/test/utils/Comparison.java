package utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.tourbook.data.TourData;

import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class Comparison {

   private static final String JSON = ".json";

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

      final String testJson = testTourData.toJson();

      // When using Java 11, convert the line below to the Java 11 method
      //String controlDocument = Files.readString(controlDocumentFilePath, StandardCharsets.US_ASCII);

      final String controlDocumentFilePath = Paths.get(controlFileName + JSON).toAbsolutePath().toString();
      final String controlDocument = readFile(controlDocumentFilePath, StandardCharsets.US_ASCII);

//      BufferedWriter bufferedWriter = null;
//      final File myFile = new File(
//            controlFileName + "-Erroneous.json"); //$NON-NLS-1$
//      // check if file exist, otherwise create the file before writing
//      if (!myFile.exists()) {
//         try {
//            myFile.createNewFile();
//            Writer writer = new FileWriter(myFile);
//            bufferedWriter = new BufferedWriter(writer);
//            writer = new FileWriter(myFile);
//            bufferedWriter.write(testJson);
//            bufferedWriter.close();
//            writer.close();
//         } catch (final IOException e) {
//            e.printStackTrace();
//         }
//      }

      JSONAssert.assertEquals(
            controlDocument,
            testJson,
            new CustomComparator(JSONCompareMode.LENIENT,
                  new Customization("importFilePathName", (o1, o2) -> true),
                  new Customization("importFilePathNameText", (o1, o2) -> true),
                  new Customization("importFilePath", (o1, o2) -> true)));
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
