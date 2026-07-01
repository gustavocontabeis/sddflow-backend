package com.example.springia.agent.responseapi.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseContentFilterResults {

    private ResponseFilterResult hate;
    private ResponseFilterResult sexual;
    private ResponseFilterResult violence;

    @JsonProperty("self_harm")
    private ResponseFilterResult selfHarm;

    private ResponseFilterResult jailbreak;

    @JsonProperty("protected_material_text")
    private ResponseFilterResult protectedMaterialText;

    @JsonProperty("protected_material_code")
    private ResponseFilterResult protectedMaterialCode;
}

