import java.io.IOException;

public class RDT21 extends RTDBase {
	
	public RDT21(double pmunge) throws IOException {this(pmunge, 0.0, null);}

	public RDT21(double pmunge, double plost, String filename) throws IOException {
		super(pmunge, plost, filename);
		sender = new RSender21();
		receiver = new RReceiver21();
	}

	public static class Packet implements PacketType{
		String checksum;
		String data;
		String seqnum;
		
		public Packet(String data){
			this(data, " ");
		}
		public Packet(String data, String seqnum){
			this(data, seqnum, CkSum.genCheck(seqnum+data));
		}
		public Packet(String data, String seqnum, String checksum) {
			this.data = data;
			this.seqnum = seqnum;
			this.checksum = checksum;
		}
		public static Packet deserialize(String data) {
			String hex = data.substring(0, 4);
			String seqnum = data.substring(4,5);
			String dat = data.substring(5);
			return new Packet(dat, seqnum, hex);
		}
		@Override
		public String serialize() {
			return checksum+seqnum+data;
		}
		@Override
		public boolean isCorrupt() {
			return !CkSum.checkString(seqnum+data, checksum);
		}
		@Override
		public String toString() {
			return String.format("%s %s (%s/%s)", data, seqnum, checksum, CkSum.genCheck(seqnum+data));
		}
	}
	
	public class RSender21 extends RSender {
		Packet packet = null;
		@Override
		public int loop(int myState) throws IOException {
		    // your code here
			return myState;
		}
	}

	public class RReceiver21 extends RReceiver {
		@Override
		public int loop(int myState) throws IOException {
		    // your code here
			return myState;
		}
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = UChannel.argParser("RDT10", args);
		RDT21 rdt21 = new RDT21((Double)pargs[0], (Double)pargs[1], (String)pargs[3]);
		rdt21.run();
	}
	
}
