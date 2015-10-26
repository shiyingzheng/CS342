import java.io.IOException;

public class RDT20 extends RTDBase {

	public RDT20(double pmunge) throws IOException {this(pmunge, 0.0, null);}

	public RDT20(double pmunge, double plost, String filename) throws IOException {
		super(pmunge, plost, filename);
		sender = new RSender20();
		receiver = new RReceiver20();
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
	
	public class RSender20 extends RSender {
		Packet packet = null;
		@Override
		public int loop(int myState) throws IOException {
		    // your code here
			return myState;			
		}
	}

	public class RReceiver20 extends RReceiver {
		@Override
		public int loop(int myState) throws IOException {
		    // your code here
			return myState;
		}
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = UChannel.argParser("RDT10", args);
		RDT20 rdt20 = new RDT20((Double)pargs[0], (Double)pargs[1], (String)pargs[3]);
		rdt20.run();
	}
	
}
