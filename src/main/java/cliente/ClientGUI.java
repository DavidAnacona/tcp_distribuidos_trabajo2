package cliente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientGUI {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton, connectButton, disconnectButton, updateListButton;
    private JCheckBox privateMessageCheckbox;
    private JComboBox<String> clientListComboBox;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;
    private boolean connected = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    public ClientGUI() {
        frame = new JFrame("Cliente TCP");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField(30);
        sendButton = new JButton("Enviar");
        connectButton = new JButton("Conectar");
        disconnectButton = new JButton("Desconectar");
        updateListButton = new JButton("Actualizar Lista");
        privateMessageCheckbox = new JCheckBox("Mensaje privado");
        clientListComboBox = new JComboBox<>();
        
        sendButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        clientListComboBox.setEnabled(false);

        sendButton.addActionListener(e -> sendMessage());
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        updateListButton.addActionListener(e -> updateClientList());

        privateMessageCheckbox.addActionListener(e -> {
            clientListComboBox.setEnabled(privateMessageCheckbox.isSelected());
        });

        JPanel panel = new JPanel();
        panel.add(inputField);
        panel.add(sendButton);
        panel.add(connectButton);
        panel.add(disconnectButton);
        panel.add(privateMessageCheckbox);
        panel.add(clientListComboBox);
        panel.add(updateListButton);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1234);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            clientName = JOptionPane.showInputDialog(frame, "Ingresa tu nombre:");
            out.println(clientName);

            new Thread(new ReaderHandler()).start();
            sendButton.setEnabled(true);
            disconnectButton.setEnabled(true);
            connectButton.setEnabled(false);
            messageArea.append("Conectado al servidor.\n");
        } catch (IOException e) {
            messageArea.append("Error al conectarse al servidor: " + e.getMessage() + "\n");
        }
    }

    private void disconnectFromServer() {
        try {
            out.println("salir");
            socket.close();
            sendButton.setEnabled(false);
            disconnectButton.setEnabled(false);
            connectButton.setEnabled(true);
            messageArea.append("Desconectado del servidor.\n");
        } catch (IOException e) {
            messageArea.append("Error al desconectarse: " + e.getMessage() + "\n");
        }
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            if (privateMessageCheckbox.isSelected()) {
                String selectedClient = (String) clientListComboBox.getSelectedItem();
                if (selectedClient != null) {
                    out.println("privado:" + selectedClient + ":" + message);
                } else {
                    messageArea.append("No se ha seleccionado ningún cliente.\n");
                }
            } else {
                out.println(message);  // Enviar a todos
            }
            inputField.setText("");
        }
    }

    private void updateClientList() {
        // Enviar solicitud al servidor para obtener la lista de clientes
        out.println("clientes");
    }

    class ReaderHandler implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.startsWith("clientes:")) {
                        updateClientListComboBox(serverMessage.substring(9));
                    } else {
                        messageArea.append(serverMessage + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateClientListComboBox(String clientList) {
        String[] clients = clientList.split(",");
        clientListComboBox.removeAllItems();
        for (String client : clients) {
            if (!client.equals(clientName)) { // No añadir el propio cliente
                clientListComboBox.addItem(client);
            }
        }
        messageArea.append("Lista de clientes actualizada.\n");
    }
}
