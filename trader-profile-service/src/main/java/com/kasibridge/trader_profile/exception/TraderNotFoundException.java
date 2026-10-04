package com.kasibridge.trader_profile.exception;

public class TraderNotFoundException extends RuntimeException{
    public TraderNotFoundException(String message){
        super(message);
    }
}
