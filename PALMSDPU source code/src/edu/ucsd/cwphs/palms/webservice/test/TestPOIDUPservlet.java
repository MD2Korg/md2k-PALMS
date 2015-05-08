package edu.ucsd.cwphs.palms.webservice.test;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import edu.ucsd.cwphs.palms.util.EventLogger;

public class TestPOIDUPservlet {
	
	private static final boolean LOCALTEST = true;
	private static final boolean BETATEST = false;

	private static String DPU_URL = null;
	
	public static void main(String[] args) {

		EventLogger.setFileName("logs\\TestPOIDPUservlet ");
		EventLogger.logEvent("TestPOIDPUservlet -- program start - LOCALTEST = " + LOCALTEST);
		if (LOCALTEST){
			DPU_URL = "http://localhost:8080/PALMSDPU/POIDPU";
		}
		else if (BETATEST){
			DPU_URL = "http://localhost:8080/PALMSDPU/POIDPU";
		}
		else {
			DPU_URL = "http://localhost:8080/PALMSDPU/POIDPU";
			}
		
		EventLogger.logEvent("TestPOIDPUservlet -- DPU_URL = " + DPU_URL);
		performTests();
		EventLogger.logEvent("TestPOIDPUservlet -- program exit");
	}
	
	static private void performTests(){
		String json, result;

		json = 	buildJSON(32.8692502, -117.2425893, 1000, "food");
		result = post2DPU(DPU_URL, json);
		EventLogger.logEvent("TestPOIDPUservlet - result = " + result);
		
		json = buildJSON(32.840373, -117.274138, 300, "food");
		result = post2DPU(DPU_URL, json);
		EventLogger.logEvent("TestPOIDPUservlet - result = " + result);
 	}
	

	private static String buildJSON(double lat, double lon, int buffer, String types){
		if (types == null)
			types = "";
		
		StringBuilder sb = new StringBuilder("{");
		sb.append("\"types\":\"" + types + "\",");
		sb.append("\"buffer\":\"" + buffer + "\",");
		sb.append("\"lat\":\"" + lat + "\",");
		sb.append("\"lon\":\"" + lon + "\"");
		sb.append("}");
		return sb.toString();
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

