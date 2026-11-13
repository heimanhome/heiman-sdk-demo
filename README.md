# Heiman Smart Home SDK Demo (heiman-sdk-demo)

> **Languages:** [English](README.md) · [简体中文](doc/README_CH.md) · [Русский](doc/README_RU.md) · [Deutsch](doc/README_DE.md) · [Français](doc/README_FR.md) · [Italiano](doc/README_IT.md) · [العربية](doc/README_AR.md) · [Español](doc/README_ES.md) · [Português](doc/README_PT.md) · [Polski](doc/README_PL.md)

| Document | Value |
|---|---|
| Language | English |
| Applies to | Heiman Smart Home SDK **v1.0.4** (`heiman-sdk-demo`) |
| Runtime | JDK 8+, Maven 3.6+ |
| Status | Formal documentation |
| Last updated | 2026-11-13 |

## Contents

1. Overview
2. Before You Run (Required Changes)
3. Demo Flow
4. Project Structure
5. Quick Start
6. Configuration Reference
7. Key Code Walkthrough
8. MQTT Callback Reference
9. FAQ
10. Credential & Security Notes
11. Technical Support
12. Version History & License

## 1. Overview

`heiman-sdk-demo` is the official comprehensive demo project for the Heiman Smart Home SDK (v1.0.4). Built around `com.heiman.sdk.demo.DemoApplication`, it demonstrates the two core server-side integration flows end to end:

- **RESTful device management**: automatic OAuth2 login, device bind/unbind, paginated device list query, product list and ThingModel parsing.
- **MQTT device access**: MQTT client initialization, property read/write commands, and the registration and handling of 30+ callbacks covering property reports, event reports, sub-device management, firmware upgrades, scenes, time synchronization, and more.

It also includes a thing-model loading demo (`DeviceDemo`), Web API / OAuth2 call-pattern demos (`client/` package), and an entity subscription / command registration demo (`server/` package).

**Intended audience:** server-side engineers integrating the Heiman IoT platform for the first time. This document covers prerequisites, credential configuration, build and run steps, configuration items, code walkthroughs, the callback inventory, troubleshooting, and security notes. All commands and snippets come from this demo and are ready to use once the placeholders listed in section 2 have been replaced.

The overall SDK integration flow (APP-side provisioning → device binding & reporting → server-side integration) is shown below:

![SDK Integration Flow](image/img.png)

## 2. Before You Run (Required Changes)

> ⚠️ **The repository ships with placeholders only. Three items must be replaced before the Demo can run successfully.**

| # | Item | File / Location | Default value | Action |
|---|---|---|---|---|
| 1 | Application credentials | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | Replace with the real `appId` / `secureKey` issued by the Heiman IoT platform (**required**) |
| 2 | Device SN | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | Replace with a real, bindable device SN |
| 3 | Test property | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | Adjust to a property ID defined in your own product thing model |

**Why this matters:** `SdkConfig` only rejects a missing file, empty values, and unresolved `${...}` placeholders. `YOUR_APP_ID` / `YOUR_SECURE_KEY` are non-empty, so they are accepted as valid credentials and sent to the platform. Leaving them unchanged does **not** raise a "missing configuration" error — OAuth2 login / authentication simply fails (for example, HTTP 401). See section 5.2 for the full configuration procedure.

## 3. Demo Flow

`com.heiman.sdk.demo.DemoApplication#main` is the main entry point and executes in the following order:

1. **DeviceDemo**: loads five built-in thing models (gateway / smoke / CO / heat / remote control) from the SDK built-in resources (`DeviceModels`) and prints an overview of their properties, events, and functions.
2. **testHttpClient() (RESTful)**:
   - Builds a `ClientConfig` from `sdk-config.properties`, creates a `HeimanRestfulClient`, and completes OAuth2 login automatically;
   - Obtains the access token and userId via `getCurrentToken()`;
   - Binds a device with `deviceService.bindUserDevice(deviceSn)`;
   - Queries the device list with pagination (`queryDeviceList`) and prints device details;
   - Queries the product list (`getProductList`), parses each product `metadata` into a `ThingModel`, and caches them by productId;
   - Finally unbinds via `unbindUserDevice(deviceSn)`. **Note: in v1.0.4 this unbind call is active (not commented out); when you need to observe MQ callback messages, manually comment out the unbind call at the end of `testHttpClient()` (including `ApiResult unbind = ...` and the following print statement).**
