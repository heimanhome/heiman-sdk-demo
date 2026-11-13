# العرض التوضيحي لحزمة SDK الخاصة بـ Heiman Smart Home (heiman-sdk-demo)

> **Languages:** [English](../README.md) · [简体中文](README_CH.md) · [Русский](README_RU.md) · [Deutsch](README_DE.md) · [Français](README_FR.md) · [Italiano](README_IT.md) · [العربية](README_AR.md) · [Español](README_ES.md) · [Português](README_PT.md) · [Polski](README_PL.md)

| معلومات المستند | القيمة |
|---|---|
| اللغة | العربية |
| ينطبق على | SDK Heiman Smart Home **v1.0.4** (`heiman-sdk-demo`) |
| بيئة التشغيل | JDK 8+، Maven 3.6+ |
| حالة المستند | وثيقة رسمية |
| آخر تحديث | 2026-11-13 |

## المحتويات

1. نظرة عامة
2. قبل التشغيل (التعديلات المطلوبة)
3. تدفّق تشغيل العرض التوضيحي
4. هيكل المشروع
5. البدء السريع
6. مرجع الإعدادات
7. شرح الشيفرة الأساسية
8. مرجع استدعاءات MQTT
9. الأسئلة الشائعة
10. ملاحظات حول بيانات الاعتماد والأمان
11. الدعم الفني
12. سجل الإصدارات والترخيص

## 1. نظرة عامة

يُعدّ `heiman-sdk-demo` مشروع العرض التوضيحي الرسمي والشامل لحزمة SDK الخاصة بـ Heiman Smart Home (الإصدار v1.0.4). وقد بُني حول `com.heiman.sdk.demo.DemoApplication` ليعرض من البداية إلى النهاية مسارَي التكامل الأساسيين على جانب الخادم:

- **إدارة الأجهزة عبر RESTful**: تسجيل دخول تلقائي عبر OAuth2، ربط الأجهزة وإلغاء ربطها، الاستعلام المُرقّم عن قائمة الأجهزة، قائمة المنتجات وتحليل ThingModel.
- **الوصول إلى الأجهزة عبر MQTT**: تهيئة عميل MQTT، أوامر قراءة/كتابة الخصائص، وتسجيل ومعالجة أكثر من 30 استدعاءً يغطي تقارير الخصائص، تقارير الأحداث، إدارة الأجهزة الفرعية، ترقيات البرنامج الثابت، المشاهد، مزامنة الوقت وغيرها.

ويضم كذلك عرضًا لتحميل ThingModel (`DeviceDemo`)، وعروضًا لأنماط استدعاء Web API / OAuth2 (الحزمة `client/`)، وعرضًا للاشتراك في الكيانات / تسجيل الأوامر (الحزمة `server/`).

**الجمهور المستهدف:** مهندسو الخادم الذين يدمجون منصة Heiman IoT للمرة الأولى. يغطي هذا المستند المتطلبات المسبقة، وإعداد بيانات الاعتماد، وخطوات البناء والتشغيل، وعناصر الإعداد، وشرح الشيفرة، وقائمة الاستدعاءات، واستكشاف الأخطاء وإصلاحها، وتوصيات الأمان. جميع الأوامر ومقاطع الشيفرة مأخوذة من هذا المشروع ويمكن استخدامها مباشرة بعد استبدال العناصر النائبة المذكورة في القسم 2.

يُعرض أدناه مسار تكامل SDK بصورته العامة (تزويد الجهاز من جانب التطبيق ← ربط الجهاز وإرسال التقارير ← التكامل على جانب الخادم):

![مخطط تدفّق تكامل SDK](../image/img.png)

## 2. قبل التشغيل (التعديلات المطلوبة)

> ⚠️ **يحتوي المستودع على عناصر نائبة فقط. يجب استبدال ثلاثة عناصر قبل أن يعمل العرض التوضيحي بنجاح.**

