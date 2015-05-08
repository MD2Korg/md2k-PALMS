package edu.ucsd.cwphs.palms.test;


import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.GPSdpu;
import edu.ucsd.cwphs.palms.gps.GPSdpuParameters;
import edu.ucsd.cwphs.palms.kml.KMLexport;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

public class TestGPSTrack {

	public static void main(String[] args) {
		EventLogger.logEvent("TestGPSTrack - Test start");
		doTest();
		EventLogger.logEvent("TestGPSTrack - Test end");
	}

	private static void doTest(){
		GPSTrack track = new GPSTrack();
		track.fromGPX("testData/Test1.gpx");
		EventLogger.logEvent("TestGPSdpu - Number of trackpoints:" + track.getSize());
	
		String json = track.toJSON();
		EventLogger.logEvent("JSON = " + json);
		
		GPSTrack track2 = new GPSTrack();
		track2.fromJSON(json);
		EventLogger.logEvent("TestGPSdpu - Number of trackpoints in track2:" + track2.getSize());
		
	}
}
