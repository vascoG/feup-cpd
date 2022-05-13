import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServer extends Remote {
    String put(String key, String value) throws RemoteException;
    String join() throws RemoteException;
}