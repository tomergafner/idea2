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
package com.intellij.psi.impl.source.javadoc;

import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;

/**
 * @author mike
 */
class SimpleDocTagInfo implements JavadocTagInfo {
  private final String myName;
  private final Class myContext;
  private final boolean myInline;
  private final LanguageLevel myLanguageLevel;

  public SimpleDocTagInfo(@NonNls String name, Class context, boolean isInline, LanguageLevel level) {
    myName = name;
    myContext = context;
    myInline = isInline;
    myLanguageLevel = level;
  }

  public String getName() {
    return myName;
  }

  public boolean isValidInContext(PsiElement element) {
    if (PsiUtil.getLanguageLevel(element).compareTo(myLanguageLevel) < 0) {
      return false;
    }

    return myContext.isInstance(element);
  }

  public Object[] getPossibleValues(PsiElement context, PsiElement place, String prefix) {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public String checkTagValue(PsiDocTagValue value) {
    return null;
  }

  public PsiReference getReference(PsiDocTagValue value) {
    return null;
  }

  public boolean isInline() {
    return myInline;
  }
}
