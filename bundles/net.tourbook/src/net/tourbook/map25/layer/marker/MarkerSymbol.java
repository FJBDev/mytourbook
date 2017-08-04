/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 * Copyright 2016 Izumi Kawashima
 * Copyright 2017 Longri
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map25.layer.marker;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.PointF;
import org.oscim.renderer.atlas.TextureRegion;

public class MarkerSymbol {

	final Bitmap		mBitmap;

	final TextureRegion	mTextureRegion;

	/**
	 * Hotspot offset
	 */
	final PointF		mOffset;

	final boolean		mBillboard;
	float				rotation	= 0;

	/**
	 * Indicates a hotspot for an area. This is where the origin (0,0) of a point will be located
	 * relative to the area. In otherwords this acts as an offset. NONE indicates that no adjustment
	 * should be made.
	 */
	public enum HotspotPlace {

		NONE, //
		CENTER, //

		BOTTOM_CENTER, //
		TOP_CENTER, //
		RIGHT_CENTER, //
		LEFT_CENTER, //

		UPPER_RIGHT_CORNER, //
		LOWER_RIGHT_CORNER, //
		UPPER_LEFT_CORNER, //
		LOWER_LEFT_CORNER
	}

	public MarkerSymbol(final Bitmap bitmap, final float relX, final float relY) {
		this(bitmap, relX, relY, true);
	}

	public MarkerSymbol(final Bitmap bitmap, final float relX, final float relY, final boolean billboard) {

		mBitmap = bitmap;
		mOffset = new PointF(relX, relY);
		mBillboard = billboard;
		mTextureRegion = null;
	}

	public MarkerSymbol(final Bitmap bitmap, final HotspotPlace hotspot) {
		this(bitmap, hotspot, true);
	}

	public MarkerSymbol(final Bitmap bitmap, final HotspotPlace hotspot, final boolean billboard) {

		switch (hotspot) {

		case BOTTOM_CENTER:
			mOffset = new PointF(0.5f, 1);
			break;

		case TOP_CENTER:
			mOffset = new PointF(0.5f, 0);
			break;

		case RIGHT_CENTER:
			mOffset = new PointF(1, 0.5f);
			break;

		case LEFT_CENTER:
			mOffset = new PointF(0, 0.5f);
			break;

		case UPPER_RIGHT_CORNER:
			mOffset = new PointF(1, 0);
			break;

		case LOWER_RIGHT_CORNER:
			mOffset = new PointF(1, 1);
			break;

		case UPPER_LEFT_CORNER:
			mOffset = new PointF(0, 0);
			break;

		case LOWER_LEFT_CORNER:
			mOffset = new PointF(0, 1);
			break;

		default:
			mOffset = new PointF(0.5f, 0.5f);
		}

		mBitmap = bitmap;
		mBillboard = billboard;
		mTextureRegion = null;
	}

	public MarkerSymbol(final TextureRegion textureRegion, final float relX, final float relY) {
		this(textureRegion, relX, relY, true);
	}

	public MarkerSymbol(final TextureRegion textureRegion,
						final float relX,
						final float relY,
						final boolean billboard) {

		mBitmap = null;
		mTextureRegion = textureRegion;
		mOffset = new PointF(relX, relY);
		mBillboard = billboard;
	}

	public MarkerSymbol(final TextureRegion textureRegion, final HotspotPlace hotspot) {
		this(textureRegion, hotspot, true);
	}

	public MarkerSymbol(final TextureRegion textureRegion, final HotspotPlace hotspot, final boolean billboard) {

		mBitmap = null;
		mTextureRegion = textureRegion;

		switch (hotspot) {

		case BOTTOM_CENTER:
			mOffset = new PointF(0.5f, 1);
			break;

		case TOP_CENTER:
			mOffset = new PointF(0.5f, 0);
			break;

		case RIGHT_CENTER:
			mOffset = new PointF(1, 0.5f);
			break;

		case LEFT_CENTER:
			mOffset = new PointF(0, 0.5f);
			break;

		case UPPER_RIGHT_CORNER:
			mOffset = new PointF(1, 0);
			break;

		case LOWER_RIGHT_CORNER:
			mOffset = new PointF(1, 1);
			break;

		case UPPER_LEFT_CORNER:
			mOffset = new PointF(0, 0);
			break;

		case LOWER_LEFT_CORNER:
			mOffset = new PointF(0, 1);
			break;

		default:
			mOffset = new PointF(0.5f, 0.5f);
		}

		mBillboard = billboard;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	public PointF getHotspot() {
		return mOffset;
	}

	public float getRotation() {
		return this.rotation;
	}

	public TextureRegion getTextureRegion() {
		return mTextureRegion;
	}

	public boolean isBillboard() {
		return mBillboard;
	}

	public boolean isBitmap() {
		return mBitmap != null;
	}

	public boolean isInside(final float dx, final float dy) {

		/* TODO handle no-billboard */
		int w, h;
		if (isBitmap()) {
			w = mBitmap.getWidth();
			h = mBitmap.getHeight();
		} else {
			w = mTextureRegion.rect.w;
			h = mTextureRegion.rect.h;
		}
		final float ox = -w * mOffset.x;
		final float oy = -h * (1 - mOffset.y);

		return dx >= ox && dy >= oy && dx <= ox + w && dy <= oy + h;
	}

	public void setRotation(final float rotation) {
		this.rotation = rotation;
	}

	@Override
	public String toString() {

		return "MarkerSymbol ["

//				+ "mBitmap=" + mBitmap + ", "
//				+ "mTextureRegion=" + mTextureRegion + ", "
				+ "mOffset=" + mOffset + ", "
				+ "mBillboard=" + mBillboard + ", "
				+ "rotation=" + rotation

				+ "]";
	}
}
