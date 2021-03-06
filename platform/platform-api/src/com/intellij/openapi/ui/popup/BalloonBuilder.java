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
package com.intellij.openapi.ui.popup;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionListener;

public interface BalloonBuilder {

  @NotNull
  BalloonBuilder setPreferredPosition(Balloon.Position position);

  @NotNull
  BalloonBuilder setBorderColor(@NotNull Color color);

  @NotNull
  BalloonBuilder setFillColor(@NotNull Color color);

  @NotNull
  BalloonBuilder setHideOnClickOutside(boolean hide);

  @NotNull
  BalloonBuilder setHideOnKeyOutside(boolean hide);

  @NotNull
  BalloonBuilder setShowCallout(boolean show);

  @NotNull
  BalloonBuilder setCloseButtonEnabled(boolean enabled);

  @NotNull
  BalloonBuilder setFadeoutTime(long fadeoutTime);

  @NotNull
  BalloonBuilder setHideOnFrameResize(boolean hide);

  @NotNull
  Balloon createBalloon();

  @NotNull
  BalloonBuilder setClickHandler(ActionListener listener, boolean closeOnClick);

}