| # | البند | الملف / الموضع | القيمة الافتراضية | الإجراء |
|---|---|---|---|---|
| 1 | بيانات اعتماد التطبيق | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | استبدلها بقيم `appId` / `secureKey` الحقيقية الصادرة عن منصة Heiman IoT (**مطلوب**) |
| 2 | الرقم التسلسلي للجهاز | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | استبدله برقم تسلسلي حقيقي وقابل للربط |
| 3 | خاصية الاختبار | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | عدّلها لتتوافق مع معرّف خاصية مُعرَّف في ThingModel لمنتجك |

**سبب الأهمية:** يرفض `SdkConfig` فقط غياب الملف أو القيم الفارغة أو العناصر النائبة `${...}` غير المستبدلة. وبما أن `YOUR_APP_ID` / `YOUR_SECURE_KEY` ليست فارغة، فإنها تُقبل كبيانات اعتماد صالحة وتُرسل إلى المنصة. لذا فإن تركها دون تغيير **لا** يُنتج خطأ «نقص الإعداد»، بل يفشل تسجيل الدخول / المصادقة عبر OAuth2 فحسب (مثل HTTP 401). راجع القسم 5.2 للاطلاع على إجراء الإعداد الكامل.

## 3. تدفّق تشغيل العرض التوضيحي

`com.heiman.sdk.demo.DemoApplication#main` هو نقطة الدخول الرئيسية ويعمل بالترتيب التالي:

1. **DeviceDemo**: يُحمّل خمسة نماذج ThingModel مدمجة (البوابة / مستشعر الدخان / أول أكسيد الكربون / مستشعر الحرارة / جهاز التحكم عن بُعد) من الموارد المدمجة في SDK (`DeviceModels`)، ويطبع نظرة عامة على خصائصها وأحداثها ووظائفها.
2. **testHttpClient() (RESTful)**:
   - يُنشئ `ClientConfig` من `sdk-config.properties`، ويُنشئ `HeimanRestfulClient`، ويُكمل تسجيل الدخول عبر OAuth2 تلقائيًا؛
   - يحصل على رمز الوصول و userId عبر `getCurrentToken()`؛
   - يربط جهازًا باستخدام `deviceService.bindUserDevice(deviceSn)`؛
   - يستعلم عن قائمة الأجهزة مع الترقيم (`queryDeviceList`) ويطبع تفاصيل الأجهزة؛
   - يستعلم عن قائمة المنتجات (`getProductList`)، ويحوّل `metadata` لكل منتج إلى `ThingModel`، ويخزّنها مؤقتًا بحسب productId؛
   - وأخيرًا يُلغي الربط عبر `unbindUserDevice(deviceSn)`. **ملاحظة: في الإصدار v1.0.4 يكون استدعاء إلغاء الربط هذا فعّالًا (غير مُعلَّق)؛ وعند الحاجة إلى مراقبة رسائل استدعاءات MQ، علّقه يدويًا في نهاية `testHttpClient()` (بما في ذلك `ApiResult unbind = ...` وعبارة الطباعة التي تليها).**
3. **testMqtt() (MQTT)**:
   - يُنشئ `MqttConfig` عبر `SdkConfig.get().toMqttConfig(userId)` (الوسيط broker مدمج في SDK ولا يتطلب إعداد عنوان URL)؛
   - يُنفّذ جميع أساليب `MqttCallback` (ردود قراءة/كتابة الخصائص، تقارير الأحداث/الخصائص، اتصال/انقطاع الأجهزة الفرعية، تقدّم ترقية البرنامج الثابت، إضافة/تحديث/حذف المشاهد، حالة الاتصال، إلخ)؛
   - يُهيّئ العميل عبر `HeimanMqttClient.getInstance().init(mqttConfig, callback)`؛
   - يتكرّر على قائمة الأجهزة التي جرى الاستعلام عنها في الخطوة 2، ويُعدّ خصائص الاختبار من ThingModel للمنتج (يستخدم المثال الخاصية `FreezingPointEnable` — عدّل `createTestProperties()` وفقًا لنموذج منتجك)، ثم يُرسل الأمرين `propertiesWrite` و `propertiesRead`.
