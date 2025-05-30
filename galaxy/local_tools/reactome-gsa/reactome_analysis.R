#!/usr/bin/env Rscript

library(optparse)
library(httr)
library(ReactomeGSA)

option_list <- list(
  make_option(c("-m", "--method"), type = "character", default = "PADOG",
              help = "Analysis method (e.g. PADOG, Camera, ssGSEA) [default %default]"),

  make_option(c("-n", "--name"), type = "character", default = "Analysis_Dataset",
              help = "Dataset name for the analysis output [default %default]"),

  make_option(c("-o", "--pathways_output_file"), type = "character", default = "pathways.csv",
              help = "Output file name for pathways data [default %default]"),

  make_option(c("--entities_found_output_file"), type = "character", default = "entities_found.csv",
              help = "Output file name for found entities data [default %default]"),

  make_option(c("--entities_not_found_output_file"), type = "character", default = "entities_not_found.csv",
              help = "Output file name for not found entities data [default %default]"),

  make_option(c("--pdf_output_file"), type = "character", default = "report.pdf",
              help = "Output file name for the pdf report [default %default]"),

  make_option(c("--json_output_file"), type = "character", default = "result.json",
              help = "Output file name for the analysis results json file [default %default]"),

  make_option(c("--excel_output_file"), type = "character", default = "reactome_analysis.xlsx",
              help = "Output file name for the Excel report [default %default]"),

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

  make_option(c("--sample_groups"), type = "character", default = "",
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
              help = "Comma-separated list of variables (column names from annotation) to include as covariates."),

  make_option(c("--pathways_to_include"), type = "character", default = "",
              help = "Comma-separated list of pathways to include in the analysis. If not provided, all pathways will be included."),

  make_option(c("--min_pathway_size"), type = "integer", default = NULL,
              help = "Minimum pathway size to include in the analysis."),

  make_option(c("--max_pathway_size"), type = "integer", default = NULL,
              help = "Maximum pathway size to include in the analysis.")
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
                          create_reports = TRUE,
                          max_missing_values = opt$max_missing_values,
                          use_interactors = opt$use_interactors,
                          include_disease_pathways = opt$include_disease_pathways)

additional_factors_vector <- NULL
if (!is.null(opt$covariates) && nchar(opt$covariates) > 0) {
  additional_factors_vector <- unlist(strsplit(opt$covariates, split = ",", fixed = TRUE))
  additional_factors_vector <- trimws(additional_factors_vector)
  additional_factors_vector <- additional_factors_vector[additional_factors_vector != ""]
}

if (opt$method == "PADOG") {
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
} else if (opt$method == "Camera") {
    request <- add_dataset(request = request,
                           expression_values = dataset,
                           name = opt$name,
                           type = opt$data_type,
                           sample_data = annotations,
                           comparison_factor = opt$comparison_factor,
                           comparison_group_1 = opt$reference_group,
                           comparison_group_2 = opt$comparison_group,
                           additional_factors = additional_factors_vector,
                           normalisation_method = normalisation_function
    )
} else if (opt$method == "ssGSEA") {
    request <- add_dataset(request = request,
                           expression_values = dataset,
                           name = opt$name,
                           type = opt$data_type,
                           sample_data = annotations,
                           comparison_factor = opt$comparison_factor,
                           comparison_group_1 = opt$reference_group,
                           comparison_group_2 = opt$comparison_group,
                           additional_factors = additional_factors_vector,
                           pathways_to_include = opt$pathways_to_include,
                           min_pathway_size = opt$min_pathway_size,
                           max_pathway_size = opt$max_pathway_size
    )
} else {
    stop("Error: Invalid method. Please provide a valid value for --method.")
}

cat("Prepared request for Reactome analysis:\n")
str(request)

cat("Performing Reactome GSA analysis...\n")
result <- perform_reactome_analysis(request = request, compress = FALSE)
cat("Analysis complete.\n")

# need to be consistent with the ReactomeGSA
reactome_gsa_url <- "https://gsa.reactome.org"
reactome_gsa_version <- "0.1"

get_reactome_analysis_report_status <- function(analysis_id) {
  report_status_url = paste(c(reactome_gsa_url, reactome_gsa_version, "report_status", analysis_id), collapse="/")
  status_obj <- tryCatch(
    jsonlite::fromJSON(report_status_url),
    error = function(e) list(completed = 0, description = "Unknown", status = "running")
  )

  return(status_obj)
}

