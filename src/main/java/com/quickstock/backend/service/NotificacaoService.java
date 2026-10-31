package com.quickstock.backend.service;

import com.quickstock.backend.dto.NotificacaoDTO;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.SolicitacaoCompra;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.SolicitacaoCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NotificacaoService {

    private static final List<String> TIPOS_DISTRIBUIDORA_DESTAQUE = List.of("DISTRIBUIDOR", "AMBOS");
    /** Contas criadas há menos dias que este limite recebem apenas boas-vindas (+ compras reais). */
    private static final int DIAS_CONTA_NOVA = 7;

    @Autowired private SolicitacaoCompraRepository solicitacaoRepository;
    @Autowired private EmpresaRepository empresaRepository;

    public List<NotificacaoDTO> listar(Long empresaCompradoraId) {
        Empresa empresa = empresaRepository.findById(empresaCompradoraId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));

        List<NotificacaoDTO> notificacoes = new ArrayList<>();
        notificacoes.addAll(notificacoesCompras(empresaCompradoraId));

        if (isContaNova(empresa)) {
            notificacoes.add(boasVindas(empresa));
        } else {
            notificacoes.addAll(notificacoesPromocoes(empresaCompradoraId));
            notificacoes.addAll(notificacoesOfertas(empresaCompradoraId));
        }

        notificacoes.sort(Comparator.comparing(NotificacaoDTO::getCriadoEm).reversed());
        return notificacoes;
    }

    private boolean isContaNova(Empresa empresa) {
        if (empresa.getCriadoEm() == null) {
            return true;
        }
        return empresa.getCriadoEm().isAfter(LocalDateTime.now().minusDays(DIAS_CONTA_NOVA));
    }

    private NotificacaoDTO boasVindas(Empresa empresa) {
        String nome = empresa.getNome() != null && !empresa.getNome().isBlank()
                ? empresa.getNome()
                : "sua empresa";
        LocalDateTime criadoEm = empresa.getCriadoEm() != null ? empresa.getCriadoEm() : LocalDateTime.now();
        return new NotificacaoDTO(
                "sistema-boas-vindas",
                "sistema",
                "Bem-vindo ao QuickStock!",
                "Olá, " + nome + "! Sua conta foi criada com sucesso. Explore as distribuidoras em destaque e monte seu estoque.",
                null,
                null,
                null,
                criadoEm
        );
    }

    private List<NotificacaoDTO> notificacoesCompras(Long empresaCompradoraId) {
        List<SolicitacaoCompra> solicitacoes = solicitacaoRepository
                .findByEmpresaCompradoraIdOrderByCriadoEmDesc(empresaCompradoraId)
                .stream()
                .limit(8)
                .toList();

        List<NotificacaoDTO> lista = new ArrayList<>();
        for (SolicitacaoCompra s : solicitacoes) {
            String fornecedor = s.getEmpresaFornecedora().getNome();
            String titulo = switch (s.getStatus() != null ? s.getStatus().toLowerCase() : "aguardando_liberacao") {
                case "aguardando_liberacao" -> "Aguardando liberação";
                case "em_rota" -> "Pedido a caminho";
                case "aprovada" -> "Compra aprovada";
                case "em_transito" -> "Pedido a caminho";
                case "entregue" -> "Entrega concluída";
                case "cancelada" -> "Pedido cancelado";
                default -> "Status da compra";
            };
            String mensagem = switch (s.getStatus() != null ? s.getStatus().toLowerCase() : "aguardando_liberacao") {
                case "aguardando_liberacao" -> "Pedido #" + s.getId() + " com " + fornecedor + " aguardando liberação da distribuidora.";
                case "em_rota" -> "Pedido #" + s.getId() + " saiu para rota de entrega (" + fornecedor + ").";
                case "aprovada" -> "Sua solicitação #" + s.getId() + " com " + fornecedor + " foi aprovada.";
                case "em_transito" -> "Pedido #" + s.getId() + " saiu para entrega (" + fornecedor + ").";
                case "entregue" -> "Pedido #" + s.getId() + " entregue com sucesso.";
                case "cancelada" -> "Solicitação #" + s.getId() + " foi cancelada pelo fornecedor.";
                default -> "Solicitação #" + s.getId() + " enviada para " + fornecedor + " — aguardando confirmação.";
            };
            lista.add(new NotificacaoDTO(
                    "compra-" + s.getId(),
                    "compra",
                    titulo,
                    mensagem,
                    s.getEmpresaFornecedora().getId(),
                    fornecedor,
                    s.getId(),
                    s.getCriadoEm() != null ? s.getCriadoEm() : LocalDateTime.now()
            ));
        }
        return lista;
    }

    private List<NotificacaoDTO> notificacoesPromocoes(Long empresaCompradoraId) {
        List<Empresa> fornecedoras = empresaRepository.findByTipoInAndIdNot(TIPOS_DISTRIBUIDORA_DESTAQUE, empresaCompradoraId);
        String[][] promos = {
                {"10% OFF em Heineken", "Válido até domingo — mínimo 2 fardos."},
                {"Frete grátis", "Compras acima de R$ 500 na região metropolitana."},
                {"Pack Skol -15%", "Promoção de atacado para bares e quiosques."},
                {"Red Bull 2ª unidade -20%", "Ideal para eventos e festas juninas."},
                {"Coca-Cola 2L combo", "Leve 10 garrafas e ganhe 1 fardo de água."},
        };

        List<NotificacaoDTO> lista = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now();
        for (int i = 0; i < Math.min(fornecedoras.size(), promos.length); i++) {
            Empresa f = fornecedoras.get(i);
            lista.add(new NotificacaoDTO(
                    "promo-" + f.getId(),
                    "promocao",
                    promos[i][0],
                    f.getNome() + ": " + promos[i][1],
                    f.getId(),
                    f.getNome(),
                    null,
                    base.minusHours(6L + i * 5)
            ));
        }
        return lista;
    }

    private List<NotificacaoDTO> notificacoesOfertas(Long empresaCompradoraId) {
        List<Empresa> fornecedoras = empresaRepository.findByTipoInAndIdNot(TIPOS_DISTRIBUIDORA_DESTAQUE, empresaCompradoraId);
        if (fornecedoras.size() < 3) return List.of();

        List<NotificacaoDTO> lista = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now();
        String[][] ofertas = {
                {"Oferta relâmpago", "Brahma 600ml a partir de R$ 3,49/un — estoque limitado."},
                {"Nova distribuidora em destaque", "Confira o catálogo completo de cervejas craft."},
                {"Combo festa junina", "Guaraná + Skol + Água com preço especial para revenda."},
        };

        for (int i = 0; i < ofertas.length; i++) {
            Empresa f = fornecedoras.get((i + 3) % fornecedoras.size());
            lista.add(new NotificacaoDTO(
                    "oferta-" + f.getId() + "-" + i,
                    "oferta",
                    ofertas[i][0],
                    f.getNome() + " — " + ofertas[i][1],
                    f.getId(),
                    f.getNome(),
                    null,
                    base.minusDays(1L + i).minusHours(i * 3L)
            ));
        }
        return lista;
    }
}
