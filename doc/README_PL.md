# Projekt demonstracyjny SDK Heiman Smart Home (heiman-sdk-demo)

> **Languages:** [English](../README.md) · [简体中文](README_CH.md) · [Русский](README_RU.md) · [Deutsch](README_DE.md) · [Français](README_FR.md) · [Italiano](README_IT.md) · [العربية](README_AR.md) · [Español](README_ES.md) · [Português](README_PT.md) · [Polski](README_PL.md)

| Dokument | Wartość |
|---|---|
| Język | Polski |
| Dotyczy | Heiman Smart Home SDK **v1.0.4** (`heiman-sdk-demo`) |
| Środowisko uruchomieniowe | JDK 8+, Maven 3.6+ |
| Status | Oficjalna dokumentacja |
| Ostatnia aktualizacja | 2026-11-13 |

## Spis treści

1. Przegląd
2. Przed uruchomieniem (wymagane zmiany)
3. Przebieg działania projektu demonstracyjnego
4. Struktura projektu
5. Szybki start
6. Opis parametrów konfiguracji
7. Omówienie kluczowego kodu
8. Przegląd wywołań zwrotnych MQTT
9. Najczęściej zadawane pytania
10. Dane uwierzytelniające i zalecenia dotyczące bezpieczeństwa
11. Wsparcie techniczne
12. Historia wersji i licencja

## 1. Przegląd

`heiman-sdk-demo` to oficjalny, kompleksowy projekt demonstracyjny SDK Heiman Smart Home (v1.0.4). Zbudowany wokół `com.heiman.sdk.demo.DemoApplication`, w pełni przedstawia dwa kluczowe scenariusze integracji po stronie serwera:

- **Zarządzanie urządzeniami przez RESTful**: automatyczne logowanie OAuth2, wiązanie / odwiązywanie urządzeń, stronicowane zapytanie o listę urządzeń, lista produktów i analiza modelu ThingModel.
- **Dostęp urządzeń przez MQTT**: inicjalizacja klienta MQTT, polecenia odczytu / zapisu właściwości oraz rejestracja i obsługa ponad 30 wywołań zwrotnych obejmujących raporty właściwości, raporty zdarzeń, zarządzanie urządzeniami podrzędnymi, aktualizacje oprogramowania sprzętowego, scenariusze, synchronizację czasu i inne.

Projekt zawiera również demonstrację wczytywania modelu urządzenia (`DeviceDemo`), demonstracje wzorców wywołań Web API / OAuth2 (pakiet `client/`) oraz demonstrację subskrypcji encji / rejestracji poleceń (pakiet `server/`).

**Grupa docelowa:** inżynierowie po stronie serwera integrujący platformę IoT Heiman po raz pierwszy. Dokument obejmuje wymagania wstępne, konfigurację danych uwierzytelniających, kroki kompilacji i uruchomienia, parametry konfiguracji, omówienie kodu, zestawienie wywołań zwrotnych, rozwiązywanie problemów oraz zalecenia dotyczące bezpieczeństwa. Wszystkie polecenia i fragmenty kodu pochodzą z tego projektu demonstracyjnego i są gotowe do użycia po zastąpieniu symboli zastępczych wymienionych w sekcji 2.

Ogólny przebieg integracji SDK (konfiguracja po stronie aplikacji → wiązanie urządzenia i raportowanie → integracja po stronie serwera) przedstawiono poniżej:

![Przebieg integracji SDK](../image/img.png)

## 2. Przed uruchomieniem (wymagane zmiany)

> ⚠️ **Repozytorium zawiera wyłącznie symbole zastępcze. Przed pomyślnym uruchomieniem projektu demonstracyjnego należy zastąpić trzy elementy.**

