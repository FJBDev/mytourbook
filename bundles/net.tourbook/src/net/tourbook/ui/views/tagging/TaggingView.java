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
package net.tourbook.ui.views.tagging;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageTags;
import net.tourbook.preferences.ViewContext;
import net.tourbook.tag.ActionSetTagStructure;
import net.tourbook.tag.ActionSetTagStructure_All;
import net.tourbook.tag.ChangedTags;
import net.tourbook.tag.TagManager;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.ViewNames;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.part.ViewPart;

public class TaggingView extends ViewPart implements ITourProvider, ITourViewer, ITreeViewer {

   public static final String        ID                                     = "net.tourbook.views.tagViewID";           //$NON-NLS-1$

   private static final String       MEMENTO_TAG_VIEW_LAYOUT                = "tagview.layout";                         //$NON-NLS-1$

   /**
    * The expanded tag items have these structure:
    * <p>
    * 1. Type<br>
    * 2. id/year/month<br>
    * <br>
    * 3. Type<br>
    * 4. id/year/month<br>
    * ...
    */
   private static final String       STATE_EXPANDED_ITEMS                   = "STATE_EXPANDED_ITEMS";                   //$NON-NLS-1$
   private static final String       STATE_IS_ON_SELECT_EXPAND_COLLAPSE     = "STATE_IS_ON_SELECT_EXPAND_COLLAPSE";     //$NON-NLS-1$
   private static final String       STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS = "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS"; //$NON-NLS-1$
   private static final String       STATE_TAG_FILTER                       = "STATE_TAG_FILTER";                       //$NON-NLS-1$

   private static final int          STATE_ITEM_TYPE_SEPARATOR              = -1;

   private static final int          STATE_ITEM_TYPE_CATEGORY               = 1;
   private static final int          STATE_ITEM_TYPE_TAG                    = 2;
   private static final int          STATE_ITEM_TYPE_YEAR                   = 3;
   private static final int          STATE_ITEM_TYPE_MONTH                  = 4;

   static final int                  TAG_VIEW_LAYOUT_FLAT                   = 0;
   static final int                  TAG_VIEW_LAYOUT_HIERARCHICAL           = 10;

   private static final NumberFormat _nf0                                   = NumberFormat.getNumberInstance();
   private static final NumberFormat _nf1                                   = NumberFormat.getNumberInstance();

   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private final IPreferenceStore              _prefStore                               = TourbookPlugin.getPrefStore();
   private final IPreferenceStore              _prefStore_Common                        = CommonActivator.getPrefStore();

   private final IDialogSettings               _state                                   = TourbookPlugin.getState(ID);

   private int                                 _tagViewLayout                           = TAG_VIEW_LAYOUT_HIERARCHICAL;
   private TreeViewerTourInfoToolTip           _tourInfoToolTip;
   private TagFilterType                       _tagFilterType                           = TagFilterType.ALL_IS_DISPLAYED;

   private boolean                             _isToolTipInTag;
   private boolean                             _isToolTipInTitle;
   private boolean                             _isToolTipInTags;

   private boolean                             _isMouseContextMenu;
   private boolean                             _isSelectedWithKeyboard;

   private boolean                             _isBehaviour_SingleExpand_CollapseOthers = true;
   private boolean                             _isBehaviour_OnSelect_ExpandCollapse     = true;
   private boolean                             _isInCollapseAll;
   private boolean                             _isInExpandingSelection;
   private int                                 _expandRunnableCounter;
   private long                                _lastExpandSelectionTime;

   private TourDoubleClickState                _tourDoubleClickState                    = new TourDoubleClickState();

   private int                                 _numIteratedTours;

   private TreeViewer                          _tagViewer;
   private TVITaggingView_Root                 _rootItem;
   private ColumnManager                       _columnManager;
   private TreeColumnDefinition                _colDef_TagImage;
   private int                                 _columnIndex_TagImage;
   private int                                 _columnWidth_TagImage;

   private ISelectionListener                  _postSelectionListener;
   private PostSelectionProvider               _postSelectionProvider;
   private IPropertyChangeListener             _prefChangeListener;
   private IPropertyChangeListener             _prefChangeListener_Common;
   private ITourEventListener                  _tourEventListener;

   private TagMenuManager                      _tagMenuManager;
   private TourTypeMenuManager                 _tourTypeMenuManager;
   private MenuManager                         _viewerMenuManager;
   private IContextMenuProvider                _viewerContextMenuProvider               = new TreeContextMenuProvider();
   //
   private HashMap<String, Object>             _allTourActions_Edit;
   private HashMap<String, Object>             _allTourActions_Export;
   //
   private ActionOpenPrefDialog                _action_PrefDialog;
   private ActionRefreshView                   _action_RefreshView;
   private Action_TagLayout                    _action_ToggleTagLayout;
   private Action_TagFilter                    _action_ToggleTagFilter;

   private Action_CollapseAll_WithoutSelection _actionCollapseAll_WithoutSelection;
   private ActionCollapseOthers                _actionCollapseOthers;
   private Action_DeleteTag                    _actionDeleteTag;
   private Action_DeleteTagCategory            _actionDeleteTagCategory;
   private ActionEditQuick                     _actionEditQuick;
   private ActionEditTag                       _actionEditTag;
   private ActionEditTour                      _actionEditTour;
   private ActionExpandSelection               _actionExpandSelection;
   private ActionExport                        _actionExportTour;
   private Action_OnMouseSelect_ExpandCollapse _actionOnMouseSelect_ExpandCollapse;
   private ActionOpenPrefDialog                _actionOpenTagPrefs;
   private ActionOpenTour                      _actionOpenTour;
   private ActionSetTagStructure               _actionSetTagStructure;
   private ActionSetTagStructure_All           _actionSetTagStructure_All;
   private ActionSetTourTypeMenu               _actionSetTourType;
   private Action_SingleExpand_CollapseOthers  _actionSingleExpand_CollapseOthers;

   private PixelConverter                      _pc;

   private Color                               _colorContentCategory;
   private Color                               _colorContentSubCategory;
   private Color                               _colorDateCategory;
   private Color                               _colorDateSubCategory;
   private Color                               _colorTour;

   /*
    * UI resources
    */
   private final Image _imgTag         = TourbookPlugin.getImage(Images.Tag);
   private final Image _imgTagCategory = TourbookPlugin.getImage(Images.Tag_Category);
   private final Image _imgTagRoot     = TourbookPlugin.getImage(Images.Tag_Root);

   /*
    * UI controls
    */
   private Composite _viewerContainer;

   private Menu      _treeContextMenu;

   private class Action_CollapseAll_WithoutSelection extends ActionCollapseAll {

      public Action_CollapseAll_WithoutSelection() {
         super(TaggingView.this);
      }

      @Override
      public void run() {

         _isInCollapseAll = true;
         {
            super.run();
         }
         _isInCollapseAll = false;
      }
   }

   private class Action_DeleteTag extends Action {

      Action_DeleteTag() {

         super(Messages.Action_Tag_Delete, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));
      }

