/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.jetbrains.python;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.MockSdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.jetbrains.python.codeInsight.typing.PyTypeShed;
import com.jetbrains.python.codeInsight.userSkeletons.PyUserSkeletonsUtil;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.PythonSdkUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;

/**
 * @author yole
 */
public class PythonMockSdk {
  @NonNls private static final String MOCK_SDK_NAME = "Mock Python SDK";

  private PythonMockSdk() {
  }

  public static Sdk create(final String version, @NotNull final VirtualFile... additionalRoots) {
    final String mock_path = PythonTestUtil.getTestDataPath() + "/MockSdk" + version + "/";

    String sdkHome = new File(mock_path, "bin/python" + version).getPath();
    SdkType sdkType = PythonSdkType.getInstance();

    MultiMap<OrderRootType, VirtualFile> roots = MultiMap.create();
    OrderRootType classes = OrderRootType.CLASSES;

    ContainerUtil.putIfNotNull(classes, LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(mock_path, "Lib")), roots);

    ContainerUtil.putIfNotNull(classes, PyUserSkeletonsUtil.getUserSkeletonsDirectory(), roots);

    final LanguageLevel level = LanguageLevel.fromPythonVersion(version);
    final VirtualFile typeShedDir = PyTypeShed.INSTANCE.getDirectory();
    PyTypeShed.INSTANCE
      .findRootsForLanguageLevel(level)
      .forEach(path -> ContainerUtil.putIfNotNull(classes, typeShedDir.findFileByRelativePath(path), roots));

    String mock_stubs_path = mock_path + PythonSdkUtil.SKELETON_DIR_NAME;
    ContainerUtil.putIfNotNull(classes, LocalFileSystem.getInstance().refreshAndFindFileByPath(mock_stubs_path), roots);

    roots.putValues(classes, Arrays.asList(additionalRoots));

    MockSdk sdk = new MockSdk(MOCK_SDK_NAME + " " + version, sdkHome, "Python " + version + " Mock SDK", roots, sdkType);

    // com.jetbrains.python.psi.resolve.PythonSdkPathCache.getInstance() corrupts SDK, so have to clone
    return sdk.clone();
  }
}
