# Toolkit

Gradle plugins for modular builds: they find a build's projects, check the boundaries between its modules, hold its
code to a standard, version it, collect its release files, and publish it to Maven and Docker registries. Each plugin stands on its own; a build
applies only the ones it needs.

| Plugin | ID | Applied in | What it does |
| --- | --- | --- | --- |
| [Project discovery](#project-discovery) | `me.whereareiam.toolkit.project-discovery` | `settings.gradle.kts` | Includes every directory with a build script as a project |
| [Architecture](#architecture) | `me.whereareiam.toolkit.architecture` | the root project, and modules that override their role | Fails the build when a module depends on another family's API or implementation |
| [Shipshape](#shipshape) | `me.whereareiam.toolkit.shipshape` | the root project | Fails the build where its sources and build scripts break the standard's general rules |
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

Other plugins ask for a project's kind through `ArchitectureKinds.of(project)`, which returns the `kind` its
`architecture` block states and otherwise the inferred one, with or without the plugin applied; Shipshape takes
module roles from it.

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

<a id="shipshape"></a>
<details>
<summary><strong>Shipshape</strong> · <code>me.whereareiam.toolkit.shipshape</code></summary>

### What it is for

A standard is only kept where a build checks it. Shipshape reads a build's Java sources and build scripts and fails
the build where they break the standard's general rules: the shape of classes and packages, imports, Lombok,
nullability, types and tests. Other plugins can add checks of their own to the same report.

### Applying it

Apply `me.whereareiam.toolkit.shipshape` to the root project. It holds the Java and the build files of the root
project and of every module below it to a set of general rules. Every rule lies in a category, and each category
is a task of its own, part of `check`: `verifyImports` runs the rules of `imports` and writes
`build/verify-imports/report.txt`. A task reads what its rules read:

| A rule reads | As |
| --- | --- |
| The shape of the Java classes (`ShapeRule`) | Parsed by the JDK that runs Gradle, once per build however many tasks read it; nothing is compiled |
| The Java sources as text (`SourceRule`) | The files below each module's `src/*/java` |
| The build files (`BuildRule`) | Every `build.gradle.kts`, of the root, the modules and included builds, as text |

The built-in categories are `imports`, `format`, `lombok`, `models`, `types`, `language`, `packages`, `size`,
`flow`, `tests`, `documentation` and `buildfile`; a plugin adds its rules to one of them or brings a category of its
own, which gets its own task ([Registering rules](#registering-rules)).

```kotlin
plugins {
    id("me.whereareiam.toolkit.shipshape")
}
```

A task reads every file once and fails for each breach of a rule that fails, naming the rule, the place and what
is asked instead:

```text
The rules of lombok were violated:
 - hand-written-logger session-common/src/main/java/com/example/session/DefaultSessionService.java:16 declares the logger LOGGER by hand; @Slf4j writes it
```

```text
The rules of tests were violated:
 - fixed-sleep session-common/src/test/java/com/example/session/DefaultSessionServiceTest.java:22 waits a fixed time with Thread.sleep; wait for what the test expects, up to a deadline, or move the clock
```

The report holds the same lines and the count of each rule. A rule that warns prints its places and the build goes
on; every built-in rule fails.

### Settings

```kotlin
shipshape {
    // The packages the build's own types lie in; by default the root project's group.
    packageRoots.add("com.example")
    // Annotations of wiring methods, whose parameters are collaborators: Spring's @Bean.
    wiringAnnotations.add("Bean")
    // Id namespaces of the build's own convention plugins; an id of one word always is one.
    conventionPlugins.add("com.example.convention")
    // The language level of the sources; the parser reads them at it.
    javaRelease = 21
}
```

The shape of the classes is parsed with the JDK that runs Gradle. Without `javaRelease` it reads the sources at
that JDK's level; with it, a JDK older than `javaRelease`, or a runtime without the compiler, makes each task with
rules of the shape say so and hold none of them; its other rules still run. A file with a syntax error is left to the compiler.

Every task fails when `packageRoots` is empty, which it is only where neither it nor the root project's group is set.

### Module roles

Some rules hold in one role of module only: an API keeps its enums below `type/`. A module's role is its
architecture kind as [`me.whereareiam.toolkit.architecture`](#architecture) classifies it: the
`kind` its `architecture` block states, otherwise the one inferred from its name, plugins and sources (`api` or
`-api` is an API, Gradle-plugin projects and source-free dependency bundles are assemblies, everything else is an
implementation). Shipshape does not apply the architecture plugin; without it every module has its inferred kind,
and only the `architecture` block needs the plugin applied to that module. Load both plugins from one build
script classpath; a module whose `architecture` block another class loader owns fails the tasks.

### Rules

A rule is known by its id, its name in lower case with hyphens: a breach is printed and counted under it, and no two
rules a build runs share one. The built-in rules are grouped by subject, each subject a module of its own
(`me.whereareiam.toolkit:shipshape-rule-<subject>`) that registers its rules through the same `shipshape` block
another plugin uses. Shipshape adds one rule of its own to every task, `unused-exemption`: a declared exemption
excuses something. Each table below is one category and its task, `verify<Category>`.

#### Imports: `verifyImports`, `shipshape-rule-imports`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `importable-name` | text | A name is imported, not written in full, wherever nothing else in the file answers to the same simple name; of two names in full of one simple name neither is asked to be imported |
| `wildcard-import` | text | No package and no class is imported whole |
| `static-import` | text | A static import stands in the code of tests only |
| `import-order` | text | Imports stand sorted in one group, then `java.*` and `javax.*` after a blank line, then the static imports after another; a module import stands first |

#### Layout of the source: `verifyFormat`, `shipshape-rule-format`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `line-length` | text | A line is at most 140 columns wide, a tab counted as four |
| `space-indentation` | text | Code is indented with tabs; a space only aligns behind them |
| `blank-line` | shape | At most six statements stand together without a blank line, and a final `return` below more than two lines has one before it |
| `one-line-guard` | shape | An `if` without `else` that only returns, throws, continues or breaks stands on one line without braces, wherever that line fits |

#### What Lombok writes: `verifyLombok`, `shipshape-rule-lombok`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `hand-written-logger` | text | A logger is Lombok's `@Slf4j` |
| `empty-private-constructor` | text | The empty private constructor of a class of static members is `@NoArgsConstructor(access = AccessLevel.PRIVATE)` |
| `assigning-constructor` | shape | A constructor that only assigns its parameters is Lombok's |
| `returning-accessor` | shape | An accessor that only returns its field is Lombok's `@Getter`, but a `final` one of a class that can be extended, as Lombok writes no final method |
| `accessors-annotation` | text | No file carries `@Accessors` |
| `builder-place` | shape | `@Builder` stands on a class below `model/` or `config/`, on a member type, or in tests |

#### Models and records: `verifyModels`, `shipshape-rule-models`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `immutable-model` | shape | A class below `model/` has final fields, no setter, no `@Data` and no `@Setter` |
| `collection-copy` | shape | A class below `model/` copies the `List`, `Set` or `Map` it is handed, or lets the builder collect it (`@Singular`) |
| `top-level-record` | shape | No record is a top-level type |
| `nested-record` | shape | A member record is private, or a part of at most three components of its model |

#### Typed values: `verifyTypes`, `shipshape-rule-types`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `enum-word` | shape | Outside `type/` nothing makes an enum's word from `name()`, finds a constant by an upper-cased text or keeps a `…_PREFIX` |
| `string-choice` | shape | Main code chooses by no text: no `switch` on a string and no `equals("…")`, but in an enum and in a `…Mapper` of an implementation |
| `object-type` | shape | Main code declares no `Object` field, parameter or result; overrides are not read |
| `string-state` | shape | In an API and an implementation no `String` field or parameter is named a state, kind, type, status, mode, role, level or category; a `…Mapper` of an implementation is not read |

#### Use of the language: `verifyLanguage`, `shipshape-rule-language`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `reflection` | text | Main code calls no `Class.forName`, `Proxy.newProxyInstance`, `MethodHandles` lookup, any `getDeclared…`, `getConstructor(s)`, `getMethod`/`getField` with a name, or `setAccessible`; taking a `Method` a library hands over is no reflection |
| `stated-nullability` | shape | A public or protected method or constructor, and every method of an interface, states `@NotNull` or `@Nullable` on each reference, as does a field of a model and one Lombok writes a signature from; overrides, wiring methods and assemblies are not read |
| `vague-name` | text | No type of main code is named `…Util`, `…Utils`, `…Impl` or `…Helper` |
| `broad-catch` | text | `Exception`, `RuntimeException` and `Throwable` are caught only in a class declared with `catchesEverything` |

#### Packages: `verifyPackages`, `shipshape-rule-packages`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `package-size` | text | A package holds at most seven files directly |
| `role-folder` | shape | In an API an enum lies below `type/` and a class named `…Options` below `config/`; in every module an exception lies below `exception/` |
| `lone-package` | text | No package is made for one file, but `config`, `exception`, `model`, `type`, a module's own package (the one all its main packages lie in) and a package that groups others |
| `util-package` | shape | A package named `util` holds classes of static operations only, and no assembly has one |

#### Size: `verifySize`, `shipshape-rule-size`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `class-size` | shape | A top-level type of main code is at most 250 lines |
| `method-size` | shape | A method's body is at most 40 lines |
| `collaborators` | shape | A class outside `model/` and `config/` holds at most eight collaborators, and a wiring method takes at most eight |
| `parameters` | shape | A method or constructor takes at most five parameters; overrides and wiring methods are not read |

#### Control flow: `verifyFlow`, `shipshape-rule-flow`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `cognitive-complexity` | shape | A method's cognitive complexity, as SonarSource counts it, is at most 15 |
| `else-if` | shape | Main code writes no `else if` |
| `ladder` | shape | No three `if`s in a row without `else` test or do the same thing |

#### Tests: `verifyTests`, `shipshape-rule-tests`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `test-name` | text | A test is named `<Class>Test` or `<Class>IntegrationTest` after a class of its package and module, and the second name goes with `@Tag("integration")` |
| `module-test` | text | A module whose classes do something holds a test |
| `test-clock` | text | The code of tests declares no class extending `Clock`; a library of test fixtures holds the one clock |
| `fixed-sleep` | text | The code of tests never calls `Thread.sleep` |
| `mocked-own-type` | text | Mockito stands in for a foreign type only: none below `packageRoots` or of the test's package |

#### Documentation: `verifyDocumentation`, `shipshape-rule-documentation`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `type-javadoc` | text | A top-level type of main code has a Javadoc |
| `snippet-tag` | text | A Javadoc writes an example in `<pre>{@code …}</pre>`, never `{@snippet …}` |

#### Build files: `verifyBuildfile`, `shipshape-rule-buildfile`

| Rule | Reads | What it asks |
| --- | --- | --- |
| `dependency-order` | build files | The roles of a `dependencies` block stand as `api`, `implementation`, `compileOnly`, `embedded`, `runtimeOnly`, `annotationProcessor`, of main code, then test fixtures, then tests; in a role projects stand before catalog aliases, each sorted |
| `role-separation` | build files | An empty line stands between two roles of a `dependencies` block |
| `plugin-order` | build files | Plugins stand as built-ins, external ids, then conventions, each kind by the length of the name and then by the alphabet |

### Exemptions

A rule may name kinds of place a build can excuse from it. A declaration names one place and its reason, never a
whole rule, in the root build file; one that excuses nothing fails the build (`unused-exemption`). The built-in kind
is `catchesEverything` of `LanguageRules`, a class by its full name that is a boundary and catches whatever is
thrown (`broad-catch`):

```kotlin
import me.whereareiam.toolkit.shipshape.rule.language.LanguageRules

shipshape {
    exempt(LanguageRules.CATCHES_EVERYTHING, "com.example.delivery.DeliveryWorker", "A delivery that fails must not end the worker")
}
```

### Modules

| Module | Artifact | Holds |
| --- | --- | --- |
| `shipshape-api` | `me.whereareiam.toolkit:shipshape-api` | What a rule is and reads, the `shipshape` block, exemptions, the report and the measures: what another plugin builds on |
| `shipshape-common` | `me.whereareiam.toolkit:shipshape-common` | The engine: parsing the sources, the tasks, the registry of rules and exemptions; its test fixtures hold `LayoutFixture`, which lays out a build for a rule's tests |
| `shipshape-rule/rule-<subject>` | `me.whereareiam.toolkit:shipshape-rule-<subject>` | One subject's rules and the object that registers them, such as `ImportRules` |
| `shipshape` | `me.whereareiam.toolkit:shipshape` | The plugin, which wires the engine and registers every subject |

A plugin with rules of its own depends on `shipshape-api` alone; applying `me.whereareiam.toolkit.shipshape` brings
the rest.

### Registering rules

Another plugin adds its own rules and exemption kinds through the `shipshape` block (`ShipshapeExtension`). Rules
implement `ShapeRule` (reads one `ParsedSource`), `SourceRule` (reads all `JavaSources`) or `BuildRule` (reads one
`BuildFile`), all in `me.whereareiam.toolkit.shipshape.api`, each with an `id`, a `RuleCategory`, an `Enforcement`
(`FAILS` or `WARNS`) and the `ExemptionKind`s it reads. A rule joins a built-in category through its subject's
object, `ImportRules.CATEGORY` of `shipshape-rule-imports`, and then runs in `verifyImports`; or it names a category
of its own, which gets a task of its own. `RuleContext` tells a rule each module's role, the package roots and the
wiring annotations.

```kotlin
import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionPlace
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import org.gradle.api.Plugin
import org.gradle.api.Project

val OWNS_DIGEST = ExemptionKind("ownsDigest")
val DIGESTS = RuleCategory("digests", "Verifies who computes a digest.")

class DigestOwnerRule : SourceRule {
    override val id = "digest-owner"
    override val category = DIGESTS
    override val enforcement = Enforcement.FAILS
    override val exemptions = setOf(OWNS_DIGEST)

    override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filter { it.main }.flatMap { source ->
        source.find(Regex("""MessageDigest\s*\.\s*getInstance""")).map {
            Breach(
                source.path, it.line, "computes a digest; declare its owner: ${OWNS_DIGEST.declaration(source.qualifiedName)}",
                ExemptionPlace(OWNS_DIGEST, source.qualifiedName),
            )
        }
    }
}

class DigestRulesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("me.whereareiam.toolkit.shipshape")
        project.extensions.configure(ShipshapeExtension::class.java) {
            exemptionKind(OWNS_DIGEST)
            source(DigestOwnerRule())
        }
    }
}
```

The build then has `verifyDigests`, a part of `check`, and declares a place of the kind:

```kotlin
shipshape {
    exempt(OWNS_DIGEST, "com.example.account.VerificationCode", "The SHA-256 a confirmation code is kept as")
}
```

A rule's id is written in lower case with hyphens between its words; it is not called `name`, so that an enum can
be a set of rules. A second rule of an id shipshape already has is refused, as is a second category of one id that
says something else, a second kind of one method, a declaration of a kind nobody registered, a kind no rule reads,
and a kind the rules of two categories read. A `SourceRule` that reads a file beside the Java sources names it in
`reads`, so its category's task runs again when it changes. Tasks keep their rules in the configuration cache, so a rule
holds plain values and no `Project`.

A plugin whose own block hands declarations on passes the block's name, so a declaration that excuses nothing is
named where it was written: `exempt(OWNS_DIGEST, type, reason, block = "exemptions")`.

### Building a check of your own

A plugin's own task holds its findings the way shipshape's tasks do, with the same public parts of
`shipshape-api`: `ExemptionDeclarations` (`api.exemption`) keeps what a block declares, refusing a blank place or
reason and a place declared twice; `DeclaredExemptions` takes out of the found breaches what the declarations excuse
and makes an `unused-exemption` breach of each that excuses nothing; and `BreachReport` (`api.report`) writes the
report, prints what warns and fails for the rest. Its rules implement `Rule` with an id of their own.

```kotlin
val findings = DeclaredExemptions(declarations.of(kinds), "session-common/build.gradle.kts").excuse(found, kinds)
BreachReport(findings).conclude(name, report.get().asFile, logger, "The rules of the layout were violated")
```

`LineWidth` (`api.measure`) measures a line as `line-length` does, for files shipshape does not read, and
`ImportOrder` (`api.measure`) puts a file's imports in the order `import-order` asks, for a build that writes Java.

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
