package edu.ucsd.cwphs.palms.gps;

import org.json.JSONObject;

import edu.ucsd.cwphs.palms.util.EventLogger;
import edu.ucsd.cwphs.palms.util.JSONPrettyPrint;

public class GPSdpuParameters {

	private static final String VERSION = "4.1.0   08 Dec 2014";
	
	// General values
	public int interval = 30; // interval between samples in seconds
	public boolean merge_insertMissingTrackpoints = true;
	public boolean merge_includeNoData = true;
	public boolean merge_insert_max = false;
	public int merge_insert_max_seconds = 600;			// default to gps_filter_max_los value

	// GPS TAB
	// *********************************************************************
	// Filter invalid values:
	public int gps_filter_invalid = 0; // 0 = filter, 1 = mark, 2 = include
	// (don't filter)
	public boolean gps_filter_boolean = true;		// external representation -- mark not used 
	
	public int gps_filter_max_speed = 130; // max speed allowed (in km/hour)
	public int gps_filter_max_elevation_delta = 1000; // max change in elevation
	// allowed between fixes (in meters)
	public int gps_filter_max_distance_delta = 5000; // max distance change between fixes (in
	// meters)
	public int gps_filter_min_distance_delta = 10; // min distance change over three fixes(in
	// meters)
	public int gps_filter_max_los = 600; // max loss of signal in seconds
	public boolean gps_filter_lonefixes = true;

	// Apply averages to:
	public boolean gps_average_speed = false; // speed
	public boolean gps_average_elevation = false; // elevation change
	public int gps_average_samples = 3; // number of samples to average

	// Detect location clusters
	public boolean gps_cluster_before_trips = false; // detect locations before
	// trips
	public boolean gps_cluster_include_pauses = false; // cluster pauses in trips
	public boolean gps_cluster_reset_locations_each_participant = false;
	public boolean gps_cluster_trap_indoors = true; // trap to location clusters if marked indoors
	public boolean gps_cluster_trap_outdoors = true;  // trap if marked as outdoors
	public boolean gps_cluster_trap_trips = false;  // trap even if part of trip   
	public int gps_cluster_radius = 30; // cluster radius in meters
	public int gps_cluster_min_duration = 0; // cluster if at location > nseconds
	public boolean gps_cluster_allow_wo_trips = false;  // allow locations without trips
	public int gps_cluster_iterations = 1000; // number of iterations
	public int gps_cluster_max = 100; // max number of clusters

	// Detect trips?
	public boolean gps_trip_detect = true;
	public int gps_trip_min_distance = 34; // min change in distance in meters
	// (over 60 seconds)
	// 1kph = 17 meters/minute,  2=34, 3=51
	public int gps_trip_min_length = 100; // min trip length (in meters)
	public int gps_trip_min_pause = 180; // min pause duration in seconds
	public int gps_trip_max_pause = 300; // max pause duration in seconds
	public int gps_trip_min_duration = 180; // min total trip duration (in seconds)
	public int gps_trip_percentage_one_location = 90;	// percentage of trip allowed within a single location
	public int gps_trip_percentage_indoors = 50;		// percentage of trip allowed indoors
	public boolean gps_trip_trim_indoor_points = true; // remove indoor points from start/stop of trips
	public boolean gps_trip_reconsider_segments = false;	// reconsider trips after segmenting
	
	
	// Detect indoors?
	public boolean gps_indoors_detect = true;
	public int gps_indoors_sat_ratio = 50; // max ratio of used/inview to be
	// considered indoors
	public int gps_indoors_max_snr = 250; // max value of snr to be considered
	// indoors

	// Speed cutoff values (in km/hour) 
	// min speeds faster than cutoff are classified as cutoff
	public int gps_speed_vehicle = 25;	// vehicle - 15.5 mph  (was 35 - 21.7 mph)
	public int gps_speed_bicycle = 10;	// bicycle - 6.2 mph
	public int gps_speed_run = 12; 		// running -- 7.4 mph
	public int gps_speed_jog = 9; 		// jogging -- 5.6 mph
	public int gps_speed_walk = 1; 		// walking
	public int gps_speed_sedentary = 0; // sedentary
	public int gps_speed_percentile = 90;	// this % of speeds must be less than cutoff							
	public int gps_speed_segment_length = 30;
	
	public boolean gps_trans_mode_useActivity = false;
	public boolean gps_trans_mode_useHeartrate = false;
	public boolean gps_trans_mode_reclassEntireTrip = false;
	public int gps_trans_mode_activityDuration = 60;		// must last this long or longer
	public int gps_trans_mode_activityIntensityLevel = 1;
	public int gps_trans_mode_heartRateIntensityLevel = 1;

	// Acceleration cutoff values (in km/hour)
	public int gps_acc_vehicle = 8; // vehicle  // experminental values - not verified
	public int gps_acc_bicycle = 3; // bicycle
	public int gps_acc_pedestrian = 1; // pedestrian
	
