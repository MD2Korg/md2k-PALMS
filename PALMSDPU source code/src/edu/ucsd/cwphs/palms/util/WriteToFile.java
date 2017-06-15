package edu.ucsd.cwphs.palms.util;

import java.io.FileWriter;

public class WriteToFile {
	
	public static boolean newFile(String fileName){
		boolean rc = true;
		try {
			FileWriter log = new FileWriter(fileName, false);
			log.close();
			}
		catch (Exception ex){
		EventLogger.logException("WriteToFile - Error creating file "+ fileName + "\n", ex);
		rc = false;
		}	
		return rc;
	}

	public static boolean write(String fileName, String output){
		boolean rc = true;
		try {
			FileWriter log = new FileWriter(fileName, true);
			log.write(output, 0, output.length());
			log.flush();
			log.close();
			}
		catch (Exception ex){
		EventLogger.logException("WriteToFile - Error writing to file "+ fileName + "\n", ex);
		rc = false;
		}
		return rc;
	}	
}
