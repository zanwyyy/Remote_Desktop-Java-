package Server;

import Include.ImageUtils;
import Include.KeyboardHandler;
import Include.MouseHandler;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler {
    private JLabel screenLabel;  // Dùng để hiển thị màn hình từ client
    private Map<Integer, FrameBuffer> frameBuffers = new HashMap<>();
    private ImageUtils utils;
    public void handleClient(Socket client) {
        JFrame displayFrame = new JFrame("Displaying Client Screen - " + client.getInetAddress().getHostAddress());
        displayFrame.setSize(800, 600);
        displayFrame.setLayout(new BorderLayout());
        displayFrame.setVisible(true);
        displayFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        screenLabel = new JLabel();  // JLabel để hiển thị ảnh màn hình
        utils = new ImageUtils();
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
            screenLabel.addMouseWheelListener(mouseHandler);
            displayFrame.addKeyListener(new KeyboardHandler(keyboardSocket)); // Gắn sự kiện bàn phím

            System.out.println("Đã kết nối thành công tới socket chuột và bàn phím.");

            // Bắt đầu nhận dữ liệu màn hình
            new Thread(() -> receiveScreenData(1238)).start();  // Chạy luồng để nhận ảnh màn hình

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(displayFrame, "Không thể kết nối tới client. Vui lòng kiểm tra lại!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public static class FrameBuffer {
        private short totalPackets;
        private Map<Short, byte[]> packetsReceived;

        public FrameBuffer(short totalPackets) {
            this.totalPackets = totalPackets;
            this.packetsReceived = new HashMap<>();
        }

        public void addPacket(short packetNum, byte[] data) {
            packetsReceived.put(packetNum, data);
        }

        public boolean isComplete() {
            return packetsReceived.size() == totalPackets;
        }

        public byte[] getPacketData(int packetNum) {
            return packetsReceived.get((short) packetNum);
        }
    }
    // Hàm nhận và hiển thị dữ liệu màn hình từ client
    private void receiveScreenData(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1400]; // Kích thước tối đa của gói tin UDP
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                byte[] packetData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, packetData, 0, packet.getLength());

                // Đọc header
                ByteBuffer byteBuffer = ByteBuffer.wrap(packetData);
                int frameId = byteBuffer.getInt();
                short totalPackets = byteBuffer.getShort();
                short packetNum = byteBuffer.getShort();

                // Lấy dữ liệu hình ảnh
                byte[] imageData = new byte[packetData.length - 8];
                System.arraycopy(packetData, 8, imageData, 0, imageData.length);

                // Lưu dữ liệu vào FrameBuffer
                FrameBuffer frameBuffer = frameBuffers.get(frameId);
                if (frameBuffer == null) {
                    frameBuffer = new FrameBuffer(totalPackets);
                    frameBuffers.put(frameId, frameBuffer);
                }
                frameBuffer.addPacket(packetNum, imageData);

                // Kiểm tra nếu đã nhận đủ gói tin để tái tạo
                if (frameBuffer.isComplete()) {
                    // Tái tạo mảng byte hoàn chỉnh
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (int i = 0; i < totalPackets; i++) {
                        baos.write(frameBuffer.getPacketData(i));
                    }
                    byte[] fullImageData = baos.toByteArray();
                    // Chuyển đổi byte[] thành BufferedImage
                    BufferedImage screenImage = ImageIO.read(new ByteArrayInputStream(fullImageData));
                    BufferedImage resizeimage = utils.resizeImage(screenImage,800,600);
                    if (screenImage != null) {
                        // Hiển thị hình ảnh lên JLabel
                        ImageIcon icon = new ImageIcon(resizeimage);
                        screenLabel.setIcon(icon);
                        screenLabel.repaint();
                    }

                    System.out.println("Đã tái tạo và hiển thị Frame ID: " + frameId);

                    // Xóa FrameBuffer sau khi hoàn thành
                    frameBuffers.remove(frameId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
