import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class RTDBase implements Runnable {
	protected Channel forward, backward;
	protected RSender sender;
	protected RReceiver receiver;
	protected StringPitcher sp = null;		

	protected RTDBase(double pmunge, double plost, String filename) throws IOException {
		if (filename != null) sp = new StringPitcher(new File(System.getenv("user.dir"), filename), 2000, 1000);
		this.forward = new UChannel(pmunge, plost);
		this.backward = new UChannel(pmunge, plost);
	}
	
	protected abstract class RSender extends FSM {
		protected BufferedReader appIn;
		
		protected RSender() {
			appIn = (sp != null) ? sp.getReader() : new BufferedReader(new InputStreamReader(System.in));
		}

		protected String getFromApp(int delay) throws IOException {
			String dat = appIn.readLine();
			if (sp != null) {
				System.out.println(dat);
				if (delay > 0) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return dat;
		}
		
		@Override
		public abstract int loop(int myState) throws IOException;
	}

	protected abstract class RReceiver extends FSM {
		protected void deliverToApp(String dat) {
			System.out.println("-->        "+dat);
		}
		@Override
		public abstract int loop(int myState) throws IOException;
	}
	
	@Override
	public void run() {
		if (forward != null) new Thread(forward).start();
		if (backward != null) new Thread(backward).start();
		if (sender != null) new Thread(sender).start();
		if (receiver != null) new Thread(receiver).start();
		if (sp != null) new Thread(sp).start();
	}

}
