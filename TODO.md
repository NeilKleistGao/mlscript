- [ ] Remove `State.noteImportedModule` hack;
  just make sure the State has the right origin in compiler context
- [x] rm `buildAmbientSymbolMapping`, `ambientSymbolMappingTo`, etc.
- [x] Avoid making Elaborator invent symbols for things like Runtime definitions. Add a pre-declaring mechanism?
