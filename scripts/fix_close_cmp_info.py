from pathlib import Path
p = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t = p.read_text(encoding="utf-8")
t = t.replace('    val closeCmpInfo: () -> Unit = { CompareViewModel.setInfoDialogOpen(false) }\n', '')
# 替换 TextButton 调用 — 避免引号问题, 用 chr(34)
old = 'TextButton(onClick = closeCmpInfo) { Text(' + chr(34) + '知道了' + chr(34) + ') }'
new = 'TextButton(onClick = { CompareViewModel.setInfoDialogOpen(false) }) { Text(' + chr(34) + '知道了' + chr(34) + ') }'
t = t.replace(old, new)
p.write_text(t, encoding="utf-8")
print("OK")
