package com.example.searchenginemcp;

import com.example.searchenginemcp.dto.template.QueryExecution;
import com.example.searchenginemcp.dto.template.QueryResult;
import com.example.searchenginemcp.dto.template.QueryType;
import com.example.searchenginemcp.dto.template.TemplateExecutionSchema;
import com.example.searchenginemcp.dto.template.TemplateListResult;
import com.example.searchenginemcp.service.TemplateExecutionService;
import java.util.Map;
import java.util.UUID;
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
            Lists saved query templates available to the current user.
            Use this first to find a template that matches the user's request.
            The result contains template IDs used by the other template tools.
            """)
    public TemplateListResult listQueryTemplates(
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
            Integer offset) {
        return templateService.listTemplates(Boolean.TRUE.equals(personal), limit, offset);
    }

    @Tool(description = """
            Returns the execution parameters for a saved query template.
            Call this after selecting a template and before executing it.
            Parameters marked required have no value in the template and must be supplied.
            Optional parameters already have a default value and may be overridden.
            """)
    public TemplateExecutionSchema getQueryTemplateParameters(
            @ToolParam(description = "UUID of the saved query template.")
            UUID templateId) {
        return templateService.getExecutionSchema(templateId);
    }

    @Tool(description = """
            Starts asynchronous execution of a saved query template.
            Get the parameter schema first and supply every required parameter.
            DATE values use YYYY-MM-DD, DATETIME values use ISO-8601,
            and parameters marked multiple are passed as arrays.
            The result contains resultId and queryType; pass both to getQueryResult.
            """)
    public QueryExecution executeQueryTemplate(
            @ToolParam(description = "UUID of the saved query template.")
            UUID templateId,
            @ToolParam(description = "Template version returned by getQueryTemplateParameters.")
            long templateVersion,
            @ToolParam(description = "Values keyed by parameter key from the template schema.")
            Map<String, Object> parameters) {
        return templateService.execute(templateId, templateVersion, parameters);
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
            Integer limit) {
        return templateService.getResult(resultId, queryType, offset, limit);
    }
}
