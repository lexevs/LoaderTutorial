package org.lexevs.lexgrid.loader.tutorial;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.LexGrid.codingSchemes.CodingScheme;
import org.LexGrid.commonTypes.Properties;
import org.LexGrid.commonTypes.Property;
import org.LexGrid.commonTypes.PropertyQualifier;
import org.LexGrid.commonTypes.Text;
import org.LexGrid.concepts.Comment;
import org.LexGrid.concepts.Entities;
import org.LexGrid.concepts.Entity;
import org.LexGrid.concepts.Presentation;
import org.LexGrid.naming.Mappings;
import org.LexGrid.naming.SupportedContainerName;
import org.LexGrid.naming.SupportedProperty;
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
	  Hashtable<String, AssociationPredicate> assocPredicateNames = new Hashtable<String, AssociationPredicate>();
	  Hashtable<String, AssociationSource> assocSources = new Hashtable<String, AssociationSource>();
	  
	public CodingScheme map(URI resourceUri) throws Exception {
		Entity entity = null;
    	Property property = null;
    	AssociationTarget targetAssoc = null;
        CodingScheme scheme = new CodingScheme();
        Relations rels = new Relations();
        rels.setContainerName("defaultRelations");
        scheme.addRelations(new Relations());
        scheme.setEntities(new Entities());
        scheme.setMappings(new Mappings());
        //Add a supported container for a set of Association Predicates
        SupportedContainerName containerName = new SupportedContainerName();
        containerName.setContent("defaultContainer");
        containerName.setContent("defaultContainer");
        containerName.setUri(scheme.getCodingSchemeURI());
        scheme.getMappings().addSupportedContainerName(containerName);
        CSVParser parser = CSVParser.parse(new File(resourceUri.getPath()), Charset.defaultCharset(), CSVFormat.DEFAULT);
        for(CSVRecord rec: parser){
        	System.out.println("First record " + rec.get(0));

			switch(rec.get(0)){
        	case "Scheme": scheme.setCodingSchemeName(rec.get(1));
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
        	case "Property": property = addEntityProperty(rec, entity);
        	break;
        	case "qualifier" : addPropertyQualifier(rec, property);
        	break;
        	case "assocId": targetAssoc = addAssociation(rec, scheme);
        	break;
        	case "restriction": addAssociationQualifier(rec, targetAssoc);
        	}

        }
        
		Iterator<Map.Entry<String, AssociationPredicate>> itr = assocPredicateNames.entrySet().iterator();
		while(itr.hasNext()){
			scheme.getRelations(0).addAssociationPredicate(itr.next().getValue());
		}
		return scheme;
	}
	
	private void addAssociationQualifier(CSVRecord rec, AssociationTarget targetAssoc) {
		AssociationQualification qual = new AssociationQualification();
		qual.setAssociationQualifier(rec.get(0));
		Text text = new Text();
		text.setContent(rec.get(1));
		qual.setQualifierText(text);
		targetAssoc.addAssociationQualification(qual);
		
	}

	private AssociationTarget addAssociation(CSVRecord rec, CodingScheme scheme) {
		AssociationSource source = null;
		AssociationPredicate ap = null;
		AssociationTarget target = null;
		if(!assocSources.containsKey(rec.get(2))){
		source = new AssociationSource();
		source.setSourceEntityCode(rec.get(2));
		source.setSourceEntityCodeNamespace(scheme.getCodingSchemeName());
		assocSources.put(rec.get(2), source);
		}
		else{
			source = assocSources.get(rec.get(2));
		}
		target = new AssociationTarget();
		target.setTargetEntityCode(rec.get(4));
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


	private void addPropertyQualifier(CSVRecord rec, Property property) {
		PropertyQualifier qual = new PropertyQualifier();
		qual.setPropertyQualifierName(rec.get(0));
		Text value = new Text();
		value.setContent(rec.get(1));
		qual.setValue(value);
		property.addPropertyQualifier(qual);
	}

	private Property addEntityProperty(CSVRecord rec, Entity entity) {
		Property prop = null;
		switch (rec.get(1)) {
		case "Presentation":
			prop = new Presentation();
			if (StringUtils.isNotBlank(rec.get(3)) && rec.get(3).equals("preferred")) {
				((Presentation) prop).setIsPreferred(true);
			}
			entity.addPresentation((Presentation)prop);
			break;
		case "Commment":
			prop = new Comment();
			entity.addComment((Comment)prop);
		default:
			prop = new Property();
			entity.addProperty(prop);;
			//supported property stuff should go here
		}
		prop.setPropertyName(rec.get(1));
		Text value = new Text();
		value.setContent(rec.get(2));
		prop.setValue(value);
		entity.addAnyProperty(prop);
		return prop;
	}

	private Entity setEntity(CSVRecord rec, CodingScheme scheme) {
		Entity entity = new Entity();
		entity.setEntityCode(rec.get(1));
		entity.setEntityCodeNamespace(scheme.getCodingSchemeName());
		scheme.getEntities().addEntity(entity);
		return entity;
	}

	private void setAuthorProperty(CSVRecord rec, CodingScheme scheme) {
		Property prop = new Property();
		prop.setPropertyName(rec.get(0));
		Text text = new Text();
		text.setContent(rec.get(1));
		prop.setValue(text);
		SupportedProperty suppProp = new SupportedProperty();
		suppProp.setContent(rec.get(1));
		suppProp.setLocalId(rec.get(0));
		suppProp.setUri(scheme.getCodingSchemeURI() != null? scheme.getCodingSchemeURI(): "");
		scheme.getMappings().addSupportedProperty(suppProp);
		Properties props = new Properties();
		scheme.setProperties(props);
		scheme.getProperties().addProperty(prop);
		
	}

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
