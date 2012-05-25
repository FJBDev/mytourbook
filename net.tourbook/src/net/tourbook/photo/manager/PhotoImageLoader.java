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
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import javax.imageio.ImageIO;

import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.UI;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Rotation;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class PhotoImageLoader {

	private static String[]				awtImageFileSuffixes;

	Photo								_photo;
	private GalleryMT20Item				_galleryItem;
	ImageQuality						_requestedImageQuality;
	private String						_imageFramework;
	private int							_hqImageSize;
	private String						_requestedImageKey;

	private ILoadCallBack				_loadCallBack;

	Display								_display;

	/**
	 * Contains AWT images which are disposed after loading
	 */
	private ArrayList<BufferedImage>	_trackedAWTImages	= new ArrayList<BufferedImage>();

	/**
	 * Contains SWT images which are disposed after loading
	 */
	private ArrayList<Image>			_trackedSWTImages	= new ArrayList<Image>();

	private int[]						_recursiveCounter	= { 0 };

	static {

		awtImageFileSuffixes = ImageIO.getReaderFileSuffixes();

//		final String[] formatNames = ImageIO.getReaderFormatNames();
//		final String mimeReadFormats[] = ImageIO.getReaderMIMETypes();
//
//		System.out.println("Mime Reader:      " + Arrays.asList(mimeReadFormats));
//		System.out.println("Format Reader:    " + Arrays.asList(formatNames));
//		System.out.println("Suffixes Readers: " + Arrays.asList(awtImageFileSuffixes));

//		final String writeFormats[] = ImageIO.getWriterMIMETypes();
//		System.out.println("Mime Writers:     " + Arrays.asList(writeFormats));

	}

	public PhotoImageLoader(final Display display,
							final GalleryMT20Item galleryItem,
							final Photo photo,
							final ImageQuality imageQuality,
							final String imageFramework,
							final int hqImageSize,
							final ILoadCallBack loadCallBack) {

		_display = display;
		_galleryItem = galleryItem;
		_photo = photo;
		_requestedImageQuality = imageQuality;
		_imageFramework = imageFramework;
		_hqImageSize = hqImageSize;
		_loadCallBack = loadCallBack;

		_requestedImageKey = photo.getImageKey(_requestedImageQuality);
	}

	private Image createSWTimageFromAWTimage(final BufferedImage awtImage) {

		final ImageData swtImageData = UI.convertAWTimageToSWTimage(awtImage);

		if (swtImageData != null) {
			return new Image(_display, swtImageData);
		}

		return null;
	}

	private void disposeTrackedImages() {

		for (final BufferedImage awtImage : _trackedAWTImages) {
			if (awtImage != null) {
				awtImage.flush();
			}
		}
		_trackedAWTImages.clear();

		for (final Image swtImage : _trackedSWTImages) {
			if (swtImage != null) {
				swtImage.dispose();
			}
		}
		_trackedSWTImages.clear();
	}

	/**
	 * @return Returns roatation according to the EXIF data or <code>null</code> when image is not
	 *         rotatet.
	 */
	private Rotation getRotation() {

		Rotation thumbRotation = null;
		final int orientation = _photo.getOrientation();

		if (orientation > 1) {

			// see here http://www.impulseadventure.com/photo/exif-orientation.html

			if (orientation == 8) {
				thumbRotation = Rotation.CW_270;
			} else if (orientation == 3) {
				thumbRotation = Rotation.CW_180;
			} else if (orientation == 6) {
				thumbRotation = Rotation.CW_90;
			}
		}

		return thumbRotation;
	}

	/**
	 * @return Returns <code>true</code> when the image file can be loaded with AWT
	 */
	private boolean isAWTImageSupported() {

		final String photoSuffix = _photo.getPhotoWrapper().imageFileExt;

		for (final String awtImageSuffix : awtImageFileSuffixes) {
			if (photoSuffix.equals(awtImageSuffix)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * check if the image is still visible
	 * 
	 * @return
	 */
	private boolean isImageVisible() {

		final boolean isItemVisible = _galleryItem.gallery.isItemVisible(_galleryItem);

		return isItemVisible;
	}

	/**
	 * This is called from the executor when the loading task is starting. It loads an image and
	 * puts it into the image cache from where it is fetched when painted.
	 * 
	 * <pre>
	 * 
	 * 2 Threads
	 * =========
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
	 * ========
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
	 * 
	 * @param waitingqueueexif
	 */
	public void loadImage() {

		if (isImageVisible() == false) {
			setStateUndefined();
			return;
		}

		boolean isLoadedImageInRequestedQuality = false;
		Image loadedImage = null;
		String imageKey = null;
		boolean isLoadingError = false;

		try {

			// 1. get image with the requested quality from the image store
			final Image storeImage = loadImageFromStore(_requestedImageQuality);
			if (storeImage != null) {

				isLoadedImageInRequestedQuality = true;

				imageKey = _requestedImageKey;
				loadedImage = storeImage;

			} else {

				// 2. get image from thumbnail image in the EXIF data

				final IPath storeThumbImageFilePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

				final Image exifThumbnail = loadImageFromEXIFThumbnail(storeThumbImageFilePath);
				if (exifThumbnail != null) {

					// EXIF image is available

					isLoadedImageInRequestedQuality = _requestedImageQuality == ImageQuality.THUMB;

					imageKey = _photo.getImageKey(ImageQuality.THUMB);
					loadedImage = exifThumbnail;
				}
			}

		} catch (final Exception e) {

			setStateLoadingError();

			isLoadingError = true;

		} finally {

			disposeTrackedImages();

			final boolean isImageLoaded = loadedImage != null;

			/*
			 * keep image in cache
			 */
			if (isImageLoaded) {

				PhotoImageCache.putImage(imageKey, loadedImage, _photo.getImageMetaData());

				updateImageData(loadedImage);
			}

			/*
			 * update loading state
			 */
			if (isLoadedImageInRequestedQuality) {

				// image is loaded with requested quality, reset image state

				setStateUndefined();

			} else {

				// load image with higher quality

				PhotoLoadManager.putImageInHQLoadingQueue(_galleryItem, _photo, _requestedImageQuality, _loadCallBack);
			}

			// show in the UI, that meta data are loaded, loading message is displayed with another color
			final boolean isUpdateUI = _photo.getImageMetaDataRaw() != null;

			// display image in the loading callback
			_loadCallBack.callBackImageIsLoaded(isUpdateUI || isImageLoaded || isLoadingError);
		}
	}

	/**
	 * @param storeImageFilePath
	 *            Path to store image in the thumbnail store
	 * @return
	 */
	private Image loadImageFromEXIFThumbnail(final IPath storeImageFilePath) {

		BufferedImage awtBufferedImage = null;

		try {

			// read exif meta data
			final IImageMetadata metadata = _photo.getImageMetaData(true);

			if (metadata == null) {
				return null;
			}

// this will print out all metadata
//			System.out.println(metadata);

			if (metadata instanceof JpegImageMetadata) {

				awtBufferedImage = ((JpegImageMetadata) metadata).getEXIFThumbnail();

				_trackedAWTImages.add(awtBufferedImage);

				if (awtBufferedImage == null) {
					return null;
				}

				Image swtThumbnailImage = null;
				try {

					/*
					 * transform EXIF image and save it in the thumb store
					 */
					try {

						awtBufferedImage = transformImageCrop(awtBufferedImage);
						awtBufferedImage = transformImageRotate(awtBufferedImage);

					} catch (final Exception e) {
						StatusUtil.log(NLS.bind(//
								"Image \"{0}\" cannot be resized", //$NON-NLS-1$
								_photo.getPhotoWrapper().imageFilePathName), e);
						return null;
					}

					/*
					 * convert awt into swt image
					 */
//					final boolean isUseFileConversion = false;
//					if (isUseFileConversion) {
//
//						ThumbnailStore.saveImageAWT(awtBufferedImage, storeImageFilePath);
//
//						// get SWT image from saved AWT image
//						swtThumbnailImage = new Image(_display, storeImageFilePath.toOSString());
//
////////////////////////////////////////////
////
//// MUST BE REMOVED, IS ONLY FOR TESTING
////
////
//						// it performs better when existing thumb images do not keep saved
//						new File(storeImageFilePath.toOSString()).delete();
////
//// MUST BE REMOVED, IS ONLY FOR TESTING
////
////////////////////////////////////////////
//					} else {

					final ImageData imageData = UI.convertAWTimageToSWTimage(awtBufferedImage);

					if (imageData != null) {

						swtThumbnailImage = new Image(_display, imageData);

						_photo.setStateExifThumb(awtBufferedImage == null ? 0 : 1);

						return swtThumbnailImage;
					}

//					}

				} catch (final Exception e) {
					StatusUtil.log(NLS.bind(//
							"SWT store image \"{0}\" cannot be created", //$NON-NLS-1$
							storeImageFilePath.toOSString()), e);
				} finally {

					if (swtThumbnailImage == null) {

						System.out.println(NLS.bind( //
								UI.timeStamp() + "EXIF image \"{0}\" cannot be created", //$NON-NLS-1$
								storeImageFilePath));
					}
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
	 * @param _photo
	 * @param requestedImageQuality
	 * @return
	 */
	private Image loadImageFromStore(final ImageQuality requestedImageQuality) {

		Image storeImage = null;

		/*
		 * check if image is available in the thumbstore
		 */
		final IPath requestedStoreImageFilePath = ThumbnailStore.getStoreImagePath(_photo, requestedImageQuality);

		final String imageStoreFilePath = requestedStoreImageFilePath.toOSString();
		final File storeImageFile = new File(imageStoreFilePath);

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

				storeImage = new Image(_display, imageStoreFilePath);

			} catch (final Exception e) {
				StatusUtil.log(NLS.bind("Image cannot be loaded with SWT (1): \"{0}\"", //$NON-NLS-1$
						imageStoreFilePath), e);
			} finally {

				if (storeImage == null) {

					String message = "Image \"{0}\" cannot be loaded and an exception did not occure.\n"
							+ "The image file is available but it's possible that SWT.ERROR_NO_HANDLES occured";

					System.out.println(UI.timeStamp() + NLS.bind(message, imageStoreFilePath));

					PhotoImageCache.dispose();

					/*
					 * try loading again
					 */
					try {

						storeImage = new Image(_display, imageStoreFilePath);

					} catch (final Exception e) {
						StatusUtil.log(NLS.bind("Image cannot be loaded with SWT (2): \"{0}\"", //$NON-NLS-1$
								imageStoreFilePath), e);
					} finally {

						if (storeImage == null) {

							message = "Image cannot be loaded again with SWT, even when disposing the image cache: \"{0}\" ";

							System.out.println(UI.timeStamp() + NLS.bind(message, imageStoreFilePath));
						}
					}
				}
			}
		}

		return storeImage;
	}

	/**
	 * Image could not be loaded with {@link #loadImage()}, try to load high quality image.
	 * 
	 * @param smallImageWaitingQueue
	 *            waiting queue for small images
	 * @param exifWaitingQueue
	 */
	public void loadImageHQ(final LinkedBlockingDeque<PhotoImageLoader> smallImageWaitingQueue,
							final LinkedBlockingDeque<PhotoExifLoader> exifWaitingQueue) {

		if (isImageVisible() == false) {
			setStateUndefined();
			return;
		}

		/*
		 * wait until exif data and small images are loaded
		 */
		try {
			while (smallImageWaitingQueue.size() > 0 || exifWaitingQueue.size() > 0) {
				Thread.sleep(100);
			}
		} catch (final InterruptedException e) {
			// should not happen, I hope so
		}

		boolean isLoadingError = false;
		Image hqImage = null;

		try {

			/**
			 * sometimes (when images are loaded concurrently) larger images could not be loaded
			 * with SWT methods in Win7 (Eclipse 3.8 M6), try to load image with AWT. This bug fix
			 * <code>https://bugs.eclipse.org/bugs/show_bug.cgi?id=350783</code> has not solved this
			 * problem
			 */

			// load original image and create thumbs

			if (_imageFramework.equals(PhotoLoadManager.IMAGE_FRAMEWORK_SWT)
			// use SWT when image format is not supported by AWT which is the case for tiff images
					|| isAWTImageSupported() == false) {

				hqImage = loadImageHQ_10_WithSWT();
			} else {
				hqImage = loadImageHQ_20_WithAWT();
			}

		} catch (final Exception e) {

			setStateLoadingError();

			isLoadingError = true;

		} finally {

			disposeTrackedImages();

			if (hqImage == null) {

				System.out.println(NLS.bind(//
						UI.timeStamp() + "image == NULL when loading with {0}: \"{1}\"",
						_imageFramework.toUpperCase(),
						_photo.getPhotoWrapper().imageFilePathName));

				if (_imageFramework.equals(PhotoLoadManager.IMAGE_FRAMEWORK_AWT)) {

					/*
					 * AWT fails, try to load image with SWT
					 */

					try {

						hqImage = loadImageHQ_10_WithSWT();

					} catch (final Exception e2) {

						setStateLoadingError();

						isLoadingError = true;

					} finally {

						if (hqImage == null) {
							System.out.println(NLS.bind(//
									UI.timeStamp() + "image == NULL when loading with SWT: \"{0}\"",
									_photo.getPhotoWrapper().imageFilePathName));
						}
					}
				}
			}

			// update image state
			final boolean isImageLoaded = hqImage != null;
			if (isImageLoaded) {

				setStateUndefined();

			} else {

				setStateLoadingError();

				isLoadingError = true;
			}

			// display image in the loading callback
			_loadCallBack.callBackImageIsLoaded(isImageLoaded || isLoadingError);
		}
	}

	private Image loadImageHQ_10_WithSWT() throws Exception {

		if (_recursiveCounter[0]++ > 2) {
			return null;
		}

		final long start = System.currentTimeMillis();
		long endHqLoad = 0;
		long endResizeHQ = 0;
		long endResizeThumb = 0;
		long endSaveHQ = 0;
		long endSaveThumb = 0;

		Image loadedImage = null;

		/*
		 * load original image
		 */
		final String originalImagePathName = _photo.getPhotoWrapper().imageFilePathName;
		try {

			final long startHqLoad = System.currentTimeMillis();

			loadedImage = new Image(_display, originalImagePathName);

			endHqLoad = System.currentTimeMillis() - startHqLoad;

		} catch (final Exception e) {

			System.out.println(NLS.bind(//
					"SWT: image \"{0}\" cannot be loaded", //$NON-NLS-1$
					originalImagePathName));

		} finally {

			if (loadedImage == null) {

				System.out.println(NLS.bind( //
						UI.timeStamp() + "SWT: image \"{0}\" cannot be loaded, will load with AWT", //$NON-NLS-1$
						originalImagePathName));

				/**
				 * sometimes (when images are loaded concurrently) larger images could not be loaded
				 * with SWT methods in Win7 (Eclipse 3.8 M6), try to load image with AWT. This bug
				 * fix <code>
				 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=350783
				 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845
				 * </code>
				 * has not solved this problem
				 */

				try {
					return loadImageHQ_20_WithAWT();
				} catch (final Exception e) {
					throw e;
				}
			}
		}

		Rectangle imageBounds = loadedImage.getBounds();
		int imageWidth = imageBounds.width;
		int imageHeight = imageBounds.height;

		final int thumbSize = PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;

		// images are rotated only ONE time (the first one)
		boolean isRotated = false;

		boolean isHQCreated = false;

		Image hqImage;
		Image requestedSWTImage = null;

		/*
		 * create HQ image
		 */
		if (imageWidth > _hqImageSize || imageHeight > _hqImageSize) {

			/*
			 * image is larger than HQ image -> resize to HQ
			 */

			final long startResizeHQ = System.currentTimeMillis();

			final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, _hqImageSize, _hqImageSize);

			Rotation hqRotation = null;

			if (isRotated == false) {
				isRotated = true;
				hqRotation = getRotation();
			}

			final Image scaledHQImage = ImageUtils.resize(
					_display,
					loadedImage,
					bestSize.x,
					bestSize.y,
					SWT.ON,
					SWT.LOW,
					hqRotation);

			endResizeHQ = System.currentTimeMillis() - startResizeHQ;

			hqImage = scaledHQImage;

			imageBounds = scaledHQImage.getBounds();
			imageWidth = imageBounds.width;
			imageHeight = imageBounds.height;

			// new image has been created, loaded must be disposed
			_trackedSWTImages.add(loadedImage);

			if (_requestedImageQuality == ImageQuality.HQ) {
				// keep scaled image
				requestedSWTImage = scaledHQImage;
			} else {
				// dispose scaled image
				_trackedSWTImages.add(scaledHQImage);
			}

			/*
			 * save scaled image in store
			 */
			final long startSaveHQ = System.currentTimeMillis();
			final IPath storeHQImagePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.HQ);

			ThumbnailStore.saveThumbImageWithSWT(scaledHQImage, storeHQImagePath);

			isHQCreated = true;

			endSaveHQ = System.currentTimeMillis() - startSaveHQ;

		} else {
			hqImage = loadedImage;
		}

		/*
		 * create thumb image
		 */
		if (imageWidth > thumbSize || imageHeight > thumbSize) {

			/*
			 * image is larger than thumb image -> resize to thumb
			 */

			if (isHQCreated == false) {

				// image size is between thumb and HQ

				if (_requestedImageQuality == ImageQuality.HQ) {
					requestedSWTImage = hqImage;
				}
			}

			if (_photo.getExifThumbImageState() == 1) {

				if (requestedSWTImage == null) {

					// get thumb image

					final IPath storeImageFilePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

					requestedSWTImage = loadImageFromEXIFThumbnail(storeImageFilePath);
				}

			} else {

				// create thumb image

				final long startResizeThumb = System.currentTimeMillis();

				final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, thumbSize, thumbSize);
				Rotation thumbRotation = null;

				if (isRotated == false) {
					isRotated = true;
					thumbRotation = getRotation();
				}

				final Image scaledThumbImage = ImageUtils.resize(
						_display,
						hqImage,
						bestSize.x,
						bestSize.y,
						SWT.ON,
						SWT.LOW,
						thumbRotation);

				// new image has been created, source image must be disposed
				_trackedSWTImages.add(hqImage);

				if (requestedSWTImage == null) {
					// keep scaled image
					requestedSWTImage = scaledThumbImage;
				} else {
					// dispose scaled image
					_trackedSWTImages.add(scaledThumbImage);
				}

				endResizeThumb = System.currentTimeMillis() - startResizeThumb;

				/*
				 * save scaled image in store
				 */
				final long startSaveThumb = System.currentTimeMillis();
				final IPath storeThumbImagePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

				ThumbnailStore.saveThumbImageWithSWT(scaledThumbImage, storeThumbImagePath);

				endSaveThumb = System.currentTimeMillis() - startSaveThumb;
			}

		} else {

			// loaded image is smaller than a thumb image

			requestedSWTImage = loadedImage;
		}

		if (requestedSWTImage != null) {

			// keep requested image in cache
			PhotoImageCache.putImage(_requestedImageKey, requestedSWTImage, _photo.getImageMetaData());

			updateImageData(requestedSWTImage);
		}

		if (requestedSWTImage == null) {
			setStateLoadingError();
		}

		final long end = System.currentTimeMillis() - start;

		System.out.println(UI.timeStamp()
				+ "SWT: "
				+ (Thread.currentThread().getName() + " " + _photo.getPhotoWrapper().imageFileName)
				+ ("\ttotal: " + end)
				+ ("\tload: " + endHqLoad)
				+ ("\tresizeHQ: " + endResizeHQ)
				+ ("\tsaveHQ: " + endSaveHQ)
				+ ("\tresizeThumb: " + endResizeThumb)
				+ ("\tsaveThumb: " + endSaveThumb));

		return requestedSWTImage;
	}

	private Image loadImageHQ_20_WithAWT() throws Exception {

		if (_recursiveCounter[0]++ > 2) {
			return null;
		}

		final PhotoWrapper photoWrapper = _photo.getPhotoWrapper();

		final long start = System.currentTimeMillis();
		long endHqLoad = 0;
		long endResizeHQ = 0;
		long endResizeThumb = 0;
		long endSaveHQ = 0;
		long endSaveThumb = 0;

		Image requestedSWTImage = null;
		String exceptionMessage = null;

		// images are rotated only ONCE (the first one)
		boolean isRotated = false;

		/*
		 * load original image
		 */
		BufferedImage originalImage = null;
		final String originalPathName = photoWrapper.imageFilePathName;
		try {

			final long startHqLoad = System.currentTimeMillis();
			{
				originalImage = ImageIO.read(photoWrapper.imageFile);

				_trackedAWTImages.add(originalImage);
			}
			endHqLoad = System.currentTimeMillis() - startHqLoad;

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind("AWT: image \"{0}\" cannot be loaded.", originalPathName)); //$NON-NLS-1$

		} finally {

			if (originalImage == null) {

				System.out.println(NLS.bind(//
						UI.timeStamp() + "AWT: image \"{0}\" cannot be loaded, will load with SWT", //$NON-NLS-1$
						originalPathName));

				return loadImageHQ_10_WithSWT();
			}
		}

		/*
		 * handle thumb save error
		 */
		final boolean isThumbSaveError = PhotoLoadManager.isThumbSaveError(originalPathName);
		if (isThumbSaveError) {

			// the thumb image could not be previously saved in the thumb store, display original image

			final Image swtImage = createSWTimageFromAWTimage(originalImage);

			if (swtImage == null) {

				exceptionMessage = NLS.bind(//
						"Photo image with thumb save error cannot be created with SWT (1): ", //$NON-NLS-1$
						originalPathName);
			} else {

				requestedSWTImage = swtImage;
			}

		} else {

			/*
			 * create HQ image from original image
			 */

			boolean isHQCreated = false;

			int imageWidth = originalImage.getWidth();
			int imageHeight = originalImage.getHeight();
			BufferedImage hqImage;

			if (imageWidth >= _hqImageSize || imageHeight >= _hqImageSize) {

				// original image is larger than HQ image -> resize to HQ

				BufferedImage scaledHQImage;

				final long startResizeHQ = System.currentTimeMillis();
				{
					final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, _hqImageSize, _hqImageSize);
					final int maxSize = Math.max(bestSize.x, bestSize.y);

					scaledHQImage = Scalr.resize(originalImage, Method.SPEED, maxSize);

					_trackedAWTImages.add(scaledHQImage);

					// rotate image according to the EXIF flag
					if (isRotated == false) {
						isRotated = true;

						scaledHQImage = transformImageRotate(scaledHQImage);
					}

					hqImage = scaledHQImage;

					imageWidth = scaledHQImage.getWidth();
					imageHeight = scaledHQImage.getHeight();
				}
				endResizeHQ = System.currentTimeMillis() - startResizeHQ;

				/*
				 * save scaled HQ image in store
				 */
				final long startSaveHQ = System.currentTimeMillis();
				{
					final boolean isSaved = ThumbnailStore.saveThumbImageWithAWT(
							scaledHQImage,
							ThumbnailStore.getStoreImagePath(_photo, ImageQuality.HQ));

					if (isSaved == false) {
						// AWT save error has occured, possible error: "Bogus input colorspace"
						_photo.setThumbSaveError();

					}

					// check if the scaled image has the requested image quality
					if (_requestedImageQuality == ImageQuality.HQ) {

						// create swt image from saved AWT image, this converts AWT -> SWT

						requestedSWTImage = loadImageFromStore(ImageQuality.HQ);
					}
				}
				endSaveHQ = System.currentTimeMillis() - startSaveHQ;

				isHQCreated = true;

			} else {
				hqImage = originalImage;
			}

			/*
			 * create thumb image from HQ image
			 */
			BufferedImage saveThumbAWT = null;

			final int thumbSize = PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;
			if (imageWidth >= thumbSize || imageHeight >= thumbSize) {

				/*
				 * image is larger than thumb image -> resize to thumb
				 */

				if (isHQCreated == false) {

					// image size is between thumb and HQ

					if (_requestedImageQuality == ImageQuality.HQ) {

						final ImageData imageData = UI.convertAWTimageToSWTimage(originalImage);
						requestedSWTImage = new Image(_display, imageData);
					}
				}

				if (_photo.getExifThumbImageState() == 1) {

					if (requestedSWTImage == null) {

						// get thumb image

						final IPath storeImageFilePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);
						requestedSWTImage = loadImageFromEXIFThumbnail(storeImageFilePath);
					}

				} else {

					// check if thumb image is already available
					final Image exifThumbImage = loadImageFromStore(ImageQuality.THUMB);
					if (exifThumbImage != null) {

						// EXIF thumb image is already available in the thumbstore

						if (requestedSWTImage == null && _requestedImageQuality == ImageQuality.THUMB) {

							requestedSWTImage = exifThumbImage;
						} else {
							_trackedSWTImages.add(exifThumbImage);
						}

					} else {

						/*
						 * create thumb image
						 */

						BufferedImage scaledThumbImage;
						final long startResizeThumb = System.currentTimeMillis();
						{
							final Point bestSize = ImageUtils
									.getBestSize(imageWidth, imageHeight, thumbSize, thumbSize);
							final int maxSize = Math.max(bestSize.x, bestSize.y);

							scaledThumbImage = Scalr.resize(hqImage, Method.QUALITY, maxSize);

							_trackedAWTImages.add(scaledThumbImage);

							// rotate image according to the exif flag
							if (isRotated == false) {

								isRotated = true;

								scaledThumbImage = transformImageRotate(scaledThumbImage);
							}

							saveThumbAWT = scaledThumbImage;
						}
						endResizeThumb = System.currentTimeMillis() - startResizeThumb;
					}
				}

			} else {

				// loaded image is smaller than a thumb image

				saveThumbAWT = originalImage;
			}

			/*
			 * save thumb image
			 */
			if (saveThumbAWT == originalImage) {

				// original image is not saved as a thumb

				if (requestedSWTImage == null) {

					requestedSWTImage = createSWTimageFromAWTimage(saveThumbAWT);

					if (requestedSWTImage == null) {
						exceptionMessage = NLS.bind(//
								"Photo image cannot be converted from AWT to SWT: ", //$NON-NLS-1$
								originalPathName);
					}
				}

			} else {

				boolean isSaved = true;

				if (saveThumbAWT != null) {

					final long startSaveThumb = System.currentTimeMillis();
					{
						final IPath storeThumbImagePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

						isSaved = ThumbnailStore.saveThumbImageWithAWT(saveThumbAWT, storeThumbImagePath);
					}
					endSaveThumb = System.currentTimeMillis() - startSaveThumb;
				}

				if (isSaved == false) {

					// AWT save error has occured, possible error: "Bogus input colorspace"
					_photo.setThumbSaveError();

					if (requestedSWTImage == null) {

						requestedSWTImage = createSWTimageFromAWTimage(saveThumbAWT);

						if (requestedSWTImage == null) {
							exceptionMessage = NLS.bind(
									"Photo image with thumb save error cannot be created with SWT (2): ", //$NON-NLS-1$
									originalPathName);
						}
					}
				}
			}

			// check if the requested image is set, if not load thumb
			if (requestedSWTImage == null) {

				// create swt image from saved AWT image (convert AWT->SWT)

				requestedSWTImage = loadImageFromStore(ImageQuality.THUMB);
			}

			if (requestedSWTImage != null) {

				// keep requested image in cache
				PhotoImageCache.putImage(_requestedImageKey, requestedSWTImage, _photo.getImageMetaData());

				updateImageData(requestedSWTImage);
			}

			if (requestedSWTImage == null) {
				setStateLoadingError();
			}
		}

		final long end = System.currentTimeMillis() - start;

		System.out.println(UI.timeStamp()
				+ "AWT: "
				+ (Thread.currentThread().getName() + " " + photoWrapper.imageFileName)
				+ ("\ttotal: " + end)
				+ ("\tload: " + endHqLoad)
				+ ("\tresizeHQ: " + endResizeHQ)
				+ ("\tsaveHQ: " + endSaveHQ)
				+ ("\tresizeThumb: " + endResizeThumb)
				+ ("\tsaveThumb: " + endSaveThumb)
		//
				);

		if (exceptionMessage != null) {
			throw new Exception(exceptionMessage);
		}

		return requestedSWTImage;
	}

	private void setStateLoadingError() {

		// prevent loading the image again
		_photo.setLoadingState(PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR, _requestedImageQuality);

		PhotoLoadManager.putPhotoInLoadingErrorMap(_photo.getPhotoWrapper().imageFilePathName);
	}

