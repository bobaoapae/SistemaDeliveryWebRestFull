package restFul.modelo;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class TokenSecurityContext implements SecurityContext {

    private Token token;

    public TokenSecurityContext(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public Principal getUserPrincipal() {
        return token;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