3. **testMqtt() (MQTT)**:
   - Builds a `MqttConfig` via `SdkConfig.get().toMqttConfig(userId)` (the broker is built into the SDK, no URL configuration needed);
   - Implements all `MqttCallback` methods (property read/write replies, event/property reports, sub-device online/offline, firmware upgrade progress, scene add/update/delete, connection state, etc.);
   - Initializes with `HeimanMqttClient.getInstance().init(mqttConfig, callback)`;
   - Iterates over the device list queried in step 2, builds test properties from the product thing model (the example uses the `FreezingPointEnable` property — adjust `createTestProperties()` according to your own product model), then sends `propertiesWrite` and `propertiesRead` commands.
4. **Cleanup on exit**: a JVM shutdown hook releases resources gracefully by calling `HeimanMqttClient.getInstance().shutdown()` and `restfulClient.close()`.

## 4. Project Structure

```
heiman-sdk-demo/
├── src/main/java/com/heiman/sdk/demo/
│   ├── DemoApplication.java          # Main entry: full RESTful + MQTT flow
│   ├── DeviceDemo.java               # Device thing-model loading & overview demo
│   ├── ProductDemo.java              # Product list query demo (real RESTful call)
│   ├── SdkConfig.java                # Runtime credential loader (sdk-config.properties)
│   ├── client/
│   │   ├── WebApiClientDemo.java     # Web API call-pattern demo (simulated output)
│   │   └── WebOAuth2ClientDemo.java  # OAuth2 call-pattern demo (simulated output)
│   ├── model/                        # Thing-model POJOs (9 files)
│   │   ├── ThingModel.java           #   Thing model (tags/properties/events/functions)
│   │   ├── Property.java             #   Property
│   │   ├── Function.java             #   Function
│   │   ├── FunctionInput.java        #   Function input
│   │   ├── Event.java                #   Event
│   │   ├── ValueType.java            #   Value type
│   │   ├── Tag.java                  #   Tag
│   │   ├── Expands.java              #   Expands
│   │   └── Element.java              #   Element
│   └── server/
│       └── EntitySubscribeDemo.java  # Entity subscription / command registration demo (simulated)
├── src/main/resources/
│   └── sdk-config.properties         # Runtime credentials (appId / secureKey; placeholders by default, replace before running)
├── libs/
│   ├── heiman-sdk-mqtt-1.0.4.jar     # MQTT module (system scope)
│   └── heiman-sdk-restful-1.0.4.jar  # RESTful module (system scope)
├── image/
│   ├── img.png                       # Integration flow diagram (EN, used by README.md and translations in doc/)
│   └── img_1.png                     # Integration flow diagram (CN, used by doc/README_CH.md)
├── doc/
│   ├── README_CH.md                  # 简体中文
│   ├── README_RU.md                  # Русский
│   ├── README_DE.md                  # Deutsch
│   ├── README_FR.md                  # Français
│   ├── README_IT.md                  # Italiano
│   ├── README_AR.md                  # العربية
│   ├── README_ES.md                  # Español
│   ├── README_PT.md                  # Português
│   └── README_PL.md                  # Polski
├── README.md                         # English (default)
├── .gitignore                        # Ignore rules (build output, IDE, secrets)
├── LICENSE                           # Apache-2.0 (source & docs only)
├── NOTICE                            # Licence scope + third-party notices
├── CHANGELOG.md                      # Version history
├── RELEASE_NOTES.md                  # v1.0.4 release notes
├── sbom.json                         # CycloneDX SBOM (mvn -P sbom package)
└── pom.xml
```

## 5. Quick Start

### 5.1 Requirements

- JDK 8+
- Maven 3.6+ (or simply open the project in IDEA)
- Application credentials (`appId` / `secureKey`) issued by the Heiman IoT platform
- A real bindable device (deviceSn)
- Network access to the Heiman IoT platform; the RESTful endpoints and MQTT broker are built into the SDK

