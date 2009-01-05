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
package net.tourbook.ui.views;

import net.tourbook.chart.ComputeChartValue;

/**
 * contains the information which is needed to draw the average values in the tour chart analyzer
 */
public class TourChartAnalyzerInfo {

	private boolean				showAvg			= false;
	private boolean				showAvgDecimals	= false;
	private ComputeChartValue	computeValues	= null;
	private int					avgDecimals		= 1;

	/**
	 * default constructor
	 */
	public TourChartAnalyzerInfo() {}

	/**
	 * @param showAvg
	 */
	public TourChartAnalyzerInfo(final boolean showAvg) {
		this.showAvg = showAvg;
	}

	/**
	 * @param showAvg
	 * @param showAvgDecimals
	 */
	public TourChartAnalyzerInfo(final boolean showAvg, final boolean showAvgDecimals) {
		this.showAvg = showAvg;
		this.showAvgDecimals = showAvgDecimals;
	}

	/**
	 * @param showAvg
	 * @param computeValues
	 */
	public TourChartAnalyzerInfo(final boolean showAvg, final ComputeChartValue computeValues) {
		this.showAvg = showAvg;
		this.computeValues = computeValues;
	}

	/**
	 * @param showAvg
	 * @param showAvgDecimals
	 * @param computeValues
	 * @param avgDecimals
	 */
	public TourChartAnalyzerInfo(	final boolean showAvg,
									final boolean showAvgDecimals,
									final ComputeChartValue computeValues,
									final int avgDecimals) {

		this.showAvg = showAvg;
		this.showAvgDecimals = showAvgDecimals;
		this.computeValues = computeValues;
		this.avgDecimals = avgDecimals;
	}

	/**
	 * @return Returns the computeChartValue.
	 */
	public ComputeChartValue getComputeChartValue() {
		return computeValues;
	}

	/**
	 * @return Returns the showAvg.
	 */
	public boolean isShowAvg() {
		return showAvg;
	}

	/**
	 * @return Returns the showAvgDecimals.
	 */
	public boolean isShowAvgDecimals() {
		return showAvgDecimals;
	}

	/**
	 * @return Returns the avgDecimals.
	 */
	public int getAvgDecimals() {
		return avgDecimals;
	}

}
