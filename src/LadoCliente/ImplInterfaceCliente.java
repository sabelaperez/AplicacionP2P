package LadoCliente;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

import javafx.application.Platform;

public class ImplInterfaceCliente extends UnicastRemoteObject implements InterfaceCliente {
    private HashMap<String, InterfacePeer> peersEnLinea;
    private Consumer<String> newUserHandler;
    private Consumer<String> disconnectedUserHandler;
    private Consumer<String> friendRequestHandler;

    public ImplInterfaceCliente() throws RemoteException {
        super();
        peersEnLinea = new HashMap<>();
    }

    public void addUsuarioEnLinea(InterfacePeer usuario) throws RemoteException{
        String nombre = usuario.getName();
        System.out.println(nombre + " está en linea");
        peersEnLinea.put(usuario.getName(), usuario);

        // Si hay UI, actualizarla
        if (newUserHandler != null) {
            Platform.runLater(() -> newUserHandler.accept(nombre));
        }
    }

    public void removeUsuarioEnLinea(String nombre) throws RemoteException{
        System.out.println(nombre + " se ha desconectado");
        peersEnLinea.remove(nombre);

        // Si hay UI, actualizarla
        if (disconnectedUserHandler != null) {
            Platform.runLater(() -> disconnectedUserHandler.accept(nombre));
        }
    }

    public void notifyFriendRequest(String requesterName) throws RemoteException {
        System.out.println("Nueva solicitud de amistad de: " + requesterName);
        
        // Si hay UI, actualizarla
        if (friendRequestHandler != null) {
            Platform.runLater(() -> friendRequestHandler.accept(requesterName));
        }
    }

    public Set<String> getPeerNames(){
        return this.peersEnLinea.keySet();
    }

    public InterfacePeer find(String nombre){
        return peersEnLinea.get(nombre);
    }

    public void setUserAdditionHandler(Consumer<String> handler) {
        this.newUserHandler = handler;
    }

    public void setUserRemovalHandler(Consumer<String> handler) {
        this.disconnectedUserHandler = handler;
    }

    public void setFriendRequestHandler(Consumer<String> handler) {
        this.friendRequestHandler = handler;
    }
}