### 5.2 Configure credentials

> ⚠️ **You must edit `src/main/resources/sdk-config.properties`**: replace the default placeholders with the real application credentials issued by the Heiman IoT platform before building and running.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

Checklist:

- **Leaving the placeholders does NOT raise a "missing configuration" error**: `SdkConfig` only rejects a missing file, empty values, and unresolved `${...}` placeholders. `YOUR_APP_ID` / `YOUR_SECURE_KEY` are treated as valid values and sent to the platform, so OAuth2 login / authentication fails (e.g. 401) instead of a clear credential error;
- No real credentials are bundled in the repository; if the file is missing or a value is empty, the SDK throws `missing valid SDK credential configuration: appId/secureKey`;
- Keep the key names `appId` / `secureKey` unchanged and save the file as UTF-8. `tenantId` uses the SDK built-in default, and the MQTT broker plus RESTful endpoints are also built into the SDK — **do not add extra entries to this file**;
- After editing, re-run `mvn compile` (or rebuild in IDEA) so that `target/classes/sdk-config.properties` on the classpath is up to date;
- `deviceSn` is not in this file: also replace `String deviceSn = "xxxxxxx"` in `DemoApplication#testHttpClient()` with a real, bindable device SN that is not bound to another user;
- Never commit real credentials to the repository (only placeholders are kept here); if credentials may have leaked, rotate `secureKey` on the IoT platform;
- When the SDK Demo is packaged and downloaded from the IoT platform via `/application/{id}/sdk/download`, this file is automatically replaced with the real credentials of that application — **no manual edit is needed in that case**.

### 5.3 Build

```bash
cd heiman-sdk-demo
mvn compile
```

The SDK dependencies are provided as system-scope jars under `libs/` (plain SDK binaries), so there is no need to install them into your local repository.

### 5.4 Run

> Before running, confirm that step 5.2 is done (`sdk-config.properties` replaced with real credentials and the project rebuilt), and that `deviceSn` and `createTestProperties()` in `DemoApplication` have been adjusted for your device / thing model.

The recommended way is to open the project in IDEA and run:

```
com.heiman.sdk.demo.DemoApplication#main
```

Expected console output, in order: the built-in thing-model overview, a masked access token and userId after login, the paginated device list, the product list with parsed thing-model properties, the MQTT connection message, and callback logs as device messages arrive. The process keeps running so that callbacks can be observed — press `Ctrl+C` to exit, at which point the shutdown hook releases both connections.

### 5.5 Demo classes

| Class | Description |
|---|---|
| `DemoApplication` | Main entry: full RESTful + MQTT flow (real calls) |
| `ProductDemo` | Product list query (real RESTful call) |
| `DeviceDemo` | Thing-model loading and overview printing |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | Web API / OAuth2 call-pattern demos (simulated output) |
| `server.EntitySubscribeDemo` | Entity subscription / command registration demo (simulated output) |

> Note: only `DemoApplication` and `ProductDemo` issue real RESTful / MQTT calls. `DeviceDemo` uses the SDK built-in thing models, while `client/*` and `server/EntitySubscribeDemo` print simulated local output; none of them issue real network requests.

## 6. Configuration Reference

| Item | Source | Must be changed | Description |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | Yes | Application ID issued by the Heiman IoT platform; read by `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | Yes | Application secret; read by `SdkConfig.getSecureKey()`; never commit it |
| `tenantId` | SDK built-in (`HeimanEndpoints.DEFAULT_TENANT_ID`) | No | Returned by `SdkConfig.getTenantId()`; not read from the properties file |
| RESTful endpoints / MQTT broker | SDK built-in | No | No URL configuration is required; `MqttConfig` only needs appId, secureKey, tenantId and userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | Yes | SN of the device to bind; the default `xxxxxxx` is a placeholder |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | No | Login user ID, passed to `toMqttConfig(userId)` at runtime |
| Property ID / value | `DemoApplication#createTestProperties()` | Yes, if your model differs | Must match the property ID and value type defined in your product thing model |

`SdkConfig` exposes `toClientConfig()` (with 30 s connect / 60 s read & write timeouts and logging enabled) and `toMqttConfig(userId)`. Both RESTful and MQTT clients therefore share exactly the same credential source: `sdk-config.properties`.

