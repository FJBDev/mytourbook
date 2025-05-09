/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.collateTours;

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.ViewContext;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tour.printing.ActionPrint;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeSQLData;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionComputeDistanceValuesFromGeoposition;
import net.tourbook.ui.action.ActionComputeElevationGain;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetElevationValuesFromSRTM;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.ViewNames;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.ui.views.rawData.ActionReimportTours;
import net.tourbook.ui.views.tourBook.TVITourBookTour;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.part.ViewPart;

public class CollatedToursView extends ViewPart implements ITourProvider, ITourViewer3, ITourProviderByID, ITreeViewer {

   public static final String ID = "net.tourbook.ui.views.collateTours.CollatedToursView"; //$NON-NLS-1$

   private static Styler      DATE_STYLER;

   static {

      DATE_STYLER = StyledString.createColorRegistryStyler(net.tourbook.ui.UI.VIEW_COLOR_DATE_CATEGORY, null);
   }

   private final IPreferenceStore  _prefStore        = TourbookPlugin.getPrefStore();
   private final IPreferenceStore  _prefStore_Common = CommonActivator.getPrefStore();
   private final IDialogSettings   _state            = TourbookPlugin.getState(ID);
   //
   private ColumnManager           _columnManager;
   private OpenDialogManager       _openDlgMgr       = new OpenDialogManager();
   //
   private PostSelectionProvider   _postSelectionProvider;
   private ISelectionListener      _postSelectionListener;
   private ITourEventListener      _tourPropertyListener;
   private IPropertyChangeListener _prefChangeListener;
   private IPropertyChangeListener _prefChangeListener_Common;
   //
   private TVICollatedTour_Root    _rootItem;
   //
   private final NumberFormat      _nf0;
   private final NumberFormat      _nf1;
   private final NumberFormat      _nf1_NoGroup;

   {
      _nf0 = NumberFormat.getNumberInstance();
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1 = NumberFormat.getNumberInstance();
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);

      _nf1_NoGroup = NumberFormat.getNumberInstance();
      _nf1_NoGroup.setMinimumFractionDigits(1);
      _nf1_NoGroup.setMaximumFractionDigits(1);
      _nf1_NoGroup.setGroupingUsed(false);
   }

   private final ArrayList<Long> _selectedTourIds = new ArrayList<>();

   private boolean               _isInStartup;
   private boolean               _isInReload;
   private boolean               _isInUIUpdate;

   //
   private boolean                                    _isToolTipInCollation;
   private boolean                                    _isToolTipInTags;
   private boolean                                    _isToolTipInTime;
   private boolean                                    _isToolTipInTitle;
   private boolean                                    _isToolTipInWeekDay;
   //
   private final TourDoubleClickState                 _tourDoubleClickState      = new TourDoubleClickState();
   private TreeViewerTourInfoToolTip                  _tourInfoToolTip;
   //
   private TagMenuManager                             _tagMenuManager;
   private TourTypeMenuManager                        _tourTypeMenuManager;
   private MenuManager                                _viewerMenuManager;
   private IContextMenuProvider                       _viewerContextMenuProvider = new TreeContextMenuProvider();
   //
   private TreeColumnDefinition                       _colDef_TourTypeImage;
   private TreeColumnDefinition                       _colDef_WeatherClouds;
   private int                                        _columnIndex_TourTypeImage = -1;
   private int                                        _columnIndex_WeatherClouds = -1;
   private int                                        _columnWidth_TourTypeImage;
   private int                                        _columnWidth_WeatherClouds;
   //
   private HashMap<String, Object>                    _allTourActions_Adjust;
   private HashMap<String, Object>                    _allTourActions_Edit;
   private HashMap<String, Object>                    _allTourActions_Export;
   //
   private ActionCollapseAll                          _actionCollapseAll;
   private ActionCollapseOthers                       _actionCollapseOthers;
   private ActionComputeDistanceValuesFromGeoposition _actionComputeDistanceValuesFromGeoPosition;
   private ActionComputeElevationGain                 _actionComputeElevationGain;
   private ActionEditQuick                            _actionEditQuick;
   private ActionExpandSelection                      _actionExpandSelection;
   private ActionExport                               _actionExportTour;
   private ActionEditTour                             _actionEditTour;
   private ActionOpenTour                             _actionOpenTour;
   private ActionOpenMarkerDialog                     _actionOpenMarkerDialog;
   private ActionOpenAdjustAltitudeDialog             _actionOpenAdjustAltitudeDialog;
   private ActionJoinTours                            _actionJoinTours;
   private ActionMergeTour                            _actionMergeTour;
   private ActionPrint                                _actionPrintTour;
   private ActionRefreshView                          _actionRefreshView;
   private ActionReimportTours                        _actionReimport_Tours;
   private ActionSetElevationValuesFromSRTM           _actionSetElevationFromSRTM;
   private ActionSetPerson                            _actionSetOtherPerson;

   private CollateTourContributionItem                _contribItem_CollatedTours;

   private TreeViewer                                 _tourViewer;

   private PixelConverter                             _pc;

   /*
    * UI controls
    */
   private Composite _viewerContainer;

   private Menu      _treeContextMenu;

   private static class ItemComparer implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {
            return true;
         }

