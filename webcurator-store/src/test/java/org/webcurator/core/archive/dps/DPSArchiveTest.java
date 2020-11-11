package org.webcurator.core.archive.dps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.webcurator.core.archive.Constants.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.govt.natlib.ndha.wctdpsdepositor.CustomDepositField;
import nz.govt.natlib.ndha.wctdpsdepositor.CustomDepositFormMapping;
import nz.govt.natlib.ndha.wctdpsdepositor.DpsDepositProxy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.webcurator.core.archive.ArchiveFile;
import org.webcurator.core.archive.dps.DPSArchive.DepData;
import org.webcurator.core.archive.dps.DpsDepositFacade.DepositResult;
import org.webcurator.domain.model.core.CustomDepositFormCriteriaDTO;
import org.webcurator.domain.model.core.CustomDepositFormResultDTO;

public class DPSArchiveTest {

    private DpsDepositProxy mockDpsDepositFacade;
    private File[] testFiles = new File[]{
            new File("FileOne"),
            new File("FileTwo")
    };
    private long expectedSipId = 100;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGetDpsDepositFacade() {
        DPSArchive archiver = new DPSArchive();
        try {
            /*
             * This method will throw ClassNotFoundException or NoClassDefFoundError
             * if the DpsDepositProxy is not in the class path. In that case, don't
             * fail the test, but print a message and the Throwable details.
             */
            DpsDepositFacade facade = archiver.getDpsDepositFacade();
            assertNotNull(facade);
            assertEquals("nz.govt.natlib.ndha.wctdpsdepositor.DpsDepositProxy", facade.getClass().getName());
        } catch (Throwable e) {
            System.out.println("Error loading the DpsDepositFacade implementation");
            e.printStackTrace();
        }
    }

