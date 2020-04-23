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
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class DropboxBrowser extends TitleAreaDialog {
   //TODO FB add a parameter to give the user the abaility to select a file (so that this class can be resued in the easy import
   //TODO FB remove unused imports
   //TODO FB double or single click to enter a folder ? compare with GC. Also, does GC disable the OK button when selecting a file ?
   //TODO FB enable multiple file selection for the imports but disable the ok button if a folder is part of the selection
   //TODO FB enable file import when double clicking on it
   //TODO FB enable file extension filtering for file import
   //TODO FB externalize strings
   //TODO FB when importing manual files from dropbox, if a folder was not selected, dont display an error message
   //TODO FB DO i nees this string ? Dialog_DropboxFolderChooser_AccessToken_Missing
   //TODO what if I revoke the token ? what happens when opening the folder ? renewing the otken ? etc..
//TODO what if the user selects to delete the file from the device ? maybe we should disable that

   //TODO Fix : The regex match pattern doesnt work for dropbox
   //see easyimportmanager.java  try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(validPath, globPattern)) {

   //TOOD will my dropbox easy import implementation be able to detect if a file is already imported ?
   // i dont remember how that check is performed

   //Several bugs : Choosing the root folder doesn't add "/" to the UI.
   //Choosing the root folder creates a red message for the easyimporter
   //choosing the USerFiles directory shows 0 files to be imported !?
   //changing the user folder doesn't trigger an update on the easyimport
   private static final String ROOT_FOLDER = "/";                           //$NON-NLS-1$

   final IPreferenceStore      _prefStore  = CommonActivator.getPrefStore();

   private DbxRequestConfig    _requestConfig;

   private DbxClientV2         _dropboxClient;
   private List<Metadata>      _folderList;

   private TableViewer         _contentViewer;
   private String              _selectedFolder;
   private ArrayList<String>   _selectedFiles;

   private String              _accessToken;

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

      _accessToken = _prefStore.getString(ICommonPreferences.DROPBOX_ACCESSTOKEN);

      //It is possible that the user just retrieved an access token but hasn't saved it yet
      //in the preferences
      if (StringUtils.isNullOrEmpty(_accessToken) &&
            !StringUtils.isNullOrEmpty(accessToken)) {
         _accessToken = accessToken;
      }

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

      //Getting the current version of MyTourbook
      final Version version = FrameworkUtil.getBundle(getClass()).getVersion();

      _requestConfig = DbxRequestConfig.newBuilder("mytourbook/" + version.toString().replace(".qualifier", "")).build(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      _dropboxClient = new DbxClientV2(_requestConfig, _accessToken);

      updateViewers();

      // enableControls();

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
         //TODO FB
         _buttonParentFolder.setToolTipText("TODO: Go to Parent folder");
         _buttonParentFolder.setImage(Activator.getImageDescriptor(Messages.Image__Dropbox_Parentfolder).createImage());
         GridDataFactory.fillDefaults().applyTo(_buttonParentFolder);
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
         _textSelectedAbsolutePath = new Text(container, SWT.LEFT);
         _textSelectedAbsolutePath.setEditable(false);
         //TODO FB
         // _textAccessToken.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
         GridDataFactory.fillDefaults()
               .applyTo(_textSelectedAbsolutePath);
         _textSelectedAbsolutePath.setText(ROOT_FOLDER);

         createUI_10_FilterViewer(container);
      }

      return container;
   }

   private void createUI_10_FilterViewer(final Composite parent) {

      final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().span(3, 1).grab(true, true).hint(600, 300).applyTo(layouter);

      final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
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

      _contentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelectItem(event.getSelection(), false);
         }
      });

      _contentViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {
            onSelectItem(event.getSelection(), true);
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

      if (_chooserType == ChooserType.Folder) {
         _selectedFolder = _textSelectedAbsolutePath.getText();
      } else if (_chooserType == ChooserType.File) {
         final StructuredSelection selection = (StructuredSelection) _contentViewer.getSelection();
         final Object[] selectionArray = selection.toArray();
         if (selectionArray.length > 0) {
            _selectedFiles = new ArrayList<>();

            final Metadata item = ((Metadata) selection.toArray()[0]);

            _selectedFiles.add(item.getPathDisplay());
         }
      }

      super.okPressed();
   }

   protected void onClickParentFolder() {

      final String currentFolder = _textSelectedAbsolutePath.getText();

      final int endIndex = currentFolder.lastIndexOf(ROOT_FOLDER);
      if (endIndex != -1) {
         final String parentFolder = currentFolder.substring(0, endIndex);
         selectFolder(parentFolder);
      }
   }

   protected void onSelectItem(final ISelection selectedItem, final boolean doubleClick) {
      final StructuredSelection selection = (StructuredSelection) selectedItem;
      final Object[] selectionArray = selection.toArray();
      if (selectionArray.length == 0) {
         return;
      }

      final Metadata item = ((Metadata) selection.toArray()[0]);

      if (item instanceof FolderMetadata) {

         if (doubleClick) {
            selectFolder(item.getPathDisplay());
         }
      }
   }

   private void selectFolder(final String folderAbsolutePath) {

      try {
         final ListFolderResult list = _dropboxClient.files().listFolder(folderAbsolutePath);
         _folderList = list.getEntries();

         _textSelectedAbsolutePath.setText(folderAbsolutePath);

         _buttonParentFolder.setEnabled(_textSelectedAbsolutePath.getText().length() > 1);

         _contentViewer.refresh();

      } catch (final DbxException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();

      }
   }

   private void updateViewers() {

      selectFolder(UI.EMPTY_STRING);

      // show contents in the viewer
      _contentViewer.setInput(new Object());

   }
}
