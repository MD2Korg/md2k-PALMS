package edu.ucsd.cwphs.palms.webservice;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsd.cwphs.palms.gps.GPSdpu;


/**
 * Servlet implementation class CycoreNotification
 */

public class GPSDPUservlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GPSDPUservlet() {
        super();
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String result = "";
		String json = "";
		
		// get the body
		String str = null;
		DataInputStream dataStreamIn = new DataInputStream(request.getInputStream());
		while (null != ((str = dataStreamIn.readLine()))){
			json = json + str;
		}
		dataStreamIn.close();
		
		if (json.length() == 0)
			result = "error:POST does not contain data.";
		else 
			result = process(json);
		
		response.setContentType("text/json");
		PrintWriter out = response.getWriter();
		out.println(result);
		out.close();
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String result = "Error: The GPSDPU Service only supports POST.";
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println(result);
		out.close();
	}
	
	
	private String process(String json){
		GPSdpu dpu = new GPSdpu();
		String result = dpu.setParametersAndTrackFromJSON(json);
		if (result != null)
			return result;				// contains error
		result = dpu.process();
		if (result != null)
			return result;				// contains error
		return dpu.getResultSet().toJSON();
	}

}
