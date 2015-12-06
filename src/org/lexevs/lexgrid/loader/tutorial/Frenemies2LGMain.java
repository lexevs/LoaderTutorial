package org.lexevs.lexgrid.loader.tutorial;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.LexGrid.codingSchemes.CodingScheme;
import org.LexGrid.commonTypes.EntityDescription;
import org.LexGrid.commonTypes.Properties;
import org.LexGrid.commonTypes.Property;
import org.LexGrid.commonTypes.PropertyQualifier;
import org.LexGrid.commonTypes.Text;
import org.LexGrid.concepts.Comment;
import org.LexGrid.concepts.Entities;
import org.LexGrid.concepts.Entity;
import org.LexGrid.concepts.Presentation;
import org.LexGrid.naming.Mappings;
import org.LexGrid.naming.SupportedAssociation;
import org.LexGrid.naming.SupportedAssociationQualifier;
import org.LexGrid.naming.SupportedCodingScheme;
import org.LexGrid.naming.SupportedContainerName;
import org.LexGrid.naming.SupportedNamespace;
import org.LexGrid.naming.SupportedProperty;
import org.LexGrid.naming.SupportedPropertyQualifier;
import org.LexGrid.relations.AssociationPredicate;
import org.LexGrid.relations.AssociationQualification;
import org.LexGrid.relations.AssociationSource;
import org.LexGrid.relations.AssociationTarget;
import org.LexGrid.relations.Relations;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

public class Frenemies2LGMain {
	//Create some contianers to help track supported assertions in this coding scheme
	  Hashtable<String, AssociationPredicate> assocPredicateNames = new Hashtable<String, AssociationPredicate>();
	  Hashtable<String, AssociationSource> assocSources = new Hashtable<String, AssociationSource>();
	  Hashtable<String, SupportedAssociation> supportedAssoc = new Hashtable<String, SupportedAssociation>();
	  Hashtable<String, SupportedProperty> supportedProps = new Hashtable<String, SupportedProperty>();
	  Hashtable<String, SupportedAssociationQualifier> supportedAssocQual = new Hashtable<String, SupportedAssociationQualifier>();
	  Hashtable<String, SupportedPropertyQualifier> supportedPropQual = new Hashtable<String, SupportedPropertyQualifier>();
	  
	  
	  
