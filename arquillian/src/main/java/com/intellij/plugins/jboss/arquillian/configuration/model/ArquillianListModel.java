package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianListState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianState;
import com.intellij.util.containers.JBIterable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ArquillianListModel<
  ChildState extends ArquillianState,
  ChildModel extends ArquillianModel<ChildState, ChildModel>,
  State extends ArquillianListState<ChildState>,
  Model extends ArquillianListModel<ChildState, ChildModel, State, Model>>
  extends ArquillianModel<State, Model> {

  final protected ArquillianModelCreator<ChildState, ChildModel> childModelCreator;

  ArquillianListenersHolder<ArquillianListModel.Listener<ChildState, ChildModel>> listListenersHolder
    = new ArquillianListenersHolder<>();

  private final List<ChildModel> children;
  private final HashMap<ChildModel, ArquillianListenersHolder.ListenerRemover> listenerRemovers
    = new HashMap<>();

  public ArquillianListenersHolder<ArquillianListModel.Listener<ChildState, ChildModel>> getListListenersHolder() {
    return listListenersHolder;
  }

  public ArquillianListModel(State state, final ArquillianModelCreator<ChildState, ChildModel> childModelCreator) {
    this.childModelCreator = childModelCreator;
    this.children = new ArrayList<>(JBIterable.from(state.getChildren()).transform(
      childModelCreator::createModel).toList());
  }

  @Override
  public boolean hasChanges(State state) {
    if (state.getChildren().size() != children.size()) {
      return true;
    }
    for (int i = 0; i < children.size(); ++i) {
      ChildModel childModel = children.get(i);
      ChildState childState = state.getChildren().get(i);
      if (childModel.hasChanges(childState)) {
        return true;
      }
    }
    return false;
  }

  public void addItem(final ChildModel childModel) {
    addItem(childModel, children.size());
  }

  public void addItem(final ChildModel childModel, final int index) {
    children.add(index, childModel);
    listListenersHolder.notifyListeners(listener -> listener.itemAdded(childModel, index));
    notifyMeChanged();
    listenerRemovers.put(childModel, childModel.addChangeListener(new ArquillianModel.Listener<>() {
      @Override
      public void itemChanged(ChildModel model) {
        listListenersHolder.notifyListeners(listener -> listener.itemChanged(childModel));
        notifyMeChanged();
      }
    }));
  }

  public void removeItem(final ChildModel childModel) {
    final int index = children.indexOf(childModel);
    if (index == -1) {
      return;
    }
    children.remove(childModel);
    ArquillianListenersHolder.ListenerRemover listenerRemover = listenerRemovers.remove(childModel);
    if (listenerRemover != null) {
      listenerRemover.close();
    }
    listListenersHolder.notifyListeners(listener -> listener.itemRemoved(childModel, index));
    notifyMeChanged();
  }

  public List<ChildModel> getChildren() {
    return children;
  }

  public List<ChildState> getChildrenStates() {
    return JBIterable.from(children).transform(ArquillianModel::getCurrentState).toList();
  }

  public interface Listener<ChildState extends ArquillianState, ChildModel extends ArquillianModel<ChildState, ChildModel>>
    extends ArquillianModel.Listener<ChildState, ChildModel> {
    void itemAdded(ChildModel item, int index);

    void itemRemoved(ChildModel item, int index);
  }

  public ListModel createListModel() {
    return new ListModel() {
      private final HashMap<ListDataListener, ArquillianListenersHolder.ListenerRemover> removers
        = new HashMap<>();

      @Override
      public int getSize() {
        return getChildren().size();
      }

      @Override
      public Object getElementAt(int index) {
        return getChildren().get(index);
      }

      @Override
      public void addListDataListener(final ListDataListener l) {
        ArquillianListenersHolder.ListenerRemover remover = getListListenersHolder().addListener(new Listener<>() {
          @Override
          public void itemAdded(ChildModel item, int index) {
            l.intervalAdded(new ListDataEvent(item, ListDataEvent.INTERVAL_ADDED, index, index));
          }

          @Override
          public void itemRemoved(ChildModel item, int index) {
            l.intervalRemoved(new ListDataEvent(item, ListDataEvent.INTERVAL_REMOVED, index, index));
          }

          @Override
          public void itemChanged(ChildModel item) {
            int index = getChildren().indexOf(item);
            l.contentsChanged(new ListDataEvent(item, ListDataEvent.CONTENTS_CHANGED, index, index));
          }
        });
        removers.put(l, remover);
      }

      @Override
      public void removeListDataListener(ListDataListener l) {
        ArquillianListenersHolder.ListenerRemover remover = removers.remove(l);
        if (remover != null) {
          remover.close();
        }
      }
    };
  }
}
