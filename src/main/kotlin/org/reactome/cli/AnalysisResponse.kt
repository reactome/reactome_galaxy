package org.reactome.cli

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisResponse(
    val summary: Summary,
    val expression: Expression,
    val identifiersNotFound: Int,
    val pathwaysFound: Int,
    val pathways: List<Pathway>,
    val resourceSummary: List<ResourceSummary>,
    val speciesSummary: List<SpeciesSummary>,
    val warnings: List<String>
)

@Serializable
data class Summary(
    val token: String,
    val projection: Boolean,
    val interactors: Boolean,
    val type: String,
    val sampleName: String,
    val text: Boolean,
    val includeDisease: Boolean
)

@Serializable
data class Expression(
    val columnNames: List<String>
)

@Serializable
data class Pathway(
    val stId: String,
    val dbId: Int,
    val name: String,
    val species: Species,
    val llp: Boolean,
    val entities: Entities,
    val reactions: Reactions,
    val inDisease: Boolean
)

@Serializable
data class Species(
    val dbId: Int,
    val taxId: String,
    val name: String
)

@Serializable
data class Entities(
    val resource: String,
    val total: Int,
    val found: Int,
    val ratio: Double,
    val pValue: Double,
    val fdr: Double,
    val exp: List<String>
)

@Serializable
data class Reactions(
    val resource: String,
    val total: Int,
    val found: Int,
    val ratio: Double
)

@Serializable
data class ResourceSummary(
    val resource: String,
    val pathways: Int,
    val filtered: Int
)

@Serializable
data class SpeciesSummary(
    val dbId: Int,
    val taxId: String,
    val name: String,
    val pathways: Int,
    val filtered: Int
)
