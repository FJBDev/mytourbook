/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * Draws the graph and axis into the canvas
 * 
 * @author Wolfgang Schramm
 */
public class ChartComponentGraph extends Canvas {

	private static final int			TOUR_INFO_ICON_KEEP_OUT_AREA	= 50;

	private static final double			ZOOM_RATIO_FACTOR				= 1.3;

	private static final int			BAR_MARKER_WIDTH				= 16;

	private static final int[]			DOT_DASHES						= new int[] { 1, 1 };

	private static final NumberFormat	_nf								= NumberFormat.getNumberInstance();

	private static final RGB			_gridRGB						= new RGB(241, 239, 226);
	private static final RGB			_gridRGBMajor					= new RGB(222, 220, 208);

	private static final int[][]		_leftAccelerator				= new int[][] {
			{ -40, -200 },
			{ -30, -50 },
			{ -20, -10 },
			{ -10, -5 },
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			// !!! move 2 instead of 1, with 1 it would sometimes not move, needs more investigation
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			{ 0, -2 }													};

	private static final int[][]		_rightAccelerator				= new int[][] {
			{ 10, 2 },
			{ 20, 5 },
			{ 30, 10 },
			{ 40, 50 },
			{ Integer.MAX_VALUE, 200 }									};

	Chart								_chart;

	private final ChartComponents		_chartComponents;

	/**
	 * This image contains one single graph without title and x-axis with units.
	 * <p>
	 * This image was created to fix clipping bugs which occured when gradient filling was painted
	 * with a path.
	 */
	private Image						_chartImage10Graphs;

	/**
	 * This image contains the chart without additional layers.
	 */
	private Image						_chartImage20Chart;

	/**
	 * Contains custom layers like the markers or tour segments which are painted in the foreground.
	 */
	private Image						_chartImage30Custom;

	/**
	 * Contains layers like the x/y sliders, x-marker, selection or hovered line/bar.
	 */
	private Image						_chartImage40Overlay;

	/**
	 * 
	 */
	private ChartDrawingData			_chartDrawingData;

	/**
	 * drawing data which is used to draw the chart, when this list is empty, an error is displayed
	 */
	private ArrayList<GraphDrawingData>	_graphDrawingData				= new ArrayList<GraphDrawingData>();

	/**
	 * zoom ratio between the visible and the virtual chart width
	 */
	private double						_graphZoomRatio					= 1;

	/**
	 * Contains the width for a zoomed graph this includes also the invisible parts.
	 */
	private int							_xxDevGraphWidth;

	/**
	 * When the graph is zoomed, the chart shows only a part of the whole graph in the viewport.
	 * This value contains the left border of the viewport.
	 */
	private int							_xxDevViewPortLeftBorder;

	/**
	 * ratio for the position where the chart starts on the left side within the virtual graph width
	 */
	private double						_zoomRatioLeftBorder;

	/**
	 * ratio where the mouse was double clicked, this position is used to zoom the chart with the
	 * mouse
	 */
	private double						_zoomRatioCenter;

	/**
	 * when the slider is dragged and the mouse up event occures, the graph is zoomed to the sliders
	 * when set to <code>true</code>
	 */
	boolean								_canAutoZoomToSlider;

	/**
	 * when <code>true</code> the vertical sliders will be moved to the border when the chart is
	 * zoomed
	 */
	boolean								_canAutoMoveSliders;

	/**
	 * true indicates the graph needs to be redrawn in the paint event
	 */
	private boolean						_isChartDirty;

	/**
	 * true indicates the slider needs to be redrawn in the paint event
	 */
	private boolean						_isSliderDirty;

	/**
	 * when <code>true</code> the custom layers above the graph image needs to be redrawn in the
	 * next paint event
	 */
	private boolean						_isCustomLayerImageDirty;

	/**
	 * set to <code>true</code> when the selection needs to be redrawn
	 */
	private boolean						_isSelectionDirty;

	/**
	 * status for the x-slider, <code>true</code> indicates, the slider is visible
	 */
	private boolean						_isXSliderVisible;

	/**
	 * true indicates that the y-sliders is visible
	 */
	private boolean						_isYSliderVisible;

	/*
	 * chart slider
	 */
	private final ChartXSlider			_xSliderA;
	private final ChartXSlider			_xSliderB;

	/**
	 * xSliderDragged is set when the slider is being dragged, otherwise it is to <code>null</code>
	 */
	private ChartXSlider				_xSliderDragged;

	/**
	 * This is the slider which is drawn on top of the other, this is normally the last dragged
	 * slider
	 */
	private ChartXSlider				_xSliderOnTop;

	/**
	 * this is the slider which is below the top slider
	 */
	private ChartXSlider				_xSliderOnBottom;

	/**
	 * contains the x-slider when the mouse is over it, or <code>null</code> when the mouse is not
	 * over it
	 */
	private ChartXSlider				_mouseOverXSlider;

	/**
	 * Contains the slider which has the focus.
	 */
	private ChartXSlider				_selectedXSlider;

	/**
	 * Device position of the x-slider line when the slider is dragged. The position can be outside
	 * of the viewport which causes autoscrolling.
	 */
	private int							_devXDraggedXSliderLine;

	/**
	 * Mouse device position when autoscrolling is done with the mouse but without a x-slider
	 */
	private int							_devXAutoScrollMousePosition	= Integer.MIN_VALUE;

	/**
	 * list for all y-sliders
	 */
	private ArrayList<ChartYSlider>		_ySliders;

	/**
	 * contextLeftSlider is set when the right mouse button was clicked and the left slider was hit
	 */
	private ChartXSlider				_contextLeftSlider;

	/**
	 * contextRightSlider is set when the right mouse button was clicked and the right slider was
	 * hit
	 */
	private ChartXSlider				_contextRightSlider;

	/**
	 * cursor when the graph can be resizes
	 */
	private Cursor						_cursorResizeLeftRight;

	private Cursor						_cursorResizeTopDown;
	private Cursor						_cursorDragged;
	private Cursor						_cursorModeSlider;
	private Cursor						_cursorModeZoom;
	private Cursor						_cursorModeZoomMove;
	private Cursor						_cursorMove1x;
	private Cursor						_cursorMove2x;
	private Cursor						_cursorMove3x;
	private Cursor						_cursorMove4x;
	private Cursor						_cursorMove5x;
	private Cursor						_cursorXSliderLeft;
	private Cursor						_cursorXSliderRight;
	private Cursor						_cursorDragXSlider_ModeZoom;
	private Cursor						_cursorDragXSlider_ModeSlider;
	private Cursor						_cursorHoverXSlider;

	private Color						_gridColor;
	private Color						_gridColorMajor;

	/**
	 * serie index for the hovered bar, the bar is hidden when -1;
	 */
	private int							_hoveredBarSerieIndex			= -1;

	private int							_hoveredBarValueIndex;
	private boolean						_isHoveredBarDirty;

	private ToolTipV1					_hoveredBarToolTip;

	private boolean						_isHoveredLineVisible			= false;
	private int							_hoveredLineValueIndex			= -1;
	private ArrayList<Rectangle[]>		_lineFocusRectangles			= new ArrayList<Rectangle[]>();
	private ArrayList<Point[]>			_lineDevPositions				= new ArrayList<Point[]>();

	/**
	 * Tooltip for value points, can be <code>null</code> when not set.
	 */
	IValuePointToolTip					valuePointToolTip;

	private ChartYSlider				_hitYSlider;
	private ChartYSlider				_ySliderDragged;
	private int							_ySliderGraphX;

	private boolean						_isSetXSliderPositionLeft;
	private boolean						_isSetXSliderPositionRight;

	/**
	 * <code>true</code> when the x-marker is moved with the mouse
	 */
	private boolean						_isXMarkerMoved;

	/**
	 * x-position when the x-marker was started to drag
	 */
	private int							_devXMarkerDraggedStartPos;

	/**
	 * x-position when the x-marker is moved
	 */
	private int							_devXMarkerDraggedPos;

	private int							_movedXMarkerStartValueIndex;
	private int							_movedXMarkerEndValueIndex;

	private float						_xMarkerValueDiff;

	/**
	 * <code>true</code> when the chart is dragged with the mouse
	 */
	private boolean						_isChartDragged					= false;

	/**
	 * <code>true</code> when the mouse button in down but not moved
	 */
	private boolean						_isChartDraggedStarted			= false;

	private Point						_draggedChartStartPos;
	private Point						_draggedChartDraggedPos;

	private boolean[]					_selectedBarItems;

	private final int[]					_drawAsyncCounter				= new int[1];

	private boolean						_isAutoScroll;
	private boolean						_isDisableHoveredLineValueIndex;
	private int[]						_autoScrollCounter				= new int[1];

	private final ColorCache			_colorCache						= new ColorCache();

	private boolean						_isSelectionVisible;

	/**
	 * Is <code>true</code> when this chart gained the focus, <code>false</code> when the focus is
	 * lost.
	 */
	private boolean						_isFocusActive;

	private boolean						_isOverlayDirty;

	/*
	 * position of the mouse in the mouse down event
	 */
	private int							_devXMouseDown;
	private int							_devYMouseDown;
	private int							_devXMouseMove;
	private int							_devYMouseMove;

	private boolean						_isPaintDraggedImage			= false;

	/**
	 * is <code>true</code> when data for a graph is available
	 */
	private boolean						_isGraphVisible					= false;

	/**
	 * Client area for this canvas
	 */
	Rectangle							_clientArea;

	private long						_mouseTimeExit;
	private boolean						_isMouseMovedFromGraph;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the parent of this control.
	 * @param style
	 *            the style of this control.
	 */
	ChartComponentGraph(final Chart chartWidget, final Composite parent, final int style) {

		// create composite with horizontal scrollbars
		super(parent, SWT.H_SCROLL | SWT.NO_BACKGROUND);

		_chart = chartWidget;

		_cursorResizeLeftRight = new Cursor(getDisplay(), SWT.CURSOR_SIZEWE);
		_cursorResizeTopDown = new Cursor(getDisplay(), SWT.CURSOR_SIZENS);
		_cursorDragged = new Cursor(getDisplay(), SWT.CURSOR_SIZEALL);

		_cursorModeSlider = createCursorFromImage(Messages.Image_cursor_mode_slider);
		_cursorModeZoom = createCursorFromImage(Messages.Image_cursor_mode_zoom);
		_cursorModeZoomMove = createCursorFromImage(Messages.Image_cursor_mode_zoom_move);
		_cursorDragXSlider_ModeZoom = createCursorFromImage(Messages.Image_Cursor_DragXSlider_ModeZoom);
		_cursorDragXSlider_ModeSlider = createCursorFromImage(Messages.Image_Cursor_DragXSlider_ModeSlider);
		_cursorHoverXSlider = createCursorFromImage(Messages.Image_Cursor_Hover_XSlider);

		_cursorMove1x = createCursorFromImage(Messages.Image_Cursor_Move1x);
		_cursorMove2x = createCursorFromImage(Messages.Image_Cursor_Move2x);
		_cursorMove3x = createCursorFromImage(Messages.Image_Cursor_Move3x);
		_cursorMove4x = createCursorFromImage(Messages.Image_Cursor_Move4x);
		_cursorMove5x = createCursorFromImage(Messages.Image_Cursor_Move5x);

		_cursorXSliderLeft = createCursorFromImage(Messages.Image_Cursor_X_Slider_Left);
		_cursorXSliderRight = createCursorFromImage(Messages.Image_Cursor_X_Slider_Right);

		_gridColor = new Color(getDisplay(), _gridRGB);
		_gridColorMajor = new Color(getDisplay(), _gridRGBMajor);

		_chartComponents = (ChartComponents) parent;

		// setup the x-slider
		_xSliderA = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_LEFT);
		_xSliderB = new ChartXSlider(this, Integer.MIN_VALUE, ChartXSlider.SLIDER_TYPE_RIGHT);

		_xSliderOnTop = _xSliderB;
		_xSliderOnBottom = _xSliderA;

		_hoveredBarToolTip = new ToolTipV1(_chart);

		addListener();
		createContextMenu();

		final Point devMouse = this.toControl(getDisplay().getCursorLocation());
		setCursorStyle(devMouse.y);
	}

	/**
	 * execute the action which is defined when a bar is selected with the left mouse button
	 */
	private void actionSelectBars() {

		if (_hoveredBarSerieIndex < 0) {
			return;
		}

		boolean[] selectedBarItems;

		if (_graphDrawingData.size() == 0) {
			selectedBarItems = null;
		} else {

			final GraphDrawingData graphDrawingData = _graphDrawingData.get(0);
			final ChartDataXSerie xData = graphDrawingData.getXData();

			selectedBarItems = new boolean[xData._highValues[0].length];
			selectedBarItems[_hoveredBarValueIndex] = true;
		}

		setSelectedBars(selectedBarItems);

		_chart.fireBarSelectionEvent(_hoveredBarSerieIndex, _hoveredBarValueIndex);
	}

	/**
	 * hookup all listeners
	 */
	private void addListener() {

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {

				if (_isChartDragged) {
					drawSync020DraggedChart(event.gc);
				} else {

//					final long start = System.nanoTime();

					drawSync000onPaint(event.gc);

//					System.out.println("onPaint\t" + (((double) System.nanoTime() - start) / 1000000) + "ms");
//					// TODO remove SYSTEM.OUT.PRINTLN
				}
			}
		});

		// horizontal scrollbar
		final ScrollBar horizontalBar = getHorizontalBar();
		horizontalBar.setEnabled(false);
		horizontalBar.setVisible(false);
		horizontalBar.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				onScroll(event);
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseMove(e.x, e.y);
				}
			}
		});

		addMouseListener(new MouseListener() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseDoubleClick(e);
				}
			}

			public void mouseDown(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseDown(e);
				}
			}

			public void mouseUp(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseUp(e);
				}
			}
		});

		addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseEnter(e);
				}
			}

			public void mouseExit(final MouseEvent e) {
				if (_isGraphVisible) {
					onMouseExit(e);
				}
			}

			public void mouseHover(final MouseEvent e) {}
		});

		addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(final Event event) {
				onMouseWheel(event);
			}
		});

		addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {

				setFocusToControl();

				_isFocusActive = true;
				_isSelectionDirty = true;
				redraw();
			}

			public void focusLost(final FocusEvent e) {
				_isFocusActive = false;
				_isSelectionDirty = true;
				redraw();
			}
		});

		addListener(SWT.Traverse, new Listener() {
			public void handleEvent(final Event event) {

				switch (event.detail) {
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					event.doit = true;
					break;
				}
			}
		});

		addControlListener(new ControlListener() {

			@Override
			public void controlMoved(final ControlEvent e) {}

			@Override
			public void controlResized(final ControlEvent e) {

				_clientArea = getClientArea();

				_isDisableHoveredLineValueIndex = true;
			}
		});

		addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(final Event event) {
				onKeyDown(event);
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

	}

	private void adjustYSlider() {

		/*
		 * check if the y slider was outside of the bounds, recompute the chart when necessary
		 */

		final GraphDrawingData drawingData = _ySliderDragged.getDrawingData();

		final ChartDataYSerie yData = _ySliderDragged.getYData();
		final ChartYSlider slider1 = yData.getYSlider1();
		final ChartYSlider slider2 = yData.getYSlider2();

		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.devGraphHeight;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float scaleY = drawingData.getScaleY();

		final int devYSliderLine1 = slider1.getDevYSliderLine();
		final int devYSliderLine2 = slider2.getDevYSliderLine();

		final float graphValue1 = ((float) devYBottom - devYSliderLine1) / scaleY + graphYBottom;
		final float graphValue2 = ((float) devYBottom - devYSliderLine2) / scaleY + graphYBottom;

		// get value which was adjusted
		if (_ySliderDragged == slider1) {
			yData.adjustedYValue = graphValue1;
		} else if (_ySliderDragged == slider2) {
			yData.adjustedYValue = graphValue2;
		} else {
			// this case should not happen
			System.out.println("y-slider is not set correctly\t");//$NON-NLS-1$
			return;
		}

		float minValue;
		float maxValue;

		if (graphValue1 < graphValue2) {

			minValue = graphValue1;
			maxValue = graphValue2;

			// position the lower slider to the bottom of the chart
			slider1.setDevYSliderLine(devYBottom);
			slider2.setDevYSliderLine(devYTop);

		} else {

			// graphValue1 >= graphValue2

			minValue = graphValue2;
			maxValue = graphValue1;

			// position the upper slider to the top of the chart
			slider1.setDevYSliderLine(devYTop);
			slider2.setDevYSliderLine(devYBottom);
		}
		yData.setVisibleMinValue(minValue);
		yData.setVisibleMaxValue(maxValue);

		_ySliderDragged = null;

		// the cursor could be outside of the chart, reset it
//		setCursorStyle();

		/*
		 * the hited slider could be outsite of the chart, hide the labels on the slider
		 */
		_hitYSlider = null;

		/*
		 * when the chart is synchronized, the y-slider position is modified, so we overwrite the
		 * synchronized chart y-slider positions until the zoom in marker is overwritten
		 */
		final SynchConfiguration synchedChartConfig = _chartComponents._synchConfigSrc;

		if (synchedChartConfig != null) {

			final ChartYDataMinMaxKeeper synchedChartMinMaxKeeper = synchedChartConfig.getYDataMinMaxKeeper();

			// get the id for the changed y-slider
			final Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);

			// adjust min value for the changed y-slider
			final Float synchedChartMinValue = synchedChartMinMaxKeeper.getMinValues().get(yDataInfo);

			if (synchedChartMinValue != null) {
				synchedChartMinMaxKeeper.getMinValues().put(yDataInfo, minValue);
			}

			// adjust max value for the changed y-slider
			final Float synchedChartMaxValue = synchedChartMinMaxKeeper.getMaxValues().get(yDataInfo);

			if (synchedChartMaxValue != null) {
				synchedChartMinMaxKeeper.getMaxValues().put(yDataInfo, maxValue);
			}
		}

		computeChart();
	}

	/**
	 * when the chart was modified, recompute all
	 */
	private void computeChart() {
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					_chartComponents.onResize();
				}
			}
		});
	}

	private void computeSliderForContextMenu(final int devX, final int devY) {

		ChartXSlider slider1 = null;
		ChartXSlider slider2 = null;

		// reset the context slider
		_contextLeftSlider = null;
		_contextRightSlider = null;

		// check if a slider or the slider line was hit
		if (_xSliderA.getHitRectangle().contains(devX, devY)) {
			slider1 = _xSliderA;
		}

		if (_xSliderB.getHitRectangle().contains(devX, devY)) {
			slider2 = _xSliderB;
		}

		/*
		 * check if a slider was hit
		 */
		if (slider1 == null && slider2 == null) {
			// no slider was hit
			return;
		}

		/*
		 * check if one slider was hit, when yes, the leftslider is set and the right slider is null
		 */
		if (slider1 != null && slider2 == null) {
			// only slider 1 was hit
			_contextLeftSlider = slider1;
			return;
		}
		if (slider2 != null && slider1 == null) {
			// only slider 2 was hit
			_contextLeftSlider = slider2;
			return;
		}

		/*
		 * both sliders are hit
		 */
		final int xSlider1Position = slider1.getHitRectangle().x;
		final int xSlider2Position = slider2.getHitRectangle().x;

		if (xSlider1Position == xSlider2Position) {
			// both sliders are at the same position
			_contextLeftSlider = slider1;
			return;
		}
		if (xSlider1Position < xSlider2Position) {
			_contextLeftSlider = slider1;
			_contextRightSlider = slider2;
		} else {
			_contextLeftSlider = slider2;
			_contextRightSlider = slider1;
		}
	}

	private int computeXMarkerValue(final float[] xValues,
									final int xmStartIndex,
									final float valueDiff,
									final float valueXMarkerPosition) {

		int valueIndex;
		float valueX = xValues[xmStartIndex];
		float valueHalf;

		/*
		 * get the marker positon for the next value
		 */
		if (valueDiff > 0) {

			// moved to the right

			for (valueIndex = xmStartIndex; valueIndex < xValues.length; valueIndex++) {

				valueX = xValues[valueIndex];
				valueHalf = ((valueX - xValues[Math.min(valueIndex + 1, xValues.length - 1)]) / 2);

				if (valueX >= valueXMarkerPosition + valueHalf) {
					break;
				}
			}

		} else {

			// moved to the left

			for (valueIndex = xmStartIndex; valueIndex >= 0; valueIndex--) {

				valueX = xValues[valueIndex];
				valueHalf = ((valueX - xValues[Math.max(0, valueIndex - 1)]) / 2);

				if (valueX < valueXMarkerPosition + valueHalf) {
					break;
				}
			}
		}

		return Math.max(0, Math.min(valueIndex, xValues.length - 1));
	}

	/**
	 * Get the x-axis value according to the slider position in the UI
	 * 
	 * @param slider
	 * @param devXSliderLinePosition
	 */
	void computeXSliderValue(final ChartXSlider slider, final int devXSliderLinePosition) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final float[][] xValueSerie = xData.getHighValues();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		if (_hoveredLineValueIndex == -1) {
			// this should not happen
			return;
		}

		final float[] xDataValues = xValueSerie[0];

		slider.setValuesIndex(_hoveredLineValueIndex);
		slider.setValueX(xDataValues[_hoveredLineValueIndex]);
	}

	/**
	 * Get the x-axis value according to the slider position in the UI
	 * 
	 * @param slider
	 * @param devXSliderLinePosition
	 */
	void computeXSliderValueOLD(final ChartXSlider slider, final int devXSliderLinePosition) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final float[][] xValueSerie = xData.getHighValues();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final float[] xDataValues = xValueSerie[0];
		final int serieLength = xDataValues.length;
		final int maxIndex = Math.max(0, serieLength - 1);

		/*
		 * The non time value (distance) is not linear, the value is increasing steadily but with
		 * different distance on the x axis. So first we have to find the nearest position in the
		 * values array and then interpolite from the found position to the slider position
		 */

		final float minValue = xData.getOriginalMinValue();
		final float maxValue = xData.getOriginalMaxValue();
		final float valueRange = maxValue > 0 ? (maxValue - minValue) : -(minValue - maxValue);

		final double positionRatio = (double) devXSliderLinePosition / _xxDevGraphWidth;
		int valueIndex = (int) (positionRatio * serieLength);

		// check array bounds
		valueIndex = Math.min(valueIndex, maxIndex);
		valueIndex = Math.max(valueIndex, 0);

		// sliderIndex points into the value array for the current slider position
		float xDataValue = xDataValues[valueIndex];

		// compute the value for the slider on the x-axis
		final double sliderValue = positionRatio * valueRange;

		if (xDataValue == sliderValue) {

			// nothing to do

		} else if (sliderValue > xDataValue) {

			/*
			 * in the value array move towards the end to find the position where the value of the
			 * slider corresponds with the value in the value array
			 */

			while (sliderValue > xDataValue) {

				xDataValue = xDataValues[valueIndex++];

				// check if end of the x-data are reached
				if (valueIndex == serieLength) {
					break;
				}
			}
			valueIndex--;
			xDataValue = xDataValues[valueIndex];

		} else {

			/*
			 * xDataValue > sliderValue
			 */

			while (sliderValue < xDataValue) {

				// check if beginning of the x-data are reached
				if (valueIndex == 0) {
					break;
				}

				xDataValue = xDataValues[--valueIndex];
			}
		}

		/*
		 * This is a bit of a hack because at some positions the value is too small. Solving the
		 * problem in the algorithm would take more time than using this hack.
		 */
		if (xDataValue < sliderValue) {
			valueIndex++;
		}

		// check array bounds
		valueIndex = Math.min(valueIndex, maxIndex);
		xDataValue = xDataValues[valueIndex];

		// !!! debug values !!!
