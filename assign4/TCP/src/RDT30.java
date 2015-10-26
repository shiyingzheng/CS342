import java.io.IOException;

public class RDT30 extends RTDBase {
	int timeout;
	public RDT30(double pmunge, double ploss, int timeout, String filename) throws IOException {
		super(pmunge, ploss, filename);
		this.timeout = timeout;
		backward = new TUChannel((UChannel)backward);
		sender = new RSender30();
		receiver = new RReceiver30();
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
	
	public class RSender30 extends RSender {
		Packet packet = null;
		TUChannel backward = (TUChannel)RDT30.this.backward;
		@Override
		public int loop(int myState) throws IOException {
		    // your code here
		    return myState;
		}
	}

	public class RReceiver30 extends RReceiver {
		@Override
		public int loop(int myState) throws IOException {
		    // your code here
		    return myState;			
		}
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = UChannel.argParser("RDT10", args);
		RDT30 rdt30 = new RDT30((Double)pargs[0], (Double)pargs[1], (Integer)pargs[2], (String)pargs[3]);
		rdt30.run();
	}
	
}
