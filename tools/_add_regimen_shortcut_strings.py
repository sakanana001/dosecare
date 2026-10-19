"""第 2 期-c: 给相互作用检测页加"用我的用药方案"快捷导入按钮的文案。"""

import io
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
RES = os.path.join(ROOT, 'app/src/main/res')

# 4 语言:英文默认 + 简中 + 繁中 + 港繁。跟前面几期一致,港繁沿用繁中用词。
ADDITIONS = {
    'values': [
        ('interaction_use_my_regimen', 'Use my regimen'),
        ('interaction_use_my_regimen_n', 'Use my regimen (%1$d)'),
    ],
    'values-b+zh+Hans': [
        ('interaction_use_my_regimen', '用我的用药方案'),
        ('interaction_use_my_regimen_n', '用我的用药方案（%1$d）'),
    ],
    'values-b+zh+Hant': [
        ('interaction_use_my_regimen', '用我的用藥方案'),
        ('interaction_use_my_regimen_n', '用我的用藥方案（%1$d）'),
    ],
    'values-b+yue+Hant+HK': [
        ('interaction_use_my_regimen', '用我的用藥方案'),
        ('interaction_use_my_regimen_n', '用我的用藥方案（%1$d）'),
    ],
}


def main():
    for lang, items in ADDITIONS.items():
        path = os.path.join(RES, lang, 'strings.xml')
        content = io.open(path, encoding='utf-8').read()
        if 'interaction_use_my_regimen' in content:
            print(f'  SKIP (已存在) {lang}')
            continue
        # 跟随该文件自身的行尾:B 的 strings.xml 是 LF
        newline = '\r\n' if '\r\n' in content else '\n'
        block = '    <!-- v0.9g merge: 用药方案快捷导入 -->' + newline
        for key, value in items:
            block += f'    <string name="{key}">{value}</string>' + newline
        content = content.replace('</resources>', block + '</resources>')
        with io.open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(content)
        print(f'  PATCH {lang}  +{len(items)} 条')


if __name__ == '__main__':
    main()
