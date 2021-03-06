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

package com.intellij.ide.projectView.impl;

import com.intellij.ProjectTopics;
import com.intellij.ide.CopyPasteUtil;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarksListener;
import com.intellij.ide.projectView.BaseProjectTreeBuilder;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewPsiTreeChangeListener;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Alarm;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;

public class ProjectTreeBuilder extends BaseProjectTreeBuilder {
  private final ProjectViewPsiTreeChangeListener myPsiTreeChangeListener;
  private final MyFileStatusListener myFileStatusListener;

  private final CopyPasteUtil.DefaultCopyPasteListener myCopyPasteListener;
  //private final PropertiesFileListener myPropertiesFileListener;
  private final WolfTheProblemSolver.ProblemListener myProblemListener;

  public ProjectTreeBuilder(final Project project, JTree tree, DefaultTreeModel treeModel, Comparator<NodeDescriptor> comparator, ProjectAbstractTreeStructureBase treeStructure) {
    super(project, tree, treeModel, treeStructure, comparator);

    final MessageBusConnection connection = project.getMessageBus().connect(this);

    myPsiTreeChangeListener = createPsiTreeChangeListener(myProject);
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      public void beforeRootsChange(ModuleRootEvent event) {
      }
      public void rootsChanged(ModuleRootEvent event) {
        getUpdater().addSubtreeToUpdate(getRootNode());
      }
    });

    connection.subscribe(BookmarksListener.TOPIC, new MyBookmarksListener());

    PsiManager.getInstance(myProject).addPsiTreeChangeListener(myPsiTreeChangeListener);
    myFileStatusListener = new MyFileStatusListener();
    FileStatusManager.getInstance(myProject).addFileStatusListener(myFileStatusListener);
    myCopyPasteListener = new CopyPasteUtil.DefaultCopyPasteListener(getUpdater());
    CopyPasteManager.getInstance().addContentChangedListener(myCopyPasteListener);

    /*myPropertiesFileListener = new PropertiesFileListener();
    final PropertiesFilesManager propertiesFilesManager = PropertiesFilesManager.getInstance();
    if (propertiesFilesManager != null) {
      propertiesFilesManager.addPropertiesFileListener(myPropertiesFileListener);
    }*/
    myProblemListener = new MyProblemListener();
    WolfTheProblemSolver.getInstance(project).addProblemListener(myProblemListener);

    //todo: kirillk turn into a real test
    setCanYieldUpdate(!ApplicationManager.getApplication().isUnitTestMode());

    initRootNode();
  }

  public final void dispose() {
    super.dispose();
    PsiManager.getInstance(myProject).removePsiTreeChangeListener(myPsiTreeChangeListener);
    FileStatusManager.getInstance(myProject).removeFileStatusListener(myFileStatusListener);
    CopyPasteManager.getInstance().removeContentChangedListener(myCopyPasteListener);
   /* final PropertiesFilesManager propertiesFilesManager = PropertiesFilesManager.getInstance();
    if (propertiesFilesManager != null) {
      propertiesFilesManager.removePropertiesFileListener(myPropertiesFileListener);
    }*/
    WolfTheProblemSolver.getInstance(myProject).removeProblemListener(myProblemListener);
  }

  /**
   * Creates psi tree changes listener. This method will be invoked in constructor of ProjectTreeBuilder
   * thus builder object will be not completely initialized
   * @param project Project
   * @return Listener
   */
  protected ProjectViewPsiTreeChangeListener createPsiTreeChangeListener(final Project project) {
    return new ProjectTreeBuilderPsiListener(project);
  }

  protected class ProjectTreeBuilderPsiListener extends ProjectViewPsiTreeChangeListener {
    public ProjectTreeBuilderPsiListener(final Project project) {
      super(project);
    }

    protected DefaultMutableTreeNode getRootNode(){
      return ProjectTreeBuilder.this.getRootNode();
    }

    protected AbstractTreeUpdater getUpdater() {
      return ProjectTreeBuilder.this.getUpdater();
    }

    protected boolean isFlattenPackages(){
      return ((AbstractProjectTreeStructure)getTreeStructure()).isFlattenPackages();
    }
  }

  private final class MyBookmarksListener implements BookmarksListener {
    public void bookmarkAdded(Bookmark b) {
      updateForFile(b.getFile());
    }

    public void bookmarkRemoved(Bookmark b) {
      updateForFile(b.getFile());
    }

    private void updateForFile(VirtualFile file) {
      PsiElement element = findPsi(file);
      if (element != null) {
        getUpdater().addSubtreeToUpdateByElement(element);
      }
    }
  }

  private final class MyFileStatusListener implements FileStatusListener {
    public void fileStatusesChanged() {
      getUpdater().addSubtreeToUpdate(getRootNode());
    }

    public void fileStatusChanged(@NotNull VirtualFile vFile) {
      PsiElement element = findPsi(vFile);

      final boolean fileAdded = getUpdater().addSubtreeToUpdateByElement(element);
      if (!fileAdded) {
        if (element instanceof PsiFile) {
          getUpdater().addSubtreeToUpdateByElement(((PsiFile)element).getContainingDirectory());
        }
        else {
          getUpdater().addSubtreeToUpdate(getRootNode());
        }
      }
    }
  }

  private PsiElement findPsi(VirtualFile vFile) {
    PsiElement element;
    PsiManager psiManager = PsiManager.getInstance(myProject);
    if (vFile.isDirectory()) {
      element = psiManager.findDirectory(vFile);
    }
    else {
      element = psiManager.findFile(vFile);
    }
    return element;
  }

 /* private class PropertiesFileListener implements PropertiesFilesManager.PropertiesFileListener {
    public void fileAdded(VirtualFile propertiesFile) {
      fileChanged(propertiesFile, null);
    }

    public void fileRemoved(VirtualFile propertiesFile) {
      fileChanged(propertiesFile, null);
    }

    public void fileChanged(VirtualFile propertiesFile, final VirtualFilePropertyEvent event) {
      if (!myProject.isDisposed()) {
        VirtualFile parent = propertiesFile.getParent();
        if (parent != null && parent.isValid()) {
          PsiDirectory dir = PsiManager.getInstance(myProject).findDirectory(parent);
          myUpdater.addSubtreeToUpdateByElement(dir);
        }
      }
    }
  }*/

  private class MyProblemListener extends WolfTheProblemSolver.ProblemListener {
    private final Alarm myUpdateProblemAlarm = new Alarm();
    private final Collection<VirtualFile> myFilesToRefresh = new THashSet<VirtualFile>();

    public void problemsAppeared(VirtualFile file) {
      queueUpdate(file);
    }

    public void problemsDisappeared(VirtualFile file) {
      queueUpdate(file);
    }

    private void queueUpdate(final VirtualFile fileToRefresh) {
      synchronized (myFilesToRefresh) {
        if (myFilesToRefresh.add(fileToRefresh)) {
          myUpdateProblemAlarm.cancelAllRequests();
          myUpdateProblemAlarm.addRequest(new Runnable() {
            public void run() {
              if (!myProject.isOpen()) return;
              Set<VirtualFile> filesToRefresh;
              synchronized (myFilesToRefresh) {
                filesToRefresh = new THashSet<VirtualFile>(myFilesToRefresh);
              }
              updateNodesContaining(filesToRefresh, getRootNode());
              synchronized (myFilesToRefresh) {
                myFilesToRefresh.removeAll(filesToRefresh);
              }
            }
          }, 200, ModalityState.NON_MODAL);
        }
      }
    }
  }

  private void updateNodesContaining(final Collection<VirtualFile> filesToRefresh, final DefaultMutableTreeNode rootNode) {
    if (!(rootNode.getUserObject() instanceof ProjectViewNode)) return;
    ProjectViewNode node = (ProjectViewNode)rootNode.getUserObject();
    Collection<VirtualFile> containingFiles = null;
    for (VirtualFile virtualFile : filesToRefresh) {
      if (!virtualFile.isValid()) {
        addSubtreeToUpdate(rootNode); // file must be deleted
        return;
      }
      if (node.contains(virtualFile)) {
        if (containingFiles == null) containingFiles = new SmartList<VirtualFile>();
        containingFiles.add(virtualFile);
      }
    }
    if (containingFiles != null) {
      updateNode(rootNode);
      Enumeration children = rootNode.children();
      while (children.hasMoreElements()) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
        updateNodesContaining(containingFiles, child);
      }
    }
  }
}
