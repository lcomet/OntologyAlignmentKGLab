package main;


import java.util.*;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import com.fasterxml.jackson.databind.JsonNode;


public class Metrics {

    /**
     * checks if the prefLabels of the two classes are equal
     *
     * @param class_1 the JsonNode of the first class we want to check
     * @param class_2 the JsonNode of the second class we want to check
     * @return calculated values for the jaccard similarity metric, and the two compared labels
     */
    static void comparePrefLabel(JsonNode class_1, JsonNode class_2) {
        String label1 = class_1.findValue("prefLabel").asText().toUpperCase();
        String label2 = class_2.findValue("prefLabel").asText().toUpperCase();
        Values jaccardsimilarity_values = calculateJaccardSimilarity(label1, label2);
        PrintValues(jaccardsimilarity_values);
    }

    /**
     * print the values returned for the function comparePrefLabel
     *
     * @param values returned for the function comparePrefLabel
     */
    private static void PrintValues(Values values) {
        System.out.print("SIMILARITY - COMPARE LABELS: \n \n" + "LABEL 1: " + values.lab1 +
                "\n \nLABEL2: " + values.lab2 + " \n \nJACCARD SIMILARITY:"
                + values.jaccard_value + "\n\n" + "SIGMOID SIMILARITY: " + values.sigmoid_similarity + "\n\n");
        if (values.jaccard_value >= 0.7) {
            System.out.println("JACCARD SIMILARITY: Good");
        } else {
            System.out.println("JACCARD SIMILARITY: Bad");
        }

        if (values.sigmoid_similarity >= 0.7) {
            System.out.println("SIGMOID SIMILARITY: Good");
        } else {
            System.out.println("SIGMOID SIMILARITY: Bad");
        }
    }

    /**
     * A class that encapsulates all the returned values from the calculateJaccardSimilary function
     */
    static class Values {
        String lab1;
        String lab2;
        double jaccard_value;
        double sigmoid_similarity;

        Values(String l1, String l2, double js, double sigmoid) {
            jaccard_value = js;
            lab1 = l1;
            lab2 = l2;
            sigmoid_similarity = sigmoid;
        }
    }


    /**
     * Calculates values for the Jaccard Similarity, and the Sigmoid Similarity (based on paper:
     * How to improve Jaccard’s feature-based similarity measure - Likavec et al.)
     * of two character sequences passed as input.
     * The Jaccard Similarity value is calculated by identifying the union
     * (characters in at least one of the two sets) of the two sets and intersection (characters
     * which are present in set one which are present in set two)
     * The Sigmoid Similarity is calculated by taking the intersection (common features) as arguments of
     * the sigmoid function.
     *
     * @param label1 first character sequence
     * @param label2 second character sequence
     * @return an object of the class Values containing the Jaccard similarity value, the Sigmoid similarity value,
     * and the two labels received as parameters
     */
    static Values calculateJaccardSimilarity(String label1, String label2) {
        Set<String> intersectionSet = new HashSet<String>();
        Set<String> unionSet = new HashSet<String>();
        boolean unionFilled = false;
        int ont1Length = label1.length();
        int ont2Length = label2.length();
        if (ont1Length == 0 || ont2Length == 0) {
            return new Values(label1, label2, 0d, 0d);
        }

        for (int ont1Index = 0; ont1Index < ont1Length; ont1Index++) {
            unionSet.add(String.valueOf(label1.charAt(ont1Index)));
            for (int ont2Index = 0; ont2Index < ont2Length; ont2Index++) {
                if (!unionFilled) {
                    unionSet.add(String.valueOf(label2.charAt(ont2Index)));
                }
                if (label1.charAt(ont1Index) == label2.charAt(ont2Index)) {
                    intersectionSet.add(String.valueOf(label1.charAt(ont1Index)));
                }
            }
            unionFilled = true;
        }

        double jaccard_similarity_value = Double.valueOf(intersectionSet.size()) / Double.valueOf(unionSet.size());

        double exp_intersection = Math.pow(Math.E, intersectionSet.size());
        double diff_union_intersection = unionSet.size() - intersectionSet.size();
        double sigmoid_similarity =  (exp_intersection - 1) / ((exp_intersection + 1) * (diff_union_intersection + 1));
        return new Values(label1, label2, jaccard_similarity_value, sigmoid_similarity);

    }


