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
package com.intellij.ide.structureView.impl.java;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class PsiFieldTreeElement extends JavaClassTreeElementBase<PsiField> implements SortableTreeElement {
  public PsiFieldTreeElement(PsiField field, boolean isInherited) {
    super(isInherited,field);
 }

  @NotNull
  public Collection<StructureViewTreeElement> getChildrenBase() {
    return Collections.emptyList();
  }

  public ItemPresentation getPresentation() {
    return this;
  }

  public String getPresentableText() {
    return PsiFormatUtil.formatVariable(
      getElement(),
      PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_TYPE | PsiFormatUtil.TYPE_AFTER | PsiFormatUtil.SHOW_INITIALIZER,
      PsiSubstitutor.EMPTY
    );
  }

  public PsiField getField() {
    return getElement();
  }

  public String getAlphaSortKey() {
    final PsiField field = getElement();
    if (field != null) {
      String name = field.getName();
      if (name != null) {
        return name;
      }
    }
    return "";
  }
}
