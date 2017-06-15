package edu.ucsd.cwphs.palms.test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.gps.WayPoint;
import edu.ucsd.cwphs.palms.kml.KMLgenerator;
import edu.ucsd.cwphs.palms.poi.DAOpoi;
import edu.ucsd.cwphs.palms.poi.POI;
import edu.ucsd.cwphs.palms.poi.POIdpu;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class TestDAOpoi {

	public static void main(String[] args) {
		EventLogger.setFileName("logs\\TestDAOpio ");
		EventLogger.logEvent("TestDAOpio - Test start");
//		testDeleteScope(POI.SCOPEGOOGLE);
//		testDeleteScope(POI.SCOPEYELP);
//		testDeleteScope(POI.SCOPELOCAL);
//		testInsert();
//		testFind();
//		distanceTest("bar | health");
		String type = "";
		mapAllPOIs(type);
		EventLogger.logEvent("TestDAOpio - Test end");
	}

	private static void testInsert(){
		Double lat = 32.882574;				// Calit2
		Double lon = -117.234729;
		DAOpoi dao = new DAOpoi();
		POI poi = new POI(null, "Calit2", POI.SCOPELOCAL, "Office", "Office", "UCSD", "Engineer Lane, La Jolla, CA 92093", lat, lon);
		dao.insertPOI(poi);	
		dao.closeDB();
	}
	
	private static void testDeleteScope(String scope){
		DAOpoi dao = new DAOpoi();
		dao.deleteScope(scope);
		dao.closeDB();
	}
	
	
	private static void testFind(){
		Double lat = 32.882574;				// Calit2
		Double lon = -117.234729;
		int radius = 100;					// in meters
		String types = null;				// all types 
		
		DAOpoi dao = new DAOpoi();
		ArrayList<POI> results = null;
		
		results = dao.getNearBy(lat, lon, radius, types);
		
		EventLogger.logEvent("Number of results returned:" + results.size());
		
		if (results.size() == 0) {
			
		}
		dao.closeDB();
	}
	
	
public static void getAllPOIs(String types){
	DAOpoi dao = new DAOpoi();
	ArrayList<POI> poiList = dao.getAllPOIs(types);
	EventLogger.logEvent("Types:" + types + " Number of POIs:" + poiList.size());
	for (int i = 0; i < poiList.size(); i++){
		POI poi = poiList.get(i);
			EventLogger.logEvent(poi.getName() + " - Type:" + poi.getTypes());
	} // end for
}

public static void mapAllPOIs(String types) {
	try {
	PrintWriter printWriter = new PrintWriter("logs/" + types + " POIs.kml");
	EventLogger eventLogger = new EventLogger();

	DAOpoi dao = new DAOpoi();
	ArrayList<POI> poiList = dao.getAllPOIs(types);
	EventLogger.logEvent("Types:" + types + " Number of POIs:" + poiList.size());
	KMLgenerator kml = new KMLgenerator(eventLogger, printWriter);
	kml.initKML(types+" POIs");
	kml.writeStylePoint("green", "CC00FF00", .45);
	
	for (int i = 0; i < poiList.size(); i++){
		POI poi = poiList.get(i);
			EventLogger.logEvent(poi.getName() + " - Type:" + poi.getTypes());
			WayPoint wp = new WayPoint(poi.getLat(), poi.getLon(), 0, poi.getName());
			wp.desc = poi.getScope() + ": " + poi.getTypes();
			kml.addPlacemark(wp, "green", true, poi.getName());			
	} // end for
	kml.closeKML();
	}
	catch (Exception ex){
		EventLogger.logException("mapAllPOIs - ", ex);
	}
}


private static void distanceTest(String types){
	DAOpoi dao = new DAOpoi();
	int buffer = 300;			// 30 meters

	GPSTrack track = new GPSTrack();
	track.fromGPX("testData/Test1.gpx");
	EventLogger.logEvent("Number of trackpoints:" + track.getSize());
	
	
	int index = 0;
	
	while  (index<track.getSize()){
		TrackPoint tp = track.getTrackPoint(index);
		ArrayList<POI> poiList = dao.findPOI(tp.getLat(), tp.getLon(), buffer, types);
		index = index + 1;		// use every 10 points
		EventLogger.logEvent("Timestamp:" + tp.getDateTimeStr() + "  Lat:"+ tp.getLat() + " Lon:" + tp.getLon() + " Number of POIs:" + poiList.size());
			for (int i = 0; i < poiList.size(); i++){
				POI poi = poiList.get(i);
					int distance = poi.getDistanceFrom(tp.getLat(), tp.getLon());
					EventLogger.logEvent(poi.getName() + " - Distance:" + distance);
			} // end for
	} // end while
	dao.closeDB();
}
}
