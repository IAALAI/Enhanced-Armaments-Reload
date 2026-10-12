# Enhanced Armaments Reload 26.3-1.1.3

将 Forge 1.20.1 分支的原有玩法移植到 Minecraft 26.3 正式版（Fabric），对应发布分支 `26.3`。

- 保留原品质默认权重、效果和 JSON 自定义品质。
- 保留原技能列表、技能阶级、等级上限、经验购买和升级代币规则。
- 保留背包初始化、护甲逐件减伤、命中吸血、击杀经验及耐久修复、生命回复和霜行效果。
- 保留旧版强化界面布局与翻译；改用 26.3 的渲染接口，并为文字颜色补足 Alpha。
- 调整事件注册、命令、网络包和物品组件 API。新版组件读取返回副本，因此显式保存升级后的技能和代币数据。
- 构建环境：Java 25、Gradle 9.5.1、Fabric Loader 0.19.5、Fabric API 0.162.0+26.3、Loom 1.17.21。

此分支保持 `1.1.3` 玩法版本。`26.3_n` 的技能容量、耐久成长、品质合成、PRD、ICD、永恒技能和通知设置属于后续 `1.3.0` 更新。

Fabric 通用配置位于 `config/enhancedarmaments/common.json`，品质配置继续使用 `config/enhancedarmaments/rarity/*.json`。从 Forge 配置迁移时，将原 TOML 中的同名设置填入 JSON；此版本不自动导入 Forge TOML。

验证：`./gradlew build --no-daemon` 成功，6 项旧规则回归测试通过；专用服务器初始化及战斗 Mixin 加载检查通过。尚未进行客户端实际游玩验证。

发布文件：`build/libs/Enhanced-Armaments-Reload-26.3-1.1.3.jar`。
