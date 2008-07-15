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
package net.tourbook.ui.views.tourCatalog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class CompareResultView extends ViewPart implements ITourViewer {

	public static final String					ID					= "net.tourbook.views.tourCatalog.CompareResultView";	//$NON-NLS-1$

//	public static final int						COLUMN_REF_TOUR				= 0;
//	public static final int						COLUMN_ALTITUDE_DIFFERENCE	= 1;
//	public static final int						COLUMN_SPEED_COMPUTED		= 2;
//	public static final int						COLUMN_SPEED_SAVED			= 3;															// speed saved in the database
//	public static final int						COLUMN_SPEED_MOVED			= 4;															// speed saved in the database
//	public static final int						COLUMN_DISTANCE				= 5;
//	public static final int						COLUMN_TIME_INTERVAL		= 6;

	/**
	 * This memento allows this view to save and restore state when it is closed and opened within a
	 * session. A different memento is supplied by the platform for persistance at workbench
	 * shutdown.
	 */
	private static IMemento						fSessionMemento		= null;

	private Composite							fViewerContainer;
	private CheckboxTreeViewer					fTourViewer;
	private TVICompareResultRootItem			fRootItem;

	private ISelectionListener					fPostSelectionListener;
	private IPartListener2						fPartListener;
	private IPropertyChangeListener				fPrefChangeListener;

	PostSelectionProvider						fPostSelectionProvider;

	private ActionSaveComparedTours				fActionSaveComparedTours;
	private ActionRemoveComparedTourSaveStatus	fActionRemoveComparedTourSaveStatus;
	private ActionCheckTours					fActionCheckTours;
	private ActionUncheckTours					fActionUncheckTours;
	private ActionModifyColumns					fActionModifyColumns;

	/**
	 * resource manager for images
	 */
	private Image								dbImage				= TourbookPlugin.getImageDescriptor(Messages.Image__database)
																			.createImage(true);

	private final NumberFormat					nf					= NumberFormat.getNumberInstance();

	private ITourPropertyListener				fCompareTourPropertyListener;

	private ColumnManager						fColumnManager;

	SelectionRemovedComparedTours				fOldRemoveSelection	= null;

//	private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {
//
//		public Color getBackground(final Object element) {
//			return null;
//		}
//
//		public Image getColumnImage(final Object element, final int columnIndex) {
//
//			if (element instanceof CompareResultItemComparedTour) {
//				final CompareResultItemComparedTour compTour = (CompareResultItemComparedTour) element;
//				if (columnIndex == COLUMN_REF_TOUR) {
//					if (compTour.compId != -1) {
//						return resManager.createImageWithDefault(dbImgDescriptor);
//					}
//				}
//			}
//
//			return null;
//		}
//
//		public String getColumnText(final Object obj, final int index) {
//
//			if (obj instanceof CompareResultItemReferenceTour) {
//
//				final CompareResultItemReferenceTour refTour = (CompareResultItemReferenceTour) obj;
//				if (index == 0) {
//					return refTour.label;
//				} else {
//					return ""; //$NON-NLS-1$
//				}
//
//			} else if (obj instanceof CompareResultItemComparedTour) {
//
//				final CompareResultItemComparedTour result = (CompareResultItemComparedTour) obj;
//
//				switch (index) {
//				case COLUMN_REF_TOUR:
//					return TourManager.getTourDate(result.comparedTourData);
//
//				case COLUMN_SPEED_COMPUTED:
//					nf.setMinimumFractionDigits(1);
//					nf.setMaximumFractionDigits(1);
//					return nf.format(result.compareSpeed / UI.UNIT_VALUE_DISTANCE);
//
//				case COLUMN_SPEED_SAVED:
//					final float speedSaved = result.dbSpeed;
//					if (speedSaved == 0) {
//						return ""; //$NON-NLS-1$
//					} else {
//						nf.setMinimumFractionDigits(1);
//						nf.setMaximumFractionDigits(1);
//						return nf.format(speedSaved / UI.UNIT_VALUE_DISTANCE);
//					}
//
//				case COLUMN_SPEED_MOVED:
//					final float speedMoved = result.movedSpeed;
//					if (speedMoved == 0) {
//						return ""; //$NON-NLS-1$
//					} else {
//						nf.setMinimumFractionDigits(1);
//						nf.setMaximumFractionDigits(1);
//						return nf.format(speedMoved / UI.UNIT_VALUE_DISTANCE);
//					}
//
//				case COLUMN_DISTANCE:
//					nf.setMinimumFractionDigits(2);
//					nf.setMaximumFractionDigits(2);
//					return nf.format((result.compareDistance) / (1000 * UI.UNIT_VALUE_DISTANCE));
//
//				case COLUMN_ALTITUDE_DIFFERENCE:
//					return Integer.toString(result.minAltitudeDiff
//							* 100
//							/ (result.normalizedEndIndex - result.normalizedStartIndex));
//
//				case COLUMN_TIME_INTERVAL:
//					return Integer.toString(result.timeIntervall);
//
//				}
//			}
//
//			return ""; //$NON-NLS-1$
//		}
//
//		public Color getForeground(final Object element) {
//			if (element instanceof CompareResultItemComparedTour) {
//				if (((CompareResultItemComparedTour) (element)).compId != -1) {
//					return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
//				}
//			}
//			return null;
//		}
//	}

	class ResultContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return fRootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Recursive method to walk down the tour tree items and find the compared tours
	 * 
	 * @param parentItem
	 * @param CompareIds
	 */
	private static void getComparedTours(	final ArrayList<TVICompareResultComparedTour> comparedTours,
											final TreeViewerItem parentItem,
											final ArrayList<Long> CompareIds) {

		final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

		if (unfetchedChildren != null) {

			// children are available

			for (final TreeViewerItem tourTreeItem : unfetchedChildren) {

				if (tourTreeItem instanceof TVICompareResultComparedTour) {
					final TVICompareResultComparedTour ttiCompResult = (TVICompareResultComparedTour) tourTreeItem;
					final long compId = ttiCompResult.compId;
					for (final Long removedCompId : CompareIds) {
						if (compId == removedCompId) {
							comparedTours.add(ttiCompResult);
						}
					}
				} else {
					// this is a child which can be the parent for other childs
					getComparedTours(comparedTours, tourTreeItem, CompareIds);
				}
			}
		}
	}

	public CompareResultView() {}

	private void addCompareTourPropertyListener() {

		fCompareTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					final long compareId = compareTourProperty.compareId;

					final ArrayList<Long> compareIds = new ArrayList<Long>();
					compareIds.add(compareId);

					if (compareId == -1) {

						// compare result is not saved

						final Object comparedTourItem = compareTourProperty.comparedTourItem;

						if (comparedTourItem instanceof TVICompareResultComparedTour) {
							final TVICompareResultComparedTour resultItem = (TVICompareResultComparedTour) comparedTourItem;

							resultItem.movedStartIndex = compareTourProperty.startIndex;
							resultItem.movedEndIndex = compareTourProperty.endIndex;
							resultItem.movedSpeed = compareTourProperty.speed;

							// update viewer
							fTourViewer.update(comparedTourItem, null);
						}

					} else {

						// compare result is saved

						// find compared tour in the viewer
						final ArrayList<TVICompareResultComparedTour> comparedTours = new ArrayList<TVICompareResultComparedTour>();
						getComparedTours(comparedTours, fRootItem, compareIds);

						if (comparedTours.size() > 0) {

							final TVICompareResultComparedTour compareTourItem = comparedTours.get(0);

							if (compareTourProperty.isDataSaved) {

								// compared tour was saved

								compareTourItem.dbStartIndex = compareTourProperty.startIndex;
								compareTourItem.dbEndIndex = compareTourProperty.endIndex;
								compareTourItem.dbSpeed = compareTourProperty.speed;

							} else {

								compareTourItem.movedStartIndex = compareTourProperty.startIndex;
								compareTourItem.movedEndIndex = compareTourProperty.endIndex;
								compareTourItem.movedSpeed = compareTourProperty.speed;
							}

							// update viewer
							fTourViewer.update(compareTourItem, null);
						}
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fCompareTourPropertyListener);
	}

	/**
	 * set the part listener to save the view settings, the listeners are called before the controls
	 * are disposed
	 */
	private void addPartListeners() {

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

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

// ORIGINAL 
//					// measurement system has changed
//
//					UI.updateUnits();
//
//					saveSettings();
//
//					// dispose viewer
//					final Control[] children = fViewerContainer.getChildren();
//					for (int childIndex = 0; childIndex < children.length; childIndex++) {
//						children[childIndex].dispose();
//					}
//
//					createTourViewer(fViewerContainer);
//					fViewerContainer.layout();
//
//					// update the viewer
//					fTourViewer.setInput(((ResultContentProvider) fTourViewer.getContentProvider()).getRootItem());
//
//					restoreState(fSessionMemento);

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fSessionMemento);

					fColumnManager.resetColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer();

				} else if (property.equals(ITourbookPreferences.TAG_COLOR_AND_LAYOUT_CHANGED)) {

					fTourViewer.getTree()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.TAG_VIEW_SHOW_LINES));

					fTourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTourViewer.getTree().redraw();
				}
			}
		};
		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * Listen to post selections
	 */
	private void addSelectionListeners() {

		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionRemovedComparedTours) {

					removeComparedToursFromViewer(selection);

				} else if (selection instanceof SelectionPersistedCompareResults) {

					final SelectionPersistedCompareResults selectionPersisted = (SelectionPersistedCompareResults) selection;

					final ArrayList<TVICompareResultComparedTour> persistedCompareResults = selectionPersisted.persistedCompareResults;

					if (persistedCompareResults.size() > 0) {

						final TVICompareResultComparedTour comparedTourItem = persistedCompareResults.get(0);

						// uncheck persisted tours
						fTourViewer.setChecked(comparedTourItem, false);

						// update changed item
						fTourViewer.update(comparedTourItem, null);

					}
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);

	}

	private void createActions() {

		fActionSaveComparedTours = new ActionSaveComparedTours(this);
		fActionRemoveComparedTourSaveStatus = new ActionRemoveComparedTourSaveStatus(this);
		fActionCheckTours = new ActionCheckTours(this);
		fActionUncheckTours = new ActionUncheckTours(this);

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
				CompareResultView.this.fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		final Control tourViewer = fTourViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns for the viewer
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);

		addPartListeners();
		addSelectionListeners();
		addCompareTourPropertyListener();
		addPrefListener();

		createActions();
		fillViewMenu();

		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fTourViewer.setInput(fRootItem = new TVICompareResultRootItem());

		restoreState(fSessionMemento);
	}

	private Control createTourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION
				| SWT.CHECK);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(TourbookPlugin.getDefault()
				.getPluginPreferences()
				.getBoolean(ITourbookPreferences.TAG_VIEW_SHOW_LINES));

		fTourViewer = new ContainerCheckedTreeViewer(tree);
		fColumnManager.createColumns();

		fTourViewer.setContentProvider(new ResultContentProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

				if (obj1 instanceof TVICompareResultComparedTour) {
					return ((TVICompareResultComparedTour) obj1).minAltitudeDiff
							- ((TVICompareResultComparedTour) obj2).minAltitudeDiff;
				}

				return 0;
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectionChanged(event);
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				// expand/collapse current item

				final Object treeItem = ((IStructuredSelection) event.getSelection()).getFirstElement();

				if (fTourViewer.getExpandedState(treeItem)) {
					fTourViewer.collapseToLevel(treeItem, 1);
				} else {
					fTourViewer.expandToLevel(treeItem, 1);
				}
			}
		});

		fTourViewer.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent keyEvent) {
				if (keyEvent.keyCode == SWT.DEL) {
					removeComparedTourFromDb();
				}
			}
		});

		fTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				if (event.getElement() instanceof TVICompareResultComparedTour) {
					final TVICompareResultComparedTour compareResult = (TVICompareResultComparedTour) event.getElement();
					if (event.getChecked() && compareResult.compId != -1) {
						/*
						 * uncheck elements which are already stored for the reftour, it would be
						 * better to disable them, but this is not possible because this is a
						 * limitation by the OS
						 */
						fTourViewer.setChecked(compareResult, false);
					} else {
						enableActions();
					}
				} else {
					// uncheck all other tree items
					fTourViewer.setChecked(event.getElement(), false);
				}
			}
		});

		createContextMenu();

		return tree;
	}

	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * tree column: reference tour/date
		 */
		colDef = new TreeColumnDefinition("comparedTour", SWT.LEAD); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnLabel(Messages.Compare_Result_Column_tour);
		colDef.setColumnText(Messages.Compare_Result_Column_tour);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(25) + 16);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();

				if (element instanceof TVICompareResultReferenceTour) {

					final TVICompareResultReferenceTour refItem = (TVICompareResultReferenceTour) element;
					cell.setText(refItem.label);

				} else if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;
					cell.setText(TourManager.getTourDate(compareItem.comparedTourData));

					// display an image when a tour is saved
					if (compareItem.compId != -1) {
						cell.setImage(dbImage);
					} else {
						cell.setImage(null);
					}
				}

				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude difference
		 */
		colDef = new TreeColumnDefinition("diff", SWT.TRAIL); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnText(Messages.Compare_Result_Column_diff);
		colDef.setColumnToolTipText(Messages.Compare_Result_Column_diff_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_diff_label);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					cell.setText(Integer.toString((compareItem.minAltitudeDiff * 100)
							/ (compareItem.normalizedEndIndex - compareItem.normalizedStartIndex)));

					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: speed computed
		 */
		/*
		 * column: altitude difference
		 */
		colDef = new TreeColumnDefinition("computedSpeed", SWT.TRAIL); //$NON-NLS-1$
		fColumnManager.addColumn(colDef);

		colDef.setColumnText(UI.UNIT_LABEL_SPEED);
		colDef.setColumnToolTipText(Messages.Compare_Result_Column_kmh_tooltip);
		colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_label);
		colDef.setColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour compareItem = (TVICompareResultComparedTour) element;

					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);
					cell.setText(nf.format(compareItem.compareSpeed / UI.UNIT_VALUE_DISTANCE));
					setCellColor(cell, element);
				}
			}
		});
