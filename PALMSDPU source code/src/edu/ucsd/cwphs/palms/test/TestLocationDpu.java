package edu.ucsd.cwphs.palms.test;


import java.io.PrintWriter;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.GPSdpu;
import edu.ucsd.cwphs.palms.gps.GPSdpuParameters;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.kml.KMLexport;
import edu.ucsd.cwphs.palms.kml.KMLgenerator;
import edu.ucsd.cwphs.palms.location.LocationDpu;
import edu.ucsd.cwphs.palms.location.LocationDpuParameters;
import edu.ucsd.cwphs.palms.location.LocationDpuResultSet;
import edu.ucsd.cwphs.palms.location.passed.LocationPassedDpuResultSet;
import edu.ucsd.cwphs.palms.trip.TRIPdpu;
import edu.ucsd.cwphs.palms.trip.TRIPdpuParameters;
import edu.ucsd.cwphs.palms.trip.TRIPdpuResultSet;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;
import edu.ucsd.cwphs.palms.util.WriteToFile;

public class TestLocationDpu {
	
	private static String fileName = "testData/Test1.gpx";
	private static GPSTrack track;
	

public static void main(String[] args) {
	EventLogger.logEvent("TestLocationDpu - Test start");
	fileName = "testData/memphis 2014.gpx";
	fileName = "testData/Datakit/LOCATION 2016-07-27.csv";
	
	doTest();
	EventLogger.logEvent("TestLocationDpu - Test end");
}

private static void doTest(){
	String s = null;

	track = new GPSTrack();
//	track.fromGPX(fileName);
	track.fromDatakitCSV(fileName, 100);			// accuracy must be within N meters
	
	EventLogger.logEvent("TestLocationDpu - Number of trackpoints:" + track.getSize());
	
	GPSdpu dpu= new GPSdpu();
	GPSdpuParameters parameters = new GPSdpuParameters();
	parameters.interval = 15;
	
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
	WriteToFile.newFile("logs/Trip Dpu Test Results.csv");
	WriteToFile.write("logs/Trip Dpu Test Results.csv", rs.toCSV());
	
	LocationDpu locationDpu = new LocationDpu();
	LocationDpuParameters locationParameters = new LocationDpuParameters();
	locationDpu.setParameters(locationParameters);
	locationDpu.setDataFromJSON(json);
	
	locationDpu.process();
	
	LocationDpuResultSet lrs = locationDpu.getResultSet();
	
	String jsonResult = lrs.toJSON();
	EventLogger.logEvent(JSONPrettyPrint.print(jsonResult));
	
	EventLogger.logEvent("Writing Location dpu results to file...");
	WriteToFile.newFile("logs/Location Dpu Test Results.csv");
	WriteToFile.write("logs/Location Dpu Test Results.csv", lrs.toCSV());
	
	EventLogger.logEvent("Writing Location dpu results to kml...");
	writeKML("logs/Location Dpu Test Results.kml", lrs);
	
}	

private static void writeKML(String outputFileName, LocationDpuResultSet lrs){
	int tpCount = 0;
	double prevLat = -999.0, prevLon = -999.0;
	try {
	PrintWriter printWriter = new PrintWriter(outputFileName);
	EventLogger eventLogger = new EventLogger();

	// pass reference to KML generator
	KMLgenerator gen = new KMLgenerator(eventLogger, printWriter);
	gen.initKML("Locations");
	gen.defineDefaultStyles();
	
	gen.addFolder("Locations", false);
	for (int i=0; i<lrs.getSize(); i++){
		String s = lrs.get(i).toKML();
		printWriter.append(s);
	}
	
	gen.closeFolder();
	gen.addFolder("Tracks", false);
	track.toKML(gen);
	gen.closeFolder();
	gen.closeKML();
	}
	catch (Exception ex){
		EventLogger.logException("writeKML - ", ex);
	}
}
}