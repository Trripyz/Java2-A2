package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.OIS;
import cn.edu.sustech.cs209.chatting.common.OOS;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Main implements Serializable {
    private static final int SERVER_PORT = 8888;

    private static HashMap<String, Socket> socketsfromUserNames = new HashMap<>();

    private static ArrayList<String> onlineUsers = new ArrayList<>();

    private static ArrayList<PrivateChat> privateChats = new ArrayList<>();

    private static ArrayList<GroupChat> groupChats = new ArrayList<>();

    private static ArrayList<User> users = new ArrayList<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("privateChat.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        privateChats = (ArrayList<PrivateChat>) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
        FileInputStream fileInputStream1 = new FileInputStream("groupChat.txt");
        ObjectInputStream objectInputStream1 = new ObjectInputStream(fileInputStream1);
        groupChats = (ArrayList<GroupChat>) objectInputStream1.readObject();
        objectInputStream1.close();
        fileInputStream1.close();
        FileInputStream fileInputStream2 = new FileInputStream("user.txt");
        ObjectInputStream objectInputStream2 = new ObjectInputStream(fileInputStream2);
        users = (ArrayList<User>) objectInputStream2.readObject();
        objectInputStream2.close();
        fileInputStream2.close();
        System.out.println("Server start!");
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    FileOutputStream fileOutputStream =
                        new FileOutputStream("privateChat.txt");
                    ObjectOutputStream objectOutputStream =
                        new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(privateChats);
                    objectOutputStream.close();
                    fileOutputStream.close();
                    FileOutputStream fileOutputStream1 =
                        new FileOutputStream("groupChat.txt");
                    ObjectOutputStream objectOutputStream1 =
                        new ObjectOutputStream(fileOutputStream1);
                    objectOutputStream1.writeObject(groupChats);
                    objectOutputStream1.close();
                    fileOutputStream1.close();
                    FileOutputStream fileOutputStream2 = new FileOutputStream("user.txt");
                    ObjectOutputStream objectOutputStream2 =
                        new ObjectOutputStream(fileOutputStream2);
                    objectOutputStream2.writeObject(users);
                    objectOutputStream2.close();
                    fileOutputStream2.close();
                } catch (IOException e) {

                }
            }
        });
        try {
            while (true) {
                Socket s = serverSocket.accept();
                new Thread(new ServerThread(s)).start();
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    public void save() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("privateChat.txt");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(privateChats);
        objectOutputStream.close();
        fileOutputStream.close();
    }

    public void load() throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("privateChat.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        privateChats = (ArrayList<PrivateChat>) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
    }


    public static class ServerThread implements Runnable {
        private Socket s;
        // read message object which comes from client
        private OIS ois;
        String name;


        public ServerThread(Socket s) throws IOException {

            this.s = s;
            //initialize the object input stream
            ois = new OIS(s.getInputStream());
        }

        @Override
        public void run() {
            try {
                if (!s.isClosed()) {
                    System.out.println(s + " port has connected to the server!");
                }

                while (s.isConnected()) {
                    Message message = (Message) ois.readObject();
                    if (message != null) {
                        switch (message.getType()) {
                            case "CONNECT":
                                checkConnect(message);
                                break;
                            case "DISCONNECT":
                                closeConnect(message);
                                s.close();
                                break;
                            case "MSG":
                                sendMSG(message);
                                break;
                            case "QUERY":
                                sendOnlineUserList(false);
                                break;
                            case "NEWPRIVATECHAT":
                                newPrivatechat(message);
                                break;
                            case "getPrivateChat":
                                getPrivatechat(message);
                                break;
                            case "NEWGROUPCHAT":
                                newGroupChat(message);
                                break;
                            case "getGroupChat":
                                getGroupchat(message);
                                break;
                            case "MSGTOGROUP":
                                sendMSGToGroup(message);
                                break;
                            case "FILE":
                                sendFile(message);
                                break;
                            default:
                                break;
                        }
                    }
                }
                onlineUsers.remove(name);
                socketsfromUserNames.remove(name);
                sendOnlineUserList(true);
                ois.close();
            } catch (IOException | ClassNotFoundException e) {
//                e.printStackTrace();//注释掉就不会报错////////
// ////////////////////////////////////////////////////////////////////////////////////////////
            } finally {
                onlineUsers.remove(name);
                socketsfromUserNames.remove(name);
                try {
                    sendOnlineUserList(true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


        private void send(Message message, Socket socket) throws IOException {
            System.out.println("Point to point message: " + message);
            OOS oos = new OOS(socket.getOutputStream());
            oos.writeObject(message);
            System.out.println("Message(P) send successfully!");
        }

        private void sendToAll(Message message, boolean removeOwn) throws IOException {
            System.out.println("To all message: " + message);
            //initialize the object output stream
            OOS oos;
            if (removeOwn) {
                //acquire Username-Socket of hashmap to traverse to send
                for (HashMap.Entry<String, Socket> entry : socketsfromUserNames.entrySet()) {
                    //send to all except oneself
                    if (!entry.getKey().equals(message.getFrom())) {
                        oos = new OOS(entry.getValue().getOutputStream());
                        oos.writeObject(message);
                    }
                }
            } else {
                //send to everyone
                for (Socket socket : socketsfromUserNames.values()) {
                    oos = new OOS(socket.getOutputStream());
                    oos.writeObject(message);
                }
            }
            System.out.println("Message(A) send successfully!");
        }

        private void checkConnect(Message message) throws IOException {
            String username = message.getFrom();
            System.out.println(username + "is coming");
            //check user name from existed username-socket list
            if (!socketsfromUserNames.containsKey(username)) {
                socketsfromUserNames.put(username, s);
                onlineUsers.add(message.getFrom());
                name = message.getFrom();
                System.out.println(onlineUsers);

                System.out.println(username + "login successfully!");
                //feedback message(success)
                Message sResult = new Message();
                sResult.setType("SUCCESS");
                sResult.setContent(username + " connect successfully!");

                send(sResult, s);
                //update list
                sendOnlineUserList(true);
                //send online notification
                sendNotification(username + " is online!");
            } else {
                System.out.println(username + " login failed!");
                //feedback message(fail)
                Message fResult = new Message();
                fResult.setType("FAIL");
                fResult.setContent(fResult.getFrom() + " is existed, connect failed!");
                //send
                send(fResult, s);
            }
        }

        private void closeConnect(Message message) throws IOException {
            String userName = message.getFrom();
            System.out.println(userName + " ready to login out!");
            Socket socket = socketsfromUserNames.get(userName);
            if (socket != null) {
                socketsfromUserNames.remove(userName);
            }
            System.out.println(userName
                + " login out successful! Ready to update online user list!");
            //update list
            onlineUsers.removeIf(temp -> temp.equals(userName));
            sendOnlineUserList(true);
            //send offline notification
            sendNotification(userName + " is offline!");
        }

        private void sendOnlineUserList(boolean isAllUsers) throws IOException {
            System.out.println("Ready to update online user list!");
            //feedback message
            Message uResult = new Message();
            uResult.setType("USERLIST");
            uResult.setUserlist(onlineUsers);
            if (isAllUsers) {
                //send to all
                sendToAll(uResult, false);
            } else {
                send(uResult, s);
            }
            System.out.println("Online user list update successfully!");
        }

        private void sendNotification(String notice) throws IOException {
            System.out.println("Ready to send notification message!");
            Message message = new Message();
            message.setType("NOTIFICATION");
            message.setContent(notice);
            sendToAll(message, false);
        }

        private void sendMSG(Message message) throws IOException {
            String userName = message.getFrom();
            String toUser = message.getTo();
            //check receiver
            if (toUser.equals("@ALL*")) {
                System.out.println(userName + " is sending to All!");
                // unnecessary to send to oneself
                sendToAll(message, true);
            } else {
                System.out.println(userName + " is sending to user!");
                for (int i = 0; i < privateChats.size(); i++) {
                    if (privateChats.get(i).user.contains(message.getFrom())
                         && privateChats.get(i).user.contains(message.getTo())) {
                        privateChats.get(i).messages.add(message);
                    }
                }
                send(message, socketsfromUserNames.get(toUser));
            }
        }

        private void sendMSGToGroup(Message message) throws IOException {
            String userName = message.getFrom();
            System.out.println(userName + " is sending to group!");

            int k = 0;
            for (int i = 0; i < groupChats.size(); i++) {
                if (groupChats.get(i).user.size() == message.getUserList().size()) {
                    int n = 0;
                    for (int j = 0; j < message.getUserList().size(); j++) {
                        if (groupChats.get(i).user.contains(message.getUserList().get(j))) {
                            n++;
                        }
                    }
                    if (n == groupChats.get(i).user.size()) {
                        k = i;
                        break;
                    }
                }
            }
            groupChats.get(k).messages.add(message);
            for (int i = 0; i < message.getUserList().size(); i++) {
                if (!message.getUserList().get(i).equals(userName)) {
                    send(message, socketsfromUserNames.get(message.getUserList().get(i)));
                }
            }
        }

        private void newPrivatechat(Message message) {
            boolean label = false;
            for (int i = 0; i < privateChats.size(); i++) {
                if (privateChats.get(i).user.contains(message.getFrom())
                    && privateChats.get(i).user.contains(message.getTo())) {
                    label = true;
                }
            }
            if (!label) {
                PrivateChat privateChat = new PrivateChat();
                privateChat.user.add(message.getFrom());
                privateChat.user.add(message.getTo());
                privateChats.add(privateChat);
                System.out.println("new private chat");
            }
        }

        private void newGroupChat(Message message) {
            boolean label = false;
            for (int i = 0; i < groupChats.size(); i++) {
                if (groupChats.get(i).user.size() == message.getUserList().size()) {
                    int n = 0;
                    for (int j = 0; j < message.getUserList().size(); j++) {
                        if (groupChats.get(i).user.contains(message.getUserList().get(j))) {
                            n++;
                        }
                    }
                    if (n == groupChats.get(i).user.size()) {
                        label = true;
                        break;
                    }
                }
            }
            if (!label) {
                GroupChat groupChat = new GroupChat();
                groupChat.user.addAll(message.getUserList());
                groupChats.add(groupChat);
                System.out.println("new group chat");
            }
        }

        private void getPrivatechat(Message message) throws IOException {
            for (int i = 0; i < privateChats.size(); i++) {
                if (privateChats.get(i).user.contains(message.getFrom())
                    && privateChats.get(i).user.contains(message.getTo())) {
                    Message message1 = new Message();
                    message1.setType("privatechat");
                    message1.setTo(message.getFrom());
                    message1.setFrom("server");
                    message1.setMessages(privateChats.get(i).messages);
                    send(message1, socketsfromUserNames.get(message.getFrom()));
                }
            }
        }

        private void getGroupchat(Message message) throws IOException {
            for (int i = 0; i < groupChats.size(); i++) {
                if (groupChats.get(i).user.size() == message.getUserList().size()) {
                    int n = 0;
                    for (int j = 0; j < message.getUserList().size(); j++) {
                        if (groupChats.get(i).user.contains(message.getUserList().get(j))) {
                            n++;
                        }
                    }
                    if (n == message.getUserList().size()) {
                        Message message1 = new Message();
                        message1.setType("groupchat");
                        message1.setTo(message.getFrom());
                        message1.setFrom("server");
                        message1.setMessages(groupChats.get(i).messages);
                        send(message1, socketsfromUserNames.get(message.getFrom()));
                    }
                }
            }
        }

        private void sendFile(Message message) throws IOException {
            send(message, socketsfromUserNames.get(message.getSendTo()));
        }
    }
}

class PrivateChat implements Serializable {
    Set<String> user = new HashSet<>();
    ArrayList<Message> messages = new ArrayList<>();
}

class GroupChat implements Serializable {
    Set<String> user = new HashSet<>();
    ArrayList<Message> messages = new ArrayList<>();
}

class User implements Serializable {
    String username;
    String passport;
}