package com.quickstock.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class ProdutoUploadService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp",
            "image/heic", "image/heif", "application/octet-stream"
    );
    private static final long TAMANHO_MAX_BYTES = 10 * 1024 * 1024;

    @Value("${upload.dir:uploads/produtos}")
    private String uploadDir;

    public String salvarImagem(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de imagem é obrigatório.");
        }

        if (file.getSize() > TAMANHO_MAX_BYTES) {
            throw new IllegalArgumentException("Imagem deve ter no máximo 10 MB.");
        }

        String contentType = file.getContentType();
        String nomeOriginal = file.getOriginalFilename();
        if (!tipoPermitido(contentType, nomeOriginal)) {
            throw new IllegalArgumentException("Formato de imagem não suportado. Use JPEG, PNG ou WebP.");
        }

        String extensao = resolverExtensao(contentType, nomeOriginal);

        Path diretorio = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(diretorio);

        String nomeArquivo = UUID.randomUUID() + "." + extensao;
        Path destino = diretorio.resolve(nomeArquivo);
        Files.copy(file.getInputStream(), destino);

        return "/uploads/produtos/" + nomeArquivo;
    }

    private boolean tipoPermitido(String contentType, String nomeOriginal) {
        if (contentType != null && TIPOS_PERMITIDOS.contains(contentType)) {
            return true;
        }
        if (nomeOriginal == null) {
            return false;
        }
        String nome = nomeOriginal.toLowerCase();
        return nome.endsWith(".jpg") || nome.endsWith(".jpeg")
                || nome.endsWith(".png") || nome.endsWith(".webp")
                || nome.endsWith(".heic") || nome.endsWith(".heif");
    }

    private String resolverExtensao(String contentType, String nomeOriginal) {
        if (nomeOriginal != null) {
            String nome = nomeOriginal.toLowerCase();
            if (nome.endsWith(".png")) return "png";
            if (nome.endsWith(".webp")) return "webp";
            if (nome.endsWith(".jpg") || nome.endsWith(".jpeg")) return "jpg";
            if (nome.endsWith(".heic") || nome.endsWith(".heif")) return "jpg";
        }

        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
