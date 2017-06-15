package edu.ucsd.cwphs.palms.location.passed;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class LocationPassedDpu {

	private LocationPassedDpuResultSet resultSet = new LocationPassedDpuResultSet();
	private LocationPassedDpuParameters pb = new LocationPassedDpuParameters();
	private GPSTrack tracks = new GPSTrack();
	private String errors = null;
	private String log = null;
	
	public String setParametersAndTrackFromJSON(String json){
		String rc = setParametersFromJSON(json);
		if (rc != null)
			return rc;
		return setDataFromJSON(json);
	}
	
	public String setParametersFromJSON(String json){
		return pb.fromJSON(json);
	}
	
	public boolean setParameters(LocationPassedDpuParameters parameters){
		if (parameters == null) return false;
		this.pb = parameters;
		return true;
	}
	
	public String setDataFromJSON(String json){
		return this.tracks.fromJSON(json);
	}
	
	public LocationPassedDpuResultSet getResultSet(){
		return resultSet;
	}
	
	public String process(){
		errors = null;
// 		LocationPassedProcessingR1 r1 = new LocationPassedProcessingR1();
		LocationPassedProcessingR2 r1 = new LocationPassedProcessingR2();			// NOTE
		EventLogger.logEvent("LocationDpu - Calculation - " + r1.getVersion());
		EventLogger.logEvent(pb.getVersion());
		EventLogger.logEvent("Parameters = " + pb.prettyPrint());
		
		errors = r1.process(pb, tracks);
		if (errors != null)
			return errors;
		else {
			resultSet = r1.getResultSet();
			return resultSet.toJSON();
		}
	}
	
	public String getErrors(){
	return errors;
	}
	
	public String getLog(){
	return log;
	}
}