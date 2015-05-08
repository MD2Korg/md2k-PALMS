package edu.ucsd.cwphs.palms.trip;

import java.util.ArrayList;
import java.util.Date;

import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.gps.WayPoint;

public class TRIPprocessingR1 {
	private static final String VERSION = "0.1.0   11 Jan 2015";
	private final int NODATA = 0;
	private final int NOGPS = 1;
	private final int STATIONARY = 2;
	private final int MOVING = 3;
	private final int PAUSED = 4;
	
	private TRIPdpuResultSet rs = new TRIPdpuResultSet();
	int duration = 0;
	int distance = 0;
	double avgSpeed = 0.0;
	int speedCounter = 1;
	int startLocationNumber = 0;
	double startLat = WayPoint.UNKNOWNCOORDINATES;
	double startLon = WayPoint.UNKNOWNCOORDINATES;
	double startEle = WayPoint.UNKNOWNELEVATION;
	int endLocationNumber = 0;
	Date startDate = null;
	int state = NODATA;
	
	
	
	public String getVersion(){
		return "TRIPprocessingR1 - Version " + VERSION;
	}
	
	
	public TRIPdpuResultSet getResultSet(){
		return rs;
	}
	
	public String process(TRIPdpuParameters pb, ArrayList<TrackPoint> vtp){
		String error = null;
		TrackPoint tp = null;
		
		for (int index=0; index<vtp.size(); index++){
			
			tp = vtp.get(index);
			int tripType = tp.getTripType();
			
			// TODO first switch on state to catch errors
			
			switch (tripType){
			
			case TrackPoint.STARTPOINT: {
				startTrip(tp);
				break;
			}
			
			case TrackPoint.ENDPOINT: {
				endTrip(tp);
				output(tp);
				break;
			}
			
			case TrackPoint.MIDPOINT: {
				continueTrip(tp);
				break;
			}
			
			case TrackPoint.PAUSEPOINT: {
				if (pb.includePausePoints);
					// TODO -- figure what to do
				else
					continueTrip(tp);
				break;
			}
			
			default:
				break;
			} // end switch (tripType)
			
			
		} // end for index
		
		return error;
		
	}
	
	private void startTrip(TrackPoint tp){
		duration = tp.getDuration();
		distance = tp.getDistance();
		avgSpeed = tp.getSpeed();
		speedCounter = 1;
		startDate = tp.getDateTime();
		startLocationNumber = tp.getLocationNumber();
		startLat = tp.getLat();
		startLon = tp.getLon();
		startEle = tp.getEle();
	}
	
	private void continueTrip(TrackPoint tp){
		duration = duration + tp.getDuration();
		distance = distance + tp.getDistance();
		avgSpeed = avgSpeed + tp.getSpeed();
		speedCounter++;
	}
	
	private void endTrip(TrackPoint tp){
		duration = duration + tp.getDuration();
		distance = distance + tp.getDistance();
		avgSpeed = avgSpeed + tp.getSpeed();
		speedCounter++;
		avgSpeed = avgSpeed / speedCounter;
		avgSpeed = Math.rint(avgSpeed);
	}
	
	private void output(TrackPoint tp){
		TRIPdpuResult result = new TRIPdpuResult(startDate, tp.getDateTime(), tp.getTripNumber(), duration, distance, tp.getTripMode(), avgSpeed,
				startLocationNumber, tp.getLocationNumber(), startLat, startLon, startEle, tp.getLat(), tp.getLon(), tp.getEle());
		rs.addResult(result);
	}
	
	
	private void resetTrip(){
		duration = 0;
		distance = 0;
		avgSpeed = 0.0;
		startDate = null;
		startLocationNumber = 0;
		startLat = WayPoint.UNKNOWNCOORDINATES;
		startLon = WayPoint.UNKNOWNCOORDINATES;
		startEle = WayPoint.UNKNOWNELEVATION;
		endLocationNumber = 0;
		
		state = STATIONARY;
	}
	
}
