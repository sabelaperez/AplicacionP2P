package LadoCliente;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceCliente extends Remote {

    public void addUsuarioEnLinea(InterfacePeer usuario) throws RemoteException;
    public void removeUsuarioEnLinea(String nombre) throws RemoteException;
    public void notifyFriendRequest(String requesterName) throws RemoteException;
}
