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

package com.example.app.profile.model.company;

import com.example.app.config.ProjectCacheRegions;
import com.example.app.profile.model.ProfileDAO;
import com.example.app.profile.model.ProfileType;
import com.example.app.profile.model.membership.Membership;
import com.example.app.profile.model.membership.MembershipOperation;
import com.example.app.profile.model.membership.MembershipType;
import com.example.app.profile.model.membership.MembershipTypeInfo;
import com.example.app.profile.model.terminology.FallbackProfileTermProvider;
import com.example.app.profile.model.terminology.ProfileTermProvider;
import com.example.app.profile.model.terminology.ProfileTerms;
import com.example.app.profile.model.user.User;
import com.example.app.profile.model.user.UserDAO;
import com.example.app.profile.service.MembershipOperationConfiguration;
import com.example.app.support.service.AppUtil;
import com.example.app.support.service.EntityIdCollector;
import com.example.app.support.service.FileSaver;
import org.apache.commons.fileupload.FileItem;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.proteusframework.cms.FileSystemDirectory;
import net.proteusframework.cms.dao.CmsFrontendDAO;
import net.proteusframework.core.GloballyUniqueStringGenerator;
import net.proteusframework.core.hibernate.dao.DAOHelper;
import net.proteusframework.core.hibernate.dao.EntityRetriever;
import net.proteusframework.core.locale.TransientLocalizedObjectKey;
import net.proteusframework.core.locale.annotation.I18N;
import net.proteusframework.core.locale.annotation.I18NFile;
import net.proteusframework.core.locale.annotation.L10N;
import net.proteusframework.core.spring.ApplicationContextUtils;
import net.proteusframework.data.filesystem.DirectoryEntity;
import net.proteusframework.data.filesystem.FileSystemDAO;
import net.proteusframework.internet.http.Hostname;
import net.proteusframework.ui.search.QLBuilder;
import net.proteusframework.ui.search.QLBuilderImpl;
import net.proteusframework.users.config.UsersCacheRegions;
import net.proteusframework.users.model.AuthenticationDomain;
import net.proteusframework.users.model.Principal;

import static java.util.Collections.singletonMap;
import static net.proteusframework.core.StringFactory.isEmptyString;
import static net.proteusframework.ui.search.PropertyConstraint.Operator.eq;

/**
 * {@link DAOHelper} implementation for {@link Company}
 *
 * @author Alan Holt (aholt@venturetech.net)
 * @since 11 /23/15 9:32 AM
 */
@I18NFile(
    symbolPrefix = "com.lrsuccess.ldp.model.profile.CompanyDAO",
    i18n = {
        @I18N(symbol = "Error {0} is a required {1} for {2} {3} stage", l10n = @L10N("{0} is a required {1} for {2} {3} stage.")),
        @I18N(symbol = "Error {0} {1} count exceeds limit of {2}.", l10n = @L10N("{0} {1} count exceeds limit of {2}."))
    }
)
@Repository
@Lazy
public class CompanyDAO extends DAOHelper implements Serializable
{
    private static final long serialVersionUID = -1864046652465972120L;
    @Autowired private transient AppUtil _appUtil;
    @Autowired private transient ProfileDAO _profileDAO;
    @Autowired private transient UserDAO _userDAO;
    @Autowired private transient MembershipOperationConfiguration _mop;
    @Autowired private transient CmsFrontendDAO _cmsFrontendDAO;
    @Autowired private transient FileSystemDAO _fileSystemDAO;
    @Value("${admin-access-role}") private String _adminRoleProgId;
    @Autowired private transient EntityRetriever _er;

    private FileSaver<Company> _webImageSaver;
    private FileSaver<Company> _emailLogoSaver;


    /**
     * Gets image saver.
     *
     * @return the image saver
     */
    @Nonnull
    protected FileSaver<Company> getWebLogoImageSaver()
    {
        if(_webImageSaver == null)
        {
            _webImageSaver = new FileSaver<>(
                (company, image) -> {
                    company.setImage(image);
                    return company;
                },
                Company::getImage,
                (company, image) -> {
                    final DirectoryEntity dir = FileSystemDirectory.Pictures.getDirectory2(
                        _cmsFrontendDAO.getOperationalSite());
                    return _fileSystemDAO.mkdirs(dir, null, "CompanyPictures", company.getId().toString());
                },
                (company, image) -> "web-logo" + AppUtil.getExtensionWithDot(image),
                this::saveCompany);
        }
        return _webImageSaver;
    }

