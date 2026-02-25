package org.example.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;

/**
 * 截图工具类：封装屏幕区域截图、图片转URL编码Base64，无本地文件生成
 */
public class CaptureUtils {
    // 私有化构造方法，禁止实例化
    private CaptureUtils() {}

    /**
     * 对指定屏幕区域进行内存截图（BufferedImage），无本地文件
     */
    public static BufferedImage captureScreen(Rectangle captureRect, Robot robot) {
        return robot.createScreenCapture(captureRect);
    }

    /**
     * 将BufferedImage转换为URL编码的Base64字符串（适配OCR接口要求）
     */
    public static String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        // 先Base64编码，再URL编码，适配OCR接口参数要求
        return URLEncoder.encode(Base64.getEncoder().encodeToString(imageBytes), "UTF-8");
    }
}