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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.Assets
import net.ankio.auto.databinding.DialogBookSelectBinding
import net.ankio.auto.ui.adapter.AssetsSelectorAdapter

class AssetsSelectorDialog(private val context: Context,private val callback:(Assets)->Unit): BaseSheetDialog(context) {
    private lateinit var binding: DialogBookSelectBinding
    override fun onCreateView(inflater: LayoutInflater): View {
        binding =  DialogBookSelectBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        cardView = binding.cardView
        cardViewInner = binding.recyclerView


        val dataItems = mutableListOf<Assets>()

        val adapter = AssetsSelectorAdapter(dataItems) { item ->
            callback(item)
            dismiss()
        }

        binding.recyclerView.adapter = adapter


        lifecycleScope.launch {
            val newData = withContext(Dispatchers.IO) {
                Db.get().AssetsDao().loadAll()
            }

            val collection = newData.takeIf { it.isNotEmpty() } ?: listOf()

            dataItems.addAll(collection)

            adapter.notifyItemInserted(0)
        }

        return binding.root
    }

}