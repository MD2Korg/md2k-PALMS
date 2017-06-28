# Location Passed Parameters Format

**Function**

The Location Passed DPU identifies locations of interest along a given GPS track that a person passed or visited.  It utilizes numerous cutoff values define by these parameters.   Each has a default value that will be used unless overridden by a value specified.  Therefore all of the properties below are optional.

**Field Specification**

*location_passed_dpu_parameters (json object)* - identifies parameters

*buffer (integer)* - buffer size in meters.  POIs within this distance from the trackpoints will be identified as a location. By default, POIs within 30 meters of the track will be returned.

*types (String)* - limits locations to POIs of the specified type. If not specified, all types will be returned by default.

*min_duration_at (integer)* - minimum amount of time (in seconds) that the person needs to spend within <buffer> distance of the POI to be counted as a visit. By default, 180 seconds.

*min_distance_change (integer)* - minimum distance between trackpoints in meters.  By default set to 68, which is the distance traveled when walking 4 Km per hour.  This cutoff is used to calculate when a person leaves the vicinity of a location.

*min_time_between_passes (integer)* - minimum amount of time (in seconds) that must elapse between the times a person passes a location.  By default, set to 60 seconds.

*detail (boolean)* - if true, provide POI details

*debug (boolean)* - if true, include extra debug information in the log file.


**Example - Default values are shown**
```json
{
	"location_passed_dpu_parameters":
		{
			"buffer": 30,
			"min_duration_at": 180,
			"min_distance_change": 68,
			"min_time_between_passes": 60,
			"detail": true,
			"debug": false
		}
}
```
