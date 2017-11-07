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
import org.wso2.carbon.testgrid.common.Infrastructure;
import org.wso2.carbon.testgrid.common.Node;
import org.wso2.carbon.testgrid.common.TestPlan;
import org.wso2.carbon.testgrid.common.exception.TestGridInfrastructureException;
import org.wso2.carbon.testgrid.infrastructure.providers.OpenStackProvider;
import org.wso2.carbon.testgrid.infrastruture.testutil.TestEnvironmentUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class to test the functionality of the
 * {@link org.wso2.carbon.testgrid.infrastructure.providers.OpenStackProvider}
 *
 * @since 1.0.0
 */
public class OpenStackProviderTest {

    private final String OPENSTACK_REGION_NAME = "OPENSTACK_REGION_NAME";
    private final String OPENSTACK_TENANT = "OPENSTACK_TENANT";
    private final String OPENSTACK_ENDPOINT = "OPENSTACK_ENDPOINT";
    private final String OPENSTACK_USERNAME = "OPENSTACK_USERID";
    private final String OPENSTACK_PASSWORD = "OPENSTACK_PASSWORD";

    @BeforeTest
    public void setUp() {
        // Set environment variables.
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put(OPENSTACK_REGION_NAME, "RegionOne");
        environmentVariables.put(OPENSTACK_TENANT, "dev");
        environmentVariables.put(OPENSTACK_ENDPOINT, "http://203.94.95.131:5000/v2.0");
        environmentVariables.put(OPENSTACK_USERNAME, "username");
        environmentVariables.put(OPENSTACK_PASSWORD, "Password");

        TestEnvironmentUtil.setEnvironmentVariables(environmentVariables);
    }

    /**
     * Tests infrastructure creation of OpenStack.
     *
     * @throws TestGridInfrastructureException thrown when infrastructure creation fails
     */
    @Test
    public void testAuthentication() throws TestGridInfrastructureException {

        List<Node> nodeList = new ArrayList<>();
        Node node1 = new Node();
        node1.setLabel("TGOSCon1");
        node1.setSize("m1.small");
        node1.setImage("ubuntu-16.04");
        node1.setTimeout(30000);
        nodeList.add(node1);

        Node node2 = new Node();
        node2.setLabel("TGOSCon2");
        node2.setSize("m1.small");
        node2.setImage("ubuntu-16.04");
        node2.setTimeout(30000);
        nodeList.add(node2);

        Infrastructure infrastructure = Mockito.mock(Infrastructure.class);
        Mockito.when(infrastructure.getNodes()).thenReturn(nodeList);

        TestPlan testPlan = Mockito.mock(TestPlan.class);
        Mockito.when(testPlan.getInfrastructure()).thenReturn(infrastructure);
        Mockito.when(testPlan.getName()).thenReturn("Sample");

        OpenStackProvider provider = new OpenStackProvider();
        Assert.assertTrue(provider.createInfrastructure(testPlan));
    }

    @AfterTest
    public void tearDown() {
        // Unset environment variables.
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_REGION_NAME);
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_TENANT);
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_ENDPOINT);
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_USERNAME);
        TestEnvironmentUtil.unsetEnvironmentVariable(OPENSTACK_PASSWORD);
    }
}
