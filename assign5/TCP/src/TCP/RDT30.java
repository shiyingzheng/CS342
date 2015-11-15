package TCP;
import java.io.IOException;

/**
 * Implements simulator using rdt3.0 protocol
 * 
 * @author rms
 *
 */
public class RDT30 extends RTDBase {
	int timeout;
	/**
	 * Constructs an RDT22 simulator with given munge factor, loss factor and file feed
	 * @param pmunge		probability of character errors
	 * @param plost			probability of packet loss
	 * @param timeout		receive timeout in milliseconds 
	 * @param filename		file used for automatic data feed
	 * @throws IOException	if channel transmissions fail
	 */
	public RDT30(double pmunge, double plost, int timeout, String filename) throws IOException {
		super(pmunge, plost, filename);
		this.timeout = timeout;
		backward = new TUChannel(pmunge, plost);
		sender = new RSender30();
		receiver = new RReceiver30();
	}

	/**
	 * Packet appropriate for rdt3.0;
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
	 * RSender Class implementing rdt3.0 protocol
	 * @author rms
	 *
	 */
	public class RSender30 extends RSender {
		Packet packet = null;
		TUChannel backward = (TUChannel)RDT30.this.backward;
		@Override
		public int loop(int myState) throws IOException {
			switch(myState) {
			case 0: // wait for 0 from above
				String line = getFromApp(0);
				packet = new Packet(line, "0");
				System.out.println("Sender(0): "+packet);
				forward.send(packet);
				System.out.println("  **Sender(0->1): ");
				backward.startTimer(timeout);
				return 1;
			case 1: // wait for ACK or NAK from below
				String ackNackMsg = null;
				Packet ackNack = null;				
				try {
					ackNackMsg = backward.receive();
					ackNack = Packet.deserialize(ackNackMsg); 
					System.out.println("  **Sender(1): "+ackNack+" ***");
					if (ackNack.isCorrupt() || ackNack.seqnum.equals("1")) {
						System.out.println("  **Sender(1->1): wrong or corrupt acknowledgement ***");
						return 1;
					}
					backward.stopTimer();
				} catch (TUChannel.TimedOutException ex) {
					System.out.println("  **Sender(1->1): timeout; resending ***");
					forward.send(packet);
					backward.startTimer(timeout);					
					return 1;					
				}
				System.out.println("  **Sender(1->2)");
				return 2;					
			case 2: // wait for 1 from above
				line = getFromApp(0);
				packet = new Packet(line, "1");
				System.out.println("Sender(2): "+packet);
				forward.send(packet);
				System.out.println("  **Sender(2->3): ");
				backward.startTimer(timeout);				
				return 3;
			case 3: // wait for ACK or NAK from below
				ackNackMsg = null;
				ackNack = null;				
				try {
					ackNackMsg = backward.receive();
					ackNack = Packet.deserialize(ackNackMsg); 
					System.out.println("  **Sender(3): "+ackNack+" ***");
					if (ackNack.isCorrupt() || ackNack.seqnum.equals("0")) {
						System.out.println("  **Sender(3->3): wrong or corrupt acknowledgement ***");
						return 3;
					}
					backward.stopTimer();								
				} catch (TUChannel.TimedOutException ex) {
					System.out.println("  **Sender(3->3): timeout; resending ***");
					forward.send(packet);
					backward.startTimer(timeout);					
					return 3;
				}
				System.out.println("  **Sender(3->0)");
				return 0;					
			}
			return myState;
		}
	}

	/**
	 * RReceiver Class implementing rdt3.0 protocol
	 * @author rms
	 *
	 */
	public class RReceiver30 extends RReceiver {
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
				} else if (packet.seqnum.equals("1")) {
					System.out.println("         **Receiver(0->0): duplicate 1 packet; discarding; replying ACK/1 **");
					backward.send(new Packet("ACK", "1"));
					return 0;
				} else {
					System.out.println("         **Receiver(0->1): ok 0 data; replying ACK/0 **");					
					System.out.println("--->               "+packet.data);
					backward.send(new Packet("ACK", "0"));
					return 1;
				}
			case 1: // wait for 1 from below
				packet = Packet.deserialize(forward.receive());
				System.out.println("         **Receiver(1): "+packet+" **");
				if (packet.isCorrupt()) {
					System.out.println("         **Receiver(1->1): corrupt data; replying ACK/0 **");
					backward.send(new Packet("ACK", "0"));
					return 1;
				} else if (packet.seqnum.equals("0")) {
					System.out.println("         **Receiver(1->1): duplicate 0 packet; discarding; replying ACK/0 **");
					backward.send(new Packet("ACK", "0"));
					return 1;
				} else {
					System.out.println("         **Receiver(1->0): ok 1 data; replying ACK/1 **");					
					System.out.println("--->               "+packet.data);
					backward.send(new Packet("ACK, 1"));				
					return 0;
				}
			}
			return myState;			
		}
	}

	/**
	 * Runs rdt2.2 simulation
	 * @param args	[-m pmunge][-l ploss][-t timeout][-f filename]
	 * @throws IOException	if i/o error occurs
	 */
	public static void main(String[] args) throws IOException {
		Object[] pargs = argParser("RDT10", args);
		RDT30 rdt30 = new RDT30((Double)pargs[0], (Double)pargs[1], (Integer)pargs[2], (String)pargs[3]);
		rdt30.run();
	}
	
}
