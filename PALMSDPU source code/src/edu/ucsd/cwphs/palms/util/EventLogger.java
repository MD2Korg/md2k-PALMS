package edu.ucsd.cwphs.palms.util;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Formatter;

/**
 * 
 * @author Fredric Raab, UCSD CWPHS, fraab@ucsd.edu
 * @version 1.1.0	
 * @since 2015-05-28
 * 
 * Timestamps and logs messages and errors to text file, 
 * typically on the system running the application.
 * 
 * History:
 * 		2015-03-31	1.0.0	Initial published release
 * 		2015-05-28	1.1.0	Added getCounters(), streamlined date/time formating, added javadoc
 *
 */
	public class EventLogger {
		private static long warningCounter = 0;
		private static long errorCounter = 0;
		private static long exceptionCounter = 0;
		private static boolean writeToConsole = true;
		private static boolean debug = false;
		private static boolean errorWritingToFile = false;
		
		private static String filename = "logs/EventLogger_";		// default file name
	
		/**
		 * Sets counters to zero
		 */
		public static void clearCounters(){
			warningCounter = 0;
			errorCounter = 0;
			exceptionCounter = 0;			
		}
		
		/**
		 * 
		 * @return String containing counters
		 */
		public static String getCounters(){
			return "Exceptions:" + exceptionCounter + "  Errors:" + errorCounter + "  Warnings:" + warningCounter;
		}
		
		/**
		 * sets the directory and file name of the logging file
		 * 
		 * @param path to file
		 */
		public static boolean setFileName(String logFileName){
			filename = logFileName + " - ";
			return logEvent("Logging to " + filename +getCurrentYYYYMMDDStr()+".log");
		}
		
		
		/**
		 * 
		 * @param debugFlag -- when true, log debug messages
		 */
		public static void setDebug(boolean debugFlag){
			debug = debugFlag;
		}
		
		/**
		 * 
		 * @param consoleFlag -- when true, also write messages to console (System.out)
		 */
		public static void setWriteToConsole(boolean consoleFlag){
			writeToConsole = consoleFlag;
		}
		
		/**
		 * 
		 * @return current date as String in the format YYYY-MM-DD
		 */
		public static String getCurrentYYYYMMDDStr(){
			Calendar now = Calendar.getInstance();	
			return String.format("%1$tY-%1$tm-%1$td", now);
		}
		
		/**
		 * 
		 * @return current date & time as String in the format YYYY-MM-DD HH:MM:SS
		 */
		public static String getCurrentDateTimeStr(){
			Calendar now = Calendar.getInstance();			
			return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", now);
		}
		
		/**
		 * Appends message msg to log file.  Creates file if it doesn't exist.
		 * Opens file, writes text, flushes and closes file each time to ensure a message 
		 * will not be lost.
		 * 
		 * @param msg	Text of message
		 */
		public static boolean logEvent(String msg){
			boolean rc = true;
			String fileName = filename +getCurrentYYYYMMDDStr()+".log";
			String logStr = getCurrentDateTimeStr() + " " + msg + "\n";
			
			debug("LOG: "+ logStr);
			try {
				FileWriter log = new FileWriter(fileName, true);
				log.write(logStr, 0, logStr.length());
				log.flush();
				log.close();
				}
			catch (Exception ex){
				if (!errorWritingToFile){
					debug("ERROR Writing to Log file "+ fileName + "- " + ex.toString() + "\n");
					errorWritingToFile = true;
					rc = false;
				}
			}
			return rc;
		}

		/**
		 * Logs exceptions
		 * 
		 * @param msg	Text of message
		 * @param ex	Exception to be logged
		 */
		public static void logException(String msg, Exception ex){
			exceptionCounter++;
			errorCounter--;					// logError increments errorCounter
			logEvent("EXCEPTION: " + msg + " - " + ex.toString());
			ex.printStackTrace();			// only prints locally - not logged
		}
		
		/**
		 * Logs errors detected by program
		 * 
		 * @param msg	Text of message
		 */
		public static void logError(String msg){
			errorCounter++;
			logEvent("ERROR: " + msg);
		}
		
		/**
		 * Logs warnings detected by program
		 * 
		 * @param msg	Text of message
		 */
		public static void logWarning(String msg){
			warningCounter++;
			logEvent("WARNING: " + msg);
		}
		
		
		/**
		 * Logs debug messages when debug flag is true
		 * 
		 * @param msg	Text of message
		 */
		public static void logDebug(String msg){
			if (debug)
				logEvent(msg);
		}
		
		/**
		 * Displays message on console when writeToConsole is true
		 * 
		 * @param s	String to be displayed
		 */
		public static void debug(String s){
		
			if (writeToConsole)
				System.out.print(s);			// write to console
		}
		
		/**
		 * Clears log file by deleting the file
		 *
		 */
		public static void clearLog() {
			File log = new File(filename);
			log.delete();
		}
}
