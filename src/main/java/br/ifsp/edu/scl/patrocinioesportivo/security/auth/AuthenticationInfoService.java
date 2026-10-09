package br.ifsp.edu.scl.patrocinioesportivo.security.auth;

import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthenticationInfoService {
    public PerfilUsuario getAuthenticatedUserProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Unauthorized user request.");
        }
        return user.getRole() == Role.ADMIN ? PerfilUsuario.DIRETOR_FINANCEIRO : PerfilUsuario.COMERCIAL;
    }

    public UUID getAuthenticatedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new IllegalStateException("Unauthorized user request.");
        var applicationUser = (User) authentication.getPrincipal();
        return applicationUser.getId();
    }
}