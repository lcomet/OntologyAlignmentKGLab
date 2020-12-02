package main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import main.Metrics;
import main.DataHandler;
import main.DataHandler.Pair;
import main.DataHandler.Values;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;


public class ListOntologies {

	final static boolean hpmp = true;
	final static boolean ordodoid = false ;
	public static void main(String[] args) throws FileNotFoundException {

		List<String> ontNames = new ArrayList<String>();
		//boolean mapPresent = false;
		String filePath = "data/id_map_rdf.txt";
		if(hpmp) {
			ontNames.add("HP");
			ontNames.add("MP");
			OntModel MP = DataHandler.readRDFfile("data/ontfiles/MPowlapi.xrdf","rdf");
			OntModel HP = DataHandler.readRDFfile("data/ontfiles/HPowlapi.xrdf","rdf");
			DataHandler.fetchMappings(ontNames,filePath);
			EvaluationModel evaluationModel = evaluation(HP,"HP", MP,"MP");
			evaluationModel.toFile();
			
		}
		
		
		if(ordodoid) {
			ontNames.add("DOID");
			ontNames.add("ORDO");
			OntModel DOID = DataHandler.readRDFfile("DOID.xrdf","rdf");
			OntModel ORDO = DataHandler.readRDFfile("ordo.owl","owl");
			DataHandler.fetchMappings(ontNames,filePath);
			EvaluationModel evaluationModel = evaluation(DOID,"DOID", ORDO,"ORDO");
			evaluationModel.toFile();
			
		}
		


	}


