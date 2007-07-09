/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.ui.views.tourBook;

import net.tourbook.Messages;
import net.tourbook.chart.ChartContextProvider;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourChart;
import net.tourbook.ui.views.tourMap.ReferenceTourManager;
import net.tourbook.ui.views.tourMap.SelectionNewRefTours;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Display;

class TourChartContextProvider implements ChartContextProvider {

	TourBookView			fView;

	public ChartXSlider		fSlider;

	private MarkerDialog	fMarkerDialog;

	/**
	 * add a new reference tour to all reference tours
	 */
	class ActionAddTourReference extends Action {

		ActionAddTourReference() {
			setText(Messages.TourMap_Action_create_reference_tour);
		}

		public void run() {

			TourReference refTour = ReferenceTourManager.getInstance().addReferenceTour(
					fView.getTourChart());

			if (refTour != null) {

				SelectionNewRefTours selection = new SelectionNewRefTours();
				selection.newRefTours.add(refTour);

				fView.firePostSelection(selection);
			}
		}
	}

	class SliderAction extends Action {

		SliderAction(String text, ChartXSlider slider) {
			super(text);

			fSlider = slider;
		}

		public void run() {

			if (fView != null) {

				final TourChart tourChart = fView.getTourChart();
				TourData tourData = tourChart.getTourData();
				TourMarker newTourMarker = createTourMarker(tourData);

				// add new marker to the marker list
				tourData.getTourMarkers().add(newTourMarker);
				

				(new MarkerDialog(Display.getCurrent().getActiveShell(), tourData, newTourMarker))
						.open();

				// force the tour to be saved
				tourChart.setTourDirty(true);

				// update chart
				tourChart.updateMarkerLayer(true);

				// update marker list and other listener
				tourChart.fireTourChartSelection();

			} else if (fMarkerDialog != null) {

				TourMarker newTourMarker = createTourMarker(fMarkerDialog.getTourData());
				
				fMarkerDialog.addTourMarker(newTourMarker);
			}
		}

		private TourMarker createTourMarker(TourData tourData) {
			
			int serieIndex = fSlider.getValuesIndex();

			// create a new marker
			TourMarker newTourMarker = new TourMarker(tourData, ChartMarker.MARKER_TYPE_CUSTOM);
			newTourMarker.setSerieIndex(serieIndex);
			newTourMarker.setDistance(tourData.distanceSerie[serieIndex]);
			newTourMarker.setTime(tourData.timeSerie[serieIndex]);
			newTourMarker.setLabel(Messages.TourData_Label_new_marker);
			newTourMarker.setVisualPosition(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

			return newTourMarker;
		}
	}

	public TourChartContextProvider(TourBookView view) {
		fView = view;
	}

	public TourChartContextProvider(MarkerDialog markerDialog) {
		fMarkerDialog = markerDialog;
	}

	public void fillBarChartContextMenu(IMenuManager menuMgr) {}

	public void fillXSliderContextMenu(	IMenuManager menuMgr,
										ChartXSlider leftSlider,
										ChartXSlider rightSlider) {

		if (fView != null) {

			// context dialog for the tourbook view

			if (leftSlider != null || rightSlider != null) {

				createMarkerMenu(menuMgr, leftSlider, rightSlider);

				// add to chart reference
				menuMgr.add(new ActionAddTourReference());
			}

		} else if (fMarkerDialog != null) {

			// context menu for the marker dialog

			createMarkerMenu(menuMgr, leftSlider, rightSlider);
		}

	}

	private void createMarkerMenu(	IMenuManager menuMgr,
									ChartXSlider leftSlider,
									ChartXSlider rightSlider) {

		if (leftSlider != null || rightSlider != null) {

			// marker menu
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(new SliderAction(Messages.TourMap_Action_create_marker, leftSlider));
			} else {
				menuMgr
						.add(new SliderAction(
								Messages.TourMap_Action_create_left_marker,
								leftSlider));
				menuMgr.add(new SliderAction(
						Messages.TourMap_Action_create_right_marker,
						rightSlider));
			}
		}
	}

}
