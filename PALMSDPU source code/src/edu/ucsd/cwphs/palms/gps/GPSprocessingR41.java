package edu.ucsd.cwphs.palms.gps;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import edu.ucsd.cwphs.palms.util.EventLogger;

/*
 * Version	Date		Change
 * 1.0.3	3/5/10		Add cluster_center tag to location centers
 * 1.0.4	3/9/10		Remove leading +signs in TrackPoint.removeType
 * 1.0.5				Trap fixes marked as indoors to locations
 * 1.0.6	3/12/10		Remove static references; fixed divide by zero bug where nsatView = 0
 * 1.0.7	4/15/10		Moved "magic numbers" to parameter block; 
 * 						Fixed trip detection to use duration, not # of samples
 * 						Added option to filter lone fixes
 * 1.0.8	6/15/10		Fix bug in detectOutdoors logic
 * 1.0.9	6/17/10		Temp Kludge for detectTrips between Locations 
 * 1.0.10	7/22/10		Fix epoch issue in trip detect
 * 						Fix trip mode speed cutoffs to agree with documentation; fixed bug in else statement (maxSpeed)
 * 						Trip detection - when moving, declare zero speed as pause points
 * 1.0.11	7/30/10		Reconsider trips -- drop trips where total distance < gps_trip_mimlength
 * 1.0.12	12/20/10	Perform alignment last
 * 1.0.13	2/11/11		Changed elevationDelta filtering logic to use last Valid elevation
 * 1.0.14	03/01/11	Fixed bug in TrackPoint.calculateSnrView
 * 1.0.15	03/04/11	Changed way pauses are calculated -- reclassify pauses < gps_trip_min_pause as midpoints
 * 						Don't mark last fixes as start of trips (occurs with large gaps in GPS coverage)
 * 						Changed way locations are calculated -- give priority to locations marked as lastFix and trip ends
 * 1.0.16	03/09/11	Fixed index out of range bug when check for forward / backwards movement on last element
 * 						Added timestamp of trip end when removing trips 
 * 1.0.17	04/01/11	Marked changes in speed
 * 1.0.18	04/05/11	Classify mode of transportation based on speed between pauses
 * 1.0.19	04/22/11	When classifying trip legs, allow for pausePoint followed by endPoint
 * 						When LOS occurs during pause, set endPoint, remove pause (corrects above problem)
 * 1.2.0	05/03/11	When calculating pause duration, include time at original pause point (pauseTp.duration)
 * 						Special case in processPausePoints -- start == end
 * 1.2.1	05/10/11	Trips occurring 90% indoors are removed
 * 1.2.2	06/10/11	Fixed bug were pause duration was double counted
 * 1.2.3	08/16/11	Remove short trips caused by short legs. PALMS-320
 * 						consider distance covered at max speed when marking start of trip
 * 1.2.4	09/29/11	Add code to use common location list for all participants
 * 						Remove call to alignToInerval since it's called by CalculationOneB
 *  					Record stats in log file on percentage of invalid points
 * 3.0.0	10/06/11	Changed logic and parameter blocks --remove trips indoors and within one location
 * 						Estimate mode of transportation based on cutoff "bin" with most entries 
 * 						Compute acceleration in meters/second -- display >=7
 * 						Trim indoor points from start and end of trips
 * 						Add option to capture points that are part of trips	 			
 * 			11/01/11	Classify mode by percentile of speed
   3.0.1	01/28/12	Fixed bug improperly marking start of trips
 * 3.1.0	12/12/11	Experimental code -- based on certaintity of segments -- not fully developed
 * 						(wait for R4?)
 * 4.0.0	03/12/12	Add separate columns for fix type, iov, trip type, cluster flag
 * 						No longer remove "redundant" points; only use parameter for forward-backwards filtering
 * 						Improve algorithm for filtering on Max Distance traveled over short time frames
 * 						Add warning when more than 50% of the points are considered invalid and filtered
 * 						Trap logic changed -- trap if (indoors OR not_in_trip OR pb.trap_trips)
 * 						Add options to trap indoors / outdoors / part of trip
 * 						Fix bug in which bearing values were not be set properly for firstFixes
 * 						Remove locations where timeAtLocation < pb.gps_cluster_min_duration
 * 	4.0.1	06/04/12	Add code to merge adjacent segments of same mode type
 *  4.0.2	06/20/12	remove call to faulty code that removed locations without trips
 *  					(forgot to update version below)
 *  4.0.3	08/15/13	fixed PALMS-498 -- removing trips within location -1
 *  					display length of removed trip & warning on removal of longer trips
 *  					trimTrims - don't remove trips if allowed to be 100% indoors
 *  					comment out max distance filter
 *  4.1.0	08/22/12	add option to not reconsider trip after segmenting
 *  4.1.1	09/11/12	fixed bug in RemoveLocationsWithoutTrips -- see PALMS-526
 * 
 */

public class GPSprocessingR41 {

	private static final String VERSION = "4.1.1   11 Sep 2012";
	LocationList commonLocations = new LocationList();
	private int GPSepoch = 0;

	// stat counters
	private int filtered_speed = 0;
	private int filtered_max_distance = 0;
	private int filtered_max_acceleration = 0;
	private int filtered_elevation = 0;
	private int filtered_lonefix = 0;
	private int points_processed = 0;
	private int filtered_backforth = 0;
	private int first_fixes = 0;
	private int last_fixes = 0;
	private int lone_fixes = 0;
	private int raw_fixes = 0;


	/* 
	 * No common locations unless this method is called
	 */
	public void setLocationList(LocationList list){
		commonLocations = list;
	}

	/*
	 * Call this method to retrieve list of locations
	 */
	public LocationList getLocationList(){
		return commonLocations;
	}

	public LocationList clearLocationList(){
		commonLocations = new LocationList();
		return commonLocations;
	}

	public String getVersion(){
		return "GPSprocessingR4 - Version " + VERSION;
	}

	public void clearStats(){
		filtered_speed = 0;
		filtered_max_distance = 0;
		filtered_max_acceleration = 0;
		filtered_elevation = 0;
		filtered_lonefix = 0;
		filtered_backforth = 0;
		points_processed = 0;
		first_fixes = 0;
		last_fixes = 0;
		lone_fixes = 0;
		raw_fixes = 0;
	}

	public String getStatsStr(){
		String s = "Processed: "+ points_processed + "  Filtered Speed: "+ filtered_speed + "  ElevationDelta: " + filtered_elevation + 
		"  MaxDistance:" + filtered_max_distance + 
		"  MaxAcceleration:" + filtered_max_acceleration + "  BackForth:" + filtered_backforth + 
		"  LoneFix:" + filtered_lonefix;

		int totalInvalid = filtered_speed + filtered_elevation + filtered_max_distance + 
		//						filtered_max_acceleration 						// max acceleration counted but not filtered (yet)
		+ filtered_backforth + filtered_lonefix;
		int percentInvalid = 0;
		if (points_processed != 0){
			percentInvalid = (totalInvalid*100)/points_processed;
		}

		s = s + "\n     Invalid Points:" + totalInvalid + "  " + percentInvalid +"%";
		if (percentInvalid >= 50)
			s = s + "\n     *** WARNING *** Verify your filtering paramters.";
		return s;
	}

	/* 
	 * Process
	 */

	public boolean process(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		boolean rc = true;
		LocationList newLocations;

		if (vtp.size() < 3){
			EventLogger.logWarning("GPSprocessing - too few points to process");
			return false;
		}
		clearStats();
		GPSepoch = getEpoch(vtp);

		preProcess(pb, vtp);
		if (pb.gps_indoors_detect)
			markOutdoors(pb, vtp);

		if (pb.gps_filter_invalid != 2) {
			filter(pb, vtp);						
			EventLogger.logEvent("GPSprocessing.process - After filtering - " + getStatsStr());
			EventLogger.logEvent("GPSprocessing.process - After filtering - Points remaining:" + vtp.size());
		}

		// check for valid speed percentile
		if (pb.gps_speed_percentile > 100){
			EventLogger.logWarning("GPSprocessing.process -- Speed percentile value > 100; reseting to 90");
			pb.gps_speed_percentile = 90;
		}

		if (pb.gps_cluster_before_trips){

			// detect trip starts / ends
			detectTrips(pb, vtp);

			// TODO - need better clustering - current code doesn't detect clusters that
			//     don't include first/last/start/end fixes

			newLocations = clusterByFix(pb, vtp);	 	// determine locations of interest
			addLocations(newLocations, commonLocations.size()+1);  // add new locations to common list

			setLocation(pb, vtp, commonLocations);	// sets location number in trackpoint
			if (pb.gps_cluster_trap_indoors || pb.gps_cluster_trap_outdoors)
				locationCapture(pb, vtp, commonLocations);  // sets trackpoint coordinates to that of locations

			// then detect trips between locations
			detectTripsBetweenLocations(pb, vtp);	
			reconsiderTrips(pb, vtp, commonLocations);			// remove short & indoor trips	
			classifyAndNumberTrips(pb, vtp);
			
			if (pb.gps_trip_reconsider_segments){
				EventLogger.logEvent("GPSprocessing.process - Reconsider trips after segmenting and classification");
				reconsiderTrips(pb, vtp, commonLocations);			// reconsider again to evaluate trips from short legs
			}
			
			numberTrips(vtp);							// renumber any removed trips
		}
		else {
			// Trip detect first, then location detect
			detectTrips(pb, vtp);
			if (pb.gps_trip_trim_indoor_points)
				trimTrips(pb, vtp);			

			// then location detect
			newLocations = clusterByFix(pb, vtp);	 	// determine locations of interest
			int firstLocationNumber = commonLocations.size()+1;
			addLocations(newLocations, firstLocationNumber);  // add new locations to common list

//			locList = clusterByGrid(pb, vtp);
//			EventLogger.logEvent("GPSProcessing - locList = \n" + locList.toString());
//			EventLogger.logEvent("GPSProcessing - locList2 = \n" + locList2.toString());

			// TESTING -- add additional locations to list

//			for (int i=0; i < locList2.size(); i++)
//			locList.add(locList2.get(i));
//			EventLogger.logEvent("GPSProcessing - locList = \n" + locList.toString());

			setLocation(pb, vtp, commonLocations);	// sets location number in trackpoint			

			reconsiderTrips(pb, vtp, commonLocations);	// remove short trips and trips solely within one location	
			classifyAndNumberTrips(pb, vtp);
			
			if (pb.gps_trip_reconsider_segments) {
				EventLogger.logEvent("GPSprocessing.process - Reconsider trips after segmenting and classification");
				reconsiderTrips(pb, vtp, commonLocations);			// reconsider again to evaluate trips from short legs
			}
			numberTrips(vtp);							// renumber any removed trips

			// Remove locations wo trips (if not allowed) 
			if (!pb.gps_cluster_allow_wo_trips)
				commonLocations = removeLocationsWithoutTrips(pb, vtp, commonLocations, firstLocationNumber);

			if (pb.gps_cluster_trap_indoors || pb.gps_cluster_trap_outdoors)
				locationCapture(pb, vtp, commonLocations);  // sets trackpoint coordinates to that of locations

		}

		averageSpeedElevation(pb, vtp);

		return rc;
	}

	/*
	 * Align to interval is involked by calculation
	 */

	public boolean alignToInterval(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, int epoch){
		int tpDropped = 0;
		TrackPoint tp;
		long difference, msec, previousMsec = 0;
		int index = 0;
		int epochInMsec = epoch * 1000;				// compute value once
		while (index < vtp.size()) {
			Date ts;
			tp = vtp.get(index);
			ts = tp.getDateTime();
			msec = ts.getTime();
			difference = msec%(epochInMsec);
			msec = msec - difference;					// results in going back in time
			// TODO: should go forward
			// to round forward
			// difference = (epochInMsec) - (msec%(epochInMsec));
			// msec = msec + difference;

			ts.setTime(msec);
			tp.setDateTime(ts);

			// if timestamp is same as previous, remove it
			if (msec == previousMsec){
				index = alignTrackPoint(vtp, index);
				tpDropped++;
			}
			else {
				previousMsec = msec;
				index++;
			}
		} // end while

		EventLogger.logEvent("GPSprocessing.alignToInterval - Interval (seconds):" + epoch + 
				"  Points dropped:" + tpDropped + "  remaining:" + vtp.size());

		if (vtp.size() < 3)
			EventLogger.logWarning("GPSprocessing.alignToInterval - too many points dropped!  Try decreasing interval.");

		return true;
	}

	/*
	 * PRIVATE methods follow
	 */

