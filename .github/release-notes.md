---
version: 0.3.0
---

# Toolkit 0.3.0 - API Families

Architecture verification now checks that modules use only the APIs of their own family, of ancestors they share a
contract with, and of other families they declare they build on. A new distribution plugin collects a project's
release files under their final names.

## Highlights

* `me.whereareiam.toolkit.distribution` collects a project's release files under their final names in `build/distribution` with `assembleDistribution`.
* Architecture verification enforces API family boundaries, lets nested API owners share an ancestor's contract they declare (`sharedApis`), and lets a family build on the APIs of other families it declares (`dependsOnApis`).

## Upgrading

* `verifyArchitecture` now fails when a module depends on the API of another family. Declare an ancestor's contract in `sharedApis` and another family's API in `dependsOnApis`, or move the dependency into an assembly; see the README.
