package edu.ucsd.cwphs.palms.kml;

import java.io.PrintWriter;

import edu.ucsd.cwphs.palms.gps.TrackPoint;
import edu.ucsd.cwphs.palms.gps.WayPoint;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class KMLgenerator {

	    private EventLogger eventLogger;
	    private PrintWriter printWriter; 
		private int folderCount, trackCount;
		private String trackBuffer = "";
		private String trackCoords ="";
		private boolean enableTimeSlider = false;
		
		// stats 
		private int linesWritten = 0;
		
		public int getNumLinesWritten(){
			return linesWritten;
		}
		
	    public void setEnableTimeSlider (boolean flag){
	    	enableTimeSlider = flag;
	    }
		
	    public KMLgenerator(EventLogger eventLoggerReference, PrintWriter printWriterReference){
	    	eventLogger = eventLoggerReference;
	    	printWriter = printWriterReference;
	    }
		
		public boolean initKML(String docName){
			folderCount = 0;
			trackCount = 0;
			linesWritten = 0;
			
			// write initial tags
			write("<?xml version=\"1.0\" standalone=\"yes\"?>");
			write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
			write("<Document>");
			// <name> displayed in Google Earth as top level folder for file
			write("  <name><![CDATA["+docName+"]]></name>");
			return true;
		}	
		
		public void closeKML(){
			// write closing tags
			while (trackCount-- > 0){
				write("</Pacemark>");
				eventLogger.logWarning("KMLGenerator.closeKML called with open placemark");
			}
			while (folderCount-- > 0){		// close any open folders
				write("</Folder>");
				eventLogger.logWarning("KMLGenerator.closeKML called with open folder");
			}
			write("</Document>");
			write("</kml>");
			printWriter.flush();			// flush buffer
		}
		
		
		public void setInitialCamera(String lat, String lon){
			setInitialCamera(lat, lon, "10000");  // default to 10,000 km
		}
		
		public void setInitialCamera(String lat, String lon, String altitudeInKm){
			write ("<Camera id=\"inital view\">");
			write ("  <longitude>" + lon + "</longitude>");
			write ("  <latitude>" + lat + "</latitude>");
			write ("  <altitude>" + altitudeInKm + "</altitude>");
			write ("</Camera>");
		}
		
		public void addFolder(String name){
			addFolder(name, true);
		}
		
		public void addFolder(String name, boolean visbility){
			folderCount++;
			write("<Folder>");
			write("  <name><![CDATA["+name+"]]></name>");
			if (visbility)
				write("  <visibility>1</visibility>");
			else
				write("  <visibility>0</visibility>");
		}
		
		public void addRadioFolder(String name, boolean visbility){
			addFolder(name, visbility);
			write("  <Style>");
			write("    <ListStyle>");
			write("       <listItemType>radioFolder</listItemType>");
			write("    </ListStyle>");
			write("  </Style>");
		}
		
		public void closeFolder(){
			if (folderCount > 0){
				folderCount--;
				write("</Folder>");	
			}
			else
				eventLogger.logWarning("KMLGenerator.closeFolder called without an open folder");
		}
		
		public void addTrack(String name, String style, boolean visible){
			trackCount++;
			buffer("<Placemark>");
			buffer("  <name><![CDATA["+name+"]]></name>");
			if (visible)
				write("  <visibility>1</visibility>");
			else
				write("  <visibility>0</visibility>");
			buffer("  <styleUrl>" + style + "</styleUrl>");
			buffer("  <MultiGeometry>");
		}
		
		public void addTrackCoords(double lat, double lon, int ele){
			try {
				String latStr = Double.toString(lat);
				String lonStr = Double.toString(lon);
				String eleStr = Integer.toString(ele);
				addTrackCoords (latStr, lonStr, eleStr);
			}
			catch (Exception ex){
				EventLogger.logException("KMLgenerator.addTrackCoors - invalid parameter: " + lat + "," +
										lon + "," + ele, ex);
			}
		}
		
		public void addTrackCoords(String lat, String lon, String ele){
			if (trackCount > 0)
				trackCoords = trackCoords.concat(lon + "," + lat + "," + ele + " ");	
		}
		
		public void addTrackSegment(String coords, boolean clamped){
			buffer("    <LineString>");
			if (!clamped){
				buffer("       <altitudeMode>relativeToGround</altitudeMode>");
				buffer("       <tessellate>0</tessellate>");
			}
			else
				buffer("       <altitudeMode>clampToGround</altitudeMode>");
				buffer("       <tessellate>1</tessellate>");
			buffer("       <coordinates>"+coords+"</coordinates>");
			buffer("    </LineString>");
		}
		
		public void writeTrackCoords(boolean clamped){
			addTrackSegment(trackCoords, clamped);
		}
		
		public void writeTrack(){
			write(trackBuffer);
			trackBuffer = "";
			trackCoords = "";
		}
		
		public void resetTrack(){
			trackBuffer = "";
			trackCoords = "";
			trackCount--;
		}
		
		public void closeTrack(){
			if (trackCount > 0){
				trackCount--;
				write("  </MultiGeometry>");
				write("</Placemark>");	
			}
			else
				eventLogger.logWarning("KMLGenerator.closeTrack called without an open track");
		}
		
		
		public void addTrackPointx(TrackPoint tp, String style, boolean visible){
			if (tp.getTripType() == TrackPoint.MIDPOINT)
				tp.desc = tp.getDateTimeStr() + " - Dur:" + tp.getDuration() + " Spd:" + tp.getSpeed();
			else
				tp.desc = tp.getDateTimeStr() + " - Dur:" + tp.getDuration();
			
			addPlacemark(tp, style, visible, null);
		}
		
			
		public void addTrackPoint(TrackPoint tp){
			addPlacemark(tp, "#midtp", true, null);
		}
		
		public void addTrackPoint(TrackPoint tp, String style, boolean visible){
			addPlacemark(tp, style, visible, tp.name);  // use name within wp
		}
		
		public void addPlacemark(TrackPoint tp, String style, boolean visible, String name){
			/*
			if (trackCount > 0)
				eventLogger.logError("KMLGenerator.addWayPoint called with open track");
			else 
			*/ {
				write("<Placemark>");
				if (visible)
					write("  <visibility>1</visibility>");
				else
					write("  <visibility>0</visibility>");
				if (name != null)
					write("  <name><![CDATA["+name+"]]></name>");
				write("  <description><![CDATA["+tp.desc+"]]></description>");
				write("  <styleUrl>" + style + "</styleUrl>");
				write("  <Point>");
				
				// TEST
				// write("     <altitudeMode>relativeToGround</altitudeMode>");
				// clampedToGround is the default
				
				write("     <coordinates>"+tp.getLonLatAltStr()+"</coordinates>");
				write("  </Point>");
				
				if (enableTimeSlider) {
					write("  <TimeStamp>");
					write("     <when>"+tp.getDateTimeStr() + "</when>");
					write("  </TimeStamp>");
				}

				write("</Placemark>");
			}
		}	
		
		public void addPlacemark(WayPoint wp, String style, boolean visible, String name){
				write("<Placemark>");
				if (visible)
					write("  <visibility>1</visibility>");
				else
					write("  <visibility>0</visibility>");
				if (name != null)
					write("  <name><![CDATA["+name+"]]></name>");
				write("  <description><![CDATA["+wp.desc+"]]></description>");
				write("  <styleUrl>" + style + "</styleUrl>");
				write("  <Point>");
				write("     <coordinates>"+wp.getLonLatAltStr()+"</coordinates>");
				write("  </Point>");
				write("</Placemark>");
		}
		
		public void addLegend(String name, String style){
			if (trackCount > 0)
				eventLogger.logError("KMLGenerator.addLegend called with open track");
			else {
				write("<Placemark>");
				write("  <visibility>0</visibility>");
				write("  <name><![CDATA["+name+"]]></name>");
				write("  <styleUrl>" + style + "</styleUrl>");
				write("  <Point>");
				write("     <coordinates>"+"-117.234, 32.774,0"+"</coordinates>");   // mission bay area
				write("  </Point>");					
				write("</Placemark>");
			}
		}
		
		public void addCircle(WayPoint wp, int radiusInMeters, String lineStyle, boolean visible){
			double lat1, long1;
			double d_rad;
			double d;
			double radial, lat_rad, dlon_rad, lon_rad;

			// convert coordinates to radians
			lat1 = Math.toRadians(wp.getLat());
			long1 = Math.toRadians(wp.getLon());

			//Earth measures 
			//Year Name a (meters) b (meters) 1/f Where Used 
			//1980 International 6,378,137 6,356,752 298.257 Worldwide 
			d = radiusInMeters;
			d_rad = d/6378137;

			// output header
			write("<Placemark>");
			if (visible)
				write("  <visibility>1</visibility>");
			else
				write("  <visibility>0</visibility>");
			write("  <styleUrl>" + lineStyle + "</styleUrl>");
			write("<LineString>\n<coordinates>");

			int num_points = 360;		// FULL circle ?
			
			// loop through the array and write path linestrings
			for(int i=0; i<=num_points; i++) {
				//delta_pts = 360/(double)num_points;
				//radial = Math.toRadians((double)i*delta_pts);
				radial = Math.toRadians((double)i);

				//This algorithm is limited to distances such that dlon <pi/2
				lat_rad = Math.asin(Math.sin(lat1)* Math.cos(d_rad) + Math.cos(lat1)* Math.sin(d_rad)* Math.cos(radial));
				dlon_rad = Math.atan2(Math.sin(radial)* Math.sin(d_rad)* Math.cos(lat1), Math.cos(d_rad)- Math.sin(lat1)* Math.sin(lat_rad));
				lon_rad = ((long1 + dlon_rad + Math.PI) % (2*Math.PI)) - Math.PI;

				//write results
				write( Math.toDegrees(lon_rad) + ", " + Math.toDegrees(lat_rad) + ", 0");
			}
			// output footer
			write("</coordinates>\n</LineString>\n</Placemark>");
		}
		
		private void writeRegion(WayPoint wp, boolean close){
				double delta = .0001;			// .0001 works well with LOD of 128 pixels
				double north = wp.getLat()+delta;
				double south = wp.getLat()-delta;
				double east = wp.getLon()+delta;
				double west = wp.getLon()-delta;
				write("  <Region>");
				write("     <LatLonAltBox>");
				write("       <north>"+north+"</north>");
				write("       <south>"+south+"</south>");
				write("       <east>"+east+"</east>");
				write("       <west>"+west+"</west>");
				write("     </LatLonAltBox>");
				write("     <Lod>");
				if (close == true)
					write("        <minLodPixels>128</minLodPixels>");		// was 128
				else														// larger number requires user
					write("        <maxLodPixels>128</maxLodPixels>");		// to be zoomed in closer
				write("     </Lod>");										// to see data
				write("   </Region>");
		}
		
		private void buffer(String s){
			trackBuffer = trackBuffer + s + "\r\n";
		}
		
		private void write(String s){
			printWriter.write(s + "\r\n");
			linesWritten++;
		}
				
		public void writeStylePoint(String id, String color_AABBGGRR, double scale){
            write("<Style id = \""+id+"\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>" + color_AABBGGRR + "</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>"+scale+"</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>" + color_AABBGGRR + "</color>");
            write("  </LabelStyle>");
            write("  <BalloonStyle>");
            write("     <text>$[description]</text>");
            write("  </BalloonStyle>");
            write("</Style>");	
		}
		
		public void writeStyleLine(String id, String color_AABBGGRR, int width){
        write("<Style id = \""+id+"\">");
        write("  <LineStyle>");
        write("     <color>" + color_AABBGGRR + "</color>");
        write("     <width>"+ width +"</width>");
        write("  </LineStyle>");
        write("</Style>");
		}
		
		public void writeStyleLine(String id, String color_AABBGGRR, int width, boolean randomColorMode){
	        write("<Style id = \""+id+"\">");
	        write("  <LineStyle>");
	        write("     <color>" + color_AABBGGRR + "</color>");
	        if (randomColorMode)
	        	 write("     <colorMode>random</colorMode>");
	        write("     <width>"+ width +"</width>");
	        write("  </LineStyle>");
	        write("</Style>");
			}
		
		
		/*
		 *  No longer used
		 *  
		private void writeDefaultStyles(){
			
            write("<Style id = \"unknowntp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>AAAAAAAA</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.50</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"rawtp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>AAFFFFFF</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.35</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"stationarytp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>FF00CCCC</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.40</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"starttp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>FF00CC00</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.50</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"midtp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>FFCC8800</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.35</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"pausetp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>FFCCCC00</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.35</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"endtp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>FF0000CC</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.50</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"avgtp\">");
            write("  <IconStyle>");
            write("     <Icon>");
            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
            write("     </Icon>");
            write("     <color>CC0077FF</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.50</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"locationwp\">");
            write("  <IconStyle>");
//            write("     <Icon>");
//            write("        <href>http://maps.google.com/mapfiles/kml/pal2/icon26.png</href>");
//            write("     </Icon>");
            write("     <color>CC00FFFF</color>");
            write("     <colorMode>normal</colorMode>");
            write("     <scale>0.50</scale>");
            write("  </IconStyle>");
            write("  <LabelStyle>");
            write("     <color>FFCC8800</color>");
            write("  </LabelStyle>");
            write("</Style>");
            
            write("<Style id = \"trackStyleWalking\">");
            write("  <LineStyle>");
            write("     <color>FFCC8800</color>");
            write("     <width>4</width>");
            write("  </LineStyle>");
            write("</Style>");
            
            write("<Style id = \"trackStyleAuto\">");
            write("  <LineStyle>");
            write("     <color>FFCCCC00</color>");
            write("     <width>4</width>");
            write("  </LineStyle>");
            write("</Style>");
            
            write("<Style id = \"trackStyleRunning\">");
            write("  <LineStyle>");
            write("     <color>FFCC8800</color>");
            write("     <width>4</width>");
            write("  </LineStyle>");
            write("</Style>");
		}
		*/
		
		
}
