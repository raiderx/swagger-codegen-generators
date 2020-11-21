package io.swagger.codegen.v3.generators.java;

import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.CodegenArgument;
import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;
import static io.swagger.codegen.v3.generators.java.JavaJAXRSSpecServerCodegen.GENERATE_POM;
import static io.swagger.codegen.v3.generators.java.JavaJAXRSSpecServerCodegen.INTERFACE_ONLY;

public class JavaJAXRSSpecServerCodegenTest {

    @Test
    public void testProcessOpts() {
        CodegenConfig codegen = new JavaJAXRSSpecServerCodegen();
        codegen.processOpts();

        Assert.assertEquals(codegen.additionalProperties().get(CodegenConstants.ARTIFACT_ID), "swagger-jaxrs-server");
        Assert.assertTrue(codegen.supportingFiles().stream().anyMatch(f -> new SupportingFile("pom.mustache", "", "pom.xml").equals(f)));
        Assert.assertTrue(codegen.supportingFiles().stream().anyMatch(f -> new SupportingFile("RestApplication.mustache", "src/gen/java/io/swagger/api", "RestApplication.java").equals(f)));
    }

    @Test
    public void testProcessOpts2() {
        CodegenConfig codegen = new JavaJAXRSSpecServerCodegen();
        codegen.additionalProperties().put(GENERATE_POM, false);
        codegen.additionalProperties().put(INTERFACE_ONLY, true);
        codegen.processOpts();

        Assert.assertEquals(codegen.additionalProperties().get(CodegenConstants.ARTIFACT_ID), "swagger-jaxrs-client");
        Assert.assertFalse(codegen.supportingFiles().stream().anyMatch(f -> new SupportingFile("pom.mustache", "", "pom.xml").equals(f)));
        Assert.assertFalse(codegen.supportingFiles().stream().anyMatch(f -> new SupportingFile("RestApplication.mustache", "src/geb/java/io/swagger/api", "RestApplication.java").equals(f)));
    }

    @Test
    public void testFromModel() {
        final Schema model = new Schema()
            .description("a sample model")
            .addProperties("id", new IntegerSchema().format(SchemaTypeUtil.INTEGER64_FORMAT))
            .addRequiredItem("id")
            .addRequiredItem("name");
        final CodegenConfig codegen = new JavaJAXRSSpecServerCodegen();
        codegen.processOpts();
        codegen.preprocessOpenAPI(new OpenAPI().components(new Components()));
        final CodegenModel cm = codegen.fromModel("sample", model);

        Assert.assertEquals(cm.name, "sample");
        Assert.assertEquals(cm.classname, "Sample");
        Assert.assertEquals(cm.description, "a sample model");
        Assert.assertEquals(cm.vars.size(), 1);
        Assert.assertEquals(cm.imports.size(), 1);

        final List<CodegenProperty> vars = cm.vars;

        final CodegenProperty property1 = vars.get(0);
        Assert.assertEquals(property1.baseName, "id");
        Assert.assertEquals(property1.getter, "getId");
        Assert.assertEquals(property1.setter, "setId");
        Assert.assertEquals(property1.datatype, "Long");
        Assert.assertEquals(property1.name, "id");
        Assert.assertEquals(property1.defaultValue, "null");
        Assert.assertEquals(property1.baseType, "Long");
        Assert.assertTrue(property1.required);

        Assert.assertTrue(cm.imports.contains("Schema"));
    }

    @Test(description = "verify that parameters are listed in following order: header, query, path, cookie, body (OAS 3.x)")
    public void testParameterOrdersUseOas3() throws Exception {
        File folder = Paths.get(System.getProperty("user.dir"), "generated").toFile();
        if (folder.exists()) {
            folder.delete();
        }
        folder.mkdirs();
        final File output = folder;

        final CodegenConfigurator configurator = new CodegenConfigurator()
            .setLang("jaxrs-spec")
            //.setLang("spring")
            .setInputSpecURL("src/test/resources/3_0_0/petstore.yaml")
            .setOutputDir(output.getAbsolutePath());

        configurator.getAdditionalProperties().put("interfaceOnly", true);
        configurator.getAdditionalProperties().put(CodegenConstants.SOURCE_FOLDER, "src/main/java");

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        new DefaultGenerator().opts(clientOptInput).generate();

        //final File petControllerFile = new File(output, "/src/main/java/io/swagger/api/AdminApi.java");
        //final String content = FileUtils.readFileToString(petControllerFile);

        //Assert.assertTrue(content.contains("ResponseEntity<LocalizedText> updateTest(@Parameter(in = ParameterIn.PATH, description = \"description\", required=true, schema=@Schema()) @PathVariable(\"id\") Long id"));
        //Assert.assertTrue(content.contains("@Parameter(in = ParameterIn.DEFAULT, description = \"Localized Text object containing updated data.\", required=true, schema=@Schema()) @Valid @RequestBody LocalizedText body"));

        //folder.delete();
    }

    /**
     *
     */
    @Test
    public void newTest() {
        generate("spring", "ex0-0");
        generate("jaxrs-spec", "ex0-1");
        generate1("spring", "ex1-0");
        generate1("jaxrs-spec", "ex1-1");
    }

    private void generate(String lang, String folder) {
        File root = prepareFolder(folder);

        final CodegenConfigurator configurator = getConfigurator(lang, root.getAbsolutePath());

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        new DefaultGenerator().opts(clientOptInput).generate();
    }

    private void generate1(String lang, String folder) {
        File root = prepareFolder(folder);

        final CodegenConfigurator configurator = getConfigurator(lang, root.getAbsolutePath());

        configurator.getAdditionalProperties().put("interfaceOnly", true);

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        new DefaultGenerator().opts(clientOptInput).generate();
    }

    private File prepareFolder(String folder) {
        File root = Paths.get(System.getProperty("user.dir"),"..", "..", "IdeaProjects", "generated", folder).toFile();
        if (root.exists()) {
            root.delete();
        }
        root.mkdirs();
        return root;
    }

    private CodegenConfigurator getConfigurator(String lang, String folder) {
        final CodegenConfigurator configurator = new CodegenConfigurator()
            .setLang(lang)
            .setInputSpecURL("src/test/resources/3_0_0/hello.yaml")
            .setOutputDir(folder);
        configurator.getAdditionalProperties().put(CodegenConstants.SOURCE_FOLDER, "src/main/java");
        return configurator;
    }
}
