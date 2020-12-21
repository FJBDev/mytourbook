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
package exportdata.garmin.tcx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourTCX;
import net.tourbook.export.TourExporter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import utils.Comparison;
import utils.Initializer;

public class ExportTcxTester {

   private static final String IMPORT_PATH       = "test/exportdata/garmin/tcx/files/"; //$NON-NLS-1$
   private static final String _testTourFilePath = IMPORT_PATH + "TCXExport.tcx";

   private static TourExporter _tourExporter;

   @BeforeAll
   static void initAll() {
      //TODO Fix : the pop up to confirm the overwrite of files doesn't seem to work anymore
      //
      final TourData tour = Initializer.importTour();
      final TourType tourType = new TourType();
      tourType.setName("Running");
      tour.setTourType(tourType);

      _tourExporter = new TourExporter(ExportTourTCX.TCX_2_0_TEMPLATE).useTourData(tour);
      _tourExporter.setActivityType(tourType.getName());
   }

   private void compareTcxAgainstControl(final String controlTourFileName) {

      _tourExporter.export(_testTourFilePath);

      final String controlTour = Comparison.readFileContent(IMPORT_PATH + controlTourFileName);
      final String testTour = Comparison.readFileContent(_testTourFilePath);

      final Diff documentDiff = DiffBuilder
            .compare(controlTour)
            .withTest(testTour)
//            .withNodeFilter(node -> !node.getNodeName().equals("someName"))
            .build();

      try {
         Files.delete(Paths.get(_testTourFilePath));
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      Assertions.assertFalse(documentDiff.hasDifferences(), documentDiff.toString());
   }

   @Test
   void testTcxExportDescriptionAndActivity() {

      final String controlTourFileName = "LongsPeak-Description-RunningActivity.tcx";

      _tourExporter.setUseAbsoluteDistance(true);
      _tourExporter.setUseDescription(true);
      _tourExporter.setUseActivityType(true);
      _tourExporter.setIsExportAllTourData(true);

      //TODO FB Maybe implement the setters as it will be useful when doing a test with setIsCamoufalge Speed.
      // instead of recreating the whole TouRExporter

      compareTcxAgainstControl(controlTourFileName);
   }
}
