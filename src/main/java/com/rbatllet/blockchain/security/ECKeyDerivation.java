package com.rbatllet.blockchain.security;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.custom.sec.SecP256R1Curve;
import com.rbatllet.blockchain.util.CryptoUtil;

/**
 * High-performance, thread-safe EC key derivation utility.
 * 
 * This class provides efficient derivation of public keys from private keys
 * using elliptic curve cryptography. It implements proper validation,
 * caching, and error handling for production use.
 * 
 * Features:
 * - Fully thread-safe operations with synchronized provider initialization
 * - Efficient caching of curve parameters using ConcurrentHashMap
 * - Thread-local KeyFactory instances for optimal performance
 * - Comprehensive input validation
 * - Optimized for high-throughput concurrent scenarios
 * - Support for multiple EC curves
 * - Uses BouncyCastle for reliable EC point multiplication
 *
 * Thread Safety:
 * - All public methods are thread-safe
 * - Uses ConcurrentHashMap for curve parameter caching
 * - ThreadLocal KeyFactory instances prevent contention
 * - Synchronized BouncyCastle provider initialization
 * - Stateless design with no mutable shared state
 * 
 * @version 2.1
 */
public class ECKeyDerivation {
    
    // Cache for curve parameters to avoid repeated lookups
    private static final Map<String, ECParameterSpec> CURVE_CACHE = new ConcurrentHashMap<>();
    
