/*
 * OpenSIRF
 * 
 * Copyright IBM Corporation 2016.
 * All Rights Reserved.
 * 
 * MIT License:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of a copyright holder shall not
 * be used in advertising or otherwise to promote the sale, use or other
 * dealings in this Software without prior written authorization of the
 * copyright holder.
 */

package org.opensirf.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opensirf.catalog.SIRFCatalog;
import org.opensirf.container.MagicObject;
import org.opensirf.container.ProvenanceInformation;
import org.opensirf.obj.PackagingFormat;
import org.opensirf.obj.PreservationObjectIdentifier;
import org.opensirf.obj.PreservationObjectInformation;
import org.opensirf.obj.PreservationObjectVersionIdentifier;

/**
 * @author pviana
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicApiAndClientTest {
	
	private Properties props;
	
	private static String testContainerName = "unitTestContainer";
	private static String testPOuuid = "unitTestPO";
	//private static String endpoint = "200.144.189.109:8088";
	private static String endpoint = "localhost:8088";
	private static SirfClient cli = new SirfClient(endpoint);
	
	@BeforeClass
	public static void setup() {
		cli.deleteContainer(testContainerName);
	}
	
	@Test
	public void A_createContainer() {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE A: Create container...");
		System.out.println("-------------------------------------");
		Response r = cli.createContainer(testContainerName);
		System.out.println("createContainer response: " + r.getStatus());
		Assert.assertEquals(201, r.getStatus());
		System.out.println();
	}
	
	@Test
	public void B_provenanceExistsAndIsConsistent() {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE B1: Verifying provenance info...");
		System.out.println("-------------------------------------");
		try {
			ProvenanceInformation pi = cli.getProvenance(testContainerName);
			System.out.println("provenanceExistsAndIsConsistent response: " + pi.getAuthor() + " " + pi.getTimestamp());
			Assert.assertEquals("SNIA LTR TWG", pi.getAuthor());
			System.out.println();
		} catch(XMLMarshalException ue) {
			System.out.println("ERROR unmarshalling provenance.");
		}
	}
	
	@Test
	public void B_catalogExistsAndIsConsistent() throws Exception {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE B2: Verifying catalog info...");
		System.out.println("-------------------------------------");
		SIRFCatalog cat = cli.getCatalog(testContainerName);
		System.out.println("catalogExistsAndIsConsistent response: " + cat.getCatalogId());
		
		Assert.assertEquals(1, cat.getSirfObjects().size());
		Assert.assertEquals("ready", cat.getContainerInformation().getContainerState().getContainerStateType());
		Assert.assertEquals("true", cat.getContainerInformation().getContainerState().getContainerStateValue());
		System.out.println();
	}
	
	@Test
	public void C_addPreservationObject() throws NoSuchAlgorithmException {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE C: Adding preservation object, verifying SHA1");
		System.out.println("-------------------------------------");
		
		byte[] mockPoContents = "this is a unit test po".getBytes();
		String poSha1Sum = DatatypeConverter.printHexBinary(
				MessageDigest.getInstance("SHA-1").digest(mockPoContents));
		PreservationObjectInformation poi = new PreservationObjectInformation("none");
		PreservationObjectVersionIdentifier vid = new PreservationObjectVersionIdentifier();
		vid.setObjectIdentifierType("versionIdentifier");
		vid.setObjectIdentifierLocale("en");
		vid.setObjectIdentifierValue(testPOuuid);
		PreservationObjectIdentifier id = new PreservationObjectIdentifier();
		id.setObjectVersionIdentifier(vid);
		poi.addObjectIdentifier(id);
		Response r = cli.pushPreservationObject(testContainerName, poi, mockPoContents);
		System.out.println("addPreservationObject response: " + r.getStatus());
		SIRFCatalog cat = cli.getCatalog(testContainerName);
		PreservationObjectInformation poi2 = cat.getSirfObjects().get(testPOuuid);
		PreservationObjectInformation poi3 = cli.getPreservationObjectInformation(
				testContainerName, testPOuuid);
		
		System.out.println("Asserting client's getPOInformation() == catalog.getPO()");
		Assert.assertEquals(poi2.getVersionIdentifierUUID(), poi3.getVersionIdentifierUUID());
		Assert.assertEquals(poi2.getObjectFixity().getDigestInformation().get(0).getDigestValue(),
				poi3.getObjectFixity().getDigestInformation().get(0).getDigestValue());
		
		System.out.println("SHA1 of mock PO: " + poSha1Sum);
		Assert.assertEquals(poSha1Sum.toUpperCase(), poi2.
				getObjectFixity().getDigestInformation().get(0).getDigestValue());
		
		System.out.println("Asserting PO contents are not null");
		byte[] b = cli.getPreservationObject(testContainerName, testPOuuid);
		Assert.assertNotNull(b);
		
		System.out.println("PO contents from the container: " + new String(b));
		Assert.assertEquals(new String(b), new String(mockPoContents));
		
		System.out.println();
	}
	
	@Test
	public void D_editPreservationObject() throws NoSuchAlgorithmException {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE D: Editing the PO info...");
		System.out.println("-------------------------------------");
		
		PreservationObjectInformation poi = cli.getPreservationObjectInformation(testContainerName,
				testPOuuid);
		poi.setPackagingFormat(new PackagingFormat("testPackagingFormat"));
		poi.setObjectLastAccessedDate("20160925120200");
		poi.setObjectCreationDate("20160925120201");
		
		SIRFCatalog catalog = cli.getCatalog(testContainerName);
		catalog.getSirfObjects().remove(testPOuuid);
		catalog.getSirfObjects().add(poi);
		cli.pushCatalog(testContainerName, catalog);
		
		catalog = cli.getCatalog(testContainerName);
		PreservationObjectInformation updatedPOI = catalog.getSirfObjects().get(testPOuuid);

		Assert.assertEquals(poi.getPackagingFormat().getPackagingFormatName(), updatedPOI.
				getPackagingFormat().getPackagingFormatName());
		Assert.assertEquals(poi.getObjectLastAccessedDate(), updatedPOI.getObjectLastAccessedDate());
		Assert.assertEquals(poi.getObjectCreationDate(), updatedPOI.getObjectCreationDate());
	}
	
	@Test
	public void E_deletePreservationObject() {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE E: Delete preservation object contents");
		System.out.println("-------------------------------------");
		
		// Checking PO is there
		SIRFCatalog catalog = cli.getCatalog(testContainerName);
		PreservationObjectInformation poi = catalog.getSirfObjects().get(testPOuuid);
		Assert.assertNotNull(poi);
		
		// Deletes PO file and POI from catalog
		Response r = cli.deletePreservationObject(testContainerName, testPOuuid);

		// Checking PO is no longer there
		Assert.assertEquals(200, r.getStatus());
		catalog = cli.getCatalog(testContainerName);
		poi = catalog.getSirfObjects().get(testPOuuid);
		Assert.assertNull(poi);
		
		byte[] po = cli.getPreservationObject(testContainerName, testPOuuid);
		if(po != null) {
			System.out.println("PO contents: " + new String(po));
		}
		
		System.out.println("PO contents null: " + (po == null));
		
		Assert.assertNull(po);
	}
	
	@Test
	public void F_getMagicObject() {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE F: Retrieving magic object");
		System.out.println("-------------------------------------");
		
		MagicObject mo = cli.getMagicObject(testContainerName);

		Assert.assertEquals(mo.getContainerSpecification(), "1.0");
		Assert.assertEquals(mo.getSirfCatalogId(), "catalog.json");		
	}
	
	@Test
	public void G_deleteContainer() {
		System.out.println("-------------------------------------");
		System.out.println("TEST CASE G: Deleting container");
		System.out.println("-------------------------------------");
		
		cli.deleteContainer(testContainerName);

		SIRFCatalog cat = cli.getCatalog(testContainerName);
		Assert.assertNull(cat);
	}
	
	private String getProperty(String prop) {
		return props.getProperty(prop);
	}
}
