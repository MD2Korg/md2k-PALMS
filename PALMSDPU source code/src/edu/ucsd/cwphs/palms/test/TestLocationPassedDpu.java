package edu.ucsd.cwphs.palms.test;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.GPSdpu;
import edu.ucsd.cwphs.palms.gps.GPSdpuParameters;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.kml.KMLexport;
import edu.ucsd.cwphs.palms.kml.KMLgenerator;
import edu.ucsd.cwphs.palms.location.LocationDpu;
import edu.ucsd.cwphs.palms.location.LocationDpuParameters;
import edu.ucsd.cwphs.palms.location.LocationDpuResultSet;
import edu.ucsd.cwphs.palms.location.passed.LocationPassedDpu;
import edu.ucsd.cwphs.palms.location.passed.LocationPassedDpuParameters;
import edu.ucsd.cwphs.palms.location.passed.LocationPassedDpuResult;
import edu.ucsd.cwphs.palms.location.passed.LocationPassedDpuResultSet;
import edu.ucsd.cwphs.palms.trip.TRIPdpu;
import edu.ucsd.cwphs.palms.trip.TRIPdpuParameters;
import edu.ucsd.cwphs.palms.trip.TRIPdpuResultSet;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;
import edu.ucsd.cwphs.palms.util.WriteToFile;

public class TestLocationPassedDpu {
	
	
	private static boolean PALMSprocessing = true;
	// When true, the PALMS DPU is used to pre-process and clean the track file
	// When false, the track is used as-is.
	
	
	private static String fileName = "testData/Test1.gpx";
	// fileName is the path to the GPX or CSV file containing the track to be processed
	
	private static String parmFile = null;
	// parmFile is the path to the JSON file containing the parameters
	// when set to null, the default parameters are used
	
	private static LocationPassedDpu dpu;
	private static GPSTrack track;
	
public static void main(String[] args) {
	
//	fileName = "testData/Columbus PALMS.gpx";
//	fileName = "testData/Chicago.gpx";
//	fileName = "testData/Memphis 2014.gpx";
	
	if (args.length != 0)
		fileName = args[0];
	if (args.length > 1)
		parmFile = args[1];
	
	EventLogger.logEvent("TestLocationPassedDpu - Starting using: " + fileName);
	if (doTest())
		EventLogger.logEvent("TestLocationPassedDpu - Ended with success");
	else
		EventLogger.logEvent("TestLocationPassedDpu - Ended with errors");
}


private static boolean doTest(){

	track = new GPSTrack();
	String fnlc = fileName.toLowerCase();
	
	if (fnlc.contains("gpx"))
		track.fromGPX(fileName);
	else if (fnlc.contains("csv"))
		track.fromCSV(fileName);
	else {
		EventLogger.logError("Unsupported file format - expecting GPX or CSV");
		return false;
	}
	
	EventLogger.logEvent("TestLocationPassedDpu - Number of trackpoints:" + track.getSize());
	if (track.getSize() == 0){
		EventLogger.logError("No track points found -- exiting");
		return false;
	}
	
	// PALMS processing
	
	if (PALMSprocessing){
		GPSdpu dpu= new GPSdpu();
		GPSdpuParameters parameters = new GPSdpuParameters();	// start with default GPSdpu parameters
		parameters.interval = 30;								// override default interval
	
		String json = parameters.toJSON();
		parameters.fromJSON(json);
	
		String pj = parameters.toJSON();
		String tj = track.toJSON();;
	
		pj = pj.substring(0, pj.length()-1);		// remove ending }
		tj = tj.substring(1);						// remove leading {
		json = pj + "," + tj;
	
		dpu.setParametersAndTrackFromJSON(json);
	
		dpu.process();
		track = dpu.getTrack();			// replace track
		EventLogger.logEvent("TestLocationPassedDpu - Number of trackpoints after PALMS processing:" + track.getSize());
	} // end PALMS processing
	
	
	dpu = new LocationPassedDpu();
	LocationPassedDpuParameters parameters = new LocationPassedDpuParameters();
	parameters.buffer = 30;
	parameters.types = "tobacco";
	
	if (parmFile != null){
		String s = readParameterFile(parmFile);
		if (s == null){
			EventLogger.logWarning("Error reading parameter file - using defaults");
		}
		parameters.fromJSON(s);
	}
	
	dpu.setParameters(parameters);
	String json = track.toJSON();
	dpu.setDataFromJSON(json);
	
	String jsonResult = dpu.process();
	EventLogger.logEvent("JSON = " + jsonResult);
	EventLogger.logEvent(JSONPrettyPrint.print(jsonResult));
	
	EventLogger.logEvent("Writing Location Passed dpu results to json file...");
	WriteToFile.newFile("logs/Location Passed Dpu Test Results.json");
	WriteToFile.write("logs/Location Passed Dpu Test Results.json", jsonResult);
	
	EventLogger.logEvent("Writing Location dpu results to kml...");
	writeKML("logs/Location Passed Dpu Test Results.kml");

	EventLogger.logEvent("Writing Location dpu results to csv file...");
	WriteToFile.newFile("logs/Location Passed Dpu Test Results.csv");
	WriteToFile.write("logs/Location Passed Dpu Test Results.csv", LocationPassedDpuResult.CSVheader()+ "\n");
	WriteToFile.write("logs/Location Passed Dpu Test Results.csv", dpu.getResultSet().toCSV());
	
	return true;
}	

	private static String readParameterFile(String fileName){
		try {
		  byte[] encoded = Files.readAllBytes(Paths.get(fileName));
		  return new String(encoded, Charset.defaultCharset());
		}
		catch (Exception ex){
			EventLogger.logException("readParameterFile - ", ex);
			return null;
		}
	}

	private static void writeKML(String outputFileName){
		int tpCount = 0;
		double prevLat = -999.0, prevLon = -999.0;
		try {
		PrintWriter printWriter = new PrintWriter(outputFileName);
		EventLogger eventLogger = new EventLogger();

		// pass reference to KML generator
		KMLgenerator gen = new KMLgenerator(eventLogger, printWriter);
		gen.initKML("Locations Passed");
		gen.defineDefaultStyles();
		
		gen.addFolder("Locations", false);
		LocationPassedDpuResultSet rs = dpu.getResultSet();
		for (int i=0; i<rs.size(); i++){
			String s = rs.get(i).toKML();
			printWriter.append(s);
		}
		
		gen.closeFolder();
		gen.addFolder("Tracks", false);
		track.toKML(gen);
		gen.closeFolder();
		
		/*
		gen.addFolder("TrackPoints", false);
		for (int i=0;i<track.getSize(); i++){
			TrackPoint tp = track.getTrackPoint(i);
			double lat = tp.getLat();
			double lon = tp.getLon();
			// Don't add duplicate track coords
			if (lat == prevLat && lon == prevLon) 
				continue;
			else {
				tp.desc = tp.getDateTimeStr();
				gen.addPlacemark(tp, KMLgenerator.GRAY_POINT, true, Integer.toString(i));
				prevLat = lat;
				prevLon = lon;
				if (tpCount++ > 5000) break;	// exit after 5000 points, otherwise GE runs too slow
			}
		} // end for
		gen.closeFolder();
				*/
		
		gen.closeKML();
		}
		catch (Exception ex){
			EventLogger.logException("writeKML - ", ex);
		}
	}
	
}