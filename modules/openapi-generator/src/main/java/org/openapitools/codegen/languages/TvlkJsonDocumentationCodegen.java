package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TvlkJsonDocumentationCodegen extends DefaultCodegen implements CodegenConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(TvlkJsonDocumentationCodegen.class);
    public static final String PROJECT_NAME = "projectName";
    public static final String OUTPUT_NAME = "outputFile";

    protected String outputFile = "tvlk-doc.json";

    @Override
    public CodegenType getTag() {
        return CodegenType.DOCUMENTATION;
    }

    public String getName() {
        return "tvlk-json";
    }

    public String getHelp() {
        return "Generates a tvlk-json documentation.";
    }

    public TvlkJsonDocumentationCodegen() {
        super();

        modifyFeatureSet(features -> features
                .documentationFeatures(EnumSet.allOf(DocumentationFeature.class))
                .dataTypeFeatures(EnumSet.allOf(DataTypeFeature.class))
                .wireFormatFeatures(EnumSet.allOf(WireFormatFeature.class))
                .securityFeatures(EnumSet.allOf(SecurityFeature.class))
                .globalFeatures(EnumSet.allOf(GlobalFeature.class))
                .parameterFeatures(EnumSet.allOf(ParameterFeature.class))
                .schemaSupportFeatures(EnumSet.allOf(SchemaSupportFeature.class))
        );

        outputFolder = "generated-code" + File.separator + "tvlk-json";
        embeddedTemplateDir = templateDir = "tvlk-json-documentation";
        cliOptions.add(CliOption.newString(OUTPUT_NAME, "Output filename").defaultValue(outputFile));
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        OpenAPI openAPI = ((OpenAPI) objs.get("openAPI"));
        Paths currentPath = openAPI.getPaths();
        Set<String> pathKeys = currentPath.keySet();
        Paths resultPath = new Paths();
        for (String key: pathKeys) {
            PathItem item = currentPath.get(key);
            Content itemContent = item.getPost().getRequestBody().getContent();

            for(MediaType mediaType: itemContent.values()) {
                Schema schema = mediaType.getSchema();

                String tempRef = schema.get$ref();

                schema.setName("inline_object");
                schema.setType("object");
                schema.$ref(null);
                schema.setProperties(createTravelokaProperties(tempRef));
            }

            resultPath.addPathItem(key, item);
        }
        openAPI.setPaths(resultPath);

        generateJSONSpecFile(objs);
        return super.postProcessSupportingFileData(objs);
    }

    LinkedHashMap<String, Schema> createTravelokaProperties(String ref) {
        StringSchema clientInterface = new StringSchema();
        clientInterface.example("mobile-android");

        Schema data = new Schema();
        data.$ref(ref);

        Schema context = generateContextSchema();

        ArraySchema field = new ArraySchema();
        field.items(new ObjectSchema());
        field.example(new String[0]);

        LinkedHashMap<String, Schema> result = new LinkedHashMap<>();
        result.put("clientInterface", clientInterface);
        result.put("context", context);
        result.put("data", data);
        result.put("fields", field);


        return result;
    }

    private Schema generateContextSchema() {
        StringSchema nonce = new StringSchema();
        nonce.setExample("nonce");

        StringSchema tvLifeTime = new StringSchema();
        tvLifeTime.setExample("LIFE_TIME_KEY_HERE");

        StringSchema tvSession = new StringSchema();
        tvSession.setExample("SESSION_KEY_HERE");

        LinkedHashMap<String, Schema> properties = new LinkedHashMap<>();
        properties.put("nonce", nonce);
        properties.put("tvLifeTime", tvLifeTime);
        properties.put("tvSession", tvSession);

        Schema contextSchema = new Schema();
        contextSchema.setProperties(properties);
        contextSchema.setType("object");

        return contextSchema;
    }

    @Override
    public void processOpts() {
        super.processOpts();
        if (additionalProperties.containsKey(OUTPUT_NAME)) {
            outputFile = additionalProperties.get(OUTPUT_NAME).toString();
        }
        LOGGER.info("Output file [outputFile={}]", outputFile);
        supportingFiles.add(new SupportingFile("openapi.mustache", outputFile));
    }

    @Override
    public String escapeQuotationMark(String input) {
        // just return the original string
        return input;
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        // just return the original string
        return input;
    }
}
