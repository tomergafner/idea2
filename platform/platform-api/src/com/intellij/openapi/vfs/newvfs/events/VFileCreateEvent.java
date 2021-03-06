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
 * @author max
 */
package com.intellij.openapi.vfs.newvfs.events;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NonNls;

public class VFileCreateEvent extends VFileEvent {
  private final VirtualFile myParent;
  private final boolean myDirectory;
  private final String myChildName;

  public VFileCreateEvent(final Object requestor, final VirtualFile parent, final String childName, boolean isDirectory, boolean isFromRefresh) {
    super(requestor, isFromRefresh);
    myChildName = childName;
    myParent = parent;
    myDirectory = isDirectory;
  }

  public String getChildName() {
    return myChildName;
  }

  public boolean isDirectory() {
    return myDirectory;
  }

  public VirtualFile getParent() {
    return myParent;
  }

  @NonNls
  public String toString() {
    return "VfsEvent[create " + (isDirectory() ? "dir " : "file ") + myChildName +  " in " + myParent.getUrl() + "]";
  }

  public String getPath() {
    return myParent.getPath() + "/" + myChildName;
  }

  public VirtualFileSystem getFileSystem() {
    return myParent.getFileSystem();
  }

  public boolean isValid() {
    return myParent.isValid() && myParent.findChild(myChildName) == null;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final VFileCreateEvent event = (VFileCreateEvent)o;

    if (myDirectory != event.myDirectory) return false;
    if (!myChildName.equals(event.myChildName)) return false;
    if (!myParent.equals(event.myParent)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myParent.hashCode();
    result = 31 * result + (myDirectory ? 1 : 0);
    result = 31 * result + myChildName.hashCode();
    return result;
  }
}
