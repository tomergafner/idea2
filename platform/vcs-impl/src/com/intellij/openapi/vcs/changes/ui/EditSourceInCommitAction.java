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
package com.intellij.openapi.vcs.changes.ui;

import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.pom.Navigatable;
import com.intellij.util.OpenSourceUtil;

import javax.swing.*;

public class EditSourceInCommitAction extends AnAction {
  private final DialogWrapper myDialogWrapper;

  public EditSourceInCommitAction(final DialogWrapper dialogWrapper) {
    super(ActionsBundle.actionText("EditSource"),
          ActionsBundle.actionDescription("EditSource"),
          IconLoader.getIcon("/actions/editSource.png"));
    myDialogWrapper = dialogWrapper;
  }

  public void actionPerformed(AnActionEvent e) {
    final Navigatable[] navigatableArray = e.getData(PlatformDataKeys.NAVIGATABLE_ARRAY);
    if (navigatableArray != null && navigatableArray.length > 0) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          OpenSourceUtil.navigate(navigatableArray, true);
        }
      });
      myDialogWrapper.doCancelAction();
    }
  }
}