    @Test
    public void testGetCustomDepositFormDetails() throws Exception {
        String customFormUrl_eJournal = "/some/nice/url/eJournal";
        String customFormUrl_eManuscript = "/some/nice/url/eManuscript";
        String customFormUrl_Blog = "/some/nice/url/blog";
        String customFormUrls = customFormUrl_eJournal + ", " + customFormUrl_eManuscript + ", " + customFormUrl_Blog;
        DPSArchive archiver;
        CustomDepositFormResultDTO result;
        CustomDepositFormCriteriaDTO criteria;

        // Agency ID is different from (ignoring case) the HTML serial agencies configured in DPS Archive.
        // Target DC type of harvest is equal to (ignore case) one of the HTML serial types configured in DPS Archive
        // Expect true for the isCustomDepositFormRequired(), a proper value for the URL of custom form.
        archiver = new DPSArchive();
        archiver.setCustomDepositFormURLsForHtmlSerialIngest(customFormUrls);
        archiver.setTargetDCTypesOfHtmlSerials("HTML Serial Type 1 - eJournals, HTML Serial Type 2 - Manuscripts , HTML Serial Type 3 - Blogs    ");
        archiver.setAgenciesResponsibleForHtmlSerials("   Electronic Journals   ,  Electronic Serials     ");
        criteria = new CustomDepositFormCriteriaDTO();
        criteria.setAgencyName("some agency");
        criteria.setTargetType("HTML Serial Type 2 - maNuScripts");
        result = archiver.getCustomDepositFormDetails(criteria);
        assertNotNull(result);
        assertEquals(true, result.isCustomDepositFormRequired());
        assertEquals(customFormUrl_eManuscript, result.getUrlForCustomDepositForm());
        assertNull(result.getHTMLForCustomDepositForm());

        // Agency ID is same as (ignoring case) one of the HTML serial agencies configured in DPS Archive
        // Target DC type of harvest is different from (ignoring case) the HTML serial types configured in DPS Archive
        // Expect true for the isCustomDepositFormRequired(), and "invalid dc type JSP" for the URL of custom form.
        archiver = new DPSArchive();
        archiver.setCustomDepositFormURLsForHtmlSerialIngest(customFormUrls);
        archiver.setTargetDCTypesOfHtmlSerials("HTML Serial Type 1 - eJournals, HTML Serial Type 2 - Manuscripts , HTML Serial Type 3 - Blogs    ");
        archiver.setAgenciesResponsibleForHtmlSerials("   Electronic Journals   ,  Electronic Serials     ");
        archiver.setRestrictHTMLSerialAgenciesToHTMLSerialTypes("true");
        criteria = new CustomDepositFormCriteriaDTO();
        criteria.setAgencyName("eLectroniC SeRials");
        criteria.setTargetType("some target DC type");
        result = archiver.getCustomDepositFormDetails(criteria);
        assertNotNull(result);
        assertEquals(true, result.isCustomDepositFormRequired());
        assertEquals("/customDepositForms/rosetta_custom_deposit_form_invalid_dctype.jsp", result.getUrlForCustomDepositForm());
        assertNull(result.getHTMLForCustomDepositForm());

        // Agency ID is same as (ignoring case) one of the HTML serial agencies configured in DPS Archive
        // Target DC type of harvest is equal to (ignore case) one of the HTML serial types configured in DPS Archive
        // Expect true for the isCustomDepositFormRequired(), a proper value for the URL of custom form.
        archiver = new DPSArchive();
        archiver.setCustomDepositFormURLsForHtmlSerialIngest(customFormUrls);
        archiver.setTargetDCTypesOfHtmlSerials("HTML Serial Type 1 - eJournals, HTML Serial Type 2 - Manuscripts , HTML Serial Type 3 - Blogs    ");
        archiver.setAgenciesResponsibleForHtmlSerials("   Electronic Journals   ,  Electronic Serials     ");
        criteria = new CustomDepositFormCriteriaDTO();
        criteria.setAgencyName("eLectroniC SeRials");
        criteria.setTargetType("HTML Serial Type 2 - maNuScripts");
        result = archiver.getCustomDepositFormDetails(criteria);
        assertNotNull(result);
        assertEquals(true, result.isCustomDepositFormRequired());
        assertEquals(customFormUrl_eManuscript, result.getUrlForCustomDepositForm());
        assertNull(result.getHTMLForCustomDepositForm());

        // Agency ID and target DC type are different from the HTML serial types and agencies configured in DPS Archive
        // Expect false for the isCustomDepositFormRequired(), null for the URL and HTML of custom form.
        archiver = new DPSArchive();
        archiver.setCustomDepositFormURLsForHtmlSerialIngest(customFormUrls);
        archiver.setTargetDCTypesOfHtmlSerials("HTML Serial Type 1 - eJournals, HTML Serial Type 2 - Manuscripts , HTML Serial Type 3 - Blogs    ");
        archiver.setAgenciesResponsibleForHtmlSerials("   Electronic Journals   ,  Electronic Serials     ");
        criteria = new CustomDepositFormCriteriaDTO();
        criteria.setAgencyName("some agency");
        criteria.setTargetType("some target DC type");
        result = archiver.getCustomDepositFormDetails(criteria);
        assertNotNull(result);
        assertEquals(false, result.isCustomDepositFormRequired());
        assertNull(result.getUrlForCustomDepositForm());
        assertNull(result.getHTMLForCustomDepositForm());

        // Null criteria object
        archiver = new DPSArchive();
        archiver.setCustomDepositFormURLsForHtmlSerialIngest(customFormUrls);
        archiver.setTargetDCTypesOfHtmlSerials("HTML Serial Type 1 - eJournals, HTML Serial Type 2 - Manuscripts , HTML Serial Type 3 - Blogs    ");
        archiver.setAgenciesResponsibleForHtmlSerials("   Electronic Journals   ,  Electronic Serials     ");
        criteria = null;
        result = archiver.getCustomDepositFormDetails(criteria);
        assertNotNull(result);
        assertEquals(false, result.isCustomDepositFormRequired());
        assertNull(result.getUrlForCustomDepositForm());
        assertNull(result.getHTMLForCustomDepositForm());
    }

    @Test
    public void testValidateMaterialFlowAssociation() {
        DPSArchive archiver;

        // No material flows are associated with the producer
        archiver = new DPSArchive() {
            public DepData[] getMaterialFlows(String producerID) {
                return null;
            }
        };
        archiver.setTargetDCTypesOfHtmlSerials("eJournal, eManuscript, eSerial");
        archiver.setMaterialFlowsOfHtmlSerials("1111, 2222, 3333");
        assertFalse(archiver.validateMaterialFlowAssociation("1234", "eSerial"));

        // The producer is not associated with the material flow of the requested target DC type
        archiver = new DPSArchive() {
            public DepData[] getMaterialFlows(String producerID) {
                DepData[] depData = new DepData[10];
                for (int i = 0; i < depData.length; i++) {
                    depData[i] = mockDepData("" + i, "Description for " + i);
                }
                return depData;
            }
        };
        archiver.setTargetDCTypesOfHtmlSerials("eJournal, eManuscript, eSerial");
        archiver.setMaterialFlowsOfHtmlSerials("1111, 2222, 3333");
        assertFalse(archiver.validateMaterialFlowAssociation("1234", "eSerial"));

        // Requested target DC type is not of an HTML serial type
        assertFalse(archiver.validateMaterialFlowAssociation("1234", "someOtherDcType"));

        // The perfect situation - the producer is associated with the correct material flow
        archiver.setMaterialFlowsOfHtmlSerials("1111, 2222, 5");
        assertTrue(archiver.validateMaterialFlowAssociation("1234", "eSerial"));
    }

