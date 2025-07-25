# Meteor Addon Miku (彗星插件)

[English](README_EN.md) | 中文

这是一个为 [Meteor Client](https://meteorclient.com/) 设计的插件，旨在通过添加一系列实用、增强或自动化功能的模块来扩展 Meteor Client 的能力。

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x%20~%201.21-green.svg?style=for-the-badge&logo=minecraft)
![Language](https://img.shields.io/badge/语言-中文-blue.svg?style=for-the-badge)

## 声明

本插件包完全免费，所有模块均以实用和便利为目标，请合理使用。

本插件主要是生存、建造辅助功能，PVP功能不优先考虑。


## QQ群

1013297171

## ✨ 功能模块

不断添加新的模块中，你可以提出新功能的建议。目前的功能如下：

### **Auto Trash (自动扔垃圾)**
- **功能**: 自动丢弃您库存中指定的物品。
- **特点**:
    - **分组管理**: 您可以设置两组独立的物品列表，方便对不同类型的垃圾进行分类。
    - **自定义延迟**: 可自由调整丢弃物品的频率，防止操作过快。
    - **一键开关**: 在 GUI 中轻松启用或禁用。

### **Auto Sand Miner (自动挖沙)**
- **功能**: 智能自动挖沙，集成背包管理和工具维护功能。
- **特点**:
    - **智能挖掘**: 使用 Baritone 自动寻找并挖掘沙子
    - **背包管理**: 背包满时自动前往指定潜影盒存放沙子
    - **工具维护**: 铲子耐久度低时自动更换新工具
    - **自动返回**: 完成存储或换工具后自动返回挖掘位置
    - **可配置设置**: 自定义挖掘范围、延迟、潜影盒位置等

### **Auto Use Items (自动使用物品)**
- **功能**: 自动使用指定物品，支持多种使用模式和条件触发。
- **特点**:
    - **定时触发**: 定时自动使用物品
    - **智能触发**: 根据生命值、饥饿值等条件自动使用物品
    - **物品优先级**: 可设置多个物品的使用优先级
    - **使用延迟**: 防止过快使用物品导致浪费
    - **条件设置**: 支持血量阈值、饥饿值阈值等多种触发条件
    - **快捷栏管理**: 自动从背包补充快捷栏中的消耗品

### **Seed Mine (种子矿透)**
- **功能**: 基于世界种子的高级矿物透视，精确显示矿物位置。在有反矿透的服务器有用。
- **特点**:
    - **种子计算**: 使用世界种子精确计算矿物生成位置
    - **多维度支持**: 支持主世界、下界、末地的矿物透视
    - **智能渲染**: 可配置区块范围，优化性能表现
    - **空气检查**: 多种空气检查模式，确保矿物位置准确性
    - **Baritone集成**: 可同步矿物位置到Baritone自动挖矿
    - **生物群系适配**: 根据不同生物群系显示对应的矿物类型
    - **可视化显示**: 不同矿物使用不同颜色高亮显示
    - **实时更新**: 随着区块加载自动更新矿物位置信息

### **Elytra Finder (鞘翅搜索)**
- **功能**: 自动搜索带有末地船的末地城结构，快速定位鞘翅位置。
- **特点**:
    - **智能搜索**: 以玩家为中心，在指定半径内搜索所有带末地船的末地城
    - **种子计算**: 基于世界种子精确计算末地城生成位置，无需实际探索
    - **Xaero地图集成**: 自动将找到的鞘翅位置添加到Xaero地图路径点
    - **异步处理**: 使用多线程搜索，避免游戏卡顿
    - **智能过滤**: 只显示包含鞘翅的末地城（有末地船的末地城）
    - **距离排序**: 按距离从近到远排序，优先显示最近的鞘翅位置
    - **可配置范围**: 支持10-2000区块的搜索半径设置
    - **路径点管理**: 防重复添加，智能命名包含距离信息
    - **简洁输出**: 只显示找到的总数，不在聊天中刷屏

---

## 🛠️ 安装

1.  前往本项目的 **[Releases](https://github.com/mikumiku7/meteor-miku/releases)** 页面。 
2.  下载最新版本的 `.jar` 文件。
3.  将下载的 `.jar` 文件放入您 Minecraft 游戏目录下的 `mods` 文件夹。
4.  确保您已经安装了 Meteor Client 和 Fabric Loader。
5.  启动游戏。

## 🚀 使用方法

1.  启动游戏后，在游戏中按下 `右 Shift` 键打开 Meteor Client 的 GUI 界面。
2.  在最上面的modules右边的config配置font中, 选择中文字体,如 等线（dengxian）.
3.  在模块分类中找到本插件的分类.
4.  点击您想要使用的模块，即可在右侧进行详细设置。

## 🌐 语言支持

目前，本插件的所有模块界面和描述 **仅支持中文**。

如果没显示文字, 请安装 [xingke0/meteor_chinese: 为 meteor客户端 提供中文支持](https://github.com/xingke0/meteor_chinese)

## 🤝 如何贡献 (Contributing)

我们非常欢迎您加入本项目的开发！无论是提交代码、报告 Bug 还是提出新功能的建议，都是对项目宝贵的贡献。

- **报告问题**: 如果您在使用中遇到任何问题或有好的想法，请通过 **[Issues](https://github.com/YOUR_USERNAME/YOUR_REPOSITORY/issues)** 告诉我们。
- **贡献代码**:
    1.  Fork 本仓库。
    2.  创建一个新的分支 (`git checkout -b feature/YourAmazingFeature`)。
    3.  提交您的代码 (`git commit -m 'Add some AmazingFeature'`)。
    4.  将您的分支推送到 GitHub (`git push origin feature/YourAmazingFeature`)。
    5.  提交一个 Pull Request。


## 📄 许可证 (License)

本项目采用 [GPL3](LICENSE) 开源许可。

基于 [meteor-addon-template](https://github.com/MeteorDevelopment/meteor-addon-template) 开发。





## 参考

参考或者借鉴了下面的这些， 感谢他们

裤子条纹

https://github.com/etianl/Trouser-Streak/releases

jefff's mod

https://github.com/miles352/meteor-stashhunting-addon/releases

[自动交易](https://github.com/sebseb7/autotrade-fabric)

https://github.com/AntiCope/meteor-rejects

https://github.com/xingke0/meteor_chinese

https://anticope.pages.dev/addons/

https://github.com/JFronny/MeteorAdditions

https://github.com/RedCarlos26/HIGTools

https://github.com/maxsupermanhd/meteor-villager-roller
