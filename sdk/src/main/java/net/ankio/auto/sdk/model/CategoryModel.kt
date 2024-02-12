/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.sdk.model

/**
 * 自动记账要求不同分类类型的分类不允许出现重复，即 支出或者收入 分类中，不能出现两个一样的分类，无论是一级分类还是二级分类
 */
data class CategoryModel(
    val name:String,//分类名称
    val icon:String,//分类图标，url或者base64
    val type:Int,//分类类型，0：支出，1：收入
    val sort:Int,//排序
    val id:Int = 0,//分类id
    val parent:Int = -1,//父分类id
    val level:Int = 1,//标记为一级分类还是二级分类
)