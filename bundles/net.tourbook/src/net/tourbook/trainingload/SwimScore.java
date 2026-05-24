/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard and Contributors
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
package net.tourbook.trainingload;

import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;

public class SwimScore extends TrainingStress {

   public SwimScore(final TourPerson tourPerson) {
      super(tourPerson, null);
   }

   public SwimScore(final TourPerson tourPerson, final TourData tourData) {
      super(tourPerson, tourData);
   }

   /**
    * Function that calculates the SwimScore for a given swim activity and athlete.
    * References
    * https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.590.2897&rep=rep1&type=pdf
    * https://3record.de/about/power_estimation#swim
    * Note : This function will assume that the tour is a swim activity. If not, be aware that the
    * SwimScore value will be worthless.
    *
    * @return The SwimScore value
    */
   @Override
   public int Compute(final int startIndex, final int endIndex) {
      if (_tourPerson == null || _tourData == null || _tourData.timeSerie == null || startIndex >= endIndex) {
         return 0;
      }

      return 0;
   }
}
