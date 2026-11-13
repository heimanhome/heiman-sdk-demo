# Heiman Smart Home SDK-Demo (heiman-sdk-demo)

> **Languages:** [English](../README.md) · [简体中文](README_CH.md) · [Русский](README_RU.md) · [Deutsch](README_DE.md) · [Français](README_FR.md) · [Italiano](README_IT.md) · [العربية](README_AR.md) · [Español](README_ES.md) · [Português](README_PT.md) · [Polski](README_PL.md)

| Dokument | Wert |
|---|---|
| Sprache | Deutsch |
| Gilt für | Heiman Smart Home SDK **v1.0.4** (`heiman-sdk-demo`) |
| Laufzeitumgebung | JDK 8+, Maven 3.6+ |
| Status | Formale Dokumentation |
| Zuletzt aktualisiert | 2026-11-13 |

## Inhaltsverzeichnis

1. Überblick
2. Vor dem Start (erforderliche Änderungen)
3. Ablauf der Demo
4. Projektstruktur
5. Schnellstart
6. Konfigurationsreferenz
7. Erläuterung des Kerncodes
8. MQTT-Callback-Referenz
9. Häufig gestellte Fragen
10. Hinweise zu Zugangsdaten und Sicherheit
11. Technischer Support
12. Versionsverlauf und Lizenz

## 1. Überblick

`heiman-sdk-demo` ist das offizielle, umfassende Demo-Projekt für das Heiman Smart Home SDK (v1.0.4). Rund um `com.heiman.sdk.demo.DemoApplication` aufgebaut, demonstriert es die beiden zentralen serverseitigen Integrationsabläufe vollständig:

- **RESTful-Geräteverwaltung**: automatische OAuth2-Anmeldung, Binden und Lösen von Geräten, seitenweise Abfrage der Geräteliste, Produktliste und Auswertung des ThingModel.
- **MQTT-Geräteanbindung**: Initialisierung des MQTT-Clients, Befehle zum Lesen/Schreiben von Eigenschaften sowie Registrierung und Verarbeitung von über 30 Callbacks für Eigenschaftsberichte, Ereignisberichte, Verwaltung von Untergeräten, Firmware-Upgrades, Szenen, Zeitsynchronisierung und weiteres.

Es enthält außerdem eine Demo zum Laden von Dingmodellen (`DeviceDemo`), Demos zu Aufrufmustern der Web-API / OAuth2 (`client/`-Paket) sowie eine Demo zur Entitätsabonnement- und Befehlsregistrierung (`server/`-Paket).

**Zielgruppe:** Serverseitige Entwickler, die die Heiman IoT-Plattform erstmals anbinden. Dieses Dokument behandelt Voraussetzungen, Konfiguration der Zugangsdaten, Erstellungs- und Ausführungsschritte, Konfigurationseinträge, Code-Erläuterungen, die Übersicht der Callbacks, Fehlerbehebung und Sicherheitshinweise. Alle Befehle und Codeausschnitte stammen aus dieser Demo und sind sofort einsatzbereit, sobald die in Abschnitt 2 aufgeführten Platzhalter ersetzt wurden.

Der gesamte SDK-Integrationsablauf (Provisionierung auf App-Seite → Binden und Melden des Geräts → serverseitige Integration) ist unten dargestellt:

![SDK-Integrationsablauf](../image/img.png)

## 2. Vor dem Start (erforderliche Änderungen)

> ⚠️ **Das Repository enthält ausschließlich Platzhalter. Vor der erfolgreichen Ausführung der Demo müssen drei Angaben ersetzt werden.**

| # | Element | Datei / Position | Standardwert | Maßnahme |
|---|---|---|---|---|
| 1 | Anwendungs-Zugangsdaten | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | Durch die von der Heiman IoT-Plattform ausgegebenen echten `appId` / `secureKey` ersetzen (**erforderlich**) |
| 2 | Geräte-SN | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | Durch eine echte, bindbare Geräte-SN ersetzen |
| 3 | Testeigenschaft | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | An eine im eigenen Produkt-Dingmodell definierte Eigenschafts-ID anpassen |

