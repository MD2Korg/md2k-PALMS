package edu.ucsd.cwphs.palms.location.passed;

import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

public class LocationPassedDpuParameters {

	private static final String VERSION = "1.3.0   08 June 2016";
	public int buffer = 30;			// default to 30 meters
	public String types = null;		// all types
	public int minDistanceChange = 68;   // (in meters over 1 minute)
										 // average walking speed = 5 kmph
	                                     // 1kmph = 17 meters/minute,  2=34, 3=51, 4=68, 5=85
	public int minDurationAt = 180;
	public int minTimeBetweenPasses = 60;
	public static int LOSsec = 5 * 60;
	public boolean debug = false;
	public boolean detail = false;
	
	
	// return version number
	public String getVersion(){
		return "PALMS Location Passed DPU ParameterBlock - Version " + VERSION;
	}
	
	public String toJSON(){
		StringBuilder sb = new StringBuilder("{\"location_passed_dpu_parameters\":{");
		sb.append("\"buffer\":" + buffer + ",");
		sb.append("\"types\":\"" + types + "\",");
		sb.append("\"debug\":" + debug + ",");
		sb.append("\"detail\":" + detail + ",");
		sb.append("\"min_distance_change\":" + minDistanceChange + ",");
		sb.append("\"min_duration_at\":" + minDurationAt + ",");
		sb.append("\"min_time_between_passes\":" + minTimeBetweenPasses);
		sb.append("}}");
		return sb.toString();
	}
	
	public String prettyPrint(){
		String s = JSONPrettyPrint.print(toJSON());
		return s;
	}
	
	// TODO: Implement code
	public String  fromJSON(String json){
		String error = null;
		return error;
	}
	
}
