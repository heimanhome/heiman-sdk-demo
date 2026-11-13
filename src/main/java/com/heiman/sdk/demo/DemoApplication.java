package com.heiman.sdk.demo;

/**
 * SDK 综合演示入口。
 * SDK comprehensive demo entry point.
 * <p>
 * 演示 MQTT 设备接入、RESTful 设备管理 (OAuth2 登录 / 设备列表 / 绑定解绑) 等完整流程,
 * Demonstrates the full flow of MQTT device access and RESTful device management
 * (OAuth2 login / device list / bind & unbind),
 * 并在 JVM 退出时通过 shutdownHook 优雅释放 MQTT 连接与 RESTful 连接池资源。
 * and releases MQTT connections and the RESTful connection pool gracefully via a shutdownHook on JVM exit.
 */
// Add new imports // 添加新的导入
import com.alibaba.fastjson2.JSONObject;
import com.heiman.sdk.demo.model.Property;
import com.heiman.sdk.demo.model.ThingModel;
import com.heiman.sdk.mqtt.client.HeimanMqttClient;
import com.heiman.sdk.mqtt.client.MqttCallback;
import com.heiman.sdk.mqtt.client.MqttConfig;
import com.heiman.sdk.mqtt.entity.*;
import com.heiman.sdk.restful.client.HeimanRestfulClient;
import com.heiman.sdk.restful.client.service.DeviceService;
import com.heiman.sdk.restful.client.service.ProductService;
import com.heiman.sdk.restful.model.ApiResult;
import com.heiman.sdk.restful.model.ClientConfig;
import com.heiman.sdk.restful.model.OAuth2Token;
import com.heiman.sdk.restful.model.common.PageResult;
import com.heiman.sdk.restful.model.device.DeviceDetail;
import com.heiman.sdk.restful.model.device.DeviceDetailMac;
import com.heiman.sdk.restful.model.device.DeviceListQueryParam;
import com.heiman.sdk.restful.model.product.ProductInfo;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import java.util.*;

public class DemoApplication {

    // RESTful client: created by testFHIR () and automatically logged in, with shutdownHook responsible for closing
    private static HeimanRestfulClient restfulClient;
    // Current login user ID: Recorded by testFHIR (), testMqtt() is used to build MQTT connection
    private static String currentUserId;
    // Equipment list/product model: queried by testFHIR (), testMqtt() traverses the equipment and issues control instructions
    private static PageResult<DeviceDetail> deviceListResult;
    private static Map<String, ThingModel> productIdLinkModels = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Heiman SDK Demo Startup ===");

        // Run device demo (integrates gateway and multi-device functionality)
        System.out.println("=== Starting Device Demo Program ===");
        DeviceDemo deviceDemo = new DeviceDemo();
        deviceDemo.run();
        System.out.println("\nDevice Demo completed\n");
        System.out.println("Running client demos");

        // RESTful: OAuth2 Login + Device Binding + Device List / Product Model Query
        testHttpClient();

