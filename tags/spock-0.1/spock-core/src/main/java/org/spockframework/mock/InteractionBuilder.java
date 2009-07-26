/*
 * Copyright 2009 the original author or authors.
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

package org.spockframework.mock;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.Closure;

import org.spockframework.util.SyntaxException;

import spock.lang.Predef;

/**
 *
 * @author Peter Niederwieser
 */
public class InteractionBuilder {
  private static final IResultGenerator NO_RESULT_GENERATOR =
      new ConstantResultGenerator(IResultGenerator.NO_VALUE);

  private final int line;
  private final int column;
  private final String text;

  private int minCount = 0;
  private int maxCount = Integer.MAX_VALUE;
  private List<IInvocationConstraint> invConstraints = new ArrayList<IInvocationConstraint>();
  private List<Object> argNames;
  private List<IArgumentConstraint> argConstraints;
  private IResultGenerator resultGenerator = NO_RESULT_GENERATOR;

  public InteractionBuilder(int line, int column, String text) {
    this.line = line;
    this.column = column;
    this.text = text;
  }

  public static final String SET_FIXED_COUNT = "setFixedCount";
  public InteractionBuilder setFixedCount(Object count) {
    if (count == Predef._) {
      minCount = 0;
      maxCount = Integer.MAX_VALUE;
    } else
      minCount = maxCount = convertCount(count, true);

    return this;
  }

  public static final String SET_RANGE_COUNT = "setRangeCount";
  public InteractionBuilder setRangeCount(Object minCount, Object maxCount, boolean inclusive) {
    this.minCount = minCount == Predef._ ? 0 : convertCount(minCount, true);
    this.maxCount = maxCount == Predef._ ? Integer.MAX_VALUE : convertCount(maxCount, inclusive);
    if (this.minCount > this.maxCount)
      throw new SyntaxException("lower bound of invocation count must come before upper bound");
    return this;
  }

  public static final String ADD_EQUAL_TARGET = "addEqualTarget";
  public InteractionBuilder addEqualTarget(Object target) {
    if (target != Predef._) invConstraints.add(new IdenticalTargetConstraint(target));
    return this;
  }

  public static final String ADD_EQUAL_METHOD_NAME = "addEqualMethodName";
  public InteractionBuilder addEqualMethodName(String name) {
    if (!name.equals(Predef._.toString()))
      invConstraints.add(new EqualMethodNameConstraint(name));
    return this;
  }

  public static final String ADD_REGEX_METHOD_NAME = "addRegexMethod";
  public InteractionBuilder addRegexMethod(String regex) {
    invConstraints.add(new RegexMethodNameConstraint(regex));
    return this;
  }

  public static final String SET_ARG_LIST_KIND = "setArgListKind";
  public InteractionBuilder setArgListKind(boolean isPositional) {
    argConstraints = new ArrayList<IArgumentConstraint>();
    if (isPositional)
      invConstraints.add(new PositionalArgumentListConstraint(argConstraints));
    else {
      argNames = new ArrayList<Object>();
      invConstraints.add(new NamedArgumentListConstraint(argNames, argConstraints));
    }
    return this;
  }

  public static final String ADD_ARG_NAME= "addArgName";
  public InteractionBuilder addArgName(String name) {
    argNames.add(name);
    return this;
  }

  public static final String ADD_CODE_ARG = "addCodeArg";
  public InteractionBuilder addCodeArg(Closure closure) {
    argConstraints.add(new CodeArgumentConstraint(closure));
    return this;
  }

  public static final String ADD_EQUAL_ARG = "addEqualArg";
  public InteractionBuilder addEqualArg(Object arg) {
    argConstraints.add(arg == Predef._ ?
        AnyArgumentConstraint.INSTANCE : new EqualArgumentConstraint(arg));
    return this;
  }

  public static final String TYPE_LAST_ARG = "typeLastArg";
  public InteractionBuilder typeLastArg(Class<?> type) {
    IArgumentConstraint last = argConstraints.get(argConstraints.size() - 1);
    argConstraints.set(argConstraints.size() - 1, new TypeArgumentConstraint(type, last));
    return this;
  }

  public static final String NEGATE_LAST_ARG = "negateLastArg";
  public InteractionBuilder negateLastArg() {
    IArgumentConstraint last = argConstraints.get(argConstraints.size() - 1);
    argConstraints.set(argConstraints.size() - 1, new NegatingArgumentConstraint(last));
    return this;
  }

  public static final String SET_DUMMY_RESULT = "setDummyResult";
  public InteractionBuilder setDummyResult() {
    resultGenerator = DummyResultGenerator.INSTANCE;
    return this;
  }

  public static final String SET_CONSTANT_RESULT = "setConstantResult";
  public InteractionBuilder setConstantResult(Object constant) {
    if (constant != Predef._)
      resultGenerator = new ConstantResultGenerator(constant);
    return this;
  }

  public static final String SET_CODE_RESULT = "setCodeResult";
  public InteractionBuilder setCodeResult(Closure closure) {
    resultGenerator = new CodeResultGenerator(closure);
    return this;
  }

  public static final String SET_ITERABLE_RESULT = "setIterableResult";
  public InteractionBuilder setIterableResult(Object iterable) {
    resultGenerator = new IterableResultGenerator(iterable);
    return this;
  }

  public static final String BUILD = "build";
  public IMockInteraction build() {
    return new MockInteraction(line, column, text,
        minCount, maxCount, invConstraints, resultGenerator);
  }

  private static int convertCount(Object count, boolean inclusive) {
    if (!(count instanceof Number))
      throw new SyntaxException("invocation count must be a number");

    int intCount = ((Number)count).intValue();
    if (!inclusive) intCount--;
    if (intCount < 0)
      throw new SyntaxException("invocation count must be >= 0");
    return intCount;
  }
}