import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Store implements RMIServer{

    private String ip_mcast_addr;
    private String ip_mcast_port;
    private int node_id;
    private int store_port;

    private File membership_log;
    private File membership_counter;

    public Store(String ip_mcast_addr, String ip_mcast_port, String node_id, String store_port) {
        this.ip_mcast_addr = ip_mcast_addr;
        this.ip_mcast_port = ip_mcast_port;
        this.node_id = Integer.parseInt(node_id);
        this.store_port = Integer.parseInt(store_port);

        membership_log = new File("membership_log");
        membership_counter = new File("membership_counter");
        try {
            if(membership_counter.createNewFile())
                writeToCounter(0);
            if(membership_log.createNewFile())
                writeToLog(node_id + "-0");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToLog(String arg) {
        FileWriter fw;
        try {
            //test if there are 32 events to delete the older ones
            fw = new FileWriter(membership_log,true);
            fw.write(arg);
            fw.write(System.getProperty("line.separator"));
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToCounter(int arg)
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

    public String getIp_mcast_port() {
        return ip_mcast_port;
    }

    public void setIp_mcast_port(String ip_mcast_port) {
        this.ip_mcast_port = ip_mcast_port;
    }

    public int getNode_id() {
        return node_id;
    }

    public void setNode_id(int node_id) {
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
        // TODO Auto-generated method stub
        return null;
    }





    public static void main(String args[]) {

        //args
        if(args.length!=4)
            System.err.println("Store exception: Wrong number of arguments!");
        try {

            Store obj = new Store(args[0],args[1],args[2],args[3]);
            RMIServer stub = (RMIServer) UnicastRemoteObject.exportObject(obj, obj.getStore_port());

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Node", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
}
