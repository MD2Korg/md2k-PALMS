package edu.ucsd.cwphs.palms.util;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Formatter;


/**
 * Timestamps and logs messages and errors to text file, typically on the system running the application.
 * 
 * @author Fredric Raab, UCSD CWPHS, fraab@ucsd.edu
 *
 */
	public class EventLogger {
		private static long warningCounter = 0;
		private static long errorCounter = 0;
		private static long exceptionCounter = 0;
		private static boolean writeToConsole = true;
		private static boolean debug = false;
		private static boolean errorWritingToFile = false;
		
		private static String filename = "logs/EventLogger_";		// production
	
		public static void clearCounters(){
			warningCounter = 0;
			errorCounter = 0;
			exceptionCounter = 0;			
		}
		
		public static void setFileName(String s){
			filename = s;
			logEvent("Logging to " + filename);
		}
		
		public static void setDebug(boolean b){
			debug = b;
		}
		
		public static void setWriteToConsole(boolean b){
			writeToConsole = b;
		}
		
		public static String getCurrentYYYYMMDDStr(){
			Calendar now = Calendar.getInstance();
			int currentYear = now.get(Calendar.YEAR);
			int currentMonth = 1 + now.get(Calendar.MONTH);
			int currentDay = now.get(Calendar.DAY_OF_MONTH);
			Formatter fmt = new Formatter();
			fmt.format("%04d-%02d-%02d", currentYear, currentMonth, currentDay);
			String s = fmt.toString();
			fmt.close();
			return s;
			
		}
		
		public static String getCurrentDateTimeStr(){
			Calendar now = Calendar.getInstance();
			int currentYear = now.get(Calendar.YEAR);
			int currentMonth = 1 + now.get(Calendar.MONTH);
			int currentDay = now.get(Calendar.DAY_OF_MONTH);
			int currentHour = now.get(Calendar.HOUR_OF_DAY);
			int currentMinute = now.get(Calendar.MINUTE);
			int currentSecond = now.get(Calendar.SECOND);

			Formatter fmt = new Formatter();		
			fmt.format("%2d/%02d/%04d %2d:%02d:%02d", currentMonth, currentDay, currentYear, currentHour, currentMinute, currentSecond);
			String s = fmt.toString();
			fmt.close();
			return s;
		}
		
		/**
		 * Appends message msg to file (Constants.LOGFILE).  Creates file if it doesn't exist.
		 * Opens file, writes text, flushs and closes file each time to ensure a message 
		 * will not be lost.
		 * 
		 * @param msg	Text of message
		 */
		public static void logEvent(String msg){
			String fileName = filename +getCurrentYYYYMMDDStr()+".txt";
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
				}
			}
		}

		/**
		 * Logs exceptions
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
		 * @param msg	Text of message
		 */
		public static void logError(String msg){
			errorCounter++;
			logEvent("ERROR: " + msg);
		}
		
		/**
		 * Logs warnings detected by program
		 * @param msg	Text of message
		 */
		public static void logWarning(String msg){
			warningCounter++;
			logEvent("WARNING: " + msg);
		}
		
		
		public static void logDebug(String msg){
			if (debug)
				logEvent(msg);
		}
		
		/**
		 * Displays message on console
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
