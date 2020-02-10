package org.bp

typealias LocaleName = String
typealias Value = String

data class LocalizationSource(val name: String, val values: MutableList<LocalizedString> = mutableListOf()) {

    fun addTranslation(key: String, locale: String, translation: String) {
        val localizedString = values.firstOrAdd({ it.key == key }, LocalizedString(key))
        localizedString.values[locale] = translation
    }

    fun getExistsLocaleNames(): List<LocaleName> {
        return values
            .flatMap { it.values.keys }
            .distinct()
            .sortedWith(Comparator { l1, l2 ->
                if (l1 == "en") -1 else (if (l2 == "en") 1 else l1.compareTo(l2))
            })
    }

    fun merge(localization: LocalizationSource): LocalizationSource {
        val localizedStringMap = values
            .map { it.key to it.deepCopy() }
            .toMap()
            .toMutableMap()

        localization.values.forEach { newLocalizedString ->
            localizedStringMap.merge(newLocalizedString.key, newLocalizedString) { exitsValue, newValue ->
                exitsValue.merge(with = newValue)
            }
        }
        return copy(values = localizedStringMap.values.toMutableList())
    }
}

data class LocalizedString(val key: String, val values: MutableMap<LocaleName, Value> = mutableMapOf()) {

    fun deepCopy(): LocalizedString {
        return copy(values = values.toMutableMap())
    }

    fun merge(with: LocalizedString): LocalizedString {
        val newValues = values.toMutableMap()
        newValues.putAll(with.values)
        return copy(values = newValues)
    }
}