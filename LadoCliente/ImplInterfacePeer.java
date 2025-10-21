package LadoCliente;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ImplInterfacePeer extends UnicastRemoteObject implements InterfacePeer {
    
    public ImplInterfacePeer() throws RemoteException{
        super();
    }

    public void receiveMessage(String message) throws RemoteException{
        System.out.println("El mensaje recibido es: " + message);
    }
}
