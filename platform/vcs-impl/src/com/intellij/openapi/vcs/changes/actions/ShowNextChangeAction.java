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
package com.intellij.openapi.vcs.changes.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diff.SimpleDiffRequest;
import com.intellij.openapi.diff.DiffViewer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.ChangeRequestChain;

/**
 * @author yole
*/
public class ShowNextChangeAction extends AnAction implements DumbAware {
  public ShowNextChangeAction() {
    setEnabledInModalContext(true);
  }

  public void update(AnActionEvent e) {
    final ChangeRequestChain chain = e.getData(VcsDataKeys.DIFF_REQUEST_CHAIN);
    e.getPresentation().setEnabled((chain != null) && (chain.canMoveForward()));
  }

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(PlatformDataKeys.PROJECT);
    final ChangeRequestChain chain = e.getData(VcsDataKeys.DIFF_REQUEST_CHAIN);
    if ((project == null) || (chain == null) || (! chain.canMoveForward())) {
      return;
    }

    final DiffViewer diffViewer = e.getData(PlatformDataKeys.DIFF_VIEWER);
    if (diffViewer == null) return;
    final SimpleDiffRequest request = chain.moveForward();
    if (request != null) {
      diffViewer.setDiffRequest(request);
    }
  }
}