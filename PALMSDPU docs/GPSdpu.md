# GPSDPU POST Format

**Function**

The GPSDPU accepts GPS track-points (timestamp, latitude, longitude, elevation), cleans and filters, adjusts epoch, and classifies by trip, location, mode of transportation, duration, distance traveled, etc.,  for a user-specified interval (typically 1 minute).

**Field Specification - POST Parameters**

*trk (json object)* - track -- collection of GPS fixes (see GPStrack.md)

*parameters (json object)* - parameters used by DPU (see GPSparameters.md)

POST
```json
{
"trk":{ insert trk object},
"parameters":{ insert parameter object}
}
```

**Field Specification - Result Parameters**

The GPSDPU produces the following output variables.    These variables are input to the Trip and Location DPUs.

*GPS_DPU_output_variables* - Array of results

*date_time (string)* - date and time in UTC

*dow (numeric)* - day of week as integer value (1 - Monday, 7-Sat)

*lat (numeric)* - latitude of participant's location at this time

*lon (numeric)* - longitude of participant's location at this time

*ele (numeric)* - elevation (in meters) at participant's location at this time

*duration (numeric)* - duration of interval in seconds

*distance (numeric)* - distance traveled during interval in meters

*speed (numeric)* - speed of travel during the interval (in KM/hr)

*bearing (numeric)* - direction of travel as computed between previous location and current location (in degrees 0-359, 0 = north)

*bearing_delta (numeric)* - change in direction (in degrees -180 to +180, positive value = clockwise change in direction)

*elevation_delta (numeric)* - change in elevation (in meters)

*fix_type (numeric)* - integer value representing type of GPS fix:
* -1 = unknown
* 0 = invalid
* 1 = valid (raw, unprocessed)
* 2 = first fix
* 3 = last fix
* 4 = last valid fix
* 5 = lone fix
* 6 = inserted fix

*iov (numeric)* - integer value indicating if trackpoint was indoors, outdoors, or in-vehicle:
* -1 = unknown
* 0 = outdoors
* 1 = indoors
* 2 = in-vehicle

*trip_number (numeric)* - integer number assigned to this trip. Numbers assigned sequentially as trips are detected. 0 indicates trackpoint is not part of a trip. >0 indicates trackpoint is part of a trip.

*trip_type (numeric)* - indicates start of trip, end of trip, pauses and mid-points:
* 0 = stationary
* 1 = start point
* 2 = midpoint
* 3 = pause point
* 4 = end point

*trip_mot (numeric)* - identifies estimated mode of transportation:
* -1 = unknown
* 0 = stationary (not in a trip)
* 1 = pedestrian
* 2 = bicycle
* 3 = vehicle

*location_number (numeric)* - integer number assigned to participant's current location.  Numbers assigned sequentially as locations are detected.  -1 = unknown location.

*location_cluster_flag (numeric)* - indicates if a trackpoint was clustered, that is the trackpoint was within a specified radius of the location and its coordinates were changed to that of the location’s center:
* 0 = not clustered
* 1 = point was clustered
* 2 = point is the center of the location cluster

SAMPLE RESULT
```json
{
"GPS_DPU_output_variables": [ {
 "date_time": "2014-11-18T10:10:00Z",
 "dow":2,
 "lat": "32.882468",
 "lon": "-117.234807",
 "ele": 45.3,
 "duration": 30,
 "distance": 100,
 "speed": 3.2,
 "bearing": 145,
 "bearing_delta": -10,
 "ele_delta": -3.0,
 "fix_type_code": 1,
 "iov": -1,
 "trip_number": 1,
 "trip_type": 1,
 "trip_mot": 1,
 "location_number": 0,
 "location_cluster_flag": 0
 } ]
}

```
