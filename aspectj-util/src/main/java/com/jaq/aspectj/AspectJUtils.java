package com.jaq.aspectj;

import com.android.build.gradle.AppExtension;

import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.MessageHandler;
import org.aspectj.tools.ajc.Main;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * AspectJ 工具类 - 用于配置和管理 AspectJ 织入任务
 */
public class AspectJUtils {

    private static final String TAG = "AspectJUtils";
    /**
     * 配置依赖 aspectjrt 包
     */
    public static final String CONFIG_TASK = "aspectJConfigure";
    /**
     * 执行 切片代码织入任务
     */
    public static final String WEAVING_TASK = "aspectJWeaving";
    /**
     * 检查 aspectjrt 依赖是否正确配置，AspectJ 核心类是否可用
     */
    public static final String VERIFY_TASK = "aspectJSetupVerify";

    /**
     * 设置 AspectJ 织入任务
     */
    public static void setupAspectJ(Project project) {
        TaskContainer tasks = project.getTasks();

        // 注册主要的 AspectJ 任务
        tasks.register(CONFIG_TASK, task -> {
            task.doLast(t -> {
                System.out.println("✅ 开始配置 AspectJ 织入环境 for " + project.getName());
                configureAspectJForProject(project);
            });
        });

        // 注册织入执行任务
        tasks.register(WEAVING_TASK, task -> {
            task.doLast(t -> {
                System.out.println("🚀 执行 AspectJ 字节码织入 for " + project.getName());
                String javaVersion = System.getProperty("java.version");
                System.out.println("JDK Version 1: " + javaVersion);
                System.out.println("JDK Version 2: " + JavaVersion.current());
                executeAspectJWeaving(project);
            });
        });

        // 注册验证任务
        tasks.register(VERIFY_TASK, task -> {
            task.doLast(t -> {
                System.out.println("🔍 验证 AspectJ 配置 for " + project.getName());
                verifyAspectJConfiguration(project);
            });
        });

        System.out.println("✅ AspectJ 任务注册完成 for " + project.getName());
    }

