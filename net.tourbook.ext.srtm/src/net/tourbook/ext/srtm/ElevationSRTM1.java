/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.ext.srtm;

import java.io.File;
import java.util.*;

public class ElevationSRTM1 extends ElevationBase {
   
   ;
   static private SRTM1I fSRTMi;
   static private HashMap<Integer,SRTM1I> hm;

   public ElevationSRTM1() {
      // create map with used Files
      // to find file, calculate and remember key value
      hm = new HashMap<Integer,SRTM1I>(); // default initial 16 Files
      gridLat.setDegreesMinutesSecondsDirection(0, 0, 1, 'N');
      gridLon.setDegreesMinutesSecondsDirection(0, 0, 1, 'E');
   }

   public short getElevation(GeoLat lat, GeoLon lon) {

      if (lat.getTertias() != 0) return getElevationGrid(lat, lon);
      if (lon.getTertias() != 0) return getElevationGrid(lat, lon);
      
      int i = lon.getDegrees();
      if (lon.isWest())
         i += 256;
      i *= 1024;
      i += lat.getDegrees();
      if (lat.isSouth())
         i += 256;
      Integer ii = new Integer(i);
      fSRTMi = (SRTM1I)hm.get(ii);

      if (fSRTMi == null) {
         // first time only
         fSRTMi = new SRTM1I(lat, lon);
         hm.put(ii, fSRTMi);
      }

      return fSRTMi.getElevation(lat, lon);

   }

   public double getElevationDouble(GeoLat lat, GeoLon lon) {

      if (lat.getDecimal() == 0 && lon.getDecimal() == 0) return 0.;
      if (lat.getTertias() != 0) return getElevationGridDouble(lat, lon);
      if (lon.getTertias() != 0) return getElevationGridDouble(lat, lon);
      return (double) getElevation(lat, lon);
   }
   
   public short getSecDiff() {
	   // number of degrees seconds between two data points
	   return 1;
   }
   
   public String getName() {
   	return "SRTM1"; //$NON-NLS-1$
   }

   private class SRTM1I {

      ElevationFile elevationFile;

      private SRTM1I(GeoLat lat, GeoLon lon) {

			final String srtm1DataPath = getElevationDataPath("srtm1"); //$NON-NLS-1$
			final String srtm1Suffix = ".hgt"; //$NON-NLS-1$

			String fileName = new String(srtm1DataPath
					+ File.separator
					+ lat.getDirection()
					+ NumberForm.n2(lat.isNorth() ? lat.getDegrees() : lat.getDegrees() + 1)
					+ lon.getDirection()
					+ NumberForm.n3(lon.isEast() ? lon.getDegrees() : lon.getDegrees() + 1)
					+ srtm1Suffix);

         try {
            elevationFile = new ElevationFile(fileName,  Constants.ELEVATION_TYPE_SRTM1);
         } catch (Exception e) {
            System.out.println("SRTM1I: Error: " + e.getMessage()); // NOT File not found //$NON-NLS-1$
            // dont return exception
         }
       }
      
      public short getElevation(GeoLat lat, GeoLon lon) {
      	return elevationFile.get(offset(lat, lon));
      }

      //    Offset in the SRTM1-File
      public int offset(GeoLat lat, GeoLon lon) {

      	if (lat.isSouth()) {
      		if (lon.isEast())
      			return 3601 * (lat.getMinutes() * 60 + lat.getSeconds())
            		+ lon.getMinutes() * 60 + lon.getSeconds();
      		else
      			return 3601 * (lat.getMinutes() * 60 + lat.getSeconds())
					+ 3599 - lon.getMinutes() * 60 - lon.getSeconds();
      	}
      	else {
      		if (lon.isEast())
      			return 3601 * (3599 - lat.getMinutes() * 60 - lat.getSeconds())
            		+ lon.getMinutes() * 60 + lon.getSeconds();
      		else
      			return 3601 * (3599 - lat.getMinutes() * 60 - lat.getSeconds())
					+ 3599 - lon.getMinutes() * 60 - lon.getSeconds();
      	}
      }

   }

   public static void main(String[] args) {
   }
}
