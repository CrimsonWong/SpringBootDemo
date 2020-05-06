package com.myproject.demo.Service;

import java.text.ParseException;
import java.util.Date;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @Title: TokenService
 * @author shaodong
 *
 */

@Service
public class TokenService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private RSAKey privateKey;
    private RSAKey publicKey;

    public String generateToken() throws JOSEException {
        // Generate RSA key (private key)
        try{
            privateKey = new RSAKeyGenerator(2048).keyID("123").generate();
            publicKey = privateKey.toPublicJWK();
        } catch (JOSEException e){
            logger.error("生成私钥公钥失败！",e);
        }
        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(privateKey);
        // Prepare JWT with claims set
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                //.subject("alice")
                //.issuer("https://c2id.com")
                .expirationTime(new Date(new Date().getTime() + 10 * 60 * 1000))
                .build();
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(privateKey.getKeyID()).build(),
                claimsSet);
        // Compute the RSA signature and sign
        signedJWT.sign(signer);
        String s = signedJWT.serialize();
        System.out.println("The token is: " + s);
        return s;
    }

    public boolean validateToken(String s) throws ParseException, JOSEException {
        boolean result = false;
        // On the consumer side, parse the JWS and verify its RSA signature
        try{
            SignedJWT signedJWT = SignedJWT.parse(s);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            // Verify JWT
            result = signedJWT.verify(verifier) && new Date().before(signedJWT.getJWTClaimsSet().getExpirationTime());
        } catch (NullPointerException e){
            logger.error("请先生成令牌！",e);
        }
        return result;
    }
}
