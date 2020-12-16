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
package net.tourbook.common;

import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.tourbook.common.util.StringUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CloudUploaderManager {

   private static List<TourbookCloudUploader> _cloudUploadersList;

//   /**
//    * Returns the {@link FileSystem}, if found, for a given device folder.
//    *
//    * @param deviceFolder
//    * @return
//    */
//   public static FileSystem getCloudUploader(final String deviceFolder) {
//
//      final TourbookCloudUploader tourbookFileSystem = getTourbookFileSystem(deviceFolder);
//
//      if (tourbookFileSystem != null) {
//         return tourbookFileSystem.getFileSystem();
//      }
//
//      return null;
//   }

   /**
    * Collects all the identifiers of the available file systems
    *
    * @return Returns a list of {@link String}
    */
   public static List<String> getCloudUploaders() {

      final ArrayList<String> cloudUploadersList = new ArrayList<>();

      getFileSystemsList();

      _cloudUploadersList.forEach(cu -> cloudUploadersList.add(cu.getName()));

      return cloudUploadersList;
   }

   /**
    * Returns the id, if the {@link TourbookFileSystem} was found, for a given device folder name
    *
    * @param deviceFolderName
    * @return
    */
//   public static String getFileSystemId(final String deviceFolderName) {
//
//      final TourbookCloudUploader tourbookFileSystem = getTourbookFileSystem(deviceFolderName);
//
//      if (tourbookFileSystem != null) {
//         return tourbookFileSystem.getId();
//      }
//
//      return UI.EMPTY_STRING;
//   }

   /**
    * Read file stores that can be used as file systems.
    *
    * @return Returns a list of {@link FileSystem}
    */
   public static List<TourbookCloudUploader> getFileSystemsList() {

      if (_cloudUploadersList == null) {
         _cloudUploadersList = readCloudUploaderExtensions("fileSystem"); //$NON-NLS-1$
      }

      return _cloudUploadersList;
   }

//   public static Path getfolderPath(final String folderName) {
//
//      final TourbookFileSystem tourbookFileSystem = getTourbookFileSystem(folderName);
//
//      if (tourbookFileSystem != null) {
//         return tourbookFileSystem.getfolderPath(folderName);
//      }
//
//      return null;
//   }

   /**
    * Returns the {@link TourbookFileSystem}, if found, for a given device folder.
    *
    * @param deviceFolder
    * @return
    */
   public static TourbookCloudUploader getTourbookFileSystem(final String cloudUploaderId) {

      if (StringUtils.isNullOrEmpty(cloudUploaderId)) {
         return null;
      }

      getFileSystemsList();

      final Optional<TourbookCloudUploader> cloudUploaderSearchResult = _cloudUploadersList.stream()
            .filter(cu -> cloudUploaderId.toLowerCase().startsWith(cu.getName().toLowerCase()) ||
                  cloudUploaderId.toLowerCase().startsWith(cu.getName().toLowerCase()))
            .findAny();

      if (cloudUploaderSearchResult.isPresent()) {
         return cloudUploaderSearchResult.get();
      }

      return null;
   }

   /**
    * Read and collects all the extensions that implement {@link TourbookFileSystem}.
    *
    * @param extensionPointName
    *           The extension point name
    * @return The list of {@link TourbookFileSystem}.
    */
   private static List<TourbookCloudUploader> readCloudUploaderExtensions(final String extensionPointName) {

      final List<TourbookCloudUploader> cloudUploadersList = new ArrayList<>();

      final IExtensionPoint extPoint = Platform
            .getExtensionRegistry()
            .getExtensionPoint("net.tourbook", extensionPointName); //$NON-NLS-1$

      if (extPoint != null) {

         for (final IExtension extension : extPoint.getExtensions()) {

            for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

               if (configElement.getName().equalsIgnoreCase("cloudUploader")) { //$NON-NLS-1$

                  Object object;
                  try {

                     object = configElement.createExecutableExtension("class"); //$NON-NLS-1$

                     if (object instanceof TourbookCloudUploader) {
                        final TourbookCloudUploader cloudUploader = (TourbookCloudUploader) object;
                        cloudUploadersList.add(cloudUploader);
                     }

                  } catch (final CoreException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      return cloudUploadersList;
   }
}
