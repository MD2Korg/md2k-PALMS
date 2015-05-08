package edu.ucsd.cwphs.palms.location;

import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ucsd.cwphs.palms.gps.WayPoint;

public class LocationDpuResult {
	private int number = 0;
	private String name = "";
	private double lat = WayPoint.UNKNOWNCOORDINATES;
	private double lon = WayPoint.UNKNOWNCOORDINATES;
	private double ele = WayPoint.UNKNOWNELEVATION;
	private int buffer = -1;
	private int durationAt = 0;
	private int nVisits = 0;
	private int tripsFrom = 0;
	private int tripsTotalDistance = 0;
	private int tripsMaxDistance = 0;
	private int tripsTotalDuration = 0;
	private int tripsMaxDuration = 0;
	private Date earliestDate = null;
	private Date latestDate = null;
	
	
	LocationDpuResult(int number, String name, double lat, double lon, double ele, int buffer, Date firstDate){
		this.number = number;
		this.name = name;
		this.lat = lat;
		this.lon = lon;
		this.ele = ele;
		this.buffer = buffer;
		this.earliestDate = firstDate;
		this.latestDate = firstDate;
	}
	
	public int getLocationNumber(){
		return number;
	}
	
	public void addTime(int duration, Date dateTime){
		durationAt = durationAt + duration;
		if (earliestDate.after(dateTime))
			earliestDate = dateTime;
		if (latestDate.before(dateTime))
			latestDate = dateTime;
	}
	
	public void addVisit(int duration, Date dateTime){
		nVisits++;;
		addTime(duration, dateTime);
	}
	
	public void addTripFrom(int distance, int duration, Date dateTime){
		tripsFrom++;
		addToTrip(distance, duration, dateTime);
		return;
	}
		
	
	public void addToTrip(int distance, int duration, Date dateTime){	
		tripsTotalDistance = tripsTotalDistance + distance;
		if (tripsMaxDistance < distance)									// TODO -  DOESN'T work unless at end of trip
			tripsMaxDistance = distance;
		tripsTotalDuration = tripsTotalDuration + duration;
		if (tripsMaxDuration < duration)
			tripsMaxDuration = duration;
		if (earliestDate.after(dateTime))
			earliestDate = dateTime;
		if (latestDate.before(dateTime))
			latestDate = dateTime;
	}

	public String toJSON(){
		StringBuilder sb = new StringBuilder("{\"location_number\":" + number + ",");
		sb.append("\"name\":\"" + name + "\",");
		sb.append("\"lat\":" + lat + ",");
		sb.append("\"lon\":" + lon + ",");
		sb.append("\"ele\":" + ele + ",");
		sb.append("\"buffer\":" + buffer + ",");
		sb.append("\"duration_at\":" + durationAt + ",");
		sb.append("\"number_visits\":" + nVisits + ",");
		sb.append("\"trips_from\":" + tripsFrom + ",");
		sb.append("\"trips_average_distance\":" + computeAverageDistance() + ",");
		sb.append("\"trips_max_distance\":" + tripsMaxDistance + ",");
		sb.append("\"trips_average_duration\":" + computeAverageDuration() + ",");
		sb.append("\"trips_max_duration\":" + tripsMaxDuration + ",");
		sb.append("\"earliest_date_time\":\"" + ISO8601Str(earliestDate) + "\",");
		sb.append("\"latest_date_time\":\"" + ISO8601Str(latestDate) + "\"}");
		return sb.toString();
	}
	
	public String CSVheader(){
		String s = "locationNumber, name, lat, lon, ele, buffer, durationAt, nVisits, tripsFrom, tripsAvgDistance, tripsMaxDistance, " +
					"tripsAvgDuration, tripsMaxDuration, earliestVisit, latestVisit";
		return s;
	}
	
	public String toCSV(){
		StringBuilder sb = new StringBuilder(number + ",");
		sb.append(name + ",");
		sb.append(lat + ",");
		sb.append(lon + ",");
		sb.append(ele + ",");
		sb.append(buffer + ",");
		sb.append(durationAt + ",");
		sb.append(nVisits + ",");
		sb.append(tripsFrom + ",");
		sb.append(computeAverageDistance() + ",");
		sb.append(tripsMaxDistance + ",");
		sb.append(computeAverageDuration() + ",");
		sb.append(tripsMaxDuration + ",");
		sb.append(ISO8601Str(earliestDate) + ",");
		sb.append(ISO8601Str(latestDate));
		return sb.toString();
	}
	
	private String ISO8601Str(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String s = sdf.format(date);
		return s;
	}
	
	private int computeAverageDistance(){
		if (tripsFrom == 0)
			return 0;
		else
			return tripsTotalDistance / tripsFrom;
	}
	
	private int computeAverageDuration(){
		if (tripsFrom == 0)
			return 0;
		else
			return tripsTotalDuration / tripsFrom;
	}


}
