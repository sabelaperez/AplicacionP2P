package LadoServidor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

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
}
