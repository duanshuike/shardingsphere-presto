# shardingsphere-presto

基于Presto插件框架实现的ShardingSphere连接器

## 工具简介

shardingsphere-presto是基于Presto插件框架实现的ShardingSphere连接器，由于采用shardingsphere的JDBC模式，避免了额外的部署，且提升了相应的查询性能。

## 使用步骤

### 1. 设置版本

根据实际使用的shardingsphere和presto版本，修改pom中的版本属性

```xml
        <shardingsphere.version>5.2.0</shardingsphere.version>
        <presto.version>0.275</presto.version>
```

### 2. 构建此工具

执行mvn package命令，得到target/shardingsphere即为此工具，将其上传的presto的plugin目录下。

### 3. 配置catalog

在presto的etc/catlog下新增example.properties，其内容示例如下：

```properties
connector.name=shardingsphere
shardingsphere-rule-location=~/test-db.yaml
```

其中shardingsphere-rule-location为shardingsphere数据源的分片规则文件。