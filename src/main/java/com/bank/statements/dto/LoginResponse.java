package com.bank.statements.dto;

public record LoginResponse(String token, Long userId, String name, String roles) {
}
