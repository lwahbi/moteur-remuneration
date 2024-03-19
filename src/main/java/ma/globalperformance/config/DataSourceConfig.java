package ma.globalperformance.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void initialize() {
        try {
            // set login timeout to 10 seconds
            dataSource.setLoginTimeout(10);
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                log.info("Active connections: " + hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                log.info("getThreadsAwaitingConnection connections: " + hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            }
        } catch (SQLException e) {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                log.info("Active connections: " + hikariDataSource.getHikariPoolMXBean().getActiveConnections());
            }
            throw new RuntimeException(e);
        }
    }
}
