# Demo del SDK de Hogar Inteligente Heiman (heiman-sdk-demo)

> **Languages:** [English](../README.md) · [简体中文](README_CH.md) · [Русский](README_RU.md) · [Deutsch](README_DE.md) · [Français](README_FR.md) · [Italiano](README_IT.md) · [العربية](README_AR.md) · [Español](README_ES.md) · [Português](README_PT.md) · [Polski](README_PL.md)

| Información del documento | Valor |
|---|---|
| Idioma | Español |
| Se aplica a | SDK de Hogar Inteligente Heiman **v1.0.4** (`heiman-sdk-demo`) |
| Entorno de ejecución | JDK 8+, Maven 3.6+ |
| Estado | Documentación oficial |
| Última actualización | 2026-11-13 |

## Índice

1. Descripción general
2. Antes de ejecutar (cambios obligatorios)
3. Flujo del Demo
4. Estructura del proyecto
5. Inicio rápido
6. Referencia de configuración
7. Análisis del código clave
8. Referencia de callbacks MQTT
9. Preguntas frecuentes
10. Notas sobre credenciales y seguridad
11. Soporte técnico
12. Historial de versiones y licencia

## 1. Descripción general

`heiman-sdk-demo` es el proyecto de demostración oficial y completo del SDK de Hogar Inteligente Heiman (v1.0.4). Estructurado en torno a `com.heiman.sdk.demo.DemoApplication`, muestra de extremo a extremo los dos flujos de integración principales en el lado del servidor:

- **Gestión de dispositivos RESTful**: inicio de sesión OAuth2 automático, vinculación / desvinculación de dispositivos, consulta paginada de la lista de dispositivos, lista de productos y análisis del ThingModel.
- **Acceso a dispositivos MQTT**: inicialización del cliente MQTT, comandos de lectura / escritura de propiedades y el registro y la gestión de más de 30 callbacks que cubren informes de propiedades, informes de eventos, gestión de subdispositivos, actualizaciones de firmware, escenas, sincronización de hora y mucho más.

También incluye una demostración de carga del ThingModel (`DeviceDemo`), demostraciones de patrones de llamada a Web API / OAuth2 (paquete `client/`) y una demostración de suscripción de entidades / registro de comandos (paquete `server/`).

**Público destinatario:** ingenieros del lado del servidor que integran por primera vez la plataforma IoT de Heiman. Este documento cubre los requisitos previos, la configuración de credenciales, los pasos de compilación y ejecución, los elementos de configuración, el análisis del código, el inventario de callbacks, la resolución de problemas y las notas de seguridad. Todos los comandos y fragmentos proceden de este demo y están listos para usar una vez sustituidos los marcadores de posición indicados en la sección 2.

El flujo general de integración del SDK (aprovisionamiento en la APP → vinculación e informes de dispositivos → integración en el servidor) se muestra a continuación:

![Flujo de integración del SDK](../image/img.png)

## 2. Antes de ejecutar (cambios obligatorios)

> ⚠️ **El repositorio solo incluye marcadores de posición. Es necesario sustituir tres elementos antes de que el Demo pueda ejecutarse correctamente.**

| # | Elemento | Archivo / ubicación | Valor predeterminado | Acción |
|---|---|---|---|---|
| 1 | Credenciales de la aplicación | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | Sustituir por los `appId` / `secureKey` reales emitidos por la plataforma IoT de Heiman (**obligatorio**) |
| 2 | SN del dispositivo | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | Sustituir por un SN de dispositivo real y vinculable |
| 3 | Propiedad de prueba | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | Ajustar a un ID de propiedad definido en el ThingModel de su propio producto |

**Por qué es importante:** `SdkConfig` solo rechaza la ausencia del archivo, los valores vacíos y los marcadores `${...}` sin resolver. `YOUR_APP_ID` / `YOUR_SECURE_KEY` no están vacíos, por lo que se aceptan como credenciales válidas y se envían a la plataforma. Dejarlos sin cambios **no** genera un error de "configuración ausente": simplemente falla el inicio de sesión / la autenticación OAuth2 (por ejemplo, HTTP 401). Consulte la sección 5.2 para el procedimiento completo de configuración.

