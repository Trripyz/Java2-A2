package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Client implements Runnable{
    private Controller controller;
    boolean userNameOK = false;
    String userName;
    OOS oos;
    OIS ois;
    ArrayList<String> userList = new ArrayList<>();
    Message mas;
    String chatTo;
    ArrayList<Message> messages;

    ObservableList<Message> items1;
    ObservableList<String> items;
    ObservableList<String> items2;
    ArrayList<String> chatToGroup = new ArrayList<>();
    Alert dialog;
    public Client(String userName,ObservableList<String> items,ObservableList<Message> items1,
        Alert dialog,Controller controller,ObservableList<String> items2) {
        this.userName = userName;
        this.items = items;
        this.items1 = items1;
        this.dialog = dialog;
        this.controller = controller;
        this.items2 = items2;
    }

    @Override
    public void run() {
        try {
            Socket s = new Socket("localhost",8888);
            oos = new OOS(s.getOutputStream());
            connect();
            while (s.isConnected()) {
                ois = new OIS(s.getInputStream());
                mas = (Message) ois.readObject();
                if(mas != null){
                    switch (mas.getType())
                    {
                        case "SUCCESS":
                            // switch to chat interface
//                            loginController.changeStage(Main.CHATUIID);
                            userNameOK = true;
                            Platform.runLater(() ->{
                                controller.currentUsername.setText("Current User: " + userName);
                            });
                            break;
                        case "FAIL":
                            // login failed, show reason
//                            loginController.setResultText(message.getContent());
                            break;
                        case "MSG":
                            // chat interface, two types -> single and multiple
//                            controller.addOtherMessges(message);
                            receiveMessage(mas);
                            break;
                        case "USERLIST":
                            // update user list and calculate the number of online users
//                            controller.setUserList(message.getUserlist());
                            setUserList();
                            break;
                        case "NOTIFICATION":
                            // online & offline notification
//                            controller.addNotification(message.getContent());
                            break;
                        case "privatechat":
                            Platform.runLater(() ->{
                                for (int i = 0; i < mas.getMessages().size(); i++) {
                                    items1.add(mas.getMessages().get(i));
                                }
                            });
                            messages = mas.getMessages();
                            break;
                        case "groupchat":
                            Platform.runLater(() ->{
                                for (int i = 0; i < mas.getMessages().size(); i++) {
                                    items1.add(mas.getMessages().get(i));
                                }
                            });
                            break;
                        case "MSGTOGROUP":
                            receiveGroupMessage(mas);
                            break;
                        case "FILE":
                            getFile(mas);
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() ->{
                Alert dialog1 = new Alert(AlertType.WARNING);
                dialog1.setTitle("Information dialog");
                dialog1.setHeaderText(null);
                dialog1.setContentText("Server is closed");
                dialog1.showAndWait();
            });
        }

    }

    public void setUserList() {
        userList = mas.getUserList();
        Platform.runLater(() ->{
            controller.currentOnlineCnt.setText("Online: " + userList.size());
            for (int i = 0; i < controller.itemsOfPrivate.size(); i++) {
                if(!userList.contains(controller.itemsOfPrivate.get(i))){
                    controller.itemsOfPrivateState.set(i,"↓");
                }
            }
        });
    }

    public void send(Message message) {
        try
        {
            oos.writeObject(message);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public void connect() {
        //create message class
        Message message = new Message();
        message.setType("CONNECT");
        userList.add(userName);
        message.setUserlist(userList);
        message.setFrom(userName);
        //send
        send(message);
    }

    public ArrayList<String> getCurrentUser(){
        ArrayList<String> ans = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            if(!userList.get(i).equals(userName)){
                ans.add(userList.get(i));
            }
        }
        return ans;
    }

    public void sendQuireGetPrivateChat(){
        Message message = new Message();
        message.setType("getPrivateChat");
        message.setFrom(userName);
        message.setTo(chatTo);
        send(message);
    }
    public void sendQuireGetGroupChat(){
        Message message = new Message();
        message.setType("getGroupChat");
        message.setUserlist(chatToGroup);
        message.setFrom(userName);
        send(message);
    }

    public void newPrivateChat(){
        Message message = new Message();
        message.setType("NEWPRIVATECHAT");
        message.setFrom(userName);
        message.setTo(chatTo);
        send(message);
    }

    public void newGroupChat(){
        Message message = new Message();
        message.setType("NEWGROUPCHAT");
        message.setFrom(userName);
        message.setUserlist(chatToGroup);
        send(message);
    }
    public boolean isUserNameOK() {
        return userNameOK;
    }

    public void sendMessage(String data){
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setFrom(userName);
        message.setSentBy(userName);
        message.setTo(chatTo);
        message.setData(data);
        message.setType("MSG");
        send(message);
    }

    public void sendGroupMessage(String data){
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setFrom(userName);
        message.setSentBy(userName);
        message.setTo(chatTo);
        message.setUserlist(chatToGroup);
        message.setData(data);
        message.setType("MSGTOGROUP");
        send(message);
    }

    public void receiveMessage(Message message){
        if(!items.contains(message.getFrom())){
            Platform.runLater(() ->{
                items.add("");
                for (int i = items.size() - 2; i >= 0; i--) {
                    items.set(i+1,items.get(i));
                    items2.set(i+1,items2.get(i));
                }
                items.set(0,message.getFrom());
            });
        }else {
            Platform.runLater(() ->{
                int n = items.indexOf(message.getFrom());
                for (int i = n; i > 0; i--) {
                    items.set(i,items.get(i-1));
                    items2.set(i,items2.get(i-1));
                }
                items.set(0,message.getFrom());
            });
        }
        if(chatTo == null || !chatTo.equals(message.getFrom())){
//            chatTo = message.getFrom();
//            items1.add(message);
            Platform.runLater(() ->{
                items2.set(0,"~");
            });
        }else {
            Platform.runLater(() ->{
                items1.add(message);
            });
        }
    }
    public void receiveGroupMessage(Message message){
        if(!controller.itemsOfGroup.contains(message.getUserList())){
            Platform.runLater(() ->{
                controller.itemsOfGroup.add(new ArrayList<>());
                for (int i = controller.itemsOfGroup.size() - 2; i >= 0; i--) {
                    controller.itemsOfGroup.set(i+1,controller.itemsOfGroup.get(i));
                    controller.itemsOfGroupState.set(i+1,controller.itemsOfGroupState.get(i));
                }
                controller.itemsOfGroup.set(0,message.getUserList());
            });
        }else {
            Platform.runLater(() ->{
                int n = controller.itemsOfGroup.indexOf(message.getUserList());
                for (int i = n; i > 0; i--) {
                    controller.itemsOfGroup.set(i,controller.itemsOfGroup.get(i-1));
                    controller.itemsOfGroupState.set(i,controller.itemsOfGroupState.get(i-1));
                }
                controller.itemsOfGroup.set(0,message.getUserList());
            });
        }
        if(chatToGroup == null || !chatToGroup.equals(message.getUserList())){
//            chatTo = message.getFrom();
//            items1.add(message);
            Platform.runLater(() ->{
                controller.itemsOfGroupState.set(0,"~");
            });
        }else {
            Platform.runLater(() ->{
                items1.add(message);
            });
        }
    }

    public void sendFile(File file) throws IOException {
        Message message = new Message();
        message.setFrom(userName);
        message.setSendTo(chatTo);
        message.setType("FILE");
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
        BufferedReader br = new BufferedReader(isr);
        ArrayList<String> file1 = new ArrayList<>();
        String s = null;
        while((s = br.readLine()) != null) {
            file1.add(s);
        }
        System.out.println(file1);
        br.close();
        message.setUserlist(file1);
        message.setData(file.getName());
        send(message);


//        OutputStreamWriter osw = new OutputStreamWriter(
//            new FileOutputStream("C:\\Users\\zyz'\\Desktop\\ttt.md"), "UTF-8");
//        for (int i = 0; i < file1.size(); i++) {
//            osw.write(file1.get(i));
//            osw.write("\n");
//        }
//        osw.close();
    }

    public void getFile(Message message) throws IOException {
        Platform.runLater(() ->{
            Stage stage = new Stage();
            stage.setTitle(message.getFrom()+" send a file");
            ComboBox<String> userSel = new ComboBox<>();
            userSel.getItems().addAll("download","no download");
            Button okBtn = new Button("OK");
            okBtn.setOnAction(e -> {
                stage.close();
                String item = userSel.getSelectionModel().getSelectedItem();
                if(item.equals("download")){
                    String Path = "C:\\Users\\zyz'\\Desktop\\" + message.getData();

                    File file = new File(Path);
                    try {
                        file.createNewFile();
                        OutputStreamWriter osw = new OutputStreamWriter(
                            new FileOutputStream(Path), "UTF-8");
                        for (int i = 0; i < message.getUserList().size(); i++) {
                            osw.write(message.getUserList().get(i));
                            osw.write("\n");
                        }
                        osw.close();
                    } catch (IOException ex) {
                    }
                }
            });
            HBox box = new HBox(10);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(200, 200, 200, 200));
            box.getChildren().addAll(userSel, okBtn);
            stage.setScene(new Scene(box));
            stage.showAndWait();
        });
//        String Path = "C:\\Users\\zyz'\\Desktop\\"+ userName + "\\" + message.getData();

    }
}
