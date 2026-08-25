# Toolkit

Reusable Gradle plugins for publishing Maven artifacts and Docker/OCI images.

## Maven publishing

Apply `me.whereareiam.toolkit.publish.maven` to a Maven-publishing project. The
default base URL is `https://registry.whereareiam.me/maven`.

Publishing is selected with environment variables. The canonical Maven host is
`registry.whereareiam.me`; `maven.whereareiam.me` is a deprecated
consumer-compatibility host:

```text
PUBLISH_VISIBILITY=private|public
PUBLISH_CHANNEL=development|release
PUBLISH_MAVEN_BASE_URL=https://registry.whereareiam.me/maven
PUBLISH_MAVEN_REPOSITORY=<optional exact repository key>
PUBLISH_USER=<Artifact Keeper account or service account>
PUBLISH_TOKEN=<Artifact Keeper token>
```

The repository key is `maven-private` for private publishing and
`maven-public` for public publishing. `PUBLISH_CHANNEL` remains a version
channel, not a repository selector. An explicit `PUBLISH_MAVEN_REPOSITORY`
takes precedence. CI workflows can populate `PUBLISH_USER` and
`PUBLISH_TOKEN` from the DevOps Artifact Keeper OIDC action.

## Docker publishing

Apply `me.whereareiam.toolkit.publish.docker`. It adds
`toolkitDockerLogin`, `toolkitDockerBuild`, and `toolkitDockerPush` tasks. The
default destination is private:

```text
PUBLISH_DOCKER_REGISTRY=registry.whereareiam.me
PUBLISH_VISIBILITY=private|public
PUBLISH_NAMESPACE=whereareiam
PUBLISH_USER=<Artifact Keeper account or service account>
PUBLISH_TOKEN=<Artifact Keeper token>
```

The default registry is `registry.whereareiam.me`. This resolves to
`<registry>/docker-private/<namespace>/<image>` or the corresponding
`docker-public` path. The plugin also supports `repositoryOverride` for a
non-standard registry layout.
