package main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.OntModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * This class is a model of the results that are calculated in the analysis.
 * The main attribute is evaluations, which is a Map from MapyKey -> MatchEvaluation
 * MapKey is a key consisting of two strings, a match can stored this way.
 * MatchEvaluation is a wrapper that contains all the metrics that were calculated for a single match
 */
public class EvaluationModel implements Serializable {


	public Map<MatchKey, MatchEvaluation> evaluations = new HashMap<>();
	// ontologies which are compared
	public MatchKey ontologies;


	public EvaluationModel(OntModel ont1,String ont1_name, OntModel ont2,String ont2_name) {
		this.ontologies = new MatchKey(ont1_name, ont2_name);
	}

	/**
	 * Stores a possible loop that was detected.
	 */
	public class Loop implements Serializable {
		public String[] match1;
		public String[] match2;

		Loop(String[] match1, String[] match2) {
			if (match1.length == 2 && match2.length == 2) {
				this.match1 = match1;
				this.match2 = match2;
			} else {
				throw new NumberFormatException("Matches should be String Arrays of size 2.");
			}
		}
	}

	/**
	 * Stores all the calculated data for one match.
	 */
	public class MatchEvaluation implements Serializable {

		public double jaccardSimilarity;
		public double jaccardSimilarity_def;
		public double jaccardSimilarity_syn;
		public double jaccardSubtree;
		public double sigmoidSimilarity;
		public double sigmoidSimilarity_def;
		public double sigmoidSimilarity_syn;
		public double cosineSimilarity;
		public double cosineSimilarity_def;
		public double cosineSimilarity_syn;
		public double cosineSubtree;
		public boolean descendantsEvaluation;
		public boolean hasLoop;
		public int score;
		public ArrayList<Loop> loops;

		// this attribute only applies to the few mappings between ontologies provided by heiner
		public double silverMapping;

		public MatchEvaluation() {
			this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, new ArrayList<Loop>(), 0.0, 0.0,0.0,0);

		}


		public MatchEvaluation(double jaccardSimilarity, double jaccardSimilarity_def, double jaccardSimilarity_syn, 
				double sigmoidSimilarity, double sigmoidSimilarity_def, double sigmoidSimilarity_syn, double cosineSimilarity, 
				double cosineSimilarity_def, double cosineSimilarity_syn, boolean descendantsEvaluation, boolean hasLoop, ArrayList<Loop> loops, 
				double silverMapping, double jaccardSubtree, double cosineSubtree,int score) {
			this.jaccardSimilarity = jaccardSimilarity;
			this.jaccardSimilarity_def = jaccardSimilarity_def;
			this.jaccardSimilarity_syn = jaccardSimilarity_syn;
			this.sigmoidSimilarity = sigmoidSimilarity;
			this.sigmoidSimilarity_def = sigmoidSimilarity_def;
			this.sigmoidSimilarity_syn = sigmoidSimilarity_syn;
			this.cosineSimilarity = cosineSimilarity;
			this.cosineSimilarity_def = cosineSimilarity_def;
			this.cosineSimilarity_syn = cosineSimilarity_syn;
			this.descendantsEvaluation = descendantsEvaluation;
			this.hasLoop = hasLoop;
			this.loops = loops;
			this.silverMapping = silverMapping;
			this.jaccardSubtree = jaccardSubtree;
			this.score = score;
			this.cosineSubtree = cosineSubtree;
		}

