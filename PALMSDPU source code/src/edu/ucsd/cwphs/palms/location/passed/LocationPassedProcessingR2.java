package edu.ucsd.cwphs.palms.location.passed;

import java.util.ArrayList;
import java.util.Date;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.poi.POI;
import edu.ucsd.cwphs.palms.poi.POIdpu;
import edu.ucsd.cwphs.palms.poi.POIstatsList;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class LocationPassedProcessingR2 {
	private static final String VERSION = "0.3.0   08 Jun 2016";

	private LocationPassedDpuResultSet rs = new LocationPassedDpuResultSet();	

	public String getVersion(){
		return "LocationPassedProcessingR2 - Version " + VERSION;
	}

	public LocationPassedDpuResultSet getResultSet(){
		return rs;
	}

	public String process(LocationPassedDpuParameters pb, GPSTrack track){
		String error = null;
		POIdpu dpu= new POIdpu();
		int max = track.getSize();
		EventLogger.logEvent("LocationPassedDpu - Number of trackpoints:" + track.getSize());
		TrackPoint previousTp =track.getTrackPoint(0);
		int timeAt = 0;
		boolean countAsVisit = false;
		int index = 1;
		ArrayList<POI> poiList = null;

//		max = 15000;				// for debugging

		while  (index < max){
			TrackPoint currentTp = track.getTrackPoint(index);
			index++;
			int duration = currentTp.timeBetween(previousTp);
			int distance = currentTp.distanceBetween(previousTp);	
			int distanceCutoff = (int)Math.round((pb.minDistanceChange * (duration/60.0)));
		
			if (distance <= distanceCutoff){
				// still at same location
				timeAt = timeAt + currentTp.timeBetween(previousTp);
	//			EventLogger.logEvent("index:" + (index-1) + "  distance:" + distance + "  cutoff:" + distanceCutoff + "  timeAt:" + timeAt);
				if (timeAt > pb.minDurationAt)
					countAsVisit = true;
			}
			else {
				// check for LOS
				if (duration > (pb.LOSsec)){
					EventLogger.logEvent("LocationPassedProccessingR2 - LOS detected - duration = " + duration);
				}
				else {
					// on the move
	//				EventLogger.logEvent("index:" + (index-1) + "  distance:" + distance + "  cutoff:" + distanceCutoff + "  timeAt:" + timeAt + "  duration:" + duration + "  countAsVisit:" + countAsVisit);
					
					// if count as visit -- get closest POIs to previousTp, else get POIs along route
					if (countAsVisit)
						poiList = dpu.getPOI(previousTp.getLat(), previousTp.getLon(), pb.buffer, pb.types);
					else
						poiList = dpu.getPOIAlongRoute(previousTp.getLat(), previousTp.getLon(), 
								currentTp.getLat(), currentTp.getLon(), pb.buffer, pb.types);
					
					for (int i = 0; i < poiList.size(); i++){
						POI poi = poiList.get(i);
						if (countAsVisit) {
							rs.addVisit(poi, currentTp.getDateTime());
							rs.addTimeAt(poi, timeAt);
						}
						else
							rs.addTimesPassed(poi, currentTp.getDateTime(), currentTp.getDuration(), pb.minTimeBetweenPasses);			// adds POI to result set if it doesn't exist
					} // end for
				}
				timeAt = 0;
				countAsVisit = false;
			} // end else
			previousTp = currentTp;
		} // end while	
		return error;
	}	
}
