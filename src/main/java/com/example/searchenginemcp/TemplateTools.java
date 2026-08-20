package com.example.searchenginemcp;

import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListResult;
import com.example.searchenginemcp.service.TemplateExecutionService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TemplateTools {

    private final TemplateExecutionService templateService;

    public TemplateTools(TemplateExecutionService templateService) {
        this.templateService = templateService;
    }

    @Tool(description = """
            Lists SAVED QUERY TEMPLATES available to the current user.
            Use this for requests containing template, saved query or шаблон,
            including requests to find, inspect or execute a template.
            This tool does NOT return data sources, databases or topology.
            Never substitute listSearchServiceSources for this tool.
            The result contains template IDs used by the other template tools.
            """)
    public TemplateListResult listSavedQueryTemplates(
            @ToolParam(
                    description = "True for personal templates, false for shared templates.",
                    required = false)
            Boolean personal,
            @ToolParam(
                    description = "Number of templates to return, from 1 to 50. Defaults to 20.",
                    required = false)
            Integer limit,
            @ToolParam(
                    description = "Zero-based pagination offset. Defaults to 0.",
                    required = false)
            Integer offset,
            ToolContext toolContext) {
        return templateService.listTemplates(
                Boolean.TRUE.equals(personal),
                limit,
                offset,
                McpRequestAuthorization.requireBearerToken(toolContext));
    }

    @Tool(description = """
            Returns the execution parameters for a saved query template.
            Call this after selecting a template and before executing it.
            Every returned parameter is an unlocked filter with an empty value
            and must be supplied before execution.
            """)
    public TemplateExecutionSchema getQueryTemplateParameters(
            @ToolParam(description = "UUID of the saved query template.")
            UUID templateId,
            ToolContext toolContext) {
        return templateService.getExecutionSchema(
                templateId,
                McpRequestAuthorization.requireBearerToken(toolContext));
    }

    @Tool(description = """
            Starts asynchronous execution of a saved query template.
            Get the parameter schema first and supply every required parameter.
            Every parameter value is passed as an array of strings.
            The result contains resultId and queryType; pass both to getQueryResult.
            """)
    public QueryExecution executeQueryTemplate(
            @ToolParam(description = "UUID of the saved query template.")
            UUID templateId,
            @ToolParam(description = """
                    Values keyed by parameter key from the template schema.
                    Each value must be an array of non-blank strings.
                    """)
            Map<String, List<String>> parameters,
            ToolContext toolContext) {
        return templateService.execute(
                templateId,
                parameters,
                McpRequestAuthorization.requireBearerToken(toolContext));
    }

    @Tool(description = """
            Checks an asynchronous query and returns one page when it is ready.
            State PENDING means the backend returned HTTP 425 or 426; call this tool again later.
            State READY contains the result rows.
            At most 100 rows can be returned in one call.
            Use metaInfo.count and truncated to tell the user when more rows exist.
            """)
    public QueryResult getQueryResult(
            @ToolParam(description = "Result UUID returned by executeQueryTemplate.")
            UUID resultId,
            @ToolParam(description = "QUERY or CROSS, returned by executeQueryTemplate.")
            QueryType queryType,
            @ToolParam(
                    description = "Zero-based row offset. Defaults to 0.",
                    required = false)
            Integer offset,
            @ToolParam(
                    description = "Number of rows to return, from 1 to 100. Defaults to 20.",
                    required = false)
            Integer limit,
            ToolContext toolContext) {
        return templateService.getResult(
                resultId,
                queryType,
                offset,
                limit,
                McpRequestAuthorization.requireBearerToken(toolContext));
    }
}
