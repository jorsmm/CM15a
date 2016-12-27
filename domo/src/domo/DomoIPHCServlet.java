package domo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jsmm.cm15a.Utils;

/**
 * Servlet implementation class DomoServlet
 */
@WebServlet(urlPatterns="/domoIPHC", loadOnStartup=1)
public class DomoIPHCServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

 
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
    	
    	Utils.logErr("domoIPHC?q="+q);
    	
    	String result="";
    	if (q!=null) {
    		q=q.toLowerCase();
			String[] command=new String[]{"curl","-sS","http://localhost:18080/domo/iPHC-status.php?q="+q+"&b=true"};
			String actualStatus=execCommand(command);

			if (actualStatus!=null) {
				try {
					String[] command2=new String[]{"curl","-sS","http://localhost:18080/domo/iPHC-process.php?q="+q+"+"+actualStatus};
					result=execCommand(command2);
				}
				catch (Exception e) {
					Utils.logErr("domoIPHC?q="+q+". Exception:"+e);
					e.printStackTrace();
				}
			}
			

    	}
    	response.getWriter().write(q+"="+result);
   	}
	
	protected String execCommand(String[] curlCommand) {
		StringBuilder sb = new StringBuilder();
		try {
			Utils.log("domoIPHC: exec:"+Arrays.toString(curlCommand));
			Process p= Runtime.getRuntime().exec(curlCommand);

			InputStream is =p.getInputStream();
			BufferedReader br = new BufferedReader (new InputStreamReader (is));
			while (true) {
				String linea=br.readLine();
				if (linea==null) {
					break;
				}
				sb.append(linea);
				Utils.log("domoIPHC exec res:"+linea);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

}