	private int getEpoch(ArrayList<TrackPoint> vtp){
		TrackPoint tp;
		long first, second;
		if (vtp.size() < 2){
			EventLogger.logWarning("GPSprocessing.getEpoch -- short vector");
			return -1;
		}

		tp = vtp.get(0);
		first = tp.getDateTime().getTime();
		second = vtp.get(1).getDateTime().getTime();
		int epoch =  (int) (second-first)/1000;
		EventLogger.logEvent("GPSprocessing.getEpoch -- Epoch calculated as: " + epoch);
		return epoch;
	}

	/* 
	 * Preprocess
	 * 
	 * sets type as FirstFix, LastFix, LoneFix or Raw
	 * computes duration, distance, elevation delta and speed between fixes
	 */

	private boolean preProcess(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		boolean rc = true;
		int total_fixes = 0;
		int avgSNRused = 0;
		TrackPoint previousTp, currentTp;
		int duration;
		int distance;

		previousTp = vtp.get(0);				// get 1st element in vector
		previousTp.setFixType(TrackPoint.FIRSTFIX);
		previousTp.setElevationDelta(0);
		previousTp.setSpeed(0);
		previousTp.setDistance(0);
		previousTp.setBearing(-1); 			// fjr 3/8/12
		previousTp.setBearingDelta(0);		// fjr 3/8/12
		first_fixes++;

		previousTp.calculateSnrUsed();
		previousTp.calculateSnrView();
		if (previousTp.getSatsUsed()> 0)
			avgSNRused = previousTp.getSnrUsed() / previousTp.getSatsUsed();
		else
			avgSNRused = 0;
		previousTp.setAvgSnrUsed(avgSNRused);

		// loop through the rest
		for (int index = 1; index < vtp.size(); index++){
			currentTp = vtp.get(index);

			currentTp.calculateSnrUsed();
			currentTp.calculateSnrView();	
			if (currentTp.getSatsUsed()> 0)
				avgSNRused = currentTp.getSnrUsed() / currentTp.getSatsUsed();
			else
				avgSNRused = 0;
			currentTp.setAvgSnrUsed(avgSNRused);

			duration = currentTp.timeBetween(previousTp);
			distance = currentTp.calDistance(previousTp);
			previousTp.setDuration(duration);					// set time at previous location

			// Does gap in time indicates loss of signal ?
			if (duration > pb.gps_filter_max_los) {
				// yes - is previous marked as first fix ?
				if (previousTp.isFirstFix()) {
					previousTp.setFixType(TrackPoint.LONEFIX);			// yes - mark as loner
					lone_fixes++;
					first_fixes--;
				}
				else { 
					// is previous marked as VALID?   // TODO: is this correct?  (how could this test fail?)
					if (previousTp.isValidFix()){
						previousTp.setFixType(TrackPoint.LASTFIX);			// yes, mark as last fix
						last_fixes++;
						raw_fixes--;
					}
					else
						EventLogger.logWarning("GPSProcessing.preProcess - TP: " + previousTp.getDateTimeStr() + 
								" has unexpected fixType value = " + previousTp.getFixType());    // fjr 3/8/12
				}

				currentTp.setFixType(TrackPoint.FIRSTFIX);
				first_fixes++;
				currentTp.setElevationDelta(0);
				currentTp.setSpeed(0);
				currentTp.setDistance(0);
				currentTp.setBearing(-1);				// fjr 3/8/12
				currentTp.setBearingDelta(0);			// fjr 3/8/12
			}

			else {
				// no loss of signal detected
				currentTp.setFixType(TrackPoint.VALID);	
				raw_fixes++;
				currentTp.setElevationDelta(currentTp.elevationBetween(previousTp));
				currentTp.setSpeed(currentTp.speedBetween(previousTp));
				currentTp.setDistance(distance);
				if (distance > 0){
					currentTp.setBearing(currentTp.calBearing(previousTp));
					currentTp.setBearingDelta(currentTp.bearingChange(previousTp));
				}
				else {
					currentTp.setBearing(previousTp.getBearing()); // didn't move, use previous bearing
					currentTp.setBearingDelta(0);
				}
			}
			previousTp = currentTp;
		} // end for

		// end of loop -- set duration at last point as 0
		previousTp.setDuration(0);							// 
		// was last point marked FIRST FIX?
		if (previousTp.isFirstFix()) {
			previousTp.setFixType(TrackPoint.LONEFIX);			// yes - mark as loner
			lone_fixes++;
			first_fixes--;
		}
		else { 
			// is previous marked as valid?
			if (previousTp.isValidFix()){
				previousTp.setFixType(TrackPoint.LASTFIX);			// yes, mark as last fix
				last_fixes++;
				raw_fixes--;
			}
		}
		total_fixes = first_fixes + last_fixes + lone_fixes + raw_fixes;
		EventLogger.logEvent("GPSprocessing.preProcess - First Fixes:"+first_fixes + " Last Fixes:" + last_fixes + " Lone Fixes:"+ lone_fixes +
				" Raw Fixes:"+ raw_fixes + " Total:" + total_fixes);

		if (total_fixes != vtp.size()){
			EventLogger.logError("GPSprocessiong.preProcess -- Total NOT equal to size of " + vtp.size());
			rc = false;
		}
		return rc;
	}

	/*
	 * markOutdoors -- Indoor / Outdoor detection
	 */

	private void markOutdoors(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		TrackPoint tp;
		int counter_snr = 0, counter_ratio = 0, counter_snr_indoors = 0, counter_ratio_indoors = 0;
		for (int index=0; index < vtp.size(); index++){
			tp = vtp.get(index);
			
			
			// attempt to use viewed signal strength if available
			int snr = tp.calculateSnrView();
			if (snr > 0){
				if (snr >= pb.gps_indoors_max_snr){
					tp.setOutdoors();
					counter_snr++;
				}
				else {
					tp.setIndoors();
					counter_snr_indoors++;	
				}
			}
			else {
				// otherwise attempt to use sat ratio
				int nsatView = tp.getSatsInView();
				if (nsatView > 1){
					int ratio = (tp.getSatsUsed()*100) / nsatView;		// compute percentage as integer
					if (ratio > pb.gps_indoors_sat_ratio) {
						tp.setOutdoors();
						counter_ratio++;
					}
					else {
						tp.setIndoors();
						counter_ratio_indoors++;	
					}
				}
			}	
			
		} // end for
		int counter_total = counter_snr+counter_ratio+counter_snr_indoors+counter_ratio;
		if (counter_total > 0){
			EventLogger.logEvent("GPSprocessing.markOutdoors - Outdoors based on SNR:" + counter_snr + 
					" Based on sat_ratio:" + counter_ratio);
			EventLogger.logEvent("GPSprocessing.markOutdoors -  Indoors based on SNR:" + counter_snr_indoors + 
					" Based on sat_ratio:" + counter_ratio_indoors);
//			EventLogger.logEvent("GPSprocessing.markOutdoors - In Vehicle based on speed:" + counter_inVehicle); 
		}
		else {
			EventLogger.logEvent("GPSprocessing.markOutdoors -- Could not mark - no satellite signal information collected.");
			pb.gps_indoors_detect = false;		// set false to bypass later processing
		}
		return;
	}

	/*
	 * Filter
	 */

	private boolean filter(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		boolean rc = true;
		TrackPoint previousTp, currentTp, nextTp, lastValidTp;	
		boolean removeFlag = true;

		points_processed = vtp.size();
		currentTp = vtp.get(0);
		lastValidTp = currentTp;
		double previousGoodElevation = currentTp.getEle();		// store elevation
		double elevationDelta = 0.0;

		int index = 1;					// skip over 1st element		
		while (index < vtp.size()) {
			previousTp = currentTp;
			currentTp = vtp.get(index);

			if (pb.gps_filter_lonefixes) {
				if (currentTp.isLoneFix()){
					index = deleteTrackPoint(vtp, index, TrackPoint.FILTERED_LONEFIX, removeFlag, false);
					filtered_lonefix++;
					continue;
				}
			}

			// do not filter first and last fixes
			if (currentTp.isFirstFix()){
				previousGoodElevation = currentTp.ele;
				lastValidTp = currentTp;
				index++;
				continue;
			}

			if (currentTp.isLastFix()){
				lastValidTp = currentTp;
				index++;
				continue;
			}

			// check for invalid speed
			if (currentTp.getSpeed() > pb.gps_filter_max_speed){
				index = deleteTrackPoint(vtp, index, TrackPoint.FILTERED_MAXSPEED, removeFlag, false);
				filtered_speed++;
				continue;
			}

			// TODO: check for invalid acceleration ??

			double deltaSpeed = Math.abs(previousTp.getSpeed() - currentTp.getSpeed());
			double deltaMPS = deltaSpeed * .277778;				// convert to meters / sec
			int deltaTime = currentTp.timeBetween(previousTp);
			if (deltaTime != 0){
				int acceleration = (int)deltaMPS / deltaTime;
				if (acceleration >= 7){							// equivalent to a Ferrari    typical car 3-4						
					filtered_max_acceleration++;
					EventLogger.logWarning("GPSproccessing.filter - Acceleration of " + acceleration + " m/sec at " + currentTp.getDateTimeStr());
					//		index = deleteTrackPoint(vtp, index, TrackPoint.FILTERED_MINDISTANCE, removeFlag, false);
					//		continue;
				}
			}

			// check for invalid elevationDelta
			// TODO: see PALMS-437 (too many points can be filtered when value is set too low)
			elevationDelta = previousGoodElevation - currentTp.ele;
			if (Math.abs(elevationDelta) > pb.gps_filter_max_elevation_delta){
				index = deleteTrackPoint(vtp, index, TrackPoint.FILTERED_MAXELEVATION, removeFlag, false);
				filtered_elevation++;
				continue;
			}
			previousGoodElevation = currentTp.ele;

			// check for invalid distance traveled over short interval  
			// (how does this differ from max speed?
			/*
			 * Same as max speed - remove

			// TODO: Accommodate for large changes that can occur in temporary lost of signal (when LOS not declared)
			// somewhat analogous to invalid elevation delta issue

			if (currentTp.distanceBetween(lastValidTp)> pb.gps_filter_max_distance_delta){   // if distance traveled > max
				EventLogger.logWarning("GPSprocessing.filter - Max distance of " + currentTp.distanceBetween(lastValidTp) + 
						" at " + currentTp.dateTimeStr);
				if (currentTp.getDuration() < pb.interval){
					EventLogger.logWarning("GPSprocessing.filter - removed");
					filtered_max_distance++;
					index = deleteTrackPoint(vtp, index, TrackPoint.FILTERED_MINDISTANCE, removeFlag, false);
					continue;					
				}
			}		
			 */
			
			/*
			// check for redundant point -- are points close together?
			if (currentTp.getDistance() < pb.gps_filter_min_distance_delta){   // if distance traveled < min
				filtered_min_distance++;
				index = deleteTrackPoint(vtp, index, TrackPoint.FILTERED_MINDISTANCE, removeFlag, false);
				continue;
			}
			 */

			// Filter Back and Forth movements
			// if (not last element) AND (distance traveled > min distance), then             fjr 2/24/12
			// check for forwards/backwards movement
			if (index+1 < vtp.size() && (currentTp.getDistance() >  pb.gps_filter_min_distance_delta)){
				nextTp = vtp.get(index+1);     // caution -- will be out of range on last element, thus the above check
				double distance = previousTp.distanceBetween(nextTp);
				if (distance < pb.gps_filter_min_distance_delta){
					filtered_backforth++;
					index = deleteTrackPoint(vtp, index, TrackPoint.FILTERED_FORWARDBACKWARDS, removeFlag, false);
					continue;
				}
			}

			// AOK, leave in place and move on to next trackpoint
			lastValidTp = currentTp;
			index++;
		} // end while
		return rc;
	}

	/*
	 * Location Detection Alogrithms
	 * 
	 */
	// TODO: JCA doesn't work as well as desired -- can't seem to find the optimal number of clusters
	// TODO: Try using a version of DBSCAN 

