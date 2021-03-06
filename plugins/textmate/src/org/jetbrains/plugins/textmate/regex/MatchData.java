package org.jetbrains.plugins.textmate.regex;

import com.google.common.base.Preconditions;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joni.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MatchData {
  public static final MatchData NOT_MATCHED = new MatchData(false, Collections.emptyList());

  private final boolean matched;
  private final List<TextRange> offsets;

  private MatchData(boolean matched, List<TextRange> offsets) {
    this.matched = matched;
    this.offsets = offsets;
  }

  public static MatchData fromRegion(@Nullable Region matchedRegion) {
    if (matchedRegion != null) {
      List<TextRange> offsets = new ArrayList<>(matchedRegion.numRegs);
      for (int i = 0; i < matchedRegion.numRegs; i++) {
        offsets.add(i, TextRange.create(Math.max(matchedRegion.beg[i], 0), Math.max(matchedRegion.end[i], 0)));
      }
      return new MatchData(true, offsets);
    }
    return NOT_MATCHED;
  }

  public int count() {
    return offsets.size();
  }

  public TextRange byteOffset() {
    return byteOffset(0);
  }

  @NotNull
  public TextRange byteOffset(int group) {
    Preconditions.checkElementIndex(group, offsets.size());
    return offsets.get(group);
  }

  public TextRange charOffset(byte[] stringBytes) {
    return charOffset(stringBytes, 0);
  }

  @NotNull
  public TextRange charOffset(byte[] stringBytes, int group) {
    return RegexUtil.charRangeByByteRange(stringBytes, byteOffset(group));
  }

  public boolean matched() {
    return matched;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MatchData matchData = (MatchData)o;

    if (matched != matchData.matched) return false;
    if (!Objects.equals(offsets, matchData.offsets)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (matched ? 1 : 0);
    result = 31 * result + (offsets != null ? offsets.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "{ matched=" + matched +
           ", offsets=" + offsets +
           '}';
  }
}
