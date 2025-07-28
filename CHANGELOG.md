# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.6] - 2025-07-28
### Added
- Initial Docker support with multi-stage builds.
- Docker Compose configuration for running the analyzer.
- Maven Shade Plugin for creating an executable JAR.
- Dependencies for Apache JMeter, OpenCSV, and JMeter Plugins.
- CSV report generation and performance summary features.

### Fixed
- Resolved issue with missing JAR file during Docker build.

##  [Unreleased]
### Added
- Initial release of JMeter Analyzer.
- Support for analyzing JMeter test results and generating CSV reports.
- Integration with Apache JMeter binaries.
- Documentation for building and running the project locally and in Docker.