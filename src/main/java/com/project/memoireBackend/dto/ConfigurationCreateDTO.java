package com.project.memoireBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationCreateDTO {
    @NotBlank(message = "La clé de configuration est obligatoire")
    private String configKey;

    private String configValue;
    private String description;
}
