/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

public class ChartLayerNight implements IChartLayer, IChartOverlay {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   private int                           LABEL_OFFSET;

   private int                           PAUSE_POINT_SIZE;
   private TourChart                     _tourChart;

   private ChartNightConfig              _cnc;
   private int                           _devXPause;

   private int                           _devYPause;

   public ChartLayerNight(final TourChart tourChart) {

      _tourChart = tourChart;
   }

   /**
    * This paints the pause(s) for the current graph configuration.
    */
   @Override
   public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

      final int opacity = _prefStore.getInt(ITourbookPreferences.GRAPH_OPACITY_NIGHT_SECTIONS);
      final Device display = gc.getDevice();

      PAUSE_POINT_SIZE = pc.convertVerticalDLUsToPixels(100);

      final int pausePointSize2 = PAUSE_POINT_SIZE / 2;
      final int devYTop = drawingData.getDevYTop();
      final int devYBottom = drawingData.getDevYBottom();
      final long devVirtualGraphImageOffset = chart.getXXDevViewPortLeftBorder();
      final int devGraphHeight = drawingData.devGraphHeight;
      final long devVirtualGraphWidth = drawingData.devVirtualGraphWidth;
      final int devVisibleChartWidth = drawingData.getChartDrawingData().devVisibleChartWidth;
      final boolean isGraphZoomed = devVirtualGraphWidth != devVisibleChartWidth;

      final float graphYBottom = drawingData.getGraphYBottom();
      //what if a tour spans across more than 1 day?
      final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
      final double scaleX = drawingData.getScaleX();
      final double scaleY = drawingData.getScaleY();

      final Color colorDefault = new Color(display, new RGB(0x8c, 0x8c, 0x8c));
      final Color colorDevice = new Color(display, new RGB(0xff, 0x0, 0x80));
      final Color colorHidden = new Color(display, new RGB(0x24, 0x9C, 0xFF));

      gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

      /*
       * For each pont at night, we draw a gray line from the top of the graph to the altitude line
       */
      for (final ChartLabel chartLabel : _cnc.chartLabels) {

         //The current charlabel is the start or at least a point in which it's night.
         //find the end of the night section (serieIndex)
         // calculate the according graph coordinates
         //display a box from this point to the end of this night section

         final float yValue = yValues[chartLabel.serieIndex];

         final int devYGraph = (int) (graphYBottom * scaleY) - 0;

         /*
          * Get pause point top/left position
          */
         final int devXPauseTopLeft = _devXPause - pausePointSize2;
         final int devYPauseTopLeft = _devYPause - pausePointSize2;

         /*
          * Draw pause point
          */
         gc.setBackground(colorDefault);

         gc.setAlpha(opacity);
         gc.fillRectangle(devXPauseTopLeft, devYGraph, 100, devYGraph);
//      }
      }
      colorDefault.dispose();
      colorDevice.dispose();
      colorHidden.dispose();

      gc.setClipping((Rectangle) null);
   }

   /**
    * This is painting the hovered pause.
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {
      //Nothing
   }

   public void setChartNightConfig(final ChartNightConfig chartNightConfig) {
      _cnc = chartNightConfig;
   }
}
