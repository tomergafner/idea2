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
package com.intellij.openapi.diff.impl.external;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.DiffContent;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.DiffTool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class CompositeDiffTool implements DiffTool {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.diff.impl.external.CompositeDiffTool");
  private final List<DiffTool> myTools;

  public CompositeDiffTool(List<DiffTool> tools) {
    myTools = new ArrayList<DiffTool>(tools);
  }

  public void show(DiffRequest data) {
    checkDiffData(data);
    DiffTool tool = chooseTool(data);
    if (tool != null) tool.show(data);
    else LOG.error("No diff tool found which is able to handle request " + data);
  }

  public boolean canShow(DiffRequest data) {
    checkDiffData(data);
    return chooseTool(data) != null;
  }

  @Nullable
  private DiffTool chooseTool(DiffRequest data) {
    for (DiffTool tool : myTools) {
      if (tool.canShow(data)) return tool;
    }
    return null;
  }

  private static void checkDiffData(@NotNull DiffRequest data) {
    DiffContent[] contents = data.getContents();
    for (DiffContent content : contents) {
      LOG.assertTrue(content != null);
    }
  }
}