4. **التنظيف عند الخروج**: يُحرّر خطاف إيقاف JVM (shutdown hook) الموارد بسلاسة عبر استدعاء `HeimanMqttClient.getInstance().shutdown()` و `restfulClient.close()`.

## 4. هيكل المشروع

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

## 5. البدء السريع

### 5.1 المتطلبات

- JDK 8+
- Maven 3.6+ (أو ببساطة افتح المشروع في IDEA)
- بيانات اعتماد التطبيق (`appId` / `secureKey`) الصادرة عن منصة Heiman IoT
- جهاز حقيقي قابل للربط (deviceSn)
- إمكانية الوصول عبر الشبكة إلى منصة Heiman IoT؛ نقاط النهاية RESTful والوسيط MQTT مدمجة في SDK

### 5.2 إعداد بيانات الاعتماد

> ⚠️ **يجب تعديل `src/main/resources/sdk-config.properties`**: استبدل العناصر النائبة الافتراضية ببيانات اعتماد التطبيق الحقيقية الصادرة عن منصة Heiman IoT قبل البناء والتشغيل.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

قائمة التحقق:

- **ترك العناصر النائبة لا يُنتج خطأ «نقص الإعداد»**: يرفض `SdkConfig` فقط غياب الملف أو القيم الفارغة أو العناصر النائبة `${...}` غير المستبدلة. وتُعامل `YOUR_APP_ID` / `YOUR_SECURE_KEY` كقيم صالحة وتُرسل إلى المنصة، لذا يفشل تسجيل الدخول / المصادقة عبر OAuth2 (مثل 401) بدلًا من خطأ واضح في بيانات الاعتماد؛
- لا تتضمن الحزمة أي بيانات اعتماد حقيقية؛ وإذا غاب الملف أو كانت إحدى القيم فارغة، يُطلق SDK الاستثناء `missing valid SDK credential configuration: appId/secureKey`؛
- احتفظ بأسماء المفاتيح `appId` / `secureKey` دون تغيير واحفظ الملف بترميز UTF-8. يستخدم `tenantId` القيمة الافتراضية المدمجة في SDK، كما أن الوسيط MQTT ونقاط النهاية RESTful مدمجة في SDK أيضًا — **لا تُضف إدخالات أخرى إلى هذا الملف**؛
- بعد التعديل، أعد تنفيذ `mvn compile` (أو أعد البناء في IDEA) لضمان تحديث `target/classes/sdk-config.properties` على مسار الفئات (classpath)؛
- لا يوجد `deviceSn` في هذا الملف: استبدل أيضًا `String deviceSn = "xxxxxxx"` داخل `DemoApplication#testHttpClient()` برقم تسلسلي حقيقي وقابل للربط وغير مرتبط بمستخدم آخر؛
- لا تُودِع بيانات الاعتماد الحقيقية في المستودع (يُحتفظ هنا بالعناصر النائبة فقط)؛ وإذا اشتبهت في تسريبها، جدّد `secureKey` على منصة IoT؛
- عند تغليف عرض SDK التوضيحي وتنزيله من منصة IoT عبر `/application/{id}/sdk/download`، يُستبدل هذا الملف تلقائيًا ببيانات الاعتماد الحقيقية لذلك التطبيق — **ولا حاجة إلى تعديل يدوي في هذه الحالة**.

### 5.3 البناء

```bash
cd heiman-sdk-demo
mvn compile
```

تُوفَّر تبعيات SDK كملفات jar بنطاق `system` داخل `libs/` (حزم SDK غير مُبهمة)، لذا لا حاجة إلى تثبيتها في المستودع المحلي.

### 5.4 التشغيل

> قبل التشغيل، تأكد من إتمام الخطوة 5.2 (استبدال `sdk-config.properties` ببيانات اعتماد حقيقية وإعادة بناء المشروع)، ومن تعديل `deviceSn` و `createTestProperties()` في `DemoApplication` بما يتناسب مع جهازك / نموذج ThingModel.

الطريقة الموصى بها هي فتح المشروع في IDEA وتشغيل:

