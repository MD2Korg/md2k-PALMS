package edu.ucsd.cwphs.palms.gps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucsd.cwphs.palms.util.EventLogger;

// TODO:  This hides the fact that GPSResultSets are processed GPSTracks
//			GPSResult should really extend TrackPoint.

public class GPSResultSet {
	
	private GPSTrack gpsTrack = new GPSTrack();

	
	public GPSResultSet(){
		this.gpsTrack = new GPSTrack();
	}
	
	public GPSResultSet(GPSTrack gpsTrack){
		this.gpsTrack = gpsTrack;
	}
	
	public int getSize(){
		return gpsTrack.getSize();
	}
	
	public TrackPoint get(int index){
		return gpsTrack.getTrackPoint(index);
	}
	
	public ArrayList<TrackPoint> getResults(){
		return gpsTrack.getTrackPoints();
	}
	
	public static String csvHeader(){
		return TrackPoint.resultsCSVheader();
	}
	
	public static String csvHeaderPALMSformat(){
		return "identitifer,dateTime,dow,lat,lon,ele,duration,distance,speed,bearing,bearingDelta," +
				"elevationDelta,fixType,fixTypeCode,iov,tripNumber,tripType,tripMode,tripMOT,locationNumber,locationClusterFlag," +
				"nsatView,nsatUsed,snrView,snrUsed";
	}
	
	public String getPALMSformat(String identifier, String timezone, int index){
		TrackPoint tp = gpsTrack.getTrackPoint(index);
		if (tp == null)
			return ("Error - out of range index:"+index + "\n");
		
		StringBuffer sb = new StringBuffer(identifier + ",");
		sb.append(convertTime(tp, timezone) + ",");
		sb.append(tp.getLat() + ",");
		sb.append(tp.getLon() + ",");
		sb.append(tp.getEle() + ",");
		sb.append(tp.getDuration() + ",");
		sb.append(tp.getDistance() + ",");
		sb.append(tp.getSpeed() + ",");
		sb.append(tp.getBearing() + ",");
		sb.append(tp.getBearingDelta() + ",");
		sb.append(tp.getElevationDelta() + ",");
		sb.append(oldPALMSfixType(tp) + "," );
		sb.append(tp.getFixType() + ",");
		sb.append(tp.getIndoorsEstimate() + ",");
		sb.append(tp.getTripNumber() + ",");
		sb.append(tp.getTripType() + ",");
		sb.append(oldPALMStripMode(tp) + ",");
		sb.append(tp.getTripMode() + ",");
		sb.append(tp.getLocationNumber() + ",");
		sb.append(tp.getClusterFlag() + ",");
		sb.append(tp.getSatsInView() + ",");
		sb.append(tp.getSatsUsed() + ",");
		sb.append(tp.getSnrView() + ",");
		sb.append(tp.getSnrUsed() + "\n");
		return sb.toString();
	}
	
	private String convertTime(TrackPoint tp, String timezone){
		return tp.getDateTimeStr() + ",-1";			// TODO: need to implement tz and dow	
	}
	
	private String oldPALMSfixType(TrackPoint tp){
		StringBuffer sb = new StringBuffer();
		String s = "";
		// indicate fixType
		
		switch (tp.getFixType()){
		case 0:	s = "invalid + ";
				break;
		case 1: s = "valid + ";
				break;
		case 2: s = "firstfix + ";
				break;
		case 3:	s = "lastfix + ";
				break;
		case 4:	s = "lastvalidfix + ";
				break;
		case 5: s = "lonefix + ";
				break;
		case 6: s = "inserted + ";
				break;
		default: s = "";
		}
		sb.append(s);
		
		// indicate indoors / outdoors
		
		if (tp.getIndoorsEstimate() == 0)
			sb.append("outdoors +");
		else
			if (tp.getIndoorsEstimate() == 1)
				sb.append("indoors + ");
			else
				if (tp.getIndoorsEstimate() == 2)
					sb.append("invehicle + ");
		
		// indicate trip type
		switch (tp.getTripType()){
		case 0:	s = "stationary + ";
				break;
		case 1: s = "startpoint + ";
				break;
		case 2: s = "midpoint + ";
				break;
		case 3:	s = "pausepoint + ";
				break;
		case 4:	s = "endpoint + ";
				break;
		default: s = "";
		}
		sb.append(s);
		
		// indicate location clustering
		
		if (tp.getClusterFlag() == 1)
			sb.append("clustered");
		else
			if (tp.getClusterFlag() == 2)
				sb.append("clustered_center");
		
		// remove trailing + if present
		s = sb.toString();
		int len = s.length();
		int i = s.lastIndexOf(" + ");
		if (i+3 == len)
			s = s.substring(0, i);		
		return s;
	}
	
