package LadoCliente;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.application.Platform;

public class ImplInterfaceCliente extends UnicastRemoteObject implements InterfaceCliente {
    private HashMap<String, InterfacePeer> peersEnLinea;
    private BiConsumer<String, InterfacePeer> newUserHandler;
    private Consumer<String> disconnectedUserHandler;

    public ImplInterfaceCliente() throws RemoteException {
        super();
        peersEnLinea = new HashMap<>();
    }

    public boolean addUsuarioEnLinea(InterfacePeer usuario) throws RemoteException{
        String nombre = usuario.getName();
        System.out.println(nombre + " está en linea");
        peersEnLinea.put(usuario.getName(), usuario);

        // If UI is available, update it
        if (newUserHandler != null) {
            Platform.runLater(() -> newUserHandler.accept(nombre, usuario));
        }

        // TODO
        return true;
    }

    public boolean removeUsuarioEnLinea(String nombre) throws RemoteException{
        System.out.println(nombre + " se ha desconectado");
        peersEnLinea.remove(nombre);

        // If UI is available, update it
        if (disconnectedUserHandler != null) {
            Platform.runLater(() -> disconnectedUserHandler.accept(nombre));
        }

        // TODO
        return true;
    }

    public Set<String> getPeerNames() throws RemoteException{
        return this.peersEnLinea.keySet();
    }

    public InterfacePeer find(String nombre){
        return peersEnLinea.get(nombre);
    }

    public void setUserAdditionHandler(BiConsumer<String, InterfacePeer> handler) {
        this.newUserHandler = handler;
    }

    public void setUserRemovalHandler(Consumer<String> handler) {
        this.disconnectedUserHandler = handler;
    }
}
