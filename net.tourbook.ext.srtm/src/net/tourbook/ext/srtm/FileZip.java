package net.tourbook.ext.srtm;

import java.io.*;
import java.util.zip.*;

public final class FileZip {

   public final static String unzip(String zipName) throws Exception {
      
      String outFileName = null;
      String zipEntryName = null;
      
      try {
         // Open the ZIP file
         ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipName));
         // Get the first entry
         ZipEntry zipEntry = zipInputStream.getNextEntry();
         zipEntryName = zipEntry.getName();
         System.out.println("zipEntryName " + zipEntryName);
                  
         if (zipEntryName.indexOf(File.separator) != -1) 
            // Delimiter im Namen (z.B. bei selbsterzeugten kmz-Files)
            zipEntryName = zipEntryName.substring(zipEntryName.lastIndexOf(File.separator)+1);
         
         outFileName = zipName.substring(0, zipName.lastIndexOf(File.separator))
                     + File.separator + zipEntryName;
         
         System.out.println("outFileName " + outFileName);
         
         OutputStream fileOutputStream = new FileOutputStream(outFileName);

         // Transfer bytes from the ZIP file to the output file
         byte[] buf = new byte[1024];
         int len;
         while ((len = zipInputStream.read(buf)) > 0) {
            fileOutputStream.write(buf, 0, len);
         }

         fileOutputStream.close();
         zipInputStream.close();
         
         return zipEntryName;
         
      } catch (IOException e) {
         System.out.println("unzip: Fehler: " + e.getMessage());
         throw(e); // Exception weitergeben
      }
   }


   public final static String gunzip(String gzipName) throws Exception {
      
      String outFileName = null;
      String gzipEntryName = null;
      try {
         // Open the GZIP file
         GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(gzipName));
         
         gzipEntryName = gzipName;
         if (gzipEntryName.indexOf(File.separator) != -1) // Delimiter im Namen
            gzipEntryName = gzipEntryName.substring(gzipEntryName.lastIndexOf(File.separator)+1);
         if (gzipEntryName.indexOf('.') != -1)
            gzipEntryName = gzipEntryName.substring(0, gzipEntryName.lastIndexOf('.'));

         outFileName = gzipName.substring(0, gzipName.lastIndexOf(File.separator))
                     + File.separator + gzipEntryName;
         
         System.out.println("outFileName " + outFileName);
         
         OutputStream fileOutputStream = new FileOutputStream(outFileName);

         // Transfer bytes from the GZIP file to the output file
         byte[] buf = new byte[1024];
         int len;
         while ((len = gzipInputStream.read(buf)) > 0) {
            fileOutputStream.write(buf, 0, len);
         }

         fileOutputStream.close();
         gzipInputStream.close();
         
         return gzipEntryName;
         
      } catch (IOException e) {
         System.out.println("gunzip: Fehler: " + e.getMessage());
         throw(e); // Exception weitergeben
      }
   }

   
   
   
   public final static void zip(String fileName, String zipName) throws Exception {
      try {
         // Compress the file         
         File file = new File(fileName);
         FileInputStream fileInputStream = new FileInputStream(file);
                  
         // Create the ZIP file
         ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipName));

         // Add ZIP entry to output stream (Filename only)
         if (fileName.indexOf(File.separator) != -1) 
            fileName = fileName.substring(fileName.lastIndexOf(File.separator)+1);
         zipOutputStream.putNextEntry(new ZipEntry(fileName));

         // Create a buffer for reading the files
         byte[] buf = new byte[1024];
         
         // Transfer bytes from the file to the ZIP file
         int len;
         while ((len = fileInputStream.read(buf)) > 0) {
             zipOutputStream.write(buf, 0, len);
         }
 
         // Complete the entry
         zipOutputStream.closeEntry();
         fileInputStream.close();
         // Complete the ZIP file
         zipOutputStream.close();
         
      } catch (IOException e) {
         System.out.println("zip: Fehler: " + e.getMessage());
         throw(e); // Exception weitergeben
      }
   }
   

   public static void main(String[] args) throws Exception {
   }
}