## 7. Key Code Walkthrough

### 7.1 RESTful client (automatic login)

```java
// Build the client config (credentials come from sdk-config.properties)
ClientConfig clientConfig = SdkConfig.get().toClientConfig();

// Create the RESTful client — OAuth2 login happens automatically in the constructor
HeimanRestfulClient restfulClient = new HeimanRestfulClient(clientConfig);

// Get the current token information
OAuth2Token token = restfulClient.getCurrentToken();
String userId = token.getUserId();

// Bind a device
DeviceService deviceService = restfulClient.getDeviceService();
deviceService.bindUserDevice(deviceSn);

// Query the device list with pagination
DeviceListQueryParam queryParam = new DeviceListQueryParam();
queryParam.getHelp().setPageIndex(0);
queryParam.getHelp().setPageSize(10);
PageResult<DeviceDetail> deviceList = deviceService.queryDeviceList(queryParam);

// Query the product list and parse thing models
ProductService productService = restfulClient.getProductService();
List<ProductInfo> products = productService.getProductList();
ThingModel thingModel = JSONObject.parseObject(product.getMetadata(), ThingModel.class);
```

### 7.2 MQTT client initialization and callbacks

```java
// The broker is built into the SDK; only the logged-in userId is required
MqttConfig mqttConfig = SdkConfig.get().toMqttConfig(userId);

MqttCallback callback = new MqttCallback() {
    @Override
    public void onPropertiesReadReply(String productId, String deviceId,
                                      PropertiesReadResponse response) { ... }

    @Override
    public void onPropertiesWriteReply(String productId, String deviceId,
                                       PropertiesWriteResponse response) { ... }

    @Override
    public void onEventReport(String productId, String deviceId, String eventId,
                              EventReportRequest request) { ... }

    @Override
    public void onPropertiesReport(String productId, String deviceId,
                                   PropertiesReportRequest request) { ... }

    @Override
    public void onChildOnline(String productId, String deviceId, String childDeviceId,
                              ChildDeviceOnlineRequest request) { ... }

    @Override
    public void onFirmwareUpgradeProgress(String productId, String deviceId,
                                          FirmwareUpgradeProgressRequest request) { ... }

    @Override
    public void onConnected(boolean reconnect, String serverURI) { ... }

    @Override
    public void onConnectionLost(Throwable cause) { ... }

    // Remaining callbacks (sub-device management, topology, scenes, time sync, log report,
    // etc.) — see the full implementation in DemoApplication
};

HeimanMqttClient.getInstance().init(mqttConfig, callback);
```

### 7.3 Property read / write

```java
// Property write: keys are thing-model property IDs; value types must match the model
PropertiesWriteRequest writeRequest = new PropertiesWriteRequest();
writeRequest.setProperties(properties);
HeimanMqttClient.getInstance().propertiesWrite(productId, deviceId, writeRequest);

// Property read
PropertiesReadRequest readRequest = new PropertiesReadRequest();
readRequest.setProperties(new ArrayList<>(properties.keySet()));
HeimanMqttClient.getInstance().propertiesRead(productId, deviceId, readRequest);
```

Command results are returned asynchronously through the `onPropertiesWriteReply` / `onPropertiesReadReply` callbacks. Keep the process alive after sending commands so the replies can be observed.

### 7.4 Graceful shutdown

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. MQTT Callback Reference

`MqttCallback` is implemented in full in `DemoApplication#testMqtt()`. The 30+ callbacks are grouped below; each representative name can be found in the source file.

| Category | Representative callbacks | Purpose |
|---|---|---|
| Connection | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | Connection state, reconnect notification, delivery confirmation |
| Properties & functions | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | Replies to commands sent from the server |
| Reports | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | Device-side data, events, metadata and online/offline state |
| Sub-devices | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | Sub-device lifecycle, topology and child-device messages |
| Firmware | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | OTA upgrade progress and firmware information |
| Scenes & system | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | Scene management, time synchronization, logs and reset reports |

> Note: the callback bodies in the Demo only print the received payload. In a real integration, replace them with your own business logic (persistence, alerting, forwarding, etc.).

