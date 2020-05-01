/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.cloud.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class DropboxClient {

   private static DbxClientV2      _dropboxClient;

   private static DbxRequestConfig _requestConfig;
   final static IPreferenceStore   _prefStore = CommonActivator.getPrefStore();
   private static String           _accessToken;

   static {
      _accessToken = _prefStore.getString(ICommonPreferences.DROPBOX_ACCESSTOKEN);

      //Getting the current version of MyTourbook
      final Version version = FrameworkUtil.getBundle(DropboxClient.class).getVersion();

      _requestConfig = DbxRequestConfig.newBuilder("mytourbook/" + version.toString().replace(".qualifier", UI.EMPTY_STRING)).build(); //$NON-NLS-1$ //$NON-NLS-2$

      _dropboxClient = new DbxClientV2(_requestConfig, _accessToken);
   }

   /**
    * Downloads a remote Dropbox file to a local temporary location
    *
    * @param dropboxFilefilePath
    *           The Dropbox path of the file
    * @return The local path of the downloaded file
    */
   public static Path CopyLocally(final String dropboxFilefilePath) {

      //Retrieve the download link for the file
      final String fileLink = GetFileLink(dropboxFilefilePath);
      if (StringUtils.isNullOrEmpty(fileLink)) {
         return null;
      }

      final String fileName = Paths.get(dropboxFilefilePath).getFileName().toString();
      final Path filePath = Paths.get(FileUtils.getTempDirectoryPath(), fileName);

      //Downloading the file from Dropbox to the local disk
      try (InputStream inputStream = URI.create(fileLink).toURL().openStream()) {

         final long writtenBytes = Files.copy(inputStream, filePath);

         if (writtenBytes > 0) {
            return filePath;
         }
      } catch (final IOException e) {
         StatusUtil.log(e);
      }

      return null;
   }

   public static DbxClientV2 getDefault() {
      return _dropboxClient;
   }

   /**
    * Retrieves, for a given file, the link to download it
    *
    * @param dropboxFilePath
    *           The Dropbox path of the file
    * @return The Dropbox link of the file
    */
   private static String GetFileLink(final String dropboxFilePath) {

      String fileLink = null;

      try {
         //TODO FB Use the files.download() function instead... to reduce code ?
         fileLink = _dropboxClient.files().getTemporaryLink(dropboxFilePath).getLink();
      } catch (final DbxException e) {
         StatusUtil.log(e);
      }

      return fileLink;
   }
}
