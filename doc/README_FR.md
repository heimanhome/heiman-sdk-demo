# Démo du SDK Heiman Smart Home (heiman-sdk-demo)

> **Languages:** [English](../README.md) · [简体中文](README_CH.md) · [Русский](README_RU.md) · [Deutsch](README_DE.md) · [Français](README_FR.md) · [Italiano](README_IT.md) · [العربية](README_AR.md) · [Español](README_ES.md) · [Português](README_PT.md) · [Polski](README_PL.md)

| Document | Valeur |
|---|---|
| Langue | Français |
| S'applique à | Heiman Smart Home SDK **v1.0.4** (`heiman-sdk-demo`) |
| Environnement d'exécution | JDK 8+, Maven 3.6+ |
| Statut | Documentation officielle |
| Dernière mise à jour | 2026-11-13 |

## Sommaire

1. Vue d'ensemble
2. Avant l'exécution (modifications requises)
3. Déroulement de la démo
4. Structure du projet
5. Démarrage rapide
6. Référence de configuration
7. Présentation du code principal
8. Référence des callbacks MQTT
9. Foire aux questions
10. Notes sur les identifiants et la sécurité
11. Support technique
12. Historique des versions et licence

## 1. Vue d'ensemble

`heiman-sdk-demo` est le projet de démonstration officiel et complet du SDK Heiman Smart Home (v1.0.4). Conçu autour de `com.heiman.sdk.demo.DemoApplication`, il présente de bout en bout les deux flux d'intégration côté serveur essentiels :

- **Gestion des appareils RESTful** : connexion OAuth2 automatique, association/dissociation des appareils, interrogation paginée de la liste des appareils, liste des produits et analyse du ThingModel.
- **Accès aux appareils via MQTT** : initialisation du client MQTT, commandes de lecture/écriture des propriétés, ainsi que l'enregistrement et le traitement de plus de 30 callbacks couvrant les rapports de propriétés, les rapports d'événements, la gestion des sous-appareils, les mises à jour du micrologiciel, les scénarios, la synchronisation de l'heure, etc.

Il comprend également une démonstration du chargement du modèle d'objet (`DeviceDemo`), des démonstrations des schémas d'appel Web API / OAuth2 (package `client/`) et une démonstration de l'abonnement aux entités / de l'enregistrement des commandes (package `server/`).

**Public cible :** ingénieurs côté serveur intégrant la plateforme IoT Heiman pour la première fois. Ce document couvre les prérequis, la configuration des identifiants, les étapes de compilation et d'exécution, les éléments de configuration, la présentation du code, l'inventaire des callbacks, le dépannage et les notes de sécurité. Toutes les commandes et tous les extraits proviennent de cette démo et sont utilisables directement une fois les espaces réservés indiqués à la section 2 remplacés.

Le flux global d'intégration du SDK (provisionnement côté application → association et remontée des appareils → intégration côté serveur) est illustré ci-dessous :

