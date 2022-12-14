/*
 * Copyright 2020 http://mobile-dev.pro
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

package com.mobiledevpro.common.ui.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.mobiledevpro.common.ui.extension.getThemeColorCompatible
import com.mobiledevpro.common.ui.extension.isColorLight

abstract class BaseFragment<B : ViewDataBinding>(
    @LayoutRes
    private val layoutId: Int,
    private val settings: FragmentSettings
) : Fragment() {

    lateinit var viewBinding: B

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(settings.optionsMenuId != 0 || settings.homeIconBackPressEnabled)

        if (settings.enterTransition != 0 || settings.exitTransition != 0)
            TransitionInflater.from(requireContext()).let {
                if (settings.enterTransition != 0)
                    enterTransition = it.inflateTransition(settings.enterTransition)
                if (settings.exitTransition != 0)
                    exitTransition = it.inflateTransition(settings.exitTransition)
            }
    }

    /**
     * Called to Initialize view data binding variables when fragment view is created.
     */
    abstract fun onInitDataBinding()

    abstract fun observeLifecycleEvents()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        viewBinding.lifecycleOwner = viewLifecycleOwner
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onInitDataBinding()
        observeLifecycleEvents()
        applyResources()
        if (settings.statusBarColor != 0)
            setStatusBarContentColor(settings.statusBarColor, view)

        if (settings.navigationBarColor != 0)
            setNavigationBarContentColor(settings.navigationBarColor, view)
    }

    override fun onStop() {
        val activity = requireActivity()

        //hide keyboard if it was shown
        val inputManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?

        inputManager?.let {
            val view = activity.currentFocus
            it.hideSoftInputFromWindow(view?.windowToken, InputMethodManager.RESULT_UNCHANGED_SHOWN)
        }

        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        //Menu items are doubling after fragment has been re-created. Need to execute clear()
        menu.clear()
        if (settings.optionsMenuId != 0) {
            inflater.inflate(settings.optionsMenuId, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home && settings.homeIconBackPressEnabled) {
            requireActivity().onBackPressed()
            true
        } else super.onOptionsItemSelected(item)
    }


    private fun applyResources() {
        val isBaseActivity = requireActivity() is BaseActivityInterface

        if (!isBaseActivity) {
            if (settings.statusBarColor != 0)
                throw UnsupportedOperationException("Activity must be inherited from 'BaseActivity' to set StatusBar color")

            if (settings.appBarColor != 0)
                throw UnsupportedOperationException("Activity must be inherited from 'BaseActivity' to set AppBar color")

            if (settings.navigationBarColor != 0)
                throw UnsupportedOperationException("Activity must be inherited from 'BaseActivity' to set Navigation Bar color")

            if (settings.appBarTitle as Int != 0 || (settings.appBarTitle as String).isNotEmpty())
                throw UnsupportedOperationException("Activity must be inherited from 'BaseActivity' to set AppBar title")

            if (settings.appBarSubTitle as Int != 0 || (settings.appBarSubTitle as String).isNotEmpty())
                throw UnsupportedOperationException("Activity must be inherited from 'BaseActivity' to set AppBar sub-title")

            if (settings.homeIconId != 0)
                throw UnsupportedOperationException("Activity must be inherited from 'BaseActivity' to set home indicator icon")

            if (settings.appBarTitleColor != 0)
                throw UnsupportedOperationException("Activity must be inherited from 'BaseActivity' to set AppBar Title color")

        }

        (requireActivity() as BaseActivityInterface).apply {
            //apply title
            setAppBarTitle(
                when (settings.appBarTitle) {
                    is Int -> if (settings.appBarTitle != 0) resources.getString(settings.appBarTitle) else ""
                    is String -> settings.appBarTitle.ifEmpty { "" }
                    else -> ""
                }
            )

            //apply sub-title
            setAppBarSubTitle(
                when (settings.appBarSubTitle) {
                    is Int -> if (settings.appBarSubTitle != 0) resources.getString(settings.appBarSubTitle) else ""
                    is String -> settings.appBarSubTitle.ifEmpty { "" }
                    else -> ""
                }
            )

            //apply color to appbar
            if (settings.appBarColor != 0)
                setAppBarColor(settings.appBarColor)

            //apply color to status bar
            if (settings.statusBarColor != 0)
                setStatusBarColor(settings.statusBarColor)

            //apply color to appbar title
            if (settings.appBarTitleColor != 0)
                setAppBarTitleColor(settings.appBarTitleColor)

            //apply window background
            if (settings.appWindowBackground != 0)
                setAppWindowBackground(settings.appWindowBackground)

            //apply color to navigation bar
            if (settings.navigationBarColor != 0)
                setNavigationBarColor(settings.navigationBarColor)

            //enable or disable home icon (0 - disable)
            setHomeAsUpIndicatorIcon(settings.homeIconId)
        }

    }

    private fun setStatusBarContentColor(@AttrRes statusBarColor: Int, view: View) {
        val rgb: Int = requireContext().getThemeColorCompatible(statusBarColor) // 0xAARRGGBB
        val isLight = rgb.isColorLight()

        //For API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            view.windowInsetsController?.setSystemBarsAppearance(
                if (isLight) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0, // value
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS // mask
            )

        //for API 23+
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isLight) {
                var flags = view.systemUiVisibility
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                view.systemUiVisibility = flags
            }
            requireActivity().window.statusBarColor = rgb
        }
    }

    private fun setNavigationBarContentColor(@AttrRes navigationBarColor: Int, view: View) {
        val rgb: Int = requireContext().getThemeColorCompatible(navigationBarColor) // 0xAARRGGBB

        val isLight = rgb.isColorLight()

        //For API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            view.windowInsetsController?.setSystemBarsAppearance(
                if (isLight) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0, // value
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS // mask
            )

        //for API 23+
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isLight) {
                var flags = view.systemUiVisibility
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                view.systemUiVisibility = flags
            }
            requireActivity().window.navigationBarColor = rgb
        }
    }

}
