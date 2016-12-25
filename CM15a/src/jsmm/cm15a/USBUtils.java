package jsmm.cm15a;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbPort;
import javax.usb.UsbServices;

public class USBUtils {

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
	static void dumpDevice(final UsbDevice device)
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

}
