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

    override fun getAll(sourceNames: List<String>?): List<LocalizationSource> {
        val service = getSheetsService()

        val sheets = service.getSheetsFor(spreadSheetId)

        var localizationSources = sheets
            .map { LocalizationSource(it.properties["title"].toString()) }

        if (sourceNames != null) {
            localizationSources = localizationSources
                .filter { sourceNames.contains(it.name) }
        }

        localizationSources.forEach { source ->
            val values = service.getSheetValues(spreadSheetId, source.name)
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
        val service = getSheetsService()

        val spreadsheet = service.getSheetsFor(spreadSheetId)
        val sheetTitle = localization.name

        val sheet = spreadsheet.findByTitle(sheetTitle)
        if (sheet != null) {
            clearSheet(service, spreadSheetId, sheet.properties["sheetId"] as Int)
        } else {
            createSheet(service, spreadSheetId, sheetTitle)
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

    private fun createSheet(service: Sheets, spreadSheetId: String, sheetName: String) {
        val request = Request().setAddSheet(
            AddSheetRequest().setProperties(SheetProperties().setTitle(sheetName))
        )
        val body = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
        service.spreadsheets().batchUpdate(spreadSheetId, body)
            .execute()
    }

    private fun clearSheet(service: Sheets, spreadSheetId: String, sheetId: Int) {
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
            this.find { sheet -> sheet.properties["title"] == sheetName }

        private fun Sheets.getSheetsFor(spreadSheetId: String): List<Sheet> =
            spreadsheets().get(spreadSheetId).execute().sheets

        private fun Sheets.getSheetValues(spreadSheetId: String, sheetName: String): List<MutableList<Any>> =
            spreadsheets().values().get(spreadSheetId, sheetName).execute().getValues()
    }
}