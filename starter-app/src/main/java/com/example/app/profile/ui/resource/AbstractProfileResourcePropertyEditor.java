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

package com.example.app.profile.ui.resource;

import com.example.app.profile.model.Profile;
import com.example.app.profile.model.ProfileDAO;
import com.example.app.profile.model.user.User;
import com.example.app.profile.model.user.UserDAO;
import com.example.app.profile.service.MembershipOperationProvider;
import com.example.app.repository.model.Repository;
import com.example.app.repository.model.RepositoryDAO;
import com.example.app.repository.model.RepositoryItemRelation;
import com.example.app.repository.model.RepositoryItemRelationType;
import com.example.app.repository.model.ResourceRepositoryItem;
import com.example.app.repository.ui.ResourceRepositoryItemValueEditor;
import com.example.app.resource.model.ResourceDAO;
import com.example.app.resource.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Supplier;

import com.i2rd.cms.component.miwt.impl.MIWTPageElementModelPropertyEditor;

import net.proteusframework.core.hibernate.dao.EntityRetriever;
import net.proteusframework.core.html.HTMLElement;
import net.proteusframework.core.locale.annotation.I18N;
import net.proteusframework.core.locale.annotation.I18NFile;
import net.proteusframework.core.locale.annotation.L10N;
import net.proteusframework.ui.management.nav.NavigationAction;
import net.proteusframework.ui.management.nav.config.CurrentURLPropertyValueResolver;
import net.proteusframework.ui.management.nav.config.PropertyValueResolverParameter;
import net.proteusframework.ui.miwt.component.composite.editor.PropertyEditor;
import net.proteusframework.ui.miwt.event.Event;
import net.proteusframework.ui.miwt.util.CommonActions;


/**
 * {@link PropertyEditor} for {@link ResourceRepositoryItem}
 *
 * @author Alan Holt (aholt@venturetech.net)
 * @since 12/11/15 11:14 AM
 */
@I18NFile(
    symbolPrefix = "com.example.app.profile.ui.resource.AbstractProfileResourcePropertyEditor",
    i18n = {
        @I18N(symbol = "Error Message Insufficient Permissions FMT",
            l10n = @L10N("You do not have the correct roles to modify this {0}."))
    }
)
public abstract class AbstractProfileResourcePropertyEditor extends MIWTPageElementModelPropertyEditor<ResourceRepositoryItem>
{
    @Autowired protected RepositoryDAO _repositoryDAO;
    @Autowired protected ProfileDAO _profileDAO;
    @Autowired protected MembershipOperationProvider _mop;
    @Autowired protected UserDAO _userDAO;
    @Autowired protected ResourceDAO _resourceDAO;
    @Autowired protected EntityRetriever _er;

    private boolean _canEdit;
    private ResourceRepositoryItem _saved;
    private RepositoryItemRelation _relation;
    private User _currentUser;

    /**
     * Instantiate a new instance of AbstractProfileResourcePropertyEditor
     */
    public AbstractProfileResourcePropertyEditor()
    {
        super(new ResourceRepositoryItemValueEditor());

        addClassName("resource-repo-item-editor");
        setHTMLElement(HTMLElement.section);
    }

    /**
     * Gets management application function.
     *
     * @return the management application function
     */
    protected abstract String getManagementApplicationFunction();

    /**
     * Gets viewer application function.
     *
     * @return the viewer application function
     */
    protected abstract String getViewerApplicationFunction();

