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

package org.wso2.carbon.testgrid.common.exception;

/**
 * This is a custom exception class to throw exceptions when there's an issue in creating instances of
 * InfrastructureProvider.
 */
public class InfrastructureProviderInitializationException extends Exception {

    private static final long serialVersionUID = -3151279311929070297L;

    public InfrastructureProviderInitializationException(String msg, Exception nestedEx) {
        super(msg, nestedEx);
    }

    public InfrastructureProviderInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InfrastructureProviderInitializationException(String msg) {
        super(msg);
    }

    public InfrastructureProviderInitializationException() {
        super();
    }

    public InfrastructureProviderInitializationException(Throwable cause) {
        super(cause);
    }
}