**Warum das wichtig ist:** `SdkConfig` weist nur eine fehlende Datei, leere Werte und nicht aufgelöste `${...}`-Platzhalter zurück. `YOUR_APP_ID` / `YOUR_SECURE_KEY` sind nicht leer und werden daher als gültige Zugangsdaten akzeptiert und an die Plattform gesendet. Werden sie unverändert gelassen, wird **kein** Fehler "fehlende Konfiguration" ausgelöst — die OAuth2-Anmeldung bzw. Authentifizierung schlägt einfach fehl (z. B. HTTP 401). Das vollständige Konfigurationsverfahren finden Sie in Abschnitt 5.2.

## 3. Ablauf der Demo

`com.heiman.sdk.demo.DemoApplication#main` ist der Haupteinstiegspunkt und wird in der folgenden Reihenfolge ausgeführt:

1. **DeviceDemo**: lädt fünf integrierte Dingmodelle (Gateway / Rauch / CO / Wärme / Fernbedienung) aus den SDK-eigenen Ressourcen (`DeviceModels`) und gibt eine Übersicht ihrer Eigenschaften, Ereignisse und Funktionen aus.
2. **testHttpClient() (RESTful)**:
   - Erstellt aus `sdk-config.properties` ein `ClientConfig`, erzeugt einen `HeimanRestfulClient` und führt die OAuth2-Anmeldung automatisch durch;
   - Ermittelt Zugriffstoken und userId über `getCurrentToken()`;
   - Bindet ein Gerät mit `deviceService.bindUserDevice(deviceSn)`;
   - Fragt die Geräteliste seitenweise ab (`queryDeviceList`) und gibt Gerätedetails aus;
   - Fragt die Produktliste ab (`getProductList`), parst die `metadata` jedes Produkts in ein `ThingModel` und speichert sie nach productId zwischen;
   - Hebt die Bindung schließlich über `unbindUserDevice(deviceSn)` auf. **Hinweis: In v1.0.4 ist dieser Aufruf zum Lösen der Bindung aktiv (nicht auskommentiert); wenn Sie MQ-Callback-Nachrichten beobachten möchten, kommentieren Sie den Aufruf am Ende von `testHttpClient()` manuell aus (einschließlich `ApiResult unbind = ...` und der darauf folgenden Ausgabeanweisung).**
3. **testMqtt() (MQTT)**:
   - Erzeugt ein `MqttConfig` über `SdkConfig.get().toMqttConfig(userId)` (der Broker ist im SDK integriert, es ist keine URL-Konfiguration erforderlich);
   - Implementiert alle `MqttCallback`-Methoden (Antworten auf Lese-/Schreibvorgänge, Ereignis-/Eigenschaftsberichte, Online-/Offline-Status von Untergeräten, Fortschritt von Firmware-Upgrades, Hinzufügen/Aktualisieren/Löschen von Szenen, Verbindungsstatus usw.);
   - Initialisiert mit `HeimanMqttClient.getInstance().init(mqttConfig, callback)`;
   - Durchläuft die in Schritt 2 abgefragte Geräteliste, erstellt Testeigenschaften aus dem Produkt-Dingmodell (im Beispiel wird die Eigenschaft `FreezingPointEnable` verwendet — passen Sie `createTestProperties()` an Ihr eigenes Produktmodell an) und sendet anschließend die Befehle `propertiesWrite` und `propertiesRead`.
4. **Bereinigung beim Beenden**: Ein JVM-Shutdown-Hook gibt Ressourcen geordnet frei, indem er `HeimanMqttClient.getInstance().shutdown()` und `restfulClient.close()` aufruft.

## 4. Projektstruktur

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

## 5. Schnellstart

