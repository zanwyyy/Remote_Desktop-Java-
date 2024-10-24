package Server;

import Include.ImageUtils;
import Include.KeyboardHandler;
import Include.MouseHandler;
import Include.MyScreen;  // Đảm bảo đã import lớp Screen

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
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

        try {
            // Tạo các socket để gửi sự kiện chuột và bàn phím
            System.out.println("Kết nối tới socket chuột và bàn phím...");
            Socket mouseSocket = new Socket(client.getInetAddress(), 1236); // Socket chuột
            Socket keyboardSocket = new Socket(client.getInetAddress(), 1237); // Socket bàn phím
            Socket screenSocket = new Socket(client.getInetAddress(), 1238);  // Socket để nhận dữ liệu màn hình

            // Gắn listener cho frame
            MouseHandler mouseHandler = new MouseHandler(mouseSocket);
            displayFrame.addMouseListener(mouseHandler);
            displayFrame.addMouseMotionListener(mouseHandler); // Gắn sự kiện di chuyển chuột
            displayFrame.addKeyListener(new KeyboardHandler(keyboardSocket)); // Gắn sự kiện bàn phím

            System.out.println("Đã kết nối thành công tới socket chuột và bàn phím.");

            // Bắt đầu nhận dữ liệu màn hình
            new Thread(() -> receiveScreenData(screenSocket)).start();  // Chạy luồng để nhận ảnh màn hình

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(displayFrame, "Không thể kết nối tới client. Vui lòng kiểm tra lại!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Hàm nhận và hiển thị dữ liệu màn hình từ client
    private void receiveScreenData(Socket screenSocket) {
        try (ObjectInputStream screenIn = new ObjectInputStream(screenSocket.getInputStream())) {
            while (true) {
                MyScreen myScreenData = (MyScreen) screenIn.readObject();
                BufferedImage screenImage = myScreenData.getScreenData();
                ImageUtils imageUtils = new ImageUtils();
                BufferedImage resizedImage = imageUtils.resizeImage(screenImage, 750, 550);

                // Tính toán tỉ lệ
                double scaleX = (double) resizedImage.getWidth() / myScreenData.getWidth();
                double scaleY = (double) resizedImage.getHeight() / myScreenData.getHeight();

                // Xử lý hiển thị hình ảnh
                if (screenImage != null) {
                    ImageIcon icon = new ImageIcon(resizedImage);
                    screenLabel.setIcon(icon);
                    screenLabel.revalidate();
                    screenLabel.repaint();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
