// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.changes.ui;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author irengrig
 */
public class DummyChangesViewContentManager implements ChangesViewContentI {
  @Override
  public void setUp(ToolWindow toolWindow) {

  }

  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public void addContent(final Content content) {
  }

  @Override
  public void removeContent(final Content content) {
  }

  @Override
  public void setSelectedContent(final Content content) {
  }

  @Override
  public void setSelectedContent(@NotNull Content content, boolean requestFocus) {

  }

  @Override
  public <T> T getActiveComponent(final Class<T> aClass) {
    return null;
  }

  @Override
  public void selectContent(final String tabName) {
  }

  @NotNull
  @Override
  public List<Content> findContents(@NotNull Predicate<Content> predicate) {
    return Collections.emptyList();
  }
}
