"""第 2 期: 给设置页的相互作用入口补一个不带计数占位符的标题。

背景: DoseCare 的 `interaction_title` 是 "相互作用 · %1$d 药",本是为检测页顶栏
      动态显示已选药数用的。设置页的入口项是静态标题,直接复用会让 AAPT 渲染出
      字面量 "%1$d"(实机截图确认)。

做法: 新增 `interaction_entry_title`(纯静态标题,4 语言),设置页改用它。
      检测页顶栏继续用 `interaction_title` + 参数,行为不变。
"""

import io
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
RES = os.path.join(ROOT, 'app/src/main/res')

# 底座语言目录 -> DoseCare 语言目录(与前两期一致的映射)
LANG_MAP = {
    'values': 'values-en',
    'values-b+zh+Hans': 'values',
    'values-b+zh+Hant': 'values-en',
    'values-b+yue+Hant+HK': 'values-en',
}

SRC_RES = r'C:\Users\yuwen\Desktop\dosecare-workspace\dosecare\app\src\main\res'

KEY = 'interaction_entry_title'


def main():
    # 取 DoseCare 各语言里最接近的静态标题作为译文来源:
    # interaction_no_alerts_title 是纯静态的中文标题,直接复用它的措辞风格。
    src_values = {}
    for lang in ('values', 'values-en'):
        p = os.path.join(SRC_RES, lang, 'strings.xml')
        content = io.open(p, encoding='utf-8').read()
        src_values[lang] = content

    additions = {
        'values': '<string name="interaction_entry_title">Drug interactions</string>',
        'values-b+zh+Hans': '<string name="interaction_entry_title">药物相互作用</string>',
        'values-b+zh+Hant': '<string name="interaction_entry_title">藥物相互作用</string>',
        'values-b+yue+Hant+HK': '<string name="interaction_entry_title">藥物相互作用</string>',
    }

    for lang, line in additions.items():
        path = os.path.join(RES, lang, 'strings.xml')
        content = io.open(path, encoding='utf-8').read()
        if f'name="{KEY}"' in content:
            print(f'  SKIP (已存在) {lang}')
            continue
        # 跟随各文件自身的行尾风格:B 的 strings.xml 是 LF,不要改成 CRLF
        newline = '\r\n' if '\r\n' in content else '\n'
        block = (
            f'    <!-- v0.9g merge: 设置页入口标题(静态,不带计数占位符) -->{newline}'
            f'    {line}{newline}'
        )
        content = content.replace('</resources>', block + '</resources>')
        with io.open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(content)
        print(f'  PATCH {lang}')


if __name__ == '__main__':
    main()
