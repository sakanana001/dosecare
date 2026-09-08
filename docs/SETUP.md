# SETUP · 从零跑通 v0.1

> 目标：让 DoseCare APP 编译出 debug APK 并装到手机。本文档写给"刚睡醒要装环境的你"。

## 5 分钟路径（最推荐）

**装 Android Studio（一次解决全部：JDK 17 + Android SDK + Gradle + IDE）**

1. 下载：**https://developer.android.com/studio**
2. 安装（约 5 分钟，下载 1-2GB；安装后约 5-7GB）
3. 第一次启动 → 选 **Standard** 模式 → 装 SDK 35 + Build Tools
4. **File → Open** → 选 `C:\Users\yuwen\Desktop\精品神药` 目录
5. 等 Gradle Sync（5-10 分钟，下载依赖 ~250MB）
6. **Build → Make Project** 或 `Ctrl+F9`（首次编译 2-3 分钟）
7. **Run → Run 'app'**（需要手机 USB 连着 + 开启 USB 调试）

完成。手机上能看到主屏。

## 脚本路径（已写好）

- `scripts/check-env.ps1` — 检查 JDK / Android SDK / Gradle 装没装
- `scripts/build.ps1` — 一键跑 `./gradlew assembleDebug`（前提：环境都装了）

如果装好 Android Studio 后想用纯命令行：
```powershell
cd C:\Users\yuwen\Desktop\精品神药
.\scripts\check-env.ps1    # 验证
.\scripts\build.ps1        # 编译
```

## 如果只想装 JDK（不装 Android Studio IDE）

```powershell
# 任选一种
winget install Microsoft.OpenJDK.17
# 或
choco install temurin17
```

然后单独装 Android SDK Command-Line Tools：
- 下载：https://developer.android.com/studio#command-line-tools-only
- 解压到 `C:\Android\Sdk`
- 设置环境变量：
  ```powershell
  [System.Environment]::SetEnvironmentVariable('ANDROID_HOME', 'C:\Android\Sdk', 'User')
  [System.Environment]::SetEnvironmentVariable('PATH', $env:PATH + ';C:\Android\Sdk\platform-tools;C:\Android\Sdk\cmdline-tools\latest\bin', 'User')
  ```
- 重开 PowerShell
- 装 SDK 35：
  ```bash
  sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
  ```

## 常见坑

| 坑 | 表现 | 修法 |
|---|---|---|
| JAVA_HOME 没设 | `./gradlew` 报 `JAVA_HOME is not set` | PowerShell：`$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.X.X-hotspot"` |
| SDK 35 没装 | `Failed to find Build Tools revision 35.0.0` | Android Studio → SDK Manager → 勾选 Android 35 + Build Tools 35.0.0 |
| Gradle 第一次下载慢 | Sync 5-10 分钟 | 正常，泡茶 |
| 手机不识别 | `adb devices` 空 | 手机开"开发者选项"+"USB 调试"，换根数据线 |
| 装上秒崩 `No native library` | SQLCipher .so 没加载 | 检查 `DatabaseModule` 里 `System.loadLibrary("sqlcipher")` 还在 |
| 装上闪退 `no such table: user_drugs` | 首次启动没建表 | 检查 `Room.databaseBuilder` 调用，删除 APP 数据重装 |

## 编译成功后报告给我

发我以下任一信号（你睡醒后）：
- "跑通了" → 我推进 P1 #2
- "编译失败 + 错误日志" → 我修
- "手机装上了，看到 5 个药" → 我加新功能
- "装上后崩了 + logcat" → 我修

## 主屏该看到什么

1. 顶部"DoseCare v0.1"标题
2. 状态横幅 "v0.1 P1 #1 实施完成" 之类
3. "DrugCatalog 加载成功 · 5 个精神科核心药"
4. "氯氮平 100mg bid 第 5 天：Cmax=... Cmin=... Cavg=... ng/mL"
5. **新按钮**："插入测试 UserDrug"
6. **新指标**："已落库 X 条"
7. 5 张药卡片：氯氮平/奥氮平/利培酮/丙戊酸/锂
8. 底部"⚠️ 仅供参考"免责声明

如果主屏显示正常 + 按钮可点 + logcat 输出 `Inserted UserDrug: id=1, drugCatalogId=clozapine, doseMg=100.0` —— **v0.1 验收通过**，准备进 v0.3。
