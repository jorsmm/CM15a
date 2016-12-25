package jsmm.cm15a;

public interface ReadDataListener {
	public void receive(byte [] buffer, int length);
	public void onError(String message);
}