	private String oldPALMStripMode(TrackPoint tp){
		String mode = "unknown";
		switch (tp.getTripMode()){

		case 0:	mode = "stationary";
		break;

		case 1: mode = "pedestrian";
		break;

		case 2: mode = "bicycle";
		break;

		case 3:	mode = "vehicle";
		break;

		default:mode = "unknown";		
		}
		return mode;
	}
	
	public String toCSV(){
		return gpsTrack.resultsToCSV();
	}
	
	public String toJSON(){
		return gpsTrack.resultsToJSON();
	}
	
	public String fromJSON(String json){
		JSONObject obj = new JSONObject(json);
		return fromJSONObject(obj);
	}
	
	public String fromJSONObject(JSONObject obj){
		String dateTime = null;
		Date date = null;
		String nSat = null, qsatInfo = null;
		Double lat, lon, ele, speed;
		int dow, duration, distance, bearing, bearingDelta, eleDelta, fixTypeCode, iov = -1;
		int tripNumber, tripType, tripMOT, locationNumber, locationClusterFlag;
		String result = null;
		
		JSONArray array = getJSONArray(obj, "GPS_DPU_output_variables");
		if (array == null){
			EventLogger.logWarning("GPSResultSet.fromJSONObject - GPS_DPU_output_variables not found");
			return "error: GPS_DPU_output_variables not found";
		}

		for (int index=0; index < array.length(); index++) {
			try {
				JSONObject item = array.getJSONObject(index);
				dateTime = item.getString("date_time");
				// dow = item.getInt("dow");
				lat = item.getDouble("lat");
				lon = item.getDouble("lon");
				ele = item.getDouble("ele");
				duration = item.getInt("duration");
				distance = item.getInt("distance");
				speed = item.getDouble("speed");
				bearing = item.getInt("bearing");
				bearingDelta = item.getInt("bearing_delta");
				eleDelta = item.getInt("ele_delta");
				fixTypeCode = item.getInt("fix_type_code");
				// iov = item.getInt("iov");
				tripNumber = item.getInt("trip_number");
				tripType = item.getInt("trip_type");
				tripMOT = item.getInt("trip_mot");
				locationNumber = item.getInt("location_number");
				locationClusterFlag = item.getInt("location_cluster_flag");
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
				try {
					date = sdf.parse(dateTime);
				}
				catch (Exception ex){
					EventLogger.logWarning("GPSTrack.addTrackPoint - dateTime has invalid format:" + dateTime);
				}
				
				TrackPoint tp = new TrackPoint(date, lat, lon, ele, null, null); 
				tp.setDuration(duration);
				tp.setDistance(distance);
				tp.setSpeed(speed);
				tp.setBearing(bearing);
				tp.setBearingDelta(bearingDelta);
				tp.setElevationDelta(eleDelta);
				tp.setFixType(fixTypeCode);
				tp.setTripNumber(tripNumber);
				tp.setTripType(tripType);
				tp.setTripMode(tripMOT);
				tp.setLocationNumber(locationNumber);
				tp.setClusterFlag(locationClusterFlag);
				
				gpsTrack.add(tp);;

			}
			catch (Exception ex){
				EventLogger.logException("GPSResultSet.fromJSON - error parsing JSON - index = "+ index, ex);
			}
		} // end for
		return result;
	}
	
	private JSONObject getJSONObject(JSONObject obj, String key){
		JSONObject o  = null;
		try {
			o = obj.getJSONObject(key);
		}
		catch (Exception ex){
		}
		return o;
	}
	
	private JSONArray getJSONArray(JSONObject obj, String key){
		JSONArray a = null;
		try {
			a = obj.getJSONArray(key);
		}
		catch (Exception ex){
		}
		return a;
	}

	private String getJSONString(JSONObject obj, String key){
		String s = null;
		try {
			s = obj.getString(key);
		}
		catch (Exception ex){
		}
		return s;
	}

	private Double getJSONDouble(JSONObject obj, String key){
		Double d = -999.0;
		try {
			d = obj.getDouble(key);
		}
		catch (Exception ex){
		}
		return d;
	}

	
}
