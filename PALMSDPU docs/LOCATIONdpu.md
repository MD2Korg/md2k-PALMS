# LOCATION DPU POST Format

**Function**

The LOCATION DPU accepts the output of the GPS DPU, identifies the locations and summaries each location.  Point of Interest (POI) details are provided for each location.  Current schema and example shows the POI details provided by the Google Places and Yelp APIs.

**Field Specification - POST Parameters**

*GPS_DPU_output_variables (json object)* - output of GPSDPU (refer to GPSdpu.md)

*parameters (json object)* - parameters used by DPU (refer to LOCATIONparameters.md)

POST
```json
{
"GPS_DPU_output_variables":{ insert GPS output object},
"parameters":{ insert parameter object}
}
```

**Field Specification - Result Parameters**

The LOCATION DPU produces the following output variables.

*GPS_DPU_output_variables* - Array of results

*location_number (numeric)* - integer number assigned to participant's current location.  Numbers assigned sequentially as locations are detected.  -1 = unknown location.

*lat (numeric)* - latitude of location

*lon (numeric)* - longitude of location

*ele (numeric)* - elevation (in meters) of location

*buffer (numeric)* - radius of buffer around location (in meters)

*duration_at (numeric)* - total duration spent at location in seconds

*number_visits (numeric)* - total number of visits to location (trip arrivals)

*trips_from (numeric)* - total number of trips departing from location

*trips_average_distance (numeric)* - average distance of trips departing from location (in meters)

*trips_max_distance (numeric)* - distance traveled of longest trip departing from location (in meters)

*trips_average_duration (numeric)* - average duration of trips departing from location (in seconds)

*trips_max_duration (numeric)* - duration of longest trips departing from location (in seconds)  

*earliest_date_time (string)* - date and time in UTC of first visit to location

*latest_date_time (string)* - date and time in UTC of last visit to location

*poi_details (json object)* - location's POI details (if available) See POIdpu.md for defination.

SAMPLE RESULT
```json
{
	"PALMS_location_summary": [ {
		"location_number":1,
		"name":"office",
		"lat": "32.882468",
		"lon": "-117.234807",
		"ele": 45.4,
		"buffer": 300,
		"duration_at": 13405,
		"number_visits":12,
		"trips_from":10,
		"trips_average_distance": 10,
		"trips_max_distance": 12345,
		"trips_average_duration":300,
		"trips_max_duration": 343455,
		"earliest_date_time": "2013-11-18T10:10:00Z",
		"latest_date_time": "2014-11-18T14:12:00Z",
		"poi_details": {
     	"types": ["restaurant", "food", "establishment"],
            	"place_id" : "ChIJyWEHuEmuEmsRm9hTkapTCrk",
            	"scope":"Google",
            	"vicinity": "UCSD Atkinson Hall",
            	"postal_address": "Voigt Drive, La Jolla, CA 92093"
	}
	} ]
}

```
