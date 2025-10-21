package LadoCliente;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ImplInterfacePeer extends UnicastRemoteObject implements InterfacePeer {
    private String nombre;
    
    public ImplInterfacePeer(String nombre) throws RemoteException{
        super();
        this.nombre = nombre;
    }

    public void receiveMessage(String message, String nome) throws RemoteException{
        System.out.println("[" + nome + "]: " + message);
    }

    public String getName() throws RemoteException{
        return this.nombre;
    }
}
