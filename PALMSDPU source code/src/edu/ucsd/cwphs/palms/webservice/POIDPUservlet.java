package edu.ucsd.cwphs.palms.webservice;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsd.cwphs.palms.poi.POIdpu;

/**
 * Servlet implementation class CycoreNotification
 */

public class POIDPUservlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public POIDPUservlet() {
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
			result = "POST does not contain data.";
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
		String result = "Error: The POIDPU Service only supports POST. ";
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println(result);
		out.close();
	}
	
	
	private String process(String json){
		POIdpu dpu = new POIdpu();
		String result = dpu.setParametersFromJSON(json);
		if (result != null)
			return result;				// contains error
		dpu.process();
		result = dpu.getResultSetJSON();
		if (result != null)
			return result;				// contains error
		return dpu.getResultSetJSON();
	}

}
