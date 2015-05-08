package edu.ucsd.cwphs.palms.kml;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.Location;
import edu.ucsd.cwphs.palms.gps.LocationList;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.gps.WayPoint;
import edu.ucsd.cwphs.palms.util.EventLogger;


/*
 * Version	Date		Change
 * 1.0.3	3/5/10		outputLocations - fixed clustering problem (time @ location not accumulated
 * 1.0.4	3/9/10  	move indoors/outdoor outside of date checks; include activity Intensity
 * 1.0.6	3/10/10		add arrival, departure timestamps to location
 * 1.0.7	3/12/10		classify heart rate by intensity level
 * 1.0.8	4/19/10		new defaults for Pilot 3
 * 1.0.9	4/21/10		Add option to display trip numbers
 * 1.0.10	4/22/10		Fix bug - don't include invalid coordinates in line tracks
 * 1.0.11	4/30/10		When filtering redundant points, don't filter start and end points
 * 1.0.12	5/03/10		New EventLogger code
 * 1.0.13	7/22/10		Set initial camera view to first point in dataset
 * 						Add distance value to by-trip fixpoints
 * 1.0.14	7/28/10		OutputByTrip - don't output points where tripNumber == 0
 * 						add OutputRaw option
 * 						color track by mode of transportation
 * 1.0.15	8/06/10		organize folders by date is now optional
 * 1.0.16	8/17/10		outputLocations tests for both CLUSTER_CENTERED and CLUSTERED
 * 						need both for cases where CLUSTER_CENTERED fix might not be in result set
 * 1.0.17	8/20/10		add bicycle track; modified all track colors
 * 						add tracks color code by subjects
 * 
 * 1.1.0	2/24/11		Uses PrintWriter instead of string buffer to prevent out-of-memory failures
 * 1.1.1	3/01/11		Set HeartRate and Indoors folders default to false -- not visible when opened
 * 1.1.2	3/04/11		Filter only redundant points marked as stationary
 * 1.1.3	4/01/11		Add Change-of-speed points (cyan) in outputByTrips
 * 1.1.4	4/18/11		Filter %xx in filename and replace with underscores
 * 1.1.5	4/20/11		Remove FirstFix folder on Trips
 * 1.1.6	4/22/11		Fixed bug in outputByLocations where non-locations (-1) were included; and others skipped
 * 						Allow for endPoint and startPoint to be set within same TrackPoint
 * 1.1.7	5/03/11		Set wayPoint name in  outputBySpeed to ""
 * 1.1.8	6/22/11		Add calls to System.cg() and logMemory()
 * 1.1.9	6/24/11		Verify that lat/lon exist in result set; throw exception in not present
 * 1.1.10	6/30/11		Add option to not export midpoints of trips
 * 1.1.11	7/15/11		Changed KMLgenerator.initKML - remove creator attribute; update xmlns
 * 1.1.12	8/12/11		Filter null points in outputByActivityLevel
 * 1.1.13	9/06/11		Set initial camera position to first valid coordinate in result set
 * 1.1.14	10/04/11	Added Main() for testing
 *						Moved test for no locations (to allow for common locations - needs additional work
 * 1.1.15	10/14/11	in outputByTracks -- test for trip number == 0 to be consistent with outputByTrips
 * 1.1.16	06/21/12	Add outputByStationary -- useful for detecting missed trips
 * 1.2.0	08/06/12	Fixed bug where First Time of arrival may be improperly set. see PALMS-511
 * 1.2.1	08/07/12	Sort locations before output; pad names to equal length for sorting
 * 1.2.2	08/10/12	Output Stationary points in folders by date; don't output inserted points
 * 2.1.0	12/12/14	Modify from MD2K
 * 		
 * 
 */ 



/*
 *  These routines turn a result set into KML
 *  
 *  Typical usage:
 *  
 *  	KMLexport kml = new KMLexport(PrintWriter printWriterReference);
 *  	kml.setKMLName(typically_set_to_name_of_output_file);
 *		kml.export(GPSTrack_to_be_exported);  
 *  
 */

public class KMLexport {

