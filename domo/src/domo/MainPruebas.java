package domo;

import java.io.FileOutputStream;
import java.util.Properties;

public class MainPruebas {

	public static void main (String args[]) {
		try {
			
			//Utils.class.getResourceAsStream("/config.properties");
			
			FileOutputStream fos = new FileOutputStream("prueba.properties");
			Properties prop = new Properties();
			prop.put("com.app.port", "8090");
			prop.put("com.app.ip", "128.0.0.1");

			long ini=System.currentTimeMillis();
			prop.store(fos,null);
			fos.flush();
			long fin=System.currentTimeMillis();
			
			System.out.println ("==>"+(fin-ini));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
