package org.bp

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
    fun `getBy name`() {
        val source = googleSheetLocalizationStorage.getBy("Test")
        assertNotNull(source)
    }

    @Test
    fun save() {
        val localizationSource = LocalizationSource(
            "Test", mutableListOf(
                LocalizedString("greeting", mutableMapOf("en" to "Hello", "ru" to "Привет")),
                LocalizedString("goodbuy", mutableMapOf("en" to "Buy", "ru" to "Пока"))
            )
        )
        googleSheetLocalizationStorage.save(localizationSource)
    }
}