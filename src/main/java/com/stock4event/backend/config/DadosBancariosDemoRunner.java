package com.stock4event.backend.config;

import com.stock4event.backend.entity.DadosBancarios;
import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.repository.DadosBancariosRepository;
import com.stock4event.backend.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Demonstração: fornecedores sem conta cadastrada ganham dados bancários de exemplo,
 * para o checkout mostrar para onde vai o pagamento. O fornecedor troca em Ajustes → Dados bancários.
 * Não mexe em quem já cadastrou.
 */
@Component
@Order(95)
public class DadosBancariosDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosBancariosDemoRunner.class);
    private static final List<String> TIPOS_FORNECEDOR = List.of("DISTRIBUIDOR", "AMBOS");

    private final EmpresaRepository empresaRepository;
    private final DadosBancariosRepository dadosRepository;

    public DadosBancariosDemoRunner(EmpresaRepository empresaRepository, DadosBancariosRepository dadosRepository) {
        this.empresaRepository = empresaRepository;
        this.dadosRepository = dadosRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int criados = 0;
        for (Empresa empresa : empresaRepository.findAll()) {
            if (!TIPOS_FORNECEDOR.contains(empresa.getTipo())) continue;
            if (dadosRepository.findByEmpresaId(empresa.getId()).isPresent()) continue;
            String cnpj = empresa.getCnpj() == null ? "" : empresa.getCnpj().replaceAll("\\D", "");
            if (cnpj.length() != 14) continue;

            DadosBancarios dados = new DadosBancarios();
            dados.setEmpresa(empresa);
            dados.setBancoCodigo("001");
            dados.setBancoNome("Banco do Brasil");
            dados.setAgencia(String.format("%04d", 1000 + (empresa.getId() * 37) % 9000));
            dados.setConta(String.format("%05d-%d", 10000 + (empresa.getId() * 7919) % 90000, empresa.getId() % 10));
            dados.setTipoConta("corrente");
            dados.setTitular(empresa.getNome());
            dados.setDocumento(cnpj);
            dados.setTipoChavePix("cnpj");
            dados.setChavePix(cnpj);
            dadosRepository.save(dados);
            criados++;
        }
        if (criados > 0) log.info("Dados bancários de demonstração criados para {} fornecedor(es).", criados);
    }
}
