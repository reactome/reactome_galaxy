package org.reactome.cli

enum class SpeciesName(val scientificName: String, val shortName: String, val dbId: Int) {
    COW("Bos taurus", "cow", 48898),
    WORM("Caenorhabditis elegans", "worm", 68320),
    DOG("Canis familiaris", "dog", 49646),
    ZEBRAFISH("Danio rerio", "zebrafish", 68323),
    AMOEBA("Dictyostelium discoideum", "amoeba", 170941),
    FLY("Drosophila melanogaster", "fly", 56210),
    CHICKEN("Gallus gallus", "chicken", 49591),
    MOUSE("Mus musculus", "mouse", 48892),
    TUBERCOLOSIS("Mycobacterium tuberculosis", "tuberculosis", 176806),
    MALARIA("Plasmodium falciparum", "malaria", 170928),
    RAT("Rattus norvegicus", "rat", 48895),
    BAKERS_YEAST("Saccharomyces cerevisiae", "yeast", 68322),
    FISSION_YEAST("Schizosaccharomyces pombe", "fission_yeast", 68324),
    PIG("Sus scrofa", "pig", 49633),
    FROG("Xenopus tropicalis", "frog", 205621),
    HUMAN("Homo sapiens", "human", 48887);

    companion object {
        fun lookup(name: String): SpeciesName? {
            return values().find { it.scientificName.equals(name, ignoreCase = true) || it.shortName.equals(name, ignoreCase = true) }
        }
    }
}