    @Test
    public void testToListOfLowerCaseValues() {
        List<String> output;

        // 3 tokens, some with space
        output = DPSArchive.toListOfLowerCaseValues("HTML Serial Type 1 - eJournals, HTML Serial Type 2 - Manuscripts , HTML Serial Type 3 - Blogs    ");
        assertNotNull(output);
        assertEquals(3, output.size());
        assertEquals("html serial type 1 - ejournals", output.get(0));
        assertEquals("html serial type 2 - manuscripts", output.get(1));
        assertEquals("html serial type 3 - blogs", output.get(2));

        // 1 token - no leading/trailing space
        output = DPSArchive.toListOfLowerCaseValues("Electronic Journals and Serials");
        assertNotNull(output);
        assertEquals(1, output.size());
        assertEquals("electronic journals and serials", output.get(0));

        // 1 token - with leading/trailing space
        output = DPSArchive.toListOfLowerCaseValues("   Electronic Journals and Serials      	   		");
        assertNotNull(output);
        assertEquals(1, output.size());
        assertEquals("electronic journals and serials", output.get(0));

        // Null string
        output = DPSArchive.toListOfLowerCaseValues(null);
        assertNotNull(output);
        assertEquals(0, output.size());

        // empty string content
        output = DPSArchive.toListOfLowerCaseValues("     		    ");
        assertNotNull(output);
        assertEquals(0, output.size());

        // comma-separated set of empty string contents
        output = DPSArchive.toListOfLowerCaseValues("     		    ,  		        ,    	    ");
        assertNotNull(output);
        assertEquals(0, output.size());
    }

    @Test
    public void testPopulateDepositParameterFromFields_webHarvest() {
        DPSArchive archiver = new DPSArchive();
        setVariousParameters(archiver);

        Map<String, String> attributes = mockDasAttributeMap();
        String finalSIP = "someFinalSIPXml";
        String targetInstanceOID = "112233";

        Map<String, String> parameters = archiver.populateDepositParameterFromFields(attributes, finalSIP, targetInstanceOID);

        assertEquals("aDpsUserInstitution", parameters.get(DpsDepositFacade.DPS_INSTITUTION));
        assertEquals("aDpsUserName", parameters.get(DpsDepositFacade.DPS_USER_NAME));
        assertEquals("aDpsUserPassword", parameters.get(DpsDepositFacade.DPS_PASSWORD));
        assertEquals("aFtpHost", parameters.get(DpsDepositFacade.FTP_HOST));
        assertEquals("aFtpPassword", parameters.get(DpsDepositFacade.FTP_PASSWORD));
        assertEquals("aFtpUserName", parameters.get(DpsDepositFacade.FTP_USER_NAME));
        assertEquals("aFtpDirectory", parameters.get(DpsDepositFacade.FTP_DIRECTORY));
        assertEquals("aMaterialFlowId", parameters.get(DpsDepositFacade.MATERIAL_FLOW_ID));
        assertEquals("aPdsUrl", parameters.get(DpsDepositFacade.PDS_URL));
        assertEquals("aProducerId", parameters.get(DpsDepositFacade.PRODUCER_ID));
        assertEquals("http://someserver.natlib.govt.nz:80000/dpsws/deposit/DepositWebServices?wsdl", parameters.get(DpsDepositFacade.DPS_WSDL_URL));
        assertEquals("112233", parameters.get(DpsDepositFacade.TARGET_INSTANCE_ID));
        assertEquals("someFinalSIPXml", parameters.get(DpsDepositFacade.WCT_METS_XML_DOCUMENT));
        assertEquals("1234567890", parameters.get(DpsDepositFacade.ILS_REFERENCE));
        assertEquals("anAccessRestriction", parameters.get(DpsDepositFacade.ACCESS_RESTRICTION));
        assertEquals("TraditionalWebHarvest", parameters.get(DpsDepositFacade.HARVEST_TYPE));
    }

