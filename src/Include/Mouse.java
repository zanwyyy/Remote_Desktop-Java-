package Include;



import java.io.Serializable;

// Lớp lưu trữ thông tin sự kiện chuột
public class Mouse implements Serializable {
    private static final long serialVersionUID = 1L;
    private double x;
    private double y;
    private int button;
    private int eventID;
    private double width;
    private double height;

    public Mouse(double x, double y, int button, int eventID, double width, double height) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.eventID = eventID;
        this.width = width;
        this.height = height;
    }



    // Getters and setters
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getButton() {
        return button;
    }

    public int getEventID() {
        return eventID;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
