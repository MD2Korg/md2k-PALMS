package edu.ucsd.cwphs.palms.gps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucsd.cwphs.palms.kml.KMLgenerator;
import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;
import edu.ucsd.cwphs.palms.util.WriteToFile;

import com.csvreader.CsvReader;

public class GPSTrack {

	private static final int LOSsec = 5 * 60;			// seconds
	private static final int LOSdistance = 1000;		// meters
	
	private String name;
	private ArrayList<TrackPoint>trackPoints = new ArrayList<TrackPoint>();

	public String getName(){
		return name;
	}

	public void setName(String s){
		name = s;
	}

	public TrackPoint getTrackPoint(int index) {
		return trackPoints.get(index);
	}

	public ArrayList<TrackPoint> getTrackPoints(){
		return trackPoints;
	}

	public int getSize(){
		return trackPoints.size();
	}

	public void add(TrackPoint tp){
		trackPoints.add(tp);
	}

	public String toJSON() {
		StringBuilder sb = new StringBuilder("{\"trk\":{");
		if (name != null){
			sb.append("\"name\":\"" + name + "\",");
		}
		sb.append("\"trkpts\":[");
		int size = trackPoints.size();
		if (size > 0){
			for (int index = 0; index < (size-1); index++){
				sb.append(trackPoints.get(index).toJSON() + ",");
			} // end for
			sb.append(trackPoints.get(size-1).toJSON());	// last item
		}
		sb.append("]}}");
		return sb.toString();
	}

	public String prettyPrint(){
		String s = JSONPrettyPrint.print(toJSON());
		return s;
	}

	public String resultsToJSON() {
		StringBuilder sb = new StringBuilder("{\"GPS_DPU_output_variables\":[");
		int size = trackPoints.size();
		if (size > 0){
			for (int index = 0; index < (size-1); index++){
				sb.append(trackPoints.get(index).resultsToJSON() + ",");
			} // end for
			sb.append(trackPoints.get(size-1).resultsToJSON());	// last item
		}
		sb.append("]}");
		return sb.toString();
	}

	public String resultsToCSV() {
		StringBuffer sb = new StringBuffer();	
		int size = trackPoints.size();
		for (int index = 0; index < size; index++)
			sb.append(trackPoints.get(index).resultsToCSV());
		return sb.toString();
	}


