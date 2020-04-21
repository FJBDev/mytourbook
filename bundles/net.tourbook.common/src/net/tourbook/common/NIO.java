/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.common;

import com.github.fge.filesystem.provider.FileSystemRepository;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.common.util.StatusUtil;

/**
 * Tools for the java.nio package.
 */
public class NIO {

   public static final String   DEVICE_FOLDER_NAME_START = "[";                                  //$NON-NLS-1$
   private final static Pattern DRIVE_LETTER_PATTERN     = Pattern.compile("\\s*\\(([^(]*)\\)"); //$NON-NLS-1$

   /** Extracts <code>W530</code> from <code>[W530]\temp\other</code> */
   private final static Pattern DEVICE_NAME_PATTERN      = Pattern.compile("\\s*\\[([^]]*)");    //$NON-NLS-1$

   /** <code>([\\].*)</code> */
   private final static Pattern DEVICE_NAME_PATH_PATTERN = Pattern.compile("([\\\\].*)");        //$NON-NLS-1$

   public static FileSystem     _dropboxFileSystem;

   /**
    * Replace device name with drive letter, e.g. [MEDIA]\CACHE -> D:\CACHE. This do not validate if
    * the path exists.
    *
    * @param folder
    * @return Returns the os path or <code>null</code> when the device name cannot be converted into
    *         a driveletter.
    */
   public static String convertToOSPath(final String folder) {

      if (folder == null) {
         return null;
      }

      String osPath = null;

      if (isDeviceNameFolder(folder)) {

         // replace device name with drive letter, [MEDIA]\CACHE ->  D:\CACHE

         final String deviceName = parseDeviceName(folder);
         final Iterable<FileStore> fileStores = getFileStores();

         for (final FileStore store : fileStores) {

            final String storeName = store.name();

            if (storeName.equals(deviceName)) {

               final String driveLetter = parseDriveLetter(store);
               final String namedPath = parseDeviceNamePath(folder);

               osPath = driveLetter + namedPath;

               break;
            }
         }
      } else if (isDeviceNameCloudFolder(folder)) {
//TODO unnecesary since the line below does the same ?!?!
         osPath = folder;
      } else {

         // OS path is contained in the folder path

         osPath = folder;
      }

      return osPath;
   }

   public static Iterable<FileStore> getFileStores() {

//		final long start = System.nanoTime();
//
      final Iterable<FileStore> systemFileStore = FileSystems.getDefault().getFileStores();

      //create new list
      final ArrayList<FileStore> fileStores = new ArrayList<>();

      //iterate through current objects and add them to new list
      Iterator<FileStore> fileStoresIterator = systemFileStore.iterator();
      while (fileStoresIterator.hasNext()) {
         fileStores.add(fileStoresIterator.next());
      }

      /*
       * TODO put the acess token in commn preferences
       * final IPreferenceStore cloudPreferenceStore =
       * net.tourbook.cloud.Activator.getDefault().getPreferenceStore();
       * final String accessToken =
       * cloudPreferenceStore.getString(ICloudPreferences.DROPBOX_ACCESSTOKEN);
       */
      final URI uri = URI.create("dropbox://root");
      final Map<String, String> env = new HashMap<>();
      env.put("accessToken", "");

      final FileSystemRepository repository = new DropBoxFileSystemRepository();
      final FileSystemProvider provider = new DropBoxFileSystemProvider(repository);

      //final String testDirectoryPath = "/YOUPI-" + UUID.randomUUID().toString();

      final Iterable<FileStore> dropboxFileStore;
      try {
         _dropboxFileSystem = provider.newFileSystem(uri, env);
         final String testDirectoryPath = "/test-" + UUID.randomUUID().toString();


         dropboxFileStore = _dropboxFileSystem.getFileStores();

         //iterate through current objects and add them to new list
         fileStoresIterator = dropboxFileStore.iterator();
         while (fileStoresIterator.hasNext()) {
            fileStores.add(fileStoresIterator.next());
         }

         //Can I do sthing with a filestore ?
         // fileStores.get(0).
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      //TODO when do we close the _dropboxFIleSystem resource ? at the end of the program but where is it ?

      //add object you would like to the list
      //myList.add(dropboxRepository.);

      //
//		System.out.println((UI.timeStampNano() + " " + NIO.class.getName() + " \t")
//				+ (((float) (System.nanoTime() - start) / 1000000) + " ms"));
//		// TODO remove SYSTEM.OUT.PRINTLN

      return fileStores;
   }

   /**
    * @param fileName
    * @return Returns a path or <code>null</code> when an exception occurs.
    */
   public static Path getPath(final String fileName) {

      if (fileName == null) {
         return null;
      }

      try {

         return Paths.get(fileName);

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return null;
   }

   public static boolean isDeviceNameCloudFolder(final String folder) {
      return folder.equalsIgnoreCase("dropboxFolder://");
   }

   /**
    * @param folderName
    * @return Returns <code>true</code> when the folder name starts with
    *         {@value #DEVICE_FOLDER_NAME_START}.
    */
   public static boolean isDeviceNameFolder(final String folderName) {

      if (folderName == null) {
         return false;
      }

      return folderName.startsWith(DEVICE_FOLDER_NAME_START);
   }

   private static String parseDeviceName(final String fullName) {

      final Matcher matcher = DEVICE_NAME_PATTERN.matcher(fullName);

      while (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   private static String parseDeviceNamePath(final String fullName) {

      final Matcher matcher = DEVICE_NAME_PATH_PATTERN.matcher(fullName);

      while (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   public static String parseDriveLetter(final FileStore store) {

      final String fullName = store.toString();

      final Matcher matcher = DRIVE_LETTER_PATTERN.matcher(fullName);

      while (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }
}
