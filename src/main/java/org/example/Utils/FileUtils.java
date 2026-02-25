package org.example.Utils;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 文件工具类：封装识别结果的文件写入、清空，支持动态输出文件路径
 */
public class FileUtils {
    // 私有化构造方法，禁止实例化
    private FileUtils() {}

    /**
     * 清空指定输出文件（避免追加旧内容）
     */
    public static void clearOutputFile(String outputFile) throws IOException {
        try (FileWriter fw = new FileWriter(outputFile, false)) {
            fw.write("");
        }
    }

    /**
     * 将识别结果写入指定文件，按题目编号追加
     */
    public static void writeRawResult(String rawContent, int questionNum, String outputFile) throws IOException {
        try (FileWriter fw = new FileWriter(outputFile, true)) {
            fw.write(questionNum + ". " + rawContent + "\n\n");
        }
    }

    /**
     * 获取文件绝对路径，方便GUI显示
     */
    public static String getAbsolutePath(String outputFile) {
        return new java.io.File(outputFile).getAbsolutePath();
    }
}