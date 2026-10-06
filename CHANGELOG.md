# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] - 2026-10-06

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.17.0 → 41.17.1 (patch)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 8.1.3 → 8.2.0 (minor)

## [2.2.0] - 2026-10-05

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.14.0 → 41.17.0 (minor)

## [2.1.1] - 2026-10-02

### Dependencies
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 8.1.1 → 8.1.3 (patch)

## [2.1.0] - 2026-10-01

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.13.0 → 41.14.0 (minor)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 11.5.0 → 11.7.0 (minor)
- **ch.admin.bit.jeap:jeap-opensearch-index-writer-service-instance**: 6.7.0 → 6.8.0 (minor)

## [2.0.0] - 2026-10-01

### Dependencies
- **ch.admin.bit.jeap:jeap-spring-boot-parent**: 41.5.0 → 41.13.0 (minor)
- **org.opensearch:opensearch-testcontainers**: 2.1.3 → 4.1.0 (major)
- **ch.admin.bit.jeap:jeap-oauth-mock-server**: 7.2.0 → 11.5.0 (major)
- **ch.admin.bit.jeap:jeap-opensearch-index-writer-service-instance**: 6.2.1 → 6.7.0 (minor)
- **ch.admin.bit.jeap.jme:jme-spring-boot-integration-test**: 5.5.0 → 8.1.1 (major)

## [1.1.2] - 2026-09-16

### Added

- Demonstrate custom OpenSearch analyzers, normalizers, tokenizers, token filters and character filters with transit
  decision V3.
- Verify accent folding, case normalization and preservation of the original source values end to end.

### Changed

- Use jEAP parent 41.5.0 and OpenSearch index writer 6.2.1.

## [1.1.1] - 2026-09-03

### Added

- Let callers page and sort the transit-document and transit-decision inspection searches with a Spring Data
  `Pageable` (`page`, `size`, `sort`).

### Fixed

- Sort the transit-document and transit-decision inspection searches by `origin.created` descending by default. The
  searches cap the result at a page size; without an explicit sort OpenSearch returned an arbitrary slice of the
  matches, so a freshly indexed item could stay invisible for good once more documents matched than fit on a page.

## [1.1.0] - 2026-08-14

### Added

- Demonstrate top-level, nested and deeply nested OpenSearch collection fields in the transit-document flow.
- Provide authorization-aware searches for scalar, collection and nested transit-document fields.

### Changed

- Use jEAP parent 39.0.1.

## [1.0.1] - 2026-08-11

### Fixed

- Avoid duplicate source artifacts while retaining required publication classifiers.

## [1.0.0] - 2026-08-10

### Added

- Portable resource, index-writer, inspection and OAuth mock applications.
- Local Kafka, Schema Registry, OpenSearch and OpenSearch Dashboards infrastructure.
- Event-driven indexing for transit documents and transit decisions.
- Authorization-aware inspection APIs and a verified local walkthrough.
- Automated end-to-end tests for the transit-document and transit-decision indexing flows.
- Optional Process Archive decree and decree-document integration.
