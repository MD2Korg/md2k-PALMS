package edu.ucsd.cwphs.palms.test;


import java.io.PrintWriter;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.GPSdpu;
import edu.ucsd.cwphs.palms.gps.GPSdpuParameters;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.kml.KMLexport;
import edu.ucsd.cwphs.palms.kml.KMLgenerator;
import edu.ucsd.cwphs.palms.location.passed.LocationPassedDpuResultSet;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

public class TestGPSTrack {

	static String fileName = "testData/Test1.gpx";
	
	public static void main(String[] args) {
		EventLogger.logEvent("TestGPSTrack - Test start");
		fileName = "testData/Columbus PALMS.gpx";
		
		doTest();
		EventLogger.logEvent("TestGPSTrack - Test end");
	}

	private static void doTest(){
		GPSTrack track = new GPSTrack();
		track.fromGPX(fileName);
		EventLogger.logEvent("TestGPSdpu - Number of trackpoints:" + track.getSize());
		
		writeKML(track);
		/*

		String json = track.toJSON();
		EventLogger.logEvent("JSON = " + json);

		GPSTrack track2 = new GPSTrack();
		track2.fromJSON(json);
		EventLogger.logEvent("TestGPSdpu - Number of trackpoints in track2:" + track2.getSize());

		GPSTrack track3 = new GPSTrack();
		track3.fromDatakitCSV("testData/Datakit/LOCATION 2016-09-20.csv", 100);
		EventLogger.logEvent("TestGPSdpu - Number of trackpoints in track3:" + track3.getSize());
		writeKML(track3);
		
		EventLogger.logEvent("TestGPSdpu - WriteGPX file for track3");
		track3.toGPX("logs/Track3.gpx");
		*/

	}

	private static void writeKML(GPSTrack track){
		int tpCount = 0;
		double prevLat = -999.0, prevLon = -999.0;
		try {	
			PrintWriter printWriter = new PrintWriter("logs/TestGPSTrack.kml");
			EventLogger eventLogger = new EventLogger();

			// pass reference to KML generator
			KMLgenerator gen = new KMLgenerator(eventLogger, printWriter);
			gen.initKML("From Phone");
			gen.defineDefaultStyles();

			gen.addFolder("Tracks", false);
			track.toKML(gen);
			gen.closeFolder();

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
			gen.closeKML();
		}
		catch (Exception ex){
			EventLogger.logException("writeKML - ", ex);
		}
	}


}
