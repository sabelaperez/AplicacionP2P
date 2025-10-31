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
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import LadoServidor.InterfaceServidor;

public class ClienteUI extends Application {
    
    private InterfaceServidor servidor;
    private ImplInterfaceCliente cliente;
    private ImplInterfacePeer peer;
    private String nombre;
    private String contrasinal;
    
    private Stage primaryStage;
    private ListView<HBox> userListView;
    private ListView<HBox> pendingRequestsListView;
    private VBox chatArea;
    private TextField messageField;
    private Button sendBtn;
    private Label currentChatLabel;
    private Label pendingCountLabel;
    private String currentChatUser = null;
    
    // Store chat history for each user
    private Map<String, ArrayList<ChatMessage>> chatHistories = new HashMap<>();
    
    // Store notification indicators for each user
    private Map<String, Circle> userNotificationIcons = new HashMap<>();
    
    // Store HBox containers for each user in the list
    private Map<String, HBox> userListItems = new HashMap<>();
    
    // Store pending friend requests
    private Map<String, HBox> pendingRequestItems = new HashMap<>();

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
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            statusLabel.setText("Username cannot be empty!");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            statusLabel.setText("Password cannot be empty!");
            return;
        }
        
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

            // ADD HANDLERS
            // Set message handler to automatically update the chat
            peer.setMessageHandler((message, senderName) -> {
                handleIncomingMessage(message, senderName);
            });
            
            // Set handler to automatically add newly connected friends
            cliente.setUserAdditionHandler((amigo, interfazAmigo) -> {
                addUserToList(amigo, true);
            });

            // Set handler to automatically remove disconnected friends
            cliente.setUserRemovalHandler((amigo) -> {
                removeUserFromList(amigo);
            });

            // Set handler to automatically receive friend requests
            cliente.setFriendRequestHandler((requesterName) -> {
                handleNewFriendRequest(requesterName);
            });
            
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

        // Left side - User list and friend requests
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(250);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: #f0f0f0;");

        // Friends section
        HBox friendsHeader = new HBox(10);
        friendsHeader.setAlignment(Pos.CENTER_LEFT);
        Label friendsLabel = new Label("Amigos");
        friendsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Button addFriendBtn = new Button("+");
        addFriendBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        addFriendBtn.setTooltip(new Tooltip("Enviar solicitud de amizade"));
        addFriendBtn.setOnAction(e -> showAddFriendDialog());
        friendsHeader.getChildren().addAll(friendsLabel, addFriendBtn);
        
        userListView = new ListView<>();
        userListView.setPrefHeight(300);
        
        // Pending requests section
        HBox requestsHeader = new HBox(10);
        requestsHeader.setAlignment(Pos.CENTER_LEFT);
        Label requestsLabel = new Label("Solicitudes Pendentes");
        requestsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        pendingCountLabel = new Label("(0)");
        pendingCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Refresh button
        Button refreshRequestsBtn = new Button("⟳");
        refreshRequestsBtn.setStyle("-fx-font-size: 12px;");
        refreshRequestsBtn.setTooltip(new Tooltip("Refrescar solicitudes"));
        refreshRequestsBtn.setOnAction(e -> refreshPendingRequests());

        requestsHeader.getChildren().addAll(requestsLabel, pendingCountLabel, refreshRequestsBtn);
        
        pendingRequestsListView = new ListView<>();
        pendingRequestsListView.setPrefHeight(150);
        pendingRequestsListView.setSelectionModel(null);
        
        leftPanel.getChildren().addAll(friendsHeader, userListView, requestsHeader, pendingRequestsListView);

        // Settings button
        Button settingsButton = new Button("Configuración");
        settingsButton.setStyle("-fx-background-color: #424242; -fx-text-fill: white; -fx-font-size: 12px;");
        settingsButton.setOnAction(e -> showSettingsWindow());

        leftPanel.getChildren().add(settingsButton);
        VBox.setMargin(settingsButton, new Insets(20, 0, 0, 0));        

        // Center - Chat area
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(10));

        currentChatLabel = new Label("Select a friend to chat");
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

        Scene scene = new Scene(mainLayout, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("P2P Chat - " + nombre);

        // Load pending friend requests from server
        loadPendingRequests();
        
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

    private void showSettingsWindow() {
        Stage settingsStage = new Stage();
        settingsStage.setTitle("Configuración");
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #2c2c2c;");
        
        Label titleLabel = new Label("Configuración de Cuenta");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Botón para cambiar contraseña
        Button changePasswordButton = new Button("Cambiar Contraseña");
        changePasswordButton.setPrefWidth(250);
        changePasswordButton.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        changePasswordButton.setOnAction(e -> {
            showChangePasswordWindow();
        });
        
        // Botón para eliminar cuenta
        Button deleteAccountButton = new Button("Eliminar Cuenta");
        deleteAccountButton.setPrefWidth(250);
        deleteAccountButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        deleteAccountButton.setOnAction(e -> {
            showDeleteAccountWindow();
            settingsStage.close();
        });
        
        // Botón para cerrar sesión
        Button logoutButton = new Button("Cerrar Sesión");
        logoutButton.setPrefWidth(250);
        logoutButton.setStyle("-fx-background-color: #f57c00; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        logoutButton.setOnAction(e -> {
            handleLogout();
            settingsStage.close();
        });
        
        // Botón para cancelar
        Button cancelButton = new Button("Cancelar");
        cancelButton.setPrefWidth(250);
        cancelButton.setStyle("-fx-background-color: #616161; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        cancelButton.setOnAction(e -> settingsStage.close());
        
        mainLayout.getChildren().addAll(
            titleLabel,
            new Label("Usuario: " + nombre) {{
                setStyle("-fx-text-fill: #bbbbbb; -fx-font-size: 14px;");
            }},
            new Separator(),
            changePasswordButton,
            deleteAccountButton,
            logoutButton,
            new Separator(),
            cancelButton
        );
        
        Scene scene = new Scene(mainLayout, 400, 450);
        settingsStage.setScene(scene);
        settingsStage.showAndWait();
    }

    private void loadPendingRequests() {
        new Thread(() -> {
            try {
                ArrayList<String> requests = servidor.getFriendRequests(nombre, contrasinal);
                
                if (requests != null && !requests.isEmpty()) {
                    Platform.runLater(() -> {
                        for (String requesterName : requests) {
                            addPendingRequest(requesterName);
                        }
                        updatePendingCount();
                    });
                }
            } catch (RemoteException e) {
                System.err.println("Error loading friend requests: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void showChangePasswordWindow() {
        Stage changePassStage = new Stage();
        changePassStage.setTitle("Cambiar Contraseña");
        changePassStage.initModality(Modality.APPLICATION_MODAL);
        
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #2c2c2c;");
        
        Label titleLabel = new Label("Cambiar Contraseña");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Contraseña actual");
        oldPasswordField.setPrefWidth(300);
        oldPasswordField.setStyle("-fx-background-color: #424242; -fx-text-fill: white; -fx-prompt-text-fill: #888;");
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nueva contraseña");
        newPasswordField.setPrefWidth(300);
        newPasswordField.setStyle("-fx-background-color: #424242; -fx-text-fill: white; -fx-prompt-text-fill: #888;");
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmar nueva contraseña");
        confirmPasswordField.setPrefWidth(300);
        confirmPasswordField.setStyle("-fx-background-color: #424242; -fx-text-fill: white; -fx-prompt-text-fill: #888;");
        
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 12px;");
        
        Button changeButton = new Button("Cambiar Contraseña");
        changeButton.setPrefWidth(150);
        changeButton.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white;");
        changeButton.setOnAction(e -> {
            String oldPass = oldPasswordField.getText();
            String newPass = newPasswordField.getText();
            String confirmPass = confirmPasswordField.getText();
            
            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                statusLabel.setText("Por favor, completa todos los campos");
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                statusLabel.setText("Las contraseñas nuevas no coinciden");
                return;
            }
            
            if (!oldPass.equals(contrasinal)) {
                statusLabel.setText("La contraseña actual es incorrecta");
                return;
            }
            
            try {
                boolean success = servidor.changePassword(nombre, oldPass, newPass);
                if (success) {
                    contrasinal = newPass;
                    showInfoAlert("Éxito", "Contraseña cambiada correctamente");
                    changePassStage.close();
                } else {
                    statusLabel.setText("Error al cambiar la contraseña");
                }
            } catch (RemoteException ex) {
                showAlert("Error", "Error de conexión con el servidor");
                ex.printStackTrace();
            }
        });
        
        Button cancelButton = new Button("Cancelar");
        cancelButton.setPrefWidth(150);
        cancelButton.setStyle("-fx-background-color: #616161; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> changePassStage.close());
        
        HBox buttonBox = new HBox(10, changeButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        layout.getChildren().addAll(
            titleLabel,
            oldPasswordField,
            newPasswordField,
            confirmPasswordField,
            statusLabel,
            buttonBox
        );
        
        Scene scene = new Scene(layout, 400, 350);
        changePassStage.setScene(scene);
        changePassStage.showAndWait();
    }

    private void showDeleteAccountWindow() {
        Stage deleteStage = new Stage();
        deleteStage.setTitle("Eliminar Cuenta");
        deleteStage.initModality(Modality.APPLICATION_MODAL);
        
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #2c2c2c;");
        
        Label warningLabel = new Label("⚠ ADVERTENCIA ⚠");
        warningLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f44336;");
        
        Label infoLabel = new Label("Esta acción eliminará permanentemente tu cuenta.\nNo se puede deshacer.");
        infoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-text-alignment: center;");
        infoLabel.setWrapText(true);
        
        Label confirmLabel = new Label("Escribe tu contraseña para confirmar:");
        confirmLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Contraseña");
        passwordField.setPrefWidth(300);
        passwordField.setStyle("-fx-background-color: #424242; -fx-text-fill: white; -fx-prompt-text-fill: #888;");
        
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 12px;");
        
        Button deleteButton = new Button("Eliminar Cuenta");
        deleteButton.setPrefWidth(150);
        deleteButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteButton.setOnAction(e -> {
            String password = passwordField.getText();
            
            if (password.isEmpty()) {
                statusLabel.setText("Por favor, introduce tu contraseña");
                return;
            }
            
            if (!password.equals(contrasinal)) {
                statusLabel.setText("Contraseña incorrecta");
                return;
            }
            
            try {
                boolean success = servidor.deleteUser(nombre, password);
                if (success) {
                    showInfoAlert("Cuenta eliminada", "Tu cuenta ha sido eliminada correctamente");
                    deleteStage.close();
                    // Volver a la pantalla de login
                    start(new Stage());
                } else {
                    statusLabel.setText("Error al eliminar la cuenta");
                }
            } catch (RemoteException ex) {
                showAlert("Error", "Error de conexión con el servidor");
                ex.printStackTrace();
            }
        });
        
        Button cancelButton = new Button("Cancelar");
        cancelButton.setPrefWidth(150);
        cancelButton.setStyle("-fx-background-color: #616161; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> deleteStage.close());
        
        HBox buttonBox = new HBox(10, deleteButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        layout.getChildren().addAll(
            warningLabel,
            infoLabel,
            confirmLabel,
            passwordField,
            statusLabel,
            buttonBox
        );
        
        Scene scene = new Scene(layout, 400, 300);
        deleteStage.setScene(scene);
        deleteStage.showAndWait();
    }

    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cerrar Sesión");
        confirmAlert.setHeaderText("¿Estás seguro que deseas cerrar sesión?");
        confirmAlert.setContentText("Tendrás que volver a iniciar sesión para usar la aplicación.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    servidor.logOut(nombre, contrasinal);
                    // Volver a la pantalla de login
                    Stage currentStage = (Stage) Stage.getWindows().stream()
                        .filter(Window::isShowing)
                        .findFirst()
                        .orElse(null);
                    
                    if (currentStage != null) {
                        currentStage.close();
                    }
                    
                    start(new Stage());
                } catch (RemoteException e) {
                    showAlert("Error", "Error al cerrar sesión: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void refreshPendingRequests() {
        new Thread(() -> {
            try {
                ArrayList<String> requests = servidor.getFriendRequests(nombre, contrasinal);
                
                Platform.runLater(() -> {
                    // Clear existing requests from UI
                    pendingRequestItems.clear();
                    pendingRequestsListView.getItems().clear();
                    
                    // Add all current requests
                    if (requests != null && !requests.isEmpty()) {
                        for (String requesterName : requests) {
                            addPendingRequest(requesterName);
                        }
                    }
                    
                    updatePendingCount();
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Erro ao refrescar solicitudes: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }
    private void showAddFriendDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Engadir Amigo");
        dialog.setHeaderText("Enviar solicitude de amizade");
        dialog.setContentText("Nome de usuario:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            if (!username.trim().isEmpty()) {
                sendFriendRequest(username.trim());
            }
        });
    }

    private void sendFriendRequest(String targetUsername) {
        if (targetUsername.equals(nombre)) {
            showInfoAlert("Error", "Non podes enviarte unha solicitude a ti mesmo!");
            return;
        }
        
        // Check if already friends
        if (userListItems.containsKey(targetUsername)) {
            showInfoAlert("Información", "Ya eres amigo de " + targetUsername);
            return;
        }
        
        new Thread(() -> {
            try {
                boolean success = servidor.sendFriendRequest(nombre, contrasinal, targetUsername);
                
                Platform.runLater(() -> {
                    if (success) {
                        showInfoAlert("Éxito", "Solicitude enviada a " + targetUsername);
                    } else {
                        showInfoAlert("Error", "Non se puido enviar a solicitude. O usuario pode non existir ou xa tés unha solicitude pendente.");
                    }
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Erro ao enviar solicitude: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void addPendingRequest(String requesterName) {
        HBox requestItem = new HBox(10);
        requestItem.setAlignment(Pos.CENTER_LEFT);
        requestItem.setPadding(new Insets(5));
        requestItem.setStyle("-fx-background-color: #FFF9E6; -fx-background-radius: 5;");
        
        Label nameLabel = new Label(requesterName);
        nameLabel.setStyle("-fx-font-size: 12px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button acceptBtn = new Button("✓");
        acceptBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        acceptBtn.setTooltip(new Tooltip("Aceptar"));
        acceptBtn.setOnAction(e -> acceptFriendRequest(requesterName));
        
        Button rejectBtn = new Button("✗");
        rejectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        rejectBtn.setTooltip(new Tooltip("Rexeitar"));
        rejectBtn.setOnAction(e -> rejectFriendRequest(requesterName));
        
        requestItem.getChildren().addAll(nameLabel, spacer, acceptBtn, rejectBtn);
        
        pendingRequestItems.put(requesterName, requestItem);
        pendingRequestsListView.getItems().add(requestItem);
    }

    private void acceptFriendRequest(String requesterName) {
        new Thread(() -> {
            try {
                boolean success = servidor.answerFriendRequest(nombre, contrasinal, requesterName, true);
                
                Platform.runLater(() -> {
                    if (success) {
                        // Remove from pending requests
                        removePendingRequest(requesterName);

                        showInfoAlert("Éxito", "Ahora eres amigo de " + requesterName);
                    } else {
                        showInfoAlert("Error", "No se pudo aceptar la solicitud");
                    }
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Erro ao aceptar solicitude: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void rejectFriendRequest(String requesterName) {
        new Thread(() -> {
            try {
                boolean success = servidor.answerFriendRequest(nombre, contrasinal, requesterName, false);
                
                Platform.runLater(() -> {
                    if (success) {
                        removePendingRequest(requesterName);
                        showInfoAlert("Información", "Solicitude de " + requesterName + " rexeitada");
                    } else {
                        showInfoAlert("Error", "Non se puido rexeitar a solicitude");
                    }
                });
            } catch (RemoteException e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Erro ao rexeitar solicitude: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void removePendingRequest(String requesterName) {
        HBox item = pendingRequestItems.remove(requesterName);
        if (item != null) {
            pendingRequestsListView.getItems().remove(item);
            updatePendingCount();
        }
    }

    private void updatePendingCount() {
        int count = pendingRequestItems.size();
        pendingCountLabel.setText("(" + count + ")");
        
        if (count > 0) {
            pendingCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        } else {
            pendingCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        }
    }

    private void handleNewFriendRequest(String requesterName) {
        // Check if request already exists
        if (pendingRequestItems.containsKey(requesterName)) {
            return;
        }
        
        // Add to pending requests
        addPendingRequest(requesterName);
        updatePendingCount();
        
        // Optionally show a notification
        showInfoAlert("Nueva Solicitud", requesterName + " quiere ser tu amigo!");
    }

    private void switchToChat(String username) {
        if (username.equals(currentChatUser)){
            return;
        }

        currentChatUser = username;
        currentChatLabel.setText("Chat with: " + username);
        messageField.setDisable(false);
        sendBtn.setDisable(false);
        
        // Clear current chat area
        chatArea.getChildren().clear();
        
        // Load chat history for this user
        ArrayList<ChatMessage> history = chatHistories.get(username);
        if (history != null) {
            for(ChatMessage message : history){
                if(message.isDisconection()){
                    chatArea.getChildren().add(createDisconnectLabel(message.message()));
                }
                else{
                    chatArea.getChildren().add(createMessageLabel(message.message(), message.isSent()));
                }
            }
        }
        
        // Clear notification icon for this user
        clearNotificationIcon(username);
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
        
        // New user notification icon (badge)
        Label newUserBadge = new Label("NOVO");
        newUserBadge.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 2 4; -fx-background-radius: 3;");
        newUserBadge.setVisible(isNewUser);
        
        userItem.getChildren().addAll(usernameLabel, spacer, messageNotification, newUserBadge);
        
        userListItems.put(username, userItem);
        userNotificationIcons.put(username, messageNotification);
        
        // Initialize chat history for this user if not exists
        if (!chatHistories.containsKey(username)) {
            chatHistories.put(username, new ArrayList<ChatMessage>());
            // chatHistories.get(username).setPadding(new Insets(10));
        }
        
        Platform.runLater(() -> {
            userListView.getItems().add(userItem);
            
            // Auto-hide the new user badge after 5 seconds
            if (isNewUser) {
                Timeline timeline = new Timeline(new KeyFrame(
                    javafx.util.Duration.seconds(5),
                    event -> newUserBadge.setVisible(false)
                ));
                timeline.setCycleCount(1);
                timeline.play();
            }
        });
    }

    private void removeUserFromList(String username) {
        // Add disconnect message to history (preserves it)
        String text = "--- " + username + " disconnected ---";
        ArrayList<ChatMessage> history = chatHistories.get(username);
        if (history != null) {
            history.add(new ChatMessage(text, false, true));
        }
        
        // Check if we're currently chatting with the disconnected user
        if (username.equals(currentChatUser)) {            
            Platform.runLater(() -> {
                chatArea.getChildren().add(createDisconnectLabel(text));
                messageField.setDisable(true);
                sendBtn.setDisable(true);
                currentChatLabel.setText("Chat with: " + username + " (Offline)");
            });
        }
        
        // Remove from notification icons
        userNotificationIcons.remove(username);
        
        // Remove from user list
        HBox item = userListItems.remove(username);
                
        if (item != null) {
            Platform.runLater(() -> {
                userListView.getItems().remove(item);
            });
        }
    }

    private Label createDisconnectLabel(String message) {
        Label disconnectLabel = new Label(message);
        disconnectLabel.setWrapText(true);
        disconnectLabel.setMaxWidth(400);
        disconnectLabel.setPadding(new Insets(5, 10, 5, 10));
        disconnectLabel.setStyle("-fx-background-color: #FFE4B5; -fx-background-radius: 10; -fx-text-fill: #8B4513; -fx-font-style: italic;");
        disconnectLabel.setAlignment(Pos.CENTER);
        return disconnectLabel;
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
        ArrayList<ChatMessage> history = chatHistories.get(username);
        if (history == null) {
            history = new ArrayList<ChatMessage>();
            chatHistories.put(username, history);
        }
        
        Label msgLabel = createMessageLabel(message, isSent);
        history.add(new ChatMessage(message, isSent, false));
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

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}