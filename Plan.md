# Inline Across Compilation Units

Please document your progress here, adding comments detailing you current progress,
any subtasks you have identified, the problems you encountered,
any questions you have for reviewers, and your important design choices.


## Introduction

We want to be able to inline functions across compilation units, which are essentially files compiled by the MLscript compiler.

We already have most of the pieces in place:
* The inliner at `hkmc2/shared/src/main/scala/hkmc2/codegen/BlockSimplifier.scala`.
* The `irDefn` value in `DefinitionSymbol`, which is inserted by IR passes and can be used by the inliner.
* We also now lower compilation units as part of `getElaboratedBlock` in `CompilerCtx`,
  meaning that the `irDefn` fields will be populated for imported files.

There are still some missing pieces, though.


## [x] 1. Allow the inliner to inline module methods

Currently, the inliner only inlines top-level functions.
But since only module methods are available from other files, this means it never inlines across files.

There are some complications to inlining module methods.
The main one is explained in `hkmc2/shared/src/test/mlscript/opt/InlineModuleMethods.mls`
and has to do with reconstructing paths that may be rooted in non-top-level modules.

To address this, we may need to mark top-level module symbols (so we know if a path is truly rooted)
and make sure to re-root paths appropriately. I believe this is similar to what the Lifter already has to do,
so we should try to share that logic into reusable Scala helpers as much as possible.

### Progress

Implemented in `BlockSimplifier`.

Only methods owned by true `module` symbols are eligible. Instance methods and `object` methods stay
uninlined because moving them could change the meaning of instance state and initialization checks.
When a module method is inlined, the copier reconstructs the chain of module `this` references from
the call-site qualifier so nested module paths remain correctly rooted.

Cross-unit inlining is also blocked when the caller and callee disagree on `noFreeze`, because moving
an `Instantiate` node across that boundary would change whether generated values are frozen. Bodies
that refer to `import.meta.url` are likewise kept in their defining module because moving them would
change the reported file URL.


## [x] 2. Allow IR that refers to top-level symbols from other compilation units

Currently, a file like:

```
// FilePrivateFunctions.mls
fun inaccessibleFunction() = 42
module FilePrivateFunctions with
  fun accessibleFunction() =
    inaccessibleFunction() + 1
```

essentially compiles to (assuming `inaccessibleFunction` is not inlined)

```js
let Example1, inaccessibleFunction;
inaccessibleFunction = function inaccessibleFunction() {
  return 42
};
(class FilePrivateFunctions {
  static {
    FilePrivateFunctions1 = this
  }
  static accessibleFunction1() {
    let tmp;
    tmp = inaccessibleFunction();
    return tmp + 1
  }
});
let FilePrivateFunctions = FilePrivateFunctions1; export default FilePrivateFunctions;
```

The problem is that `inaccessibleFunction` is not importable from the outside.
If we want to allow inlining `accessibleFunction` into another file,
we need to change this.

The scheme I propose is to export top-level functions like `inaccessibleFunction` under a mangled name, such as:

```js
export { inaccessibleFunction as _$_modulePrivate_$_inaccessibleFunction };
```

Moreover, when compiling the importing file,
JSBuilder will need to make a pre-pass over the program being compiled to detect whether the program uses
any top-level symbols from other compilation units, and if so, allocate these symbols into the current scope
and insert corresponding imports, resulting in something like:

```js
import { _$_modulePrivate_$_inaccessibleFunction as allocatedNameForThisSymbol, ... /* other similar imports */ } from "./FilePrivateFunctions.mls";
```

The test for this is at `hkmc2/shared/src/test/mlscript/opt/InlineAcrossFiles.mls`.

### Progress

Implemented in `JSBuilder`, with provenance recorded on symbols during import resolution and lowering.

Compiled modules now export referenced file-private top-level symbols under fresh
`_$_modulePrivate_$_...` names. The defining compilation unit allocates these names through the normal
JavaScript `Scope`, which escapes them and resolves collisions without exposing unstable symbol UIDs;
importers reuse the resulting immutable name table from the defining state. Before rendering a module
or worksheet, `JSBuilder` traverses the IR,
detects external compilation-unit symbols exposed by inlining, allocates their names, and synthesizes
the corresponding named imports. The same pre-pass recreates default imports when an inlined body
introduces references such as `runtime`, `Term`, or a source-level imported module. External
dependencies are ordered first by normalized module path and only then by their state-local symbol
UID and name, so symbols from different elaboration states cannot make import or alias order depend
on set/map iteration.

Static module compilation preserves lowered top-level symbols as a private ABI. This matters for
helpers generated by passes such as `TailRecOpt`: a separately lowered importing worksheet can still
refer to one even if the defining module's own optimization pass would otherwise inline it away.

Cross-unit inlining remains conservative for JS-private module members. Their generated accessor
symbols are shared only inside one emitted module, so a body that touches such a member stays in its
defining compilation unit rather than publishing a broader private-member ABI.

Imported compilation units are lowered in worksheet mode so their symbols have `irDefn` values for the
inliner to inspect. Their effective root configuration is stabilized in `CompilerCtx`, rather than
depending on whichever parallel test happens to import the artifact first.

Cached IR used to be able to contain builtin symbols from the first importing worksheet while later
worksheets had distinct Prelude symbols, so the inliner seeded `SymbolRefresher` with a mapping that
rebound ambient compiler symbols and Prelude builtins onto the importer's equivalents. That mapping is
gone: every compilation unit now genuinely shares one Prelude and one `Runtime.mls` elaboration (see
the section on importer-independent caching below), so a cross-unit body already refers to the symbols
the importing file knows. Copying such a body needs no rebinding beyond the ordinary refreshing of the
definitions it introduces.

