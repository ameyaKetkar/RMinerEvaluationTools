/*
 * Copyright 2014 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.rhino.IR;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfoBuilder;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts ES6 code to valid ES5 code. This class does most of the transpilation, and
 * https://github.com/google/closure-compiler/wiki/ECMAScript6 lists which ES6 features are
 * supported. Other classes that start with "Es6" do other parts of the transpilation.
 *
 * <p>In most cases, the output is valid as ES3 (hence the class name) but in some cases, if
 * the output language is set to ES5, we rely on ES5 features such as getters, setters,
 * and Object.defineProperties.
 *
 * @author tbreisacher@google.com (Tyler Breisacher)
 */
public final class Es6ToEs3Converter implements NodeTraversal.Callback, HotSwapCompilerPass {
  private final AbstractCompiler compiler;

  static final DiagnosticType CANNOT_CONVERT = DiagnosticType.error(
      "JSC_CANNOT_CONVERT",
      "This code cannot be converted from ES6. {0}");

  // TODO(tbreisacher): Remove this once we have implemented transpilation for all the features
  // we intend to support.
  static final DiagnosticType CANNOT_CONVERT_YET = DiagnosticType.error(
      "JSC_CANNOT_CONVERT_YET",
      "ES6 transpilation of ''{0}'' is not yet implemented.");

  static final DiagnosticType DYNAMIC_EXTENDS_TYPE = DiagnosticType.error(
      "JSC_DYNAMIC_EXTENDS_TYPE",
      "The class in an extends clause must be a qualified name.");

  static final DiagnosticType CLASS_REASSIGNMENT = DiagnosticType.error(
      "CLASS_REASSIGNMENT",
      "Class names defined inside a function cannot be reassigned.");

  static final DiagnosticType CONFLICTING_GETTER_SETTER_TYPE = DiagnosticType.error(
      "CONFLICTING_GETTER_SETTER_TYPE",
      "The types of the getter and setter for property ''{0}'' do not match.");

  static final DiagnosticType BAD_REST_PARAMETER_ANNOTATION = DiagnosticType.warning(
      "BAD_REST_PARAMETER_ANNOTATION",
      "Missing \"...\" in type annotation for rest parameter.");

  // The name of the index variable for populating the rest parameter array.
  private static final String REST_INDEX = "$jscomp$restIndex";

  // The name of the placeholder for the rest parameters.
  private static final String REST_PARAMS = "$jscomp$restParams";

  // The name of the vars that capture 'this' and 'arguments'
  // for converting arrow functions.
  private static final String THIS_VAR = "$jscomp$this";
  private static final String ARGUMENTS_VAR = "$jscomp$arguments";

  private static final String FRESH_SPREAD_VAR = "$jscomp$spread$args";

  private static final String DESTRUCTURING_TEMP_VAR = "$jscomp$destructuring$var";

  private int destructuringVarCounter = 0;

  private static final String FRESH_COMP_PROP_VAR = "$jscomp$compprop";

  private static final String ITER_BASE = "$jscomp$iter$";

  private static final String ITER_RESULT = "$jscomp$key$";

  // These functions are defined in js/es6_runtime.js
  static final String INHERITS = "$jscomp.inherits";
  static final String MAKE_ITER = "$jscomp.makeIterator";

  public Es6ToEs3Converter(AbstractCompiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverse(compiler, root, this);
  }

  @Override
  public void hotSwapScript(Node scriptRoot, Node originalRoot) {
    NodeTraversal.traverse(compiler, scriptRoot, this);
  }

  /**
   * Some nodes (such as arrow functions) must be visited pre-order in order to rewrite the
   * references to {@code this} correctly.
   * Everything else is translated post-order in {@link #visit}.
   */
  @Override
  public boolean shouldTraverse(NodeTraversal t, Node n, Node parent) {
    switch (n.getType()) {
      case Token.FUNCTION:
        if (n.isArrowFunction()) {
          visitArrowFunction(t, n);
        }
        break;
      case Token.PARAM_LIST:
        visitParamList(n, parent);
        break;
      case Token.GETTER_DEF:
      case Token.SETTER_DEF:
        if (compiler.getOptions().getLanguageOut() == LanguageMode.ECMASCRIPT3) {
          cannotConvert(n, "ES5 getters/setters (consider using --language_out=ES5)");
          return false;
        }
        break;
    }
    return true;
  }

  @Override
  public void visit(NodeTraversal t, Node n, Node parent) {
    switch (n.getType()) {
      case Token.OBJECTLIT:
        for (Node child : n.children()) {
          if (child.isComputedProp()) {
            visitObjectWithComputedProperty(n, parent);
            break;
          }
        }
        break;
      case Token.MEMBER_FUNCTION_DEF:
        if (parent.isObjectLit()) {
          visitMemberDefInObjectLit(n, parent);
        }
        break;
      case Token.FOR_OF:
        visitForOf(n, parent);
        break;
      case Token.STRING_KEY:
        visitStringKey(n);
        break;
      case Token.CLASS:
        for (Node member = n.getLastChild().getFirstChild();
            member != null;
            member = member.getNext()) {
          if (member.getBooleanProp(Node.COMPUTED_PROP_GETTER)
              || member.getBooleanProp(Node.COMPUTED_PROP_SETTER)) {
            cannotConvert(member, "computed getter or setter in class definition");
            return;
          }
        }
        visitClass(n, parent);
        break;
      case Token.ARRAYLIT:
      case Token.NEW:
      case Token.CALL:
        for (Node child : n.children()) {
          if (child.isSpread()) {
            visitArrayLitOrCallWithSpread(n, parent);
            break;
          }
        }
        break;
      case Token.TAGGED_TEMPLATELIT:
        Es6TemplateLiterals.visitTaggedTemplateLiteral(t, n);
        break;
      case Token.TEMPLATELIT:
        if (!parent.isTaggedTemplateLit()) {
          Es6TemplateLiterals.visitTemplateLiteral(t, n);
        }
        break;
      case Token.ARRAY_PATTERN:
        visitArrayPattern(t, n, parent);
        break;
      case Token.OBJECT_PATTERN:
        visitObjectPattern(t, n, parent);
        break;
    }
  }

