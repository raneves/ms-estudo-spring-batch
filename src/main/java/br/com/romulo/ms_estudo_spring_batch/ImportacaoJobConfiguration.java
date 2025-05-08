package br.com.romulo.ms_estudo_spring_batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;



//classe responsavel pela configuracao do job
@Configuration
public class ImportacaoJobConfiguration {
	@Autowired
    private PlatformTransactionManager transactionManager;
	
	
	@Bean //seja gerenciado pelo spring
    public Job job(Step passoInicial, JobRepository jobRepository) {
		
		//O JobRepository mantém o estado do job e funciona como uma máquina de estados. Ele registra a duração da execução, status, erros de escrita e leitura, e é compartilhado com todos os outros passos da execução e componentes da solução.
        return new JobBuilder("geracao-tickets", jobRepository)//O job precisa de um nome, e poderemos chamá-lo de "geracao-tickets". O .start() precisará de um step inicial, que chamaremos de passoInicial.
            .start(passoInicial) //STEP
            .next(moverArquivosStep(jobRepository))//ao final do passo inicial, executar o proximo passo, moverarquivostep
            .incrementer(new RunIdIncrementer()) //vai gerar logs, a cada execucao incrementar o job para rastrear o item
            .build(); //construir o job
    }
	
	
	//implementacao do passo inicial
	//ponto de partida para quando a tarefa for iniciada. Para implementarmos isso,  um método chamado passoInicial(), que retornará um Step, ou passo, então utilizaremos o StepBuilder(), assim como usamos o JobBuilder() anteriormente.
	//retorna um step
	//sera um bean gerenciado pelo spring
   // ItemReader para ler dados de um arquivo CSV;
	//ItemWriter para escrever dados no nosso banco de dados banco de dados.
	@Bean
	public Step passoInicial(ItemReader<Importacao> reader, ItemWriter<Importacao> writer, JobRepository jobRepository) {
	    return new StepBuilder("passo-inicial", jobRepository)
	        .<Importacao, Importacao>chunk(200, transactionManager)//parametro nome  - passo inicial, e um jobrepository, o chunk sera o tamanbo do chunck, quantos dados sera processado durante a transacao
	        .reader(reader) //leitura
	        .writer(writer) //escrita
	        .processor(processor())
	        .build(); //finalizacao
	}
	
	//As colunas do arquivo estão delimitadas, então indicaremos isso com .delimited() e com .names()
	// cada coluna será identificada: CPF, cliente, nascimento, evento, data, tipo ingresso, valor, conforme indicado na nossa classe de importação.
	//indicaremos o .target(), ou seja, o tipo de destino, o alvo que se transformará em uma classe de importação, e incluiremos .build() para indicar o momento de construção.
	//retornar ItemReader<Importacao>
	//implementacaoo do reader
	@Bean
	public ItemReader<Importacao> reader() {
	    return new FlatFileItemReaderBuilder<Importacao>()//leitura do arquivo cvs, por isso o flatfleitemreadeerbuilder
	        .name("leitura-csv") //nome que damos poara o itemreader
	        .resource(new FileSystemResource("files/dados.csv")) //caminho do arquivo a ser lido
	        .comments("--") //comentario do arquivo, se tiver comentario no arquivo deixar como -- no arquivo csv, neste contexto sera desprezado a linha com comentario
	        .delimited() //colunas delimitados 
	        .delimiter(";") //colunas delimitados 
	        .names("cpf", "cliente", "nascimento", "evento", "data", "tipoIngresso", "valor") //nome de cada coluna
	        //.targetType(Importacao.class)//qual eh o destino, o alvo, o resultado final
	        .fieldSetMapper(new ImportacaoMapper()) //classe que transforma os valores para persistir os dados
	        .build();//buildar, construir
	}
	
	//eh um bean do spring seja gerenciado pelo spring
	//retornar ItemWriter<Importacao>
	//implementacaoo do writer
	@Bean
	public ItemWriter<Importacao> writer(DataSource dataSource) {
	    return new JdbcBatchItemWriterBuilder<Importacao>()//preciso retornar o que vai persistir no banco
	        .dataSource(dataSource) //informacoes do banco de dados
	        .sql( //gerar a query sql, para gravar a informacao
	            "INSERT INTO importacao (cpf, cliente, nascimento, evento, data, tipo_ingresso, valor, hora_importacao, taxa_adm) VALUES" +
	            " (:cpf, :cliente, :nascimento, :evento, :data, :tipoIngresso, :valor, :horaImportacao, :taxaAdm)"
	        )
	        .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())//os parametros vem do bean de leitura
	        .build();//buildar, construir
	}
	
	@Bean
    public ImportacaoProcessor processor() {
        return new ImportacaoProcessor();
    }
	
	@Bean
    public Tasklet moverArquivosTasklet() {
        return (contribution, chunkContext) -> {
            File pastaOrigem = new File("files");
            File pastaDestino = new File("imported-files");

            if (!pastaDestino.exists()) {
                pastaDestino.mkdirs();
            }

            File[] arquivos = pastaOrigem.listFiles((dir, name) -> name.endsWith(".csv"));

            if (arquivos != null) {
                for (File arquivo : arquivos) {
                    File arquivoDestino = new File(pastaDestino, arquivo.getName());
                    if (arquivo.renameTo(arquivoDestino)) {
                        System.out.println("Arquivo movido: " + arquivo.getName());
                    } else {
                        throw new RuntimeException("Não foi possível mover o arquivo: " + arquivo.getName());
                    }
                }
            }
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step moverArquivosStep(JobRepository jobRepository) {
        return new StepBuilder("mover-arquivo", jobRepository)
                .tasklet(moverArquivosTasklet(), transactionManager)
                .build();
    }


}
