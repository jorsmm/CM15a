package domo;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.annotation.PreDestroy;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jsmm.cm15a.CM15a;
import jsmm.cm15a.CM15aData;
import jsmm.cm15a.CM15aData.Function;
import jsmm.cm15a.Utils;

/**
 * Servlet implementation class DomoServlet
 */
@WebServlet(urlPatterns="/domo", loadOnStartup=1)
public class DomoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    @Override
    public void init(ServletConfig config) {
		Utils.logErr("####DomoServlet.init()");
    	CM15aSingleton cm15aSingleton=CM15aSingleton.getInstance();
    	cm15aSingleton.init();
    }

    
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		myservice(req, resp);
		
	}



	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		myservice(req, resp);
		
	}



	private void myservice(ServletRequest request, ServletResponse response) throws ServletException, IOException {
    	String q=request.getParameter("q");
    	
    	Utils.logErr("domo?q="+q);
    	
    	boolean result=false;
    	if (q!=null) {
        	CM15aSingleton cm15aSingleton=CM15aSingleton.getInstance();
        	CM15a cm15a=cm15aSingleton.getCm15a();
        	if (cm15a!=null && cm15a.isConnected()) {
        		StringTokenizer st = new StringTokenizer(q," ");
        		String hcdev=st.nextToken();
        		String action=st.nextToken();
        		action=action.replaceAll("lights", "l");
        		action=action.replaceAll("units", "u");
        		String percent="0";
        		if (st.hasMoreTokens()) {
					percent=st.nextToken();
					try {
						result=cm15a.parseCommand(hcdev,action,percent);
					}
					catch (Exception e) {
						Utils.logErr("DomoServlet: Excepction: "+e);
					}
				}
				else {
					result=cm15a.parseCommand(hcdev,action);
				}
        		// notificar la accion como si se hubiera recibido por RF
        		cm15aSingleton.receive(new CM15aData(hcdev.charAt(0),Integer.parseInt(hcdev.substring(1)),Function.valueOf(action.toUpperCase()),Integer.parseInt(percent)));
        	}
        	else {
        		Utils.logErr("cm15a is not available, cannot exec request (q): "+q);
        	}
    	}

    	response.getWriter().write(q+"="+result);
   	}

	@Override
	@PreDestroy
    public void destroy() {
		Utils.logErr("####DomoServlet.destroy()");
    	CM15aSingleton cm15aSingleton=CM15aSingleton.getInstance();
    	cm15aSingleton.destroy();
		HueSensor.stop();
		// jsmm 27/12/2016 espera 1,5s pues el sondeo dle hilo es de 1000
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}