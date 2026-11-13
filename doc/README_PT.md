# Projeto de demonstração do SDK Heiman Smart Home (heiman-sdk-demo)

> **Languages:** [English](../README.md) · [简体中文](README_CH.md) · [Русский](README_RU.md) · [Deutsch](README_DE.md) · [Français](README_FR.md) · [Italiano](README_IT.md) · [العربية](README_AR.md) · [Español](README_ES.md) · [Português](README_PT.md) · [Polski](README_PL.md)

| Documento | Valor |
|---|---|
| Idioma | Português |
| Aplica-se a | SDK Heiman Smart Home **v1.0.4** (`heiman-sdk-demo`) |
| Ambiente de execução | JDK 8+, Maven 3.6+ |
| Estado | Documentação formal |
| Última atualização | 2026-11-13 |

## Índice

1. Visão geral
2. Antes de executar (alterações obrigatórias)
3. Fluxo de execução do Demo
4. Estrutura do projeto
5. Início rápido
6. Referência de configuração
7. Análise do código principal
8. Referência de callbacks MQTT
9. Perguntas frequentes
10. Notas sobre credenciais e segurança
11. Suporte técnico
12. Histórico de versões e licença

## 1. Visão geral

O `heiman-sdk-demo` é o projeto de demonstração oficial e abrangente do SDK Heiman Smart Home (v1.0.4). Estruturado em torno de `com.heiman.sdk.demo.DemoApplication`, apresenta de ponta a ponta os dois fluxos essenciais de integração no lado do servidor:

- **Gerenciamento de dispositivos RESTful**: login OAuth2 automático, vinculação/desvinculação de dispositivos, consulta paginada da lista de dispositivos, lista de produtos e interpretação do ThingModel.
- **Acesso a dispositivos via MQTT**: inicialização do cliente MQTT, comandos de leitura/escrita de propriedades e o registro e tratamento de mais de 30 callbacks que cobrem relatórios de propriedades, relatórios de eventos, gerenciamento de subdispositivos, atualizações de firmware, cenários, sincronização de tempo e outros.

Inclui ainda uma demonstração de carregamento do ThingModel (`DeviceDemo`), demonstrações dos padrões de chamada Web API / OAuth2 (pacote `client/`) e uma demonstração de assinatura de entidades / registro de comandos (pacote `server/`).

**Público-alvo:** engenheiros de backend que integram a plataforma IoT Heiman pela primeira vez. Este documento abrange os pré-requisitos, a configuração de credenciais, as etapas de compilação e execução, os itens de configuração, a análise do código, o inventário de callbacks, a resolução de problemas e as recomendações de segurança. Todos os comandos e trechos de código provêm deste projeto e podem ser utilizados diretamente após a substituição dos marcadores indicados na seção 2.

O fluxo geral de integração do SDK (provisionamento no aplicativo → vinculação e reporte do dispositivo → integração no servidor) é apresentado a seguir:

![Fluxo de integração do SDK](../image/img.png)

## 2. Antes de executar (alterações obrigatórias)

> ⚠️ **O repositório contém apenas marcadores de posição. Três itens devem ser substituídos antes que o Demo possa ser executado com sucesso.**

| # | Item | Arquivo / Local | Valor padrão | Ação |
|---|---|---|---|---|
| 1 | Credenciais da aplicação | `src/main/resources/sdk-config.properties` | `YOUR_APP_ID` / `YOUR_SECURE_KEY` | Substitua pelos `appId` / `secureKey` reais emitidos pela plataforma IoT Heiman (**obrigatório**) |
| 2 | SN do dispositivo | `DemoApplication#testHttpClient()` | `String deviceSn = "xxxxxxx"` | Substitua por um SN de dispositivo real e vinculável |
| 3 | Propriedade de teste | `DemoApplication#createTestProperties()` | `FreezingPointEnable` | Ajuste para um ID de propriedade definido no ThingModel do seu produto |

**Por que isso é importante:** `SdkConfig` rejeita apenas a ausência do arquivo, valores vazios e marcadores `${...}` não resolvidos. Como `YOUR_APP_ID` / `YOUR_SECURE_KEY` não são vazios, são aceitos como credenciais válidas e enviados à plataforma. Mantê-los inalterados **não** gera um erro de "configuração ausente" — simplesmente o login / a autenticação OAuth2 falha (por exemplo, HTTP 401). Consulte a seção 5.2 para o procedimento completo de configuração.

