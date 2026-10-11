"""第 1 期: 把 DoseCare 的 104 条药理枚举翻译资源搬进合并底座。

DoseCare 的药理 enum(DrugCategory / IndicationGroup / PathwayType / CypEnzyme /
Severity / OverdoseSeverity)在 domain 层用 `@StringRes val displayNameRes: Int` 绑 R 资源,
但合并底座 PicoHRT 的架构规矩是 model 包纯 Kotlin 不碰 Android,所以那层字段被移除了,
enum 只剩硬编码 displayName。本脚本把对应的 104 条 R 资源补回底座的 strings.xml,
并生成 PharmaLabels.kt 的映射表,供 UI 层在本地化时使用。

底座有 4 个语言文件(默认 en / 简中 / 繁中 / 港繁),DoseCare 只有 zh(默认)/ en / ja:
- 默认 en:  从 DoseCare values-en 取
- 简中:      从 DoseCare values(默认中文)取
- 繁中 / 港繁: DoseCare 没有,从英文回落并打标,后续人工补翻译
- 日语:      第 3 期做(决策:要日语),此脚本先不落 values-ja

用法:
    python tools/_merge_pharma_strings.py            # dry-run
    python tools/_merge_pharma_strings.py --apply
"""

import io
import os
import re
import sys
from xml.sax.saxutils import escape

WS = r'C:\Users\yuwen\Desktop\dosecare-workspace'
SRC_DOMAIN = os.path.join(WS, 'dosecare/app/src/main/java/com/dosecare/app/domain')
SRC_RES = os.path.join(WS, 'dosecare/app/src/main/res')
DST_ROOT = os.path.join(WS, 'merged')
DST_RES = os.path.join(DST_ROOT, 'app/src/main/res')

# 底座语言目录 -> DoseCare 语言目录
LANG_MAP = {
    'values': 'values-en',                 # 底座默认是英文
    'values-b+zh+Hans': 'values',          # DoseCare 默认是中文
    'values-b+zh+Hant': 'values-en',       # 暂无繁中译文,回落英文
    'values-b+yue+Hant+HK': 'values-en',   # 暂无港繁译文,回落英文
}

APPLY = '--apply' in sys.argv


def collect_enum_r_keys():
    """从 DoseCare domain 源码里抓 enum -> {枚举值: R.string key}。"""
    mapping = {}
    order = []
    for dirpath, _, filenames in os.walk(SRC_DOMAIN):
        for name in sorted(filenames):
            if not name.endswith('.kt'):
                continue
            path = os.path.join(dirpath, name)
            content = io.open(path, encoding='utf-8').read()
            for em in re.finditer(
                r'enum class (\w+)\([^)]*?\)[^{]*\{([\s\S]*?)\n\}', content
            ):
                enum_name, body = em.group(1), em.group(2)
                entries = re.findall(
                    r'^    ([A-Z][A-Z0-9_]*)\([^)]*?R\.string\.(\w+)', body, re.M
                )
                if entries:
                    if enum_name not in mapping:
                        order.append(enum_name)
                    mapping[enum_name] = entries
    return [(name, mapping[name]) for name in sorted(order)]


def load_source_strings():
    """读 DoseCare 每个语言的 strings.xml,返回 {key: 值}。"""
    out = {}
    for lang in ('values', 'values-en', 'values-ja'):
        path = os.path.join(SRC_RES, lang, 'strings.xml')
        if not os.path.exists(path):
            continue
        content = io.open(path, encoding='utf-8').read()
        table = {}
        for m in re.finditer(r'<string name="([^"]+)">(.*?)</string>', content, re.S):
            table[m.group(1)] = m.group(2)
        out[lang] = table
    return out


