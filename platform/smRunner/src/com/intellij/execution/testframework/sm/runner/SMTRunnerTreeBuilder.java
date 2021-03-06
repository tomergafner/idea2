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
package com.intellij.execution.testframework.sm.runner;

import com.intellij.execution.testframework.ui.AbstractTestTreeBuilder;
import com.intellij.ide.util.treeView.IndexComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.StatusBarProgress;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author: Roman Chernyatchik
 */
public class SMTRunnerTreeBuilder extends AbstractTestTreeBuilder {
  public SMTRunnerTreeBuilder(final JTree tree, final SMTRunnerTreeStructure structure) {
    super(tree,
          new DefaultTreeModel(new DefaultMutableTreeNode(structure.getRootElement())),
          structure,
          IndexComparator.INSTANCE);
    
    initRootNode();
  }

  public SMTRunnerTreeStructure getRTestUnitTreeStructure() {
    return ((SMTRunnerTreeStructure)getTreeStructure()) ;
  }

  public void updateTestsSubtree(final SMTestProxy parentTestProxy) {
    final AbstractTreeUpdater updater = getUpdater();
    if (updater != null) {
      updater.addSubtreeToUpdateByElement(parentTestProxy);
    }
  }


  protected boolean isAutoExpandNode(final NodeDescriptor nodeDescriptor) {
    final AbstractTreeStructure treeStructure = getTreeStructure();
    final Object rootElement = treeStructure.getRootElement();
    final Object nodeElement = nodeDescriptor.getElement();

    if (nodeElement == rootElement) {
      return true;
    }

    if (((SMTestProxy)nodeElement).getParent() == rootElement
        && ((SMTestProxy)rootElement).getChildren().size() == 1){
      return true;
    }
    return false;
  }

  protected boolean isAlwaysShowPlus(final NodeDescriptor descriptor) {
    //TODO[romeo] move to base class
    return false;
  }

  @NotNull
  protected ProgressIndicator createProgressIndicator() {
    //TODO[romeo] maybe move to base class
    return new StatusBarProgress();
  }

  protected boolean isSmartExpand() {
    //TODO[romeo] move to base class
    return false;
  }

  /**
   * for java unit tests
   */
  public void performUpdate() {
    getUpdater().performUpdate();
  }  
}
