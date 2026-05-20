package hkmc2

import mlscript.utils.*, shorthands.*
import semantics.Import
import collection.mutable.Map as MutMap
import semantics.Elaborator.{Ctx, Mode, State}
import hkmc2.Message.MessageContext
import hkmc2.io
import utils.TL
import syntax.LetBind

class WebImporter(tl0: TL, wd0: io.Path, prelude0: Ctx)
    (using raise: Raise, state: State, cctx: CompilerCtx, config: Config)
  extends semantics.Elaborator(tl0, wd0, prelude0):
  import WebImporter.embeddedSourceMap
  
  override def importPath(path: Str, alias: Opt[syntax.Tree.Ident])(using cfg: Config): Import =
    import syntax.*, semantics.*
    
    val file =
      if path.startsWith("/")
      then io.Path(path)
      else wd / io.RelPath(path)
    val nme = file.baseName
    val id = new syntax.Tree.Ident(nme) // TODO loc
    
    lazy val sym = TermSymbol(LetBind, N, id)
    
    if path.startsWith(".") || path.startsWith("/") then // leave alone imports like "fs"
      file.ext match
      
      case "mjs" | "js" =>
        Import(sym, file.toString, file)
        
      case "mls" =>
        embeddedSourceMap.get(file.last) match
          case Some(block -> optMjsFile) =>
            val fph = new FastParseHelpers(block)
            val origin = Origin(file, 0, fph)
            
            val sym = tl.trace(s">>> Importing $file"):
              
              // TODO add parser option to omit internal impls
              
              val lexer = new syntax.Lexer(origin, dbg = tl.doTrace)
              val tokens = lexer.bracketedTokens
              val rules = syntax.ParseRules()
              val p = new syntax.Parser(origin, tokens, rules, raise, dbg = tl.doTrace):
                def doPrintDbg(msg: => Str): Unit =
                  // if dbg then output(msg)
                  if dbg then tl.log(msg)
              val res = p.parseAll(p.block(allowNewlines = true))
              val resBlk = new syntax.Tree.Block(res)
              
              given Elaborator.Ctx = prelude.copy(mode = Mode.Light).nestLocal("prelude")
              given CompilerCtx = cctx.derive(file)
              val elab = new WebImporter(tl, file.up, prelude)
              elab.importFrom(resBlk)
              
              resBlk.definedSymbols.find(_._1 === nme) match
              case Some(nme -> sym) => sym
              case None => lastWords(s"File $file does not define a symbol named $nme")
            
            val jsFile = file.up / io.RelPath(file.baseName + ".mjs")
            val jsStr = optMjsFile.fold(jsFile.toString)(_ => jsFile.toString)
            Import(sym, jsStr, jsFile)
          case None =>
            raise(ErrorReport(msg"Source file ${file.toString} not found" -> N :: Nil))
            Import(sym, path, file)
        
      case _ =>
        raise(ErrorReport(msg"Unsupported file extension: ${file.ext}" -> N :: Nil))
        Import(sym, path, file)
      
    else
      Import(sym, path, file)

