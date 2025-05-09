/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourMarker;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StreamUtils;
import net.tourbook.data.FlatGainLoss;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourMarkerType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ActionDeleteMarkerDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.SubMenu_SetTourMarkerType;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.views.referenceTour.SelectionReferenceTourView;
import net.tourbook.ui.views.referenceTour.TVIElevationCompareResult_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_RefTourItem;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMarkerView extends ViewPart implements ITourProvider, ITourViewer {

   public static final String        ID                              = "net.tourbook.views.TourMarkerView";       //$NON-NLS-1$

   private final IPreferenceStore    _prefStore                      = TourbookPlugin.getPrefStore();
   private final IPreferenceStore    _prefStore_Common               = CommonActivator.getPrefStore();
   private final IDialogSettings     _state                          = TourbookPlugin.getState("TourMarkerView"); //$NON-NLS-1$

   private TourData                  _tourData;

   private PostSelectionProvider     _postSelectionProvider;
   private ISelectionListener        _postSelectionListener;
   private IPropertyChangeListener   _prefChangeListener;
   private IPropertyChangeListener   _prefChangeListener_Common;
   private ITourEventListener        _tourEventListener;

   private MenuManager               _viewerMenuManager;
   private IContextMenuProvider      _tableViewerContextMenuProvider = new TableContextMenuProvider();

   private ActionOpenMarkerDialog    _actionEditTourMarkers;
   private ActionDeleteMarkerDialog  _actionDeleteTourMarkers;
   private ActionToolbarSlideout     _actionTourMarkerOptions;
   private SubMenu_SetTourMarkerType _actionSubMenu_SetTourMarkerType;

   private PixelConverter            _pc;

   private TableViewer               _markerViewer;
   private ColumnManager             _columnManager;

   private boolean                   _isInUpdate;
   private boolean                   _isMultipleTours;

   private ColumnDefinition          _colDefName;
   private ColumnDefinition          _colDefVisibility;

   private final NumberFormat        _nf3                            = NumberFormat.getNumberInstance();
   {
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
   }

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private Composite _pageNoData;
   private Composite _viewerContainer;

   private Font      _boldFont;

   private Menu      _tableContextMenu;

   private class MarkerViewerContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {

         if (_tourData == null) {
            return new Object[0];
         } else {

            Object[] tourMarkers;

            if (_tourData.isMultipleTours()) {

               tourMarkers = _tourData.multipleTourMarkers.toArray();

            } else {

               tourMarkers = _tourData.getTourMarkers().toArray();
            }

            return tourMarkers;
         }
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   /**
    * Sort the markers by time
    */
   private class MarkerViewerProfileComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

//         return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
// time is disabled because it's not always available in gpx files

         if (_isMultipleTours) {

            return ((TourMarker) (obj1)).getMultiTourSerieIndex() - ((TourMarker) (obj2)).getMultiTourSerieIndex();
         } else {

            return ((TourMarker) (obj1)).getSerieIndex() - ((TourMarker) (obj2)).getSerieIndex();
         }

      }
   }

   private class TableContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_tableContextMenu != null) {
            _tableContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {

         return _markerViewer.getTable().getSelectionCount() > 0
               ? _tableContextMenu : null;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _tableContextMenu = createUI_22_CreateViewerContextMenu();

         return _tableContextMenu;
      }

   }

   public TourMarkerView() {
      super();
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _markerViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
            _markerViewer.refresh();

         } else if (property.equals(ITourbookPreferences.TOURMARKERVIEW_USE_ELAPSED_TIME)
               || property.equals(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME)
               || property.equals(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME)) {

            refreshView();

         } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            // ensure that the tour is reloaded when tour marker types are modified, e.g. renamed
            clearView();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed

            refreshView();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = (workbenchPart, selection) -> {

         if (workbenchPart == TourMarkerView.this) {
            return;
         }

         onSelectionChanged(selection);
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (_isInUpdate || workbenchPart == TourMarkerView.this) {
            return;
         }

         if (tourEventId == TourEventId.TOUR_SELECTION && eventData instanceof final ISelection selection) {

            onSelectionChanged(selection);

         } else {

            if (_tourData == null) {
               return;
            }

            if (tourEventId == TourEventId.TOUR_CHANGED && eventData instanceof final TourEvent tourEvent) {

               final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();
               if (modifiedTours != null) {

                  // update modified tour

                  final long viewTourId = _tourData.getTourId();

                  // The view contains multiple tours
                  if (_tourData.isMultipleTours()) {

                     final List<Long> allTourIds = new ArrayList<>();
                     final List<Long> oldTourIDs = tourEvent.oldTourIDs;

                     if (oldTourIDs != null) {

                        // tour is saved but use the old tours to update the UI

                        allTourIds.addAll(oldTourIDs);

                     } else {

                        modifiedTours.forEach(tour -> allTourIds.add(tour.getTourId()));
                     }

                     _tourData = TourManager.createJoinedTourData(allTourIds);
                     _isMultipleTours = true;

                     updateUI_MarkerViewer();

                     // removed old tour data from the selection provider
                     _postSelectionProvider.clearSelection();

                  } else {

                     // The view contains a single tour
                     for (final TourData tourData : modifiedTours) {
                        if (tourData.getTourId() == viewTourId) {

                           // get modified tour
                           _tourData = tourData;
                           _isMultipleTours = tourData.isMultipleTours();

                           updateUI_MarkerViewer();

                           // removed old tour data from the selection provider
                           _postSelectionProvider.clearSelection();

                           // nothing more to do, the view contains only one tour
                           return;
                        }
                     }
                  }

               }

            } else if (tourEventId == TourEventId.MARKER_SELECTION) {

               onTourEvent_TourMarker(eventData);

            } else if (tourEventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _tourData = null;

      updateUI_MarkerViewer();

      _postSelectionProvider.clearSelection();
   }

   /**
    * Computes the average speed between two markers (in km/h or mph)
    *
    * @param cell
    *
    * @return
    */
   private double computeAverageSpeed(final ViewerCell cell) {

      final int previousMarkerIndex = getPreviousMarkerIndex(cell);

      int currentMarkerIndex = getCurrentMarkerIndex(cell);
      if (_tourData.isMultipleTours()) {
         currentMarkerIndex = getMultiTourSerieIndex(currentMarkerIndex);
      }

      final float[] distanceSerieMetric = _tourData.getMetricDistanceSerie();
      final int[] timeSerie = _tourData.timeSerie;
      if (distanceSerieMetric == null || timeSerie == null) {
         return 0.0;
      }

      //The distance in km or miles
      final float distanceDifference = (distanceSerieMetric[currentMarkerIndex] - distanceSerieMetric[previousMarkerIndex])
            / UI.UNIT_VALUE_DISTANCE
            / 1000;

      final int timeDifference = timeSerie[currentMarkerIndex] - timeSerie[previousMarkerIndex] - getStoppedTimeBetweenIndices(previousMarkerIndex,
            currentMarkerIndex);

      final double averageSpeed = timeDifference == 0 ? 0.0 : 3600 * distanceDifference / timeDifference;

      return averageSpeed;
   }

   private void createActions() {

      _actionTourMarkerOptions = new ActionTourMarkerOptions(_pageBook);
      _actionEditTourMarkers = new ActionOpenMarkerDialog(this, true);
      _actionDeleteTourMarkers = new ActionDeleteMarkerDialog(this);
      _actionSubMenu_SetTourMarkerType = new SubMenu_SetTourMarkerType();
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(manager -> fillContextMenu(manager));
   }

   @Override
   public void createPartControl(final Composite parent) {

      _pc = new PixelConverter(parent);
      _boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

      createMenuManager();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      _columnManager.setIsCategoryAvailable(true);
      defineAllColumns();

      createUI(parent);

      addSelectionListener();
      addTourEventListener();
      addPrefListener();

      createActions();
      fillToolbar();

      // this part is a selection provider
      _postSelectionProvider = new PostSelectionProvider(ID);
      getSite().setSelectionProvider(_postSelectionProvider);

      // show default page
      _pageBook.showPage(_pageNoData);

      // show marker from last selection
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      if (_tourData == null) {
         showTourFromTourProvider();
      }
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = net.tourbook.common.UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _viewerContainer = new Composite(_pageBook, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_TableViewer(_viewerContainer);
      }
   }

   private void createUI_10_TableViewer(final Composite parent) {

      /*
       * create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI /* | SWT.BORDER */);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      table.addKeyListener(keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.DEL) {

            if (_actionDeleteTourMarkers.isEnabled() == false) {
               return;
            }

            // Retrieves the markers that were selected in the marker dialog
            final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
            _actionDeleteTourMarkers.setTourMarkers(selection.toArray());
            _actionDeleteTourMarkers.run();

         }
      }));

      /*
       * create table viewer
       */
      _markerViewer = new TableViewer(table);