	private static final String VERSION = "2.1.0  12 Dec 2014";
	private EventLogger eventLogger = new EventLogger();
	private PrintWriter printWriter;

	private String previousLat = "";
	private String previousLon = "";
	private KMLgenerator gen;	

	// Parameters that ideally would be passed in parameter block
	// Use these defaults for now

	private GPSTrack inputTrack = null;
	private String kmlName = "PALMS";					// name of kml top level
	private Calendar startDate = null;					// starting date
	private Calendar endDate = null;					// ending date - null = all dates in result set

	private boolean includeHeartRate = true;			// determine what "views" to display
	private boolean includeSpeed = false;
	private boolean includeActivityIntensity = true;
	private boolean includeActivityBouts = true;
	private boolean displayBoutNumber = true;
	private boolean includeSendentary = false;
	private boolean includeStationary = true;
	private boolean includeTrips = true;
	private boolean includeMidPoints = true;
	private boolean displayTripNumber = true;
	private boolean includeTracks = true;
	private boolean includeTracksColoredRandom = false;
	private boolean includeLocations = true;			
	private boolean includeFirstLast = false;	
	private boolean includeIndoorOutdoor = false;
	private boolean includeAll = false;					// output all points

	private boolean temporalMode = false;				// misc parameters
	private boolean organizeByDate = false;
	private boolean enableTimeSlider = false;
	private boolean filterRedundantPoints = false;
	private boolean filterNullPoints = true;
	private int nLocationBreaks = 8;
	private int AImin = -1; 							// *** Min Activity Intensity Level to display
	private boolean AIfilter_vehicle = false;			//
	private int AIvehicle_speed_cutoff = 20;			//

	public KMLexport(String outputFileName) throws Exception {
		printWriter = new PrintWriter(outputFileName);

		// generate unique log file for this instance
		eventLogger.setFileName("logs/KML-"+UUID.randomUUID());
		eventLogger.logEvent("KMLexport - Version "+ VERSION);

		// pass reference to KML generator
		gen = new KMLgenerator(eventLogger, printWriter);
	}


	public void setKMLName(String s){
		if (s.contains("%")){
			// replace %xx with underscores
			int position = s.indexOf("%");
			kmlName = s.substring(0, position) + '_';
			int begin = position + 3;

			while (true){
				position = s.indexOf("%", begin);
				if (position != -1){
					kmlName = kmlName + s.substring(begin, position) + '_';
					begin = position + 3;
				}
				else {
					// copy end of string
					kmlName = kmlName + s.substring(begin);	
					break;
				}
			} // end while
		} // end if (s.contains)
		else
			kmlName = s;
	}

	public void setStartDate(Calendar cal){
		startDate = (Calendar) cal.clone();
	}

	public void setStartDate(Timestamp ts){
		startDate = Calendar.getInstance();
		startDate.setTimeInMillis(ts.getTime());
	}

	public void setEndDate(Calendar cal){
		endDate = (Calendar) cal.clone();
	}

	public void setEndDate(Timestamp ts){
		endDate = Calendar.getInstance();
		endDate.setTimeInMillis(ts.getTime());
	}

	public void setIncludeHeartRate(boolean flag){
		includeHeartRate = flag;
	}

	public void setIncludeActivityIntensity(boolean flag){
		includeActivityIntensity = flag;
	}
	public void setIncludeActivityBouts(boolean flag){
		includeActivityBouts = flag;
	}
	public void setIncludeSpeed(boolean flag){
		includeSpeed = flag;
	}
	public void setIncludeTrips(boolean flag){
		includeTrips = flag;
	}	
	public void setIncludeMidPoints(boolean flag){
		includeMidPoints = flag;
	}
	public void setIncludeLocations(boolean flag){
		includeLocations = flag;
	}
	public void setIncludeFirstLast(boolean flag){
		includeFirstLast = flag;
	}
	public void setIncludeTracks(boolean flag){
		includeTracks = flag;
	}
	public void setIncludeIndoorOutdoor(boolean flag) {
		includeIndoorOutdoor = flag;
	}

