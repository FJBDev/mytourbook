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

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import net.tourbook.photo.PicDirGallery;
import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr;

public class PhotoImageLoaderItem {

	private static final ReentrantLock	RESIZE_LOCK		= new ReentrantLock();

	Photo								photo;
	private GalleryMTItem				_galleryItem;
	int									galleryIndex;
	int									imageQuality;
	private String						_imageKey;

	Display								_display		= Display.getDefault();

	private ILoadCallBack				_loadCallBack;

	private static int					_checkCounter	= 0;

	public PhotoImageLoaderItem(final GalleryMTItem galleryItem,
								final Photo photo,
								final int imageQuality,
								final ILoadCallBack loadCallBack) {

		_galleryItem = galleryItem;
		this.photo = photo;
		this.imageQuality = imageQuality;
		_loadCallBack = loadCallBack;

		galleryIndex = photo.getGalleryIndex();
		_imageKey = photo.getImageKey(imageQuality);
	}

	private Image getStoreImageAWT(final Photo photo, final int requestedImageQuality) {

		IPath storeImageFilePath = null;

		storeImageFilePath = ThumbnailStore.getStoreImagePath(photo, requestedImageQuality);

		// check if image is available
		final File storeImageFile = new File(storeImageFilePath.toOSString());
		if (storeImageFile.isFile()) {

			// photo image is available in the thumbnail store

			return new Image(_display, storeImageFilePath.toOSString());
		}

		try {

			// load full size image
			final long startLoadAWT = System.currentTimeMillis();

			final BufferedImage img = ImageIO.read(photo.getImageFile());
			if (img == null) {
				StatusUtil.log(NLS.bind("Image \"{0}\" cannot be loaded", photo.getFilePathName())); //$NON-NLS-1$
				return null;
			}
			final long endLoadAWT = System.currentTimeMillis();

//			RESIZE_LOCK.lock();
			long startSaveAWT = 0;
			long endSaveAWT = 0;
			long endResize = 0;
			long startResize = 0;
			{
				final int thumbSize = PhotoManager.IMAGE_SIZE[imageQuality];

				final Point bestSize = ImageUtils.getBestSize(//
						new Point(img.getWidth(), img.getHeight()),
						new Point(thumbSize, thumbSize));

				try {
					final boolean isResizeRequired = ImageUtils.isResizeRequiredAWT(img, bestSize.x, bestSize.y);

					if (isResizeRequired) {

						startResize = System.currentTimeMillis();

						final BufferedImage scaledImg = Scalr.resize(img, Math.max(bestSize.x, bestSize.y));

						endResize = System.currentTimeMillis();
						startSaveAWT = System.currentTimeMillis();

						ThumbnailStore.saveImageAWT(scaledImg, storeImageFilePath);

						endSaveAWT = System.currentTimeMillis();

					}
				} catch (final Exception e) {
					StatusUtil.log(NLS.bind("Image \"{0}\" cannot be resized", photo.getFilePathName()), e); //$NON-NLS-1$
					return null;

				} finally {
//					RESIZE_LOCK.unlock();
				}
			}

			final long startLoadSWT = System.currentTimeMillis();

			final Image thumbnailImage = new Image(_display, storeImageFilePath.toOSString());

			final long endLoadSWT = System.currentTimeMillis();

			System.out.println((Thread.currentThread().getName() + "\t")
					+ photo.getFileName()
					+ "\tload:\t"
					+ ((endLoadAWT - startLoadAWT) + "")
					+ "\tresize:\t"
					+ ((endResize - startResize) + "")
					+ "\tsave AWT:\t"
					+ ((endSaveAWT - startSaveAWT) + "")
					+ "\tload SWT:\t"
					+ ((endLoadSWT - startLoadSWT) + "")
					+ "\ttotal:\t"
					+ ((endLoadSWT - startLoadAWT) + "")
			//
					);
			// TODO remove SYSTEM.OUT.PRINTLN

			return thumbnailImage;

		} catch (final Exception e) {
			StatusUtil.log(NLS.bind("Store image \"{0}\" cannot be created", storeImageFilePath.toOSString()), e); //$NON-NLS-1$
		}

		return null;
	}

