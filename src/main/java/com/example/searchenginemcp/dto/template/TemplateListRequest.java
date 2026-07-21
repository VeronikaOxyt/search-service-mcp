package com.example.searchenginemcp.dto.template;

import java.util.List;

public record TemplateListRequest(
        List<TemplateSort> sort,
        boolean sortCount,
        boolean isPersonal,
        int limit,
        int offset) {

    public static TemplateListRequest of(boolean personal, int limit, int offset) {
        return new TemplateListRequest(
                List.of(new TemplateSort("srcTable", false)),
                false,
                personal,
                limit,
                offset);
    }
}
