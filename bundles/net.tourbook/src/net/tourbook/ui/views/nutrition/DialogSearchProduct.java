/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package net.tourbook.ui.views.nutrition;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.nutrition.NutritionQuery;
import net.tourbook.nutrition.openfoodfacts.Product;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class DialogSearchProduct extends Dialog implements PropertyChangeListener {

   //todo fb
   //put a link with "Not finding the product you used ? You can create it here"
   //https://world.openfoodfacts.org/cgi/product.pl

   //todo fb
   // enable the "add" button only if an element is selected in the table
   //todo fb
   //Sync error after adding a 2nd product

   private static final IPreferenceStore _prefStore             = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                 = TourbookPlugin.getState("net.tourbook.ui.views.nutrition.DialogSearchProduct");//$NON-NLS-1$
   private static final String           STATE_SEARCHED_QUERIES = "searched.queries";                                                            //$NON-NLS-1$

   private TableViewer                   _productsViewer;
   private List<Product>                 _products;

   private long                          _tourId;

   private PixelConverter                _pc;
   private boolean                       _isInUIInit;

   /*
    * UI controls
    */
   private Button                        _btnAdd;
   private Button                        _btnSearch;
   private List<String>                  _searchHistory  = new ArrayList<>();

   private Combo                         _cboSearchQuery;
   private ComboViewer                   _queryViewer;
   private final NutritionQuery          _nutritionQuery = new NutritionQuery();

   private PostSelectionProvider         _postSelectionProvider;

   private final Image                   _iconPlaceholder;
   private final HashMap<Integer, Image> _graphImages    = new HashMap<>();

   private IPropertyChangeListener       _prefChangeListener;

   private class SearchContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {
         //Nothing to do
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _searchHistory.toArray(new String[_searchHistory.size()]);
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
         // Nothing to do
      }
   }

   private class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {
         //Nothing to do
      }

      @Override
      public Object[] getElements(final Object parent) {

         return _products == null ? new String[] {} : _products.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
         // Nothing to do
      }
   }

   private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

      @Override
      public Image getColumnImage(final Object obj, final int index) {

         if (index == 0) {
            return getImage(obj);
         }
         return null;
      }

      @Override
      public String getColumnText(final Object obj, final int index) {

         final Product product = (Product) obj;

         switch (index) {
         case 0:
            return product.code();

         case 1:
            return product.productName();

         default:
            return getText(obj);
         }
      }

      @Override
      public Image getImage(final Object obj) {

         //todo fb display the image from the url ?
         return null;
      }
   }

   /**
    * @param parentShell
    * @param tourId
    */
   public DialogSearchProduct(final Shell parentShell, final long tourId) {

      super(parentShell);

      // make dialog resizable and display maximize button
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

      // set icon for the window
      setDefaultImage(TourbookPlugin.getImageDescriptor(Images.TourNutrition).createImage());

      _tourId = tourId;
      _iconPlaceholder = TourbookPlugin.getImageDescriptor(Images.App_EmptyIcon_Placeholder).createImage();

   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _productsViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _productsViewer.refresh();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);

      _nutritionQuery.addPropertyChangeListener(this);
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_SearchProduct_Title);

      shell.addDisposeListener(disposeEvent -> onDispose());
   }

   @Override
   public void create() {

      addPrefListener();

      // create UI widgets
      super.create();

//      _isInUIInit = true;
//      {
//         restoreState();
//      }
//      _isInUIInit = false;

      enableActions();

   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);
      getButton(IDialogConstants.OK_ID).setVisible(false);
      getButton(IDialogConstants.CANCEL_ID).setVisible(false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider("ID");

      //todo fb
      // https://www.vogella.com/tutorials/EclipseJFaceTable/article.html

      restoreState();

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_10_Header(container);
         createUI_20_Viewer(container);
      }
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite queryContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(queryContainer);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(5, 5, 2, 2)
            .spacing(5, 0)
            .numColumns(3)
            .applyTo(queryContainer);
      {
         {
            /*
             * combo: search
             */
            _cboSearchQuery = new Combo(queryContainer, SWT.NONE);
            _cboSearchQuery.setVisibleItemCount(30);
            _cboSearchQuery.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchProduct()));
            _cboSearchQuery.addModifyListener(event -> _btnSearch.getShell().setDefaultButton(_btnSearch));
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_cboSearchQuery);
         }
         {
            /*
             * button: search
             */
            _btnSearch = new Button(queryContainer, SWT.PUSH);
            _btnSearch.setText(Messages.Dialog_SearchProduct_Button_Search);
            _btnSearch.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchProduct()));
         }
         {
            /*
             * Button: Add
             */
            _btnAdd = new Button(queryContainer, SWT.PUSH);
            _btnAdd.setText(Messages.Dialog_SearchProduct_Button_Add);
            _btnAdd.setToolTipText(Messages.Dialog_SearchProduct_Button_Add_Tooltip);
            _btnAdd.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAddProduct()));
         }
      }

      _queryViewer = new ComboViewer(_cboSearchQuery);
      _queryViewer.setContentProvider(new SearchContentProvider());
      _queryViewer.setComparator(new ViewerComparator());
   }

   private void createUI_20_Viewer(final Composite parent) {

      /*
       * table viewer: products
       */
      final Table productsTable = new Table(parent, /* SWT.BORDER | */SWT.SINGLE | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(productsTable);
      productsTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      productsTable.setHeaderVisible(true);

      // column: category
      final TableColumn columnCategory = new TableColumn(productsTable, SWT.LEFT);
      columnCategory.setText("Category"); //$NON-NLS-1$
      columnCategory.setWidth(75);

      // column: name
      final TableColumn columnName = new TableColumn(productsTable, SWT.LEFT);
      columnName.setText("Name"); //$NON-NLS-1$
      columnName.setWidth(300);

      _productsViewer = new TableViewer(productsTable);

      _productsViewer.setContentProvider(new ViewContentProvider());
      _productsViewer.setLabelProvider(new ViewLabelProvider());

      _productsViewer.addPostSelectionChangedListener(selectionChangedEvent -> {

         final ISelection selection = selectionChangedEvent.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final Product selectedProduct = (Product) firstElement;

         _postSelectionProvider.setSelection(selectedProduct);
      });
   }

   private void enableActions() {

      _btnAdd.setEnabled(_productsViewer.getSelection() != null);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
//		return null;
      return _state;
   }

   @Override
   protected void okPressed() {

      super.okPressed();
   }

   private void onAddProduct() {

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         final ISelection selection = _productsViewer.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final Product selectedProduct = (Product) firstElement;

//         final OpenFoodFactsWrapperImpl wrapper = new OpenFoodFactsWrapperImpl();
//         final ProductResponse productResponse = wrapper.fetchProductByCode(selectedProduct.code());

         final TourData tourData = TourManager.getTour(_tourId);
         final TourNutritionProduct tfp = new TourNutritionProduct(tourData, selectedProduct);
         tourData.addNutritionProduct(tfp);

         TourManager.saveModifiedTour(tourData);
      });
   }

   private void onDispose() {

      _iconPlaceholder.dispose();

      _graphImages.values().forEach(image -> image.dispose());

      _prefStore.removePropertyChangeListener(_prefChangeListener);
   }

   private void onSearchProduct() {

      // disable search controls
      _cboSearchQuery.setEnabled(false);
      _btnSearch.setEnabled(false);

      final String searchText = _cboSearchQuery.getText();

      // remove same search text
      if (_searchHistory.contains(searchText) == false) {

         // update model
         _searchHistory.add(searchText);

         // update viewer
         _queryViewer.add(searchText);
      }

      // start product search

      _nutritionQuery.asyncFind(searchText);
   }

   @Override
   public void propertyChange(final PropertyChangeEvent propertyChangeEvent) {

      @SuppressWarnings("unchecked")
      final List<net.tourbook.nutrition.openfoodfacts.Product> searchResults =
            (List<net.tourbook.nutrition.openfoodfacts.Product>) propertyChangeEvent.getNewValue();

      if (searchResults != null) {
         _products = searchResults;
      }

      Display.getDefault().asyncExec(() -> {

         // check if view is closed
         if (_btnSearch.isDisposed()) {
            return;
         }

         // refresh viewer
         _productsViewer.setInput(new Object());

         // select first entry, if there is one
         final Table productsTable = _productsViewer.getTable();
         if (productsTable.getItemCount() > 0) {

            final Object firstData = productsTable.getItem(0).getData();
            if (firstData instanceof Product) {

               _productsViewer.setSelection(new StructuredSelection(firstData));
               setViewerFocus();
            }
         }

         _cboSearchQuery.setEnabled(true);
         _btnSearch.setEnabled(true);
      });

   }

   private void restoreState() {

      // restore old used queries
      final String[] stateSearchedQueries = _state.getArray(STATE_SEARCHED_QUERIES);
      if (stateSearchedQueries != null) {
         Stream.of(stateSearchedQueries).forEach(query -> _searchHistory.add(query));
      }

      // update content in the comboviewer
      _queryViewer.setInput(new Object());
   }

   private void saveState() {
      _state.put(STATE_SEARCHED_QUERIES, _searchHistory.toArray(new String[_searchHistory.size()]));

   }

   /**
    * set focus to selected item, selection and focus are not the same !!!
    */
   private void setViewerFocus() {

      final Table table = _productsViewer.getTable();

      table.setSelection(table.getSelectionIndex());
      table.setFocus();
   }

}