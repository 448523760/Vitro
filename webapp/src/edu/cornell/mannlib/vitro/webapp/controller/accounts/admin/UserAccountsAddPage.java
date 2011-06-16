/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.controller.accounts.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount.Status;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.accounts.UserAccountsPage;
import edu.cornell.mannlib.vitro.webapp.controller.authenticate.Authenticator;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;

/**
 * Handle the "Add new account" form display and submission.
 * 
 * TODO Associate a profile from this account
 */
public class UserAccountsAddPage extends UserAccountsPage {
	private static final String PARAMETER_SUBMIT = "submitAdd";
	private static final String PARAMETER_EMAIL_ADDRESS = "emailAddress";
	private static final String PARAMETER_EXTERNAL_AUTH_ID = "externalAuthId";
	private static final String PARAMETER_FIRST_NAME = "firstName";
	private static final String PARAMETER_LAST_NAME = "lastName";
	private static final String PARAMETER_ROLE = "role";
	private static final String PARAMETER_ASSOCIATE_WITH_PROFILE = "associate";

	private static final String ERROR_NO_EMAIL = "errorEmailIsEmpty";
	private static final String ERROR_EMAIL_IN_USE = "errorEmailInUse";
	private static final String ERROR_EMAIL_INVALID_FORMAT = "errorEmailInvalidFormat";
	private static final String ERROR_EXTERNAL_AUTH_ID_IN_USE = "errorExternalAuthIdInUse";
	private static final String ERROR_NO_FIRST_NAME = "errorFirstNameIsEmpty";
	private static final String ERROR_NO_LAST_NAME = "errorLastNameIsEmpty";
	private static final String ERROR_NO_ROLE = "errorNoRoleSelected";

	private static final String TEMPLATE_NAME = "userAccounts-add.ftl";

	private final UserAccountsAddPageStrategy strategy;

	/* The request parameters */
	private boolean submit;
	private String emailAddress = "";
	private String externalAuthId = "";
	private String firstName = "";
	private String lastName = "";
	private String selectedRoleUri = "";
	private boolean associateWithProfile;

	/** The result of validating a "submit" request. */
	private String errorCode = "";

	/** The new user account, if one was created. */
	private UserAccount addedAccount;

	public UserAccountsAddPage(VitroRequest vreq) {
		super(vreq);

		this.strategy = UserAccountsAddPageStrategy.getInstance(vreq, this,
				isEmailEnabled());

		parseRequestParameters();

		if (submit) {
			validateParameters();
		}
	}

	private void parseRequestParameters() {
		submit = isFlagOnRequest(PARAMETER_SUBMIT);
		emailAddress = getStringParameter(PARAMETER_EMAIL_ADDRESS, "");
		externalAuthId = getStringParameter(PARAMETER_EXTERNAL_AUTH_ID, "");
		firstName = getStringParameter(PARAMETER_FIRST_NAME, "");
		lastName = getStringParameter(PARAMETER_LAST_NAME, "");
		selectedRoleUri = getStringParameter(PARAMETER_ROLE, "");
		associateWithProfile = isParameterAsExpected(
				PARAMETER_ASSOCIATE_WITH_PROFILE, "yes");

		strategy.parseAdditionalParameters();
	}

	public boolean isSubmit() {
		return submit;
	}

	private void validateParameters() {
		if (emailAddress.isEmpty()) {
			errorCode = ERROR_NO_EMAIL;
		} else if (isEmailInUse()) {
			errorCode = ERROR_EMAIL_IN_USE;
		} else if (!isEmailValidFormat()) {
			errorCode = ERROR_EMAIL_INVALID_FORMAT;
		} else if (isExternalAuthIdInUse()) {
			errorCode = ERROR_EXTERNAL_AUTH_ID_IN_USE;
		} else if (firstName.isEmpty()) {
			errorCode = ERROR_NO_FIRST_NAME;
		} else if (lastName.isEmpty()) {
			errorCode = ERROR_NO_LAST_NAME;
		} else if (selectedRoleUri.isEmpty()) {
			errorCode = ERROR_NO_ROLE;
		} else {
			errorCode = strategy.additionalValidations();
		}
	}

	private boolean isEmailInUse() {
		return userAccountsDao.getUserAccountByEmail(emailAddress) != null;
	}

	private boolean isExternalAuthIdInUse() {
		if (externalAuthId.isEmpty()) {
			return false;
		}
		return userAccountsDao.getUserAccountByExternalAuthId(externalAuthId) != null;
	}

	private boolean isEmailValidFormat() {
		return Authenticator.isValidEmailAddress(emailAddress);
	}

	public boolean isValid() {
		return errorCode.isEmpty();
	}

	public void createNewAccount() {
		UserAccount u = new UserAccount();
		u.setEmailAddress(emailAddress);
		u.setFirstName(firstName);
		u.setLastName(lastName);
		u.setExternalAuthId(externalAuthId);

		u.setMd5Password("");
		u.setOldPassword("");
		u.setPasswordChangeRequired(false);
		u.setPasswordLinkExpires(0);
		u.setLoginCount(0);
		u.setStatus(Status.INACTIVE);

		u.setPermissionSetUris(Collections.singleton(selectedRoleUri));

		strategy.setAdditionalProperties(u);

		String uri = userAccountsDao.insertUserAccount(u);
		this.addedAccount = userAccountsDao.getUserAccountByUri(uri);

		strategy.notifyUser();
	}

	public final ResponseValues showPage() {
		Map<String, Object> body = new HashMap<String, Object>();

		body.put("emailAddress", emailAddress);
		body.put("externalAuthId", externalAuthId);
		body.put("firstName", firstName);
		body.put("lastName", lastName);
		body.put("selectedRole", selectedRoleUri);
		if (associateWithProfile) {
			body.put("associate", Boolean.TRUE);
		}
		body.put("roles", buildRolesList());
		body.put("formUrls", buildUrlsMap());

		if (!errorCode.isEmpty()) {
			body.put(errorCode, Boolean.TRUE);
		}

		strategy.addMoreBodyValues(body);

		return new TemplateResponseValues(TEMPLATE_NAME, body);
	}

	public UserAccount getAddedAccount() {
		return addedAccount;
	}

	public boolean wasPasswordEmailSent() {
		return this.strategy.wasPasswordEmailSent();
	}

}
