package com.myproject.demo.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;

@Service
public class ETagService {
    private  final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String getMD5(String message) {
        String md5str = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] input = message.getBytes();
            byte[] buff = md.digest(input);
            md5str = bytesToHex(buff);

        } catch (Exception e) {
            logger.info("生产eTag出错：", e);
            e.printStackTrace();
        }
        return md5str;
    }

    public String bytesToHex(byte[] bytes) {
        StringBuffer md5str = new StringBuffer();
        int digital;
        for (int i = 0; i < bytes.length; i++) {
            digital = bytes[i];

            if (digital < 0) {
                digital += 256;
            }
            if (digital < 16) {
                md5str.append("0");
            }
            md5str.append(Integer.toHexString(digital));
        }
        return md5str.toString().toLowerCase();
    }
}
