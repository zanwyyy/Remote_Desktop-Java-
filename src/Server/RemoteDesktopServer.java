package Server;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class RemoteDesktopServer {
    private JFrame frame;
    private JPanel clientPanel;
    private ArrayList<Socket> clients = new ArrayList<>();
    private ArrayList<JButton> clientButtons = new ArrayList<>(); // Danh sách các nút
    private Socket selectedClient;
    private JLabel selectedClientLabel;
    private ClientHandler clientHandler;
    private String address = "192.168.2.2"; // Địa chỉ IP của server (có thể để "0.0.0.0" để nghe trên tất cả các giao diện)

    public RemoteDesktopServer() {
        // Khởi tạo UI
        frame = new JFrame("Server - Connected Clients");
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        clientPanel = new JPanel();
        clientPanel.setLayout(new BoxLayout(clientPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(clientPanel);

        JButton selectClientButton = new JButton("Select Client");
        selectClientButton.setEnabled(false);
        selectClientButton.addActionListener(e -> {
            if (selectedClient != null) {
                clientHandler = new ClientHandler();
                try {
                    clientHandler.handleClient(selectedClient);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Không thể xử lý client: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    return;
                }
                selectClientButton.setEnabled(false); // Vô hiệu hóa nút sau khi chọn
                selectedClientLabel.setText("Selected Client: " + selectedClient.getInetAddress().getHostAddress() + " (Connecting)");
            }
        });

        selectedClientLabel = new JLabel("Selected Client: None");

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(selectClientButton, BorderLayout.SOUTH);
        frame.add(selectedClientLabel, BorderLayout.NORTH);

        frame.setVisible(true);

        // Lắng nghe kết nối từ các client
        listenForClients(selectClientButton);
    }

    private void listenForClients(JButton selectClientButton) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(1234, 50, InetAddress.getByName(address))) {
                System.out.println("Server is listening on port 1234...");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    clients.add(clientSocket);
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    addClientButton(clientIP, clientSocket, selectClientButton);
                    checkClientConnection(clientSocket, clientIP);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Hàm thêm nút cho mỗi client kết nối
    private void addClientButton(String clientIP, Socket clientSocket, JButton selectClientButton) {
        JButton clientButton = new JButton(clientIP);
        clientButtons.add(clientButton); // Thêm vào danh sách nút

        clientButton.addActionListener(e -> {
            selectedClient = clientSocket;
            selectedClientLabel.setText("Selected Client: " + clientIP + " (Connecting)");
            selectClientButton.setEnabled(true);
        });

        clientPanel.add(clientButton);
        clientPanel.revalidate();
        clientPanel.repaint();
    }

    // Hàm kiểm tra xem client còn kết nối không
    private void checkClientConnection(Socket clientSocket, String clientIP) {
        new Thread(() -> {
            try {
                while (true) {
                    if (clientSocket.isClosed() || clientSocket.getInputStream().read() == -1) {
                        // Client ngắt kết nối, xóa nút của client này
                        removeClientButton(clientIP);
                        clients.remove(clientSocket);
                        break;
                    }
                    Thread.sleep(1000); // Kiểm tra mỗi giây
                }
            } catch (IOException | InterruptedException e) {
                // Xử lý khi client mất kết nối hoặc bị lỗi
                removeClientButton(clientIP);
                clients.remove(clientSocket);
            }
        }).start();
    }

    // Hàm xóa nút khi client mất kết nối
    private void removeClientButton(String clientIP) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < clientButtons.size(); i++) {
                JButton button = clientButtons.get(i);
                if (button.getText().equals(clientIP)) {
                    clientPanel.remove(button);
                    clientButtons.remove(i);
                    clientPanel.revalidate();
                    clientPanel.repaint();
                    break;
                }
            }
        });
    }

    public static void main(String[] args) {
        new RemoteDesktopServer();
    }
}
