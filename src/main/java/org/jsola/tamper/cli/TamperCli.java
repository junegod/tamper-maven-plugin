/*
 * @date 2018年05月18日 12:52
 */
package org.jsola.tamper.cli;

import org.apache.commons.cli.*;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.jsola.tamper.ITamperConfigBuilder;
import org.jsola.tamper.ITamperService;
import org.jsola.tamper.TamperConfig;

import java.io.File;
import java.util.List;

/**
 * @author june
 */
public class TamperCli {

    private static Log log = new SystemStreamLog();


    public static void main(String[] args) throws Exception {
        Options options = buildOptions();
        CommandLine cmd = new BasicParser().parse(options, args);

        displayHelpIfNeeded(options, cmd);

        File target = null;
        File config = null;
        File source = null;
        if(cmd.getArgs().length > 0){
            target = new File(cmd.getArgs()[0]);
            log.info("target File:"+target.getAbsolutePath());
        }
        if (cmd.hasOption("s") && cmd.getOptionValue("s") != null) {
            source = new File(cmd.getOptionValue("s"));
        }
        if (cmd.hasOption("c") && cmd.getOptionValue("c") != null) {
            config = new File(cmd.getOptionValue("c"));
        }
        verify(target, config);

        PlexusContainer container = new DefaultPlexusContainer();
        ITamperConfigBuilder tamperConfigBuilder = container.lookup(ITamperConfigBuilder.class);
        TamperConfig tamperConfig = tamperConfigBuilder.build(config,source);
        ITamperService tamperService = container.lookup(ITamperService.class);
        // 下载cloud 配置
        List<File> cloudConfigFiles = tamperService.fetchCloudConfig(tamperConfig);
        // 替换本地配置
        tamperService.replaceConfigFile(tamperConfig);
        if (cmd.hasOption("s") && cmd.getOptionValue("s") != null) {
            tamperService.replace(tamperConfig.getConfigFile(), cloudConfigFiles, source);
        }
        else {
            tamperService.replace(tamperConfig.getConfigFile(), cloudConfigFiles, target);
        }
    }

    private static void verify(File target, File config) {
        // 指定target必须指定配置文件
        if(target != null && (config == null || !config.exists())){
            System.err.println("config file does not exist.");
            System.exit(1);
        }
        if (target != null && !target.exists()) {
            System.err.println("File/Directory: " + target.getAbsolutePath() + " does not exist");
            System.exit(1);
        }
        if (config != null && !config.exists()) {
            System.err.println("Config: " + config.getAbsolutePath() + " does not exist");
            System.exit(1);
        }
    }

    private static void displayHelpIfNeeded(Options options, CommandLine cmd) {
        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar tamper-maven-plugin-1.0-cli.jar", options);
            System.exit(0);
        }
    }

    private static Options buildOptions() {
        Options options = new Options();

        Option c = OptionBuilder.withArgName("configFile").hasArg().withDescription("the cloud config file").create("c");
        Option s = OptionBuilder.withArgName("sourceFile").hasArg().withDescription("the source file of config values").create("s");
        Option h = OptionBuilder.withDescription("display help").create("h");

        options.addOption(c);
        options.addOption(h);
        options.addOption(s);
        return options;
    }
}