    // Cache for KeyFactory instances
    private static final ThreadLocal<KeyFactory> KEY_FACTORY_CACHE = 
        ThreadLocal.withInitial(() -> {
            try {
                return KeyFactory.getInstance(CryptoUtil.EC_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("EC algorithm not available", e);
            }
        });
    
    // Ensure BouncyCastle provider is available (thread-safe registration)
    static {
        synchronized (ECKeyDerivation.class) {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        }
    }
    
    /**
     * Derives a public key from the given private key using elliptic curve point multiplication.
     * 
     * This method performs the mathematical operation Q = d * G where:
     * - Q is the derived public key point
     * - d is the private key scalar
     * - G is the generator point of the curve
     * 
     * @param privateKey the EC private key to derive from
     * @return the corresponding EC public key
     * @throws IllegalArgumentException if the private key is null, not an EC key, 
     *                                  or contains invalid values
     * @throws ECKeyDerivationException if the derivation process fails
     */
    public PublicKey derivePublicKeyFromPrivate(PrivateKey privateKey) {
        validatePrivateKey(privateKey);
        
        try {
            ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
            BigInteger privateKeyValue = ecPrivateKey.getS();
            ECParameterSpec ecParams = ecPrivateKey.getParams();
            
            // Validate private key is in valid range (0 < d < n)
            validatePrivateKeyRange(privateKeyValue, ecParams);
            
            // Use the simplest and most reliable approach:
            // Create the private key and let the crypto system derive the public key
            ECPrivateKeySpec privateSpec = new ECPrivateKeySpec(privateKeyValue, ecParams);
            PrivateKey reconstructedPrivate = KEY_FACTORY_CACHE.get().generatePrivate(privateSpec);
            
            // Now derive the public key using a KeyPair approach
            PublicKey derivedPublic = derivePublicKeySimple(reconstructedPrivate);
            
            return derivedPublic;
            
        } catch (InvalidKeySpecException e) {
            throw new ECKeyDerivationException("Failed to generate public key", e);
        } catch (Exception e) {
            throw new ECKeyDerivationException("Unexpected error during key derivation", e);
        }
    }
    
    /**
     * Derives a public key using a specific curve parameter specification.
     * Useful when you need to derive keys with different curve parameters
     * than those embedded in the private key.
     * 
     * @param privateKey the EC private key
     * @param curveParams the curve parameters to use
     * @return the derived public key
     * @throws IllegalArgumentException if inputs are invalid
     * @throws ECKeyDerivationException if derivation fails
     */
    public PublicKey derivePublicKeyFromPrivate(PrivateKey privateKey, ECParameterSpec curveParams) {
        validatePrivateKey(privateKey);
        if (curveParams == null) {
            throw new IllegalArgumentException("Curve parameters cannot be null");
        }
        
        try {
            ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
            BigInteger privateKeyValue = ecPrivateKey.getS();
            
            validatePrivateKeyRange(privateKeyValue, curveParams);
            
            // Create private key with custom parameters
            ECPrivateKeySpec privateSpec = new ECPrivateKeySpec(privateKeyValue, curveParams);
            PrivateKey reconstructedPrivate = KEY_FACTORY_CACHE.get().generatePrivate(privateSpec);
            
            return derivePublicKeySimple(reconstructedPrivate);
            
        } catch (InvalidKeySpecException e) {
            throw new ECKeyDerivationException("Failed to generate public key with custom parameters", e);
        } catch (Exception e) {
            throw new ECKeyDerivationException("Unexpected error during key derivation with custom parameters", e);
        }
    }
    
    /**
     * Simple and reliable public key derivation method.
     * Uses BouncyCastle's proven EC point multiplication.
     */
    private PublicKey derivePublicKeySimple(PrivateKey privateKey) throws Exception {
        ECPrivateKey ecPrivate = (ECPrivateKey) privateKey;
        ECParameterSpec params = ecPrivate.getParams();
        BigInteger privateValue = ecPrivate.getS();
        ECPoint generator = params.getGenerator();
        
        // Use BouncyCastle for reliable EC point multiplication
        return deriveUsingBouncyCastle(privateValue, generator, params);
    }
    
    /**
     * Uses BouncyCastle's EC point operations for reliable point multiplication.
     */
    private PublicKey deriveUsingBouncyCastle(BigInteger privateValue, ECPoint generator, ECParameterSpec params) throws Exception {
        // Convert Java EC parameters to BouncyCastle format
        EllipticCurve curve = params.getCurve();
        ECFieldFp field = (ECFieldFp) curve.getField();
        BigInteger p = field.getP();
        BigInteger a = curve.getA();
        BigInteger b = curve.getB();
        
        // Create BouncyCastle curve using modern API
        ECCurve bcCurve = new SecP256R1Curve();
        
        // If we need a custom curve, use the direct constructor with all parameters
        if (!isStandardCurve(params)) {
            bcCurve = new ECCurve.Fp(p, a, b, params.getOrder(), BigInteger.valueOf(params.getCofactor()));
        }
        
        // Convert generator point to BouncyCastle format
        org.bouncycastle.math.ec.ECPoint bcGenerator = bcCurve.createPoint(generator.getAffineX(), generator.getAffineY());
        
        // Perform point multiplication: Q = d * G
        org.bouncycastle.math.ec.ECPoint bcPublicPoint = bcGenerator.multiply(privateValue);
        
        // Normalize the point to get affine coordinates
        bcPublicPoint = bcPublicPoint.normalize();
        
        // Convert back to Java format
        BigInteger publicX = bcPublicPoint.getAffineXCoord().toBigInteger();
        BigInteger publicY = bcPublicPoint.getAffineYCoord().toBigInteger();
        java.security.spec.ECPoint javaPublicPoint = new java.security.spec.ECPoint(publicX, publicY);
        
        // Create the public key spec and generate the key
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(javaPublicPoint, params);
        return KEY_FACTORY_CACHE.get().generatePublic(publicKeySpec);
    }
    
    /**
     * Verifies that a private key and public key form a valid key pair.
     * 
     * @param privateKey the private key
     * @param publicKey the public key
     * @return true if they form a valid pair, false otherwise
     * @throws IllegalArgumentException if either key is null or not an EC key
     */
    public boolean verifyKeyPair(PrivateKey privateKey, PublicKey publicKey) {
        validatePrivateKey(privateKey);
        validatePublicKey(publicKey);
        
        try {
            PublicKey derivedPublicKey = derivePublicKeyFromPrivate(privateKey);
            return areKeysEqual(publicKey, derivedPublicKey);
        } catch (ECKeyDerivationException e) {
            return false;
        }
    }
    
    /**
     * Gets the curve parameters for a given curve name, with caching for performance.
     * 
     * @param curveName the name of the curve (e.g., "secp256r1")
     * @return the curve parameters
     * @throws IllegalArgumentException if the curve is not supported
     */
    public ECParameterSpec getCurveParameters(String curveName) {
        if (curveName == null) {
            throw new IllegalArgumentException("Curve name cannot be null");
        }
        
        return CURVE_CACHE.computeIfAbsent(curveName, this::loadCurveParameters);
    }
    
    /**
     * Validates that a point lies on the specified elliptic curve.
     * 
     * @param point the point to validate
     * @param curveParams the curve parameters
     * @return true if the point is on the curve, false otherwise
     */
    public boolean isPointOnCurve(ECPoint point, ECParameterSpec curveParams) {
        if (point == null) {
            return false;
        }
        
        if (point.equals(ECPoint.POINT_INFINITY)) {
            return false; // Point at infinity is not considered a valid finite point
        }
        
        try {
            EllipticCurve curve = curveParams.getCurve();
            BigInteger x = point.getAffineX();
            BigInteger y = point.getAffineY();
            BigInteger p = ((ECFieldFp) curve.getField()).getP();
            BigInteger a = curve.getA();
            BigInteger b = curve.getB();
            
            // Verify: y² ≡ x³ + ax + b (mod p)
            BigInteger leftSide = y.multiply(y).mod(p);
            BigInteger rightSide = x.multiply(x).multiply(x)
                .add(a.multiply(x))
                .add(b)
                .mod(p);
            
            return leftSide.equals(rightSide);
        } catch (Exception e) {
            // If we can't validate, assume invalid
            return false;
        }
    }
    
    // Private helper methods
    
    private void validatePrivateKey(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        
        if (!CryptoUtil.EC_ALGORITHM.equals(privateKey.getAlgorithm())) {
            throw new IllegalArgumentException(
                String.format("Expected EC key, but got %s", privateKey.getAlgorithm()));
        }
        
        if (!(privateKey instanceof ECPrivateKey)) {
            throw new IllegalArgumentException("Private key must be an instance of ECPrivateKey");
        }
        
        ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
        if (ecPrivateKey.getS() == null) {
            throw new IllegalArgumentException("Private key scalar value is null");
        }
        
        if (ecPrivateKey.getParams() == null) {
            throw new IllegalArgumentException("Private key curve parameters are null");
        }
    }
    
    private void validatePublicKey(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        
        if (!CryptoUtil.EC_ALGORITHM.equals(publicKey.getAlgorithm())) {
            throw new IllegalArgumentException(
                String.format("Expected EC key, but got %s", publicKey.getAlgorithm()));
        }
        
        if (!(publicKey instanceof ECPublicKey)) {
            throw new IllegalArgumentException("Public key must be an instance of ECPublicKey");
        }
    }
    
    private void validatePrivateKeyRange(BigInteger privateKeyValue, ECParameterSpec ecParams) {
        BigInteger order = ecParams.getOrder();
        
        if (privateKeyValue.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Private key must be positive");
        }
        
        if (privateKeyValue.compareTo(order) >= 0) {
            throw new IllegalArgumentException("Private key must be less than curve order");
        }
    }
    
    private ECParameterSpec loadCurveParameters(String curveName) {
        try {
            // Use Java's standard curve parameters when possible
            AlgorithmParameters params = AlgorithmParameters.getInstance(CryptoUtil.EC_ALGORITHM);
            params.init(new ECGenParameterSpec(curveName));
            return params.getParameterSpec(ECParameterSpec.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported or invalid curve: " + curveName, e);
        }
    }
    
    private boolean areKeysEqual(PublicKey key1, PublicKey key2) {
        if (!(key1 instanceof ECPublicKey) || !(key2 instanceof ECPublicKey)) {
            return false;
        }
        
        ECPublicKey ecKey1 = (ECPublicKey) key1;
        ECPublicKey ecKey2 = (ECPublicKey) key2;
        
        ECPoint point1 = ecKey1.getW();
        ECPoint point2 = ecKey2.getW();
        
        return point1.getAffineX().equals(point2.getAffineX()) &&
               point1.getAffineY().equals(point2.getAffineY());
    }
    
    /**
     * Custom exception for key derivation errors.
     */
    public static class ECKeyDerivationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public ECKeyDerivationException(String message) {
            super(message);
        }
        
        public ECKeyDerivationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Check if the curve parameters match a standard curve (P-256)
     * @param params The EC parameters to check
     * @return true if it's a standard curve, false otherwise
     */
    private static boolean isStandardCurve(ECParameterSpec params) {
        // Check if this is the P-256 curve by comparing the field size
        EllipticCurve curve = params.getCurve();
        ECFieldFp field = (ECFieldFp) curve.getField();
        BigInteger p = field.getP();
        
        // P-256 prime: 2^256 - 2^224 + 2^192 + 2^96 - 1
        BigInteger p256Prime = new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853951");
        
        return p.equals(p256Prime);
    }
}