//         if (a instanceof TVITourBookYear && b instanceof TVITourBookYear) {
//
//            final TVITourBookYear item1 = (TVITourBookYear) a;
//            final TVITourBookYear item2 = (TVITourBookYear) b;
//            return item1.tourYear == item2.tourYear;
//         }
//
//         if (a instanceof TVITourBookYearSub && b instanceof TVITourBookYearSub) {
//
//            final TVITourBookYearSub item1 = (TVITourBookYearSub) a;
//            final TVITourBookYearSub item2 = (TVITourBookYearSub) b;
//            return item1.tourYear == item2.tourYear && item1.tourYearSub == item2.tourYearSub;
//         }
//
//         if (a instanceof TVICollatedTour_Tour && b instanceof TVICollatedTour_Tour) {
//
//            final TVICollatedTour_Tour item1 = (TVICollatedTour_Tour) a;
//            final TVICollatedTour_Tour item2 = (TVICollatedTour_Tour) b;
//            return item1.tourId == item2.tourId;
//         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }
   }

   private class TourBookContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getChildren(final Object parentElement) {
         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _rootItem.getFetchedChildrenAsArray();
      }

      @Override
      public Object getParent(final Object element) {
         return ((TreeViewerItem) element).getParentItem();
      }

      @Override
      public boolean hasChildren(final Object element) {
         return ((TreeViewerItem) element).hasChildren();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public class TreeContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_treeContextMenu != null) {
            _treeContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _treeContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _treeContextMenu = createUI_22_CreateViewerContextMenu();

         return _treeContextMenu;
      }

   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

//               fActivePerson = TourbookPlugin.getDefault().getActivePerson();
//               fActiveTourTypeFilter = TourbookPlugin.getDefault().getActiveTourTypeFilter();

            reloadViewer();

         } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            // update tourbook viewer
            _tourViewer.refresh();

            // redraw must be done to see modified tour type image colors
            _tourViewer.getTree().redraw();

         } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

            updateToolTipState();

         } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

//               updateDisplayFormats();

            _tourViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _tourViewer.refresh();

            /*
             * the tree must be redrawn because the styled text does not show with the new color
             */
            _tourViewer.getTree().redraw();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed

            _columnManager.saveState(_state);
            _columnManager.clearColumns();
            defineAllColumns();

            _tourViewer = (TreeViewer) recreateViewer(_tourViewer);
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addSelectionListener() {
      // this view part is a selection listener
      _postSelectionListener = (part, selection) -> {

         if (selection instanceof SelectionDeletedTours) {
            reloadViewer();
         }
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourPropertyListener = (part, eventId, eventData) -> {

         if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

            /*
             * it is possible when a tour type was modified, the tour can be hidden or visible in
             * the viewer because of the tour type filter
             */
            reloadViewer();

         } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
               || eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

            reloadViewer();
         }
      };
      TourManager.getInstance().addTourEventListener(_tourPropertyListener);
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionCollapseAll                           = new ActionCollapseAll(this);
      _actionCollapseOthers                        = new ActionCollapseOthers(this);
      _contribItem_CollatedTours                   = new CollateTourContributionItem(this);
      _actionComputeDistanceValuesFromGeoPosition  = new ActionComputeDistanceValuesFromGeoposition(this);
      _actionComputeElevationGain                  = new ActionComputeElevationGain(this);
      _actionEditQuick                             = new ActionEditQuick(this);
      _actionEditTour                              = new ActionEditTour(this);
      _actionExpandSelection                       = new ActionExpandSelection(this);
      _actionExportTour                            = new ActionExport(this);
      _actionJoinTours                             = new ActionJoinTours(this);
      _actionOpenMarkerDialog                      = new ActionOpenMarkerDialog(this, true);
      _actionOpenAdjustAltitudeDialog              = new ActionOpenAdjustAltitudeDialog(this);
      _actionMergeTour                             = new ActionMergeTour(this);
      _actionOpenTour                              = new ActionOpenTour(this);
      _actionPrintTour                             = new ActionPrint(this);
      _actionRefreshView                           = new ActionRefreshView(this);
      _actionReimport_Tours                        = new ActionReimportTours(this);
      _actionSetElevationFromSRTM                  = new ActionSetElevationValuesFromSRTM(this);
      _actionSetOtherPerson                        = new ActionSetPerson(this);

      _allTourActions_Adjust  = new HashMap<>();
      _allTourActions_Edit    = new HashMap<>();
      _allTourActions_Export  = new HashMap<>();

      _allTourActions_Edit.put(_actionEditQuick                   .getClass().getName(),  _actionEditQuick);
      _allTourActions_Edit.put(_actionEditTour                    .getClass().getName(),  _actionEditTour);
      _allTourActions_Edit.put(_actionOpenMarkerDialog            .getClass().getName(),  _actionOpenMarkerDialog);
      _allTourActions_Edit.put(_actionOpenAdjustAltitudeDialog    .getClass().getName(),  _actionOpenAdjustAltitudeDialog);
//    _allTourActions_Edit.put(_actionSetStartEndLocation         .getClass().getName(),  _actionSetStartEndLocation);
      _allTourActions_Edit.put(_actionOpenTour                    .getClass().getName(),  _actionOpenTour);
//    _allTourActions_Edit.put(_actionDuplicateTour               .getClass().getName(),  _actionDuplicateTour);
//    _allTourActions_Edit.put(_actionCreateTourMarkers           .getClass().getName(),  _actionCreateTourMarkers);
      _allTourActions_Edit.put(_actionMergeTour                   .getClass().getName(),  _actionMergeTour);
      _allTourActions_Edit.put(_actionJoinTours                   .getClass().getName(),  _actionJoinTours);

