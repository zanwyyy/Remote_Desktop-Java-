package Include;



import java.io.Serializable;

// Lớp lưu trữ thông tin sự kiện bàn phím
public class Keyboard implements Serializable {
    private int keyCode;
    private int eventID;

    public Keyboard(int keyCode, int eventID) {
        this.keyCode = keyCode;
        this.eventID = eventID;
    }

    // Getters and setters
    public int getKeyCode() {
        return keyCode;
    }

    public int getEventID() {
        return eventID;
    }
}
