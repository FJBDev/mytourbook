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

package net.tourbook.tour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartMarkerLayer;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * The tour chart extends the chart with all the functionality for a tour chart
 */
public class TourChart extends Chart {

	static final String					COMMAND_ID_CHARTOPTIONS			= "net.tourbook.command.tourChart.options";
	static final String					COMMAND_ID_SHOWSTARTTIME		= "net.tourbook.command.tourChart.showStartTime";

	static final String					COMMAND_ID_X_AXIS_DISTANCE		= "net.tourbook.command.tourChart.xAxisDistance";
	static final String					COMMAND_ID_X_AXIS_TIME			= "net.tourbook.command.tourChart.xAxisTime";

	static final String					COMMAND_ID_GRAPH_ALTITUDE		= "net.tourbook.command.graph.altitude";
	static final String					COMMAND_ID_GRAPH_SPEED			= "net.tourbook.command.graph.speed";
	static final String					COMMAND_ID_GRAPH_PULSE			= "net.tourbook.command.graph.pulse";
	static final String					COMMAND_ID_GRAPH_TEMPERATURE	= "net.tourbook.command.graph.temperature";
	static final String					COMMAND_ID_GRAPH_CADENCE		= "net.tourbook.command.graph.cadence";
	static final String					COMMAND_ID_GRAPH_ALTIMETER		= "net.tourbook.command.graph.altimeter";
	static final String					COMMAND_ID_GRAPH_GRADIENT		= "net.tourbook.command.graph.gradient";

	static final String					SEGMENT_VALUES					= "segmentValues";									//$NON-NLS-1$

	TourData							fTourData;
	TourChartConfiguration				fTourChartConfig;

	Map<String, TourChartActionProxy>	fActionProxies;

	private ActionChartOptions			fActionOptions;
	private Action						fActionZoomFitGraph;

//	private Action					fActionAdjustAltitude;
//	private ActionGraphAnalyzer		fActionGraphAnalyzer;
//	private ActionTourSegmenter		fActionTourSegmenter;
//	private ActionTourMarker		fActionMarkerEditor;

	private ListenerList				fSelectionListeners				= new ListenerList();

	/**
	 * datamodel listener is called when the chart data is created
	 */
	private IDataModelListener			fChartDataModelListener;

	private ITourModifyListener			fTourModifyListener;

	private ChartMarkerLayer			fMarkerLayer;
	private ChartSegmentLayer			fSegmentLayer;
	private ChartSegmentValueLayer		fSegmentValueLayer;

	private boolean						fIsXSliderVisible;

	private IPropertyChangeListener		fPrefChangeListener;
	private boolean						fIsTourDirty;
	private boolean						fIsSegmentLayerVisible;

	private boolean						fShowActions;

	public TourChart(final Composite parent, final int style, boolean showActions) {

		super(parent, style);

		fShowActions = showActions;

		setPrefListeners();

		/*
		 * when the focus is changed, fire a tour chart selection, this is neccesarry to update the
		 * tour markers when a tour chart got the focus
		 */
		addFocusListener(new Listener() {
			public void handleEvent(Event event) {
				fireTourChartSelection();
			}
		});

	}

	/**
	 * Set the X-axis to distance
	 * 
	 * @param isChecked
	 * @return Returns <code>true</code> when the x-axis was set to the distance
	 */
	boolean actionXAxisDistance(boolean isChecked) {

		// check if the distance axis button was pressed
		if (isChecked && !fTourChartConfig.showTimeOnXAxis) {
			return false;
		}

		if (isChecked) {

			// show distance on x axes

			fTourChartConfig.showTimeOnXAxis = !fTourChartConfig.showTimeOnXAxis;
			fTourChartConfig.showTimeOnXAxisBackup = fTourChartConfig.showTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateChart(true);

//			fActionOptions.actionStartTimeOption.setEnabled(false);

		} else {
			// this action got unchecked
//			fActionOptions.actionStartTimeOption.setEnabled(true);
		}
		return true;
	}

