/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package net.tourbook.ui.views;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import de.byteholder.gpx.PointOfInterest;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.nutrition.NutritionQuery;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

public class TourNutritionView extends ViewPart implements PropertyChangeListener {

   public static final String            ID                     = "de.byteholder.geoclipse.poi.poiView"; //$NON-NLS-1$

   private static final String           STATE_SEARCHED_NUTRITIONQUERIES = "searched.nutritionQueries";           //$NON-NLS-1$

   private static final String           IMG_KEY_ANCHOR         = "anchor";                              //$NON-NLS-1$
   private static final String           IMG_KEY_CAR            = "car";                                 //$NON-NLS-1$
   private static final String           IMG_KEY_CART           = "cart";                                //$NON-NLS-1$
   private static final String           IMG_KEY_FLAG           = "flag";                                //$NON-NLS-1$
   private static final String           IMG_KEY_HOUSE          = "house";                               //$NON-NLS-1$
   private static final String           IMG_KEY_SOCCER         = "soccer";                              //$NON-NLS-1$
   private static final String           IMG_KEY_STAR           = "star";                                //$NON-NLS-1$

   private static final IPreferenceStore _prefStore             = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                 = TourbookPlugin.getState("PoiView");    //$NON-NLS-1$

   private TableViewer                   _poiViewer;
   private List<String>                  _pois;
   private List<String>                  _searchHistory         = new ArrayList<>();

   private PostSelectionProvider         _postSelectionProvider;

   private IPropertyChangeListener       _prefChangeListener;
   final NutritionQuery                  _nutritionQuery                 = new NutritionQuery();
   /*
    * UI controls
    */
   private Button      _btnSearch;

   private Combo       _cboSearchQuery;
   private ComboViewer _queryViewer;

   public class SearchContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _searchHistory.toArray(new String[_searchHistory.size()]);
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   class ViewContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object parent) {
         if (_pois == null) {
            return new String[] {};
         } else {
            return _pois.toArray();
         }
      }

      @Override
      public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
   }

   class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

      @Override
      public Image getColumnImage(final Object obj, final int index) {
         switch (index) {
         case 0:
            return getImage(obj);
         default:
            return null;
         }
      }

      @Override
      public String getColumnText(final Object obj, final int index) {

         return obj.toString();
//         final PointOfInterest poi = (PointOfInterest) obj;
//
//         switch (index) {
//         case 0:
//            return poi.getCategory();
//         case 1:
//
//            final StringBuilder sb = new StringBuilder(poi.getName());
//
//            final List<? extends Waypoint> nearestPlaces = poi.getNearestPlaces();
//            if (nearestPlaces != null && nearestPlaces.size() > 0) {
//
//               // create a string with all nearest waypoints
//               boolean isFirstPoi = true;
//               for (final Waypoint waypoint : nearestPlaces) {
//
//                  if (isFirstPoi) {
//                     isFirstPoi = false;
//                     sb.append("Messages.Poi_View_Label_NearestPlacesPart1");
//                     sb.append("Messages.Poi_View_Label_Near");
//                  } else {
//                     sb.append("Messages.Poi_View_Label_NearestPlacesPart2");
//                  }
//
//                  sb.append("Messages.Poi_View_Label_NearestPlacesPart3");
//                  sb.append(waypoint.getName());
//               }
//               sb.append("Messages.Poi_View_Label_NearestPlacesPart4");
//            }
//            return sb.toString();
//         default:
//            return getText(obj);
//         }
      }

      @Override
      public Image getImage(final Object obj) {

         if (obj instanceof PointOfInterest) {

            Image img;
            final PointOfInterest poi = (PointOfInterest) obj;

            // TODO find/make better matching icons

            final ImageRegistry imageRegistry = TourbookPlugin.getDefault().getImageRegistry();
            final String poiCategory = poi.getCategory();

            if (poiCategory.equals("highway")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_CAR);
            } else if (poiCategory.equals("place")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_HOUSE);
            } else if (poiCategory.equals("waterway")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_ANCHOR);
            } else if (poiCategory.equals("amenity")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_CART);
            } else if (poiCategory.equals("leisure")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_STAR);
            } else if (poiCategory.equals("sport")) { //$NON-NLS-1$
               img = imageRegistry.get(IMG_KEY_SOCCER);
            } else {
               img = imageRegistry.get(IMG_KEY_FLAG);
            }

            return img;
         } else {
            return null;
         }
      }
   }

   public TourNutritionView() {}

   public TourNutritionView(final List<String> pois) {
      _pois = pois;
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _poiViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _poiViewer.refresh();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);

      _nutritionQuery.addPropertyChangeListener(this);
   }

   @Override
   public void createPartControl(final Composite parent) {

      initImageRegistry();

      createUI(parent);

      addPrefListener();

      // this part is a selection provider
      //getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

      restoreState();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(container);
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
             * label: POI
             */
            final Label label = new Label(queryContainer, SWT.NONE);
            label.setText("Messages.Poi_View_Label_POI");
            label.setToolTipText("Messages.Poi_View_Label_POI_Tooltip");
         }
         {
            /*
             * combo: search
             */
            _cboSearchQuery = new Combo(queryContainer, SWT.NONE);
            _cboSearchQuery.setVisibleItemCount(30);
            _cboSearchQuery.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
               // start searching when ENTER is pressed
               onSearchPoi();
            }));
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
            _btnSearch.setText("Messages.Poi_View_Button_Search");
            _btnSearch.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSearchPoi()));
         }
      }

      _queryViewer = new ComboViewer(_cboSearchQuery);
      _queryViewer.setContentProvider(new SearchContentProvider());
      _queryViewer.setComparator(new ViewerComparator());

      // add autocomplete feature to the combo viewer
      // this feature is disable because it's not working very well
