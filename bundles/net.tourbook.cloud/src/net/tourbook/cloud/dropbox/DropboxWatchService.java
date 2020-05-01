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

import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderGetLatestCursorResult;
import com.dropbox.core.v2.files.ListFolderLongpollResult;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.IOException;
import java.lang.Thread.State;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DropboxWatchService implements WatchService {

   private String  _folderToWatch;
   private AtomicBoolean _hasChanges;
   private Thread        folderPoll;

   public DropboxWatchService(final String folderToWatch) {
      _folderToWatch = folderToWatch;
   }

   /**
    * Returns latest cursor for listing changes to a directory in
    * Dropbox with the given path.
    *
    * @param dbxClient
    *           Dropbox client to use for fetching the latest cursor
    * @param path
    *           path to directory in Dropbox
    * @return cursor for listing changes to the given Dropbox directory
    */
   private static String getLatestCursor(final String path)
         throws DbxApiException, DbxException {

      final ListFolderGetLatestCursorResult result = DropboxClient.getDefault()
            .files()
            .listFolderGetLatestCursorBuilder(path)
            .withIncludeDeleted(true)
            .withIncludeMediaInfo(false)
            .withRecursive(true)
            .start();

      return result.getCursor();
   }

   @Override
   public void close() throws IOException {
      folderPoll.interrupt();
final boolean toto = folderPoll.isInterrupted();
final State sate = folderPoll.getState();
      folderPoll = null;
final String tata = "dd";
      //    https://www.tutorialspoint.com/java/java_multithreading.htm
      //TODO stop the thread (if it was started) that is running in take()
   }

   /**
    * Prints changes made to a folder in Dropbox since the given
    * cursor was retrieved.
    *
    * @param dbxClient
    *           Dropbox client to use for fetching folder changes
    * @param cursor
    *           lastest cursor received since last set of changes
    * @return latest cursor after changes
    */
   private String examineChanges(String cursor)
         throws DbxApiException, DbxException {

      while (true) {
         final ListFolderResult result = DropboxClient.getDefault()
               .files()
               .listFolderContinue(cursor);
         for (final Metadata metadata : result.getEntries()) {
            String type;
            String details;

            if (metadata instanceof FileMetadata) {
               final FileMetadata fileMetadata = (FileMetadata) metadata;
               type = "file";
               details = "(rev=" + fileMetadata.getRev() + ")";
               _hasChanges.getAndSet(true);
            } else if (metadata instanceof DeletedMetadata) {
               type = "deleted";
               details = "";
               _hasChanges.getAndSet(true);
            } else {
               throw new IllegalStateException("Unrecognized metadata type: " + metadata.getClass());
            }

            System.out.printf("\t%10s %24s \"%s\"\n", type, details, metadata.getPathLower());
         }
         // update cursor to fetch remaining results
         cursor = result.getCursor();

         if (!result.getHasMore()) {
            break;
         }
      }

      return cursor;
   }

   @Override
   public WatchKey poll() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public WatchKey poll(final long timeout, final TimeUnit unit) throws InterruptedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public WatchKey take() throws InterruptedException {

      //Start the thread https://stackoverflow.com/questions/4182491/how-to-stop-a-thread-waiting-in-a-blocking-read-operation-in-java
      _hasChanges = new AtomicBoolean(false);

      folderPoll = new Thread(new Runnable() {
         @Override
         public void run() {
            while (!Thread.currentThread().isInterrupted()) {
               try {

                  final String cursor = getLatestCursor(_folderToWatch);

                  while (_hasChanges.get() == false) {
                     final ListFolderLongpollResult listFolderLongpollResult = DropboxClient.getDefault().files().listFolderLongpoll(cursor);
                     if (listFolderLongpollResult.getChanges()) {
                        examineChanges(cursor);
                     }

                     // we were asked to back off from our polling, wait the requested amount of seconds
                     // before issuing another longpoll request.
                     final Long backoff = listFolderLongpollResult.getBackoff();
                     if (backoff != null) {
                        try {
                           System.out.printf("backing off for %d secs...\n", backoff.longValue());
                           Thread.sleep(TimeUnit.SECONDS.toMillis(backoff));
                        } catch (final InterruptedException ex) {
                           System.exit(0);
                        }
                     }
                  }
               } catch (final DbxException ex) {
                  // if a user message is available, try using that instead
                  System.err.println("ListFolderLongpollErrorException: " + ex);
               }
            }
         }
      });

      folderPoll.start();
      folderPoll.join();

      //We return an empty Watchkey. The goal here is that we only
      //want to notify that some changes happened
      final WatchKey dropboxWatchKey = new WatchKey() {

         @Override
         public void cancel() {
            // TODO Auto-generated method stub

         }

         @Override
         public boolean isValid() {
            // TODO Auto-generated method stub
            return false;
         }

         @Override
         public List<WatchEvent<?>> pollEvents() {
            // TODO Auto-generated method stub
            return null;
         }

         @Override
         public boolean reset() {
            // TODO Auto-generated method stub
            return true;
         }

         @Override
         public Watchable watchable() {
            // TODO Auto-generated method stub
            return null;
         }
      };

      return dropboxWatchKey;
   }
}
