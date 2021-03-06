/*
 * Copyright 2003-2005 Dave Griffith
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
package com.siyeh.ig.fixes;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.InspectionGadgetsBundle;
import org.jetbrains.annotations.NotNull;

public class NormalizeDeclarationFix extends InspectionGadgetsFix{
    @NotNull
    public String getName(){
        return InspectionGadgetsBundle.message("normalize.declaration.quickfix");
    }

    public void doFix(Project project, ProblemDescriptor descriptor)
            throws IncorrectOperationException{
        final PsiElement variableNameElement = descriptor.getPsiElement();
        final PsiVariable var = (PsiVariable) variableNameElement.getParent();
        assert var != null;
        var.normalizeDeclaration();
    }
}