	/**
	 * @param isChecked
	 * @return Returns <code>true</code> when the check state was changed
	 */
	boolean actionXAxisTime(boolean isChecked) {

		// check if the time axis button was pressed
		if (isChecked && fTourChartConfig.showTimeOnXAxis) {
			return false;
		}

		if (isChecked) {

			// show time on x axis

			fTourChartConfig.showTimeOnXAxis = !fTourChartConfig.showTimeOnXAxis;
			fTourChartConfig.showTimeOnXAxisBackup = fTourChartConfig.showTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateChart(true);

//			fActionOptions.actionStartTimeOption.setEnabled(true);

		} else {
			// this action got unchecked - keep this state
//			fActionOptions.actionStartTimeOption.setEnabled(false);
		}
		return true;
	}

	/**
	 * Activate all tour chart action handlers, this must be done when the part with a tour chart is
	 * activated
	 */
	public void activateActions() {

		if (fUseInternalActionBar) {
			return;
		}

		TourChartActionHandlerManager.getInstance().activateActions(this);
	}

	/**
	 * add a data model listener which is fired when the data model has changed
	 * 
	 * @param dataModelListener
	 */
	public void addDataModelListener(final IDataModelListener dataModelListener) {
		fChartDataModelListener = dataModelListener;
	}

	public void addTourChartListener(ITourChartSelectionListener listener) {
		fSelectionListeners.add(listener);
	}

	public void addTourModifyListener(final ITourModifyListener listener) {
		fTourModifyListener = listener;
	}

	/**
	 * Create action proxy for a graph action
	 * 
	 * @param id
	 * @param label
	 * @param toolTip
	 * @param imageName
	 * @param definitionId
	 * @param isChecked
	 * @return
	 */
	private void createGraphActionProxy(final int graphId,
										final String label,
										final String toolTip,
										final String imageName,
										String commandId) {

		Action action = null;

		if (isUseInternalActionBar()) {
			action = new ActionGraph(this, graphId, label, toolTip, imageName);
		}

		final TourChartActionProxy actionProxy = new TourChartActionProxy(this, commandId, action);
		actionProxy.setIsGraphAction();

		fActionProxies.put(getProxyId(graphId), actionProxy);
	}

	/**
	 * create the layer which displays the tour marker
	 */
	private void createMarkerLayer() {

		final int[] xAxisSerie = fTourChartConfig.showTimeOnXAxis
				? fTourData.timeSerie
				: fTourData.distanceSerie;

		fMarkerLayer = new ChartMarkerLayer();
		fMarkerLayer.setLineColor(new RGB(50, 100, 10));

		final Collection<TourMarker> tourMarkerList = fTourData.getTourMarkers();

		for (final TourMarker tourMarker : tourMarkerList) {

			final ChartMarker chartMarker = new ChartMarker();

			chartMarker.graphX = xAxisSerie[tourMarker.getSerieIndex()];

			chartMarker.markerLabel = tourMarker.getLabel();
			chartMarker.graphLabel = Integer.toString(fTourData.altitudeSerie[tourMarker.getSerieIndex()]);

			chartMarker.serieIndex = tourMarker.getSerieIndex();
			chartMarker.visualPosition = tourMarker.getVisualPosition();
			chartMarker.type = tourMarker.getType();
			chartMarker.visualType = tourMarker.getVisibleType();

			chartMarker.labelXOffset = tourMarker.getLabelXOffset();
			chartMarker.labelYOffset = tourMarker.getLabelYOffset();

			fMarkerLayer.addMarker(chartMarker);
		}
	}

