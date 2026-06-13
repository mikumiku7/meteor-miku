# Meteor Addon Miku (彗星插件)

本项目采用 [GPL3](LICENSE) 开源许可。任何包含或修改该代码的衍生作品，都必须以相同的 GPLv3 许可证免费公开其全部源代码。


[English](README_EN.md) | 中文

这是一个为 [Meteor Client](https://meteorclient.com/) 设计的插件，添加一系列实用、增强或自动化功能的模块, 增强生电和生存能力。

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x%20~%201.21-green.svg?style=for-the-badge&logo=minecraft)
![Language](https://img.shields.io/badge/语言-中文-blue.svg?style=for-the-badge)

![scr.png](pic/scr.png)

## 声明

本插件主要是生存、建造辅助功能，PVP功能不优先考虑。


## QQ群

1013297171


## 🛠️ 安装

1.  前往本项目的 **[Releases](https://github.com/mikumiku7/meteor-miku/releases)** 页面。
2.  下载对应的最新版本的 `.jar` 文件。
3.  将下载的 `.jar` 文件放入您 Minecraft 游戏目录下的 `mods` 文件夹。
4.  确保您已经安装了 Meteor Client 和 https://github.com/xingke0/meteor_chinese
5.  启动游戏。

## 🚀 使用方法

1.  启动游戏后，在游戏中按下 `右 Shift` 键打开 Meteor Client 的 GUI 界面。
2.  在最上面的modules右边的config配置font中, 选择中文字体,如 等线（dengxian）.
3.  在模块分类中找到本插件的分类.
4.  点击您想要使用的模块，即可在右侧进行详细设置。

## 注意

目前，本插件的所有模块界面和描述 **仅支持中文**。

如果没显示文字, 请安装 [xingke0/meteor_chinese: 为 meteor客户端 提供中文支持](https://github.com/xingke0/meteor_chinese)

## ✨ 功能模块介绍

不断添加新的模块中，你可以提出新功能的建议。目前的功能如下：

* **自动扔垃圾** - 根据两组独立列表与自定义延迟设置，自动丢弃背包中的指定物品。
* **自动挖沙** - 通过 Baritone 自动寻找并挖掘沙子，同时集成工具维护、自动返回与潜影盒存储管理。
* **自动使用物品** - 根据时间或血量与饥饿值阈值，按照优先级和自定义延迟自动使用指定物品。
* **种子矿透** - 基于世界种子精确计算并高亮所有维度的矿物生成位置，并支持同步至 Baritone 自动挖矿。
* **鞘翅搜索** - 基于世界种子在指定半径内搜索带末地船的末地城，并将其按距离排序自动添加到 Xaero 地图路径点。
* **结构搜索** - 基于世界种子多线程搜索双维度的多种游戏结构，并按距离远近排序自动添加至 Xaero 地图路径点。
* **自动种树** - 自动寻找合适地点种植树苗，并智能使用骨粉施肥以加速树木生长。
* **潜影盒物品获取器** - 自动搜索背包中包含目标物品的潜影盒，并完成放置、提取及回收的全自动操作。
* **自动打讲台** - 自动破坏并放置讲台以刷新村民交易，直到筛选出满足配置要求的特定附魔书。
* **靓仔转圈** - 让玩家以自定义角度持续旋转，并在检测到玩家移动时自动暂停。
* **自动挡水晶** - 实时检测附近自定义范围内的末影水晶，并立即放置最佳防护方块以阻挡爆炸伤害。
* **无限耐久鞘翅** - 通过周期性切换装备状态实现鞘翅零耐久消耗，并自动使用烟花火箭维持滑翔飞行。

---


## 🤝 如何贡献 (Contributing)

欢迎加入本项目的开发！提交代码、报告 Bug、提出新功能的建议，都是对项目宝贵的贡献。

- **报告问题**: 如果您在使用中遇到任何问题或有好的想法，请通过 **[Issues](https://github.com/YOUR_USERNAME/YOUR_REPOSITORY/issues)** 告诉我们。
- **贡献代码**:
    1.  Fork 本仓库。
    2.  创建一个新的分支 (`git checkout -b feature/YourAmazingFeature`)。
    3.  提交您的代码 (`git commit -m 'Add some AmazingFeature'`)。
    4.  将您的分支推送到 GitHub (`git push origin feature/YourAmazingFeature`)。
    5.  提交一个 Pull Request。



## 多版本编译构建

运行 build-all.bat 即可一次性编译1.21.1 - 1.21.11 的jar

CrossVersionCompat Via 跨版本代码实现




## 📄 许可证 (License)

本项目采用 [GPL3](LICENSE) 开源许可。任何包含或修改该代码的衍生作品，都必须以相同的 GPLv3 许可证免费公开其全部源代码。

基于 [meteor-addon-template](https://github.com/MeteorDevelopment/meteor-addon-template) 开发。





## 参考 Credits

参考或者借鉴了下面的这些项目， 感谢他们

https://anticope.pages.dev/addons/

https://github.com/etianl/Trouser-Streak/releases

https://github.com/miles352/meteor-stashhunting-addon/releases

[自动交易](https://github.com/sebseb7/autotrade-fabric)

https://github.com/AntiCope/meteor-rejects

https://github.com/xingke0/meteor_chinese

https://github.com/JFronny/MeteorAdditions

https://github.com/RedCarlos26/HIGTools

https://github.com/maxsupermanhd/meteor-villager-roller

https://github.com/KassuK1/BlackOut

https://github.com/iM4dCat/Alien

https://github.com/60124808866/OpenMyau

https://github.com/lambda-client/lambda

https://github.com/CCBlueX/LiquidBounce

https://github.com/srgantmoomoo/postman

https://github.com/220814/Lemon-0.0.9-buildable

https://github.com/SkidderMC/FDPClient

https://github.com/ItziSpyder/ClickCrystals

https://github.com/hax0r31337/ProtoHax

https://github.com/ArsenicClient/Arsenic

https://github.com/Aspw-w/NightX-Client

https://github.com/cutepuppy73/enhance-client

https://github.com/MeteorDevelopment/meteor-client

https://github.com/ApertureStatic/KuraNG-Public-Edition

https://github.com/BynBangsAlts/epitaph-crack

https://github.com/ya-ilya/progreso

https://github.com/TangyKiwi/KiwiClient

https://github.com/ghluka/MedvedClient

https://github.com/tranarchy/nicotine

https://github.com/TrilliumSolutions/Epitaph

https://github.com/SyutoBestCoder/Byte-1.21

https://github.com/G8LOL/Artemis-Hacked-client

https://github.com/lolwut2/Shoreline-V2

https://github.com/cutepuppy73/oxium-client

https://github.com/RageCat420/AlienClient-OpenSource

https://github.com/tanishisherewithhh/Smp-hack

https://github.com/DLindustries/System

https://github.com/antidotxe/silkxd

https://github.com/maththekid/fog-client

https://github.com/4everdies/fogclient

https://github.com/60124808866/OpenMyau

https://github.com/NamiDevelopment/nami

https://github.com/HopefulHorse453/OpenAstralis

https://github.com/Logging4J/CurryMod

https://github.com/PhilipPanda/Temple-Client

https://github.com/tRollaURa/Caizm

https://github.com/Zane2b2t/Grassware.win-rewrite

https://github.com/doickswag/drughack

https://github.com/MrHakan/Agalar-Hack

https://github.com/TrilliumSolutions/198macros-v1.4.0

https://github.com/sydney-client/Mint

https://github.com/jntdevelopment/vegaline

https://github.com/ScRichard/Gothaj-Next-Gen

https://github.com/ScRichard/GOTHAJ_RECODE_UNRELEASED

https://github.com/Nooniboi/Public-Ikea

https://github.com/Dovyrn/JoblessClientSrc

https://github.com/NoboKik/Achilles

https://github.com/IMXNOOBX/FuFuClient

https://github.com/Skitttyy/Sn0w

https://github.com/LvStrnggg/argon

https://github.com/wrrulosdev/mcpclient

https://github.com/LuminaDevelopment/LuminaClient

https://github.com/SpoonClient/Spoon

https://github.com/levin1337/exosware-1.21.4

https://github.com/Cypphi/kana-client

https://github.com/lgsquen/PubDLC

https://github.com/Gri11edHam/HamHacks

https://github.com/Lwescool/phantom

https://github.com/deadshxll/fusion

https://github.com/qzqc/AsteraClient

https://github.com/randomguy3725/MoonLight

https://github.com/DGVPSH/SlackOpen

https://github.com/Skitttyy/shoreline-client

https://github.com/Snowiiii/MasterMind

https://github.com/IUDevman/gamesense-client

https://github.com/sydney-client/Sydney-Legacy

https://github.com/AresClient/ares

https://github.com/KamiSkidder/shgr-client

https://github.com/WalmartSolutions/Dimasik

https://github.com/shxp3/CrossSine

https://github.com/smokedope2k16/polloshook-buildable

https://github.com/OrangetteTeam/Orangette

https://github.com/WalmartSolutions/Litka

https://github.com/bluteest/trollgod.cc-deobf

https://github.com/Nitaki-dev/Skidd.ed-Client

https://github.com/MrsRina/onepop

https://github.com/Atani-Client/client

https://github.com/0mlml/korppu

https://github.com/kawaiizenbo/MoonlightMeadows

https://github.com/Lyzev/Schizoid

https://github.com/Librry1337/Blessed-Client-1.16.5

https://github.com/0xTas/oasis

https://github.com/epsilonteam/Cursa

https://github.com/Librry1337/Troxill-1.16.5-src

https://github.com/VVriter/VydraHack

https://github.com/HeliosDevelopement/HeliosClient

https://github.com/WalmartSolutions/Virgins-4.4

https://github.com/WalmartSolutions/Pulse-1.0.3

https://github.com/0xFruzz/Extazyy-1.20.1

https://github.com/pisun6666/shoreline-main

https://github.com/pixelcmtd/CXClient

https://github.com/WalmartSolutions/Virgins-1.5.0












































