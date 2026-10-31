package com.quickstock.backend.config;

import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.FormaPagamentoSalva;
import com.quickstock.backend.entity.Usuario;
import com.quickstock.backend.repository.FormaPagamentoSalvaRepository;
import com.quickstock.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(91)
public class FormaPagamentoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FormaPagamentoSeedRunner.class);
    private static final List<String> TIPOS_FORNECEDOR = List.of("DISTRIBUIDOR", "PLATAFORMA");

    private static final List<String[]> FORMAS_PADRAO = List.of(
            new String[]{"pix", "Meu PIX"},
            new String[]{"credito", "Cartão de crédito"},
            new String[]{"debito", "Cartão de débito"},
            new String[]{"dinheiro", "Dinheiro"}
    );

    private final UsuarioRepository usuarioRepository;
    private final FormaPagamentoSalvaRepository formaRepository;

    public FormaPagamentoSeedRunner(
            UsuarioRepository usuarioRepository,
            FormaPagamentoSalvaRepository formaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.formaRepository = formaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int criadas = 0;
        for (Usuario usuario : usuarioRepository.findAll()) {
            Empresa empresa = usuario.getEmpresa();
            if (empresa == null) continue;
            if (empresa.getTipo() != null && TIPOS_FORNECEDOR.contains(empresa.getTipo())) continue;
            if (formaRepository.countByEmpresaId(empresa.getId()) > 0) continue;
            criadas += seedParaEmpresa(empresa);
        }
        if (criadas > 0) {
            log.info("FormaPagamento seed: {} formas demo criadas.", criadas);
        }
    }

    private int seedParaEmpresa(Empresa empresa) {
        int count = 0;
        for (int i = 0; i < FORMAS_PADRAO.size(); i++) {
            String[] forma = FORMAS_PADRAO.get(i);
            FormaPagamentoSalva salva = new FormaPagamentoSalva();
            salva.setEmpresa(empresa);
            salva.setTipo(forma[0]);
            salva.setApelido(forma[1]);
            salva.setPrincipal(i == 0);
            formaRepository.save(salva);
            count++;
        }
        return count;
    }
}
