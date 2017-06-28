# DPU Source Packages

### Introduction

The source code is organized into 7 packages:

* edu.ucsd.cwphs.palms.gps - contains the GPS DPU
* edu.ucsd.cwphs.palms.location - contains the Location DPU
* edu.ucsd.cwphs.palms.location.passed - contains the Location Passed DPU
* edu.ucsd.cwphs.palms.trip - contains the Trip DPU
* edu.used.cwphs.palms.poi - contains the POI DPU
* edu.ucsd.cwphs.palms.webservice - contains the RESTful web service for each of the above DPUs
* edu.ucsd.cwphs.palms.util - common utility methods

There are 3 additional packages useful for testing:
* edu.ucsd.cwphs.palms.test - contains test programs for the DPUs
* edu.ucsd.cwphs.palms.webservice.test - contains test programs for the DPU web services
* edu.ucsd.cwphs.palms.kml - creates KML files from the results of the GPS DPU.  Useful for visualizing the DPU's output.

### General Organization of DPUs

Each DPU is implemented using a common class structure. Data classes define the data structures:

* *XXXXdpuParameters* - defines parameters used by the DPU  
* *XXXXdpuResult* - defines results (output) of DPU  

Processing classes contain the processing and algorithms:

* *XXXXdpu* - main entry points  
* *XXXXprocessing* - contains the algorithms by the DPU. It is anticipated this class will be replaced as the algorithms improve over time.  
* *XXXXdpuServlet* - defines the web service that invokes the dpu


### Classes Contained in the palms.gps Package

**Data Classes**

* GPSTrack - defines input JSON
* GPSdpuParameters - defines parameters JSON
* GPSResultSet - defines output JSON
* Location - internal class used to keep track of locations
* LocationList - internal class (list of locations)
* TrackPoint - internal class used to store track points
* WayPoint - internal super class of TrackPoint & Location

**Processing Classes**

* GPSdpu - DPU entry points
* GPSprocessingR41 - processing algorithms.

### Classes Contained in the palms.location Package

**Data Classes**

* LocationDpuParameters - defines parameters JSON
* LocationDpuResult
* LocationResultSet - defines output JSON

**Processing Classes**

* LocationDpu - DPU entry points
* LocationProcessingR1 - processing algorithms.

### Classes Contained in the palms.trip Package

**Data Classes**

* TRIPdpuParameters - defines parameters JSON
* TRIPdpuResult
* TRIPdpuResultSet - defines output JSON

**Processing Classes**

* TRIPdpu- DPU entry points
* TRIPprocessingR1 - processing algorithms.

### Classes Contained in the palms.poi Package

**Data Classes**

* POI - defines output JSON
* GooglePlaces - interface to Google Places API
* YelpPlaces - interface to YelpPlaces API
* LocalStore - not currently used

**Processing Classes**

* POIdpu - DPU entry points
* DAOpoi - Data Access Object that handles database storage
* POIpurge - utility program to purge "expired" POIs from the database.
* TwoStepOAuth - utility class used by YelpPlaces for authorization

### Classes Contained in the palms.util Package

* EventLogger
* JSONPrettyPrint
* WriteToFile

### Classes Contained in the palms.webservice Package

* GPSDPUservlet
* LocationDPUservlet
* TripDPUservlet
* POIDPUservlet

### Classes Contained in the palms.test Package

* TestGPSdpu
* TestGPSTrack
* TestLocationDpu
* TestLocationPassedDpu
* TestTripDpu
* TestPoiDpu
* TestDAOpoi
* TestGooglePlacesAPI
* TestYelpAPI

### Classes Contained in the palms.webservice.test Package

* TestGPSDPUservlet
* TestLocationDPUservlet
* TestTripDPUservlet
* TestPOIDPUservlet

### Classes Contained in the palms.kml Package

* JenksBreaks
* KMLexport
* KMLgenerator
