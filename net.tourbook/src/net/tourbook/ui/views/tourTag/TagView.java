/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.ui.views.tourTag;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionMenuSetAllTagStructures;
import net.tourbook.tag.ActionMenuSetTagStructure;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionRenameTag;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.ChangedTags;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionCollapseAll;
import net.tourbook.ui.ActionCollapseOthers;
import net.tourbook.ui.ActionEditTour;
import net.tourbook.ui.ActionExpandSelection;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionRefreshView;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

public class TagView extends ViewPart implements ISelectedTours, ITourViewer {

	static public final String				ID								= "net.tourbook.views.tagViewID";	//$NON-NLS-1$

	private static final String				MEMENTO_TAG_VIEW_LAYOUT			= "tagview.layout";

	static final int						TAG_VIEW_LAYOUT_FLAT			= 0;
	static final int						TAG_VIEW_LAYOUT_HIERARCHICAL	= 10;

	private int								fTagViewLayout					= TAG_VIEW_LAYOUT_HIERARCHICAL;

	private IMemento						fSessionMemento;

	private Composite						fViewerContainer;

	private TreeViewer						fTagViewer;
//	private DrillDownAdapter				fDrillDownAdapter;
	private TVITagViewRoot					fRootItem;

	private ColumnManager					fColumnManager;

	private PostSelectionProvider			fPostSelectionProvider;

	private ITourPropertyListener			fTourPropertyListener;
	private ISelectionListener				fPostSelectionListener;

	private ActionSetTourTag				fActionAddTag;
	private ActionCollapseAll				fActionCollapseAll;
	private ActionCollapseOthers			fActionCollapseOthers;
	private ActionEditQuick					fActionEditQuick;
	private ActionEditTour					fActionEditTour;
	private ActionExpandSelection			fActionExpandSelection;
	private ActionOpenPrefDialog			fActionOpenTagPrefs;
	private ActionSetLayoutHierarchical		fActionSetLayoutHierarchical;
	private ActionSetLayoutFlat				fActionSetLayoutFlat;
	private ActionRefreshView				fActionRefreshView;
	private ActionRemoveAllTags				fActionRemoveAllTags;
	private ActionSetTourTag				fActionRemoveTag;
	private ActionRenameTag					fActionRenameTag;
	private ActionMenuSetAllTagStructures	fActionSetAllTagStructures;
	private ActionMenuSetTagStructure		fActionSetTagStructure;
	private ActionSetTourType				fActionSetTourType;

	private ActionModifyColumns				fActionModifyColumns;

	private IPropertyChangeListener			fPrefChangeListener;

	private final Image						fImgTagCategory					= TourbookPlugin.getImageDescriptor(Messages.Image__tag_category)
																					.createImage();
	private final Image						fImgTag							= TourbookPlugin.getImageDescriptor(Messages.Image__tag)
																					.createImage();
	private final Image						fImgTagRoot						= TourbookPlugin.getImageDescriptor(Messages.Image__tag_root)
																					.createImage();

	private IPartListener2					fPartListener;

	private static final NumberFormat		fNF								= NumberFormat.getNumberInstance();

	private final class TagComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			if (obj1 instanceof TVITagViewTour && obj2 instanceof TVITagViewTour) {

				// sort tours by date
				final TVITagViewTour tourItem1 = (TVITagViewTour) (obj1);
				final TVITagViewTour tourItem2 = (TVITagViewTour) (obj2);

				return tourItem1.tourDate.compareTo(tourItem2.tourDate);
			}

			if (obj1 instanceof TVITagViewYear && obj2 instanceof TVITagViewYear) {
				final TVITagViewYear yearItem1 = (TVITagViewYear) (obj1);
				final TVITagViewYear yearItem2 = (TVITagViewYear) (obj2);

				return yearItem1.compareTo(yearItem2);
			}

			if (obj1 instanceof TVITagViewMonth && obj2 instanceof TVITagViewMonth) {
				final TVITagViewMonth monthItem1 = (TVITagViewMonth) (obj1);
				final TVITagViewMonth monthItem2 = (TVITagViewMonth) (obj2);

				return monthItem1.compareTo(monthItem2);
			}

