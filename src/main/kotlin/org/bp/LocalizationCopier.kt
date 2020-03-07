package org.bp

class LocalizationCopier {

    fun copy(fromStorage: LocalizationStorage, toStorage: LocalizationStorage, sourceNames: List<String>? = null) {
        fromStorage.getAll(sourceNames)
            .forEach { toStorage.save(it) }
    }

    fun updateExisting(fromStorage: LocalizationStorage, toStorage: LocalizationStorage, sourceNames: List<String>? = null) {
        fromStorage.getAll(sourceNames)
            .forEach { toStorage.updateExisting(it) }
    }
}