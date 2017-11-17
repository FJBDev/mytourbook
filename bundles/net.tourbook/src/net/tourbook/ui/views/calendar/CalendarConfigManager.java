/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.formatter.ValueFormatter_Number_1_0;
import net.tourbook.common.formatter.ValueFormatter_Number_1_1;
import net.tourbook.common.formatter.ValueFormatter_Number_1_2;
import net.tourbook.common.formatter.ValueFormatter_Number_1_3;
import net.tourbook.common.formatter.ValueFormatter_Time_HH;
import net.tourbook.common.formatter.ValueFormatter_Time_HHMM;
import net.tourbook.common.formatter.ValueFormatter_Time_HHMMSS;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class CalendarConfigManager {

	private static final String				CONFIG_FILE_NAME						= "calendar-config.xml";			//$NON-NLS-1$
	//
	/**
	 * Version number is not yet used.
	 */
	private static final int				CONFIG_VERSION							= 1;
	//
// SET_FORMATTING_OFF
	//
	private static final Bundle				_bundle									= TourbookPlugin.getDefault().getBundle();
	private static final IPath				_stateLocation							= Platform.getStateLocation(_bundle);
	//
// SET_FORMATTING_ON
	//
	private static final String				CUSTOM_DEFAULT_NAME						= "#";
	//
	// common attributes
	private static final String				ATTR_ACTIVE_CONFIG_ID					= "activeConfigId";					//$NON-NLS-1$
	private static final String				ATTR_ID									= "id";								//$NON-NLS-1$
	private static final String				ATTR_CONFIG_NAME						= "name";							//$NON-NLS-1$
	//
	/*
	 * Root
	 */
	private static final String				TAG_ROOT								= "CalendarConfiguration";			//$NON-NLS-1$
	private static final String				ATTR_CONFIG_VERSION						= "configVersion";					//$NON-NLS-1$

	//
	/*
	 * Calendars
	 */
	private static final String				TAG_CALENDAR_CONFIG						= "CalendarConfig";					//$NON-NLS-1$
	private static final String				TAG_CALENDAR							= "Calendar";						//$NON-NLS-1$
	//
	private static final String				TAG_ALL_TOUR_FORMATTER					= "AllTourFormatter";				//$NON-NLS-1$
	private static final String				TAG_ALL_WEEK_FORMATTER					= "AllWeekFormatter";				//$NON-NLS-1$
	private static final String				TAG_ALTERNATE_MONTH_RGB					= "AlternateMonthRGB";				//$NON-NLS-1$
	private static final String				TAG_CALENDAR_BACKGROUND_RGB				= "CalendarBackgroundRGB";			//$NON-NLS-1$
	private static final String				TAG_CALENDAR_FOREGROUND_RGB				= "CalendarForegroundRGB";			//$NON-NLS-1$
	private static final String				TAG_FORMATTER							= "Formatter";						//$NON-NLS-1$
	//
	private static final String				ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR		= "isHideDayDateWhenNoTour";		//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_DATE_COLUMN				= "isShowDateColumn";				//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_DAY_DATE					= "isShowDayDate";					//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR		= "isShowDayDateWeekendColor";		//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_SUMMARY_COLUMN				= "isShowSummaryColumn";			//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_TOUR_CONTENT				= "isShowTourContent";				//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_TOUR_VALUE_UNIT			= "isShowTourValueUnit";			//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_YEAR_COLUMNS				= "isShowYearColumns";				//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_VALUE						= "isShowValue";					//$NON-NLS-1$
	private static final String				ATTR_IS_SHOW_WEEK_VALUE_UNIT			= "isShowWeekValueUnit";			//$NON-NLS-1$
	private static final String				ATTR_IS_TOGGLE_MONTH_COLOR				= "isToggleMonthColor";				//$NON-NLS-1$
	private static final String				ATTR_IS_TRUNCATE_TOUR_TEXT				= "isTruncateTourText";				//$NON-NLS-1$
	private static final String				ATTR_CONFIG_DEFAULT_ID					= "configDefaultId";				//$NON-NLS-1$
	private static final String				ATTR_DATE_COLUMN_CONTENT				= "dateColumnContent";				//$NON-NLS-1$
	private static final String				ATTR_DATE_COLUMN_FONT					= "dateColumnFont";					//$NON-NLS-1$
	private static final String				ATTR_DATE_COLUMN_WIDTH					= "dateColumnWidth";				//$NON-NLS-1$
	private static final String				ATTR_DAY_DATE_FORMAT					= "dayDateFormat";					//$NON-NLS-1$
	private static final String				ATTR_DAY_DATE_FONT						= "dayDateFont";					//$NON-NLS-1$
	private static final String				ATTR_FORMATTER_ID						= "formatterId";					//$NON-NLS-1$
	private static final String				ATTR_FORMATTER_VALUE_FORMAT				= "formatterValueFormat";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND					= "tourBackground";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND_COLOR1				= "tourBackgroundColor1";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND_COLOR2				= "tourBackgroundColor2";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BORDER_WIDTH					= "tourBackgroundWidth";			//$NON-NLS-1$
	private static final String				ATTR_TOUR_BORDER						= "tourBorder";						//$NON-NLS-1$
	private static final String				ATTR_TOUR_BORDER_COLOR					= "tourBorderColor";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_BACKGROUND_WIDTH				= "tourBorderWidth";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_CONTENT_FONT					= "tourContentFont";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_CONTENT_COLOR					= "tourContentColor";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_TITLE_COLOR					= "tourTitleColor";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_TITLE_FONT					= "tourTitleFont";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_TRUNCATED_LINES				= "tourTruncatedLines";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_VALUE_COLOR					= "tourValueColor";					//$NON-NLS-1$
	private static final String				ATTR_TOUR_VALUE_COLUMNS					= "tourValueColumns";				//$NON-NLS-1$
	private static final String				ATTR_TOUR_VALUE_FONT					= "tourValueFont";					//$NON-NLS-1$
	private static final String				ATTR_USE_DRAGGED_SCROLLING				= "useDraggedScrolling";			//$NON-NLS-1$
	private static final String				ATTR_WEEK_COLUMN_WIDTH					= "weekColumnWidth";				//$NON-NLS-1$
	private static final String				ATTR_WEEK_HEIGHT						= "weekHeight";						//$NON-NLS-1$
	private static final String				ATTR_WEEK_VALUE_COLOR					= "weekValueColor";					//$NON-NLS-1$
	private static final String				ATTR_WEEK_VALUE_FONT					= "weekValueFont";					//$NON-NLS-1$
	private static final String				ATTR_YEAR_COLUMNS						= "yearColumns";					//$NON-NLS-1$
	private static final String				ATTR_YEAR_COLUMNS_SPACING				= "yearColumnsSpacing";				//$NON-NLS-1$
	private static final String				ATTR_YEAR_COLUMNS_START					= "yearColumnsStart";				//$NON-NLS-1$
	private static final String				ATTR_YEAR_HEADER_FONT					= "yearHeaderFont";					//$NON-NLS-1$
	//
	static final RGB						DEFAULT_ALTERNATE_MONTH_RGB				= new RGB(0xf0, 0xf0, 0xf0);
	static final ConfigDefault				DEFAULT_CONFIG_DEFAULT_ID				= ConfigDefault.DEFAULT;
	static final RGB						DEFAULT_CALENDAR_BACKGROUND_RGB;
	static final RGB						DEFAULT_CALENDAR_FOREBACKGROUND_RGB;
	static final DayDateFormat				DEFAULT_DAY_DATE_FORMAT					= DayDateFormat.DAY;
	static final int						DEFAULT_DATE_COLUMN_WIDTH				= 50;
	static final DateColumnContent			DEFAULT_DATE_COLUMN_CONTENT				= DateColumnContent.MONTH;
	static final DataFormatter				DEFAULT_EMPTY_FORMATTER;
	static final boolean					DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR	= false;
	static final boolean					DEFAULT_IS_TRUNCATE_TOUR_TEXT			= true;
	static final int						DEFAULT_SUMMARY_COLUMN_WIDTH			= 100;
	static final TourBackground				DEFAULT_TOUR_BACKGROUND					= TourBackground.FILL;
	static final CalendarColor				DEFAULT_TOUR_BACKGROUND_COLOR1			= CalendarColor.DARK;
	static final CalendarColor				DEFAULT_TOUR_BACKGROUND_COLOR2			= CalendarColor.BRIGHT;
	static final int						DEFAULT_TOUR_BACKGROUND_WIDTH			= 3;
	static final TourBorder					DEFAULT_TOUR_BORDER						= TourBorder.NO_BORDER;
	static final CalendarColor				DEFAULT_TOUR_BORDER_COLOR				= CalendarColor.LINE;
	static final int						DEFAULT_TOUR_BORDER_WIDTH				= 1;
	static final CalendarColor				DEFAULT_TOUR_COLOR						= CalendarColor.CONTRAST;
	static final int						DEFAULT_TOUR_TRUNCATED_LINES			= 3;
	static final int						DEFAULT_TOUR_VALUE_COLUMNS				= 3;
	static final int						DEFAULT_WEEK_HEIGHT						= 120;
	static final CalendarColor				DEFAULT_WEEK_VALUE_COLOR				= CalendarColor.BRIGHT;
	static final int						DEFAULT_YEAR_COLUMNS					= 1;
	static final ColumnStart				DEFAULT_YEAR_COLUMNS_LAYOUT				= ColumnStart.CONTINUOUSLY;
	static final int						DEFAULT_YEAR_COLUMNS_SPACING			= 30;
	//
	static final FormatterData[]			DEFAULT_TOUR_FORMATTER_DATA;
	static final FormatterData[]			DEFAULT_WEEK_FORMATTER_DATA;
	static final int						NUM_DEFAULT_TOUR_FORMATTER;
	static final int						NUM_DEFAULT_WEEK_FORMATTER;
	//
	static final int						YEAR_COLUMNS_MIN						= 1;
	static final int						YEAR_COLUMNS_MAX						= 100;
	static final int						CALENDAR_COLUMNS_SPACE_MIN				= 0;
	static final int						CALENDAR_COLUMNS_SPACE_MAX				= 300;
	static final int						WEEK_HEIGHT_MIN							= 1;
	static final int						WEEK_HEIGHT_MAX							= 500;

	private static final IValueFormatter	_valueFormatter_Number_1_0				= new ValueFormatter_Number_1_0();
	private static final IValueFormatter	_valueFormatter_Number_1_1				= new ValueFormatter_Number_1_1();
	private static final IValueFormatter	_valueFormatter_Number_1_2				= new ValueFormatter_Number_1_2();
	private static final IValueFormatter	_valueFormatter_Number_1_3				= new ValueFormatter_Number_1_3();
	private static final IValueFormatter	_valueFormatter_Time_HH					= new ValueFormatter_Time_HH();
	private static final IValueFormatter	_valueFormatter_Time_HHMM				= new ValueFormatter_Time_HHMM();
	private static final IValueFormatter	_valueFormatter_Time_HHMMSS				= new ValueFormatter_Time_HHMMSS();

	private static final DataFormatter		_tourFormatter_Altitude;
	private static final DataFormatter		_tourFormatter_Distance;
	private static final DataFormatter		_tourFormatter_Pace;
	private static final DataFormatter		_tourFormatter_Speed;
	private static final DataFormatter		_tourFormatter_Time_Moving;
	private static final DataFormatter		_tourFormatter_Time_Paused;
	private static final DataFormatter		_tourFormatter_Time_Recording;
	private static final DataFormatter		_tourFormatter_TourDescription;
	private static final DataFormatter		_tourFormatter_TourTitle;

	private static final DataFormatter		_weekFormatter_Altitude;
	private static final DataFormatter		_weekFormatter_Distance;
	private static final DataFormatter		_weekFormatter_Pace;
	private static final DataFormatter		_weekFormatter_Speed;
	private static final DataFormatter		_weekFormatter_Time_Moving;
	private static final DataFormatter		_weekFormatter_Time_Paused;
	private static final DataFormatter		_weekFormatter_Time_Recording;

	static final DataFormatter[]			allTourContentFormatter;
	static final DataFormatter[]			allWeekFormatter;

// SET_FORMATTING_OFF
	//
	static {

		DEFAULT_CALENDAR_BACKGROUND_RGB         = new RGB (59, 59, 59);
		DEFAULT_CALENDAR_FOREBACKGROUND_RGB     = new RGB (197, 197, 197);

//		DEFAULT_CALENDAR_BACKGROUND_RGB			= Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
//		DEFAULT_CALENDAR_FOREBACKGROUND_RGB		= Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB();

		/*
		 * Formatter
		 */
		DEFAULT_EMPTY_FORMATTER 		= createFormatter_Empty();

		// Tour
		_tourFormatter_TourDescription	= createFormatter_TourDescription();
		_tourFormatter_TourTitle	 	= createFormatter_TourTitle();
		
		_tourFormatter_Altitude 		= createFormatter_Altitude();
		_tourFormatter_Distance 		= createFormatter_Distance();

		_tourFormatter_Pace 			= createFormatter_Pace();
		_tourFormatter_Speed 			= createFormatter_Speed();

		_tourFormatter_Time_Moving 		= createFormatter_Time_Moving();
		_tourFormatter_Time_Paused 		= createFormatter_Time_Paused();
		_tourFormatter_Time_Recording 	= createFormatter_Time_Recording();

		allTourContentFormatter = new DataFormatter[] {
				
				DEFAULT_EMPTY_FORMATTER,
				
				_tourFormatter_TourTitle,
				_tourFormatter_TourDescription,
				
				_tourFormatter_Altitude,
				_tourFormatter_Distance,
				
				_tourFormatter_Speed,
				_tourFormatter_Pace,
				
				_tourFormatter_Time_Recording,
				_tourFormatter_Time_Moving,
				_tourFormatter_Time_Paused,
		};
		
		// Week
		_weekFormatter_Altitude 		= createFormatter_Altitude();
		_weekFormatter_Distance 		= createFormatter_Distance();
		
		_weekFormatter_Pace 			= createFormatter_Pace();
		_weekFormatter_Speed 			= createFormatter_Speed();
		
		_weekFormatter_Time_Moving 		= createFormatter_Time_Moving();
		_weekFormatter_Time_Paused 		= createFormatter_Time_Paused();
		_weekFormatter_Time_Recording 	= createFormatter_Time_Recording();

		allWeekFormatter = new DataFormatter[] {
				
				DEFAULT_EMPTY_FORMATTER,
				
				_weekFormatter_Altitude,
				_weekFormatter_Distance,
				
				_weekFormatter_Speed,
				_weekFormatter_Pace,
				
				_weekFormatter_Time_Recording,
				_weekFormatter_Time_Moving,
				_weekFormatter_Time_Paused,
		};

		DEFAULT_TOUR_FORMATTER_DATA = new FormatterData[] {

			new FormatterData(true,		FormatterID.TOUR_TITLE,			_tourFormatter_TourTitle.getDefaultFormat()),
			new FormatterData(true,		FormatterID.TOUR_DESCRIPTION,	_tourFormatter_TourDescription.getDefaultFormat()),
			new FormatterData(true,		FormatterID.ALTITUDE,			_tourFormatter_Altitude.getDefaultFormat()),
			new FormatterData(true,		FormatterID.DISTANCE,			_tourFormatter_Distance.getDefaultFormat()),
			new FormatterData(true,		FormatterID.TIME_MOVING,		_tourFormatter_Time_Moving.getDefaultFormat()),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
		};
		
		NUM_DEFAULT_TOUR_FORMATTER = DEFAULT_TOUR_FORMATTER_DATA.length;

		DEFAULT_WEEK_FORMATTER_DATA = new FormatterData[] {
				
			new FormatterData(true,		FormatterID.ALTITUDE,			_weekFormatter_Altitude.getDefaultFormat()),
			new FormatterData(true,		FormatterID.DISTANCE,			_weekFormatter_Distance.getDefaultFormat()),
			new FormatterData(true,		FormatterID.SPEED,				_weekFormatter_Speed.getDefaultFormat()),
			new FormatterData(true,		FormatterID.PACE,				_weekFormatter_Pace.getDefaultFormat()),
			new FormatterData(true,		FormatterID.TIME_MOVING,		_weekFormatter_Time_Moving.getDefaultFormat()),
			new FormatterData(false,	FormatterID.EMPTY,				ValueFormat.DUMMY_VALUE),
		};
		
		NUM_DEFAULT_WEEK_FORMATTER = DEFAULT_WEEK_FORMATTER_DATA.length;
	}
	//
	//
	private static final CalendarColor_ComboData[] _allCalendarColor_ComboData =

		new CalendarColor_ComboData[] {

			new CalendarColor_ComboData(CalendarColor.BRIGHT,	Messages.Calendar_Config_Color_Bright),
			new CalendarColor_ComboData(CalendarColor.DARK,		Messages.Calendar_Config_Color_Dark),
			new CalendarColor_ComboData(CalendarColor.LINE,		Messages.Calendar_Config_Color_Line),
			new CalendarColor_ComboData(CalendarColor.TEXT,		Messages.Calendar_Config_Color_Text),
			new CalendarColor_ComboData(CalendarColor.BLACK,	Messages.Calendar_Config_Color_Black),
			new CalendarColor_ComboData(CalendarColor.WHITE,	Messages.Calendar_Config_Color_White),
		};

	private static final DateColumn_ComboData[] _allDateColumn_ComboData =

		new DateColumn_ComboData[] {

			new DateColumn_ComboData(DateColumnContent.WEEK_NUMBER,	Messages.Calendar_Config_DateColumn_WeekNumber),
			new DateColumn_ComboData(DateColumnContent.MONTH, 		Messages.Calendar_Config_DateColumn_Month),
			new DateColumn_ComboData(DateColumnContent.YEAR, 		Messages.Calendar_Config_DateColumn_Year),
		};

	private static final ColumnLayout_ComboData[] _allColumnLayout_ComboData =

		new ColumnLayout_ComboData[] {

			new ColumnLayout_ComboData(ColumnStart.CONTINUOUSLY, Messages.Calendar_Config_ColumnLayout_Continuously),

			new ColumnLayout_ComboData(ColumnStart.JAN, TimeTools.month_Full[0]),
			new ColumnLayout_ComboData(ColumnStart.FEB, TimeTools.month_Full[1]),
			new ColumnLayout_ComboData(ColumnStart.MAR, TimeTools.month_Full[2]),
			new ColumnLayout_ComboData(ColumnStart.APR, TimeTools.month_Full[3]),
			new ColumnLayout_ComboData(ColumnStart.MAY, TimeTools.month_Full[4]),
			new ColumnLayout_ComboData(ColumnStart.JUN, TimeTools.month_Full[5]),
			new ColumnLayout_ComboData(ColumnStart.JUL, TimeTools.month_Full[6]),
			new ColumnLayout_ComboData(ColumnStart.AUG, TimeTools.month_Full[7]),
			new ColumnLayout_ComboData(ColumnStart.SEP, TimeTools.month_Full[8]),
			new ColumnLayout_ComboData(ColumnStart.OCT, TimeTools.month_Full[9]),
			new ColumnLayout_ComboData(ColumnStart.NOV, TimeTools.month_Full[10]),
			new ColumnLayout_ComboData(ColumnStart.DEC, TimeTools.month_Full[11]),

			// repeat continuously -> is more handier
			new ColumnLayout_ComboData(ColumnStart.CONTINUOUSLY, Messages.Calendar_Config_ColumnLayout_Continuously),
		};

	private static final DayHeaderDateFormat_ComboData[] _allDateHeaderDateFormat_ComboData =

		new DayHeaderDateFormat_ComboData[] {

			new DayHeaderDateFormat_ComboData(DayDateFormat.DAY,
					NLS.bind(
							Messages.Calendar_Config_DayHeaderDateFormat_Day,
							TimeTools.Formatter_Day.format(LocalDate.now()))),

			new DayHeaderDateFormat_ComboData(DayDateFormat.DAY_MONTH,				TimeTools.Formatter_DayMonth.format(LocalDate.now())),
			new DayHeaderDateFormat_ComboData(DayDateFormat.DAY_MONTH_YEAR,			TimeTools.Formatter_DayMonthYear.format(LocalDate.now())),
			new DayHeaderDateFormat_ComboData(DayDateFormat.AUTOMATIC,				Messages.Calendar_Config_DayHeaderDateFormat_Automatic),
		};

	private static final TourBackground_ComboData[] _allTourBackground_ComboData =

		new TourBackground_ComboData[] {

			new TourBackground_ComboData(TourBackground.NO_BACKGROUND,		Messages.Calendar_Config_TourBackground_NoBackground,
					false,
					false,
					false),

			new TourBackground_ComboData(TourBackground.FILL,				Messages.Calendar_Config_TourBackground_Fill,
					true,
					false,
					false),

			new TourBackground_ComboData(TourBackground.FILL_LEFT,			Messages.Calendar_Config_TourBackground_Fill_Left,
					true,
					false,
					true),

			new TourBackground_ComboData(TourBackground.FILL_RIGHT,			Messages.Calendar_Config_TourBackground_Fill_Right,
					true,
					false,
					true),

			new TourBackground_ComboData(TourBackground.CIRCLE,				Messages.Calendar_Config_TourBackground_Circle,
					true,
					false,
					false),

			new TourBackground_ComboData(TourBackground.GRADIENT_HORIZONTAL,	Messages.Calendar_Config_TourBackground_GradientHorizontal,
					true,
					true,
					false),

			new TourBackground_ComboData(TourBackground.GRADIENT_VERTICAL,		Messages.Calendar_Config_TourBackground_GradientVertical,
					true,
					true,
					false),
		};

	private static final TourBorder_ComboData[] _allTourBorder_ComboData =

		new TourBorder_ComboData[] {

			new TourBorder_ComboData(TourBorder.NO_BORDER,				Messages.Calendar_Config_TourBorder_NoBorder,
					false,
					false),

			new TourBorder_ComboData(TourBorder.BORDER_ALL,				Messages.Calendar_Config_TourBorder_All,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_TOP,				Messages.Calendar_Config_TourBorder_Top,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_BOTTOM,			Messages.Calendar_Config_TourBorder_Bottom,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_TOP_BOTTOM,		Messages.Calendar_Config_TourBorder_TopBottom,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_LEFT,			Messages.Calendar_Config_TourBorder_Left,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_RIGHT,			Messages.Calendar_Config_TourBorder_Right,
					true,
					true),

			new TourBorder_ComboData(TourBorder.BORDER_LEFT_RIGHT,		Messages.Calendar_Config_TourBorder_LeftRight,
					true,
					true),
		};

	private static final DayContentColor_ComboData[] _allTourContentColor_ComboData =

		new DayContentColor_ComboData[] {

			new DayContentColor_ComboData(CalendarColor.CONTRAST, Messages.Calendar_Config_Color_Contrast),
			new DayContentColor_ComboData(CalendarColor.BRIGHT, Messages.Calendar_Config_Color_Bright),
			new DayContentColor_ComboData(CalendarColor.DARK, Messages.Calendar_Config_Color_Dark),
			new DayContentColor_ComboData(CalendarColor.LINE, Messages.Calendar_Config_Color_Line),
			new DayContentColor_ComboData(CalendarColor.TEXT, Messages.Calendar_Config_Color_Text),
			new DayContentColor_ComboData(CalendarColor.BLACK, Messages.Calendar_Config_Color_Black),
			new DayContentColor_ComboData(CalendarColor.WHITE, Messages.Calendar_Config_Color_White),
		};
	//
// SET_FORMATTING_ON
	//
	/**
	 * Contains all configurations which are loaded from a xml file.
	 */
	private static final ArrayList<CalendarConfig>			_allCalendarConfigs					= new ArrayList<>();
	private static CalendarConfig							_activeCalendarConfig;
	//
	private static String									_fromXml_ActiveCalendarConfigId;
	//
	/**
	 * Calendarview or <code>null</code> when closed.
	 */
	private static ICalendarConfigProvider					_configProvider_CalendarView;
	private static ICalendarConfigProvider					_configProvider_SlideoutCalendarOptions;

	public static class CalendarColor_ComboData {

		String			label;
		CalendarColor	color;

		public CalendarColor_ComboData(final CalendarColor color, final String label) {

			this.color = color;
			this.label = label;
		}

	}

	static class ColumnLayout_ComboData {

		String		label;
		ColumnStart	columnLayout;

		public ColumnLayout_ComboData(final ColumnStart columnLayout, final String label) {

			this.columnLayout = columnLayout;
			this.label = label;
		}
	}

	static class DateColumn_ComboData {

		String				label;
		DateColumnContent	dateColumn;

		public DateColumn_ComboData(final DateColumnContent dateColumn, final String label) {

			this.dateColumn = dateColumn;
			this.label = label;
		}
	}

	static class DayContentColor_ComboData {

		String			label;
		CalendarColor	dayContentColor;

		DayContentColor_ComboData(final CalendarColor dayContentColor, final String label) {

			this.label = label;
			this.dayContentColor = dayContentColor;
		}

	}

	static class DayHeaderDateFormat_ComboData {

		String			label;
		DayDateFormat	dayHeaderDateFormat;

		public DayHeaderDateFormat_ComboData(final DayDateFormat dayHeaderDateFormat, final String label) {

			this.dayHeaderDateFormat = dayHeaderDateFormat;
			this.label = label;
		}
	}

	interface ICalendarConfigProvider {

		/**
		 * Calendar config has changed, update the UI.
		 */
		void updateUI_CalendarConfig();
	}

	static class TourBackground_ComboData {

		TourBackground	tourBackground;
		String			label;

		boolean			isWidth;
		boolean			isColor1;
		boolean			isColor2;

		public TourBackground_ComboData(final TourBackground tourBackground,
										final String label,
										final boolean isColor1,
										final boolean isColor2,
										final boolean isWidth) {

			this.tourBackground = tourBackground;
			this.label = label;

			this.isWidth = isWidth;
			this.isColor1 = isColor1;
			this.isColor2 = isColor2;
		}
	}

	static class TourBorder_ComboData {

		TourBorder	tourBorder;
		String		label;

		boolean		isColor;
		boolean		isWidth;

		public TourBorder_ComboData(final TourBorder tourBorder,
									final String label,
									final boolean isColor,
									final boolean isWidth) {

			this.tourBorder = tourBorder;
			this.label = label;
			this.isColor = isColor;
			this.isWidth = isWidth;
		}
	}

	private static XMLMemento create_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// config version
		xmlRoot.putInteger(ATTR_CONFIG_VERSION, CONFIG_VERSION);

		return xmlRoot;
	}

	private static void createConfig_All() {

		_allCalendarConfigs.clear();

		// add default default - CONFIG_DEFAULT_ID
		_allCalendarConfigs.add(new CalendarConfig());

		_allCalendarConfigs.add(createConfig_Classic());

		_allCalendarConfigs.add(createConfig_Col_01());
		_allCalendarConfigs.add(createConfig_Col_01_Dark());
		_allCalendarConfigs.add(createConfig_Col_03());
		_allCalendarConfigs.add(createConfig_Col_03_Dark());
		_allCalendarConfigs.add(createConfig_Col_10());
		_allCalendarConfigs.add(createConfig_Col_10_Dark());
		_allCalendarConfigs.add(createConfig_Col_20());
		_allCalendarConfigs.add(createConfig_Col_20_Dark());

		_allCalendarConfigs.add(createConfig_Crazy());

		// append 10 custom configurations created from default default config
		for (int configIndex = 1; configIndex < 11; configIndex++) {

			final CalendarConfig config = new CalendarConfig();

			config.name = CUSTOM_DEFAULT_NAME + configIndex;

			_allCalendarConfigs.add(config);
		}
	}

	private static CalendarConfig createConfig_Classic() {

		final CalendarConfig config = new CalendarConfig();

		config.name = Messages.Calendar_Config_Name_Classic;

		// SET_FORMATTING_OFF

//		                                     Classic

		config.defaultId                     = ConfigDefault.CLASSIC;

		// layout
		config.isToggleMonthColor            = true;
		config.useDraggedScrolling           = false;
		config.alternateMonthRGB             = new RGB (240, 240, 240);
		config.calendarBackgroundRGB         = new RGB (255, 255, 255);
		config.calendarForegroundRGB         = new RGB (0, 0, 0);
		config.weekHeight                    = 150;
		                                                                                         
		// year columns
		config.isShowYearColumns             = false;
		config.yearColumns                   = 2;
		config.yearColumnsSpacing            = 30;
		config.yearColumnsStart              = ColumnStart.CONTINUOUSLY;
		config.yearHeaderFont                = CalendarConfig.createFont(2.2f, SWT.BOLD);
		                                                                                         
		// date column
		config.isShowDateColumn              = true;
		config.dateColumnContent             = DateColumnContent.WEEK_NUMBER;
		config.dateColumnFont                = CalendarConfig.createFont(1.5f, SWT.BOLD);
		config.dateColumnWidth               = 50;
		                                                                                         
		// day date
		config.isHideDayDateWhenNoTour       = false;
		config.isShowDayDate                 = true;
		config.isShowDayDateWeekendColor     = true;
		config.dayDateFont                   = CalendarConfig.createFont(1.2f, SWT.NORMAL);
		config.dayDateFormat                 = DayDateFormat.DAY_MONTH_YEAR;
		                                                                                         
		// tour background
		config.tourBackground                = TourBackground.FILL;
		config.tourBackgroundColor1          = CalendarColor.DARK;
		config.tourBackgroundColor2          = CalendarColor.BRIGHT;
		config.tourBackgroundWidth           = 3;
		config.tourBorder                    = TourBorder.NO_BORDER;
		config.tourBorderColor               = CalendarColor.LINE;
		config.tourBorderWidth               = 1;
		                                                                                         
		// tour content
		config.isShowTourContent             = true;
		config.isShowTourValueUnit           = true;
		config.isTruncateTourText            = true;
		config.tourContentColor              = CalendarColor.CONTRAST;
		config.tourContentFont               = CalendarConfig.createFont(0.9f, SWT.NORMAL);
		config.tourTitleColor                = CalendarColor.CONTRAST;
		config.tourTitleFont                 = CalendarConfig.createFont(1.3f, SWT.BOLD);
		config.tourTruncatedLines            = 2;
		config.tourValueColor                = CalendarColor.CONTRAST;
		config.tourValueColumns              = 2;
		config.tourValueFont                 = CalendarConfig.createFont(1.2f, SWT.BOLD);
		                                                                                         
		// week summary column
		config.isShowSummaryColumn           = true;
		config.isShowWeekValueUnit           = true;
		config.weekColumnWidth               = 100;
		config.weekValueColor                = CalendarColor.TEXT;
		config.weekValueFont                 = CalendarConfig.createFont(1.2f, SWT.BOLD);

		// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_01() {

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
		
		config.defaultId	= ConfigDefault.COL_1;
		config.name			= Messages.Calendar_Config_Name_Col_01;
		
// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_01_Dark() {

		final CalendarConfig config = new CalendarConfig();

		config.name = Messages.Calendar_Config_Name_Col_01_Dark;

		// SET_FORMATTING_OFF

//		                                     1 Column

		config.defaultId                     = ConfigDefault.COL_1;

		// layout
		config.isToggleMonthColor            = false;
		config.useDraggedScrolling           = false;
		config.alternateMonthRGB             = new RGB (240, 240, 240);
		config.calendarBackgroundRGB         = new RGB (59, 59, 59);
		config.calendarForegroundRGB         = new RGB (197, 197, 197);
		config.weekHeight                    = 40;
		                                                                                         
		// year columns
		config.isShowYearColumns             = true;
		config.yearColumns                   = 1;
		config.yearColumnsSpacing            = 30;
		config.yearColumnsStart              = ColumnStart.CONTINUOUSLY;
		config.yearHeaderFont                = CalendarConfig.createFont(2.8f, SWT.BOLD);
		                                                                                         
		// date column
		config.isShowDateColumn              = true;
		config.dateColumnContent             = DateColumnContent.MONTH;
		config.dateColumnFont                = CalendarConfig.createFont(1.7f, SWT.BOLD);
		config.dateColumnWidth               = 50;
		                                                                                         
		// day date
		config.isHideDayDateWhenNoTour       = true;
		config.isShowDayDate                 = false;
		config.isShowDayDateWeekendColor     = false;
		config.dayDateFont                   = CalendarConfig.createFont(1.2f, SWT.BOLD);
		config.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                         
		// tour background
		config.tourBackground                = TourBackground.FILL;
		config.tourBackgroundColor1          = CalendarColor.DARK;
		config.tourBackgroundColor2          = CalendarColor.BRIGHT;
		config.tourBackgroundWidth           = 3;
		config.tourBorder                    = TourBorder.NO_BORDER;
		config.tourBorderColor               = CalendarColor.LINE;
		config.tourBorderWidth               = 1;
		                                                                                         
		// tour content
		config.isShowTourContent             = true;
		config.isShowTourValueUnit           = true;
		config.isTruncateTourText            = true;
		config.tourContentColor              = CalendarColor.CONTRAST;
		config.tourContentFont               = CalendarConfig.createFont(0.9f, SWT.NORMAL);
		config.tourTitleColor                = CalendarColor.CONTRAST;
		config.tourTitleFont                 = CalendarConfig.createFont(1.2f, SWT.BOLD);
		config.tourTruncatedLines            = 1;
		config.tourValueColor                = CalendarColor.CONTRAST;
		config.tourValueColumns              = 3;
		config.tourValueFont                 = CalendarConfig.createFont(1.0f, SWT.BOLD);
		                                                                                         
		// week summary column
		config.isShowSummaryColumn           = true;
		config.isShowWeekValueUnit           = true;
		config.weekColumnWidth               = 100;
		config.weekValueColor                = CalendarColor.BRIGHT;
		config.weekValueFont                 = CalendarConfig.createFont(1.0f, SWT.BOLD);

		// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_03() {

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
		
		config.defaultId	= ConfigDefault.COL_3;
		config.name			= Messages.Calendar_Config_Name_Col_03;
		
// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_03_Dark() {

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
			
			config.defaultId	= ConfigDefault.DARK_COL_3;
			config.name			= Messages.Calendar_Config_Name_Col_03_Dark;
			
// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_10() {

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
		
		config.defaultId	= ConfigDefault.COL_10;
		config.name			= Messages.Calendar_Config_Name_Col_10;
		
// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_10_Dark() {

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
		
		config.defaultId	= ConfigDefault.DARK_COL_10;
		config.name			= Messages.Calendar_Config_Name_Col_10_Dark;
		
// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_20() {

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
		
		config.defaultId	= ConfigDefault.COL_20;
		config.name			= Messages.Calendar_Config_Name_Col_20;
		
// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Col_20_Dark() {

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
		
		config.defaultId	= ConfigDefault.DARK_COL_20;
		config.name			= Messages.Calendar_Config_Name_Col_20_Dark;

// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_Crazy() {

		final CalendarConfig config = new CalendarConfig();

		// SET_FORMATTING_OFF

//		                                     Crazy

		config.defaultId                     = ConfigDefault.CRAZY;

		// layout
		config.isToggleMonthColor            = false;
		config.useDraggedScrolling           = false;
		config.alternateMonthRGB             = new RGB (240, 240, 240);
		config.calendarBackgroundRGB         = new RGB (0, 0, 0);
		config.calendarForegroundRGB         = new RGB (114, 114, 114);
		config.weekHeight                    = 13;
		                                                                                         
		// year columns
		config.isShowYearColumns             = true;
		config.yearColumns                   = 20;
		config.yearColumnsSpacing            = 0;
		config.yearColumnsStart              = ColumnStart.JAN;
		config.yearHeaderFont                = CalendarConfig.createFont(1.6f, SWT.BOLD);
		                                                                                         
		// date column
		config.isShowDateColumn              = false;
		config.dateColumnContent             = DateColumnContent.MONTH;
		config.dateColumnFont                = CalendarConfig.createFont(1.5f, SWT.BOLD);
		config.dateColumnWidth               = 50;
		                                                                                         
		// day date
		config.isHideDayDateWhenNoTour       = true;
		config.isShowDayDate                 = false;
		config.isShowDayDateWeekendColor     = false;
		config.dayDateFont                   = CalendarConfig.createFont(1.2f, SWT.BOLD);
		config.dayDateFormat                 = DayDateFormat.DAY;
		                                                                                         
		// tour background
		config.tourBackground                = TourBackground.FILL;
		config.tourBackgroundColor1          = CalendarColor.DARK;
		config.tourBackgroundColor2          = CalendarColor.WHITE;
		config.tourBackgroundWidth           = 3;
		config.tourBorder                    = TourBorder.BORDER_ALL;
		config.tourBorderColor               = CalendarColor.DARK;
		config.tourBorderWidth               = 1;
		                                                                                         
		// tour content
		config.isShowTourContent             = false;
		config.isShowTourValueUnit           = true;
		config.isTruncateTourText            = true;
		config.tourContentColor              = CalendarColor.CONTRAST;
		config.tourContentFont               = CalendarConfig.createFont(0.9f, SWT.NORMAL);
		config.tourTitleColor                = CalendarColor.CONTRAST;
		config.tourTitleFont                 = CalendarConfig.createFont(1.2f, SWT.BOLD);
		config.tourTruncatedLines            = 2;
		config.tourValueColor                = CalendarColor.CONTRAST;
		config.tourValueColumns              = 2;
		config.tourValueFont                 = CalendarConfig.createFont(1.0f, SWT.NORMAL);
		                                                                                         
		// week summary column
		config.isShowSummaryColumn           = false;
		config.isShowWeekValueUnit           = true;
		config.weekColumnWidth               = 60;
		config.weekValueColor                = CalendarColor.TEXT;
		config.weekValueFont                 = CalendarConfig.createFont(1.2f, SWT.BOLD);

		// SET_FORMATTING_ON

		return config;
	}

	private static CalendarConfig createConfig_FromId(final ConfigDefault defaultId) {

// SET_FORMATTING_OFF
		
		switch (defaultId) {
		
		case CLASSIC:		return createConfig_Classic();
		case CRAZY:			return createConfig_Crazy();
		
		case COL_1:			return createConfig_Col_01();
		case COL_3:			return createConfig_Col_03();
		case COL_10:		return createConfig_Col_10();
		case COL_20:		return createConfig_Col_20();
		
		case DARK_COL_1:	return createConfig_Col_01();
		case DARK_COL_3:	return createConfig_Col_03();
		case DARK_COL_10:	return createConfig_Col_10();
		case DARK_COL_20:	return createConfig_Col_20();

		default:
			// create default default
			return new CalendarConfig();
		}
		
// SET_FORMATTING_ON
	}

	/**
	 * Altitude
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Altitude() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.ALTITUDE,
				Messages.Calendar_Config_Value_Altitude,
				GraphColorManager.PREF_GRAPH_ALTITUDE) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.altitude > 0) {

					final float altitude = data.altitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;
					final String valueText = valueFormatter.printDouble(altitude);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_ALTITUDE + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {

						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Distance
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Distance() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.DISTANCE,
				Messages.Calendar_Config_Value_Distance,
				GraphColorManager.PREF_GRAPH_DISTANCE) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.distance > 0) {

					final double distance = data.distance / 1000.0 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

					final String valueText = valueFormatter.printDouble(distance);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_DISTANCE + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_2,
						ValueFormat.NUMBER_1_3 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Empty
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Empty() {

		final DataFormatter dataFormatter = new DataFormatter(FormatterID.EMPTY) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {
				return UI.EMPTY_STRING;
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return null;
			}

			@Override
			public String getText() {
				return Messages.Calendar_Config_Value_ShowNothing;
			}

			@Override
			public ValueFormat[] getValueFormats() {
				return null;
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		return dataFormatter;
	}

	/**
	 * Pace
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Pace() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.PACE,
				Messages.Calendar_Config_Value_Pace,
				GraphColorManager.PREF_GRAPH_PACE) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0 && data.distance > 0) {

					final float pace = data.distance == 0
							? 0
							: 1000 * data.recordingTime / data.distance * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

					final String valueText = UI.format_mm_ss((long) pace);

					return isShowValueUnit //
							? valueText + UI.SPACE + UI.UNIT_LABEL_PACE + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.PACE_MM_SS;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] { ValueFormat.PACE_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		return dataFormatter;
	}

	/**
	 * Speed
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Speed() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.SPEED,
				Messages.Calendar_Config_Value_Speed,
				GraphColorManager.PREF_GRAPH_SPEED) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.distance > 0 && data.recordingTime > 0) {

					final float speed = data.distance == 0
							? 0
							: data.distance / (data.recordingTime / 3.6f);

					final String valueText = valueFormatter.printDouble(speed);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_SPEED + UI.SPACE
							: valueText + UI.SPACE;
				} else {

					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.NUMBER_1_0;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_2 };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatter = getFormatter_Number(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Moving time
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Time_Moving() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TIME_MOVING,
				Messages.Calendar_Config_Value_MovingTime,
				GraphColorManager.PREF_GRAPH_TIME) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0) {

					final String valueText = valueFormatter.printLong(data.drivingTime);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_TIME + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TIME_HH_MM;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						ValueFormat.TIME_HH_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatter = getFormatter_Time(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Paused time
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Time_Paused() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TIME_PAUSED,
				Messages.Calendar_Config_Value_PausedTime,
				GraphColorManager.PREF_GRAPH_TIME) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0) {

					final String valueText = valueFormatter.printLong(data.recordingTime - data.drivingTime);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_TIME + UI.SPACE
							: valueText + UI.SPACE;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TIME_HH_MM;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						ValueFormat.TIME_HH_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatter = getFormatter_Time(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Recording time
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_Time_Recording() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TIME_RECORDING,
				Messages.Calendar_Config_Value_RecordingTime,
				GraphColorManager.PREF_GRAPH_TIME) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				if (data.recordingTime > 0) {

					final String valueText = valueFormatter.printLong(data.recordingTime);

					return isShowValueUnit
							? valueText + UI.SPACE + UI.UNIT_LABEL_TIME + UI.SPACE
							: valueText + UI.SPACE;

				} else {

					return UI.EMPTY_STRING;
				}
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TIME_HH_MM;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] {
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						ValueFormat.TIME_HH_MM_SS };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {

				valueFormatter = getFormatter_Time(valueFormat.name());
			}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Description
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_TourDescription() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TOUR_DESCRIPTION,
				Messages.Calendar_Config_Value_Description,
				UI.EMPTY_STRING) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				return data.tourDescription;
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TEXT;
			}

			@Override
			public ValueFormat[] getValueFormats() {

				return new ValueFormat[] { ValueFormat.TEXT };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	/**
	 * Title
	 * 
	 * @return
	 */
	private static DataFormatter createFormatter_TourTitle() {

		final DataFormatter dataFormatter = new DataFormatter(
				FormatterID.TOUR_TITLE,
				Messages.Calendar_Config_Value_Title,
				UI.EMPTY_STRING) {

			@Override
			String format(final CalendarTourData data, final ValueFormat valueFormat, final boolean isShowValueUnit) {

				return data.tourTitle;
			}

			@Override
			public ValueFormat getDefaultFormat() {
				return ValueFormat.TEXT;
			}

			@Override
			public ValueFormat[] getValueFormats() {
				return new ValueFormat[] { ValueFormat.TEXT };
			}

			@Override
			void setValueFormat(final ValueFormat valueFormat) {}
		};

		// setup default formatter
		dataFormatter.setValueFormat(dataFormatter.getDefaultFormat());

		return dataFormatter;
	}

	static CalendarConfig getActiveCalendarConfig() {

		if (_activeCalendarConfig == null) {
			readConfigFromXml();
		}

		return _activeCalendarConfig;
	}

	/**
	 * @return Returns the index for the {@link #_activeCalendarConfig}, the index starts with 0.
	 */
	static int getActiveCalendarConfigIndex() {

		final CalendarConfig activeConfig = getActiveCalendarConfig();

		for (int configIndex = 0; configIndex < _allCalendarConfigs.size(); configIndex++) {

			final CalendarConfig config = _allCalendarConfigs.get(configIndex);

			if (config.equals(activeConfig)) {
				return configIndex;
			}
		}

		// this case should not happen but ensure that a correct config is set

		setActiveCalendarConfigIntern(_allCalendarConfigs.get(0), null);

		return 0;
	}

	static CalendarColor_ComboData[] getAllCalendarColor_ComboData() {
		return _allCalendarColor_ComboData;
	}

	static ArrayList<CalendarConfig> getAllCalendarConfigs() {

		// ensure configs are loaded
		getActiveCalendarConfig();

		return _allCalendarConfigs;
	}

	static ColumnLayout_ComboData[] getAllColumnLayout_ComboData() {
		return _allColumnLayout_ComboData;
	}

	static DateColumn_ComboData[] getAllDateColumnData() {
		return _allDateColumn_ComboData;
	}

	static DayHeaderDateFormat_ComboData[] getAllDayHeaderDateFormat_ComboData() {
		return _allDateHeaderDateFormat_ComboData;
	}

	static TourBackground_ComboData[] getAllTourBackground_ComboData() {
		return _allTourBackground_ComboData;
	}

	static TourBorder_ComboData[] getAllTourBorderData() {
		return _allTourBorder_ComboData;
	}

	static DayContentColor_ComboData[] getAllTourContentColor_ComboData() {
		return _allTourContentColor_ComboData;
	}

	private static CalendarConfig getConfig_Calendar() {

		CalendarConfig activeConfig = null;

		if (_fromXml_ActiveCalendarConfigId != null) {

			// ensure config id belongs to a config which is available

			for (final CalendarConfig config : _allCalendarConfigs) {

				if (config.id.equals(_fromXml_ActiveCalendarConfigId)) {

					activeConfig = config;
					break;
				}
			}
		}

		if (activeConfig == null) {

			// this case should not happen, create a config

			StatusUtil.log("Created default config for calendar properties");//$NON-NLS-1$

			createConfig_All();

			activeConfig = _allCalendarConfigs.get(0);
		}

		return activeConfig;
	}

	private static File getConfigXmlFile() {

		final File layerFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

		return layerFile;
	}

	private static IValueFormatter getFormatter_Number(final String formatName) {

		if (formatName.equals(ValueFormat.NUMBER_1_0.name())) {

			return _valueFormatter_Number_1_0;

		} else if (formatName.equals(ValueFormat.NUMBER_1_1.name())) {

			return _valueFormatter_Number_1_1;

		} else if (formatName.equals(ValueFormat.NUMBER_1_2.name())) {

			return _valueFormatter_Number_1_2;

		} else if (formatName.equals(ValueFormat.NUMBER_1_3.name())) {

			return _valueFormatter_Number_1_3;

		} else {

			// default

			return _valueFormatter_Number_1_0;
		}
	}

	private static IValueFormatter getFormatter_Time(final String formatName) {

		if (formatName.equals(ValueFormat.TIME_HH.name())) {

			return _valueFormatter_Time_HH;

		} else if (formatName.equals(ValueFormat.TIME_HH_MM.name())) {

			return _valueFormatter_Time_HHMM;

		} else if (formatName.equals(ValueFormat.TIME_HH_MM_SS.name())) {

			return _valueFormatter_Time_HHMMSS;

		} else {

			// default

			return _valueFormatter_Time_HHMMSS;
		}
	}

	private static void parse_200_Calendars(final XMLMemento xmlRoot,
											final ArrayList<CalendarConfig> allCalendarConfigs) {

		if (xmlRoot == null) {
			return;
		}

		final XMLMemento xmlCalendars = (XMLMemento) xmlRoot.getChild(TAG_CALENDAR_CONFIG);

		if (xmlCalendars == null) {
			return;
		}

		_fromXml_ActiveCalendarConfigId = Util.getXmlString(xmlCalendars, ATTR_ACTIVE_CONFIG_ID, null);

		for (final IMemento mementoConfig : xmlCalendars.getChildren()) {

			final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

			try {

				final String xmlConfigType = xmlConfig.getType();

				if (xmlConfigType.equals(TAG_CALENDAR)) {

					// <Calendar>

					allCalendarConfigs.add(restoreConfig(xmlConfig));
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlConfig), e);
			}
		}
	}

	/**
	 * Read or create configuration a xml file
	 * 
	 * @return
	 */
	private static void readConfigFromXml() {

		InputStreamReader reader = null;

		try {

			XMLMemento xmlRoot = null;

			// try to get layer structure from saved xml file
			final File layerFile = getConfigXmlFile();
			final String absoluteLayerPath = layerFile.getAbsolutePath();

			final File inputFile = new File(absoluteLayerPath);
			if (inputFile.exists()) {

				try {

					reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
					xmlRoot = XMLMemento.createReadRoot(reader);

				} catch (final Exception e) {
					// ignore
				}
			}

			// parse xml
			parse_200_Calendars(xmlRoot, _allCalendarConfigs);

			// ensure config is created
			if (_allCalendarConfigs.size() == 0) {
				createConfig_All();
			}

			setActiveCalendarConfigIntern(getConfig_Calendar(), null);

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(reader);
		}
	}

	static void resetActiveCalendarConfiguration() {

		// do not replace the name
//		final String oldName = _activeCalendarConfig.name;

		final int activeCalendarConfigIndex = getActiveCalendarConfigIndex();

		// remove old config
		_allCalendarConfigs.remove(_activeCalendarConfig);

		// create new config
		final CalendarConfig newConfig = createConfig_FromId(_activeCalendarConfig.defaultId);
//		newConfig.name = oldName;

		// update model
		_allCalendarConfigs.add(activeCalendarConfigIndex, newConfig);
		setActiveCalendarConfigIntern(newConfig, null);
	}

	static void resetAllCalendarConfigurations() {

		createConfig_All();

		setActiveCalendarConfigIntern(_allCalendarConfigs.get(0), null);
	}

	private static CalendarConfig restoreConfig(final XMLMemento xmlConfig) {

		// !!! getFontData() MUST be created for EVERY font otherwise they use all the SAME font !!!
		final Font defaultFont = JFaceResources.getFontRegistry().defaultFont();

		final CalendarConfig config = new CalendarConfig();

// SET_FORMATTING_OFF
		
		// config
		config.id							= Util.getXmlString(xmlConfig,						ATTR_ID,						Long.toString(System.nanoTime()));
		config.name							= Util.getXmlString(xmlConfig,						ATTR_CONFIG_NAME,				UI.EMPTY_STRING);
		config.defaultId					= (ConfigDefault) Util.getXmlEnum(xmlConfig,		ATTR_CONFIG_DEFAULT_ID,			DEFAULT_CONFIG_DEFAULT_ID);
		
		// layout
		config.isToggleMonthColor			= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_TOGGLE_MONTH_COLOR,		true);
		config.useDraggedScrolling			= Util.getXmlBoolean(xmlConfig, 					ATTR_USE_DRAGGED_SCROLLING,		true);
		config.weekHeight					= Util.getXmlInteger(xmlConfig, 					ATTR_WEEK_HEIGHT,				DEFAULT_WEEK_HEIGHT);
		config.alternateMonthRGB			= Util.getXmlRgb(xmlConfig, 						TAG_ALTERNATE_MONTH_RGB,		DEFAULT_ALTERNATE_MONTH_RGB);
		config.calendarBackgroundRGB		= Util.getXmlRgb(xmlConfig, 						TAG_CALENDAR_BACKGROUND_RGB,	DEFAULT_CALENDAR_BACKGROUND_RGB);
		config.calendarForegroundRGB		= Util.getXmlRgb(xmlConfig, 						TAG_CALENDAR_FOREGROUND_RGB,	DEFAULT_CALENDAR_FOREBACKGROUND_RGB);
		
		// year columns
		config.isShowYearColumns			= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_YEAR_COLUMNS,		true);
		config.yearColumns					= Util.getXmlInteger(xmlConfig,						ATTR_YEAR_COLUMNS,				DEFAULT_YEAR_COLUMNS);
		config.yearColumnsSpacing			= Util.getXmlInteger(xmlConfig, 					ATTR_YEAR_COLUMNS_SPACING,		DEFAULT_YEAR_COLUMNS_SPACING);
		config.yearColumnsStart				= (ColumnStart) Util.getXmlEnum(xmlConfig,			ATTR_YEAR_COLUMNS_START,		DEFAULT_YEAR_COLUMNS_LAYOUT);
		config.yearHeaderFont				= Util.getXmlFont(xmlConfig, 						ATTR_YEAR_HEADER_FONT,			defaultFont.getFontData()[0]);
		
		// date column
		config.isShowDateColumn				= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_DATE_COLUMN,		true);
		config.dateColumnFont 				= Util.getXmlFont(xmlConfig, 						ATTR_DATE_COLUMN_FONT, 			defaultFont.getFontData()[0]);
		config.dateColumnWidth				= Util.getXmlInteger(xmlConfig, 					ATTR_DATE_COLUMN_WIDTH,			DEFAULT_DATE_COLUMN_WIDTH);
		config.dateColumnContent			= (DateColumnContent) Util.getXmlEnum(xmlConfig,	ATTR_DATE_COLUMN_CONTENT,		DateColumnContent.WEEK_NUMBER);

		// day date
		config.isHideDayDateWhenNoTour		= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR,		true);
		config.isShowDayDate				= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_DAY_DATE,					true);
		config.isShowDayDateWeekendColor	= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR,	DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR);
		config.dayDateFont 					= Util.getXmlFont(xmlConfig, 						ATTR_DAY_DATE_FONT, 					defaultFont.getFontData()[0]);
		config.dayDateFormat				= (DayDateFormat) Util.getXmlEnum(xmlConfig,		ATTR_DAY_DATE_FORMAT,					DEFAULT_DAY_DATE_FORMAT);
		                                    
		// day content
		config.tourBackgroundWidth			= Util.getXmlInteger(xmlConfig, 					ATTR_TOUR_BACKGROUND_WIDTH, 	DEFAULT_TOUR_BACKGROUND_WIDTH, 1, 100);
		config.tourBorderWidth				= Util.getXmlInteger(xmlConfig, 					ATTR_TOUR_BORDER_WIDTH,		 	DEFAULT_TOUR_BORDER_WIDTH, 1, 100);
		config.tourBackground 				= (TourBackground) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_BACKGROUND,			DEFAULT_TOUR_BACKGROUND);
		config.tourBackgroundColor1 		= (CalendarColor) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_BACKGROUND_COLOR1,	DEFAULT_TOUR_BACKGROUND_COLOR1);
		config.tourBackgroundColor2 		= (CalendarColor) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_BACKGROUND_COLOR2,	DEFAULT_TOUR_BACKGROUND_COLOR2);
		config.tourBorder 					= (TourBorder) Util.getXmlEnum(xmlConfig,			ATTR_TOUR_BORDER,				DEFAULT_TOUR_BORDER);
		config.tourBorderColor 				= (CalendarColor) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_BORDER_COLOR,			DEFAULT_TOUR_BORDER_COLOR);
		
		// tour content
		config.isShowTourContent			= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_TOUR_CONTENT,		true);
		config.isShowTourValueUnit			= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_TOUR_VALUE_UNIT,	true);
		config.isTruncateTourText			= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_TRUNCATE_TOUR_TEXT,		DEFAULT_IS_TRUNCATE_TOUR_TEXT);
		config.tourContentColor				= (CalendarColor) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_CONTENT_COLOR,		DEFAULT_TOUR_COLOR);
		config.tourContentFont 				= Util.getXmlFont(xmlConfig, 						ATTR_TOUR_CONTENT_FONT, 		defaultFont.getFontData()[0]);
		config.tourTitleColor				= (CalendarColor) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_TITLE_COLOR,			DEFAULT_TOUR_COLOR);
		config.tourTitleFont 				= Util.getXmlFont(xmlConfig, 						ATTR_TOUR_TITLE_FONT, 			defaultFont.getFontData()[0]);
		config.tourTruncatedLines			= Util.getXmlInteger(xmlConfig, 					ATTR_TOUR_TRUNCATED_LINES,	 	DEFAULT_TOUR_TRUNCATED_LINES, 1, 10);
		config.tourValueColumns				= Util.getXmlInteger(xmlConfig, 					ATTR_TOUR_VALUE_COLUMNS,	 	DEFAULT_TOUR_VALUE_COLUMNS, 1, 3);
		config.tourValueFont				= Util.getXmlFont(xmlConfig, 						ATTR_TOUR_VALUE_FONT, 			defaultFont.getFontData()[0]);
		config.tourValueColor				= (CalendarColor) Util.getXmlEnum(xmlConfig,		ATTR_TOUR_VALUE_COLOR,			DEFAULT_TOUR_COLOR);
		
		// week summary column
		config.isShowSummaryColumn			= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_SUMMARY_COLUMN,	true);
		config.isShowWeekValueUnit			= Util.getXmlBoolean(xmlConfig, 					ATTR_IS_SHOW_WEEK_VALUE_UNIT,	true);
		config.weekColumnWidth				= Util.getXmlInteger(xmlConfig, 					ATTR_WEEK_COLUMN_WIDTH,			DEFAULT_SUMMARY_COLUMN_WIDTH);
		config.weekValueColor		 		= (CalendarColor) Util.getXmlEnum(xmlConfig,		ATTR_WEEK_VALUE_COLOR,			DEFAULT_WEEK_VALUE_COLOR);
		config.weekValueFont				= Util.getXmlFont(xmlConfig, 						ATTR_WEEK_VALUE_FONT,			defaultFont.getFontData()[0]);

