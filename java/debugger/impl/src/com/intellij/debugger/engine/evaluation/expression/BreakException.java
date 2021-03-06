/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.debugger.engine.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.DebuggerBundle;

/**
 * Created by IntelliJ IDEA.
 * User: lex
 * Date: Apr 20, 2004
 * Time: 7:12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class BreakException extends EvaluateException{
  private final String myLabelName;

  public BreakException(String labelName) {
    super(DebuggerBundle.message("evaluation.error.lebeled.loops.not.found", labelName), null);
    myLabelName = labelName;
  }

  public String getLabelName() {
    return myLabelName;
  }
}
