/*
 * Copyright 2016 Geetesh Kalakoti <kalakotig@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.geeteshk.hyper.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import io.geeteshk.hyper.R
import io.geeteshk.hyper.extensions.inflate
import io.geeteshk.hyper.git.GitWrapper
import kotlinx.android.synthetic.main.dialog_pull.view.*
import kotlinx.android.synthetic.main.item_remote.view.*
import java.io.File
import java.util.*

class RemotesAdapter(private val mainContext: Context, private val remotesView: View, private val repo: File) : RecyclerView.Adapter<RemotesAdapter.RemotesHolder>() {

    private val remotesList: ArrayList<String>? = GitWrapper.getRemotes(remotesView, repo)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemotesHolder {
        val view = parent.inflate(R.layout.item_remote)
        return RemotesHolder(view)
    }

    override fun onBindViewHolder(holder: RemotesHolder, position: Int)  = holder.bind(remotesList!![position])

    override fun getItemCount(): Int = remotesList!!.size

    fun add(remote: String, url: String) {
        GitWrapper.addRemote(remotesView, repo, remote, url)
        remotesList!!.add(remote)
        notifyDataSetChanged()
    }

    inner class RemotesHolder(var rootView: View) : RecyclerView.ViewHolder(rootView) {

        fun bind(remote: String) {
            with (rootView) {
                remoteName.text = remote
                remoteUrl.text = GitWrapper.getRemoteUrl(remotesView, repo, remote)

                setOnClickListener {
                    val pullView = View.inflate(context, R.layout.dialog_pull, null)
                    pullView.remotesSpinner.adapter = ArrayAdapter(mainContext, android.R.layout.simple_list_item_1, remotesList!!)
                    AlertDialog.Builder(mainContext)
                            .setTitle("Fetch from remote")
                            .setView(pullView)
                            .setPositiveButton("FETCH") { dialogInterface, _ ->
                                dialogInterface.dismiss()
                                GitWrapper.fetch(context, remotesView, repo, pullView.remotesSpinner.selectedItem as String, pullView.pullUsername.text.toString(), pullView.pullPassword.text.toString())
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                }

                setOnLongClickListener {
                    AlertDialog.Builder(mainContext)
                            .setTitle("Remove $remote?")
                            .setMessage("This remote will be removed permanently.")
                            .setPositiveButton(R.string.remove) { _, _ ->
                                GitWrapper.removeRemote(remotesView, repo, remote)
                                remotesList?.remove(remote)
                                notifyDataSetChanged()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()

                    true
                }
            }
        }
    }
}