  private void visitObjectPattern(NodeTraversal t, Node objectPattern, Node parent) {
    Node rhs, nodeToDetach;
    if (NodeUtil.isNameDeclaration(parent) && !NodeUtil.isEnhancedFor(parent.getParent())) {
      rhs = objectPattern.getLastChild();
      nodeToDetach = parent;
    } else if (parent.isAssign() && parent.getParent().isExprResult()) {
      rhs = parent.getLastChild();
      nodeToDetach = parent.getParent();
    } else if (parent.isStringKey() || parent.isArrayPattern()
        || parent.isDefaultValue()) {
      // Nested object pattern; do nothing. We will visit it after rewriting the parent.
      return;
    } else if (NodeUtil.isEnhancedFor(parent) || NodeUtil.isEnhancedFor(parent.getParent())) {
      visitDestructuringPatternInEnhancedFor(objectPattern);
      return;
    } else if (parent.isCatch()) {
      visitDestructuringPatternInCatch(objectPattern);
      return;
    } else {
      throw new IllegalStateException("Unexpected OBJECT_PATTERN parent: " + parent);
    }

    // Convert 'var {a: b, c: d} = rhs' to:
    // var temp = rhs;
    // var b = temp.a;
    // var d = temp.c;
    String tempVarName = DESTRUCTURING_TEMP_VAR + (destructuringVarCounter++);
    Node tempDecl = IR.var(IR.name(tempVarName), rhs.detachFromParent())
        .useSourceInfoIfMissingFromForTree(objectPattern);
    nodeToDetach.getParent().addChildBefore(tempDecl, nodeToDetach);

    for (Node child = objectPattern.getFirstChild(), next;
        child != null;
        child = next) {
      next = child.getNext();

      Node newLHS, newRHS;
      if (child.isStringKey()) {
        Preconditions.checkState(child.hasChildren());
        Node getprop = new Node(child.isQuotedString() ? Token.GETELEM : Token.GETPROP,
                                IR.name(tempVarName),
                                IR.string(child.getString()));

        Node value = child.removeFirstChild();
        if (!value.isDefaultValue()) {
          newLHS = value;
          newRHS = getprop;
        } else {
          newLHS = value.removeFirstChild();
          Node defaultValue = value.removeFirstChild();
          newRHS = defaultValueHook(getprop, defaultValue);
        }
      } else if (child.isComputedProp()) {
        if (child.getLastChild().isDefaultValue()) {
          newLHS = child.getLastChild().removeFirstChild();
          Node getelem = IR.getelem(
              IR.name(tempVarName),
              child.removeFirstChild());

          String intermediateTempVarName = DESTRUCTURING_TEMP_VAR + (destructuringVarCounter++);
          Node intermediateDecl = IR.var(IR.name(intermediateTempVarName), getelem);
          intermediateDecl.useSourceInfoIfMissingFromForTree(child);
          nodeToDetach.getParent().addChildBefore(intermediateDecl, nodeToDetach);

          newRHS = defaultValueHook(
              IR.name(intermediateTempVarName),
              child.getLastChild().removeFirstChild());
        } else {
          newRHS = IR.getelem(IR.name(tempVarName), child.removeFirstChild());
          newLHS = child.removeFirstChild();
        }
      } else if (child.isDefaultValue()) {
        newLHS = child.removeFirstChild();
        Node defaultValue = child.removeFirstChild();
        Node getprop = IR.getprop(IR.name(tempVarName), IR.string(newLHS.getString()));
        newRHS = defaultValueHook(getprop, defaultValue);
      } else {
        throw new IllegalStateException("Unexpected OBJECT_PATTERN child: " + child);
      }

      Node newNode;
      if (NodeUtil.isNameDeclaration(parent)) {
        newNode = IR.declaration(newLHS, newRHS, parent.getType());
      } else if (parent.isAssign()) {
        newNode = IR.exprResult(IR.assign(newLHS, newRHS));
      } else {
        throw new IllegalStateException("not reached");
      }
      newNode.useSourceInfoIfMissingFromForTree(child);

      nodeToDetach.getParent().addChildBefore(newNode, nodeToDetach);

      // Explicitly visit the LHS of the new node since it may be a nested
      // destructuring pattern.
      visit(t, newLHS, newLHS.getParent());
    }

    nodeToDetach.detachFromParent();
    compiler.reportCodeChange();
  }

