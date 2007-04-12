/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.util;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

/**
 * A wizard dialog where the positon and size is stored in dialog settings
 */
public class PositionedWizardDialog extends WizardDialog {

	private static final String	WIZARD_X		= "Wizard.x";		//$NON-NLS-1$
	private static final String	WIZARD_Y		= "Wizard.y";		//$NON-NLS-1$
	private static final String	WIZARD_WIDTH	= "Wizard.width";	//$NON-NLS-1$
	private static final String	WIZARD_HEIGHT	= "Wizard.height";	//$NON-NLS-1$

	private IDialogSettings		fDialogSettings;
	private String				fSettingsSection;

	private int					fDefaultWidth;
	private int					fDefaultHeight;

	/**
	 * @param parent
	 * @param wizard
	 * @param fSettingsSection
	 *        dialog settings section to store the dialog position and size
	 */
	public PositionedWizardDialog(Shell parent, Wizard wizard, String settingsSection,
			int defaultWidth, int defaultHeight) {

		super(parent, wizard);

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettings();
		fSettingsSection = settingsSection;

		fDefaultWidth = defaultWidth;
		fDefaultHeight = defaultHeight;

		Point point = getInitialSize();
		if (point != null) {
			setMinimumPageSize(point.x, point.y);
		}
	}

	protected Point getInitialSize() {

		IDialogSettings settings = fDialogSettings.getSection(fSettingsSection);

		int width = fDefaultWidth;
		int height = fDefaultHeight;

		if (settings == null) {
			final Shell shell = getShell();
			if (shell != null) {
				Point shellSize = shell.getSize();
				return new Point(shellSize.x, shellSize.y);
			}
		} else {
			try {
				width = settings.getInt(WIZARD_WIDTH);
			} catch (NumberFormatException e) {}
			try {
				height = settings.getInt(WIZARD_HEIGHT);
			} catch (NumberFormatException e) {}
		}
		return new Point(width, height);

	}

	protected Point getInitialLocation(Point initialSize) {

		Point location = super.getInitialLocation(initialSize);

		IDialogSettings bounds = fDialogSettings.getSection(fSettingsSection);
		if (bounds != null) {
			try {
				location.x = bounds.getInt(WIZARD_X);
			} catch (NumberFormatException e) {
				// silently ignored
			}
			try {
				location.y = bounds.getInt(WIZARD_Y);
			} catch (NumberFormatException e) {
				// silently ignored
			}
		}
		return location;
	}

	protected void finishPressed() {
		saveBounds();
		super.finishPressed();
	}

	private void saveBounds() {

		IDialogSettings settings = fDialogSettings.getSection(fSettingsSection);
		if (settings == null) {
			settings = new DialogSettings(fSettingsSection);
			fDialogSettings.addSection(settings);
		}

		Rectangle bounds = getShell().getBounds();

		settings.put(WIZARD_X, bounds.x);
		settings.put(WIZARD_Y, bounds.y);
		settings.put(WIZARD_WIDTH, bounds.width);
		settings.put(WIZARD_HEIGHT, bounds.height);
	}

	protected void cancelPressed() {
		saveBounds();
		super.cancelPressed();
	}

}
