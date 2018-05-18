package org.jsola.tamper;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author june
 */
public class TamperServiceImplTest {

    private ITamperService tamperService = new TamperServiceImpl();
    private ITamperConfigBuilder tamperConfigBuilder = new TamperConfigBuilder();
    private TamperConfig yamlTamperConfig = null;
    private TamperConfig propTamperConfig = null;


    @Before
    public void setup() throws Exception {
        File yamlConfigFile = new File("/Users/june/IdeaProjects/tamper-maven-plugin/src/test/reources/bootstrap.yml");
        File propertiesConfigFile = new File("/Users/june/IdeaProjects/tamper-maven-plugin/src/test/reources/bootstrap.properties");
        yamlTamperConfig = new TamperConfigBuilder().build(yamlConfigFile,null);
        propTamperConfig = new TamperConfigBuilder().build(propertiesConfigFile,null);
    }

    @Test
    public void fetchCloudConfig() throws Exception {
        List<File> cloudFiles = tamperService.fetchCloudConfig(yamlTamperConfig);
        for (File cloudFile : cloudFiles) {
            System.out.println(cloudFile.getAbsolutePath());
            System.out.println(FileUtils.readFileToString(cloudFile));
        }
    }

    @Test
    public void replaceConfigFile() throws Exception {
        tamperService.replaceConfigFile(propTamperConfig);
        System.out.println(FileUtils.readFileToString(propTamperConfig.getConfigFile()));

        tamperService.replaceConfigFile(yamlTamperConfig);
        System.out.println(FileUtils.readFileToString(yamlTamperConfig.getConfigFile()));

    }

    @Test
    public void replace() throws Exception {
        tamperService.replaceConfigFile(yamlTamperConfig);
        List<File> cloudFiles = tamperService.fetchCloudConfig(yamlTamperConfig);
        File file = new File(this.getClass().getResource("/user.war").toURI());
        tamperService.replace(yamlTamperConfig.getConfigFile(),cloudFiles,file);
    }

    @Test
    public void replace2() throws Exception {
        tamperService.replaceConfigFile(yamlTamperConfig);
        List<File> cloudFiles = tamperService.fetchCloudConfig(yamlTamperConfig);
        File file = new File(this.getClass().getResource("/user").toURI());
        tamperService.replace(yamlTamperConfig.getConfigFile(),cloudFiles,file);
    }

}