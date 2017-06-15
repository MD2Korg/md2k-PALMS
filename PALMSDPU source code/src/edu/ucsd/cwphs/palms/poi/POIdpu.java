package edu.ucsd.cwphs.palms.poi;

import java.util.ArrayList;

import org.json.JSONObject;

import edu.ucsd.cwphs.palms.gps.WayPoint;

public class POIdpu {
	private double lat = WayPoint.UNKNOWNCOORDINATES;
	private double lon = WayPoint.UNKNOWNCOORDINATES;
	private int buffer = 300;
	private int searchRadius = 1000;			// expand radius when searching

	private String types = "";			// Separated by |
	ArrayList<POI> results = new ArrayList<POI>();
	DAOpoi dao = new DAOpoi();
	GooglePlaces googlePlaces = new GooglePlaces(null);
	YelpPlaces yelpPlaces = new YelpPlaces();

	// TODO: need to pass google/yelp authorization parameters
	// TODO: Need to return error reason on database connection failure

	public POIdpu() {
	}

	// close any open DB connections
	public void close(){
		if (dao != null)
			dao.closeDB();
	}


	public void setParameters(double lat, double lon, int buffer, String types){
		this.lat = lat;
		this.lon = lon;
		this.buffer = buffer;
		this.types = types;
	}

	public String setParametersFromJSON(String json){
		try {
			JSONObject obj = new JSONObject(json);
			lat = getJSONDouble(obj, "lat");
			lon = getJSONDouble(obj, "lon");
			int i = getJSONInt(obj, "buffer");
			if (i == -1)
				i = 300;
			buffer = i;
			types = getJSONString(obj,"types");
		}
		catch (Exception ex){}
		return null;	// success
	}


	private Double getJSONDouble(JSONObject obj, String key){
		Double d = WayPoint.UNKNOWNCOORDINATES;
		try {
			d = obj.getDouble(key);
		}
		catch (Exception ex){
		}
		return d;
	}

	private int getJSONInt(JSONObject obj, String key){
		int i = -1;
		try {
			i = obj.getInt(key);
		}
		catch (Exception ex){
		}
		return i;
	}

	private String getJSONString(JSONObject obj, String key){
		String s = null;
		try {
			s = obj.getString(key);
		}
		catch (Exception ex){
		}
		return s;
	}


	public ArrayList<POI> getPOIAlongRoute(double lat1, double lon1, double lat2, double lon2, int radius, String types){
		results = dao.findPOIAlongRoute(lat1, lon1, lat2, lon2, radius, types);

		if (results.size() == 0){
			getPOI(lat1, lon1, searchRadius, types);
			results = dao.findPOIAlongRoute(lat1, lon1, lat2, lon2, radius, types);	
		}
		return results;
	}

	public ArrayList<POI> getPOI(double lat, double lon, int radius, String types){
		POI poi;
		boolean expandSearch = true;		// assume we need to expand the search area to radius
		results = new ArrayList<POI>();
		ArrayList<POI> yelpResults = new ArrayList<POI>();
		ArrayList<POI> googleResults = new ArrayList<POI>();

		// TODO:  When types == null?  Implies user wants everything, but local database will only contain previous
		//			search results; including nothing nearby


		// first check local database
		results = dao.getNearBy(lat, lon, radius, types);
		if (results.size() > 0) {
			// found some results in local db -- remove no POI entries
			for (int i=0; i<results.size(); ){
				poi = results.get(i);
				if (poi.isNothingNearBy()) {
					if (poi.getBuffer() >= radius)		
						expandSearch = false;	// no need to expand search
					results.remove(i);			// remove NothingNearBy entry
				} // endif
				else i++;
			} // end for
			if (results.size() > 0)
				return results;					// got POIs

			if (!expandSearch)
				return results;					// don't expand search - return empty arrayList
			// else fall thru and search using larger radius
		} // endif check local database

		// nothing in local - check Yelp Places
		yelpResults = yelpPlaces.getNearBy(lat, lon, searchRadius, types);
		if (yelpResults == null)
			yelpResults = new ArrayList<POI>();				// create empty list

		else {
			if (yelpResults.size() > 0) {
				// got results - insert in local
				dao.insertPOIs(yelpResults);
				//				return results;
			}
			else {
				// no results - insert a nothing-nearby entry
				poi = new POI(POI.SCOPEYELP, types, types, lat, lon, searchRadius);
				dao.insertPOI(poi);
			}
		}

		// check Google and add to those found in Yelp	

		if (GooglePlacesTypes.isValid(types)) {
			// type is valid, call google
			googleResults = googlePlaces.getNearBy(lat, lon, searchRadius, types);

			if (googleResults == null)
				googleResults = new ArrayList<POI>();				// create empty list

			else {
				if (googleResults.size() > 0) {
					// got results - insert in local
					dao.insertPOIs(googleResults);
					//				return results;
				}
				else {
					// no results - insert a nothing-nearby entry
					poi = new POI(POI.SCOPEGOOGLE, types, types, lat, lon, searchRadius);
					dao.insertPOI(poi);
				}
			}
		}
		else 
			// type is not valid, don't search because Google will ignore type and return everything
			googleResults = new ArrayList<POI>();				// create empty list

		// start with empty result set
		results = new ArrayList<POI>();

		// add Yelp results
		for (int i=0; i < yelpResults.size(); i++){
			if (yelpResults.get(i).isNearBy(lat, lon, radius))
				results.add(yelpResults.get(i));			
		}

		// add Google results
		for (int i=0; i < googleResults.size(); i++){
			if (googleResults.get(i).isNearBy(lat, lon, radius))
				results.add(googleResults.get(i));			
		}

		return results;
	}

	public ArrayList<POI> process(){
		return getPOI(lat, lon, buffer, types);
	}

	public int getPOICount(double lat, double lon, int radius, String types){
		getPOI(lat, lon, buffer, types);
		return results.size();

	}

	public ArrayList<POI> getResultSet(){
		return results;
	}

	public String getResultSetJSON(){
		StringBuilder sb = new StringBuilder("{\"poi_results\":[");
		int size = results.size();
		if (size > 0){
			for (int index = 0; index < (size-1); index++){
				sb.append(results.get(index).toJSON() + ",");
			} // end for
			sb.append(results.get(size-1).toJSON());	// last item
		}
		sb.append("]}");
		return sb.toString();
	}

}
