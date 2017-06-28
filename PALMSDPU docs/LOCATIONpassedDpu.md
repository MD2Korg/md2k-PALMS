# LOCATION Passed DPU POST Format

**Function**

The LOCATION DPU accepts a GPX Track file (or JSON representation), identifies the locations passed along that track.  It can be used to identify all tobacco outlets along a give track, the number of times each location was visited or passed and the amount of time at or in the vicinity of each location.  Point of Interest (POI) details can be provided for each location.  Current schema and example shows the POI details provided by the Google Places and Yelp APIs.

**Field Specification - POST Parameters**

*track (json object)* - output of GPSDPU (refer to GPStrack.md)

*parameters (json object)* - parameters used by DPU (refer to LOCATIONpassedDPUparameters.md)

POST
```json
{
"track":{ insert GPStrack object},
"location_passed_dpu_parameters":{ insert parameter object}
}
```

**Field Specification - Result Parameters**

The LOCATION Passed DPU produces the following output variables.

*LOCATION_passed_DPU_output_variables* - Array of results

*poi_details (JSON object)* - Details about the POI

*place_id (uuid)* - Unique identifier of POI

*name (string)* - Name of POI

*source (string)* - Source of information (Google, Yelp, Local)

*types (string)* - Classification of POI (seperated by | )

*postal_address (string)* - Postal Address of POI

*vicinity (string)* - Name of neighborhood or general area; often the same as postal_address

*lat (numeric)* - Latitude of POI

*lon (numeric)* - Longitude of POI

*distance (numeric)* - distance from closest trackpoint (in meters)

*nvisits (numeric)* - number of times person visited the location

*npasses (numeric)* - number of times person passed the location

*timeat (numeric)* - amount of time (in seconds) a person spent at the location

*timewithin (numeric)* - amount of time (in seconds) a person spent nearby the location (within a specified distance of the location).

SAMPLE RESULT
```json
{
	"location_passed": [ {
			"poi_details":
	  			{"place_id":"fd6ff35cb457c863655db3d2e339981b32b1c725",
	  			"name":"Burger King",
	  			"source":"Google",
	  			"types":"restaurant|food|establishment",
	  			"vicinity":"University Centers - 0076, 9500 Gil, La Jolla",
	  			"postal_address":"University Centers - 0076, 9500 Gil, La Jolla",
	  			"lat":32.884373,
	  			"lon":-117.233849,
	  			"distance": 16},
			"nvisits": 2,
			"npasses:":	12,
			"timeat":	324,
			"timewithin": 1233
	}
	} ]
}

```
