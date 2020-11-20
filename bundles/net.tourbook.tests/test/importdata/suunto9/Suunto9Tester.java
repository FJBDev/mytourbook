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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.data.TourData;
import net.tourbook.data.TourWayPoint;
import net.tourbook.device.suunto.Suunto9DeviceDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.tour.TourManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class Suunto9Tester {

   private static SAXParser   parser;

   /**
    * Resource path to GPX file, generally available from net.tourbook Plugin in test/net.tourbook
    */
   public static final String IMPORT_FILE_PATH = "/importdata/suunto9/files/1536723722706_183010004848_post_timeline-1.json.gz"; //$NON-NLS-1$

   @BeforeAll
   static void setUp() throws ParserConfigurationException, SAXException {
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      parser = factory.newSAXParser();
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
   }

   /**
    * Regression test. Imports GPX into TourData and checks all values of tour1 and waypoint1.
    *
    * @throws ParserConfigurationException
    * @throws IOException
    * @throws SAXException
    */
   @Test
   void testParse() throws SAXException, IOException {

      final DeviceData deviceData = new DeviceData();
      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();

      final Suunto9DeviceDataReader handler = new Suunto9DeviceDataReader();
      handler.processDeviceData(Paths.get(IMPORT_FILE_PATH).getFileName(), deviceData, alreadyImportedTours, newlyImportedTours);

      final TourData tour = newlyImportedTours.get(Long.valueOf(201010101005999L));
tour.toXml() compare final with the local xml (take the code from the suunto9-unittests branch)
      final Set<TourWayPoint> tourWayPoints = tour.getTourWayPoints();

      final Integer numWayPoints = 2;
      assert numWayPoints.equals(tourWayPoints.size());
      final Iterator<TourWayPoint> iter = tourWayPoints.iterator();

      TourWayPoint waypoint1 = iter.next();
      final String wayPoint = "waypoint1";
      if (!wayPoint.equals(waypoint1.getName())) {
         waypoint1 = iter.next();
      }
      final Double latitude = waypoint1.getLatitude();
      assert latitude.equals(47.5674099);
      final Double longitude = waypoint1.getLongitude();
      assert longitude.equals(9.47741068);
      final Float altitude = waypoint1.getAltitude();
      assert altitude.equals(394f);
      final Long time = waypoint1.getTime();
      assert time.equals(1286706600000L);
      final String name = waypoint1.getName();
      assert name.equals(wayPoint);
      final String comment = waypoint1.getComment();
      assert comment.equals(wayPoint);
      final String description = waypoint1.getDescription();
      assert description.equals(wayPoint);

      final String track1 = tour.getTourTitle();
      assert track1.equals("track1");

      final float[] altitudeSerie = tour.altitudeSerie;
      assert Arrays.equals(altitudeSerie, new float[] { 395, 394, 393, 394 });
      final String tourDateFull = TourManager.getTourDateFull(tour);
      assert tourDateFull.equals("2010-10-10T12:00:00.000+02:00"); //$NON-NLS-1$
      final Integer timeSerieLength = tour.timeSerie.length;
      assert timeSerieLength.equals(4);
      final int[] timeSerie = tour.timeSerie;
      assert Arrays.equals(timeSerie, new int[] { 0, 600, 1200, 1800 });

      final double[] latitudeSerie = tour.latitudeSerie;
      final double[] expectedLat = new double[] { 47.567517620, 47.585505568, 47.585397817, 47.567409937 };
      assert Arrays.equals(latitudeSerie, expectedLat);
      final double[] longitudeSerie = tour.longitudeSerie;
      final double[] expectedLon = new double[] { 9.450832423, 9.450987056, 9.477574415, 9.477410677 };
      assert Arrays.equals(longitudeSerie, expectedLon);
      final float[] temperatureSerie = tour.temperatureSerie;
      assert Arrays.equals(temperatureSerie, new float[] { 20, 21, 22, 21 });
      final float[] pulseSerie = tour.pulseSerie;
      assert Arrays.equals(pulseSerie, new float[] { 110, 120, 130, 120 });
      final float[] cadenceSerie = tour.getCadenceSerie();
      assert Arrays.equals(cadenceSerie, new float[] { 40, 50, 60, 50 });
   }
}
