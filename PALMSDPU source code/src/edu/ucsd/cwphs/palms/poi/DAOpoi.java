package edu.ucsd.cwphs.palms.poi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;

import edu.ucsd.cwphs.palms.util.EventLogger;

public class DAOpoi {

	private final String databaseURL = "jdbc:postgresql://137.110.115.96/md2kPOI";
	private final String databaseUser = "md2k";
	private final String databasePassword = "md2k";
	
	private Connection con = null;
	private ResultSet rs = null;
	private PreparedStatement ps = null;
	
	private static ArrayList<POI> localCache = new ArrayList<POI>();
	private static int cacheHits = 0;
	private static int inserts = 0;
	
	public DAOpoi(){
		connect();
	}
	
	public ArrayList<POI> getNearBy(Double lat, Double lon, int radius, String types){
		ArrayList<POI> results = new ArrayList<POI>();
		
		for (int i=0; i<localCache.size(); i++){
			POI poi= localCache.get(i);
				if (poi.isType(types))
					if (poi.isNearBy(lat, lon, radius)){
					results.add(poi);
					cacheHits++;
					}
		}
		// query DB
		results = findPOI(lat, lon, radius, types);
		
		
		return results;
	}
	
	public boolean insertPOIs(ArrayList<POI> poiList){
		for (int i=0; i<poiList.size(); i++){
			POI poi = poiList.get(i);
			localCache.add(poi);
			insertPOI(poi);
			inserts++;
		}
		return true;
	}
	
	public int dbSize(){
		int count = 0;
		count = localCache.size();
		return count;
	}
	
	public boolean closeDB(){
			closeAll();
			try {
			if (con != null){
				con.close();
				con = null;
			}
			} 
			catch (Exception ex){};
		return true;
	}
	
	public int getCacheHitCount(){
		return cacheHits;
	}
	
	public int getInsertsCount(){
		return inserts;
	}
	
	public boolean connect(){
		try {
			con = DriverManager.getConnection(databaseURL, databaseUser, databasePassword);
		}
		catch (Exception ex){
			System.out.println("DAOpoi Exception:" + ex.getMessage());
			con = null;
			return false;
		}
		return true;
	}
	
	public boolean insertPOI(POI poi){
		boolean rc = true;
		try {
			ps = con.prepareStatement("INSERT INTO poi (place_id, name, scope, vicinity, postal_address, date_created, date_expires, lon, lat, types, lonlatgeometry)" +
									" VALUES (?,?,?,?,?,?,?,?,?,?, ST_GeometryFromText('POINT(" + poi.getLon() + " " + 
									 poi.getLat() + ")', 4326));");
			ps.setString(1, poi.getPlaceId());
			ps.setString(2, poi.getName());
			ps.setString(3, poi.getScope());
			ps.setString(4, poi.getVicinity());
			ps.setString(5, poi.getPostalAddress());
			ps.setDate(6, toSqlDate(poi.getDateCached())); 
			ps.setDate(7, toSqlDate(poi.getDateExpires()));
			ps.setDouble(8, poi.getLon());			// lon first
			ps.setDouble(9, poi.getLat());
			ps.setString(10, poi.getTypes());
			ps.execute();
			EventLogger.logEvent("DAOpoi.insertPOI - inserted:" + poi.toCSV());
		}
		catch (Exception ex){
			if (ex.getMessage().contains("duplicate key value"))
				rc = true;		// location already exists - return success
			else {
				EventLogger.logException("DAOpoi.insertPOI Exception:", ex);
				rc = false;
			}
		}
		closeAll();
		return rc;
	}
	
	public ArrayList<POI> findPOI(double lat, double lon, int radius, String types){
		ArrayList<POI> results = new ArrayList<POI>();
		Double buffer = radius /1.0;					// turns int to double
		
		try {
//			ps = con.prepareStatement("Select * FROM poi WHERE ST_DWithin(lonlatgeometry, 'POINT(" + lon + " " + lat + ")',?);");
//			ps = con.prepareStatement("Select * FROM poi WHERE ST_DWithin(lonlatgeometry, ST_GeomFromText('POINT(" + lon + " " + lat + ")', 4326),?);");
			String sql = "Select * FROM poi WHERE ST_Distance_Sphere(lonlatgeometry, ST_GeomFromText('POINT(" + lon + " " + lat + ")', 4326)) <= ?";
			sql = sql + parseTypes(types);
			ps = con.prepareStatement(sql);
			ps.setDouble(1, buffer);
			rs = ps.executeQuery();
			while (rs.next()){
				String placeId = rs.getString("place_id");
				String name = rs.getString("name");
				String scope = rs.getString("scope");
				types = rs.getString("types");
				String vicinity = rs.getString("vicinity");
				String postalAddress = rs.getString("postal_address");
				lon = rs.getDouble("lon");
				lat = rs.getDouble("lat");
				POI poi = new POI(placeId, name, scope, types, vicinity, postalAddress, lat, lon);
				results.add(poi);	
			}
		}
		catch (Exception ex){
			System.out.println("DAOpoi.findPOI Exception:" + ex.getMessage());
		}
		closeAll();
		return results;
	}
	
