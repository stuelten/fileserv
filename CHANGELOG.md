# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- External configuration directory support for Docker container setup (mapping to `/app/etc`).
- Automation script `test/run-full-test.sh` for full environment setup, data generation, and WebDAV testing.
- GitHub Action "start-test-setup" for automated testing.
- Shaded executable JARs for CLI tools (`fileserv-smbpasswd`, `fileserv-test-generate-hierarchy`, `fileserv-test-webdav`).
- Option `--allow-http` to allow Basic Authentication over insecure connections (for testing/local dev).

### Fixed
- Invalid signature errors in shaded JARs by excluding `META-INF/*.SF`, `*.DSA`, `*.RSA`.
- Versioned JAR names handling in test scripts.
- Docker entrypoint now correctly picks up external authentication configurations.

## [0.2.0] - 2026-01-22

### Added
- Feature: `--size` option in test data generator generates a total size of bytes for files and directories combined 
  (rather than size per file) [9a52570](https://github.com/stuelten/fileserv/commit/9a525703253d18176e9a1253af52dbaac8d1cc93)

### Changed
- Updated test data generator documentation [1ae690e](https://github.com/stuelten/fileserv/commit/1ae690e97542ae859c9f9351e881f11d380cc59e)
- Refined GitHub Actions configurations [8dce462](https://github.com/stuelten/fileserv/commit/8dce46263210004fc74d5084447763c32272f120)

### Improved
- Simplified error handling by moving `exit 1` into the `error()` function.

### Removed
- Redundant picocli version properties [1ae690e](https://github.com/stuelten/fileserv/commit/1ae690e97542ae859c9f9351e881f11d380cc59e)

[Unreleased]: https://github.com/stuelten/fileserv/compare/0.2.0...HEAD
[0.2.0]: https://github.com/stuelten/fileserv/releases/tag/0.2.0
