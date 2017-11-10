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

package org.wso2.carbon.testgrid.common;

import java.util.List;

/**
 * Defines a model object for a created deployment.
 *
 * @since 1.0.0
 */
public class Deployment {

    private List<Host> hosts;

    /**
     * Returns the list of hosts related to the deployment.
     *
     * @return list of hosts related to the deployment
     */
    public List<Host> getHosts() {
        return hosts;
    }

    /**
     * Sets the list of hosts related to the deployment.
     *
     * @param hosts list of hosts related to the deployment
     */
    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }
}
