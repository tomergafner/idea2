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
package com.intellij.ide.util;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiFormatUtil;

import javax.swing.*;

public class MethodCellRenderer extends PsiElementListCellRenderer<PsiMethod>{
  private final boolean myShowMethodNames;
  private final PsiClassListCellRenderer myClassListCellRenderer = new PsiClassListCellRenderer();
  public MethodCellRenderer(boolean showMethodNames) {
    myShowMethodNames = showMethodNames;
  }

  public String getElementText(PsiMethod element) {
    final PsiNamedElement container = fetchContainer(element);
    String text = container instanceof PsiClass ? myClassListCellRenderer.getElementText((PsiClass)container) : container.getName();
    if (myShowMethodNames) {
      final int options = PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS;
      text += "."+PsiFormatUtil.formatMethod(element, PsiSubstitutor.EMPTY, options, PsiFormatUtil.SHOW_TYPE);
    }
    return text;
  }

  protected Icon getIcon(PsiElement element) {
    return super.getIcon(myShowMethodNames ? element : fetchContainer((PsiMethod)element));
  }

  private static PsiNamedElement fetchContainer(PsiMethod element){
    PsiClass aClass = element.getContainingClass();
    if (aClass == null) {
      return element.getContainingFile();
    }
    else {
      return aClass;
    }
  }

  public String getContainerText(final PsiMethod element, final String name) {
    return myClassListCellRenderer.getContainerTextStatic(element);
  }

  public int getIconFlags() {
    return myClassListCellRenderer.getIconFlags();
  }
}
