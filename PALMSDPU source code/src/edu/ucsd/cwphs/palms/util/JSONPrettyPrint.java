package edu.ucsd.cwphs.palms.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JSONPrettyPrint {

	public static String print(String uglyString){
		JsonElement je = null;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		try {
		je = jp.parse(uglyString);
		}
		catch (Exception ex){
			EventLogger.logException("JSONPrettyPrint.print parse exception - ", ex);
			return uglyString;
		}
		return gson.toJson(je);
	}
}
