/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class UITests {

   @AfterAll
   static void cleanUp() {
      setMetricSystem();
   }

   private static void setMetricSystem() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(0, true);
      UI.updateUnits();
   }

   private void setImperialSystem() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(1, true);
      UI.updateUnits();
   }

   @Test
   void testConvertAverageElevationChangeFromMetric() {

      setMetricSystem();
      //250m/km -> 250m/km
      assertEquals(250, UI.convertAverageElevationChangeFromMetric(250));

      setImperialSystem();
      //250m/km -> 1320ft/mi
      assertEquals(1320.0f, UI.convertAverageElevationChangeFromMetric(250));
   }

   @Test
   void testConvertPrecipitation_FromMetric() {

      setMetricSystem();
      //1mm -> 1mm
      assertEquals(1.0f, UI.convertPrecipitation_FromMetric(1.0f));

      setImperialSystem();
      //1mm -> 0.03938in
      assertEquals(0.03937007874f, UI.convertPrecipitation_FromMetric(1.0f));
   }

   @Test
   void testConvertPrecipitation_ToMetric() {

      setMetricSystem();
      //1mm -> 1mm
      assertEquals(1.0f, UI.convertPrecipitation_ToMetric(1.0f));

      setImperialSystem();
      //1in -> 25.4mm
      assertEquals(25.4f, UI.convertPrecipitation_ToMetric(1.0f));
   }

   @Test
   void testGetCardinalDirectionText() {

//      Messages.Weather_WindDirection_ESE,
//      Messages.Weather_WindDirection_SE,
//      Messages.Weather_WindDirection_SSE,
//      ,
//      Messages.Weather_WindDirection_SSW,
//      Messages.Weather_WindDirection_SW,
//      Messages.Weather_WindDirection_WSW,
//      M,
//      Messages.Weather_WindDirection_WNW,
//      Messages.Weather_WindDirection_NW,
//      Messages.Weather_WindDirection_NNW
      //0° -> N
      assertEquals(Messages.Weather_WindDirection_N, UI.getCardinalDirectionText(0));

      //20° -> NNE
      assertEquals(Messages.Weather_WindDirection_NNE, UI.getCardinalDirectionText(20));

      //45° -> NE
      assertEquals(Messages.Weather_WindDirection_NE, UI.getCardinalDirectionText(45));

      //65° -> ENE
      assertEquals(Messages.Weather_WindDirection_ENE, UI.getCardinalDirectionText(65));

      //90° -> E
      assertEquals(Messages.Weather_WindDirection_E, UI.getCardinalDirectionText(90));

      //180° -> S
      assertEquals(Messages.Weather_WindDirection_S, UI.getCardinalDirectionText(180));

      //270° -> W
      assertEquals(Messages.Weather_WindDirection_W, UI.getCardinalDirectionText(270));
   }
}