	public boolean fromGPX(String gpx){
		SAXBuilder builder = new SAXBuilder();
		boolean rc = true;
		String dateTime = null, latStr = null, lonStr = null, eleStr = null;
		Date date;

		boolean trkFound = false;
		boolean trksegFound = false;
		boolean trkptFound = false;

		// Prepare date converter for ISO 8601 GMT 
		SimpleDateFormat gmtFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		// for non-standard GPX output produced by DNR Garmin application
		SimpleDateFormat dnrFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		dnrFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		// Support for iTrail timestamps
		SimpleDateFormat iTrailFormat = new SimpleDateFormat("MM/dd/yyyy'T'hh:mm:ss aa");
		iTrailFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		try {
			Document doc;
			synchronized (builder) {
				doc = builder.build(gpx);
			}

			Element root = doc.getRootElement();
			Namespace rootNamespace = root.getNamespace();
			if (rootNamespace == null) {
				EventLogger.logError("GPSTrack.fromGPX - GPX string is empty");
				return false;
			}
			if (!root.getName().equals("gpx")) {
				EventLogger.logError("GPSTrack.fromGPX - GPX root should be gpx, but is "
						+ root.getName());
				return false;
			}

			// Loop through all tracks 

			for (Iterator<Element> it2 = ((List<Element>) root
					.getChildren()).iterator(); it2.hasNext();) {
				Element gpxElement = it2.next();
				if (gpxElement.getName() == "trk") {
					trkFound = true;

					// Loop through all track segments 

					for (Iterator<Element> it1 = ((List<Element>) gpxElement
							.getChildren()).iterator(); it1.hasNext();) {
						Element trkElement = it1.next();
						if (trkElement.getName() == "trkseg") {
							trksegFound = true;

							// Loop through all observations and create row list

							for (Iterator<Element> it = ((List<Element>) trkElement
									.getChildren()).iterator(); it.hasNext();) {
								Element child = it.next();
								if (child.getName() == "trkpt") {
									trkptFound = true;
									latStr = child.getAttributeValue("lat");
									lonStr = child.getAttributeValue("lon");
									eleStr = child.getChildText("ele", rootNamespace);
									if (eleStr == null)
										eleStr = "-999";
									dateTime = child.getChildText("time", rootNamespace);
									if (latStr == null || lonStr == null || dateTime == null) {
										EventLogger.logWarning("GPSTrack.fromGPX - TRKPT does not contain lat, lon and time");
										continue;
									}

									// Extract date and time from ISO 8601 format
									try {
										date = gmtFormat.parse(dateTime);
									}
									catch (Exception ex){
										// try Garmin DNR format
										try {
											date = dnrFormat.parse(dateTime);
											dateTime = gmtFormat.format(date);
										}
										catch (Exception ex2){
											// try iTrail format
											try {
												date = iTrailFormat.parse(dateTime);
												dateTime = gmtFormat.format(date);
											}
											catch (Exception ex3) {
												EventLogger.logWarning("GPSTrack.fromGPX - Invalid date/time format:" + dateTime);
												continue;
											}
										}
									}
									// Add data
									addTrackPoint(dateTime, latStr, lonStr, eleStr, null, null);
								} //end for <trk>
							} // end if
						} // end for
					} // end if
				} // end for <trkseg>
			} // end try
		} catch (Exception ex) {
			EventLogger.logException("GPSTrack.fromGPX - Unhandled exception: ", ex);
			return false;
		}

		if (!trkFound){
			EventLogger.logError("GPSTrack.fromGPX - No tracks TRK found.");
			rc = false;
		}
		if (!trksegFound) {
			EventLogger.logError("GPSTrack.fromGPX - No track segments TRKSEG found.");
			rc = false;
		}
		if (!trkptFound) {
			EventLogger.logError("GPSTrack.fromGPX - No track points TRKPT found.");
			rc = false;
		}
		return rc;
	}
	
	public boolean fromCSV(String fileName){
		try {
			CsvReader csvReader = new CsvReader(fileName);
			csvReader.readHeaders();
			while (csvReader.readRecord()) {
				String latitude = csvReader.get("Latitude");
				String longitude = csvReader.get("Longitude");
				String elevation = csvReader.get("Elevation");
				String timestamp = csvReader.get("Timestamp");
				timestamp = timestamp.replace("T", " ");
				timestamp = timestamp.replace("Z", "");
				addTrackPoint(timestamp, latitude, longitude , elevation, "", "");				
			} // end while
		} // end try
		catch (Exception ex){
			EventLogger.logException("GPSTrack.fromCSV - " , ex);
			return false;
		}		
		return true;
	}

	public boolean fromDatakitCSV(String fileName, int maxError){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		int kRaw = 0, kIgnored = 0;
		try {
			CsvReader csvReader = new CsvReader(fileName);
			while (csvReader.readRecord()) {
				kRaw++;
				String unixTimeStr = csvReader.get(0);
				String latitude = csvReader.get(1);
				String longitude = csvReader.get(2);
				String elevation = csvReader.get(3);
				String accuracyStr = csvReader.get(6);
				Double accuracy = Double.parseDouble(accuracyStr);	
				if (accuracy > maxError){
					kIgnored++;
					continue;
				}
				
				long unixTime = Long.parseLong(unixTimeStr);
				Date date = new Date(unixTime);
				String timestamp = sdf.format(date).toString();
				
				addTrackPoint(timestamp, latitude, longitude , elevation, "", "");				
			} // end while
		} // end try
		catch (Exception ex){
			EventLogger.logException("GPSTrack.fromDatakitCSV - " , ex);
			return false;
		}
		EventLogger.logEvent("GPSTrack.fromDatakitCSV - trackPoints processed:" + kRaw + " - Out of range:" + kIgnored);
		return true;
	}

	public String fromJSON(String json){
		JSONObject obj = new JSONObject(json);
		return fromJSONObject(obj);
	}

