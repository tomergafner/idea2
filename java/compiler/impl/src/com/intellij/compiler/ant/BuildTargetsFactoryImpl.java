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
 * Date: 19-Dec-2006
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.j2ee.BuildExplodedTarget;
import com.intellij.compiler.ant.j2ee.BuildJarTarget;
import com.intellij.compiler.ant.j2ee.CompositeBuildTarget;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.openapi.compiler.make.BuildRecipe;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public class BuildTargetsFactoryImpl extends BuildTargetsFactory {

  public CompositeGenerator createCompositeBuildTarget(final ExplodedAndJarTargetParameters parameters, @NonNls final String targetName,
                                                       final String description, final String depends, @Nullable String jarPath) {
    return new CompositeBuildTarget(parameters, targetName, description, depends, jarPath);
  }

  public Target createBuildExplodedTarget(final ExplodedAndJarTargetParameters parameters, final BuildRecipe buildRecipe, final String description) {
    return new BuildExplodedTarget(parameters, buildRecipe, description);
  }

  public Target createBuildJarTarget(final ExplodedAndJarTargetParameters parameters, final BuildRecipe buildRecipe, final String description) {
    return new BuildJarTarget(parameters, buildRecipe, description);
  }


  public Generator createComment(final String comment) {
    return new Comment(comment);
  }

  @TestOnly
  public GenerationOptions getDefaultOptions(Project project) {
    return new GenerationOptionsImpl(project, true, false, false, true, ArrayUtil.EMPTY_STRING_ARRAY);
  }
}