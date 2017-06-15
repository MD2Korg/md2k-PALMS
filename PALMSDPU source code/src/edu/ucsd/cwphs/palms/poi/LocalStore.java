package edu.ucsd.cwphs.palms.poi;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.ucsd.cwphs.palms.util.EventLogger;

public class LocalStore {
	
	private ArrayList<POI> pois = new ArrayList<POI>();		
	
	// ** tomcat6 for beta  tomcat7 for production
	private final String poiFile = "/var/lib/tomcat7/shared/pois.txt";
	
	public LocalStore(){
		refresh();
	}
	
	public void refresh(){
		readPOIs();					// load cache from file
	}
	
	public ArrayList<POI> getPOIs(){
		return pois;
	}
	
	public int getPoisSize(){
		return pois.size();
	}
	

	
	public boolean addPOI(POI p){
		pois.add(p);
		return savePOIs();
	}
	
	public boolean removePOI(String placeId){
		int i = findPlaceIdIndex(placeId);
		if (i == -1)
			return false;
		pois.remove(i);
		return savePOIs();
	}
	
	private int findPlaceIdIndex(String placeId){
		for (int i=0; i< pois.size(); i++){
			POI poi = pois.get(i);
			if (poi.getPlaceId().equalsIgnoreCase(placeId))
				return i;
		}
		return -1;
	}
	
	public ArrayList<POI> findPOIs(double lat, double lon, double buffer, double types){
		ArrayList<POI> results= new ArrayList<POI>();
		return results;
	}
	
	public POI findPOIbyPlaceId(String placeId){
		int index = findPlaceIdIndex(placeId);
		if (index == -1)
			return null;
		else return pois.get(index);
	}
	
	/*
	 * File Read/Write Routines
	 */
	
	// clears cache files (should only be called from testing)
	public boolean clearCacheFile(){
		pois = new ArrayList<POI>();	
		return savePOIs();

	}
	
	private boolean readPOIs(){
		boolean rc = true;
		String line;
		pois = new ArrayList<POI>();	
		try {	
			InputStream fis = new FileInputStream(poiFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while ((line = br.readLine()) != null){
				POI poi = new POI(line);
				if (poi.isValid())
					pois.add(poi);
			}
			br.close();
		}
		catch (Exception ex){
			EventLogger.logException("LocalStore.readPOIs - failed to read - ", ex);
			rc = false;
		}
		return rc;
	}
	
	private boolean savePOIs(){
		boolean rc = true;
		try {
			FileWriter cacheFile = new FileWriter(poiFile);
			for (POI poi : pois ){		// for each assignment
				cacheFile.write(poi.toCSV() + "\r\n");	// write to file
			}
			cacheFile.close();
		} // end try
		catch (Exception ex){
			EventLogger.logException("LocalStore.saveAssignments - failed to save - ", ex);
			rc = false;
		}
		return rc;
	}	
}