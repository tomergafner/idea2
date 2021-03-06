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
package com.intellij.packaging.impl.artifacts;

import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.artifacts.ArtifactModel;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.facet.impl.DefaultFacetsProvider;
import org.jetbrains.annotations.NotNull;

/**
* @author nik
*/
class DefaultPackagingElementResolvingContext implements PackagingElementResolvingContext {
  private final Project myProject;
  private final DefaultModulesProvider myModulesProvider;

  public DefaultPackagingElementResolvingContext(Project project) {
    myProject = project;
    myModulesProvider = new DefaultModulesProvider(myProject);
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public ArtifactModel getArtifactModel() {
    return ArtifactManager.getInstance(myProject);
  }

  @NotNull
  public ModulesProvider getModulesProvider() {
    return myModulesProvider;
  }

  @NotNull
  public FacetsProvider getFacetsProvider() {
    return DefaultFacetsProvider.INSTANCE;
  }
}
