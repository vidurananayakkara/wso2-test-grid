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

import com.google.common.base.Optional;
import com.google.common.io.Closeables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Awaitility;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.wso2.carbon.testgrid.common.Deployment;
import org.wso2.carbon.testgrid.common.InfrastructureProvider;
import org.wso2.carbon.testgrid.common.Node;
import org.wso2.carbon.testgrid.common.TestPlan;
import org.wso2.carbon.testgrid.common.exception.TestGridInfrastructureException;
import org.wso2.carbon.testgrid.common.util.EnvironmentUtil;
import org.wso2.carbon.testgrid.common.util.StringUtil;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.wso2.carbon.testgrid.common.util.LambdaExceptionUtil.rethrowConsumer;

/**
 * Infrastructure connector for {@code OpenStack}.
 * <p>
 * This connector is implemented based on OpenStack4J OpenStack Java SDK - {@code http://www.openstack4j.com}
 *
 * @since 1.0.0
 */
public class OpenStackProvider implements InfrastructureProvider {

    private static final Log log = LogFactory.getLog(OpenStackProvider.class);
    private final String PROVIDER_NAME = "openstack-nova";
    private final String OPENSTACK_REGION_NAME = "OPENSTACK_REGION_NAME";
    private final String OPENSTACK_TENANT = "OPENSTACK_TENANT";
    private final String OPENSTACK_ENDPOINT = "OPENSTACK_ENDPOINT";
    private final String OPENSTACK_USERNAME = "OPENSTACK_USERID";
    private final String OPENSTACK_PASSWORD = "OPENSTACK_PASSWORD";
    private final NovaApi connection;
    private Map<String, Server> serverList = new ConcurrentHashMap<>();

