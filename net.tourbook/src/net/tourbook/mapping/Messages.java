/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.mapping;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.mapping.messages";	//$NON-NLS-1$

	public static String		graph_label_gradiend_unit;
	public static String		graph_label_heartbeat_unit;

	public static String		image_action_change_tile_factory;
	public static String		image_action_show_tour_in_map;
	public static String		image_action_show_tour_in_map_disabled;
	public static String		image_action_synch_with_tour;
	public static String		image_action_synch_with_tour_disabled;

	public static String		image_action_tour_color_altitude;
	public static String		image_action_tour_color_altitude_disabled;
	public static String		image_action_tour_color_gradient;
	public static String		image_action_tour_color_gradient_disabled;
	public static String		image_action_tour_color_pace;
	public static String		image_action_tour_color_pace_disabled;
	public static String		image_action_tour_color_pulse;
	public static String		image_action_tour_color_pulse_disabled;
	public static String		image_action_tour_color_speed;
	public static String		image_action_tour_color_speed_disabled;

	public static String		image_action_zoom_centered;
	public static String		image_action_zoom_in;
	public static String		image_action_zoom_in_disabled;
	public static String		image_action_zoom_out;
	public static String		image_action_zoom_out_disabled;
	public static String		image_action_zoom_show_all;
	public static String		image_action_zoom_show_all_disabled;
	public static String		image_action_zoom_show_entire_tour;

	public static String		map_action_change_tile_factory_tooltip;
	public static String		map_action_save_default_position;
	public static String		map_action_set_default_position;
	public static String		map_action_show_tour_in_map;
	public static String		map_action_synch_with_tour;

	public static String		map_action_tour_color_altitude_tooltip;
	public static String		map_action_tour_color_gradient_tooltip;
	public static String		map_action_tour_color_pase_tooltip;
	public static String		map_action_tour_color_pulse_tooltip;

	public static String		map_action_tour_color_speed_tooltip;
	public static String		map_action_zoom_centered;
	public static String		map_action_zoom_in;
	public static String		map_action_zoom_level_centered_tour;
	public static String		map_action_zoom_level_default;
	public static String		map_action_zoom_level_x_value;
	public static String		map_action_zoom_out;
	public static String		map_action_zoom_show_all;
	public static String		map_action_zoom_show_entire_tour;

	public static String		map_properties_show_tile_info;
	public static String		map_properties_show_tile_info_no;
	public static String		map_properties_show_tile_info_yes;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
