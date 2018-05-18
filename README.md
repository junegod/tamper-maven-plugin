# Spring Cloud 配置中心配置本地化

spring cloud配置中心的配置如果使用git的方式托管，存在一个问题，war包和配置是分离的，导致如果部署后要还原到其他版本，需要还原war包和配置中心的配置。
本插件很完美的解决这问题，基本思路：

1.maven打包的时候从配置中心把配置下载下来
2.修改本地的spring boot配置文件(一般是bootstrap.yml),禁用配置中心

第二步会替换如下配置：
* spring.cloud.config.enabled设为false
* 增加或替换spring.profiles.active配置
* 指定spring.config.name为${spring.application.name},application
     
这样就把配置也本地化了，一起打包在war里，不管是还原，还是docker容器化，都比较方便。
同时，又没有损失统一配置的方便性，一举两得。

## maven插件使用方式

```xml
<plugin>
    <groupId>org.jsola</groupId>
    <artifactId>tamper-maven-plugin</artifactId>
    <version>1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>replace-package</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<!-- 必须放在spring boo插件前面 -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```
打包参数

```bash
# disableCloudConfig 默认为false，也就不开启替换。不指定配置文件会查找默认
mvn clean package -DdisableCloudConfig=true

# 指定配置文件，指定该属性后disableCloudConfig不生效
mvn clean package -DconfigFile=src/main/resources/bootstrap.yml 
```

## 命令行方式

插件还支持命令行的方式，这样不用每个应用去配置maven插件，直接加在部署脚本上即可。

```bash
java -jar tamper-maven-plugin-1.0-cli.jar -s user.war
```

`-s` 指定war包。会默认查找war包里classes下的spring boot配置文件，查找顺序：

```
bootstrap.properties
bootstrap.yml
bootstrap.yaml
application.properties
application.yml
application.yaml
```

`-c` 指定spring boot配置文件

## todo

如果本地和配置中心文件都存在，则需要合并，目前是不支持，会报错