| # | Element | Plik / lokalizacja | Wartość domyślna | Działanie |
|---|---|---|---|---|
| 1 | Dane uwierzytelniające aplikacji | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | Zastąp rzeczywistymi wartościami `appId` / `secureKey` wydanymi przez platformę IoT Heiman (**wymagane**) |
| 2 | SN urządzenia | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | Zastąp rzeczywistym SN urządzenia, które można związać |
| 3 | Właściwość testowa | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | Dostosuj do identyfikatora właściwości zdefiniowanego w modelu urządzenia własnego produktu |

**Dlaczego to ważne:** `SdkConfig` odrzuca jedynie brakujący plik, puste wartości i niepodstawione symbole zastępcze `${...}`. Wartości `YOUR_APP_ID` / `YOUR_SECURE_KEY` nie są puste, dlatego są przyjmowane jako prawidłowe dane uwierzytelniające i wysyłane na platformę. Pozostawienie ich bez zmian **nie** powoduje błędu "brak konfiguracji" — po prostu logowanie / uwierzytelnianie OAuth2 kończy się niepowodzeniem (np. HTTP 401). Pełną procedurę konfiguracji opisano w sekcji 5.2.

## 3. Przebieg działania projektu demonstracyjnego

`com.heiman.sdk.demo.DemoApplication#main` to główny punkt wejścia; wykonanie przebiega w następującej kolejności:

1. **DeviceDemo**: wczytuje pięć wbudowanych modeli urządzeń (brama / czujnik dymu / CO / czujnik temperatury / pilot zdalnego sterowania) z zasobów wbudowanych w SDK (`DeviceModels`) i wyświetla przegląd ich właściwości, zdarzeń i funkcji.
2. **testHttpClient() (RESTful)**:
   - Tworzy `ClientConfig` na podstawie `sdk-config.properties`, tworzy `HeimanRestfulClient` i automatycznie wykonuje logowanie OAuth2;
   - Pobiera token dostępu i userId przez `getCurrentToken()`;
   - Wiąże urządzenie wywołaniem `deviceService.bindUserDevice(deviceSn)`;
   - Wykonuje stronicowane zapytanie o listę urządzeń (`queryDeviceList`) i wyświetla szczegóły urządzeń;
   - Pobiera listę produktów (`getProductList`), parsuje `metadata` każdego produktu do `ThingModel` i buforuje je według productId;
   - Na koniec odwiązuje urządzenie przez `unbindUserDevice(deviceSn)`. **Uwaga: w v1.0.4 to wywołanie odwiązania jest aktywne (nie jest zakomentowane); aby obserwować komunikaty wywołań zwrotnych MQ, ręcznie zakomentuj wywołanie odwiązania na końcu `testHttpClient()` (łącznie z `ApiResult unbind = ...` i następującą po nim instrukcją wyświetlania).**
3. **testMqtt() (MQTT)**:
   - Tworzy `MqttConfig` przez `SdkConfig.get().toMqttConfig(userId)` (broker jest wbudowany w SDK, konfiguracja adresu URL nie jest wymagana);
   - Implementuje wszystkie metody `MqttCallback` (odpowiedzi na odczyt / zapis właściwości, raporty zdarzeń / właściwości, dostępność urządzeń podrzędnych, postęp aktualizacji oprogramowania sprzętowego, dodawanie / aktualizowanie / usuwanie scenariuszy, stan połączenia itd.);
   - Inicjalizuje wywołaniem `HeimanMqttClient.getInstance().init(mqttConfig, callback)`;
   - Iteruje po liście urządzeń pobranej w kroku 2, tworzy testowe właściwości na podstawie modelu urządzenia produktu (w przykładzie użyto właściwości `FreezingPointEnable` — dostosuj `createTestProperties()` do własnego modelu produktu), a następnie wysyła polecenia `propertiesWrite` i `propertiesRead`.
4. **Czyszczenie przy wyjściu**: przechwytywacz zamknięcia JVM zwalnia zasoby w sposób uporządkowany, wywołując `HeimanMqttClient.getInstance().shutdown()` i `restfulClient.close()`.

## 4. Struktura projektu

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

## 5. Szybki start

