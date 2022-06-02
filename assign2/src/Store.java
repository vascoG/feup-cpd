import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        if(membership_log.exists())
            membership_log.delete();
        try {
            membership_log.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //writeToLog(node_id + "-0-"+KeyHash.getSHA256(node_id));
    }

    public void writeToLog(String arg) {
        FileWriter fw;
        try {
            //test if there are 32 events to delete the older ones
            fw = new FileWriter(membership_log);
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

        Member node=protocol.clusterMembership.findSucessor(key);
        if(node.ipAddress.equals(this.node_id)){
            try{
                Member sucessor=protocol.clusterMembership.findSucessor(KeyHash.getSHA256(this.node_id));
                Member predecessor=protocol.clusterMembership.findPredecessor(KeyHash.getSHA256(this.node_id));

                File keyFile= new File("./"+node_id+"/"+key+".txt");
                if(keyFile.exists())
                    keyFile.delete();
                try {
                    keyFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FileWriter fw =new FileWriter(keyFile);
                fw.write(value);
                fw.close();

                Socket socketSuc = new Socket("localhost", sucessor.port);
                Socket socketPre = new Socket("localhost", predecessor.port);

                OutputStream outputSuc = socketSuc.getOutputStream();
                PrintWriter writerSuc = new PrintWriter(outputSuc, true);

                OutputStream outputPre = socketPre.getOutputStream();
                PrintWriter writerPre = new PrintWriter(outputPre, true);

                Message message = new Message(this.node_id, this.store_port, key,value,MessageType.PUT);
                writerSuc.println(message.toString());
                writerPre.println(message.toString());

                socketSuc.close();
                socketPre.close();
                //mandar duas mensagens put para o antecessor e o sucessor
                //   Socket socket = new Socket("localhost", porta sucessor e antecessor);
                return "done: "+key;
            }catch(IOException e){
                e.printStackTrace();
                return "failed";
            }
        }else{
            try {
                Socket socket = new Socket("localhost", node.port);
                System.out.println("GOING TO SEND PUT");

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                //mandar put replicate
                Message message = new Message(this.node_id, this.store_port, key,value,MessageType.PUTREPLICATE);
                writer.println(message.toString());

                socket.close();
                return "done: "+key;
            }catch (UnknownHostException e) {
                e.printStackTrace();
                return "failed";
            } catch (IOException e) {
                e.printStackTrace();
                return "failed";
            }

        }
    }

    @Override
    public String get(String key) throws RemoteException {

        Member node=protocol.clusterMembership.findSucessor(key);
        if(node.ipAddress.equals(this.node_id)){
            try{
                File keyFile= new File("./"+node_id+"/"+key+".txt");
                if(keyFile.exists()){
                    String log = Files.readString(Paths.get("./"+node_id+"/"+key+".txt"));
                    return "done: " +log;
                }
                else{
                    return "failed";
                }
            }catch(IOException e){
                e.printStackTrace();
                return "failed";
            }
        }else{
            return "contact this node: " +node.ipAddress;
        }
    }

    @Override
    public String join() throws RemoteException {
        System.out.println("joining");
        if(protocol.join(this.ip_mcast_addr,this.ip_mcast_port,this.node_id,this.store_port))
            return "done";
        return "failed";
    }

    @Override
    public String leave() throws RemoteException {
        System.out.println("leaving");
        if(protocol.leave(this.ip_mcast_addr,this.ip_mcast_port,this.node_id,this.store_port))
            return "done";
        return "failed";
    }

    @Override
    public String delete(String key) throws RemoteException {
        System.out.println("deleting");
        Member node=protocol.clusterMembership.findSucessor(key);
        if(node.ipAddress.equals(this.node_id)){
            File keyFile= new File("./"+node_id+"/"+key+".txt");
            if(keyFile.exists())
               return keyFile.delete()?"done": "failed";
            else
                return "failed: this file does not exist";
        }else{
            try {
                Socket socket = new Socket("localhost", node.port);
                System.out.println("GOING TO SEND DELETE");

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                Message message = new Message(this.node_id, this.store_port, key,MessageType.DELETE);
                writer.println(message.toString());

                socket.close();
                return "done";
            }catch (UnknownHostException e) {
                e.printStackTrace();
                return "failed";
            } catch (IOException e) {
                e.printStackTrace();
                return "failed";
            }

        }
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
        ReceiverUDP receiverUDP = new ReceiverUDP(obj.getIp_mcast_addr(), obj.getIp_mcast_port(), obj.getNode_id(), obj.getStore_port(),obj.protocol);
        new Thread(receiverUDP).start();

        ReceiverTCP receiverTCP = new ReceiverTCP(obj.getStore_port(),obj.getNode_id(), obj.protocol);
        new Thread(receiverTCP).start();

    }
    //java Store 224.0.0.0 4003 172.0.0.1 8001

    @Override
    public String show() throws RemoteException {
        return protocol.clusterMembership.show();
    }

   
}