	private LocationList clusterByFix(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		TrackPoint tp;
		Location loc;
		int counter = 0;
		int priority = -1;
		LocationList newLocations = new LocationList();

		EventLogger.logEvent("GPSprocessing.clusterByFix - Finding track points with duration > " + pb.gps_cluster_min_duration);
		if (pb.gps_cluster_include_pauses)
			EventLogger.logEvent("GPSprocessing.clusterByFix - Including pause points as locations.");

		for (int i=0; i < vtp.size(); i++){
			tp = vtp.get(i);
			// ignore lone fixes -- not considered locations
			if (tp.isLoneFix())
				continue;
			// ignore midpoints
			if (tp.isMidPoint())
				continue;
			// ignore stationary
			if (tp.isStationary())
				continue;
			// ignore locations already in location list
			loc = commonLocations.findLocationWithinBuffer(tp.lat, tp.lon);
			if (loc != null)
				continue;

			// cluster on fix type	
			if (tp.isEndPoint() || tp.isLastFix() || tp.isStartPoint() || tp.isFirstFix()){ 
				priority = assignFixPriority(tp);
				newLocations.add(Integer.toString(counter++), priority, tp.lat, tp.lon, tp.ele, pb.gps_cluster_radius, pb.gps_cluster_radius);
				//TODO: should buffer be same as cluster ?
			}
			else {
				// should PausePoints be included
				if (pb.gps_cluster_include_pauses)
					// yes - is it a PausePoint
					if (tp.isPausePoint()){
						// yes - then include it
						priority = assignFixPriority(tp);
						newLocations.add(Integer.toString(counter++), priority, tp.lat, tp.lon, tp.ele, pb.gps_cluster_radius, pb.gps_cluster_radius);      			
					}
			} // end if
		} // end for
		EventLogger.logEvent("GPSprocessing.clusterByFix - new locations initially detected:" + counter);
		if (counter > 1) {
			removeDuplicateLocations(pb, vtp, newLocations);
			removeShortDurationLocations(pb, vtp, newLocations);
		}
		return newLocations;			
	}

	/*
	 * 	ClusterByGrid - clusters by creating a grid of fixed length squares and accumulating the time spent
	 * 		in each square.  Center of the squares with the most amount of time are returned as locations.
	 *  
	 */

	private LocationList clusterByGrid(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		double radiusInMeters = 6367000;    // radius of the earth
		int gridLength = 30;  				// length of grid box in meets
		TrackPoint tp;
		Location loc;
		LocationList locList = new LocationList();

		EventLogger.logEvent("GPSprocessing.clusterByGrid - Finding extremes....");
		double maxLat = -300;
		double maxLon = -300;
		double minLat = +300;
		double minLon = +300;

		for (int i=0; i < vtp.size(); i++){
			tp = vtp.get(i);

			if (tp.lat > maxLat)
				maxLat = tp.lat;

			if (tp.lat < minLat)
				minLat = tp.lat; 

			if (tp.lon > maxLon)
				maxLon = tp.lon;

			if (tp.lon < minLon)
				minLon = tp.lon;
		}

		EventLogger.logEvent("GPSprocessing.clusterByGrid - Max: " + maxLat + "," + maxLon + 
				"   Min:" + minLat + "," + minLon);

		WayPoint minmin = new WayPoint(minLat, minLon, 0, "min min"); 
		WayPoint minmax = new WayPoint(minLat, maxLon, 0, "min max");       
		WayPoint maxmin = new WayPoint(maxLat, minLon, 0, "max min");  

		int latDelta = (int)minmin.calDistance(maxmin);
		int lonDelta = (int)minmin.calDistance(minmax);

		EventLogger.logEvent("GPSprocessing.clusterByGrid - latDelta:" + latDelta +  
				"   lonDelta:" + lonDelta);

		int nlatBoxes = (latDelta / gridLength) + 1;
		int nlonBoxes = (lonDelta / gridLength) + 1;

		EventLogger.logEvent("GPSprocessing.clusterByGrid - nlatBoxes:" + nlatBoxes +  
				"   nlonBoxes:" + nlonBoxes);    

//		replaced with HashMap to conserve memory
//		long[][] clusterGrid = new long[nlatBoxes][nlonBoxes];

		HashMap<String, Long> clusterMap = new HashMap<String, Long>();
		String key ="";
		Long value = 0L;

		EventLogger.logEvent("GPSprocessing.clusterByGrid - Counting....");

		for (int i=0; i < vtp.size(); i++){
			tp = vtp.get(i);

			if (tp.getSpeed() == 0){  // cluster stationary points
				// process lat
				WayPoint wp = new WayPoint(minmin.lat, tp.lon, 0, "temp");
				int distance = (int) wp.calDistance(tp);
				int latIndex = distance / gridLength;

				// process lon
				wp = new WayPoint(tp.lat, minmin.lon, 0, "temp");
				distance = (int) wp.calDistance(tp);
				int lonIndex = distance / gridLength;

				key = "X" + lonIndex + "Y" + latIndex;
				value = clusterMap.get(key);
				if (value == null){
					value = (long) tp.getDuration();
					EventLogger.logEvent("GPSprocessing.clusterByGrid - inserting " + key + " value:" + value);
					clusterMap.put(key, value);
				}
				else {
					value = value + (long) tp.getDuration();
					EventLogger.logEvent("GPSprocessing.clusterByGrid - adding  " + value + " to" + key);
					clusterMap.put(key, value);	
				}

				// TODO: Most duration or most references ???
				//      		clusterGrid[latIndex][lonIndex] = clusterGrid[latIndex][lonIndex] + tp.getDuration();
			}
		}

		EventLogger.logEvent("GPSprocessing.clusterByGrid - Finding locations with time > " + pb.gps_trip_max_pause);	
		/*

        int locNumber = 100;	
        	for (int i=0; i < nlatBoxes; i++)
        		for (int j=0; j<nlonBoxes; j++){
        			if (clusterGrid[i][j] >= pb.gps_trip_max_pause) {

        	        	// compute wp that's the center of the grid
        				// compute lat north on minLat
        				double distance = (gridLength/2) + (gridLength * i);
        				distance = distance / radiusInMeters;
        				double locLat = Math.toDegrees(Math.toRadians(minLat) + distance);

        				// compute lon east of minLon
        				distance = (gridLength/2) + (gridLength * j);
        				distance = distance / radiusInMeters;
        				double bearing = Math.toRadians(90);
        				double y = Math.sin(bearing)*Math.sin(distance)*Math.cos(Math.toRadians(minLat)); 
        				double x = Math.cos(distance) - Math.sin(Math.toRadians(minLat)) * Math.sin(Math.toRadians(minLat)); 
        				double locLon = minLon + Math.toDegrees(Math.atan2(y, x));

        				locNumber++;
        				loc = new Location (Integer.toString(locNumber), "unknown", locLat, locLon, 0, pb.gps_cluster_radius, pb.gps_cluster_radius);

        	        	// add location to location list
        				locList.add(loc);

        				EventLogger.logEvent("GPSprocessing.clusterByGrid - got cluster at:" + i + "," + j + " time:" + clusterGrid[i][j] +
        						" Lat:" + locLat + " Lon:" + locLon);
        			}
        		}
		 */

		int locNumber = 100;
		int x,y,i;
		Set<Map.Entry<String, Long>> clusterSet = clusterMap.entrySet();

		for (Map.Entry<String, Long> cluster : clusterSet) {
			value = cluster.getValue();
			if (value >= pb.gps_trip_max_pause) {
				key = cluster.getKey();

				i = key.indexOf("Y");
				x = Integer.valueOf(key.substring(1, i));
				y = Integer.valueOf(key.substring(i+1));


				// compute wp that's the center of the grid
				// compute lat north on minLat
				double distance = (gridLength/2) + (gridLength * y);
				distance = distance / radiusInMeters;
				double locLat = Math.toDegrees(Math.toRadians(minLat) + distance);

				// compute lon east of minLon
				distance = (gridLength/2) + (gridLength * x);
				distance = distance / radiusInMeters;
				double bearing = Math.toRadians(90);
				double yy = Math.sin(bearing)*Math.sin(distance)*Math.cos(Math.toRadians(minLat)); 
				double xx = Math.cos(distance) - Math.sin(Math.toRadians(minLat)) * Math.sin(Math.toRadians(minLat)); 
				double locLon = minLon + Math.toDegrees(Math.atan2(yy, xx));

				locNumber++;
				loc = new Location (Integer.toString(locNumber), -1, locLat, locLon, 0, pb.gps_cluster_radius, pb.gps_cluster_radius);

				// add location to location list
				locList.add(loc);

				EventLogger.logEvent("GPSprocessing.clusterByGrid - got cluster at:" + key + " time:" + value +
						" Lat:" + locLat + " Lon:" + locLon);
			} // end if
		} // end for

		EventLogger.logEvent("GPSprocessing.clusterByGrid - Number of locations returned:" + locList.size());
		return locList;			
	}

	private LocationList removeDuplicateLocations(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, LocationList locList){
		TrackPoint tp;
		Location loc;
		int counter = 0;

		// determine most popular locations
		for (int i=0; i < vtp.size(); i++){
			tp = vtp.get(i);
			loc = locList.findNearbyLocation(tp);
			if (loc != null)
				loc.incReferences();
		}

		// purge duplicates - remove in this order -- firstFix, Pause, start, lastfix, end
		EventLogger.logEvent("GPSprocessing.removeDuplicateLocations - purging duplicate locations...");

		for (int index = 0; index < locList.size(); index++){
			Location me = locList.get(index);
			Location nearby = locList.findNearbyLocationWithMoreReferences(me);
			if (nearby != null){
				int mePri = me.getPriority();
				int nearByPri = nearby.getPriority();
				if (nearByPri >= mePri){
					//   				EventLogger.logEvent("Dropping a " + me.type + " for a " + nearby.type);
					// increase nearby's reference count then purge me
					nearby.setReferences(nearby.getReferences()+ me.getReferences());
					if (locList.purgeMe(me)){
						index--;	// adjust index for item removed
						counter++;
					}
				}
				else{
					//   				EventLogger.logEvent("Dropping a " + nearby.type + " for a " + me.type);
					// increase me's reference count then purge nearby
					me.setReferences(nearby.getReferences()+ me.getReferences());
					if (locList.purgeMe(nearby)){
						index--;	// adjust index for item removed
						counter++;
					}
				}
			} // end if (nearby != null)
		} // end for

		EventLogger.logEvent("GPSprocessing.removeDuplicateLocations - Duplicate locations removed:" + counter);
		EventLogger.logEvent("GPSprocessing.removeDuplicateLocations - Number of locations returned:" + locList.size());
		return locList;		
	}

	// TODO: consider using consecutive seconds at location as cutoff instead of total time at location
	private void removeShortDurationLocations(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, LocationList locations){
		Location loc;
		int counter = 0;
		// compute time spend at each location
		for (int index=0; index<vtp.size(); index++){
			TrackPoint tp = vtp.get(index);
			loc = locations.findLocationWithinBuffer(tp.lat, tp.lon);
			if (loc != null)
				loc.addTimeAtLocation(tp.getDuration());
		}
		// remove locations 
		for (int index=0; index<locations.size(); index++){
			loc = locations.get(index);
			// TODO: don't remove if duraction <= epoch
			if (loc.getTimeAtLocation() < pb.gps_cluster_min_duration){
				EventLogger.logEvent("GPSprocessing.removeShortDurationLocations - Removing location at " + loc.getLat() + 
						" " + loc.getLon() + "  Duration:" + loc.getTimeAtLocation());
				locations.remove(index);
				index--;	// adjust index for item removed
				counter++;
			} // end if
		} // end for
		if (counter > 0)
			EventLogger.logEvent("GPSprocessing.removeShortDurationLocations - Locations removed:" + counter);
		EventLogger.logEvent("GPSprocessing.removeShortDurationLocations - Number of locations returned:" + locations.size());
		return;
	}

	private void addLocations(LocationList newLocations, int startingNumber){
		for (int index = 0; index < newLocations.size(); index++){
			Location loc = newLocations.get(index);
			loc.name = Integer.toString(startingNumber++);
			commonLocations.add(loc);
		} // end for
		EventLogger.logEvent("GPSprocessing.addLocations - total number of common locations:"+ commonLocations.size());
	}

	/*
	private int calcFixPriority(Location loc){
		final int[] fixPriority = {TrackPoint.FIRSTFIX, TrackPoint.PAUSEPOINT, TrackPoint.STARTPOINT, 
				TrackPoint.LASTFIX, TrackPoint.ENDPOINT}; // ordered from lowest to highest
		for (int i=fixPriority.length-1; i >= 0; i--){ // scan from highest to lowest
			if (loc.type == fixPriority[i]))
				return i;
		}
		return -1;
	}
	 */

	// assign Fix Priority from lowest to highest
	private int assignFixPriority(TrackPoint tp){
		if (tp.isFirstFix())
			return 0;
		if (tp.isPausePoint())
			return 1;
		if (tp.isStartPoint())
			return 2;
		if (tp.isLastFix())
			return 3;
		if (tp.isEndPoint())
			return 4;
		return -1;
	}

