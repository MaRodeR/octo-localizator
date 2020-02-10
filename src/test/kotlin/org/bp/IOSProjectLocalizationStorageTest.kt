package org.bp

import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue


class IOSProjectLocalizationStorageTest {

    @Test
    fun getAll() {
        val iosProjectLocalizationStorage = IOSProjectLocalizationStorage(
            this.javaClass.classLoader.getResource("ios-test-project")!!.file.toString()
        )
        val result = iosProjectLocalizationStorage.getAll()

        assertEquals(4, result.size)

        val launchScreenRes = result.first { it.name == "LaunchScreen" }
        assertTrue { launchScreenRes.values.isEmpty() }

        val infoPlistRes = result.first { it.name == "InfoPlist" }
        assertEquals(3, infoPlistRes.values.size)
        assertEquals(
            "needs to access",
            infoPlistRes.getTranslation("NSLocationAlwaysAndWhenInUseUsageDescription", "en")
        )
        assertEquals(
            "Deiner Nähe zu finden.",
            infoPlistRes.getTranslation("NSLocationAlwaysAndWhenInUseUsageDescription", "de")
        )
        assertEquals(
            "your location",
            infoPlistRes.getTranslation("NSLocationWhenInUseUsageDescription", "en")
        )
        assertEquals(
            "Position um einwandfrei",
            infoPlistRes.getTranslation("NSLocationWhenInUseUsageDescription", "de")
        )
        assertEquals(
            "to find people nearby your location.",
            infoPlistRes.getTranslation("NSLocationAlwaysUsageDescription", "en")
        )
        assertEquals(
            "funktionieren und Menschen",
            infoPlistRes.getTranslation("NSLocationAlwaysUsageDescription", "de")
        )

        val localizableRes = result.first { it.name == "Localizable" }
        assertEquals(6, localizableRes.values.size)

        val mainRes = result.first { it.name == "Main" }
        assertEquals(4, mainRes.values.size)
        assertEquals(
            "Sign up",
            mainRes.getTranslation("0ix-TJ-xZf.normalTitle", "en")
        )
        assertEquals(
            "Anmelden",
            mainRes.getTranslation("0ix-TJ-xZf.normalTitle", "de")
        )
        assertEquals(
            "About me:",
            mainRes.getTranslation("0l2-74-dYu.text", "en")
        )
        assertEquals(
            "Über mich:",
            mainRes.getTranslation("0l2-74-dYu.text", "de")
        )
        assertEquals(
            "Forgot your password?",
            mainRes.getTranslation("2tx-IY-4Ap.text", "en")
        )
        assertEquals(
            "Haben Sie Ihr Passwort vergessen?",
            mainRes.getTranslation("2tx-IY-4Ap.text", "de")
        )
        assertEquals(
            "Name",
            mainRes.getTranslation("3ib-YA-eKx.text", "en")
        )
    }

    @Test
    internal fun save() {
        val iOSProjectPath = Files.createTempDirectory("ios-localization").toFile()

        val iosProjectFrom = File(this.javaClass.classLoader.getResource("ios-test-project").file)
        iosProjectFrom.copyRecursively(iOSProjectPath, true)
        val iosProjectLocalizationStorage = IOSProjectLocalizationStorage(iOSProjectPath.toString())

        var localizations = iosProjectLocalizationStorage.getAll()
        assertNull(localizations.firstOrNull { it.name == "Main-2" })

        val main2Localization = LocalizationSource(
            "Main-2",
            mutableListOf(
                LocalizedString("greeting", mutableMapOf("en" to "Hello", "ru" to "Привет", "de" to "Hallo")),
                LocalizedString("goodbye", mutableMapOf("en" to "Goodbye", "ru" to "До свидания", "de" to "Tschüss"))
            )
        )
        iosProjectLocalizationStorage.save(main2Localization)

        val localizableLocalization = LocalizationSource(
            "Localizable",
            mutableListOf(
                LocalizedString("confirm", mutableMapOf("en" to "Confirm!", "ru" to "Подтвердить", "de" to "Bestätigen")),
                LocalizedString("new_message", mutableMapOf("en" to "New message!", "ru" to "Новое сообщение", "de" to "neue Nachricht"))
            )
        )
        iosProjectLocalizationStorage.save(localizableLocalization)

        localizations = iosProjectLocalizationStorage.getAll()
        assertThat(localizations)
            .extracting("name")
            .containsExactlyInAnyOrder("Main-2", "InfoPlist", "LaunchScreen", "Localizable", "Main")

        assertThat(localizations.first { it.name == "Main-2" }.values)
            .hasSize(2)
            .containsExactlyInAnyOrderElementsOf(main2Localization.values)

        assertThat(localizations.first { it.name == "Localizable" }.values)
            .hasSize(7)
            .containsExactlyInAnyOrder(
                LocalizedString("confirm", mutableMapOf("en" to "Confirm!", "ru" to "Подтвердить", "de" to "Bestätigen")),
                LocalizedString("oops", mutableMapOf("en" to "Oops...", "ru" to "", "de" to "Ups ...")),
                LocalizedString("agree", mutableMapOf("en" to "Agree", "ru" to "", "de" to "Zustimmen")),
                LocalizedString("disagree", mutableMapOf("en" to "Disagree", "ru" to "", "de" to "nicht einverstanden")),
                LocalizedString("success", mutableMapOf("en" to "Success!", "ru" to "", "de" to "Erfolg!")),
                LocalizedString("mistake", mutableMapOf("en" to "Mistake!", "ru" to "", "de" to "Fehler!")),
                LocalizedString("new_message", mutableMapOf("en" to "New message!", "ru" to "Новое сообщение", "de" to "neue Nachricht"))
            )
    }

    private fun LocalizationSource.getTranslation(key: String, locale: String) =
        this.values.first { it.key == key }.values[locale]
}