# -*- coding: utf-8 -*-
"""对比 英文 / 简中 / 繁中 / 港繁 / 日语 在"英文散文型"那批 key 上的取值。"""
import io
import os
import xml.etree.ElementTree as ET

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
RES = os.path.join(REPO, "app", "src", "main", "res")


def load(folder):
    path = os.path.join(RES, folder, "strings.xml")
    tree = ET.parse(path)
    return {el.get("name"): (el.text or "").strip()
            for el in tree.iter() if el.tag == "string" and el.get("name")}


en = load("values")
hans = load("values-b+zh+Hans")
hant = load("values-b+zh+Hant")
yue = load("values-b+yue+Hant+HK")
ja = load("values-ja")

# 繁中里仍是英文散文的 key 集合 (与上一轮审计一致)
KEYS = [
    "drug_cat_alzheimers", "drug_cat_anesthetic", "drug_cat_antibiotic",
    "drug_cat_anticoagulant", "drug_cat_anxiolytic", "drug_cat_herbal",
    "drug_cat_hormone_replacement", "drug_cat_mood_stabilizer",
    "drug_cat_muscle_relaxant", "drug_cat_osteoporosis_drug", "drug_cat_ppi",
    "drug_cat_statin", "drug_cat_substance_use",
    "ind_adrenal", "ind_alzheimers", "ind_anticoagulation",
    "ind_antidepressant_augmentation", "ind_anxiety", "ind_bipolar", "ind_di",
    "ind_eating_disorder", "ind_heart_failure", "ind_hormone", "ind_infection",
    "ind_inflammation", "ind_nausea", "ind_neuropathic_pain", "ind_nicotine",
    "ind_parkinson", "ind_substance_use", "ind_thyroid",
    "interaction_add_drug_n", "interaction_alerts_count", "interaction_card_title",
    "interaction_evaluate_n", "interaction_high_overdose_n", "interaction_info",
    "interaction_info_lines", "interaction_info_title", "interaction_more_drugs",
    "interaction_n_drugs_available", "interaction_no_alerts_desc",
    "interaction_no_alerts_title", "interaction_overdose_help",
    "interaction_reevaluate_n", "interaction_serotonin_format", "interaction_title",
    "pathway_esterase", "pathway_mao", "pathway_renal_excretion",
    "search_hint", "search_no_match", "search_placeholder", "search_select_title",
    "sev_life_threatening", "sevrule_high", "sevrule_low", "sevrule_medium",
]

for k in KEYS:
    print("### %s" % k)
    print("  EN  : %s" % en.get(k, "(missing)").replace("\n", "\\n")[:300])
    print("  简中: %s" % hans.get(k, "(missing)").replace("\n", "\\n")[:300])
    print("  繁中: %s" % hant.get(k, "(missing)").replace("\n", "\\n")[:120])
    print("  港繁: %s" % yue.get(k, "(missing)").replace("\n", "\\n")[:120])
    print("  日语: %s" % ja.get(k, "(missing)").replace("\n", "\\n")[:120])
    print()
