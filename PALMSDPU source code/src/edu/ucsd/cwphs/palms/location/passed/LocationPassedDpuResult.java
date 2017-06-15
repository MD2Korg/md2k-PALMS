package edu.ucsd.cwphs.palms.location.passed;

import java.util.Date;

import edu.ucsd.cwphs.palms.kml.KMLgenerator;
import edu.ucsd.cwphs.palms.poi.POI;
import edu.ucsd.cwphs.palms.util.EventLogger;

/*
 * LocationPassedDpuResult	
 * 
 * Fredric Raab		fraab@ucsd.edu
 * 
 * 2016.05.03		1.0.0				Initial Release
 * 	
 */

public class LocationPassedDpuResult {
	private POI poi;
	private int timeAt = 0;
	private int timeWithin = 0;
	private int numberOfPasses = 0;
	private int numberOfVisits = 0;
	private Date firstSeen = null;
	private Date lastSeen = null;		
	
	LocationPassedDpuResult(POI poi){
	 this.poi = poi;
	}
	
	public POI getPOI() {
		return poi;
	}
	
	public boolean isPlaceId(String placeId){
		if (poi.getPlaceId().equalsIgnoreCase(placeId))
			return true;
		else
			return false;
	}
	
	
	public boolean addPass(Date currentDate, int duration, int minTimeBetween){
		boolean rc = false;
		if (firstSeen == null)
			firstSeen = currentDate;
		
		int seconds = timeBetween(currentDate, lastSeen);
		
		if (seconds < 0 || seconds > minTimeBetween) {
			numberOfPasses++;
			rc = true;
		}
		
		if (seconds < 0)
			seconds = duration;
		
		timeWithin = timeWithin + seconds;
		lastSeen = currentDate;
		EventLogger.logEvent("addPass - POI name: " + poi.getName() + " seconds:" + seconds + "  passes:" +
								numberOfPasses + "  timeWithin:" + timeWithin);
		
		if (seconds > LocationPassedDpuParameters.LOSsec)
			EventLogger.logWarning("addPass - timelonger than LOS timeout");
		
		return rc;
	}
	
	
	private int timeBetween(Date currentDate, Date previousDate){
		if (previousDate == null || currentDate == null)
			return -1;
		
		long cMsec = currentDate.getTime();
		long pMsec = previousDate.getTime();
		long diff = Math.abs(cMsec - pMsec);
		return (int) (diff / 1000.0);
	}
	
	
	public void addVisit(Date currentDate){
		if (firstSeen == null)
			firstSeen = currentDate;
		lastSeen = currentDate;
		numberOfVisits++;
	}
	 
	public void addTimeAt(int seconds){
		timeAt = timeAt + seconds;
		timeWithin = timeWithin + seconds;
		
		EventLogger.logEvent("addTimeAt - POI name: " + poi.getName() + " seconds:" + seconds + "  total:" + timeAt + "  visits:" + numberOfVisits);
	}
	
	public int getTimeAt(){
		return timeAt;
	}
	
	public int getTimeWithin(){
		return timeWithin;
	}
	
	public int getNumberOfPasses(){
		return numberOfPasses;
	}
	
	public int getNumberOfVisits(){
		return numberOfVisits;
	}
	
	public static String CSVheader(){
		String header = new POI().CSVheader();
		return header + ",nPasses,nVisits,timeAt,timeWithin,firstSeen,lastSeen";
	}
	
	public String toCSV(){
		String s = poi.toCSV();
		return s + "," + numberOfPasses + "," + numberOfVisits + "," + timeAt + "," + 
				timeWithin + "," + firstSeen + "," + lastSeen;
	}
	
	public String toJSON(){
		StringBuilder sb = new StringBuilder("\"location_passed\":{");
		sb.append(poi.toJSON());
		sb.append(",\"nvisits\":" + numberOfVisits + ",");
		sb.append("\"npasses\":" + numberOfPasses + ",");
		sb.append("\"timeat\":" + timeAt + ",");
		sb.append("\"timewithin\":" + timeWithin);
		sb.append("}");
		return sb.toString();
	}
	
	public String toKML(){
		String style = KMLgenerator.GREEN_POINT;			// assume pass only
		if (numberOfPasses > 0 && numberOfVisits > 0)
			style = KMLgenerator.YELLOW_POINT;				// pass and visit
		else if (numberOfPasses == 0 && numberOfVisits > 0)
			style = KMLgenerator.RED_POINT;					// visit only
		
		StringBuilder sb = new StringBuilder();
		sb.append("<Placemark>\n");
		sb.append("  <name><![CDATA["+poi.getName()+"]]></name>\n");
		sb.append("<description><![CDATA["+makeDescription()+"]]></description>\n");
		if (style != null)
			sb.append("  <styleUrl>" + style + "</styleUrl>");
		sb.append("  <Point>\n");
		sb.append("     <coordinates>"+poi.getLon()+ "," + poi.getLat()+"</coordinates>\n");
		sb.append("  </Point>\n");
		sb.append("</Placemark>\n");
		return sb.toString();
	}
	
	private String makeDescription(){
		return "Type: " + poi.getTypes() + "\nSource: " + poi.getScope() + 
				" \nNvisits: " + numberOfVisits + "\nNpasses: " + numberOfPasses + 
				" \nTimeAt: " + timeAt + "\nTimeWithin: " + timeWithin;
	}	
}
