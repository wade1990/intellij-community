// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.collectors.fus.ui;

import com.intellij.internal.statistic.beans.MetricEvent;
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector;
import com.intellij.ui.JreHiDpiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * @author tav
 */
public class HiDPIModeUsagesCollector extends ApplicationUsagesCollector {
  @NotNull
  @Override
  public Set<MetricEvent> getMetrics() {
    String mode = JreHiDpiUtil.isJreHiDPIEnabled() ? "per_monitor_dpi" : "system_dpi";
    return Collections.singleton(new MetricEvent(mode));
  }

  @NotNull
  @Override
  public String getGroupId() {
    return "ui.hidpi.mode";
  }
}
