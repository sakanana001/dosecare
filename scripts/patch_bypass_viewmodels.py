"""绕过 viewModels() delegate, 用 ViewModelProvider + LocalViewModelStoreOwner (K2 兼容)"""
from pathlib import Path

# 改 BottomNavTabs.kt: 把 viewModels() 改成 ViewModelProvider(...).get(...)
# 加 helper import
p1 = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t1 = p1.read_text(encoding="utf-8")

# 移除 viewModels import, 加 LocalViewModelStoreOwner + ViewModelProvider
t1 = t1.replace(
    "import androidx.lifecycle.viewmodel.compose.viewModels\n",
    "import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner\nimport androidx.lifecycle.ViewModelProvider\n"
)

# 替换 3 个 viewModels 调用
# CatalogTab: val catVm: CatalogViewModel = viewModels()
t1 = t1.replace(
    "val catVm: CatalogViewModel = viewModels()",
    "val catVm: CatalogViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[CatalogViewModel::class.java]"
)
# CompareTab
t1 = t1.replace(
    "val cmpVm: CompareViewModel = viewModels()",
    "val cmpVm: CompareViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[CompareViewModel::class.java]"
)
# InteractionsTab
t1 = t1.replace(
    "val intVm: InteractionsViewModel = viewModels()",
    "val intVm: InteractionsViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[InteractionsViewModel::class.java]"
)
p1.write_text(t1, encoding="utf-8")
print("OK: BottomNavTabs.kt 改用 ViewModelProvider")

# 改 PrescriptionTab.kt
p2 = Path("app/src/main/java/com/dosecare/app/ui/PrescriptionTab.kt")
t2 = p2.read_text(encoding="utf-8")
t2 = t2.replace(
    "import androidx.lifecycle.viewmodel.compose.viewModels\n",
    "import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner\nimport androidx.lifecycle.ViewModelProvider\n"
)
t2 = t2.replace(
    "val vm: PrescriptionViewModel = viewModels()",
    "val vm: PrescriptionViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[PrescriptionViewModel::class.java]"
)
p2.write_text(t2, encoding="utf-8")
print("OK: PrescriptionTab.kt 改用 ViewModelProvider")
