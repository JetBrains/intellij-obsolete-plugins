// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util.version;

import org.jetbrains.annotations.Nullable;

public class Range<T extends Comparable<T>> {

  protected @Nullable T myStart;
  protected boolean myStartInclusive = true;
  protected @Nullable T myEnd;
  protected boolean myEndInclusive = true;

  public Range() {
  }

  public @Nullable T getStart() {
    return myStart;
  }

  public Range<T> setStart(@Nullable T start) {
    myStart = start;
    return this;
  }

  public boolean isStartInclusive() {
    return myStartInclusive;
  }

  public Range<T> setStartInclusive(boolean startInclusive) {
    myStartInclusive = startInclusive;
    return this;
  }

  public @Nullable T getEnd() {
    return myEnd;
  }

  public Range<T> setEnd(@Nullable T end) {
    myEnd = end;
    return this;
  }

  public boolean isEndInclusive() {
    return myEndInclusive;
  }

  public Range<T> setEndInclusive(boolean endInclusive) {
    myEndInclusive = endInclusive;
    return this;
  }

  public boolean contains(T object) {
    return (myStart == null || myStart.compareTo(object) < (myStartInclusive ? 1 : 0))
           && (myEnd == null || object.compareTo(myEnd) < (myEndInclusive ? 1 : 0));
  }

  @Override
  public String toString() {
    return String.valueOf((myStartInclusive ? '[' : '(')) + myStart + ", " + myEnd + (myEndInclusive ? ']' : ')');
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Range<?> range = (Range<?>)o;

    if (myStartInclusive != range.myStartInclusive) return false;
    if (myEndInclusive != range.myEndInclusive) return false;
    if (myStart != null ? !myStart.equals(range.myStart) : range.myStart != null) return false;
    if (myEnd != null ? !myEnd.equals(range.myEnd) : range.myEnd != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myStart != null ? myStart.hashCode() : 0;
    result = 31 * result + (myStartInclusive ? 1 : 0);
    result = 31 * result + (myEnd != null ? myEnd.hashCode() : 0);
    result = 31 * result + (myEndInclusive ? 1 : 0);
    return result;
  }
}
