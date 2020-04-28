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

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class DropboxBrowser extends TitleAreaDialog {
   //TODO FB remove unused imports
   //TODO FB externalize strings
   //TODO FB DO i nees this string ? Dialog_DropboxFolderChooser_AccessToken_Missing
   //TODO what if I revoke the token ? what happens when opening the folder ? renewing the otken ? etc..
//TODO what if the user selects to delete the file from the device ? maybe we should disable that

   //Several bugs :
   //Debian choosing the USerFiles directory shows 0 files to be imported !?
   //changing the user folder doesn't trigger an update on the easyimport
   //When clicking on OK in the easyimport ocnfig, sortly a red message is displayed ot say that the dropbox folder is not available
   // adding files in the dropbox remote folder does not update the number of files to be imported

   //TODO FB use _dropboxFileSystem.GetPathSeparator() instead of "/"
   private static final String ROOT_FOLDER    = "/";                           //$NON-NLS-1$

   final IPreferenceStore      _prefStore     = CommonActivator.getPrefStore();

   private List<Metadata>      _folderList;

   private TableViewer         _contentViewer;
   private String              _selectedFolder;
   private ArrayList<String>   _selectedFiles = new ArrayList<>();

   private ChooserType         _chooserType;

   /*
    * UI controls
    */
   private Text   _textSelectedAbsolutePath;
   private Button _buttonParentFolder;

   public DropboxBrowser(final Shell parentShell, final ChooserType chooserType, final String accessToken) {

      super(parentShell);

      setShellStyle(getShellStyle() | SWT.RESIZE);

      _chooserType = chooserType;

      setDefaultImage(Activator.getImageDescriptor(Messages.Image__Dropbox_Logo).createImage());
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(net.tourbook.cloud.dropbox.Messages.Dialog_DropboxFolderChooser_Area_Title);
   }

   @Override
   public void create() {

      super.create();

      setTitle(net.tourbook.cloud.dropbox.Messages.Dialog_DropboxFolderChooser_Area_Title);

   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgAreaContainer);

      updateViewers();

      return dlgAreaContainer;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().margins(20, 20).numColumns(2).applyTo(container);
      {
         /*
          * Parent folder button
          */
         _buttonParentFolder = new Button(container, SWT.LEFT);
         _buttonParentFolder.setToolTipText(Messages.Dialog_DropboxBrowser_Button_ParentFolder_Tooltip);
         _buttonParentFolder.setImage(Activator.getImageDescriptor(Messages.Image__Dropbox_Parentfolder).createImage());
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_buttonParentFolder);
         _buttonParentFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
               onClickParentFolder();
            }
         });
         _buttonParentFolder.setEnabled(false);

         /*
          * Dropbox folder path
          */
         _textSelectedAbsolutePath = new Text(container, SWT.BORDER);
         _textSelectedAbsolutePath.setEditable(false);
         _textSelectedAbsolutePath.setToolTipText(Messages.Dialog_DropboxBrowser_Text_AbsolutePath_Tooltip);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_textSelectedAbsolutePath);
         _textSelectedAbsolutePath.setText(ROOT_FOLDER);

         createUI_10_FilterViewer(container);
      }

      return container;
   }

   private void createUI_10_FilterViewer(final Composite parent) {

      final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().span(3, 1).grab(true, true).hint(600, 300).applyTo(layouter);

      final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));
      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      _contentViewer = new TableViewer(table);

      // column: name + image
      final TableViewerColumn tvc = new TableViewerColumn(_contentViewer, SWT.NONE);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Metadata entry = ((Metadata) cell.getElement());

            String filterName = null;
            Image filterImage = null;

            filterName = entry.getName();

            if (entry instanceof FolderMetadata) {

               filterImage =
                     Activator.getImageDescriptor(Messages.Image__Dropbox_Folder).createImage();
            } else if (entry instanceof FileMetadata) {

               filterImage =
                     Activator.getImageDescriptor(Messages.Image__Dropbox_File).createImage();
            }

            cell.setText(filterName);
            cell.setImage(filterImage);
         }
      });
      layouter.addColumnData(new ColumnWeightData(1));

      _contentViewer.setContentProvider(new IStructuredContentProvider() {
         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {
            return _folderList.toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _contentViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {
            onSelectItem(event.getSelection());
         }
      });
   }

   public ArrayList<String> getSelectedFiles() {
      return _selectedFiles;
   }

   public String getSelectedFolder() {
      return _selectedFolder;
   }

   @Override
   protected void okPressed() {

      boolean readyToQuit = true;

      if (_chooserType == ChooserType.Folder) {
         _selectedFolder = _textSelectedAbsolutePath.getText();
      } else if (_chooserType == ChooserType.File) {
         final StructuredSelection selection = (StructuredSelection) _contentViewer.getSelection();

         final Object[] selectionArray = selection.toArray();
         if (selectionArray.length == 0) {
            return;
         } else {
            boolean selectionContainsFile = false;

            for (final Object item : selectionArray) {
               if ((Metadata) item instanceof FileMetadata) {
                  selectionContainsFile = true;
                  break;
               }
            }

            //If the list of selected items don't contain any files,
            //we dive into the last folder
            if (!selectionContainsFile) {

               final Metadata lastSelecteditem = ((Metadata) selection.toArray()[selection.toArray().length - 1]);
               selectFolder(lastSelecteditem.getPathDisplay());
               readyToQuit = false;
            } else {

               //Otherwise, we import all the selected items that are files.

               for (final Object item : selectionArray) {
                  final Metadata metadata = ((Metadata) item);

                  if (metadata instanceof FileMetadata) {
                     _selectedFiles.add(metadata.getPathDisplay());
                  }
               }
            }
         }
      }

      if (readyToQuit) {
         super.okPressed();
      }
   }

   protected void onClickParentFolder() {

      final String currentFolder = _textSelectedAbsolutePath.getText();

      final int endIndex = currentFolder.lastIndexOf(ROOT_FOLDER);
      if (endIndex != -1) {
         final String parentFolder = currentFolder.substring(0, endIndex);
         selectFolder(parentFolder);
      }
   }

   protected void onSelectItem(final ISelection selectedItem) {
      final StructuredSelection selection = (StructuredSelection) selectedItem;
      final Object[] selectionArray = selection.toArray();
      if (selectionArray.length == 0) {
         return;
      }

      // Double clicking on an item should always return only 1 element.
      final Metadata item = ((Metadata) selection.toArray()[0]);
      final String itemPath = item.getPathDisplay();

      if (item instanceof FolderMetadata) {

            selectFolder(itemPath);
      }

      if (_chooserType == ChooserType.File) {

         if (item instanceof FileMetadata) {

               _selectedFiles.add(itemPath);
               super.okPressed();
         }
      }

   }

   private void selectFolder(final String folderAbsolutePath) {

      try {
         final ListFolderResult list = DropboxClient.getDefault().files().listFolder(folderAbsolutePath);
         _folderList = list.getEntries();

         _textSelectedAbsolutePath.setText(
               StringUtils.isNullOrEmpty(folderAbsolutePath) ? ROOT_FOLDER : folderAbsolutePath);

         _buttonParentFolder.setEnabled(_textSelectedAbsolutePath.getText().length() > 1);

         _contentViewer.refresh();

      } catch (final DbxException e) {
         StatusUtil.log(e);
      }
   }

   private void updateViewers() {

      selectFolder(UI.EMPTY_STRING);

      // show contents in the viewer
      _contentViewer.setInput(new Object());
   }
}