  private void visitArrayPattern(NodeTraversal t, Node arrayPattern, Node parent) {
    Node rhs, nodeToDetach;
    if (NodeUtil.isNameDeclaration(parent) && !NodeUtil.isEnhancedFor(parent.getParent())) {
      // The array pattern is the only child, because Es6SplitVariableDeclarations
      // has already run.
      Preconditions.checkState(arrayPattern.getNext() == null);
      rhs = arrayPattern.getLastChild();
      nodeToDetach = parent;
    } else if (parent.isAssign()) {
      rhs = arrayPattern.getNext();
      nodeToDetach = parent.getParent();
      Preconditions.checkState(nodeToDetach.isExprResult());
    } else if (parent.isArrayPattern() || parent.isDefaultValue()
        || parent.isStringKey()) {
      // This is a nested array pattern. Don't do anything now; we'll visit it
      // after visiting the parent.
      return;
    } else if (NodeUtil.isEnhancedFor(parent) || NodeUtil.isEnhancedFor(parent.getParent())) {
      visitDestructuringPatternInEnhancedFor(arrayPattern);
      return;
    } else if (parent.isCatch()) {
      visitDestructuringPatternInCatch(arrayPattern);
      return;
    } else {
      throw new IllegalStateException("Unexpected ARRAY_PATTERN parent: " + parent);
    }

    // Convert 'var [x, y] = rhs' to:
    // var temp = rhs;
    // var x = temp[0];
    // var y = temp[1];
    String tempVarName = DESTRUCTURING_TEMP_VAR + (destructuringVarCounter++);
    Node tempDecl = IR.var(IR.name(tempVarName), rhs.detachFromParent())
        .useSourceInfoIfMissingFromForTree(arrayPattern);
    nodeToDetach.getParent().addChildBefore(tempDecl, nodeToDetach);

    int i = 0;
    for (Node child = arrayPattern.getFirstChild(), next;
        child != null;
        child = next, i++) {
      next = child.getNext();
      if (child.isEmpty()) {
        continue;
      }

      Node newLHS, newRHS;
      if (child.isDefaultValue()) {
        Node getElem = IR.getelem(IR.name(tempVarName), IR.number(i));
        //   [x = defaultValue] = rhs;
        // becomes
        //   var temp = rhs;
        //   x = (temp[0] === undefined) ? defaultValue : temp[0];
        newLHS = child.getFirstChild().detachFromParent();
        newRHS = defaultValueHook(getElem, child.getLastChild().detachFromParent());
      } else if (child.isRest()) {
        newLHS = child.detachFromParent();
        newLHS.setType(Token.NAME);
        // [].slice.call(temp, i)
        newRHS = IR.call(
            IR.getprop(IR.getprop(IR.arraylit(), IR.string("slice")), IR.string("call")),
            IR.name(tempVarName), IR.number(i));
      } else {
        newLHS = child.detachFromParent();
        newRHS = IR.getelem(IR.name(tempVarName), IR.number(i));
      }
      Node newNode;
      if (parent.isAssign()) {
        Node assignment = IR.assign(newLHS, newRHS);
        newNode = IR.exprResult(assignment);
      } else {
        newNode = IR.declaration(newLHS, newRHS, parent.getType());
      }
      newNode.useSourceInfoIfMissingFromForTree(arrayPattern);

      nodeToDetach.getParent().addChildBefore(newNode, nodeToDetach);
      // Explicitly visit the LHS of the new node since it may be a nested
      // destructuring pattern.
      visit(t, newLHS, newLHS.getParent());
    }
    nodeToDetach.detachFromParent();
    compiler.reportCodeChange();
  }

  private void visitDestructuringPatternInCatch(Node pattern) {
    String tempVarName = DESTRUCTURING_TEMP_VAR + (destructuringVarCounter++);
    Node catchBlock = pattern.getNext();

    pattern.getParent().replaceChild(pattern, IR.name(tempVarName));
    catchBlock.addChildToFront(
        IR.declaration(pattern, IR.name(tempVarName), Token.LET));
  }

  private void visitDestructuringPatternInEnhancedFor(Node pattern) {
    Node forNode;
    int declarationType;
    if (NodeUtil.isEnhancedFor(pattern.getParent())) {
      forNode = pattern.getParent();
      declarationType = Token.ASSIGN;
    } else {
      forNode = pattern.getParent().getParent();
      declarationType = pattern.getParent().getType();
      Preconditions.checkState(NodeUtil.isEnhancedFor(forNode));
    }

    String tempVarName = DESTRUCTURING_TEMP_VAR + (destructuringVarCounter++);
    Node block = forNode.getLastChild();
    if (declarationType == Token.ASSIGN) {
      pattern.getParent().replaceChild(pattern,
          IR.declaration(IR.name(tempVarName), Token.LET));
      block.addChildToFront(
          IR.exprResult(IR.assign(pattern, IR.name(tempVarName))));
    } else {
      pattern.getParent().replaceChild(pattern, IR.name(tempVarName));
      block.addChildToFront(
          IR.declaration(pattern, IR.name(tempVarName), declarationType));
    }
  }

  /**
   * Converts a member definition in an object literal to an ES3 key/value pair.
   * Member definitions in classes are handled in {@link #visitClass}.
   */
  private void visitMemberDefInObjectLit(Node n, Node parent) {
    String name = n.getString();
    Node stringKey = IR.stringKey(name, n.getFirstChild().detachFromParent());
    parent.replaceChild(n, stringKey);
    compiler.reportCodeChange();
  }

  /**
   * Converts extended object literal {a} to {a:a}.
   */
  private void visitStringKey(Node n) {
    if (!n.hasChildren()) {
      Node name = IR.name(n.getString());
      name.copyInformationFrom(n);
      n.addChildToBack(name);
      compiler.reportCodeChange();
    }
  }

  private void visitForOf(Node node, Node parent) {
    Node variable = node.removeFirstChild();
    Node iterable = node.removeFirstChild();
    Node body = node.removeFirstChild();

    Node iterName = IR.name(ITER_BASE + compiler.getUniqueNameIdSupplier().get());
    Node getNext = IR.call(IR.getprop(iterName.cloneTree(), IR.string("next")));
    String variableName;
    int declType;
    if (variable.isName()) {
      declType = Token.NAME;
      variableName = variable.getQualifiedName();
    } else {
      Preconditions.checkState(NodeUtil.isNameDeclaration(variable),
          "Expected var, let, or const. Got %s", variable);
      declType = variable.getType();
      variableName = variable.getFirstChild().getQualifiedName();
    }
    Node iterResult = IR.name(ITER_RESULT + variableName);

    Node makeIter = IR.call(
        NodeUtil.newQName(
            compiler, MAKE_ITER),
        iterable);
    compiler.needsEs6Runtime = true;

    Node init = IR.var(iterName.cloneTree(), makeIter);
    Node initIterResult = iterResult.cloneTree();
    initIterResult.addChildToFront(getNext.cloneTree());
    init.addChildToBack(initIterResult);

    Node cond = IR.not(IR.getprop(iterResult.cloneTree(), IR.string("done")));
    Node incr = IR.assign(iterResult.cloneTree(), getNext.cloneTree());

    Node declarationOrAssign;
    if (declType == Token.NAME) {
      declarationOrAssign = IR.exprResult(IR.assign(
          IR.name(variableName),
          IR.getprop(iterResult.cloneTree(), IR.string("value"))));
    } else {
      declarationOrAssign = new Node(declType, IR.name(variableName));
      declarationOrAssign.getFirstChild().addChildToBack(
          IR.getprop(iterResult.cloneTree(), IR.string("value")));
    }
    body.addChildToFront(declarationOrAssign);

    Node newFor = IR.forNode(init, cond, incr, body);
    newFor.useSourceInfoIfMissingFromForTree(node);
    parent.replaceChild(node, newFor);
    compiler.reportCodeChange();
  }

