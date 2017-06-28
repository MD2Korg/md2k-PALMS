# TRIPDPU POST Format

**Function**

The TRIPDPU accepts the output of the GPS DPU, identifies the trips and summaries each trip.

**Field Specification - POST Parameters**

*GPS_DPU_output_variables (json object)* - output of GPSDPU (refer to GPSdpu.md)

*parameters (json object)* - parameters used by DPU (refer to TRIPparameters.md)

POST
```json
{
"GPS_DPU_output_variables":{ insert GPS output object},
"parameters":{ insert parameter object}
}
```

**Field Specification - Result Parameters**

The TRIPDPU produces the following output variables.

*GPS_DPU_output_variables* - Array of results

*time_inveral (json object)* -

*start_date_time (string)* - trip start date and time in UTC

*end_date_time (string)* - trip end date and time in UTC

*duration (numeric)* - duration of interval in seconds

*distance (numeric)* - distance traveled during interval in meters

*trip_mot (numeric)* - identifies estimated mode of transportation:
* -1 = unknown
* 0 = stationary (not in a trip)
* 1 = pedestrian
* 2 = bicycle
* 3 = vehicle

*average_speed (numeric)* - average speed of travel during the trip (in KM/hr)

*trip_number (numeric)* - integer number assigned to this trip. Numbers assigned sequentially as trips are detected. 0 indicates trackpoint is not part of a trip. >0 indicates trackpoint is part of a trip.

*start_location_number (numeric)* - location number at start of trip (departure location)

*end_location_number (numeric)* - location number at end of trip (arrival location)

*start_lat (numeric)* - latitude of starting location

*start_lon (numeric)* - longitude of starting location

*start_ele (numeric)* - elevation (in meters) of starting location

*end_lat (numeric)* - latitude of ending location

*end_lon (numeric)* - longitude of ending location

*end_ele (numeric)* - elevation (in meters) of ending location 

SAMPLE RESULT
```json
{
"PALMS_trip_summary": [ {
		"time_interval":  {
			"start_date_time":"2014-11-18T10:10:00Z",
			"end_date_time":"2014-11-18T10:12:00Z"
		},
		"duration": 120,
		"trip_mot": 1,
		"distance": 123,
		"trip_number": 1,
		"average_speed": 3.4,
		"start_location_number": 4,
		"end_location_number":15,
		"start_lat": 32.3456,
		"start_lon":-117.34343,
		"start_ele": 4.5,
		"end_lat": 32.3456,
		"end_lon":-117.34343,
		"end_ele": 4.5
	} ]
}
```
