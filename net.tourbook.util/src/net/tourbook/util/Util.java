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
package net.tourbook.util;

import java.util.Calendar;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class Util {

	private static final double	EARTH_RADIUS	= 6371;										// km

	// WGS-84 Ellipsoid
	private static final double	HALBACHSE_A		= 6378.137;
	private static final double	HALBACHSE_B		= 6356.7523142;

	// = 1/298.2572229328709613   1/298.257223563 // ca. (A-B)/A
	private static final double	ABPLATTUNG_F	= (HALBACHSE_A - HALBACHSE_B) / HALBACHSE_A;

	//http://www.kowoma.de/gps/geo/mapdatum/mapdatums.php
	//EUROPEAN 1950/1979, Western Europe
	//static final double HALBACHSE_A = 6378.388;
	//static final double HALBACHSE_B = 6356.911946;
	//static final double ABPLATTUNG_F = 1/297....;

	public static int adjustScaleValueOnMouseScroll(final MouseEvent event) {

		// accelerate with Ctrl + Shift key
		int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

		final Scale scale = (Scale) event.widget;
		final int newValueDiff = event.count > 0 ? accelerator : -accelerator;

		return scale.getSelection() + newValueDiff;
	}

	public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event) {

		// accelerate with Ctrl + Shift key
		int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

		final Spinner spinner = (Spinner) event.widget;
		final int newValue = ((event.count > 0 ? 1 : -1) * accelerator);

		spinner.setSelection(spinner.getSelection() + newValue);
	}

	/**
	 * @param sourceString
	 * @param lookFor
	 * @return Returns the number of characters which are found in the string or -1 when the
	 *         string is <code>null</code>
	 */
	public static int countCharacter(final String sourceString, final char lookFor) {

		if (sourceString == null) {
			return -1;
		}

		int count = 0;

		for (int i = 0; i < sourceString.length(); i++) {
			final char c = sourceString.charAt(i);
			if (c == lookFor) {
				count++;
			}
		}

		return count;
	}

	/**
	 * creates a int array backup
	 * 
	 * @param original
	 * @return the backup array or <code>null</code> when the original data is <code>null</code>
	 */
	public static int[] createDataSerieCopy(final int[] original) {

		int[] backup = null;

		if (original != null) {
			final int serieLength = original.length;
			backup = new int[serieLength];
			System.arraycopy(original, 0, backup, 0, serieLength);
		}

		return backup;
	}

	public static Resource disposeResource(final Resource resource) {
		if (resource != null && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	/**
	 * Haversine Formula to calculate distance between 2 geo points
	 * <p>
	 * <a href="http://en.wikipedia.org/wiki/Haversine_formula"
	 * >http://en.wikipedia.org/wiki/Haversine_formula</a>
	 */
	public static double distanceHaversine(final double lat1, final double lon1, final double lat2, final double lon2) {

		if (lat1 == lat2 && lon1 == lon2) {
			return 0;
		}

		final double dLat = Math.toRadians(lat2 - lat1);
		final double dLon = Math.toRadians(lon2 - lon1);

		final double a = (Math.sin(dLat / 2))
				* (Math.sin(dLat / 2))
				+ (Math.cos(lat1) * Math.cos(lat2) * (Math.sin(dLon / 2)))
				* (Math.cos(lat1) * Math.cos(lat2) * (Math.sin(dLon / 2)));

		final double c = 2 * Math.asin(Math.min(1.0, Math.sqrt(a)));
		final double km = EARTH_RADIUS * c;

		return km * 1000;
	}

	/**
	 * Calculates geodetic distance between two points specified by latitude/longitude using
	 * Vincenty inverse formula for ellipsoids
	 * <p>
	 * <p>
	 * Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2010
	 * <p>
	 * from: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on
	 * the Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975 *
	 * <p>
	 * http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
	 * <p>
	 * javascript source location: http://www.movable-type.co.uk/scripts/latlong-vincenty.html
	 * 
	 * @param {Number} lat1, lon1: first point in decimal degrees
	 * @param {Number} lat2, lon2: second point in decimal degrees
	 * @returns (Number} distance in metres between points
	 */
	public static double distanceVincenty(final double lat1, final double lon1, final double lat2, final double lon2) {

		final double L = Math.toRadians(lon2 - lon1);
		final double U1 = Math.atan((1 - ABPLATTUNG_F) * Math.tan(Math.toRadians(lat1)));
		final double U2 = Math.atan((1 - ABPLATTUNG_F) * Math.tan(Math.toRadians(lat2)));
		final double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
		final double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

		double lambda = L, lambdaP = 2 * Math.PI;
		int iterLimit = 20;
		double cosSqAlpha = 0, sinSigma = 0, cos2SigmaM = 0, cosSigma = 0, sigma = 0;

		while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0) {

			final double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);

			sinSigma = Math.sqrt((cosU2 * sinLambda)
					* (cosU2 * sinLambda)
					+ (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
					* (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));

			if (sinSigma == 0) {
				return 0; // co-incident points
			}

			cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
			sigma = Math.atan2(sinSigma, cosSigma);
			final double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
			cosSqAlpha = 1 - sinAlpha * sinAlpha;

			if (cosSqAlpha == 0) {
				return Math.abs(HALBACHSE_A * L); // two points on equator  // neu 20.04.2006
			}

			cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha; // nach Erg. auf Website
			// cos2SigmaM = cosSigma;
			// if (cosSqAlpha != 0) cos2SigmaM -= 2*sinU1*sinU2/cosSqAlpha; // Abfrage auf 0 neu 27.02.2005

			final double C = ABPLATTUNG_F / 16 * cosSqAlpha * (4 + ABPLATTUNG_F * (4 - 3 * cosSqAlpha));
			lambdaP = lambda;

			lambda = L
					+ (1 - C)
					* ABPLATTUNG_F
					* sinAlpha
					* (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
		}

		if (iterLimit == 0) {
			return 0; // formula failed to converge
		}

		final double uSq = cosSqAlpha
				* (HALBACHSE_A * HALBACHSE_A - HALBACHSE_B * HALBACHSE_B)
				/ (HALBACHSE_B * HALBACHSE_B);

		final double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
		final double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

		final double deltaSigma = B
				* sinSigma
				* (cos2SigmaM + B
						/ 4
						* (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B
								/ 6
								* cos2SigmaM
								* (-3 + 4 * sinSigma * sinSigma)
								* (-3 + 4 * cos2SigmaM * cos2SigmaM)));

		final double s = HALBACHSE_B * A * (sigma - deltaSigma);

		return s * 1000;
	}

	/*
	 * !!! ORIGINAL CODE !!!
	 */
//	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
//	/* Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2010             */
//	/*                                                                                                */
//	/* from: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the */
//	/*       Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975    */
//	/*       http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf                                             */
//	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
//
//	/**
//	 * Calculates geodetic distance between two points specified by latitude/longitude using
//	 * Vincenty inverse formula for ellipsoids
//	 *
//	 * @param   {Number} lat1, lon1: first point in decimal degrees
//	 * @param   {Number} lat2, lon2: second point in decimal degrees
//	 * @returns (Number} distance in metres between points
//	 */
//	function distVincenty(lat1, lon1, lat2, lon2) {
//	  var a = 6378137, b = 6356752.3142,  f = 1/298.257223563;  // WGS-84 ellipsoid params
//	  var L = (lon2-lon1).toRad();
//	  var U1 = Math.atan((1-f) * Math.tan(lat1.toRad()));
//	  var U2 = Math.atan((1-f) * Math.tan(lat2.toRad()));
//	  var sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
//	  var sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);
//
//	  var lambda = L, lambdaP, iterLimit = 100;
//	  do {
//	    var sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);
//	    var sinSigma = Math.sqrt((cosU2*sinLambda) * (cosU2*sinLambda) +
//	      (cosU1*sinU2-sinU1*cosU2*cosLambda) * (cosU1*sinU2-sinU1*cosU2*cosLambda));
//	    if (sinSigma==0) return 0;  // co-incident points
//	    var cosSigma = sinU1*sinU2 + cosU1*cosU2*cosLambda;
//	    var sigma = Math.atan2(sinSigma, cosSigma);
//	    var sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
//	    var cosSqAlpha = 1 - sinAlpha*sinAlpha;
//	    var cos2SigmaM = cosSigma - 2*sinU1*sinU2/cosSqAlpha;
//	    if (isNaN(cos2SigmaM)) cos2SigmaM = 0;  // equatorial line: cosSqAlpha=0 (�6)
//	    var C = f/16*cosSqAlpha*(4+f*(4-3*cosSqAlpha));
//	    lambdaP = lambda;
//	    lambda = L + (1-C) * f * sinAlpha *
//	      (sigma + C*sinSigma*(cos2SigmaM+C*cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)));
//	  } while (Math.abs(lambda-lambdaP) > 1e-12 && --iterLimit>0);
//
//	  if (iterLimit==0) return NaN  // formula failed to converge
//
//	  var uSq = cosSqAlpha * (a*a - b*b) / (b*b);
//	  var A = 1 + uSq/16384*(4096+uSq*(-768+uSq*(320-175*uSq)));
//	  var B = uSq/1024 * (256+uSq*(-128+uSq*(74-47*uSq)));
//	  var deltaSigma = B*sinSigma*(cos2SigmaM+B/4*(cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)-
//	    B/6*cos2SigmaM*(-3+4*sinSigma*sinSigma)*(-3+4*cos2SigmaM*cos2SigmaM)));
//	  var s = b*A*(sigma-deltaSigma);
//
//	  s = s.toFixed(3); // round to 1mm precision
//	  return s;
//	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a boolean value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static boolean getStateBoolean(final IDialogSettings state, final String key, final boolean defaultValue) {
		return state.get(key) == null ? defaultValue : state.getBoolean(key);
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a float value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static double getStateDouble(final IDialogSettings state, final String key, final double defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getDouble(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

//	/**
//	 * @param combo
//	 *            combo box, the items in the combo box
//	 *            must correspond to the items in the states array
//	 * @param states
//	 *            array which contains all states
//	 * @return Returns the state which is selected or null when nothing is selected
//	 */
//	public static String getStateFromCombo(final Combo combo, final String[] states) {
//
//		final int selectedIndex = combo.getSelectionIndex();
//		if (selectedIndex == -1 || selectedIndex >= states.length) {
//			return null;
//		} else {
//			return states[selectedIndex];
//		}
//	}

	/**
	 * @param combo
	 *            combo box, the items in the combo box
	 *            must correspond to the items in the states array
	 * @param states
	 *            array which contains all states
	 * @param defaultState
	 *            state when an item is not selected in the combo box
	 * @return Returns the state which is selected in the combo box
	 */
	public static String getStateFromCombo(final Combo combo, final String[] states, final String defaultState) {

		final int selectedIndex = combo.getSelectionIndex();

		String selectedState;

		if (selectedIndex == -1) {
			selectedState = defaultState;
		} else {
			selectedState = states[selectedIndex];
		}

		return selectedState;
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns an integer value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static int getStateInt(final IDialogSettings state, final String key, final int defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getInt(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a long value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static long getStateLong(final IDialogSettings state, final String key, final int defaultValue) {
		try {
			return state.get(key) == null ? defaultValue : state.getLong(key);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * @param state
	 * @param key
	 * @param defaultValue
	 * @return Returns a string value from {@link IDialogSettings}. When the key is not found, the
	 *         default value is returned.
	 */
	public static String getStateString(final IDialogSettings state, final String key, final String defaultValue) {

		final String stateValue = state.get(key);

		return stateValue == null ? defaultValue : stateValue;
	}

	/**
	 * found here: http://www.odi.ch/prog/design/datetime.php
	 * 
	 * @param cal
	 * @return
	 */
	public static int getYearForWeek(final Calendar cal) {

		final int year = cal.get(Calendar.YEAR);
		final int week = cal.get(Calendar.WEEK_OF_YEAR);
		final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

		if (week == 1 && dayOfMonth > 20) {
			return year + 1;
		}

		if (week >= 52 && dayOfMonth < 10) {
			return year - 1;
		}

		return year;
	}

	/**
	 * Selects an item in the combo box which is retrieved from a state.
	 * 
	 * @param state
	 * @param stateKey
	 * @param comboStates
	 *            this array must must have the same number of entries as the combo box has items
	 * @param defaultState
	 * @param combo
	 */
	public static void selectStateInCombo(	final IDialogSettings state,
											final String stateKey,
											final String[] comboStates,
											final String defaultState,
											final Combo combo) {

		final String stateValue = Util.getStateString(state, stateKey, defaultState);

		int stateIndex = 0;
		for (final String comboStateValue : comboStates) {
			if (stateValue.equals(comboStateValue)) {
				break;
			}

			stateIndex++;
		}

		combo.select(stateIndex);
	}

	/**
	 * Set the state for an integer array
	 * 
	 * @param state
	 * @param stateKey
	 * @param intValues
	 */
	public static void setState(final IDialogSettings state, final String stateKey, final int[] intValues) {

		final String[] stateIndices = new String[intValues.length];
		for (int index = 0; index < intValues.length; index++) {
			stateIndices[index] = Integer.toString(intValues[index]);
		}

		state.put(stateKey, stateIndices);
	}

	/**
	 * Open view and activate it
	 * 
	 * @param viewId
	 * @return
	 * @throws PartInitException
	 */
	public static IViewPart showView(final String viewId) {

		try {

			final IWorkbench wb = PlatformUI.getWorkbench();
			if (wb == null) {
				return null;
			}

			final IWorkbenchWindow wbWin = wb.getActiveWorkbenchWindow();
			if (wbWin == null) {
				return null;
			}

			final IWorkbenchPage page = wbWin.getActivePage();
			if (page == null) {
				return null;
			}

			return page.showView(viewId, null, IWorkbenchPage.VIEW_ACTIVATE);

		} catch (final PartInitException e) {
			StatusUtil.showStatus(e);
		}

		return null;
	}

}
