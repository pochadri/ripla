/*******************************************************************************
 * Copyright (c) 2012 RelationWare, Benno Luthiger
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * RelationWare, Benno Luthiger
 ******************************************************************************/

package org.ripla.demo;

import java.util.Dictionary;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.ripla.web.RiplaApplication;
import org.ripla.web.exceptions.LoginException;
import org.ripla.web.interfaces.IAppConfiguration;
import org.ripla.web.interfaces.IAuthenticator;

import com.vaadin.ui.Window;

/**
 * The Demo application class.
 * 
 * @author Luthiger
 */
@SuppressWarnings("serial")
public class DemoApplication extends RiplaApplication {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ripla.web.RiplaApplication#createWindow()
	 */
	@Override
	protected Window createWindow() {
		final Window outWindow = new Window("Ripla Demo Application");
		setMainWindow(outWindow);
		return outWindow;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ripla.web.RiplaApplication#getAppConfiguration()
	 */
	@Override
	protected IAppConfiguration getAppConfiguration() {
		return new IAppConfiguration() {
			@Override
			public String getWelcome() {
				return Activator.getMessages().getMessage("login.welcome");
			}

			@Override
			public String getDftSkinID() {
				return "org.ripla.demo.skin";
			}

			@Override
			public IAuthenticator getLoginAuthenticator() {
				if (Boolean.parseBoolean(getPreferences().get(
						Constants.KEY_LOGIN, Boolean.FALSE.toString()))) {
					return new IAuthenticator() {
						@Override
						public User authenticate(final String inName,
								final String inPassword,
								final UserAdmin inUserAdmin)
								throws LoginException {
							final User outUser = inUserAdmin.getUser(
									Constants.KEY_USER, inName);
							if (outUser == null) {
								throw new LoginException(Activator
										.getMessages().getFormattedMessage(
												"login.failure.name", inName));
							}
							if (!outUser.hasCredential(Constants.KEY_PW,
									inPassword)) {
								throw new LoginException(Activator
										.getMessages().getMessage(
												"login.failure.credentials"));
							}
							return outUser;
						}
					};
				}
				return null;
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ripla.web.RiplaApplication#setUserAdmin(org.osgi.service.useradmin
	 * .UserAdmin)
	 */
	@Override
	public void setUserAdmin(final UserAdmin inUserAdmin) {
		super.setUserAdmin(inUserAdmin);

		final User lAdmin = (User) inUserAdmin.createRole(
				Constants.USER_NAME_ADMIN, Role.USER);
		setNamePassword(lAdmin, Constants.USER_NAME_ADMIN,
				Constants.USER_PW_ADMIN);

		final User lUser = (User) inUserAdmin.createRole(
				Constants.USER_NAME_USER, Role.USER);
		setNamePassword(lUser, Constants.USER_NAME_USER, Constants.USER_PW_USER);

		final Group lAdministrators = (Group) inUserAdmin.createRole(
				Constants.ADMIN_GROUP_NAME, Role.GROUP);
		if (lAdministrators != null) {
			lAdministrators.addRequiredMember(lAdmin);
			lAdministrators.addMember(inUserAdmin.getRole(Role.USER_ANYONE));
		}
		initializePermissions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setNamePassword(final User inUser, final String inName,
			final String inPass) {
		if (inUser == null) {
			return;
		}

		Dictionary lProperties = inUser.getProperties();
		lProperties.put(Constants.KEY_USER, inName);

		lProperties = inUser.getCredentials();
		lProperties.put(Constants.KEY_PW, inPass);
	}

}