object WebImporter:
  val preludeSource: Str =
    """

declare type Any
declare type Anything
declare type Nothing

declare type untyped

declare type tailrec
declare type tailcall

declare class Object
declare module Object with
  fun
    create
    freeze
    assign
    entries
    prototype
    fromEntries
    getPrototypeOf
    defineProperty
    getOwnPropertyDescriptor
    getOwnPropertyDescriptors
declare class JSON
declare module JSON with
  fun
    stringify
declare class Number
declare module Number with
  val
    MIN_VALUE
    MAX_VALUE
    MIN_SAFE_INTEGER
    MAX_SAFE_INTEGER
    NEGATIVE_INFINITY
    POSITIVE_INFINITY
declare class BigInt
declare module BigInt
declare class Function
declare module Function
declare class String
declare module String with
  fun
    fromCharCode
    fromCodePoint
    raw
declare class RegExp
declare module RegExp
declare class Set[V]
declare module Set
declare class Map[K, V]
declare module Map
declare class WeakSet[V]
declare module WeakSet
declare class WeakMap[K, V]
declare module WeakMap
declare class Error//(info) // TODO: handle JS classes that can be instantiated without `new` specially in codegen.
declare class TypeError//(info) // TODO: handle JS classes that can be instantiated without `new` specially in codegen.
declare class RangeError//(info) // TODO: handle JS classes that can be instantiated without `new` specially in codegen.
declare class Date
declare module Date

declare class ArrayBuffer
declare module ArrayBuffer
declare class TypedArray
declare module TypedArray
declare class Int8Array
declare module Int8Array
declare class Uint8Array
declare module Uint8Array
declare class Uint8ClampedArray
declare module Uint8ClampedArray
declare class Int16Array
declare module Int16Array
declare class Uint16Array
declare module Uint16Array
declare class Int32Array
declare module Int32Array
declare class Uint32Array
declare module Uint32Array
declare class Float16Array
declare module Float16Array
declare class Float32Array
declare module Float32Array
declare class Float64Array
declare module Float64Array
declare class BigInt64Array
declare module BigInt64Array
declare class BigUint64Array
declare module BigUint64Array

// MLscript-specific types
declare class Bool
declare class Int
declare class Num
declare class Str

// The `Array` class/function is a footgun:
//    > Array()
//    []
//    > Array(1)
//    [ <1 empty item> ]
//    > Array(1, 2)
//    [ 1, 2 ]
// We used to declare it as a class taking exactly one argument here to avoid that footgun:
//    declare class Array[T](val length: Int): Array[T]
// but this made instance checks wrongly use `Array.class`;
// TODO: handle Array and other JS classes that can be instantiated without `new` specially in codegen.
declare class Array[T]
declare module Array with
  fun
    from
    concat
    isArray
    prototype

declare object Symbol with
  // The `TermDef` needs `rhs` to be defined to be recognized as `isMLsFun`.
  // Otherwise, it would be wrapped in `runtime.safeCall`, which accesses the
  // uninitialized `runtime` in the `Rendering` module.
  fun for: Str -> Any = ()
  val iterator: Any

// MLwasm-specific types
declare class Int31

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math
declare module Math with
  declare
    val
      E: Num
      LN10: Num
      LN2: Num
      LOG10E: Num
      LOG2E: Num
      PI: Num
      SQRT1_2: Num
      SQRT2: Num
    fun
      abs: Num -> Num
      acos: Num -> Num
      acosh: Num -> Num
      asin: Num -> Num
      asinh: Num -> Num
      atan: Num -> Num
      atan2: (Num, Num) -> Num
      atanh: Num -> Num
      cbrt: Num -> Num
      ceil: Num -> Num
      clz32: Num -> Int
      cos: Num -> Num
      cosh: Num -> Num
      exp: Num -> Num
      expm1: Num -> Num
      floor: Num -> Num
      f16round: Num -> Num
      fround: Num -> Num
      hypot(...values): Num
      imul: (Num, Num) -> Int
      log: Num -> Num
      log10: Num -> Num
      log1p: Num -> Num
      log2: Num -> Num
      max(...values): Num
      min(...values): Num
      pow: (Num, Num) -> Num
      random: () -> Num
      round: Num -> Num
      sign: Num -> (-1 | 0 | 1)
      sin: Num -> Num
      sinh: Num -> Num
      sqrt: Num -> Num
      tan: Num -> Num
      tanh: Num -> Num
      trunc: Num -> Num

declare module Reflect with
  fun
    get
    // set // TODO: handle keyword-named members
    has
    ownKeys
    getPrototypeOf
    apply
    construct
declare module console with
  declare
    fun
      log
      debug
      info
      warn
      error
      // assert
      clear
      count
      countReset
      dir
      dirxml
      group
      groupCollapsed
      groupEnd
      table
      time
      timeEnd
      trace

declare val process // TODO make `module`
declare val fs // TODO make `module`

declare val Infinity

// Wasm support
declare class Promise
declare val Promise
declare object WebAssembly with
  declare class Memory
  declare object Module with
    fun
      exports
      imports

  fun
    instantiate
    validate

// declare fun typeof: (Any) -> Str

declare fun parseInt(str: Str, radix: Int): Int
declare fun parseFloat(str: Str): Num


declare module source with
  object
    line
    name
    file

declare module js with
  fun
    bitand
    bitnot
    bitor
    shl
    try_catch

declare module wasm with
  fun
    plus_impl
    minus_impl
    times_impl
    div_impl
    mod_impl
    eq_impl
    neq_impl
    lt_impl
    le_impl
    gt_impl
    ge_impl
    neg_impl
    pos_impl
    not_impl

declare module debug with
  fun printStack

declare module annotations with
  object compile
  object buffered
  object bufferable

declare module scope with
  fun locally


// HTML DOM API definitions.
// Move them to a separate Prelude file when there are enough of them.
declare val document
declare val customElements
declare class HTMLElement
"""

  val fileNameSourceMap: MutMap[Str, (Str, Opt[Str])] = MutMap.empty
  def embeddedSourceMap: MutMap[Str, (Str, Opt[Str])] = fileNameSourceMap
  lazy val embeddedFiles: Map[String, String] =
    fileNameSourceMap.iterator.foldLeft(Map.empty[String, String]):
      case (acc, (name, (source, optMjs))) =>
        val base = io.Path("/") / name
        val withSource = acc + (base.toString -> source)
        optMjs match
          case S(jsSource) =>
            withSource + ((io.Path("/") / s"${base.baseName}.mjs").toString -> jsSource)
          case N => withSource

  fileNameSourceMap += "Prelude.mls" -> (preludeSource -> N)
  
  fileNameSourceMap += "Rendering.mls" -> ("""
module Rendering with ...


fun pass1(f)(...xs) = f(xs.0)
fun pass2(f)(...xs) = f(xs.0, xs.1)
fun pass3(f)(...xs) = f(xs.0, xs.1, xs.2)

fun passing(f, ...args) = f.bind(null, ...args)

fun map(f)(...xs) = xs.map(pass1(f))

fun fold(f)(init, ...rest) =
  let
    i = 0
    len = rest.length
  while i < len do
    set
      init = f(init, rest.at(i))
      i += 1
  init

fun interleave(sep)(...args) =
  if args.length === 0 then [] else...
  let
    res = Array of args.length * 2 - 1
    len = args.length
    i = 0
  while i < len do
    let idx = i * 2
    set
      res.[idx] = args.[i]
      i += 1
    if i < len do set res.[idx + 1] = sep
  res

fun render(arg) = if
  arg is
    undefined then "undefined"
    null      then "null"
    Array     then fold(+)("[", ...interleave(", ")(...map(render)(...arg)), "]")
    Str       then JSON.stringify(arg)
    Set       then fold(+)("Set{", ...interleave(", ")(...map(render)(...arg)), "}")
    Map       then fold(+)("Map{", ...interleave(", ")(...map(render)(...arg)), "}")
    Function  and
      let p = Object.getOwnPropertyDescriptor(arg, "prototype")
      (p is Object and p.("writable")) || (p is undefined) then
        "[function" + (if arg.name is
          ""  then ""
          nme then " " + nme
        ) + "]"
    Object then
      if arg.constructor.name is "Object"
      then
        let es = Object.entries(arg)
        fold(+)("{", ...interleave(", ")(...map(case [k, v] then k + ": " + render(v))(...es)), "}")
      else String(arg)
  let ts = arg.("toString") // not accessing as `arg.toString` to avoid the sanity check
  ts is undefined then "[" + typeof(arg) + "]"
  else ts.call(arg)
""" -> S("""
import runtime from "./Runtime.mjs";
let Rendering1;
(class Rendering {
  static {
    Rendering1 = Rendering;
  }
  static pass1(f) {
    return (...xs) => {
      return runtime.safeCall(f(xs[0]))
    }
  } 
  static pass2(f1) {
    return (...xs) => {
      return runtime.safeCall(f1(xs[0], xs[1]))
    }
  } 
  static pass3(f2) {
    return (...xs) => {
      return runtime.safeCall(f2(xs[0], xs[1], xs[2]))
    }
  } 
  static passing(f3, ...args) {
    return f3.bind(null, ...args)
  } 
  static map(f4) {
    return (...xs) => {
      let tmp;
      tmp = Rendering.pass1(f4);
      return runtime.safeCall(xs.map(tmp))
    }
  } 
  static fold(f5) {
    return (init, ...rest) => {
      let i, len, scrut, tmp, tmp1, tmp2, tmp3;
      i = 0;
      len = rest.length;
      tmp4: while (true) {
        scrut = i < len;
        if (scrut === true) {
          tmp = runtime.safeCall(rest.at(i));
          tmp1 = runtime.safeCall(f5(init, tmp));
          init = tmp1;
          tmp2 = i + 1;
          i = tmp2;
          tmp3 = runtime.Unit;
          continue tmp4;
        } else {
          tmp3 = runtime.Unit;
        }
        break;
      }
      return init
    }
  } 
  static interleave(sep) {
    return (...args1) => {
      let res, len, i, scrut, idx, scrut1, scrut2, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
      scrut2 = args1.length === 0;
      if (scrut2 === true) {
        return []
      } else {
        tmp = args1.length * 2;
        tmp1 = tmp - 1;
        tmp2 = globalThis.Array(tmp1);
        res = tmp2;
        len = args1.length;
        i = 0;
        tmp8: while (true) {
          scrut = i < len;
          if (scrut === true) {
            tmp3 = i * 2;
            idx = tmp3;
            res[idx] = args1[i];
            tmp4 = i + 1;
            i = tmp4;
            scrut1 = i < len;
            if (scrut1 === true) {
              tmp5 = idx + 1;
              res[tmp5] = sep;
              tmp6 = runtime.Unit;
            } else {
              tmp6 = runtime.Unit;
            }
            tmp7 = tmp6;
            continue tmp8;
          } else {
            tmp7 = runtime.Unit;
          }
          break;
        }
        return res
      }
    }
  } 
  static render(arg) {
    let ts, scrut, es, p, scrut1, scrut2, scrut3, nme, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13, tmp14, tmp15, tmp16, tmp17, tmp18, tmp19, tmp20, tmp21, tmp22, tmp23, tmp24, tmp25, tmp26, tmp27, tmp28, tmp29, tmp30, tmp31, tmp32, tmp33, tmp34, tmp35, tmp36, lambda, lambda1, lambda2, lambda3, lambda4, lambda5, lambda6;
    if (arg === undefined) {
      return "undefined"
    } else if (arg === null) {
      return "null"
    } else if (arg instanceof globalThis.Array) {
      lambda = (undefined, function (arg1, arg2) {
        return arg1 + arg2
      });
      tmp = Rendering.fold(lambda);
      tmp1 = Rendering.interleave(", ");
      tmp2 = Rendering.map(Rendering.render);
      tmp3 = runtime.safeCall(tmp2(...arg));
      tmp4 = runtime.safeCall(tmp1(...tmp3));
      return runtime.safeCall(tmp("[", ...tmp4, "]"))
    } else if (typeof arg === 'string') {
      return runtime.safeCall(globalThis.JSON.stringify(arg))
    } else if (arg instanceof globalThis.Set) {
      lambda1 = (undefined, function (arg1, arg2) {
        return arg1 + arg2
      });
      tmp5 = Rendering.fold(lambda1);
      tmp6 = Rendering.interleave(", ");
      tmp7 = Rendering.map(Rendering.render);
      tmp8 = runtime.safeCall(tmp7(...arg));
      tmp9 = runtime.safeCall(tmp6(...tmp8));
      return runtime.safeCall(tmp5("Set{", ...tmp9, "}"))
    } else if (arg instanceof globalThis.Map) {
      lambda2 = (undefined, function (arg1, arg2) {
        return arg1 + arg2
      });
      tmp10 = Rendering.fold(lambda2);
      tmp11 = Rendering.interleave(", ");
      tmp12 = Rendering.map(Rendering.render);
      tmp13 = runtime.safeCall(tmp12(...arg));
      tmp14 = runtime.safeCall(tmp11(...tmp13));
      return runtime.safeCall(tmp10("Map{", ...tmp14, "}"))
    } else if (arg instanceof globalThis.Function) {
      p = globalThis.Object.getOwnPropertyDescriptor(arg, "prototype");
      if (p instanceof globalThis.Object) {
        scrut1 = p["writable"];
        if (scrut1 === true) {
          tmp15 = true;
        } else {
          tmp15 = false;
        }
      } else {
        tmp15 = false;
      }
      if (p === undefined) {
        tmp16 = true;
      } else {
        tmp16 = false;
      }
      scrut2 = tmp15 || tmp16;
      if (scrut2 === true) {
        scrut3 = arg.name;
        if (scrut3 === "") {
          tmp17 = "";
        } else {
          nme = scrut3;
          tmp17 = " " + nme;
        }
        tmp18 = "[function" + tmp17;
        return tmp18 + "]"
      } else {
        if (arg instanceof globalThis.Object) {
          scrut = arg.constructor.name;
          if (scrut === "Object") {
            tmp19 = runtime.safeCall(globalThis.Object.entries(arg));
            es = tmp19;
            lambda3 = (undefined, function (arg1, arg2) {
              return arg1 + arg2
            });
            tmp20 = Rendering.fold(lambda3);
            tmp21 = Rendering.interleave(", ");
            lambda4 = (undefined, function (caseScrut) {
              let first1, first0, k, v, tmp37, tmp38;
              if (globalThis.Array.isArray(caseScrut) && caseScrut.length === 2) {
                first0 = caseScrut[0];
                first1 = caseScrut[1];
                k = first0;
                v = first1;
                tmp37 = k + ": ";
                tmp38 = Rendering.render(v);
                return tmp37 + tmp38
              } else {
                throw new globalThis.Error("match error");
              }
            });
            tmp22 = lambda4;
            tmp23 = Rendering.map(tmp22);
            tmp24 = runtime.safeCall(tmp23(...es));
            tmp25 = runtime.safeCall(tmp21(...tmp24));
            return runtime.safeCall(tmp20("{", ...tmp25, "}"))
          } else {
            return globalThis.String(arg)
          }
        } else {
          ts = arg["toString"];
          if (ts === undefined) {
            tmp26 = typeof arg;
            tmp27 = "[" + tmp26;
            return tmp27 + "]"
          } else {
            return runtime.safeCall(ts.call(arg))
          }
        }
      }
    } else if (arg instanceof globalThis.Object) {
      scrut = arg.constructor.name;
      if (scrut === "Object") {
        tmp28 = runtime.safeCall(globalThis.Object.entries(arg));
        es = tmp28;
        lambda5 = (undefined, function (arg1, arg2) {
          return arg1 + arg2
        });
        tmp29 = Rendering.fold(lambda5);
        tmp30 = Rendering.interleave(", ");
        lambda6 = (undefined, function (caseScrut) {
          let first1, first0, k, v, tmp37, tmp38;
          if (globalThis.Array.isArray(caseScrut) && caseScrut.length === 2) {
            first0 = caseScrut[0];
            first1 = caseScrut[1];
            k = first0;
            v = first1;
            tmp37 = k + ": ";
            tmp38 = Rendering.render(v);
            return tmp37 + tmp38
          } else {
            throw new globalThis.Error("match error");
          }
        });
        tmp31 = lambda6;
        tmp32 = Rendering.map(tmp31);
        tmp33 = runtime.safeCall(tmp32(...es));
        tmp34 = runtime.safeCall(tmp30(...tmp33));
        return runtime.safeCall(tmp29("{", ...tmp34, "}"))
      } else {
        return globalThis.String(arg)
      }
    } else {
      ts = arg["toString"];
      if (ts === undefined) {
        tmp35 = typeof arg;
        tmp36 = "[" + tmp35;
        return tmp36 + "]"
      } else {
        return runtime.safeCall(ts.call(arg))
      }
    }
  }
  static toString() { return "Rendering"; }
});
let Rendering = Rendering1; export default Rendering;
"""))
  
  fileNameSourceMap += "Runtime.mls" -> ("""
import "./RuntimeJS.mjs"
import "./Rendering.mls"


module Runtime with ...


object Unit with
  fun toString() = "()"


fun unreachable = throw Error("unreachable")

fun checkArgs(functionName, expected, isUB, got) =
  if got < expected || isUB && got > expected do
    let name = if functionName.length > 0 then " '" + functionName + "'" else ""
    // throw globalThis.Error("Function" + name + " expected "
    //   + (if isUB then "" else "at least ")
    //   + expected
    //   + " argument(s) but got " + got)
    throw Error of "Function" + name + " expected "
      + (if isUB then "" else "at least " )
      + expected + " argument"
      + (if expected === 1 then "" else "s")
      + " but got " + got

fun safeCall(x) =
  if x is undefined then Unit else x

fun checkCall(x) =
  if x is undefined
  then throw Error("MLscript call unexpectedly returned `undefined`, the forbidden value.")
  else x

fun deboundMethod(mtdName, clsName) =
  throw Error of
    "[debinding error] Method '" + mtdName + "' of class '" + clsName + "' was accessed without being called."


val try_catch = RuntimeJS.try_catch

class EffectHandle(_reified) with
  val reified = _reified
  fun resumeWith(value) =
    Runtime.try(() => resume(reified.contTrace)(value))
  fun raise() =
    topLevelEffect(reified, false)

fun try(f) =
  let res = f()
  if res is EffectSig then EffectHandle(res) else res


// For `pattern` definitions
data class MatchResult(captures)
data class MatchFailure(errors)

// For pattern matching on tuples
module Tuple with
  fun slice(xs, i, j) =
    // * This is more robust than `xs.slice(i, xs.length - j)`
    // * as it is not affected by users redefining `slice`
    globalThis.Array.prototype.slice.call(xs, i, xs.length - j)

  fun get(xs, i) =
    // * Contrary to `xs.[i]`, this supports negative indices (Python-style)
    if i >= xs.length then
      throw RangeError("Tuple.get: index out of bounds")
    else globalThis.Array.prototype.at.call(xs, i)

module Str with
  fun startsWith(string, prefix) = string.startsWith(prefix)

  fun get(string, i) =
    if i >= string.length then
      throw RangeError("Str.get: index out of bounds")
    else string.at(i)

  fun drop(string, n) = string.slice(n)

// Re-export rendering functions
val render = Rendering.render

fun printRaw(x) = console.log(render(x))

// TraceLogger

module TraceLogger with
  mut val enabled = false
  mut val indentLvl = 0
  fun indent() =
    if enabled then
      let prev = indentLvl
      set indentLvl = prev + 1
      prev
    else ()
  fun resetIndent(n) =
    if enabled then
      set indentLvl = n
    else ()
  fun log(msg) =
    if enabled then
      console.log("| ".repeat(indentLvl) + msg.replaceAll("\n", "\n" + "  ".repeat(indentLvl)))
    else ()

// Private definitions for algebraic effects

object FatalEffect
object PrintStackEffect

data abstract class FunctionContFrame(next) with
  fun resume(value)
data class HandlerContFrame(next, nextHandler, handler)

data class ContTrace(next, last, nextHandler, lastHandler, resumed)
data class EffectSig(contTrace, handler, handlerFun)

class NonLocalReturn with
  fun ret(value)

data class FnLocalsInfo(fnName, locals)
data class LocalVarInfo(localName, value)


fun raisePrintStackEffect(showLocals) =
  mkEffect(PrintStackEffect, showLocals)

fun topLevelEffect(tr, debug) =
  while tr.handler === PrintStackEffect do
    console.log(showStackTrace("Stack Trace:", tr, debug, tr.handlerFun))
    set tr = resume(tr.contTrace)(())
  if tr is EffectSig then
    throw showStackTrace("Error: Unhandled effect " + tr.handler.constructor.name, tr, debug, false)
  else
    tr

fun showStackTrace(header, tr, debug, showLocals) =
  let
    msg = header
    curHandler = tr.contTrace
    atTail = true
  if debug do
    while curHandler !== null do
      let cur = curHandler.next
      while cur !== null do
        let locals = cur.getLocals
        let curLocals = locals.at(locals.length - 1)
        let loc = cur.getLoc
        let loc = if loc is null then "pc=" + cur.pc else loc
        let localsMsg = if showLocals and curLocals.locals.length > 0 then
          " with locals: " + curLocals.locals.map(l => l.localName + "=" + Rendering.render(l.value)).join(", ")
        else
          ""
        set
          msg += "\n\tat " + curLocals.fnName + " (" + loc + ")"
          msg += localsMsg
          cur = cur.next
          atTail = false
      set curHandler = curHandler.nextHandler
      if curHandler !== null do
        set
          msg += "\n\twith handler " + curHandler.handler.constructor.name
          atTail = false
    if atTail do
      set msg += "\n\tat tail position"
  msg

fun showFunctionContChain(cont, hl, vis, reps) =
  if cont is FunctionContFrame then
    let result = cont.constructor.name + "(pc=" + cont.pc
    hl.forEach((m, marker) => if m.has(cont) do set result += ", " + marker)
    if vis.has(cont) then
      set reps = reps + 1
      if reps > 10 do
        throw Error("10 repeated continuation frame (loop?)")
      set result += ", REPEAT"
    else
      vis.add(cont)
    result + ") -> " + showFunctionContChain(cont.next, hl, vis, reps)
  else if cont === null then
    "(null)"
  else
    "(NOT CONT)"

fun showHandlerContChain(cont, hl, vis, reps) =
  if cont is HandlerContFrame then
    let result = cont.handler.constructor.name
    hl.forEach((m, marker) => if m.has(cont) do set result += ", " + marker)
    if vis.has(cont) then
      set reps = reps + 1
      if reps > 10 do
        throw Error("10 repeated continuation frame (loop?)")
      set result += ", REPEAT"
    else
      vis.add(cont)
    result + " -> " + showFunctionContChain(cont.next, hl, vis, reps)
  else if cont === null then
    "(null)"
  else
    "(NOT HANDLER CONT)"

fun debugCont(cont) = console.log(showFunctionContChain(cont, new Map(), new Set(), 0))
fun debugHandler(cont) = console.log(showHandlerContChain(cont, new Map(), new Set(), 0))

fun debugContTrace(contTrace) =
  if contTrace is ContTrace then
    console.log("resumed: ", contTrace.resumed)
    if contTrace.last === contTrace do
      console.log("<last is self>")
    if contTrace.lastHandler === contTrace do
      console.log("<lastHandler is self>")
    let vis = new Set()
    let hl = new Map()
    hl.set("last", new Set([contTrace.last]))
    hl.set("last-handler", new Set([contTrace.lastHandler]))
    console.log(showFunctionContChain(contTrace.next, hl, vis, 0))
    let cur = contTrace.nextHandler
    while cur !== null do
      console.log(showHandlerContChain(cur, hl, vis, 0))
      set cur = cur.nextHandler
    console.log()
  else
    console.log("Not a cont trace:")
    console.log(contTrace)

fun debugEff(eff) =
  if eff is EffectSig then
    console.log("Debug EffectSig:")
    console.log("handler: ", eff.handler.constructor.name)
    console.log("handlerFun: ", eff.handlerFun)
    debugContTrace(eff.contTrace)
  else
    console.log("Not an effect:")
    console.log(eff)

// runtime implementations
fun mkEffect(handler, handlerFun) =
  let res = new EffectSig(new ContTrace(null, null, null, null, false), handler, handlerFun)
  set
    res.contTrace.last = res.contTrace
    res.contTrace.lastHandler = res.contTrace
  res

fun handleBlockImpl(cur, handler) =
  let handlerFrame = new HandlerContFrame(null, null, handler)
  set
    cur.contTrace.lastHandler.nextHandler = handlerFrame
    cur.contTrace.lastHandler = handlerFrame
    cur.contTrace.last = handlerFrame
  handleEffects(cur)

fun enterHandleBlock(handler, body) =
  let cur = body()
  if cur is EffectSig then
    handleBlockImpl(cur, handler)
  else
    cur

fun handleEffects(cur) =
  while cur is
    EffectSig then
      let nxt = handleEffect(cur)
      if cur === nxt then
        return cur
      else
        set cur = nxt
    else
      return cur

// return either new effect, final result or the same continuation if there is no handler
fun handleEffect(cur) =
  // debugEff(cur)
  // find the handle block corresponding to the current effect
  let prevHandlerFrame = cur.contTrace
  while prevHandlerFrame.nextHandler !== null and prevHandlerFrame.nextHandler.handler !== cur.handler do
    set prevHandlerFrame = prevHandlerFrame.nextHandler

  // no matching handle block
  if prevHandlerFrame.nextHandler === null do
    return cur

  // the matching handle block
  let handlerFrame = prevHandlerFrame.nextHandler

  // unlink and save frames
  let saved = new ContTrace(
    handlerFrame.next,
    cur.contTrace.last,
    handlerFrame.nextHandler,
    cur.contTrace.lastHandler,
    false
  )
  set
    cur.contTrace.last = handlerFrame
    cur.contTrace.lastHandler = handlerFrame
    handlerFrame.next = null
    handlerFrame.nextHandler = null

  // handle the effect
  set cur = cur.handlerFun(resume(cur.contTrace))
  if cur is EffectSig then
    // relink the saved frames
    if saved.next !== null do
      set
        cur.contTrace.last.next = saved.next
        cur.contTrace.last = saved.last
    if saved.nextHandler !== null do
      set
        cur.contTrace.lastHandler.nextHandler = saved.nextHandler
        cur.contTrace.lastHandler = saved.lastHandler
    cur
  else
    // resume the unlinked handle blocks
    resumeContTrace(saved, cur)

fun resume(contTrace)(value) =
  if contTrace.resumed do
    throw Error("Multiple resumption")
  set contTrace.resumed = true
  handleEffects(resumeContTrace(contTrace, value))

fun resumeContTrace(contTrace, value) =
  let cont = contTrace.next
  let handlerCont = contTrace.nextHandler
  while
    cont is FunctionContFrame then
      set value = cont.resume(value)
      if value is EffectSig then
        set
          value.contTrace.last.next = cont.next
          value.contTrace.lastHandler.nextHandler = handlerCont
        if contTrace.last !== cont do
          set value.contTrace.last = contTrace.last
        if handlerCont !== null do
          set value.contTrace.lastHandler = contTrace.lastHandler
        return value
      else
        set cont = cont.next
    handlerCont is HandlerContFrame then
      set cont = handlerCont.next
      set handlerCont = handlerCont.nextHandler
    else
      return value

// stack safety
mut val stackLimit = 0 // How deep the stack can go before heapifying the stack
mut val stackDepth = 0 // Tracks the virtual + real stack depth
mut val stackOffset = 0 // How much to offset stackDepth by to get the true stack depth (i.e. the virtual depth)
mut val stackHandler = null
mut val stackResume = null

object StackDelayHandler with
  fun delay() = mkEffect of this, k =>
    set stackResume = k

fun checkDepth() =
  if stackDepth - stackOffset >= stackLimit && stackHandler !== null then
    // this is a tail call to effectful function
    stackHandler.delay()
  else
    ()

fun resetDepth(tmp, curDepth) =
  set stackDepth = curDepth
  if curDepth < stackOffset do
    set stackOffset = curDepth
  tmp

fun runStackSafe(limit, f) =
  set
    stackLimit = limit
    stackDepth = 1
    stackOffset = 0
    stackHandler = StackDelayHandler
  let result = enterHandleBlock(StackDelayHandler, f)
  while stackResume !== null do
    let saved = stackResume
    set
      stackResume = null
      stackOffset = stackDepth
      result = saved()
  set
    stackLimit = 0
    stackDepth = 0
    stackOffset = 0
    stackHandler = null
  result
""" -> S("""
import runtime from "./Runtime.mjs";
import RuntimeJS from "./RuntimeJS.mjs";
import Rendering from "./Rendering.mjs";
let Runtime1;
(class Runtime {
  static {
    Runtime1 = Runtime;
    const Unit$class = class Unit {
      constructor() {}
      toString() {
        return "()"
      }
    };
    this.Unit = new Unit$class;
    this.Unit.class = Unit$class;
    this.try_catch = RuntimeJS.try_catch;
    this.EffectHandle = function EffectHandle(_reified1) {
      return new EffectHandle.class(_reified1);
    };
    this.EffectHandle.class = class EffectHandle {
      #_reified;
      constructor(_reified) {
        this.#_reified = _reified;
        this.reified = this.#_reified;
      }
      resumeWith(value) {
        let lambda;
        const this$EffectHandle = this;
        lambda = (undefined, function () {
          let tmp;
          tmp = Runtime.resume(this$EffectHandle.reified.contTrace);
          return runtime.safeCall(tmp(value))
        });
        return Runtime1.try(lambda)
      } 
      raise() {
        return Runtime.topLevelEffect(this.reified, false)
      }
      toString() { return "EffectHandle(" + "" + ")"; }
    };
    this.MatchResult = function MatchResult(captures1) {
      return new MatchResult.class(captures1);
    };
    this.MatchResult.class = class MatchResult {
      constructor(captures) {
        this.captures = captures;
      }
      toString() { return "MatchResult(" + runtime.render(this.captures) + ")"; }
    };
    this.MatchFailure = function MatchFailure(errors1) {
      return new MatchFailure.class(errors1);
    };
    this.MatchFailure.class = class MatchFailure {
      constructor(errors) {
        this.errors = errors;
      }
      toString() { return "MatchFailure(" + runtime.render(this.errors) + ")"; }
    };
    (class Tuple {
      static {
        Runtime.Tuple = Tuple;
      }
      static slice(xs, i, j) {
        let tmp;
        tmp = xs.length - j;
        return runtime.safeCall(globalThis.Array.prototype.slice.call(xs, i, tmp))
      } 
      static get(xs1, i1) {
        let scrut;
        scrut = i1 >= xs1.length;
        if (scrut === true) {
          throw globalThis.RangeError("Tuple.get: index out of bounds");
        } else {
          return globalThis.Array.prototype.at.call(xs1, i1)
        }
      }
      static toString() { return "Tuple"; }
    });
    (class Str {
      static {
        Runtime.Str = Str;
      }
      static startsWith(string, prefix) {
        return runtime.safeCall(string.startsWith(prefix))
      } 
      static get(string1, i) {
        let scrut;
        scrut = i >= string1.length;
        if (scrut === true) {
          throw globalThis.RangeError("Str.get: index out of bounds");
        } else {
          return runtime.safeCall(string1.at(i))
        }
      } 
      static drop(string2, n) {
        return runtime.safeCall(string2.slice(n))
      }
      static toString() { return "Str"; }
    });
    this.render = Rendering.render;
    (class TraceLogger {
      static {
        Runtime.TraceLogger = TraceLogger;
        this.enabled = false;
        this.indentLvl = 0;
      }
      static indent() {
        let scrut, prev, tmp;
        scrut = TraceLogger.enabled;
        if (scrut === true) {
          prev = TraceLogger.indentLvl;
          tmp = prev + 1;
          TraceLogger.indentLvl = tmp;
          return prev
        } else {
          return runtime.Unit
        }
      } 
      static resetIndent(n) {
        let scrut;
        scrut = TraceLogger.enabled;
        if (scrut === true) {
          TraceLogger.indentLvl = n;
          return runtime.Unit
        } else {
          return runtime.Unit
        }
      } 
      static log(msg) {
        let scrut, tmp, tmp1, tmp2, tmp3, tmp4;
        scrut = TraceLogger.enabled;
        if (scrut === true) {
          tmp = runtime.safeCall("| ".repeat(TraceLogger.indentLvl));
          tmp1 = runtime.safeCall("  ".repeat(TraceLogger.indentLvl));
          tmp2 = "\n" + tmp1;
          tmp3 = msg.replaceAll("\n", tmp2);
          tmp4 = tmp + tmp3;
          return runtime.safeCall(globalThis.console.log(tmp4))
        } else {
          return runtime.Unit
        }
      }
      static toString() { return "TraceLogger"; }
    });
    const FatalEffect$class = class FatalEffect {
      constructor() {}
      toString() { return "FatalEffect"; }
    };
    this.FatalEffect = new FatalEffect$class;
    this.FatalEffect.class = FatalEffect$class;
    const PrintStackEffect$class = class PrintStackEffect {
      constructor() {}
      toString() { return "PrintStackEffect"; }
    };
    this.PrintStackEffect = new PrintStackEffect$class;
    this.PrintStackEffect.class = PrintStackEffect$class;
    this.FunctionContFrame = function FunctionContFrame(next1) {
      return new FunctionContFrame.class(next1);
    };
    this.FunctionContFrame.class = class FunctionContFrame {
      constructor(next) {
        this.next = next;
      }
      toString() { return "FunctionContFrame(" + runtime.render(this.next) + ")"; }
    };
    this.HandlerContFrame = function HandlerContFrame(next1, nextHandler1, handler1) {
      return new HandlerContFrame.class(next1, nextHandler1, handler1);
    };
    this.HandlerContFrame.class = class HandlerContFrame {
      constructor(next, nextHandler, handler) {
        this.next = next;
        this.nextHandler = nextHandler;
        this.handler = handler;
      }
      toString() { return "HandlerContFrame(" + runtime.render(this.next) + ", " + runtime.render(this.nextHandler) + ", " + runtime.render(this.handler) + ")"; }
    };
    this.ContTrace = function ContTrace(next1, last1, nextHandler1, lastHandler1, resumed1) {
      return new ContTrace.class(next1, last1, nextHandler1, lastHandler1, resumed1);
    };
    this.ContTrace.class = class ContTrace {
      constructor(next, last, nextHandler, lastHandler, resumed) {
        this.next = next;
        this.last = last;
        this.nextHandler = nextHandler;
        this.lastHandler = lastHandler;
        this.resumed = resumed;
      }
      toString() { return "ContTrace(" + runtime.render(this.next) + ", " + runtime.render(this.last) + ", " + runtime.render(this.nextHandler) + ", " + runtime.render(this.lastHandler) + ", " + runtime.render(this.resumed) + ")"; }
    };
    this.EffectSig = function EffectSig(contTrace1, handler1, handlerFun1) {
      return new EffectSig.class(contTrace1, handler1, handlerFun1);
    };
    this.EffectSig.class = class EffectSig {
      constructor(contTrace, handler, handlerFun) {
        this.contTrace = contTrace;
        this.handler = handler;
        this.handlerFun = handlerFun;
      }
      toString() { return "EffectSig(" + runtime.render(this.contTrace) + ", " + runtime.render(this.handler) + ", " + runtime.render(this.handlerFun) + ")"; }
    };
    this.NonLocalReturn = class NonLocalReturn {
      constructor() {}
      toString() { return "NonLocalReturn"; }
    };
    this.FnLocalsInfo = function FnLocalsInfo(fnName1, locals1) {
      return new FnLocalsInfo.class(fnName1, locals1);
    };
    this.FnLocalsInfo.class = class FnLocalsInfo {
      constructor(fnName, locals) {
        this.fnName = fnName;
        this.locals = locals;
      }
      toString() { return "FnLocalsInfo(" + runtime.render(this.fnName) + ", " + runtime.render(this.locals) + ")"; }
    };
    this.LocalVarInfo = function LocalVarInfo(localName1, value1) {
      return new LocalVarInfo.class(localName1, value1);
    };
    this.LocalVarInfo.class = class LocalVarInfo {
      constructor(localName, value) {
        this.localName = localName;
        this.value = value;
      }
      toString() { return "LocalVarInfo(" + runtime.render(this.localName) + ", " + runtime.render(this.value) + ")"; }
    };
    this.stackLimit = 0;
    this.stackDepth = 0;
    this.stackOffset = 0;
    this.stackHandler = null;
    this.stackResume = null;
    const StackDelayHandler$class = class StackDelayHandler {
      constructor() {}
      delay() {
        let lambda;
        lambda = (undefined, function (k) {
          Runtime.stackResume = k;
          return runtime.Unit
        });
        return Runtime.mkEffect(this, lambda)
      }
      toString() { return "StackDelayHandler"; }
    };
    this.StackDelayHandler = new StackDelayHandler$class;
    this.StackDelayHandler.class = StackDelayHandler$class;
  }
  static get unreachable() {
    throw globalThis.Error("unreachable");
  } 
  static checkArgs(functionName, expected, isUB, got) {
    let scrut, name, scrut1, scrut2, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13, tmp14;
    tmp = got < expected;
    tmp1 = got > expected;
    tmp2 = isUB && tmp1;
    scrut = tmp || tmp2;
    if (scrut === true) {
      scrut1 = functionName.length > 0;
      if (scrut1 === true) {
        tmp3 = " '" + functionName;
        tmp4 = tmp3 + "'";
      } else {
        tmp4 = "";
      }
      name = tmp4;
      tmp5 = "Function" + name;
      tmp6 = tmp5 + " expected ";
      if (isUB === true) {
        tmp7 = "";
      } else {
        tmp7 = "at least ";
      }
      tmp8 = tmp6 + tmp7;
      tmp9 = tmp8 + expected;
      tmp10 = tmp9 + " argument";
      scrut2 = expected === 1;
      if (scrut2 === true) {
        tmp11 = "";
      } else {
        tmp11 = "s";
      }
      tmp12 = tmp10 + tmp11;
      tmp13 = tmp12 + " but got ";
      tmp14 = tmp13 + got;
      throw globalThis.Error(tmp14);
    } else {
      return runtime.Unit
    }
  } 
  static safeCall(x) {
    if (x === undefined) {
      return Runtime.Unit
    } else {
      return x
    }
  } 
  static checkCall(x1) {
    if (x1 === undefined) {
      throw globalThis.Error("MLscript call unexpectedly returned `undefined`, the forbidden value.");
    } else {
      return x1
    }
  } 
  static deboundMethod(mtdName, clsName) {
    let tmp, tmp1, tmp2, tmp3;
    tmp = "[debinding error] Method '" + mtdName;
    tmp1 = tmp + "' of class '";
    tmp2 = tmp1 + clsName;
    tmp3 = tmp2 + "' was accessed without being called.";
    throw globalThis.Error(tmp3);
  } 
  static try(f) {
    let res, tmp;
    tmp = runtime.safeCall(f());
    res = tmp;
    if (res instanceof Runtime.EffectSig.class) {
      return runtime.safeCall(Runtime.EffectHandle(res))
    } else {
      return res
    }
  } 
  static printRaw(x2) {
    let tmp;
    tmp = runtime.safeCall(Runtime.render(x2));
    return runtime.safeCall(globalThis.console.log(tmp))
  } 
  static raisePrintStackEffect(showLocals) {
    return Runtime.mkEffect(Runtime.PrintStackEffect, showLocals)
  } 
  static topLevelEffect(tr, debug) {
    let scrut, tmp, tmp1, tmp2, tmp3, tmp4, tmp5;
    tmp6: while (true) {
      scrut = tr.handler === Runtime.PrintStackEffect;
      if (scrut === true) {
        tmp = Runtime.showStackTrace("Stack Trace:", tr, debug, tr.handlerFun);
        tmp1 = runtime.safeCall(globalThis.console.log(tmp));
        tmp2 = Runtime.resume(tr.contTrace);
        tmp3 = runtime.safeCall(tmp2(runtime.Unit));
        tr = tmp3;
        tmp4 = runtime.Unit;
        continue tmp6;
      } else {
        tmp4 = runtime.Unit;
      }
      break;
    }
    if (tr instanceof Runtime.EffectSig.class) {
      tmp5 = "Error: Unhandled effect " + tr.handler.constructor.name;
      throw Runtime.showStackTrace(tmp5, tr, debug, false);
    } else {
      return tr
    }
  } 
  static showStackTrace(header, tr1, debug1, showLocals1) {
    let msg, curHandler, atTail, scrut, cur, scrut1, locals, curLocals, loc, loc1, localsMsg, scrut2, scrut3, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13, tmp14, tmp15, tmp16, tmp17, tmp18, tmp19, lambda;
    msg = header;
    curHandler = tr1.contTrace;
    atTail = true;
    if (debug1 === true) {
      tmp20: while (true) {
        scrut = curHandler !== null;
        if (scrut === true) {
          cur = curHandler.next;
          tmp21: while (true) {
            scrut1 = cur !== null;
            if (scrut1 === true) {
              locals = cur.getLocals;
              tmp = locals.length - 1;
              tmp1 = runtime.safeCall(locals.at(tmp));
              curLocals = tmp1;
              loc = cur.getLoc;
              if (loc === null) {
                tmp2 = "pc=" + cur.pc;
              } else {
                tmp2 = loc;
              }
              loc1 = tmp2;
              if (showLocals1 === true) {
                scrut2 = curLocals.locals.length > 0;
                if (scrut2 === true) {
                  lambda = (undefined, function (l) {
                    let tmp22, tmp23;
                    tmp22 = l.localName + "=";
                    tmp23 = Rendering.render(l.value);
                    return tmp22 + tmp23
                  });
                  tmp3 = runtime.safeCall(curLocals.locals.map(lambda));
                  tmp4 = runtime.safeCall(tmp3.join(", "));
                  tmp5 = " with locals: " + tmp4;
                } else {
                  tmp5 = "";
                }
              } else {
                tmp5 = "";
              }
              localsMsg = tmp5;
              tmp6 = "\n\tat " + curLocals.fnName;
              tmp7 = tmp6 + " (";
              tmp8 = tmp7 + loc1;
              tmp9 = tmp8 + ")";
              tmp10 = msg + tmp9;
              msg = tmp10;
              tmp11 = msg + localsMsg;
              msg = tmp11;
              cur = cur.next;
              atTail = false;
              tmp12 = runtime.Unit;
              continue tmp21;
            } else {
              tmp12 = runtime.Unit;
            }
            break;
          }
          curHandler = curHandler.nextHandler;
          scrut3 = curHandler !== null;
          if (scrut3 === true) {
            tmp13 = "\n\twith handler " + curHandler.handler.constructor.name;
            tmp14 = msg + tmp13;
            msg = tmp14;
            atTail = false;
            tmp15 = runtime.Unit;
          } else {
            tmp15 = runtime.Unit;
          }
          tmp16 = tmp15;
          continue tmp20;
        } else {
          tmp16 = runtime.Unit;
        }
        break;
      }
      if (atTail === true) {
        tmp17 = msg + "\n\tat tail position";
        msg = tmp17;
        tmp18 = runtime.Unit;
      } else {
        tmp18 = runtime.Unit;
      }
      tmp19 = tmp18;
    } else {
      tmp19 = runtime.Unit;
    }
    return msg
  } 
  static showFunctionContChain(cont, hl, vis, reps) {
    let scrut, result, scrut1, scrut2, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, lambda;
    if (cont instanceof Runtime.FunctionContFrame.class) {
      tmp = cont.constructor.name + "(pc=";
      tmp1 = tmp + cont.pc;
      result = tmp1;
      lambda = (undefined, function (m, marker) {
        let scrut3, tmp10, tmp11;
        scrut3 = runtime.safeCall(m.has(cont));
        if (scrut3 === true) {
          tmp10 = ", " + marker;
          tmp11 = result + tmp10;
          result = tmp11;
          return runtime.Unit
        } else {
          return runtime.Unit
        }
      });
      tmp2 = lambda;
      tmp3 = runtime.safeCall(hl.forEach(tmp2));
      scrut1 = runtime.safeCall(vis.has(cont));
      if (scrut1 === true) {
        tmp4 = reps + 1;
        reps = tmp4;
        scrut2 = reps > 10;
        if (scrut2 === true) {
          throw globalThis.Error("10 repeated continuation frame (loop?)");
        } else {
          tmp5 = runtime.Unit;
        }
        tmp6 = result + ", REPEAT";
        result = tmp6;
        tmp7 = runtime.Unit;
      } else {
        tmp7 = runtime.safeCall(vis.add(cont));
      }
      tmp8 = result + ") -> ";
      tmp9 = Runtime.showFunctionContChain(cont.next, hl, vis, reps);
      return tmp8 + tmp9
    } else {
      scrut = cont === null;
      if (scrut === true) {
        return "(null)"
      } else {
        return "(NOT CONT)"
      }
    }
  } 
  static showHandlerContChain(cont1, hl1, vis1, reps1) {
    let scrut, result, scrut1, scrut2, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, lambda;
    if (cont1 instanceof Runtime.HandlerContFrame.class) {
      result = cont1.handler.constructor.name;
      lambda = (undefined, function (m, marker) {
        let scrut3, tmp8, tmp9;
        scrut3 = runtime.safeCall(m.has(cont1));
        if (scrut3 === true) {
          tmp8 = ", " + marker;
          tmp9 = result + tmp8;
          result = tmp9;
          return runtime.Unit
        } else {
          return runtime.Unit
        }
      });
      tmp = lambda;
      tmp1 = runtime.safeCall(hl1.forEach(tmp));
      scrut1 = runtime.safeCall(vis1.has(cont1));
      if (scrut1 === true) {
        tmp2 = reps1 + 1;
        reps1 = tmp2;
        scrut2 = reps1 > 10;
        if (scrut2 === true) {
          throw globalThis.Error("10 repeated continuation frame (loop?)");
        } else {
          tmp3 = runtime.Unit;
        }
        tmp4 = result + ", REPEAT";
        result = tmp4;
        tmp5 = runtime.Unit;
      } else {
        tmp5 = runtime.safeCall(vis1.add(cont1));
      }
      tmp6 = result + " -> ";
      tmp7 = Runtime.showFunctionContChain(cont1.next, hl1, vis1, reps1);
      return tmp6 + tmp7
    } else {
      scrut = cont1 === null;
      if (scrut === true) {
        return "(null)"
      } else {
        return "(NOT HANDLER CONT)"
      }
    }
  } 
  static debugCont(cont2) {
    let tmp, tmp1, tmp2;
    tmp = new globalThis.Map();
    tmp1 = new globalThis.Set();
    tmp2 = Runtime.showFunctionContChain(cont2, tmp, tmp1, 0);
    return runtime.safeCall(globalThis.console.log(tmp2))
  } 
  static debugHandler(cont3) {
    let tmp, tmp1, tmp2;
    tmp = new globalThis.Map();
    tmp1 = new globalThis.Set();
    tmp2 = Runtime.showHandlerContChain(cont3, tmp, tmp1, 0);
    return runtime.safeCall(globalThis.console.log(tmp2))
  } 
  static debugContTrace(contTrace) {
    let scrut, scrut1, vis2, hl2, cur, scrut2, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13, tmp14;
    if (contTrace instanceof Runtime.ContTrace.class) {
      tmp = globalThis.console.log("resumed: ", contTrace.resumed);
      scrut = contTrace.last === contTrace;
      if (scrut === true) {
        tmp1 = runtime.safeCall(globalThis.console.log("<last is self>"));
      } else {
        tmp1 = runtime.Unit;
      }
      scrut1 = contTrace.lastHandler === contTrace;
      if (scrut1 === true) {
        tmp2 = runtime.safeCall(globalThis.console.log("<lastHandler is self>"));
      } else {
        tmp2 = runtime.Unit;
      }
      tmp3 = new globalThis.Set();
      vis2 = tmp3;
      tmp4 = new globalThis.Map();
      hl2 = tmp4;
      tmp5 = new globalThis.Set([
        contTrace.last
      ]);
      tmp6 = hl2.set("last", tmp5);
      tmp7 = new globalThis.Set([
        contTrace.lastHandler
      ]);
      tmp8 = hl2.set("last-handler", tmp7);
      tmp9 = Runtime.showFunctionContChain(contTrace.next, hl2, vis2, 0);
      tmp10 = runtime.safeCall(globalThis.console.log(tmp9));
      cur = contTrace.nextHandler;
      tmp15: while (true) {
        scrut2 = cur !== null;
        if (scrut2 === true) {
          tmp11 = Runtime.showHandlerContChain(cur, hl2, vis2, 0);
          tmp12 = runtime.safeCall(globalThis.console.log(tmp11));
          cur = cur.nextHandler;
          tmp13 = runtime.Unit;
          continue tmp15;
        } else {
          tmp13 = runtime.Unit;
        }
        break;
      }
      return runtime.safeCall(globalThis.console.log())
    } else {
      tmp14 = runtime.safeCall(globalThis.console.log("Not a cont trace:"));
      return runtime.safeCall(globalThis.console.log(contTrace))
    }
  } 
  static debugEff(eff) {
    let tmp, tmp1, tmp2, tmp3;
    if (eff instanceof Runtime.EffectSig.class) {
      tmp = runtime.safeCall(globalThis.console.log("Debug EffectSig:"));
      tmp1 = globalThis.console.log("handler: ", eff.handler.constructor.name);
      tmp2 = globalThis.console.log("handlerFun: ", eff.handlerFun);
      return Runtime.debugContTrace(eff.contTrace)
    } else {
      tmp3 = runtime.safeCall(globalThis.console.log("Not an effect:"));
      return runtime.safeCall(globalThis.console.log(eff))
    }
  } 
  static mkEffect(handler, handlerFun) {
    let res, tmp, tmp1;
    tmp = new Runtime.ContTrace.class(null, null, null, null, false);
    tmp1 = new Runtime.EffectSig.class(tmp, handler, handlerFun);
    res = tmp1;
    res.contTrace.last = res.contTrace;
    res.contTrace.lastHandler = res.contTrace;
    return res
  } 
  static handleBlockImpl(cur, handler1) {
    let handlerFrame, tmp;
    tmp = new Runtime.HandlerContFrame.class(null, null, handler1);
    handlerFrame = tmp;
    cur.contTrace.lastHandler.nextHandler = handlerFrame;
    cur.contTrace.lastHandler = handlerFrame;
    cur.contTrace.last = handlerFrame;
    return Runtime.handleEffects(cur)
  } 
  static enterHandleBlock(handler2, body) {
    let cur1, tmp;
    tmp = runtime.safeCall(body());
    cur1 = tmp;
    if (cur1 instanceof Runtime.EffectSig.class) {
      return Runtime.handleBlockImpl(cur1, handler2)
    } else {
      return cur1
    }
  } 
  static handleEffects(cur1) {
    let nxt, scrut, tmp, tmp1, tmp2;
    tmp3: while (true) {
      if (cur1 instanceof Runtime.EffectSig.class) {
        tmp = Runtime.handleEffect(cur1);
        nxt = tmp;
        scrut = cur1 === nxt;
        if (scrut === true) {
          return cur1
        } else {
          cur1 = nxt;
          tmp1 = runtime.Unit;
        }
        tmp2 = tmp1;
        continue tmp3;
      } else {
        return cur1
      }
      break;
    }
    return tmp2
  } 
  static handleEffect(cur2) {
    let prevHandlerFrame, scrut, scrut1, scrut2, handlerFrame, saved, scrut3, scrut4, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6;
    prevHandlerFrame = cur2.contTrace;
    tmp7: while (true) {
      scrut = prevHandlerFrame.nextHandler !== null;
      if (scrut === true) {
        scrut1 = prevHandlerFrame.nextHandler.handler !== cur2.handler;
        if (scrut1 === true) {
          prevHandlerFrame = prevHandlerFrame.nextHandler;
          tmp = runtime.Unit;
          continue tmp7;
        } else {
          tmp = runtime.Unit;
        }
      } else {
        tmp = runtime.Unit;
      }
      break;
    }
    scrut2 = prevHandlerFrame.nextHandler === null;
    if (scrut2 === true) {
      return cur2
    } else {
      tmp1 = runtime.Unit;
    }
    handlerFrame = prevHandlerFrame.nextHandler;
    tmp2 = new Runtime.ContTrace.class(handlerFrame.next, cur2.contTrace.last, handlerFrame.nextHandler, cur2.contTrace.lastHandler, false);
    saved = tmp2;
    cur2.contTrace.last = handlerFrame;
    cur2.contTrace.lastHandler = handlerFrame;
    handlerFrame.next = null;
    handlerFrame.nextHandler = null;
    tmp3 = Runtime.resume(cur2.contTrace);
    tmp4 = runtime.safeCall(cur2.handlerFun(tmp3));
    cur2 = tmp4;
    if (cur2 instanceof Runtime.EffectSig.class) {
      scrut3 = saved.next !== null;
      if (scrut3 === true) {
        cur2.contTrace.last.next = saved.next;
        cur2.contTrace.last = saved.last;
        tmp5 = runtime.Unit;
      } else {
        tmp5 = runtime.Unit;
      }
      scrut4 = saved.nextHandler !== null;
      if (scrut4 === true) {
        cur2.contTrace.lastHandler.nextHandler = saved.nextHandler;
        cur2.contTrace.lastHandler = saved.lastHandler;
        tmp6 = runtime.Unit;
      } else {
        tmp6 = runtime.Unit;
      }
      return cur2
    } else {
      return Runtime.resumeContTrace(saved, cur2)
    }
  } 
  static resume(contTrace1) {
    return (value) => {
      let scrut, tmp, tmp1;
      scrut = contTrace1.resumed;
      if (scrut === true) {
        throw globalThis.Error("Multiple resumption");
      } else {
        tmp = runtime.Unit;
      }
      contTrace1.resumed = true;
      tmp1 = Runtime.resumeContTrace(contTrace1, value);
      return Runtime.handleEffects(tmp1)
    }
  } 
  static resumeContTrace(contTrace2, value) {
    let cont4, handlerCont, scrut, scrut1, tmp, tmp1, tmp2, tmp3, tmp4;
    cont4 = contTrace2.next;
    handlerCont = contTrace2.nextHandler;
    tmp5: while (true) {
      if (cont4 instanceof Runtime.FunctionContFrame.class) {
        tmp = runtime.safeCall(cont4.resume(value));
        value = tmp;
        if (value instanceof Runtime.EffectSig.class) {
          value.contTrace.last.next = cont4.next;
          value.contTrace.lastHandler.nextHandler = handlerCont;
          scrut = contTrace2.last !== cont4;
          if (scrut === true) {
            value.contTrace.last = contTrace2.last;
            tmp1 = runtime.Unit;
          } else {
            tmp1 = runtime.Unit;
          }
          scrut1 = handlerCont !== null;
          if (scrut1 === true) {
            value.contTrace.lastHandler = contTrace2.lastHandler;
            tmp2 = runtime.Unit;
          } else {
            tmp2 = runtime.Unit;
          }
          return value
        } else {
          cont4 = cont4.next;
          tmp3 = runtime.Unit;
        }
        tmp4 = tmp3;
        continue tmp5;
      } else {
        if (handlerCont instanceof Runtime.HandlerContFrame.class) {
          cont4 = handlerCont.next;
          handlerCont = handlerCont.nextHandler;
          tmp4 = runtime.Unit;
          continue tmp5;
        } else {
          return value
        }
      }
      break;
    }
    return tmp4
  } 
  static checkDepth() {
    let scrut, tmp, tmp1, tmp2;
    tmp = Runtime.stackDepth - Runtime.stackOffset;
    tmp1 = tmp >= Runtime.stackLimit;
    tmp2 = Runtime.stackHandler !== null;
    scrut = tmp1 && tmp2;
    if (scrut === true) {
      return runtime.safeCall(Runtime.stackHandler.delay())
    } else {
      return runtime.Unit
    }
  } 
  static resetDepth(tmp, curDepth) {
    let scrut, tmp1;
    Runtime.stackDepth = curDepth;
    scrut = curDepth < Runtime.stackOffset;
    if (scrut === true) {
      Runtime.stackOffset = curDepth;
      tmp1 = runtime.Unit;
    } else {
      tmp1 = runtime.Unit;
    }
    return tmp
  } 
  static runStackSafe(limit, f1) {
    let result, scrut, saved, tmp1, tmp2, tmp3;
    Runtime.stackLimit = limit;
    Runtime.stackDepth = 1;
    Runtime.stackOffset = 0;
    Runtime.stackHandler = Runtime.StackDelayHandler;
    tmp1 = Runtime.enterHandleBlock(Runtime.StackDelayHandler, f1);
    result = tmp1;
    tmp4: while (true) {
      scrut = Runtime.stackResume !== null;
      if (scrut === true) {
        saved = Runtime.stackResume;
        Runtime.stackResume = null;
        Runtime.stackOffset = Runtime.stackDepth;
        tmp2 = runtime.safeCall(saved());
        result = tmp2;
        tmp3 = runtime.Unit;
        continue tmp4;
      } else {
        tmp3 = runtime.Unit;
      }
      break;
    }
    Runtime.stackLimit = 0;
    Runtime.stackDepth = 0;
    Runtime.stackOffset = 0;
    Runtime.stackHandler = null;
    return result
  }
  static toString() { return "Runtime"; }
});
let Runtime = Runtime1; export default Runtime;
"""))

  fileNameSourceMap += "CachedHash.mls" -> ("""
class CachedHash with
  let _hash = null
  fun hash() = if _hash !== null then _hash else
    let h = this.toString() // TODO: use a proper hash function!!
    set _hash = h
    h

""" -> S("""
const definitionMetadata = globalThis.Symbol.for("mlscript.definitionMetadata");
const prettyPrint = globalThis.Symbol.for("mlscript.prettyPrint");
import runtime from "./Runtime.mjs";
let CachedHash1;
(class CachedHash {
  static {
    CachedHash1 = this
  }
  constructor() {
    this.#_hash = null;
  }
  #_hash;
  hash() {
    let scrut, h;
    scrut = this.#_hash !== null;
    if (scrut === true) {
      return this.#_hash
    } else {
      h = runtime.safeCall(this.toString());
      this.#_hash = h;
      return h
    }
  }
  toString() { return runtime.render(this); }
  static [definitionMetadata] = ["class", "CachedHash"]; 
});
let CachedHash = CachedHash1; export default CachedHash;
"""))

  fileNameSourceMap += "Block.mls" -> ("""
import "./Predef.mls"
import "./Option.mls"
import "./StrOps.mls"
import "./CachedHash.mls"

// import "fs"
// import "process"
// import "path"
// import "url"

open Predef
open StrOps
open Option

module Block with...

type Opt[A] = Option[A]

// dependancies referenced in Block classes, referencing implementation in Term.mls

type Literal = null | undefined | Str | Int | Num | Bool

type ParamList = Array[Symbol]

class Symbol(val name: Str) extends CachedHash
class VirtualClassSymbol(val name: Str) extends Symbol(name)
class ClassSymbol(val name: Str, val value, val paramsOpt: Option[ParamList], val auxParams: Array[ParamList]) extends Symbol(name)
class ModuleSymbol(val name: Str, val value) extends Symbol(name)
class Arm(val cse: Case, val body: Block)

fun isPrimitiveType(sym: Symbol) =
  if sym.name is
    "Str" then true
    "Int" then true
    "Num" then true
    "Bool" then true
    else false

fun isPrimitiveTypeOf(sym: Symbol, l: Literal) =
  if [sym.name, l] is
    ["Str", l] and l is Str then true
    ["Int", i] and i is Int then true
    ["Num", n] and n is Num then true
    ["Bool", b] and b is Bool then true
    else false

// TODO: move this to somewhere appropriate
// TODO: is this the only way? we cannot assign a property to a class that is not extensible
fun checkMap(symbolMap, key, value) =
  // this approach of updating the class value doesn't work if the class is not extensible
  // if classValue.("stagedSymbol") is
  //   v and v is Symbol then v
  //   else set classValue.("stagedSymbol") = value; value
  // console.log(symbolMap, value)
  // let key = value.hash()
  let v = symbolMap.get(key)
  if v is
    Symbol then v
    else symbolMap.set(key, value); value

class Arg(val value: Path)

class Case with
  constructor
    Lit(val lit: Literal)
    Cls(val cls: ClassSymbol, val path: Path)
    Tup(val len: Int)

class Result extends CachedHash with
  constructor
    Call(val _fun: Path, val args: Array[Arg])
    Instantiate(val cls: Path, val args: Array[Arg]) // assume immutable
    Tuple(val elems: Array[Arg]) // assume immutable

class Path extends Result with
  constructor
    Select(val qual: Path, val name: Symbol)
    DynSelect(val qual: Path, val fld: Path, val arrayIdx: Bool) // is arrayIdx used?
    ValueRef(val l: Symbol)
    ValueLit(val lit: Literal)

class Defn with
  constructor
    ValDefn(val tsym: Symbol, val sym: Symbol, val rhs: Path)
    ClsLikeDefn(val sym: ClassSymbol, val methods: Array[FunDefn], val companion: Opt[ClsLikeBody]) // companion unused
    FunDefn(val sym: Symbol, val params: Array[ParamList], val body: Block)

class ClsLikeBody(val isym: Symbol, val methods: Array[FunDefn], val publicFields: Array[[Symbol, Symbol]]) // unused

class Block with
  constructor
    Match(val scrut: Path, val arms: Array[Arm], val dflt: Opt[Block], val rest: Block)
    Return(val res: Result, val implct: Bool)
    Assign(val lhs: Symbol, val rhs: Result, val rest: Block)
    Define(val defn: Defn, val rest: Block)
    // TODO: [fyp] handle Scoped nodes
    Scoped(val symbols: Array[Symbol], val rest: Block)
    End()

fun concat(b1: Block, b2: Block) = if b1 is
  Match(scrut, arms, dflt, rest) then Match(scrut, arms, dflt, concat(rest, b2))
  Return(res, implct) then b1
  Assign(lhs, rhs, rest) then Assign(lhs, rhs, concat(rest, b2))
  Define(defn, rest) then Define(defn, concat(rest, b2))
  Scoped(symbols, rest) then Scoped(symbols, concat(rest, b2))
  End() then b2

fun indent(s: Str) = s.replaceAll("\n", "\n  ")

fun showLiteral(l: Literal) =
  if l is
    undefined then "undefined"
    null then "null"
    Str then "\"" + l.toString() + "\""
    else l.toString()

fun showSymbol(s: Symbol) = if s.name is
  "runtime" then "Runtime"
  else s.name.replaceAll("$", "_")

fun showPath(p: Path): Str =
  if p is
    Select(qual, name) then showPath(qual) + "." + showSymbol(name)
    DynSelect(qual, fld, arrayIdx) then showPath(qual) + ".(" + showPath(fld) + ")"
    ValueRef(l) then showSymbol(l)
    ValueLit(lit) then showLiteral(lit)

fun showArg(arg: Arg) =
  showPath(arg.value)

fun showArgs(args: Array[Arg]) =
  args.map(showArg).join(", ")

fun showResult(r: Result): Str =
  if r is
    Path then showPath(r)
    Call(fun_, args) and
      args is [lhs, rhs] and fun_ is
        ValueRef(Symbol("+")) then showArg(lhs) + "+" + showArg(rhs)
        ValueRef(Symbol("-")) then showArg(lhs) + "-" + showArg(rhs)
      else showPath(fun_) + "(" + showArgs(args) + ")"
    Instantiate(cls, args) then "new " + showPath(cls) + "(" + showArgs(args) + ")"
    Tuple(elems) then "[" + showArgs(elems) + "]"
    _ then "<unknown result:" ~ r ~ ">"

fun showCase(c) =
  if c is
    Lit(l) then showLiteral(l)
    Cls(cls, p) then showSymbol(cls) +
      if isPrimitiveType(cls) then ""
        else showParamsOpt(cls.paramsOpt)
    Tup(len) then "[" + Array(len).fill("_").join(", ") + "]"
    _ then "<unknown case>"

fun showArm(a) =
  showCase(a.cse) + " then" + (if a.body is Return then " " else "\n  ") + indent(showBlock(a.body))

fun showParams(p: ParamList) =
  "(" + p.map(showSymbol(_)).join(", ") + ")"

fun showParamsOpt(p) =
  if p is
    Some(s) then showParams(s)
    None then ""

fun showParamList(ps: Array[ParamList]) =
  ps.map(showParams).join("")

fun showDefn(d: Defn): Str =
  if d is
    FunDefn(sym, ps, body) then
      "fun " + showSymbol(sym) + showParamList(ps) + " =" +
      (if body is Return then " " else "\n  ") + indent(showBlock(body))
    ClsLikeDefn(sym, methods, _) then
      "class " + showSymbol(sym) + showParamsOpt(sym.paramsOpt) + sym.auxParams.map(showParams).join("")
        + if methods is [] then "" else " with \n  " + indent(methods.map(showDefn).join("\n"))
    ValDefn(owner, sym, rhs) then
      (if owner is Some(owner) then showSymbol(owner) else "")
        + "." + showSymbol(sym) + " = " + showPath(rhs)
    _ then "<unknown defn: " + d.toString() + " >"

fun showBlock(b) =
  if b is
    Assign(lhs, rhs, rest) then
      showSymbol(lhs) + " = " + showResult(rhs) + showRestBlock(rest)
    Define(d, rest) then
      showDefn(d) + showRestBlock(rest)
    Return(res, _) then
      showResult(res)
    Match(scrut, arms, dflt, rest) then
      "if " + showPath(scrut) + " is"
      + indent("\n" + arms.map(showArm).join("\n"))
      + indent(if dflt is Some(db) then "\nelse" + indent("\n" + showBlock(db)) else "")
      + showRestBlock(rest)
    Scoped(symbols, rest) then
      symbols.map(showSymbol).map("let " + _).join("\n") + showRestBlock(rest)
    End() then "()"
    _ then "<unknown block:" ~ b ~ ">"

// removes trailing newline
fun showRestBlock(b : Block): Str =
  if b is End then "" else "\n" + showBlock(b)

fun show(x) =
  if x is
    Symbol then showSymbol(x)
    Path then showPath(x)
    Result then showResult(x)
    Case then showCase(x)
    Defn then showDefn(x)
    Block then showBlock(x)

fun printCode(x) =
  print(show(x))

fun printModule(name, methods) = print("module " + name + " with" + indent("\n" + methods.map(showDefn).join("\n")))

fun genMod(name, methods) =
  "module " + name + " with" + indent("\n" + methods.map(showDefn).join("\n"))

// TODO: move to another file!
fun codegen(name, methods, file) = ()
  // let fullpath = path.join of process.cwd(), file
  // let code = "module " + name + " with" + indent("\n" + methods.map(showDefn).join("\n"))
  // if not fs.existsSync(fullpath) do
  //   fs.mkdirSync(path.dirname(fullpath), recursive: true)
  //   fs.writeFileSync(fullpath, "", "utf8")
  // let originData = fs.readFileSync(fullpath, "utf8")
  // let newData = code
  // if newData != originData do
  //   fs.writeFileSync(fullpath, newData, "utf8")

""" -> S("""
const definitionMetadata = globalThis.Symbol.for("mlscript.definitionMetadata");
const prettyPrint = globalThis.Symbol.for("mlscript.prettyPrint");
import runtime from "./Runtime.mjs";
import Predef from "./Predef.mjs";
import Option from "./Option.mjs";
import StrOps from "./StrOps.mjs";
import CachedHash from "./CachedHash.mjs";
let Block2;
(class Block {
  static {
    Block2 = this
  }
  constructor() {
    runtime.Unit;
  }
  static {
    this.Symbol = function Symbol(name) {
      return globalThis.Object.freeze(new Symbol.class(name));
    };
    (class Symbol extends CachedHash {
      static {
        Block.Symbol.class = this
      }
      constructor(name) {
        super();
        this.name = name;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Symbol", ["name"]]; 
    });
    this.VirtualClassSymbol = function VirtualClassSymbol(name) {
      return globalThis.Object.freeze(new VirtualClassSymbol.class(name));
    };
    (class VirtualClassSymbol extends Block.Symbol.class {
      static {
        Block.VirtualClassSymbol.class = this
      }
      constructor(name) {
        super(name);
        this.name = name;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "VirtualClassSymbol", ["name"]]; 
    });
    this.ClassSymbol = function ClassSymbol(name, value, paramsOpt, auxParams) {
      return globalThis.Object.freeze(new ClassSymbol.class(name, value, paramsOpt, auxParams));
    };
    (class ClassSymbol extends Block.Symbol.class {
      static {
        Block.ClassSymbol.class = this
      }
      constructor(name, value, paramsOpt, auxParams) {
        super(name);
        this.name = name;
        this.value = value;
        this.paramsOpt = paramsOpt;
        this.auxParams = auxParams;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "ClassSymbol", ["name", "value", "paramsOpt", "auxParams"]]; 
    });
    this.ModuleSymbol = function ModuleSymbol(name, value) {
      return globalThis.Object.freeze(new ModuleSymbol.class(name, value));
    };
    (class ModuleSymbol extends Block.Symbol.class {
      static {
        Block.ModuleSymbol.class = this
      }
      constructor(name, value) {
        super(name);
        this.name = name;
        this.value = value;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "ModuleSymbol", ["name", "value"]]; 
    });
    this.Arm = function Arm(cse, body) {
      return globalThis.Object.freeze(new Arm.class(cse, body));
    };
    (class Arm {
      static {
        Block.Arm.class = this
      }
      constructor(cse, body) {
        this.cse = cse;
        this.body = body;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Arm", ["cse", "body"]]; 
    });
    this.Arg = function Arg(value) {
      return globalThis.Object.freeze(new Arg.class(value));
    };
    (class Arg {
      static {
        Block.Arg.class = this
      }
      constructor(value) {
        this.value = value;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Arg", ["value"]]; 
    });
    (class Case {
      static {
        Block.Case = this
      }
      constructor() {}
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Case"]; 
    });
    this.Lit = function Lit(lit) {
      return globalThis.Object.freeze(new Lit.class(lit));
    };
    (class Lit extends Block.Case {
      static {
        Block.Lit.class = this
      }
      constructor(lit) {
        super();
        this.lit = lit;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Lit", ["lit"]]; 
    });
    this.Cls = function Cls(cls, path) {
      return globalThis.Object.freeze(new Cls.class(cls, path));
    };
    (class Cls extends Block.Case {
      static {
        Block.Cls.class = this
      }
      constructor(cls, path) {
        super();
        this.cls = cls;
        this.path = path;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Cls", ["cls", "path"]]; 
    });
    this.Tup = function Tup(len) {
      return globalThis.Object.freeze(new Tup.class(len));
    };
    (class Tup extends Block.Case {
      static {
        Block.Tup.class = this
      }
      constructor(len) {
        super();
        this.len = len;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Tup", ["len"]]; 
    });
    (class Result extends CachedHash {
      static {
        Block.Result = this
      }
      constructor() {
        super();
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Result"]; 
    });
    this.Call = function Call(_fun, args) {
      return globalThis.Object.freeze(new Call.class(_fun, args));
    };
    (class Call extends Block.Result {
      static {
        Block.Call.class = this
      }
      constructor(_fun, args) {
        super();
        this._fun = _fun;
        this.args = args;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Call", ["_fun", "args"]]; 
    });
    this.Instantiate = function Instantiate(cls, args) {
      return globalThis.Object.freeze(new Instantiate.class(cls, args));
    };
    (class Instantiate extends Block.Result {
      static {
        Block.Instantiate.class = this
      }
      constructor(cls, args) {
        super();
        this.cls = cls;
        this.args = args;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Instantiate", ["cls", "args"]]; 
    });
    this.Tuple = function Tuple(elems) {
      return globalThis.Object.freeze(new Tuple.class(elems));
    };
    (class Tuple extends Block.Result {
      static {
        Block.Tuple.class = this
      }
      constructor(elems) {
        super();
        this.elems = elems;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Tuple", ["elems"]]; 
    });
    (class Path extends Block.Result {
      static {
        Block.Path = this
      }
      constructor() {
        super();
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Path"]; 
    });
    this.Select = function Select(qual, name) {
      return globalThis.Object.freeze(new Select.class(qual, name));
    };
    (class Select extends Block.Path {
      static {
        Block.Select.class = this
      }
      constructor(qual, name) {
        super();
        this.qual = qual;
        this.name = name;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Select", ["qual", "name"]]; 
    });
    this.DynSelect = function DynSelect(qual, fld, arrayIdx) {
      return globalThis.Object.freeze(new DynSelect.class(qual, fld, arrayIdx));
    };
    (class DynSelect extends Block.Path {
      static {
        Block.DynSelect.class = this
      }
      constructor(qual, fld, arrayIdx) {
        super();
        this.qual = qual;
        this.fld = fld;
        this.arrayIdx = arrayIdx;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "DynSelect", ["qual", "fld", "arrayIdx"]]; 
    });
    this.ValueRef = function ValueRef(l) {
      return globalThis.Object.freeze(new ValueRef.class(l));
    };
    (class ValueRef extends Block.Path {
      static {
        Block.ValueRef.class = this
      }
      constructor(l) {
        super();
        this.l = l;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "ValueRef", ["l"]]; 
    });
    this.ValueLit = function ValueLit(lit) {
      return globalThis.Object.freeze(new ValueLit.class(lit));
    };
    (class ValueLit extends Block.Path {
      static {
        Block.ValueLit.class = this
      }
      constructor(lit) {
        super();
        this.lit = lit;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "ValueLit", ["lit"]]; 
    });
    (class Defn {
      static {
        Block.Defn = this
      }
      constructor() {}
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Defn"]; 
    });
    this.ValDefn = function ValDefn(tsym, sym, rhs) {
      return globalThis.Object.freeze(new ValDefn.class(tsym, sym, rhs));
    };
    (class ValDefn extends Block.Defn {
      static {
        Block.ValDefn.class = this
      }
      constructor(tsym, sym, rhs) {
        super();
        this.tsym = tsym;
        this.sym = sym;
        this.rhs = rhs;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "ValDefn", ["tsym", "sym", "rhs"]]; 
    });
    this.ClsLikeDefn = function ClsLikeDefn(sym, methods, companion) {
      return globalThis.Object.freeze(new ClsLikeDefn.class(sym, methods, companion));
    };
    (class ClsLikeDefn extends Block.Defn {
      static {
        Block.ClsLikeDefn.class = this
      }
      constructor(sym, methods, companion) {
        super();
        this.sym = sym;
        this.methods = methods;
        this.companion = companion;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "ClsLikeDefn", ["sym", "methods", "companion"]]; 
    });
    this.FunDefn = function FunDefn(sym, params, body) {
      return globalThis.Object.freeze(new FunDefn.class(sym, params, body));
    };
    (class FunDefn extends Block.Defn {
      static {
        Block.FunDefn.class = this
      }
      constructor(sym, params, body) {
        super();
        this.sym = sym;
        this.params = params;
        this.body = body;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "FunDefn", ["sym", "params", "body"]]; 
    });
    this.ClsLikeBody = function ClsLikeBody(isym, methods, publicFields) {
      return globalThis.Object.freeze(new ClsLikeBody.class(isym, methods, publicFields));
    };
    (class ClsLikeBody {
      static {
        Block.ClsLikeBody.class = this
      }
      constructor(isym, methods, publicFields) {
        this.isym = isym;
        this.methods = methods;
        this.publicFields = publicFields;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "ClsLikeBody", ["isym", "methods", "publicFields"]]; 
    });
    (class Block1 {
      static {
        Block.Block = this
      }
      constructor() {}
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Block"]; 
    });
    this.Match = function Match(scrut, arms, dflt, rest) {
      return globalThis.Object.freeze(new Match.class(scrut, arms, dflt, rest));
    };
    (class Match extends Block.Block {
      static {
        Block.Match.class = this
      }
      constructor(scrut, arms, dflt, rest) {
        super();
        this.scrut = scrut;
        this.arms = arms;
        this.dflt = dflt;
        this.rest = rest;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Match", ["scrut", "arms", "dflt", "rest"]]; 
    });
    this.Return = function Return(res, implct) {
      return globalThis.Object.freeze(new Return.class(res, implct));
    };
    (class Return extends Block.Block {
      static {
        Block.Return.class = this
      }
      constructor(res, implct) {
        super();
        this.res = res;
        this.implct = implct;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Return", ["res", "implct"]]; 
    });
    this.Assign = function Assign(lhs, rhs, rest) {
      return globalThis.Object.freeze(new Assign.class(lhs, rhs, rest));
    };
    (class Assign extends Block.Block {
      static {
        Block.Assign.class = this
      }
      constructor(lhs, rhs, rest) {
        super();
        this.lhs = lhs;
        this.rhs = rhs;
        this.rest = rest;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Assign", ["lhs", "rhs", "rest"]]; 
    });
    this.Define = function Define(defn, rest) {
      return globalThis.Object.freeze(new Define.class(defn, rest));
    };
    (class Define extends Block.Block {
      static {
        Block.Define.class = this
      }
      constructor(defn, rest) {
        super();
        this.defn = defn;
        this.rest = rest;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Define", ["defn", "rest"]]; 
    });
    this.Scoped = function Scoped(symbols, rest) {
      return globalThis.Object.freeze(new Scoped.class(symbols, rest));
    };
    (class Scoped extends Block.Block {
      static {
        Block.Scoped.class = this
      }
      constructor(symbols, rest) {
        super();
        this.symbols = symbols;
        this.rest = rest;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Scoped", ["symbols", "rest"]]; 
    });
    this.End = function End() {
      return globalThis.Object.freeze(new End.class());
    };
    (class End extends Block.Block {
      static {
        Block.End.class = this
      }
      constructor() {
        super();
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "End", []]; 
    });
  }
  static isPrimitiveType(sym) {
    let scrut;
    scrut = sym.name;
    switch (scrut) {
      case "Str":
        return true;
        break;
      case "Int":
        return true;
        break;
      case "Num":
        return true;
        break;
      case "Bool":
        return true;
        break;
      default:
        return false;
        break;
    }
  } 
  static isPrimitiveTypeOf(sym, l) {
    let scrut, l1, i, n, b, element1$, element0$;
    scrut = globalThis.Object.freeze([
      sym.name,
      l
    ]);
    if (runtime.Tuple.isArrayLike(scrut) && scrut.length === 2) {
      element0$ = runtime.Tuple.get(scrut, 0);
      element1$ = runtime.Tuple.get(scrut, 1);
      switch (element0$) {
        case "Str":
          l1 = element1$;
          if (typeof l1 === 'string') {
            return true
          } else {
            return false
          }
          break;
        case "Int":
          i = element1$;
          if (globalThis.Number.isInteger(i)) {
            return true
          } else {
            return false
          }
          break;
        case "Num":
          n = element1$;
          if (typeof n === 'number') {
            return true
          } else {
            return false
          }
          break;
        case "Bool":
          b = element1$;
          if (typeof b === 'boolean') {
            return true
          } else {
            return false
          }
          break;
        default:
          return false;
          break;
      }
    } else {
      return false
    }
  } 
  static checkMap(symbolMap, key, value) {
    let v, tmp;
    v = runtime.safeCall(symbolMap.get(key));
    if (v instanceof Block.Symbol.class) {
      return v
    } else {
      tmp = symbolMap.set(key, value);
      return (tmp , value)
    }
  } 
  static concat(b1, b2) {
    let scrut, rest, dflt, arms, rhs, rest1, lhs, defn, rest2, rest3, symbols, arg$Scoped$0$, arg$Scoped$1$, arg$Define$0$, arg$Define$1$, arg$Assign$0$, arg$Assign$1$, arg$Assign$2$, arg$Match$0$, arg$Match$1$, arg$Match$2$, arg$Match$3$, tmp, tmp1, tmp2, tmp3;
    if (b1 instanceof Block.Match.class) {
      arg$Match$0$ = b1.scrut;
      arg$Match$1$ = b1.arms;
      arg$Match$2$ = b1.dflt;
      arg$Match$3$ = b1.rest;
      rest = arg$Match$3$;
      dflt = arg$Match$2$;
      arms = arg$Match$1$;
      scrut = arg$Match$0$;
      tmp = Block.concat(rest, b2);
      return Block.Match(scrut, arms, dflt, tmp)
    } else if (b1 instanceof Block.Return.class) {
      b1.res;
      b1.implct;
      return b1
    } else if (b1 instanceof Block.Assign.class) {
      arg$Assign$0$ = b1.lhs;
      arg$Assign$1$ = b1.rhs;
      arg$Assign$2$ = b1.rest;
      rest1 = arg$Assign$2$;
      rhs = arg$Assign$1$;
      lhs = arg$Assign$0$;
      tmp1 = Block.concat(rest1, b2);
      return Block.Assign(lhs, rhs, tmp1)
    } else if (b1 instanceof Block.Define.class) {
      arg$Define$0$ = b1.defn;
      arg$Define$1$ = b1.rest;
      rest2 = arg$Define$1$;
      defn = arg$Define$0$;
      tmp2 = Block.concat(rest2, b2);
      return Block.Define(defn, tmp2)
    } else if (b1 instanceof Block.Scoped.class) {
      arg$Scoped$0$ = b1.symbols;
      arg$Scoped$1$ = b1.rest;
      rest3 = arg$Scoped$1$;
      symbols = arg$Scoped$0$;
      tmp3 = Block.concat(rest3, b2);
      return Block.Scoped(symbols, tmp3)
    } else if (b1 instanceof Block.End.class) {
      return b2
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static indent(s) {
    return s.replaceAll("
", "
  ")
  } 
  static showLiteral(l) {
    let tmp, tmp1;
    if (l === undefined) {
      return "undefined"
    } else if (l === null) {
      return "null"
    } else if (typeof l === 'string') {
      tmp = runtime.safeCall(l.toString());
      tmp1 = "\"" + tmp;
      return tmp1 + "\""
    } else {
      return runtime.safeCall(l.toString())
    }
  } 
  static showSymbol(s) {
    let scrut;
    scrut = s.name;
    if (scrut === "runtime") {
      return "Runtime"
    } else {
      return s.name.replaceAll("$", "_")
    }
  } 
  static showPath(p) {
    let name, qual, qual1, fld, l, lit, arg$ValueLit$0$, arg$ValueRef$0$, arg$DynSelect$0$, arg$DynSelect$1$, arg$Select$0$, arg$Select$1$, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6;
    if (p instanceof Block.Select.class) {
      arg$Select$0$ = p.qual;
      arg$Select$1$ = p.name;
      name = arg$Select$1$;
      qual = arg$Select$0$;
      tmp = Block.showPath(qual);
      tmp1 = tmp + ".";
      tmp2 = Block.showSymbol(name);
      return tmp1 + tmp2
    } else if (p instanceof Block.DynSelect.class) {
      arg$DynSelect$0$ = p.qual;
      arg$DynSelect$1$ = p.fld;
      p.arrayIdx;
      fld = arg$DynSelect$1$;
      qual1 = arg$DynSelect$0$;
      tmp3 = Block.showPath(qual1);
      tmp4 = tmp3 + ".(";
      tmp5 = Block.showPath(fld);
      tmp6 = tmp4 + tmp5;
      return tmp6 + ")"
    } else if (p instanceof Block.ValueRef.class) {
      arg$ValueRef$0$ = p.l;
      l = arg$ValueRef$0$;
      return Block.showSymbol(l)
    } else if (p instanceof Block.ValueLit.class) {
      arg$ValueLit$0$ = p.lit;
      lit = arg$ValueLit$0$;
      return Block.showLiteral(lit)
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static showArg(arg) {
    return Block.showPath(arg.value)
  } 
  static showArgs(args) {
    let tmp;
    tmp = runtime.safeCall(args.map(Block.showArg));
    return runtime.safeCall(tmp.join(", "))
  } 
  static showResult(r) {
    let fun_, args, rhs, lhs, cls, args1, elems, arg$Tuple$0$, arg$Instantiate$0$, arg$Instantiate$1$, arg$Call$0$, arg$Call$1$, element1$, element0$, arg$ValueRef$0$, arg$Symbol$0$, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13, tmp14, tmp15, tmp16, tmp17, tmp18;
    split_root$: {
      split_1$: {
        if (r instanceof Block.Path) {
          tmp = Block.showPath(r);
          break split_root$
        } else if (r instanceof Block.Call.class) {
          arg$Call$0$ = r._fun;
          arg$Call$1$ = r.args;
          args = arg$Call$1$;
          fun_ = arg$Call$0$;
          if (runtime.Tuple.isArrayLike(args) && args.length === 2) {
            element0$ = runtime.Tuple.get(args, 0);
            element1$ = runtime.Tuple.get(args, 1);
            rhs = element1$;
            lhs = element0$;
            if (fun_ instanceof Block.ValueRef.class) {
              arg$ValueRef$0$ = fun_.l;
              if (arg$ValueRef$0$ instanceof Block.Symbol.class) {
                arg$Symbol$0$ = arg$ValueRef$0$.name;
                switch (arg$Symbol$0$) {
                  case "+":
                    tmp1 = Block.showArg(lhs);
                    tmp2 = tmp1 + "+";
                    tmp3 = Block.showArg(rhs);
                    tmp = tmp2 + tmp3;
                    break split_root$;
                    break;
                  case "-":
                    tmp4 = Block.showArg(lhs);
                    tmp5 = tmp4 + "-";
                    tmp6 = Block.showArg(rhs);
                    tmp = tmp5 + tmp6;
                    break split_root$;
                    break;
                  default:
                    break split_1$;
                    break;
                }
              } else {
                break split_1$
              }
            } else {
              break split_1$
            }
          } else {
            break split_1$
          }
        } else if (r instanceof Block.Instantiate.class) {
          arg$Instantiate$0$ = r.cls;
          arg$Instantiate$1$ = r.args;
          args1 = arg$Instantiate$1$;
          cls = arg$Instantiate$0$;
          tmp7 = Block.showPath(cls);
          tmp8 = "new " + tmp7;
          tmp9 = tmp8 + "(";
          tmp10 = Block.showArgs(args1);
          tmp11 = tmp9 + tmp10;
          tmp = tmp11 + ")";
          break split_root$
        } else if (r instanceof Block.Tuple.class) {
          arg$Tuple$0$ = r.elems;
          elems = arg$Tuple$0$;
          tmp12 = Block.showArgs(elems);
          tmp13 = "[" + tmp12;
          tmp = tmp13 + "]";
          break split_root$
        } else {
          tmp14 = StrOps.concat2("<unknown result:", r);
          tmp = StrOps.concat2(tmp14, ">");
          break split_root$
        }
      }
      tmp15 = Block.showPath(fun_);
      tmp16 = tmp15 + "(";
      tmp17 = Block.showArgs(args);
      tmp18 = tmp16 + tmp17;
      tmp = tmp18 + ")";
    }
    return tmp
  } 
  static showCase(c) {
    let l, cls, scrut, len, arg$Tup$0$, arg$Cls$0$, arg$Lit$0$, tmp, tmp1, tmp2, tmp3, tmp4, tmp5;
    if (c instanceof Block.Lit.class) {
      arg$Lit$0$ = c.lit;
      l = arg$Lit$0$;
      return Block.showLiteral(l)
    } else if (c instanceof Block.Cls.class) {
      arg$Cls$0$ = c.cls;
      c.path;
      cls = arg$Cls$0$;
      tmp = Block.showSymbol(cls);
      scrut = Block.isPrimitiveType(cls);
      if (scrut === true) {
        tmp1 = "";
      } else {
        tmp1 = Block.showParamsOpt(cls.paramsOpt);
      }
      return tmp + tmp1
    } else if (c instanceof Block.Tup.class) {
      arg$Tup$0$ = c.len;
      len = arg$Tup$0$;
      tmp2 = runtime.safeCall(globalThis.Array(len));
      tmp3 = runtime.safeCall(tmp2.fill("_"));
      tmp4 = runtime.safeCall(tmp3.join(", "));
      tmp5 = "[" + tmp4;
      return tmp5 + "]"
    } else {
      return "<unknown case>"
    }
  } 
  static showArm(a) {
    let scrut, tmp, tmp1, tmp2, tmp3, tmp4, tmp5;
    tmp = Block.showCase(a.cse);
    tmp1 = tmp + " then";
    scrut = a.body;
    if (scrut instanceof Block.Return.class) {
      tmp2 = " ";
    } else {
      tmp2 = "
  ";
    }
    tmp3 = tmp1 + tmp2;
    tmp4 = Block.showBlock(a.body);
    tmp5 = Block.indent(tmp4);
    return tmp3 + tmp5
  } 
  static showParams(p) {
    let lambda, tmp, tmp1, tmp2;
    lambda = (undefined, function (_0) {
      return Block.showSymbol(_0)
    });
    tmp = runtime.safeCall(p.map(lambda));
    tmp1 = runtime.safeCall(tmp.join(", "));
    tmp2 = "(" + tmp1;
    return tmp2 + ")"
  } 
  static showParamsOpt(p) {
    let s, arg$Some$0$;
    if (p instanceof Option.Some.class) {
      arg$Some$0$ = p.value;
      s = arg$Some$0$;
      return Block.showParams(s)
    } else if (p instanceof Option.None.class) {
      return ""
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static showParamList(ps) {
    let tmp;
    tmp = runtime.safeCall(ps.map(Block.showParams));
    return runtime.safeCall(tmp.join(""))
  } 
  static showDefn(d) {
    let body, sym, ps, methods, sym1, rhs, sym2, owner, owner1, arg$ValDefn$0$, arg$ValDefn$1$, arg$ValDefn$2$, arg$ClsLikeDefn$0$, arg$ClsLikeDefn$1$, arg$FunDefn$0$, arg$FunDefn$1$, arg$FunDefn$2$, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13, tmp14, tmp15, tmp16, tmp17, tmp18, tmp19, arg$Some$0$, tmp20, tmp21, tmp22, tmp23, tmp24, tmp25, tmp26, tmp27;
    if (d instanceof Block.FunDefn.class) {
      arg$FunDefn$0$ = d.sym;
      arg$FunDefn$1$ = d.params;
      arg$FunDefn$2$ = d.body;
      body = arg$FunDefn$2$;
      ps = arg$FunDefn$1$;
      sym = arg$FunDefn$0$;
      tmp = Block.showSymbol(sym);
      tmp1 = "fun " + tmp;
      tmp2 = Block.showParamList(ps);
      tmp3 = tmp1 + tmp2;
      tmp4 = tmp3 + " =";
      if (body instanceof Block.Return.class) {
        tmp5 = " ";
      } else {
        tmp5 = "
  ";
      }
      tmp6 = tmp4 + tmp5;
      tmp7 = Block.showBlock(body);
      tmp8 = Block.indent(tmp7);
      return tmp6 + tmp8
    } else if (d instanceof Block.ClsLikeDefn.class) {
      arg$ClsLikeDefn$0$ = d.sym;
      arg$ClsLikeDefn$1$ = d.methods;
      d.companion;
      methods = arg$ClsLikeDefn$1$;
      sym1 = arg$ClsLikeDefn$0$;
      tmp9 = Block.showSymbol(sym1);
      tmp10 = "class " + tmp9;
      tmp11 = Block.showParamsOpt(sym1.paramsOpt);
      tmp12 = tmp10 + tmp11;
      tmp13 = runtime.safeCall(sym1.auxParams.map(Block.showParams));
      tmp14 = runtime.safeCall(tmp13.join(""));
      tmp15 = tmp12 + tmp14;
      if (runtime.Tuple.isArrayLike(methods) && methods.length === 0) {
        tmp16 = "";
      } else {
        tmp17 = runtime.safeCall(methods.map(Block.showDefn));
        tmp18 = runtime.safeCall(tmp17.join("
"));
        tmp19 = Block.indent(tmp18);
        tmp16 = " with 
  " + tmp19;
      }
      return tmp15 + tmp16
    } else if (d instanceof Block.ValDefn.class) {
      arg$ValDefn$0$ = d.tsym;
      arg$ValDefn$1$ = d.sym;
      arg$ValDefn$2$ = d.rhs;
      rhs = arg$ValDefn$2$;
      sym2 = arg$ValDefn$1$;
      owner = arg$ValDefn$0$;
      if (owner instanceof Option.Some.class) {
        arg$Some$0$ = owner.value;
        owner1 = arg$Some$0$;
        tmp20 = Block.showSymbol(owner1);
      } else {
        tmp20 = "";
      }
      tmp21 = tmp20 + ".";
      tmp22 = Block.showSymbol(sym2);
      tmp23 = tmp21 + tmp22;
      tmp24 = tmp23 + " = ";
      tmp25 = Block.showPath(rhs);
      return tmp24 + tmp25
    } else {
      tmp26 = runtime.safeCall(d.toString());
      tmp27 = "<unknown defn: " + tmp26;
      return tmp27 + " >"
    }
  } 
  static showBlock(b) {
    let rhs, rest, lhs, rest1, d, res, scrut, rest2, dflt, arms, db, rest3, symbols, arg$Scoped$0$, arg$Scoped$1$, arg$Match$0$, arg$Match$1$, arg$Match$2$, arg$Match$3$, arg$Return$0$, arg$Define$0$, arg$Define$1$, arg$Assign$0$, arg$Assign$1$, arg$Assign$2$, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13, tmp14, arg$Some$0$, tmp15, tmp16, tmp17, tmp18, tmp19, tmp20, tmp21, tmp22, lambda, tmp23, tmp24, tmp25, tmp26;
    if (b instanceof Block.Assign.class) {
      arg$Assign$0$ = b.lhs;
      arg$Assign$1$ = b.rhs;
      arg$Assign$2$ = b.rest;
      rest = arg$Assign$2$;
      rhs = arg$Assign$1$;
      lhs = arg$Assign$0$;
      tmp = Block.showSymbol(lhs);
      tmp1 = tmp + " = ";
      tmp2 = Block.showResult(rhs);
      tmp3 = tmp1 + tmp2;
      tmp4 = Block.showRestBlock(rest);
      return tmp3 + tmp4
    } else if (b instanceof Block.Define.class) {
      arg$Define$0$ = b.defn;
      arg$Define$1$ = b.rest;
      rest1 = arg$Define$1$;
      d = arg$Define$0$;
      tmp5 = Block.showDefn(d);
      tmp6 = Block.showRestBlock(rest1);
      return tmp5 + tmp6
    } else if (b instanceof Block.Return.class) {
      arg$Return$0$ = b.res;
      b.implct;
      res = arg$Return$0$;
      return Block.showResult(res)
    } else if (b instanceof Block.Match.class) {
      arg$Match$0$ = b.scrut;
      arg$Match$1$ = b.arms;
      arg$Match$2$ = b.dflt;
      arg$Match$3$ = b.rest;
      rest2 = arg$Match$3$;
      dflt = arg$Match$2$;
      arms = arg$Match$1$;
      scrut = arg$Match$0$;
      tmp7 = Block.showPath(scrut);
      tmp8 = "if " + tmp7;
      tmp9 = tmp8 + " is";
      tmp10 = runtime.safeCall(arms.map(Block.showArm));
      tmp11 = runtime.safeCall(tmp10.join("
"));
      tmp12 = "
" + tmp11;
      tmp13 = Block.indent(tmp12);
      tmp14 = tmp9 + tmp13;
      if (dflt instanceof Option.Some.class) {
        arg$Some$0$ = dflt.value;
        db = arg$Some$0$;
        tmp15 = Block.showBlock(db);
        tmp16 = "
" + tmp15;
        tmp17 = Block.indent(tmp16);
        tmp18 = "
else" + tmp17;
      } else {
        tmp18 = "";
      }
      tmp19 = Block.indent(tmp18);
      tmp20 = tmp14 + tmp19;
      tmp21 = Block.showRestBlock(rest2);
      return tmp20 + tmp21
    } else if (b instanceof Block.Scoped.class) {
      arg$Scoped$0$ = b.symbols;
      arg$Scoped$1$ = b.rest;
      rest3 = arg$Scoped$1$;
      symbols = arg$Scoped$0$;
      tmp22 = runtime.safeCall(symbols.map(Block.showSymbol));
      lambda = (undefined, function (_0) {
        return "let " + _0
      });
      tmp23 = runtime.safeCall(tmp22.map(lambda));
      tmp24 = runtime.safeCall(tmp23.join("
"));
      tmp25 = Block.showRestBlock(rest3);
      return tmp24 + tmp25
    } else if (b instanceof Block.End.class) {
      return "()"
    } else {
      tmp26 = StrOps.concat2("<unknown block:", b);
      return StrOps.concat2(tmp26, ">")
    }
  } 
  static showRestBlock(b) {
    let tmp;
    if (b instanceof Block.End.class) {
      return ""
    } else {
      tmp = Block.showBlock(b);
      return "
" + tmp
    }
  } 
  static show(x) {
    if (x instanceof Block.Symbol.class) {
      return Block.showSymbol(x)
    } else if (x instanceof Block.Path) {
      return Block.showPath(x)
    } else if (x instanceof Block.Result) {
      return Block.showResult(x)
    } else if (x instanceof Block.Case) {
      return Block.showCase(x)
    } else if (x instanceof Block.Defn) {
      return Block.showDefn(x)
    } else if (x instanceof Block.Block) {
      return Block.showBlock(x)
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static printCode(x) {
    let tmp;
    tmp = Block.show(x);
    return Predef.print(tmp)
  } 
  static printModule(name, methods) {
    let tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6;
    tmp = "module " + name;
    tmp1 = tmp + " with";
    tmp2 = runtime.safeCall(methods.map(Block.showDefn));
    tmp3 = runtime.safeCall(tmp2.join("
"));
    tmp4 = "
" + tmp3;
    tmp5 = Block.indent(tmp4);
    tmp6 = tmp1 + tmp5;
    return Predef.print(tmp6)
  } 
  static genMod(name, methods) {
    let tmp, tmp1, tmp2, tmp3, tmp4, tmp5;
    tmp = "module " + name;
    tmp1 = tmp + " with";
    tmp2 = runtime.safeCall(methods.map(Block.showDefn));
    tmp3 = runtime.safeCall(tmp2.join("
"));
    tmp4 = "
" + tmp3;
    tmp5 = Block.indent(tmp4);
    return tmp1 + tmp5
  } 
  static codegen(name, methods, file) {
    return runtime.Unit
  }
  toString() { return runtime.render(this); }
  static [definitionMetadata] = ["class", "Block"]; 
});
let Block = Block2; export default Block;
"""))

  fileNameSourceMap += "Shape.mls" -> ("""
import "./Block.mls"
import "./Option.mls"
import "./CachedHash.mls"

open Block { Literal, ClassSymbol, showSymbol, isPrimitiveType, isPrimitiveTypeOf }
open Option

type Shape = Shape.Shape

module Shape with...

class Shape extends CachedHash with
  constructor
    Dyn()
    Lit(val l: Literal)
    Arr(val shapes: Array[Shape])
    Class(val sym: ClassSymbol, val params: Array[Shape])

fun show(s: Shape) =
  if s is
    Dyn then "Dyn"
    Lit(lit) then "Lit(" + Block.showLiteral(lit) + ")"
    Arr(shapes) then "Arr(" + shapes.map(show).join(", ") + ")"
    Class(sym, params) then "Class(" + showSymbol(sym) + ", [" + params.map(show).join(", ") + "])"

fun sel(s1: Shape, s2: Shape): Array[Shape] =
  if [s1, s2] is
    [Class(ClassSymbol(_, _, paramsOpt, auxParams), params), Lit(n)] and n is Str
      and paramsOpt is Some(paramsSymb)
      and paramsSymb.indexOf(n) is
        -1 then []
        i then [params.(i)]
    [Class(ClassSymbol, p), Dyn] then [Dyn()]
    [Dyn, Lit(n)] and n is Str
      then [Dyn()]
    [Arr(shapes), Lit(n)] and n is Int
      then [shapes.(n)]
    [Arr(shapes), Dyn] then
      shapes
    [Dyn, Lit(n)] and n is Int
      then [Dyn()]
    [Dyn, Dyn]
      then [Dyn()]
    else throw Error("sel error")

fun static(s: Shape) =
  if s is
    Dyn then false
    Lit(l) then not (l is Str and isPrimitiveType(l)) // redundant bracket?
    Class(_, params) then params.every(static)
    Arr(shapes) then shapes.every(static)

open Block { Case }

fun silh(p: Case): Shape = if p is
  Block.Lit(l) then Lit(l)
  Block.Cls(sym, path) then
    val size = if sym.args is Some(i) then i else 0
    Class(sym, Array(size).fill(Dyn))
  Block.Tup(n) then Arr(Array(n).fill(Dyn))

// TODO: use Option instead, since all of them return at most one shape
fun filter(s: Shape, p: Case): Array[Shape] =
  if [s, p] is
    [Lit(l1), Block.Lit(l2)] and l1 == l2 then [s]
    [Lit(l), Block.Cls(c, _)] and isPrimitiveTypeOf(c, l) then [s]
    [Arr(ls), Block.Tup(n)] and ls.length == n then [s]
    [Class(c1, _), Block.Cls(c2, _)] and c1.name == c2.name then [s]
    [Dyn, _] then [silh(p)]
    else []

fun rest(s: Shape, p: Case): Array[Shape] =
  if [s, p] is
    [Lit(l1), Block.Lit(l2)] and l1 == l2 then []
    [Lit(l), Block.Cls(c, _)] and isPrimitiveTypeOf(c, l) then []
    [Arr(ls), Block.Tup(n)] and ls.length == n then []
    [Class(c1, _), Block.Cls(c2, _)] and c1.name == c2.name then []
    [Dyn, _] then [s]
    else [s]
""" -> S("""
const definitionMetadata = globalThis.Symbol.for("mlscript.definitionMetadata");
const prettyPrint = globalThis.Symbol.for("mlscript.prettyPrint");
import runtime from "./Runtime.mjs";
import Block from "./Block.mjs";
import Option from "./Option.mjs";
import CachedHash from "./CachedHash.mjs";
let Shape2;
(class Shape {
  static {
    Shape2 = this
  }
  constructor() {
    runtime.Unit;
  }
  static {
    (class Shape1 extends CachedHash {
      static {
        Shape.Shape = this
      }
      constructor() {
        super();
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Shape"]; 
    });
    this.Dyn = function Dyn() {
      return globalThis.Object.freeze(new Dyn.class());
    };
    (class Dyn extends Shape.Shape {
      static {
        Shape.Dyn.class = this
      }
      constructor() {
        super();
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Dyn", []]; 
    });
    this.Lit = function Lit(l) {
      return globalThis.Object.freeze(new Lit.class(l));
    };
    (class Lit extends Shape.Shape {
      static {
        Shape.Lit.class = this
      }
      constructor(l) {
        super();
        this.l = l;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Lit", ["l"]]; 
    });
    this.Arr = function Arr(shapes) {
      return globalThis.Object.freeze(new Arr.class(shapes));
    };
    (class Arr extends Shape.Shape {
      static {
        Shape.Arr.class = this
      }
      constructor(shapes) {
        super();
        this.shapes = shapes;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Arr", ["shapes"]]; 
    });
    this.Class = function Class(sym, params) {
      return globalThis.Object.freeze(new Class.class(sym, params));
    };
    (class Class extends Shape.Shape {
      static {
        Shape.Class.class = this
      }
      constructor(sym, params) {
        super();
        this.sym = sym;
        this.params = params;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Class", ["sym", "params"]]; 
    });
  }
  static show(s) {
    let lit, shapes, params, sym, arg$Class$0$, arg$Class$1$, arg$Arr$0$, arg$Lit$0$, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7, tmp8, tmp9, tmp10;
    if (s instanceof Shape.Dyn.class) {
      return "Dyn"
    } else if (s instanceof Shape.Lit.class) {
      arg$Lit$0$ = s.l;
      lit = arg$Lit$0$;
      tmp = Block.showLiteral(lit);
      tmp1 = "Lit(" + tmp;
      return tmp1 + ")"
    } else if (s instanceof Shape.Arr.class) {
      arg$Arr$0$ = s.shapes;
      shapes = arg$Arr$0$;
      tmp2 = runtime.safeCall(shapes.map(Shape.show));
      tmp3 = runtime.safeCall(tmp2.join(", "));
      tmp4 = "Arr(" + tmp3;
      return tmp4 + ")"
    } else if (s instanceof Shape.Class.class) {
      arg$Class$0$ = s.sym;
      arg$Class$1$ = s.params;
      params = arg$Class$1$;
      sym = arg$Class$0$;
      tmp5 = Block.showSymbol(sym);
      tmp6 = "Class(" + tmp5;
      tmp7 = tmp6 + ", [";
      tmp8 = runtime.safeCall(params.map(Shape.show));
      tmp9 = runtime.safeCall(tmp8.join(", "));
      tmp10 = tmp7 + tmp9;
      return tmp10 + "])"
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static sel(s1, s2) {
    let scrut, n, params, paramsOpt, paramsSymb, scrut1, i, n1, n2, shapes, shapes1, n3, element1$, element0$, arg$Lit$0$, arg$Arr$0$, arg$Class$0$, arg$Class$1$, arg$ClassSymbol$2$, arg$Some$0$, tmp, tmp1;
    split_root$: {
      split_1$: {
        split_2$: {
          scrut = globalThis.Object.freeze([
            s1,
            s2
          ]);
          if (runtime.Tuple.isArrayLike(scrut) && scrut.length === 2) {
            element0$ = runtime.Tuple.get(scrut, 0);
            element1$ = runtime.Tuple.get(scrut, 1);
            if (element0$ instanceof Shape.Class.class) {
              arg$Class$0$ = element0$.sym;
              arg$Class$1$ = element0$.params;
              if (arg$Class$0$ instanceof Block.ClassSymbol.class) {
                arg$Class$0$.name;
                arg$Class$0$.value;
                arg$ClassSymbol$2$ = arg$Class$0$.paramsOpt;
                arg$Class$0$.auxParams;
                if (element1$ instanceof Shape.Lit.class) {
                  arg$Lit$0$ = element1$.l;
                  n = arg$Lit$0$;
                  params = arg$Class$1$;
                  paramsOpt = arg$ClassSymbol$2$;
                  if (typeof n === 'string') {
                    if (paramsOpt instanceof Option.Some.class) {
                      arg$Some$0$ = paramsOpt.value;
                      paramsSymb = arg$Some$0$;
                      scrut1 = runtime.safeCall(paramsSymb.indexOf(n));
                      if (scrut1 === -1) {
                        tmp = globalThis.Object.freeze([]);
                        break split_root$
                      } else {
                        i = scrut1;
                        tmp = globalThis.Object.freeze([
                          params[i]
                        ]);
                        break split_root$
                      }
                    } else {
                      break split_1$
                    }
                  } else {
                    break split_1$
                  }
                } else if (element1$ instanceof Shape.Dyn.class) {
                  break split_2$
                } else {
                  break split_1$
                }
              } else {
                break split_1$
              }
            } else if (element0$ instanceof Shape.Dyn.class) {
              if (element1$ instanceof Shape.Lit.class) {
                arg$Lit$0$ = element1$.l;
                n1 = arg$Lit$0$;
                if (typeof n1 === 'string') {
                  break split_2$
                } else {
                  n3 = arg$Lit$0$;
                  if (globalThis.Number.isInteger(n3)) {
                    break split_2$
                  } else {
                    break split_1$
                  }
                }
              } else if (element1$ instanceof Shape.Dyn.class) {
                break split_2$
              } else {
                break split_1$
              }
            } else if (element0$ instanceof Shape.Arr.class) {
              arg$Arr$0$ = element0$.shapes;
              if (element1$ instanceof Shape.Lit.class) {
                arg$Lit$0$ = element1$.l;
                n2 = arg$Lit$0$;
                shapes = arg$Arr$0$;
                if (globalThis.Number.isInteger(n2)) {
                  tmp = globalThis.Object.freeze([
                    shapes[n2]
                  ]);
                  break split_root$
                } else {
                  break split_1$
                }
              } else if (element1$ instanceof Shape.Dyn.class) {
                shapes1 = arg$Arr$0$;
                tmp = shapes1;
                break split_root$
              } else {
                break split_1$
              }
            } else {
              break split_1$
            }
          } else {
            break split_1$
          }
        }
        tmp1 = Shape.Dyn();
        tmp = globalThis.Object.freeze([
          tmp1
        ]);
        break split_root$;
      }
      throw runtime.safeCall(globalThis.Error("sel error"));
    }
    return tmp
  } 
  static static(s) {
    let l, scrut, params, shapes, arg$Arr$0$, arg$Class$1$, arg$Lit$0$, tmp;
    if (s instanceof Shape.Dyn.class) {
      return false
    } else if (s instanceof Shape.Lit.class) {
      arg$Lit$0$ = s.l;
      l = arg$Lit$0$;
      if (typeof l === 'string') {
        scrut = Block.isPrimitiveType(l);
        if (scrut === true) {
          tmp = true;
        } else {
          tmp = false;
        }
      } else {
        tmp = false;
      }
      return ! tmp
    } else if (s instanceof Shape.Class.class) {
      s.sym;
      arg$Class$1$ = s.params;
      params = arg$Class$1$;
      return runtime.safeCall(params.every(Shape.static))
    } else if (s instanceof Shape.Arr.class) {
      arg$Arr$0$ = s.shapes;
      shapes = arg$Arr$0$;
      return runtime.safeCall(shapes.every(Shape.static))
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static silh(p) {
    let size, l, sym, scrut, i, n, arg$Tup$0$, arg$Cls$0$, arg$Lit$0$, arg$Some$0$, tmp, tmp1, tmp2, tmp3, tmp4;
    if (p instanceof Block.Lit.class) {
      arg$Lit$0$ = p.lit;
      l = arg$Lit$0$;
      return Shape.Lit(l)
    } else if (p instanceof Block.Cls.class) {
      arg$Cls$0$ = p.cls;
      p.path;
      sym = arg$Cls$0$;
      scrut = sym.args;
      if (scrut instanceof Option.Some.class) {
        arg$Some$0$ = scrut.value;
        i = arg$Some$0$;
        tmp = i;
      } else {
        tmp = 0;
      }
      size = tmp;
      tmp1 = runtime.safeCall(globalThis.Array(size));
      tmp2 = runtime.safeCall(tmp1.fill(Shape.Dyn));
      return Shape.Class(sym, tmp2)
    } else if (p instanceof Block.Tup.class) {
      arg$Tup$0$ = p.len;
      n = arg$Tup$0$;
      tmp3 = runtime.safeCall(globalThis.Array(n));
      tmp4 = runtime.safeCall(tmp3.fill(Shape.Dyn));
      return Shape.Arr(tmp4)
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static filter(s, p) {
    let scrut, l1, l2, scrut1, l, c, scrut2, n, ls, scrut3, c2, c1, scrut4, element1$, element0$, arg$Class$0$, arg$Cls$0$, arg$Arr$0$, arg$Tup$0$, arg$Lit$0$, arg$Lit$0$1, tmp;
    scrut = globalThis.Object.freeze([
      s,
      p
    ]);
    if (runtime.Tuple.isArrayLike(scrut) && scrut.length === 2) {
      element0$ = runtime.Tuple.get(scrut, 0);
      element1$ = runtime.Tuple.get(scrut, 1);
      if (element0$ instanceof Shape.Lit.class) {
        arg$Lit$0$ = element0$.l;
        if (element1$ instanceof Block.Lit.class) {
          arg$Lit$0$1 = element1$.lit;
          l2 = arg$Lit$0$1;
          l1 = arg$Lit$0$;
          scrut1 = l1 == l2;
          if (scrut1 === true) {
            return globalThis.Object.freeze([
              s
            ])
          } else {
            return globalThis.Object.freeze([])
          }
        } else if (element1$ instanceof Block.Cls.class) {
          arg$Cls$0$ = element1$.cls;
          element1$.path;
          c = arg$Cls$0$;
          l = arg$Lit$0$;
          scrut2 = Block.isPrimitiveTypeOf(c, l);
          if (scrut2 === true) {
            return globalThis.Object.freeze([
              s
            ])
          } else {
            return globalThis.Object.freeze([])
          }
        } else {
          return globalThis.Object.freeze([])
        }
      } else if (element0$ instanceof Shape.Arr.class) {
        arg$Arr$0$ = element0$.shapes;
        if (element1$ instanceof Block.Tup.class) {
          arg$Tup$0$ = element1$.len;
          n = arg$Tup$0$;
          ls = arg$Arr$0$;
          scrut3 = ls.length == n;
          if (scrut3 === true) {
            return globalThis.Object.freeze([
              s
            ])
          } else {
            return globalThis.Object.freeze([])
          }
        } else {
          return globalThis.Object.freeze([])
        }
      } else if (element0$ instanceof Shape.Class.class) {
        arg$Class$0$ = element0$.sym;
        element0$.params;
        if (element1$ instanceof Block.Cls.class) {
          arg$Cls$0$ = element1$.cls;
          element1$.path;
          c2 = arg$Cls$0$;
          c1 = arg$Class$0$;
          scrut4 = c1.name == c2.name;
          if (scrut4 === true) {
            return globalThis.Object.freeze([
              s
            ])
          } else {
            return globalThis.Object.freeze([])
          }
        } else {
          return globalThis.Object.freeze([])
        }
      } else if (element0$ instanceof Shape.Dyn.class) {
        tmp = Shape.silh(p);
        return globalThis.Object.freeze([
          tmp
        ])
      } else {
        return globalThis.Object.freeze([])
      }
    } else {
      return globalThis.Object.freeze([])
    }
  } 
  static rest(s, p) {
    let scrut, l1, l2, scrut1, l, c, scrut2, n, ls, scrut3, c2, c1, scrut4, element1$, element0$, arg$Class$0$, arg$Cls$0$, arg$Arr$0$, arg$Tup$0$, arg$Lit$0$, arg$Lit$0$1, tmp;
    split_root$: {
      split_1$: {
        scrut = globalThis.Object.freeze([
          s,
          p
        ]);
        if (runtime.Tuple.isArrayLike(scrut) && scrut.length === 2) {
          element0$ = runtime.Tuple.get(scrut, 0);
          element1$ = runtime.Tuple.get(scrut, 1);
          if (element0$ instanceof Shape.Lit.class) {
            arg$Lit$0$ = element0$.l;
            if (element1$ instanceof Block.Lit.class) {
              arg$Lit$0$1 = element1$.lit;
              l2 = arg$Lit$0$1;
              l1 = arg$Lit$0$;
              scrut1 = l1 == l2;
              if (scrut1 === true) {
                tmp = globalThis.Object.freeze([]);
                break split_root$
              } else {
                break split_1$
              }
            } else if (element1$ instanceof Block.Cls.class) {
              arg$Cls$0$ = element1$.cls;
              element1$.path;
              c = arg$Cls$0$;
              l = arg$Lit$0$;
              scrut2 = Block.isPrimitiveTypeOf(c, l);
              if (scrut2 === true) {
                tmp = globalThis.Object.freeze([]);
                break split_root$
              } else {
                break split_1$
              }
            } else {
              break split_1$
            }
          } else if (element0$ instanceof Shape.Arr.class) {
            arg$Arr$0$ = element0$.shapes;
            if (element1$ instanceof Block.Tup.class) {
              arg$Tup$0$ = element1$.len;
              n = arg$Tup$0$;
              ls = arg$Arr$0$;
              scrut3 = ls.length == n;
              if (scrut3 === true) {
                tmp = globalThis.Object.freeze([]);
                break split_root$
              } else {
                break split_1$
              }
            } else {
              break split_1$
            }
          } else if (element0$ instanceof Shape.Class.class) {
            arg$Class$0$ = element0$.sym;
            element0$.params;
            if (element1$ instanceof Block.Cls.class) {
              arg$Cls$0$ = element1$.cls;
              element1$.path;
              c2 = arg$Cls$0$;
              c1 = arg$Class$0$;
              scrut4 = c1.name == c2.name;
              if (scrut4 === true) {
                tmp = globalThis.Object.freeze([]);
                break split_root$
              } else {
                break split_1$
              }
            } else {
              break split_1$
            }
          } else if (element0$ instanceof Shape.Dyn.class) {
            break split_1$
          } else {
            break split_1$
          }
        } else {
          break split_1$
        }
      }
      tmp = globalThis.Object.freeze([
        s
      ]);
    }
    return tmp
  }
  toString() { return runtime.render(this); }
  static [definitionMetadata] = ["class", "Shape"]; 
});
let Shape = Shape2; export default Shape;
"""))

  fileNameSourceMap += "ShapeSet.mls" -> ("""
import "./Block.mls"
import "./CachedHash.mls"
import "./Option.mls"
import "./Predef.mls"
import "./Shape.mls"
import "./StrOps.mls"

open Block {ClassSymbol}
open Shape {Dyn, Lit, Arr, Class}
open Predef
open Option
open StrOps

type ShapeSet = ShapeSet.ShapeSet

module ShapeSet with...

class ShapeSet(val shapeset: Map[String, Shape]) extends CachedHash with
  fun keys() = [...shapeset.keys()].toSorted()

  fun values() = shapeset.values().toArray()

  fun isEmpty() = shapeset.size == 0

  fun contains(s: Shape) = shapeset.has(s.hash())

  fun flatMap(f) = liftMany(values().flatMap(f))

  fun toString() = "{" ~ shapeset.keys().toArray().toSorted().toString() ~ "}"

  fun isDyn() =
    shapeset.size == 1 and values().0 is Dyn

  fun isDynArr() =
    if shapeset.size == 1 and values().0 is Arr(shapes) then shapes.every(s => s is Dyn) else false


module ShapeSet with
  fun empty = ShapeSet(new Map)

fun lift(s: Shape) = ShapeSet(new Map([[s.hash(), s]]))

fun liftMany(arr: Array[Shape]) = ShapeSet(new Map(arr.map(s => [s.hash(), s])))

fun union(s1: ShapeSet, s2: ShapeSet) = ShapeSet(new Map([...s1.shapeset, ...s2.shapeset]))

fun flat(arr: Array[ShapeSet]) = ShapeSet(new Map(arr.map(_.shapeset.entries().toArray()).flat()))

// Cartesian product: https://stackoverflow.com/a/43053803
fun prod(xs) =
  if xs.length ==
    0 then [[]]
    1 then xs
  else xs.reduce((a, b) => a.flatMap(d => b.map(e => [d, e].flat())))

// lifted constructors

fun mkBot() = ShapeSet.empty

fun mkDyn() = lift(Dyn())

fun mkLit(l) = lift(Lit(l))

fun mkArr(shapes: Array[ShapeSet]) =
  shapes
    .map(_.shapeset.values().toArray())
    |> prod
    .map(x => Arr(x))
    |> liftMany

fun mkClass(sym: ClassSymbol, params: Array[ShapeSet]) =
  params
    .map(_.shapeset.values().toArray())
    |> prod
    .map(Class(sym, _))
    |> liftMany

// helper functions

fun filterSet(s: ShapeSet, p: Block.Case) = s.flatMap(Shape.filter(_, p))

fun restSet(s: ShapeSet, p: Block.Case) = s.flatMap(Shape.rest(_, p))

fun selSet(s1: ShapeSet, s2: ShapeSet) =
  prod([s1.values(), s2.values()])
    .flatMap(pair => Shape.sel(pair.0, pair.1))
    |> liftMany

fun staticSet(s: ShapeSet) =
  let v = s.values()
  if v.length ==
    1 then Shape.static(v.0)
    else false

fun valOf(s : Shape) =
  if s is
    Dyn() then throw Error("valOf on Dyn")
    Lit(l) then l
    Arr(shapes) then shapes.map((x, _, _) => valOf(x))
    Class(ClassSymbol(name, value, paramsOpt, auxParams), params) then value(...params.map(valOf))
    else throw Error("Unknown shape")

fun valOfSet(s : ShapeSet) =
  if s.values().length ==
    1 then valOf(s.values().0)
    else throw Error("valOfSet on non-singleton ShapeSet")

let idCounter = 0
fun freshId(prefix) =
  set idCounter = idCounter + 1
  Block.Symbol(prefix + "_" + idCounter.toString())

fun val2path(v, allocs) =
  if typeof(v) == "number" || typeof(v) == "string" || typeof(v) == "boolean" then
    [Block.End(), Block.ValueLit(v)]
  else if Array.isArray(v) then
    let mapped = v.map(val2path(_, allocs))
    let blocks = mapped.map(_.0)
    let paths = mapped.map(_.1)
    let tupSym = freshId("tup")
    allocs.push(tupSym)
    let tupAssign = Block.Assign(tupSym, Block.Tuple(paths.map(Block.Arg(_))), Block.End())
    let fullBlock = foldl((b, acc) => Block.concat(b, acc))(tupAssign, ...blocks)
    [fullBlock, Block.ValueRef(tupSym)]
  else if v !== undefined and v !== null and v.constructor !== undefined and v.constructor.(Symbols.definitionMetadata) !== undefined then
    let md = v.constructor.(Symbols.definitionMetadata)
    let clsName = md.1
    let paramNames = md.2
    let classSym = Block.ClassSymbol(clsName, undefined, None, []) // TODO: add correct Paramters to ClassSymbol
    let mapped = paramNames.map((fld, _, _) => val2path(v.(fld), allocs))
    let blocks = mapped.map(_.0)
    let paths = mapped.map(_.1)
    let objSym = freshId("obj")
    allocs.push(objSym)
    let objAssign = Block.Assign(objSym, Block.Instantiate(Block.ValueRef(classSym), paths.map(Block.Arg(_))), Block.End())
    let fullBlock = fold((b, acc) => Block.concat(acc, b))(objAssign, ...blocks)
    [fullBlock, Block.ValueRef(objSym)]
  else [Block.End(), Block.ValueLit(42)]
""" -> S("""
const definitionMetadata = globalThis.Symbol.for("mlscript.definitionMetadata");
const prettyPrint = globalThis.Symbol.for("mlscript.prettyPrint");
import runtime from "./Runtime.mjs";
import Block from "./Block.mjs";
import CachedHash from "./CachedHash.mjs";
import Option from "./Option.mjs";
import Predef from "./Predef.mjs";
import Shape from "./Shape.mjs";
import StrOps from "./StrOps.mjs";
let ShapeSet2;
(class ShapeSet {
  static {
    ShapeSet2 = this
  }
  constructor() {
    runtime.Unit;
  }
  static #idCounter;
  static {
    this.ShapeSet = function ShapeSet(shapeset) {
      return globalThis.Object.freeze(new ShapeSet.class(shapeset));
    };
    (class ShapeSet1 extends CachedHash {
      static {
        ShapeSet.ShapeSet.class = this
      }
      constructor(shapeset) {
        super();
        this.shapeset = shapeset;
      }
      static get empty() {
        let tmp;
        tmp = globalThis.Object.freeze(new globalThis.Map());
        return ShapeSet.ShapeSet(tmp);
      }
      keys() {
        let tmp, tmp1;
        tmp = runtime.safeCall(this.shapeset.keys());
        tmp1 = globalThis.Object.freeze([
          ...tmp
        ]);
        return runtime.safeCall(tmp1.toSorted())
      } 
      values() {
        let tmp;
        tmp = runtime.safeCall(this.shapeset.values());
        return runtime.safeCall(tmp.toArray())
      } 
      isEmpty() {
        return Predef.equals(this.shapeset.size, 0)
      } 
      contains(s) {
        let tmp;
        tmp = runtime.safeCall(s.hash());
        return runtime.safeCall(this.shapeset.has(tmp))
      } 
      flatMap(f) {
        let tmp, tmp1;
        tmp = this.values();
        tmp1 = runtime.safeCall(tmp.flatMap(f));
        return ShapeSet.liftMany(tmp1)
      } 
      toString() {
        let tmp, tmp1, tmp2, tmp3, tmp4;
        tmp = runtime.safeCall(this.shapeset.keys());
        tmp1 = runtime.safeCall(tmp.toArray());
        tmp2 = runtime.safeCall(tmp1.toSorted());
        tmp3 = runtime.safeCall(tmp2.toString());
        tmp4 = StrOps.concat2("{", tmp3);
        return StrOps.concat2(tmp4, "}")
      } 
      isDyn() {
        let scrut, scrut1, tmp;
        scrut = Predef.equals(this.shapeset.size, 1);
        if (scrut === true) {
          tmp = this.values();
          scrut1 = tmp[0];
          if (scrut1 instanceof Shape.Dyn.class) {
            return true
          } else {
            return false
          }
        } else {
          return false
        }
      } 
      isDynArr() {
        let scrut, shapes, scrut1, arg$Arr$0$, tmp, lambda;
        scrut = Predef.equals(this.shapeset.size, 1);
        if (scrut === true) {
          tmp = this.values();
          scrut1 = tmp[0];
          if (scrut1 instanceof Shape.Arr.class) {
            arg$Arr$0$ = scrut1.shapes;
            shapes = arg$Arr$0$;
            lambda = (undefined, function (s) {
              if (s instanceof Shape.Dyn.class) {
                return true
              } else {
                return false
              }
            });
            return runtime.safeCall(shapes.every(lambda))
          } else {
            return false
          }
        } else {
          return false
        }
      }
      [prettyPrint]() { return this.toString(); }
      static [definitionMetadata] = ["class", "ShapeSet", ["shapeset"]]; 
    });
    ShapeSet.#idCounter = 0;
  }
  static lift(s) {
    let tmp, tmp1, tmp2, tmp3;
    tmp = runtime.safeCall(s.hash());
    tmp1 = globalThis.Object.freeze([
      tmp,
      s
    ]);
    tmp2 = globalThis.Object.freeze([
      tmp1
    ]);
    tmp3 = globalThis.Object.freeze(new globalThis.Map(tmp2));
    return ShapeSet.ShapeSet(tmp3)
  } 
  static liftMany(arr) {
    let lambda, tmp, tmp1;
    lambda = (undefined, function (s) {
      let tmp2;
      tmp2 = runtime.safeCall(s.hash());
      return globalThis.Object.freeze([
        tmp2,
        s
      ])
    });
    tmp = runtime.safeCall(arr.map(lambda));
    tmp1 = globalThis.Object.freeze(new globalThis.Map(tmp));
    return ShapeSet.ShapeSet(tmp1)
  } 
  static union(s1, s2) {
    let tmp, tmp1;
    tmp = globalThis.Object.freeze([
      ...s1.shapeset,
      ...s2.shapeset
    ]);
    tmp1 = globalThis.Object.freeze(new globalThis.Map(tmp));
    return ShapeSet.ShapeSet(tmp1)
  } 
  static flat(arr) {
    let lambda, tmp, tmp1, tmp2;
    lambda = (undefined, function (_0) {
      let tmp3;
      tmp3 = runtime.safeCall(_0.shapeset.entries());
      return runtime.safeCall(tmp3.toArray())
    });
    tmp = runtime.safeCall(arr.map(lambda));
    tmp1 = runtime.safeCall(tmp.flat());
    tmp2 = globalThis.Object.freeze(new globalThis.Map(tmp1));
    return ShapeSet.ShapeSet(tmp2)
  } 
  static prod(xs) {
    let scrut, scrut1, scrut2, tmp, lambda;
    scrut = xs.length;
    scrut1 = Predef.equals(scrut, 0);
    if (scrut1 === true) {
      tmp = globalThis.Object.freeze([]);
      return globalThis.Object.freeze([
        tmp
      ])
    } else {
      scrut2 = Predef.equals(scrut, 1);
      if (scrut2 === true) {
        return xs
      } else {
        lambda = (undefined, function (a, b) {
          let lambda1;
          lambda1 = (undefined, function (d) {
            let lambda2;
            lambda2 = (undefined, function (e) {
              let tmp1;
              tmp1 = globalThis.Object.freeze([
                d,
                e
              ]);
              return runtime.safeCall(tmp1.flat())
            });
            return runtime.safeCall(b.map(lambda2))
          });
          return runtime.safeCall(a.flatMap(lambda1))
        });
        return runtime.safeCall(xs.reduce(lambda))
      }
    }
  } 
  static mkBot() {
    return ShapeSet.ShapeSet.class.empty
  } 
  static mkDyn() {
    let tmp;
    tmp = Shape.Dyn();
    return ShapeSet.lift(tmp)
  } 
  static mkLit(l) {
    let tmp;
    tmp = Shape.Lit(l);
    return ShapeSet.lift(tmp)
  } 
  static mkArr(shapes) {
    let lambda, tmp, tmp1, lambda1, tmp2;
    lambda = (undefined, function (_0) {
      let tmp3;
      tmp3 = runtime.safeCall(_0.shapeset.values());
      return runtime.safeCall(tmp3.toArray())
    });
    tmp = runtime.safeCall(shapes.map(lambda));
    tmp1 = Predef.pipeInto(tmp, ShapeSet.prod);
    lambda1 = (undefined, function (x) {
      return Shape.Arr(x)
    });
    tmp2 = runtime.safeCall(tmp1.map(lambda1));
    return Predef.pipeInto(tmp2, ShapeSet.liftMany)
  } 
  static mkClass(sym, params) {
    let lambda, tmp, tmp1, lambda1, tmp2;
    lambda = (undefined, function (_0) {
      let tmp3;
      tmp3 = runtime.safeCall(_0.shapeset.values());
      return runtime.safeCall(tmp3.toArray())
    });
    tmp = runtime.safeCall(params.map(lambda));
    tmp1 = Predef.pipeInto(tmp, ShapeSet.prod);
    lambda1 = (undefined, function (_0) {
      return Shape.Class(sym, _0)
    });
    tmp2 = runtime.safeCall(tmp1.map(lambda1));
    return Predef.pipeInto(tmp2, ShapeSet.liftMany)
  } 
  static filterSet(s, p) {
    let lambda;
    lambda = (undefined, function (_0) {
      return Shape.filter(_0, p)
    });
    return s.flatMap(lambda)
  } 
  static restSet(s, p) {
    let lambda;
    lambda = (undefined, function (_0) {
      return Shape.rest(_0, p)
    });
    return s.flatMap(lambda)
  } 
  static selSet(s1, s2) {
    let tmp, tmp1, tmp2, tmp3, lambda, tmp4;
    tmp = s1.values();
    tmp1 = s2.values();
    tmp2 = globalThis.Object.freeze([
      tmp,
      tmp1
    ]);
    tmp3 = ShapeSet.prod(tmp2);
    lambda = (undefined, function (pair) {
      return Shape.sel(pair[0], pair[1])
    });
    tmp4 = runtime.safeCall(tmp3.flatMap(lambda));
    return Predef.pipeInto(tmp4, ShapeSet.liftMany)
  } 
  static staticSet(s) {
    let v, scrut, scrut1;
    v = s.values();
    scrut = v.length;
    scrut1 = Predef.equals(scrut, 1);
    if (scrut1 === true) {
      return Shape.static(v[0])
    } else {
      return false
    }
  } 
  static valOf(s) {
    let l, shapes, params, value, arg$Class$0$, arg$Class$1$, arg$ClassSymbol$1$, arg$Arr$0$, arg$Lit$0$, tmp, lambda, tmp1;
    split_root$: {
      split_1$: {
        if (s instanceof Shape.Dyn.class) {
          throw runtime.safeCall(globalThis.Error("valOf on Dyn"))
        } else if (s instanceof Shape.Lit.class) {
          arg$Lit$0$ = s.l;
          l = arg$Lit$0$;
          tmp = l;
          break split_root$
        } else if (s instanceof Shape.Arr.class) {
          arg$Arr$0$ = s.shapes;
          shapes = arg$Arr$0$;
          lambda = (undefined, function (x, _, _1) {
            return ShapeSet.valOf(x)
          });
          tmp = runtime.safeCall(shapes.map(lambda));
          break split_root$
        } else if (s instanceof Shape.Class.class) {
          arg$Class$0$ = s.sym;
          arg$Class$1$ = s.params;
          if (arg$Class$0$ instanceof Block.ClassSymbol.class) {
            arg$Class$0$.name;
            arg$ClassSymbol$1$ = arg$Class$0$.value;
            arg$Class$0$.paramsOpt;
            arg$Class$0$.auxParams;
            params = arg$Class$1$;
            value = arg$ClassSymbol$1$;
            tmp1 = runtime.safeCall(params.map(ShapeSet.valOf));
            tmp = runtime.safeCall(value(...tmp1));
            break split_root$
          } else {
            break split_1$
          }
        } else {
          break split_1$
        }
      }
      throw runtime.safeCall(globalThis.Error("Unknown shape"));
    }
    return tmp
  } 
  static valOfSet(s) {
    let scrut, scrut1, tmp, tmp1;
    tmp = s.values();
    scrut = tmp.length;
    scrut1 = Predef.equals(scrut, 1);
    if (scrut1 === true) {
      tmp1 = s.values();
      return ShapeSet.valOf(tmp1[0])
    } else {
      throw runtime.safeCall(globalThis.Error("valOfSet on non-singleton ShapeSet"))
    }
  } 
  static freshId(prefix) {
    let tmp, tmp1, tmp2, tmp3;
    tmp = ShapeSet.#idCounter + 1;
    ShapeSet.#idCounter = tmp;
    tmp1 = prefix + "_";
    tmp2 = runtime.safeCall(ShapeSet.#idCounter.toString());
    tmp3 = tmp1 + tmp2;
    return Block.Symbol(tmp3)
  } 
  static val2path(v, allocs) {
    let scrut, scrut1, mapped, blocks, paths, tupSym, tupAssign, fullBlock, scrut2, md, clsName, paramNames, classSym, mapped1, blocks1, paths1, objSym, objAssign, fullBlock1, scrut3, scrut4, scrut5, tmp, tmp1, lambda, tmp2, lambda1, tmp3, tmp4, lambda2, lambda3, lambda4, lambda5, tmp5, tmp6, tmp7, lambda6, tmp8, tmp9, tmp10, lambda7, lambda8, lambda9, tmp11, lambda10, tmp12, tmp13, tmp14, lambda11, tmp15, tmp16, tmp17, tmp18, tmp19;
    tmp = typeof v;
    tmp1 = Predef.equals(tmp, "number");
    lambda = (undefined, function () {
      let tmp20;
      tmp20 = typeof v;
      return Predef.equals(tmp20, "string")
    });
    tmp2 = runtime.short_or(tmp1, lambda);
    lambda1 = (undefined, function () {
      let tmp20;
      tmp20 = typeof v;
      return Predef.equals(tmp20, "boolean")
    });
    scrut = runtime.short_or(tmp2, lambda1);
    if (scrut === true) {
      tmp3 = Block.End();
      tmp4 = Block.ValueLit(v);
      return globalThis.Object.freeze([
        tmp3,
        tmp4
      ])
    } else {
      scrut1 = globalThis.Array.isArray(v);
      if (scrut1 === true) {
        lambda2 = (undefined, function (_0) {
          return ShapeSet.val2path(_0, allocs)
        });
        mapped = runtime.safeCall(v.map(lambda2));
        lambda3 = (undefined, function (_0) {
          return _0[0]
        });
        blocks = runtime.safeCall(mapped.map(lambda3));
        lambda4 = (undefined, function (_0) {
          return _0[1]
        });
        paths = runtime.safeCall(mapped.map(lambda4));
        tupSym = ShapeSet.freshId("tup");
        runtime.safeCall(allocs.push(tupSym));
        lambda5 = (undefined, function (_0) {
          return Block.Arg(_0)
        });
        tmp5 = runtime.safeCall(paths.map(lambda5));
        tmp6 = Block.Tuple(tmp5);
        tmp7 = Block.End();
        tupAssign = Block.Assign(tupSym, tmp6, tmp7);
        lambda6 = (undefined, function (b, acc) {
          return Block.concat(b, acc)
        });
        tmp8 = runtime.safeCall(Predef.foldl(lambda6));
        fullBlock = runtime.safeCall(tmp8(tupAssign, ...blocks));
        tmp9 = Block.ValueRef(tupSym);
        return globalThis.Object.freeze([
          fullBlock,
          tmp9
        ])
      } else {
        split_root$: {
          split_1$: {
            scrut2 = v !== undefined;
            if (scrut2 === true) {
              scrut5 = v !== null;
              if (scrut5 === true) {
                scrut4 = v.constructor !== undefined;
                if (scrut4 === true) {
                  scrut3 = v.constructor[Predef.Symbols.definitionMetadata] !== undefined;
                  if (scrut3 === true) {
                    md = v.constructor[Predef.Symbols.definitionMetadata];
                    clsName = md[1];
                    paramNames = md[2];
                    tmp10 = globalThis.Object.freeze([]);
                    classSym = Block.ClassSymbol(clsName, undefined, Option.None, tmp10);
                    lambda7 = (undefined, function (fld, _, _1) {
                      return ShapeSet.val2path(v[fld], allocs)
                    });
                    mapped1 = runtime.safeCall(paramNames.map(lambda7));
                    lambda8 = (undefined, function (_0) {
                      return _0[0]
                    });
                    blocks1 = runtime.safeCall(mapped1.map(lambda8));
                    lambda9 = (undefined, function (_0) {
                      return _0[1]
                    });
                    paths1 = runtime.safeCall(mapped1.map(lambda9));
                    objSym = ShapeSet.freshId("obj");
                    runtime.safeCall(allocs.push(objSym));
                    tmp11 = Block.ValueRef(classSym);
                    lambda10 = (undefined, function (_0) {
                      return Block.Arg(_0)
                    });
                    tmp12 = runtime.safeCall(paths1.map(lambda10));
                    tmp13 = Block.Instantiate(tmp11, tmp12);
                    tmp14 = Block.End();
                    objAssign = Block.Assign(objSym, tmp13, tmp14);
                    lambda11 = (undefined, function (b, acc) {
                      return Block.concat(acc, b)
                    });
                    tmp15 = runtime.safeCall(Predef.fold(lambda11));
                    fullBlock1 = runtime.safeCall(tmp15(objAssign, ...blocks1));
                    tmp16 = Block.ValueRef(objSym);
                    tmp17 = globalThis.Object.freeze([
                      fullBlock1,
                      tmp16
                    ]);
                    break split_root$
                  } else {
                    break split_1$
                  }
                } else {
                  break split_1$
                }
              } else {
                break split_1$
              }
            } else {
              break split_1$
            }
          }
          tmp18 = Block.End();
          tmp19 = Block.ValueLit(42);
          tmp17 = globalThis.Object.freeze([
            tmp18,
            tmp19
          ]);
        }
        return tmp17
      }
    }
  }
  toString() { return runtime.render(this); }
  static [definitionMetadata] = ["class", "ShapeSet"]; 
});
let ShapeSet = ShapeSet2; export default ShapeSet;
"""))

  fileNameSourceMap += "SpecializeHelpers.mls" -> ("""
import "./Block.mls"
import "./Shape.mls"
import "./Option.mls"
import "./ShapeSet.mls"
import "./Predef.mls"
import "./Runtime.mls"

open Block
open Shape
open Option
open Predef
open ShapeSet

module SpecializeHelpers with ...

fun wrapScoped(symbols, block) =
  if symbols.length == 0 then block
  else if block is Scoped(oldSymbols, rest) then Scoped([...symbols, ...oldSymbols], rest)
  else Scoped(symbols, block)

class Ctx(val ctx: Map[String, ShapeSet], val allocs: Array[Block.Symbol]) with
  fun get(path) =
    let ps = showPath(path)
    if ctx.has(ps) then Some(ctx.get(ps))
    else None
  fun clone = Ctx(new Map(ctx), allocs)
  fun add(path, ss) =
    let ps = showPath(path)
    if ctx.has(ps) then
      ctx.set(ps, union(ctx.get(ps), ss))
    else
      ctx.set(ps, ss)
    this
module Ctx with
  fun empty = Ctx(new Map(), mut [])

fun sov(v): ShapeSet =
  if typeof(v) == "number" || typeof(v) == "string" || typeof(v) == "boolean" then
    mkLit(v)
  else if Array.isArray(v) then
    mkArr(v.map(sov))
  else if v !== undefined and v !== null and v.constructor !== undefined and v.constructor.(Symbols.definitionMetadata) !== undefined then
    // TODO
    undefined

fun sop(ctx, p): ShapeSet =
  if ctx.get(p) is Some(s) then s
  else if p is
    Select(qual, sym) then
      selSet(sop(ctx, qual), mkLit(sym.name))
    DynSelect(qual, fld, _) then
      selSet(sop(ctx, qual), sop(ctx, fld))
    ValueLit(lit) then mkLit(lit)
    ValueRef(l) then mkDyn()

fun sor(ctx, r) = if r is
  Path and
    let s = sop(ctx, r)
    staticSet(s) and val2path(valOfSet(s), ctx.allocs) is [blk, res] then [blk, res, s]
    else
      [End(), r, s]
  Instantiate(cls, args) then
    let clsSymb = if cls is
      ValueRef(symb) and symb is ClassSymbol then
        symb
      Select(_, symb) and symb is ClassSymbol then symb
      else throw Error("Instantiate with non-ClassSymbol in shape propagation: " + cls.toString())
    [End(), r, mkClass(clsSymb, args.map(a => sop(ctx, a.value)))]
  Tuple(elems) then [End(), r, mkArr(elems.map(a => sop(ctx, a.value)))]
  Call(f, args) then
    fun lit(l) = [End(), ValueLit(l), mkLit(l)]
    let argShapes = args.map((a, _, _) => sop(ctx, a.value))
    if f is
      Select(Select(ValueRef(Symbol("runtime")), Symbol("Tuple")), Symbol("get"))
        and args is [Arg(scrut), Arg(litArg)] then
          let recovered = DynSelect(scrut, litArg, false)
          [End(), recovered, sop(ctx, recovered)]
      Select(Select(ValueRef(Symbol("runtime")), Symbol("Tuple")), Symbol("slice")) then
        throw Error("runtime.Tuple.slice not handled in shape propagation") // TODO
      ValueRef(clsSymb) and clsSymb is ClassSymbol then
        [End(), r, mkClass(clsSymb, args.map(a => sop(ctx, a.value)))]
      ValueRef(symb) and  // built-in or top-level
        let name = symb.name
        args is
          [x] and sop(ctx, x.value).values() is [Shape.Lit(l)] and name is
            "!" then lit(not l)
            "-" then lit(-l)
            "+" then lit(+l)
          [x, y] and sop(ctx, x.value).values() is [Shape.Lit(l1)] and sop(ctx, y.value).values() is [Shape.Lit(l2)] and name is
            "+" then lit(l1 + l2)
            "-" then lit(l1 - l2)
            "*" then lit(l1 * l2)
            "/" then lit(l1 / l2)
            "%" then lit(l1 % l2)
            "==" then lit(l1 == l2)
            "!=" then lit(l1 != l2)
            "<" then lit(l1 < l2)
            "<=" then lit(l1 <= l2)
            ">" then lit(l1 > l2)
            ">=" then lit(l1 >= l2)
            "===" then lit(l1 === l2)
            "!==" then lit(l1 !== l2)
            // "&&" then lit(l1 && l2)
            // "||" then lit(l1 || l2)
      Select(ValueRef(ModuleSymbol(name, value)), Symbol(symb)) and
        let mapPropName = "generatorMap$" + name
        let cachePropName = "cache$" + name
        let genMap = value.(mapPropName)
        not (genMap is undefined) and
          let f = genMap.get(symb)
          not (f is Runtime.Unit) then // staged function
            let res = f(...argShapes)
            [End(), Call(Select(ValueRef(ModuleSymbol(name, value)), res.0), args), res.1]
          else
            [End(), r, mkDyn()] // TODO
        argShapes.every(staticSet) then // non staged function but params known
          let f = value.(symb)
          let evaluated = f(...argShapes.map(valOfSet))
          let evaluated_path = val2path(evaluated, ctx.allocs)
          [evaluated_path.0, evaluated_path.1, sov(evaluated)]
      Select(ValueRef(ClassSymbol(name, value, paramsOpt, auxParams)), Symbol(symb)) then
        // TODO
        [End(), r, mkDyn()]
      Select(ValueRef(symb), Symbol(f)) and sop(ctx, ValueRef(symb)) is Class(sym, params) then // class method call
        // TODO
    else
      [End(), r, mkDyn()]

fun prop(ctx, b) = if b is
    End then [b, mkBot()]
    Return(res, implct) and sor(ctx, res) is [blk, r1, s1] then
      [concat(blk, Return(r1, implct)), s1]
    Scoped(symbols, rest) then
      symbols.forEach((x, _, _) => ctx.add(ValueRef(x), mkBot()))
      let newAllocs = mut []
      let newCtx = Ctx(new Map(ctx.ctx), newAllocs)
      let res = prop(newCtx, rest)
      [wrapScoped([...symbols, ...newAllocs], res.0), res.1]
    Assign(x, r, b) and
      sor(ctx, r) is [blk, r1, s1] then
        if prop(ctx.add(ValueRef(x), s1), b) is [b2, s2] then
          [concat(blk, Assign(x, r1, b2)), s2]
    Match(p, arms, dflt, restBlock) then
      let s = sop(ctx, p)
      let filteredArms = foldl((r, arm) =>
        let fs = filterSet(r.0, arm.cse)
        if fs.isEmpty() then r
        else
          let branchCtx = ctx.clone
          if not p is ValueLit do branchCtx.add(p, fs)
          let res = prop(branchCtx, concat(arm.body, restBlock))
          [restSet(r.0, arm.cse), union(r.1, res.1), [...r.2, Arm(arm.cse, res.0)]]
      )([s, mkBot(), []], ...arms)
      if filteredArms.2.length is
        0 and
          filteredArms.0.isEmpty() then prop(ctx, restBlock)
          else prop(ctx, concat(if dflt is Some(d) then d else End(), restBlock))
        1 and
          filteredArms.0.isEmpty() then
            let newRest = prop(ctx, restBlock)
            [concat(filteredArms.2.0.body, newRest.0), union(filteredArms.1, newRest.1)]
          dflt is
            Some(d) then
              let branchCtx = ctx.clone
              if not p is ValueLit do branchCtx.add(p, filteredArms.0)
              let newDflt = prop(branchCtx, concat(d, restBlock))
              [Match(p, filteredArms.2, Some(newDflt.0), End()), union(filteredArms.1, newDflt.1)]
            else
              [Match(p, filteredArms.2, None, End()), filteredArms.1]
        n and dflt is
            Some(d) then
              let branchCtx = ctx.clone
              if not p is ValueLit do branchCtx.add(p, filteredArms.0)
              let newDflt = prop(branchCtx, concat(d, restBlock))
              [Match(p, filteredArms.2, Some(newDflt.0), End()), union(filteredArms.1, newDflt.1)]
            else
              [Match(p, filteredArms.2, None, End()), filteredArms.1]

// TODO: debug only; remove this
fun propStub(ctx, body) =
  console.log("Calling the propStub now which performs no shape propagation")
  [body, mkDyn()]

fun buildShapeName(s: Shape): Str =
  if s is
    Dyn then "Dyn"
    Lit(lit) and lit is Str then "Str" + lit
    Lit(lit) then "Lit" + lit.toString()
    Arr(shapes) then "Arr_" + shapes.map(buildShapeName).join("_") + "_end"
    Class(sym, params) then sym.name + "_" + params.map(buildShapeName).join("_")
    else "Unk"

fun buildShapeSetName(ss: ShapeSet): Str =
  let vals = ss.values()
  if vals.length == 1 then buildShapeName(vals.0)
  else "Union_" + vals.map(buildShapeName).join("_") + "_end"

fun specializeName(funName, shapes) =
  if shapes.every(ps => ps.every(s => s.isDyn())) then funName
  else
    funName + "_" + shapes.map(ps => ps.map(buildShapeSetName).join("_")).join("__")

fun specialize(cache, funName, dflt, shapes) =
  // replace function symbol in block definition to new name
  let newName = specializeName(funName, shapes)
  if cache.getFun(newName) is
    Some(x) then [x.0.sym, x.1]
    None and
      let defn = dflt()
      defn is FunDefn(Symbol(_), ps, body) then
        let ctx = Ctx.empty
        ps.forEach((p, i, _) => p.forEach((p2, j, _) => ctx.add(ValueRef(p2), shapes.(i).(j))))
        cache.setFun(newName, [FunDefn(Symbol(newName), ps, body), mkDyn()])
        let res = prop(ctx, body)
        let bodyWithScoped = wrapScoped(ctx.allocs, res.0)
        let entry = cache.setFun(newName, [FunDefn(Symbol(newName), ps, bodyWithScoped), res.1])
        [entry.0.sym, entry.1]

class FunCache(val cache: Map[String, [FunDefn, ShapeSet]]) with
  fun getFun(k) = if cache.has(k) then Some(cache.get(k)) else None
  // NOTE: this will be called at the beginning of specialization to avoid infinite calls when specializing recursive functions
  fun setFun(k, v) = cache.set(k, v); v
  fun toString() = cache.values().map((d, _, _) => showDefn(d.0)).toArray().join("\n")
  fun dump() = cache.values().map((d, _, _) => d.0).toArray()



""" -> S("""
const definitionMetadata = globalThis.Symbol.for("mlscript.definitionMetadata");
const prettyPrint = globalThis.Symbol.for("mlscript.prettyPrint");
import runtime from "./Runtime.mjs";
import Block from "./Block.mjs";
import Shape from "./Shape.mjs";
import Option from "./Option.mjs";
import ShapeSet from "./ShapeSet.mjs";
import Predef from "./Predef.mjs";
import Runtime from "./Runtime.mjs";
let SpecializeHelpers1;
(class SpecializeHelpers {
  static {
    SpecializeHelpers1 = this
  }
  constructor() {
    runtime.Unit;
  }
  static {
    this.Ctx = function Ctx(ctx, allocs) {
      return globalThis.Object.freeze(new Ctx.class(ctx, allocs));
    };
    (class Ctx {
      static {
        SpecializeHelpers.Ctx.class = this
      }
      constructor(ctx, allocs) {
        this.ctx = ctx;
        this.allocs = allocs;
      }
      static get empty() {
        let tmp, tmp1;
        tmp = globalThis.Object.freeze(new globalThis.Map());
        tmp1 = [];
        return SpecializeHelpers.Ctx(tmp, tmp1);
      }
      get(path) {
        let ps, scrut, tmp;
        ps = Block.showPath(path);
        scrut = runtime.safeCall(this.ctx.has(ps));
        if (scrut === true) {
          tmp = runtime.safeCall(this.ctx.get(ps));
          return Option.Some(tmp)
        } else {
          return Option.None
        }
      } 
      get clone() {
        let tmp;
        tmp = globalThis.Object.freeze(new globalThis.Map(this.ctx));
        return SpecializeHelpers.Ctx(tmp, this.allocs);
      } 
      add(path, ss) {
        let ps, scrut, tmp, tmp1;
        ps = Block.showPath(path);
        scrut = runtime.safeCall(this.ctx.has(ps));
        if (scrut === true) {
          tmp = runtime.safeCall(this.ctx.get(ps));
          tmp1 = ShapeSet.union(tmp, ss);
          this.ctx.set(ps, tmp1);
        } else {
          this.ctx.set(ps, ss);
        }
        return this
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Ctx", ["ctx", "allocs"]]; 
    });
    this.FunCache = function FunCache(cache) {
      return globalThis.Object.freeze(new FunCache.class(cache));
    };
    (class FunCache {
      static {
        SpecializeHelpers.FunCache.class = this
      }
      constructor(cache) {
        this.cache = cache;
      }
      getFun(k) {
        let scrut, tmp;
        scrut = runtime.safeCall(this.cache.has(k));
        if (scrut === true) {
          tmp = runtime.safeCall(this.cache.get(k));
          return Option.Some(tmp)
        } else {
          return Option.None
        }
      } 
      setFun(k, v) {
        let tmp;
        tmp = this.cache.set(k, v);
        return (tmp , v)
      } 
      toString() {
        let tmp, lambda, tmp1, tmp2;
        tmp = runtime.safeCall(this.cache.values());
        lambda = (undefined, function (d, _, _1) {
          return Block.showDefn(d[0])
        });
        tmp1 = runtime.safeCall(tmp.map(lambda));
        tmp2 = runtime.safeCall(tmp1.toArray());
        return runtime.safeCall(tmp2.join("
"))
      } 
      dump() {
        let tmp, lambda, tmp1;
        tmp = runtime.safeCall(this.cache.values());
        lambda = (undefined, function (d, _, _1) {
          return d[0]
        });
        tmp1 = runtime.safeCall(tmp.map(lambda));
        return runtime.safeCall(tmp1.toArray())
      }
      [prettyPrint]() { return this.toString(); }
      static [definitionMetadata] = ["class", "FunCache", ["cache"]]; 
    });
  }
  static wrapScoped(symbols, block) {
    let scrut, rest, oldSymbols, arg$Scoped$0$, arg$Scoped$1$, tmp;
    scrut = Predef.equals(symbols.length, 0);
    if (scrut === true) {
      return block
    } else {
      if (block instanceof Block.Scoped.class) {
        arg$Scoped$0$ = block.symbols;
        arg$Scoped$1$ = block.rest;
        rest = arg$Scoped$1$;
        oldSymbols = arg$Scoped$0$;
        tmp = globalThis.Object.freeze([
          ...symbols,
          ...oldSymbols
        ]);
        return Block.Scoped(tmp, rest)
      } else {
        return Block.Scoped(symbols, block)
      }
    }
  } 
  static sov(v) {
    let scrut, scrut1, scrut2, scrut3, scrut4, scrut5, tmp, tmp1, lambda, tmp2, lambda1, tmp3, tmp4;
    tmp = typeof v;
    tmp1 = Predef.equals(tmp, "number");
    lambda = (undefined, function () {
      let tmp5;
      tmp5 = typeof v;
      return Predef.equals(tmp5, "string")
    });
    tmp2 = runtime.short_or(tmp1, lambda);
    lambda1 = (undefined, function () {
      let tmp5;
      tmp5 = typeof v;
      return Predef.equals(tmp5, "boolean")
    });
    scrut = runtime.short_or(tmp2, lambda1);
    if (scrut === true) {
      return ShapeSet.mkLit(v)
    } else {
      scrut1 = globalThis.Array.isArray(v);
      if (scrut1 === true) {
        tmp3 = runtime.safeCall(v.map(SpecializeHelpers.sov));
        return ShapeSet.mkArr(tmp3)
      } else {
        split_root$: {
          split_default$: {
            scrut2 = v !== undefined;
            if (scrut2 === true) {
              scrut5 = v !== null;
              if (scrut5 === true) {
                scrut4 = v.constructor !== undefined;
                if (scrut4 === true) {
                  scrut3 = v.constructor[Predef.Symbols.definitionMetadata] !== undefined;
                  if (scrut3 === true) {
                    tmp4 = undefined;
                    break split_root$
                  } else {
                    break split_default$
                  }
                } else {
                  break split_default$
                }
              } else {
                break split_default$
              }
            } else {
              break split_default$
            }
          }
          throw globalThis.Object.freeze(new globalThis.Error("match error"));
        }
        return tmp4
      }
    }
  } 
  static sop(ctx, p) {
    let scrut, s, qual, sym, qual1, fld, lit, arg$Some$0$, arg$ValueLit$0$, arg$DynSelect$0$, arg$DynSelect$1$, arg$Select$0$, arg$Select$1$, tmp, tmp1, tmp2, tmp3;
    scrut = runtime.safeCall(ctx.get(p));
    if (scrut instanceof Option.Some.class) {
      arg$Some$0$ = scrut.value;
      s = arg$Some$0$;
      return s
    } else {
      if (p instanceof Block.Select.class) {
        arg$Select$0$ = p.qual;
        arg$Select$1$ = p.name;
        sym = arg$Select$1$;
        qual = arg$Select$0$;
        tmp = SpecializeHelpers.sop(ctx, qual);
        tmp1 = ShapeSet.mkLit(sym.name);
        return ShapeSet.selSet(tmp, tmp1)
      } else if (p instanceof Block.DynSelect.class) {
        arg$DynSelect$0$ = p.qual;
        arg$DynSelect$1$ = p.fld;
        p.arrayIdx;
        fld = arg$DynSelect$1$;
        qual1 = arg$DynSelect$0$;
        tmp2 = SpecializeHelpers.sop(ctx, qual1);
        tmp3 = SpecializeHelpers.sop(ctx, fld);
        return ShapeSet.selSet(tmp2, tmp3)
      } else if (p instanceof Block.ValueLit.class) {
        arg$ValueLit$0$ = p.lit;
        lit = arg$ValueLit$0$;
        return ShapeSet.mkLit(lit)
      } else if (p instanceof Block.ValueRef.class) {
        p.l;
        return ShapeSet.mkDyn()
      } else {
        throw globalThis.Object.freeze(new globalThis.Error("match error"))
      }
    }
  } 
  static sor(ctx, r) {
    let lit, s, scrut, blk, res, scrut1, cls, args, clsSymb, symb, symb1, elems, f, args1, argShapes, scrut2, litArg, recovered, clsSymb1, symb2, name, x, l, scrut3, x1, y, l1, l2, scrut4, scrut5, name1, symb3, value, mapPropName, genMap, scrut6, f1, scrut7, res1, scrut8, f2, evaluated, evaluated_path, symb4, scrut9, arg$Call$0$, arg$Call$1$, arg$Tuple$0$, arg$Instantiate$0$, arg$Instantiate$1$, element1$, element0$, tmp, tmp1, tmp2, arg$Select$1$, arg$ValueRef$0$, tmp3, tmp4, tmp5, tmp6, lambda, tmp7, tmp8, tmp9, lambda1, tmp10, tmp11, lambda2, arg$Select$0$, arg$Select$1$1, arg$ValueRef$0$1, arg$Symbol$0$, arg$ModuleSymbol$0$, arg$ModuleSymbol$1$, arg$ValueRef$0$2, element1$1, element0$1, element0$2, arg$Lit$0$, element0$3, arg$Lit$0$1, element0$4, arg$Lit$0$2, arg$Select$0$1, arg$Select$1$2, arg$ValueRef$0$3, arg$Symbol$0$1, arg$Symbol$0$2, arg$Arg$0$, arg$Arg$0$1, tmp12, tmp13, tmp14, tmp15, tmp16, tmp17, tmp18, tmp19, tmp20, tmp21, tmp22, tmp23, tmp24, tmp25, tmp26, lambda3, tmp27, tmp28, tmp29, tmp30, tmp31, tmp32, tmp33, tmp34, tmp35, tmp36, tmp37, tmp38, tmp39, tmp40, tmp41, tmp42, tmp43, tmp44, tmp45, tmp46, tmp47, tmp48, tmp49;
    if (r instanceof Block.Path) {
      s = SpecializeHelpers.sop(ctx, r);
      scrut = ShapeSet.staticSet(s);
      if (scrut === true) {
        tmp = ShapeSet.valOfSet(s);
        scrut1 = ShapeSet.val2path(tmp, ctx.allocs);
        if (runtime.Tuple.isArrayLike(scrut1) && scrut1.length === 2) {
          element0$ = runtime.Tuple.get(scrut1, 0);
          element1$ = runtime.Tuple.get(scrut1, 1);
          res = element1$;
          blk = element0$;
          return globalThis.Object.freeze([
            blk,
            res,
            s
          ])
        } else {
          tmp1 = Block.End();
          return globalThis.Object.freeze([
            tmp1,
            r,
            s
          ])
        }
      } else {
        tmp2 = Block.End();
        return globalThis.Object.freeze([
          tmp2,
          r,
          s
        ])
      }
    } else if (r instanceof Block.Instantiate.class) {
      arg$Instantiate$0$ = r.cls;
      arg$Instantiate$1$ = r.args;
      args = arg$Instantiate$1$;
      cls = arg$Instantiate$0$;
      split_root$: {
        split_1$: {
          if (cls instanceof Block.ValueRef.class) {
            arg$ValueRef$0$ = cls.l;
            symb = arg$ValueRef$0$;
            if (symb instanceof Block.ClassSymbol.class) {
              tmp3 = symb;
              break split_root$
            } else {
              break split_1$
            }
          } else if (cls instanceof Block.Select.class) {
            cls.qual;
            arg$Select$1$ = cls.name;
            symb1 = arg$Select$1$;
            if (symb1 instanceof Block.ClassSymbol.class) {
              tmp3 = symb1;
              break split_root$
            } else {
              break split_1$
            }
          } else {
            break split_1$
          }
        }
        tmp4 = runtime.safeCall(cls.toString());
        tmp5 = "Instantiate with non-ClassSymbol in shape propagation: " + tmp4;
        throw runtime.safeCall(globalThis.Error(tmp5));
      }
      clsSymb = tmp3;
      tmp6 = Block.End();
      lambda = (undefined, function (a) {
        return SpecializeHelpers.sop(ctx, a.value)
      });
      tmp7 = runtime.safeCall(args.map(lambda));
      tmp8 = ShapeSet.mkClass(clsSymb, tmp7);
      return globalThis.Object.freeze([
        tmp6,
        r,
        tmp8
      ])
    } else if (r instanceof Block.Tuple.class) {
      arg$Tuple$0$ = r.elems;
      elems = arg$Tuple$0$;
      tmp9 = Block.End();
      lambda1 = (undefined, function (a) {
        return SpecializeHelpers.sop(ctx, a.value)
      });
      tmp10 = runtime.safeCall(elems.map(lambda1));
      tmp11 = ShapeSet.mkArr(tmp10);
      return globalThis.Object.freeze([
        tmp9,
        r,
        tmp11
      ])
    } else if (r instanceof Block.Call.class) {
      arg$Call$0$ = r._fun;
      arg$Call$1$ = r.args;
      args1 = arg$Call$1$;
      f = arg$Call$0$;
      lit = function lit(l3) {
        let tmp50, tmp51, tmp52;
        tmp50 = Block.End();
        tmp51 = Block.ValueLit(l3);
        tmp52 = ShapeSet.mkLit(l3);
        return globalThis.Object.freeze([
          tmp50,
          tmp51,
          tmp52
        ])
      };
      lambda2 = (undefined, function (a, _, _1) {
        return SpecializeHelpers.sop(ctx, a.value)
      });
      argShapes = runtime.safeCall(args1.map(lambda2));
      split_root$1: {
        split_1$1: {
          if (f instanceof Block.Select.class) {
            arg$Select$0$ = f.qual;
            arg$Select$1$1 = f.name;
            if (arg$Select$0$ instanceof Block.Select.class) {
              arg$Select$0$1 = arg$Select$0$.qual;
              arg$Select$1$2 = arg$Select$0$.name;
              if (arg$Select$0$1 instanceof Block.ValueRef.class) {
                arg$ValueRef$0$3 = arg$Select$0$1.l;
                if (arg$ValueRef$0$3 instanceof Block.Symbol.class) {
                  arg$Symbol$0$1 = arg$ValueRef$0$3.name;
                  if (arg$Symbol$0$1 === "runtime") {
                    if (arg$Select$1$2 instanceof Block.Symbol.class) {
                      arg$Symbol$0$2 = arg$Select$1$2.name;
                      if (arg$Symbol$0$2 === "Tuple") {
                        if (arg$Select$1$1 instanceof Block.Symbol.class) {
                          arg$Symbol$0$ = arg$Select$1$1.name;
                          switch (arg$Symbol$0$) {
                            case "get":
                              if (runtime.Tuple.isArrayLike(args1) && args1.length === 2) {
                                element0$1 = runtime.Tuple.get(args1, 0);
                                element1$1 = runtime.Tuple.get(args1, 1);
                                if (element0$1 instanceof Block.Arg.class) {
                                  arg$Arg$0$ = element0$1.value;
                                  if (element1$1 instanceof Block.Arg.class) {
                                    arg$Arg$0$1 = element1$1.value;
                                    litArg = arg$Arg$0$1;
                                    scrut2 = arg$Arg$0$;
                                    recovered = Block.DynSelect(scrut2, litArg, false);
                                    tmp12 = Block.End();
                                    tmp13 = SpecializeHelpers.sop(ctx, recovered);
                                    tmp14 = globalThis.Object.freeze([
                                      tmp12,
                                      recovered,
                                      tmp13
                                    ]);
                                    break split_root$1
                                  } else {
                                    break split_1$1
                                  }
                                } else {
                                  break split_1$1
                                }
                              } else {
                                break split_1$1
                              }
                              break;
                            case "slice":
                              throw runtime.safeCall(globalThis.Error("runtime.Tuple.slice not handled in shape propagation"));
                              break;
                            default:
                              break split_1$1;
                              break;
                          }
                        } else {
                          break split_1$1
                        }
                      } else {
                        break split_1$1
                      }
                    } else {
                      break split_1$1
                    }
                  } else {
                    break split_1$1
                  }
                } else {
                  break split_1$1
                }
              } else {
                break split_1$1
              }
            } else if (arg$Select$0$ instanceof Block.ValueRef.class) {
              arg$ValueRef$0$1 = arg$Select$0$.l;
              if (arg$ValueRef$0$1 instanceof Block.ModuleSymbol.class) {
                arg$ModuleSymbol$0$ = arg$ValueRef$0$1.name;
                arg$ModuleSymbol$1$ = arg$ValueRef$0$1.value;
                if (arg$Select$1$1 instanceof Block.Symbol.class) {
                  arg$Symbol$0$ = arg$Select$1$1.name;
                  symb3 = arg$Symbol$0$;
                  value = arg$ModuleSymbol$1$;
                  name1 = arg$ModuleSymbol$0$;
                  mapPropName = "generatorMap$" + name1;
                  genMap = value[mapPropName];
                  if (genMap === undefined) {
                    tmp15 = true;
                  } else {
                    tmp15 = false;
                  }
                  scrut6 = ! tmp15;
                  if (scrut6 === true) {
                    f1 = runtime.safeCall(genMap.get(symb3));
                    if (f1 instanceof Runtime.Unit.class) {
                      tmp16 = true;
                    } else {
                      tmp16 = false;
                    }
                    scrut7 = ! tmp16;
                    if (scrut7 === true) {
                      res1 = runtime.safeCall(f1(...argShapes));
                      tmp17 = Block.End();
                      tmp18 = Block.ModuleSymbol(name1, value);
                      tmp19 = Block.ValueRef(tmp18);
                      tmp20 = Block.Select(tmp19, res1[0]);
                      tmp21 = Block.Call(tmp20, args1);
                      tmp14 = globalThis.Object.freeze([
                        tmp17,
                        tmp21,
                        res1[1]
                      ]);
                      break split_root$1
                    } else {
                      break split_1$1
                    }
                  } else {
                    scrut8 = runtime.safeCall(argShapes.every(ShapeSet.staticSet));
                    if (scrut8 === true) {
                      f2 = value[symb3];
                      tmp22 = runtime.safeCall(argShapes.map(ShapeSet.valOfSet));
                      evaluated = runtime.safeCall(f2(...tmp22));
                      evaluated_path = ShapeSet.val2path(evaluated, ctx.allocs);
                      tmp23 = SpecializeHelpers.sov(evaluated);
                      tmp14 = globalThis.Object.freeze([
                        evaluated_path[0],
                        evaluated_path[1],
                        tmp23
                      ]);
                      break split_root$1
                    } else {
                      symb4 = arg$ValueRef$0$1;
                      tmp24 = Block.ValueRef(symb4);
                      scrut9 = SpecializeHelpers.sop(ctx, tmp24);
                      if (scrut9 instanceof Shape.Class.class) {
                        scrut9.sym;
                        scrut9.params;
                        tmp14 = runtime.Unit;
                        break split_root$1
                      } else {
                        break split_1$1
                      }
                    }
                  }
                } else {
                  break split_1$1
                }
              } else if (arg$ValueRef$0$1 instanceof Block.ClassSymbol.class) {
                arg$ValueRef$0$1.name;
                arg$ValueRef$0$1.value;
                arg$ValueRef$0$1.paramsOpt;
                arg$ValueRef$0$1.auxParams;
                if (arg$Select$1$1 instanceof Block.Symbol.class) {
                  arg$Symbol$0$ = arg$Select$1$1.name;
                  break split_1$1
                } else {
                  break split_1$1
                }
              } else {
                if (arg$Select$1$1 instanceof Block.Symbol.class) {
                  arg$Symbol$0$ = arg$Select$1$1.name;
                  symb4 = arg$ValueRef$0$1;
                  tmp25 = Block.ValueRef(symb4);
                  scrut9 = SpecializeHelpers.sop(ctx, tmp25);
                  if (scrut9 instanceof Shape.Class.class) {
                    scrut9.sym;
                    scrut9.params;
                    tmp14 = runtime.Unit;
                    break split_root$1
                  } else {
                    break split_1$1
                  }
                } else {
                  break split_1$1
                }
              }
            } else {
              break split_1$1
            }
          } else if (f instanceof Block.ValueRef.class) {
            arg$ValueRef$0$2 = f.l;
            clsSymb1 = arg$ValueRef$0$2;
            if (clsSymb1 instanceof Block.ClassSymbol.class) {
              tmp26 = Block.End();
              lambda3 = (undefined, function (a) {
                return SpecializeHelpers.sop(ctx, a.value)
              });
              tmp27 = runtime.safeCall(args1.map(lambda3));
              tmp28 = ShapeSet.mkClass(clsSymb1, tmp27);
              tmp14 = globalThis.Object.freeze([
                tmp26,
                r,
                tmp28
              ]);
              break split_root$1
            } else {
              symb2 = arg$ValueRef$0$2;
              name = symb2.name;
              if (runtime.Tuple.isArrayLike(args1) && args1.length === 1) {
                element0$1 = runtime.Tuple.get(args1, 0);
                x = element0$1;
                tmp29 = SpecializeHelpers.sop(ctx, x.value);
                scrut3 = runtime.safeCall(tmp29.values());
                if (runtime.Tuple.isArrayLike(scrut3) && scrut3.length === 1) {
                  element0$4 = runtime.Tuple.get(scrut3, 0);
                  if (element0$4 instanceof Shape.Lit.class) {
                    arg$Lit$0$2 = element0$4.l;
                    l = arg$Lit$0$2;
                    switch (name) {
                      case "!":
                        tmp30 = ! l;
                        tmp14 = lit(tmp30);
                        break split_root$1;
                        break;
                      case "-":
                        tmp31 = - l;
                        tmp14 = lit(tmp31);
                        break split_root$1;
                        break;
                      case "+":
                        tmp32 = + l;
                        tmp14 = lit(tmp32);
                        break split_root$1;
                        break;
                      default:
                        break split_1$1;
                        break;
                    }
                  } else {
                    break split_1$1
                  }
                } else {
                  break split_1$1
                }
              } else if (runtime.Tuple.isArrayLike(args1) && args1.length === 2) {
                element0$1 = runtime.Tuple.get(args1, 0);
                element1$1 = runtime.Tuple.get(args1, 1);
                y = element1$1;
                x1 = element0$1;
                tmp33 = SpecializeHelpers.sop(ctx, x1.value);
                scrut5 = runtime.safeCall(tmp33.values());
                if (runtime.Tuple.isArrayLike(scrut5) && scrut5.length === 1) {
                  element0$2 = runtime.Tuple.get(scrut5, 0);
                  if (element0$2 instanceof Shape.Lit.class) {
                    arg$Lit$0$ = element0$2.l;
                    l1 = arg$Lit$0$;
                    tmp34 = SpecializeHelpers.sop(ctx, y.value);
                    scrut4 = runtime.safeCall(tmp34.values());
                    if (runtime.Tuple.isArrayLike(scrut4) && scrut4.length === 1) {
                      element0$3 = runtime.Tuple.get(scrut4, 0);
                      if (element0$3 instanceof Shape.Lit.class) {
                        arg$Lit$0$1 = element0$3.l;
                        l2 = arg$Lit$0$1;
                        switch (name) {
                          case "+":
                            tmp35 = l1 + l2;
                            tmp14 = lit(tmp35);
                            break split_root$1;
                            break;
                          case "-":
                            tmp36 = l1 - l2;
                            tmp14 = lit(tmp36);
                            break split_root$1;
                            break;
                          case "*":
                            tmp37 = l1 * l2;
                            tmp14 = lit(tmp37);
                            break split_root$1;
                            break;
                          case "/":
                            tmp38 = l1 / l2;
                            tmp14 = lit(tmp38);
                            break split_root$1;
                            break;
                          case "%":
                            tmp39 = l1 % l2;
                            tmp14 = lit(tmp39);
                            break split_root$1;
                            break;
                          case "==":
                            tmp40 = Predef.equals(l1, l2);
                            tmp14 = lit(tmp40);
                            break split_root$1;
                            break;
                          case "!=":
                            tmp41 = Predef.nequals(l1, l2);
                            tmp14 = lit(tmp41);
                            break split_root$1;
                            break;
                          case "<":
                            tmp42 = l1 < l2;
                            tmp14 = lit(tmp42);
                            break split_root$1;
                            break;
                          case "<=":
                            tmp43 = l1 <= l2;
                            tmp14 = lit(tmp43);
                            break split_root$1;
                            break;
                          case ">":
                            tmp44 = l1 > l2;
                            tmp14 = lit(tmp44);
                            break split_root$1;
                            break;
                          case ">=":
                            tmp45 = l1 >= l2;
                            tmp14 = lit(tmp45);
                            break split_root$1;
                            break;
                          case "===":
                            tmp46 = l1 === l2;
                            tmp14 = lit(tmp46);
                            break split_root$1;
                            break;
                          case "!==":
                            tmp47 = l1 !== l2;
                            tmp14 = lit(tmp47);
                            break split_root$1;
                            break;
                          default:
                            break split_1$1;
                            break;
                        }
                      } else {
                        break split_1$1
                      }
                    } else {
                      break split_1$1
                    }
                  } else {
                    break split_1$1
                  }
                } else {
                  break split_1$1
                }
              } else {
                break split_1$1
              }
            }
          } else {
            break split_1$1
          }
        }
        tmp48 = Block.End();
        tmp49 = ShapeSet.mkDyn();
        tmp14 = globalThis.Object.freeze([
          tmp48,
          r,
          tmp49
        ]);
      }
      return tmp14
    } else {
      throw globalThis.Object.freeze(new globalThis.Error("match error"))
    }
  } 
  static prop(ctx, b) {
    let res, implct, blk, s1, r1, scrut, rest, symbols, newAllocs, newCtx, res1, x, b1, r, scrut1, blk1, s11, r11, scrut2, b2, s2, restBlock, dflt, arms, p, s, filteredArms, scrut3, scrut4, d, scrut5, newRest, d1, branchCtx, scrut6, newDflt, d2, branchCtx1, scrut7, newDflt1, arg$Match$0$, arg$Match$1$, arg$Match$2$, arg$Match$3$, arg$Assign$0$, arg$Assign$1$, arg$Assign$2$, element2$, element1$, element0$, arg$Scoped$0$, arg$Scoped$1$, arg$Return$0$, arg$Return$1$, element2$1, element1$1, element0$1, tmp, tmp1, tmp2, tmp3, lambda, tmp4, tmp5, tmp6, element1$2, element0$2, tmp7, tmp8, tmp9, tmp10, tmp11, lambda1, tmp12, tmp13, tmp14, tmp15, arg$Some$0$, tmp16, arg$Some$0$1, tmp17, tmp18, tmp19, tmp20, tmp21, tmp22, tmp23, tmp24, tmp25, tmp26, tmp27, tmp28, tmp29, tmp30, tmp31, tmp32, tmp33, tmp34;
    split_root$: {
      split_default$: {
        if (b instanceof Block.End.class) {
          tmp = ShapeSet.mkBot();
          tmp1 = globalThis.Object.freeze([
            b,
            tmp
          ]);
          break split_root$
        } else if (b instanceof Block.Return.class) {
          arg$Return$0$ = b.res;
          arg$Return$1$ = b.implct;
          implct = arg$Return$1$;
          res = arg$Return$0$;
          scrut = SpecializeHelpers.sor(ctx, res);
          if (runtime.Tuple.isArrayLike(scrut) && scrut.length === 3) {
            element0$1 = runtime.Tuple.get(scrut, 0);
            element1$1 = runtime.Tuple.get(scrut, 1);
            element2$1 = runtime.Tuple.get(scrut, 2);
            s1 = element2$1;
            r1 = element1$1;
            blk = element0$1;
            tmp2 = Block.Return(r1, implct);
            tmp3 = Block.concat(blk, tmp2);
            tmp1 = globalThis.Object.freeze([
              tmp3,
              s1
            ]);
            break split_root$
          } else {
            break split_default$
          }
        } else if (b instanceof Block.Scoped.class) {
          arg$Scoped$0$ = b.symbols;
          arg$Scoped$1$ = b.rest;
          rest = arg$Scoped$1$;
          symbols = arg$Scoped$0$;
          lambda = (undefined, function (x1, _, _1) {
            let tmp35, tmp36;
            tmp35 = Block.ValueRef(x1);
            tmp36 = ShapeSet.mkBot();
            return ctx.add(tmp35, tmp36)
          });
          runtime.safeCall(symbols.forEach(lambda));
          newAllocs = [];
          tmp4 = globalThis.Object.freeze(new globalThis.Map(ctx.ctx));
          newCtx = SpecializeHelpers.Ctx(tmp4, newAllocs);
          res1 = SpecializeHelpers.prop(newCtx, rest);
          tmp5 = globalThis.Object.freeze([
            ...symbols,
            ...newAllocs
          ]);
          tmp6 = SpecializeHelpers.wrapScoped(tmp5, res1[0]);
          tmp1 = globalThis.Object.freeze([
            tmp6,
            res1[1]
          ]);
          break split_root$
        } else if (b instanceof Block.Assign.class) {
          arg$Assign$0$ = b.lhs;
          arg$Assign$1$ = b.rhs;
          arg$Assign$2$ = b.rest;
          b1 = arg$Assign$2$;
          r = arg$Assign$1$;
          x = arg$Assign$0$;
          scrut1 = SpecializeHelpers.sor(ctx, r);
          if (runtime.Tuple.isArrayLike(scrut1) && scrut1.length === 3) {
            element0$ = runtime.Tuple.get(scrut1, 0);
            element1$ = runtime.Tuple.get(scrut1, 1);
            element2$ = runtime.Tuple.get(scrut1, 2);
            s11 = element2$;
            r11 = element1$;
            blk1 = element0$;
            tmp7 = Block.ValueRef(x);
            tmp8 = ctx.add(tmp7, s11);
            scrut2 = SpecializeHelpers.prop(tmp8, b1);
            if (runtime.Tuple.isArrayLike(scrut2) && scrut2.length === 2) {
              element0$2 = runtime.Tuple.get(scrut2, 0);
              element1$2 = runtime.Tuple.get(scrut2, 1);
              s2 = element1$2;
              b2 = element0$2;
              tmp9 = Block.Assign(x, r11, b2);
              tmp10 = Block.concat(blk1, tmp9);
              tmp11 = globalThis.Object.freeze([
                tmp10,
                s2
              ]);
            } else {
              throw globalThis.Object.freeze(new globalThis.Error("match error"))
            }
            tmp1 = tmp11;
            break split_root$
          } else {
            break split_default$
          }
        } else if (b instanceof Block.Match.class) {
          arg$Match$0$ = b.scrut;
          arg$Match$1$ = b.arms;
          arg$Match$2$ = b.dflt;
          arg$Match$3$ = b.rest;
          restBlock = arg$Match$3$;
          dflt = arg$Match$2$;
          arms = arg$Match$1$;
          p = arg$Match$0$;
          s = SpecializeHelpers.sop(ctx, p);
          lambda1 = (undefined, function (r2, arm) {
            let fs, scrut8, branchCtx2, scrut9, res2, tmp35, tmp36, tmp37, tmp38, tmp39, tmp40;
            fs = ShapeSet.filterSet(r2[0], arm.cse);
            scrut8 = runtime.safeCall(fs.isEmpty());
            if (scrut8 === true) {
              return r2
            } else {
              branchCtx2 = ctx.clone;
              if (p instanceof Block.ValueLit.class) {
                tmp35 = true;
              } else {
                tmp35 = false;
              }
              scrut9 = ! tmp35;
              if (scrut9 === true) {
                branchCtx2.add(p, fs);
              }
              tmp36 = Block.concat(arm.body, restBlock);
              res2 = SpecializeHelpers.prop(branchCtx2, tmp36);
              tmp37 = ShapeSet.restSet(r2[0], arm.cse);
              tmp38 = ShapeSet.union(r2[1], res2[1]);
              tmp39 = Block.Arm(arm.cse, res2[0]);
              tmp40 = globalThis.Object.freeze([
                ...r2[2],
                tmp39
              ]);
              return globalThis.Object.freeze([
                tmp37,
                tmp38,
                tmp40
              ])
            }
          });
          tmp12 = runtime.safeCall(Predef.foldl(lambda1));
          tmp13 = ShapeSet.mkBot();
          tmp14 = globalThis.Object.freeze([]);
          tmp15 = globalThis.Object.freeze([
            s,
            tmp13,
            tmp14
          ]);
          filteredArms = runtime.safeCall(tmp12(tmp15, ...arms));
          split_root$1: {
            split_1$: {
              scrut3 = filteredArms[2].length;
              switch (scrut3) {
                case 0:
                  scrut4 = runtime.safeCall(filteredArms[0].isEmpty());
                  if (scrut4 === true) {
                    tmp16 = SpecializeHelpers.prop(ctx, restBlock);
                    break split_root$1
                  } else {
                    if (dflt instanceof Option.Some.class) {
                      arg$Some$0$1 = dflt.value;
                      d = arg$Some$0$1;
                      tmp17 = d;
                    } else {
                      tmp17 = Block.End();
                    }
                    tmp18 = Block.concat(tmp17, restBlock);
                    tmp16 = SpecializeHelpers.prop(ctx, tmp18);
                    break split_root$1
                  }
                  break;
                case 1:
                  scrut5 = runtime.safeCall(filteredArms[0].isEmpty());
                  if (scrut5 === true) {
                    newRest = SpecializeHelpers.prop(ctx, restBlock);
                    tmp19 = Block.concat(filteredArms[2][0].body, newRest[0]);
                    tmp20 = ShapeSet.union(filteredArms[1], newRest[1]);
                    tmp16 = globalThis.Object.freeze([
                      tmp19,
                      tmp20
                    ]);
                    break split_root$1
                  } else {
                    if (dflt instanceof Option.Some.class) {
                      arg$Some$0$ = dflt.value;
                      d1 = arg$Some$0$;
                      branchCtx = ctx.clone;
                      if (p instanceof Block.ValueLit.class) {
                        tmp21 = true;
                      } else {
                        tmp21 = false;
                      }
                      scrut6 = ! tmp21;
                      if (scrut6 === true) {
                        branchCtx.add(p, filteredArms[0]);
                      }
                      tmp22 = Block.concat(d1, restBlock);
                      newDflt = SpecializeHelpers.prop(branchCtx, tmp22);
                      tmp23 = Option.Some(newDflt[0]);
                      tmp24 = Block.End();
                      tmp25 = Block.Match(p, filteredArms[2], tmp23, tmp24);
                      tmp26 = ShapeSet.union(filteredArms[1], newDflt[1]);
                      tmp16 = globalThis.Object.freeze([
                        tmp25,
                        tmp26
                      ]);
                      break split_root$1
                    } else {
                      break split_1$
                    }
                  }
                  break;
                default:
                  if (dflt instanceof Option.Some.class) {
                    arg$Some$0$ = dflt.value;
                    d2 = arg$Some$0$;
                    branchCtx1 = ctx.clone;
                    if (p instanceof Block.ValueLit.class) {
                      tmp27 = true;
                    } else {
                      tmp27 = false;
                    }
                    scrut7 = ! tmp27;
                    if (scrut7 === true) {
                      branchCtx1.add(p, filteredArms[0]);
                    }
                    tmp28 = Block.concat(d2, restBlock);
                    newDflt1 = SpecializeHelpers.prop(branchCtx1, tmp28);
                    tmp29 = Option.Some(newDflt1[0]);
                    tmp30 = Block.End();
                    tmp31 = Block.Match(p, filteredArms[2], tmp29, tmp30);
                    tmp32 = ShapeSet.union(filteredArms[1], newDflt1[1]);
                    tmp16 = globalThis.Object.freeze([
                      tmp31,
                      tmp32
                    ]);
                    break split_root$1
                  } else {
                    break split_1$
                  }
                  break;
              }
            }
            tmp33 = Block.End();
            tmp34 = Block.Match(p, filteredArms[2], Option.None, tmp33);
            tmp16 = globalThis.Object.freeze([
              tmp34,
              filteredArms[1]
            ]);
          }
          tmp1 = tmp16;
          break split_root$
        } else {
          break split_default$
        }
      }
      throw globalThis.Object.freeze(new globalThis.Error("match error"));
    }
    return tmp1
  } 
  static propStub(ctx, body) {
    let tmp;
    runtime.safeCall(globalThis.console.log("Calling the propStub now which performs no shape propagation"));
    tmp = ShapeSet.mkDyn();
    return globalThis.Object.freeze([
      body,
      tmp
    ])
  } 
  static buildShapeName(s) {
    let lit, lit1, shapes, params, sym, arg$Class$0$, arg$Class$1$, arg$Arr$0$, arg$Lit$0$, tmp, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6;
    if (s instanceof Shape.Dyn.class) {
      return "Dyn"
    } else if (s instanceof Shape.Lit.class) {
      arg$Lit$0$ = s.l;
      lit = arg$Lit$0$;
      if (typeof lit === 'string') {
        return "Str" + lit
      } else {
        lit1 = arg$Lit$0$;
        tmp = runtime.safeCall(lit1.toString());
        return "Lit" + tmp
      }
    } else if (s instanceof Shape.Arr.class) {
      arg$Arr$0$ = s.shapes;
      shapes = arg$Arr$0$;
      tmp1 = runtime.safeCall(shapes.map(SpecializeHelpers.buildShapeName));
      tmp2 = runtime.safeCall(tmp1.join("_"));
      tmp3 = "Arr_" + tmp2;
      return tmp3 + "_end"
    } else if (s instanceof Shape.Class.class) {
      arg$Class$0$ = s.sym;
      arg$Class$1$ = s.params;
      params = arg$Class$1$;
      sym = arg$Class$0$;
      tmp4 = sym.name + "_";
      tmp5 = runtime.safeCall(params.map(SpecializeHelpers.buildShapeName));
      tmp6 = runtime.safeCall(tmp5.join("_"));
      return tmp4 + tmp6
    } else {
      return "Unk"
    }
  } 
  static buildShapeSetName(ss) {
    let vals, scrut, tmp, tmp1, tmp2;
    vals = runtime.safeCall(ss.values());
    scrut = Predef.equals(vals.length, 1);
    if (scrut === true) {
      return SpecializeHelpers.buildShapeName(vals[0])
    } else {
      tmp = runtime.safeCall(vals.map(SpecializeHelpers.buildShapeName));
      tmp1 = runtime.safeCall(tmp.join("_"));
      tmp2 = "Union_" + tmp1;
      return tmp2 + "_end"
    }
  } 
  static specializeName(funName, shapes) {
    let scrut, lambda, tmp, lambda1, tmp1, tmp2;
    lambda = (undefined, function (ps) {
      let lambda2;
      lambda2 = (undefined, function (s) {
        return runtime.safeCall(s.isDyn())
      });
      return runtime.safeCall(ps.every(lambda2))
    });
    scrut = runtime.safeCall(shapes.every(lambda));
    if (scrut === true) {
      return funName
    } else {
      tmp = funName + "_";
      lambda1 = (undefined, function (ps) {
        let tmp3;
        tmp3 = runtime.safeCall(ps.map(SpecializeHelpers.buildShapeSetName));
        return runtime.safeCall(tmp3.join("_"))
      });
      tmp1 = runtime.safeCall(shapes.map(lambda1));
      tmp2 = runtime.safeCall(tmp1.join("__"));
      return tmp + tmp2
    }
  } 
  static specialize(cache, funName, dflt, shapes) {
    let newName, scrut, x, defn, body, ps, ctx, res, bodyWithScoped, entry, arg$FunDefn$0$, arg$FunDefn$1$, arg$FunDefn$2$, arg$Some$0$, tmp, lambda, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
    newName = SpecializeHelpers.specializeName(funName, shapes);
    split_root$: {
      split_default$: {
        scrut = runtime.safeCall(cache.getFun(newName));
        if (scrut instanceof Option.Some.class) {
          arg$Some$0$ = scrut.value;
          x = arg$Some$0$;
          tmp = globalThis.Object.freeze([
            x[0].sym,
            x[1]
          ]);
          break split_root$
        } else if (scrut instanceof Option.None.class) {
          defn = runtime.safeCall(dflt());
          if (defn instanceof Block.FunDefn.class) {
            arg$FunDefn$0$ = defn.sym;
            arg$FunDefn$1$ = defn.params;
            arg$FunDefn$2$ = defn.body;
            if (arg$FunDefn$0$ instanceof Block.Symbol.class) {
              arg$FunDefn$0$.name;
              body = arg$FunDefn$2$;
              ps = arg$FunDefn$1$;
              ctx = SpecializeHelpers.Ctx.class.empty;
              lambda = (undefined, function (p, i, _) {
                let lambda1;
                lambda1 = (undefined, function (p2, j, _1) {
                  let tmp8;
                  tmp8 = Block.ValueRef(p2);
                  return ctx.add(tmp8, shapes[i][j])
                });
                return runtime.safeCall(p.forEach(lambda1))
              });
              runtime.safeCall(ps.forEach(lambda));
              tmp1 = Block.Symbol(newName);
              tmp2 = Block.FunDefn(tmp1, ps, body);
              tmp3 = ShapeSet.mkDyn();
              tmp4 = globalThis.Object.freeze([
                tmp2,
                tmp3
              ]);
              cache.setFun(newName, tmp4);
              res = SpecializeHelpers.prop(ctx, body);
              bodyWithScoped = SpecializeHelpers.wrapScoped(ctx.allocs, res[0]);
              tmp5 = Block.Symbol(newName);
              tmp6 = Block.FunDefn(tmp5, ps, bodyWithScoped);
              tmp7 = globalThis.Object.freeze([
                tmp6,
                res[1]
              ]);
              entry = cache.setFun(newName, tmp7);
              tmp = globalThis.Object.freeze([
                entry[0].sym,
                entry[1]
              ]);
              break split_root$
            } else {
              break split_default$
            }
          } else {
            break split_default$
          }
        } else {
          break split_default$
        }
      }
      throw globalThis.Object.freeze(new globalThis.Error("match error"));
    }
    return tmp
  }
  toString() { return runtime.render(this); }
  static [definitionMetadata] = ["class", "SpecializeHelpers"]; 
});
let SpecializeHelpers = SpecializeHelpers1; export default SpecializeHelpers;
"""))