// JAI implementation to read tiff images with AWT
//
//	private BufferedImage loadImageHQ_22_ExtendedAWT(final PhotoWrapper photoWrapper) throws IOException {
//
//		if (isAWTImageSupported == false) {
//
//			// extension is not supported
//
//			return null;
//
////
////			final SeekableStream s = new FileSeekableStream(_photo.getPhotoWrapper().imageFile);
////
////			final TIFFDecodeParam param = null;
////
////			final ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
////
////			// Which of the multiple images in the TIFF file do we want to load
////			// 0 refers to the first, 1 to the second and so on.
////			final int imageToLoad = 0;
////
////			final RenderedImage op = new NullOpImage(
////					dec.decodeAsRenderedImage(imageToLoad),
////					null,
////					OpImage.OP_IO_BOUND,
////					null);
////
////			final BufferedImage img = new BufferedImage(op.getWidth(), op.getHeight(), BufferedImage.TYPE_INT_ARGB);
//
//		} else {
//
//			return ImageIO.read(photoWrapper.imageFile);
//		}
//	}

	private void setStateUndefined() {

		// set state to undefined that it will be loaded again when image is visible and not in the cache
		_photo.setLoadingState(PhotoLoadingState.UNDEFINED, _requestedImageQuality);
	}

	@Override
	public String toString() {
		return "PhotoImageLoaderItem [" //$NON-NLS-1$
				+ ("_filePathName=" + _requestedImageKey + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("imageQuality=" + _requestedImageQuality + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("photo=" + _photo) //$NON-NLS-1$
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
	private BufferedImage transformImageCrop(final BufferedImage thumbImage) {

		final int thumbWidth = thumbImage.getWidth();
		final int thumbHeight = thumbImage.getHeight();
		final int photoWidth = _photo.getWidthRotated();
		final int photoHeight = _photo.getHeightRotated();

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
			if (_photo.getOrientation() <= 1) {
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
		_trackedAWTImages.add(croppedImage);

		BufferedImage rotatedImage = croppedImage;
		if (isRotate) {

			rotatedImage = Scalr.rotate(croppedImage, Rotation.CW_90);
			_trackedAWTImages.add(rotatedImage);
		}

		return rotatedImage;
	}

	/**
	 * @param scaledImage
	 * @return Returns rotated image when orientations is not default
	 */
	private BufferedImage transformImageRotate(final BufferedImage scaledImage) {

		BufferedImage rotatedImage = scaledImage;

		final int orientation = _photo.getOrientation();

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
			_trackedAWTImages.add(rotatedImage);
		}

		return rotatedImage;
	}

	/**
	 * @param loadedImage
	 */
	private void updateImageData(final Image loadedImage) {

		// check if height is set
		if (_photo.getHeight() == Integer.MIN_VALUE) {

			// image dimension is not yet set

			final Rectangle bounds = loadedImage.getBounds();
			_photo.setDimension(bounds.width, bounds.height);
		}
	}
}