	public ArrayList<POI> getAllPOIs(String types){
		ArrayList<POI> results = new ArrayList<POI>();
		
		try {
			String sql = "Select * FROM poi WHERE name NOT LIKE '"+ POI.NOPOINEARBY + "'";
			sql = sql + parseTypes(types);
			ps = con.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()){
				String placeId = rs.getString("place_id");
				String name = rs.getString("name");
				String scope = rs.getString("scope");
				types = rs.getString("types");
				String vicinity = rs.getString("vicinity");
				String postalAddress = rs.getString("postal_address");
				Double lon = rs.getDouble("lon");
				Double lat = rs.getDouble("lat");
				POI poi = new POI(placeId, name, scope, types, vicinity, postalAddress, lat, lon);
				results.add(poi);	
			}
		}
		catch (Exception ex){
			System.out.println("DAOpoi.getAllPOIs Exception:" + ex.getMessage());
		}
		closeAll();
		return results;
	}
	
	
	public String getGoogleType(String yahooType){
		String result = null;
		
		try {
			String sql = "Select googletype FROM typemapping WHERE type LIKE '?'";
			ps = con.prepareStatement(sql);
			ps.setString(1, yahooType);
			rs = ps.executeQuery();
			while (rs.next()){
				result = rs.getString("googletype");
			}
		}
		catch (Exception ex){
			System.out.println("DAOpoi.getGoogleType Exception:" + ex.getMessage());
		}
		closeAll();
		return result;
	}
	
	public boolean deleteExpired(Date date){
		boolean rc = true;
		try {
			ps = con.prepareStatement("DELETE FROM poi WHERE date_expires < ?;");
			ps.setDate(1, toSqlDate(date)); 
			ps.execute();
		}
		catch (Exception ex){
			System.out.println("DAOpoi.deleteExpired Exception:" + ex.getMessage());
			rc = false;
		}
		closeAll();
		return rc;
	}
	
	public int countExpired(Date date){
		int count = 0;
		try {
			ps = con.prepareStatement("Select Count(*) FROM poi WHERE date_expires < ?;");
			ps.setDate(1, toSqlDate(date)); 
			rs = ps.executeQuery();
			if (rs.next())
				count = rs.getInt(1);
		}
		catch (Exception ex){
			System.out.println("DAOpoi.countExpired Exception:" + ex.getMessage());
			count = -1;
		}
		closeAll();
		return count;
	}
	
	public boolean deleteScope(String scope){
		boolean rc = true;
		try {
			ps = con.prepareStatement("DELETE FROM poi WHERE scope = ?;");
			ps.setString(1, scope); 
			ps.execute();
		}
		catch (Exception ex){
			System.out.println("DAOpoi.deleteScope Exception:" + ex.getMessage());
			rc = false;
		}
		closeAll();
		return rc;
	}
	
	public int countScope(String scope){
		int count = 0;
		try {
			if (scope == null) 
				ps = con.prepareStatement("Select Count(*) FROM poi;");
				else {
					ps = con.prepareStatement("Select Count(*) FROM poi WHERE scope = ?;");
					ps.setString(1, scope); 
				}
			rs = ps.executeQuery();
			if (rs.next())
				count = rs.getInt(1);
		}
		catch (Exception ex){
			System.out.println("DAOpoi.countScope Exception:" + ex.getMessage());
			count = -1;
		}
		closeAll();
		return count;
	}
	
	private String parseTypes(String types){
		if (types == null)
			return "";
		if (types.length()==0)
			return "";
		
		if (types.contains("&"))
			return parseTypesAND(types);
		else 
			return parseTypesOR(types);
	}
	
	private String parseTypesAND(String types){
		if (types == null)
			return "";
		if (types.length()==0)
			return "";
		
		types = types.toLowerCase();
		String s = " AND (types LIKE '%";
		int i = types.indexOf('&');;
		while (true){
			if (i == -1){
				types = types.trim();
				s = s + types + "%')";
				return s;
			}
			else {
				String keyword = types.substring(0, i).trim();
				types = types.substring(i+1);
				s = s + keyword + "%' AND types LIKE '%";
				i = types.indexOf('|');
			}
		} // end while
	}
	
	private String parseTypesOR(String types){
		if (types == null)
			return "";
		if (types.length()==0)
			return "";
		
		types = types.toLowerCase();
		String s = " AND (types LIKE '%";
		int i = types.indexOf('|');;
		while (true){
			if (i == -1){
				types = types.trim();
				s = s + types + "%')";
				return s;
			}
			else {
				String keyword = types.substring(0, i).trim();
				types = types.substring(i+1);
				s = s + keyword + "%' OR types LIKE '%";
				i = types.indexOf('|');
			}
		} // end while
	}
	
	private java.sql.Date toSqlDate(Date date){
		if (date == null)
			return null;
		return new java.sql.Date(date.getTime());
	}
	
	private Date fromSqlDate(java.sql.Date date){
		if (date == null)
			return null;
		return new Date(date.getTime());	
	}
	
	private void closeAll(){
		try {
			if (rs != null){
				rs.close();
				rs = null;
			}
			if (ps != null){
				ps.close();
				ps = null;
			}
		}
		catch (Exception ex){
			System.out.println("DAOpoi.closeAll Exception:" + ex.getMessage());
		}
	}
}
