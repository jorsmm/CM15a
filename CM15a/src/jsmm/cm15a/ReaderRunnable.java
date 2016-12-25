package jsmm.cm15a;

import javax.usb.UsbException;
import javax.usb.UsbPipe;
import javax.usb.util.UsbUtil;

/**
 * Class to listen in a dedicated Thread input data (read from device).
 * <p>
 */
public class ReaderRunnable implements Runnable {

	private boolean running = true;
	private UsbPipe usbPipe = null;
	private ReadDataListener readDataListener=null;
	

	public ReaderRunnable(UsbPipe pipe, ReadDataListener readDataListener) {
		usbPipe = pipe;
		this.readDataListener=readDataListener;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[UsbUtil.unsignedInt(usbPipe.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];

		int length = 0;

		while (running) {
			try {
				length = usbPipe.syncSubmit(buffer);
				if (running) {
					readDataListener.receive(buffer, length);
				}
			}
			catch ( UsbException uE ) {
				if (running) {
					if (uE.getMessage().indexOf("LIBUSB_ERROR_TIMEOUT")==-1) {
						Utils.log("########"+uE);
						running=false;
						readDataListener.onError(uE.getMessage());
					}
				}
			}
		}
	}

	

	/**
	 * Stop/abort listening for data events.
	 */
	public void stop() {
		running = false;
	}
}