```
com.heiman.sdk.demo.DemoApplication#main
```

المخرجات المتوقعة في وحدة التحكم، بالترتيب: نظرة عامة على نماذج ThingModel المدمجة، رمز الوصول المُقنَّع و userId بعد تسجيل الدخول، قائمة الأجهزة المُرقّمة، قائمة المنتجات مع خصائص ThingModel المُحلَّلة، رسالة اتصال MQTT، ثم سجلات الاستدعاءات عند وصول رسائل الأجهزة. تبقى العملية قيد التشغيل لتمكين مراقبة الاستدعاءات — اضغط `Ctrl+C` للخروج، وعندها يُحرّر خطاف الإيقاف كلا الاتصالين.

### 5.5 فئات العرض التوضيحي

| الفئة | الوصف |
|---|---|
| `DemoApplication` | نقطة الدخول الرئيسية: التدفّق الكامل RESTful + MQTT (استدعاءات حقيقية) |
| `ProductDemo` | الاستعلام عن قائمة المنتجات (استدعاء RESTful حقيقي) |
| `DeviceDemo` | تحميل ThingModel وطباعة النظرة العامة |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | عرض أنماط استدعاء Web API / OAuth2 (مخرجات محاكاة) |
| `server.EntitySubscribeDemo` | عرض الاشتراك في الكيانات / تسجيل الأوامر (مخرجات محاكاة) |

> ملاحظة: تُجري `DemoApplication` و `ProductDemo` فقط استدعاءات RESTful / MQTT حقيقية. يستخدم `DeviceDemo` نماذج ThingModel المدمجة في SDK، بينما تُخرج `client/*` و `server/EntitySubscribeDemo` مخرجات محاكاة محلية؛ ولا يُصدر أي منها طلبات شبكة حقيقية.

## 6. مرجع الإعدادات

| البند | المصدر | يجب تعديله | الوصف |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | نعم | معرّف التطبيق الصادر عن منصة Heiman IoT؛ يقرأه `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | نعم | سر التطبيق؛ يقرأه `SdkConfig.getSecureKey()`؛ لا تُودِعه في المستودع إطلاقًا |
| `tenantId` | مدمج في SDK (`HeimanEndpoints.DEFAULT_TENANT_ID`) | لا | يُعاد من `SdkConfig.getTenantId()`؛ لا يُقرأ من ملف الخصائص |
| نقاط النهاية RESTful / الوسيط MQTT | مدمجة في SDK | لا | لا يلزم إعداد عنوان URL؛ يحتاج `MqttConfig` فقط إلى appId و secureKey و tenantId و userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | نعم | الرقم التسلسلي للجهاز المراد ربطه؛ القيمة الافتراضية `xxxxxxx` عنصر نائب |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | لا | معرّف المستخدم بعد تسجيل الدخول، ويُمرَّر إلى `toMqttConfig(userId)` في وقت التشغيل |
| معرّف الخاصية / قيمتها | `DemoApplication#createTestProperties()` | نعم، إذا اختلف النموذج | يجب أن يطابق معرّف الخاصية ونوع قيمتها المُعرَّفين في ThingModel لمنتجك |

يعرض `SdkConfig` الأسلوبين `toClientConfig()` (بمهلة اتصال 30 ثانية ومهلتَي قراءة/كتابة 60 ثانية مع تمكين السجلات) و `toMqttConfig(userId)`. وبذلك يتشارك عميلا RESTful و MQTT مصدر بيانات الاعتماد نفسه تمامًا: `sdk-config.properties`.

## 7. شرح الشيفرة الأساسية

### 7.1 عميل RESTful (تسجيل دخول تلقائي)

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

### 7.2 تهيئة عميل MQTT واستدعاءاته

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

### 7.3 قراءة / كتابة الخصائص

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

تُعاد نتائج الأوامر بشكل غير متزامن عبر استدعاءَي `onPropertiesWriteReply` / `onPropertiesReadReply`. أبقِ العملية قيد التشغيل بعد إرسال الأوامر ليتسنّى مراقبة الردود.

