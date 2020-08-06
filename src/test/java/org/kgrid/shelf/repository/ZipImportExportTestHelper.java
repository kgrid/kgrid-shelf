package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.zeroturnaround.zip.ZipUtil.pack;

public class ZipImportExportTestHelper {
    public static final String SERVICE_YAML_PATH = "service.yaml";
    public static final String DEPLOYMENT_YAML_PATH = "deployment.yaml";
    public static final String PAYLOAD_PATH = "src/index.js";
    public static final String NAAN = "naan";
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String KO_PATH = NAAN + "-" + NAME + "-" + VERSION;
    public static final ArkId ARK_ID = new ArkId(NAAN, NAME, VERSION);
    public static final String SEPARATOR = "/";
    
    public static final byte[] DEPLOYMENT_BYTES =
            ("endpoints:\n  /welcome:\n    artifact: " + PAYLOAD_PATH + "\n    function: welcome\n")
                    .getBytes();
    public static final byte[] SERVICE_BYTES =
            ("paths:\n  /welcome:\n    post:\n      x-kgrid-activation:\n        artifact:\n            - "
                    + PAYLOAD_PATH
                    + "\n")
                    .getBytes();
    public static final byte[] PAYLOAD_BYTES = "function(input){return \"hi\";}".getBytes();

    public static ByteArrayInputStream packZip(byte[] metadata, byte[] deploymentSpec, byte[] serviceSpec, byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<ZipEntrySource> filesToBeZipped = new ArrayList<>();

        if (metadata != null && metadata.length != 0) {
            filesToBeZipped.add(
                    new ByteSource(KO_PATH + SEPARATOR + KoFields.METADATA_FILENAME.asStr(), metadata));
        }
        if (deploymentSpec != null && deploymentSpec.length != 0) {
            filesToBeZipped.add(new ByteSource(KO_PATH + SEPARATOR + DEPLOYMENT_YAML_PATH, deploymentSpec));
        }
        if (serviceSpec != null && serviceSpec.length != 0) {
            filesToBeZipped.add(new ByteSource(KO_PATH + SEPARATOR + SERVICE_YAML_PATH, serviceSpec));
        }
        if (payload != null && payload.length != 0) {
            filesToBeZipped.add(new ByteSource(KO_PATH + SEPARATOR + PAYLOAD_PATH, payload));
        }
        pack(filesToBeZipped.toArray(new ZipEntrySource[filesToBeZipped.size()]), outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public static JsonNode generateMetadata(String serviceYamlPath, String deploymentYamlPath, boolean hasAtId, boolean hasIdentifier, boolean hasVersion, boolean hasType) {
        ObjectNode metadata = new ObjectMapper().createObjectNode();
        if (hasAtId) {
            metadata.put("@id", KO_PATH);
        }
        if (hasType) {
            metadata.put("@type", "koio:KnowledgeObject");
        }
        if (hasIdentifier) {
            metadata.put("identifier", ARK_ID.toString());
        }
        if (hasVersion) {
            metadata.put(KoFields.VERSION.asStr(), VERSION);
        }
        if (deploymentYamlPath != null) {
            metadata.put(KoFields.DEPLOYMENT_SPEC_TERM.asStr(), deploymentYamlPath);
        }
        if (serviceYamlPath != null) {
            metadata.put(KoFields.SERVICE_SPEC_TERM.asStr(), serviceYamlPath);
        }
        return metadata;
    }
}