	// return version number
	public String getVersion(){
		return "PALMS Calculation ParameterBlock - Version " + VERSION;
	}
	
	
	public String toJSON(){
		StringBuilder sb = new StringBuilder("{\"parameters\":{");
		sb.append("\"general\":{");
		sb.append("\"interval\":" + interval + ",");
		sb.append("\"insert_missing\":" + merge_insertMissingTrackpoints + ",");
		sb.append("\"insert_until\":" + this.merge_insert_max + ",");
		sb.append("\"insert_max_seconds\":" + this.merge_insert_max_seconds + ",");
		sb.append("\"los_max_duration\":" + this.gps_filter_max_los);
		sb.append("},");
		
		sb.append("\"filter_options\":{");
		sb.append("\"remove_lone\":" + this.gps_filter_lonefixes + ",");
		sb.append("\"filter_invalid\":" + this.gps_filter_invalid + ",");
		sb.append("\"max_speed\":" + this.gps_filter_max_speed + ",");
		sb.append("\"max_ele_change\":" + this.gps_filter_max_elevation_delta + ",");
		sb.append("\"min_change_3_fixes\":" + this.gps_filter_min_distance_delta);
		sb.append("},");
		
		sb.append("\"detect_indoors\":{");
		sb.append("\"enabled\":" + this.gps_indoors_detect + ",");
		sb.append("\"max_sat_ratio\":" + this.gps_indoors_sat_ratio + ",");
		sb.append("\"max_SNR_value\":" + this.gps_indoors_max_snr);
		sb.append("},");
		
		sb.append("\"trip_detection\":{");
		sb.append("\"min_distance\":" + this.gps_trip_min_distance + ",");
		sb.append("\"min_trip_length\":" + this.gps_trip_min_length + ",");
		sb.append("\"min_trip_duration\":" + this.gps_trip_min_duration + ",");
		sb.append("\"min_pause_duration\":" + this.gps_trip_min_pause + ",");
		sb.append("\"max_pause_duration\":" + this.gps_trip_max_pause + ",");
		sb.append("\"max_percent_single_location\":" + this.gps_trip_percentage_one_location + ",");
		sb.append("\"max_percent_allowed_indoors\":" + this.gps_trip_percentage_indoors + ",");
		sb.append("\"remove_indoor_fixes\":" + this.gps_trip_trim_indoor_points);		
		sb.append("},");
		
		sb.append("\"location_detection\":{");
		sb.append("\"include_trip_pauses\":" + this.gps_cluster_include_pauses + ",");
		sb.append("\"trap_indoor_fixes\":" + this.gps_cluster_trap_indoors + ",");
		sb.append("\"trap_outdoor_fixes\":" + this.gps_cluster_trap_outdoors + ",");
		sb.append("\"trap_trip_fixes\":" + this.gps_cluster_trap_trips + ",");
		sb.append("\"allow_non_trips\":" + this.gps_cluster_allow_wo_trips + ",");
		sb.append("\"location_radius\":" + this.gps_cluster_radius + ",");
		sb.append("\"min_duration_at_location\":" + this.gps_cluster_min_duration);
		sb.append("},");
		
		sb.append("\"mode_of_transportation\":{");
		sb.append("\"vehicle_cutoff\":" + this.gps_speed_vehicle + ",");
		sb.append("\"bicycle_cutoff\":" + this.gps_speed_bicycle + ",");
		sb.append("\"walk_cutoff\":" + this.gps_speed_walk + ",");
		sb.append("\"percentile_to_sample\":" + this.gps_speed_percentile + ",");
		sb.append("\"min_segment_length\":" + this.gps_speed_segment_length);	
		sb.append("}");				// last block
		
		sb.append("}}");			// closing parameters:
		return sb.toString();
	}
	
	public String prettyPrint(){
		String s = JSONPrettyPrint.print(toJSON());
		return s;
	}
	
	public String fromJSON(String json){
		JSONObject obj = new JSONObject(json);
		return fromJSONObject(obj);
	}
			
