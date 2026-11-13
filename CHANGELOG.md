# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.4] - 2026-11-13

Initial release of `heiman-sdk-demo` as a standalone Git repository with a
clean history and no remote configured. See [RELEASE_NOTES.md](RELEASE_NOTES.md)
for installation and compatibility details.

### Added

- `LICENSE` (Apache-2.0) and `NOTICE` defining the exact scope of the licence,
  the proprietary binaries under `libs/` and the third-party component list.
- `CHANGELOG.md`, `RELEASE_NOTES.md`, `sbom.json` and a `v1.0.4` version tag.
- Standalone `pom.xml` with explicit `1.0.4` coordinates, UTF-8 / Java 8
  compiler settings, licence metadata and an opt-in `sbom` profile.
- Repository-level `.gitignore` covering build output, IDE files, archives and
  local credential files, with a `!libs/*.jar` exception for build
  reproducibility.
- Multilingual documentation: `README.md` (EN) plus nine translations in `doc/`.

### Changed

- Removed the inheritance from `com.heiman.sdk:heiman-sdk:1.0.4`; the demo now
  builds from a fresh clone without the original parent reactor.
- Removed the unused `spring-webflux` / `reactor-netty-http` dependencies
  (Java 17 baseline and a milestone release), `commons-codec`,
  `javaparser-core` and `jackson-dataformat-xml`, together with the unused
  `Mono` import. The documented `JDK 8+` requirement is now accurate.
- Aligned `logback-classic` (1.2.13) with `slf4j-api 1.7.36`.
- Replaced the shaded, obfuscated fat jars in `libs/` (SDK classes relocated
  to `obf.*`) with plain SDK binaries whose `com.heiman.sdk.*` class names
  match the demo sources; third-party classes are no longer bundled.
- Unified the licence statement in every README with explicit references to
  `LICENSE` and `NOTICE`.

### Fixed

- A fresh clone is now buildable: `mvn clean compile` succeeds without a
  pre-populated local Maven repository for the Heiman parent POM.
- Repository contains no real credentials: `sdk-config.properties` ships with
  placeholders only, and the access token printed by `DemoApplication` is
  masked.

### Security

- This repository was created with a clean history, so the two `appId` /
  `secureKey` pairs from the previous repository's history are not present
  here. Creating a new Git history does NOT revoke them: both `secureKey`
  values must still be rotated on the Heiman IoT platform, and any device
  serial numbers or `clientSecret` values from those commits should be
  reviewed as well.
