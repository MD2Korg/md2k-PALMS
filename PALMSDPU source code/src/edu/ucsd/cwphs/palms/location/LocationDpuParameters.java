package edu.ucsd.cwphs.palms.location;

import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

public class LocationDpuParameters {

	private static final String VERSION = "1.1.0   27 May 2015";
	public boolean includePausePoints = true;
	public boolean includeStationary = false;
	public boolean includePOIs = true;
	
	
	// return version number
	public String getVersion(){
		return "PALMS Location Summary ParameterBlock - Version " + VERSION;
	}
	
	
	public String toJSON(){
		StringBuilder sb = new StringBuilder("{\"location_dpu_parameters\":{");
		sb.append("\"include_stationary\":" + includeStationary + ",");
		sb.append("\"include_pause\":" + includePausePoints + ",");
		sb.append("\"include_pois\":" + includePOIs + "}}");
		return sb.toString();
	}
	
	public String prettyPrint(){
		String s = JSONPrettyPrint.print(toJSON());
		return s;
	}
	
	
	public String  fromJSON(String json){
		String error = null;
		return error;
	}
	
}
