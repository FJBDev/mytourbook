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

package net.tourbook.tour;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.IMouseListener;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.SplineData;
import net.tourbook.data.TourData;
import net.tourbook.math.CubicSpline;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.ChartLayer2ndAltiSerie;
import net.tourbook.ui.tourChart.I2ndAltiLayer;
import net.tourbook.ui.tourChart.IXAxisSelectionListener;
import net.tourbook.ui.tourChart.SplineDrawingData;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.part.PageBook;

/**
 * Dialog to adjust the altitude, this dialog can be opened from within a tour chart or from the
 * tree viewer
 */
public class DialogAdjustAltitude extends TitleAreaDialog implements I2ndAltiLayer {

	private static final String			WIDGET_DATA_ALTI_ID				= "altiId";						//$NON-NLS-1$
	private static final String			WIDGET_DATA_METRIC_ALTITUDE		= "metricAltitude";				//$NON-NLS-1$

	private static final int			ALTI_ID_START					= 1;
	private static final int			ALTI_ID_END						= 2;
	private static final int			ALTI_ID_MAX						= 3;

	private static final int			ADJUST_TYPE_UNTIL_LEFT_SLIDER	= 1000;
	private static final int			ADJUST_TYPE_WHOLE_TOUR			= 1001;
	private static final int			ADJUST_TYPE_START_AND_END		= 1002;
	private static final int			ADJUST_TYPE_MAX_HEIGHT			= 1003;
	private static final int			ADJUST_TYPE_END					= 1004;

	private static final String			PREF_ADJUST_TYPE				= "adjust.altitude.adjust_type";	//$NON-NLS-1$

	private final IPreferenceStore		fPrefStore						= TourbookPlugin.getDefault()
																				.getPreferenceStore();
	private final NumberFormat			fNF								= NumberFormat.getNumberInstance();

	private Image						fShellImage;

	private boolean						fIsChartUpdated;
	private boolean						fIsTourSaved					= false;

	private TourData					fTourData;
	private int[]						fBackupAltitudeSerie;
	private int[]						fSrtmMetricValues;

	private TourChart					fTourChart;
	private TourChartConfiguration		fTourChartConfig;

//	private Composite					fDialogContainer;

	private PageBook					fPageBookOptions;
	private Label						fPageEmpty;
	private Composite					fPageOptionSRTM;
	private Composite					fPageOptionNoSRTM;

	private Button						fBtnRemoveAllPoints;
	private Combo						fComboAdjustmentType;

	private static AdjustmentType[]		fAllAdjustmentTypes;
	private ArrayList<AdjustmentType>	fAvailableAdjustmentTypes		= new ArrayList<AdjustmentType>();

	private ChartLayer2ndAltiSerie		fChartLayer2ndAltiSerie;

	private int							fPointHitIndex					= -1;
	private SplineData					fSplineData;
	private int							fAltiDiff;
	private int							fSliderXAxisValue;

	private boolean						fCanDeletePoint;
	protected boolean					fIsModifiedInternal;

	private Spinner						fSpinnerNewStartAlti;
	private Spinner						fSpinnerNewMaxAlti;
	private Spinner						fSpinnerNewEndAlti;

	private Label						fLblOldStartAlti;
	private Label						fLblOldMaxAlti;
	private Label						fLblOldEndAlti;

	private Button						fRadioKeepBottom;
	private Button						fRadioKeepStart;

	private Composite					fDlgContainer;

	private int							fInitialAltiStart;
	private int							fInitialAltiMax;

	private int							fAltiMaxDiff;
	private int							fAltiStartDiff;

	private int							fOldStartAlti;
	private int							fOldAltiInputMax;
	private int							fOldAltiInputStart;

	{
		fNF.setMinimumFractionDigits(0);
		fNF.setMaximumFractionDigits(3);

		fAllAdjustmentTypes = new AdjustmentType[] {
			new AdjustmentType(ADJUST_TYPE_UNTIL_LEFT_SLIDER, Messages.adjust_altitude_type_until_left_slider),
			new AdjustmentType(ADJUST_TYPE_WHOLE_TOUR, Messages.adjust_altitude_type_adjust_whole_tour),
			new AdjustmentType(ADJUST_TYPE_START_AND_END, Messages.adjust_altitude_type_start_and_end),
			new AdjustmentType(ADJUST_TYPE_MAX_HEIGHT, Messages.adjust_altitude_type_adjust_height),
			new AdjustmentType(ADJUST_TYPE_END, Messages.adjust_altitude_type_adjust_end) //
		};
	};

	private class AdjustmentType {

		int		id;
		String	visibleName;

		AdjustmentType(final int id, final String visibleName) {
			this.id = id;
			this.visibleName = visibleName;
		}
	}

	public DialogAdjustAltitude(final Shell parentShell, final TourData tourData) {

		super(parentShell);

		fTourData = tourData;
		fSrtmMetricValues = fTourData.getSRTMSerieMetric();

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.Image__edit_adjust_altitude).createImage();
		setDefaultImage(fShellImage);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	void actionCreateSplinePoint(final int mouseDownDevPositionX, final int mouseDownDevPositionY) {

		if (computeNewPoint(mouseDownDevPositionX, mouseDownDevPositionY)) {

			onChangeAdjustType();
		}
	}

	private void adjustAltitude(final Integer altiFlag) {

		final int newAltiStart = (Integer) fSpinnerNewStartAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
		final int newAltiMax = (Integer) fSpinnerNewMaxAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);
		final int newAltiEnd = (Integer) fSpinnerNewEndAlti.getData(WIDGET_DATA_METRIC_ALTITUDE);

