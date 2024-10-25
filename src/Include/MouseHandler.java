package Include;

import java.awt.event.*;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MouseHandler extends MouseAdapter {
    private final Socket mouseSocket;
    private ObjectOutputStream mouseOut;

    public MouseHandler(Socket mouseSocket) {
        this.mouseSocket = mouseSocket;
        try {
            Thread.sleep(1000);
            mouseOut = new ObjectOutputStream(mouseSocket.getOutputStream());
            mouseOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Bắt sự kiện di chuyển chuột
    @Override
    public void mouseMoved(MouseEvent e) {
        sendMouseEvent(e);
    }

    // Bắt sự kiện nhấn chuột
    @Override
    public void mousePressed(MouseEvent e) {
        sendMouseEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        sendMouseEvent(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        sendMouseEvent(e);
    }
    // Gửi sự kiện chuột đến client qua socket
    private void sendMouseEvent(MouseEvent e) {
        try {
            // Tạo đối tượng Mouse để lưu thông tin sự kiện chuột
            System.out.println("Gửi sự kiện chuột");
            Mouse mouseData = new Mouse(e.getX(), e.getY(), e.getButton(), e.getID(),800,600);
            mouseOut.writeObject(mouseData);
            mouseOut.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void sendMouseWheelEvent(MouseWheelEvent e) {
        try {
            // Tạo đối tượng Mouse để lưu thông tin sự kiện chuột
            System.out.println("Gửi sự kiện chuột");
            MouseWheel mouseData = new MouseWheel(e.getX(),e.getY(),e.getWheelRotation(),e.getScrollAmount(),e.getScrollType(),800,600);
            mouseOut.writeObject(mouseData);
            mouseOut.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