        // MQTT: Initialize the connection and issue property read/write/control commands to the devices queried by testFHIR ()
        testMqtt();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                HeimanMqttClient.getInstance().shutdown();
            } catch (Exception e) {
                System.err.println("MQTT shutdown error: " + e.getMessage());
            }
            try {
                if (restfulClient != null) {
                    restfulClient.close();
                }
            } catch (Exception e) {
                System.err.println("Restful client close error: " + e.getMessage());
            }
        }, "heiman-demo-shutdown"));

        System.out.println("All demos completed!");
        System.out.println("Demo running, press Ctrl+C to exit...");
    }

    /**
     * RESTful (HTTP) 客户端测试: OAuth2 登录、设备绑定、设备列表与产品物模型查询。
     * Test the RESTful HTTP client: OAuth2 login, device bind, device list and product thing-model query.
     * <p>
     * 注意: 需要先调用本方法获取登录态与设备数据, 再调用 testMqtt()。
     * Must be called first to obtain the login session & device data before testMqtt().
     */
    private static void testHttpClient() throws Exception {
        // 1. Use HeimanRestfulClient instead of OAuth2Config and WebOAuth2ApiClient.
        // Create client configuration. Credentials come from sdk-config.properties (falls back to the
        // built-in sample values), so no need to expose appId/secureKey/tenantId/saasId here.
        ClientConfig clientConfig = SdkConfig.get().toClientConfig();

        // Create RESTful client (auto login)
        restfulClient = new HeimanRestfulClient(clientConfig);

        // 2. Get current token information
        OAuth2Token currentToken = restfulClient.getCurrentToken();
        currentUserId = currentToken.getUserId();
        System.out.println("Login successful!");
        String rawToken = currentToken.getAccessToken();
        String masked = rawToken == null || rawToken.length() <= 8
                ? "***" : rawToken.substring(0, 4) + "****" + rawToken.substring(rawToken.length() - 4);
        System.out.println("Access Token: " + masked);
        System.out.println("Token Type: " + currentToken.getTokenType());
        System.out.println("Expires In: " + currentToken.getExpiresIn());
        System.out.println("User ID: " + currentToken.getUserId());

        // 3. Get device list (need to call RESTful API)
        DeviceService deviceService = restfulClient.getDeviceService();
        // Query device detail by MAC     APP -> Device -> Device detail
        //DeviceDetailMac deviceDetail = deviceService.getDeviceDetailByMac(ProductIdList.DEVICEWS2GW_R_868MHZ, "4055481e5d0b");
        //System.out.println("Device detail query successful!" + deviceDetail.toString());
        String deviceSn = "xxxxxxx";
        deviceService.bindUserDevice(deviceSn);
        System.out.println("Device bind successful!");
        // Create device query parameters
        DeviceListQueryParam queryParam = new DeviceListQueryParam();
        // Set pagination parameters (page 0, 10 items per page)
        queryParam.getHelp().setPageIndex(0);
        queryParam.getHelp().setPageSize(10);

        // Query device list
        deviceListResult = deviceService.queryDeviceList(queryParam);

        System.out.println("Device list query successful!");
        System.out.println("Total count: " + deviceListResult.getTotal());
        System.out.println("Current page: " + deviceListResult.getPageIndex());
        System.out.println("Page size: " + deviceListResult.getPageSize());

        // 4. Query product list
        ProductService productService = restfulClient.getProductService();
        List<ProductInfo> products = productService.getProductList();

        System.out.println("Product list query successful, total: " + products.size());
        productIdLinkModels = new HashMap<>();
        for (ProductInfo product : products) {
            ThingModel thingModel = JSONObject.parseObject(product.getMetadata(), ThingModel.class);
            productIdLinkModels.put(product.getId(), thingModel);
            System.out.println("----- product : " + product.getId() + "-----");
            if (thingModel != null && thingModel.getProperties() != null && !thingModel.getProperties().isEmpty()) {
                for (Property property : thingModel.getProperties()) {
                    System.out.println("source: " + property.getId() + " -- " + property.getName());
                }
            } else {
                System.out.println("no prop");
            }
        }

        //5. unbind device, testing MQ callback requires adding comments to the code.
        ApiResult unbind = deviceService.unbindUserDevice(deviceSn);
        System.out.println("Unbind successful: " + unbind);
    }

    /**
     * MQTT 客户端测试: 初始化 MQTT 连接, 并对设备列表中的设备下发属性读写 / 控制指令。
     * Test the MQTT client: init the MQTT connection and send property read/write & control commands
     * to each device in the list queried by testHttpClient().
     */
    private static void testMqtt() throws Exception {
        System.out.println("\n\nStarting MQTT Demo...");

        // 1. Configure MQTT client - broker is built into the SDK; credentials come from sdk-config.properties
        MqttConfig mqttConfig = SdkConfig.get().toMqttConfig(currentUserId);

        // 2. Implement MQTT callback
        MqttCallback callback = new MqttCallback() {
            @Override
            public void onPropertiesReadReply(String productId, String deviceId, PropertiesReadResponse response) {
                System.out.println("onPropertiesReadReply: " + productId + ", " + deviceId + ", " + response);
            }

            @Override
            public void onPropertiesWriteReply(String productId, String deviceId, PropertiesWriteResponse response) {
                System.out.println("onPropertiesWriteReply: " + productId + ", " + deviceId + ", " + response);
            }

            @Override
            public void onFunctionInvokeReply(String productId, String deviceId, FunctionInvokeResponse response) {
                System.out.println("onFunctionInvokeReply: " + productId + ", " + deviceId + ", " + response);
            }

            @Override
            public void onEventReport(String productId, String deviceId, String eventId, EventReportRequest request) {
                System.out.println("onEventReport: " + productId + ", " + deviceId + ", " + eventId + ", " + request);
            }

            @Override
            public void onPropertiesReport(String productId, String deviceId, PropertiesReportRequest request) {
                System.out.println("onPropertiesReport: " + productId + ", " + deviceId + ", " + request);
            }

            @Override
            public void onMetadataDerived(String productId, String deviceId, String metadata) {
                System.out.println("onMetadataDerived: " + productId + ", " + deviceId + ", " + metadata);
            }

            @Override
            public void onUnsubdeviceReport(String productId, String deviceId, String report) {
                System.out.println("onUnsubdeviceReport: " + productId + ", " + deviceId + ", " + report);
            }

            @Override
            public void onChildGetDeviceInfo(String productId, String deviceId, String info) {
                System.out.println("onChildGetDeviceInfo: " + productId + ", " + deviceId + ", " + info);
            }

            @Override
            public void onTopoGet(String productId, String deviceId, String topo) {
                System.out.println("onTopoGet: " + productId + ", " + deviceId + ", " + topo);
            }

            @Override
            public void onTagsUpdate(String productId, String deviceId, TagsUpdateRequest request) {
                System.out.println("onTagsUpdate: " + productId + ", " + deviceId + ", " + request);
            }

            @Override
            public void onFirmwareUpgradeProgress(String productId, String deviceId, FirmwareUpgradeProgressRequest request) {
                System.out.println("onFirmwareUpgradeProgress: " + productId + ", " + deviceId + ", " + request);
            }

            @Override
            public void onFirmwarePull(String productId, String deviceId, FirmwarePullRequest request) {
                System.out.println("onFirmwarePull: " + productId + ", " + deviceId + ", " + request);
            }

            @Override
            public void onFirmwareReport(String productId, String deviceId, String report) {
                System.out.println("onFirmwareReport: " + productId + ", " + deviceId + ", " + report);
            }

            @Override
            public void onFirmwareReadReply(String productId, String deviceId, String reply) {
                System.out.println("onFirmwareReadReply: " + productId + ", " + deviceId + ", " + reply);
            }

            @Override
            public void onLogReport(String productId, String deviceId, String log) {
                System.out.println("onLogReport: " + productId + ", " + deviceId + ", " + log);
            }

            @Override
            public void onSceneList(String productId, String deviceId, String list) {
                System.out.println("onSceneList: " + productId + ", " + deviceId + ", " + list);
            }

            @Override
            public void onTimeSync(String productId, String deviceId, String time) {
                System.out.println("onTimeSync: " + productId + ", " + deviceId + ", " + time);
            }

            @Override
            public void onUserBindQueryReply(String productId, String deviceId, String reply) {
                System.out.println("onUserBindQueryReply: " + productId + ", " + deviceId + ", " + reply);
            }

            @Override
            public void onSysResetReport(String productId, String deviceId, String report) {
                System.out.println("onSysResetReport: " + productId + ", " + deviceId + ", " + report);
            }

            @Override
            public void onChildPropertiesReport(String gatewayProductId, String gatewayDeviceId, String childDeviceId, String report) {
                System.out.println("onChildPropertiesReport: " + gatewayProductId + ", " + gatewayDeviceId + ", " + childDeviceId + ", " + report);
            }

            @Override
            public void onChildOnline(String productId, String deviceId, String childDeviceId, ChildDeviceOnlineRequest request) {
                System.out.println("onChildOnline: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + request);
            }

            @Override
            public void onChildOffline(String productId, String deviceId, String childDeviceId, ChildDeviceOfflineRequest request) {
                System.out.println("onChildOffline: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + request);
            }

            @Override
            public void onChildPropertiesReadReply(String productId, String deviceId, String childDeviceId, String reply) {
                System.out.println("onChildPropertiesReadReply: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + reply);
            }

            @Override
            public void onChildPropertiesWriteReply(String productId, String deviceId, String childDeviceId, String reply) {
                System.out.println("onChildPropertiesWriteReply: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + reply);
            }

            @Override
            public void onChildFunctionInvokeReply(String productId, String deviceId, String childDeviceId, String reply) {
                System.out.println("onChildFunctionInvokeReply: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + reply);
            }

            @Override
            public void onChildEventReport(String productId, String deviceId, String childDeviceId, String eventId, String report) {
                System.out.println("onChildEventReport: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + eventId + ", " + report);
            }

            @Override
            public void onChildMetadataDerived(String productId, String deviceId, String childDeviceId, String metadata) {
                System.out.println("onChildMetadataDerived: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + metadata);
            }

            @Override
            public void onChildRegister(String productId, String deviceId, String childDeviceId, ChildDeviceRegisterRequest request) {
                System.out.println("onChildRegister: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + request);
            }

            @Override
            public void onChildUnregister(String productId, String deviceId, String childDeviceId, ChildDeviceUnregisterRequest request) {
                System.out.println("onChildUnregister: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + request);
            }

            @Override
            public void onChildListFind(String productId, String deviceId, String childDeviceId, String list) {
                System.out.println("onChildListFind: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + list);
            }

            @Override
            public void onChildDisconnectReply(String productId, String deviceId, String childDeviceId, String reply) {
                System.out.println("onChildDisconnectReply: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + reply);
            }

            @Override
            public void onChildStateCheckReply(String productId, String deviceId, String childDeviceId, ChildDeviceStateCheckResponse response) {
                System.out.println("onChildStateCheckReply: " + productId + ", " + deviceId + ", " + childDeviceId + ", " + response);
            }

            @Override
            public void onDeviceOnline(String productId, String deviceId, DeviceOnlineRequest request) {
                System.out.println("onDeviceOnline: " + productId + ", " + deviceId + ", " + request);
            }

            @Override
            public void onDeviceOffline(String productId, String deviceId, DeviceOfflineRequest request) {
                System.out.println("onDeviceOffline: " + productId + ", " + deviceId + ", " + request);
            }

            @Override
            public void onSceneAddReply(String productId, String deviceId, String reply) {
                System.out.println("onSceneAddReply: " + productId + ", " + deviceId + ", " + reply);
            }

            @Override
            public void onSceneUpdateReply(String productId, String deviceId, String reply) {
                System.out.println("onSceneUpdateReply: " + productId + ", " + deviceId + ", " + reply);
            }

            @Override
            public void onSceneDeleteReply(String productId, String deviceId, String reply) {
                System.out.println("onSceneDeleteReply: " + productId + ", " + deviceId + ", " + reply);
            }

            @Override
            public void onSceneFireReply(String productId, String deviceId, String reply) {
                System.out.println("onSceneFireReply: " + productId + ", " + deviceId + ", " + reply);
            }

            @Override
            public void onConnected(boolean reconnect, String serverURI) {
                System.out.println("Connected to MQTT broker: " + serverURI);
            }

            @Override
            public void onConnectionLost(Throwable cause) {
                System.out.println("Connection lost: " + cause);
            }

            @Override
            public void onDeliveryComplete(IMqttDeliveryToken token) {
                System.out.println("Delivery complete: " + token);
            }
        };

        // 3. Initialize MQTT client
        HeimanMqttClient.getInstance().init(mqttConfig, callback);

        // 4. Traverse the devices queried by testHttpClient() and send control commands
        if (deviceListResult != null && deviceListResult.getData() != null && !deviceListResult.getData().isEmpty()) {
            System.out.println("Device list:");
            for (DeviceDetail device : deviceListResult.getData()) {
                System.out.println("  Device ID: " + device.getDeviceId());
                System.out.println("  Device Name: " + device.getDeviceName());
                System.out.println("  Product ID: " + device.getProductId());
                System.out.println("  Device Status: " + device.getState());
                System.out.println("  ----");
                String productId = device.getProductId();
                String deviceId = device.getDeviceId();
                // 5. 控制设备 (示例：设置属性)
                System.out.println("SDK initialization successful, sending control command...");

                // 新增设备属性测试功能
                ThingModel thingModel = productIdLinkModels.get(productId);
                if (thingModel == null) {
                    continue;
                }

                testDeviceProperties(productId, deviceId, thingModel);

                PropertiesWriteRequest writeRequest = new PropertiesWriteRequest();
                // ... Set writeRequest properties // ... 设置 writeRequest 的属性
                HeimanMqttClient.getInstance().propertiesWrite(productId, deviceId, writeRequest);
                System.out.println("Sent propertiesWrite request to device: " + deviceId);
            }
        } else {
            System.out.println("No device data available"); // 暂无设备数据
        }
    }

    /**
     * Test device property functionality
     * Create property test data based on model classes in the model package
     */
    private static void testDeviceProperties(String productId, String deviceId, ThingModel thingModel) {
        System.out.println("\n=== Starting Device Property Test ===");

        // Create property test data
        Map<String, Object> properties = createTestProperties(thingModel);

        // Create property write request
        PropertiesWriteRequest writeRequest = new PropertiesWriteRequest();
        writeRequest.setProperties(properties);

        System.out.println("Test property data: " + properties);

        try {
            // Send property write request
            HeimanMqttClient.getInstance().propertiesWrite(productId, deviceId, writeRequest);
            System.out.println("Property write request sent");

            // Create property read request
            PropertiesReadRequest readRequest = new PropertiesReadRequest();
            readRequest.setProperties(new ArrayList<>(properties.keySet()));

            // Send property read request
            HeimanMqttClient.getInstance().propertiesRead(productId, deviceId, readRequest);
            System.out.println("Property read request sent");

        } catch (Exception e) {
            System.err.println("Error occurred during property testing: " + e.getMessage());
        }

        System.out.println("=== Device Property Test Completed ===\n");
    }

    /**
     * You can configure specific properties; please refer to the thingModel for details.
     * @param thingModel
     * @return
     */
    private static Map<String, Object> createTestProperties(ThingModel thingModel) {
        Map<String, Object> propties = new HashMap<>();
        List<Property> properties = thingModel.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property pr = properties.get(i);

            // set target property
            if (pr.getId().equalsIgnoreCase("FreezingPointEnable")) {
                propties.put(pr.getId(), 1);
                break;
            }
        }

        System.out.println("proper to set : " + JSONObject.toJSONString(propties));
        return propties;
    }


}
