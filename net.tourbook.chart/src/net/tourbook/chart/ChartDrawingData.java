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
package net.tourbook.chart;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Rectangle;

public class ChartDrawingData {

	// position for the x-axis unit text
	protected static final int		XUNIT_TEXT_POS_LEFT		= 0;
	protected static final int		XUNIT_TEXT_POS_CENTER	= 1;

	public static final int			BAR_POS_LEFT			= 0;
	public static final int			BAR_POS_CENTER			= 1;

	private ChartDataXSerie			xData;
	private ChartDataXSerie			xData2nd;
	private ChartDataYSerie			yData;

	private Rectangle[]				ySliderHitRect;

	private String					fXTitle;

	/**
	 * management for the bar graph
	 */
	private Rectangle[][]			barRectangles;
	private Rectangle[][]			barFocusRectangles;

	private int						fBarRectangleWidth;
	private int						fDevBarRectangleXPos;

	/**
	 * Contains all unit labels and their positions for the x axis
	 */
	private ArrayList<ChartUnit>	xUnits					= new ArrayList<ChartUnit>();
	private int						xUnitTextPos			= XUNIT_TEXT_POS_LEFT;

	/**
	 * List with all unit labels and positions for the y axis
	 */
	private ArrayList<ChartUnit>	yUnits					= new ArrayList<ChartUnit>();

	// scaling from graph value to device value
	private float					scaleX;
	private float					scaleY;

	/**
	 * scaling the the x unit
	 */
	private float					scaleUnitX				= Float.MIN_VALUE;

	private int						devMarginTop;
	private int						devXTitelBarHeight;
	private int						devMarkerBarHeight;
	private int						devSliderBarHeight;

	// graph position
	private int						devYTop;
	private int						devYBottom;

	/**
	 * virtual graph width in dev (pixel) units
	 */
	int								devVirtualGraphWidth;

	/**
	 * graph height in dev (pixel) units
	 */
	public int						devGraphHeight;

	private int						devSliderHeight;

	/**
	 * graph value for the bottom of the graph
	 */
	private int						graphYBottom;
	private int						graphYTop;

	private int						barPosition				= BAR_POS_LEFT;

	private int						fChartType;

	public ChartDrawingData(final int chartType) {
		fChartType = chartType;
	}

	/**
	 * @return Returns the barFocusRectangles.
	 */
	public Rectangle[][] getBarFocusRectangles() {
		return barFocusRectangles;
	}

	/**
	 * @return Returns the barPosition, this can be set to BAR_POS_LEFT, BAR_POS_CENTER
	 */
	public int getBarPosition() {
		return barPosition;
	}

	/**
	 * @return Returns the barRectangles.
	 */
	public Rectangle[][] getBarRectangles() {
		return barRectangles;
	}

	/**
	 * @return Returns the barRectangleWidth.
	 */
	public int getBarRectangleWidth() {
		return fBarRectangleWidth;
	}

	public int getChartType() {
		return fChartType;
	}

	/**
	 * @return Returns the barRectanglePos.
	 */
	public int getDevBarRectangleXPos() {
		return fDevBarRectangleXPos;
	}

//	public int getDevGraphHeight() {
//		return devGraphHeight;
//	}
//
//	/**
//	 * virtual graph width in dev (pixel) units
//	 */
//	int getDevGraphWidth() {
//		return devGraphWidth;
//	}

	/**
	 * @return Returns the devMarginTop.
	 */
	public int getDevMarginTop() {
		return devMarginTop;
	}

	/**
	 * @return Returns the devMarkerBarHeight.
	 */
	public int getDevMarkerBarHeight() {
		return devMarkerBarHeight;
	}

	/**
	 * @return Returns the devSliderBarHeight.
	 */
	public int getDevSliderBarHeight() {
		return devSliderBarHeight;
	}

	public int getDevSliderHeight() {
		return devSliderHeight;
	}

	/**
	 * @return Returns the devTitelBarHeight.
	 */
	public int getDevXTitelBarHeight() {
		return devXTitelBarHeight;
	}

	/**
	 * @return Returns the bottom of the chart in dev units
	 */
	public int getDevYBottom() {
		return devYBottom;
	}

	/**
	 * @return Returns the y position for the title
	 */
	public int getDevYTitle() {
		return getDevYBottom() - devGraphHeight - getDevSliderBarHeight() - getDevXTitelBarHeight();
	}

	/**
	 * @return Returns the top of the chart in dev units
	 */
	public int getDevYTop() {
		return devYTop;
	}

	/**
	 * @return Returns the bottom of the chart in graph units
	 */
	public int getGraphYBottom() {
		return graphYBottom;
	}

	/**
	 * @return Returns the top of the chart in graph units
	 */
	public int getGraphYTop() {
		return graphYTop;
	}

	public float getScaleUnitX() {
		return scaleUnitX;
	}

	public float getScaleX() {
		return scaleX;
	}

	public float getScaleY() {
		return scaleY;
	}

	/**
	 * @return Returns the xData.
	 */
	public ChartDataXSerie getXData() {
		return xData;
	}

	/**
	 * @return Returns the xData2nd.
	 */
	public ChartDataXSerie getXData2nd() {
		return xData2nd;
	}

	public String getXTitle() {
		return fXTitle;
	}

