package Server;

import Include.KeyboardHandler;
import Include.MouseHandler;
// Đảm bảo đã import lớp Screen

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class ClientHandler {
    private JLabel screenLabel;  // Dùng để hiển thị màn hình từ client

    public void handleClient(Socket client) {
        JFrame displayFrame = new JFrame("Displaying Client Screen - " + client.getInetAddress().getHostAddress());
        displayFrame.setSize(800, 600);
        displayFrame.setLayout(new BorderLayout());
        displayFrame.setVisible(true);
        displayFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        screenLabel = new JLabel();  // JLabel để hiển thị ảnh màn hình
        displayFrame.add(screenLabel, BorderLayout.CENTER);  // Thêm JLabel vào frame
        System.out.println(screenLabel.getHeight());
        System.out.println(screenLabel.getWidth());
        try {
            // Tạo các socket để gửi sự kiện chuột và bàn phím
            System.out.println("Kết nối tới socket chuột và bàn phím...");
            Socket mouseSocket = new Socket(client.getInetAddress(), 1236); // Socket chuột
            Socket keyboardSocket = new Socket(client.getInetAddress(), 1237); // Socket bàn phím

            // Gắn listener cho frame
            MouseHandler mouseHandler = new MouseHandler(mouseSocket);
            screenLabel.addMouseListener(mouseHandler);
            screenLabel.addMouseMotionListener(mouseHandler); // Gắn sự kiện di chuyển chuột
            displayFrame.addKeyListener(new KeyboardHandler(keyboardSocket)); // Gắn sự kiện bàn phím

            System.out.println("Đã kết nối thành công tới socket chuột và bàn phím.");

            // Bắt đầu nhận dữ liệu màn hình
            new Thread(() -> receiveScreenData(1238)).start();  // Chạy luồng để nhận ảnh màn hình

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(displayFrame, "Không thể kết nối tới client. Vui lòng kiểm tra lại!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Hàm nhận và hiển thị dữ liệu màn hình từ client
    private void receiveScreenData(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[65507]; // Kích thước tối đa của UDP
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                byte[] screenBytes = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, screenBytes, 0, packet.getLength());

                // Chuyển đổi byte[] thành BufferedImage
                BufferedImage screenImage = ImageIO.read(new ByteArrayInputStream(screenBytes));

                if (screenImage != null) {
                    // Hiển thị hình ảnh lên JLabel
                    ImageIcon icon = new ImageIcon(screenImage);
                    screenLabel.setIcon(icon);
                    screenLabel.repaint();
                }

                System.out.println("Đã nhận dữ liệu màn hình!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
