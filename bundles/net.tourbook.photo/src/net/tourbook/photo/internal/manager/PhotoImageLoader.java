/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal.manager;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

import javax.imageio.ImageIO;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.NoAutoScalingImageDataProvider;
import net.tourbook.common.util.SWT2Dutil;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.ImageUtils;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Rotation;

import pixelitor.filters.curves.ToneCurvesFilter;
import pixelitor.gui.GUIMessageHandler;
import pixelitor.utils.Messages;

public class PhotoImageLoader {

   private static IPreferenceStore _prefStore = PhotoActivator.getPrefStore();

   static {

      Messages.setHandler(new GUIMessageHandler());

   }

   private Photo                    _photo;
   private ImageQuality             _requestedImageQuality;
   private int                      _hqImageSize;
   private String                   _requestedImageKey;

   private ILoadCallBack            _loadCallBack;

   private Display                  _display;

   /**
    * Contains AWT images which are disposed after loading
    */
   private ArrayList<BufferedImage> _trackedAWTImages = new ArrayList<>();

   /**
    * Contains SWT images which are disposed after loading
    */
   private ArrayList<Image>         _trackedSWTImages = new ArrayList<>();

   private int[]                    _recursiveCounter = { 0 };

   public PhotoImageLoader(final Display display,
                           final Photo photo,
                           final ImageQuality imageQuality,
                           final int hqImageSize,
                           final ILoadCallBack loadCallBack) {

      _display = display;
      _photo = photo;
      _requestedImageQuality = imageQuality;
      _hqImageSize = hqImageSize;
      _loadCallBack = loadCallBack;

      _requestedImageKey = photo.getImageKey(_requestedImageQuality);
   }

   /**
    * @param notAdjustedImage
    *
    * @return
    */
   private BufferedImage adjustImage(final BufferedImage notAdjustedImage) {

      BufferedImage adjustedImage = null;

      if (_photo.isCropped) {

         adjustedImage = adjustImage_Crop(notAdjustedImage);
      }

      if (_photo.isSetTonality) {

         if (adjustedImage == null) {

            /*
             * Complicated: create a valid image which can be adjusted, there is a bug that a not
             * cropped image is not adjusted with tonality
             */
            adjustedImage = Scalr.crop(notAdjustedImage,
                  0,
                  0,
                  notAdjustedImage.getWidth(),
                  notAdjustedImage.getHeight());
         }

         final BufferedImage srcImage = adjustedImage != null ? adjustedImage : notAdjustedImage;

         final BufferedImage tonalityImage = adjustImage_Tonality(srcImage);

         // replace adjusted image
         if (tonalityImage != null) {

            UI.disposeResource(adjustedImage);
            adjustedImage = tonalityImage;
         }
      }

      if (adjustedImage != null) {

         final String imageKey = _photo.getImageKey(ImageQuality.THUMB_HQ_ADJUSTED);

         PhotoImageCache.putImage_AWT(imageKey, adjustedImage, _photo.imageFilePathName);
      }

      return adjustedImage;
   }

   private BufferedImage adjustImage_Crop(final BufferedImage notAdjustedImage) {

      final int imageWidth = notAdjustedImage.getWidth();
      final int ImageHeight = notAdjustedImage.getHeight();

      final float cropAreaX1 = _photo.cropAreaX1;
      final float cropAreaY1 = _photo.cropAreaY1;
      final float cropAreaX2 = _photo.cropAreaX2;
      final float cropAreaY2 = _photo.cropAreaY2;

      final int cropX1 = (int) (imageWidth * cropAreaX1);
      final int cropY1 = (int) (ImageHeight * cropAreaY1);
      final int cropX2 = (int) (imageWidth * cropAreaX2);
      final int cropY2 = (int) (ImageHeight * cropAreaY2);

      int cropWidth = cropX2 - cropX1;
      int cropHeight = cropY2 - cropY1;

      // fix image size, otherwise it would cause an invalid image
      if (cropWidth == 0) {
         cropWidth = 1;
      }
      if (cropWidth < 0) {
         cropWidth = -cropWidth;
      }
      if (cropHeight == 0) {
         cropHeight = 1;
      }
      if (cropHeight < 0) {
         cropHeight = -cropHeight;
      }

      if (cropX1 + cropWidth > imageWidth) {
         cropWidth = imageWidth - cropX1;
      }

      if (cropAreaY1 + cropHeight > ImageHeight) {
         cropHeight = ImageHeight - cropY1;
      }

      BufferedImage croppedImage = null;
      try {

         croppedImage = Scalr.crop(notAdjustedImage, cropX1, cropY1, cropWidth, cropHeight);

      } catch (final Exception e) {

         StatusUtil.log(e);
      }

      return croppedImage;
   }

   private BufferedImage adjustImage_Tonality(final BufferedImage srcImage) {

      BufferedImage tonalityImage = null;

      try {

         final ToneCurvesFilter toneCurvesFilter = _photo.getToneCurvesFilter();

         tonalityImage = toneCurvesFilter.transformImage(srcImage);

      } catch (final Exception e) {

         StatusUtil.log(e);
      }

      return tonalityImage;
   }

