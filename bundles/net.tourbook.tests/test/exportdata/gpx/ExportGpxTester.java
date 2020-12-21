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
package exportdata.gpx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourGPX;
import net.tourbook.export.TourExporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.Initializer;

public class ExportGpxTester {

   private static final String IMPORT_PATH       = "test/exportdata/gpx/files/"; //$NON-NLS-1$
   private static final String _testTourFilePath = IMPORT_PATH + "GPXExport.gpx";

   private static TourExporter _tourExporter;

   @BeforeAll
   static void initAll() {
      //TODO Fix : the pop up to confirm the overwrite of files doesn't seem to work anymore
      //
      final TourData tour = Initializer.importTour();
      final TourType tourType = new TourType();
      tourType.setName("Running");
      tour.setTourType(tourType);

      _tourExporter = new TourExporter(ExportTourGPX.GPX_1_0_TEMPLATE).useTourData(tour);
      _tourExporter.setActivityType(tourType.getName());
   }

   @AfterEach
   void afterEach() {

      if (!Files.exists(Paths.get(_testTourFilePath))) {
         return;
      }
      try {
         Files.delete(Paths.get(_testTourFilePath));
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void executeTest(final String controlTourFileName) {

      _tourExporter.export(_testTourFilePath);

      Comparison.compareXmlAgainstControl(IMPORT_PATH + controlTourFileName, _testTourFilePath);
   }

   @Test
   void testGpxExportAllOptions() {

      final String controlTourFileName = "LongsPeak-AllOptions-RelativeDistance.gpx";

      _tourExporter.setUseAbsoluteDistance(true);

      executeTest(controlTourFileName);
   }

   @Test
   void testGpxExportDescriptionAndActivity() {

      final String controlTourFileName = "LongsPeak-AbsoluteDistance.gpx";

      _tourExporter.setUseAbsoluteDistance(true);

      executeTest(controlTourFileName);
   }
}
