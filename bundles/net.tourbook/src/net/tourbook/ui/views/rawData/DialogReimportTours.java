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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DialogReimportTours extends TitleAreaDialog {

   private static final String STATE_GPX_IS_ABSOLUTE_DISTANCE    = "STATE_GPX_IS_ABSOLUTE_DISTANCE";    //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_DESCRITION    = "STATE_GPX_IS_EXPORT_DESCRITION";    //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_MARKERS       = "STATE_GPX_IS_EXPORT_MARKERS";       //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_TOUR_DATA     = "STATE_GPX_IS_EXPORT_TOUR_DATA";     //$NON-NLS-1$
   private static final String STATE_GPX_IS_EXPORT_SURFING_WAVES = "STATE_GPX_IS_EXPORT_SURFING_WAVES"; //$NON-NLS-1$
   private static final String STATE_GPX_IS_WITH_BAROMETER       = "STATE_GPX_IS_WITH_BAROMETER";       //$NON-NLS-1$

   private static final String STATE_TCX_ACTIVITY_TYPES          = "STATE_TCX_ACTIVITY_TYPES";          //$NON-NLS-1$
   private static final String STATE_TCX_ACTIVITY_TYPE           = "STATE_TCX_ACTIVITY_TYPE";           //$NON-NLS-1$
   private static final String STATE_TCX_IS_COURSES              = "STATE_TCX_IS_COURSES";              //$NON-NLS-1$
   private static final String STATE_TCX_IS_EXPORT_DESCRITION    = "STATE_TCX_IS_EXPORT_DESCRITION";    //$NON-NLS-1$
   private static final String STATE_TCX_IS_NAME_FROM_TOUR       = "STATE_TCX_IS_NAME_FROM_TOUR";       //$NON-NLS-1$
   private static final String STATE_TCX_COURSE_NAME             = "STATE_TCX_COURSE_NAME";             //$NON-NLS-1$

   private static final String STATE_IS_EXPORT_TOUR_RANGE        = "isExportTourRange";                 //$NON-NLS-1$
   private static final String STATE_IS_OVERWRITE_FILES          = "isOverwriteFiles";                  //$NON-NLS-1$
   private static final String STATE_EXPORT_PATH_NAME            = "exportPathName";                    //$NON-NLS-1$
   private static final String STATE_EXPORT_FILE_NAME            = "exportFileName";                    //$NON-NLS-1$

   private static final String            ZERO                       = "0";                                                //$NON-NLS-1$

   private static final int               VERTICAL_SECTION_MARGIN    = 10;
   private static final int               SIZING_TEXT_FIELD_WIDTH    = 250;
   private static final int               COMBO_HISTORY_LENGTH       = 20;

   private static String                  _dlgDefaultMessage;
   //
   private static final DecimalFormat     _nf1                       = (DecimalFormat) NumberFormat.getInstance(Locale.US);
   private static final DecimalFormat     _nf3                       = (DecimalFormat) NumberFormat.getInstance(Locale.US);
   private static final DecimalFormat     _nf8                       = (DecimalFormat) NumberFormat.getInstance(Locale.US);

   private static final DateTimeFormatter _dtIso                     = ISODateTimeFormat.dateTimeNoMillis();
   private static final SimpleDateFormat  _dateFormat                = new SimpleDateFormat();

   static {

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf1.setGroupingUsed(false);

      _nf3.setMinimumFractionDigits(1);
      _nf3.setMaximumFractionDigits(3);
      _nf3.setGroupingUsed(false);

      _nf8.setMinimumFractionDigits(1);
      _nf8.setMaximumFractionDigits(8);
      _nf8.setGroupingUsed(false);

      _dtIso.withZoneUTC();
      _dateFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
      _dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
   }

   private final IDialogSettings     _state              = TourbookPlugin
         .getState("DialogExportTour");                                                                                                                   //$NON-NLS-1$

   /**
    * Is <code>true</code> when multiple tours are selected and NOT merged into 1 file.
    */
   private boolean                   _isExport_MultipleToursWithMultipleFiles;

   private boolean                   _isInUIInit;

   /**
    * Is <code>true</code> when only a part is exported.
    */
   private boolean                   _isSetup_TourRange;

   /**
    * Is <code>true</code> when multiple tours are exported.
    */
   private boolean                   _isSetup_MultipleTours;



   private Point                     _shellDefaultSize;


   private PixelConverter            _pc;

   /*
    * UI controls
    */
   private Button    _btnSelectDirectory;
   private Button    _btnSelectFile;

   private Button    _chkExportTourRange;
   private Button    _chkOverwriteFiles;

   private Button    _chkGPX_WithBarometer;

   private Combo     _comboFile;

   private Composite _dlgContainer;
   private Composite _inputContainer;

   private Label     _lblTcxActivityType;

   private Text      _txtFilePath;

   /**
    * @param parentShell
    */
   public DialogReimportTours(final Shell parentShell) {

      //TODO FB pass the selected tours in case the user wants to reimport only for the selected tours
      super(parentShell);

      int shellStyle = getShellStyle();

      shellStyle = //
            SWT.NONE //
                  | SWT.TITLE
                  | SWT.CLOSE
                  | SWT.MIN
//          | SWT.MAX
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

      shell.setText(Messages.dialog_export_shell_text);

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

      setTitle(Messages.dialog_export_dialog_title);
      setMessage(_dlgDefaultMessage);

      _isInUIInit = true;
      {
         restoreState();
      }
      _isInUIInit = false;

      enableFields();
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
      }
   }

   private void createUI_10_Tours(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         createUI_14_OptionsRight(container);
      }
   }

   private void createUI_14_OptionsRight(final Composite parent) {



         final Group groupCustomGPX = new Group(parent, SWT.NONE);
         groupCustomGPX.setText(Messages.Dialog_Export_Group_Custom);
         groupCustomGPX.setToolTipText(Messages.Dialog_Export_Group_Custom_Tooltip);
         GridDataFactory.fillDefaults().grab(false, false).applyTo(groupCustomGPX);
         GridLayoutFactory.swtDefaults().applyTo(groupCustomGPX);
         {
            createUI_72_Option_GPX_Custom(groupCustomGPX);
      }
   }

   private void createUI_20_Data(final Composite parent) {

      Label label;

      final ModifyListener filePathModifyListener = new ModifyListener() {
         @Override
         public void modifyText(final ModifyEvent e) {
            validateFields();
         }
      };

      /*
       * group: filename
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.dialog_export_group_exportFileName);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {
         /*
          * label: filename
          */
         label = new Label(group, SWT.NONE);
         label.setText(Messages.dialog_export_label_fileName);

         /*
          * combo: path
          */
         _comboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboFile);
         ((GridData) _comboFile.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
         _comboFile.setVisibleItemCount(20);
         _comboFile.addVerifyListener(net.tourbook.common.UI.verifyFilenameInput());
         _comboFile.addModifyListener(filePathModifyListener);
         _comboFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               validateFields();
            }
         });

         /*
          * button: browse
          */
         _btnSelectFile = new Button(group, SWT.PUSH);
         _btnSelectFile.setText(Messages.app_btn_browse);
         _btnSelectFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectBrowseFile();
               validateFields();
            }
         });
         setButtonLayoutData(_btnSelectFile);

         // -----------------------------------------------------------------------------

         /*
          * label: path
          */
         label = new Label(group, SWT.NONE);
         label.setText(Messages.dialog_export_label_exportFilePath);

         /*
          * button: browse
          */
         _btnSelectDirectory = new Button(group, SWT.PUSH);
         _btnSelectDirectory.setText(Messages.app_btn_browse);
         _btnSelectDirectory.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               validateFields();
            }
         });
         setButtonLayoutData(_btnSelectDirectory);

         // -----------------------------------------------------------------------------

         /*
          * label: file path
          */
         label = new Label(group, SWT.NONE);
         label.setText(Messages.dialog_export_label_filePath);

         /*
          * text: filename
          */
         _txtFilePath = new Text(group, /* SWT.BORDER | */SWT.READ_ONLY);
         GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFilePath);
         _txtFilePath.setToolTipText(Messages.dialog_export_txt_filePath_tooltip);
         _txtFilePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

         // -----------------------------------------------------------------------------

         /*
          * checkbox: overwrite files
          */
         _chkOverwriteFiles = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .span(3, 1)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkOverwriteFiles);
         _chkOverwriteFiles.setText(Messages.dialog_export_chk_overwriteFiles);
         _chkOverwriteFiles.setToolTipText(Messages.dialog_export_chk_overwriteFiles_tooltip);
      }

   }






   private void createUI_72_Option_GPX_Custom(final Composite parent) {

      /*
       * checkbox: export with barometer
       */
      _chkGPX_WithBarometer = new Button(parent, SWT.CHECK);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(_chkGPX_WithBarometer);
      _chkGPX_WithBarometer.setText(Messages.Dialog_Export_Checkbox_WithBarometer);
      _chkGPX_WithBarometer.setToolTipText(Messages.Dialog_Export_Checkbox_WithBarometer_Tooltip);
   }


   private void doReimport() throws IOException {


   }

   private void enableExportButton(final boolean isEnabled) {
      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   private void enableFields() {

      final boolean isSingleTour = _isSetup_MultipleTours == false;
      final boolean isMergeIntoOneTour = false;


      _isExport_MultipleToursWithMultipleFiles = _isSetup_MultipleTours;


         final boolean isNoneGPX = isSingleTour || _isExport_MultipleToursWithMultipleFiles;


      _comboFile.setEnabled(isSingleTour || isMergeIntoOneTour);
      _btnSelectFile.setEnabled(isSingleTour || isMergeIntoOneTour);


   }





   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      // keep window size and position
      return _state;
   }

   private String getExportFileName() {
      return _comboFile.getText().trim();
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
               doReimport();
            } catch (final IOException e) {
               StatusUtil.log(e);
            }
         }
      });

      super.okPressed();
   }



   private void onSelectBrowseFile() {


   }

   private void restoreState() {

         final String[] activityTypes = _state.getArray(STATE_TCX_ACTIVITY_TYPES);
         if (activityTypes == null) {
            /*
             * Fill-up the default activity types
             */

         }

         final String lastSelected_ActivityType = _state.get(STATE_TCX_ACTIVITY_TYPE);


      // merge all tours

      // export tour part
      if (_isSetup_TourRange) {
         _chkExportTourRange.setSelection(_state.getBoolean(STATE_IS_EXPORT_TOUR_RANGE));
      }

      // camouflage speed

      // export file/path
      UI.restoreCombo(_comboFile, _state.getArray(STATE_EXPORT_FILE_NAME));
      _chkOverwriteFiles.setSelection(_state.getBoolean(STATE_IS_OVERWRITE_FILES));
   }

   private void saveState() {

         _state.put(STATE_GPX_IS_WITH_BAROMETER, _chkGPX_WithBarometer.getSelection());

      // merge all tours

      // export tour part
      if (_isSetup_TourRange) {
         _state.put(STATE_IS_EXPORT_TOUR_RANGE, _chkExportTourRange.getSelection());
      }


      _state.put(STATE_IS_OVERWRITE_FILES, _chkOverwriteFiles.getSelection());
   }





   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      /*
       * validate fields
       */


      setErrorMessage(null);
      enableExportButton(true);
   }
}