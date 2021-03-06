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
package com.intellij.codeInsight.completion;

import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.xml.util.XmlUtil;

/**
 *
 */
public class XmlAutoLookupHandler extends CodeCompletionHandlerBase {
  public XmlAutoLookupHandler() {
    super(CompletionType.BASIC);
  }

  protected boolean mayAutocompleteOnInvocation() {
    return false;
  }

  protected boolean isAutocompleteCommonPrefixOnInvocation() {
    return false;
  }

  protected void handleEmptyLookup(CompletionContext context, final CompletionParameters parameters,
                                   final CompletionProgressIndicator indicator) {
  }

  protected void doComplete(final int offset1, final int offset2, final CompletionContext context, final FileCopyPatcher dummyIdentifier, Editor editor,
                            final int invocationCount) {
    PsiFile file = context.file;
    int offset = context.getStartOffset();

    PsiElement lastElement = InjectedLanguageUtil.findElementAtNoCommit(file, offset - 1);
    if (lastElement == null) return;

    final Ref<Boolean> isRelevantLanguage = new Ref<Boolean>();
    final Ref<Boolean> isAnt = new Ref<Boolean>();
    String text = lastElement.getText();
    final int len = context.getStartOffset() - lastElement.getTextRange().getStartOffset();
    if (len < text.length()) {
      text = text.substring(0, len);
    }
    if (text.equals("<") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt) ||
        text.equals(" ") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt) ||
        text.endsWith("${") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt) && isAnt.get().booleanValue() ||
        text.endsWith("@{") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt) && isAnt.get().booleanValue() ||
        text.endsWith("</") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt)) {
      super.doComplete(offset1, offset2, context, dummyIdentifier, editor, invocationCount);
    }
  }

  private static boolean isLanguageRelevant(final PsiElement element,
                                            final PsiFile file,
                                            final Ref<Boolean> isRelevantLanguage,
                                            final Ref<Boolean> isAnt) {
    Boolean isAntFile = isAnt.get();
    if (isAntFile == null) {
      isAntFile = XmlUtil.isAntFile(file);
      isAnt.set(isAntFile);
    }
    Boolean result = isRelevantLanguage.get();
    if (result == null) {
      Language language = element.getLanguage();
      if (element instanceof PsiWhiteSpace) language = element.getParent().getLanguage();
      result = language instanceof XMLLanguage || isAntFile.booleanValue();
      isRelevantLanguage.set(result);
    }
    return result.booleanValue();
  }
}
