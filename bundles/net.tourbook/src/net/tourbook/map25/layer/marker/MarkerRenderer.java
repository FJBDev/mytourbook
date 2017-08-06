/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.layer.marker;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import net.tourbook.common.color.ColorUtil;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.marker.algorithm.distance.ClusterItem;
import net.tourbook.map25.layer.marker.algorithm.distance.DistanceBasedAlgorithm;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.layers.marker.utils.SparseIntArray;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.utils.FastMath;
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;

/**
 * Original: {@link org.oscim.layers.marker.MarkerRenderer}
 */
public class MarkerRenderer extends BucketRenderer {

	/**
	 * default color of number inside the icon. Would be super-cool to cook this into the map theme
	 */
	private static final int						CLUSTER_COLOR_TEXT			= 0xff8000c0;

	/**
	 * default color of circle background
	 */
	private static final int						CLUSTER_COLOR_BACK			= 0xffffffff;

	/**
	 * Map Cluster Icon Size. This is the biggest size for clusters of CLUSTER_MAXSIZE elements.
	 * Smaller clusters will be slightly smaller
	 */
	public static final int							MAP_MARKER_CLUSTER_SIZE_DP	= 64;

	/**
	 * Clustering grid square size, decrease to cluster more aggresively. Ideally this value is the
	 * typical marker size
	 */
	private static final int						MAP_GRID_SIZE_DP			= 64;

	private static final HashMap<Integer, Bitmap>	_clusterBitmaps				= new HashMap<>();

	private static final TimSort<ProjectedMarker>	ZSORT						= new TimSort<ProjectedMarker>();

	final static Comparator<ProjectedMarker>		zComparator;
	static {

		zComparator = new Comparator<ProjectedMarker>() {

			@Override
			public int compare(	final ProjectedMarker a,
								final ProjectedMarker b) {

				if (a.isVisible && b.isVisible) {

					if (a.dy > b.dy) {
						return -1;
					}
					if (a.dy < b.dy) {
						return 1;
					}

				} else if (a.isVisible) {

					return -1;

				} else if (b.isVisible) {

					return 1;
				}

				return 0;
			}
		};
	}

	private MarkerSymbol						_defaultMarkerSymbol;

	private final MarkerLayer					_markerLayer;
	private final SymbolBucket					_symbolBucket;

	private final float[]						_tmpBox					= new float[8];
	private final Point							_tmpPoint				= new Point();

	/**
	 * increase view to show items that are partially visible
	 */
	private int									_extents				= 100;

	/**
	 * flag to force update of markers
	 */
	private boolean								_isForceUpdateMarkers;

	private ProjectedMarker[]					_allProjectedMarker;

	private int									_clusterBackgroundColor	= CLUSTER_COLOR_BACK;
	private int									_clusterForegroundColor	= CLUSTER_COLOR_TEXT;

	/**
	 * Discrete scale step, used to trigger reclustering on significant scale change
	 */
	private int									_scaleSaved				= 0;

	/**
	 * We use a flat Sparse array to calculate the clusters. The sparse array models a 2D map where
	 * every (x,y) denotes a grid slot, ie. 64x64dp. For efficiency I use a linear sparsearray with
	 * ARRindex = SLOTypos * max_x + SLOTxpos"
	 */
	private SparseIntArray						_clusterCells			= new SparseIntArray(200);					// initial space for 200 markers, that's not a lot of memory, and in most cases will avoid resizing the array

	private double								_mapTileScale;

	private int									_clusterSymbolSizeDP	= MAP_MARKER_CLUSTER_SIZE_DP;
	private int									_clusterGridSize		= MAP_GRID_SIZE_DP;

	/**
	 * When <code>true</code> all items are clustered, otherwise nothing is clustered.
	 */
	private boolean								_isClustering			= true;

	/**
	 * When <code>true</code> then the symbol is displayed as billboard, otherwise it is clamped to
	 * the ground.
	 */
	private boolean								_isBillboard;

	private ClusterAlgorithm					_clusterAlgorithm;

	private DistanceBasedAlgorithm<ClusterItem>	_distanceAlgorithm		= new DistanceBasedAlgorithm<ClusterItem>();

	/**
	 * Class to wrap the cluster icon style properties
	 */
	public static class ClusterStyle {

		final int	background;
		final int	foreground;

		/**
		 * Creates the Cluster style
		 *
		 * @param fore
		 *            Foreground (border and text) color
		 * @param back
		 *            Background (circle) color
		 */

		public ClusterStyle(final int fore, final int back) {

			foreground = fore;
			background = back;
		}
	}

	/**
	 * Constructs a clustered marker renderer
	 *
	 * @param markerLayer
	 *            The owner layer
	 * @param defaultSymbol
	 *            The default symbol
	 * @param style
	 *            The desired style, or NULL to disable clustering
	 */
	public MarkerRenderer(final MarkerLayer markerLayer) {

		_markerLayer = markerLayer;

		_symbolBucket = new SymbolBucket();

		configureRenderer();
	}

