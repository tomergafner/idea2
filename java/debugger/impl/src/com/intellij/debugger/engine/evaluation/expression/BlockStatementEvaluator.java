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

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateException;

/**
 * Created by IntelliJ IDEA.
 * User: lex
 * Date: Apr 20, 2004
 * Time: 5:21:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class BlockStatementEvaluator implements Evaluator{
  protected Evaluator [] myStatements;

  public BlockStatementEvaluator(Evaluator[] statements) {
    myStatements = statements;
  }

  public Object evaluate(EvaluationContextImpl context) throws EvaluateException {
    Object result = context.getSuspendContext().getDebugProcess().getVirtualMachineProxy().mirrorOf();
    for (int i = 0; i < myStatements.length; i++) {
      Evaluator statement = myStatements[i];
      result = statement.evaluate(context);
    }
    return result;
  }

  public Modifier getModifier() {
    return myStatements.length > 0 ? myStatements[myStatements.length - 1].getModifier() : null;
  }
}
