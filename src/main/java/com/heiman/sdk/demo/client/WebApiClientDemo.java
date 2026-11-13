package com.heiman.sdk.demo.client;

import java.util.Map;
import java.util.HashMap;

/**
 * Web API Client Demo
 * Demonstrates how to use WebApiClient for API calls
 * 原文：Web API客户端Demo
 * 原文：演示如何使用WebApiClient进行API调用
 */
public class WebApiClientDemo {

    public static void main(String[] args) {
        WebApiClientDemo demo = new WebApiClientDemo();
        demo.run();
    }

    public void run() {
        System.out.println("=== Web API Client Demo Started ==="); // 原文：=== Web API客户端Demo启动 ===
        
        // Configure client
        // Simulate client configuration
        // 原文：配置客户端
        // 原文：模拟客户端配置
        Map<String, String> clientConfig = new HashMap<>();
        clientConfig.put("baseUrl", "https://spapi.heiman.cn");
        clientConfig.put("clientId", "YOUR_CLIENT_ID");  // Please replace with actual client ID // 原文：请替换为实际的客户端ID
        clientConfig.put("clientSecret", "YOUR_CLIENT_SECRET");  // Please replace with actual secret key // 原文：请替换为实际的密钥
        
        System.out.println("Web API client created successfully (simulated)"); // 原文：Web API客户端创建成功（模拟）

        try {
            // Simulate device information query
            // 原文：模拟查询设备信息
            System.out.println("Querying device information..."); // 原文：正在查询设备信息...
            
            System.out.println("Query parameters:"); // 原文：查询参数:
            System.out.println("  Product ID: 1734847105526476800"); // 原文：产品ID
            System.out.println("  Pagination: 0-100"); // 原文：分页
            
            System.out.println("\nQuery successful (simulated)!"); // 原文：查询成功（模拟）!
            System.out.println("Total count: 25"); // 原文：总数量
            System.out.println("Current page device count: 25"); // 原文：当前页设备数量
            System.out.println("Device list:"); // 原文：设备列表
            System.out.println("  - Device 1: Smart Temperature and Humidity Sensor (Online)"); // 原文：设备1: 智能温湿度传感器 (在线)
            System.out.println("  - Device 2: Smart Door Lock (Offline)"); // 原文：设备2: 智能门锁 (离线)
            System.out.println("  - Device 3: Smart Switch (Online)"); // 原文：设备3: 智能开关 (在线)
            
        } catch (Exception e) {
            System.err.println("API call exception: " + e.getMessage()); // 原文：API调用异常
            e.printStackTrace();
        }
        
        System.out.println("\n=== Web API Client Demo Ended ==="); // 原文：=== Web API客户端Demo结束 ===
    }

    /**
     * Demonstrate client configuration
     * 原文：演示客户端配置
     */
    private void demonstrateClientConfiguration() {
        System.out.println("\n--- Demonstrate Client Configuration ---"); // 原文：--- 演示客户端配置 ---
        try {
            // Simulate client configuration
            // 原文：模拟客户端配置
            Map<String, Object> config = new HashMap<>();
            config.put("baseUrl", "https://spapi.heiman.cn");
            config.put("connectTimeout", 30);
            config.put("readTimeout", 60);
            config.put("maxRetries", 3);
            
            System.out.println("Client configuration:"); // 原文：客户端配置
            System.out.println("  Base URL: " + config.get("baseUrl")); // 原文：基础URL
            System.out.println("  Connection timeout: " + config.get("connectTimeout") + " seconds"); // 原文：连接超时...秒
            System.out.println("  Read timeout: " + config.get("readTimeout") + " seconds"); // 原文：读取超时...秒
            System.out.println("  Max retries: " + config.get("maxRetries") + " times"); // 原文：最大重试...次
            
            System.out.println("Web API client created successfully (simulated)"); // 原文：Web API客户端创建成功（模拟）
            
        } catch (Exception e) {
            System.err.println("Client configuration failed: " + e.getMessage()); // 原文：客户端配置失败
        }
    }

    /**
     * Demonstrate device information query
     * 原文：演示设备信息查询
     */
    private void demonstrateDeviceInfoQuery() {
        System.out.println("\n--- Demonstrate Device Information Query ---"); // 原文：--- 演示设备信息查询 ---
        try {
            // Simulate query request
            // 原文：模拟查询请求
            Map<String, String> request = new HashMap<>();
            request.put("deviceId", "device_12345");
            request.put("productId", "product_67890");
            
            System.out.println("Query device information:"); // 原文：查询设备信息
            System.out.println("  Device ID: " + request.get("deviceId")); // 原文：设备ID
            System.out.println("  Product ID: " + request.get("productId")); // 原文：产品ID
            
            System.out.println("\nDevice information query successful (simulated):"); // 原文：设备信息查询成功（模拟）
            System.out.println("  Device name: Smart Temperature and Humidity Sensor"); // 原文：设备名称: 智能温湿度传感器
            System.out.println("  Device status: Online"); // 原文：设备状态: 在线
            System.out.println("  Last report time: 2024-01-15 14:30:00"); // 原文：最后上报时间
            System.out.println("  Firmware version: v1.2.3"); // 原文：固件版本
            System.out.println("  Signal strength: -45 dBm"); // 原文：信号强度
            
        } catch (Exception e) {
            System.err.println("Device information query failed: " + e.getMessage()); // 原文：设备信息查询失败
        }
    }
}