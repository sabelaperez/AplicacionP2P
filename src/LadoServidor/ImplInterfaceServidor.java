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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import LadoCliente.InterfaceCliente;
import LadoCliente.InterfacePeer;



public class ImplInterfaceServidor extends UnicastRemoteObject implements InterfaceServidor {
    private ConcurrentHashMap<String, Interfaces> clientesEnLinea;
    private ConcurrentHashMap<String, String> usuariosRegistrados;
    private ConcurrentHashMap<String, ArrayList<String>> amigos;
    private ConcurrentHashMap<String, ArrayList<String>> solicitudesAmistad;
    
    public ImplInterfaceServidor() throws RemoteException {
        super();
        clientesEnLinea = new ConcurrentHashMap<>();
        amigos = new ConcurrentHashMap<>();
        usuariosRegistrados = cargarDatos();
        solicitudesAmistad = cargarSolicitudes();
    }

    private ConcurrentHashMap<String, String> cargarDatos() {
        ConcurrentHashMap<String, String> usuarios = new ConcurrentHashMap<>();
        amigos.clear();

        File bd = new File("LadoServidor/bd.txt");
        if (!bd.exists()) {
            bd = new File("bd.txt");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(bd))) {
            String line = br.readLine();

            // Validar cabeceira USER PASSWD
            if (line == null || !line.trim().replaceAll("\\s+", " ").equals("USER PASSWD")) {
                throw new IOException("O arquivo de base de datos non ten o formato correcto (falta USER PASSWD).");
            }

            // Ler usuarios ata atopar a cabeceira USER NUMBER (ou EOF)
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.replaceAll("\\s+", " ").equals("USER NUMBER")) {
                    // Pasamos á sección de amigos
                    break;
                }

