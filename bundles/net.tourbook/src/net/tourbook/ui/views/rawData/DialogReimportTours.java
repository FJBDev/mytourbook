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
package net.tourbook.ui.views.rawData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.ReImport;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogState;
import net.tourbook.tour.TourLogView;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class DialogReimportTours extends TitleAreaDialog {

   private static final String   STATE_REIMPORT_TOURS_ALL        = "STATE_REIMPORT_TOURS_ALL";      //$NON-NLS-1$
   private static final String   STATE_REIMPORT_TOURS_SELECTED   = "STATE_REIMPORT_TOURS_SELECTED"; //$NON-NLS-1$

   private static final String   STATE_IS_IMPORT_ALTITUDE        = "isImportAltitude";              //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_CADENCE         = "isImportCadence";               //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_GEAR            = "isImportGear";                  //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_POWERANDPULSE   = "isImportPowerAndPulse";         //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_POWERANDSPEED   = "isImportPowerAndSpeed";         //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_RUNNINGDYNAMICS = "isImportRunningDynamics";       //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_SWIMMING        = "isImportSwimming";              //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TEMPERATURE     = "isImportTemperature";           //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TRAINING        = "isImportTraining";              //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TIMESLICES      = "isImportTimeSlices";            //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TOURMARKERS     = "isImportTourMarkers";           //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_TIMERPAUSES     = "isImportTimerPauses";           //$NON-NLS-1$
   private static final String   STATE_IS_IMPORT_ENTIRETOUR      = "isImportEntireTours";           //$NON-NLS-1$

   private static final int      VERTICAL_SECTION_MARGIN         = 10;

   private static String         _dlgDefaultMessage;

   private final IDialogSettings _state                          = TourbookPlugin
         .getState("DialogReimportTours");                                                          //$NON-NLS-1$

   private Point                 _shellDefaultSize;

   private final ITourViewer3    _tourViewer;
   //  private final ITourProviderByID _tourProvider;
   private PixelConverter        _pc;

   /*
    * UI controls
    */
   private Button    _chkAltitude;
   private Button    _chkCadence;
   private Button    _chkGear;
   private Button    _chkPowerAndPulse;
   private Button    _chkPowerAndSpeed;
   private Button    _chkRunningDynamics;
   private Button    _chkSwimming;
   private Button    _chkTemperature;
   private Button    _chkTraining;
   private Button    _chkTimeSlices;
   private Button    _chkTourMarkers;
   private Button    _chkTourTimerPauses;
   private Button    _chkEntireTour;

   private Button    _chkSkip_Tours_With_ImportFile_NotFound;

   private Button    _chkReimport_Tours_All;
   private Button    _chkReimport_Tours_Selected;

   private Composite _dlgContainer;
   private Composite _inputContainer;

   /**
    * @param parentShell
    */
   public DialogReimportTours(final Shell parentShell,
                              final ITourViewer3 tourViewer) {
      //final ITourProviderByID tourProvider) {

      //TODO FB pass the selected tours in case the user wants to reimport only for the selected tours

      super(parentShell);

      //  _tourProvider = tourProvider;
      _tourViewer = tourViewer;

      int shellStyle = getShellStyle();

      shellStyle = //
            SWT.NONE //
                  | SWT.TITLE
                  | SWT.CLOSE
                  | SWT.MIN
                  | SWT.RESIZE
                  | SWT.NONE;

      // make dialog resizable
      setShellStyle(shellStyle);
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText("Reimport Tours...");//TODO FB Messages.dialog_export_shell_text);

      shell.addListener(SWT.Resize, new Listener() {
         @Override
         public void handleEvent(final Event event) {

            // allow resizing the width but not the height

            if (_shellDefaultSize == null) {
               _shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            }

            final Point shellSize = shell.getSize();

            shellSize.x = shellSize.x < _shellDefaultSize.x ? _shellDefaultSize.x : shellSize.x;
            shellSize.y = _shellDefaultSize.y;

            shell.setSize(shellSize);
         }
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle("Reimport Tours...");//dialog_export_dialog_title);
      setMessage(_dlgDefaultMessage);

      restoreState();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      getButton(IDialogConstants.OK_ID).setText(Messages.dialog_reimport_tours_btn_reimport);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      _dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(_dlgContainer);

      return _dlgContainer;
   }

   private void createUI(final Composite parent) {

      _inputContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);
      {
         createUI_10_Tours(_inputContainer);
         createUI_20_Data(_inputContainer);

         /*
          * Checkbox: Skip tours for which the import file was not found
          */
         _chkSkip_Tours_With_ImportFile_NotFound = new Button(_inputContainer, SWT.CHECK);
         GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_chkSkip_Tours_With_ImportFile_NotFound);
         _chkSkip_Tours_With_ImportFile_NotFound.setText("Skip Tours for which the file is not found");//Messages.dialog_export_txt_filePath_tooltip);
         _chkSkip_Tours_With_ImportFile_NotFound.setToolTipText("When the original file is not found, skip to the next tour");
         _chkSkip_Tours_With_ImportFile_NotFound.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
      }
   }

   /**
    * UI to select either all the tours in the database or only the selected tours
    *
    * @param parent
    */
   private void createUI_10_Tours(final Composite parent) {

      final SelectionAdapter buttonListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            final Button btn = (Button) e.getSource();
            enableReimportButton(btn.getSelection());
         }
      };

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         final Group groupCustomGPX = new Group(container, SWT.NONE);
         groupCustomGPX.setText("Tours to reimport");//TODO FBMessages.Dialog_Export_Group_Custom);
         groupCustomGPX.setToolTipText(Messages.Dialog_Export_Group_Custom_Tooltip);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(groupCustomGPX);
         GridLayoutFactory.swtDefaults().numColumns(2).applyTo(groupCustomGPX);
         {
            /*
             * checkbox: Reimport all tours in the database
             */
            _chkReimport_Tours_All = new Button(groupCustomGPX, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkReimport_Tours_All);
            _chkReimport_Tours_All.setText("All tours in the database");//Messages.Dialog_Export_Checkbox_WithBarometer);
            _chkReimport_Tours_All.setToolTipText(Messages.Dialog_Export_Checkbox_WithBarometer_Tooltip);
            _chkReimport_Tours_All.addSelectionListener(buttonListener);

            /*
             * checkbox: Reimport the selected tours
             */
            _chkReimport_Tours_Selected = new Button(groupCustomGPX, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkReimport_Tours_Selected);
            _chkReimport_Tours_Selected.setText("Only the selected tours");//Messages.Dialog_Export_Checkbox_WithBarometer);
            _chkReimport_Tours_Selected.setToolTipText(Messages.Dialog_Export_Checkbox_WithBarometer_Tooltip);
            _chkReimport_Tours_Selected.addSelectionListener(buttonListener);
         }
      }
   }

   /**
    * UI to select the data to reimport for the chosen tours
    *
    * @param parent
    */
   private void createUI_20_Data(final Composite parent) {

      /*
       * group: filename
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText("Data to reimport");//TODO FBMessages.dialog_export_group_exportFileName);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         /*
          * checkbox: Entire Tour
          */
         _chkEntireTour = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .span(2, 1)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkEntireTour);
         _chkEntireTour.setText("Entire Tour");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkEntireTour.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);
         _chkEntireTour.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               final Button btn = (Button) e.getSource();
               enableDataButtons(!btn.getSelection());
            }
         });

         /*
          * checkbox: altitude
          */
         _chkAltitude = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkAltitude);
         _chkAltitude.setText("Altitude Values");//TODO FB Messages.dialog_export_chk_overwriteFiles);
         _chkAltitude.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: cadence
          */
         _chkCadence = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkCadence);
         _chkCadence.setText("Cadence Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkCadence.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Gear
          */
         _chkGear = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkGear);
         _chkGear.setText("Gear Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkGear.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Power And Pulse
          */
         _chkPowerAndPulse = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkPowerAndPulse);
         _chkPowerAndPulse.setText("Power and Pulse Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkPowerAndPulse.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Power And Speed
          */
         _chkPowerAndSpeed = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkPowerAndSpeed);
         _chkPowerAndSpeed.setText("Power And Speed Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkPowerAndSpeed.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Running Dynamics
          */
         _chkRunningDynamics = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkRunningDynamics);
         _chkRunningDynamics.setText("Running Dynamics Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkRunningDynamics.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Swimming
          */
         _chkSwimming = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkSwimming);
         _chkSwimming.setText("Swimming Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkSwimming.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Temperature
          */
         _chkTemperature = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkTemperature);
         _chkTemperature.setText("Temperature Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkTemperature.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Training
          */
         _chkTraining = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkTraining);
         _chkTraining.setText("Training Values");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkTraining.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Time slices
          */
         _chkTimeSlices = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkTimeSlices);
         _chkTimeSlices.setText("Time slices");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkTimeSlices.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Tour markers
          */
         _chkTourMarkers = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkTourMarkers);
         _chkTourMarkers.setText("Tour markers");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkTourMarkers.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);

         /*
          * checkbox: Timer Pauses
          */
         _chkTourTimerPauses = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkTourTimerPauses);
         _chkTourTimerPauses.setText("Timer Pauses");//TODO FBessages.dialog_export_chk_overwriteFiles);
         _chkTourTimerPauses.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);
      }

   }

   private void doReimport(final List<ReImport> reimportIds) throws IOException {

      final boolean isReimportAllTours = _chkReimport_Tours_All.getSelection();

      if (isReimportAllTours) {

         saveState();

         TourLogManager.addLog(
               TourLogState.DEFAULT, //
               NLS.bind(RawDataManager.LOG_REIMPORT_COMBINED_VALUES, "ddd"),
               TourLogView.CSS_LOG_TITLE);

         final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

            @Override
            public boolean computeTourValues(final TourData oldTourData) {

               TourData previousTourData = null;
               try {
                  previousTourData = (TourData) oldTourData.clone();

                  for (final ReImport reimportId : reimportIds) {
                     switch (reimportId) {

                     case AltitudeValues:

                        previousTourData.setTourAltDown(oldTourData.getTourAltDown());
                        previousTourData.setTourAltUp(oldTourData.getTourAltUp());

                        oldTourData.computeAltitudeUpDown();
                        TourLogManager.addLog(
                              TourLogState.DEFAULT, //
                              "TOURDATE",
                              TourLogView.CSS_LOG_TITLE);
                        RawDataManager.displayReimportDataDifferences(ReImport.AltitudeValues, previousTourData, oldTourData);


                        break;

                     case TourTimerPauses:
                        previousTourData.setTourDeviceTime_Paused(oldTourData.getTourDeviceTime_Paused());
                        previousTourData.setPausedTime_Start(oldTourData.getPausedTime_Start());
                        previousTourData.setPausedTime_End(oldTourData.getPausedTime_End());

                        break;

                     default:
                        break;
                     }
                  }

               } catch (final CloneNotSupportedException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }


               return true;//TODO FB
            }

            @Override
            public String getResultText() {

               return "";
            }

            @Override
            public String getSubTaskText(final TourData savedTourData) {

               return "";
            }
         };

         TourDatabase.computeAnyValues_ForAllTours(computeTourValueConfig, null);

         fireTourModifyEvent();

      } else {



         RawDataManager.getInstance().actionReimportTour(reimportIds, _tourViewer);
      }
   }

   private void enableDataButtons(final boolean isNotReimportEntireTour) {

      _chkAltitude.setEnabled(isNotReimportEntireTour);
      _chkCadence.setEnabled(isNotReimportEntireTour);
      _chkGear.setEnabled(isNotReimportEntireTour);
      _chkPowerAndPulse.setEnabled(isNotReimportEntireTour);
      _chkPowerAndSpeed.setEnabled(isNotReimportEntireTour);
      _chkRunningDynamics.setEnabled(isNotReimportEntireTour);
      _chkSwimming.setEnabled(isNotReimportEntireTour);
      _chkTemperature.setEnabled(isNotReimportEntireTour);
      _chkTraining.setEnabled(isNotReimportEntireTour);
      _chkTimeSlices.setEnabled(isNotReimportEntireTour);
      _chkTourMarkers.setEnabled(isNotReimportEntireTour);
      _chkTourTimerPauses.setEnabled(isNotReimportEntireTour);
   }
   private void enableReimportButton(final boolean isEnabled) {
      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   private void fireTourModifyEvent() {

      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      // keep window size and position
      return _state;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   protected void okPressed() {

      net.tourbook.ui.UI.disableAllControls(_inputContainer);

      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {
            try {

               final List<ReImport> reimportIds = new ArrayList<>();

               if (_chkEntireTour.getSelection()) {
                  reimportIds.add(ReImport.Tour);
               } else {

                  if (_chkAltitude.getSelection()) {
                     reimportIds.add(ReImport.AltitudeValues);
                  }
                  if (_chkCadence.getSelection()) {
                     reimportIds.add(ReImport.CadenceValues);
                  }
                  if (_chkGear.getSelection()) {
                     reimportIds.add(ReImport.GearValues);
                  }
                  if (_chkPowerAndPulse.getSelection()) {
                     reimportIds.add(ReImport.PowerAndPulseValues);
                  }
                  if (_chkPowerAndSpeed.getSelection()) {
                     reimportIds.add(ReImport.PowerAndSpeedValues);
                  }
                  if (_chkRunningDynamics.getSelection()) {
                     reimportIds.add(ReImport.RunningDynamics);
                  }
                  if (_chkSwimming.getSelection()) {
                     reimportIds.add(ReImport.Swimming);
                  }
                  if (_chkTemperature.getSelection()) {
                     reimportIds.add(ReImport.TemperatureValues);
                  }
                  if (_chkTraining.getSelection()) {
                     reimportIds.add(ReImport.TrainingValues);
                  }
                  if (_chkTimeSlices.getSelection()) {
                     reimportIds.add(ReImport.TimeSlices);
                  }
                  if (_chkTourMarkers.getSelection()) {
                     reimportIds.add(ReImport.TourMarkers);
                  }
                  if (_chkTourTimerPauses.getSelection()) {
                     reimportIds.add(ReImport.TourTimerPauses);
                  }
               }

               doReimport(reimportIds);

            } catch (final IOException e) {
               StatusUtil.log(e);
            }
         }
      });

      super.okPressed();
   }

   private void restoreState() {

      // Data to reimport
      final boolean isReimportEntireTour = _state.getBoolean(STATE_IS_IMPORT_ENTIRETOUR);
      _chkEntireTour.setSelection(isReimportEntireTour);
      _chkAltitude.setSelection(_state.getBoolean(STATE_IS_IMPORT_ALTITUDE));
      _chkCadence.setSelection(_state.getBoolean(STATE_IS_IMPORT_CADENCE));
      _chkGear.setSelection(_state.getBoolean(STATE_IS_IMPORT_GEAR));
      _chkPowerAndPulse.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWERANDPULSE));
      _chkPowerAndSpeed.setSelection(_state.getBoolean(STATE_IS_IMPORT_POWERANDSPEED));
      _chkRunningDynamics.setSelection(_state.getBoolean(STATE_IS_IMPORT_RUNNINGDYNAMICS));
      _chkSwimming.setSelection(_state.getBoolean(STATE_IS_IMPORT_SWIMMING));
      _chkTemperature.setSelection(_state.getBoolean(STATE_IS_IMPORT_TEMPERATURE));
      _chkTraining.setSelection(_state.getBoolean(STATE_IS_IMPORT_TRAINING));
      _chkTimeSlices.setSelection(_state.getBoolean(STATE_IS_IMPORT_TIMESLICES));
      _chkTourMarkers.setSelection(_state.getBoolean(STATE_IS_IMPORT_TOURMARKERS));
      _chkTourTimerPauses.setSelection(_state.getBoolean(STATE_IS_IMPORT_TIMERPAUSES));

      final boolean isReimportAllTours = _state.getBoolean(STATE_REIMPORT_TOURS_ALL);
      _chkReimport_Tours_All.setSelection(isReimportAllTours);
      final boolean isReimportSelectedTours = _state.getBoolean(STATE_REIMPORT_TOURS_SELECTED);
      _chkReimport_Tours_Selected.setSelection(isReimportSelectedTours);

      enableDataButtons(!isReimportEntireTour);
      enableReimportButton(isReimportAllTours || isReimportSelectedTours);
   }

   private void saveState() {

      _state.put(STATE_REIMPORT_TOURS_ALL, _chkReimport_Tours_All.getSelection());
      _state.put(STATE_REIMPORT_TOURS_SELECTED, _chkReimport_Tours_Selected.getSelection());

      // Data to import
      _state.put(STATE_IS_IMPORT_ENTIRETOUR, _chkEntireTour.getSelection());
      _state.put(STATE_IS_IMPORT_ALTITUDE, _chkAltitude.getSelection());
      _state.put(STATE_IS_IMPORT_CADENCE, _chkCadence.getSelection());
      _state.put(STATE_IS_IMPORT_GEAR, _chkGear.getSelection());
      _state.put(STATE_IS_IMPORT_POWERANDPULSE, _chkPowerAndPulse.getSelection());
      _state.put(STATE_IS_IMPORT_POWERANDSPEED, _chkPowerAndSpeed.getSelection());
      _state.put(STATE_IS_IMPORT_RUNNINGDYNAMICS, _chkRunningDynamics.getSelection());
      _state.put(STATE_IS_IMPORT_SWIMMING, _chkSwimming.getSelection());
      _state.put(STATE_IS_IMPORT_TEMPERATURE, _chkTemperature.getSelection());
      _state.put(STATE_IS_IMPORT_TRAINING, _chkTraining.getSelection());
      _state.put(STATE_IS_IMPORT_TIMESLICES, _chkTimeSlices.getSelection());
      _state.put(STATE_IS_IMPORT_TOURMARKERS, _chkTourMarkers.getSelection());
      _state.put(STATE_IS_IMPORT_TIMERPAUSES, _chkTourTimerPauses.getSelection());
   }
}
