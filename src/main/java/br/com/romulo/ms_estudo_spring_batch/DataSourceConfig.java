package br.com.romulo.ms_estudo_spring_batch;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration//gerenciado pelo spring
public class DataSourceConfig {
	//@Bean de que precisamos para o data source e para o próprio transactionManager injetado na classe de importação do job
	@Bean
	@ConfigurationProperties(prefix = "spring.datasource") //identifica quem eh a fonte de dados, a configuracao no arquivo properties eh a spring.datasource, os dados sao buscados por aquii
    public DataSource dataSource() {//mesmo nome referenciado no job
        return DataSourceBuilder.create().build();
    }
	
	//controle de transações para indicar que ele é feito nesse data source
	//Retornaremos um novo DataSourceTransactionManager, passando o dataSource indicado. Assim, o controle de transação transactionManager opera e controla transações no @Qualifier que definimos como dataSource.
	@Bean
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
