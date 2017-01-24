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

package experimental.cms.dsl.content

import com.i2rd.cms.bean.JavaScriptBean
import com.i2rd.cms.bean.util.JavaScriptBeanContentBuilder
import com.i2rd.cms.visibility.VisibilityConditionInstance
import experimental.cms.dsl.Content
import experimental.cms.dsl.ContentHelper
import experimental.cms.dsl.ContentInstance
import experimental.cms.dsl.Identifiable
import net.proteusframework.cms.component.ContentElement

class JavaScript(id: String, var javaScriptContent: String = "", var renderInHead: Boolean = false)
    : Identifiable(id), Content {

    override fun createInstance(helper: ContentHelper, existing: ContentElement?): ContentInstance {
        val contentElement = existing?:JavaScriptBean()
        val builder = JavaScriptBeanContentBuilder()
        updateBuilder(builder)
        return ContentInstance(contentElement, builder.content)
    }

    override fun isModified(helper: ContentHelper, contentElement: ContentElement): Boolean {
        val builder = JavaScriptBeanContentBuilder.load(contentElement.publishedData[helper.getCmsSite().primaryLocale], false)
        updateBuilder(builder)
        return builder.isDirty
    }

    private fun updateBuilder(builder: JavaScriptBeanContentBuilder) {
        builder.isOutputInHEAD = renderInHead
        builder.javaScriptData = javaScriptContent
    }

    override fun toString(): String {
        return "JavaScript(" +
            "javaScriptContent='$javaScriptContent'," +
            "renderInHead=$renderInHead," +
            "path='$path'," +
            "htmlId='$htmlId'," +
            "htmlClass='$htmlClass'," +
            "cssPaths=$cssPaths," +
            "javaScriptPaths=$javaScriptPaths" +
            ")"
    }

    override var visibilityCondition: VisibilityConditionInstance? = null
    override var path: String = ""
    override var htmlId: String = ""
    override var htmlClass: String = ""
    override val cssPaths = mutableListOf<String>()
    override val javaScriptPaths = mutableListOf<String>()
    override var parent: Any? = null

}