//      // set editing support after the viewer is created but before the columns are created
//      net.tourbook.common.UI.setCellEditSupport(_markerViewer);
//
//      _colDefName.setEditingSupport(new MarkerEditingSupportLabel(_markerViewer));
//      _colDefVisibility.setEditingSupport(new MarkerEditingSupportVisibility(_markerViewer));

      _columnManager.createColumns(_markerViewer);

      _markerViewer.setUseHashlookup(true);
      _markerViewer.setContentProvider(new MarkerViewerContentProvider());
      _markerViewer.setComparator(new MarkerViewerProfileComparator());

      _markerViewer.addSelectionChangedListener(
            selectionChangedEvent -> onSelect_TourMarker((StructuredSelection) selectionChangedEvent.getSelection()));

      _markerViewer.addDoubleClickListener(doubleClickEvent -> {

         if (isTourSavedInDB() == false) {
            return;
         }

         // edit selected marker
         final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
         if (selection.size() > 0) {
            _actionEditTourMarkers.setTourMarker((TourMarker) selection.getFirstElement());
            _actionEditTourMarkers.run();
         }
      });

      createUI_20_ContextMenu();
   }

   /**
    * create the views context menu
    */
   private void createUI_20_ContextMenu() {

      _tableContextMenu = createUI_22_CreateViewerContextMenu();

      final Table table = (Table) _markerViewer.getControl();

      _columnManager.createHeaderContextMenu(table, _tableViewerContextMenuProvider);
   }

   private Menu createUI_22_CreateViewerContextMenu() {

      final Table table = (Table) _markerViewer.getControl();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      return tableContextMenu;
   }

   private void defineAllColumns() {

      defineColumn_Marker_IsVisible();

      defineColumn_Time_MarkerDate();
      defineColumn_Time_MarkerTime();

      defineColumn_Time_RelativeTime();
      defineColumn_Time_TimeDelta();

      defineColumn_Motion_Distance();
      defineColumn_Motion_DistanceDelta();
      defineColumn_Motion_AvgPace();
      defineColumn_Motion_AvgSpeed();

      defineColumn_Altitude_ElevationGainDelta();
      defineColumn_Altitude_ElevationLossDelta();
      defineColumn_Altitude_AvgGradient();

      defineColumn_Body_AvgPulse();

      defineColumn_Waypoint_Name();
      defineColumn_Marker_Type();
      defineColumn_Waypoint_Description();
      defineColumn_Marker_Url();

      defineColumn_Data_SerieIndex();
   }

   private void defineColumn_Altitude_AvgGradient() {

      final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_GRADIENT_AVG.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int previousMarkerIndex = getPreviousMarkerIndex(cell);

            final int currentMarkerIndex = getCurrentMarkerIndex(cell);

            final double averageSlope = StreamUtils.computeAverage(_tourData.getGradientSerie(), previousMarkerIndex, currentMarkerIndex);

            colDef.printDetailValue(cell, averageSlope);
         }
      });
   }

   /**
    * Column: Elevation gain
    */
   private void defineColumn_Altitude_ElevationGainDelta() {

      final ColumnDefinition colDef = TableColumnFactory.MARKER_ALTITUDE_ELEVATION_GAIN_DELTA.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int previousMarkerIndex = getPreviousMarkerIndex(cell);

            final int currentMarkerIndex = getCurrentMarkerIndex(cell);

            final FlatGainLoss elevationGainLoss = _tourData.computeAltitudeUpDown(previousMarkerIndex, currentMarkerIndex);

            if (elevationGainLoss == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               final double value = elevationGainLoss.elevationGain / UI.UNIT_VALUE_ELEVATION;
               colDef.printValue_0(cell, value);
            }
         }
      });
   }

   /**
    * Column: Elevation loss
    */
   private void defineColumn_Altitude_ElevationLossDelta() {
      final ColumnDefinition colDef = TableColumnFactory.MARKER_ALTITUDE_ELEVATION_LOSS_DELTA.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int previousMarkerIndex = getPreviousMarkerIndex(cell);

            final int currentMarkerIndex = getCurrentMarkerIndex(cell);

            final FlatGainLoss elevationGainLoss = _tourData.computeAltitudeUpDown(
                  previousMarkerIndex,
                  currentMarkerIndex);

            if (elevationGainLoss == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               final double value = elevationGainLoss.elevationLoss / UI.UNIT_VALUE_ELEVATION;

               colDef.printValue_0(cell, value);
            }
         }
      });
   }

   private void defineColumn_Body_AvgPulse() {
      final ColumnDefinition colDef = TableColumnFactory.BODY_AVG_PULSE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int previousMarkerIndex = getPreviousMarkerIndex(cell);

            final int currentMarkerIndex = getCurrentMarkerIndex(cell);

            final float averagePace = _tourData.computeAvg_PulseSegment(previousMarkerIndex, currentMarkerIndex);

            colDef.printValue_0(cell, averagePace);
         }
      });
   }

   /**
    * Column: Serie index
    */
   private void defineColumn_Data_SerieIndex() {

      _colDefName = TableColumnFactory.MARKER_SERIE_INDEX.createColumn(_columnManager, _pc);

      _colDefName.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker marker = (TourMarker) cell.getElement();

            cell.setText(Integer.toString(marker.getSerieIndex()));
         }
      });
   }

   /**
    * Column: Is visible
    */
   private void defineColumn_Marker_IsVisible() {

      _colDefVisibility = TableColumnFactory.MARKER_MAP_VISIBLE.createColumn(_columnManager, _pc);

      _colDefVisibility.setIsDefaultColumn();

      _colDefVisibility.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            cell.setText(tourMarker.isMarkerVisible()
                  ? Messages.App_Label_BooleanYes
                  : Messages.App_Label_BooleanNo);
         }
      });
   }

   /**
    * Column: Marker type
    */
   private void defineColumn_Marker_Type() {

      final ColumnDefinition colDef = TableColumnFactory.MARKER_TYPE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker marker = (TourMarker) cell.getElement();
            final TourMarkerType markerType = marker.getTourMarkerType();

            if (markerType == null) {

               cell.setForeground(null);
               cell.setBackground(null);
               cell.setText(UI.EMPTY_STRING);

            } else {

               cell.setForeground(markerType.getForegroundColorSWT());
               cell.setBackground(markerType.getBackgroundColorSWT());
               cell.setText(markerType.getTypeName());
            }
         }
      });
   }

   /**
    * Column: Url
    */
   private void defineColumn_Marker_Url() {

      final ColumnDefinition colDef = TableColumnFactory.MARKER_URL.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker marker = (TourMarker) cell.getElement();

            String columnText = UI.EMPTY_STRING;

            /*
             * Url
             */
            final String urlText = marker.getUrlText();
            final String urlAddress = marker.getUrlAddress();
            final boolean isText = urlText.length() > 0;
            final boolean isAddress = urlAddress.length() > 0;

            if (isText || isAddress) {

               if (isAddress == false) {

                  // only text is in the link -> this is not a internet address but create a link of it

                  columnText = urlText;

               } else {

                  columnText = urlAddress;
               }
            }

            cell.setText(columnText);
         }
      });
   }

   /**
    * Column: Average Pace
    */
   private void defineColumn_Motion_AvgPace() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final double averageSpeed = computeAverageSpeed(cell);
            final double averagePace = averageSpeed == 0.0 ? 0.0 : 3600 / averageSpeed;

            cell.setText(UI.format_mm_ss((long) averagePace));
         }
      });
   }

   /**
    * Column: Average Speed (km/h or mph)
    */
   private void defineColumn_Motion_AvgSpeed() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final double averageSpeed = computeAverageSpeed(cell);

            colDef.printDoubleValue(cell, averageSpeed, cell.getElement() instanceof TourMarker);
         }
      });
   }

   /**
    * Column: Distance km/mi
    */
   private void defineColumn_Motion_Distance() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();

            final float markerDistance = tourMarker.getDistance();
            if (markerDistance == -1) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               final double value = markerDistance / 1000 / UI.UNIT_VALUE_DISTANCE;
               colDef.printDetailValue(cell, value);
            }

            if (tourMarker.getType() == ChartLabelMarker.MARKER_TYPE_DEVICE) {
               cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            }
         }
      });
   }

   /**
    * Column: Distance delta km/mi
    */
   private void defineColumn_Motion_DistanceDelta() {

      final ColumnDefinition colDef = TableColumnFactory.MOTION_DISTANCE_DELTA.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();

            final float markerDistance = tourMarker.getDistance();

            if (markerDistance == -1) {

               cell.setText(UI.EMPTY_STRING);

            } else {

               float prevDistance = 0;
               final ViewerRow lastRow = cell.getViewerRow().getNeighbor(ViewerRow.ABOVE, false);

               if (null != lastRow) {
                  final TourMarker element = (TourMarker) lastRow.getElement();
                  if (element instanceof TourMarker) {
                     prevDistance = element.getDistance();
                  }
                  prevDistance = prevDistance < 0 ? 0 : prevDistance;
               }

               final double value = (markerDistance - prevDistance) / 1000 / UI.UNIT_VALUE_DISTANCE;
               colDef.printDetailValue(cell, value);
            }
         }
      });
   }

   /**
    * Column: Marker date
    */
   private void defineColumn_Time_MarkerDate() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_DATE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();

            final TourData tourData = tourMarker.getTourData();
            final ZonedDateTime tourStartTime = tourData.getTourStartTime();
            final ZonedDateTime markerTime = TimeTools.getZonedDateTime(
                  tourMarker.getTourTime(),
                  tourStartTime.getZone());

            cell.setText(TimeTools.Formatter_Date_S.format(markerTime));
         }
      });
   }

   /**
    * Column: Marker time
    */
   private void defineColumn_Time_MarkerTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_TIME.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();

            final TourData tourData = tourMarker.getTourData();
            final ZonedDateTime tourStartTime = tourData.getTourStartTime();
            final ZonedDateTime markerTime = TimeTools.getZonedDateTime(
                  tourMarker.getTourTime(),
                  tourStartTime.getZone());

            cell.setText(TimeTools.Formatter_Time_S.format(markerTime));
         }
      });
   }

   /**
    * Column: Relative time
    */
   private void defineColumn_Time_RelativeTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_TIME_HH_MM_SS.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      // hide wrong tooltip
      colDef.setColumnHeaderToolTipText(UI.EMPTY_STRING);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker marker = (TourMarker) cell.getElement();
            final long time = marker.getTime() - getStoppedTimeBetweenIndices(0, marker.getSerieIndex());

            cell.setText(net.tourbook.common.UI.format_hh_mm_ss(time));
         }

      });
   }

   /**
    * Column: Time
    */
   private void defineColumn_Time_TimeDelta() {

      final ColumnDefinition colDef = TableColumnFactory.MARKER_TIME_DELTA.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ViewerRow lastRow = cell.getViewerRow().getNeighbor(ViewerRow.ABOVE, false);
            int lastTime = 0;
            int lastSerieIndex = 0;
            TourData lastTourData = null;
            if (null != lastRow) {

               final Object element = lastRow.getElement();

               if (element instanceof final TourMarker tourMarker) {

                  lastTime = tourMarker.getTime();
                  lastSerieIndex = tourMarker.getSerieIndex();
                  lastTourData = tourMarker.getTourData();
               }
            }

            final TourMarker currentTourMarker = ((TourMarker) cell.getElement());
            final int currentTime = currentTourMarker.getTime();
            final TourData currentTourData = currentTourMarker.getTourData();

            int timeDifference = currentTime - lastTime;
            if (lastTourData != null && !lastTourData.getTourId().equals(currentTourData.getTourId())) {
               timeDifference = currentTime;
            }

            int currentMarkerIndex = getCurrentMarkerIndex(cell);
            if (_tourData.isMultipleTours()) {
               currentMarkerIndex = getMultiTourSerieIndex(currentMarkerIndex);
            }

            timeDifference -= getStoppedTimeBetweenIndices(lastSerieIndex, currentMarkerIndex);

            cell.setText(net.tourbook.common.UI.format_hh_mm_ss(timeDifference));

            final String text = currentTourMarker.getLabel();

            /*
             * Show text in red/bold when the text ends with a !, this hidden feature was introduced
             * by helmling
             */
            if (text.endsWith(UI.SYMBOL_EXCLAMATION_POINT)) {

               final Display display = Display.getCurrent();

               if (null != display) {
                  cell.setForeground(display.getSystemColor(SWT.COLOR_RED));
               }

               cell.setFont(_boldFont);
            }
         }
      });
   }

   /**
    * Column: Description
    */
   private void defineColumn_Waypoint_Description() {

      final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_DESCRIPTION.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker marker = (TourMarker) cell.getElement();
            cell.setText(marker.getDescription());
         }
      });
   }

   /**
    * Column: Name
    */
   private void defineColumn_Waypoint_Name() {

      _colDefName = TableColumnFactory.WAYPOINT_NAME.createColumn(_columnManager, _pc);

      _colDefName.setIsDefaultColumn();

      _colDefName.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker marker = (TourMarker) cell.getElement();

            cell.setText(marker.getLabel());
         }
      });
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      getSite().getPage().removePostSelectionListener(_postSelectionListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   /**
    * Enable actions
    */
   private void enableActions() {

      final boolean isTourSaved = isTourSavedInDB();
      final boolean isTour = _tourData != null;
      final boolean isSingleTour = isTour && _tourData.isMultipleTours() == false;

      final boolean isSingleSaveTour = isTourSaved && isSingleTour;

      _actionEditTourMarkers.setEnabled(isSingleSaveTour);
      _actionDeleteTourMarkers.setEnabled(isSingleSaveTour);
      _actionSubMenu_SetTourMarkerType.setEnabled(isTour);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      // set the marker which should be selected in the marker dialog
      final IStructuredSelection selection = (IStructuredSelection) _markerViewer.getSelection();
      final TourMarker firstMarker = (TourMarker) selection.getFirstElement();

      final Object[] allTourMarker = selection.toArray();

      _actionDeleteTourMarkers.setTourMarkers(allTourMarker);
      _actionEditTourMarkers.setTourMarker(firstMarker);

      _actionSubMenu_SetTourMarkerType.setTourMarker(allTourMarker);
      _actionSubMenu_SetTourMarkerType.setOldTourIDs(getOldTourIDs());

      menuMgr.add(_actionEditTourMarkers);
      menuMgr.add(_actionSubMenu_SetTourMarkerType);

      menuMgr.add(new Separator());
      menuMgr.add(_actionDeleteTourMarkers);

      // add standard group which allows other plug-ins to contribute here
      menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

      enableActions();
   }

   private void fillToolbar() {

      /*
       * View toolbar
       */
      final IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
      toolBarManager.add(_actionTourMarkerOptions);

//      final IActionBars actionBars = getViewSite().getActionBars();

      /*
       * Fill view menu
       */
//      final IMenuManager menuMgr = actionBars.getMenuManager();
//
//      menuMgr.add(new Separator());

      /*
       * Fill view toolbar
       */
//      final IToolBarManager tbm = actionBars.getToolBarManager();
//
//      tbm.add();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   /**
    * Retrieves the index of the marker currently selected.
    *
    * @param cell
    *
    * @return
    */
   private int getCurrentMarkerIndex(final ViewerCell cell) {

      return ((TourMarker) cell.getElement()).getSerieIndex();
   }

   private int getMultiTourSerieIndex(final int currentMarkerIndex) {

      if (_tourData == null || _tourData.multipleTourMarkers == null) {
         return 0;
      }

      final TourMarker result = _tourData.multipleTourMarkers.stream()
            .filter(t -> t.getSerieIndex() == currentMarkerIndex)
            .findAny()
            .orElse(null);

      int multiTourSerieIndex = 0;
      if (result != null) {
         multiTourSerieIndex = result.getMultiTourSerieIndex();
      }
      return multiTourSerieIndex;
   }

   private List<Long> getOldTourIDs() {

      if (_tourData.isMultipleTours()) {

         final Long[] multipleTourIds = _tourData.multipleTourIds;

         return Arrays.asList(multipleTourIds);

      } else {

         final List<Long> allOldTourIDs = new ArrayList<>();

         allOldTourIDs.add(_tourData.getTourId());

         return allOldTourIDs;
      }
   }

   /**
    * Retrieves the index of the marker located before the current marker.
    *
    * @param cell
    *
    * @return
    */
   private int getPreviousMarkerIndex(final ViewerCell cell) {

      final ViewerRow lastRow = cell.getViewerRow().getNeighbor(ViewerRow.ABOVE, false);
      int previousMarkerIndex = 0;
      if (null != lastRow) {
         final Object element = lastRow.getElement();
         if (element instanceof final TourMarker tourMarker) {
            previousMarkerIndex = tourMarker.getSerieIndex();
         }
      }
      return previousMarkerIndex;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> selectedTours = new ArrayList<>();

      if (_tourData != null) {
         selectedTours.add(_tourData);
      }

      return selectedTours;
   }

   private int getStoppedTimeBetweenIndices(final int previousMarkerIndex, final int currentMarkerIndex) {

      if (useRecordedTime()) {
         return _tourData.getPausedTime(previousMarkerIndex, currentMarkerIndex);
      } else if (useMovingTime()) {
         return _tourData.getBreakTime(previousMarkerIndex, currentMarkerIndex);
      }

      return 0;
   }

   @Override
   public ColumnViewer getViewer() {
      return _markerViewer;
   }

   /**
    * @return Returns <code>true</code> when the tour is saved in the database.
    */
   private boolean isTourSavedInDB() {

      if ((_tourData != null) && (_tourData.getTourPerson() != null)) {
         return true;
      }

      return false;
   }

   private void onSelect_TourMarker(final StructuredSelection selection) {

      if (_isInUpdate) {
         return;
      }

      final ArrayList<TourMarker> selectedTourMarker = new ArrayList<>();

      for (final Object name : selection) {
         selectedTourMarker.add((TourMarker) name);
      }

      TourManager.fireEventWithCustomData(
            TourEventId.MARKER_SELECTION,
            new SelectionTourMarker(_tourData, selectedTourMarker),
            getSite().getPart());
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_isInUpdate || selection == null) {
         return;
      }

      long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;
      TourData tourData = null;

      if (selection instanceof final SelectionTourData tourDataSelection) {

         // a tour was selected, get the chart and update the marker viewer

         tourData = tourDataSelection.getTourData();

      } else if (selection instanceof final SelectionTourId selectionTourId) {

         tourId = selectionTourId.getTourId();

      } else if (selection instanceof final SelectionTourIds selectionTourIds) {

         final ArrayList<Long> tourIds = selectionTourIds.getTourIds();

         if (tourIds != null && tourIds.size() > 0) {

            if (tourIds.size() == 1) {
               tourId = tourIds.get(0);
            } else {
               tourData = TourManager.createJoinedTourData(tourIds);
            }
         }

      } else if (selection instanceof final SelectionReferenceTourView tourCatalogSelection) {

         final TVIRefTour_RefTourItem refItem = tourCatalogSelection.getRefItem();
         if (refItem != null) {
            tourId = refItem.getTourId();
         }

      } else if (selection instanceof final StructuredSelection structuredSelection) {

         final Object firstElement = structuredSelection.getFirstElement();
         if (firstElement instanceof final TVIRefTour_ComparedTour tviRefTour_ComparedTour) {
            tourId = tviRefTour_ComparedTour.getTourId();
         } else if (firstElement instanceof final TVIElevationCompareResult_ComparedTour tviElevationCompareResult_ComparedTour) {
            tourId = tviElevationCompareResult_ComparedTour.getTourId();
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }

      if (tourData == null) {

         if (tourId >= TourDatabase.ENTITY_IS_NOT_SAVED) {

            tourData = TourManager.getInstance().getTourData(tourId);
            if (tourData != null) {
               _tourData = tourData;
               _isMultipleTours = tourData.isMultipleTours();
            }
         }
      } else {

         _tourData = tourData;
         _isMultipleTours = tourData.isMultipleTours();
      }

      updateUI_MarkerViewer();
   }

   private void onTourEvent_TourMarker(final Object eventData) {

      if (eventData instanceof final SelectionTourMarker selection) {

         /*
          * Select the tour marker in the view
          */
         final TourData tourData = selection.getTourData();
         final ArrayList<TourMarker> tourMarker = selection.getSelectedTourMarker();

         if (tourData != _tourData) {

            _tourData = tourData;
            _isMultipleTours = tourData.isMultipleTours();

            updateUI_MarkerViewer();
         }

         _isInUpdate = true;
         {
            _markerViewer.setSelection(new StructuredSelection(tourMarker), true);
         }
         _isInUpdate = false;
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _markerViewer.getTable().dispose();

         createUI_10_TableViewer(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _markerViewer;
   }

   private void refreshView() {

      _columnManager.saveState(_state);
      _columnManager.clearColumns();

      defineAllColumns();

      _markerViewer = (TableViewer) recreateViewer(_markerViewer);
   }

   @Override
   public void reloadViewer() {

      updateUI_MarkerViewer();
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);
   }

   @Override
   public void setFocus() {

      _markerViewer.getTable().setFocus();
   }

   private void showTourFromTourProvider() {

      _pageBook.showPage(_pageNoData);

      // a tour is not displayed, find a tour provider which provides a tour
      Display.getCurrent().asyncExec(() -> {

         // validate widget
         if (_pageBook.isDisposed()) {
            return;
         }

         /*
          * check if tour was set from a selection provider
          */
         if (_tourData != null) {
            return;
         }

         onSelectionChanged(TourManager.getSelectedToursSelection());
      });
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   private void updateUI_MarkerViewer() {

      if (_tourData == null) {

         _pageBook.showPage(_pageNoData);

      } else {

         _isInUpdate = true;
         {
            _markerViewer.setInput(new Object[0]);
         }
         _isInUpdate = false;

         _pageBook.showPage(_viewerContainer);
      }

      enableActions();
   }

   private boolean useMovingTime() {

      return _prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_MOVING_TIME);
   }

   private boolean useRecordedTime() {

      return _prefStore.getBoolean(ITourbookPreferences.TOURMARKERVIEW_USE_RECORDED_TIME);
   }
}
