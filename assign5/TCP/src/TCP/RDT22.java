package TCP;
import java.io.IOException;

/**
 * Implements simulator using rdt2.2 protocol
 * 
 * @author rms
 *
 */
public class RDT22 extends RTDBase {
	
	/**
	 * Constructs an RDT22 simulator with given munge factor
	 * @param pmunge		probability of character errors
	 * @throws IOException	if channel transmissions fail
	 */
	public RDT22(double pmunge) throws IOException {this(pmunge, 0.0, null);}

	/**
	 * Constructs an RDT22 simulator with given munge factor, loss factor and file feed
	 * @param pmunge		probability of character errors
	 * @param plost			probability of packet loss
	 * @param filename		file used for automatic data feed
	 * @throws IOException	if channel transmissions fail
	 */
	public RDT22(double pmunge, double plost, String filename) throws IOException {
		super(pmunge, plost, filename);
		sender = new RSender22();
		receiver = new RReceiver22();
	}

	/**
	 * Packet appropriate for rdt2.2;
	 * contains data, seqnum and checksum
	 * @author rms
	 *
	 */
	public static class Packet extends RDT21.Packet {
		public Packet(String data){
			super(data);
		}
		public Packet(String data, String seqnum){
			super(data, seqnum);
		}
		public Packet(String data, String seqnum, String checksum) {
			super(data, seqnum, checksum);
		}
		public static Packet deserialize(String data) {
			String hex = data.substring(0, 4);
			String seqnum = data.substring(4,5);
			String dat = data.substring(5);
			return new Packet(dat, seqnum, hex);
		}
	}
	
	/**
	 * RSender Class implementing rdt2.2 protocol
	 * @author rms
	 *
	 */
	public class RSender22 extends RSender {
		Packet packet = null;
		@Override
		public int loop(int myState) throws IOException {
			switch(myState) {
			case 0: // wait for 0 from above
				String line = getFromApp(0);
				if (sp != null) System.out.println(line);				
				packet = new Packet(line, "0");
				System.out.println("Sender(0): "+packet);
				try {
					Thread.sleep(750);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				forward.send(packet);
				System.out.println("  **Sender(0->1): ");
				System.out.flush();
				return 1;
			case 1: // wait for ACK or NAK from below
				Packet ackNack = Packet.deserialize(backward.receive()); 
				System.out.println("  **Sender(1): "+ackNack+" ***");
				System.out.flush();
				if (ackNack.isCorrupt() || ackNack.seqnum.equals("1")) {
					System.out.println("  **Sender(1->1): wrong or corrupt acknowledgement; resending ***");
					System.out.flush();
					forward.send(packet);
					return 1;
				} else {
					System.out.println("  **Sender(1->2)");
					System.out.flush();
					return 2;					
				}
			case 2: // wait for 1 from above
				line = getFromApp(0);
				if (sp != null) System.out.println(line);
				packet = new Packet(line, "1");
				System.out.println("Sender(2): "+packet);
				try {
					Thread.sleep(750);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				forward.send(packet);
				System.out.println("  **Sender(2->3): ");
				return 3;
			case 3: // wait for ACK or NAK from below
				ackNack = Packet.deserialize(backward.receive()); 
				System.out.println("  **Sender(3): "+ackNack+" ***");
				if (ackNack.isCorrupt() || ackNack.seqnum.equals("0")) {
					System.out.println("  **Sender(3->3): wrong or corrupt acknowledgement; resending ***");										
					forward.send(packet);
					return 3;
				} else {
					System.out.println("  **Sender(3->0)");
					return 0;					
				}
			}
			return myState;
		}
	}

	/**
	 * RReceiver Class implementing rdt2.2 protocol
	 * @author rms
	 *
	 */
	public class RReceiver22 extends RReceiver {
		@Override
		public int loop(int myState) throws IOException {
			switch (myState) {
			case 0: // wait for 0 from below
				Packet packet = Packet.deserialize(forward.receive());
				System.out.println("         **Receiver(0): "+packet+" **");
				if (packet.isCorrupt()) {
					System.out.println("         **Receiver(0->0): corrupt data; replying ACK/1 **");
					backward.send(new Packet("ACK", "1"));
					return 0;
				} 
				if (packet.seqnum.equals("1")) {
					System.out.println("         **Receiver(0->0): duplicate 1 packet; discarding; replying ACK/1 **");
					backward.send(new Packet("ACK", "1"));
					return 0;
				} 
				System.out.println("         **Receiver(0->1): ok 0 data; replying ACK/0 **");					
				System.out.println("--->               "+packet.data);
				backward.send(new Packet("ACK", "0"));
				return 1;
			case 1: // wait for 1 from below
				packet = Packet.deserialize(forward.receive());
				System.out.println("         **Receiver(1): "+packet+" **");
				if (packet.isCorrupt()) {
					System.out.println("         **Receiver(1->1): corrupt data; replying ACK/0 **");
					backward.send(new Packet("ACK", "0"));
					return 1;
				} 
				if (packet.seqnum.equals("0")) {
					System.out.println("         **Receiver(1->1): duplicate 0 packet; discarding; replying ACK/0 **");
					backward.send(new Packet("ACK", "0"));
					return 1;
				} 
				System.out.println("         **Receiver(1->0): ok 1 data; replying ACK/1 **");					
				System.out.println("--->               "+packet.data);
				backward.send(new Packet("ACK, 1"));				
				return 0;
			}
			return myState;
		}
	}

	
	/**
	 * Runs rdt2.2 simulation
	 * @param args	[-m pmunge][-l ploss][-f filename]
	 * @throws IOException	if i/o error occurs
	 */
	public static void main(String[] args) throws IOException {
		Object[] pargs = argParser("RDT22", args);
		RDT22 rdt22 = new RDT22((Double)pargs[0], (Double)pargs[1], (String)pargs[3]);
		rdt22.run();
	}
	
}