	/*
	 * setLocation - sets location number in TrackPoint
	 */

	private boolean setLocation(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, LocationList locList){
		boolean rc = true;
		int index = 0;
		TrackPoint tp;
		Location loc = null;
		for (index = 0; index < vtp.size(); index++){
			tp = vtp.get(index);
			loc = locList.findNearbyLocation(tp);
			if (loc != null){
				tp.setLocationNumber(Integer.parseInt(loc.name));		
				if (tp.lat == loc.lat && tp.lon == loc.lon)
					tp.setClusterFlag(TrackPoint.CLUSTERCENTER);
			}	
		}
		return rc;
	}

	// TODO: should this routine delete redundant captured points ?
	// captures trackPoints near location if trackPoint is indoors
	private boolean locationCapture(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, LocationList locList){
		boolean rc = true;
		boolean trapPoint = false;
		int counter = 0;
		int index = 0;
		int locationNumber = 0;
		TrackPoint tp, prevTP;
		Location loc = null;
		prevTP = vtp.get(0);
		for (index = 0; index < vtp.size(); index++){
			tp = vtp.get(index);
			locationNumber = tp.getLocationNumber();
			if (locationNumber > 0) {						// is near a location ?
				// yes - determine if it should be trapped
				if (tp.isIndoors() && pb.gps_cluster_trap_indoors)
					trapPoint = true;		// indoors
				else
					if ((tp.getTripNumber() > 0) && (!pb.gps_cluster_trap_trips))
						trapPoint = false;		// part of trip - don't trap
					else
						if (pb.gps_cluster_trap_indoors && pb.gps_cluster_trap_outdoors)
							trapPoint = true;				// trap everything
						else
							if (tp.getIndoorsEstimate() == TrackPoint.UNKNOWN && 
									(pb.gps_cluster_trap_indoors || pb.gps_cluster_trap_outdoors))
								trapPoint = true;			// unknown trap it
							else
								if (tp.isOutdoors() && pb.gps_cluster_trap_outdoors)
									trapPoint = true;	// outdoors
								else
									if (tp.isInVehicle())
										trapPoint = true;
									else
										trapPoint = false;  // none of the above

				if (trapPoint) {								// Trap this point?
					loc = locList.get(locationNumber-1);  // yes - find location in list
					if (tp.lat == loc.lat && tp.lon == loc.lon)
						tp.setClusterFlag(TrackPoint.CLUSTERCENTER);
					else {
						tp.setClusterFlag(TrackPoint.CLUSTERED);
						tp.lat = loc.lat;							// set tp's coordinates to those of location
						tp.lon = loc.lon;
						tp.ele = loc.ele;
					}
					tp.setDistance(tp.distanceBetween(prevTP));  			// update distance between points
					tp.setElevationDelta(tp.elevationBetween(prevTP));
					tp.setSpeed(tp.speedBetween(prevTP));			
					tp.setBearing(tp.calBearing(prevTP));
					tp.setBearingDelta(tp.bearingChange(prevTP));
//					EventLogger.logEvent("GPSprocessing.locationCapture - TP " + index + " captured at "+ loc.name);
					counter++;
				}
			}
			prevTP = tp;
		} // end for
		EventLogger.logEvent("GPSprocessing.locationCapture - TrackPoints captured:"+ counter + 
				" Number of locations:"+ locList.size());
		return rc;
	}

	/*
	 * Trip Detection
	 */

	/* 
	 * Detect Trips between Locations -- Experimental
	 * 
	 * TODO: Needs to incorporate all code from detectTrips
	 */

	private boolean detectTripsBetweenLocations(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		boolean rc = true;
		TrackPoint previousTp = null, currentTp = null, pauseTp = null;
		int previousLocation = -1, tripCount = 0;

		final int STATIONARY = 1;
		final int MOVING = 2;
		final int PAUSED = 3;
		int state = STATIONARY;

		// kludge -- mark all points as Stationary
		int index = 0;
		for (index = 0; index < vtp.size(); index ++){
			currentTp = vtp.get(index);
			currentTp.setTripType(TrackPoint.STATIONARY); // consider all points stationary
		}

		// find first non-location
		previousTp = vtp.get(0);
		for (index = 0; index < vtp.size(); index ++){
			currentTp = vtp.get(index);
			if (currentTp.getLocationNumber() != -1)   // location numbers are > 0
				break;
			previousTp = currentTp;
		}

		if (index == vtp.size()){
			EventLogger.logWarning("GPSprocessing.detectTripsBetweenLocations - No trips found. All points assigned to locations.");
			return true;
		}

		// set start of trip in previous TrackPoint
		previousLocation = previousTp.getLocationNumber();
		previousTp.setTripType(TrackPoint.STARTPOINT);
		tripCount++;
		state = MOVING;
		previousTp = currentTp;
		index++;

		while (index < vtp.size()) {
			currentTp = vtp.get(index);

			switch (state){		

			case MOVING:											// in moving state
				// check for loss of signal
				if (currentTp.isLastFix()){
					currentTp.setTripType(TrackPoint.ENDPOINT);
					state = STATIONARY;
					break;
				}

				// are we at a location?
				if (currentTp.getLocationNumber() != -1)
					// is it a new location ?
					if (currentTp.getLocationNumber() != previousLocation){
						currentTp.setTripType(TrackPoint.ENDPOINT);		// yes - mark as end of trip
						state = STATIONARY;
						previousLocation = currentTp.getLocationNumber();
						break;
					}

				// are we stopped ?
				if (currentTp.getDuration() > pb.gps_trip_max_pause){ 	
					currentTp.setTripType(TrackPoint.ENDPOINT);		// yes - mark as end of trip	
					state = STATIONARY;
					break;
				}

				// are we paused ?
				if (currentTp.getDuration() > pb.gps_trip_min_pause){
					currentTp.setTripType(TrackPoint.PAUSEPOINT); // yes - mark as paused
					pauseTp = currentTp;  					// save trackPoint of initial pause point
					state = PAUSED;
					break;
				}

				// otherwise we are in middle of trip
				currentTp.setTripType(TrackPoint.MIDPOINT);			// yes - mark as middle of trip
				break;

			case PAUSED:										// in paused state
				// check for lost of signal
				if (currentTp.isLastFix()){
					currentTp.setTripType(TrackPoint.ENDPOINT);	
					/* 
					vtp.get(indexPause).reType(TrackPoint.ENDPOINT);
					indexPause++;
					//  yes - mark last group of Pause TPs as Stationary
					while (indexPause < index){
						vtp.get(indexPause).reType(TrackPoint.STATIONARY);
						counter_pause--;
						indexPause++;
					} // end while
					 */

					break;
				}

				// is distance traveled great enough to be considered moving?
				if (currentTp.distanceBetween(pauseTp) >= pb.gps_trip_min_distance) {
					currentTp.setTripType(TrackPoint.MIDPOINT);			// yes - mark as middle of trip
					state = MOVING;									// set state to moving
					break;
				}

				// not moving -- max pause time exceeded?
				if ((currentTp.getDuration() > pb.gps_trip_max_pause) || (currentTp.timeBetween(pauseTp) > pb.gps_trip_max_pause)){
					currentTp.setTripType(TrackPoint.ENDPOINT);		// yes - consider us stopped
					state = STATIONARY;
					break;
				}

				// we are still paused
				currentTp.setTripType(TrackPoint.PAUSEPOINT);
				break;


			case STATIONARY:											// stationary
				// are we at a location?
				if (currentTp.getLocationNumber() > 0){
					currentTp.setTripType(TrackPoint.STATIONARY);		// yes - mark as stationary	
				}
				else{
					previousTp.setTripType(TrackPoint.STARTPOINT);		// yes - mark previous as start
					tripCount++;
					currentTp.setTripType(TrackPoint.MIDPOINT);			// mark current as moving
					state = MOVING;									// set state to moving
					previousLocation = previousTp.getLocationNumber();
				}
				break;

			default:
				EventLogger.logError("GPSprocessing.detectTripsBetweenLocations - invalid state:" + state);
			state = STATIONARY;
			break;

			} // end switch

			previousTp = currentTp;
			index++;
		} // end while
		EventLogger.logWarning("GPSprocessing.detectTripsBetweenLocations - " + tripCount + " trips found.");
		return rc;
	}

