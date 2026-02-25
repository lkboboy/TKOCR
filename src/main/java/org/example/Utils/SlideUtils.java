package org.example.Utils;

import org.example.Config.FileConfig;


import java.awt.*;
import java.awt.event.InputEvent;

/**
 * 翻页工具类：封装小程序鼠标滑动翻页逻辑，精准翻页不卡顿
 */
public class SlideUtils {
    // 私有化构造方法，禁止实例化
    private SlideUtils() {}

    /**
     * 高效精准翻页（从Config读取翻页参数，翻页后还原鼠标位置）
     */
    public static boolean highEfficiencySlide(Robot robot, FileConfig config) {
        try {
            // 获取鼠标原始位置，翻页后还原，避免鼠标偏移
            Point originalPos = MouseInfo.getPointerInfo().getLocation();
            // 获取框选的小程序窗口
            Rectangle miniProgramWindow = SelectWindowUtils.getMiniProgramWindow();

            // 计算翻页坐标（垂直居中滑动，从右向左）
            int slideY = miniProgramWindow.y + miniProgramWindow.height / 2;
            int slideStartX = miniProgramWindow.x + miniProgramWindow.width - 50;
            int slideEndX = slideStartX - FileConfig.SLIDE_DISTANCE;
            if (slideEndX < miniProgramWindow.x + 50) slideEndX = miniProgramWindow.x + 50;

            // 执行鼠标滑动翻页
            robot.mouseMove(slideStartX, slideY);
            robot.delay(FileConfig.MOUSE_DELAY);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(50);

            // 分步滑动，确保翻页到位（避免一次性滑动失效）
            int currentX = slideStartX;
            while (currentX > slideEndX) {
                currentX -= FileConfig.SLIDE_STEP;
                if (currentX < slideEndX) currentX = slideEndX;
                robot.mouseMove(currentX, slideY);
                robot.delay(FileConfig.STEP_DELAY);
            }

            // 释放鼠标，还原位置
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(50);
            robot.mouseMove(originalPos.x, originalPos.y);

            return true;
        } catch (Exception e) {
            System.err.println("【翻页失败】：" + e.getMessage());
            return false;
        }
    }
}