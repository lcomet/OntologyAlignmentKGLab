# KGlab project

## Goal of this project
[BioPortal](http://bioportal.bioontology.org/) is a repository of biomedical ontologies. A lot of these ontologies cover similiar domains, so there are partial overlaps between them.
In BioPortal, mappings between these ontologies exist, which express which nodes in both ontologies are matching. A match between two nodes is recognized with one of following three methodologies:
- Same URI: Both nodes have the same URI, and, therefore, should be matching
- [LOOM](https://www.ncbi.nlm.nih.gov/pubmed/20351849): A matching was recognized with the LOOM algorithm
- Expert opinion: A domain expert verified, that these nodes are matching

There is a great percentage of matchings which were detected by the LOOM algorithm, and some of them are wrong, especially for semantic reasons.

Therefore, our main goal is to evaluate the quality of the existing matchings on BioPortal. For this, we want to focus on providing quality measures indicating how appropriate are those matches.
Being able to detect wrong matches, we will hopefully be able to increase the general quality of the available mappings.

## Our proposal
As a first step, our approach is the following:
- Evaluation of existent links between ontologies by applying metrics and comparing their results.
  The metrics we are implementing in this project are based on structural similarity (considering similarity between descendants/ancestors, etc.), and terminological similarity (taking into account the similarity between preferred labels, and other descriptions)
- Testing of our findings against mappings corresponding to the silver standard from Pistoia.
- Evaluation of the possible new links that possibly should be included as matching between two ontologies, and those that may should deleted because they are considered to be a bad match.
- Report of our findings

## Ontology selection criteria
In order to evaluate the suitability of the matches in the BioPortal, we select ontologies based on the following criteria:
- We selected the ontologies based on higher similarity among them, based on the information provided by the BioPortal API.
- We included the ontologies [HP](https://bioportal.bioontology.org/ontologies/HP) and [MP](https://bioportal.bioontology.org/ontologies/MP), in order to test our evaluation against the provided mappings from Pistoia.
It is worth to mention that we selected these ontologies as a sample for our evaluation, and based on the criteria, we considered that they represent a good candidate of the ontologies and mappings in the BioPortal.

## Execution of the code
Our code is written in java, and the main class can be found in [src/main/ListOntologies.java](https://git.rwth-aachen.de/tobias.raewer/kglab-project/blob/aa0f0684ef862b1e6c70e8c3db00b57f3f72aa1d/src/main/ListOntologies.java).
Manipulating the parameters hpmp and ordodoid, the user can choose if he wants to run the code for the [HP](https://bioportal.bioontology.org/ontologies/HP) and [MP](https://bioportal.bioontology.org/ontologies/MP) ontologies or for the [ORDO](https://bioportal.bioontology.org/ontologies/ORDO) and [DOID](https://bioportal.bioontology.org/ontologies/DOID) ones. Additionally, it is possible to evaluate different ontologies, but then a reference alignment has to be provided.
The code runs on Java 10 and Maven 3.6.1 was used as a package manager.

## Further questions
In case of further questions, please don't hesitate to contact one of the developers under tobias.raewer@rwth-aachen.de ,lina.molinas.comet@rwth-aachen.de or
jonas.ruelfing@rwth-aachen.de.
