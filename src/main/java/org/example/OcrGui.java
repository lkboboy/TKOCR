package org.example;

import org.example.Config.FileConfig;
import org.example.Utils.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCR识别工具GUI主界面：程序入口，包含所有可视化操作、配置、日志、结果预览
 */
public class OcrGui extends JFrame {
    // 核心配置对象
    private final FileConfig config = new FileConfig();
    // 后台线程池：避免识别逻辑卡死GUI界面
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // 识别状态控制：volatile保证多线程可见性
    private volatile boolean isRunning = false;

    // GUI核心组件
    private JTextField tfBaiduApiKey, tfBaiduSecretKey;
    private JTextField tfTencentSecretId, tfTencentSecretKey;
    private JTextField tfOutputFile, tfRecognizeCount;
    private JComboBox<String> cbOcrPlatform;
    private JTextArea taLog, taResult;
    private JLabel lblWindowCoord, lblAreaCoord; // 新增：显示坐标的标签
    private JButton btnSelectWindow, btnSelectArea, btnStart, btnStop, btnClearLog;

    // 构造方法：初始化GUI界面
    public OcrGui() {
        // 窗口基础设置
        setTitle("通用题库OCR识别导出工具");
        setSize(1000, 800); // 增大窗口高度，从700改为800
        setDefaultCloseOperation(EXIT_ON_CLOSE); // 关闭窗口退出程序
        setLocationRelativeTo(null); // 窗口居中显示
        setLayout(new BorderLayout(10, 10)); // 边框布局，分区域
        setResizable(true); // 允许窗口缩放

        // 初始化所有UI组件
        initComponents();
        // 绑定所有组件的点击/选择事件
        bindEvents();
    }

