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
package net.tourbook.ui.views.tourDataEditor;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ITourEditor;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.ITourSaveListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEditorInput;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperties;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.MessageManager;
import net.tourbook.ui.TableColumnDefinition;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 24.08.2007

public class TourDataEditorView extends ViewPart implements ISaveablePart2, ITourViewer, ISelectedTours, ITourEditor {

	public static final String			ID						= "net.tourbook.views.TourDataEditorView";	//$NON-NLS-1$

	private static final String			WIDGET_KEY				= "widgetKey";								//$NON-NLS-1$
	private static final String			WIDGET_KEY_TOURDISTANCE	= "tourDistance";							//$NON-NLS-1$
	private static final String			WIDGET_KEY_PERSON		= "tourPerson";							//$NON-NLS-1$

	private static final int			BUSY_INDICATOR_ITEMS	= 5000;

	private static final String			MEMENTO_SELECTED_TAB	= "tourProperties.selectedTab";			//$NON-NLS-1$
	private static final String			MEMENTO_ROW_EDIT_MODE	= "tourProperties.editMode";				//$NON-NLS-1$

	private static final int			TAB_INDEX_TITLE			= 0;
	private static final int			TAB_INDEX_INFO			= 1;
	private static final int			TAB_INDEX_TIME_SLICES	= 2;

	private static IMemento				fSessionMemento;

	/*
	 * data series which are displayed in the viewer
	 */
	private int[]						fSerieTime;

	private int[]						fSerieDistance;

	private int[]						fSerieAltitude;
	private int[]						fSerieTemperature;
	private int[]						fSerieCadence;
	private int[]						fSerieGradient;
	private int[]						fSerieSpeed;
	private int[]						fSeriePace;
	private int[]						fSeriePower;
	private int[]						fSeriePulse;
	private double[]					fSerieLatitude;
	private double[]					fSerieLongitude;

	private TableColumnDefinition		fColDefAltitude;
	private TableColumnDefinition		fColDefCadence;
	private TableColumnDefinition		fColDefPulse;
	private TableColumnDefinition		fColDefTemperature;
	private TableColumnDefinition		fColDefLongitude;
	private TableColumnDefinition		fColDefLatitude;

	private ActionModifyColumns			fActionModifyColumns;
	private ActionSaveTour				fActionSaveTour;
	private ActionUndoChanges			fActionUndoChanges;
	private ActionToggleRowSelectMode	fActionToggleRowSelectMode;

	private ActionSetTourTag			fActionAddTag;
	private ActionSetTourTag			fActionRemoveTag;
	private ActionRemoveAllTags			fActionRemoveAllTags;

	private ActionOpenPrefDialog		fActionOpenTagPrefs;
	private ActionOpenPrefDialog		fActionOpenTourTypePrefs;

	private PageBook					fPageBook;
	private Label						fPageNoTour;
	private Form						fEditorForm;
	private CTabFolder					fTabFolder;

	/**
	 * contains the tour editor when the tour is opened by an editor or <code>null</code> when the
	 * source of the tour is not from an editor
	 */
	private TourEditor					fTourEditor;
	private TourChart					fTourChart;
	private TourData					fTourData;

	private Composite					fDataContainer;
	private Composite					fInfoContainer;

	private Composite					fDataViewerContainer;
	private TableViewer					fDataViewer;

	/**
	 * items which are displayed in the time slice vierer
	 */
	public Object[]						fDataViewerItems;

	private ColumnManager				fColumnManager;

	private Text						fTextTitle;
	private Text						fTextDescription;

	private DateTime					fDtTourDate;
	private DateTime					fDtStartTime;

	private DateTime					fDtRecordingTime;
	private DateTime					fDtDrivingTime;
	private DateTime					fDtPausedTime;

	private Label						fLblDatapoints;
	private Label						fLblDeviceName;

	private Text						fTextStartLocation;
	private Text						fTextEndLocation;

	private Text						fTextTourDistance;
	private Label						fLblTourDistanceUnit;

	private Link						fTagLink;
	private Label						fLblTourTags;
	private Link						fTourTypeLink;
	private CLabel						fLblTourType;

//	private Label						fLblWeekDay;
	private Label						fLblRefTour;

	private Label						fTimeSliceLabel;

	private MessageManager				fMessageManager;

	private PostSelectionProvider		fPostSelectionProvider;
	private ISelectionListener			fPostSelectionListener;
	private IPartListener2				fPartListener;
	private IPropertyChangeListener		fPrefChangeListener;
	private ITourPropertyListener		fTourPropertyListener;
	private ITourSaveListener			fTourSaveListener;

	private Calendar					fCalendar				= GregorianCalendar.getInstance();
	private NumberFormat				fNumberFormatter		= NumberFormat.getNumberInstance();

	/**
	 * <code>true</code>: rows can be selected in the viewer<br>
	 * <code>false</code>: cell can be selected in the viewer
	 */
	private boolean						fIsRowEditMode			= true;

	private long						fPostReloadViewerTourId;

	private boolean						fIsTourDirty			= false;

	/**
	 * <code>true</code> when the tour is currently being saved
	 */
	private boolean						fIsSavingInProgress		= false;

	/**
	 * <code>true</code> will not make the tour dirty when data are loaded into the fields, modify
	 * event for the fields will be disabled
	 */
	private boolean						fDisableModifyEvent		= false;

	private Long						fSelectionTourId;

	private KeyAdapter					fKeyListener;
	private ModifyListener				fModifyListener;
	private ModifyListener				fVerifyFloatValue;
	private SelectionAdapter			fTourTimeListener;

	private PixelConverter				fPixelConverter;

	/**
	 * this width is used as a hint for the width of the description field, this value also
	 * influences the width of the columns in this editor
	 */
	private int							fTextColumnWidth		= 150;

	/**
	 * is <code>true</code> when {@link #fTourChart} contains reference tours
	 */
	private boolean						fIsReferenceTourAvailable;

	private Label						fLblPerson;

	private Composite					fTimeSliceContainer;

	/**
	 * range for the reference tours, is <code>null</code> when reference tours are not available,
	 * 1st index = ref tour, 2nd index: 0:start, 1:end
	 */
	private int[][]						fRefTourRange;

	private ActionDeleteTimeSlices		fActionDeleteTimeSlices;

	private SelectionAdapter			fDateTimeListener;

	private final class IntegerEditingSupport extends IntegerDataSerieEditingSupport {

		private final TextCellEditor	fCellEditor;

		private IntegerEditingSupport(final TextCellEditor cellEditor, final int[] dataSerie) {
			super(fDataViewer);
			fCellEditor = cellEditor;
			fDataSerie = dataSerie;
		}

		@Override
		protected boolean canEdit(final Object element) {
			if (fDataSerie == null || isTourInDb() == false) {
				return false;
			}
			return true;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return fCellEditor;
		}

