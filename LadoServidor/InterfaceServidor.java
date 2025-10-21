package LadoServidor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import LadoCliente.InterfaceCliente;
import LadoCliente.InterfacePeer;

public interface InterfaceServidor extends Remote {
    /* 
    // Xestión de usuarios
    boolean registerUser(String username, String passwordHash) throws RemoteException;
    boolean authenticate(String username, String passwordHash) throws RemoteException;

    // Xestión de amizades
    boolean sendFriendRequest(String fromUser, String toUser) throws RemoteException;
    List<FriendRequest> getPendingRequests(String username) throws RemoteException;
    boolean respondFriendRequest(String username, String requester, boolean accept) throws RemoteException;
     */

    // Conectar un nuevo usuario en línea
    public boolean logIn(InterfaceCliente usuario, InterfacePeer peer) throws RemoteException;
    public boolean logOut(InterfaceCliente usuario) throws RemoteException;

}