   private Image createSWTimageFromAWTimage(final BufferedImage awtBufferedImage, final String imageFilePath) {

      final ImageData swtImageData = SWT2Dutil.convertToSWT(awtBufferedImage, imageFilePath);

      if (swtImageData != null) {

         // image could be converted

         return new Image(_display, new NoAutoScalingImageDataProvider(swtImageData));
      }

      /*
       * Try to convert it into a JPG file
       */

      String tempFilename = null;
      Image swtImage = null;

      try {

         // get temp file name
         final File tempFile = File.createTempFile("prefix", UI.EMPTY_STRING);//$NON-NLS-1$
         tempFilename = tempFile.getName();
         tempFile.delete();

         ImageIO.write(awtBufferedImage, ThumbnailStore.THUMBNAIL_IMAGE_EXTENSION_JPG, new File(tempFilename));

      } catch (final Exception e) {

         StatusUtil.log("Cannot save resized image with AWT: \"%s\"".formatted(imageFilePath), e); //$NON-NLS-1$

      } finally {

         try {

            // get SWT image from saved AWT image
            swtImage = new Image(_display, tempFilename);

         } catch (final Exception e) {

            StatusUtil.log("Cannot load resized image with SWT: \"%s\"".formatted(tempFilename), e); //$NON-NLS-1$

         } finally {

            // remove temp tile
            new File(tempFilename).delete();
         }
      }

      return swtImage;
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

   private boolean getIsRotateImageAutomatically() {
      return _prefStore.getBoolean(IPhotoPreferences.PHOTO_SYSTEM_IS_ROTATE_IMAGE_AUTOMATICALLY);
   }

   public Photo getPhoto() {
      return _photo;
   }

   public ImageQuality getRequestedImageQuality() {
      return _requestedImageQuality;
   }

   /**
    * @return Returns rotation according to the EXIF data or <code>null</code> when image is not
    *         rotated.
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
    * @param storeImageFilePath
    *           Path to store image in the thumbnail store
    *
    * @return
    */
   private BufferedImage loadImageFromEXIFThumbnail_AWT(final IPath storeImageFilePath) {

      BufferedImage awtBufferedImage = null;

      try {

         // read exif meta data
         final ImageMetadata metadata = _photo.getImageMetaData(true);

         if (metadata == null) {
            return null;
         }

         if (metadata instanceof JpegImageMetadata) {

            awtBufferedImage = ((JpegImageMetadata) metadata).getEXIFThumbnail();

            _trackedAWTImages.add(awtBufferedImage);

            if (awtBufferedImage == null) {
               return null;
            }

            /*
             * Transform EXIF image
             */
            try {

               awtBufferedImage = transformImageCrop(awtBufferedImage);
               awtBufferedImage = transformImageRotate(awtBufferedImage);

               return awtBufferedImage;

            } catch (final Exception e) {

               StatusUtil.log("Image \"%s\" cannot be resized".formatted(_photo.imageFilePathName), e); //$NON-NLS-1$

               return null;
            }
         }

      } catch (final ImageReadException | IOException e) {

         StatusUtil.log(e);

      } finally {

         // set state after creating image
         _photo.setStateExifThumb(awtBufferedImage == null ? 0 : 1);
      }

      return null;
   }

   private void loadImageFromEXIFThumbnail_Original() {

      Image loadedExifImage = null;
      String imageKey = null;

      try {

         // get image from thumbnail image in the EXIF data

         final IPath storeThumbImageFilePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

         final Image exifThumbnail = loadImageFromEXIFThumbnail_SWT(storeThumbImageFilePath);
         if (exifThumbnail != null) {

            // EXIF image is available

            imageKey = _photo.getImageKey(ImageQuality.THUMB);
            loadedExifImage = exifThumbnail;
         }

      } catch (final Exception e) {

      } finally {

         disposeTrackedImages();

         if (loadedExifImage != null) {

            // cache loaded thumb, that the redraw finds the image

            final String originalImagePathName = _photo.imageFilePathName;

            PhotoImageCache.putImage_SWT(imageKey, loadedExifImage, originalImagePathName);

            // display image in the loading callback
//            _loadCallBack.callBackImageIsLoaded(true);
         }
      }
   }

   /**
    * @param storeImageFilePath
    *           Path to store image in the thumbnail store
    *
    * @return
    */
   private Image loadImageFromEXIFThumbnail_SWT(final IPath storeImageFilePath) {

      BufferedImage awtBufferedImage = null;

      try {

         // read exif meta data
         final ImageMetadata metadata = _photo.getImageMetaData(true);

         if (metadata == null) {
            return null;
         }

// this will print out all metadata
//         System.out.println(metadata);

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
                  StatusUtil.log(NLS.bind(
                        "Image \"{0}\" cannot be resized", //$NON-NLS-1$
                        _photo.imageFilePathName), e);
                  return null;
               }

               swtThumbnailImage = createSWTimageFromAWTimage(awtBufferedImage, storeImageFilePath.toOSString());

               // set state after creating image, this could cause an error
               _photo.setStateExifThumb(swtThumbnailImage == null ? 0 : 1);

               if (swtThumbnailImage != null) {
                  return swtThumbnailImage;
               }

            } catch (final Exception e) {
               StatusUtil.log(NLS.bind(
                     "SWT store image \"{0}\" cannot be created", //$NON-NLS-1$
                     storeImageFilePath.toOSString()), e);
            } finally {

               if (swtThumbnailImage == null) {

                  System.out.println(NLS.bind( //
                        UI.timeStampNano() + " EXIF image \"{0}\" cannot be created", //$NON-NLS-1$
                        storeImageFilePath));
               }
            }
         }
      } catch (final ImageReadException | IOException e) {
         StatusUtil.log(e);
      }

      return null;
   }

   /**
    * Get AWT image from thumb store with the requested image quality.
    *
    * @param _photo
    * @param requestedImageQuality
    *
    * @return
    */
   private BufferedImage loadImageFromStore_AWT(final ImageQuality requestedImageQuality) {

      /*
       * check if image is available in the thumbstore
       */
      final IPath requestedStoreImageFilePath = ThumbnailStore.getStoreImagePath(_photo, requestedImageQuality);

      final String imageStoreFilePath = requestedStoreImageFilePath.toOSString();
      final File storeImageFile = new File(imageStoreFilePath);

      if (storeImageFile.isFile() == false) {
         return null;
      }

      // photo image is available in the thumbnail store

      /*
       * touch store file when it is not yet done today, this is done to track last access time so
       * that a store cleanup can check the date
       */
      final LocalDate dtModified = TimeTools.getZonedDateTime(storeImageFile.lastModified()).toLocalDate();

      if (dtModified.equals(LocalDate.now()) == false) {

         storeImageFile.setLastModified(TimeTools.now().toInstant().toEpochMilli());
      }

      BufferedImage awtImage = null;

      try {

         awtImage = ImageIO.read(new File(imageStoreFilePath));

         loadImageProperties(requestedStoreImageFilePath);

      } catch (final Exception e) {

         StatusUtil.log("Image cannot be loaded with AWT: \"%s\"".formatted(imageStoreFilePath), e); //$NON-NLS-1$
      }

      return awtImage;
   }

   /**
    * Get image from thumb store with the requested image quality.
    *
    * @param _photo
    * @param requestedImageQuality
    *
    * @return
    */
   private Image loadImageFromStore_SWT(final ImageQuality requestedImageQuality) {

      /*
       * check if image is available in the thumbstore
       */
      final IPath requestedStoreImageFilePath = ThumbnailStore.getStoreImagePath(_photo, requestedImageQuality);

      final String imageStoreFilePath = requestedStoreImageFilePath.toOSString();
      final File storeImageFile = new File(imageStoreFilePath);

      if (storeImageFile.isFile() == false) {
         return null;
      }

      // photo image is available in the thumbnail store

      Image swtImage = null;

      /*
       * touch store file when it is not yet done today, this is done to track last access time so
       * that a store cleanup can check the date
       */
      final LocalDate dtModified = TimeTools.getZonedDateTime(storeImageFile.lastModified()).toLocalDate();

      if (dtModified.equals(LocalDate.now()) == false) {

         storeImageFile.setLastModified(TimeTools.now().toInstant().toEpochMilli());
      }

      try {

         final BufferedImage awtImage = ImageIO.read(new File(imageStoreFilePath));

         swtImage = new Image(Display.getCurrent(), new NoAutoScalingImageDataProvider(awtImage));

         loadImageProperties(requestedStoreImageFilePath);

      } catch (final Exception e) {

         StatusUtil.log("Image cannot be loaded with SWT (1): \"%s\"".formatted(imageStoreFilePath), e); //$NON-NLS-1$

      } finally {

         if (swtImage == null) {

            final String message = "Image \"{0}\" cannot be loaded and an exception did not occure.\n" //$NON-NLS-1$
                  + "The image file is available but it's possible that SWT.ERROR_NO_HANDLES occurred"; //$NON-NLS-1$

            System.out.println(UI.timeStampNano() + NLS.bind(message, imageStoreFilePath));

            PhotoImageCache.disposeResizedImage(null);

            /*
             * try loading again
             */
            try {

               swtImage = new Image(_display, imageStoreFilePath);

            } catch (final Exception e) {

               StatusUtil.log("Image cannot be loaded with SWT (2): \"%s\"".formatted(imageStoreFilePath), e); //$NON-NLS-1$

            } finally {

               if (swtImage == null) {

                  System.out.println(UI.timeStampNano()
                        + "Image cannot be loaded again with SWT, even when disposing the image cache: \"%s\" " //$NON-NLS-1$
                              .formatted(imageStoreFilePath));
               }
            }
         }
      }

      return swtImage;
   }

   /**
    * Image could not be loaded with {@link #loadImage()}, try to load high quality image.
    *
    * @param thumbImageWaitingQueue
    *           waiting queue for small images
    * @param exifWaitingQueue
    * @param isAWTImage
    */
   public void loadImageHQ(final LinkedBlockingDeque<PhotoImageLoader> thumbImageWaitingQueue,
                           final LinkedBlockingDeque<PhotoExifLoader> exifWaitingQueue) {

      /*
       * Wait until exif data and small images are loaded
       */
      try {

         while (thumbImageWaitingQueue.size() > 0 || exifWaitingQueue.size() > 0) {
            Thread.sleep(PhotoLoadManager.DELAY_TO_CHECK_WAITING_QUEUE);
         }

      } catch (final InterruptedException e) {

         // should not happen, I hope so
      }

      boolean isLoadingError = false;
      Image hqImage = null;

      try {

         hqImage = loadImageHQ_10();

      } catch (final Exception e) {

         setState_LoadingError();

         isLoadingError = true;

      } finally {

         disposeTrackedImages();

         if (hqImage == null) {

            System.out.println(UI.timeStampNano() + " image == NULL when loading: \"%s\"".formatted(_photo.imageFilePathName)); //$NON-NLS-1$
         }

         // update image state
         final boolean isImageLoaded = hqImage != null;
         if (isImageLoaded) {

            setState_Undefined();

         } else {

            if (_recursiveCounter[0] > 2) {

               /**
                * This may occur when heap stack is full, e.g. "[158.711s][warning][gc,alloc]
                * LoadImg-HQ-6: Retried waiting for GCLocker too often allocating 6718466 words"
                */

               System.out.println(UI.timeStampNano()
                     + " Heap may be full when loading \"%s\"".formatted(_photo.imageFilePathName)); //$NON-NLS-1$
            }

            setState_LoadingError();

            isLoadingError = true;
         }

         // display image in the loading callback
         _loadCallBack.callBackImageIsLoaded(isImageLoaded || isLoadingError);
      }
   }

   private Image loadImageHQ_10() throws Exception {

      // prevent recursive calls
      if (_recursiveCounter[0]++ > 2) {
         return null;
      }

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

      ImageCacheWrapper imageCacheWrapper = null;

      BufferedImage awtHQImage = null;
      BufferedImage awtOriginalImage = null;

      /*
       * Load original image
       */
      final String originalImagePathName = _photo.imageFilePathName;
      try {

         final long startHqLoad = System.currentTimeMillis();
         {
            awtOriginalImage = ImageIO.read(_photo.imageFile);

            _trackedAWTImages.add(awtOriginalImage);
         }
         endHqLoad = System.currentTimeMillis() - startHqLoad;

      } catch (final Exception e) {

         StatusUtil.logError("AWT: image \"%s\" cannot be loaded.".formatted(originalImagePathName)); //$NON-NLS-1$

      } finally {

         if (awtOriginalImage == null) {

            System.out.println(UI.timeStampNano() + " AWT: image \"%s\" cannot be loaded".formatted(originalImagePathName)); //$NON-NLS-1$

            return null;
         }
      }

      /*
       * Handle thumb save error
       */
      final boolean isThumbSaveError = PhotoLoadManager.isThumbSaveError(originalImagePathName);
      if (isThumbSaveError) {

         // the thumb image could not be previously saved in the thumb store, display original image

         final Image swtImage = createSWTimageFromAWTimage(awtOriginalImage, originalImagePathName);

         if (swtImage == null) {

            exceptionMessage = "Photo image with thumb save error cannot be created with SWT (1): %s".formatted(originalImagePathName); //$NON-NLS-1$

         } else {

            requestedSWTImage = swtImage;
         }

      } else {

         /*
          * Create HQ image from original image
          */

         boolean isHQCreated = false;

         final int originalImageWidth = awtOriginalImage.getWidth();
         final int originalImageHeight = awtOriginalImage.getHeight();

         final Properties originalImageProperties = new Properties();
         originalImageProperties.put(ThumbnailStore.ORIGINAL_IMAGE_WIDTH, Integer.toString(originalImageWidth));
         originalImageProperties.put(ThumbnailStore.ORIGINAL_IMAGE_HEIGHT, Integer.toString(originalImageHeight));

         int imageWidth = originalImageWidth;
         int imageHeight = originalImageHeight;

         // update dimension
         updatePhotoImageSize(imageWidth, imageHeight, true);

         if (imageWidth >= _hqImageSize || imageHeight >= _hqImageSize) {

            // the original image is larger than HQ image -> resize it to HQ

            BufferedImage scaledHQImage;

            final long startResizeHQ = System.currentTimeMillis();
            {
               final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, _hqImageSize, _hqImageSize);
               final int maxSize = Math.max(bestSize.x, bestSize.y);

               scaledHQImage = Scalr.resize(awtOriginalImage, Method.QUALITY, maxSize);

               _trackedAWTImages.add(scaledHQImage);

               // rotate image according to the EXIF flag
               if (isRotated == false) {
                  isRotated = true;

                  scaledHQImage = transformImageRotate(scaledHQImage);
               }

               awtHQImage = scaledHQImage;

               imageWidth = scaledHQImage.getWidth();
               imageHeight = scaledHQImage.getHeight();
            }
            endResizeHQ = System.currentTimeMillis() - startResizeHQ;

            /*
             * Save scaled HQ image in store
             */
            final long startSaveHQ = System.currentTimeMillis();
            {
               final boolean isSaved = ThumbnailStore.saveResizedImage_AWT(
                     scaledHQImage,
                     ThumbnailStore.getStoreImagePath(_photo, ImageQuality.HQ),
                     originalImageProperties);

               if (isSaved == false) {

                  // AWT save error has occurred, possible error: "Bogus input colorspace"
                  PhotoLoadManager.putPhotoInThumbSaveErrorMap(originalImagePathName);
               }

               // check if the scaled image has the requested image quality
               if (_requestedImageQuality == ImageQuality.HQ) {

                  // create swt image from saved AWT image, this converts AWT -> SWT

                  requestedSWTImage = loadImageFromStore_SWT(ImageQuality.HQ);
               }
            }
            endSaveHQ = System.currentTimeMillis() - startSaveHQ;

            isHQCreated = true;

         } else {

            awtHQImage = awtOriginalImage;
         }

         /*
          * Create thumb image from HQ image
          */
         BufferedImage awtSaveThumbImage = null;

         final int thumbSize = PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;
         if (imageWidth >= thumbSize || imageHeight >= thumbSize) {

            /*
             * image is larger than thumb image -> resize to thumb
             */

            if (isHQCreated == false) {

               // image size is between thumb and HQ

               if (_requestedImageQuality == ImageQuality.HQ) {
                  requestedSWTImage = createSWTimageFromAWTimage(awtOriginalImage, originalImagePathName);
               }
            }

            if (_photo.getExifThumbImageState() == 1) {

               if (requestedSWTImage == null) {

                  // get thumb image

                  final IPath storeImageFilePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);
                  requestedSWTImage = loadImageFromEXIFThumbnail_SWT(storeImageFilePath);
               }

            } else {

               // check if thumb image is already available
               final Image exifThumbImage = loadImageFromStore_SWT(ImageQuality.THUMB);
               if (exifThumbImage != null) {

                  // EXIF thumb image is already available in the thumbstore

                  if (requestedSWTImage == null && _requestedImageQuality == ImageQuality.THUMB) {

                     requestedSWTImage = exifThumbImage;
                  } else {
                     _trackedSWTImages.add(exifThumbImage);
                  }

               } else {

                  /*
                   * Create thumb image
                   */

                  BufferedImage awtScaledThumbImage;
                  final long startResizeThumb = System.currentTimeMillis();
                  {
                     final Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, thumbSize, thumbSize);
                     final int maxSize = Math.max(bestSize.x, bestSize.y);

                     awtScaledThumbImage = Scalr.resize(awtHQImage, Method.QUALITY, maxSize);

                     _trackedAWTImages.add(awtScaledThumbImage);

                     // rotate image according to the exif flag
                     if (isRotated == false) {

                        isRotated = true;

                        awtScaledThumbImage = transformImageRotate(awtScaledThumbImage);
                     }

                     awtSaveThumbImage = awtScaledThumbImage;
                  }
                  endResizeThumb = System.currentTimeMillis() - startResizeThumb;
               }
            }

         } else {

            // loaded image is smaller than a thumb image

            awtSaveThumbImage = awtOriginalImage;
         }

         /*
          * Save thumb image
          */
         if (awtSaveThumbImage == awtOriginalImage) {

            // original image is not saved as a thumb

            if (requestedSWTImage == null) {

               requestedSWTImage = createSWTimageFromAWTimage(awtSaveThumbImage, originalImagePathName);

               if (requestedSWTImage == null) {
                  exceptionMessage = "Photo image cannot be converted from AWT to SWT: %s".formatted(originalImagePathName); //$NON-NLS-1$
               }
            }

         } else {

            boolean isSaved = true;

            if (awtSaveThumbImage != null) {

               final long startSaveThumb = System.currentTimeMillis();
               {
                  final IPath storeThumbImagePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

                  isSaved = ThumbnailStore.saveResizedImage_AWT(
                        awtSaveThumbImage,
                        storeThumbImagePath,
                        originalImageProperties);
               }
               endSaveThumb = System.currentTimeMillis() - startSaveThumb;
            }

            if (isSaved == false) {

               // AWT save error has occurred, possible error: "Bogus input colorspace"
               PhotoLoadManager.putPhotoInThumbSaveErrorMap(originalImagePathName);

               if (requestedSWTImage == null) {

                  requestedSWTImage = createSWTimageFromAWTimage(awtSaveThumbImage, originalImagePathName);

                  if (requestedSWTImage == null) {
                     exceptionMessage = "Photo image with thumb save error cannot be created with SWT (2): %s".formatted(originalImagePathName); //$NON-NLS-1$
                  }
               }
            }
         }

         // check if the requested image is set, if not load thumb
         if (requestedSWTImage == null) {

            // create swt image from saved AWT image (convert AWT->SWT)

            requestedSWTImage = loadImageFromStore_SWT(ImageQuality.THUMB);
         }

         if (requestedSWTImage != null) {

            // ensure metadata are loaded
            _photo.getImageMetaData();

            // keep requested image in cache
            imageCacheWrapper = PhotoImageCache.putImage_SWT(_requestedImageKey, requestedSWTImage, originalImagePathName);
         }

         if (requestedSWTImage == null) {
            setState_LoadingError();
         }
      }

      // keep AWT image in the cache
      if (imageCacheWrapper != null && awtHQImage != null) {

         imageCacheWrapper.awtImage = awtHQImage;
      }

      final long end = System.currentTimeMillis() - start;

      final String text = " AWT: " //$NON-NLS-1$
            + "%-15s " //$NON-NLS-1$
            + "%-15s  " //$NON-NLS-1$
            + "total: %5d  " //$NON-NLS-1$
            + "load: %5d  " //$NON-NLS-1$
            + "resizeHQ: %3d  " //$NON-NLS-1$
            + "saveHQ: %4d  " //$NON-NLS-1$
            + "resizeThumb: %3d  " //$NON-NLS-1$
            + "saveThumb: %3d"; //$NON-NLS-1$

      System.out.println(UI.timeStampNano() + text.formatted(

            Thread.currentThread().getName(),
            _photo.imageFileName,

            end,
            endHqLoad,
            endResizeHQ,
            endSaveHQ,
            endResizeThumb,
            endSaveThumb));

      if (exceptionMessage != null) {
         throw new Exception(exceptionMessage);
      }

      return requestedSWTImage;
   }

   /**
    * Image could not be loaded with {@link #loadImage()}, try to load high quality image.
    *
    * @param thumbImageWaitingQueue
    *           waiting queue for small images
    * @param exifWaitingQueue
    * @param photo
    * @param imageQuality
    */
   public void loadImageHQThumb_Map(final LinkedBlockingDeque<PhotoImageLoader> thumbImageWaitingQueue,
                                    final LinkedBlockingDeque<PhotoExifLoader> exifWaitingQueue,
                                    final Photo photo,
                                    final ImageQuality imageQuality) {

      /*
       * Wait until exif data and small images are loaded
       */
      try {

         while (thumbImageWaitingQueue.size() > 0 || exifWaitingQueue.size() > 0) {
            Thread.sleep(PhotoLoadManager.DELAY_TO_CHECK_WAITING_QUEUE);
         }

      } catch (final InterruptedException e) {

         // should not happen, I hope so
      }

      boolean isLoadingError = false;
      BufferedImage hqThumbImage = null;

      // reset adjustment state that it is not reloaded again
      photo.isAdjustmentModified = false;

      try {

         // load original image and create thumbs

         if (imageQuality == ImageQuality.THUMB_HQ_ADJUSTED) {

            // it is possible that the not cropped image is already loaded -> only crop it

            final BufferedImage notAdjustedImage = PhotoImageCache.getImage_AWT(photo, ImageQuality.HQ);

            if (notAdjustedImage != null) {
               hqThumbImage = adjustImage(notAdjustedImage);
            }
         }

         if (hqThumbImage == null) {
            hqThumbImage = loadImageHQThumb_Map_10(imageQuality);
         }

      } catch (final Exception e) {

         setState_LoadingError();

         isLoadingError = true;

      } finally {

         disposeTrackedImages();

         // update image state
         final boolean isImageLoaded = hqThumbImage != null;
         if (isImageLoaded) {

            setState_Undefined();

         } else {

            if (_recursiveCounter[0] > 2) {

               /**
                * This may occur when heap stack is full, e.g. "[158.711s][warning][gc,alloc]
                * LoadImg-HQ-6: Retried waiting for GCLocker too often allocating 6718466 words"
                */

               System.out.println(UI.timeStampNano()
                     + " Heap may be full when loading \"%s\"".formatted(_photo.imageFilePathName)); //$NON-NLS-1$
            }

            setState_LoadingError();

            isLoadingError = true;
         }

         // display image in the loading callback
         _loadCallBack.callBackImageIsLoaded(isImageLoaded || isLoadingError);
      }
   }

   private BufferedImage loadImageHQThumb_Map_10(final ImageQuality imageQuality) throws Exception {

      // prevent recursive calls
      if (_recursiveCounter[0]++ > 2) {
         return null;
      }

      final long start = System.currentTimeMillis();
      long endHqLoad = 0;
      long endResizeHQ = 0;
      long endSaveHQ = 0;

      BufferedImage awtOriginalImage = null;
      BufferedImage awtHQImage = null;

      /*
       * Load original image
       */
      final String originalImagePathName = _photo.imageFilePathName;
      try {

         final long startHqLoad = System.currentTimeMillis();
         {
            awtOriginalImage = ImageIO.read(_photo.imageFile);

            _trackedAWTImages.add(awtOriginalImage);
         }
         endHqLoad = System.currentTimeMillis() - startHqLoad;

      } catch (final Exception e) {

         StatusUtil.logError("AWT: image \"%s\" cannot be loaded.".formatted(originalImagePathName)); //$NON-NLS-1$

      } finally {

         if (awtOriginalImage == null) {

            System.out.println(UI.timeStampNano() + " AWT: image \"%s\" cannot be loaded, will load with SWT".formatted(originalImagePathName)); //$NON-NLS-1$

            return null;
         }
      }

      final int originalImageWidth = awtOriginalImage.getWidth();
      final int originalImageHeight = awtOriginalImage.getHeight();

      /*
       * Create HQ thumb image from original image
       */

      // update dimension
      updatePhotoImageSize(

            originalImageWidth,
            originalImageHeight,

            true // isOriginalSize
      );

      if (originalImageWidth >= _hqImageSize || originalImageHeight >= _hqImageSize) {

         // the original image is larger than HQ image -> resize it to HQ

         BufferedImage scaledHQImage;

         final long startResizeHQ = System.currentTimeMillis();
         {
            final Point bestSize = ImageUtils.getBestSize(originalImageWidth, originalImageHeight, _hqImageSize, _hqImageSize);
            final int scaleWidth = bestSize.x;
            final int scaledHeight = bestSize.y;

            final int maxSize = Math.max(scaleWidth, scaledHeight);
            scaledHQImage = Scalr.resize(awtOriginalImage, Method.QUALITY, maxSize);

            _trackedAWTImages.add(scaledHQImage);

            // rotate image according to the EXIF flag
            scaledHQImage = transformImageRotate(scaledHQImage);

            awtHQImage = scaledHQImage;
         }
         endResizeHQ = System.currentTimeMillis() - startResizeHQ;

         /*
          * Keep image in cache
          */
         final String imageKey = _photo.getImageKey(ImageQuality.THUMB_HQ);

         PhotoImageCache.putImage_AWT(imageKey, awtHQImage, originalImagePathName);

         /*
          * Save scaled thumb HQ image in store
          */
         final long startSaveHQ = System.currentTimeMillis();
         {
            ThumbnailStore.saveResizedImage_AWT(
                  awtHQImage,
                  ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB_HQ),
                  null);
         }
         endSaveHQ = System.currentTimeMillis() - startSaveHQ;

      } else {

         awtHQImage = awtOriginalImage;
      }

      /*
       * Crop image NOW, to prevent flickering when cropping is done later
       */
      if (imageQuality == ImageQuality.THUMB_HQ_ADJUSTED) {

         // rotate image according to the EXIF flag
         final BufferedImage awtRotatedImage = transformImageRotate(awtOriginalImage);

         final BufferedImage awtAdjustedImage = adjustImage(awtRotatedImage);

         if (awtAdjustedImage != null) {

            awtHQImage.flush();

            awtHQImage = awtAdjustedImage;

            final String imageKey = _photo.getImageKey(ImageQuality.THUMB_HQ_ADJUSTED);

            PhotoImageCache.putImage_AWT(imageKey, awtHQImage, originalImagePathName);
         }
      }

      logImageLoading(start, endHqLoad, endResizeHQ, endSaveHQ);

      return awtHQImage;
   }

   public void loadImageOriginal() {

      final long start = System.currentTimeMillis();
      long endOriginalLoad1 = 0;
      long endOriginalLoad2 = 0;
      long endRotate = 0;

      /*
       * display thumb image during loading the original when it's not in the cache, when it's in
       * the cache, the thumb is already displayed
       */
      final Image photoImage = PhotoImageCache.getImage_SWT(_photo, ImageQuality.THUMB);
      if (photoImage == null || photoImage.isDisposed()) {
         loadImageFromEXIFThumbnail_Original();
      }

      /*
       * ensure metadata are loaded, is needed for image rotation, it can be not loaded when many
       * images are in the gallery and loading exif data has not yet finished
       */
      @SuppressWarnings("unused")
      final PhotoImageMetadata imageMetaData = _photo.getImageMetaData();

      boolean isLoadingException = false;
      Image swtImage = null;
      final String originalImagePathName = _photo.imageFilePathName;

      try {

         /*
          * 1st try: load original image only with SWT
          */
         try {

            final long startLoading = System.currentTimeMillis();

            swtImage = new Image(_display, originalImagePathName);

            endOriginalLoad1 = System.currentTimeMillis() - startLoading;

         } catch (final Exception e) {

            isLoadingException = true;

            System.out.println(NLS.bind(
                  "SWT: image \"{0}\" cannot be loaded (1)", //$NON-NLS-1$
                  originalImagePathName));

         } finally {

            if (swtImage == null) {

               /*
                * 2nd try
                */

               System.out.println(NLS.bind( //
                     UI.timeStampNano() + " SWT: image \"{0}\" cannot be loaded (2)", //$NON-NLS-1$
                     originalImagePathName));

               /**
                * sometimes (when images are loaded concurrently) larger images could not be
                * loaded with SWT methods in Win7 (Eclipse 3.8 M6), try to load image with AWT.
                * This bug fix <code>
                * https://bugs.eclipse.org/bugs/show_bug.cgi?id=350783
                * https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845
                * </code>
                * has not solved this problem
                */

               PhotoImageCache.disposeOriginal(null);
               PhotoImageCache.disposeResizedImage(null);

               try {

                  final long startLoading = System.currentTimeMillis();

                  swtImage = new Image(_display, originalImagePathName);

                  endOriginalLoad2 = System.currentTimeMillis() - startLoading;

               } catch (final Exception e) {

                  isLoadingException = true;

                  System.out.println(NLS.bind(
                        "SWT: image \"{0}\" cannot be loaded (3)", //$NON-NLS-1$
                        originalImagePathName));

               } finally {

                  if (swtImage == null) {
                     System.out.println(NLS.bind( //
                           UI.timeStampNano() + " SWT: image \"{0}\" cannot be loaded (4)", //$NON-NLS-1$
                           originalImagePathName));
                  }
               }
            }
         }

      } finally {

         /**
          * The not thrown NO MORE HANDLE EXCEPTION cannot be caught therefore the check:
          * swtImage == null
          */

         disposeTrackedImages();

         if (swtImage != null) {

            final Rectangle imageBounds = swtImage.getBounds();
            final int imageWidth = imageBounds.width;
            final int imageHeight = imageBounds.height;

            // update dimension
            updatePhotoImageSize(imageWidth, imageHeight, true);

            /*
             * rotate image when necessary
             */

            final Rotation rotation = getRotation();
            if (rotation != null) {

               // image must be rotated

               final long startRotate = System.currentTimeMillis();

               final boolean isRotateImageAutomatically = getIsRotateImageAutomatically();
               final Image rotatedImage = net.tourbook.common.util.ImageUtils.resize(
                     _display,
                     swtImage,
                     imageWidth,
                     imageHeight,
                     SWT.ON,
                     SWT.LOW,
                     rotation,
                     isRotateImageAutomatically);

               swtImage.dispose();
               swtImage = rotatedImage;

               endRotate = System.currentTimeMillis() - startRotate;
            }

            // keep requested image in cache
            PhotoImageCache.putImageOriginal(_requestedImageKey, swtImage, originalImagePathName);
         }

         if (isLoadingException) {
            setState_LoadingError();

         } else {

            // image is loaded with requested quality or a SWT error has occurred, reset image state
            setState_Undefined();
         }

         final long end = System.currentTimeMillis() - start;

         System.out.println(UI.timeStampNano() + " SWT: " //$NON-NLS-1$
               + Thread.currentThread().getName()
               + UI.SPACE1
               + _photo.imageFileName
               + ("\ttotal: " + end) //$NON-NLS-1$
               + ("\tload1: " + endOriginalLoad1) //$NON-NLS-1$
               + ("\tload2: " + endOriginalLoad2) //$NON-NLS-1$
               + ("\trotate: " + endRotate) //$NON-NLS-1$
         //
         );

         // display image in the loading callback
         _loadCallBack.callBackImageIsLoaded(true);
      }
   }

   /**
    * @param requestedStoreImageFilePath
    *
    * @return Returns <code>null</code> when properties cannot be loaded.
    */
   private void loadImageProperties(final IPath requestedStoreImageFilePath) {

      final IPath propPath = ThumbnailStore.getPropertiesPathFromImagePath(requestedStoreImageFilePath);

      Properties imageProperties = null;

      try (FileInputStream fileStream = new FileInputStream(propPath.toOSString())) {

         final File propFile = propPath.toFile();

         if (propFile.isFile() == false) {
            return;
         }

         imageProperties = new Properties();

         imageProperties.load(fileStream);

      } catch (final IOException e) {
         StatusUtil.log(NLS.bind("Image properties cannot be loaded from: \"{0}\"", //$NON-NLS-1$
               propPath.toOSString()), e);
      }

      if (imageProperties != null) {

         final String originalImageWidth = imageProperties.getProperty(ThumbnailStore.ORIGINAL_IMAGE_WIDTH);
         final String originalImageHeight = imageProperties.getProperty(ThumbnailStore.ORIGINAL_IMAGE_HEIGHT);

         if (originalImageWidth != null && originalImageHeight != null) {
            try {

               final int width = Integer.parseInt(originalImageWidth);
               final int height = Integer.parseInt(originalImageHeight);

               _photo.setPhotoSize(width, height);

            } catch (final NumberFormatException e) {
               StatusUtil.log(e);
            }
         }
      }
   }

   /**
    * This is called from the executor when the loading task is starting. It loads an image and
    * puts it into the image cache from where it is fetched when painted.
    *
    * @param waitingqueueoriginal
    * @param waitingqueueexif
    *
    * @return Returns <code>true</code> when image should be loaded in HQ.
    */
   public boolean loadImageThumb_AWT(final LinkedBlockingDeque<PhotoImageLoader> waitingQueueOriginal) {

      /*
       * Wait until original images are loaded
       */
      try {

         while (waitingQueueOriginal.size() > 0) {

            Thread.sleep(PhotoLoadManager.DELAY_TO_CHECK_WAITING_QUEUE);
         }

      } catch (final InterruptedException e) {

         // this should not happen, I hope so
         Thread.currentThread().interrupt();
      }

      boolean isLoadedImageInRequestedQuality = false;
      BufferedImage loadedExifImage = null;
      String imageKey = null;
      boolean isLoadingError = false;

      boolean isHQRequired = false;

      try {

         // 1. get image with the requested quality from the image store

         final BufferedImage storeImage = loadImageFromStore_AWT(_requestedImageQuality);
         if (storeImage != null) {

            isLoadedImageInRequestedQuality = true;

            imageKey = _requestedImageKey;
            loadedExifImage = storeImage;

         } else {

            // 2. get image from thumbnail image in the EXIF data

            final IPath storeThumbImageFilePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

            final BufferedImage awtExifThumbnail = loadImageFromEXIFThumbnail_AWT(storeThumbImageFilePath);
            if (awtExifThumbnail != null) {

               // EXIF image is available

               isLoadedImageInRequestedQuality = _requestedImageQuality == ImageQuality.THUMB;

               imageKey = _photo.getImageKey(ImageQuality.THUMB);
               loadedExifImage = awtExifThumbnail;

               // update photo thumb image size
               _photo.setThumbSize(awtExifThumbnail.getWidth(), awtExifThumbnail.getHeight());
            }
         }

      } catch (final Exception e) {

         setState_LoadingError();

         isLoadingError = true;

      } finally {

         disposeTrackedImages();

         final boolean isImageLoaded = loadedExifImage != null;

         /*
          * keep image in cache
          */
         if (isImageLoaded) {

            final String originalImagePathName = _photo.imageFilePathName;

            // ensure metadata are loaded
            _photo.getImageMetaData();

            // check if width is set
            if (_photo.getPhotoImageWidth() == Integer.MIN_VALUE) {

               // photo image width/height is not set from metadata, set it from the image

               // update dimension
               updatePhotoImageSize(loadedExifImage.getWidth(), loadedExifImage.getHeight(), false);
            }

            PhotoImageCache.putImage_AWT(imageKey, loadedExifImage, originalImagePathName);
         }

         /*
          * update loading state
          */
         if (isLoadedImageInRequestedQuality) {

            // image is loaded with requested quality, reset image state

            setState_Undefined();

         } else {

            // load image with higher quality

            isHQRequired = true;
         }

         // show in the UI, that meta data are loaded, loading message is displayed with another color
         final boolean isUpdateUI = _photo.getImageMetaDataRaw() != null;

         // display image in the loading callback
         _loadCallBack.callBackImageIsLoaded(isUpdateUI || isImageLoaded || isLoadingError);
      }

      return isHQRequired;
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
    * Photo-Image-Loader-1   IMG_1219_10.JPG  load:   1165   resize:   645   save:   110  total:   1920
    * Photo-Image-Loader-0   IMG_1219_9.JPG   load:   1165   resize:   650   save:   110  total:   1925
    * Photo-Image-Loader-1   IMG_1219.JPG     load:   566    resize:   875   save:   60   total:   1501
    * Photo-Image-Loader-0   IMG_1219_2.JPG   load:   835    resize:   326   save:   55   total:   1216
    * Photo-Image-Loader-1   IMG_1219_3.JPG   load:   1150   resize:   625   save:   55   total:   1830
    * Photo-Image-Loader-0   IMG_1219_4.JPG   load:   565    resize:   630   save:   60   total:   1255
    * Photo-Image-Loader-1   IMG_1219_5.JPG   load:   566    resize:   880   save:   60   total:   1506
    * Photo-Image-Loader-0   IMG_1219_6.JPG   load:   845    resize:   341   save:   65   total:   1251
    * Photo-Image-Loader-1   IMG_1219_7.JPG   load:   575    resize:   875   save:   50   total:   1500
    * Photo-Image-Loader-0   IMG_1219_8.JPG   load:   845    resize:   356   save:   45   total:   1246
    *                                                8277             6203          670           15150
    *
    *
    * AWT
    * Photo-Image-Loader-1   IMG_1219_9.JPG   load:  1005      resize:   770      save AWT:   25   load SWT:  10   total:   1810
    * Photo-Image-Loader-0   IMG_1219_10.JPG  load:  1015      resize:  1311      save AWT:  145   load SWT:   5   total:   2476
    * Photo-Image-Loader-1   IMG_1219.JPG     load:   931      resize:   755      save AWT:   65   load SWT:   5   total:   1756
    * Photo-Image-Loader-0   IMG_1219_2.JPG   load:   960      resize:   737      save AWT:   30   load SWT:   5   total:   1732
    * Photo-Image-Loader-1   IMG_1219_3.JPG   load:  1340      resize:   700      save AWT:   25   load SWT:  10   total:   2075
    * Photo-Image-Loader-0   IMG_1219_4.JPG   load:   935      resize:   751      save AWT:   25   load SWT:  10   total:   1721
    * Photo-Image-Loader-1   IMG_1219_5.JPG   load:   981      resize:   810      save AWT:   25   load SWT:   5   total:   1821
    * Photo-Image-Loader-0   IMG_1219_6.JPG   load:   970      resize:   821      save AWT:   30   load SWT:   5   total:   1826
    * Photo-Image-Loader-1   IMG_1219_7.JPG   load:   950      resize:   710      save AWT:   25   load SWT:   5   total:   1690
    * Photo-Image-Loader-0   IMG_1219_8.JPG   load:   950      resize:   706      save AWT:   30   load SWT:   5   total:   1691
    *                                               10037               8071                 425              65           18598
    *
    * 1 Thread
    * ========
    *
    * SWT
    * Photo-Image-Loader-0   IMG_1219_10.JPG  load:   595   resize:   330   save:   70   total:   995
    * Photo-Image-Loader-0   IMG_1219.JPG     load:   561   resize:   325   save:   80   total:   966
    * Photo-Image-Loader-0   IMG_1219_2.JPG   load:   560   resize:   330   save:   50   total:   940
    * Photo-Image-Loader-0   IMG_1219_3.JPG   load:   561   resize:   325   save:   45   total:   931
    * Photo-Image-Loader-0   IMG_1219_4.JPG   load:   570   resize:   325   save:   50   total:   945
    * Photo-Image-Loader-0   IMG_1219_5.JPG   load:   570   resize:   340   save:   50   total:   960
    * Photo-Image-Loader-0   IMG_1219_6.JPG   load:   575   resize:   330   save:   45   total:   950
    * Photo-Image-Loader-0   IMG_1219_7.JPG   load:   560   resize:   335   save:   50   total:   945
    * Photo-Image-Loader-0   IMG_1219_8.JPG   load:   565   resize:   330   save:   45   total:   940
    * Photo-Image-Loader-0   IMG_1219_9.JPG   load:   565   resize:   330   save:   45   total:   940
    *                                                5682            3300          530           9512
    *
    * AWT
    * Photo-Image-Loader-0   IMG_1219.JPG     load:   1115   resize:   790   save AWT:   45   load SWT:   5   total:   1955
    * Photo-Image-Loader-0   IMG_1219_2.JPG   load:   1070   resize:   695   save AWT:   30   load SWT:   5   total:   1800
    * Photo-Image-Loader-0   IMG_1219_3.JPG   load:   1035   resize:   695   save AWT:   25   load SWT:   5   total:   1760
    * Photo-Image-Loader-0   IMG_1219_4.JPG   load:   1040   resize:   695   save AWT:   25   load SWT:   5   total:   1765
    * Photo-Image-Loader-0   IMG_1219_5.JPG   load:   1040   resize:   695   save AWT:   25   load SWT: 110   total:   1870
    * Photo-Image-Loader-0   IMG_1219_6.JPG   load:   1050   resize:   690   save AWT:   25   load SWT:   5   total:   1770
    * Photo-Image-Loader-0   IMG_1219_7.JPG   load:   1035   resize:   690   save AWT:  145   load SWT:   5   total:   1875
    * Photo-Image-Loader-0   IMG_1219_8.JPG   load:   1032   resize:   700   save AWT:   20   load SWT:  10   total:   1762
    * Photo-Image-Loader-0   IMG_1219_9.JPG   load:   1030   resize:   700   save AWT:   25   load SWT:   5   total:   1760
    * Photo-Image-Loader-0   IMG_1219_10.JPG  load:   1032   resize:   700   save AWT:   25   load SWT:   5   total:   1762
    *                                                10479            7050              390             160           18079
    * </pre>
    *
    * @param waitingqueueoriginal
    * @param waitingqueueexif
    *
    * @return Returns <code>true</code> when image should be loaded in HQ.
    */
   public boolean loadImageThumb_SWT(final LinkedBlockingDeque<PhotoImageLoader> waitingQueueOriginal) {

      /*
       * Wait until original images are loaded
       */
      try {

         while (waitingQueueOriginal.size() > 0) {

            Thread.sleep(PhotoLoadManager.DELAY_TO_CHECK_WAITING_QUEUE);
         }

      } catch (final InterruptedException e) {

         // should not happen, I hope so
         Thread.currentThread().interrupt();
      }

      boolean isLoadedImageInRequestedQuality = false;
      Image loadedExifImage = null;
      String imageKey = null;
      boolean isLoadingError = false;

      boolean isHQRequired = false;

      try {

         // 1. get image with the requested quality from the image store
         final Image storeImage = loadImageFromStore_SWT(_requestedImageQuality);
         if (storeImage != null) {

            isLoadedImageInRequestedQuality = true;

            imageKey = _requestedImageKey;
            loadedExifImage = storeImage;

         } else {

            // 2. get image from thumbnail image in the EXIF data

//  debug (delay) image loading
//            Thread.sleep(500);

            final IPath storeThumbImageFilePath = ThumbnailStore.getStoreImagePath(_photo, ImageQuality.THUMB);

            final Image exifThumbnail = loadImageFromEXIFThumbnail_SWT(storeThumbImageFilePath);
            if (exifThumbnail != null) {

               // EXIF image is available

               isLoadedImageInRequestedQuality = _requestedImageQuality == ImageQuality.THUMB;

               imageKey = _photo.getImageKey(ImageQuality.THUMB);
               loadedExifImage = exifThumbnail;
            }
         }

      } catch (final Exception e) {

         setState_LoadingError();

         isLoadingError = true;

      } finally {

         disposeTrackedImages();

         final boolean isImageLoaded = loadedExifImage != null;

         /*
          * keep image in cache
          */
         if (isImageLoaded) {

            final String originalImagePathName = _photo.imageFilePathName;

            // ensure metadata are loaded
            _photo.getImageMetaData();

            int imageWidth = _photo.getPhotoImageWidth();
            int imageHeight = _photo.getPhotoImageHeight();

            // check if width is set
            if (imageWidth == Integer.MIN_VALUE) {

               // photo image width/height is not set from metadata, set it from the image

               final Rectangle imageBounds = loadedExifImage.getBounds();
               imageWidth = imageBounds.width;
               imageHeight = imageBounds.height;

               // update dimension
               updatePhotoImageSize(imageWidth, imageHeight, false);
            }

            PhotoImageCache.putImage_SWT(imageKey, loadedExifImage, originalImagePathName);
         }

         /*
          * update loading state
          */
         if (isLoadedImageInRequestedQuality) {

            // image is loaded with requested quality, reset image state

            setState_Undefined();

         } else {

            // load image with higher quality

            isHQRequired = true;
         }

         // show in the UI, that meta data are loaded, loading message is displayed with another color
         final boolean isUpdateUI = _photo.getImageMetaDataRaw() != null;

         // display image in the loading callback
         _loadCallBack.callBackImageIsLoaded(isUpdateUI || isImageLoaded || isLoadingError);
      }

      return isHQRequired;
   }

   private void logImageLoading(final long start, final long endHqLoad, final long endResizeHQ, final long endSaveHQ) {

      final long duration = System.currentTimeMillis() - start;

      final String text = "" //$NON-NLS-1$

            + " AWT: " //$NON-NLS-1$
            + "%-15s " //$NON-NLS-1$
            + "%-15s  " //$NON-NLS-1$

            + "total: %5d  " //$NON-NLS-1$
            + "load: %5d  " //$NON-NLS-1$
            + "resizeHQThumb: %3d  " //$NON-NLS-1$
            + "saveHQThumb: %4d  " //$NON-NLS-1$
      ;

      System.out.println(UI.timeStampNano() + text.formatted(

            Thread.currentThread().getName(),
            _photo.imageFileName,

            duration,
            endHqLoad,
            endResizeHQ,
            endSaveHQ));
   }

   private void setState_LoadingError() {

      // prevent loading the image again
      _photo.setLoadingState(PhotoLoadingState.IMAGE_IS_INVALID, _requestedImageQuality);

      PhotoLoadManager.putPhotoInLoadingErrorMap(_photo.imageFilePathName);
   }

   /**
    * Set state to undefined that it will be loaded again when image is visible and not in the cache
    */
   private void setState_Undefined() {

      _photo.setLoadingState(PhotoLoadingState.UNDEFINED, _requestedImageQuality);
   }

   @Override
   public String toString() {

      return "PhotoImageLoaderItem [" //$NON-NLS-1$

            + "_filePathName=" + _requestedImageKey + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "imageQuality=" + _requestedImageQuality + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "photo=" + _photo //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

   /**
    * Crop thumb image when it has a different ratio than the original image. This will remove the
    * black margins which are set in the thumb image depending on the image ratio.
    *
    * @param thumbImage
    * @param width
    * @param height
    *
    * @return
    */
   private BufferedImage transformImageCrop(final BufferedImage thumbImage) {

      final int thumbWidth = thumbImage.getWidth();
      final int thumbHeight = thumbImage.getHeight();
      final int photoImageWidth = _photo.getAvailableImageWidth();
      final int photoImageHeight = _photo.getAvailableImageHeight();

      final double thumbRatio = (double) thumbWidth / thumbHeight;
      double photoRatio;
      boolean isRotate = false;

      if (photoImageWidth == Integer.MIN_VALUE) {

         return thumbImage;

      } else {

         photoRatio = (double) photoImageWidth / photoImageHeight;

         if (thumbRatio < 1.0 && photoRatio > 1.0 || thumbRatio > 1.0 && photoRatio < 1.0) {

            /*
             * thumb and photo have total different ratios, this can happen when an image is
             * resized or rotated and the thumb image was not adjusted
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
      }

      final int thumbRatioTruncated = (int) (thumbRatio * 100);
      final int photoRatioTruncated = (int) (photoRatio * 100);

      if (thumbRatioTruncated == photoRatioTruncated) {
         // ratio is the same
         return thumbImage;
      }

      int cropX;
      int cropY;
      int cropWidth;
      int cropHeight;

      if (thumbRatioTruncated < photoRatioTruncated) {

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
    * @param providedImage
    *
    * @return Returns rotated image when orientations is not default
    */
   private BufferedImage transformImageRotate(final BufferedImage providedImage) {

      BufferedImage rotatedImage = providedImage;

      final int orientation = _photo.getOrientation();

      if (orientation > 1) {

         // see here http://www.impulseadventure.com/photo/exif-orientation.html

         Rotation correction = null;

// SET_FORMATTING_OFF

         if (       orientation == 8) {   correction = Rotation.CW_270;
         } else if (orientation == 3) {   correction = Rotation.CW_180;
         } else if (orientation == 6) {   correction = Rotation.CW_90;
         }

// SET_FORMATTING_ON

         rotatedImage = Scalr.rotate(providedImage, correction);

         _trackedAWTImages.add(rotatedImage);
      }

      return rotatedImage;
   }

   /**
    * @param imageWidth
    * @param imageHeight
    * @param isOriginalSize
    */
   private void updatePhotoImageSize(final int imageWidth,
                                     final int imageHeight,
                                     final boolean isOriginalSize) {

      if (isOriginalSize) {

         _photo.setPhotoSize(imageWidth, imageHeight);

         // update cached image size

         PhotoImageCache.setImageSize(_photo, imageWidth, imageHeight);

      } else {

         _photo.setThumbSize(imageWidth, imageHeight);
      }
   }

}
