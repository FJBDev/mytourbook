/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
/**
 * @author Wolfgang Schramm Created: 06.07.2005
 */
package net.tourbook.ui.tourChart;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * This layer displays the altitude values.
 */
public class ChartSegmentAltitudeLayer implements IChartLayer {

	private static final Color		SYSTEM_COLOR_0		= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

	private static final Color		SYSTEM_COLOR_DOWN	= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
	private static final Color		SYSTEM_COLOR_UP		= Display.getCurrent().getSystemColor(SWT.COLOR_RED);
	private TourChart				_tourChart;

	private TourData				_tourData;
	private ArrayList<ChartMarker>	_chartMarkers		= new ArrayList<ChartMarker>();
	private RGB						_lineColor			= new RGB(189, 0, 255);

	private boolean					_isShowSegmenterMarker;
	private boolean					_isShowSegmenterValue;
	private int						_stackedValues;

	private final NumberFormat		_nf1				= NumberFormat.getNumberInstance();

	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	ChartSegmentAltitudeLayer(final TourChart tourChart) {

		_tourChart = tourChart;
	}

	/**
	 * Adds a new marker to the internal marker list, the list can be retrieved with getMarkerList()
	 * 
	 * @param chartMarker
	 * @param xCoord
	 *            Position of the marker on the x axis
	 * @param label
	 */
	public void addMarker(final ChartMarker chartMarker) {
		_chartMarkers.add(chartMarker);
	}

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param graphDrawingData
	 * @param fChartComponents
	 */
	@Override
	public void draw(	final GC gc,
						final GraphDrawingData graphDrawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		final Display display = Display.getCurrent();

		final int graphWidth = graphDrawingData.getChartDrawingData().devVisibleChartWidth;
		final int devYTop = graphDrawingData.getDevYTop();
		final int devYBottom = graphDrawingData.getDevYBottom();
		final long devGraphImageXOffset = chart.getXXDevViewPortLeftBorder();
//		final int devGraphHeight = drawingData.devGraphHeight;

		final float graphYBottom = graphDrawingData.getGraphYBottom();
		final float[] yValues = graphDrawingData.getYData().getHighValuesFloat()[0];
		final double scaleX = graphDrawingData.getScaleX();
		final double scaleY = graphDrawingData.getScaleY();

		final ValueOverlapChecker valueCheckerUp = new ValueOverlapChecker(_stackedValues);
		final ValueOverlapChecker valueCheckerDown = new ValueOverlapChecker(_stackedValues);

		final LineAttributes defaultLineAttributes = gc.getLineAttributes();
		final LineAttributes vertLineLA = new LineAttributes(5);
		vertLineLA.dashOffset = 3;
		vertLineLA.style = SWT.LINE_CUSTOM;
		vertLineLA.dash = new float[] { 1f, 2f };
		vertLineLA.width = 1f;

		// setup font
		final Font fontBackup = gc.getFont();
		gc.setFont(_tourChart.getValueFont());

		// do not draw over the graph area
		gc.setClipping(0, devYTop, graphWidth, devYBottom - devYTop);

		gc.setAntialias(chart.graphAntialiasing);
		gc.setTextAntialias(chart.graphAntialiasing);

		final ChartMarker[] chartMarkers = _chartMarkers.toArray(new ChartMarker[_chartMarkers.size()]);

		final Color colorLine = new Color(display, _lineColor);
		{
			Point previousPoint = null;

			final float[] segmentSerieAltitudeDiff = _tourData.segmentSerieAltitudeDiff;
			final float[] segmentSerieComputedAltitudeDiff = _tourData.segmentSerieComputedAltitudeDiff;

			for (int segmentIndex = 0; segmentIndex < chartMarkers.length; segmentIndex++) {

				// get current value
				final ChartMarker chartMarker = chartMarkers[segmentIndex];
				final int devXValue = (int) (chartMarker.graphX * scaleX - devGraphImageXOffset);

				// optimize performance
				if (devXValue < 0 || devXValue > graphWidth) {

					// get next value
					if (segmentIndex < chartMarkers.length - 2) {

						final ChartMarker chartMarkerNext = chartMarkers[segmentIndex + 1];
						final int devXValueNext = (int) (chartMarkerNext.graphX * scaleX - devGraphImageXOffset);

						if (devXValueNext < 0) {
							// current and next value are outside of the visible area
							continue;
						}
					}

					// get previous value
					if (segmentIndex > 0) {

						final ChartMarker chartMarkerPrev = chartMarkers[segmentIndex - 1];
						final int devXValuePrev = (int) (chartMarkerPrev.graphX * scaleX - devGraphImageXOffset);

						if (devXValuePrev > graphWidth) {
							// current and previous value are outside of the visible area
							break;
						}
					}
				}

				final int yValueIndex = Math.min(yValues.length - 1, chartMarker.serieIndex);
				final float yValue = yValues[yValueIndex];

				final int devYGraph = (int) ((yValue - graphYBottom) * scaleY);
				final int devYSegment = devYBottom - devYGraph;

				float altiDiff = 0;
				if (segmentIndex > 0) {
					if (segmentSerieComputedAltitudeDiff != null) {
						altiDiff = segmentSerieComputedAltitudeDiff[segmentIndex];
					} else {
						altiDiff = segmentSerieAltitudeDiff[segmentIndex];
					}
				}
				final boolean isValueUp = altiDiff >= 0;

				final String valueText = _nf1.format(altiDiff);
				final Point textExtent = gc.textExtent(valueText);

				final int textWidth = textExtent.x;
				final int textHeight = textExtent.y;

				final Color altiDiffColor = getColor(altiDiff);

				/*
				 * Connect two segments with a line
				 */
				if (previousPoint == null) {

					previousPoint = new Point(devXValue, devYSegment);

				} else {

					gc.setLineAttributes(defaultLineAttributes);
					gc.setForeground(altiDiffColor);
					gc.drawLine(//
							previousPoint.x,
							previousPoint.y,
							devXValue,
							devYSegment);
				}

				/*
				 * Draw a line from the value marker to the top or the bottom
				 */
				if (_isShowSegmenterMarker) {

					int devYLine;
					if (isValueUp) {
						devYLine = devYSegment - 1 * textHeight;
					} else {
						devYLine = devYSegment + 1 * textHeight;
					}

					gc.setForeground(altiDiffColor);
					gc.setLineAttributes(vertLineLA);
					gc.drawLine(//
							devXValue,
							devYSegment,
							devXValue,
							devYLine);
				}

				if (segmentIndex > 0) {

					if (_isShowSegmenterValue) {

						/*
						 * Draw the diff value
						 */

						final int segmentWidth = devXValue - previousPoint.x;
						final int devXDiffValue = previousPoint.x + segmentWidth / 2 - textWidth / 2;

						int devYValue;

						final int yDiff = (devYSegment - previousPoint.y) / 2;

						if (isValueUp) {
							devYValue = devYSegment - yDiff - 2 * textHeight;
						} else {
							devYValue = devYSegment - yDiff + textHeight + 2;
						}

						/*
						 * Ensure the value text do not overlap, if possible :-)
						 */
						Rectangle textRect = new Rectangle(devXDiffValue, devYValue, textWidth, textHeight);

						boolean isDrawValue = true;

						if (isValueUp) {

							if (valueCheckerUp.intersectsWithValues(textRect)) {
								devYValue = valueCheckerUp.getPreviousValue().y - textHeight;
							}

							if (valueCheckerUp.intersectsNoValues(textRect)) {
								isDrawValue = false;
							}

						} else {

							if (valueCheckerDown.intersectsWithValues(textRect)) {
								devYValue = valueCheckerDown.getPreviousValue().y + textHeight;
							}

							if (valueCheckerDown.intersectsNoValues(textRect)) {
								isDrawValue = false;
							}
						}

						// don't draw over the graph borders
						if (isDrawValue && devYValue > devYTop && devYValue + textHeight < devYBottom) {

							// keep current up/down rectangle
							final int margin = 0;
							textRect = new Rectangle(//
									devXDiffValue - margin,
									devYValue - margin,
									textWidth + 2 * margin,
									textHeight + 2 * margin);

							if (isValueUp) {
								valueCheckerUp.setupNext(textRect);
							} else {
								valueCheckerDown.setupNext(textRect);
							}

							gc.setForeground(altiDiffColor);
							gc.drawText(//
									valueText,
									devXDiffValue,
									devYValue,
									true);
						}
					}

					previousPoint.x = devXValue;
					previousPoint.y = devYSegment;
				}
			}
		}
		colorLine.dispose();

		// reset clipping
		gc.setClipping((Rectangle) null);

		// restore font
		gc.setFont(fontBackup);
	}

	private Color getColor(final float altiDiff) {

		if (altiDiff > 0) {
			return SYSTEM_COLOR_UP;
		} else if (altiDiff < 0) {
			return SYSTEM_COLOR_DOWN;
		} else {
			return SYSTEM_COLOR_0;
		}
	}

	void setLayerConfig(final TourData tourData,
						final RGB lineColor,
						final boolean isShowSegmenterMarker,
						final boolean isShowSegmenterValue,
						final int checkedVisibleValues) {

		_tourData = tourData;
		_lineColor = lineColor;

		_isShowSegmenterValue = isShowSegmenterValue;
		_isShowSegmenterMarker = isShowSegmenterMarker;

		_stackedValues = checkedVisibleValues;
	}

}