	/**
	 * @param resourceUri
	 * @return CodingScheme
	 * @throws Exception
	 * 
	 * Main entry point into the class which provides a coding scheme for 
	 * LexEVS to process into data base and lucene index entries
	 */
	public CodingScheme map(URI resourceUri) throws Exception {
		
		//Some elements need to have references in this scope
		//to allow continued processing
		Entity entity = null;
    	Property property = null;
    	AssociationTarget targetAssoc = null;
    	
    	//Begin to assert coding scheme metadata
    	//This is a curation activity and requires
    	//input from the source author
        CodingScheme scheme = new CodingScheme();
        scheme.setApproxNumConcepts(6L);
        scheme.setDefaultLanguage("en");
        scheme.setIsActive(true);
        EntityDescription ed = new EntityDescription();
        ed.setContent("description");
        scheme.setEntityDescription(ed);
        
        //Adding a container for associations
        Relations rels = new Relations();
        rels.setContainerName("defaultRelations");
        scheme.addRelations(rels);
        
        //Initializing container for Entities
        scheme.setEntities(new Entities());
        //Initializing a container for assertions
        //made by the coding scheme
        scheme.setMappings(new Mappings());
        
        //Add a supported container for a set of Association Predicates
        SupportedContainerName containerName = new SupportedContainerName();
        containerName.setContent("defaultRelations");
        containerName.setLocalId("defaultRelations");
        containerName.setUri(scheme.getCodingSchemeURI());
        scheme.getMappings().addSupportedContainerName(containerName);
        
        //Using a parsing library to process line by line
        //the comma separated value file
        CSVParser parser = CSVParser.parse(new File(resourceUri.getPath()), Charset.defaultCharset(), CSVFormat.DEFAULT);
        for(CSVRecord rec: parser){
        	System.out.println("First record " + rec.get(0));

			switch(rec.get(0)){
        	case "Scheme": scheme.setCodingSchemeName(rec.get(1));
            scheme.setFormalName(rec.get(1));
        	break;
        	case "Version": scheme.setRepresentsVersion(rec.get(1));
        	break;
        	case "URI": scheme.setCodingSchemeURI(rec.get(1));
        	break;
        	case "CopyRight": setCopyright(rec.get(1), scheme);
        	break;
        	case "Author": setAuthorProperty(rec, scheme);
        	break;
        	case "Id": entity = setEntity(rec, scheme);
        	break;
        	case "Property": property = addEntityProperty(rec, entity, scheme);
        	break;
        	case "qualifier" : addPropertyQualifier(rec, property, scheme);
        	break;
        	case "assocId": targetAssoc = addAssociation(rec, scheme);
        	break;
        	case "restriction": addAssociationQualifier(rec, targetAssoc, scheme);
        	}

        }
        //Start creating meta data assertions
        //Add supported coding scheme for enclosing code system
        SupportedCodingScheme suppCodingScheme = new SupportedCodingScheme();
        suppCodingScheme.setContent(scheme.getCodingSchemeName());
        suppCodingScheme.setIsImported(false);
        suppCodingScheme.setLocalId(scheme.getCodingSchemeName());
        suppCodingScheme.setUri(scheme.getCodingSchemeURI());
        scheme.getMappings().addSupportedCodingScheme(suppCodingScheme);
        //Add supported namespace for this code system
        //This is where you could add other supported namespaces
        SupportedNamespace suppNS = new SupportedNamespace();
        suppNS.setContent(scheme.getCodingSchemeName());
        suppNS.setEquivalentCodingScheme(scheme.getCodingSchemeName());
        suppNS.setLocalId(scheme.getCodingSchemeName());
        suppNS.setUri(scheme.getCodingSchemeURI());
        scheme.getMappings().addSupportedNamespace(suppNS);
        //Add predicates and supported associations
		Iterator<Map.Entry<String, AssociationPredicate>> itr = assocPredicateNames.entrySet().iterator();
		while(itr.hasNext()){
			AssociationPredicate predicate = itr.next().getValue();
			scheme.getRelations(0).addAssociationPredicate(predicate);
			SupportedAssociation spAss = new SupportedAssociation();
			spAss.setCodingScheme(scheme.getCodingSchemeName());
			spAss.setContent(predicate.getAssociationName());
			spAss.setLocalId(predicate.getAssociationName());
			spAss.setEntityCodeNamespace(scheme.getCodingSchemeName());
			spAss.setUri(scheme.getCodingSchemeURI());
			scheme.getMappings().addSupportedAssociation(spAss);
		}
		
		//Supported Properties
		Iterator<Map.Entry<String, SupportedProperty>> propItr = supportedProps.entrySet().iterator();
		while(propItr.hasNext()){
			scheme.getMappings().addSupportedProperty(propItr.next().getValue());
		}
		
		//Supported Property Qualifiers
		Iterator<Map.Entry<String, SupportedPropertyQualifier>> propQualItr = supportedPropQual.entrySet().iterator();
		while(propQualItr.hasNext()){
			scheme.getMappings().addSupportedPropertyQualifier(propQualItr.next().getValue());
		}
		//Supported Association Qualifiers
		Iterator<Map.Entry<String, SupportedAssociationQualifier>> assQualItr = supportedAssocQual.entrySet().iterator();
		while(assQualItr.hasNext()){
			scheme.getMappings().addSupportedAssociationQualifier(assQualItr.next().getValue());
		}
		
		return scheme;
	}
	
	//Boiler plate for association qualifiers
	//also tracking the coding scheme assertions about
	//what is supported as association qualifiers
	private void addAssociationQualifier(CSVRecord rec, AssociationTarget targetAssoc, CodingScheme scheme) {
		AssociationQualification qual = new AssociationQualification();
		qual.setAssociationQualifier(rec.get(0));
		Text text = new Text();
		text.setContent(rec.get(1));
		qual.setQualifierText(text);
		targetAssoc.addAssociationQualification(qual);
		SupportedAssociationQualifier supAsscQ = new SupportedAssociationQualifier();
		supAsscQ.setContent(rec.get(0));
		supAsscQ.setLocalId(rec.get(0));
		supAsscQ.setUri(scheme.getCodingSchemeURI());
		if(!supportedAssocQual.containsKey(rec.get(0))){
			supportedAssocQual.put(rec.get(0), supAsscQ);
		}
		
	}