			return 0;
		}
	}

	/**
	 * The comparator is necessary to set and restore the expanded elements
	 */
	private class TagComparer implements IElementComparer {

		public boolean equals(final Object a, final Object b) {

			if (a == b) {

				return true;

			} else if (a instanceof TVITagViewYear && b instanceof TVITagViewYear) {

				final TVITagViewYear yearItem1 = (TVITagViewYear) a;
				final TVITagViewYear yearItem2 = (TVITagViewYear) b;

				return yearItem1.getTagId() == yearItem2.getTagId() //
						&& yearItem1.getYear() == yearItem2.getYear();

			} else if (a instanceof TVITagViewMonth && b instanceof TVITagViewMonth) {

				final TVITagViewMonth month1 = (TVITagViewMonth) a;
				final TVITagViewMonth month2 = (TVITagViewMonth) b;
				final TVITagViewYear yearItem1 = month1.getYearItem();
				final TVITagViewYear yearItem2 = month2.getYearItem();

				return yearItem1.getTagId() == yearItem2.getTagId()
						&& yearItem1.getYear() == yearItem2.getYear()
						&& month1.getMonth() == month2.getMonth();

			} else if (a instanceof TVITagViewTagCategory && b instanceof TVITagViewTagCategory) {

				return ((TVITagViewTagCategory) a).tagCategoryId == ((TVITagViewTagCategory) b).tagCategoryId;

			} else if (a instanceof TVITagViewTag && b instanceof TVITagViewTag) {

				return ((TVITagViewTag) a).getTagId() == ((TVITagViewTag) b).getTagId();

			}

			return false;
		}

		public int hashCode(final Object element) {
			return 0;
		}

	}

	private class TagContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {

			if (parentElement instanceof TVITagViewItem) {
				return ((TVITagViewItem) parentElement).getFetchedChildrenAsArray();
			}

			return new Object[0];
		}

		public Object[] getElements(final Object inputElement) {
			return getChildren(inputElement);
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {

			if (newInput == null) {
				return;
			}

			setTagViewTitle(newInput);
		}
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
					reloadViewer();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update viewer

					fTagViewer.refresh();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fSessionMemento);
					fColumnManager.resetColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer();

				} else if (property.equals(ITourbookPreferences.TAG_COLOR_AND_LAYOUT_CHANGED)) {

					fTagViewer.getTree()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.TAG_VIEW_SHOW_LINES));

					fTagViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTagViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					final SelectionDeletedTours deletedTourSelection = (SelectionDeletedTours) selection;

					updateViewerAfterTourIsDeleted(fRootItem, deletedTourSelection.removedTours);
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked")
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_TAGS_CHANGED) {
					if (propertyData instanceof ChangedTags) {

						final ChangedTags changedTags = (ChangedTags) propertyData;

						final boolean isAddMode = changedTags.isAddMode();

						// get a clone of the modified tours/tags because the tours are removed from the list
						final ChangedTags changedTagsClone = new ChangedTags(changedTags.getModifiedTags(),
								changedTags.getModifiedTours(),
								isAddMode);

						updateViewerAfterTagStructureIsModified(fRootItem, changedTagsClone, isAddMode);
					}

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					reloadViewer();

				} else if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					// get a clone of the modified tours because the tours are removed from the list
					final ArrayList<TourData> modifiedTours = (ArrayList<TourData>) ((ArrayList<TourData>) propertyData).clone();

					updateViewerAfterTourIsModified(fRootItem, modifiedTours);
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);

		fActionSetTourType = new ActionSetTourType(this);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionRefreshView = new ActionRefreshView(this);
		fActionSetTagStructure = new ActionMenuSetTagStructure(this);
		fActionSetAllTagStructures = new ActionMenuSetAllTagStructures(this);
		fActionRenameTag = new ActionRenameTag(this);

		fActionExpandSelection = new ActionExpandSelection(this);
		fActionCollapseAll = new ActionCollapseAll(this);
		fActionCollapseOthers = new ActionCollapseOthers(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.app_action_tag_open_tagging_structure,
				"net.tourbook.preferences.PrefPageTags");

		fActionSetLayoutFlat = new ActionSetLayoutFlat(this);
		fActionSetLayoutHierarchical = new ActionSetLayoutHierarchical(this);

		fActionModifyColumns = new ActionModifyColumns(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		final Control tourViewer = fTagViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createActions();
		fillViewMenu();

		// viewer must be created after the action are created
		createTagViewer(fViewerContainer);

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		addTourPropertyListener();
		addPrefListener();
		addPartListener();
		addSelectionListener();

		restoreState(fSessionMemento);
		reloadViewer();
	}

	private void createTagViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setHeaderVisible(true);

		tree.setLinesVisible(TourbookPlugin.getDefault()
				.getPluginPreferences()
				.getBoolean(ITourbookPreferences.TAG_VIEW_SHOW_LINES));

		fTagViewer = new TreeViewer(tree);
		fColumnManager.createColumns();
