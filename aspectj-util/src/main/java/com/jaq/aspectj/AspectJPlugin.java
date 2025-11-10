package com.jaq.aspectj;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.component.SoftwareComponentContainer;

import javax.annotation.Nullable;

public class AspectJPlugin implements Plugin<Project> {
    @Override
    public void apply(@Nullable Project project) {
        if (project == null) return;
        // 插件逻辑入口
        AspectJUtils.setupAspectJ(project);

//        // 示例：动态扩展属性
//        project.extensions.create("myPluginConfig", MyPluginExtension::class.java)

        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(@Nullable Project p) {
                if (p == null) return;
                // 检查是否应用了Android插件
                if (!p.getPlugins().hasPlugin("com.android.application") &&
                        !p.getPlugins().hasPlugin("com.android.library")) {
                    project.getLogger().warn(": AspectJ Plugin 被应用，但未检测到 'com.android.application' 或 'com.android.library' 插件。插件可能不会生效。");
                    return;
                }

                try {
                    // 获取 androidComponents 扩展
                    Object androidComponents = p.getExtensions().findByName("androidComponents");
                    if (androidComponents == null) {
                        project.getLogger().warn(": 无法找到 'androidComponents' 扩展。请确保应用了合适的 Android Gradle Plugin (AGP) 版本。");
                        return;
                    }

                    // 调用 beforeVariants 回调
                    invokeBeforeVariantsCallback(androidComponents, p);

                } catch (Exception e) {
                    project.getLogger().error(": 在配置 AspectJ Plugin 时发生错误", e);
                }
            }
        });
    }

    private void invokeBeforeVariantsCallback(Object androidComponents, Project project) {
        try {
            project.getLogger().lifecycle(":data 配置变体 - 模拟 beforeVariants 回调");
            // 模拟在beforeVariants中为preBuild任务添加依赖
            Task preBuildTask = project.getTasks().findByName("preBuild");
            Task configTask = project.getTasks().findByName(AspectJUtils.CONFIG_TASK); // 假设您的配置任务叫这个
            if (preBuildTask != null && configTask != null) {
                preBuildTask.dependsOn(configTask);
            }

            // 模拟将编织任务依赖关联到编译任务
            project.getTasks().withType(org.gradle.api.tasks.compile.JavaCompile.class).configureEach(javaCompileTask -> {
                Task weavingTask = project.getTasks().findByName(AspectJUtils.WEAVING_TASK); // 假设您的编织任务叫这个
                if (weavingTask != null) {
                    javaCompileTask.dependsOn(weavingTask);
                    project.getLogger().lifecycle(": 为任务 '" + javaCompileTask.getName() + "' 添加对AspectJ编织任务的依赖");
                }
            });

        } catch (Exception e) {
            project.getLogger().warn(": 调用 beforeVariants 回调失败。", e);
        }
    }
}
