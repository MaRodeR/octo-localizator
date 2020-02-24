package org.bp

interface LocalizationStorage {

    fun save(localization: LocalizationSource)

    fun getAll(sourceNames: List<String>? = null): List<LocalizationSource>

    fun getBy(name: String): LocalizationSource? {
        return getAll(listOf(name))
            .firstOrNull()
    }

    fun deleteAll(sourceName: String)
}