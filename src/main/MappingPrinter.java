package main;

import com.fasterxml.jackson.databind.JsonNode;

public class MappingPrinter {

	static final String REST_URL = "http://data.bioontology.org";
	static final String API_KEY = "0d2f26ee-2c64-412d-81e2-e7e249994d86";

	public static void main(String[] args) {

		String exampleOntology = "ICF";

		//String mappings_string = ListOntologies.get(REST_URL + "/ontologies/ICF/mappings");
		String mappings_string = DataHandler.get(REST_URL + "/ontologies/" + exampleOntology + "/mappings");
		JsonNode mappings = DataHandler.jsonToNode(mappings_string);

		System.out.println("URL: " + mappings_string + "\n");
		System.out.println("Mappings: " + mappings);

	}

}