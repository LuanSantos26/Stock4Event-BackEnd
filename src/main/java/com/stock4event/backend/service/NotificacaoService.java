package com.stock4event.backend.service;

import com.stock4event.backend.dto.NotificacaoDTO;
import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.entity.SolicitacaoCompra;
import com.stock4event.backend.exception.CadastroException;
import com.stock4event.backend.repository.EmpresaRepository;
import com.stock4event.backend.repository.SolicitacaoCompraRepository;
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

    /** Notificações do cliente (compras, promoções, ofertas, boas-vindas). */
    public List<NotificacaoDTO> listar(Long empresaCompradoraId) {
        return listar(empresaCompradoraId, null);
    }

    /**
     * Com perfil "fornecedor", só os pedidos recebidos (novo pedido, etapas, conclusão pelo
     * cliente, cancelamento). Sem perfil ou "cliente", as notificações de quem compra.
     */
    public List<NotificacaoDTO> listar(Long empresaId, String perfil) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));

        if (perfil != null && "fornecedor".equalsIgnoreCase(perfil.trim())) {
            List<NotificacaoDTO> pedidos = notificacoesPedidosRecebidos(empresaId);
            pedidos.sort(Comparator.comparing(NotificacaoDTO::getCriadoEm).reversed());
            return pedidos;
        }

        Long empresaCompradoraId = empresaId;
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
                "Bem-vindo ao Stock4event!",
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
                case "em_separacao" -> "Produto em separação";
                case "abastecendo" -> "Abastecendo o carro";
                case "em_rota" -> "Pedido a caminho";
                case "pronto_retirada" -> "Pronto para retirada";
                case "aprovada" -> "Compra aprovada";
                case "em_transito" -> "Pedido a caminho";
                case "entregue" -> "Entrega concluída";
                case "cancelada" -> "Pedido cancelado";
                default -> "Status da compra";
            };
            String mensagem = switch (s.getStatus() != null ? s.getStatus().toLowerCase() : "aguardando_liberacao") {
                case "aguardando_liberacao" -> "Pedido #" + s.getId() + " com " + fornecedor + " aguardando liberação da distribuidora.";
                case "em_separacao" -> "Pedido #" + s.getId() + ": " + fornecedor + " está separando os produtos.";
                case "abastecendo" -> "Pedido #" + s.getId() + ": " + fornecedor + " está abastecendo o carro de entrega.";
                case "em_rota" -> "Pedido #" + s.getId() + " saiu para rota de entrega (" + fornecedor + "). Ao receber, toque em Concluir entrega.";
                case "pronto_retirada" -> "Pedido #" + s.getId() + " está pronto para retirada em " + fornecedor + ". Ao retirar, toque em Concluir retirada.";
                case "aprovada" -> "Sua solicitação #" + s.getId() + " com " + fornecedor + " foi aprovada.";
                case "em_transito" -> "Pedido #" + s.getId() + " saiu para entrega (" + fornecedor + ").";
                case "entregue" -> "retirada".equals(s.getTipoEntrega())
                        ? "Você concluiu a retirada do pedido #" + s.getId() + "."
                        : "Você concluiu a entrega do pedido #" + s.getId() + ": entregue com sucesso.";
                case "cancelada" -> "Pedido #" + s.getId() + " com " + fornecedor + " foi cancelado.";
                default -> "Solicitação #" + s.getId() + " enviada para " + fornecedor + " — aguardando confirmação.";
            };
            lista.add(new NotificacaoDTO(
                    // O status no id faz cada etapa nova aparecer como não lida.
                    "compra-" + s.getId() + "-" + (s.getStatus() != null ? s.getStatus() : "aguardando_liberacao"),
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

    /** Uma notificação por pedido recebido, no status em que ele está agora. */
    private List<NotificacaoDTO> notificacoesPedidosRecebidos(Long empresaFornecedoraId) {
        List<SolicitacaoCompra> solicitacoes = solicitacaoRepository
                .findByEmpresaFornecedoraIdOrderByCriadoEmDesc(empresaFornecedoraId)
                .stream()
                .limit(15)
                .toList();

        List<NotificacaoDTO> lista = new ArrayList<>();
        for (SolicitacaoCompra s : solicitacoes) {
            String status = s.getStatus() != null ? s.getStatus().toLowerCase() : "aguardando_liberacao";
            String cliente = s.getEmpresaCompradora().getNome();
            boolean retirada = "retirada".equals(s.getTipoEntrega());
            String valor = String.format(new java.util.Locale("pt", "BR"), "R$ %,.2f",
                    s.getValorTotal() != null ? s.getValorTotal() : java.math.BigDecimal.ZERO);
            String titulo = switch (status) {
                case "aguardando_liberacao" -> "Novo pedido #" + s.getId();
                case "em_separacao" -> "Pedido #" + s.getId() + " em separação";
                case "abastecendo" -> "Pedido #" + s.getId() + ": abastecendo o carro";
                case "em_rota" -> "Pedido #" + s.getId() + " a caminho";
                case "pronto_retirada" -> "Pedido #" + s.getId() + " pronto para retirada";
                case "entregue" -> retirada ? "Retirada concluída pelo cliente" : "Entrega concluída pelo cliente";
                case "cancelada" -> "Pedido #" + s.getId() + " cancelado";
                default -> "Pedido #" + s.getId();
            };
            String mensagem = switch (status) {
                case "aguardando_liberacao" -> cliente + " fez um pedido de " + valor
                        + (retirada ? " para retirar no local." : " com entrega.") + " Toque para separar os produtos.";
                case "em_separacao" -> "Separando os produtos de " + cliente + ". Depois, avance para "
                        + (retirada ? "Pronto para retirada." : "Abastecer o carro.");
                case "abastecendo" -> "Carro sendo abastecido para entregar a " + cliente + ".";
                case "em_rota" -> "Saiu para entrega. Aguardando " + cliente + " confirmar o recebimento.";
                case "pronto_retirada" -> "Aguardando " + cliente + " retirar e confirmar a retirada.";
                case "entregue" -> cliente + " confirmou " + (retirada ? "a retirada" : "o recebimento")
                        + " do pedido #" + s.getId() + " (" + valor + ").";
                case "cancelada" -> "O pedido de " + cliente + " (" + valor + ") foi cancelado.";
                default -> "Pedido de " + cliente + ".";
            };
            LocalDateTime quando = s.getConcluidoEm() != null ? s.getConcluidoEm()
                    : (s.getCriadoEm() != null ? s.getCriadoEm() : LocalDateTime.now());
            lista.add(new NotificacaoDTO(
                    "pedido-" + s.getId() + "-" + status,
                    "pedido",
                    titulo,
                    mensagem,
                    null,
                    null,
                    s.getId(),
                    quando
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
