"""修 CompareTab 嵌套 lambda K2 解析 cmpVm 失败 — 在 lambda 顶部加 val alias"""
from pathlib import Path
p = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t = p.read_text(encoding="utf-8")

# 在 CompareTab 头部 val cmpVm 后加一个普通 val 让 K2 识别
old = "    val cmpVm: CompareViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[CompareViewModel::class.java]\n    val drugAId by cmpVm.drugAId.collectAsStateWithLifecycle()"
new = "    val cmpVm: CompareViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[CompareViewModel::class.java]\n    val closeCmpInfo: () -> Unit = { cmpVm.setInfoDialogOpen(false) }\n    val drugAId by cmpVm.drugAId.collectAsStateWithLifecycle()"
t = t.replace(old, new)

# 替换 line 619
t = t.replace(
    'TextButton(onClick = { cmpVm.setInfoDialogOpen(false) }) { Text("知道了") }',
    'TextButton(onClick = closeCmpInfo) { Text("知道了") }'
)
p.write_text(t, encoding="utf-8")
print("OK")
