package com.heiman.sdk.demo.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Entity Subscription Demo
 * 实体订阅Demo
 * Demonstrates entity subscription and command registration handling functionality
 * 演示实体订阅和命令注册处理功能
 */
public class EntitySubscribeDemo {

    // Simulated subscription storage - 模拟订阅存储
    // 注意 (R11): 此处为实例级存储, 仅在 demo 生命周期内存在。生产环境若改为静态/单例长期持有,
    // 应使用带容量上限与 TTL 过期的缓存 (如 Caffeine/Guava), 并显式 unregister 清理, 避免内存泄漏。
    private final Map<String, SubscriptionInfo> subscriptions = new ConcurrentHashMap<>();
    private final AtomicInteger subscriptionCounter = new AtomicInteger(0);

    // Simulated command registration storage - 模拟命令注册存储
    private final Map<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();
    private final AtomicInteger commandCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        EntitySubscribeDemo demo = new EntitySubscribeDemo();
        demo.run();
    }

    public void run() {
        System.out.println("=== Entity Subscription Demo Started ==="); // === 实体订阅Demo启动 ===
        
        // Demonstrate entity subscription - 演示实体订阅
        demonstrateEntitySubscription();
        
        // Demonstrate command registration - 演示命令注册
        demonstrateCommandRegistration();
        
        // Demonstrate subscription management - 演示订阅管理
        demonstrateSubscriptionManagement();
        
        // Demonstrate event dispatch - 演示事件分发
        demonstrateEventDispatch();
        
        System.out.println("\n=== Entity Subscription Demo Ended ==="); // === 实体订阅Demo结束 ===
        stop(); // 演示结束释放订阅/命令注册存储 (R11)
    }

    /**
     * 释放本 demo 持有的订阅与命令注册存储 (R11)。
     * <p>生产环境应在实体取消订阅/命令注销时按需调用, 避免存储无限累积导致内存泄漏。</p>
     */
    public void stop() {
        subscriptions.clear();
        commandHandlers.clear();
        subscriptionCounter.set(0);
        commandCounter.set(0);
    }

    /**
     * Demonstrate entity subscription functionality
     * 演示实体订阅功能
     */
    private void demonstrateEntitySubscription() {
        System.out.println("\n--- Demonstrating Entity Subscription ---"); // --- 演示实体订阅 ---
        try {
            // Subscribe to device status changes - 订阅设备状态变化
            String deviceId1 = "device_001";
            String deviceId2 = "device_002";
            
            System.out.println("Subscribing to device status changes..."); // 订阅设备状态变化...
            
            // Create subscriptions - 创建订阅
            String subscription1 = subscribeToEntity(deviceId1, "status_change", new StatusChangeHandler());
            String subscription2 = subscribeToEntity(deviceId2, "property_update", new PropertyUpdateHandler());
            String subscription3 = subscribeToEntity(deviceId1, "alarm", new AlarmHandler());
            
            System.out.println("Created subscription: " + subscription1 + " (device: " + deviceId1 + ", event: status_change)"); // 创建订阅
            System.out.println("Created subscription: " + subscription2 + " (device: " + deviceId2 + ", event: property_update)"); // 创建订阅
            System.out.println("Created subscription: " + subscription3 + " (device: " + deviceId1 + ", event: alarm)"); // 创建订阅
            
            // Display current subscription status - 显示当前订阅状态
            System.out.println("\nCurrent total subscriptions: " + subscriptions.size()); // 当前订阅总数
            
            // Simulate event triggers - 模拟事件触发
            System.out.println("\nSimulating event triggers..."); // 模拟事件触发...
            triggerEvent(deviceId1, "status_change", "Device online"); // 设备上线
            triggerEvent(deviceId2, "property_update", "Temperature: 25.5°C"); // 温度: 25.5°C
            triggerEvent(deviceId1, "alarm", "High temperature alarm"); // 高温报警
            
        } catch (Exception e) {
            System.err.println("Entity subscription demo failed: " + e.getMessage()); // 实体订阅演示失败
        }
    }

    /**
     * Demonstrate command registration functionality
     * 演示命令注册功能
     */
    private void demonstrateCommandRegistration() {
        System.out.println("\n--- Demonstrating Command Registration ---"); // --- 演示命令注册 ---
        try {
            System.out.println("Registering device control commands..."); // 注册设备控制命令...
            
            // Register various command handlers - 注册各种命令处理器
            String cmd1 = registerCommand("turn_on", new TurnOnCommandHandler());
            String cmd2 = registerCommand("turn_off", new TurnOffCommandHandler());
            String cmd3 = registerCommand("set_temperature", new SetTemperatureCommandHandler());
            String cmd4 = registerCommand("get_status", new GetStatusCommandHandler());
            
            System.out.println("Registered command: " + cmd1 + " (turn_on)"); // 注册命令
            System.out.println("Registered command: " + cmd2 + " (turn_off)"); // 注册命令
            System.out.println("Registered command: " + cmd3 + " (set_temperature)"); // 注册命令
            System.out.println("Registered command: " + cmd4 + " (get_status)"); // 注册命令
            
            System.out.println("\nCurrent total registered commands: " + commandHandlers.size()); // 当前注册命令总数
            
            // Simulate command execution - 模拟命令执行
            System.out.println("\nSimulating command execution..."); // 模拟命令执行...
            executeCommand("turn_on", "device_001");
            executeCommand("set_temperature", "device_002", "23.5");
            executeCommand("get_status", "device_001");
            executeCommand("turn_off", "device_001");
            
        } catch (Exception e) {
            System.err.println("Command registration demo failed: " + e.getMessage()); // 命令注册演示失败
        }
    }

    /**
     * Demonstrate subscription management functionality
     * 演示订阅管理功能
     */
    private void demonstrateSubscriptionManagement() {
        System.out.println("\n--- Demonstrating Subscription Management ---"); // --- 演示订阅管理 ---
        try {
            // View all subscriptions - 查看所有订阅
            System.out.println("All current subscriptions:"); // 当前所有订阅:
            subscriptions.forEach((id, info) -> {
                System.out.println("  Subscription ID: " + id + ", Entity: " + info.entityId + ", Event: " + info.eventType); // 订阅ID, 实体, 事件
            });
            
            // Find subscriptions by entity - 按实体查找订阅
            String targetEntity = "device_001";
            System.out.println("\nSubscriptions for entity '" + targetEntity + "':"); // 实体的订阅
            subscriptions.entrySet().stream()
                .filter(entry -> targetEntity.equals(entry.getValue().entityId))
                .forEach(entry -> {
                    SubscriptionInfo info = entry.getValue();
                    System.out.println("  Subscription ID: " + entry.getKey() + ", Event: " + info.eventType); // 订阅ID, 事件
                });
            
            // Cancel some subscriptions - 取消部分订阅
            System.out.println("\nCanceling some subscriptions..."); // 取消部分订阅...
            List<String> toRemove = new ArrayList<>();
            subscriptions.entrySet().stream()
                .filter(entry -> "alarm".equals(entry.getValue().eventType))
                .forEach(entry -> toRemove.add(entry.getKey()));
            
            toRemove.forEach(id -> {
                SubscriptionInfo removed = subscriptions.remove(id);
                if (removed != null) {
                    System.out.println("Canceled subscription: " + id + " (" + removed.eventType + ")"); // 取消订阅
                }
            });
            
            System.out.println("Remaining subscriptions: " + subscriptions.size()); // 剩余订阅数
            
        } catch (Exception e) {
            System.err.println("Subscription management demo failed: " + e.getMessage()); // 订阅管理演示失败
        }
    }

    /**
     * Demonstrate event dispatch functionality
     * 演示事件分发功能
     */
    private void demonstrateEventDispatch() {
        System.out.println("\n--- Demonstrating Event Dispatch ---"); // --- 演示事件分发 ---
        try {
            // Batch event dispatch - 批量事件分发
            System.out.println("Batch event dispatch test..."); // 批量事件分发测试...
            
            String[] devices = {"device_001", "device_002", "device_003"};
            String[] events = {"status_change", "property_update", "heartbeat"};
            
            for (String device : devices) {
                for (String event : events) {
                    // Create subscription for each device's each event - 为每个设备的每种事件创建订阅
                    String subId = subscribeToEntity(device, event, new GenericEventHandler());
                    System.out.println("Created subscription: " + device + " -> " + event); // 创建订阅
                }
            }
            
            System.out.println("\nTotal subscriptions: " + subscriptions.size()); // 总订阅数
            
            // Simulate massive event triggers - 模拟大量事件触发
            System.out.println("\nSimulating massive event triggers..."); // 模拟大量事件触发...
            int eventCount = 0;
            for (String device : devices) {
                for (String event : events) {
                    triggerEvent(device, event, "Event data_" + (++eventCount)); // 事件数据_
                }
            }
            
            System.out.println("Total triggered events: " + eventCount); // 总共触发事件: %d 个
            
        } catch (Exception e) {
            System.err.println("Event dispatch demo failed: " + e.getMessage()); // 事件分发演示失败
        }
    }

    // Subscribe to entity events - 订阅实体事件
    private String subscribeToEntity(String entityId, String eventType, EventHandler handler) {
        String subscriptionId = "sub_" + subscriptionCounter.incrementAndGet();
        SubscriptionInfo info = new SubscriptionInfo(entityId, eventType, handler);
        subscriptions.put(subscriptionId, info);
        return subscriptionId;
    }

    // Register command handler - 注册命令处理器
    private String registerCommand(String commandType, CommandHandler handler) {
        String commandId = "cmd_" + commandCounter.incrementAndGet();
        commandHandlers.put(commandType, handler);
        return commandId;
    }

    // Trigger event - 触发事件
    private void triggerEvent(String entityId, String eventType, String eventData) {
        subscriptions.entrySet().stream()
            .filter(entry -> {
                SubscriptionInfo info = entry.getValue();
                return entityId.equals(info.entityId) && eventType.equals(info.eventType);
            })
            .forEach(entry -> {
                try {
                    entry.getValue().handler.handleEvent(entityId, eventType, eventData);
                } catch (Exception e) {
                    System.err.println("Event handling failed: " + e.getMessage()); // 事件处理失败
                }
            });
    }

    // Execute command - 执行命令
    private void executeCommand(String commandType, String targetEntity, String... params) {
        CommandHandler handler = commandHandlers.get(commandType);
        if (handler != null) {
            try {
                handler.handleCommand(targetEntity, params);
            } catch (Exception e) {
                System.err.println("Command execution failed: " + e.getMessage()); // 命令执行失败
            }
        } else {
            System.err.println("Command handler not found: " + commandType); // 未找到命令处理器
        }
    }

    // Subscription information class - 订阅信息类
    private static class SubscriptionInfo {
        final String entityId;
        final String eventType;
        final EventHandler handler;

        SubscriptionInfo(String entityId, String eventType, EventHandler handler) {
            this.entityId = entityId;
            this.eventType = eventType;
            this.handler = handler;
        }
    }

    // Event handler interface - 事件处理器接口
    private interface EventHandler {
        void handleEvent(String entityId, String eventType, String eventData);
    }

    // Command handler interface - 命令处理器接口
    private interface CommandHandler {
        void handleCommand(String targetEntity, String... params);
    }

    // Concrete event handler implementations - 具体的事件处理器实现
    private static class StatusChangeHandler implements EventHandler {
        @Override
        public void handleEvent(String entityId, String eventType, String eventData) {
            System.out.println("[Status Change] Entity: " + entityId + ", Data: " + eventData); // [状态变化] 实体, 数据
        }
    }

    private static class PropertyUpdateHandler implements EventHandler {
        @Override
        public void handleEvent(String entityId, String eventType, String eventData) {
            System.out.println("[Property Update] Entity: " + entityId + ", Data: " + eventData); // [属性更新] 实体, 数据
        }
    }

    private static class AlarmHandler implements EventHandler {
        @Override
        public void handleEvent(String entityId, String eventType, String eventData) {
            System.out.println("[Alarm Event] Entity: " + entityId + ", Data: " + eventData); // [报警事件] 实体, 数据
        }
    }

    private static class GenericEventHandler implements EventHandler {
        @Override
        public void handleEvent(String entityId, String eventType, String eventData) {
            System.out.println("[Generic Event] Type: " + eventType + ", Entity: " + entityId + ", Data: " + eventData); // [通用事件] 类型, 实体, 数据
        }
    }

    // Concrete command handler implementations - 具体的命令处理器实现
    private static class TurnOnCommandHandler implements CommandHandler {
        @Override
        public void handleCommand(String targetEntity, String... params) {
            System.out.println("[Turn On Command] Target: " + targetEntity + " -> Device turned on"); // [开启命令] 目标 -> 设备已开启
        }
    }

    private static class TurnOffCommandHandler implements CommandHandler {
        @Override
        public void handleCommand(String targetEntity, String... params) {
            System.out.println("[Turn Off Command] Target: " + targetEntity + " -> Device turned off"); // [关闭命令] 目标 -> 设备已关闭
        }
    }

    private static class SetTemperatureCommandHandler implements CommandHandler {
        @Override
        public void handleCommand(String targetEntity, String... params) {
            String temperature = params.length > 0 ? params[0] : "Unknown"; // 未知
            System.out.println("[Set Temperature] Target: " + targetEntity + " -> Temperature set to: " + temperature); // [设置温度] 目标 -> 温度设置为
        }
    }

    private static class GetStatusCommandHandler implements CommandHandler {
        @Override
        public void handleCommand(String targetEntity, String... params) {
            System.out.println("[Get Status] Target: " + targetEntity + " -> Status: online, Temperature: 24.5°C, Humidity: 60%"); // [获取状态] 目标 -> 状态: 在线, 温度, 湿度
        }
    }
}