  private void checkClassReassignment(Node clazz) {
    Node name = NodeUtil.getClassNameNode(clazz);
    Node enclosingFunction = NodeUtil.getEnclosingFunction(clazz);
    if (enclosingFunction == null) {
      return;
    }
    CheckClassAssignments checkAssigns = new CheckClassAssignments(name);
    NodeTraversal.traverse(compiler, enclosingFunction, checkAssigns);
  }

  /**
   * Processes trailing default and rest parameters.
   */
  private void visitParamList(Node paramList, Node function) {
    Node insertSpot = null;
    Node block = function.getLastChild();
    for (int i = 0; i < paramList.getChildCount(); i++) {
      Node param = paramList.getChildAtIndex(i);
      if (param.isDefaultValue()) {
        Node nameOrPattern = param.removeFirstChild();
        Node defaultValue = param.removeFirstChild();
        Node newParam;

        // Treat name=undefined (and equivalent) as if it was just name.  There
        // is no need to generate a (name===void 0?void 0:name) statement for
        // such arguments.
        boolean isNoop = false;
        if (!nameOrPattern.isName()) {
          // Do not try to optimise unless nameOrPattern is a simple name.
        } else if (defaultValue.isName()) {
          isNoop = "undefined".equals(defaultValue.getString());
        } else if (defaultValue.isVoid()) {
          // Any kind of 'void literal' is fine, but 'void fun()' or anything
          // else with side effects isn't.  We're not trying to be particularly
          // smart here and treat 'void {}' for example as if it could cause
          // side effects.  Any sane person will type 'name=undefined' or
          // 'name=void 0' so this should not be an issue.
          isNoop = NodeUtil.isImmutableValue(defaultValue.getFirstChild());
        }

        if (isNoop) {
          newParam = nameOrPattern.cloneTree();
        } else {
          newParam = nameOrPattern.isName() ? nameOrPattern
              : IR.name(DESTRUCTURING_TEMP_VAR + (destructuringVarCounter++));
          Node lhs = nameOrPattern.cloneTree();
          Node rhs = defaultValueHook(newParam.cloneTree(), defaultValue);
          Node newStatement = nameOrPattern.isName()
              ? IR.exprResult(IR.assign(lhs, rhs))
              : IR.var(lhs, rhs);
          newStatement.useSourceInfoIfMissingFromForTree(param);
          block.addChildAfter(newStatement, insertSpot);
          insertSpot = newStatement;
        }

        paramList.replaceChild(param, newParam);
        newParam.setOptionalArg(true);

        compiler.reportCodeChange();
      } else if (param.isRest()) { // rest parameter
        param.setType(Token.NAME);
        param.setVarArgs(true);
        String paramName = param.getString();

        Node newBlock = IR.block().useSourceInfoFrom(block);
        Node name = IR.name(paramName);
        Node let = IR.let(name, IR.name(REST_PARAMS))
            .useSourceInfoIfMissingFromForTree(block);
        newBlock.addChildToFront(let);
        if (insertSpot != null) {
          for (Node statement = insertSpot.getNext(); statement != null;
              statement = statement.getNext()) {
            newBlock.addChildToBack(statement.detachFromParent());
          }
        } else {
          for (Node child : block.children()) {
            newBlock.addChildToBack(child.detachFromParent());
          }
        }

        // Make sure rest parameters are typechecked
        JSTypeExpression type = null;
        JSDocInfo info = param.getJSDocInfo();
        if (info != null) {
          type = info.getType();
        } else {
          JSDocInfo functionInfo = function.getJSDocInfo();
          if (functionInfo != null) {
            type = functionInfo.getParameterType(paramName);
          }
        }
        if (type != null) {
          Node arrayType = IR.string("Array");
          Node typeNode = type.getRoot();
          if (typeNode.getType() != Token.ELLIPSIS) {
            compiler.report(JSError.make(typeNode, BAD_REST_PARAMETER_ANNOTATION));
          }
          Node memberType = typeNode.getType() == Token.ELLIPSIS
              ? typeNode.getFirstChild().cloneNode()
              : typeNode.cloneNode();
          arrayType.addChildToFront(
              new Node(Token.BLOCK, memberType).copyInformationFrom(typeNode));
          JSDocInfoBuilder builder = new JSDocInfoBuilder(false);
          builder.recordType(new JSTypeExpression(new Node(Token.BANG, arrayType),
              param.getSourceFileName()));
          name.setJSDocInfo(builder.build());
        }

        Node newArr = IR.var(IR.name(REST_PARAMS), IR.arraylit());
        block.addChildAfter(newArr.useSourceInfoIfMissingFromForTree(param), insertSpot);
        Node init = IR.var(IR.name(REST_INDEX), IR.number(i));
        Node cond = IR.lt(IR.name(REST_INDEX),
            IR.getprop(IR.name("arguments"), IR.string("length")));
        Node incr = IR.inc(IR.name(REST_INDEX), false);
        Node body = IR.block(IR.exprResult(IR.assign(
            IR.getelem(IR.name(REST_PARAMS), IR.sub(IR.name(REST_INDEX), IR.number(i))),
            IR.getelem(IR.name("arguments"), IR.name(REST_INDEX)))));
        block.addChildAfter(IR.forNode(init, cond, incr, body)
            .useSourceInfoIfMissingFromForTree(param), newArr);
        block.addChildToBack(newBlock);
        compiler.reportCodeChange();
      } else if (param.isDestructuringPattern()) {
        String tempVarName = DESTRUCTURING_TEMP_VAR + (destructuringVarCounter++);
        paramList.replaceChild(param, IR.name(tempVarName));
        Node newDecl = IR.var(param, IR.name(tempVarName));
        block.addChildAfter(newDecl, insertSpot);
        insertSpot = newDecl;
      }
    }
    // For now, we are running transpilation before type-checking, so we'll
    // need to make sure changes don't invalidate the JSDoc annotations.
    // Therefore we keep the parameter list the same length and only initialize
    // the values if they are set to undefined.
  }

