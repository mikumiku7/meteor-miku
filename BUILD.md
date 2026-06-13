# 构建说明

这个项目支持为多个Minecraft版本构建jar包。

## 支持的版本

- **Minecraft 1.21.1**
  - Yarn mappings: 1.21.1+build.3
  - Meteor Client: 0.5.8-SNAPSHOT
  - Fabric Loader: 0.16.5

- **Minecraft 1.21.4**
  - Yarn mappings: 1.21.4+build.1
  - Meteor Client: 1.21.4-SNAPSHOT
  - Fabric Loader: 0.16.5

## 构建方法

### 方法1: 使用批处理脚本（推荐）

**Windows:**
```bash
./build-all.bat
```

**Linux/macOS:**
```bash
./build-all.sh
```

这将自动构建两个版本的jar包。

### 方法2: 手动构建单个版本

**构建1.21.1版本:**
```bash
./gradlew clean build -x test --no-configuration-cache -Pminecraft_version="1.21.1"
```

**构建1.21.4版本:**
```bash
./gradlew build -x test --no-configuration-cache -Pminecraft_version="1.21.4"
```

### 方法3: 使用Gradle任务

```bash
# 构建1.21.1版本
./gradlew buildFor1211

# 构建1.21.4版本
./gradlew buildFor1214

# 构建所有版本
./gradlew buildAll
```

## 输出文件

构建完成后，jar文件将位于 `build/libs/` 目录中：

- `meteor-miku-1.21.1-1.2.0.jar` - 1.21.1版本
- `meteor-miku-1.21.4-1.2.0.jar` - 1.21.4版本

## 注意事项

1. 使用 `--no-configuration-cache` 参数来避免配置缓存问题
2. 第一次构建1.21.1版本时使用 `clean`，后续版本可以不用clean来节省时间
3. 构建脚本会自动根据版本选择正确的依赖项和映射
