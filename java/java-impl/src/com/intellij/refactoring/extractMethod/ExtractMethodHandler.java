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
package com.intellij.refactoring.extractMethod;

import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.refactoring.util.duplicates.DuplicatesImpl;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExtractMethodHandler implements RefactoringActionHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.extractMethod.ExtractMethodHandler");

  public static final String REFACTORING_NAME = RefactoringBundle.message("extract.method.title");

  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    if (dataContext != null) {
      final PsiFile file = LangDataKeys.PSI_FILE.getData(dataContext);
      final Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
      if (file != null && editor != null) {
        invokeOnElements(project, editor, file, elements);
      }
    }
  }

  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file, DataContext dataContext) {
    final Pass<PsiElement[]> callback = new Pass<PsiElement[]>() {
      public void pass(final PsiElement[] selectedValue) {
        invokeOnElements(project, editor, file, selectedValue);
      }
    };
    selectAndPass(project, editor, file, callback);
  }

  public static void selectAndPass(final Project project, final Editor editor, final PsiFile file, final Pass<PsiElement[]> callback) {
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    if (!editor.getSelectionModel().hasSelection()) {
      final int offset = editor.getCaretModel().getOffset();
      final PsiElement[] statementsInRange = IntroduceVariableBase.findStatementsAtOffset(editor, file, offset);
      final List<PsiExpression> expressions = IntroduceVariableBase.collectExpressions(file, editor, offset, statementsInRange);
      if (expressions.size() < 2) {
        editor.getSelectionModel().selectLineAtCaret();
      }
      else {
        IntroduceVariableBase.showChooser(editor, expressions, new Pass<PsiExpression>() {
          @Override
          public void pass(PsiExpression psiExpression) {
            callback.pass(new PsiExpression[]{psiExpression});
          }
        });
        return;
      }
    }

    int startOffset = editor.getSelectionModel().getSelectionStart();
    int endOffset = editor.getSelectionModel().getSelectionEnd();

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    PsiElement[] elements;
    PsiExpression expr = CodeInsightUtil.findExpressionInRange(file, startOffset, endOffset);
    if (expr != null) {
      elements = new PsiElement[]{expr};
    }
    else {
      elements = CodeInsightUtil.findStatementsInRange(file, startOffset, endOffset);
      if (elements.length == 0) {
        final PsiExpression expression = IntroduceVariableBase.getSelectedExpression(project, file, startOffset, endOffset);
        if (expression != null) {
          final PsiType originalType = RefactoringUtil.getTypeByExpressionWithExpectedType(expression);
          if (originalType != null) {
            elements = new PsiElement[]{expression};
          }
        }
      }
    }
    callback.pass(elements);
  }

  private static void invokeOnElements(Project project, Editor editor, PsiFile file, PsiElement[] elements) {
    final ExtractMethodProcessor processor = getProcessor(elements, project, file, editor, true);
    if (processor != null) {
      invokeOnElements(project, editor, processor, true);
    }
  }

  private static boolean invokeOnElements(final Project project, final Editor editor, @NotNull final ExtractMethodProcessor processor, final boolean directTypes) {
    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, processor.getTargetClass().getContainingFile())) return false;
    if (processor.showDialog(directTypes)) {
      CommandProcessor.getInstance().executeCommand(project, new Runnable() {
        public void run() {
          PostprocessReformattingAspect.getInstance(project).postponeFormattingInside(new Runnable() {
            public void run() {
              ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                  try {
                    processor.doRefactoring();
                  }
                  catch (IncorrectOperationException e) {
                    LOG.error(e);
                  }
                }
              });
              DuplicatesImpl.processDuplicates(processor, project, editor);
            }
          });
        }
      }, REFACTORING_NAME, null);
      return true;
    }
    return false;
  }

  @Nullable
  private static ExtractMethodProcessor getProcessor(final PsiElement[] elements,
                                                    final Project project,
                                                    final PsiFile file,
                                                    final Editor editor,
                                                    final boolean showErrorMessages) {
    if (elements == null || elements.length == 0) {
      if (showErrorMessages) {
        String message = RefactoringBundle
          .getCannotRefactorMessage(RefactoringBundle.message("selected.block.should.represent.a.set.of.statements.or.an.expression"));
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.EXTRACT_METHOD);
      }
      return null;
    }

    for (PsiElement element : elements) {
      if (element instanceof PsiStatement && RefactoringUtil.isSuperOrThisCall((PsiStatement)element, true, true)) {
        if (showErrorMessages) {
          String message = RefactoringBundle
            .getCannotRefactorMessage(RefactoringBundle.message("selected.block.contains.invocation.of.another.class.constructor"));
          CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.EXTRACT_METHOD);
        }
        return null;
      }
    }

    final ExtractMethodProcessor processor =
      new ExtractMethodProcessor(project, editor, elements, null, REFACTORING_NAME, "", HelpID.EXTRACT_METHOD);
    processor.setShowErrorDialogs(showErrorMessages);
    try {
      if (!processor.prepare()) return null;
    }
    catch (PrepareFailedException e) {
      if (showErrorMessages) {
        CommonRefactoringUtil.showErrorHint(project, editor, e.getMessage(), REFACTORING_NAME, HelpID.EXTRACT_METHOD);
        highlightPrepareError(e, file, editor, project);
      }
      return null;
    }
    return processor;
  }

  public static void highlightPrepareError(PrepareFailedException e, PsiFile file, Editor editor, final Project project) {
    if (e.getFile() == file) {
      final TextRange textRange = e.getTextRange();
      final HighlightManager highlightManager = HighlightManager.getInstance(project);
      EditorColorsManager colorsManager = EditorColorsManager.getInstance();
      TextAttributes attributes = colorsManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
      highlightManager.addRangeHighlight(editor, textRange.getStartOffset(), textRange.getEndOffset(), attributes, true, null);
      final LogicalPosition logicalPosition = editor.offsetToLogicalPosition(textRange.getStartOffset());
      editor.getScrollingModel().scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE);
      WindowManager.getInstance().getStatusBar(project).setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
    }
  }

  @Nullable
  public static ExtractMethodProcessor getProcessor(final Project project,
                                                    final PsiElement[] elements,
                                                    final PsiFile file,
                                                    final boolean openEditor) {
    return getProcessor(elements, project, file, openEditor ? openEditor(project, file) : null, false);
  }

  public static boolean invokeOnElements(final Project project, @NotNull final ExtractMethodProcessor processor, final PsiFile file, final boolean directTypes) {
    return invokeOnElements(project, openEditor(project, file), processor, directTypes);
  }

  @Nullable
  private static Editor openEditor(final Project project, final PsiFile file) {
    final VirtualFile virtualFile = file.getVirtualFile();
    LOG.assertTrue(virtualFile != null);
    final OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project, virtualFile);
    return FileEditorManager.getInstance(project).openTextEditor(fileDescriptor, false);
  }
}