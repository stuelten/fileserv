package de.sty.fileserv.core;

public interface Authenticator {
    boolean authenticate(String user, String password);
}
