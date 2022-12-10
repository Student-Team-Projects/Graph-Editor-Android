package com.example.graph_editor.client;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket;
    private DataInputStream socketInput;
    private DataOutputStream socketOutput;
    private static final int bufsize = 4 * 1024;

    public Client(String ip) throws IOException {
        socket = new Socket(ip, 5000);
        socketInput = new DataInputStream(socket.getInputStream());
        socketOutput = new DataOutputStream(socket.getOutputStream());
    }

    // TO DO exception should actually be handled
    public void getPlugin(String name) throws IOException {
        socketOutput.writeUTF("get");
        socketOutput.writeUTF(name);
        
        // what to do if the directory already exists?
        new File(name).mkdirs();
        int fileCount = socketInput.readInt();
        for(int i = 0; i < fileCount; i++) {
            String filename = socketInput.readUTF();
            new File(name + '/' + filename).createNewFile();
            FileOutputStream stream = new FileOutputStream(name + '/' + filename);

            long size = socketInput.readLong(); // get the size of the file
            byte[] buffer = new byte[Client.bufsize];
            int bytes = 0;
            while(size > 0 && (bytes = socketInput.read(buffer, 0 , (int)Math.min(buffer.length, size))) != -1) {
                stream.write(buffer, 0, bytes);
                size -= bytes;
            }
            stream.close();
        }
    }

    public void getList() throws IOException {
        socketOutput.writeUTF("list");
        int numOfPlugins = socketInput.readInt();
        for(int i = 0; i < numOfPlugins; i++) {
            System.out.println(socketInput.readUTF());
        }
    }

    public void disconnect() throws IOException {
        // this might not be the best place to do that
        socketOutput.writeUTF("bye");
        socket.close();
        socketInput.close();
        socketOutput.close();
    }

    public static void main(String[] argv) {
        try {
            Client client = new Client("20.0.120.235");
            client.getList();
            client.getPlugin("Dummy");
            client.disconnect();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
