package edu.ucsd.cwphs.palms.location;

import java.util.ArrayList;
import java.util.Date;

import edu.ucsd.cwphs.palms.gps.TrackPoint;

import edu.ucsd.cwphs.palms.util.EventLogger;

public class LocationProcessingR1 {
	private static final String VERSION = "0.1.0   11 Jan 2015";
	
	private String NAME = "";					// TEMP
	private int BUFFER = 300;					// TEMP
	
	private LocationDpuResultSet rs = new LocationDpuResultSet();	
	
	public String getVersion(){
		return "LocationProcessingR1 - Version " + VERSION;
	}
	
	public LocationDpuResultSet getResultSet(){
		return rs;
	}
	
	public String process(LocationDpuParameters pb, ArrayList<TrackPoint> vtp){
		String error = null;
		TrackPoint tp = null;
		int locationNumber = -1;
		int previousLocationNumber = -1;
		int tripDistance = 0, tripDuration = 0;
		Date tripDateTime = null;
		LocationDpuResult tripLR = null;
		

		for (int index=0; index<vtp.size(); index++){
			tp = vtp.get(index);
			locationNumber = tp.getLocationNumber();
			if (locationNumber < 1)
				continue;						// not at a location

			LocationDpuResult lr = rs.findLocation(locationNumber);
			if (lr == null){
				// location doesn't exist - create and add
				lr = new LocationDpuResult(locationNumber, NAME, tp.getLat(), tp.getLon(), tp.getEle(), BUFFER, tp.getDateTime());
				rs.addResult(lr);
			}

			if (tripLR == null){
				// user not in a trip
				if (tp.getTripNumber() > 0){
					EventLogger.logWarning("LocationProcessingR1 - trip detected without startPoint at:" + tp.getDateTimeStr());
				}
				
				if (tp.getTripType() == TrackPoint.STARTPOINT) {
					// got a trip start
					tripLR = lr;
					tripDuration = tp.getDuration();
					tripDistance = tp.getDistance();
					tripDateTime = tp.getDateTime();
				}
				else {
					// not a start -- user must be stationary
					if (previousLocationNumber == locationNumber)
						lr.addTime(tp.getDuration(), tp.getDateTime());	// same location
					else 
						lr.addVisit(tp.getDuration(), tp.getDateTime()); // new location
				}
			}
			else {
				// user is in trip
				if (tp.getTripNumber() < 1){
					EventLogger.logWarning("LocationProcessingR1 - trip ends without endPoint at:" + tp.getDateTimeStr());
					tripLR.addTripFrom(tripDistance, tripDuration, tripDateTime);
					tripLR = null;
					lr.addVisit(tp.getDuration(), tp.getDateTime()); // new location
				}
				if (tp.getTripType() == TrackPoint.ENDPOINT){
					// end of trip
					tripDuration = tripDuration + tp.getDuration();
					tripDistance = tripDistance + tp.getDistance();
					tripLR.addTripFrom(tripDistance, tripDuration, tripDateTime);
					tripLR = null;
					lr.addVisit(0, tp.getDateTime()); // new location
				}
		
		}
		previousLocationNumber = locationNumber;
	} // end for index

	return error;

}
		
}
