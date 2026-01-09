package de.sty.fileserv.core;

import java.util.List;

public final class MultiAuthenticator implements Authenticator {
    private final List<Authenticator> authenticators;

    public MultiAuthenticator(List<Authenticator> authenticators) {
        this.authenticators = List.copyOf(authenticators);
    }

    public MultiAuthenticator(Authenticator... authenticators) {
        this.authenticators = List.of(authenticators);
    }

    @Override
    public boolean authenticate(String user, String password) {
        for (Authenticator auth : authenticators) {
            if (auth.authenticate(user, password)) {
                return true;
            }
        }
        return false;
    }
}