		final int[] altitudeSerie = fTourData.altitudeSerie;
		final int[] adjustedAltiSerie = fTourData.dataSerieAdjustedAlti = new int[fTourData.timeSerie.length];

		final boolean isAltiSetByUser = altiFlag != null;

		// set adjustment type and enable the field(s) which can be modified
		switch (getSelectedAdjustmentType().id) {

		case ADJUST_TYPE_START_AND_END:

			// adjust start, end and max

			// adjust end alti to start alti
			adjustAltitudeEnd(altitudeSerie, adjustedAltiSerie, altitudeSerie[0]);

			adjustAltitudeStartAndMax(adjustedAltiSerie, adjustedAltiSerie, isAltiSetByUser, newAltiStart, newAltiMax);

			break;

		case ADJUST_TYPE_WHOLE_TOUR:

			// adjust evenly
			adjustAltitudeEvenly(altitudeSerie, adjustedAltiSerie, newAltiStart);
			break;

		case ADJUST_TYPE_END:

			// adjust end
			adjustAltitudeEnd(altitudeSerie, adjustedAltiSerie, newAltiEnd);
			break;

		case ADJUST_TYPE_MAX_HEIGHT:

			// adjust max

			adjustAltitudeStartAndMax(altitudeSerie, adjustedAltiSerie, isAltiSetByUser, newAltiStart, newAltiMax);
			break;

		default:
			break;
		}

//		/*
//		 * make a backup of the current values
//		 */
//		final int[] altitudeSerie = fTourData.altitudeSerie;
//		if (altitudeSerie != null) {
//			fAltitudeSerieModified = new int[altitudeSerie.length];
//
//			for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
//				fAltitudeSerieModified[altiIndex] = altitudeSerie[altiIndex];
//			}
//		}