	/**
	 * This function aggregates all the metrics and stores the result in a corresponding EvaluationModel object.
	 * TODO: Tcreate empty maphis should get a DataHandler Object. Otherwise, its not possible to extend this to multiple ontologies
	 *
	 * @param ont1 Ontology 1
	 * @param ont2 Ontology 2
	 * @return EvaluatinModel Model of the results
	 */
	private static EvaluationModel evaluation(OntModel ont1,String ont1_name, OntModel ont2,String ont2_name) {
		EvaluationModel evaluationModel = new EvaluationModel(ont1,ont1_name, ont2,ont2_name);

		// start analysis
		System.out.println("Started evaluation");

		// fill valuation model with hashmap of matches
		//evaluationModel.createEmptyMap(DataHandler.id_map);
		evaluationModel.createEmptyMap(DataHandler.id_map_rdf);
		System.out.println("created empty map");



			// Calculate jaccard, sigmoid, and cosine similarity
		for (MatchKey matchKey : evaluationModel.evaluations.keySet()) {
			OntClass class1 = ont1.getOntClass(matchKey.getX());
			OntClass class2 = ont2.getOntClass(matchKey.getY());
			System.out.print(matchKey.getX());
			String label1 = class1.getLabel(null);
			String label2 = class2.getLabel(null);
			
			// calculate similarity values for class labels
			Metrics.Values jaccardsimilarity_values = Metrics.calculateJaccardSimilarity(label1, label2);
			evaluationModel.evaluations.get(matchKey).jaccardSimilarity = jaccardsimilarity_values.jaccard_value;
			evaluationModel.evaluations.get(matchKey).sigmoidSimilarity = jaccardsimilarity_values.sigmoid_similarity;
			evaluationModel.evaluations.get(matchKey).cosineSimilarity = Metrics.LabelsPeprocessing(label1, label2);
			
			// calculate similarity values for class definitions
			ArrayList <Pair <OntClass,OntClass> > setpairs = new ArrayList <Pair <OntClass, OntClass> > ();
			setpairs.add(new Pair <OntClass,OntClass> (class1, class2));

			// get definitions for the pair of classes
			Values defvalues = DataHandler.getDefinitions(setpairs);
			String definition1 = defvalues.def1;
			String definition2 = defvalues.def2;
			Metrics.Values jaccardsimilarity_values_definitions = Metrics.calculateJaccardSimilarity(definition1, definition2);

			evaluationModel.evaluations.get(matchKey).jaccardSimilarity_def = jaccardsimilarity_values_definitions.jaccard_value;
			evaluationModel.evaluations.get(matchKey).sigmoidSimilarity_def = jaccardsimilarity_values_definitions.sigmoid_similarity;

			evaluationModel.evaluations.get(matchKey).cosineSimilarity_def = Metrics.LabelsPeprocessing(definition1, definition2);
			
			// calculate similarity values for labels of the first ontology and synonyms of the second ontology
			Values synvalues = DataHandler.getExactSynonym(setpairs);
			String labelsyn1 = synvalues.def1;
			String synonym2= synvalues.def2;
			Metrics.Values jaccardsimilarity_values_synonyms = Metrics.calculateJaccardSimilarity(labelsyn1, synonym2);

			evaluationModel.evaluations.get(matchKey).jaccardSimilarity_syn = jaccardsimilarity_values_synonyms.jaccard_value;
			evaluationModel.evaluations.get(matchKey).sigmoidSimilarity_syn = jaccardsimilarity_values_synonyms.sigmoid_similarity;

			evaluationModel.evaluations.get(matchKey).cosineSimilarity_syn = Metrics.LabelsPeprocessing(labelsyn1, synonym2);

		}
		System.out.println("label checking done");
		// evaluate descendants
		for (MatchKey matchKey : evaluationModel.evaluations.keySet()) {
			//evaluationModel.evaluations.get(matchKey).descendantsEvaluation = Metrics.compareDescendants(DataHandler.content_map.get(matchKey.getX()), DataHandler.content_map.get(matchKey.getY()));
			evaluationModel.evaluations.get(matchKey).descendantsEvaluation = Metrics.compareChildren(ont1.getOntClass(matchKey.getX()), ont2.getOntClass(matchKey.getY()));
		}



		// calculate jaccardi similarity for subgraph
		System.out.println("Started calculating jaccard on subtree.");
		for (MatchKey matchKey : evaluationModel.evaluations.keySet()) {
			evaluationModel.evaluations.get(matchKey).jaccardSubtree = Metrics.calculateJaccardiSubtree(ont1.getOntClass(matchKey.getX()), ont2.getOntClass(matchKey.getY()), ont1, ont2);

		}
		System.out.println("Finished calculating jaccard on subtree.");

		// calculate cosine similarity for subgraph
		System.out.println("Started calculating cosine on subtree.");
		for (MatchKey matchKey : evaluationModel.evaluations.keySet()) {
			evaluationModel.evaluations.get(matchKey).cosineSubtree =  Metrics.calculateCosineSubtree(ont1.getOntClass(matchKey.getX()), ont2.getOntClass(matchKey.getY()));
		}
		System.out.println("Finished calculating cosine on subtree.");

		// look for loops
		System.out.println("Started checking for loops.");

		for (MatchKey matchKey : evaluationModel.evaluations.keySet()) {
			evaluationModel.evaluations.get(matchKey).hasLoop = Metrics.hasLooprdf(ont1.getOntClass(matchKey.getX()), ont2.getOntClass(matchKey.getY()));
		}
		// TODO: also store loops
		System.out.println("Finished checking for loops.");


		// read in silver mappings (if available)
		System.out.println("Started comparing silver mappings.");
		if (DataHandler.silver_mappings == null || DataHandler.silver_mappings.size() == 0) {
			try {
				if(hpmp) {
					DataHandler.readAndStoreHPMPExcel("data/Silver_mappings_OAEI2017/Silver-hp-mp-rdf-max_votes.xlsx");
				}
				if(ordodoid) {
					DataHandler.readAndStoreHPMPExcel("data/Silver_mappings_OAEI2017/Silver-doid-ordo-rdf-max_votes.xlsx");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidFormatException e) {
				e.printStackTrace();
			}
		}
		for (MatchKey matchKey : DataHandler.silver_mappings.keySet()) {
			try {
				evaluationModel.evaluations.get(matchKey).silverMapping = DataHandler.silver_mappings.get(matchKey);
			} catch (NullPointerException e) {
			}
		}
		System.out.println("Finished comparing silver mappings.");

		evaluationModel.calculate_score();
		// analysis finished
		System.out.println("Finished evaluation");
		
		return evaluationModel;
	}


	/**
	 * Returns the number of mappings between two ontologies.
	 *
	 * @param ont1, ont2 String representation of the two ontologies
	 * @return count  number of mappings counted between ont1 and ont2
	 */
	public static int countMappings(String ont1, String ont2) {
		int count = 0;
		count = DataHandler.id_map_rdf.size();
		return count;
	}


	public static String test() {
		return "this is a test";
	}


}