    @Test
    public void testPopulateDepositParameterFromFields_htmlSerialHarvest() {
        DPSArchive archiver = new DPSArchive();
        setVariousParameters(archiver);
        archiver.setTargetDCTypesOfHtmlSerials("eJournal, HtmlSerialHarvest, eSerial");
        archiver.setMaterialFlowsOfHtmlSerials("1111, anHtmlSerialMaterialFlowId, 3333");
        archiver.setIeEntityTypesOfHtmlSerials("OneHTMLSerialIeEntityType, TwoHTMLSerialIeEntityType, ThreeHTMLSerialIeEntityType");
        archiver.setCustomDepositFormURLsForHtmlSerialIngest("/customDepositForms/eJournal_form.jsp, /customDepositForms/anHtmlSerialTargetDcType_form.jsp, /customDepositForms/eSerial_form.jsp");

        // Custom Deposit Form Mapping
        List<CustomDepositField> customDepositFieldsForTest = new ArrayList<CustomDepositField>();
        customDepositFieldsForTest.add(new CustomDepositField("customDepositForm_bibliographicCitation", "DctermsBibliographicCitation", "bibliographicCitation", "dcterms"));
        customDepositFieldsForTest.add(new CustomDepositField("customDepositForm_dctermsAvailable", "DctermsAvailable", "available", "dcterms"));

        Map<String, List<CustomDepositField>> customDepositFieldMap = new HashMap<String, List<CustomDepositField>>();
        customDepositFieldMap.put("/customDepositForms/anHtmlSerialTargetDcType_form.jsp", customDepositFieldsForTest);

        CustomDepositFormMapping testCustomDepositFormMapping = new CustomDepositFormMapping();
        testCustomDepositFormMapping.setCustomDepositFormFieldMaps(customDepositFieldMap);
        archiver.setCustomDepositFormMapping(testCustomDepositFormMapping);


        Map<String, String> attributes = mockDasAttributeMapForHtmlSerials();
        String finalSIP = "someFinalSIPXml";
        String targetInstanceOID = "112233";

        Map<String, String> parameters = archiver.populateDepositParameterFromFields(attributes, finalSIP, targetInstanceOID);

        assertEquals("aDpsUserInstitution", parameters.get(DpsDepositFacade.DPS_INSTITUTION));
        assertEquals("anHtmlSerialUserName", parameters.get(DpsDepositFacade.DPS_USER_NAME));
        assertEquals("anHtmlSerialUserPassword", parameters.get(DpsDepositFacade.DPS_PASSWORD));
        assertEquals("aFtpHost", parameters.get(DpsDepositFacade.FTP_HOST));
        assertEquals("aFtpPassword", parameters.get(DpsDepositFacade.FTP_PASSWORD));
        assertEquals("aFtpUserName", parameters.get(DpsDepositFacade.FTP_USER_NAME));
        assertEquals("aFtpDirectory", parameters.get(DpsDepositFacade.FTP_DIRECTORY));
        assertEquals("anHtmlSerialMaterialFlowId", parameters.get(DpsDepositFacade.MATERIAL_FLOW_ID));
        assertEquals("TwoHTMLSerialIeEntityType", parameters.get(DpsDepositFacade.IE_ENTITY_TYPE));
        assertEquals("aPdsUrl", parameters.get(DpsDepositFacade.PDS_URL));
        assertEquals("anHtmlSerialProducerId", parameters.get(DpsDepositFacade.PRODUCER_ID));
        assertEquals("http://someserver.natlib.govt.nz:80000/dpsws/deposit/DepositWebServices?wsdl", parameters.get(DpsDepositFacade.DPS_WSDL_URL));
        assertEquals("112233", parameters.get(DpsDepositFacade.TARGET_INSTANCE_ID));
        assertEquals("someFinalSIPXml", parameters.get(DpsDepositFacade.WCT_METS_XML_DOCUMENT));
        assertEquals("1234567890", parameters.get(DpsDepositFacade.ILS_REFERENCE));
        assertEquals("anAccessRestriction", parameters.get(DpsDepositFacade.ACCESS_RESTRICTION));
        assertEquals("anHtmlSerialBibCitation", parameters.get(DpsDepositFacade.DCTERMS_BIBLIOGRAPHIC_CITATION));
        assertEquals("anHtmlSerialDctermsAvailable", parameters.get(DpsDepositFacade.DCTERMS_AVAILABLE));
        assertEquals("HtmlSerialHarvest", parameters.get(DpsDepositFacade.HARVEST_TYPE));
    }