### 5.1 Wymagania

- JDK 8+
- Maven 3.6+ (lub po prostu otwórz projekt w IDEA)
- Dane uwierzytelniające aplikacji (`appId` / `secureKey`) wydane przez platformę IoT Heiman
- Rzeczywiste urządzenie, które można związać (deviceSn)
- Dostęp sieciowy do platformy IoT Heiman; punkty końcowe RESTful i broker MQTT są wbudowane w SDK

### 5.2 Konfiguracja danych uwierzytelniających

> ⚠️ **Należy zmodyfikować `src/main/resources/sdk-config.properties`**: przed kompilacją i uruchomieniem zastąp domyślne symbole zastępcze rzeczywistymi danymi uwierzytelniającymi aplikacji wydanymi przez platformę IoT Heiman.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

Lista kontrolna:

- **Pozostawienie symboli zastępczych NIE powoduje błędu "brak konfiguracji"**: `SdkConfig` odrzuca jedynie brakujący plik, puste wartości i niepodstawione symbole zastępcze `${...}`. Wartości `YOUR_APP_ID` / `YOUR_SECURE_KEY` są traktowane jako prawidłowe i wysyłane na platformę, dlatego zamiast jednoznacznego błędu danych uwierzytelniających występuje niepowodzenie logowania / uwierzytelniania OAuth2 (np. 401);
- Repozytorium nie zawiera żadnych rzeczywistych danych uwierzytelniających; jeśli plik nie istnieje lub wartość jest pusta, SDK zgłasza `missing valid SDK credential configuration: appId/secureKey`;
- Zachowaj bez zmian nazwy kluczy `appId` / `secureKey` i zapisz plik w kodowaniu UTF-8. Dla `tenantId` używana jest wartość domyślna wbudowana w SDK; broker MQTT i punkty końcowe RESTful również są wbudowane w SDK — **nie dodawaj do tego pliku dodatkowych wpisów**;
- Po modyfikacji uruchom ponownie `mvn compile` (lub przebuduj projekt w IDEA), aby plik `target/classes/sdk-config.properties` na ścieżce classpath był aktualny;
- `deviceSn` nie znajduje się w tym pliku: zastąp również `String deviceSn = "xxxxxxx"` w `DemoApplication#testHttpClient()` rzeczywistym SN urządzenia, które można związać i które nie jest przypisane do innego użytkownika;
- Nigdy nie zatwierdzaj rzeczywistych danych uwierzytelniających w repozytorium (znajdują się tu wyłącznie symbole zastępcze); w razie podejrzenia wycieku danych odnowij `secureKey` na platformie IoT;
- Gdy projekt demonstracyjny SDK jest pakowany i pobierany z platformy IoT przez `/application/{id}/sdk/download`, plik ten jest automatycznie zastępowany rzeczywistymi danymi uwierzytelniającymi tej aplikacji — **w takim przypadku ręczna modyfikacja nie jest potrzebna**.

### 5.3 Kompilacja

```bash
cd heiman-sdk-demo
mvn compile
```

Zależności SDK są dostarczane jako pliki jar o zakresie system w katalogu `libs/` (zwykłe pliki binarne SDK), dlatego nie ma potrzeby instalowania ich w lokalnym repozytorium.

### 5.4 Uruchomienie

> Przed uruchomieniem upewnij się, że wykonano krok 5.2 (`sdk-config.properties` zastąpiono rzeczywistymi danymi uwierzytelniającymi i projekt przebudowano) oraz że `deviceSn` i `createTestProperties()` w `DemoApplication` dostosowano do Twojego urządzenia / modelu urządzenia.

Zalecanym sposobem jest otwarcie projektu w IDEA i uruchomienie:

```
com.heiman.sdk.demo.DemoApplication#main
```

