package org.reactome.cli

enum class TissueName(val displayName: String, val tissueId: Int) {
    ADRENAL_GLAND("Adrenal Gland", 2),
    BONE_MARROW("Bone Marrow", 3),
    BREAST("Breast", 4),
    BRONCHUS("Bronchus", 5),
    CEREBELLUM("Cerebellum", 6),
    CEREBRAL_CORTEX("Cerebral Cortex", 7),
    CERVIX_UTERINE("Cervix - Uterine", 8),
    COLON("Colon", 9),
    DUODENUM("Duodenum", 10),
    EPIDIDYMIS("Epididymis", 12),
    ESOPHAGUS("Esophagus", 13),
    FALLOPIAN_TUBE("Fallopian Tube", 14),
    GALL_BLADDER("Gall Bladder", 15),
    HEART_MUSCLE("Heart Muscle", 16),
    HIPPOCAMPAL_FORMATION("Hippocampal Formation", 17),
    KIDNEY("Kidney", 18),
    LIVER("Liver", 19),
    LUNG("Lung", 20),
    LYMPH_NODE("Lymph Node", 21),
    NASOPHARYNX("Nasopharynx", 22),
    ORAL_MUCOSA("Oral Mucosa", 23),
    OVARY("Ovary", 24),
    PANCREAS("Pancreas", 25),
    PARATHYROID_GLAND("Parathyroid Gland", 26),
    PLACENTA("Placenta", 27),
    PROSTATE_GLAND("Prostate Gland", 28),
    RECTUM("Rectum", 29),
    SALIVA_SECRETING_GLAND("Saliva-secreting Gland", 30),
    SEMINAL_VESICLE("Seminal Vesicle", 31),
    SKELETAL_MUSCLE_TISSUE("Skeletal Muscle Tissue", 32),
    SMALL_INTESTINE("Small Intestine", 33),
    SMOOTH_MUSCLE_TISSUE("Smooth Muscle Tissue", 34),
    SPLEEN("Spleen", 36),
    TELENCEPHALIC_VENTRICLE("Telencephalic Ventricle", 38),
    TESTIS("Testis", 39),
    THYROID_GLAND("Thyroid Gland", 40),
    TONSIL("Tonsil", 41),
    URINARY_BLADDER("Urinary Bladder", 42),
    VAGINA("Vagina", 43),
    VERMIFORM_APPENDIX("Vermiform Appendix", 44);

    companion object {
        fun lookup(name: String): TissueName? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}
