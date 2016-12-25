package jsmm;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.usb.*;
import javax.usb.util.UsbUtil;

import java.io.UnsupportedEncodingException;

public class Pruebas {

	short devmap[]={
		    0xff,
		    0x06,0x0e,0x02,0x0a,0x01,0x09,0x05,0x0d,
		    0x07,0x0f,0x03,0x0b,0x00,0x08,0x04,0x0c};
	
	char uhcmap[]={
		    'M','E','C','K','O','G','A','I',
		        'N','F','D','L','P','H','B','J'};
	
	private static final short CM15A_VENDORID=0x0bc7;
	private static final short CM15A_PRODUCTID=0x0001;
	
	// control read
	private static final byte AHP_EP1_READ=(byte) 0x81;
	// bulk write
	private static final byte AHP_EP2_WRITE=(byte) 0x02;
	//bulk read
	//#define AHP_EP3              0x83
	
	/**
	 * Returns an array of currently connected usb devices
	 */
	public static List<UsbDevice> getDevices()  throws SecurityException, UsbException, UnsupportedEncodingException {
	    ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>();

	    UsbServices services = UsbHostManager.getUsbServices();
	    UsbHub root = services.getRootUsbHub();
	    devices.addAll( getHubDevices( root ) );

	    return devices;
	}

	/**
	 * Returns a list of devices attached to the hub
	 */
	@SuppressWarnings("unchecked")
	public static List<UsbDevice> getHubDevices( UsbHub hub )  throws UnsupportedEncodingException, UsbException {
	    ArrayList<UsbDevice> devices = new ArrayList<UsbDevice>();

	    for (UsbDevice child: (List<UsbDevice>)hub.getAttachedUsbDevices()) {
	        if( child.isUsbHub() ) {
	            devices.addAll( getHubDevices( (UsbHub)child ) );
	        }
	        else {
	            devices.add( child );
	        }
	    }

	    return devices;
	}

