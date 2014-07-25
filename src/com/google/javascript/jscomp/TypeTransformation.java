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

import com.google.common.collect.ImmutableMap;
import com.google.javascript.jscomp.parsing.TypeTransformationParser;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.JSTypeNative;
import com.google.javascript.rhino.jstype.JSTypeRegistry;

/**
 * A class for processing type transformation expressions
 *
 * @author lpino@google.com (Luis Fernando Pino Duque)
 */
class TypeTransformation {
  private Compiler compiler;
  private JSTypeRegistry typeRegistry;

  TypeTransformation(Compiler compiler) {
    this.compiler = compiler;
    this.typeRegistry = compiler.getTypeRegistry();
  }

  private static boolean isTypeVar(Node n) {
    return n.isName();
  }

  private static boolean isCallTo(Node n,
      TypeTransformationParser.Keywords keyword) {
    if (!n.isCall()) {
      return false;
    }
    return n.getFirstChild().getString().equals(keyword.name);
  }

  /**
   * Checks if the expression is type()
   */
  private static boolean isTypePredicate(Node n) {
    return isCallTo(n, TypeTransformationParser.Keywords.TYPE);
  }

  private JSType getUnknownType() {
    return typeRegistry.getNativeObjectType(JSTypeNative.UNKNOWN_TYPE);
  }

  private JSType typeOrUnknown(JSType type) {
    return (type == null) ? getUnknownType() : type;
  }

  /** Evaluates the type transformation expression and returns the resulting
   * type.
   *
   * @param ttlAst The node representing the type transformation
   * expression
   * @param typeVars The environment containing the information about
   * the type variables
   * @return JSType The resulting type after the transformation
   */
  JSType eval(Node ttlAst, ImmutableMap<String, JSType> typeVars) {
    // Case type variable: T
    if (isTypeVar(ttlAst)) {
      return evalTypeVariable(ttlAst, typeVars);
    }
    // Case basic type: type(typename)
    if (isTypePredicate(ttlAst)) {
      return evalTypePredicate(ttlAst);
    }
    throw new IllegalStateException(
        "Could not evaluate the type transformation expression");
  }

  private JSType evalTypePredicate(Node ttlAst) {
    String typeName = ttlAst.getChildAtIndex(1).getString();
    JSType resultingType = typeRegistry.getType(typeName);
    // If the type name is not defined then return UNKNOWN
    return typeOrUnknown(resultingType);
  }

  private JSType evalTypeVariable(Node ttlAst,
      ImmutableMap<String, JSType> typeVars) {
    String typeVar = ttlAst.getString();
    JSType resultingType = typeVars.get(typeVar);
    // If the type variable is not found in the environment then it will be
    // taken as UNKNOWN
    return typeOrUnknown(resultingType);
  }
}