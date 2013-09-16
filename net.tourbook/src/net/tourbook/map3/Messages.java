/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.map3.messages";				//$NON-NLS-1$

	public static String		Custom_Layer_Status;
	public static String		Custom_Layer_TerrainProfile;
	public static String	Custom_Layer_TourLegend;

	public static String		Custom_Layer_TourTrack;
	public static String		Custom_Layer_ViewerController;

	public static String		Default_Layer_Atmosphere;
	public static String		Default_Layer_Compass;
	public static String		Default_Layer_MS_VirtualEarchAerial;
	public static String		Default_Layer_NASABlueMarble;
	public static String		Default_Layer_PlaceNames;
	public static String		Default_Layer_PoliticalBoundaries;
	public static String		Default_Layer_ScaleBar;
	public static String		Default_Layer_Stars;
	public static String		Default_Layer_USGS_UrbanArea;
	public static String		Default_Layer_WMS_BingImagery;
	public static String		Default_Layer_WMS_i_cubed_Landsat;
	public static String		Default_Layer_WMS_NASABlueMarble2004;
	public static String		Default_Layer_WMS_OpenStreetMap;
	public static String		Default_Layer_WMS_USDA_NAIP;
	public static String		Default_Layer_WMS_USDA_NAIP_USGS;
	public static String		Default_Layer_WMS_USGS_Topo100;
	public static String		Default_Layer_WMS_USGS_Topo24;
	public static String		Default_Layer_WMS_USGS_Topo250;
	public static String		Default_Layer_WorldMap;

	public static String		Image_Map3_Map3PropertiesView;

	public static String	Layer_Category_Info;

	public static String	Layer_Category_Map;

	public static String	Layer_Category_Tools;

	public static String	Layer_Category_Tour;

	public static String		Map3_Action_OpenMap3PropertiesView;
	public static String		Map3_Action_TrackColor;

	public static String		Prop_Viewer_Label_AltitudeRange_Tooltip;

	public static String		Terrain_Follow_Cursor;
	public static String		Terrain_Follow_Eye;
	public static String		Terrain_Follow_None;
	public static String		Terrain_Follow_Object;
	public static String		Terrain_Follow_View;

	public static String		Terrain_Profile_Checkbox_KeepProportions;
	public static String		Terrain_Profile_Checkbox_ShowEye;
	public static String		Terrain_Profile_Checkbox_ZeroBased;
	public static String		Terrain_Profile_Dimension_Large;
	public static String		Terrain_Profile_Dimension_Medium;
	public static String		Terrain_Profile_Dimension_Small;
	public static String		Terrain_Profile_Label_Dimension;
	public static String		Terrain_Profile_Label_Follow;
	public static String		Terrain_Profile_Label_ProfileLength;

	public static String		TourTrack_Properties_Button_Default;
	public static String		TourTrack_Properties_Button_Default_Tooltip;
	public static String		TourTrack_Properties_Checkbox_AltitudeOffset;
	public static String		TourTrack_Properties_Checkbox_AltitudeOffset_Tooltip;
	public static String		TourTrack_Properties_Checkbox_DrawVerticals;
	public static String		TourTrack_Properties_Checkbox_DrawVerticals_Tooltip;
	public static String		TourTrack_Properties_Checkbox_ExtrudePath;
	public static String		TourTrack_Properties_Checkbox_ExtrudePath_Tooltip;
	public static String		TourTrack_Properties_Checkbox_IsFollowTerrain;
	public static String		TourTrack_Properties_Checkbox_IsFollowTerrain_Tooltip;
	public static String		TourTrack_Properties_Checkbox_ShowTrackPositions;
	public static String		TourTrack_Properties_Checkbox_ShowTrackPositions_Tooltip;
	public static String		TourTrack_Properties_Label_Altitude;
	public static String		TourTrack_Properties_Label_Altitude_Tooltip;
	public static String		TourTrack_Properties_Label_CurtainColor;
	public static String		TourTrack_Properties_Label_CurtainColor_Tooltip;
	public static String		TourTrack_Properties_Label_CurtainColorHovered;
	public static String		TourTrack_Properties_Label_CurtainColorHovered_Tooltip;
	public static String		TourTrack_Properties_Label_CurtainColorHovSel;
	public static String		TourTrack_Properties_Label_CurtainColorHovSel_Tooltip;
	public static String		TourTrack_Properties_Label_CurtainColorSelected;
	public static String		TourTrack_Properties_Label_CurtainColorSelected_Tooltip;
	public static String		TourTrack_Properties_Label_DialogTitle;
	public static String		TourTrack_Properties_Label_OutlineColorHovered;
	public static String		TourTrack_Properties_Label_OutlineColorHovered_Tooltip;
	public static String		TourTrack_Properties_Label_OutlineColorHovSel;
	public static String		TourTrack_Properties_Label_OutlineColorHovSel_Tooltip;
	public static String		TourTrack_Properties_Label_OutlineColorSelected;
	public static String		TourTrack_Properties_Label_OutlineColorSelected_Tooltip;
	public static String		TourTrack_Properties_Label_OutlineWidth;
	public static String		TourTrack_Properties_Label_OutlineWidth_Tooltip;
	public static String		TourTrack_Properties_Label_PathResolution;
	public static String		TourTrack_Properties_Label_PathResolution_Tooltip;
	public static String		TourTrack_Properties_Label_TrackColor;
	public static String		TourTrack_Properties_Label_TrackColor_Tooltip;
	public static String		TourTrack_Properties_Label_TrackPositionSize;
	public static String		TourTrack_Properties_Label_TrackPositionSize_Tooltip;
	public static String		TourTrack_Properties_Label_TrackPositionThreshold;
	public static String		TourTrack_Properties_Label_TrackPositionThreshold_Tooltip;

	public static String		Track_Config_Altitude_Mode_Absolute;
	public static String		Track_Config_Altitude_Mode_ClampToGround;
	public static String		Track_Config_Altitude_Mode_RelativeToGround;
	public static String		Track_Config_Path_Resolution_High;
	public static String		Track_Config_Path_Resolution_Optimized;
	public static String		Track_Config_Path_Resolution_Viewport;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