	private boolean detectTrips(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		boolean rc = true;
		TrackPoint currentTp, pauseTp = null, futureTp = null;
		int indexPause = 0;								// index of 1st Pause TP
		int pauseDuration = 0;
		int index = 0;									
		int futureIndex = 0;
		String tripStart ="";

		// counters
		int counter_start = 0;
		int counter_end = 0;
		int counter_pause = 0;
		int counter_midpoint = 0;
		int counter_stationary = 0;

		// state definitions
		final int STATIONARY = 1;
		final int MOVING = 2;
		final int PAUSED = 3;
		int state = STATIONARY;     


		if (vtp.size() <= 3){
			EventLogger.logWarning("GPSprocessing.detectTrips - not enough points to detect trips");
			return true;
		}

		int maxDistancePerMinute = (pb.gps_filter_max_speed * 1000) / 60;
		EventLogger.logEvent("GPSprocessing.detectTrips - Interval = "+ pb.interval);
		EventLogger.logEvent("GPSprocessing.detectTrips - Min distance per minute (meters) = "+ pb.gps_trip_min_distance);
		EventLogger.logEvent("GPSprocessing.detectTrips - Max distance per minute (meters) = "+ maxDistancePerMinute);
		EventLogger.logEvent("GPSprocessing.detectTrips - Min pause duration (seconds) = "+ pb.gps_trip_min_pause);
		EventLogger.logEvent("GPSprocessing.detectTrips - Max pause duration (seconds) = "+ pb.gps_trip_max_pause);
		index = 0;										
		while (index < vtp.size()) {
			currentTp = vtp.get(index);
			currentTp.setTripType(TrackPoint.STATIONARY);		// set point as initially stationary
			futureIndex = getFutureTPIndex(vtp, index, futureIndex, 60);
			futureTp = vtp.get(futureIndex);	// find tp at least 60 seconds in the future

			switch (state){		

			case STATIONARY:											// stationary
				/*
				if (futureTp.isIndoors() && futureTp.getSpeed()< pb.gps_speed_vehicle){
					break;   // don't consider start of trip -- *** EXPERMINTAL
				}
				*	This code was an attempt to limit false trips, but it also missed walking trips with poor GPS coverage
				*	Better to detect trips here solely based on distance and time
				*	And use indoor/outdoor to filter false trips
				*
				*/

				// is distance over next 60 seconds great enough to be considered start of trip AND
				// less than the distance that can be traveled in Max speed AND
				// current point is NOT a last fix AND
				// duration at current point <= 60 seconds   ** fjr 1/28/12
				if ((currentTp.distanceBetween(futureTp) >= pb.gps_trip_min_distance) &&
						(currentTp.distanceBetween(futureTp)<= maxDistancePerMinute ) &&
						(!currentTp.isLastFix()) &&
						(currentTp.getDuration() <= 60)){

					currentTp.setTripType(TrackPoint.STARTPOINT);		// yes - mark as start
					tripStart = "  Trip start:" + currentTp.getDateTimeStr();  
					counter_start++;
					state = MOVING;								    // set state to moving
				}
				else {
					currentTp.setTripType(TrackPoint.STATIONARY);			// else mark as stationary
					counter_stationary++;
				}
				break;

			case MOVING:											// in moving state	
				// did loss of signal occur?
				if (currentTp.isLastFix()){
					EventLogger.logEvent(tripStart + " end:" + currentTp.getDateTimeStr() + " - LOS while moving");  // for debugging
					currentTp.setTripType(TrackPoint.ENDPOINT);			// yes - mark as trip end
					counter_end++;
					state = STATIONARY;
					break;
				}

				if (currentTp.getDuration() > pb.gps_trip_max_pause){ 	// is time at current location > max pause
					EventLogger.logEvent(tripStart + " end:" + currentTp.getDateTimeStr() + " - Max Pause exceeded while moving");  // for debugging
					currentTp.setTripType(TrackPoint.ENDPOINT);					// yes - mark current as ENDPOINT
					counter_end++;
					state = STATIONARY;										// set state to stationary
					break;
				}

				if (currentTp.getDuration() > pb.gps_trip_min_pause){   // is time at location > min pause
					currentTp.setTripType(TrackPoint.PAUSEPOINT);			// yes - mark as pause
					counter_pause++;
					pauseTp = currentTp;
					indexPause = index;
					state = PAUSED;
					break;
				}

				// is distance traveled over next n seconds great enough to be considered still moving?
				if (currentTp.distanceBetween(futureTp) >= pb.gps_trip_min_distance){       // was over 60 seconds
					currentTp.setTripType(TrackPoint.MIDPOINT);		// yes - mark as middle of trip
					counter_midpoint++;
				}
				else {
					// haven't traveled very far 
					/* old logic
								currentTp.addType(TrackPoint.PAUSEPOINT);			// consider us paused
								counter_pause++;
								state = PAUSED;										// set state to paused
								pauseTp = currentTp;			                    // same start of pause
								indexPause = index;
					 */
					// new logic
					currentTp.setTripType(TrackPoint.MIDPOINT);				// mark this point as still moving
					counter_midpoint++;
					state = PAUSED;										// BUT set state to paused
					indexPause = index+1;			                    // mark next point as paused
					pauseTp = vtp.get(indexPause);	


				}
				break;

			case PAUSED:										// in paused state
				// check for lost of signal
				if (currentTp.isLastFix()){
					EventLogger.logEvent(tripStart + " end:" + currentTp.getDateTimeStr() + " - LOS while paused");  // for debugging
					counter_end++;
					state = STATIONARY;
					vtp.get(indexPause).setTripType(TrackPoint.ENDPOINT); // yes - mark start of pause as trip end
					indexPause++;
					//  yes - mark last group of Pause TPs as Stationary
					while (indexPause < index){
						vtp.get(indexPause).setTripType(TrackPoint.STATIONARY);
						counter_pause--;
						indexPause++;
					} // end while
					break;
				}

				// is distance great enough to be considered moving?
				if ((currentTp.distanceBetween(pauseTp) >= pb.gps_trip_min_distance)) {
					currentTp.setTripType(TrackPoint.MIDPOINT);			// yes - mark as middle of trip
					counter_midpoint++;
					state = MOVING;									// set state to moving

					// was time paused < min pause time?
//					if (currentTp.timeBetween(pauseTp) < pb.gps_trip_min_pause){					
					if ((currentTp.timeBetween(pauseTp) + currentTp.getDuration() + pauseTp.getDuration()) < pb.gps_trip_min_pause){		
						//  yes - mark last group of Pause TPs as Midpoints
						while (indexPause < index){
							vtp.get(indexPause).setTripType(TrackPoint.MIDPOINT);
							counter_midpoint++;
							counter_pause--;
							indexPause++;
						} // end while
					} // end if

				}
				else{
					// not moving - how long have we been paused
					if (indexPause == index-1)
						pauseDuration = currentTp.timeBetween(pauseTp) + currentTp.getDuration();  // don't double count
					else
						pauseDuration = currentTp.timeBetween(pauseTp) + currentTp.getDuration() + pauseTp.getDuration();

//					if ((currentTp.getDuration() > pb.gps_trip_max_pause) ||
//					(currentTp.timeBetween(pauseTp) > pb.gps_trip_max_pause)){  
//					((currentTp.timeBetween(pauseTp) + currentTp.getDuration() + pauseTp.getDuration()) > pb.gps_trip_max_pause))

					if (pauseDuration > pb.gps_trip_max_pause) {
						EventLogger.logEvent(tripStart + " end:" + currentTp.getDateTimeStr() + " - Max Pause exceeded while paused for " + pauseDuration + " secs");  // for debugging
						currentTp.setTripType(TrackPoint.STATIONARY);			// yes - consider us stopped - mark current as stationary
						counter_stationary++;
						pauseTp.setTripType(TrackPoint.ENDPOINT);					// mark first Pause TP as end
						counter_end++;
						counter_pause--;
						indexPause++;

						// mark last group of Pause TPs as Stationary
						while (indexPause < index){
							vtp.get(indexPause).setTripType(TrackPoint.STATIONARY);
							counter_stationary++;
							counter_pause--;
							indexPause++;
						}					
						state = STATIONARY;
					}
					else {
						currentTp.setTripType(TrackPoint.PAUSEPOINT);    // we are still paused
						counter_pause++;
					}
				}
				break;

			default:
				EventLogger.logError("GPSprocessing.detectTrips - invalid state:" + state);
			state = STATIONARY;
			break;

			} // end switch
			index++;
		} // end while
		EventLogger.logEvent("GPSprocessing.detectTrips - Starts:" + counter_start + " Ends:" + counter_end +
				" Pauses:" + counter_pause + " Mids:" + counter_midpoint + " Stationary:" + counter_stationary);		
		return rc;
	}

//	Not used
	/*
	private int  getPastTP(ArrayList<TrackPoint> vtp, int previousIndex, int currentIndex, int secondsInPast){
		TrackPoint currentTp = vtp.get(currentIndex);
		TrackPoint previousTp = currentTp;
		int backIndex = 0;
		if (currentIndex < 1)
			return currentIndex;

		// don't backup beyond firstfix
		if (currentTp.isFirstFix())
			return currentIndex;

		for (backIndex = previousIndex; backIndex < currentIndex; backIndex++){
			previousTp = vtp.get(backIndex);
			int duration = currentTp.timeBetween(previousTp);
			if (duration > secondsInPast)
				continue;
			else
				if (backIndex <= 0)
					return 0;
				else
					return backIndex-1;
		}
		return backIndex;
	}
	 */

	private int  getFutureTPIndex(ArrayList<TrackPoint> vtp, int currentIndex, int lookAheadIndex, int secondsInFuture){
		TrackPoint currentTp = vtp.get(currentIndex);
		do {
			TrackPoint futureTp = vtp.get(lookAheadIndex);
			int duration = currentTp.timeBetween(futureTp);
			if (duration >= secondsInFuture)
				return lookAheadIndex;
			lookAheadIndex++;
		}  while (lookAheadIndex < vtp.size());
		return vtp.size()-1;
	}	

	// this should be called after filtering and trip detection
	private boolean averageSpeedElevation(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		boolean rc = true;
		double averageSpeed = 0, averageElevationDelta = 0;
		ArrayList<Double>previousSpeeds = new ArrayList<Double>();
		ArrayList<Integer>previousElevationDeltas = new ArrayList<Integer>();
		double n;
		int index;
		TrackPoint tp;

		// check that averaging is desired
		if (!(pb.gps_average_speed || pb.gps_average_elevation))
			return true;

		// check that we have enought points
		if (pb.gps_average_samples >= vtp.size()){
			EventLogger.logWarning("GPSprocessing.averageSpeedElevation - not enought fixes to compute moving average.");
			return true;
		}

		// calculate averages for first n samples
		n = pb.gps_average_samples;		
		for (index = 0; index < n; index++){
			tp = vtp.get(index);
			previousSpeeds.add(tp.getSpeed());
			previousElevationDeltas.add(tp.getElevationDelta());
			averageSpeed = averageSpeed + tp.getSpeed();
			averageElevationDelta = averageElevationDelta + tp.getElevationDelta();	
		}
		averageSpeed = averageSpeed / n;
		averageElevationDelta = averageElevationDelta / n;

		// replace first n points with average
		// TODO:  Is this the correct thing to do?   Or leave unchanged?
		for (index = 0; index < n; index++){
			tp = vtp.get(index);
			if (pb.gps_average_speed)
				tp.setSpeed((int)averageSpeed);
			if (pb.gps_average_elevation)
				tp.setElevationDelta((int)averageElevationDelta);	
		} // end for

		// process entire vector
		for (index = (int)n; index < vtp.size(); index++){
			tp = vtp.get(index);
			// add current to average and subtract (current - n)
			if (pb.gps_average_speed){
				averageSpeed = (averageSpeed + ((double)(tp.getSpeed())/n)) - ((double)(previousSpeeds.get(0))/n);
				previousSpeeds.remove(0);
				previousSpeeds.add(tp.getSpeed());
				tp.setSpeed((int)averageSpeed);
				// TODO: bug - average speed goes negative & what to do about pauses ???
				// TODO: bug - (current -n) is now moving average - not original value !!!
			}
			if (pb.gps_average_elevation){
				averageElevationDelta = averageElevationDelta + ((double)(tp.getElevationDelta())/n) - ((double)(previousElevationDeltas.get(0))/n);
				previousElevationDeltas.remove(0);
				previousElevationDeltas.add(tp.getElevationDelta());
				tp.setElevationDelta((int)averageElevationDelta);
			}	
		} // end for
		return rc;
	}


	private LocationList removeLocationsWithoutTrips(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, LocationList inList, int firstLocationNumber){
		TrackPoint tp = null;
		LocationList outList = new LocationList();
		boolean locationUsed[] = new boolean[inList.size()];
		int locationMap[] = new int[inList.size()];
		int lastCommonLocationIndex = firstLocationNumber -2;	 // was -1

		EventLogger.logEvent("GPSprocessing.removeLocationsWithoutTrips - Number of locations:" + inList.size() +
				" First new location number:" + firstLocationNumber);
		try {
			if (lastCommonLocationIndex <= 0)
				lastCommonLocationIndex = -1;  // first time - no common locations

			// save common locations
			for (int index=0; index < inList.size(); index++)
				if (index <= lastCommonLocationIndex) {
					locationUsed[index] = true;				// it was a location from previous participants
					outList.add(inList.get(index));	// copy to output list
				}
				else
					locationUsed[index] = false;

			for (int index=0; index < vtp.size(); index++){
				tp = vtp.get(index);
				int loc = tp.getLocationNumber();
				if (loc > 0){
					if (tp.isStartPoint() || tp.isEndPoint() || (pb.gps_cluster_include_pauses && tp.isPausePoint()))   // fjr 9/11
						locationUsed[loc-1] = true;
				}
			} // end for

			// remove unused locations and keep track of new location numbers
			boolean removedLocation = false;
			int newLocationNumber = firstLocationNumber;
			for (int index=lastCommonLocationIndex+1; index < inList.size(); index++){	
				Location loc = inList.get(index);
				if (locationUsed[index]){
					loc.name = Integer.toString(newLocationNumber);	// renumber locations
					locationMap[index] = newLocationNumber++;
					outList.add(loc);
				}
				else {
					removedLocation = true;
					locationMap[index]= -1;			// set to unknown
					EventLogger.logEvent("  Removed location " + (index+1) + 
							" at " + loc.lat + ", " + loc.lon + 
							"  Duration:" + loc.getTimeAtLocation());
				}

			}
			if (removedLocation) {
				// renumber location references in vtp elements
				for (int index=0; index < vtp.size(); index++){
					int loc = vtp.get(index).getLocationNumber();
					if (loc >= firstLocationNumber){
						vtp.get(index).setLocationNumber(locationMap[loc-1]);	
					}
				} // end for

				String s = "";
				for (int index=0; index < locationMap.length; index++){
					s = s + index + ":" + locationMap[index] + ", ";
				}

				EventLogger.logEvent("locationMap - " + s);

			} // end if (removedLocation)
		} // end try
		catch (Exception ex){
			EventLogger.logException("GPSprocessing.removeLocationsWithoutTrips - Error occurred during processing. No locations removed.", ex);
			outList = inList;
		}

		EventLogger.logEvent("GPSprocessing.removeLocationsWithoutTrips - Number of locations returned:" + outList.size());
		return outList;
	}