	public String fromJSONObject(JSONObject obj){
		String dateTime = null;
		String nSat = null, qsatInfo = null;
		Double lat, lon, ele;
		String result = null;

		JSONObject trk = getJSONObject(obj, "trk");
		if (trk == null){
			EventLogger.logWarning("GPSTrack.fromJSONObject - trk not found");
			return "error: trk not found";
		}
		JSONArray array = getJSONArray(trk, "trkpts");
		if (array == null){
			EventLogger.logWarning("GPSTrack.fromJSON - trkpts not found");
			return "error: trkpts not found";
		}

		for (int index=0; index < array.length(); index++) {
			try {
				dateTime = array.getJSONObject(index).getString("dateTime");
				lat = array.getJSONObject(index).getDouble("lat");
				lon = array.getJSONObject(index).getDouble("lon");
				ele = getJSONDouble(array.getJSONObject(index), "ele");
				nSat = getJSONString(array.getJSONObject(index), "nsat");
				qsatInfo = getJSONString(array.getJSONObject(index), "qsatinfo");

				addTrackPoint(dateTime, Double.toString(lat), Double.toString(lon) , Double.toString(ele),
						nSat, qsatInfo);
			}
			catch (Exception ex){
				EventLogger.logException("GPSTrack.fromJSON - error parsing JSON - index = "+ index, ex);
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

	private boolean addTrackPoint(String dateTime, String latStr, String lonStr, String eleStr, String nSat, String qsatInfo){
		Date date;
		double lat, lon, ele;
		if (dateTime == null){
			EventLogger.logWarning("GPSTrack.addTrackPoint - dateTime is missing");
			return false;
		}
		if (latStr == null){
			EventLogger.logWarning("GPSTrack.addTrackPoint - lat is missing for:" + dateTime);
			return false;
		}
		if (lonStr == null){
			EventLogger.logWarning("GPSTrack.addTrackPoint - lon is missing for:" + dateTime);
			return false;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
		try {
			date = sdf.parse(dateTime);
		}
		catch (Exception ex){
			EventLogger.logWarning("GPSTrack.addTrackPoint - dateTime has invalid format:" + dateTime);
			return false;
		}
		try {
			lat = Double.parseDouble(latStr);
		}
		catch (Exception ex){
			EventLogger.logWarning("GPSTrack.addTrackPoint - invalid format for lat:" + latStr);
			return false;
		}
		try {
			lon = Double.parseDouble(lonStr);
		}
		catch (Exception ex){
			EventLogger.logWarning("GPSTrack.addTrackPoint - invalid format for lon:" + lonStr);
			return false;
		}
		try {
			ele = Double.parseDouble(eleStr);
		}
		catch (Exception ex){
			EventLogger.logWarning("GPSTrack.addTrackPoint - invalid format for ele:" + eleStr);
			ele = 0.0;
		}
		TrackPoint tp = new TrackPoint (date, lat, lon, ele, nSat, qsatInfo);
		trackPoints.add(tp);
		return true;
	}

	public void toGPX(String fileName){
		TrackPoint prevTp, currentTp;
		double prevLat = -999.0, prevLon = -999.0;
		boolean inTrip = false;
		int tracknumber = 0;
		WriteToFile.newFile(fileName);
		WriteToFile.write(fileName,
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
			"<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
			" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
		
		int size = trackPoints.size();
		if (size > 2){
			prevTp = trackPoints.get(0);
			for (int index = 1; index < size; index++){
				currentTp = trackPoints.get(index);
				int seconds = currentTp.timeBetween(prevTp);
				int distance = currentTp.distanceBetween(prevTp);
				if (distance > LOSdistance)
					EventLogger.logEvent("GPSTrack.toGPX - LOS distance max exceeded - distance:" + 
							distance + " - sec:" + seconds);
					
				if (seconds > (LOSsec) || (seconds < 0)){
					EventLogger.logEvent("GPSTrack.toGPX - LOS detected - sec:" + seconds);
					
					// LOS detected
					if (inTrip){
						// end trip
						WriteToFile.write(fileName, "</trkseg>\n</trk>\n");
						inTrip = false;
					}
					prevTp = currentTp;
					continue;
				}
				else {
					// LOS not detected
					if (inTrip){
						double lat = currentTp.getLat();
						double lon = currentTp.getLon();
						// Don't add duplicate track coords
						if (lat == prevLat && lon == prevLon) {
							prevTp = currentTp;
							continue;
						}
						else {
							WriteToFile.write(fileName,
									"<trkpt lat= \"" + lat + "\"" + " lon=\"" + lon + "\">\n" +
									"<ele>" + currentTp.getEle() + "</ele>\n" +
									"<time>" + currentTp.getISO8601Str() + "</time>\n" +
									"</trkpt>\n");
							prevLat = lat;
							prevLon = lon;
							prevTp = currentTp;
						}
					}
					else {
						// not in a trip, start one
						tracknumber++;
						WriteToFile.write(fileName,
								"<trk>\n<name>Track - "+ tracknumber + "</name>\n<trkseg>\n");
						WriteToFile.write(fileName,
								"<trkpt lat= \"" + prevTp.getLat() + "\"" + " lon=\"" + prevTp.getLon() + "\">\n" +
								"<ele>" + prevTp.getEle() + "</ele>\n" +
								"<time>" + prevTp.getISO8601Str() + "</time>\n" +
								"</trkpt>\n");
						WriteToFile.write(fileName,
								"<trkpt lat= \"" + prevTp.getLat() + "\"" + " lon=\"" + prevTp.getLon() + "\">\n" +
								"<ele>" + currentTp.getEle() + "</ele>\n" +
								"<time>" + currentTp.getISO8601Str() + "</time>\n" +
								"</trkpt>\n");
						inTrip = true;
						prevTp = currentTp;
					}
				} // end for
			}
			if (inTrip){
				// end trip
				WriteToFile.write(fileName, "</trkseg>\n</trk>\n");
			}
			WriteToFile.write(fileName, "</gpx>\n");
			return;
		}
	}
	
	public void toKML(KMLgenerator gen){
		TrackPoint prevTp, currentTp;
		double prevLat = -999.0, prevLon = -999.0;
		boolean inTrip = false;
		int tracknumber = 0;
		int size = trackPoints.size();
		if (size > 2){
			prevTp = trackPoints.get(0);
			for (int index = 1; index < size; index++){
				currentTp = trackPoints.get(index);
				int seconds = currentTp.timeBetween(prevTp);
				int distance = currentTp.distanceBetween(prevTp);
				
				if (seconds > (LOSsec) || (seconds < 0) || (distance > LOSdistance)){
					EventLogger.logEvent("GPSTrack.toKML - LOS detected - distance:" + distance + "  sec:" + seconds);
					// LOS detected
					if (inTrip){
						gen.writeTrackCoords(true);
						gen.writeTrack();
						gen.closeTrack();
						inTrip = false;
					}
					prevTp = currentTp;
					continue;
				}
				else {
					// LOS not detected
					if (inTrip){
						double lat = currentTp.getLat();
						double lon = currentTp.getLon();
						// Don't add duplicate track coords
						if (lat == prevLat && lon == prevLon) {
							prevTp = currentTp;
							continue;
						}
						else {
							gen.addTrackCoords(currentTp.getLat(), currentTp.getLon(), 0);
							prevLat = lat;
							prevLon = lon;
							prevTp = currentTp;
						}
					}
					else {
						// not in a trip, start one
						tracknumber++;
						gen.addTrack("Track " + tracknumber + " - "+prevTp.getDateTimeStr(), KMLgenerator.CYAN_LINE, true);
						gen.addTrackCoords(prevTp.getLat(), prevTp.getLon(), 0);
						gen.addTrackCoords(currentTp.getLat(), currentTp.getLon(), 0);
						inTrip = true;
						prevTp = currentTp;
					}


				} // end for
			}
			if (inTrip){
				gen.writeTrackCoords(true);
				gen.writeTrack();
				gen.closeTrack();
			}
			return;
		}
	}
}

