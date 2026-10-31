package com.quickstock.backend.config;

import com.quickstock.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtService jwtService;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthFilter(jwtService);
        ReflectionTestUtils.setField(filter, "autenticacaoObrigatoria", true);
    }

    private MockHttpServletResponse executar(MockHttpServletRequest req, MockFilterChain chain) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res;
    }

    @Test
    void bloqueiaRotaProtegidaSemToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse res = executar(new MockHttpServletRequest("GET", "/api/produtos"), chain);

        assertEquals(401, res.getStatus());
        assertNull(chain.getRequest(), "não deveria seguir para o controller");
    }

    @Test
    void bloqueiaTokenInvalido() throws Exception {
        when(jwtService.isTokenValid("ruim")).thenReturn(false);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/produtos");
        req.addHeader("Authorization", "Bearer ruim");

        assertEquals(401, executar(req, new MockFilterChain()).getStatus());
    }

    @Test
    void liberaTokenValidoEGuardaUsuario() throws Exception {
        when(jwtService.isTokenValid("bom")).thenReturn(true);
        when(jwtService.getUserId("bom")).thenReturn(42L);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/produtos");
        req.addHeader("Authorization", "Bearer bom");
        MockFilterChain chain = new MockFilterChain();

        assertEquals(200, executar(req, chain).getStatus());
        assertNotNull(chain.getRequest());
        assertEquals(42L, req.getAttribute(JwtAuthFilter.ATRIBUTO_USUARIO_ID));
    }

    @Test
    void liberaRotasPublicas() throws Exception {
        String[][] publicas = {
                {"POST", "/api/usuarios/login"},
                {"POST", "/api/cadastro"},
                {"POST", "/api/usuarios/recuperar-senha"},
                {"GET", "/api/enderecos/cep/01001000"},
                {"GET", "/uploads/produtos/foto.png"},
                {"GET", "/swagger"},
                {"OPTIONS", "/api/produtos"},
        };
        for (String[] rota : publicas) {
            MockFilterChain chain = new MockFilterChain();
            executar(new MockHttpServletRequest(rota[0], rota[1]), chain);
            assertNotNull(chain.getRequest(), rota[0] + " " + rota[1] + " deveria ser pública");
        }
    }

    @Test
    void loginSoEPublicoNoPost() throws Exception {
        // GET /api/usuarios lista todos os usuários: precisa de token.
        assertEquals(401, executar(new MockHttpServletRequest("GET", "/api/usuarios"), new MockFilterChain()).getStatus());
    }

    @Test
    void desligadoPorPropriedadeLiberaTudo() throws Exception {
        ReflectionTestUtils.setField(filter, "autenticacaoObrigatoria", false);
        MockFilterChain chain = new MockFilterChain();
        executar(new MockHttpServletRequest("DELETE", "/api/produtos/1"), chain);
        assertNotNull(chain.getRequest());
    }
}
