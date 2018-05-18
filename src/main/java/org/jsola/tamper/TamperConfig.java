/*
 * @date 2018年05月18日 10:52
 */
package org.jsola.tamper;

import java.io.File;
import java.util.Map;

/**
 * @author june
 */
public class TamperConfig {

    public final static String DEFAULT_CONFIG_FILE_DIRECTORY = "WEB-INF/classes/";
    public final static String[] CONFIG_FILE_PATHS = new String[]{
            DEFAULT_CONFIG_FILE_DIRECTORY + "bootstrap.properties",
            DEFAULT_CONFIG_FILE_DIRECTORY + "bootstrap.yml",
            DEFAULT_CONFIG_FILE_DIRECTORY + "bootstrap.yaml",
            DEFAULT_CONFIG_FILE_DIRECTORY + "application.properties",
            DEFAULT_CONFIG_FILE_DIRECTORY + "application.yml",
            DEFAULT_CONFIG_FILE_DIRECTORY + "application.yaml"
    };
    public final static String ENABLED_KEY = "spring.cloud.config.enabled";
    public final static String CONFIG_PROFILE_KEY = "spring.cloud.config.profile";
    public final static String CONFIG_URI_KEY = "spring.cloud.config.uri";
    public final static String CONFIG_LABEL_KEY = "spring.cloud.config.label";
    public final static String APP_NAME_KEY = "spring.application.name";
    public final static String ACTIVE_PROFILE_KEY = "spring.profiles.active";
    public final static String CONFIG_NAME_KEY = "spring.config.name";

    public final static String PROPERTY_SUFFIX = "properties";

    /**
     * 配置文件
     */
    private File configFile;

    /**
     * 配置文件名前缀
     */
    private String configFileNamePrefix;

    /**
     * 配置文件名后缀，扩展名
     */
    private String configFileNameSuffix;

    /**
     * 配置文件的所有key和value，打平，property类型
     */
    private Map<String,Object> configValueMap;

    /**
     * 是否是yaml配置
     */
    private boolean isYaml;

    /**
     * 配置中心下载地址
     */
    private String cloudConfigUri;



    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    public String getConfigFileNamePrefix() {
        return configFileNamePrefix;
    }

    public void setConfigFileNamePrefix(String configFileNamePrefix) {
        this.configFileNamePrefix = configFileNamePrefix;
    }

    public String getConfigFileNameSuffix() {
        return configFileNameSuffix;
    }

    public void setConfigFileNameSuffix(String configFileNameSuffix) {
        this.configFileNameSuffix = configFileNameSuffix;
    }

    public Map<String, Object> getConfigValueMap() {
        return configValueMap;
    }

    public void setConfigValueMap(Map<String, Object> configValueMap) {
        this.configValueMap = configValueMap;
    }

    public boolean isYaml() {
        return isYaml;
    }

    public void setYaml(boolean yaml) {
        isYaml = yaml;
    }

    public String getCloudConfigUri() {
        return cloudConfigUri;
    }

    public void setCloudConfigUri(String cloudConfigUri) {
        this.cloudConfigUri = cloudConfigUri;
    }
}