	// Trims indoor points from start and end of trips 
	// note -- trips are not numbered when this routine is called
	private boolean trimTrips(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		int indexTripStart = 0, newTripStart = 0;
		int indexTripEnd = 0, newTripEnd = 0;
		int index = 0;
		String tripStart, tripEnd;
		TrackPoint tp;
		boolean removedTrip = false;
		boolean trimmed = false;

		if (pb.gps_indoors_detect == false)
			return true;

		// find trips 
		while (true){
			indexTripStart = findTripStart(vtp, indexTripEnd);
			if (indexTripStart == -1)
				break;						// no more trips, exit loop
			indexTripEnd = findTripEnd(vtp, indexTripStart);
			tripEnd = vtp.get(indexTripEnd).getDateTimeStr();
			if (indexTripEnd == -1){
				tp = vtp.get(indexTripStart);	// this shouldn't happen
				tripStart = tp.getDateTimeStr();
				EventLogger.logError("GPSprocessing.trimTrips -- programming error - trip start: " + tripStart );
				return false;
			}
			
			if (pb.gps_trip_percentage_indoors == 100){
				// trips may be 100% indoors
				boolean indoors100 = true;	// assume this trip is 100% indoors
				for (int i=indexTripStart; i<= indexTripEnd; i++){
					if (!vtp.get(index).isIndoors()){
						indoors100 = false;			// found point not indoors
						break;						// no need to continue looking
					} // end if
				}// end for
				if (indoors100)						// was trip 100% indoors?
					return true;					// yes - exit - don't trim trip
			} // end if (pb.gps_
			

			// get start/end timestamps
			tripStart = vtp.get(indexTripStart).getDateTimeStr();
			tripEnd = vtp.get(indexTripEnd).getDateTimeStr();
			newTripStart = indexTripStart;
			newTripEnd = indexTripEnd;

			// trim start of trip
			removedTrip = false;
			trimmed = false;
			index = indexTripStart;
			tp = vtp.get(index++);
			while (tp.isIndoors() && tp.getSpeed()<pb.gps_speed_vehicle){
				trimmed = true;
				tp.setTripType(TrackPoint.STATIONARY);
				tp.setTripNumber(0);
				tp = vtp.get(index++);
				if (index > indexTripEnd){
					EventLogger.logEvent("GPSprocessing.trimTrips -- Removed trip: " + tripStart + " - " + tripEnd +
							" was 100% indoors");
					removedTrip = true;
					trimmed = false;
					break;
				}
			} // end while

			if (trimmed){
				// reset trip start to first indoor point
				newTripStart = index -1;
				tp = vtp.get(newTripStart);
				tp.setTripType(TrackPoint.STARTPOINT);
			}

			if (!removedTrip){
				trimmed = false;				// think this caused the loop
				// trim end of trip
				index = indexTripEnd;
				tp = vtp.get(index--);
				while (tp.isIndoors() && tp.getSpeed()<pb.gps_speed_vehicle){
					trimmed = true;
					tp.setTripType(TrackPoint.STATIONARY);
					tp.setTripNumber(0);
					tp = vtp.get(index--);
				} // end while

				if (trimmed){
					// reset trip end to first indoor point
					newTripEnd = index+2;		// note +2
					tp = vtp.get(newTripEnd);
					tp.setTripType(TrackPoint.ENDPOINT);
				}
			} // end if (!removedTrip	
			// check for error
			if (newTripEnd <= newTripStart){
				EventLogger.logError("GPSproccessing.trimTrip -- End <= start - end:" + newTripEnd +  
						" start:" + newTripStart + " - Original start: " + tripStart);
			}

		} // end while(true)
		return true;
	}

	private boolean reconsiderTrips(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, LocationList locList){
		int indexTripStart = 0;
		int indexTripEnd = 0;
		int index = 0;
		int duration;
		int counter_valid = 0;
		int counter_removed = 0;
		int counter_total = 0;
		int counter_initial_location = 0;
		int counter_final_location = 0;
		int location, initialLocation, finalLocation;
		int tripDistance = 0;
		int percentageIndoors = 0;
		String reason = "";
		boolean validTrip = true;

		EventLogger.logEvent("GPSprocessing.reconsiderTrips - min duration:" + pb.gps_trip_min_duration +
				" min length:" + pb.gps_trip_min_length); 

		// find trips 
		while (true){
			indexTripStart = findTripStart(vtp, indexTripEnd);
			if (indexTripStart == -1)
				break;						// no more trips, exit loop
			indexTripEnd = findTripEnd(vtp, indexTripStart);
			if (indexTripEnd == -1) {
				String tripStart = vtp.get(indexTripStart).getDateTimeStr();	// this shouldn't happen
				EventLogger.logError("GPSprocessing.reconsiderTrips -- programming error - trip start: " + tripStart );
				return false;
			}

			validTrip = true;						// assume trip is valid
			reason = "";

			// compute trip distance and duration
			tripDistance = 0;
			percentageIndoors = 0;
			for (index = indexTripStart+1; index <= indexTripEnd; index++) 
				tripDistance = tripDistance + (int)vtp.get(index).getDistance();
			duration = vtp.get(indexTripStart).timeBetween(vtp.get(indexTripEnd-1));

			// is trip long enough?
			if (tripDistance < pb.gps_trip_min_length){
				validTrip = false;
				reason = "length " + tripDistance + " less than mimimum";
			}		

			// is trip duration < min?
			if (validTrip){
				if (duration < pb.gps_trip_min_duration){
					validTrip = false;
					reason = "duration " + duration +" was too short";
				}
			} // end if (validTrip

			if (validTrip) {
				// may be valid - did trip occur within one location?
				initialLocation =  vtp.get(indexTripStart).getLocationNumber();
				finalLocation = vtp.get(indexTripEnd).getLocationNumber();
				if ((initialLocation != -1) && (finalLocation != -1)) {
					// initial and final locations are known locations
					counter_initial_location = 0;
					counter_final_location = 0;
					counter_total = 0;
					for (index = indexTripStart+1; index <= indexTripEnd; index++) {
						counter_total++;
						location = vtp.get(index).getLocationNumber();
						if (location == initialLocation)
							counter_initial_location++;
						else
							if (location == finalLocation)
								counter_final_location++;
					} // end for
					int count = counter_initial_location;
					location = initialLocation;
					if (count < counter_final_location){
						count = counter_final_location;
						location = finalLocation;
					}

					if (location > 0){							// fjr PALMS-498
						int percentage = count*100 / counter_total;
						if (percentage > pb.gps_trip_percentage_one_location){
							validTrip = false;
							reason = "occurred " + percentage + "% within location " + 
							location  +	" - max allowed is " + pb.gps_trip_percentage_one_location + "%";
						} // end if (percentage
					} // end if (location 
				} // end initial and final are known
			} // end if (validTrip)

			// Trip valid -- check if entire trip was (mostly) indoors
			if (validTrip){
				int indoorsCount = 0;
				int outdoorsCount = 0;
				int inVehicleCount = 0;
				boolean gotIndoors = false;
				boolean gotOutdoors = false;
				for (index = indexTripStart+1; index <= indexTripEnd; index++) {
					TrackPoint tp = vtp.get(index);
					if (tp.getSpeed() >= pb.gps_speed_vehicle)
						inVehicleCount++;
					else {
						if (vtp.get(index).isIndoors()) {
							gotIndoors = true;	
							indoorsCount++;
						}
						else {
							if (vtp.get(index).isOutdoors()){
								gotOutdoors = true;
								outdoorsCount++;
							}
						}
					} // end else
				} // end for


				// is any indoors/outdoor information present?
				if (gotIndoors || gotOutdoors){
					// yes
					int total = indoorsCount + outdoorsCount + inVehicleCount;
					percentageIndoors = indoorsCount*100 / total;
					if (percentageIndoors > pb.gps_trip_percentage_indoors){
						
						int percentageInVehicle = inVehicleCount*100 / total;
						if (percentageInVehicle > 10){
							EventLogger.logEvent("Allowing trip - in vehicle:"+ percentageInVehicle + "%  indoors:" + 
									percentageIndoors + "%");
						}
						else {
							validTrip = false;
							reason = "occurred " + percentageInVehicle +"% in vehicle, " + 
							percentageIndoors + "% indoors - max allowed indoors is " + pb.gps_trip_percentage_indoors +"%";
						}
					} // end if (percentage
				} // end if (gotIndoors
			} // end if (validTrip

			if (validTrip){
				counter_valid++;
				EventLogger.logEvent("  Trip:" + vtp.get(indexTripStart).getDateTimeStr() + " - " +
						vtp.get(indexTripEnd).getDateTimeStr() + " length:" + tripDistance + 
						" duration:" + duration + " indoors:" + percentageIndoors + "%");
			}
			else {
				// Trip not valid - mark as stationary
				for (index = indexTripStart; index <= indexTripEnd; index++){
					TrackPoint tp = vtp.get(index);
					tp.setTripType(TrackPoint.STATIONARY);
					tp.setSpeed(0);
					if (pb.gps_cluster_trap_indoors || pb.gps_cluster_trap_outdoors){
						if (tp.getLocationNumber() > 0){
							// trap to location
							Location loc = locList.get(tp.getLocationNumber()-1);  // yes - find location in list
							if (tp.lat == loc.lat && tp.lon == loc.lon)
								tp.setClusterFlag(TrackPoint.CLUSTERCENTER);
							else {
								tp.setClusterFlag(TrackPoint.CLUSTERED);
								tp.lat = loc.lat;							// set tp's coordinates to those of location
								tp.lon = loc.lon;
								tp.ele = loc.ele;
							}
							tp.setDistance(0); 
							tp.setElevationDelta(0);
							tp.setBearingDelta(0);
						} // end if
					} // end if
				} // end for

				String message = "  Removed trip:" + 
						vtp.get(indexTripStart).getDateTimeStr() + " - " +
						vtp.get(indexTripEnd).getDateTimeStr() + " length:" + 
						tripDistance + " duration:" + 
						duration + " - " +
						reason;

				if (tripDistance > (pb.gps_trip_min_length * 2))
					EventLogger.logWarning(message);
				else
					EventLogger.logEvent(message);
				
				counter_removed++;
			} // end else  (!validTrip)
		} // end while
		EventLogger.logEvent("GPSprocessing.reconsiderTrips - Number of trips removed: " + counter_removed + 
				" Trips remaining:" + counter_valid);
		return true;
	}

	/*
	 * 
	 * Work in progress -- detect trips between locations and delete short trips
	 * 

	private boolean preTripDetect(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, LocationList locList){
		boolean loopFlag = true;
		int indexTripStart = 0;
		int indexTripEnd = 0;
		int index = 0;
		int location;
		boolean validTrip = true;

		// find trips 
		while (loopFlag){
			indexTripStart = findNextLocation(vtp, indexTripEnd);
			if (indexTripStart == -1)
				break;						// no more trips, exit loop
			location = vtp.get(indexTripStart).getLocationNumber();

			indexTripEnd = findNextNonLocation(vtp, indexTripStart);
			if (indexTripEnd == -1)
				break;

			indexTripEnd = findNextLocation(vtp, indexTripStart);
			if (indexTripEnd == -1)
				indexTripEnd = vtp.size()-1;		// this shouldn't happen 

			// got a trip, remove short legs  (determine time between legs)
			int legDuration = vtp.get(indexTripStart).timeBetween(vtp.get(indexTripEnd));
			if (legDuration < 180){   // LEGS MUST BE LONGER THAN 2 MINUTES
				Location loc = locList.findLocationByName(Integer.toString(location));
				if (loc != null){
					for (index = indexTripStart; index <= indexTripEnd; index++){
						// replace lat/lon/ele
						vtp.get(index).lat = loc.lat;
						vtp.get(index).lon = loc.lon;
						vtp.get(index).ele = loc.ele;	
					} // end for					
				}


			} //end if

		} // end while
		return true;
	}

	private int findNextLocation(ArrayList<TrackPoint> vtp, int startIndex){
		for (int index = startIndex; index < vtp.size(); index++)
			if (vtp.get(index).getLocationNumber() != -1)
				return index;
		return -1;
	}

	private int findNextNonLocation(ArrayList<TrackPoint> vtp, int startIndex){
		for (int index = startIndex; index < vtp.size(); index++)
			if (vtp.get(index).getLocationNumber() == -1)
				return index;
		return -1;
	}

	 */


