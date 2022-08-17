package de.byteholder.geoclipse.poi;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.byteholder.gpx.PointOfInterest;

public class GeoQuery extends Observable implements Runnable {

//	private final static String			URL				= "http://www.frankieandshadow.com/osm/search.xml?find="; //$NON-NLS-1$
//	private final static String			SEARCH_URL		= "http://gazetteer.openstreetmap.org/namefinder/search.xml?find="; //$NON-NLS-1$
	private final static String			SEARCH_URL		= "https://nominatim.openstreetmap.org/search?format=xml&addressdetails=0&q=";	//$NON-NLS-1$

	private ArrayList<PointOfInterest>	_searchResult	= new ArrayList<>();

	private Exception							_exception;

	private String								_query;

	public GeoQuery(final String query) {
		_query = query;
	}

	public void asyncFind() {

		final Job job = new Job(Messages.job_name_searchingPOI) {

			@Override
			protected IStatus run(final IProgressMonitor arg0) {
				GeoQuery.this.run();
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}

	public Exception getException() {
		return _exception;
	}

	public List<PointOfInterest> getSearchResult() {
		return _searchResult;
	}

	@Override
	public void run() {

		try {

			_searchResult.clear();

			final String uri = SEARCH_URL + URLEncoder.encode(_query, "utf8"); //$NON-NLS-1$

			SAXParserFactory.newInstance().newSAXParser().parse(uri, new GeoQuerySAXHandler(_searchResult));

		} catch (final Exception e) {
			_exception = e;
		}

		setChanged();
		notifyObservers();
	}

//	private List<PointOfInterest> find() throws Exception {
//
//		SAXParserFactory.newInstance().newSAXParser().parse(//
//				SEARCH_URL + URLEncoder.encode(_query, "utf8"), //
//				new GeoQuerySAXHandler(_searchResult));//$NON-NLS-1$
//
//		final Document document = parse(url);
//
//		final List<PointOfInterest> pois = parsePOIs(document.getRootElement());
//
//		return pois;
//	}

//	private Document parse(final URL url) throws DocumentException {
//
////		final SAXReader reader = new SAXReader();
////		final Document document = reader.read(url);
//
//		return document;
//	}

//	private PointOfInterest parsePOI(final Element poiElement) {
//
//		final PointOfInterest poi = new PointOfInterest();
//
//		final Attribute latitudeAttribute = poiElement.attribute("lat"); //$NON-NLS-1$
//		final Attribute longitudeAttribute = poiElement.attribute("lon"); //$NON-NLS-1$
//		final Attribute nameAttribute = poiElement.attribute("name"); //$NON-NLS-1$
//		final Attribute categoryAttribute = poiElement.attribute("category"); //$NON-NLS-1$
//		final Attribute infoAttribute = poiElement.attribute("info"); //$NON-NLS-1$
//		final Attribute typeAttribute = poiElement.attribute("type"); //$NON-NLS-1$
//		final Attribute zoomAttribute = poiElement.attribute("zoom"); //$NON-NLS-1$
//
//		double latitude = 0;
//		double longitude = 0;
//		int zoom = 8; // mks: dont know, if this is a good "fallback" value
//
//		try {
//			latitude = Double.parseDouble(latitudeAttribute.getText());
//			longitude = Double.parseDouble(longitudeAttribute.getText());
//			zoom = Integer.parseInt(zoomAttribute.getText());
//		} catch (final Exception e) {
//			// happens very often, because zoom is not always set
//		}
//
//		assert nameAttribute != null;
//		assert latitudeAttribute != null;
//		assert longitudeAttribute != null;
//
//		poi.setPosition(new GeoPosition(latitude, longitude));
//		poi.setName(nameAttribute.getText());
//		poi.setRecommendedZoom(zoom);
//
//		if (categoryAttribute != null) {
//			poi.setCategory(categoryAttribute.getText());
//		}
//
//		if (infoAttribute != null) {
//			poi.setInfo(infoAttribute.getText());
//		}
//
//		if (typeAttribute != null) {
//			poi.setType(typeAttribute.getText());
//		}
//
//		@SuppressWarnings("unchecked")
//		final List<Element> nearestPlaces = poiElement.elements("nearestplaces"); //$NON-NLS-1$
//
//		for (final Element place : nearestPlaces) {
//			poi.setNearestPlaces(parsePOIs(place));
//		}
//
//		return poi;
//	}
//
//	private List<PointOfInterest> parsePOIs(final Element root) {
//
//		@SuppressWarnings("unchecked")
//		final List<Element> searchResultElements = root.elements("named"); //$NON-NLS-1$
//
//		final List<PointOfInterest> pois = new ArrayList<PointOfInterest>();
//
//		for (final Element element : searchResultElements) {
//			pois.add(parsePOI(element));
//		}
//
//		Activator.getDefault().getLog().log(
//				new Status(Status.INFO, Activator.PLUGIN_ID, pois.size() + " POIs found for " + query)); //$NON-NLS-1$
//
//		return pois;
//	}

}
