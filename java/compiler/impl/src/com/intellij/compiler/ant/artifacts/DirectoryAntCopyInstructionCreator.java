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
package com.intellij.compiler.ant.artifacts;

import com.intellij.compiler.ant.Generator;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.taskdefs.Copy;
import com.intellij.compiler.ant.taskdefs.FileSet;
import com.intellij.compiler.ant.taskdefs.Mkdir;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class DirectoryAntCopyInstructionCreator implements AntCopyInstructionCreator {
  private String myOutputDirectory;

  public DirectoryAntCopyInstructionCreator(String outputDirectory) {
    myOutputDirectory = outputDirectory;
  }

  public String getOutputDirectory() {
    return myOutputDirectory;
  }

  @NotNull
  public Tag createDirectoryContentCopyInstruction(@NotNull String dirPath) {
    final Copy copy = new Copy(myOutputDirectory);
    copy.add(new FileSet(dirPath));
    return copy;
  }

  @NotNull
  public Tag createFileCopyInstruction(@NotNull String filePath, String outputFileName) {
    return new Copy(filePath, myOutputDirectory + "/" + outputFileName);
  }

  @NotNull
  public AntCopyInstructionCreator subFolder(String directoryName) {
    return new DirectoryAntCopyInstructionCreator(myOutputDirectory + "/" + directoryName);
  }

  public Generator createSubFolderCommand(String directoryName) {
    return new Mkdir(myOutputDirectory + "/" + directoryName);
  }
}