## 3. Fluxo de execução do Demo

`com.heiman.sdk.demo.DemoApplication#main` é o ponto de entrada principal e executa na seguinte ordem:

1. **DeviceDemo**: carrega cinco ThingModels integrados (gateway / fumaça / CO / temperatura / controle remoto) dos recursos incorporados ao SDK (`DeviceModels`) e imprime uma visão geral de suas propriedades, eventos e funções.
2. **testHttpClient() (RESTful)**:
   - Constrói um `ClientConfig` a partir de `sdk-config.properties`, cria um `HeimanRestfulClient` e conclui o login OAuth2 automaticamente;
   - Obtém o token de acesso e o userId por meio de `getCurrentToken()`;
   - Vincula um dispositivo com `deviceService.bindUserDevice(deviceSn)`;
   - Consulta a lista de dispositivos com paginação (`queryDeviceList`) e imprime os detalhes dos dispositivos;
   - Consulta a lista de produtos (`getProductList`), converte o `metadata` de cada produto em um `ThingModel` e os armazena em cache por productId;
   - Por fim, desvincula o dispositivo com `unbindUserDevice(deviceSn)`. **Observação: na v1.0.4 essa chamada de desvinculação está ativa (não comentada); quando precisar observar as mensagens dos callbacks MQ, comente manualmente a chamada de desvinculação no final de `testHttpClient()` (incluindo `ApiResult unbind = ...` e a instrução de impressão seguinte).**
3. **testMqtt() (MQTT)**:
   - Constrói um `MqttConfig` por meio de `SdkConfig.get().toMqttConfig(userId)` (o broker é incorporado ao SDK, sem necessidade de configurar uma URL);
   - Implementa todos os métodos de `MqttCallback` (respostas de leitura/escrita de propriedades, relatórios de eventos/propriedades, subdispositivos online/offline, progresso de atualização de firmware, inclusão/alteração/exclusão de cenários, estado da conexão etc.);
   - Inicializa com `HeimanMqttClient.getInstance().init(mqttConfig, callback)`;
   - Percorre a lista de dispositivos consultada na etapa 2, monta as propriedades de teste a partir do ThingModel do produto (o exemplo usa a propriedade `FreezingPointEnable` — ajuste `createTestProperties()` conforme o modelo do seu produto) e envia os comandos `propertiesWrite` e `propertiesRead`.
4. **Limpeza na saída**: um shutdown hook da JVM libera os recursos de forma controlada chamando `HeimanMqttClient.getInstance().shutdown()` e `restfulClient.close()`.

## 4. Estrutura do projeto

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

## 5. Início rápido

### 5.1 Requisitos

- JDK 8+
- Maven 3.6+ (ou simplesmente abra o projeto na IDEA)
- Credenciais da aplicação (`appId` / `secureKey`) emitidas pela plataforma IoT Heiman
- Um dispositivo real vinculável (deviceSn)
- Acesso de rede à plataforma IoT Heiman; os endpoints RESTful e o broker MQTT são incorporados ao SDK

### 5.2 Configurar as credenciais

> ⚠️ **É obrigatório editar `src/main/resources/sdk-config.properties`**: substitua os marcadores padrão pelas credenciais reais da aplicação emitidas pela plataforma IoT Heiman antes de compilar e executar.

```properties
# Before (repository defaults)
appId=YOUR_APP_ID
secureKey=YOUR_SECURE_KEY

# After (example — use your own real credentials)
appId=YOUR_REAL_APP_ID
secureKey=YOUR_REAL_SECURE_KEY
```

Lista de verificação:

