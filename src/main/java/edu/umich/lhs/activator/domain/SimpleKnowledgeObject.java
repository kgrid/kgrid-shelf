package edu.umich.lhs.activator.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;


@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleKnowledgeObject implements KnowledgeObject {

	@JsonInclude(Include.NON_NULL)
	private ObjectNode metadata;

	private String inputMessage;

	private String outputMessage;

	private Payload payload;

	public ObjectNode getMetadata() {
		return metadata;
	}

	public ObjectNode getModelMetadata() {
		return new ObjectMapper().valueToTree(payload);
	}

	public String getInputMessage() {
		return inputMessage;
	}

	public String getOutputMessage() {
		return outputMessage;
	}

	public Payload getPayload() {
		return payload;
	}

	public ArkId getArkId() {
		return new ArkId(metadata.get("arkId").asText());
	}

	public String getVersion() {
		return metadata.get("version").asText();
	}

	@Override
	public String getAdapterType() {
		return payload.getEngineType();
	}

	@Override
	public URI getResourceLocation() {
		try {
			return new URI(payload.getContent());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public URI getServiceLocation() {
		return null;
	}

	public void setInputMessage(String inputMessage) {
		this.inputMessage = inputMessage;
	}

	public URI getBaseMetadataLocation() {
		return null;
	}

	public URI getModelMetadataLocation() {
		return null;
	}

	public void setMetadata(ObjectNode metadata) {
		this.metadata = metadata;
	}

	public void setModelMetadata(ObjectNode modelMetadata) {
		this.metadata.set("models", modelMetadata);
	}

	public void setOutputMessage(String outputMessage) {
		this.outputMessage = outputMessage;
	}

	public void setPayload(Payload payload) {
		this.payload = payload;
	}

	public Payload genPayload(String content, String engineType, String functionName) {
		this.payload = new Payload();
		this.payload.setContent(content);
		this.payload.setEngineType(engineType);
		this.payload.setFunctionName(functionName);
		return this.payload;
	}
	
	@Override
	public String toString() {
		return "SimpleKnowledgeObject [inputMessage=" + inputMessage + ", outputMessage=" + outputMessage + ", payload="
				+ payload + "]";
	}
}
