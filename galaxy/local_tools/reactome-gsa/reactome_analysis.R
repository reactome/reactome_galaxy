#!/usr/bin/env Rscript

library(optparse)
library(ReactomeGSA)

# CLI options
option_list <- list(
  make_option(c("-m", "--method"), type = "character", default = "Camera",
              help = "Analysis method (e.g., Camera, PADOG) [default %default]"),
  make_option(c("-n", "--name"), type = "character", default = "Example_Dataset",
              help = "Dataset name [default %default]"),
 make_option(c("-o", "--pathways_output_file"), type = "character", default = "pathways_output.csv",
              help = "Output file name for pathways data [default %default]")
)

opt_parser <- OptionParser(option_list = option_list)
opt <- parse_args(opt_parser)

library(ReactomeGSA.data)
data("griss_melanoma_proteomics")


request <- ReactomeAnalysisRequest(method = opt$method)

request <- set_parameters(request = request, max_missing_values = 0.5)

request <- add_dataset(request = request,
                          expression_values = griss_melanoma_proteomics,
                          name = "Proteomics",
                          type = "proteomics_int",
                          comparison_factor = "condition",
                          comparison_group_1 = "MOCK",
                          comparison_group_2 = "MCM",
                          additional_factors = c("cell.type", "patient.id"))

# Optional: print the request to verify
print(request)

# Perform analysis
result <- perform_reactome_analysis(request = request, compress = FALSE)

# Print summary
print(result)

pathways <- get_result(result, type = "pathways", name = "Proteomics")

# Write pathways data frame to the specified output file
write.csv(pathways, file = opt$pathways_output_file, row.names = FALSE)

cat("Pathways data written to:", opt$pathways_output_file, "\n")

