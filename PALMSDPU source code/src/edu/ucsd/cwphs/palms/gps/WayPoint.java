package edu.ucsd.cwphs.palms.gps;

public class WayPoint {
	protected double lat = UNKNOWNCOORDINATES;
	protected double lon = UNKNOWNCOORDINATES;
	protected double ele = UNKNOWNELEVATION;
	public String name = "wp";
	public String desc = "waypoint";
	
	public static double UNKNOWNCOORDINATES = -180.0;
	public static double UNKNOWNELEVATION = -999.0;
	
	public WayPoint(double latitude, double longitude, double elevation, String name){
		lat = latitude;
		lon = longitude;
		ele = elevation;
		this.name = name;
	}
	
	public WayPoint(String latitude, String longitude, String elevation, String name){
		try {
			lat = Double.parseDouble(latitude);
			lon = Double.parseDouble(longitude);
			ele = Double.parseDouble(elevation);
			this.name = name;
		}
		catch (Exception ex){
			System.out.print("WayPoint Constructor error: " + latitude + " " + longitude + " " + elevation);
		}
	}
	
	public WayPoint(TrackPoint tp){
		lat = tp.getLat();
		lon = tp.getLon();
		ele = tp.getEle();
		name = tp.name;
	}
	
	public WayPoint(){	
	}
		
	public double getLat(){
		return lat;
	}
	public String getLatStr(){
		return Double.toString(lat);
	}
	
	public void setLat(double l){
		lat = l;
	}
	
	public void setLat(String s){
		lat = Double.parseDouble(s);
	}
	
	public double getLon(){
		return lon;
	}
	
	public String getLonStr(){
		return Double.toString(lon);
	}
	
	public void setLon(double l){
		lon = l;
	}
	public void setLon(String s){
		lon = Double.parseDouble(s);
	}
	
	public double getEle(){
		return ele;
	}
	
	public String getEleStr(){
		return Double.toString(ele);
	}
	
	public void setEle(int e){
		ele = e;
	}
	public void setEle(String s){
		ele = (int) Double.parseDouble(s);		// elevation may be expressed as floating point
	}	
	
	public int calDistance(WayPoint wp){
		double radiusInFeet = 20889108;
		double radiusInMeters = 6367000;
		double result;
		
		// d=acos(sin(lat1)*sin(lat2)+cos(lat1)*cos(lat2)*cos(lon1-lon2))
		
		double a = Math.toRadians(90 - lat);
		double b = Math.toRadians(90 - wp.lat);
		double theta = Math.toRadians(wp.lon - lon);
		double c = Math.acos((Math.cos(a)* Math.cos(b)+ Math.sin(a) * Math.sin(b) * Math.cos(theta)));
		result = radiusInMeters * c;
		return (int)result;
	}
	
	// degrees = atan2(sin(delta_long).cos(lat2),cos(lat1).sin(lat2) - sin(lat1).cos(lat2).cos(delta_long) )
	
	public int calBearing(WayPoint wp){
	    double lat1 = Math.toRadians(lat);
	    double long1 = Math.toRadians(lon);
	    double lat2 = Math.toRadians(wp.lat);
	    double long2 = Math.toRadians(wp.lon);

	    double bearingradians = 
	    	        Math.atan2(Math.asin(long2-long1)*Math.cos(lat2),Math.cos(lat1)*Math.sin(lat2) - 
	    		    Math.sin(lat1)*Math.cos(lat2)*Math.cos(long2-long1));
	    
	    double bearingdegrees = Math.toDegrees(bearingradians);
	    if (bearingdegrees == 360)
	    	bearingdegrees = 0;
	    else
	    	if (bearingdegrees < 0)
	    		bearingdegrees = 360 + bearingdegrees;
	    
	    return (int)bearingdegrees;
	}
	
	
	public String getLonLatAltStr(){
		String str = lon + "," + lat +"," + ele;
		return str;
	}
}