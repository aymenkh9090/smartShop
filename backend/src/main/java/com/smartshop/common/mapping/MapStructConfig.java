package com.smartshop.common.mapping;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Configuration partagee des mappers : un champ cible oublie fait echouer la compilation.
 */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = TextNormalizer.class)
public interface MapStructConfig {
}
