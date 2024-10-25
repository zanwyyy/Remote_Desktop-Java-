package Include;

import java.io.Serializable;

public class MouseWheel implements Serializable {
    private static final long serialVersionUID = 1L;

    private int x;
    private int y;
    private int wheelRotation;
    private int scrollAmount;
    private int scrollType;
    private int screenWidth;
    private int screenHeight;

    public MouseWheel(int x, int y, int wheelRotation, int scrollAmount, int scrollType, int screenWidth, int screenHeight) {
        this.x = x;
        this.y = y;
        this.wheelRotation = wheelRotation;
        this.scrollAmount = scrollAmount;
        this.scrollType = scrollType;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    // Getters và Setters (nếu cần)

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWheelRotation() {
        return wheelRotation;
    }

    public int getScrollAmount() {
        return scrollAmount;
    }

    public int getScrollType() {
        return scrollType;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setX(double x) {
        this.x = (int)x;
    }

    public void setY(double y) {
        this.y = (int)y;
    }
}