	public void setTemporalMode(boolean flag){
		temporalMode = flag;
	}
	public void setFilterRedundantPoints(boolean flag){
		filterRedundantPoints = flag;
	}

	public void exportTrack(GPSTrack track) {

		inputTrack = track;					// store reference to track

		if (!includeHeartRate && !includeSpeed && !includeActivityIntensity && !includeTrips && !temporalMode){
			eventLogger.logError("KMLexport.export - No export options selected.");
			return;
		}	

		// verify input rowList to export is valid
		if (track == null) {
			eventLogger.logError("KMLexport.export - No input track set.");
			return;
		}

		if (track.getSize() == 0){
			eventLogger.logWarning("KMLexport.export - track has size 0 - nothing to export.");
			return;
		}

		eventLogger.logEvent("KMLexport.export - track size = " + track.getSize());
		eventLogger.logEvent("KMLexport.export - kmlName = " + kmlName);

		gen.initKML(kmlName);	
		gen.setEnableTimeSlider(enableTimeSlider);
		defineStyles();
		setInitialCamera();

		export();

		eventLogger.logEvent("KMLexport.export - finished  " + gen.getNumLinesWritten() + " lines written");
		gen.closeKML();

	}

	/*
	 * PRIVATE methods follow
	 */

	// set initial camera position to first valid coordinate in result set
	private boolean setInitialCamera(){
		TrackPoint tp;
		Double dlon;
		for (int index = 0; index < inputTrack.getSize(); index++){
			tp = inputTrack.getTrackPoint(index);
			if (tp.getLat() == WayPoint.UNKNOWNCOORDINATES)
				continue;
			else {
				gen.setInitialCamera(tp.getLatStr(), tp.getLonStr());
				return true;
			}
		} // end for
		eventLogger.logWarning("KMLexport.setInitialCamera -- no valid coordinates found in track");
		return false;
	}


	private boolean defineStyles(){

		// KML color mask - AABBGGRR
		// define styles			
		gen.writeStylePoint("hr90", "CC0000FF", .40);
		gen.writeStylePoint("hr80", "CC0066FF", .40);
		gen.writeStylePoint("hr70", "CC0099FF", .40);
		gen.writeStylePoint("hr60", "CC00CCFF", .40);
		gen.writeStylePoint("hr50", "CC99FFFF", .40);   // moderate
		gen.writeStylePoint("hr40", "CC99FFCC", .40);   // light
		gen.writeStylePoint("hr30", "CCAAAA00", .40);   // resting

		gen.writeStylePoint("red", "CC0000FF", .70);
		gen.writeStylePoint("orange", "CC0099FF", .65);
		gen.writeStylePoint("yellow", "CC00FFFF", .50);
		gen.writeStylePoint("yellow45", "CC00FFFF", .45);
		gen.writeStylePoint("green", "CC00FF00", .45);
		gen.writeStylePoint("blue", "CCFF0000", .40);
		gen.writeStylePoint("blue45", "CCFF0000", .45);
		gen.writeStylePoint("cyan", "CCAAAA00", .50);
		gen.writeStylePoint("gray", "66AAAAAA", .35);

		if (includeTracks){
			gen.writeStyleLine("trackAir", "770000FF", 4);		// red
			gen.writeStyleLine("trackAuto", "77CCCC00", 4);		// cyan
			gen.writeStyleLine("trackBicycle", "7700CCCC", 4);	// yellow
			gen.writeStyleLine("trackRunning", "77CC8800", 4);
			gen.writeStyleLine("trackWalking", "7700FF00", 4);	// green
			gen.writeStyleLine("trackBySubject", "77FFFFFF", 4, true);  // random color
		}

		// add Legends

		gen.addFolder("Legend");
		if (includeActivityIntensity || includeActivityBouts) {
			gen.addFolder("Activity Intensity");
			gen.addLegend("Very Vigorous", "red");
			gen.addLegend("Vigorous", "orange");	
			gen.addLegend("Moderate", "yellow");
			gen.addLegend("Light", "green");
			gen.addLegend("Stationary", "blue");
			gen.addLegend("Unknown", "gray");
			gen.closeFolder();
		}
		if (includeHeartRate) {
			gen.addFolder("HeartRate Intensity");
			gen.addLegend("VO2 Max","hr90");
			gen.addLegend("Anaerobic", "hr80");	
			gen.addLegend("Aerobic", "hr70");
			gen.addLegend("Weight Control", "hr60");
			gen.addLegend("Moderate","hr50");
			gen.addLegend("Light", "hr40");
			gen.addLegend("Resting", "hr30");
			gen.addLegend("Unknown", "gray");
			gen.closeFolder();
		}
		if (includeSpeed) {
			gen.addFolder("Speed");
			gen.addLegend("Vehicle     15+", "red");
			gen.addLegend("Run - Bike  7-14.9", "orange");	
			gen.addLegend("Jog         4-6.9", "yellow");
			gen.addLegend("Walking     0-3.9", "green");
			gen.addLegend("Stationary  0", "blue");
			gen.addLegend("Unknown", "gray");
			gen.closeFolder();
		}
		if (includeTrips) {
			gen.addFolder("Trip Indicators");
			gen.addLegend("Stationary", "blue");
			gen.addLegend("Start", "green");
			gen.addLegend("Moving", "yellow");
			gen.addLegend("Pause", "orange");
			gen.addLegend("Speed Change", "cyan");
			gen.addLegend("Stop", "red");
			gen.addLegend("Unknown", "gray");
			gen.closeFolder();
		}
		if (includeIndoorOutdoor){
			gen.addFolder("Indoor / Outdoor Indicators");
			gen.addLegend("Indoor", "yellow45");
			gen.addLegend("Outdoor", "green");
			gen.addLegend("Unknown", "gray");
			gen.closeFolder();					
		}
		gen.closeFolder();			// legend folder
		return true;
	}

