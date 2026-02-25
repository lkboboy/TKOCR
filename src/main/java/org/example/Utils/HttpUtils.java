package org.example.Utils;

import okhttp3.OkHttpClient;
import org.example.Config.FileConfig;

import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端工具类：全局单例，统一管理HTTP请求配置
 */
public class HttpUtils {
    // 全局单例OkHttpClient
    private static OkHttpClient httpClient;

    // 私有化构造方法，禁止实例化
    private HttpUtils() {}

    /**
     * 获取全局OkHttpClient实例，读取Config中的超时配置
     */
    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient().newBuilder()
                    .readTimeout(FileConfig.HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                    .connectTimeout(FileConfig.HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .build();
        }
        return httpClient;
    }
}