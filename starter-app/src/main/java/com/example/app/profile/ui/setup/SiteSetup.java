/*
 * Copyright (c) Interactive Information R & D (I2RD) LLC.
 * All Rights Reserved.
 *
 * This software is confidential and proprietary information of
 * I2RD LLC ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered
 * into with I2RD.
 */

package com.example.app.profile.ui.setup;

import com.example.app.profile.model.company.Company;
import com.example.app.profile.model.company.CompanyStatus;
import com.example.app.profile.model.setup.SiteSetupDAO;
import com.example.app.profile.model.user.User;
import com.example.app.profile.ui.ApplicationFunctions;
import com.example.app.profile.ui.URLConfigurations;
import com.example.app.profile.ui.company.AbstractCompanyPropertyEditor;
import com.example.app.profile.ui.starter.StarterSiteSetupLOK;
import com.example.app.support.ui.Application;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

import com.i2rd.hibernate.util.HibernateUtil;

import net.proteusframework.cms.category.CmsCategory;
import net.proteusframework.cms.label.LabelDomain;
import net.proteusframework.core.locale.annotation.I18N;
import net.proteusframework.core.locale.annotation.I18NFile;
import net.proteusframework.core.locale.annotation.L10N;
import net.proteusframework.ui.management.ApplicationFunction;
import net.proteusframework.ui.management.ParsedRequest;
import net.proteusframework.ui.management.nav.NavigationAction;
import net.proteusframework.ui.management.nav.NavigationDestination;
import net.proteusframework.ui.miwt.event.ActionEvent;
import net.proteusframework.users.model.AuthenticationDomain;

/**
 * Setup page for the starter Site.  Really just allows creation of Company when there are not any
 *
 * @author Alan Holt (aholt@venturetech.net)
 * @since 1/3/17
 */
@I18NFile(
    symbolPrefix = "StarterSiteSetup",
    i18n = {
        @I18N(symbol = "Component Name", l10n = @L10N("Starter Site Setup"))
    }
)
@ApplicationFunction(
    applicationName = Application.NAME,
    sessionName = Application.SESSION,
    name = ApplicationFunctions.StarterSite.SETUP,
    description = "Starter Site Setup",
    urlConfigName = URLConfigurations.Company.EDIT
)
public class SiteSetup extends AbstractCompanyPropertyEditor
{
    @Autowired private SiteSetupDAO _siteSetupDAO;

    /**
     * Instantiates a new Starter site setup.
     */
    public SiteSetup()
    {
        super();
        setName(StarterSiteSetupLOK.COMPONENT_NAME());
        addCategory(CmsCategory.ClientBackend);
        withEditMode(EditMode.DefaultCompany);
    }

    @Override
    protected Consumer<Company> getHostnameSetup()
    {
        return company -> {
            company.setHostname(_appUtil.getSite().getDefaultHostname());
            HibernateUtil.getInstance().setEntityReadOnly(company.getHostname(), false);
            if(company.getHostname().getDomain() == null)
            {
                final AuthenticationDomain authDomain = new AuthenticationDomain();
                authDomain.setDomainName(company.getHostname().getName());
                company.getHostname().setDomain(authDomain);
            }
        };
    }

    @Override
    protected Runnable getPostSave()
    {
        return () -> {
            _siteSetupDAO.getAdminsNeedingUser().forEach(p -> {
                User newUser = new User();
                newUser.setPrincipal(p);
                _companyDAO.addUserToCompany(getSaved(), newUser);
            });
            Company saved = getSaved();
            saved.setStatus(CompanyStatus.Active);
            setSaved(_companyDAO.saveCompany(saved));
        };
    }

    @Override
    protected Consumer<NavigationAction> getPostPersist()
    {
        return action -> {
            Company saved = getSaved();
            final LabelDomain rcld = new LabelDomain();
            rcld.setProgrammaticIdentifier("company-resourcecategories-" + saved.getId());
            rcld.setName(_rclp.getLabelDomain().getName().createCopy());
            _labelDAO.saveLabelDomain(rcld);
            copyLabelsFromLabelDomainProvider(_rclp, rcld);
            final LabelDomain rtld = new LabelDomain();
            rtld.setProgrammaticIdentifier("company-resourcetags-" + saved.getId());
            rtld.setName(_rtlp.getLabelDomain().getName().createCopy());
            _labelDAO.saveLabelDomain(rtld);
            copyLabelsFromLabelDomainProvider(_rtlp, rtld);

            saved.setResourceCategories(_er.reattachIfNecessary(rcld));
            saved.setResourceTags(_er.reattachIfNecessary(rtld));
            setSaved(_companyDAO.saveCompany(saved));

            action.actionPerformed(new ActionEvent(this, this, "companySaved"));
        };
    }

    @Override
    protected void configure(ParsedRequest request)
    {
        super.configure(request);

        if(!_siteSetupDAO.needsSetup())
        {
            NavigationDestination destination = new NavigationDestination(ApplicationFunctions.Company.MANAGEMENT);
            destination.setRecordNavigation(false);
            destination.setUseRedirect(true);
            destination.redirect();
        }
    }
}
