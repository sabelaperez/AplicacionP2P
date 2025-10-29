package LadoServidor;

import LadoCliente.InterfaceCliente;
import LadoCliente.InterfacePeer;

public record Interfaces(InterfaceCliente cliente, InterfacePeer peer) {}