    /**
     * Calculate Jaccardi Similarity for subtrees of two nodes
     * The root node is not taken into consideration; i.e. it doesn't make a difference if the provided nodes are a match or not
     * The formula is A^B/AvB, with A^B being the matches between the two subgraphs
     *
     * @param node1
     * @param node2
     * @return Jaccardi Similarity
     */
    protected static double calculateJaccardiSubtree(OntClass node1, OntClass node2, OntModel ont1, OntModel ont2) {

        ArrayList<OntClass> descendants1 = DataHandler.getDescendants(node1);
        ArrayList<OntClass> descendants2 = DataHandler.getDescendants(node2);


        // calculate amount of descendants of both
        int sizeDesc1 = descendants1.size();
        int sizeDesc2 = descendants2.size();

        // if both dont have descendants, similarity is 1
        if (sizeDesc1 == 0 && sizeDesc2 == 0) {
            return 1;
        }

        // if one has descendands and the other not, similarity is 0
        if (sizeDesc1 == 0 || sizeDesc2 == 0) {
            return 0;
        }

        // calculate amount of matches between the descendants
        int amountMatches = 0;
        for (OntClass node : descendants1) {
            if (DataHandler.id_map_rdf.containsKey(node.getURI())) {
                ArrayList<String> possibleMatches = DataHandler.id_map_rdf.get(node.getURI());
                for (String match : possibleMatches) {
                    if (descendants2.contains(ont2.getOntClass(match))) {
                        amountMatches++;
                    }
                }
            }
        }

        // if amount of matches is 0, the similarity is 0
        if (amountMatches == 0) {
            return 0;
        }

        // calculate Jaccardi Similarity; A^B/(A+B-A^B) = A^B/AvB
        return (amountMatches + 0.0) / (sizeDesc1 + sizeDesc2 - amountMatches + 0.0);
    }


    /**
     * This function calculates a metric, that indicates how tightly the ancestors of two nodes in different ontologies are linked.
     * This is an experimental metric, and it has to be seen if it will be of any use.
     *
     * @param node1
     * @param node2
     * @return metric. Currently just a single number, but maybe some kind of wrapper could be used to include more information
     */
  /*  private static double compareAncestors(String node1, String node2) {

        // fetch list of ancestors of both nodes
        ArrayList<String> ancesNode1 = null;//= getAncestors(node1);
        ArrayList<String> ancesNode2 = null;//= getAncestors(node2);

        // if one of the lists is empty, there cannot be a mapping
        if (ancesNode1.size() == 0 || ancesNode2.size() == 0) {
            return 0;
        }

        // fetch list of mappings between both ontologies. This could be given as a parameter

        // calculate how many mappings exist between the ancestors of the nodes


        // sum up ancestors
        int amount1 = ancesNode1.size();
        int amount2 = ancesNode2.size();

        // calculate a metric by the amount depending on the amount of ancestors and mappings between them

        return 0;
    }

*/
    /*static boolean compareDescendants(JsonNode class_1, JsonNode class_2) {

        ArrayList<String> dec1 = DataHandler.dec_map.get(class_1.get("links").findValue("self").asText());
        ArrayList<String> dec2 = DataHandler.dec_map.get(class_2.get("links").findValue("self").asText());

        if (dec1.get(0).equals("null") && dec2.get(0).equals("null")) {
            return false;
        } else if (!dec1.get(0).equals("null")) {
            Iterator<String> it = dec1.iterator();
            while (it.hasNext()) {
                String tmp = it.next();
                if (DataHandler.id_map.containsKey(tmp) && dec2.contains(DataHandler.id_map.get(tmp).get(0))) {
                    //there exists a mapping between children
                    //maybe check if the labels are the same
                    //this indicates a good mapping

                } else {
                    //there is a mapping from a child if class_1 which is not a child of class_2
                    //this indicates a bad mapping
                    //System.out.println("Mapping from a dec in class_1 which is not a dec of class_2, comparison: bad");
                    return true;
                }
            }
        } else if (!dec2.get(0).equals("null")) {
            Iterator<String> it = dec2.iterator();
            while (it.hasNext()) {
                String tmp = it.next();
                if (DataHandler.id_map.containsKey(tmp) && dec1.contains(DataHandler.id_map.get(tmp).get(0))) {
                    //there exists a mapping between children
                    //maybe check if the labels are the same
                    //this indicates a good mapping
                } else {
                    //there is a mapping from a child of class_2 which is not a child of class_1
                    //this indicates a bad mapping
                    //System.out.println("There is a mapping from a child of class_2 which is not a child of class_1, comparison: bad");
                    return true;
                }
            }


        }
        //System.out.println("comparions: good");
        return false;
    }
*/

