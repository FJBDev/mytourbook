/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.ui.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayer2ndAltiSerie implements IChartLayer {

	/**
	 * contains tour which is displayed in the chart
	 */
	private TourData				fTourData;
	private int[]					fXDataSerie;
	private TourChartConfiguration	fTourChartConfig;
	private Rectangle[]				fSpPointRects;

	public ChartLayer2ndAltiSerie(	final TourData tourData,
									final int[] xDataSerie,
									final TourChartConfiguration tourChartConfig) {

		fTourData = tourData;
		fTourChartConfig = tourChartConfig;

		// x-data serie contains the time or distance distance data serie
		fXDataSerie = xDataSerie;
	}

	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final int[] xValues = fXDataSerie;

		final int[] yValues2ndSerie = fTourData.dataSerie2ndAlti;
		final int[] yDiffTo2ndSerie = fTourData.dataSerieDiffTo2ndAlti;
		final int[] yAdjustedSerie = fTourData.dataSerieAdjustedAlti;

		final boolean isDiffValues = yDiffTo2ndSerie != null;
		final boolean isAdjustedValues = yAdjustedSerie != null;

		if (xValues == null || xValues.length == 0 || yValues2ndSerie == null || yValues2ndSerie.length == 0) {
			return;
		}

		final float measurementSystem = UI.UNIT_VALUE_ALTITUDE;

		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		// get the horizontal offset for the graph
		final int graphXValueOffset = (int) (Math.max(0, chart.getDevGraphImageXOffset()) / scaleX);

		final Display display = Display.getCurrent();

		final Path path2ndSerie = new Path(display);
		final Path pathValueDiff = new Path(display);
		final Path pathAdjustValue = new Path(display);

//		final RGB rgbFg = yData.getRgbLine()[0];
//		final RGB rgbBg1 = yData.getRgbDark()[0];
//		final RGB rgbBg2 = yData.getRgbBright()[0];
//
//		final int graphYTop = drawingData.getGraphYTop();
		final int graphYBottom = drawingData.getGraphYBottom();

		final int devGraphHeight = drawingData.getDevGraphHeight();
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - devGraphHeight;
		
		// write spline into the middle of the chart 
		final int devY0Spline = devYBottom - devGraphHeight / 2;

		/*
		 * convert all diff values into positive values
		 */
		int diffValues[] = null;
		float scaleValueDiff = scaleY;
		if (isDiffValues) {

			int valueIndex = 0;
			int maxValueDiff = 0;

			diffValues = new int[yDiffTo2ndSerie.length];
			for (int valueDiff : yDiffTo2ndSerie) {
				diffValues[valueIndex++] = valueDiff = (valueDiff < 0) ? -valueDiff : valueDiff;
				maxValueDiff = (maxValueDiff >= valueDiff) ? maxValueDiff : valueDiff;
			}
			maxValueDiff /= measurementSystem;

			// set value diff scaling
			if (fTourChartConfig.isRelativeValueDiffScaling) {
				scaleValueDiff = maxValueDiff == 0 ? scaleY : (float) devGraphHeight / 2 / maxValueDiff;
			}
		}

		// position for the x-axis line in the graph
		final float devY0 = devYBottom + (scaleY * graphYBottom);

		final int startIndex = 0;
		final int endIndex = xValues.length;

		final Rectangle graphRect = new Rectangle(0, devYTop, gc.getClipping().width, devGraphHeight);

		/*
		 * create paths
		 */
		for (int xValueIndex = startIndex; xValueIndex < endIndex; xValueIndex++) {

			// make sure the x-index is not higher than the yValues length
			if (xValueIndex >= yValues2ndSerie.length) {
				return;
			}

			final int graphXValue = xValues[xValueIndex] - graphXValueOffset;
			final int graphYValue2nd = (int) (yValues2ndSerie[xValueIndex] / measurementSystem);

			final float devXValue = graphXValue * scaleX;
			final float devYValue2nd = graphYValue2nd * scaleY;

			/*
			 * draw adjusted value graph
			 */
			if (isAdjustedValues) {

				final float devYAdjustedValue = yAdjustedSerie[xValueIndex] * scaleY / measurementSystem;

				if (xValueIndex == startIndex) {

					// move to the first point
					pathAdjustValue.moveTo(0, devYBottom);
					pathAdjustValue.lineTo(devXValue, devY0 - devYAdjustedValue);
				}

				// draw line to the next point
				pathAdjustValue.lineTo(devXValue, devY0 - devYAdjustedValue);

				if (xValueIndex == endIndex - 1) {

					/*
					 * this is the last point, draw the line to the x-axis and the start of the
					 * chart
					 */
					pathAdjustValue.lineTo(devXValue, devYBottom);
				}
			}

			/*
			 * draw value graph
			 */
			if (xValueIndex == startIndex) {

				// move to the first point
				path2ndSerie.moveTo(devXValue, devY0 - devYValue2nd);
			}

			// draw line to the next point
			path2ndSerie.lineTo(devXValue, devY0 - devYValue2nd);

			/*
			 * draw diff values
			 */
			if (isDiffValues) {

				final int graphValueDiff = (int) (diffValues[xValueIndex] / measurementSystem);
				final float devLayerValueDiff = graphValueDiff * scaleValueDiff;

				if (xValueIndex == startIndex) {

					// move to the first point
					pathValueDiff.moveTo(devXValue, devYBottom - devLayerValueDiff);
				}

				// draw line to the next point
				pathValueDiff.lineTo(devXValue, devYBottom - devLayerValueDiff);
			}
		}

		// draw the line of the graph
		gc.setAntialias(SWT.OFF);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setClipping(graphRect);

		/*
		 * paint adjusted value graph
		 */
		if (isAdjustedValues) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			gc.setAlpha(0x80);

			// fill background
			gc.setClipping(pathAdjustValue);
			gc.fillGradientRectangle(0, devYTop, gc.getClipping().width, devGraphHeight, true);
			gc.setClipping(graphRect);

			// draw graph
			gc.drawPath(pathAdjustValue);

			gc.setAlpha(0xff);
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
		final int[] ySplineSerie = fTourData.dataSerieSpline;
		if (ySplineSerie != null) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

			int devPrevX = (int) ((xValues[0] - graphXValueOffset) * scaleX);
			int devPrevY = (int) (ySplineSerie[0] / measurementSystem * scaleY);


			for (int xIndex = 1; xIndex < xValues.length; xIndex++) {

				final float graphX = xValues[xIndex];
				final float graphY = ySplineSerie[xIndex] / measurementSystem;

				final int devX = (int) (graphX * scaleX);
				final int devY = (int) (graphY * scaleY);

				if (!(devX == devPrevX && devY == devPrevY)) {
					gc.drawLine(devPrevX, devY0Spline - devPrevY, devX, devY0Spline - devY);
				}

				devPrevX = devX;
				devPrevY = devY;
			}
		}

		/*
		 * paint data graph
		 */
		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		gc.drawPath(path2ndSerie);

		/*
		 * paint special points on the diff graph
		 */
		final SplineData splineData = fTourData.splineDataPoints;
		if (splineData != null) {

			final double[] xSplineValues = splineData.xValues;
			final double[] ySplineValues = splineData.yValues;
			final boolean[] isPointMovable = splineData.isPointMovable;
			
			final int splinePointLength = xSplineValues.length;

			fSpPointRects = new Rectangle[splinePointLength];

			final int pointSize = 10;
			final int pointSize2 = pointSize / 2;
			final int hitSize = 10;
			final int hitSize2 = hitSize / 2;

			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

			for (int pointIndex = 0; pointIndex < splinePointLength; pointIndex++) {

				final double graphSpX = xSplineValues[pointIndex] - graphXValueOffset;
				final double graphSpY = ySplineValues[pointIndex] / measurementSystem;

				final int devPointX = (int) (graphSpX * scaleX);
				final int devPointY = (int) (graphSpY * scaleValueDiff);

				final int devX = devPointX - pointSize2;
				final int devY = devY0Spline - devPointY - pointSize2;

				// draw movable points with different colors
				if (isPointMovable[pointIndex]) {
					gc.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
				} else {
					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
				}
				
				gc.fillOval(devX, devY, pointSize, pointSize);

				// keep point position
				fSpPointRects[pointIndex] = new Rectangle(devPointX - hitSize2,
						devY0Spline - devPointY - hitSize2,
						hitSize,
						hitSize);
			}
		}

		// dispose resources
		path2ndSerie.dispose();
		pathValueDiff.dispose();
		pathAdjustValue.dispose();
	}

	public Rectangle[] getPointHitRectangels() {
		return fSpPointRects;
	}
}
