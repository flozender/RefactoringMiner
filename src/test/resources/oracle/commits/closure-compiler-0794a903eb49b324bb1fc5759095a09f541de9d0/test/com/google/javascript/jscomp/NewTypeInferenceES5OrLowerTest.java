/*
 * Copyright 2013 The Closure Compiler Authors.
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

import com.google.common.base.Joiner;

import com.google.javascript.jscomp.newtypes.JSTypeCreatorFromJSDoc;

/**
 * @author blickly@google.com (Ben Lickly)
 * @author dimvar@google.com (Dimitris Vardoulakis)
 */

public final class NewTypeInferenceES5OrLowerTest extends NewTypeInferenceTestBase {

  public void testExterns() {
    typeCheck(
        "/** @param {Array<string>} x */ function f(x) {}; f([5]);",
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testVarDefinitionsInExterns() {
    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "var undecl = {};", "if (undecl) { undecl.x = 7 };");

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "var undecl = {};",
        "function f() { if (undecl) { undecl.x = 7 }; }");

    typeCheckCustomExterns(DEFAULT_EXTERNS + "var undecl;", "undecl(5);");

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "/** @type {number} */ var num;", "num - 5;");

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "var maybeStr; /** @type {string} */ var maybeStr;",
        "maybeStr - 5;");

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "/** @type {string} */ var str;", "str - 5;",
        NewTypeInference.INVALID_OPERAND_TYPE);

    // TODO(blickly): Warn if function in externs has body
    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "function f() {/** @type {string} */ var invisible;}",
        "invisible - 5;");
    //         VarCheck.UNDEFINED_VAR_ERROR);

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "/** @type {number} */ var num;",
        "/** @type {undefined} */ var x = num;",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "var untypedNum;",
        Joiner.on('\n').join(
            "function f(x) {",
            " x < untypedNum;",
            " untypedNum - 5;",
            "}",
            "f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testThisInAtTypeFunction() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){};",
        "/** @type {number} */ Foo.prototype.n;",
        "/** @type {function(this:Foo)} */ function f() { this.n = 'str' };"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "/** @type {function(this:gibberish)} */ function foo() {}",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "var /** function(this:Foo) */ x = function() {};"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {function(this:Foo)} x */",
        "function f(x) {}",
        "f(/** @type {function(this:Foo)} */ (function() {}));"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {number} */ this.prop = 1; }",
        "/** @type {function(this:Foo)} */",
        "function f() { this.prop = 'asdf'; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}",
        "/** @param {function(this:Foo)} x */",
        "function f(x) {}",
        "f(/** @type {function(this:Bar)} */ (function() {}));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "function f(/** function(this:Low) */ low,",
        "           /** function(this:High) */ high) {",
        "  var fun = (1 < 2) ? low : high;",
        "  var /** function(this:High) */ f2 = fun;",
        "  var /** function(this:Low) */ f3 = fun;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "function f(/** function(function(this:Low)) */ low,",
        "           /** function(function(this:High)) */ high) {",
        "  var fun = (1 < 2) ? low : high;",
        "  var /** function(function(this:High)) */ f2 = fun;",
        "  var /** function(function(this:Low)) */ f3 = fun;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/**",
        " * @template T",
        " * @param {function(this:Foo<T>)} fun",
        " */",
        "function f(fun) { return fun; }",
        "var /** function(this:Foo<string>) */ x =",
        "    f(/** @type {function(this:Foo<number>)} */ (function() {}));"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testThisInFunctionJsdoc() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "/** @type {number} */ Foo.prototype.n;",
        "/** @this {Foo} */",
        "function f() { this.n = 'str'; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "/** @this {gibberish} */ function foo() {}",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {number} */ this.prop = 1; }",
        "/** @this {Foo} */",
        "function f() { this.prop = 'asdf'; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  // TODO(dimvar): we must warn when a THIS fun isn't called as a method
  public void testDontCallMethodAsFunction() {
    typeCheck(Joiner.on('\n').join(
        "/** @type{function(this: Object)} */",
        "function f() {}",
        "f();"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype.method = function() {};",
        "var f = (new Foo).method;",
        "f();"));
  }

  public void testNewInFunctionJsdoc() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function h(/** function(new:Foo, ...number):number */ f) {",
        "  (new f()) - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/**",
        " * @template T",
        " * @param {function(new:Foo<T>)} fun",
        " */",
        "function f(fun) { return fun; }",
        "/** @type {function(new:Foo<number>)} */",
        "function f2() {}",
        "var /** function(new:Foo<string>) */ x = f(f2);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(x) {",
        "  x();",
        "  var /** !Foo */ y = new x();",
        "  var /** function(new:Foo, number) */ z = x;",
        "}"));

    // TODO(dimvar): this is a bogus warning, x can be arbitrary and does not
    // need to have the num property; see NominalType#getConstructorObject
    // for what needs to change.
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @type {number} */",
        "Foo.num = 123;",
        "function f(/** function(new:Foo, string) */ x) {",
        "  var /** string */ s = x.num;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testAlhpaRenamingDoesntChangeType() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {U} x",
        " * @param {U} y",
        " * @template U",
        " */",
        "function f(x, y){}",
        "/**",
        " * @template T",
        " * @param {function(T, T): boolean} comp",
        " * @param {!Array<T>} arr",
        " */",
        "function g(comp, arr) {",
        "  var compare = comp || f;",
        "  compare(arr[0], arr[1]);",
        "}"));
  }

  public void testInvalidThisReference() {
    typeCheck("this.x = 5;", CheckGlobalThis.GLOBAL_THIS);

    typeCheck("function f(x){}; f(this);");

    typeCheck("function f(){ return this; }");

    typeCheck("function f() { this.p = 1; }", CheckGlobalThis.GLOBAL_THIS);

    typeCheck("function f() { return this.p; }", CheckGlobalThis.GLOBAL_THIS);

    typeCheck("function f() { this['p']; }", CheckGlobalThis.GLOBAL_THIS);

    // TODO(dimvar): Will be fixed once we handle any JSType as a receiver type
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @this {T}",
        " */",
        "function f(x) {",
        "  this.p = 123;",
        "}"),
        CheckGlobalThis.GLOBAL_THIS);
  }

  public void testSuperClassWithUndeclaredProps() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Error() {};",
        "Error.prototype.sourceURL;",
        "/** @constructor @extends {Error} */ function SyntaxError() {}"));
  }

  public void testInheritMethodFromParent() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "/** @param {string} x */ Foo.prototype.method = function(x) {};",
        "/** @constructor @extends {Foo} */ function Bar() {};",
        "(new Bar).method(4)"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testSubClassWithUndeclaredProps() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Super() {};",
        "/** @type {string} */ Super.prototype.str;",
        "/** @constructor @extends {Super} */ function Sub() {};",
        "Sub.prototype.str;"));
  }

  public void testUseBeforeDeclaration() {
    // typeCheck("x; var x;", VariableReferenceCheck.EARLY_REFERENCE);

    // typeCheck("x = 7; var x;", VariableReferenceCheck.EARLY_REFERENCE);

    typeCheck(Joiner.on('\n').join(
        "function f() { return 9; }",
        "var x = f();",
        "x - 7;"));
  }

  // public void testUseWithoutDeclaration() {
  //   typeCheck("x;", VarCheck.UNDEFINED_VAR_ERROR);
  //   typeCheck("x = 7;", VarCheck.UNDEFINED_VAR_ERROR);
  //   typeCheck("var y = x;", VarCheck.UNDEFINED_VAR_ERROR);
  // }

  // public void testVarRedeclaration() {
  //   typeCheck(
  //       "function f(x) { var x; }",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "function f(x) { function x() {} }",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "function f(x) { /** @typedef {number} */ var x; }",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "var x; var x;",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "var x; function x() {}",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "var x; /** @typedef {number} */ var x;",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "function x() {} function x() {}",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "function x() {} var x;",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "function x() {} /** @typedef {number} */ var x;",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "/** @typedef {number} */ var x; /** @typedef {number} */ var x;",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "/** @typedef {number} */ var x; var x;",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "/** @typedef {number} */ var x; function x() {}",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck("var f = function g() {}; function f() {};",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck("var f = function g() {}; var f = function h() {};",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck("var g = function f() {}; var h = function f() {};");

  //   typeCheck(
  //       "var x; /** @enum */ var x = { ONE: 1 };",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck(
  //       "/** @enum */ var x = { ONE: 1 }; var x;",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);
  // }

  public void testDeclaredVariables() {
    typeCheck("var /** null */ obj = 5;", NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "var /** ?number */ n = true;", NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testEmptyBlockPropagation() {
    typeCheck(
        "var x = 5; { }; var /** string */ s = x",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testForLoopInference() {
    typeCheck(Joiner.on('\n').join(
        "var x = 5;",
        "for (;true;) {",
        "  x = 'str';",
        "}",
        "var /** (string|number) */ y = x;",
        "(function(/** string */ s){})(x);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var x = 5;",
        "while (true) {",
        "  x = 'str';",
        "}",
        "(function(/** string */ s){})(x);",
        "var /** (string|number) */ y = x;"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "while (true) {",
        "  var x = 'str';",
        "}",
        "var /** (string|undefined) */ y = x;",
        "(function(/** string */ s){})(x);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "for (var x = 5; x < 10; x++) {}",
        "(function(/** string */ s){})(x);",
        "var /** number */ y = x;"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testConditionalSpecialization() {
    typeCheck(Joiner.on('\n').join(
        "var x, y = 5;",
        "if (true) {",
        "  x = 5;",
        "} else {",
        "  x = 'str';",
        "}",
        "if (x === 5) {",
        "  y = x;",
        "}",
        "y - 5"));

    typeCheck(Joiner.on('\n').join(
        "var x, y = 5;",
        "if (true) {",
        "  x = 5;",
        "} else {",
        "  x = null;",
        "}",
        "if (x !== null) {",
        "  y = x;",
        "}",
        "y - 5"));

    typeCheck(Joiner.on('\n').join(
        "var x, y;",
        "if (true) {",
        "  x = 5;",
        "} else {",
        "  x = null;",
        "}",
        "if (x === null) {",
        "  y = 5;",
        "} else {",
        "  y = x;",
        "}",
        "y - 5"));

    typeCheck(Joiner.on('\n').join(
        "var numOrNull = true ? null : 1",
        "if (null === numOrNull) { var /** null */ n = numOrNull; }"));
  }

  public void testUnspecializedStrictComparisons() {
    typeCheck(
        "var /** number */ n = (1 === 2);",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testAndOrConditionalSpecialization() {
    typeCheck(Joiner.on('\n').join(
        "var x, y = 5;",
        "if (true) {",
        "  x = 5;",
        "} else if (true) {",
        "  x = null;",
        "}",
        "if (x !== null && x !== undefined) {",
        "  y = x;",
        "}",
        "y - 5"));

    typeCheck(Joiner.on('\n').join(
        "var x, y;",
        "if (true) {",
        "  x = 5;",
        "} else if (true) {",
        "  x = null;",
        "}",
        "if (x === null || x === void 0) {",
        "  y = 5;",
        "} else {",
        "  y = x;",
        "}",
        "y - 5"));

    typeCheck(Joiner.on('\n').join(
        "var x, y = 5;",
        "if (true) {",
        "  x = 5;",
        "} else if (true) {",
        "  x = null;",
        "}",
        "if (x === null || x === undefined) {",
        "  y = x;",
        "}",
        "var /** (number|null|undefined) **/ z = y;",
        "(function(/** (number|null) */ x){})(y);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var x, y;",
        "if (true) {",
        "  x = 5;",
        "} else if (true) {",
        "  x = null;",
        "}",
        "if (x !== null && x !== undefined) {",
        "  y = 5;",
        "} else {",
        "  y = x;",
        "}",
        "var /** (number|null|undefined) **/ z = y;",
        "(function(/** (number|null) */ x){})(y);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var x, y = 5;",
        "if (true) {",
        "  x = 5;",
        "} else {",
        "  x = 'str';",
        "}",
        "if (x === 7 || x === 8) {",
        "  y = x;",
        "}",
        "y - 5"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function C(){}",
        "var obj = new C;",
        "if (obj || false) { 123, obj.asdf; }"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  (typeof x === 'number') && (x - 5);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|string|null) */ x) {",
        "  (x && (typeof x === 'number')) && (x - 5);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|string|null) */ x) {",
        "  (x && (typeof x === 'string')) && (x - 5);",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|string|null) */ x) {",
        "  typeof x === 'string' && x;",
        "  x < 'asdf';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testLoopConditionSpecialization() {
    typeCheck(Joiner.on('\n').join(
        "var x = true ? null : 'str';",
        "while (x !== null) {}",
        "var /** null */ y = x;"));

    typeCheck(Joiner.on('\n').join(
        "var x = true ? null : 'str';",
        "for (;x !== null;) {}",
        "var /** null */ y = x;"));

    typeCheck(Joiner.on('\n').join(
        "for (var x = true ? null : 'str'; x === null;) {}",
        "var /** string */ y = x;"));

    typeCheck(Joiner.on('\n').join(
        "var x;",
        "for (x = true ? null : 'str'; x === null;) {}",
        "var /** string */ y = x;"));

    typeCheck(Joiner.on('\n').join(
        "var x = true ? null : 'str';",
        "do {} while (x === null);",
        "var /** string */ y = x;"));
  }

  public void testVarDecls() {
    typeCheck("/** @type {number} */ var x, y;", TypeCheck.MULTIPLE_VAR_DEF);

    typeCheck(
        "var /** number */ x = 5, /** string */ y = 6;",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "var /** number */ x = 'str', /** string */ y = 'str2';",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testBadInitialization() {
    typeCheck(
        "/** @type {string} */ var s = 123;",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testBadAssignment() {
    typeCheck(
        "/** @type {string} */ var s; s = 123;",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testBadArithmetic() {
    typeCheck("123 - 'str';", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("123 * 'str';", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("123 / 'str';", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("123 % 'str';", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var y = 123; var x = 'str'; var z = x - y;",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var y = 123; var x; var z = x - y;",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("+true;"); // This is considered an explicit coercion

    typeCheck("true + 5;", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("5 + true;", NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testTypeAfterIF() {
    typeCheck(
        "var x = true ? 1 : 'str'; x - 1;",
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testSimpleBwdPropagation() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) { x - 5; }",
        "f(123);",
        "f('asdf')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { x++; }",
        "f(123);",
        "f('asdf')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(y) { var x = y; x - 5; }",
        "f(123);",
        "f('asdf')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(y) { var x; x = y; x - 5; }",
        "f(123);",
        "f('asdf')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { x + 5; }",
        "f(123);",
        "f('asdf')"));
  }

  public void testSimpleReturn() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) {}",
        "var /** undefined */ x = f();",
        "var /** number */ y = f();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { return; }",
        "var /** undefined */ x = f();",
        "var /** number */ y = f();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { return 123; }",
        "var /** undefined */ x = f();",
        "var /** number */ y = f();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { if (x) {return 123;} else {return 'asdf';} }",
        "var /** (string|number) */ x = f();"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) { if (x) {return 123;} }",
        "var /** (undefined|number) */ x = f();"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) { var y = x; y - 5; return x; }",
        "var /** undefined */ x = f(1);",
        "var /** number */ y = f(2);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testComparisons() {
    typeCheck(
        "1 < 0; 'a' < 'b'; true < false; null < null; undefined < undefined;");

    typeCheck(
        "/** @param {{ p1: ?, p2: ? }} x */ function f(x) { x.p1 < x.p2; }");

    typeCheck("function f(x, y) { x < y; }");

    typeCheck(
        "var x = 1; var y = true ? 1 : 'str'; x < y;",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var x = 'str'; var y = 1; x < y;",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = 1;",
        "  x < y;",
        "  return x;",
        "}",
        "var /** undefined */ x = f(1);",
        "var /** number */ y = f(2);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = x, z = 7;",
        "  y < z;",
        "}"));
  }

  public void testFunctionJsdoc() {
    typeCheck(Joiner.on('\n').join(
        "/** @param {number} n */",
        "function f(n) { n < 5; }"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {string} n */",
        "function f(n) { n < 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {string} */",
        "function f() { return 1; }"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {string} */",
        "function f() { return; }"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {string} */",
        "function f(s) { return s; }",
        "f(123);",
        "f('asdf')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {number} */",
        "function f() {}"),
        CheckMissingReturn.MISSING_RETURN_STATEMENT);

    typeCheck(Joiner.on('\n').join(
        "/** @return {(undefined|number)} */",
        "function f() { if (true) { return 'str'; } }"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(number)} fun */",
        "function f(fun) {}",
        "f(function (/** string */ s) {});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "/** @param {number} n */ function f(/** number */ n) {}",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck("/** @constructor */ var Foo = function() {}; new Foo;");

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @param {number} x */ Foo.prototype.method = function(x) {};",
        "(new Foo).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.method = /** @param {number} x */ function(x) {};",
        "(new Foo).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.method = function(/** number */ x) {};",
        "(new Foo).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "/** @type {function(number)} */ function f(x) {}; f('asdf');",
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "/** @type {number} */ function f() {}",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @type {function():number} */",
        "function /** number */ f() { return 1; }"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(number) */ fnum, floose, cond) {",
        "  var y;",
        "  if (cond) {",
        "    y = fnum;",
        "  } else {",
        "    floose();",
        "    y = floose;",
        "  }",
        "  return y;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(): *} x */ function g(x) {}",
        "/** @param {function(number): string} x */ function f(x) {",
        "  g(x);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "var x = {}; x.a = function(/** string */ x) {}; x.a(123);",
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck("/** @param {function(...)} x */ function f(x) {}");

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " */",
        "function A() {};",
        "/** @return {number} */",
        "A.prototype.foo = function() {};"));

    typeCheck(
        "/** @param {number} x */ function f(y) {}",
        GlobalTypeInfo.INEXISTENT_PARAM);
  }

  public void testFunctionSubtyping() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}",
        "function f(/** function(new:Foo) */ x) {}",
        "f(Bar);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** function(new:Foo) */ x) {}",
        "f(function() {});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor @extends {Foo} */",
        "function Bar() {}",
        "function f(/** function(new:Foo) */ x) {}",
        "f(Bar);"));
  }

  public void testFunctionJoin() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/**",
        " * @param {function(new:Foo, (number|string))} x ",
        " * @param {function(new:Foo, number)} y ",
        " */",
        "function f(x, y) {",
        "  var z = 1 < 2 ? x : y;",
        "  return new z(123);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}",
        "/**",
        " * @param {function(new:Foo)} x ",
        " * @param {function(new:Bar)} y ",
        " */",
        "function f(x, y) {",
        "  var z = 1 < 2 ? x : y;",
        "  return new z();",
        "}"),
        NewTypeInference.NOT_A_CONSTRUCTOR);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** function(new:Foo) */ x, /** function() */ y) {",
        "  var z = 1 < 2 ? x : y;",
        "  return new z();",
        "}"),
        NewTypeInference.NOT_A_CONSTRUCTOR);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** function(new:Foo) */ x, /** function() */ y) {",
        "  var z = 1 < 2 ? x : y;",
        "  return z();",
        "}"));
  }

  public void testFunctionMeet() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/**",
        " * @param {function(new:Foo, (number|string))} x ",
        " * @param {function(new:Foo, number)} y ",
        " */",
        "function f(x, y) { if (x === y) { return x; } }"));
  }

  public void testRecordWithoutTypesJsdoc() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** {a, b} */ x) {}",
        "f({c: 123});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testBackwardForwardPathologicalCase() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) { var y = 5; y < x; }",
        "f(123);",
        "f('asdf')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testTopInitialization() {
    typeCheck("function f(x) { var y = x; y < 5; }");

    typeCheck("function f(x) { x < 5; }");

    typeCheck(
        "function f(x) { x - 5; x < 'str'; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = x; y - 5; y < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  // public void testMultipleFunctions() {
  //   typeCheck("function g() {};\n function f(x) { var x; };",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);

  //   typeCheck("function f(x) { var x; };\n function g() {};",
  //       VariableReferenceCheck.REDECLARED_VARIABLE);
  // }

  public void testSimpleCalls() {
    typeCheck("function f() {}; f(5);", TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck("function f(x) { x-5; }; f();", TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "/** @return {number} */ function f() { return 1; }",
        "var /** string */ s = f();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "function f(/** number */ x) {}; f(true);",
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** boolean */ x) {}",
        "function g() { f(123); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** void */ x) {}",
        "function g() { f(123); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** boolean */ x) {}",
        "function g(x) {",
        "  var /** string */ s = x;",
        "  f(x < 7);",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "function g(x, y) {",
        "  y < x;",
        "  f(x);",
        "  var /** string */ s = y;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testObjectType() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function takesObj(/** Object */ x) {}",
        "takesObj(new Foo);"));

    typeCheck(Joiner.on('\n').join(
        "function takesObj(/** Object */ x) {}",
        "takesObj(null);"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function /** Object */ returnsObj() { return {}; }",
        "function takesFoo(/** Foo */ x) {}",
        "takesFoo(returnsObj());"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck("Object.prototype.hasOwnProperty.call({}, 'asdf');");
  }

  public void testCallsWithComplexOperator() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "function fun(cond, /** !Foo */ f, /** !Bar */ g) {",
        "  (cond ? f : g)();",
        "}"),
        TypeCheck.NOT_CALLABLE);
  }

  public void testDeferredChecks() {
    typeCheck(Joiner.on('\n').join(
        "function f() { return 'str'; }",
        "function g() { f() - 5; }"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { x - 5; }",
        "f(5 < 6);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) { x - y; }",
        "f(5);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "function f() { return 'str'; }",
        "function g() { var x = f(); x - 7; }"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, y) { return x-y; }",
        "f(5, 'str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {number} */ function f(x) { return x; }",
        "f('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) { return x; }",
        "function g(x) {",
        "  var /** string */ s = f(x);",
        "};"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f() { new Foo('asdf'); }",
        "/** @constructor */ function Foo(x) { x - 5; }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Arr() {}",
        "/**",
        " * @template T",
        " * @param {...T} var_args",
        " */",
        "Arr.prototype.push = function(var_args) {};",
        "function f(x) {}",
        "var renameByParts = function(parts) {",
        "  var mapped = new Arr();",
        "  mapped.push(f(parts));",
        "};"));

    // Here we don't want a deferred check and an INVALID_INFERRED_RETURN_TYPE
    // warning b/c the return type is declared.
    typeCheck(Joiner.on('\n').join(
        "/** @return {string} */ function foo(){ return 'str'; }",
        "function g() { foo() - 123; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        " function x() {};",
        " function g() { x(1); }",
        " g();",
        "}"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    // We used to erroneously create a deferred check for the call to f
    // (and crash as a result), because we had a bug where the top-level
    // function was not being shadowed by the formal parameter.
    typeCheck(Joiner.on('\n').join(
        "function f() { return 123; }",
        "var outer = 123;",
        "function g(/** function(number) */ f) {",
        "  f(123) < 'str';",
        "  return outer;",
        "}"));
  }

  public void testShadowing() {
    typeCheck(Joiner.on('\n').join(
        "var /** number */ x = 5;",
        "function f() {",
        "  var /** string */ x = 'str';",
        "  return x - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var /** number */ x = 5;",
        "function f() {",
        "  /** @typedef {string} */ var x;",
        "  return x - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var /** number */ x = 5;",
        "function f() {",
        "  /** @enum {string} */ var x = { FOO : 'str' };",
        "  return x - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    // Types that are only present in types, and not in code do not cause shadowing in code
    typeCheck(Joiner.on('\n').join(
        "var /** number */ X = 5;",
        "/** @template X */",
        "function f() {",
        "  return X - 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "var /** string */ X = 'str';",
        "/** @template X */",
        "function f() {",
        "  return X - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testTypedefIsUndefined() {
    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  /** @typedef {string} */ var x;",
        "  /** @type {undefined} */ var y = x;",
        "}"));
  }

  public void testFunctionsInsideFunctions() {
    typeCheck(Joiner.on('\n').join(
        "(function() {",
        "  function f() {}; f(5);",
        "})();"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "(function() {",
        "  function f() { return 'str'; }",
        "  function g() { f() - 5; }",
        "})();"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var /** number */ x;",
        "function f() { x = 'str'; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var x;",
        "function f() { x - 5; x < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testCrossScopeWarnings() {
    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  x < 'str';",
        "}",
        "var x = 5;",
        "f()"),
        NewTypeInference.CROSS_SCOPE_GOTCHA);

    // CROSS_SCOPE_GOTCHA is only for undeclared variables
    typeCheck(Joiner.on('\n').join(
        "/** @type {string} */ var s;",
        "function f() {",
        "  s = 123;",
        "}",
        "f();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function g(x) {",
        "  function f() { x < 'str'; z < 'str'; x = 5; }",
        "  var z = x;",
        "  f();",
        "  x - 5;",
        "  z < 'str';",
        "}"));

    // TODO(dimvar): we can't do this yet; requires more info in the summary
    // typeCheck(Joiner.on('\n').join(
    //     "/** @constructor */",
    //     "function Foo() {",
    //     "  /** @type{?Object} */ this.prop = null;",
    //     "}",
    //     "Foo.prototype.initProp = function() { this.prop = {}; };",
    //     "var obj = new Foo();",
    //     "if (obj.prop == null) {",
    //     "  obj.initProp();",
    //     "  obj.prop.a = 123;",
    //     "}"));
  }

  public void testTrickyUnknownBehavior() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** function() */ x, cond) {",
        "  var y = cond ? x() : 5;",
        "  y < 'str';",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {function() : ?} x */ function f(x, cond) {",
        "  var y = cond ? x() : 5;",
        "  y < 'str';",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** function() */ x) {",
        "  x() < 'str';",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function g() { return {}; }",
        "function f() {",
        "  var /** ? */ x = g();",
        "  return x.y;",
        "}"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g() { return {}; }",
        "function f() {",
        "  var /** ? */ x = g()",
        "  x.y = 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function g(x) { return x; }",
        "function f(z) {",
        "  var /** ? */ x = g(z);",
        "  x.y2 = 123;",
        // specializing to a loose object here
        "  return x.y1 - 5;",
        "}"));
  }

  public void testDeclaredFunctionTypesInFormals() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** function():number */ x) {",
        "  var /** string */ s = x();",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(number) */ x) {",
        "  x(true);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g(x, y, /** function(number) */ f) {",
        "  y < x;",
        "  f(x);",
        "  var /** string */ s = y;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = x(); y - 5; y < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function():?} x */ function f(x) {",
        "  var y = x(); y - 5; y < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "function f(/** ? */ x) { x < 'asdf'; x - 5; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(number): string} x */ function g(x) {}",
        "/** @param {function(number): string} x */ function f(x) {",
        "  g(x);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(number): *} x */ function g(x) {}",
        "/** @param {function(*): string} x */ function f(x) {",
        "  g(x);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(*): string} x */ function g(x) {}",
        "/** @param {function(number): string} x */ function f(x) {",
        "  g(x);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(number): string} x */ function g(x) {}",
        "/** @param {function(number): *} x */ function f(x) {",
        "  g(x);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testSpecializedFunctions() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** function(string) : number */ x) {",
        "  if (x('str') === 5) {",
        "    x(5);",
        "  }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(string) : string */ x) {",
        "  if (x('str') === 5) {",
        "    x(5);",
        "  }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(string) */ x, y) {",
        "  y(1);",
        "  if (x === y) {",
        "    x(5);",
        "  }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x === null) {",
        "    return 5;",
        "  } else {",
        "    return x - 43;",
        "  }",
        "}",
        "f('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var goog = {};",
        "/** @type {!Function} */ goog.abstractMethod = function(){};",
        "/** @constructor */ function Foo(){};",
        "/** @return {!Foo} */ Foo.prototype.clone = goog.abstractMethod;",
        "/** @constructor @extends {Foo} */",
        "function Bar() {}",
        "/** @return {!Bar} */ Bar.prototype.clone = goog.abstractMethod;"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var goog = {};",
        "/** @type {!Function} */ goog.abstractMethod = function(){};",
        "/** @constructor */ function Foo(){};",
        "/** @return {!Foo} */ Foo.prototype.clone = goog.abstractMethod;",
        "/** @constructor @extends {Foo} */",
        "function Bar() {}",
        "/** @return {!Bar} */ Bar.prototype.clone = goog.abstractMethod;",
        "var /** null */ n = (new Bar).clone();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testDifficultObjectSpecialization() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function X() { this.p = 1; }",
        "/** @constructor */",
        "function Y() { this.p = 2; }",
        "/** @param {(!X|!Y)} a */",
        "function fn(a) {",
        "  a.p;",
        "  /** @type {!X} */ (a);",
        "}"));

    // Currently, two types that have a common subtype specialize to bottom
    // instead of to the common subtype. If we change that, then this test will
    // have no warnings.
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High1() {}",
        "/** @interface */",
        "function High2() {}",
        "/**",
        " * @constructor",
        " * @implements {High1}",
        " * @implements {High2}",
        " */",
        "function Low() {}",
        "function f(x) {",
        "  var /** !High1 */ v1 = x;",
        "  var /** !High2 */ v2 = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    // Currently, two types that have a common subtype specialize to bottom
    // instead of to the common subtype. If we change that, then this test will
    // have no warnings, and the type of x will be !Low.
    // (We must normalize the output of specialize to avoid getting (!Med|!Low))
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High1() {}",
        "/** @interface */",
        "function High2() {}",
        "/** @interface */",
        "function High3() {}",
        "/**",
        " * @interface",
        " * @extends {High1}",
        " * @extends {High2}",
        " */",
        "function Mid() {}",
        "/**",
        " * @interface",
        " * @extends {Mid}",
        " * @extends {High3}",
        " */",
        "function Low() {}",
        "function f(x) {",
        "  var /** !High1 */ v1 = x;",
        "  var /** (!High2|!High3) */ v2 = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testLooseConstructors() {
    typeCheck(Joiner.on('\n').join(
        "function f(ctor) {",
        "  new ctor(1);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(ctor) {",
        "  new ctor(1);",
        "}",
        "/** @constructor */ function Foo(/** string */ y) {}",
        "f(Foo);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testLooseFunctions() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(1);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(1);",
        "}",
        "function g(/** string */ y) {}",
        "f(g);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(1);",
        "}",
        "function g(/** number */ y) {}",
        "f(g);"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(1);",
        "}",
        "function g(/** (number|string) */ y) {}",
        "f(g);"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  5 - x(1);",
        "}",
        "/** @return {string} */",
        "function g(/** number */ y) { return ''; }",
        "f(g);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  5 - x(1);",
        "}",
        "/** @return {(number|string)} */",
        "function g(/** number */ y) { return 5; }",
        "f(g);"));

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  x(5);",
        "  y(5);",
        "  return x(y);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x();",
        "  return x;",
        "}",
        "function g() {}",
        "function h() { f(g) - 5; }"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, cond) {",
        "  x();",
        "  return cond ? 5 : x;",
        "}",
        "function g() {}",
        "function h() { f(g, true) - 5; }"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);
    // A loose function is a loose subtype of a non-loose function.
    // Traditional function subtyping would warn here.
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(5);",
        "  return x;",
        "}",
        "function g(x) {}",
        "function h() {",
        "  var /** function((number|string)) */ fun = f(g);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function g(/** string */ x) {}",
        "function f(x, y) {",
        "  y - 5;",
        "  x(y);",
        "  y + y;",
        "}",
        "f(g, 5)"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {string} */",
        "function g(/** number */ x) { return 'str'; }",
        "/** @return {number} */",
        "function f(x) {",
        "  var y = 5;",
        "  var z = x(y);",
        "  return z;",
        "}",
        "f(g)"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {number} */",
        "function g(/** number */ y) { return 6; }",
        "function f(x, cond) {",
        "  if (cond) {",
        "    5 - x(1);",
        "  } else {",
        "    x('str') < 'str';",
        "  }",
        "}",
        "f(g, true)"));

    typeCheck(Joiner.on('\n').join(
        "function f(g, cond) {",
        "  if (cond) {",
        "    g(5, cond);",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {function (number)|Function} x",
        " */",
        "function f(x) {};",
        "f(function () {});"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(true, 'asdf');",
        "  x(false);",
        "}"));
  }

  public void testBackwardForwardPathologicalCase2() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, /** string */ y, z) {",
        "  var w = z;",
        "  x < z;",
        "  w < y;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testNotCallable() {
    typeCheck(Joiner.on('\n').join(
        "/** @param {number} x */ function f(x) {",
        "  x(7);",
        "}"),
        TypeCheck.NOT_CALLABLE);
  }

  public void testSimpleLocallyDefinedFunction() {
    typeCheck(Joiner.on('\n').join(
        "function f() { return 'str'; }",
        "var x = f();",
        "x - 7;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f() { return 'str'; }",
        "f() - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "(function() {",
        "  function f() { return 'str'; }",
        "  f() - 5;",
        "})();"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "(function() {",
        "  function f() { return 'str'; }",
        "  f() - 5;",
        "})();"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testIdentityFunction() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) { return x; }",
        "5 - f(1);"));
  }

  public void testReturnTypeInferred() {
    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  var x = g();",
        "  var /** string */ s = x;",
        "  x - 5;",
        "};",
        "function g() { return 'str'};"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testGetpropOnNonObjects() {
    typeCheck("(null).foo;", NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "var /** undefined */ n;",
        "n.foo;"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck("var x = {}; x.foo.bar = 1;", TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "var /** undefined */ n;",
        "n.foo = 5;"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (x.prop) {",
        "    var /** { prop: ? } */ y = x;",
        "  }",
        "}"));

    // TODO(blickly): Currently, this warning is not good, referring to props of
    // BOTTOM. Ideally, we could warn about accessing a prop on undefined.
    typeCheck(Joiner.on('\n').join(
        "/** @param {undefined} x */",
        "function f(x) {",
        "  if (x.prop) {}",
        "}"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck("null[123];", NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(
        "function f(/** !Object */ x) { if (x[123]) { return 1; } }");

    typeCheck(
        "function f(/** undefined */ x) { if (x[123]) { return 1; } }",
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|null) */ n) {",
        "  n.foo;",
        "}"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|null|undefined) */ n) {",
        "  n.foo;",
        "}"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "function f(/** (!Object|number|null|undefined) */ n) {",
        "  n.foo;",
        "}"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "Foo.prototype.prop;",
        "function f(/** (!Foo|undefined) */ n) {",
        "  n.prop;",
        "}"),
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "/** @type {string} */ Foo.prototype.prop1;",
        "function g(/** Foo */ f) {",
        "  f.prop1.prop2 = 'str';",
        "};"),
        NewTypeInference.NULLABLE_DEREFERENCE);
  }

  public void testNonexistentProperty() {
    typeCheck(Joiner.on('\n').join(
        "/** @param {{ a: number }} obj */",
        "function f(obj) {",
        "  123, obj.b;",
        "  obj.b = 'str';",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck("({}).p < 'asdf';", TypeCheck.INEXISTENT_PROPERTY);

    typeCheck("(/** @type {?} */ (null)).prop - 123;");

    typeCheck("(/** @type {?} */ (null)).prop += 123;");

    typeCheck("var x = {}; var y = x.a;", TypeCheck.INEXISTENT_PROPERTY);

    typeCheck("var x = {}; x.y - 3; x.y = 5;", TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testNullableDereference() {
    typeCheck(
        "function f(/** ?{ p : number } */ o) { return o.p; }",
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() { /** @const */ this.p = 5; }",
        "function g(/** ?Foo */ f) { return f.p; }"),
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.p = function(){};",
        "function g(/** ?Foo */ f) { f.p(); }"),
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(
        "var f = 5 ? function() {} : null; f();",
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(
        "var f = 5 ? function(/** number */ n) {} : null; f('str');",
        NewTypeInference.NULLABLE_DEREFERENCE,
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** ?{ p : number } */ o) {",
        "  goog.asserts.assert(o);",
        "  return o.p;",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** ?{ p : number } */ o) {",
        "  goog.asserts.assertObject(o);",
        "  return o.p;",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** ?Array<string> */ a) {",
        "  goog.asserts.assertArray(a);",
        "  return a.length;",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.p = function(){};",
        "function g(/** ?Foo */ f) {",
        "  goog.asserts.assertInstanceof(f, Foo);",
        "  f.p();",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "function g(/** !Bar */ o) {",
        "  goog.asserts.assertInstanceof(o, Foo);",
        "}"),
        NewTypeInference.ASSERT_FALSE);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function g(/** !Foo */ o) {",
        "  goog.asserts.assertInstanceof(o, 42);",
        "}"),
        NewTypeInference.UNKNOWN_ASSERTION_TYPE);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function Bar() {}",
        "function g(/** !Foo */ o) {",
        "  goog.asserts.assertInstanceof(o, Bar);",
        "}"),
        NewTypeInference.UNKNOWN_ASSERTION_TYPE);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @interface */ function Bar() {}",
        "function g(/** !Foo */ o) {",
        "  goog.asserts.assertInstanceof(o, Bar);",
        "}"),
        NewTypeInference.UNKNOWN_ASSERTION_TYPE);
  }

  public void testAsserts() {
    typeCheck(
        CLOSURE_BASE + Joiner.on('\n').join(
            "function f(/** ({ p : string }|null|undefined) */ o) {",
            "  goog.asserts.assert(o);",
            "  o.p - 5;",
            "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function f(/** (Array<string>|Foo) */ o) {",
        "  goog.asserts.assert(o instanceof Array);",
        "  var /** string */ s = o.length;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.p = function(/** number */ x){};",
        "function f(/** (function(new:Foo)) */ ctor,",
        "           /** ?Foo */ o) {",
        "  goog.asserts.assertInstanceof(o, ctor);",
        "  o.p('str');",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  var y = x;",
        "  goog.asserts.assertInstanceof(y, Foo);",
        "}"));
  }

  public void testDontInferBottom() {
    typeCheck(
        // Ensure we don't infer bottom for x here
        "function f(x) { var /** string */ s; (s = x) - 5; } f(9);",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testDontInferBottomReturn() {
    typeCheck(
        // Technically, BOTTOM is correct here, but since using dead code is error prone,
        // we'd rather infer f to return TOP (and get a warning).
        "function f() { throw ''; } f() - 5;",
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testAssignToInvalidObject() {
    typeCheck(
        "n.foo = 5; var n;",
        // VariableReferenceCheck.EARLY_REFERENCE,
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);
  }

  public void testAssignmentDoesntFlowWrongInit() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ n) {",
        "  n = 'typo';",
        "  n - 5;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ n: number }} x */ function f(x) {",
        "  x.n = 'typo';",
        "  x.n - 5;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testPossiblyNonexistentProperties() {
    typeCheck(Joiner.on('\n').join(
        "/** @param {{ n: number }} x */ function f(x) {",
        "  if (x.p) {",
        "    return x.p;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p : string }} x */ function reqHasPropP(x){}",
        "/** @param {{ n: number }} x */ function f(x, cond) {",
        "  if (cond) {",
        "    x.p = 'str';",
        "  }",
        "  reqHasPropP(x);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ n: number }} x */ function f(x, cond) {",
        "  if (cond) { x.p = 'str'; }",
        "  if (x.p) {",
        "    x.p - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** { n : number } */ x) {",
        "  x.s = 'str';",
        "  return x.inexistentProp;",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testDeclaredRecordTypes() {
    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  return x.p - 3;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: string }} x */ function f(x) {",
        "  return x.p - 3;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ 'p': string }} x */ function f(x) {",
        "  return x.p - 3;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  return x.q;",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: string }} obj */ function f(obj, x, y) {",
        "  x < y;",
        "  x - 5;",
        "  obj.p < y;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  x.p = 3;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  x.p = 'str';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  x.q = 'str';",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  x.q = 'str';",
        "}",
        "/** @param {{ p: number }} x */ function g(x) {",
        "  f(x);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  x.q = 'str';",
        "  return x.q;",
        "}",
        "/** @param {{ p: number }} x */ function g(x) {",
        "  f(x) - 5;",
        "}"),
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} x */ function f(x) {",
        "  x.q = 'str';",
        "  x.q = 7;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** { prop: number} */ obj) {",
        "  obj.prop = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** { prop: number} */ obj, cond) {",
        "  if (cond) { obj.prop = 123; } else { obj.prop = 234; }",
        "  obj.prop = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** {p: number} */ x, /** {p: (number|null)} */ y) {",
        "  var z;",
        "  if (true) { z = x; } else { z = y; }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "var /** { a: number } */ obj1 = { a: 321};",
        "var /** { a: number, b: number } */ obj2 = obj1;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testSimpleObjectLiterals() {
    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} obj */",
        "function f(obj) {",
        "  obj = { p: 123 };",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number, p2: string }} obj */",
        "function f(obj) {",
        "  obj = { p: 123 };",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} obj */",
        "function f(obj) {",
        "  obj = { p: 'str' };",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var obj;",
        "obj = { p: 123 };",
        "obj.p < 'str';"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} obj */",
        "function f(obj, x) {",
        "  obj = { p: x };",
        "  x < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} obj */",
        "function f(obj, x) {",
        "  obj = { p: 123, q: x };",
        "  obj.q - 5;",
        "  x < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
    // An example of how record types can hide away the extra properties and
    // allow type misuse.
    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} obj */",
        "function f(obj) {",
        "  obj.q = 123;",
        "}",
        "/** @param {{ p: number, q: string }} obj */",
        "function g(obj) { f(obj); }"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {{ p: number }} obj */",
        "function f(obj) {}",
        "var obj = {p: 5};",
        "if (true) {",
        "  obj.q = 123;",
        "}",
        "f(obj);"));

    typeCheck(
        "function f(/** number */ n) {}; f({});",
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testInferPreciseTypeWithDeclaredUnknown() {
    typeCheck(
        "var /** ? */ x = 'str'; x - 123;",
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testSimpleLooseObjects() {
    typeCheck("function f(obj) { obj.x = 1; obj.x - 5; }");

    typeCheck(
        "function f(obj) { obj.x = 'str'; obj.x - 5; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  var /** number */ x = obj.p;",
        "  obj.p < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  var /** @type {{ p: number }} */ x = obj;",
        "  obj.p < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  obj.x = 1;",
        "  return obj.x;",
        "}",
        "f({x: 'str'});"));

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  obj.x - 1;",
        "}",
        "f({x: 'str'});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj, cond) {",
        "  if (cond) {",
        "    obj.x = 'str';",
        "  }",
        "  obj.x - 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  obj.x - 1;",
        "  return obj;",
        "}",
        "var /** string */ s = (f({x: 5})).x;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }


  public void testNestedLooseObjects() {
    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  obj.a.b = 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  obj.a.b = 123;",
        "  obj.a.b < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj, cond) {",
        "  (cond ? obj : obj).x - 1;",
        "  return obj.x;",
        "}",
        "f({x: 'str'}, true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  obj.a.b - 123;",
        "}",
        "f({a: {b: 'str'}})"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  obj.a.b = 123;",
        "}",
        "f({a: {b: 'str'}})"));

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  var o;",
        "  (o = obj).x - 1;",
        "  return o.x;",
        "}",
        "f({x: 'str'});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  ({x: obj.foo}).x - 1;",
        "}",
        "f({foo: 'str'});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  ({p: x++}).p = 'str';",
        "}",
        "f('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  ({p: 'str'}).p = x++;",
        "}",
        "f('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, y, z) {",
        "  ({p: (y = x++), q: 'str'}).p = z = y;",
        "  z < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testLooseObjectSubtyping() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}",
        "function f(obj) { obj.prop - 5; }",
        "var /** !Foo */ x = new Foo;",
        "f(x);",
        "var /** !Bar */ y = x;"),
        NewTypeInference.INVALID_ARGUMENT_TYPE,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function f(obj) { obj.prop - 5; }",
        "f(new Foo);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {string} */ this.prop = 'str'; }",
        "function f(obj) { obj.prop - 5; }",
        "f(new Foo);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() { /** @type {number} */ this.prop = 1; }",
        "function g(obj) { var /** string */ s = obj.prop; return obj; }",
        "var /** !Foo */ x = g({ prop: '' });"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    // Infer obj.a as loose, don't warn at the call to f.
    typeCheck(Joiner.on('\n').join(
        "function f(obj) { obj.a.num - 5; }",
        "function g(obj) {",
        "  obj.a.str < 'str';",
        "  f(obj);",
        "}"));

    // A loose object is a subtype of Array even if it has a dotted property
    typeCheck(Joiner.on('\n').join(
        "function f(/** Array<?> */ x) {}",
        "function g(obj) {",
        "  obj.x = 123;",
        "  f(obj);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(g) {",
        "  if (g.randomName) {",
        "  } else {",
        "    return g();",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x.a) {} else {}",
        "}",
        "f({ b: 123 }); "));

    // TODO(dimvar): We could warn about this since x is callable and we're
    // passing a non-function, but we don't catch it for now.
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x.randomName) {",
        "  } else {",
        "    return x();",
        "  }",
        "}",
        "f({ abc: 123 }); "));
  }

  public void testUnionOfRecords() {
    // The previous type inference doesn't warn because it keeps records
    // separate in unions.
    // We treat {x:number}|{y:number} as {x:number=, y:number=}
    typeCheck(Joiner.on('\n').join(
        "/** @param {({x:number}|{y:number})} obj */",
        "function f(obj) {}",
        "f({x: 5, y: 'asdf'});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testUnionOfFunctionAndNumber() {
    typeCheck("var x = function(/** number */ y){};");

    // typeCheck("var x = function(/** number */ y){}; var x = 5",
    //     VariableReferenceCheck.REDECLARED_VARIABLE);

    typeCheck(
        "var x = function(/** number */ y){}; x('str');",
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "var x = true ? function(/** number */ y){} : 5; x('str');",
        TypeCheck.NOT_CALLABLE,
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testAnonymousNominalType() {
    typeCheck(Joiner.on('\n').join(
        "function f() { return {}; }",
        "/** @constructor */",
        "f().Foo = function() {};"),
        GlobalTypeInfo.ANONYMOUS_NOMINAL_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var x = {};",
        "function f() { return x; }",
        "/** @constructor */",
        "f().Foo = function() {};",
        "new (f().Foo)();"),
        GlobalTypeInfo.ANONYMOUS_NOMINAL_TYPE);
  }

  public void testFoo() {
    typeCheck(
        "/** @constructor */ function Foo() {}; Foo();",
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);

    typeCheck(
        "function Foo() {}; new Foo();", NewTypeInference.NOT_A_CONSTRUCTOR);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "function reqFoo(/** Foo */ f) {};",
        "reqFoo(new Foo());"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "/** @constructor */ function Bar() {};",
        "function reqFoo(/** Foo */ f) {};",
        "reqFoo(new Bar());"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "function reqFoo(/** Foo */ f) {};",
        "function g() {",
        "  /** @constructor */ function Foo() {};",
        "  reqFoo(new Foo());",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {number} x */",
        "Foo.prototype.method = function(x) {};",
        "/** @param {!Foo} x */",
        "function f(x) { x.method('asdf'); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testComma() {
    typeCheck(
        "var x; var /** string */ s = (x = 1, x);",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = x;",
        "  y < (123, 'asdf');",
        "}",
        "f(123);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testTypeof() {
    typeCheck("(typeof 'asdf') < 123;", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = x;",
        "  y < (typeof 123);",
        "}",
        "f(123);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x === 'string') {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x != 'function') {",
        "    x - 5;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x == 'string') {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if ('string' === typeof x) {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x === 'number') {",
        "    x < 'asdf';",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x === 'boolean') {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x === 'undefined') {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x === 'function') {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (typeof x === 'function') {",
        "    x();",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x === 'object') {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (!(typeof x == 'number')) {",
        "    x.prop;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (!(typeof x == 'undefined')) {",
        "    x - 5;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (!(typeof x == 'undefined')) {",
        "    var /** undefined */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (typeof x !== 'undefined') {",
        "    var /** undefined */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (typeof x == 'undefined') {} else {",
        "    var /** undefined */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|undefined) */ x) {",
        "  if (typeof x !== 'undefined') {",
        "    x - 5;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  return (typeof 123 == 'number' ||",
        "    typeof 123 == 'string' ||",
        "    typeof 123 == 'boolean' ||",
        "    typeof 123 == 'undefined' ||",
        "    typeof 123 == 'function' ||",
        "    typeof 123 == 'object' ||",
        "    typeof 123 == 'unknown');",
        "}"));

    typeCheck(
        "function f(){ if (typeof 123 == 'numbr') return 321; }",
        TypeValidator.UNKNOWN_TYPEOF_VALUE);

    typeCheck(
        "switch (typeof 123) { case 'foo': }",
        TypeValidator.UNKNOWN_TYPEOF_VALUE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @param {(number|null|Foo)} x */",
        "function f(x) {",
        "  if (!(typeof x === 'object')) {",
        "    var /** number */ n = x;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {(number|function(number):number)} x */",
        "function f(x) {",
        "  if (!(typeof x === 'function')) {",
        "    var /** number */ n = x;",
        "  }",
        "}"));
  }

  public void testAssignWithOp() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = x, z = 0;",
        "  y < (z -= 123);",
        "}",
        "f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var y = x, z = { prop: 0 };",
        "  y < (z.prop -= 123);",
        "}",
        "f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  var z = { prop: 0 };",
        "  x < z.prop;",
        "  z.prop -= 123;",
        "}",
        "f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck("var x = 0; x *= 'asdf';", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var /** string */ x = 'asdf'; x *= 123;",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("var x; x *= 123;", NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testClassConstructor() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {",
        "  /** @type {number} */ this.n = 5;",
        "};",
        "(new Foo()).n - 5;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {",
        "  /** @type {number} */ this.n = 5;",
        "};",
        "(new Foo()).n = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {",
        "  /** @type {number} */ this.n;",
        "};",
        "(new Foo()).n = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f() { (new Foo()).n = 'str'; }",
        "/** @constructor */ function Foo() {",
        "  /** @type {number} */ this.n = 5;",
        "};"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f() { var x = new Foo(); x.n = 'str'; }",
        "/** @constructor */ function Foo() {",
        "  /** @type {number} */ this.n = 5;",
        "};"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f() { var x = new Foo(); return x.n - 5; }",
        "/** @constructor */ function Foo() {",
        "  this.n = 5;",
        "};"));

    typeCheck(Joiner.on('\n').join(
        "function f() { var x = new Foo(); x.s = 'str'; x.s < x.n; }",
        "/** @constructor */ function Foo() {",
        "  /** @type {number} */ this.n = 5;",
        "};"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {",
        "  /** @type {number} */ this.n = 5;",
        "};",
        "function reqFoo(/** Foo */ x) {};",
        "reqFoo({ n : 20 });"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f() { var x = new Foo(); x.n - 5; x.n < 'str'; }",
        "/** @constructor */ function Foo() {",
        "  this.n = 5;",
        "};"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testPropertyDeclarations() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */ this.x = 'abc';",
        "  /** @type {string} */ this.x = 'def';",
        "}"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */ this.x = 5;",
        "  /** @type {number} */ this.x = 7;",
        "}"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  this.x = 5;",
        "  /** @type {number} */ this.x = 7;",
        "}",
        "function g() { (new Foo()).x < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */ this.x = 7;",
        "  this.x = 5;",
        "}",
        "function g() { (new Foo()).x < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */ this.x = 7;",
        "  this.x < 'str';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {?} */ this.x = 1;",
        "  /** @type {?} */ this.x = 1;",
        "}"),
        GlobalTypeInfo.REDECLARED_PROPERTY);
  }

  public void testPrototypePropertyAssignments() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {string} */ Foo.prototype.x = 'str';",
        "function g() { (new Foo()).x - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.x = 'str';",
        "function g() { var f = new Foo(); f.x - 5; f.x < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {function(string)} s */",
        "Foo.prototype.bar = function(s) {};",
        "function g() { (new Foo()).bar(5); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "Foo.prototype.bar = function(s) {",
        "  /** @type {string} */ this.x = 'str';",
        "};",
        "(new Foo()).x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "(function() { Foo.prototype.prop = 123; })();"),
        GlobalTypeInfo.CTOR_IN_DIFFERENT_SCOPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function F() {}",
        "F.prototype.bar = function() {};",
        "F.prototype.bar = function() {};"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function F() {}",
        "/** @return {void} */ F.prototype.bar = function() {};",
        "F.prototype.bar = function() {};"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function C(){}",
        "C.prototype.foo = {};",
        "C.prototype.method = function() { this.foo.bar = 123; }"));

    // TODO(dimvar): I think we can fix the next one with better deferred checks
    // for prototype methods. Look into it.
    // typeCheck(Joiner.on('\n').join(
    //     "/** @constructor */ function Foo() {};",
    //     "Foo.prototype.bar = function(s) { s < 'asdf'; };",
    //     "function g() { (new Foo()).bar(5); }"),
    //     NewTypeInference.INVALID_ARGUMENT_TYPE);

    // TODO(blickly): Add fancier JSDoc annotation finding to jstypecreator
    // typeCheck(Joiner.on('\n').join(
    //     "/** @constructor */ function Foo() {};",
    //     "/** @param {string} s */ Foo.prototype.bar = function(s) {};",
    //     "function g() { (new Foo()).bar(5); }"),
    //     NewTypeInference.INVALID_ARGUMENT_TYPE);
    // typeCheck(Joiner.on('\n').join(
    //     "/** @constructor */ function Foo() {};",
    //     "Foo.prototype.bar = function(/** string */ s) {};",
    //     "function g() { (new Foo()).bar(5); }"),
    //     NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f() {}",
        "function g() { f.prototype.prop = 123; }"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {!Function} f */",
        "function foo(f) { f.prototype.bar = function(x) {}; }"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype.method = function() {};",
        "/** @type {number} */",
        "Foo.prototype.method.pnum = 123;",
        "var /** number */ n = Foo.prototype['method.pnum'];"),
        TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testPrototypeAssignment() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype = { a: 1, b: 2 };",
        "var x = (new Foo).a;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype = { a: 1, b: 2 - 'asdf' };",
        "var x = (new Foo).a;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype = { a: 1, /** @const */ b: 2 };",
        "(new Foo).b = 3;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype = { method: function(/** number */ x) {} };",
        "(new Foo).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype = { method: function(/** number */ x) {} };",
        "/** @constructor @extends {Foo} */",
        "function Bar() {}",
        "(new Bar).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testAssignmentsToPrototype() {
    // TODO(dimvar): the 1st should pass, the 2nd we may stop catching
    // if we decide to not check these assignments at all.

    // typeCheck(Joiner.on('\n').join(
    //     "/** @constructor */",
    //     "function Foo() {}",
    //     "/** @constructor @extends {Foo} */",
    //     "function Bar() {}",
    //     "Bar.prototype = new Foo;",
    //     "Bar.prototype.method1 = function() {};"));

    // typeCheck(Joiner.on('\n').join(
    //     "/**",
    //     " * @constructor",
    //     " * @struct",
    //     " */",
    //     "function Bar() {}",
    //     "Bar.prototype = {};"),
    //     TypeCheck.CONFLICTING_SHAPE_TYPE);
  }

  public void testConflictingPropertyDefinitions() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() { this.x = 'str1'; };",
        "/** @type {string} */ Foo.prototype.x = 'str2';",
        "(new Foo).x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {string} */ Foo.prototype.x = 'str1';",
        "Foo.prototype.x = 'str2';",
        "(new Foo).x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.x = 'str2';",
        "/** @type {string} */ Foo.prototype.x = 'str1';",
        "(new Foo).x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {string} */ this.x = 'str1'; };",
        "Foo.prototype.x = 'str2';",
        "(new Foo).x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() { this.x = 5; };",
        "/** @type {string} */ Foo.prototype.x = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {string} */ this.x = 'str1'; };",
        "Foo.prototype.x = 5;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {string} */ this.x = 'str'; };",
        "/** @type {number} */ Foo.prototype.x = 'str';"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {number} */ Foo.prototype.x = 1;",
        "/** @type {number} */ Foo.prototype.x = 2;"),
        GlobalTypeInfo.REDECLARED_PROPERTY);
  }

  public void testPrototypeAliasing() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.x = 'str';",
        "var fp = Foo.prototype;",
        "fp.x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testInstanceof() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function takesFoos(/** Foo */ afoo) {}",
        "function f(/** (number|Foo) */ x) {",
        "  takesFoos(x);",
        "  if (x instanceof Foo) { takesFoos(x); }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "({} instanceof function(){});", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "(123 instanceof Foo);"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function takesFoos(/** Foo */ afoo) {}",
        "function f(/** boolean */ cond, /** (number|Foo) */ x) {",
        "  if (x instanceof (cond || Foo)) { takesFoos(x); }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function f(/** (number|!Foo) */ x) {",
        "  if (x instanceof Foo) {} else { x - 5; }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function f(/** (number|!Foo) */ x) {",
        "  if (!(x instanceof Foo)) { x - 5; }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "function takesFoos(/** Foo */ afoo) {}",
        "function f(/** Foo */ x) {",
        "  if (x instanceof Bar) {} else { takesFoos(x); }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "function takesFoos(/** Foo */ afoo) {}",
        "/** @param {*} x */ function f(x) {",
        "  takesFoos(x);",
        "  if (x instanceof Foo) { takesFoos(x); }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "var x = new Foo();",
        "x.bar = 'asdf';",
        "if (x instanceof Foo) { x.bar - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    // typeCheck(
    //     "function f(x) { if (x instanceof UndefinedClass) {} }",
    //     VarCheck.UNDEFINED_VAR_ERROR);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() { this.prop = 123; }",
        "function f(x) { x = 123; if (x instanceof Foo) { x.prop; } }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "/** @param {(number|!Bar)} x */",
        "function f(x) {",
        "  if (!(x instanceof Foo)) {",
        "    var /** number */ n = x;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @enum {!Foo} */",
        "var E = { ONE: new Foo };",
        "/** @param {(number|E)} x */",
        "function f(x) {",
        "  if (!(x instanceof Foo)) {",
        "    var /** number */ n = x;",
        "  }",
        "}"));
  }

  public void testFunctionsExtendFunction() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x instanceof Function) { x(); }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x instanceof Function) { x(1); x('str') }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** (null|function()) */ x) {",
        "  if (x instanceof Function) { x(); }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** (null|function()) */ x) {",
        "  if (x instanceof Function) {} else { x(); }",
        "}"),
        TypeCheck.NOT_CALLABLE);

    typeCheck("(function(){}).call(null);");

    typeCheck(Joiner.on('\n').join(
        "function greet(name) {}",
        "greet.call(null, 'bob');",
        "greet.apply(null, ['bob']);"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "Foo.prototype.greet = function(name){};",
        "Foo.prototype.greet.call(new Foo, 'bob');"));

    typeCheck(Joiner.on('\n').join(
        "Function.prototype.method = function(/** string */ x){};",
        "(function(){}).method(5);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(value) {",
        "  if (value instanceof Function) {} else if (value instanceof Object) {",
        "    return value.displayName || value.name || '';",
        "  }",
        "};"));
  }

  public void testObjectsAreNotClassy() {
    typeCheck(Joiner.on('\n').join(
        "function g(obj) {",
        "  if (!(obj instanceof Object)) { throw -1; }",
        "  return obj.x - 5;",
        "}",
        "g(new Object);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testFunctionWithProps() {
    typeCheck(Joiner.on('\n').join(
        "function f() {}",
        "f.x = 'asdf';",
        "f.x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testConstructorProperties() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {number} */ Foo.n = 1",
        "/** @type {number} */ Foo.n = 1"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "function g() { Foo.bar - 5; }",
        "/** @constructor */ function Foo() {}",
        "Foo.bar = 42;"));

    typeCheck(Joiner.on('\n').join(
        "function g() { Foo.bar - 5; }",
        "/** @constructor */ function Foo() {}",
        "/** @type {string} */ Foo.bar = 'str';"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g() { return (new Foo).bar; }",
        "/** @constructor */ function Foo() {}",
        "/** @type {string} */ Foo.bar = 'str';"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {string} */ Foo.prop = 'asdf';",
        "var x = Foo;",
        "x.prop - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g() { Foo.prototype.baz = (new Foo).bar + Foo.bar; }",
        "/** @constructor */ function Foo() {}",
        "/** @type {number} */ Foo.prototype.bar = 5",
        "/** @type {string} */ Foo.bar = 'str';"),
        GlobalTypeInfo.CTOR_IN_DIFFERENT_SCOPE);

    // TODO(dimvar): warn about redeclared property
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {number} */ Foo.n = 1;",
        "Foo.n = 1;"));

    // TODO(dimvar): warn about redeclared property
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {number} */ Foo.n;",
        "Foo.n = '';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testTypeTighteningHeuristic() {
    typeCheck(
        "/** @param {*} x */ function f(x) { var /** ? */ y = x; x - 5; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** ? */ x) {",
        "  if (!(typeof x == 'number')) {",
        "    x < 'asdf';",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** { prop: ? } */ x) {",
        "  var /** (number|string) */ y = x.prop;",
        "  x.prop < 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|string) */ x, /** (number|string) */ y) {",
        "  var z;",
        "  if (1 < 2) {",
        "    z = x;",
        "  } else {",
        "    z = y;",
        "  }",
        "  z - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testDeclaredPropertyIndirectly() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** { n: number } */ obj) {",
        "  var o2 = obj;",
        "  o2.n = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testNonRequiredArguments() {
    typeCheck(Joiner.on('\n').join(
        "function f(f1, /** function(string=) */ f2, cond) {",
        "  var y;",
        "  if (cond) {",
        "    f1();",
        "    y = f1;",
        "  } else {",
        "    y = f2;",
        "  }",
        "  return y;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(number=)} fnum */",
        "function f(fnum) {",
        "  fnum(); fnum('asdf');",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(...number)} fnum */",
        "function f(fnum) {",
        "  fnum(); fnum(1, 2, 3, 'asdf');",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(number=, number)} g */",
        "function f(g) {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @param {number=} x */",
        "function f(x) {}",
        "f(); f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {number=} x */",
        "function f(x) {}",
        "f(1, 2);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(
        "/** @type {function()} */ function f(x) {}",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "/** @type {function(number)} */ function f() {}",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "/** @type {function(number)} */ function f(/** number */ x) {}",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {number=} x",
        " * @param {number} y",
        " */",
        "function f(x, y) {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @type {function(number=)} */ function f(x) {}",
        "f(); f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "/** @type {function(number=, number)} */ function f(x, y) {}",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "function /** number */ f() { return 'asdf'; }",
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    typeCheck(
        "/** @return {number} */ function /** number */ f() { return 1; }",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @type {function(): number} */",
        "function /** number */ f() { return 1; }"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @type {function(...number)} */ function f() {}",
        "f(); f(1, 2, 3); f(1, 2, 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {...number} var_args */ function f(var_args) {}",
        "f(); f(1, 2, 3); f(1, 2, 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(
        "/** @type {function(...number)} */ function f(x) {}",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {...number} var_args",
        " * @param {number=} x",
        " */",
        "function f(var_args, x) {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @type {function(number=, ...number)} */",
        "function f(x) {}",
        "f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(number=) */ fnum,",
        "  /** function(string=) */ fstr, cond) {",
        "  var y;",
        "  if (cond) {",
        "    y = fnum;",
        "  } else {",
        "    y = fstr;",
        "  }",
        "  y();",
        "  y(123);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(...number) */ fnum,",
        "  /** function(...string) */ fstr, cond) {",
        "  var y;",
        "  if (cond) {",
        "    y = fnum;",
        "  } else {",
        "    y = fstr;",
        "  }",
        "  y();",
        "  y(123);",
        "}"),
        TypeCheck.NOT_CALLABLE,
        TypeCheck.NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "function f(",
        "  /** function() */ f1, /** function(string=) */ f2, cond) {",
        "  var y;",
        "  if (cond) {",
        "    y = f1;",
        "  } else {",
        "    y = f2;",
        "  }",
        "  y(123);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(string): *} x */ function g(x) {}",
        "/** @param {function(...number): string} x */ function f(x) {",
        "  g(x);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {number=} x",
        " * @param {number=} y",
        " */",
        "function f(x, y) {}",
        "f(undefined, 123);",
        "f('str')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(...) */ fun) {}",
        "f(function() {});"));

    // The restarg formal doesn't have to be called var_args.
    // It shouldn't be used in the body of the function.
    // typeCheck(
    //     "/** @param {...number} x */ function f(x) { x - 5; }",
    //     VarCheck.UNDEFINED_VAR_ERROR);

    typeCheck(
        "/** @param {number=} x */ function f(x) { x - 5; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "/** @param {number=} x */ function f(x) { if (x) { x - 5; } }");

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(...number) */ x) {}",
        "f(function() {});"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** function() */ x) {}",
        "f(/** @type {function(...number)} */ (function(nums) {}));"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(string=) */ x) {}",
        "f(/** @type {function(...number)} */ (function(nums) {}));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** function(...number) */ x) {}",
        "f(/** @type {function(string=)} */ (function(x) {}));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {number} opt_num */ function f(opt_num) {}",
        "f();"));

    typeCheck(
        "function f(opt_num, x) {}", RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck("function f(var_args) {} f(1, 2, 3);");

    typeCheck(
        "function f(var_args, x) {}", RhinoErrorReporter.BAD_JSDOC_ANNOTATION);
  }

  public void testInferredOptionalFormals() {
    typeCheck("function f(x) {} f();");

    typeCheck("function f(/** number */ x, y) { x-5; } f(123);");

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x !== undefined) {",
        "    return x-5;",
        "  } else {",
        "    return 0;",
        "  }",
        "}",
        "f() - 1;",
        "f('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {function(number=)} */",
        "function f() {",
        "  return function(x) {};",
        "}",
        "f()();",
        "f()('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testSimpleClassInheritance() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {}",
        "/** @constructor @extends{Parent} */",
        "function Child() {}",
        "Child.prototype = new Parent();"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {",
        "  /** @type {string} */ this.prop = 'asdf';",
        "}",
        "/** @constructor @extends{Parent} */",
        "function Child() {}",
        "Child.prototype = new Parent();",
        "(new Child()).prop - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {",
        "  /** @type {string} */ this.prop = 'asdf';",
        "}",
        "/** @constructor @extends{Parent} */",
        "function Child() {}",
        "Child.prototype = new Parent();",
        "(new Child()).prop = 5;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {}",
        "/** @type {string} */ Parent.prototype.prop = 'asdf';",
        "/** @constructor @extends{Parent} */",
        "function Child() {}",
        "Child.prototype = new Parent();",
        "(new Child()).prop - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {}",
        "/** @type {string} */ Parent.prototype.prop = 'asdf';",
        "/** @constructor @extends{Parent} */",
        "function Child() {",
        "  /** @type {number} */ this.prop = 5;",
        "}",
        "Child.prototype = new Parent();"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {}",
        "/** @type {string} */ Parent.prototype.prop = 'asdf';",
        "/** @constructor @extends{Parent} */",
        "function Child() {}",
        "Child.prototype = new Parent();",
        "/** @type {number} */ Child.prototype.prop = 5;"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {}",
        "/** @extends {Parent} */ function Child() {}"),
        JSTypeCreatorFromJSDoc.EXTENDS_NOT_ON_CTOR_OR_INTERF);

    typeCheck(
        "/** @constructor @extends{number} */ function Foo() {}",
        JSTypeCreatorFromJSDoc.EXTENDS_NON_OBJECT);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @implements {string}",
        " */",
        "function Foo() {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @extends {number}",
        " */",
        "function Foo() {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Foo() {}",
        "/** @implements {Foo} */ function bar() {}"),
        JSTypeCreatorFromJSDoc.IMPLEMENTS_WITHOUT_CONSTRUCTOR);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype.method = function(x) { x - 1; };",
        "/** @constructor @extends {Foo} */",
        "function Bar() {}",
        "Bar.prototype.method = function(x, y) { x - y; };",
        "Bar.prototype.method2 = function(x, y) {};",
        "Bar.prototype.method = Bar.prototype.method2;"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);
  }

  public void testInheritingTheParentClassInterfaces() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/** @type {number} */",
        "High.prototype.p;",
        "/** @constructor @implements {High} */",
        "function Mid() {}",
        "Mid.prototype.p = 123;",
        // Low has p from Mid, no warning here
        "/** @constructor @extends {Mid} */",
        "function Low() {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/** @constructor @implements {High} */",
        "function Mid() {}",
        "/** @constructor @extends {Mid} */",
        "function Low() {}",
        "var /** !High */ x = new Low();"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function High() {}",
        "/**",
        " * @constructor",
        " * @template T",
        " * @implements {High<T>}",
        " */",
        "function Mid() {}",
        "/** @constructor @extends {Mid<number>} */",
        "function Low() {}",
        "var /** !High<string> */ x = new Low;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testInheritanceSubtyping() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Parent() {}",
        "/** @constructor @extends{Parent} */ function Child() {}",
        "(function(/** Parent */ x) {})(new Child);"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Parent() {}",
        "/** @constructor @extends{Parent} */ function Child() {}",
        "(function(/** Child */ x) {})(new Parent);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Parent() {}",
        "/** @constructor @extends{Parent} */ function Child() {}",
        "/** @constructor */",
        "function Foo() { /** @type {Parent} */ this.x = new Child(); }",
        "/** @type {Child} */ Foo.prototype.y = new Parent();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/** @constructor @implements {High} */",
        "function Low() {}",
        "var /** !High */ x = new Low"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/** @interface @extends {High}*/",
        "function Low() {}",
        "function f(/** !High */ h, /** !Low */ l) { h = l; }"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/** @interface @extends {High}*/",
        "function Low() {}",
        "/** @constructor @implements {Low} */",
        "function Foo() {}",
        "var /** !High */ x = new Foo;"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Foo() {}",
        "/** @interface */",
        "function High() {}",
        "/** @interface @extends {High} */",
        "function Med() {}",
        "/**",
        " * @interface",
        " * @extends {Med}",
        " * @extends {Foo}",
        " */",
        "function Low() {}",
        "function f(/** !High */ x, /** !Low */ y) { x = y }"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function Foo() {}",
        "function f(/** !Foo<number> */ x, /** !Foo<string> */ y) { x = y; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function Foo() {}",
        "/**",
        " * @constructor",
        " * @implements {Foo<number>}",
        " */",
        "function Bar() {}",
        "function f(/** !Foo<string> */ x, /** Bar */ y) { x = y; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function Foo() {}",
        "/**",
        " * @constructor",
        " * @template T",
        " * @implements {Foo<T>}",
        " */",
        "function Bar() {}",
        "function f(/** !Foo<string> */ x, /** !Bar<number> */ y) { x = y; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function Foo() {}",
        "/**",
        " * @constructor",
        " * @template T",
        " * @implements {Foo<T>}",
        " */",
        "function Bar() {}",
        "function f(/** !Foo<string> */ x, /** !Bar<string> */ y) {",
        "  x = y;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function Foo() {}",
        "/**",
        " * @constructor",
        " * @template T",
        " * @implements {Foo<T>}",
        " */",
        "function Bar() {}",
        "/**",
        " * @template T",
        " * @param {!Foo<T>} x",
        " * @param {!Bar<number>} y",
        " */",
        "function f(x, y) { x = y; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    // When getting a method signature from the parent, the receiver type is
    // still the child's type.
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/** @param {number} x */",
        "High.prototype.method = function (x) {};",
        "/** @constructor @implements {High} */",
        "function Low() {}",
        "Low.prototype.method = function (x) {",
        "  var /** !Low */ y = this;",
        "};"));
  }

  public void testInheritanceImplicitObjectSubtyping() {
    String objectExterns = DEFAULT_EXTERNS
        + "/** @return {string} */ Object.prototype.toString = function() {};";

    typeCheckCustomExterns(
        objectExterns,
        Joiner.on('\n').join(
            "/** @constructor */ function Foo() {}",
            "/** @override */ Foo.prototype.toString = function(){ return ''; };"));

    typeCheckCustomExterns(
        objectExterns,
        Joiner.on('\n').join(
            "/** @constructor */ function Foo() {}",
            "/** @override */ Foo.prototype.toString = function(){ return 5; };"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);
  }

  public void testRecordtypeSubtyping() {
    // TODO(dimvar): fix
    // typeCheck(Joiner.on('\n').join(
    //     "/** @interface */ function I() {}",
    //     "/** @type {number} */ I.prototype.prop;",
    //     "function f(/** !I */ x) {",
    //     "  var /** { prop: number} */ y = x;",
    //     "}"));
  }

  public void testWarnAboutOverridesNotVisibleDuringGlobalTypeInfo() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @extends {Parent} */ function Child() {}",
        "/** @type {string} */ Child.prototype.y = 'str';",
        "/** @constructor */ function Grandparent() {}",
        "/** @type {number} */ Grandparent.prototype.y = 9;",
        "/** @constructor @extends {Grandparent} */ function Parent() {}"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);
  }

  public void testInvalidMethodPropertyOverride() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/** @type {number} */ Parent.prototype.y;",
        "/** @constructor @implements {Parent} */ function Child() {}",
        "/** @param {string} x */ Child.prototype.y = function(x) {};"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/** @param {string} x */ Parent.prototype.y = function(x) {};",
        "/** @constructor @implements {Parent} */ function Child() {}",
        "/** @type {number} */ Child.prototype.y = 9;"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Parent() {}",
        "/** @type {number} */ Parent.prototype.y = 9;",
        "/** @constructor @extends {Parent} */ function Child() {}",
        "/** @param {string} x */ Child.prototype.y = function(x) {};"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Parent() {}",
        "/** @param {string} x */ Parent.prototype.y = function(x) {};",
        "/** @constructor @extends {Parent} */ function Child() {}",
        "/** @type {number} */ Child.prototype.y = 9;"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    // TODO(dimvar): fix
    // typeCheck(Joiner.on('\n').join(
    //     "/** @constructor */",
    //     "function Foo() {}",
    //     "Foo.prototype.f = function(/** number */ x, /** number */ y) {};",
    //     "/** @constructor @extends {Foo} */",
    //     "function Bar() {}",
    //     "/** @override */",
    //     "Bar.prototype.f = function(x) {};"),
    //     GlobalTypeInfo.INVALID_PROP_OVERRIDE);
  }

  public void testMultipleObjects() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "/** @param {(Foo|Bar)} x */ function reqFooBar(x) {}",
        "function f(cond) {",
        "  reqFooBar(cond ? new Foo : new Bar);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "/** @param {Foo} x */ function reqFoo(x) {}",
        "function f(cond) {",
        "  reqFoo(cond ? new Foo : new Bar);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "/** @param {(Foo|Bar)} x */ function g(x) {",
        "  if (x instanceof Foo) {",
        "    var /** Foo */ y = x;",
        "  } else {",
        "    var /** Bar */ z = x;",
        "  }",
        "  var /** Foo */ w = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {string} */ this.s = 'str'; }",
        "/** @param {(!Foo|{n:number, s:string})} x */ function g(x) {",
        "  if (x instanceof Foo) {",
        "  } else {",
        "    x.s - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {number} */ Foo.prototype.n = 5;",
        "/** @param {{n : number}} x */ function reqRecord(x) {}",
        "function f() {",
        "  reqRecord(new Foo);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @type {number} */ Foo.prototype.n = 5;",
        "/** @param {{n : string}} x */ function reqRecord(x) {}",
        "function f() {",
        "  reqRecord(new Foo);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @param {{n : number}|!Foo} x */",
        "function f(x) {",
        "  x.n - 5;",
        "}"),
        NewTypeInference.POSSIBLY_INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @param {{n : number}|!Foo} x */",
        "function f(x) {",
        "  x.abc - 5;",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "/** @param {!Bar|!Foo} x */",
        "function f(x) {",
        "  x.abc = 'str';",
        "  if (x instanceof Foo) {",
        "    x.abc - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testMultipleFunctionsInUnion() {
    typeCheck(Joiner.on('\n').join(
        "/** @param {function():string | function():number} x",
        "  * @return {string|number} */",
        "function f(x) {",
        "  return x();",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(string)|function(number)} x",
        "  * @param {string|number} y */",
        "function f(x, y) {",
        "  x(y);",
        "}"),
        JSTypeCreatorFromJSDoc.UNION_IS_UNINHABITABLE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template S, T",
        " * @param {function(S):void | function(T):void} fun",
        " */",
        "function f(fun) {}"),
        JSTypeCreatorFromJSDoc.UNION_IS_UNINHABITABLE);
  }

  public void testPrototypeOnNonCtorFunction() {
    typeCheck("function Foo() {}; Foo.prototype.y = 5;");
  }

  public void testInvalidTypeReference() {
    typeCheck(
        "/** @type {gibberish} */ var x;",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(
        "/** @param {gibberish} x */ function f(x){};",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(
        "function f(/** gibberish */ x){};",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(Joiner.on('\n').join(
        "/** @returns {gibberish} */",
        "function f(x) { return x; };"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(
        "/** @interface @extends {gibberish} */ function Foo(){};",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(
        "/** @constructor @implements {gibberish} */ function Foo(){};",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(
        "/** @constructor @extends {gibberish} */ function Foo() {};",
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);
  }

  public void testCircularDependencies() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @extends {Bar}*/ function Foo() {}",
        "/** @constructor */ function Bar() {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {Foo} x */ function f(x) {}",
        "/** @constructor */ function Foo() {}"));

    typeCheck(Joiner.on('\n').join(
        "f(new Bar)",
        "/** @param {Foo} x */ function f(x) {}",
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @param {Foo} x */ function Bar(x) {}",
        "/** @constructor @param {Bar} x */ function Foo(x) {}",
        "new Bar(new Foo(null));"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @param {Foo} x */ function Bar(x) {}",
        "/** @constructor @param {Bar} x */ function Foo(x) {}",
        "new Bar(new Foo(undefined));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @extends {Bar} */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}"),
        JSTypeCreatorFromJSDoc.INHERITANCE_CYCLE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface @extends {Bar} */ function Foo() {}",
        "/** @interface @extends {Foo} */ function Bar() {}"),
        JSTypeCreatorFromJSDoc.INHERITANCE_CYCLE);

    typeCheck(
        "/** @constructor @extends {Foo} */ function Foo() {}",
        JSTypeCreatorFromJSDoc.INHERITANCE_CYCLE);
  }

  public void testInvalidInitOfInterfaceProps() throws Exception {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function T() {};",
        "T.prototype.x = function() { return 'foo'; }"),
        TypeCheck.INTERFACE_METHOD_NOT_EMPTY);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I() {};",
        "/** @type {number} */",
        "I.prototype.n = 123;"),
        GlobalTypeInfo.INVALID_INTERFACE_PROP_INITIALIZER);
  }

  public void testInterfaceSingleInheritance() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I() {}",
        "/** @type {string} */ I.prototype.prop;",
        "/** @constructor @implements{I} */ function C() {}"),
        TypeValidator.INTERFACE_METHOD_NOT_IMPLEMENTED);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I() {}",
        "/** @param {number} x */",
        "I.prototype.method = function(x) {};",
        "/** @constructor @implements{I} */ function C() {}"),
        TypeValidator.INTERFACE_METHOD_NOT_IMPLEMENTED);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function IParent() {}",
        "/** @type {number} */ IParent.prototype.prop;",
        "/** @interface @extends{IParent} */ function IChild() {}",
        "/** @constructor @implements{IChild} */",
        "function C() { this.prop = 5; }",
        "(new C).prop < 'adsf';"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function IParent() {}",
        "/** @type {number} */ IParent.prototype.prop;",
        "/** @interface @extends{IParent} */ function IChild() {}",
        "/** @constructor @implements{IChild} */",
        "function C() { this.prop = 'str'; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() { /** @type {number} */ this.prop = 123; }",
        "/** @constructor @extends {Parent} */ function Child() {}",
        "(new Child).prop = 321;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() { /** @type {number} */ this.prop = 123; }",
        "/** @constructor @extends {Parent} */ function Child() {}",
        "(new Child).prop = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I() {}",
        "/** @param {number} x */",
        "I.prototype.method = function(x, y) {};",
        "/** @constructor @implements{I} */ function C() {}",
        "/** @param {string} y */",
        "C.prototype.method = function(x, y) {};",
        "(new C).method(5, 6);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I() {}",
        "/** @param {number} x */",
        "I.prototype.method = function(x, y) {};",
        "/** @constructor @implements{I} */ function C() {}",
        "/** @param {string} y */",
        "C.prototype.method = function(x, y) {};",
        "(new C).method('asdf', 'fgr');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I() {}",
        "/** @param {number} x */",
        "I.prototype.method = function(x) {};",
        "/** @constructor @implements{I} */ function C() {}",
        "C.prototype.method = function(x) {};",
        "(new C).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I1() {}",
        "/** @param {number} x */ I1.prototype.method = function(x, y) {};",
        "/** @interface */ function I2() {}",
        "/** @param {string} y */ I2.prototype.method = function(x, y) {};",
        "/** @constructor @implements{I1} @implements{I2} */ function C(){}",
        "C.prototype.method = function(x, y) {};",
        "(new C).method('asdf', 'fgr');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I1() {}",
        "/** @param {number} x */ I1.prototype.method = function(x, y) {};",
        "/** @interface */ function I2() {}",
        "/** @param {string} y */ I2.prototype.method = function(x, y) {};",
        "/** @constructor @implements{I1} @implements{I2} */ function C(){}",
        "C.prototype.method = function(x, y) {};",
        "(new C).method(1, 2);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I1() {}",
        "/** @param {number} x */ I1.prototype.method = function(x) {};",
        "/** @interface */ function I2() {}",
        "/** @param {string} x */ I2.prototype.method = function(x) {};",
        "/** @constructor @implements{I1} @implements{I2} */ function C(){}",
        // Type of C.method is @param {(string|number)}
        "C.prototype.method = function(x) {};"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I1() {}",
        "/** @param {number} x */ I1.prototype.method = function(x) {};",
        "/** @interface */ function I2() {}",
        "/** @param {string} x */ I2.prototype.method = function(x) {};",
        "/** @constructor @implements{I1} @implements{I2} */ function C(){}",
        // Type of C.method is @param {(string|number)}
        "C.prototype.method = function(x) {};",
        "(new C).method(true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I() {}",
        "/** @param {number} x */ I.prototype.method = function(x) {};",
        "/** @constructor */ function S() {}",
        "/** @param {string} x */ S.prototype.method = function(x) {};",
        "/** @constructor @implements{I} @extends{S} */ function C(){}",
        // Type of C.method is @param {(string|number)}
        "C.prototype.method = function(x) {};",
        "(new C).method(true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testInterfaceMultipleInheritanceNoCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function I1() {}",
        "I1.prototype.method = function(x) {};",
        "/** @interface */",
        "function I2() {}",
        "I2.prototype.method = function(x) {};",
        "/**",
        " * @interface",
        " * @extends {I1}",
        " * @extends {I2}",
        " */",
        "function I3() {}",
        "/** @constructor @implements {I3} */",
        "function Foo() {}",
        "Foo.prototype.method = function(x) {};"));
  }

  public void testInterfaceArgument() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function I() {}",
        "/** @param {number} x */",
        "I.prototype.method = function(x) {};",
        "/** @param {!I} x */",
        "function foo(x) { x.method('asdf'); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function IParent() {}",
        "/** @param {number} x */",
        "IParent.prototype.method = function(x) {};",
        "/** @interface @extends {IParent} */",
        "function IChild() {}",
        "/** @param {!IChild} x */",
        "function foo(x) { x.method('asdf'); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testExtendedInterfacePropertiesCompatibility() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */function Int0() {};",
        "/** @interface */function Int1() {};",
        "/** @type {number} */",
        "Int0.prototype.foo;",
        "/** @type {string} */",
        "Int1.prototype.foo;",
        "/** @interface \n @extends {Int0} \n @extends {Int1} */",
        "function Int2() {};"),
        TypeCheck.INCOMPATIBLE_EXTENDED_PROPERTY_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Parent1() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {number}",
        " */",
        "Parent1.prototype.method = function(x) {};",
        "/** @interface */",
        "function Parent2() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {string}",
        " */",
        "Parent2.prototype.method = function(x) {};",
        "/** @interface @extends {Parent1} @extends {Parent2} */",
        "function Child() {}"),
        TypeCheck.INCOMPATIBLE_EXTENDED_PROPERTY_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "/** @interface */",
        "function Parent1() {}",
        "/** @type {!Foo} */",
        "Parent1.prototype.obj;",
        "/** @interface */",
        "function Parent2() {}",
        "/** @type {!Bar} */",
        "Parent2.prototype.obj;",
        "/** @interface @extends {Parent1} @extends {Parent2} */",
        "function Child() {}"),
        TypeCheck.INCOMPATIBLE_EXTENDED_PROPERTY_TYPE);
  }

  public void testTwoLevelExtendedInterface() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */function Int0() {};",
        "/** @type {function()} */",
        "Int0.prototype.foo;",
        "/** @interface @extends {Int0} */function Int1() {};",
        "/** @constructor \n @implements {Int1} */",
        "function Ctor() {};"),
        TypeValidator.INTERFACE_METHOD_NOT_IMPLEMENTED);
  }

  public void testConstructorExtensions() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function I() {}",
        "/** @param {number} x */",
        "I.prototype.method = function(x) {};",
        "/** @constructor @extends{I} */ function C() {}",
        "C.prototype.method = function(x) {};",
        "(new C).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function I() {}",
        "/** @param {number} x */",
        "I.prototype.method = function(x, y) {};",
        "/** @constructor @extends{I} */ function C() {}",
        "/** @param {string} y */",
        "C.prototype.method = function(x, y) {};",
        "(new C).method('asdf', 'fgr');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testInterfaceAndConstructorInvalidConstructions() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @extends {Bar} */",
        "function Foo() {}",
        "/** @interface */",
        "function Bar() {}"),
        TypeCheck.CONFLICTING_EXTENDED_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @implements {Bar} */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @interface @implements {Foo} */",
        "function Bar() {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @interface @extends {Foo} */",
        "function Bar() {}"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);
  }

  public void testNot() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor */",
        "function Bar() { /** @type {string} */ this.prop = 'asdf'; }",
        "function f(/** (!Foo|!Bar) */ obj) {",
        "  if (!(obj instanceof Foo)) {",
        "    obj.prop - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(cond) {",
        "  var x = cond ? null : 123;",
        "  if (!(x === null)) { x - 5; }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){ this.prop = 123; }",
        "function f(/** Foo */ obj) {",
        "  if (!obj) { obj.prop; }",
        "}"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);
  }

  public void testNullability() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {Foo} x */",
        "function f(x) {}",
        "f(new Foo);"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){ this.prop = 123; }",
        "function f(/** Foo */ obj) { obj.prop; }"),
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function I() {}",
        "I.prototype.method = function() {};",
        "/** @param {I} x */",
        "function foo(x) { x.method(); }"),
        NewTypeInference.NULLABLE_DEREFERENCE);
  }

  public void testGetElem() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function C(){ /** @type {number} */ this.prop = 1; }",
        "(new C)['prop'] < 'asdf';"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  x < y;",
        "  ({})[y - 5];",
        "}",
        "f('asdf', 123);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // We don't see the warning here b/c the formal param x is assigned to a
    // string, and we use x's type at the end of the function to create the
    // summary.
    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  x < y;",
        "  ({})[y - 5];",
        "  x = 'asdf';",
        "}",
        "f('asdf', 123);"));

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  ({})[y - 5];",
        "  x < y;",
        "}",
        "f('asdf', 123);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x['prop'] = 'str';",
        "  return x['prop'] - 5;",
        "}",
        "f({});"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("function f(/** ? */ o) { return o[0].prop; }");

    // TODO(blickly): The fact that this has no warnings is somewhat unpleasant.
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x['prop'] = 7;",
        "  var p = 'prop';",
        "  x[p] = 'str';",
        "  return x['prop'] - 5;",
        "}",
        "f({});"));
  }

  public void testNamespaces() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @constructor */ ns.C = function() {};",
        "ns.C();"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @param {number} x */ ns.f = function(x) {};",
        "ns.f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @constructor */ ns.C = function(){}",
        "ns.C.prototype.method = function(/** string */ x){};",
        "(new ns.C).method(5);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @const */ ns.ns2 = {};",
        "/** @constructor */ ns.ns2.C = function() {};",
        "ns.ns2.C();"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @const */ ns.ns2 = {};",
        "/** @constructor */ ns.ns2.C = function() {};",
        "ns.ns2.C.prototype.method = function(/** string */ x){};",
        "(new ns.ns2.C).method(11);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function C1(){}",
        "/** @constructor */ C1.C2 = function(){}",
        "C1.C2.prototype.method = function(/** string */ x){};",
        "(new C1.C2).method(1);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function C1(){};",
        "/** @constructor */ C1.prototype.C2 = function(){};",
        "(new C1).C2();"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @type {number} */ ns.N = 5;",
        "ns.N();"),
        TypeCheck.NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @type {number} */ ns.foo = 123;",
        "/** @type {string} */ ns.foo = '';"),
        GlobalTypeInfo.REDECLARED_PROPERTY,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @type {number} */ ns.foo;",
        "/** @type {string} */ ns.foo;"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    // We warn for duplicate declarations even if they are the same type.
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @type {number} */ ns.foo;",
        "/** @type {number} */ ns.foo;"),
        GlobalTypeInfo.REDECLARED_PROPERTY);

    // Without the @const, we don't consider it a namespace and don't warn.
    typeCheck(Joiner.on('\n').join(
        "var ns = {};",
        "/** @type {number} */ ns.foo = 123;",
        "/** @type {string} */ ns.foo = '';"));

    // TODO(dimvar): warn about redeclared property
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "ns.x = 5;",
        "/** @type {string} */",
        "ns.x = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "ns.prop = 1;",
        "function f() { var /** string */ s = ns.prop; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testNamespacesInExterns() {
    typeCheckCustomExterns(
        DEFAULT_EXTERNS + Joiner.on('\n').join(
            "/** @const */ var ns = {};",
            "/** @type {number} */ ns.num;"),
        "var /** number */ n = ns.num;");

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + Joiner.on('\n').join(
            "/** @const */ var ns = {};",
            "/** @type {number} */ ns.num;"),
        "var /** string */ s = ns.num;",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testDontInferNamespaces() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @const */ var x = ns;"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */ var e = { FOO : 5 };",
        "/** @const */ var x = e;"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @type {number} */ ns.n = 5;",
        "/** @const */ var x = ns.n;"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);
  }

  public void testDontInferUndeclaredFunctionReturn() {
    typeCheck(Joiner.on('\n').join(
        "function f() {}",
        "/** @const */ var x = f();"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "ns.f = function() {}",
        "/** @const */ var x = f();"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);
  }

  // public void testUndeclaredNamespaces() {
  //   typeCheck(Joiner.on('\n').join(
  //       "/** @constructor */ ns.Foo = function(){};",
  //       "ns.Foo.prototype.method = function(){};"),
  //       VarCheck.UNDEFINED_VAR_ERROR,
  //       VarCheck.UNDEFINED_VAR_ERROR);
  // }

  public void testNestedNamespaces() {
    // In the previous type inference, ns.subns did not need a
    // @const annotation, but we require it.
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.subns = {};",
        "/** @type {string} */",
        "ns.subns.n = 'str';",
        "function f() { ns.subns.n - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testNonnamespaceLooksLikeANamespace() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @type {Object} */",
        "ns.obj = null;",
        "function setObj() {",
        "  ns.obj = {};",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @type {Object} */",
        "ns.obj = null;",
        "function setObj() {",
        "  ns.obj = {};",
        "  ns.obj.str = 'str';",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @type {Object} */",
        "ns.obj = null;",
        "ns.obj = {};",
        "ns.obj.x = 'str';",
        "ns.obj.x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @type {Object} */",
        "ns.obj = null;",
        "ns.obj = { x : 1, y : 5};",
        "ns.obj.x = 'str';"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @type {Object} */",
        "ns.obj = null;",
        "ns.obj = { x : 1, y : 5};",
        "ns.obj.x = 'str';",
        "ns.obj.x - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testNamespacedObjectsDontCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @constructor */",
        "ns.Foo = function() {",
        "  ns.Foo.obj.value = ns.Foo.VALUE;",
        "};",
        "ns.Foo.obj = {};",
        "ns.Foo.VALUE = 128;"));
  }

  public void testRedeclaredNamespaces() {
    // TODO(blickly): Consider a warning if RHS doesn't contain ||
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = ns || {}",
        "/** @const */ var ns = ns || {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = ns || {}",
        "ns.subns = ns.subns || {}",
        "ns.subns = ns.subns || {}"));
  }

  public void testReferenceToNonexistentNamespace() {
    // typeCheck(
    //     "/** @constructor */ ns.Foo = function(){};",
    //     VarCheck.UNDEFINED_VAR_ERROR);

    // typeCheck(
    //     "ns.subns = {};",
    //     VarCheck.UNDEFINED_VAR_ERROR);

    // typeCheck(
    //     "/** @enum {number} */ ns.NUM = { N : 1 };",
    //     VarCheck.UNDEFINED_VAR_ERROR);

    // typeCheck(
    //     "/** @typedef {number} */ ns.NUM;",
    //     VarCheck.UNDEFINED_VAR_ERROR);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @constructor */ ns.subns.Foo = function(){};"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "ns.subns.subsubns = {};"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @enum {number} */ ns.subns.NUM = { N : 1 };"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @typedef {number} */ ns.subns.NUM;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "Foo.subns.subsubns = {};"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "/** @constructor */ Foo.subns.Bar = function(){};"),
        TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testThrow() {
    typeCheck("throw 123;");

    typeCheck("var msg = 'hello'; throw msg;");

    typeCheck(Joiner.on('\n').join(
        "function f(cond, x, y) {",
        "  if (cond) {",
        "    x < y;",
        "    throw 123;",
        "  } else {",
        "    x < 2;",
        "  }",
        "}"));

    typeCheck("throw (1 - 'asdf');", NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testQnameInJsdoc() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @constructor */ ns.C = function() {};",
        "/** @param {!ns.C} x */ function f(x) {",
        "  123, x.prop;",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testIncrementDecrements() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = { x : 5 };",
        "ns.x++; ++ns.x; ns.x--; --ns.x;"));

    typeCheck(
        "function f(ns) { --ns.x; }; f({x : 'str'})",
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testAndOr() {
    typeCheck("function f(x, y, z) { return x || y && z;}");

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, /** string */ y) {",
        "  var /** number */ n = x || y;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, /** string */ y) {",
        "  var /** number */ n = y || x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function /** number */ f(/** ?number */ x) {",
        "  return x || 42;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function /** (number|string) */ f(/** ?number */ x) {",
        "  return x || 'str';",
        "}"));
  }

  public void testNonStringComparisons() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (null == x) {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x == null) {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (null == x) {",
        "    var /** null */ y = x;",
        "    var /** undefined */ z = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (5 == x) {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (x == 5) {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (null == x) {",
        "  } else {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (x == null) {",
        "  } else {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (null != x) {",
        "  } else {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x != null) {",
        "  } else {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (5 != x) {",
        "  } else {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (x != 5) {",
        "  } else {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (null != x) {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {*} x */",
        "function f(x) {",
        "  if (x != null) {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testAnalyzeLoopsBwd() {
    typeCheck("for(;;);");

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  for (; x - 5 > 0; ) {}",
        "  x = undefined;",
        "}",
        "f(true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  while (x - 5 > 0) {}",
        "  x = undefined;",
        "}",
        "f(true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (x - 5 > 0) {}",
        "  x = undefined;",
        "}",
        "f(true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  do {} while (x - 5 > 0);",
        "  x = undefined;",
        "}",
        "f(true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testDontLoosenNominalTypes() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() { this.prop = 123; }",
        "function f(x) { if (x instanceof Foo) { var y = x.prop; } }"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() { this.prop = 123; }",
        "/** @constructor */ function Bar() { this.prop = 123; }",
        "function f(cond, x) {",
        "  x = cond ? new Foo : new Bar;",
        "  var y = x.prop;",
        "}"));
  }

  public void testFunctionsWithAbnormalExit() {
    typeCheck("function f(x) { x = 1; throw x; }");

    // TODO(dimvar): to fix these, we must collect all THROWs w/out an out-edge
    // and use the envs from them in the summary calculation. (Rare case.)

    // typeCheck(Joiner.on('\n').join(
    //     "function f(x) {",
    //     "  var y = 1;",
    //     "  x < y;",
    //     "  throw 123;",
    //     "}",
    //     "f('asdf');"),
    //     NewTypeInference.INVALID_ARGUMENT_TYPE);
    // typeCheck(Joiner.on('\n').join(
    //     "function f(x, cond) {",
    //     "  if (cond) {",
    //     "    var y = 1;",
    //     "    x < y;",
    //     "    throw 123;",
    //     "  }",
    //     "}",
    //     "f('asdf', 'whatever');"),
    //     NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testAssignAdd() {
    // Without a type annotation, we can't find the type error here.
    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  x < y;",
        "  var /** number */ z = 5;",
        "  z += y;",
        "}",
        "f('asdf', 5);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  x < y;",
        "  var z = 5;",
        "  z += y;",
        "}",
        "f('asdf', 5);"));

    typeCheck(
        "var s = 'asdf'; (s += 'asdf') - 5;",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("var s = 'asdf'; s += 5;");

    typeCheck("var b = true; b += 5;", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var n = 123; n += 'asdf';", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var s = 'asdf'; s += true;", NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testTypeCoercions() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** * */ x) {",
        "  var /** string */ s = !x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** * */ x) {",
        "  var /** string */ s = +x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** * */ x) {",
        "  var /** string */ s = '' + x;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** * */ x) {",
        "  var /** number */ s = '' + x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testSwitch() {
    typeCheck(
        "switch (1) { case 1: break; case 2: break; default: break; }");

    typeCheck(Joiner.on('\n').join(
        "switch (1) {",
        "  case 1:",
        "    1 - 'asdf';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "switch (1) {",
        "  default:",
        "    1 - 'asdf';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "switch (1 - 'asdf') {",
        "  case 1:",
        "    break;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "switch (1) {",
        "  case (1 - 'asdf'):",
        "    break;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** Foo */ x) {",
        "  switch (x) {",
        "    case null:",
        "      break;",
        "    default:",
        "      var /** !Foo */ y = x;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  switch (x) {",
        "    case 123:",
        "      x - 5;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** Foo */ x) {",
        "  switch (x) {",
        "    case null:",
        "    default:",
        "      var /** !Foo */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  switch (x) {",
        "    case null:",
        "      x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  switch (x) {",
        "    case null:",
        "      var /** undefined */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    // Tests for fall-through
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  switch (x) {",
        "    case 1: x - 5;",
        "    case 'asdf': x < 123; x < 'asdf'; break;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  switch (x) {",
        "    case 1: x - 5;",
        "    case 'asdf': break;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function g(/** number */ x) { return 5; }",
        "function f() {",
        "  switch (3) { case g('asdf'): return 123; }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // TODO(dimvar): warn for type mismatch between label and condition
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, /** string */ y) {",
        "  switch (y) { case x: ; }",
        "}"));
  }

  public void testForIn() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** string */ y) {",
        "  for (var x in { a: 1, b: 2 }) { y = x; }",
        "  x = 234;",
        "  return 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(y) {",
        "  var z = x + 234;",
        "  for (var x in { a: 1, b: 2 }) {}",
        "  return 123;",
        "}"),
        // VariableReferenceCheck.EARLY_REFERENCE,
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ y) {",
        "  for (var x in { a: 1, b: 2 }) { y = x; }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "function f(/** Object? */ o) { for (var x in o); }",
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck("for (var x in 123) ;", NewTypeInference.FORIN_EXPECTS_OBJECT);

    typeCheck(
        "var /** number */ x = 5; for (x in {a : 1});",
        NewTypeInference.FORIN_EXPECTS_STRING_KEY);

    typeCheck(Joiner.on('\n').join(
        "function f(/** undefined */ y) {",
        "  var x;",
        "  for (x in { a: 1, b: 2 }) { y = x; }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testTryCatch() {
    // typeCheck(
    //     "try { e; } catch (e) {}",
    //     VariableReferenceCheck.EARLY_REFERENCE);

    // typeCheck(
    //     "e; try {} catch (e) {}",
    //     VariableReferenceCheck.EARLY_REFERENCE);

    typeCheck("try {} catch (e) { e; }");
    // If the CFG can see that the TRY won't throw, it doesn't go to the catch.
    typeCheck("try {} catch (e) { 1 - 'asdf'; }");

    typeCheck(
        "try { throw 123; } catch (e) { 1 - 'asdf'; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "try { throw 123; } catch (e) {} finally { 1 - 'asdf'; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    // Outside of the catch block, e is unknown, like any other global variable.
    typeCheck(Joiner.on('\n').join(
        "try {",
        "  throw new Error();",
        "} catch (e) {}",
        "var /** number */ n = e;"));

    // // For this to pass, we must model local scopes properly.
    // typeCheck(Joiner.on('\n').join(
    //     "var /** string */ e = 'str';",
    //     "try {",
    //     "  throw new Error();",
    //     "} catch (e) {}",
    //     "e - 3;"),
    //     NewTypeInference.INVALID_OPERAND_TYPE);

    // typeCheck(
    //     "var /** string */ e = 'asdf'; try {} catch (e) {} e - 5;",
    //     VariableReferenceCheck.REDECLARED_VARIABLE);

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  try {",
        "  } catch (e) {",
        "    return e.stack;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  try {",
        "    throw new Error();",
        "  } catch (e) {",
        "    var /** Error */ x = e;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  try {",
        "    throw new Error();",
        "  } catch (e) {",
        "    var /** number */ x = e;",
        "    var /** string */ y = e;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testIn() {
    typeCheck("(true in {});", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("('asdf' in 123);", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var /** number */ n = ('asdf' in {});",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** { a: number } */ obj) {",
        "  if ('p' in obj) {",
        "    return obj.p;",
        "  }",
        "}",
        "f({ a: 123 });"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** { a: number } */ obj) {",
        "  if (!('p' in obj)) {",
        "    return obj.p;",
        "  }",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testDelprop() {
    typeCheck("delete ({ prop: 123 }).prop;");

    typeCheck(
        "var /** number */ x = delete ({ prop: 123 }).prop;",
        NewTypeInference.MISTYPED_ASSIGN_RHS);
    // We don't detect the missing property
    typeCheck("var obj = { a: 1, b: 2 }; delete obj.a; obj.a;");
  }

  public void testArrayLit() {
    typeCheck("[1, 2, 3 - 'asdf']", NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  x < y;",
        "  [y - 5];",
        "}",
        "f('asdf', 123);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testArrayAccesses() {
    typeCheck(
        "var a = [1,2,3]; a['str'];", NewTypeInference.NON_NUMERIC_ARRAY_INDEX);

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Array<number> */ arr, i) {",
        "  arr[i];",
        "}",
        "f([1, 2, 3], 'str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testRegExpLit() {
    typeCheck("/abc/");
  }

  public void testDifficultLvalues() {
    typeCheck(Joiner.on('\n').join(
        "function f() { return {}; }",
        "f().x = 123;"));

    typeCheck(Joiner.on('\n').join(
        "function f() { return {}; }",
        "f().ns = {};"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {number} */ this.a = 123; }",
        "/** @return {!Foo} */",
        "function retFoo() { return new Foo(); }",
        "function f(cond) {",
        "  (retFoo()).a = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "(new Foo).x += 123;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {number} */ this.a = 123; }",
        "function f(cond, /** !Foo */ foo1) {",
        "  var /** { a: number } */ x = { a: 321 };",
        "  (cond ? foo1 : x).a = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "function f(obj) { obj[1 - 'str'] = 3; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "function f(/** undefined */ n, pname) { n[pname] = 3; }",
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);
  }

  public void testQuestionableUnionJsDoc() {
    // 'string|?' is the same as '?'
    typeCheck(
        "/** @type {string|?} */ var x;",
        JSTypeCreatorFromJSDoc.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "",
        "/**",
        " * @return {T|S}",
        " * @template T, S",
        " */",
        "function f(){};"));

    typeCheck("/** @param {(?)} x */ function f(x) {}");
  }

  public void testGenericsJsdocParsing() {
    typeCheck("/** @template T\n@param {T} x */ function f(x) {}");

    typeCheck(Joiner.on('\n').join(
        "/** @template T\n @param {T} x\n @return {T} */",
        "function f(x) { return x; };"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " * @param {T} x",
        " * @extends {Bar<T>} // error, Bar is not templatized ",
        " */",
        "function Foo(x) {}",
        "/** @constructor */",
        "function Bar() {}"),
        JSTypeCreatorFromJSDoc.INVALID_GENERICS_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/** @param {Foo<number, string>} x */",
        "function f(x) {}"),
        JSTypeCreatorFromJSDoc.INVALID_GENERICS_INSTANTIATION);

    typeCheck("/** @type {Array<number>} */ var x;");

    typeCheck("/** @type {Object<number>} */ var x;");

    typeCheck(
        "/** @template T\n@param {!T} x */ function f(x) {}",
        JSTypeCreatorFromJSDoc.BAD_JSDOC_ANNOTATION);
  }

  public void testPolymorphicFunctionInstantiation() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function id(x) { return x; }",
        "id('str') - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "f(123, 'asdf');"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {(T|null)} x",
        " * @return {(T|number)}",
        " */",
        "function f(x) { return x === null ? 123 : x; }",
        "/** @return {(null|undefined)} */ function g() { return null; }",
        "var /** (number|undefined) */ y = f(g());"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {(T|number)} x",
        " */",
        "function f(x) {}",
        "/** @return {*} */ function g() { return 1; }",
        "f(g());"),
        NewTypeInference.FAILED_TO_UNIFY);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function id(x) { return x; }",
        "/** @return {*} */ function g() { return 1; }",
        "id(g()) - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T, U",
        " * @param {T} x",
        " * @param {U} y",
        " * @return {U}",
        " */",
        "function f(x, y) { return y; }",
        "f(10, 'asdf') - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g(x) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  f(x, 5);",
        "}",
        "g('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g(/** ? */ x) {",
        "  /**",
        "   * @template T",
        "   * @param {(T|number)} x",
        "   */",
        "  function f(x) {}",
        "  f(x)",
        "}"));

    // TODO(blickly): Catching the INVALID_ARGUMENT_TYPE here requires
    // return-type unification.
    typeCheck(Joiner.on('\n').join(
        "function g(x) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @return {T}",
        "   */",
        "  function f(x) { return x; }",
        "  f(x) - 5;",
        "  x = 'asdf';",
        "}",
        "g('asdf');"));

    // Empty instantiations
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {(T|number)} x",
        " */",
        "function f(x) {}",
        "f(123);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {(T|null)} x",
        " * @param {(T|number)} y",
        " */",
        "function f(x, y) {}",
        "f(null, 'str');"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){};",
        "/**",
        " * @template T",
        " * @param {(T|Foo)} x",
        " * @param {(T|number)} y",
        " */",
        "function f(x, y) {}",
        "f(new Foo(), 'str');"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(T):T} f",
        " * @param {T} x",
        " */",
        "function apply(f, x) { return f(x); }",
        "/** @type {string} */",
        "var out;",
        "var result = apply(function(x){ out = x; return x; }, 0);"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/** @template T */",
        "function f(/** T */ x, /** T */ y) {}",
        "f(1, 'str');"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/** @template T */",
        "function /** T */ f(/** T */ x) { return x; }",
        "f('str') - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  /** @constructor */",
        "  function Foo() {",
        "    /** @type {T} */",
        "    this.prop = x;",
        "  }",
        "  return (new Foo()).prop;",
        "}",
        "f('asdf') - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {*} x",
        " */",
        "function f(x) {}",
        "f(123);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "/**",
        " * @template U",
        " * @param {function(U)} x",
        " */",
        "Foo.prototype.f = function(x) { this.f(x); };"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(T)} x",
        " */",
        "function f(x) {}",
        "function g(x) {}",
        "f(g);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(T=)} x",
        " */",
        "function f(x) {}",
        "function g(/** (number|undefined) */ x) {}",
        "f(g);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(...T)} x",
        " */",
        "function f(x) {}",
        "function g() {}",
        "f(g);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {!Array<T>} arr",
        " * @param {?function(this:S, T, number, ?) : boolean} f",
        " * @param {S=} opt_obj",
        " * @return {T|null}",
        " * @template T,S",
        " */",
        "function gaf(arr, f, opt_obj) {",
        "  return null;",
        "};",
        "/** @type {number|null} */",
        "var x = gaf([1, 2, 3], function(x, y, z) { return true; });"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(T):boolean} x",
        " */",
        "function f(x) {}",
        "f(function(x) { return 'asdf'; });"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {function(T)} y",
        " */",
        "function f(x, y) {}",
        "f(123, function(x) { var /** string */ s = x; });"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {}",
        "var y = null;",
        "if (!y) {",
        "} else {",
        "  f(y)",
        "}"));
  }

  public void testGenericReturnType() {
    typeCheck(Joiner.on('\n').join(
        "/** @return {T|string} @template T */",
        "function f() { return 'str'; }"));
  }

  public void testUnificationWithGenericUnion() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template T */ function Foo(){}",
        "/**",
        " * @template T",
        " * @param {!Array<T>|!Foo<T>} arr",
        " * @return {T}",
        " */",
        "function get(arr) {",
        "  return arr[0];",
        "}",
        "var /** null */ x = get([5]);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {Array<T>} arr",
        " * @return {T|undefined}",
        " */",
        "function get(arr) {",
        "  if (arr === null || arr.length === 0) {",
        "    return undefined;",
        "  }",
        "  return arr[0];",
        "}",
        "var /** (number|undefined) */ x = get([5]);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {Array<T>} arr",
        " * @return {T|undefined}",
        " */",
        "function get(arr) {",
        "  if (arr === null || arr.length === 0) {",
        "    return undefined;",
        "  }",
        "  return arr[0];",
        "}",
        "var /** null */ x = get([5]);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template U */ function Foo(/** U */ x){}",
        "/**",
        " * @template T",
        " * @param {U|!Array<T>} arr",
        " * @return {U}",
        " */",
        "Foo.prototype.get = function(arr, /** ? */ opt_arg) {",
        "  return opt_arg;",
        "}",
        "var /** null */ x = (new Foo('str')).get([5], 1);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template U */ function Foo(/** U */ x){}",
        "/**",
        " * @template T",
        " * @param {U|!Array<T>} arr",
        " * @return {U}",
        " */",
        "Foo.prototype.get = function(arr, /** ? */ opt_arg) {",
        "  return opt_arg;",
        "}",
        "Foo.prototype.f = function() {",
        "  var /** null */ x = this.get([5], 1);",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " */",
        "function Bar() {}",
        "/** @constructor */",
        "function Foo() {}",
        "/**",
        " * @template T",
        " * @param {!Bar<(!Bar<T>|!Foo)>} x",
        " */",
        "function f(x) {}",
        "f(/** @type {!Bar<!Bar<number>>} */ (new Bar));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/**",
        " * @template T",
        " * @param {T|null} x",
        " */",
        "function f(x) {}",
        "f(new Foo);"));
  }

  public void testBoxedUnification() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {V} value",
        " * @constructor",
        " * @template V",
        " */",
        "function Box(value) {};",
        "/**",
        " * @constructor",
        " * @param {K} key",
        " * @param {V} val",
        " * @template K, V",
        " */",
        "function Map(key, val) {};",
        "/**",
        " * @param {!Map<K, (V | !Box<V>)>} inMap",
        " * @constructor",
        " * @template K, V",
        " */",
        "function WrappedMap(inMap){};",
        "/** @return {(boolean |!Box<boolean>)} */",
        "function getUnion(/** ? */ u) { return u; }",
        "var inMap = new Map('asdf', getUnion(123));",
        "/** @param {!WrappedMap<string, boolean>} x */",
        "function getWrappedMap(x) {}",
        "getWrappedMap(new WrappedMap(inMap));"));
  }


  public void testUnification() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){};",
        "/** @constructor */ function Bar(){};",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function id(x) { return x; }",
        "var /** Bar */ x = id(new Foo);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function id(x) { return x; }",
        "id({}) - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function id(x) { return x; }",
        "var /** (number|string) */ x = id('str');"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** * */ a, /** string */ b) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  f(a, b);",
        "}"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "function f(/** string */ b) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  f({p:5, r:'str'}, {p:20, r:b});",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** string */ b) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  f({r:'str'}, {p:20, r:b});",
        "}"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "function g(x) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  var /** boolean */ y = true;",
        "  f(x, y);",
        "}",
        "g('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {number} y",
        " */",
        "function f(x, y) {}",
        "f(123, 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "/**",
        " * @template T",
        " * @param {Foo<T>} x",
        " */",
        "function takesFoo(x) {}",
        "takesFoo(undefined);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T|undefined} x",
        " */",
        "function f(x) {}",
        "/**",
        " * @template T",
        " * @param {T|undefined} x",
        " */",
        "function g(x) { f(x); }"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T|undefined} x",
        " * @return {T}",
        " */",
        "function f(x) {",
        "  if (x === undefined) {",
        "    throw new Error('');",
        "  }",
        "  return x;",
        "}",
        "/**",
        " * @template T",
        " * @param {T|undefined} x",
        " * @return {T}",
        " */",
        "function g(x) { return f(x); }",
        "g(123) - 5;"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T|undefined} x",
        " * @return {T}",
        " */",
        "function f(x) {",
        "  if (x === undefined) {",
        "    throw new Error('');",
        "  }",
        "  return x;",
        "}",
        "/**",
        " * @template T",
        " * @param {T|undefined} x",
        " * @return {T}",
        " */",
        "function g(x) { return f(x); }",
        "g(123) < 'asdf';"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testUnifyObjects() {
    typeCheck(Joiner.on('\n').join(
        "function f(b) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  f({p:5, r:'str'}, {p:20, r:b});",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(b) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  f({p:20, r:b}, {p:5, r:'str'});",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function g(x) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  f({prop: x}, {prop: 5});",
        "}",
        "g('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g(x, cond) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  var y = cond ? {prop: 'str'} : {prop: 5};",
        "  f({prop: x}, y);",
        "}",
        "g({}, true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function g(x, cond) {",
        "  /**",
        "   * @template T",
        "   * @param {T} x",
        "   * @param {T} y",
        "   */",
        "  function f(x, y) {}",
        "  /** @type {{prop : (string | number)}} */",
        "  var y = cond ? {prop: 'str'} : {prop: 5};",
        "  f({prop: x}, y);",
        "}",
        "g({}, true);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {{a: number, b: T}} x",
        " * @return {T}",
        " */",
        "function f(x) { return x.b; }",
        "f({a: 1, b: 'asdf'}) - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @return {T}",
        " */",
        "function f(x) { return x.b; }",
        "f({b: 'asdf'}) - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testFunctionTypeUnifyUnknowns() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "/** @type {function(number)} */",
        "function g(x) {}",
        "/** @type {function(?)} */",
        "function h(x) {}",
        "f(g, h);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "/** @type {function(number)} */",
        "function g(x) {}",
        "/** @type {function(string)} */",
        "function h(x) {}",
        "f(g, h);"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "/** @type {function(number)} */",
        "function g(x) {}",
        "/** @type {function(?, string)} */",
        "function h(x, y) {}",
        "f(g, h);"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "/** @type {function(number=, ...string)} */",
        "function g(x) {}",
        "/** @type {function(number=, ...?)} */",
        "function h(x) {}",
        "f(g, h);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "/** @type {function(number):number} */",
        "function g(x) { return 1; }",
        "/** @type {function(?):string} */",
        "function h(x) { return ''; }",
        "f(g, h);"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "/** @constructor */ function Foo() {}",
        "/** @type {function(new:Foo)} */",
        "function g() {}",
        "/** @type {function(new:Foo)} */",
        "function h() {}",
        "f(g, h);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "/** @constructor */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "/** @type {function(this:Foo)} */",
        "function g() {}",
        "/** @type {function(this:Bar)} */",
        "function h() {}",
        "f(g, h);"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);
  }

  public void testInstantiationInsideObjectTypes() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template U",
        " * @param {U} y",
        " */",
        "function g(y) {",
        "  /**",
        "   * @template T",
        "   * @param {{a: U, b: T}} x",
        "   * @return {T}",
        "   */",
        "  function f(x) { return x.b; }",
        "  f({a: y, b: 'asdf'}) - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template U",
        " * @param {U} y",
        " */",
        "function g(y) {",
        "  /**",
        "   * @template T",
        "   * @param {{b: T}} x",
        "   * @return {T}",
        "   */",
        "  function f(x) { return x.b; }",
        "  f({b: y}) - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testInstantiateInsideFunctionTypes() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {function(T):T} fun",
        " */",
        "function f(x, fun) {}",
        "function g(x) { return x - 5; }",
        "f('asdf', g);"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(T):number} fun",
        " */",
        "function f(fun) {}",
        "function g(x) { return 'asdf'; }",
        "f(g);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(T=)} fun",
        " */",
        "function f(fun) {}",
        "/** @param{string=} x */ function g(x) {}",
        "f(g);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(...T)} fun",
        " */",
        "function f(fun) {}",
        "/** @param {...number} var_args */ function g(var_args) {}",
        "f(g);"));
  }

  public void testPolymorphicFuncallsFromDifferentScope() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function id(x) { return x; }",
        "function g() {",
        "  id('asdf') - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {number} y",
        " */",
        "function f(x, y) {}",
        "function g() {",
        "  f('asdf', 'asdf');",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "function g() {",
        "  f(123, 'asdf');",
        "}"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);
  }

  public void testOpacityOfTypeParameters() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  x - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {{ a: T }} x",
        " */",
        "function f(x) {",
        "  x.a - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {function(T):T} fun",
        " */",
        "function f(x, fun) {",
        "  fun(x) - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function f(x) {",
        "  return 5;",
        "}"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  var /** ? */ y = x;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {(T|number)}",
        " */",
        "function f(x) {",
        "  var y;",
        "  if (1 < 2) {",
        "    y = x;",
        "  } else {",
        "    y = 123;",
        "  }",
        "  return y;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {(T|number)}",
        " */",
        "function f(x) {",
        "  var y;",
        "  if (1 < 2) {",
        "    y = x;",
        "  } else {",
        "    y = 123;",
        "  }",
        "  return y;",
        "}",
        "f(123) - 5;"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {(T|number)}",
        " */",
        "function f(x) {",
        "  var y;",
        "  if (1 < 2) {",
        "    y = x;",
        "  } else {",
        "    y = 123;",
        "  }",
        "  return y;",
        "}",
        "var /** (number|boolean) */ z = f('asdf');"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  var /** T */ y = x;",
        "  y - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T, U",
        " * @param {T} x",
        " * @param {U} y",
        " */",
        "function f(x, y) {",
        "  x = y;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testGenericClassInstantiation() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) {}",
        "/** @param {T} y */",
        "Foo.prototype.bar = function(y) {}",
        "new Foo('str').bar(5)"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) {}",
        "/** @type {function(T)} y */",
        "Foo.prototype.bar = function(y) {};",
        "new Foo('str').bar(5)"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) { /** @type {T} */ this.x = x; }",
        "/** @return {T} */",
        "Foo.prototype.bar = function() { return this.x; };",
        "new Foo('str').bar() - 5"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) { /** @type {T} */ this.x = x; }",
        "/** @type {function() : T} */",
        "Foo.prototype.bar = function() { return this.x; };",
        "new Foo('str').bar() - 5"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) {}",
        "/** @type {function(this:Foo<T>, T)} */",
        "Foo.prototype.bar = function(x) { this.x = x; };",
        "new Foo('str').bar(5)"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) {}",
        "/** @param {!Foo<number>} x */",
        "function f(x) {}",
        "f(new Foo(7));"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) {}",
        "/** @param {Foo<number>} x */",
        "function f(x) {}",
        "f(new Foo('str'));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) {}",
        "/** @param {T} x */",
        "Foo.prototype.method = function(x) {};",
        "/** @param {!Foo<number>} x */",
        "function f(x) { x.method('asdf'); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "/** @param {T} x */",
        "Foo.prototype.method = function(x) {};",
        "var /** @type {Foo<string>} */ foo = null;",
        "foo.method('asdf');"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);
  }

  public void testLooserCheckingForInferredProperties() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo(x) { this.prop = x; }",
        "function f(/** !Foo */ obj) {",
        "  obj.prop = true ? 1 : 'asdf';",
        "  obj.prop - 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo(x) { this.prop = x; }",
        "function f(/** !Foo */ obj) {",
        "  if (!(typeof obj.prop == 'number')) {",
        "    obj.prop < 'asdf';",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo(x) { this.prop = x; }",
        "function f(/** !Foo */ obj) {",
        "  obj.prop = true ? 1 : 'asdf';",
        "  obj.prop - 5;",
        "  obj.prop < 'asdf';",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function /** string */ f(/** ?number */ x) {",
        "  var o = { prop: 'str' };",
        "  if (x) {",
        "    o.prop = x;",
        "  }",
        "  return o.prop;",
        "}"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);
  }

  public void testInheritanceWithGenerics() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/** @param {T} x */",
        "I.prototype.bar = function(x) {};",
        "/** @constructor @implements {I<number>} */",
        "function Foo() {}",
        "Foo.prototype.bar = function(x) {};",
        "(new Foo).bar(123);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/** @param {T} x */",
        "I.prototype.bar = function(x) {};",
        "/** @constructor @implements {I<number>} */",
        "function Foo() {}",
        "Foo.prototype.bar = function(x) {};",
        "(new Foo).bar('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/** @param {T} x */",
        "I.prototype.bar = function(x) {};",
        "/** @constructor @implements {I<number>} */",
        "function Foo() {}",
        "/** @override */",
        "Foo.prototype.bar = function(x) {};",
        "new Foo().bar('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/** @param {T} x */",
        "I.prototype.bar = function(x) {};",
        "/**",
        " * @template U",
        " * @constructor",
        " * @implements {I<U>}",
        " * @param {U} x",
        " */",
        "function Foo(x) {}",
        "Foo.prototype.bar = function(x) {};{}",
        "new Foo(5).bar('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/** @param {T} x */",
        "I.prototype.bar = function(x) {};",
        "/** @constructor @implements {I<number>} */",
        "function Foo() {}",
        "Foo.prototype.bar = function(x) {};",
        "/** @param {I<string>} x */ function f(x) {};",
        "f(new Foo());"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/** @param {T} x */",
        "I.prototype.bar = function(x) {};",
        "/** @constructor @implements {I<number>} */",
        "function Foo() {}",
        "/** @param {string} x */",
        "Foo.prototype.bar = function(x) {};"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/** @param {T} x */",
        "I.prototype.bar = function(x) {};",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor @implements {I<number>}",
        " */",
        "function Foo(x) {}",
        "/** @param {T} x */",
        "Foo.prototype.bar = function(x) {};"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " */",
        "function Foo() {}",
        "/** @param {T} x */",
        "Foo.prototype.method = function(x) {};",
        "/**",
        " * @template T",
        " * @constructor",
        " * @extends {Foo<T>}",
        " * @param {T} x",
        " */",
        "function Bar(x) {}",
        "/** @param {number} x */",
        "Bar.prototype.method = function(x) {};"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " */",
        "function High() {}",
        "/** @param {Low<T>} x */",
        "High.prototype.method = function(x) {};",
        "/**",
        " * @template T",
        " * @constructor",
        " * @extends {High<T>}",
        " */",
        "function Low() {}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " */",
        "function High() {}",
        "/** @param {Low<number>} x */",
        "High.prototype.method = function(x) {};",
        "/**",
        " * @template T",
        " * @constructor",
        " * @extends {High<T>}",
        " */",
        "function Low() {}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " */",
        "function High() {}",
        "/** @param {Low<T>} x */ // error, low is not templatized",
        "High.prototype.method = function(x) {};",
        "/**",
        " * @constructor",
        " * @extends {High<number>}",
        " */",
        "function Low() {}"),
        JSTypeCreatorFromJSDoc.INVALID_GENERICS_INSTANTIATION);

    // BAD INHERITANCE, WE DON'T HAVE A WARNING TYPE FOR THIS
    // TODO(dimvar): fix
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function I() {}",
        "/**",
        " * @template T",
        " * @constructor",
        " * @implements {I<T>}",
        " * @extends {Bar}",
        " */",
        "function Foo(x) {}",
        "/**",
        " * @constructor",
        " * @implements {I<number>}",
        " */",
        "function Bar(x) {}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function Foo() {}",
        "/** @constructor @implements {Foo<number>} */",
        "function A() {}",
        "var /** Foo<number> */ x = new A();"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "High.prototype.method = function (x) {};",
        "/** @constructor @implements {High} */",
        "function Low() {}",
        "Low.prototype.method = function (x) {",
        "  return x;",
        "};",
        "(new Low).method(123) - 123;"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "High.prototype.method = function (x) {};",
        "/** @constructor @implements {High} */",
        "function Low() {}",
        "Low.prototype.method = function (x) {",
        "  return x;",
        "};",
        "(new Low).method('str') - 123;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @interface",
        " */",
        "function High() {}",
        "/** @return {T} */",
        "High.prototype.method = function () {};",
        "/** @constructor @implements {High} */",
        "function Low() {}",
        "Low.prototype.method = function () { return /** @type {?} */ (null); };",
        "(new Low).method() - 123;",
        "(new Low).method() < 'asdf';"));
  }

  public void testGenericsSubtyping() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "Parent.prototype.method = function(x, y){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {number} x",
        " * @param {number} y",
        " */",
        "Child.prototype.method = function(x, y){};"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "Parent.prototype.method = function(x, y){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {?} x",
        " * @param {number} y",
        " */",
        "Child.prototype.method = function(x, y){};"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "Parent.prototype.method = function(x, y){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {*} x",
        " * @param {*} y",
        " */",
        "Child.prototype.method = function(x, y){};"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "Parent.prototype.method = function(x, y){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {?} x",
        " * @param {?} y",
        " */",
        "Child.prototype.method = function(x, y){};"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {?} x",
        " * @return {?}",
        " */",
        "Child.prototype.method = function(x){ return x; };"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {*} x",
        " * @return {?}",
        " */",
        "Child.prototype.method = function(x){ return x; };"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {*} x",
        " * @return {*}",
        " */",
        "Child.prototype.method = function(x){ return x; };"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {number} x",
        " * @return {number}",
        " */",
        "Child.prototype.method = function(x){ return x; };"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {?} x",
        " * @return {*}",
        " */",
        "Child.prototype.method = function(x){ return x; };"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template T",
        " * @param {function(T, T) : boolean} x",
        " */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @param {function(number, number) : boolean} x",
        " */",
        "Child.prototype.method = function(x){ return x; };"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {}",
        "/** @param {function(number, number)} x */",
        "function g(x) {}",
        "g(f);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {}",
        "/** @param {function()} x */",
        "function g(x) {}",
        "g(f);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Parent() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "Parent.prototype.method = function(x) {};",
        "/**",
        " * @constructor",
        " * @implements {Parent}",
        " */",
        "function Child() {}",
        "/**",
        " * @template U",
        " * @param {U} x",
        " */",
        "Child.prototype.method = function(x) {};"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/** @param {string} x */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "Child.prototype.method = function(x){};"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/** @param {*} x */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "Child.prototype.method = function(x){};"));

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/** @param {?} x */",
        "Parent.prototype.method = function(x){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "Child.prototype.method = function(x){};"));

    // This shows a bug in subtyping of generic functions.
    // We don't catch the invalid prop override.
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @param {string} x",
        " * @param {number} y",
        " */",
        "Parent.prototype.method = function(x, y){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "Child.prototype.method = function(x, y){};"));

    // This shows a bug in subtyping of generic functions.
    // We don't catch the invalid prop override.
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Parent() {}",
        "/**",
        " * @template A, B",
        " * @param {A} x",
        " * @param {B} y",
        " * @return {A}",
        " */",
        "Parent.prototype.method = function(x, y){};",
        "/** @constructor @implements {Parent} */",
        "function Child() {}",
        "/**",
        " * @template A, B",
        " * @param {A} x",
        " * @param {B} y",
        " * @return {B}",
        " */",
        "Child.prototype.method = function(x, y){ return y; };"));
  }

  public void testGenericsVariance() {
    // Array generic parameter is co-variant
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "var /** Array<Foo> */ a = [new Bar];"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {!Array<number|string>} x",
        " * @return {!Array<number>}",
        " */",
        "function f(x) {",
        "  return /** @type {!Array<number>} */ (x);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "var /** Array<Bar> */ a = [new Foo];"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {!Array<null|T>} y",
        " */",
        "function f(x, y) {}",
        "f(new Foo, [new Foo]);"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @param {T} x @template T */ function Gen(x){}",
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "var /** Gen<Foo> */ a = new Gen(new Bar);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @param {T} x @template T */ function Gen(x){}",
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "var /** Gen<Bar> */ a = new Gen(new Foo);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testInferredArrayGenerics() {
    typeCheck(
        "/** @const */ var x = [];", GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(
        "/** @const */ var x = [1, 'str'];",
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "/** @const */ var x = [new Foo, new Bar];"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(
        "var /** Array<string> */ a = [1, 2];",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var arr = [];",
        "var /** Array<string> */ as = arr;"));

    typeCheck(Joiner.on('\n').join(
        "var arr = [1, 2, 3];",
        "var /** Array<string> */ as = arr;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "var /** Array<string> */ a = [new Foo, new Foo];"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "var /** Array<Foo> */ a = [new Foo, new Bar];"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "var /** Array<Bar> */ a = [new Foo, new Bar];"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var x = [1, 2, 3];",
        "function g() { var /** Array<string> */ a = x; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "/** @const */ var x = [new Foo, new Foo];",
        "function g() { var /** Array<Bar> */ a = x; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testSpecializedInstanceofCantGoToBottom() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "ns.f = function() {};",
        "if (ns.f instanceof Function) {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "/** @const */ var ns = {};",
        "ns.f = new Foo;",
        "if (ns.f instanceof Foo) {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "/** @constructor */ function Bar(){}",
        "/** @const */ var ns = {};",
        "ns.f = new Foo;",
        "if (ns.f instanceof Bar) {}"));
  }

  public void testDeclaredGenericArrayTypes() {
    typeCheck(Joiner.on('\n').join(
        "/** @type {Array<string>} */",
        "var arr = ['str'];",
        "arr[0]++;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var arr = ['str'];",
        "arr[0]++;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function foo (/** Array<string> */ a) {}",
        "/** @type {Array<number>} */",
        "var b = [1];",
        "foo(b);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function foo (/** Array<string> */ a) {}",
        "foo([1]);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @type {!Array<number>} */",
        "var arr = [1, 2, 3];",
        "arr[0] = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @type {!Array<number>} */",
        "var arr = [1, 2, 3];",
        "arr['0'] = 'str';"));

    // We warn here even though the declared type of the lvalue includes null.
    typeCheck(Joiner.on('\n').join(
        "/** @type {Array<number>} */",
        "var arr = [1, 2, 3];",
        "arr[0] = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** Array<number> */ arr) {",
        "  arr[0] = 'str';",
        "}"),
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var arr = [1, 2, 3];",
        "arr[0] = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var arr = [1, 2, 3];",
        "arr[0] = 'str';"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Super(){}",
        "/** @constructor @extends {Super} */ function Sub(){}",
        "/** @type {!Array<Super>} */ var arr = [new Sub];",
        "arr[0] = new Super;"));

    typeCheck(Joiner.on('\n').join(
        "/** @type {Array<number>} */ var arr = [];",
        "arr[0] = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @type {Array<number>} */ var arr = [];",
        "(function (/** Array<string> */ x){})(arr);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function /** string */ f(/** !Array<number> */ arr) {",
        "  return arr[0];",
        "}"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    // TODO(blickly): Would be nice if we caught the MISTYPED_ASSIGN_RHS here
    typeCheck(Joiner.on('\n').join(
        "var arr = [];",
        "arr[0] = 5;",
        "var /** Array<string> */ as = arr;"));
  }

  public void testInferConstTypeFromGoogGetMsg() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var s = goog.getMsg('asdf');",
        "s - 1;"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testInferConstTypeFromQualifiedName() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {}",
        "/** @return {string} */ ns.f = function() { return 'str'; };",
        "/** @const */ var s = ns.f();",
        "function f() {",
        "  s - 1;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testInferConstTypeFromGenerics() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function f(x) { return x; }",
        "/** @const */ var x = f(5);",
        "function g() { var /** null */ n = x; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @constructor",
        " */",
        "function Foo(x) {}",
        "/** @const */ var foo_str = new Foo('str');",
        "function g() { var /** !Foo<number> */ foo_num = foo_str; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function f(x) { return x; }",
        "/** @const */ var x = f(f ? 'str' : 5);"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " * @return {T}",
        " */",
        "function f(x, y) { return true ? y : x; }",
        "/** @const */ var x = f(5, 'str');",
        "function g() { var /** null */ n = x; }"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE,
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function f(x) { return x; }",
        "/** @const */",
        "var y = f(1, 2);"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE,
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function f(x) { return x; }",
        "/** @const */",
        "var y = f();"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE,
        TypeCheck.WRONG_ARGUMENT_COUNT);
  }

  public void testDifficultClassGenericsInstantiation() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/** @param {Bar<T>} x */",
        "Foo.prototype.method = function(x) {};",
        "/**",
        " * @template T",
        " * @constructor",
        " * @param {T} x",
        " */",
        "function Bar(x) {}",
        "/** @param {Foo<T>} x */",
        "Bar.prototype.method = function(x) {};",
        "(new Foo(123)).method(new Bar('asdf'));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/** @param {Foo<Foo<T>>} x */",
        "Foo.prototype.method = function(x) {};",
        "(new Foo(123)).method(new Foo(new Foo('asdf')));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface\n @template T */function A() {};",
        "/** @return {T} */A.prototype.foo = function() {};",
        "/** @interface\n @template U\n @extends {A<U>} */function B() {};",
        "/** @constructor\n @implements {B<string>} */function C() {};",
        "/** @return {string}\n @override */",
        "C.prototype.foo = function() { return 123; };"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    // Polymorphic method on a generic class.
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/**",
        " * @template U",
        " * @param {U} x",
        " * @return {U}",
        " */",
        "Foo.prototype.method = function(x) { return x; };",
        "(new Foo(123)).method('asdf') - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    // typeCheck(Joiner.on('\n').join(
    //     "/**",
    //     " * @template T",
    //     " * @constructor",
    //     " */",
    //     "function Foo() {}",
    //     "/** @param {T} x */",
    //     "Foo.prototype.method = function(x) {};",
    //     "",
    //     "/**",
    //     " * @template T",
    //     " * @constructor",
    //     " * @extends {Foo<T>}",
    //     " * @param {T} x",
    //     " */",
    //     "function Bar(x) {}",
    //     // Invalid instantiation here, must be T, o/w bugs like the call to f
    //     "/** @param {number} x */",
    //     "Bar.prototype.method = function(x) {};",
    //     "",
    //     "/** @param {!Foo<string>} x */",
    //     "function f(x) { x.method('sadf'); };",
    //     "f(new Bar('asdf'));"),
    //     NewTypeInference.FAILED_TO_UNIFY);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T,U",
        " */",
        "function Foo() {}",
        "Foo.prototype.m1 = function() {",
        "  this.m2(123);",
        "};",
        "/**",
        " * @template U",
        " * @param {U} x",
        " */",
        "Foo.prototype.m2 = function(x) {};"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T, U",
        " */",
        "function Foo() {}",
        "/**",
        " * @template T", // shadows Foo#T, U still visible
        " * @param {T} x",
        " * @param {U} y",
        " */",
        "Foo.prototype.method = function(x, y) {};",
        "var obj = /** @type {!Foo<number, number>} */ (new Foo);",
        "obj.method('asdf', 123);", // OK
        "obj.method('asdf', 'asdf');"), // warning
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function High() {}",
        "/** @param {T} x */",
        "High.prototype.method = function(x) {};",
        "/**",
        " * @constructor",
        " * @implements {High<number>}",
        " */",
        "function Low() {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "Low.prototype.method = function(x) {};"));
  }

  public void testNominalTypeUnification() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T, U",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/**",
        " * @template T",
        // {!Foo<T>} is instantiating only the 1st template var of Foo
        " * @param {!Foo<T>} x",
        " */",
        "function fn(x) {}",
        "fn(new Foo('asdf'));"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template S, T",
        " * @param {S} x",
        " */",
        "function Foo(x) {",
        "  /** @type {S} */ this.prop = x;",
        "}",
        "/**",
        " * @template T",
        // {!Foo<T>} is instantiating only the 1st template var of Foo
        " * @param {!Foo<T>} x",
        " * @return {T}",
        " */",
        "function fn(x) { return x.prop; }",
        "fn(new Foo('asdf')) - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testCasts() {
    typeCheck("(/** @type {number} */ ('asdf'));", TypeValidator.INVALID_CAST);

    typeCheck(Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  var y = /** @type {number} */ (x);",
        "}"));

    typeCheck("(/** @type {(number|string)} */ (1));");

    typeCheck("(/** @type {number} */ (/** @type {?} */ ('asdf')))");

    // Ignore null when checking whether we are casting to a subtype
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {}",
        "/** @constructor @extends {Parent} */",
        "function Child() {}",
        "/** @type {Child|null} */ (new Parent);"));
  }

  public void testOverride() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Intf() {}",
        "/** @param {(number|string)} x */",
        "Intf.prototype.method = function(x) {};",
        "/**",
        " * @constructor",
        " * @implements {Intf}",
        " */",
        "function C() {}",
        "/** @override */",
        "C.prototype.method = function (x) { x - 1; };",
        "(new C).method('asdf');"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Intf() {}",
        "/** @param {(number|string)} x */",
        "Intf.prototype.method = function(x) {};",
        "/**",
        " * @constructor",
        " * @implements {Intf}",
        " */",
        "function C() {}",
        "/** @inheritDoc */",
        "C.prototype.method = function (x) { x - 1; };",
        "(new C).method('asdf');"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @override */",
        "Foo.prototype.method = function() {};"),
        TypeCheck.UNKNOWN_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @inheritDoc */",
        "Foo.prototype.method = function() {};"),
        TypeCheck.UNKNOWN_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "/** @param {number=} x */",
        "High.prototype.f = function(x) {};",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "/** @override */",
        "Low.prototype.f = function(x) {};",
        "(new Low).f();",
        "(new Low).f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function F() {}",
        "/**",
        " * @param {string} x",
        " * @param {...*} var_args",
        " * @return {*}",
        " */",
        "F.prototype.method;",
        "/**",
        " * @constructor",
        " * @extends {F}",
        " */",
        "function G() {}",
        "/** @override */",
        "G.prototype.method = function (x, opt_index) {};",
        "(new G).method('asdf');"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function F() {}",
        "/**",
        " * @param {string} x",
        " * @param {...number} var_args",
        " * @return {number}",
        " */",
        "F.prototype.method;",
        "/**",
        " * @constructor",
        " * @extends {F}",
        " */",
        "function G() {}",
        "/** @override */",
        "G.prototype.method = function (x, opt_index) {};",
        "(new G).method('asdf', 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE,
        CheckMissingReturn.MISSING_RETURN_STATEMENT);
  }

  public void testOverrideNoInitializer() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Intf() {}",
        "/** @param {number} x */",
        "Intf.prototype.method = function(x) {};",
        "/** @interface @extends {Intf} */",
        "function Subintf() {}",
        "/** @override */",
        "Subintf.prototype.method;",
        "function f(/** !Subintf */ x) { x.method('asdf'); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Intf() {}",
        "/** @param {number} x */",
        "Intf.prototype.method = function(x) {};",
        "/** @interface @extends {Intf} */",
        "function Subintf() {}",
        "Subintf.prototype.method;",
        "function f(/** !Subintf */ x) { x.method('asdf'); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Intf() {}",
        "/** @param {number} x */",
        "Intf.prototype.method = function(x) {};",
        "/** @constructor  @implements {Intf} */",
        "function C() {}",
        "/** @override */",
        "C.prototype.method = (function(){ return function(x){}; })();",
        "(new C).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Intf() {}",
        "/** @param {number} x */",
        "Intf.prototype.method = function(x) {};",
        "/** @constructor  @implements {Intf} */",
        "function C() {}",
        "C.prototype.method = (function(){ return function(x){}; })();",
        "(new C).method('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Intf() {}",
        "/** @type {string} */",
        "Intf.prototype.s;",
        "/** @constructor @implements {Intf} */",
        "function C() {}",
        "/** @override */",
        "C.prototype.s = 'str2';",
        "(new C).s - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Intf() {}",
        "/** @type {string} */",
        "Intf.prototype.s;",
        "/** @constructor @implements {Intf} */",
        "function C() {}",
        "/** @type {number} @override */",
        "C.prototype.s = 72;"),
        GlobalTypeInfo.INVALID_PROP_OVERRIDE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Intf() {}",
        "/** @type {string} */",
        "Intf.prototype.s;",
        "/** @constructor @implements {Intf} */",
        "function C() {}",
        "/** @override */",
        "C.prototype.s = 72;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testFunctionConstructor() {
    typeCheck(Joiner.on('\n').join(
        "/** @type {Function} */ function topFun() {}",
        "topFun(1);"));

    typeCheck(
        "/** @type {Function} */ function topFun(x) { return x - 5; }");

    typeCheck(Joiner.on('\n').join(
        "function f(/** Function */ fun) {}",
        "f(function g(x) { return x - 5; });"));

    typeCheck(
        "function f(/** !Function */ fun) { return new fun(1, 2); }");

    typeCheck("function f(/** !Function */ fun) { [] instanceof fun; }");
  }

  public void testConditionalExBranch() {
    typeCheck(Joiner.on('\n').join(
        "function g() { throw 1; }",
        "function f() {",
        "  try {",
        "    if (g()) {}",
        "  } catch (e) {}",
        "};"));
  }

  public void testGenericInterfaceDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @interface @template T */",
        "ns.Interface = function(){}"));
  }

  public void testGetpropOnTopDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "/** @type {*} */ Foo.prototype.stuff;",
        "function f(/** !Foo */ foo, x) {",
        "  (foo.stuff.prop = x) || false;",
        "};"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {};",
        "/** @type {*} */ Foo.prototype.stuff;",
        "function f(/** Foo */ foo) {",
        "  foo.stuff.prop || false;",
        "};"),
        NewTypeInference.NULLABLE_DEREFERENCE);
  }

  public void testImplementsGenericInterfaceDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface @template Z */",
        "function Foo(){}",
        "Foo.prototype.getCount = function /** number */ (){};",
        "/**",
        " * @constructor @implements {Foo<T>}",
        " * @template T",
        " */",
        "function Bar(){}",
        "Bar.prototype.getCount = function /** number */ (){};"));
  }

  public void testDeadCodeDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "   throw 'Error';",
        "   return 5;",
        "}"));
  }

  public void testSpecializeFunctionToNominalDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Foo() {}",
        "function reqFoo(/** Foo */ foo) {};",
        "/** @param {Function} fun */",
        "function f(fun) {",
        "    reqFoo(fun);",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "function f(x) {",
        "  if (typeof x == 'function') {",
        "    var /** !Foo */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testPrototypeMethodOnUndeclaredDoesntCrash() {
    typeCheck(
        "Foo.prototype.method = function(){ this.x = 5; };",
        // VarCheck.UNDEFINED_VAR_ERROR,
        CheckGlobalThis.GLOBAL_THIS);
  }

  public void testFunctionGetpropDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "function g() {}",
        "function f() {",
        "  g();",
        "  return g.prop;",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testUnannotatedBracketAccessDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "function f(foo, i, j) {",
        "  foo.array[i][j] = 5;",
        "}"));
  }

  public void testUnknownTypeReferenceDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I(){}",
        "/** @type {function(NonExistentClass)} */",
        "I.prototype.method;"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);
  }

  public void testSpecializingTypeVarDoesntGoToBottom() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        "  * @template T",
        "  * @param {T} x",
        "  */",
        "function f(x) {",
        "   if (typeof x === 'string') {",
        "     return x.length;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        "  * @template T",
        "  * @param {T} x",
        "  */",
        "function f(x) {",
        "   if (typeof x === 'string') {",
        "     return x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        "  * @template T",
        "  * @param {T} x",
        "  */",
        "function f(x) {",
        "   if (typeof x === 'string') {",
        "     return x - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        "  * @template T",
        "  * @param {T} x",
        "  */",
        "function f(x) {",
        "   if (typeof x === 'number') {",
        "     (function(/** string */ y){})(x);",
        "  }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testBottomPropAccessDoesntCrash() {
    // TODO(blickly): This warning is not very good.
    typeCheck(Joiner.on('\n').join(
        "var obj = null;",
        "if (obj) obj.prop += 7;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype.m = function(/** number */ x) {};",
        "/** @constructor */",
        "function Bar() {}",
        "Bar.prototype.m = function(/** string */ x) {};",
        "function f(/** null|!Foo|!Bar */ x, y) {",
        "  if (x) {",
        "    return x.m(y);",
        "  }",
        "}"),
        NewTypeInference.BOTTOM_PROP);
  }

  public void testUnannotatedFunctionSummaryDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "var /** !Promise */ p;",
        "function f(unused) {",
        "  function g(){ return 5; }",
        "  p.then(g);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "var /** !Promise */ p;",
        "function f(unused) {",
        "  function g(){ return 5; }",
        "  var /** null */ n = p.then(g);",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testSpecializeLooseNullDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "function reqFoo(/** Foo */ x) {}",
        "function f(x) {",
        "   x = null;",
        "   reqFoo(x);",
        "}"));
  }

  public void testOuterVarDefinitionJoinDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "function f() {",
        "  if (true) {",
        "    function g() { new Foo; }",
        "    g();",
        "  }",
        "}"));

    // typeCheck(Joiner.on('\n').join(
    //     "function f() {",
    //     "  if (true) {",
    //     "    function g() { new Foo; }",
    //     "    g();",
    //     "  }",
    //     "}"),
    //     VarCheck.UNDEFINED_VAR_ERROR);
  }

  public void testUnparameterizedArrayDefinitionDoesntCrash() {
    typeCheckCustomExterns(
        DEFAULT_EXTERNS + Joiner.on('\n').join(
            "/** @constructor */ function Function(){}",
            "/** @constructor */ function Array(){}"),
        Joiner.on('\n').join(
            "function f(/** !Array */ arr) {",
            "  var newarr = [];",
            "  newarr[0] = arr[0];",
            "  newarr[0].prop1 = newarr[0].prop2;",
            "};"));
  }

  public void testInstanceofGenericTypeDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template T */ function Foo(){}",
        "function f(/** !Foo<?> */ f) {",
        "  if (f instanceof Foo) return true;",
        "};"));
  }

  public void testRedeclarationOfFunctionAsNamespaceDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = ns || {};",
        "ns.fun = function(name) {};",
        "ns.fun = ns.fun || {};",
        "ns.fun.get = function(/** string */ name) {};"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = ns || {};",
        "ns.fun = function(name) {};",
        "ns.fun.get = function(/** string */ name) {};",
        "ns.fun = ns.fun || {};"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testInvalidEnumDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {Array<number>} */",
        "var FooEnum = {",
        "  BAR: [5]",
        "};",
        "/** @param {FooEnum} x */",
        "function f(x) {",
        "    var y = x[0];",
        "};"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "var ns = {};",
        "function f() {",
        "  /** @enum {number} */ var EnumType = ns;",
        "}"),
        GlobalTypeInfo.MALFORMED_ENUM);
  }

  public void testRemoveNonexistentPropDoesntCrash() {
    // TODO(blickly): Would be nice not to warn here,
    // even if it means missing the warning below
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {",
        " /** @type {!Object} */ this.obj = {arr : []}",
        "}",
        "Foo.prototype.bar = function() {",
        " this.obj.arr.length = 0;",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {",
        " /** @type {!Object} */ this.obj = {}",
        "}",
        "Foo.prototype.bar = function() {",
        " this.obj.prop1.prop2 = 0;",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);
  }

  public void testDoublyAssignedPrototypeMethodDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "Foo.prototype.method = function(){};",
        "var f = function() {",
        "   Foo.prototype.method = function(){};",
        "}"),
        GlobalTypeInfo.CTOR_IN_DIFFERENT_SCOPE);
  }

  public void testTopFunctionAsArgumentDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "function f(x) {}",
        "function g(value) {",
        "  if (typeof value == 'function') {",
        "    f(value);",
        "  }",
        "}"));
  }

  public void testGetpropDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Obj(){}",
        "/** @constructor */ var Foo = function() {",
        "    /** @private {Obj} */ this.obj;",
        "};",
        "Foo.prototype.update = function() {",
        "    if (!this.obj) {}",
        "};"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Obj(){}",
        "/** @constructor */",
        "var Foo = function() {",
        "  /** @private {Obj} */ this.obj;",
        "};",
        "Foo.prototype.update = function() {",
        "    if (!this.obj.size) {}",
        "};"),
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Obj(){}",
        "/** @constructor */",
        "var Foo = function() {",
        "  /** @private {Obj} */ this.obj;",
        "};",
        "/** @param {!Foo} x */",
        "function f(x) {",
        "  if (!x.obj.size) {}",
        "};"),
        NewTypeInference.NULLABLE_DEREFERENCE);
  }

  public void testLooseFunctionSubtypeDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "var Foo = function() {};",
        "/** @param {function(!Foo)} fooFun */",
        "var reqFooFun = function(fooFun) {};",
        "/** @type {function(!Foo)} */",
        "var declaredFooFun;",
        "function f(opt_fooFun) {",
        "  reqFooFun(opt_fooFun);",
        "  var fooFun = opt_fooFun || declaredFooFun;",
        "  reqFooFun(fooFun);",
        "};"));

    typeCheck(Joiner.on('\n').join(
        "var /** @type {function(number)} */ f;",
        "f = (function(x) {",
        "  x(1, 2);",
        "  return x;",
        "})(function(x, y) {});"));
  }

  public void testMeetOfLooseObjAndNamedDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){ this.prop = 5; }",
        "/** @constructor */ function Bar(){}",
        "/** @param {function(!Foo)} func */",
        "Bar.prototype.forEach = function(func) {",
        "  this.forEach(function(looseObj) { looseObj.prop; });",
        "};"));
  }

  // public void testAccessVarargsDoesntCrash() {
  //   // TODO(blickly): Support arguments so we only get one warning
  //   typeCheck(Joiner.on('\n').join(
  //       "/** @param {...} var_args */",
  //       "function f(var_args) { return true ? var_args : arguments; }"),
  //       VarCheck.UNDEFINED_VAR_ERROR,
  //       VarCheck.UNDEFINED_VAR_ERROR);
  // }

  public void testUninhabitableObjectTypeDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ n) {",
        "  if (typeof n == 'string') {",
        "    return { 'First': n, 'Second': 5 };",
        "  }",
        "};"));
  }

  public void testMockedOutConstructorDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "/** @constructor */ Foo = function(){};"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testNamespacePropWithNoTypeDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @public */ ns.prop;"));
  }

  public void testArrayLiteralUsedGenericallyDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {!Array<T>} arr",
        " * @return {T}",
        " */",
        "function f(arr) { return arr[0]; }",
        "f([1,2,3]);"));
  }

  public void testSpecializeLooseFunctionDoesntCrash() {
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** !Function */ func) {};",
        "function g(obj) {",
        "    if (goog.isFunction(obj)) {",
        "      f(obj);",
        "    }",
        "};"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Function */ func) {};",
        "function g(obj) {",
        "    if (typeof obj === 'function') {",
        "      f(obj);",
        "    }",
        "};"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {function(T)} fn",
        " * @param {T} x",
        " * @template T",
        " */",
        "function reqGenFun(fn, x) {};",
        "function g(obj, str) {",
        "  var member = obj[str];",
        "  if (typeof member === 'function') {",
        "    reqGenFun(member, str);",
        "  }",
        "};"));
  }

  public void testUnificationWithTopFunctionDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {*} value",
        " * @param {function(new: T, ...)} type",
        " * @template T",
        " */",
        "function assertInstanceof(value, type) {}",
        "/** @const */ var ctor = unresolvedGlobalVar;",
        "function f(obj) {",
        "  if (obj instanceof ctor) {",
        "    return assertInstanceof(obj, ctor);",
        "  }",
        "}"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE,
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testGetpropOnPossiblyInexistentPropertyDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){};",
        "function f() {",
        "  var obj = 3 ? new Foo : { prop : { subprop : 'str'}};",
        "  obj.prop.subprop = 'str';",
        "};"),
        NewTypeInference.POSSIBLY_INEXISTENT_PROPERTY);
  }

  public void testCtorManipulationDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ var X = function() {};",
        "var f = function(ctor) {",
        "  /** @type {function(new: X)} */",
        "  function InstantiableCtor() {};",
        "  InstantiableCtor.prototype = ctor.prototype;",
        "}"));
  }

  public void testAbstractMethodOverrides() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var goog = {};",
        "/** @type {!Function} */ goog.abstractMethod = function(){};",
        "/** @interface */ function I() {}",
        "/** @param {string=} opt_str */",
        "I.prototype.done = goog.abstractMethod;",
        "/** @implements {I} @constructor */ function Foo() {}",
        "/** @override */ Foo.prototype.done = function(opt_str) {}",
        "/** @param {I} stats */ function f(stats) {}",
        "function g() {",
        "  var x = new Foo();",
        "  f(x);",
        "  x.done();",
        "}"));
  }

  public void testThisReferenceUsedGenerically() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template T */",
        "var Foo = function(t) {",
        "  /** @type {Foo<T>} */",
        "  this.parent_ = null;",
        "}",
        "Foo.prototype.method = function() {",
        "  var p = this;",
        "  while (p != null) p = p.parent_;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template T */",
        "var Foo = function(t) {",
        "  /** @type {Foo<T>} */",
        "  var p = this;",
        "}"));
  }

  public void testGrandparentTemplatizedDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template VALUE */",
        "var Grandparent = function() {};",
        "/** @constructor @extends {Grandparent<number>} */",
        "var Parent = function(){};",
        "/** @constructor @extends {Parent} */ function Child(){}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @template VALUE */",
        "var Grandparent = function() {};",
        "/** @constructor @extends {Grandparent} */",
        "var Parent = function(){};",
        "/** @constructor @extends {Parent} */ function Child(){}"));
  }

  public void testDirectPrototypeAssignmentDoesntCrash() {
    typeCheck(Joiner.on('\n').join(
        "function UndeclaredCtor(parent) {}",
        "UndeclaredCtor.prototype = {__proto__: Object.prototype};"));
  }

  public void testDebuggerStatementDoesntCrash() {
    typeCheck("debugger;");
  }

  public void testDeclaredMethodWithoutScope() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Foo(){}",
        "/** @type {function(number)} */ Foo.prototype.bar;",
        "/** @constructor @implements {Foo} */ function Bar(){}",
        "Bar.prototype.bar = function(x){}"));

    typeCheck(Joiner.on('\n').join(
        "/** @type {!Function} */",
        "var g = function() { throw 0; };",
        "/** @constructor */ function Foo(){}",
        "/** @type {function(number)} */ Foo.prototype.bar = g;",
        "/** @constructor @extends {Foo} */ function Bar(){}",
        "Bar.prototype.bar = function(x){}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {string} s */",
        "var reqString = function(s) {};",
        "/** @constructor */ function Foo(){}",
        "/** @type {function(string)} */ Foo.prototype.bar = reqString;",
        "/** @constructor @extends {Foo} */ function Bar(){}",
        "Bar.prototype.bar = function(x){}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {string} s */",
        "var reqString = function(s) {};",
        "/** @constructor */ function Foo(){}",
        "/** @type {function(number)} */ Foo.prototype.bar = reqString;",
        "/** @constructor @extends {Foo} */ function Bar(){}",
        "Bar.prototype.bar = function(x){}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "/** @type {Function} */ Foo.prototype.bar = null;",
        "/** @constructor @extends {Foo} */ function Bar(){}",
        "Bar.prototype.bar = function(){}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){}",
        "/** @type {!Function} */ Foo.prototype.bar = null;",
        "/** @constructor @extends {Foo} */ function Bar(){}",
        "Bar.prototype.bar = function(){}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function I(){}",
        "/** @return {void} */",
        "I.prototype.method;"));
  }

  public void testDontOverrideNestedPropWithWorseType() {
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "var Bar = function() {};",
        "/** @type {Function} */",
        "Bar.prototype.method;",
        "/** @interface */",
        "var Baz = function() {};",
        "Baz.prototype.method = function() {};",
        "/** @constructor */",
        "var Foo = function() {};",
        "/** @type {!Bar|!Baz} */",
        "Foo.prototype.obj;",
        "Foo.prototype.set = function() {",
        "    this.obj.method = 5;",
        "};"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** { prop: number } */ obj, x) {",
        " x < obj.prop;",
        " obj.prop < 'str';",
        " obj.prop = 123;",
        " x = 123;",
        "}",
        "f({ prop: 123}, 123)"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testPropNamesWithDot() {
    typeCheck("var x = { '.': 1, ';': 2, '/': 3, '{': 4, '}': 5 }");

    typeCheck(Joiner.on('\n').join(
        "function f(/** { foo : { bar : string } } */ x) {",
        "  x['foo.bar'] = 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "var x = { '.' : 'str' };",
        "x['.'] - 5"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testObjLitDeclaredProps() {
    typeCheck(
        "({ /** @type {string} */ prop: 123 });",
        NewTypeInference.INVALID_OBJLIT_PROPERTY_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var lit = { /** @type {string} */ prop: 'str' };",
        "lit.prop = 123;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var lit = { /** @type {(number|string)} */ prop: 'str' };",
        "var /** string */ s = lit.prop;"));
  }

  public void testCallArgumentsChecked() {
    typeCheck(
        "3(1 - 'str');",
        TypeCheck.NOT_CALLABLE,
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testRecursiveFunctions() {
    typeCheck(
        "function foo(){ foo() - 123; return 'str'; }",
        NewTypeInference.INVALID_INFERRED_RETURN_TYPE);

    typeCheck(
        "/** @return {string} */ function foo(){ foo() - 123; return 'str'; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {number} */",
        "var f = function rec() { return rec; };"),
        NewTypeInference.RETURN_NONDECLARED_TYPE);
  }

  public void testStructPropAccess() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() { this.prop = 123; }",
        "(new Foo).prop;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() { this.prop = 123; }",
        "(new Foo)['prop'];"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */ function Foo() {}",
        "/** @type {number} */ Foo.prototype.prop;",
        "function f(/** !Foo */ x) { x['prop']; }"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() {",
        "  this.prop = 123;",
        "  this['prop'] - 123;",
        "}"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() { this.prop = 123; }",
        "(new Foo)['prop'] = 123;"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() { this.prop = 123; }",
        "function f(pname) { (new Foo)[pname] = 123; }"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() { this.prop = {}; }",
        "(new Foo)['prop'].newprop = 123;"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "function f(cond) {",
        "  var x;",
        "  if (cond) {",
        "    x = new Foo;",
        "  }",
        "  else {",
        "    x = new Bar;",
        "  }",
        "  x['prop'] = 123;",
        "}"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck("(/** @struct */ { 'prop' : 1 });", TypeCheck.ILLEGAL_OBJLIT_KEY);

    typeCheck(
        "var lit = /** @struct */ { prop : 1 }; lit['prop'];",
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "function f(cond) {",
        "  var x;",
        "  if (cond) {",
        "    x = /** @struct */ { a: 1 };",
        "  }",
        "  else {",
        "    x = /** @struct */ { a: 2 };",
        "  }",
        "  x['a'] = 123;",
        "}"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "function f(cond) {",
        "  var x;",
        "  if (cond) {",
        "    x = /** @struct */ { a: 1 };",
        "  }",
        "  else {",
        "    x = {};",
        "  }",
        "  x['random' + 'propname'] = 123;",
        "}"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @struct",
        " */",
        "function Foo() {",
        "  /** @type {number} */",
        "  this.prop;",
        "  this.prop = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @struct",
        " */",
        "function Foo() {",
        "  /** @type {number} */",
        "  this.prop;",
        "}",
        "(new Foo).prop = 'asdf';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testDictPropAccess() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */ function Foo() { this['prop'] = 123; }",
        "(new Foo)['prop'];"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */ function Foo() { this['prop'] = 123; }",
        "(new Foo).prop;"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */ function Foo() {",
        "  this['prop'] = 123;",
        "  this.prop - 123;",
        "}"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */ function Foo() { this['prop'] = 123; }",
        "(new Foo).prop = 123;"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */ function Foo() { this['prop'] = {}; }",
        "(new Foo).prop.newprop = 123;"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */ function Foo() {}",
        "/** @constructor */ function Bar() {}",
        "function f(cond) {",
        "  var x;",
        "  if (cond) {",
        "    x = new Foo;",
        "  }",
        "  else {",
        "    x = new Bar;",
        "  }",
        "  x.prop = 123;",
        "}"),
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck("(/** @dict */ { prop : 1 });", TypeCheck.ILLEGAL_OBJLIT_KEY);

    typeCheck(
        "var lit = /** @dict */ { 'prop' : 1 }; lit.prop;",
        TypeValidator.ILLEGAL_PROPERTY_ACCESS);

    typeCheck(
        "(/** @dict */ {}).toString();", TypeValidator.ILLEGAL_PROPERTY_ACCESS);
  }

  public void testStructWithIn() {
    typeCheck("('prop' in /** @struct */ {});", TypeCheck.IN_USED_WITH_STRUCT);

    typeCheck(
        "for (var x in /** @struct */ {});", TypeCheck.IN_USED_WITH_STRUCT);
  }

  public void testStructDictSubtyping() {
    typeCheck("var lit = { a: 1 }; lit.a - 2; lit['a'] + 5;");

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */ function Foo() {}",
        "/** @constructor @dict */ function Bar() {}",
        "function f(/** Foo */ x) {}",
        "f(/** @dict */ {});"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** { a : number } */ x) {}",
        "f(/** @dict */ { 'a' : 5 });"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testInferStructDictFormal() {
    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  return obj.prop;",
        "}",
        "f(/** @dict */ { 'prop': 123 });"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  return obj['prop'];",
        "}",
        "f(/** @struct */ { prop: 123 });"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "function f(obj) { obj['prop']; return obj; }",
        "var /** !Foo */ x = f({ prop: 123 });"));

    // We infer unrestricted loose obj for mixed . and [] access
    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  if (obj['p1']) {",
        "    obj.p2.p3 = 123;",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(obj) {",
        "  return obj['a'] + obj.b;",
        "}",
        "f({ a: 123, 'b': 234 });"));
  }

  public void testStructDictInheritance() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "/** @constructor @struct @extends {Foo} */",
        "function Bar() {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "/** @constructor @unrestricted @extends {Foo} */",
        "function Bar() {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */",
        "function Foo() {}",
        "/** @constructor @dict @extends {Foo} */",
        "function Bar() {}"));

    // TODO(dimvar): remove conflicting shape type warning
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @unrestricted */",
        "function Foo() {}",
        "/** @constructor @struct @extends {Foo} */",
        "function Bar() {}"),
        JSTypeCreatorFromJSDoc.CONFLICTING_SHAPE_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @unrestricted */",
        "function Foo() {}",
        "/** @constructor @dict @extends {Foo} */",
        "function Bar() {}"),
        JSTypeCreatorFromJSDoc.CONFLICTING_SHAPE_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Foo() {}",
        "/** @constructor @dict @implements {Foo} */",
        "function Bar() {}"),
        JSTypeCreatorFromJSDoc.DICT_IMPLEMENTS_INTERF);
  }

  public void testStructPropCreation() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() { this.prop = 1; }",
        "(new Foo).prop = 2;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "Foo.prototype.method = function() { this.prop = 1; };"),
        TypeCheck.ILLEGAL_PROPERTY_CREATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "Foo.prototype.method = function() { this.prop = 1; };",
        "(new Foo).prop = 2;"),
        TypeCheck.ILLEGAL_PROPERTY_CREATION,
        TypeCheck.ILLEGAL_PROPERTY_CREATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "(new Foo).prop += 2;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "Foo.prototype.method = function() { this.prop = 1; };",
        "(new Foo).prop++;"),
        TypeCheck.ILLEGAL_PROPERTY_CREATION,
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(
        "(/** @struct */ { prop: 1 }).prop2 = 123;",
        TypeCheck.ILLEGAL_PROPERTY_CREATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "/** @constructor @struct @extends {Foo} */",
        "function Bar() {}",
        "Bar.prototype.prop = 123;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "/** @constructor @struct @extends {Foo} */",
        "function Bar() {}",
        "Bar.prototype.prop = 123;",
        "(new Foo).prop = 234;"),
        TypeCheck.ILLEGAL_PROPERTY_CREATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @struct",
        " */",
        "function Foo() {",
        "  var t = this;",
        "  t.x = 123;",
        "}"),
        TypeCheck.ILLEGAL_PROPERTY_CREATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @struct",
        " */",
        "function Foo() {}",
        "Foo.someprop = 123;"));

    // TODO(dimvar): the current type inf also doesn't catch this.
    // Consider warning when the prop is not an "own" prop.
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "Foo.prototype.bar = 123;",
        "(new Foo).bar = 123;"));

    typeCheck(Joiner.on('\n').join(
        "function f(obj) { obj.prop = 123; }",
        "f(/** @struct */ {});"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @struct */",
        "function Foo() {}",
        "function f(obj) { obj.prop - 5; return obj; }",
        "var s = (1 < 2) ? new Foo : f({ prop: 123 });",
        "s.newprop = 123;"),
        TypeCheck.ILLEGAL_PROPERTY_CREATION);
  }

  public void testMisplacedStructDictAnnotation() {
    typeCheck(
        "/** @struct */ function Struct1() {}",
        GlobalTypeInfo.STRUCTDICT_WITHOUT_CTOR);
    typeCheck(
        "/** @dict */ function Dict() {}",
        GlobalTypeInfo.STRUCTDICT_WITHOUT_CTOR);
  }

  // public void testGlobalVariableInJoin() {
  //   typeCheck(
  //       "function f() { true ? globalVariable : 123; }",
  //       VarCheck.UNDEFINED_VAR_ERROR);
  // }

  // public void testGlobalVariableInAssign() {
  //   typeCheck(
  //       "u.prop = 123;",
  //       VarCheck.UNDEFINED_VAR_ERROR);
  // }

  public void testGetters() {
    typeCheck(
        "var x = { /** @return {string} */ get a() { return 1; } };",
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    typeCheck(
        "var x = { /** @param {number} n */ get a() {} };",
        GlobalTypeInfo.INEXISTENT_PARAM);

    typeCheck(
        "var x = { /** @type {string} */ get a() {} };",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "var x = {",
        "  /**",
        "   * @return {T|number} b",
        "   * @template T",
        "   */",
        "  get a() {}",
        "};"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "var x = /** @dict */ { get a() {} };", TypeCheck.ILLEGAL_OBJLIT_KEY);

    typeCheck(
        "var x = /** @struct */ { get 'a'() {} };",
        TypeCheck.ILLEGAL_OBJLIT_KEY);

    typeCheck(
        "var x = { get a() { 1 - 'asdf'; } };",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var x = { get a() { return 1; } };",
        "x.a < 'str';"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var x = { get a() { return 1; } };",
        "x.a();"),
        TypeCheck.NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "var x = { get 'a'() { return 1; } };",
        "x['a']();"),
        TypeCheck.NOT_CALLABLE);

    // assigning to a getter doesn't remove it
    typeCheck(Joiner.on('\n').join(
        "var x = { get a() { return 1; } };",
        "x.a = 'str';",
        "x.a - 1;"));

    typeCheck(
        "var x = /** @struct */ { get a() {} }; x.a = 123;",
        TypeCheck.ILLEGAL_PROPERTY_CREATION);
  }

  public void testSetters() {
    typeCheck(
        "var x = { /** @return {string} */ set a(b) {} };",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "var x = { /** @type{function(number):number} */ set a(b) {} };",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "var x = { set /** string */ a(b) {} };",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "var x = {",
        "  /**",
        "   * @param {T|number} b",
        "   * @template T",
        "   */",
        "  set a(b) {}",
        "};"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "var x = { set a(b) { return 1; } };",
        NewTypeInference.RETURN_NONDECLARED_TYPE);

    typeCheck(
        "var x = { /** @type {string} */ set a(b) {} };",
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(
        "var x = /** @dict */ { set a(b) {} };", TypeCheck.ILLEGAL_OBJLIT_KEY);

    typeCheck(
        "var x = /** @struct */ { set 'a'(b) {} };",
        TypeCheck.ILLEGAL_OBJLIT_KEY);

    typeCheck(
        "var x = { set a(b) { 1 - 'asdf'; } };",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(
        "var x = { set a(b) {}, prop: 123 }; var y = x.a;",
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "var x = { /** @param {string} b */ set a(b) {} };",
        "x.a = 123;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var x = { set a(b) { b - 5; } };",
        "x.a = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var x = { set 'a'(b) { b - 5; } };",
        "x['a'] = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testConstMissingInitializer() {
    typeCheck("/** @const */ var x;", GlobalTypeInfo.CONST_WITHOUT_INITIALIZER);

    typeCheck("/** @final */ var x;", GlobalTypeInfo.CONST_WITHOUT_INITIALIZER);

    typeCheckCustomExterns(DEFAULT_EXTERNS + "/** @const {number} */ var x;", "");

    // TODO(dimvar): must fix externs initialization
    // typeCheck("/** @const {number} */ var x;", "x - 5;");

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @const */",
        "Foo.prop;"),
        GlobalTypeInfo.CONST_WITHOUT_INITIALIZER);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */ this.prop;",
        "}"),
        GlobalTypeInfo.CONST_WITHOUT_INITIALIZER);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @const */",
        "Foo.prototype.prop;"),
        GlobalTypeInfo.CONST_WITHOUT_INITIALIZER);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.prop;"),
        GlobalTypeInfo.CONST_WITHOUT_INITIALIZER);
  }

  public void testMisplacedConstPropertyAnnotation() {
    typeCheck(
        "function f(obj) { /** @const */ obj.prop = 123; }",
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);

    typeCheck(
        "function f(obj) { /** @const */ obj.prop; }",
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);

    typeCheck(
        "var obj = { /** @const */ prop: 1 };",
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "Foo.prototype.method = function() {",
        "  /** @const */ this.prop = 1;",
        "}"),
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);

    // A final constructor isn't the same as a @const property
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /**",
        "   * @constructor",
        "   * @final",
        "   */",
        "  this.Bar = function() {};",
        "}"));
  }

  public void testConstVarsDontReassign() {
    typeCheck(
        "/** @const */ var x = 1; x = 2;", NewTypeInference.CONST_REASSIGNED);

    typeCheck(
        "/** @const */ var x = 1; x += 2;", NewTypeInference.CONST_REASSIGNED);

    typeCheck(
        "/** @const */ var x = 1; x -= 2;", NewTypeInference.CONST_REASSIGNED);

    typeCheck(
        "/** @const */ var x = 1; x++;", NewTypeInference.CONST_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var x = 1;",
        "function f() { x = 2; }"),
        NewTypeInference.CONST_REASSIGNED);

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "/** @const {number} */ var x;", "x = 2;",
        NewTypeInference.CONST_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var x = 1;",
        "function g() {",
        "  var x = 2;",
        "  x = x + 3;",
        "}"));
  }

  public void testConstPropertiesDontReassign() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */ this.prop = 1;",
        "}",
        "var obj = new Foo;",
        "obj.prop = 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const {number} */",
        "  this.prop = 1;",
        "}",
        "var obj = new Foo;",
        "obj.prop = 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */ this.prop = 1;",
        "}",
        "var obj = new Foo;",
        "obj.prop += 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */ this.prop = 1;",
        "}",
        "var obj = new Foo;",
        "obj.prop++;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.prop = 1;",
        "ns.prop = 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.prop = 1;",
        "function f() { ns.prop = 2; }"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const {number} */",
        "ns.prop = 1;",
        "ns.prop = 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.prop = 1;",
        "ns.prop++;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @const */ Foo.prop = 1;",
        "Foo.prop = 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @const {number} */ Foo.prop = 1;",
        "Foo.prop++;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @const */ Foo.prototype.prop = 1;",
        "Foo.prototype.prop = 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @const */ Foo.prototype.prop = 1;",
        "var protoAlias = Foo.prototype;",
        "protoAlias.prop = 2;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @const */ this.X = 4; }",
        "/** @constructor */",
        "function Bar() { /** @const */ this.X = 5; }",
        "var fb = true ? new Foo : new Bar;",
        "fb.X++;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);
  }

  public void testConstantByConvention() {
    typeCheck(Joiner.on('\n').join(
        "var ABC = 123;",
        "ABC = 321;"),
        NewTypeInference.CONST_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  this.ABC = 123;",
        "}",
        "(new Foo).ABC = 321;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);
  }

  public void testDontOverrideFinalMethods() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @final */",
        "Foo.prototype.method = function(x) {};",
        "/** @constructor @extends {Foo} */",
        "function Bar() {}",
        "Bar.prototype.method = function(x) {};"),
        GlobalTypeInfo.CANNOT_OVERRIDE_FINAL_METHOD);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @final */",
        "Foo.prototype.num = 123;",
        "/** @constructor @extends {Foo} */",
        "function Bar() {}",
        "Bar.prototype.num = 2;"));

    // // TODO(dimvar): fix
    // typeCheck(Joiner.on('\n').join(
    //     "/** @constructor */",
    //     "function High() {}",
    //     "/**",
    //     " * @param {number} x",
    //     " * @final",
    //     " */",
    //     "High.prototype.method = function(x) {};",
    //     "/** @constructor @extends {High} */",
    //     "function Mid() {}",
    //     "/** @constructor @extends {Mid} */",
    //     "function Low() {}",
    //     "Low.prototype.method = function(x) {};"),
    //     GlobalTypeInfo.CANNOT_OVERRIDE_FINAL_METHOD);
  }

  public void testInferenceOfConstType() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var s = 'str';",
        "function f() { s - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** string */ x) {",
        "  /** @const */",
        "  var s = x;",
        "  function g() { s - 5; }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function RegExp(){}",
        "/** @const */",
        "var r = /find/;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function RegExp(){}",
        "/** @const */",
        "var r = /find/;",
        "function g() { r - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var a = [5];"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var a = [5];",
        "function g() { a - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var x;",
        "/** @const */ var o = x = {};",
        "function g() { return o.prop; }"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var o = (0,{});",
        "function g() { return o.prop; }"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var s = true ? null : null;",
        "function g() { s - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var s = true ? void 0 : undefined;",
        "function g() { s - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var b = true ? (1<2) : ('' in {});",
        "function g() { b - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var n = 0 || 6;",
        "function g() { n < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var s = 'str' + 5;",
        "function g() { s - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    // TODO(dimvar): must fix externs initialization
    // typeCheck(Joiner.on('\n').join(
    //     "var /** string */ x;",
    //     "/** @const */",
    //     "var s = x;",
    //     "function g() { s - 5; }"),
    //     NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */",
        "  this.prop = 'str';",
        "}",
        "(new Foo).prop - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @const */",
        "Foo.prop = 'str';",
        "function g() { Foo.prop - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** string */ s) {",
        "  /** @constructor */",
        "  function Foo() {}",
        "  /** @const */",
        "  Foo.prototype.prop = s;",
        "  function g() {",
        "    (new Foo).prop - 5;",
        "  }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.prop = 'str';",
        "function f() {",
        "  ns.prop - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) {",
        "  /** @const */",
        "  var n = x - y;",
        "  function g() { n < 'str'; }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  /** @const */",
        "  var notx = !x;",
        "  function g() { notx - 5; }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var lit = { a: 'a', b: 'b' };",
        "function g() { lit.a - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var n = ('str', 123);",
        "function f() { n < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var s = x;",
        "var /** string */ x;"),
        // VariableReferenceCheck.EARLY_REFERENCE,
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  /** @const */",
        "  var c = x;",
        "}"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  /** @const */",
        "  var c = { a: 1, b: x };",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @param {{ a: string }} x",
        " */",
        "function Foo(x) {",
        "  /** @const */",
        "  this.prop = x.a;",
        "}",
        "(new Foo({ a: ''})).prop - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @return {string} */",
        "function f() { return ''; }",
        "/** @const */",
        "var s = f();",
        "function g() { s - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var s = f();",
        "/** @return {string} */",
        "function f() { return ''; }"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}",
        "/** @const */",
        "var foo = new Foo;",
        "function g() {",
        "  var /** Bar */ bar = foo;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var n1 = 1;",
        "/** @const */",
        "var n2 = n1;",
        "function g() { n2 < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    // Don't treat aliased constructors as if they were const variables.
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Bar() {}",
        "/**",
        " * @constructor",
        " * @final",
        " */",
        "var Foo = Bar;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Bar() {}",
        "/** @const */",
        "var ns = {};",
        "/**",
        " * @constructor",
        " * @final",
        " */",
        "ns.Foo = Bar;"));

    // (Counterintuitive) On a constructor, @final means don't subclass, not
    // that it's a const. We don't warn about reassignment.
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @final",
        " */",
        "var Foo = function() {};",
        "Foo = /** @type {?} */ (function() {});"));
  }

  public void testSuppressions() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @fileoverview",
        " * @suppress {newCheckTypes}",
        " */",
        "123();"));

    typeCheck(Joiner.on('\n').join(
        "123();",
        "/** @suppress {newCheckTypes} */",
        "function f() { 123(); }"),
        TypeCheck.NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "123();",
        "/** @suppress {newCheckTypes} */",
        "function f() { 1 - 'str'; }"),
        TypeCheck.NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @type {Object} */",
        "ns.obj = { prop: 123 };",
        "/**",
        " * @suppress {duplicate}",
        " * @type {Object}",
        " */",
        "ns.obj = null;"));

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  /** @const */",
        "  var ns = {};",
        "  /** @type {number} */",
        "  ns.prop = 1;",
        "  /**",
        "   * @type {number}",
        "   * @suppress {duplicate}",
        "   */",
        "  ns.prop = 2;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @suppress {constantProperty}",
        " */",
        "function Foo() {",
        "  /** @const */ this.bar = 3; this.bar += 4;",
        "}"));
  }

  public void testTypedefs() {
    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number} */",
        "var num = 1;"),
        GlobalTypeInfo.CANNOT_INIT_TYPEDEF);

    // typeCheck(Joiner.on('\n').join(
    //     "/** @typedef {number} */",
    //     "var num;",
    //     "num - 5;"),
    //     VarCheck.UNDEFINED_VAR_ERROR);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {NonExistentType} */",
        "var t;",
        "function f(/** t */ x) { x - 1; }"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    // typeCheck(Joiner.on('\n').join(
    //     "/** @typedef {number} */",
    //     "var dup;",
    //     "/** @typedef {number} */",
    //     "var dup;"),
    //     VariableReferenceCheck.REDECLARED_VARIABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number} */",
        "var dup;",
        "/** @typedef {string} */",
        "var dup;",
        "var /** dup */ n = 'str';"),
        // VariableReferenceCheck.REDECLARED_VARIABLE,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number} */",
        "var num;",
        "/** @type {num} */",
        "var n = 1;"));

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number} */",
        "var num;",
        "/** @type {num} */",
        "var n = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @type {num} */",
        "var n = 'str';",
        "/** @typedef {number} */",
        "var num;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number} */",
        "var num;",
        "function f() {",
        "  /** @type {num} */",
        "  var n = 'str';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @type {num2} */",
        "var n = 'str';",
        "/** @typedef {num} */",
        "var num2;",
        "/** @typedef {number} */",
        "var num;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {rec2} */",
        "var rec1;",
        "/** @typedef {rec1} */",
        "var rec2;"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {{ prop: rec2 }} */",
        "var rec1;",
        "/** @typedef {{ prop: rec1 }} */",
        "var rec2;"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @typedef {Foo} */",
        "var Bar;",
        "var /** Bar */ x = null;"));

    // NOTE(dimvar): I don't know if long term we want to support ! on anything
    // other than a nominal-type name, but for now it's good to have this test.
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @typedef {Foo} */",
        "var Bar;",
        "var /** !Bar */ x = null;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number} */",
        "var N;",
        "function f() {",
        "  /** @constructor */",
        "  function N() {}",
        "  function g(/** N */ obj) { obj - 5; }",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testLends() {
    typeCheck(
        "(/** @lends {InexistentType} */ { a: 1 });",
        GlobalTypeInfo.LENDS_ON_BAD_TYPE);

    typeCheck(
        "(/** @lends {number} */ { a: 1 });", GlobalTypeInfo.LENDS_ON_BAD_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "(/** @lends {Foo.badname} */ { a: 1 });"),
        GlobalTypeInfo.LENDS_ON_BAD_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Foo() {}",
        "(/** @lends {Foo} */ { a: 1 });"),
        GlobalTypeInfo.LENDS_ON_BAD_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Foo() {}",
        "(/** @lends {Foo.prototype} */ { a: 1 });"),
        GlobalTypeInfo.LENDS_ON_BAD_TYPE);

    typeCheck(
        "(/** @lends {Inexistent.Type} */ { a: 1 });",
        GlobalTypeInfo.LENDS_ON_BAD_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "(/** @lends {ns} */ { /** @type {number} */ prop : 1 });",
        "function f() { ns.prop = 'str'; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "(/** @lends {ns} */ { /** @type {number} */ prop : 1 });",
        "/** @const */ var ns = {};",
        "function f() { ns.prop = 'str'; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "(/** @lends {ns} */ { prop : 1 });",
        "function f() { var /** string */ s = ns.prop; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.subns = {};",
        "(/** @lends {ns.subns} */ { prop: 1 });",
        "var /** string */ s = ns.subns.prop;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "(/** @lends {Foo} */ { prop: 1 });",
        "var /** string */ s = Foo.prop;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "(/** @lends {Foo} */ { prop: 1 });",
        "function f() { var /** string */ s = Foo.prop; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @constructor */",
        "ns.Foo = function() {};",
        "(/** @lends {ns.Foo} */ { prop: 1 });",
        "function f() { var /** string */ s = ns.Foo.prop; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "(/** @lends {Foo.prototype} */ { /** @type {number} */ a: 1 });",
        "var /** string */ s = Foo.prototype.a;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "(/** @lends {Foo.prototype} */ { /** @type {number} */ a: 1 });",
        "var /** string */ s = (new Foo).a;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {number} */ this.prop = 1; }",
        "(/** @lends {Foo.prototype} */",
        " { /** @return {number} */ m: function() { return this.prop; } });",
        "var /** string */ s = (new Foo).m();"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testEnumBasicTyping() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "function f(/** E */ x) { x < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum */",
        // No type annotation defaults to number
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "function f(/** E */ x) { x < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "function f(/** E */ x) {}",
        "function g(/** number */ x) {}",
        "f(E.TWO);",
        "g(E.TWO);"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "function f(/** E */ x) {}",
        "f(1);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "function f() { E.THREE - 5; }"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {!Foo} */",
        "var E = { ONE: new Foo };",
        "/** @constructor */",
        "function Foo() {}"));

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number} */",
        "var num;",
        "/** @enum {num} */",
        "var E = { ONE: 1 };",
        "function f(/** E */ x) { x < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testEnumsWithNonScalarDeclaredType() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {!Object} */ var E = {FOO: { prop: 1 }};",
        "E.FOO.prop - 5;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {{prop: number}} */ var E = {FOO: { prop: 1 }};",
        "E.FOO.prop - 5;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */ this.prop = 1;",
        "}",
        "/** @enum {!Foo} */",
        "var E = { ONE: new Foo() };",
        "function f(/** E */ x) { x.prop < 'str'; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */ this.prop = 1;",
        "}",
        "/** @enum {!Foo} */",
        "var E = { ONE: new Foo() };",
        "function f(/** E */ x) { x.prop = 2; }"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @enum {!Foo} */",
        "var E = { A: new Foo };",
        "function f(/** E */ x) { x instanceof Foo; }"));
  }

  public void testEnumBadInitializer() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E;"),
        GlobalTypeInfo.MALFORMED_ENUM);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {};"),
        GlobalTypeInfo.MALFORMED_ENUM);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = 1;"),
        GlobalTypeInfo.MALFORMED_ENUM);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: true",
        "};"),
        NewTypeInference.INVALID_OBJLIT_PROPERTY_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = { a: 1 };"),
        TypeCheck.ENUM_NOT_CONSTANT);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = { A: 1, A: 2 };"),
        GlobalTypeInfo.DUPLICATE_PROP_IN_ENUM);
  }

  public void testEnumPropertiesConstant() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "E.THREE = 3;"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "E.ONE = E.TWO;"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = {",
        "  ONE: 1,",
        "  TWO: 2",
        "};",
        "function f(/** E */) { E.ONE = E.TWO; }"),
        NewTypeInference.CONST_PROPERTY_REASSIGNED);
  }

  public void testEnumIllegalRecursion() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {Type2} */",
        "var Type1 = {",
        "  ONE: null",
        "};",
        "/** @enum {Type1} */",
        "var Type2 = {",
        "  ONE: null",
        "};"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION,
        // This warning is a side-effect of the fact that, when there is a
        // cycle, the resolution of one enum will fail but the others will
        // complete successfully.
        NewTypeInference.INVALID_OBJLIT_PROPERTY_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {Type2} */",
        "var Type1 = {",
        "  ONE: null",
        "};",
        "/** @typedef {Type1} */",
        "var Type2;"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);
  }

  public void testEnumBadDeclaredType() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {InexistentType} */",
        "var E = { ONE : null };"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {*} */",
        "var E = { ONE: 1, STR: '' };"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    // No free type variables in enums
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  /** @enum {function(T):number} */",
        "  var E = { ONE: x };",
        "}"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  /** @enum {T} */",
        "  var E1 = { ONE: 1 };",
        "  /** @enum {function(E1):E1} */",
        "  var E2 = { ONE: function(x) { return x; } };",
        "}"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " */",
        "function f(x) {",
        "  /** @typedef {T} */ var AliasT;",
        "  /** @enum {T} */",
        "  var E1 = { ONE: 1 };",
        "  /** @enum {function(E1):T} */",
        "  var E2 = { ONE: function(x) { return x; } };",
        "}"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME,
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME,
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    // No unions in enums
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number|string} */",
        "var E = { ONE: 1, STR: '' };"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @enum {Foo} */",
        "var E = { ONE: new Foo, TWO: null };"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @typedef {number|string} */",
        "var NOS;",
        "/** @enum {NOS} */",
        "var E = { ONE: 1, STR: '' };"),
        RhinoErrorReporter.BAD_JSDOC_ANNOTATION);
  }

  public void testEnumsWithGenerics() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum */ var E1 = { A: 1};",
        "/**",
        " * @template T",
        " * @param {(T|E1)} x",
        " * @return {(T|E1)}",
        " */",
        "function f(x) { return x; }",
        "var /** string */ n = f('str');"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @enum */ var E1 = { A: 1};",
        "/** @enum */ var E2 = { A: 2};",
        "/**",
        " * @template T",
        " * @param {(T|E1)} x",
        " * @return {(T|E1)}",
        " */",
        "function f(x) { return x; }",
        "var /** (E2|string) */ x = f('str');"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @enum */",
        "var E = { A: 1 };",
        "/**",
        " * @template T",
        " * @param {number|!Array<T>} x",
        " */",
        "function f(x) {}",
        "f(E.A);",
        "f(123);"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum {string} */",
        "var e1 = { A: '' };",
        "/** @enum {string} */",
        "var e2 = { B: '' };",
        "/**",
        " * @template T",
        " * @param {T|e1} x",
        " * @return {T}",
        " */",
        "function f(x) { return /** @type {T} */ (x); }",
        "/** @param {number|e2} x */",
        "function g(x) { f(x) - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testEnumJoinSpecializeMeet() {
    // join: enum {number} with number
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = { ONE: 1 };",
        "function f(cond) {",
        "  var x = cond ? E.ONE : 5;",
        "  x - 2;",
        "  var /** E */ y = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    // join: enum {Low} with High, to High
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "/** @enum {!Low} */",
        "var E = { A: new Low };",
        "function f(cond) {",
        "  var x = cond ? E.A : new High;",
        "  var /** High */ y = x;",
        "  var /** E */ z = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    // join: enum {High} with Low, to (enum{High}|Low)
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "/** @enum {!High} */",
        "var E = { A: new High };",
        "function f(cond) {",
        "  var x = cond ? E.A : new Low;",
        "  if (!(x instanceof Low)) { var /** E */ y = x; }",
        "}"));

    // meet: enum {?} with string, to enum {?}
    typeCheck(Joiner.on('\n').join(
        "/** @enum {?} */",
        "var E = { A: 123 };",
        "function f(x) {",
        "  var /** string */ s = x;",
        "  var /** E */ y = x;",
        "  s = x;",
        "}",
        "f('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // meet: E1|E2 with E1|E3, to E1
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E1 = { ONE: 1 };",
        "/** @enum {number} */",
        "var E2 = { TWO: 1 };",
        "/** @enum {number} */",
        "var E3 = { THREE: 1 };",
        "function f(x) {",
        "  var /** (E1|E2) */ y = x;",
        "  var /** (E1|E3) */ z = x;",
        "  var /** E1 */ w = x;",
        "}",
        "f(E2.TWO);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // meet: enum {number} with number, to enum {number}
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = { ONE: 1 };",
        "function f(x) {",
        "  var /** E */ y = x;",
        "  var /** number */ z = x;",
        "}",
        "f(123);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // meet: enum {Low} with High, to enum {Low}
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "/** @enum {!Low} */",
        "var E = { A: new Low };",
        "function f(x) {",
        "  var /** !High */ y = x;",
        "  var /** E */ z = x;",
        "}",
        "f(new High);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // meet: enum {Low} with (High1|High2), to enum {Low}
    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function High1() {}",
        "/** @interface */",
        "function High2() {}",
        "/** @constructor @implements {High1} @implements {High2} */",
        "function Low() {}",
        "/** @enum {!Low} */",
        "var E = { A: new Low };",
        "function f(x) {",
        "  var /** (!High1 | !High2) */ y = x;",
        "  var /** E */ z = x;",
        "}",
        "f(/** @type {!High1} */ (new Low));"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // meet: enum {High} with Low
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "/** @enum {!High} */",
        "var E = { A: new High };",
        "/** @param {function(E)|function(!Low)} x */",
        "function f(x) { x(123); }"),
        JSTypeCreatorFromJSDoc.UNION_IS_UNINHABITABLE);
  }

  public void testEnumAliasing() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var e1 = { A: 1 };",
        "/** @enum {number} */",
        "var e2 = e1;"));

    typeCheck(Joiner.on('\n').join(
        "var x;",
        "/** @enum {number} */",
        "var e1 = { A: 1 };",
        "/** @enum {number} */",
        "var e2 = x;"),
        GlobalTypeInfo.MALFORMED_ENUM);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns1 = {};",
        "/** @enum {number} */",
        "ns1.e1 = { A: 1 };",
        "/** @const */",
        "var ns2 = {};",
        "/** @enum {number} */",
        "ns2.e2 = ns1.e1;"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var e1 = { A: 1 };",
        "/** @enum {number} */",
        "var e2 = e1;",
        "function f(/** e2 */ x) {}",
        "f(e1.A);"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var e1 = { A: 1 };",
        "/** @enum {number} */",
        "var e2 = e1;",
        "function f(/** e2 */ x) {}",
        "f(e2.A);"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var e1 = { A: 1 };",
        "/** @enum {number} */",
        "var e2 = e1;",
        "function f(/** e2 */ x) {}",
        "f('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns1 = {};",
        "/** @enum {number} */",
        "ns1.e1 = { A: 1 };",
        "/** @const */",
        "var ns2 = {};",
        "/** @enum {number} */",
        "ns2.e2 = ns1.e1;",
        "function f(/** ns2.e2 */ x) {}",
        "f(ns1.e1.A);"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns1 = {};",
        "/** @enum {number} */",
        "ns1.e1 = { A: 1 };",
        "/** @const */",
        "var ns2 = {};",
        "/** @enum {number} */",
        "ns2.e2 = ns1.e1;",
        "function f(/** ns1.e1 */ x) {}",
        "f(ns2.e2.A);"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns1 = {};",
        "/** @enum {number} */",
        "ns1.e1 = { A: 1 };",
        "function g() {",
        "  /** @const */",
        "  var ns2 = {};",
        "  /** @enum {number} */",
        "  ns2.e2 = ns1.e1;",
        "  function f(/** ns1.e1 */ x) {}",
        "  f(ns2.e2.A);",
        "}"));
  }

  public void testNoDoubleWarnings() {
    typeCheck(
        "if ((4 - 'str') && true) { 4 + 5; }",
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("(4 - 'str') ? 5 : 6;", NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testRecordSpecializeNominalPreservesRequired() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {number|string} */ this.x = 5 };",
        "var o = true ? {x:5} : {y:'str'};",
        "if (o instanceof Foo) {",
        "  var /** {x:number} */ o2 = o;",
        "}",
        "(function(/** {x:number} */ o3){})(o);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testGoogIsPredicatesNoSpecializedContext() {
    typeCheck(CLOSURE_BASE + "goog.isNull();", TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(
        CLOSURE_BASE + "goog.isNull(1, 2, 5 - 'str');",
        TypeCheck.WRONG_ARGUMENT_COUNT,
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(CLOSURE_BASE
        + "function f(x) { var /** boolean */ b = goog.isNull(x); }");
  }

  public void testGoogIsPredicatesTrue() {
    typeCheck(CLOSURE_BASE
        + "function f(x) { if (goog.isNull(x)) { var /** undefined */ y = x; } }",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @param {number=} x */",
        "function f(x) {",
        "  if (goog.isDef(x)) {",
        "    x - 5;",
        "  }",
        "  x - 5;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {Foo=} x */",
        "function f(x) {",
        "  var /** !Foo */ y;",
        "  if (goog.isDefAndNotNull(x)) {",
        "    y = x;",
        "  }",
        "  y = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (Array<number>|number) */ x) {",
        "  var /** Array<number> */ a;",
        "  if (goog.isArray(x)) {",
        "    a = x;",
        "  }",
        "  a = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @param {null|function(number)} x */ ",
        "function f(x) {",
        "  if (goog.isFunction(x)) {",
        "    x('str');",
        "  }",
        "}"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(x) {",
        "  if (goog.isObject(x)) {",
        "    var /** null */ y = x;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  if (goog.isString(x)) {",
        "    x < 'str';",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  if (goog.isNumber(x)) {",
        "    x - 5;",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|boolean) */ x) {",
        "  if (goog.isBoolean(x)) {",
        "    var /** boolean */ b = x;",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/**",
        " * @param {number|string} x",
        " * @return {string}",
        " */",
        "function f(x) {",
        "  return goog.isString(x) && (1 < 2) ? x : 'a';",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/**",
        " * @param {*} o1",
        " * @param {*} o2",
        " * @return {boolean}",
        " */",
        "function deepEquals(o1, o2) {",
        "  if (goog.isObject(o1) && goog.isObject(o2)) {",
        "    if (o1.length != o2.length) {",
        "      return true;",
        "    }",
        "  }",
        "  return false;",
        "}"));
  }

  public void testGoogIsPredicatesFalse() {
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** Foo */ x) {",
        "  var /** !Foo */ y;",
        "  if (!goog.isNull(x)) {",
        "    y = x;",
        "  }",
        "  y = x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @param {number=} x */",
        "function f(x) {",
        "  if (!goog.isDef(x)) {",
        "    var /** undefined */ u = x;",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {Foo=} x */",
        "function f(x) {",
        "  if (!goog.isDefAndNotNull(x)) {",
        "    var /** (null|undefined) */ y = x;",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  if (!goog.isString(x)) {",
        "    x - 5;",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  if (!goog.isNumber(x)) {",
        "    x < 'str';",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|boolean) */ x) {",
        "  if (!goog.isBoolean(x)) {",
        "    x - 5;",
        "  }",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|!Array<number>) */ x) {",
        "  if (!goog.isArray(x)) {",
        "    x - 5;",
        "  }",
        "}"));
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(x) {",
        "  if (goog.isArray(x)) {",
        "    return x[0] - 5;",
        "  }",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|function(number)) */ x) {",
        "  if (!goog.isFunction(x)) {",
        "    x - 5;",
        "  }",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {?Foo} x */",
        "function f(x) {",
        "  if (!goog.isObject(x)) {",
        "    var /** null */ y = x;",
        "  }",
        "}"));
  }

  public void testGoogTypeof() {
    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  if (goog.typeOf(x) === 'number') {",
        "    var /** number */ n = x;",
        "  } else {",
        "    var /** string */ s = x;",
        "  }",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  if ('number' === goog.typeOf(x)) {",
        "    var /** number */ n = x;",
        "  }",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "function f(/** (number|string) */ x) {",
        "  if (goog.typeOf(x) == 'number') {",
        "    var /** number */ n = x;",
        "  }",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @param {number=} x */",
        "function f(x) {",
        "  if (goog.typeOf(x) === 'undefined') {",
        "    var /** undefined */ u = x;",
        "  } else {",
        "    var /** number */ n = x;",
        "  }",
        "}"));

    typeCheck(CLOSURE_BASE + Joiner.on('\n').join(
        "/** @param {string} x */",
        "function f(x, other) {",
        "  if (goog.typeOf(x) === other) {",
        "    var /** null */ n = x;",
        "  } else {",
        "    x - 5;",
        "  }",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS,
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testSuperClassCtorProperty() throws Exception {
    String CLOSURE_DEFS = Joiner.on('\n').join(
        "/** @const */ var goog = {};",
        "goog.inherits = function(child, parent){};");

    typeCheck(CLOSURE_DEFS + Joiner.on('\n').join(
        "/** @constructor */function base() {}",
        "/** @return {number} */ ",
        "  base.prototype.foo = function() { return 1; };",
        "/** @extends {base}\n * @constructor */function derived() {}",
        "goog.inherits(derived, base);",
        "var /** number */ n = derived.superClass_.foo()"));

    typeCheck(CLOSURE_DEFS + Joiner.on('\n').join(
        "/** @constructor */ function OldType() {}",
        "/** @param {?function(new:OldType)} f */ function g(f) {",
        "  /**",
        "    * @constructor",
        "    * @extends {OldType}",
        "    */",
        "  function NewType() {};",
        "  goog.inherits(NewType, f);",
        "  NewType.prototype.method = function() {",
        "    NewType.superClass_.foo.call(this);",
        "  };",
        "}"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(CLOSURE_DEFS + Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "goog.inherits(Foo, Object);",
        "var /** !Object */ b = Foo.superClass_;"));

    typeCheck(CLOSURE_DEFS + Joiner.on('\n').join(
        "/** @constructor */ function base() {}",
        "/** @constructor @extends {base} */ function derived() {}",
        "goog.inherits(derived, base);",
        "var /** null */ b = derived.superClass_"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        CLOSURE_DEFS + "var /** !Object */ b = Object.superClass_",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        CLOSURE_DEFS + "var o = {x: 'str'}; var q = o.superClass_;",
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(CLOSURE_DEFS
        + "var o = {superClass_: 'str'}; var /** string */ s = o.superClass_;");
  }

  public void testAcrossScopeNamespaces() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "(function() {",
        "  /** @constructor */",
        "  ns.Foo = function() {};",
        "})();",
        "ns.Foo();"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "(function() {",
        "  /** @type {string} */",
        "  ns.str = 'str';",
        "})();",
        "ns.str - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "(function() {",
        "  /** @constructor */",
        "  ns.Foo = function() {};",
        "})();",
        "function f(/** ns.Foo */ x) {}",
        "f(1);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "(function() {",
        "  /** @constructor */",
        "  ns.Foo = function() {};",
        "})();",
        "var /** ns.Foo */ x = 123;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testQualifiedNamedTypes() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @constructor */",
        "ns.Foo = function() {};",
        "ns.Foo();"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @typedef {number} */",
        "ns.num;",
        "var /** ns.num */ y = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @enum {number} */",
        "ns.Foo = { A: 1 };",
        "var /** ns.Foo */ y = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var E = { A: 1 };",
        "/** @typedef {number} */",
        "E.num;",
        "var /** E.num */ x = 'str';"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testEnumsAsNamespaces() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @enum {number} */",
        "ns.E = {",
        "  ONE: 1,",
        "  TWO: true",
        "};"),
        NewTypeInference.INVALID_OBJLIT_PROPERTY_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum */",
        "var E = { A: 1 };",
        "/** @enum */",
        "E.E2 = { B: true };",
        "var /** E */ x = E.A;"),
        NewTypeInference.INVALID_OBJLIT_PROPERTY_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @enum */",
        "var E = { A: 1 };",
        "/** @constructor */",
        "E.Foo = function(x) {};",
        "var /** E */ x = E.A;"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @enum {number} */",
        "ns.E = { A: 1 };",
        "/** @constructor */",
        "ns.E.Foo = function(x) {};"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @enum {number} */",
        "ns.E = { A: 1 };",
        "/** @constructor */",
        "ns.E.Foo = function(x) {};",
        "function f() { ns.E.Foo(); }"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);
  }

  public void testStringMethods() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function String(){}",
        "/** @this {String|string} */",
        "String.prototype.substr = function(start) {};",
        "/** @const */ var ns = {};",
        "/** @const */ var s = 'str';",
        "ns.r = s.substr(2);"));
  }

  public void testOutOfOrderDeclarations() {
    // This is technically valid JS, but we don't support it
    typeCheck("Foo.STATIC = 5; /** @constructor */ function Foo(){}");
  }

  public void testAbstractMethodsAreTypedCorrectly() {
    typeCheck(Joiner.on('\n').join(
        "/** @type {!Function} */",
        "var abstractMethod = function(){};",
        "/** @constructor */ function Foo(){};",
        "/** @constructor @extends {Foo} */ function Bar(){};",
        "/** @param {number} index */",
        "Foo.prototype.m = abstractMethod;",
        "/** @override */",
        "Bar.prototype.m = function(index) {};"));

    typeCheck(Joiner.on('\n').join(
        "/** @type {!Function} */",
        "var abstractMethod = function(){};",
        "/** @constructor */ function Foo(){};",
        "/** @constructor @extends {Foo} */ function Bar(){};",
        "/** @param {number} index */",
        "Foo.prototype.m = abstractMethod;",
        "/** @override */",
        "Bar.prototype.m = function(index) {};",
        "(new Bar).m('str');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @type {!Function} */",
        "var abstractMethod = function(){};",
        "/** @constructor */ function Foo(){};",
        "/** @constructor @extends {Foo} */ function Bar(){};",
        "/**",
        " * @param {number} b",
        " * @param {string} a",
        " */",
        "Foo.prototype.m = abstractMethod;",
        "/** @override */",
        "Bar.prototype.m = function(a, b) {};",
        "(new Bar).m('str', 5);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE,
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @type {!Function} */",
        "var abstractMethod = function(){};",
        "/** @constructor */ function Foo(){};",
        "/** @constructor @extends {Foo} */ function Bar(){};",
        "/** @type {function(number, string)} */",
        "Foo.prototype.m = abstractMethod;",
        "/** @override */",
        "Bar.prototype.m = function(a, b) {};",
        "(new Bar).m('str', 5);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE,
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testUseJsdocOfCalleeForUnannotatedFunctionsInArgumentPosition() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {string} */ this.prop = 'asdf'; }",
        "/** @param {function(!Foo)} fun */",
        "function f(fun) {}",
        "f(function(x) { x.prop = 123; });"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { /** @type {string} */ this.prop = 'asdf'; }",
        "function f(/** function(this:Foo) */ x) {}",
        "f(function() { this.prop = 123; });"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(string)} fun */",
        "function f(fun) {}",
        "f(function(str) { str - 5; });"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(number, number=)} fun */",
        "function f(fun) {}",
        "f(function(num, maybeNum) { num - maybeNum; });"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {function(string, ...string)} fun */",
        "function f(fun) {}",
        "f(function(str, maybeStrs) { str - 5; });"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @param {function(string)} fun */",
        "ns.f = function(fun) {}",
        "ns.f(function(str) { str - 5; });"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @type {function(function(string))} */",
        "ns.f = function(fun) {}",
        "ns.f(function(str) { str - 5; });"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @param {function(string)} fun */",
        "Foo.f = function(fun) {}",
        "Foo.f(function(str) { str - 5; });"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "(/** @param {function(string)} fun */ function(fun) {})(",
        "  function(str) { str - 5; });"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @param {function(function(function(string)))} outerFun */",
        "ns.f = function(outerFun) {};",
        "ns.f(function(innerFun) {",
        "  innerFun(function(str) { str - 5; });",
        "});"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testNamespacesWithNonEmptyObjectLiteral() {
    typeCheck("/** @const */ var o = { /** @const */ PROP: 5 };");

    typeCheck(
        "var x = 5; /** @const */ var o = { /** @const {number} */ PROP: x };");

    typeCheck(Joiner.on('\n').join(
        "var x = 'str';",
        "/** @const */ var o = { /** @const {string} */ PROP: x };",
        "function g() { o.PROP - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "/** @const */ ns.o = { /** @const */ PROP: 5 };"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "var x = 5;",
        "/** @const */ ns.o = { /** @const {number} */ PROP: x };"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */ var ns = {};",
        "var x = 'str';",
        "/** @const */ ns.o = { /** @const {string} */ PROP: x };",
        "function g() { ns.o.PROP - 5; }"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    // These declarations are not considered namespaces
    typeCheck(
        "(function(){ return {}; })().ns = { /** @const */ PROP: 5 };",
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "function f(/** { x : string } */ obj) {",
        "  obj.ns = { /** @const */ PROP: 5 };",
        "}"),
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);
  }

  // TODO(dimvar): fix
  public void testAllTestsShouldHaveDupPropWarnings() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.Foo = {};",
        "ns.Foo = { a: 123 };"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.Foo = {};",
        "/** @const */",
        "ns.Foo = { a: 123 };"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.Foo = {};",
        "/**",
        " * @const",
        // @suppress is ignored here b/c there is no @type in the jsdoc.
        " * @suppress {duplicate}",
        " */",
        "ns.Foo = { a: 123 };"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @const */",
        "ns.Foo = {};",
        "/** @type {number} */",
        "ns.Foo = 123;"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum {number} */",
        "var en = { A: 5 };",
        "/** @const */",
        "en.Foo = {};",
        "/** @type {number} */",
        "en.Foo = 123;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @const */",
        "Foo.ns = {};",
        "/** @const */",
        "Foo.ns = {};"));
  }

  public void testNominalTypeAliasing() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "var Bar = Foo;",
        "var /** !Bar */ x = new Foo();"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @constructor */",
        "ns.Foo = function() {};",
        "/** @constructor */",
        "ns.Bar = ns.Foo;",
        "function g() {",
        "  var /** !ns.Bar */ x = new ns.Foo();",
        "  var /** !ns.Bar */ y = new ns.Bar();",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @type {number} */",
        "var n = 123;",
        "/** @constructor */",
        "var Foo = n;"),
        GlobalTypeInfo.EXPECTED_CONSTRUCTOR);

    typeCheck(Joiner.on('\n').join(
        "/** @type {number} */",
        "var n = 123;",
        "/** @interface */",
        "var Foo = n;"),
        GlobalTypeInfo.EXPECTED_INTERFACE);

    typeCheck(Joiner.on('\n').join(
        "/** @interface */",
        "function Foo() {}",
        "/** @constructor */",
        "var Bar = Foo;"),
        GlobalTypeInfo.EXPECTED_CONSTRUCTOR);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @interface */",
        "var Bar = Foo;"),
        GlobalTypeInfo.EXPECTED_INTERFACE);

    // TODO(dimvar): When we allow unknown type names, eg, for fwd-declared
    // types, then we can also fix this.
    // Currently, the type checker doesn't know what !Foo is.
    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "var Bar;",
        Joiner.on('\n').join(
            "/**",
            " * @constructor",
            " * @final",
            " */",
            "var Foo = Bar;",
            "var /** !Foo */ x;"),
        GlobalTypeInfo.EXPECTED_CONSTRUCTOR);
  }

  public void testTypeVariablesVisibleInPrototypeMethods() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "Foo.prototype.method = function() {",
        "  /** @type {T} */",
        "  this.prop = 123;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "/** @param {T} x */",
        "Foo.prototype.method = function(x) {",
        "  x = 123;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "/** @param {T} x */",
        "Foo.prototype.method = function(x) {",
        "  this.prop = x;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "/** @param {T} x */",
        "Foo.prototype.method = function(x) {",
        "  /** @const */",
        "  this.prop = x;",
        "}"),
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Parent() {}",
        "/**",
        " * @constructor",
        " * @extends {Parent<string>}",
        " */",
        "function Child() {}",
        "Child.prototype.method = function() {",
        "  /** @type {T} */",
        "  this.prop = 123;",
        "}"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);
  }

  public void testInferConstTypeFromEnumProps() {
    typeCheck(Joiner.on('\n').join(
        "/** @enum */",
        "var e = { A: 1 };",
        "/** @const */",
        "var numarr = [ e.A ];"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum */",
        "var e = { A: 1 };",
        "/** @type {number} */",
        "e.prop = 123;",
        "/** @const */",
        "var x = e.prop;"),
        GlobalTypeInfo.COULD_NOT_INFER_CONST_TYPE);
  }

  private static final String FORWARD_DECLARATION_DEFINITIONS = Joiner.on('\n').join(
        "/** @const */ var goog = {};",
        "goog.addDependency = function(file, provides, requires){};",
        "goog.forwardDeclare = function(name){};");

  public void testForwardDeclarations() {

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.addDependency('', ['Foo'], []);",
        "goog.forwardDeclare('Bar');",
        "function f(/** !Foo */ x) {}",
        "function g(/** !Bar */ y) {}"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "/** @const */ var ns = {};",
        "goog.addDependency('', ['ns.Foo'], []);",
        "goog.forwardDeclare('ns.Bar');",
        "function f(/** !ns.Foo */ x) {}",
        "function g(/** !ns.Bar */ y) {}"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "/** @const */ var ns = {};",
        "goog.forwardDeclare('ns.Bar');",
        "function f(/** !ns.Baz */ x) {}"),
        GlobalTypeInfo.UNRECOGNIZED_TYPE_NAME);

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.addDependency('', ['Foo'], []);",
        "goog.forwardDeclare('Bar');",
        "var f = new Foo;",
        "var b = new Bar;"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "/** @const */ var ns = {};",
        "goog.addDependency('', ['ns.Foo'], []);",
        "goog.forwardDeclare('ns.Bar');",
        "var f = new ns.Foo;",
        "var b = new ns.Bar;"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "/** @const */ var ns = {};",
        "goog.addDependency('', ['ns.subns.Foo'], []);",
        "goog.forwardDeclare('ns.subns.Bar');",
        "var f = new ns.subns.Foo;",
        "var b = new ns.subns.Bar;"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.addDependency('', ['ns.subns.Foo'], []);",
        "goog.forwardDeclare('ns.subns.Bar');",
        "var f = new ns.subns.Foo;",
        "var b = new ns.subns.Bar;"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.forwardDeclare('ns.subns');",
        "goog.forwardDeclare('ns.subns.Bar');",
        "var b = new ns.subns.Bar;"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.forwardDeclare('num');",
        "/** @type {number} */ var num = 5;",
        "function f() { var /** null */ o = num; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.forwardDeclare('Foo');",
        "/** @constructor */ function Foo(){}",
        "function f(/** !Foo */ x) { var /** null */ n = x; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.forwardDeclare('ns.Foo');",
        "/** @const */ var ns = {};",
        "/** @constructor */ ns.Foo = function(){}",
        "function f(/** !ns.Foo */ x) { var /** null */ n = x; }"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    // In the following cases the old type inference warned about arg type,
    // but we allow rather than create synthetic named type
    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "goog.forwardDeclare('Foo');",
        "function f(/** !Foo */ x) {}",
        "/** @constructor */ function Bar(){}",
        "f(new Bar);"));

    typeCheck(Joiner.on('\n').join(FORWARD_DECLARATION_DEFINITIONS,
        "/** @const */ var ns = {};",
        "goog.forwardDeclare('ns.Foo');",
        "function f(/** !ns.Foo */ x) {}",
        "/** @constructor */ function Bar(){}",
        "f(new Bar);"));
  }

  public void testDontLookupInParentScopeForNamesWithoutDeclaredType() {
    typeCheck(Joiner.on('\n').join(
        "/** @type {number} */",
        "var x;",
        "function f() {",
        "  var x = true;",
        "}"));
  }

  public void testSpecializationInPropertyAccesses() {
    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @type {?number} */ ns.n = 123;",
        "if (ns.n === null) {",
        "} else {",
        "  ns.n - 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @const */",
        "var ns = {};",
        "/** @type {?number} */ ns.n = 123;",
        "if (ns.n !== null) {",
        "  ns.n - 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "var obj = { n: (1 < 2 ? null : 123) };",
        "if (obj.n !== null) {",
        "  obj.n - 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "var obj = { n: (1 < 2 ? null : 123) };",
        "if (obj.n !== null) {",
        "  obj.n - 123;",
        "}",
        "obj.n - 123;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  this.a = 123;",
        "}",
        "/** @constructor */",
        "function Bar(x) {",
        "  /** @type {Foo} */",
        "  this.b = x;",
        "  if (this.b != null) {",
        "    return this.b.a;",
        "  }",
        "}"));
  }

  public void testAutoconvertBoxedNumberToNumber() {
    typeCheck(
        "var /** !Number */ n = 123;", NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "var /** number */ n = new Number(123);",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Number */ x, y) {",
        "  return x - y;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(x, /** !Number */ y) {",
        "  return x - y;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Number */ x) {",
        "  return x + 'asdf';",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Number */ x) {",
        "  return -x;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Number */ x) {",
        "  x -= 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Number */ x, y) {",
        "  y -= x;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Number */ x, /** !Array<string>*/ arr) {",
        "  return arr[x];",
        "}"));
  }

  public void testAutoconvertBoxedStringToString() {
    typeCheck(
        "var /** !String */ s = '';", NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "var /** string */ s = new String('');",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  var /** !String */ x;",
        "  for (x in { p1: 123, p2: 234 }) ;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** !String */ x) {",
        "  return x + 1;",
        "}"));
  }

  public void testAutoconvertBoxedBooleanToBoolean() {
    typeCheck(
        "var /** !Boolean */ b = true;", NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "var /** boolean */ b = new Boolean(true);",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** !Boolean */ x) {",
        "  if (x) { return 123; };",
        "}"));
  }

  public void testAutoconvertScalarsToBoxedScalars() {
    typeCheck(Joiner.on('\n').join(
        "var /** number */ n = 123;",
        "n.toString();"));

    typeCheck(Joiner.on('\n').join(
        "var /** boolean */ b = true;",
        "b.toString();"));

    typeCheck(Joiner.on('\n').join(
        "var /** string */ s = '';",
        "s.toString();"));

    typeCheck(Joiner.on('\n').join(
        "var /** number */ n = 123;",
        "n.prop = 0;",
        "n.prop - 5;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "var /** number */ n = 123;",
        "n['to' + 'String'];"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */",
        "  this.prop = 123;",
        "}",
        "(new Foo).prop.newprop = 5;"));

    typeCheck(Joiner.on('\n').join(
        "/** @enum */",
        "var E = { A: 1 };",
        "function f(/** E */ x) {",
        "  return x.toString();",
        "}"),
        NewTypeInference.PROPERTY_ACCESS_ON_NONOBJECT);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype.toString = function() {};",
        "function f(/** (number|!Foo) */ x) {",
        "  return x.toString();",
        "}"));
  }

  public void testConstructorsCalledWithoutNew() {
    typeCheck(Joiner.on('\n').join(
        "var n = new Number();",
        "n.prop = 0;",
        "n.prop - 5;"));

    typeCheck(Joiner.on('\n').join(
        "var n = Number();",
        "n.prop = 0;",
        "n.prop - 5;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor @return {number} */ function Foo(){ return 5; }",
        "var /** !Foo */ f = new Foo;",
        "var /** number */ n = Foo();"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo(){ return 5; }",
        "var /** !Foo */ f = new Foo;",
        "var n = Foo();"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);

    // For constructors, return of ? is interpreted the same as undeclared
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @return {?} */ function Foo(){}",
        "var /** !Foo */ f = new Foo;",
        "var n = Foo();"),
        TypeCheck.CONSTRUCTOR_NOT_CALLABLE);
  }

  public void testFunctionBind() {
    // Don't handle specially
    typeCheck(Joiner.on('\n').join(
        "var obj = { bind: function() { return 'asdf'; } };",
        "obj.bind() - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { return x; }",
        "f.bind(null, 1, 2);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { return x; }",
        "f.bind();"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { return x - 1; }",
        "var g = f.bind(null);",
        "g();"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "function f() {}",
        "f.bind(1);"),
        NewTypeInference.INVALID_THIS_TYPE_IN_BIND);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}",
        "/** @this {!Foo} */",
        "function f() {}",
        "f.bind(new Bar);"),
        NewTypeInference.INVALID_THIS_TYPE_IN_BIND);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, /** number */ y) { return x - y; }",
        "var g = f.bind(null, 123);",
        "g('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, y) { return x - y; }",
        "var g = f.bind(null, 123);",
        "g('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) { return x - 1; }",
        "var g = f.bind(null, 'asdf');",
        "g() - 3;"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {number=} x */",
        "function f(x) {}",
        "var g = f.bind(null);",
        "g();",
        "g('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {...number} var_args */",
        "function f(var_args) {}",
        "var g = f.bind(null);",
        "g();",
        "g(123, 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {number=} x */",
        "function f(x) {}",
        "f.bind(null, undefined);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "f.bind(null, 1, 2);"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "var g = f.bind(null, 1);",
        "g(2);",
        "g('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // T was instantiated to ? in the f.bind call.
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "var g = f.bind(null);",
        "g(2, 'asdf');"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "f.bind(null, 1, 'asdf');"),
        NewTypeInference.NOT_UNIQUE_INSTANTIATION);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " * @param {T} x",
        " */",
        " function Foo(x) {}",
        "/**",
        " * @template T",
        " * @this {Foo<T>}",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "function f(x, y) {}",
        "f.bind(new Foo('asdf'), 1, 2);"),
        NewTypeInference.INVALID_THIS_TYPE_IN_BIND);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " * @param {T} x",
        " */",
        " function Foo(x) {}",
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {T} y",
        " */",
        "Foo.prototype.f = function(x, y) {};",
        "Foo.prototype.f.bind(new Foo('asdf'), 1, 2);"),
        NewTypeInference.INVALID_THIS_TYPE_IN_BIND);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.bind(new Foo);"),
        NewTypeInference.CANNOT_BIND_CTOR);

    // We can't detect that f takes a string
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " * @param {T} x",
        " */",
        "function Foo(x) {}",
        "/**",
        " * @template T",
        " * @this {Foo<T>}",
        " * @param {T} x",
        " */",
        "function poly(x) {}",
        "function f(x) {",
        "  poly.bind(new Foo('asdf'), x);",
        "}",
        "f(123);"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {string} */",
        "  this.p = 'asdf';",
        "}",
        "(function() { this.p - 5; }).bind(new Foo);"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */",
        "  this.p = 123;",
        "}",
        "(function(x) { this.p - x; }).bind(new Foo, 321);"));
  }

  public void testClosureStyleFunctionBind() {
    typeCheck(
        "goog.bind(123, null);", NewTypeInference.GOOG_BIND_EXPECTS_FUNCTION);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { return x; }",
        "goog.bind(f, null, 1, 2);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "function f(x) { return x; }",
        "goog.bind(f);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "function f() {}",
        "goog.bind(f, 1);"),
        NewTypeInference.INVALID_THIS_TYPE_IN_BIND);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, /** number */ y) { return x - y; }",
        "var g = goog.bind(f, null, 123);",
        "g('asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) { return x - 1; }",
        "var g = goog.partial(f, 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) { return x - 1; }",
        "var g = goog.partial(f, 'asdf');",
        "g() - 3;"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  if (typeof x == 'function') {",
        "    goog.bind(x, {}, 1, 2);",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {string} */",
        "  this.p = 'asdf';",
        "}",
        "goog.bind(function() { this.p - 5; }, new Foo);"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck("goog.partial(function(x) {}, 123)");

    typeCheck("goog.bind(function() {}, null)();");
  }

  public void testPlusBackwardInference() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, w) {",
        "  var y = x + 2;",
        "  function g() { return (y + 2) - 5; }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x, w) {",
        "  function h() { return (w + 2) - 5; }",
        "}"));
  }

  public void testPlus() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {!Foo} x */",
        "function f(x, i) {",
        "  var /** string */ s = x[i];",
        "  var /** number */ y = x[i] + 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** ? */ x) {",
        "  var /** string */ s = '' + x;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** ? */ x) {",
        "  var /** number */ s = '' + x;",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "function f(/** ? */ x, /** ? */ y) {",
        "  var /** number */ s = x + y;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** * */ x) {",
        "  var /** number */ n = x + 1;",
        "  var /** string */ s = 1 + x;",
        "}"),
        NewTypeInference.INVALID_OPERAND_TYPE,
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "var /** number */ n = 1 + null;",
        "var /** string */ s = 1 + null;"),
        NewTypeInference.INVALID_OPERAND_TYPE,
        NewTypeInference.INVALID_OPERAND_TYPE,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var /** number */ n = undefined + 2;",
        "var /** string */ s = undefined + 2;"),
        NewTypeInference.INVALID_OPERAND_TYPE,
        NewTypeInference.INVALID_OPERAND_TYPE,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "var /** number */ n = 3 + true;",
        "var /** string */ s = 3 + true;"),
        NewTypeInference.INVALID_OPERAND_TYPE,
        NewTypeInference.INVALID_OPERAND_TYPE,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheckCustomExterns(
        DEFAULT_EXTERNS + "/** @type {number} */ var NaN;",
        Joiner.on('\n').join(
            "var /** number */ n = NaN + 1;",
            "var /** string */ s = NaN + 1;"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testUndefinedFunctionCtorNoCrash() {
    typeCheckCustomExterns("", "function f(x) {}",
        GlobalTypeInfo.FUNCTION_CONSTRUCTOR_NOT_DEFINED);

    // Test that NTI is not run
    typeCheckCustomExterns("", "function f(x) { 1 - 'asdf'; }",
        GlobalTypeInfo.FUNCTION_CONSTRUCTOR_NOT_DEFINED);
  }

  public void testTrickyPropertyJoins() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @type {number} */",
        "Foo.prototype.length;",
        "/** @param {{length:number}|!Foo} x */",
        "function f(x) {",
        "  return x.length - 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @type {number} */",
        "Foo.prototype.length;",
        "/** @param {null|{length:number}|!Foo} x */",
        "function f(x) {",
        "  return x.length - 123;",
        "}"),
        NewTypeInference.NULLABLE_DEREFERENCE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @type {number} */",
        "Foo.prototype.length;",
        "/** @param {null|!Foo|{length:number}} x */",
        "function f(x) {",
        "  return x.length - 123;",
        "}"),
        NewTypeInference.NULLABLE_DEREFERENCE);
  }

  public void testJoinOfClassyAndLooseObject() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo(){}",
        "/** @type {number} */",
        "Foo.prototype.p = 5;",
        "function f(o) {",
        "  if (o.p == 5) {",
        "    (function(/** !Foo */ x){})(o);",
        "  }",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() { this.p = 123; }",
        "function f(x) {",
        "  var y;",
        "  if (x.p) {",
        "    y = x;",
        "  } else {",
        "    y = new Foo;",
        "    y.prop = 'asdf';",
        "  }",
        "  y.p -123;",
        "}"));
  }

  public void testUnificationWithSubtyping() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */ function Foo() {}",
        "/** @constructor @extends {Foo} */ function Bar() {}",
        "/** @constructor @extends {Foo} */ function Baz() {}",
        "/**",
        " * @template T",
        " * @param {T|!Foo} x",
        " * @param {T} y",
        " * @return {T}",
        " */",
        "function f(x, y) { return y; }",
        "/** @param {!Bar|!Baz} x */",
        "function g(x) {",
        "  f(x, 123) - 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Parent() {}",
        "/** @constructor @extends {Parent} */",
        "function Child() {}",
        "/**",
        " * @template T",
        " * @param {T|!Parent} x",
        " * @return {T}",
        " */",
        "function f(x) { return /** @type {?} */ (x); }",
        "function g(/** (number|!Child) */ x) {",
        "  f(x) - 5;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Parent() {}",
        "/**",
        " * @constructor",
        " * @extends {Parent<number>}",
        " */",
        "function Child() {}",
        "/**",
        " * @template T",
        " * @param {!Parent<T>} x",
        " */",
        "function f(x) {}",
        "/**",
        " * @param {!Child} x",
        " */",
        "function g(x) { f(x); }"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Parent() {}",
        "/**",
        " * @constructor",
        " * @template U",
        " * @extends {Parent<U>}",
        " */",
        "function Child() {}",
        "/**",
        " * @template T",
        " * @param {!Child<T>} x",
        " */",
        "function f(x) {}",
        "/**",
        " * @param {!Parent<number>} x",
        " */",
        "function g(x) { f(x); }"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @constructor",
        " */",
        "function High() {}",
        "/** @constructor @extends {High<number>} */",
        "function Low() {}",
        "/**",
        " * @template T",
        " * @param {!High<T>} x",
        " * @return {T}",
        " */",
        "function f(x) { return /** @type {?} */ (null); }",
        "var /** string */ s = f(new Low);"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function High() {}",
        "/** @return {T} */",
        "High.prototype.get = function() { return /** @type {?} */ (null); };",
        "/**",
        " * @constructor",
        " * @template U",
        " * @extends {High<U>}",
        " */",
        "function Low() {}",
        "/**",
        " * @template V",
        " * @param {!High<V>} x",
        " * @return {V}",
        " */",
        "function f(x) { return x.get(); }",
        "/** @param {!Low<number>} x */",
        "function g(x) {",
        "  var /** number */ n = f(x);",
        "  var /** string */ s = f(x);",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @interface",
        " * @template T",
        " */",
        "function High() {}",
        "/**",
        " * @constructor",
        " * @template T",
        " * @implements {High<T>}",
        " */",
        "function Mid() {}",
        "/**",
        " * @constructor",
        " * @template T",
        " * @extends {Mid<T>}",
        " * @param {T} x",
        " */",
        "function Low(x) {}",
        "/**",
        " * @template T",
        " * @param {!High<T>} x",
        " * @return {T}",
        " */",
        "function f(x) {",
        "  return /** @type {?} */ (null);",
        "}",
        "f(new Low('asdf')) - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testArgumentsArray() {
    typeCheck("arguments = 123;",
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(
        "function f(x, i) { return arguments[i]; }");

    typeCheck(
        "function f(x) { return arguments['asdf']; }",
        NewTypeInference.NON_NUMERIC_ARRAY_INDEX);

    // Arguments is array-like, but not Array
    typeCheck(
        "function f() { return arguments.splice(); }",
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "function f(x, i) { return arguments[i]; }",
        "f(123, 'asdf')"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f() {",
        "  var arguments = 1;",
        "  return arguments - 1;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @param {string} var_args */",
        "function f(var_args) {",
        "  return arguments[0];",
        "}",
        "f('asdf') - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @param {string} var_args */",
        "function f(var_args) {",
        "  var x = arguments;",
        "  return x[0];",
        "}",
        "f('asdf') - 5;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(x, i) {",
        "  x < i;",
        "  arguments[i];",
        "}",
        "f('asdf', 0);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);
  }

  public void testGenericResolutionWithPromises() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @param {function():(T|!Promise<T>)} x",
        " * @return {!Promise<T>}",
        " * @template T",
        " */",
        "function f(x) { return /** @type {?} */ (null); }",
        "function g(/** function(): !Promise<number> */ x) {",
        "  var /** !Promise<number> */ n = f(x);",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {(T|!Promise<T>)} x",
        " * @return {T}",
        " */",
        "function f(x) { return /** @type {?} */ (null); }",
        "function g(/** !Promise<number> */ x) {",
        "  var /** number */ n = f(x);",
        "  var /** string */ s = f(x);",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);
  }

  public void testFunctionCallProperty() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "f.call(null, 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "f.call(null);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "f.call(null, 1, 2);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {number} x */",
        "Foo.prototype.f = function(x) {};",
        "Foo.prototype.f.call({ a: 123}, 1);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // We don't infer anything about a loose function from a .call invocation.
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(123) - 5;",
        "  x.call(null, 'asdf');",
        "}",
        "f(function(/** string */ s) { return s; });"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function f(x) { return x; }",
        "var /** number */ n = f.call(null, 'asdf');"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "new Foo.call(null);"),
        NewTypeInference.NOT_A_CONSTRUCTOR);
  }

  public void testFunctionApplyProperty() {
    // We only check the receiver argument of a .apply invocation
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "f.apply(null, ['asdf']);"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "f.apply(null, 'asdf');"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // We don't check arity in the array argument
    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "f.apply(null, []);"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** number */ x) {}",
        "f.apply(null, [], 1, 2);"),
        TypeCheck.WRONG_ARGUMENT_COUNT);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @param {number} x */",
        "Foo.prototype.f = function(x) {};",
        "Foo.prototype.f.apply({ a: 123}, [1]);"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    // We don't infer anything about a loose function from a .apply invocation.
    typeCheck(Joiner.on('\n').join(
        "function f(x) {",
        "  x(123) - 5;",
        "  x.apply(null, ['asdf']);",
        "}",
        "f(function(/** string */ s) { return s; });"),
        NewTypeInference.INVALID_ARGUMENT_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @return {T}",
        " */",
        "function f(x) { return x; }",
        "var /** number */ n = f.apply(null, ['asdf']);"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "new Foo.apply(null);"),
        NewTypeInference.NOT_A_CONSTRUCTOR);

    typeCheck(Joiner.on('\n').join(
        "function f(x) {}",
        "function g() { f.apply(null, arguments); }"));
  }

  public void testDontWarnOnPropAccessOfBottom() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Bar() {",
        "  /** @type {?Object} */",
        "  this.obj;",
        "}",
        "Bar.prototype.f = function() {",
        "  this.obj = {};",
        "  if (this.obj != null) {}",
        "};"));

    typeCheck(Joiner.on('\n').join(
        "var x = {};",
        "x.obj = {};",
        "if (x.obj != null) {}"));

    typeCheck(Joiner.on('\n').join(
        "var x = {};",
        "x.obj = {};",
        "if (x['obj'] != null) {}"));
  }

  public void testClasslessObjectsHaveBuiltinProperties() {
    typeCheck(Joiner.on('\n').join(
        "function f(/** !Object */ x) {",
        "  return x.hasOwnProperty('asdf');",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "function f(/** { a: number } */ x) {",
        "  return x.hasOwnProperty('asdf');",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "var x = {};",
        "x.hasOwnProperty('asdf');"));

    typeCheck(Joiner.on('\n').join(
        "var x = /** @struct */ { a: 1 };",
        "x.hasOwnProperty('asdf');"));

    typeCheck(Joiner.on('\n').join(
        "var x = /** @dict */ { 'a': 1 };",
        "x['hasOwnProperty']('asdf');"));
  }

  public void testInferThisInSimpleInferExprType() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @const */ var x = this",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {string} */",
        "  this.p = 'asdf';",
        "}",
        "Foo.prototype.m = function() {",
        "  goog.bind(function() { this.p - 5; }, this);",
        "};"),
        NewTypeInference.INVALID_OPERAND_TYPE);
  }

  public void testNoInexistentPropWarningsForDicts() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor @dict */",
        "function Foo() {}",
        "(new Foo)['prop'] - 1;"));
  }

  public void testAddingPropsToExpandosInWhateverScopes() {
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** !Foo */ x) {",
        "  x.prop = 123;",
        "}",
        "(new Foo).prop - 1;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** !Foo */ x) {",
        "  x.prop = 'asdf';", // we don't declare the type to be string
        "}",
        "(new Foo).prop - 1;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f() {",
        "  var x = new Foo;",
        "  x.prop = 123;", // x not inferred as Foo during GTI
        "}",
        "(new Foo).prop - 1;"),
        TypeCheck.INEXISTENT_PROPERTY);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** !Foo */ x) {",
        "  /** @const */",
        "  x.prop = 123;",
        "}",
        "(new Foo).prop - 1;"),
        GlobalTypeInfo.MISPLACED_CONST_ANNOTATION);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "function f(/** !Foo */ x) {",
        "  /** @type {string} */",
        "  x.prop = 'asdf';",
        "}",
        "(new Foo).prop - 123;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function High() {}",
        "function f(/** !High */ x) {",
        "  /** @type {string} */",
        "  x.prop = 'asdf';",
        "}",
        "/** @constructor @extends {High} */",
        "function Low() {}",
        "(new Low).prop - 123;"),
        NewTypeInference.INVALID_OPERAND_TYPE);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */",
        "  this.prop = 123;",
        "}",
        "function f(/** !Foo */ x) {",
        "  /** @type {string} */",
        "  x.prop = 'asdf';",
        "}"),
        GlobalTypeInfo.REDECLARED_PROPERTY,
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {",
        "  /** @type {number} */",
        "  this.prop = 123;",
        "}",
        "function f(/** !Foo */ x) {",
        "  x.prop = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @type {number} */",
        "Foo.prototype.prop = 123;",
        "function f(/** !Foo */ x) {",
        "  x.prop = 'asdf';",
        "}"),
        NewTypeInference.MISTYPED_ASSIGN_RHS);

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo() {}",
        "function f(/** !Foo<string> */ x) {",
        "  x.prop = 123;",
        "}",
        "(new Foo).prop - 1;"));

    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @constructor",
        " * @template T",
        " */",
        "function Foo(/** T */ x) {}",
        "/** @template U */",
        "function addProp(/** !Foo<U> */ x, /** U */ y) {",
        "  /** @type {U} */ x.prop = y;",
        "  return x;",
        "}",
        "addProp(new Foo(1), 5).prop - 1;"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype.m = function() {",
        "  this.prop = 123;",
        "}",
        "function f(/** !Foo */ x) {",
        "  /** @type {number} */",
        "  x.prop = 123;",
        "}"));

    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "Foo.prototype.prop = 123;",
        "/** @type {!Foo} */",
        "var x = new Foo;",
        "/** @type {number} */",
        "x.prop = 123;"));
  }

  public void testFunctionSubtypingWithReceiverTypes() {
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {function(this:T)} x",
        " */",
        "function f(x) {}",
        "/** @constructor */",
        "function Foo() {}",
        "f(/** @this{Foo} */ function () {});"));

    // We don't catch the NOT_UNIQUE_INSTANTIATION warning
    typeCheck(Joiner.on('\n').join(
        "/**",
        " * @template T",
        " * @param {T} x",
        " * @param {function(this:T)} y",
        " */",
        "function f(x, y) {}",
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {}",
        "f(new Bar, /** @this{Foo} */function () {});"));

    // Sets Bar#p to a number but we don't catch it
    typeCheck(Joiner.on('\n').join(
        "/** @constructor */",
        "function Foo() {}",
        "/** @constructor */",
        "function Bar() {",
        "  /** @type {string} */ this.p = 'asdf';",
        "}",
        "/**",
        " * @this {Foo}",
        " * @param {number} x",
        " */",
        "function f(x) { this.p = x; }",
        "/** @param {function(number)} x */",
        "function g(x) { x.call(new Bar, 123); }",
        "g(f);"));
  }
}
