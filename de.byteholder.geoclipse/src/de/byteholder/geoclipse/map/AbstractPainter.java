package de.byteholder.geoclipse.map;

import org.eclipse.swt.graphics.GC;

public abstract class AbstractPainter<T> {
 
	/**
	 * Dispose resources in the {@link Painter}
	 */
	protected abstract void dispose();

	/**
	 * Dispose resources which are used temporarily
	 */
	protected void disposeTempResources() {}

	/**
	 * @param gc
	 * @param map
	 * @param tile
	 * @param tileSize
	 * @param parts
	 *            number or parts, this is an odd number where the requested tile image is in the
	 *            middle of a square with parts*parts drawing tiles
	 * @return
	 */
	protected abstract boolean doPaint(final GC gc, final T map, final Tile tile, final int parts);
}
