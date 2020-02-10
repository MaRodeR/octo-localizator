package org.bp

fun <T> MutableList<T>.firstOrAdd(predicate: (T) -> Boolean, newValue: T): T {
    var element = this.firstOrNull(predicate)
    return if (element == null) {
        element = newValue
        this.add(element)
        element
    } else {
        element
    }
}