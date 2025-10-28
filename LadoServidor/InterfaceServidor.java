package LadoServidor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

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
    public boolean logIn(InterfaceCliente usuario, InterfacePeer peer, String contrasinal) throws RemoteException;
    public boolean logOut(String usuario, String contraseña) throws RemoteException;

    // Xestionar os usuarios rexistrados
    public boolean registerUser(String usuario, String contrasinal) throws RemoteException;
    public boolean changePassword(String usuario, String contrasinalAntigo, String contrasinalNovo) throws RemoteException;
    public boolean deleteUser(String usuario, String contrasinal) throws RemoteException;

    // Xestionar amizades
    public boolean sendFriendRequest(String usuario, String contraseña, String nombreAmigo) throws RemoteException;
    public ArrayList<String> getFriendRequests(String usuario, String contraseña) throws RemoteException;
    public boolean answerFriendRequest(String usuario, String contraseña, String nombreAmigo, boolean aceptar) throws RemoteException;
}
