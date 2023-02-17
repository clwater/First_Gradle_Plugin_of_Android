---
title: 开始使用Android Gradle Plugin来帮助你进行项目的构建
date: 2023-02-10 14:30:46
tags: ["android" , "Gradle" , "Android" , "gradle"]
categories : "Gradle"
cover : https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302101904126.png
top_img : https://source.unsplash.com/1600x900/?nature/
---



![top.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302101625015.png)


> 一切的一切到要从`阿里云emas移动推送`说起, 这玩意的接入过程暂且不表, 有兴趣的可以看一看, 尤其是其控制台.
> 接入后主要需要在`build.gradle`中进行配置, 同时还有一份配置文件在`app`目录下.
> 但是emas的配置文件只能配置一份, (甚至不能实时更新, 需要`gradle clean`), 而实际的开发使用中可能包含开发,测试和上线环境的配置. 
> 这些配置就需要我们在构架的时候自动匹配为最近的配置文件.


# 0.在开始之前

其实最开始的实现方式并不是这篇文章使用的`Android Gradle Plugin`的方法来解决,  遇到这个情况的时候, 第一个想到的方法是在`build.gradle`中修改task来实现, 但是`阿里云emas移动推送`是通过Gradle Plugin的形式引入的, 如果task来实现的话会在`emas plugin`执行后再执行我们的task,  这样就导致了对应的配置文件可能没有更新成功问题的发生. 

第二个的想到的方法是通过shell在`gradle build`执行前处理对应的文件. 不过由于是团队合作的项目, 更改项目的构架方法可能会影响到其他人, 秉承着对他人影响最小的原则, 这个方法就不合适了.

最后就是现在的实现方法, 根据`Gradle Plugin`的相关特性, 我们通过自定义我们自己的`Android Gradle Plugin`来帮我们构架我们的项目.

# 1.开始使用Android Gradle Plugin来帮助你构建的项目 

### 什么是Android Gradle Plugin插件?

![1676127877003.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302112304030.png)

当我们创建一个新的项目的时候, 在默认的配置文件中我们就能看到以下使用的插件信息. 当然不仅仅官方可以发布插件, 我们也可以创建和发布我们自己的插件.
用官方的解释来说就是[^1]:"Android Gradle 插件 (AGP) 是官方的 Android 应用构建系统。该系统支持编译多种不同类型的源代码，以及将其链接到可在实体 Android 设备或模拟器上运行的应用中。"
简单概括来说, Android Gradle Plugin就是可以在Android构架的时候将那些可重复使用的构建逻辑抽出来, 作为一个独立的项目/插件, 应用在不同的项目构建中. 例如多渠道打包, 修改图片, 压缩图片等相关的操作都可以作为一个插件来应用到不同的项目中.

### Gradle和Android Gradle Plugin的区别?

Gradle和Android Gradle Plugin是两个不同的方向和功能, Gradle是用来进行项目的构建, 而Android Gradle Plugin是一个构建的动作, 和我们项目中的.gradle文件功能相同.

# 2.为什么要使用Android Gradle Plugin?

可能在初次接触到类似问题的时候都会有这样的问题, 这玩应到底有啥用?  

如果你打包过.apk, 那么你就是`Android Gradle Plugin`的头号潜在用户.

我们可以构建我们自己的`Android Gradle Plugin`来帮助我们实现以下的功能

* 资源的预处理: 压缩图片,  修改资源内容
* 配置文件的处理:  根据不同flavor更新不同的配置文件内容
* 自定义规则添加: 针对项目中自定的规则进行检查
* AOP: 切面添加相关代码

当然, `Android Gradle Plugin`能做的不仅仅是这些, 我们还可以根据我们自己的修改创建我们自己的`Android Gradle Plugin`


# 3. 创建我们自己的Android Gradle Plugin

`Android Gradle Plugin`有三种创建的方法, 

## 3.1 build.gradle
第一种就是直接在app module的build.gradle文件中添加以下代码
``` groovy
// 编写plugin类
class DemoPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        println "==============="
        println "DemoPlugin"
        println "==============="
    }
}

// 使用自定义Plugin
apply plugin: DemoPlugin

```

我们Sync后就可以在Build窗口中看到以下的数据了.

![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302151102612.png)


