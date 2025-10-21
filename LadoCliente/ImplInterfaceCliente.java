package LadoCliente;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ImplInterfaceCliente extends UnicastRemoteObject implements InterfaceCliente {
    private ArrayList<InterfacePeer> peersEnLinea;

    public ImplInterfaceCliente() throws RemoteException {
        super();
        peersEnLinea = new ArrayList<>();
    }

    public boolean addUsuarioEnLinea(InterfacePeer usuario) throws RemoteException{
        System.out.println("Hay un nuevo usuario en línea");
        return peersEnLinea.add(usuario);
    }

    public boolean removeUsuarioEnLinea(InterfacePeer usuario) throws RemoteException{
        System.out.println("Un usuario se ha desconectado");
        return peersEnLinea.remove(usuario);
    }
}
