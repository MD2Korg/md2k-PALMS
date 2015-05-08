package edu.ucsd.cwphs.palms.util;

import java.io.FileWriter;

public class WriteToFile {

	public static void write(String fileName, String output){
		try {
			FileWriter log = new FileWriter(fileName, true);
			log.write(output, 0, output.length());
			log.flush();
			log.close();
			}
		catch (Exception ex){
		EventLogger.logException("WriteToFile - Error writing to file "+ fileName + "\n", ex);
		}
	}	
}
