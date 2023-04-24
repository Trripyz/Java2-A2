package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;


public class Controller implements Initializable {
    private static Controller instance;
    @FXML
    ListView<Message> chatContentList;
    ObservableList<Message> itemsOfMessage = FXCollections.observableArrayList();
    @FXML
    ListView<String> chatContentList11;
    @FXML
    private ListView<String> chatContentList111;
    @FXML
    ObservableList<String> itemsOfPrivate = FXCollections.observableArrayList();

    ObservableList<String> itemsOfPrivateState = FXCollections.observableArrayList();
    @FXML
    private ListView<ArrayList<String>> chatContentList1111;
    ObservableList<ArrayList<String>> itemsOfGroup = FXCollections.observableArrayList();
    @FXML
    private ListView<String> chatContentList112;
    ObservableList<String> itemsOfGroupState = FXCollections.observableArrayList();
    String username;

    HashMap<String, String> loginState;
    Client client;
    Thread runnableThread;
    @FXML
    private TextArea inputArea;
    boolean group = false;

    Alert dialog111 = new Alert(AlertType.INFORMATION);

    @FXML
    Label currentOnlineCnt;

    @FXML
    Label currentUsername;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chatContentList.setItems(itemsOfMessage);
        chatContentList111.setItems(itemsOfPrivate);
        chatContentList11.setItems(itemsOfPrivateState);
        chatContentList1111.setItems(itemsOfGroup);
        chatContentList112.setItems(itemsOfGroupState);
        for (int i = 0; i < 12; i++) {
            itemsOfPrivateState.add("");
        }
        for (int i = 0; i < 5; i++) {
            itemsOfGroupState.add("");
        }
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            client = new Client(input.get(), itemsOfPrivate, itemsOfMessage, dialog111,
                this,
                itemsOfPrivateState);
            runnableThread = new Thread(client);
            runnableThread.start();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (client.isUserNameOK()) {
                username = input.get();
            } else {
                Alert dialog1 = new Alert(AlertType.WARNING);
                dialog1.setTitle("Information dialog");
                dialog1.setHeaderText(null);
                dialog1.setContentText("Invalid username");
                dialog1.showAndWait();
                Platform.exit();
            }

        } else {
            Alert dialog1 = new Alert(AlertType.WARNING);
            dialog1.setTitle("Information dialog");
            dialog1.setHeaderText(null);
            dialog1.setContentText("Invalid username");
            dialog1.showAndWait();
            Platform.exit();
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        userSel.getItems().addAll(client.getCurrentUser());

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            group = false;
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
            client.chatTo = userSel.getSelectionModel().getSelectedItem();
            if (!itemsOfPrivate.contains(userSel.getSelectionModel().getSelectedItem())) {
                itemsOfPrivate.add("");
                for (int i = itemsOfPrivate.size() - 1; i > 0; i--) {
                    itemsOfPrivate.set(i, itemsOfPrivate.get(i - 1));
                }
                itemsOfPrivate.set(0, userSel.getSelectionModel().getSelectedItem());
//                items.add(userSel.getSelectionModel().getSelectedItem());
//                items2.add("");
                client.newPrivateChat();
            }
            try {
                updatePrivateChat();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user,
        //  just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel,
        //  the title should be the selected user's name
    }

    public void updatePrivateChat() throws InterruptedException {
        itemsOfMessage.clear();
        client.sendQuireGetPrivateChat();
//        items1.add(new Message(System.currentTimeMillis(),"123","456","nice"));
        Thread.sleep(400);
//        for (int i = 0; i < client.messages.size(); i++) {
//            items1.add(client.messages.get(i));
//        }
    }

    public void updateGroupChat() throws InterruptedException {
        itemsOfMessage.clear();
        client.sendQuireGetGroupChat();
        Thread.sleep(400);
    }

    @FXML
    public void createGroupChat() {
        Stage stage = new Stage();
        ArrayList<String> a = client.getCurrentUser();
        VBox vBox = new VBox(a.size());
        ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            checkBoxes.add(new CheckBox(a.get(i)));
        }
        Button okBtn = new Button("OK");
        ArrayList<String> users = new ArrayList<>();
        okBtn.setOnAction(e -> {
            group = true;
            for (int i = 0; i < checkBoxes.size(); i++) {
                if (checkBoxes.get(i).isSelected()) {
                    users.add(checkBoxes.get(i).getText());
                }
            }
            users.add(username);
            stage.close();
            client.chatToGroup = users;
            if (!itemsOfGroup.contains(users)) {
                itemsOfGroup.add(new ArrayList<>());
                for (int i = itemsOfGroup.size() - 1; i > 0; i--) {
                    itemsOfGroup.set(i, itemsOfGroup.get(i-1));
                }
                itemsOfGroup.set(0, users);
                client.newGroupChat();
            }
            try {
                updateGroupChat();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        vBox.getChildren().addAll(checkBoxes);
        vBox.getChildren().add(okBtn);
        vBox.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(vBox));
        stage.showAndWait();
    }

    @FXML
    public void doSendMessage() {
        // TODO
        if (!group) {
            if(!inputArea.getText().equals("")){
                client.sendMessage(inputArea.getText());
                itemsOfMessage.add(new Message(System.currentTimeMillis(),
                    username, client.chatTo, inputArea.getText()));
                inputArea.clear();
                int n = itemsOfPrivate.indexOf(client.chatTo);
                for (int i = n; i > 0; i--) {
                    itemsOfPrivate.set(i, itemsOfPrivate.get(i-1));
                }
                itemsOfPrivate.set(0, client.chatTo);
            }
        } else {
            if (!inputArea.getText().equals("")) {
                client.sendGroupMessage(inputArea.getText());
                itemsOfMessage.add(new Message(System.currentTimeMillis(),
                    username, client.chatTo, inputArea.getText()));
                inputArea.clear();
                int n = itemsOfGroup.indexOf(client.chatToGroup);
                for (int i = n; i > 0; i--) {
                    itemsOfGroup.set(i, itemsOfGroup.get(i - 1));
                }
                itemsOfGroup.set(0, client.chatToGroup);
            }
        }
    }

    @FXML
    public void doFile() throws IOException {
        FileChooser fileChooser = new FileChooser();
        Stage stage = new Stage();
        stage.setTitle("文件选择器");
        File file = fileChooser.showOpenDialog(stage);
        client.sendFile(file);
    }

    @FXML
    public void doEnter() throws InterruptedException {
        // TODO
        group = false;
        client.chatTo = chatContentList111.getSelectionModel().getSelectedItem();
        updatePrivateChat();
        itemsOfPrivateState
            .set(itemsOfPrivate
                .indexOf(chatContentList111.getSelectionModel().getSelectedItem()), "");
    }
    @FXML
    public void doGroup() throws InterruptedException {
        // TODO
        group = true;
        client.chatToGroup = chatContentList1111.getSelectionModel().getSelectedItem();
        updateGroupChat();
        itemsOfGroupState.set(itemsOfGroup
            .indexOf(chatContentList1111.getSelectionModel().getSelectedItem()), "");
    }

    @FXML
    public void doDelete() throws InterruptedException {
        // TODO
        itemsOfPrivateState.set(itemsOfPrivate
            .indexOf(chatContentList111.getSelectionModel().getSelectedItem()), "");
        itemsOfPrivate.remove(chatContentList111.getSelectionModel().getSelectedItem());
        if (itemsOfPrivate.size() == 0) {
            client.chatTo = null;
        } else {
            client.chatTo = itemsOfPrivate.get(0);
        }
        updatePrivateChat();
    }

    public void messageReminder(String from) {
        Alert dialog1 = new Alert(AlertType.INFORMATION);
        dialog1.setTitle("Information dialog");
        dialog1.setHeaderText(null);
        dialog1.setContentText(from + "send message");
        dialog1.showAndWait();
    }

    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    public static Controller getInstance() {
        return instance;
    }

    public Controller() {
        instance = this;
    }

    public void sendDisconnect(){
    }
}