	/**
	 * Creates the layers from the segmented tour data
	 */
	private void createSegmentsLayer() {

		if (fTourData == null) {
			return;
		}

		final int[] segmentSerie = fTourData.segmentSerieIndex;

		if (segmentSerie == null || fIsSegmentLayerVisible == false) {
			// no segmented tour data available or segments are invisible
			return;
		}

		final int[] xDataSerie = fTourChartConfig.showTimeOnXAxis
				? fTourData.timeSerie
				: fTourData.distanceSerie;

		fSegmentLayer = new ChartSegmentLayer();
		fSegmentLayer.setLineColor(new RGB(0, 177, 219));

		for (int segmentIndex = 0; segmentIndex < segmentSerie.length; segmentIndex++) {

			final int serieIndex = segmentSerie[segmentIndex];

			final ChartMarker chartMarker = new ChartMarker();

			chartMarker.graphX = xDataSerie[serieIndex];
			chartMarker.serieIndex = serieIndex;

			fSegmentLayer.addMarker(chartMarker);
		}

		fSegmentValueLayer = new ChartSegmentValueLayer();
		fSegmentValueLayer.setLineColor(new RGB(231, 104, 38));
		fSegmentValueLayer.setTourData(fTourData);
		fSegmentValueLayer.setXDataSerie(xDataSerie);

		// draw the graph lighter so the segments are more visible
		setGraphAlpha(0x40);
	}

	/**
	 * Creates the handlers for the tour chart actions in the workbench, the internal action bar for
	 * the chart will be disabled
	 * 
	 * @param workbenchWindow
	 * @param tourChartConfig
	 */
	public void createTourActionHandlers(	IWorkbenchWindow workbenchWindow,
											TourChartConfiguration tourChartConfig) {

		// use commands defined in the plugin.xml
		fUseInternalActionBar = false;

		fShowActions = true;

		TourChartActionHandlerManager.getInstance().createActionHandlers(workbenchWindow);

		fTourChartConfig = tourChartConfig;

		createTourActionProxies();
	}

	private void createTourActionProxies() {

		// create actions only once
		if (fActionProxies != null) {
			return;
		}

		fActionProxies = new HashMap<String, TourChartActionProxy>();

		/*
		 * Actions for all chart graphs
		 */
		createGraphActionProxy(TourManager.GRAPH_ALTITUDE,
				Messages.Graph_Label_Altitude,
				Messages.Tour_Action_graph_altitude_tooltip,
				Messages.Image_graph_altitude,
				COMMAND_ID_GRAPH_ALTITUDE);

		createGraphActionProxy(TourManager.GRAPH_SPEED,
				Messages.Graph_Label_Speed,
				Messages.Tour_Action_graph_speed_tooltip,
				Messages.Image_graph_speed,
				COMMAND_ID_GRAPH_SPEED);

		createGraphActionProxy(TourManager.GRAPH_ALTIMETER,
				Messages.Graph_Label_Altimeter,
				Messages.Tour_Action_graph_altimeter_tooltip,
				Messages.Image_graph_altimeter,
				COMMAND_ID_GRAPH_ALTIMETER);

		createGraphActionProxy(TourManager.GRAPH_PULSE,
				Messages.Graph_Label_Heartbeat,
				Messages.Tour_Action_graph_heartbeat_tooltip,
				Messages.Image_graph_heartbeat,
				COMMAND_ID_GRAPH_PULSE);

		createGraphActionProxy(TourManager.GRAPH_TEMPERATURE,
				Messages.Graph_Label_Temperature,
				Messages.Tour_Action_graph_temperature_tooltip,
				Messages.Image_graph_temperature,
				COMMAND_ID_GRAPH_TEMPERATURE);

		createGraphActionProxy(TourManager.GRAPH_CADENCE,
				Messages.Graph_Label_Cadence,
				Messages.Tour_Action_graph_cadence_tooltip,
				Messages.Image_graph_cadence,
				COMMAND_ID_GRAPH_CADENCE);

		createGraphActionProxy(TourManager.GRAPH_GRADIENT,
				Messages.Graph_Label_Gradiend,
				Messages.Tour_Action_graph_gradient_tooltip,
				Messages.Image_graph_gradient,
				COMMAND_ID_GRAPH_GRADIENT);

		Action action;
		TourChartActionProxy actionProxy;

		/*
		 * Action: x-axis time
		 */
		if (isUseInternalActionBar()) {
			action = new ActionXAxisTime(this);
		} else {
			action = null;
		}
		actionProxy = new TourChartActionProxy(this, COMMAND_ID_X_AXIS_TIME, action);
		actionProxy.setChecked(fTourChartConfig.showTimeOnXAxis);
		fActionProxies.put(COMMAND_ID_X_AXIS_TIME, actionProxy);

		/*
		 * Action: x-axis distance
		 */
		if (isUseInternalActionBar()) {
			action = new ActionXAxisDistance(this);
		} else {
			action = null;
		}
		actionProxy = new TourChartActionProxy(this, COMMAND_ID_X_AXIS_DISTANCE, action);
		actionProxy.setChecked(!fTourChartConfig.showTimeOnXAxis);
		fActionProxies.put(COMMAND_ID_X_AXIS_DISTANCE, actionProxy);

		/*
		 * Action: fit graph to window
		 */
		fActionZoomFitGraph = new ActionZoomFitGraph(this);

		/*
		 * Action: chart options
		 */
		actionProxy = new TourChartActionProxy(this, COMMAND_ID_CHARTOPTIONS, null);
		fActionProxies.put(COMMAND_ID_CHARTOPTIONS, actionProxy);

		/*
		 * Action: show start time
		 */
		actionProxy = new TourChartActionProxy(this, COMMAND_ID_SHOWSTARTTIME, null);
		fActionProxies.put(COMMAND_ID_SHOWSTARTTIME, actionProxy);
	}

//	/**
//	 * Deactivate all tour chart action handlers, this is used when a part is deactivated
//	 */
//	public void deactivateHandler() {
//
//		if (fUseInternalActionBar) {
//			return;
//		}
//	}

