package edu.ucsd.cwphs.palms.location;

import edu.ucsd.cwphs.palms.gps.GPSResultSet;
import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class LocationDpu {

	private LocationDpuResultSet resultSet = new LocationDpuResultSet();
	private LocationDpuParameters pb = new LocationDpuParameters();
	private GPSResultSet data = new GPSResultSet();
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
	
	public boolean setParameters(LocationDpuParameters parameters){
		if (parameters == null) return false;
		this.pb = parameters;
		return true;
	}
	
	public String setDataFromJSON(String json){
		return this.data.fromJSON(json);
	}
	
	public LocationDpuResultSet getResultSet(){
		return resultSet;
	}
	
	public String process(){
		errors = null;
		LocationProcessingR1 r1 = new LocationProcessingR1();
		EventLogger.logEvent("LocationDpu - Calculation - " + r1.getVersion());
		EventLogger.logEvent(pb.getVersion());
		EventLogger.logEvent("Parameters = " + pb.prettyPrint());
		
		errors = r1.process(pb, data.getResults());
		if (errors != null)
			return errors;
		else {
			if (pb.includePOIs)
				r1.addPOIs();
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