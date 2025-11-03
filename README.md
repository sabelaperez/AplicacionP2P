# AplicaciónP2P

Aplicación P2P implementada con Java RMI para permitir la comunicación de varios clientes usando un único servidor.

## Ejecución
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
