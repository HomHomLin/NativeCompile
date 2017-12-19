### Android Gradle Native Compile Plugin

依赖插件

```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        //未正式发布
        classpath 'io.github.lizhangqu:native-compile-plugin:1.0.0'
    }
}

apply plugin: 'native-compile-plugin'
```

依赖动态库

```
dependencies {
    nativeCompile "$groupId:$name:$version:$classifier@so"
}
```

其中classifier可选，其值为 armeabi, armeabi-v7a, arm64-v8a, x86, x86_64, mips, mips64其中一个，不是这些值会抛异常

如

```
dependencies {
    nativeCompile 'com.snappydb:snappydb-native:0.2.0:armeabi'
    nativeCompile 'com.snappydb:snappydb-native:0.2.0:x86@so'
    nativeCompile 'com.snappydb:snappydb-native:0.2.0:mips@so'
    nativeCompile "com.snappydb:snappydb-native:0.2.0:armeabi-v7a@so"
}
```

如果依赖了非.so的远程库，则构建过程不会发生异常，但是该文件会被忽略，如

```
dependencies {
    nativeCompile 'com.android.support:appcompat-v7:26.1.0'
}
```

在评估配置阶段，nativeCompile依赖会将对应的so拷贝到对一个的jniLibs下，命名方式为libname.so，如果对应的文件是以lib开头，则不添加lib前缀


当前不支持将动态库拷贝到buildType或者flavor下的目录，需求强烈后续考虑添加