/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ext.velocity;

import java.util.Properties;

import net.tourbook.util.StatusUtil;

import org.apache.velocity.app.Velocity;

public class VelocityService {

	private static VelocityService	_instance;

	private static boolean			_isInitialized	= false;

	public static void init() {

		if (_isInitialized) {
			return;
		}

		Properties veloProp = new Properties();
		try {
			veloProp.load(instance().getClass().getResourceAsStream("/velocity.properties")); //$NON-NLS-1$
			Velocity.init(veloProp);

			_isInitialized = true;

		} catch (Exception ex) {
			StatusUtil.log(ex);
		}
	}

	private VelocityService() {}

	private static VelocityService instance() {

		if (_instance == null) {
			_instance = new VelocityService();
		}
		return _instance;
	}

}
