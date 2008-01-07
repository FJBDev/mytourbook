/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.PageBook;

public class WizardPageCompareTour extends WizardPage {

	// dialog settings
	private static final String	COMP_TOUR_VIEWER_WIDTH		= "CompTour.viewerWidth";			//$NON-NLS-1$
	private static final String	COMP_TOUR_SELECT_ALL		= "CompTour.selectAll";			//$NON-NLS-1$

	// tree columns
	static final int			COLUMN_DATE					= 0;
	static final int			COLUMN_DISTANCE				= 1;
	static final int			COLUMN_UP					= 2;
	static final int			COLUMN_RECORDING			= 3;

	private Button				fCheckSelectAll;
	private PageBook			fPageBook;
	private Label				fPageNoTourIsSelected;

	private CheckboxTreeViewer	fTourViewer;
	private TourChart			fTourChart;
	private ViewerDetailForm	fViewerDetailForm;

	private WizardDataModel		fWizardDataModel			= new WizardDataModel();
	private boolean				fIsTourViewerInitialized	= false;

	private NumberFormat		fNf							= NumberFormat.getNumberInstance();

	private Group				fChartGroup;

	public class TourContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {

			TourMapTourItem tourItem = ((TourMapTourItem) parentElement);

			// fetch the children if not yet done
			if (!tourItem.hasChildrenBeenFetched()) {
				fWizardDataModel.fetchChildren(tourItem);
			}
			return tourItem.getChildren();
		}

		public Object getParent(Object element) {
			return ((TourMapTourItem) element).getParent();
		}

		public boolean hasChildren(Object element) {
			return ((TourMapTourItem) element).hasChildren();
		}

