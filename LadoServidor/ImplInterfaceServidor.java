package LadoServidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import LadoCliente.InterfaceCliente;
import LadoCliente.InterfacePeer;



public class ImplInterfaceServidor extends UnicastRemoteObject implements InterfaceServidor {
    private HashMap<String, Interfaces> clientesEnLinea;
    private HashMap<String, String> usuariosRegistrados;
    private HashMap<String, ArrayList<String>> amigos;
    private HashMap<String, ArrayList<String>> solicitudesAmistad;
    
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
        String nombreUsuario = peer.getName();
        Interfaces datosUsuario = new Interfaces(usuario, peer);
        clientesEnLinea.put(nombreUsuario, datosUsuario);

        System.out.println("Se ha registrado un nuevo usuario en el servidor");
        
        for(String amigo : this.amigos.get(nombreUsuario)){
            Interfaces interfacesAmigo = clientesEnLinea.get(amigo);
            if(interfacesAmigo.cliente() != usuario){
                // Notificar a todos los amigos del usuario del nuevo registro
                interfacesAmigo.cliente().addUsuarioEnLinea(peer);

                // Notificar al nuevo usuario de todos los usuarios actualmente en línea
                usuario.addUsuarioEnLinea(interfacesAmigo.peer());
            }
        } 
        return true;
    }

    public boolean logOut(String usuario, String contraseña) throws RemoteException{
        // Comprobar se o usuario está autenticado
        if(!authenticate(usuario, contraseña)){
            return false;
        }

        // Eliminar al usuario de la lista de usuarios en línea
        Interfaces usuarioEliminado = clientesEnLinea.remove(usuario);
        if(usuarioEliminado == null){
            return false;
        }

        System.out.println("Un usuario se ha desconectado del servidor");
        
        // Notificar al resto de amigos de la baja
        for(String amigo : this.amigos.get(usuario)){
            Interfaces interfacesAmigo = clientesEnLinea.get(amigo);
            interfacesAmigo.cliente().removeUsuarioEnLinea(usuario);
        }
        
        return true;
    }

    public boolean sendFriendRequest(String usuario, String contraseña, String nombreAmigo) throws RemoteException{
        // Comprobar se o usuario está autenticado
        if(!authenticate(usuario, contraseña)){
            return false;
        }

        // Añadir solicitud de amistad
        ArrayList<String> amigosExistentes = this.solicitudesAmistad.get(nombreAmigo);
        if(amigosExistentes == null){
            // Crear un ArrayList nuevo
            amigosExistentes = new ArrayList<>();
        }
        amigosExistentes.add(usuario);
        this.solicitudesAmistad.put(nombreAmigo, amigosExistentes);

        return true;
    }

    // Puede ser null, hay que comprobar
    public ArrayList<String> getFriendRequests(String usuario, String contraseña) throws RemoteException{
        // Comprobar se o usuario está autenticado
        if(!authenticate(usuario, contraseña)){
            return null;
        }

        ArrayList<String> solicitudes = this.solicitudesAmistad.get(usuario);
        if(solicitudes == null){
            solicitudes = new ArrayList<>();
        }
        return solicitudes;
    }

    public boolean answerFriendRequest(String usuario, String contraseña, String nombreAmigo, boolean aceptar) throws RemoteException{
        // Comprobar se o usuario está autenticado
        if(!authenticate(usuario, contraseña)){
            return false;
        }

        // Comprobar que existe la solicitud de amistad
        if(!this.solicitudesAmistad.get(usuario).contains(nombreAmigo)){
            return false;
        }

        // Responder a la solicitud de amistad
        if(aceptar){
            amigos.get(usuario).add(nombreAmigo);

            // Si el amigo está en línea, les mandas sus respectivos peers
            Interfaces nuevoAmigo = clientesEnLinea.get(nombreAmigo);
            Interfaces propioUsuario = clientesEnLinea.get(usuario);
            if(nuevoAmigo != null && propioUsuario != null){
                propioUsuario.cliente().addUsuarioEnLinea(nuevoAmigo.peer());
                nuevoAmigo.cliente().addUsuarioEnLinea(propioUsuario.peer());
            }
        }

        // Eliminar la solicitud de la lista de solicitudes pendientes, en todos los casos
        if(!this.solicitudesAmistad.get(usuario).remove(nombreAmigo)){
            return false;
        }

        return true;
    }

    public boolean changePassword(String usuario, String contrasinalAntigo, String contrasinalNovo) throws RemoteException {
        // Comprobar se o usuario está rexistrado e o contrasinal antigo é correcto
        if(!authenticate(usuario, contrasinalAntigo)){
            return false; // O usuario non está rexistrado ou o contrasinal antigo é incorrecto
        }

        // Cambiar o contrasinal
        usuariosRegistrados.put(usuario, contrasinalNovo);
        return true;
    }
}
