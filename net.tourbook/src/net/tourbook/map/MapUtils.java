/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map;

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.common.color.MapLegendImageConfig;
import net.tourbook.data.TourData;
import net.tourbook.map2.Messages;

public class MapUtils {

	/**
	 * Update the min/max values in the {@link IGradientColors} for the currently displayed legend
	 * 
	 * @param allTourData
	 * @param mapColorProvider
	 * @param legendHeight
	 * @return Return <code>true</code> when the legend value could be updated, <code>false</code>
	 *         when data are not available
	 */
	public static boolean updateMinMaxValues(	final ArrayList<TourData> allTourData,
												final IGradientColors mapColorProvider,
												final int legendHeight) {

		if (allTourData.size() == 0) {
			return false;
		}

		final GraphColorManager colorProvider = GraphColorManager.getInstance();

		ColorDefinition colorDefinition = null;
		final MapLegendImageConfig mapColorConfig = mapColorProvider.getMapLegendImageConfig();

		// tell the legend provider how to draw the legend
		switch (mapColorProvider.getMapColorId()) {

		case Altitude:

			float minValue = Float.MIN_VALUE;
			float maxValue = Float.MAX_VALUE;
			boolean setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getAltitudeSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {

						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_ALTITUDE);

			mapColorProvider.setMapColorColors(colorDefinition.getNewMapColor());
			mapColorProvider.setMapConfigValues(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_ALTITUDE,
					LegendUnitFormat.Number);

			break;

		case Gradient:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getGradientSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			mapColorConfig.numberFormatDigits = 1;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_GRADIENT);

			mapColorProvider.setMapColorColors(colorDefinition.getNewMapColor());
			mapColorProvider.setMapConfigValues(
					legendHeight,
					minValue,
					maxValue,
					Messages.graph_label_gradient_unit,
					LegendUnitFormat.Number);

			break;

		case Pace:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getPaceSerieSeconds();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			mapColorConfig.unitFormat = LegendUnitFormat.Pace;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_PACE);

			mapColorProvider.setMapColorColors(colorDefinition.getNewMapColor());
			mapColorProvider.setMapConfigValues(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_PACE,
					LegendUnitFormat.Pace);

			break;

		case Pulse:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.pulseSerie;
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					// patch from Kenny Moens / 2011-08-04
					if (dataValue == 0 || dataValue == Float.MIN_VALUE) {
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_HEARTBEAT);

			mapColorProvider.setMapColorColors(colorDefinition.getNewMapColor());
			mapColorProvider.setMapConfigValues(
					legendHeight,
					minValue,
					maxValue,
					Messages.graph_label_heartbeat_unit,
					LegendUnitFormat.Number);

			break;

		case Speed:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : allTourData) {

				final float[] dataSerie = tourData.getSpeedSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			mapColorConfig.numberFormatDigits = 1;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_SPEED);

			mapColorProvider.setMapColorColors(colorDefinition.getNewMapColor());
			mapColorProvider.setMapConfigValues(
					legendHeight,
					minValue,
					maxValue,
					UI.UNIT_LABEL_SPEED,
					LegendUnitFormat.Number);

			break;

		default:
			break;
		}

		return true;
	}
}
