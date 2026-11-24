package com.authoritypro.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authoritypro.platform.runtime.application.BuildRevision;
import com.authoritypro.platform.runtime.application.ModuleCatalogue;
import com.authoritypro.platform.runtime.application.ModuleManifest;
import com.authoritypro.platform.runtime.infrastructure.http.HttpProblems;
import com.authoritypro.platform.runtime.infrastructure.http.InventoryController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class InventoryControllerTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static ModuleManifest manifest() {
        return new ModuleManifest("0.1.0", "access", "0.1.0", "platform-module", "first-party",
                "compiled-first-party", List.of(),
                new ModuleManifest.Capabilities(List.of(), List.of()),
                List.of(new ModuleManifest.Permission("platform.inventory.read", "control-plane")),
                new ModuleManifest.DataOwnership(
                        List.of(new ModuleManifest.OwnedResource("roles", "control-plane"))),
                new ModuleManifest.Ui(List.of()));
    }

    private MockMvc mvc(ModuleCatalogue catalogue) {
        InventoryController controller = new InventoryController(catalogue,
                new BuildRevision("20261124120000"), new HttpProblems(MAPPER));
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(MAPPER))
                .build();
    }

    private static ModuleCatalogue available() {
        return new ModuleCatalogue(List.of(new ModuleCatalogue.Entry(manifest(),
                "sha256:" + "0".repeat(64))), null);
    }

    @Test
    void returnsOnlyTheApprovedInventoryFields() throws Exception {
        MvcResult result = mvc(available()).perform(get("/api/v1/platform/inventory"))
                .andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("\"buildRevision\":\"20261124120000\""), json);
        assertTrue(json.contains("\"id\":\"access\""), json);
        assertTrue(json.contains("\"digest\":\"sha256:" + "0".repeat(64) + "\""), json);
        assertTrue(json.contains("\"lifecycle\":\"REGISTERED\""), json);
        assertTrue(json.contains("\"availabilityReason\":\"COMPILED_FIRST_PARTY\""), json);
        assertTrue(json.contains("\"platform.inventory.read\""), json);
        assertTrue(json.contains("\"dataOwnership\":[{"), json);
        assertFalse(json.contains("unavailableReason"), json);
        assertFalse(json.toLowerCase().contains("password"), json);
        assertFalse(json.contains(":\\"), json);
        assertFalse(result.getResponse().getHeaders("Cache-Control").isEmpty());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, result.getResponse().getContentType());
    }

    @Test
    void unavailableCatalogueFailsClosedWithAProblem() throws Exception {
        MvcResult result = mvc(ModuleCatalogue.unavailable("MALFORMED_JSON"))
                .perform(get("/api/v1/platform/inventory"))
                .andExpect(status().isServiceUnavailable()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(result.getResponse().getContentType().startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE), body);
        assertTrue(body.contains("\"code\":\"INVENTORY_UNAVAILABLE\""), body);
        assertTrue(body.contains("\"traceId\""), body);
        assertFalse(body.contains("MALFORMED_JSON"), body);
        assertFalse(result.getResponse().getHeaders("Cache-Control").isEmpty());
    }

    @Test
    void inventoryIsReadOnly() throws Exception {
        mvc(available()).perform(post("/api/v1/platform/inventory")).andExpect(status().isMethodNotAllowed());
    }
}