Oczekiwane dane wyjściowe w konsoli, w tej kolejności: przegląd wbudowanych modeli urządzeń, zamaskowany token dostępu i userId po logowaniu, stronicowana lista urządzeń, lista produktów z przeanalizowanymi właściwościami modeli urządzeń, komunikat o połączeniu MQTT oraz dzienniki wywołań zwrotnych w miarę napływania komunikatów od urządzeń. Proces pozostaje uruchomiony, aby można było obserwować wywołania zwrotne — naciśnij `Ctrl+C`, aby zakończyć; przechwytywacz zamknięcia zwolni wówczas oba połączenia.

### 5.5 Klasy demonstracyjne

| Klasa | Opis |
|---|---|
| `DemoApplication` | Główny punkt wejścia: pełny przebieg RESTful + MQTT (rzeczywiste wywołania) |
| `ProductDemo` | Zapytanie o listę produktów (rzeczywiste wywołanie RESTful) |
| `DeviceDemo` | Wczytywanie modelu urządzenia i wyświetlanie przeglądu |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | Demonstracje wzorców wywołań Web API / OAuth2 (symulowane dane wyjściowe) |
| `server.EntitySubscribeDemo` | Demonstracja subskrypcji encji / rejestracji poleceń (symulowane dane wyjściowe) |

> Uwaga: rzeczywiste wywołania RESTful / MQTT wykonują wyłącznie `DemoApplication` i `ProductDemo`. `DeviceDemo` korzysta z modeli urządzeń wbudowanych w SDK, natomiast `client/*` i `server/EntitySubscribeDemo` generują symulowane dane lokalne; żadne z nich nie wykonuje rzeczywistych żądań sieciowych.

## 6. Opis parametrów konfiguracji

| Parametr | Źródło | Wymaga zmiany | Opis |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | Tak | Identyfikator aplikacji wydany przez platformę IoT Heiman; odczytywany przez `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | Tak | Sekret aplikacji; odczytywany przez `SdkConfig.getSecureKey()`; nigdy nie zatwierdzaj go w repozytorium |
| `tenantId` | Wbudowane w SDK (`HeimanEndpoints.DEFAULT_TENANT_ID`) | Nie | Zwracany przez `SdkConfig.getTenantId()`; nie jest odczytywany z pliku właściwości |
| Punkty końcowe RESTful / broker MQTT | Wbudowane w SDK | Nie | Konfiguracja adresu URL nie jest wymagana; `MqttConfig` wymaga jedynie appId, secureKey, tenantId i userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | Tak | SN urządzenia do związania; domyślna wartość `xxxxxxx` jest symbolem zastępczym |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | Nie | Identyfikator zalogowanego użytkownika przekazywany w czasie wykonywania do `toMqttConfig(userId)` |
| Identyfikator / wartość właściwości | `DemoApplication#createTestProperties()` | Tak, jeśli model się różni | Muszą odpowiadać identyfikatorowi i typowi wartości właściwości zdefiniowanym w modelu urządzenia Twojego produktu |

`SdkConfig` udostępnia `toClientConfig()` (30 s limitu czasu połączenia, 60 s limitów odczytu i zapisu oraz włączone rejestrowanie dzienników) i `toMqttConfig(userId)`. Dzięki temu oba klienty — RESTful i MQTT — korzystają z dokładnie tego samego źródła danych uwierzytelniających: `sdk-config.properties`.

## 7. Omówienie kluczowego kodu

### 7.1 Klient RESTful (automatyczne logowanie)

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

### 7.2 Inicjalizacja klienta MQTT i wywołania zwrotne

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

### 7.3 Odczyt / zapis właściwości

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

Wyniki wykonania poleceń są zwracane asynchronicznie przez wywołania zwrotne `onPropertiesWriteReply` / `onPropertiesReadReply`. Po wysłaniu poleceń pozostaw proces uruchomiony, aby można było zaobserwować odpowiedzi.

### 7.4 Bezpieczne zakończenie pracy

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. Przegląd wywołań zwrotnych MQTT