	public String fromJSONObject(JSONObject obj){
		Integer i = null;
		Boolean b = null;
		String error = null;
		JSONObject parameters = obj.getJSONObject("parameters");
		if (parameters == null){
			EventLogger.logWarning("GPSdpuParameters.fromJSONObject - parameters not found");
			return "\"error\": \"DPU parameters not found\"";
		}
		
			try {
				JSONObject general = getJSONObject(parameters, "general");
				if (general != null){
					i = getJSONInt(general, "interval");
					if (i != null)
						interval = i;
					b = getJSONBoolean(general, "insert_missing");
					if (b != null)
						this.merge_insertMissingTrackpoints = b;
					b = getJSONBoolean(general, "insert_until");
					if (b != null)
						this.merge_insert_max = b;
					i = getJSONInt(general, "insert_max_seconds");
					if (i != null)
						this.merge_insert_max_seconds = i;
					i = getJSONInt(general, "los_max_duration");
					if (i != null)
						this.gps_filter_max_los = i;
				}

				JSONObject filter = getJSONObject(parameters, "filter_options");
				if (filter != null){
					b = getJSONBoolean(filter,"remove_lone");
					if (b != null)
						this.gps_filter_lonefixes = b;
					i = getJSONInt(filter, "filter_invalid");
					if (i != null)
						this.gps_filter_invalid = i;
					i = getJSONInt(filter, "max_speed");
					if (i != null)
						this.gps_filter_max_speed = i;
					i = getJSONInt(filter, "max_ele_change");
					if (i != null)
						this.gps_filter_max_elevation_delta = i;
					i = getJSONInt(filter, "min_change_3_fixes");
					if (i != null)
						this.gps_filter_min_distance_delta = i;
				}
				
				JSONObject indoors = getJSONObject(parameters, "detect_indoors");
				if (indoors != null){
					b = getJSONBoolean(indoors,"enabled");
					if (b != null)
						this.gps_indoors_detect = b;
					i = getJSONInt(indoors, "max_sat_ratio");
					if (i != null)
						this.gps_indoors_sat_ratio = i;
					i = getJSONInt(indoors, "max_SNR_value");
					if (i != null)
						this.gps_indoors_max_snr = i;
				}
				
				JSONObject trip = getJSONObject(parameters, "trip_detection");
				if (trip != null){
					i = getJSONInt(trip, "min_distance");
					if (i != null)
						this.gps_trip_min_distance = i;
					i = getJSONInt(trip, "min_trip_length");
					if (i != null)
						this.gps_trip_min_length = i;
					i = getJSONInt(trip, "min_trip_duration");
					if (i != null)
						this.gps_trip_min_duration = i;
					i = getJSONInt(trip, "min_pause_duration");
					if (i != null)
						this.gps_trip_min_pause = i;
					i = getJSONInt(trip, "max_pause_duration");
					if (i != null)
						this.gps_trip_max_pause = i;
					i = getJSONInt(trip, "max_percent_single_location");
					if (i != null)
						this.gps_trip_percentage_one_location = i;
					i = getJSONInt(trip, "max_percent_allowed_indoors");
					if (i != null)
						this.gps_trip_percentage_indoors = i;
					b = getJSONBoolean(trip,"remove_indoor_fixes");
					if (b != null)
						this.gps_trip_trim_indoor_points = b;
				}
				
				JSONObject location = getJSONObject(parameters, "location_detection");
				if (location != null){
					b = getJSONBoolean(location, "include_trip_pauses");
					if (b != null)
						this.gps_cluster_include_pauses = b;
					b = getJSONBoolean(location, "trap_indoor_fixes");
					if (b != null)
						this.gps_cluster_trap_indoors = b;
					b = getJSONBoolean(location, "trap_outdoor_fixes");
					if (b != null)
						this.gps_cluster_trap_outdoors = b;
					b = getJSONBoolean(location, "trap_trip_fixes");
					if (b != null)
						this.gps_cluster_trap_trips = b;
					b = getJSONBoolean(location, "allow_non_trips");
					if (b != null)
						this.gps_cluster_allow_wo_trips = b;
					i = getJSONInt(location, "location_radius");
					if (i != null)
						this.gps_cluster_radius = i;
					i = getJSONInt(location, "min_duration_at_location");
					if (i != null)
						this.gps_cluster_min_duration = i;
				}
				
				JSONObject mot = getJSONObject(parameters, "mode_of_transportation");
				if (mot != null){
					i = getJSONInt(mot, "vehicle_cutoff");
					if (i != null)
						this.gps_speed_vehicle = i;
					i = getJSONInt(mot, "bicycle_cutoff");
					if (i != null)
						this.gps_speed_bicycle = i;
					i = getJSONInt(mot, "walk_cutoff");
					if (i != null)
						this.gps_speed_walk = i;
					i = getJSONInt(mot, "percentile_to_sample");
					if (i != null)
						this.gps_speed_percentile = i;
					i = getJSONInt(mot, "min_segment_length");
					if (i != null)
						this.gps_speed_segment_length = i;
				}				
			}
			catch (Exception ex){
				EventLogger.logException("GPSdpuParameters - error parsing JSON" , ex);
				error = "\"error\": \"error parsing JSON\"";
			}
		return error;
	}
	
	private JSONObject getJSONObject(JSONObject obj, String key){
		JSONObject returnObj = null;
		try {
			returnObj = obj.getJSONObject(key);
		}
		catch (Exception ex){
		}
		return returnObj;
	}

	private String getJSONString(JSONObject obj, String key){
		String s = null;
		try {
			s = obj.getString(key);
		}
		catch (Exception ex){
		}
		return s;
	}

	private Double getJSONDouble(JSONObject obj, String key){
		Double d = null;
		try {
			d = obj.getDouble(key);
		}
		catch (Exception ex){
		}
		return d;
	}
	
	private int getJSONInt(JSONObject obj, String key){
		Integer i = null;
		try {
			i = obj.getInt(key);
		}
		catch (Exception ex){
		}
		return i;
	}
	
	private Boolean getJSONBoolean(JSONObject obj, String key){
		Boolean b = null;
		try {
			b = obj.getBoolean(key);
		}
		catch (Exception ex){
		}
		return b;
	}
	
}
