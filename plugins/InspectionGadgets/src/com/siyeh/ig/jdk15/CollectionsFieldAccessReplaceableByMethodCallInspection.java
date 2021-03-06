/*
 * Copyright 2008 Bas Leijdekkers
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
package com.siyeh.ig.jdk15;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CollectionsFieldAccessReplaceableByMethodCallInspection
        extends BaseInspection {

    @Nls @NotNull
    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "collections.field.access.replaceable.by.method.call.display.name");
    }

    @NotNull
    protected String buildErrorString(Object... infos) {
        return InspectionGadgetsBundle.message(
                "collections.field.access.replaceable.by.method.call.problem.descriptor",
                infos[1]);
    }

    @Nullable
    protected InspectionGadgetsFix buildFix(Object... infos) {
        final PsiReferenceExpression expression =
                (PsiReferenceExpression) infos[0];
        return new CollectionsFieldAccessReplaceableByMethodCallFix(
                expression.getReferenceName());
    }

    private static class CollectionsFieldAccessReplaceableByMethodCallFix
            extends InspectionGadgetsFix {

        private final String replacementText;

        private CollectionsFieldAccessReplaceableByMethodCallFix(String referenceName) {
            replacementText = getCollectionsMethodCallText(referenceName);
        }

        @NotNull
        public String getName() {
            return InspectionGadgetsBundle.message(
                    "collections.field.access.replaceable.by.method.call.quickfix",
                    replacementText);
        }

        private static String getCollectionsMethodCallText(
                String collectionsFieldName) {
            if ("EMPTY_LIST".equals(collectionsFieldName)) {
                return "Collections.emptyList()";
            } else if ("EMPTY_MAP".equals(collectionsFieldName)) {
                return "Collections.emptyMap()";
            } else if ("EMPTY_SET".equals(collectionsFieldName)) {
                return "Collections.emptySet()";
            } else {
                throw new AssertionError("unknown collections field name: " +
                        collectionsFieldName);
            }
        }

        protected void doFix(Project project, ProblemDescriptor descriptor)
                throws IncorrectOperationException {
            final PsiElement element = descriptor.getPsiElement();
            if (!(element instanceof PsiReferenceExpression)) {
                return;
            }
            final PsiReferenceExpression referenceExpression =
                    (PsiReferenceExpression) element;
            final String name = referenceExpression.getReferenceName();
            final String newMethodCallText = getCollectionsMethodCallText(name);
            replaceExpression(referenceExpression,
                    "java.util." + newMethodCallText);
        }
    }

    public BaseInspectionVisitor buildVisitor() {
        return new CollectionsFieldAccessReplaceableByMethodCallVisitor();
    }

    private static class CollectionsFieldAccessReplaceableByMethodCallVisitor
            extends BaseInspectionVisitor {

        //List<String> foo() {
        //    return Collections.emptyList();
        //}

        @Override
        public void visitReferenceExpression(
                PsiReferenceExpression expression) {
            if (!PsiUtil.isLanguageLevel5OrHigher(expression)) {
                return;
            }
            super.visitReferenceExpression(expression);
            final String name = expression.getReferenceName();
            final String replacement;
            if ("EMPTY_LIST".equals(name)) {
                replacement = "emptyList()";
            } else if ("EMPTY_MAP".equals(name)) {
                replacement = "emptyMap()";
            } else if ("EMPTY_SET".equals(name)) {
                replacement = "emptySet()";
            } else {
                return;
            }
            final PsiElement target = expression.resolve();
            if (!(target instanceof PsiField)) {
                return;
            }
            final PsiField field = (PsiField) target;
            final PsiClass containingClass = field.getContainingClass();
            final String qualifiedName = containingClass.getQualifiedName();
            if (!"java.util.Collections".equals(qualifiedName)) {
                return;
            }
            registerError(expression, expression, replacement);
        }
    }
}