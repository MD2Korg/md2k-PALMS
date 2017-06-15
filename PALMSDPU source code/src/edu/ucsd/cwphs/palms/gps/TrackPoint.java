package edu.ucsd.cwphs.palms.gps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TrackPoint extends WayPoint {

	private String nSat = null;
	private String qsatInfo = null;
	private Date dateTime = null;
	
	// derived values follow
	private int duration = 0; // at this point (before next point) in seconds
	private double speed = 0; // speed of travel from previous point in km / hour
	private int distance = 0; // distance from previous point in meters
	private int elevationDelta = 0; // change in elevation from last point
	private int bearing = -1;	// bearing from last point
	private int bearingDelta = 0; // change in direction since last point
	
	// trip & location related
	private int tripNumber = -1; // not part of any trip
	private int tripMode = MOT_UNKNOWN; // mode of transportation
	private int tripType = -1;
	private int locationNumber = -1; // not assigned to a location
	private int clusterFlag = 0;
	
	// fix related
	private int indoorsEstimate = -1; // -1=not known, 0=outdoors, 1=indoors, 2=in vehicle
	private int snrUsed = -1;
	private int snrView = -1;
	private int avgSnrUsed = -1;
	private int filterReason = 0;
	private int fixType = -1;
	private int nsatUsed = -1;
	private int nsatView = -1;
	private String debugStr = ""; // for debugging

	// Trip types
	public static final int STATIONARY = 0;
	public static final int STARTPOINT = 1;
	public static final int MIDPOINT = 2;
	public static final int PAUSEPOINT = 3;
	public static final int ENDPOINT = 4;
	public static final int CHANGEMODE = 5;
	
	// Fix Types
	public static final int UNKNOWN = -1;
	public static final int INVALID = 0;
	public static final int VALID = 1;
	public static final int FIRSTFIX = 2;
	public static final int LASTFIX = 3;
	public static final int LASTVALIDFIX = 4;
	public static final int LONEFIX = 5;
	public static final int INSERTED = 6;
	
	// indoors / outdoors / in vehicle
	public static final int OUTDOORS = 0;
	public static final int INDOORS = 1;
	public static final int INVEHICLE = 2;
	
	// cluster flags
	public static final int NOTCLUSTERED = 0;
	public static final int CLUSTERED = 1;
	public static final int CLUSTERCENTER = 2;

	// Filter reasons
	public static final int FILTERED_NOT = 0;
	public static final int FILTERED_MAXGAP = 1;
	public static final int FILTERED_LONEFIX = 2;
	public static final int FILTERED_MAXSPEED = 3;
	public static final int FILTERED_MAXELEVATION = 4;
	public static final int FILTERED_MINDISTANCE = 5;
	public static final int FILTERED_MINDURATION = 6;
	public static final int FILTERED_FORWARDBACKWARDS = 7;
	public static final int FILTERED_DUPLICATETIME = 8;
	public static final int FILTERED_CLUSTERED = 9;
	public static final int FILTERED_ALIGN = 10;
	
	// TrackPoint trip modes of transportation
	public static final int MOT_UNKNOWN = -1;
	public static final int MOT_STATIONARY  = 0;	
	public static final int MOT_PEDESTRIAN = 1;
	public static final int MOT_BICYCLE = 2;
	public static final int MOT_VEHICLE = 3;
	
	
	
	
	public TrackPoint(Date dateTime, double lat, double lon, double ele, String nSat, String qsatInfo){
		this.dateTime = dateTime;
		this.lat = lat;
		this.lon = lon;
		this.ele = ele;
		this.nSat = nSat;
		this.qsatInfo = qsatInfo;
	}
	
public String toJSON(){
	if (dateTime == null) return "";
	
	String json = "{";
	json = json + "\"lat\":" + lat + ",";
	json = json + "\"lon\":" + lon + ",";
	json = json + "\"ele\":" + ele + ",";
	
	if (nSat != null)
		json = json + "\"nSat\":\"" + nSat + "\",";
	if (qsatInfo != null)
		json = json + "\"qsatInfo\":\"" + qsatInfo + "\",";
	json = json + "\"dateTime\":\"" + getISO8601Str() + "\"";
	json = json + "}";
	return json;
	}

public String resultsToJSON(){
	if (dateTime == null) return "";
	
	String json = "{";
	json = json + "\"date_time\":\"" + getISO8601Str() + "\",";
	json = json + "\"lat\":" + lat + ",";
	json = json + "\"lon\":" + lon + ",";
	json = json + "\"ele\":" + ele + ",";
	json = json + "\"duration\":" + duration + ",";
	json = json + "\"distance\":" + distance + ",";
	json = json + "\"speed\":" + speed + ",";
	json = json + "\"bearing\":" + bearing + ",";
	json = json + "\"bearing_delta\":" + bearingDelta + ",";
	json = json + "\"ele_delta\":" + elevationDelta + ",";
	json = json + "\"fix_type_code\":" + fixType + ",";
//	json = json + "\"iov\":" + iov + ",";
	json = json + "\"trip_number\":" + tripNumber + ",";
	json = json + "\"trip_type\":" + tripType + ",";
	json = json + "\"trip_mot\":" + tripMode + ",";
	json = json + "\"location_number\":" + locationNumber + ",";
	json = json + "\"location_cluster_flag\":" + clusterFlag + "";
	json = json + "}";
	return json;
	}

public static String resultsCSVheader(){
	// TODO: Insert iov when available
	String s = "dateTime, lat, lon, ele, duration, distance, speed, bearing, bearingDelta, eleDelta, " +
				"fixType, tripNumber, tripType, tripMOT, locationNumber, locCluster";
	return s;
	}

public String resultsToCSV(){
	if (dateTime == null) 
		return "";
	else 
		return toString() + "\n";
}
	
public String toString(){				// override toString() and return data as CSV -- useful in debugger
	String s = getISO8601Str() + ",";
	s = s + lat + "," + lon + "," + ele + ",";
	s = s + duration + "," + distance + "," + speed + "," + bearing + ",";
	s = s + bearingDelta + "," + elevationDelta + "," + fixType + ",";
//	s = s + iov + ",";
	s = s + tripNumber + "," + tripType + "," + tripMode + ",";
	s = s + locationNumber + "," + clusterFlag; 
	return s;
	}

//gets / sets

	public Date getDateTime(){
		return dateTime;
	}
	
	public void setDateTime(Date dateTime){
		this.dateTime = dateTime;
	}

	public String getDateTimeStr(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String s = sdf.format(dateTime);
		return s;
	}
	
	public String getISO8601Str(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String s = sdf.format(dateTime);
		return s;
	}
	
	public int getFilterReason(){
		return filterReason;
	}
	
	public void setFilterReason(int r){
		filterReason = r;
	}
	
	public int getFixType() {
		return fixType;
	}

	public void setFixType(int t) {
		fixType = t;
	}
	
	public boolean isFirstFix(){
		return (fixType == FIRSTFIX);
	}
	
	public boolean isLastFix(){
		return (fixType == LASTFIX);
	}
	
	public boolean isLoneFix(){
		return (fixType == LONEFIX);
	}
	
	public boolean isValidFix(){
		return (fixType == VALID);
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int d) {
		duration = d;
	}

	public void addDuration(int d) {
		duration = duration + d;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double s) {
		speed = s;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int d) {
		distance = d;
	}

	public int getElevationDelta() {
		return elevationDelta;
	}

	public void setElevationDelta(int d) {
		elevationDelta = d;
	}
	
	public int getBearing() {
		return bearing;
	}

	public void setBearing(int b) {
		bearing = b;
	}
	
	public int getBearingDelta() {
		return bearingDelta;
	}

	public void setBearingDelta(int d) {
		bearingDelta = d;
	}

	public void setTripNumber(int t) {
		tripNumber = t;
	}

	public int getTripNumber() {
		return tripNumber;
	}

	public void setLocationNumber(int t) {
		locationNumber = t;
	}

	public int getLocationNumber() {
		return locationNumber;
	}
	
	public int getClusterFlag(){
		return clusterFlag;
	}
	
	public void setClusterFlag(int f){
		clusterFlag = f;
	}
	
	public boolean isClustered(){
		return (clusterFlag != NOTCLUSTERED);
	}
	
	public boolean isClusterCenter(){
		return (clusterFlag == CLUSTERCENTER);
	}

	public void setIndoorsEstimate(int e) {
		indoorsEstimate = e;
	}

	public int getIndoorsEstimate() {
		return indoorsEstimate;
	}
		
	public void setIndoors(){
		indoorsEstimate = INDOORS;
	}
	
	public void setOutdoors(){
		indoorsEstimate = OUTDOORS;
	}
	
	public void setInVehicle(){
		indoorsEstimate = INVEHICLE;
	}
	
	public boolean isIndoors(){
		return (indoorsEstimate == INDOORS);
	}
	
	public boolean isOutdoors(){
		return (indoorsEstimate == OUTDOORS);
	}
	
	public boolean isInVehicle(){
		return (indoorsEstimate == INVEHICLE);
	}
	
	public void setTripType(int t){
		tripType = t;
	}
	
	public int getTripType(){
		return tripType;
	}
	
	public boolean isPausePoint(){
		return (tripType == PAUSEPOINT);
	}
	
	public boolean isStartPoint(){
		return (tripType == STARTPOINT);
	}
	
	public boolean isEndPoint(){
		return (tripType == ENDPOINT);
	}
	
	public boolean isMidPoint(){
		return (tripType == MIDPOINT);
	}
	
	public boolean isStationary(){
		return (tripType == STATIONARY);
	}
	
	public void setTripMode(int mode) {
		tripMode = mode;
	}

	public int getTripMode() {
		return tripMode;
	}

	public void setDebugStr(String s) {
		debugStr = s;
	}

	public String getDebugStr() {
		return debugStr;
	}



	public void setQSatInfo(String s) {
		qsatInfo = s;
	}

	public void setSatsInView(String s) {
		try {
			nsatView = Integer.parseInt(s);
		} catch (Exception ex) {
			nsatView = -1;
		}
	}

	public void setSatsInView(int n) {
		nsatView = n;
	}

	public void setSatsUsed(String s) {
		try {
			nsatUsed = Integer.parseInt(s);
		} catch (Exception ex) {
			nsatUsed = -1;
		}
	}

	public void setSatsUsed(int n) {
		nsatUsed = n;
	}

	public int getSatsUsed() {
		return nsatUsed;
	}

	public int getSatsInView() {
		return nsatView;
	}

	public int getSnrUsed() {
		return snrUsed;
	}

	public void setSnrView(int snr) {
		snrView = snr;
	}

	public int getSnrView() {
		return snrView;
	}

	public void setAvgSnrUsed(int snr) {
		avgSnrUsed = snr;
	}

	public int getAvgSnrUsed() {
		return avgSnrUsed;
	}

	public String getQSatInfo() {
		return qsatInfo;
	}

	public int calculateSnrUsed() {
		int snr = 0;
		if (qsatInfo == null || qsatInfo.equalsIgnoreCase(""))
			return -1; // no sat info available

		for (int satIndex = 0; satIndex < nsatView; satIndex++) {
			if (isUsed(satIndex)) {
				snr = snr + getSNR(satIndex);
			}
		}
		snrUsed = snr;
		return snr;
	}

	public int calculateSnrView() {
		int totalSnr = 0, snr = 0;
		if (qsatInfo == null || qsatInfo.equalsIgnoreCase(""))
			return -1; // no sat info available

		for (int satIndex = 0; satIndex < nsatView; satIndex++) {
			snr = getSNR(satIndex);
			if (snr < 0)
				break;
			else
				totalSnr = totalSnr + snr;
		}	
		snrView = totalSnr;
		return totalSnr;
	}

	private int getSNR(int satIndex) {
		int rc = -1;
		int i;
		String s;
		String info = getColumn(qsatInfo, satIndex);
		try {
			if (info.length() > 2) {
				i = info.length() - 2; // SNR is last two digits
				s = info.substring(i).trim();
				rc = Integer.parseInt(s);
			}
		}
		catch (Exception ex){
			System.out.println("TrackPoint.getSNR - invalid value at index:" + satIndex + " qsatInfo:" + qsatInfo);	
		}
		return rc;
	}

	private boolean isUsed(int satIndex) {
		String info = getColumn(qsatInfo, satIndex);
		if (info.length() > 1)
			return (info.contains("#"));
		else
			return false;
	}

	private String getColumn(String data, int column) {
		int si = 0, ei = 0;
		int count = 0;
		String str = "";
		String s = data + ";"; // append ; so we'll always find one
		for (count = 0; count != column; count++) {
			ei = s.indexOf(";", si);
			if (si == -1)
				return str;
			si = ei + 1;
		}
		ei = s.indexOf(";", si);
		if (ei == -1)
			return "";
		str = s.substring(si, ei);
		return str;
	}

	public double speedBetween(TrackPoint tp) {
		double speed = 0.0;
		int time = timeBetween(tp); // get time in seconds
		if (time == 0)
			return 0;

		double distance = calDistance(tp); // get distance in meters
		speed = distance / time; // in meters per second
		speed = speed * 3.6; // convert to km per hour

		return speed;
	}

	public int timeBetween(TrackPoint tp) {
		// compute duration in seconds
		int sec = Math
				.abs((int) (dateTime.getTime() - tp.dateTime.getTime()) / 1000);
		return sec;
	}

	public int timeBetween(Date ts) {
		// compute duration in seconds
		int sec = Math.abs((int) (dateTime.getTime() - ts.getTime()) / 1000);
		return sec;
	}

	public int elevationBetween(TrackPoint tp) {
		// compute difference in elevation
		return (int) (ele - tp.ele);
	}

	public int distanceBetween(TrackPoint tp) {
		return calDistance(tp);
	}
	
	// parameter tp is the previous trackpoint 
	public int bearingChange(TrackPoint tp) {
		if ((bearing == -1) || (tp.bearing == -1))
			return 0;		// can't calculate, return 0
		
		int delta = bearing - tp.bearing;
		if (delta > 180)
			delta = delta-360;
		else 
			if (delta < -180)
				 delta = delta +360;
		return delta;
	}

	public void average(TrackPoint tp) {
		lat = (lat + tp.lat) / 2; // average coordinates
		lon = (lon + tp.lon) / 2;
		ele = (ele + tp.ele) / 2;
		distance = (int) this.calDistance(tp); // compute distance of average
		// from previous
	}

	public static int findLatest(ArrayList<TrackPoint> vtp, int startingIndex,
			Date ts, int threshold) {
		int index = 0;
		TrackPoint tp;
		;
		long targetmsec = ts.getTime();
		if (startingIndex < 0)
			startingIndex = 0;
		for (index = startingIndex; index < vtp.size(); index++) {
			tp = vtp.get(index);
			if (tp.dateTime.before(ts))
				continue;
			if (tp.dateTime.after(ts)) {
				long msec = tp.dateTime.getTime();
				if ((msec - targetmsec) <= (threshold * 1000))
					return index;
				else {
					index = index - 1;
					msec = vtp.get(index).dateTime.getTime();
					if ((targetmsec - msec) <= (threshold * 1000))
						return index;
					else
						return -1;
				}
			}
		} // end for
		return -1; // not found
	}

	public static int findSubjectLocation(ArrayList<TrackPoint> vtp,
			int startingIndex, Date ts, int threshold) {
		int index = -1;
		TrackPoint tp;
		;
		if (startingIndex < 0)
			startingIndex = 0;
		for (index = startingIndex; index < vtp.size(); index++) {
			tp = vtp.get(index);
			if (tp.dateTime.before(ts))
				continue;
			index--;
			if (index < 0) // special case for 1st element
				index = 0;
			tp = vtp.get(index); // get earlier location
			int dif = tp.timeBetween(ts);
			if (tp.duration >= dif)
				return index; // subject was at that location
			else
				return -1;
		} // end for
		return -1; // not found
	}

}