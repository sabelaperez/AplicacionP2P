package LadoCliente;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceCliente extends Remote {

    public boolean addUsuarioEnLinea(InterfacePeer usuario) throws RemoteException;
    public boolean removeUsuarioEnLinea(InterfacePeer usuario) throws RemoteException;
}