//		xValue = valueIndex * 1000;
//		xValue = (int) (slider.getPositionRatio() * 1000000000);

		slider.setValuesIndex(valueIndex);
		slider.setValueX(xDataValue);
	}

	/**
	 * create the context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				actionSelectBars();

				_hoveredBarToolTip.toolTip20Hide();

				_chart.fillContextMenu(
						menuMgr,
						_contextLeftSlider,
						_contextRightSlider,
						_hoveredBarSerieIndex,
						_hoveredBarValueIndex,
						_devXMouseDown,
						_devYMouseDown);
			}
		});

		final Menu contextMenu = menuMgr.createContextMenu(this);

		contextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_chart.onHideContextMenu(e, ChartComponentGraph.this);
			}

			@Override
			public void menuShown(final MenuEvent e) {
				_chart.onShowContextMenu(e, ChartComponentGraph.this);
			}
		});

		setMenu(contextMenu);
	}

	/**
	 * Create a cursor resource from an image file
	 * 
	 * @param imageName
	 * @return
	 */
	private Cursor createCursorFromImage(final String imageName) {

		Image cursorImage = null;
		final ImageDescriptor imageDescriptor = Activator.getImageDescriptor(imageName);

		if (imageDescriptor == null) {

			final String resourceName = "icons/" + imageName;//$NON-NLS-1$

			final ClassLoader classLoader = getClass().getClassLoader();

			final InputStream imageStream = classLoader == null
					? ClassLoader.getSystemResourceAsStream(resourceName)
					: classLoader.getResourceAsStream(resourceName);

			if (imageStream == null) {
				return null;
			}

			cursorImage = new Image(Display.getCurrent(), imageStream);

		} else {

			cursorImage = imageDescriptor.createImage();
		}

		final Cursor cursor = new Cursor(getDisplay(), cursorImage.getImageData(), 0, 0);

		cursorImage.dispose();

		return cursor;
	}

	/**
	 * Creates the label(s) and the position for each graph
	 * 
	 * @param gc
	 * @param xSlider
	 */
	private void createXSliderLabel(final GC gc, final ChartXSlider xSlider) {

		final int devSliderLinePos = xSlider.getXXDevSliderLinePos() - _xxDevViewPortLeftBorder;

		int sliderValuesIndex = xSlider.getValuesIndex();
		// final int valueX = slider.getValueX();

		final ArrayList<ChartXSliderLabel> labelList = new ArrayList<ChartXSliderLabel>();
		xSlider.setLabelList(labelList);

		final ScrollBar hBar = getHorizontalBar();
		final int hBarOffset = hBar.isVisible() ? hBar.getSelection() : 0;

		final int leftPos = hBarOffset;
		final int rightPos = leftPos + getDevVisibleChartWidth();

		// create slider label for each graph
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();
			final int labelFormat = yData.getSliderLabelFormat();
			final int valueDivisor = yData.getValueDivisor();

			if (labelFormat == ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS) {

				// format: mm:ss

			} else {

				// use default format: ChartDataYSerie.SLIDER_LABEL_FORMAT_DEFAULT

				if (valueDivisor == 1) {
					_nf.setMinimumFractionDigits(0);
				} else if (valueDivisor == 10) {
					_nf.setMinimumFractionDigits(1);
				}
			}

			final ChartXSliderLabel label = new ChartXSliderLabel();
			labelList.add(label);

			// draw label on the left or on the right side of the slider,
			// depending on the slider position
			final float[] yValues = yData.getHighValues()[0];

			// make sure the slider value index is not of bounds, this can
			// happen when the data have changed
			sliderValuesIndex = Math.min(sliderValuesIndex, yValues.length - 1);

			final float yValue = yValues[sliderValuesIndex];
			// final int xAxisUnit = xData.getAxisUnit();
			final StringBuilder labelText = new StringBuilder();

			// create the slider text
			if (labelFormat == ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS) {

				// format: mm:ss

				labelText.append(Util.format_mm_ss((long) yValue));

			} else {

				// use default format: ChartDataYSerie.SLIDER_LABEL_FORMAT_DEFAULT

				if (valueDivisor == 1) {
					labelText.append(_nf.format(yValue));
				} else {
					labelText.append(_nf.format(yValue / valueDivisor));
				}
			}

			labelText.append(' ');
			labelText.append(yData.getUnitLabel());
			labelText.append(' ');

			// calculate position of the slider label
			final Point labelExtend = gc.stringExtent(labelText.toString());
			final int labelWidth = labelExtend.x + 0;
			int labelXPos = devSliderLinePos - labelWidth / 2;

			final int labelRightPos = labelXPos + labelWidth;

			if (xSlider == _xSliderDragged) {
				/*
				 * current slider is the dragged slider, clip the slider label position at the
				 * viewport
				 */
				if (labelXPos < leftPos) {
					labelXPos += (leftPos - labelXPos);
				} else if (labelRightPos >= rightPos) {
					labelXPos = rightPos - labelWidth - 1;
				}

			} else {
				/*
				 * current slider is not dragged, clip the slider label position at the chart bounds
				 */
				if (labelXPos < 0) {

					labelXPos = 0;

				} else {

					/*
					 * show the whole label when the slider is on the right border
					 */
					if (labelRightPos > getDevVisibleChartWidth()) {
						labelXPos = getDevVisibleChartWidth() - labelWidth - 1;
					}
				}
			}

			label.text = labelText.toString();

			label.height = labelExtend.y - 5;
			label.width = labelWidth;

			label.x = labelXPos;
			label.y = drawingData.getDevYBottom() - drawingData.devGraphHeight - label.height;

			/*
			 * get the y position of the marker which marks the y value in the graph
			 */
			int devYGraph = drawingData.getDevYBottom()
					- (int) ((yValue - drawingData.getGraphYBottom()) * drawingData.getScaleY());

			if (yValue < yData.getVisibleMinValue()) {
				devYGraph = drawingData.getDevYBottom();
			}
			if (yValue > yData.getVisibleMaxValue()) {
				devYGraph = drawingData.getDevYTop();
			}
			label.devYGraph = devYGraph;
		}
	}

	void disposeColors() {
		_colorCache.dispose();
	}

	private void doAutoScroll() {

// this is not working the mouse can't sometime not be zoomed to the border, depending on the mouse speed
//		/*
//		 * check if the mouse has reached the left or right border
//		 */
//		if (_graphDrawingData == null
//				|| _graphDrawingData.size() == 0
//				|| _hoveredLineValueIndex == 0
//				|| _hoveredLineValueIndex == _graphDrawingData.get(0).getXData()._highValues.length - 1) {
//
//			_isAutoScroll = false;
//
//			return;
//		}

		final int AUTO_SCROLL_INTERVAL = 20; // 20ms == 25fps

		_isAutoScroll = true;

		_autoScrollCounter[0]++;

		getDisplay().timerExec(AUTO_SCROLL_INTERVAL, new Runnable() {

			final int	__runnableScrollCounter	= _autoScrollCounter[0];

			public void run() {

				if (__runnableScrollCounter != _autoScrollCounter[0]) {
					return;
				}

				if (isDisposed()
						|| _isAutoScroll == false
						|| (_xSliderDragged == null && _devXAutoScrollMousePosition == Integer.MIN_VALUE)) {

					// make sure that autoscroll/automove is disabled

					_isAutoScroll = false;
					_devXAutoScrollMousePosition = Integer.MIN_VALUE;

					return;
				}

				_isDisableHoveredLineValueIndex = true;

				/*
				 * the offset values are determined experimentally and depends on the mouse position
				 */
				int devMouseOffset = 0;

				if (_devXAutoScrollMousePosition != Integer.MIN_VALUE) {

					if (_devXAutoScrollMousePosition < 0) {

						// autoscroll the graph to the left

						for (final int[] accelerator : _leftAccelerator) {
							if (_devXAutoScrollMousePosition < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}

					} else {

						// autoscroll the graph to the right

						final int devXAutoScrollMousePositionRelative = _devXAutoScrollMousePosition
								- getDevVisibleChartWidth();

						for (final int[] accelerator : _rightAccelerator) {
							if (devXAutoScrollMousePositionRelative < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}
					}

					doAutoScroll10RunnableScrollGraph(this, AUTO_SCROLL_INTERVAL, devMouseOffset);

				} else {

					if (_devXDraggedXSliderLine < 0) {

						// move x-slider to the left

						for (final int[] accelerator : _leftAccelerator) {
							if (_devXDraggedXSliderLine < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}

					} else {

						// move x-slider to the right

						final int devXSliderLineRelative = _devXDraggedXSliderLine - getDevVisibleChartWidth();

						for (final int[] accelerator : _rightAccelerator) {
							if (devXSliderLineRelative < accelerator[0]) {
								devMouseOffset = accelerator[1];
								break;
							}
						}
					}

					doAutoScroll20RunnableMoveSlider(this, AUTO_SCROLL_INTERVAL, devMouseOffset);
				}
			}
		});
	}

	private void doAutoScroll10RunnableScrollGraph(	final Runnable runnable,
													final int autoScrollInterval,
													final int devMouseOffset) {

		final int xxDevNewPosition = _xxDevViewPortLeftBorder + devMouseOffset;

		// reposition chart
		setChartPosition(xxDevNewPosition);

		// check if scrolling can be redone
		final boolean isRepeatScrollingLeft = _xxDevViewPortLeftBorder > 1;
		final boolean isRepeatScrollingRight = xxDevNewPosition < _xxDevGraphWidth;
		final boolean isRepeatScrolling = isRepeatScrollingLeft || isRepeatScrollingRight;

		// start scrolling again when the bounds have not been reached
		if (isRepeatScrolling) {
			getDisplay().timerExec(autoScrollInterval, runnable);
		} else {
			_isAutoScroll = false;
		}
	}

	private void doAutoScroll20RunnableMoveSlider(	final Runnable runnable,
													final int autoScrollInterval,
													final int devMouseOffset) {

		// get new slider position
		final int xxDevOldSliderLinePos = _xSliderDragged.getXXDevSliderLinePos();
		final int xxDevNewSliderLinePos2 = xxDevOldSliderLinePos + devMouseOffset;
		final int xxDevNewSliderLinePos3 = xxDevNewSliderLinePos2 - _xxDevViewPortLeftBorder;

		// move the slider
		moveXSlider(_xSliderDragged, xxDevNewSliderLinePos3);

		// redraw slider
		_isSliderDirty = true;
		redraw();

		// redraw chart
		setChartPosition(_xSliderDragged, false);

		final boolean isRepeatScrollingLeft = _xxDevViewPortLeftBorder > 1;
		final boolean isRepeatScrollingRight = _xxDevViewPortLeftBorder + xxDevNewSliderLinePos3 < _xxDevGraphWidth;
		final boolean isRepeatScrolling = isRepeatScrollingLeft || isRepeatScrollingRight;

		// start scrolling again when the bounds have not been reached
		if (isRepeatScrolling) {
			getDisplay().timerExec(autoScrollInterval, runnable);
		} else {
			_isAutoScroll = false;
		}
	}

	private void doAutoZoomToXSliders() {

		if (_canAutoZoomToSlider) {

			// the graph can't be scrolled but the graph should be
			// zoomed to the x-slider positions

			// zoom into the chart
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!isDisposed()) {
						_chart.onExecuteZoomInWithSlider();
					}
				}
			});
		}
	}

	/**
	 * Draw the graphs into the chart+graph image
	 */
	private void drawAsync100StartPainting() {

		_drawAsyncCounter[0]++;

		getDisplay().asyncExec(new Runnable() {

			final int	__runnableBgCounter	= _drawAsyncCounter[0];

			public void run() {

//				final long startTime = System.nanoTime();
//				// TODO remove SYSTEM.OUT.PRINTLN

				/*
				 * create the chart image only when a new onPaint event has not occured
				 */
				if (__runnableBgCounter != _drawAsyncCounter[0]) {
					// a new onPaint event occured
					return;
				}

				if (isDisposed()) {
					// this widget is disposed
					return;
				}

				if (_graphDrawingData.size() == 0) {
					// drawing data are not set
					return;
				}

				// ensure minimum size
				final int devNewImageWidth = Math.max(ChartComponents.CHART_MIN_WIDTH, getDevVisibleChartWidth());

				/*
				 * the image size is adjusted to the client size but it must be within the min/max
				 * ranges
				 */
				final int devNewImageHeight = Math.max(
						ChartComponents.CHART_MIN_HEIGHT,
						Math.min(getDevVisibleGraphHeight(), ChartComponents.CHART_MAX_HEIGHT));

				/*
				 * when the image is the same size as the new we will redraw it only if it is set to
				 * dirty
				 */
				if (_isChartDirty == false && _chartImage20Chart != null) {

					final Rectangle oldBounds = _chartImage20Chart.getBounds();

					if (oldBounds.width == devNewImageWidth && oldBounds.height == devNewImageHeight) {
						return;
					}
				}

				final Rectangle chartImageRect = new Rectangle(0, 0, devNewImageWidth, devNewImageHeight);

				// ensure correct image size
				if (chartImageRect.width <= 0 || chartImageRect.height <= 0) {
					return;
				}

				// create image on which the graph is drawn
				if (Util.canReuseImage(_chartImage20Chart, chartImageRect) == false) {
					_chartImage20Chart = Util.createImage(getDisplay(), _chartImage20Chart, chartImageRect);
				}

				/*
				 * The graph image is only a part where ONE single graph is painted without any
				 * title or unit tick/values
				 */
				final int devGraphHeight = _graphDrawingData.get(0).devGraphHeight;
				final Rectangle graphImageRect = new Rectangle(0, 0, //
						devNewImageWidth,
						devGraphHeight < 1 ? 1 : devGraphHeight + 1); // ensure valid height

				if (Util.canReuseImage(_chartImage10Graphs, graphImageRect) == false) {
					_chartImage10Graphs = Util.createImage(getDisplay(), _chartImage10Graphs, graphImageRect);
				}

				// create chart context
				final GC gcChart = new GC(_chartImage20Chart);
				final GC gcGraph = new GC(_chartImage10Graphs);
				{
					gcChart.setFont(_chart.getFont());

					// fill background
					gcChart.setBackground(_chart.getBackgroundColor());
					gcChart.fillRectangle(_chartImage20Chart.getBounds());

					if (_chartComponents.errorMessage == null) {

						drawAsync110GraphImage(gcChart, gcGraph);

					} else {

						// an error was set in the chart data model
						drawSyncBg999ErrorMessage(gcChart);
					}
				}
				gcChart.dispose();
				gcGraph.dispose();

				// remove dirty status
				_isChartDirty = false;

				// dragged image will be painted until the graph image is recomputed
				_isPaintDraggedImage = false;

				// force the overlay image to be redrawn
				_isOverlayDirty = true;

				redraw();

//				final long endTime = System.nanoTime();
//				System.out.println("draw100ChartImage: "
//						+ (((double) endTime - startTime) / 1000000)
//						+ " ms   #:"
//						+ _drawCounter[0]);
//				// TODO remove SYSTEM.OUT.PRINTLN
			}
		});
	}

	/**
	 * Draw all graphs, each graph is painted in the same canvas (gcGraph) which is painted in the
	 * the chart image (gcChart).
	 * 
	 * @param gcChart
	 * @param gcGraph
	 */
	private void drawAsync110GraphImage(final GC gcChart, final GC gcGraph) {

		int graphIndex = 0;
		final int lastGraphIndex = _graphDrawingData.size() - 1;

		// reset line positions, they are set when a line graph is painted
		_lineDevPositions.clear();
		_lineFocusRectangles.clear();

		final Color chartBackgroundColor = _chart.getBackgroundColor();
		final Rectangle graphBounds = _chartImage10Graphs.getBounds();

		// loop: all graphs in the chart
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			// fill background
			gcGraph.setBackground(chartBackgroundColor);
			gcGraph.fillRectangle(graphBounds);

			final int chartType = drawingData.getChartType();

			if (graphIndex == 0) {
				drawAsync200XTitle(gcChart, drawingData);
			}

			drawAsync150Segments(gcGraph, drawingData);

			if (graphIndex == lastGraphIndex) {
				// draw the unit label and unit tick for the last graph
				drawAsync210XUnitsAndVGrid(gcChart, gcGraph, drawingData, true);
			} else {
				drawAsync210XUnitsAndVGrid(gcChart, gcGraph, drawingData, false);
			}

			// draw only the horizontal grid
			drawAsync220XAsisHGrid(gcGraph, drawingData, false);

			if (chartType == ChartDataModel.CHART_TYPE_LINE || chartType == ChartDataModel.CHART_TYPE_LINE) {}

			// draw units and grid on the x and y axis
			switch (chartType) {
			case ChartDataModel.CHART_TYPE_LINE:
				drawAsync500LineGraph(gcGraph, drawingData);
				drawAsync520RangeMarker(gcGraph, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_BAR:
				drawAsync530BarGraph(gcGraph, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_LINE_WITH_BARS:
				drawAsync540LineWithBarGraph(gcGraph, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_XY_SCATTER:
				drawAsync550XYScatter(gcGraph, drawingData);
				break;

			default:
				break;
			}

			// draw only the x-axis, this is drawn lately because the graph can overwrite it
			drawAsync220XAsisHGrid(gcGraph, drawingData, true);

			// draw graph image into the chart image
			gcChart.drawImage(_chartImage10Graphs, 0, drawingData.getDevYTop());

			graphIndex++;
		}

		if (valuePointToolTip != null) {

			if (_hoveredLineValueIndex == -1) {
				valuePointToolTip.hide();
			} else {
				valuePointToolTip.setValueIndex(_hoveredLineValueIndex, _devXMouseMove, _devYMouseMove);
			}
		}
	}

	private void drawAsync150Segments(final GC gc, final GraphDrawingData drawingData) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();

		if (chartSegments == null) {
			return;
		}

		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

		final float scaleX = drawingData.getScaleX();

		final int[] startValues = chartSegments.valueStart;
		final int[] endValues = chartSegments.valueEnd;

		if (startValues == null || endValues == null) {
			return;
		}

		final Color alternateColor = new Color(gc.getDevice(), 0xf5, 0xf5, 0xf5); // efefef

		for (int segmentIndex = 0; segmentIndex < startValues.length; segmentIndex++) {

			if (segmentIndex % 2 == 1) {

				// draw segment background color for every second segment

				final int startValue = startValues[segmentIndex];
				final int endValue = endValues[segmentIndex];

				final int devXValueStart = (int) (scaleX * startValue) - _xxDevViewPortLeftBorder;

				// adjust endValue to fill the last part of the segment
				final int devValueEnd = (int) (scaleX * (endValue + 1)) - _xxDevViewPortLeftBorder;

				gc.setBackground(alternateColor);
				gc.fillRectangle(//
						devXValueStart,
						devYTop,
						devValueEnd - devXValueStart,
						devYBottom - devYTop);
			}
		}

		alternateColor.dispose();
	}

	private void drawAsync200XTitle(final GC gc, final GraphDrawingData drawingData) {

		final ChartSegments chartSegments = drawingData.getXData().getChartSegments();
		final int devYTitle = _chartDrawingData.devMarginTop;

		final int devGraphWidth = _chartComponents.getDevVisibleChartWidth();

		if (chartSegments == null) {

			/*
			 * draw default title, center within the chart
			 */

			final String title = drawingData.getXTitle();

			if (title == null || title.length() == 0) {
				return;
			}

			final int titleWidth = gc.textExtent(title).x;
			final int devXTitle = (devGraphWidth / 2) - (titleWidth / 2);

			gc.drawText(title, //
					devXTitle < 0 ? 0 : devXTitle,
					devYTitle,
					true);

		} else {

			/*
			 * draw title for each segment
			 */

			final float scaleX = drawingData.getScaleX();

			final int[] valueStart = chartSegments.valueStart;
			final int[] valueEnd = chartSegments.valueEnd;
			final String[] segmentTitles = chartSegments.segmentTitle;

			if (valueStart != null && valueEnd != null && segmentTitles != null) {

				int devXChartTitleEnd = -1;

				for (int segmentIndex = 0; segmentIndex < valueStart.length; segmentIndex++) {

					// draw the title in the center of the segment
					final String segmentTitle = segmentTitles[segmentIndex];
					if (segmentTitle != null) {

						final int devXSegmentStart = (int) (scaleX * valueStart[segmentIndex])
								- _xxDevViewPortLeftBorder;
						final int devXSegmentEnd = (int) (scaleX * (valueEnd[segmentIndex] + 1))
								- _xxDevViewPortLeftBorder;

						final int devXSegmentLength = devXSegmentEnd - devXSegmentStart;
						final int devXSegmentCenter = devXSegmentEnd - (devXSegmentLength / 2);
						final int devXTitleCenter = gc.textExtent(segmentTitle).x / 2;

						final int devX = devXSegmentCenter - devXTitleCenter;

						if (devX <= devXChartTitleEnd) {
							// skip title when it overlaps the previous title
							continue;
						}

						gc.drawText(segmentTitle, devX, devYTitle, false);

						devXChartTitleEnd = devXSegmentCenter + devXTitleCenter + 3;
					}
				}
			}
		}

	}

	/**
	 * Draw the unit label, tick and the vertical grid line for the x axis
	 * 
	 * @param gcChart
	 * @param gcGraph
	 * @param drawingData
	 * @param isDrawUnit
	 *            <code>true</code> indicate to draws the unit tick and unit label additional to the
	 *            unit grid line
	 */
	private void drawAsync210XUnitsAndVGrid(final GC gcChart,
											final GC gcGraph,
											final GraphDrawingData drawingData,
											final boolean isDrawUnit) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> xUnits = drawingData.getXUnits();

		final ChartDataXSerie xData = drawingData.getXData();
		final int devYBottom = drawingData.getDevYBottom();
		final int xUnitTextPos = drawingData.getXUnitTextPos();
		float scaleX = drawingData.getScaleX();
		final boolean isXUnitOverlapChecked = drawingData.isXUnitOverlapChecked();
		final boolean isDrawVerticalGrid = _chart.isShowVerticalGridLines;
		final boolean[] isDrawUnits = drawingData.isDrawUnits();

		final double devGraphWidth = drawingData.devVirtualGraphWidth;
		final double scalingFactor = xData.getScalingFactor();
		final double scalingMaxValue = xData.getScalingMaxValue();
		final boolean isExtendedScaling = scalingFactor != 1.0;
		final double extScaleX = ((devGraphWidth - 1) / Math.pow(scalingMaxValue, scalingFactor));

		// check if the x-units has a special scaling
		final float scaleUnitX = drawingData.getScaleUnitX();
		if (scaleUnitX != Float.MIN_VALUE) {
			scaleX = scaleUnitX;
		}

		// get distance between two units
		final float devUnitWidth = xUnits.size() > 1 //
				? ((xUnits.get(1).value * scaleX) - (xUnits.get(0).value * scaleX))
				: 0;

		int unitCounter = 0;
		final int devVisibleChartWidth = getDevVisibleChartWidth();

		boolean isFirstUnit = true;
		int devXLastUnitRightPosition = -1;

		final String unitLabel = drawingData.getXData().getUnitLabel();
		final int devUnitLabelWidth = gcChart.textExtent(unitLabel).x;

		gcChart.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gcGraph.setForeground(_gridColor);

		for (final ChartUnit xUnit : xUnits) {

			// get dev x-position for the unit tick
			int devXUnitTick;
			if (isExtendedScaling) {

				// extended scaling
				final double scaledUnitValue = ((Math.pow(xUnit.value, scalingFactor)) * extScaleX);
				devXUnitTick = (int) (scaledUnitValue);

			} else {
				// scale with devXOffset
				devXUnitTick = (int) (xUnit.value * scaleX) - _xxDevViewPortLeftBorder;
			}

			/*
			 * skip units which are outside of the visible area
			 */
			if (devXUnitTick < 0) {
				continue;
			}
			if (devXUnitTick > devVisibleChartWidth) {
				break;
			}

			if (isDrawUnit) {

				boolean isBreakOuterLoop = false;

				while (true) {

					/*
					 * draw unit tick
					 */
					if (devXUnitTick > 0 && (isDrawUnits == null || isDrawUnits[unitCounter])) {

						// draw unit tick, don't draw it on the vertical 0 line

						gcChart.setLineStyle(SWT.LINE_SOLID);
						gcChart.drawLine(devXUnitTick, devYBottom, devXUnitTick, devYBottom + 5);
					}

					/*
					 * draw unit value
					 */
					final int devUnitValueWidth = gcChart.textExtent(xUnit.valueLabel).x;

					if (devUnitWidth != 0 && xUnitTextPos == GraphDrawingData.X_UNIT_TEXT_POS_CENTER) {

						/*
						 * draw unit value BETWEEN two units
						 */

						final int devXUnitCenter = ((int) devUnitWidth - devUnitValueWidth) / 2;
						int devXUnitLabelPosition = devXUnitTick + devXUnitCenter;

						if (devXUnitLabelPosition < 0) {
							// position could be < 0 for the first unit
							devXUnitLabelPosition = 0;
						}

						if (isXUnitOverlapChecked == false && devXUnitLabelPosition <= devXLastUnitRightPosition) {

							// skip unit when it overlaps the previous unit

						} else {

							gcChart.drawText(xUnit.valueLabel, devXUnitLabelPosition, devYBottom + 7, true);

							devXLastUnitRightPosition = devXUnitLabelPosition + devUnitValueWidth + 0;
						}

					} else {

						/*
						 * draw unit value in the MIDDLE of the unit tick
						 */

						final int devUnitValueWidth2 = devUnitValueWidth / 2;
						int devXUnitValueDefaultPosition = devXUnitTick - devUnitValueWidth2;

						if (isFirstUnit) {

							// draw first unit

							isFirstUnit = false;

							/*
							 * this is the first unit, do not center it on the unit tick, because it
							 * would be clipped on the left border
							 */
							int devXUnit = devXUnitValueDefaultPosition;
							if (devXUnit < 0) {
								devXUnit = 0;
							}

							gcChart.drawText(xUnit.valueLabel, devXUnit, devYBottom + 7, true);

							// draw unit label (km, mi, h)

							final int devXUnitLabel = devXUnit + devUnitValueWidth + 2;

							gcChart.drawText(unitLabel,//
									devXUnitLabel,
									devYBottom + 7,
									true);

							devXLastUnitRightPosition = devXUnitLabel + devUnitLabelWidth + 2;

						} else {

							// draw subsequent units

							if (devXUnitValueDefaultPosition >= 0) {

								/*
								 * check if the unit value would be clipped at the right border,
								 * move it to the left to make it fully visible
								 */
								if ((devXUnitTick + devUnitValueWidth2) > devVisibleChartWidth) {

									devXUnitValueDefaultPosition = devVisibleChartWidth - devUnitValueWidth;

									// check if the unit value is overlapping the previous unit value
									if (devXUnitValueDefaultPosition <= devXLastUnitRightPosition + 2) {
										isBreakOuterLoop = true;
										break;
									}
								}

								if (devXUnitValueDefaultPosition > devXLastUnitRightPosition) {

									gcChart.drawText(
											xUnit.valueLabel,
											devXUnitValueDefaultPosition,
											devYBottom + 7,
											true);

									devXLastUnitRightPosition = devXUnitValueDefaultPosition + devUnitValueWidth + 2;
								}
							}
						}
					}
					break; // while(true)
				}

				if (isBreakOuterLoop) {
					break;
				}
			}

			// draw vertical gridline but not on the vertical 0 line
			if (devXUnitTick > 0 && isDrawVerticalGrid) {

				if (xUnit.isMajorValue) {
					gcGraph.setLineStyle(SWT.LINE_SOLID);
					gcGraph.setForeground(_gridColorMajor);
				} else {
					/*
					 * line width is a complicated topic, when it's not set the gridlines of the
					 * first graph is different than the subsequent graphs, but setting it globally
					 * degrades performance dramatically
					 */
//					gcGraph.setLineWidth(0);
					gcGraph.setLineDash(DOT_DASHES);
					gcGraph.setForeground(_gridColor);
				}
				gcGraph.drawLine(devXUnitTick, 0, devXUnitTick, drawingData.devGraphHeight);

			}

			unitCounter++;
		}
	}

	/**
	 * draw the horizontal gridlines or the x-axis
	 * 
	 * @param gcGraph
	 * @param drawingData
	 * @param isDrawOnlyXAsis
	 */
	private void drawAsync220XAsisHGrid(final GC gcGraph,
										final GraphDrawingData drawingData,
										final boolean isDrawOnlyXAsis) {

		final Display display = getDisplay();

		final ArrayList<ChartUnit> yUnits = drawingData.getYUnits();
		final int unitListSize = yUnits.size();

		final float scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final int devGraphHeight = drawingData.devGraphHeight;
		final int devVisibleChartWidth = getDevVisibleChartWidth();

		final boolean isBottomUp = drawingData.getYData().isYAxisDirection();
		final boolean isTopDown = isBottomUp == false;

		final int devYTop = 0;
		final int devYBottom = devGraphHeight;

		final boolean isDrawHorizontalGrid = _chart.isShowHorizontalGridLines;

		int devY;
		int unitIndex = 0;

		// loop: all units
		for (final ChartUnit yUnit : yUnits) {

			final float unitValue = yUnit.value;
			final float devYUnit = ((unitValue - graphYBottom) * scaleY) + 0.5f;

			if (isBottomUp || unitListSize == 1) {
				devY = devYBottom - (int) devYUnit;
			} else {
				devY = devYTop + (int) devYUnit;
			}

			// check if a y-unit is on the x axis
			final boolean isXAxis = (isTopDown && unitIndex == unitListSize - 1) || //
					(isBottomUp && unitIndex == 0);

			if (isDrawOnlyXAsis) {

				// draw only the x-axis

				if (isXAxis) {

					gcGraph.setLineStyle(SWT.LINE_SOLID);
					gcGraph.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
					gcGraph.drawLine(0, devY, devVisibleChartWidth, devY);

					// only the x-axis needs to be drawn
					break;
				}

			} else {

				if (isXAxis == false && isDrawHorizontalGrid) {

					// draw gridlines

					if (yUnit.isMajorValue) {
						gcGraph.setLineStyle(SWT.LINE_SOLID);
						gcGraph.setForeground(_gridColorMajor);
					} else {
						gcGraph.setLineDash(DOT_DASHES);
						gcGraph.setForeground(_gridColor);
					}
					gcGraph.drawLine(0, devY, devVisibleChartWidth, devY);
				}
			}

			unitIndex++;
		}
	}

	private void drawAsync500LineGraph(final GC gcGraph, final GraphDrawingData graphDrawingData) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartDataYSerie yData = graphDrawingData.getYData();

		final int serieSize = xData.getHighValues()[0].length;
		final float scaleX = graphDrawingData.getScaleX();

		// create line hovered positions
		_lineFocusRectangles.add(new Rectangle[serieSize]);
		_lineDevPositions.add(new Point[serieSize]);
		_isHoveredLineVisible = true;

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBgDark = yData.getRgbDark()[0];
		final RGB rgbBgBright = yData.getRgbBright()[0];

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = Math.max(0, _xxDevViewPortLeftBorder) / scaleX;
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = _xxDevViewPortLeftBorder / scaleX;
		}

		if (xData.getSynchMarkerStartIndex() == -1) {

			// synch marker is not displayed

			int graphLineAlpha = (int) (_chart.graphTransparencyLine * _chart.graphTransparencyAdjustment);
			int graphFillingAlpha = (int) (_chart.graphTransparencyFilling * _chart.graphTransparencyAdjustment);

			graphLineAlpha = graphLineAlpha < 0 ? 0 : graphLineAlpha > 255 ? 255 : graphLineAlpha;
			graphFillingAlpha = graphFillingAlpha < 0 ? 0 : graphFillingAlpha > 255 ? 255 : graphFillingAlpha;

			drawAsync510LineGraphSegment(
					gcGraph,
					graphDrawingData,
					0,
					serieSize,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					graphLineAlpha,
					graphFillingAlpha,
					graphValueOffset);

		} else {

			// draw synched tour

			final int noneMarkerLineAlpha = (int) (_chart.graphTransparencyLine * 0.5);
			final int noneMarkerFillingAlpha = (int) (_chart.graphTransparencyFilling * 0.5);

			// draw the x-marker
			drawAsync510LineGraphSegment(
					gcGraph,
					graphDrawingData,
					xData.getSynchMarkerStartIndex(),
					xData.getSynchMarkerEndIndex() + 1,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					_chart.graphTransparencyLine,
					_chart.graphTransparencyFilling,
					graphValueOffset);

			// draw segment before the marker
			drawAsync510LineGraphSegment(
					gcGraph,
					graphDrawingData,
					0,
					xData.getSynchMarkerStartIndex() + 1,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					noneMarkerLineAlpha,
					noneMarkerFillingAlpha,
					graphValueOffset);

			// draw segment after the marker
			drawAsync510LineGraphSegment(
					gcGraph,
					graphDrawingData,
					xData.getSynchMarkerEndIndex() - 0,
					serieSize,
					rgbFg,
					rgbBgDark,
					rgbBgBright,
					noneMarkerLineAlpha,
					noneMarkerFillingAlpha,
					graphValueOffset);
		}
	}

	/**
	 * first we draw the graph into a path, the path is then drawn on the device with a
	 * transformation
	 * 
	 * @param gcSegment
	 * @param graphDrawingData
	 * @param startIndex
	 * @param endIndex
	 * @param rgbFg
	 * @param rgbBgDark
	 * @param rgbBgBright
	 * @param graphValueOffset
	 */
	private void drawAsync510LineGraphSegment(	final GC gcSegment,
												final GraphDrawingData graphDrawingData,
												final int startIndex,
												final int endIndex,
												final RGB rgbFg,
												final RGB rgbBgDark,
												final RGB rgbBgBright,
												final int graphLineAlpha,
												final int graphFillingAlpha,
												final float graphValueOffset) {

		final ChartDataXSerie xData = graphDrawingData.getXData();
		final ChartDataYSerie yData = graphDrawingData.getYData();

		final int graphFillMethod = yData.getGraphFillMethod();

		final float[][] yHighValues = yData.getHighValues();

		final float xValues[] = xData.getHighValues()[0];
		final float yValues[] = yHighValues[0];

		final boolean[] noLine = xData.getNoLine();

		// check array bounds
		final int xValueLength = xValues.length;
		if (startIndex >= xValueLength) {
			return;
		}
		final int yValueLength = yValues.length;

		/*
		 * 2nd path is currently used to draw the SRTM altitude line
		 */
		final boolean isPath2 = yHighValues.length > 1;
		float[] yValues2 = null;
		if (isPath2) {
			yValues2 = yHighValues[1];
		}

		// get top/bottom border values of the graph
		final float graphYBorderTop = graphDrawingData.getGraphYTop();
		final float graphYBorderBottom = graphDrawingData.getGraphYBottom();
		final int devYTop = graphDrawingData.getDevYTop();
		final int devChartHeight = getDevVisibleGraphHeight();

		final float scaleX = graphDrawingData.getScaleX();
		final float scaleY = graphDrawingData.getScaleY();

		final boolean isShowSkippedValues = _chartDrawingData.chartDataModel.isNoLinesValuesDisplayed();
		final Display display = getDisplay();

		// path is scaled in device pixel
		final Path path = new Path(display);
		final Path path2 = isPath2 ? new Path(display) : null;

		final ArrayList<Point> skippedValues = new ArrayList<Point>();

		final int devGraphHeight = graphDrawingData.devGraphHeight;
		final float devYGraphTop = scaleY * graphYBorderTop;
		final float devYGraphBottom = scaleY * graphYBorderBottom;

		final Rectangle[] lineFocusRectangles = _lineFocusRectangles.get(_lineFocusRectangles.size() - 1);
		final Point[] lineDevPositions = _lineDevPositions.get(_lineDevPositions.size() - 1);
		Rectangle prevLineRect = null;

		/*
		 * 
		 */
		final float devY0Inverse = devGraphHeight + devYGraphBottom;

		/*
		 * x-axis line with y==0
		 */
		float graphY_XAxisLine = 0;

		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
				|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

			graphY_XAxisLine = graphYBorderBottom > 0 //
					? graphYBorderBottom
					: graphYBorderTop < 0 //
							? graphYBorderTop
							: graphYBorderBottom;

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

			graphY_XAxisLine = graphYBorderBottom > 0 ? graphYBorderBottom //
					: graphYBorderTop < 0 ? graphYBorderTop //
							: 0;
		}
		final float devY_XAxisLine = scaleY * graphY_XAxisLine;

		final float graphXStart = xValues[startIndex] - graphValueOffset;
		final float graphYStart = yValues[startIndex];

		float graphY1Prev = graphYStart;

		float devXPrev = graphXStart * scaleX;
		float devY1Prev = graphY1Prev * scaleY;

		final Rectangle chartRectangle = gcSegment.getClipping();
		final int devXVisibleWidth = chartRectangle.width;

		boolean isDrawFirstPoint = true;

		final int lastIndex = endIndex - 1;
		float devXPrevNoLine = 0;
		boolean isNoLine = false;

		int valueIndexFirstPoint = startIndex;
		int valueIndexLastPoint = startIndex;
		int prevValueIndex = startIndex;

		/*
		 * set the hovered index only ONCE because when autoscrolling is done to the right side this
		 * can cause that the last value is used for the hovered index instead of the previous
		 * before the last
		 */
		boolean isSetHoveredIndex = false;

		final int[] devXPositions = new int[endIndex];
		final float devY0 = devY0Inverse - devY_XAxisLine;

		/*
		 * draw the lines into the paths
		 */
		for (int valueIndex = startIndex; valueIndex < endIndex; valueIndex++) {

			// check array bounds
			if (valueIndex >= yValueLength) {
				break;
			}

			final float graphX = xValues[valueIndex] - graphValueOffset;
			final float devX = graphX * scaleX;

			final float graphY1 = yValues[valueIndex];
			final float devY1 = graphY1 * scaleY;

			float graphY2 = 0;
			float devY2 = 0;

			if (isPath2) {
				graphY2 = yValues2[valueIndex];
				devY2 = graphY2 * scaleY;
			}

			devXPositions[valueIndex] = (int) devX;

			// check if position is horizontal visible
			if (devX < 0) {

				// keep current position which is used as the painting starting point

				graphY1Prev = graphY1;

				devXPrev = devX;
				devY1Prev = devY1;

				valueIndexFirstPoint = valueIndex;
				prevValueIndex = valueIndex;

				continue;
			}

			/*
			 * draw first point
			 */
			if (isDrawFirstPoint) {

				// move to the first point

				isDrawFirstPoint = false;

				// set first point before devX==0 that the first line is not visible but correctly painted
				final float devXFirstPoint = devXPrev;
				float devYStart = 0;

				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

					// start from the bottom of the graph

					devYStart = devGraphHeight;

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					// start from the x-axis, y=0

					devYStart = devY0;
				}

				final float devY = devY0Inverse - devY1Prev;

				path.moveTo(devXFirstPoint, devYStart);
				path.lineTo(devXFirstPoint, devY);

				if (isPath2) {
					path2.moveTo(devXFirstPoint, devY0Inverse - devY2);
					path2.lineTo(devXFirstPoint, devY0Inverse - devY2);
				}

				// set line hover positions for the first point
				final Rectangle currentRect = new Rectangle((int) devXFirstPoint, 0, 1, devChartHeight);
				final Point currentPoint = new Point((int) devXFirstPoint, devYTop + (int) devY);

				lineDevPositions[valueIndexFirstPoint] = currentPoint;
				lineFocusRectangles[valueIndexFirstPoint] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw line to current point
			 */
			if ((int) devX != (int) devXPrev || graphY1 == 0 || graphY2 == 0) {

				// optimization: draw only ONE line for the current x-position
				// but draw to the 0 line otherwise it's possible that a triangle is painted

				float devY = 0;

				if (noLine != null && noLine[valueIndex]) {

					/*
					 * draw NO line, but draw a line at the bottom or the x-axis with y=0
					 */

					if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
							|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

						// start from the bottom of the graph

						devY = devGraphHeight;

					} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

						// start from the x-axis, y=0

						devY = devY0;
					}
					path.lineTo(devXPrev, devY);
					path.lineTo(devX, devY);

					/*
					 * keep positions, because skipped values will be painted as a dot outside of
					 * the path, but don't draw on the graph bottom or x-axis
					 */
					if (isShowSkippedValues) {

						final int devYSkipped = (int) (devY0Inverse - devY1);
						if (devYSkipped != devY0 && graphY1 != 0) {
							skippedValues.add(new Point((int) devX, devYSkipped));
						}
					}

					isNoLine = true;
					devXPrevNoLine = devX;

					// keep correct position that the hovered line dev position is painted at the correct position
					devY = devY0Inverse - devY1;

				} else {

					/*
					 * draw line to the current point
					 */

					// check if a NO line was painted
					if (isNoLine) {

						isNoLine = false;

						path.lineTo(devXPrevNoLine, devY0Inverse - devY1Prev);
					}

					devY = devY0Inverse - devY1;

					path.lineTo(devX, devY);

					if (isPath2) {
						path2.lineTo(devX, devY0Inverse - devY2);
					}
				}

				/*
				 * set line hover positions
				 */
				final float devXDiff = (devX - devXPrev) / 2;
				final int devXDiffWidth = devXDiff < 1 ? 1 : (int) (devXDiff + 0.5);
				final int devXRect = (int) (devX - devXDiffWidth);

				// set right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth + 1;

				// check if hovered line is hit, this check is an inline for .contains(...)
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredLineValueIndex = prevValueIndex;
					isSetHoveredIndex = true;
				}

				final Rectangle currentRect = new Rectangle(devXRect, 0, devXDiffWidth + 1, devChartHeight);
				final Point currentPoint = new Point((int) devX, devYTop + (int) devY);

				lineDevPositions[valueIndex] = currentPoint;
				lineFocusRectangles[valueIndex] = currentRect;

				prevLineRect = currentRect;
			}

			/*
			 * draw last point
			 */
			if (valueIndex == lastIndex || //

					// check if last visible position + 1 is reached
					devX > devXVisibleWidth) {

				/*
				 * this is the last point for a filled graph
				 */

				final float devY = devY0Inverse - devY1;

				path.lineTo(devX, devY);

				// move path to the final point
				if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM
						|| graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

					// draw line to the bottom of the graph

					path.lineTo(devX, devGraphHeight);

				} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

					// draw line to the x-axis, y=0

					path.lineTo(devX, devY0);
				}

				// moveTo() is necessary that the graph is filled correctly (to prevent a triangle filled shape)
				// finalize previous subpath
				path.moveTo(devX, 0);

				if (isPath2) {
					path2.lineTo(devX, devY0Inverse - devY2);
					path2.moveTo(devX, 0);
				}

				valueIndexLastPoint = valueIndex;

				/*
				 * set line rectangle
				 */
				final float devXDiff = (devX - devXPrev) / 2;
				final int devXDiffWidth = devXDiff < 1 ? 1 : (int) (devXDiff + 0.5);

				// set right part of the rectangle width into the previous rectangle
				prevLineRect.width += devXDiffWidth;

				// check if hovered line is hit, this check is an inline for .contains(...)
				if (isSetHoveredIndex == false && prevLineRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredLineValueIndex = valueIndex;
					isSetHoveredIndex = true;
				}

