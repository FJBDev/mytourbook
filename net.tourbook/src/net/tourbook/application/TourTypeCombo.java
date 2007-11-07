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
package net.tourbook.application;

import net.tourbook.ui.ImageCombo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * Because the {@link ImageCombo} looks very bad on osx, a normal combo box is used on osx and the
 * image combo is used on win32 and linux
 */
public class TourTypeCombo {

	private static final boolean	osx	= "carbon".equals(SWT.getPlatform());

	private Combo					fTourTypeComboOSX;
	private ImageCombo				fTourTypeCombo;

	TourTypeCombo(Composite container, int style) {
		if (osx) {
			fTourTypeComboOSX = new Combo(container, style);
		} else {
			fTourTypeCombo = new ImageCombo(container, style);
		}
	}

	void add(String filterName, Image filterImage) {
		if (osx) {
			fTourTypeComboOSX.add(filterName);
		} else {
			fTourTypeCombo.add(filterName, filterImage);
		}
	}

	void addDisposeListener(DisposeListener disposeListener) {
		if (osx) {
			fTourTypeComboOSX.addDisposeListener(disposeListener);
		} else {
			fTourTypeCombo.addDisposeListener(disposeListener);
		}
	}

	void addSelectionListener(SelectionListener selectionListener) {
		if (osx) {
			fTourTypeComboOSX.addSelectionListener(selectionListener);
		} else {
			fTourTypeCombo.addSelectionListener(selectionListener);
		}
	}

	int getSelectionIndex() {
		if (osx) {
			return fTourTypeComboOSX.getSelectionIndex();
		} else {
			return fTourTypeCombo.getSelectionIndex();
		}
	}

	void removeAll() {
		if (osx) {
			fTourTypeComboOSX.removeAll();
		} else {
			fTourTypeCombo.removeAll();
		}
	}

	void select(int index) {
		if (osx) {
			fTourTypeComboOSX.select(index);
		} else {
			fTourTypeCombo.select(index);
		}
	}

	void setLayoutData(GridData gridData) {
		if (osx) {
			fTourTypeComboOSX.setLayoutData(gridData);
		} else {
			fTourTypeCombo.setLayoutData(gridData);
		}
	}

	void setToolTipText(String tooltip) {
		if (osx) {
			fTourTypeComboOSX.setToolTipText(tooltip);
		} else {
			fTourTypeCombo.setToolTipText(tooltip);
		}
	}

	void setVisibleItemCount(int count) {
		if (osx) {
			fTourTypeComboOSX.setVisibleItemCount(count);
		} else {
			fTourTypeCombo.setVisibleItemCount(count);
		}
	}

}
