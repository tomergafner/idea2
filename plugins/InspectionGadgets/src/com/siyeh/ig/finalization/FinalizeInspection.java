/*
 * Copyright 2003-2007 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ig.finalization;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.NotNull;

public class FinalizeInspection extends BaseInspection {

    @NotNull
    public String getID(){
        return "FinalizeDeclaration";
    }

    @NotNull
    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "finalize.declaration.display.name");
    }

    @NotNull
    public String buildErrorString(Object... infos) {
        return InspectionGadgetsBundle.message(
                "finalize.declaration.problem.descriptor");
    }

    public BaseInspectionVisitor buildVisitor() {
        return new FinalizeDeclaredVisitor();
    }

    private static class FinalizeDeclaredVisitor extends BaseInspectionVisitor {

        @Override public void visitMethod(@NotNull PsiMethod method) {
            //note: no call to super;
            final String methodName = method.getName();
            if (!HardcodedMethodConstants.FINALIZE.equals(methodName)) {
                return;
            }
            final PsiParameterList parameterList = method.getParameterList();
            if (parameterList.getParametersCount() != 0) {
                return;
            }
            registerMethodError(method);
        }
    }
}