## 3. Flujo del Demo

`com.heiman.sdk.demo.DemoApplication#main` es el punto de entrada principal y se ejecuta en el siguiente orden:

1. **DeviceDemo**: carga cinco ThingModel integrados (gateway / humo / CO / calor / control remoto) desde los recursos integrados del SDK (`DeviceModels`) e imprime un resumen de sus propiedades, eventos y funciones.
2. **testHttpClient() (RESTful)**:
   - Crea un `ClientConfig` a partir de `sdk-config.properties`, crea un `HeimanRestfulClient` y completa automáticamente el inicio de sesión OAuth2;
   - Obtiene el access token y el userId mediante `getCurrentToken()`;
   - Vincula un dispositivo con `deviceService.bindUserDevice(deviceSn)`;
   - Consulta la lista de dispositivos con paginación (`queryDeviceList`) e imprime los detalles de los dispositivos;
   - Consulta la lista de productos (`getProductList`), analiza el `metadata` de cada producto en un `ThingModel` y los almacena en caché por productId;
   - Finalmente desvincula mediante `unbindUserDevice(deviceSn)`. **Nota: en la v1.0.4 esta llamada de desvinculación está activa (no comentada); cuando necesite observar los mensajes de los callbacks MQ, comente manualmente la llamada de desvinculación al final de `testHttpClient()` (incluidos `ApiResult unbind = ...` y la instrucción de impresión siguiente).**
3. **testMqtt() (MQTT)**:
   - Crea un `MqttConfig` mediante `SdkConfig.get().toMqttConfig(userId)` (el broker está integrado en el SDK, no se necesita configurar ninguna URL);
   - Implementa todos los métodos de `MqttCallback` (respuestas de lectura / escritura de propiedades, informes de eventos / propiedades, subdispositivos en línea / fuera de línea, progreso de actualización de firmware, alta / modificación / eliminación de escenas, estado de la conexión, etc.);
   - Inicializa con `HeimanMqttClient.getInstance().init(mqttConfig, callback)`;
   - Recorre la lista de dispositivos consultada en el paso 2, construye las propiedades de prueba a partir del ThingModel del producto (el ejemplo usa la propiedad `FreezingPointEnable`; ajuste `createTestProperties()` según su propio modelo de producto) y envía los comandos `propertiesWrite` y `propertiesRead`.
4. **Limpieza al salir**: un hook de apagado de la JVM libera los recursos de forma ordenada llamando a `HeimanMqttClient.getInstance().shutdown()` y `restfulClient.close()`.

## 4. Estructura del proyecto

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

## 5. Inicio rápido

### 5.1 Requisitos

- JDK 8+
- Maven 3.6+ (o simplemente abra el proyecto en IDEA)
- Credenciales de la aplicación (`appId` / `secureKey`) emitidas por la plataforma IoT de Heiman
- Un dispositivo real vinculable (deviceSn)
- Acceso de red a la plataforma IoT de Heiman; los endpoints RESTful y el broker MQTT están integrados en el SDK

### 5.2 Configuración de credenciales

> ⚠️ **Debe editar `src/main/resources/sdk-config.properties`**: sustituya los marcadores de posición predeterminados por las credenciales reales de la aplicación emitidas por la plataforma IoT de Heiman antes de compilar y ejecutar.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

Lista de comprobación:

