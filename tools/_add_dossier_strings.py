"""第 2 期-c: 药理档案区块的 string 资源 (4 语言)。

组件 ui/catalog/dossier/PharmacologyDossierSection.kt 用到 6 个 key。
占位符规矩(第 2 期踩过):带 %1$s / %1$d 的必须 stringResource(id, arg) 传参,
漏传会让 UI 显示字面量。dossier_summary_format 用 %1$s/%2$s 已经在组件里传了。
"""

import io
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
RES = os.path.join(ROOT, 'app/src/main/res')

ADDITIONS = {
    'values': [
        ('dossier_section_title', 'Pharmacology'),
        ('dossier_picker_label', 'Link to catalog entry'),
        ('dossier_search_placeholder', 'Search 202 catalog medicines by name or pinyin'),
        ('dossier_summary_format', '%1$s · %2$s'),
        ('dossier_no_indications', 'No indications listed'),
        ('dossier_unlink_action', 'Unlink from catalog'),
    ],
    'values-b+zh+Hans': [
        ('dossier_section_title', '药理档案'),
        ('dossier_picker_label', '关联药理目录条目'),
        ('dossier_search_placeholder', '按名称或拼音搜索 202 种药'),
        ('dossier_summary_format', '%1$s · %2$s'),
        ('dossier_no_indications', '未标注适应症'),
        ('dossier_unlink_action', '解除关联'),
    ],
    'values-b+zh+Hant': [
        ('dossier_section_title', '藥理檔案'),
        ('dossier_picker_label', '關聯藥理目錄條目'),
        ('dossier_search_placeholder', '按名稱或拼音搜尋 202 種藥'),
        ('dossier_summary_format', '%1$s · %2$s'),
        ('dossier_no_indications', '未標註適應症'),
        ('dossier_unlink_action', '解除關聯'),
    ],
    'values-b+yue+Hant+HK': [
        ('dossier_section_title', '藥理檔案'),
        ('dossier_picker_label', '關聯藥理目錄條目'),
        ('dossier_search_placeholder', '按名稱或拼音搜尋 202 種藥'),
        ('dossier_summary_format', '%1$s · %2$s'),
        ('dossier_no_indications', '未標註適應症'),
        ('dossier_unlink_action', '解除關聯'),
    ],
}


def main():
    for lang, items in ADDITIONS.items():
        path = os.path.join(RES, lang, 'strings.xml')
        content = io.open(path, encoding='utf-8').read()
        existing = set()
        missing = []
        for key, value in items:
            if f'name="{key}"' in content:
                existing.add(key)
            else:
                missing.append((key, value))
        if not missing:
            print('  SKIP %-22s (%d 条已存在)' % (lang, len(existing)))
            continue
        newline = '\r\n' if '\r\n' in content else '\n'
        block = '    <!-- v0.9g merge: 药理档案区块 -->' + newline
        for key, value in missing:
            block += '    <string name="%s">%s</string>%s' % (key, value, newline)
        content = content.replace('</resources>', block + '</resources>')
        with io.open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(content)
        print('  PATCH %-22s +%d 条 (已存在 %d)' % (lang, len(missing), len(existing)))

    print()
    print('占位符检查:')
    print('  dossier_summary_format 用 %%1$$s / %%2$$s,组件里 stringResource(id, categoryText, indicationText) 已传两参')


if __name__ == '__main__':
    main()
