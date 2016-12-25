package domo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jsmm.cm15a.CM15a;
import jsmm.cm15a.CM15aData;
import jsmm.cm15a.CM15aData.Function;
import jsmm.cm15a.CM15aDataListener;
import jsmm.cm15a.Utils;

public class CM15aSingleton implements CM15aDataListener {

	private static final char HC_NAVIDAD = 'A';
	private static final int DEVICE_NAVIDAD = 3;

	////////////////////////////////////////////////////////////
	private static CM15aSingleton instance;
	
	////////////////////////////////////////////////////////////
	private CM15a cm15a;
	private Set<CM15aWebSocket> cm15aWebSockets=new HashSet<>();

	private long lastReceived=System.currentTimeMillis();
	
	////////////////////////////////////////////////////////////
	private CM15aSingleton() {	
	}
	public static CM15aSingleton getInstance() {
		if (instance==null) {
			instance=new CM15aSingleton();
		}
		return instance;
	}

	public CM15a getCm15a() {
		return this.cm15a;
	}

	////////////////////////////////////////////////////////////
	@Override
	public synchronized void receive(CM15aData cm15aData) {
						
		if (cm15aData!=null && cm15aData.getFunction()!=null) {
			Utils.log("Singleton: received from cm15a:"+cm15aData);
			long justReceived= System.currentTimeMillis();

			if (justReceived-lastReceived<1200) {
				Utils.log("Singleton: ignoring received");
				return;
			}
			else {
				lastReceived=justReceived;
			}
			String[] command=new String[]{"curl","-sS",("http://localhost:18080/domo/iPHC-process.php?q="+cm15aData.getHc()+cm15aData.getDevice()+"+"+Function.getOpposite(cm15aData.getFunction())+"&noexec=true").toLowerCase()};
			// si es navidad (creo que es D1), hacer una napa, invocando directametne al script de navidad con on u off
			if (cm15aData.getHc()==HC_NAVIDAD && cm15aData.getDevice()==DEVICE_NAVIDAD) {
				Utils.log("Singleton: EXEC NAVIDAD");
				command=new String[]{"/Applications/XAMPP/htdocs/domo/navidad.sh",cm15aData.getFunction().toString().toLowerCase()};
			}
			execCommand(command);
		}
		
		Utils.log("WebSockets.size="+cm15aWebSockets.size());
		
		List<CM15aWebSocket> removing = new ArrayList<CM15aWebSocket>();
		
		for (CM15aWebSocket cm15aWebSocket: cm15aWebSockets){
			Utils.log("cm15aWebSocket.miSession="+cm15aWebSocket.miSession+". isOpen="+cm15aWebSocket.miSession.isOpen());
			if (cm15aWebSocket.miSession!= null && cm15aWebSocket.miSession.isOpen()) {
				cm15aWebSocket.receive(cm15aData);
			}
			else {
				removing.add(cm15aWebSocket);
			}
		}
		for (CM15aWebSocket cm15aWebSocket:removing) {
			cm15aWebSockets.remove(cm15aWebSocket);
		}
	}
	protected void execCommand(String[] curlCommand) {
		try {
			Utils.log("Singleton: exec:"+Arrays.toString(curlCommand));
			Process p= Runtime.getRuntime().exec(curlCommand);
/*
			InputStream es =p.getErrorStream();
			InputStream is =p.getInputStream();
			BufferedReader br = new BufferedReader (new InputStreamReader (is));
			while (true) {
				String linea=br.readLine();
				if (linea==null) {
					break;
				}
				Utils.log("Singleton exec res:"+linea);
			}
			br = new BufferedReader (new InputStreamReader (es));
			while (true) {
				String linea=br.readLine();
				if (linea==null) {
					break;
				}
				Utils.logErr("Singleton exec err res:"+linea);
			}
*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	////////////////////////////////////////////////////////////
	public void init() {
		cm15a=new CM15a();
    	cm15a.setCM15aDataListener(this);
    	cm15a.init();
	}

	public void destroy() {
		Utils.logErr("CM15aSingleton.destroy");
		if (cm15a!=null) {
			cm15a.destroy(true);
		}
		cm15a=null;
	}
	public void addWS(CM15aWebSocket cm15aWebSocket) {
		this.cm15aWebSockets.add(cm15aWebSocket);
		
	}
	public void removeWS(CM15aWebSocket cm15aWebSocket) {
		this.cm15aWebSockets.remove(cm15aWebSocket);
	}
}