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
package com.siyeh.ig.imports;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticImportInspection extends BaseInspection {

    @NotNull
    public String getDisplayName(){
        return InspectionGadgetsBundle.message("static.import.display.name");
    }

    @NotNull
    public String buildErrorString(Object... infos){
        return InspectionGadgetsBundle.message(
                "static.import.problem.descriptor");
    }

    public BaseInspectionVisitor buildVisitor(){
        return new StaticImportVisitor();
    }

    protected InspectionGadgetsFix buildFix(Object... infos){
        return new StaticImportFix();
    }

    private static class StaticImportFix extends InspectionGadgetsFix{

        @NotNull
        public String getName(){
            return InspectionGadgetsBundle.message(
                    "static.import.replace.quickfix");
        }

        public void doFix(Project project, ProblemDescriptor descriptor)
                throws IncorrectOperationException{
            final PsiImportStaticStatement importStatement =
                    (PsiImportStaticStatement)descriptor.getPsiElement();
            final PsiJavaCodeReferenceElement importReference =
                    importStatement.getImportReference();
            if (importReference == null) {
                return;
            }
            final JavaResolveResult[] importTargets =
                    importReference.multiResolve(false);
            if (importTargets.length == 0) {
                return;
            }
            final boolean onDemand = importStatement.isOnDemand();
            final StaticImportReferenceCollector referenceCollector =
                    new StaticImportReferenceCollector(importTargets,
                            onDemand);
            final PsiJavaFile file =
                    (PsiJavaFile) importStatement.getContainingFile();
            file.accept(referenceCollector);
            final List<PsiJavaCodeReferenceElement> references =
                    referenceCollector.getReferences();
            final Map<PsiJavaCodeReferenceElement, PsiMember>
                    referenceTargetMap = new HashMap();
            for (PsiJavaCodeReferenceElement reference : references) {
                final PsiElement target = reference.resolve();
                if (target instanceof PsiMember) {
                    final PsiMember member = (PsiMember)target;
                    referenceTargetMap.put(reference, member);
                }
            }
            importStatement.delete();
            for (Map.Entry<PsiJavaCodeReferenceElement, PsiMember> entry :
                    referenceTargetMap.entrySet()) {
                removeReference(entry.getKey(), entry.getValue());
            }
        }

        private static void removeReference(
                PsiJavaCodeReferenceElement reference, PsiMember target) {
            final PsiManager manager = reference.getManager();
          final PsiElementFactory factory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
            final PsiClass aClass = target.getContainingClass();
            final String qualifiedName = aClass.getQualifiedName();
            final String text = reference.getText();
            final String referenceText = qualifiedName + '.' + text;
            if (reference instanceof PsiReferenceExpression) {
                try {
                    final PsiExpression newReference =
                            factory.createExpressionFromText(
                                    referenceText, reference);
                    reference.replace(newReference);
                } catch (IncorrectOperationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                final PsiJavaCodeReferenceElement referenceElement =
                        factory.createReferenceElementByFQClassName(
                                referenceText, reference.getResolveScope());
                try {
                    reference.replace(referenceElement);
                } catch (IncorrectOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private static StringBuilder replace(
                PsiElement element, PsiJavaCodeReferenceElement reference,
                String newReferenceText, StringBuilder out) {
            if (element.equals(reference)) {
                out.append(newReferenceText);
                return out;
            }
            final PsiElement[] children = element.getChildren();
            if (children.length == 0) {
                out.append(element.getText());
                return out;
            }
            for (PsiElement child : children) {
                replace(child, reference, newReferenceText, out);
            }
            return out;
        }

        static class StaticImportReferenceCollector
                extends JavaRecursiveElementVisitor {

            private final JavaResolveResult[] importTargets;
            private final boolean onDemand;
            private final List<PsiJavaCodeReferenceElement> references =
                    new ArrayList();

            StaticImportReferenceCollector(
                    @NotNull JavaResolveResult[] importTargets,
                    boolean onDemand) {
                this.importTargets = importTargets;
                this.onDemand = onDemand;
            }

            @Override public void visitReferenceElement(
                    PsiJavaCodeReferenceElement reference) {
                super.visitReferenceElement(reference);
                if (isFullyQualifiedReference(reference)) {
                    return;
                }
                PsiElement parent = reference.getParent();
                if (parent instanceof PsiImportStatementBase) {
                    return;
                }
                while (parent instanceof PsiJavaCodeReferenceElement) {
                    parent = parent.getParent();
                    if (parent instanceof PsiImportStatementBase) {
                        return;
                    }
                }
                checkStaticImportReference(reference);
            }

            private void checkStaticImportReference(
                    PsiJavaCodeReferenceElement reference) {
                if (reference.isQualified()) {
                    return;
                }
                final PsiElement target = reference.resolve();
                if (!(target instanceof PsiMethod) &&
                        !(target instanceof PsiClass) &&
                        !(target instanceof PsiField)) {
                    return;
                }
                final PsiMember member = (PsiMember) target;
                for (JavaResolveResult importTarget : importTargets) {
                    final PsiElement targetElement = importTarget.getElement();
                    if (targetElement instanceof PsiMethod) {
                        if (member.equals(targetElement)) {
                            addReference(reference);
                        }
                    } else if (targetElement instanceof PsiClass) {
                        if (onDemand) {
                            final PsiClass containingClass =
                                    member.getContainingClass();
                            if (InheritanceUtil.isInheritorOrSelf(
                                    (PsiClass)targetElement, containingClass,
                                    true)) {
                                addReference(reference);
                            }
                        } else {
                            if (targetElement.equals(member)) {
                                addReference(reference);
                            }
                        }
                    }
                }
            }

            private void addReference(PsiJavaCodeReferenceElement reference) {
                references.add(reference);
            }

            public List<PsiJavaCodeReferenceElement> getReferences() {
                return references;
            }

            public boolean isFullyQualifiedReference(
                    PsiJavaCodeReferenceElement reference) {
                if (!reference.isQualified()) {
                    return false;
                }
                final PsiElement directParent = reference.getParent();
                if (directParent instanceof PsiMethodCallExpression ||
                        directParent instanceof PsiAssignmentExpression ||
                        directParent instanceof PsiVariable) {
                    return false;
                }
                final PsiElement parent = PsiTreeUtil.getParentOfType(reference,
                        PsiImportStatementBase.class, PsiPackageStatement.class,
                        JavaCodeFragment.class);
                if (parent != null) {
                    return false;
                }
                final PsiElement target = reference.resolve();
                if(!(target instanceof PsiClass)) {
                    return false;
                }
                final PsiClass aClass = (PsiClass) target;
                final String fqName = aClass.getQualifiedName();
                if (fqName == null) {
                    return false;
                }
                final String text = stripAngleBrackets(reference.getText());
                return text.equals(fqName);
            }

            private static String stripAngleBrackets(String string) {
                final int index = string.indexOf('<');
                if (index == -1) {
                    return string;
                }
                return string.substring(0, index);
            }
        }
    }

    private static class StaticImportVisitor extends BaseInspectionVisitor{

        @Override public void visitClass(@NotNull PsiClass aClass){
            // no call to super, so it doesn't drill down
            if(!(aClass.getParent() instanceof PsiJavaFile)){
                return;
            }
          if (JspPsiUtil.isInJspFile(aClass.getContainingFile())) {
            return;
          }
            final PsiJavaFile file = (PsiJavaFile) aClass.getParent();
            if(file == null){
                return;
            }
            if(!file.getClasses()[0].equals(aClass)){
                return;
            }
            final PsiImportList importList = file.getImportList();
            if(importList == null){
                return;
            }
            final PsiImportStaticStatement[] importStatements =
                    importList.getImportStaticStatements();
            for(final PsiImportStaticStatement importStatement :
                    importStatements){
                registerError(importStatement);
            }
        }
    }
}