- **Manter os marcadores NÃO gera um erro de "configuração ausente"**: `SdkConfig` rejeita apenas a ausência do arquivo, valores vazios e marcadores `${...}` não resolvidos. `YOUR_APP_ID` / `YOUR_SECURE_KEY` são tratados como valores válidos e enviados à plataforma, de modo que o login / a autenticação OAuth2 falha (por exemplo, 401) em vez de um erro explícito de credencial;
- Nenhuma credencial real acompanha o repositório; se o arquivo estiver ausente ou algum valor estiver vazio, o SDK lança `missing valid SDK credential configuration: appId/secureKey`;
- Mantenha os nomes das chaves `appId` / `secureKey` inalterados e salve o arquivo em UTF-8. `tenantId` utiliza o padrão incorporado ao SDK, e o broker MQTT e os endpoints RESTful também são incorporados ao SDK — **não adicione outras entradas a este arquivo**;
- Após a edição, execute novamente `mvn compile` (ou recompile na IDEA) para que `target/classes/sdk-config.properties` no classpath esteja atualizado;
- `deviceSn` não está neste arquivo: substitua também `String deviceSn = "xxxxxxx"` em `DemoApplication#testHttpClient()` por um SN de dispositivo real, vinculável e não associado a outro usuário;
- Nunca envie credenciais reais ao repositório (aqui são mantidos apenas marcadores); se houver suspeita de vazamento, renove o `secureKey` na plataforma IoT;
- Quando o Demo do SDK é empacotado e baixado da plataforma IoT por meio de `/application/{id}/sdk/download`, este arquivo é substituído automaticamente pelas credenciais reais daquela aplicação — **nesse caso, nenhuma edição manual é necessária**.

### 5.3 Compilação

```bash
cd heiman-sdk-demo
mvn compile
```

As dependências do SDK são fornecidas como jars com escopo `system` em `libs/` (binários SDK não ofuscados), portanto não é necessário instalá-las no repositório local.

### 5.4 Execução

> Antes de executar, confirme que a etapa 5.2 foi concluída (`sdk-config.properties` substituído por credenciais reais e o projeto recompilado) e que `deviceSn` e `createTestProperties()` em `DemoApplication` foram ajustados para o seu dispositivo / ThingModel.

A forma recomendada é abrir o projeto na IDEA e executar:

```
com.heiman.sdk.demo.DemoApplication#main
```

A saída esperada no console, em ordem: a visão geral dos ThingModels integrados, o token de acesso mascarado e o userId após o login, a lista paginada de dispositivos, a lista de produtos com as propriedades do ThingModel interpretadas, a mensagem de conexão MQTT e os registros dos callbacks à medida que as mensagens dos dispositivos chegam. O processo permanece em execução para permitir a observação dos callbacks — pressione `Ctrl+C` para sair; nesse momento, o shutdown hook libera ambas as conexões.

### 5.5 Classes de demonstração

| Classe | Descrição |
|---|---|
| `DemoApplication` | Ponto de entrada principal: fluxo completo RESTful + MQTT (chamadas reais) |
| `ProductDemo` | Consulta da lista de produtos (chamada RESTful real) |
| `DeviceDemo` | Carregamento do ThingModel e impressão da visão geral |
| `client.WebApiClientDemo` / `client.WebOAuth2ClientDemo` | Demonstrações dos padrões de chamada Web API / OAuth2 (saída simulada) |
| `server.EntitySubscribeDemo` | Demonstração de assinatura de entidades / registro de comandos (saída simulada) |

> Observação: apenas `DemoApplication` e `ProductDemo` realizam chamadas RESTful / MQTT reais. `DeviceDemo` utiliza os ThingModels incorporados ao SDK, enquanto `client/*` e `server/EntitySubscribeDemo` imprimem saída local simulada; nenhum deles emite requisições de rede reais.

## 6. Referência de configuração

| Item | Origem | Deve ser alterado | Descrição |
|---|---|---|---|
| `appId` | `src/main/resources/sdk-config.properties` | Sim | ID da aplicação emitido pela plataforma IoT Heiman; lido por `SdkConfig.getAppId()` |
| `secureKey` | `src/main/resources/sdk-config.properties` | Sim | Segredo da aplicação; lido por `SdkConfig.getSecureKey()`; nunca o envie ao repositório |
| `tenantId` | Incorporado ao SDK (`HeimanEndpoints.DEFAULT_TENANT_ID`) | Não | Retornado por `SdkConfig.getTenantId()`; não lido do arquivo de propriedades |
| Endpoints RESTful / broker MQTT | Incorporados ao SDK | Não | Nenhuma configuração de URL é necessária; `MqttConfig` requer apenas appId, secureKey, tenantId e userId |
| `deviceSn` | `DemoApplication#testHttpClient()` | Sim | SN do dispositivo a vincular; o valor padrão `xxxxxxx` é um marcador |
| `userId` | `restfulClient.getCurrentToken().getUserId()` | Não | ID do usuário autenticado, passado a `toMqttConfig(userId)` em tempo de execução |
| ID / valor da propriedade | `DemoApplication#createTestProperties()` | Sim, se o modelo for diferente | Deve corresponder ao ID e ao tipo de valor da propriedade definidos no ThingModel do seu produto |

