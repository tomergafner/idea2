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

/*
 * User: anna
 * Date: 23-Jan-2008
 */
package com.intellij.psi.impl.file;

import com.intellij.ide.IconProvider;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.roots.ui.configuration.IconSet;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DirectoryIconProvider extends IconProvider implements DumbAware {
  public Icon getIcon(@NotNull final PsiElement element, final int flags) {
    if (element instanceof PsiDirectory) {
      final PsiDirectory psiDirectory = (PsiDirectory)element;
      final VirtualFile vFile = psiDirectory.getVirtualFile();
      boolean inTestSource = ProjectRootsUtil.isInTestSource(vFile, psiDirectory.getProject());
      boolean isSourceOrTestRoot = ProjectRootsUtil.isSourceOrTestRoot(vFile, psiDirectory.getProject());
      final boolean isOpen = (flags & Iconable.ICON_FLAG_OPEN) != 0;
      if (isSourceOrTestRoot) {
        return IconSet.getSourceRootIcon(inTestSource, isOpen);
      }
      else {
        return isOpen ? Icons.DIRECTORY_OPEN_ICON : Icons.DIRECTORY_CLOSED_ICON;
      }
    }
    return null;
  }
}