//				/*
//				 * advance to the next point and check array bounds
//				 */
//				if (++valueIndex >= yValueLength) {
//					break;
//				}

				final Rectangle lastRect = new Rectangle(
						(int) (devX - devXDiffWidth),
						0,
						devXDiffWidth + 1000,
						devChartHeight);
				final Point lastPoint = new Point((int) devX, devYTop + (int) devY);

				lineDevPositions[valueIndex] = lastPoint;
				lineFocusRectangles[valueIndex] = lastRect;

				if (isSetHoveredIndex == false && lastRect.contains(_devXMouseMove, _devYMouseMove)) {
					_hoveredLineValueIndex = valueIndex;
				}

				break;
			}

			devXPrev = devX;
			devY1Prev = devY1;
			prevValueIndex = valueIndex;
		}

		final Color colorLine = new Color(display, rgbFg);
		final Color colorBgDark = new Color(display, rgbBgDark);
		final Color colorBgBright = new Color(display, rgbBgBright);

		gcSegment.setAntialias(_chart.graphAntialiasing);
		gcSegment.setAlpha(graphFillingAlpha);

		final float graphWidth = xValues[Math.min(xValueLength - 1, endIndex)] - graphValueOffset;

		/*
		 * force a max width because the fill will not be drawn on Linux
		 */
		final int devGraphWidth = Math.min(0x7fff, (int) (graphWidth * scaleX));

		gcSegment.setClipping(path);

		/*
		 * fill the graph
		 */
		if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_BOTTOM) {

			/*
			 * adjust the fill gradient in the height, otherwise the fill is not in the whole
			 * rectangle
			 */

			gcSegment.setForeground(colorBgDark);
			gcSegment.setBackground(colorBgBright);

			gcSegment.fillGradientRectangle(//
					0,
					devGraphHeight,
					devGraphWidth,
					-devGraphHeight,
					true);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_FILL_ZERO) {

			/*
			 * fill above 0 line
			 */

			gcSegment.setForeground(colorBgDark);
			gcSegment.setBackground(colorBgBright);

			gcSegment.fillGradientRectangle(//
					0,
					(int) devY0,
					devGraphWidth,
					-(int) (devYGraphTop - devY_XAxisLine),
					true);

			/*
			 * fill below 0 line
			 */
			gcSegment.setForeground(colorBgBright);
			gcSegment.setBackground(colorBgDark);

			gcSegment.fillGradientRectangle(//
					0,
					devGraphHeight, // start from the graph bottom
					devGraphWidth,
					-(int) Math.min(devGraphHeight, devGraphHeight - devY0Inverse),
					true);

		} else if (graphFillMethod == ChartDataYSerie.FILL_METHOD_CUSTOM) {

			final IFillPainter customFillPainter = yData.getCustomFillPainter();

			if (customFillPainter != null) {

				gcSegment.setForeground(colorBgDark);
				gcSegment.setBackground(colorBgBright);

				customFillPainter.draw(
						gcSegment,
						graphDrawingData,
						_chart,
						devXPositions,
						valueIndexFirstPoint,
						valueIndexLastPoint);
			}
		}

		// reset clipping that the line is drawn everywere
		gcSegment.setClipping((Rectangle) null);

		gcSegment.setBackground(colorLine);

		/*
		 * paint skipped values
		 */
		if (isShowSkippedValues && skippedValues.size() > 0) {
			for (final Point skippedPoint : skippedValues) {
				gcSegment.fillRectangle(skippedPoint.x, skippedPoint.y, 2, 2);
			}
		}

		/*
		 * draw line along the path
		 */
		gcSegment.setAlpha(graphLineAlpha);

		// set line style
		gcSegment.setLineStyle(SWT.LINE_SOLID);
