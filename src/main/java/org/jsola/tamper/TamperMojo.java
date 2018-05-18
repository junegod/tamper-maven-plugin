/*
 * @date 2018年05月11日 10:46
 */
package org.jsola.tamper;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * @author june
 */
@Mojo(
        name = "replace-package",
        defaultPhase = LifecyclePhase.PACKAGE
)
public class TamperMojo extends AbstractMojo {

    @Parameter(property = "disableCloudConfig")
    private Boolean disableCloudConfig;

    @Parameter(property = "configFile")
    private File configFile;

    @Parameter(readonly = true, defaultValue = "${project.packaging}")
    private String packaging;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File outputDirectory;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}")
    private File finalPackage;

    @Component(role = ITamperService.class)
    private ITamperService tamperService;

    @Component(role = ITamperConfigBuilder.class)
    private ITamperConfigBuilder tamperConfigBuilder;
    private Log log = getLog();


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // 如果设置了configFile 默认开启
        if (configFile != null && disableCloudConfig == null){
            disableCloudConfig = true;
        }
        // 默认不开启 禁用cloud配置
        if (disableCloudConfig == null || !disableCloudConfig) {
            log.warn("disableCloudConfig is not config or false, skipping running.");
            return;
        }
        if (isSupported(packaging)) {
            TamperConfig tamperConfig = tamperConfigBuilder.build(configFile,finalPackage);
            // 从配置中心下载配置
            List<File> cloudConfigFiles = tamperService.fetchCloudConfig(tamperConfig);
            // 替换本地配置
            tamperService.replaceConfigFile(tamperConfig);
            if (outputDirectory.exists() && outputDirectory.isDirectory()) {
                log.info("Replacing: " + outputDirectory.getAbsolutePath());
                tamperService.replace(tamperConfig.getConfigFile(), cloudConfigFiles, outputDirectory);
            }
            log.info("Replacing: " + finalPackage.getAbsolutePath());
            tamperService.replace(tamperConfig.getConfigFile(), cloudConfigFiles, finalPackage);
        } else {
            log.info(String.format("Ignoring packaging %s", packaging));
        }
    }

    /**
     * 只支持war和jar
     * @param packaging 打包类型
     * @return true or false
     */
    private boolean isSupported(String packaging) {
        return "war".equals(packaging) || "jar".equals(packaging);
    }
}