  /**
   * Processes array literals or calls containing spreads.
   * Eg.: [1, 2, ...x, 4, 5] => [1, 2].concat(x, [4, 5]);
   * Eg.: f(...arr) => f.apply(null, arr)
   * Eg.: new F(...args) => new Function.prototype.bind.apply(F, [].concat(args))
   */
  private void visitArrayLitOrCallWithSpread(Node node, Node parent) {
    Preconditions.checkArgument(node.isCall() || node.isArrayLit() || node.isNew());
    List<Node> groups = new ArrayList<>();
    Node currGroup = null;
    Node callee = node.isArrayLit() ? null : node.removeFirstChild();
    Node currElement = node.removeFirstChild();
    while (currElement != null) {
      if (currElement.isSpread()) {
        if (currGroup != null) {
          groups.add(currGroup);
          currGroup = null;
        }
        groups.add(currElement.removeFirstChild());
      } else {
        if (currGroup == null) {
          currGroup = IR.arraylit();
        }
        currGroup.addChildToBack(currElement);
      }
      currElement = node.removeFirstChild();
    }
    if (currGroup != null) {
      groups.add(currGroup);
    }
    Node result = null;
    Node joinedGroups = IR.call(IR.getprop(IR.arraylit(), IR.string("concat")),
            groups.toArray(new Node[groups.size()]));
    if (node.isArrayLit()) {
      result = joinedGroups;
    } else if (node.isCall()) {
      if (NodeUtil.mayHaveSideEffects(callee) && callee.isGetProp()) {
        Node statement = node;
        while (!NodeUtil.isStatement(statement)) {
          statement = statement.getParent();
        }
        Node freshVar = IR.name(FRESH_SPREAD_VAR + compiler.getUniqueNameIdSupplier().get());
        Node n = IR.var(freshVar.cloneTree());
        n.useSourceInfoIfMissingFromForTree(statement);
        statement.getParent().addChildBefore(n, statement);
        callee.addChildToFront(IR.assign(freshVar.cloneTree(), callee.removeFirstChild()));
        result = IR.call(
            IR.getprop(callee, IR.string("apply")),
            freshVar,
            joinedGroups);
      } else {
        Node context = callee.isGetProp() ? callee.getFirstChild().cloneTree() : IR.nullNode();
        result = IR.call(IR.getprop(callee, IR.string("apply")), context, joinedGroups);
      }
    } else {
      Node bindApply = NodeUtil.newQName(compiler,
          "Function.prototype.bind.apply");
      result = IR.newNode(bindApply, callee, joinedGroups);
    }
    result.useSourceInfoIfMissingFromForTree(node);
    parent.replaceChild(node, result);
    compiler.reportCodeChange();
  }

  private void visitObjectWithComputedProperty(Node obj, Node parent) {
    Preconditions.checkArgument(obj.isObjectLit());
    List<Node> props = new ArrayList<>();
    Node currElement = obj.getFirstChild();

    while (currElement != null) {
      if (currElement.getBooleanProp(Node.COMPUTED_PROP_GETTER)
          || currElement.getBooleanProp(Node.COMPUTED_PROP_SETTER)) {
        cannotConvertYet(currElement, "computed getter/setter");
        return;
      } else if (currElement.isGetterDef() || currElement.isSetterDef()) {
        currElement = currElement.getNext();
      } else {
        Node nextNode = currElement.getNext();
        obj.removeChild(currElement);
        props.add(currElement);
        currElement = nextNode;
      }
    }

    String objName = FRESH_COMP_PROP_VAR + compiler.getUniqueNameIdSupplier().get();

    props = Lists.reverse(props);
    Node result = IR.name(objName);
    for (Node propdef : props) {
      if (propdef.isComputedProp()) {
        Node propertyExpression = propdef.removeFirstChild();
        Node value = propdef.removeFirstChild();
        result = IR.comma(
            IR.assign(
                IR.getelem(
                    IR.name(objName),
                    propertyExpression),
                value),
            result);
      } else {
        if (!propdef.hasChildren()) {
          Node name = IR.name(propdef.getString()).copyInformationFrom(propdef);
          propdef.addChildToBack(name);
        }
        Node val = propdef.removeFirstChild();
        propdef.setType(Token.STRING);
        int type = propdef.isQuotedString() ? Token.GETELEM : Token.GETPROP;
        Node access = new Node(type, IR.name(objName), propdef);
        result = IR.comma(IR.assign(access, val), result);
      }
    }

    Node statement = obj;
    while (!NodeUtil.isStatement(statement)) {
      statement = statement.getParent();
    }

    result.useSourceInfoIfMissingFromForTree(obj);
    parent.replaceChild(obj, result);

    Node var = IR.var(IR.name(objName), obj);
    var.useSourceInfoIfMissingFromForTree(statement);
    statement.getParent().addChildBefore(var, statement);
    compiler.reportCodeChange();
  }

