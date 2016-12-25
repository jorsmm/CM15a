package jsmm.cm15a;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.usb.*;
import javax.usb.util.UsbUtil;

import jsmm.cm15a.CM15aData.Function;

public class CM15a implements ReadDataListener {


	// USB product & vendor id
	private static final short CM15A_VENDORID=Utils.getShort("vendorid", (short)0x0bc7);
	private static final short CM15A_PRODUCTID=Utils.getShort("productid", (short)0x0001);

	// control read endpoint
	private static final byte AHP_EP1_READ=(byte) 0x81;
	// bulk write endpoint
	private static final byte AHP_EP2_WRITE=(byte) 0x02;


	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	private boolean connected=false;

	private UsbDevice usbDevice;
	private UsbConfiguration usbConfiguration;
	private UsbInterface usbInterface;
	private UsbPipe usbPipeRead;
	private UsbPipe usbPipeWrite;
	private ReaderRunnable readerRunnable;
	private StatusRunnable statusRunnable;
	private CM15aDataListener cm15aDataListener;

	private boolean _firstInit=true;

	BlockingQueue<CM15aData> queue = new LinkedBlockingQueue<CM15aData>();
	
	private char lastHC='A';
	private int lastDevice=1;

	// sync send command and 0x55 confirmation reception
	final ReentrantLock lock = new ReentrantLock();
	final Condition condition = lock.newCondition();

	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	public boolean isConnected() {
		return this.connected;
	}

	public void setCM15aDataListener(CM15aDataListener cm15aDataListener) {
		this.cm15aDataListener=cm15aDataListener;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public boolean init () {

		Thread.currentThread().setName("cm15a");
		
		if (this.statusRunnable==null) {
			this.statusRunnable=new StatusRunnable(this);
			Thread tstatus = new Thread(this.statusRunnable);
			tstatus.setDaemon(true);
			tstatus.start();
		}
		this.statusRunnable.pause();

		Utils.log("Getting Devices...");
		// 1) Find all connected (non-hub) USB devices:
		List<UsbDevice> allDevices=null;
		try {
			allDevices = USBUtils.getDevices();
		} catch (SecurityException | UnsupportedEncodingException | UsbException e) {
			e.printStackTrace();
			this.statusRunnable.resume();
			return false;
		} 

		Utils.log("Finding device...");
		// 2) Iterate the list to find the UsbDevice you're looking for ( match the manufacturer/product id )
		this.usbDevice = USBUtils.findDevice(allDevices, CM15A_VENDORID, CM15A_PRODUCTID);

		if (this.usbDevice==null) {
			Utils.logErr("X10 USB Device not found");
			this.statusRunnable.resume();
			return false;
		}
		else {
			Utils.logErr("X10 USB Device found!");
			if (this._firstInit) {
				this._firstInit=false;
			}
			else {
				Utils.waitFor(Utils.getLong("onReload.wait",500));
			}
		}

		try {
			Utils.log("Device Manufacturer: ["+usbDevice.getManufacturerString()+"]");
			Utils.log("Device Product     : ["+usbDevice.getProductString()+"]");
		} catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) {
			e.printStackTrace();
			this.statusRunnable.resume();
			return false;
		}


		//		Utils.log("===================================================");
		//		USBUtils.dumpDevice(usbDevice);
		//		Utils.log("====================================================");

		this.usbConfiguration = ((List<UsbConfiguration>) this.usbDevice.getUsbConfigurations()).get(0);
		this.usbInterface = ((List<UsbInterface>) this.usbConfiguration.getUsbInterfaces()).get(0);

		Utils.log("Claiming interface...");

		// 3) Claim the device (force claim)
		try {
			usbInterface.claim();
		} catch (UsbNotActiveException | UsbDisconnectedException | UsbException e) {
			e.printStackTrace();
			this.statusRunnable.resume();
			return false;
		}

		Utils.log("Creating endPoints & pipes..");
		// 4) Get the correct endpoint
		UsbEndpoint endpointRead = this.usbInterface.getUsbEndpoint( AHP_EP1_READ );
		UsbEndpoint endpointWrite = this.usbInterface.getUsbEndpoint( AHP_EP2_WRITE );

		// 5) Get a pipe from the endpoint and open it
		this.usbPipeRead = endpointRead.getUsbPipe();
		this.usbPipeWrite = endpointWrite.getUsbPipe();
		try {
			this.usbPipeRead.open();
			this.usbPipeWrite.open();
		} catch (UsbNotActiveException | UsbNotClaimedException | UsbDisconnectedException | UsbException e) {
			e.printStackTrace();
			this.statusRunnable.resume();
			return false;
		}

		this.readerRunnable = new ReaderRunnable(this.usbPipeRead,this);
		Thread tread = new Thread(this.readerRunnable);
		tread.setName("cm15a reader");
		tread.setDaemon(true);
		tread.start();

		this.connected=true;
		this.statusRunnable.resume();

		return this.connected;
	}

