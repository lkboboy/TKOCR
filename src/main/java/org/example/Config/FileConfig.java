package org.example.Config;

/**
 * 动态配置类：所有参数从GUI界面读取，支持实时修改
 */
public class FileConfig {
    // OCR平台（BAIDU/TENCENT/AUTO）
    private String ocrPlatform = "BAIDU";
    // 百度OCR标准版配置
    private String baiduApiKey = "";
    private String baiduSecretKey = "";
    // 腾讯OCR标准版配置
    private String tencentSecretId = "";
    private String tencentSecretKey = "";
    private String tencentRegion = "ap-beijing";
    // 程序运行配置
    private String outputFile = "识别结果.txt";
    private int recognizeCount = 5;
    // 翻页固定参数（无需GUI修改，全局通用）
    public static final int SLIDE_DISTANCE = 350;
    public static final int SLIDE_STEP = 3;
    public static final int STEP_DELAY = 8;
    public static final int PAGE_LOAD_DELAY = 1500;
    public static final int MOUSE_DELAY = 60;
    // HTTP客户端固定配置
    public static final int HTTP_READ_TIMEOUT = 300; // 秒
    public static final int HTTP_CONNECT_TIMEOUT = 60; // 秒

    // Getter & Setter 全量生成
    public String getOcrPlatform() { return ocrPlatform; }
    public void setOcrPlatform(String ocrPlatform) { this.ocrPlatform = ocrPlatform; }
    public String getBaiduApiKey() { return baiduApiKey; }
    public void setBaiduApiKey(String baiduApiKey) { this.baiduApiKey = baiduApiKey; }
    public String getBaiduSecretKey() { return baiduSecretKey; }
    public void setBaiduSecretKey(String baiduSecretKey) { this.baiduSecretKey = baiduSecretKey; }
    public String getTencentSecretId() { return tencentSecretId; }
    public void setTencentSecretId(String tencentSecretId) { this.tencentSecretId = tencentSecretId; }
    public String getTencentSecretKey() { return tencentSecretKey; }
    public void setTencentSecretKey(String tencentSecretKey) { this.tencentSecretKey = tencentSecretKey; }
    public String getTencentRegion() { return tencentRegion; }
    public void setTencentRegion(String tencentRegion) { this.tencentRegion = tencentRegion; }
    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String outputFile) { this.outputFile = outputFile; }
    public int getRecognizeCount() { return recognizeCount; }
    public void setRecognizeCount(int recognizeCount) { this.recognizeCount = recognizeCount; }
}