/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.action;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;

public class ActionReloadFailedMapImages extends Action {

   private Map2View _mapView;

   public ActionReloadFailedMapImages(final Map2View mapView) {

      super(null, AS_PUSH_BUTTON);

      _mapView = mapView;

      setText(Messages.map_action_reload_map);

      setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Refresh));
   }

   @Override
   public void run() {
      _mapView.actionReloadFailedMapImages();
   }

}