	/*
	private void classifyAndNumberTripsOLD(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		int indexTripStart = 0;
		int indexTripEnd = 0;
		int indexLegStart = 0, indexLegEnd = 0;
		int indexPauseStart = 0, indexPauseEnd = 0;
		int index = 0, tripNumber = 0, legNumber = 0;
		int mode = TrackPoint.MOT_STATIONARY, previousMode;
		boolean modeChange = false;
		boolean lastLeg = false;
		boolean pauseNoMid = false;

		// first classify all tripModes as stationary; set trip numbers to zero
		for (index = 0; index < vtp.size(); index++){
			vtp.get(index).setTripMode(TrackPoint.STATIONARY);
			vtp.get(index).setTripNumber(0);
		}

		// find trips and classify based on average speed between pauses
		while (true){
			indexTripStart = findTripStart(vtp, indexTripEnd);
			if (indexTripStart == -1)
				break;						// no more trips, exit loop
			indexTripEnd = findTripEnd(vtp, indexTripStart);
			if (indexTripEnd == -1) {
				String tripStart = vtp.get(indexTripStart).getDateTimeStr();
				EventLogger.logError("GPSprocessing.classifyAndNumberTrips -- programming error - trip start: " + tripStart );
				break;
			}

			tripNumber++;
			if (tripNumber > 1000){
				EventLogger.logError("GPSprocessing.classifyAndNumberTrips -- programming error -- number of trips exceeds 1000");
				return;
			}

			legNumber = 1;
			indexPauseStart = findTripPause(vtp, indexLegStart);
			if ((indexPauseStart == -1) || (indexPauseStart >= indexTripEnd)) {
				// no pauses -- label entire trip 
				mode = classifyTripLegByPercentile(pb, vtp, tripNumber, legNumber, indexTripStart, indexTripEnd);
				setModeandNumber(vtp, mode, tripNumber, indexTripStart, indexTripEnd);
			}
			else {
				// trip contains pauses -- mark first leg
				previousMode = classifyTripLegByPercentile(pb, vtp, tripNumber, legNumber, indexTripStart, indexPauseStart-1);
				legNumber++;
				setModeandNumber(vtp, previousMode, tripNumber, indexTripStart, indexPauseStart-1);
				indexLegStart = findTripMidPoint(vtp,indexPauseStart);
				if (indexLegStart == -1){
					// Special case for PAUSE followed by END without MIDPoint -- should not occur
					lastLeg = true;
					indexLegStart = indexPauseStart;
					indexPauseEnd = indexLegEnd;
					modeChange = false;
					pauseNoMid = true;
					EventLogger.logWarning("GPSprocessing.classifyAndNumberTrips - pausePoint without MidPoint detected for trip " + tripNumber);
				}
				else {
					indexPauseEnd = indexLegStart-1;
					modeChange = false;
					lastLeg = false;
					pauseNoMid = false;
				}

				// process other legs until no more pauses
				while (!lastLeg){
					indexLegEnd = findTripPause(vtp, indexLegStart);
					if ((indexLegEnd == -1) || (indexLegEnd >= indexTripEnd)) {
						lastLeg = true;
						break; // exit inner loop
					}
					mode = classifyTripLegByPercentile(pb, vtp, tripNumber, legNumber, indexLegStart, indexLegEnd-1);
					legNumber++;

					if (mode != previousMode){
						modeChange = true;
						tripNumber++;
						legNumber = 1;
					}
					setModeandNumber(vtp, mode, tripNumber, indexLegStart, indexLegEnd-1);
					processPausePoints(vtp, tripNumber, indexPauseStart, indexPauseEnd, modeChange, previousMode, mode);
					modeChange = false;
					previousMode = mode;
					indexPauseStart = indexLegEnd;
					indexLegStart = findTripMidPoint(vtp, indexLegEnd);
					if (indexLegStart == -1){
						EventLogger.logError("GPSprocessing.classifyAndNumberTrips -- programming error");
						break; // exit inner loop
					}
					indexPauseEnd = indexLegStart-1;

				} // end while

				// process last leg
				if (!pauseNoMid){
					// don't do this when pause is followed by end with a midpoint
					mode = classifyTripLegByPercentile(pb, vtp, tripNumber, legNumber, indexLegStart, indexTripEnd);
					if (mode != previousMode){
						modeChange = true;
						tripNumber++;
					}
				}
				setModeandNumber(vtp, mode, tripNumber, indexLegStart, indexTripEnd);
				processPausePoints(vtp, tripNumber, indexPauseStart, indexPauseEnd, modeChange, previousMode, mode);
			} // end if

		} // end while
	}
	 */


	// Private class to keep track of segments
	class Segment {
		public int indexStart, indexEnd;
		public int mode = TrackPoint.MOT_UNKNOWN;
		public boolean certain = false;
		public int distance=0, duration=0;
		public String startTime;
		Segment(int s, int e, int m){
			indexStart = s;
			indexEnd = e;
			mode = m;
		}
		public String toString(){
			String s = "["+indexStart+","+indexEnd+"] Start:" + startTime + " Mode:"+mode+ " Certain:" + certain;
			s = s + "  Dist:" + distance + "  Dur:" + duration; 
			return s;
		}
	}

	private void classifyAndNumberTrips(GPSdpuParameters pb, ArrayList<TrackPoint> vtp){
		int indexTripStart = 0;
		int indexTripEnd = 0;
		int indexSegmentStart = 0, indexSegmentEnd = 0;
		int index = 0, tripNumber = 0, segmentNumber = 0;
		int mode;
		boolean lastSegment = false;

		// first classify all tripModes as stationary; set trip numbers to zero
		for (index = 0; index < vtp.size(); index++){
			vtp.get(index).setTripMode(TrackPoint.STATIONARY);
			vtp.get(index).setTripNumber(0);
		}

		// find trips and classify segments based on speed
		while (true){
			indexTripStart = findTripStart(vtp, indexTripEnd);
			if (indexTripStart == -1)
				break;						// no more trips, exit loop
			indexTripEnd = findTripEnd(vtp, indexTripStart);
			if (indexTripEnd == -1) {
				String tripStart = vtp.get(indexTripStart).getDateTimeStr();
				EventLogger.logError("GPSprocessing.classifyAndNumberTrips -- programming error - trip start: " + tripStart );
				break;
			}

			tripNumber++;
			if (tripNumber > 1000){
				EventLogger.logError("GPSprocessing.classifyAndNumberTrips -- programming error -- number of trips exceeds 1000");
				return;
			}

			ArrayList<Segment> segments = new ArrayList<Segment>();
			segmentNumber = 1;
			indexSegmentStart = indexTripStart;
			indexSegmentEnd = findSegmentEnd(pb, vtp, indexTripStart, indexTripEnd);			
			if (indexSegmentEnd >= indexTripEnd) {
				// no pauses -- label entire trip 
				mode = classifyTripLegByPercentile(pb, vtp, tripNumber, segmentNumber, indexTripStart, indexTripEnd);
				setModeandNumber(vtp, mode, tripNumber, indexTripStart, indexTripEnd);
			}
			else {
				lastSegment = false;
				// trip multiple segments -- mark each
				while (!lastSegment){
					Segment segment = segmentTrip(pb, vtp, tripNumber, segmentNumber, indexSegmentStart, indexSegmentEnd);
					segmentNumber++;
					segments.add(segment);

					indexSegmentStart = indexSegmentEnd + 1;
					indexSegmentEnd = findSegmentEnd(pb, vtp, indexSegmentStart, indexTripEnd);
					if (indexSegmentEnd >= indexTripEnd)
						lastSegment = true;
				} // end while

				// process last segment
				Segment segment = segmentTrip(pb, vtp, tripNumber, segmentNumber, indexSegmentStart, indexSegmentEnd);
				segments.add(segment);

				// merge adjanct segments
				ArrayList<Segment> outSegments = new ArrayList<Segment>();
				Segment os = segments.get(0);
				Segment is;
				outSegments.add(os);
				EventLogger.logEvent("Outputing segment #1 - Start:" + os.startTime + " Mode:" + os.mode);

				for (int si = 1; si < segments.size(); si++){
					is = segments.get(si);
					if (os.mode == is.mode){  						// is mode the same ?
						os.distance = os.distance + is.distance;	// yes - combine segments
						os.duration = os.duration + is.duration;
						os.indexEnd = is.indexEnd;
						EventLogger.logEvent("Combining segment #" + (si+1));
					}
					else {
						os = is;									// no - output this segment
						outSegments.add(os);
						EventLogger.logEvent("Outputing segment #" + (si+1) + " Start:" + os.startTime + " Mode:" + os.mode);
					}
				}	// end for
				EventLogger.logEvent("Number of segments after combining:" + outSegments.size());
				segments = outSegments;

				// process vector of segments looking for mode changes

				for (int si = 1; si < segments.size()-1; si++){
					Segment s = segments.get(si);
					if (s.mode == (segments.get(si-1).mode))
						continue;		// current mode matches previous mode
					else {
						if (s.mode == (segments.get(si+1).mode))
							continue;		// current mode matches next mode
						else {
							// current mode doesn't match either previous or next mode
							// are previous and next the same?
							if (segments.get(si-1).mode == (segments.get(si+1).mode)){
								// yes -- is segment long enought to be a trip by itself?
								if (s.distance < pb.gps_trip_min_distance || s.duration < pb.gps_trip_min_duration){
									// no -- set current mode to previous mode
									EventLogger.logEvent("Reseting mode of segment " + s.startTime + " from " + s.mode + 
											" to "+ segments.get(si-1).mode);
									s.mode = segments.get(si-1).mode;	
								}
							}
							else {
								EventLogger.logEvent("GPSprocessing.classifyAndNumberTrips -- unexpected mode change - segment:" + segments.get(si+1).startTime);
								continue;	// leave as is
							} // end else
						}	// end else
					} // end else
				} // end for

				// set mode in trip
				for (int si = 0; si < segments.size(); si++){
					for (int ti = segments.get(si).indexStart; ti <= segments.get(si).indexEnd; ti++){
						TrackPoint tp = vtp.get(ti);
						mode = segments.get(si).mode;
						if (tp.isPausePoint())
							tp.setTripMode(TrackPoint.STATIONARY);
						else
							tp.setTripMode(mode);
						// for vehicles, change indoors estimate if present
						if (mode == TrackPoint.MOT_VEHICLE)
							if (tp.isIndoors() || tp.isOutdoors())
								tp.setInVehicle();
					} // end for
				} // end for

				//	 TODO: break trips based on mode change & then need to reconsider trips !!
				//   TODO: breaks at first mode change but not any others -- this might be ok	


				TrackPoint previousTp = vtp.get(indexTripStart);
				EventLogger.logEvent("  Checking for mode changes from " + previousTp.getDateTimeStr() +
						" to " + vtp.get(indexTripEnd).getDateTimeStr());

				int previousMode = previousTp.getTripMode();
				for (int ti = indexTripStart+1; ti<indexTripEnd; ti++){
					TrackPoint tp = vtp.get(ti);
					if (tp.isPausePoint())
						continue;			// ignore pauses
					if (tp.getTripMode() == previousMode){
						previousTp = tp;
						continue;			// ok - same mode
					}
					// change of mode detected
					EventLogger.logEvent("    Spliting trip at:" + tp.getDateTimeStr() +
							" Previous mode:" + previousMode + "  Current Mode:" + tp.getTripMode());
					if (previousTp.isStartPoint())
						EventLogger.logError("Programming error...");   // TODO: check next two lines
					previousTp.setTripType(TrackPoint.ENDPOINT);  // TODO: What about pauses we may have skipped over?
					tp.setTripType(TrackPoint.STARTPOINT);
					previousTp = tp;
					previousMode = tp.getTripMode();					
				}// end for

			} // end else
		} // end while(true)

		// number trips
		boolean inTrip = false;
		tripNumber = 0;
		for (index = 0; index < vtp.size(); index++){
			TrackPoint tp = vtp.get(index);
			if (inTrip){
				tp.setTripNumber(tripNumber);
				if (tp.isEndPoint())
					inTrip = false;
			}
			else {
				// not in trip
				if (tp.isStartPoint()){
					tripNumber++;
					tp.setTripNumber(tripNumber);
					inTrip = true;
				} // end if
				// else tp.setTripType(TrackPoint.Stationary) // removes any reclassified pause points
			} // end else
		} // end for
	}

	/*
	private void processPausePoints(ArrayList<TrackPoint> vtp,
			int currentTripNumber, int start, int end, boolean modeChange, int previousMode, int newMode){
		TrackPoint tp;

//		modeChange = false;			// for testing

		if (!modeChange)
			// just set tripNumber
			setTripNumber(vtp, currentTripNumber, start, end);
		else {
			// Break into two trips and make pause stationary
			// setTripNumber(vtp, 0, start, end); // but just set tripNumber = 0 for now
			EventLogger.logEvent("GPSprocessing.processPausePoints - spliting trip " + (currentTripNumber-1) +
					" previous mode: " + previousMode + " current mode: " + newMode);

			// is there only one pause point?
			if (start == end){				
				// yes - only one pause - make it the end of the trip
				tp = vtp.get(start);
//				tp.removeType(TrackPoint.PAUSEPOINT);
				tp.setTripType(TrackPoint.ENDPOINT);
				tp.setTripNumber(currentTripNumber-1);
				tp.setTripMode(previousMode);

				// make next location the start point of the new trip
				tp = vtp.get(start+1);
//				tp.removeType(TrackPoint.MIDPOINT);
				tp.setTripType(TrackPoint.STARTPOINT);
				tp.setTripNumber(currentTripNumber);
				tp.setTripMode(newMode);
			}
			else {
				// no - multiple pause points - make first pause the end point of the previous trip
				tp = vtp.get(start);
//				tp.removeType(TrackPoint.PAUSEPOINT);
				tp.setTripType(TrackPoint.ENDPOINT);
				tp.setTripNumber(currentTripNumber-1);
				tp.setTripMode(previousMode);

				// make last pause the start point of the new trip
				tp = vtp.get(end);
//				tp.removeType(TrackPoint.PAUSEPOINT);
				tp.setTripType(TrackPoint.STARTPOINT);
				tp.setTripNumber(currentTripNumber);
				tp.setTripMode(newMode);

				// make the rest stationary
				for (int index = start+1; index <= end-1; index++){
					tp = vtp.get(index);
//					tp.removeType(TrackPoint.PAUSEPOINT);
					tp.setTripType(TrackPoint.STATIONARY);
					tp.setTripNumber(0);
					tp.setTripMode(TrackPoint.STATIONARY);	
				} // end for
			} // end else
		} // end else
	}

	 */