- **Dejar los marcadores de posición NO genera un error de "configuración ausente"**: `SdkConfig` solo rechaza la ausencia del archivo, los valores vacíos y los marcadores `${...}` sin resolver. `YOUR_APP_ID` / `YOUR_SECURE_KEY` se tratan como valores válidos y se envían a la plataforma, por lo que falla el inicio de sesión / la autenticación OAuth2 (p. ej., 401) en lugar de mostrarse un error claro de credenciales;
- El repositorio no incluye credenciales reales; si falta el archivo o algún valor está vacío, el SDK lanza `missing valid SDK credential configuration: appId/secureKey`;
- Mantenga sin cambios los nombres de clave `appId` / `secureKey` y guarde el archivo en UTF-8. `tenantId` usa el valor predeterminado integrado del SDK, y el broker MQTT y los endpoints RESTful también están integrados en el SDK — **no añada entradas adicionales a este archivo**;
- Tras la edición, vuelva a ejecutar `mvn compile` (o recompile en IDEA) para que `target/classes/sdk-config.properties` en el classpath esté actualizado;
- `deviceSn` no está en este archivo: sustituya también `String deviceSn = "xxxxxxx"` en `DemoApplication#testHttpClient()` por un SN de dispositivo real, vinculable y no vinculado a otro usuario;
- No confirme nunca credenciales reales en el repositorio (aquí solo se conservan marcadores de posición); si las credenciales pudieran haberse filtrado, rote `secureKey` en la plataforma IoT;
- Cuando el Demo del SDK se empaqueta y se descarga desde la plataforma IoT mediante `/application/{id}/sdk/download`, este archivo se sustituye automáticamente por las credenciales reales de esa aplicación — **en ese caso no se necesita ninguna edición manual**.

### 5.3 Compilación

```bash
cd heiman-sdk-demo
mvn compile
```

Las dependencias del SDK se proporcionan como jars con ámbito `system` en `libs/` (binarios SDK sin ofuscar), por lo que no es necesario instalarlas en el repositorio local.

### 5.4 Ejecución

> Antes de ejecutar, confirme que el paso 5.2 se ha completado (`sdk-config.properties` sustituido por las credenciales reales y el proyecto recompilado) y que `deviceSn` y `createTestProperties()` en `DemoApplication` se han ajustado a su dispositivo / ThingModel.

La forma recomendada es abrir el proyecto en IDEA y ejecutar:

```
com.heiman.sdk.demo.DemoApplication#main
```

La salida esperada por consola, en orden: el resumen de los ThingModel integrados, un access token enmascarado y el userId tras el inicio de sesión, la lista paginada de dispositivos, la lista de productos con las propiedades del ThingModel analizadas, el mensaje de conexión MQTT y los registros de los callbacks a medida que llegan los mensajes de los dispositivos. El proceso permanece en ejecución para poder observar los callbacks — pulse `Ctrl+C` para salir, momento en que el hook de apagado libera ambas conexiones.

### 5.5 Clases de demostración

| Clase | Descripción |
|---|---|
| `DemoApplication` | Punto de entrada principal: flujo RESTful + MQTT completo (llamadas reales) |
| `ProductDemo` | Consulta de la lista de productos (llamada RESTful real) |
| `DeviceDemo` | Carga del ThingModel e impresión del resumen |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | Demostraciones de patrones de llamada a Web API / OAuth2 (salida simulada) |
| `server.EntitySubscribeDemo` | Demostración de suscripción de entidades / registro de comandos (salida simulada) |

> Nota: solo `DemoApplication` y `ProductDemo` realizan llamadas RESTful / MQTT reales. `DeviceDemo` utiliza los ThingModel integrados en el SDK, mientras que `client/*` y `server/EntitySubscribeDemo` imprimen una salida local simulada; ninguno de ellos genera solicitudes de red reales.

## 6. Referencia de configuración

