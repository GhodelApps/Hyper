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

import android.app.Activity
import android.content.Context
import android.view.View
import io.geeteshk.hyper.extensions.snack
import org.eclipse.jgit.api.errors.GitAPIException
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class CheckoutTask internal constructor(context: WeakReference<Context>, view: WeakReference<View>, repo: File, values: Array<String>) : GitTask(context, view, repo, values) {

    init {
        id = 2
    }

    override fun doInBackground(vararg strings: String): Boolean? {
        try {
            val git = GitWrapper.getGit(rootView.get()!!, repo)
            git?.checkout()?.setCreateBranch(strings[0].toBoolean())?.setName(strings[1])?.call()
        } catch (e: GitAPIException) {
            Timber.e(e)
            rootView.get()?.snack(e.toString())
            return false
        }

        return true
    }

    override fun onPostExecute(aBoolean: Boolean?) {
        super.onPostExecute(aBoolean)
        if (aBoolean!!) (context.get() as Activity).finish()
    }
}
