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


## [ ] 1. Allow the inliner to inline module methods

Currently, the inliner only inlines top-level functions.
But since only module methods are available from other files, this means it never inlines across files.

There are some complications to inlining module methods.
The main one is explained in `hkmc2/shared/src/test/mlscript/opt/InlineModuleMethods.mls`
and has to do with reconstructing paths that may be rooted in non-top-level modules.

To address this, we may need to mark top-level module symbols (so we know if a path is truly rooted)
and make sure to re-root paths appropriately. I believe this is similar to what the Lifter already has to do,
so we should try to share that logic into reusable Scala helpers as much as possible.


## [ ] 2. Allow IR that refers to top-level symbols from other compilation units

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
export _$_modulePrivate_$_inaccessibleFunction;
```

Moreover, when compiling the importing file,
JSBuilder will need to make a pre-pass over the program being compiled to detect whether the program uses
any top-level symbols from other compilation units, and if so, allocate these symbols into the current scope
and insert corresponding imports, resulting in something like:

```js
import { _$_modulePrivate_$_inaccessibleFunction as allocatedNameForThisSymbol, ... /* other similar imports */ } from "./FilePrivateFunctions.mls";
```

The test for this is at `hkmc2/shared/src/test/mlscript/opt/InlineAcrossFiles.mls`.


