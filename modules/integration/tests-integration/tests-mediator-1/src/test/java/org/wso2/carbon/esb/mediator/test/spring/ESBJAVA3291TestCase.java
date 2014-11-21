package org.wso2.carbon.esb.mediator.test.spring;

import java.io.File;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.apache.axis2.AxisFault;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.api.clients.registry.ResourceAdminServiceClient;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.automation.api.clients.logging.LogViewerClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import javax.activation.DataHandler;

import java.net.URL;
import java.rmi.RemoteException;

/**
 * Related to https://wso2.org/jira/browse/ESBJAVA-3291
 * This class tests whether the SpringMediator throws proper error while the 
 * custom java class having any problem. 
 */

public class ESBJAVA3291TestCase extends ESBIntegrationTest {

	private final String SIMPLE_BEAN_JAR = "org.wso2.carbon.test.mediator.errorMediator.jar";
	private final String JAR_LOCATION = "/artifacts/ESB/jar";
	private LogViewerClient logViewerClient;

	private ServerConfigurationManager serverConfigurationManager;

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {

		init(ProductConstant.ADMIN_USER_ID);
		clearUploadedResource();
		serverConfigurationManager = new ServerConfigurationManager(
				esbServer.getBackEndUrl());
		serverConfigurationManager.copyToComponentLib(new File(getClass()
				.getResource(JAR_LOCATION + File.separator + SIMPLE_BEAN_JAR)
				.toURI()));
		serverConfigurationManager.restartGracefully();

		init(ProductConstant.ADMIN_USER_ID);
		uploadResourcesToConfigRegistry();
		loadESBConfigurationFromClasspath("/artifacts/ESB/mediatorconfig/spring/spring_mediation_error.xml");
		logViewerClient = new LogViewerClient(esbServer.getBackEndUrl(),
				esbServer.getSessionCookie());

	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		try {
			deleteSequence("main");
			clearUploadedResource();
			Thread.sleep(5000);
			super.cleanup();
		} finally {

			serverConfigurationManager.removeFromComponentLib(SIMPLE_BEAN_JAR);
			serverConfigurationManager.restartGracefully();
			serverConfigurationManager = null;
		}
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.integration_all })
	@Test(groups = { "wso2.esb", "localOnly" }, description = "Spring Mediator - Provide proper error message when problem in the custom java class")
	public void testBeanSpringMediation() throws AxisFault {

		// To check whether the correct error message is getting printed 
		boolean responseInLog = false;

		try {
			axis2Client.sendSimpleStockQuoteRequest(getMainSequenceURL(), null,
					"IBM");

		} catch (Exception axisFault) {
			try {
				LogEvent[] logs = logViewerClient.getAllSystemLogs();
				for (LogEvent logEvent : logs) {
					String message = logEvent.getMessage();
					if (message
							.contains("Error in org.wso2.carbon.test.mediator.errorMediator.ErrorMediator")) {
						responseInLog = true;
						break;
					}
				}

				Assert.assertTrue(
						responseInLog,
						" Fault: Error message mismatched, expected 'Error in org.wso2.carbon.test.mediator.errorMediator.ErrorMediator'");

			} catch (RemoteException e) {}
		}

	}

	private void uploadResourcesToConfigRegistry() throws Exception {

		ResourceAdminServiceClient resourceAdminServiceStub = new ResourceAdminServiceClient(
				esbServer.getBackEndUrl(), esbServer.getSessionCookie());

		resourceAdminServiceStub.deleteResource("/_system/config/spring");
		resourceAdminServiceStub.addCollection("/_system/config/", "spring",
				"", "Contains spring bean config files");
		
		resourceAdminServiceStub.addResource(
				"/_system/config/spring/spring_bean_for_error_client.xml","application/xml", "spring bean config files",
                new DataHandler(new URL("file:///" + getClass().getResource(
                		"/artifacts/ESB/mediatorconfig/spring/utils/spring_bean_for_error_client.xml").getPath())));
	}

	private void clearUploadedResource() throws InterruptedException,
			ResourceAdminServiceExceptionException, RemoteException {

		ResourceAdminServiceClient resourceAdminServiceStub = new ResourceAdminServiceClient(
				esbServer.getBackEndUrl(), esbServer.getSessionCookie());

		resourceAdminServiceStub.deleteResource("/_system/config/spring");
	}
}