	/**
	 * @return Returns the units for the x-axis.
	 */
	public ArrayList<ChartUnit> getXUnits() {
		return xUnits;
	}

	/**
	 * @return Returns the xUnitTextPos.
	 */
	public int getXUnitTextPos() {
		return xUnitTextPos;
	}

	/**
	 * @return Returns the ChartDataXSerie for the y-axis
	 */
	public ChartDataYSerie getYData() {
		return yData;
	}

	/**
	 * @return Returns the ySliderHitRect.
	 */
	public Rectangle[] getYSliderHitRect() {
		return ySliderHitRect;
	}

	/**
	 * @return Returns the yUnits.
	 */
	public ArrayList<ChartUnit> getYUnits() {
		return yUnits;
	}

	/**
	 * @param barFocusRectangles
	 *            The barFocusRectangles to set.
	 */
	public void setBarFocusRectangles(final Rectangle[][] barFocusRectangles) {
		this.barFocusRectangles = barFocusRectangles;
	}

	/**
	 * @param barPosition
	 *            The barPosition to set.
	 */
	public void setBarPosition(final int barPosition) {
		this.barPosition = barPosition;
	}

	/**
	 * @param barRectangles
	 *            The barRectangles to set.
	 */
	public void setBarRectangles(final Rectangle[][] barRectList) {
		this.barRectangles = barRectList;
	}

	/**
	 * @param barRectangleWidth
	 *            The barRectangleWidth to set.
	 */
	public void setBarRectangleWidth(final int barRectangleWidth) {
		this.fBarRectangleWidth = barRectangleWidth;
	}

	/**
	 * @param barRectanglePos
	 *            The barRectanglePos to set.
	 */
	public void setDevBarRectangleXPos(final int barRectanglePos) {
		fDevBarRectangleXPos = barRectanglePos;
	}

//	public void setDevGraphHeight(final int heightDev) {
//		this.devGraphHeight = heightDev;
//	}
//
//	public void setDevGraphWidth(final int devGraphWidth) {
//		this.devGraphWidth = devGraphWidth;
//	}

	/**
	 * @param devMarginTop
	 *            The devMarginTop to set.
	 */
	public void setDevMarginTop(final int devMarginTop) {
		this.devMarginTop = devMarginTop;
	}

	/**
	 * @param devMarkerBarHeight
	 *            The devMarkerBarHeight to set.
	 */
	public void setDevMarkerBarHeight(final int devMarkerBarHeight) {
		this.devMarkerBarHeight = devMarkerBarHeight;
	}

	/**
	 * @param devSliderBarHeight
	 *            The devSliderBarHeight to set.
	 */
	public void setDevSliderBarHeight(final int devSliderBarHeight) {
		this.devSliderBarHeight = devSliderBarHeight;
	}

	public void setDevSliderHeight(final int devSliderHeight) {
		this.devSliderHeight = devSliderHeight;
	}

	/**
	 * @param devTitelBarHeight
	 *            The devTitelBarHeight to set.
	 */
	void setDevXTitelBarHeight(final int devTitelBarHeight) {
		this.devXTitelBarHeight = devTitelBarHeight;
	}

	public void setDevYBottom(final int devY) {
		this.devYBottom = devY;
	}

	/**
	 * @param devYTop
	 *            The devYTop to set.
	 */
	protected void setDevYTop(final int devYTop) {
		this.devYTop = devYTop;
	}

	public void setGraphYBottom(final int yGraphMin) {
		this.graphYBottom = yGraphMin;
	}

	/**
	 * @param graphYTop
	 *            The graphYTop to set.
	 */
	protected void setGraphYTop(final int graphYTop) {
		this.graphYTop = graphYTop;
	}

	/**
	 * Set scaling for the x-axis unit
	 * 
	 * @param scaleXUnit
	 */
	public void setScaleUnitX(final float scaleXUnit) {
		this.scaleUnitX = scaleXUnit;
	}

	/**
	 * Set scaling for the x-axis values
	 * 
	 * @param scaleX
	 */
	public void setScaleX(final float scaleX) {
		this.scaleX = scaleX;
	}

	public void setScaleY(final float scaleY) {
		this.scaleY = scaleY;
	}

	/**
	 * @param xData
	 *            The xData to set.
	 */
	public void setXData(final ChartDataXSerie xData) {
		this.xData = xData;
	}

	/**
	 * @param data2nd
	 *            The xData2nd to set.
	 */
	public void setXData2nd(final ChartDataXSerie data2nd) {
		xData2nd = data2nd;
	}

	public void setXTitle(final String title) {
		fXTitle = title;
	}

	/**
	 * set the position of the unit text, this can be XUNIT_TEXT_POS_LEFT or XUNIT_TEXT_POS_CENTER
	 * 
	 * @param unitTextPos
	 *            The xUnitTextPos to set.
	 */
	public void setXUnitTextPos(final int unitTextPos) {
		xUnitTextPos = unitTextPos;
	}

	/**
	 * @param data
	 *            The yData to set.
	 */
	public void setYData(final ChartDataYSerie data) {
		this.yData = data;
	}

	/**
	 * @param sliderHitRect
	 *            The ySliderHitRect to set.
	 */
	public void setYSliderHitRect(final Rectangle[] sliderHitRect) {
		ySliderHitRect = sliderHitRect;
	}
}
