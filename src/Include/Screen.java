package Include;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import javax.imageio.ImageIO;

public class Screen implements Serializable {
    private static final long serialVersionUID = 1L;

    private int width;
    private int height;
    private byte[] screenData;  // Dữ liệu màn hình dưới dạng byte array

    public Screen(int width, int height, BufferedImage screenData) throws IOException {
        this.width = width;
        this.height = height;
        this.screenData = convertToByteArray(screenData);  // Chuyển đổi BufferedImage sang byte array
    }

    // Getter và setter cho thuộc tính width
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    // Getter và setter cho thuộc tính height
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    // Chuyển đổi BufferedImage thành byte[]
    private byte[] convertToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    // Chuyển đổi byte[] trở lại BufferedImage
    public BufferedImage getScreenData() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(screenData);
        return ImageIO.read(bais);
    }
}
