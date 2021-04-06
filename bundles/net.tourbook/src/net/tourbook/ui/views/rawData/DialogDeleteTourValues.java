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
package net.tourbook.ui.views.rawData;

import de.byteholder.geoclipse.map.UI;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.TourValueType;
import net.tourbook.importdata.ReImportStatus;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogState;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.collateTours.CollatedToursView;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

public class DialogDeleteTourValues extends TitleAreaDialog {

   //TODO FB delete values ? delete data ? Values because of Adjust TOur Values

   private static final String          STATE_DELETE_TOURVALUES_ALL                 = "STATE_DELETE_TOURVALUES_ALL";                  //$NON-NLS-1$
   private static final String          STATE_DELETE_TOURVALUES_SELECTED            = "STATE_DELETE_TOURVALUES_SELECTED";             //$NON-NLS-1$

   private static final String          STATE_DELETE_TOURVALUES_BETWEEN_DATES       = "STATE_DELETE_TOURVALUES_BETWEEN_DATES";        //$NON-NLS-1$
   private static final String          STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM  = "STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM";   //$NON-NLS-1$
   private static final String          STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL = "STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL";  //$NON-NLS-1$

   private static final String          STATE_IS_DELETE_ALL_TIME_SLICES             = "STATE_IS_IMPORT_ALL_TIME_SLICES";              //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_CADENCE                     = "STATE_IS_DELETE_CADENCE";                      //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_ELEVATION                   = "STATE_IS_DELETE_ELEVATION";                    //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_GEAR                        = "STATE_IS_DELETE_GEAR";                         //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_POWER_AND_PULSE             = "STATE_IS_DELETE_POWER_AND_PULSE";              //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_POWER_AND_SPEED             = "STATE_IS_DELETE_POWER_AND_SPEED";              //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_RUNNING_DYNAMICS            = "STATE_IS_DELETE_RUNNING_DYNAMICS";             //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_SWIMMING                    = "STATE_IS_DELETE_SWIMMING";                     //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TEMPERATURE                 = "STATE_IS_DELETE_TEMPERATURE";                  //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TRAINING                    = "STATE_IS_DELETE_TRAINING";                     //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TOUR_MARKERS                = "STATE_IS_DELETE_TOUR_MARKERS";                 //$NON-NLS-1$
   private static final String          STATE_IS_DELETE_TIMER_PAUSES                = "STATE_IS_DELETE_TIMER_PAUSES";                 //$NON-NLS-1$

   private static final int             VERTICAL_SECTION_MARGIN                     = 10;

   private static final IDialogSettings _state                                      = TourbookPlugin.getState("DialogReimportTours"); //$NON-NLS-1$

   private final ITourViewer3           _tourViewer;

   private SelectionAdapter             _defaultListener;

   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _btnDeselectAll;

   private Button    _chkData_AllTimeSlices;
   private Button    _chkData_Cadence;
   private Button    _chkData_Elevation;
   private Button    _chkData_Gear;
   private Button    _chkData_PowerAndPulse;
   private Button    _chkData_PowerAndSpeed;
   private Button    _chkData_RunningDynamics;
   private Button    _chkData_Swimming;
   private Button    _chkData_Temperature;
   private Button    _chkData_Training;
   private Button    _chkData_TourMarkers;
   private Button    _chkData_TourTimerPauses;

   private Button    _rdoDeleteTourValues_Tours_All;
   private Button    _rdoDeleteTourValues_Tours_BetweenDates;
   private Button    _rdoDeleteTourValues_Tours_Selected;

   private DateTime  _dtTourDate_From;
   private DateTime  _dtTourDate_Until;