	private void export(){
		String prevDate = "", date, dateTime;
		int index = 0;
		if (includeAll)
			outputAll(inputTrack);
		if (includeLocations)
			outputLocations(inputTrack, nLocationBreaks);
		if (includeTracks)
			outputByTracks(inputTrack);

		if (organizeByDate){ 
			// Break input into individual dates
			GPSTrack dayTrack = new GPSTrack();
			TrackPoint tp = inputTrack.getTrackPoint(0);					// get first 
			prevDate = tp.getDateTimeStr().substring(0,10);
			dayTrack.add(tp);
			index = 1;
			while (index < inputTrack.getSize()){
				tp = inputTrack.getTrackPoint(index);
				dateTime = tp.getDateTimeStr();
				date = dateTime.substring(0,10);
				if (prevDate.equalsIgnoreCase(date)){
					// same date
					dayTrack.add(tp);
					prevDate = date;
				}
				else {
					// new date - process current rowList		

					if (dayTrack.getSize() > 0){
						gen.addRadioFolder(prevDate, false);
						if (includeSpeed)
							outputBySpeed(dayTrack);
						if (includeTrips)
							outputByTrips(dayTrack);
						if (includeStationary)
							outputByStationary(dayTrack);
						if (temporalMode)			
							outputPathByTime(dayTrack);
						gen.closeFolder(); // date folder
					}
					dayTrack = new GPSTrack();
					if (index < inputTrack.getSize())
						index--; 					// back up index
					prevDate = date;
				} // end else
			} // end while

			// process final date
			if (dayTrack.getSize() > 0){
				gen.addRadioFolder(prevDate, false);

				if (includeSpeed)
					outputBySpeed(dayTrack);
				if (includeTrips)
					outputByTrips(dayTrack);
				if (includeStationary)
					outputByStationary(dayTrack);
				if (temporalMode)			
					outputPathByTime(dayTrack);
				gen.closeFolder(); // date folder
			}
		}
		else {
			// don't organize into date folders

			if (includeSpeed)
				outputBySpeed(inputTrack);
			if (includeTrips)
				outputByTrips(inputTrack);
			if (includeStationary)
				outputByStationary(inputTrack);
			if (temporalMode)			
				outputPathByTime(inputTrack);	
		} // end else

	}	


