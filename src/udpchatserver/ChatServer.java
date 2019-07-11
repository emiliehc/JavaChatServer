/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udpchatserver;

/**
 *
 * @author njche
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer extends Thread {

    public final static int PORT = 7331;
    private final static int BUFFER = 1024;

    private DatagramSocket socket;
    private ArrayList<InetAddress> clientAddresses;
    private ArrayList<Integer> clientPorts;
    private ArrayList<char[]> clientNames;
    private HashSet<String> existingClients;

    public ChatServer() throws IOException {
        socket = new DatagramSocket(PORT);
        clientAddresses = new ArrayList();
        clientPorts = new ArrayList();
        clientNames = new ArrayList();
        existingClients = new HashSet();
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
                    System.out.println(id);
                    String clientName1 = content.split(":")[1];
                    char[] clientNameChars = clientName1.toCharArray();
                    
                    if (!existingClients.contains(id)) {
                        existingClients.add(id);
                        clientPorts.add(clientPort);
                        clientAddresses.add(clientAddress);
                        clientNames.add(clientNameChars);
                    }
                    
                } else {
                    // separate the username from the content
                    //System.out.println(content);
                    String[] contentDecoded = content.split("XXXSEPARATORXXX");
                    //System.out.println(Arrays.toString(contentDecoded));
                    content = contentDecoded[1]; // the message body
                    //System.out.println(content);
                    String id = clientAddress.toString() + "," + clientPort + ". " + contentDecoded[0];
                    //System.out.println(id);
                    String recipient = contentDecoded[2];
                    System.out.println(recipient);
                    textTransmissionRequest(id, content, packet, recipient);
                }
            } catch (Exception e) {
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
            System.out.println("Broadcast");
            for (int i = 0; i < clientAddresses.size(); i++) {
                InetAddress cl = clientAddresses.get(i);
                int cp = clientPorts.get(i);
                packet = new DatagramPacket(data, data.length, cl, cp);
                socket.send(packet);
            }
        } else {
            System.out.println("One-to-one messsage");
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
        ChatServer s = new ChatServer();
        s.start();
    }
    
}