### 5.1 Voraussetzungen

- JDK 8+
- Maven 3.6+ (oder das Projekt einfach in IDEA öffnen)
- Von der Heiman IoT-Plattform ausgegebene Anwendungs-Zugangsdaten (`appId` / `secureKey`)
- Ein reales, bindbares Gerät (deviceSn)
- Netzwerkzugriff auf die Heiman IoT-Plattform; die RESTful-Endpunkte und der MQTT-Broker sind im SDK integriert

### 5.2 Zugangsdaten konfigurieren

> ⚠️ **Sie müssen `src/main/resources/sdk-config.properties` bearbeiten**: Ersetzen Sie die Standard-Platzhalter vor dem Erstellen und Ausführen durch die echten, von der Heiman IoT-Plattform ausgegebenen Anwendungs-Zugangsdaten.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

Prüfliste:

- **Werden die Platzhalter nicht ersetzt, wird KEIN Fehler "fehlende Konfiguration" ausgelöst**: `SdkConfig` weist nur eine fehlende Datei, leere Werte und nicht aufgelöste `${...}`-Platzhalter zurück. `YOUR_APP_ID` / `YOUR_SECURE_KEY` werden als gültige Werte behandelt und an die Plattform gesendet, sodass die OAuth2-Anmeldung bzw. Authentifizierung fehlschlägt (z. B. 401) anstelle eines eindeutigen Fehlers bei den Zugangsdaten;
- Im Repository sind keine echten Zugangsdaten enthalten; fehlt die Datei oder ist ein Wert leer, löst das SDK `missing valid SDK credential configuration: appId/secureKey` aus;
- Lassen Sie die Schlüsselnamen `appId` / `secureKey` unverändert und speichern Sie die Datei als UTF-8. `tenantId` verwendet den SDK-internen Standardwert, und auch der MQTT-Broker sowie die RESTful-Endpunkte sind im SDK integriert — **fügen Sie dieser Datei keine zusätzlichen Einträge hinzu**;
- Führen Sie nach der Änderung `mvn compile` erneut aus (oder erstellen Sie das Projekt in IDEA neu), damit `target/classes/sdk-config.properties` im Klassenpfad auf dem aktuellen Stand ist;
- `deviceSn` steht nicht in dieser Datei: Ersetzen Sie außerdem `String deviceSn = "xxxxxxx"` in `DemoApplication#testHttpClient()` durch eine echte, bindbare Geräte-SN, die keinem anderen Benutzer zugeordnet ist;
- Übernehmen Sie niemals echte Zugangsdaten in das Repository (hier werden nur Platzhalter vorgehalten); falls Zugangsdaten möglicherweise kompromittiert wurden, erneuern Sie `secureKey` auf der IoT-Plattform;
- Wenn die SDK-Demo über `/application/{id}/sdk/download` von der IoT-Plattform paketiert und heruntergeladen wird, wird diese Datei automatisch durch die echten Zugangsdaten der betreffenden Anwendung ersetzt — **in diesem Fall ist keine manuelle Änderung erforderlich**.

### 5.3 Kompilieren

```bash
cd heiman-sdk-demo
mvn compile
```

Die SDK-Abhängigkeiten liegen als Jars mit dem Gültigkeitsbereich `system` unter `libs/` (einfache SDK-Binärdateien) vor, sodass sie nicht in Ihr lokales Repository installiert werden müssen.

### 5.4 Ausführen

> Vergewissern Sie sich vor der Ausführung, dass Schritt 5.2 abgeschlossen ist (`sdk-config.properties` durch echte Zugangsdaten ersetzt und das Projekt neu erstellt) und dass `deviceSn` sowie `createTestProperties()` in `DemoApplication` an Ihr Gerät bzw. Dingmodell angepasst wurden.

Empfohlen wird, das Projekt in IDEA zu öffnen und Folgendes auszuführen:

```
com.heiman.sdk.demo.DemoApplication#main
```

