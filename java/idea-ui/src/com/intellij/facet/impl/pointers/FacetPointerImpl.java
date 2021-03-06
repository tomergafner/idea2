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

package com.intellij.facet.impl.pointers;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.facet.pointers.FacetPointer;
import com.intellij.facet.pointers.FacetPointersManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class FacetPointerImpl<F extends Facet> implements FacetPointer<F> {
  private final FacetPointersManagerImpl myManager;
  private String myModuleName;
  private String myFacetTypeId;
  private String myFacetName;
  private F myFacet;

  public FacetPointerImpl(FacetPointersManagerImpl manager, String id) {
    myManager = manager;
    final int i = id.indexOf('/');
    myModuleName = id.substring(0, i);

    final int j = id.lastIndexOf('/');
    myFacetTypeId = id.substring(i + 1, j);
    myFacetName = id.substring(j+1);
  }

  public FacetPointerImpl(FacetPointersManagerImpl manager, final @NotNull F facet) {
    myManager = manager;
    myFacet = facet;
    updateInfo(myFacet);
    registerDisposable();
  }

  public void refresh() {
    findAndSetFacet();

    if (myFacet != null) {
      updateInfo(myFacet);
    }
  }

  private void findAndSetFacet() {
    if (myFacet == null) {
      myFacet = findFacet();
      if (myFacet != null) {
        registerDisposable();
      }
    }
  }

  private void registerDisposable() {
    Disposer.register(myFacet, new Disposable() {
      public void dispose() {
        myManager.dispose(FacetPointerImpl.this);
        myFacet = null;
      }
    });
  }

  private void updateInfo(final @NotNull F facet) {
    myModuleName = facet.getModule().getName();
    myFacetTypeId = facet.getType().getStringId();
    myFacetName = facet.getName();
  }

  @NotNull
  public Project getProject() {
    return myManager.getProject();
  }

  public F getFacet() {
    findAndSetFacet();
    return myFacet;
  }

  @Nullable
  private F findFacet() {
    final Module module = ModuleManager.getInstance(myManager.getProject()).findModuleByName(myModuleName);
    if (module == null) return null;

    final FacetType<F, ?> type = getFacetType();
    if (type == null) return null;

    return FacetManager.getInstance(module).findFacet(type.getId(), myFacetName);
  }

  @Nullable
  public F findFacet(ModulesProvider modulesProvider, FacetsProvider facetsProvider) {
    final Module module = modulesProvider.getModule(myModuleName);
    if (module == null) return null;
    final FacetType<F, ?> type = getFacetType();
    if (type == null) return null;
    return facetsProvider.findFacet(module, type.getId(), myFacetName);
  }

  @NotNull
  public String getModuleName() {
    return myModuleName;
  }

  @NotNull
  public String getFacetName() {
    return myFacetName;
  }

  @NotNull
  public String getId() {
    return FacetPointersManager.constructId(myModuleName, myFacetTypeId, myFacetName);
  }

  @NotNull
  public String getFacetTypeId() {
    return myFacetTypeId;
  }

  @Nullable
  public FacetType<F, ?> getFacetType() {
    //noinspection unchecked
    return FacetTypeRegistry.getInstance().findFacetType(myFacetTypeId);
  }
}
