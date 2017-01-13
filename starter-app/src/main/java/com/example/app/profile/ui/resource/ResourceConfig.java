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


import com.example.app.profile.model.resource.Resource;
import com.example.app.support.ui.vtcrop.VTCropPictureEditorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Configuration} defining configuration information for {@link Resource} viewers and editors
 *
 * @author Alan Holt (aholt@venturetech.net)
 */
@Configuration
public class ResourceConfig
{
    /**
     * Get the {@link VTCropPictureEditorConfig} for the Resource viewer/editor
     *
     * @return a VTCropPictureEditorConfig
     */
    @Bean
    public VTCropPictureEditorConfig resourcePictureEditorConfig()
    {
        VTCropPictureEditorConfig config = new VTCropPictureEditorConfig();
        config.setMaxHeight(450);
        config.setMaxWidth(700);
        config.setMinHeight(112);
        config.setMinWidth(175);
        config.setCropHeight(225);
        config.setCropWidth(350);
        config.setImageBackgroundStr("rgba(255,255,255, 1.0)");
        config.setImageType("image/jpeg");
        config.setImageScales(new VTCropPictureEditorConfig.ImageScaleOption(1.0, 1.0, "resource_img"));
        return config;
    }
}
