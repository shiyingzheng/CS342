package TCP;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Vector;

import javax.swing.Timer;

public class SlidingWindow extends RTDBase {
    public final static int MAXSEQ = 10000, N = 5;
    private int timeout;
    public SlidingWindow(double pmunge, double ploss, int timeout, String filename) throws IOException {
        super(pmunge, ploss, filename, 1000);
        this.timeout = timeout;
        sender = new RSenderSW();
        receiver = new RReceiverSW();
    }

    public static class Packet implements PacketType{
        public final static int DIGITS = (int)Math.log10(MAXSEQ);
        public final static String fmt = String.format("%%0%dd", DIGITS);
        public String checksum;
        public String data;
        public boolean corrupt = false;
        public int seqnum;

        public Packet(String data){
            this(data, 0);
        }
        public Packet(String data, int seqnum){
            this(data, seqnum, CkSum.genCheck(pack(seqnum, data)));
        }
        public Packet(String data, int seqnum, String checksum) {
            this.data = data;
            this.seqnum = seqnum;
            this.checksum = checksum;
        }
        public static Packet deserialize(String data) {
            String hex = data.substring(0, 4);
            String dat = data.substring(4+DIGITS);
            try {
                int seqnum = Integer.parseInt(data.substring(4, 4+DIGITS));
                return new Packet(dat, seqnum, hex);
            } catch (NumberFormatException ex) {
                Packet packet = new Packet(dat, 0, hex);
                packet.corrupt = true;
                return packet;
            }
        }
        @Override
        public String serialize() {
            return checksum+pack(seqnum, data);
        }
        @Override
        public boolean isCorrupt() {
            return corrupt || !CkSum.checkString(pack(seqnum, data), checksum);
        }
        @Override
        public String toString() {
            return String.format("%s "+fmt+" (%s/%s)", data, seqnum, checksum, CkSum.genCheck(pack(seqnum,data)));
        }
        private static String pack(int seqnum, String data) {
            return String.format(fmt, seqnum)+data;
        }
    }

    public class Window {

        Packet[] packets;
        int base;
        int nextSeqnum;
        int nextIndex;
        int baseIndex;

        public Window(int size) {
            packets = new Packet[size];
            base = 0;
            nextSeqnum = 0
            baseIndex = 0;
            nextIndex = 0;
        }

        synchronized public void add(Packet packet) {
            packets[nextIndex] = packet;
            nextIndex = (nextIndex + 1) % packets.length;
            nextSeqnum++;
        }

        synchronized public void rebase(int newBase) {
            if (newBase <= base) { // trying to rebase to a smaller seqnum
                return;
            }
            baseIndex = (baseIndex + (newBase - base)) % packets.length;
            base = newBase;
        }

        synchronized public int getBase() {
            return base;
        }

        synchronized public Packet[] getInWindow(int top) {
            int diff = top - base;
            Packet[] newPackets = new Packet[diff];
            for (int i = 0; i < diff; i++) {
                newPackets[i] = packets[(base + i) % packets.length];
            }
            return newPackets;
        }

        synchronized public boolean isFull() {
            return (nextSeqnum - base) >= packets.length;
        }
    }

    public class RSenderSW extends RSender {
        Packet packet = null;
        Window window = new Window(N);
        int nextSeqnum = 1;
        Vector<String> hold = new Vector<String>();
        Timer timer = new Timer(timeout, new TimerAction());

        public RSenderSW() {
            super();
            timer.setRepeats(false);
        }

        public String getFromApp(int n) throws IOException {
            if (hold.size() == 0) return super.getFromApp(n);
            try{Thread.sleep(1000);}catch(InterruptedException ex){}
            String ans = hold.remove(0);
            return ans;
        }

        public void refuseInput(String s) {
            hold.add(s);
        }

        public Thread getInputThread() {
            return new Thread(new Runnable(){
                public void run() {
                    try {
                        for (;;) loop1();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        };

        // 	Timeout Action
        public class TimerAction implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                //		Your code here
            }
        }

        // Input Thread
        public void loop1() throws IOException {
            //		Your code here
        }

        // Acknowledgment Thread
        @Override
        public int loop(int myState) throws IOException {
            //		Your code here
            return myState;
        }
    }

    public class RReceiverSW extends RReceiver {
        int expectedSeqnum;
        @Override
        public int loop(int myState) throws IOException {
            String dat = forward.receive();
            Packet packet = Packet.deserialize(dat);
            System.out.printf("         **Receiver: %s ***\n", packet.toString());
            if (packet.isCorrupt()){
                System.out.printf("         **Receiver: corrupt data; replying ACK %04d **\n", expectedSeqnum-1);
                Packet p = new Packet("ACK", expectedSeqnum - 1);
                backward.send(p);
                return myState;
            }
            if (packet.seqnum < expectedSeqnum) {
                System.out.printf("         **Receiver: Duplicate %d packet; discarding; replying ACK %04d **\n", packet.seqnum, expectedSeqnum - 1);
                Packet p = new Packet("ACK", expectedSeqnum - 1);
                backward.send(p);
                return myState;
            }
            if (packet.seqnum > expectedSeqnum) {
                System.out.printf("         **Receiver: %d packet received out of sequence; discarding; replying ACK %04d **\n", packet.seqnum, expectedSeqnum - 1);
                Packet p = new Packet("ACK", expectedSeqnum - 1);
                backward.send(p);
                return myState;
            }
            System.out.printf("         **Receiver: ok %d data; replying ACK %04d **\n", packet.seqnum, packet.seqnum);
            deliverToApp(packet.data);
            p = new Packet("ACK", packet.seqnum);
            backward.send(p);
            expectedSeqnum = packet.seqnum + 1;
            return myState;
        }
    }
    @Override
    public void run() {
        super.run();
        ((SlidingWindow.RSenderSW)sender).getInputThread().start();
    }

    public static void main(String[] args) throws IOException {
        Object[] pargs = argParser("SlidingWindow", args);
        SlidingWindow rdt30 = new SlidingWindow((Double)pargs[0], (Double)pargs[1], (Integer)pargs[2], (String)pargs[3]);
        rdt30.run();
    }

}
