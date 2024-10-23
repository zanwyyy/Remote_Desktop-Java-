package Include;

import java.awt.event.*;
import java.io.ObjectOutputStream;
import java.net.Socket;
import Include.Keyboard;

public class KeyboardHandler implements KeyListener {
    private Socket keyboardSocket;
    private ObjectOutputStream keyOut;

    public KeyboardHandler(Socket keyboardSocket) {
        this.keyboardSocket = keyboardSocket;
        try {
            keyOut = new ObjectOutputStream(keyboardSocket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        sendKeyEvent(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        sendKeyEvent(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Có thể bỏ qua hoặc dùng nếu cần
    }

    // Gửi sự kiện bàn phím đến client qua socket
    private void sendKeyEvent(KeyEvent e) {
        try {
            // Tạo đối tượng Keyboard để lưu thông tin sự kiện bàn phím
            System.out.println("Gửi sự kiện phím");
            Keyboard keyData = new Keyboard(e.getKeyCode(), e.getID());
            keyOut.writeObject(keyData);
            keyOut.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

