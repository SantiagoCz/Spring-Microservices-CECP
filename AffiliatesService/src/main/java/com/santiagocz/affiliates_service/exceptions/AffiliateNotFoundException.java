package com.santiagocz.affiliates_service.exceptions;

public class AffiliateNotFoundException extends RuntimeException {
    public AffiliateNotFoundException(String message) {
        super(message);
    }
}
