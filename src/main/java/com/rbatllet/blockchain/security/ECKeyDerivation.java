package com.rbatllet.blockchain.security;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import com.rbatllet.blockchain.util.CryptoUtil;

import org.bouncycastle.jce.ECNamedCurveTable;

public class ECKeyDerivation {
    
    // Thread-safe provider registration
    private static final ReentrantReadWriteLock providerLock = new ReentrantReadWriteLock();
    private static volatile boolean bouncyCastleRegistered = false;
    
    // Static initialization to register BouncyCastle at class loading
    static {
        ensureBouncyCastleProvider();
    }
    
    /**
     * Thread-safe BouncyCastle provider registration
     */
    private static void ensureBouncyCastleProvider() {
        if (!bouncyCastleRegistered) {
            providerLock.writeLock().lock();
            try {
                if (!bouncyCastleRegistered) {
                    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                        Security.addProvider(new BouncyCastleProvider());
                    }
                    bouncyCastleRegistered = true;
                }
            } finally {
                providerLock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Derive public key from private key in a thread-safe manner
     */
    public PublicKey derivePublicKeyFromPrivate(PrivateKey privateKey) {
        // Input validation
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        
        if (!CryptoUtil.EC_ALGORITHM.equals(privateKey.getAlgorithm())) {
            throw new IllegalArgumentException("Expected an EC key, but got " + privateKey.getAlgorithm());
        }
        
        ECPrivateKey ecPrivateKey;
        try {
            ecPrivateKey = (ECPrivateKey) privateKey;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Private key is not an EC private key", e);
        }
        
        BigInteger privateKeyValue = ecPrivateKey.getS();
        if (privateKeyValue == null) {
            throw new IllegalArgumentException("Private key does not contain a valid scalar value");
        }
        
        try {
            // Get the domain parameters for the curve
            ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(CryptoUtil.EC_CURVE);
            if (ecSpec == null) {
                throw new IllegalArgumentException("Unsupported curve: " + CryptoUtil.EC_CURVE);
            }
            
            // Validate private key range (0 < d < n)
            if (privateKeyValue.compareTo(BigInteger.ZERO) <= 0 || 
                privateKeyValue.compareTo(ecSpec.getN()) >= 0) {
                throw new IllegalArgumentException("Private key value is outside valid range");
            }
            
            // Perform EC point multiplication: Q = d * G
            org.bouncycastle.math.ec.ECPoint q = ecSpec.getG().multiply(privateKeyValue);
            q = q.normalize();
            
            if (q.isInfinity()) {
                throw new IllegalArgumentException("Derived public key point is invalid");
            }
            
            // Convert to Java ECPoint
            java.security.spec.ECPoint javaECPoint = new java.security.spec.ECPoint(
                q.getAffineXCoord().toBigInteger(), 
                q.getAffineYCoord().toBigInteger()
            );
            
            // Use consistent curve parameters
            ECParameterSpec javaECParameterSpec = convertToJavaECParameterSpec(ecSpec);
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(javaECPoint, javaECParameterSpec);
            
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.EC_ALGORITHM);
            return keyFactory.generatePublic(publicKeySpec);
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to derive public key: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert BouncyCastle parameters to Java ECParameterSpec
     */
    private ECParameterSpec convertToJavaECParameterSpec(ECNamedCurveParameterSpec bcSpec) {
        EllipticCurve curve = new EllipticCurve(
            new ECFieldFp(bcSpec.getCurve().getField().getCharacteristic()),
            bcSpec.getCurve().getA().toBigInteger(),
            bcSpec.getCurve().getB().toBigInteger()
        );
        
        java.security.spec.ECPoint generator = new java.security.spec.ECPoint(
            bcSpec.getG().getAffineXCoord().toBigInteger(),
            bcSpec.getG().getAffineYCoord().toBigInteger()
        );
        
        return new ECParameterSpec(curve, generator, bcSpec.getN(), bcSpec.getH().intValue());
    }
}