   public DialogDeleteTourValues(final Shell parentShell,
                                 final ITourViewer3 tourViewer) {

      super(parentShell);

      _tourViewer = tourViewer;
   }

//   private boolean actionReimportTour_30(final List<TourValueType> tourValueTypes,
//                                         final File reimportedFile,
//                                         final TourData oldTourData) {
//
//      final boolean isTourReImported = false;
//
//      final Long oldTourId = oldTourData.getTourId();
//      final String reimportFileNamePath = reimportedFile.getAbsolutePath();
//
//      /*
//       * tour must be removed otherwise it would be recognized as a duplicate and therefore not
//       * imported
//       */
////      final TourData oldTourDataInImportView = _toursInImportView.remove(oldTourId);
//
//      if (importRawData(reimportedFile, null, false, null, false)) {
//
//         /*
//          * tour(s) could be re-imported from the file, check if it contains a valid tour
//          */
//
//         TourData clonedTourData = null;
//
//         try {
//
//            clonedTourData = (TourData) oldTourData.clone();
//
//            // loop: For each tour value type, we save the associated data for future display
//            //to compare with the new data
//            for (final TourValueType tourValueType : tourValueTypes) {
//
//               switch (tourValueType) {
//
//               case TOUR_MARKER:
//                  clonedTourData.setTourMarkers(new HashSet<>(oldTourData.getTourMarkers()));
//                  break;
//
//               //
//
//               case TIME_SLICES_CADENCE:
//                  clonedTourData.setAvgCadence(oldTourData.getAvgCadence());
//                  clonedTourData.setCadenceMultiplier(oldTourData.getCadenceMultiplier());
//                  break;
//
//               case TIME_SLICES_ELEVATION:
//                  clonedTourData.setTourAltDown(oldTourData.getTourAltDown());
//                  clonedTourData.setTourAltUp(oldTourData.getTourAltUp());
//                  break;
//
//               case TIME_SLICES_GEAR:
//                  clonedTourData.setFrontShiftCount(oldTourData.getFrontShiftCount());
//                  clonedTourData.setRearShiftCount(oldTourData.getRearShiftCount());
//                  break;
//
//               case TIME_SLICES_POWER_AND_PULSE:
//                  clonedTourData.setPower_Avg(oldTourData.getPower_Avg());
//                  clonedTourData.setAvgPulse(oldTourData.getAvgPulse());
//                  clonedTourData.setCalories(oldTourData.getCalories());
//                  break;
//
//               case TIME_SLICES_POWER_AND_SPEED:
//                  clonedTourData.setPower_Avg(oldTourData.getPower_Avg());
//                  clonedTourData.setCalories(oldTourData.getCalories());
//                  break;
//
//               case TIME_SLICES_TEMPERATURE:
//                  clonedTourData.setAvgTemperature(oldTourData.getAvgTemperature());
//                  break;
//
//               case TIME_SLICES_TIMER_PAUSES:
//                  clonedTourData.setTourDeviceTime_Paused(oldTourData.getTourDeviceTime_Paused());
//                  break;
//
//               default:
//                  break;
//               }
//            }
//         } catch (final CloneNotSupportedException e) {
//            StatusUtil.log(e);
//         }
//
////         TourData updatedTourData = actionReimportTour_40(tourValueTypes, reimportedFile, oldTourData);
//
////         if (updatedTourData == null) {
////
////            // error is already logged
////
////         } else {
////
////            isTourReImported = true;
////
////            // set re-import file path as new location
////            updatedTourData.setImportFilePath(reimportFileNamePath);
////
////            // check if tour is saved
////            final TourPerson tourPerson = oldTourData.getTourPerson();
////            if (tourPerson != null) {
////
////               // re-save tour when the re-imported tour was already saved
////
////               updatedTourData.setTourPerson(tourPerson);
////
////               /*
////                * Save tour but don't fire a change event because the tour editor would set the tour
////                * to dirty
////                */
////               final TourData savedTourData = TourManager.saveModifiedTour(updatedTourData, false);
////
////               updatedTourData = savedTourData;
////            }
//
////            TourLogManager.addSubLog(TourLogState.IMPORT_OK,
////                  NLS.bind("LOG_IMPORT_TOUR_IMPORTED",
////                        updatedTourData.getTourStartTime().format(TimeTools.Formatter_DateTime_S),
////                        reimportFileNamePath));
//
////            // Print the old vs new data comparison
////            for (final TourValueType tourValueType : tourValueTypes) {
////               displayReimportDataDifferences(tourValueType, clonedTourData, updatedTourData);
////            }
//
//            // check if tour is displayed in the import view
////            if (oldTourDataInImportView != null) {
////
////               // replace tour data in the import view
////
////               _toursInImportView.put(updatedTourData.getTourId(), updatedTourData);
////            }
////         }
//
//      } else {
//
//         TourLogManager.addSubLog(TourLogState.IMPORT_ERROR, reimportFileNamePath);
//
////         if (oldTourDataInImportView != null) {
////
////            // re-attach removed tour
////
////            _toursInImportView.put(oldTourId, oldTourDataInImportView);
////         }
//      }
//
//      return isTourReImported;
//   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_ReimportTours_Dialog_Title);