    /**
     * Gets image saver.
     *
     * @return the image saver
     */
    @Nonnull
    protected FileSaver<Company> getEmailLogoImageSaver()
    {
        if(_emailLogoSaver == null)
        {
            _emailLogoSaver = new FileSaver<>(
                (company, image) -> {
                    company.setEmailLogo(image);
                    return company;
                },
                Company::getEmailLogo,
                (company, image) -> {
                    final DirectoryEntity dir = FileSystemDirectory.Pictures.getDirectory2(
                        _cmsFrontendDAO.getOperationalSite());
                    return _fileSystemDAO.mkdirs(dir, null, "CompanyPictures", company.getId().toString());
                },
                (company, image) -> "email-logo" + AppUtil.getExtensionWithDot(image),
                this::saveCompany);
        }
        return _emailLogoSaver;
    }

    /**
     * Add user to company user, merging the user if necessary.
     *
     * @param company the company.
     * @param user the user.
     *
     * @return the merged user if the specified user was transient.
     */
    public User addUserToCompany(Company company, User user)
    {
        return
        doInTransaction(session -> {
            AuthenticationDomain domain = company.getHostname().getDomain();
            if(domain != null && !user.getPrincipal().getAuthenticationDomains().contains(domain))
                user.getPrincipal().getAuthenticationDomains().add(domain);
            user.getPrincipal().getCredentials().forEach(session::saveOrUpdate);
            final User mergedUser = _userDAO.mergeUser(user);
            company.addUser(mergedUser);
            saveCompany(company);
            return mergedUser;
        });
    }

    /**
     * Save the given Company into the database
     *
     * @param company the company to save
     * @return the saved Company
     */
    public Company saveCompany(Company company)
    {
        BiConsumer<Company, Session> presave = (toSave, session) -> {
            if(toSave.getProfileTerms() == null)
                toSave.setProfileTerms(new ProfileTerms());
            AuthenticationDomain domain = toSave.getHostname().getDomain();
            if(domain != null
               && (domain.getId() == null || domain.getId() == 0L))
            {
                session.save(domain);
            }
        };
        if(isAttached(company))
        {
            return doInTransaction(session -> {
                presave.accept(company, session);
                session.saveOrUpdate(company);
                return company;
            });
        }
        else
        {
            return doInTransaction(session -> {
                presave.accept(company, session);
                return (Company)session.merge(company);
            });
        }
    }

    /**
     * Merge profile terms.
     *
     * @param profileTerms the profile terms.
     * @return the profile terms.
     */
    public ProfileTerms mergeProfileTerms(ProfileTerms profileTerms)
    {
        // Currently profileTerms should always be persistent
        return doInTransaction(session -> (ProfileTerms) session.merge(profileTerms));
    }

    /**
     * Save company image.
     *
     * @param company the company
     * @param image the image
     *
     * @return the company
     */
    @Nonnull
    public Company saveCompanyImage(@Nonnull Company company, @Nullable FileItem image)
    {
        return getWebLogoImageSaver().save(company, image);
    }

    /**
     * Save company image.
     *
     * @param company the company
     * @param image the image
     *
     * @return the company
     */
    @Nonnull
    public Company saveCompanyEmailLogo(@Nonnull Company company, @Nullable FileItem image)
    {
        return getEmailLogoImageSaver().save(company, image);
    }

    /**
     * Delete the given Company from the database
     *
     * @param company the company to delete
     */
    public void deleteCompany(Company company)
    {
        doInTransaction(session -> {
            session.delete(company);
        });
    }

    /**
     * Get the Company whose ID corresponds to the given ID
     *
     * @param id the ID to look for
     *
     * @return the matching Company, or null of none exists
     */
    @Nullable
    public Company getCompany(@Nullable Integer id)
    {
        if(id == null || id == 0L) return null;
        return (Company)getSession().get(Company.class, id);
    }

    /**
     * Get the Company shoe programmaticIdentifier corresponds to the given programmatic identifier
     *
     * @param programmaticId the programmatic identifier to look for
     *
     * @return the matching Company, or null if none exists
     */
    @Nullable
    public Company getCompany(@Nullable String programmaticId)
    {
        if(isEmptyString(programmaticId)) return null;
        return (Company) getCompanyQLBuilder()
            .appendCriteria(Company.PROGRAMMATIC_IDENTIFIER_COLUMN_PROP, eq, programmaticId)
            .getQueryResolver().createQuery(getSession()).uniqueResult();
    }