      @Override
      public void run() {
         onAction_DeleteTag();
      }
   }

   private class Action_DeleteTagCategory extends Action {

      Action_DeleteTagCategory() {

         super(Messages.Action_Tag_DeleteCategory, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));
      }

      @Override
      public void run() {
         onAction_DeleteTagCategory();
      }
   }

   private class Action_OnMouseSelect_ExpandCollapse extends Action {

      public Action_OnMouseSelect_ExpandCollapse() {
         super(Messages.Tour_Tags_Action_OnMouseSelect_ExpandCollapse, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         onAction_OnMouseSelect_ExpandCollapse();
      }
   }

   private class Action_SingleExpand_CollapseOthers extends Action {

      public Action_SingleExpand_CollapseOthers() {
         super(Messages.Tour_Tags_Action_SingleExpand_CollapseOthers, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         onAction_SingleExpandCollapseOthers();
      }
   }

   private class Action_TagFilter extends Action {

      Action_TagFilter() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_Tags_Action_ToggleTagFilter_Tooltip);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter));
      }

      @Override
      public void runWithEvent(final Event event) {
         onAction_ToggleTagFilter(event);
      }
   }

   private class Action_TagLayout extends Action {

      Action_TagLayout() {

         super(Messages.action_tagView_flat_layout, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Flat));
      }

      @Override
      public void run() {
         onAction_ToggleTagLayout();
      }
   }

   private class StateSegment {

      private long __itemType;
      private long __itemData;

      public StateSegment(final long itemType, final long itemData) {

         __itemType = itemType;
         __itemData = itemData;
      }
   }

   /**
    * Comparator is sorting the tree items
    */
   private final class TagComparator extends ViewerComparator {
      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         if (obj1 instanceof final TVITaggingView_Tour tourItem1 && obj2 instanceof final TVITaggingView_Tour tourItem2) {

            // sort tours by date

            return tourItem1.tourDate.compareTo(tourItem2.tourDate);

         }

         if (obj1 instanceof final TVITaggingView_Year yearItem1 && obj2 instanceof final TVITaggingView_Year yearItem2) {

            return yearItem1.compareTo(yearItem2);
         }

         if (obj1 instanceof final TVITaggingView_Month monthItem1 && obj2 instanceof final TVITaggingView_Month monthItem2) {

            return monthItem1.compareTo(monthItem2);
         }

         if (obj1 instanceof final TVITaggingView_TagCategory iItem1 && obj2 instanceof final TVITaggingView_TagCategory item2) {

            return iItem1.getTourTagCategory().getCategoryName().compareTo(item2.getTourTagCategory().getCategoryName());
         }

         return 0;
      }
   }

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    * <br>
    * A comparer is necessary to set and restore the expanded elements <br>
    * <br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    */
   private class TagComparer implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {

            return true;

         } else if (a instanceof final TVITaggingView_Year yearItem1 && b instanceof final TVITaggingView_Year yearItem2) {

            return yearItem1.getTagId() == yearItem2.getTagId() //
                  && yearItem1.getYear() == yearItem2.getYear();

         } else if (a instanceof final TVITaggingView_Month monthItemA
               && b instanceof final TVITaggingView_Month monthItemB) {

            final TVITaggingView_Year yearItemA = monthItemA.getYearItem();
            final TVITaggingView_Year yearItemB = monthItemB.getYearItem();

            return yearItemA.getTagId() == yearItemB.getTagId()
                  && yearItemA.getYear() == yearItemB.getYear()
                  && monthItemA.getMonth() == monthItemB.getMonth();

         } else if (a instanceof final TVITaggingView_TagCategory itemA
               && b instanceof final TVITaggingView_TagCategory itemB) {

            final TourTagCategory tagCategoryA = itemA.getTourTagCategory();
            final TourTagCategory tagCategoryB = itemB.getTourTagCategory();

            return tagCategoryA.getTagCategoryId() == tagCategoryB.getTagCategoryId();

         } else if (a instanceof final TVITaggingView_Tag tagItemA && b instanceof final TVITaggingView_Tag tabItemB) {

            return tagItemA.getTagId() == tabItemB.getTagId();

         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }

   }

   private class TagContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getChildren(final Object parentElement) {

         if (parentElement instanceof final TVITaggingView_Item viewItem) {
            return viewItem.getFetchedChildrenAsArray();
         }

         return new Object[0];
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return getChildren(inputElement);
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
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {

         if (newInput == null) {
            return;
         }

         setTagViewTitle(newInput);
      }
   }

   private class TagFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         return isInTagFilter(element);
      }
   }

   private enum TagFilterType {

      ALL_IS_DISPLAYED,

      /**
       * Only tags with tours are displayed
       */
      TAGS_WITH_TOURS,

      /**
       * Only tags without tours are displayed
       */
      TAGS_WITHOUT_TOURS
   }

   private class TreeContextMenuProvider implements IContextMenuProvider {

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
            reloadViewer();

         } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            // update viewer

            _tagViewer.refresh();

         } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

            updateToolTipState();

         } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            updateColors();

            _tagViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _tagViewer.refresh();

            /*
             * the tree must be redrawn because the styled text does not show with the new color
             */
            _tagViewer.getTree().redraw();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed

            _columnManager.saveState(_state);
            _columnManager.clearColumns();
            defineAllColumns();

            _tagViewer = (TreeViewer) recreateViewer(_tagViewer);
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addSelectionListener() {

      // this view part is a selection listener
      _postSelectionListener = (workbenchPart, selection) -> {

         if (selection instanceof final SelectionDeletedTours deletedTourSelection) {

            updateViewerAfterTourIsDeleted(_rootItem, deletedTourSelection.removedTours);
         }
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (workbenchPart == TaggingView.this) {
            return;
         }

         if (tourEventId == TourEventId.NOTIFY_TAG_VIEW) {

            if (eventData instanceof final ChangedTags changedTags) {

               final boolean isAddMode = changedTags.isAddMode();

               // get a clone of the modified tours/tags because the tours are removed from the list
               final ChangedTags changedTagsClone = new ChangedTags(
                     changedTags.getModifiedTags(),
                     changedTags.getModifiedTours(),
                     isAddMode);

               updateViewerAfterTagStructureIsModified(_rootItem, changedTagsClone, isAddMode);
            }

         } else if (tourEventId == TourEventId.TAG_STRUCTURE_CHANGED
               || tourEventId == TourEventId.TAG_CONTENT_CHANGED // tag image size is modified
               || tourEventId == TourEventId.UPDATE_UI) {

            reloadViewer();

         } else if (tourEventId == TourEventId.TOUR_CHANGED && eventData instanceof final TourEvent tourEvent) {

            final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

            if (modifiedTours != null) {
               updateViewerAfterTourIsModified(_rootItem, modifiedTours);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionCollapseAll_WithoutSelection  = new Action_CollapseAll_WithoutSelection();
      _actionCollapseOthers                = new ActionCollapseOthers(this);
      _actionDeleteTag                     = new Action_DeleteTag();
      _actionDeleteTagCategory             = new Action_DeleteTagCategory();
      _actionEditQuick                     = new ActionEditQuick(this);
      _actionEditTag                       = new ActionEditTag(this);
      _actionEditTour                      = new ActionEditTour(this);
      _actionExpandSelection               = new ActionExpandSelection(this, true);
      _actionExportTour                    = new ActionExport(this);
      _actionOnMouseSelect_ExpandCollapse  = new Action_OnMouseSelect_ExpandCollapse();
      _actionOpenTagPrefs                  = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
      _actionOpenTour                      = new ActionOpenTour(this);
      _actionSetTagStructure               = new ActionSetTagStructure(this);
      _actionSetTagStructure_All           = new ActionSetTagStructure_All();
      _actionSetTourType                   = new ActionSetTourTypeMenu(this);
      _actionSingleExpand_CollapseOthers   = new Action_SingleExpand_CollapseOthers();

      _allTourActions_Edit    = new HashMap<>();
      _allTourActions_Export  = new HashMap<>();

      _allTourActions_Edit.put(_actionEditQuick                   .getClass().getName(),  _actionEditQuick);
      _allTourActions_Edit.put(_actionEditTour                    .getClass().getName(),  _actionEditTour);
//    _allTourActions_Edit.put(_actionOpenMarkerDialog            .getClass().getName(),  _actionOpenMarkerDialog);
//    _allTourActions_Edit.put(_actionOpenAdjustAltitudeDialog    .getClass().getName(),  _actionOpenAdjustAltitudeDialog);
//    _allTourActions_Edit.put(_actionSetStartEndLocation         .getClass().getName(),  _actionSetStartEndLocation);
      _allTourActions_Edit.put(_actionOpenTour                    .getClass().getName(),  _actionOpenTour);
//    _allTourActions_Edit.put(_actionDuplicateTour               .getClass().getName(),  _actionDuplicateTour);
//    _allTourActions_Edit.put(_actionCreateTourMarkers           .getClass().getName(),  _actionCreateTourMarkers);
//    _allTourActions_Edit.put(_actionMergeTour                   .getClass().getName(),  _actionMergeTour);
//    _allTourActions_Edit.put(_actionJoinTours                   .getClass().getName(),  _actionJoinTours);

//    menuMgr.add(_actionEditQuick);
//    menuMgr.add(_actionEditTour);
//    menuMgr.add(_actionOpenTour);

//    _allTourActions_Export.put(_actionUploadTour                .getClass().getName(),  _actionUploadTour);
      _allTourActions_Export.put(_actionExportTour                .getClass().getName(),  _actionExportTour);
//    _allTourActions_Export.put(_actionExportViewCSV             .getClass().getName(),  _actionExportViewCSV);
//    _allTourActions_Export.put(_actionPrintTour                 .getClass().getName(),  _actionPrintTour);
//
//    menuMgr.add(_actionExportTour);

//    _allTourActions_Adjust.put(_actionAdjustTourValues          .getClass().getName(),  _actionAdjustTourValues);
//    _allTourActions_Adjust.put(_actionDeleteTourValues          .getClass().getName(),  _actionDeleteTourValues);
//    _allTourActions_Adjust.put(_actionReimport_Tours            .getClass().getName(),  _actionReimport_Tours);
//    _allTourActions_Adjust.put(_actionSetOtherPerson            .getClass().getName(),  _actionSetOtherPerson);
//    _allTourActions_Adjust.put(_actionDeleteTourMenu            .getClass().getName(),  _actionDeleteTourMenu);

// SET_FORMATTING_ON

      TourActionManager.setAllViewActions(ID,
            _allTourActions_Edit.keySet(),
            _allTourActions_Export.keySet(),
            _tagMenuManager.getAllTagActions().keySet(),
            _tourTypeMenuManager.getAllTourTypeActions().keySet());

      _action_RefreshView = new ActionRefreshView(this);
      _action_ToggleTagFilter = new Action_TagFilter();
      _action_ToggleTagLayout = new Action_TagLayout();

      _action_PrefDialog = new ActionOpenPrefDialog(
            Messages.Tour_Tags_Action_Preferences_Tooltip,
            PrefPageTags.ID);

      _action_PrefDialog.setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions));
   }

   private void createMenuManager() {

      _tagMenuManager = new TagMenuManager(this, true);
      _tourTypeMenuManager = new TourTypeMenuManager(this);

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(menuManager -> {

         _tourInfoToolTip.hideToolTip();

         fillContextMenu(menuManager);
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      _pc = new PixelConverter(parent);
      createMenuManager();

      // define all columns
      _columnManager = new ColumnManager(this, _state);
      _columnManager.setIsCategoryAvailable(true);
      defineAllColumns();

      createActions();
      fillViewMenu();

      // viewer must be created after the action are created
      createUI(parent);

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      addTourEventListener();
      addPrefListener();
      addSelectionListener();
      restoreState();

      updateColors();
      reloadViewer();

      restoreState_Viewer();

      enableActions(false);
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_TagViewer(_viewerContainer);
      }
   }

   private void createUI_10_TagViewer(final Composite parent) {

      /*
       * Create tree
       */
      final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      /*
       * Create viewer
       */
      _tagViewer = new TreeViewer(tree);
      _columnManager.createColumns(_tagViewer);

      _tagViewer.setContentProvider(new TagContentProvider());
      _tagViewer.setComparer(new TagComparer());
      _tagViewer.setComparator(new TagComparator());
      _tagViewer.setFilters(new TagFilter());

      _tagViewer.setUseHashlookup(true);

      _tagViewer.addSelectionChangedListener(selectionChangedEvent -> onTagViewer_Selection(selectionChangedEvent));
      _tagViewer.addDoubleClickListener(doubleClickEvent -> onTagViewer_DoubleClick());

      tree.addListener(SWT.MouseDoubleClick, event -> onTagTree_DoubleClick(event));
      tree.addListener(SWT.MouseDown, event -> onTagTree_MouseDown(event));

      tree.addKeyListener(keyPressedAdapter(keyEvent -> {

         _isSelectedWithKeyboard = true;

         enableActions(true);

         switch (keyEvent.keyCode) {

         case SWT.DEL:

            // delete tag only when the delete button is enabled
            if (_actionDeleteTag.isEnabled()) {

               onAction_DeleteTag();

            } else if (_actionDeleteTagCategory.isEnabled()) {

               onAction_DeleteTagCategory();
            }

            break;

         case SWT.F2:
            onTagViewer_RenameTag();
            break;
         }
      }));

      /*
       * the context menu must be created AFTER the viewer is created which is also done after the
       * measurement system has changed, if not, the context menu is not displayed because it
       * belongs to the old viewer
       */
      createUI_20_ContextMenu();
      createUI_30_ColumnImages(tree);

      fillToolBar();

      // set tour info tooltip provider
      _tourInfoToolTip = new TreeViewerTourInfoToolTip(_tagViewer);
      _tourInfoToolTip.setTooltipUIProvider(new TaggingView_TooltipUIProvider(this));
   }

   /**
    * Setup the viewer context menu
    */
   private void createUI_20_ContextMenu() {

      _treeContextMenu = createUI_22_CreateViewerContextMenu();

      final Tree tree = (Tree) _tagViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
   }

   /**
    * Create the viewer context menu
    *
    * @return
    */
   private Menu createUI_22_CreateViewerContextMenu() {

      final Tree tree = (Tree) _tagViewer.getControl();

      // add the context menu to the tree viewer

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

   private void createUI_30_ColumnImages(final Tree tree) {

      // update column index which is needed for repainting
      final ColumnProfile activeProfile = _columnManager.getActiveProfile();
      _columnIndex_TagImage = activeProfile.getColumnIndex(_colDef_TagImage.getColumnId());

      final int numColumns = tree.getColumns().length;

      // add listeners
      if (_columnIndex_TagImage >= 0 && _columnIndex_TagImage < numColumns) {

         // column is visible

         final ControlListener controlResizedAdapter = ControlListener.controlResizedAdapter(controlEvent -> onResize_SetWidthForImageColumn());

         tree.getColumn(_columnIndex_TagImage).addControlListener(controlResizedAdapter);
         tree.addControlListener(controlResizedAdapter);

         /*
          * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
          * critical for performance that these methods be as efficient as possible.
          */
         tree.addListener(SWT.PaintItem, event -> onPaintViewer(event));
      }
   }

   /**
    * Defines all columns for the table viewer in the column manager
    */
   private void defineAllColumns() {

      defineColumn_1stColumn();

      defineColumn_Time_ElapsedTime();
      defineColumn_Time_MovingTime();
      defineColumn_Time_PausedTime();

      defineColumn_Tour_Title();
      defineColumn_Tour_Tags();
      defineColumn_Tour_TagAndCategoryNotes();
      defineColumn_Tour_TagID();
      defineColumn_Tour_TagImage();
      defineColumn_Tour_TagImageFilePath();

      defineColumn_Motion_Distance();
      defineColumn_Motion_MaxSpeed();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_AvgPace();

      defineColumn_Altitude_Up();
      defineColumn_Altitude_Down();
      defineColumn_Altitude_Max();

      defineColumn_Body_MaxPulse();
      defineColumn_Body_AvgPulse();

      defineColumn_Weather_Temperature_Avg_Device();

      defineColumn_Powertrain_AvgCadence();
   }

   /**
    * tree column: category/tag/year/month/tour
    */
   private void defineColumn_1stColumn() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAG_AND_CATEGORY.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Object getData(final ViewerCell cell) {

            if (_isToolTipInTag == false) {
               return null;
            }

            final TVITaggingView_Item viewItem = (TVITaggingView_Item) cell.getElement();

            if (viewItem instanceof TVITaggingView_Tag || viewItem instanceof TVITaggingView_TagCategory) {

               // return tag/category to show it's notes fields in the tooltip

               return viewItem;
            }

            return null;
         }

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTag == false) {
               return null;
            }

            final Object element = cell.getElement();
            final TVITaggingView_Item viewItem = (TVITaggingView_Item) element;

            if (viewItem instanceof final TVITaggingView_Tour tourItem) {
               return tourItem.tourId;
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVITaggingView_Item viewItem = (TVITaggingView_Item) element;

            long numTours = viewItem.numTours;

            // hide number of tours
            if (_tagFilterType == TagFilterType.TAGS_WITHOUT_TOURS) {
               numTours = 0;
            }

            final StyledString styledString = new StyledString();

            if (viewItem instanceof final TVITaggingView_Tour tourItem) {

               /*
                * Tour
                */

               styledString.append(viewItem.firstColumn);

               cell.setImage(TourTypeImage.getTourTypeImage(tourItem.tourTypeId));
               setCellColor(cell, element);

            } else if (viewItem instanceof final TVITaggingView_Tag tagItem) {

               /*
                * Tag
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.CONTENT_SUB_CATEGORY_STYLER);

               if (numTours > 0) {
                  styledString.append(UI.SPACE3 + numTours, net.tourbook.ui.UI.TOTAL_STYLER);
               }

               cell.setImage(tagItem.getTourTag().isRoot() ? _imgTagRoot : _imgTag);

            } else if (viewItem instanceof final TVITaggingView_TagCategory categoryItem) {

               /*
                * Tag category
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.CONTENT_CATEGORY_STYLER);

               // get number of tags/categories
               int numTags = categoryItem.numTags;
               int numCategories = categoryItem.numTagCategories;

               /**
                * Hide number of tags & categories, it's toooo complicated to compute it, an
                * alternative could be to filter tags with sql.
                */
               if (_tagFilterType == TagFilterType.TAGS_WITHOUT_TOURS
                     || _tagFilterType == TagFilterType.TAGS_WITH_TOURS) {

                  numTags = 0;
                  numCategories = 0;
               }

               if (numCategories > 0) {
                  styledString.append(UI.SPACE3 + numCategories, net.tourbook.ui.UI.TOUR_STYLER);
               }

               if (numTags > 0) {
                  styledString.append(UI.SPACE3 + numTags, net.tourbook.ui.UI.CONTENT_SUB_CATEGORY_STYLER);
               }

               if (numTours > 0) {

                  final String numToursText = (numTours > 0

                        ? UI.SPACE3
                        : UI.EMPTY_STRING)

                        + numTours;

                  styledString.append(numToursText, net.tourbook.ui.UI.TOTAL_STYLER);
               }

               cell.setImage(_imgTagCategory);

            } else if (viewItem instanceof TVITaggingView_Year
                  || viewItem instanceof TVITaggingView_Month) {

               /*
                * Year or month
                */

               styledString.append(viewItem.firstColumn);

               if (numTours > 0) {
                  styledString.append(UI.SPACE3 + numTours, net.tourbook.ui.UI.TOTAL_STYLER);
               }

               if (viewItem instanceof TVITaggingView_Month) {
                  cell.setForeground(_colorDateSubCategory);
               } else {
                  cell.setForeground(_colorDateCategory);
               }

            } else {

               styledString.append(viewItem.firstColumn);
            }

            cell.setText(styledString.getString());
            cell.setStyleRanges(styledString.getStyleRanges());
         }
      });
   }

   /**
    * Column: Elevation loss (m)
    */
   private void defineColumn_Altitude_Down() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final double dbAltitudeDown = ((TVITaggingView_Item) element).colAltitudeDown;
            final double value = -dbAltitudeDown / UI.UNIT_VALUE_ELEVATION;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Max elevation
    */
   private void defineColumn_Altitude_Max() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_MAX.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final long dbMaxAltitude = ((TVITaggingView_Item) element).colMaxAltitude;
            final double value = dbMaxAltitude / UI.UNIT_VALUE_ELEVATION;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation gain (m)
    */
   private void defineColumn_Altitude_Up() {

      final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final long dbAltitudeUp = ((TVITaggingView_Item) element).colAltitudeUp;
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

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final double value = ((TVITaggingView_Item) element).colAvgPulse;

            colDef.printDoubleValue(cell, value, element instanceof TVITaggingView_Tour);

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

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final long value = ((TVITaggingView_Item) element).colMaxPulse;

            colDef.printValue_0(cell, value);

            setCellColor(cell, element);
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
            final float pace = ((TVITaggingView_Item) element).colAvgPace * UI.UNIT_VALUE_DISTANCE;

            if (pace == 0.0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(net.tourbook.common.UI.format_mm_ss((long) pace));
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
            final float value = ((TVITaggingView_Item) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;

            colDef.printDoubleValue(cell, value, element instanceof TVITaggingView_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: distance (km/miles)
    */
   private void defineColumn_Motion_Distance() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final double value = ((TVITaggingView_Item) element).colDistance / 1000.0 / UI.UNIT_VALUE_DISTANCE;

            colDef.printDoubleValue(cell, value, element instanceof TVITaggingView_Tour);

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

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final double value = ((TVITaggingView_Item) element).colMaxSpeed / UI.UNIT_VALUE_DISTANCE;

            colDef.printDoubleValue(cell, value, element instanceof TVITaggingView_Tour);

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

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final float value = ((TVITaggingView_Item) element).colAvgCadence;

            colDef.printDoubleValue(cell, value, element instanceof TVITaggingView_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: elapsed time (h)
    */
   private void defineColumn_Time_ElapsedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final long value = ((TVITaggingView_Item) element).colElapsedTime;

            colDef.printLongValue(cell, value, element instanceof TVITaggingView_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: moving time (h)
    */
   private void defineColumn_Time_MovingTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__COMPUTED_MOVING_TIME.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final long value = ((TVITaggingView_Item) element).colMovingTime;

            colDef.printLongValue(cell, value, element instanceof TVITaggingView_Tour);

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

            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final long value = ((TVITaggingView_Item) element).colPausedTime;

            colDef.printLongValue(cell, value, element instanceof TVITaggingView_Tour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Tag/category notes
    */
   private void defineColumn_Tour_TagAndCategoryNotes() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAG_AND_CATEGORY_NOTES.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVITaggingView_Tag tagItem) {

               cell.setText(TourDatabase.getTagPropertyValue((tagItem).getTagId(), TreeColumnFactory.TOUR_TAG_AND_CATEGORY_NOTES_ID));
               setCellColor(cell, element);

            } else if (element instanceof final TVITaggingView_TagCategory categoryItem) {

               cell.setText(TourDatabase.getTagCategoryNotes(categoryItem.getTourTagCategory().getCategoryId()));
               setCellColor(cell, element);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Tag ID
    */
   private void defineColumn_Tour_TagID() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAG_ID.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVITaggingView_Tag tagItem) {

               final long tagId = tagItem.getTagId();

               cell.setText(Long.toString(tagId));
               setCellColor(cell, element);

            } else if (element instanceof final TVITaggingView_TagCategory categoryItem) {

               final long categoryId = categoryItem.getTourTagCategory().getCategoryId();

               cell.setText(Long.toString(categoryId));
               setCellColor(cell, element);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Image
    */
   private void defineColumn_Tour_TagImage() {

      _colDef_TagImage = TreeColumnFactory.TOUR_TAG_IMAGE.createColumn(_columnManager, _pc);

      _colDef_TagImage.setLabelProvider(new CellLabelProvider() {

         // !!! set dummy label provider, otherwise an error occurs !!!
         // the image is painted in onPaintViewer()

         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   private void defineColumn_Tour_TagImageFilePath() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAG_IMAGE_FILE_PATH.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVITaggingView_Tag tagItem) {

               cell.setText(TourDatabase.getTagPropertyValue(
                     (tagItem).getTagId(),
                     TreeColumnFactory.TOUR_TAG_IMAGE_FILE_PATH_ID));

               setCellColor(cell, element);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
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

            final Object element = cell.getElement();

            if (element instanceof final TVITaggingView_Tour tourItem) {
               return tourItem.tourId;
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVITaggingView_Tour tourItem) {

               String tagNames = TourDatabase.getTagNames(tourItem.tagIds);

               if (net.tourbook.common.UI.IS_SCRAMBLE_DATA) {
                  tagNames = net.tourbook.common.UI.scrambleText(tagNames);
               }

               cell.setText(tagNames);
               setCellColor(cell, element);

            } else {
               cell.setText(UI.EMPTY_STRING);
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

            final Object element = cell.getElement();

            if (element instanceof final TVITaggingView_Tour tourItem) {
               return tourItem.tourId;
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVITaggingView_Tour tourItem) {
               cell.setText(tourItem.tourTitle);
               setCellColor(cell, element);
            } else {
               cell.setText(UI.EMPTY_STRING);
            }
         }
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
            if (element instanceof TVITaggingView_TagCategory) {
               return;
            }

            final double temperature = net.tourbook.common.UI.convertTemperatureFromMetric(
                  ((TVITaggingView_Item) element).colAvgTemperature_Device);

            colDef.printDoubleValue(cell, temperature, element instanceof TVITaggingView_Tour);

            setCellColor(cell, element);
         }
      });
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();

      super.dispose();
   }

   void editTag(final Object viewerCellData) {

      _actionEditTag.editTag(viewerCellData);
   }

   private void enableActions(final boolean isIterateTours) {

      final StructuredSelection selection = (StructuredSelection) _tagViewer.getSelection();
      final int numTreeItems = _tagViewer.getTree().getItemCount();

      // this can be very cpu intensive -> avoid when not necessary
      boolean isIteratedTours = false;
      if (isIterateTours) {

         final ArrayList<Long> allSelectedTourIds = getSelectedTourIDs();
         _numIteratedTours = allSelectedTourIds.size();
         isIteratedTours = _numIteratedTours > 0;
      }

      /*
       * Count number of selected tours/tags/categories
       */
      int numTours = 0;
      int numTags = 0;
      int numCategorys = 0;
      int numItems = 0;
      int numOtherItems = 0;

      TVITaggingView_Tour firstTour = null;

      for (final Object treeItem : selection) {

         if (treeItem instanceof final TVITaggingView_Tour tourItem) {
            if (numTours == 0) {
               firstTour = tourItem;
            }
            numTours++;
         } else if (treeItem instanceof TVITaggingView_Tag) {
            numTags++;
         } else if (treeItem instanceof TVITaggingView_TagCategory) {
            numCategorys++;
         } else {
            numOtherItems++;
         }
         numItems++;
      }

      final boolean isTourSelected = numTours > 0;
      final boolean isTagSelected = numTags > 0 && numTours == 0 && numCategorys == 0 && numOtherItems == 0;
      final boolean isCategorySelected = numCategorys == 1 && numTours == 0 && numTags == 0 && numOtherItems == 0;
      final boolean isOneTour = numTours == 1;
      final boolean isItemsAvailable = numTreeItems > 0;

      final int selectedItems = selection.size();
      final TVITaggingView_Item firstElement = (TVITaggingView_Item) selection.getFirstElement();
      final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();

      _tourDoubleClickState.canEditTour = isOneTour;
      _tourDoubleClickState.canOpenTour = isOneTour;
      _tourDoubleClickState.canQuickEditTour = isOneTour;
      _tourDoubleClickState.canEditMarker = isOneTour;
      _tourDoubleClickState.canAdjustAltitude = isOneTour;

      _actionEditTour.setEnabled(isOneTour);
      _actionOpenTour.setEnabled(isOneTour);
      _actionEditQuick.setEnabled(isOneTour);

      // action: set tour type
      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
      _actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

      // enable rename action
      if (selectedItems == 1) {

         if (isTagSelected) {

            _actionEditTag.setText(Messages.Action_Tag_Edit);
            _actionEditTag.setEnabled(true);

         } else if (isCategorySelected) {

            _actionEditTag.setText(Messages.Action_TagCategory_Edit);
            _actionEditTag.setEnabled(true);

         } else {
            _actionEditTag.setEnabled(false);
         }

      } else {
         _actionEditTag.setEnabled(false);
      }

      /*
       * tree expand type can be set if only tags are selected or when an item is selected which is
       * not a category
       */
      _actionSetTagStructure.setEnabled(isTagSelected || (numItems == 1 && numCategorys == 0));
      _actionSetTagStructure_All.setEnabled(isItemsAvailable);
      _actionDeleteTag.setEnabled(isTagSelected);
      _actionDeleteTagCategory.setEnabled(isCategorySelected);

//      _actionContext_ExpandSelection.setEnabled(firstElement == null
//            ? false
//            : selectedItems == 1
//                  ? firstElementHasChildren
//                  : true);
      _actionExpandSelection.setEnabled(true);

      _actionExportTour.setEnabled(isIteratedTours);

      _actionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);
      _actionCollapseAll_WithoutSelection.setEnabled(isItemsAvailable);

      _tagMenuManager.enableTagActions(isTourSelected, isOneTour, firstTour == null ? null : firstTour.tagIds);

      _tourTypeMenuManager.enableTourTypeActions(isTourSelected,
            isOneTour
                  ? firstTour.tourTypeId
                  : TourDatabase.ENTITY_IS_NOT_SAVED);
   }

   private void expandCollapseItem(final TreeViewerItem treeItem) {

      if (_tagViewer.getExpandedState(treeItem)) {
         _tagViewer.collapseToLevel(treeItem, 1);
      } else {
         _tagViewer.expandToLevel(treeItem, 1);
      }
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionCollapseOthers);
      menuMgr.add(_actionExpandSelection);
      menuMgr.add(_actionCollapseAll_WithoutSelection);
      menuMgr.add(_actionOnMouseSelect_ExpandCollapse);
      menuMgr.add(_actionSingleExpand_CollapseOthers);

      // edit actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EDIT, _allTourActions_Edit, this);

      // add/remove ... tags in the tours
      _tagMenuManager.fillTagMenu_WithActiveActions(menuMgr, this);

      // tour type actions
      _tourTypeMenuManager.fillContextMenu_WithActiveActions(menuMgr, this);

      // export actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EXPORT, _allTourActions_Export, this);

      // customize tags in the view
      menuMgr.add(new Separator());
      menuMgr.add(_actionEditTag);
      menuMgr.add(_actionSetTagStructure);
      menuMgr.add(_actionSetTagStructure_All);
      menuMgr.add(_actionOpenTagPrefs);
      menuMgr.add(_actionDeleteTag);
      menuMgr.add(_actionDeleteTagCategory);

      // customize context menu
      TourActionManager.fillContextMenu_CustomizeAction(menuMgr)

            // set pref page custom data that actions from this view can be identified
            .setPrefData(new ViewContext(ID, ViewNames.VIEW_NAME_TAGGED_TOURS));

      enableActions(true);

      // set AFTER the actions are enabled this retrieves the number of tours
      _actionExportTour.setNumberOfTours(_numIteratedTours);
   }

   private void fillToolBar() {
      /*
       * action in the view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      // recreate the toolbar
      tbm.removeAll();

      tbm.add(_action_ToggleTagFilter);
      tbm.add(_actionExpandSelection);
      tbm.add(_actionCollapseAll_WithoutSelection);
      tbm.add(_action_ToggleTagLayout);

      tbm.add(_action_RefreshView);
      tbm.add(_action_PrefDialog);

      tbm.update(true);
   }

   private void fillViewMenu() {

      /*
       * fill view menu
       */
//      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   private ArrayList<Long> getSelectedTourIDs() {

      final ArrayList<Long> allTourIds = new ArrayList<>();
      final Set<Long> checkedTourIds = new HashSet<>();

      final Object[] selection = ((IStructuredSelection) _tagViewer.getSelection()).toArray();

      for (final Object selectedItem : selection) {

         if (selectedItem instanceof final TVITaggingView_Tour tourItem) {

            final long tourId = tourItem.tourId;

            if (checkedTourIds.add(tourId)) {
               allTourIds.add(tourId);
            }

         } else if (selectedItem instanceof final TVITaggingView_Item viewItem) {

            getTagChildren(viewItem, allTourIds, checkedTourIds);
         }
      }

      return allTourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tour id's
      final ArrayList<Long> tourIds = getSelectedTourIDs();

      /*
       * Show busyindicator when multiple tours needs to be retrieved from the database
       */
      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      if (tourIds.size() > 1) {

         BusyIndicator.showWhile(Display.getCurrent(), () -> TourManager.getInstance().getTourData(selectedTourData, tourIds));

      } else {

         TourManager.getInstance().getTourData(selectedTourData, tourIds);
      }

      return selectedTourData;
   }

   /**
    * Recursive !!!
    * <p>
    * Fetch children of a tag item and collect tour id's.
    *
    * @param tagItem
    * @param allTourIds
    * @param checkedTourIds
    */
   private void getTagChildren(final TVITaggingView_Item tagItem, final ArrayList<Long> allTourIds, final Set<Long> checkedTourIds) {

      // iterate over all tag children

      for (final TreeViewerItem viewerItem : tagItem.getFetchedChildren()) {

         if (viewerItem instanceof final TVITaggingView_Tour tourItem) {

            final long tourId = tourItem.tourId;

            if (checkedTourIds.add(tourId)) {
               allTourIds.add(tourId);
            }

         } else if (viewerItem instanceof final TVITaggingView_Item viewItem) {

            getTagChildren(viewItem, allTourIds, checkedTourIds);
         }
      }
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tagViewer;
   }

   @Override
   public ColumnViewer getViewer() {
      return _tagViewer;
   }

   /**
    * @param item
    *
    * @return Returns <code>true</code> when the item is visible in the current tag filter
    */
   private boolean isInTagFilter(final Object item) {

      if (_tagFilterType == TagFilterType.ALL_IS_DISPLAYED) {

         // nothing is filtered

         return true;
      }

      // tags are filtered

      if (false
            || item instanceof TVITaggingView_TagCategory
            || item instanceof TVITaggingView_Tag
            || item instanceof TVITaggingView_Year
            || item instanceof TVITaggingView_Month) {

         final boolean hasTour = ((TVITaggingView_Item) item).numTours > 0;
         final boolean hasTagsNoTours = ((TVITaggingView_Item) item).numTags_NoTours > 0;

         if (_tagFilterType == TagFilterType.TAGS_WITH_TOURS && hasTour) {

            // show tags WITH tours

            return true;

         } else if (_tagFilterType == TagFilterType.TAGS_WITHOUT_TOURS && hasTagsNoTours) {

            // show tags WITHOUT tours

            return true;

         } else {

            return false;
         }
      }

      // all other items are not filtered

      return true;
   }

   /**
    * Load all tree items that category items do show the number of items
    */
   private void loadAllTreeItems() {

      // get all tag viewer items

      final List<TreeViewerItem> allRootItems = _rootItem.getFetchedChildren();

      for (final TreeViewerItem rootItem : allRootItems) {

         // Is recursive !!!
         loadAllTreeItems(rootItem);
      }
   }

   /**
    * !!! RECURSIVE !!!
    * <p>
    * Traverses all tag viewer items
    *
    * @param parentItem
    */
   private void loadAllTreeItems(final TreeViewerItem parentItem) {

      final ArrayList<TreeViewerItem> allFetchedChildren = parentItem.getFetchedChildren();

      for (final TreeViewerItem childItem : allFetchedChildren) {

         // skip tour items, they do not have further children
         if (childItem instanceof TVITaggingView_Tour) {
            continue;
         }

         loadAllTreeItems(childItem);
      }

      /*
       * Collect number of ...
       */
      int numAllTagCategories = 0;
      int numAllTags = 0;

      int numTags_NoTours = 0;

      int numTours_InTourItems = 0;
      int numTours_InTagSubCats = 0;

      for (final TreeViewerItem childItem : allFetchedChildren) {

         if (childItem instanceof TVITaggingView_Tour) {

            numTours_InTourItems++;

         } else if (childItem instanceof TVITaggingView_Year
               || childItem instanceof TVITaggingView_Month) {

            // collect number of tours in the tag sub categories

            numTours_InTagSubCats += ((TVITaggingView_Item) childItem).numTours;

         } else if (childItem instanceof TVITaggingView_TagCategory) {

            numAllTagCategories++;

         } else if (childItem instanceof TVITaggingView_Tag) {

            numAllTags++;

         }
      }

      if (numTours_InTourItems == 0 && numTours_InTagSubCats == 0) {

         numTags_NoTours++;
      }

// SET_FORMATTING_OFF

      /*
       * Update number of tours in parent item and up to the tag item
       */
      if (parentItem instanceof final TVITaggingView_Tag tagItem) {

         tagItem.numTours           += numTours_InTourItems;
         tagItem.numTags_NoTours    += numTags_NoTours;

      } else if (parentItem instanceof final TVITaggingView_Year yearItem) {

         yearItem.numTours          += numTours_InTourItems;
         yearItem.numTags_NoTours   += numTags_NoTours;

         final TreeViewerItem yearParent = yearItem.getParentItem();
         if (yearParent instanceof final TVITaggingView_Tag tagItem) {

            tagItem.numTours           += numTours_InTourItems;
            tagItem.numTags_NoTours    += numTags_NoTours;
         }

      } else if (parentItem instanceof final TVITaggingView_Month monthItem) {

         monthItem.numTours            += numTours_InTourItems;
         monthItem.numTags_NoTours     += numTags_NoTours;

         final TreeViewerItem monthParent = monthItem.getParentItem();
         if (monthParent instanceof final TVITaggingView_Year yearItem) {

            yearItem.numTours          += numTours_InTourItems;
            yearItem.numTags_NoTours   += numTags_NoTours;

            final TreeViewerItem yearParent = yearItem.getParentItem();
            if (yearParent instanceof final TVITaggingView_Tag tagItem) {

               tagItem.numTours           += numTours_InTourItems;
               tagItem.numTags_NoTours    += numTags_NoTours;
            }
         }

      } else if (parentItem instanceof final TVITaggingView_TagCategory categoryItem) {

         long allNumChild_Tours           = 0;
         long allNumChild_TagsNoTours     = 0;

         for (final TreeViewerItem treeViewerItem : allFetchedChildren) {

            if (treeViewerItem instanceof final TVITaggingView_Item viewItem) {

               allNumChild_Tours          += viewItem.numTours;
               allNumChild_TagsNoTours    += viewItem.numTags_NoTours;
            }
         }

         categoryItem.numTagCategories    += numAllTagCategories;
         categoryItem.numTags             += numAllTags;

         categoryItem.numTours            += allNumChild_Tours;
         categoryItem.numTags_NoTours     += allNumChild_TagsNoTours;
      }

// SET_FORMATTING_ON
   }

   private void onAction_DeleteTag() {

      final ITreeSelection structuredSelection = _tagViewer.getStructuredSelection();
      final List<?> allSelection = structuredSelection.toList();

      final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

      final ArrayList<TourTag> allSelectedTags = new ArrayList<>();
      for (final Object object : allSelection) {

         if (object instanceof final TVITaggingView_Tag tviTag) {

            allSelectedTags.add(allTourTags.get(tviTag.getTagId()));
         }
      }

      if (allSelectedTags.size() > 0) {

         // delete tags

         TagManager.deleteTourTag(allSelectedTags);
      }
   }

   private void onAction_DeleteTagCategory() {

      final ITreeSelection structuredSelection = _tagViewer.getStructuredSelection();
      final List<?> allSelection = structuredSelection.toList();

      for (final Object object : allSelection) {

         if (object instanceof final TVITaggingView_TagCategory categoryItem) {

            final TourTagCategory tagCategory = categoryItem.getTourTagCategory();

            TagManager.deleteTourTagCategory(tagCategory.getCategoryId(), tagCategory.getCategoryName());

            // currently only one empty tag category can be deleted -> other cases need more time

            return;
         }
      }
   }

   private void onAction_OnMouseSelect_ExpandCollapse() {

      _isBehaviour_OnSelect_ExpandCollapse = _actionOnMouseSelect_ExpandCollapse.isChecked();
   }

   private void onAction_SingleExpandCollapseOthers() {

      _isBehaviour_SingleExpand_CollapseOthers = _actionSingleExpand_CollapseOthers.isChecked();
   }

   private void onAction_ToggleTagFilter(final Event event) {

      final boolean isForwards = UI.isCtrlKey(event) == false;

      if (_tagFilterType == TagFilterType.ALL_IS_DISPLAYED) {

         if (isForwards) {
            setTagFilter_WithTours();
         } else {
            setTagFilter_NoTours();
         }

      } else if (_tagFilterType == TagFilterType.TAGS_WITH_TOURS) {

         if (isForwards) {
            setTagFilter_NoTours();
         } else {
            setTagFilter_ShowAll();
         }

      } else {

         if (isForwards) {
            setTagFilter_ShowAll();
         } else {
            setTagFilter_WithTours();
         }
      }

      final Tree tree = _tagViewer.getTree();
      tree.setRedraw(false);
      {
         _tagViewer.refresh();
      }
      tree.setRedraw(true);

   }

   private void onAction_ToggleTagLayout() {

      switch (_tagViewLayout) {

      case TAG_VIEW_LAYOUT_FLAT:

         _tagViewLayout = TAG_VIEW_LAYOUT_HIERARCHICAL;
         break;

      case TAG_VIEW_LAYOUT_HIERARCHICAL:

         _tagViewLayout = TAG_VIEW_LAYOUT_FLAT;
         break;
      }

      updateUI_TagLayoutAction();

      reloadViewer();
   }

   private void onPaintViewer(final Event event) {

      // skip other columns
      if (event.index != _columnIndex_TagImage) {
         return;
      }

      final TreeItem item = (TreeItem) event.item;
      final Object itemData = item.getData();

      // skip other tree items
      if (itemData instanceof final TVITaggingView_Tag prefTag) {

         /*
          * Paint tag image
          */

         final TourTag tourTag = prefTag.getTourTag();
         final Image tagImage = TagManager.getTagImage(tourTag);

         if (tagImage != null && tagImage.isDisposed() == false) {

            UI.paintImage(

                  event,
                  tagImage,
                  _columnWidth_TagImage,

                  _colDef_TagImage.getColumnStyle(), //  horizontal alignment
                  SWT.CENTER, //                         vertical alignment

                  0 //                                   horizontal offset
            );
         }
      }
   }

   private void onResize_SetWidthForImageColumn() {

      if (_colDef_TagImage != null) {

         final TreeColumn treeColumn = _colDef_TagImage.getTreeColumn();

         if (treeColumn != null && treeColumn.isDisposed() == false) {

            _columnWidth_TagImage = treeColumn.getWidth();
         }
      }
   }

   private void onSelect_CategoryItem(final TreeSelection treeSelection) {

      if (_isInExpandingSelection) {

         // prevent endless loops
         return;
      }

      final TreePath[] selectedTreePaths = treeSelection.getPaths();
      if (selectedTreePaths.length == 0) {
         return;
      }

      final TreePath selectedTreePath = selectedTreePaths[0];
      if (selectedTreePath == null) {
         return;
      }

      onSelect_CategoryItem_10_AutoExpandCollapse(treeSelection);
   }

   /**
    * This is not yet working thoroughly because the expanded position moves up or down and all
    * expanded children are not visible (but they could) like when the triangle (+/-) icon in the
    * tree is clicked.
    *
    * @param treeSelection
    */
   private void onSelect_CategoryItem_10_AutoExpandCollapse(final ITreeSelection treeSelection) {

      if (_isInCollapseAll) {

         // prevent auto expand
         return;
      }

      if (_isBehaviour_SingleExpand_CollapseOthers) {

         /*
          * run async because this is doing a reselection which cannot be done within the current
          * selection event
          */
         Display.getCurrent().asyncExec(new Runnable() {

            private long           __expandRunnableCounter = ++_expandRunnableCounter;

            private ITreeSelection __treeSelection         = treeSelection;

            @Override
            public void run() {

               // check if a newer expand event occurred
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               if (_tagViewer.getTree().isDisposed()) {
                  return;
               }

               /*
                * With Linux the selection event is fired twice when a subcategory, e.g. month is
                * selected which causes an endless loop !!!
                */
               final long now = System.currentTimeMillis();
               final long timeDiff = now - _lastExpandSelectionTime;
               if (timeDiff < 200) {
                  return;
               }

               onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(__treeSelection);
            }
         });

      } else {

         if (_isBehaviour_OnSelect_ExpandCollapse) {

            // expand folder with one mouse click but not with the keyboard

            final TreePath selectedTreePath = treeSelection.getPaths()[0];

            expandCollapseItem((TreeViewerItem) selectedTreePath.getLastSegment());
         }
      }
   }

   /**
    * This behavior is complex and still have possible problems.
    *
    * @param treeSelection
    */
   private void onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(final ITreeSelection treeSelection) {

      /*
       * Create expanded elements from the tree selection
       */
      final TreePath selectedTreePath = treeSelection.getPaths()[0];
      final int numSegments = selectedTreePath.getSegmentCount();

      final Object[] expandedElements = new Object[numSegments];

      for (int segmentIndex = 0; segmentIndex < numSegments; segmentIndex++) {
         expandedElements[segmentIndex] = selectedTreePath.getSegment(segmentIndex);
      }

      _isInExpandingSelection = true;
      {
         final Tree tree = _tagViewer.getTree();

         tree.setRedraw(false);
         {
            final TreeItem topItem = tree.getTopItem();

            final boolean isExpanded = _tagViewer.getExpandedState(selectedTreePath);

            /*
             * collapse all tree paths
             */
            final TreePath[] allExpandedTreePaths = _tagViewer.getExpandedTreePaths();
            for (final TreePath treePath : allExpandedTreePaths) {
               _tagViewer.setExpandedState(treePath, false);
            }

            /*
             * expand and select selected folder
             */
            _tagViewer.setExpandedElements(expandedElements);
            _tagViewer.setSelection(treeSelection, true);

            if (_isBehaviour_OnSelect_ExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _tagViewer.setExpandedState(selectedTreePath, false);
            }

            /**
             * Set top item to the previous top item, otherwise the expanded/collapse item is
             * positioned at the bottom and the UI is jumping all the time
             * <p>
             * Win behaviour: When an item is set to top which was collapsed before, it will be
             * expanded
             */
            if (topItem.isDisposed() == false) {
               tree.setTopItem(topItem);
            }
         }
         tree.setRedraw(true);
      }
      _isInExpandingSelection = false;
      _lastExpandSelectionTime = System.currentTimeMillis();
   }

   /**
    * Ctrl state is not available in the tree viewer selection event -> use tree event
    *
    * @param event
    */
   private void onTagTree_DoubleClick(final Event event) {

      final boolean isCtrl = (event.stateMask & SWT.CTRL) != 0;

      if (isCtrl) {

         final Object selection = ((IStructuredSelection) _tagViewer.getSelection()).getFirstElement();

         if (selection instanceof TVITaggingView_Tag
               || selection instanceof TVITaggingView_TagCategory) {

            // edit tag/category

            _actionEditTag.run();
         }
      }
   }

   private void onTagTree_MouseDown(final Event event) {

      _isMouseContextMenu = event.button == 3;
   }

   private void onTagViewer_DoubleClick() {

      final Object selection = ((IStructuredSelection) _tagViewer.getSelection()).getFirstElement();

      if (selection instanceof TVITaggingView_Tour) {

         TourManager.getInstance().tourDoubleClickAction(TaggingView.this, _tourDoubleClickState);

      } else if (selection != null) {

         // expand/collapse current item

         final TreeViewerItem treeItem = (TreeViewerItem) selection;

         expandCollapseItem(treeItem);
      }
   }

   private void onTagViewer_RenameTag() {

      final Object selection = ((IStructuredSelection) _tagViewer.getSelection()).getFirstElement();

      if (selection instanceof TVITaggingView_Tag || selection instanceof TVITaggingView_TagCategory) {

         // edit tag/category

         _actionEditTag.run();
      }
   }

   private void onTagViewer_Selection(final SelectionChangedEvent event) {

      if (_isMouseContextMenu) {
         return;
      }

      final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
      final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

      if (selectedItem instanceof final TVITaggingView_Tour tourItem && selectedTours.size() == 1) {

         // one tour is selected

         _postSelectionProvider.setSelection(new SelectionTourId(tourItem.getTourId()));

      } else if (selectedItem instanceof TVITaggingView_Tag
            || selectedItem instanceof TVITaggingView_TagCategory
            || selectedItem instanceof TVITaggingView_Year
            || selectedItem instanceof TVITaggingView_Month

      ) {

         // category is selected, expand/collapse category items

         if (_isSelectedWithKeyboard == false) {

            // do not expand/collapse when keyboard is used -> unusable

            onSelect_CategoryItem((TreeSelection) event.getSelection());
         }

      } else {

         // multiple tours are selected

         final ArrayList<Long> tourIds = new ArrayList<>();

         for (final Object viewItem : selectedTours) {

            if (viewItem instanceof final TVITaggingView_Tour tourItem) {
               tourIds.add(tourItem.getTourId());
            }
         }

         if (tourIds.size() > 0) {
            _postSelectionProvider.setSelection(new SelectionTourIds(tourIds));
         }
      }

      // reset state
      _isSelectedWithKeyboard = false;

      enableActions(false);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         final Object[] expandedElements = _tagViewer.getExpandedElements();
         final ISelection selection = _tagViewer.getSelection();

         _tagViewer.getTree().dispose();

         createUI_10_TagViewer(_viewerContainer);
         _viewerContainer.layout();

         reloadViewer_0_SetContent();

         _tagViewer.setExpandedElements(expandedElements);
         _tagViewer.setSelection(selection);
      }
      _viewerContainer.setRedraw(true);

      return _tagViewer;
   }

   /**
    * Reload the content of the tag viewer
    */
   @Override
   public void reloadViewer() {

      final Tree tree = _tagViewer.getTree();

      tree.setRedraw(false);
      {
         final Object[] expandedElements = _tagViewer.getExpandedElements();

         reloadViewer_0_SetContent();

         _tagViewer.setExpandedElements(expandedElements);
      }
      tree.setRedraw(true);
   }

   private void reloadViewer_0_SetContent() {

      final boolean isTreeLayoutHierarchical = _tagViewLayout == TAG_VIEW_LAYOUT_HIERARCHICAL;

      _rootItem = new TVITaggingView_Root(_tagViewer, isTreeLayoutHierarchical);

      // first: load all tree items
      loadAllTreeItems();

      // second: update viewer
      _tagViewer.setInput(_rootItem);
   }

   private void restoreState() {

      _tagViewLayout = TAG_VIEW_LAYOUT_HIERARCHICAL;

      // restore view layout
      try {

         final int viewLayout = _state.getInt(MEMENTO_TAG_VIEW_LAYOUT);
         switch (viewLayout) {

         case TAG_VIEW_LAYOUT_FLAT:

            _tagViewLayout = viewLayout;
            break;

         case TAG_VIEW_LAYOUT_HIERARCHICAL:

            _tagViewLayout = viewLayout;
            break;

         default:
            break;
         }

      } catch (final NumberFormatException e) {

         // set default tag view layout
         _tagViewLayout = TAG_VIEW_LAYOUT_HIERARCHICAL;
      }

      // on mouse select -> expand/collapse
      _isBehaviour_OnSelect_ExpandCollapse = Util.getStateBoolean(_state, STATE_IS_ON_SELECT_EXPAND_COLLAPSE, true);
      _actionOnMouseSelect_ExpandCollapse.setChecked(_isBehaviour_OnSelect_ExpandCollapse);

      // single expand -> collapse others
      _isBehaviour_SingleExpand_CollapseOthers = Util.getStateBoolean(_state, STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, true);
      _actionSingleExpand_CollapseOthers.setChecked(_isBehaviour_SingleExpand_CollapseOthers);

      _tagFilterType = (TagFilterType) Util.getStateEnum(_state, STATE_TAG_FILTER, TagFilterType.ALL_IS_DISPLAYED);

      updateUI_TagFilter();
      updateUI_TagLayoutAction();
      updateToolTipState();
   }

   /**
    * Restore viewer state after the viewer is loaded.
    */
   private void restoreState_Viewer() {

      /*
       * Expanded tag categories
       */
      final long[] allStateItems = Util.getStateLongArray(_state, STATE_EXPANDED_ITEMS, null);
      if (allStateItems != null) {

         final ArrayList<TreePath> viewerTreePaths = new ArrayList<>();

         final ArrayList<StateSegment[]> allStateSegments = restoreState_Viewer_GetSegments(allStateItems);
         for (final StateSegment[] stateSegments : allStateSegments) {

            final ArrayList<Object> pathSegments = new ArrayList<>();

            // start tree items with the root and go deeper with every segment
            ArrayList<TreeViewerItem> treeItems = _rootItem.getFetchedChildren();

            for (final StateSegment stateSegment : stateSegments) {

               /*
                * This is somehow recursive as it goes deeper into the child tree items until there
                * are no children
                */
               treeItems = restoreState_Viewer_ExpandItem(pathSegments, treeItems, stateSegment);
            }

            if (pathSegments.size() > 0) {
               viewerTreePaths.add(new TreePath(pathSegments.toArray()));
            }
         }

         if (viewerTreePaths.size() > 0) {
            _tagViewer.setExpandedTreePaths(viewerTreePaths.toArray(new TreePath[viewerTreePaths.size()]));
         }
      }
   }

   /**
    * @param pathSegments
    * @param treeItems
    * @param stateSegment
    *
    * @return Returns children when it could be expanded otherwise <code>null</code>.
    */
   private ArrayList<TreeViewerItem> restoreState_Viewer_ExpandItem(final ArrayList<Object> pathSegments,
                                                                    final ArrayList<TreeViewerItem> treeItems,
                                                                    final StateSegment stateSegment) {

      if (treeItems == null) {
         return null;
      }

      final long stateData = stateSegment.__itemData;

      if (stateSegment.__itemType == STATE_ITEM_TYPE_CATEGORY) {

         for (final TreeViewerItem treeItem : treeItems) {

            if (treeItem instanceof final TVITaggingView_TagCategory categoryItem) {

               final long viewerCatId = categoryItem.getTourTagCategory().getCategoryId();

               if (viewerCatId == stateData) {

                  pathSegments.add(treeItem);

                  return categoryItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.__itemType == STATE_ITEM_TYPE_TAG) {

         for (final TreeViewerItem treeItem : treeItems) {

            if (treeItem instanceof final TVITaggingView_Tag tagItem) {

               final long viewerTagId = tagItem.getTagId();

               if (viewerTagId == stateData) {

                  pathSegments.add(treeItem);

                  return tagItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.__itemType == STATE_ITEM_TYPE_YEAR) {

         for (final TreeViewerItem treeItem : treeItems) {

            if (treeItem instanceof final TVITaggingView_Year yearItem) {

               final long viewerYear = yearItem.getYear();

               if (viewerYear == stateData) {

                  pathSegments.add(treeItem);

                  return yearItem.getFetchedChildren();
               }
            }
         }

      } else if (stateSegment.__itemType == STATE_ITEM_TYPE_MONTH) {

         for (final TreeViewerItem treeItem : treeItems) {

            if (treeItem instanceof final TVITaggingView_Month monthItem) {

               final long viewerYear = monthItem.getMonth();

               if (viewerYear == stateData) {

                  pathSegments.add(treeItem);

                  return monthItem.getFetchedChildren();
               }
            }
         }
      }

      return null;
   }

   /**
    * Convert state structure into a 'segment' structure.
    */
   private ArrayList<StateSegment[]> restoreState_Viewer_GetSegments(final long[] expandedItems) {

      final ArrayList<StateSegment[]> allTreePathSegments = new ArrayList<>();
      final ArrayList<StateSegment> currentSegments = new ArrayList<>();

      for (int itemIndex = 0; itemIndex < expandedItems.length;) {

         // ensure array bounds
         if (itemIndex + 1 >= expandedItems.length) {
            // this should not happen when data are not corrupted
            break;
         }

         final long itemType = expandedItems[itemIndex++];
         final long itemData = expandedItems[itemIndex++];

         if (itemType == STATE_ITEM_TYPE_SEPARATOR) {

            // a new tree path starts

            if (currentSegments.size() > 0) {

               // keep current tree path segments

               allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));

               // start a new path
               currentSegments.clear();
            }

         } else {

            // a new segment is available

            if (itemType == STATE_ITEM_TYPE_CATEGORY
                  || itemType == STATE_ITEM_TYPE_TAG
                  || itemType == STATE_ITEM_TYPE_YEAR
                  || itemType == STATE_ITEM_TYPE_MONTH) {

               currentSegments.add(new StateSegment(itemType, itemData));
            }
         }
      }

      if (currentSegments.size() > 0) {
         allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));
      }

      return allTreePathSegments;
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(MEMENTO_TAG_VIEW_LAYOUT, _tagViewLayout);

      _state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionSingleExpand_CollapseOthers.isChecked());
      _state.put(STATE_IS_ON_SELECT_EXPAND_COLLAPSE, _actionOnMouseSelect_ExpandCollapse.isChecked());

      Util.setStateEnum(_state, STATE_TAG_FILTER, _tagFilterType);

      saveState_ExpandedItems();
   }

   /**
    * Save state for expanded tree items.
    */
   private void saveState_ExpandedItems() {

      final Object[] visibleExpanded = _tagViewer.getVisibleExpandedElements();

      if (visibleExpanded.length == 0) {
         Util.setState(_state, STATE_EXPANDED_ITEMS, new long[0]);
         return;
      }

      final LongArrayList expandedItems = new LongArrayList();

      final TreePath[] expandedOpenedTreePaths = net.tourbook.common.UI.getExpandedOpenedItems(
            visibleExpanded,
            _tagViewer.getExpandedTreePaths());

      for (final TreePath expandedPath : expandedOpenedTreePaths) {

         // start a new path, always set it twice to have a even structure
         expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);
         expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);

         for (int segmentIndex = 0; segmentIndex < expandedPath.getSegmentCount(); segmentIndex++) {

            final Object segment = expandedPath.getSegment(segmentIndex);

            if (segment instanceof final TVITaggingView_TagCategory categoryItem) {

               expandedItems.add(STATE_ITEM_TYPE_CATEGORY);
               expandedItems.add(categoryItem.getTourTagCategory().getCategoryId());

            } else if (segment instanceof final TVITaggingView_Tag tagItem) {

               expandedItems.add(STATE_ITEM_TYPE_TAG);
               expandedItems.add((tagItem).getTagId());

            } else if (segment instanceof final TVITaggingView_Year yeatItem) {

               expandedItems.add(STATE_ITEM_TYPE_YEAR);
               expandedItems.add(yeatItem.getYear());

            } else if (segment instanceof final TVITaggingView_Month monthItem) {

               expandedItems.add(STATE_ITEM_TYPE_MONTH);
               expandedItems.add(monthItem.getMonth());
            }
         }
      }

      Util.setState(_state, STATE_EXPANDED_ITEMS, expandedItems.toArray());
   }

   private void setCellColor(final ViewerCell cell, final Object element) {

      // set color

      if (element instanceof TVITaggingView_TagCategory) {

         cell.setForeground(_colorContentCategory);

      } else if (element instanceof TVITaggingView_Tag) {

         cell.setForeground(_colorContentSubCategory);

      } else if (element instanceof TVITaggingView_Year) {

         cell.setForeground(_colorDateCategory);

      } else if (element instanceof TVITaggingView_Month) {

         cell.setForeground(_colorDateSubCategory);

      } else if (element instanceof TVITaggingView_Tour) {

         cell.setForeground(_colorTour);
      }
   }

   @Override
   public void setFocus() {

      _tagViewer.getTree().setFocus();
   }

   private void setTagFilter_NoTours() {

      _tagFilterType = TagFilterType.TAGS_WITHOUT_TOURS;

      _action_ToggleTagFilter.setChecked(true);
      _action_ToggleTagFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagFilter_NoTours));
   }

   private void setTagFilter_ShowAll() {

      _tagFilterType = TagFilterType.ALL_IS_DISPLAYED;

      _action_ToggleTagFilter.setChecked(false);
      _action_ToggleTagFilter.setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter));
   }

   private void setTagFilter_WithTours() {

      _tagFilterType = TagFilterType.TAGS_WITH_TOURS;

      _action_ToggleTagFilter.setChecked(true);
      _action_ToggleTagFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourTagFilter));
   }

   private void setTagViewTitle(final Object newInput) {

      String description = UI.EMPTY_STRING;

      if (newInput instanceof final TVITaggingView_Tag tagItem) {

         description = Messages.tag_view_title_tag + tagItem.getTourTag().getTagName();

      } else if (newInput instanceof final TVITaggingView_TagCategory categoryItem) {

         description = Messages.tag_view_title_tag_category + categoryItem.getTourTagCategory().getCategoryName();
      }

      setContentDescription(description);
   }

   private void updateColors() {

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

// SET_FORMATTING_OFF

      _colorContentCategory      = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_CATEGORY);
      _colorContentSubCategory   = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_SUB_CATEGORY);
      _colorDateCategory         = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_DATE_CATEGORY);
      _colorDateSubCategory      = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_DATE_SUB_CATEGORY);
      _colorTour                 = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_TOUR);

