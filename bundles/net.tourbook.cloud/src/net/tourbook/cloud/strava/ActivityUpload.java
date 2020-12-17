/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.cloud.strava;

public class ActivityUpload {
   private long   id;
   private String id_str;
   private String external_id;
   private String error       = null;
   private String status;
   private String activity_id = null;

   // Getter Methods

   public String getActivity_id() {
      return activity_id;
   }

   public String getError() {
      return error;
   }

   public String getExternal_id() {
      return external_id;
   }

   public float getId() {
      return id;
   }

   public String getId_str() {
      return id_str;
   }

   public String getStatus() {
      return status;
   }
}
