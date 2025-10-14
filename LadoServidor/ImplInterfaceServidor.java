package LadoServidor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ImplInterfaceServidor extends UnicastRemoteObject implements InterfaceServidor {
    public ImplInterfaceServidor() throws RemoteException {
        super();
    }
}