    /**
     * Configure this component
     *
     * @param value the value
     * @param resourceType the resource type
     * @param repo the repo
     * @param owner the owner
     */
    protected void configure(
        @Nullable ResourceRepositoryItem value,
        @Nullable ResourceType resourceType,
        @Nullable Repository repo, @Nullable Profile owner)
    {
        Optional<Profile> oOwner = Optional.ofNullable(owner);
        if (value != null && !_repositoryDAO.isTransient(value))
        {
            resourceType = value.getResource().getResourceType();
            oOwner = _repositoryDAO.getOwnerOfRepository(_repositoryDAO.getOwnerOfRepositoryItem(value));
        }
        assert resourceType != null : "ResourceType was null.  This should not happen unless the URL params are screwed up.";
        getValueEditor().setResourceType(resourceType);
        repo = oOwner.map(Profile::getRepository).orElse(repo);
        getValueEditor().setOwner(repo);
        final TimeZone tz = Event.getRequest().getTimeZone();
        _currentUser = _userDAO.getAssertedCurrentUser();
        _canEdit =
            oOwner.map(profile -> _profileDAO.canOperate(_currentUser, profile, tz, _mop.viewRepositoryResources()))
                .orElse(false)
            && oOwner.map(profile -> _profileDAO.canOperate(_currentUser, profile, tz, _mop.modifyRepositoryResources()))
                .orElse(false);

        final Repository fRepo = repo;
        _relation = value != null && value.getId() != null && value.getId() > 0
            ? _repositoryDAO.getRelation(repo, value).orElse(null)
            : ((Supplier<RepositoryItemRelation>) () -> {
                RepositoryItemRelation relation = new RepositoryItemRelation();
                relation.setRelationType(RepositoryItemRelationType.owned);
                relation.setRepository(fRepo);
                return relation;
            }).get();

        if (_canEdit && _relation != null)
        {
            getValueEditor().setValue(value);
            setSaved(value);
        }
        else
        {
            throw new IllegalArgumentException("Invalid Permissions To View Page");
        }
    }

    /**
     * Set the saved ResourceRepositoryItem to be used for constructing URL properties after saving the ResourceRepositoryItem
     *
     * @param saved the persisted ResourceRepositoryItem
     */
    public void setSaved(@Nullable ResourceRepositoryItem saved)
    {
        _saved = _er.narrowProxyIfPossible(saved);
    }

    /**
     * Gets the saved ResourceRepositoryItem to be used for constructing URL properties.
     *
     * @return the saved
     */
    public ResourceRepositoryItem getSaved()
    {
        return _er.reattachIfNecessary(_saved);
    }

    @Override
    public ResourceRepositoryItemValueEditor getValueEditor()
    {
        return (ResourceRepositoryItemValueEditor) super.getValueEditor();
    }

    @Override
    public void init()
    {
        super.init();

        NavigationAction saveAction = CommonActions.SAVE.navAction();
        saveAction.onCondition(input ->
            persist(toSave -> {
                assert toSave != null : "ResourceRepositoryItem should not be null if you are persisting!";
                toSave.setResource(_resourceDAO.saveResourceImage(
                    toSave.getResource(), getValueEditor().getPictureEditor().commitValue()));
                _relation.setRepositoryItem(toSave);
                setSaved(_repositoryDAO.mergeRepositoryItemRelation(_relation)
                    .getCastRepositoryItem(ResourceRepositoryItem.class));
                return Boolean.TRUE;
            }));
        saveAction.configure().toReturnPath(getViewerApplicationFunction()).withSourceComponent(this);
        saveAction.setPropertyValueResolver(new CurrentURLPropertyValueResolver()
        {
            @Override
            public Map<String, Object> resolve(PropertyValueResolverParameter parameter)
            {
                Map<String, Object> map = super.resolve(parameter);
                map.put(com.example.app.repository.ui.URLProperties.REPOSITORY_ITEM, getSaved());
                return map;
            }
        });
        saveAction.setTarget(this, "close");

        NavigationAction cancelAction = CommonActions.CANCEL.navAction();
        cancelAction.configure().toReturnPath(getManagementApplicationFunction())
            .usingCurrentURLData().withSourceComponent(this);
        cancelAction.setTarget(this, "close");

        if (_canEdit)
        {
            setPersistenceActions(saveAction, cancelAction);
        }
        else
        {
            setPersistenceActions(cancelAction);
        }
    }


}