// SET_FORMATTING_ON

		// tour formatter
		final FormatterData[] tourFormatterData = restoreConfig_FormatterData(
				xmlConfig,
				TAG_ALL_TOUR_FORMATTER,
				config);
		if (tourFormatterData != null) {
			config.allTourFormatterData = tourFormatterData;
		}

		// week formatter
		final FormatterData[] weekFormatterData = restoreConfig_FormatterData(
				xmlConfig,
				TAG_ALL_WEEK_FORMATTER,
				config);
		if (weekFormatterData != null) {
			config.allWeekFormatterData = weekFormatterData;
		}

		return config;
	}

	private static FormatterData[] restoreConfig_FormatterData(	final XMLMemento xmlConfig,
																final String tagAllFormatter,
																final CalendarConfig config) {

		final XMLMemento xmlAllFormatter = (XMLMemento) xmlConfig.getChild(tagAllFormatter);
		if (xmlAllFormatter != null) {

			final ArrayList<FormatterData> allFormatterData = new ArrayList<>();

			for (final IMemento xmlFormatterData : xmlAllFormatter.getChildren()) {

				final boolean isEnabled = Util.getXmlBoolean(xmlFormatterData, ATTR_IS_SHOW_VALUE, true);

				final FormatterID id = (FormatterID) Util.getXmlEnum(
						xmlFormatterData,
						ATTR_FORMATTER_ID,
						FormatterID.EMPTY);

				final ValueFormat valueFormat = (ValueFormat) Util.getXmlEnum(
						xmlFormatterData,
						ATTR_FORMATTER_VALUE_FORMAT,
						ValueFormat.DUMMY_VALUE);

				allFormatterData.add(new FormatterData(isEnabled, id, valueFormat));
			}

			return allFormatterData.toArray(new FormatterData[allFormatterData.size()]);
		}

		return null;
	}

	private static void saveConfig(final CalendarConfig config, final IMemento xmlConfig) {

// SET_FORMATTING_OFF
					
		// config
		xmlConfig.putString(		ATTR_ID, 								config.id);
		xmlConfig.putString(		ATTR_CONFIG_NAME, 						config.name);
		Util.setXmlEnum(xmlConfig,	ATTR_CONFIG_DEFAULT_ID,			 		config.defaultId);
		
		// layout
		xmlConfig.putBoolean(		ATTR_IS_TOGGLE_MONTH_COLOR, 			config.isToggleMonthColor);
		xmlConfig.putBoolean(		ATTR_USE_DRAGGED_SCROLLING, 			config.useDraggedScrolling);
		xmlConfig.putInteger(		ATTR_WEEK_HEIGHT, 						config.weekHeight);
		Util.setXmlRgb(xmlConfig,	TAG_ALTERNATE_MONTH_RGB, 				config.alternateMonthRGB);
		Util.setXmlRgb(xmlConfig,	TAG_CALENDAR_BACKGROUND_RGB, 			config.calendarBackgroundRGB);
		Util.setXmlRgb(xmlConfig,	TAG_CALENDAR_FOREGROUND_RGB, 			config.calendarForegroundRGB);
		
		// year columns
		xmlConfig.putBoolean(		ATTR_IS_SHOW_YEAR_COLUMNS, 				config.isShowYearColumns);
		xmlConfig.putInteger(		ATTR_YEAR_COLUMNS, 						config.yearColumns);
		xmlConfig.putInteger(		ATTR_YEAR_COLUMNS_SPACING, 				config.yearColumnsSpacing);
		Util.setXmlEnum(xmlConfig,	ATTR_YEAR_COLUMNS_START, 				config.yearColumnsStart);
		Util.setXmlFont(xmlConfig,	ATTR_YEAR_HEADER_FONT, 					config.yearHeaderFont);
		
		// date column
		xmlConfig.putBoolean(		ATTR_IS_SHOW_DATE_COLUMN, 				config.isShowDateColumn);
		xmlConfig.putInteger(		ATTR_DATE_COLUMN_WIDTH, 				config.dateColumnWidth);
		Util.setXmlEnum(xmlConfig,	ATTR_DATE_COLUMN_CONTENT, 				config.dateColumnContent);
		Util.setXmlFont(xmlConfig,	ATTR_DATE_COLUMN_FONT, 					config.dateColumnFont);

		// day date
		xmlConfig.putBoolean(		ATTR_IS_HIDE_DAY_DATE_WHEN_NO_TOUR, 	config.isHideDayDateWhenNoTour);
		xmlConfig.putBoolean(		ATTR_IS_SHOW_DAY_DATE, 					config.isShowDayDate);
		xmlConfig.putBoolean(		ATTR_IS_SHOW_DAY_DATE_WEEKEND_COLOR, 	config.isShowDayDateWeekendColor);
		Util.setXmlEnum(xmlConfig,	ATTR_DAY_DATE_FORMAT, 					config.dayDateFormat);
		Util.setXmlFont(xmlConfig,	ATTR_DAY_DATE_FONT, 					config.dayDateFont);

		// tour background
		xmlConfig.putInteger(		ATTR_TOUR_BACKGROUND_WIDTH, 			config.tourBackgroundWidth);
		xmlConfig.putInteger(		ATTR_TOUR_BORDER_WIDTH, 				config.tourBorderWidth);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_BACKGROUND, 					config.tourBackground);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_BACKGROUND_COLOR1, 			config.tourBackgroundColor1);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_BACKGROUND_COLOR2, 			config.tourBackgroundColor2);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_BORDER, 						config.tourBorder);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_BORDER_COLOR, 				config.tourBorderColor);

		// tour content
		xmlConfig.putBoolean(		ATTR_IS_SHOW_TOUR_CONTENT,				config.isShowTourContent);
		xmlConfig.putBoolean(		ATTR_IS_SHOW_TOUR_VALUE_UNIT,			config.isShowTourValueUnit);
		xmlConfig.putBoolean(		ATTR_IS_TRUNCATE_TOUR_TEXT,				config.isTruncateTourText);
		xmlConfig.putInteger(		ATTR_TOUR_TRUNCATED_LINES, 				config.tourTruncatedLines);
		xmlConfig.putInteger(		ATTR_TOUR_VALUE_COLUMNS, 				config.tourValueColumns);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_CONTENT_COLOR, 				config.tourContentColor);
		Util.setXmlFont(xmlConfig,	ATTR_TOUR_CONTENT_FONT, 				config.tourContentFont);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_TITLE_COLOR, 					config.tourTitleColor);
		Util.setXmlFont(xmlConfig,	ATTR_TOUR_TITLE_FONT, 					config.tourTitleFont);
		Util.setXmlEnum(xmlConfig,	ATTR_TOUR_VALUE_FONT, 					config.tourValueColor);
		Util.setXmlFont(xmlConfig,	ATTR_TOUR_VALUE_FONT, 					config.tourValueFont);
		
		// week summary column
		xmlConfig.putBoolean(		ATTR_IS_SHOW_SUMMARY_COLUMN, 			config.isShowSummaryColumn);
		xmlConfig.putBoolean(		ATTR_IS_SHOW_WEEK_VALUE_UNIT, 			config.isShowWeekValueUnit);
		xmlConfig.putInteger(		ATTR_WEEK_COLUMN_WIDTH, 				config.weekColumnWidth);
		Util.setXmlEnum(xmlConfig,	ATTR_WEEK_VALUE_COLOR, 					config.weekValueColor);
		Util.setXmlFont(xmlConfig,	ATTR_WEEK_VALUE_FONT, 					config.weekValueFont);
					
