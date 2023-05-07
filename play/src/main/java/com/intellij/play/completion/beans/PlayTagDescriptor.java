package com.intellij.play.completion.beans;

import org.jetbrains.annotations.NotNull;

public class PlayTagDescriptor {
  private final String tagName;
  private final boolean isClosingTag;
  private final NameValueDescriptor[] myDescriptors;

  public static final String CLOSING_TAG = "/}";
  public static final String TAG_START = "#{";
  public static final String END_TAG_START = "#{/";
  public static final String TAG_END = "}";

  public static PlayTagDescriptor create(@NotNull String tagName, NameValueDescriptor... descriptors) {
    return new PlayTagDescriptor(tagName, descriptors);
  }

  public static PlayTagDescriptor create(@NotNull String tagName, boolean closingTag, NameValueDescriptor... descriptors) {
    return new PlayTagDescriptor(tagName, closingTag, descriptors);
  }

  public PlayTagDescriptor(@NotNull String tagName, NameValueDescriptor... descriptors) {
    this(tagName, false, descriptors);
  }

  public PlayTagDescriptor(@NotNull String tagName, boolean closingTag, NameValueDescriptor... descriptors) {
    this.tagName = tagName;
    isClosingTag = closingTag;
    myDescriptors = descriptors;
  }

  public NameValueDescriptor[] getDescriptors() {
    return myDescriptors;
  }

  public String getTagName() {
    return tagName;
  }

  public boolean isClosingTag() {
    return isClosingTag;
  }

  public String getTailText() {
    return " " + (isClosingTag() ? CLOSING_TAG : TAG_END + END_TAG_START + getTagName() + TAG_END);
  }

  public String getPresentableText() {
    return TAG_START + getTagName() + (isClosingTag() ? "" : " ...") + getTailText();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlayTagDescriptor that)) return false;

    if (isClosingTag != that.isClosingTag) return false;
    if (tagName != null ? !tagName.equals(that.tagName) : that.tagName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = tagName != null ? tagName.hashCode() : 0;
    result = 31 * result + (isClosingTag ? 1 : 0);
    return result;
  }
}
