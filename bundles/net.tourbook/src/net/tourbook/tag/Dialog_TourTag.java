/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourTag;
import net.tourbook.photo.ImageUtils;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to modify a {@link TourTag}
 */
public class Dialog_TourTag extends TitleAreaDialog {

   private static final String           ID                = "net.tourbook.tag.Dialog_TourTag"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore        = TourbookPlugin.getPrefStore();
   private static final String           IMPORT_IMAGE_PATH = "Dialog_TourTag_ImportImagePath";  //$NON-NLS-1$

   private final IDialogSettings         _state            = TourbookPlugin.getState(ID);

   private String                        _dlgMessage;
   private TourTag                       _tourTag_Original;

   private TourTag                       _tourTag_Clone;

   /*
    * UI controls
    */
   private Button _btnImportImage;
   private Text   _txtNotes;
   private Text   _txtName;

   public Dialog_TourTag(final Shell parentShell, final String dlgMessage, final TourTag tourTag) {

      super(parentShell);

      _dlgMessage = dlgMessage;

      _tourTag_Original = tourTag;
      _tourTag_Clone = tourTag.clone();

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_TourTag_Title);
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_TourTag_EditTag_Title);
      setMessage(_dlgMessage);
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Save
      getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      restoreState();

      _txtName.selectAll();
      _txtName.setFocus();

      return dlgContainer;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         {
            // Text: Name

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_TourTag_Label_TagName);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _txtName = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtName);
         }
         {
            // Text: Image
            final Label label = UI.createLabel(container, UI.EMPTY_STRING);
            label.setText(Messages.Dialog_TourTag_Label_Image);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _btnImportImage = new Button(container, SWT.NONE);
            _btnImportImage.setSize(1000, 1000);
            _btnImportImage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onImportImage()));
            GridDataFactory.fillDefaults()
                  .align(SWT.LEFT, SWT.FILL)
                  .applyTo(_btnImportImage);
         }
         {
            // Text: Notes

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_TourTag_Label_Notes);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtNotes = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(20))
                  .applyTo(_txtNotes);
         }
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   @Override
   protected void okPressed() {

      // set model from UI
      saveState();

      if (_tourTag_Clone.isValidForSave() == false) {
         return;
      }

      // update original model
      _tourTag_Original.updateFromModified(_tourTag_Clone);

      super.okPressed();
   }

   private void onImportImage() {

      final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);

      fileDialog.setText(Messages.Dialog_TourTag_ImportImage_Title);
      fileDialog.setFilterPath(_prefStore.getString(IMPORT_IMAGE_PATH));

      fileDialog.setFilterExtensions(new String[] { "*.png", "*.jpg" });//$NON-NLS-1$ //$NON-NLS-2$

      // open file dialog
      final String imageFilePath = fileDialog.open();

      // check if user canceled the dialog
      if (imageFilePath == null) {
         return;
      }

      //todo fb dispose image
      final Image image = ImageUtils.convertFileToImage(imageFilePath);
      _btnImportImage.setImage(image);
   }

   private void restoreState() {

      _txtName.setText(_tourTag_Clone.getTagName());
      _txtNotes.setText(_tourTag_Clone.getNotes());
      _btnImportImage.setImage(ImageUtils.convertByteArrayToImage(_tourTag_Clone.getImage()));
   }

   private void saveState() {

      _tourTag_Clone.setNotes(_txtNotes.getText());
      _tourTag_Clone.setTagName(_txtName.getText());
      _tourTag_Clone.setImage(ImageUtils.formatImage(_btnImportImage.getImage(), 0));
   }
}
