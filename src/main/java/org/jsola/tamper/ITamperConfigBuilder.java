/*
 * @date 2018年05月18日 10:52
 */
package org.jsola.tamper;

import java.io.File;

/**
 * @author june
 */
public interface ITamperConfigBuilder {

    /**
     * 构建配置
     * @param configFile 配置文件
     * @param sourceFile 需要替换的文件
     * @return 配置
     */
    TamperConfig build(File configFile,File sourceFile);
}