		@Override
		protected Object getValue(final Object element) {
			final TimeSlice timeSlice = (TimeSlice) element;
			return new Integer(fDataSerie[timeSlice.serieIndex]).toString();
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				try {
					final TimeSlice timeSlice = (TimeSlice) element;
					final int newValue = Integer.parseInt((String) value);

					if (newValue != fDataSerie[timeSlice.serieIndex]) {

						// value has changed

						setTourDirty();

						fDataSerie[timeSlice.serieIndex] = newValue;

						updateTimeSlices();
					}

				} catch (final Exception e) {
					// ignore invalid characters
				}
			}
		}
	}

	private class TimeSlice {

		int			serieIndex;

		/*
		 * uniqueCreateIndex is required because changing the serieIndex when items are remove from
		 * the viewer fails because table item widgets are disposed and the viewer trys to select
		 * them
		 */
		private int	uniqueCreateIndex;

		public TimeSlice(final int serieIndex) {
			uniqueCreateIndex = this.serieIndex = serieIndex;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof TimeSlice)) {
				return false;
			}
			final TimeSlice other = (TimeSlice) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (uniqueCreateIndex != other.uniqueCreateIndex) {
				return false;
			}
			return true;
		}

		private TourDataEditorView getOuterType() {
			return TourDataEditorView.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + uniqueCreateIndex;
			return result;
		}

		@Override
		public String toString() {

			final StringBuilder sb = new StringBuilder();

			sb.append("createIndex:\t");//$NON-NLS-1$
			sb.append(uniqueCreateIndex);
			sb.append("\t\tdataIndex:\t");//$NON-NLS-1$
			sb.append(serieIndex);

			return sb.toString();
		}
	}

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return fDataViewerItems;
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	/**
	 * delete selected time slices
	 */
	void actionDeleteTimeSlices() {

		// a tour with reference tours is currently not supported 
		if (fIsReferenceTourAvailable) {
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_delete_rows_title,
					Messages.tour_editor_dlg_delete_rows_message);
			return;
		}

		// get selected time slices
		final StructuredSelection selection = (StructuredSelection) fDataViewer.getSelection();
		if (selection.size() == 0) {
			return;
		}

		// run this task synchronized

		final Object[] selectedTimeSlices = selection.toArray();

		/*
		 * check if time slices have a successive selection
		 */
		int lastIndex = -1;
		int firstIndex = -1;

		for (final Object selectedItem : selectedTimeSlices) {

			final TimeSlice timeSlice = (TimeSlice) selectedItem;

			if (lastIndex == -1) {

				// first slice

				firstIndex = lastIndex = timeSlice.serieIndex;

			} else {

				// 2...n slices

				if (lastIndex - timeSlice.serieIndex == -1) {

					// successive selection

					lastIndex = timeSlice.serieIndex;

				} else {

					MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
							Messages.tour_editor_dlg_delete_rows_title,
							Messages.tour_editor_dlg_delete_rows_not_successive);
					return;
				}
			}
		}

		/*
		 * get last selection index to select a time slice after removal
		 */
		final Table table = (Table) fDataViewer.getControl();
		int lastSelectionIndex = table.getSelectionIndex();

		/*
		 * update data series
		 */
		int[] intSerie = fTourData.altitudeSerie;
		if (intSerie != null) {
			fTourData.altitudeSerie = getRemainingIntegerSerieData(intSerie, firstIndex, lastIndex);
		}
		intSerie = fTourData.cadenceSerie;
		if (intSerie != null) {
			fTourData.cadenceSerie = getRemainingIntegerSerieData(intSerie, firstIndex, lastIndex);
		}
		intSerie = fTourData.distanceSerie;
		if (intSerie != null) {
			fTourData.distanceSerie = getRemainingIntegerSerieData(intSerie, firstIndex, lastIndex);
		}
		intSerie = fTourData.pulseSerie;
		if (intSerie != null) {
			fTourData.pulseSerie = getRemainingIntegerSerieData(intSerie, firstIndex, lastIndex);
		}
		intSerie = fTourData.temperatureSerie;
		if (intSerie != null) {
			fTourData.temperatureSerie = getRemainingIntegerSerieData(intSerie, firstIndex, lastIndex);
		}
		intSerie = fTourData.timeSerie;
		if (intSerie != null) {
			fTourData.timeSerie = getRemainingIntegerSerieData(intSerie, firstIndex, lastIndex);
		}

		double[] doubleSerie = fTourData.latitudeSerie;
		if (doubleSerie != null) {
			fTourData.latitudeSerie = getRemainingDoubleSerieData(doubleSerie, firstIndex, lastIndex);
		}
		doubleSerie = fTourData.longitudeSerie;
		if (doubleSerie != null) {
			fTourData.longitudeSerie = getRemainingDoubleSerieData(doubleSerie, firstIndex, lastIndex);
		}

		// reset computed data series
		fTourData.clearComputedSeries();

		updateDataSeriesFromTourData();

//		System.out.println();
//		System.out.println("before removel");
//		dumpViewerItems();

		// update viewer items and adjust serie index
		fDataViewerItems = getRemainingViewerItems(fDataViewerItems, firstIndex, lastIndex);

//		System.out.println();
//		System.out.println("after removel");
//		dumpViewerItems();

		fDataViewer.getControl().setRedraw(false);
		{
			// update viewer
			fDataViewer.remove(selectedTimeSlices);

			// update serie index label
			fDataViewer.refresh(true);
		}
		fDataViewer.getControl().setRedraw(true);

		/*
		 * select next available time slice
		 */
		final int itemCount = table.getItemCount();
		if (itemCount > 0) {

			lastSelectionIndex = Math.max(0, Math.min(lastSelectionIndex, itemCount - 1));

			table.setSelection(lastSelectionIndex);
			table.showSelection();
			
//			fireSliderPosition();
		}

		setTourDirty();

		/*
		 * notify other viewers
		 */
		fireModifyNotification();
	}

	void actionSaveTour() {

		// action is enabled when the tour is modified

		saveTourWithoutConfirmation();
	}

	void actionToggleEditMode() {

		fIsRowEditMode = fActionToggleRowSelectMode.isChecked();

		recreateViewer();
	}

	void actionUndoChanges() {

		if (confirmUndoChanges()) {
			discardModifications();
		}
	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {

				/*
				 * fire a selection when this editor shows another tour as the currently selected
				 * tour in another part. This happens when the tour is modified, another tour is
				 * selected and saving this tour is canceled
				 */

				if (fTourData == null || fSelectionTourId == null) {
					return;
				}

				if (fTourData.getTourId().longValue() != fSelectionTourId.longValue()) {
					fPostSelectionProvider.setSelection(new SelectionTourData(null, fTourData, true));
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {

					// keep settings for this part
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		// register the listener in the page
		getSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				if (fTourData == null) {
					return;
				}

				final String property = event.getProperty();
				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					/*
					 * tour data could have been changed but the changes are not reflected in the
					 * data model, the model needs to be updated from the UI
					 */
					if (isTourValid()) {
						updateTourDataFromUI();
					} else {
						MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
								Messages.tour_editor_dlg_discard_tour_title,
								Messages.tour_editor_dlg_discard_tour_message);
						discardModifications();
					}

					fDisableModifyEvent = true;
					{
						if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

							// measurement system has changed

							UI.updateUnits();

							fColumnManager.saveState(fSessionMemento);
							fColumnManager.clearColumns();
							defineViewerColumns(fDataViewerContainer);

							recreateViewer();
							updateUITabData();

						} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

							// reload tour data

							updateUIFromTourData(fTourData, false);
						}
					}
					fDisableModifyEvent = false;
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourDataEditorView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part, final int propertyId, final Object propertyData) {

				if (fTourData == null || part == TourDataEditorView.this) {
					return;
				}

				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED && propertyData instanceof TourProperties) {

					final TourProperties tourProperties = (TourProperties) propertyData;
					final ArrayList<TourData> modifiedTours = tourProperties.modifiedTours;
					if (modifiedTours != null) {

						// get modified tours

						final long viewTourId = fTourData.getTourId();

						// update modified tour
						for (final TourData tourData : modifiedTours) {
							if (tourData.getTourId() == viewTourId) {

								if (tourProperties.isReverted) {
									setTourClean();
								}

								updateUIFromTourData(tourData, true);
								setTourDirty();
								return;
							}
						}
					}

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					fDisableModifyEvent = true;
					{
						updateUIFromTourData(fTourData, false);
					}
					fDisableModifyEvent = false;
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void addTourSaveListener() {

		fTourSaveListener = new ITourSaveListener() {
			public boolean saveTour() {
				return TourDataEditorView.this.saveTourData();
			}
		};

		TourManager.getInstance().addTourSaveListener(fTourSaveListener);
	}

	private boolean confirmUndoChanges() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		if (store.getBoolean(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR)) {

			return true;

		} else {
			final MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(Display.getCurrent()
					.getActiveShell(),//
					Messages.tour_editor_dlg_revert_tour_title, // title
					Messages.tour_editor_dlg_revert_tour_message, // message
					Messages.tour_editor_dlg_revert_tour_toggle_message, // toggle message
					false, // toggle default state
					null,
					null);

			store.setValue(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR, dialog.getToggleState());

			return dialog.getReturnCode() == Window.OK;
		}
	}

	private void createActions() {

		fActionModifyColumns = new ActionModifyColumns(this);

		fActionSaveTour = new ActionSaveTour(this);
		fActionUndoChanges = new ActionUndoChanges(this);
		fActionToggleRowSelectMode = new ActionToggleRowSelectMode(this);

		fActionDeleteTimeSlices = new ActionDeleteTimeSlices(this);

		fActionAddTag = new ActionSetTourTag(this, true, false);
		fActionRemoveTag = new ActionSetTourTag(this, false, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this, false);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fActionOpenTourTypePrefs = new ActionOpenPrefDialog(Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionSaveTour);
		tbm.add(fActionUndoChanges);

		tbm.add(new Separator());
		tbm.add(fActionToggleRowSelectMode);

		tbm.update(true);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionModifyColumns);
	}

	/**
	 * @param parent
	 */
	private void createDataViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		createDataViewerContextMenu(table);