| Elemento | Origen | Debe modificarse | Descripción |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | Sí | ID de la aplicación emitido por la plataforma IoT de Heiman; leído por `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | Sí | Secreto de la aplicación; leído por `SdkConfig.getSecureKey()`; no lo confirme nunca en el repositorio |
| `tenantId` | Integrado en el SDK (`HeimanEndpoints.DEFAULT_TENANT_ID`) | No | Devuelto por `SdkConfig.getTenantId()`; no se lee del archivo de propiedades |
| Endpoints RESTful / broker MQTT | Integrados en el SDK | No | No se requiere configuración de URL; `MqttConfig` solo necesita appId, secureKey, tenantId y userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | Sí | SN del dispositivo a vincular; el valor predeterminado `xxxxxxx` es un marcador de posición |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | No | ID del usuario que inició sesión, pasado a `toMqttConfig(userId)` en tiempo de ejecución |
| ID de propiedad / valor | `DemoApplication#createTestProperties()` | Sí, si su modelo difiere | Debe coincidir con el ID y el tipo de valor de la propiedad definidos en el ThingModel de su producto |

`SdkConfig` expone `toClientConfig()` (con tiempos de espera de conexión de 30 s, de lectura y escritura de 60 s y registro habilitado) y `toMqttConfig(userId)`. Por tanto, los clientes RESTful y MQTT comparten exactamente el mismo origen de credenciales: `sdk-config.properties`.

## 7. Análisis del código clave

### 7.1 Cliente RESTful (inicio de sesión automático)

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

### 7.2 Inicialización del cliente MQTT y callbacks

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

### 7.3 Lectura / escritura de propiedades

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

Los resultados de los comandos se devuelven de forma asíncrona mediante los callbacks `onPropertiesWriteReply` / `onPropertiesReadReply`. Mantenga el proceso en ejecución tras enviar los comandos para poder observar las respuestas.

### 7.4 Cierre ordenado

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. Referencia de callbacks MQTT

`MqttCallback` está implementado por completo en `DemoApplication#testMqtt()`. Los más de 30 callback se agrupan a continuación; cada nombre representativo puede encontrarse en el archivo de código fuente.

| Categoría | Callbacks representativos | Finalidad |
|---|---|---|
| Conexión | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | Estado de la conexión, notificación de reconexión, confirmación de entrega |
| Propiedades y funciones | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | Respuestas a los comandos enviados desde el servidor |
| Informes | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | Datos del lado del dispositivo, eventos, metadatos y estado en línea / fuera de línea |
| Subdispositivos | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | Ciclo de vida de los subdispositivos, topología y mensajes de dispositivos hijo |
| Firmware | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | Progreso de la actualización OTA e información del firmware |
| Escenas y sistema | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | Gestión de escenas, sincronización de hora, registros e informes de reinicio |

> Nota: los cuerpos de los callback en el Demo solo imprimen el payload recibido. En una integración real, sustitúyalos por su propia lógica de negocio (persistencia, alertas, reenvío, etc.).

## 9. Preguntas frecuentes

### Q1: El inicio falla con "missing valid SDK credential configuration", o el inicio de sesión OAuth2 falla / 401

**A:** Compruebe primero `src/main/resources/sdk-config.properties`:

1. Falta el archivo, `appId` / `secureKey` están vacíos o siguen siendo marcadores `${...}` → rellene las credenciales reales y recompile;
2. El archivo sigue conteniendo los marcadores predeterminados `YOUR_APP_ID` / `YOUR_SECURE_KEY` → los marcadores no son rechazados por la validación y se usan como credenciales reales, por lo que el síntoma es un fallo de inicio de sesión / autenticación en lugar de un error de configuración. Sustitúyalos por las credenciales reales;
3. Las credenciales que no pertenecen a la aplicación del dispositivo también provocan fallos de autenticación — use las credenciales emitidas para esa aplicación en la plataforma IoT.

### Q2: La vinculación del dispositivo falla

**A:** Asegúrese de que `String deviceSn = "xxxxxxx"` en `DemoApplication#testHttpClient()` se haya sustituido por un SN de dispositivo válido y vinculable y de que el dispositivo no esté vinculado a otro usuario; confirme asimismo que `sdk-config.properties` se ha actualizado según la sección 5.2.

### Q3: No se reciben mensajes en los callbacks MQ

