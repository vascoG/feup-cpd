import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MembershipProtocol {

    private String ip_mcast_addr;
    private int ip_mcast_port;
    private String node_id;
    private int store_port;

    private File membership_log;
    private File membership_counter;

    

    public MembershipProtocol(String ip_mcast_addr,int ip_mcast_port, String node_id, int store_port,
            File membership_log, File membership_counter) {
        this.ip_mcast_addr = ip_mcast_addr;
        this.ip_mcast_port = ip_mcast_port;
        this.node_id = node_id;
        this.store_port = store_port;
        this.membership_log = membership_log;
        this.membership_counter = membership_counter;
    }



    public void join()
    {

        try {
            MulticastSocket multi_cast_socket = new MulticastSocket(ip_mcast_port);
            multi_cast_socket.joinGroup(InetAddress.getByName(ip_mcast_addr));
            //create JOIN Message, counter can be different than 0
            String msg = "Type:JOIN;IP:"+node_id+"PORT:"+store_port+";COUNTER:0";
            DatagramPacket datagram_packet = new DatagramPacket(msg.getBytes(), msg.length(),InetAddress.getByName(ip_mcast_addr), ip_mcast_port);
            multi_cast_socket.send(datagram_packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        // Atualizar chaves 
    }
}
