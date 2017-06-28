# POIDPU POST Format

**Function**

The POIDPU returns a list of Points of Interest (POIs) within a given radius (buffer) of a specified location (lat/lon).  Optionally, the desired types of POIs can be specified.

The DPU uses the publicly available POI web services provided by Google and YELP,
as well as a user provided database of locally collected POIs (i.e.: food outlets, advertisements,  smoking locations).   

**Field Specification - POST Parameters**

*lat (numeric)* - Location's latitude in degrees decimal

*lon (numeric)* - Location's longitude in degrees decimal

*buffer (numeric)* - Desired radius around location in meters

*types (string)* - Type of POI to be returned (optional);  multiple types are separated by '|'  If *types* are not specified, all POIs will be returned

POST
```json
{
"lat":32.88263,
"lon":-117.23523,
"buffer":300,
"types":"food"
}
```

**Field Specification - Result Parameters**

*poi_results* - Array of poi_details

*poi_detail* - Details about the POI

*place_id (uuid)* - Unique identifier of POI

*name (string)* - Name of POI

*source (string)* - Source of information (Google, Yelp, Local)

*types (string)* - Classification of POI (seperated by | )

*postal_address (string)* - Postal Address of POI

*vicinity (string)* - Name of neighborhood or general area; often the same as postal_address

*lat (numeric)* - Latitude of POI

*lon (numeric)* - Longitude of POI

*distance (numeric)* - distance from closest trackpoint (in meters)

RESULT
```json
 {
 "poi_results":
 ["poi_details":
 {"place_id":"fd6ff35cb457c863655db3d2e339981b32b1c725",
 "name":"Burger King",
 "source":"Google",
 "types":"restaurant|food|establishment",
 "vicinity":"University Centers - 0076, 9500 Gil, La Jolla",
 "postal_address":"University Centers - 0076, 9500 Gil, La Jolla",
 "lat":32.884373,
 "lon":-117.233849,
 "distance": 16},
"poi_details": {"place_id":"22efdc5a61530ba090c3824d23a51763770b43fa",
"name":"Earl's Place and Market",
"source":"Google",
"types":"grocery_or_supermarket|food|store|establishment",
"vicinity":"San Diego",
"postal_address":"San Diego",
"lat":32.883845,
"lon":-117.233278,
 "distance": 10}]
 }
```
