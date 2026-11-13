# Демонстрационный проект SDK Heiman Smart Home (heiman-sdk-demo)

> **Languages:** [English](../README.md) · [简体中文](README_CH.md) · [Русский](README_RU.md) · [Deutsch](README_DE.md) · [Français](README_FR.md) · [Italiano](README_IT.md) · [العربية](README_AR.md) · [Español](README_ES.md) · [Português](README_PT.md) · [Polski](README_PL.md)

| Документ | Значение |
|---|---|
| Язык | Русский |
| Применимо к | Heiman Smart Home SDK **v1.0.4** (`heiman-sdk-demo`) |
| Среда выполнения | JDK 8+, Maven 3.6+ |
| Статус | Официальная документация |
| Дата обновления | 2026-11-13 |

## Содержание

1. Обзор
2. Перед запуском (обязательные изменения)
3. Порядок работы демо-проекта
4. Структура проекта
5. Быстрый старт
6. Справочник параметров конфигурации
7. Разбор ключевого кода
8. Справочник обратных вызовов MQTT
9. Часто задаваемые вопросы
10. Учётные данные и рекомендации по безопасности
11. Техническая поддержка
12. История версий и лицензия

## 1. Обзор

`heiman-sdk-demo` — официальный комплексный демонстрационный проект SDK Heiman Smart Home (v1.0.4), построенный вокруг `com.heiman.sdk.demo.DemoApplication`. Он полностью демонстрирует два ключевых сценария серверной интеграции:

- **Управление устройствами через RESTful**: автоматический вход OAuth2, привязка / отвязка устройств, постраничный запрос списка устройств, получение списка продуктов и разбор ThingModel.
- **Подключение устройств через MQTT**: инициализация MQTT-клиента, команды чтения / записи свойств, а также регистрация и обработка более 30 обратных вызовов, охватывающих отчёты о свойствах, отчёты о событиях, управление подчинёнными устройствами, обновление прошивки, сценарии, синхронизацию времени и многое другое.

Кроме того, проект включает демонстрацию загрузки модели устройства (`DeviceDemo`), демонстрации шаблонов вызовов Web API / OAuth2 (пакет `client/`) и демонстрацию подписки на сущности / регистрации команд (пакет `server/`).

**Целевая аудитория:** серверные инженеры, впервые выполняющие интеграцию с IoT-платформой Heiman. Документ охватывает предварительные требования, настройку учётных данных, шаги сборки и запуска, параметры конфигурации, разбор кода, перечень обратных вызовов, устранение неполадок и рекомендации по безопасности. Все команды и фрагменты кода взяты из этого демо-проекта и готовы к использованию после замены заполнителей, перечисленных в разделе 2.

Общий порядок интеграции SDK (настройка на стороне приложения → привязка устройства и передача данных → серверная интеграция) показан ниже:

![Схема интеграции SDK](../image/img.png)

## 2. Перед запуском (обязательные изменения)

> ⚠️ **В репозитории содержатся только заполнители. Перед успешным запуском демо-проекта необходимо заменить три элемента.**

| # | Элемент | Файл / расположение | Значение по умолчанию | Действие |
|---|---|---|---|---|
| 1 | Учётные данные приложения | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | Замените на реальные `appId` / `secureKey`, выданные IoT-платформой Heiman (**обязательно**) |
| 2 | SN устройства | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | Замените на реальный SN устройства, доступного для привязки |
| 3 | Тестовое свойство | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | Скорректируйте в соответствии с идентификатором свойства, определённым в модели устройства вашего продукта |

**Почему это важно:** `SdkConfig` отклоняет только отсутствующий файл, пустые значения и неразрешённые заполнители `${...}`. Значения `YOUR_APP_ID` / `YOUR_SECURE_KEY` непустые, поэтому они принимаются как допустимые учётные данные и отправляются на платформу. Если оставить их без изменений, ошибка "отсутствует конфигурация" **не** возникнет — просто не удастся выполнить вход / аутентификацию OAuth2 (например, HTTP 401). Полную процедуру настройки см. в разделе 5.2.

## 3. Порядок работы демо-проекта

`com.heiman.sdk.demo.DemoApplication#main` — главная точка входа; выполнение происходит в следующем порядке:

