/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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

import java.util.concurrent.CopyOnWriteArrayList;

import net.tourbook.common.util.Util;

public class TourLogManager {

	public static final String							LOG_DELETE_TOURS	= "Delete tours";				//$NON-NLS-1$
	public static final String							LOG_SAVE_TOURS		= "Save tours";				//$NON-NLS-1$

	private static final CopyOnWriteArrayList<TourLog>	_tourLogs			= new CopyOnWriteArrayList<>();

	private static TourLogView							_logView;

	private static void addLog(final TourLog tourLog) {

		// update model
		_tourLogs.add(tourLog);

		// update UI
		if (isTourLogOpen()) {
			_logView.addLog(tourLog);
		}
	}

	public static void addLog(final TourLogState logState, final String message) {

		final TourLog tourLog = new TourLog(logState, message);

		addLog(tourLog);
	}

	public static void addLog(final TourLogState logState, final String message, final String css) {

		final TourLog tourLog = new TourLog(logState, message);

		tourLog.css = css;

		addLog(tourLog);
	}

	public static void addSubLog(final TourLogState logState, final String message) {

		final TourLog tourLog = new TourLog(logState, message);

		tourLog.isSubLogItem = true;

		addLog(tourLog);
	}

	public static void clear() {

		_logView.clear();
		_tourLogs.clear();
	}

	public static CopyOnWriteArrayList<TourLog> getLogs() {

		return _tourLogs;
	}

	private static boolean isTourLogOpen() {

		final boolean isLogViewOpen = _logView != null && _logView.isDisposed() == false;

		return isLogViewOpen;
	}

	public static void openLogView() {

		if (_logView == null || _logView.isDisposed()) {

			_logView = (TourLogView) Util.showView(TourLogView.ID, true);
		}
	}

	public static void setLogView(final TourLogView tourLogView) {

		_logView = tourLogView;
	}
}
