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
package net.tourbook.cloud.dropbox;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class DropboxPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {


   private final IPreferenceStore _prefStore   = TourbookPlugin.getDefault().getPreferenceStore();

   /*
    * UI controls
    */
   private Group _groupData;

   private Combo _comboDistanceDataSource;

   private Label _lblAltitudeDataSource;
   private Label _lblDistanceDataSource;

   @Override
   protected void createFieldEditors() {

      createUI();

      setupUI();

   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      /*
       * Data
       */
      _groupData = new Group(parent, SWT.NONE);
      _groupData.setText("dd");//Messages.pref_data_source);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupData);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupData);
      {
         // label: Altitude data source
         _lblAltitudeDataSource = new Label(_groupData, SWT.NONE);
         _lblAltitudeDataSource.setText("Authorize");
         /*
          * combo: Altitude source
          */
         // button: update map
         final Button _btnShowMap = new Button(_groupData, SWT.NONE);
         _btnShowMap.setText("click");
         _btnShowMap.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }

         });

         // label: Distance data source
         _lblDistanceDataSource = new Label(_groupData, SWT.NONE);
         _lblDistanceDataSource.setText("");//Messages.pref_distance_source);

         /*
          * combo: Distance source
          */
         _comboDistanceDataSource = new Combo(_groupData, SWT.READ_ONLY | SWT.BORDER);
         _comboDistanceDataSource.setVisibleItemCount(2);
      }
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void onClickAuthorize() {
      // TODO Auto-generated method stub
      System.out.print("dd");
   }

   @Override
   protected void performDefaults() {

   }
   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      /*
       * if (isOK) {
       * _prefStore.setValue(IPreferences.ALTITUDE_DATA_SOURCE,
       * _comboAltitudeDataSource.getSelectionIndex());
       * _prefStore.setValue(IPreferences.DISTANCE_DATA_SOURCE,
       * _comboDistanceDataSource.getSelectionIndex());
       * }
       */
      return isOK;
   }

   private void setupUI() {

   }

}