	public static UsbDevice findDevice(List<UsbDevice> devices, short vendorId, short productId) {
        for (UsbDevice device : devices) {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
            	return device;
            }
        }
        return null;
    }

    /**
     * Dumps the specified USB device to stdout.
     * 
     * @param device
     *            The USB device to dump.
     */
    @SuppressWarnings("unchecked")
	private static void dumpDevice(final UsbDevice device)
    {
        // Dump information about the device itself
        System.out.println(device);
        final UsbPort port = device.getParentUsbPort();
        if (port != null)
        {
            System.out.println("Connected to port: " + port.getPortNumber());
            System.out.println("Parent: " + port.getUsbHub());
        }

        // Dump device descriptor
        System.out.println(device.getUsbDeviceDescriptor());

        // Process all configurations
        for (UsbConfiguration configuration: (List<UsbConfiguration>) device
            .getUsbConfigurations())
        {
            // Dump configuration descriptor
            System.out.println(configuration.getUsbConfigurationDescriptor());

            // Process all interfaces
            for (UsbInterface iface: (List<UsbInterface>) configuration
                .getUsbInterfaces())
            {
                // Dump the interface descriptor
                System.out.println(iface.getUsbInterfaceDescriptor());

                // Process all endpoints
                for (UsbEndpoint endpoint: (List<UsbEndpoint>) iface
                    .getUsbEndpoints())
                {
                    // Dump the endpoint descriptor
                    System.out.println(endpoint.getUsbEndpointDescriptor());
                }
            }
        }
    }

    private static String getTime() {
    	return new SimpleDateFormat("[dd:mm:yyyyy HH:MM:ss.SSS]").format(new Date());
    }
    
    private static void log(String log) {
    	System.out.print("\n"+getTime()+" "+log);
    }
    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
    	
    	log("Getting Devices...");
    	// 1) Find all connected (non-hub) USB devices:
    	List<UsbDevice> allDevices=null;
		try {
			allDevices = getDevices();
		} catch (SecurityException | UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(1);
		} 

    	log("Finding device...");
		// 2) Iterate the list to find the UsbDevice you're looking for ( match the manufacturer/product id )
    	UsbDevice usbDevice = findDevice(allDevices, CM15A_VENDORID, CM15A_PRODUCTID);
    	
    	if (usbDevice==null) {
    		System.exit(2);
    	}
    	
		//usbInterface=usbDevice;
		UsbDeviceDescriptor desc = usbDevice.getUsbDeviceDescriptor();
		log("device: Manufacturer ["+usbDevice.getManufacturerString()+"] Product ["+usbDevice.getProductString()+"]");
		//System.out.println("desc:"+desc);
		System.out.format("\nVendorID:%04x. ProductId:%04x%n", desc.idVendor() & 0xffff, desc.idProduct() & 0xffff);
		
		log("===================================================\n");
		dumpDevice(usbDevice);
		log("====================================================\n");
		
		UsbConfiguration usbConfiguration = ((List<UsbConfiguration>) usbDevice.getUsbConfigurations()).get(0);
		UsbInterface usbInterface = ((List<UsbInterface>)usbConfiguration.getUsbInterfaces()).get(0);
		
    	log("Claiming interface...");

    	// 3) Claim the device (force claim)
    	usbInterface.claim( new UsbInterfacePolicy() {
			@Override
			public boolean forceClaim(UsbInterface arg0) {
				log("forceClaim:"+arg0);
				return true;
			}} );
    	
    	log("Creating endPoints & pipes..");
    	// 4) Get the correct endpoint
    	UsbEndpoint endpointRead = usbInterface.getUsbEndpoint( AHP_EP1_READ );
    	UsbEndpoint endpointWrite = usbInterface.getUsbEndpoint( AHP_EP2_WRITE );

    	// 5) Get a pipe from the endpoint and open it
    	UsbPipe usbPipeRead = endpointRead.getUsbPipe();
    	usbPipeRead.open();
    	
    	UsbPipe usbPipeWrite = endpointWrite.getUsbPipe();
    	usbPipeWrite.open();
    	
		log("====================================================\n");

    	byte[] bufferWrite = new byte[UsbUtil.unsignedInt(usbPipeWrite.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];

    	try {
	    	log("Writting...");
	    	bufferWrite[0]=0x04;
	    	bufferWrite[1]=0x66;
	    	int lengthWrite = usbPipeWrite.syncSubmit(bufferWrite);
	    	log("Got " + lengthWrite + " bytes of data written");
	    	Thread.sleep(500);
	    	bufferWrite[0]=0x06;
	    	bufferWrite[1]=0x63;
	    	lengthWrite = usbPipeWrite.syncSubmit(bufferWrite);
	    	log("Got " + lengthWrite + " bytes of data written");
	
	    	Thread.sleep(1000);
	    	bufferWrite[0]=0x04;
	    	bufferWrite[1]=0x66;
	    	lengthWrite = usbPipeWrite.syncSubmit(bufferWrite);
	    	log("Got " + lengthWrite + " bytes of data written");
	    	Thread.sleep(500);
	    	bufferWrite[0]=0x06;
	    	bufferWrite[1]=0x62;
	    	lengthWrite = usbPipeWrite.syncSubmit(bufferWrite);
	    	log("Got " + lengthWrite + " bytes of data written");
    	}
    	catch ( UsbException uE ) {
    		if (uE.getMessage().indexOf("LIBUSB_ERROR_TIMEOUT")==-1) {
    			log("########"+uE);
			//uE.printStackTrace();
    		}
    	}

		log("====================================================\n");

		/*
    	byte bmRequestType = UsbConst.REQUESTTYPE_DIRECTION_IN | UsbConst.REQUESTTYPE_TYPE_STANDARD | UsbConst.REQUESTTYPE_RECIPIENT_DEVICE;
        byte bRequest = UsbConst.REQUEST_GET_DESCRIPTOR;
        short wValue = UsbConst.DESCRIPTOR_TYPE_DEVICE << 8;
        short wIndex = 0;
        // For this specific case, where we are getting a device descriptor,
        // 256 bytes is enough; device descriptors are fixed-length.
        
        byte[] buffer = new byte[256];
	*/
    	
    	// UsbControlIrp irp = pipe.createUsbControlIrp(arg0, arg1, arg2, arg3)
    	
    	byte[] bufferRead = new byte[UsbUtil.unsignedInt(usbPipeRead.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];
    	
    	boolean running=true;
    	long length=0;
    	while (running){
	    	try {
	    		length = usbPipeRead.syncSubmit(bufferRead);
	    		log("Got " + length + " bytes of data:");
	    		for (int i=0; i<length; i++)
	    			System.out.print(" 0x" + UsbUtil.toHexString(bufferRead[i]));
	    	}
	    	catch ( UsbException uE ) {
	    		if (uE.getMessage().indexOf("LIBUSB_ERROR_TIMEOUT")==-1) {
	    			log("########"+uE);
				//uE.printStackTrace();
				break;
	    		}
	    	}
    	}
    	
    	usbPipeRead.abortAllSubmissions();
    	
    	//6) Use either the UsbIrp object or a byte[] to communicate over the open pipe via the sync/async methods.
    	
    	/*
    	UsbControlIrp irp = pipe.createUsbControlIrp(bmRequestType, bRequest,
    			wValue, wIndex);
    			7.  byte data[] = new byte[8];
    			8.  irp.setData(data);
    			9.  pipe.open();
    			10. int a =0;
    			11. while(true){
    			12.   pipe.syncSubmit(irp);
    			13.   for(int i=0; i<data.length; i++){
    			14.     System.out.print(data[i]+" ");
    			15.   }
    			16.   irp.setComplete(false);
    			17.   sleep(1000);
    			18. }
*/
    }
}