  /**
   * Classes are processed in 3 phases:
   * <ol>
   *   <li>The class name is extracted.
   *   <li>Class members are processed and rewritten.
   *   <li>The constructor is built.
   * </ol>
   */
  private void visitClass(Node classNode, Node parent) {
    checkClassReassignment(classNode);
    // Collect Metadata
    ClassDeclarationMetadata metadata = ClassDeclarationMetadata.create(classNode, parent);

    if (metadata == null || metadata.fullClassName == null) {
      cannotConvert(parent, "Can only convert classes that are declarations or the right hand"
          + " side of a simple assignment.");
      return;
    }
    if (metadata.hasSuperClass() && !metadata.superClassNameNode.isQualifiedName()) {
      compiler.report(JSError.make(metadata.superClassNameNode, DYNAMIC_EXTENDS_TYPE));
      return;
    }

    boolean useUnique = NodeUtil.isStatement(classNode) && !NodeUtil.isInFunction(classNode);
    String uniqueFullClassName =
        useUnique ? getUniqueClassName(metadata.fullClassName) : metadata.fullClassName;
    Node classNameAccess = NodeUtil.newQName(compiler, uniqueFullClassName);
    Node prototypeAccess = NodeUtil.newPropertyAccess(compiler, classNameAccess, "prototype");

    Preconditions.checkState(NodeUtil.isStatement(metadata.insertionPoint),
        "insertion point must be a statement: %s", metadata.insertionPoint);

    Node constructor = null;
    JSDocInfo ctorJSDocInfo = null;
    // Process all members of the class
    Node classMembers = classNode.getLastChild();
    Map<String, JSTypeExpression> prototypeMembersToDeclare = new LinkedHashMap<>();
    Map<String, JSTypeExpression> classMembersToDeclare = new LinkedHashMap<>();
    for (Node member : classMembers.children()) {
      if (member.isEmpty()) {
        continue;
      }
      Preconditions.checkState(
          member.isMemberFunctionDef() || member.isGetterDef() || member.isSetterDef()
              || (member.isComputedProp() && !member.getBooleanProp(Node.COMPUTED_PROP_VARIABLE)),
          "Member variables should have been transpiled earlier: ", member);

      if (member.isGetterDef() || member.isSetterDef()) {
        JSTypeExpression typeExpr = getTypeFromGetterOrSetter(member).clone();
        addToDefinePropertiesObject(metadata, member);

        Map<String, JSTypeExpression> membersToDeclare =
            member.isStaticMember() ? classMembersToDeclare : prototypeMembersToDeclare;
        JSTypeExpression existingType = membersToDeclare.get(member.getString());
        if (existingType != null && !existingType.equals(typeExpr)) {
          compiler.report(JSError.make(member, CONFLICTING_GETTER_SETTER_TYPE, member.getString()));
        } else {
          membersToDeclare.put(member.getString(), typeExpr);
        }
      } else if (member.isMemberFunctionDef() && member.getString().equals("constructor")) {
        ctorJSDocInfo = member.getJSDocInfo();
        constructor = member.getFirstChild().detachFromParent();
        if (!metadata.anonymous) {
          // Turns class Foo { constructor: function() {} } into function Foo() {},
          // i.e. attaches the name the ctor function.
          constructor.replaceChild(
              constructor.getFirstChild(), metadata.classNameNode.cloneNode());
        }
      } else {
        Node qualifiedMemberAccess =
            getQualifiedMemberAccess(compiler, member, classNameAccess, prototypeAccess);
        Node method = member.getLastChild().detachFromParent();

        Node assign = IR.assign(qualifiedMemberAccess, method);
        assign.useSourceInfoIfMissingFromForTree(member);

        JSDocInfo info = member.getJSDocInfo();
        if (member.isStaticMember() && NodeUtil.referencesThis(assign.getLastChild())) {
          JSDocInfoBuilder memberDoc = JSDocInfoBuilder.maybeCopyFrom(info);
          memberDoc.recordThisType(
              new JSTypeExpression(new Node(Token.BANG, new Node(Token.QMARK)),
              member.getSourceFileName()));
          info = memberDoc.build();
        }
        if (info != null) {
          assign.setJSDocInfo(info);
        }

        Node newNode = NodeUtil.newExpr(assign);
        metadata.insertNodeAndAdvance(newNode);
      }
    }

    // Add declarations for properties that were defined with a getter and/or setter,
    // so that the typechecker knows those properties exist on the class.
    // This is a temporary solution. Eventually, the type checker should understand
    // Object.defineProperties calls directly.
    for (Map.Entry<String, JSTypeExpression> entry : prototypeMembersToDeclare.entrySet()) {
      String declaredMember = entry.getKey();
      Node declaration = IR.getprop(prototypeAccess.cloneTree(), IR.string(declaredMember));
      JSDocInfoBuilder declInfo = new JSDocInfoBuilder(true);

      declInfo.recordType(entry.getValue());
      declaration.setJSDocInfo(declInfo.build());
      metadata.insertNodeAndAdvance(
          IR.exprResult(declaration).useSourceInfoIfMissingFromForTree(classNode));
    }
    for (Map.Entry<String, JSTypeExpression> entry : classMembersToDeclare.entrySet()) {
      String declaredMember = entry.getKey();
      Node declaration = IR.getprop(classNameAccess.cloneTree(), IR.string(declaredMember));
      JSDocInfoBuilder declInfo = new JSDocInfoBuilder(true);

      declInfo.recordType(entry.getValue());
      declaration.setJSDocInfo(declInfo.build());
      metadata.insertNodeAndAdvance(
          IR.exprResult(declaration).useSourceInfoIfMissingFromForTree(classNode));
    }

    if (metadata.definePropertiesObjForPrototype.hasChildren()) {
      Node definePropsCall =
          IR.exprResult(
              IR.call(
                  NodeUtil.newQName(compiler, "Object.defineProperties"),
                  prototypeAccess.cloneTree(),
                  metadata.definePropertiesObjForPrototype));
      definePropsCall.useSourceInfoIfMissingFromForTree(classNode);
      metadata.insertNodeAndAdvance(definePropsCall);
    }

    if (metadata.definePropertiesObjForClass.hasChildren()) {
      Node definePropsCall =
          IR.exprResult(
              IR.call(
                  NodeUtil.newQName(compiler, "Object.defineProperties"),
                  classNameAccess.cloneTree(),
                  metadata.definePropertiesObjForClass));
      definePropsCall.useSourceInfoIfMissingFromForTree(classNode);
      metadata.insertNodeAndAdvance(definePropsCall);
    }

    Preconditions.checkNotNull(constructor);

    JSDocInfo classJSDoc = NodeUtil.getBestJSDocInfo(classNode);
    JSDocInfoBuilder newInfo = JSDocInfoBuilder.maybeCopyFrom(classJSDoc);

    newInfo.recordConstructor();

    if (metadata.hasSuperClass()) {
      String superClassString = metadata.superClassNameNode.getQualifiedName();
      if (newInfo.isInterfaceRecorded()) {
        newInfo.recordExtendedInterface(new JSTypeExpression(new Node(Token.BANG,
            IR.string(superClassString)),
            metadata.superClassNameNode.getSourceFileName()));
      } else {
        Node inherits = IR.call(
            NodeUtil.newQName(compiler, INHERITS),
            NodeUtil.newQName(compiler, metadata.fullClassName),
            NodeUtil.newQName(compiler, superClassString));
        Node inheritsCall = IR.exprResult(inherits);
        compiler.needsEs6Runtime = true;

        inheritsCall.useSourceInfoIfMissingFromForTree(classNode);
        Node enclosingStatement = NodeUtil.getEnclosingStatement(classNode);
        enclosingStatement.getParent().addChildAfter(inheritsCall, enclosingStatement);
        newInfo.recordBaseType(new JSTypeExpression(new Node(Token.BANG,
            IR.string(superClassString)),
            metadata.superClassNameNode.getSourceFileName()));
      }
    }

    // Classes are @struct by default.
    if (!newInfo.isUnrestrictedRecorded() && !newInfo.isDictRecorded()
        && !newInfo.isStructRecorded()) {
      newInfo.recordStruct();
    }

    if (ctorJSDocInfo != null) {
      newInfo.recordSuppressions(ctorJSDocInfo.getSuppressions());
      for (String param : ctorJSDocInfo.getParameterNames()) {
        newInfo.recordParameter(param, ctorJSDocInfo.getParameterType(param));
      }
      newInfo.mergePropertyBitfieldFrom(ctorJSDocInfo);
    }

    if (NodeUtil.isStatement(classNode)) {
      constructor.getFirstChild().setString("");
      Node ctorVar = IR.let(metadata.classNameNode.cloneNode(), constructor);
      ctorVar.useSourceInfoIfMissingFromForTree(classNode);
      parent.replaceChild(classNode, ctorVar);
    } else {
      parent.replaceChild(classNode, constructor);
    }

    if (NodeUtil.isStatement(constructor)) {
      constructor.setJSDocInfo(newInfo.build());
    } else if (parent.isName()) {
      // The constructor function is the RHS of a var statement.
      // Add the JSDoc to the VAR node.
      Node var = parent.getParent();
      var.setJSDocInfo(newInfo.build());
    } else if (constructor.getParent().isName()) {
      // Is a newly created VAR node.
      Node var = constructor.getParent().getParent();
      var.setJSDocInfo(newInfo.build());
    } else if (parent.isAssign()) {
      // The constructor function is the RHS of an assignment.
      // Add the JSDoc to the ASSIGN node.
      parent.setJSDocInfo(newInfo.build());
    } else {
      throw new IllegalStateException("Unexpected parent node " + parent);
    }

    compiler.reportCodeChange();
  }

