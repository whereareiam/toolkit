# Toolkit

Reusable Gradle plugins for modular builds, architecture verification, versioning, and publishing.

## Architecture verification

Apply `me.whereareiam.toolkit.architecture` to the root project and run
`./gradlew verifyArchitecture`. The root `check` task also runs verification.
All projects are checked; applying the plugin to a child project enables its
`architecture` configuration block.

Implementations may depend on APIs from their own family, explicitly approved
ancestor contracts, and the build's shared root API. Family APIs follow the same ownership rule and expose project dependencies
through `api` (or a configuration inherited by `api`). The shared root API cannot
have project dependencies. Assemblies may compose other modules.

Families follow API ownership rather than the top-level directory:

- An API named `api` or ending in `-api` belongs to its immediate non-root parent.
- An implementation belongs to the nearest non-root ancestor, including itself,
  that directly contains an API module. Grouping directories with only deeper APIs
  do not combine those families.
- A module with no inferred owner is its own family. Flat layouts can explicitly
  give their API and implementation the same family identifier.
- A direct root child named `api` or `<root-name>-api` is the shared root API;
  matching the root name is case-insensitive. At most one shared root API is allowed.

For example, `:environment:java:java-api` and its owning `:environment:java`
implementation are one family; `:environment:cache:cache-api` belongs to another.
A dependency between them fails verification.

Apply the plugin to a module to override its inferred ownership when needed:

```kotlin
architecture {
    family = "billing"
    // kind = api, implementation, or assembly
    // rootApi = true for a custom shared API, or false to disable inferred sharing
}
```

An owning module's family override applies to its unconfigured API children and
implementations. Gradle-plugin projects and source-free dependency bundles are inferred as assemblies.
Custom or generated main source roots count as implementations even before generated
files exist; a `:default` name alone does not grant an exemption. Other packaging
modules can explicitly use `kind = assembly`.

### Shared contracts for nested API owners

Client and server APIs can share a contract API owned by an ancestor without
becoming one family:

```text
agent/
├── agent-api/
├── client/
│   └── client-api/
└── server/
    └── server-api/
```

Apply the architecture plugin and declare the shared API in both `agent/client/build.gradle.kts`
and `agent/server/build.gradle.kts`:

```kotlin
architecture {
    sharedApis = setOf(":agent:agent-api")
}
```

Each owner and its API children and implementations may then depend on
`:agent:agent-api`. Declare API dependencies through `api` as usual. The client
still cannot depend on `:agent:server:server-api` or the server implementation,
and the shared contract cannot depend back on either role API.

The declaration accepts only specific APIs owned by a strict ancestor of the
declaring API owner. Sibling APIs, unrelated APIs, implementations, the shared
root API, and APIs already in the owner's family are rejected. It can be placed
on an API for that API alone, or on its owning project for its family. A nested
project with its own API starts a separate family and does not inherit the
ancestor's permissions. No permission is granted just by sharing a directory.

Sharing is directed and is not transitive. If an approved contract re-exports
another ancestor API, each consumer must explicitly approve that API too.
Verification checks the exported dependency paths, including when an
implementation consumes only its own API, so re-exports cannot hide a foreign
contract behind an allowed dependency.

Verification checks declared project dependencies in `api`, `implementation`,
`compileOnly`, `compileOnlyApi`, `runtimeOnly`, `embedded`, and their inherited
configurations. Main compile/runtime classpaths and outgoing API/runtime variants
are included as well. Test and test-fixture configurations are excluded unless inherited
into production. Forbidden API re-exports fail at the declaring API module. The task
does not inspect Java signatures, bytecode, or externally resolved Maven artifacts.

## Maven publishing

Apply `me.whereareiam.toolkit.publish.maven` to a Maven-publishing project. The
default base URL is `https://registry.whereareiam.me/maven`.

Publishing is selected with environment variables. The canonical Maven host is
`registry.whereareiam.me`; `maven.whereareiam.me` is a deprecated
consumer-compatibility host:

```text
PUBLISH_VISIBILITY=private|public
PUBLISH_MAVEN_BASE_URL=https://registry.whereareiam.me/maven
PUBLISH_MAVEN_REPOSITORY=<optional exact repository key>
PUBLISH_USER=<Artifact Keeper account or service account>
PUBLISH_TOKEN=<Artifact Keeper token>
```

The repository key is `packages-private` for private publishing and `packages`
for public publishing. An explicit `PUBLISH_MAVEN_REPOSITORY` takes
precedence. CI workflows can populate `PUBLISH_USER` and `PUBLISH_TOKEN` from
the DevOps Artifact Keeper OIDC action.

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
`<registry>/images-private/<namespace>/<image>` or the corresponding `images`
path. The plugin also supports `repositoryOverride` for a
non-standard registry layout.
