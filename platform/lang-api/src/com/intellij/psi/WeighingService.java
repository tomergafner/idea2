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
package com.intellij.psi;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyedExtensionCollector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author peter
 */
public class WeighingService {
  private static final KeyedExtensionCollector<Weigher,Key> COLLECTOR = new KeyedExtensionCollector<Weigher, Key>("com.intellij.weigher") {
    protected String keyToString(final Key key) {
      return key.toString();
    }
  };

  private WeighingService() {
  }

  @NotNull
  public static <T,Loc> WeighingComparable<T,Loc> weigh(Key<? extends Weigher<T,Loc>> key, T element, Loc location) {
    final List<Weigher> weighers = COLLECTOR.forKey(key);
    return new WeighingComparable<T,Loc>(element, location, weighers.toArray(new Weigher[weighers.size()]));
  }

}
