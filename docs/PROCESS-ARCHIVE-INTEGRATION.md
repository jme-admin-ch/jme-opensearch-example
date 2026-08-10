# Process Archive Integration

The OpenSearch example can index decrees and decree documents produced by the
[JME Process Archive example](https://github.com/jme-admin-ch/jme-process-archive-example). This is an optional
cross-application demonstration: the OpenSearch example builds, tests and runs its transit flow without a Process
Archive deployment.

## Flow

1. Process Archive stores a decree or decree document and publishes `SharedArchivedArtifactVersionCreatedEvent`.
2. The OpenSearch index writer consumes that event from an environment-configured topic.
3. `JmeDecreeCondition` and `JmeDecreeDocumentCondition` select the operation matching the event's archived data schema.
4. The corresponding reference provider creates an OpenSearch origin reference from the storage bucket, object key and
   object version.
5. The index writer obtains an OAuth2 client-credentials token and retrieves the SearchItem from the Process Archive
   SearchItems API.
6. The index writer upserts the SearchItem as `JmeDecree` or `JmeDecreeDocument`.
7. The inspection service exposes the indexed data through `/api/decrees` and `/api/decreedocuments`.

## Portable Implementation

The OSS modules own all application code and public dependencies:

| Module | Portable responsibility |
| --- | --- |
| `jme-opensearch-index-writer-service` | Archived-artifact event type dependency, decree index types, conditions and reference providers |
| `jme-opensearch-inspection-service` | Decree index types and authorization-aware inspection controllers |

The event and index-type artifacts are available from Maven Central. No Nivel or AWS dependency is required to compile
the integration.

## Deployment Configuration Contract

An environment that enables the integration supplies:

- A `SharedArchivedArtifactVersionCreatedEvent` entry in `opensearch/messages.json`.
- The event topic name.
- Separate operations for `JmeDecree` and `JmeDecreeDocument`.
- The Process Archive SearchItems API base URI, normally exposed as `jme.opensearch.archive.base-uri`.
- An OAuth2 client registration referenced by the message operations, such as `pas-index-writer-read-client`.
- OpenSearch connectivity and the normal index-writer Kafka configuration.

These values intentionally do not appear in the standalone local profile. Deployment-specific packaging and environment
configuration supply the concrete endpoint, topic, OAuth and secret settings.

## Ownership

| Concern | Owner |
| --- | --- |
| Conditions, reference providers and inspection controllers | This OSS repository |
| Message and index-type artifacts | Public JME registries and Maven Central |
| Process Archive application behavior | JME Process Archive example |
| Environment endpoints, topics, OAuth clients and secrets | Platform deployment configuration |
| Deployed cross-application verification | Platform wrapper smoke tests |