`SdkConfig` disponibiliza `toClientConfig()` (com tempo limite de conexão de 30 s, tempos limite de leitura e escrita de 60 s e registro de logs ativado) e `toMqttConfig(userId)`. Assim, os clientes RESTful e MQTT compartilham exatamente a mesma fonte de credenciais: `sdk-config.properties`.

## 7. Análise do código principal

### 7.1 Cliente RESTful (login automático)

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

### 7.2 Inicialização e callbacks do cliente MQTT

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

### 7.3 Leitura / escrita de propriedades

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

Os resultados dos comandos são retornados de forma assíncrona pelos callbacks `onPropertiesWriteReply` / `onPropertiesReadReply`. Mantenha o processo em execução após o envio dos comandos para que as respostas possam ser observadas.

### 7.4 Encerramento controlado

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    HeimanMqttClient.getInstance().shutdown();   // Release the MQTT connection
    restfulClient.close();                       // Release the RESTful connection pool
}));
```

## 8. Referência de callbacks MQTT

`MqttCallback` é implementado integralmente em `DemoApplication#testMqtt()`. Os mais de 30 callbacks estão agrupados abaixo; cada nome representativo pode ser encontrado no arquivo de código-fonte.

| Categoria | Callbacks representativos | Finalidade |
|---|---|---|
| Conexão | `onConnected`, `onConnectionLost`, `onDeliveryComplete` | Estado da conexão, notificação de reconexão e confirmação de entrega |
| Propriedades e funções | `onPropertiesReadReply`, `onPropertiesWriteReply`, `onFunctionInvokeReply` | Respostas aos comandos enviados pelo servidor |
| Relatórios | `onPropertiesReport`, `onEventReport`, `onMetadataDerived`, `onTagsUpdate`, `onDeviceOnline`, `onDeviceOffline` | Dados do dispositivo, eventos, metadados e estado online/offline |
| Subdispositivos | `onChildOnline`, `onChildOffline`, `onChildRegister`, `onChildUnregister`, `onChildPropertiesReport`, `onChild*Reply`, `onTopoGet` | Ciclo de vida dos subdispositivos, topologia e mensagens de dispositivos filhos |
| Firmware | `onFirmwareUpgradeProgress`, `onFirmwarePull`, `onFirmwareReport`, `onFirmwareReadReply` | Progresso da atualização OTA e informações de firmware |
| Cenários e sistema | `onSceneAddReply`, `onSceneUpdateReply`, `onSceneDeleteReply`, `onSceneFireReply`, `onSceneList`, `onTimeSync`, `onLogReport`, `onSysResetReport` | Gerenciamento de cenários, sincronização de tempo, logs e relatórios de reinicialização |

> Observação: os corpos dos callbacks no Demo apenas imprimem o payload recebido. Em uma integração real, substitua-os pela sua própria lógica de negócio (persistência, alertas, encaminhamento etc.).

## 9. Perguntas frequentes

### Q1: Falha na inicialização com "missing valid SDK credential configuration" ou falha no login OAuth2 / 401

**A:** Verifique primeiramente `src/main/resources/sdk-config.properties`:

1. O arquivo está ausente, `appId` / `secureKey` estão vazios ou ainda são marcadores `${...}` → preencha as credenciais reais e recompile;
2. O arquivo ainda contém os marcadores padrão `YOUR_APP_ID` / `YOUR_SECURE_KEY` → os marcadores não são rejeitados pela validação e são usados como credenciais reais, de modo que o sintoma é uma falha de login / autenticação em vez de um erro de configuração. Substitua-os por credenciais reais;
3. Credenciais que não pertencem à aplicação do dispositivo também causam falhas de autenticação — use as credenciais emitidas para essa aplicação na plataforma IoT.

### Q2: Falha na vinculação do dispositivo