### 7.4 الإيقاف بسلاسة

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. مرجع استدعاءات MQTT

يُنفَّذ `MqttCallback` بالكامل داخل `DemoApplication#testMqtt()`. وتُجمَّع استدعاءاته التي تتجاوز 30 استدعاءً في الجدول أدناه؛ ويمكن العثور على كل اسم تمثيلي في ملف الشيفرة المصدرية.

| الفئة | الاستدعاءات التمثيلية | الغرض |
|---|---|---|
| الاتصال | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | حالة الاتصال، إشعار إعادة الاتصال، تأكيد التسليم |
| الخصائص والوظائف | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | ردود على الأوامر المُرسَلة من الخادم |
| التقارير | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | بيانات الجهاز والأحداث والبيانات الوصفية وحالة الاتصال/الانقطاع |
| الأجهزة الفرعية | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | دورة حياة الأجهزة الفرعية والطوبولوجيا ورسائل الأجهزة الفرعية |
| البرنامج الثابت | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | تقدّم ترقية OTA ومعلومات البرنامج الثابت |
| المشاهد والنظام | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | إدارة المشاهد ومزامنة الوقت والسجلات وتقارير إعادة الضبط |

> ملاحظة: تكتفي أجسام الاستدعاءات في العرض التوضيحي بطباعة الحمولة الواردة. وعند التكامل الفعلي، استبدلها بمنطق العمل الخاص بك (التخزين الدائم، التنبيهات، إعادة التوجيه، إلخ).

## 9. الأسئلة الشائعة

### Q1: فشل التشغيل مع ظهور "missing valid SDK credential configuration" أو فشل تسجيل الدخول عبر OAuth2 / 401

**A:** تحقّق أولًا من `src/main/resources/sdk-config.properties`:

1. الملف مفقود، أو `appId` / `secureKey` فارغان، أو لا يزالان عنصرين نائبين `${...}` → أدخل بيانات الاعتماد الحقيقية وأعد البناء؛
2. ما زال الملف يحتوي على العنصرين النائبين الافتراضيين `YOUR_APP_ID` / `YOUR_SECURE_KEY` → لا يرفضهما التحقق وتُستخدمان كبيانات اعتماد حقيقية، لذا يظهر العَرَض على صورة فشل في تسجيل الدخول / المصادقة بدلًا من خطأ في الإعداد. استبدلهما ببيانات اعتماد حقيقية؛
3. تؤدي بيانات الاعتماد التي لا تعود إلى تطبيق الجهاز أيضًا إلى فشل المصادقة — استخدم بيانات الاعتماد الصادرة لذلك التطبيق على منصة IoT.

### Q2: فشل ربط الجهاز

**A:** تأكد من استبدال `String deviceSn = "xxxxxxx"` داخل `DemoApplication#testHttpClient()` برقم تسلسلي صالح وقابل للربط، وأن الجهاز غير مرتبط بمستخدم آخر؛ وتأكد كذلك من تحديث `sdk-config.properties` كما هو موضح في القسم 5.2.

### Q3: عدم وصول أي رسائل إلى استدعاءات MQ

**A:** يُلغي الاستدعاء `unbindUserDevice(deviceSn)` في نهاية `testHttpClient()` ربط الجهاز، لذا لا تصل رسائل الجهاز اللاحقة. وفي الإصدار v1.0.4 **لا يُعلَّق هذا الاستدعاء** — علّقه يدويًا (بما في ذلك `ApiResult unbind = ...` وعبارة الطباعة) عند تصحيح أخطاء استدعاءات MQ. تحقّق أيضًا من صلاحية بيانات الاعتماد ومن أن `deviceSn` رقم تسلسلي حقيقي للجهاز.

### Q4: لا يصل رد عند كتابة الخصائص

**A:** تحقّق بالترتيب التالي: هل الجهاز متصل؟ هل يطابق معرّف الخاصية نموذج ThingModel للمنتج؟ هل يطابق نوع القيمة تعريف النموذج؟ الخاصية `FreezingPointEnable` في المثال للعرض فقط — عدّل `createTestProperties()` وفقًا لنموذج منتجك.