//		gcSegment.setLineWidth(1);

		// draw the line of the graph
		gcSegment.setForeground(colorLine);

//		gcGraph.setAlpha(0x80);
		gcSegment.drawPath(path);

		// dispose resources
		colorLine.dispose();
		colorBgDark.dispose();
		colorBgBright.dispose();

		path.dispose();

		/*
		 * draw path2 above the other graph, this is currently used to draw the srtm graph
		 */
		if (path2 != null) {

			gcSegment.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			gcSegment.drawPath(path2);

			path2.dispose();
		}

		gcSegment.setAlpha(0xFF);
		gcSegment.setAntialias(SWT.OFF);
	}

	private void drawAsync520RangeMarker(final GC gc, final GraphDrawingData drawingData) {

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();

		final int[] startIndex = xData.getRangeMarkerStartIndex();
		final int[] endIndex = xData.getRangeMarkerEndIndex();

		if (startIndex == null) {
			return;
		}

		final float scaleX = drawingData.getScaleX();

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBg1 = yData.getRgbDark()[0];
		final RGB rgbBg2 = yData.getRgbBright()[0];

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = Math.max(0, _xxDevViewPortLeftBorder) / scaleX;
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = _xxDevViewPortLeftBorder / scaleX;
		}

		int graphFillingAlpha = (int) (_chart.graphTransparencyFilling * 0.5);
		int graphLineAlpha = (int) (_chart.graphTransparencyFilling * 0.5);

		graphFillingAlpha = graphFillingAlpha < 0 ? 0 : graphFillingAlpha > 255 ? 255 : graphFillingAlpha;
		graphLineAlpha = graphLineAlpha < 0 ? 0 : graphLineAlpha > 255 ? 255 : graphLineAlpha;

		int runningIndex = 0;
		for (final int markerStartIndex : startIndex) {

			// draw range marker
			drawAsync510LineGraphSegment(gc, //
					drawingData,
					markerStartIndex,
					endIndex[runningIndex] + 1,
					rgbFg,
					rgbBg1,
					rgbBg2,
					graphLineAlpha,
					graphFillingAlpha,
					graphValueOffset);

			runningIndex++;
		}
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * 
	 * @param gcGraph
	 * @param drawingData
	 */
	private void drawAsync530BarGraph(final GC gcGraph, final GraphDrawingData drawingData) {

		// get the chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int[][] colorsIndex = yData.getColorsIndex();

		gcGraph.setLineStyle(SWT.LINE_SOLID);

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		// get the chart values
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		final float graphYBorderBottom = drawingData.getGraphYBottom();
		final boolean axisDirection = yData.isYAxisDirection();

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a synch marker is not set, draw it normally
			graphValueOffset = Math.max(0, _xxDevViewPortLeftBorder) / scaleX;
		} else {
			// adjust the start position to the synch marker position
			graphValueOffset = _xxDevViewPortLeftBorder / scaleX;
		}

		final int devGraphCanvasHeight = drawingData.devGraphHeight;

		/*
		 * Get the top/bottom for the graph, a chart can contain multiple canvas. Canvas is the area
		 * where the graph is painted.
		 */
		final int devYCanvasBottom = devGraphCanvasHeight;
		final int devYCanvasTop = 0;

		final int devYChartBottom = drawingData.getDevYBottom();
		final int devYChartTop = devYChartBottom - devGraphCanvasHeight;

		final float xValues[] = xData.getHighValues()[0];
		final float yHighSeries[][] = yData.getHighValues();
		final float yLowSeries[][] = yData.getLowValues();

		final int serieLength = yHighSeries.length;
		final int valueLength = xValues.length;

		// keep the bar rectangles for all canvas
		final Rectangle[][] barRecangles = new Rectangle[serieLength][valueLength];
		final Rectangle[][] barFocusRecangles = new Rectangle[serieLength][valueLength];
		drawingData.setBarRectangles(barRecangles);
		drawingData.setBarFocusRectangles(barFocusRecangles);

		// keep the height for stacked bar charts
		final int devHeightSummary[] = new int[valueLength];

		final int devBarWidthOriginal = drawingData.getBarRectangleWidth();
		final int devBarWidth = Math.max(1, devBarWidthOriginal);
		final int devBarWidth2 = devBarWidth / 2;

		final int serieLayout = yData.getChartLayout();
		final int devBarRectangleStartXPos = drawingData.getDevBarRectangleXPos();
		final int barPosition = drawingData.getBarPosition();

		// loop: all data series
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final float yHighValues[] = yHighSeries[serieIndex];
			float yLowValues[] = null;
			if (yLowSeries != null) {
				yLowValues = yLowSeries[serieIndex];
			}

			int devBarXPos = devBarRectangleStartXPos;
			int devBarWidthPositioned = devBarWidth;

			// reposition the rectangle when the bars are beside each other
			if (serieLayout == ChartDataYSerie.BAR_LAYOUT_BESIDE) {
				devBarXPos += serieIndex * devBarWidth;
				devBarWidthPositioned = devBarWidth - 1;
			}

			int devXPosNextBar = 0;

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

				// get the x position
				int devXPos = (int) ((xValues[valueIndex] - graphValueOffset) * scaleX) + devBarXPos;

				// center the bar
				if (devBarWidth > 1 && barPosition == GraphDrawingData.BAR_POS_CENTER) {
					devXPos -= devBarWidth2;
				}

				float valueYLow;
				if (yLowValues == null) {
					valueYLow = yData.getVisibleMinValue();
				} else {
					// check array bounds
					if (valueIndex >= yLowValues.length) {
						break;
					}
					valueYLow = yLowValues[valueIndex];
				}

				// check array bounds
				if (valueIndex >= yHighValues.length) {
					break;
				}
				final float valueYHigh = yHighValues[valueIndex];

				final float barHeight = (Math.max(valueYHigh, valueYLow) - Math.min(valueYHigh, valueYLow));
				if (barHeight == 0) {
					continue;
				}

				final int devBarHeight = (int) (barHeight * scaleY);

				// get the old y position for stacked bars
				int devYPreviousHeight = 0;
				if (serieLayout == ChartDataYSerie.BAR_LAYOUT_STACKED) {
					devYPreviousHeight = devHeightSummary[valueIndex];
				}

				/*
				 * get y positions
				 */
				int devYPosChart;
				int devYPosCanvas;
				if (axisDirection) {

					final int devYBar = (int) ((valueYHigh - graphYBorderBottom) * scaleY) + devYPreviousHeight;

					devYPosChart = devYChartBottom - devYBar;
					devYPosCanvas = devYCanvasBottom - devYBar;

				} else {
					final int devYBar = (int) ((valueYLow - graphYBorderBottom) * scaleY) + devYPreviousHeight;

					devYPosChart = devYChartTop + devYBar;
					devYPosCanvas = devYCanvasTop + devYBar;
				}

				int devXPosShape = devXPos;
				int devShapeBarWidth = devBarWidthPositioned;

				/*
				 * make sure the bars do not overlap
				 */
				if (serieLayout != ChartDataYSerie.BAR_LAYOUT_SINGLE_SERIE) {
					if (devXPosNextBar > 0) {
						if (devXPos < devXPosNextBar) {

							// bars do overlap

							final int devDiff = devXPosNextBar - devXPos;

							devXPosShape = devXPos + devDiff;
							devShapeBarWidth = devBarWidthPositioned - devDiff;
						}
					}
				}
				devXPosNextBar = devXPos + devBarWidthPositioned;

				/*
				 * get colors
				 */
				final int colorIndex = colorsIndex[serieIndex][valueIndex];
				final RGB rgbBrightDef = rgbBright[colorIndex];
				final RGB rgbDarkDef = rgbDark[colorIndex];
				final RGB rgbLineDef = rgbLine[colorIndex];

				final Color colorBright = getColor(rgbBrightDef);
				final Color colorDark = getColor(rgbDarkDef);
				final Color colorLine = getColor(rgbLineDef);

				gcGraph.setBackground(colorDark);

				/*
				 * draw bar
				 */
				final Rectangle barShapeCanvas = new Rectangle(
						devXPosShape,
						devYPosCanvas,
						devShapeBarWidth,
						devBarHeight);

				if (devBarWidthOriginal > 0) {

					gcGraph.setForeground(colorBright);
					gcGraph.fillGradientRectangle(
							barShapeCanvas.x,
							barShapeCanvas.y,
							barShapeCanvas.width,
							barShapeCanvas.height,
							false);

					gcGraph.setForeground(colorLine);
					gcGraph.drawRectangle(barShapeCanvas);

				} else {

					gcGraph.setForeground(colorLine);
					gcGraph.drawLine(
							barShapeCanvas.x,
							barShapeCanvas.y,
							barShapeCanvas.x,
							(barShapeCanvas.y + barShapeCanvas.height));
				}

				barRecangles[serieIndex][valueIndex] = new Rectangle( //
						devXPosShape,
						devYPosChart,
						devShapeBarWidth,
						devBarHeight);

				barFocusRecangles[serieIndex][valueIndex] = new Rectangle(//
						devXPosShape - 2,
						(devYPosChart - 2),
						devShapeBarWidth + 4,
						(devBarHeight + 7));

				// keep the height for the bar
				devHeightSummary[valueIndex] += devBarHeight;
			}
		}

		// reset clipping
		gcGraph.setClipping((Rectangle) null);
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawAsync540LineWithBarGraph(final GC gc, final GraphDrawingData drawingData) {

		// get the chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final int[][] colorsIndex = yData.getColorsIndex();

		gc.setLineStyle(SWT.LINE_SOLID);

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		// get the chart values
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final boolean axisDirection = yData.isYAxisDirection();
//		final int barPosition = drawingData.getBarPosition();

		// get the horizontal offset for the graph
		float graphValueOffset;
		if (_chartComponents._synchConfigSrc == null) {
			// a zoom marker is not set, draw it normally
			graphValueOffset = Math.max(0, _xxDevViewPortLeftBorder) / scaleX;
		} else {
			// adjust the start position to the zoom marker position
			graphValueOffset = _xxDevViewPortLeftBorder / scaleX;
		}

		// get the top/bottom of the graph
		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

		// virtual 0 line for the y-axis of the chart in dev units
//		final float devChartY0Line = (float) devYBottom + (scaleY * graphYBottom);

		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		final float xValues[] = xData.getHighValues()[0];
		final float yHighSeries[][] = yData.getHighValues();
//		final int yLowSeries[][] = yData.getLowValues();

		final int serieLength = yHighSeries.length;
		final int valueLength = xValues.length;

		final int devBarWidthComputed = drawingData.getBarRectangleWidth();
		final int devBarWidth = Math.max(1, devBarWidthComputed);

		final int devBarXPos = drawingData.getDevBarRectangleXPos();

		// loop: all data series
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final float yHighValues[] = yHighSeries[serieIndex];
//			int yLowValues[] = null;
//			if (yLowSeries != null) {
//				yLowValues = yLowSeries[serieIndex];
//			}

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

				// get the x position
				final int devXPos = (int) ((xValues[valueIndex] - graphValueOffset) * scaleX) + devBarXPos;

//				final int devBarWidthSelected = devBarWidth;
//				final int devBarWidth2 = devBarWidthSelected / 2;

//				int devXPosSelected = devXPos;
//
//				// center the bar
//				if (devBarWidthSelected > 1 && barPosition == GraphDrawingData.BAR_POS_CENTER) {
//					devXPosSelected -= devBarWidth2;
//				}

				// get the bar height
				final float graphYLow = graphYBottom;
				final float graphYHigh = yHighValues[valueIndex];

				final float graphBarHeight = Math.max(graphYHigh, graphYLow) - Math.min(graphYHigh, graphYLow);

				// skip bars which have no height
				if (graphBarHeight == 0) {
					continue;
				}

				final int devBarHeight = (int) (graphBarHeight * scaleY);

				// get the y position
				int devYPos;
				if (axisDirection) {
					devYPos = devYBottom - ((int) ((graphYHigh - graphYBottom) * scaleY));
				} else {
					devYPos = devYTop + ((int) ((graphYLow - graphYBottom) * scaleY));
				}

				final Rectangle barShape = new Rectangle(devXPos, devYPos, devBarWidth, devBarHeight);

				final int colorSerieIndex = colorsIndex.length >= serieIndex ? colorsIndex.length - 1 : serieIndex;
				final int colorIndex = colorsIndex[colorSerieIndex][valueIndex];

				final RGB rgbBrightDef = rgbBright[colorIndex];
				final RGB rgbDarkDef = rgbDark[colorIndex];
				final RGB rgbLineDef = rgbLine[colorIndex];

				final Color colorBright = getColor(rgbBrightDef);
				final Color colorDark = getColor(rgbDarkDef);
				final Color colorLine = getColor(rgbLineDef);

				gc.setBackground(colorDark);

				/*
				 * draw bar
				 */
				if (devBarWidthComputed > 0) {

					gc.setForeground(colorBright);
					gc.fillGradientRectangle(barShape.x, barShape.y, barShape.width, barShape.height, false);

					gc.setForeground(colorLine);
					gc.drawRectangle(barShape);

				} else {

					gc.setForeground(colorLine);
					gc.drawLine(barShape.x, barShape.y, barShape.x, (barShape.y + barShape.height));
				}
			}
		}

		// reset clipping
		gc.setClipping((Rectangle) null);
	}

	/**
	 * Draws a bar graph, this requires that drawingData.getChartData2ndValues does not return null,
	 * if null is returned, a line graph will be drawn instead
	 * <p>
	 * <b> Zooming the chart is not yet supported for this charttype because logarithmic scaling is
	 * very complex for a zoomed chart </b>
	 * 
	 * @param gc
	 * @param drawingData
	 */
	private void drawAsync550XYScatter(final GC gc, final GraphDrawingData drawingData) {

		// get chart data
		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		final float graphYBottom = drawingData.getGraphYBottom();
		final double devGraphWidth = drawingData.devVirtualGraphWidth;

		final double scalingFactor = xData.getScalingFactor();
		final double scalingMaxValue = xData.getScalingMaxValue();
		final boolean isExtendedScaling = scalingFactor != 1.0;
		final double scaleXExtended = ((devGraphWidth - 1) / Math.pow(scalingMaxValue, scalingFactor));

		// get colors
		final RGB[] rgbLine = yData.getRgbLine();

		// get the top/bottom of the graph
		final int devYTop = 0;
		final int devYBottom = drawingData.devGraphHeight;

//		gc.setAntialias(SWT.ON);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		final float[][] xSeries = xData.getHighValues();
		final float[][] ySeries = yData.getHighValues();
		final int size = 6;
		final int size2 = size / 2;

		for (int serieIndex = 0; serieIndex < xSeries.length; serieIndex++) {

			final float xValues[] = xSeries[serieIndex];
			final float yHighValues[] = ySeries[serieIndex];

			gc.setBackground(getColor(rgbLine[serieIndex]));

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < xValues.length; valueIndex++) {

				// check array bounds
				if (valueIndex >= yHighValues.length) {
					break;
				}

				final float xValue = xValues[valueIndex];
				final float yValue = yHighValues[valueIndex];

				// get the x/y positions
				int devX;
				if (isExtendedScaling) {
					devX = (int) ((Math.pow(xValue, scalingFactor)) * scaleXExtended);
				} else {
					devX = (int) (xValue * scaleX);
				}

				final int devY = devYBottom - ((int) ((yValue - graphYBottom) * scaleY));

				// draw shape
//				gc.fillRectangle(devXPos - size2, devYPos - size2, size, size);
				gc.fillOval(devX - size2, devY - size2, size, size);
			}
		}

		// reset clipping/antialias
		gc.setClipping((Rectangle) null);
		gc.setAntialias(SWT.OFF);
	}

	/**
	 * Paint event handler
	 * 
	 * <pre>
	 * Top-down sequence how the images are painted
	 * 
	 * {@link #_chartImage40Overlay}
	 * {@link #_chartImage30Custom}
	 * {@link #_chartImage20Chart}
	 * {@link #_chartImage10Graphs}
	 * </pre>
	 * 
	 * @param gc
	 */
	private void drawSync000onPaint(final GC gc) {

		if (_graphDrawingData == null || _graphDrawingData.size() == 0) {

			// fill the image area when there is no graphic
			gc.setBackground(_chart.getBackgroundColor());
			gc.fillRectangle(_clientArea);

			drawSyncBg999ErrorMessage(gc);

			return;
		}

		if (_isChartDirty) {

			// paint chart

			drawAsync100StartPainting();

			if (_isPaintDraggedImage) {

				/*
				 * paint dragged chart until the chart is recomputed
				 */
				drawSync020DraggedChart(gc);
				return;
			}

			// prevent flickering the graph

			/*
			 * mac osx is still flickering, added the drawChartImage in version 1.0
			 */
			if (_chartImage20Chart != null) {

				final Image image = drawSync010ImageChart(gc);
				if (image == null) {
					return;
				}

				final int gcHeight = _clientArea.height;
				final int imageHeight = image.getBounds().height;

				if (gcHeight > imageHeight) {

					// fill the gap between the image and the drawable area
					gc.setBackground(_chart.getBackgroundColor());
					gc.fillRectangle(0, imageHeight, _clientArea.width, _clientArea.height - imageHeight);

				} else {
					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
				}
			} else {
				gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			}

		} else {

			/*
			 * if the graph is not yet drawn (because this is done in another thread) there is
			 * nothing to do
			 */
			if (_chartImage20Chart == null) {
				// fill the image area when there is no graphic
				gc.setBackground(_chart.getBackgroundColor());
				gc.fillRectangle(_clientArea);
				gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
				return;
			}

			drawSync300Image30Custom();
			drawSync010ImageChart(gc);
		}
	}

	private Image drawSync010ImageChart(final GC gc) {

		final boolean isOverlayImageVisible = _isXSliderVisible
				|| _isYSliderVisible
				|| _isXMarkerMoved
				|| _isSelectionVisible
				|| _isHoveredLineVisible;

		if (isOverlayImageVisible) {

			drawSync400OverlayImage();

			if (_chartImage40Overlay != null) {
				gc.drawImage(_chartImage40Overlay, 0, 0);
			}
			return _chartImage40Overlay;

		} else {

			if (_chartImage20Chart != null) {
				gc.drawImage(_chartImage20Chart, 0, 0);
			}
			return _chartImage20Chart;
		}
	}

	private void drawSync020DraggedChart(final GC gc) {

		if (_draggedChartDraggedPos == null) {
			return;
		}

		final int devXDiff = _draggedChartDraggedPos.x - _draggedChartStartPos.x;
		final int devYDiff = 0;

		/*
		 * draw background that the none painted areas do not look ugly
		 */
		gc.setBackground(_chart.getBackgroundColor());

		if (devXDiff > 0) {
			gc.fillRectangle(0, devYDiff, devXDiff, _clientArea.height);
		} else {
			gc.fillRectangle(_clientArea.width + devXDiff, devYDiff, -devXDiff, _clientArea.height);
		}

		if (_chartImage40Overlay != null && _chartImage40Overlay.isDisposed() == false) {

			gc.drawImage(_chartImage40Overlay, devXDiff, devYDiff);

		} else if (_chartImage30Custom != null && _chartImage30Custom.isDisposed() == false) {

			gc.drawImage(_chartImage30Custom, devXDiff, devYDiff);

		} else if (_chartImage20Chart != null && _chartImage20Chart.isDisposed() == false) {

			gc.drawImage(_chartImage20Chart, devXDiff, devYDiff);
		}
	}

	/**
	 * Draws custom foreground layers on top of the graphs.
	 */
	private void drawSync300Image30Custom() {

		// the layer image has the same size as the graph image
		final Rectangle chartRect = _chartImage20Chart.getBounds();

		// ensure correct image size
		if (chartRect.width <= 0 || chartRect.height <= 0) {
			return;
		}

		/*
		 * when the existing image is the same size as the new image, we will redraw it only if it's
		 * set to dirty
		 */
		if (_isCustomLayerImageDirty == false && _chartImage30Custom != null) {

			final Rectangle oldBounds = _chartImage30Custom.getBounds();

			if (oldBounds.width == chartRect.width && oldBounds.height == chartRect.height) {
				return;
			}
		}

		if (Util.canReuseImage(_chartImage30Custom, chartRect) == false) {
			_chartImage30Custom = Util.createImage(getDisplay(), _chartImage30Custom, chartRect);
		}

		final GC gcCustom = new GC(_chartImage30Custom);
		{
			gcCustom.fillRectangle(chartRect);

			/*
			 * draw the chart image with the graphs into the custom layer image, the custom
			 * foreground layers are drawn on top of the graphs
			 */
			gcCustom.drawImage(_chartImage20Chart, 0, 0);

			for (final GraphDrawingData graphDrawingData : _graphDrawingData) {

				final ArrayList<IChartLayer> customFgLayers = graphDrawingData.getYData().getCustomForegroundLayers();

				for (final IChartLayer layer : customFgLayers) {
					layer.draw(gcCustom, graphDrawingData, _chart);
				}
			}
		}
		gcCustom.dispose();

		_isCustomLayerImageDirty = false;
	}

	/**
	 * Draws the overlays into the graph (fg layer image) slider image which contains the custom
	 * layer image
	 */
	private void drawSync400OverlayImage() {

		if (_chartImage30Custom == null) {
			return;
		}

		// the slider image is the same size as the graph image
		final Rectangle graphImageRect = _chartImage30Custom.getBounds();

		// check if an overlay image redraw is necessary
		if (_isOverlayDirty == false
				&& _isSliderDirty == false
				&& _isSelectionDirty == false
				&& _isHoveredBarDirty == false
				&& _hoveredLineValueIndex == -1
				&& _chartImage40Overlay != null) {

			final Rectangle oldBounds = _chartImage40Overlay.getBounds();
			if (oldBounds.width == graphImageRect.width && oldBounds.height == graphImageRect.height) {
				return;
			}
		}

		// ensure correct image size
		if (graphImageRect.width <= 0 || graphImageRect.height <= 0) {
			return;
		}

		if (Util.canReuseImage(_chartImage40Overlay, graphImageRect) == false) {
			_chartImage40Overlay = Util.createImage(getDisplay(), _chartImage40Overlay, graphImageRect);
		}

		if (_chartImage40Overlay.isDisposed()) {
			return;
		}

		final GC gcOverlay = new GC(_chartImage40Overlay);
		{
			/*
			 * copy the graph image into the slider image, the slider will be drawn on top of the
			 * graph
			 */
			gcOverlay.fillRectangle(graphImageRect);
			gcOverlay.drawImage(_chartImage30Custom, 0, 0);

			/*
			 * draw x/y-sliders
			 */
			if (_isXSliderVisible) {
				createXSliderLabel(gcOverlay, _xSliderOnTop);
				createXSliderLabel(gcOverlay, _xSliderOnBottom);
				updateXSliderYPosition();

				drawSync410XSlider(gcOverlay, _xSliderOnBottom);
				drawSync410XSlider(gcOverlay, _xSliderOnTop);
			}
			if (_isYSliderVisible) {
				drawSync420YSliders(gcOverlay);
			}
			_isSliderDirty = false;

			if (_isXMarkerMoved) {
				drawSync430XMarker(gcOverlay);
			}

			if (_isSelectionVisible) {
				drawSync440Selection(gcOverlay);
			}

			if (_isHoveredBarDirty) {
				drawSync450HoveredBar(gcOverlay);
				_isHoveredBarDirty = false;
			}

			if (_hoveredLineValueIndex != -1 && _lineDevPositions.size() > 0) {

				// hovered lines are set -> draw it
				drawSync460HoveredLine(gcOverlay);
			}

		}
		gcOverlay.dispose();

		_isOverlayDirty = false;
	}

	/**
	 * @param gcGraph
	 * @param slider
	 */
	private void drawSync410XSlider(final GC gcGraph, final ChartXSlider slider) {

		final Display display = getDisplay();

		final int devSliderLinePos = slider.getXXDevSliderLinePos() - _xxDevViewPortLeftBorder;

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		int labelIndex = 0;

		final ArrayList<ChartXSliderLabel> labelList = slider.getLabelList();

		// draw slider for each graph
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();
			final ChartXSliderLabel label = labelList.get(labelIndex);

			final Color colorLine = new Color(display, yData.getRgbLine()[0]);
			final Color colorBright = new Color(display, yData.getRgbBright()[0]);
			final Color colorDark = new Color(display, yData.getRgbDark()[0]);

			final int labelHeight = label.height;
			final int labelWidth = label.width;
			final int devXLabel = label.x;
			final int devYLabel = label.y;

			final int devYBottom = drawingData.getDevYBottom();
			final boolean isSliderHovered = _mouseOverXSlider != null && _mouseOverXSlider == slider;

			/*
			 * when the mouse is over the slider, the slider is painted in a darker color
			 */

			// draw slider line
			if ((_isFocusActive && _selectedXSlider == slider) || isSliderHovered) {
				gcGraph.setAlpha(0xd0);
			} else {
				gcGraph.setAlpha(0x60);
			}
			gcGraph.setForeground(colorLine);
			gcGraph.setLineDash(new int[] { 4, 1, 4, 1 });
			gcGraph.drawLine(devSliderLinePos, devYLabel + labelHeight, devSliderLinePos, devYBottom);

			gcGraph.setBackground(colorDark);
			gcGraph.setForeground(colorBright);

			// draw label border
			gcGraph.setForeground(colorLine);
			gcGraph.setLineStyle(SWT.LINE_SOLID);
			gcGraph.drawRoundRectangle(devXLabel, devYLabel - 4, labelWidth, labelHeight + 3, 4, 4);

			// draw slider label
			gcGraph.setAlpha(0xff);
			gcGraph.setForeground(colorTxt);
			gcGraph.drawText(label.text, devXLabel + 2, devYLabel - 5, true);

			// draw a tiny marker on the graph
			gcGraph.setBackground(colorLine);
			gcGraph.fillRectangle(devSliderLinePos - 3, label.devYGraph - 2, 7, 3);

			/*
			 * draw a marker below the x-axis to make the selection more visible
			 */
			if (_isFocusActive && slider == _selectedXSlider) {

				final int markerWidth = BAR_MARKER_WIDTH;
				final int markerWidth2 = markerWidth / 2;

				final int devMarkerXPos = devSliderLinePos - markerWidth2;

				final int[] marker = new int[] {
						devMarkerXPos,
						devYBottom + 1 + markerWidth2,
						devMarkerXPos + markerWidth2,
						devYBottom + 1,
						devMarkerXPos + markerWidth,
						devYBottom + 1 + markerWidth2 };

				gcGraph.setAlpha(0xc0);
				gcGraph.setLineStyle(SWT.LINE_SOLID);

				// draw background
				gcGraph.setBackground(colorDark);
				gcGraph.fillPolygon(marker);

				// draw border
				gcGraph.setForeground(colorLine);
				gcGraph.drawPolygon(marker);

				gcGraph.setAlpha(0xff);
			}

			colorLine.dispose();
			colorBright.dispose();
			colorDark.dispose();

			labelIndex++;
		}

		colorTxt.dispose();
	}

	/**
	 * Draw the y-slider which it hit.
	 * 
	 * @param gcGraph
	 */
	private void drawSync420YSliders(final GC gcGraph) {

		if (_hitYSlider == null) {
			return;
		}

		final Display display = getDisplay();

		final int grayColorIndex = 60;
		final Color colorTxt = new Color(display, grayColorIndex, grayColorIndex, grayColorIndex);

		final int devXChartWidth = getDevVisibleChartWidth();

		for (final ChartYSlider ySlider : _ySliders) {

			if (_hitYSlider == ySlider) {

				final ChartDataYSerie yData = ySlider.getYData();

				final Color colorLine = new Color(display, yData.getRgbLine()[0]);
				final Color colorBright = new Color(display, yData.getRgbBright()[0]);
				final Color colorDark = new Color(display, yData.getRgbDark()[0]);

				final GraphDrawingData drawingData = ySlider.getDrawingData();
				final int devYBottom = drawingData.getDevYBottom();
				final int devYTop = devYBottom - drawingData.devGraphHeight;

				final int devYSliderLine = ySlider.getDevYSliderLine();

				// set the label and line NOT outside of the chart
				int devYLabelPos = devYSliderLine;

				if (devYSliderLine > devYBottom) {
					devYLabelPos = devYBottom;
				} else if (devYSliderLine < devYTop) {
					devYLabelPos = devYTop;
				}

				// ySlider is the slider which was hit by the mouse, draw the
				// slider label

				final StringBuilder labelText = new StringBuilder();

				final float devYValue = ((float) devYBottom - devYSliderLine)
						/ drawingData.getScaleY()
						+ drawingData.getGraphYBottom();

				// create the slider text
				labelText.append(Util.formatValue(devYValue, yData.getAxisUnit(), yData.getValueDivisor(), true));
				labelText.append(' ');
				labelText.append(yData.getUnitLabel());
				labelText.append("  "); //$NON-NLS-1$
				final String label = labelText.toString();

				final Point labelExtend = gcGraph.stringExtent(label);

				final int labelHeight = labelExtend.y - 2;
				final int labelWidth = labelExtend.x + 0;
				final int labelX = _ySliderGraphX - labelWidth - 5;
				final int labelY = devYLabelPos - labelHeight;

				// draw label background
				gcGraph.setForeground(colorBright);
				gcGraph.setBackground(colorDark);
				gcGraph.setAlpha(0xb0);
				gcGraph.fillGradientRectangle(labelX, labelY, labelWidth, labelHeight, true);

				// draw label border
				gcGraph.setAlpha(0xa0);
				gcGraph.setForeground(colorLine);
				gcGraph.drawRectangle(labelX, labelY, labelWidth, labelHeight);
				gcGraph.setAlpha(0xff);

				// draw label text
				gcGraph.setForeground(colorTxt);
				gcGraph.drawText(label, labelX + 2, labelY - 2, true);

				// draw slider line
				gcGraph.setForeground(colorLine);
				gcGraph.setLineDash(DOT_DASHES);
				gcGraph.drawLine(0, devYLabelPos, devXChartWidth, devYLabelPos);

				colorLine.dispose();
				colorBright.dispose();
				colorDark.dispose();

				// only 1 y-slider can be hit
				break;
			}
		}

		colorTxt.dispose();
	}

	private void drawSync430XMarker(final GC gc) {

		final Display display = getDisplay();
		final Color colorXMarker = new Color(display, 255, 153, 0);

		final int devDraggingDiff = _devXMarkerDraggedPos - _devXMarkerDraggedStartPos;

		// draw x-marker for each graph
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataXSerie xData = drawingData.getXData();
			final float scaleX = drawingData.getScaleX();

			final float valueDraggingDiff = devDraggingDiff / scaleX;

			final int synchStartIndex = xData.getSynchMarkerStartIndex();
			final int synchEndIndex = xData.getSynchMarkerEndIndex();

			final float[] xValues = xData.getHighValues()[0];
			final float valueXStart = xValues[synchStartIndex];
			final float valueXEnd = xValues[synchEndIndex];

			final int devXStart = (int) (scaleX * valueXStart - _xxDevViewPortLeftBorder);
			final int devXEnd = (int) (scaleX * valueXEnd - _xxDevViewPortLeftBorder);
			int devMovedXStart = devXStart;
			int devMovedXEnd = devXEnd;

			final float valueXStartWithOffset = valueXStart + valueDraggingDiff;
			final float valueXEndWithOffset = valueXEnd + valueDraggingDiff;

			_movedXMarkerStartValueIndex = computeXMarkerValue(
					xValues,
					synchStartIndex,
					valueDraggingDiff,
					valueXStartWithOffset);

			_movedXMarkerEndValueIndex = computeXMarkerValue(
					xValues,
					synchEndIndex,
					valueDraggingDiff,
					valueXEndWithOffset);

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _xxDevViewPortLeftBorder);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _xxDevViewPortLeftBorder);

			/*
			 * when the moved x-marker is on the right or the left border, make sure that the
			 * x-markers don't get too small
			 */
			final float valueMovedDiff = xValues[_movedXMarkerEndValueIndex] - xValues[_movedXMarkerStartValueIndex];

			/*
			 * adjust start and end position
			 */
			if (_movedXMarkerStartValueIndex == 0 && valueMovedDiff < _xMarkerValueDiff) {

				/*
				 * the x-marker is moved to the left, the most left x-marker is on the first
				 * position
				 */

				int valueIndex;

				for (valueIndex = 0; valueIndex < xValues.length; valueIndex++) {
					if (xValues[valueIndex] >= _xMarkerValueDiff) {
						break;
					}
				}

				_movedXMarkerEndValueIndex = valueIndex;

			} else if (_movedXMarkerEndValueIndex == xValues.length - 1 && valueMovedDiff < _xMarkerValueDiff) {

				/*
				 * the x-marker is moved to the right, the most right x-marker is on the last
				 * position
				 */

				int valueIndex;
				final float valueFirstIndex = xValues[xValues.length - 1] - _xMarkerValueDiff;

				for (valueIndex = xValues.length - 1; valueIndex > 0; valueIndex--) {
					if (xValues[valueIndex] <= valueFirstIndex) {
						break;
					}
				}

				_movedXMarkerStartValueIndex = valueIndex;
			}

			if (valueMovedDiff > _xMarkerValueDiff) {

				/*
				 * force the value diff for the x-marker, the moved value diff can't be wider then
				 * one value index
				 */

				final float valueStart = xValues[_movedXMarkerStartValueIndex];
				int valueIndex;
				for (valueIndex = _movedXMarkerEndValueIndex - 0; valueIndex >= 0; valueIndex--) {
					if (xValues[valueIndex] - valueStart < _xMarkerValueDiff) {
						valueIndex++;
						break;
					}
				}
				valueIndex = Math.min(valueIndex, xValues.length - 1);

				_movedXMarkerEndValueIndex = valueIndex;
			}

			_movedXMarkerEndValueIndex = Math.min(_movedXMarkerEndValueIndex, xValues.length - 1);

			devMovedXStart = (int) (scaleX * xValues[_movedXMarkerStartValueIndex] - _xxDevViewPortLeftBorder);
			devMovedXEnd = (int) (scaleX * xValues[_movedXMarkerEndValueIndex] - _xxDevViewPortLeftBorder);

			final int devYTop = drawingData.getDevYBottom() - drawingData.devGraphHeight;
			final int devYBottom = drawingData.getDevYBottom();

			// draw moved x-marker
			gc.setForeground(colorXMarker);
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

			gc.setAlpha(0x80);

			gc.fillGradientRectangle(//
					devMovedXStart,
					devYBottom,
					devMovedXEnd - devMovedXStart,
					devYTop - devYBottom,
					true);

			gc.drawLine(devMovedXStart, devYTop, devMovedXStart, devYBottom);
			gc.drawLine(devMovedXEnd, devYTop, devMovedXEnd, devYBottom);

			gc.setAlpha(0xff);
		}

		colorXMarker.dispose();
	}

	private void drawSync440Selection(final GC gc) {

		_isSelectionDirty = false;

		final int chartType = _chart.getChartDataModel().getChartType();

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {
			switch (chartType) {
			case ChartDataModel.CHART_TYPE_LINE:
				// drawLineSelection(gc, drawingData);
				break;

			case ChartDataModel.CHART_TYPE_BAR:
				drawSync442BarSelection(gc, drawingData);
				break;

			default:
				break;
			}
		}
	}

	private void drawSync442BarSelection(final GC gc, final GraphDrawingData drawingData) {

		// check if multiple bars are selected
		boolean drawSelection = false;
		int selectedIndex = 0;
		if (_selectedBarItems != null) {
			int selectionIndex = 0;
			for (final boolean isBarSelected : _selectedBarItems) {
				if (isBarSelected) {
					if (drawSelection == false) {
						drawSelection = true;
						selectedIndex = selectionIndex;
					} else {
						drawSelection = false;
						return;
					}
				}
				selectionIndex++;
			}
		}

		if (drawSelection == false) {
			return;
		}

		/*
		 * a bar is selected
		 */

		// get the chart data
		final ChartDataYSerie yData = drawingData.getYData();
		final int[][] colorsIndex = yData.getColorsIndex();

		// get the colors
		final RGB[] rgbLine = yData.getRgbLine();
		final RGB[] rgbDark = yData.getRgbDark();
		final RGB[] rgbBright = yData.getRgbBright();

		final int devYBottom = drawingData.getDevYBottom();
//		final int devYBottom = drawingData.devGraphHeight;

		final Rectangle[][] barRectangeleSeries = drawingData.getBarRectangles();

		if (barRectangeleSeries == null) {
			return;
		}

		final int markerWidth = BAR_MARKER_WIDTH;
		final int barThickness = 1;
		final int markerWidth2 = markerWidth / 2;

		gc.setLineStyle(SWT.LINE_SOLID);

		// loop: all data series
		for (int serieIndex = 0; serieIndex < barRectangeleSeries.length; serieIndex++) {

			// get selected rectangle
			final Rectangle[] barRectangles = barRectangeleSeries[serieIndex];
			if (barRectangles == null || selectedIndex >= barRectangles.length) {
				continue;
			}

			final Rectangle barRectangle = barRectangles[selectedIndex];
			if (barRectangle == null) {
				continue;
			}

			/*
			 * current bar is selected, draw the selected bar
			 */

			final Rectangle barShapeSelected = new Rectangle(
					(barRectangle.x - markerWidth2),
					(barRectangle.y - markerWidth2),
					(barRectangle.width + markerWidth),
					(barRectangle.height + markerWidth));

			final Rectangle barBarSelected = new Rectangle(
					barRectangle.x - 1,
					barRectangle.y - barThickness,
					barRectangle.width + barThickness,
					barRectangle.height + 2 * barThickness);

			final int colorIndex = colorsIndex[serieIndex][selectedIndex];
			final RGB rgbBrightDef = rgbBright[colorIndex];
			final RGB rgbDarkDef = rgbDark[colorIndex];
			final RGB rgbLineDef = rgbLine[colorIndex];

			final Color colorBrightSelected = getColor(rgbBrightDef);
			final Color colorDarkSelected = getColor(rgbDarkDef);
			final Color colorLineSelected = getColor(rgbLineDef);

			// do't write into the x-axis units which also contains the
			// selection marker
			if (barShapeSelected.y + barShapeSelected.height > devYBottom) {
				barShapeSelected.height = devYBottom - barShapeSelected.y;
			}

			// draw the selection darker when the focus is set
			if (_isFocusActive) {
//				gc.setAlpha(0xb0);
				gc.setAlpha(0xf0);
			} else {
//				gc.setAlpha(0x70);
				gc.setAlpha(0xa0);
			}

			// fill bar background
			gc.setForeground(colorDarkSelected);
			gc.setBackground(colorBrightSelected);

			gc.fillGradientRectangle(
					barShapeSelected.x + 1,
					barShapeSelected.y + 1,
					barShapeSelected.width - 1,
					barShapeSelected.height - 1,
					true);

			// draw bar border
			gc.setForeground(colorLineSelected);
			gc.drawRoundRectangle(
					barShapeSelected.x,
					barShapeSelected.y,
					barShapeSelected.width,
					barShapeSelected.height,
					4,
					4);

			// draw bar thicker
			gc.setBackground(colorDarkSelected);
			gc.fillRoundRectangle(//
					barBarSelected.x,
					barBarSelected.y,
					barBarSelected.width,
					barBarSelected.height,
					2,
					2);

			/*
			 * draw a marker below the x-axis to make the selection more visible
			 */
			if (_isFocusActive) {

				final int devMarkerXPos = barRectangle.x + (barRectangle.width / 2) - markerWidth2;

				final int[] marker = new int[] {
						devMarkerXPos,
						devYBottom + 1 + markerWidth2,
						devMarkerXPos + markerWidth2,
						devYBottom + 1,
						devMarkerXPos + markerWidth - 0,
						devYBottom + 1 + markerWidth2 };

				// draw background
				gc.setBackground(colorDarkSelected);
				gc.fillPolygon(marker);

				// draw border
				gc.setForeground(colorLineSelected);
				gc.drawPolygon(marker);

				gc.setAlpha(0xff);
			}
		}
	}

	private void drawSync450HoveredBar(final GC gcOverlay) {

		// check if hovered bar is disabled
		if (_hoveredBarSerieIndex == -1) {
			return;
		}

		// draw only bar chars
		if (_chart.getChartDataModel().getChartType() != ChartDataModel.CHART_TYPE_BAR) {
			return;
		}

		gcOverlay.setLineStyle(SWT.LINE_SOLID);
		gcOverlay.setAlpha(0xd0);

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			// get the chart data
			final ChartDataYSerie yData = drawingData.getYData();
			final int serieLayout = yData.getChartLayout();
			final int[][] colorsIndex = yData.getColorsIndex();

			// get the colors
			final RGB[] rgbLine = yData.getRgbLine();
			final RGB[] rgbDark = yData.getRgbDark();
			final RGB[] rgbBright = yData.getRgbBright();

			final int devYBottom = drawingData.getDevYBottom();
//			final int devYBottom = drawingData.devGraphHeight;

			final Rectangle[][] barRectangeleSeries = drawingData.getBarRectangles();

			final int markerWidth = BAR_MARKER_WIDTH;
			final int markerWidth2 = markerWidth / 2;

			// loop: all data series
			for (int serieIndex = 0; serieIndex < barRectangeleSeries.length; serieIndex++) {

				// get hovered rectangle
				final Rectangle hoveredRectangle = barRectangeleSeries[serieIndex][_hoveredBarValueIndex];

				if (hoveredRectangle == null) {
					continue;
				}

				if (serieIndex != _hoveredBarSerieIndex) {
					continue;
				}

				final int colorIndex = colorsIndex[serieIndex][_hoveredBarValueIndex];
				final RGB rgbBrightDef = rgbBright[colorIndex];
				final RGB rgbDarkDef = rgbDark[colorIndex];
				final RGB rgbLineDef = rgbLine[colorIndex];

				final Color colorBright = getColor(rgbBrightDef);
				final Color colorDark = getColor(rgbDarkDef);
				final Color colorLine = getColor(rgbLineDef);

				if (serieLayout != ChartDataYSerie.BAR_LAYOUT_STACKED) {

				}

				final Rectangle hoveredBarShape = new Rectangle(
						(hoveredRectangle.x - markerWidth2),
						(hoveredRectangle.y - markerWidth2),
						(hoveredRectangle.width + markerWidth),
						(hoveredRectangle.height + markerWidth));

				// do't write into the x-axis units which also contains the
				// selection marker
				if (hoveredBarShape.y + hoveredBarShape.height > devYBottom) {
					hoveredBarShape.height = devYBottom - hoveredBarShape.y;
				}

				// fill bar background
				gcOverlay.setForeground(colorDark);
				gcOverlay.setBackground(colorBright);

				gcOverlay.fillGradientRectangle(
						hoveredBarShape.x + 1,
						hoveredBarShape.y + 1,
						hoveredBarShape.width - 1,
						hoveredBarShape.height - 1,
						true);

				// draw bar border
				gcOverlay.setForeground(colorLine);
				gcOverlay.drawRoundRectangle(
						hoveredBarShape.x,
						hoveredBarShape.y,
						hoveredBarShape.width,
						hoveredBarShape.height,
						4,
						4);
			}
		}

		gcOverlay.setAlpha(0xff);
	}

	private void drawSync460HoveredLine(final GC gcOverlay) {

		int graphIndex = 0;

		gcOverlay.setAntialias(SWT.ON);

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			// draw only line graphs
			if (_chart.getChartDataModel().getChartType() != ChartDataModel.CHART_TYPE_LINE) {
				continue;
			}

			// get the chart data
			final ChartDataYSerie yData = drawingData.getYData();
			final int[][] colorsIndex = yData.getColorsIndex();

			/*
			 * get hovered rectangle
			 */
			// check bounds
			if (_lineDevPositions.size() - 1 < graphIndex) {
				return;
			}

			// check bounds
			final Point[] lineDevPositions = _lineDevPositions.get(graphIndex);
			if (lineDevPositions.length - 1 < graphIndex) {
				return;
			}

			final Rectangle[] lineFocusRectangles = _lineFocusRectangles.get(graphIndex);

			final Point devPosition = lineDevPositions[_hoveredLineValueIndex];
			final Rectangle hoveredRectangle = lineFocusRectangles[_hoveredLineValueIndex];

			// check if hovered line positions are set
			if (hoveredRectangle == null || devPosition == null) {
				continue;
			}

			int devX;
			final int devVisibleChartWidth = getDevVisibleChartWidth();
			Color colorLine;

			/*
			 * paint the points which are outside of the visible area at the border with gray color
			 */
			if (devPosition.x < 0) {

				devX = 0;
				colorLine = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

			} else if (devPosition.x > devVisibleChartWidth) {

				devX = devVisibleChartWidth;
				colorLine = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

			} else {

				devX = devPosition.x;

				// get the colors
				final RGB[] rgbLine = yData.getRgbLine();
				final int colorIndex = colorsIndex[0][_hoveredLineValueIndex];

				final RGB rgbLineDef = rgbLine[colorIndex];
				colorLine = getColor(rgbLineDef);
			}

			// draw value point marker
			final int devOffsetFill = 10;
			final int devOffsetPoint = 3;

			gcOverlay.setBackground(colorLine);

			gcOverlay.setAlpha(0x40);
			gcOverlay.fillOval(//
					devX - devOffsetFill,
					devPosition.y - devOffsetFill,
					devOffsetFill * 2,
					devOffsetFill * 2);

			gcOverlay.setAlpha(0xff);
			gcOverlay.fillOval(//
					devX - devOffsetPoint,
					devPosition.y - devOffsetPoint,
					devOffsetPoint * 2,
					devOffsetPoint * 2);

			// move to next graph
			graphIndex++;
		}

		gcOverlay.setAntialias(SWT.OFF);
	}

	private void drawSyncBg999ErrorMessage(final GC gc) {

		final String errorMessage = _chartComponents.errorMessage;
		if (errorMessage != null) {
			gc.drawText(errorMessage, 0, 10);
		}
	}