	//Boiler plate allowing the creation of an association 
	//Predicates are tracked for later inclusion in predicate table
	//as well as assertions about supported associations
	private AssociationTarget addAssociation(CSVRecord rec, CodingScheme scheme) {
		AssociationSource source = null;
		AssociationPredicate ap = null;
		AssociationTarget target = null;

		source = new AssociationSource();
		source.setSourceEntityCode(rec.get(2));
		source.setSourceEntityCodeNamespace(scheme.getCodingSchemeName());

		target = new AssociationTarget();
		target.setTargetEntityCode(rec.get(4));
		target.setTargetEntityCodeNamespace(scheme.getCodingSchemeName());
		source.getTargetAsReference().add(target);
		
		if(!assocPredicateNames.containsKey(rec.get(3))){
			ap = new AssociationPredicate();
			ap.setAssociationName(rec.get(3));
			assocPredicateNames.put(rec.get(3), ap);
			if(!ap.getSourceAsReference().contains(source)){
			ap.addSource(source);
			}
		}
		else{
			ap = assocPredicateNames.get(rec.get(3));
			if(!ap.getSourceAsReference().contains(source)){
			ap.addSource(source);
			}
		}
		
		return target;
	}


	//Boiler plate for property qualifier createion
	//Usual assertion tracking 
	private void addPropertyQualifier(CSVRecord rec, Property property, CodingScheme scheme) {
		PropertyQualifier qual = new PropertyQualifier();
		qual.setPropertyQualifierName(rec.get(1));
		Text value = new Text();
		value.setContent(rec.get(2));
		qual.setValue(value);
		property.addPropertyQualifier(qual);
		if(supportedPropQual.containsKey(rec.get(1))){
		SupportedPropertyQualifier suppPropQ = new SupportedPropertyQualifier();
		suppPropQ.setContent(rec.get(1));
		suppPropQ.setLocalId(rec.get(1));
		suppPropQ.setUri(scheme.getCodingSchemeURI());
		supportedPropQual.put(rec.get(1), suppPropQ);
		}
	}

	//Boiler plate for property creation
	//Assertions about supported properties tracked as well
	private Property addEntityProperty(CSVRecord rec, Entity entity, CodingScheme scheme) {
		Property prop = null;
		EntityDescription ed = new EntityDescription();
		switch (rec.get(1)) {
		case "Presentation":
			prop = new Presentation();
			
			if (StringUtils.isNotBlank(rec.get(3)) && rec.get(3).equals("preferred")) {
				((Presentation) prop).setIsPreferred(true);
			}
			entity.addAnyProperty((Presentation)prop);
			
			break;
		case "Commment":
			prop = new Comment();
			entity.addAnyProperty((Comment)prop);
		default:
			prop = new Property();
			entity.addAnyProperty(prop);

		}
		prop.setPropertyName(rec.get(1));
		Text value = new Text();
		value.setContent(rec.get(2));
		prop.setValue(value);
		if(!supportedProps.contains(value)){
			SupportedProperty supProp = new SupportedProperty();
			supProp.setContent(rec.get(1));
			supProp.setLocalId(rec.get(1));
			supProp.setUri(scheme.getCodingSchemeURI());
			supportedProps.put(rec.get(1), supProp);
		}

		if(entity.getEntityDescription() == null)
		{		ed.setContent(rec.get(2));
			entity.setEntityDescription(ed);
		}
		return prop;
	}

	//Entity creation boiler plate
	private Entity setEntity(CSVRecord rec, CodingScheme scheme) {
		Entity entity = new Entity();
		entity.setEntityCode(rec.get(1));
		entity.setEntityCodeNamespace(scheme.getCodingSchemeName());
		scheme.getEntities().addEntity(entity);
		return entity;
	}

	//Some coding scheme metadata boiler plate
	private void setAuthorProperty(CSVRecord rec, CodingScheme scheme) {
		Property prop = new Property();
		prop.setPropertyName(rec.get(0));
		Text text = new Text();
		text.setContent(rec.get(1));
		prop.setValue(text);
		SupportedProperty suppProp = new SupportedProperty();
		suppProp.setContent(rec.get(0));
		suppProp.setLocalId(rec.get(0));
		suppProp.setUri(scheme.getCodingSchemeURI() != null? scheme.getCodingSchemeURI(): "");
		scheme.getMappings().addSupportedProperty(suppProp);
		Properties props = new Properties();
		scheme.setProperties(props);
		scheme.getProperties().addProperty(prop);
		
	}

	//Creating a copyright element for the
	//coding scheme
	private void setCopyright(String string, CodingScheme scheme) {
		Text text = new Text();
		text.setContent(string);
		scheme.setCopyright(text);
	}

	public static void main(String[] args){
		try {
			CodingScheme scheme = new Frenemies2LGMain().map(new URI("MyCodingScheme.csv"));
			scheme.getApproxNumConcepts();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
