# MD2K PALMS Data Processing Units

**Overview**

These Data Processing Units (DPUs), coded in Java, can be deployed as RESTful web services or incorporated into (and directly invoked) by the MD2K infrastructure.   The DPUs accept JSON strings as inputs and output the results as JSON.

Initially there will be 4 DPUs what can be used individually or cascaded together:

*	GPS Processing – Accepts GPS track-points (timestamp, latitude, longitude, elevation), cleans and filters, adjusts epoch, and classifies by trip, location, mode of transportation, duration, distance traveled, etc.,  for a user-specified interval (typically 1 minute).

*	Location Summary – Accepts output of GPS Processing DPU and returns location information (times when user was stationary)
*	Trip Summary – Accepts output of GPS Processing DPU and returns trip information (times when user was traveling between locations).
*	POI (Point of Interest) DPU – Accepts output of other DPUs and provides POI information about specific locations or a list of POIs along a trip’s route.

For each DPU, 3 JSON schemas are defined:  
*	Input – input data stream
*	Output  - output data stream
*	Parameters – user defined variables and cutoffs used by the processing algorithms.

The GPS, Location, and Trip DPUs are based on existing PALMS services.  The POI DPU will use publically available POI web services provided by Google, FourSquare and others, as well as a user provided database of locally collected POIs (i.e.: advertisements, smoking locations).   
