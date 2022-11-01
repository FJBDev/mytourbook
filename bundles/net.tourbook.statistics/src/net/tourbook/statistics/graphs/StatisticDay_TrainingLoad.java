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
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ChartOptions_TrainingLoad;
import net.tourbook.statistic.SlideoutStatisticOptions;
import net.tourbook.statistic.TrainingStressPrefKeys;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public class StatisticDay_TrainingLoad extends StatisticDay {

   private IPropertyChangeListener _statDay_PrefChangeListener;
   private boolean                 _isShowDistance;
   private boolean                 _isShowAltitude;

   private void addPrefListener(final Composite container) {

      // create pref listener
      _statDay_PrefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         // observe which data are displayed
         if (property.equals(ITourbookPreferences.STAT_TRAINING_MODEL_DEVICE)
               || property.equals(ITourbookPreferences.STAT_TRAINING_MODEL_TRIMP)

               || property.equals(ITourbookPreferences.STAT_TRAINING_MODEL_SKIBA)

         ) {

               _isDuration_ReloadData = true;

            // get the changed preferences
            getPreferences();

            // update chart
            preferencesHasChanged();
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_statDay_PrefChangeListener);

      // remove pref listener
      container.addDisposeListener(disposeEvent -> _prefStore.removePropertyChangeListener(_statDay_PrefChangeListener));
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      super.createStatisticUI(parent, viewSite);

      addPrefListener(parent);
      getPreferences();
   }

   @Override
   ChartDataModel getChartDataModel() {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.LINE_WITH_BARS);
//      chartDataModel.setIsGraphOverlapped(true);
      //TODO FB Why does the weird cursor appear ?
      createXDataDay(chartDataModel);
      if (_prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_MODEL_DEVICE)) {
         createYData_PredictedPerformance(chartDataModel);
      }
      if (_prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_MODEL_SKIBA)) {
      createYData_TrainingStress(chartDataModel);
      }

      return chartDataModel;
   }
   @Override
   protected String getGridPrefPrefix() {
      return GRID_DAY_TRAININGLOAD;
   }

   private void getPreferences() {

      _isShowAltitude = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_MODEL_DEVICE);

      _isShowDistance = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_MODEL_SKIBA);
   }

   @Override
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      final TrainingStressPrefKeys prefKeys = new TrainingStressPrefKeys();

      prefKeys.isShow_Device = ITourbookPreferences.STAT_TRAINING_MODEL_DEVICE;
      prefKeys.isShow_Skiba = ITourbookPreferences.STAT_TRAINING_MODEL_SKIBA;

      slideout.setStatisticOptions(new ChartOptions_TrainingLoad(prefKeys));
   }
}
