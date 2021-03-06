/*
 * Copyright 2008-2009 Bas Leijdekkers
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
package com.siyeh.ig.performance;

import com.intellij.psi.*;
import com.intellij.openapi.project.Project;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.InspectionGadgetsBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class RedundantStringFormatCallInspection extends BaseInspection {
    
    @Override
    @Nls
    @NotNull
    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "redundant.string.format.call.display.name");
    }

    @Override
    @NotNull
    protected String buildErrorString(Object... infos) {
        return InspectionGadgetsBundle.message(
                "redundant.string.format.call.problem.descriptor");
    }

    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        return new RedundantStringFormatCallFix();
    }

    private static class RedundantStringFormatCallFix
            extends InspectionGadgetsFix {

        @NotNull
        public String getName() {
            return InspectionGadgetsBundle.message(
                    "redundant.string.format.call.quickfix");
        }

        @Override
        protected void doFix(Project project, ProblemDescriptor descriptor)
                throws IncorrectOperationException {
            final PsiElement element = descriptor.getPsiElement();
            final PsiElement parent = element.getParent();
            final PsiElement grandParent = parent.getParent();
            if (!(grandParent instanceof PsiMethodCallExpression)) {
                return;
            }
            final PsiMethodCallExpression methodCallExpression =
                    (PsiMethodCallExpression) grandParent;
            final PsiExpressionList argumentList =
                    methodCallExpression.getArgumentList();
            final PsiExpression[] arguments = argumentList.getExpressions();
            final PsiExpression lastArgument = arguments[arguments.length - 1];
            methodCallExpression.replace(lastArgument);
        }
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new RedundantStringFormatCallVisitor();
    }

    private static class RedundantStringFormatCallVisitor
            extends BaseInspectionVisitor {

        @Override
        public void visitMethodCallExpression(
                PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            final PsiReferenceExpression methodExpression =
                    expression.getMethodExpression();
            final String methodName =
                    methodExpression.getReferenceName();
            if (!"format".equals(methodName)) {
                return;
            }
            final PsiExpressionList argumentList = expression.getArgumentList();
            final PsiExpression[] arguments = argumentList.getExpressions();
            if (arguments.length > 2 || arguments.length == 0) {
                return;
            }
            final PsiExpression qualifier =
                    methodExpression.getQualifierExpression();
            if (qualifier == null) {
                return;
            }
            if (!(qualifier instanceof PsiReference)) {
                return;
            }
            final PsiReference referenceExpression =
                    (PsiReference) qualifier;
            final PsiElement target = referenceExpression.resolve();
            if (!(target instanceof PsiClass)) {
                return;
            }
            final PsiClass aClass = (PsiClass) target;
            final String className = aClass.getQualifiedName();
            if (!"java.lang.String".equals(className)) {
                return;
            }
            final PsiExpression firstArgument = arguments[0];
            final PsiType firstType = firstArgument.getType();
            if (firstType == null) {
                return;
            }
            if (firstArgument instanceof PsiLiteralExpression) {
                final PsiLiteralExpression literalExpression =
                        (PsiLiteralExpression)firstArgument;
                final String expressionText = literalExpression.getText();
                if (expressionText.contains("%n")) {
                    return;
                }
            }
            if (firstType.equalsToText("java.lang.String") &&
                    arguments.length == 1) {
                registerMethodCallError(expression);
            } else if (firstType.equalsToText("java.util.Locale")) {
                if (arguments.length != 2) {
                    return;
                }
                final PsiExpression secondArgument = arguments[1];
                final PsiType secondType = secondArgument.getType();
                if (secondType == null) {
                    return;
                }
                if (secondType.equalsToText("java.lang.String")) {
                    registerMethodCallError(expression);
                }
            }
        }
    }
}
