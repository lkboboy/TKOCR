package org.example.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 窗口框选工具类：封装小程序窗口、题目文字区域的可视化框选逻辑
 */
public class SelectWindowUtils {
    // 存储框选结果，全局唯一
    private static Rectangle miniProgramWindow; // 小程序完整窗口
    private static Rectangle textCaptureRect;   // 题目文字区域

    // 定义一个回调接口，用于框选完成后通知
    public interface SelectionCallback {
        void onSelectionCompleted(Rectangle rect);
    }

    // 私有化构造方法，禁止实例化
    private SelectWindowUtils() {}

    // ===================== 框选小程序完整窗口 =====================
    /**
     * 弹出透明框选窗口，让用户选择包含翻页区域的小程序完整窗口
     */
    public static void selectMiniProgramWindow(SelectionCallback callback) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("【第一步】框选小程序完整窗口（必须包含翻页区域）");
            frame.setAlwaysOnTop(true); // 置顶，避免被遮挡
            frame.setUndecorated(true); // 无标题栏
            frame.setBackground(new Color(255, 0, 0, 40)); // 红色透明背景
            frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());

            Point[] start = new Point[1]; // 鼠标按下位置
            Point[] end = new Point[1];   // 鼠标释放位置

            // 绘制框选矩形
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (start[0] != null && end[0] != null) {
                        g.setColor(Color.GREEN);
                        g.drawRect(
                                Math.min(start[0].x, end[0].x),
                                Math.min(start[0].y, end[0].y),
                                Math.abs(end[0].x - start[0].x),
                                Math.abs(end[0].y - start[0].y)
                        );
                    }
                }
            };
            panel.setOpaque(false);
            frame.add(panel);

            // 鼠标事件绑定
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { start[0] = e.getPoint(); }

                @Override
                public void mouseReleased(MouseEvent e) {
                    end[0] = e.getPoint();
                    // 校验框选大小，避免过小
                    int w = Math.abs(end[0].x - start[0].x);
                    int h = Math.abs(end[0].y - start[0].y);
                    if (w > 100 && h > 100) {
                        miniProgramWindow = new Rectangle(
                                Math.min(start[0].x, end[0].x),
                                Math.min(start[0].y, end[0].y),
                                w, h
                        );
                        // 框选完成，调用回调
                        if (callback != null) {
                            callback.onSelectionCompleted(miniProgramWindow);
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "窗口太小！请重新框选包含翻页区域的完整小程序窗口！", "提示", JOptionPane.WARNING_MESSAGE);
                        miniProgramWindow = null;
                    }
                    frame.dispose(); // 关闭框选窗口
                }
            });

            // 鼠标拖动实时绘制
            panel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    end[0] = e.getPoint();
                    panel.repaint();
                }
            });

            frame.setVisible(true);
        });
    }

    // ===================== 框选题目文字区域 =====================
    /**
     * 弹出透明框选窗口，让用户选择题干+选项+答案的文字区域（会显示已框选的小程序窗口红框）
     */
    public static void selectTextCaptureArea(SelectionCallback callback) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("【第二步】框选题目文字区域（题干+选项+答案）");
            frame.setAlwaysOnTop(true);
            frame.setUndecorated(true);
            frame.setBackground(new Color(0, 255, 0, 40)); // 绿色透明背景
            frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());

            Point[] start = new Point[1];
            Point[] end = new Point[1];

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    // 绘制已框选的小程序窗口（红色矩形），方便对齐
                    if (miniProgramWindow != null) {
                        g.setColor(Color.RED);
                        g.drawRect(miniProgramWindow.x, miniProgramWindow.y, miniProgramWindow.width, miniProgramWindow.height);
                    }
                    // 绘制当前文字区域框选（蓝色矩形）
                    if (start[0] != null && end[0] != null) {
                        g.setColor(Color.BLUE);
                        g.drawRect(
                                Math.min(start[0].x, end[0].x),
                                Math.min(start[0].y, end[0].y),
                                Math.abs(end[0].x - start[0].x),
                                Math.abs(end[0].y - start[0].y)
                        );
                    }
                }
            };
            panel.setOpaque(false);
            frame.add(panel);

            // 鼠标事件绑定
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { start[0] = e.getPoint(); }

                @Override
                public void mouseReleased(MouseEvent e) {
                    end[0] = e.getPoint();
                    // 校验框选大小
                    int w = Math.abs(end[0].x - start[0].x);
                    int h = Math.abs(end[0].y - start[0].y);
                    if (w > 50 && h > 50) {
                        textCaptureRect = new Rectangle(
                                Math.min(start[0].x, end[0].x),
                                Math.min(start[0].y, end[0].y),
                                w, h
                        );
                        // 框选完成，调用回调
                        if (callback != null) {
                            callback.onSelectionCompleted(textCaptureRect);
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "区域太小！请重新框选包含题干和选项的文字区域！", "提示", JOptionPane.WARNING_MESSAGE);
                        textCaptureRect = null;
                    }
                    frame.dispose();
                }
            });

            // 鼠标拖动实时绘制
            panel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    end[0] = e.getPoint();
                    panel.repaint();
                }
            });

            frame.setVisible(true);
        });
    }

    // ===================== 框选结果获取器 =====================
    public static Rectangle getMiniProgramWindow() { return miniProgramWindow; }
    public static Rectangle getTextCaptureRect() { return textCaptureRect; }
}