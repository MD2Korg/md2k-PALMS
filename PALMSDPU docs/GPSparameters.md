# GPS Parameters Format

**Function**

The GPS DPU contains numerous options and cutoffs organized by function.   Each has a default value that will be used unless overridden by a value specified.  Therefore all of the properties below are optional.

**Field Specification**

*parameters (json object)* - identifies parameters

*general (json object)* - general purpose parameters

*interval (numeric)* - duration of interval between results in seconds

*insert_missing (boolean)* - if true, gaps in GPS fixes are replaced by the last valid fix

*insert_until (boolean)* - if true, inserts until a max time is reached

*insert_max_seconds (numeric)* - max number of seconds to replace missing fixes with last valid fix

*los_max_duration (numeric)* - max amount of time allowed to pass before Loss of Signal is declared

*filter_options (json object)* - parameters used to detect and remove invalid fixes

*remove_lone (boolean)* - if true, removes lone fixes

*filter_invalid (boolean)* - if true, removes invalid fixes

*max_speed (numeric)* - Consider fix invalid if speed is greater than this value (in km/hr)

*max_elevation_change (numeric)* - Consider fix invalid if elevation change is greater than this value (in meters)

*min_change_3_fixes (numeric)* - Consider fix invalid if change in distance between fix 1 and 3 is less than this value (in meters).  This helps remove GPS jitter.

*detect_indoors (json object)* - parameters used to estimate if fix is indoors based on additional satellite info provided by some GPS devices.

*enabled (boolean)* - If true, mark position as indoors, outdoors or in-vehicle.

*max_sat_ratio (numeric)* -

*max_snr_value (numeric)* -

*trip_detection (json object)* - parameters used to detect trips

*min_distance (numeric)* - minimum distance (in meters) that must be travelled over one minute to indicate the start of a trip.  Default choosen to be 34 meters which is equal to a typical walking speed of 2KM/hr.

*min_trip_length (numeric)* - trips less than this distance (in meters) are not considered trips.

*min_trip_duration (numeric)* - trips less than this duration (in seconds) are not considered trips.

*min_pause_duration (numeric)* - when the duration at a location exceeds this value, the point is marked as a pause point.

*max_pause_duration (numeric)* - when the duration of a pause exceeds this value, the point is marked as an end point.

*max_percent_single_location (numeric)* - maximum percentage of a trip's fixes that can occur at a single location.

*max_percent_allowed_indoors (numeric)* - maximum percentage of a trip that is allowed indoors.

*remove_indoor_fixes (boolean)* - if true, points at the start and end of a trip that are marked indoors are removed from the trip.

*location_detection (json object)* - parameters used to detect locations

*include_trip_pauses (boolean)* - if true, include trip pause points as locations.

*trap_indoor_fixes (boolean)* - if true, stationary indoor fixes within a given radius of the location will be set to the center of the location.

*trap_outdoor_fixes (boolean)* - if true, stationary outdoor fixes will be set to the location center.

*trap_trip_fixes (boolean)* - if true, also include fixes that are part of trips.

*allow_non_trips (boolean)* - if true, locations may be included that are not part of a trip.

*location_radius (numeric)* - defines radius (in meters) of location in which fixes are trapped.

*min_duration_at_location (numeric)* - minimum amount of time (in seconds) that must be spent at a location for it to be considered a location.

*mode_of_transportation (json object)* - parameters used to estimate mode of transportation (based on speed cutoffs)

*vehicle_cutoff (numeric)* - speeds greater than this value (in KM/hr) will be marked as vehicle.

*bicycle_cutoff (numeric)* - speeds greater than this value (in KM/hr) will be marked as bicycle.

*walk_cutoff (numeric)* - speeds greater than this value (in KM/hr) will be marked as pedestrian.

*percentile_to_sample (numeric)* - speed comparisons are made at this percentile.

*min_segment_length (numeric)* - minimum length (in meters) of segments used to classify mode of transportation.


**Example - Default values are shown**
```json
{
	"parameters":
		{
		"general": {
			"interval": 30,
			"insert_missing": true,
			"insert_until": false,
			"insert_max_seconds": 600,
			"los_max_duration": 60
},
		"filter_options": {
			"remove_lone":false,
			"filter_invalid": true,
			"max_speed":130,
			"max_ele_change":1000,
			"min_change_3_fixes":10
		},
		"detect_indoors":  {
"enabled":  true,
			"max_sat_ratio": 50,
			"max_SNR_value": 250
			},
		"trip_detection": {
			"min_distance" : 34,
			"min_trip_length": 100,
			"min_trip_duration": 180,
			"min_pause_duration": 180,
			"max_pause_duration": 300,
			"max_percent_single_location": 90,
			"max_percent_allowed_indoors": 50,
			"remove_indoor_fixes": true
},
		"location_detection": {
			"include_trip_pauses": false,
			"trap_indoor_fixes": true,
			"trap_outdoor_fixes":true,
			"trap_trip_fixes": false,
			"allow_non_trips": false,
			"location_radius": 30,
			"min_duration_at_location": 0
			},
		"mode_of_transportation": {
			"vehicle_cutoff": 25,
			"bicycle_cutoff": 10,
			"walk_cutoff": 1,
			"percentile_to_sample": 90,
			"min_segment_length": 30
		}
	}
}


```
