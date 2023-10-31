/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class OSMLocation {

   /**
    * <pre>
    *
    *   {
    *      "place_id"      : 78981669,
    *      "osm_id"        : 44952014,

    *      "licence"       : "Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
    *
    *      "place_rank"    : 30,
    *      "importance"    : 0.00000999999999995449,
    *
    *      "addresstype"   : "leisure",
    *      "class"         : "leisure",
    *      "osm_type"      : "way",
    *      "type"          : "pitch",

    *      "lat"           : "47.116116899999994",
    *      "lon"           : "7.989645450000001",
    *
    *      "name"          : "",
    *      "display_name"  : "5a, Schlossfeldstrasse, St. Niklausenberg, Guon, Willisau, Luzern, 6130, Schweiz/Suisse/Svizzera/Svizra",
    *
    *      "address": {
    *         "house_number"     : "5a",
    *         "road"             : "Schlossfeldstrasse",
    *         "neighbourhood"    : "St. Niklausenberg",
    *         "farm"             : "Guon",
    *         "village"          : "Willisau",
    *         "state"            : "Luzern",
    *         "ISO3166-2-lvl4"   : "CH-LU",
    *         "postcode"         : "6130",
    *         "country"          : "Schweiz/Suisse/Svizzera/Svizra",
    *         "country_code"     : "ch"
    *      },
    *
    *      "boundingbox": [
    *         "47.1159171",
    *         "47.1163167",
    *         "7.9895150",
    *         "7.9897759"
    *      ]
    *   }
    *
    * </pre>
    */

   long   place_id;
   long   osm_id;
   String licence;

   String osm_type;
   String type;
   String addresstype;

   @JsonAlias({ "class" })
   String locationClass;

   double lat;
   double lon;
   String name;
   String display_name;
//
   int    place_rank;
   double importance;

//   String address= {
//   String      "house_number= "5a",
//   String      "road= "Schlossfeldstrasse",
//   String      "neighbourhood= "St. Niklausenberg",
//   String      "farm= "Guon",
//   String      "village= "Willisau",
//   String      "state= "Luzern",
//   String      "ISO3166-2-lvl4= "CH-LU",
//   String      "postcode= "6130",
//   String      "country= "Schweiz/Suisse/Svizzera/Svizra",
//   String      "country_code= "ch"
//   String      },

}