	private Segment segmentTrip(GPSdpuParameters pb, ArrayList<TrackPoint> vtp,
			int tripNumber, int segmentNumber, int indexLegStart, int indexLegEnd){

		int mode = TrackPoint.MOT_STATIONARY;
		ArrayList<Integer> speeds = new ArrayList<Integer>();
		int zeroSpeeds = 0, index = 0, speed = 0;
		int distance = 0, duration = 0;

		String startTime = vtp.get(indexLegStart).getDateTimeStr();
		String endTime = vtp.get(indexLegEnd).getDateTimeStr();

		// create vector of speeds
		for (index = indexLegStart; index <= indexLegEnd; index++){
			TrackPoint tp = vtp.get(index);
			distance = distance + tp.getDistance();
			duration = duration + tp.getDuration();
			speed = (int)Math.round(tp.getSpeed());
			if (speed > 0)
				speeds.add(speed);
			else 
				zeroSpeeds++;
		} // end for

		if (speeds.size() == 0) {
			mode = TrackPoint.MOT_PEDESTRIAN;
		}
		else {
			// Sort vector
			Collections.sort(speeds);

			// find speed at x percentile
			double factor = (double)pb.gps_speed_percentile / 100.0;
			index = (int) ((double)speeds.size() * factor);
			if (index >= speeds.size())		// check within range
				index = speeds.size()-1; 	// set to last element if not within range
			speed = speeds.get(index);

			// classify trip
			if (speed < pb.gps_speed_bicycle)
				mode = TrackPoint.MOT_PEDESTRIAN;
			else
				if (speed < pb.gps_speed_vehicle)
					mode = TrackPoint.MOT_BICYCLE;
				else
					mode = TrackPoint.MOT_VEHICLE;
		} 

		EventLogger.logEvent("GPSprocessing.segmentTrip - Trip:" + tripNumber + " Segment:" + segmentNumber +
				" Start:" +  startTime + " End:" + endTime + " Mode:" + mode + " Speed:" + speed);
//		EventLogger.logEvent("    Size:" + speeds.size() + "  Index:" + index + " Speed:" + speed + " Zeros:" + zeroSpeeds);

		Segment segment = new Segment(indexLegStart, indexLegEnd, mode);
		segment.startTime = startTime;
		segment.distance = distance;
		segment.duration = duration;
		// calculate certainty
		if ((distance > 100) || (duration > 120))  // TODO: read from PB
			segment.certain = true;

//		EventLogger.logEvent(segment.toString());
		return segment;
	}

	private int classifyTripLegByPercentile(GPSdpuParameters pb, ArrayList<TrackPoint> vtp,
			int tripNumber, int legNumber, int indexLegStart, int indexLegEnd){

		int mode;
		ArrayList<Integer> speeds = new ArrayList<Integer>();
		int zeroSpeeds = 0, index = 0, speed = 0;

		String startTime = vtp.get(indexLegStart).getDateTimeStr();

		// create vector of speeds
		for (index = indexLegStart; index <= indexLegEnd; index++){
			speed = (int)Math.round(vtp.get(index).getSpeed());
			if (speed > 0)
				speeds.add(speed);
			else 
				zeroSpeeds++;
		} // end for

		if (speeds.size() == 0) {
			mode = TrackPoint.MOT_PEDESTRIAN;
		}
		else {
			// Sort vector
			Collections.sort(speeds);

			// find speed at x percentile
			double factor = (double)pb.gps_speed_percentile / 100.0;
			index = (int) ((double)speeds.size() * factor);
			if (index >= speeds.size())		// check within range
				index = speeds.size()-1; 	// set to last element if not within range
			speed = speeds.get(index);

			// classify trip
			if (speed < pb.gps_speed_bicycle)
				mode = TrackPoint.MOT_PEDESTRIAN;
			else
				if (speed < pb.gps_speed_vehicle)
					mode = TrackPoint.MOT_BICYCLE;
				else
					mode = TrackPoint.MOT_VEHICLE;
		} 

		EventLogger.logEvent("GPSprocessing.classifyTripLegByPercentile - Trip:" + tripNumber + " Leg:" + legNumber +
				" Start:" +  startTime + " Mode:" + mode);
		EventLogger.logEvent("    Size:" + speeds.size() + "  Index:" + index + " Speed:" + speed + " Zeros:" + zeroSpeeds);
		return mode;
	}

	private void setModeandNumber(ArrayList<TrackPoint> vtp, int mode,
			int tripNumber, int start, int end) {
		TrackPoint tp;
		for (int index = start; index <= end; index++){
			tp = vtp.get(index);
			tp.setTripMode(mode);
			tp.setTripNumber(tripNumber);
			// for vehicles, change indoors estimate if present
			if (mode == TrackPoint.MOT_VEHICLE)
				if (tp.isIndoors() || tp.isOutdoors())
					tp.setInVehicle();
		}
		return;
	}
	/*
	private void setTripNumber(ArrayList<TrackPoint> vtp, int tripNumber, int start, int end){
		for (int index = start; index <= end; index++)
			vtp.get(index).setTripNumber(tripNumber);
	}
	 */

	private void numberTrips(ArrayList<TrackPoint> vtp){
		boolean inTrip = false;
		int tripNumber = 0;
		int index;
		for (index = 0; index < vtp.size(); index++){
			TrackPoint tp = vtp.get(index);
			// test for trip start
			if (tp.isStartPoint()){
				inTrip = true;
				tripNumber++;
			}

			// set trip number
			if (inTrip)
				tp.setTripNumber(tripNumber);
			else {
				tp.setTripNumber(0);
				tp.setTripMode(TrackPoint.STATIONARY);
			}

			// test for trip end
			if (tp.isEndPoint())
				inTrip = false;
		} // end for
	}

	// segment where distance traveled > 100 && speed = 0
	// also has problems -- few true zeros; too many <.5
	// misses pausePoints 
	/*
	private int findSegmentEndOLD(ArrayList<TrackPoint> vtp, int startingIndex, int tripEndIndex){
		TrackPoint startingTp = vtp.get(startingIndex);

		for (int index = startingIndex; index < tripEndIndex; index++){
			TrackPoint tp = vtp.get(index);
			if (tp.isPausePoint())
				continue;					// ignore pauses
			double speed = tp.getSpeed();
			double roundSpeed = Math.round(speed);
			int distance = startingTp.distanceBetween(tp);
			if (distance > 100) 							// TODO: read from PB
				if (roundSpeed == 0)
					return index;

		} // end for
		return tripEndIndex;
	}
	 */

	private int findSegmentEnd(GPSdpuParameters pb, ArrayList<TrackPoint> vtp, int startingIndex, int tripEndIndex){
		int index = startingIndex;
		// skip over initial pauses
		for (; index < tripEndIndex; index++){
			TrackPoint tp = vtp.get(index);
			if (!tp.isPausePoint()){
				break;			
			}
		}
		if (index == tripEndIndex){
			EventLogger.logError("GPSprocessingR4.findSegmentEnd - programming error");
			return tripEndIndex;
		}

		TrackPoint startingTp = vtp.get(index);
		for (; index < tripEndIndex; index++){
			TrackPoint tp = vtp.get(index);
			if (tp.isPausePoint())
				return index;					// pause point indicates segment end
			
			
			// break segment next time speed = 0 AND distance between points > segment length
			double speed = tp.getSpeed();
			double roundSpeed = Math.round(speed);
			int distance = startingTp.distanceBetween(tp);
			if (distance > pb.gps_speed_segment_length) 							
				if (roundSpeed == 0)
					return index;
			
		} // end for
		return tripEndIndex;
	}

	/*
	private int findSegmentStart(ArrayList<TrackPoint> vtp, int startingIndex, int tripEndIndex){
		for (int index = startingIndex; index < tripEndIndex; index++){
			TrackPoint tp = vtp.get(index);
			double speed = tp.getSpeed();
			double roundSpeed = Math.round(speed);
			if (roundSpeed >= 1){
				if (index != startingIndex)
					return index;
			}
		}
		return tripEndIndex;
	}
	 */

	private int findTripStart(ArrayList<TrackPoint> vtp, int startingIndex ){
		return findTripType(vtp, startingIndex, TrackPoint.STARTPOINT);
	}

	private int findTripEnd(ArrayList<TrackPoint> vtp, int startingIndex ){
		return findTripType(vtp, startingIndex, TrackPoint.ENDPOINT);
	}

	/*
	private int findTripPause(ArrayList<TrackPoint> vtp, int startingIndex ){
		return findTripType(vtp, startingIndex, TrackPoint.PAUSEPOINT);
	}

	private int findTripMidPoint(ArrayList<TrackPoint> vtp, int startingIndex ){
		return findTripType(vtp, startingIndex, TrackPoint.MIDPOINT);
	}
	 */

	private int findTripType(ArrayList<TrackPoint> vtp, int startingIndex, int type){
		int index;
		for (index = startingIndex; index < vtp.size(); index++){
			if (vtp.get(index).getTripType() == type ){
				return index;
			}
		}
		return -1;	
	}

	// Aligns trackPoint from vector -- acculumates distance -- (filter does not)
	private int alignTrackPoint(ArrayList<TrackPoint> vtp, int index){
		return deleteTrackPoint(vtp, index, TrackPoint.FILTERED_ALIGN, true, true);
	}

	// deletes trackPoint from calculation
	// if remove flag is true, trackPoint is removed from vector
	// if false, trackPoint is marked, but left in vector
	private int deleteTrackPoint(ArrayList<TrackPoint> vtp, int index, int reason, boolean removeFlag, boolean accumulateDistanceFlag){
		TrackPoint previousTp, nextTp, currentTp;
		if ((index == 0) || (index == vtp.size()-1)){
			index++;
			return index;		// don't remove 1st or last element
		}

		previousTp = vtp.get(index-1);
		currentTp = vtp.get(index);
		nextTp = vtp.get(index+1);	

		// is current trackpoint a last fix?
		if (currentTp.isLastFix())
			previousTp.setFixType(TrackPoint.LASTFIX);	// yes -- transfer to the previous trackpoint						
		// is current trackPoint an endPoint?
		if (currentTp.isEndPoint())
			previousTp.setTripType(TrackPoint.ENDPOINT);
		// is current trackPoint a cluster center ?
		if (currentTp.isClusterCenter() && previousTp.getLocationNumber() != -1)
			previousTp.setClusterFlag(TrackPoint.CLUSTERCENTER);


		if (currentTp.isFirstFix() || currentTp.isStartPoint()){
			// transfer values to next trackPoint
			nextTp.setFixType(currentTp.getFixType());			// transfer fix type
			nextTp.setClusterFlag(currentTp.getClusterFlag());	// transfer cluster flag
			nextTp.setTripType(currentTp.getTripType());		// transfer trip type 
			nextTp.setTripNumber(currentTp.getTripNumber());  // copy trip number and mode fjr 4/21
			nextTp.setTripMode(currentTp.getTripMode());		
		} 

		if (!removeFlag){
			// mark tp as invalid
			vtp.get(index).setFilterReason(reason);
			index++;
		}
		else {
			// prepare to remove fix
			previousTp.setDuration(nextTp.timeBetween(previousTp));	 	// update time at previous location
			if (currentTp.isFirstFix() || currentTp.isLoneFix()){
				// first fix - zero values
				nextTp.setBearing(-1);
				nextTp.setBearingDelta(0);
				nextTp.setDistance(0);
				nextTp.setSpeed(0);
			}
			else {
				// not firstFix -- update values
				nextTp.setBearing(nextTp.calBearing(previousTp));
				nextTp.setBearingDelta(nextTp.bearingChange(previousTp));
				nextTp.setElevationDelta(nextTp.elevationBetween(previousTp));
				if (accumulateDistanceFlag)
					nextTp.setDistance(nextTp.getDistance() + currentTp.getDistance()); // accumulate during alignment
				else {
					nextTp.setDistance(nextTp.calDistance(previousTp)); 
					nextTp.setSpeed(nextTp.speedBetween(previousTp));			// update SPEED 
				}
			}

			vtp.remove(index);												// remove current
		}
		return index;
	}
}
