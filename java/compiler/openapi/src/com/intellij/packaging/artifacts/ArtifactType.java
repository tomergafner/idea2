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
package com.intellij.packaging.artifacts;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.ui.ArtifactValidationManager;
import com.intellij.packaging.ui.PackagingSourceItem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author nik
 */
public abstract class ArtifactType {
  public static final ExtensionPointName<ArtifactType> EP_NAME = ExtensionPointName.create("com.intellij.packaging.artifactType");
  private final String myId;
  private final String myTitle;

  protected ArtifactType(@NonNls String id, String title) {
    myId = id;
    myTitle = title;
  }

  public final String getId() {
    return myId;
  }

  public String getPresentableName() {
    return myTitle;
  }

  @NotNull
  public abstract Icon getIcon();

  @Nullable
  public abstract String getDefaultPathFor(@NotNull PackagingSourceItem sourceItem);

  @Nullable
  public abstract String getDefaultPathFor(@NotNull PackagingElement<?> element, @NotNull PackagingElementResolvingContext context);

  public boolean isSuitableItem(@NotNull PackagingSourceItem sourceItem) {
    return true;
  }

  public static ArtifactType[] getAllTypes() {
    return Extensions.getExtensions(EP_NAME);
  }

  @Nullable
  public static ArtifactType findById(@NotNull @NonNls String id) {
    for (ArtifactType type : getAllTypes()) {
      if (id.equals(type.getId())) {
        return type;
      }
    }
    return null;
  }

  @NotNull
  public abstract CompositePackagingElement<?> createRootElement(@NotNull String artifactName);

  public void checkRootElement(@NotNull CompositePackagingElement<?> rootElement, @NotNull Artifact artifact, @NotNull ArtifactValidationManager manager) {
  }

  @Nullable
  public List<? extends PackagingElement<?>> getSubstitution(@NotNull Artifact artifact, @NotNull PackagingElementResolvingContext context,
                                                             @NotNull ArtifactType parentType) {
    return null;
  }
}
