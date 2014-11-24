/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import net.tourbook.common.UI;
import net.tourbook.web.WEB;
import net.tourbook.web.WebContentServer;

import org.eclipse.swt.browser.Browser;

public class Search {

	static final String	SEARCH_FOLDER	= "/search/";	//$NON-NLS-1$

	private Browser		_browser;

	Search(final Browser browser) {

		// ensure web server is started
		WebContentServer.init();

		_browser = browser;

		final String searchUrl = WEB.SERVER_URL + SEARCH_FOLDER + "search.html";

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\t" + searchUrl));
		// TODO remove SYSTEM.OUT.PRINTLN

//		_browser.setUrl("http://dojotoolkit.org/api/");
		_browser.setUrl(searchUrl);
	}

	void setFocus() {
		_browser.setFocus();
	}

}