//		table.addTraverseListener(new TraverseListener() {
//			public void keyTraversed(final TraverseEvent e) {
//				e.doit = e.keyCode != SWT.CR; // vetoes all CR traversals
//			}
//		});

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					actionDeleteTimeSlices();
				}
			}
		});

		fDataViewer = new TableViewer(table);

		if (fIsRowEditMode == false) {

			/*
			 * initialize cell editing
			 */
			final TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(fDataViewer,
					new FocusCellOwnerDrawHighlighter(fDataViewer));

			final ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(fDataViewer) {
				@Override
				protected boolean isEditorActivationEvent(final ColumnViewerEditorActivationEvent event) {
					return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
							|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
							|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
							|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
				}
			};

			TableViewerEditor.create(fDataViewer, //
					focusCellManager,
					actSupport,
					ColumnViewerEditor.TABBING_HORIZONTAL
							| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
							| ColumnViewerEditor.TABBING_VERTICAL
							| ColumnViewerEditor.KEYBOARD_ACTIVATION);
		}

		/*
		 * create editing support after the viewer is created but before the columns are created
		 */
		final TextCellEditor cellEditor = new TextCellEditor(fDataViewer.getTable());

		fColDefAltitude.setEditingSupport(new IntegerEditingSupport(cellEditor, fSerieAltitude));
		fColDefPulse.setEditingSupport(new IntegerEditingSupport(cellEditor, fSeriePulse));
		fColDefTemperature.setEditingSupport(new IntegerEditingSupport(cellEditor, fSerieTemperature));
		fColDefCadence.setEditingSupport(new IntegerEditingSupport(cellEditor, fSerieCadence));
