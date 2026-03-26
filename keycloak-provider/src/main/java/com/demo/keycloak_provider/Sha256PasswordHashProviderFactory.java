package com.demo.keycloak_provider;

import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class Sha256PasswordHashProviderFactory implements PasswordHashProviderFactory {

    public static final String PROVIDER_ID = "sha256-password-hash";

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Sha256PasswordHashProvider(PROVIDER_ID);
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}