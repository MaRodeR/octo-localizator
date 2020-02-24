package org.bp

import java.io.File
import java.nio.file.Paths

class IOSProjectLocalizationStorage(private val iOSProjectPath: String) : LocalizationStorage {


    override fun save(localization: LocalizationSource) {
        val projectFolder = getProjectFolder()

        //read current values, update them and save again
        val existsLocalization = getBy(localization.name)
        val mergedLocalization = existsLocalization?.merge(localization) ?: localization

        mergedLocalization.getExistsLocaleNames().forEach { locale ->
            val localeFolder = Paths.get(projectFolder.toString(), "$locale.lproj").toFile()
            localeFolder.mkdir()

            val localizationFile = Paths.get(localeFolder.toString(), "${mergedLocalization.name}.strings").toFile()

            val keyToValueString = mergedLocalization.values
                .joinToString("\n") { """"${it.key}" = "${it.values.getOrDefault(locale, "")}";""" }

            localizationFile.writeText(keyToValueString)
        }
    }

    @Throws(IllegalStateException::class)
    override fun getAll(sourceNames: List<String>?): List<LocalizationSource> {
        val projectFolder = getProjectFolder()

        val localization = mutableListOf<LocalizationSource>()

        projectFolder
            .listFiles { file, name -> file.isDirectory && name.matches(".{2}\\.lproj".toRegex()) }
            ?.forEach { folder ->
                folder.listFiles { _, name -> name.endsWith(".strings") }
                    ?.filter { sourceNames == null || sourceNames.contains(it.nameWithoutExtension) }
                    ?.forEach { file ->
                        readLocalization(file, localization, folder.nameWithoutExtension)
                    }
            }
        return localization
    }

    override fun deleteAll(sourceName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getProjectFolder(): File {
        val projectFolder = File(iOSProjectPath)
        if (!projectFolder.exists() || !projectFolder.isDirectory) {
            throw IllegalStateException("Project directory $iOSProjectPath not found or it is not directory")
        }
        return projectFolder
    }

    private fun readLocalization(file: File, localization: MutableList<LocalizationSource>, localeName: String) {
        val resourceName = file.nameWithoutExtension
        val localizationSource = localization.firstOrAdd({ it.name == resourceName }, LocalizationSource(resourceName))
        file.readLines()
            .removeComments()
            .toKeyValueMap()
            .forEach { localizationSource.addTranslation(it.key, localeName, it.value) }
    }

    private fun List<String>.removeComments(): List<String> {
        val lines = this.map { it.replace("//.*".toRegex(), "") }
        return lines.joinToString("\n").replace("/\\*.*?\\*/".toRegex(), "").split("\n")
    }

    private fun List<String>.toKeyValueMap(): Map<String, String> {
        return this.map { it.replace(";", "") }
            .map { it.replace("\"", "") }
            .filter { it.contains("=") }
            .map {
                val parts = it.split("=")
                parts[0].trim() to parts[1].trim()
            }.toMap()
    }

}