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

import net.tourbook.Messages;
import net.tourbook.data.TourPerson;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class PrefPageSwimScore extends PrefPageTrainingStressModel {
   //private TourPerson _tourPerson;
   private Group _swimScoreGroup;

   @Override
   public void dispose() {
      _swimScoreGroup = null;
   }

   @Override
   public String getGroupName() {

      return Messages.Pref_TrainingStress_SwimScore_GroupName;
   }

   /**
    * UI for the SwimScore preferences
    */
   @Override
   public Group getGroupUI(final Composite parent, final TourPerson tourPerson) {

      //_tourPerson = tourPerson;

      if (_swimScoreGroup == null) {
         _swimScoreGroup = new Group(parent, SWT.NONE);
         GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_swimScoreGroup);
         {
            final Label label = new Label(_swimScoreGroup, SWT.NONE);
            label.setText("SWIMSCORE"); //$NON-NLS-1$
         }
      }

      return _swimScoreGroup;

   }

   @Override
   public void restoreState() {
      // TODO Auto-generated method stub

   }

   @Override
   public void saveState() {
      // TODO Auto-generated method stub

   }
}
