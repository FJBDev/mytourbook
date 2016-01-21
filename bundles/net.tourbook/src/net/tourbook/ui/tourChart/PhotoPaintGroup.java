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
/**
 * @author Wolfgang Schramm Created: 16.9.2012
 */
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Point;

public class PhotoPaintGroup {

	ArrayList<Integer>	photoIndex	= new ArrayList<Integer>();

	Point				groupCenterPosition;

	int					hGridStart;
	int					hGridEnd;

	int					paintedGroupDevX;
	int					paintedGroupDevY;
	int					paintedTextDevX;
	int					paintedTextDevY;

	int					paintedGroupWidth;
	int					paintedGroupHeight;

	String				paintedGroupText;

	void addPhoto(final int positionIndex) {
		photoIndex.add(positionIndex);
	}

	@Override
	public String toString() {
		return "PhotoPaintGroup [" //$NON-NLS-1$
				+ ("hGridStart=" + hGridStart + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("hGridEnd=" + hGridEnd + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("paintedGroupText=" + paintedGroupText) //$NON-NLS-1$
				+ "]"; //$NON-NLS-1$
	}
}
