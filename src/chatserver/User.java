package chatserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class User {
    public char[] name;
    public InetAddress address;
    public int port;
    public long lastSeen;
    public boolean isOnline;

    public User(char[] name, InetAddress ia, int port) {
        this.name = name;
        this.address = ia;
        this.port = port;
        lastSeen = java.lang.System.currentTimeMillis();
        isOnline = true;
    }

}
