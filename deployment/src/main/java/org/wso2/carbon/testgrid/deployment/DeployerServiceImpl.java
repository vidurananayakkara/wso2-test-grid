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

package org.wso2.carbon.testgrid.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.testgrid.common.DeployerService;
import org.wso2.carbon.testgrid.common.Deployment;
import org.wso2.carbon.testgrid.common.TestPlan;
import org.wso2.carbon.testgrid.common.Utils;
import org.wso2.carbon.testgrid.common.exception.TestGridDeployerException;
import org.wso2.carbon.testgrid.infrastructure.InfrastructureProviderServiceImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * This class handles the deployment of the artifacts in the cluster.
 */
public class DeployerServiceImpl implements DeployerService {

    private static final Log log = LogFactory.getLog(InfrastructureProviderServiceImpl.class);
    private final static String DEPLOYER_NAME = "puppet";

    public void init() {

    }

    @Override
    public String getDeployerName() {
        return DEPLOYER_NAME;
    }

    @Override
    public Deployment deploy(TestPlan testPlan) throws TestGridDeployerException {
        String testPlanLocation = testPlan.getHome() +"/test-grid-is-resources/DeploymentPatterns/" + testPlan.getDeploymentPattern();
        String scriptLocation = testPlanLocation + "/OpenStack/wso2is/deploy.sh";
        try {
            String username = System.getenv("OS_USERNAME");
            String password = System.getenv("OS_PASSWORD");
            String dockerUrl = "dockerhub.private.wso2.com";
            String dockerEmail = username + "@wso2.com";

            Utils.executeCommand("chmod -R 777 " + testPlanLocation, null);
            System.setProperty("user.dir", testPlanLocation + "/OpenStack/wso2is" );
            File file = new File(System.getProperty("user.dir"));

            System.out.println("Setting KUBERNETES_MASTER environment variable...");
            log.info("Setting KUBERNETES_MASTER environment variable...");
//            setKubernetesMasterEnvVariable(testPlanLocation + "/Scripts/OpenStack");
//            Utils.executeCommand("export KUBERNETES_MASTER=http://192.168.58.71:8080", file);
            System.out.println("Deploying kubernetes artifacts...");
            log.info("Deploying kubernetes artifacts...");

//            Utils.executeCommand("bash " + scriptLocation + " " + getKubernetesMaster(testPlanLocation + "/Scripts/OpenStack"));
            Utils.executeCommand("./deploy.sh " +
                    getKubernetesMaster(testPlanLocation + "/OpenStack/k8s.properties") + " " +
                    dockerUrl + " " + username + " " + password + " " + dockerEmail, file);
            testPlan.setStatus(TestPlan.Status.DEPLOYMENT_READY);
            return DeploymentUtil.getDeploymentInfo(testPlanLocation);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    @Override
    public boolean unDeploy(TestPlan testPlan) throws TestGridDeployerException {
        String testPlanLocation = testPlan.getHome() +"/test-grid-is-resources/DeploymentPatterns/" + testPlan.getDeploymentPattern();
        String scriptLocation = testPlanLocation + "/OpenStack/wso2is/undeploy.sh";

        Utils.executeCommand("chmod -R 777 " + testPlanLocation, null);

        System.setProperty("user.dir", testPlanLocation + "/OpenStack/wso2is" );
        File file = new File(System.getProperty("user.dir"));

        if(Utils.executeCommand(/*"bash " + scriptLocation*/"./undeploy.sh", file)) {
            return true;
        }
        return false;
    }

    private void setKubernetesMasterEnvVariable (String path) throws IOException {
      /*  Properties prop = new Properties();
        InputStream inputStream = new FileInputStream(path + "/k8s.properties");
        prop.load(inputStream);
        System.out.println(inputStream);
        log.info(inputStream);*/

        Utils.executeCommand("export KUBERNETES_MASTER=http://192.168.58.7:8080" /*+ prop.getProperty("KUBERNETES_MASTER")*/, null);

      /*  ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "export");
        Map<String, String> env = pb.environment();
        env.put("KUBERNETES_MASTER", prop.getProperty("KUBERNETES_MASTER"));
        System.out.println("==============================="+pb.environment().get("KUBERNETES_MASTER"));
        pb.start();*/
    }

    /**
     * Retrieves the value of KUBERNETES_MASTER property.
     *
     * @param location location of k8s.properties file
     * @return String value of KUBERNETES_MASTER property
     */
    private String getKubernetesMaster(String location){
        Properties prop = new Properties();
        try {
            InputStream inputStream = new FileInputStream(location);
            prop.load(inputStream);

            System.out.println(inputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop.getProperty("KUBERNETES_MASTER");
    }
}
