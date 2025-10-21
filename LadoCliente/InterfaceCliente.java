package LadoCliente;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface InterfaceCliente extends Remote {

    public boolean addUsuarioEnLinea(InterfacePeer usuario) throws RemoteException;
    public boolean removeUsuarioEnLinea(String nombre) throws RemoteException;
    public Set<String> getPeerNames() throws RemoteException;
}