//		fDrillDownAdapter = new DrillDownAdapter(fTagViewer);

		fTagViewer.setContentProvider(new TagContentProvider());
		fTagViewer.setComparer(new TagComparer());
		fTagViewer.setComparator(new TagComparator());
		fTagViewer.setUseHashlookup(true);

		fTagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

				if (selectedItem instanceof TVITagViewTour) {
					final TVITagViewTour tourItem = (TVITagViewTour) selectedItem;
					fPostSelectionProvider.setSelection(new SelectionTourId(tourItem.getTourId()));
				}

				enableActions();
			}
		});

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITagViewTour) {

					// open tour in the tour editor

					TourManager.getInstance().openTourInEditor(((TVITagViewTour) selection).getTourId());

				} else if (selection != null) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (fTagViewer.getExpandedState(tourItem)) {
						fTagViewer.collapseToLevel(tourItem, 1);
					} else {
						fTagViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		/*
		 * the context menu must be created AFTER the viewer is created which is also done after the
		 * measurement system has changed, if not, the context menu is not displayed because it
		 * belongs to the old viewer
		 */
		createContextMenu();

		fillToolBar();
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * tree column
		 */
		colDef = TreeColumnFactory.TAG.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITagViewItem viewItem = (TVITagViewItem) element;
				final StyledString styledString = new StyledString();

				if (viewItem instanceof TVITagViewTour) {

					styledString.append(viewItem.treeColumn);

					final TVITagViewTour tourItem = (TVITagViewTour) viewItem;
					cell.setImage(UI.getInstance().getTourTypeImage(tourItem.tourTypeId));

				} else if (viewItem instanceof TVITagViewTag) {

					final TVITagViewTag tagItem = (TVITagViewTag) viewItem;

					styledString.append(viewItem.treeColumn, UI.TAG_STYLER);
					styledString.append("   " + viewItem.colItemCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					cell.setImage(tagItem.isRoot ? fImgTagRoot : fImgTag);

				} else if (viewItem instanceof TVITagViewTagCategory) {

					styledString.append(viewItem.treeColumn, UI.TAG_CATEGORY_STYLER);
					cell.setImage(fImgTagCategory);

				} else if (viewItem instanceof TVITagViewYear || viewItem instanceof TVITagViewMonth) {

					styledString.append(viewItem.treeColumn);
					styledString.append("   " + viewItem.colItemCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$

					if (viewItem instanceof TVITagViewMonth) {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.TAG_SUB_SUB_COLOR));
					} else {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.TAG_SUB_COLOR));
					}

				} else {
					styledString.append(viewItem.treeColumn);
				}

				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		});

		/*
		 * column: title
		 */
		colDef = TreeColumnFactory.TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVITagViewTour) {
					cell.setText(((TVITagViewTour) element).tourTitle);
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: tags
		 */
		colDef = TreeColumnFactory.TOUR_TAGS.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITagViewTour) {
					TourDatabase.getInstance();
					cell.setText(TourDatabase.getTagNames(((TVITagViewTour) element).tagIds));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: distance (km/miles)
		 */
		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				// set distance
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				final TVITagViewItem treeItem = (TVITagViewItem) element;
				final String distance = fNF.format(((float) treeItem.colDistance) / 1000 / UI.UNIT_VALUE_DISTANCE);
				cell.setText(distance);

				setCellColor(cell, element);
			}
		});

		/*
		 * column: recording time (h)
		 */
		colDef = TreeColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long recordingTime = ((TVITagViewItem) element).colRecordingTime;

				cell.setText(new Formatter().format(Messages.Format_hhmm,
						(recordingTime / 3600),
						((recordingTime % 3600) / 60)).toString());

				setCellColor(cell, element);
			}
		});

		/*
		 * column: driving time (h)
		 */
		colDef = TreeColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				final long drivingTime = ((TVITagViewItem) element).colDrivingTime;

				cell.setText(new Formatter().format(Messages.Format_hhmm,
						(drivingTime / 3600),
						((drivingTime % 3600) / 60)).toString());
				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude up (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				cell.setText(Long.toString((long) (((TVITagViewItem) element).colAltitudeUp / UI.UNIT_VALUE_ALTITUDE)));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude down (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				cell.setText(Long.toString((long) (((TVITagViewItem) element).colAltitudeDown / UI.UNIT_VALUE_ALTITUDE)));
				setCellColor(cell, element);
			}
		});