1. **DeviceDemo**: загружает пять встроенных моделей устройств (шлюз / дымовой извещатель / CO / тепловой извещатель / пульт дистанционного управления) из встроенных ресурсов SDK (`DeviceModels`) и выводит обзор их свойств, событий и функций.
2. **testHttpClient() (RESTful)**:
   - Создаёт `ClientConfig` из `sdk-config.properties`, создаёт `HeimanRestfulClient` и автоматически выполняет вход OAuth2;
   - Получает маркер доступа и userId через `getCurrentToken()`;
   - Привязывает устройство вызовом `deviceService.bindUserDevice(deviceSn)`;
   - Выполняет постраничный запрос списка устройств (`queryDeviceList`) и выводит сведения об устройствах;
   - Запрашивает список продуктов (`getProductList`), преобразует `metadata` каждого продукта в `ThingModel` и кэширует их по productId;
   - В конце снимает привязку вызовом `unbindUserDevice(deviceSn)`. **Примечание: в v1.0.4 этот вызов отвязки активен (не закомментирован); если необходимо наблюдать сообщения обратных вызовов MQ, вручную закомментируйте вызов отвязки в конце `testHttpClient()` (включая `ApiResult unbind = ...` и следующий за ним оператор вывода).**
3. **testMqtt() (MQTT)**:
   - Создаёт `MqttConfig` через `SdkConfig.get().toMqttConfig(userId)` (брокер встроен в SDK, настройка URL не требуется);
   - Реализует все методы `MqttCallback` (ответы на чтение / запись свойств, отчёты о событиях / свойствах, выход подчинённых устройств в сеть / из сети, ход обновления прошивки, добавление / изменение / удаление сценариев, состояние соединения и др.);
   - Выполняет инициализацию вызовом `HeimanMqttClient.getInstance().init(mqttConfig, callback)`;
   - Перебирает список устройств, полученный на шаге 2, формирует тестовые свойства на основе модели устройства продукта (в примере используется свойство `FreezingPointEnable` — скорректируйте `createTestProperties()` под модель своего продукта), затем отправляет команды `propertiesWrite` и `propertiesRead`.
4. **Очистка при выходе**: перехватчик завершения JVM корректно освобождает ресурсы, вызывая `HeimanMqttClient.getInstance().shutdown()` и `restfulClient.close()`.

## 4. Структура проекта

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

## 5. Быстрый старт

### 5.1 Требования

- JDK 8+
- Maven 3.6+ (или просто откройте проект в IDEA)
- Учётные данные приложения (`appId` / `secureKey`), выданные IoT-платформой Heiman
- Реальное устройство, доступное для привязки (deviceSn)
- Сетевой доступ к IoT-платформе Heiman; конечные точки RESTful и брокер MQTT встроены в SDK

### 5.2 Настройка учётных данных

> ⚠️ **Необходимо изменить `src/main/resources/sdk-config.properties`**: перед сборкой и запуском замените заполнители по умолчанию реальными учётными данными приложения, выданными IoT-платформой Heiman.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

Контрольный список:

- **Если оставить заполнители, ошибка "отсутствует конфигурация" НЕ возникнёт**: `SdkConfig` отклоняет только отсутствующий файл, пустые значения и неразрешённые заполнители `${...}`. Значения `YOUR_APP_ID` / `YOUR_SECURE_KEY` считаются допустимыми и отправляются на платформу, поэтому вместо явной ошибки учётных данных происходит сбой входа OAuth2 / аутентификации (например, 401);
- В репозитории нет реальных учётных данных; если файл отсутствует или значение пусто, SDK выбрасывает исключение `missing valid SDK credential configuration: appId/secureKey`;
- Сохраните имена ключей `appId` / `secureKey` без изменений и сохраните файл в кодировке UTF-8. Для `tenantId` используется встроенное в SDK значение по умолчанию; брокер MQTT и конечные точки RESTful также встроены в SDK — **не добавляйте в этот файл дополнительные записи**;
- После изменения повторно выполните `mvn compile` (или пересоберите проект в IDEA), чтобы файл `target/classes/sdk-config.properties` в classpath был актуален;
- `deviceSn` в этом файле отсутствует: также замените `String deviceSn = "xxxxxxx"` в `DemoApplication#testHttpClient()` на реальный SN устройства, доступного для привязки и не привязанного к другому пользователю;
- Никогда не фиксируйте реальные учётные данные в репозитории (здесь хранятся только заполнители); при подозрении на утечку учётных данных обновите `secureKey` на IoT-платформе;
- При упаковке и загрузке демо-проекта SDK с IoT-платформы через `/application/{id}/sdk/download` этот файл автоматически заменяется реальными учётными данными соответствующего приложения — **в этом случае ручное изменение не требуется**.

### 5.3 Сборка

```bash
cd heiman-sdk-demo
mvn compile
```

Зависимости SDK поставляются в виде jar-файлов с областью видимости system в каталоге `libs/` (обычные бинарные файлы SDK), поэтому устанавливать их в локальный репозиторий не требуется.

### 5.4 Запуск

> Перед запуском убедитесь, что шаг 5.2 выполнен (`sdk-config.properties` заменён реальными учётными данными и проект пересобран), а `deviceSn` и `createTestProperties()` в `DemoApplication` скорректированы под ваше устройство / модель устройства.

Рекомендуемый способ — открыть проект в IDEA и запустить:

```
com.heiman.sdk.demo.DemoApplication#main
```

Ожидаемый вывод в консоли в указанном порядке: обзор встроенных моделей устройств, маскированный маркер доступа и userId после входа, постраничный список устройств, список продуктов с разобранными свойствами моделей устройств, сообщение о подключении MQTT и журналы обратных вызовов по мере поступления сообщений от устройств. Процесс продолжает работу, чтобы можно было наблюдать обратные вызовы; нажмите `Ctrl+C` для выхода, после чего перехватчик завершения освободит оба соединения.

### 5.5 Демонстрационные классы

| Класс | Описание |
|---|---|
| `DemoApplication` | Главная точка входа: полный сценарий RESTful + MQTT (реальные вызовы) |
| `ProductDemo` | Запрос списка продуктов (реальный вызов RESTful) |
| `DeviceDemo` | Загрузка модели устройства и вывод обзора |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | Демонстрации шаблонов вызовов Web API / OAuth2 (имитируемый вывод) |
| `server.EntitySubscribeDemo` | Демонстрация подписки на сущности / регистрации команд (имитируемый вывод) |

> Примечание: реальные вызовы RESTful / MQTT выполняют только `DemoApplication` и `ProductDemo`. `DeviceDemo` использует встроенные в SDK модели устройств, а `client/*` и `server/EntitySubscribeDemo` выводят имитируемые локальные данные; ни один из них не выполняет реальные сетевые запросы.

## 6. Справочник параметров конфигурации

| Параметр | Источник | Требует изменения | Описание |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | Да | Идентификатор приложения, выданный IoT-платформой Heiman; считывается методом `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | Да | Секрет приложения; считывается методом `SdkConfig.getSecureKey()`; никогда не фиксируйте его в репозитории |
| `tenantId` | Встроено в SDK (`HeimanEndpoints.DEFAULT_TENANT_ID`) | Нет | Возвращается методом `SdkConfig.getTenantId()`; не считывается из файла свойств |
| Конечные точки RESTful / брокер MQTT | Встроено в SDK | Нет | Настройка URL не требуется; `MqttConfig` требуются только appId, secureKey, tenantId и userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | Да | SN устройства для привязки; значение по умолчанию `xxxxxxx` — заполнитель |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | Нет | Идентификатор пользователя при входе; во время выполнения передаётся в `toMqttConfig(userId)` |
| Идентификатор / значение свойства | `DemoApplication#createTestProperties()` | Да, если модель отличается | Должны совпадать с идентификатором и типом значения свойства, определёнными в модели устройства вашего продукта |

`SdkConfig` предоставляет `toClientConfig()` (тайм-аут подключения 30 с, тайм-ауты чтения и записи 60 с, ведение журнала включено) и `toMqttConfig(userId)`. Благодаря этому клиенты RESTful и MQTT используют один и тот же источник учётных данных: `sdk-config.properties`.

## 7. Разбор ключевого кода

### 7.1 RESTful-клиент (автоматический вход)

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

### 7.2 Инициализация MQTT-клиента и обратные вызовы

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

### 7.3 Чтение / запись свойств

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

Результаты выполнения команд возвращаются асинхронно через обратные вызовы `onPropertiesWriteReply` / `onPropertiesReadReply`. После отправки команд не завершайте процесс, чтобы можно было наблюдать ответы.

### 7.4 Корректное завершение работы

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. Справочник обратных вызовов MQTT

`MqttCallback` полностью реализован в `DemoApplication#testMqtt()`. Ниже приведены группы из более чем 30 обратных вызовов; каждый представительный метод можно найти в исходном файле.

| Категория | Представительные обратные вызовы | Назначение |
|---|---|---|
| Соединение | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | Состояние соединения, уведомление о повторном подключении, подтверждение доставки |
| Свойства и функции | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | Ответы на команды, отправленные сервером |
| Отчёты | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | Данные со стороны устройства, события, метаданные и состояние в сети / вне сети |
| Подчинённые устройства | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | Жизненный цикл подчинённых устройств, топология и сообщения дочерних устройств |
| Прошивка | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | Ход обновления OTA и сведения о прошивке |
| Сценарии и система | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | Управление сценариями, синхронизация времени, журналы и отчёты о сбросе |

> Примечание: тела обратных вызовов в демо-проекте только выводят полученные данные. При реальной интеграции замените их собственной бизнес-логикой (сохранение данных, оповещение, пересылка и т. д.).

## 9. Часто задаваемые вопросы

### Q1: При запуске возникает ошибка "missing valid SDK credential configuration" или сбой входа OAuth2 / 401

**A:** Сначала проверьте `src/main/resources/sdk-config.properties`:

1. Файл отсутствует, `appId` / `secureKey` пусты или всё ещё являются заполнителями `${...}` → укажите реальные учётные данные и пересоберите проект;
2. В файле остались заполнители по умолчанию `YOUR_APP_ID` / `YOUR_SECURE_KEY` → проверка не отклоняет заполнители, и они используются как реальные учётные данные, поэтому проявляется сбой входа / аутентификации, а не ошибка конфигурации. Замените их реальными учётными данными;
3. Учётные данные, не относящиеся к приложению устройства, также вызывают ошибки аутентификации — используйте учётные данные, выданные для этого приложения на IoT-платформе.

### Q2: Не удаётся привязать устройство

**A:** Убедитесь, что `String deviceSn = "xxxxxxx"` в `DemoApplication#testHttpClient()` заменено на допустимый SN устройства, доступного для привязки, и что устройство не привязано к другому пользователю; также проверьте, что `sdk-config.properties` обновлён, как описано в разделе 5.2.

### Q3: Обратные вызовы MQ не получают сообщений

**A:** Вызов `unbindUserDevice(deviceSn)` в конце `testHttpClient()` снимает привязку устройства, поэтому последующие сообщения от устройства не поступают. В v1.0.4 этот вызов **не закомментирован** — при отладке обратных вызовов MQ закомментируйте его вручную (включая `ApiResult unbind = ...` и оператор вывода). Также убедитесь, что учётные данные действительны, а `deviceSn` — реальный SN устройства.

### Q4: Нет ответа на запись свойства

**A:** Проверьте по порядку: находится ли устройство в сети; совпадает ли идентификатор свойства с моделью устройства продукта; соответствует ли тип значения определению модели. Свойство `FreezingPointEnable` в примере приведено только для демонстрации — скорректируйте `createTestProperties()` под модель своего продукта.

### Q5: Соединение разорвано

**A:** Определите причину с помощью обратного вызова `onConnectionLost`; после успешного повторного подключения SDK уведомляет через `onConnected(reconnect, serverURI)` со значением `reconnect = true`.

### Q6: Maven не может разрешить зависимости SDK

**A:** Оба jar-файла SDK подключаются с областью видимости `system` из каталога `libs/`. Убедитесь, что файлы `libs/heiman-sdk-mqtt-1.0.4.jar` и `libs/heiman-sdk-restful-1.0.4.jar` существуют по указанным точным путям и что сборка выполняется из каталога `heiman-sdk-demo` (`${project.basedir}` должен указывать на него).

## 10. Учётные данные и рекомендации по безопасности

- В репозитории содержатся только заполнители; никогда не фиксируйте реальные значения `appId` / `secureKey` в системе контроля версий и добавьте локальную конфигурацию в `.gitignore`, если храните реальные учётные данные вне репозитория.
- При упаковке демо-проекта SDK через `/application/{id}/sdk/download` платформа заменяет `sdk-config.properties` реальными учётными данными этого приложения — храните загруженный пакет в недоступном для посторонних месте.
- `DemoApplication` выводит только маскированный маркер доступа (`abcd****wxyz`); никогда не выводите в производственные журналы полные маркеры, `secureKey` или персональные данные.
- При подозрении на утечку учётных данных немедленно обновите `secureKey` на IoT-платформе и пересоберите / повторно разверните демо-проект с новым значением.
- Демо-проект реально привязывает и отвязывает устройства и отправляет реальные команды. Используйте выделенное тестовое приложение и тестовые устройства; не запускайте его с производственными данными.
- `target/classes/sdk-config.properties` — скомпилированная копия ваших учётных данных; перед передачей или архивированием проекта удалите результаты сборки.

## 11. Техническая поддержка

- **Электронная почта:** heimanapp1@heiman.cn
- **Сайт:** https://www.heimanhome.com

## 12. История версий и лицензия

### 12.1 История версий

See [../CHANGELOG.md](../CHANGELOG.md) for the full history and [../RELEASE_NOTES.md](../RELEASE_NOTES.md) for this release.

- **v1.0.4:** Текущая версия
   - Учётные данные времени выполнения централизованно управляются через `sdk-config.properties`; файл содержит только заполнители и перед запуском должен быть заменён реальными `appId` / `secureKey`
   - `DemoApplication` как главная точка входа, объединяющая полный сценарий RESTful + MQTT
   - Автоматическая замена учётных данных при упаковке демо-проекта SDK через `/application/{id}/sdk/download` на IoT-платформе
   - Корректное освобождение ресурсов с помощью перехватчика завершения JVM
   - Полный охват обратных вызовов MQTT (свойства / события / подчинённые устройства / прошивка / сценарии и др.)

### 12.2 Лицензия

Исходный код и документация этого репозитория распространяются по лицензии **Apache License 2.0** — см. [../LICENSE](../LICENSE). Проприетарные бинарные файлы в `libs/` и схемы в `image/` **не входят** в область действия лицензии; см. [../NOTICE](../NOTICE).
