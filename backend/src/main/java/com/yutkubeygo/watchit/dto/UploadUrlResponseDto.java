package com.yutkubeygo.watchit.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UploadUrlResponseDto {

    private String uploadUrl;
    private String fileUrl;
    private Map<String, String> formData;
}
