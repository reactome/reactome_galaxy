package org.reactome.cli

import com.opencsv.CSVWriter
import java.io.Writer
import java.util.Locale

private val CSV_HEADER = arrayOf(
    "Pathway name",
    "#Entities found",
    "#Entities total",
    "Entities ratio",
    "Entities pValue",
    "Entities FDR",
    "#Reactions found",
    "#Reactions total",
    "Reactions ratio",
    "Species identifier",
    "Species name"
)

class ReportGenerator {

    fun writeCsv(analysisResponse: AnalysisResponse, writer: Writer) {
        CSVWriter(writer).use { csvWriter ->
            csvWriter.writeNext(CSV_HEADER)

            for (pathway in analysisResponse.pathways) {
                val entitiesRatioStr = String.format(Locale.US, "%.12f", pathway.entities.ratio)
                val entitiesPValueStr = String.format(Locale.US, "%.2E", pathway.entities.pValue)
                val entitiesFdrStr = String.format(Locale.US, "%.12f", pathway.entities.fdr)
                val reactionsRatioStr = String.format(Locale.US, "%.12f", pathway.reactions.ratio)

                val row = arrayOf(
                    pathway.name,
                    pathway.entities.found.toString(),
                    pathway.entities.total.toString(),
                    entitiesRatioStr,
                    entitiesPValueStr,
                    entitiesFdrStr,
                    pathway.reactions.found.toString(),
                    pathway.reactions.total.toString(),
                    reactionsRatioStr,
                    pathway.species.taxId,
                    pathway.species.name
                )
                
                csvWriter.writeNext(row)
            }
        }
    }
}