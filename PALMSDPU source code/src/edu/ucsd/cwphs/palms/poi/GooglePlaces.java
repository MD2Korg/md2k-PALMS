package edu.ucsd.cwphs.palms.poi;

import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucsd.cwphs.palms.util.EventLogger;

public class GooglePlaces {

	//  *** replace with your API key for Google Places ***
	private String APIKEY = "INSERT_YOUR_API_KEY";
	private final String PLACESURL = "https://maps.googleapis.com/maps/api/place";
	private final String RETURNTYPE = "json";
	
	private final String NEARBYURL = PLACESURL + "/nearbysearch/" + RETURNTYPE + "?key=" + APIKEY + "&";
	private int GOODFOR = 30;			// expires in 30 days
	
	private static int apiUsageCount = 0;
	
	public GooglePlaces(String apiKey){
		setAPIkey(apiKey);
	}
	
	public void setAPIkey(String apiKey){
		if (apiKey != null)
			APIKEY = apiKey;
	}
	
	public int getApiUsageCount(){
		return apiUsageCount;
	}
	
	public ArrayList<POI> getNearBy(Double lat, Double lon, int radius){
		String api = NEARBYURL + "location=" + lat + "," + lon + "&radius=" + radius;
		String json = callGooglePlaces(api);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json);	
	}
	
	public ArrayList<POI> getNearBy(String lat, String lon, int radius){
		String api = NEARBYURL + "location=" + lat + "," + lon + "&radius=" + radius;
		String json = callGooglePlaces(api);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json);	
	}
	
	public ArrayList<POI> getNearBy(String lat, String lon, int radius, String types){
		String api = NEARBYURL + "location=" + lat + "," + lon + "&radius=" + radius;
		if (types != null && types.length() > 0)
				api = api + "&types=" + types;
		String json = callGooglePlaces(api);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json);	
	}
	
	public ArrayList<POI> getNearBy(Double lat, Double lon, int radius, String types){
		String api = NEARBYURL + "location=" + lat + "," + lon + "&radius=" + radius;
		if (types != null && types.length() > 0)
			api = api + "&types=" + types;
		String json = callGooglePlaces(api);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json);	
	}
	
	private ArrayList<POI> getPOIsFromJSON(String json){
		ArrayList<POI> results = new ArrayList<POI>();
		Double lat = -180.0, lon = -180.0;
		String name, placeId, vicinity = null, postalAddress = null;
		String types = null;
		
		JSONObject obj = new JSONObject(json);

		JSONArray array = obj.getJSONArray("results");
		if (array == null){
			EventLogger.logWarning(".fromJSON - no places found.");
			return results;
		}

		for (int index=0; index < array.length(); index++) {
			try {
				JSONObject item = array.getJSONObject(index);
				
				// parse lat/lon
				JSONObject geometry = item.getJSONObject("geometry");
				if (geometry == null) {
					EventLogger.logWarning(".fromJSON - no geometry found.");
				}
				else {
					JSONObject location = geometry.getJSONObject("location");
					if (location == null){
						EventLogger.logWarning(".fromJSON - no location found.");
					}
					else {
						lat = getJSONDouble(location, "lat");
						lon = getJSONDouble(location, "lng");
					}
				}
					
				placeId = getJSONString(item ,"id");
				name = getJSONString(item, "name");
				vicinity = getJSONString(item, "vicinity");
				postalAddress = getJSONString(item, "postal_address");
				if (postalAddress == null)
					postalAddress = vicinity;
				
				types = parseTypes(item);

				POI poi = new POI(placeId, name, POI.SCOPEGOOGLE, types, vicinity, postalAddress, lat, lon);
				poi.setDateToExpire(GOODFOR);
				results.add(poi);
			}
			catch (Exception ex){
				EventLogger.logException("GooglePlaces.getPOIsfromJSON - error parsing JSON - index = "+ index, ex);
			}
		} // end for
		return results;
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

	private Double getJSONDouble(JSONObject obj, String key){
		Double d = -999.0;
		try {
			d = obj.getDouble(key);
		}
		catch (Exception ex){
		}
		return d;
	}
	
	private String parseTypes(JSONObject item){
		String types = "";		
		JSONArray array = item.getJSONArray("types");
		if (array == null){
			EventLogger.logWarning(".fromJSON - no types found.");
			return "";
		}
		// loop thru array
			for (int index=0; index < array.length(); index++){
				types = types + array.getString(index) + "|";
				
			}

		int i = types.lastIndexOf("|");	
		if (i > -1)
			types = types.substring(0, i);
		return types;
		}
		
	
	/*
	 * Low Level Methods
	 */
	public String callGooglePlaces(String api) {
		String response = "";
		URL url = null;
		HttpURLConnection urlConn = null;
		DataInputStream dataStreamIn = null;

		try {
			url = new URL(api);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("Content-Type", "application/xml");
			urlConn.setRequestProperty("Accept", "application/xml");			
			urlConn.setRequestMethod("GET");
			urlConn.connect();

			EventLogger.logDebug("GooglePlaces GET URL:" + url.toString());

			// get the response
			String str = null;
			dataStreamIn = new DataInputStream(urlConn.getInputStream());
			while (null != ((str = dataStreamIn.readLine()))){
				response = response + str;
			}
			dataStreamIn.close();	

		} // end try
		catch (Exception ex){
			EventLogger.logException("GooglePlaces GET Failure - ", ex);
			response = null;
		}
		EventLogger.logDebug("GooglePlaces GET Response:" + response);
		return response;
	}
	
	private boolean gotError(String json){
		if (json == null)
			return true;				// Error already logged
		JSONObject obj = new JSONObject(json);

		String error = getJSONString(obj, "error_message");
		if (error != null){
			EventLogger.logError("GooglePlaces returned error:" + error);
			return true;
		}
		else {
			apiUsageCount++;
			return false;
		}
	}

}