//
//
//
//
//		/*
//		 * column: speed saved
//		 */
//		tc = new TreeColumn(parent, SWT.TRAIL);
//		tc.setText(UI.UNIT_LABEL_SPEED);
//		tc.setToolTipText(Messages.Compare_Result_Column_kmh_db_tooltip);
//		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
//
//		/*
//		 * column: speed moved
//		 */
//		tc = new TreeColumn(parent, SWT.TRAIL);
//		tc.setText(UI.UNIT_LABEL_SPEED);
//		tc.setToolTipText(Messages.Compare_Result_Column_kmh_moved_tooltip);
//		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
//
//		/*
//		 * column: distance
//		 */
//		tc = new TreeColumn(parent, SWT.TRAIL);
//		tc.setText(UI.UNIT_LABEL_DISTANCE);
//		tc.setToolTipText(Messages.Compare_Result_Column_km_tooltip);
//		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
//
//		/*
//		 * column: time interval
//		 */
//		tc = new TreeColumn(parent, SWT.TRAIL);
//		tc.setText(Messages.Compare_Result_time_interval);
//		tc.setToolTipText(Messages.Compare_Result_time_interval_tooltip);
//		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		/*
		 * column: tour type
		 */
		colDef = TreeColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICompareResultComparedTour) {
					cell.setImage(UI.getInstance()
							.getTourTypeImage(((TVICompareResultComparedTour) element).comparedTourData.getTourType()
									.getTypeId()));
				}
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
				if (element instanceof TVICompareResultComparedTour) {
					cell.setText(((TVICompareResultComparedTour) element).comparedTourData.getTourTitle());
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
				if (element instanceof TVICompareResultComparedTour) {

					final Set<TourTag> tourTags = ((TVICompareResultComparedTour) element).comparedTourData.getTourTags();
					if (tourTags.size() > 0) {

						// convert the tags into a list of tag ids 
						final ArrayList<Long> tagIds = new ArrayList<Long>();
						for (final TourTag tourTag : tourTags) {
							tagIds.add(tourTag.getTagId());
						}

						cell.setText(TourDatabase.getTagNames(tagIds));
					}
				}
			}
		});

	}

	//	} else if (obj instanceof CompareResultItemComparedTour) {
	//
