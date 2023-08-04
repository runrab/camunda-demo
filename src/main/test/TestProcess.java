import com.runrab.Application;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 功能描述：
 *
 * @author runrab
 * @date: 2023/8/4 16:44
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class TestProcess {

    private final String driver = "com.mysql.cj.jdbc.Driver";
    @Value("${db-info.host}")
    private String host;
    @Value("${db-info.port}")
    private String port;
    @Value("${db-info.username}")
    private String username;
    @Value("${db-info.password}")
    private String password;
    private String dbName="workflow";
    @Test
    public void test(){
        // nullCatalogMeansCurrent zeroDateTimeBehavior 不填会报错
        String suffix = "?useUnicode=true&characterEncoding=utf8&useSSL=false&zeroDateTimeBehavior=convertToNull&serverTimezone=GMT%2B8&nullCatalogMeansCurrent=true";
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + suffix;
        try {
            ProcessEngineConfiguration config = ProcessEngineConfiguration
                    .createStandaloneProcessEngineConfiguration().setJdbcDriver(driver)
                    .setJdbcUrl(url)
                    .setJdbcUsername(username).setJdbcPassword(password)
                    .setAuthorizationEnabled(false)
                    .setHistory(ProcessEngineConfiguration.HISTORY_AUDIT)//日志级别 audit
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);// 初次创建的时候设置为true
            ProcessEngine processEngine = config.buildProcessEngine();
            processEngine.close();
        } catch (Exception e) {
            System.out.println();
        }
    }
}


//@SpringBootTest
//class FlowApplicationTests {
//
//    @org.junit.jupiter.api.Test
//    void contextLoads() {
//    }
//
//}
