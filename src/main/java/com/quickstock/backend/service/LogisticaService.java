package com.quickstock.backend.service;

import com.quickstock.backend.dto.CaminhoneiroRequestDTO;
import com.quickstock.backend.dto.CaminhoneiroResponseDTO;
import com.quickstock.backend.dto.EntregaLogisticaRequestDTO;
import com.quickstock.backend.dto.EtapaLogisticaDTO;
import com.quickstock.backend.dto.LogisticaResumoDTO;
import com.quickstock.backend.entity.Caminhoneiro;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.EntregaLogistica;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.CaminhoneiroRepository;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.EntregaLogisticaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogisticaService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] ETAPAS = {
            "Pedido confirmado",
            "Separação no estoque",
            "Carregamento",
            "Em trânsito",
            "Entrega prevista"
    };

    @Autowired private CaminhoneiroRepository caminhoneiroRepository;
    @Autowired private EntregaLogisticaRepository entregaRepository;
    @Autowired private EmpresaRepository empresaRepository;

    @Transactional(readOnly = true)
    public List<CaminhoneiroResponseDTO> listar(Long empresaId) {
        if (empresaId == null) {
            return List.of();
        }
        return caminhoneiroRepository.findByEmpresaIdOrderByIdDesc(empresaId).stream()
                .map(CaminhoneiroResponseDTO::new)
                .toList();
    }

    @Transactional
    public CaminhoneiroResponseDTO criar(CaminhoneiroRequestDTO dto) {
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));

        boolean comEntrega = temEntrega(dto.getEntrega());

        Caminhoneiro caminhoneiro = new Caminhoneiro();
        caminhoneiro.setEmpresa(empresa);
        caminhoneiro.setNome(dto.getNome().trim());
        caminhoneiro.setVeiculo(dto.getVeiculo().trim());
        caminhoneiro.setRota(dto.getRota().trim());
        caminhoneiro.setStatus(comEntrega ? "Carregando" : "Disponível");
        caminhoneiro = caminhoneiroRepository.save(caminhoneiro);

        if (comEntrega) {
            EntregaLogisticaRequestDTO entregaDto = dto.getEntrega();
            EntregaLogistica entrega = new EntregaLogistica();
            entrega.setCaminhoneiro(caminhoneiro);
            entrega.setNomeCliente(entregaDto.getNomeCliente().trim());
            String cidade = entregaDto.getCidade() == null || entregaDto.getCidade().isBlank()
                    ? "Local não informado"
                    : entregaDto.getCidade().trim();
            entrega.setCidade(cidade);
            entrega.setRota(dto.getRota().trim());
            entrega.setStatus("Carregando");
            caminhoneiro.getEntregas().add(entrega);
            caminhoneiro = caminhoneiroRepository.save(caminhoneiro);
        }

        return new CaminhoneiroResponseDTO(caminhoneiro);
    }

    @Transactional(readOnly = true)
    public LogisticaResumoDTO resumo(Long empresaId) {
        if (empresaId == null || !empresaRepository.existsById(empresaId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada.");
        }

        List<EntregaLogistica> ativas = entregaRepository.findAtivasPorEmpresa(empresaId);
        EntregaLogistica entrega = ativas.isEmpty()
                ? primeira(entregaRepository.findRecentesPorEmpresa(empresaId))
                : ativas.get(0);

        if (entrega == null) {
            return LogisticaResumoDTO.vazio();
        }

        return montarResumo(entrega);
    }

    private LogisticaResumoDTO montarResumo(EntregaLogistica entrega) {
        String statusEntrega = entrega.getStatus() == null ? "Carregando" : entrega.getStatus();
        String statusMotorista = entrega.getCaminhoneiro().getStatus();
        boolean atrasado = "Atrasado".equals(statusMotorista) && "Em rota".equals(statusEntrega);

        int concluidas = switch (statusEntrega) {
            case "Entregue" -> 5;
            case "Em rota" -> 4;
            default -> 2;
        };

        String statusTela;
        String tempo;
        if ("Entregue".equals(statusEntrega)) {
            statusTela = "Entregue";
            tempo = "Concluída";
        } else if (atrasado) {
            statusTela = "Atrasado";
            tempo = "Atraso na rota";
        } else if ("Em rota".equals(statusEntrega)) {
            statusTela = "Em trânsito";
            tempo = "2h 15min";
        } else {
            statusTela = "Em separação";
            tempo = "Aguardando carregamento";
        }

        LocalDateTime base = entrega.getCriadoEm() != null ? entrega.getCriadoEm() : LocalDateTime.now();
        int[] minutos = {0, 30, 65, 135, 230};
        List<EtapaLogisticaDTO> etapas = new ArrayList<>();
        for (int i = 0; i < ETAPAS.length; i++) {
            etapas.add(new EtapaLogisticaDTO(
                    ETAPAS[i],
                    i < concluidas,
                    base.plusMinutes(minutos[i]).format(HORA)
            ));
        }

        String rota = entrega.getRota() != null && !entrega.getRota().isBlank()
                ? entrega.getRota()
                : entrega.getCaminhoneiro().getRota();

        return LogisticaResumoDTO.de(
                statusTela,
                tempo,
                "#" + entrega.getId(),
                entrega.getCaminhoneiro().getNome(),
                rota,
                etapas
        );
    }

    private boolean temEntrega(EntregaLogisticaRequestDTO entrega) {
        return entrega != null
                && entrega.getNomeCliente() != null
                && !entrega.getNomeCliente().isBlank();
    }

    private EntregaLogistica primeira(List<EntregaLogistica> lista) {
        return lista == null || lista.isEmpty() ? null : lista.get(0);
    }
}