//					final CompareResultItemComparedTour result = (CompareResultItemComparedTour) obj;
	//
//					switch (index) {
//					case COLUMN_REF_TOUR:
//						return TourManager.getTourDate(result.comparedTourData);
	//
//					case COLUMN_SPEED_COMPUTED:
//						nf.setMinimumFractionDigits(1);
//						nf.setMaximumFractionDigits(1);
//						return nf.format(result.compareSpeed / UI.UNIT_VALUE_DISTANCE);
	//
//					case COLUMN_SPEED_SAVED:
//						final float speedSaved = result.dbSpeed;
//						if (speedSaved == 0) {
//							return ""; //$NON-NLS-1$
//						} else {
//							nf.setMinimumFractionDigits(1);
//							nf.setMaximumFractionDigits(1);
//							return nf.format(speedSaved / UI.UNIT_VALUE_DISTANCE);
//						}
	//
//					case COLUMN_SPEED_MOVED:
//						final float speedMoved = result.movedSpeed;
//						if (speedMoved == 0) {
//							return ""; //$NON-NLS-1$
//						} else {
//							nf.setMinimumFractionDigits(1);
//							nf.setMaximumFractionDigits(1);
//							return nf.format(speedMoved / UI.UNIT_VALUE_DISTANCE);
//						}
	//
