package edu.ucsd.cwphs.palms.trip;

import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

public class TRIPdpuParameters {

	private static final String VERSION = "0.1.0   22 Dec 2014";
	public boolean includePausePoints = false;
	public boolean includeStationary = false;
	
	
	// return version number
	public String getVersion(){
		return "PALMS Trip Summary ParameterBlock - Version " + VERSION;
	}
	
	
	public String toJSON(){
		StringBuilder sb = new StringBuilder("{\"trip_dpu_parameters\":{");
		sb.append("\"include_stationary\":" + includeStationary + ",");
		sb.append("\"include_pause\":" + includePausePoints + "}}");
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
