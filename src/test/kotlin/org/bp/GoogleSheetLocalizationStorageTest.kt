package org.bp

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull


class GoogleSheetLocalizationStorageTest {

    private val googleSheetLocalizationStorage: GoogleSheetLocalizationStorage =
        GoogleSheetLocalizationStorage.initWithConfigFromFile(
            javaClass.classLoader.getResource("google-storage.config")!!.file,
            javaClass.classLoader.getResource("google-key.p12")!!.file.toString()
        )

    @Test
    fun `save and update localization`() {
        val sourceName = "Test"
        googleSheetLocalizationStorage.deleteAll(sourceName)

        var loadedLocalizationSource = googleSheetLocalizationStorage.getBy(sourceName)
        assertThat(loadedLocalizationSource?.values)
            .isEmpty()

        val localizationSource = LocalizationSource(
            sourceName, mutableListOf(
                LocalizedString("greeting", mutableMapOf("en" to "Hello", "ru" to "Привет")),
                LocalizedString("goodbuy", mutableMapOf("en" to "Buy", "ru" to "Пока"))
            )
        )
        googleSheetLocalizationStorage.save(localizationSource)

        val localizationSourceUpdate = LocalizationSource(
            sourceName, mutableListOf(
                LocalizedString("map", mutableMapOf("en" to "Map", "ru" to "Карта")),
                LocalizedString("goodbuy", mutableMapOf("en" to "Buy", "ru" to "До свидания", "de" to "Tschüss"))
            )
        )
        googleSheetLocalizationStorage.save(localizationSourceUpdate)

        loadedLocalizationSource = googleSheetLocalizationStorage.getBy(sourceName)

        assertThat(loadedLocalizationSource?.values)
            .containsExactlyInAnyOrder(
//                LocalizedString("greeting", mutableMapOf("en" to "Hello", "ru" to "Привет")),
                LocalizedString("goodbuy", mutableMapOf("en" to "Buy", "ru" to "До свидания", "de" to "Tschüss")),
                LocalizedString("map", mutableMapOf("en" to "Map", "ru" to "Карта", "de" to ""))
            )
    }
}