/*
 * @date 2018年05月18日 11:42
 */
package org.jsola.tamper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.codearte.props2yaml.Props2YAML;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.component.annotations.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import static org.jsola.tamper.TamperConfig.*;

/**
 * @author june
 */
@Component(role = ITamperService.class)
public class TamperServiceImpl implements ITamperService {

    private Log log = new SystemStreamLog();


    @Override
    @SuppressWarnings("unchecked")
    public List<File> fetchCloudConfig(TamperConfig tamperConfig) {
        try {
            String configJsonStr = fetchCloudConfigJson(tamperConfig.getCloudConfigUri());
            JSONObject jsonObject = (JSONObject) JSON.parse(configJsonStr);
            JSONArray propertySources = jsonObject.getJSONArray("propertySources");
            List<File> resultFiles = new ArrayList<File>();
            for (int i = 0; i < propertySources.size(); i++) {
                JSONObject propJsonObj = propertySources.getJSONObject(i);
                String name = propJsonObj.getString("name");
                name = name.substring(name.lastIndexOf("/")+1);
                String prefix = name.substring(0,name.lastIndexOf("."));
                String suffix = name.substring(name.lastIndexOf(".") + 1);

                JSONObject sourceJsonObj = propJsonObj.getJSONObject("source");
                Map<String,Object> valueMap = sourceJsonObj.toJavaObject(Map.class);
                String propertyString = convertPropertyString(valueMap);
                // 生成临时文件，添加一个@，方便重命名
                File tmpFile = File.createTempFile(prefix+"@", "."+suffix);
                if("yml".equalsIgnoreCase(suffix) || "yaml".equalsIgnoreCase(suffix)){
                    // 转yaml
                    propertyString = Props2YAML.fromContent(propertyString).convert();
                }
                FileUtils.writeStringToFile(tmpFile,propertyString);
                log.info("Fetch cloud config file success:"+tmpFile.getAbsolutePath());
                resultFiles.add(tmpFile);
            }
            return resultFiles;
        }catch (Exception e){
            e.printStackTrace();
            log.error("fetchCloudConfig error.",e);
            throw new RuntimeException("fetchCloudConfig error.",e);
        }
    }

    /**
     * 从配置中心下载配置
     * @param uri 配置中心地址
     * @return 配置
     */
    private String fetchCloudConfigJson(String uri){
        int responseCode;
        log.info("start fetch cloud config,URL:"+uri);
        try {
            URL obj = new URL(uri);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            //添加请求头
            con.setRequestProperty("Content-Type", "application/json");
            responseCode = con.getResponseCode();
            if(responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("fetchCloudConfigJson error.",e);
            throw new RuntimeException("fetchCloudConfigJson error.",e);
        }
        log.error("fetchCloudConfigJson error.responseCode:"+responseCode);
        throw new RuntimeException("fetchCloudConfigJson error.responseCode:"+responseCode);
    }


    @Override
    public void replaceConfigFile(TamperConfig tamperConfig) {
        Map<String,Object> valueMap = tamperConfig.getConfigValueMap();
        valueMap.put(ENABLED_KEY,false);
        valueMap.put(ACTIVE_PROFILE_KEY,valueMap.get(CONFIG_PROFILE_KEY));
        valueMap.put(CONFIG_NAME_KEY,"${spring.application.name},application");
        try {
            String propertyString = convertPropertyString(valueMap);
            if(tamperConfig.isYaml()) {
                // 转yaml
                propertyString = Props2YAML.fromContent(propertyString).convert();
            }
            File tmpConfigFile = File.createTempFile(tamperConfig.getConfigFileNamePrefix()+"@", "."+tamperConfig
                    .getConfigFileNameSuffix());
            FileUtils.writeStringToFile(tmpConfigFile,propertyString);
            log.info("replace config file success:"+tamperConfig.getConfigFile().getAbsolutePath());
            tamperConfig.setConfigFile(tmpConfigFile);
            log.info("new temp config file path:"+tmpConfigFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("replaceConfigFile error.",e);
            throw new RuntimeException("replaceConfigFile error",e);
        }
    }

    @Override
    public void replace(File newConfigFile, List<File> cloudConfigFiles, File file) {
        try {
            String configFileName = newConfigFile.getName();
            String newConfigFileName = configFileName.substring(0,configFileName.indexOf("@"))+configFileName.substring(configFileName.lastIndexOf("."));
            // 如果是文件夹
            if(file.isDirectory()){
                FileUtils.copyFile(newConfigFile, new File(file.getAbsolutePath()+"/WEB-INF/classes/",newConfigFileName));
                for (File cloudConfigFile : cloudConfigFiles) {
                    String name = cloudConfigFile.getName();
                    String newName = name.substring(0,name.indexOf("@"))+name.substring(name.lastIndexOf("."));
                    File newFile = new File(file.getAbsolutePath()+"/WEB-INF/classes/",newName);
                    FileUtils.copyFile(cloudConfigFile,newFile);
                }
            }else{
                // jar或war
                JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file));

                File tmpJar = File.createTempFile(Long.toString(System.nanoTime()), ".jar");
                log.info("Tmp file: " + tmpJar.getAbsolutePath());
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpJar));
                byte[] buffer = new byte[1024];
                while (true) {
                    JarEntry jarEntry = jarInputStream.getNextJarEntry();
                    if (jarEntry == null){
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
                    if(jarEntry.getName().equals("WEB-INF/classes/"+newConfigFileName)){
                        JarEntry configJarEntry = new JarEntry("WEB-INF/classes/"+newConfigFileName);
                        jarOutputStream.putNextEntry(configJarEntry);
                        IOUtils.write(FileUtils.readFileToByteArray(newConfigFile),jarOutputStream);
                    }else {
                        jarOutputStream.putNextEntry(jarEntry);
                        byteArrayOutputStream.writeTo(jarOutputStream);
                    }
                }
                // todo 判断本地是否已经包含了远程同名的配置文件，如果存在，则合并配置
                for (File cloudConfigFile : cloudConfigFiles) {
                    String name = cloudConfigFile.getName();
                    String newName = name.substring(0,name.indexOf("@"))+name.substring(name.lastIndexOf("."));
                    JarEntry newJarEntry = new JarEntry("WEB-INF/classes/"+newName);
                    jarOutputStream.putNextEntry(newJarEntry);
                    IOUtils.write(FileUtils.readFileToByteArray(cloudConfigFile),jarOutputStream);
                }

                IOUtils.closeQuietly(jarInputStream);
                IOUtils.closeQuietly(jarOutputStream);

                log.info("Replacing: " + file.getAbsolutePath() + " with: " + tmpJar.getAbsolutePath());
                FileUtils.copyFile(tmpJar, file);
                FileUtils.forceDeleteOnExit(tmpJar);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("replace error.",e);
            throw new RuntimeException("replace error",e);
        }
    }

    /**
     * map转 PropertyString
     * @param valueMap valueMap
     * @return PropertyString
     */
    private String convertPropertyString(Map<String,Object> valueMap){
        StringBuilder content = new StringBuilder();
        for (String key : valueMap.keySet()) {
            Object value = valueMap.get(key);
            if(value instanceof Collection){
                StringBuilder tempStr = new StringBuilder();
                for (Object o : (Collection) value) {
                    if(tempStr.length() == 0){
                        tempStr = new StringBuilder(o.toString());
                    }else{
                        tempStr.append(",").append(o.toString());
                    }
                }
                content.append(key).append("=").append(tempStr.toString());
            }else {
                content.append(key).append("=").append(value);
            }
            content.append("\n");
        }
        return content.toString();
    }
}
