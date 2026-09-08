"""把 3 个 by catVm.X 改成 by CatalogViewModel.X (直接引用 object)"""
from pathlib import Path

# BottomNavTabs.kt
p1 = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t1 = p1.read_text(encoding="utf-8")
t1 = t1.replace("by catVm.topLevelOpen.collectAsState()", "by CatalogViewModel.topLevelOpen.collectAsState()")
t1 = t1.replace("by catVm.expandedCategories.collectAsState()", "by CatalogViewModel.expandedCategories.collectAsState()")
t1 = t1.replace("by catVm.expandedIndications.collectAsState()", "by CatalogViewModel.expandedIndications.collectAsState()")
t1 = t1.replace("by cmpVm.drugAId.collectAsState()", "by CompareViewModel.drugAId.collectAsState()")
t1 = t1.replace("by cmpVm.drugBId.collectAsState()", "by CompareViewModel.drugBId.collectAsState()")
t1 = t1.replace("by cmpVm.infoDialogOpen.collectAsState()", "by CompareViewModel.infoDialogOpen.collectAsState()")
t1 = t1.replace("cmpVm.", "CompareViewModel.")
t1 = t1.replace("by intVm.selectedIds.collectAsState()", "by InteractionsViewModel.selectedIds.collectAsState()")
t1 = t1.replace("by intVm.results.collectAsState()", "by InteractionsViewModel.results.collectAsState()")
t1 = t1.replace("by intVm.hasEvaluated.collectAsState()", "by InteractionsViewModel.hasEvaluated.collectAsState()")
t1 = t1.replace("by intVm.infoDialogOpen.collectAsState()", "by InteractionsViewModel.infoDialogOpen.collectAsState()")
t1 = t1.replace("intVm.", "InteractionsViewModel.")
# closeCmpInfo 之前用的 cmpVm 改完已经替换成 CompareViewModel, 但 closeCmpInfo 的 lambda 闭包也已替换
p1.write_text(t1, encoding="utf-8")
print("OK: BottomNavTabs.kt 改用 object 直接引用")

# PrescriptionTab.kt
p2 = Path("app/src/main/java/com/dosecare/app/ui/PrescriptionTab.kt")
t2 = p2.read_text(encoding="utf-8")
# val vm: PrescriptionViewModel = ... 已被删
# 改 by vm.X 改 by PrescriptionViewModel.X
t2 = t2.replace("by vm.expandedGroups.collectAsState()", "by PrescriptionViewModel.expandedGroups.collectAsState()")
t2 = t2.replace("by vm.infoDialogOpen.collectAsState()", "by PrescriptionViewModel.infoDialogOpen.collectAsState()")
t2 = t2.replace("vm.setInfoDialogOpen(", "PrescriptionViewModel.setInfoDialogOpen(")
t2 = t2.replace("vm.toggleGroup(", "PrescriptionViewModel.toggleGroup(")
t2 = t2.replace("vm.", "PrescriptionViewModel.")
p2.write_text(t2, encoding="utf-8")
print("OK: PrescriptionTab.kt 改用 object 直接引用")
