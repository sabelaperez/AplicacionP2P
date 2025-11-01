package LadoCliente;

import javafx.application.Platform;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.BiConsumer;

public class ImplInterfacePeer extends UnicastRemoteObject implements InterfacePeer {
    private String nombre;
    private BiConsumer<String, String> messageHandler;
    
    public ImplInterfacePeer(String nombre) throws RemoteException{
        super();
        this.nombre = nombre;
    }

    public void setMessageHandler(BiConsumer<String, String> handler) {
        this.messageHandler = handler;
    }

    public void receiveMessage(String message, String nome) throws RemoteException{
        System.out.println("[" + nome + "]: " + message);
        
        // If UI is available, update it
        if (messageHandler != null) {
            Platform.runLater(() -> messageHandler.accept(message, nome));
        }
    }

    public String getName() throws RemoteException{
        return this.nombre;
    }
}