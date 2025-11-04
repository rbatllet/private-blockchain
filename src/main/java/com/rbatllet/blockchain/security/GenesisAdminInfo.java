package com.rbatllet.blockchain.security;

/**
 * Holds information about the genesis admin (first authorized user).
 *
 * <p>The genesis admin is created automatically on first blockchain initialization
 * when no authorized keys exist in the database. This solves the "bootstrap problem"
 * of how to create the first authorized user.</p>
 *
 * <p><strong>Security:</strong> Genesis admin keys are stored in:</p>
 * <ul>
 *   <li>Private key: {@code ./keys/genesis-admin.private} (owner read/write only)</li>
 *   <li>Public key: {@code ./keys/genesis-admin.public} (plaintext for verification)</li>
 * </ul>
 *
 * @since 1.0.6
 */
public class GenesisAdminInfo {
    private final String publicKey;
    private final String privateKeyPath;
    private final String username;

    /**
     * Creates a new GenesisAdminInfo instance.
     *
     * @param publicKey The genesis admin's public key (string format)
     * @param privateKeyPath Absolute path to the private key file
     * @param username The genesis admin username (typically "GENESIS_ADMIN")
     */
    public GenesisAdminInfo(String publicKey, String privateKeyPath, String username) {
        this.publicKey = publicKey;
        this.privateKeyPath = privateKeyPath;
        this.username = username;
    }

    /**
     * Gets the genesis admin's public key.
     *
     * @return Public key in string format
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Gets the path to the genesis admin's private key file.
     *
     * @return Absolute path to private key file
     */
    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    /**
     * Gets the genesis admin's username.
     *
     * @return Username (typically "GENESIS_ADMIN")
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the key storage location (alias for {@link #getPrivateKeyPath()}).
     *
     * @return Absolute path to private key file
     */
    public String getKeyStorageLocation() {
        return privateKeyPath;
    }
}