## 9. FAQ

### Q1: Startup fails with "missing valid SDK credential configuration", or OAuth2 login fails / 401

**A:** Check `src/main/resources/sdk-config.properties` first:

1. The file is missing, `appId` / `secureKey` are empty, or still `${...}` placeholders → fill in real credentials and rebuild;
2. The file still contains the default placeholders `YOUR_APP_ID` / `YOUR_SECURE_KEY` → placeholders are not rejected by validation and are used as real credentials, so the symptom is a login / authentication failure rather than a configuration error. Replace them with real credentials;
3. Credentials that do not belong to the device's application also cause authentication failures — use the credentials issued for that application on the IoT platform.

### Q2: Device binding fails

**A:** Make sure `String deviceSn = "xxxxxxx"` in `DemoApplication#testHttpClient()` has been replaced with a valid, bindable device SN and the device is not bound to another user; also confirm that `sdk-config.properties` was updated as described in section 5.2.

### Q3: No messages received on MQ callbacks

**A:** The `unbindUserDevice(deviceSn)` call at the end of `testHttpClient()` unbinds the device, so subsequent device messages will not arrive. In v1.0.4 this call is **not commented out** — comment it out manually (including `ApiResult unbind = ...` and the print statement) when debugging MQ callbacks. Also verify that the credentials are valid and `deviceSn` is a real device SN.

### Q4: Property write gets no response

**A:** Check in order: whether the device is online; whether the property ID matches the product thing model; whether the value type matches the model definition. The `FreezingPointEnable` property in the example is for demonstration only — adjust `createTestProperties()` according to your own product model.

### Q5: Connection lost

**A:** Use the `onConnectionLost` callback to inspect the cause; after a successful reconnect the SDK notifies via `onConnected(reconnect, serverURI)` with `reconnect = true`.

### Q6: Maven cannot resolve the SDK dependencies

**A:** The two SDK jars are referenced with `system` scope from `libs/`. Make sure `libs/heiman-sdk-mqtt-1.0.4.jar` and `libs/heiman-sdk-restful-1.0.4.jar` exist at those exact paths and that you build from the `heiman-sdk-demo` directory (`${project.basedir}` must point to it).

## 10. Credential & Security Notes

- The repository contains placeholders only; never commit real `appId` / `secureKey` values, and add your local configuration to `.gitignore` if you keep real credentials outside version control.
- When the SDK Demo is packaged via `/application/{id}/sdk/download`, the platform replaces `sdk-config.properties` with the real credentials of that application — keep that downloaded package private.
- `DemoApplication` prints only a masked access token (`abcd****wxyz`); never print full tokens, `secureKey`, or personal data in production logs.
- If a credential may have leaked, rotate `secureKey` on the IoT platform immediately and rebuild/redeploy the Demo with the new value.
- The Demo binds and unbinds real devices and sends real commands. Use a dedicated test application and test devices; do not run it against production data.
- `target/classes/sdk-config.properties` is a compiled copy of your credentials — remove build output before sharing or archiving the project.

## 11. Technical Support

- **Email:** heimanapp1@heiman.cn
- **Website:** https://www.heimanhome.com

## 12. Version History & License

### 12.1 Version History

See [CHANGELOG.md](CHANGELOG.md) for the full history and [RELEASE_NOTES.md](RELEASE_NOTES.md) for this release.

- **v1.0.4:** Current version
   - Runtime credentials managed centrally via `sdk-config.properties`; the file contains placeholders only and must be replaced with a real `appId` / `secureKey` before running
   - `DemoApplication` as the main entry integrating the full RESTful + MQTT flow
   - Credentials auto-replaced when the SDK Demo is packaged via `/application/{id}/sdk/download` on the IoT platform
   - Graceful resource release through a JVM shutdown hook
   - Comprehensive MQTT callback coverage (properties / events / sub-devices / firmware / scenes, etc.)

### 12.2 License

The source code and documentation in this repository are licensed under the **Apache License, Version 2.0** — see [LICENSE](LICENSE). The proprietary SDK binaries in `libs/` and the diagrams in `image/` are **not** covered; see [NOTICE](NOTICE) for the exact scope and third-party notices.
