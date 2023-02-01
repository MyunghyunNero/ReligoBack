package com.umcreligo.umcback.domain.church.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SignUpChurchMemberParam {
    private Long userId;
    private Long churchId;
    private String name;
    private LocalDate birthday;
    private String phoneNum;
    private String address;
    private String referee;
    private String message;
    private LocalDateTime scheduledDateTime;
}
