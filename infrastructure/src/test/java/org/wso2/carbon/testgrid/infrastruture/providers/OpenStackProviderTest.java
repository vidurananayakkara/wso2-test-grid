/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.testgrid.infrastruture.providers;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.testgrid.common.Deployment;
import org.wso2.carbon.testgrid.common.Infrastructure;
import org.wso2.carbon.testgrid.common.Script;
import org.wso2.carbon.testgrid.common.exception.TestGridInfrastructureException;
import org.wso2.carbon.testgrid.infrastructure.providers.OpenStackProvider;
import org.wso2.carbon.testgrid.infrastruture.testutil.TestEnvironmentUtil;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test class to test the functionality of the
 * {@link org.wso2.carbon.testgrid.infrastructure.providers.OpenStackProvider}
 *
 * @since 1.0.0
 */
public class OpenStackProviderTest {

    private final String OPENSTACK_ENDPOINT = "OPENSTACK_ENDPOINT";
    private final String OPENSTACK_USERNAME = "OPENSTACK_USERID";
    private final String OPENSTACK_PASSWORD = "OPENSTACK_PASSWORD";

    @BeforeTest
    public void setUp() {
        // Set environment variables.
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put(OPENSTACK_ENDPOINT, "http://203.94.95.131:5000/v3");
        environmentVariables.put(OPENSTACK_USERNAME, "testgrid");
        environmentVariables.put(OPENSTACK_PASSWORD, "Pw5dnlqpn/nLsdfrh4ni");

        TestEnvironmentUtil.setEnvironmentVariables(environmentVariables);
    }

    /**
     * Tests infrastructure creation of OpenStack.
     *
     * @throws TestGridInfrastructureException thrown when infrastructure creation fails
     */
    @Test
    public void testAuthentication() throws TestGridInfrastructureException {
        Script script = new Script();
        script.setFilePath("openstack-template.yaml");

        List<Script> scripts = new CopyOnWriteArrayList<>();
        scripts.add(script);

        Infrastructure infrastructure = Mockito.mock(Infrastructure.class);
        Mockito.when(infrastructure.getName()).thenReturn("sample-infra");
        Mockito.when(infrastructure.getRegion()).thenReturn("RegionOne");
        Mockito.when(infrastructure.getScripts()).thenReturn(scripts);

        // Infrastructure repository path
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("openstack-heat");
        if (url == null) {
            Assert.fail(); // Resource not found
        }
        Path infraRepoPath = Paths.get(url.getPath());

        OpenStackProvider provider = new OpenStackProvider();

        // Authenticate to openstack
        Assert.assertTrue(provider.authenticate());
        Deployment deployment =
                provider.createInfrastructure(infrastructure, infraRepoPath.toAbsolutePath().toString());
    }

    @AfterTest
    public void tearDown() {
        // Unset environment variables.
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_ENDPOINT);
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_USERNAME);
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_PASSWORD);
    }
}