`MqttCallback` jest w pełni zaimplementowany w `DemoApplication#testMqtt()`. Ponad 30 wywołań zwrotnych pogrupowano poniżej; każdą reprezentatywną nazwę można znaleźć w pliku źródłowym.

| Kategoria | Reprezentatywne wywołania zwrotne | Przeznaczenie |
|---|---|---|
| Połączenie | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | Stan połączenia, powiadomienie o ponownym połączeniu, potwierdzenie dostarczenia |
| Właściwości i funkcje | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | Odpowiedzi na polecenia wysłane przez serwer |
| Raporty | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | Dane po stronie urządzenia, zdarzenia, metadane oraz stan dostępności |
| Urządzenia podrzędne | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | Cykl życia urządzeń podrzędnych, topologia i komunikaty urządzeń podrzędnych |
| Oprogramowanie sprzętowe | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | Postęp aktualizacji OTA i informacje o oprogramowaniu sprzętowym |
| Scenariusze i system | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | Zarządzanie scenariuszami, synchronizacja czasu, dzienniki i raporty o resecie |

> Uwaga: ciała wywołań zwrotnych w projekcie demonstracyjnym jedynie wyświetlają odebrane dane. W rzeczywistej integracji zastąp je własną logiką biznesową (trwałe zapisywanie, alarmowanie, przekazywanie itd.).

## 9. Najczęściej zadawane pytania

### Q1: Uruchomienie kończy się błędem "missing valid SDK credential configuration" lub logowanie OAuth2 nie powiedzie się / 401

**A:** Najpierw sprawdź `src/main/resources/sdk-config.properties`:

1. Plik nie istnieje, `appId` / `secureKey` są puste lub nadal mają postać symboli zastępczych `${...}` → wpisz rzeczywiste dane uwierzytelniające i przebuduj projekt;
2. Plik nadal zawiera domyślne symbole zastępcze `YOUR_APP_ID` / `YOUR_SECURE_KEY` → walidacja nie odrzuca symboli zastępczych i traktuje je jako rzeczywiste dane uwierzytelniające, dlatego objawem jest niepowodzenie logowania / uwierzytelniania, a nie błąd konfiguracji. Zastąp je rzeczywistymi danymi uwierzytelniającymi;
3. Również dane uwierzytelniające, które nie należą do aplikacji urządzenia, powodują błędy uwierzytelniania — użyj danych wydanych dla tej aplikacji na platformie IoT.

### Q2: Wiązanie urządzenia kończy się niepowodzeniem

**A:** Upewnij się, że `String deviceSn = "xxxxxxx"` w `DemoApplication#testHttpClient()` zastąpiono prawidłowym SN urządzenia, które można związać, oraz że urządzenie nie jest przypisane do innego użytkownika; sprawdź również, czy `sdk-config.properties` zaktualizowano zgodnie z sekcją 5.2.

### Q3: Brak komunikatów w wywołaniach zwrotnych MQ

**A:** Wywołanie `unbindUserDevice(deviceSn)` na końcu `testHttpClient()` odwiązuje urządzenie, dlatego późniejsze komunikaty od urządzenia nie docierają. W v1.0.4 to wywołanie **nie jest zakomentowane** — podczas debugowania wywołań zwrotnych MQ zakomentuj je ręcznie (łącznie z `ApiResult unbind = ...` i instrukcją wyświetlania). Sprawdź również, czy dane uwierzytelniające są ważne i czy `deviceSn` jest rzeczywistym SN urządzenia.

### Q4: Zapis właściwości nie otrzymuje odpowiedzi

**A:** Sprawdź kolejno: czy urządzenie jest dostępne online; czy identyfikator właściwości odpowiada modelowi urządzenia produktu; czy typ wartości jest zgodny z definicją modelu. Właściwość `FreezingPointEnable` w przykładzie służy wyłącznie demonstracji — dostosuj `createTestProperties()` do własnego modelu produktu.

### Q5: Utrata połączenia