	private void outputLocations(GPSTrack track, int numclass){
		String locationName;
		Location loc;
		LocationList vLoc = new LocationList();
		List<Location> lLoc = new ArrayList<Location>();

		double size = 1;

		// build list of locations
		for (int i = 0; i<track.getSize(); i++){
			TrackPoint tp = track.getTrackPoint(i);

			if (tp.getClusterFlag() == TrackPoint.CLUSTERCENTER){
				locationName = Integer.toString(tp.getLocationNumber());
				if (locationName.equalsIgnoreCase("-1"))
					continue;									// fjr 4/22/11 - skip over non-locations

				loc = vLoc.findLocationByName(locationName);
				// if location doesn't exist, add it
				if (loc == null){
					loc = new Location(locationName, 
							tp.getLatStr(),
							tp.getLonStr(),
							tp.getEleStr());
					vLoc.add(loc);
					lLoc.add(loc);
				}	
			}
		} // end for

		// determine time at each location
		for (int i = 0; i<track.getSize(); i++){
			TrackPoint tp = track.getTrackPoint(i);
			locationName = Integer.toString(tp.getLocationNumber());
			locationName = locationName.trim();
			if (locationName.equalsIgnoreCase("-1"))
				continue;

			loc = vLoc.findLocationByName(locationName);
			if (loc == null) {
				// location doesn't exist yet -- add to list
				loc = new Location(locationName, 
						tp.getLatStr(),
						tp.getLonStr(),
						tp.getEleStr());
				loc.setFirstArrival(tp.getDateTimeStr());
				loc.setLastDeparture(tp.getDateTimeStr());
				vLoc.add(loc);
				lLoc.add(loc);
			}
			else {
				// location exists - update first/last arrival times
				if (loc.getFirstArrival().equalsIgnoreCase(Location.UNKNOWN))
					loc.setFirstArrival(tp.getDateTimeStr());
				loc.setLastDeparture(tp.getDateTimeStr());
			}
			// update time at location
			loc.addTimeAtLocation(tp.getDuration());
		}

		if (vLoc.size() == 0){
			eventLogger.logWarning("KMLexport.outputLocations - No locations to display.");
			return;
		}

		// Built sorted list of durations
		ArrayList<Double> list = new ArrayList<Double>();	
		for (int i = 0; i < vLoc.size(); i++){
			loc = vLoc.get(i);
			list.add(new Double(loc.getTimeAtLocation()));
		}	
		Collections.sort(list);

		if (list.size() < numclass)
			numclass = list.size();

		eventLogger.logEvent("KMLexport.outputLocations - number of locations:" + list.size());
		eventLogger.logEvent("KMLexport.outputLocations - number of classification groups:" + numclass);

		// get Jenks breaks
		int breaks[] = JenksBreaks.getJenksBreaks(list, numclass);

		if (breaks == null){
			eventLogger.logError("KMLexport.outputLocations - JenksBreaks returned null. Can not display locations.");
			// TODO - also occurs when all locations have the same duration
			return;
		}

		// get cutoff values
		Double cutoffs[] = new Double[numclass];
		for (int i = 0; i < breaks.length; i++){
			int j = breaks[i];
			if (j < 0)
				cutoffs[i] = -1.0;
			else
				cutoffs[i] = list.get(j);
		}

		// create Location Styles
		for (int i = 0; i < numclass; i++){
			gen.writeStylePoint("location"+i, "CCAAAAAA", size);    // gray circles
			size = size + 0.5;
		}

		// find the length of the longest location name
		int maxLen = 0;
		for (int i = 0; i < vLoc.size(); i++){
			loc = vLoc.get(i);
			if (loc.name.length() > maxLen)
				maxLen = loc.name.length();
		}

		// pad location name where needed
		for (int i=0; i<vLoc.size(); i++){
			loc = vLoc.get(i);
			int difference = maxLen - loc.name.length();
			while (difference-- > 0)
				loc.name = " " + loc.name;
		}

		// sort location list
		Collections.sort(lLoc, new Comparator<Location>()
				{
			public int compare(Location l1, Location l2){
				return l1.name.compareTo(l2.name);
			}
				});

		// output locations in Location List
		gen.addFolder("Locations", false);
		for (int i =0; i<lLoc.size(); i++){
			loc = lLoc.get(i);
			int tal = (int) loc.getTimeAtLocation();	// in seconds
			int hours = tal/3600;
			int min = (tal-(hours*3600))/60;
			int sec = tal-((hours*3600)+(min*60));		
			loc.desc = "Duration = " + hours+":"+min+":"+sec +
					"<BR>First arrival = " + loc.getFirstArrival() +
					"<BR>Last departure = " + loc.getLastDeparture();

			int n = 0;
			// determine classification
			for (n = 0; n < cutoffs.length; n++){
				if (tal <= cutoffs[n])
					break;
			}		
			addLocation(loc, "location"+n, false);
		} // end for
		gen.closeFolder();
	}


