#!/usr/bin/env Rscript

library(optparse)
library(ReactomeGSA)

option_list <- list(
  make_option(c("-m", "--method"), type = "character", default = "PADOG",
              help = "Analysis method (e.g. PADOG, Camera, ssGSEA) [default %default]"),
  make_option(c("-n", "--name"), type = "character", default = "Analysis_Dataset",
              help = "Dataset name for the analysis output [default %default]"),
  make_option(c("-o", "--pathways_output_file"), type = "character", default = "pathways_output.csv",
              help = "Output file name for pathways data [default %default]"),

  make_option(c("--dataset"), type = "character",
              help = "Path to the tabular dataset file (gene/protein identifiers in first column, expression values in others).",
              default = NULL),

  make_option(c("--data_type"), type = "character",
                help = "Type of dataset (rnaseq_counts, rnaseq_norm, proteomics_int, proteomics_sc, microarray_norm).",
                default = NULL),
  make_option(c("--annotation"), type = "character",
              help = "Path to the tabular annotation file (sample names in first column, sample group info in others).",
              default = NULL),

  make_option(c("--use_interactors"), type = "logical", default = FALSE,
              help = "Indicates whether interactors from IntAct should be used to extend REACTOME's pathways."),

  make_option(c("--include_disease_pathways"), type = "logical", default = TRUE,
              help = "Include disease pathways in Reactome analysis."),

  make_option(c("--max_missing_values"), type = "numeric", default = 0.5,
              help = "Maximum (relative) number of missing values within one comparison group (0-1)."),

  make_option(c("--sample_groups"), type = "character", default = "",  # Not default NULL or it will explode
              help = "Sample property name holding sample group information for matched-pair analysis."),

  make_option(c("--discrete_normalisation_function"), type = "character", default = "TMM",
              help = "Normalization function for raw RNA-seq read counts and raw Proteomics spectral counts (tmm, rle, upperquartile, none)."),

  make_option(c("--continuous_normalisation_function"), type = "character", default = "none",
              help = "Normalization function for proteomics intensity data (none, scale, quantile, cyclicloess)."),

  make_option(c("--comparison_factor"), type = "character",
              help = "Column from annotation distinguishing the two comparison groups (reference/comparison).",
              default = NULL),

  make_option(c("--reference_group"), type = "character",
              help = "The baseline group against which other groups are compared.",
              default = NULL),

  make_option(c("--comparison_group"), type = "character",
              help = "The group being compared to the reference group."),

  make_option(c("--covariates"), type = "character", default = NULL,
              help = "Comma-separated list of variables (column names from annotation) to include as covariates.")
)

opt_parser <- OptionParser(option_list = option_list)
opt <- parse_args(opt_parser)

if (is.null(opt$dataset) || !file.exists(opt$dataset)) {
  stop("Error: Dataset file is required and must exist. Please provide a valid path to --dataset.")
}

if (is.null(opt$annotation) || !file.exists(opt$annotation)) {
  stop("Error: Annotation file is required and must exist. Please provide a valid path to --annotation.")
}

if (is.null(opt$comparison_factor)) {
  stop("Error: Comparison factor is required. Please provide a value for --comparison_factor.")
}

if (is.null(opt$reference_group)) {
  stop("Error: Reference group is required. Please provide a value for --reference_group.")
}

if (is.null(opt$comparison_group)) {
  stop("Error: Comparison group is required. Please provide a value for --comparison_group.")
}

if (is.null(opt$data_type)) {
  stop("Error: Dataset type is required. Please provide a value for --data_type.")
}

detect_separator_and_load <- function(fn, header = TRUE, stringsAsFactors = FALSE) {
  first_line <- readLines(fn, n = 1)
  sep <- ifelse(grepl("\t", first_line), "\t", ",")

  has_leading_sep <- grepl(paste0("^", sep), first_line)

  df <- read.table(fn,
                   sep = sep,
                   header = header,
                   strip.white = TRUE,
                   stringsAsFactors = stringsAsFactors,
                   row.names = ifelse(has_leading_sep, 1, NULL))

  return(df)
}

# TODO double check this, and maybe allow the user to specify more dynamically in the input
if (opt$data_type %in% c("rnaseq_counts", "proteomics_sc")) {
  normalisation_function <- opt$discrete_normalisation_function
} else if (opt$data_type %in% c("rnaseq_norm", "proteomics_int", "microarray_norm")) {
  normalisation_function <- opt$continuous_normalisation_function
} else {
  stop("Error: Invalid data type. Please provide a valid value for --data_type.")
}

dataset = detect_separator_and_load(opt$dataset, header = TRUE, stringsAsFactors = FALSE)
cat("Successfully loaded dataset from:", opt$dataset, "\n")
annotations = detect_separator_and_load(opt$annotation, header = TRUE, stringsAsFactors = FALSE)
cat("Successfully loaded annotation from:", opt$annotation, "\n")

request <- ReactomeAnalysisRequest(method = opt$method)

request <- set_parameters(request = request,
                          max_missing_values = opt$max_missing_values,
                          use_interactors = opt$use_interactors,
                          include_disease_pathways = opt$include_disease_pathways)

additional_factors_vector <- NULL
if (!is.null(opt$covariates) && nchar(opt$covariates) > 0) {
  additional_factors_vector <- unlist(strsplit(opt$covariates, split = ",", fixed = TRUE))
  additional_factors_vector <- trimws(additional_factors_vector)
  additional_factors_vector <- additional_factors_vector[additional_factors_vector != ""]
}

request <- add_dataset(request = request,
                       expression_values = dataset,
                       name = opt$name,
                       type = opt$data_type,
                       sample_data = annotations,
                       comparison_factor = opt$comparison_factor,
                       comparison_group_1 = opt$reference_group,
                       comparison_group_2 = opt$comparison_group,
                       additional_factors = additional_factors_vector,
                       sample_groups = opt$sample_groups,
                       normalisation_method = normalisation_function
                       )

cat("Performing Reactome GSA analysis...\n")
result <- perform_reactome_analysis(request = request, compress = FALSE)
cat("Analysis complete.\n")

pathways <- get_result(result, type = "pathways", name = opt$name)
write.csv(pathways, file = opt$pathways_output_file, row.names = FALSE)
cat("Pathways data successfully written to:", opt$pathways_output_file, "\n")