	private Image getStoreImageSWT(final Photo photo, final int requestedImageQuality) {

		IPath storeImageFilePath = null;

		storeImageFilePath = ThumbnailStore.getStoreImagePath(photo, requestedImageQuality);

		// check if image is available
		final File storeImageFile = new File(storeImageFilePath.toOSString());
		if (storeImageFile.isFile()) {

			// photo image is available in the thumbnail store

			return new Image(_display, storeImageFilePath.toOSString());
		}

		// load full size image
		final long startLoading = System.currentTimeMillis();

		final String fullSizePathName = photo.getFilePathName();
		Image fullSizeImage = null;

		long endLoading;
		try {

			fullSizeImage = new Image(_display, fullSizePathName);

		} catch (final Exception e) {
			StatusUtil.log(NLS.bind("Fullsize image \"{0}\" cannot be loaded", fullSizePathName), e); //$NON-NLS-1$
		} finally {
			endLoading = System.currentTimeMillis();

			if (fullSizeImage == null) {
				StatusUtil.log(NLS.bind(//
						"Fullsize image \"{0}\" cannot be loaded",
						fullSizePathName), new Exception());
				return null;
			}
		}

		try {

			Image thumbnailImage = null;

			long startSave = 0;
			long endSave = 0;
			long endResize = 0;

			final Rectangle fullSizeImageBounds = fullSizeImage.getBounds();
			final int thumbSize = PhotoManager.IMAGE_SIZE[imageQuality];

			final Point bestSize = ImageUtils.getBestSize(//
					new Point(fullSizeImageBounds.width, fullSizeImageBounds.height),
					new Point(thumbSize, thumbSize));

			final long startResize = System.currentTimeMillis();
			final boolean isResizeRequired = !(fullSizeImageBounds.width == bestSize.x && fullSizeImageBounds.height == bestSize.y);

			if (isResizeRequired) {
//				RESIZE_LOCK.lock();
				{
					try {

						thumbnailImage = ImageUtils.resize(fullSizeImage, bestSize.x, bestSize.y);

					} catch (final Exception e) {
						StatusUtil.log(NLS.bind("Image \"{0}\" cannot be resized", fullSizePathName), e); //$NON-NLS-1$
						return null;

					} finally {
						fullSizeImage.dispose();
//						RESIZE_LOCK.unlock();
					}
				}

				endResize = System.currentTimeMillis();

				startSave = System.currentTimeMillis();

				ThumbnailStore.saveImageSWT(thumbnailImage, storeImageFilePath);

				endSave = System.currentTimeMillis();
			} else {
				thumbnailImage = fullSizeImage;
			}

			System.out.println((Thread.currentThread().getName() + "\t")
					+ photo.getFileName()
					+ "\tload:\t"
					+ ((endLoading - startLoading) + "")
					+ "\tresize:\t"
					+ ((endResize - startResize) + "")
					+ "\tsave:\t"
					+ ((endSave - startSave) + "")
					+ "\ttotal:\t"
					+ ((endSave - startLoading) + "")
			//
					);
			// TODO remove SYSTEM.OUT.PRINTLN

			return thumbnailImage;

		} catch (final Exception e) {
			StatusUtil.log(NLS.bind("Store image \"{0}\" cannot be created", storeImageFilePath.toOSString()), e); //$NON-NLS-1$
		}

		return null;
	}

	/**
	 * check if the image is still visible
	 * 
	 * @return
	 */
	private boolean isImageVisible() {

		if (_display.isDisposed()) {
			// this happened
			return false;
		}

		final PicDirGallery gallery = (PicDirGallery) _galleryItem.getParent();
		final Rectangle[] galleryItemBoundses = { null };
		final Rectangle[] clientAreas = { null };

		_display.syncExec(new Runnable() {
			public void run() {
				if (_galleryItem.isDisposed()) {
					return;
				}
				galleryItemBoundses[0] = _galleryItem.getBounds();
				clientAreas[0] = gallery.getClientArea();
			}
		});

		final Rectangle galleryItemBounds = galleryItemBoundses[0];

		final Rectangle clientArea = clientAreas[0];
		final int translate = gallery.getTranslate();

		final int itemTop = galleryItemBounds.y;
		final int itemBottom = itemTop + galleryItemBounds.height;
		final int visibleBottom = translate + clientArea.height;

		if (itemBottom < 0 || itemTop > visibleBottom) {

			// item is not visible

			resetState();
			return false;
		}

		return true;
	}

