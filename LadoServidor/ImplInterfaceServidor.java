package LadoServidor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import LadoCliente.InterfaceCliente;
import LadoCliente.InterfacePeer;

public class ImplInterfaceServidor extends UnicastRemoteObject implements InterfaceServidor {
    private HashMap<InterfaceCliente, InterfacePeer> clientesEnLinea;
    
    public ImplInterfaceServidor() throws RemoteException {
        super();
        clientesEnLinea = new HashMap<>();
    }

    public boolean logIn(InterfaceCliente usuario, InterfacePeer peer) throws RemoteException{
        // Añadir al nuevo usuario a la lista de usuarios en línea
        clientesEnLinea.put(usuario, peer);

        System.out.println("Se ha registrado un nuevo usuario en el servidor");
        
        // Notificar a todos los usuario del nuevo registro
        for(Map.Entry<InterfaceCliente, InterfacePeer> cliente : clientesEnLinea.entrySet()){
            if(cliente.getKey() != usuario){
                cliente.getKey().addUsuarioEnLinea(peer);
            }
        }

        // Notificar al nuevo usuario de todos los usuarios actualmente en línea
        for(InterfacePeer clientePeer : clientesEnLinea.values()){
            if(clientePeer != peer){
                usuario.addUsuarioEnLinea(clientePeer);
            }
        } 

        // TODO ( o qué TODO??)
        return true;
    }
    public boolean logOut(InterfaceCliente usuario) throws RemoteException{
        // Eliminar al usuario de la lista de usuarios en línea
        InterfacePeer usuarioPeer = clientesEnLinea.remove(usuario);
        if(usuarioPeer == null){
            return false;
        }

        System.out.println("Un usuario se ha desconectado del servidor");

        // Notificar al resto de usuario de la baja
        for(InterfaceCliente cliente : clientesEnLinea.keySet()){
            cliente.removeUsuarioEnLinea(usuarioPeer.getName());
        }
        return true;
    }
}
