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
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import io.geeteshk.hyper.R
import io.geeteshk.hyper.extensions.inflate
import io.geeteshk.hyper.extensions.intentFor
import io.geeteshk.hyper.extensions.snack
import io.geeteshk.hyper.extensions.withFlags
import io.geeteshk.hyper.ui.activity.ProjectActivity
import io.geeteshk.hyper.util.net.HtmlParser
import io.geeteshk.hyper.util.project.ProjectManager
import kotlinx.android.synthetic.main.item_project.view.*
import java.util.*

class ProjectAdapter(private val mainContext: Context, private val projects: ArrayList<String>, private val layout: CoordinatorLayout, private val recyclerView: RecyclerView) : RecyclerView.Adapter<ProjectAdapter.ProjectHolder>() {

    fun insert(project: String) {
        projects.add(project)
        val position = projects.indexOf(project)
        notifyItemInserted(position)
        recyclerView.scrollToPosition(position)
    }

    fun remove(position: Int) {
        projects.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectHolder {
        projects.sort()
        val itemView = parent.inflate(R.layout.item_project)
        return ProjectHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProjectHolder, position: Int) =
            holder.bind(projects[holder.adapterPosition], holder.adapterPosition)

    override fun getItemCount(): Int = projects.size

    inner class ProjectHolder(var view: View) : RecyclerView.ViewHolder(view) {

        fun bind(project: String, position: Int) {
            with (view) {
                val properties = HtmlParser.getProperties(project)
                title.text = properties[0]
                author.text = properties[1]
                desc.text = properties[2]
                favicon.setImageBitmap(ProjectManager.getFavicon(context, project))

                projectLayout.setOnClickListener {
                    with (mainContext) {
                        (this as AppCompatActivity).startActivityForResult(
                                intentFor<ProjectActivity>("project" to project)
                                    .withFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK).apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                                        }
                                    }, 0)
                    }
                }

                projectLayout.setOnLongClickListener {
                    AlertDialog.Builder(context)
                            .setTitle("${context.getString(R.string.delete)} $project?")
                            .setMessage(R.string.change_undone)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                ProjectManager.deleteProject(project)
                                remove(position)
                                layout.snack("Deleted $project.")
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()

                    true
                }
            }
        }
    }
}
