/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IFillPainter;
import net.tourbook.common.swimming.SwimStroke;
import net.tourbook.common.swimming.SwimStrokeManager;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.tour.TourManager;
import net.tourbook.training.TrainingManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
<<<<<<< HEAD
import org.eclipse.swt.graphics.Device;
=======
>>>>>>> refs/remotes/origin/main
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Draws background color into the graph, e.g. HR zone, swim style
 */
public class GraphBackgroundPainter implements IFillPainter {

   private Color[]               _hrZone_Colors;
   private HashMap<Short, Color> _strokeStyle_Colors;

   private void createColors_HrZone(final GC gcGraph, final TourPerson tourPerson) {

      final ArrayList<TourPersonHRZone> personHrZones = tourPerson.getHrZonesSorted();

      _hrZone_Colors = new Color[personHrZones.size()];

      for (int colorIndex = 0; colorIndex < personHrZones.size(); colorIndex++) {

         final TourPersonHRZone hrZone = personHrZones.get(colorIndex);
         final RGB rgb = hrZone.getColor();

         _hrZone_Colors[colorIndex] = new Color(rgb);
      }
   }

   private void createColors_SwimStyle(final GC gcGraph) {

      _strokeStyle_Colors = new HashMap<>();

      for (final Entry<SwimStroke, RGB> swimStrokeItem : SwimStrokeManager.getSwimStroke_RGB().entrySet()) {
         _strokeStyle_Colors.put(swimStrokeItem.getKey().getValue(), new Color(swimStrokeItem.getValue()));
      }
   }

      for (final Entry<SwimStroke, RGB> swimStrokeItem : SwimStrokeManager.getSwimStroke_RGB().entrySet()) {
         _strokeStyle_Colors.put(swimStrokeItem.getKey().getValue(), new Color(display, swimStrokeItem.getValue()));
      }
   }
   @Override
   public void draw(final GC gcGraph,
                    final GraphDrawingData graphDrawingData,
                    final Chart chart,
                    final long[] devXPositions,
                    final int xPos_FirstIndex,
                    final int xPos_LastIndex,
                    final boolean isVariableXValues) {
      final ChartDataModel dataModel = chart.getChartDataModel();

      final TourData tourData = (TourData) dataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
      final TourChartConfiguration tcc = (TourChartConfiguration) dataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_CHART_CONFIGURATION);

      final boolean useGraphBgStyle_HrZone = tcc.isBackgroundStyle_HrZone();
      final boolean useGraphBgStyle_SwimStyle = tcc.isBackgroundStyle_SwimmingStyle();

      HrZoneContext hrZoneContext = null;

      if (useGraphBgStyle_HrZone) {

         final TourPerson tourPerson = tourData.getDataPerson();
         if (tourPerson == null) {
            return;
         }

         final int numberOfHrZones = tourData.getNumberOfHrZones();
         if (numberOfHrZones == 0) {
            return;
         }

<<<<<<< HEAD
         if (tourData.pulseSerie == null) {
            return;
         }
=======
         hrZoneContext = tourData.getHrZoneContext();
         if (hrZoneContext == null) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         createColors_HrZone(gcGraph, tourPerson);
=======
            // this occure when a user do not have hr zones
            return;
         }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      } else if (useGraphBgStyle_SwimStyle) {
=======
         if (tourData.pulseSerie == null) {
            return;
         }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         createColors_SwimStyle(gcGraph);
      }
=======
         createColors_HrZone(gcGraph, tourPerson);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      boolean isGradient = false;
      boolean isWhite = false;
      boolean isBgColor = false;
