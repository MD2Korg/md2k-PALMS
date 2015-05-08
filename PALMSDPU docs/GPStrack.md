# GPS Track Format

**Function**

GPS Track is the input to the GPS processing routines.  The schema borrows on GPX, a GPS Exchange Format that is XML based.  (www.topografix.com/gpx.asp).  It incorporates a unique feature of the popular Qstarz GPS data loggers (http://www.qstarz.com/CommercialGPS_Products.html) – the reporting of the relative signal strength of each satellite in view.  This feature, if present, is used by the algorithm to estimate if the position is indoors or outdoors.

**Field Specification**

*trk (json array)* - An array of trackpoints that describe movement through space and time.

*name (string)* - Name of track (optional)

*trpkts (json object)* - An individual location reported by the GPS

*date_time (string)* - Date and time of the GPS fix reported in UTC time (required)

*lat (numeric)* - Location's latitude in degrees decimal (required)

*lon (numeric)* - Location's longitude in degrees decimal (required)

*ele (numeric)* - elevation in meters (optional)

*nstat (string)* - Number of satellites used / Number of satellites in view (optional)

*qstat_info (string)* - Relative signal strength for each satellite reported by Qstarz dataloggers (optional)

**Example**
```json
{
"trk": {
"name":"Sample Track",
"trkpts": [
		{
		"lat": 32.882468,
		"lon": -117.234807,
		"ele": 304.5,
		"date_time":"2014-02-05T07:25:00Z",
		"nsat":"4/ 9",
		"qsat_info":"#17-19;#30-25;09-00;#12-18;27-00;05-00;29-00;10-00"
		},
		{
		"lat": 32.8823012,
		"lon": -117.235039,
		"ele": 324.2,
		"date_time":"2014-02-05T07:25:30Z",
 	     "nsat":"6/ 9",
		"qsat_info":"#17-19;#30-25;09-00;#12-18;27-00;05-00;29-00;10-00"
		}
		]
     }
}

```
