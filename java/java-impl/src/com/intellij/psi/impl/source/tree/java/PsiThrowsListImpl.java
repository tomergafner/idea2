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
package com.intellij.psi.impl.source.tree.java;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.impl.source.Constants;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ChildRoleBase;

/**
 *  @author dsl
 */
public class PsiThrowsListImpl extends ReferenceListElement implements Constants {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.tree.java.PsiThrowsListImpl");

  public PsiThrowsListImpl() {
    super(THROWS_LIST);
  }

  protected String getKeywordText() {
    return PsiKeyword.THROWS;
  }

  protected IElementType getKeywordType() {
    return THROWS_KEYWORD;
  }

  public ASTNode findChildByRole(int role) {
    LOG.assertTrue(ChildRole.isUnique(role));
    switch(role){
      default:
        return null;

      case ChildRole.THROWS_KEYWORD:
        return findChildByType(THROWS_KEYWORD);
    }
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == THROWS_KEYWORD) {
      return ChildRole.THROWS_KEYWORD;
    }
    else if (i == COMMA) {
      return ChildRole.COMMA;
    }
    else if (i == JAVA_CODE_REFERENCE) {
      return ChildRole.REFERENCE_IN_LIST;
    }
    else {
      return ChildRoleBase.NONE;
    }
  }
}
