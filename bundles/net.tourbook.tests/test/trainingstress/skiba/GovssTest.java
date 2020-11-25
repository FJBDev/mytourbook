package trainingstress.skiba;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.device.garmin.GarminDeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;
import net.tourbook.trainingstress.Govss;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Initializer;

class GovssTest {

   private static TourPerson  tourPerson;
   private static SAXParser               parser;
   private static DeviceData              deviceData;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static GarminDeviceDataReader  deviceDataReader;

   /**
    * Resource path to GPX file, generally available from net.tourbook Plugin in test/net.tourbook
    */
   public static final String IMPORT_FILE_PATH = "/trainingstress/skiba/files/Move_2017_09_30_05_36_06_Trail+running-MtWhitney.gpx"; //$NON-NLS-1$

   @BeforeAll
   static void setUp() throws ParserConfigurationException, SAXException {

      tourPerson = new TourPerson();
      tourPerson.setGovssThresholdPower(367);
      tourPerson.setHeight(1.84f);
      tourPerson.setWeight(70);
      tourPerson.setGovssTimeTrialDuration(3600);

      parser = Initializer.initializeParser();
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new GarminDeviceDataReader();
   }

   @Test
   void testComputeGovss() throws SAXException, IOException {

      final InputStream gpx = GovssTest.class.getResourceAsStream(IMPORT_FILE_PATH);

      final GPX_SAX_Handler gpxSaxHandler = new GPX_SAX_Handler(
            deviceDataReader,
            IMPORT_FILE_PATH,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours);

      parser.parse(gpx, gpxSaxHandler);

      final TourData tour = newlyImportedTours.get(Long.valueOf(2017930123618648L));

      final Integer govss = new Govss(tourPerson, tour).Compute();
      assert govss.equals(114);
   }
}
