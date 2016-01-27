/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * 
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.gsma.authenticators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.cache.SessionDataCache;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheEntry;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;

import com.wso2telco.gsma.authenticators.config.LOA;
import com.wso2telco.gsma.authenticators.config.LOAConfig;
import com.wso2telco.gsma.authenticators.config.LOA.MIFEAbstractAuthenticator;

 
// TODO: Auto-generated Javadoc
/**
 * The Class LOACompositeAuthenticator.
 */
public class LOACompositeAuthenticator implements ApplicationAuthenticator,
		LocalApplicationAuthenticator {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 119680530347040691L;
	
	/** The selected loa. */
	private String selectedLOA = null;

	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#canHandle(javax.servlet.http.HttpServletRequest)
	 */
	public boolean canHandle(HttpServletRequest request) {
		LinkedHashSet<?> acrs = this.getACRValues(request);
		selectedLOA = (String) acrs.iterator().next();
		return acrs != null && acrs.size() > 0 && selectedLOA != null;
	}

	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext)
	 */
	public AuthenticatorFlowStatus process(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws AuthenticationFailedException, LogoutFailedException {
		if (!canHandle(request)) {
			return AuthenticatorFlowStatus.INCOMPLETE;
		}

		LOAConfig config = DataHolder.getInstance().getLOAConfig();
		LOA loa = config.getLOA(selectedLOA);
		if (loa.getAuthenticators() == null) {
			config.init();
		}

		SequenceConfig sequenceConfig = context.getSequenceConfig();
		Map<Integer, StepConfig> stepMap = sequenceConfig.getStepMap();

		StepConfig sc = stepMap.get(1);
		sc.setSubjectAttributeStep(false);
		sc.setSubjectIdentifierStep(false);

		int stepOrder = 2;

		while (true) {
			List<MIFEAbstractAuthenticator> authenticators = loa.getAuthenticators();
			String fallBack = loa.getAuthentication().getFallbackLevel();

			for (MIFEAbstractAuthenticator authenticator : authenticators) {
				StepConfig stepConfig = new StepConfig();
				stepConfig.setOrder(stepOrder);
				if (stepOrder == 2) {
					stepConfig.setSubjectAttributeStep(true);
					stepConfig.setSubjectIdentifierStep(true);
				}

				List<AuthenticatorConfig> authenticatorConfigs = stepConfig.getAuthenticatorList();
				if (authenticatorConfigs == null) {
					authenticatorConfigs = new ArrayList<AuthenticatorConfig>();
					stepConfig.setAuthenticatorList(authenticatorConfigs);
				}

				AuthenticatorConfig authenticatorConfig = new AuthenticatorConfig();
				authenticatorConfig.setName(authenticator.getAuthenticator().getName());
				authenticatorConfig.setApplicationAuthenticator(authenticator.getAuthenticator());

				String onFail = authenticator.getOnFailAction();

				Map<String, String> parameterMap = new HashMap<String, String>();
				parameterMap.put("currentLOA", loa.getLevel());
				parameterMap.put("fallBack", (null != fallBack) ? fallBack : "");
				parameterMap.put("onFail", (null != onFail) ? onFail : "");
				parameterMap
						.put("isLastAuthenticator",
								(authenticators.indexOf(authenticator) == authenticators.size() - 1) ? "true"
										: "false");
				authenticatorConfig.setParameterMap(parameterMap);

				stepConfig.getAuthenticatorList().add(authenticatorConfig);
				stepMap.put(stepOrder, stepConfig);

				stepOrder++;
			}

			// increment LOA to fallBack level
			if (null == fallBack) {
				break;
			}
			loa = config.getLOA(fallBack);
		}

		sequenceConfig.setStepMap(stepMap);
		context.setSequenceConfig(sequenceConfig);

		return AuthenticatorFlowStatus.SUCCESS_COMPLETED;
	}

	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#getContextIdentifier(javax.servlet.http.HttpServletRequest)
	 */
	public String getContextIdentifier(HttpServletRequest request) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#getName()
	 */
	public String getName() {
		return Constants.LOACA_AUTHENTICATOR_NAME;
	}

	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#getFriendlyName()
	 */
	public String getFriendlyName() {
		return Constants.LOACA_AUTHENTICATOR_FRIENDLY_NAME;
	}

	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#getClaimDialectURI()
	 */
	public String getClaimDialectURI() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator#getConfigurationProperties()
	 */
	public List<Property> getConfigurationProperties() {
		return new ArrayList<Property>();
	}

	/**
	 * Gets the ACR values.
	 *
	 * @param request the request
	 * @return the ACR values
	 */
	private LinkedHashSet<?> getACRValues(HttpServletRequest request) {
		String sdk = request.getParameter(OAuthConstants.SESSION_DATA_KEY);
		CacheKey ck = new SessionDataCacheKey(sdk);
		SessionDataCacheEntry sdce = (SessionDataCacheEntry) SessionDataCache.getInstance()
				.getValueFromCache(ck);
		LinkedHashSet<?> acrValues = sdce.getoAuth2Parameters().getACRValues();
		return acrValues;
	}

}