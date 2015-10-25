import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class RDT10 implements Runnable {
	private UChannel forward;
	private RSender10 sender;
	private RReceiver10 receiver;
	private StringPitcher sp = null;		
	
	public RDT10(double pmunge) throws IOException {this(pmunge, 0.0, null);}

	public RDT10(double pmunge, double plost, String filename) throws IOException {
		forward = new UChannel(pmunge, plost);
		if (filename != null) sp = new StringPitcher(new File(System.getenv("user.dir"), filename), 2000, 1000);
		sender = new RSender10(forward);
		receiver = new RReceiver10(forward);
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
	
	public class RSender10 extends FSM {
		protected UChannel forward;
		protected BufferedReader br;

		public RSender10(UChannel forward) throws IOException {
			this.forward = forward;
			br = (sp != null) ? sp.getReader() : new BufferedReader(new InputStreamReader(System.in));
		}
		@Override
		public int loop(int myState) throws IOException {
			switch(myState) {
			case 0:
				String dat = br.readLine();
				if (sp != null) System.out.println(dat);
				try {
					Thread.sleep(750);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Packet packet = new Packet(dat);
				forward.send(packet.serialize());
				return 0;
			}
			return myState;				
		}
	}
	
	public class RReceiver10 extends FSM {
		protected UChannel forward;

		public RReceiver10(UChannel forward) throws IOException {
			this.forward = forward;
		}
		@Override
		public int loop(int myState) throws IOException {
			switch (myState) {
			case 0:
				String dat = forward.receive();
				Packet packet = Packet.deserialize(dat);
				System.out.println("         "+packet.data);
				return 0;
			}
			return myState;				
		}
	}
	@Override
	public void run() {
		new Thread(forward).start();
		new Thread(sender).start();
		new Thread(receiver).start();
		if (sp != null) new Thread(sp).start();
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = UChannel.argParser("RDT10", args);
		RDT10 rdt10 = new RDT10((Double)pargs[0], (Double)pargs[1], (String)pargs[3]);
		rdt10.run();
	}
}