	/*
	 * Output All Points
	 */

	private void outputAll(GPSTrack track){
		gen.addFolder("All Fixes", false);
		for (int i =0; i<track.getSize(); i++){
			TrackPoint tp = track.getTrackPoint(i);
			addWayPoint(tp, "yellow45", false);
		} // end for;
		gen.closeFolder();
	}

	/* 
	 * Output by Speed
	 */

	private void outputBySpeed(GPSTrack track){
		String style, dateTime;
		int speed = -1;
		TrackPoint tp;
		gen.addFolder("By Speed", false);
		for (int i =0; i<track.getSize(); i++){
			tp = track.getTrackPoint(i);
			dateTime = tp.getDateTimeStr();  // dateTime in form of mm-dd-yyyy hh:mm:ss  
			speed = (int)Math.round(tp.getSpeed());

			tp.name = "";
			tp.desc = dateTime +  " SP: " +Integer.toString(speed);

			if (speed > 15)
				style = "red";						
			else
				if (speed > 6)
					style = "orange";				
				else
					if (speed > 3)
						style = "yellow";		
					else
						if (speed > 1)
							style = "green";		
						else
							if (speed >= 0)
								style = "blue";		
							else
								style = "gray";											
			addWayPoint(tp, style, false);

		} // end for;
		gen.closeFolder();
	}

	/*
	 * Output by Trips
	 */

	private void outputByTrips(GPSTrack track){
		int tripType, mode;
		int duration, tripNum;
		boolean inTrip = false;
		gen.addFolder("By Trips", false);

		for (int i =0; i<track.getSize(); i++){
			TrackPoint tp = track.getTrackPoint(i);
			tripNum = tp.getTripNumber();
			mode = tp.getTripMode();
			tripType = tp.getTripType();

			try {
				duration = tp.getDuration();
				if (duration == 0)
					continue;				// don't plot zero durations
				if (tripNum == 0)
					continue;				// don't plot non-trips
			}
			catch (Exception ex){
				continue;			// ignore points containing errors
			}

			// check if MidPoints are to be plotted
			if (tripType == TrackPoint.MIDPOINT){
				if (!includeMidPoints)
					continue;				// no - skip it
			}


			if (displayTripNumber)
				tp.name = Integer.toString(tripNum);
			else {
				// TODO: display trip numbers on start/end points?
				tp.name ="";	
			}
			tp.desc = tp.getDateTimeStr() + 
					"<br>Trip:" + tripNum + 
					"<br>Mode:" + mode +
					"<br>SP:" + (int) Math.round(tp.getSpeed()) +
					"<br>Dur:" + duration + 
					"<br>Loc:" + tp.getLocationNumber() + 
					"<br>Trip Type:" + tripType;

			// allow for startPoint and EndPoint at same location (start of one trip - end of another)

			if (tripType == TrackPoint.ENDPOINT){
				if (inTrip){
					addWayPoint(tp, "red", false);		// end
					gen.closeFolder();
					inTrip = false;
				}
			}

			if (tripType == TrackPoint.STARTPOINT){
				if (!inTrip){	
					if (tripType == TrackPoint.ENDPOINT){
						// need to update trip number and description
						tripNum++;
						tp.desc = tp.getDateTimeStr() + 
								"<br>Trip:" + tripNum + 
								"<br>Mode:" + mode +
								"<br>SP:" + (int) Math.round(tp.getSpeed()) +
								"<br>Dur:" + duration + 
								"<br>Loc:" + tp.getLocationNumber() + 
								"<br>Trip Type:" + tripType;
					}

					gen.addFolder(tripNum + " - " + tp.getDateTimeStr(), false);
					addWayPoint(tp, "green", false);								// start of trip
					inTrip = true;

				}
			}
			else {
				if (tripType == TrackPoint.MIDPOINT)
					addWayPoint(tp, "yellow", false);		// moving
				else 
					if (tripType == TrackPoint.CHANGEMODE)
						addWayPoint(tp, "cyan", false);			// speed change detected			
					else {
						if (tripType == TrackPoint.PAUSEPOINT)							
							addWayPoint(tp, "orange", false);		// pause
					}
			}
		} // end for
		if (inTrip)
			gen.closeFolder();	// close trip when endPoint was missing
		gen.closeFolder();  	// close by trips
	}