	public void dispose() {

		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	/**
	 * enable/disable and check/uncheck the zoom options
	 */
	private void enableZoomOptions() {

		// get options check status from the configuration
		final boolean canScrollZoomedChart = fTourChartConfig.scrollZoomedGraph;
		final boolean canAutoZoomToSlider = fTourChartConfig.autoZoomToSlider;

		// update the chart
		setCanScrollZoomedChart(canScrollZoomedChart);
		setCanAutoZoomToSlider(canAutoZoomToSlider);

		// check the options
		fActionOptions.actionCanScrollZoomedChart.setChecked(canScrollZoomedChart);
		fActionOptions.actionCanAutoZoomToSlider.setChecked(canAutoZoomToSlider);

		// enable the options
		fActionOptions.actionCanScrollZoomedChart.setEnabled(true);
		fActionOptions.actionCanAutoZoomToSlider.setEnabled(true);
	}

	/**
	 * create the tour specific action, they are defined in the chart configuration
	 */
	private void fillToolbar() {

		if (fUseInternalActionBar == false) {
			return;
		}

		if (fActionOptions != null) {
			return;
		}

		final IToolBarManager tbm = getToolbarManager();

		fActionOptions = new ActionChartOptions(this, (ToolBarManager) tbm);

		/*
		 * add the actions to the toolbar
		 */

		tbm.add(new Separator());

		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_SPEED)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_ALTITUDE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_PULSE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_TEMPERATURE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_CADENCE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_ALTIMETER)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_GRADIENT)).getAction());
		tbm.add(new Separator());

		tbm.add(fActionProxies.get(COMMAND_ID_X_AXIS_TIME).getAction());
		tbm.add(fActionProxies.get(COMMAND_ID_X_AXIS_DISTANCE).getAction());
		tbm.add(new Separator());

		tbm.add(fActionOptions);
		tbm.add(new Separator());

		tbm.add(fActionZoomFitGraph);
		tbm.add(new Separator());

		// ///////////////////////////////////////////////////////

