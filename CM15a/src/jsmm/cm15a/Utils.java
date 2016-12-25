package jsmm.cm15a;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.usb.util.UsbUtil;

public class Utils {

	private static boolean out;
	private static boolean err;
	private static Properties prop;

	/////////////////////////////////////////////////////////////////////////////////////

	static {
		reload();
		err=Boolean.parseBoolean(prop.getProperty("err", "true"));
		out=Boolean.parseBoolean(prop.getProperty("out", "false"));
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// PROPERTIES
	/////////////////////////////////////////////////////////////////////////////////////

	public static void reload() {
		try {
			prop = new Properties();
			prop.load(Utils.class.getResourceAsStream("/config.properties"));		
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	public 
	static String getString(String key, String def) {
		return(prop.getProperty(key,def));
	}
	public static boolean getBoolean(String key, boolean def) {
		return Boolean.parseBoolean(getString(key, Boolean.toString(def)));
	}
	public static long getLong(String key, long def) {
		return Long.parseLong(getString(key, Long.toString(def)));
	}
	public static short getShort(String key, short def) {
		return Short.parseShort(getString(key, Short.toString(def)),16);
	}

	/////////////////////////////////////////////////////////////////////////////////////

	public static void waitFor(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// LOGGING
	/////////////////////////////////////////////////////////////////////////////////////

	public static String getTime() {
		return new SimpleDateFormat("[dd:MM:yyyy HH:mm:ss.SSS]").format(new Date());
	}

	public static String getLogInfo(String scope) {
		return "["+scope+"] "+getTime()+" ["+Thread.currentThread().getName()+"] ";
	}

	public static void log(String log) {
		log(log,true);
	}

	public static void log(String log, boolean nextLine) {
		if (out) {
			if (nextLine) {
				System.out.println(getLogInfo("O")+log);
			}
			else {
				System.out.print(getLogInfo("O")+log);
			}
		}
	}

	public static void logHexBuffer(String extraInfo, byte[] buffer, int length) {
		if (out) {
			Utils.log(extraInfo+": "+length + " bytes of data:", false);
			for (int i=0; i<length; i++) {
				System.out.print(" 0x" + UsbUtil.toHexString(buffer[i]));
			}
			System.out.println("");
		}
	}

	public static void logErr(String log) {
		logErr(log,true);
	}

	public static void logErr(String log, boolean nextLine) {
		if (err) {
			if (nextLine) {
				System.out.println(getLogInfo("E")+log);
			}
			else {
				System.out.print(getLogInfo("E")+log);
			}
		}
	}
}