  /**
   * @param node A getter or setter node.
   */
  private JSTypeExpression getTypeFromGetterOrSetter(Node node) {
    JSDocInfo info = node.getJSDocInfo();

    if (info != null) {
      if (node.isGetterDef() && info.getReturnType() != null) {
        return info.getReturnType();
      } else {
        Set<String> paramNames = info.getParameterNames();
        if (paramNames.size() == 1) {
          return info.getParameterType(Iterables.getOnlyElement(info.getParameterNames()));
        }
      }
    }

    return new JSTypeExpression(new Node(Token.QMARK), node.getSourceFileName());
  }

  private void addToDefinePropertiesObject(ClassDeclarationMetadata metadata, Node member) {
    Node obj =
        member.isStaticMember()
            ? metadata.definePropertiesObjForClass
            : metadata.definePropertiesObjForPrototype;
    Node prop = NodeUtil.getFirstPropMatchingKey(obj, member.getString());
    if (prop == null) {
      prop =
          IR.objectlit(
              IR.stringKey("configurable", IR.trueNode()),
              IR.stringKey("enumerable", IR.trueNode()));
      obj.addChildToBack(IR.stringKey(member.getString(), prop));
    }

    Node function = member.getLastChild();
    JSDocInfoBuilder info = JSDocInfoBuilder.maybeCopyFrom(
        NodeUtil.getBestJSDocInfo(function));

    info.recordThisType(new JSTypeExpression(new Node(
        Token.BANG, IR.string(metadata.fullClassName)), member.getSourceFileName()));
    Node stringKey = IR.stringKey(
        member.isGetterDef() ? "get" : "set",
        function.detachFromParent());
    stringKey.setJSDocInfo(info.build());
    prop.addChildToBack(stringKey);
    prop.useSourceInfoIfMissingFromForTree(member);
  }

  /**
   * Constructs a Node that represents an access to the given class member, qualified by either the
   * static or the instance access context, depending on whether the member is static.
   *
   * <p><b>WARNING:</b> {@code member} may be modified/destroyed by this method, do not use it
   * afterwards.
   */
  static Node getQualifiedMemberAccess(AbstractCompiler compiler, Node member, Node staticAccess,
      Node instanceAccess) {
    Node context = member.isStaticMember() ? staticAccess : instanceAccess;
    context = context.cloneTree();
    if (member.isComputedProp()) {
      return IR.getelem(context, member.removeFirstChild());
    } else {
      return NodeUtil.newPropertyAccess(compiler, context, member.getString());
    }
  }

  /**
   * Converts ES6 arrow functions to standard anonymous ES3 functions.
   */
  private void visitArrowFunction(NodeTraversal t, Node n) {
    n.setIsArrowFunction(false);
    Node body = n.getLastChild();
    if (!body.isBlock()) {
      body.detachFromParent();
      body = IR.block(IR.returnNode(body).srcref(body)).srcref(body);
      n.addChildToBack(body);
    }

    UpdateThisAndArgumentsReferences updater =
        new UpdateThisAndArgumentsReferences();
    NodeTraversal.traverse(compiler, body, updater);
    addVarDecls(t, updater.changedThis, updater.changedArguments);

    compiler.reportCodeChange();
  }

