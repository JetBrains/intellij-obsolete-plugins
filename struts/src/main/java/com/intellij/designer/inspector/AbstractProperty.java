package com.intellij.designer.inspector;

import com.intellij.openapi.util.Key;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;

/**
 * @author spleaner
 */
public abstract class AbstractProperty<N, V> extends DefaultMutableTreeNode implements Property<N, V> {

  private final THashMap myUserData = new THashMap();
  private N myName;
  private V myValue;

  protected AbstractProperty(final N name, final V value) {
    myName = name;
    myValue = value;
  }

  @Override
  public void accept(final PropertyVisitor visitor) {
    visitor.visitProperty(this);

    final Enumeration<TreeNode> enumeration = children();
    while (enumeration.hasMoreElements()) {
      final AbstractProperty each = (AbstractProperty)enumeration.nextElement();
      each.accept(visitor);
    }
  }

  @Override
  public Property getParentProperty() {
    return (Property) getParent();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public <T> T getUserData(@NotNull Key<T> key) {
    return (T) myUserData.get(key);
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public <T> void putUserData(@NotNull Key<T> key, T value) {
    myUserData.put(key, value);
  }

  @Override
  public N getName() {
    return myName;
  }

  @Override
  public V getValue() {
    return myValue;
  }

  public void setName(final N name) {
    myName = name;
  }

  public void setValue(final V value) {
    myValue = value;
  }


  @Override
  public void add(MutableTreeNode newChild) {
    int position = getChildCount();

    if (newChild instanceof Comparable) {
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final TreeNode node = getChildAt(i);
        if (!(node instanceof Comparable)) {
          position = i;
          break;
        }

        Comparable c = (Comparable)node;
        if (c.compareTo(newChild) > 0) {
          position = i;
          break;
        }
      }
    }

    insert(newChild, position);
  }

  public static AbstractProperty root() {
    return new AbstractProperty("root_name", "root_value") {};
  }
}
