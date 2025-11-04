# Aplicación P2P

Aplicación P2P implementada con Java RMI para permitir la comunicación de varios clientes usando un único servidor.

## Ejecución
### Ejecución demo
Desde la carpeta raíz del proyecto.
- En el lado del servidor:
  ```
  ./gradlew runServer
  ```
  Luego se pedirá el puerto en el que se desea crear el registro RMI, se introduce y el servidor comienza a correr.
- En el lado del cliente:
  ```
  ./gradlew runClient
  ```
  Con esto se iniciará la GUI que permitirá al cliente interactuar con el servidor RMI conociendo su dirección IP y puerto.

### Ejecución real
Para ejecutar el servidor y el cliente desde máquinas distintas, habría que:
- Lado del cliente:  
  Compilar InterfaceServidor.java y copiar el .class en la carpeta LadoServidor.
- Lado del servidor
  Compilar InterfaceCliente.java y InterfacePeer.java y copiar los .class en la carpeta LadoServidor.
Luego ejecutar todo de forma normal.
> JavaRMI trabaja con las funciones públicas de la interfaz, por lo que necesita conocerlas para llamarlas.

## Funcionalidades
#### Gestión de cuentas
- Inicio de sesión
- Registro
- Cambio de contraseña
- Eliminación de la cuenta
#### Grupos de amistad
- Enviar solicitudes de amistad
- Aceptar/Rechazar solicitudes de amistad
- Enviar mensajes a tus amigos

## ❗ Aviso
Este programa es meramente educativo y no se recomienda su uso en entornos reales.
Contiene errores de seguridad graves como guardar las contraseñas en texto plano.
