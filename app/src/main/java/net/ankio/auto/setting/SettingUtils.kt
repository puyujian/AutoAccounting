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

package net.ankio.auto.setting

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBinding
import net.ankio.auto.databinding.SettingItemColorBinding
import net.ankio.auto.databinding.SettingItemInputBinding
import net.ankio.auto.databinding.SettingItemSwitchBinding
import net.ankio.auto.databinding.SettingItemTextBinding
import net.ankio.auto.databinding.SettingItemTitleBinding
import net.ankio.auto.setting.types.ItemType
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.ListPopupUtils
import net.ankio.auto.utils.SpUtils

class SettingUtils(
    private val context: Activity,
    private val container: ViewGroup,
    private val inflater: LayoutInflater,
    private val settingItems: ArrayList<SettingItem>
) {
    private val viewBinding = HashMap<SettingItem, ViewBinding>()
    fun render() {
        settingItems.forEach {
            val binding = when (it.type) {
                ItemType.SWITCH -> renderSwitch(it)
                ItemType.TITLE -> renderTitle(it)
                ItemType.TEXT -> renderText(it)
                ItemType.INPUT -> renderInput(it)
                ItemType.COLOR -> renderColor(it)
            }
            viewBinding[it] = binding
            container.addView(binding.root)
        }
    }


    private fun renderTitle(settingItem: SettingItem): SettingItemTitleBinding {
        val binding = SettingItemTitleBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        return binding
    }

    private fun setVisibility(variable: String, variableBoolean: Boolean) {
        val trueKey = "$variable=true"
        val falseKey = "$variable=false"
        viewBinding.forEach { (item, binding) ->
            if (item.regex == trueKey) {
                binding.root.visibility = if (variableBoolean) View.VISIBLE else View.GONE
            } else if (item.regex == falseKey) {
                binding.root.visibility = if (variableBoolean) View.GONE else View.VISIBLE
            }
        }
    }

    private fun renderSwitch(settingItem: SettingItem): SettingItemSwitchBinding {
        val binding = SettingItemSwitchBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        settingItem.subTitle?.let {
            binding.subTitle.setText(it)
        } ?: run {
            binding.subTitle.visibility = View.GONE
        }

        fun setLinkVisibility(isChecked: Boolean) {

            settingItem.variable?.apply {
                setVisibility(this, isChecked)
            }
        }

        binding.switchWidget.post {
            binding.switchWidget.isChecked = settingItem.onGetKeyValue?.invoke()?.let {
                it as Boolean
            } ?: settingItem.key?.let {
                getFromSp(it, (settingItem.default ?: false)) as Boolean
            } ?: false
            setLinkVisibility(binding.switchWidget.isChecked)
        }


        settingItem.icon?.let {
            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
        } ?: run {
            binding.icon.visibility = View.GONE
        }




        fun onClickSwitch() {
            val isChecked = binding.switchWidget.isChecked
            setLinkVisibility(isChecked)
            settingItem.onItemClick?.invoke(isChecked, context) ?: settingItem.key?.let {
                SpUtils.putBoolean(
                    it, isChecked
                )
            }
            settingItem.onSavedValue?.invoke(isChecked, context)
        }

        binding.root.setOnClickListener {
            binding.switchWidget.isChecked = !binding.switchWidget.isChecked
            onClickSwitch()
        }

        binding.switchWidget.setOnClickListener {
            onClickSwitch()
        }






        if (binding.subTitle.visibility == View.VISIBLE) {
            val params = binding.title.layoutParams as ConstraintLayout.LayoutParams
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            binding.title.layoutParams = params
        } else {
            val params = binding.title.layoutParams as ConstraintLayout.LayoutParams
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            binding.title.layoutParams = params
        }





        return binding
    }

    private fun renderText(settingItem: SettingItem): SettingItemTextBinding {
        val binding = SettingItemTextBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        settingItem.subTitle?.let {
            binding.subTitle.setText(it)
        } ?: run {
            binding.subTitle.visibility = View.GONE
        }

        settingItem.icon?.let {
            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
        } ?: run {
            binding.icon.visibility = View.GONE
        }


        settingItem.link?.apply {
            binding.root.setOnClickListener {
                CustomTabsHelper.launchUrlOrCopy(context, this)
            }
        }

        val savedValue = settingItem.onGetKeyValue?.invoke() ?: getFromSp(
            settingItem.key ?: "",
            settingItem.default ?: ""
        )

        settingItem.selectList?.apply {

            fun setValue(savedValue: Any) {
                for ((key, value) in this) {
                    if (value == savedValue) {
                        binding.subTitle.text = key
                        binding.subTitle.visibility = View.VISIBLE
                        break
                    }
                }
            }

            setValue(savedValue)

            val listPopupUtils = ListPopupUtils(context, binding.title, this) { pos, key, value ->
                binding.subTitle.text = key

                settingItem.onItemClick?.invoke(value, context) ?: settingItem.key?.let {
                    SpUtils.putString(it, value.toString())
                    saveToSp(it, value)
                }

                settingItem.onSavedValue?.invoke(value, context)
                setValue(value)
            }

            binding.root.setOnClickListener {
                listPopupUtils.toggle()
            }
        }


        if (binding.subTitle.visibility == View.VISIBLE) {
            val params = binding.title.layoutParams as ConstraintLayout.LayoutParams
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            binding.title.layoutParams = params
        } else {
            val params = binding.title.layoutParams as ConstraintLayout.LayoutParams
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            binding.title.layoutParams = params
        }

        return binding
    }

    private fun renderInput(settingItem: SettingItem): SettingItemInputBinding {
        val binding = SettingItemInputBinding.inflate(inflater, container, false)

        binding.input.post {
            settingItem.onGetKeyValue?.invoke()?.let {
                binding.input.setText(it.toString())
            } ?: run {
                settingItem.key?.apply {
                    binding.input.setText(
                        SpUtils.getString(
                            settingItem.key,
                            (settingItem.default ?: "").toString()
                        )
                    )
                }
            }

            binding.input.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    settingItem.onItemClick?.invoke(binding.input.text.toString(), context)
                        ?: settingItem.key?.let {
                            SpUtils.putString(
                                it, binding.input.text.toString()
                            )
                        }
                    settingItem.onSavedValue?.invoke(binding.input.text.toString(), context)
                }
            }
        }


        binding.inputLayout.setHint(settingItem.title)

        return binding
    }

    private fun renderColor(settingItem: SettingItem): SettingItemColorBinding {
        val binding = SettingItemColorBinding.inflate(inflater, container, false)
        binding.title.setText(settingItem.title)
        val color = context.getColor(settingItem.onGetKeyValue?.invoke() as Int)
        binding.colorView.setCardBackgroundColor(color)

        settingItem.icon?.let {
            binding.icon.setImageDrawable(AppCompatResources.getDrawable(context, it))
        } ?: run {
            binding.icon.visibility = View.GONE
        }

        binding.root.setOnClickListener {
            settingItem.onItemClick?.invoke(color, context)
        }
        return binding
    }


    private fun getFromSp(key: String, default: Any): Any {
        return when (default) {
            is Boolean -> SpUtils.getBoolean(key, default)
            is String -> SpUtils.getString(key, default)
            is Int -> SpUtils.getInt(key, default)
            else -> default
        }
    }

    private fun saveToSp(key: String, value: Any) {
        when (value) {
            is Boolean -> SpUtils.putBoolean(key, value)
            is String -> SpUtils.putString(key, value)
            is Int -> SpUtils.putInt(key, value)
        }
    }


}