/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.SplineData;
import net.tourbook.data.TourData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayer2ndAltiSerie implements IChartLayer {

	/**
	 * contains tour which is displayed in the chart
	 */
	private TourData				_tourData;
	private int[]					_xDataSerie;
	private TourChartConfiguration	_tourChartConfig;
	private SplineData				_splineData;

	private Rectangle[]				_spPointRects;

	private int						_graphXValueOffset;
	private int						_devGraphValueXOffset;
	private int						_devY0Spline;
	private float					_scaleX;
	private float					_scaleY;

	public ChartLayer2ndAltiSerie(	final TourData tourData,
									final int[] xDataSerie,
									final TourChartConfiguration tourChartConfig,
									final SplineData splineData) {

		_tourData = tourData;
		_tourChartConfig = tourChartConfig;
		_splineData = splineData;

		// x-data serie contains the time or distance distance data serie
		_xDataSerie = xDataSerie;
	}

	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final int[] xValues = _xDataSerie;

		final int[] yValues2ndSerie = _tourData.dataSerie2ndAlti;
		final int[] yDiffTo2ndSerie = _tourData.dataSerieDiffTo2ndAlti;
		final int[] yAdjustedSerie = _tourData.dataSerieAdjustedAlti;

		final boolean is2ndYValues = yValues2ndSerie != null;
		final boolean isDiffValues = yDiffTo2ndSerie != null;
		final boolean isAdjustedValues = yAdjustedSerie != null;
		final boolean isPointInGraph = _splineData != null && _splineData.serieIndex != null;

		if (xValues == null || xValues.length == 0 /*
													 * || yValues2ndSerie == null ||
													 * yValues2ndSerie.length == 0
													 */) {
			return;
		}

		_scaleX = drawingData.getScaleX();
		_scaleY = drawingData.getScaleY();

		// get the horizontal offset for the graph
		_devGraphValueXOffset = chart.getDevGraphImageXOffset();
		_graphXValueOffset = (int) (Math.max(0, _devGraphValueXOffset) / _scaleX);

		final Display display = Display.getCurrent();

		final Path path2ndSerie = new Path(display);
		final Path pathValueDiff = new Path(display);
		final Path pathAdjustValue = new Path(display);
//
//		final int graphYTop = drawingData.getGraphYTop();
		final int graphYBottom = drawingData.getGraphYBottom();

		final int devGraphHeight = drawingData.devGraphHeight;
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - devGraphHeight;

		// write spline into the middle of the chart
		_devY0Spline = devYBottom - devGraphHeight / 2;

		/*
		 * convert all diff values into positive values
		 */
		int diffValues[] = null;
		float scaleValueDiff = _scaleY;
		if (isDiffValues) {

			int valueIndex = 0;
			int maxValueDiff = 0;

			diffValues = new int[yDiffTo2ndSerie.length];
			for (int valueDiff : yDiffTo2ndSerie) {
				diffValues[valueIndex++] = valueDiff = (valueDiff < 0) ? -valueDiff : valueDiff;
				maxValueDiff = (maxValueDiff >= valueDiff) ? maxValueDiff : valueDiff;
			}

			// set value diff scaling
			if (_tourChartConfig.isRelativeValueDiffScaling) {
				scaleValueDiff = maxValueDiff == 0 ? _scaleY : (float) devGraphHeight / 2 / maxValueDiff;
			}
		}

		// position for the x-axis line in the graph
		final float devY0 = devYBottom + (_scaleY * graphYBottom);

		final int startIndex = 0;
		final int endIndex = xValues.length;

		final int graphClippingWidth = gc.getClipping().width;
		final Rectangle graphRect = new Rectangle(0, devYTop, graphClippingWidth, devGraphHeight);

		// get initial dev X
		int graphXValue = xValues[startIndex] - _graphXValueOffset;
		int devPrevXInt = (int) (graphXValue * _scaleX);

		int graphYValue2nd;
		if (is2ndYValues) {
			graphYValue2nd = yValues2ndSerie[startIndex];
		}

		/*
		 * create paths
		 */
		for (int xValueIndex = startIndex; xValueIndex < endIndex; xValueIndex++) {

			// make sure the x-index is not higher than the yValues length
//			if (xValueIndex >= yValues2ndSerie.length) {
//				return;
//			}

			graphXValue = xValues[xValueIndex] - _graphXValueOffset;
			final float devX = graphXValue * _scaleX;
			final int devXInt = (int) devX;

			/*
			 * draw adjusted value graph
			 */
			if (isAdjustedValues) {

				final float devYAdjustedValue = yAdjustedSerie[xValueIndex] * _scaleY;
				final float devYAdjusted = devY0 - devYAdjustedValue;

				if (xValueIndex == startIndex) {

					// move to the first point
					pathAdjustValue.moveTo(0, devYBottom);
					pathAdjustValue.lineTo(devX, devYAdjusted);
				}

				// draw line to the next point
				if (devXInt != devPrevXInt) {
					pathAdjustValue.lineTo(devX, devYAdjusted);
				}

				if (xValueIndex == endIndex - 1) {

					/*
					 * this is the last point, draw the line to the x-axis and the start of the
					 * chart
					 */
					pathAdjustValue.lineTo(devX, devYBottom);
				}
			}

			/*
			 * draw value graph
			 */
			if (is2ndYValues) {

				graphYValue2nd = yValues2ndSerie[xValueIndex];
				final float devYValue2nd = graphYValue2nd * _scaleY;

				final float devY2nd = devY0 - devYValue2nd;

				if (xValueIndex == startIndex) {

					// move to the first point
					path2ndSerie.moveTo(devX, devY2nd);
				}

				// draw line to the next point
				if (devXInt != devPrevXInt) {
					path2ndSerie.lineTo(devX, devY2nd);
				}
			}

			/*
			 * draw diff values
			 */
			if (isDiffValues) {

				final int graphValueDiff = (diffValues[xValueIndex]);
				final float devLayerValueDiff = graphValueDiff * scaleValueDiff;
				final float devYDiff = devYBottom - devLayerValueDiff;

				if (xValueIndex == startIndex) {

					// move to the first point
					pathValueDiff.moveTo(devX, devYDiff);
				}

				// draw line to the next point
				if (devXInt != devPrevXInt) {
					pathValueDiff.lineTo(devX, devYDiff);
				}
			}

			devPrevXInt = devXInt;
		}

		// draw the line of the graph
		gc.setAntialias(SWT.OFF);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setClipping(graphRect);

		/*
		 * paint and fill adjusted value graph
		 */
		if (isAdjustedValues) {

			final Color color1 = new Color(display, new RGB(0xFF, 0x3E, 0x00));
//			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

			gc.setForeground(color1);
			gc.setBackground(color1);
			gc.setAlpha(0x80);

			// fill background
			gc.setClipping(pathAdjustValue);
			gc.fillGradientRectangle(0, devYTop, graphClippingWidth, devGraphHeight, true);
			gc.setClipping(graphRect);

			// draw graph
			gc.drawPath(pathAdjustValue);

			gc.setAlpha(0xff);
			color1.dispose();
		}

		/*
		 * paint value diff graph
		 */
		if (isDiffValues) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			gc.drawPath(pathValueDiff);
		}

		/*
		 * paint splines
		 */
		final Color splineColor = new Color(display, 0x00, 0xb4, 0xff);
		final float[] ySplineSerie = _tourData.dataSerieSpline;
		if (ySplineSerie != null) {

			gc.setForeground(splineColor);

			int devXPrev = (int) ((xValues[0] - _graphXValueOffset) * _scaleX);
			int devYPrev = (int) (ySplineSerie[0] * _scaleY);

			for (int xIndex = 1; xIndex < xValues.length; xIndex++) {

				final float graphX = xValues[xIndex] - _graphXValueOffset;
				final float graphY = ySplineSerie[xIndex];

				final int devX = (int) (graphX * _scaleX);
				final int devY = (int) (graphY * _scaleY);

				if (!(devX == devXPrev && devY == devYPrev)) {
					gc.drawLine(devXPrev, _devY0Spline - devYPrev, devX, _devY0Spline - devY);
				}

				devXPrev = devX;
				devYPrev = devY;
			}
		}

		/*
		 * paint data graph
		 */
		if (is2ndYValues) {
			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			gc.drawPath(path2ndSerie);
		}

		/*
		 * paint spline points
		 */
		final SplineData splineData = _tourData.splineDataPoints;
		if (splineData != null) {

			final double[] graphXSplineValues = splineData.graphXValues;
			final double[] graphYSplineValues = splineData.graphYValues;
			final boolean[] isPointMovable = splineData.isPointMovable;

			final int splinePointLength = graphXSplineValues.length;

			_spPointRects = new Rectangle[splinePointLength];

			final int pointSize = 10;
			final int pointSize2 = pointSize / 2;
			final int hitSize = 10;
			final int hitSize2 = hitSize / 2;

			/*
			 * paint static points
			 */
			gc.setBackground(splineColor);
			for (int pointIndex = 0; pointIndex < splinePointLength; pointIndex++) {

				if (isPointMovable[pointIndex]) {
					continue;
				}

				final double graphX = graphXSplineValues[pointIndex] - _graphXValueOffset;
				final double graphY = graphYSplineValues[pointIndex];

				final int devPointX = (int) (graphX * _scaleX);
				final int devPointY = (int) (graphY * scaleValueDiff);

				final int devX = devPointX - pointSize2;
				final int devY = _devY0Spline - devPointY - pointSize2;

				gc.fillOval(devX, devY, pointSize, pointSize);

				// keep point position
				_spPointRects[pointIndex] = new Rectangle(
						devPointX - hitSize2,
						_devY0Spline - devPointY - hitSize2,
						hitSize,
						hitSize);
			}

			/*
			 * paint movable points
			 */
			gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
			for (int pointIndex = 0; pointIndex < splinePointLength; pointIndex++) {

				if (isPointMovable[pointIndex] == false) {
					continue;
				}

				final double graphSpX = graphXSplineValues[pointIndex] - _graphXValueOffset;
				final double graphSpY = graphYSplineValues[pointIndex];

				final int devPointX = (int) (graphSpX * _scaleX);
				final int devPointY = (int) (graphSpY * scaleValueDiff);

				final int devX = devPointX - pointSize2;
				final int devY = _devY0Spline - devPointY - pointSize2;

				gc.fillOval(devX, devY, pointSize, pointSize);

				// keep point position
				_spPointRects[pointIndex] = new Rectangle(
						devPointX - hitSize2,
						_devY0Spline - devPointY - hitSize2,
						hitSize,
						hitSize);
			}
		}

		/*
		 * paint spline points in the graph
		 */
		if (isPointInGraph && isAdjustedValues) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			final int[] graphSerieIndex = _splineData.serieIndex;

			for (final int serieIndex : graphSerieIndex) {

				final int graphX = xValues[serieIndex] - _graphXValueOffset;
				final int graphY = yAdjustedSerie[serieIndex];

				final int devX = (int) (graphX * _scaleX);
				final int devY = (int) (devY0 - (graphY * _scaleY));

				gc.fillOval(devX - 2, devY - 2, 5, 5);

				/*
				 * draw altitude
				 */
				final String altiText = Integer.toString(graphY);
				final Point textExtent = gc.textExtent(altiText);
				final int textWidth = textExtent.x;

				int devXText = devX - 2 - textWidth / 2;
				final int devYText = devY - 5 - textExtent.y;

				// ensure the text is visible
				if (devXText < 0) {
					devXText = 2;
				} else if (devXText + textWidth > graphClippingWidth) {
					devXText = graphClippingWidth - textWidth - 2;
				}

				gc.drawText(altiText, devXText, devYText, true);
			}
		}

		// dispose resources
		splineColor.dispose();
		path2ndSerie.dispose();
		pathValueDiff.dispose();
		pathAdjustValue.dispose();
	}

	public SplineDrawingData getDrawingData() {

		// create drawing data
		final SplineDrawingData drawingData = new SplineDrawingData();

		drawingData.graphXValueOffset = _graphXValueOffset;
		drawingData.devY0Spline = _devY0Spline;
		drawingData.scaleX = _scaleX;
		drawingData.scaleY = _scaleY;
		drawingData.devGraphValueXOffset = _devGraphValueXOffset;

		return drawingData;
	}

	public Rectangle[] getPointHitRectangels() {
		return _spPointRects;
	}
}