		public String toString() {
			return jaccardSimilarity + " , " + jaccardSimilarity_def + " , " + jaccardSimilarity_syn + " , " + sigmoidSimilarity + " , " + 
				   sigmoidSimilarity_def + " , " + sigmoidSimilarity_syn + " , " + cosineSimilarity + " , " + cosineSimilarity_def + " , " + 
					cosineSimilarity_syn + " , " + descendantsEvaluation + " , " + hasLoop + " , " + silverMapping;
		}
	}

	/**
	 * Fills evaluations with the keySet and empty MatchEvaluations
	 *
	 * @param id_map Map of matches
	 */
	public void createEmptyMap(Map<String, ArrayList<String>> id_map) {
		for (String key : id_map.keySet()) {
			for (String value : id_map.get(key)) {
				evaluations.put(new MatchKey(key, value), new MatchEvaluation());
			}
		}
	}

	/*
	 * calculates score based on decision tree rules
	 */
	public void calculate_score() {
		for (MatchKey matchKey : evaluations.keySet()) {
			MatchEvaluation matchEvaluation = evaluations.get(matchKey);
			if(matchEvaluation.hasLoop) {
				matchEvaluation.score=2;
			} else {
				if(matchEvaluation.descendantsEvaluation) {
					if(matchEvaluation.jaccardSimilarity<0.8&&matchEvaluation.sigmoidSimilarity<0.4&&matchEvaluation.cosineSimilarity<0.8) {
						matchEvaluation.score = 2;
					}else {
						matchEvaluation.score = 3;
					}
					
				} else {
					if(matchEvaluation.jaccardSimilarity<0.8&&matchEvaluation.sigmoidSimilarity<0.4&&matchEvaluation.cosineSimilarity<0.8) {
						matchEvaluation.score = 3;
					} else {
						matchEvaluation.score = 4;
					}
				}
			}	
		}
	}
	public String toString() {
		StringBuilder response = new StringBuilder();
		for (MatchKey matchKey : evaluations.keySet()) {
			MatchEvaluation matchEvaluation = evaluations.get(matchKey);
			response.append(matchKey.toString() + " " + matchEvaluation.toString() + "\n");
		}
		return response.toString();
	}

	public void toFile() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet();
		XSSFRow headerRow = sheet.createRow(0);

		String []columns = {"MP URI","HP URI", "jaccardSimilarityLabels", "jaccardSimilarityDefinitions", "jaccardSimilaritySynonyms" , 
				"sigmoidSimilarityLabels", "sigmoidSimilarityDefinitions", "sigmoidSimilaritySynonyms", "cosineSimilarityLabels", 
				"cosineSimilarityDefinitions", "cosineSimilaritySynonyms" ,"jaccardSubtree", "cosineSubtree", "descendantsEvaluation" , "hasLoop", "silverMapping","ourScore"};

		for (int i = 0; i < columns.length; i++) {
			XSSFCell cell = headerRow.createCell(i);
			cell.setCellValue(columns[i]);
		}
		
		int rowNum=1;
		for (MatchKey matchKey : evaluations.keySet()) {
			MatchEvaluation matchEvaluation = evaluations.get(matchKey);
			XSSFRow row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(matchKey.getX());
			row.createCell(1).setCellValue(matchKey.getY());
			row.createCell(2).setCellValue(matchEvaluation.jaccardSimilarity);
			row.createCell(3).setCellValue(matchEvaluation.jaccardSimilarity_def);
			row.createCell(4).setCellValue(matchEvaluation.jaccardSimilarity_syn);
			row.createCell(5).setCellValue(matchEvaluation.sigmoidSimilarity);
			row.createCell(6).setCellValue(matchEvaluation.sigmoidSimilarity_def);
			row.createCell(7).setCellValue(matchEvaluation.sigmoidSimilarity_syn);
			row.createCell(8).setCellValue(matchEvaluation.cosineSimilarity);
			row.createCell(9).setCellValue(matchEvaluation.cosineSimilarity_def);
			row.createCell(10).setCellValue(matchEvaluation.cosineSimilarity_syn); 
			row.createCell(11).setCellValue(matchEvaluation.jaccardSubtree);
			row.createCell(12).setCellValue(matchEvaluation.cosineSubtree);
			row.createCell(13).setCellValue(matchEvaluation.descendantsEvaluation);
			row.createCell(14).setCellValue(matchEvaluation.hasLoop);
			
			Cell cell;
			cell = row.createCell(15);
			cell.setCellValue(matchEvaluation.silverMapping);
			CellStyle style = workbook.createCellStyle();
			if(matchEvaluation.silverMapping==matchEvaluation.score) {
				style.setFillForegroundColor(IndexedColors.GREEN.getIndex());	
			}else {
				style.setFillForegroundColor(IndexedColors.RED.getIndex());
			}
			style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			cell.setCellStyle(style);

			cell = row.createCell(16);
			cell.setCellValue(matchEvaluation.score);
			if(matchEvaluation.silverMapping==matchEvaluation.score) {
				style.setFillForegroundColor(IndexedColors.GREEN.getIndex());	
			}else {
				style.setFillForegroundColor(IndexedColors.RED.getIndex());
			}
			style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			cell.setCellStyle(style);

		}
		for (int i = 0; i < columns.length; i++) {
			sheet.autoSizeColumn(i);
		}
		
		
		XSSFRow row2 = sheet.createRow(rowNum++);
		row2.createCell(0).setCellValue("Precision class 2");
		row2.createCell(1).setCellValue(calc_precision(2));
		row2.createCell(2).setCellValue("Recall class 2");
		row2.createCell(3).setCellValue(calc_recall(2));
		
		XSSFRow row3 = sheet.createRow(rowNum++);
		row3.createCell(0).setCellValue("Precision class 3");
		row3.createCell(1).setCellValue(calc_precision(3));
		row3.createCell(2).setCellValue("Recall class 3");
		row3.createCell(3).setCellValue(calc_recall(3));
		
		XSSFRow row4 = sheet.createRow(rowNum++);
		row4.createCell(0).setCellValue("Precision class 4");
		row4.createCell(1).setCellValue(calc_precision(4));
		row4.createCell(2).setCellValue("Recall class 4");
		row4.createCell(3).setCellValue(calc_recall(4));
		
		XSSFRow row5 = sheet.createRow(rowNum++);
		row5.createCell(0).setCellValue("Precision overall");
		row5.createCell(1).setCellValue(calc_precision(5));
		row5.createCell(2).setCellValue("Recall overall");
		row5.createCell(3).setCellValue(calc_recall(5));
		

		try {
			FileOutputStream fileOut = new FileOutputStream("data/results/eval.xlsx");
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private double calc_precision(int s) {
		int tp = 0;
		int fp = 0;
		if(s==5) {
			for (MatchKey matchKey : evaluations.keySet()) {
				MatchEvaluation matchEvaluation = evaluations.get(matchKey);
				if(matchEvaluation.silverMapping!=0) {
					if(matchEvaluation.silverMapping==matchEvaluation.score) {
						tp++;
					}else {
						fp++;
					}
				}
			}
			return (tp + 0.0)/(tp+fp+0.0);
			
		}
		
		for (MatchKey matchKey : evaluations.keySet()) {
			MatchEvaluation matchEvaluation = evaluations.get(matchKey);
			if(matchEvaluation.silverMapping==s) {
				if(matchEvaluation.score==s) {
					tp++;
				}
			}else if (matchEvaluation.silverMapping!=0){
				if(matchEvaluation.score==s) {
					fp++;
				}
			}
		}
		if(tp+fp==0) return 0;
		return (tp + 0.0)/(tp+fp+0.0);
	}
	private double calc_recall(int s) {
		int tp = 0;
		int fn = 0;
		
		if(s==5) {
			for (MatchKey matchKey : evaluations.keySet()) {
				MatchEvaluation matchEvaluation = evaluations.get(matchKey);
				if(matchEvaluation.silverMapping!=0) {
					if(matchEvaluation.silverMapping==matchEvaluation.score) {
						tp++;
					}else {
						fn++;
					}
				}
			}
			return (tp + 0.0)/(tp+fn+0.0);
		}
		
		for (MatchKey matchKey : evaluations.keySet()) {
			MatchEvaluation matchEvaluation = evaluations.get(matchKey);
			if(matchEvaluation.silverMapping==s) {
				if(matchEvaluation.score==s) {
					tp++;
				}else {
					fn++;
				}
			}
		}
		return (tp + 0.0)/(tp+fn+0.0);
	}

}
