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

package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.ActionEditTour;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogReferenceTour;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

/**
 * Shows the selected tour in a chart
 */
public class TourChartView extends ViewPart implements ISelectedTours {

	public static final String		ID	= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;
	private TourData				fTourData;

	private ISelectionListener		fPostSelectionListener;
	private IPropertyChangeListener	fPrefChangeListener;

	private PostSelectionProvider	fPostSelectionProvider;
	private IPropertyListener		fTourDbListener;
	private ITourPropertyListener	fTourPropertyListener;

	private PageBook				fPageBook;
	private Label					fPageNoChart;

	private class TourChartContextProvicer implements IChartContextProvider, ISelectedTours {

		final ActionEditQuick		fActionQuickEdit		= new ActionEditQuick(TourChartView.this);
		final ActionEditTour		fActionEditTour			= new ActionEditTour(TourChartView.this);
		final ActionSetTourType		fActionSetTourType		= new ActionSetTourType(this);
		final ActionSetTourTag		fActionAddTag			= new ActionSetTourTag(this, true);
		final ActionSetTourTag		fActionRemoveTag		= new ActionSetTourTag(this, false);
		final ActionRemoveAllTags	fActionRemoveAllTags	= new ActionRemoveAllTags(this);

		public void fillBarChartContextMenu(final IMenuManager menuMgr,
											final int hoveredBarSerieIndex,
											final int hoveredBarValueIndex) {}

		public void fillContextMenu(final IMenuManager menuMgr) {

			menuMgr.add(fActionQuickEdit);
			menuMgr.add(fActionEditTour);

			menuMgr.add(new Separator());
			menuMgr.add(fActionAddTag);
			menuMgr.add(fActionRemoveTag);
			menuMgr.add(fActionRemoveAllTags);

			TagManager.fillRecentTagsIntoMenu(menuMgr, this, true);

			menuMgr.add(new Separator());
			menuMgr.add(fActionSetTourType);

			/*
			 * enable actions
			 */
			final boolean isEnabled = fTourData != null && fTourData.getTourPerson() != null;

			fActionQuickEdit.setEnabled(isEnabled);
			fActionEditTour.setEnabled(isEnabled);

			fActionAddTag.setEnabled(isEnabled);
			fActionRemoveTag.setEnabled(isEnabled);
			fActionRemoveAllTags.setEnabled(isEnabled);

			fActionSetTourType.setEnabled(isEnabled);

			// enable actions for the recent tags
			TagManager.enableRecentTagActions(isEnabled);

		}

		public void fillXSliderContextMenu(	final IMenuManager menuMgr,
											final ChartXSlider leftSlider,
											final ChartXSlider rightSlider) {}

		public ArrayList<TourData> getSelectedTours() {

			final ArrayList<TourData> tourList = new ArrayList<TourData>();
			tourList.add(fTourData);

			return tourList;
		}

		public boolean isFromTourEditor() {
			return false;
		}
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					fTourChartConfig = TourManager.createTourChartConfiguration();

					if (fTourChart != null) {
						fTourChart.updateTourChart(fTourData, fTourChartConfig, false);
					}
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
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourDbListener() {

		fTourDbListener = new IPropertyListener() {

			public void propertyChanged(final Object source, final int propId) {
				if (propId == TourDatabase.TOUR_IS_CHANGED_AND_PERSISTED) {

					if (fTourData == null) {
						return;
					}

					// reload data from the database
					fTourData = TourDatabase.getTourData(fTourData.getTourId());

					updateChart();

				} else if (propId == TourDatabase.TOUR_IS_CHANGED) {

					updateChart();
				}
			}
		};

		TourDatabase.getInstance().addPropertyListener(fTourDbListener);
	}

	private void addTourPropertyListener() {
		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked")
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_SEGMENT_LAYER_CHANGED) {

					fTourChart.updateSegmentLayer((Boolean) propertyData);

				} else if (propertyId == TourManager.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					fTourChart.updateTourChart(true, true);

				} else if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					if (fTourData == null) {
						return;
					}

					// get modified tours
					final ArrayList<TourData> modifiedTours = (ArrayList<TourData>) propertyData;
					final long tourId = fTourData.getTourId();

					// check if the tour in the editor was modified
					for (final TourData tourData : modifiedTours) {
						if (tourData.getTourId() == tourId) {

							// keep changed data
							fTourData = tourData;

							updateChart(tourData);

							return;
						}
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fTourChart = new TourChart(fPageBook, SWT.FLAT, true);
		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
		fTourChart.setContextProvider(new TourChartContextProvicer());

		fTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				if (fTourData.getTourPerson() != null) {
					TourManager.getInstance().openTourInEditor(fTourData.getTourId());
				}
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();

		// set chart title
		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel chartDataModel) {
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		addSelectionListener();
		addPrefListener();

		addTourDbListener();
		addTourPropertyListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		// show current selected chart if there are any
		final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {
			onSelectionChanged(selection);
		} else {
			fPageBook.showPage(fPageNoChart);
		}
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(fPostSelectionListener);

		TourDatabase.getInstance().removePropertyListener(fTourDbListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	public ArrayList<TourData> getSelectedTours() {

		if (fTourData == null) {
			return null;
		}

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(fTourData);

		return tourList;
	}

	public TourChart getTourChart() {
		return fTourChart;
	}

	public boolean isFromTourEditor() {
		return false;
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();
			if (selectionTourData != null) {

				if (fTourData != null && fTourData.equals(selectionTourData)) {
					return;
				}
				updateChart(selectionTourData);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateChart(tourId);

		} else if (selection instanceof SelectionChartXSliderPosition) {

			fTourChart.setXSliderPosition((SelectionChartXSliderPosition) selection);

		} else if (selection instanceof StructuredSelection) {

			final StructuredSelection structuredSelection = (StructuredSelection) selection;

			final Object firstElement = structuredSelection.getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				updateChart(((TVICatalogComparedTour) firstElement).getTourId());
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogReferenceTour refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				updateChart(refItem.getTourId());
			}
		}
	}

	@Override
	public void setFocus() {
		fTourChart.setFocus();

		/*
		 * fire tour selection
		 */
		fPostSelectionProvider.setSelection(new SelectionTourData(fTourChart, fTourData));
	}

	private void updateChart() {

		if (fTourData == null) {
			// nothing to do
			return;
		}

		TourManager.getInstance().setActiveTourChart(fTourChart);

		fTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		fPageBook.showPage(fTourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDate(fTourData));
	}

	private void updateChart(final Long tourId) {

		if (fTourData != null && fTourData.getTourId() == tourId) {
			// optimize
			return;
		}

		updateChart(TourManager.getInstance().getTourData(tourId));
	}

	private void updateChart(final TourData tourData) {

		if (tourData == null) {
			// nothing to do
			return;
		}

		fTourData = tourData;

		updateChart();
	}

}
