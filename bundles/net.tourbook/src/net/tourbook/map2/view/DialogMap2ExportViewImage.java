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
package net.tourbook.map2.view;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.Map2ColorProfile;
import net.tourbook.map2.Messages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogMap2ExportViewImage extends TitleAreaDialog {

   private final IDialogSettings _state = TourbookPlugin.getState("DialogMap2ExportViewImage"); //$NON-NLS-1$


   private ColorDefinition              _colorDefinition;

   private boolean                      _isInitializeControls;

   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private Composite     _dlgContainer;
   private Composite     _inputContainer;

   private Button        _btnApply;

   private Combo         _comboFile;

   private Button    _btnSelectFile;

   private Combo     _comboPath;

   private Button    _btnSelectDirectory;

   private Text      _txtFilePath;

   private Button    _chkOverwriteFiles;

   private Combo     _comboImageFormatType;

   public DialogMap2ExportViewImage(final Shell parentShell) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   public void create() {

      super.create();

      getShell().setText(Messages.legendcolor_dialog_title_name);

      /*
       * initialize dialog by selecting select first value point
       */

      _isInitializeControls = true;
      {
         updateUI();
      }
      _isInitializeControls = false;

      setTitle(Messages.legendcolor_dialog_title);
      setMessage(Messages.legendcolor_dialog_title_message);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

//      initUI(parent);

      _dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(_dlgContainer);

      return _dlgContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _inputContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);
      {
         createUI_10_ImageFormat(_inputContainer);
         createUI_90_ExportImage(_inputContainer);
      }
   }

   private void createUI_10_ImageFormat(final Composite parent) {

      /*
       * group: filename
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText("Messages.dialog_export_group_exportFileName");
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {
         /*
          * label: filename
          */
         final Label label = new Label(group, SWT.NONE);
         label.setText("Messages.dialog_export_label_fileName");

         /*
          * combo: path
          */
         _comboImageFormatType = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboImageFormatType);
//         ((GridData) _comboPath.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
         _comboImageFormatType.setVisibleItemCount(20);
//         _comboPath.addModifyListener(filePathModifyListener);
         _comboImageFormatType.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               validateFields();
            }
         });

      }

   }

   private void createUI_90_ExportImage(final Composite parent) {

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
      group.setText("Messages.dialog_export_group_exportFileName");
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {
         /*
          * label: filename
          */
         label = new Label(group, SWT.NONE);
         label.setText("Messages.dialog_export_label_fileName");

         /*
          * combo: path
          */
         _comboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboFile);
//         ((GridData) _comboFile.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
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
         _btnSelectFile.setText("Messages.app_btn_browse");
         _btnSelectFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
//               onSelectBrowseFile();
               validateFields();
            }
         });
         setButtonLayoutData(_btnSelectFile);

         // -----------------------------------------------------------------------------

         /*
          * label: path
          */
         label = new Label(group, SWT.NONE);
         label.setText("Messages.dialog_export_label_exportFilePath");

         /*
          * combo: path
          */
         _comboPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboPath);
//         ((GridData) _comboPath.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
         _comboPath.setVisibleItemCount(20);
         _comboPath.addModifyListener(filePathModifyListener);
         _comboPath.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               validateFields();
            }
         });

         /*
          * button: browse
          */
         _btnSelectDirectory = new Button(group, SWT.PUSH);
         _btnSelectDirectory.setText("Messages.app_btn_browse");
         _btnSelectDirectory.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
//               onSelectBrowseDirectory();
               validateFields();
            }
         });
         setButtonLayoutData(_btnSelectDirectory);

         // -----------------------------------------------------------------------------

         /*
          * label: file path
          */
         label = new Label(group, SWT.NONE);
         label.setText("Messages.dialog_export_label_filePath");

         /*
          * text: filename
          */
         _txtFilePath = new Text(group, /* SWT.BORDER | */SWT.READ_ONLY);
         GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFilePath);
         _txtFilePath.setToolTipText("Messages.dialog_export_txt_filePath_tooltip");
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
         _chkOverwriteFiles.setText("Messages.dialog_export_chk_overwriteFiles");
         _chkOverwriteFiles.setToolTipText("Messages.dialog_export_chk_overwriteFiles_tooltip");
      }

   }

   private void enableControls() {

      // min brightness


      // live update/apply
      final boolean isLiveUpdate = true;//_chkLiveUpdate.getSelection();
      _btnApply.setEnabled(isLiveUpdate == false);
   }

   private void enableOK(final boolean isEnabled) {
      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }


   private void restoreState() {
//      _chkLiveUpdate.setSelection(_state.getBoolean(STATE_LIVE_UPDATE));
   }

   private void saveState() {
//      _state.put(STATE_LIVE_UPDATE, _chkLiveUpdate.getSelection());
   }

   /**
    * Initialized the dialog by setting the {@link Map2ColorProfile} which will be displayed in
    * this dialog, it will use a copy of the supplied {@link Map2ColorProfile}
    *
    * @param colorDefinition
    */
   public void setLegendColor(final ColorDefinition colorDefinition) {

      _colorDefinition = colorDefinition;

      // use a copy of the legendColor to support the cancel feature

//		System.out.println(UI.timeStampNano()
//				+ " ["
//				+ getClass().getSimpleName()
//				+ "] \t"
//				+ Arrays.toString(colorValues));
//		// TODO remove SYSTEM.OUT.PRINTLN

   }

   /**
    * Update legend data from the UI
    */
   private void updateModelFromUI() {

      // update color selector



      enableControls();

   }

   /**
    * Update UI from legend data
    */
   private void updateUI() {


   }




   private void validateFields() {

      if (_isInitializeControls) {
         return;
      }

      setErrorMessage(null);

      if (true) {

         setErrorMessage(Messages.legendcolor_dialog_error_max_greater_min);
         enableOK(false);
         return;
      }

      setMessage(NLS.bind(Messages.legendcolor_dialog_title_message, _colorDefinition.getVisibleName()));

      updateModelFromUI();

      enableOK(true);
   }
}
