package LadoServidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import LadoCliente.InterfaceCliente;
import LadoCliente.InterfacePeer;

public class ImplInterfaceServidor extends UnicastRemoteObject implements InterfaceServidor {
    private HashMap<InterfaceCliente, InterfacePeer> clientesEnLinea;
    private HashMap<String, String> usuariosRegistrados; 
    
    public ImplInterfaceServidor() throws RemoteException {
        super();
        clientesEnLinea = new HashMap<>();
        usuariosRegistrados = new HashMap<>();
        usuariosRegistrados = cargarUsuarios();
    }

    private HashMap<String, String> cargarUsuarios() {
        // Cargar usuarios dende o arquivo
        HashMap<String, String> usuarios = new HashMap<>();

        // Localizar o arquivo de base de datos
        File bd = new File("LadoServidor/bd.txt");
        if(!bd.exists()) {
            bd = new File("bd.txt");
        }

        // Ler o arquivo
        try (BufferedReader br = new BufferedReader(new FileReader(bd))) {
            String line = br.readLine();

            // Procesar a cabeceira
            if(!line.trim().replaceAll("\\s+", " ").equals("USER PASSWD")) {
                throw new IOException("O arquivo de base de datos non ten o formato correcto.");
            } else {
                // Ler os usuarios
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue; // saltar líneas vacías

                    String[] partes = line.split("\\s+");
                    if (partes.length == 2) {
                        String username = partes[0];
                        String passwordHash = partes[1];
                        usuarios.put(username, passwordHash);
                    } else {
                        System.err.println("Línea con formato inesperado (se ignora): '" + line + "'");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return usuarios;
    }

    protected boolean gardarUsuarios() {
        // Gardar os usuarios no arquivo
        File bd = new File("LadoServidor/bd.txt");
        if(!bd.exists()) {
            bd = new File("bd.txt");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(bd))) {
            bw.write("USER PASSWD");
            bw.newLine();
            for (Map.Entry<String, String> entry : usuariosRegistrados.entrySet()) {
                bw.write(entry.getKey() + " " + entry.getValue());
                bw.newLine();
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean registerUser(String usuario, String contrasinal) {
        // Comprobar se o usuario xa existe
        if(usuariosRegistrados.containsKey(usuario)){
            return false; 
        }

        // Rexistrar un novo usuario
        usuariosRegistrados.put(usuario, contrasinal);
        return true;
    }

    public boolean deleteUser(String usuario, String contrasinal) {
        // Eliminar un usuario rexistrado
        if(!authenticate(usuario, contrasinal)){
            return false; // O usuario non está rexistrado ou o contrasinal é incorrecto
        }

        usuariosRegistrados.remove(usuario);
        return true;
    }

    public boolean authenticate(String usuario, String contrasinal) {
        // Comprobar que o usuario está rexistrado
        String user = usuariosRegistrados.get(usuario);
        return user != null && user.equals(contrasinal);
    }

    public boolean logIn(InterfaceCliente usuario, InterfacePeer peer, String contrasinal) throws RemoteException{
        // Comprobar se o usuario está autenticado
        if(!authenticate(peer.getName(), contrasinal)){
            return false;
        }

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
