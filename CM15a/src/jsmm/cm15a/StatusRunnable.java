package jsmm.cm15a;

public class StatusRunnable implements Runnable {

	CM15a cm15a;
	boolean running=true;
	boolean pause=true;
	
	public StatusRunnable (CM15a cm15a) {
		this.cm15a=cm15a;
	}
	
	@Override
	public void run() {
		while (running) {
			try {
				Thread.sleep(Utils.getLong("status.wait",5000));
			} catch (InterruptedException e) {
			}
			if (running && !pause) {
				if (!cm15a.isConnected()) {
					Utils.log("Detected device disconnected, reloading...");
					cm15a.init();
				}
			}
		}
	}
	
	public void stop() {
		running = false;
	}
	public void pause() {
		Utils.log("pause");
		pause = true;
	}
	public void resume() {
		Utils.log("resume");
		pause=false;
	}
}