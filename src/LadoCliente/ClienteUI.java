package LadoCliente;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import LadoServidor.InterfaceServidor;

public class ClienteUI extends Application {
    
    private InterfaceServidor servidor;
    private ImplInterfaceCliente cliente;
    private ImplInterfacePeer peer;
    private String nombre;
    private String contrasinal;
    
    private Stage primaryStage;
    private ListView<HBox> userListView;
    private VBox chatArea;
    private TextField messageField;
    private Button sendBtn;
    private Label currentChatLabel;
    private String currentChatUser = null;
    
    // Store chat history for each user
    private Map<String, VBox> chatHistories = new HashMap<>();
    
    // Store notification indicators for each user
    private Map<String, Circle> userNotificationIcons = new HashMap<>();
    
    // Store HBox containers for each user in the list
    private Map<String, HBox> userListItems = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Aplicación de Mensaxería P2P");
        
        // Mostrar a pantall de rexistro e inicio de sesión
        showLoginScreen();
    }

    private void showLoginScreen() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label titleLabel = new Label("P2P Messaging App");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        grid.add(titleLabel, 0, 0, 2, 1);

        Label ipLabel = new Label("IP do Servidor:");
        grid.add(ipLabel, 0, 1);
        TextField ipField = new TextField("localhost");
        grid.add(ipField, 1, 1);

        Label portLabel = new Label("Porto do Servidor:");
        grid.add(portLabel, 0, 2);
        TextField portField = new TextField("1099");
        grid.add(portField, 1, 2);

        Label userLabel = new Label("Nome de usuario:");
        grid.add(userLabel, 0, 3);
        TextField userField = new TextField();
        grid.add(userField, 1, 3);

        Label passLabel = new Label("Contrasinal:");
        grid.add(passLabel, 0, 4);
        PasswordField passField = new PasswordField();
        grid.add(passField, 1, 4);

        Button loginBtn = new Button("Iniciar Sesión");
        Button registerBtn = new Button("Rexistrarse");
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getChildren().addAll(loginBtn, registerBtn);
        grid.add(btnBox, 0, 5, 2, 1);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");
        grid.add(statusLabel, 0, 6, 2, 1);

        loginBtn.setOnAction(e -> {
            handleLogin(ipField.getText(), portField.getText(), 
                       userField.getText(), passField.getText(), 
                       false, statusLabel);
        });

        registerBtn.setOnAction(e -> {
            handleLogin(ipField.getText(), portField.getText(), 
                       userField.getText(), passField.getText(), 
                       true, statusLabel);
        });

        Scene scene = new Scene(grid, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleLogin(String ip, String port, String username, 
                            String password, boolean isRegister, Label statusLabel) {
        try {
            // Check if arguments are correct
            InetAddress direccionIP = InetAddress.getByName(ip);
            Integer puerto = Integer.parseInt(port);

            // Connect to server
            String urlRegistro = "rmi://" + direccionIP.getHostAddress() + ":" + puerto + "/servidorRemoto";
            servidor = (InterfaceServidor) Naming.lookup(urlRegistro);
            
            nombre = username;
            contrasinal = password;
            
            // Create client and peer implementations
            cliente = new ImplInterfaceCliente();
            peer = new ImplInterfacePeer(nombre);
            
            boolean success;
            if (isRegister) {
                success = servidor.registerUser(nombre, contrasinal);
                if (success) {
                    // Automatically logs into the interface
                    servidor.logIn(cliente, peer, contrasinal);
                    statusLabel.setText("Registered successfully!");
                    showMainUI();
                } else {
                    statusLabel.setText("Username already exists!");
                }
            } else {
                success = servidor.logIn(cliente, peer, contrasinal);
                if (success) {
                    showMainUI();
                } else {
                    statusLabel.setText("Invalid credentials!");
                }
            }
        }
        catch (UnknownHostException e) {
            showAlert("La dirección IP es inválida", e.getMessage());
            e.printStackTrace();
        }
        catch (NumberFormatException e){
            showAlert("El puerto introducido no es válido", e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            showAlert("Error de conexión", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showMainUI() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // Left side - User list
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(200);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: #f0f0f0;");

        Label usersLabel = new Label("Usuarios en Liña");
        usersLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        userListView = new ListView<>();
        userListView.setPrefHeight(400);
        
        Button refreshBtn = new Button("Refrescar");
        refreshBtn.setOnAction(e -> refreshUserList());
        
        leftPanel.getChildren().addAll(usersLabel, userListView, refreshBtn);

        // Center - Chat area
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(10));

        currentChatLabel = new Label("Select a user to chat");
        currentChatLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ScrollPane chatScrollPane = new ScrollPane();
        chatArea = new VBox(5);
        chatArea.setPadding(new Insets(10));
        chatScrollPane.setContent(chatArea);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(400);
        chatScrollPane.setStyle("-fx-background-color: white;");

        HBox messageBox = new HBox(10);
        messageField = new TextField();
        messageField.setPrefWidth(400);
        messageField.setDisable(true);
        
        sendBtn = new Button("Send");
        sendBtn.setDisable(true);
        sendBtn.setOnAction(e -> sendMessage());
        
        messageField.setOnAction(e -> sendMessage());
        
        messageBox.getChildren().addAll(messageField, sendBtn);

        centerPanel.getChildren().addAll(currentChatLabel, chatScrollPane, messageBox);

        mainLayout.setLeft(leftPanel);
        mainLayout.setCenter(centerPanel);

        // User list selection handler
        userListView.setOnMouseClicked(e -> {
            HBox selectedItem = userListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                String selectedUser = getUsernameFromListItem(selectedItem);
                if (selectedUser != null) {
                    switchToChat(selectedUser);
                }
            }
        });

        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("P2P Chat - " + nombre);
        
        // Refresh user list initially
        refreshUserList();

        // Set message handler to automatically update the chat
        peer.setMessageHandler((message, senderName) -> {
            handleIncomingMessage(message, senderName);
        });
        
        // Set message handler to automatically add newly connected users
        cliente.setUserAdditionHandler((amigo, interfazAmigo) -> {
            addUserToList(amigo, true);
        });

        // Set message handler to automatically remove disconnected users
        cliente.setUserRemovalHandler((amigo, interfazAmigo) -> {
            removeUserFromList(amigo);
        });
        
        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            try {
                servidor.logOut(nombre, contrasinal);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
            System.exit(0);
        });
    }

    private void switchToChat(String username) {
        currentChatUser = username;
        currentChatLabel.setText("Chat with: " + username);
        messageField.setDisable(false);
        sendBtn.setDisable(false);
        
        // Clear current chat area
        chatArea.getChildren().clear();
        
        // Load chat history for this user
        VBox history = chatHistories.get(username);
        if (history != null) {
            chatArea.getChildren().addAll(history.getChildren());
        }
        
        // Clear notification icon for this user
        clearNotificationIcon(username);
    }

    private void refreshUserList() {
        try {
            var peers = cliente.getPeerNames();
            userListView.getItems().clear();
            userListItems.clear();
            userNotificationIcons.clear();
            
            for (String peerName : peers) {
                addUserToList(peerName, false);
            }
        } catch (RemoteException e) {
            showAlert("Error", "Failed to refresh user list: " + e.getMessage());
        }
    }

    private void addUserToList(String username, boolean isNewUser) {
        // Skip if user already exists in the list
        if (userListItems.containsKey(username)) {
            return;
        }
        
        HBox userItem = new HBox(10);
        userItem.setAlignment(Pos.CENTER_LEFT);
        userItem.setPadding(new Insets(5));
        
        Label usernameLabel = new Label(username);
        usernameLabel.setStyle("-fx-font-size: 14px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Message notification icon (red circle)
        Circle messageNotification = new Circle(5, Color.RED);
        messageNotification.setVisible(false);
        
        // New user notification icon (green circle)
        Circle newUserNotification = new Circle(5, Color.GREEN);
        newUserNotification.setVisible(isNewUser);
        
        userItem.getChildren().addAll(usernameLabel, spacer, messageNotification, newUserNotification);
        
        userListItems.put(username, userItem);
        userNotificationIcons.put(username, messageNotification);
        
        // Initialize chat history for this user if not exists
        if (!chatHistories.containsKey(username)) {
            chatHistories.put(username, new VBox(5));
            chatHistories.get(username).setPadding(new Insets(10));
        }
        
        Platform.runLater(() -> {
            userListView.getItems().add(userItem);
            
            // Auto-hide the new user notification after 3 seconds using Timeline
            if (isNewUser) {
                Timeline timeline = new Timeline(new KeyFrame(
                    javafx.util.Duration.seconds(3),
                    event -> newUserNotification.setVisible(false)
                ));
                timeline.setCycleCount(1);
                timeline.play();
            }
        });
    }

    private void removeUserFromList(String username) {
        // Create disconnected message
        Label disconnectLabel = new Label("--- " + username + " disconnected ---");
        disconnectLabel.setWrapText(true);
        disconnectLabel.setMaxWidth(400);
        disconnectLabel.setPadding(new Insets(5, 10, 5, 10));
        disconnectLabel.setStyle("-fx-background-color: #FFE4B5; -fx-background-radius: 10; -fx-text-fill: #8B4513; -fx-font-style: italic;");
        disconnectLabel.setAlignment(Pos.CENTER); 

        // Check if we're currently chatting with the disconnected use
        if (username.equals(currentChatUser)) {
            // Add the disconnect message to the current chat view as well
            Platform.runLater(() -> {
                chatArea.getChildren().add(disconnectLabel);
                
                // Disable sending messages but keep the chat open
                messageField.setDisable(true);
                sendBtn.setDisable(true);
                currentChatLabel.setText("Chat with: " + username + " (Offline)");
            });
        }
        
        // Remove from notification icons
        userNotificationIcons.remove(username);
        
        // Remove from user list
        HBox item = userListItems.remove(username);

        // Remove from chat history
        chatHistories.remove(username);

        if (item != null) {
            Platform.runLater(() -> {
                userListView.getItems().remove(item);
            });
        }
    }

    private String getUsernameFromListItem(HBox item) {
        if (item.getChildren().get(0) instanceof Label) {
            return ((Label) item.getChildren().get(0)).getText();
        }
        return null;
    }

    private void handleIncomingMessage(String message, String senderName) {
        // Add message to the sender's chat history
        addMessageToHistory(senderName, senderName + ": " + message, false);
        
        // If the message is from a different user than the currently selected one, show notification
        if (!senderName.equals(currentChatUser)) {
            showNotificationIcon(senderName);
        } else {
            // If we're currently chatting with this user, display the message
            addMessageToCurrentChat(senderName + ": " + message, false);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || currentChatUser == null) {
            return;
        }

        try {
            InterfacePeer targetPeer = ((ImplInterfaceCliente) cliente).find(currentChatUser);
            if (targetPeer != null) {
                targetPeer.receiveMessage(message, nombre);
                
                String formattedMessage = "You: " + message;
                addMessageToHistory(currentChatUser, formattedMessage, true);
                addMessageToCurrentChat(formattedMessage, true);
                
                messageField.clear();
            } else {
                showAlert("Error", "User not found!");
            }
        } catch (RemoteException e) {
            showAlert("Error", "Failed to send message: " + e.getMessage());
        }
    }

    private void addMessageToHistory(String username, String message, boolean isSent) {
        VBox history = chatHistories.get(username);
        if (history == null) {
            history = new VBox(5);
            history.setPadding(new Insets(10));
            chatHistories.put(username, history);
        }
        
        Label msgLabel = createMessageLabel(message, isSent);
        history.getChildren().add(msgLabel);
    }

    private void addMessageToCurrentChat(String message, boolean isSent) {
        Label msgLabel = createMessageLabel(message, isSent);
        Platform.runLater(() -> {
            chatArea.getChildren().add(msgLabel);
        });
    }

    private Label createMessageLabel(String message, boolean isSent) {
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(400);
        msgLabel.setPadding(new Insets(5, 10, 5, 10));
        
        if (isSent) {
            msgLabel.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10;");
            msgLabel.setAlignment(Pos.CENTER_RIGHT);
        } else {
            msgLabel.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E0E0E0; -fx-background-radius: 10;");
            msgLabel.setAlignment(Pos.CENTER_LEFT);
        }
        
        return msgLabel;
    }

    private void showNotificationIcon(String username) {
        Circle notificationIcon = userNotificationIcons.get(username);
        if (notificationIcon != null) {
            Platform.runLater(() -> notificationIcon.setVisible(true));
        }
    }

    private void clearNotificationIcon(String username) {
        Circle notificationIcon = userNotificationIcons.get(username);
        if (notificationIcon != null) {
            Platform.runLater(() -> notificationIcon.setVisible(false));
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}