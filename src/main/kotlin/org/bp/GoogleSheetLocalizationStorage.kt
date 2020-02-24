package org.bp

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import java.io.File


class GoogleSheetLocalizationStorage(
    private val spreadSheetId: String,
    private val accountId: String,
    private val pkFilePath: String
) : LocalizationStorage {

    private val service by lazy { getSheetsService() }

    override fun getAll(sourceNames: List<String>?): List<LocalizationSource> {

        val sheets = service.getListOfSheetsFor(spreadSheetId)

        var localizationSources = sheets
            .map { LocalizationSource(it.getTitle()) }

        if (sourceNames != null) {
            localizationSources = localizationSources
                .filter { sourceNames.contains(it.name) }
        }

        localizationSources.forEach { source ->
            val values = service.getSheetValues(spreadSheetId, source.name)
            if (values.isEmpty()) {
                return@forEach
            }
            val headerRow = values[0]
            val translationRows = values.subList(1, values.size)

            val columnIndexToLocaleMap = headerRow
                .mapIndexed { index, value -> index to value.toString() }
                .filter { it.second != "key" }
                .toMap()

            translationRows.forEach { row ->
                val key = row[0].toString()
                val translations = row.subList(1, row.size).map { it.toString() }

                translations.forEachIndexed { index, translation ->
                    val localeName = columnIndexToLocaleMap[index + 1]
                    if (localeName != null) {
                        source.addTranslation(key, localeName, translation)
                    }
                }
            }
        }

        return localizationSources
    }

    override fun save(localization: LocalizationSource) {

        val sheetTitle = localization.name
        val sheet = service.getListOfSheetsFor(spreadSheetId)
            .findByTitle(sheetTitle)

        if (sheet == null) {
            createSheet(spreadSheetId, sheetTitle)
        }

        val locales = localization.getExistsLocaleNames()

        val headers = mutableListOf("key") + locales

        val rows: List<List<String>> = localization.values
            .map { localizedString ->
                val row = mutableListOf(localizedString.key)
                locales.forEach { locale -> row.add(localizedString.values.getOrDefault(locale, "")) }
                row
            }

        val values: List<List<String>> = mutableListOf(headers) + rows

        service.spreadsheets().values().update(spreadSheetId, "$sheetTitle!A1", ValueRange().setValues(values))
            .setValueInputOption("USER_ENTERED")
            .execute()
    }

    override fun deleteAll(sourceName: String) {
        service.getListOfSheetsFor(spreadSheetId)
            .findByTitle(sourceName)
            ?.let { clearSheet(it.getSheetId()) }
    }

    private fun createSheet(spreadSheetId: String, sheetName: String) {
        val request = Request().setAddSheet(
            AddSheetRequest().setProperties(SheetProperties().setTitle(sheetName))
        )
        val body = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
        service.spreadsheets().batchUpdate(spreadSheetId, body)
            .execute()
    }

    private fun clearSheet(sheetId: Int) {
        val request = Request().setUpdateCells(
            UpdateCellsRequest()
                .setFields("*")
                .setRange(
                    GridRange().setSheetId(sheetId)
                        .setStartRowIndex(0).setStartColumnIndex(0)
                        .setEndRowIndex(10000).setEndColumnIndex(10000)
                )
        )
        val body = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
        service.spreadsheets().batchUpdate(spreadSheetId, body)
            .execute()
    }

    private fun getSheetsService(): Sheets {
        val credentials = GoogleCredential.Builder()
            .setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .setServiceAccountId(accountId)
            .setServiceAccountScopes(scopes)
            .setServiceAccountPrivateKeyFromP12File(File(pkFilePath))
            .build()
        return Sheets.Builder(httpTransport, jsonFactory, credentials)
            .setApplicationName(applicationName)
            .build()
    }

    companion object {

        private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        private val jsonFactory = JacksonFactory.getDefaultInstance()
        private val scopes = listOf(SheetsScopes.SPREADSHEETS)
        private const val applicationName = "localization"

        fun initWithConfigFromFile(configFilePath: String, pkFilePath: String): GoogleSheetLocalizationStorage {
            val config = File(configFilePath).readText().split(" ")
            if (config.size != 2) {
                throw IllegalStateException("File '$configFilePath' must contain credentials in format 'accountId spreadSheetId")
            }
            return GoogleSheetLocalizationStorage(
                config[1],
                config[0],
                pkFilePath
            )
        }

        private fun List<Sheet>.findByTitle(sheetName: String): Sheet? =
            this.find { sheet -> sheet.getTitle() == sheetName }

        private fun Sheets.getListOfSheetsFor(spreadSheetId: String): List<Sheet> =
            spreadsheets().get(spreadSheetId).execute().sheets

        private fun Sheets.getSheetValues(spreadSheetId: String, sheetName: String): List<MutableList<Any>> =
            spreadsheets().values().get(spreadSheetId, sheetName).execute().getValues() ?: listOf()

        private fun Sheet.getSheetId() = properties["sheetId"] as Int

        private fun Sheet.getTitle() = properties["title"] as String
    }
}