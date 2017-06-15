package edu.ucsd.cwphs.palms.location;

import java.util.ArrayList;

public class LocationDpuResultSet {
	
	ArrayList<LocationDpuResult> results = new ArrayList<LocationDpuResult>();
	
	public int getSize(){
		return results.size();
	}
	
	public void addResult(LocationDpuResult result){
		results.add(result);
	}
	
	public ArrayList<LocationDpuResult> getResults(){
		return results;
	}
	
	public LocationDpuResult get(int index){
		if (index<0 || index > results.size())
			return null;
		return results.get(index);
	}
	
	public LocationDpuResult findLocation(int locationNumber){ 
		for (int index=0; index<results.size(); index ++){
			if (results.get(index).getLocationNumber() == locationNumber)
				return results.get(index);
		}
		return null;
	}
	
	public String toJSON(){
		StringBuilder json = new StringBuilder("{\"PALMS_location_summary\":[");
		int size = results.size();
		if (size > 0){
			for (int index = 0; index < (size-1); index++){
				json.append(results.get(index).toJSON() + ",");
			} // end for
			json.append(results.get(size-1).toJSON());	// last item
		}
		json.append("]}");
		return json.toString();
	}
	
	public String toCSV() {
		StringBuilder sb = new StringBuilder("no results found.");	
		int size = results.size();
		if (size > 0){
			sb = new StringBuilder(results.get(0).CSVheader() + "\n");
			for (int index = 0; index < size; index++){
				sb.append(results.get(index).toCSV() + "\n");
			} // end for
		} // end if
		return sb.toString();
	}

}
