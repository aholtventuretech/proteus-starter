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

package com.example.app.profile.ui;


import com.example.app.profile.model.Profile;
import com.example.app.profile.model.company.Company;
import com.example.app.profile.model.user.User;

import net.proteusframework.ui.management.URLProperty;


/**
 * ApplicationFunction properties for {@link URLProperty}.
 *
 * @author Russ Tennant (russ@venturetech.net)
 */
public final class URLProperties
{
    /** URL Property to signify a {@link User} */
    public static final String USER = "user";
    /** Path Info for URL Property user */
    public static final String USER_PATH_INFO = '/' + USER + "-{" + USER + '}';

    /** URL Property to signify a boolean flag - true or false */
    public static final String COPY = "copy";
    /** Path Info for URL Property copy */
    public static final String COPY_PATH_INFO = '/' + COPY + "-{" + COPY + '}';

    /** URL Property to specify a context for saving an entity */
    public static final String SAVE_CONTEXT = "save-context";
    /** Path Info for URL Property save-context */
    public static final String SAVE_CONTEXT_PATH_INFO = '/' + SAVE_CONTEXT + "-{" + SAVE_CONTEXT + '}';

    /** URL Property to signify a {@link Profile} */
    public static final String PROFILE = "profile";
    /** Path Info for URL Property profile */
    public static final String PROFILE_PATH_INFO = '/' + PROFILE + "-{" + PROFILE + '}';

    /** URL Property to signify a selector action that should be fired upon page load */
    public static final String SELECTOR_ACTION = "selector-action";
    /** Path Info for URL Property selector-action */
    public static final String SELECTOR_ACTION_PATH_INFO = '/' + SELECTOR_ACTION + "-{" + SELECTOR_ACTION + '}';

    /** URL Property to signify a {@link Company} */
    public static final String COMPANY = "company";
    /** Path Info for URL Property company */
    public static final String COMPANY_PATH_INFO = '/' + COMPANY + "-{" + COMPANY + '}';

    private URLProperties()
    {
    }

}