**A:** Certifique-se de que `String deviceSn = "xxxxxxx"` em `DemoApplication#testHttpClient()` foi substituído por um SN de dispositivo válido e vinculável e de que o dispositivo não está vinculado a outro usuário; confirme também que `sdk-config.properties` foi atualizado conforme a seção 5.2.

### Q3: Nenhuma mensagem recebida nos callbacks MQ

**A:** A chamada `unbindUserDevice(deviceSn)` no final de `testHttpClient()` desvincula o dispositivo, de modo que as mensagens posteriores do dispositivo não chegarão. Na v1.0.4 essa chamada **não está comentada** — comente-a manualmente (incluindo `ApiResult unbind = ...` e a instrução de impressão) ao depurar os callbacks MQ. Verifique também se as credenciais são válidas e se `deviceSn` é um SN de dispositivo real.

### Q4: A escrita de propriedades não recebe resposta

**A:** Verifique, nesta ordem: se o dispositivo está online; se o ID da propriedade corresponde ao ThingModel do produto; se o tipo de valor corresponde à definição do modelo. A propriedade `FreezingPointEnable` do exemplo serve apenas para demonstração — ajuste `createTestProperties()` conforme o modelo do seu produto.

### Q5: Perda de conexão

**A:** Use o callback `onConnectionLost` para inspecionar a causa; após uma reconexão bem-sucedida, o SDK notifica por meio de `onConnected(reconnect, serverURI)` com `reconnect = true`.

### Q6: O Maven não consegue resolver as dependências do SDK

**A:** Os dois jars do SDK são referenciados com escopo `system` a partir de `libs/`. Certifique-se de que `libs/heiman-sdk-mqtt-1.0.4.jar` e `libs/heiman-sdk-restful-1.0.4.jar` existam nesses caminhos exatos e de que a compilação seja feita a partir do diretório `heiman-sdk-demo` (`${project.basedir}` deve apontar para ele).

## 10. Notas sobre credenciais e segurança

- O repositório contém apenas marcadores; nunca envie valores reais de `appId` / `secureKey` e, se mantiver credenciais reais fora do controle de versão, adicione sua configuração local ao `.gitignore`.
- Quando o Demo do SDK é empacotado por meio de `/application/{id}/sdk/download`, a plataforma substitui `sdk-config.properties` pelas credenciais reais daquela aplicação — mantenha esse pacote baixado em caráter privado.
- `DemoApplication` imprime apenas um token de acesso mascarado (`abcd****wxyz`); nunca imprima tokens completos, `secureKey` ou dados pessoais nos logs de produção.
- Se houver suspeita de vazamento de uma credencial, renove imediatamente o `secureKey` na plataforma IoT e recompile/reimplante o Demo com o novo valor.
- O Demo vincula e desvincula dispositivos reais e envia comandos reais. Use uma aplicação e dispositivos de teste dedicados; não o execute sobre dados de produção.
- `target/classes/sdk-config.properties` é uma cópia compilada das suas credenciais — remova os artefatos de compilação antes de compartilhar ou arquivar o projeto.

## 11. Suporte técnico

- **E-mail:** heimanapp1@heiman.cn
- **Site:** https://www.heimanhome.com

## 12. Histórico de versões e licença

### 12.1 Histórico de versões

See [../CHANGELOG.md](../CHANGELOG.md) for the full history and [../RELEASE_NOTES.md](../RELEASE_NOTES.md) for this release.

- **v1.0.4:** Versão atual
   - Credenciais de execução gerenciadas centralmente por `sdk-config.properties`; o arquivo contém apenas marcadores e deve ser substituído por `appId` / `secureKey` reais antes da execução
   - `DemoApplication` como ponto de entrada principal, integrando o fluxo completo RESTful + MQTT
   - Substituição automática das credenciais quando o Demo do SDK é empacotado via `/application/{id}/sdk/download` na plataforma IoT
   - Liberação controlada de recursos por meio de um shutdown hook da JVM
   - Cobertura abrangente dos callbacks MQTT (propriedades / eventos / subdispositivos / firmware / cenários etc.)

### 12.2 Licença

O código-fonte e a documentação deste repositório são licenciados sob **Apache License 2.0** — consulte [../LICENSE](../LICENSE). Os binários proprietários em `libs/` e os diagramas em `image/` **não** estão incluídos; consulte [../NOTICE](../NOTICE).
