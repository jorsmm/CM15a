package jsmm.cm15a;


public class CM15aData {

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	private static byte devmap[]={
			(byte)0xff,
			0x06,0x0e,0x02,0x0a,0x01,0x09,0x05,0x0d,
			0x07,0x0f,0x03,0x0b,0x00,0x08,0x04,0x0c};
	private static String sdevmap = new String(devmap);

	private static char hcmap[]={
			'M','E','C','K','O','G','A','I',
			'N','F','D','L','P','H','B','J'};
	private static String shcmap = new String(hcmap);
	// ordered
	public static enum Function {ALLUOFF,ALLLON,ON,OFF,DIM,BRIGHT,ALLLOFF,EXTENDEDCODE,HAILREQ,HAILACK,PDIML,PDIMH,EXTENDEDDATA,STATON,STATOFF,STATREQ;
		public static Function getOpposite (Function function) {
			switch (function) {
			case ON:
				return OFF;
			case OFF:
				return ON;
			case DIM:
				return BRIGHT;
			case BRIGHT:
				return DIM;
			case ALLLOFF:
				return ALLLON;
			case ALLLON:
				return ALLLOFF;
			default:
				return function;
			}
		};
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	private char hc;
	private int device;
	private Function function;
	private int percentage;
	
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	public CM15aData(char hc, int device, Function function, int percentage) {
		super();
		this.hc = hc;
		this.device = device;
		this.function = function;
		this.percentage = percentage;
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	public char getHc() {
		return hc;
	}
	public void setHc(char hc) {
		this.hc = hc;
	}
	public int getDevice() {
		return device;
	}
	public void setDevice(int device) {
		this.device = device;
	}
	public Function getFunction() {
		return function;
	}
	public void setFunction(Function function) {
		this.function = function;
	}
	public int getPercentage() {
		return percentage;
	}
	public void setPercentage(int percentage) {
		this.percentage = percentage;
	}
	
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////	
	protected static Function getFunction(byte[] buffer, int offset) {
		byte bhf=(byte) (buffer[offset] & (byte)0x0F);
		Function[] functions=Function.values();
		Function function=functions[bhf];
		return function;
	}

	protected static int getDevice(byte[] buffer, int offset) {
		byte bdevice=(byte) (buffer[offset] & (byte)0x0F);
		int device=sdevmap.indexOf(bdevice);
		return device;
	}
	protected static char getHC(byte[] buffer, int offset) {
		int ihc=(buffer[offset] & 0xF0) >> 4;
		char hc=hcmap[ihc];
		return hc;
	}
	protected static int getPercentage(byte[] buffer, int offset) {
		return ((buffer[offset] & 0xFF)-1) >>1;
	}

	/////////////////////////////////////////////////////////////////////////////
	protected static byte getHCByte(char hc) {
		byte shc=(byte) shcmap.indexOf(hc);
		return (byte) (shc<<4 & 0xf0);
	}
	protected static byte getDeviceByte(int device) {
		return (byte) devmap[device];
	}
	protected static byte getFunctionByte(Function function) {
		return 	(byte)function.ordinal();
	}
	public static byte getPercentageByte(int percen) {
		return (byte) ((byte)percen<<1+1);
	}	

	
	
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CM15Data [hc=").append(hc).append(", device=")
				.append(device).append(", function=").append(function)
				.append(", percentage=").append(percentage).append("]");
		return builder.toString();
	}
}