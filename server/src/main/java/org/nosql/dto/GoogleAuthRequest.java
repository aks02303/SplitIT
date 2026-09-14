package org.nosql.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String name;
    private String email;
}