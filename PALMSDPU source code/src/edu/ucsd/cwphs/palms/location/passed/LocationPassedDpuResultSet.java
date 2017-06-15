package edu.ucsd.cwphs.palms.location.passed;

import java.util.ArrayList;
import java.util.Date;

import edu.ucsd.cwphs.palms.poi.POI;

public class LocationPassedDpuResultSet {
	
private ArrayList<LocationPassedDpuResult> results = new ArrayList<LocationPassedDpuResult>();
	
	public LocationPassedDpuResult get(int index){
		if (index < 0 || index > results.size())
			return null;
		else
			return results.get(index);
	}
	
	public int size(){
		return results.size();
	}
	
	public void addTimeAt(POI poi, int seconds){
		int index = getIndex(poi);
		if (index == -1){
			LocationPassedDpuResult ps = new LocationPassedDpuResult(poi);
			ps.addTimeAt(seconds);
			results.add(ps);
		}
		else
			results.get(index).addTimeAt(seconds);
	}
	
	public void addTimesPassed(POI poi, Date currentDate, int duration, int minTimeBetween){
		int index = getIndex(poi);
		if (index == -1){
			LocationPassedDpuResult ps = new LocationPassedDpuResult(poi);
			ps.addPass(currentDate, duration, minTimeBetween);
			results.add(ps);
		}
		else
			results.get(index).addPass(currentDate, duration, minTimeBetween);
	}
	
	public void addVisit(POI poi, Date currentDate){
		int index = getIndex(poi);
		if (index == -1){
			LocationPassedDpuResult ps = new LocationPassedDpuResult(poi);
			ps.addVisit(currentDate);
			results.add(ps);
		}
		else
			results.get(index).addVisit(currentDate);
	}
	
	public String toJSON(){
		StringBuilder json = new StringBuilder("{\"PALMS_locations_passed\":[{");
		int size = results.size();
		if (size > 0){
			for (int index = 0; index < (size-1); index++){
				json.append(results.get(index).toJSON() + ",");
			} // end for
			json.append(results.get(size-1).toJSON());	// last item
		}
		json.append("}]}");
		return json.toString();
	}
	
	public String toCSV(){
		StringBuilder sb = new StringBuilder();
			for (int index = 0; index < results.size(); index++){
				sb.append(results.get(index).toCSV() + "\n");
			} // end for
		return sb.toString();
	}
	
	
	private int getIndex(POI poi){
		int index = -1;				// assume not found
		for (int i = 0; i < results.size(); i++){
			if (results.get(i).isPlaceId(poi.getPlaceId()))
				index = i;			// set to index of last element in result set
		}
		return index;			
	}
	
}