//		fActionGraphAnalyzer = new ActionGraphAnalyzer(this);
//
//		tbm.add(fActionGraphAnalyzer);
//
//		if (fIsToolActions) {
//
//			fActionTourSegmenter = new ActionTourSegmenter(this);
//			fActionMarkerEditor = new ActionTourMarker(this);
//			fActionAdjustAltitude = new ActionAdjustAltitude(this);
//
//			tbm.add(fActionAdjustAltitude);
//			tbm.add(fActionTourSegmenter);
//			tbm.add(fActionMarkerEditor);
//		}
	}

	/**
	 * fire a selection event for this tour chart
	 */
	public void fireTourChartSelection() {
		Object[] listeners = fSelectionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ITourChartSelectionListener listener = (ITourChartSelectionListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectedTourChart(new SelectionTourChart(TourChart.this));
				}
			});
		}
	}

	public TourData getTourData() {
		return fTourData;
	}

	// public boolean setFocus() {
	// }

	public boolean isTourDirty() {
		return fIsTourDirty;
	}

	public void removeTourChartListener(ITourChartSelectionListener listener) {
		fSelectionListeners.remove(listener);
	}

	/**
	 * Enable/disable the zoom options in the tour chart
	 * 
	 * @param isEnabled
	 */
	private void setEnabledZoomOptions(final boolean isEnabled) {

		fActionOptions.actionCanScrollZoomedChart.setEnabled(isEnabled);
		fActionOptions.actionCanAutoZoomToSlider.setEnabled(isEnabled);
	}

	/**
	 * set the graph layers
	 */
	private void setGraphLayers() {

		final ArrayList<IChartLayer> customLayers = new ArrayList<IChartLayer>();
		final ArrayList<IChartLayer> segmentValueLayers = new ArrayList<IChartLayer>();

		if (fSegmentLayer != null) {
			customLayers.add(fSegmentLayer);
		}

		if (fSegmentValueLayer != null) {
			segmentValueLayers.add(fSegmentValueLayer);
		}

		if (fMarkerLayer != null) {
			customLayers.add(fMarkerLayer);
		}

		ChartDataYSerie yData;

		yData = (ChartDataYSerie) fChartDataModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
		if (yData != null) {
			yData.setCustomLayers(customLayers);
		}

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSerieSpeed,
				TourManager.CUSTOM_DATA_SPEED);

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSerieGradient,
				TourManager.CUSTOM_DATA_GRADIENT);

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSerieAltimeter,
				TourManager.CUSTOM_DATA_ALTIMETER);

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSeriePulse,
				TourManager.CUSTOM_DATA_PULSE);
	}

	private boolean setMinDefaultValue(	final String property,
										boolean isChartModified,
										final String tagIsAltiMinEnabled,
										final String minValue,
										final int yDataInfoId) {

		if (property.equals(tagIsAltiMinEnabled) || property.equals(minValue)) {

			final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

			final boolean isAltMinEnabled = prefStore.getBoolean(tagIsAltiMinEnabled);

			final ArrayList<ChartDataYSerie> yDataList = fChartDataModel.getYData();

			// get altimeter data from all y-data
			ChartDataYSerie yData = null;
			for (final ChartDataYSerie yDataIterator : yDataList) {
				final Integer yDataInfo = (Integer) yDataIterator.getCustomData(ChartDataYSerie.YDATA_INFO);
				if (yDataInfo == yDataInfoId) {
					yData = yDataIterator;
				}
			}

			if (yData != null) {

				if (isAltMinEnabled) {

					// set to pref store min value
					final int altMinValue = prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);

					yData.setVisibleMinValue(altMinValue);

				} else {
					// reset to the original min value
					yData.setVisibleMinValue(yData.getOriginalMinValue());
				}

				isChartModified = true;
			}
		}
		return isChartModified;
	}

	private void setPrefListeners() {
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {
				final String property = event.getProperty();

				if (fTourChartConfig == null) {
					return;
				}

				boolean isChartModified = false;

				// test if the zoom preferences has changed
				if (property.equals(ITourbookPreferences.GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH)
						|| property.equals(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER)) {

					final IPreferenceStore prefStore = TourbookPlugin.getDefault()
							.getPreferenceStore();

					TourManager.updateZoomOptionsInChartConfig(fTourChartConfig, prefStore);

					isChartModified = true;
				}

				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					/*
					 * when the chart is computed, the changed colors are read from the preferences
					 */

					isChartModified = true;
				}

				isChartModified = setMinDefaultValue(property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTIMETER_MIN_ENABLED,
						ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
						TourManager.GRAPH_ALTIMETER);

				isChartModified = setMinDefaultValue(property,
						isChartModified,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
						TourManager.GRAPH_GRADIENT);

				if (isChartModified) {
					updateChart(true);
				}
			}
		};
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);

	}

	private void setSegmentLayer(	final ArrayList<IChartLayer> segmentValueLayers,
									float[] segmentSerie,
									String customDataKey) {

		ChartDataYSerie yData = (ChartDataYSerie) fChartDataModel.getCustomData(customDataKey);

		if (yData != null) {
			yData.setCustomLayers(segmentValueLayers);
			yData.setCustomData(SEGMENT_VALUES, segmentSerie);
		}
	}

