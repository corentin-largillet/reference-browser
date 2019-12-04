/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.reference.browser.tabs

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_remotetabs.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.concept.sync.Device
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.ext.components
import kotlin.random.Random


class RemoteTabsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remotetabs)

        val context = this
        CoroutineScope(Dispatchers.Main).launch {

            // TODO: we are only handling the optimistic case. Here are all the possible states:
            // - No Sync account
            // - Connected to FxA but not Sync (unreachable state on mobile at the moment).
            // - Connected to Sync, but needs reconnection.
            // - Connected to Sync, but tabs syncing disabled.
            // - Connected to Sync, but tabs haven't been synced yet (they stay in memory after).
            // - Connected to Sync, but only one device in the account (us), so no other tab to show.
            // - Connected to Sync.

            val remoteTabs = components.remoteTabs.getRemoteTabs()
            val clientList = remoteTabs.map { (client, tabs) ->
                ClientEntry(client, tabs.map { tab -> TabEntryWithID(tab.active()) })
            }

            val adapter = RemoteTabsListAdapter(context, clientList, components.useCases.tabsUseCases.addTab)
            remote_tabs_list.setAdapter(adapter)
            // Expand everything
            for (i in 0 until adapter.groupCount) {
                remote_tabs_list.expandGroup(i)
            }
        }
    }
}

private data class ClientEntry(val device: Device, val tabs: List<TabEntryWithID>)

data class TabEntryWithID(val entry: TabEntry, val id: Long = Random.nextLong())

private class RemoteTabsListAdapter(val context: Context, val clients: List<ClientEntry>, val openTab: TabsUseCases.AddNewTabUseCase): BaseExpandableListAdapter() {
    override fun getGroup(groupPosition: Int): Any {
        return clients[groupPosition]
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val client = clients[groupPosition]
        val inflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_remotetabs_group, parent, false)
        val groupView = view.findViewById<TextView>(R.id.remoteDeviceTitle)
        groupView.text = client.device.displayName
        return view
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val tab = clients[groupPosition].tabs[childPosition].entry
        val inflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_remotetabs_item, parent, false)
        view.setOnClickListener { openTab.invoke(tab.url) }
        val titleView = view.findViewById<TextView>(R.id.remoteTabTitle)
        titleView.text = tab.title
        val descView = view.findViewById<TextView>(R.id.remoteTabDesc)
        descView.text = tab.url
        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return clients[groupPosition].tabs.size
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return clients[groupPosition].tabs[childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return clients[groupPosition].device.id.hashCode().toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return clients[groupPosition].tabs[childPosition].id
    }

    override fun getGroupCount(): Int {
        return clients.size
    }

}