**A:** Zbadaj przyczynę za pomocą wywołania zwrotnego `onConnectionLost`; po pomyślnym ponownym połączeniu SDK powiadamia przez `onConnected(reconnect, serverURI)` z wartością `reconnect = true`.

### Q6: Maven nie może rozwiązać zależności SDK

**A:** Oba pliki jar SDK są dołączane z zakresem `system` z katalogu `libs/`. Upewnij się, że pliki `libs/heiman-sdk-mqtt-1.0.4.jar` i `libs/heiman-sdk-restful-1.0.4.jar` istnieją w dokładnie tych ścieżkach oraz że kompilacja jest wykonywana z katalogu `heiman-sdk-demo` (`${project.basedir}` musi na niego wskazywać).

## 10. Dane uwierzytelniające i zalecenia dotyczące bezpieczeństwa

- Repozytorium zawiera wyłącznie symbole zastępcze; nigdy nie zatwierdzaj rzeczywistych wartości `appId` / `secureKey` w systemie kontroli wersji, a jeśli przechowujesz rzeczywiste dane uwierzytelniające poza repozytorium, dodaj lokalną konfigurację do `.gitignore`.
- Gdy projekt demonstracyjny SDK jest pakowany przez `/application/{id}/sdk/download`, platforma zastępuje `sdk-config.properties` rzeczywistymi danymi uwierzytelniającymi tej aplikacji — zachowaj pobrany pakiet w miejscu niedostępnym dla osób postronnych.
- `DemoApplication` wyświetla wyłącznie zamaskowany token dostępu (`abcd****wxyz`); nigdy nie wypisuj w dziennikach produkcyjnych pełnych tokenów, wartości `secureKey` ani danych osobowych.
- W razie podejrzenia wycieku danych uwierzytelniających natychmiast odnowij `secureKey` na platformie IoT i przebuduj / wdróż ponownie projekt demonstracyjny z nową wartością.
- Projekt demonstracyjny rzeczywiście wiąże i odwiązuje urządzenia oraz wysyła rzeczywiste polecenia. Używaj dedykowanej aplikacji testowej i urządzeń testowych; nie uruchamiaj go na danych produkcyjnych.
- `target/classes/sdk-config.properties` to skompilowana kopia Twoich danych uwierzytelniających — przed udostępnieniem lub archiwizacją projektu usuń wyniki kompilacji.

## 11. Wsparcie techniczne

- **E-mail:** heimanapp1@heiman.cn
- **Strona internetowa:** https://www.heimanhome.com

## 12. Historia wersji i licencja

### 12.1 Historia wersji

See [../CHANGELOG.md](../CHANGELOG.md) for the full history and [../RELEASE_NOTES.md](../RELEASE_NOTES.md) for this release.

- **v1.0.4:** Bieżąca wersja
   - Dane uwierzytelniające czasu wykonania zarządzane centralnie przez `sdk-config.properties`; plik zawiera wyłącznie symbole zastępcze i przed uruchomieniem musi zostać zastąpiony rzeczywistymi wartościami `appId` / `secureKey`
   - `DemoApplication` jako główny punkt wejścia integrujący pełny przebieg RESTful + MQTT
   - Automatyczna zamiana danych uwierzytelniających podczas pakowania projektu demonstracyjnego SDK przez `/application/{id}/sdk/download` na platformie IoT
   - Uporządkowane zwalnianie zasobów przez przechwytywacz zamknięcia JVM
   - Kompleksowe pokrycie wywołań zwrotnych MQTT (właściwości / zdarzenia / urządzenia podrzędne / oprogramowanie sprzętowe / scenariusze itd.)

### 12.2 Licencja

Kod źródłowy i dokumentacja w tym repozytorium są objęte licencją **Apache License 2.0** — zobacz [../LICENSE](../LICENSE). Własnościowe pliki binarne w `libs/` oraz diagramy w `image/` **nie są** objęte licencją; zobacz [../NOTICE](../NOTICE).
