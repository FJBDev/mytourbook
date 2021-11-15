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

import net.tourbook.application.TourbookPlugin;

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
   private Button                 _chkShowSkibaModel;
   private Button                 _chkShowDeviceModel;

   private TrainingStressPrefKeys _prefKeys;

   public ChartOptions_TrainingStress(final TrainingStressPrefKeys prefKeys) {

      _prefKeys = prefKeys;
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
            _chkShowDeviceModel = new Button(group, SWT.CHECK);
            _chkShowDeviceModel.setText("Device");//Messages.Pref_Statistic_Checkbox_Distance);
            _chkShowDeviceModel.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show altitude
             */
            _chkShowSkibaModel = new Button(group, SWT.CHECK);
            _chkShowSkibaModel.setText("TRIMP");//Messages.Pref_Statistic_Checkbox_Altitude);
            _chkShowSkibaModel.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void enableControls() {

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

      _chkShowSkibaModel.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Device));
      _chkShowDeviceModel.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Skiba));

      enableControls();
   }

   @Override
   public void restoreState() {

      _chkShowDeviceModel.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Device));
      _chkShowSkibaModel.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Skiba));

      enableControls();
   }

   @Override
   public void saveState() {

      _prefStore.setValue(_prefKeys.isShow_Device, _chkShowDeviceModel.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Skiba, _chkShowSkibaModel.getSelection());

   }
}
