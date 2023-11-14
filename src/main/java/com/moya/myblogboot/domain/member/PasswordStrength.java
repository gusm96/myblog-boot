package com.moya.myblogboot.domain.member;

public enum PasswordStrength {
    HIGH_RISK("매우 위험"),
    RISK("위험"),
    SAFE("안전"),
    VERY_SAFE("매우 안전");

    private final String label;
    PasswordStrength(String label) {
        this.label = label;
    }

    public String getLabel(){
        return this.label;
    }
}
