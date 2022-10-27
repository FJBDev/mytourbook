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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class that implements the Predicted Performance model from EW Banister
 * Source : https://www.researchgate.net/publication/20910238_Modeling_human_performance_in_running
 */
public final class PredictedPerformance {

   //TODO FB make it implement an interface. ITrainingLoadModel ????
   //Preference menu just ofr this or keep it in the computed values ?
   //https://github.com/FJBDev/mytourbook/blob/04a6cd970ed157bd8f784eca2b9c0dc7843c5d6d/bundles/net.tourbook/src/net/tourbook/data/PerformanceModelingData.java
//   store.setDefault(ITourbookPreferences.FATIGUE_DECAY, 15);
   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /**
    * Computes the fitness value for a given date and a given previous training data
    * Fitness = g(t) = g(t-i)e^(-i/T1) + w(t)
    *
    * @param numberOfDays
    *           The number of days between the current's day of training and the previous day of
    *           training.
    * @param previousFatigueValue
    * @param currentFatigueValue
    * @return
    */
   private static int computeFatigueValue(final int numberOfDays, final int previousFatigueValue, final int currentFatigueValue) {

      return computeResponseValue(numberOfDays, previousFatigueValue, currentFatigueValue, _prefStore.getInt(ITourbookPreferences.FATIGUE_DECAY));
   }

   /**
    * Computes the fitness value for a given date and a given previous training data
    * Fitness = g(t) = g(t-i)e^(-i/T1) + w(t)
    *
    * @param numberOfDays
    *           The number of days between the current's day of training and the previous day of
    *           training.
    * @param previousFitnessValue
    * @param currentFitnessValue
    * @return
    */
   private static int computeFitnessValue(final int numberOfDays, final int previousFitnessValue, final int currentFitnessValue) {

      return computeResponseValue(numberOfDays, previousFitnessValue, currentFitnessValue, _prefStore.getInt(ITourbookPreferences.FITNESS_DECAY));
   }

   /**
    * Computes the fitness value for a given date and a given previous training data
    * Fitness = g(t) = g(t-i)e^(-i/T1) + w(t)
    *
    * @param numberOfDays
    *           The number of days between the current's day of training and the previous day of
    *           training.
    * @param previousStressValue
    * @param currentStressValue
    * @return
    */
   public static int computePredictedPerformanceValue(final int numberOfDays, final int previousStressValue, final int currentStressValue) {

      return computeFitnessValue(numberOfDays, previousStressValue, currentStressValue) -
            computeFatigueValue(numberOfDays, previousStressValue, currentStressValue);
   }

   /**
    * Computes the response level value for a given date and a given previous training data
    *
    * @param numberOfDays
    *           The number of days between the current day of training and the last day of
    *           training.
    * @param previousResponseValue
    * @param currentResponseValue
    * @return
    */
   private static int computeResponseValue(final int numberOfDays,
                                           final int previousResponseValue,
                                           final int currentResponseValue,
                                           final int decay) {

      final float exponent = numberOfDays * -1f / decay;

      final int resulttoremove = (int) (previousResponseValue * Math.exp(exponent) + currentResponseValue);
      return resulttoremove;
   }

}