// SET_FORMATTING_ON

		// formatter
		saveConfig_FormatterData(xmlConfig.createChild(TAG_ALL_TOUR_FORMATTER), config.allTourFormatterData);
		saveConfig_FormatterData(xmlConfig.createChild(TAG_ALL_WEEK_FORMATTER), config.allWeekFormatterData);
	}

	private static void saveConfig_FormatterData(	final IMemento xmlAllTourFormatter,
													final FormatterData[] allFormatterData) {

		for (final FormatterData formatterData : allFormatterData) {

			final IMemento xmlFormatter = xmlAllTourFormatter.createChild(TAG_FORMATTER);

			xmlFormatter.putBoolean(ATTR_IS_SHOW_VALUE, formatterData.isEnabled);
			Util.setXmlEnum(xmlFormatter, ATTR_FORMATTER_ID, formatterData.id);
			Util.setXmlEnum(xmlFormatter, ATTR_FORMATTER_VALUE_FORMAT, formatterData.valueFormat);
		}
	}

	static void saveState() {

		if (_activeCalendarConfig == null) {

			// this can happen when not yet used

			return;
		}

		final XMLMemento xmlRoot = create_Root();

		saveState_Calendars(xmlRoot);

		Util.writeXml(xmlRoot, getConfigXmlFile());
	}

	/**
	 * Calendars
	 */
	private static void saveState_Calendars(final XMLMemento xmlRoot) {

		final IMemento xmlCalendars = xmlRoot.createChild(TAG_CALENDAR_CONFIG);
		{
			xmlCalendars.putString(ATTR_ACTIVE_CONFIG_ID, _activeCalendarConfig.id);

			for (final CalendarConfig config : _allCalendarConfigs) {
				saveState_Calendars_Config(config, xmlCalendars);
			}
		}
	}

	private static void saveState_Calendars_Config(final CalendarConfig config, final IMemento xmlCalendars) {

		// <Calendar>
		final IMemento xmlConfig = xmlCalendars.createChild(TAG_CALENDAR);
		{
			saveConfig(config, xmlConfig);
		}
	}

	static void setActiveCalendarConfig(final CalendarConfig selectedConfig,
										final ICalendarConfigProvider configProvider) {

		setActiveCalendarConfigIntern(selectedConfig, configProvider);
	}

	private static void setActiveCalendarConfigIntern(	final CalendarConfig calendarConfig,
														final ICalendarConfigProvider configProvider) {

		_activeCalendarConfig = calendarConfig;

		// update config listener/provider
		if (configProvider != null) {

			if (_configProvider_CalendarView != null && _configProvider_CalendarView != configProvider) {
				_configProvider_CalendarView.updateUI_CalendarConfig();
			}

			if (_configProvider_SlideoutCalendarOptions != null
					&& _configProvider_SlideoutCalendarOptions != configProvider) {
				_configProvider_SlideoutCalendarOptions.updateUI_CalendarConfig();
			}
		}
	}

	static void setConfigProvider(final CalendarView calendarView) {
		_configProvider_CalendarView = calendarView;
	}

	static void setConfigProvider(final SlideoutCalendarOptions slideoutCalendarOptions) {
		_configProvider_SlideoutCalendarOptions = slideoutCalendarOptions;
	}

	/**
	 * Update value formatter
	 */
	static void updateFormatterValueFormat() {

		/*
		 * Tour formatter
		 */
		for (final FormatterData formatterData : _activeCalendarConfig.allTourFormatterData) {

			if (!formatterData.isEnabled) {
				continue;
			}

			final ValueFormat valueFormat = formatterData.valueFormat;

			switch (formatterData.id) {

			case ALTITUDE:
				_tourFormatter_Altitude.setValueFormat(valueFormat);
				break;

			case DISTANCE:
				_tourFormatter_Distance.setValueFormat(valueFormat);
				break;

			case PACE:
				_tourFormatter_Pace.setValueFormat(valueFormat);
				break;

			case SPEED:
				_tourFormatter_Speed.setValueFormat(valueFormat);
				break;

			case TIME_MOVING:
				_tourFormatter_Time_Moving.setValueFormat(valueFormat);
				break;

			case TIME_PAUSED:
				_tourFormatter_Time_Paused.setValueFormat(valueFormat);
				break;

			case TIME_RECORDING:
				_tourFormatter_Time_Recording.setValueFormat(valueFormat);
				break;

			default:
				break;
			}
		}

		/*
		 * Week formatter
		 */
		for (final FormatterData formatterData : _activeCalendarConfig.allWeekFormatterData) {

			if (!formatterData.isEnabled) {
				continue;
			}

			final ValueFormat valueFormat = formatterData.valueFormat;

			switch (formatterData.id) {

			case ALTITUDE:
				_weekFormatter_Altitude.setValueFormat(valueFormat);
				break;

			case DISTANCE:
				_weekFormatter_Distance.setValueFormat(valueFormat);
				break;

			case PACE:
				_weekFormatter_Pace.setValueFormat(valueFormat);
				break;

			case SPEED:
				_weekFormatter_Speed.setValueFormat(valueFormat);
				break;

			case TIME_MOVING:
				_weekFormatter_Time_Moving.setValueFormat(valueFormat);
				break;

			case TIME_PAUSED:
				_weekFormatter_Time_Paused.setValueFormat(valueFormat);
				break;

			case TIME_RECORDING:
				_weekFormatter_Time_Recording.setValueFormat(valueFormat);
				break;

			default:
				break;
			}
		}
	}

}