Erwartete Konsolenausgabe in dieser Reihenfolge: die Übersicht der integrierten Dingmodelle, ein maskiertes Zugriffstoken und die userId nach der Anmeldung, die seitenweise Geräteliste, die Produktliste mit ausgewerteten Dingmodell-Eigenschaften, die MQTT-Verbindungsmeldung und Callback-Protokolle beim Eintreffen von Gerätenachrichten. Der Prozess läuft weiter, damit Callbacks beobachtet werden können — mit `Ctrl+C` wird er beendet, wobei der Shutdown-Hook beide Verbindungen freigibt.

### 5.5 Demo-Klassen

| Klasse | Beschreibung |
|---|---|
| `DemoApplication` | Haupteinstieg: vollständiger RESTful- + MQTT-Ablauf (echte Aufrufe) |
| `ProductDemo` | Abfrage der Produktliste (echter RESTful-Aufruf) |
| `DeviceDemo` | Laden von Dingmodellen und Ausgabe der Übersicht |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | Demos zu Aufrufmustern der Web-API / OAuth2 (simulierte Ausgabe) |
| `server.EntitySubscribeDemo` | Demo zur Entitätsabonnement- / Befehlsregistrierung (simulierte Ausgabe) |

> Hinweis: Nur `DemoApplication` und `ProductDemo` führen echte RESTful- / MQTT-Aufrufe aus. `DeviceDemo` nutzt die SDK-internen Dingmodelle, während `client/*` und `server/EntitySubscribeDemo` simulierte lokale Ausgaben erzeugen; keine davon stellt echte Netzwerkanfragen.

## 6. Konfigurationsreferenz

| Element | Quelle | Muss geändert werden | Beschreibung |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | Ja | Von der Heiman IoT-Plattform ausgegebene Anwendungs-ID; gelesen von `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | Ja | Anwendungsgeheimnis; gelesen von `SdkConfig.getSecureKey()`; niemals in das Repository einchecken |
| `tenantId` | Im SDK integriert (`HeimanEndpoints.DEFAULT_TENANT_ID`) | Nein | Wird von `SdkConfig.getTenantId()` zurückgegeben; nicht aus der Properties-Datei gelesen |
| RESTful-Endpunkte / MQTT-Broker | Im SDK integriert | Nein | Es ist keine URL-Konfiguration erforderlich; `MqttConfig` benötigt nur appId, secureKey, tenantId und userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | Ja | SN des zu bindenden Geräts; der Standardwert `xxxxxxx` ist ein Platzhalter |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | Nein | Benutzer-ID der Anmeldung, zur Laufzeit an `toMqttConfig(userId)` übergeben |
| Eigenschafts-ID / -wert | `DemoApplication#createTestProperties()` | Ja, falls Ihr Modell abweicht | Muss mit der Eigenschafts-ID und dem Werttyp im Dingmodell Ihres Produkts übereinstimmen |

`SdkConfig` stellt `toClientConfig()` (mit 30 s Verbindungs- sowie 60 s Lese-/Schreib-Timeouts und aktivierter Protokollierung) und `toMqttConfig(userId)` bereit. Der RESTful- und der MQTT-Client nutzen daher exakt dieselbe Quelle für die Zugangsdaten: `sdk-config.properties`.

## 7. Erläuterung des Kerncodes

### 7.1 RESTful-Client (automatische Anmeldung)

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

### 7.2 Initialisierung des MQTT-Clients und Callbacks

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

### 7.3 Lesen / Schreiben von Eigenschaften

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

Die Ergebnisse der Befehle werden asynchron über die Callbacks `onPropertiesWriteReply` / `onPropertiesReadReply` zurückgegeben. Halten Sie den Prozess nach dem Senden der Befehle am Laufen, damit die Antworten beobachtet werden können.

### 7.4 Geordnetes Herunterfahren

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. MQTT-Callback-Referenz

