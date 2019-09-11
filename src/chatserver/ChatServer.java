/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    public final static int PORT = 8080;
    private final static int BUFFER = 1024;

    private DatagramSocket socket;
    private ArrayList<InetAddress> clientAddresses;
    private ArrayList<Integer> clientPorts;
    private ArrayList<char[]> clientNames;
    protected ArrayList<String> existingClients;
    protected ArrayList<Boolean> clientOnline;
    protected ArrayList<Long> clientLastSeen;

    public ChatServer() throws IOException {
        socket = new DatagramSocket(PORT);
        clientAddresses = new ArrayList();
        clientPorts = new ArrayList();
        clientNames = new ArrayList();
        existingClients = new ArrayList();
        clientOnline = new ArrayList();
        clientLastSeen = new ArrayList();
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

                    if (!existingClients.contains(id)) {
                        System.out.println(id + " connected");
                        existingClients.add(id);
                        clientPorts.add(clientPort);
                        clientAddresses.add(clientAddress);
                        clientNames.add(clientNameChars);
                        clientOnline.add(true);
                        clientLastSeen.add(java.lang.System.currentTimeMillis());
                        System.out.println("Current Time: " + java.lang.System.currentTimeMillis());
                    }

                    // refresh the online status of the user
                    for (int i = 0; i < existingClients.size(); i++) {
                        if (existingClients.get(i).equals(id)) {
                            clientOnline.set(i, true);
                            clientLastSeen.set(i, java.lang.System.currentTimeMillis());
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
                    for (int i = 0; i < existingClients.size(); i++) {
                        if (clientOnline.get(i)) {
                            onlineUsers = "WHOSONLINE" + existingClients.get(i);
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
            for (int i = 0; i < clientAddresses.size(); i++) {
                InetAddress cl = clientAddresses.get(i);
                int cp = clientPorts.get(i);
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
            for (int i = 0; i < clientAddresses.size(); i++) {
                if (charArrayEquality(recipient.toCharArray(), clientNames.get(i))) {
                    InetAddress cl = clientAddresses.get(i);
                    int cp = clientPorts.get(i);
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
                for (int i = 0; i < ChatServer.s.existingClients.size(); i++) {
                    if (java.lang.System.currentTimeMillis() - ChatServer.s.clientLastSeen.get(i) > 5000 && ChatServer.s.clientOnline.get(i) == true) {
                        ChatServer.s.clientOnline.set(i, false);
                        System.out.println(ChatServer.s.existingClients.get(i) + " disconnected");
                    }
                }
            } catch (InterruptedException ex) {
                System.err.println(ex);
            }

        }
    }
}
