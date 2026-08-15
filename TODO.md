# Compilation-unit provenance cleanup

The cached-compilation-unit work needs a stable owner for symbols: optimized IR from one unit can
be copied into another, and those foreign symbols must still identify the JavaScript module that
defines them.  `Elaborator.State` already provides that stable identity, but it should not also be
an incrementally updated record of every compilation phase.

The cleanup will preserve the robust final-IR scan in `JSBuilder`: recovering dependencies after
all rewrites prevents an optimization from silently losing an import.  It will change how the
owner's provenance is produced and represented.

## Green milestones

- [x] Derive imported-symbol provenance once from the completed semantic block.
  - Remove `State.noteImportedModule` and the importer's `noteImport` wrapper.
  - Include compiler-added Runtime and Term imports in the same derivation.
  - Record only symbols owned by the unit's state; unaliased `.mls` imports deliberately retain
    the imported unit's ownership.
  - Assert that one owned symbol is never associated with two module paths.
- [x] Separate compilation-unit provenance from ordinary elaborator state.
  - Pass the source module path when constructing a compilation-unit state; worksheet, prelude,
    and analysis states remain explicitly ownerless.
  - Group the default export, effective configuration, and derived import map into one immutable
    provenance value that is published once before optimization.
  - Keep private JavaScript export names in a distinct, one-shot ABI value because they are only
    available after optimization.
  - Expose narrow lookup operations to `JSBuilder` and `BlockSimplifier`; UID allocation and lazy
    runtime-symbol services remain the responsibility of `Elaborator.State`.
- [ ] Run `ctest`, the focused compilation/import tests, and `hkmc2AllTests/test`; inspect and
  commit every intentional golden-output update.

## Longer-term option

Carrying dependency metadata directly through every IR-splicing transformation could eventually
remove the final symbol-provenance lookup.  That is intentionally out of scope here: forgetting to
propagate such metadata would risk silent miscompilation, whereas the final-IR scan centrally
observes the references that actually survive optimization.

- [x] rm `buildAmbientSymbolMapping`, `ambientSymbolMappingTo`, etc.
- [x] Avoid making Elaborator invent symbols for things like Runtime definitions. Add a pre-declaring mechanism?
