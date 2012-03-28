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

import net.tourbook.photo.gallery.DefaultGalleryItemRenderer;
import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.photo.gallery.RendererHelper;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Paint image in the gallery canvas.
 * <p>
 * Original: org.sharemedia.utils.gallery.ShareMediaIconRenderer2
 */
public class PhotoRenderer extends DefaultGalleryItemRenderer {

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void draw(	final GC gc,
						final GalleryMTItem galleryItem,
						final int index,
						final int devXGallery,
						final int devYGallery,
						final int galleryItemWidth,
						final int galleryItemHeight) {

		final Object itemData = galleryItem.getData();
		if ((itemData instanceof Photo) == false) {
			return;
		}

		final Photo photo = (Photo) itemData;

		final int requestedImageQuality = galleryItemHeight > PhotoManager.THUMBNAIL_DEFAULT_SIZE
				? PhotoManager.IMAGE_QUALITY_HQ_1000
				: PhotoManager.IMAGE_QUALITY_THUMB_160;

		// get image with requested size
		Image photoImage = PhotoImageCache.getImage(photo.getImageKey(requestedImageQuality));

		if (photoImage == null) {

			// requested size is not available

			final int lowerImageQuality = galleryItemHeight > PhotoManager.THUMBNAIL_DEFAULT_SIZE
					? PhotoManager.IMAGE_QUALITY_THUMB_160
					: PhotoManager.IMAGE_QUALITY_HQ_1000;

			photoImage = PhotoImageCache.getImage(photo.getImageKey(lowerImageQuality));
		}

		final int imageBorderWidth = 2;
		final int roundingArc = 8;

		int imageWidth = 0;
		int imageHeight = 0;
		int useableHeight = galleryItemHeight;
		int fontHeight = 0;
		boolean isText = galleryItem.getText() != null && isShowLabels();
		if (galleryItemHeight <= 100) {
			isText = false;
		}
		if (isText) {
			fontHeight = gc.getFontMetrics().getHeight();
			useableHeight -= fontHeight + 2;
		}

		if (selected) {
			// draw selection
			gc.setForeground(getSelectionForegroundColor());
			gc.setBackground(getSelectionBackgroundColor());
		} else {
			gc.setForeground(getForegroundColor());
			gc.setBackground(getBackgroundColor());
		}

		if (photoImage != null && photoImage.isDisposed() == false) {

			// draw image

			/*
			 * exception can occure because the image could be disposed before it is drawn
			 */
			try {

				final Rectangle imageBounds = photoImage.getBounds();
				imageWidth = imageBounds.width;
				imageHeight = imageBounds.height;

				final int photoWidth = photo.getWidth();
				final int photoHeight = photo.getHeight();
				if (photoWidth != Integer.MIN_VALUE && photoHeight != Integer.MIN_VALUE) {

//					if (imageWidth > photoWidth || imageHeight > photoHeight) {
//
//						/*
//						 * photo image should not be displayed larger than the original photo even
//						 * when the thumb image is larger, this can happen when image is resized
//						 */
//
//						imageWidth = photoWidth;
//						imageHeight = photoHeight;
//
//					} else if (photoWidth > imageWidth && photoHeight > imageHeight) {
//
//						/*
//						 * photo is larger than the thumb, draw thumb larger with photo size, the
//						 * thumb will be blured until the original image is loaded
//						 */
////						imageWidth = galleryItemWidth;
////						imageHeight = galleryItemHeight;
//					}
				}

				final Point bestSize = RendererHelper.getBestSize(//
						imageWidth,
						imageHeight,
						galleryItemWidth - imageBorderWidth,
						useableHeight - imageBorderWidth);

				int bestWidth = bestSize.x;
				int bestHeight = bestSize.y;

				if (bestWidth > imageWidth || bestHeight > imageHeight) {
					bestWidth = imageWidth;
					bestHeight = imageHeight;
				}

				// Draw image
				if (bestWidth > 0 && bestHeight > 0) {

//					System.out.println(imageWidth + "x" + imageHeight + " - " + bestWidth + "x" + bestHeight);
//					// TODO remove SYSTEM.OUT.PRINTLN

					final int xShiftSrc = galleryItemWidth - bestWidth;
					final int yShiftSrc = useableHeight - bestHeight;

					final int xShift = xShiftSrc >> 1;
					final int yShift = yShiftSrc >> 1;

					gc.drawImage(photoImage, //
							0,
							0,
							imageWidth,
							imageHeight,
							//
							devXGallery + xShift,
							devYGallery + yShift,
							bestWidth,
							bestHeight);
//					setForegroundColor(gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
//					setBackgroundColor(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
//					drawText(gc, devXGallery, devYGallery, photo, false, false);
				}
			} catch (final Exception e) {
				drawText(gc, devXGallery, devYGallery, photo, e.getMessage(), false);
				StatusUtil.log(e);
			}
		} else {

			// image is not available

			drawText(gc, devXGallery, devYGallery, photo, null, true);
		}

		// Draw label
		if (isText) {

			// Set colors
			if (selected) {
				// Selected : use selection colors.
				gc.setForeground(getSelectionForegroundColor());
				gc.setBackground(getSelectionBackgroundColor());
			} else {
				// Not selected, use item values or defaults.

				// Background
//				if (itemBackgroundColor != null) {
//					gc.setBackground(itemBackgroundColor);
//				} else {
//					gc.setBackground(getBackgroundColor());
//				}
				gc.setBackground(getBackgroundColor());

				// Foreground
//				if (itemForegroundColor != null) {
//					gc.setForeground(itemForegroundColor);
//				} else {
//					gc.setForeground(getForegroundColor());
//				}
				gc.setForeground(getForegroundColor());
			}

			// Create label

			// RendererHelper IS A PERFORMANCE HOG when small images are displayed

//			final String text = RendererHelper.createLabel(galleryItem.getText(), gc, galleryItemWidth - 10);
			final String text = galleryItem.getText();

			// Center text
			final int textWidth = gc.textExtent(text).x;
			final int textxShift = (galleryItemWidth - (textWidth > galleryItemWidth ? galleryItemWidth : textWidth)) >> 1;

			// Draw
			gc.drawText(text, devXGallery + textxShift, devYGallery + galleryItemHeight - fontHeight, true);
		}

//		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//		gc.drawRectangle(devXGallery, devYGallery, galleryItemWidth - 2, galleryItemHeight - 2);
	}

	private void drawText(	final GC gc,
							final int x,
							final int y,
							final Photo photo,
							final String errorMessage,
							final boolean isLoading) {

		String photoData = UI.EMPTY_STRING;

		if (errorMessage != null) {
			photoData = "Error " + errorMessage + " in ";
			setForegroundColor(gc.getDevice().getSystemColor(SWT.COLOR_RED));
			setBackgroundColor(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		} else if (isLoading) {
			photoData = "Loading ";
			setForegroundColor(gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
			setBackgroundColor(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		}
		photoData += photo.getFileName();

		gc.drawText(photoData, x, y);
	}
}
