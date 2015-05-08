package edu.ucsd.cwphs.palms.webservice.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.ucsd.cwphs.palms.gps.GPSTrack;
import edu.ucsd.cwphs.palms.gps.GPSdpuParameters;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class TestGPSDPUservlet {
	
	private static final boolean LOCALTEST = true;
	private static final boolean BETATEST = false;

	private static String DPU_URL = null;
	
	public static void main(String[] args) {

		EventLogger.setFileName("logs\\TestGPSDPUservlet ");
		EventLogger.logEvent("TestGPSDPUservlet -- program start - LOCALTEST = " + LOCALTEST);
		if (LOCALTEST){
			DPU_URL = "http://localhost:8080/PALMSDPU/GPSDPU";
		}
		else if (BETATEST){
			DPU_URL = "http://localhost:8080/PALMSDPU/GPSDPU";
		}
		else {
			DPU_URL = "http://localhost:8080/PALMSDPU/GPSDPU";
			}
		
		EventLogger.logEvent("TestGPSDPUservlet -- DPU_URL = " + DPU_URL);
		performTests();
		EventLogger.logEvent("TestGPSDPUservlet -- program exit");
	}
	
	static private void performTests(){
		String json, result;
		GPSTrack track = new GPSTrack();
		track.fromGPX("testData/Test1.gpx");
		EventLogger.logEvent("TestGPSDPUservlet - Number of trackpoints:" + track.getSize());
		
		GPSdpuParameters parameters = new GPSdpuParameters();
		
		String pj = parameters.toJSON();
		String tj = track.toJSON();;
		
		pj = pj.substring(0, pj.length()-1);		// remove ending }
		tj = tj.substring(1);						// remove leading {
		json = pj + "," + tj;
		
		result = post2DPU(DPU_URL, json);
	
		EventLogger.logEvent("TestGPSDPUservlet - result = " + result);
 	}
	

	
	public static String post2DPU(String postURL, String parameters) {
		String response = "";
		URL url = null;
		HttpURLConnection urlConn = null;
		DataInputStream dataStreamIn = null;
		DataOutputStream dataStreamOut = null;

		try {
			url = new URL(postURL);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("Content-Type", "application/json");
			urlConn.setRequestProperty("Accept", "application/json");	
			urlConn.setRequestProperty("Content-Length", Integer.toString(parameters.getBytes().length));
			urlConn.setRequestProperty("charset", "utf-8");
			urlConn.setRequestMethod("POST");
			urlConn.connect();

			dataStreamOut = new DataOutputStream(urlConn.getOutputStream());
			dataStreamOut.writeBytes(parameters);
			dataStreamOut.flush();
			dataStreamOut.close();

			EventLogger.logEvent("POST URL:" + url.toString());
			EventLogger.logEvent("POST parameters:" + parameters);

			// get the response
			String str = null;
			dataStreamIn = new DataInputStream(urlConn.getInputStream());
			while (null != ((str = dataStreamIn.readLine()))){
				response = response + str;
			}
			dataStreamIn.close();	

		} // end try
		catch (Exception ex){
			EventLogger.logException("POST Failure to " + postURL + " - ", ex);
		}
		EventLogger.logEvent("POST Response:" + response);
		return response;
	}
	
	
	/*
	 * Bypasses checking that SSL Common Name matches domain name in URL
	 * 
	 * see: http://www.mkyong.com/webservices/jax-ws/java-security-cert-certificateexception-no-name-matching-localhost-found/
	 */
	
	static {
	    //for localhost testing only
	    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
	    new javax.net.ssl.HostnameVerifier(){
 
	        public boolean verify(String hostname,
	                javax.net.ssl.SSLSession sslSession) {
	            if (hostname.equals("localhost")) {
	                return true;
	            }
	            return true;		// return true to accept everything -- normally return false
	        }
	    });
	}
	
}

