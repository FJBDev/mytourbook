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
package net.tourbook.photo;

import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.graphics.GC;

public class NoGroupRendererMT extends NoGroupRenderer {


	@Override
	public void draw(	final GC gc,
						final GalleryItem group,
						final int x,
						final int y,
						final int clipX,
						final int clipY,
						final int clipWidth,
						final int clipHeight) {

		// get items in the clipping area
		final int[] visibleIndexes = getVisibleItems(group, x, y, clipX, clipY, clipWidth, clipHeight, OFFSET);

		if (visibleIndexes != null && visibleIndexes.length > 0) {

			for (int i = visibleIndexes.length - 1; i >= 0; i--) {

				// Draw item
				final GalleryItem galleryItem = group.getItem(visibleIndexes[i]);

				final boolean isSelected = group.isSelected(galleryItem);

				drawItem(gc, visibleIndexes[i], isSelected, group, OFFSET);
			}
		}
	}

}
