package Server;

import Include.ImageUtils;
import Include.KeyboardHandler;
import Include.MouseHandler;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ClientHandler {
    private JLabel screenLabel;  // Dùng để hiển thị màn hình từ client
    private Map<Integer, FrameBuffer> frameBuffers = new HashMap<>();
    private ImageUtils utils;
    private BufferedWriter out;
    private JFrame displayFrame;

    // Thông tin về retry
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 2000; // 2 giây
    private volatile boolean receiving = true;
    public void handleClient(Socket client) throws IOException {
        // Tạo giao diện để hiển thị màn hình client
        displayFrame = new JFrame("Displaying Client Screen - " + client.getInetAddress().getHostAddress());
        displayFrame.setSize(800, 600);
        displayFrame.setLayout(new BorderLayout());
        displayFrame.setVisible(true);
        displayFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Thêm WindowListener để gửi lệnh "stop" khi đóng cửa sổ
        displayFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Gửi lệnh "stop" đến client
                sendCommand("stop");
                // Đóng các tài nguyên liên quan
                closeResources();
            }
        });

        // Khởi tạo BufferedWriter để gửi lệnh đến client
        out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

        // Tạo JLabel để hiển thị ảnh màn hình
        screenLabel = new JLabel();
        utils = new ImageUtils();
        displayFrame.add(screenLabel, BorderLayout.CENTER);

        // Gửi lệnh "start" đến client
        sendCommand("start");
        System.out.println("Sent 'start' command to client.");

        try {
            // Thêm một chút thời gian để client mở các ServerSocket
            Thread.sleep(1000); // 1 giây (có thể điều chỉnh)

            // Kết nối tới socket chuột và bàn phím với retry logic
            Socket mouseSocket = connectWithRetry(client.getInetAddress(), 1236, MAX_RETRIES, RETRY_DELAY_MS);
            Socket keyboardSocket = connectWithRetry(client.getInetAddress(), 1237, MAX_RETRIES, RETRY_DELAY_MS);

            if (mouseSocket != null && keyboardSocket != null) {
                // Gắn listener cho frame
                MouseHandler mouseHandler = new MouseHandler(mouseSocket);
                screenLabel.addMouseListener(mouseHandler);
                screenLabel.addMouseMotionListener(mouseHandler); // Gắn sự kiện di chuyển chuột
                screenLabel.addMouseWheelListener(mouseHandler);
                displayFrame.addKeyListener(new KeyboardHandler(keyboardSocket)); // Gắn sự kiện bàn phím

                System.out.println("Đã kết nối thành công tới socket chuột và bàn phím.");

                // Bắt đầu nhận dữ liệu màn hình
                new Thread(() -> receiveScreenData(1238)).start();  // Chạy luồng để nhận ảnh màn hình
            } else {
                JOptionPane.showMessageDialog(displayFrame, "Không thể kết nối tới các socket chuột và bàn phím của client sau " + MAX_RETRIES + " lần thử.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Phương thức gửi lệnh đến client
    public void sendCommand(String command) {
        try {
            out.write(command);
            out.newLine();
            out.flush();
            System.out.println("Sent command to client: " + command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Phương thức kết nối với retry logic
    private Socket connectWithRetry(InetAddress address, int port, int maxRetries, int delayMillis) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("Attempt " + attempt + " to connect to " + address.getHostAddress() + ":" + port);
                Socket socket = new Socket(address, port);
                System.out.println("Connected to " + address.getHostAddress() + ":" + port);
                return socket;
            } catch (IOException e) {
                System.out.println("Connection attempt " + attempt + " to " + address.getHostAddress() + ":" + port + " failed.");
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        return null; // Không kết nối được sau maxRetries lần thử
    }

    // Hàm nhận và hiển thị dữ liệu màn hình từ client
    private void receiveScreenData(int port) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(5000); // Đặt timeout để kiểm tra flag
            byte[] buffer = new byte[1400]; // Kích thước tối đa của gói tin UDP
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (receiving) {
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    // Kiểm tra lại flag receiving
                    if (!receiving) {
                        break;
                    }
                    continue; // Tiếp tục vòng lặp
                }
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
                    BufferedImage resizeImage = utils.resizeImage(screenImage, 800, 600);
                    if (screenImage != null) {
                        // Hiển thị hình ảnh lên JLabel
                        ImageIcon icon = new ImageIcon(resizeImage);
                        screenLabel.setIcon(icon);
                        screenLabel.repaint();
                    }

                    System.out.println("Đã tái tạo và hiển thị Frame ID: " + frameId);

                    // Xóa FrameBuffer sau khi hoàn thành
                    frameBuffers.remove(frameId);
                }
            }
            System.out.println("Stopped receiving screen data.");
        } catch (Exception e) {
            if (receiving) { // Chỉ in stack trace nếu đang nhận dữ liệu
                e.printStackTrace();
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("UDP Socket closed.");
            }
        }
    }

    // Lớp FrameBuffer để tái tạo dữ liệu từ nhiều gói tin UDP
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

    private void closeResources() {
        try {
            if (out != null) {
                out.close();
            }
            // Đặt receiving thành false để thoát khỏi receiveScreenData
            receiving = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}