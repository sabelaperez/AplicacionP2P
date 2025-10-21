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
        
        if(servidor != null){
            try{
                InterfaceCliente cliente = new ImplInterfaceCliente();
                InterfacePeer peer = new ImplInterfacePeer();
                // Registrar el cliente como en línea
                servidor.logIn(cliente, peer);
            }
            catch(RemoteException remoteException){
                System.out.println("Excepción en la creción de los objetos remotos del cliente: " + remoteException.getMessage());
            }
        }

        scan.close();
    }
}