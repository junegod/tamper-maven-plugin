/*
 * @date 2018年05月18日 11:29
 */
package org.jsola.tamper;

import java.io.File;
import java.util.List;

/**
 * @author june
 */
public interface ITamperService {
    /**
     * 从配置中心下载配置文件
     * @param tamperConfig 配置
     * @return 配置中心的配置
     */
    List<File> fetchCloudConfig(TamperConfig tamperConfig);

    /**
     * 替换配置文件
     * spring.cloud.config.enabled设为false
     * 增加或替换spring.profiles.active配置
     * 指定spring.config.name为${spring.application.name},application
     * @param tamperConfig 配置
     */
    void replaceConfigFile(TamperConfig tamperConfig);

    /**
     * 执行替换jar包或target目录
     * @param newConfigFile 新的配置文件
     * @param cloudConfigFiles 配置中心的配置文件
     * @param file 需要替换的文件 jar或目录
     */
    void replace(File newConfigFile, List<File> cloudConfigFiles, File file);
}