// additional columns are disabled because they slow down the scrolling in the tree

		/*
		 * column: max pulse
		 */
		colDef = TreeColumnFactory.MAX_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				cell.setText(Long.toString(((TVITagViewItem) element).colMaxPulse));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: max altitude
		 */
		colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				cell.setText(Long.toString(((TVITagViewItem) element).colMaxAltitude));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: max speed
		 */
		colDef = TreeColumnFactory.MAX_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(((TVITagViewItem) element).colMaxSpeed / UI.UNIT_VALUE_DISTANCE));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg speed
		 */
		colDef = TreeColumnFactory.AVG_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				cell.setText(Long.toString(((TVITagViewItem) element).colAvgPulse));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg cadence
		 */
		colDef = TreeColumnFactory.AVG_CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				cell.setText(Long.toString(((TVITagViewItem) element).colAvgCadence));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: avg temperature
		 */
		colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITagViewTagCategory) {
					return;
				}

				long temperature = ((TVITagViewItem) element).colAvgTemperature;

				if (UI.UNIT_VALUE_TEMPERATURE != 1) {
					temperature = (long) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
				}
				cell.setText(Long.toString(temperature));

				setCellColor(cell, element);
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);
		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		fImgTag.dispose();
		fImgTagRoot.dispose();
		fImgTagCategory.dispose();

		super.dispose();
	}

	private void enableActions() {

		final StructuredSelection selection = (StructuredSelection) fTagViewer.getSelection();
		final int selectedItems = selection.size();

		// count number of selected items
		int tourItems = 0;
		int tagItems = 0;
		int categoryItems = 0;
		int items = 0;
		int otherItems = 0;
		TVITagViewTour firstTour = null;
		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TVITagViewTour) {
				if (tourItems == 0) {
					firstTour = (TVITagViewTour) treeItem;
				}
				tourItems++;
			} else if (treeItem instanceof TVITagViewTag) {
				tagItems++;
			} else if (treeItem instanceof TVITagViewTagCategory) {
				categoryItems++;
			} else {
				otherItems++;
			}
			items++;
		}
		final boolean isTourSelected = tourItems > 0;

		fActionEditTour.setEnabled(tourItems == 1);
		fActionEditQuick.setEnabled(tourItems == 1);

		// action: set tour type
		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		// action: add tag
		fActionAddTag.setEnabled(isTourSelected);

		// actions: remove tags
		if (firstTour != null && tourItems == 1) {

			// one tour is selected

			final ArrayList<Long> tagIds = firstTour.tagIds;
			if (tagIds != null && tagIds.size() > 0) {

				// at least one tag is within the tour

				fActionRemoveAllTags.setEnabled(true);
				fActionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				fActionRemoveAllTags.setEnabled(false);
				fActionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			fActionRemoveTag.setEnabled(isTourSelected);
			fActionRemoveAllTags.setEnabled(isTourSelected);
		}

		final boolean isTagSelected = tagItems > 0 && tourItems == 0 && categoryItems == 0 && otherItems == 0;
		final boolean isCategorySelected = categoryItems > 0 && tourItems == 0 && tagItems == 0 && otherItems == 0;

		final TVITagViewItem firstElement = (TVITagViewItem) selection.getFirstElement();
		final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();

		// enable rename action
		if (selectedItems == 1) {
			if (isTagSelected) {
				fActionRenameTag.setText(Messages.action_tag_rename_tag);
				fActionRenameTag.setEnabled(true);
			} else if (isCategorySelected) {
				fActionRenameTag.setText(Messages.action_tag_rename_tag_category);
				fActionRenameTag.setEnabled(true);

			} else {
				fActionRenameTag.setEnabled(false);
			}
		} else {
			fActionRenameTag.setEnabled(false);
		}

		/*
		 * tree expand type can be set if only tags are selected or when an item is selected which
		 * is not a category
		 */
		fActionSetTagStructure.setEnabled(isTagSelected || (items == 1 && categoryItems == 0));

		fActionExpandSelection.setEnabled(firstElement == null ? false : //
				selectedItems == 1 ? firstElementHasChildren : //
						true);

		fActionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);

		// enable/disable actions for the recent tags
		TagManager.enableRecentTagActions(isTourSelected);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionCollapseOthers);
		menuMgr.add(fActionExpandSelection);
		menuMgr.add(fActionCollapseAll);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionEditTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);

		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTagStructure);
		menuMgr.add(fActionSetAllTagStructures);
		menuMgr.add(fActionRenameTag);
		menuMgr.add(fActionOpenTagPrefs);
		
		enableActions();
	}

	private void fillToolBar() {
		/*
		 * action in the view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		// recreate the toolbar
		tbm.removeAll();

//		fDrillDownAdapter.addNavigationActions(tbm);

		tbm.add(fActionExpandSelection);
		tbm.add(fActionCollapseAll);

		tbm.add(fActionRefreshView);

		tbm.update(true);
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionSetLayoutFlat);
		menuMgr.add(fActionSetLayoutHierarchical);

		menuMgr.add(new Separator());
		menuMgr.add(fActionModifyColumns);

	}

	@SuppressWarnings("unchecked")//$NON-NLS-1$
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fTagViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTagViewer.getSelection());

		final TourManager tourManager = TourManager.getInstance();
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();
			if (treeItem instanceof TVITagViewTour) {

				final TourData tourData = tourManager.getTourData(((TVITagViewTour) treeItem).getTourId());

				if (tourData != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

	public ColumnViewer getViewer() {
		return fTagViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	public void recreateViewer() {

		final Object[] expandedElements = fTagViewer.getExpandedElements();

		fTagViewer.getTree().dispose();

		fViewerContainer.setRedraw(false);
		{
			createTagViewer(fViewerContainer);
			fViewerContainer.layout();
		}
		fViewerContainer.setRedraw(true);

		reloadViewer(expandedElements);
	}

	/**
	 * reload the content of the tag viewer
	 */
	public void reloadViewer() {

		final Object[] expandedElements = fTagViewer.getExpandedElements();

		reloadViewer(expandedElements);
	}

	private void reloadViewer(final Object[] expandedElements) {

		final Tree tree = fTagViewer.getTree();
		tree.setRedraw(false);
		{
			fTagViewer.setInput(fRootItem = new TVITagViewRoot(this, fTagViewLayout));
			fTagViewer.setExpandedElements(expandedElements);
		}
		tree.setRedraw(true);
	}

	private void restoreState(final IMemento memento) {

		fTagViewLayout = -1;

		if (memento != null) {

			/*
			 * restore states from the memento
			 */

			// restore view layout
			final Integer mementoViewLayout = memento.getInteger(MEMENTO_TAG_VIEW_LAYOUT);
			if (mementoViewLayout != null) {

				switch (mementoViewLayout) {

				case TAG_VIEW_LAYOUT_FLAT:

					fTagViewLayout = mementoViewLayout;
					fActionSetLayoutFlat.setChecked(true);
					break;

				case TAG_VIEW_LAYOUT_HIERARCHICAL:

					fTagViewLayout = mementoViewLayout;
					fActionSetLayoutHierarchical.setChecked(true);
					break;

				default:
					break;
				}
			}
		}

		// set default tag view layout
		if (fTagViewLayout == -1) {
			fTagViewLayout = TAG_VIEW_LAYOUT_FLAT;
			fActionSetLayoutFlat.setChecked(true);
		}
	}

	private void saveSettings() {
		if (fSessionMemento == null) {
			fSessionMemento = XMLMemento.createWriteRoot("TagView"); //$NON-NLS-1$
		}
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		fColumnManager.saveState(memento);

		// save view layout
		memento.putInteger(MEMENTO_TAG_VIEW_LAYOUT, fTagViewLayout);

//		final Object[] expandedElements = fTagViewer.getExpandedElements();
//		final Object[] visibleExpandedElements = fTagViewer.getVisibleExpandedElements();
//		final TreePath[] expandedTreePaths = fTagViewer.getExpandedTreePaths();
//		fTagViewer.setExpandedTreePaths(expandedTreePaths);
//		fTagViewer.setExpandedElements(expandedElements);
	}

	private void setCellColor(final ViewerCell cell, final Object element) {
		// set color
		if (element instanceof TVITagViewTag) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.TAG_COLOR));
		} else if (element instanceof TVITagViewYear) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.TAG_SUB_COLOR));
		} else if (element instanceof TVITagViewMonth) {
			cell.setForeground(JFaceResources.getColorRegistry().get(UI.TAG_SUB_SUB_COLOR));
		}
	}

	@Override
	public void setFocus() {

	}

	private void setTagViewTitle(final Object newInput) {

		String description = UI.EMPTY_STRING;

		if (newInput instanceof TVITagViewTag) {
			description = Messages.tag_view_title_tag + ((TVITagViewTag) newInput).getName();
		} else if (newInput instanceof TVITagViewTagCategory) {
			description = Messages.tag_view_title_tag_category + ((TVITagViewTagCategory) newInput).name;
		}

		setContentDescription(description);
	}

	public void setViewLayout(final int tagViewStructure) {
		fTagViewLayout = tagViewStructure;
		reloadViewer();
	}

	/**
	 * !!! Recursive !!! method to update the tags in the viewer, this method handles changes in the
	 * tag structure
	 * 
	 * @param rootItem
	 * @param changedTags
	 * @param isAddMode
	 */
	private void updateViewerAfterTagStructureIsModified(	final TreeViewerItem parentItem,
															final ChangedTags changedTags,
															final boolean isAddMode) {

		final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

		if (children == null) {
			return;
		}

		// loop: all children of the current parent item 
		for (final Object object : children) {

			if (object instanceof TVITagViewTag) {

				final TVITagViewTag tagItem = (TVITagViewTag) object;
				final long viewerTagId = tagItem.getTagId();

				final HashMap<Long, TourTag> modifiedTags = changedTags.getModifiedTags();
				final ArrayList<Long> removedIds = new ArrayList<Long>();

				for (final Long modifiedTagId : modifiedTags.keySet()) {
					if (viewerTagId == modifiedTagId.longValue()) {

						/*
						 * current tag was modified
						 */

						// add/remove tours from the tag
						tagItem.refresh(fTagViewer, changedTags.getModifiedTours(), changedTags.isAddMode());

						// update tag totals
						TVITagViewItem.readTagTotals(tagItem);

						// update viewer
						fTagViewer.refresh(tagItem);

						removedIds.add(modifiedTagId);
					}
				}

				/*
				 * modified tag id exists only once in the tree viewer, remove the id's outside of
				 * the foreach loop to avid the exception ConcurrentModificationException
				 */
				for (final Long removedId : removedIds) {
					modifiedTags.remove(removedId);
				}

				// optimize
				if (modifiedTags.size() == 0) {
					return;
				}

			} else {
				if (object instanceof TreeViewerItem) {
					updateViewerAfterTagStructureIsModified((TreeViewerItem) object, changedTags, isAddMode);
				}
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

		final ArrayList<TVITagViewTour> deletedTourItems = new ArrayList<TVITagViewTour>();

		// loop: all tree children items
		for (final Object object : parentChildren) {
			if (object instanceof TreeViewerItem) {

				final TreeViewerItem treeChildItem = (TreeViewerItem) object;
				if (treeChildItem instanceof TVITagViewTour) {

					final TVITagViewTour tourItem = (TVITagViewTour) treeChildItem;
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
					updateViewerAfterTourIsDeleted(treeChildItem, deletedTourIds);
				}
			}
		}

		if (deletedTourItems.size() > 0) {

			// update model
			parentChildren.removeAll(deletedTourItems);

			// update viewer
			fTagViewer.remove(deletedTourItems.toArray());
		}
	}

	/**
	 * !!!Recursive !!! update the data for all tour items
	 * 
	 * @param rootItem
	 * @param modifiedTours
	 */
	private void updateViewerAfterTourIsModified(	final TreeViewerItem parentItem,
													final ArrayList<TourData> modifiedTours) {

		final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

		if (children == null) {
			return;
		}

		// loop: all children
		for (final Object object : children) {
			if (object instanceof TreeViewerItem) {

				final TreeViewerItem treeItem = (TreeViewerItem) object;
				if (treeItem instanceof TVITagViewTour) {

					final TVITagViewTour tourItem = (TVITagViewTour) treeItem;
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

							tourItem.tagIds = tagIds = new ArrayList<Long>();
							for (final TourTag tourTag : tourTags) {
								tagIds.add(tourTag.getTagId());
							}

							// update item in the viewer
							fTagViewer.update(tourItem, null);

							// modified tour exists only once in the viewer, remove modified tour
//							modifiedTours.remove(modifiedTourData);

							break;
						}
					}

					// optimize
//					if (modifiedTours.size() == 0) {
//						return;
//					}

				} else {
					// update children
					updateViewerAfterTourIsModified(treeItem, modifiedTours);
				}
			}
		}
	}

}
