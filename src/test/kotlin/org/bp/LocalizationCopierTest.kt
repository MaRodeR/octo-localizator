package org.bp

import kotlin.test.Ignore
import kotlin.test.Test

class LocalizationCopierTest {

    private val localizationCopier = LocalizationCopier()

    private val iosProjectLocalizationStorage = IOSProjectLocalizationStorage(
        "<path_to_ios_project>"
    )

    private val googleSheetLocalizationStorage = GoogleSheetLocalizationStorage.initWithConfigFromFile(
        javaClass.classLoader.getResource("google-storage.config")!!.file,
        javaClass.classLoader.getResource("google-key.p12")!!.file.toString()
    )

    @Test
    @Ignore
    internal fun copyFromProjectToGoogleSheet() {
        localizationCopier.copy(iosProjectLocalizationStorage, googleSheetLocalizationStorage)
    }

    @Test
    @Ignore
    internal fun copyFromGoogleSheetToProject() {
        localizationCopier.copy(googleSheetLocalizationStorage, iosProjectLocalizationStorage,
            sourceNames = listOf("Main", "Localizable", "InfoPlist", "LaunchScreen"))
    }
}
