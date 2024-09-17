package servidor;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static boolean running = true;
    private static Map<String, ClientHandler> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado en el puerto " + PORT);

        while (running) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            new Thread(clientHandler).start();
        }
        serverSocket.close();
    }

    public static synchronized void broadcast(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients.values()) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    public static synchronized void sendToClient(String clientName, String message, ClientHandler sender) {
        ClientHandler recipient = clients.get(clientName);
        if (recipient != null && recipient != sender) {
            recipient.sendMessage(message);
            sender.sendMessage("Mensaje enviado a " + clientName);
        } else {
            sender.sendMessage("Cliente no encontrado.");
        }
    }

    public static synchronized void addClient(String clientName, ClientHandler clientHandler) {
        clients.put(clientName, clientHandler);
        broadcast("El cliente " + clientName + " se ha conectado.", null);
    }

    public static synchronized void removeClient(String clientName) {
        clients.remove(clientName);
        broadcast("El cliente " + clientName + " se ha desconectado.", null);
    }

    public static synchronized String getClientList() {
        return String.join(",", clients.keySet());
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            out.println("Por favor, ingresa tu nombre:");
            clientName = in.readLine();
            Server.addClient(clientName, this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("salir")) {
                    break;
                } else if (message.startsWith("privado:")) {
                    String[] splitMessage = message.split(":", 3);
                    String targetClient = splitMessage[1];
                    String privateMessage = splitMessage[2];
                    Server.sendToClient(targetClient, clientName + " (privado): " + privateMessage, this);
                } else if (message.equals("clientes")) {
                    out.println("clientes:" + Server.getClientList());
                } else {
                    Server.broadcast(clientName + ": " + message, this);
                }
            }

            in.close();
            out.close();
            socket.close();
            Server.removeClient(clientName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