这种使用的方法最大的有点就是简单简洁,  最大的缺点是太过简单简洁,  最适合的是小功能,  并且没有可移植性, 只能跟着build.gradle文件走.


[GitHub文件地址:https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_1/app/build.gradle](https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_1/app/build.gradle)


## 3.2 buildSrc

第二章方法是在项目中创建新的module, 并且命名为`buildSrc`.

构建后的文件目录结构如下, 具体的详情说明会在后面进行详细的说明.
![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302151151862.png)

当然, 在使用的使用仍需要在`build.gradle`中添加以下内容
``` groovy
apply plugin: 'com.clwater.plugin' // 这个名字和我们.properties文件的名字一致
```

此时我们再sync项目, 可以看到如下输出
![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302151154673.png)


这种方法具有一定的复用性, 在项目中的每个module可以使用, 但是对外仍不可使用.

> Tips:  module只能命名为`buildSrc`, 并且在`settings.gradle`将自动添加的`include ':buildSrc'`移除


[GitHub文件地址:https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_2/app/build.gradle](https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_2/app/build.gradle)

## 3.3 Android Gradle Plugin Project 

顾名思义, 我们可以在独立的Project中创建我们的`Android Gradle Plugin`, 

当然创建的方法也不止一种, 可以酌情根据时间情况来选择如果进行创建, ~~以下推荐与否, 全凭主观臆断.~~

###  3.3.1 手动创建

> ~~纯主观臆断不推荐~~

因为自己创建的时候需要手动配置很多需要关联的内容, 但是这些关联的内容对刚刚接触此部分的开发者来说, 又十分的零碎, 往往最后不得其法, 导致由于各种或理解或遗漏的小问题造成的运行结果不达预期.

### 3.3.2 通过Gradle创建

既然是`Android Gradle Plugin`, 那得先是`Gradle`中能用的, 才能扩展到`Android Gradle`中,  Gradle也提供了一个自动创建的方法, 来避免我们手动创建时引发的各种问题. 也方便相关流程的规范化.


#### 创建Android Gradle Plugin Project所在的文件夹
这里我们之间创建一个文件夹, 用作我们Android Gradle Plugin对应Project的所在目录



####  使用gradle init构建项目