		// force the imperial altitude series to be recomputed
		fTourData.clearAltitudeSeries();
	}

	/**
	 * adjust end altitude
	 * 
	 * @param altiSrc
	 * @param tourData
	 * @param newEndAlti
	 */
	private void adjustAltitudeEnd(final int[] altiSrc, final int[] altiDest, final int newEndAlti) {

		int[] endDataSerie = fTourData.getDistanceSerie();

		if (endDataSerie == null) {
			endDataSerie = fTourData.timeSerie;
		}

		final int altiEndDiff = newEndAlti - altiSrc[altiDest.length - 1];
		final float lastEndDataValue = endDataSerie[endDataSerie.length - 1];

		for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {
			final float endDataValue = endDataSerie[serieIndex];
			final float altiDiff = endDataValue / lastEndDataValue * altiEndDiff;
			altiDest[serieIndex] = altiSrc[serieIndex] + Math.round(altiDiff);
		}
	}

	/**
	 * adjust every altitude with the same difference
	 * 
	 * @param altiSrc
	 * @param altiDest
	 * @param newStartAlti
	 */
	private void adjustAltitudeEvenly(final int[] altiSrc, final int[] altiDest, final int newStartAlti) {

		final int altiStartDiff = newStartAlti - altiSrc[0];

		for (int altIndex = 0; altIndex < altiSrc.length; altIndex++) {
			altiDest[altIndex] = altiSrc[altIndex] + altiStartDiff;
		}
	}

	/**
	 * Adjust max altitude, keep min value
	 * 
	 * @param altiSrc
	 * @param altiDest
	 * @param maxAltiNew
	 */
	private void adjustAltitudeMax(final int[] altiSrc, final int[] altiDest, final int maxAltiNew) {

		// calculate min/max altitude
		int maxAltiSrc = altiSrc[0];
		int minAltiSrc = altiSrc[0];
		for (final int altitude : altiSrc) {
			if (altitude > maxAltiSrc) {
				maxAltiSrc = altitude;
			}
			if (altitude < minAltiSrc) {
				minAltiSrc = altitude;
			}
		}

		// adjust altitude
		final int altiDiffSrc = maxAltiSrc - minAltiSrc;
		final int altiDiffNew = maxAltiNew - minAltiSrc;

		final float altiDiff = (float) altiDiffSrc / (float) altiDiffNew;

		for (int serieIndex = 0; serieIndex < altiDest.length; serieIndex++) {

			float alti0Based = altiSrc[serieIndex] - minAltiSrc;
			alti0Based = alti0Based / altiDiff;

			altiDest[serieIndex] = Math.round(alti0Based) + minAltiSrc;
		}
	}

	/**
	 * Adjust start and max at the same time
	 * <p>
	 * it took me several days to figure out this algorithim, 10.4.2007 Wolfgang
	 */
	private void adjustAltitudeStartAndMax(	final int[] altiSrc,
											final int[] altiDest,
											final boolean isAltiSetByUser,
											final int newAltiStart,
											final int newAltiMax) {
		if (isAltiSetByUser) {

			// adjust max
			fAltiStartDiff -= fOldAltiInputStart - newAltiStart;
			fAltiMaxDiff -= fOldAltiInputMax - newAltiMax;

			final int oldStart = altiSrc[0];
			adjustAltitudeMax(altiSrc, altiDest, fInitialAltiMax + fAltiMaxDiff);
			final int newStart = altiDest[0];

			// adjust start
			int startDiff;
			if (fRadioKeepStart.getSelection()) {
				startDiff = 0;
			} else {
				startDiff = newStart - oldStart;
			}
			adjustAltitudeEvenly(altiDest, altiDest, fInitialAltiStart + fAltiStartDiff + startDiff);

		} else {

			// set initial altitude values

			int altiMax = altiDest[0];
			for (final int altitude : altiDest) {
				if (altitude > altiMax) {
					altiMax = altitude;
				}
			}

			fInitialAltiStart = altiDest[0];
			fInitialAltiMax = altiMax;

			fAltiStartDiff = 0;
			fAltiMaxDiff = 0;
		}
	}

	@Override
	public boolean close() {

		saveState();

		if (fIsTourSaved == false) {

			// tour is not saved, dialog is canceled, restore original values

			restoreDataBackup();
		}

		return super.close();
	}

	/**
	 * adjust start altitude until left slider
	 */
	private void computeAltitudeUntilLeftSlider() {

		// srtm values are available, otherwise this option is not available in the combo box

		final int serieLength = fTourData.timeSerie.length;
		final int sliderIndex = fTourChart.getXSliderPosition().getLeftSliderValueIndex();

//		final int[] srtm2ndAlti = fTourData.getSRTMSerieMetric();
		final int[] adjustedAltiSerie = fTourData.dataSerieAdjustedAlti = new int[serieLength];
		final int[] diffTo2ndAlti = fTourData.dataSerieDiffTo2ndAlti = new int[serieLength];
		final float[] splineDataSerie = fTourData.dataSerieSpline = new float[serieLength];

		final int[] altitudeSerie = fTourData.altitudeSerie;
		final int[] xAxisSerie = fTourChartConfig.showTimeOnXAxis ? fTourData.timeSerie : fTourData.distanceSerie;

		// get altitude diff serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
			diffTo2ndAlti[serieIndex] = altitudeSerie[serieIndex] - fSrtmMetricValues[serieIndex];
		}

		fSliderXAxisValue = xAxisSerie[sliderIndex];
		fAltiDiff = -diffTo2ndAlti[0];

		final CubicSpline cubicSpline = updateSplineData();

		// get adjusted altitude serie
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			if (serieIndex < sliderIndex) {

				// add adjusted altitude

				final float distance = xAxisSerie[serieIndex];
				final float distanceScale = 1 - (distance / fSliderXAxisValue);

				final int adjustedAltiDiff = (int) (fAltiDiff * distanceScale);
				final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

				float splineAlti = 0;
				try {

					splineAlti = (float) cubicSpline.interpolate(distance);

				} catch (final IllegalArgumentException e) {
					final double[] xValues = fTourData.splineDataPoints.xValues;
					System.out.println((xValues[0] + " ") // //$NON-NLS-1$
							+ (xValues[1] + " ") //$NON-NLS-1$
							+ (xValues[2] + " ")); //$NON-NLS-1$
					e.printStackTrace();
					return;
				}
				splineDataSerie[serieIndex] = splineAlti;

				final int adjustedAlti = newAltitude + (int) splineAlti;
				adjustedAltiSerie[serieIndex] = adjustedAlti;
				diffTo2ndAlti[serieIndex] = fSrtmMetricValues[serieIndex] - adjustedAlti;

			} else {

				// set altitude which is not adjusted

				adjustedAltiSerie[serieIndex] = altitudeSerie[serieIndex];
			}
		}

	}

	private void computeDeletedPoint() {

		if (fSplineData.isPointMovable.length <= 3) {
			// prevent deleting less than 3 points
			return;
		}

		final boolean[] oldIsPointMovable = fSplineData.isPointMovable;
		final float[] oldPosX = fSplineData.relativePositionX;
		final float[] oldPosY = fSplineData.relativePositionY;
		final double[] oldXValues = fSplineData.xValues;
		final double[] oldYValues = fSplineData.yValues;
		final double[] oldXMinValues = fSplineData.xMinValues;
		final double[] oldXMaxValues = fSplineData.xMaxValues;

		final int newLength = oldIsPointMovable.length - 1;

		final boolean[] newIsPointMovable = fSplineData.isPointMovable = new boolean[newLength];
		final float[] newPosX = fSplineData.relativePositionX = new float[newLength];
		final float[] newPosY = fSplineData.relativePositionY = new float[newLength];
		final double[] newXValues = fSplineData.xValues = new double[newLength];
		final double[] newYValues = fSplineData.yValues = new double[newLength];
		final double[] newXMinValues = fSplineData.xMinValues = new double[newLength];
		final double[] newXMaxValues = fSplineData.xMaxValues = new double[newLength];

		int srcPos, destPos, length;

		if (fPointHitIndex == 0) {

			// remove first point

			srcPos = 1;
			destPos = 0;
			length = newLength;

			System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
			System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
			System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

			System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
			System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
			System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
			System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

		} else if (fPointHitIndex == newLength) {

			// remove last point

			srcPos = 0;
			destPos = 0;
			length = newLength;

			System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
			System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
			System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

			System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
			System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
			System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
			System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

		} else {

			// remove points in the middle

			srcPos = 0;
			destPos = 0;
			length = fPointHitIndex;

			System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
			System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
			System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

			System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
			System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
			System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
			System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);

			srcPos = fPointHitIndex + 1;
			destPos = fPointHitIndex;
			length = newLength - fPointHitIndex;

			System.arraycopy(oldIsPointMovable, srcPos, newIsPointMovable, destPos, length);
			System.arraycopy(oldPosX, srcPos, newPosX, destPos, length);
			System.arraycopy(oldPosY, srcPos, newPosY, destPos, length);

			System.arraycopy(oldXValues, srcPos, newXValues, destPos, length);
			System.arraycopy(oldYValues, srcPos, newYValues, destPos, length);
			System.arraycopy(oldXMinValues, srcPos, newXMinValues, destPos, length);
			System.arraycopy(oldXMaxValues, srcPos, newXMaxValues, destPos, length);
		}
	}

	private boolean computeNewPoint(final int mouseDownDevPositionX, final int mouseDownDevPositionY) {

		final SplineDrawingData drawingData = fChartLayer2ndAltiSerie.getDrawingData();

		final float scaleX = drawingData.scaleX;
		final float scaleY = drawingData.scaleY;

		final float devX = drawingData.devGraphValueXOffset + mouseDownDevPositionX;
		final float devY = drawingData.devY0Spline - mouseDownDevPositionY;

		final int graphXMin = 0;
		final int graphXMax = fSliderXAxisValue;

		final float graphX = devX / scaleX;

		// check min/max value
		if (graphX <= graphXMin || graphX >= graphXMax) {
			// click is outside of the allowed area
			return false;
		}

		/*
		 * add the new point at the end of the existing points, CubicSpline will resort them
		 */
		final boolean[] oldIsPointMovable = fSplineData.isPointMovable;
		final float[] oldPosX = fSplineData.relativePositionX;
		final float[] oldPosY = fSplineData.relativePositionY;
		final double[] oldXValues = fSplineData.xValues;
		final double[] oldYValues = fSplineData.yValues;
		final double[] oldXMinValues = fSplineData.xMinValues;
		final double[] oldXMaxValues = fSplineData.xMaxValues;

		final int newLength = oldXValues.length + 1;

		final boolean[] newIsPointMovable = fSplineData.isPointMovable = new boolean[newLength];
		final float[] newPosX = fSplineData.relativePositionX = new float[newLength];
		final float[] newPosY = fSplineData.relativePositionY = new float[newLength];
		final double[] newXValues = fSplineData.xValues = new double[newLength];
		final double[] newYValues = fSplineData.yValues = new double[newLength];
		final double[] newXMinValues = fSplineData.xMinValues = new double[newLength];
		final double[] newXMaxValues = fSplineData.xMaxValues = new double[newLength];

		final int lastIndex = newLength - 1;

		// copy old values into new arrays
		System.arraycopy(oldIsPointMovable, 0, newIsPointMovable, 0, lastIndex);
		System.arraycopy(oldPosX, 0, newPosX, 0, lastIndex);
		System.arraycopy(oldPosY, 0, newPosY, 0, lastIndex);

		System.arraycopy(oldXValues, 0, newXValues, 0, lastIndex);
		System.arraycopy(oldYValues, 0, newYValues, 0, lastIndex);
		System.arraycopy(oldXMinValues, 0, newXMinValues, 0, lastIndex);
		System.arraycopy(oldXMaxValues, 0, newXMaxValues, 0, lastIndex);

		/*
		 * creat a new point
		 */
		final float dev1X = graphXMax * scaleX;
		final float dev1Y = fAltiDiff * scaleY;

		final float posX = dev1X == 0 ? 0 : devX / dev1X;
		final float posY = dev1Y == 0 ? 0 : devY / dev1Y;

		newIsPointMovable[lastIndex] = true;
		newPosX[lastIndex] = posX;
		newPosY[lastIndex] = posY;
		newXValues[lastIndex] = graphX;
		newYValues[lastIndex] = 0;
		newXMinValues[lastIndex] = graphXMin;
		newXMaxValues[lastIndex] = graphXMax;

		// don't move the point immediately
		fPointHitIndex = -1;

		return true;
	}

	private void computePointMoveValues(final ChartMouseEvent mouseEvent) {

		if (fPointHitIndex == -1) {
			return;
		}

		final SplineDrawingData drawingData = fChartLayer2ndAltiSerie.getDrawingData();
		final float scaleX = drawingData.scaleX;
		final float scaleY = drawingData.scaleY;

		float devX = drawingData.devGraphValueXOffset + mouseEvent.devXMouse;
		final float devY = drawingData.devY0Spline - mouseEvent.devYMouse;

		final float[] posX = fSplineData.relativePositionX;
		final float[] posY = fSplineData.relativePositionY;

		final double graphXMin = fSplineData.xMinValues[fPointHitIndex];
		final double graphXMax = fSplineData.xMaxValues[fPointHitIndex];

		float graphX = devX / scaleX;

		fCanDeletePoint = false;

		// check min value
		if (graphXMin != Double.NaN) {
			if (graphX < graphXMin) {
				graphX = (float) graphXMin;
				fCanDeletePoint = true;
			}
		}
		// check max value
		if (graphXMax != Double.NaN) {
			if (graphX > graphXMax) {
				graphX = (float) graphXMax;
				fCanDeletePoint = true;
			}
		}

		devX = graphX * scaleX;

		final int graph1X = fSliderXAxisValue;
		final int graph1Y = fAltiDiff;

		final float dev1X = graph1X * scaleX;
		final float dev1Y = graph1Y * scaleY;

		posX[fPointHitIndex] = devX / dev1X;
		posY[fPointHitIndex] = devY / dev1Y;
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.adjust_altitude_dlg_shell_title);
	}

	@Override
	public void create() {

		createDataBackup();

		// create UI widgets
		super.create();

		restoreState();

		setTitle(Messages.adjust_altitude_dlg_dialog_title);
		setMessage(NLS.bind(Messages.adjust_altitude_dlg_dialog_message, TourManager.getTourTitle(fTourData)));

		updateTourChart();
	}

	public ChartLayer2ndAltiSerie create2ndAltiLayer() {

		final int[] xDataSerie = fTourChartConfig.showTimeOnXAxis ? fTourData.timeSerie : fTourData.getDistanceSerie();

		fChartLayer2ndAltiSerie = new ChartLayer2ndAltiSerie(fTourData, xDataSerie, fTourChartConfig);

		return fChartLayer2ndAltiSerie;
	}

	/**
	 * Create altitude spinner field
	 * 
	 * @param startContainer
	 * @return Returns the field
	 */
	private Spinner createAltiField(final Composite startContainer) {

		final Spinner spinner = new Spinner(startContainer, SWT.BORDER);
		spinner.setMinimum(0);
		spinner.setMaximum(99999);
		spinner.setIncrement(1);
		spinner.setPageIncrement(1);
		UI.setWidth(spinner, convertWidthInCharsToPixels(6));

		spinner.addModifyListener(new ModifyListener() {

			public void modifyText(final ModifyEvent e) {

				if (fIsModifiedInternal) {
					return;
				}

				final Spinner spinner = (Spinner) e.widget;

				if (UI.UNIT_VALUE_ALTITUDE == 1) {

					final int modifiedAlti = spinner.getSelection();
//					int metricAlti = (Integer) spinner.getData(WIDGET_DATA_METRIC_ALTITUDE);
//					
//					final float oldAlti = metricAlti / UI.UNIT_VALUE_ALTITUDE;
//					int newMetricAlti = (int) (modifiedAlti * UI.UNIT_VALUE_ALTITUDE);
//					
//					if (modifiedAlti > oldAlti) {
//						newMetricAlti++;
//					}

					spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, modifiedAlti);

				} else {

					/**
					 * adjust the non metric (imperial) value, this seems to be complicate and it is
					 * <p>
					 * the altitude data are always saved in the database with the metric system
					 * therefor the altitude must always match to the metric system, changing the
					 * altitude in the imperial system has always 3 or 4 value differences from one
					 * meter to the next meter
					 * <p>
					 * after many hours of investigation this seems to work
					 */

					final int modifiedAlti = spinner.getSelection();
					final int metricAlti = (Integer) spinner.getData(WIDGET_DATA_METRIC_ALTITUDE);

					final float oldAlti = metricAlti / UI.UNIT_VALUE_ALTITUDE;
					int newMetricAlti = (int) (modifiedAlti * UI.UNIT_VALUE_ALTITUDE);

					if (modifiedAlti > oldAlti) {
						newMetricAlti++;
					}

					spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, newMetricAlti);
				}

				onChangeAltitude((Integer) e.widget.getData(WIDGET_DATA_ALTI_ID));
			}
		});

		spinner.addMouseWheelListener(new MouseWheelListener() {

			public void mouseScrolled(final MouseEvent e) {

				if (fIsModifiedInternal) {
					return;
				}

				final Spinner spinner = (Spinner) e.widget;

				int accelerator = (e.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
				accelerator *= (e.stateMask & SWT.SHIFT) != 0 ? 5 : 1;
				accelerator *= e.count > 0 ? 1 : -1;

				int metricAltitude = (Integer) e.widget.getData(WIDGET_DATA_METRIC_ALTITUDE);
				metricAltitude = metricAltitude + accelerator;

				fIsModifiedInternal = true;
				{
					spinner.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(metricAltitude));
					spinner.setSelection((int) (metricAltitude / UI.UNIT_VALUE_ALTITUDE));
				}
				fIsModifiedInternal = false;

				onChangeAltitude((Integer) e.widget.getData(WIDGET_DATA_ALTI_ID));
			}
		});

		spinner.addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {}

			public void focusLost(final FocusEvent e) {
				onChangeAltitude((Integer) e.widget.getData(WIDGET_DATA_ALTI_ID));
			}
		});

		return spinner;
	}

	private void createDataBackup() {

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		fBackupAltitudeSerie = Util.createDataSerieBackup(fTourData.altitudeSerie);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite container = (Composite) super.createDialogArea(parent);

		fDlgContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fDlgContainer);
		GridLayoutFactory.fillDefaults().margins(10, 0).applyTo(fDlgContainer);

		createUI(fDlgContainer);

		initializeSplineData();

		return container;
	}

	private void createUI(final Composite parent) {

		createUIAdjustmentType(parent);
		createUITourChart(parent);

		/*
		 * create options for each adjustment type in a pagebook
		 */
		fPageBookOptions = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fPageBookOptions);

		fPageEmpty = new Label(fPageBookOptions, SWT.NONE);
		fPageOptionSRTM = createUIOptionSRTM(fPageBookOptions);
		fPageOptionNoSRTM = createUIOptionNoSRTM(fPageBookOptions);
	}

	private void createUIAdjustmentType(final Composite parent) {

		/*
		 * combo: adjust type
		 */
		final Composite typeContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(typeContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 5, 0).applyTo(typeContainer);

		final Label label = new Label(typeContainer, SWT.NONE);
		label.setText(Messages.adjust_altitude_label_adjustment_type);

		fComboAdjustmentType = new Combo(typeContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboAdjustmentType.setVisibleItemCount(20);
		fComboAdjustmentType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeAdjustType();
			}
		});

		// fill combo
		for (final AdjustmentType adjustType : fAllAdjustmentTypes) {

			if (adjustType.id == ADJUST_TYPE_UNTIL_LEFT_SLIDER && fSrtmMetricValues == null) {
				// skip this type it requires srtm data
				continue;
			}

			fAvailableAdjustmentTypes.add(adjustType);

			fComboAdjustmentType.add(adjustType.visibleName);
		}
	}

	private Composite createUIOptionNoSRTM(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

		/*
		 * field: start altitude
		 */
		final Composite startContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.FILL).applyTo(startContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(startContainer);

		label = new Label(startContainer, SWT.NONE);
		label.setText(Messages.Dlg_AdjustAltitude_Label_start_altitude);
		label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

		fSpinnerNewStartAlti = createAltiField(startContainer);
		fSpinnerNewStartAlti.setData(WIDGET_DATA_ALTI_ID, new Integer(ALTI_ID_START));
		fSpinnerNewStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_start_altitude_tooltip);

		fLblOldStartAlti = new Label(startContainer, SWT.NONE);
		fLblOldStartAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

		/*
		 * field: max altitude
		 */
		final Composite maxContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, false).applyTo(maxContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(maxContainer);
		{
			label = new Label(maxContainer, SWT.NONE);
			label.setText(Messages.Dlg_AdjustAltitude_Label_max_altitude);
			label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

			fSpinnerNewMaxAlti = createAltiField(maxContainer);
			fSpinnerNewMaxAlti.setData(WIDGET_DATA_ALTI_ID, new Integer(ALTI_ID_MAX));
			fSpinnerNewMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_max_altitude_tooltip);

			fLblOldMaxAlti = new Label(maxContainer, SWT.NONE);
			fLblOldMaxAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

			/*
			 * group: keep start/bottom
			 */
			final Group groupKeep = new Group(maxContainer, SWT.NONE);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(groupKeep);
			GridLayoutFactory.swtDefaults().applyTo(groupKeep);
			groupKeep.setText(Messages.Dlg_AdjustAltitude_Group_options);
			{
				final SelectionAdapter keepButtonSelectionAdapter = new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeAltitude(new Integer(ALTI_ID_MAX));
					}
				};

				fRadioKeepBottom = new Button(groupKeep, SWT.RADIO);
				fRadioKeepBottom.setText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude);
				fRadioKeepBottom.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_bottom_altitude_tooltip);
				fRadioKeepBottom.setLayoutData(new GridData());
				fRadioKeepBottom.addSelectionListener(keepButtonSelectionAdapter);
				// fRadioKeepBottom.setSelection(true);

				fRadioKeepStart = new Button(groupKeep, SWT.RADIO);
				fRadioKeepStart.setText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude);
				fRadioKeepStart.setToolTipText(Messages.Dlg_AdjustAltitude_Radio_keep_start_altitude_tooltip);
				fRadioKeepStart.setLayoutData(new GridData());
				fRadioKeepStart.addSelectionListener(keepButtonSelectionAdapter);
			}
		}

		/*
		 * field: end altitude
		 */
		final Composite endContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(true, false).applyTo(endContainer);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(endContainer);

		label = new Label(endContainer, SWT.NONE);
		label.setText(Messages.Dlg_AdjustAltitude_Label_end_altitude);
		label.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

		fSpinnerNewEndAlti = createAltiField(endContainer);
		fSpinnerNewEndAlti.setData(WIDGET_DATA_ALTI_ID, new Integer(ALTI_ID_END));
		fSpinnerNewEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_end_altitude_tooltip);

		fLblOldEndAlti = new Label(endContainer, SWT.NONE);
		fLblOldEndAlti.setToolTipText(Messages.Dlg_AdjustAltitude_Label_original_values);

		return container;
	}

	private Composite createUIOptionSRTM(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

		fBtnRemoveAllPoints = new Button(container, SWT.NONE);
		fBtnRemoveAllPoints.setText(Messages.adjust_altitude_btn_srtm_remove_all_points);
		fBtnRemoveAllPoints.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				initializeSplineData();
				onChangeAdjustType();
			}
		});

		return container;
	}

	private void createUITourChart(final Composite parent) {

		fTourChart = new TourChart(parent, SWT.BORDER, true);
		GridDataFactory.fillDefaults().grab(true, true).indent(0, 0).minSize(300, 200).applyTo(fTourChart);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		fTourChart.setContextProvider(new DialogAdjustAltitudeChartContextProvicer(this), true);

		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {
				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfo) {

				if (fIsChartUpdated) {
					return;
				}

				onChangeAdjustType();
			}
		});

		fTourChart.addMouseListener(new IMouseListener() {

			public void mouseDoubleClick(final ChartMouseEvent event) {}

			public void mouseDownPost(final ChartMouseEvent event) {}

			public void mouseDownPre(final ChartMouseEvent event) {
				onMouseDownPre(event);
			}

			public void mouseMove(final ChartMouseEvent event) {
				onMouseMove(event);
			}

			public void mouseUp(final ChartMouseEvent event) {
				onMouseUp(event);
			}
		});

		fTourChart.addXAxisSelectionListener(new IXAxisSelectionListener() {
			public void selectionChanged(final boolean showTimeOnXAxis) {
				onChangeXAxis();
			}
		});

		/*
		 * create chart configuration
		 */
		fTourChartConfig = new TourChartConfiguration(true);

		// set altitude visible
		fTourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);

		// show srtm values 
		fTourChartConfig.isSRTMDataVisible = true;

		// overwrite x-axis from pref store
		fTourChartConfig.setIsShowTimeOnXAxis(fPrefStore.getString(ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT)
				.equals(TourManager.X_AXIS_TIME));
	}

	private void enableActions() {

		/*
		 * srtm options
		 */
		if (fSplineData != null && fSplineData.isPointMovable != null) {
			fBtnRemoveAllPoints.setEnabled(fSplineData.isPointMovable.length > 3);
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	/**
	 * @return the adjustment type which is selected in the combox
	 */
	private AdjustmentType getSelectedAdjustmentType() {

		int comboIndex = fComboAdjustmentType.getSelectionIndex();

		if (comboIndex == -1) {
			comboIndex = 0;
			fComboAdjustmentType.select(comboIndex);
		}

		return fAvailableAdjustmentTypes.get(comboIndex);
	}

	/**
	 * create spline values
	 * 
	 * @param altiDiff
	 * @param sliderDistance
	 * @return
	 */
	private void initializeSplineData() {

		fSplineData = fTourData.splineDataPoints = new SplineData();

		final int pointLength = 3;

		final boolean[] isMovable = fSplineData.isPointMovable = new boolean[pointLength];
		isMovable[0] = false;
		isMovable[1] = true;
		isMovable[2] = false;

		final float[] posX = fSplineData.relativePositionX = new float[pointLength];
		final float[] posY = fSplineData.relativePositionY = new float[pointLength];
		posX[0] = -0.00001f;
		posY[0] = 0;
		posX[1] = 0.5f;
		posY[1] = 0;
		posX[2] = 1.00001f;
		posY[2] = 0;

		final double[] splineMinX = fSplineData.xMinValues = new double[pointLength];
		final double[] splineMaxX = fSplineData.xMaxValues = new double[pointLength];
		splineMinX[0] = Double.NaN;
		splineMaxX[0] = Double.NaN;
		splineMinX[1] = 0;
		splineMaxX[1] = 0;
		splineMinX[2] = Double.NaN;
		splineMaxX[2] = Double.NaN;

		fSplineData.xValues = new double[pointLength];
		fSplineData.yValues = new double[pointLength];
	}

	boolean isActionCreateSplinePointEnabled(final int mouseDownDevPositionX, final int mouseDownDevPositionY) {

		final SplineDrawingData drawingData = fChartLayer2ndAltiSerie.getDrawingData();

		final float scaleX = drawingData.scaleX;
		final float devX = drawingData.devGraphValueXOffset + mouseDownDevPositionX;
		final float graphX = devX / scaleX;

		final int graphXMin = 0;
		final int graphXMax = fSliderXAxisValue;

		// check min/max value
		if (graphX <= graphXMin || graphX >= graphXMax) {
			// click is outside of the allowed area
			return false;
		} else {
			return true;
		}
	}

	@Override
	protected void okPressed() {

		saveTour();

		super.okPressed();
	}

	private void onChangeAdjustType() {

		// hide all 2nd data series
		fTourData.dataSerieAdjustedAlti = null;
		fTourData.dataSerieDiffTo2ndAlti = null;
		fTourData.dataSerie2ndAlti = null;
		fTourData.dataSerieSpline = null;

		final AdjustmentType selectedAdjustType = getSelectedAdjustmentType();

		switch (selectedAdjustType.id) {
		case ADJUST_TYPE_UNTIL_LEFT_SLIDER:
			fPageBookOptions.showPage(fPageOptionSRTM);
			computeAltitudeUntilLeftSlider();
			break;

		case ADJUST_TYPE_WHOLE_TOUR:
		case ADJUST_TYPE_START_AND_END:
		case ADJUST_TYPE_END:
		case ADJUST_TYPE_MAX_HEIGHT:
			fPageBookOptions.showPage(fPageOptionNoSRTM);
			resetAltitude();
			break;

		default:
			fPageBookOptions.showPage(fPageEmpty);
			break;
		}

		fDlgContainer.layout(true);

		updateUI2ndLayer();
	}

	private void onChangeAltitude(final Integer altiFlag) {

		// calcuate new altitude values
		adjustAltitude(altiFlag);

		// set new values into the fields which can change the altitude
		updateUIAltiFields();

		updateUI2ndLayer();
	}

	private void onChangeXAxis() {

		if (getSelectedAdjustmentType().id == ADJUST_TYPE_UNTIL_LEFT_SLIDER) {
			computeAltitudeUntilLeftSlider();
		}
	}

	private void onMouseDownPre(final ChartMouseEvent mouseEvent) {

		if (fChartLayer2ndAltiSerie == null) {
			return;
		}

		final Rectangle[] pointHitRectangles = fChartLayer2ndAltiSerie.getPointHitRectangels();
		if (pointHitRectangles == null) {
			return;
		}

		fPointHitIndex = -1;
		final boolean[] isPointMovable = fSplineData.isPointMovable;

		// check if the mouse hits a spline point
		for (int pointIndex = 0; pointIndex < pointHitRectangles.length; pointIndex++) {

			if (isPointMovable[pointIndex] == false) {
				// ignore none movable points
				continue;
			}

			if (pointHitRectangles[pointIndex].contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

				fPointHitIndex = pointIndex;

				mouseEvent.isWorked = true;
				return;
			}
		}
	}

	private void onMouseMove(final ChartMouseEvent mouseEvent) {

		if (fChartLayer2ndAltiSerie == null) {
			return;
		}

		final Rectangle[] pointHitRectangles = fChartLayer2ndAltiSerie.getPointHitRectangels();
		if (pointHitRectangles == null) {
			return;
		}

		if (fPointHitIndex != -1) {

			// point is moved

			computePointMoveValues(mouseEvent);

			onChangeAdjustType();

			mouseEvent.isWorked = true;

		} else {

			// point is not moved, check if the mouse hits a spline point

			final boolean[] isPointMovable = fSplineData.isPointMovable;
			for (int pointIndex = 0; pointIndex < pointHitRectangles.length; pointIndex++) {

				if (isPointMovable[pointIndex] == false) {
					// ignore none movable points
					continue;
				}

				if (pointHitRectangles[pointIndex].contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {
					mouseEvent.isWorked = true;
					break;
				}
			}
		}
	}

	private void onMouseUp(final ChartMouseEvent mouseEvent) {

		if (fPointHitIndex == -1) {
			return;
		}

		if (fCanDeletePoint) {

			fCanDeletePoint = false;

			computeDeletedPoint();

			// redraw layer to update the hit rectangles
			onChangeAdjustType();
		}

		mouseEvent.isWorked = true;
		fPointHitIndex = -1;
	}

	/**
	 * reset altitudes to it's original values
	 */
	private void resetAltitude() {

		final int[] altitudeSerie = fTourData.altitudeSerie;
		final int startAlti = altitudeSerie[0];
		final int endAlti = altitudeSerie[altitudeSerie.length - 1];

		// calculate max altitude
		int maxAlti = startAlti;
		for (final int altitude : altitudeSerie) {
			if (altitude > maxAlti) {
				maxAlti = altitude;
			}
		}

		fOldStartAlti = startAlti;

		fLblOldStartAlti.setText(Integer.toString((int) (startAlti / UI.UNIT_VALUE_ALTITUDE)));
		fLblOldStartAlti.pack(true);

		fLblOldEndAlti.setText(Integer.toString((int) (endAlti / UI.UNIT_VALUE_ALTITUDE)));
		fLblOldEndAlti.pack(true);

		fLblOldMaxAlti.setText(Integer.toString((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE)));
		fLblOldMaxAlti.pack(true);

		fIsModifiedInternal = true;
		{
			fSpinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(startAlti));
			fSpinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ALTITUDE));

			fSpinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(endAlti));
			fSpinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ALTITUDE));

			fSpinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(maxAlti));
			fSpinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE));
		}
		fIsModifiedInternal = false;

	}

	/**
	 * Restore values which have been modified in the dialog
	 * 
	 * @param selectedTour
	 */
	private void restoreDataBackup() {

		fTourData.altitudeSerie = fBackupAltitudeSerie;
	}