//    new AutoComplete(_queryViewer);
   }

   private void createUI_20_Viewer(final Composite parent) {

      /*
       * table viewer: poi items
       */
      final Table poiTable = new Table(parent, /* SWT.BORDER | */SWT.SINGLE | SWT.FULL_SELECTION);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(poiTable);
      poiTable.setLinesVisible(true);
      poiTable.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
      poiTable.setHeaderVisible(true);

      // column: category
      final TableColumn columnCategory = new TableColumn(poiTable, SWT.LEFT);
      columnCategory.setText("Category"); //$NON-NLS-1$
      columnCategory.setWidth(75);

      // column: name
      final TableColumn columnName = new TableColumn(poiTable, SWT.LEFT);
      columnName.setText("Name"); //$NON-NLS-1$
      columnName.setWidth(300);

      _poiViewer = new TableViewer(poiTable);

      _poiViewer.setContentProvider(new ViewContentProvider());
      _poiViewer.setLabelProvider(new ViewLabelProvider());

      _poiViewer.addPostSelectionChangedListener(selectionChangedEvent -> {

         final ISelection selection = selectionChangedEvent.getSelection();
         final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
         final PointOfInterest selectedPoi = (PointOfInterest) firstElement;

         _postSelectionProvider.setSelection(selectedPoi);
      });
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _nutritionQuery.removePropertyChangeListener(this);

      super.dispose();
   }

   private void initImageRegistry() {

      final TourbookPlugin activator = TourbookPlugin.getDefault();
      final ImageRegistry imageRegistry = activator.getImageRegistry();

      if (imageRegistry.get(Images.POI_Anchor) == null) {

         imageRegistry.put(IMG_KEY_ANCHOR, TourbookPlugin.getImageDescriptor(Images.POI_Anchor));
         imageRegistry.put(IMG_KEY_CAR, TourbookPlugin.getImageDescriptor(Images.POI_Car));
         imageRegistry.put(IMG_KEY_CART, TourbookPlugin.getImageDescriptor(Images.POI_Cart));
         imageRegistry.put(IMG_KEY_FLAG, TourbookPlugin.getImageDescriptor(Images.POI_Flag));
         imageRegistry.put(IMG_KEY_HOUSE, TourbookPlugin.getImageDescriptor(Images.POI_House));
         imageRegistry.put(IMG_KEY_SOCCER, TourbookPlugin.getImageDescriptor(Images.POI_Soccer));
         imageRegistry.put(IMG_KEY_STAR, TourbookPlugin.getImageDescriptor(Images.POI_Star));
      }
   }

   private void onSearchPoi() {

      // disable search controls
      _cboSearchQuery.setEnabled(false);
      _btnSearch.setEnabled(false);

      final String searchText = _cboSearchQuery.getText();

      // remove same search text
      if (_searchHistory.contains(searchText) == false) {

         // update model
         // _searchHistory.add(searchText);

         // update viewer
         _queryViewer.add(searchText);
      }

      // start product search

      _nutritionQuery.asyncFind(searchText);
   }

   @Override
   public void propertyChange(final PropertyChangeEvent evt) {

      final List<String> searchResult = (List<String>) evt.getNewValue();

      if (searchResult != null) {
         _pois = searchResult;
      }

      Display.getDefault().asyncExec(() -> {

         // check if view is closed
         if (_btnSearch.isDisposed()) {
            return;
         }

         // refresh viewer
         _poiViewer.setInput(new Object());

         // select first entry, if there is one
         final Table poiTable = _poiViewer.getTable();
         if (poiTable.getItemCount() > 0) {

            final Object firstData = poiTable.getItem(0).getData();
            if (firstData instanceof PointOfInterest) {

               _poiViewer.setSelection(new StructuredSelection(firstData));
               setViewerFocus();
            }
         }

         _cboSearchQuery.setEnabled(true);
         _btnSearch.setEnabled(true);
      });

   }

   private void restoreState() {

      // restore old used queries
      final String[] stateSearchedQueries = _state.getArray(STATE_SEARCHED_NUTRITIONQUERIES);
      if (stateSearchedQueries != null) {
         Stream.of(stateSearchedQueries).forEach(query -> _searchHistory.add(query));
      }

      // update content in the comboviewer
      _queryViewer.setInput(new Object());
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_SEARCHED_NUTRITIONQUERIES, _searchHistory.toArray(new String[_searchHistory.size()]));
   }

   @Override
   public void setFocus() {

      // set default button
      _btnSearch.getShell().setDefaultButton(_btnSearch);

      // set focus
      _cboSearchQuery.setFocus();
   }

   /**
    * set focus to selected item, selection and focus are not the same !!!
    */
   private void setViewerFocus() {

      final Table table = _poiViewer.getTable();

      table.setSelection(table.getSelectionIndex());
      table.setFocus();
   }

}