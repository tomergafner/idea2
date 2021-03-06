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

package com.intellij.codeInspection.htmlInspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.daemon.XmlErrorMessages;
import com.intellij.codeInspection.*;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.xml.*;
import com.intellij.xml.XmlBundle;
import com.intellij.xml.util.XmlUtil;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author spleaner
 */
public class XmlWrongRootElementInspection extends HtmlLocalInspectionTool {

  @Nls
  @NotNull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.XML_INSPECTIONS;
  }

  @Nls
  @NotNull
  public String getDisplayName() {
    return XmlBundle.message("xml.inspection.wrong.root.element");
  }

  @NonNls
  @NotNull
  public String getShortName() {
    return "XmlWrongRootElement";
  }

  @NotNull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  protected void checkTag(@NotNull final XmlTag tag, @NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
    if (!(tag.getParent() instanceof XmlTag)) {
      final PsiFile psiFile = tag.getContainingFile();
      if (!(psiFile instanceof XmlFile)) {
        return;
      }

      XmlFile xmlFile = (XmlFile) psiFile;

      final XmlDocument document = xmlFile.getDocument();
      if (document == null) {
        return;
      }

      XmlProlog prolog = document.getProlog();
      if (prolog == null || prolog.getUserData(DO_NOT_VALIDATE_KEY) != null) {
        return;
      }

      final XmlDoctype doctype = prolog.getDoctype();

      if (doctype == null) {
        return;
      }

      XmlElement nameElement = doctype.getNameElement();

      if (nameElement == null) {
        return;
      }

      String name = tag.getName();
      String text = nameElement.getText();
      if (tag instanceof HtmlTag) {
        name = name.toLowerCase();
        text = text.toLowerCase();
      }

      if (!name.equals(text)) {
        name = XmlUtil.findLocalNameByQualifiedName(name);

        if (!name.equals(text)) {
          if (tag instanceof HtmlTag) {
            return; // it is legal to have html / head / body omitted
          }
          final LocalQuickFix localQuickFix = new LocalQuickFix() {
            @NotNull
            public String getName() {
              return XmlBundle.message("change.root.element.to", doctype.getNameElement().getText());
            }

            @NotNull
            public String getFamilyName() {
              return getName();
            }

            public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor descriptor) {
              if (!CodeInsightUtilBase.prepareFileForWrite(tag.getContainingFile())) {
                return;
              }

              new WriteCommandAction(project) {
                protected void run(final Result result) throws Throwable {
                  tag.setName(doctype.getNameElement().getText());
                }
              }.execute();
            }
          };

          holder.registerProblem(XmlChildRole.START_TAG_NAME_FINDER.findChild(tag.getNode()).getPsi(),
            XmlErrorMessages.message("wrong.root.element"),
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, localQuickFix
          );

          final ASTNode astNode = XmlChildRole.CLOSING_TAG_NAME_FINDER.findChild(tag.getNode());
          if (astNode != null) {
            holder.registerProblem(astNode.getPsi(),
              XmlErrorMessages.message("wrong.root.element"),
              ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, localQuickFix
            );
          }
        }
      }
    }
  }
}
