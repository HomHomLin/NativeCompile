package io.github.lizhangqu.nativecompile.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.util.GFileUtils

class NativeCompilePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.getExtensions().findByName('android')
        if (extension == null) {
            return
        }
        project.getExtensions().create("nativeCompile", NativeCompileExtension.class, project)

        def mainSourceSet = extension.getSourceSets().getByName(extension.getDefaultConfig().getName())
        Set<File> jniLibsDirs = mainSourceSet.getJniLibs().getSrcDirs()
        if (jniLibsDirs.size() == 0) {
            mainSourceSet.getJniLibs().srcDirs(project.file("src/main/jniLibs"))
        }
        File jniLibsDir = mainSourceSet.getJniLibs().getSrcDirs().toList().get(0)
        createConfiguration(project, 'nativeCompile', jniLibsDir)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    void createConfiguration(Project project, String configurationName, File jniLibsDir) {
        project.logger.error("jniLibsDir:${jniLibsDir}")
        Configuration nativeCompileConfiguration = project.getConfigurations().create(configurationName) { Configuration nativeCompileConfiguration ->
            //禁止传递依赖
            nativeCompileConfiguration.setTransitive(false)
            nativeCompileConfiguration.resolutionStrategy {
                cacheChangingModulesFor(0, 'seconds')
                cacheDynamicVersionsFor(5, 'minutes')
            }
        }

        project.afterEvaluate {
            NativeCompileExtension nativeExtension = project.getExtensions().findByType(NativeCompileExtension.class)
            nativeCompileConfiguration.getDependencies().each { Dependency nativeDependency ->
                FileCollection collection = nativeCompileConfiguration.fileCollection(nativeDependency).filter { File file ->
                    //返回so文件
                    return file.getName().endsWith(".so")
                }
                //遍历
                collection.files.each { File srcFile ->
                    //文件后缀
                    String suffix = srcFile.getName().substring(srcFile.getName().lastIndexOf("."))
                    //依赖classifier
                    String classifier = srcFile.getName() - nativeDependency.getName() - "-" - nativeDependency.getVersion() - "-" - suffix
                    //如果classifier为空，则默认使用armeabi
                    if (classifier == null || classifier.length() == 0) {
                        classifier = nativeExtension.defaultClassifier
                    }
                    //目标目录
                    File destDir = new File(jniLibsDir, classifier)
                    //目标文件名
                    String destFileName = "${nativeDependency.getName().startsWith('lib') ? '' : 'lib'}${nativeDependency.getName()}.so"
                    //删除旧文件
                    GFileUtils.deleteQuietly(new File(destDir, destFileName))
                    //拷贝源文件到目标
                    project.copy { CopySpec copySpec ->
                        copySpec.from(srcFile)
                        copySpec.into(destDir)
                        copySpec.rename(srcFile.getName(), destFileName)
                    }
                }
            }
        }
    }
}