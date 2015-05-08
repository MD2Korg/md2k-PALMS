package edu.ucsd.cwphs.palms.trip;


import edu.ucsd.cwphs.palms.gps.GPSResultSet;
import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class TRIPdpu {

	private TRIPdpuResultSet resultSet = new TRIPdpuResultSet();
	private TRIPdpuParameters pb = new TRIPdpuParameters();
	private GPSResultSet data = new GPSResultSet();
	private String errors = null;
	private String log = null;
	
	public String setParametersAndDataFromJSON(String json){
		String rc = setParametersFromJSON(json);
		if (rc != null)
			return rc;
		return setDataFromJSON(json);
	}
	
	public String setParametersFromJSON(String json){
		return pb.fromJSON(json);
	}
	
	public boolean setParameters(TRIPdpuParameters parameters){
		if (parameters == null) return false;
		this.pb = parameters;
		return true;
	}
	
	public String setDataFromJSON(String json){
		return this.data.fromJSON(json);
	}
	
	public TRIPdpuResultSet getResultSet(){
		return resultSet;
	}
	
	public String process(){
		errors = null;
		TRIPprocessingR1 r1 = new TRIPprocessingR1();
		EventLogger.logEvent("TRIPdpu - Calculation - " + r1.getVersion());
		EventLogger.logEvent(pb.getVersion());
		EventLogger.logEvent("Parameters = " + pb.prettyPrint());
		
		r1.process(pb, data.getResults());
		resultSet = r1.getResultSet();
		if (errors != null)
			return errors;
		else
			return resultSet.toJSON();
	}
	
	public String getErrors(){
	return errors;
	}
	
	public String getLog(){
	return log;
	}
}