> 此章节使用的gradle指令时, 如无法找到相关指令, 检查gradle是否安装配置, 详情参考[Gradle | Installation: https://gradle.org/install/]([https://gradle.org/install/)

我们在终端中打开刚刚创建的目录,  并进行以下操作

``` groovy
gradle init

Select type of project to generate:
  1: basic
  2: application
  3: library
  4: Gradle plugin
Enter selection (default: basic) [1..4] 4
# 选择生成的Gradle项目类型, 我们需要选择4: Gradle plugin

Select implementation language:
  1: Groovy
  2: Java
  3: Kotlin
Enter selection (default: Java) [1..3] 1
# 选择插件的编码使用语言, 这里推荐使用Groovy(相关教程及内容最多)

Select build script DSL:
  1: Groovy
  2: Kotlin
Enter selection (default: Groovy) [1..2] 1
# 选择项目的编译脚本语言(类似build.gradle文件), 推荐使用Groovy, 理由同上

Generate build using new APIs and behavior (some features may change in the next minor release)? (default: no) [yes, no]                                                                                                                       no
# 是否启用一些新的实验性的APIs来进行构建, 如果没有特殊需求的话不启用即可

Project name (default: First_Gradle_Plugin_of_Android_Test_3): plugin
# Project的名称, 默认为你对应文件的名称, 实际上是你对应module的名称

Source package (default: plugin): com.clwater.plugin
# 包名的完整地址, 更改为你的地址即可

> Task :init
Get more help with your project: https://docs.gradle.org/8.0/userguide/custom_plugins.html

BUILD SUCCESSFUL in 1m 19s
2 actionable tasks: 2 executed

```

最终的构建结果如下:

![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302170838207.png)




#### [编写我们自己的Plugin]

打开默认生成的`.groovy`文件,  我就就此文件进行简单的修改, 方便后面的步骤进行
```groovy

// 更改下默认生成的文件名
class ClwaterPlugin implements Plugin<Project> {
    void apply(Project project) {
	    // 输出一下插件被调用
        println("Hello from plugin")
        // 注册一个名叫greeting的task
        project.tasks.register("greeting") {
            doLast {
                println("Hello from plugin 'com.clwater.plugin.greeting'")
            }
        }
    }
}
```


> 关于Gradle Task的相关内容, 这里不再额外赘述, 如果大家感喜欢我的文章风格的话, 我可以再帮大家整理一下.


#### 发布插件

当我们在Project中创建好并完成了我们自己的`Android Gradle Plugin`后, 下一步就是将它提供给第三方, 

##### 配置build.gradle

接下来就是在`build.gradle`中添加额外的发布配置, 构建出来的插件如不配置指定的`repositories`会在系统默认的mavenLocal中生成, 这里为了更直观便捷的使用, 我们将其生成在项目内.

``` groovy
...
plugins {
	...
    id 'maven-publish'
}

...
publishing {
    publications {
        // 此处clwater可以随便写，但是后面的MavenPublication不能随便写
        clwater(MavenPublication) {
            // 插件的组ID
            groupId = 'com.clwater.plugin'
            // 插件的ID
            artifactId = 'Plugin'
            // 插件的版本
            version = '1.0.0'
            // 插件的发布类型
            from components.java
        }
    }

    repositories {
        maven {
            // 插件的发布仓库
            name = 'repo'
            // 插件的发布仓库地址
            url = "../repo"
        }
    }
}

...

```

此时我们再使用`gradle tasks`就可以看到我们的发布task了.
![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302170938325.png)


##### 配置插件调用信息

我们可以看到在`main`文件夹下有`groovy`和`resources`两个文件夹,  `groovy`包含了我们的插件代码, 而`resources`文件夹则需要包含我们的插件声明文件.

为了使得我们的插件可以被调用, 我们需要配置相关的声明. 主要是在`resources`目录下添加`META-INF`, 并在`META-INF`中添加`gradle-plugins`, 在`gradle-plugins`添加`com.clwater.plugin.properties`, 当然这个文件的名字我们可以随便定义. 没有约束性要求. 最后的文件目录结果如下:

```shell
main
├── groovy
│   └── com
│       └── clwater
│           └── plugin
│               └── ClwaterPlugin.groovy
└── resources
    └── META-INF
        └── gradle-plugins
            └── com.clwater.plugin.properties
```

关于`com.clwater.plugin.properties`, 我们需要配置我们插件的入口, 形式如下:
``` properties
implementation-class=com.clwater.plugin.ClwaterPlugin
```


#####  生成到本地
由于我的`build.gradle`文件中配置的名称为`clwater`, 所以我的最终的文件生成task应该使用 `publishClwaterPublicationToRepoRepository`,  当我们自己开发的时候, 酌情根据自己的情况进行修改.

当然, 你也可以在Android Studio的Gradle窗口中找到相关的构建Task
![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302171451674.png)



当我们的发布task完成后, 就可以发现项目中多了一个repo的目录, 同时我们的插件也发布到了此处.
![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302171454188.png)


[GitHub文件地址:https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_3](https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_3)

#### 项目引入和使用

当我们的项目构建完成后, 就可以提供给别的项目进行使用了, 我们先构建一个新的项目,

> 由于使用的gradle版本为gradle-7.5,  项目中gradle相关文件可能有所不用,  酌情根据项目实际情况来进行配置.

##### 引入
首先是将我们生成好的`Android Gradle Plugin`所在的`repo`文件夹复制到项目中, 然后依次修改以下文件

###### settings.gradle
``` shell
 pluginManagement {
     repositories {
+        gradlePluginPortal()
         google()
         mavenCentral()
-        gradlePluginPortal()
+        //增加本地仓库
+        maven {
+            allowInsecureProtocol(true)
+            url uri('./repo')
+        }
     }
 }
 dependencyResolutionManagement {
@@ -10,6 +15,11 @@ dependencyResolutionManagement {
     repositories {
         google()
         mavenCentral()
+        //增加本地仓库
+        maven {
+            allowInsecureProtocol(true)
+            url uri('./repo')
+        }
     }
 }
rootProject.name = "First_Gradle_Plugin_of_Android_Test_4"
include ':app'
```

简单来说就是向两个`repositories`中添加我们使用的本地仓库


###### project下的build.gradle
``` shell
+ buildscript {
+     dependencies {
+         classpath('com.clwater.plugin:Plugin:1.0.0')
+     }
+ }
+ 
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '7.4.0' apply false
    id 'com.android.library' version '7.4.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.7.21' apply false
}

```

添加我们的插件(groupId:Id:version的格式)

###### app module下的build.gradle
``` shell
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
+   id 'com.clwater.plugin'
}

android {
    namespace 'com.clwater.first_gradle_plugin_of_android_test_4'
    compileSdk 33
    ...
}
```

> Tips: id 'com.clwater.plugin'中的内容和我们配置`resources`文件夹下的`.properties`需要完全一致

###### 早期版本构建项目配置文件参考

早期的项目修改起来比较容易, `app`下的`build.gradle`配置完全一致, 我们要修改的只有项目根目录下的`build.gradle`
``` shell

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
+       maven {
+           url uri('./repo')
+       }
        mavenCentral()
        google()
        jcenter()

    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.2.1'
        classpath 'org.owasp:dependency-check-gradle:6.0.5' // ★追加
+       classpath 'com.clwater.plugin:Plugin:1.0.0'
    }
}

apply plugin: 'org.owasp.dependencycheck' // ★追加

allprojects {
    repositories {
+       maven {
+           url uri('./repo')
+       }
        mavenCentral()
        google()
        jcenter()
    }
}


```




##### 使用
其实当然引入之后就已经再使用了, 当我们重新构建项目的时候就可以在`build`视图中看到如下内容
![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302171534393.png)

我们可以看到在`ClwaterPlugin`中的输出语句被调用了. 说明我们的插件已经被执行了.

同时我们执行`./gradlew tasks --all`, 就可以找到我们定义的`greeting`task, 
![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302171538632.png)

当然, 直接执行的时候也是可以的.

![image.png](https://clwater-obsidian.oss-cn-beijing.aliyuncs.com/img/202302171540072.png)


> Tips:  截止此处, 我们已经可以通过任意方式来创建一个*Android Gradle Plugin*并将其在任意的项目中使用了,  但是其实还有一个疑问,  *Android Gradle Plugin* 可以帮助我们做什么?


[GitHub文件地址:https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_4](https://github.com/clwater/First_Gradle_Plugin_of_Android/blob/master/First_Gradle_Plugin_of_Android_Test_4e)


# 4.通过Android Gradle Plugin来帮助我们修改配置文件(以修改阿里云EMAS推送为例)

前文提到过"如果你打包过.apk, 那么你就是`Android Gradle Plugin`的头号潜在用户.",  因为`Android Gradle Plugin`可以帮助我们做许多不得不做, 有具有重复性的事情.比如此章的情况: *通过Android Gradle Plugin来帮助我们修改配置文件(以修改阿里云EMAS推送为例)*

> 阿里云EMAS是一个第三方的推送插件, 可以进行数据的推送, 接入过程不过去描述, 我们使用`Android Gradle Plugin`来解决的问题是: EMAS的配置文件唯一, 无法配置多个文件, 如果不自动化配置的话, 那么上线和测试开发时使用的配置文件要不只能使用一个, 要不需要打包前手动修改, 而只有一套配置文件来开发上线显然是不应该的, 同时如果多套的话, 手动来进行打包前的修改也是不可靠的. 最终还是需要进行自动化配置.

参考代码如下:
``` groovy
package com.panasonic.plugin

import org.gradle.TaskExecutionRequest
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

class ClwaterPlugin implements Plugin<Project>{
    // 不同的渠道
    enum BuildType {
        PRODUCT,
        DEV,
        TEST,
        NONE
        ;
    }

    // project持有
    private Project project


    // 配置文件路径
    // ali push 配置文件路径(preBuild使用)
    private String emasFromFile
    // ali push 配置文件路径(/app实际构建)
    private String emasToFile

    private buildType = BuildType.NONE;

    /**
     * 插件入口
     * step1: 检查xxx(配置文件路径)文件夹是否存在
     * step2: 检查是否构建为生产环境
     * step3: 拷贝xxx内容文件夹到app文件夹
     *  step3.1: 拷贝ali push配置文件
     * step4: 构建完成后删除相关文件
     * @param project
     */
    @Override
    void apply(Project project) {
        println('==================================================')
        println('ClwaterPlugin is applying')

        this.project = project
        buildType = getBuildType(project)
        println("ClwaterPlugin: buildType: $buildType")

        deleteEAMSCache()

        initBaseConfig()

        if (checkPreBuildFile()){
            throw new GradleException("File not found: $emasFromFile")
        }

        movePreBuildFile()

        project.gradle.buildFinished {
            println('==================================================')
            println('ClwaterPlugin: finished')
            println('ClwaterPlugin: delete cache file')
            deleteCache()
            println('==================================================')
        }
    }

    /**
     * 删除阿里推送emas缓存文件, 避免新的配置文件无法生效
     */
    private void deleteEAMSCache(){
        Gradle gradle = project.gradle
        println(" del:  $project.rootDir/app/build/generated/res/emas-services")
        project.delete("$project.rootDir/app/build/generated/res/emas-services")

        List<TaskExecutionRequest> taskExecutionRequests = gradle.getStartParameter().getTaskRequests()
        for (TaskExecutionRequest taskExecutionRequest: taskExecutionRequests) {

            if (taskExecutionRequest.args.toString().contains("assemble")){
                String variant = taskExecutionRequest.args.toString()
                variant = variant.replace("[", "")
                variant = variant.replace("]", "")
                variant = variant.replace(":app:", "")
                variant = variant.replace("assemble", "")
                variant = "merge" + variant + "Resources"
                // 提取构建variant
                println(" del:  $project.rootDir/app/build/intermediates/incremental/$variant/merger.xml")
                project.delete("$project.rootDir/app/build/intermediates/incremental/$variant/merger.xml")

            }
        }
    }

    /**
     * 初始化基础配置
     */
    private void initBaseConfig(){
        switch (buildType){
            case BuildType.PRODUCT:
                emasFromFile = "xxx/product/aliyun-emas-services.json"
                break
            case BuildType.DEV:
                emasFromFile = "xxx/dev/aliyun-emas-services.json"
                break
            case BuildType.TEST:
                emasFromFile = "xxx/test/aliyun-emas-services.json"
                break
        }
        emasToFile = "$project.rootDir/app"
    }

    /**
     * 拷贝preBuild对应文件到app文件夹
     */
    private void movePreBuildFile(){
        moveEmas()
    }

    /**
     * 拷贝文件
     * @param fromPath
     * @param toPath
     */
    private void copyFile(String fromPath, String toPath){
        project.copy {
            from fromPath
            into toPath
        }
    }

    /**
     * 复制配置文件
     */
    private void moveEmas(){
        copyFile(emasFromFile, emasToFile)
    }

    /**
     * 检查是否构建为生产环境
     * @param project
     * @return
     */
    private static BuildType getBuildType(Project project){
        Gradle gradle = project.gradle

        List<TaskExecutionRequest> taskExecutionRequests = gradle.getStartParameter().getTaskRequests()
        for (TaskExecutionRequest taskExecutionRequest: taskExecutionRequests){
            if(taskExecutionRequest.args.toString().contains("assemble")){
                // 检查是否为Dev/Test/Product
            }
        }
        return BuildType.DEV
    }

    /**
     * 检查预构建文件是否存在
     * @return
     */
    private boolean checkPreBuildFile(){
        // 检查emas配置文件是否存在
        File emasFile = new File(emasFromFile)
        return !emasFile.isFile()
    }

    /**
     * 删除相关文件
     */
    private void deleteCache(){
        project.delete(emasToFile+ "/aliyun-emas-services.json")
    }
}
```

至此, 我们就完成了针对阿里EMAS的多版本配置文件的自动化配置, 再也不用担心人工配置可能引起的各种问题了.


# -1. 最后

相信至此大家已经对`Android Gradle Plugin`有了一定的了解, 也希望此文可以帮助到大家,  更欢迎大家一起交流.


# -2 最后的最后

项目完整代码可以访问:[我的GitHub: https://github.com/clwater/First_Gradle_Plugin_of_Android](https://github.com/clwater/First_Gradle_Plugin_of_Android)

[^1]:https://developer.android.com/studio/build/extend-agp?hl=zh-cn