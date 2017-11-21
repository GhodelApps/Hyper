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

package io.geeteshk.hyper.git

import android.content.Context
import android.support.design.widget.Snackbar
import android.text.SpannableString
import android.util.Log
import android.view.View
import android.widget.TextView
import io.geeteshk.hyper.R
import io.geeteshk.hyper.adapter.ProjectAdapter
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.StringUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

object GitWrapper {

    private val TAG = GitWrapper::class.java.simpleName

    fun init(context: Context, repo: File, view: View) {
        try {
            val git = Git.init()
                    .setDirectory(repo)
                    .call()
            Snackbar.make(view, context.getString(R.string.repo_init) + git.repository.directory, Snackbar.LENGTH_LONG).show()
        } catch (e: GitAPIException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
        }

    }

    fun add(view: View, repo: File) {
        try {
            val git = getGit(view, repo)
            git?.let {
                git.add()
                        .addFilepattern(".")
                        .call()
                Snackbar.make(view, R.string.added_to_stage, Snackbar.LENGTH_LONG).show()
            }
        } catch (e: GitAPIException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
        }

    }

    fun commit(context: Context, view: View, repo: File, message: String) {
        CommitTask(context, view, repo, arrayOf("Committing changes", "Committed successfully.", "Unable to commit files.")).execute(message)
    }

    private fun changeTextToNone(text: String): String {
        return if (StringUtils.isEmptyOrNull(text)) {
            "None\n"
        } else text

    }

    fun status(view: View, repo: File, vararg t: TextView) {
        try {
            val git = getGit(view, repo)
            git?.let {
                val status = git.status()
                        .call()

                val conflicting = status.conflicting
                val conflictingOut = StringBuilder()
                for (conflict in conflicting) {
                    conflictingOut.append(conflict).append("\n")
                }
                t[0].text = changeTextToNone(conflictingOut.toString())

                val added = status.added
                val addedOut = StringBuilder()
                for (add in added) {
                    addedOut.append(add).append("\n")
                }
                t[1].text = changeTextToNone(addedOut.toString())

                val changed = status.changed
                val changedOut = StringBuilder()
                for (change in changed) {
                    changedOut.append(change).append("\n")
                }
                t[2].text = changeTextToNone(changedOut.toString())

                val missing = status.missing
                val missingOut = StringBuilder()
                for (miss in missing) {
                    missingOut.append(miss).append("\n")
                }
                t[3].text = changeTextToNone(missingOut.toString())

                val modified = status.modified
                val modifiedOut = StringBuilder()
                for (mod in modified) {
                    modifiedOut.append(mod).append("\n")
                }
                t[4].text = changeTextToNone(modifiedOut.toString())

                val removed = status.removed
                val removedOut = StringBuilder()
                for (remove in removed) {
                    removedOut.append(remove).append("\n")
                }
                t[5].text = changeTextToNone(removedOut.toString())

                val uncommitted = status.uncommittedChanges
                val uncommittedOut = StringBuilder()
                for (uncom in uncommitted) {
                    uncommittedOut.append(uncom).append("\n")
                }
                t[6].text = changeTextToNone(uncommittedOut.toString())

                val untracked = status.untracked
                val untrackedOut = StringBuilder()
                for (untrack in untracked) {
                    untrackedOut.append(untrack).append("\n")
                }
                t[7].text = changeTextToNone(untrackedOut.toString())

                val untrackedFolders = status.untrackedFolders
                val untrackedFoldersOut = StringBuilder()
                for (untrackedf in untrackedFolders) {
                    untrackedFoldersOut.append(untrackedf).append("\n")
                }
                t[8].text = changeTextToNone(untrackedFoldersOut.toString())
            }
        } catch (e: GitAPIException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
        }

    }

    fun getCommits(view: View, repo: File): List<RevCommit>? {
        var log: Iterable<RevCommit>? = null
        val revCommits = ArrayList<RevCommit>()
        try {
            val git = getGit(view, repo)
            git?.let {
                log = git.log()
                        .call()
            }
        } catch (e: GitAPIException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
            return null
        }

        log?.let {
            revCommits += log!!
        }

        return revCommits
    }

    fun getBranches(view: View, repo: File): List<Ref>? {
        var branches: List<Ref>? = null
        try {
            val git = getGit(view, repo)
            git?.let {
                branches = git.branchList()
                        .call()
            }
        } catch (e: GitAPIException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
            return null
        }

        return branches
    }