![Flux d'intégration du SDK](../image/img.png)

## 2. Avant l'exécution (modifications requises)

> ⚠️ **Le dépôt ne contient que des espaces réservés. Trois éléments doivent être remplacés avant que la démo puisse s'exécuter correctement.**

| # | Élément | Fichier / Emplacement | Valeur par défaut | Action |
|---|---|---|---|---|
| 1 | Identifiants de l'application | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | Remplacer par les vrais `appId` / `secureKey` délivrés par la plateforme IoT Heiman (**obligatoire**) |
| 2 | SN de l'appareil | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | Remplacer par un SN d'appareil réel et associable |
| 3 | Propriété de test | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | Ajuster à un ID de propriété défini dans le modèle d'objet de votre propre produit |

**Pourquoi est-ce important :** `SdkConfig` rejette uniquement un fichier absent, des valeurs vides et les espaces réservés `${...}` non résolus. `YOUR_APP_ID` / `YOUR_SECURE_KEY` ne sont pas vides ; ils sont donc acceptés comme identifiants valides et envoyés à la plateforme. Les laisser tels quels ne provoque **pas** d'erreur "configuration manquante" : la connexion / l'authentification OAuth2 échoue simplement (par exemple HTTP 401). Consultez la section 5.2 pour la procédure de configuration complète.

## 3. Déroulement de la démo

`com.heiman.sdk.demo.DemoApplication#main` est le point d'entrée principal et s'exécute dans l'ordre suivant :

1. **DeviceDemo** : charge cinq modèles d'objet intégrés (passerelle / fumée / CO / chaleur / télécommande) depuis les ressources internes du SDK (`DeviceModels`) et affiche un aperçu de leurs propriétés, événements et fonctions.
2. **testHttpClient() (RESTful)** :
   - Construit un `ClientConfig` à partir de `sdk-config.properties`, crée un `HeimanRestfulClient` et effectue automatiquement la connexion OAuth2 ;
   - Obtient le jeton d'accès et le userId via `getCurrentToken()` ;
   - Associe un appareil avec `deviceService.bindUserDevice(deviceSn)` ;
   - Interroge la liste des appareils avec pagination (`queryDeviceList`) et affiche les détails des appareils ;
   - Interroge la liste des produits (`getProductList`), analyse les `metadata` de chaque produit en un `ThingModel` et les met en cache par productId ;
   - Dissocie enfin l'appareil via `unbindUserDevice(deviceSn)`. **Remarque : dans la v1.0.4, cet appel de dissociation est actif (non commenté) ; pour observer les messages des callbacks MQ, commentez manuellement l'appel de dissociation à la fin de `testHttpClient()` (y compris `ApiResult unbind = ...` et l'instruction d'affichage suivante).**
3. **testMqtt() (MQTT)** :
   - Construit un `MqttConfig` via `SdkConfig.get().toMqttConfig(userId)` (le broker est intégré au SDK, aucune configuration d'URL n'est nécessaire) ;
   - Implémente toutes les méthodes de `MqttCallback` (réponses de lecture/écriture des propriétés, rapports d'événements/de propriétés, mise en ligne/hors ligne des sous-appareils, progression de la mise à jour du micrologiciel, ajout/mise à jour/suppression de scénarios, état de la connexion, etc.) ;
   - Initialise avec `HeimanMqttClient.getInstance().init(mqttConfig, callback)` ;
   - Parcourt la liste des appareils interrogée à l'étape 2, construit les propriétés de test à partir du modèle d'objet du produit (l'exemple utilise la propriété `FreezingPointEnable` — ajustez `createTestProperties()` selon votre propre modèle de produit), puis envoie les commandes `propertiesWrite` et `propertiesRead`.
4. **Nettoyage à la sortie** : un hook d'arrêt de la JVM libère les ressources proprement en appelant `HeimanMqttClient.getInstance().shutdown()` et `restfulClient.close()`.

## 4. Structure du projet

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

## 5. Démarrage rapide

### 5.1 Prérequis

- JDK 8+
- Maven 3.6+ (ou simplement ouvrir le projet dans IDEA)
- Identifiants de l'application (`appId` / `secureKey`) délivrés par la plateforme IoT Heiman
- Un appareil réel et associable (deviceSn)
- Un accès réseau à la plateforme IoT Heiman ; les points de terminaison RESTful et le broker MQTT sont intégrés au SDK

### 5.2 Configuration des identifiants

> ⚠️ **Vous devez modifier `src/main/resources/sdk-config.properties`** : remplacez les espaces réservés par défaut par les vrais identifiants d'application délivrés par la plateforme IoT Heiman avant de compiler et d'exécuter.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

Liste de contrôle :

- **Laisser les espaces réservés ne provoque PAS d'erreur "configuration manquante"** : `SdkConfig` rejette uniquement un fichier absent, des valeurs vides et les espaces réservés `${...}` non résolus. `YOUR_APP_ID` / `YOUR_SECURE_KEY` sont traités comme des valeurs valides et envoyés à la plateforme, de sorte que la connexion / l'authentification OAuth2 échoue (par ex. 401) au lieu d'une erreur explicite d'identifiants ;
- Aucun identifiant réel n'est fourni dans le dépôt ; si le fichier est absent ou qu'une valeur est vide, le SDK lève `missing valid SDK credential configuration: appId/secureKey` ;
- Conservez les noms de clés `appId` / `secureKey` inchangés et enregistrez le fichier en UTF-8. `tenantId` utilise la valeur par défaut intégrée au SDK, et le broker MQTT ainsi que les points de terminaison RESTful sont également intégrés au SDK — **n'ajoutez aucune entrée supplémentaire à ce fichier** ;
- Après modification, relancez `mvn compile` (ou recompilez dans IDEA) afin que `target/classes/sdk-config.properties` dans le classpath soit à jour ;
- `deviceSn` ne figure pas dans ce fichier : remplacez également `String deviceSn = "xxxxxxx"` dans `DemoApplication#testHttpClient()` par un SN d'appareil réel, associable et non associé à un autre utilisateur ;
- Ne validez jamais d'identifiants réels dans le dépôt (seuls des espaces réservés sont conservés ici) ; si des identifiants ont pu être divulgués, renouvelez `secureKey` sur la plateforme IoT ;
- Lorsque la démo du SDK est empaquetée et téléchargée depuis la plateforme IoT via `/application/{id}/sdk/download`, ce fichier est automatiquement remplacé par les vrais identifiants de cette application — **aucune modification manuelle n'est nécessaire dans ce cas**.

### 5.3 Compilation

```bash
cd heiman-sdk-demo
mvn compile
```

Les dépendances du SDK sont fournies sous forme de jars de portée `system` dans `libs/` (binaires SDK non obscurcis) ; il n'est donc pas nécessaire de les installer dans votre dépôt local.

### 5.4 Exécution

> Avant l'exécution, vérifiez que l'étape 5.2 est terminée (`sdk-config.properties` remplacé par de vrais identifiants et projet recompilé) et que `deviceSn` ainsi que `createTestProperties()` dans `DemoApplication` ont été ajustés pour votre appareil / modèle d'objet.

La méthode recommandée consiste à ouvrir le projet dans IDEA et à exécuter :

```
com.heiman.sdk.demo.DemoApplication#main
```

Sortie console attendue, dans l'ordre : l'aperçu des modèles d'objet intégrés, un jeton d'accès masqué et le userId après la connexion, la liste paginée des appareils, la liste des produits avec les propriétés du modèle d'objet analysées, le message de connexion MQTT et les journaux des callbacks à l'arrivée des messages des appareils. Le processus reste actif afin de permettre l'observation des callbacks — appuyez sur `Ctrl+C` pour quitter ; le hook d'arrêt libère alors les deux connexions.

### 5.5 Classes de démonstration

| Classe | Description |
|---|---|
| `DemoApplication` | Point d'entrée principal : flux RESTful + MQTT complet (appels réels) |
| `ProductDemo` | Interrogation de la liste des produits (appel RESTful réel) |
| `DeviceDemo` | Chargement du modèle d'objet et affichage de l'aperçu |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | Démonstrations des schémas d'appel Web API / OAuth2 (sortie simulée) |
| `server.EntitySubscribeDemo` | Démonstration de l'abonnement aux entités / de l'enregistrement des commandes (sortie simulée) |

> Remarque : seuls `DemoApplication` et `ProductDemo` effectuent de véritables appels RESTful / MQTT. `DeviceDemo` utilise les modèles d'objet intégrés au SDK, tandis que `client/*` et `server/EntitySubscribeDemo` affichent une sortie locale simulée ; aucun d'eux n'émet de requêtes réseau réelles.

## 6. Référence de configuration

| Élément | Source | À modifier | Description |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | Oui | ID d'application délivré par la plateforme IoT Heiman ; lu par `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | Oui | Secret de l'application ; lu par `SdkConfig.getSecureKey()` ; ne jamais le valider dans le dépôt |
| `tenantId` | Intégré au SDK (`HeimanEndpoints.DEFAULT_TENANT_ID`) | Non | Renvoyé par `SdkConfig.getTenantId()` ; non lu depuis le fichier de propriétés |
| Points de terminaison RESTful / broker MQTT | Intégré au SDK | Non | Aucune configuration d'URL n'est requise ; `MqttConfig` n'a besoin que de appId, secureKey, tenantId et userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | Oui | SN de l'appareil à associer ; la valeur par défaut `xxxxxxx` est un espace réservé |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | Non | ID de l'utilisateur connecté, transmis à `toMqttConfig(userId)` à l'exécution |
| ID / valeur de propriété | `DemoApplication#createTestProperties()` | Oui, si votre modèle diffère | Doit correspondre à l'ID et au type de valeur de la propriété définis dans le modèle d'objet de votre produit |

`SdkConfig` expose `toClientConfig()` (avec des délais de 30 s pour la connexion et 60 s pour la lecture/écriture, journalisation activée) et `toMqttConfig(userId)`. Les clients RESTful et MQTT partagent donc exactement la même source d'identifiants : `sdk-config.properties`.

## 7. Présentation du code principal

### 7.1 Client RESTful (connexion automatique)

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

### 7.2 Initialisation du client MQTT et callbacks

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

### 7.3 Lecture / écriture des propriétés

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

Les résultats des commandes sont renvoyés de manière asynchrone via les callbacks `onPropertiesWriteReply` / `onPropertiesReadReply`. Maintenez le processus actif après l'envoi des commandes afin de pouvoir observer les réponses.

### 7.4 Arrêt propre

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. Référence des callbacks MQTT

`MqttCallback` est implémenté intégralement dans `DemoApplication#testMqtt()`. Les plus de 30 callbacks sont regroupés ci-dessous ; chaque nom représentatif figure dans le fichier source.

| Catégorie | Callbacks représentatifs | Rôle |
|---|---|---|
| Connexion | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | État de la connexion, notification de reconnexion, confirmation de livraison |
| Propriétés & fonctions | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | Réponses aux commandes envoyées depuis le serveur |
| Rapports | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | Données côté appareil, événements, métadonnées et état en ligne/hors ligne |
| Sous-appareils | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | Cycle de vie des sous-appareils, topologie et messages des sous-appareils |
| Micrologiciel | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | Progression de la mise à jour OTA et informations sur le micrologiciel |
| Scénarios & système | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | Gestion des scénarios, synchronisation de l'heure, journaux et rapports de réinitialisation |

> Remarque : les corps des callbacks dans la démo se contentent d'afficher la charge utile reçue. Dans une intégration réelle, remplacez-les par votre propre logique métier (persistance, alertes, transfert, etc.).

## 9. Foire aux questions

### Q1: le démarrage échoue avec "missing valid SDK credential configuration", ou la connexion OAuth2 échoue / 401

**A:** Vérifiez d'abord `src/main/resources/sdk-config.properties` :

1. Le fichier est absent, `appId` / `secureKey` sont vides ou encore des espaces réservés `${...}` → renseignez de vrais identifiants et recompilez ;
2. Le fichier contient encore les espaces réservés par défaut `YOUR_APP_ID` / `YOUR_SECURE_KEY` → les espaces réservés ne sont pas rejetés par la validation et sont utilisés comme de vrais identifiants ; le symptôme est donc un échec de connexion / d'authentification plutôt qu'une erreur de configuration. Remplacez-les par de vrais identifiants ;
3. Des identifiants n'appartenant pas à l'application de l'appareil provoquent également des échecs d'authentification — utilisez les identifiants délivrés pour cette application sur la plateforme IoT.

### Q2: l'association de l'appareil échoue

**A:** Assurez-vous que `String deviceSn = "xxxxxxx"` dans `DemoApplication#testHttpClient()` a été remplacé par un SN d'appareil valide et associable et que l'appareil n'est pas associé à un autre utilisateur ; vérifiez également que `sdk-config.properties` a été mis à jour comme décrit à la section 5.2.

### Q3: aucun message reçu sur les callbacks MQ

**A:** L'appel `unbindUserDevice(deviceSn)` à la fin de `testHttpClient()` dissocie l'appareil, de sorte que les messages ultérieurs de l'appareil n'arrivent pas. Dans la v1.0.4, cet appel est **non commenté** — commentez-le manuellement (y compris `ApiResult unbind = ...` et l'instruction d'affichage) lors du débogage des callbacks MQ. Vérifiez également que les identifiants sont valides et que `deviceSn` est un SN d'appareil réel.

### Q4: l'écriture de propriétés reste sans réponse

**A:** Vérifiez dans l'ordre : si l'appareil est en ligne ; si l'ID de propriété correspond au modèle d'objet du produit ; si le type de valeur correspond à la définition du modèle. La propriété `FreezingPointEnable` de l'exemple n'est fournie qu'à titre de démonstration — ajustez `createTestProperties()` selon votre propre modèle de produit.

### Q5: connexion perdue

**A:** Utilisez le callback `onConnectionLost` pour examiner la cause ; après une reconnexion réussie, le SDK notifie via `onConnected(reconnect, serverURI)` avec `reconnect = true`.

### Q6: Maven ne peut pas résoudre les dépendances du SDK

**A:** Les deux jars du SDK sont référencés avec la portée `system` depuis `libs/`. Assurez-vous que `libs/heiman-sdk-mqtt-1.0.4.jar` et `libs/heiman-sdk-restful-1.0.4.jar` existent à ces chemins exacts et que vous compilez depuis le répertoire `heiman-sdk-demo` (`${project.basedir}` doit y pointer).

## 10. Notes sur les identifiants et la sécurité

- Le dépôt ne contient que des espaces réservés ; ne validez jamais de vraies valeurs `appId` / `secureKey` et ajoutez votre configuration locale à `.gitignore` si vous conservez de vrais identifiants hors du contrôle de version.
- Lorsque la démo du SDK est empaquetée via `/application/{id}/sdk/download`, la plateforme remplace `sdk-config.properties` par les vrais identifiants de cette application — gardez ce package téléchargé confidentiel.
- `DemoApplication` n'affiche qu'un jeton d'accès masqué (`abcd****wxyz`) ; n'affichez jamais de jetons complets, `secureKey` ou données personnelles dans les journaux de production.
- Si des identifiants ont pu être divulgués, renouvelez immédiatement `secureKey` sur la plateforme IoT et recompilez / redéployez la démo avec la nouvelle valeur.
- La démo associe et dissocie de vrais appareils et envoie de vraies commandes. Utilisez une application de test dédiée et des appareils de test ; ne l'exécutez pas sur des données de production.
- `target/classes/sdk-config.properties` est une copie compilée de vos identifiants — supprimez les artefacts de build avant de partager ou d'archiver le projet.

## 11. Support technique

- **E-mail :** heimanapp1@heiman.cn
- **Site web :** https://www.heimanhome.com

## 12. Historique des versions et licence

### 12.1 Historique des versions

See [../CHANGELOG.md](../CHANGELOG.md) for the full history and [../RELEASE_NOTES.md](../RELEASE_NOTES.md) for this release.

- **v1.0.4 :** Version actuelle
   - Identifiants d'exécution gérés de façon centralisée via `sdk-config.properties` ; le fichier ne contient que des espaces réservés et doit être remplacé par de vrais `appId` / `secureKey` avant l'exécution
   - `DemoApplication` comme point d'entrée principal intégrant le flux RESTful + MQTT complet
   - Remplacement automatique des identifiants lors de l'empaquetage de la démo du SDK via `/application/{id}/sdk/download` sur la plateforme IoT
   - Libération propre des ressources via un hook d'arrêt de la JVM
   - Couverture complète des callbacks MQTT (propriétés / événements / sous-appareils / micrologiciel / scénarios, etc.)

### 12.2 Licence

Le code source et la documentation de ce dépôt sont sous **Apache License 2.0** — voir [../LICENSE](../LICENSE). Les binaires propriétaires de `libs/` et les schémas de `image/` sont **exclus** ; voir [../NOTICE](../NOTICE).