	/*
	 * Output by Stationary
	 */

	private void outputByStationary(GPSTrack track){
		String date ="", dateTime ="";
		String prevDate = "";
		WayPoint wp = new WayPoint();
		gen.addFolder("By Stationary", false);
		for (int i =0; i<track.getSize(); i++){
			TrackPoint tp = track.getTrackPoint(i);
			dateTime = tp.getDateTimeStr();
			date = dateTime.substring(0,10);
			if (!prevDate.equalsIgnoreCase(date)){
				if (prevDate.length() != 0)
					gen.closeFolder();
				gen.addFolder(date);
				prevDate = date;
			}

			if (tp.getTripType() == TrackPoint.STATIONARY){
				if (tp.getFixType() != TrackPoint.INSERTED){
					wp.name ="";	
					wp.desc = dateTime + 
							"<br>Fix Type:" + tp.getFixType() +
							"<br>Dur:" + tp.getDuration() + 
							"  Loc:" + tp.getLocationNumber(); 

					addWayPoint(tp, "blue", false);
				} // end if (!inserted
			} // end if (stationary
		} // end for
		gen.closeFolder();		// close by date
		gen.closeFolder();  	// close by Stationary
	}


	/*
	 * Output by Tracks
	 */

	private void outputByTracks(GPSTrack track){
		int tripType, mode, currentMode = -1;
		String trackStyle;
		Double lat, lon;
		boolean inTrip = false;

		gen.addFolder("Show Tracks", false);
		for (int i =0; i<track.getSize(); i++){
			TrackPoint tp = track.getTrackPoint(i);
			lat = tp.getLat();
			lon = tp.getLon();
			tripType = tp.getTripType();
			mode = tp.getTripMode();

			if (lat == WayPoint.UNKNOWNCOORDINATES && lon == WayPoint.UNKNOWNCOORDINATES)
				continue;

			if (tp.getDuration() == 0)
				continue;				// don't plot zero durations
			if (tp.getTripNumber() <= 0)
				continue;				// don't plot non trips

			if (tripType == TrackPoint.ENDPOINT){
				if (inTrip){
					// close track on endPoint
					gen.addTrackCoords(lat, lon, 0); // elevation set to zero 
					gen.writeTrackCoords(true);
					gen.writeTrack();
					gen.closeTrack();
					inTrip = false;
				}
				else{
					eventLogger.logWarning("KMLexport.outputByTrack - got Endpoint when not in trip - tp: " + tp.toString());
					continue;
				}
			} // end if == ENDPOINT

			else {

				if (tripType == TrackPoint.STARTPOINT){
					if (inTrip){
						eventLogger.logWarning("KMLexport.outputByTrack - got StartPoint while in trip - tp: " + tp.toString());
						gen.writeTrackCoords(true);
						gen.writeTrack();
						gen.closeTrack();
					}
					inTrip = true;
					// color code by mode of transportation
					currentMode = mode;
					if (mode == TrackPoint.MOT_VEHICLE)
						trackStyle = "trackAuto";
					else 
						if (mode == TrackPoint.MOT_BICYCLE)
							trackStyle = "trackBicycle";
						else 
							trackStyle = "trackWalking";

					gen.addTrack(tp.getTripNumber() + " - " + tp.getDateTimeStr(), trackStyle, false);
					gen.addTrackCoords(lat, lon, 0); 					// elevation set to zero 
				}	// end if == STARTPOINT

				else {
					// if not startPoint or endPoint
					// check for change of transportation mode
					if (mode != currentMode){
						// color code by mode of transportation
						currentMode = mode;
						if (mode == TrackPoint.MOT_VEHICLE)
							trackStyle = "trackAuto";
						else 
							if (mode == TrackPoint.MOT_BICYCLE)
								trackStyle = "trackBicycle";
							else 
								trackStyle = "trackWalking";
						// on mode change - close previous track
						gen.writeTrackCoords(true);
						gen.writeTrack();
						gen.closeTrack();
						// add new track
						gen.addTrack(tp.getTripNumber() + " - " + tp.getDateTimeStr(), trackStyle, false);
						gen.addTrackCoords(lat, lon, 0); 			// elevation set to zero 
					} // end if (change of MOT)
					gen.addTrackCoords(lat, lon, 0); // elevation set to zero 
				}
			}
		} // end for
		if (inTrip){
			gen.writeTrackCoords(true);
			gen.writeTrack();
			gen.closeTrack();
		}
		gen.closeFolder();
	}




