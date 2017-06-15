package edu.ucsd.cwphs.palms.poi;

import java.util.ArrayList;

public class POIstatsList {

	private ArrayList<POIstats> psl = new ArrayList<POIstats>();
	
	public POIstats get(int index){
		if (index < 0 || index > psl.size())
			return null;
		else
			return psl.get(index);
	}
	
	public int size(){
		return psl.size();
	}
	
	public void addTimeAt(POI poi, int seconds){
		int index = getIndex(poi);
		if (index == -1){
			POIstats ps = new POIstats(poi);
			ps.addTimeAt(seconds);
			psl.add(ps);
		}
		else
			psl.get(index).addTimeAt(seconds);
	}
	
	public void addTimePassed(POI poi){
		int index = getIndex(poi);
		if (index == -1){
			POIstats ps = new POIstats(poi);
			ps.addPass();
			psl.add(ps);
		}
		else
			psl.get(index).addPass();
	}
	
	public void addVisit(POI poi){
		int index = getIndex(poi);
		if (index == -1){
			POIstats ps = new POIstats(poi);
			ps.addVisit();
			psl.add(ps);
		}
		else
			psl.get(index).addVisit();
	}
	
	private int getIndex(POI poi){
		for (int i = 0; i < psl.size(); i++){
			if (psl.get(i).isPlaceId(poi.getPlaceId()))
				return i;
		}
		return -1;			// not found
	}
	
}