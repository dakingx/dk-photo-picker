# 简介
一个轻量级的照片选取组件，使用系统相册App来获取照片，或调用摄像头来抓拍照片。

本组件全部采用Kotlin进行书写，并使用Kotlin Coroutine将选取照片的异步操作同步化。

本组件兼容Android 11的Scoped Storage。

[![](https://jitpack.io/v/dakingx/dk-tiny-photo-picker.svg)](https://jitpack.io/#dakingx/dk-tiny-photo-picker)

# 工程项目配置
## 添加JitPack仓库
在工程项目根目录下的`build.gradle`中添加以下内容：

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

## 添加依赖库

```groovy
dependencies {
	implementation 'com.github.dakingx:dk-tiny-photo-picker:<请查看已发布版本>'
}
```

## 添加FileProvider
本组件需要配置相应的FileProvider。

```xml
<manifest>
    <application>
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.FILE_PROVIDER"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

添加`res/xml/file_paths.xml`文件，其内容如下。

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <files-path
        name="file"
        path="." />
    <cache-path
        name="cache"
        path="." />
    <external-files-path
        name="external_file"
        path="." />
    <external-cache-path
        name="external_cache"
        path="." />
</paths>
```

# 抓拍
## 申请应用权限
`PhotoFragment.REQUIRED_PERMISSIONS_FOR_CAPTURE`定义了抓拍需要的应用权限，请自行利用官方API或第三方权限库申请应用权限。

## 发起抓拍并处理结果
创建Kotlin Coroutine Scope。

```kotlin
private val coroutineScope: CoroutineScope = MainScope()
```

在Kotlin Coroutine中发起抓拍并处理结果。

```kotlin
import com.dakingx.photopicker.util.capturePhoto
import com.dakingx.photopicker.ext.toBitmap
```

```kotlin
coroutineScope.launch {
    when (val result =
        capturePhoto(supportFragmentManager, "FileProvider的authorities值")) {
        is PhotoOpResult.Success -> {
            // 将Uri转为Bitmap
            val bitmap = result.uri.toBitmap(context)
        }
        PhotoOpResult.Failure -> toast("操作失败")
        PhotoOpResult.Cancel -> toast("操作取消")
    }
}
```

销毁Kotlin Coroutine Scope。

```kotlin
coroutineScope.cancel()
```

# 选取照片
## 申请应用权限
`PhotoFragment.REQUIRED_PERMISSIONS_FOR_PICK`定义了选取照片需要的应用权限，请自行利用官方API或第三方权限库申请应用权限。

## 选取相册照片并处理结果
创建Kotlin Coroutine Scope。

```kotlin
private val coroutineScope: CoroutineScope = MainScope()
```

在Kotlin Coroutine中选取相册照片并处理结果。

```kotlin
import com.dakingx.photopicker.util.pickPhoto
import com.dakingx.photopicker.ext.toBitmap
```

```kotlin
coroutineScope.launch {
    when (val result =
        pickPhoto(supportFragmentManager, "FileProvider的authorities值")) {
        is PhotoOpResult.Success -> {
            // 将Uri转为Bitmap
            val bitmap = result.uri.toBitmap(context)
        }
        PhotoOpResult.Failure -> toast("操作失败")
        PhotoOpResult.Cancel -> toast("操作取消")
    }
}
```

销毁Kotlin Coroutine Scope。

```kotlin
coroutineScope.cancel()
```

