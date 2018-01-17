/*
 * Copyright 2012, 2013 Hannes Janetzek
 * Copyright 2017 Wolfgang Schramm
 * Copyright 2017 devemux86
 * Copyright 2017 Andrey Novikov
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
package net.tourbook.map25.layer.labeling;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.utils.async.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Original: {@link org.oscim.layers.tile.vector.labeling.LabelLayer}
 */
public class LabelLayerMT extends Layer implements Map.UpdateListener, TileManager.Listener {

	static final Logger				log					= LoggerFactory.getLogger(LabelLayerMT.class);

	public final static String		LABEL_DATA			= LabelLayerMT.class.getName();

	private final static long		MAX_RELABEL_DELAY	= 100;

	private final LabelPlacement	_labelPlacer;
	private final Worker			_worker;

	class Worker extends SimpleWorker<LabelTask> {

		public Worker(final Map map) {
			super(map, 50, new LabelTask(), new LabelTask());
		}

		@Override
		public void cleanup(final LabelTask t) {}

		@Override
		public boolean doWork(final LabelTask t) {

			if (_labelPlacer.updateLabels(t)) {

				mMap.render();

				return true;
			}

			return false;
		}

		@Override
		public void finish() {
			_labelPlacer.cleanup();
		}

		@Override
		public synchronized boolean isRunning() {
			return mRunning;
		}
	}

	public LabelLayerMT(final Map map, final VectorTileLayer l) {
		this(map, l, new LabelTileLoaderHook());
	}

	public LabelLayerMT(final Map map, final VectorTileLayer l, final VectorTileLayer.TileLoaderThemeHook h) {

		super(map);

		l.getManager().events.bind(this);
		l.addHook(h);

		_labelPlacer = new LabelPlacement(map, l.tileRenderer());
		_worker = new Worker(map);

		mRenderer = new TextRenderer(_worker);
	}

	public void clearLabels() {
		_worker.cancel(true);
	}

	@Override
	public void onDetach() {

		_worker.cancel(true);
		super.onDetach();
	}

	@Override
	public void onMapEvent(final Event event, final MapPosition mapPosition) {

		if (event == Map.CLEAR_EVENT) {
			_worker.cancel(true);
		}

		if (!isEnabled()) {
			return;
		}

		if (event == Map.POSITION_EVENT) {
			_worker.submit(MAX_RELABEL_DELAY);
		}
	}

	@Override
	public void onTileManagerEvent(final Event e, final MapTile tile) {

		if (e == TileManager.TILE_LOADED) {
			if (tile.isVisible && isEnabled()) {
				_worker.submit(MAX_RELABEL_DELAY / 4);
				//log.debug("tile loaded: {}", tile);
			}
		} else if (e == TileManager.TILE_REMOVED) {
			//log.debug("tile removed: {}", tile);
		}
	}

	//    @Override
	//    public void onMotionEvent(MotionEvent e) {
	//        //    int action = e.getAction() & MotionEvent.ACTION_MASK;
	//        //    if (action == MotionEvent.ACTION_POINTER_DOWN) {
	//        //        multi++;
	//        //        mTextRenderer.hold(true);
	//        //    } else if (action == MotionEvent.ACTION_POINTER_UP) {
	//        //        multi--;
	//        //        if (multi == 0)
	//        //            mTextRenderer.hold(false);
	//        //    } else if (action == MotionEvent.ACTION_CANCEL) {
	//        //        multi = 0;
	//        //        log.debug("cancel " + multi);
	//        //        mTextRenderer.hold(false);
	//        //    }
	//    }

	public void update() {

		if (!isEnabled()) {
			return;
		}

		_worker.submit(MAX_RELABEL_DELAY);
	}

}
