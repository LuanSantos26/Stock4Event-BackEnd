package com.stock4event.backend.config;

import com.stock4event.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exige "Authorization: Bearer <token>" nas rotas /api/**, exceto login, cadastro,
 * recuperação de senha e consulta de CEP. O front (src/services/http.ts) já envia o
 * token e trata o 401 encerrando a sessão.
 *
 * Quando o token é válido, o id do usuário fica no atributo {@link #ATRIBUTO_USUARIO_ID}
 * da requisição; os controllers podem lê-lo com @RequestAttribute.
 *
 * Para desligar a exigência (ex.: testar no Swagger sem login), use
 * stock4event.auth.obrigatoria=false no application.properties.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO_ID = "usuarioId";

    /** Rotas abertas: método + prefixo do caminho ("*" = qualquer método). */
    private static final List<String[]> ROTAS_PUBLICAS = List.of(
            new String[]{"POST", "/api/usuarios/login"},
            new String[]{"POST", "/api/usuarios/register"},
            new String[]{"POST", "/api/usuarios/recuperar-senha"},
            new String[]{"POST", "/api/cadastro"},
            new String[]{"GET", "/api/enderecos/cep/"}
    );

    private final JwtService jwtService;

    @Value("${stock4event.auth.obrigatoria:true}")
    private boolean autenticacaoObrigatoria;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extrairToken(request.getHeader("Authorization"));
        boolean tokenValido = token != null && jwtService.isTokenValid(token);
        if (tokenValido) {
            request.setAttribute(ATRIBUTO_USUARIO_ID, jwtService.getUserId(token));
        }

        if (!autenticacaoObrigatoria || tokenValido || !exigeToken(request)) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String mensagem = token == null ? "Faça login para continuar." : "Token inválido ou expirado.";
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }

    private boolean exigeToken(HttpServletRequest request) {
        String metodo = request.getMethod();
        String caminho = request.getRequestURI().substring(request.getContextPath().length());

        // Pré-flight do CORS, imagens (/uploads), Swagger e qualquer coisa fora de /api ficam abertos.
        if (HttpMethod.OPTIONS.matches(metodo) || !caminho.startsWith("/api/")) {
            return false;
        }
        for (String[] rota : ROTAS_PUBLICAS) {
            boolean metodoOk = "*".equals(rota[0]) || rota[0].equalsIgnoreCase(metodo);
            if (metodoOk && caminho.startsWith(rota[1])) {
                return false;
            }
        }
        return true;
    }

    private String extrairToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