			/*
			 * Experimental - Output Path by Time
			 */

			private void outputPathByTime(GPSTrack track){
				String hour, dateTime;
				Double lat, lon;
				gen.addFolder("By Time", false);
				gen.addTrack("Temporal Timeline", "trackStyleAuto", false);
				for (int i =0; i<track.getSize(); i++){
					TrackPoint tp = track.getTrackPoint(i);
					lat = tp.getLat();
					lon = tp.getLon();

					if (lat == WayPoint.UNKNOWNCOORDINATES && lon == WayPoint.UNKNOWNCOORDINATES)
						continue;
					else {

						if (tp.getDuration() == 0)
							continue;				// don't plot zero durations
						if (tp.getTripNumber() <= 0)
							continue;				// don't plot non trips

						dateTime = tp.getDateTimeStr();  // dateTime in form of mm-dd-yyyy hh:mm:ss
						hour = dateTime.substring(11, 13);   

						// use elevation to simulate time dimension
						int timeDimension = 50 * Integer.parseInt(hour);
						gen.addTrackCoords(lat, lon, timeDimension);
					} // end else
				} // end for

				gen.writeTrackCoords(false);
				gen.writeTrack();
				gen.closeTrack();
				gen.closeFolder();
			}

			/* 
			 * Utility Functions
			 */

			private void addWayPoint(TrackPoint tp, String style, boolean visible){
				if ((tp.getLon() == WayPoint.UNKNOWNCOORDINATES) && (tp.getLon() == WayPoint.UNKNOWNCOORDINATES))
					return;			// ignore unknown fix

				if (filterRedundantPoints){
					if (tp.getTripType() == TrackPoint.STATIONARY)	// only filter stationary points
						if (previousLat.equalsIgnoreCase(tp.getLatStr()))
							if (previousLon.equalsIgnoreCase(tp.getLonStr()))
								return;
					previousLat = tp.getLatStr();
					previousLon = tp.getLonStr();
				}
				gen.addTrackPoint(tp, style, visible);
			}

			private void addLocation(Location loc, String style, boolean visible){
				gen.addPlacemark(loc, style, visible, loc.name);
			}

			/*
			 * For debugging

public static void main(String[] args) {
	try {
		String outFile = "logs/eclipse_debug.kml";
		PrintWriter pw = new PrintWriter(outFile);
		KMLexport kml = new KMLexport(pw);
		EventLogger eventLogger = new EventLogger();
		TestCalculationOneResultSetGenerator gen = new TestCalculationOneResultSetGenerator(eventLogger);
		RowList rowList = gen.generate(null);  // null - prompts for input file name
		kml.export(rowList);
		pw.close();
	} catch (Exception e) {
		System.out.println(e);
	}
}
			 */

		}