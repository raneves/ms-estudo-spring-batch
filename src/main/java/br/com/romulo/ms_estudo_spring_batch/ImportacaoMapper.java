package br.com.romulo.ms_estudo_spring_batch;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// mapper para transformar o que ele lê em string no formato adequado para ser salvo no banco. Para isso, criaremos uma classe chamada ImportacaoMapper. Esta nova classe implementará outra, uma interface chamada FieldSetMapper.
//A partir deste mapeamento, todas as nossas conversões serão feitas nessa classe. Isso manterá nossa classe de importação limpa, representando exatamente a tabela do banco.
//A classe ImportaçãoMapper, então, implementará a FieldSetMapper de importação. No momento em que fazemos essa implementação, a IDE nos diz que precisamos fazer um override, implementando o método que falta: mapFieldSet. Nele, faremos justamente a leitura da string, definiremos os valores e retornaremos uma importação.
public class ImportacaoMapper implements FieldSetMapper<Importacao>{
	//formas para persisitir com data e data e hora
	 private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	 private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public Importacao mapFieldSet(FieldSet fieldSet) throws BindException {
    	//criar um objeto importacao e atribuir os dados a serem importados
        Importacao importacao = new Importacao();
        importacao.setCpf(fieldSet.readString("cpf"));
        importacao.setCliente(fieldSet.readString("cliente"));
        importacao.setNascimento(LocalDate.parse(fieldSet.readString("nascimento"), dateFormatter));
        importacao.setEvento(fieldSet.readString("evento"));
        importacao.setData(LocalDate.parse(fieldSet.readString("data"), dateFormatter));
        importacao.setTipoIngresso(fieldSet.readString("tipoIngresso"));
        importacao.setValor(fieldSet.readDouble("valor"));
        importacao.setHoraImportacao(LocalDateTime.now());
        return importacao;
    }
}