/*
 * The MIT License
 *
 * Copyright 2019 njche.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package chatserver;

/**
 *
 * @author njche
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer extends Thread {

    public static ChatServer s;

    public final static int PORT = 7331;
    protected final static int BUFFER = 1024;

    protected DatagramSocket socket;

    protected ArrayList<String> existingClients;
    // user ArrayList
    protected List<User> users = new ArrayList<>();

    public ChatServer() throws IOException {
        socket = new DatagramSocket(PORT);
        existingClients = new ArrayList();
    }

    public void run() {
        byte[] buf = new byte[BUFFER];
        while (true) {
            try {
                Arrays.fill(buf, (byte) 0);
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String content = new String(buf, buf.length);
                //System.out.println(content);

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                if (content.startsWith("USERCONNECTION:")) {

                    // respond to user connection
                    connectionRequest(packet);
                    String id = clientAddress.toString() + "," + clientPort + ". " + content.split(":")[1];
                    //System.out.println(id);
                    String clientName1 = content.split(":")[1];
                    char[] clientNameChars = clientName1.toCharArray();
                    String clientNameString = new String(clientNameChars).trim().replace(" ", "");
                    clientNameChars = clientNameString.toCharArray();

                    if (!existingClients.contains(id)) {
                        System.out.println(id + " connected");

                        existingClients.add(id);
                        System.out.println("Current Time: " + java.lang.System.currentTimeMillis());

                        // add to person
                        User u = new User(id, clientNameString, clientAddress, clientPort);
                        users.add(u);

                    }

                    // refresh the online status of the user
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).id.equals(id)) {
                            users.get(i).isOnline = true;
                            users.get(i).lastSeen = java.lang.System.currentTimeMillis();
                        }
                    }
                } else if (content.startsWith("WHOSONLINE")) {
                    DatagramPacket ack = new DatagramPacket("ACK".getBytes(), "ACK".getBytes().length, packet.getAddress(), packet.getPort());
                    socket.send(ack);
                    // get a list of people who are online
                    String onlineUsers;
                    byte[] data;
                    DatagramPacket packetReply;
                    onlineUsers = "WHOSONLINE------------------------------------";
                    data = onlineUsers.getBytes();
                    packetReply = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    socket.send(packetReply);
                    onlineUsers = "WHOSONLINEOnline users:";
                    data = onlineUsers.getBytes();
                    packetReply = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    socket.send(packetReply);
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).isOnline) {
                            onlineUsers = "WHOSONLINE" + users.get(i).id;
                            data = onlineUsers.getBytes();
                            packetReply = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                            socket.send(packetReply);
                        }
                    }
                    onlineUsers = "WHOSONLINE------------------------------------";
                    data = onlineUsers.getBytes();
                    packetReply = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    socket.send(packetReply);

                } else {
                    // separate the username from the content
                    //System.out.println(content);
                    String[] contentDecoded = content.split("XXXSEPARATORXXX");
                    //System.out.println(Arrays.toString(contentDecoded));
                    try {
                        content = contentDecoded[1]; // the message body
                        //System.out.println(content);
                        String id = clientAddress.toString() + "," + clientPort + ". " + contentDecoded[0];
                        //System.out.println(id);
                        String recipient = contentDecoded[2];
                        //System.out.println(recipient);
                        // ack info
                        DatagramPacket ack = new DatagramPacket("ACK".getBytes(), "ACK".getBytes().length, packet.getAddress(), packet.getPort());
                        socket.send(ack);
                        textTransmissionRequest(id, content, packet, recipient);
                    } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    }

                }
            } catch (IOException e) {
                System.err.println(e);
            } catch (java.lang.NullPointerException e) {
                e.printStackTrace();
                System.exit(-1);
            } catch (java.lang.IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectionRequest(DatagramPacket packet) throws IOException {
        InetAddress clientIP = packet.getAddress();
        int clientPort = packet.getPort();
        packet = new DatagramPacket("RECEIVED".getBytes(), "RECEIVED".getBytes().length, clientIP, clientPort);
        socket.send(packet);
    }

    private void textTransmissionRequest(String id, String content, DatagramPacket packet, String recipient) throws IOException {
        System.out.println(id + " : " + content);
        byte[] data = (id + " : " + content).getBytes();
        //System.out.println(recipient);
        //System.out.println("all".equals("all"));
        //System.out.println(recipient.equals("all"));
        if (charArrayEquality(recipient.toCharArray(), "all".toCharArray())) {
            //System.out.println("Broadcast");
            for (int i = 0; i < users.size(); i++) {
                InetAddress cl = users.get(i).address;
                int cp = users.get(i).port;
                packet = new DatagramPacket(data, data.length, cl, cp);
                socket.send(packet);
            }
        } else {
            //System.out.println("One-to-one messsage");
            DatagramPacket packetOrigin;
            InetAddress clOrigin = packet.getAddress();
            int cpOrigin = packet.getPort();
            String msg = id + " : " + content;
            packetOrigin = new DatagramPacket(msg.getBytes(), msg.getBytes().length, clOrigin, cpOrigin);
            socket.send(packetOrigin);
            for (int i = 0; i < users.size(); i++) {
                if (charArrayEquality(recipient.toCharArray(), users.get(i).name.toCharArray())) {
                    InetAddress cl = users.get(i).address;
                    int cp = users.get(i).port;
                    packet = new DatagramPacket(data, data.length, cl, cp);
                    socket.send(packet);
                }
            }
        }
    }

    private static boolean charArrayEquality(char[] a, char[] b) {
        int length = a.length;
        if (b.length < a.length) {
            length = b.length;
        }
        boolean x = true;
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                x = false;
            }
        }
        return x;
    }

    public static void main(String args[]) throws Exception {
        s = new ChatServer();
        s.start();
        CheckOnline co = new CheckOnline();
        Thread coThread = new Thread(co);
        coThread.start();
    }

}

class CheckOnline implements Runnable {

    CheckOnline() {

    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                for (int i = 0; i < ChatServer.s.users.size(); i++) {
                    if (java.lang.System.currentTimeMillis() - ChatServer.s.users.get(i).lastSeen > 5000 && ChatServer.s.users.get(i).isOnline) {
                        ChatServer.s.users.get(i).isOnline = false;
                        System.out.println(ChatServer.s.existingClients.get(i) + " disconnected");
                    }
                }
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }

        }
    }
}
