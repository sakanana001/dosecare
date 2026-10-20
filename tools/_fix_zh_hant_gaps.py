# -*- coding: utf-8 -*-
"""补齐 繁中 / 港繁 里"简中已翻译、但仍停留在英文"的 91 条漏译。

背景: 第 4 期做的是"key 齐平"审计 (key 存不存在), 而这批是 key 存在但**值还是英文**,
      属于另一种失败模式, 上一轮没覆盖到。

繁中: 标准书面繁体中文 (台湾用语习惯)。
港繁: 真粤语书面化 (嘅/唔/仲/咗/喺/揀/搵), 术语保留通用写法。
"""
import io
import os
import re
import sys

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
RES = os.path.join(REPO, "app", "src", "main", "res")

# key -> (繁中, 港繁)
T = {
    # ---- 通用按钮 ----
    "common_clear": ("清空", "清空"),
    "common_close": ("關閉", "關閉"),
    "common_known": ("知道了", "知道了"),
    "common_remove": ("移除", "移除"),
    "common_search": ("搜尋", "搜尋"),
    "common_settings": ("設定", "設定"),
    "cyp_p_gp": ("P-糖蛋白", "P-糖蛋白"),
    # ---- 药理分类 ----
    "drug_cat_alzheimers": ("抗癡呆/認知改善", "抗癡呆/認知改善"),
    "drug_cat_analgesic": ("止痛藥", "止痛藥"),
    "drug_cat_anesthetic": ("麻醉 (局麻/全麻)", "麻醉 (局麻/全麻)"),
    "drug_cat_antiarrhythmic": ("抗心律不整", "抗心律不整"),
    "drug_cat_antibiotic": ("抗感染 (抗真菌/抗結核/抗 HIV)", "抗感染 (抗真菌/抗結核/抗 HIV)"),
    "drug_cat_anticholinergic": ("抗膽鹼能", "抗膽鹼能"),
    "drug_cat_anticoagulant": ("抗凝/抗血小板", "抗凝/抗血小板"),
    "drug_cat_antidepressant": ("抗憂鬱藥", "抗憂鬱藥"),
    "drug_cat_antidiabetic": ("降血糖藥", "降血糖藥"),
    "drug_cat_antiepileptic": ("抗癲癇", "抗癲癇"),
    "drug_cat_antihistamine": ("抗組織胺", "抗組織胺"),
    "drug_cat_antihypertensive": ("降血壓藥", "降血壓藥"),
    "drug_cat_antimigraine": ("抗偏頭痛", "抗偏頭痛"),
    "drug_cat_antiparkinsonian": ("抗帕金森", "抗帕金森"),
    "drug_cat_antipsychotic": ("抗精神病藥", "抗精神病藥"),
    "drug_cat_antivertigo": ("抗眩暈", "抗眩暈"),
    "drug_cat_anxiolytic": ("抗焦慮/苯二氮䓬類", "抗焦慮/苯二氮䓬類"),
    "drug_cat_bph_agent": ("前列腺增生藥", "前列腺增生藥"),
    "drug_cat_bronchodilator": ("支氣管擴張劑", "支氣管擴張劑"),
    "drug_cat_corticosteroid": ("腎上腺皮質激素", "腎上腺皮質激素"),
    "drug_cat_gout": ("抗痛風", "抗痛風"),
    "drug_cat_herbal": ("草本補充劑", "草本補充劑"),
    "drug_cat_hormone_replacement": ("荷爾蒙替代", "荷爾蒙替代"),
    "drug_cat_mood_stabilizer": ("心境穩定劑", "心境穩定劑"),
    "drug_cat_muscle_relaxant": ("肌肉鬆弛劑", "肌肉鬆弛劑"),
    "drug_cat_osteoporosis_drug": ("骨質疏鬆藥", "骨質疏鬆藥"),
    "drug_cat_other": ("其他", "其他"),
    "drug_cat_ppi": ("質子幫浦抑制劑 (PPI)", "質子幫浦抑制劑 (PPI)"),
    "drug_cat_statin": ("調脂藥", "調脂藥"),
    "drug_cat_stimulant": ("ADHD 興奮劑", "ADHD 興奮劑"),
    "drug_cat_substance_use": ("物質依賴治療", "物質依賴治療"),
    "drug_cat_supplement": ("營養補充劑", "營養補充劑"),
    "drug_cat_thyroid": ("甲狀腺", "甲狀腺"),
    # 药名双行展示: 简中用括号, 繁中/港繁跟着用括号 (与简中观感一致)
    "drug_name_with_zh_subtitle": ("%1$s (%2$s)", "%1$s (%2$s)"),
    # ---- 相互作用 ----
    "interaction_acb_format": ("ACB 評分 %1$d", "ACB 評分 %1$d"),
    "interaction_add_drug_n": ("加入藥物 · 搜尋 (%1$d 個可選)", "加藥 · 搜尋 (%1$d 個可選)"),
    "interaction_advice": ("建議:%1$s", "建議:%1$s"),
    "interaction_alerts_count": ("RuleEngine 檢出 %1$d 條警示", "RuleEngine 檢出 %1$d 條警示"),
    "interaction_card_title": ("多藥相互作用", "多藥相互作用"),
    "interaction_evaluate_n": ("評估 %1$d 藥的所有組合", "評估 %1$d 藥的所有組合"),
    "interaction_high_overdose_n": ("⚠️ %1$d 藥為高風險過量 (重症 / 危及生命)", "⚠️ %1$d 藥為高風險過量 (重症 / 危及生命)"),
    "interaction_info": ("評估說明", "評估說明"),
    "interaction_info_lines": (
        "• 加入 2+ 個藥,RuleEngine 跑所有組合 (N×(N-1)/2 對)\\n"
        "• 6 類規則: CYP 抑制/誘導、QTc 累積、抗膽鹼能、5-HT 症候群、治療窗偏離、腎/肝劑量調整\\n"
        "• 嚴重度: LOW / MEDIUM / HIGH / CONTRAINDICATED\\n"
        "• 個體差異極大,用藥請遵醫囑或諮詢臨床藥師",
        "• 加入 2+ 個藥,RuleEngine 跑所有組合 (N×(N-1)/2 對)\\n"
        "• 6 類規則: CYP 抑制/誘導、QTc 累積、抗膽鹼能、5-HT 症候群、治療窗偏離、腎/肝劑量調整\\n"
        "• 嚴重度: LOW / MEDIUM / HIGH / CONTRAINDICATED\\n"
        "• 個體差異極大,用藥請遵醫囑或者問臨床藥師",
    ),
    "interaction_info_title": ("ℹ️ 多藥相互作用說明", "ℹ️ 多藥相互作用說明"),
    "interaction_more_drugs": ("…還有 %1$d 個藥(v0.4 加搜尋)", "…仲有 %1$d 個藥(v0.4 加搜尋)"),
    "interaction_n_drugs_available": ("%1$d 個藥可選", "%1$d 個藥可選"),
    "interaction_no_alerts_desc": (
        "但這不代表安全——本次評估僅覆蓋:CYP 抑制/誘導、QTc 累積、抗膽鹼能負荷、5-HT 症候群、治療窗偏離、腎/肝劑量調整。"
        "實際臨床還要考慮:年齡、肝腎、基因型(CYP2D6/2C19 慢代謝)、蛋白結合置換、腸肝循環、食物/草本補充劑等。"
        "個體差異極大,仍請遵醫囑。",
        "但呢個唔代表安全——今次評估只覆蓋:CYP 抑制/誘導、QTc 累積、抗膽鹼能負荷、5-HT 症候群、治療窗偏離、腎/肝劑量調整。"
        "實際臨床仲要考慮:年齡、肝腎、基因型(CYP2D6/2C19 慢代謝)、蛋白結合置換、腸肝循環、食物/草本補充劑等。"
        "個體差異極大,仲係請遵醫囑。",
    ),
    "interaction_no_alerts_title": ("✓ RuleEngine 未檢出 6 類已知警示。", "✓ RuleEngine 未檢出 6 類已知警示。"),
    "interaction_overdose_help": (
        "過量處理需多藥協調(活性炭 / 血液透析 / 解毒劑等), 立即撥打 120 或當地中毒急救中心",
        "過量處理要多藥協調(活性炭 / 血液透析 / 解毒劑等), 立即打 120 或當地中毒急救中心",
    ),
    "interaction_qtc_format": ("估算 QTc %1$.0f ms", "估算 QTc %1$.0f ms"),
    "interaction_reevaluate_n": ("重新評估 %1$d 藥的所有組合", "重新評估 %1$d 藥的所有組合"),
    "interaction_references": ("參考:%1$s", "參考:%1$s"),
    "interaction_selected_n": ("已選 (%1$d):", "已選 (%1$d):"),
    "interaction_serotonin_format": ("涉及 %1$d 個 5-HT 增強藥", "涉及 %1$d 個 5-HT 增強藥"),
    "interaction_title": ("相互作用 · %1$d 藥", "相互作用 · %1$d 藥"),
    # ---- 代谢途径 ----
    "pathway_beta_oxidation": ("β-氧化", "β-氧化"),
    "pathway_cyp450": ("CYP450 氧化", "CYP450 氧化"),
    "pathway_deiodination": ("脫碘", "脫碘"),
    "pathway_dpp4": ("DPP-IV 酶降解", "DPP-IV 酶降解"),
    "pathway_esterase": ("酯酶水解", "酯酶水解"),
    "pathway_glucuronidation": ("葡萄糖醛酸化", "葡萄糖醛酸化"),
    "pathway_hydrolysis": ("水解", "水解"),
    "pathway_mao": ("單胺氧化酶", "單胺氧化酶"),
    "pathway_other": ("其他", "其他"),
    "pathway_renal_excretion": ("腎排泄 (原型)", "腎排泄 (原型)"),
    "pathway_ugt_glucuronidation": ("UGT 葡萄糖苷酸化", "UGT 葡萄糖苷酸化"),
    # ---- 选药搜索 ----
    "search_brand_prefix": ("· 品牌: %1$s", "· 品牌: %1$s"),
    "search_count_with_query": ("· 搜尋:&quot;%1$s&quot;", "· 搜尋:&quot;%1$s&quot;"),
    "search_drug_count": ("%1$d 個藥", "%1$d 個藥"),
    "search_filtered": ("%1$d / %2$d 個藥", "%1$d / %2$d 個藥"),
    "search_hint": ("中文名 / 英文名 / 品牌名 / 拼音首字母", "中文名 / 英文名 / 品牌名 / 拼音首字母"),
    "search_no_match": ("未找到符合的藥", "未搵到相符嘅藥"),
    "search_placeholder": ("點擊搜尋選藥…", "點擊搜尋揀藥…"),
    "search_select_title": ("選藥 · %1$s", "揀藥 · %1$s"),
    # ---- 严重度 ----
    "sev_life_threatening": ("危及生命", "危及生命"),
    "sev_mild": ("輕度", "輕度"),
    "sev_moderate": ("中度", "中度"),
    "sev_severe": ("重度", "重度"),
    "sevrule_contraindicated": ("禁忌", "禁忌"),
    "sevrule_high": ("高度警示", "高度警示"),
    "sevrule_info": ("資訊", "資訊"),
    "sevrule_low": ("輕度提示", "輕度提示"),
    "sevrule_medium": ("中度警示", "中度警示"),
}

TARGETS = [("values-b+zh+Hant", 0), ("values-b+yue+Hant+HK", 1)]


def apply(folder, idx):
    path = os.path.join(RES, folder, "strings.xml")
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        text = f.read()

    done = set()
    for key, (hant, yue) in T.items():
        value = (hant, yue)[idx]
        pat = re.compile(r'(<string name="%s">)(.*?)(</string>)' % re.escape(key))
        m = pat.search(text)
        if not m:
            print("  !! 找不到 key: %s" % key)
            continue
        if m.group(2) == value:
            done.add(key)
            continue
        text = text[:m.start()] + m.group(1) + value + m.group(3) + text[m.end():]
        done.add(key)

    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)
    print("%s: 写入 %d 条" % (folder, len(done)))
    missing = set(T) - done
    if missing:
        print("  !! 未处理: %s" % sorted(missing))


for folder, idx in TARGETS:
    print("=== %s ===" % folder)
    apply(folder, idx)
print("done")