	public void destroy(boolean destroyAll) {
		Utils.logErr("Destroying...");
		if (destroyAll) {
			if (this.statusRunnable!=null) {
				this.statusRunnable.stop();
			}
		}
		if (this.readerRunnable!=null) {
			this.readerRunnable.stop();
		}
		this.connected=false;
		if (this.usbPipeRead!=null) {
			try {
				this.usbPipeRead.abortAllSubmissions();
				this.usbPipeRead.close();
			} catch (UsbNotActiveException | UsbNotOpenException | UsbDisconnectedException | UsbException e) {
			}
		}
		if (this.usbPipeWrite!=null) {
			try {
				this.usbPipeWrite.abortAllSubmissions();
				this.usbPipeWrite.close();
			} catch (UsbNotActiveException | UsbNotOpenException | UsbDisconnectedException | UsbException e) {
			}
		}
		if (this.usbInterface!=null) {
			try {
				this.usbInterface.release();
			} catch (UsbNotActiveException
					| UsbDisconnectedException | UsbException e) {
				e.printStackTrace();
			}
		}
		Utils.logErr("....Destroyed");
	}

	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void onError(String message) {
		Utils.logErr("onERROR received:"+message);
		if (message!=null && (message.indexOf("LIBUSB_ERROR_NO_DEVICE")!=-1 || message.indexOf("LIBUSB_ERROR_IO")!=-1) ) {
			this.connected=false;
			this.statusRunnable.pause();
			Utils.logErr("Device disconnected!!!");
			this.destroy(false);
			Utils.waitFor(Utils.getLong("onError.wait",5000));
			this.statusRunnable.resume();
		}
	}