### Q5: انقطاع الاتصال

**A:** استخدم الاستدعاء `onConnectionLost` لفحص سبب الانقطاع؛ وبعد نجاح إعادة الاتصال يُخطر SDK عبر `onConnected(reconnect, serverURI)` مع `reconnect = true`.

### Q6: تعذّر على Maven حلّ تبعيات SDK

**A:** يُشار إلى ملفَي jar الخاصين بـ SDK بنطاق `system` من داخل `libs/`. تأكد من وجود `libs/heiman-sdk-mqtt-1.0.4.jar` و `libs/heiman-sdk-restful-1.0.4.jar` في هذين المسارين بالضبط، ومن تنفيذ البناء من داخل دليل `heiman-sdk-demo` (يجب أن يشير `${project.basedir}` إليه).

## 10. ملاحظات حول بيانات الاعتماد والأمان

- يحتوي المستودع على عناصر نائبة فقط؛ لا تُودِع قيم `appId` / `secureKey` الحقيقية، وإذا احتفظت ببيانات اعتماد حقيقية خارج نظام إدارة الإصدارات فأضف ملف الإعداد المحلي إلى `.gitignore`.
- عند تغليف عرض SDK التوضيحي عبر `/application/{id}/sdk/download`، تستبدل المنصة `sdk-config.properties` ببيانات الاعتماد الحقيقية لذلك التطبيق — احتفظ بالحزمة المُنزَّلة بشكل خاص ولا تُشاركها.
- يطبع `DemoApplication` رمز وصول مُقنَّعًا فقط (`abcd****wxyz`)؛ لا تطبع الرموز الكاملة أو `secureKey` أو البيانات الشخصية في سجلات بيئة الإنتاج.
- إذا اشتبهت في تسريب بيانات الاعتماد، جدّد `secureKey` فورًا على منصة IoT وأعد بناء العرض التوضيحي ونشره بالقيمة الجديدة.
- يُجري العرض التوضيحي ربطًا وإلغاء ربط لأجهزة حقيقية ويُرسل أوامر حقيقية. استخدم تطبيق اختبار وأجهزة اختبار مخصصة، ولا تُشغّله على بيانات الإنتاج.
- يمثل `target/classes/sdk-config.properties` نسخة مُصرَّفة من بيانات اعتمادك — احذف مخرجات البناء قبل مشاركة المشروع أو أرشفته.

## 11. الدعم الفني

- **البريد الإلكتروني:** help@heimanhome.com.cn
- **الموقع الإلكتروني:** https://www.heimanhome.com

## 12. سجل الإصدارات والترخيص

### 12.1 سجل الإصدارات

See [../CHANGELOG.md](../CHANGELOG.md) for the full history and [../RELEASE_NOTES.md](../RELEASE_NOTES.md) for this release.

- **v1.0.4:** الإصدار الحالي
   - إدارة بيانات اعتماد التشغيل مركزيًا عبر `sdk-config.properties`؛ يحتوي الملف على عناصر نائبة فقط ويجب استبداله بقيم `appId` / `secureKey` حقيقية قبل التشغيل
   - `DemoApplication` كنقطة دخول رئيسية تدمج التدفّق الكامل RESTful + MQTT
   - استبدال بيانات الاعتماد تلقائيًا عند تغليف عرض SDK التوضيحي عبر `/application/{id}/sdk/download` على منصة IoT
   - تحرير الموارد بسلاسة عبر خطاف إيقاف JVM (shutdown hook)
   - تغطية شاملة لاستدعاءات MQTT (الخصائص / الأحداث / الأجهزة الفرعية / البرنامج الثابت / المشاهد، إلخ)

### 12.2 الترخيص

يُرخَّص كود المصدر والوثائق في هذا المستودع بموجب **Apache License 2.0** — انظر [../LICENSE](../LICENSE). الملفات الثنائية الخاصة في `libs/` والرسوم في `image/` **غير مشمولة**؛ انظر [../NOTICE](../NOTICE).
