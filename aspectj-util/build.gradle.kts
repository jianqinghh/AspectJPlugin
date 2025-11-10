plugins {
    `java-gradle-plugin` // 核心插件，帮助开发和打包Gradle插件
    `maven-publish`      // 应用Maven发布插件
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {

    implementation(gradleApi())
    compileOnly(libs.gradle)
    implementation(libs.aspectjtools)
}


// 定义Gradle插件，为了让你的插件能够被 Gradle 的 plugins {}DSL（这种现代、简洁的声明方式）正确识别和加载
gradlePlugin {
    plugins {
        // 使用 `create` 方法并指定一个内部标识符（可自定义）
        create("androidAspectJPlugin") {
            // 用户应用插件时使用的ID
            id = "io.github.jianqinghh.android-aspectj"
            // 插件实现类的全限定名
            implementationClass = "com.jaq.aspectj.AspectJPlugin"
        }
    }
}


group = "io.github.jianqinghh"
version = "1.0.2"


val githubPublishName = project.findProperty("github.publish.name")
val githubPublishToken = project.findProperty("github.publish.token")
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // 指定使用release变体组件
            from(components["java"])
            println("当前分组 " + project.group)
            println("当前版本 " + project.version)
            // java-gradle-plugin插件会自动生成 artifactId（格式：${pluginId}.gradle.plugin）
            groupId = project.group.toString()
            version = project.version.toString()
        }
    }
    //指定要上传的目标仓库，并提供认证信息，
    //发布到 JitPack 时，此配置不是必须的，因为 JitPack 会克隆您的代码仓库并在其服务器上执行构建
    repositories {
        mavenLocal() // 发布到本地Maven仓库（通常是 ~/.m2/repository）

//        maven {
//            // 生成的插件位置
//            url = uri("../repo")
//        }
    }
}