  private void addVarDecls(
      NodeTraversal t, boolean addThis, boolean addArguments) {
    Scope scope = t.getScope();
    if (scope.isDeclared(THIS_VAR, false)) {
      addThis = false;
    }
    if (scope.isDeclared(ARGUMENTS_VAR, false)) {
      addArguments = false;
    }

    Node parent = t.getScopeRoot();
    if (parent.isFunction()) {
      // Add the new node at the beginning of the function body.
      parent = parent.getLastChild();
    }
    if (parent.isSyntheticBlock() && parent.getFirstChild().isScript()) {
      // Add the new node inside the SCRIPT node instead of the
      // synthetic block that contains it.
      parent = parent.getFirstChild();
    }

    CompilerInput input = compiler.getInput(parent.getInputId());
    if (addArguments) {
      Node name = IR.name(ARGUMENTS_VAR);
      Node argumentsVar = IR.declaration(name, IR.name("arguments"), Token.CONST);
      argumentsVar.useSourceInfoIfMissingFromForTree(parent);
      parent.addChildToFront(argumentsVar);
      scope.declare(ARGUMENTS_VAR, name, input);
    }
    if (addThis) {
      Node name = IR.name(THIS_VAR);
      Node thisVar = IR.declaration(name, IR.thisNode(), Token.CONST);
      thisVar.useSourceInfoIfMissingFromForTree(parent);
      parent.addChildToFront(thisVar);
      scope.declare(THIS_VAR, name, input);
    }
  }

  private static String getUniqueClassName(String qualifiedName) {
    return qualifiedName;
  }

  /**
   * Helper for transpiling DEFAULT_VALUE trees.
   */
  private static Node defaultValueHook(Node getprop, Node defaultValue) {
    return IR.hook(
        IR.sheq(getprop, IR.name("undefined")),
        defaultValue,
        getprop.cloneTree());
  }

  private static class UpdateThisAndArgumentsReferences
      implements NodeTraversal.Callback {
    private boolean changedThis = false;
    private boolean changedArguments = false;

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      if (n.isThis()) {
        Node name = IR.name(THIS_VAR).srcref(n);
        parent.replaceChild(n, name);
        changedThis = true;
      } else if (n.isName() && n.getString().equals("arguments")) {
        Node name = IR.name(ARGUMENTS_VAR).srcref(n);
        parent.replaceChild(n, name);
        changedArguments = true;
      }
    }

    @Override
    public boolean shouldTraverse(NodeTraversal t, Node n, Node parent) {
      return !n.isFunction() || n.isArrowFunction();
    }
  }

  private class CheckClassAssignments extends NodeTraversal.AbstractPostOrderCallback {
    private Node className;

    public CheckClassAssignments(Node className) {
      this.className = className;
    }

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      if (!n.isAssign() || n.getFirstChild() == className) {
        return;
      }
      if (className.matchesQualifiedName(n.getFirstChild())) {
        compiler.report(JSError.make(n, CLASS_REASSIGNMENT));
      }
    }

  }

  private void cannotConvert(Node n, String message) {
    compiler.report(JSError.make(n, CANNOT_CONVERT, message));
  }

  /**
   * Warns the user that the given ES6 feature cannot be converted to ES3
   * because the transpilation is not yet implemented. A call to this method
   * is essentially a "TODO(tbreisacher): Implement {@code feature}" comment.
   */
  private void cannotConvertYet(Node n, String feature) {
    compiler.report(JSError.make(n, CANNOT_CONVERT_YET, feature));
  }

  /**
   * Represents static metadata on a class declaration expression - i.e. the qualified name that a
   * class declares (directly or by assignment), whether it's anonymous, and where transpiled code
   * should be inserted (i.e. which object will hold the prototype after transpilation).
   */
  static class ClassDeclarationMetadata {
    /** A statement node. Transpiled methods etc of the class are inserted after this node. */
    private Node insertionPoint;

    /**
     * An object literal node that will be used in a call to Object.defineProperties, to add getters
     * and setters to the prototype.
     */
    private final Node definePropertiesObjForPrototype;

    /**
     * An object literal node that will be used in a call to Object.defineProperties, to add getters
     * and setters to the class.
     */
    private final Node definePropertiesObjForClass;

    /**
     * The fully qualified name of the class, which will be used in the output. May come from the
     * class itself or the LHS of an assignment.
     */
    final String fullClassName;
    /** Whether the constructor function in the output should be anonymous. */
    final boolean anonymous;
    final Node classNameNode;
    final Node superClassNameNode;

    private ClassDeclarationMetadata(Node insertionPoint, String fullClassName,
        boolean anonymous, Node classNameNode, Node superClassNameNode) {
      this.insertionPoint = insertionPoint;
      this.definePropertiesObjForClass = IR.objectlit();
      this.definePropertiesObjForPrototype = IR.objectlit();
      this.fullClassName = fullClassName;
      this.anonymous = anonymous;
      this.classNameNode = classNameNode;
      this.superClassNameNode = superClassNameNode;
    }

    static ClassDeclarationMetadata create(Node classNode, Node parent) {
      Node classNameNode = classNode.getFirstChild();
      Node superClassNameNode = classNameNode.getNext();

      // If this is a class statement, or a class expression in a simple
      // assignment or var statement, convert it. In any other case, the
      // code is too dynamic, so return null.
      if (NodeUtil.isStatement(classNode)) {
        return new ClassDeclarationMetadata(classNode, classNameNode.getString(), false,
            classNameNode, superClassNameNode);
      } else if (parent.isAssign() && parent.getParent().isExprResult()) {
        // Add members after the EXPR_RESULT node:
        // example.C = class {}; example.C.prototype.foo = function() {};
        String fullClassName = parent.getFirstChild().getQualifiedName();
        if (fullClassName == null) {
          return null;
        }
        return new ClassDeclarationMetadata(parent.getParent(), fullClassName, true, classNameNode,
            superClassNameNode);
      } else if (parent.isName()) {
        // Add members after the 'var' statement.
        // var C = class {}; C.prototype.foo = function() {};
        return new ClassDeclarationMetadata(parent.getParent(), parent.getString(), true,
            classNameNode, superClassNameNode);
      } else {
        // Cannot handle this class declaration.
        return null;
      }
    }

    void insertNodeAndAdvance(Node newNode) {
      insertionPoint.getParent().addChildAfter(newNode, insertionPoint);
      insertionPoint = newNode;
    }

    boolean hasSuperClass() {
      return !superClassNameNode.isEmpty();
    }
  }
}
