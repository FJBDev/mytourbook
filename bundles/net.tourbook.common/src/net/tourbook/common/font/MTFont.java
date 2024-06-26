/*******************************************************************************
 * Copyright (C) 2017, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common.font;

import java.util.Arrays;

import net.tourbook.common.UI;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class MTFont {

   public static final String DEFAULT_MONO_FONT = "Courier"; //$NON-NLS-1$

   private static Font        _bannerFont;
   private static Font        _headerFont;
   private static Font        _titleFont;

   private static int         _bannerFontHeight;
   private static int         _headerFontHeight;
   private static int         _titleFontHeight;

   static {

      setupFonts();
   }

   private static void dispose() {

      UI.disposeResource(_bannerFont);
      UI.disposeResource(_headerFont);
      UI.disposeResource(_titleFont);
   }

   /**
    * <pre>
    *
    * Windows
    * =======
    *
    * [1|Segoe UI|9.0|0|WINDOWS|1|-12|0|0|0|400|0|0|0|1|0|0|0|0|Segoe UI]
    * [1|Segoe UI|9.0|0|WINDOWS|1|-12|0|0|0|400|0|0|0|1|0|0|0|0|Segoe UI]
    *
    * [1|Courier New|9.75|0|WINDOWS|1|-13|0|0|0|0|0|0|0|1|0|0|0|0|Courier New]
    *
    * [1|MS Sans Serif|9.75|1|WINDOWS|1|-13|0|0|0|700|0|0|0|1|0|0|0|0|MS Sans Serif]
    * [1|MS Sans Serif|12.0|1|WINDOWS|1|-16|0|0|0|700|0|0|0|1|0|0|0|0|MS Sans Serif]
    *
    * Ubuntu
    * ======
    *
    * [1|Ubuntu|11.0|0|GTK|1|]
    * [1|Ubuntu|11.0|0|GTK|1|]
    *
    * [1|Monospace|10.0|0|GTK|1|]
    *
    * [1|Sans|10.0|1|GTK|1|]
    * [1|Sans|12.0|1|GTK|1|]
    * </pre>
    *
    * <b>Banner Font</b><br>
    * Used in PDE editors, welcome pages and in the title area of many wizards. For instance the
    * New Project wizard uses this font for the top title.
    * <p>
    * <b>Dialog Font</b><br>
    * Used for widgets in dialogs.
    * <p>
    * <b>Header Font</b><br>
    * Used as a section heading. For instance the Welcome page for the Eclipse Platform uses this
    * font for the top title.
    * <p>
    * <b>Text Font</b><br>
    * Used in text editors.
    * <p>
    * Replaced code
    * <p>
    * label.setFont(JFaceResources.getBannerFont()); <br>
    * _boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
    */
   public static void dumpFonts() {

      dumpFonts_10(JFaceResources.getDefaultFont());

      dumpFonts_10(JFaceResources.getDialogFont());
      dumpFonts_10(JFaceResources.getTextFont());

      dumpFonts_10(JFaceResources.getBannerFont());
      dumpFonts_10(JFaceResources.getHeaderFont());
   }

   private static void dumpFonts_10(final Font font) {

      final FontData[] fontData = font.getFontData();

      System.out.println((UI.timeStampNano() + " [" + "] ") + ("\t" + Arrays.toString(fontData))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }

   public static Font getBannerFont() {
      return _bannerFont;
   }

   public static int getBannerFontHeight() {
      return _bannerFontHeight;
   }

   public static Font getHeaderFont() {
      return _headerFont;
   }

   public static int getHeaderFontHeight() {
      return _headerFontHeight;
   }

   public static Font getTitleFont() {
      return _titleFont;
   }

   public static int getTitleFontHeight() {
      return _titleFontHeight;
   }

   /**
    * The banner font height is 1.3 larger than the default font
    *
    * @param control
    */
   public static void setBannerFont(final Control control) {

      control.setFont(_bannerFont);
   }

   /**
    * The header font height is 1.7 larger than the default font
    *
    * @param control
    */
   public static void setHeaderFont(final Control control) {

      control.setFont(_headerFont);
   }

   private static void setupFonts() {

      final Display display = Display.getCurrent();
      Assert.isNotNull(display);

      // hook dispose
      display.disposeExec(new Runnable() {
         @Override
         public void run() {
            dispose();
         }
      });

// SET_FORMATTING_OFF

      final FontData[] allDefaultFontData    = JFaceResources.getDefaultFontDescriptor().getFontData();
      final FontData[] allTitleFontData      = JFaceResources.getDefaultFontDescriptor().getFontData();
      final FontData[] allBannerFontData     = JFaceResources.getBannerFont().getFontData();
      final FontData[] allHeaderFontData     = JFaceResources.getHeaderFontDescriptor().getFontData();

      final FontData defaultFontData   = allDefaultFontData[0];
      final FontData titleFontData     = allTitleFontData[0];
      final FontData bannerFontData    = allBannerFontData[0];
      final FontData headerFontData    = allHeaderFontData[0];

// SET_FORMATTING_ON

      /*
       * Use the same font name, win10 is using MS Sans Serif for banner/header font which looks
       * very ugly
       */
      final String defaultName = defaultFontData.getName();
      final int defaultHeight = defaultFontData.getHeight();

      _titleFontHeight = (int) (defaultHeight * 1.2);
      _bannerFontHeight = (int) (defaultHeight * 1.3);
      _headerFontHeight = (int) (defaultHeight * 1.7);

      bannerFontData.setName(defaultName);
      bannerFontData.setHeight(_bannerFontHeight);

      headerFontData.setName(defaultName);
      headerFontData.setHeight(_headerFontHeight);

      titleFontData.setName(defaultName);
      titleFontData.setHeight(_titleFontHeight);

      _titleFont = new Font(display, titleFontData);
      _bannerFont = new Font(display, bannerFontData);
      _headerFont = new Font(display, headerFontData);
   }

}
