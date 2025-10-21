package LadoCliente;

import java.lang.reflect.Array;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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
        return peersEnLinea.remove(usuario);
    }
}
