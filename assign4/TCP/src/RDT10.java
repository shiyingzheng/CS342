import java.io.IOException;

public class RDT10 extends RTDBase {
	
	public RDT10(double pmunge) throws IOException {this(pmunge, 0.0, null);}

	public RDT10(double pmunge, double plost, String filename) throws IOException {
		super(pmunge, plost, filename);
		sender = new RSender10();
		receiver = new RReceiver10();
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


	public class RSender10 extends RSender {
		@Override
		public int loop(int myState) throws IOException {
			switch(myState) {
			case 0:
				String dat = getFromApp(0);
				forward.send(new Packet(dat));
				return 0;
			}
			return myState;				
		}
	}
	
	public class RReceiver10 extends RReceiver {
		@Override
		public int loop(int myState) throws IOException {
			switch (myState) {
			case 0:
				String dat = forward.receive();
				Packet packet = Packet.deserialize(dat);
				deliverToApp(packet.data);
				return 0;
			}
			return myState;				
		}
	}

	public static void main(String[] args) throws IOException {
		Object[] pargs = UChannel.argParser("RDT10", args);
		RDT10 rdt10 = new RDT10((Double)pargs[0], (Double)pargs[1], (String)pargs[3]);
		rdt10.run();
	}
}