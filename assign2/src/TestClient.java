import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestClient{
    public static void main(String args[]) {

        if(args.length<2) {
            System.err.println("Client exception: Wrong number of arguments!");
            return;
        }
        String node_ap = args[0];
        String operation = args[1];
        String opnd="";
        if(operation.equals("put") || operation.equals("delete")||operation.equals("get"))
        {
            if(args.length<3)
            {
                System.err.println("Client exception: Wrong number of arguments!");
                return;
            }
            else
                opnd = args[2];
        }
           
        String[] node_ap_array = node_ap.split(":");


        try {
            Registry registry = LocateRegistry.getRegistry(node_ap_array[0]);
            RMIServer stub = (RMIServer) registry.lookup(node_ap_array[1]);
            String response;
            switch(operation)
        {
            case "join": response = stub.join(); break;
            case "leave": response = stub.leave(); break;
            case "show": response = stub.show();break;
            case "put":
                String value = Files.readString(Paths.get(opnd));
                String key=KeyHash.getSHA256(value);
                response = stub.put(key,value);break;
            case "delete":
                response=stub.delete(opnd); break;
            case "get":
                response=stub.get(opnd); break;
            default: response = "ERROR ON OPERATION ARGUMENT";
        }
           System.out.println("response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    


    } //java TestClient localhost:172.0.0.1 join
}