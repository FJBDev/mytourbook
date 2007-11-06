/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;

public class UI {

	private static final String				TOUR_TYPE_PREFIX				= "tourType";					//$NON-NLS-1$

	private static UI						instance;

	private final HashMap<String, Image>	fImageCache						= new HashMap<String, Image>();

	// Create image registry
	public final static ImageRegistry		IMAGE_REGISTRY					= TourbookPlugin.getDefault()
																					.getImageRegistry();

	public static final String				IMAGE_TOUR_TYPE_FILTER			= "tourType-filter";			//$NON-NLS-1$
	public static final String				IMAGE_TOUR_TYPE_FILTER_SYSTEM	= "tourType-filter-system";	//$NON-NLS-1$

	private static final int				TOUR_TYPE_IMAGE_WIDTH			= 16;
	private static final int				TOUR_TYPE_IMAGE_HEIGHT			= 16;

	static {
		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER,
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter));
		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER_SYSTEM,
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter_system));
	}

	private UI() {}

	public static ColumnPixelData getColumnPixelWidth(	final PixelConverter pixelConverter,
														final int width) {
		return new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(width), false);
	}

	public static UI getInstance() {

		if (instance == null) {
			instance = new UI();
		}

		return instance;
	}

	/**
	 * Restore the sash weight from a memento
	 * 
	 * @param sash
	 * @param fMemento
	 * @param weightKey
	 * @param sashDefaultWeight
	 */
	public static void restoreSashWeight(	final SashForm sash,
											final IMemento fMemento,
											final String weightKey,
											final int[] sashDefaultWeight) {

		final int[] sashWeights = sash.getWeights();
		final int[] newWeights = new int[sashWeights.length];

		for (int weightIndex = 0; weightIndex < sashWeights.length; weightIndex++) {

			final Integer mementoWeight = fMemento.getInteger(weightKey
					+ Integer.toString(weightIndex));

			if (mementoWeight == null) {
				try {
					newWeights[weightIndex] = sashDefaultWeight[weightIndex];

				} catch (final ArrayIndexOutOfBoundsException e) {
					newWeights[weightIndex] = 100;
				}
			} else {
				newWeights[weightIndex] = mementoWeight;
			}
		}

		sash.setWeights(newWeights);
	}

	/**
	 * Change the title for the application
	 * 
	 * @param newTitle
	 *        new title for the application or <code>null</code> to set the original title
	 */
	public static void changeAppTitle(final String newTitle) {

		final Display display = Display.getDefault();

		if (display != null) {

			// Look at all the shells and pick the first one that is a workbench window.
			final Shell shells[] = display.getShells();
			for (int shellIdx = 0; shellIdx < shells.length; shellIdx++) {

				final Object data = shells[shellIdx].getData();

				// Check whether this shell points to the Application main window's shell:
				if (data instanceof IWorkbenchWindow) {

					String title;
					if (newTitle == null) {
						title = Messages.App_Title;
					} else {
						title = newTitle;
					}

					shells[shellIdx].setText(title);
					break;
				}
			}
		}
	}

	/**
	 * Store the weights for the sash in a memento
	 * 
	 * @param sash
	 * @param memento
	 * @param weightKey
	 */
	public static void saveSashWeight(	final SashForm sash,
										final IMemento memento,
										final String weightKey) {

		final int[] weights = sash.getWeights();

		for (int weightIndex = 0; weightIndex < weights.length; weightIndex++) {
			memento.putInteger(weightKey + Integer.toString(weightIndex), weights[weightIndex]);
		}
	}

	/**
	 * Set grid layout with no margins for a composite
	 * 
	 * @param composite
	 */
	public static void set0GridLayout(final Composite composite) {
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
	}

	public static void setDefaultColor(final Control control) {
		control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		control.setBackground(null);
	}

	public static void setErrorColor(final Text control) {
		control.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
	}

	public static GridData setFieldWidth(	final Composite parent,
											final StringFieldEditor field,
											final int width) {
		final GridData gd = new GridData();
		gd.widthHint = width;
		field.getTextControl(parent).setLayoutData(gd);
		return gd;
	}

	public static void setHorizontalSpacer(final Composite parent, final int columns) {
		final Label label = new Label(parent, SWT.NONE);
		final GridData gd = new GridData();
		gd.horizontalSpan = columns;
		label.setLayoutData(gd);
	}

	public static GridData setWidth(final Control control, final int width) {
		final GridData gd = new GridData();
		gd.widthHint = width;
		control.setLayoutData(gd);
		return gd;
	}

	public static VerifyListener verifyListenerTypeLong() {

		return new VerifyListener() {
			public void verifyText(final VerifyEvent e) {
				if (e.text.equals("")) { //$NON-NLS-1$
					return;
				}
				try {
					Long.parseLong(e.text);
				} catch (final NumberFormatException e1) {
					e.doit = false;
				}
			}
		};
	}

	/**
	 * dispose resources
	 */
	public void dispose() {
		disposeImages();
	}

	private void disposeImages() {

		for (final Iterator<Image> iterator = fImageCache.values().iterator(); iterator.hasNext();) {
			iterator.next().dispose();
		}
		fImageCache.clear();
	}

	/**
	 * dispose all tour type images
	 */
	public void disposeTourTypeImages() {

		for (final Iterator<String> iterator = fImageCache.keySet().iterator(); iterator.hasNext();) {

			final String imageId = iterator.next();

			if (imageId.startsWith(TOUR_TYPE_PREFIX)) {
				fImageCache.get(imageId).dispose();
				iterator.remove();
			}
		}
	}

	/**
	 * @param display
	 * @param graphColor
	 * @return return the color for the graph
	 */
	private DrawingColors getTourTypeColors(final Display display, final long tourTypeId) {

		final DrawingColors drawingColors = new DrawingColors();
		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();

		TourType colorTourType = null;

		for (final TourType tourType : tourTypes) {
			if (tourType.getTypeId() == tourTypeId) {
				colorTourType = tourType;
			}
		}

		if (colorTourType == null || colorTourType.getTypeId() == TourType.TOUR_TYPE_ID_NOT_DEFINED) {

			// tour type was not found use default color

			drawingColors.colorBright = display.getSystemColor(SWT.COLOR_WHITE);
			drawingColors.colorDark = display.getSystemColor(SWT.COLOR_WHITE);
			drawingColors.colorLine = display.getSystemColor(SWT.COLOR_DARK_GRAY);

			// prevent disposing the colors
			drawingColors.mustBeDisposed = false;

		} else {

			drawingColors.colorBright = new Color(display, colorTourType.getRGBBright());
			drawingColors.colorDark = new Color(display, colorTourType.getRGBDark());
			drawingColors.colorLine = new Color(display, colorTourType.getRGBLine());
		}

		return drawingColors;
	}

	public Image getTourTypeImage(final long typeId) {

		if (false) {
			return getTourTypeImageOLD(typeId);
		} else {
			return getTourTypeImageNEW(typeId);
		}
	}

	/**
	 * @param typeId
	 * @return Returns an image which represents the tour type
	 */
	public Image getTourTypeImageOLD(final long typeId) {

		final String colorId = TOUR_TYPE_PREFIX + typeId;
		Image image = fImageCache.get(colorId);

		if (image == null || image.isDisposed()) {

			/*
			 * create image for the tour type
			 */

			final Display display = Display.getCurrent();

			final int imageWidth = 16;
			final int imageHeight = 16;

			final DrawingColors drawingColors = getTourTypeColors(display, typeId);

			/*
			 * Use magenta as transparency color since it is used infrequently.
			 */
			Color colorTransparent = display.getSystemColor(SWT.COLOR_MAGENTA);
			Color colorBright = drawingColors.colorBright;
			Color colorDark = drawingColors.colorDark;
			Color colorLine = drawingColors.colorLine;

			PaletteData palette = new PaletteData(new RGB[] {
					colorTransparent.getRGB(),
					colorDark.getRGB(),
					colorBright.getRGB(),
					colorLine.getRGB() });

			ImageData data = new ImageData(imageWidth, imageHeight, 8, palette);
			data.transparentPixel = 0;

			image = new Image(display, data);
			image.setBackground(colorTransparent);

			final GC gc = new GC(image);
			{

				gc.setForeground(colorBright);
				gc.setBackground(colorDark);

				gc.fillGradientRectangle(4, 4, imageWidth - 8, imageHeight - 8, false);

				gc.setForeground(colorLine);
				gc.drawRectangle(3, 3, imageWidth - 7, imageHeight - 7);

			}

			drawingColors.dispose();
			gc.dispose();

			fImageCache.put(colorId, image);
		}

		return image;
	}

	/**
	 * @param typeId
	 * @return Returns an image which represents the tour type
	 */
	public Image getTourTypeImageNEW(final long typeId) {

		final String colorId = TOUR_TYPE_PREFIX + typeId;
		Image tourTypeImage = fImageCache.get(colorId);

		if (tourTypeImage != null && tourTypeImage.isDisposed() == false) {

			return tourTypeImage;

		} else {

			// create image for the tour type

			final Display display = Display.getCurrent();

			/*
			 * create image
			 */
			tourTypeImage = new Image(display, TOUR_TYPE_IMAGE_WIDTH, TOUR_TYPE_IMAGE_HEIGHT);
			final Image maskImage = new Image(display,
					TOUR_TYPE_IMAGE_WIDTH,
					TOUR_TYPE_IMAGE_HEIGHT);

			final GC gcImage = new GC(tourTypeImage);
			{
				drawImage(typeId, gcImage);
			}
			gcImage.dispose();

			/*
			 * set transparency
			 */
			final ImageData imageData = tourTypeImage.getImageData();
			final int transparentPixel = imageData.getPixel(0, 0);
			imageData.transparentPixel = transparentPixel;
			final Image transparentImage = new Image(display, imageData);

			tourTypeImage.dispose();
			maskImage.dispose();

			// keep image in cache
			fImageCache.put(colorId, transparentImage);

			return transparentImage;
		}
	}

	private void drawImage(final long typeId, final GC gcImage) {

		final Display display = Display.getCurrent();
		final DrawingColors drawingColors = getTourTypeColors(display, typeId);

		final Color colorBright = drawingColors.colorBright;
		final Color colorDark = drawingColors.colorDark;
		final Color colorLine = drawingColors.colorLine;

		gcImage.setForeground(colorBright);
		gcImage.setBackground(colorDark);
		gcImage.fillGradientRectangle(4,
				4,
				TOUR_TYPE_IMAGE_WIDTH - 8,
				TOUR_TYPE_IMAGE_HEIGHT - 8,
				false);

		gcImage.setForeground(colorLine);
		gcImage.drawRectangle(3, 3, TOUR_TYPE_IMAGE_WIDTH - 7, TOUR_TYPE_IMAGE_HEIGHT - 7);

		drawingColors.dispose();
	}

}