                String[] partes = line.split("\\s+");
                if (partes.length == 2) {
                    String username = partes[0];
                    String passwordHash = partes[1];
                    usuarios.put(username, passwordHash);
                    // Inicializamos a súa lista de amigos
                    amigos.putIfAbsent(username, new ArrayList<>());
                } else {
                    System.err.println("Línea de usuarios con formato inesperado (se ignora): '" + line + "'");
                }
            }

            // Se chegamos a EOF e non hai sección de amigos, devolvemos o cargado
            if (line == null) {
                return usuarios;
            }

            // Ler amigos ata EOF
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] header = line.split("\\s+");
                if (header.length >= 1) {
                    String username = header[0];
                    // Poderíase comprobar se o usuario existe en 'usuariosRegistrados' 
                    int count = 0;
                    if (header.length >= 2) {
                        try {
                            count = Integer.parseInt(header[1]);
                        } catch (NumberFormatException e) {
                            System.err.println("Número de amigos no válido para '" + username + "', se asume 0: '" + line + "'");
                            count = 0;
                        }
                    } else {
                        System.err.println("Falta o número de amigos para '" + username + "', se asume 0: '" + line + "'");
                    }

                    ArrayList<String> lista = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        String friendLine = br.readLine();
                        if (friendLine == null) {
                            System.err.println("Faltan líneas de amigos para '" + username + "' (esperados " + count + ").");
                            break;
                        }
                        friendLine = friendLine.trim();
                        if (friendLine.isEmpty()) {
                            i--; // Non contar liñas baleiras (non debería ocorrer)
                            continue;
                        }
                        lista.add(friendLine);
                    }

                    // Gardamos a lista de amigos
                    amigos.put(username, lista);
                } else {
                    System.err.println("Línea de USER NUMBER malformada: '" + line + "'");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return usuarios;
    }

    protected boolean gardarUsuarios() {
        File bd = new File("LadoServidor/bd.txt");
        if (!bd.exists()) {
            bd = new File("bd.txt");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(bd))) {
            // Sección usuarios
            bw.write("USER PASSWD");
            bw.newLine();
            for (Map.Entry<String, String> entry : usuariosRegistrados.entrySet()) {
                bw.write(entry.getKey() + " " + entry.getValue());
                bw.newLine();
            }

            bw.newLine();

            // Sección amigos
            bw.write("USER NUMBER");
            bw.newLine();
            // Iteramos sobre os usuarios rexistrados
            Set<String> allUsers = new LinkedHashSet<>();
            allUsers.addAll(usuariosRegistrados.keySet());

            for (String user : allUsers) {
                ArrayList<String> lista = amigos.get(user);
                if (lista == null) lista = new ArrayList<>();
                bw.write(user + " " + lista.size());
                bw.newLine();
                for (String f : lista) {
                    bw.write(f);
                    bw.newLine();
                }
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ConcurrentHashMap<String, ArrayList<String>> cargarSolicitudes() {
        ConcurrentHashMap<String, ArrayList<String>> solicitudes = new ConcurrentHashMap<>();

        File solis = new File("LadoServidor/solis.txt");
        if (!solis.exists()) {
            solis = new File("solis.txt");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(solis))) {
            String line = br.readLine();

            // Validar cabeceira SOLICITUDES
            if (line == null || !line.trim().replaceAll("\\s+", " ").equals("SOLICITUDES")) {
                throw new IOException("O arquivo de solicitudes non ten o formato correcto (falta SOLICITUDES).");
            }

            // Ler solicitudes ata EOF
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] partes = line.split("\\s+");
                if (partes.length == 2) {
                    String nombreSolicitante = partes[0];
                    String nombreSolicitado = partes[1];

                    ArrayList<String> lista = solicitudes.getOrDefault(nombreSolicitado, new ArrayList<>());
                    lista.add(nombreSolicitante);
                    solicitudes.put(nombreSolicitado, lista);
                } else {
                    System.err.println("Línea de solicitudes con formato inesperado (se ignora): '" + line + "'");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return solicitudes;
    }

    protected boolean gardarSolicitudes() {
        File solis = new File("LadoServidor/solis.txt");
        if (!solis.exists()) {
            solis = new File("solis.txt");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(solis))) {
            // Sección solicitudes
            bw.write("SOLICITUDES");
            bw.newLine();
            for (Map.Entry<String, ArrayList<String>> entry : solicitudesAmistad.entrySet()) {
                String nombreSolicitado = entry.getKey();
                ArrayList<String> listaSolicitantes = entry.getValue();
                for (String nombreSolicitante : listaSolicitantes) {
                    bw.write(nombreSolicitante + " " + nombreSolicitado);
                    bw.newLine();
                }
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
        amigos.put(usuario, new ArrayList<>());
        return true;
    }

    public boolean deleteUser(String usuario, String contrasinal) throws RemoteException {
        // Eliminar un usuario rexistrado
        if(!authenticate(usuario, contrasinal)){
            return false; // O usuario non está rexistrado ou o contrasinal é incorrecto
        }

        usuariosRegistrados.remove(usuario);
        clientesEnLinea.remove(usuario);

        // Notificar al resto de amigos de la baja
        for(String amigo : this.amigos.get(usuario)){
            Interfaces interfacesAmigo = clientesEnLinea.get(amigo);
            if(interfacesAmigo != null){
                interfacesAmigo.cliente().removeUsuarioEnLinea(usuario);
            }
        }
        // Eliminar los amigos una vez notificados
        amigos.remove(usuario);

        // Eliminar al usuario eliminado de todas las amistades
        for(ArrayList<String> amigosUsuario : this.amigos.values()){
            amigosUsuario.remove(usuario);
        }

        // Considerar eliminar as solicitudes de amizade relacionadas

        return true;
    }

    public boolean authenticate(String usuario, String contrasinal) {
        // Comprobar que o usuario está rexistrado
        String user = usuariosRegistrados.get(usuario);
        return user != null && user.equals(contrasinal);
    }

    public boolean logIn(InterfaceCliente usuario, InterfacePeer peer, String contrasinal) throws RemoteException{
        // Comprobar se o usuario está autenticado
        String nombreUsuario = peer.getName();
        if(!authenticate(nombreUsuario, contrasinal)){
            return false;
        }

        // Añadir al nuevo usuario a la lista de usuarios en línea
        
        Interfaces datosUsuario = new Interfaces(usuario, peer);
        clientesEnLinea.put(nombreUsuario, datosUsuario);

        System.out.println("Se ha registrado un nuevo usuario en el servidor");
        
        for(String amigo : this.amigos.get(nombreUsuario)){
            Interfaces interfacesAmigo = clientesEnLinea.get(amigo);
            // Si no está en línea, puede ser null!
            if(interfacesAmigo != null && interfacesAmigo.cliente() != usuario){
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
            if(interfacesAmigo != null){
                interfacesAmigo.cliente().removeUsuarioEnLinea(usuario);
            }
        }
        
        return true;
    }

    public boolean sendFriendRequest(String usuario, String contraseña, String nombreAmigo) throws RemoteException{
        // Comprobar se o usuario está autenticado
        if(!authenticate(usuario, contraseña)){
            return false;
        }

        // Comprobar que el amigo deseado existe en la BD
        if(this.usuariosRegistrados.get(nombreAmigo) == null){
            return false;
        }

        // Comprobar que la persona no es ya tu amiga
        if(this.amigos.get(usuario).contains(nombreAmigo)){
            return false;
        }

        // Comprobar que no hay ya una solicitud emitida ya
        ArrayList<String> solicitudesExistentes = this.solicitudesAmistad.get(nombreAmigo);
        // Se non existe ningunha solicitud previa, inicializar a lista
        if (solicitudesExistentes == null) {
            solicitudesExistentes = new ArrayList<>();
        }   
        if(solicitudesExistentes.contains(usuario)){
            return false;
        }

        // Añadir solicitud de amistad
        solicitudesExistentes.add(usuario);
        this.solicitudesAmistad.put(nombreAmigo, solicitudesExistentes);

        // Notificar al usuario si está en línea
        Interfaces interfazAmigo = this.clientesEnLinea.get(nombreAmigo);
        if(interfazAmigo != null){
            try {
                interfazAmigo.cliente().notifyFriendRequest(usuario);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

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
        if(this.solicitudesAmistad.get(usuario) == null){
            return false;
        }
        if(!solicitudesAmistad.get(usuario).contains(nombreAmigo)){
            return false;
        }

        // Responder a la solicitud de amistad
        if(aceptar){
            amigos.get(usuario).add(nombreAmigo);
            amigos.get(nombreAmigo).add(usuario);

            // Si el amigo está en línea, les mandas sus respectivos peers
            Interfaces nuevoAmigo = clientesEnLinea.get(nombreAmigo);
            Interfaces propioUsuario = clientesEnLinea.get(usuario);
            if(nuevoAmigo != null && propioUsuario != null){
                propioUsuario.cliente().addUsuarioEnLinea(nuevoAmigo.peer());
                nuevoAmigo.cliente().addUsuarioEnLinea(propioUsuario.peer());
            }

            // Como la amistad es recíproca, si hay invitación en otro sentido se puede eliminar ya
            if(solicitudesAmistad.get(nombreAmigo) != null){
                this.solicitudesAmistad.get(nombreAmigo).remove(usuario);
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
