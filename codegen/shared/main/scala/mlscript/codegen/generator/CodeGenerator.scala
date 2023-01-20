package mlscript.codegen.generator

import mlscript.codegen.LocationType
import mlscript.codegen.ast._
import mlscript.codegen.LocationType
import mlscript.codegen.generator.Parentheses

class CodeGenerator(
  ast: Node,
  format: Format,
  sourceMap: SourceMapBuilder
) extends Printer(format, sourceMap):

  override def print(node: Node, parent: Option[Node])(implicit options: PrinterOptions): Unit = node match
    // BEGIN classes.ts
    case node @ ClassDeclaration(id, superClass, body, decorators) =>
      printJoin(decorators, node, PrintSequenceOptions())
      if (node.declare.getOrElse(false)) {
        word("declare")
        space()
      }
      if (node.`abstract`.getOrElse(false)) {
        word("abstract")
        space()
      }
      word("class")
      space()
      print(Some(id), Some(node))
      print(node.typeParameters, Some(node))
      superClass.foreach { superClassExpr =>
        space()
        word("extends")
        space()
        print(superClass, Some(node))
        print(node.superTypeParameters, Some(node))
      }
      node.implements.foreach { implements =>
        space()
        word("implements")
        space()
        printList(implements, node, PrintSequenceOptions())
      }
      space()
      print(Some(node.body), Some(node))
    case node @ ClassExpression(id, superClass, body, decorators) =>
      // TODO: Extract common fields from `ClassDeclaration` and `ClassExpression`
      // to `Class` and handle them 
      ???
    case node @ ClassBody(body) =>
      token("{")
      body match
        case Nil => token("}")
        case _ =>
          newline()
          indent()
          printSequence(body, node, PrintSequenceOptions())
          dedent()
          if (!endsWith('\n')) newline()
          sourceWithOffset(LocationType.End, node.location, 0, -1)
          rightBrace()
    case node @ ClassProperty(key, value, typeAnnotation, decorators, computed, static) =>
      printJoin(decorators, node, PrintSequenceOptions())
      node.key.location.flatMap(_.end.map(_.line)).foreach(catchUp)
      tsPrintClassMemberModifiers(node)
      if (node.computed) {
        token("[")
        print(Some(key), Some(node))
        token("]")
      } else {
        variance(node)
        print(Some(key), Some(node))
      }
      if (node.optional.getOrElse(false)) token("?")
      if (node.definite.getOrElse(false)) token("!")
      print(typeAnnotation, Some(node))
      value.foreach { _ =>
        space()
        token("=")
        space()
        print(value, Some(node))
      }
      semicolon()
    case node @ ClassAccessorProperty(key, value, typeAnnotation, decorators, computed, static) =>
      printJoin(decorators, node, PrintSequenceOptions())
      node.key.location.flatMap(_.end).map(_.line).foreach(catchUp)
      tsPrintClassMemberModifiers(node)
      word("accessor", true)
      space()
      if (node.computed) {
        token("[")
        print(Some(key), Some(node))
        token("]")
      } else {
        variance(node)
        print(Some(key), Some(node))
      }
      if (node.optional.getOrElse(false)) token("?")
      if (node.definite.getOrElse(false)) token("!")
      print(typeAnnotation, Some(node))
      value.foreach { _ =>
        space()
        token("=")
        space()
        print(value, Some(node))
      }
      semicolon()
    case StaticBlock(body) =>
      word("static")
      space()
      token("{")
      body match
        case Nil => token("}")
        case _ =>
          newline()
          printSequence(body, node, PrintSequenceOptions(indent = Some(true)))
          sourceWithOffset(LocationType.End, node.location, 0, -1)
          rightBrace()
    case p @ ClassPrivateProperty(key, value, dec, static) =>
      printJoin(dec, node, PrintSequenceOptions())
      if (static) word("static"); space()
      print(Some(key), Some(node))
      print(p.typeAnnotation, Some(node))
      if (value.isDefined) {
        space(); token("="); space()
        print(value, Some(node))
      }
      semicolon()
    case method @ ClassMethod(kind, key, params, body, computed, _, generator, async) =>
      printJoin(method.decorators, node, PrintSequenceOptions())
      // TODO: catch up
      tsPrintClassMemberModifiers(method)
      kind match {
        case Some(ClassMethodKind.Getter) => word("get"); space()
        case Some(ClassMethodKind.Setter) => word("set"); space()
        case _ => ()
      }
      if (async) word("async", true)
      kind match {
        case Some(ClassMethodKind.Method) if (generator) => token("*")
        case _ => ()
      }
      if (computed) {
        token("[")
        print(Some(key), Some(node))
        token("]")
      }
      else print(Some(key), Some(node))
      print(method.typeParameters, Some(node))
      token("(")
      parameters(params, method)
      token(")")
      print(method.returnType, Some(node), false)
      _noLineTerminator = false
      space()
      print(Some(body), Some(node))
    case method @ ClassPrivateMethod(kind, key, params, body, _) =>
      printJoin(method.decorators, node, PrintSequenceOptions())
      // TODO: catch up
      tsPrintClassMemberModifiers(method)
      kind match {
        case Some(ClassPrivateMethodKind.Getter) => word("get"); space()
        case Some(ClassPrivateMethodKind.Setter) => word("set"); space()
        case _ => ()
      }
      if (method.async.getOrElse(false)) word("async", true)
      kind match {
        case Some(ClassPrivateMethodKind.Method) if (method.generator.getOrElse(false)) => token("*")
        case _ => ()
      }
      if (method.computed.getOrElse(false)) {
        token("[")
        print(Some(key), Some(node))
        token("]")
      }
      else print(Some(key), Some(node))
      print(method.typeParameters, Some(node))
      token("(")
      parameters(params, method)
      token(")")
      print(method.returnType, Some(node), false)
      _noLineTerminator = false
      space()
      print(Some(body), Some(node))
    // END classes.ts
    // BEGIN flow.ts
    
    // END flow.ts
    // BEGIN jsx.ts
    case JSXAttribute(name, value) =>
      print(Some(name), Some(node))
      value.foreach { _ =>
        token("=")
        print(value, Some(node))
      }
    case JSXIdentifier(name) => word(name)
    case JSXNamespacedName(namespace, name) =>
      print(Some(namespace), Some(node))
      token(":")
      print(Some(name), Some(node))
    case JSXSpreadAttribute(argument) =>
      token("{")
      token("...")
      print(Some(argument), Some(node))
      token("}")
    case JSXExpressionContainer(expression) =>
      token("{")
      print(Some(expression), Some(node))
      token("}")
    case JSXSpreadChild(expression) =>
      token("{")
      token("...")
      print(Some(expression), Some(node))
      token("}")
    case JSXText(value) =>
      getPossibleRaw(node) match
        case None => token(value, true)
        case Some(raw) => token(raw, true)
    case JSXElement(openingElement, closingElement, children, selfClosing) =>
      print(Some(openingElement), Some(node))
      if (!openingElement.selfClosing) {
        indent()
        children.foreach { child => print(Some(child), Some(node)) }
        dedent()
        print(closingElement, Some(node))
      }
    case node @ JSXOpeningElement(name, attributes, selfClosing) =>
      token("<")
      print(Some(name), Some(node))
      print(node.typeParameters, Some(node)) // TS
      if (!attributes.isEmpty) {
        space()
        printJoin(Some(attributes), node, PrintSequenceOptions(separator = Some((p: Printer)=>p.space())))
      }
      if (selfClosing) {
        space()
        token("/>")
      } else {
        token(">")
      }
    case JSXClosingElement(name) =>
      token("</")
      print(Some(name), Some(node))
      token(">")
    case JSXEmptyExpression() =>
      printInnerComments()
    case JSXFragment(openingFragment, closingFragment, children) =>
      print(Some(openingFragment), Some(node))
      indent()
      children.foreach { child => print(Some(child), Some(node)) }
      dedent()
      print(Some(closingFragment), Some(node))
    case JSXOpeningFragment() =>
      token("<")
      token(">")
    case JSXClosingFragment() =>
      token("</")
      token(">")
    // END jsx.ts

    // BEGIN flow.ts
    case AnyTypeAnnotation() => word("any")
    case BooleanTypeAnnotation() => word("boolean")
    case NullLiteralTypeAnnotation() => word("null")
    case ExistsTypeAnnotation() => token("*")
    case MixedTypeAnnotation() => word("mixed")
    case EmptyTypeAnnotation() => word("empty")
    case NumberTypeAnnotation() => word("number")
    case StringTypeAnnotation() => word("string")
    case ThisTypeAnnotation() => word("this")
    case SymbolTypeAnnotation() => word("symbol")
    case VoidTypeAnnotation() => word("void")
    case BooleanLiteralTypeAnnotation(value) =>
      if (value) word("true")
      else word("false")
    case InferredPredicate() => { token("%"); word("checks") }
    case Variance(kind) => kind match {
      case VarianceKind.Contravariant => token("+")
      case VarianceKind.Covariant => token("-")
    }
    // To be continued...
    // END flow.ts
    // BEGIN methods.ts
    case exp @ FunctionExpression(_, _, body, _, _) =>
      functionHead(exp)
      space()
      print(Some(body), Some(node))
    case dec @ FunctionDeclaration(_, _, body, _, _) =>
      functionHead(dec)
      space()
      print(Some(body), Some(node))
    case exp @ ArrowFunctionExpression(params, body, async, _) =>
      if (async) word("async", true); space()
      print(exp.typeParameters, Some(node))
      token("(")
      parameters(params, node)
      token(")")
      print(exp.returnType, Some(node))
      _noLineTerminator = true
      predicate(exp, true)
      space()
      printInnerComments()
      token("=>")
      space()
      print(Some(body), Some(node))
    // END methods.ts

    // BEGIN modules.ts
    case node @ ImportSpecifier(local, imported) =>
      node.importKind match
        case Some(ImportKind.Type) =>
          word("type")
          space()
        case Some(ImportKind.TypeOf) =>
          word("typeof")
          space()
        case _ => ()
      print(Some(imported), Some(node))
      imported match
        case Identifier(name) if local.name != name =>
          space()
          word("as")
          space()
          print(Some(local), Some(node))
        case _ => ()
    case ImportDefaultSpecifier(local) =>
      print(Some(local), Some(node))
    case ExportDefaultSpecifier(exported) =>
      print(Some(exported), Some(node))
    case node @ ExportSpecifier(local, exported) =>
      node.exportKind match
        case Some(ExportKind.Type) =>
          word("type")
          space()
        case _ => ()
      print(Some(local), Some(node))
      exported match
        case Identifier(name) if local.name != name =>
          space()
          word("as")
          space()
          print(Some(local), Some(node))
        case _ => ()
      
    case ExportNamespaceSpecifier(exported) =>
      token("*")
      space()
      word("as")
      space()
      print(Some(exported), Some(node))
    // To be continued...
    // END modules.ts
    // BEGIN statements.ts
    case s @ WithStatement(obj, _) =>
      word("with")
      space()
      token("(")
      print(Some(obj), Some(node))
      token(")")
      printBlock(s.body, node)
    case IfStatement(test, cons, alt) =>
      word("if"); space(); token("(")
      print(Some(test), Some(node))
      token(")"); space()
      token("{"); newline(); indent()
      printAndIndentOnComments(cons, node)
      dedent(); newline(); token("}")
      alt match {
        case Some(alt) =>
          space(); word("else")
          printAndIndentOnComments(alt, node)
        case _ => ()
      }
    case ForStatement(init, test, update, body) =>
      word("for"); space(); token("(")
      print(init, Some(node))(options.derive(inFor = options.inForStatementInitCounter + 1))
      token(";")
      if (test.isDefined) space(); print(test, Some(node))
      token(";")
      if (update.isDefined) space(); print(update, Some(node))
      token(")")
      printBlock(body, node)
    case WhileStatement(test, body) =>
      word("while"); space(); token("(")
      print(Some(test), Some(node))
      token(")")
      printBlock(body, node)
    case ForInStatement(left, right, body) =>
      word("for"); space()
      noIndentInnerCommentsHere()
      token("(")
      print(Some(left), Some(node))
      space(); word("in"); space()
      print(Some(right), Some(node))
      token(")")
      printBlock(body, node)
    case ForOfStatement(left, right, body, await) =>
      word("for"); space()
      if (await) word("await"); space()
      noIndentInnerCommentsHere()
      token("(")
      print(Some(left), Some(node))
      space(); word("of"); space()
      print(Some(right), Some(node))
      token(")")
      printBlock(body, node)
    case DoWhileStatement(test, body) =>
      word("do"); space()
      print(Some(body), Some(node))
      space(); word("while"); space(); token("(")
      print(Some(test), Some(body))
      token(")"); semicolon()
    case BreakStatement(label) =>
      word("break")
      printStatementAfterKeyword(label, node, true)
    case ContinueStatement(label) =>
      word("continue")
      printStatementAfterKeyword(label, node, true)
    case ReturnStatement(arg) =>
      word("return")
      printStatementAfterKeyword(arg, node, false)
    case ThrowStatement(arg) =>
      word("throw")
      printStatementAfterKeyword(Some(arg), node, false)
    case LabeledStatement(label, body) =>
      print(Some(label), Some(node))
      token(";"); space()
      print(Some(body), Some(node))
    case TryStatement(block, handler, finalizer) =>
      word("try"); space()
      print(Some(block), Some(node))
      space()
      print(handler, Some(node))
      if (finalizer.isDefined) space(); word("finally"); space(); print(finalizer, Some(node))
    case CatchClause(param, body) =>
      word("catch"); space()
      param match {
        case Some(p) =>
          token("(")
          print(param, Some(node))
          p match {
            case i: Identifier => print(i.typeAnnotation, Some(node))
            case p: ArrayPattern => print(p.typeAnnotation, Some(node))
            case p: ObjectPattern => print(p.typeAnnotation, Some(node))
          }
          token(")"); space()
        case _ => ()
      }
      print(Some(body), Some(node))
    case SwitchStatement(dis, cases) =>
      word("switch"); space(); token("(")
      print(Some(dis), Some(node))
      token(")"); space(); token("{")
      printSequence(cases, node, PrintSequenceOptions(indent = Some(true)))
      token("}")
    case SwitchCase(test, cons) =>
      test match {
        case Some(_) =>
          word("case"); space()
          print(test, Some(node))
          token(":")
        case _ =>
          word("default"); token(":")
      }
      if (!cons.isEmpty) newline(); printSequence(cons, node, PrintSequenceOptions(indent = Some(true)))
    case DebuggerStatement() => word("debugger"); semicolon()
    case vd @ VariableDeclaration(kind, decs) =>
      if (vd.declare.getOrElse(false)) word("declare"); space()
      kind match {
        case VariableDeclarationKind.Const => word("const", false)
        case VariableDeclarationKind.Let => word("let", false)
        case VariableDeclarationKind.Using => word("using", true)
        case VariableDeclarationKind.Var => word("var", false)
      }
      space()
      val isFor = parent match {
        case Some(p) => CodeGenerator.isFor(p)
        case _ => false
      }
      val hasInits = if (!isFor) decs.foldLeft(false)((r, d) => if (d.init.isDefined) true else r) else false
      if (hasInits)
        printList(decs, node, PrintSequenceOptions(separator = Some((p: Printer) => {
          p.token(","); p.newline()
        }), indent = Some(decs.length > 1)))
      else
        printList(decs, node, PrintSequenceOptions(indent = Some(decs.length > 1)))
      if (isFor)
        parent match {
          case Some(p: ForStatement) =>
            p.init match {
              case Some(value) if (value == node) => ()
              case _ => semicolon()
            }
          case Some(p: ForInStatement) if (p.left == node) => ()
          case Some(p: ForOfStatement) if (p.left == node) => ()
          case _ => semicolon()
        }
      else semicolon()
    case dec @ VariableDeclarator(id, init) =>
      print(Some(id), Some(node))
      if (dec.definite.getOrElse(false)) token("!")
      if (init.isDefined) {
        space(); token("="); space()
        print(init, Some(node))
      }
    // END statements.ts
    // BEGIN types.ts
    case Identifier(name) => word(name)
    case _: ArgumentPlaceholder => token("?")
    case RestElement(arg) =>
      token("...")
      print(Some(arg), Some(node))
    case SpreadElement(arg) =>
      token("...")
      print(Some(arg), Some(node))
    case exp @ ObjectExpression(prop) =>
      token("{")
      if (!prop.isEmpty) {
        space()
        printList(prop, node, PrintSequenceOptions(indent = Some(true), statement = Some(true)))
        space()
      }
      sourceWithOffset(LocationType.End, exp.location, 0, -1)
      token("}")
    case p @ ObjectPattern(prop) =>
      token("{")
      if (!prop.isEmpty) {
        space()
        printList(prop, node, PrintSequenceOptions(indent = Some(true), statement = Some(true)))
        space()
      }
      sourceWithOffset(LocationType.End, p.location, 0, -1)
      token("}")
    case m @ ObjectMethod(kind, key, params, body, computed, generator, async) =>
      printJoin(m.decorators, node, PrintSequenceOptions())
      kind match {
        case Some(ObjectMethodKind.Getter) => word("get"); space()
        case Some(ObjectMethodKind.Setter) => word("set"); space()
        case _ => ()
      }
      if (async) word("async", true)
      kind match {
        case Some(ObjectMethodKind.Method) if (generator) => token("*")
        case Some(ObjectMethodKind.Init) if (generator) => token("*")
        case _ => ()
      }
      if (computed) {
        token("[")
        print(Some(key), Some(node))
        token("]")
      }
      else print(Some(key), Some(node))
      print(m.typeParameters, Some(node))
      token("(")
      parameters(params, m)
      token(")")
      print(m.returnType, Some(node), false)
      _noLineTerminator = false
      space()
      print(Some(body), Some(node))
    case ObjectProperty(key, value, computed, shorthand, dec) =>
      printJoin(dec, node, PrintSequenceOptions())
      if (computed) {
        token("[")
        print(Some(key), Some(node))
        token("]")

        token(":"); space()
        print(Some(value), Some(node))
      }
      else {
        print(Some(key), Some(node))

        if (!shorthand)
          key match {
            case key: Identifier => value match {
              case value: Identifier if (key.name.equals(value.name)) =>
                token(":"); space()
                print(Some(value), Some(node))
              case _ => ()
            }
            case _ => ()
          }
      }
    case ArrayExpression(ele) =>
      token("[")
      ele.iterator.zipWithIndex.foreach((e, i) => e match {
        case Some(_) =>
          if (i > 0) space()
          print(e, Some(node))
          if (i < ele.length - 1) token(",")
        case _ => token(",")
      })
      token("]")
    case ArrayPattern(ele) =>
      token("[")
      ele.iterator.zipWithIndex.foreach((e, i) => e match {
        case Some(_) =>
          if (i > 0) space()
          print(e, Some(node))
          if (i < ele.length - 1) token(",")
        case _ => token(",")
      })
      token("]")
    case RecordExpression(props) =>
      token("{|")
      if (!props.isEmpty) {
        space()
        printList(props, node, PrintSequenceOptions(indent = Some(true), statement = Some(true)))
        space()
      }
      token("|}")
    case TupleExpression(ele) =>
      token("[|")
      ele.iterator.zipWithIndex.foreach((e, i) => {
        if (i > 0) space()
        print(Some(e), Some(node))
        if (i < ele.length - 1) token(",")
      })
      token("|]")
    case RegExpLiteral(pattern, flags) => word(s"/$pattern/$flags")
    case BooleanLiteral(value) => if (value) word("true") else word("false")
    case StringLiteral(s) => token(s)
    case NullLiteral() => word("null")
    case NumericLiteral(value) => number(value.toString())
    case BigIntLiteral(value) => word(value)
    case DecimalLiteral(value) => word(value)
    case TopicReference() => token("#")
    case PipelineTopicExpression(exp) => print(Some(exp), Some(node))
    case PipelineBareFunction(callee) => print(Some(callee), Some(node))
    case PipelinePrimaryTopicReference() => token("#")
    // END types.ts
    case ThisExpression() => word("this")
    case Super() => word("super")
    case Import() => word("import")
    case EmptyStatement() => semicolon(true)
    case TSAnyKeyword() => word("any")
    case TSBigIntKeyword() => word("bigint")
    case TSUnknownKeyword() => word("unknown")
    case TSNumberKeyword() => word("number")
    case TSObjectKeyword() => word("object")
    case TSBooleanKeyword() => word("boolean")
    case TSStringKeyword() => word("string")
    case TSSymbolKeyword() => word("symbol")
    case TSVoidKeyword() => word("void")
    case TSUndefinedKeyword() => word("undefined")
    case TSNullKeyword() => word("null")
    case TSNeverKeyword() => word("never")
    case TSIntrinsicKeyword() => word("intrinsic")
    case TSThisType() => word("this")
    case InterpreterDirective(value) => { token(s"#!${value}"); newline(1, true) }
    case Placeholder(expected, name) => {
      token("%%")
      print(Some(name), Some(node))
      token("%%")

      if (expected == PlaceholderExpectedNode.Statement) semicolon()
    }
    case File(program, _, _) => {
      print(program.interpreter, Some(node))
      print(Some(program), Some(node))
    }
    case Program(body, directives, _, _, _) => {
      noIndentInnerCommentsHere()
      printInnerComments()
      if (directives.length > 0) {
        val newlines = if (body.length > 0) 2 else 1
        printSequence(directives, node, PrintSequenceOptions(
          trailingCommentsLineOffset = Some(newlines)
        ))

        directives.last.trailingComments match {
          case Some(comments) if (comments.length > 0) => newline(newlines)
          case _ => ()
        }
      }

      printSequence(body, node, PrintSequenceOptions())
    }
    case BlockStatement(body, directives) => {
      token("{")
      if (directives.length > 0) {
        val newlines = if (body.length > 0) 2 else 1
        printSequence(directives, node, PrintSequenceOptions(
          indent = Some(true), trailingCommentsLineOffset = Some(newlines)
        ))

        directives.last.trailingComments match {
          case Some(comments) if (comments.length > 0) => newline(newlines)
          case _ => ()
        }
      }

      printSequence(body, node, PrintSequenceOptions(indent = Some(true)))

      sourceWithOffset(LocationType.End, node.location, 0, -1)
      rightBrace()
    }
    case Directive(value) => {
      print(Some(value), Some(node))
      semicolon()
    }
    case DirectiveLiteral(value) => {
      val raw = getPossibleRaw(node)
      if (!format.minified && raw.isDefined)
        token(raw.get)
      else {
        val unescapedSingleQuoteRE = "(?:^|[^\\])(?:\\\\)*'".r
        val unescapedDoubleQuoteRE = "(?:^|[^\\])(?:\\\\)*\"".r

        if ((unescapedDoubleQuoteRE findFirstIn value).isEmpty)
          token(s"\"$value\"")
        else if ((unescapedSingleQuoteRE findFirstIn value).isEmpty)
          token(s"'$value'")
        else
          throw new Exception("Malformed AST: it is not possible to print a directive containing both unescaped single and double quotes.")
      }
    }
    case UnaryExpression(operator, argument, _) => {
      operator match {
        case UnaryOperator.Void => word("void"); space()
        case UnaryOperator.Delete => word("delete"); space()
        case UnaryOperator.TypeOf => word("typeof"); space()
        case UnaryOperator.Throw => word("throw"); space()
        case UnaryOperator.BitwiseNot => token("~")
        case UnaryOperator.LogicalNot => token("!")
        case UnaryOperator.Negation => token("-")
        case UnaryOperator.Plus => token("+")
      }

      print(Some(argument), Some(node))
    }
    case DoExpression(body, async) => {
      if (async) {
        word("async", true)
        space()
      }

      word("do")
      space()
      print(Some(body), Some(node))
    }
    case ParenthesizedExpression(exp) => {
      token("(")
      print(Some(exp), Some(node))
      token(")")
    }
    case UpdateExpression(op, arg, prefix) =>
      val s = op match {
        case UpdateOperator.Decrement => "--"
        case UpdateOperator.Increment => "++"
      }
      if (prefix) {
        token(s)
        print(Some(arg), Some(node))
      }
      else {
        printTerminatorless(arg, node, true)
        token(s)
      }
    case ConditionalExpression(test, cons, alt) => {
      print(Some(test), Some(node))
      space(); token("?"); space()
      print(Some(cons), Some(node))
      space(); token(":"); space()
      print(Some(alt), Some(node))
    }
    case ne @ NewExpression(callee, args) => {
      word("new"); space()
      print(Some(callee), Some(node))
      def run() = {
        print(ne.typeArguments, Some(ne))
        print(ne.typeParameters, Some(ne))
        token("(")
        printList(args, ne, PrintSequenceOptions())
        token(")")
      }
      parent match {
        case Some(CallExpression(callee, _)) if (callee.equals(ne)) => run()
        case Some(m: MemberExpression) => run()
        case Some(ne: NewExpression) => run()
        case _ if (!format.minified || args.length > 0) => run()
        case _ => ()
      }
    }
    case SequenceExpression(exps) =>
      printList(exps, node, PrintSequenceOptions())
    case Decorator(exp) => {
      token("@")
      if (CodeGenerator.shouldParenthesizeDecoratorExpression(exp)) {
        token("(")
        print(Some(exp), Some(node))
        token(")")
      }
      else print(Some(exp), Some(node))
      newline()
    }
    case OptionalMemberExpression(obj, prop, com, opt) => {
      print(Some(obj), Some(node))
      if (!com.getOrElse(false) && (prop match {
        case m: MemberExpression => true
        case _ => false
      })) {
        throw new Exception("Got a MemberExpression for MemberExpression property")
      }

      val computed = com.getOrElse(false)
      if (opt) token("?.")
      if (computed) {
        token("[")
        print(Some(prop), Some(node))
        token("]")
      }
      else {
        if (!opt) token(".")
        print(Some(prop), Some(node))
      }
    }
    case n @ OptionalCallExpression(callee, args, opt) => {
      print(Some(callee), Some(node))
      print(n.typeParameters, Some(node))
      if (opt) token("?.")
      print(n.typeArguments, Some(node))
      token("(")
      printList(args, node, PrintSequenceOptions())
      token(")")
    }
    case ce @ CallExpression(callee, args) => {
      print(Some(callee), Some(node))
      print(ce.typeArguments, Some(node))
      print(ce.typeParameters, Some(node))
      token("(")
      printList(args, node, PrintSequenceOptions())
      token(")")
    }
    case AwaitExpression(args) => {
      word("await")
      space()
      printTerminatorless(args, node, false)
    }
    case YieldExpression(args, delegate) => {
      word("yield", true)
      if (delegate) {
        token("*")
        args match {
          case Some(_) => {
            space(); print(args, Some(node))
          }
          case _ => ()
        }
      }
      else args match {
        case Some(value) => {
          space(); printTerminatorless(value, node, false)
        }
        case _ => ()
      }
    }
    case ExpressionStatement(exp) => {
      print(Some(exp), Some(node))
      semicolon()
    }
    case AssignmentPattern(left, right) => {
      print(Some(left), Some(node))
      space(); token("="); space()
      print(Some(right), Some(node))
    }
    case AssignmentExpression(op, left, right) => {
      val parens = options.inForStatementInitCounter > 0 &&
        op.equals("in") && Parentheses.needsParens(node, parent, Nil)
      if (parens) token("(")
      print(Some(left), Some(node))
      space()
      if (op.equals("in") || op.equals("instanceof"))
        word(op)
      else token(op)
      space()
      print(Some(right), Some(node))
      if (parens) token(")")
    }
    case BinaryExpression(op, left, right) => {
      print(Some(left), Some(node))
      space()
      op match {
        case BinaryOperator.BitwiseAnd => token("&")
        case BinaryOperator.BitwiseLeftShift => token("<<")
        case BinaryOperator.BitwiseOr => token("|")
        case BinaryOperator.BitwiseRightShift => token(">>")
        case BinaryOperator.BitwiseUnsignedRightShift => token(">>>")
        case BinaryOperator.BitwiseXor => token("^")
        case BinaryOperator.Divide => token("/")
        case BinaryOperator.Equal => token("==")
        case BinaryOperator.Exponentiation => token("**")
        case BinaryOperator.GreaterThan => token(">")
        case BinaryOperator.GreaterThanOrEqual => token(">=")
        case BinaryOperator.In => word("in")
        case BinaryOperator.InstanceOf => word("instanceof")
        case BinaryOperator.LessThan => token("<")
        case BinaryOperator.LessThanOrEqual => token("<=")
        case BinaryOperator.Minus => token("-")
        case BinaryOperator.Modolus => token("%")
        case BinaryOperator.Multiplication => token("*")
        case BinaryOperator.NotEqual => token("!=")
        case BinaryOperator.Pipeline => token("|>")
        case BinaryOperator.Plus => token("+")
        case BinaryOperator.StrictEqual => token("===")
        case BinaryOperator.StrictNotEqual => token("!==")
      }
      space()
      print(Some(right), Some(node))
    }
    case LogicalExpression(op, left, right) => {
      print(Some(left), Some(node))
      space()
      op match {
        case LogicalOperator.And => token("&&")
        case LogicalOperator.NullishCoalescing => token("??")
        case LogicalOperator.Or => token("||")
      }
      space()
      print(Some(right), Some(node))
    }
    case BindExpression(obj, callee) => {
      print(Some(obj), Some(node))
      token("::")
      print(Some(callee), Some(node))
    }
    case MemberExpression(obj, prop, com, opt) => {
      print(Some(obj), Some(node))
      if (!com && (prop match {
        case m: MemberExpression => true
        case _ => false
      })) {
        throw new Exception("Got a MemberExpression for MemberExpression property")
      }

      if (com) {
        token("[")
        print(Some(prop), Some(node))
        token("]")
      }
      else {
        token(".")
        print(Some(prop), Some(node))
      }
    }
    case MetaProperty(meta, prop) => {
      print(Some(meta), Some(node))
      token(".")
      print(Some(prop), Some(node))
    }
    case PrivateName(id) => {
      token("#")
      print(Some(id), Some(node))
    }
    case V8IntrinsicIdentifier(name) => { token("%"); word(name) }
    case ModuleExpression(body) => {
      word("module", true); space(); token("{"); indent()
      if (body.body.length > 0 || body.directives.length > 0)
        newline()
      print(Some(body), Some(node))
      dedent()
      sourceWithOffset(LocationType.End, node.location, 0, -1)
      rightBrace()
    }
    case JSXMemberExpression(obj, prop) => {
      print(Some(obj), Some(node))
      token(".")
      print(Some(prop), Some(node))
    }
    case dec @ ExportAllDeclaration(source) => {
      word("export"); space()
      dec.exportKind match {
        case Some(value) if (value == ExportKind.Type) => {
          word("type"); space()
        }
        case _ => ()
      }

      token("*"); space(); word("from"); space()
      print(Some(source), Some(node))
      semicolon()
    }
    case dec @ ExportNamedDeclaration(declaration, specifiers, source) => {
      word("export"); space()
      declaration match {
        case Some(value) => {
          print(declaration, Some(node))
          value match {
            case s: Statement => semicolon()
            case _ => ()
          }
        }
        case _ => dec.exportKind match {
          case Some(value) if (value == ExportKind.Type) => {
            word("type"); space()
          }
          case _ => ()
        }
      }

      val res = specifiers.iterator.zipWithIndex.foldLeft((false, -1))(
        (res, p) => if (!res._1 && res._2 > -1) res else p._1 match {
          case s: ExportDefaultSpecifier => {
            print(Some(s), Some(node))
            if (p._2 < specifiers.length) { token(","); space() }
            (true, p._2)
          }
          case s: ExportNamespaceSpecifier => {
            print(Some(s), Some(node))
            if (p._2 < specifiers.length) { token(","); space() }
            (true, p._2)
          }
          case _ => (res._1, p._2)
        })

      if (res._2 < specifiers.length || res._1) {
        token("{")
        if (res._2 < specifiers.length) {
          space()
          printList(specifiers.drop(res._2), node, PrintSequenceOptions())
          space()
        }
        token("}")
      }

      source match {
        case Some(value) => {
          space(); word("from"); space()
          dec.assertions match {
            case Some(assertions) if (assertions.length > 0) => {
              print(source, Some(node), true)
              space()
              word("assert"); space(); token("{"); space()
              printList(assertions, node, PrintSequenceOptions())
              space(); token("}")
            }
            case _ => print(source, Some(node))
          }
        }
        case _ => ()
      }

      semicolon()
    }
    case ExportDefaultDeclaration(declaration) => {
      word("export")
      noIndentInnerCommentsHere()
      space(); word("default"); space()
      print(Some(declaration), Some(node))
      declaration match {
        case s: Statement => semicolon()
        case _ => ()
      }
    }
    case dec @ ImportDeclaration(specifiers, source) => {
      word("import"); space()
      dec.importKind match {
        case Some(ImportKind.Type) => {
          noIndentInnerCommentsHere()
          word("type"); space()
        }
        case Some(ImportKind.TypeOf) => {
          noIndentInnerCommentsHere()
          word("typeof"); space()
        }
        case _ if (dec.module.isDefined) => {
          noIndentInnerCommentsHere()
          word("module"); space()
        }
        case _ => ()
      }

      val res = specifiers.iterator.zipWithIndex.foldLeft((!specifiers.isEmpty, -1))(
        (res, p) => if (!res._1) res else p._1 match {
          case s: ImportDefaultSpecifier => {
            print(Some(s), Some(node))
            if (p._2 < specifiers.length) { token(","); space() }
            (true, p._2)
          }
          case s: ImportNamespaceSpecifier => {
            print(Some(s), Some(node))
            if (p._2 < specifiers.length) { token(","); space() }
            (true, p._2)
          }
          case _ => (false, p._2)
        })

      if (res._2 < specifiers.length) {
        token("{")
        space()
        printList(specifiers.drop(res._2), node, PrintSequenceOptions())
        space()
        token("}")
      }
      else if (!specifiers.isEmpty) dec.importKind match {
        case Some(ImportKind.Type) => {
          space(); word("from"); space()
        }
        case Some(ImportKind.TypeOf) => {
          space(); word("from"); space()
        }
        case _ => ()
      }

      dec.assertions match {
        case Some(value) => {
          print(Some(source), Some(node), true)
          space()
          word("assert"); space(); token("{"); space()
          printList(value, node, PrintSequenceOptions())
          space(); token("}")
        }
        case None => print(Some(source), Some(node))
      }

      semicolon()
    }
    case ImportAttribute(key, value) => {
      print(Some(key))
      token(":"); space()
      print(Some(value))
    }
    case ImportNamespaceSpecifier(local) => {
      token("*"); space()
      word("as"); space()
      print(Some(local), Some(node))
    }
    case exp @ TaggedTemplateExpression(tag, quasi) => {
      print(Some(tag), Some(node))
      print(exp.typeParameters, Some(node))
      print(Some(quasi), Some(node))
    }
    case TemplateElement(value, tail) => {
      val pos = parent match {
        case Some(TemplateLiteral(quasis, _)) =>
          (quasis.head.equals(node), quasis.last.equals(node))
        case _ => throw new AssertionError("wrong parent type of TemplateElement") // This should not happen
      }

      // TODO: value.raw?
      val tok = (if (pos._1) "`" else "}") + value.toString() + (if (pos._2) "`" else "${")
      token(tok)
    }
    case TemplateLiteral(quasis, expressions) => {
      quasis.iterator.zipWithIndex.foreach((q, i) => {
        print(Some(q), Some(node))
        if (i + 1 < quasis.length) print(Some(expressions(i)), Some(node))
      })
    }
    case TSTypeAnnotation(anno) => {
      token(":"); space()
      print(Some(anno), Some(node))
    }
    case TSTypeParameterInstantiation(params) => {
      token("<")
      printList(params, node, PrintSequenceOptions())
      parent match {
        case Some(ArrowFunctionExpression(params, _, _, _)) if (params.length == 1) =>
          token(",")
        case _ => () 
      }
      token(">")
    }
    case tp @ TSTypeParameter(constraint, default, name) => {
      if (tp.in.getOrElse(false)) {
        word("in"); space()
      }
      if (tp.out.getOrElse(false)) {
        word("out"); space()
      }
      
      word(name)
      
      if (constraint.isDefined) {
        space(); word("extends"); space()
        print(constraint, Some(node))
      }
      if (default.isDefined) {
        space(); word("="); space()
        print(default, Some(node))
      }
    }
    case prop @ TSParameterProperty(params) => {
      prop.accessibility match {
        case Some(AccessModifier.Private) => {
          word("private"); space()
        }
        case Some(AccessModifier.Public) => {
          word("public"); space()
        }
        case Some(AccessModifier.Protected) => {
          word("protected"); space()
        }
        case _ => ()
      }

      if (prop.readonly.getOrElse(false)) {
        word("readonly"); space()
      }

      param(params)
    }
    case fun @ TSDeclareFunction(id, tp, params, ret) => {
      if (fun.declare.getOrElse(false)) {
        word("declare"); space()
      }
      functionHead(fun)
      token(";")
    }
    case method @ TSDeclareMethod(dec, key, tp, params, ret) => {
      // TODO: _classMethodHead(node)
      token(";")
    }
    case TSQualifiedName(left, right) => {
      print(Some(left), Some(node))
      token(".")
      print(Some(right), Some(node))
    }
    case TSCallSignatureDeclaration(tp, params, anno) => {
      print(tp, Some(node))
      token("(")
      parameters(params, node)
      token(")")
      // TODO: No return type found
      print(anno, Some(node))
      token(";")
    }
    case TSConstructSignatureDeclaration(tp, params, anno) => {
      word("new"); space()
      print(tp, Some(node))
      token("(")
      parameters(params, node)
      token(")")
      // TODO: No return type found
      print(anno, Some(node))
      token(";")
    }
    case sig @ TSPropertySignature(key, anno, init, kind) => {
      if (sig.readonly.getOrElse(false)) {
        word("readonly"); space()
      }
      if (sig.computed.getOrElse(false)) token("[")
      print(Some(key), Some(node))
      if (sig.computed.getOrElse(false)) token("]")
      if (sig.optional.getOrElse(false)) token("?")
      print(anno, Some(node))
      if (init.isDefined) {
        space(); token("="); space()
        print(init, Some(node))
      }
      token(";")
    }
    case sig @ TSMethodSignature(key, tp, params, anno, kind) => {
      kind match {
        case TSMethodSignatureKind.Getter => { word("get"); space() }
        case TSMethodSignatureKind.Setter => { word("set"); space() }
        case _ => ()
      }

      if (sig.computed.getOrElse(false)) token("[")
      print(Some(key), Some(node))
      if (sig.computed.getOrElse(false)) token("]")
      if (sig.optional.getOrElse(false)) token("?")

      print(tp, Some(node))
      token("(")
      parameters(params, node)
      token(")")
      // TODO: No return type found
      print(anno, Some(node))

      token(";")
    }
    case sig @ TSIndexSignature(params, anno) => {
      if (sig.static.getOrElse(false)) {
        word("static"); space()
      }
      if (sig.readonly.getOrElse(false)) {
        word("readonly"); space()
      }
      token("[")
      // TODO: _parameters(node.parameters, node)
      token("]")
      print(anno, Some(node))
      token(";")
    }
    case TSFunctionType(tp, params, anno) => {
      print(tp, Some(node))
      token("(")
      parameters(params, node)
      token(")"); space(); token("=>"); space()
      // TODO: No return type found
      print(anno, Some(node))
    }
    case ct @ TSConstructorType(tp, params, anno) => {
      if (ct.`abstract`.getOrElse(false)) {
        word("abstract"); space()
      }

      word("new"); space()

      print(tp, Some(node))
      token("(")
      parameters(params, node)
      token(")"); space(); token("=>"); space()
      // TODO: No return type found
      print(anno, Some(node))
    }
    case TSTypeReference(name, tp) => {
      print(Some(name), Some(node), true)
      print(tp, Some(node), true)
    }
    case TSTypePredicate(name, anno, asserts) => {
      if (asserts.getOrElse(false)) {
        word("asserts"); space()
      }

      print(Some(name))
      anno match {
        case Some(value) => {
          space(); word("is"); space()
          print(Some(value.typeAnnotation))
        }
        case _ => ()
      }
    }
    case TSTypeQuery(name, tp) => {
      word("typeof"); space()
      print(Some(name))
      if (tp.isDefined) print(tp, Some(node))
    }
    case TSTypeLiteral(members) => {
      tsPrintBraced(members, node)
    }
    case TSArrayType(element) => {
      print(Some(element), Some(node), true)
      token("[]")
    }
    case TSTupleType(element) => {
      token("[")
      printList(element, node, PrintSequenceOptions())
      token("]")
    }
    case TSOptionalType(anno) => {
      print(Some(anno), Some(node))
      token("?")
    }
    case TSRestType(anno) => {
      token("...")
      print(Some(anno), Some(node))
    }
    case TSNamedTupleMember(label, element, optional) => {
      print(Some(label), Some(node))
      if (optional) token("?")
      token(":")
      space()
      print(Some(element), Some(node))
    }
    case TSUnionType(types) =>
      tsPrintUnionOrIntersectionType(types, node, "|")
    case TSIntersectionType(types) =>
      tsPrintUnionOrIntersectionType(types, node, "&")
    case TSConditionalType(checkType, extendsType, trueType, falseType) => {
      print(Some(checkType))
      space(); word("extends"); space()
      print(Some(extendsType))
      space(); token("?"); space()
      print(Some(trueType))
      space(); token(":"); space()
      print(Some(falseType))
    }
    case TSInferType(tp) => {
      token("infer"); space()
      print(Some(tp))
    }
    case TSParenthesizedType(anno) => {
      token("(")
      print(Some(anno), Some(node))
      token(")")
    }
    case TSTypeOperator(anno, op) => {
      word(op); space()
      print(Some(anno), Some(node))
    }
    case TSIndexedAccessType(obj, index) => {
      print(Some(obj), Some(node), true)
      token("[")
      print(Some(index), Some(node))
      token("]")
    }
    case map @ TSMappedType(tp, anno, name) => {
      token("{"); space()
      map.readonly match {
        case Some("+") => token("+"); word("readonly"); space()
        case Some("-") => token("-"); word("readonly"); space()
        case Some(true) => word("readonly"); space()
        case _ => ()
      }

      token("[")
      name match {
        case Some(Identifier(name)) => word(name)
        case _ => ()
      }
      space(); word("in"); space()
      print(tp.constraint, Some(tp))
      if (name.isDefined) {
        space(); word("as"); space()
        print(name, Some(node))
      }
      token("]")
      map.readonly match {
        case Some("+") => token("+"); token("?")
        case Some("-") => token("-"); token("?")
        case Some(true) => token("?")
        case _ => ()
      }
      token(":"); space()
      print(anno, Some(node))
      space(); token("}")
    }
    case TSLiteralType(lit) => print(Some(lit), Some(node))
    case TSExpressionWithTypeArguments(exp, tp) => {
      print(Some(exp), Some(node))
      print(tp, Some(node))
    }
    case dec @ TSInterfaceDeclaration(id, tp, ext, body) => {
      if (dec.declare.getOrElse(false)) {
        word("declare"); space()
      }
      word("interface"); space()
      print(Some(id), Some(node))
      print(tp, Some(node))
      ext match {
        case Some(value) if (value.length > 0) => {
          space(); word("extends"); space()
          printList(value, node, PrintSequenceOptions())
        }
        case _ => ()
      }

      space()
      print(Some(body), Some(node))
    }
    case TSInterfaceBody(body) => tsPrintBraced(body, node)
    case dec @ TSTypeAliasDeclaration(id, tp, anno) => {
      if (dec.declare.getOrElse(false)) {
        word("declare"); space()
      }

      word("type"); space()
      print(Some(id), Some(node))
      print(tp, Some(node))
      space(); token("="); space()
      print(Some(anno), Some(node))
      token(";")
    }
    case TSAsExpression(exp, anno) => {
      val forceParens = exp.trailingComments match {
        case Some(value) => value.length > 0
        case _ => false
      }
      print(Some(exp), Some(node), true, 0, forceParens) // TODO: undefined?
      space(); word("as"); space()
      print(Some(anno), Some(node))
    }
    case TSSatisfiesExpression(exp, anno) => {
      val forceParens = exp.trailingComments match {
        case Some(value) => value.length > 0
        case _ => false
      }
      print(Some(exp), Some(node), true, 0, forceParens) // TODO: undefined?
      space(); word("satisfies"); space()
      print(Some(anno), Some(node))
    }
    case TSTypeAssertion(anno, exp) => {
      token("<")
      print(Some(anno), Some(node))
      token(">"); space()
      print(Some(exp), Some(node))
    }
    case TSInstantiationExpression(exp, tp) => {
      print(Some(exp), Some(node))
      print(tp, Some(node))
    }
    case dec @ TSEnumDeclaration(id, members) => {
      if (dec.declare.getOrElse(false)) {
        word("declare"); space()
      }

      if (dec.const.getOrElse(false)) {
        word("const"); space()
      }

      word("enum"); space()
      print(Some(id), Some(node))
      space()
      tsPrintBraced(members, node)
    }
    case TSEnumMember(id, init) => {
      print(Some(id), Some(node))
      if (init.isDefined) {
        space(); token("="); space()
        print(init, Some(node))
      }
      token(",")
    }
    case dec @ TSModuleDeclaration(id, body) => {
      if (dec.declare.getOrElse(false)) {
        word("declare"); space()
      }

      if (dec.global.getOrElse(false)) {
        id match {
          case i: Identifier => word("namespace")
          case _ => word("module")
        }
      }
      print(Some(id), Some(node))
      def run (md: TSModuleDeclaration): Unit = {
        token(".")
        print(Some(md.id), Some(md))
        md.body match {
          case m: TSModuleDeclaration => run(m)
          case _ => ()
        }
      }

      body match {
        case m: TSModuleDeclaration => run(m)
        case _ => ()
      }

      space()
      print(Some(body), Some(node))
    }
    case TSModuleBlock(body) => tsPrintBraced(body, node)
    case TSImportType(arg, qualifier, tp) => {
      word("import"); token("(")
      print(Some(arg), Some(node))
      token(")")
      if (qualifier.isDefined) {
        token(".")
        print(qualifier, Some(node))
      }
      if (tp.isDefined) {
        print(tp, Some(node))
      }
    }
    case dec @ TSImportEqualsDeclaration(id, ref, exp) => {
      if (exp) { word("export"); space() }
      word("import"); space()
      print(Some(id), Some(node))
      space()
      token("="); space()
      print(Some(ref), Some(node))
      token(";")
    }
    case TSExternalModuleReference(exp) => {
      token("require(")
      print(Some(exp), Some(node))
      token(")")
    }
    case TSNonNullExpression(exp) => {
      print(Some(exp), Some(node))
      token("!")
    }
    case TSExportAssignment(exp) => {
      word("export"); space(); token("="); space()
      print(Some(exp), Some(node))
      token(";")
    }
    case TSNamespaceExportDeclaration(id) => {
      word("export"); space(); token("as"); space(); word("namespace"); space()
      print(Some(id), Some(node))
    }
    case _ => () // TODO

  def generate() = super.generate(ast)

  private def printStatementAfterKeyword(node: Option[Node], parent: Node, isLabel: Boolean)(implicit options: PrinterOptions) =
    node match {
      case Some(node) =>
        space()
        printTerminatorless(node, parent, isLabel)
        semicolon()
      case _ => semicolon()
    }

  private def tsPrintBraced(members: List[Node], node: Node)(implicit options: PrinterOptions) = {
    token("{")
    if (members.length > 0) {
      indent()
      newline()
      members.foreach((m) => {
        print(Some(m), Some(node))
        newline()
      })
      dedent()
    }

    sourceWithOffset(LocationType.End, node.location, 0, -1)
    rightBrace()
  }

  private def tsPrintUnionOrIntersectionType(types: List[Node], node: Node, sep: String)(implicit options: PrinterOptions) =
    printJoin(Some(types), node, PrintSequenceOptions(
      separator = Some((p: Printer) => { p.space(); p.token(sep); p.space(); })
    ))

  private def tsPrintClassMemberModifiers(
    node: ClassProperty | ClassAccessorProperty | ClassMethod | ClassPrivateMethod | TSDeclareMethod
  )(implicit options: PrinterOptions) = {
    val isField = node match {
      case _: ClassAccessorProperty => true
      case _: ClassProperty => true
      case _ => false
    }

    def execute(declare: Boolean, access: Option[AccessModifier], static: Boolean, overrided: Boolean, abs: Boolean, readonly: Boolean) = {
      if (isField && declare) { word("declare"); space() }
      access match {
        case Some(AccessModifier.Public) => { word("public"); space() }
        case Some(AccessModifier.Private) => { word("private"); space() }
        case Some(AccessModifier.Protected) => { word("protected"); space() }

        case _ => ()
      }
      if (static) { word("static"); space() }
      if (overrided) { word("override"); space() }
      if (abs) { word("abstract"); space() }
      if (isField && readonly) { word("readonly"); space() }
    }

    node match {
      case node @ ClassProperty(_, _, _, _, _, static) =>
        execute(node.declare.getOrElse(false),
          node.accessibility, static,
          node.`override`.getOrElse(false),
          node.`abstract`.getOrElse(false),
          node.readonly.getOrElse(false))
      case node @ ClassAccessorProperty(_, _, _, _, _, static) =>
        execute(node.declare.getOrElse(false),
          node.accessibility, static,
          node.`override`.getOrElse(false),
          node.`abstract`.getOrElse(false),
          node.readonly.getOrElse(false))
      case node @ ClassMethod(_, _, _, _, _, static, _, _) =>
        execute(false,
          node.accessibility, static,
          node.`override`.getOrElse(false),
          node.`abstract`.getOrElse(false),
          false)
      case node @ ClassPrivateMethod(_, _, _, _, static) =>
        execute(false,
          node.accessibility, static,
          node.`override`.getOrElse(false),
          node.`abstract`.getOrElse(false),
          false)
      case node @ TSDeclareMethod(_, _, _, _, _) =>
        execute(true,
          node.accessibility, node.static.getOrElse(false),
          node.`override`.getOrElse(false),
          node.`abstract`.getOrElse(false),
          false)
    }
  }

  private def variance(
    node: TypeParameter | ObjectTypeIndexer | ObjectTypeProperty | ClassProperty | ClassPrivateProperty | ClassAccessorProperty
  )(implicit options: PrinterOptions) = {
    val vari = node match {
      case TypeParameter(_, _, v, _) => v
      case ObjectTypeIndexer(_, _, _, v, _) => v
      case ObjectTypeProperty(_, _, v, _, _, _, _, _) => v
      case p: ClassProperty => p.variance
      case p: ClassPrivateProperty => p.variance
      case p: ClassAccessorProperty => p.variance
    }

    vari match {
      case Some(Variance(VarianceKind.Covariant)) => token("+")
      case Some(Variance(VarianceKind.Contravariant)) => token("-")
      case _ => ()
    }
  }

  private def param(parameter: Identifier | RestElement | Node with Pattern | TSParameterProperty, parent: Option[Node] = None)(implicit options: PrinterOptions) = {
    val dec = parameter match {
      case id: Identifier => id.decorators
      case e: RestElement => e.decorators
      // TODO: move decorators to Pattern trait
      case p: TSParameterProperty => p.decorators
      case _ => None
    }

    printJoin(dec, parameter, PrintSequenceOptions())
    print(Some(parameter), parent)
  }

  private def parameters(parameter: List[Identifier | RestElement | Node with Pattern | TSParameterProperty], parent: Node)
    (implicit options: PrinterOptions) =
    parameter.iterator.zipWithIndex.foreach((p, i) => {
      param(p, Some(parent))
      if (i < parameter.length - 1) token(","); space()
    })

  private def predicate(
    node: FunctionDeclaration | FunctionExpression | ArrowFunctionExpression,
    noLineTerminatorAfter: Boolean = false
  )(implicit options: PrinterOptions) = {
    val pred = node match {
      case d: FunctionDeclaration => d.predicate
      case e: FunctionExpression => e.predicate
      case e: ArrowFunctionExpression => e.predicate
    }
    val rt = node match {
      case d: FunctionDeclaration => d.returnType
      case e: FunctionExpression => e.returnType
      case e: ArrowFunctionExpression => e.returnType
    }

    pred match {
      case Some(_) =>
        if (rt.isEmpty) token(":")
        space(); print(pred, Some(node), noLineTerminatorAfter)
      case _ => ()
    }
  }

  private def functionHead(
    node: FunctionDeclaration | FunctionExpression | TSDeclareFunction
  )(implicit options: PrinterOptions) = {
    val async = node match {
      case FunctionDeclaration(_, _, _, _, async) => async
      case FunctionExpression(_, _, _, _, async) => async
      case d: TSDeclareFunction => d.async.getOrElse(false)
    }

    if (async) {
      word("async")
      _endWithInnerRaw = false
      space()
    }

    word("function")

    val generator = node match {
      case FunctionDeclaration(_, _, _, generator, _) => generator
      case FunctionExpression(_, _, _, generator, _) => generator
      case d: TSDeclareFunction => d.generator.getOrElse(false)
    }

    if (generator) {
      _endWithInnerRaw = false
      token("*")
    }

    space()
    val id = node match {
      case FunctionDeclaration(id, _, _, _, _) => id
      case FunctionExpression(id, _, _, _, _) => id
      case TSDeclareFunction(id, _, _, _) => id
    }
    
    id match {
      case Some(id) => print(id, Some(node))
      case _ => ()
    }

    val tp = node match {
      case d: FunctionDeclaration => d.typeParameters
      case e: FunctionExpression => e.typeParameters
      case d: TSDeclareFunction => d.typeParameters
    }
    print(tp, Some(node))

    val params = node match {
      case FunctionDeclaration(_, p, _, _, _) => p
      case FunctionExpression(_, p, _, _, _) => p
      case TSDeclareFunction(_, _, p, _) => p
    }
    token("(")
    parameters(params, node)
    token(")")

    val rp = node match {
      case d: FunctionDeclaration => d.returnType
      case e: FunctionExpression => e.returnType
      case d: TSDeclareFunction => d.returnType
    }
    print(rp, Some(node), false)
    _noLineTerminator = false

    node match {
      case d: FunctionDeclaration => predicate(d)
      case e: FunctionExpression => predicate(e)
      case _ => ()
    }
  }
end CodeGenerator

object CodeGenerator:
  def apply(ast: Node, format: Format, sourceMap: SourceMapBuilder) =
    new CodeGenerator(ast, format, sourceMap)
  def isDecoratorMemberExpression(node: Node): Boolean =
    node match {
      case i: Identifier => true
      case MemberExpression(obj, prop, computed, _) =>
        !computed && (prop match {
          case i: Identifier => true
          case _ => false
        }) && isDecoratorMemberExpression(obj)
      case _ => false
    }
  def shouldParenthesizeDecoratorExpression(node: Node): Boolean =
    node match {
      case p: ParenthesizedExpression => false
      case e: CallExpression => !isDecoratorMemberExpression(e.callee)
      case _ => !isDecoratorMemberExpression(node)
    }
  def isFor(node: Node): Boolean =
    node match {
      case _: ForInStatement => true
      case _: ForOfStatement => true
      case _: ForStatement => true
      case _ => false
    }
