package LadoCliente;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;

import LadoServidor.InterfaceServidor;

public class ClienteUI extends Application {
    
    private InterfaceServidor servidor;
    private InterfaceCliente cliente;
    private InterfacePeer peer;
    private String nombre;
    private String contrasinal;
    
    private Stage primaryStage;
    private ListView<String> userListView;
    private VBox chatArea;
    private TextField messageField;
    private Label currentChatLabel;
    private String currentChatUser = null;

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
            // Connect to server
            String urlRegistro = "rmi://" + ip + ":" + port + "/servidorRemoto";
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
        } catch (Exception e) {
            statusLabel.setText("Connection error: " + e.getMessage());
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

        Label usersLabel = new Label("Online Users");
        usersLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        userListView = new ListView<>();
        userListView.setPrefHeight(400);
        
        Button refreshBtn = new Button("Refresh");
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
        
        Button sendBtn = new Button("Send");
        sendBtn.setDisable(true);
        sendBtn.setOnAction(e -> sendMessage());
        
        messageField.setOnAction(e -> sendMessage());
        
        messageBox.getChildren().addAll(messageField, sendBtn);

        centerPanel.getChildren().addAll(currentChatLabel, chatScrollPane, messageBox);

        mainLayout.setLeft(leftPanel);
        mainLayout.setCenter(centerPanel);

        // User list selection handler
        userListView.setOnMouseClicked(e -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                currentChatUser = selectedUser;
                currentChatLabel.setText("Chat with: " + selectedUser);
                messageField.setDisable(false);
                sendBtn.setDisable(false);
                chatArea.getChildren().clear();
            }
        });

        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("P2P Chat - " + nombre);
        
        // Refresh user list initially
        refreshUserList();
        
        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            try {
                servidor.logOut(nombre, contrasinal);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
        });
    }

    private void refreshUserList() {
        try {
            var peers = cliente.getPeerNames();
            userListView.getItems().clear();
            userListView.getItems().addAll(peers);
        } catch (RemoteException e) {
            showAlert("Error", "Failed to refresh user list: " + e.getMessage());
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
                addMessageToChat("You: " + message, true);
                messageField.clear();
            } else {
                showAlert("Error", "User not found!");
            }
        } catch (RemoteException e) {
            showAlert("Error", "Failed to send message: " + e.getMessage());
        }
    }

    private void addMessageToChat(String message, boolean isSent) {
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
        
        Platform.runLater(() -> chatArea.getChildren().add(msgLabel));
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}