	@Override
	public void receive(byte[] buffer, int length) {
		// Utils.logHexBuffer("IN ",buffer, length);
		// Receive commands from controller
		if (buffer[0]==(byte)0x5a) {
			short len=buffer[1];
			if (len>=2) {
				// Supplies list of module addresses
				// 5A 02 00 hd (sz=4 or more)
				if (buffer[2]==0x00) {
					lastHC = CM15aData.getHC(buffer,3);
					lastDevice = CM15aData.getDevice(buffer,3);
					Utils.logErr("Received [0] listing & saving HC="+lastHC+", device:"+lastDevice);
					if (this.cm15aDataListener!=null) {
						this.cm15aDataListener.receive(new CM15aData(lastHC, lastDevice, null, 0));
					}
				}
				// Module function, or module function and module device.
				// 5A 02 01 hf (size=4)
				// 5A 03 01 hf hd (size=5)
				// 5A 04 01 hf hd ?? (size=6)
				else if (buffer[2]==0x01) {
					char hc = CM15aData.getHC(buffer,3);
					char hc2=lastHC;
					int device=lastDevice;
					Function function = CM15aData.getFunction(buffer,3);
					Utils.logErr("Received [1] HC="+hc+", function="+function,false);
					if (len>=5) {
						hc2= CM15aData.getHC(buffer,4);
						device = CM15aData.getDevice(buffer,4);
						System.err.print(". Also Received HC="+hc2+", device:"+device+"\n");
					}
					else {
						System.err.print(".\n");
					}
					if (this.cm15aDataListener!=null) {
						this.cm15aDataListener.receive(new CM15aData(hc2, device, function, 0));
					}
				}
				// module Device and Module Function
				// 5A 03 02 ?? hf (size=5)
				// ?? related with time pressing button (dim/bright) ?. 22,33,51,69,179,210
				else if (buffer[2]==0x02) {
					int percentage = CM15aData.getPercentage(buffer, 3);
					char hc2 = CM15aData.getHC(buffer,4);
					Function function = CM15aData.getFunction(buffer,4);
					Utils.logErr("Received [2] HC2="+hc2+", function="+function+". percentage="+percentage);
					if (this.cm15aDataListener!=null) {
						this.cm15aDataListener.receive(new CM15aData(hc2, lastDevice, function, percentage));
					}
				}
				// Module Function and module Device
				// 5A 03 03 hf hd (size=5)
				else if (buffer[2]==0x03) {
					char hc = CM15aData.getHC(buffer,3);
					Function function = CM15aData.getFunction(buffer,3);
					char hc2 = CM15aData.getHC(buffer,4);
					int device = CM15aData.getDevice(buffer,4);
					Utils.logErr("Received [3] HC="+hc+", function="+function+". HC2="+hc2+", device="+device);
					if (this.cm15aDataListener!=null) {
						this.cm15aDataListener.receive(new CM15aData(hc2, device, function, 0));
					}
				}
			}
			else {
				Utils.logErr("Received incorrect length:"+len+", for 5A");
			}
		}
		// module/controller confirm an operation
		else if (buffer[0]==(byte)0x55) {
			this.lock.lock();
			Utils.log("Received confirmation 0x55");
			this.condition.signal();
			this.lock.unlock();
		}
		// RF received
		else if (buffer[0]==(byte)0x5d) {
			Utils.log("Received RF");
		}
		// module/controller asking for clock adjust
		else if (buffer[0]==(byte)0xa5) {
			Utils.logErr("Received Clock Adjust request");
			sendClockAdjust();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	public synchronized boolean send (char hc, int device, Function function, int percen) {

		byte shc=CM15aData.getHCByte(hc);
		byte sdev=CM15aData.getDeviceByte(device);
		byte sop=CM15aData.getFunctionByte(function);
		byte spercen=CM15aData.getPercentageByte(percen);
		
		byte[] bufferWrite = new byte[UsbUtil.unsignedInt(usbPipeWrite.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];

		try {

			Utils.log("Writting...");
			bufferWrite[0]=0x04;
			bufferWrite[1]= (byte) (shc | sdev);
			int lengthWrite = usbPipeWrite.syncSubmit(bufferWrite);
			Utils.logHexBuffer("OUT",bufferWrite, lengthWrite);
			Utils.waitFor(Utils.getLong("write.wait",500));
			bufferWrite[0]=0x06;
			bufferWrite[1]=(byte) (shc | sop);
			if (function==Function.DIM || function==Function.BRIGHT) {
				bufferWrite[2]=spercen;
			}
			lengthWrite = usbPipeWrite.syncSubmit(bufferWrite);
			this.lock.lock();
			try {
				Utils.logHexBuffer("OUT",bufferWrite, lengthWrite);
				this.condition.await(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
			} finally {
				this.lock.unlock();
			}
		}
		catch ( UsbException uE ) {
			if (uE.getMessage().indexOf("LIBUSB_ERROR_TIMEOUT")==-1) {
				Utils.log("########"+uE);
				return false;
			}
		}

		return true;
	}


	private synchronized boolean sendClockAdjust() {
		byte[] bufferWrite = new byte[UsbUtil.unsignedInt(usbPipeWrite.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];

		try {

			Utils.log("Writting...");
			bufferWrite[0]=(byte)0x9b;//function code
			bufferWrite[1]=0x00; // seconds
			bufferWrite[2]=0x00; // minutes + 60 * (hour & 1) //0 -199
			bufferWrite[3]=0x00; // hours >> 1 //0-11 (hours/2)
			bufferWrite[4]=0x00; // yday // really 9 bits
			bufferWrite[4]=0x00; // daymask (7 bits)
			bufferWrite[5]=0x60; // House Code (A) + 0:timer purge, 1:monitor clear, 3:battery clear
			bufferWrite[6]=0x00; // filler
			int lengthWrite = usbPipeWrite.syncSubmit(bufferWrite);
			Utils.logHexBuffer("OUT",bufferWrite, lengthWrite);
		}
		catch ( UsbException uE ) {
			if (uE.getMessage().indexOf("LIBUSB_ERROR_TIMEOUT")==-1) {
				Utils.log("########"+uE);
				return false;
			}
		}
/*
		size=0;
		time(&t);
		tm = localtime(&t);
		sendbuff[size++]=0x9b;//function code
		sendbuff[size++]=tm->tm_sec;//seconds
		sendbuff[size++]=tm->tm_min + 60 * (tm->tm_hour & 1);// 0 -199
		sendbuff[size++]=tm->tm_hour >> 1;//0-11 (hours/2)
		sendbuff[size++]=tm->tm_yday; //really 9 bits
		sendbuff[size]= 1 << tm->tm_wday;//daymask (7 bits)
		if(tm->tm_yday & 0x100) //
		{
			sendbuff[size] |= 0x80;
		}
		size++;
		sendbuff[size++]= 0x60;// house (0:timer purge,
		// 1:monitor clear, 3:battery clear
		sendbuff[size++]= 0x00;// Filler
*/
		return true;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	public boolean parseCommand(String hcdev,String action) {
		return parseCommand(hcdev,action,"0");
	}
	public boolean parseCommand(String hcdev,String action, String percentage) {
		if (hcdev==null || hcdev.length()==0 || action==null || action.length()==0) {
			return false;
		}
		hcdev=hcdev.toUpperCase();
		action=action.toUpperCase();
		char hc=hcdev.charAt(0);
		short dev=Short.parseShort(hcdev.substring(1,hcdev.length()));
		Function function=Function.valueOf(action);
		byte percen=(byte)Integer.parseInt(percentage);
		if (this.connected) {
			return send(hc,dev,function, percen);
		}
		else {
			return false;
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Utils.log("====================================================");
		CM15a cm15a = new CM15a();
		cm15a.init();

		if (args!=null && args.length>=3) {
			cm15a.parseCommand(args[0],args[1],args[2]);
		}
		else if (args!=null && args.length==2) {
			cm15a.parseCommand(args[0],args[1]);
		}
		else {
			interactiveMenu(cm15a);
		}

		Utils.log("====================================================");
		cm15a.destroy(true);
		Utils.logErr("Finished");
		Utils.log("====================================================");
	}

	private static void interactiveMenu(CM15a cm15a) {
		boolean running=true;
		Scanner scan=new Scanner(System.in);
		while(running) {
			Utils.log("====================================================");
			Utils.log("Insert command (ej. a1 on, b3 off,...) and ENTER to exec");
			Utils.log("Press q and ENTER to finish\n");
			
			String line=scan.nextLine();
			
			StringTokenizer st = new StringTokenizer(line," ");
			String hcdev=st.nextToken();
			if (hcdev.toUpperCase().startsWith("Q")) {
				System.out.println("Quit:");
				running=false;
				break;
			}
			else if (hcdev.toUpperCase().startsWith("H")) {
				System.out.println("Help:");
				System.out.println("h (help),q (quit), r (reload config)");
				System.out.println("HD F (H=House Code. D=Device Number. F=Function)");
				System.out.println("Functions: "+Arrays.toString(Function.values()));
			}
			else if (hcdev.toUpperCase().startsWith("R")) {
				System.out.println("Reload config:");
				Utils.reload();
			}
			else {
				String action=st.nextToken();
				System.out.print("Exec command: HD="+hcdev+". F="+action);
				String percent="0";
				if (st.hasMoreTokens()) {
					percent=st.nextToken();
					System.out.println(". Percent="+percent);
					running=cm15a.parseCommand(hcdev,action,percent);
				}
				else {
					System.out.println(".");
					running=cm15a.parseCommand(hcdev,action);
				}
				
			}
		}
		scan.close();
	}
}