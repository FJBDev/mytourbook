/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChartConfiguration;

/**
 * Contains data and configuration for the elevation compared tour
 */
public class ElevationCompareConfig {

   private TourReference                 _refTour;
   private Long                          _refTourTourId;

   private TourChartConfiguration        _refTourChartConfig;
   private TourChartConfiguration        _compareTourChartConfig;

   private SelectionChartXSliderPosition _xSliderPosition;

   public boolean                        isGeoCompareRefTour;

   ElevationCompareConfig(final TourReference refTour,
                          final ChartDataModel refChartDataModel,
                          final Long refTourTourId,
                          final TourChartConfiguration refTourChartConfig,
                          final TourChartConfiguration compTourChartConfig) {

      _refTour = refTour;
      _refTourTourId = refTourTourId;

      _refTourChartConfig = refTourChartConfig;
      _compareTourChartConfig = compTourChartConfig;
   }

   TourChartConfiguration getCompareTourChartConfig() {
      return _compareTourChartConfig;
   }

   public TourReference getRefTour() {
      return _refTour;
   }

   TourChartConfiguration getRefTourChartConfig() {
      return _refTourChartConfig;
   }

   public TourData getRefTourData() {

      /*
       * ensure to have the correct tour data, load tour data because tour data in the ref tour
       * could be changed, this is a wrong concept which could be changed but requires additonal
       * work
       */
      return TourManager.getInstance().getTourData(_refTourTourId);
   }

   SelectionChartXSliderPosition getXSliderPosition() {
      return _xSliderPosition;
   }

   void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {
      _xSliderPosition = sliderPosition;
   }

   @Override
   public String toString() {

      return "TourCompareConfig [" //$NON-NLS-1$

            + "_refTour                =" + _refTour + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "_refTourTourId          =" + _refTourTourId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "_refTourChartConfig     =" + _refTourChartConfig + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "_compareTourChartConfig =" + _compareTourChartConfig + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "_xSliderPosition        =" + _xSliderPosition + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "isGeoCompareRefTour     =" + isGeoCompareRefTour //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
