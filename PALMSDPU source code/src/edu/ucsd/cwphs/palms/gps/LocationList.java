package edu.ucsd.cwphs.palms.gps;

import java.util.ArrayList;

public class LocationList {
	private ArrayList<Location> vLoc = new ArrayList<Location>();

	
	public void add(String name, int priority, double lat, double lon, double ele, int range, int buffer){
		vLoc.add(new Location(name, priority, lat, lon, ele, range, buffer));
	}
	
	public void add(Location locPoint){
		vLoc.add(locPoint); 
		}
	
	public int size(){
		return vLoc.size();
	}
	
	public Location get(int index){
		return vLoc.get(index);
	}
	
	public void remove(int index){
		vLoc.remove(index);
	}
	
	public Location findLocationByName(String s){
		Location loc = null;
		for (int i=0; i<vLoc.size(); i++){
			loc = vLoc.get(i);
			if (loc.name.equalsIgnoreCase(s))
				return loc;
		}
		// reach end of list - location not found
		return null;
	}
	
	public String toString() {
		String s = "";
		int count = vLoc.size();
		if (count == 0)
			s = "Empty List\n";
		else {
			for (int i=0; i<count;i++)
				s = s + vLoc.get(i).toString();
		}
		return s;
	}
	
	public Location findClosestLocation(WayPoint wp){
		Location closestLoc = null, currentLoc = null;
		double closestDistance = 25000;		// slightly greater than circumference of earth
		double d = 0;
		int max = vLoc.size();
		if (max > 0){
			for (int i=0; i<max; i++){
				currentLoc = vLoc.get(i);
				d = currentLoc.calDistance(wp);
				if (d < closestDistance){
					closestDistance = d;
					closestLoc = currentLoc;
				}
			} // end for
		} // end if
		return closestLoc;
	}
	
	public Location findLocationWithinBuffer(Double lat, Double lon){
		WayPoint wp = new WayPoint(lat, lon, 0.0, "");
		Location closestLoc = findClosestLocation(wp);
		if (closestLoc != null){
			double d = closestLoc.calDistance(wp);
			if (d > closestLoc.getBuffer())
				closestLoc = null;
		} // end if
		return closestLoc;
	}
	
	public Location findNearbyLocationNotMe(Location me){
		Location closestLoc = null, loc = null;
		WayPoint wp = new WayPoint();
		double closestDistance = 25000;		// slightly greater than circumference of earth
		double d = 0;
		int max = vLoc.size();
		if (max == 0)
			return null;

			for (int i=0; i<max; i++){
				loc = vLoc.get(i);
				if (loc == me)
					continue;						// skip over me
				
				wp.lat = me.lat;
				wp.lon = me.lon;
				d = loc.calDistance(wp);
				if (d < closestDistance){
					closestDistance = d;
					closestLoc = loc;
				}
			} // end for

		if (closestDistance > me.getRange())
			return null;
		else
			return closestLoc;
	}
	
	public Location findNearbyLocationWithMoreReferences(Location me){
		Location loc = null;
		WayPoint wp = new WayPoint();
		double d = 0;
		int max = vLoc.size();
		if (max == 0)
			return null;

			for (int i=0; i<max; i++){
				loc = vLoc.get(i);
				if (loc == me)
					continue;						// skip over me
				
				wp.lat = me.lat;
				wp.lon = me.lon;
				d = loc.calDistance(wp);
				if (d < me.getRange())
					if (loc.getReferences() >= me.getReferences())
						return loc;
			} // end for
			return null;	// none found
	}
	
	public int purgeNearbyLocationsNotMe(Location me){
		Location loc = null;
		WayPoint wp = new WayPoint();
		double d = 0;
		int counter = 0;

			for (int i=0; i<vLoc.size(); i++){
				loc = vLoc.get(i);
				if (loc == me)
					continue;						// skip over me
				
				wp.lat = me.lat;
				wp.lon = me.lon;
				d = loc.calDistance(wp);
				if (d < me.getRange()){
					vLoc.remove(i);
					i--;				// adjust index for item removed
					counter++;
				}
			} // end for
		return counter;
	}
	
	public boolean purgeMe(Location me){
		Location loc = null;
			for (int i=0; i<vLoc.size(); i++){
				loc = vLoc.get(i);
				if (loc != me)
					continue;						// skip over others

				vLoc.remove(i);
				return true;
				
			} // end for
			System.out.print("LocationList.purgeMe - location not in list:" + loc.name);	
		return false;
	}
	
	public String findClosestLocationName(WayPoint wp){
		String rStr = "";
		Location loc = findClosestLocation(wp);
		if (loc != null)
			rStr = loc.name;
		return rStr;
	}
	
	public Location findNearbyLocation(TrackPoint tp){
			return findNearbyLocation((WayPoint) tp);
	}
	
	public Location findNearbyLocation(WayPoint wp){	// closest must be within range
		Location closest = findClosestLocation(wp);
			if (closest != null){
				double d = closest.calDistance(wp);
				if (d > closest.getRange())
					closest = null;				// closest not within range	
			}
		return closest;	
	}
	
	public String findNearbyLocationName(WayPoint wp){
		String rStr = null;
		Location loc = findNearbyLocation(wp);
		if (loc != null)
			rStr = loc.name;
		return rStr;
	}
	
	public void zero(){
		Location loc = null;
		int max = vLoc.size();
		if (max > 0){
			for (int i=0; i<max; i++){
				loc = vLoc.get(i);
				loc.zero();
				}
			} // end for
		} // end if
	}