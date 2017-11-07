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

package org.wso2.carbon.testgrid.infrastructure.providers;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.StackCreate;
import org.openstack4j.openstack.OSFactory;
import org.wso2.carbon.testgrid.common.Deployment;
import org.wso2.carbon.testgrid.common.Host;
import org.wso2.carbon.testgrid.common.Infrastructure;
import org.wso2.carbon.testgrid.common.InfrastructureProvider;
import org.wso2.carbon.testgrid.common.Port;
import org.wso2.carbon.testgrid.common.Script;
import org.wso2.carbon.testgrid.common.exception.TestGridInfrastructureException;
import org.wso2.carbon.testgrid.common.util.EnvironmentUtil;
import org.wso2.carbon.testgrid.common.util.StringUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Infrastructure connector for {@code OpenStack}.
 * <p>
 * This connector is implemented based on OpenStack4J OpenStack Java SDK - {@code http://www.openstack4j.com}
 *
 * @since 1.0.0
 */
public class OpenStackProvider implements InfrastructureProvider {

    private final String PROVIDER_NAME = "openstack";
    private final String SCRIPTS_FOLDER_NAME = "DeploymentPatterns";
    private final String OPENSTACK_ENDPOINT = "OPENSTACK_ENDPOINT";
    private final String OPENSTACK_USERNAME = "OPENSTACK_USERID";
    private final String OPENSTACK_PASSWORD = "OPENSTACK_PASSWORD";

    private OSClient osClient;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean canHandle(Infrastructure infrastructure) {
        return false;
    }

    @Override
    public boolean authenticate() throws TestGridInfrastructureException {
        String endpoint = getEnvVariableValue(OPENSTACK_ENDPOINT);
        String userId = getEnvVariableValue(OPENSTACK_USERNAME);
        String password = getEnvVariableValue(OPENSTACK_PASSWORD);

        osClient = OSFactory.builderV3()
                .endpoint(endpoint)
                .credentials(userId, password)
                .scopeToProject(Identifier.byId("testgrid"))
                .authenticate();

        return osClient != null;
    }

    @Override
    public Deployment createInfrastructure(Infrastructure infrastructure, String infraRepoDir)
            throws TestGridInfrastructureException {

        osClient.useRegion(infrastructure.getRegion()); // Move to specified region in the infrastructure

        List<Script> scripts = new CopyOnWriteArrayList<>(infrastructure.getScripts());
        List<Host> hosts = new CopyOnWriteArrayList<>();

        scripts.parallelStream().forEach(script -> {
            Path scriptPath =
                    Paths.get(infraRepoDir, SCRIPTS_FOLDER_NAME, infrastructure.getName(), script.getFilePath());
            StackCreate stackCreate = Builders.stack()
                    .name(infrastructure.getName())
                    .template(scriptPath.toAbsolutePath().toString())
                    .timeoutMins(1L)
                    .build();
            Stack stack = osClient.heat().stacks().create(stackCreate);

//            Host host = createHost(server.getAccessIPv4(), node.getLabel(), node.getOpenPorts());
//            hosts.add(host);
        });
        return createDeployment(hosts);
    }

    @Override
    public boolean removeInfrastructure(Deployment deployment, String infraRepoDir) throws TestGridInfrastructureException {
        return false;
    }

    /**
     * Returns an {@link Deployment} instance.
     *
     * @param hosts list of hosts in the in the deployment pattern
     * @return {@link Deployment} instance
     */
    private Deployment createDeployment(List<Host> hosts) {
        Deployment deployment = new Deployment();
        deployment.setHosts(hosts);
        return deployment;
    }

    /**
     * Returns a {@link Host} instance for the given IP, label and ports.
     *
     * @param ip    IP address of the host
     * @param label label to identify the host
     * @param ports list of open ports in the host
     * @return {@link Host} instance for the given IP, label and ports
     */
    private Host createHost(String ip, String label, List<Port> ports) {
        Host host = new Host();
        host.setIp(ip);
        host.setLabel(label);
        host.setPorts(ports);
        return host;
    }

    /**
     * Returns the environment variable value for the given environment variable key.
     *
     * @param environmentVariableKey environment variable key to obtain environment variable value
     * @return environment variable value for the given environment variable key
     * @throws TestGridInfrastructureException thrown when the environment variable key is not set or the environment
     *                                         variable is empty
     */
    private String getEnvVariableValue(String environmentVariableKey) throws TestGridInfrastructureException {
        String environmentVariableValue = EnvironmentUtil.getSystemVariableValue(environmentVariableKey);
        if (StringUtil.isStringNullOrEmpty(environmentVariableValue)) {
            throw new TestGridInfrastructureException(String
                    .format(Locale.ENGLISH, "%s environment variable not set", environmentVariableKey));
        }
        return environmentVariableValue;
    }
}
