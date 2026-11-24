# AGENTS.md

Toolkit is a set of Gradle plugins for modular builds: architecture verification, code standards, versioning, release
distribution, project discovery, and Maven and Docker publishing. Other whereareiam projects apply them, often
at a development version such as `dev-e2f8ced`.

## Modules

Every folder under `plugins/` is one Gradle plugin, published as `me.whereareiam.toolkit:<folder>` together with
its plugin marker; a plugin made of several modules keeps them below its folder:

- `architecture`: `me.whereareiam.toolkit.architecture`, verifies API family boundaries (`verifyArchitecture`) and
  classifies each project's kind (`ArchitectureKinds`), which other plugins ask instead of guessing from names.
- `shipshape`: `me.whereareiam.toolkit.shipshape`, holds a build's Java and build files to the general rules of the
  standard and lets other plugins register their own rules and exemption kinds. Rules are project-neutral: a
  module's role comes from the architecture kind, the build's own packages from `packageRoots`, never from a
  project's names. A rule is known by its id alone, its name in lower case with hyphens, and lies in a
  `RuleCategory`; each category is one task (`verifyImports`), part of `check`, which runs whatever its rules read,
  and the Java's shape is parsed once per build (`ParsedSources`). A built-in subject's object names its category
  (`ImportRules.CATEGORY`); another plugin joins it or brings its own. The plugin is a family: `shipshape-api` holds what a rule is and reads and what other plugins build on,
  `shipshape-common` the engine (parsing, tasks, registry) and in its test fixtures the `LayoutFixture` rule tests
  use, `shipshape-rule/rule-<subject>` one subject's rules each, registered by its `<Subject>Rules` object through
  the same `shipshape` block other plugins use, and the plugin's own `src` only wires them. A rule depends on
  `shipshape-api` alone. A module inside a plugin publishes as its parent's artifact and its name without the
  parent's role word (`shipshape-rule-flow`); the root build derives it.
- `distribution`: `me.whereareiam.toolkit.distribution`, collects release files under their final names
  (`assembleDistribution`).
- `versioning`: `me.whereareiam.toolkit.versioning`, sets the project version from `VERSION`.
- `project-discovery`: `me.whereareiam.toolkit.project-discovery`, a settings plugin that includes every directory
  with a build script, except separate builds (a directory with its own settings script) and the directories the
  settings script excludes through `projectDiscovery { exclude(...) }`.
- `publish-maven`: `me.whereareiam.toolkit.publish.maven`, Maven publishing to `registry.whereareiam.me`.
- `publish-docker`: `me.whereareiam.toolkit.publish.docker`, Docker build, login and push tasks.

The root `build.gradle.kts` sets the group `me.whereareiam.toolkit`, the Java 21 toolchain and the publishing
repository. The version comes from the `VERSION` environment variable and is `dev` without it. `./gradlew publish`
publishes to `https://registry.whereareiam.me/maven/packages` (`PUBLISH_*` variables, see the README); there is no
Gradle Plugin Portal publication.

## Build and test

- `./gradlew build`: compiles, tests and validates every plugin (`validatePlugins`); this is what pull request
  checks run.
- `./gradlew :plugins:distribution:test` and similar for one plugin.
- `VERSION=<version> ./gradlew publishToMavenLocal` to try a plugin from another build through `mavenLocal()`.

Update the README in the same pull request when a plugin's usage changes.

## Pull requests and releases

Build each change in its own `feature/...` branch and merge it into `dev` through a pull request. Every pull
request gets the metadata check (title and label) and the checks (`./gradlew build`).

- A development build is started by hand on any branch except `release` and `release/*` and publishes
  `<branch>-<short commit>`, such as `dev-e2f8ced`. Builds that apply Toolkit pin these versions, so the format
  stays.
- Publishing the release draft tags `dev` as `v<version>`, publishes `<version>` without the `v`, and attaches
  the plugin jars. A manual run of the release workflow rebuilds and republishes an existing release.
- Entry workflows only select a trigger and call `reusable-verify.yml` and `reusable-publish.yml`; shared steps
  come from `whereareiam/devops` and `whereareiam/blockops`.

### Release notes

A merged pull request lands in the release draft as `* <title> ([#<number>](<url>))`, under the category of its
label. Readers are developers whose builds apply Toolkit's plugins.

- Titles use `Area: Description`, with a past-tense description of what changed for a build:
  `Introduced`, `Changed`, `Fixed`, `Removed`.
- Areas are the plugins: `Architecture`, `Shipshape`, `Distribution`, `Versioning`, `Discovery`, `Maven`, `Docker`. A change
  across plugins takes `Publishing` when it is about publishing, otherwise `Toolkit`. Add an area only for a new
  plugin, and add it to this list in the same pull request. Never use class or task names as an area.

| Label | Use for |
| --- | --- |
| `feature` | something new for builds: a plugin, task, setting or extension property |
| `change` | changed behavior, defaults or configuration |
| `bug` | a fix for a bug that shipped in a release |
| `dependencies` | a library update |
| `skip-changelog` | refactors, build, CI, tests and documentation, and fixes of bugs that never shipped |

Add `major` next to the category when builds must change on upgrade: a renamed or removed plugin id, task or
setting, a changed default destination, or a version that needs a newer Gradle or Java.

Text written by hand, the subtitle, the summary and upgrade notes, lives in `.github/release-notes.md`; it
replaces the draft's placeholders while its `version` matches the drafted version. When the file names an
already published version, start the next release's notes by replacing the whole file. The release workflow
stops while a placeholder is left.
