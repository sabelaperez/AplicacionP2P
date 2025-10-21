package LadoCliente;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfacePeer extends Remote {
    public void receiveMessage(String message) throws RemoteException;
    public String getName() throws RemoteException;
}