// SET_FORMATTING_ON
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   private void updateToolTipState() {

      _isToolTipInTag = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG);
      _isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE);
      _isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS);
   }

   private void updateUI_TagFilter() {

      if (_tagFilterType == TagFilterType.ALL_IS_DISPLAYED) {

         setTagFilter_ShowAll();

      } else if (_tagFilterType == TagFilterType.TAGS_WITH_TOURS) {

         setTagFilter_WithTours();

      } else {

         setTagFilter_NoTours();
      }
   }

   private void updateUI_TagLayoutAction() {

      if (_tagViewLayout == TAG_VIEW_LAYOUT_HIERARCHICAL) {

         // hierarchy is displayed -> show icon/tooltip for flat view

         _action_ToggleTagLayout.setToolTipText(Messages.Tour_Tags_Action_Layout_Flat_Tooltip);
         _action_ToggleTagLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Flat));

      } else {

         // flat view is displayed -> show icon/tooltip for hierarchy view

         _action_ToggleTagLayout.setToolTipText(Messages.Tour_Tags_Action_Layout_Hierarchical_Tooltip);
         _action_ToggleTagLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Hierarchical));
      }
   }

   /**
    * !!! Recursive !!! method to update the tags in the viewer, this method handles changes in the
    * tag structure
    *
    * @param rootItem
    * @param changedTags
    * @param isAddMode
    */
   private void updateViewerAfterTagStructureIsModified(final TreeViewerItem parentItem,
                                                        final ChangedTags changedTags,
                                                        final boolean isAddMode) {

      final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

      if (children == null) {
         return;
      }

      // loop: all children of the current parent item
      for (final Object object : children) {

         if (object instanceof final TVITaggingView_Tag tagItem) {

            final long viewerTagId = tagItem.getTagId();

            final HashMap<Long, TourTag> modifiedTags = changedTags.getModifiedTags();
            final ArrayList<Long> removedIds = new ArrayList<>();

            for (final Long modifiedTagId : modifiedTags.keySet()) {
               if (viewerTagId == modifiedTagId.longValue()) {

                  /*
                   * current tag was modified
                   */

                  // add/remove tours from the tag
                  tagItem.refresh(_tagViewer, changedTags.getModifiedTours(), changedTags.isAddMode());

                  // update tag totals
                  TVITaggingView_Item.readTagTotals(tagItem);

                  // update viewer
                  _tagViewer.refresh(tagItem);

                  removedIds.add(modifiedTagId);
               }
            }

            /*
             * modified tag id exists only once in the tree viewer, remove the id's outside of the
             * foreach loop to avid the exception ConcurrentModificationException
             */
            for (final Long removedId : removedIds) {
               modifiedTags.remove(removedId);
            }

            // optimize
            if (modifiedTags.isEmpty()) {
               return;
            }

         } else if (object instanceof final TreeViewerItem treeViewerItem) {
            updateViewerAfterTagStructureIsModified(treeViewerItem, changedTags, isAddMode);
         }
      }
   }

   /**
    * !!!Recursive !!! delete tour items
    *
    * @param rootItem
    * @param deletedTourIds
    */
   private void updateViewerAfterTourIsDeleted(final TreeViewerItem parentItem,
                                               final ArrayList<ITourItem> deletedTourIds) {

      final ArrayList<TreeViewerItem> parentChildren = parentItem.getUnfetchedChildren();

      if (parentChildren == null) {
         return;
      }

      final ArrayList<TVITaggingView_Tour> deletedTourItems = new ArrayList<>();

      // loop: all tree children items
      for (final Object object : parentChildren) {
         if (object instanceof final TreeViewerItem childItem) {

            if (childItem instanceof final TVITaggingView_Tour tourItem) {

               final long tourItemId = tourItem.getTourId();

               // loop: all deleted tour id's
               for (final ITourItem deletedTourItem : deletedTourIds) {
                  if (deletedTourItem.getTourId().longValue() == tourItemId) {

                     // keep deleted tour item
                     deletedTourItems.add(tourItem);

                     break;
                  }
               }

            } else {

               // update children
               updateViewerAfterTourIsDeleted(childItem, deletedTourIds);
            }
         }
      }

      if (deletedTourItems.size() > 0) {

         // update model
         parentChildren.removeAll(deletedTourItems);

         // update viewer
         _tagViewer.remove(deletedTourItems.toArray());
      }
   }

   /**
    * !!!Recursive !!! update the data for all tour items
    *
    * @param rootItem
    * @param modifiedTours
    */
   private void updateViewerAfterTourIsModified(final TreeViewerItem parentItem,
                                                final ArrayList<TourData> modifiedTours) {

      final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

      if (children == null) {
         return;
      }

      // loop: all children
      for (final Object object : children) {
         if (object instanceof final TreeViewerItem treeItem) {

            if (treeItem instanceof final TVITaggingView_Tour tourItem) {

               final long tourItemId = tourItem.getTourId();

               for (final TourData modifiedTourData : modifiedTours) {
                  if (modifiedTourData.getTourId().longValue() == tourItemId) {

                     // update tree item

                     final TourType tourType = modifiedTourData.getTourType();
                     if (tourType != null) {
                        tourItem.tourTypeId = tourType.getTypeId();
                     }

                     // update item title
                     tourItem.tourTitle = modifiedTourData.getTourTitle();

                     // update item tags
                     final Set<TourTag> tourTags = modifiedTourData.getTourTags();
                     final ArrayList<Long> tagIds;

                     tourItem.tagIds = tagIds = new ArrayList<>();
                     for (final TourTag tourTag : tourTags) {
                        tagIds.add(tourTag.getTagId());
                     }

                     // update item in the viewer
                     _tagViewer.update(tourItem, null);

                     // a tour exists only once as a child in a tree item
                     break;
                  }
               }

            } else {
               // update children
               updateViewerAfterTourIsModified(treeItem, modifiedTours);
            }
         }
      }
   }

}