def main():
    enums = collect_enum_r_keys()
    total_keys = sum(len(e) for _, e in enums)
    print(f'抓到的 enum: {len(enums)} 个,共 {total_keys} 条 R 资源')
    for name, entries in enums:
        print(f'  {name:<20} {len(entries):>3} 条')
    print()

    src = load_source_strings()
    needed = {k for _, entries in enums for _, k in entries}
    print(f'涉及 {len(needed)} 个唯一 string key')
    for lang in ('values', 'values-en'):
        have = len(needed & set(src.get(lang, {})))
        print(f'  DoseCare {lang:<10} 覆盖 {have}/{len(needed)}')
    print()

    print('按底座语言目录的覆盖情况:')
    plan = {}
    for dst_lang, src_lang in LANG_MAP.items():
        table = src.get(src_lang, {})
        have = {k for k in needed if k in table}
        missing = needed - have
        plan[dst_lang] = (src_lang, have, missing)
        flag = '(英文回落)' if src_lang == 'values-en' and dst_lang not in ('values', 'values-b+zh+Hans') else ''
        print(f'  {dst_lang:<22} <- {src_lang:<10} {len(have):>3}/{len(needed)}  {flag}')
    print()

    if not APPLY:
        print('[dry-run] 加 --apply 执行')
        return

    added_total = 0
    for dst_lang, (src_lang, have, _) in plan.items():
        dst_dir = os.path.join(DST_RES, dst_lang)
        os.makedirs(dst_dir, exist_ok=True)
        path = os.path.join(dst_dir, 'strings.xml')
        existing_keys = set()
        if os.path.exists(path):
            content = io.open(path, encoding='utf-8').read()
            existing_keys = set(re.findall(r'<string name="([^"]+)">', content))
        else:
            content = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n'

        table = src.get(src_lang, {})
        new_entries = []
        skipped = 0
        for _, entries in enums:
            for _, rkey in entries:
                if rkey in existing_keys or rkey in new_entries:
                    skipped += 1
                    continue
                value = table.get(rkey)
                if value is None:
                    continue
                new_entries.append((rkey, value))

        if new_entries:
            block = '\n'.join(
                f'    <string name="{k}">{escape(v)}</string>' for k, v in new_entries
            )
            marker = '</resources>'
            content = content.replace(marker, f'\n    <!-- v0.9g merge: 药理枚举翻译 (来自 DoseCare {src_lang}) -->\n{block}\n{marker}')
            with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
                f.write(content)

        added_total += len(new_entries)
        print(f'  {dst_lang:<22} +{len(new_entries):>3} 条 (已存在跳过 {skipped})')

    print()
    print(f'共写入 {added_total} 条资源')

    # 生成 PharmaLabels 映射表,放进 model/pharmacology/ —— 唯一的 R 引用点
    labels_path = os.path.join(
        DST_ROOT, 'app/src/main/java/com/dosecare/app/model/pharmacology/PharmaLabels.kt'
    )
    lines = [
        'package com.dosecare.app.model.pharmacology',
        '',
        'import com.dosecare.app.R',
        '',
        '/**',
        ' * v0.9g merge: 药理枚举 -> 本地化 string 资源的映射。',
        ' *',
        ' * 这是 model/pharmacology 包里**唯一**允许出现 R 引用的文件。',
        ' * enum 本身保持纯 Kotlin(架构规矩见 docs/architecture.md:`model` is pure Kotlin),',
        ' * 只保留硬编码 displayName;UI 层需要本地化时调这里的 labelRes()。',
        ' *',
        ' * 资源 key 与 DoseCare 原仓库逐字一致(见 docs/third-party-notices.md 的数据源声明),',
        ' * 由 tools/_merge_pharma_strings.py 从 DoseCare 机械搬运,不要手工改名。',
        ' */',
        '',
    ]
    for name, entries in enums:
        lines.append(f'fun {name}.labelRes(): Int = when (this) {{')
        for const, rkey in entries:
            lines.append(f'    {name}.{const} -> R.string.{rkey}')
        lines.append('}')
        lines.append('')

    with io.open(labels_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines))
    print(f'生成 PharmaLabels.kt ({len(needed)} 条映射)')


if __name__ == '__main__':
    main()
