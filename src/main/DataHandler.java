package main;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import main.MatchKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;


public class DataHandler {

    static Map<String, ArrayList<String>> id_map_rdf = new HashMap<String, ArrayList<String>>();
    
    static Map<String, ArrayList<String>> dec_map = new HashMap<String, ArrayList<String>>();
    static Map<String, ArrayList<String>> anc_map = new HashMap<String, ArrayList<String>>();

    static Map<MatchKey, Integer> silver_mappings = new HashMap<>();

    static final String REST_URL = "http://data.bioontology.org";
    static final String API_KEY = "0d2f26ee-2c64-412d-81e2-e7e249994d86";
    static final ObjectMapper mapper = new ObjectMapper();

    //TODO add javadoc to the functions

    static JsonNode jsonToNode(String json) {
        JsonNode root = null;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    static String get(String urlToGet) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlToGet);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
            conn.setRequestProperty("Accept", "application/json");
            rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    static OntModel readRDFfile(String filename,String format) {
    	OntModel model;
    	if(format.equals("rdf")) {
    		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    	}else {
    		System.setProperty("entityExpansionLimit", "100000000");
    		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
    		
    	}
        model.read(filename);

        return model;

    }

    static void fetchMappings(List<String> ontologies,String filepath) throws FileNotFoundException {
        String class_1;
        ArrayList<String> class_2;
        ArrayList<String> prev;
        if(filepath.equals("")) {
        	for (int i = 0; i < ontologies.size(); i++) {
            	for (int j = i + 1; i < ontologies.size(); i++) {
                	if (i != j) {
                    	JsonNode mappings = jsonToNode(get(REST_URL + "/mappings?ontologies=" + ontologies.get(i) + "," + ontologies.get(j)));
                    	while (true) {
                        	for (int k = 0; k < mappings.get("collection").size(); k++) {
                            	class_2 = new ArrayList<String>();
                            	class_1 = mappings.get("collection").get(k).get("classes").get(0).findValue("@id").asText();

                            	class_2.add(mappings.get("collection").get(k).get("classes").get(1).findValue("@id").asText());

                            	prev = id_map_rdf.put(class_1, class_2);

                            	if (prev != null) {
                                	prev.add(mappings.get("collection").get(k).get("classes").get(1).findValue("@id").asText());
                                	id_map_rdf.put(class_1, prev);
                            	}
                        	}
                        	if (!mappings.get("links").get("nextPage").isNull()) {
                            	mappings = getNextpage(mappings);
                        	} else {
                            	break;
                        	}
                        
                    	}
                	}
            	}
        	}
        } else {
        	BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String line;
            String[] ids = new String[2];
            ArrayList<String> tmp;
            try {
                ids = new String[2];
                while ((line = reader.readLine()) != null) {
                    ids = line.split(" ");
                    tmp = new ArrayList<String>();
                    tmp.add(ids[1]);
                    id_map_rdf.put(ids[0], tmp);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

 /*   private static void fetchDecendants() throws IOException {

        int c = 0;
        File dec_file = new File("decendants.txt");
        if (dec_file.createNewFile()) {
            System.out.println("File is created!");
        } else {
            System.out.println("File already exists.");
        }
        FileWriter writer1 = new FileWriter(dec_file);
        for (Map.Entry<String, JsonNode> entry : content_map.entrySet()) {
            c++;
            System.out.println(c);
            //System.out.println("from: "+entry.getKey()+" to: " +entry.getValue());

            JsonNode dec = null;//getDescendants(entry.getValue());
            ArrayList<String> decStrg = new ArrayList<String>();

            writer1.write(entry.getKey());
            if (dec.has("collection")) {
                decStrg = getAsString(dec);
                for (int i = 0; i < decStrg.size(); i++) {
                    writer1.write(" " + decStrg.get(i));
                }
                writer1.write("\n");
            } else {
                writer1.write(" null\n");
            }

        }
        writer1.close();
    }
*/

/*    public static void fetchAncestors() throws IOException {

        int c = 0;
        File dec_file = new File("ancestors.txt");
        if (dec_file.createNewFile()) {
            System.out.println("File is created!");
        } else {
            System.out.println("File already exists.");
        }
        FileWriter writer1 = new FileWriter(dec_file);
        for (Map.Entry<String, JsonNode> entry : content_map.entrySet()) {
            c++;
            System.out.println(c);
            //System.out.println("from: "+entry.getKey()+" to: " +entry.getValue());

            JsonNode anc = null;//getAncestors(entry.getValue());
            writer1.write(entry.getKey());

            if (anc.size() != 0) {
                for (JsonNode json : anc) {
                    writer1.write(" " + json.get("links").get("self").textValue());
                }
                writer1.write("\n");

            } else {
                writer1.write(" null\n");
            }

        }
        writer1.close();
    }*/

/*    public static void saveMapsToFile() throws IOException {
        File id_file = new File("id_map.txt");
        File json_file = new File("content_map.txt");

        //Create the file
        if (id_file.createNewFile()) {
            System.out.println("File is created!");
        } else {
            System.out.println("File already exists.");
        }
        if (json_file.createNewFile()) {
            System.out.println("File is created!");
        } else {
            System.out.println("File already exists.");
        }

        FileWriter writer1 = new FileWriter(id_file);
        FileWriter writer2 = new FileWriter(json_file);

        Iterator<Entry<String, ArrayList<String>>> it = id_map.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<String, ArrayList<String>> pair = (Entry<String, ArrayList<String>>) it.next();
            writer1.write(pair.getKey() + " " + pair.getValue().get(0) + "\n");
        }

        writer1.close();
        Iterator<Entry<String, JsonNode>> it2 = content_map.entrySet().iterator();
        while (it2.hasNext()) {

            Map.Entry<String, JsonNode> pair = (Entry<String, JsonNode>) it2.next();
            writer2.write(pair.getKey() + " " + pair.getValue() + "\n");
        }
        writer2.close();

    }*/

    public static void filesToMap() throws FileNotFoundException {
        //BufferedReader reader = new BufferedReader(new FileReader("id_map.txt"));
        //BufferedReader reader2 = new BufferedReader(new FileReader("content_map.txt"));
        //BufferedReader reader3 = new BufferedReader(new FileReader("decendants.txt"));
        BufferedReader reader4 = new BufferedReader(new FileReader("id_map_rdf.txt"));
        //BufferedReader reader5 = new BufferedReader(new FileReader("ancestors.txt"));

        String line;
        String[] ids = new String[2];
        ArrayList<String> tmp;
        try {
            /*while ((line = reader.readLine()) != null) {
                ids = line.split(" ");
                tmp = new ArrayList<String>();
                tmp.add(ids[1]);
                id_map.put(ids[0], tmp);
            }
            reader.close();
            while ((line = reader2.readLine()) != null) {
                ids = line.split(" ", 2);
                content_map.put(ids[0], jsonToNode(ids[1]));
            }
            reader2.close();
            String[] dec;
            while ((line = reader3.readLine()) != null) {
                dec = line.split(" ");
                tmp = new ArrayList<String>();
                for (int i = 1; i < dec.length; i++) {
                    tmp.add(dec[i]);
                }

                dec_map.put(dec[0], tmp);
            }
            reader3.close();

            String[] anc;
            while ((line = reader5.readLine()) != null) {
                anc = line.split(" ");
                tmp = new ArrayList<String>();
                for (int i = 1; i < anc.length; i++) {
                    tmp.add(anc[i]);
                }

                anc_map.put(anc[0], tmp);
            }
            reader5.close();
			*/


            ids = new String[2];
            while ((line = reader4.readLine()) != null) {
                ids = line.split(" ");
                tmp = new ArrayList<String>();
                tmp.add(ids[1]);
                id_map_rdf.put(ids[0], tmp);
            }
            reader4.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    static ArrayList<OntClass> getChildren(OntClass ontClass) {

        Iterator<OntClass> it = ontClass.listSubClasses(true);
        ArrayList<OntClass> children = new ArrayList<OntClass>();
        while (it.hasNext()) {
            children.add(it.next());
        }

        return children;
    }

    /**
     * get the descendants of a class which is represented as JsonNode.
     *
     * @param node the JsonNode of the class we want to get the descendants from
     * @return descendants the descendants of the class represented as JsonNode
     */
    static ArrayList<OntClass> getDescendants(OntClass ontClass) {
        //JsonNode descendants;
        //descendants = jsonToNode(get(node.get("links").findValue("descendants").asText()));
        //return descendants;
    	Iterator<OntClass> it = ontClass.listSubClasses(false);
        ArrayList<OntClass> dec = new ArrayList<OntClass>();
        while (it.hasNext()) {
            OntClass next = it.next();
            if (!next.getURI().equals("http://www.w3.org/2002/07/owl#Nothing")) {
                dec.add(next);
            }
        }
        return dec;
    }
    /**
     * get the Ancestors of a class which is represented as OntClass.
     *
     * @param ontClass the OntClass of the class we want to get the Ancestors from
     * @return ancestors the Ancestors of the class represented as a ArrayList of OntClass
     */
    static ArrayList<OntClass> getAncestors(OntClass ontClass) {
        //JsonNode descendants;
        //descendants = jsonToNode(get(node.get("links").findValue("descendants").asText()));
        //return descendants;
    	Iterator<OntClass> it = ontClass.listSuperClasses(false);
        ArrayList<OntClass> anc = new ArrayList<OntClass>();
        while (it.hasNext()) {
        	OntClass next = it.next();
            if (!next.getURI().equals("http://www.w3.org/2002/07/owl#Nothing")) {
                anc.add(next);
            }
        }
        return anc;
    }

    /**
     * get the Ancestors of a class which is represented as JsonNode.
     *
     * @param node the JsonNode of the class we want to get the Ancestors from
     * @return ancestors the Ancestors of the class represented as JsonNode
     */
    /*private static JsonNode getAncestors(JsonNode node) {
        JsonNode ancestors;
        ancestors = jsonToNode(get(node.get("links").findValue("ancestors").asText()));
        return ancestors;
    }*/

    /**
     * get the Children/Ancestors/Descendants of a class which is represented as JsonNode as an Arraylist of strings.
     *
     * @param node the JsonNode of the class we want to get the Children/Ancestors/Descendants from
     * @return children the Children/Ancestors/Descendants of the class represented in a ArrayList of Strings
     */
 /*   private static ArrayList<String> getAsString(JsonNode node) {
        ArrayList<String> children = new ArrayList<String>();

        while (true) {
            for (int i = 0; i < node.get("collection").size(); i++) {
                children.add(node.get("collection").get(i).get("links").findValue("self").asText());
            }
            if (!node.get("nextPage").isNull()) {
                node = getNextpage(node);
            } else {
                break;
            }
        }
        return children;
    }*/

    /**
     * Return the next page of a request if needed. The next page is specified with a link.
     *
     * @param node input node
     * @return JsonNode nextpage the next page already as JsonNode
     */
    private static JsonNode getNextpage(JsonNode node) {
        JsonNode nextpage;
        String link = node.get("links").findValue("nextPage").asText();
        nextpage = jsonToNode(get(link));
        return nextpage;
    }

    /**
     * Get request that directly handles multipages and returns an ArrayList with the collection of pages
     *
     * @param urlToGet Url
     * @return ArrayList ArrayList of pages of response, every page is a JsonNode
     */
    public static ArrayList<JsonNode> getAll(String urlToGet) {
        ArrayList<JsonNode> completeAnswer = new ArrayList<>();
        Boolean nextPage = true;
        String url = urlToGet;
        JsonNode response = jsonToNode(get(url));
        while (nextPage) {
            completeAnswer.add(response);
            if (response.has("links") && !response.get("links").get("nextPage").isNull()) {
                response = getNextpage(response);
            } else {
                nextPage = false;
            }
        }
        return completeAnswer;
    }

    static ArrayList<String> getKeyByValue(Map<String, ArrayList<String>> map, ArrayList<String> value) {
        ArrayList<String> keys = new ArrayList<String>();
        for (Entry<String, ArrayList<String>> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public static Values getDefinitions(ArrayList<Pair<OntClass, OntClass>> setpairs){
    		String definition1 = null;
		String definition2 = null;

		for (Pair<OntClass, OntClass> temp : setpairs) {
			OntModel first = temp.getFirst().getOntModel();
			OntModel second = temp.getSecond().getOntModel();
			Property def1 = first.createProperty("http://purl.obolibrary.org/obo/IAO_0000115");
			Property def2 = second.createProperty("http://purl.obolibrary.org/obo/IAO_0000115");
			RDFNode prop1 = temp.getFirst().getPropertyValue(def1);
			if (prop1 != null) {
				definition1 = prop1.asLiteral().toString();
			}else {
				definition1 = "NULL";
			}
			RDFNode prop2 = temp.getSecond().getPropertyValue(def2);

			if (prop2 != null) {
				definition2 = prop2.asLiteral().toString();
			}else {
				definition2 = "NULL";
			}
		}
		return new Values(definition1, definition2);
    }
    
	public static Values getExactSynonym(ArrayList<Pair<OntClass, OntClass>> setpairs){
		String label1 = null;
		String synonym2 = null;

	for (Pair<OntClass, OntClass> temp : setpairs) {
		OntClass first = temp.getFirst();
		OntClass second = temp.getSecond();
		label1 = first.getLabel(null).toString();
		OntModel secondModel = second.getOntModel();
		Property syn2 = secondModel.createProperty("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym");

		if (label1 != null) {
			label1 = first.getLabel(null).toString();
		}else {
			label1 = "NULL";
		}
		
		RDFNode prop2 = temp.getSecond().getPropertyValue(syn2);

		if (prop2 != null) {
			synonym2 = prop2.asLiteral().toString();
		}else {
			synonym2 = "NULL";
		}
	}
	
	return new Values(label1, synonym2);
}
    
    /**
     * Read in the data from the excel file provided by Heiner which contains the matches found by them between the HP and MP ontology and convert it into a Map.
     * Each match has a value between 1 and 4, which indicates which of the 4 algorithms detected it as match.
     * The data is doubled, so every match can be accessed as link1|link2 and link2|link1.
     * The keys are the URIs of the nodes separated by a "|".
     * The Map is then stored in DataHandler.silver_mappings
     *
     * @param excelPath Path to the excel file provided
     * @throws IOException if the excel file was not found or couldn't be read
     */
    public static void readAndStoreHPMPExcel(String excelPath) throws IOException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        DataHandler.silver_mappings = readHPMPExcel(excelPath);
    }


    /**
     * Read in the data from the excel file provided by Heiner which contains the matches found by them between the HP and MP ontology and convert it into a Map.
     * Each match has a value between 1 and 4, which indicates which of the 4 algorithms detected it as match.
     * The data is doubled, so every match can be accessed as link1|link2 and link2|link1.
     * The keys are the URIs of the nodes separated by a "|".
     *
     * @param excelPath Path to the excel file provided
     * @return HashMap which describes the matchings and their values
     * @throws IOException if the excel file was not found or couldn't be read
     */
    public static HashMap<MatchKey, Integer> readHPMPExcel(String excelPath) throws IOException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        HashMap<MatchKey, Integer> mappingValues = new HashMap<MatchKey, Integer>();

        // Creating a Workbook from an Excel file (.xls or .xlsx)
        Workbook workbook = WorkbookFactory.create(new File(excelPath));

        // Getting the Sheet at index zero
        Sheet sheet = workbook.getSheetAt(0);

        // Create a DataFormatter to format and get each cell's value as String
        DataFormatter dataFormatter = new DataFormatter();

        // you can use a for-each loop to iterate over the rows and columns
        for (Row row : sheet) {
            String uri1 = dataFormatter.formatCellValue(row.getCell(0));
            String uri2 = dataFormatter.formatCellValue(row.getCell(2));
            try {
                int vote = Integer.parseInt(dataFormatter.formatCellValue(row.getCell(4)));
                mappingValues.put(new MatchKey(uri1, uri2), vote);
                mappingValues.put(new MatchKey(uri2, uri1), vote);
            } catch (Exception e) {
                continue;
            }
        }
        return mappingValues;
    }
    
    /**
     * A class to return the values pairs used in the function getDefinitions
     */
    static public class Pair<F, S> {
        private F first;
        private S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public void setFirst(F first) {
            this.first = first;
        }

        public void setSecond(S second) {
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }

    	/**
	     * A class that encapsulates all the returned values from the function getDefinitions
	     */
	    static class Values {
	        String def1;
	        String def2;

	        Values(String definition1, String definition2) {
	            def1 = definition1;
	            def2 = definition2;
	        }
	    }



}
