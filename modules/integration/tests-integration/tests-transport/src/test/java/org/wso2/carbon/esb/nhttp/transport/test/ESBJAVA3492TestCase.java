package org.wso2.carbon.esb.nhttp.transport.test;

import java.io.File;

import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.apache.axiom.om.OMElement;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.apache.catalina.startup.Tomcat;
import org.wso2.carbon.utils.CarbonUtils;
import org.testng.Assert;

/**
 * Related to https://wso2.org/jira/browse/ESBJAVA-3492
 * This class tests whether fault sequence handles the SSLException in nhttp transport 
 **/

public class ESBJAVA3492TestCase extends ESBIntegrationTest {

	private final Tomcat tomcat = new Tomcat();
	private final String separator = File.separator;
	private final String resourceFolderPath = getClass().getResource(
			  File.separator
			+ "artifacts"
			+ File.separator
			+ "ESB"
			+ File.separator
			+ "nhttp"
			+ File.separator
			+ "transport"
			+ File.separator + "sslHandling" + File.separator).getPath();

	@BeforeClass(alwaysRun = true)
	public void deployService() throws Exception {
		// Initializing server configuration
		super.init();

		// Initialize serverConfigurationManager
		ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(
				new AutomationContext("ESB", TestUserMode.SUPER_TENANT_ADMIN));

		// Apply the nhttp axis2 configuration
		serverConfigurationManager.applyConfiguration(new File(
				resourceFolderPath + "axis2.xml"),
				new File(CarbonUtils.getCarbonHome() + File.separator
						+ "repository" + File.separator + "conf"
						+ File.separator + "axis2" + File.separator
						+ "axis2.xml"));

		// Configure the standard host
		StandardHost stdHost = (StandardHost) tomcat.getHost();
		stdHost.setAutoDeploy(true);
		stdHost.setDeployOnStartup(true);
		stdHost.setUnpackWARs(true);
		tomcat.setHost(stdHost);

		// Setting up the connector
		Connector connector = new Connector();
		connector.setPort(8443);
		connector.setProtocol("HTTP/1.1");
		connector.setProperty("SSLEnabled", "true");
		connector.setProperty("maxThreads", "150");
		connector.setScheme("https");
		connector.setSecure(true);
		connector.setProperty("clientAuth", "false");
		connector.setProperty("sslProtocol", "TLS");
		connector.setProperty("keystoreFile", new File(resourceFolderPath
				+ "ssl_check_keystore.jks").getAbsolutePath());
		connector.setProperty("keystorePass", "localhost");

		tomcat.getService().addConnector(connector);

		// Starting Tomcat
		tomcat.start();

		// Re-initializing server configuration
		super.init();

		// Deploying the artifact defined in the ssl_check.xml
		loadESBConfigurationFromClasspath(File.separator + "artifacts"
				+ File.separator + "ESB" + File.separator + "nhttp"
				+ File.separator + "transport" + File.separator + "sslHandling"
				+ File.separator + "ssl_check.xml");
	}

	@Test(groups = "wso2.esb", description = "To check the SSL certificate failure redirect to fault sequence ")
	public void testSSLHandlingSequence() throws Exception {

		// Sending a message to the main sequence
		OMElement response = axis2Client.sendSimpleStockQuoteRequest(
				"http://localhost:8280/", null, "WSO2");

		// Check the return message -- only the fault sequence can respond in
		// this case
		Assert.assertTrue(response.toString().contains("WSO2"));
	}

	@AfterClass(alwaysRun = true)
	public void unDeployService() throws Exception {
		// Undeploying deployed artifact
		super.cleanup();
		tomcat.stop();
		tomcat.destroy();
	}

}