//	/**
//	 * update the chart by getting the tourdata from the db
//	 * 
//	 * @param tourId
//	 */
//	void updateChart(final long tourId) {
//
//		// load the tourdata from the database
//		TourData tourData = TourDatabase.getTourDataByTourId(tourId);
//
//		if (tourData != null) {
//			updateChart(tourData, fTourChartConfig, true);
//		}
//	}

	/**
	 * set the tour dirty that the tour is saved when the tour is closed
	 */
	public void setTourDirty(boolean isDirty) {

		fIsTourDirty = isDirty;

		if (fTourModifyListener != null) {
			fTourModifyListener.tourIsModified();
		}
	}

	/**
	 * set the x-marker position listener
	 * 
	 * @param isPositionListenerEnabled
	 *        <code>true</code> sets the position listener, <code>false</code> disables the
	 *        listener
	 * @param synchDirection
	 * @param tourChartListener
	 */
	public void setZoomMarkerPositionListener(	final boolean isPositionListenerEnabled,
												final TourChart tourChartListener) {

		// set/disable the position listener in the listener provider
		super.setZoomMarkerPositionListener(isPositionListenerEnabled ? tourChartListener : null);

		/*
		 * when the position listener is set, the zoom action will be deactivated
		 */
		if (isPositionListenerEnabled) {

			// disable zoom actions
			tourChartListener.setEnabledZoomActions(false);
			tourChartListener.setEnabledZoomOptions(false);

			// set the synched chart to auto-zoom
			tourChartListener.setCanScrollZoomedChart(false);
			tourChartListener.setCanAutoZoomToSlider(true);

			// hide the x-sliders
			fIsXSliderVisible = tourChartListener.isXSliderVisible();
			tourChartListener.setShowSlider(false);

			fireZoomMarkerPositionListener();

		} else {

			// enable zoom action
			tourChartListener.fActionOptions.actionCanScrollZoomedChart.setChecked(tourChartListener.getCanScrollZoomedChart());
			tourChartListener.fActionOptions.actionCanAutoZoomToSlider.setChecked(tourChartListener.getCanAutoZoomToSlider());

			tourChartListener.setEnabledZoomActions(true);
			tourChartListener.setEnabledZoomOptions(true);

			// restore the x-sliders
			tourChartListener.setShowSlider(fIsXSliderVisible);

			// reset the x-position in the listener
			tourChartListener.setZoomMarkerPositionIn(null);
			tourChartListener.zoomOut(true);
		}
	}

	/**
	 * Enable/disable the graph action buttons, this visible state of a graph is defined in the
	 * chart config
	 */
	void updateActionState() {

		final ArrayList<Integer> visibleGraphs = fTourChartConfig.getVisibleGraphs();

		// enable/uncheck all GRAPH action
		for (final TourChartActionProxy actionProxy : fActionProxies.values()) {
			if (actionProxy.isGraphAction()) {
				actionProxy.setChecked(false);
				actionProxy.setEnabled(true);
			}
		}

		// check visible graph buttons
		for (final int graphId : visibleGraphs) {
			String proxyId = getProxyId(graphId);
			fActionProxies.get(proxyId).setChecked(true);
			fActionProxies.get(proxyId).setEnabled(true);
		}

		// update UI state
		if (fUseInternalActionBar == false) {
			TourChartActionHandlerManager.getInstance().updateUIState();
		}

//		fActionOptions.actionStartTimeOption.setEnabled(fTourChartConfig.showTimeOnXAxis);
//		fActionOptions.actionStartTimeOption.setChecked(fTourChartConfig.isStartTime);
//
//		fActionXAxesTime.setChecked(fTourChartConfig.showTimeOnXAxis);
//		fActionXAxesDistance.setChecked(!fTourChartConfig.showTimeOnXAxis);

//		enableZoomOptions();
	}

	/**
	 * Converts the graph Id into a proxy Id
	 * 
	 * @param graphId
	 * @return
	 */
	private String getProxyId(int graphId) {
		return "graphId." + Integer.toString(graphId);
	}

	/**
	 * update the chart with the current tour data and chart configuration
	 * 
	 * @param keepMinMaxValues
	 *        <code>true</code> will keep the min/max values from the previous chart
	 */
	public void updateChart(final boolean keepMinMaxValues) {
		updateChart(fTourData, fTourChartConfig, keepMinMaxValues);
	}

	/**
	 * Update the chart by providing new tourdata and chart configuration which is used to create
	 * the chart data model
	 * 
	 * @param tourData
	 * @param chartConfig
	 * @param keepMinMaxValues
	 *        when set to <code>true</code> keep the min/max values from the previous chart
	 */
	public void updateChart(final TourData tourData,
							final TourChartConfiguration chartConfig,
							final boolean keepMinMaxValues) {

		if (tourData == null || chartConfig == null) {
			return;
		}

		// keep min/max values for the 'old' chart in the chart config
		if (fTourChartConfig != null
				&& fTourChartConfig.getMinMaxKeeper() != null
				&& keepMinMaxValues) {
			fTourChartConfig.getMinMaxKeeper().saveMinMaxValues(fChartDataModel);
		}

		// set current tour data and chart config
		fTourData = tourData;
		fTourChartConfig = chartConfig;

		fChartDataModel = TourManager.getInstance().createChartDataModel(tourData, chartConfig);

		if (fShowActions) {
			createTourActionProxies();
			fillToolbar();
			updateActionState();
		}

		// restore min/max values from the chart config
		if (chartConfig.getMinMaxKeeper() != null && keepMinMaxValues) {
			chartConfig.getMinMaxKeeper().restoreMinMaxValues(fChartDataModel);
		}

		if (fChartDataModelListener != null) {
			fChartDataModelListener.dataModelChanged(fChartDataModel);
		}

		createSegmentsLayer();
		createMarkerLayer();

		setGraphLayers();
		setChartDataModel(fChartDataModel);
	}

	/**
	 * Updates the marker layer in the chart
	 * 
	 * @param showLayer
	 */
	public void updateMarkerLayer(final boolean showLayer) {

		if (showLayer) {
			createMarkerLayer();
		} else {
			fMarkerLayer = null;
		}

		setGraphLayers();
		updateChartLayers();
	}

	/**
	 * 
	 */
	public void updateSegmentLayer(final boolean showLayer) {

		fIsSegmentLayerVisible = showLayer;

		if (fIsSegmentLayerVisible) {
			createSegmentsLayer();
		} else {
			fSegmentLayer = null;
			fSegmentValueLayer = null;
			setGraphAlpha(0xD0);
		}

		setGraphLayers();
		updateChartLayers();

		/*
		 * the chart needs to be redrawn because the alpha for filling the chart has changed
		 */
		redrawChart();
	}

}
