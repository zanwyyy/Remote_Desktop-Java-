package Include;



import java.io.Serializable;

// Lớp lưu trữ thông tin sự kiện chuột
public class Mouse implements Serializable {
    private static final long serialVersionUID = 1L;
    private int x;
    private int y;
    private int button;
    private int eventID;

    public Mouse(int x, int y, int button, int eventID) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.eventID = eventID;
    }

    // Getters and setters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getButton() {
        return button;
    }

    public int getEventID() {
        return eventID;
    }
}
