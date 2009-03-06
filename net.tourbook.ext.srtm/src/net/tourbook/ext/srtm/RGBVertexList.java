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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

public class RGBVertexList extends ArrayList<RGBVertex> {

	private static final long	serialVersionUID	= 1L;

	public RGB getRGB(long elev) {

		if (size() < 2)
			return new RGB(255, 255, 255);

		for (int ix = size() - 2; ix >= 0; ix--) {
			if (elev > get(ix).getElevation()) {
				RGB rgb1 = get(ix).getRGB();
				RGB rgb2 = get(ix + 1).getRGB();
				long elev1 = get(ix).getElevation();
				long elev2 = get(ix + 1).getElevation();
				long dElevG = elev2 - elev1;
				long dElev1 = elev - elev1;
				long dElev2 = elev2 - elev;
				int red = (int) ((double) (rgb2.red * dElev1 + rgb1.red * dElev2) / dElevG);
				int green = (int) ((double) (rgb2.green * dElev1 + rgb1.green * dElev2) / dElevG);
				int blue = (int) ((double) (rgb2.blue * dElev1 + rgb1.blue * dElev2) / dElevG);
				if (red > 0xFF)
					red = 0xFF;
				if (green > 0xFF)
					green = 0xFF;
				if (blue > 0xFF)
					blue = 0xFF;
				if (red < 0)
					red = 0;
				if (green < 0)
					green = 0;
				if (blue < 0)
					blue = 0;
				return new RGB(red, green, blue);
			}
		}
		return new RGB(255, 255, 255);
	}

	public String toString() {
		String s;
		s = ""; //$NON-NLS-1$
		for (int i = 0; i < size(); i++) {
			s += get(i);
		}
		return s;
	}

	@SuppressWarnings("unchecked")
	public void sort() {
		Collections.sort(this);
	}

	public void set(String s) {
		Pattern pattern = Pattern.compile("^([-]*[0-9]*),([0-9]*),([0-9]*),([0-9]*);(.*)$"); //$NON-NLS-1$
		clear();
		int ix = 0;
		while (s.length() > 0) {
			Matcher matcher = pattern.matcher(s);
			if (matcher.matches()) {
				Long elev = new Long(matcher.group(1));
				Integer red = new Integer(matcher.group(2));
				Integer green = new Integer(matcher.group(3));
				Integer blue = new Integer(matcher.group(4));
				RGBVertex rgbVertex = new RGBVertex();
				rgbVertex.setElev(elev.longValue());
				rgbVertex.setRGB(new RGB(red.intValue(), green.intValue(), blue.intValue()));
				add(ix, rgbVertex);
				ix++;
				s = matcher.group(5); // rest
			}
		}
		sort();
	}

	public void set(RGBVertexList rgbVertexList) {
		clear();
		for (int ix = 0; ix < rgbVertexList.size(); ix++) {
			RGBVertex rgbVertex = rgbVertexList.get(ix);
			add(ix, rgbVertex);
		}
	}

	public Image getImage(Display display, int width, int height) {
		final Image image = new Image(display, width, height);
		GC gc = new GC(image);
		long elevMax = size() == 0 ? 8850 : get(size() - 1).getElevation();
		for (int x = 0; x < width; x++) {
			long elev = elevMax * x / width;
			RGB rgb = getRGB(elev);
			Color color = new Color(display, rgb);
			gc.setForeground(color);
			gc.drawLine(width - x, 0, width - x, height);
		}
		Transform transform = new Transform(display);
		for (int ix = 0; ix < size(); ix++) {
			long elev = get(ix).getElevation();
			if (elev < 0) continue;
			int x = (int) (elev * width / elevMax);
			x = Math.max(x, 13);
			RGB rgb = getRGB(elev);
			rgb.red = 255 - rgb.red;
			rgb.green = 255 - rgb.green;
			rgb.blue = 255 - rgb.blue;
			Color color = new Color(display, rgb);
			gc.setForeground(color);
			transform.setElements(0, -1, 1, 0, width - x - 1, height - 3); // Rotate by -90 degrees	
			gc.setTransform(transform);
			gc.drawText(""+elev, 0, 0, true); //$NON-NLS-1$
		}
		transform.dispose();
		
		return image;
	}

	public void init() {
		if (size() > 0)
			return;
		add(0, new RGBVertex(0, 0, 255, 0));
		add(1, new RGBVertex(0, 255, 0, 1000));
		add(2, new RGBVertex(255, 0, 0, 2000));
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public static void main(String[] args) {
		return;
	}
}
