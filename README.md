# Fabric-LaoWu / 老吴模组

适用于 Minecraft 1.21.11 的 Fabric 模组。

## 功能

- 猫数量为偶数时，猫会按距离两两配对。
- 素材借鉴https://www.bilibili.com/video/BV1q93i6wEDh 
- 任意材质铲子右键猫，可将基米二维化，空手右键三次恢复
- 基米会寻找附近铁轨，与矿车接触后也会二维化。

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

## 许可证

[GNU General Public License v2.0](LICENSE)
