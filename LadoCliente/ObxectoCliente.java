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
    public static void main(String[] args) {
        // Obtener los datos del servidor
        Scanner scan = new Scanner(System.in);
        System.out.println("Introduce la dirección IP del registro RMI para el Servidor: ");
        String stringIP = scan.next();
        InetAddress direccionIP;
        try{
            direccionIP = InetAddress.getByName(stringIP);
        }
        catch(UnknownHostException e){
            System.out.println("La dirección IP introducida (" + stringIP + ") no es válida: " + e.getMessage());
            scan.close();
            return;
        }
        
        System.out.println("Introduce el puerto del registro RMI para Servidor: ");
        Integer puerto = scan.nextInt();
        scan.nextLine();

        // Obtener el objeto remoto
        InterfaceServidor servidor = null;
        try{
            String urlRegistro = "rmi://" + direccionIP.getHostAddress() + ":" + puerto + "/servidorRemoto";
            servidor = (InterfaceServidor)Naming.lookup(urlRegistro);
        }
        catch(NotBoundException e1){
            System.out.println("Error al buscar el registro RMI: " + e1.getMessage());
            scan.close();
            return;
        }
        catch(MalformedURLException e2){
            System.out.println("Error en la creación de la dirección del registro RMI: " + e2.getMessage());
            scan.close();
            return;
        }
        catch(RemoteException e3){
            System.out.println("Error en la comunicación con el objeto servidor: " + e3.getMessage());
            scan.close();
            return;
        }

        // Hook para cerrar la conexión si se recibe una señal de terminación
        InterfaceServidor finalServidor = servidor;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (finalServidor != null) {
                try {
                    InterfaceCliente cliente = new ImplInterfaceCliente();
                    finalServidor.logOut(cliente);
                    System.out.println("\nConexión RMI cerrada correctamente.");
                } catch (RemoteException e) {
                    System.out.println("Error al cerrar la conexión RMI: " + e.getMessage());
                }
            }
        }));
        
        // Creaación de las interfaces y login
        ImplInterfaceCliente cliente = null;
        ImplInterfacePeer peer = null;

        System.out.println("Introduce tu nombre");
        String nombre = scan.nextLine().trim();

        if(servidor != null){
            try{
                cliente = new ImplInterfaceCliente();
                peer = new ImplInterfacePeer(nombre);
                // Registrar el cliente como en línea
                servidor.logIn(cliente, peer);

                // Bucle de interacción
                String input = "";
                while(!input.equals("exit")){
                    System.out.println("Escribe 'exit' para cerrar el cliente: ");
                    input = scan.next();
                }

                // Cerrar la conexión
                servidor.logOut(cliente);
            }
            catch(RemoteException remoteException){
                System.out.println("Excepción en la creción de los objetos remotos del cliente: " + remoteException.getMessage());
            }
        }
        else{
            System.out.println("Error en la conexión con el servidor");
            scan.close();
            return;
        }

        boolean exit = false;
        while(!exit){
            String comando = scan.nextLine().trim();

            switch (comando) {
                case "send":
                    System.out.println("Introduce el nombre del destinatario: ");
                    String destinatario = scan.nextLine().trim();

                    System.out.println("Introduce el mensaje: ");
                    String mensaje = scan.nextLine();

                    InterfacePeer peerDestino = cliente.find(destinatario);
                    try{
                        peerDestino.receiveMessage(mensaje);
                    }
                    catch(RemoteException exception){
                        System.out.println("Error al enviar un mensaje: " + exception.getMessage());
                    }

                    System.out.println("Se ha enviado el mensaje \"" + mensaje + "\" a " + destinatario);

                    break;
            
                default:
                    // Imprimir lista de peers con los que hablar
                    System.out.println("Los usuarios disponibles son: ");
                    try{
                        for(String usuario : cliente.getPeerNames()){
                            System.out.println(usuario);
                        }
                    }
                    catch(RemoteException exception){
                        System.out.println("Error en la obtención de los peers: " + exception.getMessage());
                    }
                    
                    break;
            }
        }

        scan.close();

        // Finalizar el cliente
        System.exit(0);
    }
}