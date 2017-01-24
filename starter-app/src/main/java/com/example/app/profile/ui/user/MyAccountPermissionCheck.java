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

package com.example.app.profile.ui.user;

import com.example.app.profile.model.user.User;
import com.example.app.profile.ui.ApplicationFunctions;
import com.example.app.support.service.ApplicationFunctionPermissionCheck;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.proteusframework.internet.http.Request;
import net.proteusframework.users.model.Principal;

/**
 * Service for defining methods for determining access to My Account.
 *
 * @author Alan Holt (aholt@venturetech.net)
 * @since 1/4/17
 */
@Service
public class MyAccountPermissionCheck implements ApplicationFunctionPermissionCheck
{
    @Override
    public boolean checkPermissions(@Nonnull Request request, @Nullable User user)
    {
        return user != null;
    }

    @Override
    public boolean checkPermissions(@Nonnull Request request, @Nullable Principal principal)
    {
        return false; //We always just use the User.
    }

    @Override
    public String getApplicationFunctionName()
    {
        return ApplicationFunctions.User.MY_ACCOUNT_VIEW;
    }
}
