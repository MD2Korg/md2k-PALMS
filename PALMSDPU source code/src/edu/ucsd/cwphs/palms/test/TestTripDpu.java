package edu.ucsd.cwphs.palms.test;

import edu.ucsd.cwphs.palms.gps.GPSResultSet;
import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.GPSdpu;
import edu.ucsd.cwphs.palms.gps.GPSdpuParameters;
import edu.ucsd.cwphs.palms.kml.KMLexport;
import edu.ucsd.cwphs.palms.trip.TRIPdpu;
import edu.ucsd.cwphs.palms.trip.TRIPdpuParameters;
import edu.ucsd.cwphs.palms.trip.TRIPdpuResultSet;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;
import edu.ucsd.cwphs.palms.util.WriteToFile;

public class TestTripDpu {

public static void main(String[] args) {
	EventLogger.setFileName("logs\\TestTripDPU ");
	EventLogger.logEvent("TestTripDpu - Test start");
	doTest();
	EventLogger.logEvent("TestTripDpu - Test end");
}

private static void doTest(){
	String s = null;

	GPSTrack track = new GPSTrack();
	track.fromGPX("testData/Test1.gpx");
	EventLogger.logEvent("TestTripDpu - Number of trackpoints:" + track.getSize());
	
	GPSdpu dpu= new GPSdpu();
	GPSdpuParameters parameters = new GPSdpuParameters();
	parameters.interval = 60;
	
	String json = parameters.toJSON();
	parameters.fromJSON(json);
	
	dpu.setTrack(track);
	dpu.setParameters(parameters);
	
	json = dpu.process();
		
	TRIPdpu tripDpu = new TRIPdpu();
	TRIPdpuParameters tripParameters = new TRIPdpuParameters();
	tripDpu.setParameters(tripParameters);
	tripDpu.setDataFromJSON(json);
	
	tripDpu.process();
	
	TRIPdpuResultSet rs = tripDpu.getResultSet();
	EventLogger.logEvent("Trip dpu results = " + rs.toCSV());
	WriteToFile.write("logs/Trip Dpu Test Results.csv", rs.toCSV());
	
}	
}