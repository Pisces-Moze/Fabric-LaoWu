# Fabric-LaoWu / 猫猫应急

本仓库包含两个版本：

- `src/`：适用于 Minecraft 1.21.11 的 Fabric 模组。
- `bukkit-plugin/`：适用于 Bukkit、Spigot 与 Paper 1.18.2–1.21.1 的服务端插件，并内置资源包生成及下发服务。

## 功能

- 已加载的符合条件猫数量为偶数时，猫会按距离两两配对。
- 配对猫吸附到面对面位置，弓背、炸毛、歪头、活动下颌并播放从参考视频提取的音效。
- 威慑结束后进入无伤害的翻滚扭打动画。
- 任意材质铲子右键猫，可按玩家当前视角将猫投影为水平贴地的“纸片猫”。
- 纸片猫仍能移动；右键三次恢复立体形态。
- 猫会寻找附近铁轨，与矿车接触后按随机视角纸片化。
- 纸片姿态、取景角度及恢复点击次数均会同步并保存。

## 环境

- Minecraft `1.21.11`
- Fabric Loader `0.19.3` 或更高版本
- Fabric API `0.141.4+1.21.11`
- Java 21

客户端和服务端均需安装。单人游戏只需放入本机 `mods` 文件夹。

## 构建

Windows：

```powershell
.\gradlew.bat build
```

其他系统：

```bash
./gradlew build
```

构建产物位于 `build/libs/`。

### Bukkit/Paper 插件

```powershell
.\gradlew.bat -p bukkit-plugin clean build
```

插件位于 `bukkit-plugin/build/libs/`，独立资源包位于 `bukkit-plugin/build/distributions/`。插件运行时会根据服务器版本重新生成相应 `pack_format` 的资源包；部署方法与公网资源包地址配置见 [`bukkit-plugin/README.md`](bukkit-plugin/README.md)。

## 许可证

[GNU General Public License v2.0](LICENSE)