**A:** La llamada `unbindUserDevice(deviceSn)` al final de `testHttpClient()` desvincula el dispositivo, por lo que no llegarán los mensajes posteriores del dispositivo. En la v1.0.4 esta llamada **no está comentada** — coméntela manualmente (incluidos `ApiResult unbind = ...` y la instrucción de impresión) al depurar los callbacks MQ. Compruebe también que las credenciales sean válidas y que `deviceSn` sea un SN de dispositivo real.

### Q4: La escritura de propiedades no obtiene respuesta

**A:** Compruebe en este orden: si el dispositivo está en línea; si el ID de la propiedad coincide con el ThingModel del producto; si el tipo de valor coincide con la definición del modelo. La propiedad `FreezingPointEnable` del ejemplo es solo ilustrativa — ajuste `createTestProperties()` según su propio modelo de producto.

### Q5: Se pierde la conexión

**A:** Use el callback `onConnectionLost` para inspeccionar la causa; tras una reconexión correcta, el SDK lo notifica mediante `onConnected(reconnect, serverURI)` con `reconnect = true`.

### Q6: Maven no puede resolver las dependencias del SDK

**A:** Los dos jars del SDK se referencian con ámbito `system` desde `libs/`. Asegúrese de que `libs/heiman-sdk-mqtt-1.0.4.jar` y `libs/heiman-sdk-restful-1.0.4.jar` existen en esas rutas exactas y de que compila desde el directorio `heiman-sdk-demo` (`${project.basedir}` debe apuntar a él).

## 10. Notas sobre credenciales y seguridad

- El repositorio solo contiene marcadores de posición; no confirme nunca los valores reales de `appId` / `secureKey` y, si conserva credenciales reales fuera del control de versiones, añada su configuración local a `.gitignore`.
- Cuando el Demo del SDK se empaqueta mediante `/application/{id}/sdk/download`, la plataforma sustituye `sdk-config.properties` por las credenciales reales de esa aplicación — mantenga privado ese paquete descargado.
- `DemoApplication` imprime solo un access token enmascarado (`abcd****wxyz`); nunca imprima tokens completos, `secureKey` ni datos personales en los registros de producción.
- Si unas credenciales pudieran haberse filtrado, rote `secureKey` inmediatamente en la plataforma IoT y recompile / redespliegue el Demo con el nuevo valor.
- El Demo vincula y desvincula dispositivos reales y envía comandos reales. Use una aplicación y dispositivos de prueba dedicados; no lo ejecute contra datos de producción.
- `target/classes/sdk-config.properties` es una copia compilada de sus credenciales — elimine la salida de compilación antes de compartir o archivar el proyecto.

## 11. Soporte técnico

- **Correo electrónico:** heimanapp1@heiman.cn
- **Sitio web:** https://www.heimanhome.com

## 12. Historial de versiones y licencia

### 12.1 Historial de versiones

See [../CHANGELOG.md](../CHANGELOG.md) for the full history and [../RELEASE_NOTES.md](../RELEASE_NOTES.md) for this release.

- **v1.0.4:** Versión actual
   - Las credenciales de ejecución se gestionan de forma centralizada mediante `sdk-config.properties`; el archivo solo contiene marcadores de posición y debe sustituirse por `appId` / `secureKey` reales antes de ejecutar
   - `DemoApplication` como punto de entrada principal que integra el flujo completo RESTful + MQTT
   - Sustitución automática de las credenciales cuando el Demo del SDK se empaqueta mediante `/application/{id}/sdk/download` en la plataforma IoT
   - Liberación ordenada de recursos mediante un hook de apagado de la JVM
   - Cobertura completa de callbacks MQTT (propiedades / eventos / subdispositivos / firmware / escenas, etc.)

### 12.2 Licencia

El código fuente y la documentación de este repositorio se distribuyen bajo **Apache License 2.0** — véase [../LICENSE](../LICENSE). Los binarios propietarios de `libs/` y los diagramas de `image/` **no** están incluidos; véase [../NOTICE](../NOTICE).
