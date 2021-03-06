/*******************************************************************************
 * Copyright (c) 2012-2013 RelationWare, Benno Luthiger
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * RelationWare, Benno Luthiger
 ******************************************************************************/

package org.ripla.web.demo.config.views;

import org.ripla.interfaces.IMessages;
import org.ripla.web.demo.config.Activator;
import org.ripla.web.demo.config.controller.LoginConfigController;
import org.ripla.web.util.RiplaViewHelper;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * View to configure the login.
 * 
 * @author Luthiger
 */
@SuppressWarnings("serial")
public class LoginConfigView extends CustomComponent {

	/**
	 * LoginConfigView constructor.
	 * 
	 * @param inLoginConfig
	 * @param inController
	 *            {@link LoginConfigController}
	 * @param inEnabled
	 *            boolean <code>true</code> if login configuration is enabled
	 */
	public LoginConfigView(final boolean inLoginConfig,
			final LoginConfigController inController, final boolean inEnabled) {
		super();
		final IMessages lMessages = Activator.getMessages();
		final VerticalLayout lLayout = new VerticalLayout();
		setCompositionRoot(lLayout);
		lLayout.setStyleName("demo-view"); //$NON-NLS-1$
		lLayout.addComponent(new Label(
				String.format(
						RiplaViewHelper.TMPL_TITLE,
						"demo-pagetitle", lMessages.getMessage("config.login.page.title")), ContentMode.HTML)); //$NON-NLS-1$ //$NON-NLS-2$

		lLayout.addComponent(new Label(lMessages
				.getMessage("view.login.remark"), ContentMode.HTML)); //$NON-NLS-1$
		if (!inEnabled) {
			lLayout.addComponent(new Label(lMessages
					.getMessage("view.login.disabled"), ContentMode.HTML)); //$NON-NLS-1$
		}

		final CheckBox lCheckbox = new CheckBox(
				lMessages.getMessage("view.login.chk.label")); //$NON-NLS-1$
		lCheckbox.setValue(inLoginConfig);
		lCheckbox.setEnabled(inEnabled);
		lCheckbox.focus();
		lLayout.addComponent(lCheckbox);

		final Button lSave = new Button(
				lMessages.getMessage("config.view.button.save")); //$NON-NLS-1$
		lSave.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(final ClickEvent inEvent) {
				inController.saveChange(lCheckbox.getValue());
			}
		});
		lSave.setEnabled(inEnabled);
		lSave.setClickShortcut(KeyCode.ENTER);
		lLayout.addComponent(lSave);
	}

}