    fun createBranch(context: Context, view: View, repo: File, branchName: String, checked: Boolean) {
        if (checked) {
            CheckoutTask(context, view, repo, arrayOf("Creating new branch", "Checked out successfully.", "Unable to checkout.")).execute(true.toString(), branchName)
        } else {
            try {
                val git = getGit(view, repo)
                git?.branchCreate()?.setName(branchName)?.call()
            } catch (e: GitAPIException) {
                Log.e(TAG, e.toString())
                Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
            }

        }
    }

    fun deleteBranch(view: View, repo: File, vararg branches: String) {
        try {
            val git = getGit(view, repo)
            git?.branchDelete()?.setBranchNames(*branches)?.call()
        } catch (e: GitAPIException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
        }

    }

    fun checkout(context: Context, view: View, repo: File, branch: String) {
        CheckoutTask(context, view, repo, arrayOf("Checking out", "Checked out successfully.", "Unable to checkout.")).execute(false.toString(), branch)
    }

    fun getCurrentBranch(view: View, repo: File): String? {
        var branch = ""
        try {
            val git = getGit(view, repo)
            git?.let {
                branch = git.repository
                        .fullBranch
            }
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
            return null
        }

        return branch
    }

    fun clone(context: Context, view: View, repo: File, adapter: ProjectAdapter, remoteUrl: String, username: String, password: String) {
        if (!repo.exists()) {
            CloneTask(context, view, repo, adapter).execute(remoteUrl, username, password)
        } else {
            Snackbar.make(view, R.string.folder_exists, Snackbar.LENGTH_LONG).show()
        }
    }

    fun push(context: Context, view: View, repo: File, remoteUrl: String, options: BooleanArray, username: String, password: String) {
        PushTask(context, view, repo, arrayOf("Pushing changes", "Successfully pushed commits to remote.", "There was a problem while pushing commits."), options).execute(remoteUrl, username, password)
    }

    fun pull(context: Context, view: View, repo: File, remote: String, username: String, password: String) {
        PullTask(context, view, repo, arrayOf("Pulling changes", "Successfully pulled commits from remote.", "There was a problem while pulling commits.")).execute(remote, username, password)
    }

    fun fetch(context: Context, view: View, repo: File, remote: String, username: String, password: String) {
        FetchTask(context, view, repo, arrayOf("Fetching remote " + remote, "Successfully fetched from $remote.", "There was a problem while fetching from $remote.")).execute(remote, username, password)
    }

    internal fun getGit(view: View, repo: File): Git? {
        try {
            return Git.open(repo)
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
        }

        return null
    }

    private fun getConfig(view: View, repo: File): StoredConfig? {
        val git = getGit(view, repo)
        return git?.repository?.config

    }

    fun getRemoteUrl(view: View, repo: File, remote: String): String {
        var url = ""
        val config = getConfig(view, repo)
        config?.let {
            url = config.getString("remote", remote, "url")
        }

        return url
    }

    fun getRemotes(view: View, repo: File): ArrayList<String>? {
        var remotes: ArrayList<String>? = null
        val config = getConfig(view, repo)
        config?.let {
            remotes = ArrayList(config.getSubsections("remote"))
        }

        return remotes
    }

    fun addRemote(view: View, repo: File, remote: String, url: String) {
        val config = getConfig(view, repo)
        config?.let {
            config.setString("remote", remote, "url", url)
            try {
                config.save()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
                Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
            }

        }
    }

    fun removeRemote(view: View, repo: File, remote: String) {
        val config = getConfig(view, repo)
        config?.let {
            config.unsetSection("remote", remote)
            try {
                config.save()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
                Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
            }

        }
    }

    fun canCommit(view: View, repo: File): Boolean {
        try {
            val git = getGit(view, repo)
            git?.let {
                return git.repository.repositoryState.canCommit() && git.status().call().hasUncommittedChanges()
            }
        } catch (e: GitAPIException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
        }

        return false
    }

    fun canCheckout(view: View, repo: File): Boolean {
        val git = getGit(view, repo)
        return git != null && git.repository.repositoryState.canCheckout()
    }

    fun diff(view: View, repo: File, hash1: ObjectId, hash2: ObjectId): SpannableString? {
        var string: SpannableString? = null
        val git = getGit(view, repo)
        try {
            git?.let {
                val out = ByteArrayOutputStream()
                val formatter = DiffFormatter(out)
                formatter.setRepository(git.repository)
                formatter.format(hash1, hash2)
                string = SpannableString(out.toString())
            }
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
            Snackbar.make(view, e.toString(), Snackbar.LENGTH_LONG).show()
        }

        return string
    }
}
