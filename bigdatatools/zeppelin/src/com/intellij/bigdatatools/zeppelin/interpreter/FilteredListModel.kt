package com.intellij.bigdatatools.zeppelin.interpreter

import javax.swing.AbstractListModel

class FilteredListModel<T>(private val idGetter: (T) -> String) : AbstractListModel<T>() {

  var filter: ((T) -> Boolean)? = null
    set(value) {
      field = value
      // To perform filtering.
      data = data
    }

  var filteredData: List<T> = emptyList()
    private set

  var data: List<T> = emptyList()
    set(value) {
      field = value.sortedBy { idGetter(it).lowercase() }

      val localFilter = filter
      val newFilteredData = if (localFilter == null) {
        field
      }
      else {
        field.filter(localFilter)
      }
      val oldFilteredData = filteredData
      filteredData = newFilteredData

      if (oldFilteredData.size > newFilteredData.size) {
        fireIntervalRemoved(this, newFilteredData.size, oldFilteredData.size)
      }
      else if (oldFilteredData.size < newFilteredData.size) {
        fireIntervalAdded(this, oldFilteredData.size, newFilteredData.size)
      }

      val oldIterator = oldFilteredData.iterator()
      val newIterator = newFilteredData.iterator()
      var index = 0
      while (oldIterator.hasNext() && newIterator.hasNext()) {
        val oldValue = oldIterator.next()
        val newValue = newIterator.next()

        if (oldValue != newValue) {
          fireContentsChanged(this, index, index)
        }
        index++
      }
    }

  override fun getSize() = filteredData.size

  override fun getElementAt(index: Int) = filteredData[index]

  fun addElement(element: T) {
    data = data + element
  }

  fun removeElement(element: T) {
    data = data - element
  }

  fun replaceElement(element: T, index: Int) {
    data = data.toMutableList().apply { this[index] = element }
  }
}