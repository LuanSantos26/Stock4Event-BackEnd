package com.quickstock.backend.service;

import com.quickstock.backend.dto.CepConsultaDTO;
import com.quickstock.backend.exception.CadastroException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Service
public class CepConsultaService {

    private final RestClient restClient = RestClient.create();

    public Optional<CepConsultaDTO> buscarPorCep(String cep) {
        String digits = cep != null ? cep.replaceAll("\\D", "") : "";
        if (digits.length() != 8) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "CEP inválido. Informe 8 números.");
        }

        try {
            Map<String, Object> resposta = restClient.get()
                    .uri("https://viacep.com.br/ws/{cep}/json/", digits)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (resposta == null || resposta.containsKey("erro")) {
                return Optional.empty();
            }

            String logradouro = asString(resposta.get("logradouro"));
            String bairro = asString(resposta.get("bairro"));
            String cidade = asString(resposta.get("localidade"));
            String uf = asString(resposta.get("uf"));
            String cepFormatado = formatarCep(digits);

            return Optional.of(new CepConsultaDTO(cepFormatado, logradouro, bairro, cidade, uf));
        } catch (RestClientException ex) {
            throw new CadastroException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o CEP. Tente novamente.");
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString().trim() : "";
    }

    private String formatarCep(String digits) {
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }
}
