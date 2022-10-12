/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.impl.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.dto.BasicAuthValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.user.exceptions.UserException;
import org.wso2.carbon.apimgt.user.mgt.internal.UserManagerHolder;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class APIKeyMgtRemoteUserStoreMgtService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(APIKeyMgtRemoteUserStoreMgtService.class);

    /**
     * validates a username,password combination. Works for any tenant domain.
     * @param username username of the user(including tenant domain)
     * @param password password of the user
     * @return true if username,password is correct
     * @throws APIManagementException
     */
    public boolean authenticate(String username, String password) throws APIManagementException {

        String tenantDomain = MultitenantUtils.getTenantDomain(username);
        boolean isAuthenticated = false;
        try {
            String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(username);
            isAuthenticated = UserManagerHolder.getUserManager().authenticate(tenantAwareUserName, password);
        } catch (UserException e) {
            APIUtil.handleException("Error occurred while validating credentials of user " + username, e);
        }
        return isAuthenticated;
    }

    /**
     * Get the role list of a user. Works for any tenant domain.
     * @param username username with tenant domain
     * @return list of roles
     * @throws APIManagementException
     */
    public String[] getUserRoles(String username) throws APIManagementException {

        String userRoles[] = null;
        String tenantDomain = MultitenantUtils.getTenantDomain(username);
        try {
            userRoles = UserManagerHolder.getUserManager().getRoleListOfUser(
                    MultitenantUtils.getTenantAwareUsername(username));
        } catch (UserException e) {
            APIUtil.handleException("Error occurred retrieving roles of user " + username, e);
        }
        return userRoles;
    }

    public BasicAuthValidationInfoDTO getUserAuthenticationInfo(String username, String password)
            throws APIManagementException {

        String tenantDomain = MultitenantUtils.getTenantDomain(username);

        BasicAuthValidationInfoDTO basicAuthValidationInfoDTO = new BasicAuthValidationInfoDTO();
        boolean isAuthenticated;
        String userRoles[];
        String domainQualifiedUsername;
        try {
            isAuthenticated = UserManagerHolder.getUserManager().authenticate(
                    MultitenantUtils.getTenantAwareUsername(username), password);
            if (isAuthenticated) {
                basicAuthValidationInfoDTO.setAuthenticated(true);
                domainQualifiedUsername = UserManagerHolder.getUserManager().addDomainToName(username,
                        UserManagerHolder.getUserManager().getDomainFromThreadLocal());
                basicAuthValidationInfoDTO.setDomainQualifiedUsername(domainQualifiedUsername);
            } else {
                //return default validation DTO with authentication false
                return basicAuthValidationInfoDTO;
            }
            //Get role list of user.
            //Should give the domain qualified username when getting the role list of user.
            userRoles = UserManagerHolder.getUserManager().getRoleListOfUser(
                    MultitenantUtils.getTenantAwareUsername(domainQualifiedUsername));
            basicAuthValidationInfoDTO.setUserRoleList(userRoles);
        } catch (UserException e) {
            APIUtil.handleException("Error occurred while retrieving user authentication info of user " + username, e);
        }
        return basicAuthValidationInfoDTO;
    }

}