//    _allTourActions_Export.put(_actionUploadTour                .getClass().getName(),  _actionUploadTour);
      _allTourActions_Export.put(_actionExportTour                .getClass().getName(),  _actionExportTour);
//    _allTourActions_Export.put(_actionExportViewCSV             .getClass().getName(),  _actionExportViewCSV);
      _allTourActions_Export.put(_actionPrintTour                 .getClass().getName(),  _actionPrintTour);

//    _allTourActions_Adjust.put(_actionAdjustTourValues          .getClass().getName(),  _actionAdjustTourValues);
//    _allTourActions_Adjust.put(_actionDeleteTourValues          .getClass().getName(),  _actionDeleteTourValues);
      _allTourActions_Adjust.put(_actionReimport_Tours            .getClass().getName(),  _actionReimport_Tours);
      _allTourActions_Adjust.put(_actionSetOtherPerson            .getClass().getName(),  _actionSetOtherPerson);
//    _allTourActions_Adjust.put(_actionDeleteTourMenu            .getClass().getName(),  _actionDeleteTourMenu);

// SET_FORMATTING_ON

      TourActionManager.setAllViewActions(ID,
            _allTourActions_Edit.keySet(),
            _allTourActions_Export.keySet(),
            _allTourActions_Adjust.keySet(),
            _tagMenuManager.getAllTagActions().keySet(),
            _tourTypeMenuManager.getAllTourTypeActions().keySet());

      fillActionBars();
   }

   private void createMenuManager() {

      _tagMenuManager = new TagMenuManager(this, true);
      _tourTypeMenuManager = new TourTypeMenuManager(this);

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(manager -> fillContextMenu(manager));
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);
      createMenuManager();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      _columnManager.setIsCategoryAvailable(true);
      defineAllColumns();

      createUI(parent);
      createActions();

      addSelectionListener();
      addPrefListener();
      addTourEventListener();

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      // set column header according to the displayed values
      updateColumnHeader(null);

      restoreState();

      enableActions();

      // update the viewer
      _rootItem = new TVICollatedTour_Root(this);

      // delay loading, that the app filters are initialized
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            _isInStartup = true;

            _tourViewer.setInput(this);

            reselectTourViewer();
         }
      });
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_TourViewer(_viewerContainer);
      }
   }

   private void createUI_10_TourViewer(final Composite parent) {

      // tour tree
      final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourViewer = new TreeViewer(tree);
      _columnManager.createColumns(_tourViewer);

      _tourViewer.setContentProvider(new TourBookContentProvider());
      _tourViewer.setComparer(new ItemComparer());
      _tourViewer.setUseHashlookup(true);

      _tourViewer.addSelectionChangedListener(event -> onSelectTreeItem(event));

      _tourViewer.addDoubleClickListener(doubleClickEvent -> {

         final Object selection = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();

         if (selection instanceof TVICollatedTour_Tour //
//                  || selection instanceof TVICollatedTour_Event
         //
         ) {

            TourManager.getInstance().tourDoubleClickAction(CollatedToursView.this, _tourDoubleClickState);

         } else if (selection != null) {

            // expand/collapse current item

            final TreeViewerItem tourItem = (TreeViewerItem) selection;

            if (_tourViewer.getExpandedState(tourItem)) {
               _tourViewer.collapseToLevel(tourItem, 1);
            } else {
               _tourViewer.expandToLevel(tourItem, 1);
            }
         }
      });

      createUI_15_ColumnImages(tree);

      /*
       * the context menu must be created after the viewer is created which is also done after the
       * measurement system has changed
       */
      createUI_20_ContextMenu();

      // set tour info tooltip provider
      _tourInfoToolTip = new TreeViewerTourInfoToolTip(_tourViewer);
      _tourInfoToolTip.setNoTourTooltip(Messages.Collate_Tours_Label_DummyTour_Tooltip);
   }

   private void createUI_15_ColumnImages(final Tree tree) {

      boolean isColumnVisible = false;
      final ControlListener controlResizedAdapter = controlResizedAdapter(controlEvent -> onResize_SetWidthForImageColumn());

      // update column index which is needed for repainting
      final ColumnProfile activeProfile = _columnManager.getActiveProfile();
      _columnIndex_TourTypeImage = activeProfile.getColumnIndex(_colDef_TourTypeImage.getColumnId());
      _columnIndex_WeatherClouds = activeProfile.getColumnIndex(_colDef_WeatherClouds.getColumnId());

      final int numColumns = tree.getColumns().length;

      // add column resize listener
      if (_columnIndex_TourTypeImage >= 0 && _columnIndex_TourTypeImage < numColumns) {

         isColumnVisible = true;
         tree.getColumn(_columnIndex_TourTypeImage).addControlListener(controlResizedAdapter);
      }

      if (_columnIndex_WeatherClouds >= 0 && _columnIndex_WeatherClouds < numColumns) {

         isColumnVisible = true;
         tree.getColumn(_columnIndex_WeatherClouds).addControlListener(controlResizedAdapter);
      }

      // add table resize listener
      if (isColumnVisible) {

         /*
          * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
          * critical for performance that these methods be as efficient as possible.
          */
         final Listener paintListener = event -> {

            if (event.type == SWT.PaintItem) {

               onPaintViewer(event);
            }
         };

         tree.addControlListener(controlResizedAdapter);
         tree.addListener(SWT.PaintItem, paintListener);
      }
   }

   /**
    * create the views context menu
    */
   private void createUI_20_ContextMenu() {

      _treeContextMenu = createUI_22_CreateViewerContextMenu();

      final Tree tree = (Tree) _tourViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
   }

   private Menu createUI_22_CreateViewerContextMenu() {

      final Tree tree = (Tree) _tourViewer.getControl();

      final Menu treeContextMenu = _viewerMenuManager.createContextMenu(tree);

      treeContextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
         }
      });

      return treeContextMenu;
   }

   /**
    * Defines all columns for the table viewer in the column manager, the sequence defines the
    * default columns
    *
    * @param parent
    */
   private void defineAllColumns() {

      defineColumn_1stColumn_CollateEvent();
      defineColumn_Time_WeekDay();
      defineColumn_Time_TourStartTime();
      defineColumn_Time_MovingTime();
      defineColumn_Time_WeekNo();
      defineColumn_Time_WeekYear();
      defineColumn_Time_ElapsedTime();
      defineColumn_Time_RecordedTime();
      defineColumn_Time_PausedTime();
      defineColumn_Time_BreakTime_Relative();

      defineColumn_Tour_TypeImage();
      defineColumn_Tour_TypeText();
      defineColumn_Tour_Marker();
      defineColumn_Tour_Photos();
      defineColumn_Tour_Title();
      defineColumn_Tour_Tags();

      defineColumn_Motion_Distance();
      defineColumn_Motion_MaxSpeed();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_AvgPace();

      defineColumn_Altitude_Up();
      defineColumn_Altitude_Down();
      defineColumn_Altitude_MaxAltitude();

      defineColumn_Weather_Clouds();
      defineColumn_Weather_Temperature_Avg_Device();
      defineColumn_Weather_WindSpeed();
      defineColumn_Weather_WindDirection();

      defineColumn_Body_Calories();
      defineColumn_Body_RestPulse();
      defineColumn_Body_MaxPulse();
      defineColumn_Body_AvgPulse();
      defineColumn_Body_Person();

      defineColumn_Powertrain_AvgCadence();
      defineColumn_Powertrain_Gear_FrontShiftCount();
      defineColumn_Powertrain_Gear_RearShiftCount();

      defineColumn_Device_Distance();

      defineColumn_Data_NumTimeSlices();
      defineColumn_Data_TimeInterval();
      defineColumn_Data_DPTolerance();
   }

   /**
    * Tree column: Collate event
    */
   private void defineColumn_1stColumn_CollateEvent() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_COLLATE_EVENT.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);

      colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInCollation == false) {
               return null;
            }

            return getCellTourId(cell);
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVICollatedTour tourItem = (TVICollatedTour) element;

            if (element instanceof TVICollatedTour_Tour) {

               // tour item
               cell.setText(
                     TimeTools//
                           .getZonedDateTime(tourItem.colTourStartTime)
                           .format(TimeTools.Formatter_Date_S));

            } else if (element instanceof final TVICollatedTour_Event collatedEvent) {

               // collated event

               final StyledString styledString = new StyledString();

               /*
                * Event start
                */
               String startText;

               if (collatedEvent.isFirstEvent) {
                  startText = Messages.Collate_Tours_Label_TimeScale_BeforePresent;
               } else {
                  startText = collatedEvent.eventStart.format(TimeTools.Formatter_Date_S);
               }
               styledString.append(startText, DATE_STYLER);

               /*
                * Event end
                */
               styledString.append(UI.DASH_WITH_SPACE);
               final ZonedDateTime eventEnd = collatedEvent.eventEnd;
               if (eventEnd == null) {

                  // this can be null when the collation process is canceled by the user

                  styledString.append(UI.SYMBOL_QUESTION_MARK, DATE_STYLER);

               } else {

                  String endText;

                  if (collatedEvent.isLastEvent) {
                     endText = Messages.Collate_Tours_Label_TimeScale_Today;
                  } else {
                     endText = eventEnd.format(TimeTools.Formatter_Date_S);
                  }

                  styledString.append(endText, DATE_STYLER);
               }

               /*
                * Number of tours for each event
                */
               styledString.append(UI.SPACE3);
               styledString.append(_nf0.format(tourItem.colCounter), StyledString.QUALIFIER_STYLER);

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: altitude down (m)
    */
   private void defineColumn_Altitude_Down() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double dbAltitudeDown = ((TVICollatedTour) element).colAltitudeDown;
            final double value = -dbAltitudeDown / UI.UNIT_VALUE_ELEVATION;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: max altitude
    */
   private void defineColumn_Altitude_MaxAltitude() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_MAX.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbMaxAltitude = ((TVICollatedTour) element).colMaxAltitude;
            final double value = dbMaxAltitude / UI.UNIT_VALUE_ELEVATION;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: altitude up (m)
    */
   private void defineColumn_Altitude_Up() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbAltitudeUp = ((TVICollatedTour) element).colAltitudeUp;
            final double value = dbAltitudeUp / UI.UNIT_VALUE_ELEVATION;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: avg pulse
    */
   private void defineColumn_Body_AvgPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_AVG.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final float value = ((TVICollatedTour) element).colAvgPulse;

            colDef.printDoubleValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: calories
    */
   private void defineColumn_Body_Calories() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_CALORIES.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVICollatedTour) element).colCalories;

            cell.setText(FormatManager.formatNumber_0(value));

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: max pulse
    */
   private void defineColumn_Body_MaxPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVICollatedTour) element).colMaxPulse;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: person
    */
   private void defineColumn_Body_Person() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PERSON.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               final long dbPersonId = tviCollatedTour_Tour.colPersonId;

               cell.setText(PersonManager.getPersonName(dbPersonId));
            }
         }
      });
   }

   /**
    * column: rest pulse
    */
   private void defineColumn_Body_RestPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_RESTPULSE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVICollatedTour) element).colRestPulse;

            if (value == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(value));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: DP tolerance
    */
   private void defineColumn_Data_DPTolerance() {

      final TreeColumnDefinition colDef = TreeColumnFactory.DATA_DP_TOLERANCE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int dpTolerance = ((TVICollatedTour) element).colDPTolerance;

            if (dpTolerance == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf1.format(dpTolerance / 10.0));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: number of time slices
    */
   private void defineColumn_Data_NumTimeSlices() {

      final TreeColumnDefinition colDef = TreeColumnFactory.DATA_NUM_TIME_SLICES.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVICollatedTour) element).colNumberOfTimeSlices;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: time interval
    */

   private void defineColumn_Data_TimeInterval() {

      final TreeColumnDefinition colDef = TreeColumnFactory.DATA_TIME_INTERVAL.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               final short dbTimeInterval = tviCollatedTour_Tour.getColumnTimeInterval();
               if (dbTimeInterval == 0) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(Long.toString(dbTimeInterval));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: device distance
    */
   private void defineColumn_Device_Distance() {

      final TreeColumnDefinition colDef = TreeColumnFactory.DEVICE_DISTANCE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               final long dbStartDistance = tviCollatedTour_Tour.getColumnStartDistance();
               final double value = dbStartDistance / UI.UNIT_VALUE_DISTANCE;

               colDef.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: avg pace min/km - min/mi
    */
   private void defineColumn_Motion_AvgPace() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final float pace = ((TVICollatedTour) element).colAvgPace * UI.UNIT_VALUE_DISTANCE;

            if (pace == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(UI.format_mm_ss((long) pace));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: avg speed km/h - mph
    */
   private void defineColumn_Motion_AvgSpeed() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final float value = ((TVICollatedTour) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;

            colDef.printDoubleValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: distance (km/miles)
    */
   private void defineColumn_Motion_Distance() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVICollatedTour) element).colDistance
                  / 1000.0
                  / UI.UNIT_VALUE_DISTANCE;

            colDef.printDoubleValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: max speed
    */
   private void defineColumn_Motion_MaxSpeed() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_MAX_SPEED.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVICollatedTour) element).colMaxSpeed / UI.UNIT_VALUE_DISTANCE;

            colDef.printDoubleValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: avg cadence
    */
   private void defineColumn_Powertrain_AvgCadence() {

      final TreeColumnDefinition colDef = TreeColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final float value = ((TVICollatedTour) element).colAvgCadence;

            colDef.printDoubleValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Front shift count.
    */
   private void defineColumn_Powertrain_Gear_FrontShiftCount() {

      final TreeColumnDefinition colDef = TreeColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT.createColumn(
            _columnManager,
            _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVICollatedTour) element).colFrontShiftCount;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Rear shift count.
    */
   private void defineColumn_Powertrain_Gear_RearShiftCount() {

      final TreeColumnDefinition colDef = TreeColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT.createColumn(
            _columnManager,
            _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVICollatedTour) element).colRearShiftCount;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: relative break time %
    */
   private void defineColumn_Time_BreakTime_Relative() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__COMPUTED_BREAK_TIME_RELATIVE.createColumn(
            _columnManager,
            _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            /*
             * display paused time relative to the elapsed time
             */

            final Object element = cell.getElement();
            final TVICollatedTour item = (TVICollatedTour) element;

            final long dbBreakTime = item.colBreakTime;
            final long dbElapsedTime = item.colElapsedTime;

            final float relativePausedTime = dbElapsedTime == 0 ? 0 : (float) dbBreakTime
                  / dbElapsedTime
                  * 100;

            cell.setText(_nf1.format(relativePausedTime));

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: elapsed time (h)
    */
   private void defineColumn_Time_ElapsedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVICollatedTour) element).colElapsedTime;

            colDef.printLongValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: moving time (h)
    */
   private void defineColumn_Time_MovingTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__COMPUTED_MOVING_TIME.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVICollatedTour) element).colMovingTime;

            colDef.printLongValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: paused time (h)
    */
   private void defineColumn_Time_PausedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_PAUSED_TIME.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVICollatedTour item = (TVICollatedTour) element;

            final long value = item.colPausedTime;

            colDef.printLongValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: recorded time (h)
    */
   private void defineColumn_Time_RecordedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_RECORDED_TIME.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            /*
             * display paused time relative to the elapsed time
             */

            final Object element = cell.getElement();
            final TVICollatedTour item = (TVICollatedTour) element;

            final long value = item.colRecordedTime;

            colDef.printLongValue(cell, value, element instanceof TVICollatedTour_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: time
    */
   private void defineColumn_Time_TourStartTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME_TOUR_START_TIME //
            .createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTime == false) {
               return null;
            }

            return getCellTourId(cell);
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               final long tourStartTime = tviCollatedTour_Tour.colTourStartTime;

               cell.setText(TimeTools.getZonedDateTime(tourStartTime).format(TimeTools.Formatter_Date_S));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: week day
    */
   private void defineColumn_Time_WeekDay() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME_WEEK_DAY.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInWeekDay == false) {
               return null;
            }

            return getCellTourId(cell);
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               cell.setText(tviCollatedTour_Tour.colWeekDay);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: week
    */
   private void defineColumn_Time_WeekNo() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME_WEEK_NO.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int week = ((TVICollatedTour) element).colWeekNo;

            if (week == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(week));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: week year
    */
   private void defineColumn_Time_WeekYear() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME_WEEKYEAR.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int week = ((TVICollatedTour) element).colWeekYear;

            if (week == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(week));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: markers
    */
   private void defineColumn_Tour_Marker() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_NUM_MARKERS.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               final ArrayList<Long> markerIds = tviCollatedTour_Tour.getMarkerIds();
               if (markerIds == null) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(_nf0.format(markerIds.size()));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: number of photos
    */
   private void defineColumn_Tour_Photos() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_NUM_PHOTOS.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVICollatedTour) element).colNumberOfPhotos;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: tags
    */
   private void defineColumn_Tour_Tags() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTags == false) {
               return null;
            }

            return getCellTourId(cell);
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            List<Long> tagIds = null;
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               tagIds = tviCollatedTour_Tour.getTagIds();

            } else if (element instanceof final TVICollatedTour_Event tviCollatedTour_Event) {

               tagIds = tviCollatedTour_Event.getTagIds();
            }

            if (tagIds != null) {

               cell.setText(TourDatabase.getTagNames(tagIds));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: title
    */
   private void defineColumn_Tour_Title() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTitle == false) {
               return null;
            }

            return getCellTourId(cell);
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVICollatedTour_Tour //
                  || element instanceof TVICollatedTour_Event) {

               final String colTourTitle = ((TVICollatedTour) element).colTourTitle;

               cell.setText(colTourTitle);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Tour type image
    */
   private void defineColumn_Tour_TypeImage() {

      _colDef_TourTypeImage = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
      _colDef_TourTypeImage.setIsDefaultColumn();
      _colDef_TourTypeImage.setLabelProvider(new CellLabelProvider() {

         // !!! When using cell.setImage() then it is not centered !!!
         // !!! Set dummy label provider, otherwise an error occurs !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * Column: Tour type text
    */
   private void defineColumn_Tour_TypeText() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {

               final long tourTypeId = tviCollatedTour_Tour.getTourTypeId();
               cell.setText(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
            }
         }
      });
   }

   /**
    * column: clouds
    */
   private void defineColumn_Weather_Clouds() {

      _colDef_WeatherClouds = TreeColumnFactory.WEATHER_CLOUDS.createColumn(_columnManager, _pc);
      _colDef_WeatherClouds.setIsDefaultColumn();
      _colDef_WeatherClouds.setLabelProvider(new CellLabelProvider() {

         // !!! When using cell.setImage() then it is not centered !!!
         // !!! Set dummy label provider, otherwise an error occurs !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * Column: Weather - Average temperature (measured from the device)
    */
   private void defineColumn_Weather_Temperature_Avg_Device() {

      final TreeColumnDefinition colDef = TreeColumnFactory.WEATHER_TEMPERATURE_AVG_DEVICE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final float value = UI.convertTemperatureFromMetric(((TVICollatedTour) element).colAvgTemperature_Device);

            colDef.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: wind direction
    */
   private void defineColumn_Weather_WindDirection() {

      final TreeColumnDefinition colDef = TreeColumnFactory.WEATHER_WIND_DIR.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int windDir = ((TVICollatedTour) element).colWindDir;

            if (windDir == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(windDir));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: weather
    */
   private void defineColumn_Weather_WindSpeed() {

      final TreeColumnDefinition colDef = TreeColumnFactory.WEATHER_WIND_SPEED.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int windSpeed = (int) (((TVICollatedTour) element).colWindSpd
                  / UI.UNIT_VALUE_DISTANCE);

            if (windSpeed == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(windSpeed));
            }

            setCellColor(cell, element);
         }
      });
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   private void enableActions() {

      final ITreeSelection selection = (ITreeSelection) _tourViewer.getSelection();

      /*
       * Count number of selected items
       */
      int numTourItems = 0;

      TVICollatedTour firstTourItem = null;

      for (final Object treeItem : selection) {

         if (treeItem instanceof final TVICollatedTour tviCollatedTour) {

            boolean isDummyTour = false;

            // check if this is a dummy tour, the last tour is a dummy tour
            if (treeItem instanceof final TVICollatedTour_Event tviCollatedTour_Event) {

               if (tviCollatedTour_Event.isLastEvent) {
                  isDummyTour = true;
               }
            }

            if (firstTourItem == null && !isDummyTour) {
               firstTourItem = tviCollatedTour;
            }

            numTourItems++;
         }
      }

      final int selectedItems = selection.size();
      final boolean isTourSelected = numTourItems > 0;
      final boolean isOneTour = numTourItems == 1;
      boolean isDeviceTour = false;

      final TVICollatedTour firstElement = (TVICollatedTour) selection.getFirstElement();
      final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();
      TourData firstSavedTour = null;

      if (isOneTour && firstTourItem != null) {
         firstSavedTour = TourManager.getInstance().getTourData(firstTourItem.getTourId());
         isDeviceTour = firstSavedTour.isManualTour() == false;
      }

      /*
       * Enable actions
       */
// SET_FORMATTING_OFF

      _tourDoubleClickState.canEditTour            = isOneTour;
      _tourDoubleClickState.canOpenTour            = isOneTour;
      _tourDoubleClickState.canQuickEditTour       = isOneTour;
      _tourDoubleClickState.canEditMarker          = isOneTour;
      _tourDoubleClickState.canAdjustAltitude      = isOneTour;

      _actionComputeDistanceValuesFromGeoPosition  .setEnabled(isTourSelected);
      _actionComputeElevationGain                  .setEnabled(true);
      _actionEditQuick                             .setEnabled(isOneTour);
      _actionEditTour                              .setEnabled(isOneTour);
      _actionExportTour                            .setEnabled(isTourSelected);
      _actionJoinTours                             .setEnabled(numTourItems > 1);
      _actionMergeTour                             .setEnabled(isOneTour && isDeviceTour && firstSavedTour.getMergeSourceTourId() != null);
      _actionOpenAdjustAltitudeDialog              .setEnabled(isOneTour && isDeviceTour);
      _actionOpenMarkerDialog                      .setEnabled(isOneTour && isDeviceTour);
      _actionOpenTour                              .setEnabled(isOneTour);
      _actionPrintTour                             .setEnabled(isTourSelected);
      _actionReimport_Tours                        .setEnabled(isTourSelected);
      _actionSetElevationFromSRTM                  .setEnabled(isTourSelected);
      _actionSetOtherPerson                        .setEnabled(isTourSelected);

// SET_FORMATTING_ON

      _actionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);
      _actionExpandSelection.setEnabled(
            firstElement == null
                  ? false
                  : selectedItems == 1
                        ? firstElementHasChildren
                        : true);

      _tagMenuManager.enableTagActions(
            isTourSelected,
            isOneTour,
            firstTourItem == null
                  ? null
                  : firstTourItem.getTagIds());

      _tourTypeMenuManager.enableTourTypeActions(isTourSelected, TourDatabase.ENTITY_IS_NOT_SAVED);
   }

   private void fillActionBars() {

      /*
       * fill view menu
       */
//      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_contribItem_CollatedTours);

      tbm.add(new Separator());
      tbm.add(_actionExpandSelection);
      tbm.add(_actionCollapseAll);

      tbm.add(_actionRefreshView);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      // edit actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EDIT, _allTourActions_Edit, this);

      // tag actions
      _tagMenuManager.fillTagMenu_WithActiveActions(menuMgr, this);

      // tour type actions
      _tourTypeMenuManager.fillContextMenu_WithActiveActions(menuMgr, this);

      menuMgr.add(new Separator());
      menuMgr.add(_actionComputeElevationGain);
      menuMgr.add(_actionComputeDistanceValuesFromGeoPosition);
      menuMgr.add(_actionSetElevationFromSRTM);

      menuMgr.add(new Separator());
      menuMgr.add(_actionCollapseOthers);
      menuMgr.add(_actionExpandSelection);
      menuMgr.add(_actionCollapseAll);

      // export actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EXPORT, _allTourActions_Export, this);

      // adjust actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.ADJUST, _allTourActions_Adjust, this);

      // customize this context menu
      TourActionManager.fillContextMenu_CustomizeAction(menuMgr)

            // set pref page custom data that actions from this view can be identified
            .setPrefData(new ViewContext(ID, ViewNames.VIEW_NAME_TAGGED_TOURS));

      enableActions();
   }

   private Long getCellTourId(final ViewerCell cell) {

      final Object element = cell.getElement();

      if (element instanceof final TVICollatedTour_Tour tviCollatedTour_Tour) {
         return tviCollatedTour_Tour.getTourId();
      } else if (element instanceof final TVICollatedTour_Event tviCollatedTour_Event) {
         return tviCollatedTour_Event.getTourId();
      }

      return null;
   }

   TourTypeSQLData getCollatedSQL() {

      final TourTypeFilter collatedFilter = CollateTourManager.getSelectedCollateFilter();

      if (collatedFilter == null) {
         return null;
      }

      return collatedFilter.getSQLData();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   @Override
   public PostSelectionProvider getPostSelectionProvider() {
      return _postSelectionProvider;
   }

   private void getSelectedTourData(final ArrayList<TourData> selectedTourData, final Set<Long> tourIdSet) {
      for (final Long tourId : tourIdSet) {
         selectedTourData.add(TourManager.getInstance().getTourData(tourId));
      }
   }

   @Override
   public Set<Long> getSelectedTourIDs() {

      final LinkedHashSet<Long> tourIds = new LinkedHashSet<>();

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      for (final Object viewItem : selectedTours) {

         if (viewItem instanceof final TVICollatedTour tviCollatedTour) {
            tourIds.add(tviCollatedTour.getTourId());
         }
      }

      return tourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tour id's

      final Set<Long> tourIds = getSelectedTourIDs();

      /*
       * show busy indicator when multiple tours needs to be retrieved from the database
       */
      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      if (tourIds.size() > 1) {
         BusyIndicator.showWhile(Display.getCurrent(), () -> getSelectedTourData(selectedTourData, tourIds));
      } else {
         getSelectedTourData(selectedTourData, tourIds);
      }

      return selectedTourData;
   }

   /**
    * @return Returns the shell of the tree/part.
    */
   Shell getShell() {
      return _tourViewer.getTree().getShell();
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tourViewer;
   }

   @Override
   public ColumnViewer getViewer() {
      return _tourViewer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      /*
       * This ensures that the unit's are set otherwise they can be null
       */
      @SuppressWarnings("unused")
      final boolean is = UI.IS_WIN;
   }

   boolean isInUIUpdate() {
      return _isInUIUpdate;
   }

   private void onPaintViewer(final Event event) {

      // paint column image

      final int columnIndex = event.index;

      if (columnIndex == _columnIndex_TourTypeImage) {

         onPaintViewer_TourTypeImage(event);

      } else if (columnIndex == _columnIndex_WeatherClouds) {

         onPaintViewer_WeatherClouds(event);
      }
   }

   private void onPaintViewer_TourTypeImage(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof final TVICollatedTour_Tour collatedTour) {

         final long tourTypeId = collatedTour.getTourTypeId();
         final Image image = TourTypeImage.getTourTypeImage(tourTypeId);

         if (image != null) {

            UI.paintImage(

                  event,
                  image,
                  _columnWidth_TourTypeImage,
                  _colDef_TourTypeImage.getColumnStyle(), // horizontal alignment
                  TourTypeImage.getHorizontalOffset());
         }
      }
   }

   private void onPaintViewer_WeatherClouds(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof final TVICollatedTour collatedTour) {

         final String weatherClouds = collatedTour.colClouds;
         if (weatherClouds == null) {

            // paint nothing

         } else {

            final Image image = UI.IMAGE_REGISTRY.get(weatherClouds);

            if (image == null) {

               // paint text left aligned

               event.gc.drawText(weatherClouds, event.x, event.y, false);

            } else {

               final int alignment = _colDef_WeatherClouds.getColumnStyle();

               UI.paintImage(event, image, _columnWidth_WeatherClouds, alignment, 0);
            }
         }
      }
   }

   private void onResize_SetWidthForImageColumn() {

      if (_colDef_TourTypeImage != null) {

         final TreeColumn treeColumn = _colDef_TourTypeImage.getTreeColumn();

         if (treeColumn != null && treeColumn.isDisposed() == false) {

            _columnWidth_TourTypeImage = treeColumn.getWidth();
         }
      }

      if (_colDef_WeatherClouds != null) {

         final TreeColumn treeColumn = _colDef_WeatherClouds.getTreeColumn();

         if (treeColumn != null && treeColumn.isDisposed() == false) {

            _columnWidth_WeatherClouds = treeColumn.getWidth();
         }
      }
   }

   private void onSelectTreeItem(final SelectionChangedEvent event) {

      if (_isInReload) {
         return;
      }

      final HashSet<Long> tourIds = new HashSet<>();

      final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         final TVICollatedTour tourItem = (TVICollatedTour) treeItem;

         tourIds.add(tourItem.getTourId());
      }

      ISelection selection;
      if (tourIds.isEmpty()) {

         // fire selection that nothing is selected

         selection = new SelectionTourIds(new ArrayList<>());

      } else {

         // keep selected tour id's
         _selectedTourIds.clear();
         _selectedTourIds.addAll(tourIds);

         selection = tourIds.size() == 1 //
               ? new SelectionTourId(_selectedTourIds.get(0))
               : new SelectionTourIds(_selectedTourIds);

      }

      // _postSelectionProvider should be removed when all parts are listening to the TourManager event
      if (_isInStartup) {

         _isInStartup = false;

         // this view can be inactive -> selection is not fired with the SelectionProvider interface

         TourManager.fireEventWithCustomData(TourEventId.TOUR_SELECTION, selection, this);

      } else {

         _postSelectionProvider.setSelection(selection);
      }

      enableActions();
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         final Object[] expandedElements = _tourViewer.getExpandedElements();
         final ISelection selection = _tourViewer.getSelection();

         _tourViewer.getTree().dispose();

         createUI_10_TourViewer(_viewerContainer);
         _viewerContainer.layout();

         _tourViewer.setInput(_rootItem = new TVICollatedTour_Root(this));

         _tourViewer.setExpandedElements(expandedElements);
         _tourViewer.setSelection(selection);
      }
      _viewerContainer.setRedraw(true);

      return _tourViewer;
   }

   @Override
   public void reloadViewer() {

      if (_isInReload) {
         return;
      }

      final Tree tree = _tourViewer.getTree();
      tree.setRedraw(false);
      _isInReload = true;
      {
         final Object[] expandedElements = _tourViewer.getExpandedElements();
         final ISelection selection = _tourViewer.getSelection();

         _tourViewer.setInput(_rootItem = new TVICollatedTour_Root(this));

         _tourViewer.setExpandedElements(expandedElements);
         _tourViewer.setSelection(selection, true);
      }
      _isInReload = false;
      tree.setRedraw(true);
   }

   void reopenFirstSelectedTour() {

   }

   private void reselectTourViewer() {

   }

   private void restoreState() {

      updateToolTipState();
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);
   }

   private void setCellColor(final ViewerCell cell, final Object element) {

   }

   @Override
   public void setFocus() {
      _tourViewer.getControl().setFocus();
   }

   void setIsInUIUpdate(final boolean isInUpdate) {
      _isInUIUpdate = isInUpdate;
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {

   }

   private void updateToolTipState() {

      _isToolTipInCollation = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_COLLATION);
      _isToolTipInTime = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TIME);
      _isToolTipInWeekDay = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_WEEKDAY);
      _isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TITLE);
      _isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TAGS);
   }
}
