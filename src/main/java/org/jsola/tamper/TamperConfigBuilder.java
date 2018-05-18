/*
 * @date 2018年05月18日 10:58
 */
package org.jsola.tamper;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.component.annotations.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.jsola.tamper.TamperConfig.*;

/**
 * @author june
 */
@Component(role = ITamperConfigBuilder.class)
public class TamperConfigBuilder implements ITamperConfigBuilder {

    private Log log = new SystemStreamLog();

    @Override
    public TamperConfig build(File configFile,File sourceFile) {
        if(configFile != null && !configFile.exists()){
            log.error("config file not found,path:"+configFile.getAbsolutePath());
            throw new RuntimeException("config file not found,path:"+configFile.getAbsolutePath());
        }
        TamperConfig tamperConfig = new TamperConfig();
        if(configFile == null || !configFile.exists()){
            configFile = getDefaultConfigFile(sourceFile);
        }
        if(configFile == null){
            log.error("default config file not found.");
            throw new RuntimeException("default config file not found.");
        }
        tamperConfig.setConfigFile(configFile);
        String fileName = configFile.getName();
        String prefix = fileName.substring(0,fileName.lastIndexOf("."));
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        tamperConfig.setConfigFileNamePrefix(prefix);
        tamperConfig.setConfigFileNameSuffix(suffix);
        tamperConfig.setYaml(!PROPERTY_SUFFIX.equalsIgnoreCase(suffix));
        if(tamperConfig.isYaml()){
            tamperConfig.setConfigValueMap(getYamlValueMap(configFile));
        }else{
            tamperConfig.setConfigValueMap(getPropertiesValueMap(configFile));
        }
        tamperConfig.setCloudConfigUri(buildCloudConfigUri(tamperConfig.getConfigValueMap()));
        return tamperConfig;
    }

    /**
     * 如果配置文件不存在，则获取默认配置文件
     * @return 默认配置文件
     */
    private File getDefaultConfigFile(File sourceFile) {
        try {
            // jar或war
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(sourceFile));
            File tmpConfigFile;
            byte[] buffer = new byte[1024];
            while (true) {
                JarEntry jarEntry = jarInputStream.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while (true) {
                    int length = jarInputStream.read(buffer, 0, buffer.length);
                    if (length <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(buffer, 0, length);
                }
                for (String configFilePath : CONFIG_FILE_PATHS) {
                    String prefix = configFilePath.substring(0,configFilePath.lastIndexOf("."));
                    String suffix = configFilePath.substring(configFilePath.lastIndexOf("."));
                    if (jarEntry.getName().equals(configFilePath)){
                        tmpConfigFile = File.createTempFile(prefix+"@", suffix);
                        byteArrayOutputStream.writeTo(new FileOutputStream(tmpConfigFile));
                        return tmpConfigFile;
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("getDefaultConfigFile error.",e);
            throw new RuntimeException("getDefaultConfigFile error",e);
        }
        return null;
    }

    /**
     * yaml转map
     * @param configFile 配置文件
     * @return map
     */
    @SuppressWarnings("unchecked")
    private Map<String,Object> getYamlValueMap(File configFile){
        try{
            String yamlText = FileUtils.readFileToString(configFile);
            Yaml yaml = new Yaml();
            Map<String,Object> initMap = (Map<String, Object>) yaml.load(yamlText);
            Map<String,Object> valueMap = new HashMap<String, Object>(20);
            changeToKeyValueMap(initMap,valueMap,null);
            return valueMap;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("getYamlValueMap error.",e);
            throw new RuntimeException("getYamlValueMap error",e);
        }
    }

    /**
     * yaml深层map 打平转为一级map
     * @param yamlMap yamlMap
     * @param valueMap 打平的map
     * @param vKey key
     */
    @SuppressWarnings("unchecked")
    private void changeToKeyValueMap(Map<String,Object> yamlMap,Map<String,Object> valueMap,String vKey){
        if(yamlMap != null && !yamlMap.isEmpty()) {
            String tempKey = vKey;
            for (String key : yamlMap.keySet()) {
                Object value = yamlMap.get(key);
                if (vKey == null) {
                    vKey = key;
                }else{
                    vKey += "." + key;
                }
                if (value instanceof Map) {
                    changeToKeyValueMap((Map<String, Object>) value, valueMap, vKey);
                }else {
                    if (valueMap == null) {
                        valueMap = new HashMap<String, Object>(20);
                    }
                    valueMap.put(vKey, value);
                }
                vKey = tempKey;
            }
        }
    }

    /**
     * 把Properties配置文件转换成map
     * @param configFile Properties配置文件
     * @return map
     */
    private Map<String,Object> getPropertiesValueMap(File configFile){
        try {
            Map<String,Object> valueMap = new HashMap<String,Object>(20);
            List lines = FileUtils.readLines(configFile,"utf-8");
            for (Object obj : lines) {
                if(obj == null){
                    continue;
                }
                String line = obj.toString();
                // 注释
                if(line.startsWith("#") || line.startsWith("!")){
                    continue;
                }
                int markIndex = line.indexOf("=");
                if(markIndex == -1) {
                    markIndex = line.indexOf(":");
                }
                if(markIndex == -1){
                    continue;
                }
                String key = line.substring(0,markIndex).trim().toLowerCase();
                String value = line.substring(markIndex+1);
                valueMap.put(key,value);
            }
            return valueMap;
        } catch (IOException e) {
            log.error("getPropertiesValueMap error.",e);
            throw new RuntimeException("getPropertiesValueMap error",e);
        }
    }


    /**
     * 构建配置下载地址
     * @param valueMap 配置
     * @return 下载地址
     */
    private String buildCloudConfigUri(Map<String,Object> valueMap){
        Object profileObj = valueMap.get(CONFIG_PROFILE_KEY);
        String profile = profileObj.toString();
        if(profileObj instanceof Collection){
            StringBuilder tempStr = new StringBuilder();
            for (Object o : (Collection) profileObj) {
                if(tempStr.length() == 0){
                    tempStr = new StringBuilder(o.toString());
                }else{
                    tempStr.append(",").append(o.toString());
                }
            }
            profile = tempStr.toString();
        }
        String label = valueMap.get(CONFIG_LABEL_KEY).toString();
        String uri = valueMap.get(CONFIG_URI_KEY).toString();
        String name = valueMap.get(APP_NAME_KEY).toString();
        return uri+"/"+name+"/"+profile+"/"+label;
    }
}
