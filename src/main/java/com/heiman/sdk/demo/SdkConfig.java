package com.heiman.sdk.demo;

import com.heiman.sdk.mqtt.client.MqttConfig;
import com.heiman.sdk.restful.client.HeimanEndpoints;
import com.heiman.sdk.restful.model.ClientConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Demo 运行凭证配置。
 * Demo runtime credential configuration.
 * <p>
 * 从 classpath 下的 {@code sdk-config.properties} 读取 appId/secureKey/tenantId/saasId。
 * Reads appId/secureKey/tenantId/saasId from {@code sdk-config.properties} on the classpath.
 * 文件缺失或配置项为空时, 回退到 SDK 内置示例值 {@link HeimanEndpoints}, 保证 Demo 开箱可跑。
 * When the file is missing or a config item is empty, it falls back to the SDK built-in placeholder
 * {@link HeimanEndpoints} so the Demo still starts; missing/placeholder values raise an exception
 * prompting the caller to supply their own credentials (no real credentials are bundled).
 * <p>
 * iot 平台打包 SDK Demo 时只需覆盖该配置文件, 无需改动源码。
 * When the iot platform packages the SDK Demo, only this config file needs to be overridden; no source change required.
 */
public final class SdkConfig {

    private static final String RESOURCE = "sdk-config.properties";

    private static final SdkConfig INSTANCE = new SdkConfig();

    private final Properties properties = new Properties();

    private SdkConfig() {
        try (InputStream stream = SdkConfig.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream != null) {
                properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (Exception ignore) {
            // 配置文件缺失时使用内置默认值
        }
    }

    public static SdkConfig get() {
        return INSTANCE;
    }

    public String getAppId() {
        return require("appId", HeimanEndpoints.DEFAULT_APP_ID);
    }

    public String getSecureKey() {
        return require("secureKey", HeimanEndpoints.DEFAULT_SECURE_KEY);
    }

    public String getTenantId() {
        return HeimanEndpoints.DEFAULT_TENANT_ID;
    }

    /**
     * 构建 RESTful 客户端配置。
     */
    public ClientConfig toClientConfig() {
        return new ClientConfig()
            .setAppId(getAppId())
            .setSecureKey(getSecureKey())
            .setTenantId(getTenantId())
            .setConnectTimeout(30000L)
            .setReadTimeout(60000L)
            .setWriteTimeout(60000L)
            .setEnableLogging(true);
    }

    /**
     * 构建 MQTT 客户端配置, broker 由 SDK 内置, 仅需登录后的 userId。
     */
    public MqttConfig toMqttConfig(String userId) {
        return new MqttConfig(getAppId(), getSecureKey(), getTenantId(), userId);
    }

    private String require(String key, String placeholder) {
        String value = properties.getProperty(key);
        // 空值、未替换的占位符 (${...}) 或仍为内置占位符都视为未配置
        if (value == null || value.isEmpty() || value.startsWith("${")) {
            throw new IllegalStateException(
                "缺少有效的 SDK 运行凭证配置: " + key
                + "。请在 classpath 下 sdk-config.properties 中填写你自己的应用凭证, "
                + "SDK 不内置任何真实凭证。");
        }
        return value.trim();
    }
}
