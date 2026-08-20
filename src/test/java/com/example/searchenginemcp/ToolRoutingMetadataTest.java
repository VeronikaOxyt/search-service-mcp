package com.example.searchenginemcp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class ToolRoutingMetadataTest {

    @Test
    void savedTemplateListToolHasUnambiguousNameAndDescription() throws Exception {
        Method method = TemplateTools.class.getDeclaredMethod(
                "listSavedQueryTemplates",
                Boolean.class,
                Integer.class,
                Integer.class,
                ToolContext.class);
        String description = method.getAnnotation(Tool.class).description();

        assertTrue(description.contains("SAVED QUERY TEMPLATES"));
        assertTrue(description.contains("шаблон"));
        assertTrue(description.contains("does NOT return data sources"));
    }

    @Test
    void dataSourceListToolExplicitlyRejectsTemplateRequests() throws Exception {
        Method method = ReportingTools.class.getDeclaredMethod(
                "listSearchServiceSources",
                ToolContext.class);
        String description = method.getAnnotation(Tool.class).description();

        assertTrue(description.contains("DATA SOURCES"));
        assertTrue(description.contains("does NOT return saved query templates"));
        assertTrue(description.contains("listSavedQueryTemplates"));
    }
}
