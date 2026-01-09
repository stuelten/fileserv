package de.sty.fileserv.core;

public final class SimpleAuthenticator implements Authenticator {
    private final String expectedUser;
    private final String expectedPass;

    public SimpleAuthenticator(String expectedUser, String expectedPass) {
        this.expectedUser = expectedUser;
        this.expectedPass = expectedPass;
    }

    @Override
    public boolean authenticate(String user, String password) {
        return expectedUser.equals(user) && expectedPass.equals(password);
    }
}
