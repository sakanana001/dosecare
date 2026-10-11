"""把 UI 文案里的 PicoHRT 品牌词换成 DoseCare。

背景: 第 0 期包名 rebrand 只覆盖了代码/资源名,没覆盖 strings.xml 里的**文案内容**。
      实机验证时在 Settings → About 看到 "PicoHRT on GitHub" 露在 UI 上,
      onboarding 流程还有 6 处 "PicoHRT 需要以下权限…" 之类的说明文字。
      用户要求 PicoHrt 不能出现在 UI,所以全量清掉。

按字节替换,保留 CRLF/LF 与 BOM。

用法:
    python tools/_merge_ui_brand_to_dosecare.py           # dry-run
    python tools/_merge_ui_brand_to_dosecare.py --apply
"""

import glob
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

# 只做纯 ASCII 的品牌词替换 —— 在任何语言的文案里都是同一个 token,
# 不会误伤译文的其余部分。
OLD_BRAND = b'PicoHRT'
NEW_BRAND = b'DoseCare'

# 已知含品牌词的 string key(留档,便于 review 时对账)
EXPECTED_KEYS = [
    'settings_github_repo',
    'onboarding_disclaimer_subtitle',
    'onboarding_notifications_subtitle',
    'onboarding_notifications_permission_subtitle',
    'onboarding_notifications_master_subtitle',
    'onboarding_notifications_master_alarm_subtitle',
    'onboarding_useful_subtitle',
    'onboarding_open_app',
]

APPLY = '--apply' in sys.argv


def main():
    targets = sorted(glob.glob(os.path.join(ROOT, 'app/src/*/res/values*/strings.xml')))
    print(f'扫描 {len(targets)} 个 strings.xml')

    pending = []
    total = 0
    for path in targets:
        with open(path, 'rb') as f:
            data = f.read()
        n = data.count(OLD_BRAND)
        if n == 0:
            continue
        new = data.replace(OLD_BRAND, NEW_BRAND)
        pending.append((path, new, n))
        total += n
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        print(f'  {rel}   {n} 处')

    print()
    print(f'合计 {total} 处,分布在 {len(pending)} 个文件')

    if not APPLY:
        print('[dry-run] 加 --apply 执行')
        return

    for path, new, n in pending:
        with open(path, 'wb') as f:
            f.write(new)
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        print(f'  PATCH  {rel}  ({n} 处)')

    # 校验: 期望的 key 是否都覆盖到了
    print()
    print('校验关键 key 是否还含旧品牌词:')
    all_clean = True
    for key in EXPECTED_KEYS:
        marker = f'name="{key}"'.encode()
        still = []
        for path, _, _ in pending:
            with open(path, 'rb') as f:
                c = f.read()
            idx = c.find(marker)
            if idx == -1:
                continue
            end = c.find(b'</string>', idx)
            seg = c[idx:end]
            if OLD_BRAND in seg:
                still.append(os.path.relpath(path, ROOT))
        status = 'OK' if not still else f'仍残留 {still}'
        if still:
            all_clean = False
        print(f'  {key:<46} {status}')

    print()
    print('全部清理完成' if all_clean else '⚠️ 仍有残留,需人工处理')
    print(f'注意: 期望覆盖 {len(EXPECTED_KEYS)} 个 key;上面若某个 key 没出现在"扫描"列表里,'
          f'说明它在某个语言文件里本来就没有这个 key(非阻断)。')


if __name__ == '__main__':
    main()
