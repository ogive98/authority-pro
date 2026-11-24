package com.authoritypro.platform.runtime.infrastructure.manifest;

import com.authoritypro.platform.runtime.application.BuildRevision;
import com.authoritypro.platform.runtime.application.ModuleCatalogue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ModuleCatalogueConfiguration {

    @Bean
    ModuleCatalogue compiledModuleCatalogue() {
        return new ClasspathModuleCatalogueLoader().load();
    }

    @Bean
    BuildRevision buildRevision() {
        return BuildRevisionReader.read();
    }
}
