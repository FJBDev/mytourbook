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
package net.tourbook.statistic;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import de.byteholder.geoclipse.map.UI;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

public class ChartOptions_TrainingStress implements IStatisticOptions {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button           _chkShow_TrainingEffect;
   private Button           _chkShow_TrainingEffect_Anaerobic;
   private Button           _chkShow_TrainingPerformance;
   private Button           _chkShow_TrainingPerformance_AvgValue;

   private Button           _chkShowAltitude;
   private Button           _chkShowDistance;
   private Button           _chkShowDuration;
   private Button           _chkShowAvgSpeed;
   private Button           _chkShowAvgPace;

   private Button           _rdoDuration_ElapsedTime;
   private Button           _rdoDuration_RecordedTime;
   private Button           _rdoDuration_PausedTime;
   private Button           _rdoDuration_MovingTime;
   private Button           _rdoDuration_BreakTime;

   private TrainingPrefKeys _prefKeys;

   public ChartOptions_TrainingStress() {

   }

   @Override
   public void createUI(final Composite parent) {

      initUI();

      createUI_10_Model(parent);

   }

   private void createUI_10_Model(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText("Model");//Messages.Pref_Statistic_Group_Training);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         {
            /*
             * Show distance
             */
            _chkShowDistance = new Button(group, SWT.CHECK);
            _chkShowDistance.setText("Device");//Messages.Pref_Statistic_Checkbox_Distance);
            _chkShowDistance.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show altitude
             */
            _chkShowAltitude = new Button(group, SWT.CHECK);
            _chkShowAltitude.setText("TRIMP");//Messages.Pref_Statistic_Checkbox_Altitude);
            _chkShowAltitude.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void enableControls() {

      final boolean isShowDuration = _chkShowDuration.getSelection();
      final boolean isShowTrainingPerformance = _chkShow_TrainingPerformance.getSelection();

      _chkShow_TrainingPerformance_AvgValue.setEnabled(isShowTrainingPerformance);

      _rdoDuration_MovingTime.setEnabled(isShowDuration);
      _rdoDuration_BreakTime.setEnabled(isShowDuration);
      _rdoDuration_ElapsedTime.setEnabled(isShowDuration);
      _rdoDuration_RecordedTime.setEnabled(isShowDuration);
      _rdoDuration_PausedTime.setEnabled(isShowDuration);
   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   private void onChangeUI() {

      // update chart async (which is done when a pref store value is modified) that the UI is updated immediately

      enableControls();

      Display.getCurrent().asyncExec(this::saveState);
   }

   @Override
   public void resetToDefaults() {

      _chkShowAltitude.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Altitude));
      _chkShowAvgPace.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Avg_Pace));
      _chkShowAvgSpeed.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Avg_Speed));
      _chkShowDistance.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Distance));
      _chkShowDuration.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Duration));

      final Enum<DurationTime> durationTime = Util.getEnumValue(_prefStore.getDefaultString(_prefKeys.durationTime), DurationTime.MOVING);
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));

      _chkShow_TrainingEffect.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingPerformance));
      _chkShow_TrainingPerformance_AvgValue.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingPerformance_AvgValue));

      enableControls();
   }

   @Override
   public void restoreState() {

      _chkShowAltitude.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Altitude));
      _chkShowAvgPace.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Avg_Pace));
      _chkShowAvgSpeed.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Avg_Speed));
      _chkShowDistance.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Distance));
      _chkShowDuration.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Duration));

      final Enum<DurationTime> durationTime = Util.getEnumValue(_prefStore.getString(_prefKeys.durationTime), DurationTime.MOVING);
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));

      _chkShow_TrainingEffect.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingPerformance));
      _chkShow_TrainingPerformance_AvgValue.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingPerformance_AvgValue));

      enableControls();
   }

   @Override
   public void saveState() {

      _prefStore.setValue(_prefKeys.isShow_Altitude, _chkShowAltitude.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Avg_Pace, _chkShowAvgPace.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Avg_Speed, _chkShowAvgSpeed.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Distance, _chkShowDistance.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Duration, _chkShowDuration.getSelection());

      // duration time
      String selectedDurationType = UI.EMPTY_STRING;

      if (_rdoDuration_BreakTime.getSelection()) {
         selectedDurationType = DurationTime.BREAK.name();
      } else if (_rdoDuration_MovingTime.getSelection()) {
         selectedDurationType = DurationTime.MOVING.name();
      } else if (_rdoDuration_RecordedTime.getSelection()) {
         selectedDurationType = DurationTime.RECORDED.name();
      } else if (_rdoDuration_PausedTime.getSelection()) {
         selectedDurationType = DurationTime.PAUSED.name();
      } else if (_rdoDuration_ElapsedTime.getSelection()) {
         selectedDurationType = DurationTime.ELAPSED.name();
      }

      _prefStore.setValue(_prefKeys.durationTime, selectedDurationType);

      _prefStore.setValue(_prefKeys.isShow_TrainingEffect, _chkShow_TrainingEffect.getSelection());
      _prefStore.setValue(_prefKeys.isShow_TrainingEffect_Anaerobic, _chkShow_TrainingEffect_Anaerobic.getSelection());
      _prefStore.setValue(_prefKeys.isShow_TrainingPerformance, _chkShow_TrainingPerformance.getSelection());
      _prefStore.setValue(_prefKeys.isShow_TrainingPerformance_AvgValue, _chkShow_TrainingPerformance_AvgValue.getSelection());
   }
}
