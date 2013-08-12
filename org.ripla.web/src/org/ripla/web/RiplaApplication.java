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

package org.ripla.web; // NOPMD by Luthiger on 09.09.12 00:42

import java.util.Dictionary;
import java.util.Locale;

import org.lunifera.runtime.web.vaadin.osgi.common.OSGiUI;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.ripla.interfaces.IAppConfiguration;
import org.ripla.interfaces.IAuthenticator;
import org.ripla.interfaces.IRiplaEventDispatcher;
import org.ripla.interfaces.IWorkflowListener;
import org.ripla.services.IExtendibleMenuContribution;
import org.ripla.services.IPermissionEntry;
import org.ripla.services.ISkinService;
import org.ripla.util.PreferencesHelper;
import org.ripla.web.controllers.RiplaBody;
import org.ripla.web.internal.services.ConfigManager;
import org.ripla.web.internal.services.PermissionHelper;
import org.ripla.web.internal.services.RiplaEventDispatcher;
import org.ripla.web.internal.services.SkinRegistry;
import org.ripla.web.internal.services.ToolbarItemRegistry;
import org.ripla.web.internal.services.UseCaseManager;
import org.ripla.web.internal.views.RiplaLogin;
import org.ripla.web.services.ISkin;
import org.ripla.web.services.IToolbarItem;
import org.ripla.web.services.IUseCase;
import org.ripla.web.util.RiplaRequestHandler;
import org.ripla.web.util.ToolbarItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * <p>
 * The base class of all web applications using the Ripla platform.
 * </p>
 * <p>
 * Subclasses may override the following methods:<br />
 * <ul>
 * <li>{@link #getAppConfiguration()}</li>
 * <li>{@link #createBodyView(ISkin)}</li>
 * <li>{@link #createPreferencesHelper()}</li>
 * <li>{@link #beforeLogin()}</li>
 * <li>{@link #workflowExit()}</li>
 * <li>{@link #initializePermissions()}</li>
 * </ul>
 * </p>
 * 
 * @author Luthiger
 */
@SuppressWarnings("serial")
public class RiplaApplication extends OSGiUI implements ManagedService,
		IWorkflowListener { // NOPMD
	private static final Logger LOG = LoggerFactory
			.getLogger(RiplaApplication.class);

	private static final String APP_NAME = "Ripla";

	private final PreferencesHelper preferences = createPreferencesHelper();
	private final ConfigManager configManager = new ConfigManager();
	private final SkinRegistry skinRegistry = new SkinRegistry(preferences);
	private final ToolbarItemRegistry toolbarRegistry = new ToolbarItemRegistry();
	private final UseCaseManager useCaseHelper = new UseCaseManager();
	private RiplaEventDispatcher eventDispatcher;
	private final PermissionHelper permissionHelper = new PermissionHelper();
	private ToolbarItemFactory toolbarItemFactory;

	// private String requestURL; // old vaadin HttpServletRequestListener
	private RiplaRequestHandler requestHandler;
	private Layout bodyView;
	private boolean initialized = false;

	@Override
	public final void init(final VaadinRequest inRequest) {
		initialized = true;

		toolbarItemFactory = new ToolbarItemFactory(preferences, configManager,
				null);

		// synchronize language settings
		setSessionLocale(preferences.getLocale(getLocale()));

		setSessionPreferences(preferences);

		eventDispatcher = new RiplaEventDispatcher();
		VaadinSession.getCurrent().setAttribute(IRiplaEventDispatcher.class,
				eventDispatcher);
		useCaseHelper.registerContextMenus();
		if (!initializeLayout(getAppConfiguration())) {
			return;
		}
	}

	private void setSessionPreferences(final PreferencesHelper inPreferences) {
		VaadinSession.getCurrent().setAttribute(PreferencesHelper.class,
				inPreferences);
	}

	private void setSessionLocale(final Locale inLocale) {
		VaadinSession.getCurrent().setLocale(inLocale);
	}

	private void setSessionUser(final User inUser) {
		VaadinSession.getCurrent().setAttribute(User.class, inUser);
	}

	/**
	 * Subclasses may override to provide their own
	 * <code>PreferencesHelper</code>.
	 * 
	 * @return PreferencesHelper
	 */
	protected PreferencesHelper createPreferencesHelper() {
		return new PreferencesHelper();
	}

	/**
	 * Returns the configuration object to configure the application.<br />
	 * Subclasses may override.
	 * 
	 * @return {@link IAppConfiguration} the configuration object.
	 */
	protected IAppConfiguration getAppConfiguration() {
		return new IAppConfiguration() {
			@Override
			public String getWelcome() {
				return null;
			}

			@Override
			public String getDftSkinID() {
				return Constants.DFT_SKIN_ID;
			}

			@Override
			public IAuthenticator getLoginAuthenticator() {
				return null;
			}

			@Override
			public String getAppName() {
				return APP_NAME;
			}
		};
	}

	/**
	 * @return String the application's name, as configured in
	 *         <code>IAppConfiguration.getAppName()</code>
	 */
	public String getAppName() {
		return getAppConfiguration().getAppName();
	}

	private boolean initializeLayout(final IAppConfiguration inConfiguration) {
		setStyleName("ripla-window"); //$NON-NLS-1$
		// inMain.addListener(new Window.CloseListener() {
		// @Override
		// public void windowClose(final CloseEvent inEvent) {
		// close();
		// }
		// });

		requestHandler = setRequestHandler();
		skinRegistry.setDefaultSkin(inConfiguration.getDftSkinID());
		final ISkin lSkin = skinRegistry.getActiveSkin();

		final VerticalLayout lLayout = new VerticalLayout();
		lLayout.setSizeFull();
		lLayout.setStyleName("ripla-main");
		setContent(lLayout);

		bodyView = createBody();
		lLayout.addComponent(bodyView);
		lLayout.setExpandRatio(bodyView, 1);

		if (!beforeLogin(this)) {
			return false;
		}

		if (inConfiguration.getLoginAuthenticator() == null) {
			bodyView.addComponent(createBodyView(lSkin));
		} else {
			bodyView.addComponent(createLoginView(inConfiguration, lSkin));
		}

		if (lSkin.hasFooter()) {
			final Component lFooter = lSkin.getFooter();
			lLayout.addComponent(lFooter);
			lLayout.setExpandRatio(lFooter, 0);
		}
		return true;
	}

	/**
	 * Callback method to display the application's views after the user has
	 * successfully logged in.
	 * 
	 * @param inUser
	 *            {@link User} the user instance
	 */
	public void showAfterLogin(final User inUser) {
		toolbarItemFactory.setUser(inUser);
		setSessionUser(inUser);
		setSessionLocale(preferences.getLocale(inUser, getLocale()));
		refreshBody();
	}

	/**
	 * Hook for application configuration.<br />
	 * Subclasses may override to plug in a configuration workflow.
	 * 
	 * @param inMain
	 *            {@link Window} the application's main window
	 * @param inWorkflowListener
	 *            {@link IWorkflowListener} the listener of the application
	 *            workflow configuration
	 * @return boolean <code>true</code> in case there's no need of application
	 *         configuration and, therefore, the startup process can continue,
	 *         <code>false</code> if the startup is handed over to the
	 *         application configuration workflow.
	 */
	protected boolean beforeLogin(final IWorkflowListener inWorkflowListener) {
		return true;
	}

	private RiplaRequestHandler setRequestHandler() {
		final RiplaRequestHandler out = new RiplaRequestHandler("requestURL",
				useCaseHelper);
		// inMain.addParameterHandler(out);
		return out;
	}

	private Layout createBody() {
		final Layout outBody = new VerticalLayout();
		outBody.setStyleName("ripla-body");
		outBody.setSizeFull();
		return outBody;
	}

	/**
	 * Refreshes the body component.
	 */
	public final void refreshBody() {
		bodyView.removeAllComponents();
		bodyView.addComponent(createBodyView(skinRegistry.getActiveSkin()));
	}

	/**
	 * Creates the application's body view.<br />
	 * Subclasses may override to provide their own body views.
	 * <p>
	 * This implementation creates an instance of {@link RiplaBody}, notifies
	 * the event handler about the new body component, calls the request handler
	 * for that a requested view can be displayed in the main view and then
	 * passes the new view back to the application.
	 * </p>
	 * 
	 * @param inSkin
	 *            {@link ISkin} the actual application skin
	 * @return {@link Component} the application's body view
	 */
	protected Component createBodyView(final ISkin inSkin) {
		final RiplaBody out = RiplaBody.createInstance(inSkin, toolbarRegistry,
				useCaseHelper, this);

		eventDispatcher.setBodyComponent(out);

		// if (!requestHandler.process(out)) {
		// out.showDefault();
		// }
		return out;
	}

	/**
	 * Creates the application's login view.<br />
	 * Subclasses may override.
	 * 
	 * @param inConfiguration
	 *            {@link IAppConfiguration} the application's configuration
	 *            object
	 * @param inSkin
	 *            {@link ISkin} the actual application skin
	 * @return {@link Component} the application's login view
	 */
	private Component createLoginView(final IAppConfiguration inConfiguration,
			final ISkin inSkin) {
		final VerticalLayout out = new VerticalLayout();
		out.setStyleName("ripla-body");
		out.setSizeFull();

		if (inSkin.hasHeader()) {
			final Component lHeader = inSkin.getHeader(inConfiguration
					.getAppName());
			out.addComponent(lHeader);
			out.setExpandRatio(lHeader, 0);
		}

		final RiplaLogin lLogin = new RiplaLogin(inConfiguration, this,
				useCaseHelper.getUserAdmin());
		out.addComponent(lLogin);
		return out;
	}

	// @Override
	// public final void handleEvent(final org.osgi.service.event.Event inEvent)
	// {
	// eventHandler.handleEvent(inEvent);
	// }

	@SuppressWarnings("rawtypes")
	@Override
	public void updated(final Dictionary inProperties)
			throws ConfigurationException {
		if (inProperties == null) {
			return;
		}

		synchronized (skinRegistry) {
			final String lLanguage = (String) inProperties
					.get(ConfigManager.KEY_LANGUAGE);
			getPreferences().set(PreferencesHelper.KEY_LANGUAGE, lLanguage);

			final String lSkinID = (String) inProperties
					.get(ConfigManager.KEY_SKIN);
			skinRegistry.changeSkin(lSkinID);
		}
	}

	/**
	 * We want to synchronize the metadata value if the skin id is changed by
	 * the application.
	 * 
	 * @see com.vaadin.Application#setTheme(java.lang.String)
	 */
	// @Override
	// public void setTheme(final String inTheme) {
	// configManager.setSkinID(inTheme);
	// super.setTheme(inTheme);
	// }

	/**
	 * We want to save the locale to the preferences store.
	 * 
	 * @see com.vaadin.Application#setLocale(java.util.Locale)
	 */
	@Override
	public void setLocale(final Locale inLocale) {
		if (initialized) {
			final User lUser = VaadinSession.getCurrent().getAttribute(
					User.class);
			if (lUser == null) {
				preferences.setLocale(inLocale);
			} else {
				preferences.setLocale(inLocale, lUser);
			}
		}
		super.setLocale(inLocale);
	}

	/**
	 * Subclasses may override.
	 * 
	 * @see org.ripla.web.interfaces.IWorkflowListener#workflowExit(int,
	 *      java.lang.String)
	 */
	@Override
	public void workflowExit(final int inReturnCode, final String inMessage) {
		// intentionally left empty
	}

	/**
	 * Allows access to the application's preferences.
	 * 
	 * @return {@link PreferencesHelper}
	 */
	public PreferencesHelper getPreferences() {
		return preferences;
	}

	/**
	 * Allows access to the application's skin registry.
	 * 
	 * @return {@link SkinRegistry}
	 */
	public SkinRegistry getSkinRegistry() {
		return skinRegistry;
	}

	/**
	 * <p>
	 * Subclasses that want to make use of the OSGi user admin service
	 * functionality may trigger initialization of registered permissions.
	 * </p>
	 * 
	 * <p>
	 * This will create a permission group for each registered
	 * <code>IPermissionEntry</code> and add the members defined in those
	 * permission instances.
	 * </p>
	 * 
	 * <p>
	 * This requires that such member instances exist already. Thus, subclasses
	 * have to prepare groups (i.e. roles) that can act as members for the
	 * registered permission groups before the registered permission instances
	 * are processed.
	 * </p>
	 * <p>
	 * This method is best called in the subclass's
	 * <code>setUserAdmin(UserAdmin inUserAdmin)</code> method:
	 * 
	 * <pre>
	 * public void setUserAdmin(UserAdmin inUserAdmin) {
	 * 	super.setUserAdmin(inUserAdmin);
	 * 	Group lAdministrators = (Group) inUserAdmin.createRole(&quot;ripla.admin&quot;,
	 * 			Role.GROUP);
	 * 	initializePermissions();
	 * }
	 * </pre>
	 * 
	 * </p>
	 */
	protected void initializePermissions() {
		permissionHelper.initializePermissions();
	}

	/**
	 * The factory method to create a toolbar component instance. <br />
	 * Toolbar items created with this factory must have a constructor with the
	 * following parameters:
	 * <ul>
	 * <li>org.ripla.web.util.PreferencesHelper</li>
	 * <li>org.ripla.web.internal.services.ConfigManager</li>
	 * <li>org.osgi.service.useradmin.User</li>
	 * </ul>
	 * 
	 * @param inClass
	 *            Class the toolbar component class
	 * @return {@link Component} the created toolbar component instance or
	 *         <code>null</code> in case of an error
	 */
	public <T extends Component> T createToolbarItem(final Class<T> inClass) {
		try {
			return toolbarItemFactory.createToolbarComponent(inClass);
		}
		catch (final Exception exc) {
			LOG.error("Error encountered while creating the toolbar item!", exc);
		}
		return null;
	}

	// --- OSGi DS bind and unbind methods ---

	public void setPreferences(final PreferencesService inPreferences) {
		preferences.setPreferences(inPreferences);
		LOG.debug("The OSGi preferences service is made available.");
	}

	public void unsetPreferences(final PreferencesService inPreferences) {
		preferences.dispose();
		LOG.debug("Removed the OSGi preferences service.");
	}

	public void setUserAdmin(final UserAdmin inUserAdmin) {
		useCaseHelper.setUserAdmin(inUserAdmin);
		permissionHelper.setUserAdmin(inUserAdmin);
		LOG.debug("The OSGi user admin service is made available.");
	}

	public void unsetUserAdmin(final UserAdmin inUserAdmin) {
		useCaseHelper.setUserAdmin(null);
		permissionHelper.setUserAdmin(null);
		LOG.debug("Removed the OSGi user admin service is made available.");
	}

	public void registerSkin(final ISkinService inSkin) {
		LOG.debug("Registered skin '{}'.", inSkin.getSkinID());
		skinRegistry.registerSkin(inSkin);
	}

	public void unregisterSkin(final ISkinService inSkin) {
		LOG.debug("Unregistered skin '{}'.", inSkin.getSkinID());
		skinRegistry.unregisterSkin(inSkin);
	}

	public void registerToolbarItem(final IToolbarItem inItem) {
		LOG.debug("Registered the toolbar item '{}'.", inItem);
		toolbarRegistry.registerToolbarItem(inItem);
	}

	public void unregisterToolbarItem(final IToolbarItem inItem) {
		LOG.debug("Unregistered the toolbar item '{}'.", inItem);
		toolbarRegistry.unregisterToolbarItem(inItem);
	}

	public void addUseCase(final IUseCase inUseCase) {
		LOG.debug("Added use case {}.", inUseCase);
		useCaseHelper.addUseCase(inUseCase);
	}

	public void removeUseCase(final IUseCase inUseCase) {
		LOG.debug("Removed use case {}.", inUseCase);
		useCaseHelper.removeUseCase(inUseCase);
	}

	public void registerMenuContribution(
			final IExtendibleMenuContribution inMenuContribution) {
		LOG.debug("Registered extendible menu contribution '{}'.",
				inMenuContribution.getExtendibleMenuID());
		useCaseHelper.registerMenuContribution(inMenuContribution);
	}

	public void unregisterMenuContribution(
			final IExtendibleMenuContribution inMenuContribution) {
		LOG.debug("Unregistered extendible menu contribution '{}'.",
				inMenuContribution.getExtendibleMenuID());
		useCaseHelper.unregisterMenuContribution(inMenuContribution);
	}

	public void registerPermission(final IPermissionEntry inPermission) {
		LOG.debug("Registered permission '{}'.",
				inPermission.getPermissionName());
		permissionHelper.addPermission(inPermission);
	}

	public void unregisterPermission(final IPermissionEntry inPermission) {
		LOG.debug("Unregistered permission '{}'.",
				inPermission.getPermissionName());
		permissionHelper.removePermission(inPermission);
	}

	public void setConfiAdmin(final ConfigurationAdmin inConfigAdmin) {
		configManager.setConfigAdmin(inConfigAdmin);
	}

	public void unsetConfiAdmin(final ConfigurationAdmin inConfigAdmin) {
		configManager.clearConfigAdmin();
	}

}
