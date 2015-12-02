package org.lexevs.lexgrid.loader.tutorial;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

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
	  Hashtable<String, AssociationPredicate> assocPredicateNames = new Hashtable<String, AssociationPredicate>();
	  Hashtable<String, AssociationSource> assocSources = new Hashtable<String, AssociationSource>();
	  Hashtable<String, SupportedAssociation> supportedAssoc = new Hashtable<String, SupportedAssociation>();
	  Hashtable<String, SupportedProperty> supportedProps = new Hashtable<String, SupportedProperty>();
	  Hashtable<String, SupportedAssociationQualifier> supportedAssocQual = new Hashtable<String, SupportedAssociationQualifier>();
	  Hashtable<String, SupportedPropertyQualifier> supportedPropQual = new Hashtable<String, SupportedPropertyQualifier>();
	  
	  
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
			scheme.getRelations(0).addAssociationPredicate(itr.next().getValue());
			SupportedAssociation spAss = new SupportedAssociation();
			spAss.setCodingScheme(scheme.getCodingSchemeName());
			spAss.setContent(predicate.getAssociationName());
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


	private void addPropertyQualifier(CSVRecord rec, Property property, CodingScheme scheme) {
		PropertyQualifier qual = new PropertyQualifier();
		qual.setPropertyQualifierName(rec.get(0));
		Text value = new Text();
		value.setContent(rec.get(1));
		qual.setValue(value);
		property.addPropertyQualifier(qual);
		if(supportedProps.containsKey(rec.get(0))){
		SupportedPropertyQualifier suppPropQ = new SupportedPropertyQualifier();
		suppPropQ.setContent(rec.get(0));
		suppPropQ.setLocalId(rec.get(0));
		suppPropQ.setUri(scheme.getCodingSchemeURI());
		supportedPropQual.put(rec.get(0), suppPropQ);
		}
	}

	private Property addEntityProperty(CSVRecord rec, Entity entity, CodingScheme scheme) {
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
			entity.addProperty(prop);
			
			//supported property stuff should go here
		}
		prop.setPropertyName(rec.get(1));
		Text value = new Text();
		value.setContent(rec.get(2));
		prop.setValue(value);
		entity.addAnyProperty(prop);
		if(!supportedProps.contains(value)){
			SupportedProperty supProp = new SupportedProperty();
			supProp.setContent(rec.get(1));
			supProp.setLocalId(rec.get(1));
			supProp.setUri(scheme.getCodingSchemeURI());
			supportedProps.put(rec.get(1), supProp);
		}
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
