"""PrescriptionTab 改用 ViewModel: setInfoDialogOpen + toggleGroup"""
from pathlib import Path
p = Path("app/src/main/java/com/dosecare/app/ui/PrescriptionTab.kt")
t = p.read_text(encoding="utf-8")

# infoDialogOpen 三处赋值
t = t.replace(
    "onClick = { infoDialogOpen = true }",
    "onClick = { vm.setInfoDialogOpen(true) }"
)
t = t.replace(
    "onDismissRequest = { infoDialogOpen = false }",
    "onDismissRequest = { vm.setInfoDialogOpen(false) }"
)
t = t.replace(
    'TextButton(onClick = { infoDialogOpen = false }) { Text("知道了") }',
    'TextButton(onClick = { vm.setInfoDialogOpen(false) }) { Text("知道了") }'
)
# expandedGroups 折叠/展开
t = t.replace(
    "onToggleExpand = {\n                            expandedGroups = if (group.id in expandedGroups) expandedGroups - group.id else expandedGroups + group.id\n                        },",
    "onToggleExpand = { vm.toggleGroup(group.id) },"
)
p.write_text(t, encoding="utf-8")
print("OK")
