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
package net.tourbook.photo;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.ui.IViewPart;

public class PhotoManager {

	private static PhotoManager			_instance;

	private static final ListenerList	_photoEventListeners	= new ListenerList(ListenerList.IDENTITY);

	private PhotoManager() {}

	public static void addPhotoEventListener(final IPhotoEventListener listener) {
		_photoEventListeners.add(listener);
	}

	public static void firePhotoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

//		System.out.println(UI.timeStampNano() + " PhotoManager\tfireEvent\t" + data.getClass().getSimpleName());
//		// TODO remove SYSTEM.OUT.PRINTLN

		final Object[] allListeners = _photoEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((IPhotoEventListener) listener).photoEvent(viewPart, photoEventId, data);
		}
	}

	public static PhotoManager getInstance() {

		if (_instance == null) {
			_instance = new PhotoManager();
		}

		return _instance;
	}

	public static void removePhotoEventListener(final IPhotoEventListener listener) {
		if (listener != null) {
			_photoEventListeners.remove(listener);
		}
	}

}
