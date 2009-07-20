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
package net.tourbook.colors;

import net.tourbook.mapping.LegendColor;

import org.eclipse.swt.graphics.RGB;

public class GraphColorItem {

	private ColorDefinition	fColorDefinition;

	private String			fColorPrefName;
	private String			fVisibleName;

	/**
	 * <code>true</code> when this {@link GraphColorItem} is used as for a legend
	 */
	private boolean			fIsLegend;

	public GraphColorItem(	final ColorDefinition parent,
							final String colorPrefName,
							final String visibleName,
							final boolean isLegend) {

		fColorDefinition = parent;

		fColorPrefName = colorPrefName;
		fVisibleName = visibleName;

		fIsLegend = isLegend;
	}

	public ColorDefinition getColorDefinition() {
		return fColorDefinition;
	}

	public String getColorId() {
		return fColorDefinition.getPrefName() + "." + fColorPrefName; //$NON-NLS-1$
	}

	public String getName() {
		return fVisibleName;
	}

	public RGB getNewRGB() {
		return fColorPrefName.compareTo(GraphColorProvider.PREF_COLOR_LINE) == 0
				? fColorDefinition.getNewLineColor()
				: fColorPrefName.compareTo(GraphColorProvider.PREF_COLOR_DARK) == 0
						? fColorDefinition.getNewGradientDark()
						: fColorDefinition.getNewGradientBright();
	}

	String getPrefName() {
		return fColorPrefName;
	}

	/**
	 * @return Returns <code>true</code> when this {@link GraphColorItem} represents a
	 *         {@link LegendColor}
	 */
	public boolean isLegend() {
		return fIsLegend;
	}

	public void setName(final String fName) {
		this.fVisibleName = fName;
	}

	public void setNewRGB(final RGB rgb) {
		if (fColorPrefName.compareTo(GraphColorProvider.PREF_COLOR_LINE) == 0) {
			fColorDefinition.setNewLineColor(rgb);
		} else if (fColorPrefName.compareTo(GraphColorProvider.PREF_COLOR_DARK) == 0) {
			fColorDefinition.setNewGradientDark(rgb);
		} else {
			fColorDefinition.setNewGradientBright(rgb);
		}
	}

	void setPrefName(final String fPrefName) {
		this.fColorPrefName = fPrefName;
	}
}
