package edu.ucsd.cwphs.palms.gps;

public class Location extends WayPoint {
	private int range = 0;					// distance in meters to capture fixes
	private int buffer = 0;					// distance in meters to consider buffer
	private int references = 0;
	private int priority = 0;
	private int timeAtLocation = 0;			// in seconds
	private String firstArrival = UNKNOWN;
	private String lastDeparture = UNKNOWN;
	
	public static final String UNKNOWN = "unknown"; 
	
	public Location (String name, int priority, double lat, double lon, double ele, int range, int buffer){
		this.name = name;
		this.priority = priority;
		this.lat = lat;
		this.lon = lon;
		this.ele = ele;
		this.range = range;
		this.buffer = buffer;
	}
	
	public Location (String name, String lat, String lon, String elevation){
		this.name = name;
		this.lat = Double.parseDouble(lat);
		this.lon = Double.parseDouble(lon);
		this.ele = Double.parseDouble(elevation);
	}	
	
	public String toString(){
		String s = "Name:" + name + " Lat:" + lat + " Lon:" + lon + " Arv:" + firstArrival + " Dep:" + lastDeparture + "\n";
		return s;
	}
	
	public void setPriority(int p){
		priority = p;
	}
	
	public int getPriority(){
		return priority;
	}
	
	public void setRange(int r){
		range =  r;
	}
	
	public void setBuffer(int b){
		buffer = b;
	}
	
	public void setReferences(int r){
		references = r;
	}
	
	public int getRange(){
		return range;
	}
	
	public int getBuffer(){
		return buffer;
	}
	
	public int getReferences(){
		return references;
	}
	
	public long getTimeAtLocation(){
		return timeAtLocation;
	}
	
	public void incReferences(){
		references++;
	}
	
	public void addTimeAtLocation(int seconds){
		timeAtLocation = timeAtLocation + seconds;
	}
	
	public void setFirstArrival(String s){
		firstArrival = s;
	}
	
	public String getFirstArrival(){
		return firstArrival;
	}
	
	public void setLastDeparture(String s){
		lastDeparture = s;
	}
	
	public String getLastDeparture(){
		return lastDeparture;
	}
	
	public void zero(){
		references = 0;
		priority = 0;
		timeAtLocation = 0;
		firstArrival = UNKNOWN;
		lastDeparture = UNKNOWN;
	}
}

