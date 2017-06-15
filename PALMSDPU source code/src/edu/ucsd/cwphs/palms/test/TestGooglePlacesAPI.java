package edu.ucsd.cwphs.palms.test;

import edu.ucsd.cwphs.palms.poi.GooglePlaces;
import edu.ucsd.cwphs.palms.poi.POI;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;


public class TestGooglePlacesAPI {
	private static GooglePlaces googlePlaces = new GooglePlaces(null);

	public static void main(String[] args) {
		EventLogger.setFileName("logs/TestGooglePlacesAPI ");
		EventLogger.logEvent("TestGooglePlacesAPI - Test start");
		doTest();
		EventLogger.logEvent("TestGooglePlacesAPI - Test end");

	}
	
	private static void doTest(){
		int radius = 500;					// in meters
		String lat = "32.882574";				// Calit2
		String lon = "-117.234729";
		String foodType = "bar";

		
		ArrayList<POI> results = googlePlaces.getNearBy(lat,lon,radius, foodType);

		if (results.size() == 0)
			EventLogger.logWarning("No POIs returned");
		else
			for (int i=0; i < results.size(); i++){
				EventLogger.logEvent("Place:" + results.get(i).toCSV());
			}		
	}
	

}
