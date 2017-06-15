package edu.ucsd.cwphs.palms.gps;

import org.json.JSONObject;

import edu.ucsd.cwphs.palms.util.EventLogger;

public class GPSdpu {

	private GPSdpuParameters pb = new GPSdpuParameters();
	private GPSTrack track = new GPSTrack();
	private String errors = null;
	private String log = null;
	
	/*
	public String setParametersAndTrackFromJSON(String json){
		JSONObject obj = new JSONObject(json);
		
		String rc = pb.fromJSONObject(obj);
		if (rc != null)
			return rc;
		return  track.fromJSONObject(obj);
	}
	*/
	
	public String setParametersAndTrackFromJSON(String json){
		
		String rc = pb.fromJSON(json);
		if (rc != null)
			return rc;
		return  track.fromJSON(json);
	}
	
	public String setParametersFromJSON(String json){
		return pb.fromJSON(json);
	}
	
	public boolean setParameters(GPSdpuParameters parameters){
		if (parameters == null) return false;
		this.pb = parameters;
		return true;
	}
	
	public String setTrackFromJSON(String json){
		return this.track.fromJSON(json);
	}
	
	public boolean setTrack(GPSTrack track){
		if (track == null) return false;
		this.track = track;
		return true;
	}
	
	public GPSTrack getTrack(){
		return this.track;					// used to return processed track
	}
	
	public GPSResultSet getResultSet(){
		return new GPSResultSet(track);
	}
	
	public String process(){
		errors = null;
		GPSprocessingR41 r41 = new GPSprocessingR41();
		EventLogger.logEvent("GPSdpu - Calculation - " + r41.getVersion());
		EventLogger.logEvent(pb.getVersion());
		EventLogger.logEvent("Parameters = " + pb.prettyPrint());
		
		r41.process(pb, track.getTrackPoints());
		r41.alignToInterval(pb, track.getTrackPoints(), pb.interval);
		if (errors != null)
			return errors;
		else {
			return track.resultsToJSON();
		}
	}
	
	public String getErrors(){
	return errors;
	}
	
	public String getLog(){
	return log;
	}
	
	
}
