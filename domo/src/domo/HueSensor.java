package domo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.simple.*;
import org.json.simple.parser.*;

import jsmm.cm15a.Utils;

public class HueSensor {
	private static Thread hilo;
	private static boolean lastPresence=false;
	private static boolean running=false;

	protected static synchronized void start() {
		if (hilo==null) {
			hilo=new Thread(new Runnable() {
				@Override
				public void run() {
					while (running) {
						try {
							boolean newPresence=requestSensor();
							if (newPresence!=lastPresence) {
								Utils.log("NEW PRESENCE IS "+newPresence);
								lastPresence=newPresence;
								if (newPresence) {
									String[] command=new String[]{"curl","-sS","http://localhost:18080/domo/iPHC-process.php?q=ssm+navON"};
									execCommand(command);
								}
								else {
									String[] command=new String[]{"curl","-sS","http://localhost:18080/domo/iPHC-process.php?q=ssm+navOFF"};
									execCommand(command);
								}
							}
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							//e.printStackTrace();
						}
					}
				}
			});
			hilo.setDaemon(true);
			hilo.setName("HueSensorThread");
		}
		if (!running) {
			running=true;
			hilo.start();
			Utils.log("HueSensor.start()");
		}
	}

	protected static synchronized void stop() {
		if (running) {
			running=false;
			hilo=null;
			Utils.log("HueSensor.stop()");
		}
	}
	private static String execCommand(String[] curlCommand) {
		try {
			//Utils.log("HueSensor: exec:"+Arrays.toString(curlCommand));
			Process p= Runtime.getRuntime().exec(curlCommand);
			InputStream es =p.getErrorStream();
			InputStream is =p.getInputStream();
			BufferedReader br = new BufferedReader (new InputStreamReader (is));
			while (true) {
				String linea=br.readLine();
				if (linea==null) {
					break;
				}
				//Utils.log("HueSensor exec res:"+linea);
				return linea;
			}
			
			br = new BufferedReader (new InputStreamReader (es));
			while (true) {
				String linea=br.readLine();
				if (linea==null) {
					break;
				}
				Utils.logErr("HueSensor exec err res:"+linea);
				return null;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	private static boolean requestSensor() {
		String[] command=new String[]{"curl","-sS","http://192.168.1.10/api/hiFbSVwTwWZU1APZfOR8wkPz0uwJEtX1O0kVbUOi/sensors/4"};
		JSONParser jsonParser = new JSONParser();
		String resul=execCommand(command);
		//Utils.log("requestSensor="+resul);
		if (resul!=null) {
			try {
				JSONObject jresul=(JSONObject) jsonParser.parse(resul);
				JSONObject jstate=(JSONObject)jresul.get("state");
				boolean presence=(boolean)jstate.get("presence");
				//Utils.log("requestSensor.presence="+presence);
				return presence;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * jsmm 26/12/2016 main de pruebas
	 * @param args
	 */
	public static void main(String[] args) {
		start();
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stop();
	}
}