package com.demo.keycloak_provider;

import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Sha256PasswordHashProvider implements PasswordHashProvider {

    private final String providerId;

    public Sha256PasswordHashProvider(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        return providerId.equals(credential.getPasswordCredentialData().getAlgorithm());
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        String salt = generateSalt();
        String hash = hash(rawPassword, salt);
        return PasswordCredentialModel.createFromValues(providerId, salt.getBytes(), iterations, hash);
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        byte[] saltBytes = credential.getPasswordSecretData().getSalt();
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String storedHash = credential.getPasswordSecretData().getValue();
        return hash(rawPassword, salt).equals(storedHash);
    }

    @Override
    public void close() {}

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = salt + password;
            byte[] hashBytes = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}