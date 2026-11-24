# Toolkit

Gradle plugins for modular builds: they find a build's projects, check the boundaries between its modules, version
it, collect its release files, and publish it to Maven and Docker registries. Each plugin stands on its own; a build
applies only the ones it needs.

| Plugin | ID | Applied in | What it does |
| --- | --- | --- | --- |
| [Project discovery](#project-discovery) | `me.whereareiam.toolkit.project-discovery` | `settings.gradle.kts` | Includes every directory with a build script as a project |
| [Architecture](#architecture) | `me.whereareiam.toolkit.architecture` | the root project, and modules that override their role | Fails the build when a module depends on another family's API or implementation |
| [Versioning](#versioning) | `me.whereareiam.toolkit.versioning` | any project | Sets the project version from the `VERSION` environment variable |
| [Distribution](#distribution) | `me.whereareiam.toolkit.distribution` | the project that ships release files | Collects release files under their final names in one directory |
| [Maven publishing](#maven-publishing) | `me.whereareiam.toolkit.publish.maven` | each published module | Publishes a module, its sources and Javadoc to `registry.whereareiam.me` |
| [Docker publishing](#docker-publishing) | `me.whereareiam.toolkit.publish.docker` | each project with a `Dockerfile` | Builds an image and pushes it to `registry.whereareiam.me` |

## Getting the plugins

The plugins are published to `https://registry.whereareiam.me/maven/packages`, not to the Gradle Plugin Portal. Add
that repository to the plugin repositories in `settings.gradle.kts`, then apply a plugin by its ID and version:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://registry.whereareiam.me/maven/packages")
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    id("me.whereareiam.toolkit.architecture") version "0.3.0"
}
```

Releases are versioned `0.3.0` and are listed on the releases page. A development build of a branch is versioned
`<branch>-<short commit>`, such as `dev-e2f8ced`, and is published to the same repository, so a build can pin a
change before it is released.

## Plugins

Click a plugin to open its guide.

<a id="project-discovery"></a>
<details>
<summary><strong>Project discovery</strong> · <code>me.whereareiam.toolkit.project-discovery</code></summary>

### What it is for

A build with many modules usually lists every one of them in `settings.gradle.kts` with `include(...)`, and each new
module means another line there. Project discovery replaces that list: every directory below the root that holds a
`build.gradle.kts` becomes a project, named after its path. `libs/core` becomes `:libs:core`, and
`libs/core/core-api` becomes `:libs:core:core-api`.

### Applying it

It is a settings plugin, applied in `settings.gradle.kts`:

```kotlin
plugins {
    id("me.whereareiam.toolkit.project-discovery") version "<version>"
}

rootProject.name = "example"
```

Nothing else is needed. A new module is part of the build as soon as its `build.gradle.kts` exists.

### What it leaves out

Discovery walks the tree below the root and does not enter:

- `build-logic`, `build` and hidden directories, such as `.gradle` and `.git`;
- a directory that holds its own `settings.gradle.kts` or `settings.gradle`, because that is a separate build (for
  example a standalone example or a fixtures build), together with everything below it.

Only `build.gradle.kts` counts as a build script; a directory with only a Groovy `build.gradle` is not included. The
root's own build script belongs to the root project.

### Leaving projects out of one build

Some builds should go without a few projects: a CI job that skips an IDE plugin, or a bootstrap build that leaves
out a suite needing the build's own published artifacts. Name their directories, relative to the root, in the
settings script. A condition decides it per build:

```kotlin
projectDiscovery {
    if (providers.gradleProperty("withoutIde").isPresent)
        exclude("tools/ide")
}
```

An excluded directory leaves out its projects and every project below it. It must hold at least one project discovery
would include, so a misspelled or moved directory fails the build instead of silently excluding nothing.

Discovery includes the projects once the settings script has run, so everything the script says about exclusions is
known first. As a consequence, the settings script cannot look up a discovered project, and a project left out never
exists in the build.

> [!WARNING]
> Do not remove included projects from `ProjectDescriptor.children` instead. Gradle keeps them registered, and with
> `TYPESAFE_PROJECT_ACCESSORS` it generates project accessors (`projects.tools.ide`) that disagree between builds
> with and without the removal: they share one cache entry in the Gradle user home, and whichever build ran first on
> a machine decides the accessors of both.

</details>

<a id="architecture"></a>
<details>
<summary><strong>Architecture</strong> · <code>me.whereareiam.toolkit.architecture</code></summary>

### What it is for

In a modular build, each feature usually has an API module that others use and implementation modules that only it
uses. Gradle lets any module depend on any other, so nothing stops a module from reaching into another feature's
implementation, or an API from exposing a third feature's API. The architecture plugin checks these boundaries on
every `check` and fails with the dependencies that cross them.

### Applying it

Apply it to the root project and run the check:

```kotlin
plugins {
    id("me.whereareiam.toolkit.architecture") version "<version>"
}
```

```shell
./gradlew verifyArchitecture
```

`check` runs `verifyArchitecture` as well, so `./gradlew build` fails on a violation. All projects are verified;
applying the plugin to a child project only gives it the `architecture { }` block for overrides.

### Families and roles

Every module has a role:

- an **API**, named `api` or ending in `-api`;
- an **implementation**, any other module with sources, including custom or generated main source roots, even
  before the generated files exist;
- an **assembly**, which composes other modules: Gradle plugin projects, source-free dependency bundles, and modules
  that say `kind = assembly`.

Modules group into **families** by API ownership, not by top-level directory:

- An API belongs to its immediate non-root parent.
- An implementation belongs to the nearest non-root ancestor, itself included, that directly contains an API module.
  Grouping directories that only hold deeper APIs do not merge those families.
- A module without such an owner is a family of its own. A flat layout can give its API and implementation the same
  family explicitly.
- A direct root child named `api` or `<root-name>-api` (in any letter case) is the build's **shared root API**. A
  build has at most one, and it cannot have project dependencies.

For example, `:environment:java:java-api` and its owner `:environment:java` are one family, while
`:environment:cache:cache-api` belongs to another. A dependency between them fails verification.

### The rules

- An implementation may depend on its own family's APIs, the shared root API, and the APIs its family is granted (see
  below).
- An API follows the same rule and exposes project dependencies through `api`, or a configuration that `api`
  inherits from.
- An assembly may compose any module.

Verification reads the declared project dependencies of `api`, `implementation`, `compileOnly`, `compileOnlyApi`,
`runtimeOnly`, `embedded` and the configurations they inherit, the main compile and runtime class paths, and the
outgoing API and runtime variants. Test and test-fixture configurations are left out unless production inherits
them. A forbidden API re-export fails at the API that declares it. It does not inspect Java signatures, bytecode or
external Maven artifacts.

### Overriding what is inferred

Apply the plugin to a module and set what inference gets wrong:

```kotlin
architecture {
    family = "billing"
    kind = assembly          // or api, implementation
    rootApi = true           // a custom shared root API; false turns off inferred sharing
}
```

A `family` set on an API owner also applies to its API children and implementations that set none.

### Sharing an ancestor's contract

Client and server APIs can both use a contract API their common ancestor owns without becoming one family:

```text
agent/
├── agent-api/
├── client/
│   └── client-api/
└── server/
    └── server-api/
```

Declare the shared API in `agent/client/build.gradle.kts` and `agent/server/build.gradle.kts`:

```kotlin
architecture {
    sharedApis = setOf(":agent:agent-api")
}
```

Each owner, its API children and its implementations may now depend on `:agent:agent-api`. The client still cannot
depend on `:agent:server:server-api` or the server's implementation, and the contract cannot depend back on either.

- Only specific APIs owned by a strict ancestor of the declaring owner are accepted. Sibling APIs, unrelated APIs,
  implementations, the shared root API and APIs of the owner's own family are refused.
- Put the declaration on an API for that API alone, or on its owner for the whole family. A nested project with its
  own API starts a family of its own and inherits nothing. Sharing a directory grants nothing.
- Sharing is not transitive: when an approved contract re-exports another ancestor API, each consumer approves that
  API too. Verification follows the exported dependency paths, even when an implementation uses only its own API, so
  a re-export cannot hide a foreign contract behind an allowed one.

### Building on another family's API

A family may build on the API of a sibling or unrelated family, such as a feature that uses another feature's
contract:

```text
capability/
├── commands/
│   └── commands-api/
└── dialogs/
    └── dialogs-api/
```

Declare it in `capability/dialogs/build.gradle.kts`:

```kotlin
architecture {
    dependsOnApis = setOf(":capability:commands:commands-api")
}
```

Dialogs, its API children and its implementations may now depend on `:capability:commands:commands-api`. Commands
still cannot depend on dialogs, and no other family gains anything. Like `sharedApis`, the declaration names specific
APIs, belongs on an API or its owner, is not inherited by nested API owners and is not transitive. An ancestor's
contract belongs in `sharedApis` instead, and two families must not depend on each other's APIs.

### Reference

| `architecture { }` setting | Default | Meaning |
| --- | --- | --- |
| `kind` | inferred | `api`, `implementation` or `assembly` |
| `family` | inferred | Family identifier; on an API owner, also for its unconfigured children |
| `rootApi` | inferred | `true` marks the shared root API, `false` stops a matching name from being one |
| `sharedApis` | empty | Ancestor-owned contract APIs this API or family may use |
| `dependsOnApis` | empty | Other families' APIs this API or family builds on |

| Task | Project | What it does |
| --- | --- | --- |
| `verifyArchitecture` | root | Fails with every violation; `check` depends on it |

</details>

<a id="versioning"></a>
<details>
<summary><strong>Versioning</strong> · <code>me.whereareiam.toolkit.versioning</code></summary>

### What it is for

CI decides a build's version: a release workflow publishes `1.2.3`, a development build `dev-e2f8ced`, and a build on
a developer's machine has none. The versioning plugin reads that version from an environment variable, so the build
script never hard-codes it, and tells releases from development builds.

### Applying it

```kotlin
plugins {
    id("me.whereareiam.toolkit.versioning") version "<version>"
}
```

```shell
VERSION=1.2.3 ./gradlew publish
```

After the project is evaluated, its `version` is the value of `VERSION`, or `dev` when the variable is missing or
blank. Because it is set after evaluation, read the version lazily (in a task action or a provider) rather than at the
top of the build script, or use `toolkitVersioning.resolvedVersion()`.

### Release or development

`toolkitVersioning.resolvedChannel()` is `release` when the version is a semantic version, optionally prefixed with
`v` (`1.2.3`, `v1.2.3-rc.1`, `1.2.3+build.5`), and `development` for anything else, such as `dev` or `dev-e2f8ced`.
A build can use it to choose where to publish:

```kotlin
val channel = toolkitVersioning.resolvedChannel()
tasks.register("printChannel") {
    doLast { println(channel.get()) }
}
```

### Reference

| `toolkitVersioning { }` setting | Default | Meaning |
| --- | --- | --- |
| `environmentVariableName` | `VERSION` | Environment variable the version is read from |
| `defaultVersion` | `dev` | Version when the variable is missing or blank |
| `releasePattern` | semantic version, optional `v` | Regular expression a release version matches |
| `channelOverride` | none | Channel to report instead of the classified one, lowercased |

| Method | Returns |
| --- | --- |
| `resolvedVersion()` | Provider of the version the project gets |
| `resolvedChannel()` | Provider of `release` or `development`, or the override |

</details>

<a id="distribution"></a>
<details>
<summary><strong>Distribution</strong> · <code>me.whereareiam.toolkit.distribution</code></summary>

### What it is for

A release ships a handful of files built by different projects: an API JAR, a shaded bundle, a ZIP of platform JARs.
Each task names its output its own way, and a release workflow ends up copying and renaming them. The distribution
plugin declares the shipped files once, by a label, and writes them under consistent final names into one directory
that a workflow uploads as it is.

### Applying it

Apply it to the project that ships the files, usually the root, and declare each file by its label:

```kotlin
plugins {
    id("me.whereareiam.toolkit.distribution") version "<version>"
}

toolkitDistribution {
    file("API", ":example-api", "jar")
    file("BUNDLE", ":example-platform:bundle", "shadowJar")
    archive("PLATFORMS") {
        file("VELOCITY", ":platform-velocity", "shadowJar")
        file("BUNGEECORD", ":platform-bungeecord", "shadowJar")
    }
}
```

```shell
./gradlew assembleDistribution
```

The task builds the sources and writes `build/distribution`:

```text
Example-API-1.2.3.jar
Example-BUNDLE-1.2.3.jar
Example-PLATFORMS-1.2.3.zip   (Example-VELOCITY-1.2.3.jar, Example-BUNGEECORD-1.2.3.jar)
```

### How files are named and found

- Every file is named `<name>-<label>-<version>.<extension>`; the extension comes from the source file.
- A source is the single output of a task of another project, as above, or anything a file collection accepts that
  resolves to exactly one file, such as `tasks.named("jar")`. A task of another project is looked up only when the
  distribution is assembled, so that project need not be evaluated first.
- `archive(label) { }` packs its files into `<name>-<label>-<version>.zip`. They keep their final names inside the
  archive and are not also written beside it.
- The task empties the directory first, so it never holds files of an earlier version.

### Reference

| `toolkitDistribution { }` setting | Default | Meaning |
| --- | --- | --- |
| `name` | root project's name | First part of every file name |
| `version` | the project's version | Last part of every file name |
| `directory` | `build/distribution` | Where the files are written |
| `file(label, source)` | | Adds a file from anything that resolves to one file |
| `file(label, projectPath, taskName)` | | Adds the single output of another project's task |
| `archive(label) { }` | | Adds a ZIP of the files declared inside |

| Task | What it does |
| --- | --- |
| `assembleDistribution` | Builds the declared files and writes them under their final names |

</details>

<a id="maven-publishing"></a>
<details>
<summary><strong>Maven publishing</strong> · <code>me.whereareiam.toolkit.publish.maven</code></summary>

### What it is for

Every published module needs the same setup: a publication of its Java component, sources and Javadoc JARs, a POM
name and description, and the right repository with credentials from CI. The Maven publishing plugin does all of it,
so a module only applies the plugin, and CI decides with environment variables where it goes.

### Applying it

Apply it to each module to publish, next to the Java plugin:

```kotlin
plugins {
    `java-library`
    id("me.whereareiam.toolkit.publish.maven") version "<version>"
}
```

```shell
PUBLISH_USER=<user> PUBLISH_TOKEN=<token> ./gradlew publish
```

The module gets a publication `mavenJava` from its `java` component with a sources JAR and a Javadoc JAR (doclint
off), a POM named after the project and described by its `description`, and a repository `whereAreIAm`. `publish`
publishes there; `publishToMavenLocal` still works for trying a change from another build.

### Where it publishes

The repository URL is `<base URL>/<repository key>`. The base URL is `https://registry.whereareiam.me/maven`, and the
key comes from the visibility: `packages` for `public`, the default, and `packages-private` for `private`. An
explicit key takes precedence over the visibility. CI sets these through environment variables:

| Variable | Default | Meaning |
| --- | --- | --- |
| `PUBLISH_VISIBILITY` | `public` | `public` or `private` |
| `PUBLISH_MAVEN_BASE_URL` | `https://registry.whereareiam.me/maven` | Base URL holding the repositories |
| `PUBLISH_MAVEN_REPOSITORY` | none | Exact repository key, overriding the visibility |
| `PUBLISH_USER` | none | Account or service account; also the Gradle property `publishUser` |
| `PUBLISH_TOKEN` | none | Its token; also the Gradle property `publishToken` |

The older names `MAVEN_VISIBILITY`, `MAVEN_REPOSITORY_BASE_URL`, `MAVEN_REPOSITORY`, `MAVEN_USERNAME` and
`MAVEN_PASSWORD` are still read when the `PUBLISH_*` variable is not set. CI workflows can fill `PUBLISH_USER` and
`PUBLISH_TOKEN` from the DevOps Artifact Keeper OIDC action.

### Customising the publication

```kotlin
toolkitPublish {
    artifactId = "example-core"
    javadoc {
        title = "Example Core"
        doclint = true
    }
    pom {
        description = "The core of Example."
    }
    publication {
        pom { url = "https://github.com/whereareiam/example" }
    }
}
```

A module that publishes something other than a Java library sets `component` to its component's name, or
`includeComponent = false` to add its artifacts itself in `publication { }`; sources and Javadoc JARs are only added
for the `java` component.

### Reference

| `toolkitPublish { }` setting | Default | Meaning |
| --- | --- | --- |
| `name` | `mavenJava` | Publication name; an existing publication of that name is reused |
| `component` | `java` | Software component to publish |
| `includeComponent` | `true` | Whether the component's artifacts and dependencies are published |
| `artifactId` | project name | Artifact ID of the publication |
| `sourcesJar` | `true` | Adds a sources JAR (`java` component only) |
| `javadoc.jar` | `true` | Adds a Javadoc JAR (`java` component only) |
| `javadoc.doclint` | `false` | Runs doclint on Javadoc |
| `javadoc.title`, `javadoc.windowTitle` | none | Javadoc document and window titles |
| `pom.name` | project name | POM name |
| `pom.description` | project description, else name | POM description |
| `repositoryBaseUrl` | from `PUBLISH_MAVEN_BASE_URL` | Base URL holding the repositories |
| `repositoryVisibility` | from `PUBLISH_VISIBILITY` | `public` or `private` |
| `repositoryKeyOverride` | from `PUBLISH_MAVEN_REPOSITORY` | Exact repository key |
| `publication { }` | | Further configuration of the `MavenPublication` |

</details>

<a id="docker-publishing"></a>
<details>
<summary><strong>Docker publishing</strong> · <code>me.whereareiam.toolkit.publish.docker</code></summary>

### What it is for

A project that ships an image builds it from its `Dockerfile`, tags it with the release version, logs in to the
registry and pushes it. The Docker publishing plugin turns those steps into three tasks that run the local Docker CLI,
and derives the image's repository from the registry's layout, private unless said otherwise.

### Applying it

Apply it to the project that holds the `Dockerfile`:

```kotlin
plugins {
    id("me.whereareiam.toolkit.publish.docker") version "<version>"
}
```

```shell
PUBLISH_USER=<user> PUBLISH_TOKEN=<token> ./gradlew toolkitDockerPush
```

`toolkitDockerPush` logs in, builds the image from `Dockerfile` with the project directory as context, tags it with
the project's version, and pushes every tag. The Docker CLI must be installed; the token reaches `docker login`
through standard input, never as an argument.

### Where it pushes

The image's repository is `<registry>/images-private/<namespace>/<image>`, or `<registry>/images/<namespace>/<image>`
for public images, so `example` of version `1.2.3` becomes
`registry.whereareiam.me/images-private/whereareiam/example:1.2.3`. A registry with another layout takes a complete
repository path instead.

| Variable | Default | Meaning |
| --- | --- | --- |
| `PUBLISH_DOCKER_REGISTRY` | `registry.whereareiam.me` | Registry host, without a scheme; also `DOCKER_REGISTRY` |
| `PUBLISH_VISIBILITY` | `private` | `private` or `public` |
| `PUBLISH_NAMESPACE` | `whereareiam` | Namespace inside the repository |
| `PUBLISH_DOCKER_REPOSITORY` | none | Complete repository path, overriding the derived one; also `DOCKER_REPOSITORY` |
| `PUBLISH_USER` | none | Account or service account; also `REGISTRY_USER` |
| `PUBLISH_TOKEN` | none | Its token; also `REGISTRY_TOKEN` |

### Customising the image

```kotlin
toolkitPublishDocker {
    image = "example-server"
    tags = listOf(version.toString(), "latest")
    dockerfile = layout.projectDirectory.file("docker/Dockerfile")
    context = layout.projectDirectory
}
```

### Reference

| `toolkitPublishDocker { }` setting | Default | Meaning |
| --- | --- | --- |
| `registry` | from `PUBLISH_DOCKER_REGISTRY` | Registry host |
| `visibility` | from `PUBLISH_VISIBILITY` | `private` or `public` |
| `namespace` | from `PUBLISH_NAMESPACE` | Namespace inside the repository |
| `image` | project name | Image name |
| `repositoryOverride` | from `PUBLISH_DOCKER_REPOSITORY` | Complete repository path |
| `tags` | project version | Tags to build and push; at least one |
| `dockerfile` | `Dockerfile` | Dockerfile to build |
| `context` | project directory | Build context |
| `username`, `token` | from `PUBLISH_USER`, `PUBLISH_TOKEN` | Registry credentials |

| Task | What it does |
| --- | --- |
| `toolkitDockerLogin` | `docker login` to the registry; fails without credentials |
| `toolkitDockerBuild` | `docker build` with every tag |
| `toolkitDockerPush` | Logs in, builds, and `docker push` of every tag |

</details>

## Developing Toolkit

`./gradlew build` compiles, tests and validates every plugin. To try a change from another build, publish it to Maven
Local under a version of its own and add `mavenLocal()` to that build's plugin repositories:

```shell
VERSION=my-change ./gradlew publishToMavenLocal
```

Contribution and release rules are in [AGENTS.md](AGENTS.md).
