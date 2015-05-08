package edu.ucsd.cwphs.palms.trip;

import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ucsd.cwphs.palms.gps.WayPoint;

public class TRIPdpuResult {
	private Date startDate = null;
	private Date endDate = null;
	private int number = 0;
	private int duration = -1;
	private int distance = -1;
	private int tripMot = -1;
	private double avgSpeed = -1.0;
	private int startLocationNumber = -1;
	private int endLocationNumber = -1;
	private double startLat = WayPoint.UNKNOWNCOORDINATES;
	private double startLon = WayPoint.UNKNOWNCOORDINATES;
	private double startEle = -999.0;
	private double endLat = WayPoint.UNKNOWNCOORDINATES;
	private double endLon = WayPoint.UNKNOWNCOORDINATES;
	private double endEle = -999.0;
	
	
	TRIPdpuResult(Date startDate, Date endDate, int number, int duration, int distance, int tripMot, double avgSpeed,
				int startLocationNumber, int endLocationNumber, double startLat, double startLon, double startEle, 
				double endLat, double endLon, double endEle){
		this.startDate = startDate;
		this.endDate = endDate;
		this.number = number;
		this.duration = duration;
		this.distance = distance;
		this.tripMot = tripMot;
		this.avgSpeed = avgSpeed;
		this.startLocationNumber = startLocationNumber;
		this.endLocationNumber = endLocationNumber;
		this.startLat = startLat;
		this.startLon = startLon;
		this.startEle = startEle;
		this.endLat = endLat;
		this.endLon = endLon;
		this.endEle = endEle;
		}
	
	public String toJSON(){
		StringBuilder sb = new StringBuilder("{\"time_interal\":{");
		sb.append("\"start_date_time\":\"" + ISO8601Str(startDate) + "\",");
		sb.append("\"end_date_time\":\"" + ISO8601Str(endDate) + "\"},");
		sb.append("\"number\":" + number + ",");
		sb.append("\"duration\":" + duration + ",");
		sb.append("\"distance\":" + distance + ",");
		sb.append("\"trip_mot\":" + tripMot + ",");
		sb.append("\"avg_speed\":" + avgSpeed + ",");
		sb.append("\"start_location_number\":" + startLocationNumber + ",");
		sb.append("\"start_lat\":" + startLat + ",");
		sb.append("\"start_lon\":" + startLon + ",");
		sb.append("\"start_ele\":" + startEle + ",");
		sb.append("\"end_location_number\":" + endLocationNumber + ",");
		sb.append("\"end_lat\":" + endLat + ",");
		sb.append("\"end_lon\":" + endLon + ",");
		sb.append("\"end_ele\":" + endEle + "}");
		return sb.toString();
	}
	
	public String CSVheader(){
		String s = "startDate, endDate, number, duration, distance, mot, avgSpeed, startLocation, startLat, startLon, startEle, " +
					"endLocation, endLat, endLon, endEle";
		return s;
	}
	
	public String toCSV(){
		StringBuilder sb = new StringBuilder(ISO8601Str(startDate) + ",");
		sb.append(ISO8601Str(startDate) + ",");
		sb.append(number + ",");
		sb.append(duration + ",");
		sb.append(distance + ",");
		sb.append(tripMot + ",");
		sb.append(avgSpeed + ",");
		sb.append(startLocationNumber + ",");
		sb.append(startLat + ",");
		sb.append(startLon + ",");
		sb.append(startEle + ",");
		sb.append(endLocationNumber + ",");
		sb.append(endLat + ",");
		sb.append(endLon + ",");
		sb.append(endEle);
		return sb.toString();
	}
	
	private String ISO8601Str(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String s = sdf.format(date);
		return s;
	}
	
	
}
