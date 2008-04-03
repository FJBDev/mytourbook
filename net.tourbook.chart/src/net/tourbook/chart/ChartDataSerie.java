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

package net.tourbook.chart;

import java.util.HashMap;

import org.eclipse.swt.graphics.RGB;

public abstract class ChartDataSerie {

	public static final int			AXIS_UNIT_NUMBER				= 10;
	public static final int			AXIS_UNIT_HOUR_MINUTE			= 20;
	public static final int			AXIS_UNIT_HOUR_MINUTE_24H		= 21;
	public static final int			AXIS_UNIT_HOUR_MINUTE_SECOND	= 22;
	public static final int			AXIS_UNIT_MONTH					= 30;
	public static final int			AXIS_UNIT_DAY					= 40;
	public static final int			AXIS_UNIT_YEAR					= 50;

	public static final int			X_AXIS_UNIT_WEEK				= 100;

//	public static enum OverlayStatus {
//		NOT_SET, IN_QUEUE, IMAGE_AVAILABLE, NO_IMAGE
//	};

	/**
	 * Default color, when default color is not set
	 */
	private static RGB				DefaultDefaultRGB				= new RGB(0xFF, 0xA5, 0xCB);

	/**
	 * contains the values for the chart, highValues contains the upper value, lowValues the lower
	 * value. When lowValues is null then the low values is set to 0
	 */
	int[][]							fLowValues;

	int[][]							fHighValues;

	/**
	 * divisor for highValues
	 */
	private int						valueDivisor					= 1;

	/**
	 * unit which is drawn on the x-axis
	 */
	private int						axisUnit						= AXIS_UNIT_NUMBER;

	/**
	 * Text label for the unit
	 */
	private String					unitLabel						= new String();

	/**
	 * Text label for the chart data, e.g. distance, altitude, speed...
	 */
	private String					label							= new String();

	private HashMap<String, Object>	fCustomData						= new HashMap<String, Object>();

	/**
	 * max value which is used to draw the chart
	 */
	protected int					fVisibleMaxValue;

	/**
	 * min value which is used to draw the chart
	 */
	protected int					fVisibleMinValue;

	private RGB						fRgbBright[]					= new RGB[] { new RGB(255, 0, 0) };
	private RGB						fRgbDark[]						= new RGB[] { new RGB(0, 0, 255) };
	private RGB						fRgbLine[]						= new RGB[] { new RGB(0, 255, 0) };
	/**
	 * minimum value found in the provided values
	 */
	int								fOriginalMinValue;

	/**
	 * maximum value found in the provided values
	 */
	int								fOriginalMaxValue;

	private RGB						fDefaultRGB;

	public int getAxisUnit() {
		return axisUnit;
	}

	/**
	 * Returns the application defined property of the receiver with the specified name, or null if
	 * it was not been set.
	 */
	public Object getCustomData(String key) {
		if (fCustomData.containsKey(key)) {
			return fCustomData.get(key);
		} else {
			return null;
		}
	}

	/**
	 * @return Returns the default color for this data serie
	 */
	public RGB getDefaultRGB() {

		/*
		 * when default color is not set, return an ugly color to see in the ui that something is
		 * wrong
		 */
		if (fDefaultRGB == null) {
			return DefaultDefaultRGB;
		}

		return fDefaultRGB;
	}

	/**
	 * @return returns the value array
	 */
	public int[][] getHighValues() {
		return fHighValues;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * @return Returns the lowValues.
	 */
	public int[][] getLowValues() {
		return fLowValues;
	}

	public int getOriginalMaxValue() {
		return fOriginalMaxValue;
	}

	public int getOriginalMinValue() {
		return fOriginalMinValue;
	}

	public RGB[] getRgbBright() {
		return fRgbBright;
	}

	public RGB[] getRgbDark() {
		return fRgbDark;
	}

	public RGB[] getRgbLine() {
		return fRgbLine;
	}

	/**
	 * @return Returns the unit label for the data, e.g. m km/h sec h:m
	 */
	public String getUnitLabel() {
		return unitLabel;
	}

	public int getValueDivisor() {
		return valueDivisor;
	}

	/**
	 * @return returns the maximum value in the data serie
	 */
	public int getVisibleMaxValue() {
		return fVisibleMaxValue;
	}

	/**
	 * @return returns the minimum value in the data serie
	 */
	public int getVisibleMinValue() {
		return fVisibleMinValue;
	}

	public void setAxisUnit(int axisUnit) {
		this.axisUnit = axisUnit;
	}

	/**
	 * Sets the application defined property of the receiver with the specified name to the given
	 * value.
	 */
	public void setCustomData(String key, Object value) {
		fCustomData.put(key, value);
	}

	public void setDefaultRGB(RGB color) {
		fDefaultRGB = color;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	void setMinMaxValues(int valueSeries[][]) {

		if (valueSeries == null || valueSeries.length == 0 || valueSeries[0] == null || valueSeries[0].length == 0) {
			fVisibleMaxValue = fVisibleMinValue = 0;
			fOriginalMaxValue = fOriginalMinValue = 0;
			fHighValues = new int[1][2];
			fLowValues = new int[1][2];
		} else {

			fHighValues = valueSeries;

			// set initial min/max value
			fVisibleMaxValue = fVisibleMinValue = valueSeries[0][0];

			// calculate min/max highValues
			for (int[] valueSerie : valueSeries) {
				for (int value : valueSerie) {
					fVisibleMaxValue = Math.max(fVisibleMaxValue, value);
					fVisibleMinValue = Math.min(fVisibleMinValue, value);
				}
			}

			/*
			 * force the min/max values to have not the same value this is necessary to display a
			 * visible line in the chart
			 */
			if (fVisibleMinValue == fVisibleMaxValue) {

				fVisibleMaxValue++;

				if (fVisibleMinValue > 0) {
					fVisibleMinValue--;
				}
			}

			fOriginalMinValue = fVisibleMinValue;
			fOriginalMaxValue = fVisibleMaxValue;
		}
	}

	abstract void setMinMaxValues(int[][] lowValues, int[][] highValues);

	public void setRgbBright(RGB[] rgbBright) {
		fRgbBright = rgbBright;
	}

	public void setRgbDark(RGB[] rgbDark) {
		fRgbDark = rgbDark;
	}

	public void setRgbLine(RGB[] rgbLine) {
		fRgbLine = rgbLine;
	}

	/**
	 * @param unit
	 *        The measurement to set.
	 */
	public void setUnitLabel(String unit) {
		this.unitLabel = unit == null ? "" : unit; //$NON-NLS-1$
	}

	public void setValueDivisor(int valueDivisor) {
		this.valueDivisor = valueDivisor;
	}

	public void setVisibleMaxValue(int maxValue) {
		fVisibleMaxValue = maxValue;
	}

	public void setVisibleMinValue(int minValue) {
		fVisibleMinValue = minValue;
	}

}