During regression testing, first-class function adapters exposed one additional issue: generated
`call` methods copied a rest parameter without forwarding it. They now eagerly spread the rest argument
when invoking the wrapped function.


## Integrating with the `CompilationPipeline` refactoring

Upstream moved the post-lowering passes out of `Lowering.program` into `codegen.CompilationPipeline`.
`CompilerCtx.getElaboratedBlock` now runs that pipeline itself, for both statically compiled modules
and worksheet-mode imports: the `irDefn` fields the inliner reads are owned by the *latest* rewrite of
each definition, so only fully-transformed definitions are valid to splice into another unit.

Two properties of this work were previously implicit in the pass ordering and are now explicit:

* The compilation unit's private ABI is collected through `CompilationPipeline.extraSymbolsToPreserve`,
  which sees the program as it enters the optimization passes — after the mandatory lowering passes and
  the first tail-call optimization, once the definitions making up the unit are settled. Collecting it
  from the freshly lowered program instead over-preserves: forwarders such as the ones `TailRecOpt`
  leaves behind then survive into the emitted module, which costs a stack frame per iteration and
  overflows deeply recursive programs.
* The automatic-inlining fuel is a budget per *file*, not per pass. Since the pipeline applies the
  simplifier twice, `CompilationPipeline` instantiates a single `BlockSimplifier` and applies it twice
  so both passes draw from one budget; `BlockSimplifier` grants the budget on first application only.

Upstream's new dead-assignment removal marks an assignment live when a data-flow fact reaching it is
read. The branch's `Match` handling merges branch facts more precisely than upstream's and sometimes
gives up and drops them; where it does, it now explicitly marks the dropped facts live, since a read
after the match can no longer reach them and the assignments would otherwise look dead.


## Caching compilation units is only sound if their identity is importer-independent

Sharing symbols across compilation units means a cached artifact *is* the identity of its definitions.
That only works if every requester agrees on which artifact a file has, so the cache must not be keyed
on anything belonging to the requester. Two ways it was, both of which produced silently inconsistent
symbols rather than a visible failure:

* `getElaboratedBlock` and `getPrelude` derived a unit's effective configuration from the *importing*
  file's config, whose `baseDir` is that file's own directory. Importers from different directories
  therefore invalidated each other's artifacts constantly (~2800 re-elaborations over one diff-test
  run, against 33 files). A worksheet could then capture `State.tupleSymbol` from one elaboration of
  `Runtime.mls` and reach a different elaboration through its own `import` statement — the two print
  as `Tuple⁰` and `Tuple¹`, and which one appeared depended on what else was running.
* `CompilerCache.upsert` was not atomic, so concurrent requesters each elaborated the file and each
  kept a *different* set of symbols, only one of which stayed in the cache.

Both now elaborate under `CompilerCtx.rootConfig`, which is required when the context is created and
cannot be changed afterwards, and `upsert` is synchronized like `upsertPrelude`. Whoever owns a context
therefore also fixes the configuration of everything cached in it; the test harnesses that share one
context across many files set it to the same base directory the compile tests use, so an imported unit
lowers exactly as those tests build the corresponding `.mjs`, and source locations baked into inlined
bodies agree with the ones in the separately compiled module. `MLsCompiler` reads its configuration
from the context rather than taking its own, so the two cannot disagree.

The compiler paths are fixed on the context for the same reason, since they take part in lowering —
the lowered program imports the runtime by path. That makes every compilation unit lowered, so
`Artifact.ir` is no longer optional and the `loweredPaths` it was compared against is gone: an
importer can never be handed a unit without IR, which it could only have supplied by elaborating that
unit a second time, under symbols nobody else shares. `MLsCompiler` takes both the paths and the
configuration from the context rather than its own parameters, so nothing can disagree with what the
context has already cached.

Since the root configuration cannot change during a compilation session, a cached artifact was
necessarily built under the one being asked for. Rather than comparing configurations and silently
re-elaborating on a mismatch — which made every field of `Config` an implicit part of a compilation
unit's identity — the caches now `softAssert` that they agree and keep the cached artifact. Reusing a
single identity is the lesser evil if it ever fails, and the diagnostic makes the misuse (two contexts
with different root configurations sharing one cache) visible rather than silent.

The same "one spelling per file" requirement applies to the cache keys. On the JVM it comes free from
`os.Path`, which is normalized by construction; the JS `VirtualPath` only normalized paths built with
`/`, so a path built from a string — which is how the web entry point receives one — could denote the
same file under a second key. That would both split its elaboration and defeat `InMemoryFileSystem`,
which documents that it assumes normalized paths. `VirtualPath` now normalizes on construction and
compares by the normalized form.


## Review follow-up

The final review found three more cache/inlining edge cases and one test-hygiene issue:

* Absolute virtual paths now clamp `..` at the root, and the JavaScript-facing string APIs of
  `InMemoryFileSystem` normalize their keys too. Otherwise `/../a`, `/a`, and a host-written
  `/x/../a` could still denote the same file under different cache/file-system keys.
* Replacing a cached compilation-unit artifact or prelude now invalidates every dependent artifact.
  Cached terms and IR retain the old symbols, so updating only the changed file could leave an
  unchanged importer referring to definitions from the previous artifact.
  Cache lookup checks all published source timestamps so requesting the importer itself also notices
  a changed dependency. A Scala.js regression test changes a dependency in a way that shifts a private
  helper's UID and verifies both that its scope-allocated ABI name remains stable and that the importer
  is rebuilt with the changed inlined body.
* Automatic-inlining fuel is spent only after all argument lists have matched. A call that could not
  be inlined previously consumed budget and could prevent an unrelated later call from being inlined.
* Temporary experiments were removed from `HkScratch.mls`, as required by the diff-test workflow.
