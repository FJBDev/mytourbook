/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
/*
 * 2007-04-29
 * - completely removed DesignTime; this can be re-added later if needed
 * - extends JXPanel -> extends Canvas
 * - changed default constructor to receive standard SWT parameters parent and style
 * - added getHeight() and getWidth() for those Swing calls
 * - added isOpaque() for the same reason
 * - added getInsets() for the same reason
 * - renamed doPaintComponent to paintControl (SWT default); it now also receives a
 * PaintEvent as parameter instead of a Graphics object; it's not private anymore,
 * which is also a SWT convention
 * - addPaintListener() for that method
 * - ported paintControl() to SWT graphics operations
 * - added computeSize()
 * ! basically works
 * however needs much work, no Listeners are implemented yet and there are some
 * issues with thread access
 * - thread access problem found and fixed; UI methods may only be called from the
 * UI thread!
 * 2007-04-30
 * - fixed memory leaks; all images should now be disposed, when no longer needed
 * - implemented Listeners except MouseWheel
 */
package de.byteholder.geoclipse.map;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.IBreadcrumbListener;
import de.byteholder.geoclipse.map.event.IExternalAppListener;
import de.byteholder.geoclipse.map.event.IGeoPositionListener;
import de.byteholder.geoclipse.map.event.IHoveredTourListener;
import de.byteholder.geoclipse.map.event.IMapGridListener;
import de.byteholder.geoclipse.map.event.IMapInfoListener;
import de.byteholder.geoclipse.map.event.IMapPositionListener;
import de.byteholder.geoclipse.map.event.IMapSelectionListener;
import de.byteholder.geoclipse.map.event.IPOIListener;
import de.byteholder.geoclipse.map.event.ITourSelectionListener;
import de.byteholder.geoclipse.map.event.MapGeoPositionEvent;
import de.byteholder.geoclipse.map.event.MapHoveredTourEvent;
import de.byteholder.geoclipse.map.event.MapPOIEvent;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.ui.TextWrapPainter;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.Images;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.DPITools;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.HoveredAreaContext;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.ImageConverter;
import net.tourbook.common.util.ImageUtils;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.NoAutoScalingImageDataProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourMarkerType;
import net.tourbook.data.TourPhoto;
import net.tourbook.data.TourWayPoint;
import net.tourbook.map.location.LocationType;
import net.tourbook.map.location.MapLocationToolTip;
import net.tourbook.map2.view.Map2Config;
import net.tourbook.map2.view.Map2ConfigManager;
import net.tourbook.map2.view.Map2PainterConfig;
import net.tourbook.map2.view.Map2Point;
import net.tourbook.map2.view.Map2PointManager;
import net.tourbook.map2.view.Map2View;
import net.tourbook.map2.view.MapLabelLayout;
import net.tourbook.map2.view.MapPointStatistics;
import net.tourbook.map2.view.MapPointToolTip;
import net.tourbook.map2.view.MapPointType;
import net.tourbook.map2.view.MapTourMarkerTime;
import net.tourbook.map2.view.SelectionMapSelection;
import net.tourbook.map2.view.SlideoutMap2_PhotoHistogram;
import net.tourbook.map2.view.SlideoutMap2_PhotoImage;
import net.tourbook.map2.view.SlideoutMap2_PhotoOptions;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map25.layer.marker.ScreenUtils;
import net.tourbook.map25.layer.marker.algorithm.distance.Cluster;
import net.tourbook.map25.layer.marker.algorithm.distance.ClusterItem;
import net.tourbook.map25.layer.marker.algorithm.distance.DistanceClustering;
import net.tourbook.map25.layer.marker.algorithm.distance.QuadItem;
import net.tourbook.map25.layer.marker.algorithm.distance.StaticCluster;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.IPhotoServiceProvider;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImages;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.Map2_Appearance;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.filter.TourFilterFieldOperator;
import net.tourbook.tour.filter.geo.TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;
import net.tourbook.tour.photo.TourPhotoManager;
import net.tourbook.ui.IInfoToolTipProvider;
import net.tourbook.ui.IMapToolTipProvider;
import net.tourbook.ui.MTRectangle;

import org.apache.commons.text.WordUtils;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.oscim.core.GeoPoint;

import particlelabeling.PointFeature;
import particlelabeling.PointFeatureLabeler;

public class Map2 extends Canvas {

   private static final char            NL                                             = UI.NEW_LINE;

   private static final IDialogSettings _geoFilterState                                = TourGeoFilter_Manager.getState();

   /**
    * Min zoomlevels which the maps supports
    */
   public static final int              MAP_MIN_ZOOM_LEVEL                             = 0;

   /**
    * Max zoomlevels which the maps supports
    */
   public static final int              MAP_MAX_ZOOM_LEVEL                             = 22;

   /**
    * These zoom levels are displayed in the UI therefore they start with 1 instead of 0
    */
   public static final int              UI_MIN_ZOOM_LEVEL                              = MAP_MIN_ZOOM_LEVEL + 1;
   public static final int              UI_MAX_ZOOM_LEVEL                              = MAP_MAX_ZOOM_LEVEL + 1;

   public static final int              EXPANDED_HOVER_SIZE                            = 20;
   public static final int              EXPANDED_HOVER_SIZE2                           = EXPANDED_HOVER_SIZE / 2;

   private static final String          DIRECTION_E                                    = "E";                             //$NON-NLS-1$
   private static final String          DIRECTION_N                                    = "N";                             //$NON-NLS-1$

   private static final int             TEXT_MARGIN                                    = 6;
   public static final int              MAP_POINT_BORDER                               = UI.IS_4K_DISPLAY ? 4 : 2;

   private static final String          GEO_GRID_ACTION_UPDATE_GEO_LOCATION_ZOOM_LEVEL = "\uE003";                        //$NON-NLS-1$

   /*
    * Wikipedia data
    */
// private static final String WIKI_PARAMETER_DIM  = "dim";  //$NON-NLS-1$
   private static final String WIKI_PARAMETER_TYPE = "type"; //$NON-NLS-1$

//   http://toolserver.org/~geohack/geohack.php?pagename=Sydney&language=de&params=33.85_S_151.2_E_region:AU-NSW_type:city(3641422)
//   http://toolserver.org/~geohack/geohack.php?pagename=Palm_Island,_Queensland&params=18_44_S_146_35_E_scale:20000_type:city
//   http://toolserver.org/~geohack/geohack.php?pagename=P%C3%B3voa_de_Varzim&params=41_22_57_N_8_46_45_W_region:PT_type:city//
//
//   where D is degrees, M is minutes, S is seconds, and NS/EWO are the directions
//
//   D;D
//   D_N_D_E
//   D_M_N_D_M_E
//   D_M_S_N_D_M_S_E

   private static final String PATTERN_SEPARATOR                          = "_";                               //$NON-NLS-1$
   private static final String PATTERN_END                                = "_?(.*)";                          //$NON-NLS-1$

   private static final String PATTERN_WIKI_URL                           = ".*pagename=([^&]*).*params=(.*)"; //$NON-NLS-1$
   private static final String PATTERN_WIKI_PARAMETER_KEY_VALUE_SEPARATOR = ":";                               //$NON-NLS-1$

   private static final String PATTERN_DOUBLE                             = "([-+]?[0-9]*\\.?[0-9]+)";         //$NON-NLS-1$
   private static final String PATTERN_DOUBLE_SEP                         = PATTERN_DOUBLE + PATTERN_SEPARATOR;

   private static final String PATTERN_DIRECTION_NS                       = "([NS])_";                         //$NON-NLS-1$
   private static final String PATTERN_DIRECTION_WE                       = "([WE])";                          //$NON-NLS-1$

//   private static final String      PATTERN_WIKI_POSITION_10               = "([-+]?[0-9]*\\.?[0-9]+)_([NS])_([-+]?[0-9]*\\.?[0-9]+)_([WE])_?(.*)";   //$NON-NLS-1$
//   private static final String      PATTERN_WIKI_POSITION_20               = "([0-9]*)_([NS])_([0-9]*)_([WE])_?(.*)";                           //$NON-NLS-1$
//   private static final String      PATTERN_WIKI_POSITION_21               = "([0-9]*)_([0-9]*)_([NS])_([0-9]*)_([0-9]*)_([WE])_?(.*)";            //$NON-NLS-1$
//   private static final String      PATTERN_WIKI_POSITION_22               = "([0-9]*)_([0-9]*)_([0-9]*)_([NS])_([0-9]*)_([0-9]*)_([0-9]*)_([WE])_?(.*)";   //$NON-NLS-1$

   private static final String           PATTERN_WIKI_POSITION_D_D             = PATTERN_DOUBLE + ";"                                        //$NON-NLS-1$
         + PATTERN_DOUBLE
         + PATTERN_END;

   private static final String           PATTERN_WIKI_POSITION_D_N_D_E         = PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final String           PATTERN_WIKI_POSITION_D_M_N_D_M_E     = PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final String           PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E = PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final Pattern          _patternWikiUrl                       = Pattern.compile(PATTERN_WIKI_URL);
   private static final Pattern          _patternWikiPosition_D_D              = Pattern.compile(PATTERN_WIKI_POSITION_D_D);
   private static final Pattern          _patternWikiPosition_D_N_D_E          = Pattern.compile(PATTERN_WIKI_POSITION_D_N_D_E);
   private static final Pattern          _patternWikiPosition_D_M_N_D_M_E      = Pattern.compile(PATTERN_WIKI_POSITION_D_M_N_D_M_E);
   private static final Pattern          _patternWikiPosition_D_M_S_N_D_M_S_E  = Pattern.compile(PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E);
   private static final Pattern          _patternWikiParamter                  = Pattern.compile(PATTERN_SEPARATOR);
   private static final Pattern          _patternWikiKeyValue                  = Pattern.compile(PATTERN_WIKI_PARAMETER_KEY_VALUE_SEPARATOR);

   private static final IPreferenceStore _prefStore                            = TourbookPlugin.getPrefStore();

   private static final ColorCacheSWT    _colorCache                           = new ColorCacheSWT();

   // [181,208,208] is the color of water in the standard OSM material
   public static final RGB               OSM_BACKGROUND_RGB         = new RGB(181, 208, 208);
   private static final RGB              MAP_DEFAULT_BACKGROUND_RGB = new RGB(0x40, 0x40, 0x40);

   private static final java.awt.Color   RATING_STAR_COLOR          = new java.awt.Color(250, 224, 0);
   private static final java.awt.Color   RATING_STAR_COLOR_BORDER   = new java.awt.Color(198, 178, 0);

   private static RGB                    _mapTransparentRGB;
   private static Color                  _mapTransparentColor;

   private IDialogSettings               _state_Map2;

   private Font                          _boldFontSWT               = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
   private Font                          _labelFontSWT;
   private String                        _labelFontName;
   private int                           _labelFontSize;

   private java.awt.Font                 _labelFontAWT              = UI.AWT_DIALOG_FONT;
   private java.awt.Font                 _clusterFontAWT;

   private Color                         _defaultBackgroundColor;

   /**
    * Map zoom level which is currently be used to display tiles. Normally a value between around 0
    * and 20.
    */
   private int                           _mapZoomLevel;

   private CenterMapBy                   _centerMapBy;

   /** This image contains the map which is painted in the map viewport */
   private Image                         _mapImage;

   /**
    * Indicates whether or not to draw the borders between tiles. Defaults to false. not very nice
    * looking, very much a product of testing Consider whether this should really be a property or
    * not.
    */
   private boolean                       _isShowDebug_TileInfo;
   private boolean                       _isShowDebug_TileBorder;
   private boolean                       _isShowDebug_GeoGrid;

   /**
    * Factory used by this component to grab the tiles necessary for painting the map.
    */
   private MP                            _mp;

   /**
    * The position in latitude/longitude of the "address" being mapped. This is a special coordinate
    * that, when moved, will cause the map to be moved as well. It is separate from "center" in that
    * "center" tracks the current center (in pixels) of the view port whereas this will not change
    * when panning or zooming. Whenever the commonLocation is changed, however, the map will be
    * repositioned.
    */
   private GeoPosition                   _commonLocation;

   /**
    * The overlay to delegate to for painting the "foreground" of the map component. This would
    * include painting waypoints, day/night, etc. also receives mouse events.
    */
   private final TourMapPainter          _mapPainter                = new TourMapPainter();

   private final TileImageLoaderCallback _tileImageLoaderCallback   = new TileImageLoaderCallback_ForTileImages();

   private Cursor                        _currentCursor;
   private final Cursor                  _cursorCross;
   private final Cursor                  _cursorDefault;
   private final Cursor                  _cursorHand;
   private final Cursor                  _cursorPan;
   private final Cursor                  _cursorPhoto_Move;
   private final Cursor                  _cursorPhoto_Select;
   private final Cursor                  _cursorSearchTour;
   private final Cursor                  _cursorSearchTour_Scroll;
   private final Cursor                  _cursorSelect;

   private final AtomicInteger           _redrawMapCounter          = new AtomicInteger();
   private final AtomicInteger           _overlayRunnableCounter    = new AtomicInteger();

   private boolean                       _canPanMap;
   private boolean                       _isMapPanned;

   private boolean                       _canPanPhoto;
   private boolean                       _isPhotoPanned;
   private Photo                         _pannedPhoto;

   private boolean                       _isMouseDown;
   private Point                         _mouseDownPosition;
   private GeoPosition                   _mouseDown_ContextMenu_GeoPosition;
   private int                           _mouseMove_DevPosition_X   = Integer.MIN_VALUE;
   private int                           _mouseMove_DevPosition_Y   = Integer.MIN_VALUE;
   private int                           _mouseMove_DevPosition_X_Last;
   private int                           _mouseMove_DevPosition_Y_Last;
   private GeoPosition                   _mouseMove_GeoPosition;

   private Thread                        _overlayThread;
   private long                          _nextOverlayRedrawTime;

   private Map2Config                    _mapConfig                 = Map2ConfigManager.getActiveConfig();

   private float                         _deviceScaling             = DPIUtil.getDeviceZoom() / 100f;

   /*
    * Map points
    */
   private boolean                         _isMapPointVisible;

   private final ExecutorService           _mapPointPainter_Executor        = createMapPoint_PainterThread();
   private Future<?>                       _mapPointPainter_Task;

   /**
    * The {@link #_mapPointPainter_Executor} is drawing into this image.
    */
   private Image                           _mapPointImage;

   /**
    * Cleanup images, they cannot be disposed in the UI thread otherwise there are tons of
    * exceptions when the map image is resized
    */
   private List<Image>                     _disposableMapPointImagesSWT     = new ArrayList<>();

   /**
    * It looks like that onResize() is not called very early from the swtbot to initialize this
    * field <a href=
    * "https://github.com/mytourbook/mytourbook/issues/1361#issuecomment-2166663604">https://github.com/mytourbook/mytourbook/issues/1361#issuecomment-2166663604</a>
    */
   private Rectangle                       _mapPointImageSize               = new Rectangle(0, 0, 400, 400);

   private int                             _mapPointPainter_LastCounter;
   private final AtomicInteger             _mapPointPainter_RunnableCounter = new AtomicInteger();
   private Rectangle                       _mapPointPainter_Viewport_WhenPainted;
   private Rectangle                       _mapPointPainter_Viewport_DuringPainting;
   private int                             _mapPointPainter_MicroAdjustment_DiffX;
   private int                             _mapPointPainter_MicroAdjustment_DiffY;

   private DistanceClustering<ClusterItem> _distanceClustering              = new DistanceClustering<>();
   private PointFeatureLabeler             _labelSpreader                   = new PointFeatureLabeler();
   private List<PaintedMapPoint>           _allPaintedClusterMarkers        = new ArrayList<>();
   private List<PaintedMapPoint>           _allPaintedCommonLocations       = new ArrayList<>();
   private List<PaintedMapPoint>           _allPaintedTourLocations         = new ArrayList<>();
   private List<PaintedMapPoint>           _allPaintedMarkers               = new ArrayList<>();
   private List<PaintedMarkerCluster>      _allPaintedMarkerClusters        = new ArrayList<>();
   private List<PaintedMapPoint>           _allPaintedPauses                = new ArrayList<>();
   private List<PaintedMapPoint>           _allPaintedPhotos                = new ArrayList<>();
   private List<PaintedMapPoint>           _allPaintedWayPoints             = new ArrayList<>();
   private final Set<String>               _allMapMarkerSkipLabels          = new HashSet<>();
   private final Map<String, Map2Point>    _allMapMarkerWithGroupedLabels   = new HashMap<>();
   private String                          _groupedMarkers;
   private List<TourLocation>              _allCommonLocations;
   private List<TourLocation>              _allTourLocations;

   private int                             _clusterSymbolBorder             = 10;
   private int                             _labelRespectMargin              = 1;
   private int                             _mapPointSymbolSize              = 5;
   private int                             _mapPointSymbolRespectSize       = _mapPointSymbolSize + 2;

   private PaintedMarkerCluster            _hoveredMarkerCluster;
   private PaintedMapPoint                 _hoveredMapPoint;
   private PaintedMapPoint                 _hoveredMapPoint_Previous;
   private boolean                         _isHoveredMapPointSymbol;
   private Photo                           _selectedPhoto;
   private PaintedMapPoint                 _selectedPhotoMapPoint;
   private boolean                         _isInHoveredRatingStar;
   private boolean                         _isMarkerClusterSelected;
   private MapPointToolTip                 _mapPointTooltip;
   private SlideoutMap2_PhotoImage         _mapPointTooltip_PhotoImage;
   private SlideoutMap2_PhotoHistogram     _mapPointTooltip_PhotoHistogram;
   private boolean                         _isPreloadHQImages;
   private boolean                         _isEnlargeSmallImages;
   private boolean                         _isShowHQPhotoImages;
   private boolean                         _isShowPhotoAdjustments;
   private final ILoadCallBack             _photoImageLoaderCallback        = new PhotoImageLoaderCallback();
   private final ILoadCallBack             _photoTooltipImageLoaderCallback = new PhotoTooltipImageLoaderCallback();

   /** Number of created map points */
   private int                             _numStatistics_AllCommonLocations;
   private int                             _numStatistics_AllTourMarkers;
   private int                             _numStatistics_AllTourLocations;
   private int                             _numStatistics_AllTourWayPoints;
   private int                             _numStatistics_AllTourPauses;
   private int                             _numStatistics_AllTourPhotos;

   private boolean                         _numStatistics_AllTourMarkers_IsTruncated;
   private boolean                         _numStatistics_AllTourWayPoints_IsTruncated;
   private boolean                         _numStatistics_AllTourPauses_IsTruncated;
   private boolean                         _numStatistics_AllTourPhotos_IsTruncated;

   private Map<Long, java.awt.Color>       _locationBoundingBoxColors       = new HashMap<>();
   private int                             _colorSwitchCounter;

   private final NumberFormat              _nf0;
   private final NumberFormat              _nf1;
   private final NumberFormat              _nf2;
   private final NumberFormat              _nf3;
   private final NumberFormat              _nfLatLon;
   {
      _nf0 = NumberFormat.getNumberInstance();
      _nf1 = NumberFormat.getNumberInstance();
      _nf2 = NumberFormat.getNumberInstance();
      _nf3 = NumberFormat.getNumberInstance();

      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);

      _nfLatLon = NumberFormat.getNumberInstance();
      _nfLatLon.setMinimumFractionDigits(4);
      _nfLatLon.setMaximumFractionDigits(4);
   }

   private final TextWrapPainter                      _textWrapper               = new TextWrapPainter();

   /**
    * cache for overlay images
    */
   private OverlayImageCache                          _overlayImageCache;

   /**
    * This queue contains tiles which overlay image must be painted
    */
   private final ConcurrentLinkedQueue<Tile>          _tileOverlayPaintQueue     = new ConcurrentLinkedQueue<>();

   private boolean                                    _isRunningDrawOverlay;

   private String                                     _overlayKey;

   /**
    * This painter is called when the map is painted in the onPaint event
    */
   private IDirectPainter                             _directMapPainter;

   private final DirectPainterContext                 _directMapPainterContext   = new DirectPainterContext();

   /**
    * When <code>true</code> the overlays are painted
    */
   private boolean                                    _isDrawOverlays;

   /**
    * Contains a legend which is painted in the map
    */
   private MapLegend                                  _mapLegend;

   private boolean                                    _isLegendVisible;

   /**
    * This is the most important point for the map because all operations depend on it.
    * <p>
    * Center position of the map viewport in <I>world pixel</I>. Dragging the map component will
    * change the center position. Zooming in/out will cause the center to be recalculated so as to
    * remain in the center of the new "map".
    * <p>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <br>
    * This MUST be in {@link Double} to be accurate when the map is zoomed<br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <br>
    */
   private Point2D                                    _worldPixel_MapCenter      = null;

   /**
    * Viewport in the map where the {@link #_mapImage} is painted <br>
    * <br>
    * <b>x</b> and <b>y</b> is the <b>top/left</b> corner in world pixel<br>
    * <b>width</b> and <b>height</b> contains the visible area in device pixel
    * <p>
    * <b>!!! x/y values can also be negative when the map is smaller than the viewport !!!</b>
    * <p>
    * I havn't yet fully understood how it works but I adjusted the map successfully in 10.7 and
    * tried to document this behaviour.
    */
   private Rectangle                                  _worldPixel_TopLeft_Viewport;

   /**
    * Size in device pixel where the map is displayed
    */
   private Rectangle                                  _devMapViewport;

   /**
    * Size of the map in tiles at the current zoom level {@link #_mapZoomLevel} (num tiles tall by
    * num tiles wide)
    */
   private Dimension                                  _mapTileSize;

   /**
    * Size of a tile in pixel (tile is quadratic)
    */
   private int                                        _tilePixelSize;

   /**
    * Size of a geo grid of 0.01 degree in pixel, this depends on the zoom level
    */
   private double                                     _devGridPixelSize_X;
   private double                                     _devGridPixelSize_Y;

   /**
    * Contains the client area of the map without trimmings, this rectangle has the width and height
    * of the map image
    */
   private Rectangle                                  _clientArea;

   private final ListenerList<IBreadcrumbListener>    _allBreadcrumbListener     = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IHoveredTourListener>   _allHoveredTourListeners   = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapGridListener>       _allMapGridListener        = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapInfoListener>       _allMapInfoListener        = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapPositionListener>   _allMapPositionListener    = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapSelectionListener>  _allMapSelectionListener   = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IGeoPositionListener>   _allMousePositionListeners = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IExternalAppListener>   _allExternalAppListeners   = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IPOIListener>           _allPOIListeners           = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<ITourSelectionListener> _allTourSelectionListener  = new ListenerList<>(ListenerList.IDENTITY);

   // measurement system
   private float   _distanceUnitValue = 1;
   private String  _distanceUnitLabel = UI.EMPTY_STRING;

   private boolean _isScaleVisible;
   private boolean _isShowTour;

   /*
    * POI image
    */
   private boolean         _isPoiVisible;
   private boolean         _isPoiPositionInViewport;
   //
   private final Image     _poiImage;
   private final Rectangle _poiImageBounds;
   private final Point     _poiImageDevPosition = new Point(0, 0);

   /*
    * POI tooltip
    */
   private PoiToolTip _poi_Tooltip;
   private final int  _poi_Tooltip_OffsetY = 5;

   /*
    * Tour tooltip
    */
   private TourToolTip        _tourTooltip;
   private HoveredAreaContext _tourTooltip_HoveredAreaContext;

   /*
    * Map locations
    */
   private MapLocationToolTip _mapLocation_Tooltip;

   /**
    * Hovered/selected tour
    */
   private boolean            _isShowHoveredOrSelectedTour       = Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT;

   private ArrayList<Point>   _allHoveredDevPoints               = new ArrayList<>();
   private ArrayList<Long>    _allHoveredTourIds                 = new ArrayList<>();
   private IntArrayList       _allHoveredSerieIndices            = new IntArrayList();

   private long               _hovered_SelectedTourId            = -1;
   private int                _hovered_SelectedSerieIndex_Front  = -1;
   private int                _hovered_SelectedSerieIndex_Behind = -1;

   /**
    * When <code>true</code> then a tour can be selected, otherwise a trackpoint (including tour)
    * can be selected
    */
   private boolean            _hoveredSelectedTour_CanSelectTour;
   private int                _hoveredSelectedTour_Hovered_Opacity;
   private Color              _hoveredSelectedTour_Hovered_Color;
   private int                _hoveredSelectedTour_HoveredAndSelected_Opacity;
   private Color              _hoveredSelectedTour_HoveredAndSelected_Color;
   private int                _hoveredSelectedTour_Selected_Opacity;
   private Color              _hoveredSelectedTour_Selected_Color;

   /**
    * When <code>true</code> the loading... image is not displayed
    */
   private boolean            _isLiveView;

   private long               _lastMapDrawTime;

   /*
    * All painted tiles in the map are within these 4 tile positions
    */
   private int           _tilePos_MinX;
   private int           _tilePos_MaxX;
   private int           _tilePos_MinY;
   private int           _tilePos_MaxY;
   //
   private Tile[][]      _allPaintedTiles;
   //
   private final Display _display;
   private final Thread  _displayThread;
   //

   /*
    * Download offline images
    */
   private boolean                   _offline_IsSelectingOfflineArea;
   private boolean                   _offline_IsOfflineSelectionStarted;
   private boolean                   _offline_IsPaintOfflineArea;

   private Point                     _offline_DevMouse_Start;
   private Point                     _offline_DevMouse_End;
   private Point                     _offline_DevTileStart;
   private Point                     _offline_DevTileEnd;

   private Point                     _offline_WorldMouse_Start;
   private Point                     _offline_WorldMouse_End;
   private Point                     _offline_WorldMouse_Move;

   private IMapContextMenuProvider   _mapContextMenuProvider;

   /**
    * Is <code>true</code> when the map context menu can be displayed
    */
   private boolean                   _isContextMenuEnabled      = true;

   private DropTarget                _dropTarget;

   private boolean                   _isMapPaintingEnabled      = true;

   private int                       _overlayAlpha              = 0xff;

   private MapGridData               _geoGrid_Data_Hovered;
   private MapGridData               _geoGrid_Data_Selected;
   private TourGeoFilter             _geoGrid_TourGeoFilter;

   private boolean                   _geoGrid_Action_IsHovered;
   private Rectangle                 _geoGrid_Action_Outline;
   private boolean                   _geoGrid_Label_IsHovered;
   private Rectangle                 _geoGrid_Label_Outline;

   private GeoPosition               _geoGrid_MapGeoCenter;
   private int                       _geoGrid_MapZoomLevel;
   private int[]                     _geoGrid_AutoScrollCounter = new int[1];

   private boolean                   _geoGrid_IsGridAutoScroll;

   private ActionManageOfflineImages _actionManageOfflineImages;

   private boolean                   _isMapBackgroundDark;
   private boolean                   _isFastMapPainting;
   private boolean                   _isFastMapPainting_Active;
   private boolean                   _isInInverseKeyboardPanning;

   /**
    * Tour direction is displayed only when tour is hovered
    */
   private boolean                   _isDrawTourDirection;

   /**
    * When <code>true</code> then the tour directions are displayed always but this is valid only,
    * when just one tour is displayed
    */
   private boolean                   _isDrawTourDirection_Always;

   /*
    * Direction arrows
    */
   private int                 _tourDirection_MarkerGap;
   private int                 _tourDirection_LineWidth;
   private RGB                 _tourDirection_RGB;
   private float               _tourDirection_SymbolSize;

   private int                 _fastMapPainting_skippedValues;

   private MapTourBreadcrumb   _tourBreadcrumb;
   private boolean             _isShowBreadcrumbs            = Map2View.STATE_IS_SHOW_BREADCRUMBS_DEFAULT;

   private int                 _prefOptions_BorderWidth;
   private boolean             _prefOptions_isCutOffLinesInPauses;
   private boolean             _prefOptions_IsDrawSquare;
   private int                 _prefOptions_LineWidth;
   private List<Long>          _allTourIds;

   private final BufferedImage _imageMapLocation_Tour;
   private final BufferedImage _imageMapLocation_Common;
   private final BufferedImage _imageMapLocation_TourStart;
   private final BufferedImage _imageMapLocation_TourEnd;
   private final BufferedImage _imageMapLocation_Hovered;
   private final BufferedImage _imageMapLocation_Disabled;
   private final BufferedImage _imageMapLocation_Disabled_Dark;

   private Point               _imageMapLocationBounds;

   private BufferedImage       _imageAnnotationCropped;
   private BufferedImage       _imageAnnotationTonality;
   private BufferedImage       _imageRatingStar;

   private int                 _ratingStarImageSize;
   private Rectangle           _paintedRatingStars;

   private final int           MAX_RATING_STARS              = 5;
   public int                  MAX_RATING_STARS_WIDTH;

   public int                  MAP_IMAGE_DEFAULT_SIZE_TINY   = 50;
   public int                  MAP_IMAGE_DEFAULT_SIZE_SMALL  = 100;
   public int                  MAP_IMAGE_DEFAULT_SIZE_MEDIUM = 200;
   public int                  MAP_IMAGE_DEFAULT_SIZE_LARGE  = 300;

   {
      final int deviceZoom = DPIUtil.getDeviceZoom();
      final float deviceScale = deviceZoom / 100f;

// SET_FORMATTING_OFF

      MAP_IMAGE_DEFAULT_SIZE_TINY   = (int) (MAP_IMAGE_DEFAULT_SIZE_TINY * deviceScale);
      MAP_IMAGE_DEFAULT_SIZE_SMALL  = (int) (MAP_IMAGE_DEFAULT_SIZE_SMALL * deviceScale);
      MAP_IMAGE_DEFAULT_SIZE_MEDIUM = (int) (MAP_IMAGE_DEFAULT_SIZE_MEDIUM * deviceScale);
      MAP_IMAGE_DEFAULT_SIZE_LARGE  = (int) (MAP_IMAGE_DEFAULT_SIZE_LARGE * deviceScale);

      _imageAnnotationCropped    = ImageConverter.convertIntoAWT(PhotoActivator.getImageDescriptor(PhotoImages.PhotoAnnotation_Cropped), deviceZoom);
      _imageAnnotationTonality   = ImageConverter.convertIntoAWT(PhotoActivator.getImageDescriptor(PhotoImages.PhotoAnnotation_Tonality), deviceZoom);
      _imageRatingStar           = ImageConverter.convertIntoAWT(PhotoActivator.getImageDescriptor(PhotoImages.PhotoRatingStar), deviceZoom);

// SET_FORMATTING_ON

      // rating star width and height are the same
      _ratingStarImageSize = _imageRatingStar.getWidth();

      MAX_RATING_STARS_WIDTH = _ratingStarImageSize * MAX_RATING_STARS;
   }

   private static enum HoveredPoint_PaintMode {

      IS_HOVERED, //
      IS_SELECTED, //
      IS_HOVERED_AND_SELECTED, //
   }

   private class PhotoImageLoaderCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         try {

            if (isUpdateUI) {

               // curves must be updated after the image is loading which is computing the curves
               _mapPointTooltip_PhotoHistogram.updateCurves();

               paint();
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      }
   }

   private class PhotoTooltipImageLoaderCallback implements ILoadCallBack {

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         try {

            if (isUpdateUI) {

               _mapPointTooltip_PhotoImage.onImageIsLoaded();
               _mapPointTooltip_PhotoHistogram.onImageIsLoaded();
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      }
   }

   /**
    * This callback is called when a tile image was loaded and is set into the tile
    */
   final class TileImageLoaderCallback_ForTileImages implements TileImageLoaderCallback {

      @Override
      public void update(final Tile tile) {

         if (tile.getZoom() == _mapZoomLevel) {

            /*
             * Because we are not in the UI thread, we have to queue the call for redraw and
             * cannot do it directly.
             */
            paint();
         }
      }
   }

   /**
    * Create a new Map
    *
    * @param state
    */
   public Map2(final Composite parent, final int style, final IDialogSettings state) {

      super(parent, style | SWT.DOUBLE_BUFFERED);

      _state_Map2 = state;

      _display = getDisplay();
      _displayThread = _display.getThread();

      addAllListener();
      addDropTarget();

      createActions();
      createContextMenu();

      updateGraphColors();
      updateMapOptions();
      updatePhotoOptions();

      grid_UpdatePaintingStateData();

// SET_FORMATTING_OFF

      _cursorCross                     = new Cursor(_display, SWT.CURSOR_CROSS);
      _cursorDefault                   = new Cursor(_display, SWT.CURSOR_ARROW);
      _cursorHand                      = new Cursor(_display, SWT.CURSOR_HAND);
      _cursorPan                       = new Cursor(_display, SWT.CURSOR_SIZEALL);

      _cursorPhoto_Move                = createCursorFromImage(Images.Cursor_Photo_Move);
      _cursorPhoto_Select              = createCursorFromImage(Images.Cursor_Photo_Select);
      _cursorSearchTour                = createCursorFromImage(Images.SearchTours_ByLocation);
      _cursorSearchTour_Scroll         = createCursorFromImage(Images.SearchTours_ByLocation_Scroll);
      _cursorSelect                    = createCursorFromImage(Images.Cursor_Select);

      _imageMapLocation_Common         = ImageUtils.createAWTImage(TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Common).createImage());
      _imageMapLocation_Tour           = ImageUtils.createAWTImage(TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Tour).createImage());
      _imageMapLocation_TourStart      = ImageUtils.createAWTImage(TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_TourStart).createImage());
      _imageMapLocation_TourEnd        = ImageUtils.createAWTImage(TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_TourEnd).createImage());

      _imageMapLocation_Disabled       = ImageUtils.createAWTImage(TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Disabled).createImage());
      _imageMapLocation_Disabled_Dark  = ImageUtils.createAWTImage(TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Disabled_Dark).createImage());
      _imageMapLocation_Hovered        = ImageUtils.createAWTImage(TourbookPlugin.getImageDescriptor(Images.MapLocationMarker_Hovered).createImage());

      _imageMapLocationBounds          = new Point(_imageMapLocation_Hovered.getWidth(), _imageMapLocation_Hovered.getHeight());

      _mapLocation_Tooltip             = new MapLocationToolTip(this);
      _mapPointTooltip                 = new MapPointToolTip(this);
      _mapPointTooltip_PhotoImage      = new SlideoutMap2_PhotoImage(this);
      _mapPointTooltip_PhotoHistogram  = new SlideoutMap2_PhotoHistogram(this);

      updateTooltips();

// SET_FORMATTING_ON

      _poiImage = TourbookPlugin.getImageDescriptor(Images.POI_InMap).createImage();
      _poiImageBounds = _poiImage.getBounds();

      _tourBreadcrumb = new MapTourBreadcrumb(this);

      paint_Overlay_0_SetupThread();

      parent.getDisplay().asyncExec(() -> {

         // must be run async because dark theme colors could not yet be initialized

         _defaultBackgroundColor = UI.IS_DARK_THEME
               ? ThemeUtil.getDarkestBackgroundColor()
               : new Color(MAP_DEFAULT_BACKGROUND_RGB);
      });
   }

   /**
    * @return Returns rgb values for the color which is used as transparent color in the map.
    */
   public static RGB getTransparentRGB() {

      if (_mapTransparentRGB == null) {

         updateTransparencyColor(Map2View.STATE_MAP_TRANSPARENCY_COLOR_DEFAULT);
      }

      return _mapTransparentRGB;
   }

   private static void updateTransparencyColor(final RGB transparentColor) {

      _mapTransparentRGB = transparentColor;
      _mapTransparentColor = new Color(transparentColor);
   }

   public void actionManageOfflineImages(final Event event) {

      // check if offline image is active
      final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
      if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE) == false) {

         MessageDialog.openInformation(
               _display.getActiveShell(),
               Messages.Dialog_OfflineArea_Error,
               Messages.Dialog_OfflineArea_Error_NoOffline);

         return;
      }

      // check if offline loading is running
      if (OfflineLoadManager.isLoading()) {

         MessageDialog.openInformation(
               _display.getActiveShell(),
               Messages.Dialog_OfflineArea_Error,
               Messages.Dialog_OfflineArea_Error_IsLoading);

         return;
      }

      _offline_IsPaintOfflineArea = true;
      _offline_IsSelectingOfflineArea = true;

      _offline_DevMouse_Start = null;
      _offline_DevMouse_End = null;

      setCursorOptimized(_cursorCross);

      redraw();
      paint();
   }

   public void actionSearchTourByLocation(final Event event) {

      _geoGrid_Data_Hovered = new MapGridData();

      // auto open geo filter slideout
      final boolean isAutoOpenSlideout = Util.getStateBoolean(TourGeoFilter_Manager.getState(),
            TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT,
            TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT_DEFAULT);
      if (isAutoOpenSlideout) {
         TourGeoFilter_Manager.setGeoFilter_OpenSlideout(true, false);
      }

      grid_UpdatePaintingStateData();
      _isFastMapPainting_Active = true;

      final Point worldMousePosition = new Point(
            _worldPixel_TopLeft_Viewport.x + _mouseMove_DevPosition_X,
            _worldPixel_TopLeft_Viewport.y + _mouseMove_DevPosition_Y);

      _geoGrid_Data_Hovered.geo_MouseMove = _mp.pixelToGeo(
            new Point2D.Double(worldMousePosition.x, worldMousePosition.y),
            _mapZoomLevel);

      setCursorOptimized(_cursorSearchTour);

      redraw();
      paint();
   }

   private void addAllListener() {

      addPaintListener(paintEvent -> onPaint(paintEvent));
      addDisposeListener(disposeEvent -> onDispose(disposeEvent));

      addFocusListener(new FocusListener() {
         @Override
         public void focusGained(final FocusEvent e) {
            updatePoiVisibility();
         }

         @Override
         public void focusLost(final FocusEvent e) {
// this is critical because the tool tip get's hidden when there are actions available in the tool tip shell
//            hidePoiToolTip();
         }
      });

      addMouseListener(new MouseListener() {

         @Override
         public void mouseDoubleClick(final MouseEvent event) {
            onMouse_DoubleClick(event);
         }

         @Override
         public void mouseDown(final MouseEvent event) {
            onMouse_Down(event);
         }

         @Override
         public void mouseUp(final MouseEvent event) {
            onMouse_Up(event);
         }
      });

      addMouseTrackListener(MouseTrackListener.mouseExitAdapter(mouseEvent -> onMouse_Exit()));
      addMouseMoveListener(mouseEvent -> onMouse_Move(mouseEvent));

      addListener(SWT.MouseHorizontalWheel, event -> onMouse_Wheel(event));
      addListener(SWT.MouseVerticalWheel, event -> onMouse_Wheel(event));

      addListener(SWT.KeyDown, event -> onKey_Down(event));

      addControlListener(ControlListener.controlResizedAdapter(controlEvent -> onResize()));

      // enable traverse keys
      addTraverseListener(traverseEvent -> traverseEvent.doit = true);
   }

   public void addBreadcrumbListener(final IBreadcrumbListener listener) {
      _allBreadcrumbListener.add(listener);
   }

   /**
    * Set map as drop target
    */
   private void addDropTarget() {

      _dropTarget = new DropTarget(this, DND.DROP_MOVE | DND.DROP_COPY);
      _dropTarget.setTransfer(URLTransfer.getInstance(), TextTransfer.getInstance());

      _dropTarget.addDropListener(new DropTargetAdapter() {
         @Override
         public void dragEnter(final DropTargetEvent event) {
            if ((event.detail == DND.DROP_DEFAULT) || (event.detail == DND.DROP_MOVE)) {
               event.detail = DND.DROP_COPY;
            }
         }

         @Override
         public void dragLeave(final DropTargetEvent event) {

         }

         @Override
         public void dragOver(final DropTargetEvent event) {
            if ((event.detail == DND.DROP_DEFAULT) || (event.detail == DND.DROP_MOVE)) {
               event.detail = DND.DROP_COPY;
            }
         }

         @Override
         public void drop(final DropTargetEvent event) {

            if (event.data == null) {
               event.detail = DND.DROP_NONE;
               return;
            }

            /*
             * run async to free the mouse cursor from the drop operation
             */
            _display.asyncExec(() -> onDropRunnable(event));
         }
      });
   }

   public void addExternalAppListener(final IExternalAppListener mapListener) {
      _allExternalAppListeners.add(mapListener);
   }

   public void addHoveredTourListener(final IHoveredTourListener hoveredTourListener) {
      _allHoveredTourListeners.add(hoveredTourListener);
   }

   public void addMapGridBoxListener(final IMapGridListener mapListener) {
      _allMapGridListener.add(mapListener);
   }

   public void addMapInfoListener(final IMapInfoListener mapInfoListener) {
      _allMapInfoListener.add(mapInfoListener);
   }

   public void addMapPositionListener(final IMapPositionListener listener) {
      _allMapPositionListener.add(listener);
   }

   public void addMapSelectionListener(final IMapSelectionListener listener) {
      _allMapSelectionListener.add(listener);
   }

   public void addMousePositionListener(final IGeoPositionListener mapListener) {
      _allMousePositionListeners.add(mapListener);
   }

   public void addPOIListener(final IPOIListener poiListener) {
      _allPOIListeners.add(poiListener);
   }

   public void addTourSelectionListener(final ITourSelectionListener tourSelectionListener) {
      _allTourSelectionListener.add(tourSelectionListener);
   }

   /**
    * Checks if an image can be reused, this is true if the image exists and has the same size
    *
    * @param image
    * @param newBounds
    *
    * @return
    */
   private boolean canReuseImage(final Image image, final Rectangle newBounds) {

      // check if we could reuse the existing image

      if (image == null || image.isDisposed()) {
         return false;
      }

      // image exist, check image bounds
      final Rectangle oldBounds = image.getBounds();

      if (oldBounds.width == newBounds.width && oldBounds.height == newBounds.height) {
         return true;
      }

      return false;
   }

   /**
    * Checks validation of a world pixel by using the current zoom level and map tile size.
    *
    * @param newWorldPixelCenter
    *
    * @return Returns adjusted world pixel when necessary.
    */
   private Point2D.Double checkWorldPixel(final Point2D newWorldPixelCenter) {

      final long maxWidth = _mapTileSize.width * _tilePixelSize;
      final long maxHeight = _mapTileSize.height * _tilePixelSize;

      double newCenterX = newWorldPixelCenter.getX();
      double newCenterY = newWorldPixelCenter.getY();

      if (newCenterX < 0) {
         newCenterX = -1;
      }
      if (newCenterX > maxWidth) {
         newCenterX = maxWidth + 1;
      }

      if (newCenterY < 0) {
         newCenterY = -1;
      }

      if (newCenterY > maxHeight) {
         newCenterY = maxHeight + 1;
      }

      return new Point2D.Double(newCenterX, newCenterY);
   }

   private Point2D.Double checkWorldPixel(final Point2D newWorldPixelCenter, final int zoomLevel) {

      final Dimension mapTileSize = _mp.getMapTileSize(zoomLevel);

      final long maxWidth = mapTileSize.width * _tilePixelSize;
      final long maxHeight = mapTileSize.height * _tilePixelSize;

      double newCenterX = newWorldPixelCenter.getX();
      double newCenterY = newWorldPixelCenter.getY();

      if (newCenterX < 0) {
         newCenterX = -1;
      }
      if (newCenterX > maxWidth) {
         newCenterX = maxWidth + 1;
      }

      if (newCenterY < 0) {
         newCenterY = -1;
      }

      if (newCenterY > maxHeight) {
         newCenterY = maxHeight + 1;
      }

      return new Point2D.Double(newCenterX, newCenterY);
   }

   @Override
   public org.eclipse.swt.graphics.Point computeSize(final int wHint, final int hHint, final boolean changed) {
      return getParent().getSize();
   }

   private void createActions() {

      _actionManageOfflineImages = new ActionManageOfflineImages(Map2.this);
   }

   private java.awt.Color createBoundingBoxColor() {

      int red = (int) (Math.random() * 255);
      int green = (int) (Math.random() * 255);
      int blue = (int) (Math.random() * 255);

      final float[] hsbValues = java.awt.Color.RGBtoHSB(red, green, blue, null);

      final float hue = hsbValues[0];
      final float saturation = hsbValues[1];
      float brightness = hsbValues[2];

      int adjustedRGB = Integer.MIN_VALUE;

      final float brightnessClipValue = 0.5f;
      final float darknessClipValue = 0.6f;

      if (_isMapBackgroundDark) {

         // background is dark -> ensure that a bright color is used

         if (brightness < brightnessClipValue) {

            brightness = brightnessClipValue;

            adjustedRGB = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
         }

      } else {

         // background is bright -> ensure that a darker color is used

         if (brightness > darknessClipValue) {

            brightness = darknessClipValue;

            adjustedRGB = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
         }
      }

      if (adjustedRGB != Integer.MIN_VALUE) {

         // brightness is adjusted

         final java.awt.Color adjustedColor = new java.awt.Color(adjustedRGB);

         red = adjustedColor.getRed();
         green = adjustedColor.getBlue();
         blue = adjustedColor.getBlue();
      }

      final java.awt.Color locationColor = new java.awt.Color(red, green, blue);

      return locationColor;
   }

   /**
    * create the context menu
    */
   private void createContextMenu() {

      final MenuManager menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);

      menuMgr.addMenuListener(menuManager -> {

         if ((_mp == null) || _isContextMenuEnabled == false) {
            return;
         }

         if (_mapContextMenuProvider != null) {
            _mapContextMenuProvider.fillContextMenu(menuManager, _actionManageOfflineImages);
         }
      });

      setMenu(menuMgr.createContextMenu(this));
   }

   private Cursor createCursorFromImage(final String imageName) {

      final String imageName4k = DPITools.get4kImageName(imageName);

      return UI.createCursorFromImage(TourbookPlugin.getImageDescriptor(imageName4k));
   }

   private void createLabelSpreaderLabels(final Graphics2D g2d,
                                          final Map2Point[] allMapPoints,
                                          final List<PointFeature> allCreatedLabels) {

      final FontMetrics fontMetrics = g2d.getFontMetrics();
      final int textHeight = fontMetrics.getHeight();

      // without margin the labels are too close
      final int margin = _labelRespectMargin;
      final int margin2x = margin * 2;

      for (final Map2Point mapPoint : allMapPoints) {

         final int devX = mapPoint.geoPointDevX;
         final int devY = mapPoint.geoPointDevY;

         final String pointLabel = mapPoint.getFormattedLabel();

         final int textWidth = fontMetrics.stringWidth(pointLabel);

         final PointFeature pointFeature = new PointFeature(

               null,
               -1,

               devX - margin,
               devY - margin,

               textWidth + margin2x,
               textHeight + margin2x);

         pointFeature.data = mapPoint;

         allCreatedLabels.add(pointFeature);
      }
   }

   private void createLabelSpreaderPhotos(final Graphics2D g2d,
                                          final Map2Point[] allMapPoints,
                                          final List<PointFeature> allCreatedItems) {

      // without a margin the images are too close
      final int margin = _labelRespectMargin;
      final int margin2x = margin * 2;

      for (final Map2Point mapPoint : allMapPoints) {

         final int devX = mapPoint.geoPointDevX;
         final int devY = mapPoint.geoPointDevY;

         final Photo photo = mapPoint.photo;
         final Point mapImageSize = photo.getMap2ImageSize(

               _isShowHQPhotoImages,
               _isShowPhotoAdjustments,
               _isEnlargeSmallImages);

         final PointFeature pointFeature = new PointFeature(

               null,
               -1,

               devX - margin,
               devY - margin,

               mapImageSize.x + margin2x,
               mapImageSize.y + margin2x);

         pointFeature.data = mapPoint;

         allCreatedItems.add(pointFeature);
      }
   }

   /**
    * Creates a new image, the old image is disposed
    *
    * @param display
    * @param oldImage
    *           image which will be disposed if the image is not null
    * @param imageSize
    * @param fillColor
    *
    * @return returns a new created image
    */
   private Image createMapImage(final Display display,
                                final Image oldImage,
                                final Rectangle imageSize,
                                final Color fillColor) {

      if (oldImage != null) {
         oldImage.dispose();
      }

      // ensure that the image has a minimum width/height of 1, otherwise this causes troubles
      final int width = Math.max(1, imageSize.width);
      final int height = Math.max(1, imageSize.height);

      final Image mapImage = new Image(display, width, height);

      if (fillColor != null) {

         final GC gc = new GC(mapImage);
         {
            gc.setBackground(fillColor);
            gc.fillRectangle(imageSize);
         }
         gc.dispose();
      }

      return mapImage;
   }

   private ExecutorService createMapPoint_PainterThread() {

      final ThreadFactory threadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "2D Map - Map Point Image Painter");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      return Executors.newSingleThreadExecutor(threadFactory);
   }

   private void createMapPoints_Locations_10_FromTourData(final List<TourData> allTourData,
                                                          final List<Map2Point> allMapPoints) {

      final HashMap<TourLocation, Map2Point> allTourLocationsMap = new HashMap<>();

      for (final TourData tourData : allTourData) {

         if (isMapPointPainterInterrupted()) {
            break;
         }

         if (tourData == null || tourData.isLatLonAvailable() == false) {
            continue;
         }

         final List<TourLocation> allTempTourLocations = new ArrayList<>();
         final TourLocation tourLocationStart = tourData.getTourLocationStart();
         final TourLocation tourLocationEnd = tourData.getTourLocationEnd();

         boolean hasStartLocation = false;

         if (tourLocationStart != null) {
            hasStartLocation = true;
            allTempTourLocations.add(tourLocationStart);
         }

         if (tourLocationEnd != null) {
            allTempTourLocations.add(tourLocationEnd);
         }

         if (allTempTourLocations.size() == 0) {
            continue;
         }

         for (int locationIndex = 0; locationIndex < allTempTourLocations.size(); locationIndex++) {

            final TourLocation tourLocation = allTempTourLocations.get(locationIndex);

            final boolean isStartLocation = hasStartLocation && locationIndex == 0;

            createMapPoints_Locations_30_TourLocation(tourLocation,

                  isStartLocation
                        ? LocationType.TourStart
                        : LocationType.TourEnd,

                  allTourLocationsMap);
         }
      }

      allMapPoints.addAll(allTourLocationsMap.values());
   }

   private void createMapPoints_Locations_20_FromTourLocations(final List<TourLocation> allTourLocations,
                                                               final List<Map2Point> allMapPoints) {

      if (allTourLocations == null) {
         return;
      }

      final HashMap<TourLocation, Map2Point> allTourLocationsMap = new HashMap<>();

      for (final TourLocation tourLocation : allTourLocations) {

         if (isMapPointPainterInterrupted()) {
            break;
         }

         createMapPoints_Locations_30_TourLocation(tourLocation, LocationType.Tour, allTourLocationsMap);
      }

      allMapPoints.addAll(allTourLocationsMap.values());
   }

   private void createMapPoints_Locations_30_TourLocation(final TourLocation tourLocation,
                                                          final LocationType locationType,
                                                          final HashMap<TourLocation, Map2Point> allTourLocationsMap) {

      final boolean isTruncateLabel = _mapConfig.isTruncateLabel;
      final int labelTruncateLength = _mapConfig.labelTruncateLength;

      final Rectangle worldPixel_Viewport = _mapPointPainter_Viewport_DuringPainting;

      /*
       * Check if location is visible
       */
      final double latitude = tourLocation.latitude;
      final double longitude = tourLocation.longitude;

      // convert location lat/long into world pixels
      final java.awt.Point worldPixel_LocationPos = _mp.geoToPixel(new GeoPosition(latitude, longitude), _mapZoomLevel);

      final int worldPixel_LocationPosX = worldPixel_LocationPos.x;
      final int worldPixel_LocationPosY = worldPixel_LocationPos.y;

      final boolean isLocationInViewport = worldPixel_Viewport.contains(worldPixel_LocationPosX, worldPixel_LocationPosY);

      if (isLocationInViewport == false) {
         return;
      }

      // convert world position into device position
      final int devX = worldPixel_LocationPosX - worldPixel_Viewport.x;
      final int devY = worldPixel_LocationPosY - worldPixel_Viewport.y;

      String locationLabel = tourLocation.getMapName();

      /*
       * Check if location is a duplicate
       */
      final Map2Point existingMap2Location = allTourLocationsMap.get(tourLocation);
      if (existingMap2Location != null) {

         // this location is a duplicate

         if (locationType.equals(LocationType.TourStart)) {

            existingMap2Location.numDuplicates_Start++;

         } else if (locationType.equals(LocationType.TourEnd)) {

            existingMap2Location.numDuplicates_End++;
         }

         return;
      }

      /*
       * Create map point
       */

      // create formatted label
      if (isTruncateLabel && locationLabel.length() > labelTruncateLength) {

         if (labelTruncateLength == 0) {

            locationLabel = UI.SYMBOL_DOT;

         } else {

            locationLabel = locationLabel.substring(0, labelTruncateLength) + UI.SYMBOL_ELLIPSIS;
         }
      }

      final Map2Point mapPoint = new Map2Point(MapPointType.TOUR_LOCATION, new GeoPoint(latitude, longitude));

      mapPoint.tourLocation = tourLocation;
      mapPoint.locationType = LocationType.Tour;

      mapPoint.geoPointDevX = (int) (devX * _deviceScaling);
      mapPoint.geoPointDevY = (int) (devY * _deviceScaling);
      mapPoint.setFormattedLabel(locationLabel);

      // update and keep skipped labels

      if (locationType.equals(LocationType.TourStart)) {

         mapPoint.numDuplicates_Start++;

      } else if (locationType.equals(LocationType.TourEnd)) {

         mapPoint.numDuplicates_End++;
      }

      if (_mapConfig.isShowLocationBoundingBox) {
         createMapPoints_Locations_90_BoundingBox(tourLocation, mapPoint);
      }

      allTourLocationsMap.put(tourLocation, mapPoint);

      _numStatistics_AllTourLocations++;
   }

   private void createMapPoints_Locations_50_FromCommonLocations(final List<TourLocation> allCommonLocations,
                                                                 final List<Map2Point> allMapPoints) {

      if (allCommonLocations == null || allCommonLocations.size() == 0) {
         return;
      }

      final boolean isTruncateLabel = _mapConfig.isTruncateLabel;
      final int labelTruncateLength = _mapConfig.labelTruncateLength;

      final Rectangle worldPixel_Viewport = _mapPointPainter_Viewport_DuringPainting;

      for (final TourLocation tourLocation : allCommonLocations) {

         if (isMapPointPainterInterrupted()) {
            break;
         }

         /*
          * Check if location is visible
          */
         final double latitude = tourLocation.latitude;
         final double longitude = tourLocation.longitude;

         // convert location lat/long into world pixels
         final java.awt.Point worldPixel_LocationPos = _mp.geoToPixel(new GeoPosition(latitude, longitude), _mapZoomLevel);

         final int worldPixel_LocationPosX = worldPixel_LocationPos.x;
         final int worldPixel_LocationPosY = worldPixel_LocationPos.y;

         final boolean isLocationInViewport = worldPixel_Viewport.contains(worldPixel_LocationPosX, worldPixel_LocationPosY);

         if (isLocationInViewport == false) {
            continue;
         }

         // convert world position into device position
         final int devX = worldPixel_LocationPosX - worldPixel_Viewport.x;
         final int devY = worldPixel_LocationPosY - worldPixel_Viewport.y;

         String locationLabel = tourLocation.getMapName();

         // create formatted label
         if (isTruncateLabel && locationLabel.length() > labelTruncateLength) {

            if (labelTruncateLength == 0) {

               locationLabel = UI.SYMBOL_DOT;

            } else {

               locationLabel = locationLabel.substring(0, labelTruncateLength) + UI.SYMBOL_ELLIPSIS;
            }
         }

         /*
          * Create map point
          */

         final Map2Point mapPoint = new Map2Point(MapPointType.COMMON_LOCATION, new GeoPoint(latitude, longitude));

         mapPoint.tourLocation = tourLocation;
         mapPoint.locationType = LocationType.Common;

         mapPoint.geoPointDevX = (int) (devX * _deviceScaling);
         mapPoint.geoPointDevY = (int) (devY * _deviceScaling);

         mapPoint.setFormattedLabel(locationLabel);

         if (_mapConfig.isShowLocationBoundingBox) {
            createMapPoints_Locations_90_BoundingBox(tourLocation, mapPoint);
         }

         allMapPoints.add(mapPoint);

         _numStatistics_AllCommonLocations++;
      }
   }

   private void createMapPoints_Locations_90_BoundingBox(final TourLocation tourLocation,
                                                         final Map2Point mapPoints) {

      final int viewportX = _worldPixel_TopLeft_Viewport.x;
      final int viewportY = _worldPixel_TopLeft_Viewport.y;

      // create original bbox

      final double latitudeMin = tourLocation.latitudeMin;
      final double latitudeMax = tourLocation.latitudeMax;
      final double longitudeMin = tourLocation.longitudeMin;
      final double longitudeMax = tourLocation.longitudeMax;

      // convert location lat/long into world pixels

      final java.awt.Point providedBBox_TopLeft = _mp.geoToPixel(new GeoPosition(latitudeMax, longitudeMin), _mapZoomLevel);
      final java.awt.Point providedBBox_TopRight = _mp.geoToPixel(new GeoPosition(latitudeMax, longitudeMax), _mapZoomLevel);
      final java.awt.Point providedBBox_BottomLeft = _mp.geoToPixel(new GeoPosition(latitudeMin, longitudeMin), _mapZoomLevel);

      final int bboxTopLeft_DevX = providedBBox_TopLeft.x - viewportX;
      final int bboxTopRight_DevX = providedBBox_TopRight.x - viewportX;

      final int bboxTopLeft_DevY = providedBBox_TopLeft.y - viewportY;
      final int bboxBottomLeft_DevY = providedBBox_BottomLeft.y - viewportY;

      final int bboxWidth = bboxTopRight_DevX - bboxTopLeft_DevX;
      final int bboxHeight = bboxBottomLeft_DevY - bboxTopLeft_DevY;

      mapPoints.boundingBoxAWT = new java.awt.Rectangle(

            (int) (bboxTopLeft_DevX * _deviceScaling),
            (int) (bboxTopLeft_DevY * _deviceScaling),
            (int) (bboxWidth * _deviceScaling),
            (int) (bboxHeight * _deviceScaling));

      mapPoints.boundingBoxSWT = new Rectangle(

            bboxTopLeft_DevX,
            bboxTopLeft_DevY,
            bboxWidth,
            bboxHeight);

      final double latitudeMin_Resized = tourLocation.latitudeMin_Resized;
      final double latitudeMax_Resized = tourLocation.latitudeMax_Resized;
      final double longitudeMin_Resized = tourLocation.longitudeMin_Resized;
      final double longitudeMax_Resized = tourLocation.longitudeMax_Resized;

      final boolean isBBoxResized = false

            || latitudeMin != latitudeMin_Resized
            || latitudeMax != latitudeMax_Resized

            || longitudeMin != longitudeMin_Resized
            || longitudeMax != longitudeMax_Resized;

      if (isBBoxResized) {

         // draw resized bbox

// SET_FORMATTING_OFF

         final java.awt.Point providedBBox_TopLeft_Resized     = _mp.geoToPixel(new GeoPosition(latitudeMax_Resized, longitudeMin_Resized), _mapZoomLevel);
         final java.awt.Point providedBBox_TopRight_Resized    = _mp.geoToPixel(new GeoPosition(latitudeMax_Resized, longitudeMax_Resized), _mapZoomLevel);
         final java.awt.Point providedBBox_BottomLeft_Resized  = _mp.geoToPixel(new GeoPosition(latitudeMin_Resized, longitudeMin_Resized), _mapZoomLevel);

         final int bboxTopLeft_DevX_Resized     = providedBBox_TopLeft_Resized.x - viewportX;
         final int bboxTopRight_DevX_Resized    = providedBBox_TopRight_Resized.x - viewportX;
         final int bboxTopLeft_DevY_Resized     = providedBBox_TopLeft_Resized.y - viewportY;
         final int bboxBottomLeft_DevY_Resized  = providedBBox_BottomLeft_Resized.y - viewportY;

         final int bboxWidth_Resized            = bboxTopRight_DevX_Resized - bboxTopLeft_DevX_Resized;
         final int bboxHeight_Resized           = bboxBottomLeft_DevY_Resized - bboxTopLeft_DevY_Resized;

// SET_FORMATTING_ON

         mapPoints.boundingBox_ResizedAWT = new java.awt.Rectangle(

               (int) (bboxTopLeft_DevX_Resized * _deviceScaling),
               (int) (bboxTopLeft_DevY_Resized * _deviceScaling),
               (int) (bboxWidth_Resized * _deviceScaling),
               (int) (bboxHeight_Resized * _deviceScaling)

         );

         mapPoints.boundingBox_ResizedSWT = new Rectangle(

               bboxTopLeft_DevX_Resized,
               bboxTopLeft_DevY_Resized,
               bboxWidth_Resized,
               bboxHeight_Resized

         );
      }

      /*
       * Paint each bbox with a different color but use the same color for the same bbox
       */
      final long bboxKey = tourLocation.boundingBoxKey;

      java.awt.Color boundingBoxColor = _locationBoundingBoxColors.get(bboxKey);

      if (boundingBoxColor == null) {

         // create bbox color

         boundingBoxColor = createBoundingBoxColor();

         _locationBoundingBoxColors.put(bboxKey, boundingBoxColor);
      }

      mapPoints.boundingBox_ColorAWT = boundingBoxColor;
   }

   /**
    * Create {@link Map2Point} wrapper for {@link TourMarker}
    *
    * @param allTourData
    * @param allMapMarkers2
    *
    * @return Returns a list with all markers which are visible in the map viewport
    */
   private void createMapPoints_TourMarkers(final List<TourData> allTourData, final List<Map2Point> allMapPoints) {

      final boolean isGroupDuplicatedMarkers = _mapConfig.isGroupDuplicatedMarkers;

      if (isGroupDuplicatedMarkers) {
         setupGroupedLabels();
      }

      _allMapMarkerWithGroupedLabels.clear();

      LongHashSet markerFilter_typeIDMap = null;
      final boolean isFilterTourMarkers = _mapConfig.isFilterTourMarkers;
      if (isFilterTourMarkers) {

         final long[] tourMarkerFilter = _mapConfig.tourMarkerFilter;

         if (tourMarkerFilter != null) {

            markerFilter_typeIDMap = new LongHashSet(tourMarkerFilter);
         }
      }

      final Rectangle worldPixel_Viewport = _mapPointPainter_Viewport_DuringPainting;

      final List<TourMarker> allFilteredMakerList = new ArrayList<>();

      for (final TourData tourData : allTourData) {

         if (isMapPointPainterInterrupted()) {
            break;
         }

         if (tourData == null || tourData.isLatLonAvailable() == false) {
            continue;
         }

         final Set<TourMarker> allTourMarkers = tourData.getTourMarkers();

         if (allTourMarkers.isEmpty()) {
            continue;
         }

         for (final TourMarker tourMarker : allTourMarkers) {

            // skip marker when hidden or not set
            if (tourMarker.isMarkerVisible() == false || tourMarker.getLabel().length() == 0) {
               continue;
            }

            // skip marker when filtered out
            if (isFilterTourMarkers) {

               // tour markers are filtered by marker type

               final TourMarkerType tourMarkerType = tourMarker.getTourMarkerType();

               if (tourMarkerType != null) {

                  if (markerFilter_typeIDMap.contains(tourMarkerType.getId()) == false) {

                     continue;
                  }
               }
            }

            /*
             * Check if marker is visible
             */
            java.awt.Point markerWorldPixelPosition = tourMarker.getWorldPixelPosition(_mapZoomLevel);

            if (markerWorldPixelPosition == null) {

               // convert marker lat/long into world pixels

               markerWorldPixelPosition = _mp.geoToPixel(

                     new GeoPosition(tourMarker.getLatitude(), tourMarker.getLongitude()),
                     _mapZoomLevel);

               tourMarker.setWorldPixelPosition(markerWorldPixelPosition, _mapZoomLevel);
            }

            final int markerWorldPixelX = markerWorldPixelPosition.x;
            final int markerWorldPixelY = markerWorldPixelPosition.y;

            final boolean isMarkerInViewport = worldPixel_Viewport.contains(markerWorldPixelX, markerWorldPixelY);

            if (isMarkerInViewport) {

               allFilteredMakerList.add(tourMarker);
            }
         }
      }

      final boolean isTruncateLabel = _mapConfig.isTruncateLabel;
      final int labelTruncateLength = _mapConfig.labelTruncateLength;
      final int skipLabelGridSize = _mapConfig.groupGridSize;

      float numAllRemainingItems = _mapConfig.labelDistributorMaxLabels;

      final int numMarkers = allFilteredMakerList.size();

      final float subMarkerItems = numMarkers / numAllRemainingItems;

      for (int markerIndex = 0; markerIndex < numMarkers; markerIndex++) {

         int markerSubIndex = markerIndex;

         if (subMarkerItems > 1) {

            // there are more pauses than visible pauses

            final float nextItemIndex = subMarkerItems * markerIndex;
            final double randomDiff = Math.random() * subMarkerItems;

            markerSubIndex = (int) (nextItemIndex + randomDiff);

            _numStatistics_AllTourMarkers_IsTruncated = true;
         }

         // check bounds
         if (markerSubIndex >= numMarkers) {
            break;
         }

         final TourMarker tourMarker = allFilteredMakerList.get(markerSubIndex);

         final java.awt.Point markerWorldPixelPosition = tourMarker.getWorldPixelPosition(_mapZoomLevel);

         if (markerWorldPixelPosition == null) {
            continue;
         }

         final int markerWorldPixelX = markerWorldPixelPosition.x;
         final int markerWorldPixelY = markerWorldPixelPosition.y;

         // convert world position into device position
         final int devX = markerWorldPixelX - worldPixel_Viewport.x;
         final int devY = markerWorldPixelY - worldPixel_Viewport.y;

         String markerText = tourMarker.getMarkerMapLabel();
         String groupKey = null;

         /*
          * Check if marker is a duplicate
          */
         if (isGroupDuplicatedMarkers && _allMapMarkerSkipLabels.size() > 0) {

            if (_allMapMarkerSkipLabels.contains(markerText)) {

               // this label is marked to be grouped

               final int groupX = devX / skipLabelGridSize;
               final int groupY = devY / skipLabelGridSize;

               groupKey = markerText + UI.DASH + groupX + UI.DASH + groupY;

               final Map2Point groupedMarker = _allMapMarkerWithGroupedLabels.get(groupKey);

               if (groupedMarker != null) {

                  // skip marker label but update number of skipped labels

                  groupedMarker.numDuplicates++;

                  continue;
               }
            }
         }

         // create formatted label
         if (isTruncateLabel && markerText.length() > labelTruncateLength) {

            // keep star at the end
            final String endSymbol = markerText.endsWith(UI.SYMBOL_STAR)
                  ? UI.SYMBOL_STAR
                  : UI.EMPTY_STRING;

            if (labelTruncateLength == 0) {

               markerText = UI.SYMBOL_DOT + endSymbol;

            } else {

               markerText = markerText.substring(0, labelTruncateLength)

                     + UI.SYMBOL_ELLIPSIS

                     + endSymbol;
            }
         }

         final MapTourMarkerTime tourMarkerDateTimeFormat = _mapConfig.tourMarkerDateTimeFormat;
         if (tourMarkerDateTimeFormat.equals(MapTourMarkerTime.NONE) == false) {

            // append time stamp

            final TourData tourData = tourMarker.getTourData();
            final ZonedDateTime tourStartTime = tourData.getTourStartTime();
            final ZonedDateTime markerStartTime = TimeTools.getZonedDateTime(
                  tourMarker.getTourTime(),
                  tourStartTime.getZone());

// SET_FORMATTING_OFF

            switch (tourMarkerDateTimeFormat) {
            case DATE:           markerText += UI.SPACE + TimeTools.Formatter_Date_S      .format(markerStartTime);  break;
            case DATE_NO_YEAR:   markerText += UI.SPACE + TimeTools.Formatter_Date_NoYear .format(markerStartTime);  break;
            case TIME:           markerText += UI.SPACE + TimeTools.Formatter_Time_S      .format(markerStartTime);  break;
            case DATE_TIME:      markerText += UI.SPACE + TimeTools.Formatter_DateTime_S  .format(markerStartTime);  break;
            case NONE:
            default:
            }
// SET_FORMATTING_ON
         }

         /*
          * Create map point
          */
         final Map2Point mapPoint = new Map2Point(

               MapPointType.TOUR_MARKER,
               new GeoPoint(tourMarker.getLatitude(), tourMarker.getLongitude()));

         mapPoint.tourMarker = tourMarker;

         mapPoint.geoPointDevX = (int) (devX * _deviceScaling);
         mapPoint.geoPointDevY = (int) (devY * _deviceScaling);
         mapPoint.setFormattedLabel(markerText);

         if (groupKey != null) {

            // update and keep skipped labels

            mapPoint.numDuplicates++;

            _allMapMarkerWithGroupedLabels.put(groupKey, mapPoint);
         }

         allMapPoints.add(mapPoint);

         _numStatistics_AllTourMarkers++;

         if (numAllRemainingItems-- <= 0) {
            break;
         }
      }
   }

   private void createMapPoints_TourPauses(final List<TourData> allTourData, final List<Map2Point> allMapPoints) {

      final Rectangle worldPixel_Viewport = _mapPointPainter_Viewport_DuringPainting;

      final List<TourPause> allFilteredPausesList = new ArrayList<>();

      final int numAllTours = allTourData.size();

      for (int tourIndex = 0; tourIndex < numAllTours; tourIndex++) {

         if (isMapPointPainterInterrupted()) {
            break;
         }

         final TourData tourData = allTourData.get(tourIndex);

         // check if geo positions are available
         if (tourData == null || tourData.isLatLonAvailable() == false) {
            continue;
         }

         // check if tour is visible
         final Rectangle tourWorldPixelBounds = tourData.getWorldPixelBounds(_mp, _mapZoomLevel);
         if (worldPixel_Viewport.intersects(tourWorldPixelBounds) == false) {
            continue;
         }

         // check if pauses are available
         final long[] pausedTime_Start = tourData.getPausedTime_Start();
         if (pausedTime_Start == null || pausedTime_Start.length == 0) {
            continue;
         }

         final int[] timeSerie = tourData.timeSerie;
         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;

         final long tourStartTimeMS = tourData.getTourStartTimeMS();

         int serieIndex = 0;
         final List<TourPause> allTourPauses = tourData.getTourPauses();

         for (int pauseIndex = 0; pauseIndex < allTourPauses.size(); pauseIndex++) {

            final TourPause tourPause = allTourPauses.get(pauseIndex);

            if (isTourPauseVisible(tourPause.duration, tourPause.isAutoPause)) {

               // pause is not filtered by the duration

               java.awt.Point pauseWorldpixelPosition = tourPause.getWorldPixelPosition(_mapZoomLevel);

               if (pauseWorldpixelPosition == null) {

                  // create world position

                  final long startTime = pausedTime_Start[pauseIndex];

                  for (int timeSerieIndex = serieIndex; timeSerieIndex < timeSerie.length; ++timeSerieIndex) {

                     final long currentTime = timeSerie[timeSerieIndex] * 1000L + tourStartTimeMS;

                     if (currentTime >= startTime) {
                        serieIndex = timeSerieIndex;
                        break;
                     }
                  }

                  // convert lat/lon into world pixels
                  final double latitude = latitudeSerie[serieIndex];
                  final double longitude = longitudeSerie[serieIndex];
                  final GeoPosition geoPosition = new GeoPosition(latitude, longitude);

                  pauseWorldpixelPosition = _mp.geoToPixel(geoPosition, _mapZoomLevel);

                  tourPause.setPosition(geoPosition, pauseWorldpixelPosition, _mapZoomLevel);
               }

               final boolean isPauseInViewport = worldPixel_Viewport.contains(pauseWorldpixelPosition.x, pauseWorldpixelPosition.y);

               if (isPauseInViewport) {

                  allFilteredPausesList.add(tourPause);
               }
            }
         }
      }

      /*
       * Create pause points for all visible pauses
       */
      final TourPause[] allFilteredPauses = allFilteredPausesList.toArray(new TourPause[allFilteredPausesList.size()]);

      final float numPauses = allFilteredPauses.length;
      float numAllRemainingItems = _mapConfig.labelDistributorMaxLabels;

      final float subPauseItems = numPauses / numAllRemainingItems;

      for (int pauseIndex = 0; pauseIndex < numPauses; pauseIndex++) {

         int pauseSubIndex = pauseIndex;

         if (subPauseItems > 1) {

            // there are more pauses than visible pauses

            final float nextItemIndex = subPauseItems * pauseIndex;
            final double randomDiff = Math.random() * subPauseItems;

            pauseSubIndex = (int) (nextItemIndex + randomDiff);

            _numStatistics_AllTourPauses_IsTruncated = true;
         }

         // check bounds
         if (pauseSubIndex >= numPauses) {
            break;
         }

         final TourPause tourPause = allFilteredPauses[pauseSubIndex];

         // convert world position into device position
         final java.awt.Point pauseWorldPixelPosition = tourPause.getWorldPixelPosition(_mapZoomLevel);

         if (pauseWorldPixelPosition == null) {

            // this happend but it should not

            continue;
         }

         final int devX = pauseWorldPixelPosition.x - worldPixel_Viewport.x;
         final int devY = pauseWorldPixelPosition.y - worldPixel_Viewport.y;

         final GeoPosition geoPosition = tourPause.geoPosition;

         /*
          * Create map point
          */
         final Map2Point mapPoint = new Map2Point(

               MapPointType.TOUR_PAUSE,
               new GeoPoint(geoPosition.latitude, geoPosition.longitude));

         mapPoint.geoPointDevX = (int) (devX * _deviceScaling);
         mapPoint.geoPointDevY = (int) (devY * _deviceScaling);

         mapPoint.tourPause = tourPause;

         mapPoint.setFormattedLabel(UI.format_hh_mm_ss(tourPause.duration));

         allMapPoints.add(mapPoint);

         _numStatistics_AllTourPauses++;

         if (numAllRemainingItems-- <= 0) {
            break;
         }
      }
   }

   private void createMapPoints_TourPhotos(final List<Photo> allPhotos, final List<Map2Point> allMapPoints) {

      // clone list to prevent concurrency exceptions, this happened
      final List<Photo> allPhotosCloned = new ArrayList<>(allPhotos);

      final Rectangle worldPixel_Viewport = _mapPointPainter_Viewport_DuringPainting;

      final boolean isLinkPhotoDisplayed = Map2PainterConfig.isLinkPhotoDisplayed;

      // world positions are cached to optimize performance
      final int projectionHash = _mp.getProjection().getId().hashCode();

      final List<Photo> allVisiblePhotos = new ArrayList<>();
      final List<java.awt.Point> allWorldPixel = new ArrayList<>();

      final int numPhotos = allPhotosCloned.size();

      for (int photoIndex = 0; photoIndex < numPhotos; photoIndex++) {

         final Photo photo = allPhotosCloned.get(photoIndex);

         // keep serie indices for the photos
         photo.photoIndex = photoIndex;

         final java.awt.Point photoWorldPixel = photo.getWorldPosition(
               _mp,
               projectionHash,
               _mapZoomLevel,
               isLinkPhotoDisplayed);

         if (photoWorldPixel == null) {
            continue;
         }

         final boolean isPhotoInViewport = worldPixel_Viewport.contains(photoWorldPixel.x, photoWorldPixel.y);
         if (isPhotoInViewport) {

            allVisiblePhotos.add(photo);
            allWorldPixel.add(photoWorldPixel);
         }
      }

      /*
       * Create map points
       */
      final float numVisiblePhotos = allVisiblePhotos.size();
      float numAllRemainingItems = _mapConfig.labelDistributorMaxLabels;

      final float subPhotoItems = numVisiblePhotos / numAllRemainingItems;

      for (int photoIndex = 0; photoIndex < numVisiblePhotos; photoIndex++) {

         /*
          * Skip photos when there are too many
          */
         int photoSubIndex = photoIndex;

         if (subPhotoItems > 1) {

            // there are more photos than visible photos

            final float nextItemIndex = subPhotoItems * photoIndex;
            final double randomDiff = Math.random() * subPhotoItems;

            photoSubIndex = (int) (nextItemIndex + randomDiff);

            _numStatistics_AllTourPhotos_IsTruncated = true;
         }

         // check bounds
         if (photoSubIndex >= numVisiblePhotos) {
            break;
         }

         final Photo photo = allVisiblePhotos.get(photoSubIndex);
         final java.awt.Point photoWorldPixel = allWorldPixel.get(photoSubIndex);

         // convert world position into device position
         final int devXPhoto = photoWorldPixel.x - worldPixel_Viewport.x;
         final int devYPhoto = photoWorldPixel.y - worldPixel_Viewport.y;

         final double latitude = isLinkPhotoDisplayed
               ? photo.getLinkLatitude()
               : photo.getTourLatitude();

         final double longitude = isLinkPhotoDisplayed
               ? photo.getLinkLongitude()
               : photo.getTourLongitude();

         /*
          * Create map point
          */
         final Map2Point mapPoint = new Map2Point(

               MapPointType.TOUR_PHOTO,
               new GeoPoint(latitude, longitude));

         mapPoint.geoPointDevX = (int) (devXPhoto * _deviceScaling);
         mapPoint.geoPointDevY = (int) (devYPhoto * _deviceScaling);

         mapPoint.photo = photo;

         allMapPoints.add(mapPoint);

         _numStatistics_AllTourPhotos++;

         if (numAllRemainingItems-- <= 0) {
            break;
         }
      }
   }

   private void createMapPoints_TourWayPoints(final List<TourData> allTourData,
                                              final List<Map2Point> allWayPointPoints) {

      final Rectangle worldPixel_Viewport = _mapPointPainter_Viewport_DuringPainting;

      final List<TourWayPoint> allWayPoints = new ArrayList<>();

      for (final TourData tourData : allTourData) {

         if (isMapPointPainterInterrupted()) {
            break;
         }

         if (tourData == null || tourData.isLatLonAvailable() == false) {
            continue;
         }

         final Set<TourWayPoint> allTourMarkers = tourData.getTourWayPoints();

         if (allTourMarkers.isEmpty()) {
            continue;
         }

         for (final TourWayPoint wayPoint : allTourMarkers) {

            // skip when not set
            if (wayPoint.getName().length() == 0) {
               continue;
            }

            /*
             * Check if marker is visible
             */
            java.awt.Point wayPointWorldPixelPosition = wayPoint.getWorldPixelPosition(_mapZoomLevel);

            if (wayPointWorldPixelPosition == null) {

               // convert marker lat/long into world pixels

               wayPointWorldPixelPosition = _mp.geoToPixel(

                     new GeoPosition(wayPoint.getLatitude(), wayPoint.getLongitude()),
                     _mapZoomLevel);

               wayPoint.setWorldPixelPosition(wayPointWorldPixelPosition, _mapZoomLevel);
            }

            final int markerWorldPixelX = wayPointWorldPixelPosition.x;
            final int markerWorldPixelY = wayPointWorldPixelPosition.y;

            final boolean isMarkerInViewport = worldPixel_Viewport.contains(markerWorldPixelX, markerWorldPixelY);

            if (isMarkerInViewport) {

               allWayPoints.add(wayPoint);
            }
         }
      }

      final boolean isTruncateLabel = _mapConfig.isTruncateLabel;
      final int labelTruncateLength = _mapConfig.labelTruncateLength;

      float numAllRemainingItems = _mapConfig.labelDistributorMaxLabels;

      final int numItems = allWayPoints.size();

      final float subItems = numItems / numAllRemainingItems;

      for (int wpIndex = 0; wpIndex < numItems; wpIndex++) {

         int markerSubIndex = wpIndex;

         if (subItems > 1) {

            // there are more names than visible names

            final float nextItemIndex = subItems * wpIndex;
            final double randomDiff = Math.random() * subItems;

            markerSubIndex = (int) (nextItemIndex + randomDiff);

            _numStatistics_AllTourWayPoints_IsTruncated = true;
         }

         // check bounds
         if (markerSubIndex >= numItems) {
            break;
         }

         final TourWayPoint wayPoint = allWayPoints.get(markerSubIndex);

         final java.awt.Point markerWorldPixelPosition = wayPoint.getWorldPixelPosition(_mapZoomLevel);

         if (markerWorldPixelPosition == null) {
            continue;
         }

         final int markerWorldPixelX = markerWorldPixelPosition.x;
         final int markerWorldPixelY = markerWorldPixelPosition.y;

         // convert world position into device position
         final int devX = markerWorldPixelX - worldPixel_Viewport.x;
         final int devY = markerWorldPixelY - worldPixel_Viewport.y;

         String wpName = wayPoint.getName();

         // create formatted label
         if (isTruncateLabel && wpName.length() > labelTruncateLength) {

            // keep star at the end
            final String endSymbol = wpName.endsWith(UI.SYMBOL_STAR)
                  ? UI.SYMBOL_STAR
                  : UI.EMPTY_STRING;

            if (labelTruncateLength == 0) {

               wpName = UI.SYMBOL_DOT + endSymbol;

            } else {

               wpName = wpName.substring(0, labelTruncateLength)

                     + UI.SYMBOL_ELLIPSIS

                     + endSymbol;
            }
         }

         /*
          * Create map point
          */
         final Map2Point mapPoint = new Map2Point(

               MapPointType.TOUR_WAY_POINT,
               new GeoPoint(wayPoint.getLatitude(), wayPoint.getLongitude()));

         mapPoint.tourWayPoint = wayPoint;

         mapPoint.geoPointDevX = (int) (devX * _deviceScaling);
         mapPoint.geoPointDevY = (int) (devY * _deviceScaling);

         mapPoint.setFormattedLabel(wpName);

         allWayPointPoints.add(mapPoint);

         _numStatistics_AllTourWayPoints++;

         if (numAllRemainingItems-- <= 0) {
            break;
         }
      }
   }

   public void deleteFailedImageFiles() {

      MapProviderManager.deleteOfflineMap(_mp, true);
   }

   /**
    * Disposes all overlay image cache and the overlay painting queue
    */
   public synchronized void disposeOverlayImageCache() {

      if (_mp != null) {
         _mp.resetOverlays();
      }

      _tileOverlayPaintQueue.clear();

      _overlayImageCache.dispose();

      grid_UpdatePaintingStateData();
   }

   public void disposeTiles() {

      _mp.disposeTiles();
   }

   private void fireEvent_HoveredTour(final Long hoveredTourId, final int hoveredValuePointIndex) {

      final MapHoveredTourEvent event = new MapHoveredTourEvent(
            hoveredTourId,
            hoveredValuePointIndex,
            _mouseMove_DevPosition_X,
            _mouseMove_DevPosition_Y);

      for (final Object listener : _allHoveredTourListeners.getListeners()) {
         ((IHoveredTourListener) listener).setHoveredTourId(event);
      }
   }

   private void fireEvent_MapGrid(final boolean isGridSelected, final MapGridData mapGridData) {

      final Object[] listeners = _allMapGridListener.getListeners();

      final GeoPosition geoCenter = getMapGeoCenter();

      for (final Object listener : listeners) {

         ((IMapGridListener) listener).onMapGrid(
               _mapZoomLevel,
               geoCenter,
               isGridSelected,
               mapGridData);
      }
   }

   private void fireEvent_MapInfo() {

      final GeoPosition geoCenter = getMapGeoCenter();

      final Object[] listeners = _allMapInfoListener.getListeners();

      for (final Object listener : listeners) {
         ((IMapInfoListener) listener).onMapInfo(geoCenter, _mapZoomLevel);
      }
   }

   /**
    * @param isZoomed
    *           Is <code>true</code> when the event is fired by zooming
    */
   private void fireEvent_MapPosition(final boolean isZoomed) {

      final GeoPosition geoCenter = getMapGeoCenter();

      final Object[] listeners = _allMapPositionListener.getListeners();

      for (final Object listener : listeners) {
         ((IMapPositionListener) listener).onMapPosition(geoCenter, _mapZoomLevel, isZoomed);
      }
   }

   /**
    * @param selection
    */
   private void fireEvent_MapSelection(final ISelection selection) {

      final Object[] listeners = _allMapSelectionListener.getListeners();

      for (final Object listener : listeners) {
         ((IMapSelectionListener) listener).onMapSelection(selection);
      }
   }

   private void fireEvent_MousePosition() {

      // check position, can initially be null
      if ((_mouseMove_DevPosition_X == Integer.MIN_VALUE) || (_mp == null)) {
         return;
      }

      /*
       * !!! DON'T OPTIMIZE THE NEXT LINE, OTHERWISE THE WRONG MOUSE POSITION IS FIRED !!!
       */
      final Rectangle topLeftViewPort = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);

      final int worldMouseX = topLeftViewPort.x + _mouseMove_DevPosition_X;
      final int worldMouseY = topLeftViewPort.y + _mouseMove_DevPosition_Y;

      final GeoPosition geoPosition = _mp.pixelToGeo(new Point2D.Double(worldMouseX, worldMouseY), _mapZoomLevel);
      final MapGeoPositionEvent event = new MapGeoPositionEvent(geoPosition, _mapZoomLevel);

      final Object[] listeners = _allMousePositionListeners.getListeners();
      for (final Object listener : listeners) {
         ((IGeoPositionListener) listener).setPosition(event);
      }
   }

   private void fireEvent_POI(final GeoPosition geoPosition, final String poiText) {

      final MapPOIEvent event = new MapPOIEvent(geoPosition, _mapZoomLevel, poiText);

      final Object[] listeners = _allPOIListeners.getListeners();
      for (final Object listener : listeners) {
         ((IPOIListener) listener).setPOI(event);
      }
   }

   private void fireEvent_RunExternalApp(final int numberOfExternalApp, final Photo photo) {

      final Object[] listeners = _allExternalAppListeners.getListeners();

      for (final Object listener : listeners) {
         ((IExternalAppListener) listener).runExternalApp(numberOfExternalApp, photo);
      }
   }

   private void fireEvent_TourBreadcrumb() {

      for (final Object selectionListener : _allBreadcrumbListener.getListeners()) {
         ((IBreadcrumbListener) selectionListener).updateBreadcrumb();
      }
   }

   private void fireEvent_TourSelection(final ISelection selection) {

      for (final Object selectionListener : _allTourSelectionListener.getListeners()) {
         ((ITourSelectionListener) selectionListener).onTourSelection(selection);
      }
   }

   /**
    * Parse bounding box string.
    *
    * @param boundingBox
    *
    * @return Returns a set with bounding box positions or <code>null</code> when boundingBox cannot
    *         be parsed.
    */
   private Set<GeoPosition> getBoundingBoxPositions(final String boundingBox) {

// example
//      "48.4838981628418,48.5500030517578,9.02030849456787,9.09173774719238"

      final String[] boundingBoxValues = boundingBox.split(","); //$NON-NLS-1$

      if (boundingBoxValues.length != 4) {
         return null;
      }

      try {

         final Set<GeoPosition> positions = new HashSet<>();

         positions.add(new GeoPosition(
               Double.parseDouble(boundingBoxValues[0]),
               Double.parseDouble(boundingBoxValues[2])));

         positions.add(new GeoPosition(
               Double.parseDouble(boundingBoxValues[1]),
               Double.parseDouble(boundingBoxValues[3])));

         return positions;

      } catch (final Exception e) {
         return null;
      }
   }

   public Rectangle getBoundingRect(final Set<GeoPosition> positions, final int zoom) {

      final java.awt.Point geoPixel = _mp.geoToPixel(positions.iterator().next(), zoom);
      final Rectangle rect = new Rectangle(geoPixel.x, geoPixel.y, 0, 0);

      for (final GeoPosition pos : positions) {
         final java.awt.Point point = _mp.geoToPixel(pos, zoom);
         rect.add(new Rectangle(point.x, point.y, 0, 0));
      }
      return rect;
   }

   public CenterMapBy getCenterMapBy() {
      return _centerMapBy;
   }

   /**
    * Gets the current common location of the map. This property does not change when the user pans
    * the map. This property is bound.
    *
    * @return the current map location (address)
    */
   public GeoPosition getCommonLocation() {

      return _commonLocation;
   }

   public float getDeviceScaling() {

      return _deviceScaling;
   }

   public PaintedMapPoint getHoveredMapPoint() {

      return _hoveredMapPoint;
   }

   /**
    * Retrieve, if any, the current tour hovered by the user.
    *
    * @return If found, the current hovered tour otherwise <code>null</code>
    */
   public Long getHoveredTourId() {

      if (_allHoveredTourIds != null && _allHoveredTourIds.size() == 1) {

         return _allHoveredTourIds.get(0);
      }

      return null;
   }

   /**
    * Find a more precise hovered position
    *
    * @param hoveredTourId
    * @param devMouseTileY
    * @param devMouseTileX
    * @param devHoveredTileY
    * @param devHoveredTileX
    * @param allPainted_HoveredRectangle
    * @param allPainted_HoveredTourId
    * @param allPainted_HoveredSerieIndices
    *
    * @return
    */
   private int getHoveredValuePointIndex(final Long hoveredTourId,
                                         final int devMouseTileX,
                                         final int devMouseTileY,
                                         final int devHoveredTileX,
                                         final int devHoveredTileY,
                                         final Rectangle[] allPainted_HoveredRectangle,
                                         final long[] allPainted_HoveredTourId,
                                         final int[] allPainted_HoveredSerieIndices) {

      /*
       * Get indices which are containing the hovered tour id
       */
      int firstPaintIndex = -1;
      int lastPaintIndex = -1;

      for (int paintIndex = 0; paintIndex < allPainted_HoveredTourId.length; paintIndex++) {

         final long paintedTourId = allPainted_HoveredTourId[paintIndex];

         if (firstPaintIndex == -1 && paintedTourId == hoveredTourId) {

            firstPaintIndex = lastPaintIndex = paintIndex;

         } else if (paintedTourId == hoveredTourId) {

            lastPaintIndex = paintIndex;

         } else if (lastPaintIndex != -1) {

            // there are no further positions for the hovered tour

            break;
         }
      }

      /*
       * Find index with the smallest x/y diff value
       */
      int minDiff = Integer.MAX_VALUE;
      int minHoverIndex = 0;

      int hoverIndex;

      for (hoverIndex = firstPaintIndex; hoverIndex <= lastPaintIndex; hoverIndex++) {

         final Rectangle painted_HoveredRectangle = allPainted_HoveredRectangle[hoverIndex];

         final int paintedHovered_CenterX = painted_HoveredRectangle.x + painted_HoveredRectangle.width / 2;
         final int paintedHovered_CenterY = painted_HoveredRectangle.y + painted_HoveredRectangle.height / 2;

         int diffX = devMouseTileX - paintedHovered_CenterX;
         int diffY = devMouseTileY - paintedHovered_CenterY;

         diffX = diffX < 0 ? -diffX : diffX;
         diffY = diffY < 0 ? -diffY : diffY;

         final int allDiff = diffX + diffY;

         if (allDiff < minDiff) {

            minDiff = allDiff;
            minHoverIndex = allPainted_HoveredSerieIndices[hoverIndex];
         }
      }

      return minHoverIndex;
   }

   public Font getLabelFont() {
      return _labelFontSWT;
   }

   /**
    * @return Returns the legend of the map
    */
   public MapLegend getLegend() {
      return _mapLegend;
   }

   /**
    * A property indicating the center position of the map, or <code>null</code> when a tile factory
    * is not set
    *
    * @return Returns the current center position of the map in latitude/longitude
    */
   public GeoPosition getMapGeoCenter() {

      if (_mp == null) {
         return null;
      }

      return _mp.pixelToGeo(_worldPixel_MapCenter, _mapZoomLevel);
   }

   public MapLocationToolTip getMapLocationTooltip() {

      return _mapLocation_Tooltip;
   }

   /**
    * @return Returns the tour map painter
    */
   public TourMapPainter getMapPainter() {

      return _mapPainter;
   }

   /**
    * Is used to activate/deactivate the tooltip
    *
    * @return
    */
   public MapPointToolTip getMapPointTooltip() {

      return _mapPointTooltip;
   }

   /**
    * Get the current map provider
    *
    * @return Returns the current map provider
    */
   public MP getMapProvider() {

      return _mp;
   }

   /**
    * @return
    */
   public GeoPosition getMouseDown_GeoPosition() {

      return _mouseDown_ContextMenu_GeoPosition;
   }

   public GeoPosition getMouseMove_GeoPosition() {

      return _mouseMove_GeoPosition;
   }

   /**
    * @param tileKey
    *
    * @return Returns the key to identify overlay images in the image cache
    */
   private String getOverlayKey(final Tile tile) {

      return _overlayKey + tile.getTileKey();
   }

   /**
    * @param tile
    * @param xOffset
    * @param yOffset
    * @param projectionId
    *
    * @return
    */
   private String getOverlayKey(final Tile tile, final int xOffset, final int yOffset, final String projectionId) {

      return _overlayKey + tile.getTileKey(xOffset, yOffset, projectionId);
   }

   /**
    * @param photo
    * @param map
    * @param tile
    *
    * @return Returns the photo image or <code>null</code> when image is not loaded.
    */
   private BufferedImage getPhotoImage(final Photo photo) {

      BufferedImage awtThumbImage = null;
      BufferedImage awtPhotoImageThumbHQ = null;

      /*
       * 1. The thumbs MUST be loaded firstly because they are also loading the image orientation
       */

      // check if image has an loading error
      final PhotoLoadingState thumbPhotoLoadingState = photo.getLoadingState(ImageQuality.THUMB);

      if (thumbPhotoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not invalid and not yet loaded

         // check if image is in the cache
         awtThumbImage = PhotoImageCache.getImage_AWT(photo, ImageQuality.THUMB);

         if (awtThumbImage == null
               && thumbPhotoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

            // the requested image is not available in the image cache -> image must be loaded

            PhotoLoadManager.putImageInLoadingQueueThumb_Map(
                  photo,
                  ImageQuality.THUMB,
                  _photoImageLoaderCallback,
                  true // is AWT image
            );

//            System.out.println(UI.timeStamp() + " Map2.getPhotoImage() 1 " + null);

            return null;
         }
      }

      if (_isShowHQPhotoImages == false) {

         // HQ image is not requested

//         System.out.println(UI.timeStamp() + " Map2.getPhotoImage() 2 " + awtThumbImage.getWidth() + " / " + awtThumbImage.getHeight());

         return awtThumbImage;
      }

      /*
       * 2. Display thumb HQ image
       */

      // check if image has an loading error
      final PhotoLoadingState thumbHqPhotoLoadingState = photo.getLoadingState(ImageQuality.THUMB_HQ);

      if (thumbHqPhotoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

         // image is not invalid and not yet loaded

         final boolean isPhotoAdjusted = photo.isCropped || photo.isSetTonality;

         final ImageQuality imageQuality = _isShowPhotoAdjustments && isPhotoAdjusted
               ? ImageQuality.THUMB_HQ_ADJUSTED
               : ImageQuality.THUMB_HQ;

         // check if image is in the cache
         awtPhotoImageThumbHQ = PhotoImageCache.getImage_AWT(photo, imageQuality);

         final boolean isImageInLoadingQueue = thumbHqPhotoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE;
         final boolean isImageNotInLoadingQueue = isImageInLoadingQueue == false;

         if (isImageNotInLoadingQueue) {

            final boolean isPhotoModified = _isShowPhotoAdjustments && photo.isAdjustmentModified;
            final boolean isPhotoNotLoaded = awtPhotoImageThumbHQ == null;

            if (isPhotoModified || isPhotoNotLoaded) {

               // the requested image is not available in the image cache or is modified -> image must be loaded

               PhotoLoadManager.putImageInLoadingQueueHQ_Map_Thumb(
                     photo,
                     Photo.getMap2ImageRequestedSize(),
                     imageQuality,
                     _photoImageLoaderCallback);
            }
         }
      }

      if (awtPhotoImageThumbHQ != null) {

//         System.out.println(UI.timeStamp() + " Map2.getPhotoImage() 3 " + awtPhotoImageThumbHQ.getWidth() + " / " + awtPhotoImageThumbHQ.getHeight());

         return awtPhotoImageThumbHQ;
      }

      if (awtThumbImage != null) {
//         System.out.println(UI.timeStamp() + " Map2.getPhotoImage() 4 " + awtThumbImage.getWidth() + " / " + awtThumbImage.getHeight());
      }

      return awtThumbImage;
   }

   public ILoadCallBack getPhotoTooltipImageLoaderCallback() {

      return _photoTooltipImageLoaderCallback;
   }

   private PoiToolTip getPoiTooltip() {

      if (_poi_Tooltip == null) {
         _poi_Tooltip = new PoiToolTip(getShell());
      }

      return _poi_Tooltip;
   }

   /**
    * @param tourId
    *
    * @return Return dev X/Y position of the hovered tour or <code>null</code>
    */
   private int[] getReducesTourPositions(final long tourId) {

      final TourData tourData = TourManager.getTour(tourId);

      if (tourData == null) {

         // this happened, it can be that previously a history/multiple tour was displayed
         return null;
      }

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      if (latitudeSerie == null) {
         return null;
      }

      final MP mp = getMapProvider();
      final int zoomLevel = getZoomLevel();

      // paint with much less points to speed it up
      final int numMaxSegments = 1000;
      final float numSlices = latitudeSerie.length;
      final int numSegments = (int) Math.min(numMaxSegments, numSlices);

      final Rectangle worldPosition_Viewport = _worldPixel_TopLeft_Viewport;

      // get world position for the first lat/lon
      final java.awt.Point worldPos_FirstAWT = mp.geoToPixel(
            new GeoPosition(latitudeSerie[0], longitudeSerie[0]),
            zoomLevel);

      // convert world position into device position
      int devPosX1 = worldPos_FirstAWT.x - worldPosition_Viewport.x;
      int devPosY1 = worldPos_FirstAWT.y - worldPosition_Viewport.y;

      final int[] devXY = new int[numSegments * 2];

      // set first position
      devXY[0] = devPosX1;
      devXY[1] = devPosY1;

      for (int segmentIndex = 1; segmentIndex < numSegments; segmentIndex++) {

         final int nextSerieIndex = (int) (numSlices / numSegments * segmentIndex);

         // get world position for the current lat/lon
         final java.awt.Point worldPosAWT = mp.geoToPixel(
               new GeoPosition(latitudeSerie[nextSerieIndex], longitudeSerie[nextSerieIndex]),
               zoomLevel);

         // convert world position into device position
         final int devPosX2 = worldPosAWT.x - worldPosition_Viewport.x;
         final int devPosY2 = worldPosAWT.y - worldPosition_Viewport.y;

         final int devXYIndex = segmentIndex * 2;

         devXY[devXYIndex + 0] = devPosX2;
         devXY[devXYIndex + 1] = devPosY2;

         // advance to the next segment
         devPosX1 = devPosX2;
         devPosY1 = devPosY2;
      }

      return devXY;
   }

   public PaintedMapPoint getSelectedPhotoMapPoint() {

      return _selectedPhotoMapPoint;
   }

   /**
    * Returns the bounds of the viewport in pixels. This can be used to transform points into the
    * world bitmap coordinate space. The viewport is the part of the map, that you can currently see
    * on the screen.
    *
    * @return Returns the bounds in <em>pixels</em> of the "view" of this map
    */
   private Rectangle getWorldPixel_TopLeft_Viewport(final Point2D worldPixelMapCenter) {

      if (_clientArea == null) {
         _clientArea = getClientArea();
      }

      final int devWidth = _clientArea.width;
      final int devHeight = _clientArea.height;

      final int worldX = (int) (worldPixelMapCenter.getX() - (devWidth / 2d));
      final int worldY = (int) (worldPixelMapCenter.getY() - (devHeight / 2d));

      return new Rectangle(worldX, worldY, devWidth, devHeight);
   }

   /**
    * @param positions
    *           Geo positions
    * @param zoom
    *           Requested zoom level
    *
    * @return Returns a rectangle in world positions which contains all geo positions for the given
    *         zoom level
    */
   public Rectangle getWorldPixelFromGeoPositions(final Set<GeoPosition> positions, final int zoom) {

      // set first point
      final java.awt.Point point1 = _mp.geoToPixel(positions.iterator().next(), zoom);
      final MTRectangle mtRect = new MTRectangle(point1.x, point1.y, 0, 0);

      // set 2..n points
      for (final GeoPosition pos : positions) {
         final java.awt.Point point = _mp.geoToPixel(pos, zoom);
         mtRect.add(point.x, point.y);
      }

      return new Rectangle(mtRect.x, mtRect.y, mtRect.width, mtRect.height);
   }

   /**
    * @return Returns the map viewport in world pixel for the current map center
    *         <p>
    *         <b>x</b> and <b>y</b> contains the position in world pixel of the top left viewport in
    *         the map<br>
    *         <b>width</b> and <b>height</b> contains the visible area in device pixel
    */
   public Rectangle getWorldPixelViewport() {
      return getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);
   }

   /**
    * Gets the current zoom level, or <code>null</code> when a tile factory is not set
    *
    * @return Returns the current zoom level of the map
    */
   public int getZoomLevel() {
      return _mapZoomLevel;
   }

   /**
    * Convert start/end positions into top-left/bottom-end positions.
    *
    * @param geo_Start
    * @param geo_End
    * @param mapGridData
    *
    * @return
    */
   private void grid_Convert_StartEnd_2_TopLeft(GeoPosition geo_Start,
                                                final GeoPosition geo_End,
                                                final MapGridData mapGridData) {

      if (geo_Start == null) {

         // this can occur when hovering but not yet selected

         geo_Start = geo_End;
      }

      final Point world_Start = UI.SWT_Point(_mp.geoToPixel(geo_Start, _mapZoomLevel));
      final Point world_End = UI.SWT_Point(_mp.geoToPixel(geo_End, _mapZoomLevel));

      mapGridData.world_Start = world_Start;
      mapGridData.world_End = world_End;

      final int world_Start_X = world_Start.x;
      final int world_Start_Y = world_Start.y;
      final int world_End_X = world_End.x;
      final int world_End_Y = world_End.y;

      // 1: top/left
      // 2: bottom/right

      double geoLat1 = 0;
      double geoLon1 = 0;

      double geoLat2 = 0;
      double geoLon2 = 0;

      // XY1: top/left
      geoLon1 = world_Start_X <= world_End_X ? geo_Start.longitude : geo_End.longitude;
      geoLat1 = world_Start_Y <= world_End_Y ? geo_Start.latitude : geo_End.latitude;

      // XY2: bottom/right
      geoLon2 = world_Start_X >= world_End_X ? geo_Start.longitude : geo_End.longitude;
      geoLat2 = world_Start_Y >= world_End_Y ? geo_Start.latitude : geo_End.latitude;

      final GeoPosition geo1 = new GeoPosition(geoLat1, geoLon1);
      final GeoPosition geo2 = new GeoPosition(geoLat2, geoLon2);

      // set lat/lon to a grid of 0.01�
      int geoGrid_Lat1_E2 = (int) (geoLat1 * 100);
      int geoGrid_Lon1_E2 = (int) (geoLon1 * 100);

      int geoGrid_Lat2_E2 = (int) (geoLat2 * 100);
      int geoGrid_Lon2_E2 = (int) (geoLon2 * 100);

      // keep geo location
      mapGridData.geoLocation_TopLeft_E2 = new Point(geoGrid_Lon1_E2, geoGrid_Lat1_E2); // X1 / Y1
      mapGridData.geoLocation_BottomRight_E2 = new Point(geoGrid_Lon2_E2, geoGrid_Lat2_E2); // X2 /Y2

      final Point devGrid_1 = grid_Geo2Dev_WithGeoGrid(geo1);
      final Point devGrid_2 = grid_Geo2Dev_WithGeoGrid(geo2);

      int devGrid_X1 = devGrid_1.x;
      int devGrid_Y1 = devGrid_1.y;

      int devGrid_X2 = devGrid_2.x;
      int devGrid_Y2 = devGrid_2.y;

      final int geoGridPixelSizeX = (int) _devGridPixelSize_X;
      final int geoGridPixelSizeY = (int) _devGridPixelSize_Y;

      final int gridSize_E2 = 1;

      /**
       * Adjust lat/lon +/-, this algorithm is created with many many many try and error
       */
      if (geoLat1 > 0 && geoLon1 > 0 && geoLat2 > 0 && geoLon2 > 0) {

         // 1: + / +
         // 2: + / +

         //     |
         //     | xx
         // ---------
         //     |
         //     |

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY;

         geoGrid_Lon2_E2 += gridSize_E2; // X2
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

      } else if (geoLat1 > 0 && geoLon1 > 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: + / +
         // 2: - / +

         //     |
         //     | xx
         // ------xx-
         //     | xx
         //     |

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon2_E2 += gridSize_E2; // X2

         geoGrid_Lat1_E2 += gridSize_E2; // Y1
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 < 0 && geoLon1 > 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: - / +
         // 2: - / +

         //     |
         //     |
         // ---------
         //     | xx
         //     |

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y1 += geoGridPixelSizeY;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon2_E2 += gridSize_E2; // X2
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 > 0 && geoLon2 < 0) {

         // 1: + / -
         // 2: + / -

         //     |
         //  xx |
         // ---------
         //     |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 < 0) {

         // 1: + / -
         // 2: - / -

         //     |
         //  xx |
         // -xx------
         //  xx |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 < 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 < 0) {

         // 1: - / -
         // 2: - / -

         //     |
         //     |
         // ---------
         //  xx |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y1 += geoGridPixelSizeY;

         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 > 0 && geoLon2 > 0) {

         // 1: + / -
         // 2: + / +

         //     |
         //   xxxxx
         // ---------
         //     |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY;

         devGrid_X2 += geoGridPixelSizeX;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

         geoGrid_Lon2_E2 += gridSize_E2; // X2

      } else if (geoLat1 < 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: - / -
         // 2: - / +

         //     |
         //     |
         // ---------
         //   xxxxx
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y1 += geoGridPixelSizeY;

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1

         geoGrid_Lon2_E2 += gridSize_E2; // X2
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: + / -
         // 2: - / +

         //     |
         //   xxxxx
         // --xxxxx--
         //   xxxxx
         //     |

         devGrid_X1 -= geoGridPixelSizeX;

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

         geoGrid_Lon2_E2 += gridSize_E2; // X2
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2
      }

      int devWidth = devGrid_X2 - devGrid_X1;
      int devHeight = devGrid_Y2 - devGrid_Y1;

      // ensure it is always visible
      devWidth = (int) Math.max(geoGridPixelSizeX * 0.7, devWidth);
      devHeight = (int) Math.max(geoGridPixelSizeY * 0.7, devHeight);

      mapGridData.devGrid_X1 = devGrid_X1;
      mapGridData.devGrid_Y1 = devGrid_Y1;

      mapGridData.devWidth = devWidth;
      mapGridData.devHeight = devHeight;

      mapGridData.numWidth = (int) (devWidth / _devGridPixelSize_X + 0.5);
      mapGridData.numHeight = (int) (devHeight / _devGridPixelSize_Y + 0.5);

      mapGridData.geoParts_TopLeft_E2 = new Point(geoGrid_Lon1_E2, geoGrid_Lat1_E2); // X1 / Y1
      mapGridData.geoParts_BottomRight_E2 = new Point(geoGrid_Lon2_E2, geoGrid_Lat2_E2); // X2 /Y2
   }

   /**
    * Hide geo grid and reset all states
    */
   private void grid_DisableGridBoxSelection() {

      _isContextMenuEnabled = true;

      TourGeoFilter_Manager.setGeoFilter_OpenSlideout(false, false);

      setCursorOptimized(_cursorDefault);
      redraw();
   }

   /**
    * @param mouseBorderPosition
    * @param eventTime
    */
   private void grid_DoAutoScroll(final Point mouseBorderPosition) {

      final int AUTO_SCROLL_INTERVAL = 50; // 20ms == 50fps

      _geoGrid_IsGridAutoScroll = true;
      _geoGrid_AutoScrollCounter[0]++;
      setCursorOptimized(_cursorSearchTour_Scroll);

      getDisplay().timerExec(AUTO_SCROLL_INTERVAL, new Runnable() {

         final int __runnableScrollCounter = _geoGrid_AutoScrollCounter[0];

         @Override
         public void run() {

            if (__runnableScrollCounter != _geoGrid_AutoScrollCounter[0]) {
               // a new runnable is created
               return;
            }

            if (isDisposed() || _geoGrid_IsGridAutoScroll == false) {
               // auto scrolling is stopped
               return;
            }

            /*
             * set new map center
             */

            final int mapDiffX = mouseBorderPosition.x / 3;
            final int mapDiffY = mouseBorderPosition.y / 3;

            final double oldCenterX = _worldPixel_MapCenter.getX();
            final double oldCenterY = _worldPixel_MapCenter.getY();

            final double newCenterX = oldCenterX - mapDiffX;
            final double newCenterY = oldCenterY - mapDiffY;

            // set new map center
            setMapCenterInWorldPixel(new Point2D.Double(newCenterX, newCenterY));
            updateViewportData();

            paint();

            fireEvent_MapPosition(false);

            // start scrolling again when the bounds have not been reached
            final Point mouseBorderPosition = grid_GetMouseBorderPosition();
            final boolean isRepeatScrolling = mouseBorderPosition != null;
            if (isRepeatScrolling) {
               getDisplay().timerExec(AUTO_SCROLL_INTERVAL, this);
            } else {
               _geoGrid_IsGridAutoScroll = false;
               setCursorOptimized(_cursorSearchTour);
            }
         }
      });
   }

   /**
    * Convert geo position into grid dev position
    *
    * @return
    */
   private Point grid_Geo2Dev_WithGeoGrid(final GeoPosition geoPos) {

      // truncate to 0.01

      final double geoLat_E2 = geoPos.latitude * 100;
      final double geoLon_E2 = geoPos.longitude * 100;

      final double geoLat = (int) geoLat_E2 / 100.0;
      final double geoLon = (int) geoLon_E2 / 100.0;

      final java.awt.Point worldGrid = _mp.geoToPixel(new GeoPosition(geoLat, geoLon), _mapZoomLevel);

      // get device rectangle for the position
      final Point gridGeoPos = new Point(
            worldGrid.x - _worldPixel_TopLeft_Viewport.x,
            worldGrid.y - _worldPixel_TopLeft_Viewport.y);

      /*
       * Adjust Y that X and Y are at the top/left position otherwise Y is at the bottom/left
       * position
       */
      gridGeoPos.y -= _devGridPixelSize_Y;

      return gridGeoPos;
   }

   /**
    * @param mouseEvent
    *
    * @return Returns mouse position in the map border or <code>null</code> when the border is not
    *         hovered. The returned absolute values are higher when the mouse is closer to the
    *         border.
    */
   private Point grid_GetMouseBorderPosition() {

      final int mapBorderSize = 30;

      final int mapWidth = _clientArea.width;
      final int mapHeight = _clientArea.height;

      // check map min size
      if (mapWidth < 2 * mapBorderSize || mapHeight < 2 * mapBorderSize) {
         return null;
      }

      final int mouseX = _mouseMove_DevPosition_X;
      final int mouseY = _mouseMove_DevPosition_Y;

      int x = 0;
      int y = 0;

      boolean isInBorder = false;

      // check left border, returns -x
      if (mouseX < mapBorderSize) {
         isInBorder = true;
         x = (mapBorderSize - mouseX);
      }

      // check right border, returns +x
      if (mouseX > mapWidth - mapBorderSize) {
         isInBorder = true;
         x = -(mapBorderSize - (mapWidth - mouseX));
      }

      // check top border, returns +y
      if (mouseY < mapBorderSize) {
         isInBorder = true;
         y = mapBorderSize - mouseY;
      }

      // check bottom border, returns -y
      if (mouseY > mapHeight - mapBorderSize) {
         isInBorder = true;
         y = -(mapBorderSize - (mapHeight - mouseY));
      }

      if (isInBorder) {

         return new Point(x, y);

      } else {

         return null;
      }
   }

   private void grid_UpdateEndPosition(final MouseEvent mouseEvent, final MapGridData mapGridData) {

      final int worldMouseX = _worldPixel_TopLeft_Viewport.x + mouseEvent.x;
      final int worldMouseY = _worldPixel_TopLeft_Viewport.y + mouseEvent.y;

      final Point worldMouse_End = new Point(worldMouseX, worldMouseY);
      final GeoPosition geo_End = _mp.pixelToGeo(new Point2D.Double(worldMouse_End.x, worldMouse_End.y), _mapZoomLevel);

      mapGridData.geo_End = geo_End;

      grid_Convert_StartEnd_2_TopLeft(mapGridData.geo_Start, mapGridData.geo_End, mapGridData);
   }

   /**
    * Update geo grid positions after map relocation
    */
   private void grid_UpdateGeoGridData() {

      if (_geoGrid_Data_Hovered != null) {

         if (_geoGrid_Data_Hovered.geo_Start != null) {

            final GeoPosition geo_Start = _geoGrid_Data_Hovered.geo_Start;
            final GeoPosition geo_End = _geoGrid_Data_Hovered.geo_End;

            grid_Convert_StartEnd_2_TopLeft(geo_Start, geo_End, _geoGrid_Data_Hovered);

         } else {

            /*
             * When hovering has started then there is no geo_Start value but the painting data can
             * be updated from the mouse move positions
             */

            final GeoPosition geo_MouseMove = _geoGrid_Data_Hovered.geo_MouseMove;
            if (geo_MouseMove != null) {

               grid_Convert_StartEnd_2_TopLeft(geo_MouseMove, geo_MouseMove, _geoGrid_Data_Hovered);
            }
         }
      }

      if (_geoGrid_Data_Selected != null && _geoGrid_Data_Selected.geo_Start != null) {

         final GeoPosition geo_Start = _geoGrid_Data_Selected.geo_Start;
         final GeoPosition geo_End = _geoGrid_Data_Selected.geo_End;

         grid_Convert_StartEnd_2_TopLeft(geo_Start, geo_End, _geoGrid_Data_Selected);
      }

      redraw();
   }

   private void grid_UpdatePaintingStateData() {

      // set fast map paining
      _isFastMapPainting = Util.getStateBoolean(_geoFilterState,
            TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING,
            TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING_DEFAULT);

      _fastMapPainting_skippedValues = Util.getStateInt(_geoFilterState,
            TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES,
            TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES_DEFAULT);
   }

   /**
    * Convert geo position into dev position
    *
    * @return
    */
   private Point grid_World2Dev(final Point worldPosition) {

      // get device rectangle for the position
      final Point gridGeoPos = new Point(
            worldPosition.x - _worldPixel_TopLeft_Viewport.x,
            worldPosition.y - _worldPixel_TopLeft_Viewport.y);

      return gridGeoPos;
   }

   private void hideTourTooltipHoveredArea() {

      if (_tourTooltip == null) {
         return;
      }

      // update tool tip because it has it's own mouse move listener for the map
      _tourTooltip.hideHoveredArea();

      if (_tourTooltip_HoveredAreaContext != null) {

         // hide hovered area
         _tourTooltip_HoveredAreaContext = null;

         redraw();
      }
   }

   private void initMap() {

      _mapTileSize = _mp.getMapTileSize(_mapZoomLevel);
      _tilePixelSize = _mp.getTileSize();

      final double tileDefaultCenter = (double) _tilePixelSize / 2;

      _worldPixel_MapCenter = new Point2D.Double(tileDefaultCenter, tileDefaultCenter);
      _worldPixel_TopLeft_Viewport = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);
   }

   public boolean isCutOffLinesInPauses() {
      return _prefOptions_isCutOffLinesInPauses;
   }

   public boolean isMapBackgroundDark() {
      return _isMapBackgroundDark;
   }

   private boolean isMapPointPainterInterrupted() {

      return _mapPointPainter_Task == null || _mapPointPainter_Task.isCancelled();
   }

   private boolean isPhotoTour(final Photo photo) {

      final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(photo);

      if (allTourPhotos.size() > 0) {

         final TourPhoto tourPhoto = allTourPhotos.get(0);

         if (tourPhoto != null) {

            final TourData tourData = tourPhoto.getTourData();

            return tourData.isPhotoTour();
         }
      }

      return false;
   }

   /**
    * @return Returns <code>true</code> when 'Search tour by location' is active
    */
   public boolean isSearchTourByLocation() {

      return _geoGrid_Data_Hovered != null || _geoGrid_IsGridAutoScroll == true;

   }

   public boolean isShowPhotoAdjustments() {

      return _isShowPhotoAdjustments;
   }

   /**
    * Checks is a tile position is within a map. It is possible that the tile is outside of the map
    * when it's value is negative or greater than the map border.
    * <p>
    * {@link #setMapCenterInWorldPixel(Point2D)}
    * <p>
    * Before version 10.6 the map was repeated on the x axis.
    *
    * @param tilePosX
    * @param tilePosY
    *
    * @return
    */
   private boolean isTileOnMap(final int tilePosX, final int tilePosY) {

      if (tilePosY < 0 || tilePosY >= _mapTileSize.height) {

         return false;

      } else {

         if (_mapZoomLevel < 5) {

            if (tilePosX < 0 || tilePosX >= _mapTileSize.width) {

               return false;
            }

         } else {

            // display one additional tile when the the map is zoomed enough
            if (tilePosX < -1 || tilePosX > _mapTileSize.width) {

               return false;
            }
         }

         return true;
      }
   }

   /**
    * The critical parts in this method is to convert mouse position into tile position.
    *
    * @return Returns <code>true</code> when a tour is hovered.
    */
   private boolean isTourHovered() {

      if (_worldPixel_TopLeft_Viewport == null) {

         // this occurred when comparing tours

         return false;
      }

      int numPrevHoveredTours;

      Rectangle[] allPainted_HoveredRectangle = null;
      long[] allPainted_HoveredTourId = null;
      int[] allPainted_HoveredSerieIndices = null;

      int devMouseTileX = 0;
      int devMouseTileY = 0;

      // top/left dev position for the hovered tile
      int devHoveredTileX = 0;
      int devHoveredTileY = 0;

      try {

         Tile hoveredTile = null;

         int hoveredTileIndex_X = 0;
         int hoveredTileIndex_Y = 0;

         final int devMouseX = _mouseMove_DevPosition_X;
         final int devMouseY = _mouseMove_DevPosition_Y;

         final Tile[][] allPaintedTiles = _allPaintedTiles;

         /*
          * Get tile which is hovered
          */
         tileLoop:

         for (int tilePosX = _tilePos_MinX; tilePosX <= _tilePos_MaxX; tilePosX++) {

            hoveredTileIndex_Y = 0;

            for (int tilePosY = _tilePos_MinY; tilePosY <= _tilePos_MaxY; tilePosY++) {

               // convert tile world position into device position
               devHoveredTileX = tilePosX * _tilePixelSize - _worldPixel_TopLeft_Viewport.x;
               devHoveredTileY = tilePosY * _tilePixelSize - _worldPixel_TopLeft_Viewport.y;

               final int devTileXNext = devHoveredTileX + _tilePixelSize;
               final int devTileYNext = devHoveredTileY + _tilePixelSize;

               if (devMouseX >= devHoveredTileX && devMouseX < devTileXNext) {
                  if (devMouseY >= devHoveredTileY && devMouseY < devTileYNext) {

                     // this is the tile which is hovered with the mouse

                     hoveredTile = allPaintedTiles[hoveredTileIndex_X][hoveredTileIndex_Y];

                     break tileLoop;
                  }
               }

               hoveredTileIndex_Y++;
            }

            hoveredTileIndex_X++;
         }

         numPrevHoveredTours = _allHoveredTourIds.size();

         // reset hovered data
         _allHoveredDevPoints.clear();
         _allHoveredTourIds.clear();
         _allHoveredSerieIndices.clear();

         if (hoveredTile == null) {

            // this can occur when map is zoomed
            return false;
         }

         final List<Rectangle> allPainted_HoveredRectangle_List = hoveredTile.allPainted_HoverRectangle;
         if (allPainted_HoveredRectangle_List.isEmpty()) {

            // nothing is painted in this tile
            return false;
         }

         // get mouse relative position in the tile
         devMouseTileX = devMouseX - devHoveredTileX;
         devMouseTileY = devMouseY - devHoveredTileY;

         // optimize performance by removing object references
         allPainted_HoveredTourId = hoveredTile.allPainted_HoverTourID.toArray();
         allPainted_HoveredSerieIndices = hoveredTile.allPainted_HoverSerieIndices.toArray();

         allPainted_HoveredRectangle = allPainted_HoveredRectangle_List.toArray(new Rectangle[allPainted_HoveredRectangle_List.size()]);

         long painted_HoveredTourId = -1;
         final int numPainted_HoveredTourId = allPainted_HoveredTourId.length;

         for (int hoverIndex = 0; hoverIndex < numPainted_HoveredTourId; hoverIndex++) {

            final Rectangle painted_HoveredRectangle = allPainted_HoveredRectangle[hoverIndex];

            final int paintedHoveredX = painted_HoveredRectangle.x;
            final int paintedHoveredY = painted_HoveredRectangle.y;

            // optimized: painted_HoveredRectangle.contains(devMouseTileX, devMouseTileY)
            if (devMouseTileX >= paintedHoveredX
                  && devMouseTileY >= paintedHoveredY
                  && devMouseTileX < (paintedHoveredX + painted_HoveredRectangle.width)
                  && devMouseTileY < (paintedHoveredY + painted_HoveredRectangle.height)) {

               // a tour is hovered

               painted_HoveredTourId = allPainted_HoveredTourId[hoverIndex];

               int devHoveredRect_Center_X = paintedHoveredX + painted_HoveredRectangle.width / 2;
               int devHoveredRect_Center_Y = paintedHoveredY + painted_HoveredRectangle.height / 2;

               // convert from tile position to device position
               devHoveredRect_Center_X += devHoveredTileX;
               devHoveredRect_Center_Y += devHoveredTileY;

               _allHoveredTourIds.add(painted_HoveredTourId);
               _allHoveredSerieIndices.add(allPainted_HoveredSerieIndices[hoverIndex]);
               _allHoveredDevPoints.add(new Point(devHoveredRect_Center_X, devHoveredRect_Center_Y));

               // advance to the next tour id
               int hoverTourIdIndex;
               for (hoverTourIdIndex = hoverIndex + 1; hoverTourIdIndex < numPainted_HoveredTourId; hoverTourIdIndex++) {

                  final long nextPainted_HoveredTourId = allPainted_HoveredTourId[hoverTourIdIndex];

                  if (nextPainted_HoveredTourId != painted_HoveredTourId) {

                     // the next tour id is discovered

                     hoverIndex = hoverTourIdIndex;

                     break;
                  }
               }

               // this must be checked again otherwise the last tour id occurs multiple times
               if (hoverTourIdIndex >= numPainted_HoveredTourId) {
                  break;
               }
            }
         }

      } finally {

         // fire hover event, when a tour is hovered

         final int numHoveredSerieIndices = _allHoveredSerieIndices.size();

         if (numHoveredSerieIndices > 0) {

            int hoveredValuePointIndex;

            if (numHoveredSerieIndices == 1) {

               hoveredValuePointIndex = getHoveredValuePointIndex(

                     _allHoveredTourIds.get(0),

                     devMouseTileX,
                     devMouseTileY,

                     devHoveredTileX,
                     devHoveredTileY,

                     allPainted_HoveredRectangle,
                     allPainted_HoveredTourId,
                     allPainted_HoveredSerieIndices);

               // overwrite initial value
               _allHoveredSerieIndices.set(0, hoveredValuePointIndex);

            } else {

               hoveredValuePointIndex = _allHoveredSerieIndices.get(0);
            }

            fireEvent_HoveredTour(getHoveredTourId(), hoveredValuePointIndex);
         }
      }

      if (_allHoveredTourIds.size() > 0) {

         return true;

      } else {

         // hide previously hovered tour
         if (numPrevHoveredTours > 0) {
            paint();
         }

         return false;
      }
   }

   /**
    * @param pauseDuration
    *           Pause duration in seconds
    * @param isPauseAnAutoPause
    *           When <code>true</code> an auto-pause happened otherwise it is an user pause
    *
    * @return
    */
   private boolean isTourPauseVisible(final long pauseDuration, final boolean isPauseAnAutoPause) {

      final Map2Config config = _mapConfig;

      if (config.isFilterTourPauses == false) {

         // nothing is filtered
         return true;
      }

      boolean isPauseVisible = false;

      if (config.isShowAutoPauses && isPauseAnAutoPause) {

         // pause is an auto-pause
         isPauseVisible = true;
      }

      if (config.isShowUserPauses && !isPauseAnAutoPause) {

         // pause is a user-pause
         isPauseVisible = true;
      }

      if (isPauseVisible && config.isFilterTourPause_Duration) {

         // filter by pause duration -> hide pause when condition is true

         final long requiredPauseDuration = config.tourPauseDuration;
         final Enum<TourFilterFieldOperator> pauseDurationOperator = config.tourPauseDurationFilter_Operator;

         if (TourFilterFieldOperator.GREATER_THAN_OR_EQUAL.equals(pauseDurationOperator)) {

            isPauseVisible = (pauseDuration >= requiredPauseDuration) == false;

         } else if (TourFilterFieldOperator.LESS_THAN_OR_EQUAL.equals(pauseDurationOperator)) {

            isPauseVisible = (pauseDuration <= requiredPauseDuration) == false;

         } else if (TourFilterFieldOperator.EQUALS.equals(pauseDurationOperator)) {

            isPauseVisible = (pauseDuration == requiredPauseDuration) == false;

         } else if (TourFilterFieldOperator.NOT_EQUALS.equals(pauseDurationOperator)) {

            isPauseVisible = (pauseDuration != requiredPauseDuration) == false;
         }
      }

      return isPauseVisible;
   }

   /**
    * @return Returns <code>true</code> when a cluster is hovered
    */
   private boolean markerCluster_SetHoveredCluster() {

      final PaintedMarkerCluster oldHoveredCluster = _hoveredMarkerCluster;
      PaintedMarkerCluster newHoveredCluster = null;

      _hoveredMarkerCluster = null;

      for (final PaintedMarkerCluster paintedCluster : _allPaintedMarkerClusters) {

         final Rectangle paintedRect = paintedCluster.clusterSymbolRectangle;

         // increase hovered rectangle to prevent flickering when moving too fast

         final int clusterWidth = paintedRect.width;
         final int clusterHeight = paintedRect.height;

         final int hoveredOffset = clusterWidth > 30 ? 0 : 10;

         if (true
               && (_mouseMove_DevPosition_X > paintedRect.x - hoveredOffset)
               && (_mouseMove_DevPosition_X < paintedRect.x + clusterWidth + hoveredOffset)
               && (_mouseMove_DevPosition_Y > paintedRect.y - hoveredOffset)
               && (_mouseMove_DevPosition_Y < paintedRect.y + clusterHeight + hoveredOffset)) {

            // a cluster is hovered

            _hoveredMarkerCluster = newHoveredCluster = paintedCluster;

            break;
         }
      }

      // repaint map when hovered state has changed
      if (oldHoveredCluster == null && newHoveredCluster == null) {

         // ignore

      } else if (false

            || oldHoveredCluster == null && newHoveredCluster != null
            || oldHoveredCluster != null && newHoveredCluster == null

            // now both should be NOT null -> compare cluster rectangles
            || oldHoveredCluster.clusterSymbolRectangle.equals(newHoveredCluster.clusterSymbolRectangle) == false) {

         // another cluster is hovered

         paint();

         return true;

      } else {

         // the same cluster is hovered

         // hide hovered tours
         _allHoveredDevPoints.clear();
         _allHoveredTourIds.clear();
         _allHoveredSerieIndices.clear();

         return true;
      }

      return false;
   }

   /**
    * Hide offline area and all states
    */
   private void offline_DisableOfflineAreaSelection() {

      _offline_IsSelectingOfflineArea = false;
      _offline_IsPaintOfflineArea = false;
      _offline_IsOfflineSelectionStarted = false;

      _isContextMenuEnabled = true;

      setCursorOptimized(_cursorDefault);
      redraw();
   }

   /**
    * Create top/left geo grid position from world position
    *
    * @param worldPosX
    * @param worldPosY
    *
    * @return
    */
   private Point offline_GetDevGridGeoPosition(final int worldPosX, final int worldPosY) {

      final Point2D.Double worldPixel = new Point2D.Double(worldPosX, worldPosY);

      final GeoPosition geoPos = _mp.pixelToGeo(worldPixel, _mapZoomLevel);

      // truncate to 0.01

      final double geoLat_E2 = geoPos.latitude * 100;
      final double geoLon_E2 = geoPos.longitude * 100;

      final double geoLat = (int) geoLat_E2 / 100.0;
      final double geoLon = (int) geoLon_E2 / 100.0;

      final java.awt.Point worldGrid = _mp.geoToPixel(new GeoPosition(geoLat, geoLon), _mapZoomLevel);

      // get device rectangle for the position
      final Point gridGeoPos = new Point(
            worldGrid.x - _worldPixel_TopLeft_Viewport.x,
            worldGrid.y - _worldPixel_TopLeft_Viewport.y);

      /*
       * Adjust Y that X and Y are at the top/left position otherwise Y is at the bottom/left
       * position
       */
      gridGeoPos.y -= _devGridPixelSize_Y;

      return gridGeoPos;
   }

   private Point offline_GetTilePosition(final int worldPosX, final int worldPosY) {

      int tilePosX = (int) Math.floor((double) worldPosX / (double) _tilePixelSize);
      int tilePosY = (int) Math.floor((double) worldPosY / (double) _tilePixelSize);

      final int mapTiles = _mapTileSize.width;

      /*
       * adjust tile position to the map border
       */
      tilePosX = tilePosX % mapTiles;
      if (tilePosX < -mapTiles) {
         tilePosX += mapTiles;
         if (tilePosX == mapTiles) {
            tilePosX = 0;
         }
      }

      if (tilePosY < 0) {
         tilePosY = 0;
      } else if ((tilePosY >= mapTiles) && (mapTiles > 0)) {
         tilePosY = mapTiles - 1;
      }

      // get device rectangle for this tile
      return new Point(
            tilePosX * _tilePixelSize - _worldPixel_TopLeft_Viewport.x,
            tilePosY * _tilePixelSize - _worldPixel_TopLeft_Viewport.y);
   }

   private void offline_OpenOfflineImageDialog() {

      new DialogManageOfflineImages(
            _display.getActiveShell(),
            _mp,
            _offline_WorldMouse_Start,
            _offline_WorldMouse_End,
            _mapZoomLevel).open();

      offline_DisableOfflineAreaSelection();

      // force to reload map images
      _mp.disposeTileImages();

      redraw();
      paint();
   }

   private void offline_UpdateOfflineAreaEndPosition(final MouseEvent mouseEvent) {

      final int worldMouseX = _worldPixel_TopLeft_Viewport.x + mouseEvent.x;
      final int worldMouseY = _worldPixel_TopLeft_Viewport.y + mouseEvent.y;

      _offline_DevMouse_End = new Point(mouseEvent.x, mouseEvent.y);
      _offline_WorldMouse_End = new Point(worldMouseX, worldMouseY);

      _offline_DevTileEnd = offline_GetTilePosition(worldMouseX, worldMouseY);
   }

   /**
    * onDispose is called when the map is disposed
    *
    * @param event
    */
   private void onDispose(final DisposeEvent event) {

      if (_mp != null) {
         _mp.resetAll(false);
      }
      if (_dropTarget != null) {
         _dropTarget.dispose();
      }

      if (_mapPointPainter_Task != null) {
         _mapPointPainter_Task.cancel(true);
      }

      UI.disposeResource(_labelFontSWT);
      UI.disposeResource(_mapImage);
      UI.disposeResource(_mapPointImage);
      UI.disposeResource(_poiImage);

      UI.disposeResource(_cursorCross);
      UI.disposeResource(_cursorDefault);
      UI.disposeResource(_cursorHand);
      UI.disposeResource(_cursorPan);
      UI.disposeResource(_cursorPhoto_Move);
      UI.disposeResource(_cursorPhoto_Select);
      UI.disposeResource(_cursorSearchTour);
      UI.disposeResource(_cursorSearchTour_Scroll);
      UI.disposeResource(_cursorSelect);

      PhotoLoadManager.stopImageLoading(true);

      // dispose resources in the overlay plugins
      _mapPainter.dispose();

      _overlayImageCache.dispose();
      _colorCache.dispose();

      if (_directMapPainter != null) {
         _directMapPainter.dispose();
      }

      // dispose legend image
      if (_mapLegend != null) {
         UI.disposeResource(_mapLegend.getImage());
      }

      if (_poi_Tooltip != null) {
         _poi_Tooltip.dispose();
      }

      // stop overlay thread
      _overlayThread.interrupt();

      _mapPointPainter_Executor.shutdownNow();
   }

   private void onDropRunnable(final DropTargetEvent event) {

      final TransferData transferDataType = event.currentDataType;

      boolean isPOI = false;

      if (TextTransfer.getInstance().isSupportedType(transferDataType)) {

         if (event.data instanceof final String eventData) {

            isPOI = parseAndDisplayPOIText(eventData);
         }

      } else if (URLTransfer.getInstance().isSupportedType(transferDataType)) {

         isPOI = parseAndDisplayPOIText((String) event.data);
      }

      if (isPOI == false) {

         String poiText = Messages.Dialog_DropNoPOI_InvalidData;

         if (event.data instanceof String) {

            poiText = (String) event.data;

            final int maxLength = 1000;
            if (poiText.length() > maxLength) {
               poiText = poiText.substring(0, maxLength) + "..."; //$NON-NLS-1$
            }
         }

         MessageDialog.openInformation(getShell(),
               Messages.Dialog_DropNoPOI_Title,
               NLS.bind(Messages.Dialog_DropNoPOI_Message, poiText));
      }
   }

   private void onKey_Down(final Event event) {

      if (_offline_IsSelectingOfflineArea) {
         offline_DisableOfflineAreaSelection();
         return;
      }

      // stop tour search by location
      if (_geoGrid_Data_Hovered != null) {

         _geoGrid_Data_Hovered = null;
         _geoGrid_IsGridAutoScroll = false;

         grid_DisableGridBoxSelection();

         return;
      }

      // accelerate with Ctrl + Shift key
      int offset = (event.stateMask & SWT.CTRL) != 0 ? 20 : 1;

      if (offset == 1) {
         // check if command (OSX) is set
         offset = (event.stateMask & SWT.COMMAND) != 0 ? 20 : 1;
      }
      offset *= (event.stateMask & SWT.SHIFT) != 0 ? 1 : 40;

      int xDiff = 0;
      int yDiff = 0;

      switch (event.keyCode) {
      case SWT.ARROW_LEFT:
         xDiff = _isInInverseKeyboardPanning ? -offset : offset;
         break;

      case SWT.ARROW_RIGHT:
         xDiff = _isInInverseKeyboardPanning ? offset : -offset;
         break;

      case SWT.ARROW_UP:
         yDiff = _isInInverseKeyboardPanning ? -offset : offset;
         break;

      case SWT.ARROW_DOWN:
         yDiff = _isInInverseKeyboardPanning ? offset : -offset;
         break;
      }

      switch (event.character) {
      case '+':
         zoomIn(CenterMapBy.Map);
         break;

      case '-':
         zoomOut(CenterMapBy.Map);
         break;
      }

      if (xDiff != 0 || yDiff != 0) {
         recenterMap(xDiff, yDiff);
      }
   }

   private void onMouse_DoubleClick(final MouseEvent mouseEvent) {

      if (mouseEvent.button != 1) {
         return;
      }

      if (_hoveredMapPoint != null && _hoveredMapPoint.mapPoint.photo != null) {

         // run external app 1

         // prevent map panning, this is happening
         _canPanMap = false;
         _canPanPhoto = false;

         fireEvent_RunExternalApp(1, _hoveredMapPoint.mapPoint.photo);

      } else {

         /*
          * Set new map center
          */
         final double x = _worldPixel_TopLeft_Viewport.x + mouseEvent.x;
         final double y = _worldPixel_TopLeft_Viewport.y + mouseEvent.y;

         setMapCenterInWorldPixel(new Point2D.Double(x, y));

         // ensure that all internal data are correctly setup
         setZoom(_mapZoomLevel);

         paint();
      }
   }

   private void onMouse_Down(final MouseEvent mouseEvent) {

      if (_worldPixel_TopLeft_Viewport == null) {

         // map is not yet fully initialized

         return;
      }

      final int devMouseX = mouseEvent.x;
      final int devMouseY = mouseEvent.y;

      // check context menu
      if (mouseEvent.button != 1) {

         // right mouse down is pressed -> keep position

         final Point worldMousePosition = new Point(
               _worldPixel_TopLeft_Viewport.x + devMouseX,
               _worldPixel_TopLeft_Viewport.y + devMouseY);

         final GeoPosition geoMousePosition = _mp.pixelToGeo(
               new Point2D.Double(worldMousePosition.x, worldMousePosition.y),
               _mapZoomLevel);

         _mouseDown_ContextMenu_GeoPosition = geoMousePosition;

         return;
      }

      _isMouseDown = true;

      final boolean isShiftKey = UI.isShiftKey(mouseEvent);
      final boolean isCtrlKey = UI.isCtrlKey(mouseEvent);

      hideTourTooltipHoveredArea();
      setPoiVisible(false);

      final Point devMousePosition = new Point(devMouseX, devMouseY);

      if (_isMarkerClusterSelected) {

         // check if a marker is selected

         if (_hoveredMapPoint != null) {

            // a marker is hovered

         } else {

            // switch cluster OFF

            _isMarkerClusterSelected = false;

            markerCluster_SetHoveredCluster();

            paint();
         }

      } else if (_hoveredMarkerCluster != null) {

         // switch cluster ON

         _isMarkerClusterSelected = true;

         paint();
      }

      boolean isCanPanMap = false;

      if (_offline_IsSelectingOfflineArea) {

         _offline_IsOfflineSelectionStarted = true;

         final int worldMouseX = _worldPixel_TopLeft_Viewport.x + devMouseX;
         final int worldMouseY = _worldPixel_TopLeft_Viewport.y + devMouseY;

         _offline_DevMouse_Start = devMousePosition;
         _offline_DevMouse_End = devMousePosition;

         _offline_WorldMouse_Start = new Point(worldMouseX, worldMouseY);
         _offline_WorldMouse_End = _offline_WorldMouse_Start;

         _offline_DevTileStart = offline_GetTilePosition(worldMouseX, worldMouseY);

         redraw();

      } else if (_geoGrid_Data_Hovered != null) {

         _geoGrid_Data_Hovered.isSelectionStarted = true;

         final Point worldMousePosition = new Point(
               _worldPixel_TopLeft_Viewport.x + devMouseX,
               _worldPixel_TopLeft_Viewport.y + devMouseY);

         final GeoPosition geoMousePosition = _mp.pixelToGeo(new Point2D.Double(worldMousePosition.x, worldMousePosition.y), _mapZoomLevel);
         _geoGrid_Data_Hovered.geo_Start = geoMousePosition;
         _geoGrid_Data_Hovered.geo_End = geoMousePosition;

         grid_Convert_StartEnd_2_TopLeft(geoMousePosition, geoMousePosition, _geoGrid_Data_Hovered);

         redraw();

      } else if (_isShowBreadcrumbs

            // check if breadcrumb is hit
            && _tourBreadcrumb.onMouseDown(devMousePosition)) {

         // a crumb is selected

         if (_tourBreadcrumb.isAction_RemoveAllCrumbs()) {

            // crumb action: remove all crumbs

            _tourBreadcrumb.removeAllCrumbs();

            // set tour info icon position in the map
            fireEvent_TourBreadcrumb();

            redraw();

         } else if (_tourBreadcrumb.isAction_UpliftLastCrumb()) {

            // crumb action: set last crumb to the first/top crumb

            _tourBreadcrumb.resetLastBreadcrumb();

            redraw();

         } else {

            // show bread crumb tours in the map

            final ArrayList<Long> crumbTourIds = _tourBreadcrumb.getHoveredCrumbedTours_WithReset();

            // hide crumb selection state, this must be done after the crumb is reset
            redraw();

            fireEvent_TourSelection(new SelectionTourIds(crumbTourIds));
         }

      } else if (_hoveredMapPoint != null) {

         final Photo photo = _hoveredMapPoint.mapPoint.photo;

         if (photo != null) {

            if (_isInHoveredRatingStar) {

               saveRatingStars(photo);

               // when a rating star is removed, then the photo may be filtered out -> hide hovered mappoint/photo
               _hoveredMapPoint = null;

            } else {

               // a photo is selected

               if (isCtrlKey) {

                  // deselect photo

                  selectPhoto(null, null);

               } else {

                  // a photo is hovered -> show photo tooltip

                  selectPhoto(photo, _hoveredMapPoint);
               }

               if (_isHoveredMapPointSymbol

                     // it is not yet supported to move photos when it is not a photo tour
                     && isPhotoTour(photo)) {

                  // when a photo symbol is hovered, then the photo can be panned, otherwise the map

                  isCanPanMap = false;

                  _canPanPhoto = true;
                  _pannedPhoto = photo;

                  _mouseDownPosition = devMousePosition;

                  setCursorOptimized(_cursorPhoto_Move);

               } else {

                  isCanPanMap = true;
               }
            }
         }

      } else if (_geoGrid_Label_IsHovered) {

         // set map location to the selected geo filter default position

         // hide hover color
         _geoGrid_Label_IsHovered = false;

         setCursorOptimized(_cursorDefault);

         /*
          * Zoom to geo filter zoom level
          */
         // set zoom level first, that recalculation is correct ->/ prevent recenter
         setZoom(_geoGrid_MapZoomLevel);

         /*
          * Center to geo filter position
          */
         setMapCenter(new GeoPosition(_geoGrid_MapGeoCenter.latitude, _geoGrid_MapGeoCenter.longitude));

      } else if (_geoGrid_Action_IsHovered) {

         // set selected geo filter default position to the map location

         // hide hover color
         _geoGrid_Action_IsHovered = false;

         setCursorOptimized(_cursorDefault);

         _geoGrid_TourGeoFilter.mapGeoCenter = _geoGrid_MapGeoCenter = getMapGeoCenter();
         _geoGrid_TourGeoFilter.mapZoomLevel = _geoGrid_MapZoomLevel = getZoomLevel();

      } else if (_allHoveredTourIds.size() > 0) {

         onMouse_Down_HoveredTour(isShiftKey);

      } else {

         isCanPanMap = true;
      }

      if (isCanPanMap) {

         // when the left mousebutton is clicked remember this point (for panning)
         _canPanMap = true;
         _mouseDownPosition = devMousePosition;

         setCursorOptimized(_cursorPan);
      }
   }

   /**
    * A tour is hovered, select/deselect tour or trackpoint
    *
    * @param isShiftKeyPressed
    */
   private void onMouse_Down_HoveredTour(final boolean isShiftKeyPressed) {

      ISelection tourSelection = null;

      if (_allHoveredTourIds.size() == 1) {

         // one tour is hovered -> select tour or trackpoint

         final long hoveredTourId = _allHoveredTourIds.get(0);

         if (_hoveredSelectedTour_CanSelectTour) {

            // a tour can be/is selected

            if (_hovered_SelectedTourId == -1) {

               // toggle selection -> select tour

               _hovered_SelectedTourId = hoveredTourId;

               tourSelection = new SelectionTourId(_hovered_SelectedTourId);

            } else {

               // toggle selection -> hide tour selection

               _hovered_SelectedTourId = -1;
            }

            _hovered_SelectedSerieIndex_Behind = -1;
            _hovered_SelectedSerieIndex_Front = -1;

         } else {

            // a trackpoint can be/is selected

            _hovered_SelectedTourId = hoveredTourId;

            final boolean isAnotherTourSelected = _hovered_SelectedTourId != -1 && _hovered_SelectedTourId != hoveredTourId;

            if (isAnotherTourSelected) {

               // fire a selection for a new tour

               tourSelection = new SelectionTourId(_hovered_SelectedTourId);

               _hovered_SelectedSerieIndex_Behind = -1;
               _hovered_SelectedSerieIndex_Front = -1;
            }

            final int hoveredSerieIndex = _allHoveredSerieIndices.get(0);

            if (isShiftKeyPressed) {

               // select 2nd value point

               _hovered_SelectedSerieIndex_Behind = hoveredSerieIndex;

            } else {

               // select 1st value point

               _hovered_SelectedSerieIndex_Front = hoveredSerieIndex;
            }

            /*
             * Ensure that the right index is larger than the left index
             */
            int selectedSerieIndex_Behind = _hovered_SelectedSerieIndex_Behind;
            int selectedSerieIndex_Front = _hovered_SelectedSerieIndex_Front;

            if (_hovered_SelectedSerieIndex_Front == -1) {

               selectedSerieIndex_Behind = -1;
               selectedSerieIndex_Front = _hovered_SelectedSerieIndex_Behind;

            } else if (_hovered_SelectedSerieIndex_Behind == -1) {

               // serie index in front is larger than the behind -> nothing to do

            } else if (_hovered_SelectedSerieIndex_Behind > _hovered_SelectedSerieIndex_Front) {

               final int hovered_SelectedSerieIndex_LeftBackup = _hovered_SelectedSerieIndex_Behind;

               selectedSerieIndex_Behind = _hovered_SelectedSerieIndex_Front;
               selectedSerieIndex_Front = hovered_SelectedSerieIndex_LeftBackup;
            }

            _hovered_SelectedSerieIndex_Front = selectedSerieIndex_Front;
            _hovered_SelectedSerieIndex_Behind = selectedSerieIndex_Behind;

            fireEvent_MapSelection(new SelectionMapSelection(

                  _hovered_SelectedTourId,
                  selectedSerieIndex_Behind,
                  selectedSerieIndex_Front));
         }

      } else {

         // multiple tours are selected

         // hide single tour selection
         _hovered_SelectedTourId = -1;
         _hovered_SelectedSerieIndex_Behind = -1;
         _hovered_SelectedSerieIndex_Front = -1;

         // clone tour id's becauses they will be removed
         final ArrayList<Long> allClonedTourIds = new ArrayList<>();
         allClonedTourIds.addAll(_allHoveredTourIds);

         tourSelection = new SelectionTourIds(_allHoveredTourIds);
      }

      if (tourSelection != null) {
         fireEvent_TourSelection(tourSelection);
      }

      redraw();
   }

   private void onMouse_Exit() {

      // keep position for out of the map events, e.g. recenter map
      _mouseMove_DevPosition_X_Last = _mouseMove_DevPosition_X;
      _mouseMove_DevPosition_Y_Last = _mouseMove_DevPosition_Y;

      // set position out of the map that the tool tip is not activated again
      _mouseMove_DevPosition_X = Integer.MIN_VALUE;
      _mouseMove_DevPosition_Y = Integer.MIN_VALUE;

      // stop grid autoscrolling
      _geoGrid_IsGridAutoScroll = false;
      _geoGrid_Label_IsHovered = false;

      // hide link to the hovered point
      _hoveredMapPoint = null;

      if (_isShowHoveredOrSelectedTour) {

         _tourBreadcrumb.onMouseExit();

         // reset hovered data to hide hovered tour background
         _allHoveredTourIds.clear();
         _allHoveredDevPoints.clear();
         _allHoveredSerieIndices.clear();
      }

      setCursorOptimized(_cursorDefault);

      redraw();
   }

   private void onMouse_Move(final MouseEvent mouseEvent) {

      if (_mp == null) {
         return;
      }

      final Point devMousePosition = new Point(mouseEvent.x, mouseEvent.y);

      final int mouseMoveDevX = _mouseMove_DevPosition_X = mouseEvent.x;
      final int mouseMoveDevY = _mouseMove_DevPosition_Y = mouseEvent.y;

      // keep position for out of the map events, e.g. to recenter map
      _mouseMove_DevPosition_X_Last = mouseMoveDevX;
      _mouseMove_DevPosition_Y_Last = mouseMoveDevY;

      final int worldMouseX = _worldPixel_TopLeft_Viewport.x + mouseMoveDevX;
      final int worldMouseY = _worldPixel_TopLeft_Viewport.y + mouseMoveDevY;
      final Point worldMouse_Move = new Point(worldMouseX, worldMouseY);

      final GeoPosition geoMouseMove = _mp.pixelToGeo(new Point2D.Double(worldMouseX, worldMouseY), _mapZoomLevel);
      _mouseMove_GeoPosition = geoMouseMove;

      boolean isSomethingHit = false;

      if (_offline_IsSelectingOfflineArea) {

         _offline_WorldMouse_Move = worldMouse_Move;

         offline_UpdateOfflineAreaEndPosition(mouseEvent);

         paint();

         fireEvent_MapInfo();

         return;
      }

      if (_geoGrid_Data_Hovered != null) {

         // tour geo filter is hovered

         _geoGrid_Data_Hovered.geo_MouseMove = geoMouseMove;
         grid_UpdateEndPosition(mouseEvent, _geoGrid_Data_Hovered);

         // pan map when mouse is near map border
         final Point mouseBorderPosition = grid_GetMouseBorderPosition();
         if (mouseBorderPosition != null) {

            // scroll map

            grid_DoAutoScroll(mouseBorderPosition);

            return;
         }

         paint();

         fireEvent_MapInfo();
         fireEvent_MapGrid(false, _geoGrid_Data_Hovered);

         return;
      }

      /*
       * Setup map points
       */
      _hoveredMapPoint_Previous = _hoveredMapPoint;
      _hoveredMapPoint = null;
      _isHoveredMapPointSymbol = false;

      // use a local ref otherwise the list could be modified in another thread which caused exceptions
      final List<PaintedMapPoint> allPaintedClusterMarkers = _allPaintedClusterMarkers;
      final List<PaintedMapPoint> allPaintedCommonLocations = _allPaintedCommonLocations;
      final List<PaintedMapPoint> allPaintedTourLocations = _allPaintedTourLocations;
      final List<PaintedMapPoint> allPaintedMarkers = _allPaintedMarkers;
      final List<PaintedMapPoint> allPaintedPauses = _allPaintedPauses;
      final List<PaintedMapPoint> allPaintedPhotos = _allPaintedPhotos;
      final List<PaintedMapPoint> allPaintedWayPoints = _allPaintedWayPoints;

      /*
       * Prio 1: Marker cluster
       */

      if (_allPaintedMarkerClusters.size() > 0) {

         // marker clusters are painted

         if (_isMarkerClusterSelected

               // map is not panned
               && _isMouseDown == false) {

            // keep selected and check if a cluster marker is hovered

            if (allPaintedClusterMarkers != null && allPaintedClusterMarkers.size() > 0) {

               for (final PaintedMapPoint paintedMarker : allPaintedClusterMarkers) {

                  final Rectangle paintedLabelRect = paintedMarker.labelRectangle;
                  final Rectangle paintedSymbolRect = paintedMarker.symbolRectangle;

                  if (true
                        && (mouseMoveDevX > paintedLabelRect.x)
                        && (mouseMoveDevX < paintedLabelRect.x + paintedLabelRect.width)
                        && (mouseMoveDevY > paintedLabelRect.y)
                        && (mouseMoveDevY < paintedLabelRect.y + paintedLabelRect.height)

                        || paintedSymbolRect != null
                              && (mouseMoveDevX > paintedSymbolRect.x)
                              && (mouseMoveDevX < paintedSymbolRect.x + paintedSymbolRect.width)
                              && (mouseMoveDevY > paintedSymbolRect.y)
                              && (mouseMoveDevY < paintedSymbolRect.y + paintedSymbolRect.height)

                  ) {

                     // a map marker is hovered

                     _hoveredMapPoint = paintedMarker;

                     break;
                  }
               }
            }

            redraw();

            return;
         }

         final boolean isClusterHovered = markerCluster_SetHoveredCluster();
         if (isClusterHovered) {

            if (_isMouseDown) {

               // allow map panning

            } else {

               return;
            }
         }
      }

      // prio 2a: Map common locations
      if (allPaintedCommonLocations.size() > 0) {
         onMouse_Move_CheckMapPoints(allPaintedCommonLocations, mouseMoveDevX, mouseMoveDevY);
      }

      // prio 2b: Map tour locations
      if (_hoveredMapPoint == null && allPaintedTourLocations.size() > 0) {
         onMouse_Move_CheckMapPoints(allPaintedTourLocations, mouseMoveDevX, mouseMoveDevY);
      }

      // prio 3: Tour marker
      if (_hoveredMapPoint == null && allPaintedMarkers.size() > 0) {
         onMouse_Move_CheckMapPoints(allPaintedMarkers, mouseMoveDevX, mouseMoveDevY);
      }

      // prio 4: Tour pauses
      if (_hoveredMapPoint == null && allPaintedPauses.size() > 0) {
         onMouse_Move_CheckMapPoints(allPaintedPauses, mouseMoveDevX, mouseMoveDevY);
      }

      // prio 5: Tour waypoints
      if (_hoveredMapPoint == null && allPaintedWayPoints.size() > 0) {
         onMouse_Move_CheckMapPoints(allPaintedWayPoints, mouseMoveDevX, mouseMoveDevY);
      }

      // prio 6: Photos
      if (_hoveredMapPoint == null && allPaintedPhotos.size() > 0) {

         onMouse_Move_CheckMapPoints(allPaintedPhotos, mouseMoveDevX, mouseMoveDevY);

         if (_hoveredMapPoint != null && Map2PainterConfig.isPhotoAutoSelect) {

            // a photo is hovered -> select photo

            final Photo photo = _hoveredMapPoint.mapPoint.photo;

            // select photo only when it is not yet selected
            if (_selectedPhoto != photo) {
               selectPhoto(photo, _hoveredMapPoint);
            }

         }

         if (_hoveredMapPoint != null) {

            if (_canPanPhoto) {

               setCursorOptimized(_cursorPhoto_Move);

            } else {

               setCursorOptimized(_cursorPhoto_Select);
            }
         }
      }

      if (_hoveredMapPoint != null) {

         isSomethingHit = true;

         // reset hovered data
         _allHoveredDevPoints.clear();
         _allHoveredTourIds.clear();
         _allHoveredSerieIndices.clear();
      }

      // ensure that the old map point is hidden
      if (_hoveredMapPoint_Previous != null) {
         redraw();
      }

      if (_canPanPhoto) {

         // pan photo

         panPhoto(mouseEvent);

         return;
      }

      if (_canPanMap) {

         // pan map

         panMap(mouseEvent);

         return;
      }

      // #######################################################################

      if (_tourTooltip != null && _tourTooltip.isActive()) {

         /*
          * check if the mouse is within a hovered area
          */
         boolean isContextValid = false;
         if (_tourTooltip_HoveredAreaContext != null) {

            final int topLeftX = _tourTooltip_HoveredAreaContext.hoveredTopLeftX;
            final int topLeftY = _tourTooltip_HoveredAreaContext.hoveredTopLeftY;

            if (mouseMoveDevX >= topLeftX
                  && mouseMoveDevX < topLeftX + _tourTooltip_HoveredAreaContext.hoveredWidth
                  && mouseMoveDevY >= topLeftY
                  && mouseMoveDevY < topLeftY + _tourTooltip_HoveredAreaContext.hoveredHeight) {

               isContextValid = true;
            }
         }

         if (isContextValid == false) {

            // old hovered context is not valid any more, update the hovered context
            updateTourToolTip_HoveredArea();
         }
      }

      if (_poi_Tooltip != null && _isPoiPositionInViewport) {

         // check if mouse is within the poi image
         if (_isPoiVisible
               && (mouseMoveDevX > _poiImageDevPosition.x)
               && (mouseMoveDevX < _poiImageDevPosition.x + _poiImageBounds.width)
               && (mouseMoveDevY > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
               && (mouseMoveDevY < _poiImageDevPosition.y + _poiImageBounds.height)) {

            // display poi
            showPoi();

         } else {

            setPoiVisible(false);
         }
      }

      /*
       * Check if mouse has hovered the grid label
       */
      if (isSomethingHit == false && _geoGrid_Label_Outline != null) {

         final boolean isHovered = _geoGrid_Label_IsHovered;

         _geoGrid_Label_IsHovered = false;

         if (_geoGrid_Label_Outline.contains(mouseMoveDevX, mouseMoveDevY)) {

            _geoGrid_Label_IsHovered = true;

            setCursorOptimized(_cursorHand);

            redraw();

            isSomethingHit = true;

         } else if (isHovered) {

            // hide hovered state

            setCursorOptimized(_cursorDefault);

            redraw();
         }
      }

      /*
       * Check if mouse has hovered the grid action
       */
      if (isSomethingHit == false && _geoGrid_Action_Outline != null) {

         final boolean isHovered = _geoGrid_Action_IsHovered;

         _geoGrid_Action_IsHovered = false;

         if (_geoGrid_Action_Outline.contains(mouseMoveDevX, mouseMoveDevY)) {

            _geoGrid_Action_IsHovered = true;

            setCursorOptimized(_cursorHand);

            redraw();

            isSomethingHit = true;

         } else if (isHovered) {

            // hide hovered state

            setCursorOptimized(_cursorDefault);

            redraw();
         }
      }

      if (isSomethingHit == false) {

         final int numOldHoveredTours = _allHoveredTourIds.size();

         if (_isShowBreadcrumbs && _tourBreadcrumb.onMouseMove(devMousePosition)) {

            // breadcrumb is hovered

            // do not show hovered tour info -> reset hovered data
            _allHoveredTourIds.clear();
            _allHoveredDevPoints.clear();
            _allHoveredSerieIndices.clear();

            if (_tourBreadcrumb.isCrumbHovered()) {
               setCursorOptimized(_cursorHand);
            } else {
               setCursorOptimized(_cursorDefault);
            }

            redraw();

         } else if (isTourHovered()) {

            // tour is hovered

            setCursorOptimized(_cursorSelect);

            // show hovered tour
            redraw();

         } else if (numOldHoveredTours > 0 || _tourBreadcrumb.isCrumbHovered() == false) {

            // reset cursor

            setCursorOptimized(_cursorDefault);
         }
      }

      fireEvent_MousePosition();
   }

   private void onMouse_Move_CheckMapPoints(final List<PaintedMapPoint> allPaintedMapPoints,
                                            int mouseMoveDevX,
                                            int mouseMoveDevY) {

      // first check the symbol

      mouseMoveDevX = (int) (mouseMoveDevX * _deviceScaling);
      mouseMoveDevY = (int) (mouseMoveDevY * _deviceScaling);

      for (final PaintedMapPoint paintedMapPoint : allPaintedMapPoints) {

         final Rectangle paintedSymbolRect = paintedMapPoint.symbolRectangle;

         if (paintedSymbolRect != null
               && (mouseMoveDevX > paintedSymbolRect.x)
               && (mouseMoveDevX < paintedSymbolRect.x + paintedSymbolRect.width)
               && (mouseMoveDevY > paintedSymbolRect.y)
               && (mouseMoveDevY < paintedSymbolRect.y + paintedSymbolRect.height)

         ) {

            // a map point is hovered

            _hoveredMapPoint = paintedMapPoint;

            _isHoveredMapPointSymbol = true;

            break;
         }
      }

      // second check the label/photo
      if (_hoveredMapPoint == null) {

         for (final PaintedMapPoint paintedMapPoint : allPaintedMapPoints) {

            final Rectangle paintedLabelRect = paintedMapPoint.labelRectangle;

            if (true
                  && (mouseMoveDevX > paintedLabelRect.x)
                  && (mouseMoveDevX < paintedLabelRect.x + paintedLabelRect.width)
                  && (mouseMoveDevY > paintedLabelRect.y)
                  && (mouseMoveDevY < paintedLabelRect.y + paintedLabelRect.height)) {

               // a map point is hovered

               _hoveredMapPoint = paintedMapPoint;

               final Photo photo = paintedMapPoint.mapPoint.photo;

               if (photo != null) {

                  onMouse_Move_CheckMapPoints_PhotoRatingStars(photo, photo.paintedRatingStars);
               }

               break;

            } else if (_paintedRatingStars != null

                  // this can be null when a hovered rating star was set
                  && _hoveredMapPoint_Previous != null

                  && (mouseMoveDevX > _paintedRatingStars.x)
                  && (mouseMoveDevX < _paintedRatingStars.x + _paintedRatingStars.width)
                  && (mouseMoveDevY > _paintedRatingStars.y)
                  && (mouseMoveDevY < _paintedRatingStars.y + _paintedRatingStars.height)) {

               // a rating star is hovered

               _hoveredMapPoint = _hoveredMapPoint_Previous;

               final Photo photo = _hoveredMapPoint.mapPoint.photo;

               if (photo != null) {

                  onMouse_Move_CheckMapPoints_PhotoRatingStars(photo, photo.paintedRatingStars);
               }

               break;
            }
         }
      }
   }

   private void onMouse_Move_CheckMapPoints_PhotoRatingStars(final Photo photo, final Rectangle paintedRatingStars) {

      int hoveredStars = 0;
      _isInHoveredRatingStar = false;

      if (paintedRatingStars != null) {

         final int mouseMoveDevX = (int) (_mouseMove_DevPosition_X * _deviceScaling);
         final int mouseMoveDevY = (int) (_mouseMove_DevPosition_Y * _deviceScaling);

         final int photoDevX = photo.paintedPhoto.x;
         final int photoWidth = photo.paintedPhoto.width;

         _isInHoveredRatingStar = paintedRatingStars.contains(mouseMoveDevX, mouseMoveDevY);

         // center ratings stars in the middle of the image
         final int ratingStarsLeftBorder = photoDevX
               + photoWidth / 2
               - MAX_RATING_STARS_WIDTH / 2;

         if (_isInHoveredRatingStar) {

            hoveredStars = (mouseMoveDevX - ratingStarsLeftBorder) / _ratingStarImageSize + 1;
         }
      }

      photo.hoveredStars = hoveredStars;
   }

   private void onMouse_Up(final MouseEvent mouseEvent) {

      _isMouseDown = false;

      if (_offline_IsSelectingOfflineArea) {

         _isContextMenuEnabled = false;

         if (_offline_IsOfflineSelectionStarted == false) {
            /*
             * offline selection is not started, this can happen when the right mouse button is
             * clicked
             */
            offline_DisableOfflineAreaSelection();

            return;
         }

         offline_UpdateOfflineAreaEndPosition(mouseEvent);

         // reset cursor
         setCursorOptimized(_cursorDefault);

         // hide selection
         _offline_IsSelectingOfflineArea = false;

         redraw();
         paint();

         offline_OpenOfflineImageDialog();

      } else if (_geoGrid_Data_Hovered != null) {

         // finalize grid selecting

         _isContextMenuEnabled = false;

         if (_geoGrid_Data_Hovered.isSelectionStarted == false) {

            // this can happen when the right mouse button is clicked

            _geoGrid_Data_Hovered = null;
            _geoGrid_IsGridAutoScroll = true;

            grid_DisableGridBoxSelection();

            return;
         }

         /*
          * Show selected grid box
          */

         grid_UpdateEndPosition(mouseEvent, _geoGrid_Data_Hovered);

         _geoGrid_Data_Selected = _geoGrid_Data_Hovered;

         _geoGrid_Data_Hovered = null;
         _geoGrid_IsGridAutoScroll = true;

         grid_DisableGridBoxSelection();

         redraw();
         paint();

         fireEvent_MapGrid(true, _geoGrid_Data_Selected);

      } else {

         if (mouseEvent.button == 1) {

            if (_isMapPanned) {
               _isMapPanned = false;
               redraw();
            }

            if (_isPhotoPanned) {
               _isPhotoPanned = false;
               redraw();
            }

            _mouseDownPosition = null;

            _canPanMap = false;
            _canPanPhoto = false;

            setCursorOptimized(_cursorDefault);

         } else if (mouseEvent.button == 2) {

            // if the middle mouse button is clicked, recenter the view
            // recenterMap(event.x, event.y);
         }
      }

      // show poi info when mouse is within the poi image
      if (true
            && (_mouseMove_DevPosition_X > _poiImageDevPosition.x)
            && (_mouseMove_DevPosition_X < _poiImageDevPosition.x + _poiImageBounds.width)
            && (_mouseMove_DevPosition_Y > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
            && (_mouseMove_DevPosition_Y < _poiImageDevPosition.y + _poiImageBounds.height)) {

         setPoiVisible(true);
      }

      if (_isShowHoveredOrSelectedTour) {

         // when a tour is unselected, show it hovered

         if (isTourHovered()) {

            // show hovered tour
            redraw();
         }
      }
   }

   private void onMouse_Wheel(final Event event) {

      if (event.count < 0) {

         zoomOut(_centerMapBy);

      } else {

         zoomIn(_centerMapBy);
      }
   }

   /**
    * There are far too many calls from SWT on this method. Much more than would bereally needed. I
    * don't know why this is. As a result of this, the Component uses up much CPU, because it runs
    * through all the tile loading code for every call. The tile loading code should only be called,
    * if something has changed. When something has changed we produce a buffer image with the
    * contents of the view port (Double/Triple buffer). This happens in the queueRedraw() method.
    * The image gets painted on every call of this method.
    */
   private void onPaint(final PaintEvent event) {

      // draw map image to the screen

//      final long start = System.nanoTime();

      if (_mapImage == null || _mapImage.isDisposed()) {
         return;
      }

      final GC gc = event.gc;

      gc.drawImage(_mapImage, 0, 0);

      if (_directMapPainter != null) {

         // is drawing sliders in map/legend

         _directMapPainterContext.gc = gc;
         _directMapPainterContext.clientArea = _clientArea;
         _directMapPainterContext.mapViewport = _worldPixel_TopLeft_Viewport;

         _directMapPainter.paint(_directMapPainterContext);
      }

      if (_tourTooltip_HoveredAreaContext != null) {
         final Image hoveredImage = _tourTooltip_HoveredAreaContext.hoveredImage;
         if (hoveredImage != null) {

            gc.drawImage(hoveredImage,
                  _tourTooltip_HoveredAreaContext.hoveredTopLeftX,
                  _tourTooltip_HoveredAreaContext.hoveredTopLeftY);
         }
      }

      if (_isPoiVisible && _poi_Tooltip != null) {
         if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {
            gc.drawImage(_poiImage, _poiImageDevPosition.x, _poiImageDevPosition.y);
         }
      }

      if (_offline_IsPaintOfflineArea) {
         paint_OfflineArea(gc);
      }

      final boolean isPaintTourInfo = paint_HoveredTour(gc);

      _geoGrid_Label_Outline = null;
      _geoGrid_Action_Outline = null;
      if (_geoGrid_Data_Selected != null) {
         paint_GeoGrid_10_Selected(gc, _geoGrid_Data_Selected);
      }
      if (_geoGrid_Data_Hovered != null) {
         paint_GeoGrid_20_Hovered(gc, _geoGrid_Data_Hovered);
      }

      // paint tooltip icon in the map
      if (_tourTooltip != null) {
         _tourTooltip.paint(gc, _clientArea);
      }

      // paint info AFTER hovered/selected tour
      if (isPaintTourInfo) {
         paint_HoveredTour_50_TourInfo(gc);
      }

//      final long end = System.nanoTime();
//      System.out.println(UI.timeStampNano() + " onPaint - %7.3f ms".formatted((float) (end - start) / 1000000));
   }

   private void onResize() {

      /*
       * The method getClientArea() is only correct in a dialog when it's called in the create()
       * method after super.create();
       */

      _clientArea = getClientArea();

      _mapPointImageSize = new Rectangle(

            0,
            0,

            _clientArea.width, // + 2 * MAP_OVERLAY_MARGIN,
            _clientArea.height // + 2 * MAP_OVERLAY_MARGIN
      );

      updateViewportData();

      // stop painting thread
      if (_mapPointPainter_Task != null) {

         synchronized (_mapPointPainter_Task) {

            // check again, this happens

            if (_mapPointPainter_Task != null) {

               _mapPointPainter_Task.cancel(true);

               try {

                  // wait until the task is canceled
                  _mapPointPainter_Task.get(5000, TimeUnit.MILLISECONDS);

               } catch (final Exception e) {

                  // ignore

//               StatusUtil.log(e);
               }
            }
         }
      }

      paint();
   }

   /**
    * Put a map redraw into a queue, the last entry in the queue will be executed
    */
   public void paint() {

      final int redrawCounter = _redrawMapCounter.incrementAndGet();

      // repaint the map point image
      _mapPointPainter_RunnableCounter.incrementAndGet();

      if (isDisposed() || _mp == null || _isMapPaintingEnabled == false) {
         return;
      }

      if (_devMapViewport == null) {

         // internal data are not yet initialized, this happens only the first time when a map is displayed

         initMap();
         updateViewportData();
      }

      // get time when the redraw is requested
      final long requestedRedrawTime = System.currentTimeMillis();
      final long timeDiff = requestedRedrawTime - _lastMapDrawTime;

      if (timeDiff > 100) {

         // update display even when this is not the last created runnable

         _display.syncExec(() -> {

            if (isDisposed()) {
               return;
            }

            paint_10_PaintMapImage();
         });

      } else {

         final Runnable asynchImageRunnable = new Runnable() {

            final int __asynchRunnableCounter = redrawCounter;

            @Override
            public void run() {

               if (isDisposed()) {
                  return;
               }

               // check if a newer runnable is available
               if (__asynchRunnableCounter != _redrawMapCounter.get()) {

                  // a newer runnable is available
                  return;
               }

               paint_10_PaintMapImage();
            }
         };

         _display.asyncExec(asynchImageRunnable);
      }

      // tell the overlay thread to draw the overlay images
      _nextOverlayRedrawTime = requestedRedrawTime;
   }

   /**
    * Draws map tiles/legend/scale into the map image {@link #_mapImage} which is displayed in the
    * SWT paint event.
    */
   private void paint_10_PaintMapImage() {

//      final long start = System.nanoTime();

      if (isDisposed()) {
         return;
      }

      GC gcMapImage = null;

      try {

         // check or create map image
         final Image currentMapImage = _mapImage;

         if (canReuseImage(currentMapImage, _clientArea) == false) {

            final Color fillColor = UI.IS_DARK_THEME

                  /*
                   * It looks ugly when in the dark theme the default map background is white which
                   * occur before the map tile images are painted
                   */
                  ? ThemeUtil.getDarkestBackgroundColor()
                  : null;

            _mapImage = createMapImage(_display, currentMapImage, _clientArea, fillColor);
         }

         gcMapImage = new GC(_mapImage);
         {
            paint_30_Tiles(gcMapImage);

            if (_isMapPointVisible || Map2PainterConfig.isShowPhotos) {
               paint_40_MapPoints(gcMapImage);
            }

            if (_isLegendVisible && _mapLegend != null) {
               paint_80_Legend(gcMapImage);
            }

            if (_isScaleVisible) {
               paint_90_Scale(gcMapImage);
            }

            if (_isShowDebug_GeoGrid) {
               paint_Debug_GeoGrid(gcMapImage);
            }
         }

      } catch (final Exception e) {

         StatusUtil.log(e);

         // map image is corrupt
         _mapImage.dispose();
         _mapPointImage.dispose();

      } finally {

         if (gcMapImage != null) {
            gcMapImage.dispose();
         }
      }

      redraw();

      _lastMapDrawTime = System.currentTimeMillis();

//      final long end = System.nanoTime();
//      System.out.println(UI.timeStampNano() + " paint_10_PaintMapImage() - %7.3f ms".formatted((float) (end - start) / 1000000));
   }

   /**
    * Draw all visible tiles into the map viewport
    *
    * @param gcMapImage
    */
   private void paint_30_Tiles(final GC gcMapImage) {

      for (int tilePosX = _tilePos_MinX, tileIndexX = 0; tilePosX <= _tilePos_MaxX; tilePosX++, tileIndexX++) {
         for (int tilePosY = _tilePos_MinY, tileIndexY = 0; tilePosY <= _tilePos_MaxY; tilePosY++, tileIndexY++) {

            /*
             * convert tile world position into device position
             */
            final int devTileX = tilePosX * _tilePixelSize - _worldPixel_TopLeft_Viewport.x;
            final int devTileY = tilePosY * _tilePixelSize - _worldPixel_TopLeft_Viewport.y;

            final Rectangle devTileViewport = new Rectangle(devTileX, devTileY, _tilePixelSize, _tilePixelSize);

            // check if current tile is within the painting area
            if (devTileViewport.intersects(_devMapViewport)) {

               /*
                * get the tile from the factory. the tile must not have been completely downloaded
                * after this step.
                */

               if (isTileOnMap(tilePosX, tilePosY)) {

                  final Tile paintedTile = paint_Tile(gcMapImage, tilePosX, tilePosY, devTileViewport);

                  _allPaintedTiles[tileIndexX][tileIndexY] = paintedTile;

               } else {

                  gcMapImage.setBackground(_defaultBackgroundColor);
                  gcMapImage.fillRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);
               }
            }
         }
      }
   }

   /**
    * @param gcMapImage
    */
   private void paint_40_MapPoints(final GC gcMapImage) {

      if (_mapConfig == null
            || _mapConfig.isShowTourMarker == false
                  && _mapConfig.isShowTourLocation == false
                  && _mapConfig.isShowCommonLocation == false
                  && _mapConfig.isShowTourPauses == false
                  && Map2PainterConfig.isShowPhotos == false) {

         // there is nothing which should be painted

         return;
      }

      if (_mapPointImage == null || _mapPointImage.isDisposed()) {

         // start the map point painting

         paint_MapPointImage();

         return;
      }

      // paint map point image

      try {

         // do micro adjustments otherwise panning the map is NOT smooth
         final Rectangle topLeft_Viewport_WhenPainted = _mapPointPainter_Viewport_WhenPainted;
         final Rectangle topLeft_Viewport_Current = _worldPixel_TopLeft_Viewport;

         final int diffX = topLeft_Viewport_WhenPainted.x - topLeft_Viewport_Current.x;
         final int diffY = topLeft_Viewport_WhenPainted.y - topLeft_Viewport_Current.y;

         gcMapImage.drawImage(_mapPointImage, diffX, diffY);

         _mapPointPainter_MicroAdjustment_DiffX = diffX;
         _mapPointPainter_MicroAdjustment_DiffY = diffY;

         // start the map point painting
         paint_MapPointImage();

      } catch (final Exception e) {

         StatusUtil.log(e);

         // new overlay image is corrupt
         UI.disposeResource(_mapPointImage);
      }
   }

   private void paint_80_Legend(final GC gc) {

      // get legend image from the legend
      final Image legendImage = _mapLegend.getImage();
      if ((legendImage == null) || legendImage.isDisposed()) {
         return;
      }

      final Rectangle imageBounds = legendImage.getBounds();

      // draw legend on bottom left
      int yPos = _worldPixel_TopLeft_Viewport.height - 5 - imageBounds.height;
      yPos = Math.max(5, yPos);

      final Point legendPosition = new Point(5, yPos);
      _mapLegend.setLegendPosition(legendPosition);

      gc.drawImage(legendImage, legendPosition.x, legendPosition.y);
   }

   /**
    * Paint scale for the map center
    *
    * @param gc
    */
   private void paint_90_Scale(final GC gc) {

      final int viewPortWidth = _worldPixel_TopLeft_Viewport.width;

      final int devScaleWidth = viewPortWidth / 3;
      final float metricWidth = 111.32f / _distanceUnitValue;

      //
      final GeoPosition mapCenter = getMapGeoCenter();
      final double latitude = mapCenter.latitude;
      final double longitude = mapCenter.longitude;

      final double devDistance = _mp.getDistance(
            new GeoPosition(latitude - 0.5, longitude),
            new GeoPosition(latitude + 0.5, longitude),
            _mapZoomLevel);

      final double scaleGeo = metricWidth * (devScaleWidth / devDistance);

      final double scaleGeoRounded = Util.roundDecimalValue(scaleGeo);
      final int devScaleWidthRounded = (int) (scaleGeoRounded / metricWidth * devDistance);

      // get scale text
      String scaleFormatted;
      if (scaleGeoRounded < 1f) {
         scaleFormatted = _nf2.format(scaleGeoRounded);
      } else {
         scaleFormatted = Integer.toString((int) scaleGeoRounded);
      }
      final String scaleText = scaleFormatted + UI.SPACE + _distanceUnitLabel;
      final Point textExtent = gc.textExtent(scaleText);

      final int devX1 = viewPortWidth - 5 - devScaleWidthRounded;
      final int devX2 = devX1 + devScaleWidthRounded;

      final int devY = _worldPixel_TopLeft_Viewport.height - 5 - 3;

      final int devYScaleLines = devY;

      final Path path1 = new Path(_display);
      final Path path2 = new Path(_display);
      final int offset = -1;
      {
         path1.moveTo(devX1, devY);
         path1.lineTo(devX2, devY);

         path2.moveTo(devX1, devY + offset);
         path2.lineTo(devX2, devY + offset);

         gc.setLineWidth(1);

         gc.setForeground(UI.SYS_COLOR_WHITE);
         gc.drawPath(path1);

         gc.setForeground(UI.SYS_COLOR_BLACK);
         gc.drawPath(path2);
      }
      path1.dispose();
      path2.dispose();

      final int devYText = devYScaleLines - textExtent.y;
      final int devXText = devX1 + devScaleWidthRounded - textExtent.x;

      /*
       * Paint text with shadow
       */

      Color shadeColor;
      Color textColor;

      if (_isMapBackgroundDark) {

         // dark background

         shadeColor = UI.SYS_COLOR_BLACK;
         textColor = UI.SYS_COLOR_WHITE;

      } else {

         // bright background

         shadeColor = UI.SYS_COLOR_WHITE;
         textColor = UI.SYS_COLOR_BLACK;
      }

      // draw shade
      gc.setForeground(shadeColor);
      gc.drawText(scaleText, devXText + 1, devYText, true);
      gc.drawText(scaleText, devXText - 1, devYText, true);
      gc.drawText(scaleText, devXText, devYText + 1, true);
      gc.drawText(scaleText, devXText, devYText - 1, true);

      // draw text
      gc.setForeground(textColor);
      gc.drawText(scaleText, devXText, devYText, true);
   }

   private void paint_Debug_GeoGrid(final GC gc) {

      final double geoGridPixelSizeX = _devGridPixelSize_X;
      final double geoGridPixelSizeY = _devGridPixelSize_Y;

      double geoGridPixelSizeXAdjusted = geoGridPixelSizeX;
      double geoGridPixelSizeYAdjusted = geoGridPixelSizeY;

      boolean isAdjusted = false;

      /*
       * Adjust grid size when it's too small
       */
      while (geoGridPixelSizeXAdjusted < 20) {

         geoGridPixelSizeXAdjusted *= 2;
         geoGridPixelSizeYAdjusted *= 2;

         isAdjusted = true;
      }

      final int vpWidth = _worldPixel_TopLeft_Viewport.width;
      final int vpHeight = _worldPixel_TopLeft_Viewport.height;

      int numX = (int) (vpWidth / geoGridPixelSizeXAdjusted);
      int numY = (int) (vpHeight / geoGridPixelSizeYAdjusted);

      // this can occur by high zoom level
      if (numX < 1) {
         numX = 1;
      }
      if (numY < 1) {
         numY = 1;
      }

      gc.setLineWidth(1);
      gc.setLineStyle(SWT.LINE_SOLID);

      // show different color when adjusted
      if (isAdjusted) {
         gc.setForeground(_display.getSystemColor(SWT.COLOR_RED));
      } else {
         gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
      }

      final Point devGeoGrid = offline_GetDevGridGeoPosition(_worldPixel_TopLeft_Viewport.x, _worldPixel_TopLeft_Viewport.y);
      final int topLeftX = devGeoGrid.x;
      final int topLeftY = devGeoGrid.y;

      // draw vertical lines, draw more lines as necessary otherwise sometimes they are not visible
      for (int indexX = -1; indexX < numX + 5; indexX++) {

         final int devX = (int) (topLeftX + indexX * geoGridPixelSizeXAdjusted);

         gc.drawLine(devX, 0, devX, vpHeight);
      }

      // draw horizontal lines
      for (int indexY = -1; indexY < numY + 5; indexY++) {

         final int devY = (int) (topLeftY + indexY * geoGridPixelSizeYAdjusted);

         gc.drawLine(0, devY, vpWidth, devY);
      }
   }

   private void paint_GeoGrid_10_Selected(final GC gc, final MapGridData mapGridData) {

      final Point devTopLeft = paint_GeoGrid_50_Outline(gc, mapGridData, true);

      Color fgColor;
      Color bgColor;

      /*
       * Paint label
       */
      if (_geoGrid_Label_IsHovered) {

         // label is hovered

         fgColor = UI.SYS_COLOR_BLACK;
         bgColor = UI.SYS_COLOR_WHITE;

      } else {

         // label is selected

         fgColor = UI.SYS_COLOR_BLACK;
         bgColor = UI.SYS_COLOR_GREEN;
      }

      // draw geo grid label
      _geoGrid_Label_Outline = paint_Text_Label(gc,
            devTopLeft.x,
            devTopLeft.y,
            mapGridData.gridBox_Text,
            fgColor,
            bgColor,
            false);

      /*
       * Paint action
       */
      if (_geoGrid_Label_Outline != null) {

         if (_geoGrid_Action_IsHovered) {

            // action is hovered

            fgColor = UI.SYS_COLOR_BLACK;
            bgColor = UI.SYS_COLOR_WHITE;

         } else {

            // action is selected

            fgColor = UI.SYS_COLOR_BLACK;
            bgColor = UI.SYS_COLOR_GREEN;
         }

         // draw geo grid action
         _geoGrid_Action_Outline = paint_Text_Label(gc,
               _geoGrid_Label_Outline.x + _geoGrid_Label_Outline.width + TEXT_MARGIN / 2,
               _geoGrid_Label_Outline.y + _geoGrid_Label_Outline.height + TEXT_MARGIN / 2,
               GEO_GRID_ACTION_UPDATE_GEO_LOCATION_ZOOM_LEVEL,
               fgColor,
               bgColor,
               false);
      }
   }

   private void paint_GeoGrid_20_Hovered(final GC gc, final MapGridData mapGridData) {

      final Point world_Start = mapGridData.world_Start;
      if (world_Start == null) {
         // map grid is not yet initialized
         return;
      }

      gc.setLineWidth(2);

      /*
       * show info in the top/left corner that selection for the offline area is active
       */
      paint_GeoGrid_70_Info_MouseGeoPos(gc, mapGridData);

      // check if mouse button is hit, this sets the start position
//      if (mapGridData.isSelectionStarted) {
//
//         final Point devTopLeft = paint_GridBox_50_Rectangle(gc, mapGridData, false);
//
//         paint_GridBox_80_Info_Text(gc, mapGridData, devTopLeft);
//      }

      final Point dev_Start = grid_World2Dev(world_Start);
      final Point dev_End = grid_World2Dev(mapGridData.world_End);

      final int dev_Start_X = dev_Start.x;
      final int dev_Start_Y = dev_Start.y;
      final int dev_End_X = dev_End.x;
      final int dev_End_Y = dev_End.y;

      final int dev_X1;
      final int dev_Y1;

      final int dev_Width;
      final int dev_Height;

      if (dev_Start_X < dev_End_X) {

         dev_X1 = dev_Start_X;
         dev_Width = dev_End_X - dev_Start_X;

      } else {

         dev_X1 = dev_End_X;
         dev_Width = dev_Start_X - dev_End_X;
      }

      if (dev_Start_Y < dev_End_Y) {

         dev_Y1 = dev_Start_Y;
         dev_Height = dev_End_Y - dev_Start_Y;

      } else {

         dev_Y1 = dev_End_Y;
         dev_Height = dev_Start_Y - dev_End_Y;
      }

      /*
       * Draw geo grid
       */

      final Point devTopLeft = paint_GeoGrid_50_Outline(gc, mapGridData, false);

      paint_Text_WithBorder(gc, mapGridData.gridBox_Text, devTopLeft);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawRectangle(dev_X1, dev_Y1, dev_Width, dev_Height);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(UI.SYS_COLOR_WHITE);

      gc.setBackground(UI.SYS_COLOR_YELLOW);
      gc.setAlpha(0x30);
      gc.fillRectangle(dev_X1 + 1, dev_Y1 + 1, dev_Width - 2, dev_Height - 2);
      gc.setAlpha(0xff);
   }

   /**
    * Paint a rectangle which shows a grid box
    *
    * @param gc
    * @param worldStart
    * @param worldEnd
    * @param mapGridData
    * @param gridPaintData
    * @param isPaintLastGridSelection
    *           When <code>true</code>, the last selected grid is painted, otherwise the currently
    *           selecting grid
    *
    * @return Returns top/left box position in the viewport
    */
   private Point paint_GeoGrid_50_Outline(final GC gc,
                                          final MapGridData mapGridData,
                                          final boolean isPaintLastGridSelection) {

      // x: longitude
      // y: latitude

      // draw geo grid
      final Color boxColor;
      if (isPaintLastGridSelection) {

         final RGB hoverRGB = Util.getStateRGB(_geoFilterState,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED_DEFAULT);

         boxColor = _colorCache.getColorRGB(hoverRGB);

      } else {

         final RGB hoverRGB = Util.getStateRGB(_geoFilterState,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER_DEFAULT);

         boxColor = _colorCache.getColorRGB(hoverRGB);
      }

      final int devGrid_X1 = mapGridData.devGrid_X1;
      final int devGrid_Y1 = mapGridData.devGrid_Y1;
      final int devWidth = mapGridData.devWidth;
      final int devHeight = mapGridData.devHeight;

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setLineWidth(1);

      // draw outline with selected color
      gc.setForeground(boxColor);
      gc.drawRectangle(devGrid_X1 + 1, devGrid_Y1 + 1, devWidth - 2, devHeight - 2);

      // draw dark outline to make it more visible
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawRectangle(devGrid_X1, devGrid_Y1, devWidth, devHeight);

      return new Point(devGrid_X1, devGrid_Y1);
   }

   /**
    * @param gc
    * @param mapGridData
    * @param numGridRectangle
    */
   private void paint_GeoGrid_70_Info_MouseGeoPos(final GC gc, final MapGridData mapGridData) {

      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.setBackground(UI.SYS_COLOR_YELLOW);

      final StringBuilder sb = new StringBuilder();

//      if (mapGridData.isSelectionStarted) {
//
//         // display selected area
//
//         final GeoPosition geoStart = mapGridData.geo_Start;
//         final GeoPosition geoEnd = mapGridData.geo_End;
//
//         sb.append(String.format(" %s / %s  ...  %s / %s", //$NON-NLS-1$
//               _nfLatLon.format(geoStart.latitude),
//               _nfLatLon.format(geoStart.longitude),
//               _nfLatLon.format(geoEnd.latitude),
//               _nfLatLon.format(geoEnd.longitude)));
//
//      } else {
//
      // display mouse move geo position

      sb.append(String.format(" %s / %s", //$NON-NLS-1$
            _nfLatLon.format(_mouseMove_GeoPosition.latitude),
            _nfLatLon.format(_mouseMove_GeoPosition.longitude)));
//      }

      final String infoText = sb.toString();
      final Point textSize = gc.textExtent(infoText);

      gc.drawString(
            infoText,
            _devMapViewport.width - textSize.x,
            0);
   }

   private boolean paint_HoveredTour(final GC gc) {

      Long hoveredTourId = null;
      boolean isTourHoveredAndSelected = false;
      boolean isPaintTourInfo = false;

      final int numHoveredTours = _allHoveredDevPoints.size();

      if (numHoveredTours > 0) {

         isPaintTourInfo = true;

         if (numHoveredTours == 1) {

            hoveredTourId = _allHoveredTourIds.get(0);

            if (hoveredTourId == _hovered_SelectedTourId) {
               isTourHoveredAndSelected = true;
            }
         }
      }

      /*
       * Paint selected tour or trackpoint
       */
      if (_hovered_SelectedTourId != -1) {

         if (_hoveredSelectedTour_CanSelectTour) {

            // a tour can be selected

            if (isTourHoveredAndSelected) {

               gc.setAlpha(_hoveredSelectedTour_HoveredAndSelected_Opacity);
               gc.setForeground(_hoveredSelectedTour_HoveredAndSelected_Color);

            } else {

               gc.setAlpha(_hoveredSelectedTour_Selected_Opacity);
               gc.setForeground(_hoveredSelectedTour_Selected_Color);
            }

            paint_HoveredTour_10(gc, _hovered_SelectedTourId);

         } else {

            // a trackpoint can be selected

            paint_HoveredTrackpoint_10(
                  gc,
                  _hovered_SelectedTourId,
                  _hovered_SelectedSerieIndex_Behind,
                  _hovered_SelectedSerieIndex_Front,

                  HoveredPoint_PaintMode.IS_SELECTED);
         }
      }

      /*
       * Paint hovered tour or trackpoint
       */
      if (_isShowHoveredOrSelectedTour) {

         if (numHoveredTours > 0) {

            isPaintTourInfo = true;

            if (numHoveredTours == 1) {

               if (// paint hovered tour when

               // tour is not selected
               isTourHoveredAndSelected == false

                     // or when a trackpoint is selected
                     || isTourHoveredAndSelected && _hoveredSelectedTour_CanSelectTour == false

               ) {

                  if (_hoveredSelectedTour_CanSelectTour) {

                     // a tour can be selected

                     gc.setAlpha(_hoveredSelectedTour_Hovered_Opacity);
                     gc.setForeground(_hoveredSelectedTour_Hovered_Color);

                     paint_HoveredTour_10(gc, hoveredTourId);

                  } else {

                     // a trackpoint can be selected

                     final int hoveredSerieIndex = _allHoveredSerieIndices.get(0);
                     final boolean isHoveredAndSelected = hoveredSerieIndex == _hovered_SelectedSerieIndex_Behind
                           || hoveredSerieIndex == _hovered_SelectedSerieIndex_Front;

                     paint_HoveredTrackpoint_10(
                           gc,
                           hoveredTourId,
                           hoveredSerieIndex,
                           -1,
                           isHoveredAndSelected
                                 ? HoveredPoint_PaintMode.IS_HOVERED_AND_SELECTED
                                 : HoveredPoint_PaintMode.IS_HOVERED);

                     // paint direction arrows
                     if (_isDrawTourDirection) {

                        final int[] devXYTourPositions = getReducesTourPositions(hoveredTourId);

                        if (devXYTourPositions != null) {
                           paint_HoveredTour_14_DirectionArrows(gc, devXYTourPositions);
                        }
                     }
                  }
               }
            }
         }
      }

      /*
       * Paint tour directions when not yet painted
       */
      if (_isShowTour

            && _isDrawTourDirection
            && _isDrawTourDirection_Always

            // currently only one tour is supported
            && _allTourIds != null && _allTourIds.size() == 1

            // nothing is currently hovered
            && numHoveredTours == 0

      ) {

         final Long tourId = _allTourIds.get(0);

         final int[] devXYTourPositions = getReducesTourPositions(tourId);

         if (devXYTourPositions != null) {
            paint_HoveredTour_14_DirectionArrows(gc, devXYTourPositions);
         }
      }

      /*
       * Paint breadcrumb bar
       */
      if (_isShowBreadcrumbs) {

         _tourBreadcrumb.paint(gc, _isShowBreadcrumbs);
      }

      return isPaintTourInfo;
   }

   private void paint_HoveredTour_10(final GC gc, final long tourId) {

      int[] devXYTourPositions;

      gc.setLineWidth(30);

      gc.setLineCap(SWT.CAP_ROUND);
      gc.setLineJoin(SWT.JOIN_ROUND);

      gc.setAntialias(SWT.ON);
      {
         devXYTourPositions = getReducesTourPositions(tourId);
         if (devXYTourPositions != null) {
            gc.drawPolyline(devXYTourPositions);
         }
      }
      gc.setAntialias(SWT.OFF);
      gc.setAlpha(0xff);

      if (_isDrawTourDirection && devXYTourPositions != null) {
         paint_HoveredTour_14_DirectionArrows(gc, devXYTourPositions);
      }
   }

   private void paint_HoveredTour_14_DirectionArrows(final GC gc, final int[] devXY) {

      int devX1 = devXY[0];
      int devY1 = devXY[1];

      int devX1_LastPainted = devX1;
      int devY1_LastPainted = devY1;

      final int numSegments = devXY.length / 2;

      gc.setLineWidth(_tourDirection_LineWidth);
      gc.setAntialias(SWT.ON);

      gc.setLineCap(SWT.CAP_SQUARE);
      gc.setLineJoin(SWT.JOIN_MITER);

      final Color directionColor_Symbol = new Color(_tourDirection_RGB);
      final Color directionColor_Contrast = UI.SYS_COLOR_WHITE;

      final Path directionPath_Color = new Path(_display);
      final Path directionPath_Contrast = new Path(_display);
      final Transform transform = new Transform(_display);
      {
         // draw direction symbol
         final float directionPos1 = 1 * _tourDirection_SymbolSize;
         final float directionPos2 = 0.8f * _tourDirection_SymbolSize;

         directionPath_Color.moveTo(0, directionPos1);
         directionPath_Color.lineTo(directionPos2, 0);
         directionPath_Color.lineTo(0, -directionPos1);

         directionPath_Contrast.moveTo(-1, directionPos1);
         directionPath_Contrast.lineTo(directionPos2 - 1, 0);
         directionPath_Contrast.lineTo(-1, -directionPos1);

         for (int segmentIndex = 1; segmentIndex < numSegments; segmentIndex++) {

            final int devXYIndex = segmentIndex * 2;

            final int devX2 = devXY[devXYIndex + 0];
            final int devY2 = devXY[devXYIndex + 1];

            /*
             * Skip locations which are too narrow
             */
            int xDiff;
            int yDiff;

            if (devX1_LastPainted > devX2) {
               xDiff = devX1_LastPainted - devX2;
            } else {
               xDiff = devX2 - devX1_LastPainted;
            }

            if (devY1_LastPainted > devY2) {
               yDiff = devY1_LastPainted - devY2;
            } else {
               yDiff = devY2 - devY1_LastPainted;
            }

            if (xDiff > _tourDirection_MarkerGap || yDiff > _tourDirection_MarkerGap

            // paint 1st direction arrow
                  || segmentIndex == 1) {

               // paint direction arrow

               final float directionRotation = (float) MtMath.angleOf(devX1, devY1, devX2, devY2);

// when debugging then autoScapeUp provided the wrong values when using a 4k display !!!
//               final int xPos1 = DPIUtil.autoScaleUp(devX1);
//               final int yPos1 = DPIUtil.autoScaleUp(devY1);

               final int xPos1 = devX1;
               final int yPos1 = devY1;

               // VERY IMPORTANT: Reset previous positions !!!
               transform.identity();

               transform.translate(xPos1, yPos1);
               transform.rotate(-directionRotation);

               gc.setTransform(transform);

               gc.setForeground(directionColor_Contrast);
               gc.drawPath(directionPath_Contrast);

               gc.setForeground(directionColor_Symbol);
               gc.drawPath(directionPath_Color);

               // keep last painted position
               devX1_LastPainted = devX1;
               devY1_LastPainted = devY1;
            }

            // advance to the next segment
            devX1 = devX2;
            devY1 = devY2;
         }
      }
      directionPath_Color.dispose();
      directionPath_Contrast.dispose();
      transform.dispose();

      gc.setTransform(null);
   }

   /**
    * Show number of tours, e.g. "Tours 21" when tours are hovered
    *
    * @param gc
    */
   private void paint_HoveredTour_50_TourInfo(final GC gc) {

      /*
       * This is for debugging
       */
//      final boolean isShowHoverRectangle = true;
//      if (isShowHoverRectangle) {
//
//         // paint hovered rectangle
//         gc.setLineWidth(1);
//
//         for (final Point hoveredPoint : _allHoveredDevPoints) {
//
//            gc.setAlpha(0x60);
//            gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//            gc.fillRectangle(
//                  hoveredPoint.x,
//                  hoveredPoint.y,
//                  EXPANDED_HOVER_SIZE,
//                  EXPANDED_HOVER_SIZE);
//
//            gc.setAlpha(0xff);
//            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
//            gc.drawRectangle(
//                  hoveredPoint.x,
//                  hoveredPoint.y,
//                  EXPANDED_HOVER_SIZE,
//                  EXPANDED_HOVER_SIZE);
//         }
//      }

      final int devXMouse = _mouseMove_DevPosition_X;
      final int devYMouse = _mouseMove_DevPosition_Y;

      final int numHoveredTours = _allHoveredTourIds.size();
      final int numDisplayedTours = _allTourIds != null
            ? _allTourIds.size()
            : 0;

      if (numHoveredTours == 1) {

         // one tour is hovered -> show tour details

         if (numDisplayedTours > 1) {

            // multiple tours are displayed

            final TourData tourData = TourManager.getTour(_allHoveredTourIds.get(0));

            if (tourData == null) {

               // this occurred, it can be that previously a history/multiple tour was displayed

            } else {

               // tour data are available

               paint_HoveredTour_52_OneTour(gc, devXMouse, devYMouse, tourData, _allHoveredSerieIndices.get(0));
            }

         } else {

            // just one tour is displayed

            // ** tour track info can be displayed in the value point tooltip
            // ** tour info can be displayed with the tour info tooltip
         }

      } else {

         // multiple tours are hovered -> show number of hovered tours

         paint_HoveredTour_54_MultipleTours(gc, devXMouse, devYMouse, _allHoveredTourIds);
      }
   }

   private void paint_HoveredTour_52_OneTour(final GC gc,
                                             final int devXMouse,
                                             final int devYMouse,
                                             final TourData tourData,
                                             final int hoveredSerieIndex) {

      final Font gcFontBackup = gc.getFont();

      final String text_TourDateTime = TourManager.getTourDateTimeFull(tourData);
      final String text_TourTitle = tourData.getTourTitle();
      final boolean isTourTitle = text_TourTitle.length() > 0;

      final long movingTime = tourData.getTourComputedTime_Moving();
      final long recordedTime = tourData.getTourDeviceTime_Recorded();
      final float distance = tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE;
      final int elevationUp = tourData.getTourAltUp();

      /*
       * Tour values
       */
      final String text_TourMovingTime = String.format(Messages.Map2_TourTooltip_Time,
            OtherMessages.TOUR_TOOLTIP_LABEL_MOVING_TIME,
            FormatManager.formatMovingTime(movingTime));

      final String text_TourRecordedTime = String.format(Messages.Map2_TourTooltip_Time,
            OtherMessages.TOUR_TOOLTIP_LABEL_RECORDED_TIME,
            FormatManager.formatMovingTime(recordedTime));

      final String text_TourDistance = String.format(Messages.Map2_TourTooltip_Distance,
            OtherMessages.TOUR_TOOLTIP_LABEL_DISTANCE,
            FormatManager.formatDistance(distance / 1000.0),
            UI.UNIT_LABEL_DISTANCE);

      final String text_ElevationUp = String.format(Messages.Map2_TourTooltip_Elevation,
            OtherMessages.TOUR_TOOLTIP_LABEL_ELEVATION_UP,
            FormatManager.formatElevation(elevationUp),
            UI.UNIT_LABEL_ELEVATION);

      /*
       * Track values
       */

      // trackpoint

      final StringBuilder sb = new StringBuilder();

      sb.append(NL);
      sb.append(text_TourDateTime + NL);
      sb.append(NL);
      sb.append(text_TourDistance + NL);
      sb.append(text_ElevationUp + NL);
      sb.append(text_TourMovingTime + NL);
      sb.append(text_TourRecordedTime);

      final Point size_DateTime = gc.textExtent(text_TourDateTime);
      final Point size_Values = gc.textExtent(sb.toString());

      Point size_Title = new Point(0, 0);
      String wrappedTitle = null;
      int titleHeight = 0;

      if (isTourTitle) {

         wrappedTitle = WordUtils.wrap(text_TourTitle, 40);

         gc.setFont(_boldFontSWT);
         size_Title = gc.textExtent(wrappedTitle);

         titleHeight = size_Title.y;
      }

      final int lineHeight = size_DateTime.y;

      final int marginHorizontal = 3;
      final int marginVertical = 1;

      final int contentWidth = Math.max(Math.max(size_DateTime.x, size_Values.x), size_Title.x);
      final int contentHeight = 0
            + (isTourTitle ? titleHeight : -lineHeight)
            + size_Values.y;

      final int detailWidth = contentWidth + marginHorizontal * 2;
      final int detailHeight = contentHeight + marginVertical * 2;

      final int marginAboveMouse = 50;
      final int marginBelowMouse = 40;

      int devXDetail = devXMouse + 20;
      int devYDetail = devYMouse - marginAboveMouse;

      // ensure that the tour detail is fully visible
      final int viewportWidth = _devMapViewport.width;
      if (devXDetail + detailWidth > viewportWidth) {
         devXDetail = viewportWidth - detailWidth;
      }
      if (devYDetail - detailHeight < 0) {
         devYDetail = devYMouse + detailHeight + marginBelowMouse;
      }

      final Rectangle clippingRect = new Rectangle(
            devXDetail,
            devYDetail - detailHeight,
            detailWidth,
            detailHeight);

      gc.setClipping(clippingRect);

      gc.setBackground(ThemeUtil.getDefaultBackgroundColor_Shell());
      gc.fillRectangle(clippingRect);

      gc.setForeground(ThemeUtil.getDefaultForegroundColor_Shell());

      final int devX = devXDetail + marginHorizontal;
      int devY = devYDetail - contentHeight - marginVertical;

      if (isTourTitle) {

         gc.setFont(_boldFontSWT);
         gc.drawText(wrappedTitle, devX, devY);

         devY += titleHeight;

      } else {

         devY -= lineHeight;
      }

      gc.setFont(gcFontBackup);
      gc.drawText(sb.toString(), devX, devY);
   }

   private void paint_HoveredTour_54_MultipleTours(final GC gc,
                                                   final int devXMouse,
                                                   final int devYMouse,
                                                   final List<Long> allHoveredTourIds) {

      final StringBuilder sb = new StringBuilder();

      // Show number of hovered tours
      sb.append(String.format("%d %s", //$NON-NLS-1$
            allHoveredTourIds.size(),
            Messages.Map2_Hovered_Tours));
      sb.append(NL);

      for (final Long tourId : allHoveredTourIds) {

         final TourData tourData = TourManager.getTour(tourId);
         if (tourData == null) {
            continue;
         }

         final ZonedDateTime tourStartTime = tourData.getTourStartTime();
         final long movingTime = tourData.getTourComputedTime_Moving();
         final float distance = tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE;
         final int elevationUp = tourData.getTourAltUp();

         final String text_TourDateTime = String.format("%s\t%s", //$NON-NLS-1$
               tourStartTime.format(TimeTools.Formatter_DateTime_S),
               tourStartTime.format(TimeTools.Formatter_Weekday));

         final String text_TourMovingTime = FormatManager.formatMovingTime(movingTime);
         final String text_TourDistance = String.format("%s %s", //$NON-NLS-1$
               FormatManager.formatDistance(distance / 1000.0),
               UI.UNIT_LABEL_DISTANCE);

         final String text_ElevationUp = String.format("%s %s", //$NON-NLS-1$
               FormatManager.formatElevation(elevationUp),
               UI.UNIT_LABEL_ELEVATION);
         /*
          * Combine tour values
          */
         sb.append(NL);
         sb.append(String.format("%s\t%s\t%8s\t%10s", //$NON-NLS-1$
               text_TourDateTime,
               text_TourMovingTime,
               text_TourDistance,
               text_ElevationUp));

      }

      final Point contentSize = gc.textExtent(sb.toString());

      final int marginHorizontal = 3;
      final int marginVertical = 1;

      final int contentWidth = contentSize.x;
      final int contentHeight = contentSize.y;

      final int tooltipWidth = contentWidth + marginHorizontal * 2;
      final int tooltipHeight = contentHeight + marginVertical * 2;

      final int marginAroundMouse = 20;

      int devXTooltip = devXMouse + marginAroundMouse;
      int devYTooltip = devYMouse - marginAroundMouse;

      /*
       * Ensure that the tour detail is fully visible
       */
      final int viewportWidth = _devMapViewport.width;
      final int viewportHeight = _devMapViewport.height;

      if (devXTooltip + tooltipWidth > viewportWidth) {

         // tooltip is truncated at the right side -> move tooltip to the left of the mouse

         devXTooltip = devXMouse - marginAroundMouse - tooltipWidth;
      }

      if (devYTooltip - tooltipHeight < 0) {

         // tooltip is truncated at the top -> move tooltip below the mouse

         devYTooltip = devYMouse + tooltipHeight + marginAroundMouse;

         if (devYTooltip + tooltipHeight > viewportHeight) {

            // tooltip is truncated at the bottom -> snap to the top

            devYTooltip = tooltipHeight;
         }
      }

      final int devX = devXTooltip + marginHorizontal;
      final int devY = devYTooltip - contentHeight - marginVertical;

      /*
       * Paint tooltip
       */
      final Rectangle clippingRect = new Rectangle(
            devXTooltip,
            devYTooltip - tooltipHeight,
            tooltipWidth,
            tooltipHeight);

      gc.setClipping(clippingRect);

      gc.setBackground(ThemeUtil.getDefaultBackgroundColor_Shell());
      gc.fillRectangle(clippingRect);

      gc.setForeground(ThemeUtil.getDefaultForegroundColor_Shell());
      gc.drawText(sb.toString(), devX, devY);
   }

   private void paint_HoveredTrackpoint_10(final GC gc,
                                           final long tourId,
                                           final int serieIndex1,
                                           final int serieIndex2,
                                           final HoveredPoint_PaintMode valuePoint_PaintMode) {

      final TourData tourData = TourManager.getTour(tourId);

      if (tourData == null) {

         // this occurred, it can be that previously a history/multiple tour was displayed
         return;
      }

      gc.setAntialias(SWT.ON);

      final MP mp = getMapProvider();
      final int zoomLevel = getZoomLevel();

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final Rectangle worldPosition_Viewport = _worldPixel_TopLeft_Viewport;

      final int dotWidth = 30;
      final int dotWidth2 = dotWidth / 2;

      int serieIndexFront;
      int serieIndexBehind;
      if (serieIndex1 > serieIndex2) {
         serieIndexFront = serieIndex1;
         serieIndexBehind = serieIndex2;
      } else {
         serieIndexFront = serieIndex2;
         serieIndexBehind = serieIndex1;
      }

      // loop: paint 2 points
      for (int paintIndex = 0; paintIndex < 2; paintIndex++) {

         final int serieIndex;
         boolean isFirstIndex = false;

         if (paintIndex == 0) {
            if (serieIndexFront == -1) {
               continue;
            } else {
               serieIndex = serieIndexFront;
            }
            isFirstIndex = true;
         } else {
            if (serieIndexBehind == -1) {
               continue;
            } else {
               serieIndex = serieIndexBehind;
            }
         }

         // fix: java.lang.ArrayIndexOutOfBoundsException: Index 15091 out of bounds for length 8244
         // this could not be reproduced, it happened when 2 tours were displayed/selected after a geo search
         if (serieIndex >= latitudeSerie.length) {
            continue;
         }

         // get world position for the current lat/lon
         final java.awt.Point worldPosAWT = mp.geoToPixel(
               new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
               zoomLevel);

         // convert world position into device position
         final int devX = worldPosAWT.x - worldPosition_Viewport.x;
         final int devY = worldPosAWT.y - worldPosition_Viewport.y;

         /*
          * Paint colored margin around a point
          */
         gc.setAlpha(_hoveredSelectedTour_Hovered_Opacity);
         gc.setBackground(_hoveredSelectedTour_Hovered_Color);

         if (_prefOptions_IsDrawSquare) {

            gc.fillRectangle(
                  devX - dotWidth2,
                  devY - dotWidth2,
                  dotWidth,
                  dotWidth);
         } else {

            gc.fillOval(
                  devX - dotWidth2,
                  devY - dotWidth2,
                  dotWidth,
                  dotWidth);
         }

         // paint original symbol but with a more contrast color

         /*
          * Fill symbol
          */
         gc.setAlpha(0xff);

         int symbolSize = _prefOptions_LineWidth;
         int symbolSize2 = symbolSize / 2;

         int paintedDevX = devX - symbolSize2;
         int paintedDevY = devY - symbolSize2;

         if (valuePoint_PaintMode.equals(HoveredPoint_PaintMode.IS_SELECTED)) {

            // paint selected

            gc.setBackground(isFirstIndex
                  ? UI.SYS_COLOR_GREEN
                  : UI.SYS_COLOR_DARK_GREEN);

         } else if (valuePoint_PaintMode.equals(HoveredPoint_PaintMode.IS_HOVERED_AND_SELECTED)) {

            gc.setBackground(UI.SYS_COLOR_RED);

         } else {

            // paint hovered

            gc.setBackground(UI.SYS_COLOR_WHITE);
         }

         if (_prefOptions_IsDrawSquare) {
            gc.fillRectangle(paintedDevX, paintedDevY, symbolSize, symbolSize);
         } else {
            gc.fillOval(paintedDevX, paintedDevY, symbolSize, symbolSize);
         }

         /*
          * Draw symbol border
          */
         gc.setLineWidth(_prefOptions_BorderWidth);

         symbolSize = _prefOptions_LineWidth + (_prefOptions_BorderWidth * 1);
         symbolSize2 = symbolSize / 2;

         // without this offset the border is not at the correct position
         final int oddOffset = symbolSize % 2 == 0 ? 0 : 1;

         paintedDevX = devX - symbolSize2 - oddOffset;
         paintedDevY = devY - symbolSize2 - oddOffset;

         gc.setForeground(UI.SYS_COLOR_BLACK);

         if (_prefOptions_IsDrawSquare) {
            gc.drawRectangle(paintedDevX, paintedDevY, symbolSize, symbolSize);
         } else {
            gc.drawOval(paintedDevX, paintedDevY, symbolSize, symbolSize);
         }
      }

      gc.setAntialias(SWT.OFF);
   }

   private void paint_MapPointImage() {

      final int currentMapPointImageCounter = _mapPointPainter_RunnableCounter.get();

      if (_mapPointPainter_LastCounter == currentMapPointImageCounter) {

         // counter is not incremented -> nothing to do

         return;
      }

      if (_mapPointPainter_Task != null) {

         // an overlay task is currently running

         final boolean isDone = _mapPointPainter_Task.isDone();

         if (isDone) {

            // this can happen when changing e.g. the map dimm level

         } else {

            // this case happened but the future was not set to null

            return;
         }
      }

      final Runnable mapPointTask = () -> {

         try {

            _mapPointPainter_LastCounter = _mapPointPainter_RunnableCounter.get();

            paint_MapPointImage_10_Runnable();

         } finally {

            _mapPointPainter_Task = null;
         }

         // redraw map image with the updated map point image
         getDisplay().asyncExec(() -> paint_10_PaintMapImage());

         /*
          * Paint again when there are viewport differences, this will fix e.g. the zoom in issue
          * where some markers are not painted
          */
//         final Rectangle topLeft_Viewport_WhenPainted = _backgroundPainter_Viewport_WhenPainted;
//         final Rectangle topLeft_Viewport_Current = _worldPixel_TopLeft_Viewport;
//
//         final int diffX = topLeft_Viewport_WhenPainted.x - topLeft_Viewport_Current.x;
//         final int diffY = topLeft_Viewport_WhenPainted.y - topLeft_Viewport_Current.y;
//
//         final boolean isCounterIncremented = _backgroundPainter_LastCounter != _backgroundPainter_RunnableCounter.get();
//
//         if (diffX != 0 || diffY != 0 || isCounterIncremented) {
//
//            getDisplay().asyncExec(() -> paint_10_PaintMapImage());
//         }

         /*
          * Update statistics
          */
         Map2PointManager.updateStatistics(new MapPointStatistics(

               _allPaintedCommonLocations.size(),
               _numStatistics_AllCommonLocations,

               _allPaintedTourLocations.size(),
               _numStatistics_AllTourLocations,

               _allPaintedMarkers.size(),
               _numStatistics_AllTourMarkers,
               _numStatistics_AllTourMarkers_IsTruncated,

               _allPaintedPauses.size(),
               _numStatistics_AllTourPauses,
               _numStatistics_AllTourPauses_IsTruncated,

               _allPaintedPhotos.size(),
               _numStatistics_AllTourPhotos,
               _numStatistics_AllTourPhotos_IsTruncated,

               _allPaintedWayPoints.size(),
               _numStatistics_AllTourWayPoints,
               _numStatistics_AllTourWayPoints_IsTruncated)

         );
      };

      /*
       * With Eclipse 4.35 this must be run in the UI thread otherwise this exception occurs
       * org.eclipse.swt.SWTException: Invalid thread access
       */
      setupPainting_SWT();

      _mapPointPainter_Task = _mapPointPainter_Executor.submit(mapPointTask);
   }

   private void paint_MapPointImage_10_Runnable() {

      if (_mapPointImageSize.width == 0 || _mapPointImageSize.height == 0) {

         // this happend, the UI is propably not yet fully initialized

         return;
      }

      /*
       * Setup common values
       */
      _mapConfig = Map2ConfigManager.getActiveConfig();

      _mapPointPainter_Viewport_DuringPainting = _worldPixel_TopLeft_Viewport;

      if (_colorSwitchCounter++ % 50 == 0) {
         // use different colors each time
         _locationBoundingBoxColors.clear();
      }

      _labelRespectMargin = _mapConfig.labelRespectMargin;
      _mapPointSymbolSize = _mapConfig.locationSymbolSize;
      _mapPointSymbolRespectSize = _mapPointSymbolSize + 2;

// SET_FORMATTING_OFF

      _numStatistics_AllCommonLocations         = 0;
      _numStatistics_AllTourLocations           = 0;
      _numStatistics_AllTourMarkers             = 0;
      _numStatistics_AllTourPauses              = 0;
      _numStatistics_AllTourPhotos              = 0;
      _numStatistics_AllTourWayPoints           = 0;

      _numStatistics_AllTourMarkers_IsTruncated = false;
      _numStatistics_AllTourPauses_IsTruncated  = false;
      _numStatistics_AllTourPhotos_IsTruncated  = false;

      final List<PaintedMapPoint>      allPaintedCommonLocations  = new ArrayList<>();
      final List<PaintedMapPoint>      allPaintedTourLocations    = new ArrayList<>();

      final List<PaintedMapPoint>      allPaintedMarkers          = new ArrayList<>();
      final List<PaintedMapPoint>      allPaintedClusterMarkers   = new ArrayList<>();
      final List<PaintedMarkerCluster> allPaintedMarkerClusters   = new ArrayList<>();
      final List<PaintedMapPoint>      allPaintedPauses           = new ArrayList<>();
      final List<PaintedMapPoint>      allPaintedPhotos           = new ArrayList<>();
      final List<PaintedMapPoint>      allPaintedWayPoints        = new ArrayList<>();

// SET_FORMATTING_ON

      try {

         _deviceScaling = DPIUtil.getDeviceZoom() / 100f;

         final BufferedImage awtImage = new BufferedImage(
               (int) (_mapPointImageSize.width * _deviceScaling),
               (int) (_mapPointImageSize.height * _deviceScaling),
               BufferedImage.TYPE_4BYTE_ABGR);

         final Graphics2D g2d = awtImage.createGraphics();
         try {

            setupPainting(g2d);

            // clone list to prevent concurrency exceptions, this happened
            final List<TourData> allTourData = new ArrayList<>(Map2PainterConfig.getTourData());

            if (_mapConfig.isShowTourMarker && _mapConfig.isTourMarkerClustered) {

               paint_MapPointImage_30_MapPointsAndCluster(g2d,
                     allTourData,
                     allPaintedCommonLocations,
                     allPaintedTourLocations,
                     allPaintedMarkers,
                     allPaintedMarkerClusters,
                     allPaintedPauses,
                     allPaintedPhotos,
                     allPaintedWayPoints);

            } else {

               paint_MapPointImage_20_MapPoints(g2d,
                     allTourData,
                     allPaintedCommonLocations,
                     allPaintedTourLocations,
                     allPaintedMarkers,
                     allPaintedPauses,
                     allPaintedPhotos,
                     allPaintedWayPoints);
            }

            if (_hoveredMarkerCluster != null) {

               /*
                * Paint hovered cluster marker at the end, over other markers but put the painted
                * locations at the beginning that they are hit before the other !!!
                */

               paint_MapPointImage_40_HoveredCluster(g2d,
                     _hoveredMarkerCluster,
                     allPaintedClusterMarkers);
            }

         } finally {
            g2d.dispose();
         }

         final Image swtImage = new Image(getDisplay(), new NoAutoScalingImageDataProvider(awtImage));

         /*
          * This may be needed to be synchronized ?
          */
         final Image oldImage = _mapPointImage;

         _mapPointImage = swtImage;

         UI.disposeResource(oldImage);

         _mapPointPainter_Viewport_WhenPainted = _mapPointPainter_Viewport_DuringPainting;

         _allPaintedCommonLocations = allPaintedCommonLocations;
         _allPaintedTourLocations = allPaintedTourLocations;
         _allPaintedMarkers = allPaintedMarkers;
         _allPaintedMarkerClusters = allPaintedMarkerClusters;
         _allPaintedClusterMarkers = allPaintedClusterMarkers;
         _allPaintedPauses = allPaintedPauses;
         _allPaintedPhotos = allPaintedPhotos;
         _allPaintedWayPoints = allPaintedWayPoints;

         // reset state which can happen when map is moved and no cluster is displayed
         if (_isMarkerClusterSelected && allPaintedMarkerClusters.size() == 0) {
            _isMarkerClusterSelected = false;
         }

         /*
          * Cleanup images, they cannot be disposed in the UI thread otherwise there are tons of
          * exceptions when the map image is resized
          */
         if (_disposableMapPointImagesSWT.size() > 0) {

            synchronized (_disposableMapPointImagesSWT) {

               for (final Image image : _disposableMapPointImagesSWT) {
                  if (image != null) {
                     image.dispose();
                  }
               }
            }
         }

         /*
          * Preload photo images in HQ
          */
         if (_isPreloadHQImages) {

            PhotoLoadManager.stopImageLoading(true);

            final ImageQuality requestedImageQuality = ImageQuality.HQ;

            for (final PaintedMapPoint paintedMapPoint : allPaintedPhotos) {

               final Photo photo = paintedMapPoint.mapPoint.photo;

               // check if image has an loading error
               final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

               if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

                  // image is not yet loaded

                  // check if image is in the cache
                  final Image photoImage = PhotoImageCache.getImage_SWT(photo, requestedImageQuality);

                  if ((photoImage == null || photoImage.isDisposed())
                        && photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

                     // the requested image is not available in the image cache -> image must be loaded

                     PhotoLoadManager.putImageInLoadingQueueHQ_Map(
                           photo,
                           requestedImageQuality,
                           _photoImageLoaderCallback);
                  }
               }
            }
         }

      } catch (final Exception e) {

         StatusUtil.log(e);
      }
   }

   private void paint_MapPointImage_20_MapPoints(final Graphics2D g2d,
                                                 final List<TourData> allTourData,
                                                 final List<PaintedMapPoint> allPaintedCommonLocations,
                                                 final List<PaintedMapPoint> allPaintedTourLocations,
                                                 final List<PaintedMapPoint> allPaintedMarkers,
                                                 final List<PaintedMapPoint> allPaintedPauses,
                                                 final List<PaintedMapPoint> allPaintedPhotos,
                                                 final List<PaintedMapPoint> allPaintedWayPoints) {

      final List<Map2Point> allCreatedCommonLocationPoints = new ArrayList<>();
      final List<Map2Point> allCreatedTourLocationPoints = new ArrayList<>();
      final List<Map2Point> allCreatedMarkerPoints = new ArrayList<>();
      final List<Map2Point> allCreatedPausesPoints = new ArrayList<>();
      final List<Map2Point> allCreatedPhotoPoints = new ArrayList<>();
      final List<Map2Point> allCreatedWayPointPoints = new ArrayList<>();

      /*
       * Create map points
       */
      if (_isMapPointVisible && _mapConfig.isShowTourMarker) {
         createMapPoints_TourMarkers(allTourData, allCreatedMarkerPoints);
      }

      if (_isMapPointVisible && _mapConfig.isShowTourPauses) {
         createMapPoints_TourPauses(allTourData, allCreatedPausesPoints);
      }

      if (_isMapPointVisible && _mapConfig.isShowCommonLocation) {
         createMapPoints_Locations_50_FromCommonLocations(_allCommonLocations, allCreatedCommonLocationPoints);
      }

      if (_isMapPointVisible && _mapConfig.isShowTourLocation) {
         createMapPoints_Locations_10_FromTourData(allTourData, allCreatedTourLocationPoints);
         createMapPoints_Locations_20_FromTourLocations(_allTourLocations, allCreatedTourLocationPoints);
      }

      if (_isMapPointVisible && _mapConfig.isShowTourWayPoint) {
         createMapPoints_TourWayPoints(allTourData, allCreatedWayPointPoints);
      }

      if (Map2PainterConfig.isShowPhotos) {
         createMapPoints_TourPhotos(Map2PainterConfig.getPhotos(), allCreatedPhotoPoints);
      }

      /*
       * Paint all collected map points
       */
      if (allCreatedMarkerPoints.size() > 0
            || allCreatedCommonLocationPoints.size() > 0
            || allCreatedTourLocationPoints.size() > 0
            || allCreatedPausesPoints.size() > 0
            || allCreatedPhotoPoints.size() > 0
            || allCreatedWayPointPoints.size() > 0) {

         final Map2Point[] allCommonLocationPoints = allCreatedCommonLocationPoints.toArray(new Map2Point[allCreatedCommonLocationPoints.size()]);
         final Map2Point[] allTourLocationPoints = allCreatedTourLocationPoints.toArray(new Map2Point[allCreatedTourLocationPoints.size()]);
         final Map2Point[] allMarkerPoints = allCreatedMarkerPoints.toArray(new Map2Point[allCreatedMarkerPoints.size()]);
         final Map2Point[] allPausePoints = allCreatedPausesPoints.toArray(new Map2Point[allCreatedPausesPoints.size()]);
         final Map2Point[] allPhotoPoints = allCreatedPhotoPoints.toArray(new Map2Point[allCreatedPhotoPoints.size()]);
         final Map2Point[] allWayPointPoints = allCreatedWayPointPoints.toArray(new Map2Point[allCreatedWayPointPoints.size()]);

         paint_MapPointImage_50_AllCollectedItems(g2d,

               allCommonLocationPoints,
               allPaintedCommonLocations,

               allTourLocationPoints,
               allPaintedTourLocations,

               allMarkerPoints,
               allPaintedMarkers,

               allPausePoints,
               allPaintedPauses,

               allPhotoPoints,
               allPaintedPhotos,

               allWayPointPoints,
               allPaintedWayPoints,

               false, // isPaintClusterMarker
               null // allClusterRectangle
         );
      }
   }

   private void paint_MapPointImage_30_MapPointsAndCluster(final Graphics2D g2d,
                                                           final List<TourData> allTourData,
                                                           final List<PaintedMapPoint> allPaintedCommonLocations,
                                                           final List<PaintedMapPoint> allPaintedTourLocations,
                                                           final List<PaintedMapPoint> allPaintedMarkers,
                                                           final List<PaintedMarkerCluster> allPaintedMarkerClusters,
                                                           final List<PaintedMapPoint> allPaintedPauses,
                                                           final List<PaintedMapPoint> allPaintedPhotos,
                                                           final List<PaintedMapPoint> allPaintedWayPoints) {

      final Map<String, Map2Point> allMarkersOnlyMap = new HashMap<>();
      final List<Map2Point> allMarkersOnlyList = new ArrayList<>();
      final List<StaticCluster<?>> allClustersOnly = new ArrayList<>();
      final List<Rectangle> allClusterSymbolRectangleOnly = new ArrayList<>();

      if (_mapConfig.isShowTourMarker) {

         final int clusterGridSize = (int) ScreenUtils.getPixels(_mapConfig.clusterGridSize);

         final List<Map2Point> allMapPoints = new ArrayList<>();

         createMapPoints_TourMarkers(allTourData, allMapPoints);

         // convert MapPoints's into ClusterItem's
         final List<ClusterItem> allClusterItems = new ArrayList<>();
         allClusterItems.addAll(allMapPoints);

         _distanceClustering.clearItems();
         _distanceClustering.addItems(allClusterItems);

         final Set<? extends Cluster<ClusterItem>> allMarkerAndCluster = _distanceClustering.getClusters(_mapZoomLevel, clusterGridSize);

         // get clusters and markers
         for (final Cluster<ClusterItem> item : allMarkerAndCluster) {

            if (item instanceof final StaticCluster staticCluster) {

               // item is a cluster

               allClustersOnly.add(staticCluster);

            } else if (item instanceof final QuadItem markerItem) {

               // item is a marker

               if (markerItem.mClusterItem instanceof final Map2Point mapMarker) {

                  allMarkersOnlyMap.put(mapMarker.ID, mapMarker);
               }
            }
         }

         /*
          * Resort markers to the original sequence, otherwise they are displayed with random
          * label positions !!!
          */
         if (allMarkersOnlyMap.size() > 0) {

            for (final Map2Point mapPoint : allMapPoints) {

               final Map2Point mapPointInMap = allMarkersOnlyMap.get(mapPoint.ID);

               if (mapPointInMap != null) {
                  allMarkersOnlyList.add(mapPoint);
               }
            }
         }
      }

      final List<Map2Point> allCommonLocationPointList = new ArrayList<>();
      final List<Map2Point> allTourLocationPointList = new ArrayList<>();
      final List<Map2Point> allPausesPointsList = new ArrayList<>();
      final List<Map2Point> allPhotoPointsList = new ArrayList<>();
      final List<Map2Point> allWayPointPointsList = new ArrayList<>();

      if (_mapConfig.isShowTourPauses) {
         createMapPoints_TourPauses(allTourData, allPausesPointsList);
      }

      if (_mapConfig.isShowCommonLocation) {
         createMapPoints_Locations_50_FromCommonLocations(_allCommonLocations, allCommonLocationPointList);
      }

      if (_mapConfig.isShowTourLocation) {
         createMapPoints_Locations_10_FromTourData(allTourData, allTourLocationPointList);
         createMapPoints_Locations_20_FromTourLocations(_allTourLocations, allTourLocationPointList);
      }

      if (Map2PainterConfig.isShowPhotos) {
         createMapPoints_TourPhotos(Map2PainterConfig.getPhotos(), allPhotoPointsList);
      }

      /*
       * Prepare marker cluster
       */
      if (allClustersOnly.size() > 0) {

         // font MUST be set before string.extend() !!!
         g2d.setFont(_clusterFontAWT);

         for (final StaticCluster<?> staticCluster : allClustersOnly) {

            final int numClusterItems = staticCluster.getSize();

            final PaintedMarkerCluster paintedCluster = paint_MapPointImage_42_OneCluster_Setup(
                  g2d,
                  staticCluster,
                  Integer.toString(numClusterItems),
                  allPaintedMarkerClusters);

            if (paintedCluster != null) {
               allClusterSymbolRectangleOnly.add(paintedCluster.clusterSymbolRectangle);
            }
         }

         g2d.setFont(_labelFontAWT);
      }

      /*
       * Paint map points
       */
      final int numCommonLocations = allCommonLocationPointList.size();
      final int numTourLocations = allTourLocationPointList.size();
      final int numMarkers = allMarkersOnlyList.size();
      final int numPauses = allPausesPointsList.size();
      final int numPhotos = allPhotoPointsList.size();
      final int numWayPoints = allWayPointPointsList.size();

      if (numMarkers > 0
            || numCommonLocations > 0
            || numTourLocations > 0
            || numPauses > 0) {

         final Map2Point[] allCommonLocationPoints = allCommonLocationPointList.toArray(new Map2Point[numCommonLocations]);
         final Map2Point[] allTourLocationPoints = allTourLocationPointList.toArray(new Map2Point[numTourLocations]);
         final Map2Point[] allMarkerPoints = allMarkersOnlyList.toArray(new Map2Point[numMarkers]);
         final Map2Point[] allPausePoints = allPausesPointsList.toArray(new Map2Point[numPauses]);
         final Map2Point[] allPhotoPoints = allPhotoPointsList.toArray(new Map2Point[numPhotos]);
         final Rectangle[] allClusterRectangle = allClusterSymbolRectangleOnly.toArray(new Rectangle[allClusterSymbolRectangleOnly.size()]);
         final Map2Point[] allWayPointPoints = allWayPointPointsList.toArray(new Map2Point[numWayPoints]);

         paint_MapPointImage_50_AllCollectedItems(g2d,

               allCommonLocationPoints,
               allPaintedCommonLocations,

               allTourLocationPoints,
               allPaintedTourLocations,

               allMarkerPoints,
               allPaintedMarkers,

               allPausePoints,
               allPaintedPauses,

               allPhotoPoints,
               allPaintedPhotos,

               allWayPointPoints,
               allPaintedWayPoints,

               false,
               allClusterRectangle);
      }

      /*
       * Paint cluster at the top
       */
      if (allPaintedMarkerClusters.size() > 0) {

         g2d.setFont(_clusterFontAWT);

         for (final PaintedMarkerCluster paintedCluster : allPaintedMarkerClusters) {

            paint_MapPointImage_60_OneCluster_Paint(g2d, paintedCluster);
         }

         g2d.setFont(_labelFontAWT);
      }
   }

   /**
    * Highligh the cluster and show its markers
    *
    * @param g2d
    * @param hoveredMarkerCluster
    * @param allPaintedMarkerPoints
    */
   private void paint_MapPointImage_40_HoveredCluster(final Graphics2D g2d,
                                                      final PaintedMarkerCluster hoveredMarkerCluster,
                                                      final List<PaintedMapPoint> allPaintedMarkerPoints) {

      final Map2Point[] allClusterMarkerPoints = hoveredMarkerCluster.allClusterMarker;
      final int numAllMarkers = allClusterMarkerPoints.length;

      if (numAllMarkers == 0) {
         return;
      }

      final Rectangle clusterRectangle = hoveredMarkerCluster.clusterSymbolRectangle;

      final int numPlacedLabels = paint_MapPointImage_50_AllCollectedItems(g2d,

            null, // common locations
            null,

            null, // tour locations
            null,

            allClusterMarkerPoints, // tour markers
            allPaintedMarkerPoints,

            null, // pauses
            null,

            null, // photos
            null,

            null, // way points
            null,

            true, // isPaintClusterMarker
            new Rectangle[] { clusterRectangle });

      if (_isMarkerClusterSelected) {
         return;
      }

      /*
       * Draw number of painted labels which can be different to the cluster labels
       */

      final int diffX = _mapPointPainter_MicroAdjustment_DiffX;
      final int diffY = _mapPointPainter_MicroAdjustment_DiffY;

      final int devX = clusterRectangle.x + diffX;
      final int devY = clusterRectangle.y + diffY;

      // the background must be filled because another number could be displayed
      g2d.setColor(_mapConfig.clusterOutline_ColorAWT);

      g2d.fillOval(

            devX,
            devY,

            clusterRectangle.width + 1,
            clusterRectangle.height + 1);

      // must be set BEFORE stringExtent !!!
      g2d.setFont(_clusterFontAWT);

      final FontMetrics fontMetrics = g2d.getFontMetrics();

      final String clusterLabel = Integer.toString(numPlacedLabels);

      final int textWidth = fontMetrics.stringWidth(clusterLabel);
      final int textAscent = fontMetrics.getAscent();
      final int textDescent = fontMetrics.getDescent();

      final int textWidth2 = textWidth / 2;
      final int textAscent2 = textAscent / 2;
      final int textDescent2 = textDescent / 2;

      final int margin = _mapConfig.clusterSymbol_Size;
      final int circleSize = textWidth + margin;
      final int circleSize2 = circleSize / 2;

      // center number in the cluster symbol
      final int clusterLabelDevX = devX + circleSize2 - textWidth2;
      final int clusterLabelDevY = devY + circleSize2 + textAscent2 - textDescent2;

      g2d.setColor(_mapConfig.clusterFill_ColorAWT);
      g2d.drawString(clusterLabel, clusterLabelDevX, clusterLabelDevY);

      g2d.setFont(_labelFontAWT);
   }

   private PaintedMarkerCluster paint_MapPointImage_42_OneCluster_Setup(final Graphics2D g2d,
                                                                        final StaticCluster<?> markerCluster,
                                                                        final String clusterLabel,
                                                                        final List<PaintedMarkerCluster> allPaintedClusters) {

      // convert marker lat/long into world pixels

      final GeoPoint geoPoint = markerCluster.getPosition();
      final GeoPosition geoPosition = new GeoPosition(geoPoint.getLatitude(), geoPoint.getLongitude());

      final java.awt.Point worldPixel_MarkerPos = _mp.geoToPixel(geoPosition, _mapZoomLevel);

      final int worldPixel_MarkerPosX = worldPixel_MarkerPos.x;
      final int worldPixel_MarkerPosY = worldPixel_MarkerPos.y;

      final Rectangle worldPixel_Viewport = _mapPointPainter_Viewport_DuringPainting;

      final boolean isClusterInViewport = worldPixel_Viewport.contains(worldPixel_MarkerPosX, worldPixel_MarkerPosY);

      if (isClusterInViewport == false) {
         return null;
      }

      // convert world position into device position
      int devX = worldPixel_MarkerPosX - worldPixel_Viewport.x;
      int devY = worldPixel_MarkerPosY - worldPixel_Viewport.y;

      devX = (int) (devX * _deviceScaling);
      devY = (int) (devY * _deviceScaling);

      final FontMetrics fontMetrics = g2d.getFontMetrics();

      final int textWidth = fontMetrics.stringWidth(clusterLabel);
      final int textAscent = fontMetrics.getAscent();
      final int textDescent = fontMetrics.getDescent();

      final int textWidth2 = textWidth / 2;
      final int textAscent2 = textAscent / 2;
      final int textDescent2 = textDescent / 2;

      final int margin = _mapConfig.clusterSymbol_Size;

      final int circleSize = textWidth + margin;
      final int circleSize2 = circleSize / 2;

      devX = devX - circleSize2;
      devY = devY - circleSize2;

      final int ovalDevX = devX;
      final int ovalDevY = devY;

      final int clusterLabelDevX = devX + circleSize2 - textWidth2;
      final int clusterLabelDevY = devY + circleSize2 + textAscent2 - textDescent2;

      final Rectangle paintedClusterRectangle = new Rectangle(

            ovalDevX,
            ovalDevY,

            circleSize,
            circleSize);

      final PaintedMarkerCluster paintedCluster = new PaintedMarkerCluster(

            markerCluster,
            paintedClusterRectangle,

            clusterLabel,

            clusterLabelDevX,
            clusterLabelDevY);

      // keep cluster painting data
      allPaintedClusters.add(paintedCluster);

      return paintedCluster;
   }

   /**
    * @param g2d
    * @param allCommonLocationPoints
    * @param allPaintedCommonLocationsPoints
    * @param allTourLocationPoints
    * @param allPaintedTourLocationsPoints
    * @param allMarkerPoints
    * @param allPaintedMarkerPoints
    * @param allPausePoints
    * @param allPaintedPauses
    * @param allPhotoPoints
    * @param allPaintedPhotos
    * @param allWayPointPoints
    * @param allPaintedWayPoints
    * @param isPaintClusterMarker
    * @param allClusterSymbolRectangle
    *
    * @return
    */
   private int paint_MapPointImage_50_AllCollectedItems(final Graphics2D g2d,

                                                        final Map2Point[] allCommonLocationPoints,
                                                        final List<PaintedMapPoint> allPaintedCommonLocationsPoints,

                                                        final Map2Point[] allTourLocationPoints,
                                                        final List<PaintedMapPoint> allPaintedTourLocationsPoints,

                                                        final Map2Point[] allMarkerPoints,
                                                        final List<PaintedMapPoint> allPaintedMarkerPoints,

                                                        final Map2Point[] allPausePoints,
                                                        final List<PaintedMapPoint> allPaintedPauses,

                                                        final Map2Point[] allPhotoPoints,
                                                        final List<PaintedMapPoint> allPaintedPhotos,

                                                        final Map2Point[] allWayPointPoints,
                                                        final List<PaintedMapPoint> allPaintedWayPoints,

                                                        final boolean isPaintClusterMarker,
                                                        final Rectangle[] allClusterSymbolRectangle) {

      final int mapPointRespectSize2 = _mapPointSymbolRespectSize / 2;

      final Rectangle clientArea = _clientArea;

      final int mapWidth = (int) (clientArea.width * _deviceScaling);
      final int mapHeight = (int) (clientArea.height * _deviceScaling);

      /*
       * Setup labels for the label spreader
       */
      final int numAllMarkers = allMarkerPoints.length;
      final int numAllCommonLocations = allCommonLocationPoints == null ? 0 : allCommonLocationPoints.length;
      final int numAllTourLocations = allTourLocationPoints == null ? 0 : allTourLocationPoints.length;
      final int numAllPauses = allPausePoints == null ? 0 : allPausePoints.length;
      final int numAllPhotos = allPhotoPoints == null ? 0 : allPhotoPoints.length;
      final int numAllWayPoints = allWayPointPoints == null ? 0 : allWayPointPoints.length;

      final List<PointFeature> allCommonLocationLabels = new ArrayList<>(numAllCommonLocations);
      final List<PointFeature> allTourLocationLabels = new ArrayList<>(numAllTourLocations);
      final List<PointFeature> allMarkerLabels = new ArrayList<>(numAllMarkers);
      final List<PointFeature> allPauseLabels = new ArrayList<>(numAllPauses);
      final List<PointFeature> allPhotoItems = new ArrayList<>(numAllPhotos);
      final List<PointFeature> allWayPointLabels = new ArrayList<>(numAllWayPoints);

      final List<List<PointFeature>> allDistributedItems = new ArrayList<>();

      if (numAllCommonLocations > 0) {

         createLabelSpreaderLabels(g2d, allCommonLocationPoints, allCommonLocationLabels);

         allDistributedItems.add(allCommonLocationLabels);
      }

      if (numAllTourLocations > 0) {

         createLabelSpreaderLabels(g2d, allTourLocationPoints, allTourLocationLabels);

         allDistributedItems.add(allTourLocationLabels);
      }

      if (numAllMarkers > 0) {

         createLabelSpreaderLabels(g2d, allMarkerPoints, allMarkerLabels);

         allDistributedItems.add(allMarkerLabels);
      }

      if (numAllPauses > 0) {

         createLabelSpreaderLabels(g2d, allPausePoints, allPauseLabels);

         allDistributedItems.add(allPauseLabels);
      }

      if (numAllWayPoints > 0) {

         createLabelSpreaderLabels(g2d, allWayPointPoints, allWayPointLabels);

         allDistributedItems.add(allWayPointLabels);
      }

      if (numAllPhotos > 0) {

         createLabelSpreaderPhotos(g2d, allPhotoPoints, allPhotoItems);

         allDistributedItems.add(allPhotoItems);
      }

      /*
       * Set label distributor parameters
       */
      PointFeatureLabeler.setSpiralRadius(_mapConfig.labelDistributorRadius);

      _labelSpreader.loadDataPriority(allDistributedItems,

            0, //          left
            mapWidth, //   right
            0, //          top
            mapHeight //   bottom
      );

      /*
       * Prevent that marker clusters are overwritten
       */
      if (allClusterSymbolRectangle != null) {

         for (final Rectangle clusterRectangle : allClusterSymbolRectangle) {

            if (isMapPointPainterInterrupted()) {
               return 0;
            }

            final int circleBorder2 = _clusterSymbolBorder / 2;

            final float circleRadius = clusterRectangle.width + _clusterSymbolBorder;
            final float circleRadius2 = circleRadius / 2;

            final float circleX = clusterRectangle.x + circleRadius2 - circleBorder2;
            final float circleY = clusterRectangle.y + circleRadius2 - circleBorder2;

            _labelSpreader.respectCircle(
                  circleX, //  x-coordinate of center point
                  circleY, //  y-coordinate of center point
                  circleRadius2);
         }
      }

      /**
       * Prevent that geo locations are overwritten
       * <p>
       * !!! This performs 3 time slower than without but for 200 max visible markers, it is OK !!!
       */
      if (numAllCommonLocations > 0) {

         final int locationRespectWidth = _imageMapLocationBounds.x;
         final int locationRespectHeight = _imageMapLocationBounds.y;
         final int locationRespectWidth2 = locationRespectWidth / 2;

         for (int itemIndex = 0; itemIndex < numAllCommonLocations; itemIndex++) {

            if (isMapPointPainterInterrupted()) {
               return 0;
            }

            final PointFeature distribLabel = allCommonLocationLabels.get(itemIndex);

            final Map2Point mapPoint = (Map2Point) distribLabel.data;

            final int locationSymbolDevX = mapPoint.geoPointDevX - locationRespectWidth2;
            final int locationSymbolDevY = mapPoint.geoPointDevY - locationRespectHeight;

            _labelSpreader.respectBox(
                  locationSymbolDevX,
                  locationSymbolDevY,
                  locationRespectWidth,
                  locationRespectHeight);
         }
      }

      if (numAllTourLocations > 0) {

         final int locationRespectWidth = _imageMapLocationBounds.x;
         final int locationRespectHeight = _imageMapLocationBounds.y;
         final int locationRespectWidth2 = locationRespectWidth / 2;

         for (int itemIndex = 0; itemIndex < numAllTourLocations; itemIndex++) {

            if (isMapPointPainterInterrupted()) {
               return 0;
            }

            final PointFeature distribLabel = allTourLocationLabels.get(itemIndex);

            final Map2Point mapPoint = (Map2Point) distribLabel.data;

            final int locationSymbolDevX = mapPoint.geoPointDevX - locationRespectWidth2;
            final int locationSymbolDevY = mapPoint.geoPointDevY - locationRespectHeight;

            _labelSpreader.respectBox(
                  locationSymbolDevX,
                  locationSymbolDevY,
                  locationRespectWidth,
                  locationRespectHeight);
         }
      }

      for (int itemIndex = 0; itemIndex < numAllMarkers; itemIndex++) {

         if (isMapPointPainterInterrupted()) {
            return 0;
         }

         final PointFeature distribLabel = allMarkerLabels.get(itemIndex);

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final int symbolDevX = mapPoint.geoPointDevX - mapPointRespectSize2;
         final int symbolDevY = mapPoint.geoPointDevY - mapPointRespectSize2;

         _labelSpreader.respectBox(
               symbolDevX,
               symbolDevY,
               _mapPointSymbolRespectSize,
               _mapPointSymbolRespectSize);
      }

      for (int itemIndex = 0; itemIndex < numAllPauses; itemIndex++) {

         if (isMapPointPainterInterrupted()) {
            return 0;
         }

         final PointFeature distribLabel = allPauseLabels.get(itemIndex);

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final int symbolDevX = mapPoint.geoPointDevX - mapPointRespectSize2;
         final int symbolDevY = mapPoint.geoPointDevY - mapPointRespectSize2;

         _labelSpreader.respectBox(
               symbolDevX,
               symbolDevY,
               _mapPointSymbolRespectSize,
               _mapPointSymbolRespectSize);
      }

      for (int itemIndex = 0; itemIndex < numAllWayPoints; itemIndex++) {

         if (isMapPointPainterInterrupted()) {
            return 0;
         }

         final PointFeature distribLabel = allWayPointLabels.get(itemIndex);

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final int symbolDevX = mapPoint.geoPointDevX - mapPointRespectSize2;
         final int symbolDevY = mapPoint.geoPointDevY - mapPointRespectSize2;

         _labelSpreader.respectBox(
               symbolDevX,
               symbolDevY,
               _mapPointSymbolRespectSize,
               _mapPointSymbolRespectSize);
      }

      for (int itemIndex = 0; itemIndex < numAllPhotos; itemIndex++) {

         if (isMapPointPainterInterrupted()) {
            return 0;
         }

         final PointFeature distribLabel = allPhotoItems.get(itemIndex);

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final int symbolDevX = mapPoint.geoPointDevX - mapPointRespectSize2;
         final int symbolDevY = mapPoint.geoPointDevY - mapPointRespectSize2;

         _labelSpreader.respectBox(
               symbolDevX,
               symbolDevY,
               _mapPointSymbolRespectSize,
               _mapPointSymbolRespectSize);
      }

      /*
       * Run label spreader, the resulting label positions are stored within the point-features
       */
      final int numPlacedLabels = _labelSpreader.label_StandardPipelineAll();
//    final int numPlacedLabels = _labelDistributor.label_StandardPipelineAdjacentAll();

      if (isMapPointPainterInterrupted()) {
         return 0;
      }

      g2d.setStroke(new BasicStroke(1));

      /*
       * Draw location label
       */
      paint_MpImage_10_AllLocationLabels(g2d, numAllCommonLocations, allCommonLocationLabels, allPaintedCommonLocationsPoints);
      paint_MpImage_10_AllLocationLabels(g2d, numAllTourLocations, allTourLocationLabels, allPaintedTourLocationsPoints);

      /*
       * Paint other items
       */
      paint_MpImage_30_AllMarker(g2d,
            numAllMarkers,
            isPaintClusterMarker,
            allMarkerLabels,
            allPaintedMarkerPoints);

      paint_MpImage_40_AllPauses(g2d,
            numAllPauses,
            allPauseLabels,
            allPaintedPauses);

      paint_MpImage_50_AllWayPoints(g2d,
            numAllWayPoints,
            allWayPointLabels,
            allPaintedWayPoints);

      paint_MpImage_80_AllPhotos(g2d,
            allPhotoItems,
            allPaintedPhotos);

      /*
       * Draw location symbol
       */
      paint_MpImage_20_AllLocationSymbols(g2d, numAllCommonLocations, allCommonLocationLabels, allPaintedCommonLocationsPoints);
      paint_MpImage_20_AllLocationSymbols(g2d, numAllTourLocations, allTourLocationLabels, allPaintedTourLocationsPoints);

      // FOR DEBUGGING
      //
//    _labelSpreader.drawParticles(gc);
//    _labelSpreader.drawSpiral(gc, (int) circleX, (int) circleY);

      return numPlacedLabels;
   }

   private void paint_MapPointImage_60_OneCluster_Paint(final Graphics2D g2d,
                                                        final PaintedMarkerCluster paintedCluster) {

      final boolean isPaintBackground = _isMarkerClusterSelected ? false : true;

      final Rectangle clusterSymbolRectangle = paintedCluster.clusterSymbolRectangle;

      final int symbolDevX = clusterSymbolRectangle.x;
      final int symbolDevY = clusterSymbolRectangle.y;
      final int circleSize = clusterSymbolRectangle.width;

      if (isPaintBackground && _mapConfig.isFillClusterSymbol) {

         g2d.setColor(_mapConfig.clusterFill_ColorAWT);
         g2d.fillOval(

               symbolDevX,
               symbolDevY,

               circleSize,
               circleSize);
      }

      g2d.setColor(_mapConfig.clusterOutline_ColorAWT);

      final int outlineWidth = _mapConfig.clusterOutline_Width;

      if (outlineWidth > 0) {

         g2d.setStroke(new BasicStroke(outlineWidth));
         g2d.drawOval(

               symbolDevX,
               symbolDevY,

               circleSize,
               circleSize);
      }

      g2d.drawString(
            paintedCluster.clusterLabel,
            paintedCluster.clusterLabelDevX,
            paintedCluster.clusterLabelDevY);
   }

   private void paint_MpImage_10_AllLocationLabels(final Graphics2D g2d,
                                                   final int numVisibleLocations,
                                                   final List<PointFeature> allLocationLabels,
                                                   final List<PaintedMapPoint> allPaintedLocationsPoints) {

      final FontMetrics fontMetrics = g2d.getFontMetrics();
      final int textHeight = fontMetrics.getHeight();

      for (int itemIndex = 0; itemIndex < numVisibleLocations; itemIndex++) {

         final PointFeature distribLabel = allLocationLabels.get(itemIndex);

         // check if label is displayed
         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         // paint location bounding box
         if (_mapConfig.isShowLocationBoundingBox) {

            g2d.setColor(mapPoint.boundingBox_ColorAWT);

            // draw original bbox
            final java.awt.Rectangle rect1 = mapPoint.boundingBoxAWT;
            g2d.drawRect(rect1.x, rect1.y, rect1.width, rect1.height);

            final java.awt.Rectangle rect2 = mapPoint.boundingBox_ResizedAWT;
            if (rect2 != null) {

               // draw resized bbox
               g2d.drawRect(rect2.x, rect2.y, rect2.width, rect2.height);
            }
         }

         // draw location label
         final String text = mapPoint.getFormattedLabel();

         final int textWidth = fontMetrics.stringWidth(text);

         final int labelDevX = (int) distribLabel.labelBoxL;
         final int labelDevY = (int) distribLabel.labelBoxT;

         final Rectangle labelRectangle = new Rectangle(
               labelDevX,
               labelDevY,
               textWidth,
               textHeight);

         paint_MpImage_90_OneLabel(
               g2d,
               mapPoint,
               labelRectangle,
               allPaintedLocationsPoints);
      }
   }

   private void paint_MpImage_20_AllLocationSymbols(final Graphics2D g2d,
                                                    final int numVisibleLocations,
                                                    final List<PointFeature> allLocationLabels,
                                                    final List<PaintedMapPoint> allPaintedLocationsPoints) {
      int paintedLocationIndex = 0;

      final int imageWidth = _imageMapLocationBounds.x;
      final int imageHeight = _imageMapLocationBounds.y;
      final int imageWidth2 = imageWidth / 2;

      for (int itemIndex = 0; itemIndex < numVisibleLocations; itemIndex++) {

         final PointFeature distribLabel = allLocationLabels.get(itemIndex);

         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final int locationDevX = mapPoint.geoPointDevX;
         final int locationDevY = mapPoint.geoPointDevY;

         final int iconDevX = locationDevX - imageWidth2;
         final int iconDevY = locationDevY - imageHeight;

         // set rectangle from the icon image
         final Rectangle paintedRectangle = new Rectangle(

               iconDevX,
               iconDevY,

               imageWidth,
               imageHeight);

         // draw location image

         final int numDuplicates_Start = mapPoint.numDuplicates_Start;
         final int numDuplicates_End = mapPoint.numDuplicates_End;

         if (_isMarkerClusterSelected) {

            if (_isMapBackgroundDark) {

               g2d.drawImage(_imageMapLocation_Disabled_Dark, iconDevX, iconDevY, null);

            } else {

               g2d.drawImage(_imageMapLocation_Disabled, iconDevX, iconDevY, null);
            }

         } else {

            if (numDuplicates_Start > 0 && numDuplicates_End > 0) {

               // start & end location

               g2d.drawImage(_imageMapLocation_TourEnd, iconDevX, iconDevY, null);
               g2d.drawImage(_imageMapLocation_TourStart, iconDevX, iconDevY, null);

            } else if (numDuplicates_Start > 0) {

               // start location

               g2d.drawImage(_imageMapLocation_TourStart, iconDevX, iconDevY, null);

            } else if (numDuplicates_End > 0) {

               // end location

               g2d.drawImage(_imageMapLocation_TourEnd, iconDevX, iconDevY, null);

            } else {

               if (mapPoint.locationType.equals(LocationType.Common)) {

                  // common location

                  g2d.drawImage(_imageMapLocation_Common, iconDevX, iconDevY, null);

               } else {

                  // other location

                  g2d.drawImage(_imageMapLocation_Tour, iconDevX, iconDevY, null);
               }
            }
         }

         // keep painted symbol position
         final PaintedMapPoint paintedMarker = allPaintedLocationsPoints.get(paintedLocationIndex++);
         paintedMarker.symbolRectangle = paintedRectangle;
      }
   }

   private void paint_MpImage_30_AllMarker(final Graphics2D g2d,
                                           final int numVisibleMarkers,
                                           final boolean isPaintClusterMarker,
                                           final List<PointFeature> allMarkerItems,
                                           final List<PaintedMapPoint> allPaintedMarkerPoints) {

      final FontMetrics fontMetrics = g2d.getFontMetrics();
      final int textHeight = fontMetrics.getHeight();

      for (int itemIndex = 0; itemIndex < numVisibleMarkers; itemIndex++) {

         final PointFeature distribLabel = allMarkerItems.get(itemIndex);

         // check if label is displayed
         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final String text = mapPoint.getFormattedLabel();

         final int textWidth = fontMetrics.stringWidth(text);

         final int labelDevX = (int) distribLabel.labelBoxL;
         final int labelDevY = (int) distribLabel.labelBoxT;

         final Rectangle markerLabelRectangle = new Rectangle(
               labelDevX,
               labelDevY,
               textWidth,
               textHeight);

         if (isPaintClusterMarker) {

            // paint a cluster marker, all markers within one cluster

            final int devX = labelDevX;
            final int devY = labelDevY + textHeight - fontMetrics.getDescent();

            // fill label background
            g2d.setColor(_mapConfig.tourMarkerFill_ColorAWT);
            g2d.fillRect(markerLabelRectangle.x, markerLabelRectangle.y, markerLabelRectangle.width, markerLabelRectangle.height);

            g2d.fillRect(
                  markerLabelRectangle.x - MAP_POINT_BORDER,
                  markerLabelRectangle.y,
                  markerLabelRectangle.width + 2 * MAP_POINT_BORDER,
                  markerLabelRectangle.height);

            g2d.setColor(_mapConfig.tourMarkerOutline_ColorAWT);

            // border: horizontal bottom
            g2d.drawLine(
                  labelDevX,
                  labelDevY + textHeight,
                  labelDevX + textWidth - 1,
                  labelDevY + textHeight);

            // marker label
            g2d.drawString(text, devX, devY);

            // keep painted positions
            allPaintedMarkerPoints.add(new PaintedMapPoint(mapPoint, markerLabelRectangle));

         } else {

            // paint a normal marker

            paint_MpImage_90_OneLabel(
                  g2d,
                  mapPoint,
                  markerLabelRectangle,
                  allPaintedMarkerPoints);
         }
      }

      /*
       * Draw marker symbol
       */

      final int symbolSize2 = _mapPointSymbolSize / 2;
      final int lineWidth = _mapPointSymbolSize / 4;
      final int lineWidth2 = lineWidth / 2;

      g2d.setStroke(new BasicStroke(lineWidth));

      java.awt.Color fillColor;
      java.awt.Color outlineColor;

      if (_isMarkerClusterSelected) {

         // all other labels are disable -> display grayed out

         fillColor = java.awt.Color.WHITE;
         outlineColor = _isMapBackgroundDark ? java.awt.Color.GRAY : java.awt.Color.WHITE;

      } else {

         fillColor = _mapConfig.tourMarkerOutline_ColorAWT;
         outlineColor = _mapConfig.tourMarkerFill_ColorAWT;
      }

      int paintedMarkerIndex = 0;

      for (int itemIndex = 0; itemIndex < numVisibleMarkers; itemIndex++) {

         final PointFeature distribLabel = allMarkerItems.get(itemIndex);

         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         if (_isMarkerClusterSelected == false) {

            // use map point color

            fillColor = mapPoint.getFillColorAWT();
            outlineColor = mapPoint.getOutlineColorAWT();
         }

         final int mapPointDevX = mapPoint.geoPointDevX;
         final int mapPointDevY = mapPoint.geoPointDevY;

         final int markerSymbolDevX = mapPointDevX - symbolSize2;
         final int markerSymbolDevY = mapPointDevY - symbolSize2;

         final Rectangle symbolRectangle = new Rectangle(
               markerSymbolDevX,
               markerSymbolDevY,
               _mapPointSymbolSize,
               _mapPointSymbolSize);

         g2d.setColor(fillColor);
         g2d.fillRect(symbolRectangle.x, symbolRectangle.y, symbolRectangle.width, symbolRectangle.height);

         g2d.setColor(outlineColor);
         g2d.drawRect(
               symbolRectangle.x + lineWidth2,
               symbolRectangle.y + lineWidth2,
               symbolRectangle.width - lineWidth,
               symbolRectangle.height - lineWidth);

         // keep painted symbol position
         final PaintedMapPoint paintedMarker = allPaintedMarkerPoints.get(paintedMarkerIndex++);
         paintedMarker.symbolRectangle = symbolRectangle;
      }
   }

   private void paint_MpImage_40_AllPauses(final Graphics2D g2d,
                                           final int numVisibleItems,
                                           final List<PointFeature> allPauseItems,
                                           final List<PaintedMapPoint> allPaintedPoints) {

      /*
       * Draw pause label
       */
      final FontMetrics fontMetrics = g2d.getFontMetrics();
      final int textHeight = fontMetrics.getHeight();

      for (int itemIndex = 0; itemIndex < numVisibleItems; itemIndex++) {

         final PointFeature distribLabel = allPauseItems.get(itemIndex);

         // check if label is displayed
         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final String text = mapPoint.getFormattedLabel();

         final int textWidth = fontMetrics.stringWidth(text);

         final int labelDevX = (int) distribLabel.labelBoxL;
         final int labelDevY = (int) distribLabel.labelBoxT;

         final Rectangle pauseLabelRectangle = new Rectangle(
               labelDevX,
               labelDevY,
               textWidth,
               textHeight);

         paint_MpImage_90_OneLabel(
               g2d,
               mapPoint,
               pauseLabelRectangle,
               allPaintedPoints);
      }

      /*
       * Draw pause symbol
       */
      final int symbolSize2 = _mapPointSymbolSize / 2;
      final int lineWidth = _mapPointSymbolSize / 4;
      final int lineWidth2 = lineWidth / 2;

      g2d.setStroke(new BasicStroke(lineWidth));

      java.awt.Color fillColor;
      java.awt.Color outlineColor;

      if (_isMarkerClusterSelected) {

         // all other labels are disable -> display grayed out

         fillColor = java.awt.Color.WHITE;
         outlineColor = _isMapBackgroundDark ? java.awt.Color.GRAY : java.awt.Color.WHITE;

      } else {

         fillColor = _mapConfig.tourPauseOutline_ColorAWT;
         outlineColor = _mapConfig.tourPauseFill_ColorAWT;
      }

      int paintedPointIndex = 0;

      for (int itemIndex = 0; itemIndex < numVisibleItems; itemIndex++) {

         final PointFeature distribLabel = allPauseItems.get(itemIndex);

         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final int mapPointDevX = mapPoint.geoPointDevX;
         final int mapPointDevY = mapPoint.geoPointDevY;

         final int symbolDevX = mapPointDevX - symbolSize2;
         final int symbolDevY = mapPointDevY - symbolSize2;

         final Rectangle symbolRectangle = new Rectangle(
               symbolDevX,
               symbolDevY,
               _mapPointSymbolSize,
               _mapPointSymbolSize);

         g2d.setColor(fillColor);
         g2d.fillRect(symbolRectangle.x, symbolRectangle.y, symbolRectangle.width, symbolRectangle.height);

         g2d.setColor(outlineColor);
         g2d.drawRect(
               symbolRectangle.x + lineWidth2,
               symbolRectangle.y + lineWidth2,
               symbolRectangle.width - lineWidth,
               symbolRectangle.height - lineWidth);

         // keep painted symbol position
         final PaintedMapPoint paintedMarker = allPaintedPoints.get(paintedPointIndex++);
         paintedMarker.symbolRectangle = symbolRectangle;
      }
   }

   private void paint_MpImage_50_AllWayPoints(final Graphics2D g2d,
                                              final int numVisibleItems,
                                              final List<PointFeature> allItems,
                                              final List<PaintedMapPoint> allPaintedPoints) {

      /*
       * Draw pause label
       */
      final FontMetrics fontMetrics = g2d.getFontMetrics();
      final int textHeight = fontMetrics.getHeight();

      for (int itemIndex = 0; itemIndex < numVisibleItems; itemIndex++) {

         final PointFeature distribLabel = allItems.get(itemIndex);

         // check if label is displayed
         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final String text = mapPoint.getFormattedLabel();

         final int textWidth = fontMetrics.stringWidth(text);

         final int labelDevX = (int) distribLabel.labelBoxL;
         final int labelDevY = (int) distribLabel.labelBoxT;

         final Rectangle pauseLabelRectangle = new Rectangle(
               labelDevX,
               labelDevY,
               textWidth,
               textHeight);

         paint_MpImage_90_OneLabel(
               g2d,
               mapPoint,
               pauseLabelRectangle,
               allPaintedPoints);
      }

      /*
       * Draw pause symbol
       */
      final int symbolSize2 = _mapPointSymbolSize / 2;
      final int lineWidth = _mapPointSymbolSize / 4;
      final int lineWidth2 = lineWidth / 2;

      g2d.setStroke(new BasicStroke(lineWidth));

      java.awt.Color fillColor;
      java.awt.Color outlineColor;

      if (_isMarkerClusterSelected) {

         // all other labels are disable -> display grayed out

         fillColor = java.awt.Color.WHITE;
         outlineColor = _isMapBackgroundDark ? java.awt.Color.GRAY : java.awt.Color.WHITE;

      } else {

         fillColor = _mapConfig.tourWayPointOutline_ColorAWT;
         outlineColor = _mapConfig.tourWayPointFill_ColorAWT;
      }

      int paintedPointIndex = 0;

      for (int itemIndex = 0; itemIndex < numVisibleItems; itemIndex++) {

         final PointFeature distribLabel = allItems.get(itemIndex);

         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final int mapPointDevX = mapPoint.geoPointDevX;
         final int mapPointDevY = mapPoint.geoPointDevY;

         final int symbolDevX = mapPointDevX - symbolSize2;
         final int symbolDevY = mapPointDevY - symbolSize2;

         final Rectangle symbolRectangle = new Rectangle(
               symbolDevX,
               symbolDevY,
               _mapPointSymbolSize,
               _mapPointSymbolSize);

         g2d.setColor(fillColor);
         g2d.fillRect(symbolRectangle.x, symbolRectangle.y, symbolRectangle.width, symbolRectangle.height);

         g2d.setColor(outlineColor);
         g2d.drawRect(
               symbolRectangle.x + lineWidth2,
               symbolRectangle.y + lineWidth2,
               symbolRectangle.width - lineWidth,
               symbolRectangle.height - lineWidth);

         // keep painted symbol position
         final PaintedMapPoint paintedMapPoint = allPaintedPoints.get(paintedPointIndex++);
         paintedMapPoint.symbolRectangle = symbolRectangle;
      }
   }

   private void paint_MpImage_80_AllPhotos(final Graphics2D g2d,
                                           final List<PointFeature> allPhotoItems,
                                           final List<PaintedMapPoint> allPaintedPhotos) {

      final int numPhotosItems = allPhotoItems.size();

      Rectangle selectedPhotoRectangle = null;

      g2d.setStroke(new BasicStroke(1));

      /*
       * Paint photos
       */
      for (int itemIndex = 0; itemIndex < numPhotosItems; itemIndex++) {

         final PointFeature distribLabel = allPhotoItems.get(itemIndex);

         // check if label is displayed
         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final Photo photo = mapPoint.photo;
         final Point mapImageSize = photo.getMap2ImageSize(

               _isShowHQPhotoImages,
               _isShowPhotoAdjustments,
               _isEnlargeSmallImages);

         final int labelDevX = (int) distribLabel.labelBoxL;
         final int labelDevY = (int) distribLabel.labelBoxT;

         final int mapImageWidth = mapImageSize.x;
         final int mapImageHeight = mapImageSize.y;

         Rectangle photoRectangle = new Rectangle(
               labelDevX,
               labelDevY,
               mapImageWidth,
               mapImageHeight);

         final BufferedImage awtPhotoImage = getPhotoImage(photo);

         if (awtPhotoImage == null) {

            // image is not yet available -> paint photo placeholder

            g2d.setColor(java.awt.Color.GRAY);

            g2d.fillRect(
                  photoRectangle.x,
                  photoRectangle.y,
                  photoRectangle.width,
                  photoRectangle.height);

         } else {

            // paint photo image

            final int photoImageWidth = awtPhotoImage.getWidth();
            final int photoImageHeight = awtPhotoImage.getHeight();

            if (photoImageWidth == mapImageWidth
                  && photoImageHeight == mapImageHeight) {

               // do NOT resize the image, it would not look very good

               g2d.drawImage(awtPhotoImage,

                     photoRectangle.x,
                     photoRectangle.y,

                     null);

            } else if (_isEnlargeSmallImages == false
                  && photoImageWidth < mapImageWidth
                  && photoImageHeight < mapImageHeight) {

               // photo image is smaller than the requested map image -> do not enlarge it

               photoRectangle = new Rectangle(
                     labelDevX,
                     labelDevY,
                     photoImageWidth,
                     photoImageHeight);

               g2d.drawImage(awtPhotoImage,

                     photoRectangle.x,
                     photoRectangle.y,

                     null);

            } else {

               // resize image, this will also enlarge small images

               g2d.drawImage(awtPhotoImage,

                     photoRectangle.x,
                     photoRectangle.y,
                     photoRectangle.width,
                     photoRectangle.height,

                     null);
            }

            // draw border

//          g2d.setColor(_mapConfig.photoOutline_ColorAWT);
//          g2d.drawRect(
//               photoRectangle.x - MAP_POINT_BORDER,
//               photoRectangle.y,
//               photoRectangle.width + 2 * MAP_POINT_BORDER,
//               photoRectangle.height);

            photo.paintedPhoto = photoRectangle;

            if (Map2PainterConfig.isShowPhotoRating) {
               paint_MpImage_RatingStars(g2d, photo);
            }

            // photo label
            if (Map2PainterConfig.isShowPhotoLabel) {
               paint_MpImage_PhotoLabel(g2d, photo, mapPoint);
            }

            // draw annotations
            if (_isShowPhotoAdjustments && _isShowHQPhotoImages && Map2PainterConfig.isShowPhotoAnnotations) {
               paint_MpImage_Annotations(g2d, photo);
            }

            if (_selectedPhoto == photo) {
               selectedPhotoRectangle = photoRectangle;
            }
         }

         // keep position
         final PaintedMapPoint paintedMapPoint = new PaintedMapPoint(mapPoint, photoRectangle);

         allPaintedPhotos.add(paintedMapPoint);
      }

      /*
       * Draw selected photo marker
       */
      if (selectedPhotoRectangle != null) {

         final int border = 4;
         final int border2 = border / 2;

         g2d.setStroke(new BasicStroke(border));
         g2d.setColor(java.awt.Color.yellow);

         g2d.drawRect(

               selectedPhotoRectangle.x - border2,
               selectedPhotoRectangle.y - border2,
               selectedPhotoRectangle.width + border,
               selectedPhotoRectangle.height + border);
      }

      /*
       * Draw photo symbol
       */
      final int symbolSize2 = _mapPointSymbolSize / 2;
      final int lineWidth = _mapPointSymbolSize / 4;
      final int lineWidth2 = lineWidth / 2;

      g2d.setStroke(new BasicStroke(lineWidth));

      // ensure that ovals are smooth
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      java.awt.Color fillColor;
      java.awt.Color outlineColor;

      final java.awt.Color positionedFillColor = java.awt.Color.YELLOW;
      final java.awt.Color positionedOutlineColor = java.awt.Color.RED;

      if (_isMarkerClusterSelected) {

         // all other labels are disable -> display grayed out

         fillColor = java.awt.Color.WHITE;
         outlineColor = _isMapBackgroundDark ? java.awt.Color.GRAY : java.awt.Color.WHITE;

      } else {

         fillColor = _mapConfig.photoOutline_ColorAWT;
         outlineColor = _mapConfig.photoFill_ColorAWT;
      }

      int paintedPointIndex = 0;

      for (int itemIndex = 0; itemIndex < numPhotosItems; itemIndex++) {

         final PointFeature distribLabel = allPhotoItems.get(itemIndex);

         if (distribLabel.isLabeled == false) {
            continue;
         }

         final Map2Point mapPoint = (Map2Point) distribLabel.data;

         final Photo photo = mapPoint.photo;
         final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(photo);

         if (allTourPhotos.size() == 0) {
            continue;
         }

         final TourPhoto tourPhoto = allTourPhotos.get(0);
         final TourData tourData = tourPhoto.getTourData();
         final Set<Long> tourPhotosWithPositionedGeo = tourData.getTourPhotosWithPositionedGeo();
         final boolean isPositionedPhoto = tourPhotosWithPositionedGeo.contains(tourPhoto.getPhotoId());

         final int mapPointDevX = mapPoint.geoPointDevX;
         final int mapPointDevY = mapPoint.geoPointDevY;

         final int symbolDevX = mapPointDevX - symbolSize2;
         final int symbolDevY = mapPointDevY - symbolSize2;

         final Rectangle symbolRectangle = new Rectangle(
               symbolDevX,
               symbolDevY,
               _mapPointSymbolSize,
               _mapPointSymbolSize);

         g2d.setColor(isPositionedPhoto ? positionedFillColor : fillColor);
         g2d.fillOval(
               symbolRectangle.x + 1, // fill a smaller shape that antialiasing do not show a light border !!!
               symbolRectangle.y + 1,
               _mapPointSymbolSize - 2,
               _mapPointSymbolSize - 2);

         g2d.setColor(isPositionedPhoto ? positionedOutlineColor : outlineColor);
         g2d.drawOval(
               symbolRectangle.x + lineWidth2,
               symbolRectangle.y + lineWidth2,
               symbolRectangle.width - lineWidth,
               symbolRectangle.height - lineWidth);

         // keep painted symbol position
         final PaintedMapPoint paintedMarker = allPaintedPhotos.get(paintedPointIndex++);
         paintedMarker.symbolRectangle = symbolRectangle;
      }

      // ensure that rectangels are not smoothed
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
   }

   private void paint_MpImage_90_OneLabel(final Graphics2D g2d,
                                          final Map2Point mapPoint,
                                          final Rectangle labelRectangle,
                                          final List<PaintedMapPoint> allPaintedMapPoints) {

      final FontMetrics fontMetrics = g2d.getFontMetrics();

      final String labelText = mapPoint.getFormattedLabel();

      final int rectangleHeight = labelRectangle.height;

      final int devX = labelRectangle.x;
      final int devY = labelRectangle.y + rectangleHeight - fontMetrics.getDescent();

      java.awt.Color fillColor;
      java.awt.Color outlineColor;

      MapLabelLayout markerLabelLayout = _mapConfig.labelLayout;

      if (_isMarkerClusterSelected) {

         // all other labels are disable -> display grayed out

         fillColor = java.awt.Color.WHITE;
         outlineColor = _isMapBackgroundDark ? java.awt.Color.GRAY : java.awt.Color.WHITE;

         markerLabelLayout = MapLabelLayout.NONE;

      } else {

         fillColor = mapPoint.getFillColorAWT();
         outlineColor = mapPoint.getOutlineColorAWT();
      }

      /*
       * Draw label background
       */
      if (markerLabelLayout.equals(MapLabelLayout.RECTANGLE_BOX)) {

         g2d.setColor(fillColor);

         g2d.fillRect(
               labelRectangle.x - MAP_POINT_BORDER,
               labelRectangle.y,
               labelRectangle.width + 2 * MAP_POINT_BORDER,
               rectangleHeight);

      } else if (markerLabelLayout.equals(MapLabelLayout.SHADOW)) {

         g2d.setColor(fillColor);

         g2d.drawString(labelText, devX + 1, devY + 1);

      } else if (markerLabelLayout.equals(MapLabelLayout.NONE)) {

         // no border

      } else if (markerLabelLayout.equals(MapLabelLayout.BORDER_1_PIXEL)) {

         g2d.setColor(fillColor);

         g2d.drawString(labelText, devX - 1, devY);
         g2d.drawString(labelText, devX + 1, devY);
         g2d.drawString(labelText, devX, devY - 1);
         g2d.drawString(labelText, devX, devY + 1);

      } else if (markerLabelLayout.equals(MapLabelLayout.BORDER_2_PIXEL)) {

         g2d.setColor(fillColor);

         g2d.drawString(labelText, devX - 1, devY);
         g2d.drawString(labelText, devX + 1, devY);
         g2d.drawString(labelText, devX, devY - 1);
         g2d.drawString(labelText, devX, devY + 1);

         g2d.drawString(labelText, devX - 2, devY);
         g2d.drawString(labelText, devX + 2, devY);
         g2d.drawString(labelText, devX, devY - 2);
         g2d.drawString(labelText, devX, devY + 2);
      }

      /*
       * Draw label text
       */
      g2d.setColor(outlineColor);
      g2d.drawString(labelText, devX, devY);

      // keep position
      allPaintedMapPoints.add(new PaintedMapPoint(mapPoint, labelRectangle));
   }

   private void paint_MpImage_Annotations(final Graphics2D g2d, final Photo photo) {

      final boolean isCropped = photo.isCropped;
      final boolean isSetTonality = photo.isSetTonality;

      if (isCropped == false && isSetTonality == false) {
         return;
      }

      final Rectangle paintedPhoto = photo.paintedPhoto;

      final int photoDevX = paintedPhoto.x;
      final int photoDevY = paintedPhoto.y;
      final int photoWidth = paintedPhoto.width;
      final int photoHeight = paintedPhoto.height;

      final int annotationWidth = _imageAnnotationCropped.getWidth();
      final int annotationHeight = _imageAnnotationCropped.getHeight();

      int devX = photoDevX + photoWidth - 2;
      final int devY = photoDevY + photoHeight - annotationHeight - 2;

      if (isCropped) {

         devX -= annotationWidth;

         g2d.drawImage(_imageAnnotationCropped, devX, devY, null);

         devX -= 3;
      }

      if (isSetTonality) {

         devX -= annotationWidth;

         g2d.drawImage(_imageAnnotationTonality, devX, devY, null);
      }
   }

   private void paint_MpImage_PhotoLabel(final Graphics2D g2d, final Photo photo, final Map2Point mapPoint) {

      final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(photo);
      if (allTourPhotos.size() < 1) {
         return;
      }

      final TourPhoto tourPhoto = allTourPhotos.get(0);
      final String photoLabel = tourPhoto.getPhotoLabel();

      if (photoLabel == null) {
         return;
      }

      final Rectangle paintedPhoto = photo.paintedPhoto;
      final int devY = paintedPhoto.y + paintedPhoto.height;
      final int devX = paintedPhoto.x + 0;

      final FontMetrics fontMetrics = g2d.getFontMetrics();

      final int textHeight = fontMetrics.getHeight();
      final int fontAscent = fontMetrics.getAscent();

      final int textWidth = fontMetrics.stringWidth(photoLabel);
      final int spaceWidth = fontMetrics.stringWidth(UI.SPACE1);
      final int spaceWidth2 = spaceWidth * 2;

      final int backgroundWidth = Math.min(textWidth + spaceWidth2, paintedPhoto.width);

      final java.awt.Rectangle labelRect = new java.awt.Rectangle(
            devX,
            devY - textHeight,
            backgroundWidth,
            textHeight);

      // background
      g2d.setColor(mapPoint.getFillColorAWT());
      g2d.fillRect(
            labelRect.x,
            labelRect.y,
            labelRect.width,
            labelRect.height);

      // label
      g2d.setClip(labelRect);
      g2d.setColor(mapPoint.getOutlineColorAWT());
      g2d.drawString(photoLabel,
            devX + spaceWidth,
            devY - textHeight + fontAscent);

      // clear clipping
      g2d.setClip(null);
   }

   private void paint_MpImage_RatingStars(final Graphics2D g2d, final Photo photo) {

      final Rectangle paintedPhoto = photo.paintedPhoto;

      final int photoDevX = paintedPhoto.x;
      final int photoDevY = paintedPhoto.y;
      final int photoWidth = paintedPhoto.width;
      final int numRatingStars = photo.ratingStars;

      final boolean isSmallRatingStar = photoWidth / _deviceScaling < 70;

      photo.isSmallRatingStars = isSmallRatingStar;

      final int smallRatingStarGap = (int) (5 * _deviceScaling);
      final int smallRatingStarSize = (photoWidth / MAX_RATING_STARS) - smallRatingStarGap;

      final int maxSmallRatingStarsWidth = MAX_RATING_STARS * smallRatingStarSize
            + (MAX_RATING_STARS - 1) * smallRatingStarGap;

      // center ratings stars in the middle of the image
      final int leftBorderWithVisibleStars = photoDevX
            + photoWidth / 2
            - MAX_RATING_STARS_WIDTH / 2;

      final int leftBorderRatingStars = isSmallRatingStar
            ? photoDevX + photoWidth / 2 - maxSmallRatingStarsWidth / 2
            : leftBorderWithVisibleStars;

      final int ratingStarImageSize = _ratingStarImageSize;

      photo.paintedRatingStars = new Rectangle(

            leftBorderWithVisibleStars,
            photoDevY,

            ratingStarImageSize * MAX_RATING_STARS,
            ratingStarImageSize);

      g2d.setStroke(new BasicStroke(1));

      for (int starIndex = 0; starIndex < numRatingStars; starIndex++) {

         // draw stars are at the top of the photo

         if (isSmallRatingStar) {

            final int ratingStarXOffset = (smallRatingStarSize + smallRatingStarGap) * starIndex;

            final int ratingStarDevX = leftBorderRatingStars + ratingStarXOffset;
            final int ratingStarDevY = photoDevY + (ratingStarImageSize / 2 - smallRatingStarSize / 2) / 2;

            g2d.setColor(RATING_STAR_COLOR);
            g2d.fillRect(

                  ratingStarDevX,
                  ratingStarDevY,
                  smallRatingStarSize,
                  smallRatingStarSize);

            g2d.setColor(RATING_STAR_COLOR_BORDER);
            g2d.drawRect(

                  ratingStarDevX,
                  ratingStarDevY,
                  smallRatingStarSize,
                  smallRatingStarSize);

         } else {

            g2d.drawImage(_imageRatingStar,

                  leftBorderRatingStars + (ratingStarImageSize * starIndex),
                  photoDevY,
                  ratingStarImageSize,
                  ratingStarImageSize,

                  null);
         }
      }
   }

   private void paint_OfflineArea(final GC gc) {

      gc.setLineWidth(2);

      /*
       * Draw previous area box
       */
//
// DISABLED: Wrong location when map is relocated
//
//      if (_offline_PreviousOfflineArea != null
//
//            // show only at the same zoomlevel
//            && _offline_PreviousOfflineArea_MapZoomLevel == _mapZoomLevel) {
//
//         gc.setLineStyle(SWT.LINE_SOLID);
//         gc.setForeground(UI.SYS_COLOR_WHITE);
//         gc.drawRectangle(_offline_PreviousOfflineArea);
//
//         final int devX = _offline_PreviousOfflineArea.x;
//         final int devY = _offline_PreviousOfflineArea.y;
//         gc.setForeground(UI.SYS_COLOR_GRAY);
//         gc.drawRectangle(
//               devX + 1,
//               devY + 1,
//               _offline_PreviousOfflineArea.width - 2,
//               _offline_PreviousOfflineArea.height - 2);
//
//         /*
//          * draw text marker
//          */
//         gc.setForeground(UI.SYS_COLOR_BLACK);
//         gc.setBackground(UI.SYS_COLOR_WHITE);
//         final Point textExtend = gc.textExtent(Messages.Offline_Area_Label_OldAreaMarker);
//         int devYMarker = devY - textExtend.y;
//         devYMarker = devYMarker < 0 ? 0 : devYMarker;
//         gc.drawText(Messages.Offline_Area_Label_OldAreaMarker, devX, devYMarker);
//      }

      /*
       * show info in the top/right corner that selection for the offline area is active
       */
      if (_offline_IsSelectingOfflineArea) {
         paint_OfflineArea_10_Info(gc);
      }

      // check if mouse button is hit which sets the start position
      if ((_offline_DevMouse_Start == null) || (_offline_WorldMouse_Move == null)) {
         return;
      }

      /*
       * Draw tile box for tiles which are selected within the area box
       */
      final int devTileStartX = _offline_DevTileStart.x;
      final int devTileStartY = _offline_DevTileStart.y;
      final int devTileEndX = _offline_DevTileEnd.x;
      final int devTileEndY = _offline_DevTileEnd.y;

      final int devTileStartX2 = Math.min(devTileStartX, devTileEndX);
      final int devTileStartY2 = Math.min(devTileStartY, devTileEndY);
      final int devTileEndX2 = Math.max(devTileStartX, devTileEndX);
      final int devTileEndY2 = Math.max(devTileStartY, devTileEndY);

      for (int devX = devTileStartX2; devX <= devTileEndX2; devX += _tilePixelSize) {
         for (int devY = devTileStartY2; devY <= devTileEndY2; devY += _tilePixelSize) {

            gc.setLineStyle(SWT.LINE_SOLID);
            gc.setForeground(_display.getSystemColor(SWT.COLOR_YELLOW));
            gc.drawRectangle(devX, devY, _tilePixelSize, _tilePixelSize);

            gc.setLineStyle(SWT.LINE_DASH);
            gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
            gc.drawRectangle(devX, devY, _tilePixelSize, _tilePixelSize);
         }
      }

      final int devArea_Start_X = _offline_DevMouse_Start.x;
      final int devArea_Start_Y = _offline_DevMouse_Start.y;
      final int devArea_End_X = _offline_DevMouse_End.x;
      final int devArea_End_Y = _offline_DevMouse_End.y;

      final int devArea_X1;
      final int devArea_Y1;

      final int devArea_Width;
      final int devArea_Height;

      if (devArea_Start_X < devArea_End_X) {

         devArea_X1 = devArea_Start_X;
         devArea_Width = devArea_End_X - devArea_Start_X;

      } else {

         devArea_X1 = devArea_End_X;
         devArea_Width = devArea_Start_X - devArea_End_X;
      }

      if (devArea_Start_Y < devArea_End_Y) {

         devArea_Y1 = devArea_Start_Y;
         devArea_Height = devArea_End_Y - devArea_Start_Y;

      } else {

         devArea_Y1 = devArea_End_Y;
         devArea_Height = devArea_Start_Y - devArea_End_Y;
      }

      /*
       * Draw selected area box
       */
      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawRectangle(devArea_X1, devArea_Y1, devArea_Width, devArea_Height);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(UI.SYS_COLOR_WHITE);

      gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_YELLOW));
      gc.setAlpha(0x30);
      gc.fillRectangle(devArea_X1 + 1, devArea_Y1 + 1, devArea_Width - 2, devArea_Height - 2);
      gc.setAlpha(0xff);

      /*
       * Draw text marker
       */
      final Point textExtend = gc.textExtent(Messages.Offline_Area_Label_AreaMarker);
      int devYMarker = devArea_Y1 - textExtend.y;
      devYMarker = devYMarker < 0 ? 0 : devYMarker;

      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.setBackground(UI.SYS_COLOR_WHITE);
      gc.drawText(Messages.Offline_Area_Label_AreaMarker, devArea_X1, devYMarker);
   }

   private void paint_OfflineArea_10_Info(final GC gc) {

      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.setBackground(UI.SYS_COLOR_YELLOW);

      final StringBuilder sb = new StringBuilder();
      sb.append(UI.SPACE + Messages.Offline_Area_Label_SelectInfo);

      if (_offline_DevMouse_Start != null) {

         // display offline area geo position

         final Point2D.Double worldPixel_Start = new Point2D.Double(_offline_WorldMouse_Start.x, _offline_WorldMouse_Start.y);
         final Point2D.Double worldPixel_End = new Point2D.Double(_offline_WorldMouse_End.x, _offline_WorldMouse_End.y);

         final GeoPosition geoStart = _mp.pixelToGeo(worldPixel_Start, _mapZoomLevel);
         final GeoPosition geoEnd = _mp.pixelToGeo(worldPixel_End, _mapZoomLevel);

         sb.append(String.format("   %s / %s  ...  %s / %s", //$NON-NLS-1$
               _nfLatLon.format(geoStart.latitude),
               _nfLatLon.format(geoStart.longitude),
               _nfLatLon.format(geoEnd.latitude),
               _nfLatLon.format(geoEnd.longitude)));

      } else {

         // display mouse move geo position

         if (_offline_WorldMouse_Move != null) {

            final Point2D.Double worldPixel_Mouse = new Point2D.Double(_offline_WorldMouse_Move.x, _offline_WorldMouse_Move.y);

            final GeoPosition mouseGeo = _mp.pixelToGeo(worldPixel_Mouse, _mapZoomLevel);

            sb.append(String.format("   %s / %s", //$NON-NLS-1$
                  _nfLatLon.format(mouseGeo.latitude),
                  _nfLatLon.format(mouseGeo.longitude)));
         }
      }

      gc.drawText(sb.toString(), 0, 0);
   }

   /**
    * Define and start the overlay thread
    */
   private void paint_Overlay_0_SetupThread() {

      _overlayImageCache = new OverlayImageCache();

      _overlayThread = new Thread("2D Map - Paint Overlay Images") { //$NON-NLS-1$
         @Override
         public void run() {

            while (!isInterrupted()) {

               try {

                  Thread.sleep(20);

                  if (_isRunningDrawOverlay) {
                     continue;
                  }

                  // overlay drawing is not running

                  final long currentTime = System.currentTimeMillis();

                  if (currentTime > _nextOverlayRedrawTime + 50) {
                     if (_tileOverlayPaintQueue.size() > 0) {

                        // create overlay images
                        paint_Overlay_10_RunThread();
                     }
                  }

               } catch (final InterruptedException e) {
                  interrupt();
               } catch (final Exception e) {
                  StatusUtil.log(e);
               }
            }
         }
      };

      _overlayThread.setDaemon(true);
      _overlayThread.start();
   }

   private void paint_Overlay_10_RunThread() {

      final int currentRunnableCounter = _overlayRunnableCounter.incrementAndGet();

      if (isDisposed()) {
         return;
      }

      final Runnable uiOverlayRunnable = new Runnable() {

         final int __runnableCounter = currentRunnableCounter;

         @Override
         public void run() {

            if (isDisposed()) {
               return;
            }

            // check if a newer runnable is available
            if (__runnableCounter != _overlayRunnableCounter.get()) {
               return;
            }

            _isRunningDrawOverlay = true;

            try {

               paint_Overlay_20_Tiles();

            } catch (final Exception e) {

               StatusUtil.log(e);

            } finally {
               _isRunningDrawOverlay = false;
            }
         }
      };

      _display.asyncExec(uiOverlayRunnable);
   }

   private void paint_Overlay_20_Tiles() {

      BusyIndicator.showWhile(_display, () -> {

         Tile tile;

         final long startTime = System.currentTimeMillis();

         while ((tile = _tileOverlayPaintQueue.poll()) != null) {

            // skip tiles from another zoom level
            if (tile.getZoom() == _mapZoomLevel) {

               // set state that this tile is checked
               tile.setOverlayTourStatus(OverlayTourState.TILE_IS_CHECKED);

               // cleanup previous positions
               tile.allPainted_Hash.clear();
               tile.allPainted_HoverRectangle.clear();
               tile.allPainted_HoverSerieIndices.clear();
               tile.allPainted_HoverTourID.clear();

               /*
                * Check if a tour, marker or photo is within the current tile
                */
               final boolean isPaintingNeeded = _mapPainter.isPaintingNeeded(Map2.this, tile);

               if (isPaintingNeeded == false) {

                  // set tile state
                  tile.setOverlayImageState(OverlayImageState.NO_IMAGE);

                  continue;
               }

               // paint overlay
               paint_Overlay_22_OneTile(tile);

               // allow to display painted overlays
               final long paintTime = System.currentTimeMillis();
               if (paintTime > startTime + 500) {
                  break;
               }

            } else {

               // tile has a different zoom level, ignore this tile
               tile.setOverlayTourStatus(OverlayTourState.TILE_IS_NOT_CHECKED);
            }
         }
      });
   }

   /**
    * Paint the tour in basic mode
    *
    * @param tile
    */
   private void paint_Overlay_22_OneTile(final Tile tile) {

      boolean isOverlayPainted = false;

      final float scaledTilePixelSize = _tilePixelSize * UI.HIDPI_SCALING * 2;
      final ImageData transparentImageData = MapUtils.createTransparentImageData((int) scaledTilePixelSize);

      final NoAutoScalingImageDataProvider imageDataProvider = new NoAutoScalingImageDataProvider(transparentImageData);

      final Image overlayImage = new Image(_display, imageDataProvider);
      final GC gcTile = new GC(overlayImage);
      {
         /*
          * Ubuntu 12.04 fails, when background is not filled, it draws a black background
          */
         final Rectangle bounds = overlayImage.getBounds();

         gcTile.setBackground(_mapTransparentColor);
         gcTile.fillRectangle(0, 0, bounds.width, bounds.height);

         // paint all overlays for the current tile
         final boolean isPainted = _mapPainter.doPaint(
               gcTile,
               Map2.this,
               tile,
               1,
               _isFastMapPainting && _isFastMapPainting_Active,
               _fastMapPainting_skippedValues);

         final ImageData imageDataAfterPainting = overlayImage.getImageData();

         imageDataProvider.setImageData(imageDataAfterPainting);

         isOverlayPainted = isOverlayPainted || isPainted;
      }
      gcTile.dispose();

      if (isOverlayPainted) {

         // overlay is painted

         final String overlayKey = getOverlayKey(tile, 0, 0, _mp.getProjection().getId());

         tile.setOverlayImage(overlayImage);
         _overlayImageCache.add(overlayKey, overlayImage);

         // set tile state
         tile.setOverlayImageState(OverlayImageState.TILE_HAS_CONTENT);
         tile.incrementOverlayContent();

         paint();

      } else {

         // image is not needed
         overlayImage.dispose();

         // set tile state
         tile.setOverlayImageState(OverlayImageState.NO_IMAGE);
      }
   }

   /**
    * Draw border around text to make it more visible
    */
   private void paint_Text_Border(final GC gc, final String text, final int devX, final int devY) {

      gc.setForeground(UI.SYS_COLOR_WHITE);

      gc.drawString(text, devX + 1, devY + 1, true);
      gc.drawString(text, devX - 1, devY - 1, true);

      gc.drawString(text, devX + 1, devY - 1, true);
      gc.drawString(text, devX - 1, devY + 1, true);

      gc.drawString(text, devX + 1, devY, true);
      gc.drawString(text, devX - 1, devY, true);

      gc.drawString(text, devX, devY + 1, true);
      gc.drawString(text, devX, devY - 1, true);
   }

   /**
    * @param gc
    * @param devXMouse
    * @param devYMouse
    * @param text
    * @param fgColor
    * @param bgColor
    * @param isAdjustToMousePosition
    *
    * @return Returns outline of the painted text or <code>null</code> when text is not painted
    */
   private Rectangle paint_Text_Label(final GC gc,
                                      final int devXMouse,
                                      final int devYMouse,
                                      final String text,
                                      final Color fgColor,
                                      final Color bgColor,
                                      final boolean isAdjustToMousePosition) {

      if (text == null) {
         return null;
      }

      final int textMargin = TEXT_MARGIN;
      final int textMargin2 = textMargin / 2;

      final Point textSize = gc.stringExtent(text);
      final int textWidth = textSize.x;
      final int textHeight = textSize.y;

      final int contentWidth = textWidth + textMargin;
      final int contentHeight = textHeight + textMargin;

      int devXDetail = devXMouse;
      int devYDetail = devYMouse;

      // ensure that the tour detail is fully visible
      final int viewportWidth = _devMapViewport.width;
      final int viewportHeight = _devMapViewport.height;

      if (isAdjustToMousePosition) {

         final int mouseHeight = 24;

         if (devXDetail + textWidth + textMargin > viewportWidth) {
            devXDetail = viewportWidth - textWidth - textMargin;
         }

         if (devYDetail - textHeight - textMargin - mouseHeight < 0) {
            devYDetail = devYMouse + textHeight + textMargin + mouseHeight;
         }

      } else {

         // make sure that the text is fully visible

         // left border
         if (devXDetail < 0) {
            devXDetail = 0;
         }

         // right border
         if (devXDetail + contentWidth > viewportWidth) {
            devXDetail = viewportWidth - contentWidth;
         }

         // top border
         if (devYDetail - contentHeight < 0) {
            devYDetail = contentHeight;
         }

         // bottom border
         if (devYDetail > viewportHeight) {
            devYDetail = viewportHeight;
         }
      }

      final int devYText = devYDetail - textHeight - textMargin2;

      final Rectangle textOutline = new Rectangle(devXDetail,
            devYText - textMargin,
            textWidth + textMargin,
            textHeight + textMargin);

      gc.setAlpha(0xff);

      gc.setBackground(bgColor);
      gc.fillRectangle(textOutline);

      gc.setForeground(fgColor);
      gc.drawString(
            text,
            devXDetail + textMargin2,
            devYText - textMargin2,
            true);

      return textOutline;
   }

   private void paint_Text_WithBorder(final GC gc, final String text, final Point topLeft) {

      if (text == null) {
         return;
      }

      final Point textSize = gc.stringExtent(text);

      final int devX = topLeft.x;
      final int devY = topLeft.y - textSize.y - 5;

      paint_Text_Border(gc, text, devX, devY);

      /*
       * Draw text
       */
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawString(text, devX, devY, true);
   }

   private Tile paint_Tile(final GC gcMapImage,
                           final int tilePositionX,
                           final int tilePositionY,
                           final Rectangle devTileViewport) {

      // get tile from the map provider, this also starts the loading of the tile image
      final Tile tile = _mp.getTile(tilePositionX, tilePositionY, _mapZoomLevel);

      Image tileImage = tile.getCheckedMapImage();
      if (tileImage != null) {

         // tile map image is available and valid

         // paint tile dimming when not yet done
         if (tile.dimImage_TileKey != null) {

            tileImage = paint_Tile_40_Dimming(tileImage, tile);

            // reset state AFTER tile key is used
            tile.dimImage_TileKey = null;
         }

         gcMapImage.drawImage(tileImage, devTileViewport.x, devTileViewport.y);

      } else {

         paint_Tile_10_Image(gcMapImage, tile, devTileViewport);
      }

      if (_isDrawOverlays) {

         gcMapImage.setAlpha(_overlayAlpha);

         paint_Tile_20_Overlay(gcMapImage, tile, devTileViewport);

         gcMapImage.setAlpha(0xff);
      }

      if (_isShowDebug_TileInfo || _isShowDebug_TileBorder) {
         paint_Tile_30_Info(gcMapImage, tile, devTileViewport);
      }

      return tile;
   }

   /**
    * Draw the tile map image
    */
   private void paint_Tile_10_Image(final GC gcMapImage, final Tile tile, final Rectangle devTileViewport) {

      if (tile.isLoadingError()) {

         // map image contains an error, it could not be loaded

         final Image errorImage = _mp.getErrorImage();
         final Rectangle imageBounds = errorImage.getBounds();

         gcMapImage.setBackground(UI.SYS_COLOR_GRAY);
         gcMapImage.fillRectangle(devTileViewport.x, devTileViewport.y, imageBounds.width, imageBounds.height);

         paint_TileInfo_Error(gcMapImage, devTileViewport, tile);

         return;
      }

      if (tile.isOfflineError()) {

         //map image could not be loaded from offline file

         gcMapImage.drawImage(_mp.getErrorImage(), devTileViewport.x, devTileViewport.y);

         paint_TileInfo_Error(gcMapImage, devTileViewport, tile);

         return;
      }

      /*
       * the tile image is not yet loaded, register an observer that handles redrawing when the tile
       * image is available. Tile image loading is started, when the tile is retrieved from the tile
       * factory which is done in drawTile()
       */
      tile.setImageLoaderCallback(_tileImageLoaderCallback);

      if (_isLiveView == false) {

         // check if the offline image is available
         if (tile.isOfflimeImageAvailable()) {

            /*
             * offline image is available but not yet loaded into the cache (this is done in the
             * background ), draw nothing to prevent flickering of the loading... message
             */

         } else {

            /*
             * offline image is not available, show loading... message
             */

            gcMapImage.drawImage(_mp.getLoadingImage(), devTileViewport.x, devTileViewport.y);

//            gc.setForeground(_display.getSystemColor(SWT.COLOR_BLACK));
//            gc.drawString(Messages.geoclipse_extensions_loading, devTileViewport.x, devTileViewport.y, true);
         }
      }
   }

   /**
    * Draw overlay image when it's available or request the image
    *
    * @param gcMapImage
    * @param tile
    * @param devTileViewport
    *           Position of the tile
    */
   private void paint_Tile_20_Overlay(final GC gcMapImage, final Tile tile, final Rectangle devTileViewport) {

      /*
       * Priority 1: draw overlay image
       */
      final OverlayImageState imageState = tile.getOverlayImageState();
      final int numOverlayContent = tile.getOverlayContent();

      if ((imageState == OverlayImageState.IMAGE_IS_BEING_CREATED)
            || ((imageState == OverlayImageState.NO_IMAGE) && (numOverlayContent == 0))) {

         // there is no image for the tile overlay or the image is currently being created
         return;
      }

      Image overlayCachedImage = null;
      Image partOverlayImage = null;
      Image tileOverlayImage = null;

      if (numOverlayContent > 0) {

         // tile has overlay content, check if an image is available

         final String overlayKey = getOverlayKey(tile);

         // get overlay image from the cache
         overlayCachedImage = partOverlayImage = _overlayImageCache.get(overlayKey);

         if (partOverlayImage == null) {

            /**
             * get image from the tile, it's possible that the part image is disposed but the tile
             * image is still available
             */
            tileOverlayImage = tile.getOverlayImage();
            if ((tileOverlayImage != null) && (tileOverlayImage.isDisposed() == false)) {
               overlayCachedImage = tileOverlayImage;
            }
         }
      }

      // draw overlay image
      if (overlayCachedImage != null && overlayCachedImage.isDisposed() == false) {

         try {

            gcMapImage.drawImage(overlayCachedImage, devTileViewport.x, devTileViewport.y);

         } catch (final Exception e) {

            /*
             * Ignore, it's still possible that the image is disposed when the images are changing
             * very often and the cache is small
             */
            partOverlayImage = null;
         }
      }

      /*
       * Priority 2: check state for the overlay
       */
      final OverlayTourState tourState = tile.getOverlayTourStatus();

      if (tourState == OverlayTourState.TILE_IS_CHECKED) {

         // it is possible that the image is disposed but the tile has overlay content

         /**
          * check if the tile overlay image (not the surrounding part images) is available, when not
          * the image must be created
          */
         if (tileOverlayImage == null) {
            tileOverlayImage = tile.getOverlayImage();
         }

         if ((tileOverlayImage == null) || tileOverlayImage.isDisposed()) {

            // overlay image is NOT available

            // check if tile has overlay content
            if (numOverlayContent == 0) {

               /**
                * tile has no overlay content -> set state that the drawing of the overlay is as
                * fast as possible
                */
               tile.setOverlayImageState(OverlayImageState.NO_IMAGE);

            } else {

               // tile has overlay content but no image, this is not good, create image again

               if (imageState == OverlayImageState.TILE_HAS_CONTENT) {

                  // overlay content is created from this tile

                  queueOverlayPainting(tile);

                  return;

               } else {

                  if (partOverlayImage == null) {

                     // tile is checked and has no image but the content is created from a part tile

                     // this method will do an endless loop and is disabled
                     // -> this problem is currently not solved
                     //   queueOverlayPainting(tile);
                     return;
                  }
               }
            }

         } else {

            // overlay image is available

            if (imageState == OverlayImageState.NOT_SET) {

               if (numOverlayContent == 0) {
                  tile.setOverlayImageState(OverlayImageState.NO_IMAGE);
               } else {
                  // something is wrong
                  queueOverlayPainting(tile);
               }
            }
         }

         // tile tours are checked and the state is OK
         return;
      }

      // when tile is queued, nothing more to do, just wait
      if (tourState == OverlayTourState.IS_QUEUED) {
         return;
      }

      // overlay tour status is not yet checked, overlayTourStatus == OverlayTourStatus.NOT_CHECKED
      queueOverlayPainting(tile);
   }

   /**
    * @param gc
    * @param tile
    * @param devTileViewport
    */
   private void paint_Tile_30_Info(final GC gc, final Tile tile, final Rectangle devTileViewport) {

      final ConcurrentHashMap<String, Tile> childrenWithErrors = tile.getChildrenWithErrors();

      if (tile.isLoadingError()
            || tile.isOfflineError()
            || ((childrenWithErrors != null) && (childrenWithErrors.size() > 0))) {

         paint_TileInfo_Error(gc, devTileViewport, tile);

         return;
      }

      if (_isShowDebug_TileBorder) {

         // draw tile border
         gc.setForeground(_isMapBackgroundDark ? UI.SYS_COLOR_YELLOW : UI.SYS_COLOR_RED);
         gc.drawRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);
      }

      if (_isShowDebug_TileInfo) {

         // draw tile info
         gc.setForeground(UI.SYS_COLOR_WHITE);
         gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_BLUE));

         final int leftMargin = 10;

         paint_TileInfo_LatLon(gc, tile, devTileViewport, 10, leftMargin);
         paint_TileInfo_Position(gc, devTileViewport, tile, 50, leftMargin);

         // draw tile image path/url
         final StringBuilder sb = new StringBuilder();

         paint_TileInfo_Path(tile, sb);

         _textWrapper.printText(
               gc,
               sb.toString(),
               devTileViewport.x + leftMargin,
               devTileViewport.y + 80,
               devTileViewport.width - 20);
      }
   }

   private Image paint_Tile_40_Dimming(final Image tileImage, final Tile tile) {

      if (tileImage == null || tileImage.isDisposed()) {
         return null;
      }

      // create dimmed image
      final Rectangle imageBounds = tileImage.getBounds();
      final Image dimmedImage = new Image(_display, imageBounds.width, imageBounds.height);

      final GC gcDimmedImage = new GC(dimmedImage);
      {
         gcDimmedImage.setBackground(new Color(_mp.getDimColor()));
         gcDimmedImage.fillRectangle(imageBounds);

         gcDimmedImage.setAlpha(_mp.getDimAlpha());
         gcDimmedImage.drawImage(tileImage, 0, 0);
      }
      gcDimmedImage.dispose();

      tileImage.dispose();

      // replace tile image with the dimmed image
      _mp.getTileImageCache().putIntoImageCache(tile.dimImage_TileKey, dimmedImage);

      return dimmedImage;
   }

   private void paint_TileInfo_Error(final GC gc, final Rectangle devTileViewport, final Tile tile) {

      // draw tile border
      gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
      gc.drawRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);

      gc.setForeground(UI.SYS_COLOR_WHITE);
      gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_MAGENTA));

      final int leftMargin = 10;

      paint_TileInfo_LatLon(gc, tile, devTileViewport, 10, leftMargin);
      paint_TileInfo_Position(gc, devTileViewport, tile, 50, leftMargin);

      // display loading error
      final StringBuilder sb = new StringBuilder();

      final String loadingError = tile.getLoadingError();
      if (loadingError != null) {
         sb.append(loadingError);
         sb.append(NL);
         sb.append(NL);
      }

      final ConcurrentHashMap<String, Tile> childrenLoadingError = tile.getChildrenWithErrors();
      if ((childrenLoadingError != null) && (childrenLoadingError.size() > 0)) {

         for (final Tile childTile : childrenLoadingError.values()) {
            sb.append(childTile.getLoadingError());
            sb.append(NL);
            sb.append(NL);
         }
      }

      paint_TileInfo_Path(tile, sb);

      final ArrayList<Tile> tileChildren = tile.getChildren();
      if (tileChildren != null) {
         for (final Tile childTile : tileChildren) {
            paint_TileInfo_Path(childTile, sb);
         }
      }

      _textWrapper.printText(
            gc,
            sb.toString(),
            devTileViewport.x + leftMargin,
            devTileViewport.y + 80,
            devTileViewport.width - 20);
   }

   private void paint_TileInfo_LatLon(final GC gc,
                                      final Tile tile,
                                      final Rectangle devTileViewport,
                                      final int topMargin,
                                      final int leftMargin) {

      final StringBuilder sb = new StringBuilder();

      final int devLineHeight = gc.getFontMetrics().getHeight();
      final int dev2ndColumn = 80;

      final BoundingBoxEPSG4326 bbox = tile.getBbox();

      sb.setLength(0);

      // lat - bottom
      sb.append(Messages.TileInfo_Position_Latitude);
      sb.append(_nfLatLon.format(bbox.bottom));
      gc.drawString(
            sb.toString(), //
            devTileViewport.x + leftMargin,
            devTileViewport.y + topMargin);

      // lat - top
      gc.drawString(
            _nfLatLon.format(bbox.top), //
            devTileViewport.x + leftMargin + dev2ndColumn,
            devTileViewport.y + topMargin);

      sb.setLength(0);

      // lon - left
      sb.append(Messages.TileInfo_Position_Longitude);
      sb.append(_nfLatLon.format(bbox.left));
      gc.drawString(
            sb.toString(), //
            devTileViewport.x + leftMargin,
            devTileViewport.y + topMargin + devLineHeight);

      // lon - right
      gc.drawString(
            _nfLatLon.format(bbox.right), //
            devTileViewport.x + leftMargin + dev2ndColumn,
            devTileViewport.y + topMargin + devLineHeight);
   }

   /**
    * !!! Recursive !!!
    *
    * @param tile
    * @param sb
    */
   private void paint_TileInfo_Path(final Tile tile, final StringBuilder sb) {

      final String url = tile.getUrl();
      if (url != null) {
         sb.append(url);
         sb.append(NL);
         sb.append(NL);
      }

      final String offlinePath = tile.getOfflinePath();
      if (offlinePath != null) {
         sb.append(offlinePath);
         sb.append(NL);
         sb.append(NL);
      }

      final ArrayList<Tile> tileChildren = tile.getChildren();
      if (tileChildren != null) {
         for (final Tile childTile : tileChildren) {
            paint_TileInfo_Path(childTile, sb);
         }
      }
   }

   private void paint_TileInfo_Position(final GC gc,
                                        final Rectangle devTileViewport,
                                        final Tile tile,
                                        final int topMargin,
                                        final int leftMargin) {

      final StringBuilder text = new StringBuilder()
            .append(Messages.TileInfo_Position_Zoom)
            .append(tile.getZoom() + 1)
            .append(Messages.TileInfo_Position_X)
            .append(tile.getX())
            .append(Messages.TileInfo_Position_Y)
            .append(tile.getY());

      gc.drawString(text.toString(),
            devTileViewport.x + leftMargin,
            devTileViewport.y + topMargin);
   }

   /**
    * pan the map
    */
   private void panMap(final MouseEvent mouseEvent) {

      /*
       * set new map center
       */
      final Point movePosition = new Point(mouseEvent.x, mouseEvent.y);

      final int mapDiffX = movePosition.x - _mouseDownPosition.x;
      final int mapDiffY = movePosition.y - _mouseDownPosition.y;

      final double oldCenterX = _worldPixel_MapCenter.getX();
      final double oldCenterY = _worldPixel_MapCenter.getY();

      final double newCenterX = oldCenterX - mapDiffX;
      final double newCenterY = oldCenterY - mapDiffY;

      _mouseDownPosition = movePosition;
      _isMapPanned = true;

      // set new map center
      setMapCenterInWorldPixel(new Point2D.Double(newCenterX, newCenterY));
      updateViewportData();

      paint();

      fireEvent_MapPosition(false);
   }

   private void panPhoto(final MouseEvent mouseEvent) {

      final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(_pannedPhoto);

      final TourPhoto tourPhoto = allTourPhotos.get(0);
      final TourData tourData = tourPhoto.getTourData();

      final GeoPosition mouseMove_GeoPosition = getMouseMove_GeoPosition();

      final double latitude = mouseMove_GeoPosition.latitude;
      final double longitude = mouseMove_GeoPosition.longitude;

      // getting the serie index is very tricky
      final int serieIndex = _pannedPhoto.photoIndex;

      if (serieIndex >= tourData.latitudeSerie.length) {

         // this happened, propably a wrong tour was set

         return;
      }

      tourData.latitudeSerie[serieIndex] = latitude;
      tourData.longitudeSerie[serieIndex] = longitude;

      tourPhoto.setGeoLocation(latitude, longitude);

      if (tourPhoto != null) {

         tourPhoto.setGeoLocation(latitude, longitude);

         // keep state for which photo a geo position was set
         tourData.getTourPhotosWithPositionedGeo().add(tourPhoto.getPhotoId());
      }

      // interpolate all geo positions
      tourData.computeGeo_Photos();

      TourManager.saveModifiedTour(tourData);
   }

   /**
    * @param text
    *
    * @return Returns <code>true</code> when POI could be identified and it's displayed in the map
    */
   private boolean parseAndDisplayPOIText(String text) {

      try {
         text = URLDecoder.decode(text, UI.UTF_8);
      } catch (final UnsupportedEncodingException e) {
         StatusUtil.log(e);
      }

      // linux has 2 lines: 1: url, 2. text
      final String[] dndText = text.split(UI.NEW_LINE1);
      if (dndText.length == 0) {
         return false;
      }

      // parse wiki url
      final Matcher wikiUrlMatcher = _patternWikiUrl.matcher(dndText[0]);
      if (wikiUrlMatcher.matches()) {

         // osm url was found

         final String pageName = wikiUrlMatcher.group(1);
         final String position = wikiUrlMatcher.group(2);

         if (position != null) {

            double lat = 0;
            double lon = 0;
            String otherParams = null;

            //   match D;D

            final Matcher wikiPos1Matcher = _patternWikiPosition_D_D.matcher(position);
            if (wikiPos1Matcher.matches()) {

               final String latPosition = wikiPos1Matcher.group(1);
               final String lonPosition = wikiPos1Matcher.group(2);
               otherParams = wikiPos1Matcher.group(3);

               if (lonPosition != null) {
                  try {

                     lat = Double.parseDouble(latPosition);
                     lon = Double.parseDouble(lonPosition);

                  } catch (final NumberFormatException e) {
                     return false;
                  }
               }
            } else {

               //   match D_N_D_E

               final Matcher wikiPos20Matcher = _patternWikiPosition_D_N_D_E.matcher(position);
               if (wikiPos20Matcher.matches()) {

                  final String latDegree = wikiPos20Matcher.group(1);
                  final String latDirection = wikiPos20Matcher.group(2);

                  final String lonDegree = wikiPos20Matcher.group(3);
                  final String lonDirection = wikiPos20Matcher.group(4);

                  otherParams = wikiPos20Matcher.group(5);

                  if (lonDirection != null) {
                     try {

                        final double latDeg = Double.parseDouble(latDegree);
                        final double lonDeg = Double.parseDouble(lonDegree);

                        lat = latDeg * (latDirection.equals(DIRECTION_N) ? 1 : -1);
                        lon = lonDeg * (lonDirection.equals(DIRECTION_E) ? 1 : -1);

                     } catch (final NumberFormatException e) {
                        return false;
                     }
                  }

               } else {

                  // match D_M_N_D_M_E

                  final Matcher wikiPos21Matcher = _patternWikiPosition_D_M_N_D_M_E.matcher(position);
                  if (wikiPos21Matcher.matches()) {

                     final String latDegree = wikiPos21Matcher.group(1);
                     final String latMinutes = wikiPos21Matcher.group(2);
                     final String latDirection = wikiPos21Matcher.group(3);

                     final String lonDegree = wikiPos21Matcher.group(4);
                     final String lonMinutes = wikiPos21Matcher.group(5);
                     final String lonDirection = wikiPos21Matcher.group(6);

                     otherParams = wikiPos21Matcher.group(7);

                     if (lonDirection != null) {
                        try {

                           final double latDeg = Double.parseDouble(latDegree);
                           final double latMin = Double.parseDouble(latMinutes);

                           final double lonDeg = Double.parseDouble(lonDegree);
                           final double lonMin = Double.parseDouble(lonMinutes);

                           lat = (latDeg + (latMin / 60f)) * (latDirection.equals(DIRECTION_N) ? 1 : -1);
                           lon = (lonDeg + (lonMin / 60f)) * (lonDirection.equals(DIRECTION_E) ? 1 : -1);

                        } catch (final NumberFormatException e) {
                           return false;
                        }
                     }
                  } else {

                     // match D_M_S_N_D_M_S_E

                     final Matcher wikiPos22Matcher = _patternWikiPosition_D_M_S_N_D_M_S_E.matcher(position);
                     if (wikiPos22Matcher.matches()) {

                        final String latDegree = wikiPos22Matcher.group(1);
                        final String latMinutes = wikiPos22Matcher.group(2);
                        final String latSeconds = wikiPos22Matcher.group(3);
                        final String latDirection = wikiPos22Matcher.group(4);

                        final String lonDegree = wikiPos22Matcher.group(5);
                        final String lonMinutes = wikiPos22Matcher.group(6);
                        final String lonSeconds = wikiPos22Matcher.group(7);
                        final String lonDirection = wikiPos22Matcher.group(8);

                        otherParams = wikiPos22Matcher.group(9);

                        if (lonDirection != null) {
                           try {

                              final double latDeg = Double.parseDouble(latDegree);
                              final double latMin = Double.parseDouble(latMinutes);
                              final double latSec = Double.parseDouble(latSeconds);

                              final double lonDeg = Double.parseDouble(lonDegree);
                              final double lonMin = Double.parseDouble(lonMinutes);
                              final double lonSec = Double.parseDouble(lonSeconds);

                              lat = (latDeg + (latMin / 60f) + (latSec / 3600f))
                                    * (latDirection.equals(DIRECTION_N) ? 1 : -1);

                              lon = (lonDeg + (lonMin / 60f) + (lonSec / 3600f))
                                    * (lonDirection.equals(DIRECTION_E) ? 1 : -1);

                           } catch (final NumberFormatException e) {
                              return false;
                           }
                        }
                     } else {
                        return false;
                     }
                  }
               }
            }

            // set default zoom level
            int zoom = 10;

            // get zoom level from parameter values
            if (otherParams != null) {

//                        String dim = null;
               String type = null;

               final String[] allKeyValues = _patternWikiParamter.split(otherParams);

               for (final String keyValue : allKeyValues) {

                  final String[] splittedKeyValue = _patternWikiKeyValue.split(keyValue);

                  if (splittedKeyValue.length > 1) {

                     if (splittedKeyValue[0].startsWith(WIKI_PARAMETER_TYPE)) {
                        type = splittedKeyValue[1];
//                              } else if (splittedKeyValue[0].startsWith(WIKI_PARAMETER_DIM)) {
//                                 dim = splittedKeyValue[1];
                     }
                  }
               }

               /*
                * !!! disabled because the zoom level is not correct !!!
                */
//                        if (dim != null) {
//                           final int scale = Integer.parseInt(dim);
//                           zoom = (int) (18 - (Math.round(Math.log(scale) - Math.log(1693)))) - 1;//, [2, 18];
//                        } else

               if (type != null) {

// source: https://wiki.toolserver.org/view/GeoHack
//
// type                                               ratio           m / pixel      {scale}    {mmscale} {span}  {altitude}   {zoom}   {osmzoom}
//
// country, satellite                                 1 : 10,000,000    3528        10000000    10000000    10.0        1430        1           5
// state                                              1 : 3,000,000     1058         3000000     4000000     3.0         429        3           7
// adm1st                                             1 : 1,000,000      353         1000000     1000000     1.0         143        4           9
// adm2nd (default)                                   1 : 300,000        106          300000      200000     0.3          42        5          11
// adm3rd, city, mountain, isle, river, waterbody     1 : 100,000         35.3        100000      100000     0.1          14        6          12
// event, forest, glacier                             1 : 50,000          17.6         50000       50000     0.05          7        7          13
// airport                                            1 : 30,000          10.6         30000       25000     0.03          4        7          14
// edu, pass, landmark, railwaystation                1 : 10,000          3.53         10000       10000     0.01          1        8          15

                  if (type.equals("country") //                   //$NON-NLS-1$
                        || type.equals("satellite")) { //         //$NON-NLS-1$

                     zoom = 5 - 1;

                  } else if (type.equals("state")) { //           //$NON-NLS-1$

                     zoom = 7 - 1;

                  } else if (type.equals("adm1st")) { //          //$NON-NLS-1$

                     zoom = 9 - 1;

                  } else if (type.equals("adm2nd")) { //          //$NON-NLS-1$

                     zoom = 11 - 1;

                  } else if (type.equals("adm3rd") //             //$NON-NLS-1$
                        || type.equals("city") //                 //$NON-NLS-1$
                        || type.equals("mountain") //             //$NON-NLS-1$
                        || type.equals("isle") //                 //$NON-NLS-1$
                        || type.equals("river") //                //$NON-NLS-1$
                        || type.equals("waterbody")) { //         //$NON-NLS-1$

                     zoom = 12 - 1;

                  } else if (type.equals("event")//               //$NON-NLS-1$
                        || type.equals("forest") //               //$NON-NLS-1$
                        || type.equals("glacier")) { //           //$NON-NLS-1$

                     zoom = 13 - 1;

                  } else if (type.equals("airport")) { //         //$NON-NLS-1$

                     zoom = 14 - 1;

                  } else if (type.equals("edu") //                //$NON-NLS-1$
                        || type.equals("pass") //                 //$NON-NLS-1$
                        || type.equals("landmark") //             //$NON-NLS-1$
                        || type.equals("railwaystation")) { //    //$NON-NLS-1$

                     zoom = 15 - 1;
                  }
               }

               final String poiText = pageName.replace('_', ' ');
               final double lat1 = lat;
               final double lon1 = lon;
               final int zoom1 = zoom;

               // hide previous tooltip
               setPoiVisible(false);

               final GeoPosition poiGeoPosition = new GeoPosition(lat1, lon1);

               final PoiToolTip poi = getPoiTooltip();
               poi.geoPosition = poiGeoPosition;
               poi.setText(poiText);

               setZoom(zoom1);
               setMapCenter(poiGeoPosition);

               _isPoiVisible = true;

               fireEvent_POI(poiGeoPosition, poiText);

               return true;
            }
         }
      }

      return false;
   }

   public void photoHistogram_Close() {

      _mapPointTooltip_PhotoHistogram.close();
   }

   public void photoHistogram_UpdateCropArea(final Rectangle2D.Float histogramCropArea) {

      _mapPointTooltip_PhotoHistogram.updateCropArea(histogramCropArea);
   }

   public void photoTooltip_Close() {

      _mapPointTooltip_PhotoImage.close();
   }

   public void photoTooltip_OnDiscardImages() {

      _mapPointTooltip_PhotoImage.onDiscardImages();
   }

   /**
    * Set tile in the overlay painting queue
    *
    * @param tile
    */
   public void queueOverlayPainting(final Tile tile) {

      tile.setOverlayTourStatus(OverlayTourState.IS_QUEUED);
      tile.setOverlayImageState(OverlayImageState.NOT_SET);

      _tileOverlayPaintQueue.add(tile);
   }

   private void recenterMap(final int xDiff, final int yDiff) {

      if (xDiff == 0 && yDiff == 0) {
         // nothing to do
         return;
      }

      final Rectangle bounds = _worldPixel_TopLeft_Viewport;

      final double newCenterX = bounds.x + bounds.width / 2.0 + xDiff;
      final double newCenterY = bounds.y + bounds.height / 2.0 + yDiff;

      final Point2D.Double pixelCenter = new Point2D.Double(newCenterX, newCenterY);

      setMapCenterInWorldPixel(pixelCenter);
      updateViewportData();

      resetMapPoints();

      paint();

      fireEvent_MapPosition(false);
   }

   /**
    * Re-centers the map to have the current common location be at the center of the map,
    * accounting for the map's width and height.
    *
    * @see getCommonLocation
    */
   public void recenterToCommonLocation() {

      final java.awt.Point worldPixel = _mp.geoToPixel(getCommonLocation(), _mapZoomLevel);

      setMapCenterInWorldPixel(worldPixel);

      paint();
   }

   public void removeMousePositionListener(final IGeoPositionListener listener) {
      _allMousePositionListeners.remove(listener);
   }

   /**
    * Reload the map by discarding all cached tiles and entries in the loading queue
    */
   public synchronized void resetAll() {

      _mp.resetAll(false);

      paint();
   }

   public void resetHoveredMapPoint() {

      _hoveredMapPoint = null;

      _mapPointTooltip.hide();
      _mapPointTooltip_PhotoImage.hide();
      _mapPointTooltip_PhotoHistogram.hide();
   }

   public void resetMapPoints() {

      _hoveredMapPoint = null;
      _hoveredMarkerCluster = null;

      _isMarkerClusterSelected = false;
   }

   /**
    * Reset hovered tour data
    */
   public void resetTours_HoveredData() {

      _allPaintedMarkerClusters.clear();
      _hoveredMarkerCluster = null;

      _hoveredMapPoint = null;

      _allHoveredTourIds.clear();
      _allHoveredDevPoints.clear();
      _allHoveredSerieIndices.clear();

      if (_allPaintedTiles != null) {

         for (final Tile[] allTileArrays : _allPaintedTiles) {

            for (final Tile tile : allTileArrays) {

               if (tile != null) {

                  tile.allPainted_HoverRectangle.clear();
                  tile.allPainted_HoverTourID.clear();
                  tile.allPainted_HoverSerieIndices.clear();
               }
            }
         }
      }
   }

   public void resetTours_Photos() {

      // prevent that old photo images are displayed when a new tour should be displayed
      Map2PainterConfig.getPhotos().clear();
   }

   /**
    * Reset selected tour data
    */
   public void resetTours_SelectedData() {

      _hovered_SelectedTourId = -1;
      _hovered_SelectedSerieIndex_Behind = -1;
      _hovered_SelectedSerieIndex_Front = -1;
   }

   /**
    * Save star rating of the hovered/selected tours
    */
   private void saveRatingStars(final Photo photo) {

      final IPhotoServiceProvider photoServiceProvider = Photo.getPhotoServiceProvider();

      final int hoveredRatingStars = photo.ratingStars;
      int newRatingStars = photo.hoveredStars;

      if (newRatingStars == hoveredRatingStars) {

         /**
          * Feature to remove rating stars:
          * <p>
          * When a rating star is hit and this rating is already set in the photo, the rating
          * stars are removed.
          */

         newRatingStars = 0;
      }

      photo.ratingStars = newRatingStars;

      final ArrayList<Photo> photos = new ArrayList<>();
      photos.add(photo);

      photoServiceProvider.saveStarRating(photos);
   }

   /**
    * Select or deselect a hovered photo
    *
    * @param photo
    * @param hoveredMapPoint
    */
   public void selectPhoto(final Photo photo, final PaintedMapPoint hoveredMapPoint) {

      _selectedPhoto = photo;
      _selectedPhotoMapPoint = hoveredMapPoint;

      _mapPointTooltip_PhotoImage.setupPhoto(hoveredMapPoint);
      _mapPointTooltip_PhotoHistogram.setupPhoto(hoveredMapPoint);

      // a photo selections border is painted with the photos in the background
      paint();
   }

   public void setCenterMapBy(final CenterMapBy centerMapBy) {
      _centerMapBy = centerMapBy;
   }

   public void setConfig_HoveredSelectedTour(final boolean isShowHoveredOrSelectedTour,
                                             final boolean isShowBreadcrumbs,

                                             final int breadcrumbItems,
                                             final RGB hoveredRGB,
                                             final int hoveredOpacity,
                                             final RGB hoveredAndSelectedRGB,
                                             final int hoveredAndSelectedOpacity,
                                             final RGB selectedRGB,
                                             final int selectedOpacity) {

// SET_FORMATTING_OFF

      _isShowHoveredOrSelectedTour                    = isShowHoveredOrSelectedTour;
      _isShowBreadcrumbs                              = isShowBreadcrumbs;


      _hoveredSelectedTour_Hovered_Color              = new Color(hoveredRGB);
      _hoveredSelectedTour_Hovered_Opacity            = hoveredOpacity;

      _hoveredSelectedTour_HoveredAndSelected_Color   = new Color(hoveredAndSelectedRGB);
      _hoveredSelectedTour_HoveredAndSelected_Opacity = hoveredAndSelectedOpacity;

      _hoveredSelectedTour_Selected_Color             = new Color(selectedRGB);
      _hoveredSelectedTour_Selected_Opacity           = selectedOpacity;

// SET_FORMATTING_ON

      _tourBreadcrumb.setVisibleBreadcrumbs(breadcrumbItems);

      if (isShowHoveredOrSelectedTour == false) {

         // hide hovered/selected tour
         resetTours_SelectedData();
      }

      resetTours_HoveredData();
   }

   public void setConfig_TourDirection(final boolean isShowTourDirection,
                                       final boolean isShowTourDirection_Always,
                                       final int markerGap,
                                       final int lineWidth,
                                       final float symbolSize,
                                       final RGB tourDirection_RGB) {

      _isDrawTourDirection = isShowTourDirection;
      _isDrawTourDirection_Always = isShowTourDirection_Always;

      _tourDirection_MarkerGap = markerGap;
      _tourDirection_LineWidth = lineWidth;
      _tourDirection_SymbolSize = symbolSize;
      _tourDirection_RGB = tourDirection_RGB;
   }

   /**
    * Set cursor only when needed, to optimize performance
    *
    * @param cursor
    */
   private void setCursorOptimized(final Cursor cursor) {

      if (cursor == _currentCursor) {
         return;
      }

      _currentCursor = cursor;

      setCursor(cursor);
   }

   /**
    * Set map dimming level for the current map factory, this will dim the map images
    *
    * @param isDimMap
    * @param dimLevel
    * @param dimColor
    * @param isUseMapDimColor
    * @param isBackgroundDark
    */
   public void setDimLevel(final boolean isDimMap,
                           final int dimLevel,
                           final RGB dimColor,
                           final boolean isUseMapDimColor,
                           final boolean isBackgroundDark) {

      _isMapBackgroundDark = isBackgroundDark;

      if (_mp != null) {
         _mp.setDimLevel(isDimMap, dimLevel, dimColor);
      }

      if (isDimMap && isUseMapDimColor) {

         final float dimFactor = dimLevel == 0
               ? 1
               : (10f - dimLevel) / 10f;

         final RGB transColor = new RGB(

               (int) (dimColor.red * dimFactor),
               (int) (dimColor.green * dimFactor),
               (int) (dimColor.blue * dimFactor));

         setTransparencyColor(transColor);
      }
   }

   public void setDirectPainter(final IDirectPainter directPainter) {

      _directMapPainter = directPainter;
   }

   /**
    * Activate/deactivate fast painting, map will be repainted afterwards.
    *
    * @param isFastMapPaintingActive
    */
   public void setIsFastMapPainting_Active(final boolean isFastMapPaintingActive) {

      _isFastMapPainting_Active = isFastMapPaintingActive;

      disposeOverlayImageCache();

      redraw();
   }

   public void setIsInInverseKeyboardPanning(final boolean isInInverseKeyboardPanning) {

      _isInInverseKeyboardPanning = isInInverseKeyboardPanning;
   }

   public void setIsMultipleTours(final boolean isMultipleTours) {

      _hoveredSelectedTour_CanSelectTour = isMultipleTours;
   }

   public void setIsShowTour(final boolean isShowTour) {

      _isShowTour = isShowTour;
   }

   /**
    * Set the legend for the map, the legend image will be disposed when the map is disposed,
    *
    * @param legend
    *           Legend for the map or <code>null</code> to disable the legend
    */
   public void setLegend(final MapLegend legend) {

      if (legend == null && _mapLegend != null) {

         // dispose legend image
         UI.disposeResource(_mapLegend.getImage());
      }

      _mapLegend = legend;
   }

   /**
    * When set to <code>false</code>, a loading image is displayed when the tile image is not in the
    * cache. When set to <code>true</code> a loading... image is not displayed which can confuse the
    * user because the map is not displaying the current state.
    *
    * @param isLiveView
    */
   public void setLiveView(final boolean isLiveView) {
      _isLiveView = isLiveView;
   }

   public void setLocations_Common(final List<TourLocation> allTourLocations) {

      _allCommonLocations = allTourLocations;

      paint();
   }

   public void setLocations_Tours(final List<TourLocation> allTourLocations) {

      _allTourLocations = allTourLocations;

      paint();
   }

   /**
    * Set the center of the map to a geo position (with lat/long) and redraw the map.
    *
    * @param geoPosition
    *           Center position in lat/lon
    */
   public synchronized void setMapCenter(final GeoPosition geoPosition) {

      if (_mp == null) {

         // this occurred when restore state had a wrong map provider

         setMapProvider(MapProviderManager.getDefaultMapProvider());

         return;
      }

      final java.awt.Point newMapCenter = _mp.geoToPixel(geoPosition, _mapZoomLevel);

      if (Thread.currentThread() == _displayThread) {

         setMapCenterInWorldPixel(newMapCenter);

      } else {

         // current thread is not the display thread

         _display.syncExec(() -> {
            if (!isDisposed()) {
               setMapCenterInWorldPixel(newMapCenter);
            }
         });
      }

      updateViewportData();
      updateTourToolTip();

      paint();
   }

   /**
    * Sets the center of the map {@link #_worldPixel_MapCenter} in world pixel coordinates with the
    * current zoom level
    *
    * @param newWorldPixelCenter
    */
   private void setMapCenterInWorldPixel(final Point2D newWorldPixelCenter) {

      _worldPixel_MapCenter = checkWorldPixel(newWorldPixelCenter);

      fireEvent_MousePosition();
   }

   /**
    * Set map context menu provider
    *
    * @param mapContextProvider
    */
   public void setMapContextProvider(final IMapContextMenuProvider mapContextProvider) {
      _mapContextMenuProvider = mapContextProvider;
   }

   /**
    * Center the map within the geo positions.
    *
    * @param geoTourPositions
    * @param isAdjustZoomLevel
    * @param requestedZoomLevelAdjustment
    */
   public void setMapPosition(final Set<GeoPosition> geoTourPositions,
                              final boolean isAdjustZoomLevel,
                              final int requestedZoomLevelAdjustment) {

      if (_mp == null) {
         return;
      }

      Rectangle wpTourRect;
      Rectangle wpMapRect;

      GeoPosition geoTourCenter;

      java.awt.Point wpTourCenter;
      java.awt.geom.Point2D.Double wpMapCenter;

      // keep current zoom level
      final int currentZoomLevel = _mapZoomLevel;

      final int minZoomLevel = _mp.getMinimumZoomLevel();
      final int maximumZoomLevel = _mp.getMaximumZoomLevel();

      int zoom = maximumZoomLevel;

      /**
       * Adjust tour zoom level to the requested zoom level, this is used that the tour is more
       * visible and not painted at the map border
       */
      final int zoomLevelDiff = isAdjustZoomLevel ? requestedZoomLevelAdjustment : 0;
      int tourZoomLevel = zoom - zoomLevelDiff;
      tourZoomLevel = tourZoomLevel > maximumZoomLevel ? maximumZoomLevel : tourZoomLevel;

      wpTourRect = getWorldPixelFromGeoPositions(geoTourPositions, tourZoomLevel);

      // get tour center in world pixel for the max zoom level
      wpTourCenter = new java.awt.Point(
            wpTourRect.x + wpTourRect.width / 2,
            wpTourRect.y + wpTourRect.height / 2);

      // set tour geo center in the center of the tour rectangle
      geoTourCenter = _mp.pixelToGeo(wpTourCenter, zoom);

      wpMapCenter = checkWorldPixel(_mp.geoToPixel(geoTourCenter, zoom), zoom);
      wpMapRect = getWorldPixel_TopLeft_Viewport(wpMapCenter);

      // use an offset that the slider are not at the map border and almost not visible
      final int offset = 30;

      // zoom out until the tour is smaller than the map viewport
      while ((wpTourRect.width + offset > wpMapRect.width) //
            || (wpTourRect.height + offset > wpMapRect.height)) {

         // check zoom level
         if (zoom - 1 < minZoomLevel) {
            // this should not occur -> a tour should not be larger than the earth
            break;
         }

         // zoom out
         zoom--;

         tourZoomLevel = zoom - zoomLevelDiff;
         tourZoomLevel = tourZoomLevel > maximumZoomLevel ? maximumZoomLevel : tourZoomLevel;

         wpTourRect = getWorldPixelFromGeoPositions(geoTourPositions, tourZoomLevel);

         wpMapCenter = checkWorldPixel(_mp.geoToPixel(geoTourCenter, zoom), zoom);
         wpMapRect = getWorldPixel_TopLeft_Viewport(wpMapCenter);
      }

      if (zoom != currentZoomLevel) {

         // set new zoom level ONLY when it was modified -> this will dispose old overlay images !!!

         setZoom(zoom);
      }

      // update map with with new map center position
      {
         // zoom position is the same as previous !!!
         // _mapZoomLevel == _mapZoomLevel

         // set new map center
         _worldPixel_MapCenter = wpMapCenter;

         updateViewportData();
      }

      fireEvent_MapInfo();
   }

   /**
    * Sets the map provider for the map and redraws the map
    *
    * @param mapProvider
    *           new map provider
    */
   public void setMapProvider(final MP mapProvider) {

      GeoPosition center = null;
      int zoom = 0;
      boolean refresh = false;

      if (_mp != null) {

         // stop downloading images for the old map provider
         _mp.resetAll(true);

         center = getMapGeoCenter();
         zoom = _mapZoomLevel;
         refresh = true;
      }

      _mp = mapProvider;

//      // check if the map is initialized
//      if (_worldPixelViewport == null) {
//         onResize();
//      }

      /*
       * !!! initialize map by setting the zoom level which setups all important data !!!
       */
      if (refresh) {

         setZoom(zoom);
         setMapCenter(center);

      } else {

         setZoom(mapProvider.getDefaultZoomLevel());
      }

      paint();
   }

   /**
    * Resets current tile factory and sets a new one. The new tile factory is displayed at the same
    * position as the previous tile factory
    *
    * @param mp
    */
   public synchronized void setMapProviderWithReset(final MP mp) {

      if (_mp != null) {
         // keep tiles with loading errors that they are not loaded again when the factory has not changed
         _mp.resetAll(_mp == mp);
      }

      _mp = mp;

      paint();
   }

   public void setMeasurementSystem(final float distanceUnitValue, final String distanceUnitLabel) {

      _distanceUnitValue = distanceUnitValue;
      _distanceUnitLabel = distanceUnitLabel;
   }

   /**
    * Set a key to uniquely identify overlays which is used to cache the overlays
    *
    * @param key
    */
   public void setOverlayKey(final String key) {
      _overlayKey = key;
   }

   public void setPaintedRatingStars(final Rectangle paintedRatingStars) {

      _paintedRatingStars = paintedRatingStars;
   }

   /**
    * @param isRedrawEnabled
    *           Set <code>true</code> to enable map drawing (which is the default). When
    *           <code>false</code>, map drawing is disabled.
    *           <p>
    *           This feature can enable the drawing of the map very late that flickering of the map
    *           is prevented when the map is setup.
    */
   public void setPainting(final boolean isRedrawEnabled) {

      _isMapPaintingEnabled = isRedrawEnabled;

      if (isRedrawEnabled) {
         paint();
      }
   }

   public void setPoi(final GeoPosition poiGeoPosition, final int zoomLevel, final String poiText) {

      _isPoiVisible = true;

      final PoiToolTip poiToolTip = getPoiTooltip();
      poiToolTip.geoPosition = poiGeoPosition;
      poiToolTip.setText(poiText);

      setZoom(zoomLevel);

      _isPoiPositionInViewport = updatePoiImageDevPosition();

      if (_isPoiPositionInViewport == false) {

         // recenter map only when poi is not visible

         setMapCenter(poiGeoPosition);
      }

      /*
       * When poi is set, it is possible that the mouse is already over the poi -> update tooltip
       */
      final Point devMouse = this.toControl(getDisplay().getCursorLocation());
      final int devMouseX = devMouse.x;
      final int devMouseY = devMouse.y;

      // check if mouse is within the poi image
      if (_isPoiVisible
            && (devMouseX > _poiImageDevPosition.x)
            && (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
            && (devMouseY > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
            && (devMouseY < _poiImageDevPosition.y + _poiImageBounds.height)) {

         showPoi();

      } else {
         setPoiVisible(false);
      }

      paint();
   }

   /**
    * Sets the visibility of the poi tooltip. Poi tooltip is visible when the tooltip is available
    * and the poi image is within the map view port
    *
    * @param isVisible
    *           <code>false</code> will hide the tooltip
    */
   private void setPoiVisible(final boolean isVisible) {

      if (_poi_Tooltip == null) {
         return;
      }

      if (isVisible) {

         if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {

            final Point poiDisplayPosition = toDisplay(_poiImageDevPosition);

            _poi_Tooltip.show(
                  poiDisplayPosition.x,
                  poiDisplayPosition.y,
                  _poiImageBounds.width,
                  _poiImageBounds.height,
                  _poi_Tooltip_OffsetY);
         }
      } else {
         _poi_Tooltip.hide();
      }
   }

   public void setShowDebugInfo(final boolean isShowDebugInfo, final boolean isShowTileBorder) {

      setShowDebugInfo(isShowDebugInfo, isShowTileBorder, false);
   }

   /**
    * Set if the tile borders should be drawn. Mainly used for debugging.
    *
    * @param isShowDebugInfo
    *           new value of this drawTileBorders
    * @param isShowTileBorder
    * @param isShowGeoGrid
    */
   public void setShowDebugInfo(final boolean isShowDebugInfo, final boolean isShowTileBorder, final boolean isShowGeoGrid) {

      _isShowDebug_TileInfo = isShowDebugInfo;
      _isShowDebug_TileBorder = isShowTileBorder;
      _isShowDebug_GeoGrid = isShowGeoGrid;

      paint();
   }

   /**
    * Legend will be drawn into the map when the visibility is <code>true</code>
    *
    * @param isVisibility
    */
   public void setShowLegend(final boolean isVisibility) {

      _isLegendVisible = isVisibility;
   }

   public void setShowMapPoint(final boolean isShowMapPoint) {

      _isMapPointVisible = isShowMapPoint;

      // prevent that hovered map points are displayed when map points are hidden, this happened
      _allPaintedClusterMarkers.clear();
      _allPaintedCommonLocations.clear();
      _allPaintedTourLocations.clear();
      _allPaintedMarkers.clear();
      _allPaintedPauses.clear();
      _allPaintedPhotos.clear();
      _allPaintedWayPoints.clear();

      paint();
   }

   /**
    * set status if overlays are painted, a {@link #paint()} must be called to update the map
    *
    * @param showOverlays
    *           set <code>true</code> to see the overlays, <code>false</code> to hide the overlays
    */
   public void setShowOverlays(final boolean showOverlays) {

      _isDrawOverlays = showOverlays;
   }

   public void setShowPOI(final boolean isShowPOI) {

      _isPoiVisible = isShowPOI;

      paint();
   }

   public void setShowScale(final boolean isScaleVisible) {

      _isScaleVisible = isScaleVisible;
   }

   /**
    * Set tours which are currently painted in the map
    *
    * @param allTourIds
    */
   public void setTourIds(final List<Long> allTourIds) {

      _allTourIds = allTourIds;
   }

   public void setTourToolTip(final TourToolTip tourToolTip) {

      _tourTooltip = tourToolTip;

      tourToolTip.addHideListener(event -> {

         // hide hovered area
         _tourTooltip_HoveredAreaContext = null;

         redraw();
      });
   }

   public void setTransparencyColor(final RGB transparentRGB) {

      int red = transparentRGB.red;
      final int green = transparentRGB.green;
      final int blue = transparentRGB.blue;

      /*
       * Ensure that the transparent color is not an exact color which may be used very likely
       */
      if (red == 0) {

         red = 1;

      } else if (red == 255) {

         red = 254;

      } else {

         red = red - 1;
      }

      final RGB newTransparentColor = new RGB(red, green, blue);

      if (newTransparentColor.equals(_mapTransparentRGB) == false) {

         // transparent color is modified

         updateTransparencyColor(newTransparentColor);
      }
   }

   private void setupGroupedLabels() {

      final String groupedMarkers = _mapConfig.groupedMarkers;

      if (groupedMarkers.equals(_groupedMarkers)) {

         // skip labels are not modified

         return;
      }

      _groupedMarkers = groupedMarkers;

      final String[] allDuplicatedMarkers = StringUtils.splitIntoLines(groupedMarkers);

      _allMapMarkerSkipLabels.clear();

      for (String markerLabel : allDuplicatedMarkers) {

         markerLabel = markerLabel.trim();

         if (markerLabel.length() > 0) {
            _allMapMarkerSkipLabels.add(markerLabel);
         }
      }
   }

   private void setupPainting(final Graphics2D g2d) {

//    Object textAntialiasingON = null;
//
//    textAntialiasingON = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
//    textAntialiasingON = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
//    textAntialiasingON = RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
//    textAntialiasingON = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR;
//    textAntialiasingON = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
//    textAntialiasingON = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR;
//    textAntialiasingON = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB;

//    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

//      final Toolkit tk = Toolkit.getDefaultToolkit();
//      final Map map = (Map) (tk.getDesktopProperty("awt.font.desktophints"));

      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            _mapConfig.isLabelAntialiased
                  ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                  : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

//    g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//    g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

//    final Map<TextAttribute, Object> fontAttributes = new HashMap<>();
//
//    fontAttributes.put(TextAttribute.SIZE, 12);
//    fontAttributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
//    fontAttributes.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
//    fontAttributes.put(TextAttribute.FAMILY, java.awt.Font.DIALOG);
//    fontAttributes.put(TextAttribute.FAMILY, java.awt.Font.SANS_SERIF);
//    fontAttributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRA_LIGHT);
//
//    _defaultFont = systemFont.deriveFont(fontAttributes);
//    _defaultFont = UI.AWT_DIALOG_FONT.deriveFont(fontAttributes);
//    _defaultFont = UI.AWT_FONT_ARIAL_12.deriveFont(fontAttributes);

//    UI.dumpAllFonts();
//
//    final java.awt.Font systemFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12); //$NON-NLS-1$
//    final java.awt.Font systemFont = new java.awt.Font("Liberation Sans", java.awt.Font.PLAIN, 12); //$NON-NLS-1$
//    final java.awt.Font systemFont = new java.awt.Font("Segoe UI Light", java.awt.Font.PLAIN, 12); //$NON-NLS-1$

      final String labelFontName = _mapConfig.labelFontName;
      final int labelFontSize = _mapConfig.labelFontSize;

      _labelFontAWT = new java.awt.Font(labelFontName, java.awt.Font.PLAIN, labelFontSize);
      _clusterFontAWT = new java.awt.Font(labelFontName, java.awt.Font.PLAIN, _mapConfig.clusterSymbol_Size * 2);

      g2d.setFont(_labelFontAWT);
   }

   private void setupPainting_SWT() {

      final String labelFontName = _mapConfig.labelFontName;
      final int labelFontSize = _mapConfig.labelFontSize;

      final int labelFontSizeScaled = (int) (labelFontSize / _deviceScaling);

      if (labelFontName.equals(_labelFontName) == false || labelFontSizeScaled != _labelFontSize) {

         // font is changed -> recreate it

         UI.disposeResource(_labelFontSWT);

         _labelFontName = labelFontName;
         _labelFontSize = (int) (labelFontSize / _deviceScaling);

         // awt and swt font have not the same size
         final int swtFontSize = (int) (_labelFontSize * 1f);

         _labelFontSWT = new Font(_display, _labelFontName, swtFontSize, SWT.NORMAL);
      }
   }

   /**
    * Set the zoom level for the map and centers the map to the previous center. The zoom level is
    * checked if the map provider supports the requested zoom level.
    * <p>
    * The map is initialize when this is not yet done be setting all internal data !!!
    *
    * @param newZoomLevel
    *           zoom level for the map, it is adjusted to the min/max zoom levels
    */
   public void setZoom(final int newZoomLevel) {

      setZoom(newZoomLevel, CenterMapBy.Map);
   }

   /**
    * Set the zoom level for the map and centers the map to <code>centerMapBy</code>. The zoom level
    * is checked if the map provider supports the requested zoom level.
    * <p>
    * The map is initialize when this is not yet done be setting all internal data !!!
    *
    * @param newZoomLevel
    *           zoom level for the map, it is adjusted to the min/max zoom levels
    * @param centerMapBy
    */
   public void setZoom(final int newZoomLevel, final CenterMapBy centerMapBy) {

      if (_mp == null) {
         return;
      }

      final int oldZoomLevel = _mapZoomLevel;

      /*
       * Check if the requested zoom level is within the bounds of the map provider
       */
      int adjustedZoomLevel = newZoomLevel;
      final int mpMinimumZoomLevel = _mp.getMinimumZoomLevel();
      final int mpMaximumZoomLevel = _mp.getMaximumZoomLevel();
      if (((newZoomLevel < mpMinimumZoomLevel) || (newZoomLevel > mpMaximumZoomLevel))) {
         adjustedZoomLevel = Math.max(newZoomLevel, mpMinimumZoomLevel);
         adjustedZoomLevel = Math.min(adjustedZoomLevel, mpMaximumZoomLevel);
      }

      boolean isNewZoomLevel = false;

      // check if zoom level has changed
      if (oldZoomLevel == adjustedZoomLevel) {

         // this is disabled that a double click can set the center of the map

         // return;

      } else {

         // a new zoom level is set

         isNewZoomLevel = true;
      }

      if (oldZoomLevel != adjustedZoomLevel) {

         // zoom level has changed -> stop downloading images for the old zoom level
         _mp.resetAll(true);
      }

      final Dimension oldMapTileSize = _mp.getMapTileSize(oldZoomLevel);

      // check if map is initialized or zoom level has not changed
      Point2D wpCurrentMapCenter = _worldPixel_MapCenter;
      if (wpCurrentMapCenter == null) {

         // setup map center

         initMap();

         wpCurrentMapCenter = _worldPixel_MapCenter;
      }

      _mapZoomLevel = adjustedZoomLevel;

      // update values for the new zoom level !!!
      _mapTileSize = _mp.getMapTileSize(adjustedZoomLevel);

      final double relativeWidth = (double) _mapTileSize.width / oldMapTileSize.width;
      final double relativeHeight = (double) _mapTileSize.height / oldMapTileSize.height;

      Point2D.Double wpNewMapCenter;

      if (CenterMapBy.Mouse.equals(centerMapBy)

            // fixes this "issue" https://github.com/wolfgang-ch/mytourbook/issues/370
            && isNewZoomLevel) {

         // set map center to the current mouse position but only when a new zoom level is set !!!

         final Rectangle wpViewPort = _worldPixel_TopLeft_Viewport;

         wpCurrentMapCenter = new Point2D.Double(
               wpViewPort.x + _mouseMove_DevPosition_X_Last,
               wpViewPort.y + _mouseMove_DevPosition_Y_Last);

      } else {

         // for any other cases: center zoom to the map center

         // this is also the zoom behavior until 18.5
      }

      wpNewMapCenter = new Point2D.Double(
            wpCurrentMapCenter.getX() * relativeWidth,
            wpCurrentMapCenter.getY() * relativeHeight);

      setMapCenterInWorldPixel(wpNewMapCenter);

      updateViewportData();
      updateTourToolTip();
//    updatePoiVisibility();

      resetMapPoints();

      isTourHovered();
      paint();

      // update zoom level in status bar
      fireEvent_MapInfo();

      fireEvent_MapPosition(true);
   }

   /**
    * @param boundingBox
    *
    * @return Returns zoom level or -1 when bounding box is <code>null</code>.
    */
   public int setZoomToBoundingBox(final String boundingBox) {

      if (boundingBox == null) {
         return -1;
      }

      final Set<GeoPosition> latLonPositions = getBoundingBoxPositions(boundingBox);
      if (latLonPositions == null) {
         return -1;
      }

      final MP mp = getMapProvider();

      final int maximumZoomLevel = mp.getMaximumZoomLevel();
      int zoom = mp.getMinimumZoomLevel();

      Rectangle positionRect = getWorldPixelFromGeoPositions(latLonPositions, zoom);
      Rectangle viewport = getWorldPixelViewport();

      // zoom in until bounding box is larger than the viewport
      while ((positionRect.width < viewport.width) && (positionRect.height < viewport.height)) {

         // center position in the map
         final java.awt.Point center = new java.awt.Point(
               positionRect.x + positionRect.width / 2,
               positionRect.y + positionRect.height / 2);

         setMapCenter(mp.pixelToGeo(center, zoom));

         zoom++;

         // check zoom level
         if (zoom >= maximumZoomLevel) {
            break;
         }

         setZoom(zoom);

         positionRect = getWorldPixelFromGeoPositions(latLonPositions, zoom);
         viewport = getWorldPixelViewport();
      }

      // the algorithm generated a larger zoom level as necessary
      zoom--;

      setZoom(zoom);

      return zoom;
   }

   /**
    * @param tourGeoFilter
    *           Show geo grid box for this geo filter, when <code>null</code> the selected grid box
    *           is set to hidden
    */
   public void showGeoSearchGrid(final TourGeoFilter tourGeoFilter) {

      if (_mp == null) {

         // the map has currently no map provider
         return;
      }

      grid_UpdatePaintingStateData();

      if (tourGeoFilter == null) {

         // hide geo grid
         _geoGrid_Data_Selected = null;
         _geoGrid_TourGeoFilter = null;

         _isFastMapPainting_Active = false;

         redraw();

      } else {

         // show requested geo grid

         _geoGrid_TourGeoFilter = tourGeoFilter;

         // geo grid is displayed
         _isFastMapPainting_Active = true;

         final boolean isSyncMapPosition = Util.getStateBoolean(_geoFilterState,
               TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION,
               TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION_DEFAULT);

         _geoGrid_MapZoomLevel = tourGeoFilter.mapZoomLevel;
         _geoGrid_MapGeoCenter = tourGeoFilter.mapGeoCenter;

         if (isSyncMapPosition) {

            // set zoom level first, that recalculation is correct
            setZoom(_geoGrid_MapZoomLevel);
         }

         MapGridData mapGridData = tourGeoFilter.mapGridData;

         if (mapGridData == null) {

            // This can occur when geofilter is loaded from xml file and not created in the map

            // create map grid box from tour geo filter
            mapGridData = new MapGridData();

            final GeoPosition geo_Start = tourGeoFilter.geoLocation_TopLeft;
            final GeoPosition geo_End = tourGeoFilter.geoLocation_BottomRight;

            mapGridData.geo_Start = geo_Start;
            mapGridData.geo_End = geo_End;

            grid_Convert_StartEnd_2_TopLeft(geo_Start, geo_End, mapGridData);

            tourGeoFilter.mapGridData = mapGridData;
         }

         _geoGrid_Data_Selected = mapGridData;

         final Rectangle world_MapViewPort = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);

         /*
          * Reposition map
          */
         if (isSyncMapPosition) {

            // check if grid box is already visible

            if (world_MapViewPort.contains(_geoGrid_Data_Selected.world_Start)
                  && world_MapViewPort.contains(_geoGrid_Data_Selected.world_End)) {

               // grid box is visible -> nothing to do

            } else {

               // recenter map to make it visible

               setMapCenter(new GeoPosition(_geoGrid_MapGeoCenter.latitude, _geoGrid_MapGeoCenter.longitude));
            }

         } else {

            // map is not synched but location is wrong

            updateViewportData();
            paint();
         }
      }
   }

   private void showPoi() {

      if (_poi_Tooltip != null && _poi_Tooltip.isVisible()) {
         // poi is hidden
         return;
      }

      final PoiToolTip poiTT = getPoiTooltip();
      final Point poiDisplayPosition = this.toDisplay(_poiImageDevPosition);

      poiTT.show(
            poiDisplayPosition.x,
            poiDisplayPosition.y,
            _poiImageBounds.width,
            _poiImageBounds.height,
            _poi_Tooltip_OffsetY);
   }

   public MapTourBreadcrumb tourBreadcrumb() {
      return _tourBreadcrumb;
   }

   public void updateGraphColors() {

      _overlayAlpha = _prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY)

            ? _prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY)

            // no opacity
            : 0xff;
   }

   public void updateMapOptions() {

      final String drawSymbol = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE);

      _prefOptions_IsDrawSquare = drawSymbol.equals(Map2_Appearance.PLOT_TYPE_SQUARE);
      _prefOptions_LineWidth = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH);
      _prefOptions_BorderWidth = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH);

      final boolean isCutOffLinesInPauses = _prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_IS_CUT_OFF_LINES_IN_PAUSES);
      _prefOptions_isCutOffLinesInPauses = isCutOffLinesInPauses && drawSymbol.equals(Map2_Appearance.PLOT_TYPE_LINE);
   }

   public void updatePhotoOptions() {

      _isEnlargeSmallImages = Util.getStateBoolean(_state_Map2,
            SlideoutMap2_PhotoOptions.STATE_IS_ENLARGE_SMALL_IMAGES,
            SlideoutMap2_PhotoOptions.STATE_IS_ENLARGE_SMALL_IMAGES_DEFAULT);

      _isShowHQPhotoImages = Util.getStateBoolean(_state_Map2,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_THUMB_HQ_IMAGES,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT);

      _isShowPhotoAdjustments = Util.getStateBoolean(_state_Map2,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_ADJUSTMENTS,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_ADJUSTMENTS_DEFAULT);

      _isPreloadHQImages = Util.getStateBoolean(_state_Map2,
            SlideoutMap2_PhotoOptions.STATE_IS_PRELOAD_HQ_IMAGES,
            SlideoutMap2_PhotoOptions.STATE_IS_PRELOAD_HQ_IMAGES_DEFAULT);

      if (_isPreloadHQImages == false) {

         // cleanup loading queue

         PhotoLoadManager.stopImageLoading(true);
      }
   }

   /**
    * @return Returns <code>true</code> when the POI image is visible and the position is set in
    *         {@link #_poiImageDevPosition}
    */
   private boolean updatePoiImageDevPosition() {

      final GeoPosition poiGeoPosition = getPoiTooltip().geoPosition;
      if (poiGeoPosition == null) {
         return false;
      }

      // get world position for the poi coordinates
      final java.awt.Point worldPoiPos = _mp.geoToPixel(poiGeoPosition, _mapZoomLevel);

      // adjust view port to contain the poi image
      final Rectangle adjustedViewport = new Rectangle(
            _worldPixel_TopLeft_Viewport.x,
            _worldPixel_TopLeft_Viewport.y,
            _worldPixel_TopLeft_Viewport.width,
            _worldPixel_TopLeft_Viewport.height);

      adjustedViewport.x -= _poiImageBounds.width;
      adjustedViewport.y -= _poiImageBounds.height;
      adjustedViewport.width += _poiImageBounds.width * 2;
      adjustedViewport.height += _poiImageBounds.height * 2;

      // check if poi is visible
      if (adjustedViewport.intersects(
            worldPoiPos.x - _poiImageBounds.width / 2,
            worldPoiPos.y - _poiImageBounds.height,
            _poiImageBounds.width,
            _poiImageBounds.height)) {

         // convert world position into device position
         final int devPoiPosX = worldPoiPos.x - _worldPixel_TopLeft_Viewport.x;
         final int devPoiPosY = worldPoiPos.y - _worldPixel_TopLeft_Viewport.y;

         // get poi size
         final int poiImageWidth = _poiImageBounds.width;
         final int poiImageHeight = _poiImageBounds.height;

         _poiImageDevPosition.x = devPoiPosX - (poiImageWidth / 2);
         _poiImageDevPosition.y = devPoiPosY - poiImageHeight;

         return true;

      } else {

         return false;
      }
   }

   /**
    * show poi info when mouse is within the poi image
    */
   private void updatePoiVisibility() {

      boolean isVisible = false;

      if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {

         final Point displayMouse = _display.getCursorLocation();
         final Point devMouse = this.toControl(displayMouse);

         final int devMouseX = devMouse.x;
         final int devMouseY = devMouse.y;

         if ((devMouseX > _poiImageDevPosition.x)
               && (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
               && (devMouseY > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
               && (devMouseY < _poiImageDevPosition.y + _poiImageBounds.height)) {

            isVisible = true;
         }
      }

      setPoiVisible(isVisible);
   }

   public void updateTooltips() {

      final boolean isShowPhotoAdjustments = _isShowPhotoAdjustments && _isShowHQPhotoImages;

      _mapPointTooltip_PhotoImage.setShowPhotoAdjustements(isShowPhotoAdjustments);
      _mapPointTooltip_PhotoHistogram.setShowPhotoAdjustements(isShowPhotoAdjustments);
   }

   /**
    * Update tour tool tip, this must be done after the view port data are updated
    */
   private void updateTourToolTip() {

      if (_mp != null && _tourTooltip != null && _tourTooltip.isActive()) {

         /*
          * redraw must be forced because the hovered area can be the same but can be at a different
          * location
          */
         updateTourToolTip_HoveredArea();

         _tourTooltip.update();
      }
   }

   /**
    * Set hovered area context for the current mouse position or <code>null</code> when a tour
    * hovered area (e.g. way point) is not hovered.
    *
    * @param isForceRedraw
    */
   private void updateTourToolTip_HoveredArea() {

      final HoveredAreaContext oldHoveredContext = _tourTooltip_HoveredAreaContext;
      HoveredAreaContext newHoveredContext = null;

      final ArrayList<ITourToolTipProvider> toolTipProvider = _tourTooltip.getToolTipProvider();

      /*
       * check tour info tool tip provider as first
       */
      for (final IToolTipProvider tttProvider : toolTipProvider) {

         if (tttProvider instanceof IInfoToolTipProvider) {

            final HoveredAreaContext hoveredContext = ((IInfoToolTipProvider) tttProvider).getHoveredContext(
                  _mouseMove_DevPosition_X,
                  _mouseMove_DevPosition_Y);

            if (hoveredContext != null) {
               newHoveredContext = hoveredContext;
               break;
            }
         }
      }

      /*
       * check map tool tip provider as second
       */
      if (newHoveredContext == null) {

         for (final IToolTipProvider tttProvider : toolTipProvider) {

            if (tttProvider instanceof IMapToolTipProvider) {

               final HoveredAreaContext hoveredContext = ((IMapToolTipProvider) tttProvider).getHoveredContext(
                     _mouseMove_DevPosition_X,
                     _mouseMove_DevPosition_Y,
                     _worldPixel_TopLeft_Viewport,
                     _mp,
                     _mapZoomLevel,
                     _tilePixelSize,
                     null);

               if (hoveredContext != null) {
                  newHoveredContext = hoveredContext;
                  break;
               }
            }
         }
      }

      _tourTooltip_HoveredAreaContext = newHoveredContext;

      // update tool tip control
      _tourTooltip.setHoveredContext(_tourTooltip_HoveredAreaContext);

      if (_tourTooltip_HoveredAreaContext != null) {
         redraw();
      }

      /*
       * Hide hovered area, this must be done because when a tile do not contain a way point, the
       * hovered area can sill be displayed when another position is set with setMapCenter()
       */
      if (oldHoveredContext != null && _tourTooltip_HoveredAreaContext == null) {

         // update tool tip because it has it's own mouse move listener for the map
         _tourTooltip.hideHoveredArea();

         redraw();
      }
   }

   /**
    * Sets all viewport data which are necessary to draw the map tiles in
    * {@link #paint_30_Tiles(GC)}. Some values are cached to optimize performance.
    * <p>
    * {@link #_worldPixel_MapCenter} and {@link #_mapZoomLevel} are the base values for the other
    * viewport fields.
    */
   private void updateViewportData() {

      if (_mp == null) {

         // the map has currently no map provider
         return;
      }

      _worldPixel_TopLeft_Viewport = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);

      final int worldTopLeft_ViewportX = _worldPixel_TopLeft_Viewport.x;
      final int worldTopLeft_ViewportY = _worldPixel_TopLeft_Viewport.y;
      final int visiblePixelWidth = _worldPixel_TopLeft_Viewport.width;
      final int visiblePixelHeight = _worldPixel_TopLeft_Viewport.height;

      _devMapViewport = new Rectangle(0, 0, visiblePixelWidth, visiblePixelHeight);

      _mapTileSize = _mp.getMapTileSize(_mapZoomLevel);
      _tilePixelSize = _mp.getTileSize();

      // get the visible tiles which can be displayed in the viewport area
      final int numTileWidth = (int) Math.ceil((double) visiblePixelWidth / (double) _tilePixelSize);
      final int numTileHeight = (int) Math.ceil((double) visiblePixelHeight / (double) _tilePixelSize);

      /*
       * tileOffsetX and tileOffsetY are the x- and y-values for the offset of the visible screen to
       * the map's origin.
       */
      final int tileOffsetX = (int) Math.floor((double) worldTopLeft_ViewportX / (double) _tilePixelSize);
      final int tileOffsetY = (int) Math.floor((double) worldTopLeft_ViewportY / (double) _tilePixelSize);

      _tilePos_MinX = tileOffsetX;
      _tilePos_MinY = tileOffsetY;
      _tilePos_MaxX = _tilePos_MinX + numTileWidth;
      _tilePos_MaxY = _tilePos_MinY + numTileHeight;

      _allPaintedTiles = new Tile[numTileWidth + 1][numTileHeight + 1];

      /*
       * Pixel size for one geo grid, 0.01 degree
       */
      final Point2D.Double viewportMapCenter_worldPixel = new Point2D.Double(
            worldTopLeft_ViewportX + visiblePixelWidth / 2,
            worldTopLeft_ViewportY + visiblePixelHeight / 2);

      final GeoPosition geoPos = _mp.pixelToGeo(viewportMapCenter_worldPixel, _mapZoomLevel);

      // round to 0.01
      final double geoLat1 = Math.round(geoPos.latitude * 100) / 100.0;
      final double geoLon1 = Math.round(geoPos.longitude * 100) / 100.0;
      final double geoLat2 = geoLat1 + 0.01;
      final double geoLon2 = geoLon1 + 0.01;

      final Point2D.Double worldGrid1 = _mp.geoToPixelDouble(new GeoPosition(geoLat1, geoLon1), _mapZoomLevel);
      final Point2D.Double worldGrid2 = _mp.geoToPixelDouble(new GeoPosition(geoLat2, geoLon2), _mapZoomLevel);

      _devGridPixelSize_X = Math.abs(worldGrid2.x - worldGrid1.x);
      _devGridPixelSize_Y = Math.abs(worldGrid2.y - worldGrid1.y);

      grid_UpdateGeoGridData();

      resetMapPoints();
      PhotoLoadManager.stopImageLoading(true);
   }

   public void zoomIn(final CenterMapBy centerMapBy) {

      setZoom(_mapZoomLevel + 1, centerMapBy);

      paint();
   }

   public void zoomOut(final CenterMapBy centerMapBy) {

      setZoom(_mapZoomLevel - 1, centerMapBy);

      paint();
   }

}
