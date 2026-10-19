"""v0.9h merge: 把 DoseCare 的 TDM / 浓度曲线 string key 搬到底座 4 个语言目录。

背景: TDM 页 (`ui/tdm/`) 移植自 DoseCare 的 `ui/TdmModule.kt` +
      `ui/InteractiveConcentrationChart.kt`,这两个文件用到 26 个 string key
      (tdm_* + chart_*),底座原本一个都没有(只有 chart_ 系里的
      common_known / common_remove 已存在,不在本清单内)。

做法: 译文直接从 DoseCare 的 strings.xml 里按 key 抽取原文搬运(不是手抄),
      这样 `%1$d` / `%1$.1f` / `&amp;` / `\n` 这些转义序列不会被改坏。
      底座语言目录 -> DoseCare 语言目录的映射与前几期一致:
        values               <- values-en  (英文)
        values-b+zh+Hans     <- values     (中文)
        values-b+zh+Hant     <- values-en  (英文回落)
        values-b+yue+Hant+HK <- values-en  (英文回落)

新增 key: `tdm_entry_title` —— 设置页入口项的**静态**标题。
      底座的 `interaction_title` 是 "相互作用 · %1$d 药",带计数占位符;静态入口
      复用它会让 AAPT 渲染出字面量 "%1$d"(前几期实机截图踩过)。所以单独加
      一个不带占位符的 key,同时给 Screen.Tdm 的 route title 用(那个不需要
      药数,用药数是逐屏 state,不是 route 级常量)。

⚠️ 不碰 values-ja —— 日语由另一条线并行合并,日文译文见交付报告。
⚠️ 不改 Room / DAO / ui/interaction / InteractionAnalysisRepository。

用法:
    python tools/_add_tdm_strings.py            # dry-run(默认,只打印计划)
    python tools/_add_tdm_strings.py --apply    # 真正写入
"""

import io
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
RES = os.path.join(ROOT, 'app/src/main/res')
SRC_RES = r'C:\Users\yuwen\Desktop\dosecare-workspace\dosecare\app\src\main\res'

# 底座语言目录 -> DoseCare 语言目录
LANG_MAP = {
    'values': 'values-en',
    'values-b+zh+Hans': 'values',
    'values-b+zh+Hant': 'values-en',
    'values-b+yue+Hant+HK': 'values-en',
}

# 从 DoseCare 搬过来的 26 个 key(全部在 values / values-en / values-ja 里都有)
PORTED_KEYS = [
    # TDM 主界面
    'tdm_title',
    'tdm_data_source_info',
    'tdm_no_drugs',
    'tdm_data_source_title',
    'tdm_data_source_lines',
    'tdm_limitations',
    'tdm_limitations_lines',
    'tdm_predict_n_drugs',        # %1$d
    'tdm_add_drug_to_chart',
    'tdm_add_drug_n',             # %1$d
    'tdm_add',
    'tdm_dose_mg',
    'tdm_weight_kg',
    'tdm_freq',
    'tdm_chart_title',
    'tdm_hide_cd',
    'tdm_show_cd',
    'tdm_no_window',
    # InteractiveConcentrationChart
    'chart_title',
    'chart_window_label',         # %1$d-%2$d
    'chart_reset_view',           # %1$s
    'chart_status_format',        # %1$.1f
    'chart_status_low',
    'chart_status_high',
    'chart_status_in_window',
    'chart_status_not_comparable',
]

# 底座新增的 key(DoseCare 没有对应项 —— 它没有设置页入口)
LOCAL_KEYS = {
    'values': 'Blood level monitoring',
    'values-b+zh+Hans': '血药浓度监测',
    'values-b+zh+Hant': 'Blood level monitoring',
    'values-b+yue+Hant+HK': 'Blood level monitoring',
}

STRING_RE = re.compile(r'<string name="([^"]+)"\s*>(.*?)</string>', re.DOTALL)


def load_source(path):
    """{key: 原始内文(含 \\n / &amp; 等转义)}"""
    content = io.open(path, encoding='utf-8-sig').read()
    return {m.group(1): m.group(2) for m in STRING_RE.finditer(content)}


def main():
    apply = '--apply' in sys.argv[1:]

    src_cache = {}
    for lang in set(LANG_MAP.values()):
        p = os.path.join(SRC_RES, lang, 'strings.xml')
        src_cache[lang] = load_source(p)

    missing = []
    for key in PORTED_KEYS:
        for lang in set(LANG_MAP.values()):
            if key not in src_cache[lang]:
                missing.append((lang, key))
    if missing:
        print('ABORT: DoseCare 源里缺 key,不能搬:')
        for lang, key in sorted(missing):
            print(f'  {lang}/{key}')
        return 1

    print(f'{"APPLY" if apply else "DRY-RUN"}: {len(PORTED_KEYS)} 搬运 + {len(LOCAL_KEYS)} 新增 '
          f'= {len(PORTED_KEYS) + len(LOCAL_KEYS)} key x {len(LANG_MAP)} 语言')
    print()

    for lang, src_lang in LANG_MAP.items():
        path = os.path.join(RES, lang, 'strings.xml')
        content = io.open(path, encoding='utf-8').read()
        new_content = content
        added = 0
        for key in PORTED_KEYS:
            if f'name="{key}"' in new_content:
                print(f'  SKIP(已存在) {lang}/{key}')
                continue
            value = src_cache[src_lang][key]
            new_content = new_content.replace(
                '</resources>',
                f'    <string name="{key}">{value}</string>\n</resources>'
            )
            added += 1
        local_key = 'tdm_entry_title'
        if f'name="{local_key}"' not in new_content:
            new_content = new_content.replace(
                '</resources>',
                f'    <string name="{local_key}">{LOCAL_KEYS[lang]}</string>\n</resources>'
            )
            added += 1
        else:
            print(f'  SKIP(已存在) {lang}/{local_key}')

        if added == 0:
            continue

        # 跟随各文件自身的行尾风格: 底座 res 是 LF, 不要改成 CRLF
        newline = '\r\n' if '\r\n' in new_content else '\n'
        block = (
            f'    <!-- v0.9h merge: TDM 血药浓度监测 + InteractiveConcentrationChart -->'
            f'{newline}'
            f'    <!-- 搬运自 DoseCare ui/TdmModule.kt + ui/InteractiveConcentration.kt -->'
            f'{newline}'
        )
        # 重新做一次: 把裸插入换成带注释头 + 跟随行尾的块
        # (上面用 \n 直接插的行已经进了 new_content, 这里不再重复插入, 只补注释头)
        new_content = new_content.replace(
            f'    <!-- v0.9h merge: TDM 血药浓度监测 + InteractiveConcentrationChart -->\n',
            block
        )

        print(f'  {"PATCH" if apply else "WOULD PATCH"} {lang} (+{added} keys, newline={newline!r})')
        if apply:
            with io.open(path, 'w', encoding='utf-8', newline='') as f:
                f.write(new_content)

    if not apply:
        print()
        print('dry-run 结束,未写任何文件。确认无误后加 --apply 重跑。')
    return 0


if __name__ == '__main__':
    sys.exit(main())
