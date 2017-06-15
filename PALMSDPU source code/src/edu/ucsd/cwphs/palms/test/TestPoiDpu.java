package edu.ucsd.cwphs.palms.test;

import java.util.ArrayList;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.poi.GooglePlaces;
import edu.ucsd.cwphs.palms.poi.POI;
import edu.ucsd.cwphs.palms.poi.POIdpu;
import edu.ucsd.cwphs.palms.poi.POIstats;
import edu.ucsd.cwphs.palms.poi.POIstatsList;
import edu.ucsd.cwphs.palms.poi.YelpPlaces;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.WriteToFile;


public class TestPoiDpu {
	private static GooglePlaces googlePlaces = new GooglePlaces(null);
	private static YelpPlaces yelpPlaces = new YelpPlaces();
	
public static void main(String[] args) {
	EventLogger.setFileName("logs\\TestPoiDpu_");
	EventLogger.logEvent("TestPoiDpu - Test start");
	String fileName = "testData/Test1.gpx";
	fileName = "testData/Memphis 2014-10-20.gpx";
	fileName = "testData/Columbus PALMS.gpx";
	
	doFileTest(fileName, 50, "");
	
	
//	doJSONTest(32.8692502, -117.2425893, 800, "food");
//	doJSONTest(32.88263, -117.23523, 300, "food");
	
	EventLogger.logEvent("TestPoiDpu - Google usage = " + googlePlaces.getApiUsageCount());
	EventLogger.logEvent("TestPoiDpu - Yelp usage = " + yelpPlaces.getApiUsageCount());
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

	POIstatsList psl = new POIstatsList();
	
	GPSTrack track = new GPSTrack();
	track.fromGPX(gpxFile);
	EventLogger.logEvent("TestTripDpu - Number of trackpoints:" + track.getSize());
	
	POIdpu dpu= new POIdpu();
	
	int index = 0;
	
	int max = track.getSize();
	
	max = 2000;			// shorten for debugging
	
	while  (index < max){
		TrackPoint tp = track.getTrackPoint(index);
		ArrayList<POI> poiList = dpu.getPOI(tp.getLat(), tp.getLon(), buffer, types);
		index = index + 10;		// use every 10 points
		EventLogger.logEvent("Timestamp:" + tp.getDateTimeStr() + "  Lat:"+ tp.getLat() + " Lon:" + tp.getLon() + " Number of POIs:" + poiList.size());
			for (int i = 0; i < poiList.size(); i++){
				POI poi = poiList.get(i);
					int distance = poi.getDistanceFrom(tp.getLat(), tp.getLon());
					EventLogger.logEvent(index + " - " + poi.getName() + " - Types:" + poi.getTypes() + " - " +
							poi.getMd2kTypes() + " - Distance:" + distance);
					psl.addTimePassed(poi);
			} // end for
	} // end while
	
	// output POI Stats to File
	WriteToFile.newFile("logs/POI DPU Test Results.csv");
	WriteToFile.write("logs/POI DPU Test Results.csv", psl.get(0).CSVHeader());
	
	for (int i = 0; i < psl.size(); i++){
		POIstats ps = psl.get(i);
		if (ps != null){
			WriteToFile.write("logs/POI DPU Test Results.csv", ps.toCSV());
			POI poi = ps.getPOI();
			EventLogger.logEvent(poi.getName() + " " + poi.getLat() + ", " + poi.getLon() + " " + 
					poi.getScope() + "- Passed:" + ps.getNumberOfPasses());
		}
	}
}
}