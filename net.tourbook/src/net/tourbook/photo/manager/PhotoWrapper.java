/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.manager;

import java.io.File;

/**
 * Wrapper for a photo image file, sorting and filtering attributes and the {@link Photo} itself.
 */
public class PhotoWrapper {

	/**
	 * Photo image file
	 */
	public File		imageFile;

	public String	imageFileName;
	public String	imageFilePathName;
	public long		imageFileLastModified;

	public Photo	photo;

	/**
	 * GPS has three states:
	 * 
	 * <pre>
	 * -1 state is not yet set
	 *  0 photo do not contain GPS data
	 *  1 photo contains GPS data
	 * </pre>
	 */
	public int		gpsState	= -1;

	public int		wrapperIndex;

	public PhotoWrapper(final File file) {

		imageFile = file;

		imageFileName = imageFile.getName();
		imageFilePathName = imageFile.getPath();
		imageFileLastModified = imageFile.lastModified();
	}

	@Override
	public String toString() {
		return photo.toString();
	}

}
