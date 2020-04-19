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

import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.ICloudPreferences;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class DropboxFolderChooser extends TitleAreaDialog {
   //TODO activate the "Choose"/OK button only when a folder is selected
   //TODO add a parameter to give the user the abaility to select a file (so that this class can be resued in the easy import
   //TODO Import configuration to detect new files from Dropbox acount ?
   //TODO Revert to original oauth2 browser and add only my necessary code
   //TODO remove unused imports
//TODO Add button to go back to the parent folder (API call for that ?)
   //TODO double or single click to enter a folder ? compare with GC. Also, does GC disable the OK button when selecting a file ?
   //TODO put the SVG of Dropbox in cloud.svg

   private IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();

   private DbxRequestConfig _requestConfig;
   private DbxClientV2      _dropboxClient;

   private List<Metadata>   _folderList;
   private TableViewer      _contentViewer;

   private String           _selectedFolder;
   private String           _accessToken;

   /*
    * UI controls
    */
   private Text _textSelectedAbsolutePath;

   public DropboxFolderChooser(final Shell parentShell) {

      super(parentShell);

      setShellStyle(getShellStyle() | SWT.RESIZE);

      //TODO put a dropbox image
      //setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

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

      _accessToken = _prefStore.getString(ICloudPreferences.DROPBOX_ACCESSTOKEN);

      //TODO How to get the current MT version !?
      _requestConfig = DbxRequestConfig.newBuilder("mytourbook/20.3.0").build();

      _dropboxClient = new DbxClientV2(_requestConfig, _accessToken);

      updateViewers();

      // enableControls();

      return dlgAreaContainer;

   }

   private Composite createUI(final Composite parent) {

      Label label = new Label(parent, SWT.WRAP);
      // label.setText(Messages.Pref_TourTypes_root_title);
      label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * Dropbox folder path
          */
         _textSelectedAbsolutePath = new Text(container, SWT.BORDER);
         _textSelectedAbsolutePath.setEditable(false);
         // _textAccessToken.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .applyTo(_textSelectedAbsolutePath);
         _textSelectedAbsolutePath.setText("/"); //$NON-NLS-1$

         createUI_10_FilterViewer(container);
      }

      // hint to use drag & drop
      label = new Label(parent, SWT.WRAP);
      // label.setText(Messages.Pref_TourTypes_dnd_hint);
      label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

      // spacer
      new Label(parent, SWT.WRAP);

      return container;
   }

   private void createUI_10_FilterViewer(final Composite parent) {

      final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).hint(200, SWT.DEFAULT).applyTo(layouter);

      final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
      table.setHeaderVisible(false);
      table.setLinesVisible(false);
      /*
       * final TableItem item = new TableItem(table, SWT.NONE);
       * item.setText("dkehfkjefgbakj");
       * item.setImage(TourbookPlugin.getImageDescriptor(net.tourbook.cloud.dropbox.Messages.
       * Image__Dropbox_folder).createImage());
       */ TableViewerColumn tvc;

      _contentViewer = new TableViewer(table);

      // column: name + image
      tvc = new TableViewerColumn(_contentViewer, SWT.NONE);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Metadata entry = ((Metadata) cell.getElement());

            String filterName = null;
            Image filterImage = null;

            filterName = entry.getName();

            if (entry instanceof FolderMetadata) {

               filterImage =
                     Activator.getImageDescriptor(Messages.Image__Dropbox_folder).createImage();
            } else if (entry instanceof FileMetadata) {

               filterImage =
                     Activator.getImageDescriptor(Messages.Image__Dropbox_file).createImage();
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

   public String getSelectedFolder() {
      return _selectedFolder;
   }

   @Override
   protected void okPressed() {

      _selectedFolder = _textSelectedAbsolutePath.getText();

      super.okPressed();
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

         _contentViewer.refresh();

      } catch (final DbxException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();

      }
   }

   private void updateViewers() {

      selectFolder("");

      // show contents in the viewer
      _contentViewer.setInput(new Object());

   }
}
