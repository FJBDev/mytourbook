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

import java.util.ArrayList;

import net.tourbook.cloud.Activator;
import net.tourbook.common.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class DropboxFolderChooser extends TitleAreaDialog {
//TODO Add the absolutepath ontop in a text field
   // TODO Add the parent ".." to go back a folder
   //TODO activate the "Choose"/OK button only when a folder is selected
   //TODO add a parameter to give the user the abaility to select a file (so that this class can be resued in the easy import
   //TODO Import configuration to detect new files from Dropbox acount ?
   //TODO Revert to original oauth2 browser and add only my necessary code
   //TODO remove unused imports

   private boolean                   _isModified;

   private ArrayList<String> _filterList;

   private String                    _selectedFolder;

   /*
    * UI controls
    */
   private TableViewer _contentViewer;
   private Text        _textAccessToken;

   private Button      _chkTourTypeContextMenu;

   private Spinner     _spinnerRecentTourTypes;

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
         _textAccessToken = new Text(container, SWT.BORDER);
         _textAccessToken.setEditable(false);
         // _textAccessToken.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .applyTo(_textAccessToken);
         _textAccessToken.setText("/"); //$NON-NLS-1$

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

            String filterName = null;
            Image filterImage = null;

            //TODO Detect if it's a folder or a file to change the icon
            // set filter name/image

            /*
             * switch (filterType) {
             * case TourTypeFilter.FILTER_TYPE_DB:
             * final TourType tourType = filter.getTourType();
             * filterName = tourType.getName();
             * //filterImage = "";//TODO TourTypeImage.getTourTypeImage(tourType.getTypeId());
             * break;
             * case TourTypeFilter.FILTER_TYPE_SYSTEM:
             * filterName = filter.getSystemFilterName();
             * filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER_SYSTEM);
             * break;
             * case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
             * filterName = filter.getTourTypeSet().getName();
             * filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER);
             * break;
             * default:
             * break;
             * }
             */
            filterName = "fdlkenfgkjgbahrsk";
            filterImage =
                  Activator.getImageDescriptor(Messages.Image__Dropbox_folder).createImage();

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
            return _filterList.toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _contentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelectFolder();
         }
      });

      _contentViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent arg0) {
            onSelectFolder();
            //Update the path string above
         }

      });

   }

   public String getSelectedFolder() {
      return _selectedFolder;
   }

   @Override
   protected void okPressed() {

      // get selected table item
      final StructuredSelection selection = (StructuredSelection) _contentViewer.getSelection();
      if (selection.size() == 0) {
         _selectedFolder = "/"; //$NON-NLS-1$
      } else {
         //TODO if multiple items selected, disable to OK button so that we are sure to be here with only 1 element.
         final Object[] selectedFolder = selection.toArray();
         //  _selectedFolder = ((TourTypeFilter) selectedFolder[0]).getFilterName();
      }
      super.okPressed();
   }



   private void onMoveDown() {
//TODO on selection changed rather than moving up and down, open the folder content if folder
/*
 * final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection)
 * _contentViewer.getSelection())
 * .getFirstElement();
 * if (filterItem == null) {
 * return;
 * }
 * final Table filterTable = _contentViewer.getTable();
 * final int selectionIndex = filterTable.getSelectionIndex();
 * if (selectionIndex < filterTable.getItemCount() - 1) {
 * _contentViewer.remove(filterItem);
 * _contentViewer.insert(filterItem, selectionIndex + 1);
 * // reselect moved item
 * _contentViewer.setSelection(new StructuredSelection(filterItem));
 * _isModified = true;
 * }
 */
   }

   private void onSelectFolder() {
//TODO we display the folder and files inside that new selected folder
/*
 * final TourTypeFilter filterItem = (TourTypeFilter) ((StructuredSelection)
 * _contentViewer.getSelection())
 * .getFirstElement();
 * if (filterItem == null) {
 * return;
 * }
 * final int filterType = filterItem.getFilterType();
 * final Object[] tourTypes;
 * switch (filterType) {
 * case TourTypeFilter.FILTER_TYPE_SYSTEM:
 * final int systemFilter = filterItem.getSystemFilterId();
 * break;
 * case TourTypeFilter.FILTER_TYPE_DB:
 * final TourType tourType = filterItem.getTourType();
 * break;
 * case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
 * break;
 * default:
 * break;
 * }
 */

   }

   private void onSelectTourType() {


   }

   private void restoreState() {

   }

   private void saveState() {

      if (_isModified) {

         _isModified = false;

      }
   }

   private void updateViewers() {

      _filterList = new ArrayList<>();
      _filterList.add("TOTO");

      // show contents in the viewer
      _contentViewer.setInput(new Object());

      //    enableButtons();
   }
}