    /**
     * 配置项目的 AspectJ 环境
     */
    private static void configureAspectJForProject(Project project) {
        System.out.println("✅ AspectJ 依赖配置开始");
        try {
            // 检查是否已应用 Android 插件
            if (!project.getPlugins().hasPlugin("com.android.application") &&
                    !project.getPlugins().hasPlugin("com.android.library")) {
                System.out.println("⚠️ 项目未应用 Android 插件，跳过 AspectJ 配置");
                return;
            }

            // 配置依赖
            project.getDependencies().add("implementation", "org.aspectj:aspectjrt:1.9.22");

            System.out.println("✅ AspectJ 依赖配置完成");

        } catch (Exception e) {
            System.err.println("❌ AspectJ 配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行 AspectJ 织入
     */
    private static void executeAspectJWeaving(Project project) {
        try {
            // 获取 Java 编译任务
            for (JavaCompile javaCompile : project.getTasks().withType(JavaCompile.class)) {
                configureJavaCompileTask(project, javaCompile);
            }

            System.out.println("✅ AspectJ 织入配置完成");

        } catch (Exception e) {
            System.err.println("❌ AspectJ 织入执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 配置 Java 编译任务的 AspectJ 织入
     */
    private static void configureJavaCompileTask(Project project, JavaCompile javaCompile) {
        javaCompile.doLast(task -> {
            try {
                System.out.println("🔧 配置 AspectJ 织入 for: " + javaCompile.getName());

                String[] args = buildAspectJArgs(project, javaCompile);
                System.out.println("AspectJ 参数: " + Arrays.toString(args));

                // 执行织入
                MessageHandler handler = new MessageHandler(true);
                new Main().run(args, handler);

                // 处理织入消息
                processWeavingMessages(handler);

                System.out.println("✅ AspectJ 织入成功完成: " + javaCompile.getName());
            } catch (Exception e) {
                System.err.println("❌ AspectJ 织入过程中出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 构建 AspectJ 参数
     */
    private static String[] buildAspectJArgs(Project project, JavaCompile javaCompile) {
        return new String[]{
                "-showWeaveInfo",
//                "-1.8",// TODO: 2025/11/5 代码获取编译使用的java版本
                "-" + JavaVersion.current(),
                "-inpath", javaCompile.getDestinationDirectory().getAsFile().get().toString(),
                "-aspectpath", javaCompile.getClasspath().getAsPath(),
                "-d", javaCompile.getDestinationDirectory().getAsFile().get().toString(),
                "-classpath", javaCompile.getClasspath().getAsPath(),
                "-bootclasspath", getBootClasspath(project)
        };
    }

    /**
     * 获取 Android bootclasspath
     */
    private static String getBootClasspath(Project project) {
        try {
            // 尝试通过反射获取 Android bootclasspath
            Object androidExtension = project.getExtensions().findByName("android");
            if (androidExtension != null) {
                // 这里需要根据具体的 Android Gradle Plugin 版本调整
                List<File> bootClasspathFiles = project.getExtensions().getByType(AppExtension.class).getBootClasspath();
                StringBuilder pathBuilder = new StringBuilder();
                for (int i = 0; i < bootClasspathFiles.size(); i++) {
                    pathBuilder.append(bootClasspathFiles.get(i).getAbsolutePath());
                    if (i < bootClasspathFiles.size() - 1) {
                        pathBuilder.append(File.pathSeparator);
                    }
                }
                System.out.println("✅ 获取 bootclasspath， " + pathBuilder);
                return pathBuilder.toString();
            }
        } catch (Exception e) {
            System.out.println("⚠️ 无法获取 bootclasspath，使用默认配置");
        }

        // 返回默认的 Android bootclasspath
        return System.getProperty("sun.boot.class.path", "");
    }

    /**
     * 处理织入消息
     */
    private static void processWeavingMessages(MessageHandler handler) {
        for (IMessage message : handler.getMessages(null, true)) {
            IMessage.Kind kind = message.getKind();
            if (kind.equals(IMessage.ABORT) || kind.equals(IMessage.ERROR) || kind.equals(IMessage.FAIL)) {
                System.err.println("❌ AspectJ 错误: " + message.getMessage());
                if (message.getThrown() != null) {
                    message.getThrown().printStackTrace();
                }
            } else if (kind.equals(IMessage.WARNING)) {
                System.out.println("⚠️ AspectJ 警告: " + message.getMessage());
            } else if (kind.equals(IMessage.INFO)) {
                System.out.println("ℹ️ AspectJ 信息: " + message.getMessage());
            } else if (kind.equals(IMessage.DEBUG)) {
                System.out.println("🔍 AspectJ 调试: " + message.getMessage());
            }
        }
    }

    /**
     * 验证 AspectJ 配置
     */
    private static void verifyAspectJConfiguration(Project project) {
        try {
            // 检查必要的依赖
            boolean hasAspectJRt = false;
            for (Dependency dep : project.getConfigurations()
                    .getByName("implementation").getDependencies()) {
                if (dep.getName().equals("aspectjrt")) {
                    hasAspectJRt = true;
                    break;
                }
            }

            if (hasAspectJRt) {
                System.out.println("✅ aspectjrt 依赖已配置");
            } else {
                System.out.println("⚠️ aspectjrt 依赖未配置");
            }

            // 检查 AspectJ 工具类是否可用
            try {
                Class.forName("org.aspectj.lang.JoinPoint");
                System.out.println("✅ AspectJ 核心类可用");
            } catch (ClassNotFoundException e) {
                System.out.println("❌ AspectJ 核心类不可用");
            }

        } catch (Exception e) {
            System.err.println("❌ 配置验证失败: " + e.getMessage());
        }
    }

    /**
     * 为特定变体配置 AspectJ
     */
    public static void configureForVariant(Project project, String variantName) {
        project.getTasks().register("configureAspectJFor" +
                variantName.substring(0, 1).toUpperCase() + variantName.substring(1), task -> {
            task.doLast(t -> {
                System.out.println("🎯 为变体配置 AspectJ: " + variantName);
                configureSpecificVariant(project, variantName);
            });
        });
    }

    /**
     * 配置特定变体
     */
    private static void configureSpecificVariant(Project project, String variantName) {
        // 查找特定变体的编译任务
        for (JavaCompile javaCompile : project.getTasks().withType(JavaCompile.class)) {
            if (javaCompile.getName().toLowerCase().contains(variantName.toLowerCase())) {
                configureJavaCompileTask(project, javaCompile);
                System.out.println("✅ 为变体配置完成: " + variantName);
            }
        }
    }
}
