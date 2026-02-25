package org.example.Utils;

import okhttp3.*;
import org.example.Config.FileConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * OCR核心工具类：封装百度/腾讯标准版OCR所有逻辑，适配动态配置
 */
public class OcrUtils {
    // 标记百度OCR是否可用（AUTO模式自动切换用）
    private static boolean baiduOcrAvailable = true;

    // 私有化构造方法，禁止实例化
    private OcrUtils() {}

    // ===================== 百度标准版OCR相关 =====================
    /**
     * 获取百度OCR AccessToken（从Config读取密钥）
     */
    public static String getBaiduAccessToken(FileConfig config) throws IOException, JSONException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType,
                "grant_type=client_credentials&client_id=" + config.getBaiduApiKey() + "&client_secret=" + config.getBaiduSecretKey());

        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        Response response = HttpUtils.getHttpClient().newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("百度Token获取失败，响应码：" + response.code());
        }
        return new JSONObject(response.body().string()).getString("access_token");
    }

    /**
     * 调用百度通用文字识别（标准版）
     * 【关键修改】：添加recognize_granularity=small参数，解决形近字符误判
     */
    public static String callBaiduStandardOCR(String accessToken, String imageBase64) throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        // 标准版纯文字识别，新增字符精准识别参数：recognize_granularity=small
        String requestBody = String.format(
                "image=%s&detect_direction=false&paragraph=false&probability=false&recognize_granularity=small",
                imageBase64
        );

        RequestBody body = RequestBody.create(mediaType, requestBody);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token=" + accessToken)
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        Response response = HttpUtils.getHttpClient().newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("百度OCR识别失败，响应码：" + response.code());
        }
        return response.body().string();
    }

    // ===================== 腾讯标准版OCR相关 =====================
    /**
     * 调用腾讯云通用文字识别（标准版），从Config读取密钥/地域
     */
    public static String callTencentStandardOCR(String imageBase64, FileConfig config) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String service = "ocr";
        String host = "ocr.tencentcloudapi.com";
        String algorithm = "TC3-HMAC-SHA256";

        // 腾讯标准版：仅传ImageBase64，移除位置相关参数
        String imageBase64NoEncode = URLDecoder.decode(imageBase64, "UTF-8");
        JSONObject reqBody = new JSONObject();
        reqBody.put("ImageBase64", imageBase64NoEncode);
        String payload = reqBody.toString();

        // 腾讯云标准签名逻辑
        String date = timestamp.substring(0, 8);
        String credentialScope = date + "/" + service + "/tc3_request";
        String canonicalRequest = "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + host + "\n\ncontent-type;host\n" + sha256Hex(payload);
        String stringToSign = algorithm + "\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);

        // 从Config读取腾讯SecretKey生成签名
        byte[] secretDate = hmacSHA256(("TC3" + config.getTencentSecretKey()).getBytes(StandardCharsets.UTF_8), date.getBytes(StandardCharsets.UTF_8));
        byte[] secretService = hmacSHA256(secretDate, service.getBytes(StandardCharsets.UTF_8));
        byte[] secretSigning = hmacSHA256(secretService, "tc3_request".getBytes(StandardCharsets.UTF_8));
        String signature = bytesToHex(hmacSHA256(secretSigning, stringToSign.getBytes(StandardCharsets.UTF_8))).toLowerCase();

        // 构造请求头（从Config读取SecretId和地域）
        String authorization = String.format(
                "%s Credential=%s/%s, SignedHeaders=content-type;host, Signature=%s",
                algorithm, config.getTencentSecretId(), credentialScope, signature
        );

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, payload);
        Request request = new Request.Builder()
                .url("https://" + host + "/")
                .post(body)
                .addHeader("Authorization", authorization)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Host", host)
                .addHeader("X-TC-Action", "GeneralBasicOCR")
                .addHeader("X-TC-Version", "2018-11-19")
                .addHeader("X-TC-Timestamp", timestamp)
                .addHeader("X-TC-Region", config.getTencentRegion())
                .build();

        Response response = HttpUtils.getHttpClient().newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("腾讯云OCR识别失败，响应码：" + response.code());
        }
        return response.body().string();
    }

    // ===================== OCR结果提取（兼容百度/腾讯格式） =====================
    /**
     * 提取OCR识别原始文字，自动处理百度/腾讯返回格式，包含错误处理
     */
    public static String extractRawOCRText(String ocrRawJson) {
        if (ocrRawJson == null || ocrRawJson.isEmpty()) return null;

        try {
            JSONObject ocrJson = new JSONObject(ocrRawJson);

            // 百度OCR错误处理
            if (ocrJson.has("error_code")) {
                System.err.println("【百度OCR错误】：" + ocrJson.getString("error_msg"));
                return null;
            }

            // 腾讯云OCR错误处理
            if (ocrJson.has("Response") && ocrJson.getJSONObject("Response").has("Error")) {
                System.err.println("【腾讯OCR错误】：" + ocrJson.getJSONObject("Response").getJSONObject("Error").getString("Message"));
                return null;
            }

            // 提取文字内容
            StringBuilder rawText = new StringBuilder();
            // 百度格式：words_result -> words
            if (ocrJson.has("words_result")) {
                JSONArray words = ocrJson.getJSONArray("words_result");
                for (int i = 0; i < words.length(); i++) {
                    rawText.append(words.getJSONObject(i).getString("words")).append("\n");
                }
            }
            // 腾讯格式：Response -> TextDetections -> DetectedText
            else if (ocrJson.has("Response") && ocrJson.getJSONObject("Response").has("TextDetections")) {
                JSONArray words = ocrJson.getJSONObject("Response").getJSONArray("TextDetections");
                for (int i = 0; i < words.length(); i++) {
                    rawText.append(words.getJSONObject(i).getString("DetectedText")).append("\n");
                }
            }

            String result = rawText.toString().trim();
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            System.err.println("【OCR结果提取失败】：" + e.getMessage());
            return null;
        }
    }

    // ===================== 平台自动切换逻辑 =====================
    /**
     * 根据Config中的平台配置，自动选择OCR平台调用（AUTO模式百度失败切腾讯）
     */
    public static String callOcrByPlatform(String imageBase64, FileConfig config) throws Exception {
        String ocrRaw = null;
        try {
            // 优先百度，或AUTO模式且百度可用
            if (config.getOcrPlatform().equals("BAIDU") || (config.getOcrPlatform().equals("AUTO") && baiduOcrAvailable)) {
                String token = getBaiduAccessToken(config);
                ocrRaw = callBaiduStandardOCR(token, imageBase64);
            } else {
                // 直接调用腾讯
                ocrRaw = callTencentStandardOCR(imageBase64, config);
            }
        } catch (Exception e) {
            System.err.println("【当前平台调用失败】：" + e.getMessage());
            // AUTO模式下，百度失败自动切换到腾讯
            if (config.getOcrPlatform().equals("AUTO") && baiduOcrAvailable) {
                baiduOcrAvailable = false;
                ocrRaw = callTencentStandardOCR(imageBase64, config);
            } else {
                throw e; // 非AUTO模式，直接抛出异常
            }
        }
        return ocrRaw;
    }

    // ===================== 腾讯签名辅助方法（私有，内部调用） =====================
    private static byte[] hmacSHA256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String sha256Hex(String data) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}