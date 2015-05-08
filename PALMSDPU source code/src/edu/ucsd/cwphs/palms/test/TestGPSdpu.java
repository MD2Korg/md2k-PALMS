package edu.ucsd.cwphs.palms.test;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.GPSdpu;
import edu.ucsd.cwphs.palms.gps.GPSdpuParameters;
import edu.ucsd.cwphs.palms.kml.KMLexport;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

public class TestGPSdpu {

	public static void main(String[] args) {
		EventLogger.logEvent("TestGPSdpu - Test start");
		doTest();
		EventLogger.logEvent("TestGPSdpu - Test end");
	}

	private static void doTest(){
		GPSTrack track = new GPSTrack();
		track.fromGPX("testData/Test1.gpx");
		EventLogger.logEvent("TestGPSdpu - Number of trackpoints:" + track.getSize());
		
		GPSdpu dpu= new GPSdpu();
		GPSdpuParameters parameters = new GPSdpuParameters();
		parameters.interval = 120;
		
		String json = parameters.toJSON();
		parameters.fromJSON(json);
		
		String pj = parameters.toJSON();
		String tj = track.toJSON();;
		
		pj = pj.substring(0, pj.length()-1);		// remove ending }
		tj = tj.substring(1);						// remove leading {
		json = pj + "," + tj;
		
		dpu.setParametersAndTrackFromJSON(json);
		
		String JSONresults = dpu.process();
		
		String s = JSONPrettyPrint.print(JSONresults);
		EventLogger.logEvent("Results = "+ s);
		
		/*
		GPSTrack resultTrack = new GPSTrack();
		resultTrack.fromJSON(JSONresults);
		
		try {
		KMLexport kml = new KMLexport("logs/test.kml");
		kml.exportTrack(resultTrack);
		}
		catch (Exception ex){
			EventLogger.logException("Error creating KML file", ex);
		}	
		*/
		
	}
}
