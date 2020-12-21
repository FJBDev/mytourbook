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

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourTCX;
import net.tourbook.export.TourExporter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import utils.Initializer;

public class ExportTcxTester {

   private static final String IMPORT_PATH = "/exportdata/garmin/tcx/files/"; //$NON-NLS-1$

   public void compareTcxAgainstControl(final TourData testTourData,
                                        final String controlFileName) {
      assertEquals(true, false);
   }

   @Test
   void testTcxExport() {

      //TODO Fix : the pop up to confirm the overwrite of files doesn't seem to work anymore
      //
      final TourData tour = Initializer.importTour();
      final TourType tourType = new TourType();
      tourType.setName("Running");
      tour.setTourType(tourType);

      final TourExporter tourExporter = new TourExporter(
            ExportTourTCX.TCX_2_0_TEMPLATE,
            true, //final boolean useAbsoluteDistance,
            false, //final boolean isCamouflageSpeed,
            0, //final float camouflageSpeed,
            false, //final boolean isRange,
            0, //final int tourStartIndex,
            0, //final int tourEndIndex,
            false, //final boolean isExportWithBarometer,
            true, //final boolean useActivityType,
            tour.getTourType().getName(), //final String activityType,
            true, //final boolean useDescription,
            false, //final boolean isExportSurfingWaves,
            true, //final boolean isExportAllTourData,
            false, //final boolean isCourse,
            "").useTourData(tour);//final String courseName) {
      //TODO FB Maybe implement the setters as it will be useful when doing a test with setIsCamoufalge Speed.
      // instead of recreating the whole TouRExporter

      Assertions.assertTrue(tourExporter.export("/home/frederic/Desktop/toto.tcx"));
   }
}