    @Test
    public void testExtractFileDetailsFrom() {
        DPSArchive archiver = new DPSArchive();
        List<ArchiveFile> archiveFileList = mockDasArchiveFileList();
        List<File> fileList = archiver.extractFileDetailsFrom(archiveFileList);
        assertEquals(testFiles.length, fileList.size());
        for (int i = 0; i < testFiles.length; i++) {
            File file = fileList.get(i);
            assertEquals(testFiles[i], file);
        }
    }

    @Ignore
    @Test
    public void testSubmitToArchive() throws DPSUploadException {
        mockDpsDepositFacade = mockDpsDepositFacade();
        DPSArchive archiver = new DPSArchive();
        setVariousParameters(archiver);
        Map<String, String> attributes = mockDasAttributeMap();

        String targetInstanceOID = "1588";

        File toBeTestDir = new File("/usr/local/wct/store/1588/1");
        File[] files = toBeTestDir.listFiles();
        List<ArchiveFile> archiveFileList = new ArrayList<>();
        for (File f : files) {
            ArchiveFile archiveFile = new ArchiveFile(f, ARC_FILE);
            archiveFileList.add(archiveFile);
        }
//		final List<ArchiveFile> archiveFileList = mockDasArchiveFileList();
        String archiveId = archiver.submitToArchive(targetInstanceOID, getFinalSip(), attributes, archiveFileList);
        assertEquals("dps-sipid-" + expectedSipId, archiveId);
    }

    private DpsDepositProxy mockDpsDepositFacade() {
        return new DpsDepositProxy() {
            public DepositResult deposit(Map<String, String> parameters, List<File> fileList) throws RuntimeException {
                for (int i = 0; i < testFiles.length; i++) {
                    File file = fileList.get(i);
                    assertEquals(testFiles[i], file);
                }
                assertEquals(testFiles.length, fileList.size());
                return mockDepositResult();
            }

            public String loginToPDS(Map<String, String> parameters) throws RuntimeException {
                return null;
            }

            @Override
            public void setCustomDepositFormMapping(CustomDepositFormMapping customDepositMapping) {
                return;
            }
        };
    }

    private DepositResult mockDepositResult() {
        return new DepositResult() {
            public String getCreationDate() {
                return null;
            }

            public long getDepositActivityId() {
                return 0;
            }

            public String getMessageCode() {
                return null;
            }

            public String getMessageDesciption() {
                return null;
            }

            public long getSipId() {
                return expectedSipId;
            }

            public String getUserParameters() {
                return null;
            }

            public boolean isError() {
                return false;
            }
        };
    }

    private List<ArchiveFile> mockDasArchiveFileList() {
        List<ArchiveFile> archiveFileList = new ArrayList<ArchiveFile>();
        for (File aFile : testFiles) {
            archiveFileList.add(new ArchiveFile(aFile, 0));
        }
        return archiveFileList;
    }

