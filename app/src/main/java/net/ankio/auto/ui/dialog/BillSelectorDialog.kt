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

package net.ankio.auto.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.hjq.toast.Toaster
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.DialogBillSelectBinding
import net.ankio.auto.events.AutoServiceErrorEvent
import net.ankio.auto.exceptions.AutoServiceException
import net.ankio.auto.ui.adapter.BillSelectorAdapter
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.event.EventBus
import net.ankio.auto.utils.server.model.SettingModel
import net.ankio.common.constant.BillType
import net.ankio.common.model.BillModel

class BillSelectorDialog(
    private val context: Context,
    private val billType: BillType,
    private val selectedBills: ArrayList<BillModel> = ArrayList(),
    private val callback: () -> Unit,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBillSelectBinding
    private val dataItems = mutableListOf<BillModel>()
    private val adapter = BillSelectorAdapter(dataItems, selectedBills)

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBillSelectBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        cardView = binding.cardView
        cardViewInner = binding.innerView

        binding.recyclerView.adapter = adapter

        binding.btn.setOnClickListener {
            callback.invoke()
            dismiss()
        }

        return binding.root
    }

    override fun show(
        float: Boolean,
        cancel: Boolean,
    ) {
        lifecycleScope.launch {
            runCatching {
                val it = SettingModel.get("server", "auto_bills_${billType.name}")
                val data = Gson().fromJson(it, Array<BillModel>::class.java)
                if (data.isEmpty()) {
                    dismiss()
                    Toaster.show(R.string.no_bills)
                    return@runCatching
                }
                super.show(float, cancel)
                dataItems.addAll(data)

                adapter.notifyItemInserted(0)
            }.onFailure {
                dismiss()
                Toaster.show(R.string.no_bills)
                Logger.e("get auto_bills_${billType.name} error", it)
                if (it is AutoServiceException) {
                    EventBus.post(AutoServiceErrorEvent(it))
                }
            }
        }
    }
}
