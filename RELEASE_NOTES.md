# Release Notes — v1.0.4

| Item | Value |
|---|---|
| Artifact | `com.heiman.sdk:heiman-sdk-demo:1.0.4` |
| Tag | `v1.0.4` |
| Release date | 2026-11-13 |
| Status | Initial standalone repository release |
| Licence | Apache-2.0 for source and docs; see [LICENSE](LICENSE) and [NOTICE](NOTICE) |

## What is included

A single-module Maven demo showing the two integration paths of the Heiman
Smart Home Server SDK:

- **RESTful**: OAuth2 auto-login, device bind/unbind, paged device list, product
  list and thing-model parsing.
- **MQTT**: client initialisation, property read/write commands, and 30+
  callbacks (property/event reports, sub-devices, firmware upgrade, scenes,
  time sync, connection state).
- Additional demos: `DeviceDemo` (built-in thing models), `ProductDemo`,
  `client/*` and `server/EntitySubscribeDemo` (local simulations, no network).
- Documentation: `README.md` (EN) and nine translations under `doc/`.
- Release artefacts: `CHANGELOG.md`, `sbom.json` (CycloneDX), `LICENSE`, `NOTICE`.

## Requirements

- JDK 8 or newer (compiler is pinned to `--release 8`)
- Maven 3.6 or newer
- `appId` / `secureKey` issued by the Heiman IoT platform
- A real, unbound device SN
- Network access to the Heiman IoT platform; REST endpoints and the MQTT broker
  are built into the SDK

## Install and run

```bash
git clone <this-repository-url> heiman-sdk-demo
cd heiman-sdk-demo
mvn clean compile
```

Then replace the placeholders in `src/main/resources/sdk-config.properties`,
adjust `deviceSn` and `createTestProperties()` in `DemoApplication`, and run
`com.heiman.sdk.demo.DemoApplication#main`. See [README.md](README.md) for the
full walkthrough and troubleshooting.

The two proprietary SDK jars in `libs/` are part of this repository because
`pom.xml` references them with `system` scope. They contain the SDK classes
only; third-party dependencies are resolved by Maven. Do not delete or rename
them.

## Compatibility with the previous repository

This release is the first commit of a **new, independent Git repository**. It
has a clean history, no configured remote, and does not inherit the parent
reactor of the previous `heiman-sdk` monorepo.

- `pom.xml` no longer inherits from `com.heiman.sdk:heiman-sdk:1.0.4`; the
  coordinates, Java 8 compiler settings and licence metadata are local.
- `spring-webflux` / `reactor-netty-http` (Java 17 baseline, milestone release)
  and other unused dependencies were removed; `logback-classic` was aligned
  with `slf4j-api 1.7.x`.
- The shaded, obfuscated fat jars were replaced with plain SDK binaries whose
  `com.heiman.sdk.*` class names match the demo sources.

## Known limitations

- `DemoApplication` and `ProductDemo` perform real REST/MQTT calls; valid
  credentials and a bindable device are required.
- `testHttpClient()` unbinds the device at the end. Comment out that call to
  observe MQTT callbacks.
- `libs/*.jar` are proprietary binaries and are **not** open source (see
  `NOTICE`). For a public open-source release, remove them and resolve the SDK
  from an authorised artifact repository instead.
- No CI pipeline is configured yet; build verification is manual.
- This release does not cover `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`,
  `SECURITY.md` or issue/PR templates.

## Security notes

- `sdk-config.properties` contains placeholders only. Never commit real
  credentials; use `sdk-config.local.properties` (gitignored) for local values.
- `DemoApplication` prints only a masked access token.
- Two `appId` / `secureKey` pairs appeared in the history of the previous
  repository. Creating this new repository does **not** revoke them — rotate
  both `secureKey` values on the Heiman IoT platform. Also review any device
  serial numbers and `clientSecret` values from those commits.

## Verification performed for this release

- `mvn clean compile` succeeds from a fresh clone of this repository.
- `mvn -P sbom package` generates a CycloneDX SBOM; the checked-in copy is
  [sbom.json](sbom.json).
- Repository contains no real credentials, personal phone numbers, IDE
  settings or build output.

See [CHANGELOG.md](CHANGELOG.md) for the complete change list.
