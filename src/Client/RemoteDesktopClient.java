package Client;

import Include.ImageUtils;
import Include.Keyboard;
import Include.Mouse;
import Include.MyScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import static java.lang.Math.round;

public class RemoteDesktopClient {
    private final Robot robot;
    private final JFrame frame;
    private final JLabel statusLabel;
    private Socket socket;
    private ServerSocket mouseServerSocket;
    private ServerSocket keyboardServerSocket;
    private ServerSocket screenServerSocket;
    private ImageUtils img;
    private Rectangle screenRect;
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

        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
        // Kết nối đến server
        connectToServer();
    }

    private void connectToServer() {
        while (true) {
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                System.out.println("Trying to connect to server  " );
                socket = new Socket("192.168.2.2", 1234); // Kết nối đến server
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

        // Mở các ServerSocket cho chuột, bàn phím và màn hình
        try {
            mouseServerSocket = new ServerSocket(1236);
            keyboardServerSocket = new ServerSocket(1237);
            screenServerSocket = new ServerSocket(1238); // Cổng cho màn hình

            // Khởi động các luồng để nhận dữ liệu
            new Thread(this::listenForMouseEvents).start();
            new Thread(this::listenForKeyboardEvents).start();
            new Thread(this::sendScreenData).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void listenForMouseEvents() {
        try {
            Socket mouseSocket = mouseServerSocket.accept();
            System.out.println("Mouse socket connected.");


            // Đọc dữ liệu liên tục từ mouseSocket
            ObjectInputStream mouse = new ObjectInputStream(mouseSocket.getInputStream());
            //screenRect.width, screenRect.height
            while (true) {
                try {
                    Mouse mouseEvent = (Mouse) mouse.readObject();
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    double scaleX =  screenRect.width / mouseEvent.getWidth();
                    double scaleY =  screenRect.height / mouseEvent.getHeight();
                    double adjustedX =  (mouseEvent.getX() * scaleX);
                    double adjustedY =  (mouseEvent.getY() * scaleY);
                    mouseEvent.setX(adjustedX);
                    mouseEvent.setY(adjustedY);
                    handleMouseEvent(mouseEvent);
                    System.out.println("Received mouse event: " + mouseEvent);
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleMouseEvent(Mouse mouseEvent) {
        // Di chuyển chuột đến vị trí nhận được
        int x = Math.toIntExact(round(mouseEvent.getX()));
        int y = Math.toIntExact(round(mouseEvent.getY()));
        robot.mouseMove(x,y);
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
            ObjectInputStream keyboard= new ObjectInputStream(keyboardSocket.getInputStream());
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
    public void sendScreenData() {
        try (DatagramSocket socket = new DatagramSocket()) {

            while (true) {
                // Chụp ảnh màn hình
                BufferedImage screenImage = robot.createScreenCapture(screenRect);
                InetAddress address = InetAddress.getByName("192.168.2.2");
                // Giảm độ phân giải nếu cần

                // Nén ảnh
                byte[] screenBytes = img.compressImage(screenImage, 0.5f); // Chọn mức nén phù hợp

                // Kiểm tra kích thước gói tin
                if (screenBytes.length > 65507) { // Giới hạn của UDP
                    System.err.println("Dữ liệu quá lớn để gửi qua UDP!");
                    continue;
                }

                // Gửi dữ liệu
                DatagramPacket packet = new DatagramPacket(screenBytes, screenBytes.length, address, 1234);
                socket.send(packet);

                System.out.println("Đã gửi dữ liệu màn hình!");

                // Điều chỉnh tần suất gửi
                Thread.sleep(100); // 10 fps
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws AWTException, IOException {
        new RemoteDesktopClient();
    }
}
