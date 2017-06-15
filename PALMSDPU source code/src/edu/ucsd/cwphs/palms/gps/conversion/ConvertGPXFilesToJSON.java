package edu.ucsd.cwphs.palms.gps.conversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.util.*;

public class ConvertGPXFilesToJSON {


	public static void main(String[] args) {
		InputStream inputStream = null;
		File inputFile = null;
		String folder = "";
		String infileName = "";
		String outfileName = "";
		EventLogger.setFileName("logs\\ConvertGPXFilesToJSON ");
		EventLogger.logEvent("ConvertGPXFilesToJSON -- Starting program");
		folder = getFolder();
		if (folder == null){
			EventLogger.logWarning("Exiting - folder not specified:" + folder);
			return;
		}

		// get list of files
		folder = ".\\testdata\\" + folder;
		File dir = new File(folder);

		if (!dir.exists()){
			EventLogger.logError("Exiting - Folder not found:" + folder);
			return;
		}

		File[] filesList = dir.listFiles();
		if (filesList.length == 0){
			EventLogger.logWarning("Exiting - No files found in folder:" + folder);
			return;
		}

		EventLogger.logEvent("Folder = " + folder);

		// process files

		for (File file : filesList){
			infileName = file.getAbsolutePath();
			String name = infileName.toLowerCase();
			if (!name.contains(".gpx"))
				continue;
			EventLogger.logEvent("Processing file:" + infileName);
			
			// create Track from GPS
			
			GPSTrack track = new GPSTrack();
			track.fromGPX(infileName);
			String json = track.toJSON();
			
			// create and write output file
			
			int i = infileName.lastIndexOf(".");		// find last .	
			outfileName = infileName.substring(0,i) + ".json";
			try {
				FileWriter out = new FileWriter(outfileName, true);
				out.write(json, 0, json.length());
				out.flush();
				out.close();
				}
			catch (Exception ex){
				EventLogger.logException("Error writing output file", ex);
				}	
			
		} // end for
		EventLogger.logEvent("ConvertGPXFilesToJSON -- Program finished");
	}

// Get Input folder name
// Read all files in folder
// For each GPX file in folder
//		Create Track from GPX
//		Save as JSON with same file name


private static String getFolder(){
	String out = null;
	BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
	System.out.println("Enter folder contain .xls files:");
		try {
			out = buffer.readLine();
		}
		catch (Exception ex){
		}
	return out;
}
		}
			