    public OpenStackProvider() throws TestGridInfrastructureException {
        String endpoint = getEnvVariableValue(OPENSTACK_ENDPOINT);
        String tenant = getEnvVariableValue(OPENSTACK_TENANT);
        String userId = getEnvVariableValue(OPENSTACK_USERNAME);
        String password = getEnvVariableValue(OPENSTACK_PASSWORD);

        connection = ContextBuilder.newBuilder(PROVIDER_NAME)
                .endpoint(endpoint)
                .credentials(tenant + ":" + userId, password)
                .buildApi(NovaApi.class);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public Deployment createInfrastructure(TestPlan testPlan) throws TestGridInfrastructureException {
        String region = getEnvVariableValue(OPENSTACK_REGION_NAME);
        ImageApi imageApi = getImageApi(region);
        FlavorApi flavorApi = getFlavorApi(region);
        ServerApi serverApi = getServerApi(region);

        List<Node> nodes = new CopyOnWriteArrayList<>(testPlan.getInfrastructure().getNodes());

        nodes.parallelStream().forEach(rethrowConsumer(node -> {
            String serverName = testPlan.getName() + "-" + node.getLabel();
            if (serverList.containsKey(serverName) || isServerSpawned(serverName, serverApi)) {
                throw new TestGridInfrastructureException(
                        String.format(Locale.ENGLISH, "Node instance %s already exists", serverName));
            }
            Server server = spawnNodeInstance(serverName, node, flavorApi, imageApi, serverApi);
            serverList.put(node.getLabel(), server);
        }));
        return true;
    }

    @Override
    public boolean removeInfrastructure(Deployment deployment) throws TestGridInfrastructureException {
        return false;
    }

    @Override
    public void close() throws IOException {
        Closeables.close(connection, true);
    }

    /**
     * Returns whether a server is spawned with the given server name or not.
     *
     * @param serverName name of the server to check status
     * @param serverApi  {@link ServerApi} to handle {@link Server} related operations
     * @return {@code true} if the server is already spawned, {@code false} otherwise
     */
    private boolean isServerSpawned(String serverName, ServerApi serverApi) {
        Optional<Server> serverOptional = serverApi.listInDetail().concat()
                .firstMatch(server -> server != null && server.getName().equalsIgnoreCase(serverName));
        return serverOptional.isPresent();
    }

    /**
     * Spawn the given node in OpenStack cloud.
     *
     * @param serverName spawning server name
     * @param node       node instance to be spawned
     * @param flavorApi  {@link FlavorApi} to handle OpenStack {@link Flavor} related operations
     * @param imageApi   {@link ImageApi} to handle OpenStack Image related operations
     * @param serverApi  {@link ServerApi} to handle OpenStack Server related operations
     * @return Server instance for the spawned node
     * @throws TestGridInfrastructureException thrown when an error occurs in spawning an instance
     */
    private Server spawnNodeInstance(String serverName, Node node, FlavorApi flavorApi, ImageApi imageApi,
                                     ServerApi serverApi) throws TestGridInfrastructureException {
        String flavorName = node.getSize();
        String imageName = node.getImage();

        Flavor flavor = getFlavor(flavorApi, flavorName);
        Image image = getImage(imageApi, imageName);
        ServerCreated serverCreated = serverApi.create(serverName, image.getId(), flavor.getId());

        Callable<Boolean> serverCallable = () -> {
            Server server = serverApi.get(serverCreated.getId());
            log.debug(String.format(Locale.ENGLISH, "Waiting for server %s to be started...", serverName));
            return server.getStatus().equals(Server.Status.ACTIVE);
        };

        try {
            Awaitility.given().pollThread(Thread::new).await()
                    .atMost(node.getTimeout(), TimeUnit.MILLISECONDS)
                    .until(serverCallable, equalTo(true));
        } catch (Exception e) {
            throw new TestGridInfrastructureException("Error in spawning node instance in OpenStack", e);
        }

        // Server successfully spawned.
        log.debug("Server successfully spawned");
        return serverApi.get(serverCreated.getId());
    }

    /**
     * Returns the {@link ServerApi} for the given region.
     *
     * @param region region of OpenStack to obtain the {@link ServerApi} from
     * @return {@link ServerApi} instance
     * @throws TestGridInfrastructureException thrown when the {@link ServerApi} for the given region cannot be located
     */
    private ServerApi getServerApi(String region) throws TestGridInfrastructureException {
        ServerApi serverApi = connection.getServerApi(region);
        if (serverApi == null) {
            throw new TestGridInfrastructureException(String
                    .format(Locale.ENGLISH, "Server API to the given OpenStack region %s cannot be located", region));
        }
        return serverApi;
    }

    /**
     * Returns the {@link FlavorApi} for the given region.
     *
     * @param region region of OpenStack to obtain the {@link FlavorApi} from
     * @return {@link FlavorApi} instance
     * @throws TestGridInfrastructureException thrown when the {@link FlavorApi} for the given region cannot be located
     */
    private FlavorApi getFlavorApi(String region) throws TestGridInfrastructureException {
        FlavorApi flavorApi = connection.getFlavorApi(region);
        if (flavorApi == null) {
            throw new TestGridInfrastructureException(String
                    .format(Locale.ENGLISH, "Flavor API to the given OpenStack region %s cannot be located", region));
        }
        return flavorApi;
    }

    /**
     * Returns the {@link ImageApi} for the given region.
     *
     * @param region region of OpenStack to obtain the {@link ImageApi} from
     * @return {@link ImageApi} instance
     * @throws TestGridInfrastructureException thrown when the {@link ImageApi} for the given region cannot be located
     */
    private ImageApi getImageApi(String region) throws TestGridInfrastructureException {
        ImageApi imageApi = connection.getImageApi(region);
        if (imageApi == null) {
            throw new TestGridInfrastructureException(String
                    .format(Locale.ENGLISH, "Image API to the given OpenStack region %s cannot be located", region));
        }
        return imageApi;
    }

    /**
     * Returns the OpenStack Image for the given {@link ImageApi} and image name.
     *
     * @param imageApi {@link ImageApi} instance to obtain the image from
     * @param image    name of the image
     * @return image for the given name
     * @throws TestGridInfrastructureException thrown when the image with the given name cannot be located
     */
    private Image getImage(ImageApi imageApi, String image) throws TestGridInfrastructureException {
        if (StringUtil.isStringNullOrEmpty(image)) {
            throw new TestGridInfrastructureException("Image name is null or empty");
        }

        Optional<Image> imageOptional = imageApi.listInDetail().concat()
                .firstMatch(dockerImage -> dockerImage != null && dockerImage.getName().equalsIgnoreCase(image));
        if (!imageOptional.isPresent()) {
            throw new TestGridInfrastructureException(String
                    .format(Locale.ENGLISH, "Image %s not found.", image));
        }
        return imageOptional.get();
    }

    /**
     * Returns the OpenStack Flavor for the given {@link FlavorApi} and flavor name.
     *
     * @param flavorApi {@link FlavorApi} instance to obtain the flavor from
     * @param flavor    name of the flavor
     * @return flavor for the given name
     * @throws TestGridInfrastructureException thrown when the flavor with the given name cannot be located
     */
    private Flavor getFlavor(FlavorApi flavorApi, String flavor) throws TestGridInfrastructureException {
        if (StringUtil.isStringNullOrEmpty(flavor)) {
            throw new TestGridInfrastructureException("Flavor name is null or empty");
        }

        Optional<Flavor> flavorOptional = flavorApi.listInDetail().concat()
                .firstMatch(size -> size != null && size.getName().equalsIgnoreCase(flavor));
        if (!flavorOptional.isPresent()) {
            throw new TestGridInfrastructureException(String
                    .format(Locale.ENGLISH, "Flavor %s not found.", flavor));
        }
        return flavorOptional.get();
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