`MqttCallback` ist in `DemoApplication#testMqtt()` vollständig implementiert. Die über 30 Callbacks sind unten gruppiert; jeder aufgeführte repräsentative Name ist in der Quelldatei zu finden.

| Kategorie | Repräsentative Callbacks | Zweck |
|---|---|---|
| Verbindung | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | Verbindungsstatus, Benachrichtigung über Wiederverbindung, Bestätigung der Zustellung |
| Eigenschaften & Funktionen | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | Antworten auf vom Server gesendete Befehle |
| Berichte | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | Geräteseitige Daten, Ereignisse, Metadaten sowie Online-/Offline-Status |
| Untergeräte | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | Lebenszyklus der Untergeräte, Topologie und Nachrichten der Untergeräte |
| Firmware | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | Fortschritt des OTA-Upgrades und Firmware-Informationen |
| Szenen & System | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | Szenenverwaltung, Zeitsynchronisierung, Protokolle und Reset-Berichte |

> Hinweis: Die Callback-Rümpfe in der Demo geben lediglich die empfangenen Nutzdaten aus. Bei einer echten Integration ersetzen Sie sie durch Ihre eigene Geschäftslogik (Persistenz, Alarmierung, Weiterleitung usw.).

## 9. Häufig gestellte Fragen

### Q1: Start schlägt mit "missing valid SDK credential configuration" fehl, oder OAuth2-Anmeldung schlägt fehl / 401

**A:** Prüfen Sie zuerst `src/main/resources/sdk-config.properties`:

1. Die Datei fehlt, `appId` / `secureKey` sind leer oder noch `${...}`-Platzhalter → tragen Sie echte Zugangsdaten ein und erstellen Sie das Projekt neu;
2. Die Datei enthält noch die Standard-Platzhalter `YOUR_APP_ID` / `YOUR_SECURE_KEY` → Platzhalter werden von der Validierung nicht zurückgewiesen und als echte Zugangsdaten verwendet, sodass sich das Symptom als Fehler bei Anmeldung / Authentifizierung statt als Konfigurationsfehler zeigt. Ersetzen Sie sie durch echte Zugangsdaten;
3. Auch Zugangsdaten, die nicht zur Anwendung des Geräts gehören, führen zu Authentifizierungsfehlern — verwenden Sie die auf der IoT-Plattform für diese Anwendung ausgegebenen Zugangsdaten.

### Q2: Binden des Geräts schlägt fehl

**A:** Stellen Sie sicher, dass `String deviceSn = "xxxxxxx"` in `DemoApplication#testHttpClient()` durch eine gültige, bindbare Geräte-SN ersetzt wurde und das Gerät keinem anderen Benutzer zugeordnet ist; prüfen Sie außerdem, dass `sdk-config.properties` wie in Abschnitt 5.2 beschrieben aktualisiert wurde.

### Q3: Keine Nachrichten bei den MQ-Callbacks empfangen

**A:** Der Aufruf `unbindUserDevice(deviceSn)` am Ende von `testHttpClient()` löst die Gerätebindung, sodass nachfolgende Gerätenachrichten nicht eintreffen. In v1.0.4 ist dieser Aufruf **nicht auskommentiert** — kommentieren Sie ihn beim Debuggen der MQ-Callbacks manuell aus (einschließlich `ApiResult unbind = ...` und der Ausgabeanweisung). Prüfen Sie außerdem, dass die Zugangsdaten gültig sind und `deviceSn` eine echte Geräte-SN ist.

### Q4: Schreiben von Eigenschaften erhält keine Antwort

**A:** Prüfen Sie der Reihe nach: ob das Gerät online ist; ob die Eigenschafts-ID zum Produkt-Dingmodell passt; ob der Werttyp der Modelldefinition entspricht. Die Eigenschaft `FreezingPointEnable` im Beispiel dient nur der Demonstration — passen Sie `createTestProperties()` an Ihr eigenes Produktmodell an.