	private static void sort(final ProjectedMarker[] a, final int lo, final int hi) {

		final int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}

		ZSORT.doSort(a, zComparator, lo, hi);
	}

	public synchronized void configureRenderer() {

		final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

		_clusterSymbolSizeDP = config.clusterSymbolSize;
		_isBillboard = config.clusterOrientation == Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;
		_clusterAlgorithm = (ClusterAlgorithm) config.clusterAlgorithm;

		_clusterForegroundColor = ColorUtil.getARGB(config.clusterOutline_Color, config.clusterOutline_Opacity);
		_clusterBackgroundColor = ColorUtil.getARGB(config.clusterFill_Color, config.clusterFill_Opacity);

		_defaultMarkerSymbol = createMarkerSymbol();

		// remove cached bitmaps
		_clusterBitmaps.clear();

		final boolean isClustering = config.isMarkerClustered;
		final int clusterGridSize = config.clusterGridSize;

		// check if modified, this is an expensive operation
		if (isClustering != _isClustering || clusterGridSize != _clusterGridSize) {

			_isClustering = isClustering;
			_clusterGridSize = clusterGridSize;

			// post repopulation to the main thread
			_markerLayer.map().post(new Runnable() {
				@Override
				public void run() {
					createCluster();
				}
			});
		}

		_isForceUpdateMarkers = true;
	}

	void createCluster() {

		ProjectedMarker[] allProjectedMarker;
		switch (_clusterAlgorithm) {

		case Distance:
			allProjectedMarker = createCluster_Distance();
			break;

		case FirstMarker:
		case Grid:
		default:
			allProjectedMarker = createCluster_Grid();
			break;
		}

		// update the UI
		synchronized (this) {

			_isForceUpdateMarkers = true;
			_allProjectedMarker = allProjectedMarker;
		}
	}

	private ProjectedMarker[] createCluster_Distance() {

		final ProjectedMarker[] allProjectedMarker = new ProjectedMarker[0];

		return allProjectedMarker;
	}

	private ProjectedMarker[] createCluster_Grid() {

		final List<MapMarker> allMarkers = _markerLayer.getAllMarkers();

		final int numMarkers = allMarkers.size();

		final ProjectedMarker[] allProjectedMarker = new ProjectedMarker[numMarkers];

		/*
		 * the grid slot size in px. increase to group more aggressively. currently set to marker
		 * size
		 */
		final int clusterGridSize = ScreenUtils.getPixels(_clusterGridSize);

		/*
		 * the factor to map into Grid Coordinates (discrete squares of GRIDSIZE x GRIDSIZE)
		 */
		final long maxCols = (long) (_mapTileScale / clusterGridSize);

		// clear grid map to count items that share the same "grid slot"
		_clusterCells.clear();

		for (int markerIndex = 0; markerIndex < numMarkers; markerIndex++) {

			final MapMarker mapMarker = allMarkers.get(markerIndex);
			final ProjectedMarker projectedMarker = allProjectedMarker[markerIndex] = new ProjectedMarker();

			projectedMarker.mapMarker = mapMarker;

			// project marker
			MercatorProjection.project(projectedMarker.mapMarker.geoPoint, _tmpPoint);
			projectedMarker.projectedX = _tmpPoint.x;
			projectedMarker.projectedY = _tmpPoint.y;

			// items can be declared non-clusterable
			if (_isClustering) {

				// absolute item X position in the grid
				final int colX = (int) (projectedMarker.projectedX * maxCols);
				final int colY = (int) (projectedMarker.projectedY * maxCols); // absolute item Y position

				// Index in the sparsearray map
				final int clusterIndex = (int) (colX + colY * maxCols);

				// we store in the linear sparsearray the index of the marker,
				// ie, index = y * maxcols + x; array[index} = markerIndex

				// Lets check if there's already an item in the grid slot
				final int storedClusterIndex = _clusterCells.get(clusterIndex, -1);

				if (storedClusterIndex == -1) {

					// no item at that grid position. The grid slot is free so let's
					// store this item "i" (we identify every item by its InternalItem index)

					_clusterCells.put(clusterIndex, markerIndex);

				} else {

					// at that grid position there's already a marker index
					// mark this item as clustered out, so it will be skipped in the update() call

					projectedMarker.isClusteredOut = true;

					// and increment the count on its "parent" that will from now on act as a cluster
					final ProjectedMarker projectedCluster = allProjectedMarker[storedClusterIndex];

					// set cluster position to the center of the grid
					if (projectedCluster.clusterSize == 0) {

						final double projectedCol_X1 = (double) colX / maxCols;
						final double projectedCol_X2 = (double) (colX + 1) / maxCols;
						final double projectedCol_Xhalf = (projectedCol_X1 - projectedCol_X2) / 2;

						final double projectedCol_Y1 = (double) colY / maxCols;
						final double projectedCol_Y2 = (double) (colY + 1) / maxCols;
						final double projectedCol_Yhalf = (projectedCol_Y1 - projectedCol_Y2) / 2;

						projectedCluster.projectedClusterX = projectedCol_X2 + projectedCol_Xhalf;
						projectedCluster.projectedClusterY = projectedCol_Y2 + projectedCol_Yhalf;
					}

					projectedCluster.clusterSize++;
				}
			}
		}

		return allProjectedMarker;
	}

	private MarkerSymbol createMarkerSymbol() {

		final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

		final Paint paintFill = CanvasAdapter.newPaint();
		paintFill.setColor(ColorUtil.getARGB(config.markerFill_Color, config.markerFill_Opacity));
		paintFill.setStyle(Paint.Style.FILL);

		final Paint paintOutline = CanvasAdapter.newPaint();
		paintOutline.setColor(ColorUtil.getARGB(config.markerOutline_Color, config.markerOutline_Opacity));
		paintOutline.setStyle(Paint.Style.STROKE);
		paintOutline.setStrokeWidth(ScreenUtils.getPixels(2));

		final int iconSize = ScreenUtils.getPixels(config.markerSymbolSize);

		final Bitmap bitmap = CanvasAdapter.newBitmap(iconSize, iconSize, 0);
		final org.oscim.backend.canvas.Canvas canvas = CanvasAdapter.newCanvas();
		canvas.setBitmap(bitmap);

		final int iconSize2 = iconSize / 2;
		final int noneClippingRadius = iconSize2 - ScreenUtils.getPixels(2);

		// fill + outline
		canvas.drawCircle(iconSize2, iconSize2, noneClippingRadius, paintFill);
		canvas.drawCircle(iconSize2, iconSize2, noneClippingRadius, paintOutline);

		final boolean isBillboard = config.markerOrientation == Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;

		return new MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.CENTER, isBillboard);
	}

	/**
	 * Gets a bitmap for a given cluster size
	 *
	 * @param size
	 *            The cluster size. Can be greater than CLUSTER_MAXSIZE.
	 * @return A somewhat cool bitmap to be used as the cluster marker
	 */
	private Bitmap getClusterBitmap(final int size) {

		// return cached bitmap if exists. cache hit !
		final Bitmap clusterBitmap = _clusterBitmaps.get(size);

		if (clusterBitmap != null) {
			return clusterBitmap;
		}

		// create and cache bitmap. This is unacceptable inside the GL thread,
		// so we'll call this routine at the beginning to pre-cache all bitmaps

		final ScreenUtils.ClusterDrawable drawable = new ScreenUtils.ClusterDrawable(

				_clusterSymbolSizeDP,

				_clusterForegroundColor,
				_clusterBackgroundColor,

				Integer.toString(size));

		final Bitmap paintedBitmap = drawable.getBitmap();

		_clusterBitmaps.put(size, paintedBitmap);

		return paintedBitmap;
	}

	public MarkerSymbol getDefaultMarkerSymbol() {
		return _defaultMarkerSymbol;
	}

	public void update() {
		_isForceUpdateMarkers = true;
	}

	@Override
	public synchronized void update(final GLViewport viewport) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tupdate()"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final MapPosition mapPosition = viewport.pos;
		final double mapScale = mapPosition.scale;
		final float mapRotation = mapPosition.bearing;

		final double mapTileScale = Tile.SIZE * mapScale;

		/*
		 * Clustering check: If clustering is enabled and there's been a significant scale change
		 * trigger repopulation and return. After repopulation, this will be called again
		 */

		// (int) log of scale gives us adequate steps to trigger clustering
		final int scaleSaved = FastMath.log2((int) mapTileScale);

		if (scaleSaved != _scaleSaved) {

			_scaleSaved = scaleSaved;
			_mapTileScale = mapTileScale;

			// post repopulation to the main thread
			_markerLayer.map().post(new Runnable() {
				@Override
				public void run() {
					createCluster();
				}
			});

			// wait and see
			return;
		}

		if (!viewport.changed() && !_isForceUpdateMarkers) {
			return;
		}

		_isForceUpdateMarkers = false;

		final double projMapX = mapPosition.x;
		final double projMapY = mapPosition.y;

		//int changesInvisible = 0;
		//int changedVisible = 0;
		int numVisible = 0;

		_markerLayer.map().viewport().getMapExtents(_tmpBox, _extents);

		final long flip = (long) (mapTileScale) >> 1;

		if (_allProjectedMarker == null) {

			if (buckets.get() != null) {
				buckets.clear();
				compile();
			}

			return;
		}

		final double angle = Math.toRadians(mapPosition.bearing);
		final float cos = (float) Math.cos(angle);
		final float sin = (float) Math.sin(angle);

		/* check visibility */
		for (final ProjectedMarker projectedMarker : _allProjectedMarker) {

			projectedMarker.isModified = false;

			projectedMarker.mapX = (float) ((projectedMarker.projectedX - projMapX) * mapTileScale);
			projectedMarker.mapY = (float) ((projectedMarker.projectedY - projMapY) * mapTileScale);

			// flip map world border
			if (projectedMarker.mapX > flip) {
				projectedMarker.mapX -= (long) mapTileScale;
			} else if (projectedMarker.mapX < -flip) {
				projectedMarker.mapX += (long) mapTileScale;
			}

			if (projectedMarker.isClusteredOut || !GeometryUtils.pointInPoly(
					projectedMarker.mapX,
					projectedMarker.mapY,
					_tmpBox,
					8,
					0)) {

				// either properly invisible, or clustered out. Items marked as clustered out mean there's another item
				// on the same-ish position that will be promoted to cluster marker, so this particular item is considered
				// invisible

				if (projectedMarker.isVisible && (!projectedMarker.isClusteredOut)) {

					// it was previously visible, but now it won't
					projectedMarker.isModified = true;

					// changes to invible
					//changesInvisible++;
				}

				continue;
			}

			// item IS definitely visible
			projectedMarker.dy = sin * projectedMarker.mapX + cos * projectedMarker.mapY;

			if (!projectedMarker.isVisible) {
				projectedMarker.isVisible = true;
				//changedVisible++;
			}

			numVisible++;
		}

		//log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

		/*
		 * only update when zoomlevel changed, new items are visible or more than 10 of the current
		 * items became invisible
		 */
		//if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
		//	return;
		buckets.clear();

		if (numVisible == 0) {
			compile();
			return;
		}

		/* keep position for current state */
		mMapPosition.copy(mapPosition);
		mMapPosition.bearing = -mMapPosition.bearing;

		// why do we sort ? z-index?
		sort(_allProjectedMarker, 0, _allProjectedMarker.length);

		//log.debug(Arrays.toString(mItems));

		for (final ProjectedMarker projMarker : _allProjectedMarker) {

			// skip invisible AND clustered-out
			if (!projMarker.isVisible || projMarker.isClusteredOut) {
				continue;
			}

			if (projMarker.isModified) {

				projMarker.isVisible = false;

				continue;
			}

			final SymbolItem mapSymbol = SymbolItem.pool.get();

			if (projMarker.clusterSize > 0) {

				// this item will act as a cluster, just use a proper bitmap
				// depending on cluster size, instead of its marker

				final Bitmap bitmap = getClusterBitmap(projMarker.clusterSize + 1);

				float mapX = 0;
				float mapY = 0;

				switch (_clusterAlgorithm) {

				case Distance:
					break;

				case Grid:

					mapX = (float) ((projMarker.projectedClusterX - projMapX) * mapTileScale);
					mapY = (float) ((projMarker.projectedClusterY - projMapY) * mapTileScale);

					// flip map world border
					if (mapX > flip) {
						mapX -= (long) mapTileScale;
					} else if (mapX < -flip) {
						mapX += (long) mapTileScale;
					}

					break;

				case FirstMarker:
				default:

					mapX = projMarker.mapX;
					mapY = projMarker.mapY;

					break;
				}

				mapSymbol.set(mapX, mapY, bitmap, true);

				mapSymbol.offset = new PointF(0.5f, 0.5f);
				mapSymbol.billboard = _isBillboard;

				if (!_isBillboard) {
// CLUSTERING IS VERY BUGGY WHEN ROTATION IS SET
//					mapSymbol.rotation = -mapRotation;
				}

			} else {

				// normal item, use its marker

				MarkerSymbol markerSymbol = projMarker.mapMarker.markerSymbol;

				if (markerSymbol == null) {
					markerSymbol = _defaultMarkerSymbol;
				}

				mapSymbol.set(projMarker.mapX, projMarker.mapY, markerSymbol.getBitmap(), markerSymbol.mBillboard);
				mapSymbol.offset = markerSymbol.getHotspot();
				mapSymbol.billboard = markerSymbol.isBillboard();
			}

//			System.out.println(
//					(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//
//							+ (String.format("\tx:%10.3f\ty:%10.3f", projectedMarker.x, projectedMarker.y))
//							+ (String.format(
//									"\tx:%10.3f\ty:%10.3f",
//									projClusterX,
//									projClusterY))
//
////							+ ("\t")
//
//			);
//			// TODO remove SYSTEM.OUT.PRINTLN

			_symbolBucket.pushSymbol(mapSymbol);
		}

		buckets.set(_symbolBucket);
		buckets.prepare();

		compile();
	}

}