    /**
     * Create a new ProfileType for a Company, giving it the given programmatic identifier.
     *
     * @param progId the programmatic identifier.  If null, one is generated.
     *
     * @return the un-persisted profile type
     */
    public ProfileType createCompanyProfileType(@Nullable String progId)
    {
        ProfileType pt = new ProfileType();
        TransientLocalizedObjectKey tlok = new TransientLocalizedObjectKey(singletonMap(Locale.ENGLISH, "Company PT"));

        pt.setName(tlok);
        pt.setProgrammaticIdentifier(isEmptyString(progId) ? GloballyUniqueStringGenerator.getUniqueString() : progId);

        pt.getMembershipTypeSet().add(createMembershipType(pt, MembershipTypeInfo.SystemAdmin,
            () -> _mop.getOperations()));

        return pt;
    }

    /**
     * New client profile type.
     *
     * @param progId the prog id
     *
     * @return the profile type
     */
    public ProfileType createClientProfileType(@Nullable String progId)
    {
        ProfileType pt = new ProfileType();
        TransientLocalizedObjectKey tlok = new TransientLocalizedObjectKey(singletonMap(Locale.ENGLISH, "Client PT"));

        pt.setName(tlok);
        pt.setProgrammaticIdentifier(isEmptyString(progId) ? GloballyUniqueStringGenerator.getUniqueString() : progId);

        pt.getMembershipTypeSet().add(createMembershipType(pt, MembershipTypeInfo.SystemAdmin, () -> {
            List<MembershipOperation> mops = new ArrayList<>();
            mops.add(_mop.modifyCompany());
            return mops;
        }));

        return pt;
    }

    /**
     * Gets company term provider.
     *
     * @param company the company.
     *
     * @return the company term provider.
     */
    public ProfileTermProvider getCompanyTermProvider(@Nonnull Company company)
    {
        return new FallbackProfileTermProvider(company.getProfileTerms());
    }

    /**
     * New membership type membership type.
     *
     * @param pt the pt
     * @param name the name
     * @param operations the operations
     * @param progId the prog id
     *
     * @return the membership type
     */
    public MembershipType createMembershipType(ProfileType pt, String name,
        Supplier<List<MembershipOperation>> operations, @Nullable String progId)
    {
        MembershipType mt = new MembershipType();
        TransientLocalizedObjectKey tlok = new TransientLocalizedObjectKey(singletonMap(Locale.ENGLISH, name));
        TransientLocalizedObjectKey dtlok = new TransientLocalizedObjectKey(singletonMap(Locale.ENGLISH, name));

        mt.setName(tlok);
        mt.setDescription(dtlok);
        mt.setProgrammaticIdentifier(isEmptyString(progId) ? GloballyUniqueStringGenerator.getUniqueString() : progId);
        mt.setDefaultOperations(operations.get());
        mt.setProfileType(pt);

        return mt;
    }

    /**
     * New membership type membership type.
     *
     * @param pt the pt
     * @param info the info
     * @param operations the operations
     *
     * @return the membership type
     */
    public MembershipType createMembershipType(ProfileType pt, MembershipTypeInfo info,
        Supplier<List<MembershipOperation>> operations)
    {
        MembershipType mt = new MembershipType();
        TransientLocalizedObjectKey tlok = info.getNewNameLocalizedObjectKey();
        TransientLocalizedObjectKey dtlok = info.getNewNameLocalizedObjectKey();

        mt.setName(tlok);
        mt.setDescription(dtlok);
        mt.setProgrammaticIdentifier(info.getProgId());
        mt.setDefaultOperations(operations.get());
        mt.setProfileType(pt);

        return mt;
    }

    /**
     * Get a QLBuilder instance for a Company
     *
     * @return a QLBuilder
     */
    @Nonnull
    public QLBuilder getCompanyQLBuilder()
    {
        return new QLBuilderImpl(Company.class, "companyAlias");
    }