poll_until_complete <- function(
  status_fn,           # function(analysis_id, reactome_url) -> list(status, description, completed, ...)
  analysis_id,
  on_progress = NULL,  # function(completed, description)
  max_errors = 10,
  sleep_time = 1
) {
  completed <- status_fn(analysis_id)
  error_count <- 0

  is_done <- FALSE
  last_message <- completed[["description"]]

  if (!is.null(on_progress)) {
    on_progress(completed[["completed"]], completed[["description"]])
  }

  while (completed[["status"]] == "running") {
    Sys.sleep(sleep_time)
    completed <- tryCatch({
      status_fn(analysis_id)
    }, error = function(cond) {
      if (error_count < max_errors) {
        error_count <<- error_count + 1
        return(completed) # return last known completed status
      }
      stop("Error: Failed to connect to ReactomeGSA. Please contact support if this error persists at help@reactome.org", call. = FALSE)
    })

    if (!is.null(on_progress)) {
      if (completed[["description"]] != last_message && completed[["status"]] == "running" && !is_done) {
        on_progress(completed[["completed"]], completed[["description"]])
        last_message <- completed[["description"]]
      } else if (!is_done) {
        on_progress(completed[["completed"]], last_message)
      }
      if (as.numeric(completed[["completed"]]) == 1) {
        is_done <- TRUE
      }
    }
  }
  completed
}

analysis_id <- start_reactome_analysis(request = request)

completed_status <- poll_until_complete(
  status_fn = get_reactome_analysis_status,
  analysis_id = analysis_id,
  on_progress = function(completed, description) {
  cat(sprintf("Progress: %d%% - %s\n", completed * 100, description))
  })

if (completed_status[["status"]] == "failed") {
    if (verbose) warning("Reactome Analysis failed: ", completed[["description"]])
    return(NULL)
}

report_completed_status <- poll_until_complete(
  status_fn = get_reactome_analysis_report_status,
  analysis_id = analysis_id,
  on_progress = function(completed, description) {
    cat(sprintf("Report Progress: %d%% - %s\n", completed * 100, description))
  })

if (report_completed_status[["status"]] == "failed") {
    if (verbose) warning("Reactome Report generation failed: ", report_completed_status[["description"]])
    return(NULL)
}

result <- get_reactome_analysis_result(analysis_id = analysis_id)

gsa_reports = report_completed_status$reports
excel_url <- gsa_reports$url[gsa_reports$name == "MS Excel Report (xlsx)"]
pdf_url <- gsa_reports$url[gsa_reports$name == "PDF Report"]

cat("Reactome analysis results:\n")
links = reactome_links(result, print_result=TRUE, return_result=TRUE)
gsas_link <- Filter(function(x) x["name"] == "Gene Set Analysis Summary", links)[[1]]
full_url  <- gsas_link["url"]
base_url <- as.character(sub("^(https?://[^/]+).*$", "\\1", full_url))
token_enc <- sub(".*ANALYSIS=([^&]+).*", "\\1", full_url)
token <- URLdecode(token_enc)
resource <- "TOTAL" # or "UNIPROT"
analysis_service = "AnalysisService"
species <- "Homo%20sapiens"

jobs <- list(
  list(url = paste(c(base_url, analysis_service, "download", token, "pathways", resource, "pathways.csv"), collapse = "/"),
       dest = opt$pathways_output_file),

  list(url = paste(c(base_url, analysis_service, "download", token, "entities", "found", resource, "entities_found.csv"), collapse = "/"),
       dest = opt$entities_found_output_file),

  list(url = paste(c(base_url, analysis_service, "download", token, "entities", "notfound",  "entities_not_found.csv"), collapse = "/"),
       dest = opt$entities_not_found_output_file),

  list(url = paste(c(base_url, analysis_service, "download", token, "result.json"), collapse = "/"),
       dest = opt$json_output_file),

  list(url = pdf_url, dest = opt$pdf_output_file),

  list(url = excel_url, dest = opt$excel_output_file)
)

cat(paste(jobs))

for (job in jobs) {
  url  <- job$url
  cat(url, "\n")
  resp <- GET(url, write_disk(job$dest, overwrite = TRUE))
  stop_for_status(resp)
  cat("Downloaded ", job$dest, "\n")
}

cat("Reactome analysis completed successfully.\n")