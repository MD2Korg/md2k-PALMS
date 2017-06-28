# MD2K PALMS LOcation DPUs Test Programs

**Overview**

The programs in the palms.test package are used to test the DPUs, typically by
debugging within the Eclipse IDE. The The programs are examples of directly calling
the DPU code from within a Java program.

### TestLocationPassed

This program tests the LocationPassed DPU which returns information about POIs
an individual passed or visted from that individual's GPS track.  

**Inputs**

  fileName -- path to GPX or CVS file containing the track to be processed
  parmFile -- path to JSON file containing parameters; if NULL, default parameters are used
  PALMSprocessing -- if TRUE, PALMS DPU is used to pre-process and clean the track

  GPX example:
  ```
  <?xml version='1.0' encoding='UTF-8'?>
  <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
   <trk>
   <trkseg>
    <trkpt lat="41.8869387" lon="-87.6282953">
      <time>2015-08-27T22:00:44Z</time>
    </trkpt>
    <trkpt lat="41.8870403" lon="-87.6283353">
       <time>2015-08-27T22:00:51Z</time>
     </trkpt>
     </trkseg>
  </trk>
  </gpx>
```
CSV example:
```
  Timestamp, Latitude, Longitude, Elevation
  2015-08-27T22:00:44Z, 41.8869387, -87.6282953, 0
  2015-08-27 22:00:51, 41.8870403, -87.6283353, 0
```
  Note: Timestamps accepted without T,Z  are assumed to be in local time.
	Set elevation to 0 if unknown.

Processing parameters as a JSON file:
```
  {
  "location_passed_dpu_parameters": {
    "buffer": 50,
    "types": "tobacco",
    "min_distance_traveled": 68,
    "min_duration_at": 180,
    "min_time_between_passes": 60,
    "debug": false,
    "detail": false
    }
  }
```
  *buffer*  - search distance (in meters) from current GPS fix

  *types* - specified poi type Multiple types can be separated by |  and returns all matching types (OR operation).  Null entry returns all types.

  *min_distance_traveled* - minimum distance traveled (in meters) within one minute to be considered moving and thus passing a location.  68 meters = 4 km/hr which is typical walking speed

  *min_time_between_passes* - minimum duration (in seconds) between consecutive GPS fixes
                           for the location to be considered passed.    If set too low,
                           a single pass may be counted multiple times.  For example,
                           a location at a street corner may be counted multiple times
                           as the participant rounds the corner.

  *min_duration_at* - minimum amount of time (in seconds) that must be spent near a  
                           location for the location to be considered visited.  If set too low,
                           time near a location may be falsely considered a visit.  
                           For example, a location may be falsely counted as visited
                           while the participant is stopped at a traffic light.

  *detail* - If true, output one entry for each time a location is passed or visited. If false, output one entry for each location

  *debug* - If true, log debugging information to determine why a location was considered passed or visited


**Outputs**

  Location Passed dpu Test Results.csv  -- CVS file contains information about each location,
  the number of times the location was passed, the number of times the location was visited
  and the total amount of time (in seconds) spent at the location during the visits.

  Location Passed dpu Test Results.json -- contains the same information in JSON format

  Location Passed dpu Test Results.kml  -- When viewed in Google Earth, shows the persons tracks
  and identified locations of interest.  The locations are color coded as green (passed), red (visited),
  yellow (both visited and passed).  Selecting the location displays location details and the times-passed, times-visited, times-at values.

  EventLogger_yyyy-mm-dd.log -- text file log of the processing containing informational messages,
  warnings and errors.

### Related Test Programs

  **TestDAOpoi** -- Tests the Data Access Object (DAO) that accesses the PostGIS
  database used by the location DPUs and is described below.

  **TestGooglePlacesAPI** -- Tests the Google Places API

  **TestYelpAPI** --  Tests the Yelp API

  **TestPoiDpu**  --  Tests the POI DPU -- returns a CSV file of POIs along
                    a specified GPX track.

### PostGIS database

The location DPUs use a PostGIS database to store information about locations.
User-provided locations can be imported into the database, typically from a CVS file. Or they
can be discovered by searching Yelp or Google.   The location DPUs search for
a POI within a specified distance (buffer) from a specified lat/lon.  It first
checks the database.  If none are found in the database, it then checks Google
and Yahoo.  Returned POIs are cached in the database for future use.  
According to terms of use agreements, Google's data can be cached for 30 days,
Yelp's data for 1 day.  Imported POIs typically do not expire.   Once a day, a
cron job is executed which deletes expired entries from the database.

**POI Table**

The POI table contains the following columns:
```
  place_id        String    Unique place identifier, typically a GUID
  name            String    name of place
  scope           String    source of entry (Yelp, Google, local, etc)
  vicinity        String    general locale or neighborhood
  postal_address  String    full street address, city, state and zip code
  date_created    Date      date the row was entered into the database
  date_expired    Date      date the row should be deleted from the database
  lon             Double    longitude in degrees.decimal
  lat             Double    latitude in degrees.decimal
  types           String    classification of POI by data provider
  md2k_types      String    classification of POI requested by call to DPU
  buffer          int       distance in meters from the search coordinates
  lonlatgeometry  Double    Value computed by PostGIS used by geographic queries  
```
**POI Importer**

The program BuildChicagoDB.java in the palms.poi.importer package was used to
insert locations in Chicago that sold tobacco products into the POI table.  The data was downloaded as
a CSV file from the Chicago Data Portal located here: https://data.cityofchicago.org/Community-Economic-Development/Business-Licenses/r5kz-chrr
