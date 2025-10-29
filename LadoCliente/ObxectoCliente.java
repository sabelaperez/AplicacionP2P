package LadoCliente;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;

import LadoServidor.InterfaceServidor;

public class ObxectoCliente {

    // Variables compartidas (campos estáticos) 
    private static InterfaceServidor servidor = null;
    private static ImplInterfaceCliente cliente = null;
    private static ImplInterfacePeer peer = null;
    
    private static String nombre = "";
    private static String contrasinal = "";

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        try {
            // Obtener los datos del servidor
            System.out.println("Introduce la dirección IP del registro RMI para el Servidor: ");
            String stringIP = scan.next();
            InetAddress direccionIP;
            try {
                direccionIP = InetAddress.getByName(stringIP);
            } catch (UnknownHostException e) {
                System.out.println("La dirección IP introducida (" + stringIP + ") no es válida: " + e.getMessage());
                return;
            }

            System.out.println("Introduce el puerto del registro RMI para Servidor: ");
            Integer puerto = scan.nextInt();
            scan.nextLine(); // consumir el salto de línea

            // Obtener el objeto remoto
            try {
                String urlRegistro = "rmi://" + direccionIP.getHostAddress() + ":" + puerto + "/servidorRemoto";
                servidor = (InterfaceServidor) Naming.lookup(urlRegistro);
            } catch (NotBoundException e1) {
                System.out.println("Error al buscar el registro RMI: " + e1.getMessage());
                return;
            } catch (MalformedURLException e2) {
                System.out.println("Error en la creación de la dirección del registro RMI: " + e2.getMessage());
                return;
            } catch (RemoteException e3) {
                System.out.println("Error en la comunicación con el objeto servidor: " + e3.getMessage());
                return;
            }

            // Entrar no sistema
            boolean logged = false;
            boolean registered = false;

            do {
                System.out.println("\nDesea registrarse como nuevo usuario o iniciar sesión? (r/i)");
                String respuesta = scan.nextLine().trim().toLowerCase();

                if(!respuesta.equals("r") && !respuesta.equals("i")) {
                    System.out.println("Respuesta no válida");
                    continue;
                }

                // Pedir datos de usuario
                System.out.println("Introduce tu nombre");
                nombre = scan.nextLine().trim();
                System.out.println("Introduce tu contraseña");
                contrasinal = scan.nextLine().trim();

                try {
                    // Crear las implementaciones del cliente y registrarse en el servidor
                    cliente = new ImplInterfaceCliente();
                    peer = new ImplInterfacePeer(nombre);

                    // Realizar a acción correspondente
                    if(respuesta.equals("r")) {
                        // Rexistrarse como novo usuario
                        registered = servidor.registerUser(nombre, contrasinal);
                            if(registered){
                                System.out.println("Usuario registrado correctamente.");
                                servidor.logIn(cliente, peer, contrasinal);
                            } else {
                                System.out.println("No se ha podido registrar el usuario. El nombre de usuario ya existe.");
                            }
                    } else if(respuesta.equals("i")){
                        // Iniciar sesión
                        logged = servidor.logIn(cliente, peer, contrasinal); 
                        if(!logged){
                            System.out.println("No se ha podido iniciar sesión. Nombre de usuario o contraseña incorrectos.");
                        }
                    } else {
                        System.out.println("Respuesta no válida");
                    }
                } catch (RemoteException e) {
                    System.out.println("Excepción en el inicio de sesión: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } while (logged == false && registered == false);

            System.out.println("\nInicio de sesión correcto. Bienvenid@ " + nombre + "!");
            printMenu();

            // Hook para cerrar la conexión si se recibe una señal de terminación
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (servidor != null && cliente != null) {
                    try {
                        servidor.logOut(nombre, contrasinal);
                        System.out.println("\nConexión RMI cerrada correctamente.");
                    } catch (RemoteException e) {
                        System.out.println("Error al cerrar la conexión RMI: " + e.getMessage());
                    }
                }
            }));

            // Bucle principal de comandos
            boolean exit = false;
            while (!exit) {
                String comando = scan.nextLine().trim();

                switch (comando) {
                    case "users":
                        // Imprimir lista de peers con los que hablar
                        try {
                            if(!cliente.getPeerNames().isEmpty()) {
                                System.out.println("Los usuarios disponibles son: ");
                                for (String usuario : cliente.getPeerNames()) {
                                    System.out.println(usuario);
                                }
                            } else {
                                System.out.println("No hay otros usuarios en línea.");
                            }
                        } catch (RemoteException exception) {
                            System.out.println("Error en la obtención de los peers: " + exception.getMessage());
                        } catch (NullPointerException npe) {
                            System.out.println("Error: cliente no inicializado correctamente.");
                        }
                        break;

                    case "send":
                        System.out.println("Introduce el nombre del destinatario: ");
                        String destinatario = scan.nextLine().trim();

                        System.out.println("Introduce el mensaje: ");
                        String mensaje = scan.nextLine();

                        try {
                            InterfacePeer peerDestino = cliente.find(destinatario);
                            if (peerDestino != null) {
                                peerDestino.receiveMessage(mensaje, nombre);
                                System.out.println("Se ha enviado el mensaje \"" + mensaje + "\" a " + destinatario);
                            } else {
                                System.out.println("No se encontró al usuario " + destinatario);
                            }
                        } catch (RemoteException exception) {
                            System.out.println("Error al enviar un mensaje: " + exception.getMessage());
                        } catch (NullPointerException npe) {
                            System.out.println("Error: cliente no inicializado correctamente.");
                        }

                        break;

                    case "passwd":
                        System.out.println("Introduce la nueva contraseña: ");
                        String nuevaContrasinal = scan.nextLine().trim();

                        try {
                            boolean changed = servidor.changePassword(nombre, contrasinal, nuevaContrasinal);
                            if(changed) {
                                System.out.println("Contraseña cambiada correctamente.");
                                contrasinal = nuevaContrasinal; // Actualizar la contraseña localmente
                            } else {
                                System.out.println("No se ha podido cambiar la contraseña.");
                            }
                        } catch (RemoteException exception) {
                            System.out.println("Error al cambiar la contraseña: " + exception.getMessage());
                        }
                        break;
                    case "delete":
                        // Elimina o usuario da base de datos do servidor
                        try {
                            boolean deleted = servidor.deleteUser(nombre, contrasinal);
                            if(deleted) {
                                System.out.println("Usuario eliminado correctamente.");
                                exit = true;
                            } else {
                                System.out.println("No se ha podido eliminar el usuario. Nombre de usuario o contraseña incorrectos.");
                            }
                        } catch (RemoteException exception) {
                            System.out.println("Error al eliminar el usuario: " + exception.getMessage());
                        }
                        break;

                    case "exit":
                        // Desconexión del cliente
                        try {
                            if (servidor != null && cliente != null) {
                                servidor.logOut(nombre, contrasinal);
                            }
                        } catch (RemoteException exception) {
                            System.out.println("Error al desconectar el cliente: " + exception.getMessage());
                        }
                        exit = true;
                        break;

                    case "request":
                        // Enviar una solicitud de amistad
                        System.out.println("Introduce el nombre del destinatario: ");
                        String amigoSolicitado = scan.nextLine().trim();
                        
                        try{
                            if(!servidor.sendFriendRequest(nombre, contrasinal, amigoSolicitado)){
                                System.out.println("No se ha podido enviar la solicitud de amistad. Nombre de usuario/contraseña incorrectos, el usuario no existe, ya se ha enviado una invitación o ya es tu amigo");
                            }
                        }
                        catch(RemoteException exception){
                            System.out.println("Error al enviar la solicitud de amistad: " + exception.getMessage());
                        }
                        break;

                    case "invites":
                        // Obtener las solicitudes de amistad pendientes
                        try{
                            ArrayList<String> invitaciones = servidor.getFriendRequests(nombre, contrasinal);
                            
                            // Imprimir las invitaciones
                            if(invitaciones.size() > 0){
                                System.out.println("Tienes las siguientes solicitudes de amistad: ");
                                for(String i : invitaciones){
                                    System.out.println("-> " + i);
                                }
                            }
                            else{
                                System.out.println("No tienes ninguna solicitud de amistad pendiente");
                            }
                        }
                        catch(RemoteException exception){
                            System.out.println("Error al obtener las solicitudes de amistad: " + exception.getMessage());
                        }
                        break;

                    case "answer":
                        // Responder a una solicitud de amistad pendientes
                        System.out.println("Introduce el nombre de la invitación a la que quieres responder: ");
                        String amigoRespondido = scan.nextLine().trim();

                        String respuesta = "";
                        do{
                            System.out.println("Introduce tu respuesta (Aceptar/Rechazar): ");
                            respuesta = scan.nextLine().trim().toLowerCase();
                        }while (!respuesta.equals("aceptar") && !respuesta.equals("rechazar") && !respuesta.equals("a") && !respuesta.equals("r"));
                        
                        boolean respuestaBool = (respuesta.equals("aceptar") || respuesta.equals("a")) ? true : false;

                        try{
                            if(servidor.answerFriendRequest(nombre, contrasinal, amigoRespondido, respuestaBool)){
                                System.out.println("Se ha respondido a la solicitud satisfactoriamente!");
                            }
                            else{
                                System.out.println("No se ha podido responder a la solicitud. Nombre de usuario/contraseña incorrecta o la solicitud no existía");
                            }
                        }
                        catch(RemoteException exception){
                            System.out.println("Error al responder a la solicitud de amistad: " + exception.getMessage());
                        }
                        break;

                    default:
                        System.out.println("\nComando no reconocido");
                        printMenu();
                        break;
                }
            }

        } finally {
            // Cerrar el scanner y finalizar
            scan.close();
            System.exit(0);
        }
    }

    static private void printMenu() {
        System.out.println("Comandos disponibles: ");
        System.out.println("users - Muestra la lista de usuarios en línea");
        System.out.println("send - Enviar un mensaje a un usuario");
        System.out.println("passwd - Cambiar la contraseña del usuario");
        System.out.println("delete - Eliminar el usuario registrado");
        System.out.println("exit - Desconectar y salir del programa");
        System.out.println("request - Envía una solicitud de amistad a otro usuario");
        System.out.println("invites - Muestra la lista de solicitudes de amistad pendientes");
        System.out.println("answer - Responde a una solicitud de amistad");
    }
}
