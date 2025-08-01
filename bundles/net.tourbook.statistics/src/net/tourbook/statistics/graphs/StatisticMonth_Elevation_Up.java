/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

public class StatisticMonth_Elevation_Up extends StatisticMonth {

	@Override
	protected String getBarOrderingStateKey() {
		return STATE_BAR_ORDERING_MONTH_ELEVATION_UP;
	}

	@Override
	ChartDataModel getChartDataModel() {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		createXData_Months(chartDataModel);
      createYData_ElevationUp(chartDataModel);

		return chartDataModel;
	}

	@Override
	public String getGridPrefPrefix() {
		return GRID_MONTH_ELEVATION_UP;
	}

   @Override
   protected String getLayoutPrefPrefix() {
      return LAYOUT_MONTH_ELEVATION_UP;
   }
}