    /**
     * Get a list of all Memberships for the given User that are associated with a Company
     *
     * @param user the User
     * @param timeZone the timezone.
     *
     * @return a list of Memberships
     */
    @Nonnull
    public List<Membership> getMemberships(@Nonnull User user, TimeZone timeZone)
    {
        return _profileDAO.getMembershipsForProfileSubClass(user, Company.class, timeZone);
    }

    /**
     * Get a list of all active Companies that the given User has a Membership for
     *
     * @param user the user to search for
     * @param timeZone the current TimeZone
     *
     * @return a list of Companies that the given User has a Membership for
     */
    @Nonnull
    public List<Company> getCompaniesThatUserHasMembershipFor(@Nullable User user, TimeZone timeZone)
    {
        return _profileDAO.getProfilesThatUserHasMembershipFor(user, Company.class, timeZone)
            .stream()
            .filter(coaching -> coaching.getStatus() == CompanyStatus.Active)
            .collect(Collectors.toList());
    }

    /**
     * Gets active companies.
     *
     * @return the active companies
     */
    @SuppressWarnings("unchecked")
    public List<Company> getActiveCompanies()
    {
        return doInTransaction(session -> (List<Company>)session.createQuery(
            "SELECT ce FROM Company ce\n"
            + "WHERE ce.status = :active")
            .setParameter("active", CompanyStatus.Active)
            .list());
    }

    /**
     * Gets active companies for a user.
     * This is all {@link User#getCompanies()} less any
     * Companies whose hostname authentication domain is not
     * present on the {@link User#getPrincipal()}.
     * @param user the user.
     * @return the active companies for the user.
     */
    @SuppressWarnings("unchecked")
    public List<Company> getActiveCompanies(User user)
    {
        List<Long> domainIds = _er.reattachIfNecessary(user.getPrincipal()).getAuthenticationDomains()
            .stream().map(AuthenticationDomain::getId).collect(new EntityIdCollector<>(() -> 0L));
        return doInTransaction(session -> (List<Company>)session.createQuery(
            "SELECT DISTINCT ce FROM Company ce\n"
            + "INNER JOIN ce.hostname h\n"
            + "WHERE ce.status = :active\n"
            + "AND h.domain.id IN (:domainIds)")
            .setParameter("active", CompanyStatus.Active)
            .setParameterList("domainIds", domainIds)
            .list());
    }

    /**
     * Gets active companies for a user.
     * This is all {@link User#getCompanies()} less any
     * Companies whose hostname authentication domain is not
     * present on the {@link User#getPrincipal()}.
     * @param user the user.
     * @return the active companies for the user.
     */
    @SuppressWarnings("unchecked")
    public List<Company> getActiveCompanies(Principal user)
    {
        List<Long> domainIds = _er.reattachIfNecessary(user.getAuthenticationDomains())
            .stream().map(AuthenticationDomain::getId).collect(new EntityIdCollector<>(() -> 0L));
        return doInTransaction(session -> (List<Company>)session.createQuery(
            "SELECT DISTINCT ce FROM Company ce\n"
            + "INNER JOIN ce.hostname h\n"
            + "WHERE ce.status = :active\n"
            + "AND h.domain.id IN (:domainIds)")
            .setParameter("active", CompanyStatus.Active)
            .setParameterList("domainIds", domainIds)
            .list());
    }

    /**
     * Update Admins for Company.
     * <ul>
     *     <li>Update Admin authentication domains to include Company authentication domain.</li>
     * </ul>
     * @param company company.
     */
    public void updateAdminsForCompany(Company company)
    {
        doInTransaction(session -> {
            AuthenticationDomain domain = company.getHostname().getDomain();
            assert domain != null;
            @SuppressWarnings("unchecked")
            List<Principal> admins = (List<Principal>)
                    session.createQuery("SELECT DISTINCT p FROM Principal p INNER JOIN p.children r\n"
                                    + "WHERE r.programmaticName = :adminRolePN")
                    .setCacheRegion(UsersCacheRegions.ROLE_QUERY)
                    .setCacheable(true)
                    .setParameter("adminRolePN", _adminRoleProgId)
                .list();
            for (Principal admin : admins)
            {
                if(!admin.getAuthenticationDomains().contains(domain))
                {
                    admin.getAuthenticationDomains().add(domain);
                    session.update(admin);
                }
            }
        });
    }