### Q5: Verbindung unterbrochen

**A:** Ermitteln Sie die Ursache über den Callback `onConnectionLost`; nach einer erfolgreichen Wiederverbindung benachrichtigt das SDK über `onConnected(reconnect, serverURI)` mit `reconnect = true`.

### Q6: Maven kann die SDK-Abhängigkeiten nicht auflösen

**A:** Die beiden SDK-Jars werden mit dem Gültigkeitsbereich `system` aus `libs/` eingebunden. Stellen Sie sicher, dass `libs/heiman-sdk-mqtt-1.0.4.jar` und `libs/heiman-sdk-restful-1.0.4.jar` an genau diesen Pfaden vorhanden sind und dass Sie aus dem Verzeichnis `heiman-sdk-demo` heraus erstellen (`${project.basedir}` muss darauf verweisen).

## 10. Hinweise zu Zugangsdaten und Sicherheit

- Das Repository enthält ausschließlich Platzhalter; übernehmen Sie niemals echte `appId`- / `secureKey`-Werte in die Versionsverwaltung und nehmen Sie Ihre lokale Konfiguration in `.gitignore` auf, falls Sie echte Zugangsdaten außerhalb der Versionsverwaltung aufbewahren.
- Wenn die SDK-Demo über `/application/{id}/sdk/download` paketiert wird, ersetzt die Plattform `sdk-config.properties` durch die echten Zugangsdaten der betreffenden Anwendung — bewahren Sie dieses heruntergeladene Paket vertraulich auf.
- `DemoApplication` gibt nur ein maskiertes Zugriffstoken aus (`abcd****wxyz`); geben Sie in Produktionsprotokollen niemals vollständige Tokens, `secureKey` oder personenbezogene Daten aus.
- Falls Zugangsdaten möglicherweise kompromittiert wurden, erneuern Sie `secureKey` umgehend auf der IoT-Plattform und erstellen / stellen Sie die Demo mit dem neuen Wert neu bereit.
- Die Demo bindet und löst echte Geräte und sendet echte Befehle. Verwenden Sie eine dedizierte Testanwendung und Testgeräte; führen Sie sie nicht gegen Produktionsdaten aus.
- `target/classes/sdk-config.properties` ist eine kompilierte Kopie Ihrer Zugangsdaten — entfernen Sie die Build-Artefakte, bevor Sie das Projekt weitergeben oder archivieren.

## 11. Technischer Support

- **E-Mail:** heimanapp1@heiman.cn
- **Website:** https://www.heimanhome.com

## 12. Versionsverlauf und Lizenz

### 12.1 Versionsverlauf

See [../CHANGELOG.md](../CHANGELOG.md) for the full history and [../RELEASE_NOTES.md](../RELEASE_NOTES.md) for this release.

- **v1.0.4:** Aktuelle Version
   - Zugangsdaten zur Laufzeit zentral über `sdk-config.properties` verwaltet; die Datei enthält nur Platzhalter und muss vor der Ausführung durch echte `appId` / `secureKey` ersetzt werden
   - `DemoApplication` als Haupteinstieg, der den vollständigen RESTful- + MQTT-Ablauf integriert
   - Automatischer Austausch der Zugangsdaten, wenn die SDK-Demo über `/application/{id}/sdk/download` auf der IoT-Plattform paketiert wird
   - Geordnete Freigabe der Ressourcen durch einen JVM-Shutdown-Hook
   - Umfassende Abdeckung der MQTT-Callbacks (Eigenschaften / Ereignisse / Untergeräte / Firmware / Szenen usw.)

### 12.2 Lizenz

Quellcode und Dokumentation dieses Repositories stehen unter der **Apache License 2.0** — siehe [../LICENSE](../LICENSE). Die proprietären Binärdateien in `libs/` und die Diagramme in `image/` sind **nicht** eingeschlossen; siehe [../NOTICE](../NOTICE).
