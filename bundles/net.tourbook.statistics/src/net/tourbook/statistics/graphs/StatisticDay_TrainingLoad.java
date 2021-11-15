/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.statistics.graphs;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartType;
import net.tourbook.statistic.ChartOptions_TrainingStress;
import net.tourbook.statistic.SlideoutStatisticOptions;

public class StatisticDay_TrainingLoad extends StatisticDay {

   @Override
   ChartDataModel getChartDataModel() {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.LINE_WITH_BARS);
//      chartDataModel.setIsGraphOverlapped(true);
      //TODO FB Why does the weird cursor appear ?
      createXDataDay(chartDataModel);
      createYData_PredictedPerformance(chartDataModel);
      createYData_TrainingStress(chartDataModel);

      return chartDataModel;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_DAY_TRAININGLOAD;
   }

   @Override
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      slideout.setStatisticOptions(new ChartOptions_TrainingStress());
   }
}
