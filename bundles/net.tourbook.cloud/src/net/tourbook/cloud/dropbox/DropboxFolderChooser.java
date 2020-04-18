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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class DropboxFolderChooser extends TitleAreaDialog {
//TODO Add the absolutepath ontop in a text field
   // TODO Add the parent ".." to go back a folder
   //TODO activate the "Choose"/OK button only when a folder is selected
   //TODO add a parameter to give the user the abaility to select a file (so that this class can be resued in the easy import
   //TODO Import configuration to detect new files from Dropbox acount ?
   //TODO Revert to original oauth2 browser and add only my necessary code
   //TODO remove unused imports
   private final IPreferenceStore    _prefStore = TourbookPlugin.getDefault().getPreferenceStore();

   private SelectionAdapter          _defaultSelectionAdapter;
   private MouseWheelListener        _defaultMouseWheelListener;
   private IPropertyChangeListener   _prefChangeListener;

   private long                      _dragStartViewerLeft;

   private boolean                   _isModified;

   private ArrayList<TourType>       _tourTypes;
   private ArrayList<TourTypeFilter> _filterList;

   private TourTypeFilter            _activeFilter;

   /*
    * UI controls
    */
   private TableViewer _filterViewer;

   private Button      _chkTourTypeContextMenu;

   private Spinner     _spinnerRecentTourTypes;

   public DropboxFolderChooser(final Shell parentShell) {

      super(parentShell);

      setShellStyle(getShellStyle() | SWT.RESIZE);

      //TODO put a dropbox image
      setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

   }

   @Override
   public void create() {

      super.create();

      setTitle(net.tourbook.cloud.dropbox.Messages.Dialog_DropboxFolderChooser_Area_Title);

   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      initUI();
      createUI(dlgAreaContainer);

      _filterViewer.add("dd");

      // enableControls();

      return dlgAreaContainer;

   }

   private Composite createUI(final Composite parent) {

      Label label = new Label(parent, SWT.WRAP);
      label.setText(Messages.Pref_TourTypes_root_title);
      label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * Dropbox folder path
          */
         final Text _textAccessToken = new Text(container, SWT.BORDER);
         _textAccessToken.setEditable(false);
         _textAccessToken.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .applyTo(_textAccessToken);
         _textAccessToken.setText("/");

         createUI_10_FilterViewer(container);
      }

      // hint to use drag & drop
      label = new Label(parent, SWT.WRAP);
      label.setText(Messages.Pref_TourTypes_dnd_hint);
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
      final TableItem item = new TableItem(table, SWT.NONE);
      item.setText("ddsss");
      TableViewerColumn tvc;

      _filterViewer = new TableViewer(table);

      // column: name + image
      tvc = new TableViewerColumn(_filterViewer, SWT.NONE);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final String filter = ((String) cell.getElement());
            final String filterName = null;
            final Image filterImage = null;

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

            cell.setText(filterName);
            cell.setImage(filterImage);
         }
      });
      layouter.addColumnData(new ColumnWeightData(1));

      _filterViewer.setContentProvider(new IStructuredContentProvider() {
         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {
            return _filterList.toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _filterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelectFolder();
            System.out.println("selection");
         }
      });

      _filterViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent arg0) {
            onSelectFolder();
            System.out.println("DOUBLECLICK");

         }

      });

   }

   private void initUI() {

      _defaultSelectionAdapter = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeProperty();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(event);
            onChangeProperty();
         }
      };
   }

   /**
    * Property was changed, fire a property change event
    */
   private void onChangeProperty() {
      _isModified = true;
   }

   private void onDeleteFilterSet() {

      final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _filterViewer.getSelection())
            .getFirstElement();

      if (filterItem == null || filterItem.getFilterType() != TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {
         return;
      }

      final Table filterTable = _filterViewer.getTable();
      final int selectionIndex = filterTable.getSelectionIndex();

      _filterViewer.remove(filterItem);

      // select next filter item
      final int nextIndex = Math.min(filterTable.getItemCount() - 1, selectionIndex);
      _filterViewer.setSelection(new StructuredSelection(_filterViewer.getElementAt(nextIndex)));

      _isModified = true;
   }

   private void onMoveDown() {

      final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _filterViewer.getSelection())
            .getFirstElement();

      if (filterItem == null) {
         return;
      }

      final Table filterTable = _filterViewer.getTable();
      final int selectionIndex = filterTable.getSelectionIndex();

      if (selectionIndex < filterTable.getItemCount() - 1) {

         _filterViewer.remove(filterItem);
         _filterViewer.insert(filterItem, selectionIndex + 1);

         // reselect moved item
         _filterViewer.setSelection(new StructuredSelection(filterItem));

         _isModified = true;
      }
   }

   private void onMoveUp() {

      final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _filterViewer.getSelection())
            .getFirstElement();

      if (filterItem == null) {
         return;
      }

      final Table filterTable = _filterViewer.getTable();

      final int selectionIndex = filterTable.getSelectionIndex();
      if (selectionIndex > 0) {
         _filterViewer.remove(filterItem);
         _filterViewer.insert(filterItem, selectionIndex - 1);

         // reselect moved item
         _filterViewer.setSelection(new StructuredSelection(filterItem));

         _isModified = true;
      }
   }

   private void onSelectFolder() {
//TODO we display the folder and files inside that new selected folder
      final TourTypeFilter filterItem = (TourTypeFilter) ((StructuredSelection) _filterViewer.getSelection())
            .getFirstElement();

      if (filterItem == null) {
         return;
      }

      _activeFilter = filterItem;

      final int filterType = filterItem.getFilterType();

      final Object[] tourTypes;
      switch (filterType) {
      case TourTypeFilter.FILTER_TYPE_SYSTEM:
         final int systemFilter = filterItem.getSystemFilterId();

         break;

      case TourTypeFilter.FILTER_TYPE_DB:
         final TourType tourType = filterItem.getTourType();
         break;

      case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
         break;

      default:
         break;
      }

   }

   private void onSelectTourType() {

      if (_activeFilter == null) {
         return;
      }

   }

   private void restoreState() {

      _chkTourTypeContextMenu.setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU));
      _spinnerRecentTourTypes.setSelection(_prefStore.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES));
   }

   private void saveState() {

      if (_isModified) {

         _isModified = false;

         TourTypeFilterManager.writeXMLFilterFile(_filterViewer);

         _prefStore.setValue(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU, _chkTourTypeContextMenu.getSelection());
         _prefStore.setValue(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES, _spinnerRecentTourTypes.getSelection());

         // fire modify event
         _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
      }
   }

   private void updateViewers() {

      _filterList = TourTypeFilterManager.readTourTypeFilters();
      _tourTypes = TourDatabase.getAllTourTypes();

      // show contents in the viewers
      _filterViewer.setInput(new Object());

   }

}
