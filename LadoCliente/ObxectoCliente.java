package LadoCliente;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

import LadoServidor.InterfaceServidor;

public class ObxectoCliente {

    // Variables compartidas (campos estáticos) para poder usarlas en el shutdown hook
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
                        System.out.println("Los usuarios disponibles son: ");
                        try {
                            for (String usuario : cliente.getPeerNames()) {
                                System.out.println(usuario);
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

                    case "delete":
                        // Elimina o usuario da base de datos do servidor
                        try {
                            boolean deleted = servidor.deleteUser(nombre, contrasinal);
                            if(deleted) {
                                System.out.println("Usuario eliminado correctamente.");
                                // Desconectar do servidor
                                servidor.logOut(nombre, contrasinal);
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

                    default:
                        System.out.println("Comando no reconocido");
                        System.out.println("Comandos disponibles: ");
                        System.out.println("users - Muestra la lista de usuarios en línea");
                        System.out.println("send - Enviar un mensaje a un usuario");
                        System.out.println("delete - Eliminar el usuario registrado");
                        System.out.println("exit - Desconectar y salir del programa");
                }
            }

        } finally {
            // Cerrar el scanner y finalizar
            scan.close();
            System.exit(0);
        }
    }
}
