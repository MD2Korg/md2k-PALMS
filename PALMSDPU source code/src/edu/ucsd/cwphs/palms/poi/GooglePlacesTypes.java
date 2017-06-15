package edu.ucsd.cwphs.palms.poi;

public class GooglePlacesTypes {

	public static String types[] = {"accounting", "airport", "amusement_park", "aquarium", "art_gallery", "atm",
				"bakery", "bank", "bar", "beauty_salon", "bicycle_store", "book_store", "bowling_alley",
				"bus_station", "cafe", "campground",
				
				// deprecated but functional until 2/16/2017
				"food", "health"};

	public static boolean isValid(String type){
		if (type == null || type.length() == 0)
			return true;
		
		for (int i= 0; i<types.length; i++){
			if (type.equalsIgnoreCase(types[i]))
				return true;
		} // end for
		return false;
	}
	
	public static String returnTypes(){
		StringBuffer sb = new StringBuffer();
		for (int i= 0; i<types.length; i++){
			sb.append(types[i] + ",");
		}
		return sb.toString();
	}
}
