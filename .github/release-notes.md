---
version: 0.4.0
---

# Toolkit 0.4.0 - Shipshape

Shipshape is a new plugin that holds a build's Java sources and build scripts to the standard's general rules and
fails `check` where they break them, one task per category of rules. Project discovery can now leave out directories
and skips separate builds on its own.

## Highlights

* `me.whereareiam.toolkit.shipshape` checks imports, format, Lombok, flow, size, models, packages, language and build
  files, each category in a task of its own (`verifyImports`, `verifyFormat`, ...) that `check` runs, with exemptions
  declared in the `shipshape` block and a rule against exemptions no longer needed.
* Builds add rules of their own through `ShipshapeExtension.source()` in the same block.
* Static state without an owner and library versions written in build scripts are refused; a framework's forced field
  is declared with `forcedStatic`.
* Mixin and MixinExtras injector handlers keep the parameters their injector gives them, and the shapes of configuration
  files below `model/config` may stay mutable.
* Project discovery takes `exclude(...)` for directories to leave out and skips directories that hold a separate build.

## Upgrading

* Nothing changes for builds that do not apply Shipshape.
* A build that applies Shipshape for the first time should run `check` and either fix what it reports or declare an
  exemption with its reason; see the README.