	/**
	 * This is called from the executor when the task is starting
	 * 
	 * <pre>
	 * 
	 * 2 Threads
	 * 
	 * SWT
	 * Photo-Image-Loader-1	IMG_1219_10.JPG	load:	1165	resize:	645	save:	110	total:	1920
	 * Photo-Image-Loader-0	IMG_1219_9.JPG	load:	1165	resize:	650	save:	110	total:	1925
	 * Photo-Image-Loader-1	IMG_1219.JPG	load:	566		resize:	875	save:	60	total:	1501
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	835		resize:	326	save:	55	total:	1216
	 * Photo-Image-Loader-1	IMG_1219_3.JPG	load:	1150	resize:	625	save:	55	total:	1830
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	565		resize:	630	save:	60	total:	1255
	 * Photo-Image-Loader-1	IMG_1219_5.JPG	load:	566		resize:	880	save:	60	total:	1506
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	845		resize:	341	save:	65	total:	1251
	 * Photo-Image-Loader-1	IMG_1219_7.JPG	load:	575		resize:	875	save:	50	total:	1500
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	845		resize:	356	save:	45	total:	1246
	 * 												8277			6203		670			15150
	 * 
	 * 
	 * AWT
	 * Photo-Image-Loader-1	IMG_1219_9.JPG	load:	1005	resize:	770		save AWT:	25	load SWT:	10	total:	1810
	 * Photo-Image-Loader-0	IMG_1219_10.JPG	load:	1015	resize:	1311	save AWT:	145	load SWT:	5	total:	2476
	 * Photo-Image-Loader-1	IMG_1219.JPG	load:	931		resize:	755		save AWT:	65	load SWT:	5	total:	1756
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	960		resize:	737		save AWT:	30	load SWT:	5	total:	1732
	 * Photo-Image-Loader-1	IMG_1219_3.JPG	load:	1340	resize:	700		save AWT:	25	load SWT:	10	total:	2075
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	935		resize:	751		save AWT:	25	load SWT:	10	total:	1721
	 * Photo-Image-Loader-1	IMG_1219_5.JPG	load:	981		resize:	810		save AWT:	25	load SWT:	5	total:	1821
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	970		resize:	821		save AWT:	30	load SWT:	5	total:	1826
	 * Photo-Image-Loader-1	IMG_1219_7.JPG	load:	950		resize:	710		save AWT:	25	load SWT:	5	total:	1690
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	950		resize:	706		save AWT:	30	load SWT:	5	total:	1691
	 * 												10037			8071				425				65			18598
	 * 
	 * 1 Thread
	 * 
	 * SWT
	 * Photo-Image-Loader-0	IMG_1219_10.JPG	load:	595	resize:	330	save:	70	total:	995
	 * Photo-Image-Loader-0	IMG_1219.JPG	load:	561	resize:	325	save:	80	total:	966
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	560	resize:	330	save:	50	total:	940
	 * Photo-Image-Loader-0	IMG_1219_3.JPG	load:	561	resize:	325	save:	45	total:	931
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	570	resize:	325	save:	50	total:	945
	 * Photo-Image-Loader-0	IMG_1219_5.JPG	load:	570	resize:	340	save:	50	total:	960
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	575	resize:	330	save:	45	total:	950
	 * Photo-Image-Loader-0	IMG_1219_7.JPG	load:	560	resize:	335	save:	50	total:	945
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	565	resize:	330	save:	45	total:	940
	 * Photo-Image-Loader-0	IMG_1219_9.JPG	load:	565	resize:	330	save:	45	total:	940
	 * 												5682		3300		530			9512
	 * 
	 * AWT
	 * Photo-Image-Loader-0	IMG_1219.JPG	load:	1115	resize:	790	save AWT:	45	load SWT:	5	total:	1955
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	1070	resize:	695	save AWT:	30	load SWT:	5	total:	1800
	 * Photo-Image-Loader-0	IMG_1219_3.JPG	load:	1035	resize:	695	save AWT:	25	load SWT:	5	total:	1760
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	1040	resize:	695	save AWT:	25	load SWT:	5	total:	1765
	 * Photo-Image-Loader-0	IMG_1219_5.JPG	load:	1040	resize:	695	save AWT:	25	load SWT:	110	total:	1870
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	1050	resize:	690	save AWT:	25	load SWT:	5	total:	1770
	 * Photo-Image-Loader-0	IMG_1219_7.JPG	load:	1035	resize:	690	save AWT:	145	load SWT:	5	total:	1875
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	1032	resize:	700	save AWT:	20	load SWT:	10	total:	1762
	 * Photo-Image-Loader-0	IMG_1219_9.JPG	load:	1030	resize:	700	save AWT:	25	load SWT:	5	total:	1760
	 * Photo-Image-Loader-0	IMG_1219_10.JPG	load:	1032	resize:	700	save AWT:	25	load SWT:	5	total:	1762
	 * 												10479			7050			390				160			18079
	 * 
	 * </pre>
	 */
	public void loadImage() {

		try {

			if (isImageVisible() == false) {
				return;
			}

//			final String imageKey = photo.getImageKey(imageQuality);
			if (imageQuality == PhotoManager.IMAGE_QUALITY_ORIGINAL) {

				// load original image

				final Image fullSizeImage = new Image(_display, photo.getFilePathName());

				PhotoImageCache.putImage(_imageKey, fullSizeImage);

			} else {

				/*
				 * check if photo is available in the thumbnail store
				 */
//				final Image storeImage = getStoreImageAWT(photo, imageQuality);
				final Image storeImage = getStoreImageSWT(photo, imageQuality);

				if (storeImage == null) {
					throw new Exception();
				} else {
					PhotoImageCache.putImage(_imageKey, storeImage);
				}
			}

			resetState();

			// tell the call back that the image is loaded
			_loadCallBack.callBackImageIsLoaded();

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind("Image \"{0}\" cannot be loaded ({1})", photo.getFileName(), _imageKey), e); //$NON-NLS-1$

			// prevent loading it again
			photo.setLoadingState(PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR, imageQuality);
		}
	}

	private void resetState() {

		// reset state to undefined that it will be loaded again when image is visible again
		photo.setLoadingState(PhotoLoadingState.UNDEFINED, imageQuality);
	}

	@Override
	public String toString() {
		return "PhotoImageLoaderItem [" //$NON-NLS-1$
				+ ("_filePathName=" + _imageKey + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("galleryIndex=" + galleryIndex + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("imageQuality=" + imageQuality + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("photo=" + photo) //$NON-NLS-1$
				+ "]"; //$NON-NLS-1$
	}
}
