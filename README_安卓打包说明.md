# 心智成长OS · 安卓 App v0.1 — Android Studio 工程

本压缩包包含一个**可编辑的 Android Studio 原生工程**，使用 Java + WebView 把先前的 HTML 体验版作为离线应用资源加载，并接入 Android 系统通知、文件导入/导出和打印能力。

## 已集成功能

1. 五大功能模块、100格经验槽、每日任务、晨间问答、日间记录、晚间复盘、手写画板。
2. 手机本地离线保存（WebView localStorage；不包含云同步）。
3. 每天 11:00 / 13:30 / 15:15 / 17:00 / 19:30 / 21:00 的六次安卓本地提醒（用户主动打开并授权通知后生效）。
4. JSON 数据导出/导入，PNG 画板图片保存，调用安卓系统打印。
5. 应用图标及全屏适配的暗色页面。

## 在 Android Studio 中生成 APK

1. 在电脑上安装 Android Studio，安装 Android SDK Platform API 35 和 Android SDK Build Tools。
2. 解压并使用 **Open** 打开包含 `settings.gradle` 的 `心智成长OS_安卓工程` 文件夹。
3. 这是精简工程，不包含 Gradle Wrapper 二进制文件（当前制作环境没有可用的 Gradle/Android SDK，也无法下载）。首次导入时请在 Android Studio 的 **Settings > Build, Execution, Deployment > Build Tools > Gradle** 中选择本机已安装的 Gradle 8.9，或让开发人员补齐对应版本的 Gradle Wrapper。
4. 安装或选择兼容的 JDK 17（Android Studio 自带 JDK 可用）。等待 Gradle 依赖同步。
5. 菜单 **Build > Build Bundle(s) / APK(s) > Build APK(s)**，成功后在 `app/build/outputs/apk/debug/app-debug.apk` 找到测试安装包。
6. 将 APK 复制到安卓手机安装。手机系统可能要求授权“安装未知来源应用”。仅在信任来源、理解风险时开启。

> 注意：本工程的源码已完成静态逻辑检查，但**未在 Android Studio 编译或在安卓真机执行回归测试**。不能把此包当成已验证可安装的 APK。

## 提醒可靠性说明

- 使用 Android 系统 AlarmManager 近似定时提醒（`setAndAllowWhileIdle`），避免申请受限的精确闹钟权限。
- 在 Android 13+，首次启用时需要用户授权通知权限；关闭通知权限后不会显示提醒。
- 手机的省电、后台限制和厂商设置可能导致延迟；不承诺精确到分钟。
- 重启手机后会尝试恢复提醒；卸载应用会删除本地数据与提醒。

## 数据与隐私

- 默认不联网、不创建账号、不发送数据到服务器。
- 使用 WebView localStorage 保存用户内容，Android 系统升级或卸载应用可能清除数据。请定期导出 JSON。
- 文件保存通过系统文件选择器进行；应用不申请完整相册或外部存储读写权限。
- 该项目为依据用户提供截图和公开方法设计的独立原型，**不是** Dan Koe 或截图应用的官方产品，也不包含其专有程序代码。

## 正式上架前还需

- 安卓真机兼容性测试、异常处理、通知投递实测、无障碍测试、性能优化。
- 如需云同步，需另行设计账号、加密传输、服务器、删除/导出机制及隐私政策。
- 上架 Google Play 需正式签名、商店资料、隐私披露和对应政策审核；这里仅提供测试工程。

## 没有 Android Studio，也可以使用 GitHub Actions 编译

本项目附带 `.github/workflows/build-android-apk.yml` 自动构建脚本。把压缩包里的**工程文件本身**上传到自己的 GitHub 仓库根目录（不应把工程外层目录多嵌套一级），打开仓库 Actions → Build Android debug APK → Run workflow，等工作流完成后在运行页面底部的 Artifacts 下载 `mind-growth-os-debug-apk`。这是可安装的**测试版 APK**，仍需在真实安卓手机上验证。

请不要把包含真实心智日记、身份等隐私的个人数据文件提交到公开代码仓库；工程仅含演示程序，不含真实记录。
