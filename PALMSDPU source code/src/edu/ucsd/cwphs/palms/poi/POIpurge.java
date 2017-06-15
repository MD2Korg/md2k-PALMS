package edu.ucsd.cwphs.palms.poi;

import java.util.Date;

import edu.ucsd.cwphs.palms.util.EventLogger;

public class POIpurge {

	public static void main(String[] args) {
		boolean rc = false;				// assume failure
		EventLogger.logEvent("PIOpurge - program started.");
		DAOpoi dao = new DAOpoi();
		Date date = new Date();
		int count = dao.countExpired(date);
		if (count == 0){
			EventLogger.logEvent("PIOpurge - no expired POIs to delete.");
			rc = true;
		}
		if (count > 0) {			
			EventLogger.logEvent("PIOpurge - deleteing " + count + " expired POIs.");
			dao = new DAOpoi();
			rc  = dao.deleteExpired(date);
		}
		dao = new DAOpoi();
		count = dao.countScope(null);
		EventLogger.logEvent("PIOpurge - " + count + " POIs remaining in database.");
		
		// exit reporting status
		if (rc)
			EventLogger.logEvent("PIOpurge - exiting with success");
		else
			EventLogger.logEvent("PIOpurge - exiting with errors");

	}

}