    /**
     * Get the first owning Company for the given User.  If one is not found, an exception is thrown.
     *
     * @param user the User to search for
     *
     * @return the Company for the User
     *
     * @throws IllegalStateException if a Company could not be found for the user.
     */
    @Nonnull
    public Company getFirstCompanyForUser(@Nonnull User user)
    {
        return user.getCompanies().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("User should have at least one owning Company"));
    }

    /**
     * Is valid company for current user boolean.
     *
     * @param coaching the coaching
     *
     * @return the boolean
     */
    public boolean isValidCompanyForCurrentUser(@Nonnull Company coaching)
    {
        final User currentUser = _userDAO.getAssertedCurrentUser();
        final List<Company> companies = getCompaniesThatUserHasMembershipFor(currentUser, AppUtil.UTC);
        return currentUser.getCompanies().contains(coaching)
               || companies.contains(coaching)
               || _appUtil.userHasAdminRole(currentUser);
    }

    /**
     * Get users matching the specified parameters.
     * All parameters are optional. If none are specified, an empty list is returned.
     *
     * @param firstName first name.
     * @param lastName last name.
     * @param email email address.
     * @param exclude optional exclusion collection.
     * @param company the company
     *
     * @return the user list.
     */
    public List<User> getUsers(@Nullable String firstName, @Nullable String lastName, @Nullable String email,
        @Nullable Collection<User> exclude, @Nonnull Company company)
    {
        boolean hasFirst = !isEmptyString(firstName);
        boolean hasLast = !isEmptyString(lastName);
        boolean hasEmail = !isEmptyString(email);
        boolean hasExclude = exclude != null && !exclude.isEmpty();
        if(!hasFirst && !hasLast && !hasEmail)
            return new ArrayList<>();
        StringBuilder hql = new StringBuilder();
        hql.append( "SELECT DISTINCT user FROM Company ce\n"
                    + " INNER JOIN ce.users user\n"
                    + " INNER JOIN user.principal as p\n"
                    + " LEFT JOIN p.contact as c \n");
        if(hasFirst || hasLast)
        {
            hql.append(" LEFT JOIN c.name as n\n");
        }
        if(hasEmail)
        {
            hql.append(" LEFT JOIN c.emailAddresses as ea\n");
        }
        hql.append(" WHERE (");
        if(hasFirst)
        {
            hql.append(" LOWER(n.first) LIKE :firstName");
        }
        if(hasLast)
        {
            if(hasFirst) hql.append(" OR");
            hql.append(" LOWER(n.last) LIKE :lastName");
        }
        if(hasEmail)
        {
            if(hasFirst || hasLast) hql.append(" OR");
            hql.append(" LOWER(ea.email) LIKE :email");
        }
        hql.append(")\n");

        if(hasExclude)
        {
            hql.append(" AND user NOT IN (:exclude)\n");
        }

        hql.append("AND ce.id = :ceid");

        final Session session = getSession();
        final Query query = session.createQuery(hql.toString());
        query.setCacheable(true);
        query.setCacheRegion(ProjectCacheRegions.MEMBER_QUERY);

        if (hasFirst) query.setParameter("firstName", '%' + firstName.trim().toLowerCase() + '%');
        if (hasLast) query.setParameter("lastName", '%' + lastName.trim().toLowerCase() + '%');
        if (hasEmail) query.setParameter("email", '%' + email.trim().toLowerCase() + '%');
        if (hasExclude) query.setParameterList("exclude", exclude);
        query.setParameter("ceid", company.getId());

        @SuppressWarnings("unchecked")
        final List<User> list = query.list();
        return list;
    }

    /**
     * Gets company for hostname.
     *
     * @param hostname the hostname
     *
     * @return the company for the given hostname, or null, if no Company could be found.
     */
    public Company getCompanyForHostname(Hostname hostname)
    {
        return doInTransaction(session -> (Company) session.createQuery(
            "SELECT ce FROM Company ce\n"
                        + "WHERE ce.hostname.id = :hostnameId")
            .setParameter("hostnameId", hostname.getId())
            .setMaxResults(1)
            .uniqueResult());
    }

    private Object readResolve() throws ObjectStreamException
    {
        ApplicationContext context = ApplicationContextUtils.getInstance().getContext();
        assert context != null;
        return context.getBean(CompanyDAO.class);
    }

    @Contract(pure = true)
    private Object writeReplace() throws ObjectStreamException
    {
        return this;
    }
}
