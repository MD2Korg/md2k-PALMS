package edu.ucsd.cwphs.palms.poi;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import edu.ucsd.cwphs.palms.gps.WayPoint;

public class POI {
	public static final String SCOPEGOOGLE = "Google";
	public static final String SCOPEYELP = "Yelp";
	public static final String SCOPELOCAL = "Local";
	public static final String NOPOINEARBY = "No POI nearby";
	public static final int NOPOIEXPIRES = 90;
	
	public static final String SEPERATOR = ",";			// will this work??  Or should we parse JSON?
														// will need to parse JSON for Google places results
	
	private String types = "";
	private String placeId = "";
	private String name = "";
	private String scope = "";
	private String vicinity = "";
	private String postalAddress = "";
	
	private double lat = WayPoint.UNKNOWNCOORDINATES;
	private double lon = WayPoint.UNKNOWNCOORDINATES;
	
	private Date dateCached = null;
	private Date dateExpires = null;
	
	
	public POI(String placeId, String name, String scope, String types, String vicinity, String postalAddress, double lat, double lon){
		this.types = types;
		this.name = name;
		this.scope = scope;
		this.vicinity = vicinity;
		this.postalAddress = postalAddress;
		this.lat = lat;
		this.lon = lon;
		this.dateCached = new Date();				// set to current timestamp
		
		if (placeId == null || placeId.isEmpty())
			placeId = UUID.randomUUID().toString();	
		this.placeId = placeId;
		
	}
	
	public POI (String scope, String types, double lat, double lon){
		this.scope = scope;
		this.types = types;
		this.lat = lat;
		this.lon = lon;
		this.name = NOPOINEARBY;
		this.dateCached = new Date();
		setDateToExpire(NOPOIEXPIRES);
		this.placeId = UUID.randomUUID().toString();	
	}
	
	public POI(String line){
		int i = line.indexOf(SEPERATOR);
		int j = line.indexOf(SEPERATOR, i+1);
		int k = line.indexOf(SEPERATOR, j+1);
		int l = line.indexOf(SEPERATOR, k+1);
		
		if ((i != -1) && (j != -1) && (k != -1) && (l != -1) ){
			this.placeId = line.substring(0, i);
			this.scope = line.substring(i+1, j);
			this.types = line.substring(j+1, k);
			this.vicinity = line.substring(k+1, l);
			this.postalAddress = line.substring(l+1);
		}
		else
			this.placeId = null;
	}

	public boolean isValid(){
		if (scope == null)
			return false;
		else 
			return true;
	}
	
	public boolean nothingNearBy(){
		return name.equalsIgnoreCase(NOPOINEARBY);
	}
	
	public void setDateToExpire(int days){
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateCached);
		cal.add(Calendar.DAY_OF_YEAR, days);
		Long msec = cal.getTimeInMillis();
		dateExpires = new Date(msec);
	}
	
	public Double getLat(){
		return lat;
	}
	
	public Double getLon(){
		return lon;
	}
	
	public String getTypes(){
		return types;
	}
	
	public String getPlaceId(){
		return placeId;
	}
	
	public String getName(){
		return name;
	}
	
	public String getScope(){
		return scope;
	}
	
	public String getVicinity(){
		return vicinity;
	}
	
	public String getPostalAddress(){
		return postalAddress;
	}
	
	public Date getDateCached(){
		return dateCached;
	}
	
	public Date getDateExpires(){
		return dateExpires;
	}
	
	public boolean hasExpired(Date currentDateTime){
		if (dateExpires == null)
			return false;
		else
			return dateExpires.after(currentDateTime);
	}
	
	public String CSVheader(){
		String s = "placeId, name, scope, types, vicinity, postalAddress, lat, lon, ele";
		return s;
	}
	
	public String toCSV(){
		StringBuilder sb = new StringBuilder(placeId + ",");
		sb.append(name + ",");
		sb.append(scope + ",");
		sb.append("\"" + types + "\",");
		sb.append("\"" + vicinity + "\",");
		sb.append("\"" + postalAddress + "\",");
		sb.append(lat + ",");
		sb.append(lon);
		return sb.toString();		
	}
	
	public String toJSON(){
		StringBuilder sb = new StringBuilder("\"poi_details\": {");
		sb.append("\"place_id\":\"" + placeId + "\",");
		sb.append("\"name\":\"" + name + "\",");
		sb.append("\"source\":\"" + scope + "\",");
		sb.append("\"types\":\"" + types + "\",");
		sb.append("\"vicinity\":\"" + vicinity + "\",");
		sb.append("\"postal_address\":\"" + postalAddress + "\",");
		sb.append("\"lat\":" + lat + ",");
		sb.append("\"lon\":" + lon);
		sb.append("}");
		return sb.toString();
	}
	
	public int getDistanceFrom(double currentLat, double currentLon){
		double radiusInFeet = 20889108;
		double radiusInMeters = 6367000;
		double result;
		
		// d=acos(sin(lat1)*sin(lat2)+cos(lat1)*cos(lat2)*cos(lon1-lon2))
		
		double a = Math.toRadians(90 - lat);
		double b = Math.toRadians(90 - currentLat);
		double theta = Math.toRadians(currentLon - lon);
		double c = Math.acos((Math.cos(a)* Math.cos(b)+ Math.sin(a) * Math.sin(b) * Math.cos(theta)));
		result = radiusInMeters * c;
		return (int)result;
	}
	
	public boolean isNearBy(double currentLat, double currentLon, int radius){
		int distance = getDistanceFrom(currentLat, currentLon);
		if (distance <= radius)
			return true;
		else 
			return false;
	}
	
	public boolean isType(String desiredTypes){
		return types.contains(desiredTypes);	
	}
	
}
