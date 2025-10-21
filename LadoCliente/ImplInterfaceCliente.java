package LadoCliente;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Set;

public class ImplInterfaceCliente extends UnicastRemoteObject implements InterfaceCliente {
    private HashMap<String, InterfacePeer> peersEnLinea;

    public ImplInterfaceCliente() throws RemoteException {
        super();
        peersEnLinea = new HashMap<>();
    }

    public boolean addUsuarioEnLinea(InterfacePeer usuario) throws RemoteException{
        System.out.println(usuario.getName() + " está en linea");
        peersEnLinea.put(usuario.getName(), usuario);

        // TODO
        return true;
    }

    public boolean removeUsuarioEnLinea(String nombre) throws RemoteException{
        System.out.println(nombre + " se ha desconectado");
        peersEnLinea.remove(nombre);

        // TODO
        return true;
    }

    public Set<String> getPeerNames() throws RemoteException{
        return this.peersEnLinea.keySet();
    }

    public InterfacePeer find(String nombre){
        return peersEnLinea.get(nombre);
    }
}
