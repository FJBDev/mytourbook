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

import net.tourbook.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class CalendarConfig {

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

// SET_FORMATTING_OFF
	
	// config
	String					id							= Long.toString(System.nanoTime());
	ConfigDefault			defaultId					= CalendarConfigManager.DEFAULT_CONFIG_DEFAULT_ID;
	String					name						= Messages.Calendar_Config_Name_Default;
	
	// layout
	boolean					isToggleMonthColor			= true;
	boolean					useDraggedScrolling			= false;
	RGB 					alternateMonthRGB			= CalendarConfigManager.DEFAULT_ALTERNATE_MONTH_RGB;
	RGB						calendarBackgroundRGB		= CalendarConfigManager.DEFAULT_CALENDAR_BACKGROUND_RGB;
	RGB						calendarForegroundRGB		= CalendarConfigManager.DEFAULT_CALENDAR_FOREBACKGROUND_RGB;
	int						weekHeight					= CalendarConfigManager.DEFAULT_WEEK_HEIGHT;
	
	// year columns
	boolean 				isShowYearColumns			= true;
	int						yearColumns					= CalendarConfigManager.DEFAULT_YEAR_COLUMNS;
	int 					yearColumnsSpacing			= CalendarConfigManager.DEFAULT_YEAR_COLUMNS_SPACING;
	ColumnStart				yearColumnsStart			= CalendarConfigManager.DEFAULT_YEAR_COLUMNS_LAYOUT;
	FontData 				yearHeaderFont				= createFont(2.2f, SWT.BOLD);
	
	// date column
	boolean					isShowDateColumn			= true;
	DateColumnContent		dateColumnContent			= CalendarConfigManager.DEFAULT_DATE_COLUMN_CONTENT;
	FontData				dateColumnFont				= createFont(1.5f, SWT.BOLD);
	int						dateColumnWidth				= CalendarConfigManager.DEFAULT_DATE_COLUMN_WIDTH;
	
	// day date
	boolean					isHideDayDateWhenNoTour		= false;
	boolean					isShowDayDate				= true;
	boolean 				isShowDayDateWeekendColor	= CalendarConfigManager.DEFAULT_IS_SHOW_DAY_DATE_WEEKEND_COLOR;
	FontData				dayDateFont					= createFont(1.2f, SWT.BOLD);
	DayDateFormat			dayDateFormat				= CalendarConfigManager.DEFAULT_DAY_DATE_FORMAT;
	
	// tour background
	TourBackground			tourBackground				= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND;
	CalendarColor			tourBackgroundColor1		= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND_COLOR1;
	CalendarColor			tourBackgroundColor2		= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND_COLOR2;
	int						tourBackgroundWidth			= CalendarConfigManager.DEFAULT_TOUR_BACKGROUND_WIDTH;
	TourBorder 				tourBorder					= CalendarConfigManager.DEFAULT_TOUR_BORDER;
	CalendarColor 			tourBorderColor				= CalendarConfigManager.DEFAULT_TOUR_BORDER_COLOR;
	int		 				tourBorderWidth				= CalendarConfigManager.DEFAULT_TOUR_BORDER_WIDTH;

	// tour content
	boolean 				isShowTourContent			= true;
	boolean					isShowTourValueUnit			= true;
	boolean					isTruncateTourText			= CalendarConfigManager.DEFAULT_IS_TRUNCATE_TOUR_TEXT;
	FormatterData[]			allTourFormatterData		= CalendarConfigManager.DEFAULT_TOUR_FORMATTER_DATA;
	CalendarColor			tourContentColor			= CalendarConfigManager.DEFAULT_DAY_CONTENT_COLOR;
	FontData				tourContentFont				= createFont(0.9f, SWT.NORMAL);
	CalendarColor 			tourTitleColor				= CalendarConfigManager.DEFAULT_DAY_CONTENT_COLOR;
	FontData				tourTitleFont				= createFont(1.2f, SWT.BOLD);
	int						tourTruncatedLines			= CalendarConfigManager.DEFAULT_TOUR_TRUNCATED_LINES;
	int 					tourValueColumns			= CalendarConfigManager.DEFAULT_TOUR_VALUE_COLUMNS;

	// week summary column
	boolean					isShowSummaryColumn			= true;
	boolean 				isShowWeekValueUnit			= true;
	FormatterData[]			allWeekFormatterData		= CalendarConfigManager.DEFAULT_WEEK_FORMATTER_DATA;
	int						weekColumnWidth				= CalendarConfigManager.DEFAULT_SUMMARY_COLUMN_WIDTH;
	CalendarColor			weekValueColor				= CalendarConfigManager.DEFAULT_WEEK_VALUE_COLOR;
	FontData 				weekValueFont				= createFont(1.2f, SWT.BOLD);

// SET_FORMATTING_ON

	/**
	 * @param relSize
	 * @param style
	 * @return
	 */
	private FontData createFont(final float relSize, final int style) {

		final Display display = Display.getDefault();

		// !!! getFontData() MUST be created for EVERY font otherwise they use all the SAME font !!!
		final FontData[] fontData = display.getSystemFont().getFontData();

		for (final FontData element : fontData) {

			element.setHeight((int) (element.getHeight() * relSize));
			element.setStyle(style);

			break;
		}

		return fontData[0];
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final CalendarConfig other = (CalendarConfig) obj;

		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());

		return result;
	}

	@Override
	public String toString() {
		return "CalendarConfig [name=" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
