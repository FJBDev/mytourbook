/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.cloud.suunto;

import java.io.File;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.dropbox.PrefPageDropbox;
import net.tourbook.common.TourbookFileSystem;
import net.tourbook.common.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Shell;

public class SuuntoFileSystem extends TourbookFileSystem {

   private IPreferenceStore         _prefStore = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener  _prefChangeListenerCommon;

   public SuuntoFileSystem() {

      super("Suunto"); //$NON-NLS-1$

//      createDropboxFileSystem();
//
//      _prefChangeListenerCommon = event -> {
//
//         if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {
//
//            closeDropboxFileSystem();
//
//            // Re create the Dropbox file system
//            createDropboxFileSystem();
//         }
//      };

      // register the listener
//      _prefStore.addPropertyChangeListener(_prefChangeListenerCommon);
   }

   @Override
   protected void close() {

//      _prefStore.removePropertyChangeListener(_prefChangeListenerCommon);
      closeDropboxFileSystem();
   }

   public void closeDropboxFileSystem() {

   }

   @Override
   protected File copyFileLocally(final String dropboxFilePath) {
      //TODO
      return null;
   }

   /**
    * Retrieves the Dropbox {@link FileStore}.
    * Creates it if necessary.
    *
    * @return A list of Dropbox {@link FileStore}
    */
   @Override
   public Iterable<FileStore> getFileStore() {

//      if (_dropboxFileSystem != null || createDropboxFileSystem()) {
//         return _dropboxFileSystem.getFileStores();
//      }

      return Paths.get(URI.create("file:///home/frederic/Downloads")).getFileSystem().getFileStores();
   }

   /**
    * Retrieves the Dropbox {@link FileSystem}.
    *
    * @return
    */
   @Override
   protected FileSystem getFileSystem() {
      return Paths.get(URI.create("file:///home/frederic/Downloads")).getFileSystem();
   }

   @Override
   public ImageDescriptor getFileSystemImageDescriptor() {
      return Activator.getImageDescriptor("Messages.Image__Dropbox_Logo");
   }

   /**
    * Get the Dropbox {@link Path} of a given filename
    *
    * @param fileName
    * @return
    */
   @Override
   protected Path getfolderPath(final String folderName) {
//      if (_dropboxFileSystem == null) {
//         return null;
//      }
//
//      //We remove the "Dropbox" string from the folderName
//      final String dropboxFilePath = folderName.substring(getId().length());
//      return _dropboxFileSystem.getPath(dropboxFilePath);
      return Paths.get(URI.create("file:///home/frederic/Downloads"));
   }

   @Override
   public String getPreferencePageId() {
      return PrefPageDropbox.ID;
   }

   /**
    * When the user clicks on the "Choose Folder" button, a dialog is opened
    * so that the user can choose which folder will be used when using their Dropbox
    * account as a device to watch.
    */
   @Override
   public String selectFileSystemFolder(final Shell shell, final String workingDirectory) {

      return UI.EMPTY_STRING;
   }
}
