package com.quickstock.backend.dto;

import com.quickstock.backend.entity.ChavePixSalva;
import lombok.Getter;

@Getter
public class ChavePixSalvaDTO {

    private final Long id;
    private final Long empresaId;
    private final String apelido;
    private final String tipoChave;
    private final String chaveMascarada;
    private final String ultimosDigitos;

    public ChavePixSalvaDTO(ChavePixSalva chave) {
        this.id = chave.getId();
        this.empresaId = chave.getEmpresa().getId();
        this.apelido = chave.getApelido();
        this.tipoChave = chave.getTipoChave();
        this.chaveMascarada = chave.getChaveMascarada();
        this.ultimosDigitos = chave.getUltimosDigitos();
    }
}
