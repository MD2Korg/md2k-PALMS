package edu.ucsd.cwphs.palms.poi;

import java.util.ArrayList;

import org.json.JSONObject;

import edu.ucsd.cwphs.palms.gps.WayPoint;

public class POIdpu {
	private double lat = WayPoint.UNKNOWNCOORDINATES;
	private double lon = WayPoint.UNKNOWNCOORDINATES;
	private int buffer = 300;
	private String types = "";			// Separated by |
	ArrayList<POI> results = new ArrayList<POI>();
	DAOpoi dao = new DAOpoi();
	GooglePlaces googlePlaces = new GooglePlaces(null);
	YelpPlaces yelpPlaces = new YelpPlaces();
	
	// TODO: need to pass google/yelp authorization parameters
	
	public POIdpu() {
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
	
	public ArrayList<POI> getPOI(double lat, double lon, int radius, String types){
		POI poi;
		results = new ArrayList<POI>();
		// first check local database
		results = dao.getNearBy(lat, lon, radius, types);
		if (results.size() > 0) {
			// found some results in local db -- remove no POI entries
			for (int i=0; i<results.size(); ){
				poi = results.get(i);
				if (poi.nothingNearBy())
					results.remove(i);
				else i++;
			}
			return results;
		}

		// nothing in local - check Google Places
		results = googlePlaces.getNearBy(lat, lon, radius, types);
		if (results != null) {
			if (results.size() > 0) {
				// got results - insert in local
				dao.insertPOIs(results);
				return results;
			}
			else {
				// no results - insert a nothing-nearby entry
				poi = new POI(POI.SCOPEGOOGLE, types, lat, lon);
				dao.insertPOI(poi);
			}
		}

		// check YELP if nothing in local or GooglePlaces			

		results = yelpPlaces.getNearBy(lat, lon, radius, types);
		if (results != null){
			if (results.size() > 0) {
				// got results - insert in local
				dao.insertPOIs(results);
				return results;
			}
			else {
				// no results - insert a nothing-nearby entry
				poi = new POI(POI.SCOPEYELP, types, lat, lon);
				dao.insertPOI(poi);
			}
		}	

		// no results - return empty array
		results = new ArrayList<POI>();
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