    //TODO calculate some kind of confidence
    static boolean compareChildren(OntClass class_1, OntClass class_2) {
        ArrayList<OntClass> c1_children = DataHandler.getChildren(class_1);
        ArrayList<OntClass> c2_children = DataHandler.getChildren(class_2);
        boolean comp = false;
        if (c1_children.isEmpty() && c2_children.isEmpty()) {
            //System.out.println("good");
            return false;
        } else if (!c1_children.isEmpty()) {
            OntModel m = class_2.getOntModel();
            for (OntClass c : c1_children) {
                String uri = c.getURI();
                if (DataHandler.id_map_rdf.containsKey(uri) && !c2_children.contains(m.getOntClass(DataHandler.id_map_rdf.get(uri).get(0)))) {
                    //return 0;
                    return true;
                }
            }

        } else if (!c2_children.isEmpty()) {
            OntModel m = class_1.getOntModel();
            for (OntClass c : c2_children) {
             //   String uri = c.getURI();
                ArrayList<String> tmp = new ArrayList<String>();
                tmp.add(c.getURI());
                ArrayList<String> keys = DataHandler.getKeyByValue(DataHandler.id_map_rdf, tmp);
                if (DataHandler.id_map_rdf.containsValue(tmp) && !c1_children.contains(m.getOntClass(keys.get(0)))) {
                    //return 0;
                    return true;
                }
            }
        }
        //System.out.println("prob. good");
        return comp;
    }

    /**
     * Given a mapping consisting of two nodes from different ontologies, this function checks if there is another mapping which creates a loop.
     *
     * @param node1
     * @param node2
     * @return boolean true if there is a loop, false if not
     */
/*    static boolean hasLoop(String node1, String node2) {
        JsonNode jNode1 = DataHandler.content_map.get(node1);
        JsonNode jNode2 = DataHandler.content_map.get(node2);
        ArrayList<String[]> loops = new ArrayList<>();
        //String ancs1 = jNode1.get("links").get("ancestors").toString();
        //String ancs1 = DataHandler.anc_map.get(jNode1);
        //ancs1 = ancs1.substring(1, ancs1.length() - 1);

        //ArrayList<JsonNode> ancestorsList1 = DataHandler.getAll(ancs1);
        ArrayList<String> descendants;
        ArrayList<String> ancestorsList1;


        if (DataHandler.dec_map.containsKey(node2)) {
            descendants = DataHandler.dec_map.get(node2);
        } else {
			

            // implement
            descendants = null;
        }

        //System.out.println(DataHandler.anc_map.toString());
        System.out.println(node1);
        System.out.println(DataHandler.anc_map.keySet().toString());

        if (DataHandler.anc_map.containsKey(node1)) {
            ancestorsList1 = DataHandler.anc_map.get(node1);
        } else {
            ancestorsList1 = null;
        }

        if (ancestorsList1 == null || descendants == null || ancestorsList1.size() == 0 || descendants.size() == 0) {
            return false;
        }
        ancestorsList1.forEach((ancestor) -> {
            //String ancestorName =  ancestor.get("links").get("self").toString();
            //ancestorName = ancestorName.substring(1, ancestorName.length() - 1);

            if (DataHandler.id_map.containsKey(ancestor)) {
                // match1 should be a match, with the node being part of the same ontology in which node1 is located
                // TODO: HANDLE ARRAYLIST PROPERLY
                String match1 = DataHandler.id_map.get(ancestor).get(0);
                // if match1 is a descendant of node1, then there is a possible loop
                if (descendants.contains(match1)) {
                    //String match2 =  ancestor.get("links").get("self").toString();
                    //match2 = match2.substring(1, match2.length()-1);
                    System.out.println("Possible loop in following matches: " + node1 + " / " + node2 + " and " + ancestor + " / " + match1);
                    String[] loop = {node1, node2, ancestor, match1};
                    loops.add(loop);
                }
            } else if (DataHandler.id_map.containsValue(ancestor)) {
                // is this part even neccesary?
                // TODO: Verify if this has to be implemented
                System.out.println("This shouldnt happen");

            }
        });


        if (loops.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
*/

