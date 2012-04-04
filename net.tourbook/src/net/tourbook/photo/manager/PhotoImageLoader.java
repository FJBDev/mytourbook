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
import java.io.IOException;

import javax.imageio.ImageIO;

import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.util.StatusUtil;

import org.apache.commons.sanselan.ImageReadException;
import org.apache.commons.sanselan.common.IImageMetadata;
import org.apache.commons.sanselan.formats.jpeg.JpegImageMetadata;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Rotation;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class PhotoImageLoader {

	Photo					photo;
	private GalleryMTItem	_galleryItem;
	int						galleryIndex;
	int						imageQuality;
	private String			_imageKey;

	private ILoadCallBack	_loadCallBack;

	Display					_display;

	public PhotoImageLoader(final Display display,
							final GalleryMTItem galleryItem,
							final Photo photo,
							final int imageQuality,
							final ILoadCallBack loadCallBack) {

		_display = display;
		_galleryItem = galleryItem;
		this.photo = photo;
		this.imageQuality = imageQuality;
		_loadCallBack = loadCallBack;

		galleryIndex = photo.getGalleryIndex();
		_imageKey = photo.getImageKey(imageQuality);
	}

	/**
	 * @param storeImageFilePath
	 *            Path to store image in the thumbnail store
	 * @return
	 */
	private Image getImageFromEXIFThumbnail(final IPath storeImageFilePath) {

		try {

			// read exif meta data
			final IImageMetadata metadata = photo.updateMetadataFromImageFile(true);

			if (metadata == null) {
				return null;
			}

			if (metadata instanceof JpegImageMetadata) {

				final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

				BufferedImage awtBufferedImage = jpegMetadata.getEXIFThumbnail();

				if (awtBufferedImage == null) {
//					System.out.println(photo.getFileName() + "\tNO EXIF THUMB");
//					// TODO remove SYSTEM.OUT.PRINTLN

					return null;
				}

				try {

					// get SWT image from AWT image

					try {

						awtBufferedImage = transformImageCrop(awtBufferedImage, photo);
						awtBufferedImage = transformImageRotate(awtBufferedImage);

						ThumbnailStore.saveImageAWT(awtBufferedImage, storeImageFilePath);

					} catch (final Exception e) {
						StatusUtil.log(NLS.bind(//
								"Image \"{0}\" cannot be resized", //$NON-NLS-1$
								photo.getFilePathName()), e);
						return null;

					} finally {

						if (awtBufferedImage != null) {
							awtBufferedImage.flush();
						}
					}

					final Image thumbnailImage = new Image(_display, storeImageFilePath.toOSString());

					return thumbnailImage;

				} catch (final Exception e) {
					StatusUtil.log(NLS.bind(//
							"Store image \"{0}\" cannot be created", //$NON-NLS-1$
							storeImageFilePath.toOSString()), e);
				}
			}
		} catch (final ImageReadException e) {
			StatusUtil.log(e);
		} catch (final IOException e) {
			StatusUtil.log(e);
		}

		return null;
	}

	/**
	 * Get image from thumb store with the requested image quality.
	 * 
	 * @param photo
	 * @param requestedImageQuality
	 * @return
	 */
	private Image getImageFromStore(final int requestedImageQuality) {

		final IPath requestedStoreImageFilePath = ThumbnailStore.getStoreImagePath(photo, requestedImageQuality);
		Image storeImage = null;

		/*
		 * check if image is available in the thumbstore
		 */
		final File storeImageFile = new File(requestedStoreImageFilePath.toOSString());
		if (storeImageFile.isFile()) {

			// photo image is available in the thumbnail store

			/*
			 * touch store file when it is not yet done today, this is done to track last access
			 * time so that a store cleanup can check the date
			 */
			final LocalDate dtModified = new LocalDate(storeImageFile.lastModified());
			if (dtModified.equals(new LocalDate()) == false) {
				storeImageFile.setLastModified(new DateTime().getMillis());
			}

			try {
				storeImage = new Image(_display, requestedStoreImageFilePath.toOSString());
			} catch (final Exception e) {
				StatusUtil.log(
						NLS.bind("Store image \"{0}\" cannot be loaded", requestedStoreImageFilePath.toOSString()), //$NON-NLS-1$
						e);
			}
		}

		return storeImage;
	}

	/**
	 * check if the image is still visible
	 * 
	 * @return
	 */
	private boolean isImageVisible() {

		final GalleryMTItem group = _galleryItem.getParentItem();
		if (group == null) {
			return true;
		}

		final GalleryMTItem[] visibleItems = group.getVisibleItems();
		if (visibleItems == null) {
			return true;
		}

		final int galleryItemX = _galleryItem.x;
		final int galleryItemY = _galleryItem.y;

		for (final GalleryMTItem visibleItem : visibleItems) {

			if (visibleItem == null) {
				continue;
			}

			// !!! visibleItem.equals() is a performance hog when many items are displayed !!!
			if (visibleItem.x == galleryItemX && visibleItem.y == galleryItemY) {
				return true;
			}
		}

		// item is not visible

		return false;
	}

	/**
	 * This is called from the executor when the loading task is starting. It loads an image and
	 * puts it into the image cache from where it is fetched when painted.
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

		if (isImageVisible() == false) {
			setStateUndefined();
			return;
		}

		boolean isImageLoadedInRequestedQuality = false;
		Image loadedImage = null;
		String imageKey = null;
		boolean isLoadingError = false;

		try {

			// 1. get image with the requested quality from the image store
			final Image storeImage = getImageFromStore(imageQuality);
			if (storeImage != null) {

				isImageLoadedInRequestedQuality = true;

				imageKey = _imageKey;
				loadedImage = storeImage;

			} else {

				// 2. get image from thumbnail image in the EXIF data

				final int defaultThumbQuality = PhotoManager.IMAGE_QUALITY_THUMB_160;
				final IPath storeThumbImageFilePath = ThumbnailStore.getStoreImagePath(photo, defaultThumbQuality);

				final Image exifThumbnail = getImageFromEXIFThumbnail(storeThumbImageFilePath);
				if (exifThumbnail != null) {

					// EXIF image is available

					isImageLoadedInRequestedQuality = imageQuality == defaultThumbQuality;

					imageKey = photo.getImageKey(defaultThumbQuality);
					loadedImage = exifThumbnail;

				} else {

					/*
					 * image could not be loaded the fast way, it must be loaded the slow way
					 */

					if (imageQuality == PhotoManager.IMAGE_QUALITY_ORIGINAL) {

						// load original image

						imageKey = _imageKey;
						loadedImage = new Image(_display, photo.getFilePathName());
					}
				}
			}

		} catch (final Exception e) {

			setStateLoadingError();

			isLoadingError = true;

		} finally {

			final boolean isImageLoaded = loadedImage != null;
			final boolean isImageVisible = isImageVisible();

			if (isImageLoaded) {

				// keep image in cache
				PhotoImageCache.putImage(imageKey, loadedImage, photo.updatePhotoMetadata());
			}

			if (isImageVisible == false) {

				// image is NOT visible
				setStateUndefined();

			} else if (isImageLoadedInRequestedQuality) {

				// image is loaded with requested quality
				setStateUndefined();

			} else {

				// load image with requested quality

				PhotoManager.putImageInHQLoadingQueue(_galleryItem, photo, imageQuality, _loadCallBack);
			}

			// display image in the loading callback
			_loadCallBack.callBackImageIsLoaded(isImageVisible, isImageLoaded || isLoadingError);
		}
	}

	/**
	 * Image could not be loaded with {@link #loadImage()}, try to load high quality image.
	 */
	public void loadImageHQ() {

		final long start = System.currentTimeMillis();

		if (isImageVisible() == false) {
			setStateUndefined();
			return;
		}

		boolean isLoadingError = false;
		Image hqImage = null;

		try {

			// load original image and create thumbs
//			hqImage = loadImageHQ_10_WithSWT();
			hqImage = loadImageHQ_20_WithAWT();

		} catch (final Exception e) {

			setStateLoadingError();

			isLoadingError = true;

		} finally {

			final boolean isImageLoaded = hqImage != null;
			final boolean isImageVisible = isImageVisible();

			if (isImageLoaded) {

				setStateUndefined();

			} else {

				setStateLoadingError();

				isLoadingError = true;
			}

			// display image in the loading callback
			_loadCallBack.callBackImageIsLoaded(isImageVisible, isImageLoaded || isLoadingError);

//			System.out.println("loadImageHQ() time: " + (System.currentTimeMillis() - start) + " ms  " + photo);
//			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

//	private Image loadImageHQ_10_WithSWT() {
//
//		// load full size image
//		final String fullSizePathName = photo.getFilePathName();
//		Image loadedHQImage = null;
//
//		int hqImageWidth = 0;
//		int hqImageHeight = 0;
//
//		try {
//
//			// !!! this is not working on win7 with images 3500x5000 !!!
//			loadedHQImage = new Image(_display, fullSizePathName);
//
//			final Rectangle hqImageSize = loadedHQImage.getBounds();
//			hqImageWidth = hqImageSize.width;
//			hqImageHeight = hqImageSize.height;
//
//		} catch (final Exception e) {
//
//			// #######################################
//			// this must be handled without logging
//			// #######################################
//			StatusUtil.log(NLS.bind("Fullsize image \"{0}\" cannot be loaded", fullSizePathName), e); //$NON-NLS-1$
//
//		} finally {
//
//			if (loadedHQImage == null) {
//
//				/**
//				 * sometimes (when images are loaded concurrently) larger images could not be loaded
//				 * with SWT methods in Win7 (Eclipse 3.8 M6), try to load image with AWT. This bug
//				 * fix <code>https://bugs.eclipse.org/bugs/show_bug.cgi?id=350783</code> has not
//				 * solved this problem
//				 */
//
//				return loadImageHQ_20_WithAWT();
//			}
//		}
//
//		Image requestedImage = null;
//
//		/*
//		 * the source image starts with the HQ Image and is scaled down to the smallest thumb image
//		 */
//		Image srcImage = loadedHQImage;
//		int srcWidth = hqImageWidth;
//		int srcHeight = hqImageHeight;
//
//		final int[] thumbSizes = PhotoManager.IMAGE_SIZES;
//
//		// the original size will not be stored in the thumb store
//		for (int thumbImageQuality = thumbSizes.length - 2; thumbImageQuality >= 0; thumbImageQuality--) {
//
//			final int thumbSize = thumbSizes[thumbImageQuality];
//
//			Image scaledImage = null;
//			IPath storeImagePath = null;
//
//			try {
//
//				if (srcWidth > thumbSize || srcHeight > thumbSize) {
//
//					final Point bestSize = ImageUtils.getBestSize(srcWidth, srcHeight, thumbSize, thumbSize);
//
//					scaledImage = ImageUtils.resize(_display, srcImage, bestSize.x, bestSize.y, SWT.ON, SWT.HIGH);
//
//				} else {
//
//					scaledImage = srcImage;
//				}
//
//				storeImagePath = ThumbnailStore.getStoreImagePath(photo, thumbImageQuality);
//				ThumbnailStore.saveImageSWT(scaledImage, storeImagePath);
//
//			} catch (final Exception e) {
//				StatusUtil.log(NLS.bind("Store image \"{0}\" couldn't be created", storeImagePath.toOSString()), e); //$NON-NLS-1$
//			}
//
//			// requested image will be cached
//			if (thumbImageQuality == requestedImageQuality) {
//
//				// keep requested image in cache
//				PhotoImageCache.putImage(_imageKey, scaledImage, photo.updatePhotoMetadata());
//
//				requestedImage = scaledImage;
//			}
//
//			// dispose source image
//			if (srcImage != requestedImage && srcImage != scaledImage) {
//				srcImage.dispose();
//			}
//
//			// replace source image with scaled image
//			srcImage = scaledImage;
//			final Rectangle srcSize = srcImage.getBounds();
//			srcWidth = srcSize.width;
//			srcHeight = srcSize.height;
//		}
//
//		return requestedImage;
//	}

	private Image loadImageHQ_20_WithAWT() {

		/**
		 * sometimes (when images are loaded concurrently) larger images could not be loaded with
		 * SWT methods in Win7 (Eclipse 3.8 M6), try to load image with AWT. This bug fix
		 * <code>https://bugs.eclipse.org/bugs/show_bug.cgi?id=350783</code> has not solved this
		 * problem
		 */

		final Image swtImage = loadImageHQ_22_WithAWT();

		if (swtImage == null) {
// must be logged in another way
//			StatusUtil.log(NLS.bind(//
//					"Fullsize image \"{0}\" cannot be loaded",
//					photo.getFilePathName()), new Exception());
		}

		return swtImage;
	}

	private Image loadImageHQ_22_WithAWT() {

		Image requestedSWTImage = null;
		BufferedImage srcImage = null;

		try {
			final BufferedImage loadedHQImage = ImageIO.read(photo.getImageFile());
			if (loadedHQImage == null) {
				return null;
			}

			/*
			 * the source image starts with the HQ Image and is scaled down to the smallest thumb
			 * image
			 */
			srcImage = loadedHQImage;
			int srcWidth = srcImage.getWidth();
			int srcHeight = srcImage.getHeight();

			final int[] thumbSizes = PhotoManager.IMAGE_SIZES;
			final Method resizeQuality = PhotoManager.getResizeQuality();
			boolean isRotated = false;

			// the original image will not be stored in the thumb store
			for (int thumbImageQuality = thumbSizes.length - 2; thumbImageQuality >= 0; thumbImageQuality--) {

				// check if default (smallest) thumbnail already exist in the thumbstore
				Image defaultThumbImage = null;
				if (thumbImageQuality == PhotoManager.IMAGE_QUALITY_THUMB_160) {
					defaultThumbImage = getImageFromStore(thumbImageQuality);
				}

				if (defaultThumbImage != null && thumbImageQuality == imageQuality) {

					requestedSWTImage = defaultThumbImage;

				} else {

					if (defaultThumbImage != null) {
						// this is not the requested image
						defaultThumbImage.dispose();
					}

					final int thumbSize = thumbSizes[thumbImageQuality];

					BufferedImage scaledImage = null;
					final IPath storeImagePath = ThumbnailStore.getStoreImagePath(photo, thumbImageQuality);

					try {

						if (srcWidth > thumbSize || srcHeight > thumbSize) {

							// src image is larger than the current thumb size -> resize image

							final Point bestSize = ImageUtils.getBestSize(srcWidth, srcHeight, thumbSize, thumbSize);
							final int maxSize = Math.max(bestSize.x, bestSize.y);

							// resize image
							if (thumbImageQuality == PhotoManager.IMAGE_QUALITY_THUMB_160) {
								// scale small image with better quality
								scaledImage = Scalr.resize(srcImage, Method.QUALITY, maxSize);
							} else {
								scaledImage = Scalr.resize(srcImage, resizeQuality, maxSize);
							}

							// rotate image according to the exif flag
							if (isRotated == false) {

								isRotated = true;

								scaledImage = transformImageRotate(scaledImage);
							}

						} else {

							scaledImage = srcImage;
						}

						// save scaled awt image in store
						ThumbnailStore.saveImageAWT(scaledImage, storeImagePath);

					} catch (final Exception e) {
						StatusUtil.log(
								NLS.bind("Store image \"{0}\" couldn't be created", storeImagePath.toOSString()), //$NON-NLS-1$
								e);
					}

					// check if the scaled image has the requested image quality
					if (thumbImageQuality == imageQuality) {

						// create swt image from saved AWT image

						requestedSWTImage = getImageFromStore(imageQuality);

					}

					// flush source image
					srcImage.flush();

					// replace source image with scaled image
					srcImage = scaledImage;
					srcWidth = scaledImage.getWidth();
					srcHeight = scaledImage.getHeight();
				}
			}

			if (requestedSWTImage != null) {

				// keep requested image in cache
				PhotoImageCache.putImage(_imageKey, requestedSWTImage, photo.updatePhotoMetadata());
			}

		} catch (final IOException e) {
			StatusUtil.log(e);
		} finally {

			if (srcImage != null) {
				srcImage.flush();
			}
		}

		if (requestedSWTImage == null) {
			setStateLoadingError();
		}

		return requestedSWTImage;
	}

	private void setStateLoadingError() {

		// prevent loading the image again
		photo.setLoadingState(PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR, imageQuality);
	}

	private void setStateUndefined() {

		// set state to undefined that it will be loaded again when image is visible
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

	/**
	 * Crop thumb image when it has a different ratio than the original image. This will remove the
	 * black margins which are set in the thumb image depending on the image ratio.
	 * 
	 * @param thumbImage
	 * @param width
	 * @param height
	 * @return
	 */
	private BufferedImage transformImageCrop(final BufferedImage thumbImage, final Photo photo) {

		final int thumbWidth = thumbImage.getWidth();
		final int thumbHeight = thumbImage.getHeight();
		final int photoWidth = photo.getWidthRotated();
		final int photoHeight = photo.getHeightRotated();
//		final int photoWidth = photo.getWidth();
//		final int photoHeight = photo.getHeight();

		final double thumbRatio = (double) thumbWidth / thumbHeight;
		double photoRatio = (double) photoWidth / photoHeight;

		boolean isRotate = false;

		if (thumbRatio < 1.0 && photoRatio > 1.0 || thumbRatio > 1.0 && photoRatio < 1.0) {

			/*
			 * thumb and photo have total different ratios, this can happen when an image is resized
			 * or rotated and the thumb image was not adjusted
			 */

			photoRatio = 1.0 / photoRatio;

			/*
			 * rotate image to the photo orientation, it's rotated 90 degree to the right but it
			 * cannot be determined which is the correct direction
			 */
			if (photo.getOrientation() <= 1) {
				// rotate it only when rotation is not set in the exif data
				isRotate = true;
			}
		}

		final int thumbRationTruncated = (int) (thumbRatio * 100);
		final int photoRationTruncated = (int) (photoRatio * 100);

		if (thumbRationTruncated == photoRationTruncated) {
			// ration is the same
			return thumbImage;
		}

		int cropX;
		int cropY;
		int cropWidth;
		int cropHeight;

		if (thumbRationTruncated < photoRationTruncated) {

			// thumb height is smaller than photo height

			cropWidth = thumbWidth;
			cropHeight = (int) (thumbWidth / photoRatio);

			cropX = 0;

			cropY = thumbHeight - cropHeight;
			cropY /= 2;

		} else {

			// thumb width is smaller than photo width

			cropWidth = (int) (thumbHeight * photoRatio);
			cropHeight = thumbHeight;

			cropX = thumbWidth - cropWidth;
			cropX /= 2;

			cropY = 0;
		}

		final BufferedImage croppedImage = Scalr.crop(thumbImage, cropX, cropY, cropWidth, cropHeight);

		thumbImage.flush();

		BufferedImage rotatedImage = croppedImage;
		if (isRotate) {

			rotatedImage = Scalr.rotate(croppedImage, Rotation.CW_90);

			croppedImage.flush();
		}

		return rotatedImage;
	}

	/**
	 * @param scaledImage
	 * @return Returns rotated image when orientations is not default
	 */
	private BufferedImage transformImageRotate(final BufferedImage scaledImage) {

		BufferedImage rotatedImage = scaledImage;

		final int orientation = photo.getOrientation();

		if (orientation > 1) {

			// see here http://www.impulseadventure.com/photo/exif-orientation.html

			Rotation correction = null;
			if (orientation == 8) {
				correction = Rotation.CW_270;
			} else if (orientation == 3) {
				correction = Rotation.CW_180;
			} else if (orientation == 6) {
				correction = Rotation.CW_90;
			}

			rotatedImage = Scalr.rotate(scaledImage, correction);

			scaledImage.flush();
		}

		return rotatedImage;
	}
}
