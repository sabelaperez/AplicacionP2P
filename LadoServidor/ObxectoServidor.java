package LadoServidor;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ObxectoServidor {
    public static void main(String[] args) {
        try {
            // Obter o porto
            int porto = -1;
            while (porto <= 0) {
                System.out.println("Inserte o porto:");
                String entrada = System.console().readLine();
                try {
                    porto = Integer.parseInt(entrada);
                    if (porto <= 0) {
                        System.out.println("O porto debe ser un número maior que 0.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Entrada non válida. Introduza un número enteiro.");
                }
            }
            ImplInterfaceServidor obxectoExportado = new ImplInterfaceServidor();

            // Rexistrar o obxecto no rexistro RMI
            rexistrarObxecto(porto);
            String URLRexistro = "rmi://localhost:" + porto + "/servidorRemoto";
            Naming.rebind(URLRexistro, obxectoExportado);
            System.out.println("Obxecto rexistrado en " + URLRexistro);

        } catch (Exception e) {
            System.out.println("Ocorreu un erro: " + e);
        }
    }

    private static void rexistrarObxecto(int porto) throws RemoteException {
        try {
            // Comprobar se hai un rexistro RMI no porto indicado
            Registry rexistro = LocateRegistry.getRegistry(porto);
            rexistro.list();

        } catch (RemoteException e) {
            // Se non hai un rexistro RMI, créase un novo
            System.out.println("Non se atopa un rexistro RMI no porto " + porto);
            LocateRegistry.createRegistry(porto);
            System.out.println("Rexistro RMI creado no porto " + porto);
        }
    } 
}