    /**
     * 初始化所有GUI组件，分区域：配置区、操作区、日志+结果预览区
     */
    private void initComponents() {
        // ========== 顶部：配置区（网格布局，8行2列） ==========
        JPanel pnlConfig = new JPanel();
        pnlConfig.setBorder(new TitledBorder("配置区（*密钥请填写自己的百度/腾讯OCR密钥）"));
        pnlConfig.setLayout(new GridLayout(8, 2, 5, 5));
        pnlConfig.setBorder(BorderFactory.createCompoundBorder(
                pnlConfig.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // 1. OCR平台选择
        pnlConfig.add(new JLabel("OCR平台选择："));
        cbOcrPlatform = new JComboBox<>(new String[]{"BAIDU", "TENCENT", "AUTO"});
        cbOcrPlatform.setSelectedItem(config.getOcrPlatform());
        pnlConfig.add(cbOcrPlatform);

        // 2. 百度API Key（改为普通文本框，不再是密码框）
        pnlConfig.add(new JLabel("百度API Key："));
        tfBaiduApiKey = new JTextField(); // 从JPasswordField改为JTextField
        tfBaiduApiKey.setToolTipText("百度智能云OCR控制台获取");
        pnlConfig.add(tfBaiduApiKey);

        // 3. 百度Secret Key（改为普通文本框）
        pnlConfig.add(new JLabel("百度Secret Key："));
        tfBaiduSecretKey = new JTextField(); // 从JPasswordField改为JTextField
        tfBaiduSecretKey.setToolTipText("百度智能云OCR控制台获取");
        pnlConfig.add(tfBaiduSecretKey);

        // 4. 腾讯SecretId（改为普通文本框）
        pnlConfig.add(new JLabel("腾讯SecretId："));
        tfTencentSecretId = new JTextField(); // 从JPasswordField改为JTextField
        tfTencentSecretId.setToolTipText("腾讯云OCR控制台API密钥管理获取");
        pnlConfig.add(tfTencentSecretId);

        // 5. 腾讯SecretKey（改为普通文本框）
        pnlConfig.add(new JLabel("腾讯SecretKey："));
        tfTencentSecretKey = new JTextField(); // 从JPasswordField改为JTextField
        tfTencentSecretKey.setToolTipText("腾讯云OCR控制台API密钥管理获取");
        pnlConfig.add(tfTencentSecretKey);

        // 6. 输出文件路径
        pnlConfig.add(new JLabel("结果输出文件："));
        tfOutputFile = new JTextField(config.getOutputFile());
        pnlConfig.add(tfOutputFile);

        // 7. 识别次数
        pnlConfig.add(new JLabel("识别题目次数："));
        tfRecognizeCount = new JTextField(String.valueOf(config.getRecognizeCount()));
        tfRecognizeCount.setToolTipText("建议填写2000，适配双平台免费额度");
        pnlConfig.add(tfRecognizeCount);

        // 8. 清空日志按钮（占位对齐）
        pnlConfig.add(new JLabel(""));
        btnClearLog = new JButton("清空日志/结果");
        pnlConfig.add(btnClearLog);

        // ========== 中部：操作区（流式布局，按钮居中） ==========
        JPanel pnlOperation = new JPanel();
        pnlOperation.setBorder(new TitledBorder("操作区（按步骤点击）"));
        pnlOperation.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));

        // 操作按钮
        btnSelectWindow = new JButton("1. 框选小程序窗口");
        btnSelectArea = new JButton("2. 框选文字区域");
        btnStart = new JButton("3. 开始识别");
        btnStop = new JButton("停止识别");
        btnStop.setEnabled(false); // 初始状态停止按钮不可用

        // 统一按钮大小，美观
        Dimension btnSize = new Dimension(160, 35);
        btnSelectWindow.setPreferredSize(btnSize);
        btnSelectArea.setPreferredSize(btnSize);
        btnStart.setPreferredSize(btnSize);
        btnStop.setPreferredSize(btnSize);

        // 添加按钮到操作区
        pnlOperation.add(btnSelectWindow);
        pnlOperation.add(btnSelectArea);
        pnlOperation.add(btnStart);
        pnlOperation.add(btnStop);

        // 新增：显示框选坐标的面板
        JPanel pnlCoord = new JPanel();
        pnlCoord.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
        lblWindowCoord = new JLabel("小程序窗口坐标：未框选");
        lblAreaCoord = new JLabel("文字区域坐标：未框选");
        pnlCoord.add(lblWindowCoord);
        pnlCoord.add(lblAreaCoord);
        pnlOperation.add(pnlCoord); // 将坐标显示添加到操作区

        // ========== 底部：日志+结果预览区（网格布局，1行2列） ==========
        JPanel pnlBottom = new JPanel();
        pnlBottom.setLayout(new GridLayout(1, 2, 10, 10));
        pnlBottom.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        pnlBottom.setPreferredSize(new Dimension(0, 350)); // 增大底部区域高度，从默认改为350

        // 左侧：运行日志区
        JPanel pnlLog = new JPanel();
        pnlLog.setBorder(new TitledBorder("运行日志（实时）"));
        pnlLog.setLayout(new BorderLayout());
        taLog = new JTextArea();
        taLog.setEditable(false);
        // 修改为支持中文的等宽字体
        taLog.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        taLog.setLineWrap(true);
        pnlLog.add(new JScrollPane(taLog), BorderLayout.CENTER);

        // 右侧：结果预览区
        JPanel pnlResult = new JPanel();
        pnlResult.setBorder(new TitledBorder("识别结果预览（与文件一致）"));
        pnlResult.setLayout(new BorderLayout());
        taResult = new JTextArea();
        taResult.setEditable(false);
        // 修改为支持中文的等宽字体
        taResult.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        taResult.setLineWrap(true);
        pnlResult.add(new JScrollPane(taResult), BorderLayout.CENTER);

        // 添加到底部面板
        pnlBottom.add(pnlLog);
        pnlBottom.add(pnlResult);

        // ========== 将所有区域添加到主窗口 ==========
        add(pnlConfig, BorderLayout.NORTH);
        add(pnlOperation, BorderLayout.CENTER);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    /**
     * 绑定所有GUI组件的事件：点击、选择等
     */
    private void bindEvents() {
        // 1. 框选小程序窗口按钮
        btnSelectWindow.addActionListener(e -> {
            appendLog("【操作】开始框选小程序窗口，请拖动鼠标选择包含翻页区域的完整窗口！");
            SelectWindowUtils.selectMiniProgramWindow(rect -> {
                // 框选完成后的回调
                lblWindowCoord.setText(String.format("小程序窗口坐标：X=%d, Y=%d, W=%d, H=%d",
                        rect.x, rect.y, rect.width, rect.height));
                appendLog("【成功】小程序窗口框选完成！坐标：" + rect);
            });
        });

// 2. 框选文字区域按钮（校验先框选窗口）
        btnSelectArea.addActionListener(e -> {
            if (SelectWindowUtils.getMiniProgramWindow() == null) {
                JOptionPane.showMessageDialog(this, "请先点击【1. 框选小程序窗口】完成窗口选择！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            appendLog("【操作】开始框选文字区域，请拖动鼠标选择题干+选项+答案的区域！");
            SelectWindowUtils.selectTextCaptureArea(rect -> {
                // 框选完成后的回调
                lblAreaCoord.setText(String.format("文字区域坐标：X=%d, Y=%d, W=%d, H=%d",
                        rect.x, rect.y, rect.width, rect.height));
                appendLog("【成功】文字区域框选完成！坐标：" + rect);
            });
        });

        // 3. 开始识别按钮（核心逻辑，后台线程运行）
        btnStart.addActionListener(e -> {
            // 第一步：校验配置是否合法，不合法直接返回
            if (!validateConfig()) return;
            // 第二步：从GUI界面更新配置到Config对象
            updateConfigFromUI();
            // 第三步：更新UI状态，禁用无关按钮
            isRunning = true;
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            btnSelectWindow.setEnabled(false);
            btnSelectArea.setEnabled(false);
            appendLog("==================== 开始识别 ====================");
            appendLog("【配置】OCR平台：" + config.getOcrPlatform() + " | 识别次数：" + config.getRecognizeCount() + " | 输出文件：" + FileUtils.getAbsolutePath(config.getOutputFile()));

            // 后台线程执行识别逻辑，避免GUI卡死
            executor.execute(() -> {
                try {
                    // 初始化Robot，用于翻页和截图
                    Robot robot = new Robot();
                    robot.setAutoWaitForIdle(true);
                    // 清空输出文件，避免追加旧内容
                    FileUtils.clearOutputFile(config.getOutputFile());

                    int questionNum = 1; // 题目编号，从1开始
                    // 识别循环：按配置的次数执行，支持手动停止
                    for (int i = 1; i <= config.getRecognizeCount() && isRunning; i++) {
                        appendLog("=== 处理第 " + i + " 题 ===");

                        // 步骤1：精准翻页
                        if (!SlideUtils.highEfficiencySlide(robot, config)) {
                            appendLog("【警告】翻页失败，跳过本次识别");
                            continue;
                        }
                        robot.delay(FileConfig.PAGE_LOAD_DELAY); // 页面加载延迟，避免截图空白

                        // 步骤2：截图并转换为Base64
                        BufferedImage img = CaptureUtils.captureScreen(SelectWindowUtils.getTextCaptureRect(), robot);
                        String imageBase64 = CaptureUtils.imageToBase64(img);

                        // 步骤3：调用OCR（按平台配置）
                        String ocrRaw = OcrUtils.callOcrByPlatform(imageBase64, config);

                        // 步骤4：提取识别文字
                        String rawText = OcrUtils.extractRawOCRText(ocrRaw);
                        if (rawText == null || rawText.isEmpty()) {
                            appendLog("【警告】OCR识别无有效内容，跳过本次");
                            continue;
                        }

                        // 步骤5：写入文件+结果预览
                        FileUtils.writeRawResult(rawText, questionNum, config.getOutputFile());
                        appendResult(questionNum + ". " + rawText + "\n\n");
                        appendLog("【成功】第 " + i + " 题识别完成，已写入文件");

                        questionNum++;
                    }

                    // 识别完成，更新UI状态
                    SwingUtilities.invokeLater(() -> {
                        appendLog("==================== 识别结束 ====================");
                        appendLog("【成功】识别完成！结果文件路径：" + FileUtils.getAbsolutePath(config.getOutputFile()));
                        resetUIState(); // 重置按钮状态
                    });

                } catch (Exception ex) {
                    // 识别异常，日志记录+重置UI
                    appendLog("【错误】识别过程中发生异常：" + ex.getMessage());
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(this::resetUIState);
                }
            });
        });

        // 4. 停止识别按钮（手动终止循环）
        btnStop.addActionListener(e -> {
            isRunning = false;
            appendLog("【操作】用户手动停止识别，当前识别过程已终止！");
            resetUIState();
        });

        // 5. 清空日志/结果按钮
        btnClearLog.addActionListener(e -> {
            taLog.setText("");
            taResult.setText("");
            appendLog("【操作】日志和结果预览已清空！");
        });
    }

    /**
     * 校验GUI配置是否合法：密钥、识别次数、框选区域等
     */
    private boolean validateConfig() {
        // 校验OCR平台和对应密钥
        String platform = (String) cbOcrPlatform.getSelectedItem();
        if (platform.equals("BAIDU") || platform.equals("AUTO")) {
            if (tfBaiduApiKey.getText().trim().isEmpty() || tfBaiduSecretKey.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "选择百度/AUTO平台时，百度API Key和Secret Key不能为空！", "配置错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (platform.equals("TENCENT") || platform.equals("AUTO")) {
            if (tfTencentSecretId.getText().trim().isEmpty() || tfTencentSecretKey.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "选择腾讯/AUTO平台时，腾讯SecretId和SecretKey不能为空！", "配置错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        // 校验识别次数：必须是1-10000的整数
        try {
            int count = Integer.parseInt(tfRecognizeCount.getText().trim());
            if (count <= 0 || count > 10000) {
                JOptionPane.showMessageDialog(this, "识别次数必须是1-10000的整数！建议填写2000！", "配置错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "识别次数必须是数字！不能输入其他字符！", "配置错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 校验输出文件：不能为空
        if (tfOutputFile.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "结果输出文件路径不能为空！", "配置错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 校验框选区域（可选，弹出确认框）
        if (SelectWindowUtils.getMiniProgramWindow() == null || SelectWindowUtils.getTextCaptureRect() == null) {
            int confirm = JOptionPane.showConfirmDialog(this, "尚未框选小程序窗口/文字区域，是否继续识别？（可能导致截图失败）", "提示", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        return true;
    }

    /**
     * 从GUI界面的输入框/下拉框更新配置到Config对象
     */
    private void updateConfigFromUI() {
        config.setOcrPlatform((String) cbOcrPlatform.getSelectedItem());
        config.setBaiduApiKey(tfBaiduApiKey.getText().trim());
        config.setBaiduSecretKey(tfBaiduSecretKey.getText().trim());
        config.setTencentSecretId(tfTencentSecretId.getText().trim());
        config.setTencentSecretKey(tfTencentSecretKey.getText().trim());
        config.setOutputFile(tfOutputFile.getText().trim());
        config.setRecognizeCount(Integer.parseInt(tfRecognizeCount.getText().trim()));
    }

    /**
     * 重置UI状态：识别完成/停止后，启用所有按钮，禁用停止按钮
     */
    private void resetUIState() {
        isRunning = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnSelectWindow.setEnabled(true);
        btnSelectArea.setEnabled(true);
    }

    /**
     * 追加日志到日志面板，自动滚动到底部（Swing线程安全）
     */
    public void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            taLog.append(msg + "\n");
            taLog.setCaretPosition(taLog.getText().length()); // 自动滚动到底部
        });
    }

    /**
     * 追加识别结果到预览面板，自动滚动到底部（Swing线程安全）
     */
    public void appendResult(String msg) {
        SwingUtilities.invokeLater(() -> {
            taResult.append(msg);
            taResult.setCaretPosition(taResult.getText().length()); // 自动滚动到底部
        });
    }

    /**
     * 程序入口：启动GUI界面
     */
    public static void main(String[] args) {
        // 启用系统原生外观，让GUI更贴合系统风格（Windows/Mac/Linux）
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 启动Swing GUI（线程安全）
        SwingUtilities.invokeLater(() -> new OcrGui().setVisible(true));
    }
}