      shell.addListener(SWT.Resize, event -> {

         // force shell default size

         final Point shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

         shell.setSize(shellDefaultSize);
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_DeleteTourValues_Dialog_Title);
      setMessage(Messages.Dialog_DeleteTourValues_Dialog_Message);

      restoreState();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      getButton(IDialogConstants.OK_ID).setText(Messages.Dialog_DeleteTourValues_Button_Delete);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _parent = parent;

      initUI();

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(container);
      {
         createUI_10_Tours(container);
         createUI_20_Values(container);
      }
   }

   /**
    * UI to select either all the tours in the database or only the selected tours
    *
    * @param parent
    */
   private void createUI_10_Tours(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_ReimportTours_Group_Tours);
      group.setToolTipText(Messages.Dialog_ReimportTours_Group_Tours_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().spacing(5, 7).numColumns(2).applyTo(group);
      {
         {
            /*
             * Re-import ALL tours in the database
             */
            _rdoDeleteTourValues_Tours_All = new Button(group, SWT.RADIO);
            _rdoDeleteTourValues_Tours_All.setText(Messages.Dialog_ReimportTours_Radio_AllTours);
            _rdoDeleteTourValues_Tours_All.addSelectionListener(_defaultListener);
            GridDataFactory.fillDefaults().span(2, 1).indent(0, 3).applyTo(_rdoDeleteTourValues_Tours_All);
         }
         {
            /*
             * Re-import the SELECTED tours
             */
            _rdoDeleteTourValues_Tours_Selected = new Button(group, SWT.RADIO);
            _rdoDeleteTourValues_Tours_Selected.setText(Messages.Dialog_ReimportTours_Radio_SelectedTours);
            _rdoDeleteTourValues_Tours_Selected.addSelectionListener(_defaultListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoDeleteTourValues_Tours_Selected);
         }
         {
            /*
             * Re-import between dates
             */
            _rdoDeleteTourValues_Tours_BetweenDates = new Button(group, SWT.RADIO);
            _rdoDeleteTourValues_Tours_BetweenDates.setText(Messages.Dialog_ReimportTours_Radio_BetweenDates);
            _rdoDeleteTourValues_Tours_BetweenDates.addSelectionListener(_defaultListener);

            final Composite container = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               {

                  _dtTourDate_From = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
                  _dtTourDate_From.addSelectionListener(_defaultListener);
               }
               {
                  _dtTourDate_Until = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
                  _dtTourDate_Until.addSelectionListener(_defaultListener);
               }
            }
         }
      }
   }

   /**
    * UI to select the values to delete for the chosen tours
    *
    * @param parent
    */
   private void createUI_20_Values(final Composite parent) {

      final int verticalDistance = _pc.convertVerticalDLUsToPixels(4);

      final GridDataFactory gridDataTour_MoreVSpace = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .span(2, 1)
            .indent(0, verticalDistance);

      final GridDataFactory gridDataItem = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER);

      final GridDataFactory gridDataItem_FirstColumn = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .indent(16, 0);

      /*
       * group: data
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Dialog_DeleteTourValues_Group_Data);
      group.setToolTipText(Messages.Dialog_DeleteTourValues_Group_Data_Tooltip);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         {
            /*
             * Checkbox: All time slices
             */
            _chkData_AllTimeSlices = new Button(group, SWT.CHECK);
            _chkData_AllTimeSlices.setText(Messages.Dialog_ReimportTours_Checkbox_TimeSlices);
            _chkData_AllTimeSlices.addSelectionListener(_defaultListener);
            gridDataTour_MoreVSpace.applyTo(_chkData_AllTimeSlices);
         }

         // row 1
         {
            /*
             * Checkbox: Cadence
             */
            _chkData_Cadence = new Button(group, SWT.CHECK);
            _chkData_Cadence.setText(Messages.Dialog_ReimportTours_Checkbox_CadenceValues);
            _chkData_Cadence.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Cadence);
         }
         {
            /*
             * Checkbox: Running Dynamics
             */
            _chkData_RunningDynamics = new Button(group, SWT.CHECK);
            _chkData_RunningDynamics.setText(Messages.Dialog_ReimportTours_Checkbox_RunningDynamicsValues);
            _chkData_RunningDynamics.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_RunningDynamics);
         }

         // row 2
         {
            /*
             * Checkbox: Elevation
             */
            _chkData_Elevation = new Button(group, SWT.CHECK);
            _chkData_Elevation.setText(Messages.Dialog_ReimportTours_Checkbox_ElevationValues);
            _chkData_Elevation.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Elevation);
         }
         {
            /*
             * Checkbox: Swimming
             */
            _chkData_Swimming = new Button(group, SWT.CHECK);
            _chkData_Swimming.setText(Messages.Dialog_ReimportTours_Checkbox_SwimmingValues);
            _chkData_Swimming.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Swimming);
         }

         // row 3
         {
            /*
             * Checkbox: Gear
             */
            _chkData_Gear = new Button(group, SWT.CHECK);
            _chkData_Gear.setText(Messages.Dialog_ReimportTours_Checkbox_GearValues);
            _chkData_Gear.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_Gear);
         }
         {
            /*
             * Checkbox: Temperature
             */
            _chkData_Temperature = new Button(group, SWT.CHECK);
            _chkData_Temperature.setText(Messages.Dialog_ReimportTours_Checkbox_TemperatureValues);
            _chkData_Temperature.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Temperature);
         }

         // row 4
         {
            /*
             * Checkbox: Power And Pulse
             */
            _chkData_PowerAndPulse = new Button(group, SWT.CHECK);
            _chkData_PowerAndPulse.setText(Messages.Dialog_ReimportTours_Checkbox_PowerAndPulseValues);
            _chkData_PowerAndPulse.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_PowerAndPulse);
         }
         {
            /*
             * Checkbox: Timer pauses
             */
            _chkData_TourTimerPauses = new Button(group, SWT.CHECK);
            _chkData_TourTimerPauses.setText(Messages.Dialog_ReimportTours_Checkbox_TourTimerPauses);
            _chkData_TourTimerPauses.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_TourTimerPauses);
         }

         // row 5
         {
            /*
             * Checkbox: Power And Speed
             */
            _chkData_PowerAndSpeed = new Button(group, SWT.CHECK);
            _chkData_PowerAndSpeed.setText(Messages.Dialog_ReimportTours_Checkbox_PowerAndSpeedValues);
            _chkData_PowerAndSpeed.addSelectionListener(_defaultListener);
            gridDataItem_FirstColumn.applyTo(_chkData_PowerAndSpeed);
         }
         {
            /*
             * Checkbox: Training
             */
            _chkData_Training = new Button(group, SWT.CHECK);
            _chkData_Training.setText(Messages.Dialog_ReimportTours_Checkbox_TrainingValues);
            _chkData_Training.addSelectionListener(_defaultListener);
            gridDataItem.applyTo(_chkData_Training);
         }

         // row 6
         {
            /*
             * Checkbox: Tour markers
             */
            _chkData_TourMarkers = new Button(group, SWT.CHECK);
            _chkData_TourMarkers.setText(Messages.Dialog_ReimportTours_Checkbox_TourMarkers);
            _chkData_TourMarkers.addSelectionListener(_defaultListener);
            gridDataTour_MoreVSpace.applyTo(_chkData_TourMarkers);
         }
         {
            /*
             * Button: Deselect all
             */
            _btnDeselectAll = new Button(group, SWT.PUSH);
            _btnDeselectAll.setText(Messages.App_Action_DeselectAll);
            _btnDeselectAll.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onDeselectAll_DataItems();
               }
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .grab(true, false)
                  .indent(0, verticalDistance).applyTo(_btnDeselectAll);
         }
      }

      // set tab ordering, cool feature but all controls MUST have the same parent !!!
      group.setTabList(new Control[] {

            _chkData_AllTimeSlices,

            // column 1
            _chkData_Cadence,
            _chkData_Elevation,
            _chkData_Gear,
            _chkData_PowerAndPulse,
            _chkData_PowerAndSpeed,

            // column 2
            _chkData_RunningDynamics,
            _chkData_Swimming,
            _chkData_Temperature,
            _chkData_TourTimerPauses,
            _chkData_Training,

            _chkData_TourMarkers,

            _btnDeselectAll
      });
   }

   private void deleteTourValues(final List<TourValueType> tourValueTypes) {
      final long start = System.currentTimeMillis();
      // get selected tour IDs

      Object[] selectedItems = null;
      if (_tourViewer instanceof TourBookView) {
         selectedItems = (((TourBookView) _tourViewer).getSelectedTourIDs()).toArray();
      } else if (_tourViewer instanceof CollatedToursView) {
         selectedItems = (((CollatedToursView) _tourViewer).getSelectedTourIDs()).toArray();
      } else if (_tourViewer instanceof RawDataView) {
         selectedItems = (((RawDataView) _tourViewer).getSelectedTourIDs()).toArray();
      }

      if (selectedItems == null || selectedItems.length == 0) {

         MessageDialog.openInformation(Display.getDefault().getActiveShell(),
               Messages.Dialog_ReimportTours_Dialog_Title,
               Messages.Dialog_ReimportTours_Dialog_ToursAreNotSelected);

         return;
      }

      /*
       * convert selection to array
       */
      final Long[] selectedTourIds = new Long[selectedItems.length];
      for (int i = 0; i < selectedItems.length; i++) {
         selectedTourIds[i] = (Long) selectedItems[i];
      }

      final IRunnableWithProgress importRunnable = new IRunnableWithProgress() {

         Display display = Display.getDefault();

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            final ReImportStatus reImportStatus = new ReImportStatus();
            final boolean isUserAsked_ToCancelReImport[] = { false };

            final File[] reimportedFile = new File[1];
            int imported = 0;
            final int importSize = selectedTourIds.length;

            monitor.beginTask(Messages.Import_Data_Dialog_Reimport_Task, importSize);

            // loop: all selected tours in the viewer
            for (final Long tourId : selectedTourIds) {

               if (monitor.isCanceled()) {
                  // stop re-importing but process re-imported tours
                  break;
               }

               monitor.worked(1);
               monitor.subTask(NLS.bind(
                     Messages.Import_Data_Dialog_Reimport_SubTask,
                     new Object[] { ++imported, importSize }));

               final TourData oldTourData = TourManager.getTour(tourId);

               if (oldTourData == null) {
                  continue;
               }

               //   reimportTour(tourValueTypes, oldTourData, reimportedFile, skipToursWithFileNotFound, reImportStatus);

//               if (reImportStatus.isCanceled_ByUser_TheFileLocationDialog && isUserAsked_ToCancelReImport[0] == false
//                     && skipToursWithFileNotFound == false) {
//
//                  // user has canceled the re-import -> ask if the whole re-import should be canceled
//
//                  final boolean isCancelReimport[] = { false };
//
//                  display.syncExec(() -> {
//
//                     if (MessageDialog.openQuestion(display.getActiveShell(),
//                           Messages.Import_Data_Dialog_IsCancelReImport_Title,
//                           Messages.Import_Data_Dialog_IsCancelReImport_Message)) {
//
//                        isCancelReimport[0] = true;
//
//                     } else {
//
//                        isUserAsked_ToCancelReImport[0] = true;
//                     }
//                  });
//
//                  if (isCancelReimport[0]) {
//                     break;
//                  }
//               }
            }

//            if (reImportStatus.isReImported) {
//
//               updateTourData_InImportView_FromDb(monitor);
//
//               // reselect tours, run in UI thread
//               display.asyncExec(() -> {
//
//                  _tourViewer.reloadViewer();
//               });
//            }
         }
      };

      try {
         new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, importRunnable);
      } catch (final Exception e) {
         TourLogManager.logEx(e);
      } finally {

         final double time = (System.currentTimeMillis() - start) / 1000.0;
         TourLogManager.addLog(//
               TourLogState.DEFAULT,
               String.format(RawDataManager.LOG_REIMPORT_END, time));

      }

      // loop: all selected tours in the viewer
      for (final Long tourId : selectedTourIds) {

         final TourData oldTourData = TourManager.getTour(tourId);

         if (oldTourData == null) {
            continue;
         }

         for (final TourValueType tourValueType : tourValueTypes) {

            switch (tourValueType) {

            case TOUR_MARKER:
//               clonedTourData.setTourMarkers(null);
               break;

            //

            case TIME_SLICES_CADENCE:
//            clonedTourData.setAvgCadence(oldTourData.getAvgCadence());
//            clonedTourData.setCadenceMultiplier(oldTourData.getCadenceMultiplier());
               break;

            case TIME_SLICES_ELEVATION:
               oldTourData.altitudeSerie = null;
               break;

            case TIME_SLICES_GEAR:
//            clonedTourData.setFrontShiftCount(oldTourData.getFrontShiftCount());
//            clonedTourData.setRearShiftCount(oldTourData.getRearShiftCount());
               break;

            case TIME_SLICES_POWER_AND_PULSE:
//            clonedTourData.setPower_Avg(oldTourData.getPower_Avg());
//            clonedTourData.setAvgPulse(oldTourData.getAvgPulse());
//            clonedTourData.setCalories(oldTourData.getCalories());
               break;

            case TIME_SLICES_POWER_AND_SPEED:
//            clonedTourData.setPower_Avg(oldTourData.getPower_Avg());
//            clonedTourData.setCalories(oldTourData.getCalories());
               break;

            case TIME_SLICES_TEMPERATURE:
//            clonedTourData.setAvgTemperature(oldTourData.getAvgTemperature());
               break;

            case TIME_SLICES_TIMER_PAUSES:
//            clonedTourData.setTourDeviceTime_Paused(oldTourData.getTourDeviceTime_Paused());
               break;

            default:
               break;
            }

            //if something was done
            oldTourData.cleanupDataSeries();

            //TODO FB it doesnt seem to persist the changes. TO FIX
         }
      }

   }

   /**
    * Start the values deletion process
    *
    * @param tourValueTypes
    *           A list of tour values to delete
    */
   private void doDeleteValues(final List<TourValueType> tourValueTypes) {

      /*
       * There maybe too much tour cleanup but it is very complex how all the caches/selection
       * provider work together
       */

      // prevent async error in the save tour method, cleanup environment
      _tourViewer.getPostSelectionProvider().clearSelection();

      Util.clearSelection();

      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, null);

      TourManager.getInstance().clearTourDataCache();

      final boolean isDeleteTourValues_AllTours = _rdoDeleteTourValues_Tours_All.getSelection();
      final boolean isDeleteTourValues_BetweenDates = _rdoDeleteTourValues_Tours_BetweenDates.getSelection();

      if (isDeleteTourValues_AllTours || isDeleteTourValues_BetweenDates) {

         //The user MUST always confirm when the tool is running for ALL tours
         if (isDeleteTourValues_AllTours) {

            final MessageDialog dialog = new MessageDialog(
                  Display.getDefault().getActiveShell(),
                  Messages.Dialog_DatabaseAction_Confirmation_Title,
                  null,
                  Messages.Dialog_DatabaseAction_Confirmation_Message,
                  MessageDialog.QUESTION,
                  new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
                  1);

            final int choice = dialog.open();

            if (choice == IDialogConstants.CANCEL_ID) {

               return;
            }
         }

         // re-import ALL tours or BETWEEN tours

         if (RawDataManager.getInstance().actionReimportTour_10_Confirm(tourValueTypes) == false) {
            return;
         }

         saveState();

         TourLogManager.showLogView();

         final File[] reimportedFile = new File[1];
         final IComputeTourValues computeTourValueConfig = new IComputeTourValues() {

            @Override
            public boolean computeTourValues(final TourData oldTourData) {

               final ReImportStatus reImportStatus = new ReImportStatus();

//               RawDataManager.getInstance().reimportTour(reImportPartIds,
//                     oldTourData,
//                     reimportedFile,
//                     reImportStatus);

               return true;
            }

            @Override
            public String getResultText() {

               return UI.EMPTY_STRING;
            }

            @Override
            public String getSubTaskText(final TourData savedTourData) {

               return UI.EMPTY_STRING;
            }
         };

         ArrayList<Long> allTourIDs = null;

         if (isDeleteTourValues_BetweenDates) {

            // get tours between the dates

            allTourIDs = TourDatabase.getAllTourIds_BetweenTwoDates(

                  LocalDate.of(
                        _dtTourDate_From.getYear(),
                        _dtTourDate_From.getMonth() + 1,
                        _dtTourDate_From.getDay()),

                  LocalDate.of(
                        _dtTourDate_Until.getYear(),
                        _dtTourDate_Until.getMonth() + 1,
                        _dtTourDate_Until.getDay())

            );

            if (allTourIDs.isEmpty()) {

               MessageDialog.openInformation(getShell(),
                     Messages.Dialog_ReimportTours_Dialog_Title,
                     Messages.Dialog_ReimportTours_Dialog_ToursAreNotAvailable);

               return;
            }
         }

         TourDatabase.computeAnyValues_ForAllTours(computeTourValueConfig, allTourIDs);

         fireTourModifyEvent();

      } else {

         // re-import SELECTED tours

         deleteTourValues(tourValueTypes);
//         RawDataManager.getInstance().actionReimportSelectedTours(reImportPartIds, _tourViewer, skipToursWithFileNotFound);
      }
   }

   private void enableControls() {

      final boolean isValid = isDataValid();

      final boolean isReimport_AllTimeSlices = _chkData_AllTimeSlices.getSelection();
      final boolean isToursBetweenDates = _rdoDeleteTourValues_Tours_BetweenDates.getSelection();

      final boolean isTourSelected = _rdoDeleteTourValues_Tours_All.getSelection() ||
            _rdoDeleteTourValues_Tours_Selected.getSelection() ||
            isToursBetweenDates;

      final boolean isDataSelected = _chkData_AllTimeSlices.getSelection() ||
            _chkData_Elevation.getSelection() ||
            _chkData_Cadence.getSelection() ||
            _chkData_Gear.getSelection() ||
            _chkData_PowerAndPulse.getSelection() ||
            _chkData_PowerAndSpeed.getSelection() ||
            _chkData_RunningDynamics.getSelection() ||
            _chkData_Swimming.getSelection() ||
            _chkData_Temperature.getSelection() ||
            _chkData_Training.getSelection() ||
            _chkData_TourMarkers.getSelection() ||
            _chkData_TourTimerPauses.getSelection();

      final boolean isTimeSlice = !isReimport_AllTimeSlices;

//      _chkData_AllTimeSlices.setEnabled(!isReimport_EntireTour);
//      _chkData_TourMarkers.setEnabled(!isReimport_EntireTour);

      _chkData_Elevation.setEnabled(isTimeSlice);
      _chkData_Cadence.setEnabled(isTimeSlice);
      _chkData_Gear.setEnabled(isTimeSlice);
      _chkData_PowerAndPulse.setEnabled(isTimeSlice);
      _chkData_PowerAndSpeed.setEnabled(isTimeSlice);
      _chkData_RunningDynamics.setEnabled(isTimeSlice);
      _chkData_Swimming.setEnabled(isTimeSlice);
      _chkData_Temperature.setEnabled(isTimeSlice);
      _chkData_TourTimerPauses.setEnabled(isTimeSlice);
      _chkData_Training.setEnabled(isTimeSlice);

      _dtTourDate_From.setEnabled(isToursBetweenDates);
      _dtTourDate_Until.setEnabled(isToursBetweenDates);

      // OK button
      getButton(IDialogConstants.OK_ID).setEnabled(isTourSelected && isDataSelected && isValid);
   }

   private void fireTourModifyEvent() {

      TourManager.getInstance().removeAllToursFromCache();
      TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

      // prevent re-importing in the import view
      RawDataManager.setIsReimportingActive(true);
      {
         // fire unique event for all changes
         TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
      }
      RawDataManager.setIsReimportingActive(false);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
//      return null;
   }

   private void initUI() {

      _pc = new PixelConverter(_parent);

      _defaultListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            enableControls();
         }
      };
   }

   private boolean isDataValid() {

      final LocalDate dtFrom = LocalDate.of(
            _dtTourDate_From.getYear(),
            _dtTourDate_From.getMonth() + 1,
            _dtTourDate_From.getDay());

      final LocalDate dtUntil = LocalDate.of(
            _dtTourDate_Until.getYear(),
            _dtTourDate_Until.getMonth() + 1,
            _dtTourDate_Until.getDay());

      if (dtUntil.toEpochDay() >= dtFrom.toEpochDay()) {

         setErrorMessage(null);
         return true;

      } else {

         setErrorMessage(Messages.Dialog_ReimportTours_Error_2ndDateMustBeLarger);
         return false;
      }
   }

   @Override
   protected void okPressed() {

      //We close the window so the user can see that import progress bar and log view
      _parent.getShell().setVisible(false);

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         final List<TourValueType> reImportPartIds = new ArrayList<>();

         if (_chkData_AllTimeSlices.getSelection()) {

            reImportPartIds.add(TourValueType.ALL_TIME_SLICES);

         } else {

            if (_chkData_Cadence.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_CADENCE);
            }
            if (_chkData_Elevation.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_ELEVATION);
            }
            if (_chkData_Gear.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_GEAR);
            }
            if (_chkData_PowerAndPulse.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_POWER_AND_PULSE);
            }
            if (_chkData_PowerAndSpeed.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_POWER_AND_SPEED);
            }
            if (_chkData_RunningDynamics.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_RUNNING_DYNAMICS);
            }
            if (_chkData_Swimming.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_SWIMMING);
            }
            if (_chkData_Temperature.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_TEMPERATURE);
            }
            if (_chkData_TourTimerPauses.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_TIMER_PAUSES);
            }
            if (_chkData_Training.getSelection()) {
               reImportPartIds.add(TourValueType.TIME_SLICES_TRAINING);
            }
         }

         if (_chkData_TourMarkers.getSelection()) {
            reImportPartIds.add(TourValueType.TOUR_MARKER);
         }

         doDeleteValues(reImportPartIds);
      });

      super.okPressed();
   }

   private void onDeselectAll_DataItems() {

      _chkData_AllTimeSlices.setSelection(false);
      _chkData_Elevation.setSelection(false);
      _chkData_Cadence.setSelection(false);
      _chkData_Gear.setSelection(false);
      _chkData_PowerAndPulse.setSelection(false);
      _chkData_PowerAndSpeed.setSelection(false);
      _chkData_RunningDynamics.setSelection(false);
      _chkData_Swimming.setSelection(false);
      _chkData_Temperature.setSelection(false);
      _chkData_TourMarkers.setSelection(false);
      _chkData_TourTimerPauses.setSelection(false);
      _chkData_Training.setSelection(false);

      enableControls();
   }

   private void restoreState() {

      // Tours to re-import
      _rdoDeleteTourValues_Tours_All.setSelection(_state.getBoolean(STATE_DELETE_TOURVALUES_ALL));
      _rdoDeleteTourValues_Tours_BetweenDates.setSelection(_state.getBoolean(STATE_DELETE_TOURVALUES_BETWEEN_DATES));
      _rdoDeleteTourValues_Tours_Selected.setSelection(_state.getBoolean(STATE_DELETE_TOURVALUES_SELECTED));

      Util.getStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM, LocalDate.now(), _dtTourDate_From);
      Util.getStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL, LocalDate.now(), _dtTourDate_Until);

      // Data to re-import
      _chkData_AllTimeSlices.setSelection(_state.getBoolean(STATE_IS_DELETE_ALL_TIME_SLICES));
      _chkData_Elevation.setSelection(_state.getBoolean(STATE_IS_DELETE_ELEVATION));
      _chkData_Cadence.setSelection(_state.getBoolean(STATE_IS_DELETE_CADENCE));
      _chkData_Gear.setSelection(_state.getBoolean(STATE_IS_DELETE_GEAR));
      _chkData_PowerAndPulse.setSelection(_state.getBoolean(STATE_IS_DELETE_POWER_AND_PULSE));
      _chkData_PowerAndSpeed.setSelection(_state.getBoolean(STATE_IS_DELETE_POWER_AND_SPEED));
      _chkData_RunningDynamics.setSelection(_state.getBoolean(STATE_IS_DELETE_RUNNING_DYNAMICS));
      _chkData_Swimming.setSelection(_state.getBoolean(STATE_IS_DELETE_SWIMMING));
      _chkData_Temperature.setSelection(_state.getBoolean(STATE_IS_DELETE_TEMPERATURE));
      _chkData_Training.setSelection(_state.getBoolean(STATE_IS_DELETE_TRAINING));
      _chkData_TourMarkers.setSelection(_state.getBoolean(STATE_IS_DELETE_TOUR_MARKERS));
      _chkData_TourTimerPauses.setSelection(_state.getBoolean(STATE_IS_DELETE_TIMER_PAUSES));

      enableControls();
   }

   private void saveState() {

      // Tours to re-import
      _state.put(STATE_DELETE_TOURVALUES_ALL, _rdoDeleteTourValues_Tours_All.getSelection());
      _state.put(STATE_DELETE_TOURVALUES_BETWEEN_DATES, _rdoDeleteTourValues_Tours_BetweenDates.getSelection());
      _state.put(STATE_DELETE_TOURVALUES_SELECTED, _rdoDeleteTourValues_Tours_Selected.getSelection());

      Util.setStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_FROM, _dtTourDate_From);
      Util.setStateDate(_state, STATE_DELETE_TOURVALUES_BETWEEN_DATES_UNTIL, _dtTourDate_Until);

      // Data to import
      _state.put(STATE_IS_DELETE_ELEVATION, _chkData_Elevation.getSelection());
      _state.put(STATE_IS_DELETE_CADENCE, _chkData_Cadence.getSelection());
      _state.put(STATE_IS_DELETE_GEAR, _chkData_Gear.getSelection());
      _state.put(STATE_IS_DELETE_POWER_AND_PULSE, _chkData_PowerAndPulse.getSelection());
      _state.put(STATE_IS_DELETE_POWER_AND_SPEED, _chkData_PowerAndSpeed.getSelection());
      _state.put(STATE_IS_DELETE_RUNNING_DYNAMICS, _chkData_RunningDynamics.getSelection());
      _state.put(STATE_IS_DELETE_SWIMMING, _chkData_Swimming.getSelection());
      _state.put(STATE_IS_DELETE_TEMPERATURE, _chkData_Temperature.getSelection());
      _state.put(STATE_IS_DELETE_TRAINING, _chkData_Training.getSelection());
      _state.put(STATE_IS_DELETE_ALL_TIME_SLICES, _chkData_AllTimeSlices.getSelection());
      _state.put(STATE_IS_DELETE_TOUR_MARKERS, _chkData_TourMarkers.getSelection());
      _state.put(STATE_IS_DELETE_TIMER_PAUSES, _chkData_TourTimerPauses.getSelection());
   }
}
