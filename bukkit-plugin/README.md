# CatFight Bukkit

适用于 Bukkit、Spigot 与 Paper 1.18.2–1.21.1 的猫猫应急插件。Java 包名统一为 `cn.moze.catfight.bukkit`。

## 功能

- 偶数只符合条件的成年猫自动按最近距离配对并吸附到面对面位置。
- 8 秒弓背、炸毛、活动下颚威慑，随后进行 5 秒无伤害扭打。
- 两只猫以世界空间相反方向歪头对视。
- 任意铲子右键可将猫压成按玩家视角捕获的纸片猫；右键三次恢复。
- 猫会靠近普通、动力、探测和激活铁轨；被矿车碰撞后随机角度纸片化。
- 自带由原视频分离的哈气及扭打音频。
- 内置 HTTP 资源包服务，根据服务器版本生成正确的资源包格式。

## 安装

1. 将插件 JAR 放入服务器 `plugins` 文件夹。
2. 启动一次服务器，编辑 `plugins/CatFight/config.yml`。
3. 将 `resource-pack.public-url` 改成玩家可以访问的地址，例如 `http://play.example.com:8123/catfight-pack.zip`。
4. 确保配置的资源包端口可由玩家访问，然后重启或执行 `/catfight reload`。

资源包默认为必需。这样所有玩家都能看到模型，也不会发生没有资源包的客户端只看到隐形猫的情况。

## 构建

在本目录执行：

```powershell
..\gradlew.bat -p . clean build
```

成品位于 `build/libs/CatFight-Bukkit-2.0.0.jar`。
