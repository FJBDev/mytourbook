/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.formatter;

import java.text.NumberFormat;

import net.tourbook.common.Messages;

public class ValueFormatter_Calories_Kcal implements IValueFormatter {

	private final static NumberFormat	_nf1	= NumberFormat.getNumberInstance();

	static {

		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	@Override
	public String printDouble(final double value) {
		return Messages.App_Error_NotSupportedValueFormatter;
	}

	@Override
	public String printLong(final long value) {
		return _nf1.format((double) value / 1000);
	}

}
