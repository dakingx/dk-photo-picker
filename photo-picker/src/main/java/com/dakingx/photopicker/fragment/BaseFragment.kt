package com.dakingx.photopicker.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    protected abstract fun restoreState(bundle: Bundle?)

    protected abstract fun storeState(bundle: Bundle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        restoreState(arguments)
        restoreState(savedInstanceState)
    }
}
