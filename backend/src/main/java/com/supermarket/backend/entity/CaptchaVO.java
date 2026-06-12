package com.supermarket.backend.entity;

import lombok.Data;

@Data
public class CaptchaVO {
    /** CAPTCHA 唯一标识 */
    private String captchaId;
    /** 算式文本，如 "8 + 3 = ?" */
    private String question;

    public CaptchaVO(String captchaId, String question) {
        this.captchaId = captchaId;
        this.question = question;
    }
}
