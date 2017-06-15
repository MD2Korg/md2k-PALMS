package edu.ucsd.cwphs.palms.poi;

public class POIstats {

	private POI poi;
	private int timeAt = 0;
	private int numberOfPasses = 0;
	private int numberOfVisits = 0;					// no sure how to compute this yet
	
	POIstats(POI poi){
	 this.poi = poi;
	}
	
	public POI getPOI() {
		return poi;
	}
	
	public boolean isPlaceId(String placeId){
		if (poi.getPlaceId().equalsIgnoreCase(placeId))
			return true;
		else
			return false;
	}
	
	public void addPass(){
		numberOfPasses++;
	}
	
	public void addVisit(){
		numberOfVisits++;
	}
	
	public void addTimeAt(int seconds){
		timeAt = timeAt + seconds;
	}
	
	public int getTimeAt(){
		return timeAt;
	}
	
	public int getNumberOfPasses(){
		return numberOfPasses;
	}
	
	public int getNumberOfVisits(){
		return numberOfVisits;
	}
	
	public String CSVHeader(){
		String header = poi.CSVheader();
		return header + ",nPasses,nVisits,timeAt";
	}
	
	public String toCSV(){
		String s = poi.toCSV();
		return s + "," + numberOfPasses + "," + numberOfVisits + "," + timeAt;
	}
	
}