//					case COLUMN_DISTANCE:
//						nf.setMinimumFractionDigits(2);
//						nf.setMaximumFractionDigits(2);
//						return nf.format((result.compareDistance) / (1000 * UI.UNIT_VALUE_DISTANCE));
	//
//					case COLUMN_ALTITUDE_DIFFERENCE:
//						return Integer.toString(result.minAltitudeDiff
//								* 100
//								/ (result.normalizedEndIndex - result.normalizedStartIndex));
	//
//					case COLUMN_TIME_INTERVAL:
//						return Integer.toString(result.timeIntervall);
	//
//					}
	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getSite().getPage().removePartListener(fPartListener);
		TourManager.getInstance().removePropertyListener(fCompareTourPropertyListener);
		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		dbImage.dispose();

		super.dispose();
	}

	private void enableActions() {

		// enable/disable save button
		fActionSaveComparedTours.setEnabled(fTourViewer.getCheckedElements().length > 0);

		// enable/disable action: remove save status
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		/*
		 * currently we only support one tour item were the save status can be removed
		 */
		if (selection.size() == 1 && selection.getFirstElement() instanceof TVICompareResultComparedTour) {

			final TVICompareResultComparedTour tviCompResult = (TVICompareResultComparedTour) (selection.getFirstElement());

			fActionRemoveComparedTourSaveStatus.setEnabled(tviCompResult.compId != -1);
		} else {
			fActionRemoveComparedTourSaveStatus.setEnabled(false);
		}
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionSaveComparedTours);

		menuMgr.add(fActionCheckTours);
		menuMgr.add(fActionUncheckTours);

		menuMgr.add(new Separator());
		menuMgr.add(fActionRemoveComparedTourSaveStatus);

		enableActions();
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionModifyColumns);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	/**
	 * @return Returns the tour viewer
	 */
	public CheckboxTreeViewer getViewer() {
		return fTourViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's net yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void onSelectionChanged(final SelectionChangedEvent event) {

		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		final Object treeItem = selection.getFirstElement();

		if (treeItem instanceof TVICompareResultReferenceTour) {

			final TVICompareResultReferenceTour refItem = (TVICompareResultReferenceTour) treeItem;

			fPostSelectionProvider.setSelection(new SelectionTourCatalogView(refItem.refTour.getRefId()));

		} else if (treeItem instanceof TVICompareResultComparedTour) {

			final TVICompareResultComparedTour resultItem = (TVICompareResultComparedTour) treeItem;

			fPostSelectionProvider.setSelection(new StructuredSelection(resultItem));
		}
	}

	public void recreateViewer() {

		final Object[] expandedElements = fTourViewer.getExpandedElements();
		final ISelection selection = fTourViewer.getSelection();

		fViewerContainer.setRedraw(false);
		{
			fTourViewer.getTree().dispose();

			createTourViewer(fViewerContainer);
			fViewerContainer.layout();

			fTourViewer.setInput(fRootItem = new TVICompareResultRootItem());

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		fViewerContainer.setRedraw(true);
	}

	public void reloadViewer() {

		final Tree tree = fTourViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = fTourViewer.getExpandedElements();
			final ISelection selection = fTourViewer.getSelection();

			fTourViewer.setInput(fRootItem = new TVICompareResultRootItem());

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		tree.setRedraw(true);
	}

	/**
	 * Remove compared tour from the database
	 */
	void removeComparedTourFromDb() {

		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		/*
		 * currently only one tour is supported to remove the save status
		 */
		if (selection.size() == 1 && selection.getFirstElement() instanceof TVICompareResultComparedTour) {

			final TVICompareResultComparedTour compareResult = (TVICompareResultComparedTour) selection.getFirstElement();

			if (TourCompareManager.removeComparedTourFromDb(compareResult.compId)) {

				// update tour catalog view
				final SelectionRemovedComparedTours removedCompareTours = new SelectionRemovedComparedTours();
				removedCompareTours.removedComparedTours.add(compareResult.compId);

				fPostSelectionProvider.setSelection(removedCompareTours);
			}
		}
	}

	private void removeComparedToursFromViewer(final ISelection selection) {

		final SelectionRemovedComparedTours tourSelection = (SelectionRemovedComparedTours) selection;
		final ArrayList<Long> removedTourCompareIds = tourSelection.removedComparedTours;

		/*
		 * return when there are no removed tours or when the selection has not changed
		 */
		if (removedTourCompareIds.size() == 0 || tourSelection == fOldRemoveSelection) {
			return;
		}

		fOldRemoveSelection = tourSelection;

		/*
		 * find/update the removed compared tours in the viewer
		 */

		final ArrayList<TVICompareResultComparedTour> comparedTours = new ArrayList<TVICompareResultComparedTour>();

		getComparedTours(comparedTours, fRootItem, removedTourCompareIds);

		// reset entity for the removed compared tours
		for (final TVICompareResultComparedTour removedCompTour : comparedTours) {

			removedCompTour.compId = -1;

			removedCompTour.dbStartIndex = -1;
			removedCompTour.dbEndIndex = -1;
			removedCompTour.dbSpeed = 0;

			removedCompTour.movedStartIndex = -1;
			removedCompTour.movedEndIndex = -1;
			removedCompTour.movedSpeed = 0;
		}

		// update viewer
		fTourViewer.update(comparedTours.toArray(), null);
	}

	private void restoreState(final IMemento memento) {

		if (memento != null) {

		}
	}

	/**
	 * Persist the compared tours which are checked in the viewer
	 */
	void saveCheckedTours() {

		final Object[] checkedTours = fTourViewer.getCheckedElements();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final EntityTransaction ts = em.getTransaction();

			try {

				final SelectionPersistedCompareResults persistedCompareResults = new SelectionPersistedCompareResults();

				for (final Object checkedTour : checkedTours) {
					if (checkedTour instanceof TVICompareResultComparedTour) {

						final TVICompareResultComparedTour comparedTourItem = (TVICompareResultComparedTour) checkedTour;

						TourCompareManager.saveComparedTourItem(comparedTourItem, em, ts);

						// uncheck the compared tour and make the persisted instance visible
						fTourViewer.setChecked(comparedTourItem, false);

						persistedCompareResults.persistedCompareResults.add(comparedTourItem);
					}
				}

				// uncheck/disable the persisted tours
				fTourViewer.update(checkedTours, null);

				// update tour map view
				fPostSelectionProvider.setSelection(persistedCompareResults);

			} catch (final Exception e) {
				e.printStackTrace();
			} finally {
				if (ts.isActive()) {
					ts.rollback();
				}
				em.close();
			}
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("CompareResultView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		fColumnManager.saveState(memento);
	}

	private void setCellColor(final ViewerCell cell, final Object element) {

		if (element instanceof TVICompareResultReferenceTour) {

			cell.setForeground(JFaceResources.getColorRegistry().get(UI.TAG_COLOR));

		} else if (element instanceof TVICompareResultComparedTour) {

			// show the saved tours in a different color

			if (((TVICompareResultComparedTour) (element)).compId != -1) {
				cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
			} else {
				// show the text in the default color
				cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			}
		}
	}

	@Override
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	/**
	 * Update the viewer by providing new data
	 */
	public void updateViewer() {
		fTourViewer.setInput(fRootItem = new TVICompareResultRootItem());
	}

}