    private Map<String, String> mockDasAttributeMap() {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ACCESS_RESTRICTION, "anAccessRestriction");
        attributes.put(REFERENCE_NUMBER, "1234567890");
        return attributes;
    }

    private Map<String, String> mockDasAttributeMapForHtmlSerials() {
        Map<String, String> attributes = mockDasAttributeMap();
        attributes.put("customDepositForm_customFormPopulated", "true");
        attributes.put("customDepositForm_producerAgent", "anHtmlSerialUserName");
        attributes.put("customDepositForm_producerAgentPassword", "anHtmlSerialUserPassword");
        attributes.put("customDepositForm_producerId", "anHtmlSerialProducerId");
        attributes.put("customDepositForm_targetDcType", "HtmlSerialHarvest");
        attributes.put("customDepositForm_bibliographicCitation", "anHtmlSerialBibCitation");
//		attributes.put("customDepositForm_dctermsAccrualPeriodicity", "anHtmlSerialAccrualPeriodicity");
        attributes.put("customDepositForm_dctermsAvailable", "anHtmlSerialDctermsAvailable");
        attributes.put("harvest-type", "HtmlSerialHarvest");
        return attributes;
    }

    private void setVariousParameters(DPSArchive archiver) {
        archiver.setPdsUrl("aPdsUrl");
        archiver.setFtpHost("aFtpHost");
        archiver.setFtpUserName("aFtpUserName");
        archiver.setFtpPassword("aFtpPassword");
        archiver.setFtpDirectory("aFtpDirectory");
        archiver.setDepositServerBaseUrl("http://someserver.natlib.govt.nz:80000");
        archiver.setDepositWsdlRelativePath("/dpsws/deposit/DepositWebServices?wsdl");
        archiver.setDpsUserInstitution("aDpsUserInstitution");
        archiver.setDpsUserName("aDpsUserName");
        archiver.setDpsUserPassword("aDpsUserPassword");
        archiver.setMaterialFlowId("aMaterialFlowId");
        archiver.setProducerId("aProducerId");
    }

    /**
     * A class to override few methods, such as getDpsDepositFacade() method to
     * pass in a mocked version of DpsDepositFacade.
     *
     * @author pushpar
     */
    private class UnitTestDPSArchive extends DPSArchive {
//		protected DpsDepositProxy getDpsDepositFacade() {
//			return mockDpsDepositFacade;
//		}

        /**
         * Since we are using dummy files, the original MD5 calculation
         * will throw FileNotFoundException. The overriding below is to
         * take care of that issue.
         */
        protected String calculateMD5(File file) throws FileNotFoundException {
            return "1234";
        }
    }

    private DepData mockDepData(final String id, final String desc) {
        return new DepData(id, desc);
    }

    private String getFinalSip() {
        String finalSIP = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns=\"http://www.loc.gov/METS/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd\">\n" +
                "<mets:metsHdr CREATEDATE=\"2020-11-10T11:38:27\">\n" +
                "<mets:agent ROLE=\"DISSEMINATOR\" TYPE=\"INDIVIDUAL\">\n" +
                "<mets:name>F. Lee</mets:name>\n" +
                "</mets:agent>\n" +
                "<mets:agent ROLE=\"CREATOR\" TYPE=\"INDIVIDUAL\">\n" +
                "<mets:name>F. Lee</mets:name>\n" +
                "</mets:agent>\n" +
                "</mets:metsHdr>\n" +
                "<mets:dmdSec ID=\"DMD55\" CREATED=\"2020-11-10T11:38:27\" STATUS=\"current\">\n" +
                "<mets:mdWrap MDTYPE=\"DC\">\n" +
                "<mets:xmlData>\n" +
                "<dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
                "<dc:title>Target_H1</dc:title>\n" +
                "<dc:creator></dc:creator>\n" +
                "<dc:subject></dc:subject>\t\t\t\t\t\t\n" +
                "<dc:description></dc:description>\n" +
                "<dc:publisher></dc:publisher>\n" +
                "<dc:contributor></dc:contributor>\n" +
                "<dc:type></dc:type>\n" +
                "<dc:format></dc:format>\n" +
                "<dc:identifier></dc:identifier>\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "<dc:source></dc:source>\n" +
                "<dc:language></dc:language>\n" +
                "<dc:relation></dc:relation>\n" +
                "<dc:coverage></dc:coverage>\n" +
                "<dc:identifier></dc:identifier>\n" +
                "<dc:identifier></dc:identifier>\n" +
                "</dc:dc>\n" +
                "<wct:wct xmlns:wct=\"http://dia-nz.github.io/webcurator/schemata/webcuratortool-1.0.dtd\"> \n" +
                "<wct:Target>\n" +
                "<wct:ReferenceNumber></wct:ReferenceNumber>\n" +
                "<wct:Name>Target_H1</wct:Name>\n" +
                "<wct:Description></wct:Description>\n" +
                "<wct:Seeds>\n" +
                "<wct:Seed>\n" +
                "<wct:SeedURL>https://www.rnz.co.nz/</wct:SeedURL>\n" +
                "<wct:SeedType>Primary</wct:SeedType>\n" +
                "</wct:Seed>\n" +
                "</wct:Seeds>\n" +
                "</wct:Target>\n" +
                "\n" +
                "<wct:Groups>\t\t\t\t\t\t\t\n" +
                "</wct:Groups>\n" +
                "</wct:wct>\n" +
                "<mets:mdRef>order.xml</mets:mdRef>\n" +
                "</mets:xmlData>\n" +
                "</mets:mdWrap>\n" +
                "</mets:dmdSec>\n" +
                "<mets:amdSec ID=\"AMD55\">\n" +
                "<mets:techMD ID=\"TMD55\">\n" +
                "<mets:mdWrap MDTYPE=\"OTHER\">\n" +
                "<mets:xmlData>\n" +
                "<wct:wct xmlns:wct=\"http://dia-nz.github.io/webcurator/schemata/webcuratortool-1.0.dtd\">\n" +
                "<wct:TargetInstance> \n" +
                "<wct:Crawl>\n" +
                "<wct:AppVersion>3.0.0-SNAPSHOT</wct:AppVersion>\n" +
                "<wct:StartDate>2020-11-04 14:54:55.0</wct:StartDate>\n" +
                "<wct:StartDate>2020-11-04 14:54:55.0</wct:StartDate>\n" +
                "<wct:Duration>15046</wct:Duration>\n" +
                "<wct:CaptureSystem>Heritrix 3.4.0</wct:CaptureSystem>\n" +
                "<wct:URLs>\n" +
                "<wct:Downloaded>51</wct:Downloaded>\n" +
                "<wct:Failed>0</wct:Failed>\n" +
                "</wct:URLs>\n" +
                "<wct:AverageBandwidth>25.0</wct:AverageBandwidth>\n" +
                "<wct:DocumentProcessRate>3.4</wct:DocumentProcessRate>\n" +
                "<wct:DownloadedDataSize>394780</wct:DownloadedDataSize>\n" +
                "</wct:Crawl>\n" +
                "<wct:Annotations>\n" +
                "</wct:Annotations>\n" +
                "</wct:TargetInstance>\n" +
                "</wct:wct>\n" +
                "</mets:xmlData>\n" +
                "</mets:mdWrap>\n" +
                "</mets:techMD>\n" +
                "<mets:rightsMD ID=\"RMD55\">\n" +
                "<mets:mdWrap MDTYPE=\"OTHER\">\n" +
                "<mets:xmlData>\n" +
                "<wct:wct xmlns:wct=\"http://dia-nz.github.io/webcurator/schemata/webcuratortool-1.0.dtd\">\n" +
                "<wct:Permissions>\n" +
                "<wct:Permission>\n" +
                "<wct:State>Granted</wct:State>\n" +
                "<wct:StartDate>2020-11-04</wct:StartDate>\n" +
                "<wct:EndDate></wct:EndDate>\n" +
                "<wct:HarvestAuthorisation>\n" +
                "<wct:Name>Auth</wct:Name>\n" +
                "<wct:Description></wct:Description>\n" +
                "<wct:OrderNumber></wct:OrderNumber>\n" +
                "<wct:IsPublished>true</wct:IsPublished>\n" +
                "</wct:HarvestAuthorisation>\n" +
                "<wct:AccessStatus>Open (unrestricted) access</wct:AccessStatus>\n" +
                "<wct:SpecialRequirements></wct:SpecialRequirements>\n" +
                "<wct:OpenAccessDate></wct:OpenAccessDate>\n" +
                "<wct:CopyrightStatement></wct:CopyrightStatement>\n" +
                "<wct:CopyrightURL></wct:CopyrightURL>\n" +
                "<wct:FileReference></wct:FileReference>\n" +
                "<wct:AuthorisingAgent>\n" +
                "<wct:Name>Agency_Auth_01</wct:Name>\n" +
                "<wct:Contact>NLNZ</wct:Contact>\n" +
                "</wct:AuthorisingAgent>\n" +
                "<wct:Paterns>\n" +
                "<wct:Pattern>https://www.rnz.co.nz/</wct:Pattern>\n" +
                "</wct:Paterns>\n" +
                "<wct:SeedsURLs>\n" +
                "<wct:SeedURL>https://www.rnz.co.nz/</wct:SeedURL>\n" +
                "</wct:SeedsURLs>\n" +
                "</wct:Permission>\n" +
                "</wct:Permissions>\n" +
                "\n" +
                "</wct:wct>\n" +
                "</mets:xmlData>\n" +
                "</mets:mdWrap>\n" +
                "</mets:rightsMD>\n" +
                "<mets:digiprovMD ID=\"DPMD55\">\n" +
                "<mets:mdWrap MDTYPE=\"OTHER\">\n" +
                "<mets:xmlData>\n" +
                "<wct:wct xmlns:wct=\"http://dia-nz.github.io/webcurator/schemata/webcuratortool-1.0.dtd\">\n" +
                "<wct:TargetInstance>\n" +
                "<wct:HarvestResult>\n" +
                "<wct:Creator>lql 0/4</wct:Creator>\n" +
                "<wct:CreationDate>2020-11-04</wct:CreationDate>\n" +
                "<wct:ProvenanceNote>css</wct:ProvenanceNote>\n" +
                "<wct:ModificationNotes>\n" +
                "<wct:ModificationNote>Imported http://kkk.google-analytics.com/font.css</wct:ModificationNote>\n" +
                "</wct:ModificationNotes>\n" +
                "<wct:DerivedFrom>\n" +
                "<wct:HarvestResult>\n" +
                "<wct:Creator>lql 1/4</wct:Creator>\n" +
                "<wct:CreationDate>2020-11-04</wct:CreationDate>\n" +
                "<wct:ProvenanceNote>Import a css</wct:ProvenanceNote>\n" +
                "<wct:ModificationNotes>\n" +
                "<wct:ModificationNote>Imported http://www.google-analytics.com/font.css</wct:ModificationNote>\n" +
                "</wct:ModificationNotes>\n" +
                "<wct:DerivedFrom>\n" +
                "<wct:HarvestResult>\n" +
                "<wct:Creator>lql 2/4</wct:Creator>\n" +
                "<wct:CreationDate>2020-11-04</wct:CreationDate>\n" +
                "<wct:ProvenanceNote>Repair   http://www.google-analytics.com/robots.txt with a xml file</wct:ProvenanceNote>\n" +
                "<wct:DerivedFrom>\n" +
                "<wct:HarvestResult>\n" +
                "<wct:Creator>lql 3/4</wct:Creator>\n" +
                "<wct:CreationDate>2020-11-04</wct:CreationDate>\n" +
                "<wct:ProvenanceNote>Original Harvest</wct:ProvenanceNote>\n" +
                "</wct:HarvestResult>\n" +
                "</wct:DerivedFrom>\n" +
                "</wct:HarvestResult>\n" +
                "</wct:DerivedFrom>\n" +
                "</wct:HarvestResult>\n" +
                "</wct:DerivedFrom>\n" +
                "</wct:HarvestResult>\n" +
                "\n" +
                "<wct:Owner> \n" +
                "<wct:UID>lql</wct:UID>\n" +
                "<wct:Agency>bootstrap</wct:Agency>\n" +
                "</wct:Owner>\n" +
                "<wct:ModificationType></wct:ModificationType>\n" +
                "<wct:ModiicationNote></wct:ModiicationNote>\n" +
                "<wct:HarvestServer>Local Agent H1</wct:HarvestServer>\n" +
                "<wct:DisplayTargetInstance>true</wct:DisplayTargetInstance>\n" +
                "<wct:TargetInstanceDisplayNote></wct:TargetInstanceDisplayNote>\n" +
                "</wct:TargetInstance>\n" +
                "<wct:Target>\n" +
                "<wct:ObjectType>Target</wct:ObjectType>\n" +
                "\n" +
                "<wct:ProfileNote></wct:ProfileNote>\t\t\t\t\t\t\t\t\n" +
                "\n" +
                "<wct:HarvestType></wct:HarvestType>\t\t\t\t\t\t\t\t\n" +
                "<wct:SelectionDate>2020-11-04</wct:SelectionDate>\n" +
                "<wct:SelectionType></wct:SelectionType>\n" +
                "<wct:SelectionNote></wct:SelectionNote>\n" +
                "<wct:EvaluationNote></wct:EvaluationNote>\n" +
                "\n" +
                "<wct:Annotations></wct:Annotations><wct:DisplayTarget>true</wct:DisplayTarget>\n" +
                "<wct:AccessZone>0</wct:AccessZone>\n" +
                "<wct:AccessZoneText>Public</wct:AccessZoneText>\n" +
                "<wct:TargetDisplayNote></wct:TargetDisplayNote>\n" +
                "</wct:Target>\n" +
                "</wct:wct>\n" +
                "<vCard:VCard xmlns:vCard=\"http://www.w3.org/2001/vcard-rdf/3.0#\">\n" +
                "<vCard:FN>Frank Lee</vCard:FN>\n" +
                "<vCard:N>\n" +
                "<vCard:FAMILY>Lee</vCard:FAMILY>\n" +
                "<vCard:GIVEN>Frank</vCard:GIVEN>\n" +
                "<vCard:PREFIX>Mr</vCard:PREFIX>\n" +
                "</vCard:N>\n" +
                "<vCard:EMAIL>\n" +
                "<vCard:WORK>leefrank9527@gmail.com</vCard:WORK>\n" +
                "</vCard:EMAIL>\n" +
                "<vCard:ORG>\n" +
                "<vCard:Orgname>bootstrap</vCard:Orgname>\n" +
                "<vCard:EMAIL></vCard:EMAIL>\n" +
                "</vCard:ORG>\n" +
                "<vCard:URL></vCard:URL>\n" +
                "<vCard:LOGO></vCard:LOGO>\n" +
                "<vCard:UID>lql</vCard:UID>\n" +
                "</vCard:VCard>\n" +
                "</mets:xmlData>\n" +
                "</mets:mdWrap>\n" +
                "</mets:digiprovMD>\n" +
                "</mets:amdSec>";
        return finalSIP;
    }
}
