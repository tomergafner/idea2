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

package com.intellij.openapi.vcs.changes;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Manages asynchronous file status updating for files under VCS.
 *
 * @author max
 * @since 6.0
 */
public abstract class VcsDirtyScopeManager {
  public static VcsDirtyScopeManager getInstance(Project project) {
    return project.getComponent(VcsDirtyScopeManager.class);
  }

  public static VcsDirtyScopeManager getInstanceChecked(final Project project) {
    return ApplicationManager.getApplication().runReadAction(new Computable<VcsDirtyScopeManager>() {
      public VcsDirtyScopeManager compute() {
        if (project.isDisposed()) throw new ProcessCanceledException();
        return project.getComponent(VcsDirtyScopeManager.class);
      }
    });
  }

  /**
   * Requests an asynchronous file status update for all files in the project.
   */
  public abstract void markEverythingDirty();

  /**
   * Requests an asynchronous file status update for the specified virtual file. Must be called from a read action.
   *
   * @param file the file for which the status update is requested.
   */
  public abstract void fileDirty(VirtualFile file);

  /**
   * Requests an asynchronous file status update for the specified file path. Must be called from a read action.
   *
   * @param file the file path for which the status update is requested.
   */
  public abstract void fileDirty(FilePath file);

  /**
   * Requests an asynchronous file status update for all files under the specified directory.
   *
   * @param dir the directory for which the file status update is requested.
   * @deprecated Use single-parameter version instead.
   */
  public abstract void dirDirtyRecursively(VirtualFile dir, final boolean scheduleUpdate);

  /**
   * Requests an asynchronous file status update for all files under the specified directory.
   *
   * @param dir the directory for which the file status update is requested.
   */
  public abstract void dirDirtyRecursively(VirtualFile dir);

  public abstract void dirDirtyRecursively(FilePath path);

  public abstract VcsInvalidated retrieveScopes();

  /**
   * Requests an asynchronous file status update for all files specified and under the specified directories
   */
  public abstract boolean filePathsDirty(@Nullable final Collection<FilePath> filesDirty, @Nullable final Collection<FilePath> dirsRecursivelyDirty);

  /**
   * Requests an asynchronous file status update for all files specified and under the specified directories
   */
  public abstract boolean filesDirty(@Nullable final Collection<VirtualFile> filesDirty, @Nullable final Collection<VirtualFile> dirsRecursivelyDirty);
}