=======
      } else if (useGraphBgStyle_SwimStyle) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      switch (tcc.graphBackground_Style) {
=======
         createColors_SwimStyle(gcGraph);
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      case GRAPH_COLOR_TOP:
=======
      boolean isGradient = false;
      boolean isWhite = false;
      boolean isBgColor = false;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         isGradient = true;
         isWhite = false;
         isBgColor = true;
         break;
=======
      switch (tcc.graphBackground_Style) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      case NO_GRADIENT:
=======
      case GRAPH_COLOR_TOP:
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         isGradient = false;
=======
         isGradient = true;
>>>>>>> refs/remotes/origin/main
         isWhite = false;
         isBgColor = true;
         break;

<<<<<<< HEAD
      case WHITE_BOTTOM:
=======
      case NO_GRADIENT:
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         isGradient = true;
         isWhite = true;
         isBgColor = false;
=======
         isGradient = false;
         isWhite = false;
         isBgColor = true;
>>>>>>> refs/remotes/origin/main
         break;

<<<<<<< HEAD
      case WHITE_TOP:
=======
      case WHITE_BOTTOM:
>>>>>>> refs/remotes/origin/main

         isGradient = true;
         isWhite = true;
<<<<<<< HEAD
         isBgColor = true;
         break;
      }
=======
         isBgColor = false;
         break;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      if (isWhite) {
         gcGraph.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
      }
=======
      case WHITE_TOP:
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      final int devCanvasHeight = graphDrawingData.devGraphHeight;
=======
         isGradient = true;
         isWhite = true;
         isBgColor = true;
         break;
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      final long devXPrev = devXPositions[valueIndexFirstPoint];
      long devXStart = devXPositions[valueIndexFirstPoint];
=======
      if (isWhite) {
         gcGraph.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      if (useGraphBgStyle_HrZone) {
=======
      final int devCanvasHeight = graphDrawingData.devGraphHeight;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         final float[] pulseSerie = tourData.pulseSerie;
=======
      final long devXPrev = devXPositions[xPos_FirstIndex];
      long devXStart = devXPositions[xPos_FirstIndex];
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         final HrZoneContext hrZoneContext = tourData.getHrZoneContext();
         int prevZoneIndex = TrainingManager.getZoneIndex(hrZoneContext, pulseSerie[valueIndexFirstPoint]);
=======
      if (useGraphBgStyle_HrZone) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         for (int valueIndex = valueIndexFirstPoint + 1; valueIndex <= valueIndexLastPoint; valueIndex++) {
=======
         final float[] dataSerie = isVariableXValues
               ? dataModel.getVariableY_Values()
               : tourData.pulseSerie;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            final long devXCurrent = devXPositions[valueIndex];
            final boolean isLastIndex = valueIndex == valueIndexLastPoint;
=======
         if (dataSerie != null) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            // ignore same position even when the HR zone has changed
            if (devXCurrent == devXPrev && isLastIndex == false) {
               continue;
            }
=======
            int prevZoneIndex = TrainingManager.getZoneIndex(hrZoneContext, dataSerie[xPos_FirstIndex]);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            // check if zone has changed
            final int zoneIndex = TrainingManager.getZoneIndex(hrZoneContext, pulseSerie[valueIndex]);
            if (zoneIndex == prevZoneIndex && isLastIndex == false) {
               continue;
            }
=======
            for (int valueIndex = xPos_FirstIndex + 1; valueIndex <= xPos_LastIndex; valueIndex++) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            final int devWidth = (int) (devXCurrent - devXStart);
            final Color color = _hrZone_Colors[prevZoneIndex];
=======
               final long devXCurrent = devXPositions[valueIndex];
               final boolean isLastIndex = valueIndex == xPos_LastIndex;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            if (isBgColor) {
               gcGraph.setBackground(color);
            } else {
               gcGraph.setForeground(color);
            }
=======
               // ignore same position even when the HR zone has changed
               if (devXCurrent == devXPrev && isLastIndex == false) {
                  continue;
               }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            if (isGradient) {
               gcGraph.fillGradientRectangle((int) devXStart, 0, devWidth, devCanvasHeight, true);
            } else {
               gcGraph.fillRectangle((int) devXStart, 0, devWidth, devCanvasHeight);
            }
=======
               // check if zone has changed
               final int zoneIndex = TrainingManager.getZoneIndex(hrZoneContext, dataSerie[valueIndex]);
               if (zoneIndex == prevZoneIndex && isLastIndex == false) {
                  continue;
               }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            // set start for the next HR zone
            devXStart = devXCurrent;
            prevZoneIndex = zoneIndex;
         }
=======
               final int devWidth = (int) (devXCurrent - devXStart);
               final Color color = _hrZone_Colors[prevZoneIndex];
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         // dispose colors
         for (final Color color : _hrZone_Colors) {
            color.dispose();
         }
=======
               if (isBgColor) {
                  gcGraph.setBackground(color);
               } else {
                  gcGraph.setForeground(color);
               }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      } else if (useGraphBgStyle_SwimStyle) {
=======
               if (isGradient) {
                  gcGraph.fillGradientRectangle((int) devXStart, 0, devWidth, devCanvasHeight, true);
               } else {
                  gcGraph.fillRectangle((int) devXStart, 0, devWidth, devCanvasHeight);
               }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         final float[] allStrokeStyles = tourData.getSwim_StrokeStyle();
         short prevStrokeStyle = (short) allStrokeStyles[valueIndexFirstPoint];
=======
               // set start for the next HR zone
               devXStart = devXCurrent;
               prevZoneIndex = zoneIndex;
            }
         }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         for (int valueIndex = valueIndexFirstPoint + 1; valueIndex <= valueIndexLastPoint; valueIndex++) {
=======
      } else if (useGraphBgStyle_SwimStyle) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            final long devXCurrent = devXPositions[valueIndex];
            final boolean isLastIndex = valueIndex == valueIndexLastPoint;
