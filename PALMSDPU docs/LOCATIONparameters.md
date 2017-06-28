# Location Parameters Format

**Function**

The Location DPU contains numerous options.   Each has a default value that will be used unless overridden by a value specified.  Therefore all of the properties below are optional.

**Field Specification**

*location_dpu_parameters (json object)* - identifies parameters

*include_stationary (boolean)* - if true, points in GPSdpu output that are marked as stationary are included as locations.

*include_pause (boolean)* - if true, points in the GPSdpu output that are marked as pause points are included as locations.

*include_pois (boolean)* - if true, POI details are included in the output



**Example - Default values are shown**
```json
{
	"location_dpu_parameters":
		{
			"include_stationary": true,
			"include_pause": false,
			"include_pois": true
		}
}
```