    @SuppressWarnings("unlikely-arg-type")
	static boolean hasLooprdf(OntClass class_1, OntClass class_2) {
        //JsonNode jNode1 = DataHandler.content_map.get(node1);
        //JsonNode jNode2 = DataHandler.content_map.get(node2);
        ArrayList<String[]> loops = new ArrayList<>();
        //String ancs1 = jNode1.get("links").get("ancestors").toString();
        //String ancs1 = DataHandler.anc_map.get(jNode1);
        //ancs1 = ancs1.substring(1, ancs1.length() - 1);

        //ArrayList<JsonNode> ancestorsList1 = DataHandler.getAll(ancs1);
        ArrayList<OntClass> descendants;
        ArrayList<OntClass> ancestorsList1;


        //if (DataHandler.dec_map.containsKey(node2)) {

        descendants = DataHandler.getDescendants(class_2);

        //} else {
			/*String desc2 = jNode2.get("links").get("descendants").toString();
			desc2 = desc2.substring(1, desc2.length() - 1);
			ArrayList<JsonNode> descendantsList2 = DataHandler.getAll(desc2);
			if (descendantsList2 == null || descendantsList2.size() == 0) {
				return false;
			}*/

        // implement
        //descendants = null;
        //}

        //System.out.println(DataHandler.anc_map.toString());
        //System.out.println(node1);
        //System.out.println(DataHandler.anc_map.keySet().toString());

        //if (DataHandler.anc_map.containsKey(node1)) {
        //	ancestorsList1 = DataHandler.anc_map.get(node1);
        //}else {
        if (class_1.hasSuperClass())
            ancestorsList1 = DataHandler.getAncestors(class_1);
        else
            ancestorsList1 = null;
        //}

        if (ancestorsList1 == null || descendants == null || ancestorsList1.size() == 0 || descendants.size() == 0) {
            return false;
        }
        OntModel dec_m = class_2.getOntModel();

        //System.out.println("class2: " +class_2.getLabel(null) +" dec size: "+  descendants.size()+ " class_1: "+class_1.getLabel(null)+" anc_size(): "+ancestorsList1.size() );

        ancestorsList1.forEach((ancestor) -> {
            //String ancestorName =  ancestor.get("links").get("self").toString();
            //ancestorName = ancestorName.substring(1, ancestorName.length() - 1);

            String uri = ancestor.getURI();
			if (DataHandler.id_map_rdf.containsKey(uri)) {
                // match1 should be a match, with the node being part of the same ontology in which node1 is located
                // TODO: HANDLE ARRAYLIST PROPERLY
                String match1 = DataHandler.id_map_rdf.get(uri).get(0);
                // if match1 is a descendant of node1, then there is a possible loop
                if (descendants.contains(dec_m.getOntClass(match1))) {
                    //String match2 =  ancestor.get("links").get("self").toString();
                    //match2 = match2.substring(1, match2.length()-1);
                    System.out.println("Possible loop in following matches: " + class_1.getLabel(null) + " / " + class_2.getLabel(null) + " and " + ancestor.getLabel(null) + " / " + match1);
                    String[] loop = {class_1.getLabel(null), class_2.getLabel(null), ancestor.getLabel(null), match1};
                    loops.add(loop);
                }
            } else if (DataHandler.id_map_rdf.containsValue(uri)) {
                // is this part even neccesary?
                // TODO: Verify if this has to be implemented
                System.out.println("This shouldnt happen");

            }
        });


        if (loops.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * Implementation of Cosine Similarity Metric
     */

    /**
     * This function do the preprocessing of the two strings that receive as input
     * It takes the the label of each class, convert them to upper case form, and apply trim function to it to eliminate
     * Whitespace at the beginning and end of the string
     *
     * @param label of the first ontology
     * @param label of the second ontology
     * @return
     */
    static double LabelsPeprocessing(String label1, String label2) {

        double cosine_similarity_value = 0.0d;

        label1 = label1.toUpperCase().trim();
        label2 = label2.toUpperCase().trim();

        cosine_similarity_value = LabelsToVector(label1, label2);

        return cosine_similarity_value;

    }

    // Split the two labels in two different sequences (arrays) of words
    static double LabelsToVector(String label1, String label2) {

        double cosine_similarity = 0.0d;
        String[] seqlabel1 = label1.split(" ");
        String[] seqlabel2 = label2.split(" ");

        cosine_similarity = WordsFrequency(seqlabel1, seqlabel2);

        return cosine_similarity;
    }


    /**
     * Calculates values for Cosine Similarity Metric of two sequence of characters passed as input
     * The counting frequencies is based on an implementation on linkipedia
     * The Cosine Similarity is calculated by comparing the values of two non-zero vectors of an inner product that measures the cosine
     * angle between them. The cosine of 0º is 1, while of 90º is 0. Meaning that two vectors are maximally similar if the
     * cosine similarity value is 1, while they are dissimilar if the cosine similarity value is 0
     *
     * @param label1 first character sequence
     * @param label2 second character sequence
     * @return the result of the calculation of the cosine similarity between the two vectors
     */
    public static double WordsFrequency(String[] vector1, String[] vector2) {

        double cosineSimilarity_value = 0.0d;
        Hashtable<String, ValuesFrequency> wordFrequency = new Hashtable<String, ValuesFrequency>();
        LinkedList<String> differencesTwoVectors = new LinkedList<String>();

        if (vector1 == null || vector2 == null) {
            throw new IllegalArgumentException("One of the vectors is null.");
        }

        //Count frequency of word occurrence in the first sequence (vector1)
        for (int i = 0; i < vector1.length; i++) {
            String stringLabel1 = vector1[i];
            if (stringLabel1.length() > 0) {
                if (wordFrequency.containsKey(stringLabel1)) {
                    ValuesFrequency count_Vector1 = wordFrequency.get(stringLabel1);
                    int freq1 = count_Vector1.freqWord1 + 1;
                    int freq2 = count_Vector1.freqWord2;
                    count_Vector1.updateValues(freq1, freq2);
                    wordFrequency.put(stringLabel1, count_Vector1);
                } else {
                    ValuesFrequency valSrc = new ValuesFrequency(1, 0);
                    wordFrequency.put(stringLabel1, valSrc);
                    differencesTwoVectors.add(stringLabel1);
                }
            }
        }

        //Count frequency of word occurrence in the first sequence (vector2)
        for (int i = 0; i < vector2.length; i++) {
            String stringLabel2 = vector2[i];
            if (stringLabel2.length() > 0) {
                if (wordFrequency.containsKey(stringLabel2)) {
                    ValuesFrequency count_Vector2 = wordFrequency.get(stringLabel2);
                    int freq1 = count_Vector2.freqWord1;
                    int freq2 = count_Vector2.freqWord2 + 1;
                    count_Vector2.updateValues(freq1, freq2);
                    wordFrequency.put(stringLabel2, count_Vector2);
                } else {
                    ValuesFrequency count_Vector2 = new ValuesFrequency(0, 1);
                    wordFrequency.put(stringLabel2, count_Vector2);
                    differencesTwoVectors.add(stringLabel2);
                }
            }
        }

        cosineSimilarity_value = CosineSimilarity(wordFrequency, differencesTwoVectors);
        return cosineSimilarity_value;

    }

    //Compute Cosine Similarity taking the two vectors
    public static double CosineSimilarity(Hashtable<String, ValuesFrequency> wordFrequency, LinkedList<String> differencesTwoVectors) {

        double UnionVectors = 0.0d, Seq_Vector1 = 0.0d, Seq_Vector2 = 0.0d, cosineSimilarity_value = 0.0d;


        for (int i = 0; i < differencesTwoVectors.size(); i++) {
            ValuesFrequency vectors = wordFrequency.get(differencesTwoVectors.get(i));

            double freq1 = vectors.freqWord1;
            double freq2 = vectors.freqWord2;

            UnionVectors = UnionVectors + (freq1 * freq2);

            Seq_Vector1 = Seq_Vector1 + freq1 * freq1;
            Seq_Vector2 = Seq_Vector2 + freq2 * freq2;
        }

        cosineSimilarity_value = ((UnionVectors) / (Math.sqrt(Seq_Vector1) * Math.sqrt(Seq_Vector2)));

        return cosineSimilarity_value;

    }

    // encapsulate the frequency of the occurrence of words in the two vectors
    public static class ValuesFrequency {

        int freqWord1;
        int freqWord2;

        ValuesFrequency(int val1, int val2) {
            this.freqWord1 = val1;
            this.freqWord2 = val2;
        }

        public void updateValues(int val1, int val2) {
            this.freqWord1 = val1;
            this.freqWord2 = val2;
        }

    }

    /**
     * Implement a variant of the cosine similarity applied to two subgraphs.
     * Each subgraph is feteched as a String Array of Words of which the labels consists.
     * These two string arrays are then fed into the cosine similarity function.
     * @param node1
     * @param node2
     * @return cosine similarity for subgraph
     */
    protected static double calculateCosineSubtree(OntClass node1, OntClass node2) {
        double cosine_similarity = 0.0d;

        ArrayList<OntClass> descendants1 = DataHandler.getDescendants(node1);
        ArrayList<OntClass> descendants2 = DataHandler.getDescendants(node2);

        if (descendants1.size() == 0 && descendants2.size() == 0) {
            return 1;
        }
        if (descendants1.size() == 0 || descendants2.size() == 0) {
            return 0;
        }

        // Fetch ArrayList<String> between all labels of subtree
        ArrayList<String> labelsSubtree1 = new ArrayList<>();
        ArrayList<String> labelsSubtree2 = new ArrayList<>();

        for (OntClass ontClass : descendants1) {
            String label = ontClass.getLabel(null);
            if (label != null) {
                label = label.toUpperCase().trim();
                String[] seqlabel = label.split(" ");
                labelsSubtree1.addAll(Arrays.asList(seqlabel));
            }
        }

        for (OntClass ontClass : descendants2) {
            String label = ontClass.getLabel(null);
            if (label != null) {
                label = label.toUpperCase().trim();
                String[] seqlabel = label.split(" ");
                labelsSubtree2.addAll(Arrays.asList(seqlabel));
            }
        }

        // Create String[] with all the Strings
        String[] subtree1 = labelsSubtree1.toArray(new String[labelsSubtree1.size()]);
        String[] subtree2 = labelsSubtree2.toArray(new String[labelsSubtree2.size()]);


        if (subtree1.length == 0 && subtree2.length == 0 ){
            return 1;
        }
        if (subtree1.length == 0 || subtree2.length == 0 ){
            return 0;
        }

        cosine_similarity = WordsFrequency(subtree1, subtree2);
        return cosine_similarity;
    }


}