//	/**
//	 * copy the old altitude values back into the tourdata altitude serie
//	 */
//	public void restoreOriginalAltitudeValues() {
//
//		final int[] altitudeSerie = fTourData.altitudeSerie;
//
//		if (altitudeSerie == null | fBackupAltitudeSerie == null) {
//			return;
//		}
//
//		for (int altiIndex = 0; altiIndex < altitudeSerie.length; altiIndex++) {
//			altitudeSerie[altiIndex] = fBackupAltitudeSerie[altiIndex];
//		}
//
//		// recompute imperial altitude values
//		fTourData.clearAltitudeSeries();
//	}

	private void restoreState() {

		// get previous selected adjustment type, use first type if not found
		final int prefAdjustType = fPrefStore.getInt(PREF_ADJUST_TYPE);
		int comboIndex = 0;
		int typeIndex = 0;
		for (final AdjustmentType availAdjustType : fAvailableAdjustmentTypes) {
			if (prefAdjustType == availAdjustType.id) {
				comboIndex = typeIndex;
				break;
			}
			typeIndex++;
		}

		fComboAdjustmentType.select(comboIndex);
	}

	private void saveState() {

		fPrefStore.setValue(PREF_ADJUST_TYPE, getSelectedAdjustmentType().id);

		fPrefStore.setValue(ITourbookPreferences.ADJUST_ALTITUDE_CHART_X_AXIS_UNIT, fTourChartConfig.showTimeOnXAxis
				? TourManager.X_AXIS_TIME
				: TourManager.X_AXIS_DISTANCE);
	}

	private void saveTour() {

		fIsTourSaved = true;
	}

	private CubicSpline updateSplineData() {

		final double[] splineX = fSplineData.xValues;
		final double[] splineY = fSplineData.yValues;

		final double[] splineMinX = fSplineData.xMinValues;
		final double[] splineMaxX = fSplineData.xMaxValues;

		final float[] posX = fSplineData.relativePositionX;
		final float[] posY = fSplineData.relativePositionY;

		final boolean[] isMovable = fSplineData.isPointMovable;

		for (int pointIndex = 0; pointIndex < isMovable.length; pointIndex++) {

			splineX[pointIndex] = posX[pointIndex] * fSliderXAxisValue;
			splineY[pointIndex] = posY[pointIndex] * fAltiDiff;

			splineMinX[pointIndex] = 0;
			splineMaxX[pointIndex] = fSliderXAxisValue;
		}

		return new CubicSpline(splineX, splineY);
	}

	private void updateTourChart() {

		fIsChartUpdated = true;

		fTourChart.updateTourChart(fTourData, fTourChartConfig, true);

		fIsChartUpdated = false;
	}

	private void updateUI2ndLayer() {
		enableActions();
		fTourChart.update2ndAltiLayer(this, true);
	}

	/**
	 * set the altitude fields with the current altitude values
	 */
	private void updateUIAltiFields() {

		final int[] altiSerie = fTourData.altitudeSerie;

		final int startAlti = altiSerie[0];
		final int endAlti = altiSerie[altiSerie.length - 1];

		// get max altitude
		int maxAlti = altiSerie[0];
		for (final int altitude : altiSerie) {
			if (altitude > maxAlti) {
				maxAlti = altitude;
			}
		}

		// keep values
		fOldAltiInputStart = startAlti;
		fOldAltiInputMax = maxAlti;

		fSpinnerNewStartAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(startAlti));
		fSpinnerNewEndAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(endAlti));
		fSpinnerNewMaxAlti.setData(WIDGET_DATA_METRIC_ALTITUDE, new Integer(maxAlti));

		/*
		 * prevent to fire the selection event in the spinner when a selection is set, this would
		 * cause endless loops
		 */
		fIsModifiedInternal = true;
		{
			fSpinnerNewStartAlti.setSelection((int) (startAlti / UI.UNIT_VALUE_ALTITUDE));
			fSpinnerNewEndAlti.setSelection((int) (endAlti / UI.UNIT_VALUE_ALTITUDE));
			fSpinnerNewMaxAlti.setSelection((int) (maxAlti / UI.UNIT_VALUE_ALTITUDE));
		}
		fIsModifiedInternal = false;

		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}

}