		public Object[] getElements(Object inputElement) {
			return fWizardDataModel.getTopLevelEntries();
		}

		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	}

	class TourLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object obj, int index) {

			TourMapTourItem tourItem = (TourMapTourItem) obj;
			long[] row = tourItem.fTourItemData;

			switch (index) {
			case COLUMN_DATE:
				return Long.toString(row[COLUMN_DATE]);

			case COLUMN_DISTANCE:
				if (tourItem.getItemType() == TourMapTourItem.ITEM_TYPE_TOUR) {
					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);
					return fNf.format(((float) row[COLUMN_DISTANCE]) / (1000 * UI.UNIT_VALUE_DISTANCE));
				} else {
					return ""; //$NON-NLS-1$
				}

			case COLUMN_RECORDING:
				if (tourItem.getItemType() == TourMapTourItem.ITEM_TYPE_TOUR) {
					long recordingTime = row[COLUMN_RECORDING];
					return new Formatter().format(Messages.Format_hhmm,
							(recordingTime / 3600),
							((recordingTime % 3600) / 60)).toString();
				} else {
					return ""; //$NON-NLS-1$
				}

			case COLUMN_UP:
				if (tourItem.getItemType() == TourMapTourItem.ITEM_TYPE_TOUR) {
					return Long.toString((long) (row[COLUMN_UP] / UI.UNIT_VALUE_ALTITUDE));
				} else {
					return ""; //$NON-NLS-1$
				}

			default:
				return (getText(obj));
			}
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	protected WizardPageCompareTour(String pageName) {
		super(pageName);
		setTitle(Messages.TourMap_Wizard_Page_compared_tours_title);
	}

	public void createControl(Composite parent) {

		Composite pageContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(pageContainer);

		fCheckSelectAll = new Button(pageContainer, SWT.CHECK);
		fCheckSelectAll.setText(Messages.TourMap_Wizard_Action_select_all_tours);
		fCheckSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableTours(fCheckSelectAll.getSelection());
				validatePage();
			}
		});

		/*
		 * create master detail layout
		 */
		Composite masterDetailContainer = new Composite(pageContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(masterDetailContainer);

		final Control viewer = createTourViewer(masterDetailContainer);

		final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

		// chart group
		fChartGroup = new Group(masterDetailContainer, SWT.NONE);
		fChartGroup.setLayout(new GridLayout());
		fChartGroup.setText(Messages.TourMap_Wizard_Group_selected_tour);
		fChartGroup.setEnabled(false);

		fViewerDetailForm = new ViewerDetailForm(masterDetailContainer, viewer, sash, fChartGroup);

		/*
		 * create pagebook with the chart and the no-chart page
		 */
		fPageBook = new PageBook(fChartGroup, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fTourChart = new TourChart(fPageBook, SWT.NONE, false);
		fTourChart.setBackgroundColor(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		fPageNoTourIsSelected = new Label(fPageBook, SWT.NONE);
		fPageNoTourIsSelected.setText(Messages.TourMap_Wizard_Label_a_tour_is_not_selected);
		fPageNoTourIsSelected.setEnabled(false);

		restoreDialogSettings();

		// set the control, otherwise nothing is displayed
		setControl(pageContainer);

		validatePage();
	}

	/**
	 * enables/disables the controls which belong to the tour
	 * 
	 * @param isChecked
	 */
	private void enableTours(boolean isChecked) {

		boolean isEnabled = !isChecked;

		// load tour data into the viewer if not yet done
		if (isEnabled && fIsTourViewerInitialized == false) {
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {

					// initialize the data before the view input is set
					fWizardDataModel.setRootItem();

					fTourViewer.setInput(this);
				}
			});

			fIsTourViewerInitialized = true;
		}

		fTourViewer.getControl().setEnabled(isEnabled);
	}

	private Control createTourViewer(Composite parent) {

		initializeDialogUnits(parent);

		final TreeColumnLayout treeLayout = new TreeColumnLayout();

		Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(treeLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.MULTI
				| SWT.BORDER
				| SWT.FULL_SELECTION
				| SWT.CHECK);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		/*
		 * tree columns
		 */
		TreeColumn tc;

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.TourMap_Wizard_Column_tour);
		treeLayout.setColumnData(tc, new ColumnPixelData(convertWidthInCharsToPixels(20)));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(UI.UNIT_LABEL_DISTANCE);
		tc.setToolTipText(Messages.TourMap_Wizard_Column_distance_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(UI.UNIT_LABEL_ALTITUDE);
		tc.setToolTipText(Messages.TourMap_Wizard_Column_altitude_up_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.TourMap_Wizard_Column_h);
		tc.setToolTipText(Messages.TourMap_Wizard_Column_h_tooltip);
		treeLayout.setColumnData(tc, new ColumnWeightData(10));

		/*
		 * tree viewer
		 */
		fTourViewer = new ContainerCheckedTreeViewer(tree);
		fTourViewer.setContentProvider(new TourContentProvider());
		fTourViewer.setLabelProvider(new TourLabelProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				showCompareTour(event);
			}
		});

		fTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				validatePage();
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {

				Object selection = ((IStructuredSelection) fTourViewer.getSelection()).getFirstElement();

				if (selection != null) {

					// expand/collapse current item

					if (fTourViewer.getExpandedState(selection)) {
						fTourViewer.collapseToLevel(selection, 1);
					} else {
						fTourViewer.expandToLevel(selection, 1);
					}
				}
			}
		});

		return layoutContainer;
	}

	private void showCompareTour(SelectionChangedEvent event) {

		IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		if (selection != null) {

			if (selection.getFirstElement() instanceof TourMapTourItem) {

				TourMapTourItem tourItem = (TourMapTourItem) selection.getFirstElement();

				if (tourItem.getItemType() == TourMapTourItem.ITEM_TYPE_TOUR) {

					// get tour data from the database
					final TourData tourData = TourManager.getInstance().getTourData(tourItem.getTourId());

					// set altitude visible
					TourChartConfiguration chartConfig = new TourChartConfiguration(true);
					chartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

					fTourChart.updateTourChart(tourData, chartConfig, false);

					fChartGroup.setText(NLS.bind(Messages.TourMap_Wizard_Group_selected_tour_2,
							TourManager.getTourDate(tourData)));
					fPageBook.showPage(fTourChart);
				} else {
					fChartGroup.setText(""); //$NON-NLS-1$
					fPageBook.showPage(fPageNoTourIsSelected);
				}
			}
		}
	}

	private void restoreDialogSettings() {

		IDialogSettings wizardSettings = getDialogSettings();

		// restore viewer width
		Integer viewerWidth = null;
		try {
			viewerWidth = wizardSettings.getInt(COMP_TOUR_VIEWER_WIDTH);
		}
		catch (NumberFormatException e) {}
		fViewerDetailForm.setViewerWidth(viewerWidth);

		// restore checkbox: select all tours
		boolean isSelectAllTours = wizardSettings.getBoolean(COMP_TOUR_SELECT_ALL);
		fCheckSelectAll.setSelection(isSelectAllTours);

		enableTours(isSelectAllTours);
	}

	void persistDialogSettings() {

		IDialogSettings wizardSettings = getDialogSettings();

		// save the viewer width
		wizardSettings.put(COMP_TOUR_VIEWER_WIDTH, fTourViewer.getTree().getSize().x);

		wizardSettings.put(COMP_TOUR_SELECT_ALL, fCheckSelectAll.getSelection());
	}

	private boolean validatePage() {

		setMessage(Messages.TourMap_Wizard_Label_page_message);

		if (fCheckSelectAll.getSelection()) {

			setPageComplete(true);
			setErrorMessage(null);
			return true;

		} else {

			final Object[] checkedElements = fTourViewer.getCheckedElements();

			if (checkedElements.length == 0) {
				setPageComplete(false);
				setErrorMessage(Messages.TourMap_Wizard_Error_tour_must_be_selected);
				return false;

			} else {
				setPageComplete(true);
				setErrorMessage(null);
				return true;
			}
		}
	}

	/**
	 * @return return all checked tours
	 */
	public Object[] getComparedTours() {

		TourMapTourItem[] tours = new TourMapTourItem[0];

		if (fCheckSelectAll.getSelection()) {

			// return all tours

			return getAllTourIds();

		} else {

			// convert the Object[] into a TreeViewerItem[]
			Object[] checked = fTourViewer.getCheckedElements();
			tours = new TourMapTourItem[checked.length];
			System.arraycopy(checked, 0, tours, 0, checked.length);
		}

		return tours;
	}

	private Long[] getAllTourIds() {

		String sqlString = "SELECT TourId FROM " + TourDatabase.TABLE_TOUR_DATA; //$NON-NLS-1$

		ArrayList<Long> tourIdList = new ArrayList<Long>();

		try {

			Connection conn = TourDatabase.getInstance().getConnection();

			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				tourIdList.add(result.getLong(1));
			}

			conn.close();

		}
		catch (SQLException e) {
			e.printStackTrace();
		}

		return tourIdList.toArray(new Long[tourIdList.size()]);
	}

}
