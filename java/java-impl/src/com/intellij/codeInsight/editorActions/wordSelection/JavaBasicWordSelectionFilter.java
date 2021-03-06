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
package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocTag;

/**
 * @author yole
 */
public class JavaBasicWordSelectionFilter implements Condition<PsiElement> {
  public boolean value(final PsiElement e) {
    if (e instanceof PsiJavaToken && ((PsiJavaToken)e).getTokenType() == JavaTokenType.IDENTIFIER) {
      return true;
    }
    return !(e instanceof PsiCodeBlock) &&
           !(e instanceof PsiArrayInitializerExpression) &&
           !(e instanceof PsiParameterList) &&
           !(e instanceof PsiExpressionList) &&
           !(e instanceof PsiBlockStatement) &&
           !(e instanceof PsiJavaCodeReferenceElement) &&
           !(e instanceof PsiJavaToken &&
           !(e instanceof PsiKeyword)) &&
           !(e instanceof PsiDocTag);
  }
}
