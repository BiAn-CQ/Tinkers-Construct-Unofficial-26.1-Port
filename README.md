# Tinkers' Construct Unofficial 26.1 Port

Tinkers' Construct 的 Minecraft 26.1 / NeoForge 非官方移植版，依赖
[Mantle Unofficial 26.1 Port](https://github.com/BiAn-CQ/Mantle-Unofficial-26.1-Port)。

## 运行环境

- Minecraft `26.1.2`
- NeoForge `26.1.2.95` 或更高的 26.1.2 兼容版本
- Java `25`
- Mantle `1.12.0` 或更高版本
- JEI `29.21.0` 或更高版本（可选）

## 构建与测试

Windows：

```powershell
.\gradlew.bat clean test build
.\gradlew.bat runGameTestServer
```

Linux/macOS：

```bash
./gradlew clean test build
./gradlew runGameTestServer
```

## 文档

有关附属模组开发和数据包的文档，请参阅
[SlimeKnights 文档站](https://slimeknights.github.io/docs/)。

## 上游与问题反馈

本项目基于 [SlimeKnights/TinkersConstruct](https://github.com/SlimeKnights/TinkersConstruct)
移植，不是 SlimeKnights 官方发布。请不要将本移植版的问题提交到上游仓库。

反馈问题时请附上 Minecraft、NeoForge、Mantle 和本模组的版本，以及复现步骤、
相关日志和截图。

## 许可证

Tinkers' Construct 及本移植版修改继续使用 [MIT License](LICENSE)。原项目版权归
SlimeKnights 及其贡献者所有。
