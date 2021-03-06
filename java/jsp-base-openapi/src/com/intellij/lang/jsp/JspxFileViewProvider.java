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

package com.intellij.lang.jsp;

import com.intellij.lang.Language;
import com.intellij.lang.DependentLanguage;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: Dec 12, 2005
 * Time: 7:40:40 PM
 * To change this template use File | Settings | File Templates.
 */
public interface JspxFileViewProvider extends TemplateLanguageFileViewProvider {
  Language JAVA_HOLDER_METHOD_TREE_LANGUAGE = new JavaHolderMethodTreeLanguage();

  public static class JavaHolderMethodTreeLanguage extends Language implements DependentLanguage{
    public JavaHolderMethodTreeLanguage() {
      super("JAVA_HOLDER_METHOD_TREE", "");
    }
  }
}
