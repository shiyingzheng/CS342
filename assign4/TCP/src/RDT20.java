import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class RDT20 implements Runnable {
	private UChannel forward, backward;
	private RSender20 sender;
	private RReceiver20 receiver;
	private StringPitcher sp = null;
	
	public RDT20(double pmunge) throws IOException {this(pmunge, 0.0, null);}

	public RDT20(double pmunge, double plost, String filename) throws IOException {
		forward = new UChannel(pmunge, plost);
		backward = new UChannel(pmunge, plost);
		if (filename != null) sp = new StringPitcher(new File(System.getenv("user.dir"), filename), 2000, 1000);
		sender = new RSender20(forward, backward);
		receiver = new RReceiver20(forward, backward);
	}

	public static class Packet implements PacketType{
		String checksum;
		String data;
		public Packet(String data){
			this(data, CkSum.genCheck(data));
		}
		public Packet(String data, String checksum) {
			this.data = data;
			this.checksum = checksum;
		}
		public static Packet deserialize(String data) {
			String hex = data.substring(0, 4);
			String dat = data.substring(4);
			return new Packet(dat, hex);
		}
		@Override
		public String serialize() {
			return checksum+data;
		}
		@Override
		public boolean isCorrupt() {
			return !CkSum.checkString(data, checksum);
		}
		@Override
		public String toString() {
			return String.format("%s (%s/%s)", data, checksum, CkSum.genCheck(data));
		}
	}
	
	public class RSender20 extends FSM {
		protected UChannel forward;
		protected UChannel backward;
		Packet packet = null;
		protected BufferedReader br;
		public RSender20(UChannel forward, UChannel backward) throws IOException {
			this.forward = forward;
			this.backward = backward;
			this.br = (sp != null) ? sp.getReader() : new BufferedReader(new InputStreamReader(System.in));
		}
		@Override
		public int loop(int myState) throws IOException {
			// Your code here
			return myState;			
		}
	}

	public class RReceiver20 extends FSM {
		protected UChannel forward;
		public RReceiver20(UChannel forward, UChannel backward) throws IOException {
			this.forward = forward;
		}
		@Override
		public int loop(int myState) throws IOException {
			// Your code here
			return myState;
		}
	}
	@Override
	public void run() {
		new Thread(forward).start();
		new Thread(backward).start();
		new Thread(sender).start();
		new Thread(receiver).start();
		new Thread(sp).start();
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = UChannel.argParser("RDT10", args);
		RDT20 rdt20 = new RDT20((Double)pargs[0], (Double)pargs[1], (String)pargs[3]);
		rdt20.run();
	}
	
}
