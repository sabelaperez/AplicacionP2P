# AplicaciónP2P

Aplicación P2P implementada con Java RMI para permitir la comunicación de varios clientes usando un único servidor.

## Execución
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
