import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ReceiverUDP implements Runnable {

    private MulticastSocket multi_cast_socket;
    private MembershipProtocol protocol  ;


    private boolean received_membership;
    Timer timer;

    private String ipAddress;
    private int port;
    private String node_id;
    private int node_port;

    class MembershipMessageTask extends TimerTask {
        public void run() {
            received_membership = false;
            timer.cancel();
        }
    }

    public ReceiverUDP(String ipAddress, int port, String node_id, int node_port, MembershipProtocol protocol)
    {
        try {
            this.multi_cast_socket = new MulticastSocket(port);
            this.multi_cast_socket.joinGroup(InetAddress.getByName(ipAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.protocol = protocol;
        this.ipAddress=ipAddress;
        this.port=port;
        this.node_id = node_id;
        this.node_port = node_port;
        this.received_membership = true;
        timer = new Timer();
        timer.schedule(new MembershipMessageTask(), 2*1000);

    }

    @Override
    public void run() {

        Thread sendMembershipThread = new Thread(new Runnable() {
            @Override
            public void run() {
               {
                   while(true)
                   {
                       try {
                        Thread.sleep((new Random().nextInt(11)+1)*10);
                        if(!received_membership)
                        {
                            String log = protocol.getMembershipLog(node_id);
                            if(log.isEmpty())
                                continue;
                            String message = new Message(ipAddress, port, log, MessageType.MEMBERSHIP).toString();
                            DatagramPacket datagram_packet = new DatagramPacket(message.getBytes(), message.length(),InetAddress.getByName(ipAddress), port);

                            System.out.println("SENT MULTICAST MESSAGE");

                            multi_cast_socket.send(datagram_packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                   }
               }
                
            }
            
        });
        sendMembershipThread.start();

        while(true){
        byte[] buf = new byte[500]; 
        DatagramPacket p = new DatagramPacket(buf, 500);
        try {
            this.multi_cast_socket.receive(p);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        String string = new String(p.getData(), StandardCharsets.ISO_8859_1);
        System.out.println(string);

        Message message = new Message(buf);
        //create thread for this
        if(message.getMessage_type()==MessageType.JOIN && !this.node_id.equals(message.getSender_id())){
            System.out.println("Going to send MEMBERSHIP MESSAGE TO " + message.getSender_id() + " " + message.getSender_port());
            protocol.updateMembershipLogOnJoinLeave(message,this.node_id);
            protocol.sendMembershipMessage(this.node_id,this.node_port,message.getSender_id(),message.getjoin_port());
            protocol.updateFilesOnJoin(message,this.node_id, this.node_port);
        }
        else if(message.getMessage_type()==MessageType.LEAVE && !this.node_id.equals(message.getSender_id())){
            System.out.println("LEAVE COUNTER:" + message.getMembership_counter());
            protocol.updateMembershipLogOnJoinLeave(message,this.node_id);
        }
        else if(message.getMessage_type()==MessageType.MEMBERSHIP){
            this.received_membership=true;
            timer=new Timer();
            timer.schedule(new MembershipMessageTask(), 5*1000);
        }

        }
    }
    
}
