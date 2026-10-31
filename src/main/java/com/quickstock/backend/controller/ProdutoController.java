package com.quickstock.backend.controller;

import com.quickstock.backend.dto.ProdutoRequestDTO;
import com.quickstock.backend.dto.ProdutoResponseDTO;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.Produto;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.ProdutoRepository;
import com.quickstock.backend.service.EstoqueProdutoService;
import com.quickstock.backend.service.ProdutoService;
import com.quickstock.backend.service.ProdutoUploadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
@CrossOrigin(origins = "*")
public class ProdutoController {

    @Autowired private ProdutoRepository repository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ProdutoUploadService uploadService;
    @Autowired private ProdutoService produtoService;
    @Autowired private EstoqueProdutoService estoqueProdutoService;

    @GetMapping
    public List<ProdutoResponseDTO> listar(@RequestParam(required = false) Long empresaId) {
        if (empresaId != null) {
            return repository.findByEmpresaIdAndAtivo(empresaId, 1)
                    .stream()
                    .map(ProdutoResponseDTO::new)
                    .toList();
        }
        return repository.findAll()
                .stream()
                .map(ProdutoResponseDTO::new)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(produto -> ResponseEntity.ok(new ProdutoResponseDTO(produto)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Arquivo de imagem é obrigatório."));
        }

        try {
            String imagemUrl = uploadService.salvarImagem(file);
            return ResponseEntity.ok(Map.of("imagemUrl", imagemUrl));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Não foi possível salvar a imagem."));
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody ProdutoRequestDTO dto) {
        var empresa = empresaRepository.findById(dto.getEmpresaId());
        if (empresa.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Empresa não encontrada."));
        }

        Produto produto = mapToEntity(dto, empresa.get());
        produto.setAtivo(1);
        Produto salvo = repository.save(produto);
        return ResponseEntity.status(201).body(new ProdutoResponseDTO(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProdutoRequestDTO dto) {
        var produtoOpt = repository.findById(id);
        if (produtoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Produto produto = produtoOpt.get();
        if (!produto.getEmpresa().getId().equals(dto.getEmpresaId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Produto não pertence à empresa informada."));
        }

        produto.setNome(dto.getNome());
        produto.setPrecoVenda(dto.getPrecoVenda());
        produto.setUnidade(dto.getUnidade());
        produto.setDescricao(dto.getDescricao());
        produto.setImagemUrl(dto.getImagemUrl());
        if (dto.getEstoque() != null) {
            produto.setEstoque(dto.getEstoque());
        }
        return ResponseEntity.ok(new ProdutoResponseDTO(repository.save(produto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> desativar(@PathVariable Long id,
                                                         @RequestParam(required = false) Long empresaId) {
        produtoService.desativar(id, empresaId);
        return ResponseEntity.ok(Map.of("mensagem", "Produto removido."));
    }

    private Produto mapToEntity(ProdutoRequestDTO dto, Empresa empresa) {
        Produto produto = new Produto();
        produto.setEmpresa(empresa);
        produto.setNome(dto.getNome());
        produto.setPrecoVenda(dto.getPrecoVenda());
        produto.setUnidade(dto.getUnidade());
        produto.setDescricao(dto.getDescricao());
        produto.setImagemUrl(dto.getImagemUrl());
        produto.setEstoque(dto.getEstoque() != null ? dto.getEstoque() : java.math.BigDecimal.ZERO);
        produto.setCodigo(estoqueProdutoService.gerarCodigoCatalogo(empresa.getId()));
        return produto;
    }
}
