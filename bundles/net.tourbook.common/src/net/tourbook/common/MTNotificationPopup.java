/*******************************************************************************
 * Copyright (C) 2023, 2024 Frédéric Bard
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

import net.tourbook.common.util.ImageUtils;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

class MTNotificationPopup extends AbstractNotificationPopup {

   private Image  _image;
   private String _text;
   private String _title;

   MTNotificationPopup(final Display display,
                       final ImageDescriptor imageDescriptor,
                       final String title,
                       final String text) {
      super(display);
      _image = ImageUtils.resize(display, imageDescriptor.createImage(), 16, 16);
      _title = title;
      _text = text;
   }

   @Override
   public boolean close() {

      UI.disposeResource(_image);
      return super.close();
   }

   @Override
   protected void createContentArea(final Composite parent) {

      final PixelConverter pixelConverter = new PixelConverter(parent);

      final Composite notificationComposite = new Composite(parent, SWT.NO_FOCUS);
      GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(3)).align(SWT.FILL, SWT.FILL)
            .applyTo(notificationComposite);
      notificationComposite.setLayout(new GridLayout(1, false));
      notificationComposite.setBackground(parent.getBackground());

      if (StringUtils.isNullOrEmpty(_text)) {
         return;
      }

      final Text descriptionLabel = new Text(notificationComposite, SWT.READ_ONLY | SWT.WRAP);
      descriptionLabel.setText(_text);
      descriptionLabel.setBackground(parent.getBackground());
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .align(SWT.FILL, SWT.FILL)
            .applyTo(descriptionLabel);
   }

   @Override
   protected Image getPopupShellImage(final int maximumHeight) {
      return _image;
   }

   @Override
   protected String getPopupShellTitle() {
      return _title;
   }

}
