package com.cosium.code.format.formatter;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.Collections;
import java.util.Set;

/** @author Réda Housni Alaoui */
public class LineRanges {

  private static final LineRanges ALL =
      new LineRanges(TreeRangeSet.create(Collections.singleton(Range.all())));

  private final RangeSet<Integer> rangeSet;

  private LineRanges(RangeSet<Integer> rangeSet) {
    if (rangeSet.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one range");
    }

    this.rangeSet = rangeSet;
  }

  public static LineRanges all() {
    return ALL;
  }

  public static LineRanges of(Set<Range<Integer>> ranges) {
    return new LineRanges(TreeRangeSet.create(ranges));
  }

  public static LineRanges singleton(Range<Integer> range) {
    return new LineRanges(TreeRangeSet.create(Collections.singleton(range)));
  }

  public boolean isAll() {
    return this == ALL;
  }

  public RangeSet<Integer> rangeSet() {
    return rangeSet;
  }

  public static LineRanges concat(LineRanges lineRanges1, LineRanges lineRanges2) {
    if (lineRanges1.isAll()) {
      return lineRanges1;
    }
    if (lineRanges2.isAll()) {
      return lineRanges2;
    }

    RangeSet<Integer> newRangeSet = TreeRangeSet.create();
    newRangeSet.addAll(lineRanges1.rangeSet);
    newRangeSet.addAll(lineRanges2.rangeSet);

    return new LineRanges(newRangeSet);
  }

  @Override
  public String toString() {
    return rangeSet.toString();
  }
}
