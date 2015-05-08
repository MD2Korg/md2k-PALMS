package edu.ucsd.cwphs.palms.trip;

import java.util.ArrayList;

public class TRIPdpuResultSet {

	private ArrayList<TRIPdpuResult> results = new ArrayList<TRIPdpuResult>();
	
	public int getSize(){
		return results.size();
	}
	
	public void addResult(TRIPdpuResult result){
		results.add(result);
	}
	
	public ArrayList<TRIPdpuResult> getResults(){
		return results;
	}
	
	public String toJSON(){
		StringBuilder json = new StringBuilder("{\"PALMS_trip_summary\":[");
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