=======
         final float[] allStrokeStyles = tourData.getSwim_StrokeStyle();
         short prevStrokeStyle = (short) allStrokeStyles[xPos_FirstIndex];
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            // ignore same position
            if (devXCurrent == devXPrev && isLastIndex == false) {
               continue;
            }
=======
         for (int valueIndex = xPos_FirstIndex + 1; valueIndex <= xPos_LastIndex; valueIndex++) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            // check if stroke style has changed
            final short currentStrokeStyle = (short) allStrokeStyles[valueIndex];
            if (currentStrokeStyle == prevStrokeStyle && isLastIndex == false) {
               continue;
            }
=======
            final long devXCurrent = devXPositions[valueIndex];
            final boolean isLastIndex = valueIndex == xPos_LastIndex;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            final int devWidth = (int) (devXCurrent - devXStart);
            final Color color = _strokeStyle_Colors.get(prevStrokeStyle);
=======
            // ignore same position
            if (devXCurrent == devXPrev && isLastIndex == false) {
               continue;
            }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            if (color != null) {
=======
            // check if stroke style has changed
            final short currentStrokeStyle = (short) allStrokeStyles[valueIndex];
            if (currentStrokeStyle == prevStrokeStyle && isLastIndex == false) {
               continue;
            }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
               /*
                * Color could be null when there is no stroke during the rest time -> nothing will
                * be painted to make the heartrate more visible
                */
=======
            final int devWidth = (int) (devXCurrent - devXStart);
            final Color color = _strokeStyle_Colors.get(prevStrokeStyle);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
               if (isBgColor) {
                  gcGraph.setBackground(color);
               } else {
                  gcGraph.setForeground(color);
               }
=======
            if (color != null) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
               if (isGradient) {
                  gcGraph.fillGradientRectangle((int) devXStart, 0, devWidth, devCanvasHeight, true);
               } else {
                  gcGraph.fillRectangle((int) devXStart, 0, devWidth, devCanvasHeight);
               }
            }
=======
               /*
                * Color could be null when there is no stroke during the rest time -> nothing will
                * be painted to make the heartrate more visible
                */
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            // set start for the next HR zone
            devXStart = devXCurrent;
            prevStrokeStyle = currentStrokeStyle;
         }
=======
               if (isBgColor) {
                  gcGraph.setBackground(color);
               } else {
                  gcGraph.setForeground(color);
               }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         // dispose colors
         for (final Color color : _strokeStyle_Colors.values()) {
            color.dispose();
=======
               if (isGradient) {
                  gcGraph.fillGradientRectangle((int) devXStart, 0, devWidth, devCanvasHeight, true);
               } else {
                  gcGraph.fillRectangle((int) devXStart, 0, devWidth, devCanvasHeight);
               }
            }

            // set start for the next HR zone
            devXStart = devXCurrent;
            prevStrokeStyle = currentStrokeStyle;
>>>>>>> refs/remotes/origin/main
         }
      }
   }

}
