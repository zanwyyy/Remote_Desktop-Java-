package Client;

import Include.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.round;

public class RemoteDesktopClient {
    private final Robot robot;
    private final JFrame frame;
    private final JLabel statusLabel;
    private Socket socket;
    private ServerSocket mouseServerSocket;
    private ServerSocket keyboardServerSocket;
    private ServerSocket screenServerSocket;
    private final ImageUtils img;
    private final Rectangle screenRect;
    private final AtomicInteger frameIdCounter = new AtomicInteger(0);
    private final Fragment fragment;
    private volatile boolean sendingScreen = false;
    private ObjectOutputStream out;

    public RemoteDesktopClient() throws AWTException, IOException {
        // Khởi tạo UI
        frame = new JFrame("Client - Waiting for Connection");
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        statusLabel = new JLabel("Waiting for connection...", SwingConstants.CENTER);
        frame.add(statusLabel);

        frame.setVisible(true);
        img = new ImageUtils();
        screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        fragment = new Fragment();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
        // Kết nối đến server
        connectToServer();
    }

    private void listenForCommands() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String command;
            while ((command = reader.readLine()) != null) {
                System.out.println("Received command from server: " + command);
                if (command.equalsIgnoreCase("start")) {
                    if (!sendingScreen) {
                        sendingScreen = true;
                        // Mở các ServerSocket cho chuột, bàn phím và màn hình
                        try {
                            mouseServerSocket = new ServerSocket(1236);
                            keyboardServerSocket = new ServerSocket(1237);
                            screenServerSocket = new ServerSocket(1238); // Cổng cho màn hình

                            // Khởi động các luồng để nhận dữ liệu chuột, bàn phím và màn hình
                            new Thread(this::listenForMouseEvents).start();
                            new Thread(this::listenForKeyboardEvents).start();
                            new Thread(this::sendScreenData).start();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Started sending screen data.");
                    }
                } else if (command.equalsIgnoreCase("stop")) {
                    sendingScreen = false;
                    System.out.println("Stopped sending screen data.");
                    // Đóng các kết nối socket nếu cần
                    closeSockets();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSockets() {
        try {
            if (mouseServerSocket != null && !mouseServerSocket.isClosed()) {
                mouseServerSocket.close();
                System.out.println("Mouse ServerSocket closed.");
            }
            if (keyboardServerSocket != null && !keyboardServerSocket.isClosed()) {
                keyboardServerSocket.close();
                System.out.println("Keyboard ServerSocket closed.");
            }
            if (screenServerSocket != null && !screenServerSocket.isClosed()) {
                screenServerSocket.close();
                System.out.println("Screen ServerSocket closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer() {
        while (true) {
            try {
                InetAddress serverAddress = InetAddress.getByName("192.168.2.2"); // Thay đổi nếu cần
                System.out.println("Trying to connect to server...");
                socket = new Socket(serverAddress, 1234); // Kết nối đến server
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                statusLabel.setText("Connected to server!");
                break; // Thoát khỏi vòng lặp nếu kết nối thành công
            } catch (IOException e) {
                statusLabel.setText("Failed to connect, retrying...");
                System.out.println("Failed to connect, retrying in 5 seconds...");
                try {
                    Thread.sleep(5000); // Đợi 5 giây trước khi thử lại
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        // Bắt đầu lắng nghe lệnh từ server
        new Thread(this::listenForCommands).start();
    }

    private void listenForMouseEvents() {
        try {
            Socket mouseSocket = mouseServerSocket.accept();
            System.out.println("Mouse socket connected.");

            // Đọc dữ liệu liên tục từ mouseSocket
            ObjectInputStream mouse = new ObjectInputStream(mouseSocket.getInputStream());
            while (true) {
                try {
                    Object obj = mouse.readObject();
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    if (obj instanceof Mouse) {
                        Mouse mouseEvent = (Mouse) obj;
                        double scaleX = screenRect.width / mouseEvent.getWidth();
                        double scaleY = screenRect.height / mouseEvent.getHeight();
                        double adjustedX = (mouseEvent.getX() * scaleX);
                        double adjustedY = (mouseEvent.getY() * scaleY);
                        mouseEvent.setX(adjustedX);
                        mouseEvent.setY(adjustedY);
                        handleMouseEvent(mouseEvent);
                        System.out.println("Received mouse event: " + mouseEvent);
                    } else if (obj instanceof MouseWheel) {
                        MouseWheel mouseWheel = (MouseWheel) obj;
                        double scaleX = screenRect.width / mouseWheel.getScreenWidth();
                        double scaleY = screenRect.height / mouseWheel.getScreenHeight();
                        double adjustedX = (mouseWheel.getX() * scaleX);
                        double adjustedY = (mouseWheel.getY() * scaleY);
                        mouseWheel.setX(adjustedX);
                        mouseWheel.setY(adjustedY);
                        handleMouseWheelEvent(mouseWheel);
                        System.out.println("Received mouse event: " + mouseWheel);
                    }
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleMouseWheelEvent(MouseWheel mouseWheelEvent) {
        try {
            // Di chuyển chuột đến vị trí nhận được
            int x = Math.toIntExact(round(mouseWheelEvent.getX()));
            int y = Math.toIntExact(round(mouseWheelEvent.getY()));
            robot.mouseMove(x, y);

            // Thực hiện cuộn chuột
            robot.mouseWheel(mouseWheelEvent.getWheelRotation());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMouseEvent(Mouse mouseEvent) {
        // Di chuyển chuột đến vị trí nhận được
        int x = Math.toIntExact(round(mouseEvent.getX()));
        int y = Math.toIntExact(round(mouseEvent.getY()));
        robot.mouseMove(x, y);
        // Xử lý các sự kiện nhấn chuột
        if (mouseEvent.getEventID() == MouseEvent.MOUSE_PRESSED) {
            int buttonMask = getMouseButtonMask(mouseEvent.getButton());
            robot.mousePress(buttonMask);
        } else if (mouseEvent.getEventID() == MouseEvent.MOUSE_RELEASED) {
            int buttonMask = getMouseButtonMask(mouseEvent.getButton());
            robot.mouseRelease(buttonMask);
        }
    }

    // Xác định buttonMask cho các nút chuột trái, phải, giữa
    private int getMouseButtonMask(int button) {
        switch (button) {
            case MouseEvent.BUTTON1: // Nút trái
                return InputEvent.BUTTON1_DOWN_MASK;
            case MouseEvent.BUTTON2: // Nút giữa
                return InputEvent.BUTTON2_DOWN_MASK;
            case MouseEvent.BUTTON3: // Nút phải
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                return 0;
        }
    }

    private void listenForKeyboardEvents() {
        try {
            Socket keyboardSocket = keyboardServerSocket.accept();
            System.out.println("Keyboard socket connected.");

            // Đọc dữ liệu liên tục từ keyboardSocket
            ObjectInputStream keyboard = new ObjectInputStream(keyboardSocket.getInputStream());
            while (true) {
                try {
                    Keyboard keyEvent = (Keyboard) keyboard.readObject();
                    handleKeyEvent(keyEvent);
                    System.out.println("Received key event: " + keyEvent);
                } catch (EOFException e) {
                    // Kết thúc luồng khi socket đóng
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleKeyEvent(Keyboard keyEvent) {
        int keyCode = keyEvent.getKeyCode();

        // Nhấn phím
        if (keyEvent.getEventID() == KeyEvent.KEY_PRESSED) {
            robot.keyPress(keyCode);
        }
        // Thả phím
        else if (keyEvent.getEventID() == KeyEvent.KEY_RELEASED) {
            robot.keyRelease(keyCode);
        }
    }

    private void sendScreenData() {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("192.168.2.2"); // Địa chỉ server
            while (true) { // Kiểm tra cờ để gửi dữ liệu
                // Chụp ảnh màn hình
                BufferedImage screenImage = robot.createScreenCapture(screenRect);

                // Giảm độ phân giải nếu cần
                BufferedImage resizedImage = img.resizeImage(screenImage, screenRect.width / 2, screenRect.height / 2);

                // Nén ảnh
                byte[] screenBytes = img.compressImage(resizedImage, 0.5f); // Chọn mức nén phù hợp
                System.out.println("Compressed screen bytes size: " + screenBytes.length);

                // Tăng Frame ID
                int frameId = frameIdCounter.getAndIncrement();

                // Chia nhỏ dữ liệu thành các gói tin nhỏ
                fragment.fragmentUDP(screenBytes, frameId, address, 1238, udpSocket);

                System.out.println("Đã gửi Frame ID: " + frameId);

                // Điều chỉnh tần suất gửi
                Thread.sleep(100); // 10 fps
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Main method để chạy client
    public static void main(String[] args) throws AWTException, IOException {
        new RemoteDesktopClient();
    }
}
