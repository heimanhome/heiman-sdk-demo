package com.heiman.sdk.demo;

import com.google.gson.Gson;
import com.heiman.sdk.demo.model.*;
import com.heiman.sdk.restful.client.DeviceModels;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Device demonstration class
 * Integrates gateway and device management functionality, supports thing model loading and demonstration for multiple device types
 * 设备演示类
 * 集成网关与设备管理功能, 支持多种设备类型的物模型加载与演示
 */
public class DeviceDemo {
    private static final Gson gson = new Gson();
    private Map<String, ThingModel> deviceModels = new HashMap<>();

    public static void main(String[] args) throws Exception {
        DeviceDemo demo = new DeviceDemo();
        demo.run();
    }

    /**
     * Run demonstration program
     * 运行演示程序
     */
    public void run() {
        System.out.println("=== Heiman Smart Device Demo Program Startup ===");
        
        // Initialize device manager, load thing models for all device types
        initialize();
        
        // Display overview of all devices
        printAllDevicesOverview();
        
        // Demonstrate specific device features
        demonstrateGatewayFeatures();
        demonstrateSmokeDetectorFeatures();
        demonstrateCoDetectorFeatures();
        demonstrateHeatDetectorFeatures();
        demonstrateRemoteControlFeatures();
        
        System.out.println("\n=== Demo Program Execution Completed ===");
    }

    /**
     * Initialize device manager, load thing models for all device types
     * 初始化设备管理器, 加载所有设备类型的物模型
     */
    public void initialize() {
        loadDeviceModel(DeviceModels.GATEWAY);
        loadDeviceModel(DeviceModels.CO);
        loadDeviceModel(DeviceModels.SMOKE);
        loadDeviceModel(DeviceModels.HEAT);
        loadDeviceModel(DeviceModels.REMOTE_CONTROL);
    }

    /**
     * Load thing model for specified device type from the SDK built-in resources.
     * 从 SDK 内置资源加载指定设备类型的物模型。
     * @param deviceType Device type constant from {@link DeviceModels} / 来自 {@link DeviceModels} 的设备类型常量
     */
    private void loadDeviceModel(String deviceType) {
        try (Reader reader = new InputStreamReader(DeviceModels.getModelStream(deviceType))) {
            ThingModel model = gson.fromJson(reader, ThingModel.class);
            deviceModels.put(deviceType, model);
            System.out.println("Successfully loaded device type: " + deviceType);
        } catch (Exception e) {
            System.err.println("Failed to load device type " + deviceType + ": " + e.getMessage());
        }
    }

    /**
     * Get thing model for specified device type
     * 获取指定设备类型的物模型
     * @param deviceType Device type / 设备类型
     * @return Thing model object / 物模型对象
     */
    public ThingModel getDeviceModel(String deviceType) {
        return deviceModels.get(deviceType);
    }

    /**
     * Print overview information of all devices
     * 打印所有设备的概览信息
     */
    public void printAllDevicesOverview() {
        System.out.println("\n=== Overview of Loaded Device Types ===");
        for (Map.Entry<String, ThingModel> entry : deviceModels.entrySet()) {
            String deviceType = entry.getKey();
            ThingModel model = entry.getValue();
            
            System.out.println("\nDevice type: " + deviceType);
            if (model.getProperties() != null) {
                System.out.println("  Property count: " + model.getProperties().size());
            }
            if (model.getEvents() != null) {
                System.out.println("  Event count: " + model.getEvents().size());
            }
            if (model.getFunctions() != null) {
                System.out.println("  Function count: " + model.getFunctions().size());
            }
        }
    }

    /**
     * Demonstrate gateway device features
     * 演示网关设备功能
     */
    private void demonstrateGatewayFeatures() {
        System.out.println("\n=== Gateway Device Feature Demo ===");
        ThingModel gatewayModel = getDeviceModel("gateway");
        
        if (gatewayModel != null) {
            // Display property information
            System.out.println("\nProperty list:");
            if (gatewayModel.getProperties() != null) {
                for (Property property : gatewayModel.getProperties()) {
                    System.out.println("- " + property.getId() + ": " + property.getName());
                }
            }
            
            // Display event information
            System.out.println("\nEvent list:");
            if (gatewayModel.getEvents() != null) {
                for (Event event : gatewayModel.getEvents()) {
                    System.out.println("- " + event.getId() + ": " + event.getName());
                }
            }
            
            // Display function information
            System.out.println("\nFunction list:");
            if (gatewayModel.getFunctions() != null) {
                for (Function function : gatewayModel.getFunctions()) {
                    System.out.println("- " + function.getId() + ": " + function.getName() + " (Async: " + function.getAsync() + ")");
                }
            }
        }
    }

    /**
     * Demonstrate smoke detector features
     * 演示烟雾探测器功能
     */
    private void demonstrateSmokeDetectorFeatures() {
        System.out.println("\n=== Smoke Detector Feature Demo ===");
        ThingModel smokeModel = getDeviceModel("smoke");
        
        if (smokeModel != null) {
            System.out.println("Functions supported by smoke detector:");
            if (smokeModel.getFunctions() != null) {
                for (Function function : smokeModel.getFunctions()) {
                    System.out.println("- " + function.getName() + " (ID: " + function.getId() + ")");
                }
            }
        }
    }

    /**
     * Demonstrate carbon monoxide detector features
     * 演示一氧化碳探测器功能
     */
    private void demonstrateCoDetectorFeatures() {
        System.out.println("\n=== Carbon Monoxide Detector Feature Demo ===");
        ThingModel coModel = getDeviceModel("co");
        
        if (coModel != null) {
            System.out.println("Events supported by carbon monoxide detector:");
            if (coModel.getEvents() != null) {
                for (Event event : coModel.getEvents()) {
                    System.out.println("- " + event.getName() + " (ID: " + event.getId() + ")");
                }
            }
        }
    }

    /**
     * Demonstrate heat detector features
     * 演示温感探测器功能
     */
    private void demonstrateHeatDetectorFeatures() {
        System.out.println("\n=== Heat Detector Feature Demo ===");
        ThingModel heatModel = getDeviceModel("heat");
        
        if (heatModel != null) {
            System.out.println("Properties supported by heat detector:");
            if (heatModel.getProperties() != null) {
                for (Property property : heatModel.getProperties()) {
                    System.out.println("- " + property.getName() + " (ID: " + property.getId() + ")");
                }
            }
        }
    }

    /**
     * Demonstrate remote control features
     * 演示遥控设备功能
     */
    private void demonstrateRemoteControlFeatures() {
        System.out.println("\n=== Remote Control Feature Demo ===");
        ThingModel remoteModel = getDeviceModel("remote_controll");
        
        if (remoteModel != null) {
            System.out.println("Functions supported by remote control:");
            if (remoteModel.getFunctions() != null) {
                for (Function function : remoteModel.getFunctions()) {
                    System.out.println("- " + function.getName() + " (ID: " + function.getId() + ")");
                    if (function.getInputs() != null && !function.getInputs().isEmpty()) {
                        System.out.println("  Input parameters:");
                        for (FunctionInput input : function.getInputs()) {
                            System.out.println("    - " + input.getName() + " (ID: " + input.getId() + ")");
                        }
                    }
                }
            }
        }
    }
}