//		fColDefLatitude.setEditingSupport(new DoubleEditingSupport(cellEditor, fSerieLatitude));
//		fColDefLongitude.setEditingSupport(new DoubleEditingSupport(cellEditor, fSerieLongitude));

		fColumnManager.createColumns();

		fDataViewer.setContentProvider(new TourDataContentProvider());
		fDataViewer.setUseHashlookup(true);

		fDataViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});

		// hide first column, this is a hack to align the "first" visible column to right
		table.getColumn(0).setWidth(0);
	}

	private void createDataViewerContextMenu(final Table table) {

		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Menu menu = menuMgr.createContextMenu(table);
		table.setMenu(menu);
	}

	private void createFieldListener() {

		fModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {

				if (fDisableModifyEvent || fIsSavingInProgress) {
					return;
				}

				setTourDirty();
			}
		};

		fKeyListener = new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				updateContentOnKeyUp();
			}
		};

		/*
		 * listener for tour date/time
		 */
		fDateTimeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (fDisableModifyEvent || fIsSavingInProgress) {
					return;
				}

				setTourDirty();

				updateUIWeek(fDtTourDate.getYear(),
						fDtTourDate.getMonth(),
						fDtTourDate.getDay(),
						fDtStartTime.getHours(),
						fDtStartTime.getMinutes());
			}
		};

		/*
		 * listener for recording/driving/paused time
		 */
		fTourTimeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {

				if (fDisableModifyEvent || fIsSavingInProgress) {
					return;
				}

				setTourDirty();

				/*
				 * ensure validation for all 3 times
				 */

				final DateTime dt = (DateTime) event.widget;

				int recTime = (fDtRecordingTime.getHours() * 3600)
						+ (fDtRecordingTime.getMinutes() * 60)
						+ fDtRecordingTime.getSeconds();

				int pausedTime = (fDtPausedTime.getHours() * 3600)
						+ (fDtPausedTime.getMinutes() * 60)
						+ fDtPausedTime.getSeconds();

				int driveTime = (fDtDrivingTime.getHours() * 3600)
						+ (fDtDrivingTime.getMinutes() * 60)
						+ fDtDrivingTime.getSeconds();

				if (dt == fDtRecordingTime) {

					// recording time is modified

					if (pausedTime > recTime) {
						pausedTime = recTime;
					}

					driveTime = recTime - pausedTime;

				} else if (dt == fDtPausedTime) {

					// paused time is modified

					if (pausedTime > recTime) {
						recTime = pausedTime;
					}

					driveTime = recTime - pausedTime;

				} else if (dt == fDtDrivingTime) {

					// driving time is modified

					if (driveTime > recTime) {
						recTime = driveTime;
					}

					pausedTime = recTime - driveTime;
				}

				fDtRecordingTime.setTime(recTime / 3600, ((recTime % 3600) / 60), ((recTime % 3600) % 60));
				fDtDrivingTime.setTime(driveTime / 3600, ((driveTime % 3600) / 60), ((driveTime % 3600) % 60));
				fDtPausedTime.setTime(pausedTime / 3600, ((pausedTime % 3600) / 60), ((pausedTime % 3600) % 60));
			}
		};

		fVerifyFloatValue = new ModifyListener() {

			public void modifyText(final ModifyEvent event) {

				if (fDisableModifyEvent || fIsSavingInProgress) {
					return;
				}

				setTourDirty();

				final Text widget = (Text) event.widget;
				final String valueText = widget.getText().trim();

				if (valueText.length() > 0) {
					try {

						// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
						//
						// Float.parseFloat() ignores localized strings therefore the databinding converter is used
						// which provides also a good error message
						//
						// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

						StringToNumberConverter.toFloat(true).convert(valueText);

						fMessageManager.removeMessage(widget.getData(WIDGET_KEY), widget);

					} catch (final IllegalArgumentException e) {

						// wrong characters are entered, display an error message

						fMessageManager.addMessage(widget.getData(WIDGET_KEY),
								e.getLocalizedMessage(),
								null,
								IMessageProvider.ERROR,
								widget);
					}
				}
			}
		};
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		/*
		 * tag menu
		 */
		MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final boolean isTagSet = fTourData.getTourTags().size() > 0;

				// enable actions
				fActionRemoveTag.setEnabled(isTagSet);
				fActionRemoveAllTags.setEnabled(isTagSet);

				// set menu items
				menuMgr.add(fActionAddTag);
				menuMgr.add(fActionRemoveTag);
				menuMgr.add(fActionRemoveAllTags);

				TagManager.fillRecentTagsIntoMenu(menuMgr, TourDataEditorView.this, true, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTagPrefs);
			}
		});

		// set menu for the tag item
		fTagLink.setMenu(menuMgr.createContextMenu(fTagLink));

		/*
		 * tour type menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourType.fillMenu(menuMgr, TourDataEditorView.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		fTourTypeLink.setMenu(menuMgr.createContextMenu(fTourTypeLink));
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		restoreStateBeforeUI(fSessionMemento);

		createFieldListener(); // must be set before the UI is created
		createUI(parent);
		createMenus();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourPropertyListener();
		addTourSaveListener();

		createActions();

		// this part is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		restoreStateWithUI(fSessionMemento);

		fPageBook.showPage(fPageNoTour);

		// show data from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		enableControls();

		if (fTourData == null) {
			/*
			 * when a tour is not displayed, find a tour provider which provides a tour
			 */
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {
					displayTour(TourManager.getTourProvider());
				}
			});
		}
	}

	private Composite createSection(final Composite parent, final FormToolkit tk, final String title) {

		final Section section = tk.createSection(parent,//
				//Section.TWISTIE | 
//				Section.SHORT_TITLE_BAR
				Section.TITLE_BAR
		// | Section.DESCRIPTION 
		// | Section.EXPANDED
		);

		section.setText(title);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(section);

		final Composite sectionContainer = tk.createComposite(section);
		section.setClient(sectionContainer);

//		section.addExpansionListener(new ExpansionAdapter() {
//			@Override
//			public void expansionStateChanged(final ExpansionEvent e) {
//				form.reflow(false);
//			}
//		});

		return sectionContainer;
	}

	private void createSectionCharacteristics(final Composite parent, final FormToolkit tk) {

		final Composite section = createSection(parent, tk, Messages.tour_editor_section_characteristics);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(section);

		/*
		 * tags
		 */
		fTagLink = new Link(section, SWT.NONE);
		fTagLink.setText(Messages.tour_editor_label_tour_tag);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(fTagLink);
		fTagLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				openControlMenu(fTagLink);
			}
		});

		fLblTourTags = tk.createLabel(section, UI.EMPTY_STRING, SWT.WRAP);
		tk.adapt(fLblTourTags, false, false);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				// hint is necessary that the width is not expanded when the text is long
				.hint(fTextColumnWidth, SWT.DEFAULT)
				.span(3, 1)
				.applyTo(fLblTourTags);

		/*
		 * tour type
		 */
		fTourTypeLink = new Link(section, SWT.NONE);
		fTourTypeLink.setText(Messages.tour_editor_label_tour_type);
		fTourTypeLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				openControlMenu(fTourTypeLink);
			}
		});

		fLblTourType = new CLabel(section, SWT.NONE);
		tk.adapt(fLblTourType);
		GridDataFactory.swtDefaults()//
				.grab(true, false)
				.span(3, 1)
				.applyTo(fLblTourType);
	}

	private void createSectionDateTime(final Composite parent, final FormToolkit tk) {

		final Composite section = createSection(parent, tk, Messages.tour_editor_section_date_time);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(20, 5).applyTo(section);

		/*
		 * container: 1. column
		 */
		final Composite tourDtContainer = tk.createComposite(section);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(tourDtContainer);
		GridDataFactory.fillDefaults().applyTo(tourDtContainer);

		/*
		 * date
		 */
		tk.createLabel(tourDtContainer, Messages.tour_editor_label_tour_date);

		fDtTourDate = new DateTime(tourDtContainer, SWT.DATE | SWT.MEDIUM | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(fDtTourDate);
		tk.adapt(fDtTourDate, true, false);
		fDtTourDate.addSelectionListener(fDateTimeListener);

		//////////////////////////////////////
		createUISeparator(tk, tourDtContainer);

		/*
		 * start time
		 */
		tk.createLabel(tourDtContainer, Messages.tour_editor_label_start_time);

		fDtStartTime = new DateTime(tourDtContainer, SWT.TIME | SWT.SHORT | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(fDtStartTime);
		tk.adapt(fDtStartTime, true, false);
		fDtStartTime.addSelectionListener(fDateTimeListener);

		//////////////////////////////////////
		createUISeparator(tk, tourDtContainer);

		/*
		 * tour distance
		 */
		tk.createLabel(tourDtContainer, Messages.tour_editor_label_tour_distance);

		fTextTourDistance = tk.createText(tourDtContainer, UI.EMPTY_STRING, SWT.TRAIL);
		GridDataFactory.fillDefaults().applyTo(fTextTourDistance);
		fTextTourDistance.addModifyListener(fVerifyFloatValue);
		fTextTourDistance.setData(WIDGET_KEY, WIDGET_KEY_TOURDISTANCE);

		fLblTourDistanceUnit = tk.createLabel(tourDtContainer, UI.UNIT_LABEL_DISTANCE);

		/*
		 * container: 2. column
		 */
		final Composite timeContainer = tk.createComposite(section);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(timeContainer);
		GridDataFactory.fillDefaults().applyTo(timeContainer);

		/*
		 * recording time
		 */
		tk.createLabel(timeContainer, Messages.tour_editor_label_recording_time);

		fDtRecordingTime = new DateTime(timeContainer, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		fDtRecordingTime.addSelectionListener(fTourTimeListener);
//		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(false, false).applyTo(fDtRecordingTime);
		tk.adapt(fDtRecordingTime, true, true);

		/*
		 * paused time
		 */
		tk.createLabel(timeContainer, Messages.tour_editor_label_paused_time);

		fDtPausedTime = new DateTime(timeContainer, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
//		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(false, false).applyTo(fDtPausedTime);
		tk.adapt(fDtPausedTime, true, true);
		fDtPausedTime.addSelectionListener(fTourTimeListener);

		/*
		 * driving time
		 */
		tk.createLabel(timeContainer, Messages.tour_editor_label_driving_time);

		fDtDrivingTime = new DateTime(timeContainer, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		tk.adapt(fDtDrivingTime, true, true);
		fDtDrivingTime.addSelectionListener(fTourTimeListener);
	}

	private void createSectionInfo(final Composite parent, final FormToolkit tk) {

		final Composite section = createSection(parent, tk, Messages.tour_editor_section_info);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(section);

		/*
		 * reference tours
		 */
		final Label label = tk.createLabel(section, Messages.tour_editor_label_ref_tour);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

		fLblRefTour = tk.createLabel(section, UI.EMPTY_STRING);

		/*
		 * data points
		 */
		tk.createLabel(section, Messages.tour_editor_label_datapoints);

		fLblDatapoints = tk.createLabel(section, UI.EMPTY_STRING, SWT.TRAIL);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(fLblDatapoints);

		/*
		 * device name
		 */
		tk.createLabel(section, Messages.tour_editor_label_device_name);

		fLblDeviceName = tk.createLabel(section, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(fLblDeviceName);

		/*
		 * person
		 */
		tk.createLabel(section, Messages.tour_editor_label_person);

		fLblPerson = tk.createLabel(section, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fLblPerson);
	}

	private void createSectionTitle(final Composite parent, final FormToolkit tk) {

		Label label;

		final Composite section = createSection(parent, tk, Messages.tour_editor_section_tour);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(section);

		/*
		 * title
		 */
		tk.createLabel(section, Messages.tour_editor_label_tour_title);

		fTextTitle = tk.createText(section, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(fTextTitle);
		fTextTitle.addKeyListener(fKeyListener);
		fTextTitle.addModifyListener(fModifyListener);

		/*
		 * description
		 */
		label = tk.createLabel(section, Messages.tour_editor_label_description);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

		fTextDescription = tk.createText(section, UI.EMPTY_STRING, SWT.BORDER //
				| SWT.WRAP
				| SWT.V_SCROLL
				| SWT.H_SCROLL//
		);

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		int descLines = store.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
		descLines = descLines == 0 ? 5 : descLines;

		GridDataFactory.fillDefaults()//
				.grab(true, false)
				//
				// SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
				//
				.hint(fTextColumnWidth, fPixelConverter.convertHeightInCharsToPixels(descLines))
				.applyTo(fTextDescription);

		fTextDescription.addModifyListener(fModifyListener);

		/*
		 * start location
		 */
		tk.createLabel(section, Messages.tour_editor_label_start_location);

		fTextStartLocation = tk.createText(section, UI.EMPTY_STRING);
		fTextStartLocation.addModifyListener(fModifyListener);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fTextStartLocation);

		/*
		 * end location
		 */
		tk.createLabel(section, Messages.tour_editor_label_end_location);

		fTextEndLocation = tk.createText(section, UI.EMPTY_STRING);
		fTextEndLocation.addModifyListener(fModifyListener);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fTextEndLocation);
	}

	private void createUI(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fPageNoTour = new Label(fPageBook, SWT.NONE);
		fPageNoTour.setText(Messages.UI_Label_no_chart_is_selected);

		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());

		fEditorForm = toolkit.createForm(fPageBook);
		toolkit.decorateFormHeading(fEditorForm);

		fMessageManager = new MessageManager(fEditorForm);
		fPixelConverter = new PixelConverter(parent);

		final Composite formBody = fEditorForm.getBody();
		GridLayoutFactory.fillDefaults().applyTo(formBody);

		fTabFolder = new CTabFolder(formBody, SWT.FLAT | SWT.BOTTOM);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fTabFolder);
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (fPostReloadViewerTourId == -1L) {
					// load viewer when this is not done
					fPostReloadViewerTourId = fTourData.getTourId();
					reloadViewer();
					updateStatus();
				}

				if (fTabFolder.getSelectionIndex() == TAB_INDEX_TIME_SLICES) {
					fDataViewer.getTable().setFocus();
				}

				enableControls();
			}
		});

		final CTabItem fTabItemLocation = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemLocation.setText(Messages.tour_editor_tabLabel_tour);
		fTabItemLocation.setControl(createUITabData(fTabFolder, toolkit));

		final CTabItem fTabMisc = new CTabItem(fTabFolder, SWT.FLAT);
		fTabMisc.setText(Messages.tour_editor_tabLabel_info);
		fTabMisc.setControl(createUITabInfo(fTabFolder, toolkit));

		final CTabItem fTabItemTimeSlices = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemTimeSlices.setText(Messages.tour_editor_tabLabel_tour_data);
		fTabItemTimeSlices.setControl(createUITabTimeSlices(fTabFolder));

	}

	private void createUISectionSeparator(final FormToolkit tk, final Composite parent) {
		final Composite sep = tk.createComposite(parent);
//		sep.setLayoutData(new ColumnLayoutData(SWT.DEFAULT, 10));
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 5).applyTo(sep);
	}

	private void createUISeparator(final FormToolkit tk, final Composite parent) {
		tk.createLabel(parent, UI.EMPTY_STRING);
	}

	private Composite createUITabData(final Composite parent, final FormToolkit tk) {

		/*
		 * scrolled container
		 */
		final ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		sc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				sc.setMinSize(fDataContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		fDataContainer = new Composite(sc, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fDataContainer);
		tk.adapt(fDataContainer);
		GridLayoutFactory.swtDefaults().applyTo(fDataContainer);

		// set content for scrolled composite
		sc.setContent(fDataContainer);

		createSectionTitle(fDataContainer, tk);

		createUISectionSeparator(tk, fDataContainer);

		createSectionDateTime(fDataContainer, tk);

		createUISectionSeparator(tk, fDataContainer);

		createSectionCharacteristics(fDataContainer, tk);

		return sc;
	}

	private Composite createUITabInfo(final Composite parent, final FormToolkit tk) {

		/*
		 * scrolled container
		 */
		final ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		sc.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				sc.setMinSize(fInfoContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		fInfoContainer = new Composite(sc, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fInfoContainer);
		tk.adapt(fInfoContainer);
		GridLayoutFactory.swtDefaults().applyTo(fInfoContainer);

		// set content for scrolled composite
		sc.setContent(fInfoContainer);

		createSectionInfo(fInfoContainer, tk);

		return sc;
	}

	/**
	 * @param parent
	 * @return returns the controls for the tab
	 */
	private Control createUITabTimeSlices(final Composite parent) {

		fTimeSliceContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fTimeSliceContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(fTimeSliceContainer);

		fDataViewerContainer = new Composite(fTimeSliceContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fDataViewerContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(fDataViewerContainer);

		createDataViewer(fDataViewerContainer);

		fTimeSliceLabel = new Label(fTimeSliceContainer, SWT.WRAP);
		fTimeSliceLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		fTimeSliceLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		fTimeSliceLabel.setVisible(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fTimeSliceLabel);

		return fTimeSliceContainer;
	}

	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		TableColumnDefinition colDef;

		/*
		 * 1. column will be hidden because the alignment for the first column is always to the left
		 */
		colDef = TableColumnFactory.FIRST_COLUMN.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setHideColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {}
		});

		/*
		 * column: #
		 */
		colDef = TableColumnFactory.SEQUENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int serieIndex = ((TimeSlice) cell.getElement()).serieIndex;
				final int logIndex = ((TimeSlice) cell.getElement()).uniqueCreateIndex;

				// the UI shows the time slice number starting with 1 and not with 0
				cell.setText(Integer.toString(logIndex + 1));

				// mark reference tour with a different background color
				if (fRefTourRange != null) {
					for (final int[] oneRange : fRefTourRange) {
						if (serieIndex >= oneRange[0] && serieIndex <= oneRange[1]) {
							cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
							break;
						}
					}
				} else {
					cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				}
			}
		});

		/*
		 * column: time
		 */
		colDef = TableColumnFactory.TOUR_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (fSerieTime != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;
					cell.setText(Integer.toString(fSerieTime[serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: distance
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (fSerieDistance != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int serieIndex = timeSlice.serieIndex;
					fNumberFormatter.setMinimumFractionDigits(3);
					fNumberFormatter.setMaximumFractionDigits(3);
					cell.setText(fNumberFormatter.format((float) fSerieDistance[serieIndex] / 1000));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: altitude
		 */
		fColDefAltitude = colDef = TableColumnFactory.ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieAltitude != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSerieAltitude[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: pulse
		 */
		fColDefPulse = colDef = TableColumnFactory.PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSeriePulse != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSeriePulse[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: temperature
		 */
		fColDefTemperature = colDef = TableColumnFactory.TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieTemperature != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSerieTemperature[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: cadence
		 */
		fColDefCadence = colDef = TableColumnFactory.CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieCadence != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSerieCadence[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: gradient
		 */
		colDef = TableColumnFactory.GRADIENT.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);

					cell.setText(fNumberFormatter.format((float) fSerieGradient[timeSlice.serieIndex] / 10));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: speed
		 */
		colDef = TableColumnFactory.SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);

					cell.setText(fNumberFormatter.format((float) fSerieSpeed[timeSlice.serieIndex] / 10));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: pace
		 */
		colDef = TableColumnFactory.PACE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);

					cell.setText(fNumberFormatter.format((float) fSeriePace[timeSlice.serieIndex] / 10));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: power
		 */
		colDef = TableColumnFactory.POWER.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSeriePower[timeSlice.serieIndex]));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: longitude
		 */
		fColDefLongitude = colDef = TableColumnFactory.LONGITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieLongitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Double.toString(fSerieLongitude[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: latitude
		 */
		fColDefLatitude = colDef = TableColumnFactory.LATITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieLatitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Double.toString(fSerieLatitude[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

	}

	/**
	 * Discard modifications and fire revert event
	 */
	private void discardModifications() {

		setTourClean();

		fMessageManager.removeAllMessages();

		fDisableModifyEvent = true;
		{
			fTourData = reloadTourData();
			updateUIFromTourData(fTourData, true);
		}
		fDisableModifyEvent = false;

		fireRevertNotification();
	}

	public void displayTour(final Long tourId) {

		if (tourId == null) {
			return;
		}

		// don't reload the same tour
		if (fTourData != null) {
			if (fTourData.getTourId().equals(tourId)) {
				return;
			}
		}

		fDisableModifyEvent = true;

		final TourData tourData = TourManager.getInstance().getTourData(tourId);
		if (tourData != null) {
			fTourEditor = null;
			fTourChart = null;
			updateUIFromTourData(tourData, false);
		}

		fDisableModifyEvent = false;
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(fPostSelectionListener);
		page.removePartListener(fPartListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);
		TourManager.getInstance().removeTourSaveListener(fTourSaveListener);

		super.dispose();
	}

	public void doSave(final IProgressMonitor monitor) {

	/*
	 * saving is done in the promptToSaveOnClose() method
	 */
	}

	public void doSaveAs() {}

	@SuppressWarnings("unused")
	private void dumpViewerItems() {
		for (final Object viewerItem : fDataViewerItems) {
			System.out.println(viewerItem);
		}
	}

	private void enableActions() {

		final boolean isTourValid = isTourValid();

		/*
		 * tour can only be saved when the tour is saved in the database
		 */
		fActionSaveTour.setEnabled(fIsTourDirty && isTourValid && isTourInDb());
		fActionUndoChanges.setEnabled(fIsTourDirty);

		final boolean isTimeSliceTab = fTabFolder.getSelectionIndex() == TAB_INDEX_TIME_SLICES;
		fActionModifyColumns.setEnabled(isTimeSliceTab);
		fActionToggleRowSelectMode.setEnabled(isTimeSliceTab);
	}

	private void enableControls() {

//		final boolean canEditTour = isTourInDb();

//		fTextTitle.setEnabled(canEditTour);
//		fTextDescription.setEnabled(canEditTour);
//
//		fTextStartLocation.setEnabled(canEditTour);
//		fTextEndLocation.setEnabled(canEditTour);

		enableActions();
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionDeleteTimeSlices);

	}

	/**
	 * fire notification for changed tour data
	 */
	private void fireModifyNotification() {

		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(fTourData);

		final TourProperties propertyData = new TourProperties(modifiedTour);
		propertyData.isTourEdited = true;

		TourManager.firePropertyChange(TourDataEditorView.this, TourManager.TOUR_PROPERTIES_CHANGED, propertyData);
	}

	/**
	 * fire notification for the reverted tour data
	 */
	private void fireRevertNotification() {

		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(fTourData);

		final TourProperties tourProperties = new TourProperties(modifiedTour);
		tourProperties.isReverted = true;

		TourManager.firePropertyChange(TourDataEditorView.this, TourManager.TOUR_PROPERTIES_CHANGED, tourProperties);
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(final StructuredSelection selection) {

		if (fTourChart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if (tourChart == null || tourChart.isDisposed()) {
				return;
			} else {
				fTourChart = tourChart;
			}
		}

		final Object[] selectedData = selection.toArray();

		if (selectedData.length > 1) {

			// two or more data are selected, set the 2 sliders to the first and last selected data

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TimeSlice) selectedData[0]).serieIndex,
					((TimeSlice) selectedData[selectedData.length - 1]).serieIndex));

		} else if (selectedData.length > 0) {

			// one data is selected

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TimeSlice) selectedData[0]).serieIndex,
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fDataViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	private Object[] getDataViewerItems() {

		if (fTourData == null) {
			return new Object[0];
		}

		updateDataSeriesFromTourData();

		/*
		 * create viewer elements (time slices), each viewer item contains the index into the data
		 * series
		 */
		final TimeSlice[] viewerItems = new TimeSlice[fTourData.timeSerie.length];
		for (int serieIndex = 0; serieIndex < viewerItems.length; serieIndex++) {
			viewerItems[serieIndex] = new TimeSlice(serieIndex);
		}

		return viewerItems;
	}

	/**
	 * Converts a string into a float value
	 * 
	 * @param valueText
	 * @return Returns the float value for the parameter valueText, return <code>0</code>
	 * @throws IllegalArgumentException
	 */
	private float getFloatValue(String valueText) throws IllegalArgumentException {

		valueText = valueText.trim();
		if (valueText.length() == 0) {

			return 0;

		} else {

			final Object convertedValue = StringToNumberConverter.toFloat(true).convert(valueText);
			if (convertedValue instanceof Float) {
				return ((Float) convertedValue).floatValue();
			}
		}

		return 0;
	}

	private double[] getRemainingDoubleSerieData(final double[] dataSerie, final int firstIndex, final int lastIndex) {

		final int oldSerieLength = dataSerie.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final double[] newDataSerie = new double[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// delete until the end 
			System.arraycopy(dataSerie, 0, newDataSerie, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(dataSerie, 0, newDataSerie, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
		}

		return newDataSerie;
	}

	private int[] getRemainingIntegerSerieData(final int[] oldDataSerie, final int firstIndex, final int lastIndex) {

		final int oldSerieLength = oldDataSerie.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final int[] newDataSerie = new int[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// delete until the end 
			System.arraycopy(oldDataSerie, 0, newDataSerie, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(oldDataSerie, 0, newDataSerie, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
		}

		return newDataSerie;
	}

	private TimeSlice[] getRemainingViewerItems(final Object[] dataViewerItems,
												final int firstIndex,
												final int lastIndex) {

		final int oldSerieLength = dataViewerItems.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final TimeSlice[] newViewerItems = new TimeSlice[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(dataViewerItems, lastIndex + 1, newViewerItems, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// get items from start, delete until the end 
			System.arraycopy(dataViewerItems, 0, newViewerItems, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(dataViewerItems, 0, newViewerItems, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(dataViewerItems, lastIndex + 1, newViewerItems, firstIndex, copyLength);
		}

		// update serie index
		int serieIndex = 0;
		for (final TimeSlice timeSlice : newViewerItems) {
			timeSlice.serieIndex = serieIndex++;
		}

		return newViewerItems;
	}

	public ArrayList<TourData> getSelectedTours() {

		if (fTourData == null) {
			return null;
		}

		final ArrayList<TourData> tourDataList = new ArrayList<TourData>();
		tourDataList.add(fTourData);

		return tourDataList;
	}

	/**
	 * get data for this tour data editor from a {@link TourEditor}
	 * 
	 * @param editor
	 */
	private void getTourEditorData(final TourEditor editor) {

		fTourEditor = editor;
		fTourChart = fTourEditor.getTourChart();

		// update dirty state from the editor
		if (fTourEditor.isDirty()) {
			setTourDirty();
		} else {
			setTourClean();
		}
	}

	public ColumnViewer getViewer() {
		return fDataViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	/**
	 * @return Returns <code>true</code> when the data have been modified and not saved
	 */
	public boolean isDirty() {
		return fIsTourDirty;
	}

	/**
	 * @return Returns <code>true</code> when the tour should be discarded<br>
	 *         returns <code>false</code> when the tour is invalid but should be saved<br>
	 */
	private boolean isDiscardTour() {

		final MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
				Messages.tour_editor_dlg_save_tour_title,
				null,
				NLS.bind(Messages.tour_editor_dlg_save_invalid_tour, TourManager.getTourDateFull(fTourData)),
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
				1);

		final int result = dialog.open();
		if (result == 0) {

			// discard modifications

			return true;

		} else {

			// save modifications

			return false;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public boolean isSaveOnCloseNeeded() {
		return isDirty();
	}

	/**
	 * @return Returns <code>true</code> when the tour is saved in the database
	 */
	private boolean isTourInDb() {

		if (fTourData != null && fTourData.getTourPerson() != null) {
			return true;
		}

		return false;
	}

	/**
	 * Checks the selection if it contains the current tour
	 * 
	 * @param selection
	 * @return Returns <code>true</code> when the current tour is within the selection
	 */
	private boolean isTourInSelection(final ISelection selection) {

		boolean isCurrentTourSelected = false;

		if (fTourData == null) {
			return false;
		}

		final long currentTourId = fTourData.getTourId();
		fSelectionTourId = null;

		if (selection instanceof SelectionTourData) {

			final TourData tourData = ((SelectionTourData) selection).getTourData();
			fSelectionTourId = tourData.getTourId();

			if (tourData != null && currentTourId == fSelectionTourId) {
				isCurrentTourSelected = true;
			}

		} else if (selection instanceof SelectionTourId) {

			fSelectionTourId = ((SelectionTourId) selection).getTourId();

			if (currentTourId == fSelectionTourId) {
				isCurrentTourSelected = true;
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

				fSelectionTourId = tourData.getTourId();
				if (currentTourId == fSelectionTourId) {
					isCurrentTourSelected = true;
				}
			}

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor == fTourEditor) {
				// the same editor is selected as the current
				isCurrentTourSelected = true;

			} else if (editor instanceof TourEditor) {

				// check tour id in the editor
				final TourEditor tourEditor = (TourEditor) editor;
				fSelectionTourId = tourEditor.getTourChart().getTourData().getTourId();
				if (currentTourId == fSelectionTourId) {

					// get editor data when tour data are set but not yet the tour editor
					getTourEditorData(tourEditor);
					isCurrentTourSelected = true;
				}
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				fSelectionTourId = ((TVICatalogComparedTour) firstElement).getTourId();
				if (currentTourId == fSelectionTourId) {
					isCurrentTourSelected = true;
				}
			}
		}

		return isCurrentTourSelected;
	}

	/**
	 * @return Returns <code>true</code> when all data for the tour are valid, <code>false</code>
	 *         otherwise
	 */
	private boolean isTourValid() {

		if (fTourData == null) {
			return true;
		}

		if (fIsTourDirty) {

			if (fTourData.getTourPerson() == null) {

				// tour is modified but not yet saved in the database

				fMessageManager.addMessage(WIDGET_KEY_PERSON,
						"Person is required.",
						null,
						IMessageProvider.ERROR,
						fLblPerson);

			} else {
				fMessageManager.removeMessage(WIDGET_KEY_PERSON, fLblPerson);
			}

			// tour is valid when there are no error messages

			return fMessageManager.getMessageCount() == 0;

		} else {

			// tour is not dirty

			return true;
		}
	}

	private void onSelectionChanged(final ISelection selection) {

		if (fIsSavingInProgress) {
			return;
		}

		// save current tour when another tour is selected
		if (isTourInSelection(selection)) {
			// tour in the selection is already displayed
			return;
		} else {
			// a new tour is selected, save modified tour
			if (saveTourData() == false) {
				return;
			}
		}

		fDisableModifyEvent = true;

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();
			if (tourData == null) {
				fTourEditor = null;
				fTourChart = null;
			} else {

				final TourChart tourChart = selectionTourData.getTourChart();

				fTourEditor = null;
				fTourChart = tourChart;
				updateUIFromTourData(tourData, false);
			}

		} else if (selection instanceof SelectionTourId) {

			displayTour(((SelectionTourId) selection).getTourId());

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				/*
				 * prevent loading the data from the same editor when data have not been modified
				 */
				if (editor == fTourEditor && editor.isDirty() == false) {
					return;
				}

				getTourEditorData(((TourEditor) editor));

				final TourData tourData = fTourChart.getTourData();
				updateUIFromTourData(tourData, false);
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

				if (fTourData == null) {
					fTourData = tourData;
					fTourEditor = null;
					fTourChart = null;
					updateUIFromTourData(tourData, false);
				} else {

					if (fTourData.getTourId() != tourData.getTourId()) {

						// a new tour id is in the selection
						fTourData = tourData;
						fTourEditor = null;
						fTourChart = null;
						updateUIFromTourData(tourData, false);
					}
				}
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				displayTour(((TVICatalogComparedTour) firstElement).getTourId());
			}
		}

		fDisableModifyEvent = false;
	}

	/**
	 * Opens the menu for a control aligned below the control on the left side
	 * 
	 * @param control
	 *            Controls which menu is opened
	 */
	private void openControlMenu(final Control control) {

		final Rectangle rect = control.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = control.getParent().toDisplay(pt);

		final Menu menu = control.getMenu();
		menu.setLocation(pt.x, pt.y);
		menu.setVisible(true);
	}

	public int promptToSaveOnClose() {

		if (fIsTourDirty == false) {
			return ISaveablePart2.NO;
		}

		if (saveTourData()) {
			return ISaveablePart2.NO;
		} else {
			return ISaveablePart2.CANCEL;
		}
	}

	public void recreateViewer() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				// preserve column width, selection and focus
				final ISelection selection = fDataViewer.getSelection();

				final Table table = fDataViewer.getTable();
				final boolean isFocus = table.isFocusControl();

// disabled: new columns are not displayed 
//				fColumnManager.saveState(fSessionMemento);

				fDataViewerContainer.setRedraw(false);
				{
					table.dispose();

					createDataViewer(fDataViewerContainer);
					fDataViewerContainer.layout();

					// update the viewer
					fDataViewerItems = getDataViewerItems();
					fDataViewer.setInput(fDataViewerItems);
				}
				fDataViewerContainer.setRedraw(true);

//				// restore selection and focus
//				Display.getCurrent().asyncExec(new Runnable() {
//					public void run() {
//						final Table table = fDataViewer.getTable();
//						table.setRedraw(false);
//						{
				fDataViewer.setSelection(selection, true);
				if (isFocus) {
					fDataViewer.getTable().setFocus();
				}
//						}
//						table.setRedraw(true);
//					}
//				});
			}
		});
	}

	private TourData reloadTourData() {

		if (fTourData.getTourPerson() == null) {

			// tour is not saved, reloading tour data is not possible

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.tour_editor_dlg_reload_data_title,
					Messages.tour_editor_dlg_reload_data_message);

			return fTourData;
		}

		final Long tourId = fTourData.getTourId();
		final TourManager tourManager = TourManager.getInstance();

		tourManager.removeTourFromCache(tourId);

		return tourManager.getTourData(tourId);
	}

	/**
	 * reload the content of the viewer
	 */
	public void reloadViewer() {

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				final ISelection previousSelection = fDataViewer.getSelection();

				final Table table = fDataViewer.getTable();
				if (table.isDisposed()) {
					return;
				}

				table.setRedraw(false);
				{

					/*
					 * update the viewer, show busy indicator when it's a large tour or the previous
					 * tour was large because it takes time to remove the old items
					 */
					if (fTourData != null
							&& fTourData.timeSerie != null
							&& fTourData.timeSerie.length > BUSY_INDICATOR_ITEMS
							|| table.getItemCount() > BUSY_INDICATOR_ITEMS) {

						BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
							public void run() {
								fDataViewerItems = getDataViewerItems();
								fDataViewer.setInput(fDataViewerItems);
							}
						});
					} else {
						fDataViewerItems = getDataViewerItems();
						fDataViewer.setInput(fDataViewerItems);
					}

					fDataViewer.setSelection(previousSelection, true);
				}
				table.setRedraw(true);
			}
		});
	}

	private void restoreStateBeforeUI(final IMemento memento) {

		if (memento != null) {

			// restore from memento

			final Integer mementoRowEditMode = memento.getInteger(MEMENTO_ROW_EDIT_MODE);
			if (mementoRowEditMode != null) {
				fIsRowEditMode = mementoRowEditMode == 1 ? true : false;
			}
		}
	}

	private void restoreStateWithUI(final IMemento memento) {

		if (memento == null) {

			// memento is not set, set defaults

			fTabFolder.setSelection(TAB_INDEX_TITLE);

		} else {

			// restore from memento

			// select tab
			final Integer selectedTab = memento.getInteger(MEMENTO_SELECTED_TAB);
			if (selectedTab != null) {
				fTabFolder.setSelection(selectedTab);
			} else {
				fTabFolder.setSelection(TAB_INDEX_TITLE);
			}

		}

		fActionToggleRowSelectMode.setChecked(fIsRowEditMode);
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("TourPropertiesView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save selected tab
		memento.putInteger(MEMENTO_SELECTED_TAB, fTabFolder.getSelectionIndex());

		memento.putInteger(MEMENTO_ROW_EDIT_MODE, fActionToggleRowSelectMode.isChecked() ? 1 : 0);

		fColumnManager.saveState(memento);
	}

	/**
	 * @return Returns <code>true</code> when the tour was saved, <code>false</code> when the tour
	 *         is not saved but canceled
	 */
	private boolean saveTourConfirmed() {

		if (fIsTourDirty == false) {
			return true;
		}

		final MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
				Messages.tour_editor_dlg_save_tour_title,
				null,
				NLS.bind(Messages.tour_editor_dlg_save_tour_message, TourManager.getTourDateFull(fTourData)),
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL },
				0);

		final int returnCode = dialog.open();
		if (returnCode == 0) {

			// button YES: save tour

			saveTourWithoutConfirmation();

			return true;

		} else if (returnCode == 1) {

			// button NO: discard modifications

			discardModifications();

			return true;

		} else {

			// button CANCEL / dialog is canceled: tour is not saved and not discarded

			return false;
		}
	}

	/**
	 * saves the tour in the {@link TourDataEditorView}
	 * 
	 * @return Returns <code>true</code> when the tour is saved or <code>false</code> when the tour
	 *         could not saved because the user canceled saving
	 */
	public boolean saveTourData() {

		if (isTourValid()) {

			return saveTourConfirmed();

		} else {

			// tour is invalid

			if (isDiscardTour()) {

				// discard modifications
				discardModifications();

				return true;

			} else {

				/*
				 * tour is not saved because the tour is invalid and should not be discarded
				 */

				return false;
			}
		}
	}

	/**
	 * @return Returns <code>true</code> when the tour was saved in a {@link TourEditor}
	 */
	private boolean saveTourInEditorParts() {

		final Long viewTourId = fTourData.getTourId();

		// check if a tour is opened in the tour editor
		for (final IEditorPart editorPart : UI.getOpenedEditors()) {
			if (editorPart instanceof TourEditor) {

				final IEditorInput editorInput = editorPart.getEditorInput();
				if (editorInput instanceof TourEditorInput) {

					final TourEditor tourEditor = (TourEditor) editorPart;
					final long editorTourId = ((TourEditorInput) editorInput).getTourId();

					if (editorTourId == viewTourId) {

						// a tour editor was found containing the current tour

						if (tourEditor.isDirty()) {

							// save tour in the editor
							editorPart.doSave(null);

							setTourClean();

							enableControls();

							fIsSavingInProgress = false;

							// there can be only one editor for a tour
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * saves the tour when it is valid
	 */
	private boolean saveTourWithoutConfirmation() {

		fIsSavingInProgress = true;

		updateTourDataFromUI();

		// tour was not found in an editor

		TourDatabase.saveTour(fTourData);

		setTourClean();
		enableControls();

		TourDatabase.getInstance().firePropertyChange(TourDatabase.TOUR_IS_CHANGED_AND_PERSISTED);

		// notify all views which display the tour type
		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(fTourData);

		TourManager.firePropertyChange(TourDataEditorView.this, TourManager.TOUR_PROPERTIES_CHANGED, modifiedTour);

		fIsSavingInProgress = false;

		return saveTourInEditorParts();
	}

	@Override
	public void setFocus() {
		fTabFolder.setFocus();
	}

	/**
	 * removes the dirty state from the tour editor, updates the save/undo actions and updates the
	 * part name
	 */
	private void setTourClean() {

		fIsTourDirty = false;

		enableActions();

		/*
		 * this is not an eclipse editor part but the property change must be fired to hide the "*"
		 * marker in the part name
		 */
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * sets the tour editor dirty, updates the save/undo actions and updates the part name
	 */
	private void setTourDirty() {

		if (fIsTourDirty) {
			return;
		}

		fIsTourDirty = true;

		enableActions();

		/*
		 * this is not an eclipse editor part but the property change must be fired to show the "*"
		 * marker in the part name
		 */
		firePropertyChange(PROP_DIRTY);
	}

	public void setTourIsModified() {

		updateUIFromTourData(fTourData, false);

		setTourDirty();
	}

	private void updateContentOnKeyUp() {

		// set changed data
		fTourData.setTourTitle(fTextTitle.getText());

		enableControls();

		fireModifyNotification();
	}

	private void updateDataSeriesFromTourData() {

		fSerieTime = fTourData.timeSerie;

		fSerieDistance = fTourData.getDistanceSerie();
		fSerieAltitude = fTourData.getAltitudeSerie();
		fSerieTemperature = fTourData.getTemperatureSerie();
		fSerieGradient = fTourData.getGradientSerie();
		fSerieSpeed = fTourData.getSpeedSerie();
		fSeriePace = fTourData.getPaceSerie();
		fSeriePower = fTourData.getPowerSerie();

		fSerieCadence = fTourData.cadenceSerie;
		fSeriePulse = fTourData.pulseSerie;

		fSerieLatitude = fTourData.latitudeSerie;
		fSerieLongitude = fTourData.longitudeSerie;

		fColDefAltitude.setEditorDataSerie(fSerieAltitude);
		fColDefTemperature.setEditorDataSerie(fSerieTemperature);
		fColDefPulse.setEditorDataSerie(fSeriePulse);
		fColDefCadence.setEditorDataSerie(fSerieCadence);
	}

	private void updateRefTourInfo(final Collection<TourReference> refTours) {

		final ArrayList<TourReference> refTourList = new ArrayList<TourReference>(refTours);

		// sort reference tours by start index
		Collections.sort(refTourList, new Comparator<TourReference>() {
			public int compare(final TourReference refTour1, final TourReference refTour2) {
				return refTour1.getStartValueIndex() - refTour2.getStartValueIndex();
			}
		});

		final StringBuilder sb = new StringBuilder();
		int refCounter = 0;

		fRefTourRange = new int[refTourList.size()][2];

		for (final TourReference refTour : refTourList) {

			if (refCounter > 0) {
				sb.append(UI.NEW_LINE);
			}

			sb.append(refTour.getLabel());

			sb.append(" ("); //$NON-NLS-1$
			sb.append(refTour.getStartValueIndex());
			sb.append(UI.DASH_WITH_SPACE);
			sb.append(refTour.getEndValueIndex());
			sb.append(")"); //$NON-NLS-1$

			final int[] oneRange = fRefTourRange[refCounter];
			oneRange[0] = refTour.getStartValueIndex();
			oneRange[1] = refTour.getEndValueIndex();

			refCounter++;
		}

		fLblRefTour.setText(sb.toString());
		fLblRefTour.pack(true);
	}

	private void updateStatus() {

		final boolean isVisible = fTimeSliceLabel.isVisible();
		boolean setVisible = false;

		if (fIsReferenceTourAvailable) {

			// tour contains reference tours

			fTimeSliceLabel.setText(Messages.TourDataEditorView_tour_editor_status_tour_contains_ref_tour);
			setVisible = true;

		} else {

			fTimeSliceLabel.setText(UI.EMPTY_STRING);
		}

		if (isVisible != setVisible) {

			// changes visibility

			fTimeSliceLabel.setVisible(setVisible);

//			fTimeSliceContainer.pack(true);
			fTimeSliceContainer.layout(true, true);
		}
	}

	/**
	 * display modified time slices in this editor and in other views/editors
	 */
	private void updateTimeSlices() {

		fTourData.clearComputedSeries();

		reloadViewer();

		enableControls();

		fireModifyNotification();
	}


	/**
	 * update {@link TourData} from the ui fields
	 */
	private void updateTourDataFromUI() {

		try {

			fTourData.setTourTitle(fTextTitle.getText());
			fTourData.setTourDescription(fTextDescription.getText());

			fTourData.setTourStartPlace(fTextStartLocation.getText());
			fTourData.setTourEndPlace(fTextEndLocation.getText());

			fTourData.setStartYear((short) fDtTourDate.getYear());
			fTourData.setStartMonth((short) (fDtTourDate.getMonth() + 1));
			fTourData.setStartDay((short) fDtTourDate.getDay());

			fTourData.setStartHour((short) fDtStartTime.getHours());
			fTourData.setStartMinute((short) fDtStartTime.getMinutes());

			// set week of year
			fCalendar.set(fTourData.getStartYear(), fTourData.getStartMonth() - 1, fTourData.getStartDay());
			fTourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

			final float distanceValue = getFloatValue(fTextTourDistance.getText()) * UI.UNIT_VALUE_DISTANCE * 1000;
			fTourData.setTourDistance((int) distanceValue);

		} catch (final IllegalArgumentException e) {

			// this should not happen (but it happend when developing the tour data editor :-)
			//
			// wrong characters are entered, display an error message

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", e.getLocalizedMessage());//$NON-NLS-1$

			e.printStackTrace();
		}
	}

	/**
	 * updates the fields in the tour data editor and enables actions and controls
	 * 
	 * @param tourData
	 * @param forceReload
	 */
	private void updateUIFromTourData(final TourData tourData, final boolean forceReload) {

		// keep tour data
		fTourData = tourData;

		// a tour which is not saved has no tour references
		final Collection<TourReference> tourReferences = tourData.getTourReferences();
		if (tourReferences == null) {
			fIsReferenceTourAvailable = false;
		} else {
			fIsReferenceTourAvailable = tourReferences.size() > 0;
		}

		// show tour type image when tour type is set
		final TourType tourType = tourData.getTourType();
		if (tourType == null) {
			fEditorForm.setImage(null);
		} else {
			fEditorForm.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
		}

		fEditorForm.setText(TourManager.getTourTitle(tourData));

		updateUITabData();
		updateUITabTimeSlices(forceReload);

		enableControls();

		fPageBook.showPage(fEditorForm);
	}

	private void updateUITabData() {

		final short tourYear = fTourData.getStartYear();
		final int tourMonth = fTourData.getStartMonth() - 1;
		final short tourDay = fTourData.getStartDay();

		// title/description
		fTextTitle.setText(fTourData.getTourTitle());
		fTextDescription.setText(fTourData.getTourDescription());

		// tour date
		fDtTourDate.setDate(tourYear, tourMonth, tourDay);

		// start time
		fDtStartTime.setTime(fTourData.getStartHour(), fTourData.getStartMinute(), 0);

		// week
		updateUIWeek(tourYear, tourMonth, tourDay, fTourData.getStartHour(), fTourData.getStartMinute());

		// recording time
		final int recordingTime = fTourData.getTourRecordingTime();
		fDtRecordingTime.setTime(recordingTime / 3600, ((recordingTime % 3600) / 60), ((recordingTime % 3600) % 60));

		// driving time
		final int drivingTime = fTourData.getTourDrivingTime();
		fDtDrivingTime.setTime(drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));

		// paused time
		final int pausedTime = recordingTime - drivingTime;
		fDtPausedTime.setTime(pausedTime / 3600, ((pausedTime % 3600) / 60), ((pausedTime % 3600) % 60));

		// data points
		final int[] timeSerie = fTourData.timeSerie;
		if (timeSerie == null) {
			fLblDatapoints.setText(UI.EMPTY_STRING);
		} else {
			final int dataPoints = timeSerie.length;
			fLblDatapoints.setText(Integer.toString(dataPoints));
		}
		fLblDatapoints.pack(true);

		// device name
		fLblDeviceName.setText(fTourData.getDeviceName());
		fLblDeviceName.pack(true);

		// start/end location
		fTextStartLocation.setText(fTourData.getTourStartPlace());
		fTextEndLocation.setText(fTourData.getTourEndPlace());

		// tour type
		final TourType tourType = fTourData.getTourType();
		if (tourType == null) {
			fLblTourType.setText(UI.EMPTY_STRING);
			fLblTourType.setImage(null);
		} else {
			fLblTourType.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
			fLblTourType.setText(tourType.getName());
		}
		fLblTourType.pack(true);
		fLblTourType.redraw(); // display changed tour image

		// tour tags
		final Set<TourTag> tourTags = fTourData.getTourTags();

		if (tourTags == null || tourTags.size() == 0) {
			fLblTourTags.setText(UI.EMPTY_STRING);
		} else {

			// sort tour tags by name
			final ArrayList<TourTag> tourTagList = new ArrayList<TourTag>(tourTags);
			Collections.sort(tourTagList, new Comparator<TourTag>() {
				public int compare(final TourTag tt1, final TourTag tt2) {
					return tt1.getTagName().compareTo(tt2.getTagName());
				}
			});

			final StringBuilder sb = new StringBuilder();
			int index = 0;
			for (final TourTag tourTag : tourTagList) {

				if (index > 0) {
					sb.append(", "); //$NON-NLS-1$
				}

				sb.append(tourTag.getTagName());

				index++;
			}
			fLblTourTags.setText(sb.toString());
			fLblTourTags.setToolTipText(sb.toString());
		}
		fLblTourTags.pack(true);

		// tour distance
		final int tourDistance = fTourData.getTourDistance();
		if (tourDistance == 0) {
			fTextTourDistance.setText(Integer.toString(tourDistance));
		} else {

			fNumberFormatter.setMinimumFractionDigits(3);
			fNumberFormatter.setMaximumFractionDigits(3);
			fNumberFormatter.setGroupingUsed(false);

			final float distance = ((float) tourDistance) / 1000 / UI.UNIT_VALUE_DISTANCE;
			fTextTourDistance.setText(fNumberFormatter.format(distance));

		}
		fLblTourDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

		/*
		 * reference tours
		 */
		final Collection<TourReference> refTours = fTourData.getTourReferences();
		if (refTours.size() > 0) {
			updateRefTourInfo(refTours);
		} else {
			fLblRefTour.setText(UI.EMPTY_STRING);
			fRefTourRange = null;
		}

		/*
		 * person
		 */
		final TourPerson tourPerson = fTourData.getTourPerson();
		if (tourPerson == null) {
			fLblPerson.setText(UI.EMPTY_STRING);
		} else {
			fLblPerson.setText(tourPerson.getName());
		}

		/*
		 * layout container to resize labels
		 */
		fDataContainer.layout(true);
		fInfoContainer.layout(true);
	}

	private void updateUITabTimeSlices(final boolean forceReload) {

		if (forceReload) {
			fPostReloadViewerTourId = -1L;
		}

		if (fTabFolder.getSelectionIndex() == TAB_INDEX_TIME_SLICES //
				&& fPostReloadViewerTourId != fTourData.getTourId()) {

			/*
			 * time slice tab is selected and the viewer is not yeat loaded
			 */

			reloadViewer();
			fPostReloadViewerTourId = fTourData.getTourId();

			updateStatus();

		} else {

			if (fPostReloadViewerTourId != fTourData.getTourId()) {
				// force reload when it's not yet loaded
				fPostReloadViewerTourId = -1L;
			}
		}
	}

	/**
	 * update week info
	 * 
	 * @param tourYear
	 * @param tourMonth
	 * @param tourDay
	 * @param hour
	 * @param minute
	 */
	private void updateUIWeek(	final int tourYear,
								final int tourMonth,
								final int tourDay,
								final int hour,
								final int minute) {

		fCalendar.set(tourYear, tourMonth, tourDay, hour, minute);

		fEditorForm.setText(TourManager.getTourTitle(fCalendar.getTime()));

	}

}
