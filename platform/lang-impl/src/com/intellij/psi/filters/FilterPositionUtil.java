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

package com.intellij.psi.filters;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class FilterPositionUtil {

  @Nullable
  public static PsiElement searchNonSpaceNonCommentBack(PsiElement element) {
    if(element == null || element.getNode() == null) return null;
    ASTNode leftNeibour = TreeUtil.prevLeaf(element.getNode());
    while (leftNeibour != null && (leftNeibour.getElementType() == TokenType.WHITE_SPACE || leftNeibour.getPsi() instanceof PsiComment)){
      leftNeibour = TreeUtil.prevLeaf(leftNeibour);
    }
    return leftNeibour != null ? leftNeibour.getPsi() : null;

  }
}