//	/**
//	 * Fills the surrounding area of an rectangle with background color
//	 *
//	 * @param gc
//	 * @param imageRect
//	 */
//	private void fillImagePadding(final GC gc, final Rectangle imageRect) {
//
//		final int clientHeight = getDevVisibleGraphHeight();
//		final int visibleGraphWidth = getDevVisibleChartWidth();
//
//		gc.setBackground(_chart.getBackgroundColor());
//
//		gc.fillRectangle(imageRect.width, 0, visibleGraphWidth, clientHeight);
//		gc.fillRectangle(0, imageRect.height, visibleGraphWidth, clientHeight);
//	}

	/**
	 * @param rgb
	 * @return Returns the color from the color cache, the color must not be disposed this is done
	 *         when the cache is disposed
	 */
	private Color getColor(final RGB rgb) {

// !!! this is a performance bottleneck !!!
//		final String colorKey = rgb.toString();

		final String colorKey = Integer.toString(rgb.hashCode());
		final Color color = _colorCache.get(colorKey);

		if (color == null) {
			return _colorCache.getColor(colorKey, rgb);
		} else {
			return color;
		}
	}

	/**
	 * @return Returns the viewport (visible width) of the chart graph
	 */
	private int getDevVisibleChartWidth() {
		return _chartComponents.getDevVisibleChartWidth();
	}

	/**
	 * @return Returns the visible height of the chart graph
	 */
	private int getDevVisibleGraphHeight() {
		return _chartComponents.getDevVisibleChartHeight();
	}

	double getGraphZoomRatio() {
		return _graphZoomRatio;
	}

	int getHoveredLineValueIndex() {
		return _hoveredLineValueIndex;
	}

	/**
	 * @return Returns the left slider
	 */
	ChartXSlider getLeftSlider() {
		return _xSliderA.getXXDevSliderLinePos() < _xSliderB.getXXDevSliderLinePos() ? _xSliderA : _xSliderB;
	}

	/**
	 * @return Returns the right most slider
	 */
	ChartXSlider getRightSlider() {
		return _xSliderA.getXXDevSliderLinePos() < _xSliderB.getXXDevSliderLinePos() ? _xSliderB : _xSliderA;
	}

	ChartXSlider getSelectedSlider() {

		final ChartXSlider slider = _selectedXSlider;

		if (slider == null) {
			return getLeftSlider();
		}
		return slider;
	}

	/**
	 * @return Returns the x-Data in the drawing data list
	 */
	private ChartDataXSerie getXData() {
		if (_graphDrawingData.size() == 0) {
			return null;
		} else {
			return _graphDrawingData.get(0).getXData();
		}
	}

	private GraphDrawingData getXDrawingData() {
		return _graphDrawingData.get(0);
	}

	/**
	 * @return Returns the virtual graph image width, this is the width of the graph image when the
	 *         full graph would be displayed
	 */
	int getXXDevGraphWidth() {
		return _xxDevGraphWidth;
	}

	/**
	 * @return When the graph is zoomed, the chart shows only a part of the whole graph in the
	 *         viewport. Returns the left border of the viewport.
	 */
	int getXXDevViewPortOffset() {
		return _xxDevViewPortLeftBorder;
	}

	private void handleChartResizeForSliders() {

		// update the width in the sliders
		final int visibleGraphHeight = getDevVisibleGraphHeight();

		getLeftSlider().handleChartResize(visibleGraphHeight);
		getRightSlider().handleChartResize(visibleGraphHeight);
	}

	/**
	 * check if mouse has moved over a bar
	 * 
	 * @param devX
	 * @param devY
	 */
	private boolean isBarHit(final int devX, final int devY) {

		boolean isBarHit = false;

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final Rectangle[][] barFocusRectangles = drawingData.getBarFocusRectangles();
			if (barFocusRectangles == null) {
				break;
			}

			final int serieLength = barFocusRectangles.length;

			// find the rectangle which is hovered by the mouse
			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

				final Rectangle[] serieRectangles = barFocusRectangles[serieIndex];

				for (int valueIndex = 0; valueIndex < serieRectangles.length; valueIndex++) {

					final Rectangle barInfoFocus = serieRectangles[valueIndex];

					// test if the mouse is within a bar focus rectangle
					if (barInfoFocus != null && barInfoFocus.contains(devX, devY)) {

						// keep the hovered bar index
						_hoveredBarSerieIndex = serieIndex;
						_hoveredBarValueIndex = valueIndex;

						_hoveredBarToolTip.toolTip10Show(devX, 100, serieIndex, valueIndex);

						isBarHit = true;
						break;
					}
				}
				if (isBarHit) {
					break;
				}
			}

			if (isBarHit) {
				break;
			}
		}

		if (isBarHit == false) {

			_hoveredBarToolTip.toolTip20Hide();

			if (_hoveredBarSerieIndex != -1) {

				/*
				 * hide last hovered bar, because the last hovered bar is visible
				 */

				// set status: no bar is hovered
				_hoveredBarSerieIndex = -1;

				// force redraw
				isBarHit = true;
			}
		}

		return isBarHit;
	}

	private boolean isInXSliderSetArea(final int devYMouse) {

		final int devVisibleChartHeight = _chartComponents.getDevVisibleChartHeight();
		final int devSetArea = (int) Math.min(100, devVisibleChartHeight * 0.3);

		Cursor cursor = null;

		if (devYMouse < devSetArea) {

			cursor = _cursorXSliderLeft;

			_isSetXSliderPositionLeft = true;
			_isSetXSliderPositionRight = false;

		} else if (devYMouse > (devVisibleChartHeight - devSetArea)) {

			cursor = _cursorXSliderRight;

			_isSetXSliderPositionLeft = false;
			_isSetXSliderPositionRight = true;
		}

		if (cursor != null) {

			setCursor(cursor);

			return true;

		} else {

			_isSetXSliderPositionLeft = false;
			_isSetXSliderPositionRight = false;

			return false;
		}
	}

	/**
	 * Check if mouse has moved over a line value.
	 * 
	 * @return
	 */
	private boolean isLineHovered() {

		if (_lineDevPositions.size() == 0) {
			return false;
		}

		Rectangle lineRect = null;

		for (final Rectangle[] lineFocusRectangles : _lineFocusRectangles) {

			// find the line rectangle which is hovered by the mouse
			for (int valueIndex = 0; valueIndex < lineFocusRectangles.length; valueIndex++) {

				lineRect = lineFocusRectangles[valueIndex];

				// test if the mouse is within a bar focus rectangle
				if (lineRect != null) {

					if (lineRect.contains(_devXMouseMove, _devYMouseMove)) {

						// keep the hovered line index
						_hoveredLineValueIndex = valueIndex;

						return true;
					}
				}
			}
		}

		// reset index
		_hoveredLineValueIndex = -1;

		return false;
	}

	/**
	 * @param devXGraph
	 * @return Returns <code>true</code> when the synch marker was hit
	 */
	private boolean isSynchMarkerHit(final int devXGraph) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return false;
		}

		final int synchMarkerStartIndex = xData.getSynchMarkerStartIndex();
		final int synchMarkerEndIndex = xData.getSynchMarkerEndIndex();

		if (synchMarkerStartIndex == -1) {
			// synch marker is not set
			return false;
		}

		final float[] xValues = xData.getHighValues()[0];
		final float scaleX = getXDrawingData().getScaleX();

		final int devXMarkerStart = (int) (xValues[Math.min(synchMarkerStartIndex, xValues.length - 1)] * scaleX - _xxDevViewPortLeftBorder);
		final int devXMarkerEnd = (int) (xValues[Math.min(synchMarkerEndIndex, xValues.length - 1)] * scaleX - _xxDevViewPortLeftBorder);

		if (devXGraph >= devXMarkerStart && devXGraph <= devXMarkerEnd) {
			return true;
		}

		return false;
	}

	private ChartXSlider isXSliderHit(final int devXMouse, final int devYMouse) {

		ChartXSlider xSlider = null;

		if (_xSliderA.getHitRectangle().contains(devXMouse, devYMouse)) {
			xSlider = _xSliderA;
		} else if (_xSliderB.getHitRectangle().contains(devXMouse, devYMouse)) {
			xSlider = _xSliderB;
		}

		return xSlider;
	}

	/**
	 * check if the mouse hit an y-slider and returns the hit slider
	 * 
	 * @param graphX
	 * @param devY
	 * @return
	 */
	private ChartYSlider isYSliderHit(final int graphX, final int devY) {

		if (_ySliders == null) {
			return null;
		}

		for (final ChartYSlider ySlider : _ySliders) {
			if (ySlider.getHitRectangle().contains(graphX, devY)) {
				_hitYSlider = ySlider;
				return ySlider;
			}
		}

		// hide previously hitted y-slider
		if (_hitYSlider != null) {

			// redraw the sliders to hide the labels
			_hitYSlider = null;
			_isSliderDirty = true;
			redraw();
		}

		return null;
	}

	/**
	 * Move left slider to the mouse down position
	 */
	void moveLeftSliderHere() {

		final ChartXSlider leftSlider = getLeftSlider();
		final int xxDevLeftPosition = _xxDevViewPortLeftBorder + _devXMouseDown;

		computeXSliderValue(leftSlider, xxDevLeftPosition);

		leftSlider.moveToXXDevPosition(xxDevLeftPosition, true, true);

		setZoomInPosition();

		_isSliderDirty = true;
		redraw();
	}

	/**
	 * Move right slider to the mouse down position
	 */
	void moveRightSliderHere() {

		final ChartXSlider rightSlider = getRightSlider();
		final int xxDevRightPosition = _xxDevViewPortLeftBorder + _devXMouseDown;

		computeXSliderValue(rightSlider, xxDevRightPosition);

		rightSlider.moveToXXDevPosition(xxDevRightPosition, true, true);

		setZoomInPosition();

		_isSliderDirty = true;
		redraw();
	}

	private void moveSlidersToBorder() {

		if (_canAutoMoveSliders == false) {
			return;
		}

		moveSlidersToBorderWithoutCheck();
	}

	void moveSlidersToBorderWithoutCheck() {

		/*
		 * get the sliders first before they are moved
		 */
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		/*
		 * adjust left slider
		 */
		final int xxDevLeftPosition = _xxDevViewPortLeftBorder + 2;

		computeXSliderValue(leftSlider, xxDevLeftPosition);
		leftSlider.moveToXXDevPosition(xxDevLeftPosition, true, true);

		/*
		 * adjust right slider
		 */
		final int xxDevRightPosition = _xxDevViewPortLeftBorder + getDevVisibleChartWidth() - 2;

		computeXSliderValue(rightSlider, xxDevRightPosition);
		rightSlider.moveToXXDevPosition(xxDevRightPosition, true, true);

		_isSliderDirty = true;
		redraw();
	}

	/**
	 * Move the slider to a new position
	 * 
	 * @param xSlider
	 *            Current slider
	 * @param xxDevSliderLinePos
	 *            x coordinate for the slider line within the graph, this can be outside of the
	 *            visible graph
	 */
	private void moveXSlider(final ChartXSlider xSlider, final int devXSliderLinePos) {

		int xxDevSliderLinePos = _xxDevViewPortLeftBorder + devXSliderLinePos;

		/*
		 * adjust the line position the the min/max width of the graph image
		 */
		xxDevSliderLinePos = Math.min(_xxDevGraphWidth, Math.max(0, xxDevSliderLinePos));

		computeXSliderValue(xSlider, xxDevSliderLinePos);

		// set new slider line position
		xSlider.moveToXXDevPosition(xxDevSliderLinePos, true, true);
	}

	/**
	 * Dispose event handler
	 */
	private void onDispose() {

		// dispose resources
		_cursorResizeLeftRight = Util.disposeResource(_cursorResizeLeftRight);
		_cursorResizeTopDown = Util.disposeResource(_cursorResizeTopDown);
		_cursorDragged = Util.disposeResource(_cursorDragged);
		_cursorModeSlider = Util.disposeResource(_cursorModeSlider);
		_cursorModeZoom = Util.disposeResource(_cursorModeZoom);
		_cursorModeZoomMove = Util.disposeResource(_cursorModeZoomMove);
		_cursorDragXSlider_ModeZoom = Util.disposeResource(_cursorDragXSlider_ModeZoom);
		_cursorDragXSlider_ModeSlider = Util.disposeResource(_cursorDragXSlider_ModeSlider);
		_cursorHoverXSlider = Util.disposeResource(_cursorHoverXSlider);

		_cursorMove1x = Util.disposeResource(_cursorMove1x);
		_cursorMove2x = Util.disposeResource(_cursorMove2x);
		_cursorMove3x = Util.disposeResource(_cursorMove3x);
		_cursorMove4x = Util.disposeResource(_cursorMove4x);
		_cursorMove5x = Util.disposeResource(_cursorMove5x);

		_cursorXSliderLeft = Util.disposeResource(_cursorXSliderLeft);
		_cursorXSliderRight = Util.disposeResource(_cursorXSliderRight);

		_chartImage20Chart = Util.disposeResource(_chartImage20Chart);
		_chartImage10Graphs = Util.disposeResource(_chartImage10Graphs);
		_chartImage40Overlay = Util.disposeResource(_chartImage40Overlay);
		_chartImage30Custom = Util.disposeResource(_chartImage30Custom);

		_gridColor = Util.disposeResource(_gridColor);
		_gridColorMajor = Util.disposeResource(_gridColorMajor);

		_hoveredBarToolTip.dispose();

		_colorCache.dispose();
	}

	private void onKeyDown(final Event event) {

		switch (_chart.getChartDataModel().getChartType()) {
		case ChartDataModel.CHART_TYPE_BAR:
			_chartComponents.selectBarItem(event);
			break;

		case ChartDataModel.CHART_TYPE_LINE:

			switch (event.character) {
			case '+':
				_chart.onExecuteZoomIn();
				break;

			case '-':
				_chart.onExecuteZoomOut(true);
				break;

			default:
				onKeyDownMoveXSlider(event);
			}

			break;

		default:
			break;
		}
	}

	/**
	 * move the x-slider with the keyboard
	 * 
	 * @param event
	 */
	private void onKeyDownMoveXSlider(final Event event) {

		final int keyCode = event.keyCode;

		/*
		 * keyboard events behaves different than the mouse event, shift & ctrl can be set in both
		 * event fields
		 */
		boolean isShift = (event.stateMask & SWT.SHIFT) != 0 || (keyCode & SWT.SHIFT) != 0;
		boolean isCtrl = (event.stateMask & SWT.CTRL) != 0 || (keyCode & SWT.CTRL) != 0;

		// ensure a slider is selected
		if (_selectedXSlider == null) {
			final ChartXSlider leftSlider = getLeftSlider();
			if (leftSlider != null) {
				// set default slider
				_selectedXSlider = leftSlider;
			} else {
				return;
			}
		}

		// toggle selected slider with the shift key
		if (isShift && isCtrl == false) {
			_selectedXSlider = _selectedXSlider == _xSliderA ? _xSliderB : _xSliderA;
			_isSliderDirty = true;
			redraw();

			return;
		}

		// accelerate with page up/down
		if (keyCode == SWT.PAGE_UP || keyCode == SWT.PAGE_DOWN) {
			isCtrl = true;
			isShift = true;
		}

		// accelerate slider move speed depending on shift/ctrl key
		int valueIndexDiff = isCtrl ? 10 : 1;
		valueIndexDiff *= isShift ? 10 : 1;

		int valueIndex = _selectedXSlider.getValuesIndex();
		final float[] xValues = getXData().getHighValues()[0];

		boolean isMoveSlider = false;

//		if (isShift && isCtrl) {
//
//			/*
//			 * this will reposition the x-slider to the exact value position in the graph, the Ctrl
//			 * key must be pressed first before the Shift key otherwise the slider is toggles
//			 */
//			isMoveSlider = true;
//
//		} else
		{

			switch (keyCode) {
			case SWT.PAGE_DOWN:
			case SWT.ARROW_RIGHT:

				valueIndex += valueIndexDiff;

				// wrap around
				if (valueIndex >= xValues.length) {
					valueIndex = 0;
				}

				isMoveSlider = true;

				break;

			case SWT.PAGE_UP:
			case SWT.ARROW_LEFT:

				valueIndex -= valueIndexDiff;

				// wrap around
				if (valueIndex < 0) {
					valueIndex = xValues.length - 1;
				}

				isMoveSlider = true;

				break;

			case SWT.HOME:

				valueIndex = 0;

				isMoveSlider = true;

				break;

			case SWT.END:

				valueIndex = xValues.length - 1;

				isMoveSlider = true;

				break;
			}
		}

		if (isMoveSlider) {

			setXSliderValueIndex(_selectedXSlider, valueIndex, false);

			redraw();
			setCursorStyle(event.y);
		}
	}

	void onMouseDoubleClick(final MouseEvent e) {

		// stop dragging the x-slider
		_xSliderDragged = null;

		if (_hoveredBarSerieIndex != -1) {

			/*
			 * execute the action which is defined when a bar is selected with the left mouse button
			 */

			_chart.fireChartDoubleClick(_hoveredBarSerieIndex, _hoveredBarValueIndex);

		} else {

			if ((e.stateMask & SWT.CONTROL) != 0) {

				// toggle mouse mode

				if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {

					// switch to mouse zoom mode
					_chart.setMouseMode(false);

				} else {

					// switch to mouse slider mode
					_chart.setMouseMode(true);
				}

			} else {

				if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {

					// switch to mouse zoom mode
					_chart.setMouseMode(false);
				}

				// mouse mode: zoom chart

				/*
				 * set position where the double click occured, this position will be used when the
				 * chart is zoomed
				 */
				final double xxDevMousePosInChart = _xxDevViewPortLeftBorder + e.x;
				_zoomRatioCenter = xxDevMousePosInChart / _xxDevGraphWidth;

				zoomInWithMouse(Integer.MIN_VALUE);
			}
		}
	}

	/**
	 * Mouse down event handler
	 * 
	 * @param event
	 */
	private void onMouseDown(final MouseEvent event) {

		// zoom out to show the whole chart with the button on the left side
		if (event.button == 4) {
			_chart.onExecuteZoomFitGraph();
			return;
		}

		final int devXMouse = event.x;
		final int devYMouse = event.y;

		final boolean isShift = (event.stateMask & SWT.SHIFT) != 0;
		final boolean isCtrl = (event.stateMask & SWT.CTRL) != 0;

		_devXMouseDown = devXMouse;
		_devYMouseDown = devYMouse;

		// show slider context menu
		if (event.button != 1) {

			// stop dragging the x-slider
			_xSliderDragged = null;

			if (event.button == 3) {

				// right button is pressed

				computeSliderForContextMenu(devXMouse, devYMouse);
			}
			return;
		}

		// use external mouse event listener
		if (_chart.isMouseDownExternalPre(devXMouse, devYMouse)) {
			return;
		}

		if (_xSliderDragged != null) {

			// x-slider is dragged

			// keep x-slider
			final ChartXSlider xSlider = _xSliderDragged;

			// stop dragging the slider
			_xSliderDragged = null;

			// set mouse zoom double click position
			final double xxDevMousePosInChart = _xxDevViewPortLeftBorder + devXMouse;
			_zoomRatioCenter = xxDevMousePosInChart / _xxDevGraphWidth;

			/*
			 * make sure that the slider is exactly positioned where the value is displayed in the
			 * graph
			 */
			setXSliderValueIndex(xSlider, _hoveredLineValueIndex, false);

			_isSliderDirty = true;
			redraw();

		} else {

			// check if a x-slider was hit
			_xSliderDragged = null;
			if (_xSliderA.getHitRectangle().contains(devXMouse, devYMouse)) {
				_xSliderDragged = _xSliderA;
			} else if (_xSliderB.getHitRectangle().contains(devXMouse, devYMouse)) {
				_xSliderDragged = _xSliderB;
			}

			if (_xSliderDragged != null) {

				// x-slider is dragged, stop dragging

				_xSliderOnTop = _xSliderDragged;
				_xSliderOnBottom = _xSliderOnTop == _xSliderA ? _xSliderB : _xSliderA;

				// set the hit offset for the mouse click
				_xSliderDragged.setDevXClickOffset(devXMouse - _xxDevViewPortLeftBorder);

				// the hit x-slider is now the selected x-slider
				_selectedXSlider = _xSliderDragged;
				_isSelectionVisible = true;
				_isSliderDirty = true;

				redraw();

			} else if (_ySliderDragged != null) {

				// y-slider is dragged, stop dragging

				adjustYSlider();

			} else if ((_ySliderDragged = isYSliderHit(devXMouse, devYMouse)) != null) {

				// start y-slider dragging

				_ySliderDragged.devYClickOffset = devYMouse - _ySliderDragged.getHitRectangle().y;

			} else if (_hoveredBarSerieIndex != -1) {

				actionSelectBars();

			} else if (_chart._draggingListenerXMarker != null && isSynchMarkerHit(devXMouse)) {

				/*
				 * start to move the x-marker, when a dragging listener and the x-marker was hit
				 */

				_isXMarkerMoved = getXData().getSynchMarkerStartIndex() != -1;

				if (_isXMarkerMoved) {

					_devXMarkerDraggedStartPos = devXMouse;
					_devXMarkerDraggedPos = devXMouse;
					_xMarkerValueDiff = _chart._draggingListenerXMarker.getXMarkerValueDiff();

					_isSliderDirty = true;
					redraw();
				}

			} else if (_isXSliderVisible //
					//
					// x-slider is NOT dragged
					&& _xSliderDragged == null
					//
					&& (isShift || isCtrl || _isSetXSliderPositionLeft || _isSetXSliderPositionRight)) {

				// position the x-slider and start dragging it

				if (_isSetXSliderPositionLeft) {
					_xSliderDragged = getLeftSlider();
				} else if (_isSetXSliderPositionRight) {
					_xSliderDragged = getRightSlider();
				} else if (isCtrl) {
					// ctrl is pressed -> left slider
					_xSliderDragged = getRightSlider();
				} else {
					// shift is pressed -> right slider
					_xSliderDragged = getLeftSlider();
				}

				_xSliderOnTop = _xSliderDragged;
				_xSliderOnBottom = _xSliderOnTop == _xSliderA ? _xSliderB : _xSliderA;

				// the left x-slider is now the selected x-slider
				_selectedXSlider = _xSliderDragged;
				_isSelectionVisible = true;

				/*
				 * move the left slider to the mouse down position
				 */

				_xSliderDragged.setDevXClickOffset(devXMouse - _xxDevViewPortLeftBorder);

				// keep position of the slider line
				final int devXSliderLinePos = devXMouse;
				_devXDraggedXSliderLine = devXSliderLinePos;

				moveXSlider(_xSliderDragged, devXSliderLinePos);

				_isSliderDirty = true;
				redraw();

			} else if (_graphZoomRatio > 1) {

				// start dragging the chart

				/*
				 * to prevent flickering with the double click event, dragged started is used
				 */
				_isChartDraggedStarted = true;

				_draggedChartStartPos = new Point(event.x, event.y);

				/*
				 * set also the move position because when changing the data model, the old position
				 * will be used and the chart is painted on the wrong position on mouse down
				 */
				_draggedChartDraggedPos = _draggedChartStartPos;
			}
		}

		setCursorStyle(devYMouse);
	}

	/**
	 * Mouse down event in the x-axis area
	 * 
	 * @param event
	 */
	void onMouseDownAxis(final MouseEvent event) {

		if (_xSliderDragged != null) {

			// stop dragging the slider
			_xSliderDragged = null;

			doAutoZoomToXSliders();
		}
	}

	private void onMouseEnter(final MouseEvent mouseEvent) {

		if (_ySliderDragged != null) {

			_hitYSlider = _ySliderDragged;

			_isSliderDirty = true;
			redraw();
		}

	}

	void onMouseEnterAxis(final MouseEvent event) {

		// set true when mouse was moved from graph
		_isMouseMovedFromGraph = _mouseTimeExit == (event.time & 0xFFFFFFFFL);
	}

	/**
	 * Mouse exit event handler
	 * 
	 * @param event
	 */
	private void onMouseExit(final MouseEvent event) {

		_hoveredBarToolTip.toolTip20Hide();

		if (_isAutoScroll) {
			// stop autoscrolling
			_isAutoScroll = false;

		} else if (_xSliderDragged == null) {

			// hide the y-slider labels
			if (_hitYSlider != null) {
				_hitYSlider = null;

				_isSliderDirty = true;
				redraw();
			}
		}

		if (_mouseOverXSlider != null) {
			// mouse left the x-slider
			_mouseOverXSlider = null;
			_isSliderDirty = true;
			redraw();
		}

		// set mouse exit time
		_mouseTimeExit = (event.time & 0xFFFFFFFFL);
		_isMouseMovedFromGraph = false;

		setCursorStyle(event.y);
	}

	/**
	 * @param mouseEvent
	 * @return Returns <code>true</code> when the mouse event was handled.
	 */
	boolean onMouseExitAxis(final MouseEvent mouseEvent) {

		if (_isAutoScroll) {

			// stop autoscrolling with x-slider
			_isAutoScroll = false;

			// stop autoscrolling without x-slider
			_devXAutoScrollMousePosition = Integer.MIN_VALUE;

			// hide move/scroll cursor
			if (mouseEvent.widget instanceof ChartComponentAxis) {
				((ChartComponentAxis) mouseEvent.widget).setCursor(null);
			}

			return true;
		}

		return false;
	}

	/**
	 * Mouse move event handler
	 * 
	 * @param event
	 */
	private void onMouseMove(final int devXMouse, final int devYMouse) {

		_devXMouseMove = devXMouse;
		_devYMouseMove = devYMouse;

		boolean isRedraw = false;

		if (_isXSliderVisible && _xSliderDragged != null) {

			// x-slider is dragged

			// keep position of the slider line
			_devXDraggedXSliderLine = devXMouse;

			/*
			 * when the x-slider is outside of the visual graph in horizontal direction, the graph
			 * can be scrolled with the mouse
			 */
			final int devVisibleChartWidth = getDevVisibleChartWidth();
			if (_devXDraggedXSliderLine > -1 && _devXDraggedXSliderLine < devVisibleChartWidth) {

				// slider is within the visible area, autoscrolling is NOT done

				// autoscroll could be active, disable it
				_isAutoScroll = false;

				moveXSlider(_xSliderDragged, devXMouse);

				_isSliderDirty = true;
				isRedraw = true;

			} else {

				/*
				 * slider is outside the visible area, auto scroll the slider and graph when this is
				 * not yet done
				 */
				if (_isAutoScroll == false) {
					doAutoScroll();
				}
			}

		} else if (_isChartDraggedStarted || _isChartDragged) {

			// chart is dragged with the mouse

			_isChartDraggedStarted = false;
			_isChartDragged = true;

			_draggedChartDraggedPos = new Point(devXMouse, devYMouse);

			isRedraw = true;

		} else if (_isYSliderVisible && _ySliderDragged != null) {

			// y-slider is dragged

			final int devYSliderLine = devYMouse
					- _ySliderDragged.devYClickOffset
					+ ChartYSlider.halfSliderHitLineHeight;

			_ySliderDragged.setDevYSliderLine(devYSliderLine);
			_ySliderGraphX = devXMouse;

			_isSliderDirty = true;
			isRedraw = true;

		} else if (_isXMarkerMoved) {

			_devXMarkerDraggedPos = devXMouse;

			_isSliderDirty = true;
			isRedraw = true;

		} else {

			ChartXSlider xSlider;

			if (_chart.isMouseMoveExternal(devXMouse, devYMouse)) {

				// set the cursor shape depending on the mouse location
				setCursor(_cursorDragged);

			} else if (_isXSliderVisible && (xSlider = isXSliderHit(devXMouse, devYMouse)) != null) {

				// mouse is over an x-slider

				if (_mouseOverXSlider != xSlider) {

					// a new x-slider is hovered

					_mouseOverXSlider = xSlider;

					// hide the y-slider
					_hitYSlider = null;

					_isSliderDirty = true;
					isRedraw = true;
				}

				// set cursor
				setCursor(_cursorResizeLeftRight);

			} else if (_mouseOverXSlider != null) {

				// mouse has left the x-slider

				_mouseOverXSlider = null;
				_isSliderDirty = true;
				isRedraw = true;

			} else if (_isYSliderVisible && isYSliderHit(devXMouse, devYMouse) != null) {

				// cursor is within a y-slider

				setCursor(_cursorResizeTopDown);

				// show the y-slider labels
				_ySliderGraphX = devXMouse;

				_isSliderDirty = true;
				isRedraw = true;

			} else if (_chart._draggingListenerXMarker != null && isSynchMarkerHit(devXMouse)) {

				setCursor(_cursorDragged);

			} else if (_isXSliderVisible && isInXSliderSetArea(devYMouse)) {

				// cursor is already set

			} else if (isBarHit(devXMouse, devYMouse)) {

				_isHoveredBarDirty = true;
				isRedraw = true;

				setCursorStyle(devYMouse);

			} else {

				setCursorStyle(devYMouse);
			}
		}

		if (_isHoveredLineVisible && isLineHovered()) {

			if (valuePointToolTip != null) {
				valuePointToolTip.setValueIndex(_hoveredLineValueIndex, _devXMouseMove, _devYMouseMove);
			}

			isRedraw = true;

		} else {

			if (valuePointToolTip != null) {
				valuePointToolTip.hide();
			}
		}

		if (isRedraw) {
			redraw();
		}
	}

	/**
	 * Mouse has been moved in the value point tooltip, move the slider and/or hovered line (value
	 * point) accordingly.
	 * 
	 * @param mouseDisplayRelativePosition
	 */
	protected void onMouseMove(final Point mouseDisplayRelativePosition) {

		final Point devPos = toControl(mouseDisplayRelativePosition);

		if (getBounds().contains(devPos)) {
			onMouseMove(devPos.x, devPos.y);
		}
	}

	/**
	 * @param mouseEvent
	 * @return Returns <code>true</code> when the mouse event was handled.
	 */
	boolean onMouseMoveAxis(final MouseEvent mouseEvent) {

		Rectangle clientArea = null;
		ChartComponentAxis axisComponent = null;
		final Cursor cursor;

		if (mouseEvent.widget instanceof ChartComponentAxis) {

			axisComponent = (ChartComponentAxis) mouseEvent.widget;
			clientArea = axisComponent.getAxisClientArea();

			if (clientArea == null) {
				return false;
			}
		}

		// ensure that the upper part of the chart is reserved for the tour info icon
		if (mouseEvent.y < TOUR_INFO_ICON_KEEP_OUT_AREA ||
		// chart is not zoomed
				_graphZoomRatio == 1) {

			// disable autoscroll
			_isAutoScroll = false;

			axisComponent.setCursor(null);

			return false;
		}

		// ensure that mouse is moved from the graph
		if (_isMouseMovedFromGraph == false) {
			return false;
		}

		if (_isXSliderVisible && _xSliderDragged != null) {

			// x-slider is dragged, do autoscroll the graph with the mouse

			// set dragged x-slider position
			final int devXMouse = mouseEvent.x;

			if (axisComponent == _chartComponents.getAxisLeft()) {

				// left x-axis

				_devXDraggedXSliderLine = -clientArea.width + devXMouse;

				cursor = //
				_devXDraggedXSliderLine < _leftAccelerator[0][0] ? _cursorMove5x : //
						_devXDraggedXSliderLine < _leftAccelerator[1][0] ? _cursorMove4x : //
								_devXDraggedXSliderLine < _leftAccelerator[2][0] ? _cursorMove3x : //
										_devXDraggedXSliderLine < _leftAccelerator[3][0] ? _cursorMove2x : //
												_cursorMove1x;

			} else {

				// right x-axis

				_devXDraggedXSliderLine = getDevVisibleChartWidth() + devXMouse;

				cursor = //
				devXMouse < _rightAccelerator[0][0] ? _cursorMove1x : //
						devXMouse < _rightAccelerator[1][0] ? _cursorMove2x : //
								devXMouse < _rightAccelerator[2][0] ? _cursorMove3x : //
										devXMouse < _rightAccelerator[3][0] ? _cursorMove4x : //
												_cursorMove5x;
			}

		} else {

			// do autoscroll the graph with the moved mouse

			// set mouse position and do autoscrolling

			final int devXMouse = mouseEvent.x;

			if (axisComponent == _chartComponents.getAxisLeft()) {

				// left x-axis

				_devXAutoScrollMousePosition = -clientArea.width + devXMouse;

				cursor = //
				_devXAutoScrollMousePosition < _leftAccelerator[0][0] ? _cursorMove5x : //
						_devXAutoScrollMousePosition < _leftAccelerator[1][0] ? _cursorMove4x : //
								_devXAutoScrollMousePosition < _leftAccelerator[2][0] ? _cursorMove3x : //
										_devXAutoScrollMousePosition < _leftAccelerator[3][0] ? _cursorMove2x : //
												_cursorMove1x;

			} else {

				// right x-axis

				_devXAutoScrollMousePosition = getDevVisibleChartWidth() + devXMouse;

				cursor = //
				devXMouse < _rightAccelerator[0][0] ? _cursorMove1x : //
						devXMouse < _rightAccelerator[1][0] ? _cursorMove2x : //
								devXMouse < _rightAccelerator[2][0] ? _cursorMove3x : //
										devXMouse < _rightAccelerator[3][0] ? _cursorMove4x : //
												_cursorMove5x;
			}
		}

		axisComponent.setCursor(cursor);

		if (_isAutoScroll == false) {
			doAutoScroll();
		}

		return true;
	}

	/**
	 * Mouse up event handler
	 * 
	 * @param event
	 */
	private void onMouseUp(final MouseEvent event) {

		final int devXMouse = event.x;
		final int devYMouse = event.y;

		if (_isAutoScroll) {

			// stop auto scolling
			_isAutoScroll = false;

			/*
			 * make sure that the sliders are at the border of the visible area are at the border
			 */
			if (_devXDraggedXSliderLine < 0) {
				moveXSlider(_xSliderDragged, 0);
			} else {
				final int devVisibleChartWidth = getDevVisibleChartWidth();
				if (_devXDraggedXSliderLine > devVisibleChartWidth - 1) {
					moveXSlider(_xSliderDragged, devVisibleChartWidth - 1);
				}
			}

			// disable dragging
			_xSliderDragged = null;

			// redraw slider
			_isSliderDirty = true;
			redraw();

		} else if (_chart.isMouseUpExternal(devXMouse, devYMouse)) {

			return;

		} else if (_ySliderDragged != null) {

			// y-slider is dragged, stop dragging

			adjustYSlider();

		} else if (_isXMarkerMoved) {

			_isXMarkerMoved = false;

			_isSliderDirty = true;
			redraw();

			// call the listener which is registered for dragged x-marker
			if (_chart._draggingListenerXMarker != null) {
				_chart._draggingListenerXMarker.xMarkerMoved(_movedXMarkerStartValueIndex, _movedXMarkerEndValueIndex);
			}

		} else if (_isChartDragged || _isChartDraggedStarted) {

			// chart was moved with the mouse

			_isChartDragged = false;
			_isChartDraggedStarted = false;

			updateDraggedChart(_draggedChartDraggedPos.x - _draggedChartStartPos.x);
		}

		setCursorStyle(devYMouse);
	}

	void onMouseWheel(final Event event) {

		if (_isGraphVisible == false) {
			return;
		}

		if (_chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER)) {

			// mouse mode: move slider

			/**
			 * when a slider in a graph is moved with the mouse wheel the direction is the same as
			 * when the mouse wheel is scrolling in the tour editor:
			 * <p>
			 * wheel up -> tour editor up
			 */
			if (event.count < 0) {
				event.keyCode |= SWT.ARROW_RIGHT;
			} else {
				event.keyCode |= SWT.ARROW_LEFT;
			}

			/*
			 * set focus when the mouse is over the chart and the mousewheel is scrolled, this will
			 * also activate the part with the chart component
			 */
			if (isFocusControl() == false) {
				forceFocus();
			}

			onKeyDown(event);

			if (_canAutoZoomToSlider) {

				/*
				 * zoom the chart
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {

						zoomInWithSlider();
						_chartComponents.onResize();

//						if (event.count < 0) {}
					}
				});
			}

		} else {

			// mouse mode: zoom chart

			final boolean isCtrl = (event.stateMask & SWT.CONTROL) != 0;
			final boolean isShift = (event.stateMask & SWT.SHIFT) != 0;

//			final boolean isShift = (event.stateMask & SWT.SHIFT) != 0 || (event.keyCode & SWT.SHIFT) != 0;
//			final boolean isCtrl = (event.stateMask & SWT.CTRL) != 0 || (event.keyCode & SWT.CTRL) != 0;

			if (isCtrl || isShift) {

				// scroll the chart

				int devXDiff = 0;
				if (event.count < 0) {
					devXDiff = -10;
				} else {
					devXDiff = 10;
				}

				if (isShift) {
					devXDiff *= 10;
				}

				updateDraggedChart(devXDiff);

			} else {

				// zoom the chart

				if (event.count < 0) {
					zoomOutWithMouse(true, _devXMouseMove);
				} else {
					zoomInWithMouse(_devXMouseMove);
				}

				moveSlidersToBorder();
			}
		}

		/*
		 * prevent scrolling the scrollbar, scrolling is done by the chart itself
		 */
		event.doit = false;
	}

	/**
	 * Scroll event handler
	 * 
	 * @param event
	 */
	private void onScroll(final SelectionEvent event) {
		redraw();
	}

	/**
	 * make the graph dirty and redraw it
	 * 
	 * @param isGraphDirty
	 */
	void redrawBarSelection() {

		if (isDisposed()) {
			return;
		}

		_isSelectionDirty = true;
		redraw();
	}

	void redrawChart() {

		if (isDisposed()) {
			return;
		}

		_isChartDirty = true;
		redraw();
	}

	/**
	 * set the slider position when the data model has changed
	 */
	void resetSliders() {

		// first get the left/right slider
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		/*
		 * reset the sliders, the temp sliders are used so that the same slider is not reset twice
		 */
		leftSlider.reset();
		rightSlider.reset();
	}

	/**
	 * select the next bar item
	 */
	int selectBarItemNext() {

		int selectedIndex = Chart.NO_BAR_SELECTION;

		if (_selectedBarItems == null || _selectedBarItems.length == 0) {
			return selectedIndex;
		}

		// find selected Index and reset last selected bar item(s)
		for (int index = 0; index < _selectedBarItems.length; index++) {
			if (selectedIndex == Chart.NO_BAR_SELECTION && _selectedBarItems[index]) {
				selectedIndex = index;
			}
			_selectedBarItems[index] = false;
		}

		if (selectedIndex == Chart.NO_BAR_SELECTION) {

			// a bar item is not selected, select first
			selectedIndex = 0;

		} else {

			// select next bar item

			if (selectedIndex == _selectedBarItems.length - 1) {
				/*
				 * last bar item is currently selected, select the first bar item
				 */
				selectedIndex = 0;
			} else {
				// select next bar item
				selectedIndex++;
			}
		}

		_selectedBarItems[selectedIndex] = true;

		redrawBarSelection();

		return selectedIndex;
	}

	/**
	 * select the previous bar item
	 */
	int selectBarItemPrevious() {

		int selectedIndex = Chart.NO_BAR_SELECTION;

		// make sure that selectable bar items are available
		if (_selectedBarItems == null || _selectedBarItems.length == 0) {
			return selectedIndex;
		}

		// find selected item, reset last selected bar item(s)
		for (int index = 0; index < _selectedBarItems.length; index++) {
			// get the first selected item if there are many selected
			if (selectedIndex == -1 && _selectedBarItems[index]) {
				selectedIndex = index;
			}
			_selectedBarItems[index] = false;
		}

		if (selectedIndex == Chart.NO_BAR_SELECTION) {

			// a bar item is not selected, select first
			selectedIndex = 0;

		} else {

			// select next bar item

			if (selectedIndex == 0) {
				/*
				 * first bar item is currently selected, select the last bar item
				 */
				selectedIndex = _selectedBarItems.length - 1;
			} else {
				// select previous bar item
				selectedIndex = selectedIndex - 1;
			}
		}

		_selectedBarItems[selectedIndex] = true;

		redrawBarSelection();

		return selectedIndex;
	}

	void setCanAutoMoveSlidersWhenZoomed(final boolean canMoveSlidersWhenZoomed) {
		_canAutoMoveSliders = canMoveSlidersWhenZoomed;
	}

	/**
	 * @param canAutoZoomToSlider
	 *            the canAutoZoomToSlider to set
	 */
	void setCanAutoZoomToSlider(final boolean canAutoZoomToSlider) {

		_canAutoZoomToSlider = canAutoZoomToSlider;
	}

	/**
	 * Move a zoomed chart so that the slider is visible.
	 * 
	 * @param slider
	 * @param isCenterSliderPosition
	 */
	private void setChartPosition(final ChartXSlider slider, final boolean isCenterSliderPosition) {

		if (_graphZoomRatio == 1) {
			// chart is not zoomed, nothing to do
			return;
		}

		final int xxDevSliderLinePos = slider.getXXDevSliderLinePos();

		final int devXViewPortWidth = getDevVisibleChartWidth();
		double xxDevOffset = xxDevSliderLinePos;

		if (isCenterSliderPosition) {

			xxDevOffset = xxDevSliderLinePos - devXViewPortWidth / 2;

		} else {

			/*
			 * check if the slider is in the visible area
			 */
			if (xxDevSliderLinePos < _xxDevViewPortLeftBorder) {

				xxDevOffset = xxDevSliderLinePos + 1;

			} else if (xxDevSliderLinePos > _xxDevViewPortLeftBorder + devXViewPortWidth) {

				xxDevOffset = xxDevSliderLinePos - devXViewPortWidth;
			}
		}

		if (xxDevOffset != xxDevSliderLinePos) {

			/*
			 * slider is not visible
			 */

			// check left border
			xxDevOffset = Math.max(xxDevOffset, 0);

			// check right border
			xxDevOffset = Math.min(xxDevOffset, _xxDevGraphWidth - devXViewPortWidth);

			_zoomRatioLeftBorder = xxDevOffset / _xxDevGraphWidth;

			/*
			 * reposition the mouse zoom position
			 */
			final float xOffsetMouse = _xxDevViewPortLeftBorder + devXViewPortWidth / 2;
			_zoomRatioCenter = xOffsetMouse / _xxDevGraphWidth;

			updateVisibleMinMaxValues();

			/*
			 * prevent to display the old chart image
			 */
			_isChartDirty = true;

			_chartComponents.onResize();
		}

		/*
		 * set position where the double click occured, this position will be used when the chart is
		 * zoomed
		 */
		_zoomRatioCenter = (double) xxDevSliderLinePos / _xxDevGraphWidth;
	}

//	/**
//	 * Set the scrolling cursor according to the vertical position of the mouse
//	 *
//	 * @param devX
//	 * @param devY
//	 *            vertical coordinat of the mouse in the graph
//	 */
//	private void setupScrollCursor(final int devX, final int devY) {
//
//		final int height = getDevVisibleGraphHeight();
//		final int height4 = height / 4;
//		final int height2 = height / 2;
//
//		final float oldValue = _scrollAcceleration;
//
//		_scrollAcceleration = devY < height4 ? 0.25f : devY < height2 ? 1 : devY > height - height4 ? 10 : 2;
//
//		// set cursor according to the position
//		if (_scrollAcceleration == 0.25) {
//			setCursor(_cursorHand05x);
//		} else if (_scrollAcceleration == 1) {
//			setCursor(_cursorHand);
//		} else if (_scrollAcceleration == 2) {
//			setCursor(_cursorHand2x);
//		} else {
//			setCursor(_cursorHand5x);
//		}
//
//		/*
//		 * when the acceleration has changed, the start positions for scrolling the graph must be
//		 * set to the current location
//		 */
//		if (oldValue != _scrollAcceleration) {
//			_startPosScrollbar = getHorizontalBar().getSelection();
//			_startPosDev = devX;
//		}
//	}

	/**
	 * Move a zoomed chart to a new position
	 * 
	 * @param xxDevNewPosition
	 */
	private void setChartPosition(final int xxDevNewPosition) {

		if (_graphZoomRatio == 1) {
			// chart is not zoomed, nothing to do
			return;
		}

		final int devXViewPortWidth = getDevVisibleChartWidth();
		double xxDevNewPosition2 = xxDevNewPosition;

		if (xxDevNewPosition < _xxDevViewPortLeftBorder) {

			xxDevNewPosition2 = xxDevNewPosition + 1;

		} else if (xxDevNewPosition > _xxDevViewPortLeftBorder + devXViewPortWidth) {

			xxDevNewPosition2 = xxDevNewPosition - devXViewPortWidth;
		}

		// check left border
		xxDevNewPosition2 = Math.max(xxDevNewPosition2, 0);

		// check right border
		xxDevNewPosition2 = Math.min(xxDevNewPosition2, _xxDevGraphWidth - devXViewPortWidth);

		_zoomRatioLeftBorder = xxDevNewPosition2 / _xxDevGraphWidth;

		// reposition the mouse zoom position
		final float xOffsetMouse = _xxDevViewPortLeftBorder + devXViewPortWidth / 2;
		_zoomRatioCenter = xOffsetMouse / _xxDevGraphWidth;

		updateVisibleMinMaxValues();

		/*
		 * prevent to display the old chart image
		 */
		_isChartDirty = true;

		_chartComponents.onResize();

		/*
		 * set position where the double click occured, this position will be used when the chart is
		 * zoomed
		 */
		_zoomRatioCenter = (double) xxDevNewPosition / _xxDevGraphWidth;
	}

	void setCursorStyle(final int devYMouse) {

		final ChartDataModel chartDataModel = _chart.getChartDataModel();
		if (chartDataModel == null) {
			return;
		}

		final int chartType = chartDataModel.getChartType();

		if (chartType == ChartDataModel.CHART_TYPE_LINE || chartType == ChartDataModel.CHART_TYPE_LINE_WITH_BARS) {

			final boolean isMouseModeSlider = _chart.getMouseMode().equals(Chart.MOUSE_MODE_SLIDER);

			if (_xSliderDragged != null) {

				// x-slider is dragged
				if (isMouseModeSlider) {
					setCursor(_cursorDragXSlider_ModeSlider);
				} else {
					setCursor(_cursorDragXSlider_ModeZoom);
				}

			} else if (_ySliderDragged != null) {

				// y-slider is dragged

				setCursor(_cursorResizeTopDown);

			} else if (_isChartDragged || _isChartDraggedStarted) {

				// chart is dragged
				setCursor(_cursorDragged);

			} else if (_isXSliderVisible && isInXSliderSetArea(devYMouse)) {

				// cursor is already set

			} else {

				// nothing is dragged

				if (isMouseModeSlider) {
					setCursor(_cursorModeSlider);
				} else {
					setCursor(_cursorModeZoom);
				}
			}
		} else {
			setCursor(null);
		}
	}

	/**
	 * Set a new configuration for the graph, the whole graph will be recreated. This method is
	 * called when the chart canvas is resized, chart is zoomed or scrolled which requires that the
	 * chart is recreated.
	 */
	void setDrawingData(final ChartDrawingData chartDrawingData) {

		_chartDrawingData = chartDrawingData;

		// create empty list if list is not available, so we do not need
		// to check for null and isEmpty
		_graphDrawingData = chartDrawingData.graphDrawingData;

		_isGraphVisible = _graphDrawingData != null && _graphDrawingData.isEmpty() == false;

		// force all graphics to be recreated
		_isChartDirty = true;
		_isSliderDirty = true;
		_isCustomLayerImageDirty = true;
		_isSelectionDirty = true;

		if (_isDisableHoveredLineValueIndex) {
			/*
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 * prevent setting new positions until the chart is redrawn otherwise the slider has the
			 * value index -1, the chart is flickering when autoscrolling and the map is WRONGLY /
			 * UGLY positioned
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 */
			_isDisableHoveredLineValueIndex = false;
		} else {

			// prevent using old value index which can cause bound exceptions
			_hoveredLineValueIndex = -1;
			_lineDevPositions.clear();
			_lineFocusRectangles.clear();
		}

		// hide previous tooltip
		_hoveredBarToolTip.toolTip20Hide();

		// force the graph to be repainted
		redraw();
	}

	@Override
	public boolean setFocus() {

		boolean isFocus = false;

		if (setFocusToControl()) {

			// check if the chart has the focus
			if (isFocusControl()) {
				isFocus = true;
			} else {
				if (forceFocus()) {
					isFocus = true;
				}
			}
		}

		return isFocus;
	}

	/**
	 * Set the focus to a control depending on the chart type
	 * 
	 * @return Returns <code>true</code> when the focus was set
	 */
	private boolean setFocusToControl() {

		if (_isGraphVisible == false) {
			// we can't get the focus
			return false;
		}

		boolean isFocus = false;

		switch (_chart.getChartDataModel().getChartType()) {
		case ChartDataModel.CHART_TYPE_LINE:

			if (_selectedXSlider == null) {
				// set focus to the left slider when x-sliders are visible
				if (_isXSliderVisible) {
					_selectedXSlider = getLeftSlider();
					isFocus = true;
				}
			} else if (_selectedXSlider != null) {
				isFocus = true;
			}

			break;

		case ChartDataModel.CHART_TYPE_BAR:

			if (_selectedBarItems == null || _selectedBarItems.length == 0) {

				setSelectedBars(null);

			} else {

				// set focus to selected x-data

				int selectedIndex = -1;

				// find selected Index, reset last selected bar item(s)
				for (int index = 0; index < _selectedBarItems.length; index++) {
					if (selectedIndex == -1 && _selectedBarItems[index]) {
						selectedIndex = index;
					}
					_selectedBarItems[index] = false;
				}

				if (selectedIndex == -1) {

					// a bar item is not selected, select first

// disabled, 11.4.2008 wolfgang
//					fSelectedBarItems[0] = true;
//
//					fChart.fireBarSelectionEvent(0, 0);

				} else {

					// select last selected bar item

					_selectedBarItems[selectedIndex] = true;
				}

				redrawBarSelection();
			}

			isFocus = true;

			break;
		}

		if (isFocus) {
			_chart.fireFocusEvent();
		}

		ChartManager.getInstance().setActiveChart(isFocus ? _chart : null);

		return isFocus;
	}

	void setGraphSize(final int xxDevGraphWidth, final int xxDevViewPortOffset, final float graphZoomRatio) {

		_xxDevGraphWidth = xxDevGraphWidth;
		_xxDevViewPortLeftBorder = xxDevViewPortOffset;
		_graphZoomRatio = graphZoomRatio;

		_xSliderA.moveToXXDevPosition(xxDevViewPortOffset, false, true);
		_xSliderB.moveToXXDevPosition(xxDevGraphWidth, false, true);
	}

	void setSelectedBars(final boolean[] selectedItems) {

		if (selectedItems == null) {

			// set focus to first bar item

			if (_graphDrawingData.size() == 0) {
				_selectedBarItems = null;
			} else {

				final GraphDrawingData graphDrawingData = _graphDrawingData.get(0);
				final ChartDataXSerie xData = graphDrawingData.getXData();

				_selectedBarItems = new boolean[xData._highValues[0].length];
			}

		} else {

			_selectedBarItems = selectedItems;
		}

		_isSelectionVisible = true;

		redrawBarSelection();
	}

	/**
	 * Set value index for a slider and move the slider to this position, the slider will be made
	 * visible.
	 * 
	 * @param slider
	 * @param valueIndex
	 * @param isCenterSliderPosition
	 * @param isBorderOffset
	 */
	void setXSliderValueIndex(final ChartXSlider slider, int valueIndex, final boolean isCenterSliderPosition) {

		final ChartDataXSerie xData = getXData();

		if (xData == null) {
			return;
		}

		final float[] xValues = xData.getHighValues()[0];

		// adjust the slider index to the array bounds
		valueIndex = valueIndex < 0 ? //
				0
				: valueIndex > (xValues.length - 1) ? //
						xValues.length - 1
						: valueIndex;

		final float xValue = xValues[valueIndex];
		final double xxDevLinePos = (double) _xxDevGraphWidth * xValue / xValues[xValues.length - 1];

		slider.setValuesIndex(valueIndex);
		slider.setValueX(xValue);

		slider.moveToXXDevPosition((int) xxDevLinePos, true, true);

		setChartPosition(slider, isCenterSliderPosition);

		_isSliderDirty = true;
	}

	/**
	 * makes the slider visible, a slider is only drawn into the chart if a slider was created with
	 * createSlider
	 * 
	 * @param isXSliderVisible
	 */
	void setXSliderVisible(final boolean isSliderVisible) {
		_isXSliderVisible = isSliderVisible;
	}

	private void setZoomInPosition() {

		// get left+right slider
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		final int devLeftVirtualSliderLinePos = leftSlider.getXXDevSliderLinePos();

		final int devZoomInPosInChart = devLeftVirtualSliderLinePos
				+ ((rightSlider.getXXDevSliderLinePos() - devLeftVirtualSliderLinePos) / 2);

		_zoomRatioCenter = (double) devZoomInPosInChart / _xxDevGraphWidth;
	}

	/**
	 * Set left border ratio for the zoomed chart, zoomed graph width and ratio is already set.
	 */
	private void setZoomRatioLeftBorder() {

		final int devViewPortWidth = getDevVisibleChartWidth();

		double xxDevPosition = _zoomRatioCenter * _xxDevGraphWidth;
		xxDevPosition -= devViewPortWidth / 2;

		// ensure left border bounds
		xxDevPosition = Math.max(xxDevPosition, 0);

		// ensure right border by setting the left border value
		final int leftBorder = _xxDevGraphWidth - devViewPortWidth;
		xxDevPosition = Math.min(xxDevPosition, leftBorder);

		_zoomRatioLeftBorder = xxDevPosition / _xxDevGraphWidth;
	}

	/**
	 * switch the sliders to the 2nd x-data (switch between time and distance)
	 */
	void switchSlidersTo2ndXData() {
		switchSliderTo2ndXData(_xSliderA);
		switchSliderTo2ndXData(_xSliderB);
	}

	/**
	 * set the slider to the 2nd x-data and keep the slider on the same xValue position as before,
	 * this can cause to the situation, that the right slider gets unvisible/unhitable or the
	 * painted graph can have a white space on the right side
	 * 
	 * @param slider
	 *            the slider which gets changed
	 */
	private void switchSliderTo2ndXData(final ChartXSlider slider) {

		if (_graphDrawingData.size() == 0) {
			return;
		}

		final GraphDrawingData graphDrawingData = _graphDrawingData.get(0);
		if (graphDrawingData == null) {
			return;
		}

		final ChartDataXSerie data2nd = graphDrawingData.getXData2nd();

		if (data2nd == null) {
			return;
		}

		final float[] xValues = data2nd.getHighValues()[0];
		int valueIndex = slider.getValuesIndex();

		if (valueIndex >= xValues.length) {
			valueIndex = xValues.length - 1;
			slider.setValuesIndex(valueIndex);
		}

		try {
			slider.setValueX(xValues[valueIndex]);

			final int linePos = (int) (_xxDevGraphWidth * (xValues[valueIndex] / xValues[xValues.length - 1]));

			slider.moveToXXDevPosition(linePos, true, true);

		} catch (final ArrayIndexOutOfBoundsException e) {
			// ignore
		}
	}

	void updateChartSize() {

		if (valuePointToolTip == null) {
			return;
		}

		final int marginTop = _chartComponents.getDevChartMarginTop();
		final int marginBottom = _chartComponents.getDevChartMarginBottom();

		valuePointToolTip.setChartMargins(marginTop, marginBottom);
	}

	void updateCustomLayers() {

		if (isDisposed()) {
			return;
		}

		_isCustomLayerImageDirty = true;
		_isSliderDirty = true;

		redraw();
	}

	private void updateDraggedChart(final int devXDiff) {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		double devXOffset = _xxDevViewPortLeftBorder - devXDiff;

		// adjust left border
		devXOffset = Math.max(devXOffset, 0);

		// adjust right border
		devXOffset = Math.min(devXOffset, _xxDevGraphWidth - devVisibleChartWidth);

		_zoomRatioLeftBorder = devXOffset / _xxDevGraphWidth;

		/*
		 * reposition the mouse zoom position
		 */
		_zoomRatioCenter = ((_zoomRatioCenter * _xxDevGraphWidth) - devXDiff) / _xxDevGraphWidth;

		updateVisibleMinMaxValues();

		/*
		 * draw the dragged image until the graph image is recomuted
		 */
		_isPaintDraggedImage = true;

		_chartComponents.onResize();

		moveSlidersToBorder();
	}

	void updateGraphSize() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		// calculate new virtual graph width
		_xxDevGraphWidth = (int) (_graphZoomRatio * devVisibleChartWidth);

		if (_graphZoomRatio == 1.0) {
			// with the ration 1.0 the graph is not zoomed
			_xxDevViewPortLeftBorder = 0;
		} else {
			// the graph is zoomed, only a part is displayed which starts at
			// the offset for the left slider
			_xxDevViewPortLeftBorder = (int) (_zoomRatioLeftBorder * _xxDevGraphWidth);
		}
	}

	/**
	 * Resize the sliders after the graph was resized
	 */
	void updateSlidersOnResize() {

		/*
		 * update all x-sliders
		 */
		final int visibleGraphHeight = getDevVisibleGraphHeight();
		_xSliderA.handleChartResize(visibleGraphHeight);
		_xSliderB.handleChartResize(visibleGraphHeight);

		/*
		 * update all y-sliders
		 */
		_ySliders = new ArrayList<ChartYSlider>();

		// loop: get all y-sliders from all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ChartDataYSerie yData = drawingData.getYData();

			if (yData.isShowYSlider()) {

				final ChartYSlider sliderTop = yData.getYSlider1();
				final ChartYSlider sliderBottom = yData.getYSlider2();

				sliderTop.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_TOP);
				_ySliders.add(sliderTop);

				sliderBottom.handleChartResize(drawingData, ChartYSlider.SLIDER_TYPE_BOTTOM);
				_ySliders.add(sliderBottom);

				_isYSliderVisible = true;
			}
		}
	}

	/**
	 * sets the min/max values for the y-axis that the visible area will be filled with the chart
	 */
	void updateVisibleMinMaxValues() {

		final ChartDataModel chartDataModel = _chartComponents.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();
		final ArrayList<ChartDataYSerie> yDataList = chartDataModel.getYData();

		if (xData == null) {
			return;
		}

		final float[][] xValueSerie = xData.getHighValues();

		if (xValueSerie.length == 0) {
			// data are not available
			return;
		}

		final float[] xValues = xValueSerie[0];
		final float lastXValue = xValues[xValues.length - 1];
		final double valueVisibleArea = lastXValue / _graphZoomRatio;

		final double valueLeftBorder = lastXValue * _zoomRatioLeftBorder;
		double valueRightBorder = valueLeftBorder + valueVisibleArea;

		// make sure right is higher than left
		if (valueLeftBorder >= valueRightBorder) {
			valueRightBorder = valueLeftBorder + 1;
		}

		/*
		 * get value index for the left and right border of the visible area
		 */
		int xValueIndexLeft = 0;
		for (int serieIndex = 0; serieIndex < xValues.length; serieIndex++) {
			final float xValue = xValues[serieIndex];
			if (xValue == valueLeftBorder) {
				xValueIndexLeft = serieIndex;
				break;
			}
			if (xValue > valueLeftBorder) {
				xValueIndexLeft = serieIndex == 0 ? //
						0
						// get index from last invisible value
						: serieIndex - 1;
				break;
			}
		}

		int xValueIndexRight = xValueIndexLeft;
		for (; xValueIndexRight < xValues.length; xValueIndexRight++) {
			if (xValues[xValueIndexRight] > valueRightBorder) {
				break;
			}
		}

		/*
		 * get visible min/max value for the x-data serie which fills the visible area in the chart
		 */
		// ensure array bounds
		final int xValuesLastIndex = xValues.length - 1;
		xValueIndexLeft = Math.min(xValueIndexLeft, xValuesLastIndex);
		xValueIndexLeft = Math.max(xValueIndexLeft, 0);
		xValueIndexRight = Math.min(xValueIndexRight, xValuesLastIndex);
		xValueIndexRight = Math.max(xValueIndexRight, 0);

		xData.setVisibleMinValue(xValues[xValueIndexLeft]);
		xData.setVisibleMaxValue(xValues[xValueIndexRight]);

		/*
		 * get min/max value for each y-data serie to fill the visible area with the chart
		 */
		for (final ChartDataYSerie yData : yDataList) {

			final float[][] yValueSeries = yData.getHighValues();
			final float yValues[] = yValueSeries[0];

			// ensure array bounds
			final int yValuesLastIndex = yValues.length - 1;
			xValueIndexLeft = Math.min(xValueIndexLeft, yValuesLastIndex);
			xValueIndexLeft = Math.max(xValueIndexLeft, 0);
			xValueIndexRight = Math.min(xValueIndexRight, yValuesLastIndex);
			xValueIndexRight = Math.max(xValueIndexRight, 0);

			float minValue = yValues[xValueIndexLeft];
			float maxValue = yValues[xValueIndexLeft];

			for (final float[] yValueSerie : yValueSeries) {

				if (yValueSerie == null) {
					continue;
				}

				for (int valueIndex = xValueIndexLeft; valueIndex <= xValueIndexRight; valueIndex++) {

					final float yValue = yValueSerie[valueIndex];

					if (yValue < minValue) {
						minValue = yValue;
					}
					if (yValue > maxValue) {
						maxValue = yValue;
					}
				}
			}

			if (yData.isForceMinValue() == false && minValue != 0) {
				yData.setVisibleMinValue(minValue);
			}

			if (yData.isForceMaxValue() == false && maxValue != 0) {
				yData.setVisibleMaxValue(maxValue);
			}
		}
	}

	/**
	 * adjust the y-position for the bottom label when the top label is drawn over it
	 */
	private void updateXSliderYPosition() {

		int labelIndex = 0;
		final ArrayList<ChartXSliderLabel> onTopLabels = _xSliderOnTop.getLabelList();
		final ArrayList<ChartXSliderLabel> onBotLabels = _xSliderOnBottom.getLabelList();

		for (final ChartXSliderLabel onTopLabel : onTopLabels) {

			final ChartXSliderLabel onBotLabel = onBotLabels.get(labelIndex);

			final int onTopWidth2 = onTopLabel.width / 2;
			final int onTopDevX = onTopLabel.x;
			final int onBotWidth2 = onBotLabel.width / 2;
			final int onBotDevX = onBotLabel.x;

			if (onTopDevX + onTopWidth2 > onBotDevX - onBotWidth2 && onTopDevX - onTopWidth2 < onBotDevX + onBotWidth2) {
				onBotLabel.y = onBotLabel.y + onBotLabel.height + 5;
			}
			labelIndex++;
		}
	}

	/**
	 * @param devXMousePosition
	 *            This relative mouse position is used to keep the position when zoomed in, when set
	 *            to {@link Integer#MIN_VALUE} this value is ignored.
	 */
	void zoomInWithMouse(final int devXMousePosition) {

		if (_xxDevGraphWidth <= ChartComponents.CHART_MAX_WIDTH) {

			// chart can be zoomed in

			final int devViewPortWidth = getDevVisibleChartWidth();
			final double devViewPortWidth2 = (double) devViewPortWidth / 2;

			final double newZoomRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
			final int xxDevNewGraphWidth = (int) (devViewPortWidth * newZoomRatio);

			if (_xSliderDragged != null) {

				// set zoom center so that the dragged x-slider keeps position when zoomed in

				final double xxDevSlider = _xxDevViewPortLeftBorder + _devXDraggedXSliderLine;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - _devXDraggedXSliderLine + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;

			} else if (devXMousePosition != Integer.MIN_VALUE) {

				final double xxDevSlider = _xxDevViewPortLeftBorder + devXMousePosition;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - devXMousePosition + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;
			}

			if (xxDevNewGraphWidth > ChartComponents.CHART_MAX_WIDTH) {

				// ensure max size
				_graphZoomRatio = (double) ChartComponents.CHART_MAX_WIDTH / devViewPortWidth;
				_xxDevGraphWidth = ChartComponents.CHART_MAX_WIDTH;

			} else {

				_graphZoomRatio = newZoomRatio;
				_xxDevGraphWidth = xxDevNewGraphWidth;
			}

			setZoomRatioLeftBorder();
			handleChartResizeForSliders();

			updateVisibleMinMaxValues();
			moveSlidersToBorder();

			_chartComponents.onResize();
		}

		_chart.enableActions();
	}

	/**
	 * Zoom into the graph with the ratio {@link #ZOOM_RATIO_FACTOR}
	 */
	void zoomInWithoutSlider() {

		final int visibleGraphWidth = getDevVisibleChartWidth();
		final int graphImageWidth = _xxDevGraphWidth;

		final int maxChartWidth = ChartComponents.CHART_MAX_WIDTH;

		if (graphImageWidth <= maxChartWidth) {

			// chart is within the range which can be zoomed in

			if (graphImageWidth * ZOOM_RATIO_FACTOR > maxChartWidth) {
				/*
				 * the double zoomed graph would be wider than the max width, reduce it to the max
				 * width
				 */
				_graphZoomRatio = maxChartWidth / visibleGraphWidth;
				_xxDevGraphWidth = maxChartWidth;
			} else {
				_graphZoomRatio = _graphZoomRatio * ZOOM_RATIO_FACTOR;
				_xxDevGraphWidth = (int) (graphImageWidth * ZOOM_RATIO_FACTOR);
			}

			_chart.enableActions();
		}
	}

	/**
	 * Zoom into the graph to the left and right slider
	 */
	void zoomInWithSlider() {

		final int devVisibleChartWidth = getDevVisibleChartWidth();

		/*
		 * offset for the left slider
		 */
		final ChartXSlider leftSlider = getLeftSlider();
		final ChartXSlider rightSlider = getRightSlider();

		// get position of the sliders within the graph
		final int devVirtualLeftSliderPos = leftSlider.getXXDevSliderLinePos();
		final int devVirtualRightSliderPos = rightSlider.getXXDevSliderLinePos();

		// difference between left and right slider
		final float devSliderDiff = devVirtualRightSliderPos - devVirtualLeftSliderPos - 0;

		if (devSliderDiff == 0) {

			// no difference between the slider
			_graphZoomRatio = 1;
			_xxDevGraphWidth = devVisibleChartWidth;

		} else {

			/*
			 * the graph image can't be scrolled, show only the zoomed part which is defined between
			 * the two sliders
			 */

			// calculate new graph ratio
			_graphZoomRatio = (_xxDevGraphWidth) / (devSliderDiff);

			// adjust rounding problems
			_graphZoomRatio = (_graphZoomRatio * devVisibleChartWidth) / devVisibleChartWidth;

			// set the position (ratio) at which the zoomed chart starts
			_zoomRatioLeftBorder = getLeftSlider().getPositionRatio();

			// set the center of the chart for the position when zooming with the mouse
			final double devVirtualWidth = _graphZoomRatio * devVisibleChartWidth;
			final double devXOffset = _zoomRatioLeftBorder * devVirtualWidth;
			final int devCenterPos = (int) (devXOffset + devVisibleChartWidth / 2);
			_zoomRatioCenter = devCenterPos / devVirtualWidth;
		}

		handleChartResizeForSliders();

		updateVisibleMinMaxValues();

		_chart.enableActions();
	}

	/**
	 * Zooms out of the graph
	 */
	void zoomOutFitGraph() {

		// reset the data which influence the computed graph image width
		_graphZoomRatio = 1;
		_zoomRatioLeftBorder = 0;

		_xxDevGraphWidth = getDevVisibleChartWidth();
		_xxDevViewPortLeftBorder = 0;

		// reposition the sliders
		final int visibleGraphHeight = getDevVisibleGraphHeight();
		_xSliderA.handleChartResize(visibleGraphHeight);
		_xSliderB.handleChartResize(visibleGraphHeight);

		_chart.enableActions();

		_chartComponents.onResize();
	}

	/**
	 * Zooms out of the graph
	 * 
	 * @param isUpdateChart
	 * @param devXMousePosition
	 *            This relative mouse position is used to keep the position when zoomed in, when set
	 *            to {@link Integer#MIN_VALUE} this value is ignored.
	 */
	void zoomOutWithMouse(final boolean isUpdateChart, final int devXMousePosition) {

		final int devViewPortWidth = getDevVisibleChartWidth();
		final double devViewPortWidth2 = (double) devViewPortWidth / 2;

		if (_graphZoomRatio > ZOOM_RATIO_FACTOR) {

			// graph is zoomed

			final double newZoomRatio = _graphZoomRatio / ZOOM_RATIO_FACTOR;
			final int xxDevNewGraphWidth = (int) (devViewPortWidth * newZoomRatio);

			if (_xSliderDragged != null) {

				/**
				 * Set zoom center so that the dragged x-slider keeps position when zoomed out.
				 * <p>
				 * This formula preserves the slider ratio for a resized graph but is using the
				 * zoomed center to preserve the x-slider position
				 * <p>
				 * very complicated and it took me some time to get this formula
				 */

				// get slider ratio before the graph width is resized
				final double xxDevSlider = _xxDevViewPortLeftBorder + _devXDraggedXSliderLine;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - _devXDraggedXSliderLine + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;

			} else if (devXMousePosition != Integer.MIN_VALUE) {

				final double xxDevSlider = _xxDevViewPortLeftBorder + devXMousePosition;
				final double sliderRatio = xxDevSlider / _xxDevGraphWidth;
				final double xxDevNewSlider = sliderRatio * xxDevNewGraphWidth;
				final double xxDevNewZoomRatioCenter = xxDevNewSlider - devXMousePosition + devViewPortWidth2;

				_zoomRatioCenter = xxDevNewZoomRatioCenter / xxDevNewGraphWidth;
			}

			_graphZoomRatio = newZoomRatio;
			_xxDevGraphWidth = xxDevNewGraphWidth;

			setZoomRatioLeftBorder();

			handleChartResizeForSliders();
			updateVisibleMinMaxValues();

			if (isUpdateChart) {
				_chartComponents.onResize();
			}

		} else {

			if (_graphZoomRatio != 1) {

				// show whole graph in the chart

				_graphZoomRatio = 1;
				_xxDevGraphWidth = devViewPortWidth;

				setZoomRatioLeftBorder();

				handleChartResizeForSliders();
				updateVisibleMinMaxValues();

				if (isUpdateChart) {
					_chartComponents.onResize();
				}
			}
		}

		moveSlidersToBorder();

		_chart.enableActions();
	}
}
