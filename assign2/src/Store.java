import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Store implements RMIServer{

    private String ip_mcast_addr;
    private int ip_mcast_port;
    private String node_id;
    private int store_port;

    private File membership_log;
    private File membership_counter;

    private MembershipProtocol protocol;

    public Store(String ip_mcast_addr, String ip_mcast_port, String node_id, String store_port) {
        this.ip_mcast_addr = ip_mcast_addr;
        this.ip_mcast_port = Integer.parseInt(ip_mcast_port);
        this.node_id = node_id;
        this.store_port = Integer.parseInt(store_port);

        this.protocol = new MembershipProtocol();

        String parent_dir = "./"+node_id+"/";

        File directory = new File(parent_dir);
        if(!directory.exists())
            directory.mkdirs();

        membership_log = new File(parent_dir+"membership_log.txt");
        membership_counter = new File(parent_dir+"membership_counter.txt");
        writeToCounter("0");
        writeToLog(node_id + "-0-"+KeyHash.getSHA256(node_id));
    }

    public void writeToLog(String arg) {
        FileWriter fw;
        try {
            //test if there are 32 events to delete the older ones
            fw = new FileWriter(membership_log);
            fw.write(arg);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToCounter(String arg)
    {
        FileWriter fw;
        try {
            fw = new FileWriter(membership_counter);
            fw.write(arg);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public String getIp_mcast_addr() {
        return ip_mcast_addr;
    }

    public void setIp_mcast_addr(String ip_mcast_addr) {
        this.ip_mcast_addr = ip_mcast_addr;
    }

    public int getIp_mcast_port() {
        return ip_mcast_port;
    }

    public void setIp_mcast_port(int ip_mcast_port) {
        this.ip_mcast_port = ip_mcast_port;
    }

    public String getNode_id() {
        return node_id;
    }

    public void setNode_id(String node_id) {
        this.node_id = node_id;
    }

    public int getStore_port() {
        return store_port;
    }

    public void setStore_port(int store_port) {
        this.store_port = store_port;
    }

    @Override
    public String put(String key, String value) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String join() throws RemoteException {
        System.out.println("joining");
        protocol.join(this.ip_mcast_addr,this.ip_mcast_port,this.node_id,this.store_port);
        return null;
    }





    public static void main(String args[]) {

        //args
        if(args.length!=4) {
            System.err.println("Store exception: Wrong number of arguments!");
            return;
        }
        Store obj = new Store(args[0],args[1],args[2],args[3]);
        
        try {

            RMIServer stub = (RMIServer) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(obj.getNode_id(), stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }


        //criar threads de escuta para multicast(so dar start apos o join)
        ReceiverThread receiver_thread = new ReceiverThread(obj.getIp_mcast_addr(), obj.getIp_mcast_port(), obj.getNode_id(), obj.getStore_port());
        new Thread(receiver_thread).start();


        


    }
    //java Store 224.0.0.0 4003 172.0.0.1 8000
}
