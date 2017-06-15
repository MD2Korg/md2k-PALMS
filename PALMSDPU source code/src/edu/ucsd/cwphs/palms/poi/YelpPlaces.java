package edu.ucsd.cwphs.palms.poi;

import java.util.ArrayList;




import org.json.JSONArray;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import edu.ucsd.cwphs.palms.gps.WayPoint;
import edu.ucsd.cwphs.palms.poi.TwoStepOAuth;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class YelpPlaces {
	
	// keys registered to fraab@ucsd.edu
	private final String CONSUMER_KEY = "m0NoXnXamwoTxwCsRAdmvA";
	private final String CONSUMER_SECRET = "COqkbb7t9l7ZVhh8N6nAqwFIXCY";
	private final String TOKEN = "31qxM1cGL13tmN1BR_xuUCNPvYg3g7Cu";
	private final String TOKEN_SECRET = "dm1vIMp-_CQh70uYvQoDUNZ8JT8";
	private final int GOODFOR = 1;			// cannot cache YELP results beyond session, so keep for 1 day
	
	private final int ERROR = -999999;

	private final String API_HOST = "api.yelp.com";
	private final String SEARCH_PATH = "/v2/search";
	private final int SEARCH_LIMIT = 10;
	
	private static OAuthService service;
	private static Token accessToken;
	
	private static int apiUsageCount = 0;
	
	public YelpPlaces() {
		YelpPlaces.service =
				new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(CONSUMER_KEY)
				.apiSecret(CONSUMER_SECRET).build();
		YelpPlaces.accessToken = new Token(TOKEN, TOKEN_SECRET);
	}
	
	public YelpPlaces(String consumerKey, String consumerSecret, String token, String tokenSecret) {
		YelpPlaces.service =
				new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(consumerKey)
				.apiSecret(consumerSecret).build();
		YelpPlaces.accessToken = new Token(token, tokenSecret);
	}
	
	public int getApiUsageCount(){
		return apiUsageCount;
	}
	
	public int clearApiUsageCount(){
		int i = apiUsageCount;
		apiUsageCount = 0;
		return i;
	}
	
	public ArrayList<POI> getNearBy(Double lat, Double lon, int radius){
		String json = searchForBusinessesByLocation(Double.toString(lat), Double.toString(lon), radius, null);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json, lat, lon, radius, "");		// any type
	}
	
	public ArrayList<POI> getNearBy(String lat, String lon, int radius){
		String json = searchForBusinessesByLocation(lat, lon, radius, null);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json,  parseDouble(lat), parseDouble(lon), radius, "");	// any type		
	}
	
	public ArrayList<POI> getNearBy(String lat, String lon, int radius, String requestedTypes){
		String json = searchForBusinessesByLocation(lat, lon, radius, requestedTypes);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json, parseDouble(lat), parseDouble(lon), radius, requestedTypes);	
	}
	
	public ArrayList<POI> getNearBy(Double lat, Double lon, int radius, String requestedTypes){
		String json = searchForBusinessesByLocation(Double.toString(lat), Double.toString(lon), radius, requestedTypes);
		if (gotError(json))
			return null;
		else
			return getPOIsFromJSON(json, lat, lon, radius, requestedTypes);	
	}
	
	public String searchForBusinessesByLocation(String lat, String lon, int radius, String requestedTypes) {
		OAuthRequest request = createOAuthRequest(SEARCH_PATH);
		if (requestedTypes != null && requestedTypes.length() > 0){
			requestedTypes.replace('|', ','); 				// replace pipes (used by Google, with commas used by Yelp		
			request.addQuerystringParameter("term", requestedTypes);
		}
		request.addQuerystringParameter("ll", lat + "," + lon);
		request.addQuerystringParameter("sort", "1");							// sort by distance
		request.addQuerystringParameter("radius_filter", Integer.toString(radius));
		request.addQuerystringParameter("limit", String.valueOf(SEARCH_LIMIT));
		return sendRequestAndGetResponse(request);
	}
	
	
	private ArrayList<POI> getPOIsFromJSON(String json, Double lat, Double lon, int radius, String requestedTypes){
		ArrayList<POI> results = new ArrayList<POI>();
		String name, placeId, vicinity = null, postalAddress = null;
		String types = null;
		String streetAddress="", city= "", state= "", zip = "";
		Double yelpLat, yelpLon;
		int distance = 0;
		
		JSONObject obj = new JSONObject(json);

		JSONArray array = obj.getJSONArray("businesses");
		if (array == null){
			EventLogger.logWarning("YelpPlaces.getPOIsFromJSON - no businesses found.");
			return results;
		}

		for (int index=0; index < array.length(); index++) {
			try {
				streetAddress="";
				city= "";
				state= "";
				zip = "";
				distance = -1;
				
				JSONObject item = array.getJSONObject(index);
					
				placeId = getJSONString(item ,"id");
				name = getJSONString(item, "name");
				distance = getJSONInt(item, "distance");
				
				if (distance == ERROR || distance > radius)
					continue;				// ignore if not within range
											// YELP sometimes returns POIs beyond the range
				
				JSONObject location = item.getJSONObject("location");
				if (location != null){
					JSONArray address = getJSONArray(location, "address");
					if (address != null && address.length() > 1)
						streetAddress = address.getString(0);
					else 
						streetAddress = "";
					city = getJSONString(location, "city");
					state = getJSONString(location, "state_code");
					zip = getJSONString(location, "postal_code");
					postalAddress = streetAddress + " " + city + " " + state + " " + zip;

					JSONArray neighborhoods = getJSONArray(location, "neighborhoods");
					if (neighborhoods != null)
						vicinity = neighborhoods.getString(0);
					
					JSONObject coordinate = getJSONObject(location, "coordinate");
					if (coordinate != null){
						yelpLat = getJSONDouble(coordinate, "latitude");
						yelpLon = getJSONDouble(coordinate, "longitude");
						
						if (yelpLat != WayPoint.UNKNOWNCOORDINATES && yelpLon != WayPoint.UNKNOWNCOORDINATES){
							lat = yelpLat;
							lon = yelpLon;
						}
					}
					
				} // end if (location != null)
				
				types = parseTypes(item);

				POI poi = new POI(placeId, name, POI.SCOPEYELP, types, requestedTypes, vicinity, postalAddress, lat, lon);
				poi.setDateToExpire(GOODFOR);
				poi.setDistance(distance);
				results.add(poi);
			}
			catch (Exception ex){
				EventLogger.logException("YelpPlaces.getPOIsfromJSON - error parsing JSON - index = "+ index, ex);
			}
		} // end for
		return results;
	}
	
	private JSONObject getJSONObject(JSONObject obj, String key){
		JSONObject o = null;
		try {
			o = obj.getJSONObject(key);
		}
		catch (Exception ex){
		}
		return o;
	}
	
	private JSONArray getJSONArray(JSONObject obj, String key){
		JSONArray a = null;
		try {
			a = obj.getJSONArray(key);
		}
		catch (Exception ex){
		}
		return a;
	}
	
	private String getJSONString(JSONObject obj, String key){
		String s = "";
		try {
			s = obj.getString(key);
		}
		catch (Exception ex){
		}
		return s;
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
	
	private Double parseDouble(String value){
		Double d = WayPoint.UNKNOWNCOORDINATES;
		try {
			d = Double.parseDouble(value);
		}
		catch (Exception ex){
		}
		return d;
	}
	
	
	private int getJSONInt(JSONObject obj, String key){
		int i = ERROR;
		try {
			i = (int) obj.getLong(key);
		}
		catch (Exception ex){
		}
		return i;
	}
	
	private String parseTypes(JSONObject item){
		String types = "";
		JSONArray categories = getJSONArray(item, "categories");
		if (categories == null){
			EventLogger.logWarning("YelpPlaces.parseTypes - no categories found.");
			return "";
		}
		// loop thru categories
			for (int index=0; index < categories.length(); index++){
				String type = categories.getJSONArray(index).getString(0);
				if (type.length()>0) {
					type = type.toLowerCase().trim();
					types = types + type + "|";
				}
			} // end for

		int i = types.lastIndexOf("|");	
		if (i > -1)
			types = types.substring(0, i);
		return types;
		}
		
	
	/*
	 * Low Level Methods
	 */

	private OAuthRequest createOAuthRequest(String path) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://" + API_HOST + path);
		return request;
	}


	private String sendRequestAndGetResponse(OAuthRequest request) {
		EventLogger.logDebug("YelpPlaces - GET Url:" + request.getCompleteUrl() + " ...");
		YelpPlaces.service.signRequest(YelpPlaces.accessToken, request);
		apiUsageCount++;
		Response response = request.send();
		if (response.getCode() != 200){
			EventLogger.logEvent("YelpPlaces - Get Failure: " + response.getMessage());	
			return null;
		}
		EventLogger.logDebug("YelpPlaces GET response:" + response.getBody());
		return response.getBody();
	}
	
	private boolean gotError(String json){
		if (json == null)
			return true;				// Error already logged
		JSONObject obj = new JSONObject(json);
		JSONObject error = getJSONObject(obj, "error");
		if (error != null){
			String errorMessage = getJSONString(error, "text");
			EventLogger.logError("YelpPlaces returned error:" + errorMessage);
			return true;
		}
		else
			return false;
	}
}
