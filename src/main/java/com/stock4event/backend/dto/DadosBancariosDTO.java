package com.stock4event.backend.dto;

import com.stock4event.backend.entity.DadosBancarios;
import lombok.Getter;

/** Conta de recebimento de um fornecedor. Formato do front em src/types/dadosBancarios.ts. */
@Getter
public class DadosBancariosDTO {

    private final Long empresaId;
    private final String empresaNome;
    private final String bancoCodigo;
    private final String bancoNome;
    private final String agencia;
    private final String conta;
    private final String tipoConta;
    private final String titular;
    private final String documento;
    private final String tipoChavePix;
    private final String chavePix;

    public DadosBancariosDTO(DadosBancarios dados) {
        this.empresaId = dados.getEmpresa().getId();
        this.empresaNome = dados.getEmpresa().getNome();
        this.bancoCodigo = dados.getBancoCodigo();
        this.bancoNome = dados.getBancoNome();
        this.agencia = dados.getAgencia();
        this.conta = dados.getConta();
        this.tipoConta = dados.getTipoConta();
        this.titular = dados.getTitular();
        this.documento = dados.getDocumento();
        this.tipoChavePix = dados.getTipoChavePix();
        this.chavePix = dados.getChavePix();
    }
}
