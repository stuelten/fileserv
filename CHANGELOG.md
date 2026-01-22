# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Worked on GitHub Action for branch creation [2489dda](https://github.com/stuelten/fileserv/commit/2489ddacc774c83eaa7e3ac0b18eca394074fa15)  
  `bin/issue-data-read.sh`: Read issue data from GitHub API with automatic repository discovery.  
  `bin/issue-data-get-field.sh`: New script to safely extract fields (title, label) from JSON data,
  avoiding shell injection risks.

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
