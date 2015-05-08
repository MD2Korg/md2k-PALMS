package edu.ucsd.cwphs.palms.test;

import java.util.ArrayList;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.poi.GooglePlaces;
import edu.ucsd.cwphs.palms.poi.POI;
import edu.ucsd.cwphs.palms.poi.POIdpu;
import edu.ucsd.cwphs.palms.util.EventLogger;


public class TestPoiDpu {
	private static GooglePlaces googlePlaces = new GooglePlaces(null);
	
public static void main(String[] args) {
	EventLogger.setFileName("logs\\TestPoiDpu_");
	EventLogger.logEvent("TestPoiDpu - Test start");
	String fileName = "testData/Test1.gpx";
	fileName = "testData/Death Valley 2014-11-17.gpx";
//	doFileTest(fileName, 300, "");
//	doJSONTest(32.8692502, -117.2425893, 800, "food");
	doJSONTest(32.88263, -117.23523, 300, "food");
	
	EventLogger.logEvent("TestPoiDpu - Google usage = " + googlePlaces.getApiUsageCount());
	EventLogger.logEvent("TestPoiDpu - Test end");
}



private static void doJSONTest(double lat, double lon, int buffer, String types){
	POIdpu dpu= new POIdpu();
	String json = buildJSON(lat, lon, buffer, types);
	dpu.setParametersFromJSON(json);
	dpu.process();
	EventLogger.logEvent("Results = " + dpu.getResultSetJSON());
}


private static String buildJSON(double lat, double lon, int buffer, String types){
	if (types == null)
		types = "";
	
	StringBuilder sb = new StringBuilder("{");
	sb.append("\"types\":\"" + types + "\",");
	sb.append("\"buffer\":\"" + buffer + "\",");
	sb.append("\"lat\":\"" + lat + "\",");
	sb.append("\"lon\":\"" + lon + "\"");
	sb.append("}");
	return sb.toString();
}

private static void doFileTest(String gpxFile, int buffer, String types){

	GPSTrack track = new GPSTrack();
	track.fromGPX(gpxFile);
	EventLogger.logEvent("TestTripDpu - Number of trackpoints:" + track.getSize());
	
	POIdpu dpu= new POIdpu();
	
	int index = 0;
	
	while  (index<track.getSize()){
		TrackPoint tp = track.getTrackPoint(index);
		ArrayList<POI> poiList = dpu.getPOI(tp.getLat(), tp.getLon(), buffer, types);
		index = index + 10;		// use every 10 points
		EventLogger.logEvent("Timestamp:" + tp.getDateTimeStr() + "  Lat:"+ tp.getLat() + " Lon:" + tp.getLon() + " Number of POIs:" + poiList.size());
			for (int i = 0; i < poiList.size(); i++){
				POI poi = poiList.get(i);
					int distance = poi.getDistanceFrom(tp.getLat(), tp.getLon());
					EventLogger.logEvent(poi.getName() + " - Distance:" + distance);
			} // end for
	} // end while
}
}